
package com.electrical.calculationspro.data

import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.round

/**
 * SLD topology-aware auto-layout.
 *
 * Responsibilities:
 * - Reposition nodes without modifying electrical connections.
 * - Preserve connection IDs, direction and engineering properties.
 * - Treat valid internal panel BUSBAR connections as assembly links.
 * - Keep external cable feeders outside the internal assembly.
 * - Keep disconnected and cyclic components visible.
 *
 * This class performs graphic layout only. It does not perform
 * electrical calculations or change network topology.
 */
object SldAutoLayoutEngine {

    private const val START_X = 120f
    private const val START_Y = 100f

    private const val COLUMN_GAP = 320f
    private const val ROW_GAP = 190f
    private const val ASSEMBLY_GAP = 260f
    private const val GRID_SNAP = 20f

    private const val INTERNAL_BUS_Y = 50f
    private const val INTERNAL_BREAKER_START_Y = 170f
    private const val INTERNAL_BREAKER_GAP_X = 200f

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
     * Calculates new positions while preserving the electrical model.
     */
    fun arrange(network: SldNetwork): LayoutResult {
        if (network.nodes.isEmpty()) {
            return LayoutResult(
                network = network,
                levels = emptyMap()
            )
        }

        val nodesById = network.nodes.associateBy { it.id }
        val assemblies = buildPanelAssemblies(network, nodesById)

        val nodeToPanel = mutableMapOf<String, String>()

        assemblies.forEach { assembly ->
            assembly.memberIds.forEach { memberId ->
                nodeToPanel.putIfAbsent(memberId, assembly.panelId)
            }
        }

        fun representative(nodeId: String): String =
            nodeToPanel[nodeId] ?: nodeId

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
         * Collapse each physical panel assembly into one representative
         * for the external graph. Internal busbar edges are not feeders.
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

        /*
         * Start with source nodes, then include all other graph roots.
         * A disconnected diagram remains editable and visible.
         */
        val sourceIds = network.nodes
            .filter { it.type == SldNodeType.SOURCE }
            .map { representative(it.id) }
            .distinct()
            .sortedBy { nodesById[it]?.name.orEmpty().uppercase() }

        val rootIds = representatives
            .filter { parents[it].orEmpty().isEmpty() }
            .sortedWith(
                compareBy<String> { rootPriority(nodesById[it]?.type) }
                    .thenBy { nodesById[it]?.name.orEmpty().uppercase() }
            )

        val roots = (sourceIds + rootIds).distinct()
        val levels = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        roots.forEach { id ->
            if (levels.putIfAbsent(id, 0) == null) {
                queue.addLast(id)
            }
        }

        /*
         * Breadth-first traversal avoids recursive stack overflow and
         * safely handles incomplete or cyclic editor diagrams.
         */
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val nextLevel = levels.getValue(current) + 1

            children[current].orEmpty()
                .sortedBy { nodesById[it]?.name.orEmpty().uppercase() }
                .forEach { childId ->
                    if (childId !in levels) {
                        levels[childId] = nextLevel
                        queue.addLast(childId)
                    }
                }
        }

        /*
         * Cycle-only components may have no root. Give every remaining
         * component a stable level instead of dropping its nodes.
         */
        var nextDisconnectedLevel =
            (levels.values.maxOrNull() ?: 0) + 1

        representatives
            .filter { it !in levels }
            .sortedBy { nodesById[it]?.name.orEmpty().uppercase() }
            .forEach { id ->
                levels[id] = nextDisconnectedLevel++
            }

        /*
         * Lay out the external graph from left to right:
         * upstream levels are columns; nodes at the same level are rows.
         */
        val positions = mutableMapOf<String, Pair<Float, Float>>()

        val orderedByLevel = representatives
            .groupBy { levels[it] ?: 0 }
            .toSortedMap()

        orderedByLevel.forEach { (level, ids) ->
            val orderedIds = ids.sortedWith(
                compareBy<String> { rootPriority(nodesById[it]?.type) }
                    .thenBy { nodesById[it]?.name.orEmpty().uppercase() }
            )

            orderedIds.forEachIndexed { row, id ->
                positions[id] = snapPoint(
                    START_X + level * COLUMN_GAP,
                    START_Y + row * ROW_GAP
                )
            }
        }

