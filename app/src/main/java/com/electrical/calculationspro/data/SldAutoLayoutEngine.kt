package com.electrical.calculationspro.data

/**
 * Professional topology-aware SLD layout engine.
 *
 * Responsibilities:
 * - Arrange SLD nodes according to electrical topology.
 * - Keep the main upstream path visually centered.
 * - Distribute downstream branches.
 * - Prevent node overlap.
 * - Preserve existing topology and connections.
 *
 * This class changes presentation coordinates only.
 * No electrical calculation is performed here.
 */
object SldAutoLayoutEngine {

    private const val START_X = 620f
    private const val START_Y = 70f

    private const val LEVEL_Y = 220f

    private const val BRANCH_X = 320f

    private const val NODE_WIDTH = 180f
    private const val NODE_HEIGHT = 118f

    private const val CLEARANCE_X = 90f
    private const val CLEARANCE_Y = 80f

    private const val MIN_X = 40f
    private const val MIN_Y = 40f

    private const val MAX_SEARCH_STEPS = 200

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

        val nodesById =
            network.nodes.associateBy { it.id }

        /*
         * ------------------------------------------------------------
         * BUILD TOPOLOGY
         * ------------------------------------------------------------
         */

        val children =
            mutableMapOf<String, MutableList<String>>()

        val parents =
            mutableMapOf<String, MutableList<String>>()

        network.nodes.forEach { node ->

            children[node.id] =
                mutableListOf()

            parents[node.id] =
                mutableListOf()
        }

        network.connections.forEach { connection ->

            val from =
                nodesById[connection.fromNodeId]

            val to =
                nodesById[connection.toNodeId]

            if (
                from != null &&
                to != null &&
                from.id != to.id
            ) {

                if (
                    to.id !in
                    children.getValue(from.id)
                ) {
                    children
                        .getValue(from.id)
                        .add(to.id)
                }

                if (
                    from.id !in
                    parents.getValue(to.id)
                ) {
                    parents
                        .getValue(to.id)
                        .add(from.id)
                }
            }
        }

        /*
         * ------------------------------------------------------------
         * ROOTS
         * ------------------------------------------------------------
         *
         * SOURCE has priority as the engineering root.
         */
        val sourceRoots =
            network.nodes.filter {
                it.type == SldNodeType.SOURCE
            }

        val topologyRoots =
            network.nodes.filter { node ->

                parents[node.id]
                    .orEmpty()
                    .isEmpty()
            }

        val roots =
            (
                sourceRoots +
                    topologyRoots
                )
                .distinctBy {
                    it.id
                }
                .ifEmpty {
                    listOf(
                        network.nodes.first()
                    )
                }

        /*
         * ------------------------------------------------------------
         * LEVEL CALCULATION
         * ------------------------------------------------------------
         *
         * Level 0:
         * SOURCE
         *
         * Level 1:
         * ACB / BREAKER
         *
         * Level 2:
         * TRANSFORMER / BUS
         *
         * etc.
         */
        val levels =
            mutableMapOf<String, Int>()

        fun visit(
            nodeId: String,
            level: Int,
            path: Set<String>
        ) {

            /*
             * Protect against circular topology.
             */
            if (nodeId in path) {
                return
            }

            val existing =
                levels[nodeId]

            /*
             * Keep the shortest upstream level.
             */
            if (
                existing == null ||
                level < existing
            ) {
                levels[nodeId] = level
            }

            val actualLevel =
                levels[nodeId]
                    ?: level

            val nextLevel =
                actualLevel + 1

            val orderedChildren =
                children[nodeId]
                    .orEmpty()
                    .sortedWith(
                        compareBy<String> {
                            typeOrder(
                                nodesById[it]?.type
                            )
                        }.thenBy {
                            nodesById[it]
                                ?.name
                                ?.uppercase()
                                ?: ""
                        }
                    )

            orderedChildren.forEach { childId ->

                visit(
                    nodeId = childId,
                    level = nextLevel,
                    path = path + nodeId
                )
            }
        }

        roots.forEach { root ->

            visit(
                nodeId = root.id,
                level = 0,
                path = emptySet()
            )
        }

        /*
         * Put disconnected nodes into separate levels.
         */
        var disconnectedLevel =
            (
                levels.values.maxOrNull()
                    ?: 0
                ) + 1

        val disconnectedNodes =
            network.nodes
                .filter {
                    it.id !in levels
                }
                .sortedWith(
                    compareBy<SldNode> {
                        typeOrder(it.type)
                    }.thenBy {
                        it.name.uppercase()
                    }
                )

        disconnectedNodes.forEach { node ->

            levels[node.id] =
                disconnectedLevel

            disconnectedLevel++
        }

        /*
         * ------------------------------------------------------------
         * POSITION MAP
         * ------------------------------------------------------------
         */
        val preferredPositions =
            mutableMapOf<
                String,
                Pair<Float, Float>
            >()

