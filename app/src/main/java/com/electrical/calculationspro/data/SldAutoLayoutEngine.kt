package com.electrical.calculationspro.data

import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.round

/**
 * Topology-aware SLD layout engine.
 *
 * The engine changes graphical coordinates only.
 * Electrical connections, IDs, direction and engineering
 * properties are preserved.
 */
object SldAutoLayoutEngine {

    private const val START_X = 120f
    private const val START_Y = 100f
    private const val COLUMN_GAP = 320f
    private const val ROW_GAP = 190f
    private const val BREAKER_GAP_X = 200f
    private const val BUS_OFFSET_X = 40f
    private const val BUS_OFFSET_Y = 50f
    private const val BREAKER_OFFSET_Y = 170f
    private const val GRID_SNAP = 20f

    data class LayoutResult(
        val network: SldNetwork,
        val levels: Map<String, Int>
    )

    private data class PanelAssembly(
        val panelId: String,
        val busIds: List<String>,
        val breakerIds: List<String>
    ) {
        val memberIds: Set<String>
            get() = (listOf(panelId) + busIds + breakerIds).toSet()
    }

    /**
     * Automatically arranges the diagram while preserving
     * the original electrical topology.
     */
    fun arrange(network: SldNetwork): LayoutResult {
        if (network.nodes.isEmpty()) {
            return LayoutResult(network, emptyMap())
        }

        val nodesById = network.nodes.associateBy { it.id }
        val assemblies = buildPanelAssemblies(network, nodesById)

        val nodeToPanel = mutableMapOf<String, String>()

        assemblies.forEach { assembly ->
            assembly.memberIds.forEach { id ->
                nodeToPanel.putIfAbsent(id, assembly.panelId)
            }
        }

        fun representative(id: String): String =
            nodeToPanel[id] ?: id

        val representatives = network.nodes
            .map { representative(it.id) }
            .distinct()

        val children = representatives.associateWith {
            linkedSetOf<String>()
        }.toMutableMap()

        val parents = representatives.associateWith {
            linkedSetOf<String>()
        }.toMutableMap()

        /*
         * External connections retain their original direction.
         * Internal BUSBAR links are not treated as feeders.
         */
        network.connections.forEach { connection ->
            val from = nodesById[connection.fromNodeId]
                ?: return@forEach
            val to = nodesById[connection.toNodeId]
                ?: return@forEach

            if (from.id == to.id) return@forEach

            if (isInternalAssemblyConnection(connection, from, to)) {
                return@forEach
            }

            val fromId = representative(from.id)
            val toId = representative(to.id)

            if (fromId == toId) return@forEach

            children.getValue(fromId).add(toId)
            parents.getValue(toId).add(fromId)
        }

        val sourceIds = network.nodes
            .filter { it.type == SldNodeType.SOURCE }
            .map { representative(it.id) }
            .distinct()
            .sortedBy {
                nodesById[it]?.name.orEmpty().uppercase()
            }

        val roots = (
            sourceIds +
                representatives.filter {
                    parents[it].orEmpty().isEmpty()
                }.sortedWith(
                    compareBy<String> {
                        rootPriority(nodesById[it]?.type)
                    }.thenBy {
                        nodesById[it]?.name.orEmpty().uppercase()
                    }
                )
            ).distinct()

        val levels = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        roots.forEach { id ->
            if (levels.putIfAbsent(id, 0) == null) {
                queue.addLast(id)
            }
        }

        /*
         * Breadth-first traversal supports incomplete diagrams
         * and prevents recursive traversal of cyclic networks.
         */
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val nextLevel = levels.getValue(current) + 1

            children[current].orEmpty()
                .sortedBy {
                    nodesById[it]?.name.orEmpty().uppercase()
                }
                .forEach { childId ->
                    if (childId !in levels) {
                        levels[childId] = nextLevel
                        queue.addLast(childId)
                    }
                }
        }

        /*
         * Keep cyclic and disconnected components visible.
         */
        var disconnectedLevel =
            (levels.values.maxOrNull() ?: 0) + 1

