package com.electrical.calculationspro.data

/**
 * Topology-aware SLD presentation layout.
 *
 * This class changes coordinates only.
 * No electrical calculation is performed here.
 */
object SldAutoLayoutEngine {

    private const val START_X = 620f
    private const val START_Y = 70f

    private const val LEVEL_Y = 190f
    private const val BRANCH_X = 250f

    private const val NODE_WIDTH = 180f
    private const val NODE_HEIGHT = 118f

    private const val CLEARANCE = 60f

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

        val nodes =
            network.nodes.associateBy {
                it.id
            }

        val children =
            network.nodes
                .associate {
                    it.id to
                        mutableListOf<String>()
                }
                .toMutableMap()

        val incoming =
            mutableSetOf<String>()

        network.connections.forEach { connection ->

            if (
                nodes.containsKey(
                    connection.fromNodeId
                ) &&
                nodes.containsKey(
                    connection.toNodeId
                )
            ) {

                children
                    .getValue(
                        connection.fromNodeId
                    )
                    .add(
                        connection.toNodeId
                    )

                incoming.add(
                    connection.toNodeId
                )
            }
        }

        val sourceRoots =
            network.nodes.filter {
                it.type ==
                    SldNodeType.SOURCE
            }

        val roots =
            sourceRoots.ifEmpty {

                network.nodes.filter {
                    it.id !in incoming
                }

            }.ifEmpty {

                listOf(
                    network.nodes.first()
                )
            }

        val levels =
            linkedMapOf<String, Int>()

        fun visit(
            id: String,
            level: Int,
            path: Set<String>
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
                levels[id] = level
            }

            children[id]
                .orEmpty()
                .forEach { child ->

                    visit(
                        id = child,
                        level = level + 1,
                        path = path + id
                    )
                }
        }

        roots.forEach { root ->

            visit(
                id = root.id,
                level = 0,
                path = emptySet()
            )
        }

        var disconnectedLevel =
            (
                levels.values.maxOrNull()
                    ?: 0
                ) + 1

        network.nodes
            .filter {
                it.id !in levels
            }
            .forEach { node ->

                levels[node.id] =
                    disconnectedLevel++

            }

        val positions =
            mutableMapOf<
                String,
                Pair<Float, Float>
            >()

        /*
         * Main trunk:
         *
         * Source -> breaker -> transformer -> bus -> panel
         *
         * remains on the center axis whenever topology permits.
         */
        roots.forEach { root ->

            var current =
                root.id

            val visited =
                mutableSetOf<String>()

            while (
                visited.add(current)
            ) {

                val level =
                    levels[current]
                        ?: 0

                positions[current] =
                    START_X to
                        (
                            START_Y +
                                level *
                                LEVEL_Y
                            )

                val next =
                    children[current]
                        .orEmpty()
                        .filter {
                            it !in visited
                        }
                        .sortedWith(
                            compareBy<String> {
                                typeOrder(
                                    nodes[it]?.type
                                )
                            }.thenBy {
                                nodes[it]?.name
                                    ?: ""
                            }
                        )
                        .singleOrNull()

                if (next == null) {
                    break
                }

                current = next
            }
        }

        /*
         * Other nodes are distributed around the center axis.
         */
        levels.entries
            .groupBy(
                keySelector = {
                    it.value
                },
                valueTransform = {
                    it.key
                }
            )
            .toSortedMap()
            .forEach { (level, ids) ->

                val unplaced =
                    ids
                        .filter {
                            it !in positions
                        }
                        .sortedWith(
                            compareBy<String> {
                                typeOrder(
                                    nodes[it]?.type
                                )
                            }.thenBy {
                                nodes[it]?.name
                                    ?: ""
                            }
                        )

                unplaced.forEachIndexed {
                    index,
                    id
                    ->

                    val side =
                        if (
                            index % 2 == 0
                        ) {
                            1
                        } else {
                            -1
                        }

                    val slot =
                        (index + 1) / 2

                    positions[id] =
                        (
                            START_X +
                                side *
                                slot *
                                BRANCH_X
                            ) to
                            (
                                START_Y +
                                    level *
                                    LEVEL_Y
                                )
                }
            }

        /*
         * Breaker -> Load is presented as a horizontal feeder.
         */
        network.connections.forEach {
            connection ->

            val from =
                nodes[
                    connection.fromNodeId
                ]

            val to =
                nodes[
                    connection.toNodeId
                ]

            if (
                from != null &&
                to != null &&
                from.type ==
                    SldNodeType.BREAKER &&
                to.type ==
                    SldNodeType.LOAD
            ) {

                val fromPosition =
                    positions[from.id]

                if (
                    fromPosition != null
                ) {

                    positions[to.id] =
                        (
                            fromPosition.first +
                                BRANCH_X / 1.7f
                            ) to
                            fromPosition.second
                }
            }
        }

        /*
         * Resolve collisions without changing topology.
         */
        val finalPositions =
            mutableMapOf<
                String,
                Pair<Float, Float>
            >()

        positions.forEach { (id, original) ->

            var candidate =
                original

            var guard = 0

            while (
                finalPositions.values.any {
                    overlaps(
                        candidate,
                        it
                    )
                } &&
                guard < 50
            ) {

                candidate =
                    candidate.first +
                        90f to
                        candidate.second +
                        LEVEL_Y / 2f

                guard++
            }

            finalPositions[id] =
                candidate
        }

        val arrangedNodes =
            network.nodes.map { node ->

                val position =
                    finalPositions[node.id]

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

    private fun overlaps(
        a: Pair<Float, Float>,
        b: Pair<Float, Float>
    ): Boolean {

        val horizontal =
            a.first <
                b.first +
                NODE_WIDTH +
                CLEARANCE &&
                a.first +
                NODE_WIDTH +
                CLEARANCE >
                b.first

        val vertical =
            a.second <
                b.second +
                NODE_HEIGHT +
                CLEARANCE &&
                a.second +
                NODE_HEIGHT +
                CLEARANCE >
                b.second

        return horizontal && vertical
    }

    private fun typeOrder(
        type: SldNodeType?
    ): Int {

        return when (type) {

            SldNodeType.SOURCE ->
                0

            SldNodeType.BREAKER ->
                1

            SldNodeType.TRANSFORMER ->
                2

            SldNodeType.BUS ->
                3

            SldNodeType.PANEL ->
                4

            SldNodeType.GENERATOR ->
                5

            SldNodeType.LOAD ->
                6

            null ->
                99
        }
    }
}
