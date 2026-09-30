package com.electrical.calculationspro.data

import java.util.ArrayDeque

/**
 * ================================================================
 * PROFESSIONAL SLD TOPOLOGY ENGINE
 * ================================================================
 *
 * Responsibilities:
 *
 * 1. Validate the stored electrical graph.
 * 2. Preserve the stored electrical direction.
 * 3. Validate BUSBAR / CABLE semantics.
 * 4. Detect duplicate directed connections.
 * 5. Detect cycles.
 * 6. Enforce radial topology.
 * 7. Enforce one upstream parent per downstream node.
 * 8. Verify source reachability.
 * 9. Build parent/child maps for engineering calculations.
 *
 * IMPORTANT:
 *
 * The topology engine NEVER reverses a connection automatically.
 *
 * Stored direction:
 *
 *     fromNodeId -> toNodeId
 *
 * is the engineering direction.
 *
 * Component type, screen position, distance, or node name must
 * never be used to silently change that direction.
 *
 * Example:
 *
 * SOURCE
 *    |
 * BREAKER
 *    |
 *  BUS
 *    |
 * TRANSFORMER
 *    |
 * BREAKER
 *    |
 * PANEL
 *    |
 * BREAKER
 *    |
 * LOAD
 *
 * Current radial model:
 *
 * - One electrical SOURCE.
 * - One upstream parent per node.
 * - No directed cycles.
 * - Every node must be reachable from SOURCE.
 *
 * BUSBAR rule:
 *
 *     PANEL -> BREAKER
 *
 * represents an internal panel bus connection.
 *
 * BUSBAR is NOT a cable and therefore does not use:
 *
 * - cable length
 * - cable size
 * - cable ampacity
 * - cable parallel runs
 *
 * Instead it uses:
 *
 * - busbar rated current
 * - busbar short-circuit rating
 * ================================================================
 */
object SldTopologyEngine {

    data class Topology(
        val source: SldNode,
        val connections: List<SldConnection>,
        val children: Map<String, List<SldNode>>,
        val parents: Map<String, List<SldNode>>
    )