        representatives
            .filter { it !in levels }
            .sortedBy {
                nodesById[it]?.name.orEmpty().uppercase()
            }
            .forEach { id ->
                levels[id] = disconnectedLevel++
            }

        val positions = mutableMapOf<String, Pair<Float, Float>>()

        /*
         * Position each external graph representative once.
         */
        representatives
            .groupBy { levels[it] ?: 0 }
            .toSortedMap()
            .forEach { (level, ids) ->
                ids.sortedWith(
                    compareBy<String> {
                        rootPriority(nodesById[it]?.type)
                    }.thenBy {
                        nodesById[it]?.name.orEmpty().uppercase()
                    }
                ).forEachIndexed { row, id ->
                    positions[id] = snapPoint(
                        START_X + level * COLUMN_GAP,
                        START_Y + row * ROW_GAP
                    )
                }
            }

        /*
         * Keep source equipment at the beginning of the diagram.
         */
        sourceIds.forEachIndexed { index, id ->
            positions[id] = snapPoint(
                START_X,
                START_Y + index * ROW_GAP
            )
        }

        /*
         * Position every internal assembly relative to its panel.
         * The assembly is laid out as one logical group.
         */
        assemblies.forEach { assembly ->
            val anchor = positions[assembly.panelId]
                ?: (START_X to START_Y)

            val panelX = anchor.first
            val panelY = anchor.second

            positions[assembly.panelId] =
                snapPoint(panelX, panelY)

            assembly.busIds.forEachIndexed { index, busId ->
                positions[busId] = snapPoint(
                    panelX + BUS_OFFSET_X,
                    panelY + BUS_OFFSET_Y + index * 24f
                )
            }

            assembly.breakerIds.forEachIndexed { index, breakerId ->
                positions[breakerId] = snapPoint(
                    panelX + index * BREAKER_GAP_X,
                    panelY + BREAKER_OFFSET_Y
                )
            }
        }

        /*
         * Place loads connected downstream of breakers.
         * Each load gets a deterministic position. Multiple
         * loads on the same breaker are vertically separated.
         */
        val loadsByBreaker = network.connections
            .mapNotNull { connection ->
                if (
                    connection.connectionType !=
                    SldConnectionType.CABLE
                ) {
                    return@mapNotNull null
                }

                val from = nodesById[connection.fromNodeId]
                    ?: return@mapNotNull null
                val to = nodesById[connection.toNodeId]
                    ?: return@mapNotNull null

                if (
                    from.type == SldNodeType.BREAKER &&
                    to.type == SldNodeType.LOAD
                ) {
                    from.id to to.id
                } else {
                    null
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )

        loadsByBreaker.forEach { (breakerId, loadIds) ->
            val breakerPosition = positions[breakerId]
                ?: return@forEach

            loadIds.distinct().forEachIndexed { index, loadId ->
                positions[loadId] = snapPoint(
                    breakerPosition.first + COLUMN_GAP,
                    breakerPosition.second +
                        index * ROW_GAP
                )
            }
        }

        /*
         * Fallback positions ensure that no node disappears,
         * even when the network is incomplete or unusual.
         */
        network.nodes.forEachIndexed { index, node ->
            if (node.id !in positions) {
                val rep = representative(node.id)
                val base = positions[rep]

                positions[node.id] = if (
                    node.id == rep && base != null
                ) {
                    base
                } else {
                    snapPoint(
                        START_X + (index % 4) * COLUMN_GAP,
                        START_Y + (index / 4) * ROW_GAP
                    )
                }
            }
        }

        /*
         * Normalize coordinates to keep the diagram visible.
         */
        val minX = positions.values.minOfOrNull {
            it.first
        } ?: START_X

        val minY = positions.values.minOfOrNull {
            it.second
        } ?: START_Y

        val offsetX = max(0f, START_X - minX)
        val offsetY = max(0f, START_Y - minY)

        val updatedNodes = network.nodes.map { node ->
            val point = positions[node.id] ?: (node.x to node.y)

            node.copy(
                x = snap(point.first + offsetX),
                y = snap(point.second + offsetY)
            )
        }

        /*
         * Do not reconstruct or modify connections.
         */
        return LayoutResult(
            network = network.copy(nodes = updatedNodes),
            levels = levels
        )
    }

