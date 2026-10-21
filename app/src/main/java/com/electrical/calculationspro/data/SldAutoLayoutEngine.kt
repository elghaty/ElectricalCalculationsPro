
package com.electrical.calculationspro.data

import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.round

/**
 * Topology-aware Single Line Diagram layout engine.
 *
 * Responsibilities:
 * - Arrange equipment according to directed electrical connections.
 * - Keep panel assemblies grouped.
 * - Preserve connection direction, IDs and engineering properties.
 * - Keep disconnected and incomplete equipment visible.
 * - Avoid recursive traversal of cyclic networks.
 * - Produce deterministic positions for repeatable layouts.
 *
 * This class changes node coordinates only.
 * It does not create, delete, or modify electrical connections.
 */
object SldAutoLayoutEngine {

    private const val START_X = 120f
    private const val START_Y = 100f

    private const val COLUMN_GAP = 360f
    private const val ROW_GAP = 220f

    private const val PANEL_ASSEMBLY_MIN_WIDTH = 240f
    private const val PANEL_ASSEMBLY_MIN_HEIGHT = 190f

    private const val BUS_OFFSET_X = 40f
    private const val BUS_OFFSET_Y = 75f

    private const val BREAKER_START_X = 0f
    private const val BREAKER_GAP_X = 200f
    private const val BREAKER_OFFSET_Y = 170f

    private const val LOAD_OFFSET_X = 300f
    private const val LOAD_GAP_Y = 120f

    private const val GRID_SNAP = 20f
    private const val MIN_COORDINATE = 20f

