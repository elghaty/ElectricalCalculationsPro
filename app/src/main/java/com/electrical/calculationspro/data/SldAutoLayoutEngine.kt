package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD AUTO LAYOUT ENGINE
 * ================================================================
 *
 * Produces a readable engineering arrangement from the actual
 * electrical topology.
 *
 * It does NOT modify the engineering network.
 * It modifies drawing coordinates only.
 * ================================================================
 */
object SldAutoLayoutEngine {

    private const val START_X = 80f
    private const val START_Y = 100f

    private const val LEVEL_SPACING = 280f
    private const val ROW_SPACING = 190f

    data class LayoutResult(
        val network: SldNetwork,
        val levels: Map<String, Int>
    )

    fun arrange(
        network: SldNetwork
    ): LayoutResult {

        if (
            network.nodes.isEmpty()
        ) {
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

        network.connections.forEach {

            children[
                it.fromNodeId
            ]?.add(
                it.toNodeId
            )
        }

        val sources =
            network.nodes.filter {
                it.type ==
                    SldNodeType.SOURCE
            }

        val roots =
            if (
                sources.isNotEmpty()
            ) {
                sources
            } else {
                network.nodes.filter { node ->

                    network.connections.none {
                        it.toNodeId ==
                            node.id
                    }
                }
            }

        val levelMap =
            mutableMapOf<String, Int>()

        fun assignLevel(
            id: String,
            level: Int,
            path: MutableSet<String>
        ) {

            if (
                id in path
            ) {
                return
            }

            val previous =
                levelMap[id]

            if (
                previous == null ||
                level > previous
            ) {
                levelMap[id] = level
            }

            val nextPath =
                path +
                    id

            children[id]
                .orEmpty()
                .forEach { child ->

                    assignLevel(
                        id = child,
                        level = level + 1,
                        path = nextPath.toMutableSet()
                    )
                }
        }

        roots.forEach {
            assignLevel(
                id = it.id,
                level = 0,
                path = mutableSetOf()
            )
        }

        /*
         * Any disconnected equipment is placed after the
         * connected topology rather than being lost.
         */
        network.nodes
            .filter {
                it.id !in levelMap
            }
            .forEach { node ->

                val maximumLevel =
                    levelMap.values
                        .maxOrNull()
                        ?: 0

                levelMap[node.id] =
                    maximumLevel + 1
            }

        val grouped =
            network.nodes
                .groupBy {
                    levelMap[it.id]
                        ?: 0
                }

        val positions =
            mutableMapOf<String, Pair<Float, Float>>()

        grouped
            .toSortedMap()
            .forEach { (level, nodes) ->

                val totalHeight =
                    (
                        nodes.size - 1
                    ) * ROW_SPACING

                val startY =
                    START_Y -
                        totalHeight / 2f

                nodes
                    .sortedWith(
                        compareBy<SldNode> {
                            it.type.order()
                        }.thenBy {
                            it.name
                        }
                    )
                    .forEachIndexed {
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
                levelMap.toMap()
        )
    }

    private fun SldNodeType.order(): Int {

        return when (this) {

            SldNodeType.SOURCE ->
                0

            SldNodeType.TRANSFORMER ->
                1

            SldNodeType.GENERATOR ->
                1

            SldNodeType.BUS ->
                2

            SldNodeType.BREAKER ->
                3

            SldNodeType.PANEL ->
                4

            SldNodeType.LOAD ->
                5
        }
    }
}