        /*
         * Prefer the source at the top of the first column.
         * Only coordinates change; electrical direction is untouched.
         */
        sourceIds.forEachIndexed { index, id ->
            positions[id] = snapPoint(
                START_X,
                START_Y + index * ROW_GAP
            )
        }

        /*
         * Place panel members relative to their enclosure anchor.
         * The drawing layer must use these positions consistently
         * with its enclosure and busbar geometry.
         */
        assemblies.forEach { assembly ->
            val anchor = positions[assembly.panelId]
                ?: (START_X to START_Y)

            val panelX = anchor.first
            val panelY = anchor.second

            positions[assembly.panelId] = snapPoint(panelX, panelY)

            assembly.busIds.forEachIndexed { index, busId ->
                positions[busId] = snapPoint(
                    panelX + 40f,
                    panelY + INTERNAL_BUS_Y + index * 24f
                )
            }

            /*
             * Breakers receive stable, non-overlapping logical positions.
             * Their graphical renderer must draw them inside the same
             * physical enclosure rather than as independent panel boxes.
             */
            assembly.breakerIds.forEachIndexed { index, breakerId ->
                positions[breakerId] = snapPoint(
                    panelX + index * INTERNAL_BREAKER_GAP_X,
                    panelY + INTERNAL_BREAKER_START_Y
                )
            }
        }

        /*
         * Place direct downstream loads outside the assembly.
         * Connections and their direction are not rewritten.
         */
        network.connections.forEach { connection ->
            val from = nodesById[connection.fromNodeId]
                ?: return@forEach
            val to = nodesById[connection.toNodeId]
                ?: return@forEach

            if (
                from.type == SldNodeType.BREAKER &&
                to.type == SldNodeType.LOAD &&
                connection.connectionType == SldConnectionType.CABLE
            ) {
                val breakerPosition = positions[from.id]
                    ?: positions[representative(from.id)]
                    ?: return@forEach

                positions[to.id] = snapPoint(
                    breakerPosition.first + COLUMN_GAP,
                    breakerPosition.second
                )
            }
        }

        /*
         * Ensure every node receives a coordinate, including nodes that
         * belong to unusual or incomplete topologies.
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
                        START_X + (index % 4) * ASSEMBLY_GAP,
                        START_Y + (index / 4) * ROW_GAP
                    )
                }
            }
        }

        /*
         * Translate negative coordinates into the visible canvas area.
         */
        val minX = positions.values.minOfOrNull { it.first } ?: START_X
        val minY = positions.values.minOfOrNull { it.second } ?: START_Y

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
         * Return the original connection list unchanged.
         */
        return LayoutResult(
            network = network.copy(nodes = updatedNodes),
            levels = levels
        )
    }

    /**
     * Finds BUS and BREAKER nodes connected to a PANEL exclusively
     * through valid internal BUSBAR relationships.
     */
    private fun buildPanelAssemblies(
        network: SldNetwork,
        nodesById: Map<String, SldNode>
    ): List<PanelAssembly> {
        val panels = network.nodes.filter {
            it.type == SldNodeType.PANEL
        }

        val claimedMembers = mutableSetOf<String>()

        return panels.map { panel ->
            val memberIds = linkedSetOf(panel.id)
            var changed = true

            while (changed) {
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

                    if (!isInternalAssemblyConnection(connection, from, to)) {
                        return@forEach
                    }

                    val fromInside = from.id in memberIds
                    val toInside = to.id in memberIds

                    if (fromInside && to.type in INTERNAL_MEMBER_TYPES) {
                        if (to.id !in claimedMembers || to.id == panel.id) {
                            if (memberIds.add(to.id)) changed = true
                        }
                    }

                    if (toInside && from.type in INTERNAL_MEMBER_TYPES) {
                        if (from.id !in claimedMembers || from.id == panel.id) {
                            if (memberIds.add(from.id)) changed = true
                        }
                    }
                }
            }

            val busIds = memberIds
                .filter { nodesById[it]?.type == SldNodeType.BUS }
                .sortedBy { nodesById[it]?.name.orEmpty().uppercase() }

            val breakerIds = memberIds
                .filter { nodesById[it]?.type == SldNodeType.BREAKER }
                .sortedBy { nodesById[it]?.name.orEmpty().uppercase() }

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
        if (connection.connectionType != SldConnectionType.BUSBAR) {
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
