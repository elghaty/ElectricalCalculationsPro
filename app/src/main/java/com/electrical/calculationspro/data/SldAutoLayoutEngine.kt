package com.electrical.calculationspro.data

/**
 * Topology-aware professional SLD auto-layout.
 *
 * PANEL + BUS + BREAKERS are treated as one physical assembly.
 *
 * The electrical topology is never changed here.
 * Only coordinates are changed.
 */
object SldAutoLayoutEngine {

    private const val ROOT_X = 720f
    private const val ROOT_Y = 80f

    private const val LEVEL_GAP = 260f
    private const val BRANCH_GAP = 320f

    private const val NODE_WIDTH = 180f
    private const val NODE_HEIGHT = 150f

    private const val CLEARANCE_X = 120f
    private const val CLEARANCE_Y = 100f

    private const val MIN_X = 80f
    private const val MIN_Y = 70f

    /*
     * Physical panel assembly coordinates.
     *
     * PANEL x/y is the enclosure anchor.
     * BUS x/y is an internal busbar anchor.
     * BREAKER x/y is the breaker node anchor.
     */
    private const val BUS_Y = 30f
    private const val BREAKER_Y = 70f
    private const val BREAKER_SLOT = 100f

    private const val DISCONNECTED_GAP = 360f
    private const val MAX_COLLISION_SEARCH = 120

    data class LayoutResult(
        val network: SldNetwork,
        val levels: Map<String, Int>
    )

    private data class PanelAssembly(
        val panelId: String,
        val memberIds: Set<String>,
        val busIds: Set<String>,
        val breakerIds: List<String>
    )