    private const val MAX_LAYOUT_PASSES = 2

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
            get() = (
                listOf(panelId) +
                    busIds +
                    breakerIds
                ).toSet()
    }

    private data class DirectedGraph(
        val representatives: List<String>,
        val children: Map<String, Set<String>>,
        val parents: Map<String, Set<String>>
    )

    private data class LayoutBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    /**
     * Entry point used by the SLD editor.
     *
     * All electrical data remains unchanged. Only node.x and
     * node.y are replaced in the returned network.
     */
    fun arrange(network: SldNetwork): LayoutResult {
        if (network.nodes.isEmpty()) {
            return LayoutResult(
                network = network,
                levels = emptyMap()
            )
        }

        val nodesById = network.nodes.associateBy { it.id }

        val assemblies = buildPanelAssemblies(
            network = network,
            nodesById = nodesById
        )

        val nodeToPanel = buildNodeToPanelMap(assemblies)

        fun representative(nodeId: String): String =
            nodeToPanel[nodeId] ?: nodeId

        val representatives = network.nodes
            .map { representative(it.id) }
            .distinct()

        val graph = buildDirectedGraph(
            network = network,
            nodesById = nodesById,
            representatives = representatives,
            representativeOf = ::representative
        )

        val levels = calculateLevels(
            network = network,
            nodesById = nodesById,
            graph = graph,
            representativeOf = ::representative
        )

        val positions = calculateInitialPositions(
            graph = graph,
            levels = levels,
            nodesById = nodesById
        )

        /*
         * A panel and its internal busbars and breakers are
         * arranged as one graphical assembly.
         */
        positionPanelAssemblies(
            assemblies = assemblies,
            positions = positions,
            nodesById = nodesById
        )

        /*
         * Reposition downstream loads using the actual directed
         * feeder connections. The electrical connections themselves
         * are never reversed or reconstructed.
         */
        positionDownstreamLoads(
            network = network,
            nodesById = nodesById,
            positions = positions
        )

        /*
         * Make sure all nodes have valid coordinates, including
         * isolated nodes and nodes belonging to unusual networks.
         */
        addFallbackPositions(
            network = network,
            positions = positions,
            representativeOf = ::representative
        )

        /*
         * Resolve coincident positions for independent nodes.
         * Panel assembly members retain their deliberate relative
         * positions.
         */
        repeat(MAX_LAYOUT_PASSES) {
            separateUnassignedOverlaps(
                network = network,
                assemblies = assemblies,
                positions = positions,
                representativeOf = ::representative
            )
        }

        /*
         * Normalize coordinates while preserving relative placement.
         */
        val normalizedPositions = normalizePositions(positions)

        val updatedNodes = network.nodes.map { node ->
            val position = normalizedPositions[node.id]
                ?: (node.x to node.y)

            node.copy(
                x = snap(position.first),
                y = snap(position.second)
            )
        }

        return LayoutResult(
            network = network.copy(nodes = updatedNodes),
            levels = levels
        )
    }

    /**
     * Maps every internal panel member to its owning panel.
     * The first panel claiming a member retains ownership.
     */
    private fun buildNodeToPanelMap(
        assemblies: List<PanelAssembly>
    ): Map<String, String> {
        val result = linkedMapOf<String, String>()

        assemblies.forEach { assembly ->
            assembly.memberIds.forEach { memberId ->
                result.putIfAbsent(
                    memberId,
                    assembly.panelId
                )
            }
        }

        return result
    }

    /**
     * Builds the external directed graph.
     *
     * BUSBAR connections belonging to panel assemblies are
     * internal physical connections, not upstream feeders.
     *
     * CABLE connections retain their from-to direction.
     */
    private fun buildDirectedGraph(
        network: SldNetwork,
        nodesById: Map<String, SldNode>,
        representatives: List<String>,
        representativeOf: (String) -> String
    ): DirectedGraph {
        val children = representatives.associateWith {
            linkedSetOf<String>()
        }.toMutableMap()

        val parents = representatives.associateWith {
            linkedSetOf<String>()
        }.toMutableMap()

        network.connections.forEach { connection ->
            val from = nodesById[connection.fromNodeId]
                ?: return@forEach

            val to = nodesById[connection.toNodeId]
                ?: return@forEach

            if (from.id == to.id) {
                return@forEach
            }

            if (
                isInternalAssemblyConnection(
                    connection = connection,
                    from = from,
                    to = to
                )
            ) {
                return@forEach
            }

            val fromId = representativeOf(from.id)
            val toId = representativeOf(to.id)

            if (fromId == toId) {
                return@forEach
            }

            children.getValue(fromId).add(toId)
            parents.getValue(toId).add(fromId)
        }

        return DirectedGraph(
            representatives = representatives,
            children = children.mapValues {
                it.value.toSet()
            },
            parents = parents.mapValues {
                it.value.toSet()
            }
        )
    }

    /**
     * Calculates graph levels using breadth-first traversal.
     *
     * Sources are preferred as roots. Other parentless equipment
     * is also included so incomplete drawings remain visible.
     * Cycles and disconnected components are handled without
     * recursive traversal.
     */
    private fun calculateLevels(
        network: SldNetwork,
        nodesById: Map<String, SldNode>,
        graph: DirectedGraph,
        representativeOf: (String) -> String
    ): Map<String, Int> {
        val levels = linkedMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        val sourceIds = network.nodes
            .filter { it.type == SldNodeType.SOURCE }
            .map { representativeOf(it.id) }
            .distinct()
            .sortedBy {
                nodesById[it]?.name.orEmpty().uppercase()
            }

        val parentlessIds = graph.representatives
            .filter {
                graph.parents[it].orEmpty().isEmpty()
            }
            .sortedWith(
                compareBy<String> {
                    rootPriority(nodesById[it]?.type)
                }.thenBy {
                    nodesById[it]?.name.orEmpty().uppercase()
                }
            )

        val roots = (sourceIds + parentlessIds).distinct()

        roots.forEach { id ->
            if (levels.putIfAbsent(id, 0) == null) {
                queue.addLast(id)
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val nextLevel = levels.getValue(current) + 1

            graph.children[current]
                .orEmpty()
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
         * Assign one common starting level to each remaining
         * disconnected group instead of placing every node in
         * a separate, unnecessarily distant column.
         */
        var fallbackLevel = (levels.values.maxOrNull() ?: 0) + 1

        val unvisited = graph.representatives
            .filter { it !in levels }
            .toMutableSet()

        while (unvisited.isNotEmpty()) {
            val componentStart = unvisited
                .minWithOrNull(
                    compareBy<String> {
                        rootPriority(nodesById[it]?.type)
                    }.thenBy {
                        nodesById[it]?.name.orEmpty().uppercase()
                    }
                ) ?: break

            levels[componentStart] = fallbackLevel
            queue.addLast(componentStart)
            unvisited.remove(componentStart)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val nextLevel = levels.getValue(current) + 1

                graph.children[current]
                    .orEmpty()
                    .sortedBy {
                        nodesById[it]?.name.orEmpty().uppercase()
                    }
                    .forEach { childId ->
                        if (childId in unvisited) {
                            levels[childId] = nextLevel
                            unvisited.remove(childId)
                            queue.addLast(childId)
                        }
                    }

                graph.parents[current]
                    .orEmpty()
                    .sortedBy {
                        nodesById[it]?.name.orEmpty().uppercase()
                    }
                    .forEach { parentId ->
                        if (parentId in unvisited) {
                            levels[parentId] = nextLevel
                            unvisited.remove(parentId)
                            queue.addLast(parentId)
                        }
                    }
            }

            fallbackLevel =
                (levels.values.maxOrNull() ?: fallbackLevel) + 1
        }

        return levels
    }

    /**
     * Assigns initial coordinates to external graph representatives.
     * Nodes at the same graph level are separated vertically.
     */
    private fun calculateInitialPositions(
        graph: DirectedGraph,
        levels: Map<String, Int>,
        nodesById: Map<String, SldNode>
    ): MutableMap<String, Pair<Float, Float>> {
        val positions = linkedMapOf<String, Pair<Float, Float>>()

        graph.representatives
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
         * Explicitly keep utility sources at the left edge.
         */
        graph.representatives
            .filter {
                nodesById[it]?.type == SldNodeType.SOURCE
            }
            .sortedBy {
                nodesById[it]?.name.orEmpty().uppercase()
            }
            .forEachIndexed { index, sourceId ->
                positions[sourceId] = snapPoint(
                    START_X,
                    START_Y + index * ROW_GAP
                )
            }

        return positions
    }

    /**
     * Positions panel, busbar and breaker nodes relative to the
     * panel anchor. Each assembly keeps its internal geometry.
     */
    private fun positionPanelAssemblies(
        assemblies: List<PanelAssembly>,
        positions: MutableMap<String, Pair<Float, Float>>,
        nodesById: Map<String, SldNode>
    ) {
        assemblies.forEach { assembly ->
            val panel = nodesById[assembly.panelId]
                ?: return@forEach

            val anchor = positions[assembly.panelId]
                ?: (START_X to START_Y)

            val panelX = anchor.first
            val panelY = anchor.second

            positions[assembly.panelId] = snapPoint(
                panelX,
                panelY
            )

            assembly.busIds.forEachIndexed { index, busId ->
                positions[busId] = snapPoint(
                    panelX + BUS_OFFSET_X,
                    panelY + BUS_OFFSET_Y + index * 24f
                )
            }

            assembly.breakerIds.forEachIndexed { index, breakerId ->
                positions[breakerId] = snapPoint(
                    panelX + BREAKER_START_X +
                        index * BREAKER_GAP_X,
                    panelY + BREAKER_OFFSET_Y
                )
            }

            /*
             * Ensure that the assembly's anchor remains valid
             * even if the panel has no internal components.
             */
            if (assembly.memberIds.isEmpty()) {
                positions[panel.id] = anchor
            }
        }
    }

    /**
     * Places loads connected downstream of breakers.
     *
     * The position follows the actual directed connection:
     * BREAKER -> LOAD.
     *
     * Other loads keep their graph-derived positions.
     */
    private fun positionDownstreamLoads(
        network: SldNetwork,
        nodesById: Map<String, SldNode>,
        positions: MutableMap<String, Pair<Float, Float>>
    ) {
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

        loadsByBreaker.forEach { (breakerId, rawLoadIds) ->
            val breakerPosition = positions[breakerId]
                ?: return@forEach

            rawLoadIds.distinct()
                .sortedBy {
                    nodesById[it]?.name.orEmpty().uppercase()
                }
                .forEachIndexed { index, loadId ->
                    positions[loadId] = snapPoint(
                        breakerPosition.first + LOAD_OFFSET_X,
                        breakerPosition.second +
                            index * LOAD_GAP_Y
                    )
                }
        }
    }

    /**
     * Supplies fallback coordinates for any node not yet positioned.
     */
    private fun addFallbackPositions(
        network: SldNetwork,
        positions: MutableMap<String, Pair<Float, Float>>,
        representativeOf: (String) -> String
    ) {
        network.nodes.forEachIndexed { index, node ->
            if (node.id in positions) {
                return@forEachIndexed
            }

            val representativeId = representativeOf(node.id)
            val representativePosition = positions[representativeId]

            positions[node.id] = if (
                node.id == representativeId &&
                representativePosition != null
            ) {
                representativePosition
            } else {
                snapPoint(
                    START_X + (index % 4) * COLUMN_GAP,
                    START_Y + (index / 4) * ROW_GAP
                )
            }
        }
    }

    /**
     * Separates nodes that occupy exactly the same coordinate,
     * without disturbing deliberate panel assembly positions.
     */
    private fun separateUnassignedOverlaps(
        network: SldNetwork,
        assemblies: List<PanelAssembly>,
        positions: MutableMap<String, Pair<Float, Float>>,
        representativeOf: (String) -> String
    ) {
        val assemblyMembers = assemblies
            .flatMap { it.memberIds }
            .toSet()

        val occupied = mutableMapOf<Pair<Int, Int>, String>()

        network.nodes.forEachIndexed { index, node ->
            val point = positions[node.id]
                ?: return@forEachIndexed

            val key = coordinateKey(point)

            if (key !in occupied) {
                occupied[key] = node.id
                return@forEachIndexed
            }

            /*
             * Internal panel members intentionally share logical
             * assembly geometry; do not move them independently.
             */
            if (node.id in assemblyMembers) {
                return@forEachIndexed
            }

            val representativeId = representativeOf(node.id)
            var candidate = point
            var attempt = 1

            while (coordinateKey(candidate) in occupied) {
                candidate = snapPoint(
                    point.first + (attempt % 3) * COLUMN_GAP,
                    point.second + attempt * ROW_GAP
                )
                attempt++
            }

            positions[node.id] = candidate
            occupied[coordinateKey(candidate)] = node.id
        }
    }

    /**
     * Normalizes coordinates so all nodes remain in positive space.
     */
    private fun normalizePositions(
        positions: Map<String, Pair<Float, Float>>
    ): Map<String, Pair<Float, Float>> {
        if (positions.isEmpty()) return positions

        val minX = positions.values.minOf { it.first }
        val minY = positions.values.minOf { it.second }

        val offsetX = max(0f, START_X - minX)
        val offsetY = max(0f, START_Y - minY)

        return positions.mapValues { (_, point) ->
            snapPoint(
                point.first + offsetX,
                point.second + offsetY
            )
        }
    }

    private fun coordinateKey(
        point: Pair<Float, Float>
    ): Pair<Int, Int> =
        point.first.toInt() to point.second.toInt()

    /**
     * Determines whether a connection is an internal panel link.
     */
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

    /**
     * Builds connected internal BUSBAR assemblies for every panel.
     */
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
                            connection = connection,
                            from = from,
                            to = to
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
                        if (members.add(to.id)) {
                            changed = true
                        }
                    }

                    if (
                        toInside &&
                        from.type in INTERNAL_MEMBER_TYPES &&
                        (
                            from.id !in claimedMembers ||
                                from.id == panel.id
                            )
                    ) {
                        if (members.add(from.id)) {
                            changed = true
                        }
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

    private fun rootPriority(
        type: SldNodeType?
    ): Int =
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