    private fun buildPanelAssemblies(
        network: SldNetwork,
        nodesById: Map<String, SldNode>
    ): List<PanelAssembly> {
        val panels = network.nodes
            .filter { it.type == SldNodeType.PANEL }

        val claimedMembers = mutableSetOf<String>()

        return panels.map { panel ->
            val members = linkedSetOf(panel.id)
            var changed: Boolean

            do {
                changed = false

                network.connections.forEach { connection ->
                    if (
                        connection.connectionType !=
                        SldConnectionType.BUSBAR
                    ) {
                        return@forEach
                    }

                    val from = nodesById[connection.fromNodeId]
                        ?: return@forEach

                    val to = nodesById[connection.toNodeId]
                        ?: return@forEach

                    if (
                        !isInternalAssemblyConnection(
                            connection,
                            from,
                            to
                        )
                    ) {
                        return@forEach
                    }

                    val fromInside = from.id in members
                    val toInside = to.id in members

                    if (
                        fromInside &&
                        to.type in INTERNAL_MEMBER_TYPES &&
                        (
                            to.id !in claimedMembers ||
                                to.id == panel.id
                            )
                    ) {
                        if (members.add(to.id)) changed = true
                    }

                    if (
                        toInside &&
                        from.type in INTERNAL_MEMBER_TYPES &&
                        (
                            from.id !in claimedMembers ||
                                from.id == panel.id
                            )
                    ) {
                        if (members.add(from.id)) changed = true
                    }
                }
            } while (changed)

            val busIds = members
                .filter {
                    nodesById[it]?.type == SldNodeType.BUS
                }
                .sortedBy {
                    nodesById[it]?.name.orEmpty().uppercase()
                }

            val breakerIds = members
                .filter {
                    nodesById[it]?.type == SldNodeType.BREAKER
                }
                .sortedBy {
                    nodesById[it]?.name.orEmpty().uppercase()
                }

            claimedMembers.addAll(busIds)
            claimedMembers.addAll(breakerIds)

            PanelAssembly(
                panelId = panel.id,
                busIds = busIds,
                breakerIds = breakerIds
            )
        }
    }

    private fun isInternalAssemblyConnection(
        connection: SldConnection,
        from: SldNode,
        to: SldNode
    ): Boolean {
        if (
            connection.connectionType !=
            SldConnectionType.BUSBAR
        ) {
            return false
        }

        val first = from.type
        val second = to.type

        return (
            first == SldNodeType.PANEL &&
                second == SldNodeType.BUS
            ) || (
            first == SldNodeType.BUS &&
                second == SldNodeType.PANEL
            ) || (
            first == SldNodeType.BUS &&
                second == SldNodeType.BREAKER
            ) || (
            first == SldNodeType.BREAKER &&
                second == SldNodeType.BUS
            ) || (
            first == SldNodeType.PANEL &&
                second == SldNodeType.BREAKER
            ) || (
            first == SldNodeType.BREAKER &&
                second == SldNodeType.PANEL
            )
    }

    private fun rootPriority(type: SldNodeType?): Int =
        when (type) {
            SldNodeType.SOURCE -> 0
            SldNodeType.GENERATOR -> 1
            SldNodeType.TRANSFORMER -> 2
            SldNodeType.BUS -> 3
            SldNodeType.PANEL -> 4
            SldNodeType.BREAKER -> 5
            SldNodeType.LOAD -> 6
            null -> 7
        }

    private fun snapPoint(
        x: Float,
        y: Float
    ): Pair<Float, Float> =
        snap(x) to snap(y)

    private fun snap(value: Float): Float {
        if (GRID_SNAP <= 0f) return value
        return round(value / GRID_SNAP) * GRID_SNAP
    }

    private val INTERNAL_MEMBER_TYPES = setOf(
        SldNodeType.BUS,
        SldNodeType.BREAKER
    )
}
