package com.electrical.calculationspro.data

import java.util.ArrayDeque
import kotlin.math.sqrt

/**
 * ================================================================
 * PROFESSIONAL SLD TOPOLOGY ENGINE
 * ================================================================
 *
 * Single authoritative topology layer for the SLD.
 *
 * Responsibilities:
 *
 * - Validate the radial electrical graph.
 * - Preserve engineering direction.
 * - Normalize legacy/internal BUSBAR connections.
 * - Reject invalid cable/busbar combinations.
 * - Detect duplicate connections.
 * - Detect cycles.
 * - Enforce one upstream parent.
 * - Verify SOURCE reachability.
 * - Build authoritative parent/child maps.
 *
 * INTERNAL BUSBAR CONNECTIONS
 *
 * Supported:
 *
 *     PANEL  -> BREAKER
 *     PANEL  -> BUS
 *     BUS    -> BREAKER
 *
 * These are internal switchboard/panel connections.
 *
 * They are NOT cables.
 *
 * Therefore:
 *
 *     lengthMeters        = 0
 *     cableSizeMm2        = 0
 *     currentCapacityA    = 0
 *     parallelRuns        = 1
 *     resistance          = 0
 *     reactance           = 0
 *
 * BUSBAR engineering data:
 *
 *     busbarRatedCurrentA
 *     busbarShortCircuitKA
 *
 * ================================================================
 */
object SldTopologyEngine {

    data class Topology(
        val source: SldNode,
        val connections: List<SldConnection>,
        val children: Map<String, List<SldNode>>,
        val parents: Map<String, List<SldNode>>
    )

    private enum class VisitState {
        UNVISITED,
        VISITING,
        VISITED
    }

    private companion object {

        const val DEFAULT_BUSBAR_CURRENT_A = 400.0
        const val DEFAULT_BUSBAR_SHORT_CIRCUIT_KA = 25.0
    }

    /**
     * ============================================================
     * INTERNAL BUSBAR PAIR
     * ============================================================
     */
    private fun isInternalBusbarPair(
        first: SldNodeType,
        second: SldNodeType
    ): Boolean {

        return (
            first == SldNodeType.PANEL &&
                second == SldNodeType.BREAKER
            ) ||
            (
                first == SldNodeType.BREAKER &&
                    second == SldNodeType.PANEL
                ) ||
            (
                first == SldNodeType.PANEL &&
                    second == SldNodeType.BUS
                ) ||
            (
                first == SldNodeType.BUS &&
                    second == SldNodeType.PANEL
                ) ||
            (
                first == SldNodeType.BUS &&
                    second == SldNodeType.BREAKER
                ) ||
            (
                first == SldNodeType.BREAKER &&
                    second == SldNodeType.BUS
                )
    }

    /**
     * ============================================================
     * BUSBAR DIRECTION
     * ============================================================
     *
     * PANEL -> BUS
     * PANEL -> BREAKER
     * BUS   -> BREAKER
     */
    private fun normalizeBusbarDirection(
        from: SldNode,
        to: SldNode
    ): Pair<SldNode, SldNode> {

        return when {

            from.type == SldNodeType.PANEL &&
                to.type == SldNodeType.BREAKER ->
                from to to

            from.type == SldNodeType.BREAKER &&
                to.type == SldNodeType.PANEL ->
                to to from

            from.type == SldNodeType.PANEL &&
                to.type == SldNodeType.BUS ->
                from to to

            from.type == SldNodeType.BUS &&
                to.type == SldNodeType.PANEL ->
                to to from

            from.type == SldNodeType.BUS &&
                to.type == SldNodeType.BREAKER ->
                from to to

            from.type == SldNodeType.BREAKER &&
                to.type == SldNodeType.BUS ->
                to to from

            else ->
                from to to
        }
    }

    /**
     * ============================================================
     * BUSBAR RATING
     * ============================================================
     */
    private fun resolveBusbarCurrent(
        from: SldNode,
        to: SldNode,
        connection: SldConnection
    ): Double {

        if (
            connection.busbarRatedCurrentA.isFinite() &&
            connection.busbarRatedCurrentA > 0.0
        ) {
            return connection.busbarRatedCurrentA
        }

        val panel =
            when {
                from.type == SldNodeType.PANEL ->
                    from

                to.type == SldNodeType.PANEL ->
                    to

                else ->
                    null
            }

        if (panel != null) {

            val kva =
                panel.ratedKva

            val voltage =
                panel.voltage

            if (
                kva.isFinite() &&
                kva > 0.0 &&
                voltage.isFinite() &&
                voltage > 0.0
            ) {

                val calculated =
                    kva *
                        1000.0 /
                        (
                            sqrt(3.0) *
                                voltage
                            )

                if (
                    calculated.isFinite() &&
                    calculated > 0.0
                ) {

                    return maxOf(
                        DEFAULT_BUSBAR_CURRENT_A,
                        calculated
                    )
                }
            }
        }

        return DEFAULT_BUSBAR_CURRENT_A
    }

