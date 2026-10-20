
package com.electrical.calculationspro.data

import kotlin.math.max

/**
 * Topology-aware SLD auto-layout.
 *
 * Responsibilities:
 * - Change node coordinates only.
 * - Preserve all electrical connections and their directions.
 * - Treat PANEL, internal BUS nodes and internal BREAKER nodes
 *   as one physical switchgear assembly.
 * - Keep external feeders and loads outside the panel enclosure.
 * - Place disconnected equipment separately for editor review.
 *
 * This class does not perform electrical calculations.
 */
object SldAutoLayoutEngine {

    private const val NODE_WIDTH = 180f
    private const val NODE_HEIGHT = 100f

    private const val LEVEL_GAP = 300f
    private const val COLUMN_GAP = 300f
    private const val ROW_GAP = 180f

    private const val START_X = 120f
    private const val START_Y = 100f

    private const val PANEL_LEFT_MARGIN = 40f
    private const val PANEL_TOP_MARGIN = 30f
    private const val PANEL_BREAKER_GAP = 150f
    private const val PANEL_BREAKER_Y = 110f
    private const val BUSBAR_Y = 45f

    private const val DISCONNECTED_GAP_X = 360f
    private const val DISCONNECTED_GAP_Y = 240f
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
            get() = (busIds + breakerIds + panelId).toSet()
    }

    /**
     * Computes new coordinates without changing network topology.
     */
    fun arrange(network: SldNetwork): LayoutResult {
        if (network.nodes.isEmpty()) {
            return LayoutResult(
                network = network,
                levels = emptyMap()
            )
        }

        val nodesById = network.nodes.associateBy { it.id }

        val assemblies = buildPanelAssemblies(network)
        val nodeToPanel = mutableMapOf<String, String>()

        assemblies.forEach { assembly ->
            assembly.memberIds.forEach { nodeId ->
                nodeToPanel.putIfAbsent(nodeId, assembly.panelId)
            }
        }

        fun representative(nodeId: String): String =
            nodeToPanel[nodeId] ?: nodeId

        /*
         * Build the external graph.
         *
         * Internal BUSBAR connections are excluded.
         * External cable directions are preserved.
         * Nodes belonging to the same panel assembly are represented
         * by that panel for layout purposes only.
         */
        val children = network.nodes.associate {
            it.id to linkedSetOf<String>()
        }.toMutableMap()

        val parents = network.nodes.associate {
            it.id to linkedSetOf<String>()
        }.toMutableMap()

        network.connections.forEach { connection ->
            val fromNode = nodesById[connection.fromNodeId]
                ?: return@forEach
            val toNode = nodesById[connection.toNodeId]
                ?: return@forEach

            if (connection.fromNodeId == connection.toNodeId) {
                return@forEach
            }

            if (isInternalAssemblyConnection(connection, fromNode, toNode)) {
                return@forEach
            }

            val fromId = representative(fromNode.id)
            val toId = representative(toNode.id)

            if (fromId == toId) {
                return@forEach
            }

            children.getValue(fromId).add(toId)
            parents.getValue(toId).add(fromId)
        }

        /*
         * Determine graph roots.
         * Sources are prioritized; other disconnected roots remain
         * visible instead of being discarded.
         */
        val sources = network.nodes
            .filter { it.type == SldNodeType.SOURCE }
            .sortedBy { it.name.uppercase() }

        val naturalRoots = network.nodes
            .filter { node ->
                val id = representative(node.id)
                parents[id].orEmpty().isEmpty() && id == node.id
            }
            .sortedWith(
                compareBy<SldNode> { typeOrder(it.type) }
                    .thenBy { it.name.uppercase() }
            )

        val roots = (sources + naturalRoots)
            .map { representative(it.id) }
            .distinct()
            .filter { it in nodesById }

        /*
         * Breadth-first levels avoid recursive traversal problems
         * when an unfinished diagram contains a cycle.
         */
        val levels = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        roots.forEach { rootId ->
            if (levels.putIfAbsent(rootId, 0) == null) {
                queue.addLast(rootId)
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val nextLevel = levels.getValue(current) + 1

            children[current].orEmpty()
                .sortedWith(
                    compareBy<String> { nodesById[it]?.name?.uppercase().orEmpty() }
                )
                .forEach { childId ->
                    val oldLevel = levels[childId]

                    if (oldLevel == null) {
                        levels[childId] = nextLevel
                        queue.addLast(childId)
                    } else if (nextLevel < oldLevel) {
                        /*
                         * Do not repeatedly re-enqueue nodes in cyclic
                         * graphs. The first BFS level remains stable.
                         */
                    }
                }
        }

        /*
         * Include nodes in disconnected components and cycle-only
         * components so every piece of equipment remains visible.
         */
        var disconnectedLevel = (levels.values.maxOrNull() ?: 0) + 2

        network.nodes
            .map { representative(it.id) }
            .distinct()
            .filter { it !in levels }
            .sortedWith(
                compareBy<String> { nodesById[it]?.name?.uppercase().orEmpty() }
            )
            .forEach { id ->
                levels[id] = disconnectedLevel++
            }

        /*
         * Place representative nodes in levels.
         *
         * Each level receives a distinct vertical row for predictable
         * left-to-right electrical flow.
         */
        val positions = mutableMapOf<String, Pair<Float, Float>>()

        val representatives = network.nodes
            .map { representative(it.id) }
            .distinct()

        val byLevel = representatives
            .groupBy { levels[it] ?: 0 }
            .toSortedMap()

        byLevel.forEach { (level, ids) ->
            val sortedIds = ids.sortedWith(
                compareBy<String> { rootPriority(nodesById[it]?.type) }
                    .thenBy { nodesById[it]?.name?.uppercase().orEmpty() }
            )

            sortedIds.forEachIndexed { column, id ->
                positions[id] = snap(
                    START_X + column * COLUMN_GAP,
                    START_Y + level * LEVEL_GAP
                )
            }
        }

        /*
         * Put the source at the beginning of its level.
         * This is a graphic preference only; connection direction
         * remains exactly as it was in the original network.
         */
        sources.forEach { source ->
            val sourceId = representative(source.id)
            val level = levels[sourceId] ?: 0
            positions[sourceId] = snap(
                START_X,
                START_Y + level * LEVEL_GAP
            )
        }

        /*
         * Place each panel assembly as one physical group.
         * The panel coordinate is the enclosure anchor.
         * Busbars and breakers are positioned relative to that anchor.
         */
        assemblies.forEach { assembly ->
            val panelPosition = positions[assembly.panelId]
                ?: (START_X to START_Y)

            val panelX = panelPosition.first
            val panelY = panelPosition.second

            positions[assembly.panelId] = snap(panelX, panelY)

            assembly.busIds.forEachIndexed { index, busId ->
                positions[busId] = snap(
                    panelX + PANEL_LEFT_MARGIN,
                    panelY + BUSBAR_Y + index * 24f
                )
            }

            val breakerCount = assembly.breakerIds.size

            if (breakerCount > 0) {
                val span = (breakerCount - 1) * PANEL_BREAKER_GAP
                val firstX = panelX - span / 2f

                assembly.breakerIds.forEachIndexed { index, breakerId ->
                    positions[breakerId] = snap(
                        firstX + index * PANEL_BREAKER_GAP,
                        panelY + PANEL_BREAKER_Y
                    )
                }
            }
        }

        /*
         * Keep direct breaker-to-load feeders outside the enclosure.
         * Other downstream nodes keep their graph-derived placement.
         */
        network.connections.forEach { connection ->
            val from = nodesById[connection.fromNodeId] ?: return@forEach
            val to = nodesById[connection.toNodeId] ?: return@forEach

            if (
                from.type == SldNodeType.BREAKER &&
                to.type == SldNodeType.LOAD &&
                connection.connectionType == SldConnectionType.CABLE
            ) {
                val breakerPosition = positions[from.id]
                    ?: positions[representative(from.id)]
                    ?: return@forEach

                positions[to.id] = snap(
                    breakerPosition.first + COLUMN_GAP,
                    breakerPosition.second
                )
            }
        }

        /*
         * Place every remaining node. This also handles unusual or
         * incomplete diagrams without dropping equipment.
         */
        network.nodes.forEachIndexed { index, node ->
            if (node.id !in positions) {
                val rep = representative(node.id)
                val base = positions[rep]
                val row = index / 4
                val column = index % 4

                positions[node.id] = if (base != null && node.id == rep) {
                    base
                } else {
                    snap(
                        START_X + column * DISCONNECTED_GAP_X,
                        START_Y + (levels[rep] ?: 0) * LEVEL_GAP +
                            row * DISCONNECTED_GAP_Y
                    )
                }
            }
        }

        /*
         * Translate all coordinates so the drawing starts near the
         * canvas origin. This operation does not change relative
         * positions or topology.
         */
        val minX = positions.values.minOfOrNull { it.first } ?: START_X
        val minY = positions.values.minOfOrNull { it.second } ?: START_Y

        val offsetX = max(0f, START_X - minX)
        val offsetY = max(0f, START_Y - minY)

        val updatedNodes = network.nodes.map { node ->
            val point = positions[node.id] ?: (node.x to node.y)

            node.copy(
                x = snap(point.first + offsetX, GRID_SNAP),
                y = snap(point.second + offsetY, GRID_SNAP)
            )
        }

        return LayoutResult(
            network = network.copy(nodes = updatedNodes),
            levels = levels
        )
    }

    private fun buildPanelAssemblies(
        network: SldNetwork
    ): List<PanelAssembly> {
        val nodesById = network.nodes.associateBy { it.id }

        return network.nodes
            .filter { it.type == SldNodeType.PANEL }
            .map { panel ->
                val memberIds = linkedSetOf(panel.id)
                val busIds = linkedSetOf<String>()
                val breakerIds = linkedSetOf<String>()

                /*
                 * Expand only through valid internal BUSBAR edges.
                 * A cable feeder never becomes part of the enclosure.
                 */
                var changed = true

                while (changed) {
                    changed = false

                    network.connections
                        .filter {
                            it.connectionType == SldConnectionType.BUSBAR
                        }
                        .forEach { connection ->
                            val from = nodesById[connection.fromNodeId]
                                ?: return@forEach
                            val to = nodesById[connection.toNodeId]
                                ?: return@forEach

                            if (!isInternalAssemblyConnection(connection, from, to)) {
                                return@forEach
                            }

                            val fromInside = from.id in memberIds
                            val toInside = to.id in memberIds

                            if (fromInside && !toInside) {
                                val eligible =
                                    to.type == SldNodeType.BUS ||
                                        to.type == SldNodeType.BREAKER

                                if (eligible) {
                                    memberIds.add(to.id)
                                    changed = true
                                }
                            }

                            if (toInside && !fromInside) {
                                val eligible =
                                    from.type == SldNodeType.BUS ||
                                        from.type == SldNodeType.BREAKER

                                if (eligible) {
                                    memberIds.add(from.id)
                                    changed = true
                                }
                            }
                        }
                }

                memberIds.forEach { id ->
                    when (nodesById[id]?.type) {
                        SldNodeType.BUS -> busIds.add(id)
                        SldNodeType.BREAKER -> breakerIds.add(id)
                        else -> Unit
                    }
                }

                PanelAssembly(
                    panelId = panel.id,
                    busIds = busIds.sortedBy {
                        nodesById[it]?.name?.uppercase().orEmpty()
                    },
                    breakerIds = breakerIds.sortedBy {
                        nodesById[it]?.name?.uppercase().orEmpty()
                    }
                )
            }
    }

    private fun isInternalAssemblyConnection(
        connection: SldConnection,
        from: SldNode,
        to: SldNode
    ): Boolean {
        if (connection.connectionType != SldConnectionType.BUSBAR) {
            return false
        }

        val first = from.type
        val second = to.type

        return (
            first == SldNodeType.PANEL && second == SldNodeType.BUS
        ) || (
            first == SldNodeType.BUS && second == SldNodeType.PANEL
        ) || (
            first == SldNodeType.BUS && second == SldNodeType.BREAKER
        ) || (
            first == SldNodeType.BREAKER && second == SldNodeType.BUS
        ) || (
            first == SldNodeType.PANEL && second == SldNodeType.BREAKER
        ) || (
            first == SldNodeType.BREAKER && second == SldNodeType.PANEL
        )
    }

    private fun typeOrder(type: SldNodeType): Int {
        return when (type) {
            SldNodeType.SOURCE -> 0
            SldNodeType.GENERATOR -> 1
            SldNodeType.TRANSFORMER -> 2
            SldNodeType.BUS -> 3
            SldNodeType.PANEL -> 4
            SldNodeType.BREAKER -> 5
            SldNodeType.LOAD -> 6
        }
    }

    private fun rootPriority(type: SldNodeType?): Int {
        return when (type) {
            SldNodeType.SOURCE -> 0
            SldNodeType.GENERATOR -> 1
            SldNodeType.TRANSFORMER -> 2
            SldNodeType.BUS -> 3
            SldNodeType.PANEL -> 4
            SldNodeType.BREAKER -> 5
            SldNodeType.LOAD -> 6
            null -> 7
        }
    }

    private fun snap(value: Float): Float =
        snap(value, GRID_SNAP)

    private fun snap(value: Float, step: Float): Float {
        if (step <= 0f) return value
        return kotlin.math.round(value / step) * step
    }
}