    fun arrange(network: SldNetwork): LayoutResult {
        if (network.nodes.isEmpty()) {
            return LayoutResult(
                network = network,
                levels = emptyMap()
            )
        }

        val nodesById = network.nodes.associateBy { it.id }

        /*
         * ------------------------------------------------------------
         * BUILD PHYSICAL PANEL ASSEMBLIES
         * ------------------------------------------------------------
         */
        val panelAssemblies =
            network.nodes
                .filter { it.type == SldNodeType.PANEL }
                .map { panel ->
                    buildPanelAssembly(
                        panel = panel,
                        nodes = network.nodes,
                        connections = network.connections
                    )
                }

        val memberToPanel = mutableMapOf<String, String>()

        panelAssemblies.forEach { assembly ->
            assembly.memberIds.forEach { memberId ->
                if (memberId !in memberToPanel) {
                    memberToPanel[memberId] = assembly.panelId
                }
            }
        }

        /*
         * ------------------------------------------------------------
         * EXTERNAL TOPOLOGY GRAPH
         * ------------------------------------------------------------
         */
        val children =
            network.nodes
                .associate { it.id to mutableListOf<String>() }
                .toMutableMap()

        val parents =
            network.nodes
                .associate { it.id to mutableListOf<String>() }
                .toMutableMap()

        network.connections.forEach { connection ->
            val fromId = connection.fromNodeId
            val toId = connection.toNodeId

            val from = nodesById[fromId]
            val to = nodesById[toId]

            if (
                from == null ||
                to == null ||
                fromId == toId
            ) {
                return@forEach
            }

            if (
                isInternalPanelConnection(
                    connection = connection,
                    nodesById = nodesById,
                    memberToPanel = memberToPanel
                )
            ) {
                return@forEach
            }

            if (toId !in children.getValue(fromId)) {
                children.getValue(fromId).add(toId)
            }

            if (fromId !in parents.getValue(toId)) {
                parents.getValue(toId).add(fromId)
            }
        }

        /*
         * ------------------------------------------------------------
         * ROOTS
         * ------------------------------------------------------------
         */
        val sourceRoots =
            network.nodes
                .filter { it.type == SldNodeType.SOURCE }
                .sortedBy { it.name.uppercase() }

        val naturalRoots =
            network.nodes
                .filter { parents[it.id].orEmpty().isEmpty() }
                .sortedWith(
                    compareBy<SldNode> {
                        typeOrder(it.type)
                    }.thenBy {
                        it.name.uppercase()
                    }
                )

        val roots =
            (sourceRoots + naturalRoots)
                .distinctBy { it.id }

        /*
         * ------------------------------------------------------------
         * LEVELS
         * ------------------------------------------------------------
         */
        val levels = mutableMapOf<String, Int>()

        fun assignLevels(
            nodeId: String,
            level: Int,
            visiting: MutableSet<String>
        ) {
            if (!visiting.add(nodeId)) {
                return
            }

            val oldLevel = levels[nodeId]

            if (
                oldLevel == null ||
                level < oldLevel
            ) {
                levels[nodeId] = level
            }

            val actualLevel =
                levels[nodeId] ?: level

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
                .forEach { childId ->
                    assignLevels(
                        nodeId = childId,
                        level = actualLevel + 1,
                        visiting = HashSet(visiting)
                    )
                }
        }

        roots.forEach { root ->
            assignLevels(
                nodeId = root.id,
                level = 0,
                visiting = mutableSetOf()
            )
        }

        var extraLevel =
            (levels.values.maxOrNull() ?: 0) + 2

        network.nodes
            .filter { it.id !in levels }
            .sortedWith(
                compareBy<SldNode> {
                    typeOrder(it.type)
                }.thenBy {
                    it.name.uppercase()
                }
            )
            .forEach { node ->
                levels[node.id] = extraLevel
                extraLevel++
            }

        /*
         * ------------------------------------------------------------
         * INITIAL POSITIONS
         * ------------------------------------------------------------
         */
        val positions =
            mutableMapOf<String, Pair<Float, Float>>()

        roots
            .map { it.id }
            .filter { it in levels }
            .forEachIndexed { index, rootId ->
                positions[rootId] =
                    (
                        if (index == 0) {
                            ROOT_X
                        } else {
                            ROOT_X + index * BRANCH_GAP
                        }
                        ) to ROOT_Y
            }

        /*
         * ------------------------------------------------------------
         * SUBTREE WIDTH
         * ------------------------------------------------------------
         */
        val subtreeWidth =
            mutableMapOf<String, Float>()

        fun calculateWidth(
            nodeId: String,
            visiting: MutableSet<String>
        ): Float {
            if (!visiting.add(nodeId)) {
                return NODE_WIDTH + CLEARANCE_X
            }

            val childIds =
                children[nodeId]
                    .orEmpty()
                    .filter { it in levels }

            if (childIds.isEmpty()) {
                val width =
                    NODE_WIDTH + CLEARANCE_X

                subtreeWidth[nodeId] = width
                return width
            }

            val childWidth =
                childIds.sumOf { childId ->
                    calculateWidth(
                        nodeId = childId,
                        visiting = HashSet(visiting)
                    ).toDouble()
                }.toFloat()

            val width =
                maxOf(
                    NODE_WIDTH + CLEARANCE_X,
                    childWidth
                )

            subtreeWidth[nodeId] = width
            return width
        }

        roots.forEach { root ->
            calculateWidth(
                nodeId = root.id,
                visiting = mutableSetOf()
            )
        }

        /*
         * ------------------------------------------------------------
         * PLACE CHILDREN
         * ------------------------------------------------------------
         */
        fun placeChildren(parentId: String) {
            val parent =
                positions[parentId]
                    ?: return

            val childIds =
                children[parentId]
                    .orEmpty()
                    .filter { it in levels }

            if (childIds.isEmpty()) {
                return
            }

            val widths =
                childIds.map { childId ->
                    maxOf(
                        subtreeWidth[childId]
                            ?: (NODE_WIDTH + CLEARANCE_X),
                        NODE_WIDTH + CLEARANCE_X
                    )
                }

            val totalWidth =
                widths.sum()

            var cursorX =
                parent.first -
                    totalWidth / 2f

            childIds.forEachIndexed { index, childId ->
                val width = widths[index]

                val childX =
                    cursorX +
                        width / 2f -
                        NODE_WIDTH / 2f

                val childLevel =
                    levels[childId]
                        ?: ((levels[parentId] ?: 0) + 1)

                val childY =
                    ROOT_Y +
                        childLevel * LEVEL_GAP

                if (childId !in positions) {
                    positions[childId] =
                        childX to childY
                }

                cursorX += width

                placeChildren(
                    parentId = childId
                )
            }
        }

        roots.forEach { root ->
            placeChildren(
                parentId = root.id
            )
        }

        /*
         * ------------------------------------------------------------
         * DISCONNECTED NODES
         * ------------------------------------------------------------
         */
        network.nodes
            .filter { it.id !in positions }
            .sortedWith(
                compareBy<SldNode> {
                    levels[it.id] ?: Int.MAX_VALUE
                }.thenBy {
                    typeOrder(it.type)
                }.thenBy {
                    it.name.uppercase()
                }
            )
            .forEachIndexed { index, node ->
                val row = index / 4
                val column = index % 4

                positions[node.id] =
                    (
                        MIN_X +
                            column * DISCONNECTED_GAP
                        ) to
                        (
                            ROOT_Y +
                                (levels[node.id] ?: 0) *
                                LEVEL_GAP +
                                row * DISCONNECTED_GAP
                            )
            }

        /*
         * ------------------------------------------------------------
         * PHYSICAL PANEL ASSEMBLY PLACEMENT
         * ------------------------------------------------------------
         *
         * PANEL, BUS and BREAKERS occupy one physical assembly.
         *
         * The PANEL itself is the enclosure anchor.
         * BUS is placed inside the enclosure.
         * BREAKERS are placed horizontally inside the enclosure.
         */
        panelAssemblies.forEach { assembly ->
            val panel =
                nodesById[assembly.panelId]
                    ?: return@forEach

            val original =
                positions[panel.id]
                    ?: (ROOT_X to ROOT_Y)

            val panelX =
                snap(original.first)

            val panelY =
                snap(original.second)

            positions[panel.id] =
                panelX to panelY

            /*
             * BUS = internal busbar anchor.
             */
            assembly.busIds.forEach { busId ->
                positions[busId] =
                    snap(panelX) to
                        snap(panelY + BUS_Y)
            }

            /*
             * BREAKERS = internal breaker positions.
             *
             * Breaker nodes use the same 180 px logical node width
             * as the drawing layer. Their symbol is centered inside
             * that logical node.
             */
            val breakerIds =
                assembly.breakerIds

            if (breakerIds.isNotEmpty()) {
                val totalSpan =
                    (breakerIds.size - 1) *
                        BREAKER_SLOT

                val firstBreakerX =
                    panelX +
                        NODE_WIDTH / 2f -
                        totalSpan / 2f -
                        NODE_WIDTH / 2f

                breakerIds.forEachIndexed { index, breakerId ->
                    val breakerX =
                        firstBreakerX +
                            index * BREAKER_SLOT

                    val breakerY =
                        panelY + BREAKER_Y

                    positions[breakerId] =
                        snap(breakerX, breakerY)
                }
            }
        }

        /*
         * ------------------------------------------------------------
         * BREAKER -> LOAD
         * ------------------------------------------------------------
         *
         * LOAD remains outside the physical panel assembly.
         */
        network.connections
            .filter { connection ->
                val from =
                    nodesById[connection.fromNodeId]

                val to =
                    nodesById[connection.toNodeId]

                from?.type == SldNodeType.BREAKER &&
                    to?.type == SldNodeType.LOAD
            }
            .forEach { connection ->
                val breakerPosition =
                    positions[connection.fromNodeId]
                        ?: return@forEach

                positions[connection.toNodeId] =
                    snap(
                        breakerPosition.first + BRANCH_GAP,
                        breakerPosition.second
                    )
            }

        /*
         * ------------------------------------------------------------
         * FINAL COLLISION RESOLUTION
         * ------------------------------------------------------------
         *
         * Members of the same physical panel assembly are allowed
         * to overlap logically because they occupy the same enclosure.
         *
         * External nodes are still collision-checked.
         */
        val groupedIds =
            memberToPanel.keys

        val finalPositions =
            mutableMapOf<String, Pair<Float, Float>>()

        val orderedIds =
            levels.keys.sortedWith(
                compareBy<String> {
                    levels[it] ?: Int.MAX_VALUE
                }.thenBy {
                    positions[it]?.first ?: ROOT_X
                }.thenBy {
                    nodesById[it]
                        ?.name
                        ?.uppercase()
                        ?: ""
                }
            )

        orderedIds.forEach { nodeId ->
            val preferred =
                positions[nodeId]
                    ?: (ROOT_X to ROOT_Y)

            if (nodeId in groupedIds) {
                finalPositions[nodeId] =
                    preferred
            } else {
                finalPositions[nodeId] =
                    resolveCollision(
                        preferred = preferred,
                        occupied =
                            finalPositions
                                .filterKeys {
                                    it !in groupedIds
                                }
                                .values
                    )
            }
        }

        /*
         * ------------------------------------------------------------
         * NORMALIZE CANVAS ORIGIN
         * ------------------------------------------------------------
         */
        val minX =
            finalPositions.values
                .minOfOrNull { it.first }
                ?: ROOT_X

        val minY =
            finalPositions.values
                .minOfOrNull { it.second }
                ?: ROOT_Y

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
                val position = entry.value

                (
                    position.first + shiftX
                    ) to
                    (
                        position.second + shiftY
                    )
            }