    /**
     * ============================================================
     * NORMALIZE ONE CONNECTION
     * ============================================================
     */
    private fun normalizeConnection(
        connection: SldConnection,
        nodeMap: Map<String, SldNode>
    ): SldConnection {

        val from =
            nodeMap[
                connection.fromNodeId
            ] ?: return connection

        val to =
            nodeMap[
                connection.toNodeId
            ] ?: return connection

        val internalPair =
            isInternalBusbarPair(
                from.type,
                to.type
            )

        /*
         * Explicit BUSBAR or legacy internal PANEL/BUS/BREAKER
         * relation is always normalized as BUSBAR.
         */
        if (
            connection.connectionType !=
                SldConnectionType.BUSBAR &&
            !internalPair
        ) {
            return connection
        }

        require(
            internalPair
        ) {
            "Connection ${connection.id}: BUSBAR is only valid inside PANEL/BUS switchgear topology."
        }

        val direction =
            normalizeBusbarDirection(
                from,
                to
            )

        val normalizedFrom =
            direction.first

        val normalizedTo =
            direction.second

        val ratedCurrent =
            resolveBusbarCurrent(
                normalizedFrom,
                normalizedTo,
                connection
            )

        val shortCircuit =
            if (
                connection.busbarShortCircuitKA.isFinite() &&
                connection.busbarShortCircuitKA > 0.0
            ) {
                connection.busbarShortCircuitKA
            } else {
                DEFAULT_BUSBAR_SHORT_CIRCUIT_KA
            }

        return connection.copy(

            fromNodeId =
                normalizedFrom.id,

            toNodeId =
                normalizedTo.id,

            connectionType =
                SldConnectionType.BUSBAR,

            /*
             * ------------------------------------------------------
             * Cable data MUST be removed from BUSBAR.
             * ------------------------------------------------------
             */
            lengthMeters = 0.0,

            resistanceOhmPerKm = 0.0,

            reactanceOhmPerKm = 0.0,

            cableSizeMm2 = 0.0,

            parallelRuns = 1,

            voltageDropPercent = 0.0,

            currentCapacityA = 0.0,

            conductorMaterial = "",

            insulationType = "",

            installationMethodCode = "",

            loadedConductors = 0,

            cableDesignation = "",

            cableManufacturer = "",

            cableModel = "",

            busbarMaterial =
                connection.busbarMaterial
                    .ifBlank {
                        "Copper"
                    },

            busbarRatedCurrentA =
                ratedCurrent,

            busbarShortCircuitKA =
                shortCircuit
        )
    }

    /**
     * ============================================================
     * NORMALIZE NETWORK
     * ============================================================
     */
    private fun normalizeNetwork(
        network: SldNetwork
    ): SldNetwork {

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val normalized =
            network.connections.map {
                normalizeConnection(
                    connection = it,
                    nodeMap = nodeMap
                )
            }

        val result =
            mutableListOf<SldConnection>()

        val seen =
            mutableSetOf<String>()

        normalized.forEach { connection ->

            val key =
                "${connection.fromNodeId}->" +
                    "${connection.toNodeId}:" +
                    connection.connectionType

            if (
                seen.add(key)
            ) {
                result += connection
            }
        }

        return SldNetwork(
            nodes = network.nodes,
            connections = result
        )
    }