        /*
         * ------------------------------------------------------------
         * MAIN TRUNK
         * ------------------------------------------------------------
         *
         * Follow a single-child path and keep it centered.
         */
        roots.forEach { root ->

            var currentId =
                root.id

            val visited =
                mutableSetOf<String>()

            while (
                visited.add(currentId)
            ) {

                val level =
                    levels[currentId]
                        ?: break

                preferredPositions[currentId] =
                    START_X to
                        (
                            START_Y +
                                level *
                                LEVEL_Y
                            )

                val candidates =
                    children[currentId]
                        .orEmpty()
                        .filter {
                            it !in visited
                        }
                        .sortedWith(
                            compareBy<String> {
                                typeOrder(
                                    nodesById[it]?.type
                                )
                            }.thenBy {
                                nodesById[it]
                                    ?.name
                                    ?.uppercase()
                                    ?: ""
                            }
                        )

                /*
                 * Only one child continues the center trunk.
                 *
                 * Multiple children become branches.
                 */
                val next =
                    candidates.singleOrNull()

                if (next == null) {
                    break
                }

                currentId =
                    next
            }
        }

        /*
         * ------------------------------------------------------------
         * LEVEL GROUPS
         * ------------------------------------------------------------
         */
        val levelGroups =
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

        /*
         * ------------------------------------------------------------
         * PLACE BRANCHES
         * ------------------------------------------------------------
         */
        levelGroups.forEach { (level, ids) ->

            val unplaced =
                ids
                    .filter {
                        it !in preferredPositions
                    }
                    .sortedWith(
                        compareBy<String> {
                            typeOrder(
                                nodesById[it]?.type
                            )
                        }.thenBy {
                            nodesById[it]
                                ?.name
                                ?.uppercase()
                                ?: ""
                        }
                    )

            if (unplaced.isEmpty()) {
                return@forEach
            }

            /*
             * First try to group nodes under their positioned parent.
             */
            val groupedByParent =
                unplaced.groupBy { childId ->

                    parents[childId]
                        .orEmpty()
                        .firstOrNull { parentId ->
                            preferredPositions.containsKey(
                                parentId
                            )
                        }
                }

            groupedByParent.forEach { entry ->

                val parentId =
                    entry.key

                val group =
                    entry.value

                if (parentId == null) {
                    return@forEach
                }

                val parentPosition =
                    preferredPositions[parentId]

                if (parentPosition == null) {
                    return@forEach
                }

                if (group.size == 1) {

                    /*
                     * Single branch.
                     */
                    val childId =
                        group.first()

                    preferredPositions[childId] =
                        (
                            parentPosition.first +
                                branchDirection(
                                    node =
                                        nodesById[childId]
                                ) *
                                BRANCH_X
                            ) to
                            (
                                START_Y +
                                    level *
                                    LEVEL_Y
                                )

                } else {

                    /*
                     * Multiple branches.
                     *
                     * They are distributed symmetrically around
                     * the parent's X coordinate.
                     */
                    val ordered =
                        group.sortedWith(
                            compareBy<String> {
                                typeOrder(
                                    nodesById[it]?.type
                                )
                            }.thenBy {
                                nodesById[it]
                                    ?.name
                                    ?.uppercase()
                                    ?: ""
                            }
                        )

                    val center =
                        (
                            ordered.size - 1
                            ) / 2f

                    ordered.forEachIndexed {
                        index,
                        childId
                        ->

                        val offset =
                            index - center

                        preferredPositions[childId] =
                            (
                                parentPosition.first +
                                    offset *
                                    BRANCH_X
                                ) to
                                (
                                    START_Y +
                                        level *
                                        LEVEL_Y
                                    )
                    }
                }
            }

            /*
             * Nodes with no positioned parent.
             */
            val remaining =
                unplaced.filter {
                    it !in preferredPositions
                }

            remaining.forEachIndexed {
                index,
                nodeId
                ->

                val side =
                    if (
                        index % 2 == 0
                    ) {
                        1f
                    } else {
                        -1f
                    }

                val slot =
                    (
                        index / 2
                    ) + 1

                preferredPositions[nodeId] =
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
         * ------------------------------------------------------------
         * BREAKER -> LOAD
         * ------------------------------------------------------------
         *
         * Keep loads horizontally aligned with their breaker.
         */
        network.connections.forEach { connection ->

            val from =
                nodesById[
                    connection.fromNodeId
                ]

            val to =
                nodesById[
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

                val breakerPosition =
                    preferredPositions[from.id]

                if (
                    breakerPosition != null
                ) {

                    preferredPositions[to.id] =
                        (
                            breakerPosition.first +
                                BRANCH_X
                            ) to
                            breakerPosition.second
                }
            }
        }

        /*
         * ------------------------------------------------------------
         * COLLISION RESOLUTION
         * ------------------------------------------------------------
         *
         * Resolve collisions without modifying topology.
         */
        val finalPositions =
            mutableMapOf<
                String,
                Pair<Float, Float>
            >()

        /*
         * Process upstream levels first.
         */
        val orderedNodeIds =
            levels.entries
                .sortedWith(
                    compareBy<
                        Map.Entry<String, Int>
                        > {
                        it.value
                    }.thenBy {
                        preferredPositions[it.key]
                            ?.first
                            ?: START_X
                    }.thenBy {
                        nodesById[it.key]
                            ?.name
                            ?.uppercase()
                            ?: ""
                    }
                )
                .map {
                    it.key
                }

        orderedNodeIds.forEach { nodeId ->

            val preferred =
                preferredPositions[nodeId]
                    ?: (
                        START_X to
                            START_Y
                        )

            finalPositions[nodeId] =
                resolveCollision(
                    preferred =
                        preferred,

                    occupied =
                        finalPositions.values
                )
        }

        /*
         * ------------------------------------------------------------
         * NORMALIZE
         * ------------------------------------------------------------
         */
        val minX =
            finalPositions.values
                .minOfOrNull {
                    it.first
                }
                ?: START_X

        val minY =
            finalPositions.values
                .minOfOrNull {
                    it.second
                }
                ?: START_Y

        val shiftX =
            if (minX < MIN_X) {
                MIN_X - minX
            } else {
                0f
            }

        val shiftY =
            if (minY < MIN_Y) {
                MIN_Y - minY
            } else {
                0f
            }

        val normalized =
            finalPositions.mapValues { entry ->

                val position =
                    entry.value

                (
                    position.first +
                        shiftX
                    ) to
                    (
                        position.second +
                            shiftY
                        )
            }

        /*
         * ------------------------------------------------------------
         * APPLY
         * ------------------------------------------------------------
         */
        val arrangedNodes =
            network.nodes.map { node ->

                val position =
                    normalized[node.id]

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

    /**
     * Resolve a node collision.
     *
     * Search order:
     * 1. Down
     * 2. Right
     * 3. Left
     * 4. Diagonal
     */
    private fun resolveCollision(
        preferred: Pair<Float, Float>,
        occupied: Collection<Pair<Float, Float>>
    ): Pair<Float, Float> {

        if (
            occupied.none {
                overlaps(
                    preferred,
                    it
                )
            }
        ) {
            return preferred
        }

        /*
         * Downward search.
         */
        for (step in 1..MAX_SEARCH_STEPS) {

            val candidate =
                preferred.first to
                    (
                        preferred.second +
                            step *
                            (
                                NODE_HEIGHT +
                                    CLEARANCE_Y
                                )
                        )

            if (
                occupied.none {
                    overlaps(
                        candidate,
                        it
                    )
                }
            ) {
                return candidate
            }
        }

        /*
         * Right / left search.
         */
        for (step in 1..MAX_SEARCH_STEPS) {

            val distance =
                step *
                    (
                        NODE_WIDTH +
                            CLEARANCE_X
                        )

            val right =
                (
                    preferred.first +
                        distance
                    ) to
                    preferred.second

            if (
                occupied.none {
                    overlaps(
                        right,
                        it
                    )
                }
            ) {
                return right
            }

            val left =
                (
                    preferred.first -
                        distance
                    ) to
                    preferred.second

            if (
                occupied.none {
                    overlaps(
                        left,
                        it
                    )
                }
            ) {
                return left
            }
        }

        /*
         * Guaranteed fallback.
         */
        var candidate =
            preferred

        var iteration =
            0

        while (
            occupied.any {
                overlaps(
                    candidate,
                    it
                )
            } &&
            iteration < 1000
        ) {

            candidate =
                (
                    candidate.first +
                        NODE_WIDTH +
                        CLEARANCE_X
                    ) to
                    (
                        candidate.second +
                            NODE_HEIGHT +
                            CLEARANCE_Y
                        )

            iteration++
        }

        return candidate
    }

    /**
     * Collision detection.
     */
    private fun overlaps(
        a: Pair<Float, Float>,
        b: Pair<Float, Float>
    ): Boolean {

        val aLeft =
            a.first

        val aTop =
            a.second

        val aRight =
            a.first +
                NODE_WIDTH +
                CLEARANCE_X

        val aBottom =
            a.second +
                NODE_HEIGHT +
                CLEARANCE_Y

        val bLeft =
            b.first

        val bTop =
            b.second

        val bRight =
            b.first +
                NODE_WIDTH +
                CLEARANCE_X

        val bBottom =
            b.second +
                NODE_HEIGHT +
                CLEARANCE_Y

        return (
            aLeft < bRight &&
                aRight > bLeft &&
                aTop < bBottom &&
                aBottom > bTop
            )
    }

    /**
     * Branch direction used only for presentation.
     */
    private fun branchDirection(
        node: SldNode?
    ): Float {

        return when (node?.type) {

            SldNodeType.GENERATOR ->
                -1f

            SldNodeType.SOURCE ->
                -1f

            else ->
                1f
        }
    }

    /**
     * Visual ordering of node types.
     *
     * This does not represent electrical priority,
     * protection priority, or design rating.
     */
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