        /*
         * ------------------------------------------------------------
         * APPLY COORDINATES
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
                    nodes = arrangedNodes
                ),
            levels = levels.toMap()
        )
    }

    /*
     * ================================================================
     * PHYSICAL PANEL ASSEMBLY
     * ================================================================
     */
    private fun buildPanelAssembly(
        panel: SldNode,
        nodes: List<SldNode>,
        connections: List<SldConnection>
    ): PanelAssembly {
        val nodesById =
            nodes.associateBy { it.id }

        val members =
            mutableSetOf<String>()

        members += panel.id

        /*
         * Only explicit BUSBAR connections can extend the physical
         * assembly from PANEL to BUS/BREAKER or between BUS/BREAKER.
         *
         * External CABLE connections never pull a node into the
         * enclosure.
         */
        var changed = true

        while (changed) {
            changed = false

            connections.forEach { connection ->
                if (
                    connection.connectionType !=
                    SldConnectionType.BUSBAR
                ) {
                    return@forEach
                }

                val from =
                    nodesById[connection.fromNodeId]
                        ?: return@forEach

                val to =
                    nodesById[connection.toNodeId]
                        ?: return@forEach

                val fromInside =
                    from.id in members

                val toInside =
                    to.id in members

                if (
                    fromInside &&
                    !toInside &&
                    isPhysicalPanelMember(to.type)
                ) {
                    /*
                     * Never absorb another PANEL into this assembly.
                     */
                    if (to.type != SldNodeType.PANEL) {
                        members += to.id
                        changed = true
                    }
                }

                if (
                    toInside &&
                    !fromInside &&
                    isPhysicalPanelMember(from.type)
                ) {
                    /*
                     * Never absorb another PANEL into this assembly.
                     */
                    if (from.type != SldNodeType.PANEL) {
                        members += from.id
                        changed = true
                    }
                }
            }
        }

        val buses =
            members
                .mapNotNull { nodesById[it] }
                .filter { it.type == SldNodeType.BUS }
                .map { it.id }
                .toSet()

        val breakers =
            members
                .mapNotNull { nodesById[it] }
                .filter { it.type == SldNodeType.BREAKER }
                .sortedWith(
                    compareBy<SldNode> {
                        it.x
                    }.thenBy {
                        it.y
                    }.thenBy {
                        it.name.uppercase()
                    }
                )
                .map { it.id }

        return PanelAssembly(
            panelId = panel.id,
            memberIds = members,
            busIds = buses,
            breakerIds = breakers
        )
    }

