
package com.electrical.calculationspro.data

import kotlin.math.max
import kotlin.math.min

/**
 * Physical geometry shared by SLD panel rendering and hit testing.
 *
 * Coordinates are in the same logical canvas space as SldNode.x/y.
 * Geometry follows the actual node positions so rendering, dragging,
 * selection and auto-layout remain aligned.
 *
 * This class does not modify topology or perform electrical calculations.
 */
object SldPanelGeometry {

    private const val NODE_WIDTH = 180f
    private const val NODE_HEIGHT = 100f

    private const val PANEL_MARGIN_X = 24f
    private const val PANEL_MARGIN_TOP = 28f
    private const val PANEL_MARGIN_BOTTOM = 28f

    private const val BUSBAR_Y_OFFSET = 25f
    private const val BUSBAR_SIDE_MARGIN = 24f

    private const val BREAKER_SYMBOL_WIDTH = 50f
    private const val BREAKER_SYMBOL_HEIGHT = 38f
    private const val BREAKER_SYMBOL_Y_OFFSET = 7f

    private const val MIN_PANEL_WIDTH = 240f
    private const val MIN_PANEL_HEIGHT = 190f

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
            point.x >= left &&
                point.x <= right &&
                point.y >= top &&
                point.y <= bottom
    }

    data class BreakerGeometry(
        val nodeId: String,
        val nodeBounds: Rect,
        val symbolBounds: Rect,
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
     * Builds geometry for a panel using the actual node coordinates.
     *
     * BUSBAR connections identify internal panel members.
     * CABLE connections are not treated as internal panel wiring.
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

        val internalNodeIds = linkedSetOf(panel.id)
        val busIds = linkedSetOf<String>()
        val breakerIds = linkedSetOf<String>()

        var changed: Boolean

        do {
            changed = false

            internalConnections.forEach { connection ->
                val from = nodesById[connection.fromNodeId]
                    ?: return@forEach
                val to = nodesById[connection.toNodeId]
                    ?: return@forEach

                val fromInside = from.id in internalNodeIds
                val toInside = to.id in internalNodeIds

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

                if (internalNodeIds.add(candidate.id)) {
                    changed = true
                }

                when (candidate.type) {
                    SldNodeType.BUS -> busIds.add(candidate.id)
                    SldNodeType.BREAKER -> breakerIds.add(candidate.id)
                    else -> Unit
                }
            }
        } while (changed)

        val breakers = breakerIds
            .mapNotNull(nodesById::get)
            .sortedWith(
                compareBy<SldNode> { it.x }
                    .thenBy { it.y }
                    .thenBy { it.name.uppercase() }
            )
            .map { breaker ->
                val symbolLeft =
                    breaker.x + (NODE_WIDTH - BREAKER_SYMBOL_WIDTH) / 2f

                val symbolTop =
                    breaker.y + BREAKER_SYMBOL_Y_OFFSET

                BreakerGeometry(
                    nodeId = breaker.id,
                    nodeBounds = Rect(
                        left = breaker.x,
                        top = breaker.y,
                        width = NODE_WIDTH,
                        height = NODE_HEIGHT
                    ),
                    symbolBounds = Rect(
                        left = symbolLeft,
                        top = symbolTop,
                        width = BREAKER_SYMBOL_WIDTH,
                        height = BREAKER_SYMBOL_HEIGHT
                    ),
                    center = Point(
                        x = symbolLeft + BREAKER_SYMBOL_WIDTH / 2f,
                        y = symbolTop + BREAKER_SYMBOL_HEIGHT / 2f
                    ),
                    connectionPoint = Point(
                        x = breaker.x + NODE_WIDTH / 2f,
                        y = breaker.y + BUSBAR_Y_OFFSET
                    )
                )
            }

        val busNodes = busIds.mapNotNull(nodesById::get)

        /*
         * Use the real positions of the panel and its internal members.
         * This avoids calculating a second, conflicting set of breaker
         * coordinates that would disagree with drag and hit testing.
         */
        val members = listOf(panel) +
            busNodes +
            breakers.mapNotNull { nodesById[it.nodeId] }

        val minX = members.minOfOrNull { it.x } ?: panel.x
        val minY = members.minOfOrNull { it.y } ?: panel.y

        val maxX = members.maxOfOrNull { it.x + NODE_WIDTH }
            ?: (panel.x + NODE_WIDTH)

        val maxY = members.maxOfOrNull { it.y + NODE_HEIGHT }
            ?: (panel.y + NODE_HEIGHT)

        val enclosureLeft = minX - PANEL_MARGIN_X
        val enclosureTop = minY - PANEL_MARGIN_TOP

        val enclosureWidth = max(
            MIN_PANEL_WIDTH,
            maxX - minX + PANEL_MARGIN_X * 2f
        )

        val enclosureHeight = max(
            MIN_PANEL_HEIGHT,
            maxY - minY + PANEL_MARGIN_TOP + PANEL_MARGIN_BOTTOM
        )

        val enclosure = Rect(
            left = enclosureLeft,
            top = enclosureTop,
            width = enclosureWidth,
            height = enclosureHeight
        )

        val busY = if (busNodes.isNotEmpty()) {
            busNodes.map { it.y + BUSBAR_Y_OFFSET }.average().toFloat()
        } else {
            panel.y + BUSBAR_Y_OFFSET
        }

        val breakerCenters = breakers.map { it.connectionPoint.x }

        val busStartX = min(
            enclosure.right - BUSBAR_SIDE_MARGIN,
            min(
                panel.x + NODE_WIDTH / 2f,
                breakerCenters.minOrNull() ?: (panel.x + NODE_WIDTH / 2f)
            ) - BUSBAR_SIDE_MARGIN
        )

        val busEndX = max(
            enclosure.left + BUSBAR_SIDE_MARGIN,
            max(
                panel.x + NODE_WIDTH / 2f,
                breakerCenters.maxOrNull() ?: (panel.x + NODE_WIDTH / 2f)
            ) + BUSBAR_SIDE_MARGIN
        )

        val primaryBusId = busNodes
            .minByOrNull { it.x }
            ?.id

        return PanelLayout(
            panelId = panel.id,
            enclosure = enclosure,
            busbar = BusbarGeometry(
                nodeId = primaryBusId,
                start = Point(busStartX, busY),
                end = Point(busEndX, busY)
            ),
            breakers = breakers,
            internalNodeIds = internalNodeIds
        )
    }

    fun calculateAll(
        network: SldNetwork
    ): Map<String, PanelLayout> =
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
