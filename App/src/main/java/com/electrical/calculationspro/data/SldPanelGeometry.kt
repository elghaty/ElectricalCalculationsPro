
package com.electrical.calculationspro.data

import kotlin.math.max

/**
 * Shared physical geometry for panel enclosure, busbars and breakers.
 *
 * Coordinates use the same logical canvas space as SldNode.x and SldNode.y.
 * This class calculates geometry only; it never changes electrical topology.
 */
object SldPanelGeometry {

    private const val NODE_WIDTH = 180f
    private const val NODE_HEIGHT = 100f

    private const val ENCLOSURE_LEFT_MARGIN = 24f
    private const val ENCLOSURE_TOP_MARGIN = 28f
    private const val ENCLOSURE_RIGHT_MARGIN = 24f
    private const val ENCLOSURE_BOTTOM_MARGIN = 30f

    private const val BUSBAR_TOP = 62f
    private const val BUSBAR_SIDE_MARGIN = 36f
    private const val BREAKER_TOP = 142f
    private const val BREAKER_WIDTH = 64f
    private const val BREAKER_GAP = 36f
    private const val MIN_ENCLOSURE_WIDTH = 240f

    data class Point(
        val x: Float,
        val y: Float
    )

    data class Rect(
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float
    ) {
        val right: Float
            get() = left + width

        val bottom: Float
            get() = top + height

        fun contains(point: Point): Boolean =
            point.x in left..right && point.y in top..bottom
    }

    data class BreakerGeometry(
        val nodeId: String,
        val bounds: Rect,
        val center: Point,
        val connectionPoint: Point
    )

    data class BusbarGeometry(
        val nodeId: String?,
        val start: Point,
        val end: Point
    )

    data class PanelLayout(
        val panelId: String,
        val enclosure: Rect,
        val busbar: BusbarGeometry,
        val breakers: List<BreakerGeometry>,
        val internalNodeIds: Set<String>
    )

    /**
     * Returns the physical geometry of one panel.
     *
     * Internal members are discovered from BUSBAR connections. Only
     * breakers directly connected to the panel or its internal bus are
     * placed in this enclosure. CABLE connections remain external feeders.
     */
    fun calculate(
        network: SldNetwork,
        panel: SldNode
    ): PanelLayout? {
        if (panel.type != SldNodeType.PANEL) return null
        if (network.nodes.none { it.id == panel.id }) return null

        val nodesById = network.nodes.associateBy { it.id }
        val internalConnections = network.connections.filter {
            it.connectionType == SldConnectionType.BUSBAR
        }

        val memberIds = linkedSetOf(panel.id)
        val busIds = linkedSetOf<String>()
        val breakerIds = linkedSetOf<String>()

        /*
         * Discover only PANEL ↔ BUS ↔ BREAKER and PANEL ↔ BREAKER
         * relationships. Do not absorb loads or external feeders.
         */
        var changed: Boolean
        do {
            changed = false

            internalConnections.forEach { connection ->
                val from = nodesById[connection.fromNodeId] ?: return@forEach
                val to = nodesById[connection.toNodeId] ?: return@forEach

                val fromInside = from.id in memberIds
                val toInside = to.id in memberIds

                if (!fromInside && !toInside) return@forEach

                val candidate = when {
                    from.type == SldNodeType.PANEL &&
                        to.type == SldNodeType.BUS -> to

                    from.type == SldNodeType.BUS &&
                        to.type == SldNodeType.PANEL -> from

                    from.type == SldNodeType.BUS &&
                        to.type == SldNodeType.BREAKER -> to

                    from.type == SldNodeType.BREAKER &&
                        to.type == SldNodeType.BUS -> from

                    from.type == SldNodeType.PANEL &&
                        to.type == SldNodeType.BREAKER -> to

                    from.type == SldNodeType.BREAKER &&
                        to.type == SldNodeType.PANEL -> from

                    else -> null
                } ?: return@forEach

                if (memberIds.add(candidate.id)) changed = true

                when (candidate.type) {
                    SldNodeType.BUS -> busIds.add(candidate.id)
                    SldNodeType.BREAKER -> breakerIds.add(candidate.id)
                    else -> Unit
                }
            }
        } while (changed)

        val orderedBreakers = breakerIds
            .mapNotNull(nodesById::get)
            .sortedWith(
                compareBy<SldNode> { it.x }
                    .thenBy { it.name.uppercase() }
            )

        val breakerCount = orderedBreakers.size
        val requiredWidth = if (breakerCount == 0) {
            MIN_ENCLOSURE_WIDTH
        } else {
            ENCLOSURE_LEFT_MARGIN +
                ENCLOSURE_RIGHT_MARGIN +
                breakerCount * BREAKER_WIDTH +
                max(0, breakerCount - 1) * BREAKER_GAP
        }

        val enclosureWidth = max(MIN_ENCLOSURE_WIDTH, requiredWidth)
        val enclosureLeft = panel.x - ENCLOSURE_LEFT_MARGIN
        val enclosureTop = panel.y - ENCLOSURE_TOP_MARGIN
        val enclosureHeight = BREAKER_TOP +
            BREAKER_WIDTH +
            ENCLOSURE_BOTTOM_MARGIN

        val enclosure = Rect(
            left = enclosureLeft,
            top = enclosureTop,
            width = enclosureWidth,
            height = enclosureHeight
        )

        val busY = enclosureTop + BUSBAR_TOP
        val busStartX = enclosure.left + BUSBAR_SIDE_MARGIN
        val busEndX = enclosure.right - BUSBAR_SIDE_MARGIN

        val primaryBusId = busIds
            .mapNotNull(nodesById::get)
            .minByOrNull { it.x }
            ?.id

        val busbar = BusbarGeometry(
            nodeId = primaryBusId,
            start = Point(busStartX, busY),
            end = Point(busEndX, busY)
        )

        val breakerStartX = enclosure.left +
            (enclosure.width -
                (breakerCount * BREAKER_WIDTH +
                    max(0, breakerCount - 1) * BREAKER_GAP)) / 2f

        val breakers = orderedBreakers.mapIndexed { index, breaker ->
            val left = breakerStartX +
                index * (BREAKER_WIDTH + BREAKER_GAP)
            val top = enclosureTop + BREAKER_TOP
            val bounds = Rect(
                left = left,
                top = top,
                width = BREAKER_WIDTH,
                height = BREAKER_WIDTH
            )

            BreakerGeometry(
                nodeId = breaker.id,
                bounds = bounds,
                center = Point(
                    left + BREAKER_WIDTH / 2f,
                    top + BREAKER_WIDTH / 2f
                ),
                connectionPoint = Point(
                    left + BREAKER_WIDTH / 2f,
                    busY
                )
            )
        }

        return PanelLayout(
            panelId = panel.id,
            enclosure = enclosure,
            busbar = busbar,
            breakers = breakers,
            internalNodeIds = memberIds
        )
    }

    fun calculateAll(network: SldNetwork): Map<String, PanelLayout> =
        network.nodes
            .asSequence()
            .filter { it.type == SldNodeType.PANEL }
            .mapNotNull { panel ->
                calculate(network, panel)?.let { panel.id to it }
            }
            .toMap()

    fun panelAt(
        network: SldNetwork,
        point: Point
    ): PanelLayout? =
        calculateAll(network)
            .values
            .firstOrNull { it.enclosure.contains(point) }
}