    /**
     * Build and validate the electrical topology.
     *
     * The stored connection direction is preserved exactly.
     */
    fun build(
        network: SldNetwork
    ): Topology {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        /*
         * ============================================================
         * NODE INDEX
         * ============================================================
         */

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        require(
            nodeMap.size == network.nodes.size
        ) {
            "Duplicate SLD node IDs."
        }

        /*
         * ============================================================
         * CONNECTION ID VALIDATION
         * ============================================================
         */

        val connectionIds =
            network.connections.map {
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
         * ELECTRICAL SOURCE
         * ============================================================
         *
         * The professional radial engine requires one explicit
         * SOURCE node.
         *
         * We do NOT infer a source from graphical position or
         * from missing incoming connections.
         */

        val sourceNodes =
            network.nodes.filter {
                it.type == SldNodeType.SOURCE
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
         * BASIC CONNECTION VALIDATION
         * ============================================================
         */

        network.connections.forEach { connection ->

            require(
                connection.fromNodeId in nodeMap
            ) {
                "Connection ${connection.id}: source node does not exist."
            }

            require(
                connection.toNodeId in nodeMap
            ) {
                "Connection ${connection.id}: destination node does not exist."
            }

            require(
                connection.fromNodeId !=
                    connection.toNodeId
            ) {
                "Connection ${connection.id}: node cannot connect to itself."
            }

            /*
             * Parallel-run data belongs to cable feeders.
             *
             * BUSBAR must use exactly one logical internal connection.
             */
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

                /*
                 * Zero capacity means that sizing has not yet been
                 * verified. It is not treated as an adequate cable.
                 *
                 * Topology remains buildable so the engineering
                 * calculation layer can report the missing sizing
                 * information rather than crashing the SLD editor.
                 */
            }

            if (
                connection.connectionType ==
                    SldConnectionType.BUSBAR
            ) {

                val fromNode =
                    nodeMap[connection.fromNodeId]!!

                val toNode =
                    nodeMap[connection.toNodeId]!!

                /*
                 * ----------------------------------------------------
                 * BUSBAR TOPOLOGY
                 * ----------------------------------------------------
                 *
                 * Current supported internal panel connection:
                 *
                 *     PANEL -> BREAKER
                 *
                 * This prevents BUSBAR from being incorrectly used
                 * as an external feeder.
                 */
                require(
                    fromNode.type ==
                        SldNodeType.PANEL &&
                        toNode.type ==
                        SldNodeType.BREAKER
                ) {
                    "Connection ${connection.id}: BUSBAR is only valid for an internal PANEL -> BREAKER connection."
                }

                /*
                 * ----------------------------------------------------
                 * BUSBAR MUST NOT CONTAIN CABLE DATA
                 * ----------------------------------------------------
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

                /*
                 * Busbar engineering data is checked when available.
                 *
                 * Zero values are allowed at topology level because
                 * sizing may be performed later by the engineering
                 * layer. They are NOT interpreted as adequate.
                 */
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
         *
         * A -> B and another A -> B represent duplicated topology.
         *
         * They are rejected here.
         *
         * A -> B and B -> A are also invalid in the radial model and
         * will be rejected by cycle detection.
         */

        val directedConnectionKeys =
            mutableSetOf<String>()

        network.connections.forEach { connection ->

            val key =
                "${connection.fromNodeId}->${connection.toNodeId}"

            require(
                directedConnectionKeys.add(key)
            ) {
                "Duplicate SLD connection detected: $key"
            }
        }

        /*
         * ============================================================
         * DIRECTED GRAPH
         * ============================================================
         *
         * The graph is intentionally directed.
         *
         * fromNodeId -> toNodeId
         *
         * Never convert it to an undirected graph.
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

        network.nodes.forEach { node ->

            childrenIds[node.id] =
                mutableListOf()

            parentIds[node.id] =
                mutableListOf()
        }

        network.connections.forEach { connection ->

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
         * SOURCE PARENT VALIDATION
         * ============================================================
         *
         * SOURCE must be the root of the radial network.
         */

        require(
            parentIds[source.id]
                .orEmpty()
                .isEmpty()
        ) {
            "Electrical SOURCE cannot have an upstream feeder."
        }

        /*
         * ============================================================
         * RADIAL PARENT VALIDATION
         * ============================================================
         *
         * Every downstream node must have no more than one upstream
         * feeder.
         *
         * This excludes transfer/changeover/ATS/synchronizing
         * topologies from the current radial model.
         */

        val multiParentNodes =
            parentIds.filter {
                it.value.size > 1
            }

        require(
            multiParentNodes.isEmpty()
        ) {

            val names =
                multiParentNodes.keys
                    .mapNotNull { id ->
                        nodeMap[id]?.name
                    }

            "Invalid radial SLD: node(s) have multiple upstream feeders: " +
                names.joinToString()
        }

        /*
         * ============================================================
         * CYCLE DETECTION
         * ============================================================
         */

        val visitState =
            mutableMapOf<
                String,
                VisitState
            >()

        network.nodes.forEach { node ->

            visitState[node.id] =
                VisitState.UNVISITED
        }

        fun dfs(
            nodeId: String
        ) {

            when (
                visitState[nodeId]
            ) {

                VisitState.VISITING -> {

                    val nodeName =
                        nodeMap[nodeId]?.name
                            ?: nodeId

                    throw IllegalArgumentException(
                        "Circular SLD connection detected at $nodeName."
                    )
                }

                VisitState.VISITED -> {
                    return
                }

                VisitState.UNVISITED,
                null -> Unit
            }

            visitState[nodeId] =
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

            visitState[nodeId] =
                VisitState.VISITED
        }

        /*
         * Check the complete graph, not only the source branch.
         */
        network.nodes.forEach { node ->

            if (
                visitState[node.id] ==
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
         *
         * All engineering nodes must be reachable through the
         * STORED electrical direction.
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
            network.nodes.filter {
                it.id !in reachable
            }

        require(
            disconnected.isEmpty()
        ) {

            val disconnectedNames =
                disconnected.joinToString {
                    it.name
                }

            "Disconnected or incorrectly directed SLD nodes: " +
                disconnectedNames +
                ". Connections must point from upstream to downstream."
        }

        /*
         * ============================================================
         * MISSING PARENT VALIDATION
         * ============================================================
         *
         * Every node except SOURCE must have exactly one parent.
         */

        val missingParents =
            network.nodes.filter { node ->

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
         * SOURCE OUTGOING CHECK
         * ============================================================
         *
         * A source with no downstream path is structurally valid
         * as a node but not useful for engineering design.
         *
         * This remains a topology error because the radial network
         * has no electrical distribution path.
         */

        require(
            childrenIds[source.id]
                .orEmpty()
                .isNotEmpty()
        ) {
            "Electrical SOURCE has no downstream connection."
        }

        /*
         * ============================================================
         * FINAL CHILDREN / PARENTS OBJECTS
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

        network.nodes.forEach { node ->

            finalChildren[node.id] =
                mutableListOf()

            finalParents[node.id] =
                mutableListOf()
        }

        network.connections.forEach { connection ->

            val from =
                nodeMap[
                    connection.fromNodeId
                ]
                    ?: error(
                        "Connection ${connection.id}: source node disappeared during topology build."
                    )

            val to =
                nodeMap[
                    connection.toNodeId
                ]
                    ?: error(
                        "Connection ${connection.id}: destination node disappeared during topology build."
                    )

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

        /*
         * ============================================================
         * FINAL TOPOLOGY
         * ============================================================
         *
         * No connection is reversed.
         *
         * The engineering chain is:
         *
         * SLD EDITOR
         *      ↓
         * STORED CONNECTION DIRECTION
         *      ↓
         * TOPOLOGY VALIDATION
         *      ↓
         * UPSTREAM ENGINEERING
         *
         * This keeps topology and engineering calculations coherent.
         */

        return Topology(
            source = source,

            connections =
                network.connections,

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

    private enum class VisitState {
        UNVISITED,
        VISITING,
        VISITED
    }
}