    /**
     * ============================================================
     * BUILD
     * ============================================================
     */
    fun build(
        network: SldNetwork
    ): Topology {

        require(
            network.nodes.isNotEmpty()
        ) {
            "SLD network is empty."
        }

        /*
         * Normalize first.
         *
         * This is the critical compatibility layer:
         *
         * old PANEL/BREAKER records
         * old BUS/BREAKER cable records
         * old busbar records carrying cable fields
         *
         * all become one consistent engineering representation.
         */
        val normalizedNetwork =
            normalizeNetwork(
                network
            )

        val nodes =
            normalizedNetwork.nodes

        val connections =
            normalizedNetwork.connections

        val nodeMap =
            nodes.associateBy {
                it.id
            }

        require(
            nodeMap.size == nodes.size
        ) {
            "Duplicate SLD node IDs."
        }

        /*
         * ============================================================
         * CONNECTION IDs
         * ============================================================
         */

        val connectionIds =
            connections.map {
                it.id
            }

        require(
            connectionIds.toSet().size ==
                connectionIds.size
        ) {
            "Duplicate SLD connection IDs."
        }

        /*
         * ============================================================
         * SOURCE
         * ============================================================
         */

        val sourceNodes =
            nodes.filter {
                it.type ==
                    SldNodeType.SOURCE
            }

        require(
            sourceNodes.size == 1
        ) {
            when {

                sourceNodes.isEmpty() ->
                    "SLD must contain one explicit electrical SOURCE."

                else ->
                    "SLD must contain exactly one electrical SOURCE for radial calculation."
            }
        }

        val source =
            sourceNodes.single()

        /*
         * ============================================================
         * CONNECTION VALIDATION
         * ============================================================
         */

        connections.forEach { connection ->

            val from =
                nodeMap[
                    connection.fromNodeId
                ]!!

            val to =
                nodeMap[
                    connection.toNodeId
                ]!!

            require(
                connection.fromNodeId !=
                    connection.toNodeId
            ) {
                "Connection ${connection.id}: node cannot connect to itself."
            }

            if (
                connection.connectionType ==
                    SldConnectionType.CABLE
            ) {

                require(
                    connection.parallelRuns >= 1
                ) {
                    "Connection ${connection.id}: parallel cable runs must be at least 1."
                }

                require(
                    connection.lengthMeters >= 0.0
                ) {
                    "Connection ${connection.id}: cable length cannot be negative."
                }

                require(
                    connection.cableSizeMm2 >= 0.0
                ) {
                    "Connection ${connection.id}: cable size cannot be negative."
                }

                require(
                    connection.currentCapacityA >= 0.0
                ) {
                    "Connection ${connection.id}: cable current capacity cannot be negative."
                }
            }

            if (
                connection.connectionType ==
                    SldConnectionType.BUSBAR
            ) {

                require(
                    isInternalBusbarPair(
                        from.type,
                        to.type
                    )
                ) {
                    "Connection ${connection.id}: invalid BUSBAR topology."
                }

                /*
                 * BUSBAR must never carry cable engineering data.
                 */
                require(
                    connection.lengthMeters == 0.0
                ) {
                    "Connection ${connection.id}: BUSBAR must not contain cable length."
                }

                require(
                    connection.cableSizeMm2 == 0.0
                ) {
                    "Connection ${connection.id}: BUSBAR must not contain cable size."
                }

                require(
                    connection.currentCapacityA == 0.0
                ) {
                    "Connection ${connection.id}: BUSBAR must not contain cable current capacity."
                }

                require(
                    connection.parallelRuns == 1
                ) {
                    "Connection ${connection.id}: BUSBAR must not contain cable parallel-run data."
                }

                require(
                    connection.busbarRatedCurrentA >= 0.0
                ) {
                    "Connection ${connection.id}: BUSBAR rated current cannot be negative."
                }

                require(
                    connection.busbarShortCircuitKA >= 0.0
                ) {
                    "Connection ${connection.id}: BUSBAR short-circuit rating cannot be negative."
                }
            }
        }

        /*
         * ============================================================
         * DUPLICATE DIRECTED CONNECTIONS
         * ============================================================
         */

        val directedKeys =
            mutableSetOf<String>()

        connections.forEach { connection ->

            val key =
                "${connection.fromNodeId}->${connection.toNodeId}"

            require(
                directedKeys.add(key)
            ) {
                "Duplicate SLD connection detected: $key"
            }
        }

        /*
         * ============================================================
         * GRAPH
         * ============================================================
         */

        val childrenIds =
            mutableMapOf<
                String,
                MutableList<String>
            >()

        val parentIds =
            mutableMapOf<
                String,
                MutableList<String>
            >()

        nodes.forEach { node ->

            childrenIds[node.id] =
                mutableListOf()

            parentIds[node.id] =
                mutableListOf()
        }

        connections.forEach { connection ->

            childrenIds[
                connection.fromNodeId
            ]!!
                .add(
                    connection.toNodeId
                )

            parentIds[
                connection.toNodeId
            ]!!
                .add(
                    connection.fromNodeId
                )
        }

        /*
         * ============================================================
         * SOURCE ROOT
         * ============================================================
         */

        require(
            parentIds[
                source.id
            ]
                .orEmpty()
                .isEmpty()
        ) {
            "Electrical SOURCE cannot have an upstream feeder."
        }

        /*
         * ============================================================
         * RADIAL PARENT RULE
         * ============================================================
         */

        val multiParent =
            parentIds.filter {
                it.value.size > 1
            }

        require(
            multiParent.isEmpty()
        ) {

            val names =
                multiParent.keys
                    .mapNotNull {
                        nodeMap[it]?.name
                    }

            "Invalid radial SLD: node(s) have multiple upstream feeders: " +
                names.joinToString()
        }

        /*
         * ============================================================
         * CYCLE DETECTION
         * ============================================================
         */

        val state =
            mutableMapOf<
                String,
                VisitState
            >()

        nodes.forEach { node ->

            state[node.id] =
                VisitState.UNVISITED
        }

        fun dfs(
            nodeId: String
        ) {

            when (
                state[nodeId]
            ) {

                VisitState.VISITING -> {

                    val name =
                        nodeMap[nodeId]?.name
                            ?: nodeId

                    throw IllegalArgumentException(
                        "Circular SLD connection detected at $name."
                    )
                }

                VisitState.VISITED ->
                    return

                VisitState.UNVISITED,
                null -> Unit
            }

            state[nodeId] =
                VisitState.VISITING

            childrenIds[
                nodeId
            ]
                .orEmpty()
                .forEach { childId ->
                    dfs(
                        childId
                    )
                }

            state[nodeId] =
                VisitState.VISITED
        }

        nodes.forEach { node ->

            if (
                state[node.id] ==
                    VisitState.UNVISITED
            ) {
                dfs(
                    node.id
                )
            }
        }

        /*
         * ============================================================
         * SOURCE REACHABILITY
         * ============================================================
         */

        val reachable =
            mutableSetOf<String>()

        val queue =
            ArrayDeque<String>()

        queue.add(
            source.id
        )

        while (
            queue.isNotEmpty()
        ) {

            val current =
                queue.removeFirst()

            if (
                !reachable.add(
                    current
                )
            ) {
                continue
            }

            childrenIds[
                current
            ]
                .orEmpty()
                .forEach { childId ->

                    if (
                        childId !in
                        reachable
                    ) {
                        queue.add(
                            childId
                        )
                    }
                }
        }

        val disconnected =
            nodes.filter {
                it.id !in reachable
            }

        require(
            disconnected.isEmpty()
        ) {

            val names =
                disconnected.joinToString {
                    it.name
                }

            "Disconnected or incorrectly directed SLD nodes: $names. Connections must point from upstream to downstream."
        }

        /*
         * ============================================================
         * MISSING PARENT
         * ============================================================
         */

        val missingParents =
            nodes.filter { node ->

                node.id != source.id &&
                    parentIds[
                        node.id
                    ]
                        .orEmpty()
                        .isEmpty()
            }

        require(
            missingParents.isEmpty()
        ) {

            val names =
                missingParents.joinToString {
                    it.name
                }

            "SLD nodes without an upstream feeder: $names"
        }

        /*
         * ============================================================
         * SOURCE OUTPUT
         * ============================================================
         */

        require(
            childrenIds[
                source.id
            ]
                .orEmpty()
                .isNotEmpty()
        ) {
            "Electrical SOURCE has no downstream connection."
        }

        /*
         * ============================================================
         * FINAL OBJECTS
         * ============================================================
         */

        val finalChildren =
            mutableMapOf<
                String,
                MutableList<SldNode>
            >()

        val finalParents =
            mutableMapOf<
                String,
                MutableList<SldNode>
            >()

        nodes.forEach { node ->

            finalChildren[node.id] =
                mutableListOf()

            finalParents[node.id] =
                mutableListOf()
        }

        connections.forEach { connection ->

            val from =
                nodeMap[
                    connection.fromNodeId
                ]!!

            val to =
                nodeMap[
                    connection.toNodeId
                ]!!

            finalChildren[
                from.id
            ]!!
                .add(
                    to
                )

            finalParents[
                to.id
            ]!!
                .add(
                    from
                )
        }

        return Topology(
            source = source,
            connections = connections,
            children =
                finalChildren.mapValues {
                    it.value.toList()
                },
            parents =
                finalParents.mapValues {
                    it.value.toList()
                }
        )
    }
}
