package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD AUTO LAYOUT ENGINE
 * ================================================================
 *
 * Layout is derived from electrical topology.
 *
 * Normal engineering calculations MUST NOT call this engine.
 *
 * Auto Arrange is an explicit drawing operation.
 *
 * Topology direction:
 *
 * SOURCE
 *   ↓
 * TRANSFORMER / GENERATOR
 *   ↓
 * BREAKER
 *   ↓
 * BUS
 *   ↓
 * PANEL
 *   ↓
 * FEEDER BREAKER
 *   ↓
 * LOAD
 *
 * The engine changes drawing coordinates only.
 * It does not change engineering connections or calculations.
 * ================================================================
 */
object SldAutoLayoutEngine {

    private const val START_X = 100f
    private const val START_Y = 160f

    private const val LEVEL_SPACING = 260f
    private const val ROW_SPACING = 180f

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

        network.nodes.forEach {
            children[it.id] =
                mutableListOf()
        }

        network.connections.forEach { connection ->

            if (
                nodeMap.containsKey(
                    connection.fromNodeId
                ) &&
                nodeMap.containsKey(
                    connection.toNodeId
                )
            ) {

                children[
                    connection.fromNodeId
                ]?.add(
                    connection.toNodeId
                )
            }
        }

        /*
         * --------------------------------------------------------
         * ROOTS
         * --------------------------------------------------------
         *
         * Prefer actual electrical sources.
         */
        val sourceRoots =
            network.nodes.filter {
                it.type ==
                    SldNodeType.SOURCE
            }

        val roots =
            if (sourceRoots.isNotEmpty()) {

                sourceRoots

            } else {

                network.nodes.filter { node ->

                    network.connections.none {
                        it.toNodeId ==
                            node.id
                    }
                }
            }

        /*
         * --------------------------------------------------------
         * LEVEL CALCULATION
         * --------------------------------------------------------
         */
        val levels =
            mutableMapOf<String, Int>()

        fun visit(
            id: String,
            level: Int,
            path: MutableSet<String>
        ) {

            if (id in path) {
                return
            }

            val old =
                levels[id]

            if (
                old == null ||
                level > old
            ) {

                levels[id] =
                    level
            }

            val nextPath =
                path.toMutableSet()

            nextPath += id

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
                id = root.id,
                level = 0,
                path = mutableSetOf()
            )
        }

        /*
         * Disconnected elements are not deleted.
         * They are placed after the connected topology.
         */
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
                    typeOrder(it.type)
                }.thenBy {
                    it.name
                }
            )
            .forEach { node ->

                levels[node.id] =
                    disconnectedLevel

                disconnectedLevel++
            }

        /*
         * --------------------------------------------------------
         * GROUP BY ELECTRICAL LEVEL
         * --------------------------------------------------------
         */
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

                /*
                 * Keep a predictable engineering ordering
                 * inside every level.
                 */
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

                val totalHeight =
                    (
                        ordered.size - 1
                    ) *
                        ROW_SPACING

                val startY =
                    START_Y -
                        totalHeight / 2f

                ordered.forEachIndexed {
                        index,
                        node
                    ->

                    positions[node.id] =
                        Pair(
                            START_X +
                                level *
                                LEVEL_SPACING,
                            startY +
                                index *
                                ROW_SPACING
                        )
                }
            }

        /*
         * --------------------------------------------------------
         * ALIGN FEEDER CHAINS
         * --------------------------------------------------------
         *
         * When a breaker feeds one load, keep them visually close
         * to the same horizontal feeder line.
         */
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

        val arrangedNodes =
            network.nodes.map { node ->

                val position =
                    positions[node.id]

                if (position == null) {

                    node

                } else {

                    node.copy(
                        x = position.first,
                        y = position.second
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

            SldNodeType.TRANSFORMER ->
                1

            SldNodeType.GENERATOR ->
                1

            SldNodeType.BREAKER ->
                2

            SldNodeType.BUS ->
                3

            SldNodeType.PANEL ->
                4

            SldNodeType.LOAD ->
                5
        }
    }
}
