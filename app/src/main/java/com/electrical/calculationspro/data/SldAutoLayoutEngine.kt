package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD AUTO LAYOUT ENGINE
 * ================================================================
 *
 * Drawing-only engine.
 *
 * It changes coordinates only.
 * It does NOT perform engineering calculations.
 * It does NOT change topology.
 *
 * Intended electrical presentation:
 *
 *                         SOURCE
 *                            │
 *                       MAIN BREAKER
 *                            │
 *                        TRANSFORMER
 *                            │
 *                           BUS
 *                            │
 *                          PANEL
 *                      ┌─────┼─────┐
 *                      │     │     │
 *                   FEEDER FEEDER FEEDER
 *                      │     │     │
 *                    LOAD  LOAD  LOAD
 *
 * Main electrical path is vertical.
 * Branches are distributed horizontally.
 * ================================================================
 */
object SldAutoLayoutEngine {

    private const val CENTER_X = 700f
    private const val START_Y = 80f

    /**
     * Vertical distance between electrical levels.
     */
    private const val LEVEL_SPACING = 220f

    /**
     * Horizontal distance between parallel branches.
     */
    private const val ROW_SPACING = 240f

    data class LayoutResult(
        val network: SldNetwork,
        val levels: Map<String, Int>
    )

    fun arrange(
        network: SldNetwork
    ): LayoutResult {

        if (network.nodes.isEmpty()) {

            return LayoutResult(
                network = network,
                levels = emptyMap()
            )
        }

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val children =
            mutableMapOf<
                String,
                MutableList<String>
            >()

        network.nodes.forEach { node ->

            children[node.id] =
                mutableListOf()
        }

        network.connections.forEach { connection ->

            val fromExists =
                nodeMap.containsKey(
                    connection.fromNodeId
                )

            val toExists =
                nodeMap.containsKey(
                    connection.toNodeId
                )

            if (
                fromExists &&
                toExists
            ) {

                children[
                    connection.fromNodeId
                ]?.add(
                    connection.toNodeId
                )
            }
        }

        // =========================================================
        // ROOTS
        // =========================================================

        val sourceRoots =
            network.nodes.filter {

                it.type ==
                    SldNodeType.SOURCE
            }

        val roots =
            if (
                sourceRoots.isNotEmpty()
            ) {

                sourceRoots

            } else {

                network.nodes.filter { node ->

                    network.connections.none {

                        it.toNodeId ==
                            node.id
                    }
                }
            }

        // =========================================================
        // LEVEL CALCULATION
        // =========================================================

        val levels =
            mutableMapOf<String, Int>()

        fun visit(
            id: String,
            level: Int,
            path: Set<String>
        ) {

            if (
                id in path
            ) {
                return
            }

            val previous =
                levels[id]

            /*
             * If a node can be reached through more than one
             * upstream path, keep the deepest electrical level.
             */
            if (
                previous == null ||
                level > previous
            ) {

                levels[id] =
                    level
            }

            val nextPath =
                path + id

            children[id]
                .orEmpty()
                .forEach { child ->

                    visit(
                        id = child,
                        level = level + 1,
                        path = nextPath
                    )
                }
        }

        roots.forEach { root ->

            visit(
                id =
                    root.id,

                level =
                    0,

                path =
                    emptySet()
            )
        }

        // =========================================================
        // DISCONNECTED ELEMENTS
        // =========================================================

        var disconnectedLevel =
            (
                levels.values.maxOrNull()
                    ?: 0
                ) + 1

        network.nodes
            .filter {
                it.id !in levels
            }
            .sortedWith(
                compareBy<SldNode> {

                    typeOrder(
                        it.type
                    )

                }.thenBy {

                    it.name
                }
            )
            .forEach { node ->

                levels[node.id] =
                    disconnectedLevel

                disconnectedLevel++
            }

        // =========================================================
        // GROUP BY ELECTRICAL LEVEL
        // =========================================================

        val grouped =
            network.nodes
                .groupBy {

                    levels[it.id]
                        ?: 0
                }

        val positions =
            mutableMapOf<
                String,
                Pair<Float, Float>
            >()

        grouped
            .toSortedMap()
            .forEach { (level, nodesAtLevel) ->

                val ordered =
                    nodesAtLevel.sortedWith(

                        compareBy<SldNode> {

                            typeOrder(
                                it.type
                            )

                        }.thenBy {

                            it.name
                        }
                    )

                /*
                 * Branches spread horizontally around the
                 * central electrical axis.
                 */
                val totalWidth =
                    (
                        ordered.size - 1
                    ) *
                        ROW_SPACING

                val startX =
                    CENTER_X -
                        totalWidth / 2f

                ordered.forEachIndexed {
                        index,
                        node
                    ->

                    positions[node.id] =
                        Pair(

                            startX +
                                index *
                                ROW_SPACING,

                            START_Y +
                                level *
                                LEVEL_SPACING
                        )
                }
            }

        // =========================================================
        // PRIMARY PATH ALIGNMENT
        // =========================================================
        //
        // For nodes with one child, preserve a central path.
        // Branching nodes remain distributed.
        // =========================================================

        fun singleChild(
            nodeId: String
        ): String? {

            val list =
                children[nodeId]
                    .orEmpty()

            return if (
                list.size == 1
            ) {
                list.first()
            } else {
                null
            }
        }

        roots.forEach { root ->

            var current =
                root.id

            while (true) {

                val child =
                    singleChild(
                        current
                    )
                        ?: break

                val currentPosition =
                    positions[current]

                val childPosition =
                    positions[child]

                if (
                    currentPosition != null &&
                    childPosition != null
                ) {

                    positions[child] =
                        Pair(
                            currentPosition.first,
                            childPosition.second
                        )
                }

                current =
                    child
            }
        }

        // =========================================================
        // FEEDER BRANCH ALIGNMENT
        // =========================================================
        //
        // A direct breaker -> load feeder should be horizontal.
        // =========================================================

        network.connections.forEach { connection ->

            val from =
                nodeMap[
                    connection.fromNodeId
                ]

            val to =
                nodeMap[
                    connection.toNodeId
                ]

            if (
                from != null &&
                to != null
            ) {

                val fromPosition =
                    positions[from.id]

                val toPosition =
                    positions[to.id]

                if (
                    fromPosition != null &&
                    toPosition != null &&
                    from.type ==
                        SldNodeType.BREAKER &&
                    to.type ==
                        SldNodeType.LOAD
                ) {

                    positions[to.id] =
                        Pair(
                            toPosition.first,
                            fromPosition.second
                        )
                }
            }
        }

        // =========================================================
        // FINAL NODE UPDATE
        // =========================================================

        val arrangedNodes =
            network.nodes.map { node ->

                val position =
                    positions[node.id]

                if (
                    position == null
                ) {

                    node

                } else {

                    node.copy(

                        x =
                            position.first,

                        y =
                            position.second
                    )
                }
            }

        return LayoutResult(

            network =
                network.copy(
                    nodes =
                        arrangedNodes
                ),

            levels =
                levels.toMap()
        )
    }

    private fun typeOrder(
        type: SldNodeType
    ): Int {

        return when (type) {

            SldNodeType.SOURCE ->
                0

            SldNodeType.GENERATOR ->
                1

            SldNodeType.TRANSFORMER ->
                2

            SldNodeType.BREAKER ->
                3

            SldNodeType.BUS ->
                4

            SldNodeType.PANEL ->
                5

            SldNodeType.LOAD ->
                6
        }
    }
}
