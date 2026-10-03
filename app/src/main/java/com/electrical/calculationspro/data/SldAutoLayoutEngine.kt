package com.electrical.calculationspro.data

/**
 * Professional topology-aware SLD auto-layout engine.
 *
 * IMPORTANT:
 * PANEL / BUS / BREAKER internal relationships are treated as
 * one physical panel assembly for visual layout.
 *
 * The electrical topology itself is NOT modified.
 * Only node coordinates are changed.
 *
 * PANEL:
 *   - represents the physical enclosure anchor.
 *
 * BUS:
 *   - represents the internal busbar location.
 *
 * BREAKER:
 *   - represents a real breaker installed inside the enclosure.
 *
 * External feeders remain outside the enclosure.
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

    private const val PANEL_BUS_Y = 145f
    private const val PANEL_BREAKER_Y = 190f
    private const val BREAKER_SLOT = 220f

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
            network.nodes.associateBy {
                it.id
            }

        /*
         * ============================================================
         * PANEL ASSEMBLIES
         * ============================================================
         *
         * A PANEL and its internal BUS / BREAKERS form one physical
         * assembly.
         *
         * This is presentation/layout grouping only.
         */
        val panelAssemblies =
            network.nodes
                .filter {
                    it.type ==
                        SldNodeType.PANEL
                }
                .map {
                    buildPanelAssembly(
                        panel = it,
                        nodes = network.nodes,
                        connections = network.connections
                    )
                }

        val memberToPanel =
            mutableMapOf<String, String>()

        panelAssemblies.forEach { assembly ->

            assembly.memberIds.forEach { memberId ->

                /*
                 * A member belongs to the first physical panel
                 * that owns it. This protects against malformed
                 * overlapping enclosure definitions.
                 */
                if (
                    memberId !in memberToPanel
                ) {
                    memberToPanel[memberId] =
                        assembly.panelId
                }
            }
        }

        /*
         * ============================================================
         * TOPOLOGY GRAPH
         * ============================================================
         *
         * Internal BUSBAR connections do not create visual levels.
         *
         * Example:
         *
         * PANEL -> BUS -> BREAKER
         *
         * is one enclosure, not three vertical levels.
         */
        val children =
            network.nodes.associate { node ->
                node.id to
                    mutableListOf<String>()
            }.toMutableMap()

        val parents =
            network.nodes.associate { node ->
                node.id to
                    mutableListOf<String>()
            }.toMutableMap()

        network.connections.forEach { connection ->

            val fromId =
                connection.fromNodeId

            val toId =
                connection.toNodeId

            if (
                fromId == toId ||
                !nodesById.containsKey(fromId) ||
                !nodesById.containsKey(toId)
            ) {
                return@forEach
            }

            /*
             * Ignore internal panel wiring when determining
             * vertical topology.
             */
            if (
                isInternalPanelConnection(
                    connection = connection,
                    nodesById = nodesById,
                    memberToPanel = memberToPanel
                )
            ) {
                return@forEach
            }

            val childList =
                children.getValue(fromId)

            if (
                toId !in childList
            ) {
                childList += toId
            }

            val parentList =
                parents.getValue(toId)

            if (
                fromId !in parentList
            ) {
                parentList += fromId
            }
        }

        /*
         * ============================================================
         * ROOTS
         * ============================================================
         */
        val sourceRoots =
            network.nodes
                .filter {
                    it.type ==
                        SldNodeType.SOURCE
                }
                .sortedBy {
                    it.name.uppercase()
                }

        val naturalRoots =
            network.nodes
                .filter {
                    parents[it.id]
                        .orEmpty()
                        .isEmpty()
                }
                .sortedWith(
                    compareBy<SldNode> {
                        typeOrder(it.type)
                    }.thenBy {
                        it.name.uppercase()
                    }
                )

        val roots =
            (
                sourceRoots +
                    naturalRoots
                )
                .distinctBy {
                    it.id
                }

        /*
         * ============================================================
         * LEVELS
         * ============================================================
         */
        val levels =
            mutableMapOf<String, Int>()

        fun assignLevels(
            nodeId: String,
            level: Int,
            visiting: MutableSet<String>
        ) {

            if (
                !visiting.add(nodeId)
            ) {
                return
            }

            val old =
                levels[nodeId]

            if (
                old == null ||
                level < old
            ) {
                levels[nodeId] =
                    level
            }

            val actual =
                levels[nodeId]
                    ?: level

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
                        level = actual + 1,
                        visiting = HashSet(
                            visiting
                        )
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

        /*
         * Any remaining node receives a safe level.
         */
        var extraLevel =
            (
                levels.values.maxOrNull()
                    ?: 0
                ) + 2

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
            .forEach { node ->

                levels[node.id] =
                    extraLevel

                extraLevel++
            }

        /*
         * ============================================================
         * NORMAL NODE POSITIONING
         * ============================================================
         */
        val positions =
            mutableMapOf<String, Pair<Float, Float>>()

        val rootIds =
            roots
                .map {
                    it.id
                }
                .filter {
                    it in levels
                }

        rootIds.forEachIndexed {
            index,
            rootId ->

            positions[rootId] =
                (
                    if (index == 0) {
                        ROOT_X
                    } else {
                        ROOT_X +
                            index *
                            BRANCH_GAP
                    }
                    ) to
                    ROOT_Y
        }

        /*
         * ============================================================
         * SUBTREE WIDTH
         * ============================================================
         */
        val subtreeWidth =
            mutableMapOf<String, Float>()

        fun calculateWidth(
            nodeId: String,
            visiting: MutableSet<String>
        ): Float {

            if (
                !visiting.add(nodeId)
            ) {
                return NODE_WIDTH +
                    CLEARANCE_X
            }

            val childIds =
                children[nodeId]
                    .orEmpty()
                    .filter {
                        it in levels
                    }

            if (
                childIds.isEmpty()
            ) {

                val width =
                    NODE_WIDTH +
                        CLEARANCE_X

                subtreeWidth[nodeId] =
                    width

                return width
            }

            val childWidth =
                childIds.sumOf { childId ->

                    calculateWidth(
                        nodeId = childId,
                        visiting =
                            HashSet(
                                visiting
                            )
                    ).toDouble()
                }.toFloat()

            val width =
                maxOf(
                    NODE_WIDTH +
                        CLEARANCE_X,

                    childWidth
                )

            subtreeWidth[nodeId] =
                width

            return width
        }

        rootIds.forEach {

            calculateWidth(
                nodeId = it,
                visiting =
                    mutableSetOf()
            )
        }

        /*
         * ============================================================
         * PLACE NORMAL CHILDREN
         * ============================================================
         */
        fun placeChildren(
            parentId: String
        ) {

            val parent =
                positions[parentId]
                    ?: return

            val childIds =
                children[parentId]
                    .orEmpty()
                    .filter {
                        it in levels
                    }

            if (
                childIds.isEmpty()
            ) {
                return
            }

            val widths =
                childIds.map { childId ->

                    maxOf(
                        subtreeWidth[childId]
                            ?: (
                                NODE_WIDTH +
                                    CLEARANCE_X
                                ),

                        NODE_WIDTH +
                            CLEARANCE_X
                    )
                }

            val totalWidth =
                widths.sum()

            var cursorX =
                parent.first -
                    totalWidth / 2f

            childIds.forEachIndexed {
                index,
                childId ->

                val width =
                    widths[index]

                val childX =
                    cursorX +
                        width / 2f -
                        NODE_WIDTH / 2f

                val childLevel =
                    (
                        levels[childId]
                            ?: (
                                levels[parentId]
                                    ?: 0
                                ) + 1
                        )

                val childY =
                    ROOT_Y +
                        childLevel *
                        LEVEL_GAP

                if (
                    childId !in positions
                ) {

                    positions[childId] =
                        childX to childY
                }

                cursorX +=
                    width

                placeChildren(
                    parentId =
                        childId
                )
            }
        }

        rootIds.forEach {
            placeChildren(
                parentId = it
            )
        }

        /*
         * ============================================================
         * DISCONNECTED NODES
         * ============================================================
         */
        network.nodes
            .filter {
                it.id !in positions
            }
            .sortedWith(
                compareBy<SldNode> {
                    levels[it.id]
                        ?: Int.MAX_VALUE
                }.thenBy {
                    typeOrder(it.type)
                }.thenBy {
                    it.name.uppercase()
                }
            )
            .forEachIndexed {
                index,
                node ->

                val row =
                    index / 4

                val column =
                    index % 4

                positions[node.id] =
                    (
                        MIN_X +
                            column *
                            DISCONNECTED_GAP
                        ) to
                        (
                            ROOT_Y +
                                (
                                    levels[node.id]
                                        ?: 0
                                    ) *
                                LEVEL_GAP +
                                row *
                                DISCONNECTED_GAP
                            )
            }

        /*
         * ============================================================
         * APPLY PANEL ASSEMBLY GEOMETRY
         * ============================================================
         *
         * The PANEL itself becomes the enclosure anchor.
         *
         * BUS is positioned on the internal busbar axis.
         *
         * BREAKERS are positioned on the lower side of the busbar.
         */
        panelAssemblies.forEach { assembly ->

            val panel =
                nodesById[assembly.panelId]
                    ?: return@forEach

            val original =
                positions[panel.id]
                    ?: (
                        ROOT_X to
                            ROOT_Y
                        )

            val panelX =
                snap(
                    original.first
                )

            val panelY =
                snap(
                    original.second
                )

            positions[panel.id] =
                panelX to
                    panelY

            val breakerIds =
                assembly.breakerIds

            val totalWidth =
                maxOf(
                    NODE_WIDTH,
                    (
                        breakerIds.size -
                            1
                        ) *
                        BREAKER_SLOT +
                        NODE_WIDTH
                )

            val firstBreakerX =
                panelX +
                    NODE_WIDTH / 2f -
                    totalWidth / 2f

            breakerIds.forEachIndexed {
                index,
                breakerId ->

                positions[breakerId] =
                    snap(
                        firstBreakerX +
                            index *
                            BREAKER_SLOT
                    ) to
                    snap(
                        panelY +
                            PANEL_BREAKER_Y
                    )
            }

            assembly.busIds.forEach { busId ->

                positions[busId] =
                    panelX to
                    snap(
                        panelY +
                            PANEL_BUS_Y -
                            30f
                    )
            }
        }

        /*
         * ============================================================
         * BREAKER -> LOAD ALIGNMENT
         * ============================================================
         *
         * Loads leave the enclosure horizontally.
         */
        network.connections
            .filter { connection ->

                val from =
                    nodesById[
                        connection.fromNodeId
                    ]

                val to =
                    nodesById[
                        connection.toNodeId
                    ]

                from?.type ==
                    SldNodeType.BREAKER &&
                    to?.type ==
                    SldNodeType.LOAD
            }
            .forEach { connection ->

                val breakerPosition =
                    positions[
                        connection.fromNodeId
                    ]

                if (
                    breakerPosition != null
                ) {

                    positions[
                        connection.toNodeId
                    ] =
                        (
                            breakerPosition.first +
                                BRANCH_GAP
                            ) to
                            breakerPosition.second
                }
            }

        /*
         * ============================================================
         * COLLISION RESOLUTION
         * ============================================================
         *
         * Do not separate members of the same enclosure from
         * each other.
         */
        val groupedIds =
            memberToPanel.keys

        val finalPositions =
            mutableMapOf<String, Pair<Float, Float>>()

        val orderedIds =
            levels.keys
                .sortedWith(
                    compareBy<String> {
                        levels[it]
                            ?: Int.MAX_VALUE
                    }.thenBy {
                        positions[it]
                            ?.first
                            ?: ROOT_X
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
                    ?: (
                        ROOT_X to
                            ROOT_Y
                        )

            if (
                nodeId in groupedIds
            ) {

                /*
                 * Panel members are already laid out as one
                 * physical assembly.
                 */
                finalPositions[nodeId] =
                    preferred

            } else {

                finalPositions[nodeId] =
                    resolveCollision(
                        preferred =
                            preferred,

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
         * ============================================================
         * NORMALIZATION
         * ============================================================
         */
        val minX =
            finalPositions.values
                .minOfOrNull {
                    it.first
                }
                ?: ROOT_X

        val minY =
            finalPositions.values
                .minOfOrNull {
                    it.second
                }
                ?: ROOT_Y

        val shiftX =
            if (
                minX < MIN_X
            ) {
                MIN_X -
                    minX
            } else {
                0f
            }

        val shiftY =
            if (
                minY < MIN_Y
            ) {
                MIN_Y -
                    minY
            } else {
                0f
            }

        val normalized =
            finalPositions.mapValues {
                entry ->

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
         * ============================================================
         * APPLY COORDINATES
         * ============================================================
         */
        val arrangedNodes =
            network.nodes.map { node ->

                val position =
                    normalized[node.id]

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

    private fun buildPanelAssembly(
        panel: SldNode,
        nodes: List<SldNode>,
        connections: List<SldConnection>
    ): PanelAssembly {

        val nodesById =
            nodes.associateBy {
                it.id
            }

        val members =
            mutableSetOf<String>()

        members +=
            panel.id

        var changed = true

        while (
            changed
        ) {

            changed = false

            connections.forEach { connection ->

                val from =
                    nodesById[
                        connection.fromNodeId
                    ]

                val to =
                    nodesById[
                        connection.toNodeId
                    ]

                if (
                    from == null ||
                    to == null
                ) {
                    return@forEach
                }

                if (
                    !isInternalPanelPair(
                        from.type,
                        to.type
                    ) &&
                    connection.connectionType !=
                        SldConnectionType.BUSBAR
                ) {
                    return@forEach
                }

                val fromInside =
                    from.id in members

                val toInside =
                    to.id in members

                if (
                    fromInside &&
                    !toInside &&
                    isPanelInternalType(
                        to.type
                    )
                ) {

                    members +=
                        to.id

                    changed = true
                }

                if (
                    toInside &&
                    !fromInside &&
                    isPanelInternalType(
                        from.type
                    )
                ) {

                    members +=
                        from.id

                    changed = true
                }
            }
        }

        val buses =
            members
                .mapNotNull {
                    nodesById[it]
                }
                .filter {
                    it.type ==
                        SldNodeType.BUS
                }
                .map {
                    it.id
                }
                .toSet()

        val breakers =
            members
                .mapNotNull {
                    nodesById[it]
                }
                .filter {
                    it.type ==
                        SldNodeType.BREAKER
                }
                .sortedWith(
                    compareBy<SldNode> {
                        it.x
                    }.thenBy {
                        it.y
                    }.thenBy {
                        it.name
                    }
                )
                .map {
                    it.id
                }

        return PanelAssembly(
            panelId =
                panel.id,

            memberIds =
                members,

            busIds =
                buses,

            breakerIds =
                breakers
        )
    }

    private fun isPanelInternalType(
        type: SldNodeType
    ): Boolean {

        return type ==
            SldNodeType.PANEL ||
            type ==
            SldNodeType.BUS ||
            type ==
            SldNodeType.BREAKER
    }

    private fun isInternalPanelPair(
        a: SldNodeType,
        b: SldNodeType
    ): Boolean {

        return isPanelInternalType(a) &&
            isPanelInternalType(b)
    }

    private fun isInternalPanelConnection(
        connection: SldConnection,
        nodesById: Map<String, SldNode>,
        memberToPanel: Map<String, String>
    ): Boolean {

        if (
            connection.connectionType ==
                SldConnectionType.BUSBAR
        ) {
            return true
        }

        val fromPanel =
            memberToPanel[
                connection.fromNodeId
            ]

        val toPanel =
            memberToPanel[
                connection.toNodeId
            ]

        return fromPanel != null &&
            fromPanel == toPanel
    }

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

        for (
            step in 1..MAX_COLLISION_SEARCH
        ) {

            val down =
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
                        down,
                        it
                    )
                }
            ) {
                return down
            }

            val right =
                (
                    preferred.first +
                        step *
                        (
                            NODE_WIDTH +
                                CLEARANCE_X
                            )
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
                        step *
                        (
                            NODE_WIDTH +
                                CLEARANCE_X
                            )
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
                    CLEARANCE_X &&

                a.first +
                    NODE_WIDTH +
                    CLEARANCE_X >
                    b.first &&

                a.second <
                    b.second +
                        NODE_HEIGHT +
                        CLEARANCE_Y &&

                a.second +
                    NODE_HEIGHT +
                    CLEARANCE_Y >
                    b.second
            )
    }

    private fun snap(
        value: Float
    ): Float {

        return (
            kotlin.math.round(
                value / 20f
            ) * 20f
            )
    }

    private fun typeOrder(
        type: SldNodeType?
    ): Int {

        return when (type) {

            SldNodeType.SOURCE ->
                0

            SldNodeType.GENERATOR ->
                1

            SldNodeType.TRANSFORMER ->
                2

            SldNodeType.BUS ->
                3

            SldNodeType.PANEL ->
                4

            SldNodeType.BREAKER ->
                5

            SldNodeType.LOAD ->
                6

            null ->
                99
        }
    }
}