    private fun isPhysicalPanelMember(
        type: SldNodeType
    ): Boolean {
        return type == SldNodeType.PANEL ||
            type == SldNodeType.BUS ||
            type == SldNodeType.BREAKER
    }

    /*
     * A connection is internal only when:
     *
     * 1. it is BUSBAR;
     * 2. both endpoints belong to the same panel assembly.
     */
    private fun isInternalPanelConnection(
        connection: SldConnection,
        nodesById: Map<String, SldNode>,
        memberToPanel: Map<String, String>
    ): Boolean {
        if (
            connection.connectionType !=
            SldConnectionType.BUSBAR
        ) {
            return false
        }

        val from =
            nodesById[connection.fromNodeId]
                ?: return false

        val to =
            nodesById[connection.toNodeId]
                ?: return false

        val fromPanel =
            memberToPanel[from.id]

        val toPanel =
            memberToPanel[to.id]

        return fromPanel != null &&
            fromPanel == toPanel
    }

    /*
     * ================================================================
     * COLLISION
     * ================================================================
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

        for (step in 1..MAX_COLLISION_SEARCH) {
            val down =
                snap(
                    preferred.first,
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
                        down,
                        it
                    )
                }
            ) {
                return down
            }

            val right =
                snap(
                    preferred.first +
                        step *
                        (
                            NODE_WIDTH +
                                CLEARANCE_X
                            ),
                    preferred.second
                )

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
                snap(
                    preferred.first -
                        step *
                        (
                            NODE_WIDTH +
                                CLEARANCE_X
                            ),
                    preferred.second
                )

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

        return preferred
    }

    private fun overlaps(
        a: Pair<Float, Float>,
        b: Pair<Float, Float>
    ): Boolean {
        return (
            a.first <
                b.first +
                    NODE_WIDTH +
                    CLEARANCE_X
            ) &&
            (
                a.first +
                    NODE_WIDTH +
                    CLEARANCE_X >
                    b.first
            ) &&
            (
                a.second <
                    b.second +
                        NODE_HEIGHT +
                        CLEARANCE_Y
            ) &&
            (
                a.second +
                    NODE_HEIGHT +
                    CLEARANCE_Y >
                    b.second
            )
    }

    /*
     * Single canonical coordinate snap.
     *
     * IMPORTANT:
     * There is intentionally no snap(value) overload.
     * All coordinates must be supplied as x/y so Kotlin cannot
     * accidentally construct Pair<Pair<Float, Float>, ...>.
     */
    private fun snap(
        x: Float,
        y: Float
    ): Pair<Float, Float> {
        val safeX =
            if (x.isFinite()) {
                x
            } else {
                MIN_X
            }

        val safeY =
            if (y.isFinite()) {
                y
            } else {
                MIN_Y
            }

        return (
            kotlin.math.round(
                safeX / 20f
            ) * 20f
            ) to
            (
                kotlin.math.round(
                    safeY / 20f
                ) * 20f
            )
    }

    private fun typeOrder(
        type: SldNodeType?
    ): Int {
        return when (type) {
            SldNodeType.SOURCE -> 0
            SldNodeType.GENERATOR -> 1
            SldNodeType.TRANSFORMER -> 2
            SldNodeType.BUS -> 3
            SldNodeType.PANEL -> 4
            SldNodeType.BREAKER -> 5
            SldNodeType.LOAD -> 6
            null -> 99
        }
    }
}
