package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD DESIGN VALIDATOR
 * ================================================================
 *
 * Validates the electrical topology before engineering calculations.
 *
 * Design
 *   ↓
 * Topology Validation
 *   ↓
 * Engineering Calculation
 *
 * This validator does not calculate equipment ratings.
 * It verifies that the network is logically valid and ready
 * for engineering calculations.
 *
 * Important engineering rules:
 *
 * 1. BUSBAR is an internal panel connection.
 * 2. BUSBAR is currently valid only for PANEL -> BREAKER.
 * 3. BUSBAR does not use cable length, cable size, cable capacity
 *    or cable parallel-run data.
 * 4. CABLE is used for external feeders.
 * 5. Missing cable ampacity is NOT treated as adequate.
 * 6. BUS and BREAKER do not carry duplicated downstream load.
 * ================================================================
 */
object SldDesignValidator {

    enum class Severity {
        ERROR,
        WARNING,
        INFORMATION
    }

    data class Issue(
        val severity: Severity,
        val code: String,
        val elementId: String?,
        val message: String
    )

    data class Result(
        val valid: Boolean,
        val errors: List<Issue>,
        val warnings: List<Issue>,
        val information: List<Issue>
    ) {
        val allIssues: List<Issue>
            get() =
                errors +
                    warnings +
                    information
    }

    /**
     * Main validation entry point.
     */
    fun validate(
        network: SldNetwork
    ): Result {

        val errors =
            mutableListOf<Issue>()

        val warnings =
            mutableListOf<Issue>()

        val information =
            mutableListOf<Issue>()

        /*
         * ------------------------------------------------------------
         * EMPTY NETWORK
         * ------------------------------------------------------------
         */
        if (network.nodes.isEmpty()) {

            errors +=
                Issue(
                    severity = Severity.ERROR,
                    code = "SLD_EMPTY",
                    elementId = null,
                    message =
                        "SLD network contains no equipment."
                )

            return Result(
                valid = false,
                errors = errors,
                warnings = warnings,
                information = information
            )
        }

        /*
         * ------------------------------------------------------------
         * NODE IDS
         * ------------------------------------------------------------
         */
        val nodeIds =
            network.nodes.map {
                it.id
            }

        val duplicateNodeIds =
            nodeIds
                .groupingBy {
                    it
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys

        duplicateNodeIds.forEach { id ->

            errors +=
                Issue(
                    severity = Severity.ERROR,
                    code = "DUPLICATE_NODE_ID",
                    elementId = id,
                    message =
                        "Duplicate SLD node ID: $id"
                )
        }

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        /*
         * ------------------------------------------------------------
         * CONNECTION IDS
         * ------------------------------------------------------------
         */
        val connectionIds =
            network.connections.map {
                it.id
            }

        val duplicateConnectionIds =
            connectionIds
                .groupingBy {
                    it
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys

        duplicateConnectionIds.forEach { id ->

            errors +=
                Issue(
                    severity = Severity.ERROR,
                    code = "DUPLICATE_CONNECTION_ID",
                    elementId = id,
                    message =
                        "Duplicate SLD connection ID: $id"
                )
        }

        /*
         * ------------------------------------------------------------
         * SOURCES
         * ------------------------------------------------------------
         */
        val sources =
            network.nodes.filter {
                it.type == SldNodeType.SOURCE
            }

        if (sources.isEmpty()) {

            errors +=
                Issue(
                    severity = Severity.ERROR,
                    code = "NO_SOURCE",
                    elementId = null,
                    message =
                        "The electrical network has no source."
                )
        }

        if (sources.size > 1) {

            warnings +=
                Issue(
                    severity = Severity.WARNING,
                    code = "MULTIPLE_SOURCES",
                    elementId = null,
                    message =
                        "Multiple electrical sources detected. Operating mode and source interlocking must be defined."
                )
        }

        /*
         * ------------------------------------------------------------
         * TOPOLOGY COUNTERS
         * ------------------------------------------------------------
         */
        val incoming =
            mutableMapOf<String, Int>()

        val outgoing =
            mutableMapOf<String, Int>()

        network.nodes.forEach { node ->

            incoming[node.id] = 0
            outgoing[node.id] = 0
        }

        /*
         * ------------------------------------------------------------
         * CONNECTION VALIDATION
         * ------------------------------------------------------------
         */
        network.connections.forEach { connection ->

            val fromNode =
                nodeMap[connection.fromNodeId]

            val toNode =
                nodeMap[connection.toNodeId]

            /*
             * Invalid source node.
             */
            if (fromNode == null) {

                errors +=
                    Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_FROM_NODE",
                        elementId = connection.id,
                        message =
                            "Connection source node does not exist."
                    )
            }

            /*
             * Invalid destination node.
             */
            if (toNode == null) {

                errors +=
                    Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_TO_NODE",
                        elementId = connection.id,
                        message =
                            "Connection destination node does not exist."
                    )
            }

            /*
             * Self connection.
             */
            if (
                connection.fromNodeId ==
                    connection.toNodeId
            ) {

                errors +=
                    Issue(
                        severity = Severity.ERROR,
                        code = "SELF_CONNECTION",
                        elementId = connection.id,
                        message =
                            "An SLD element cannot be connected to itself."
                    )
            }

            /*
             * --------------------------------------------------------
             * BUSBAR SEMANTICS
             * --------------------------------------------------------
             *
             * BUSBAR represents the internal bus connection inside
             * a panel. It must not silently become a cable.
             */
            if (
                connection.connectionType ==
                    SldConnectionType.BUSBAR
            ) {

                /*
                 * Current supported topology:
                 *
                 * PANEL -> BREAKER
                 *
                 * This prevents accidental use of BUSBAR for:
                 * SOURCE -> PANEL
                 * PANEL -> PANEL
                 * BREAKER -> LOAD
                 * TRANSFORMER -> PANEL
                 * etc.
                 */
                if (
                    fromNode != null &&
                    toNode != null &&
                    (
                        fromNode.type != SldNodeType.PANEL ||
                            toNode.type != SldNodeType.BREAKER
                        )
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "INVALID_BUSBAR_TOPOLOGY",
                            elementId = connection.id,
                            message =
                                "BUSBAR connection is only valid for an internal PANEL -> BREAKER connection."
                        )
                }

                /*
                 * BUSBAR must not contain cable engineering data.
                 *
                 * These values are deliberately ignored by the
                 * engineering calculation for BUSBAR connections.
                 */
                if (
                    connection.lengthMeters != 0.0
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "BUSBAR_HAS_CABLE_LENGTH",
                            elementId = connection.id,
                            message =
                                "BUSBAR connection must not contain cable length."
                        )
                }

                if (
                    connection.cableSizeMm2 != 0.0
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "BUSBAR_HAS_CABLE_SIZE",
                            elementId = connection.id,
                            message =
                                "BUSBAR connection must not contain cable cross-sectional area."
                        )
                }

                if (
                    connection.currentCapacityA != 0.0
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "BUSBAR_HAS_CABLE_CAPACITY",
                            elementId = connection.id,
                            message =
                                "BUSBAR connection must not contain cable current capacity."
                        )
                }

                if (
                    connection.parallelRuns != 1
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "BUSBAR_HAS_PARALLEL_RUNS",
                            elementId = connection.id,
                            message =
                                "BUSBAR connection must not contain cable parallel-run data."
                        )
                }

                /*
                 * BUSBAR electrical rating.
                 */
                if (
                    connection.busbarRatedCurrentA <= 0.0
                ) {

                    warnings +=
                        Issue(
                            severity = Severity.WARNING,
                            code = "BUSBAR_RATING_NOT_VERIFIED",
                            elementId = connection.id,
                            message =
                                "BUSBAR rated current is not defined; busbar adequacy cannot be verified."
                        )
                }

                if (
                    connection.busbarShortCircuitKA <= 0.0
                ) {

                    warnings +=
                        Issue(
                            severity = Severity.WARNING,
                            code = "BUSBAR_SHORT_CIRCUIT_NOT_VERIFIED",
                            elementId = connection.id,
                            message =
                                "BUSBAR short-circuit withstand rating is not defined; busbar fault-duty adequacy cannot be verified."
                        )
                }
            }

            /*
             * --------------------------------------------------------
             * CABLE SEMANTICS
             * --------------------------------------------------------
             */
            if (
                connection.connectionType ==
                    SldConnectionType.CABLE
            ) {

                /*
                 * External feeder cable requires a valid run count.
                 */
                if (
                    connection.parallelRuns < 1
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "INVALID_PARALLEL_RUNS",
                            elementId = connection.id,
                            message =
                                "Parallel cable runs must be at least 1."
                        )
                }

                /*
                 * Cable length must be non-negative.
                 */
                if (
                    connection.lengthMeters < 0.0
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "INVALID_LENGTH",
                            elementId = connection.id,
                            message =
                                "Feeder cable length cannot be negative."
                        )
                }

                /*
                 * Cable size must be non-negative.
                 */
                if (
                    connection.cableSizeMm2 < 0.0
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "INVALID_CABLE_SIZE",
                            elementId = connection.id,
                            message =
                                "Cable cross-sectional area cannot be negative."
                        )
                }

                /*
                 * Cable capacity must be non-negative.
                 */
                if (
                    connection.currentCapacityA < 0.0
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "INVALID_CABLE_CAPACITY",
                            elementId = connection.id,
                            message =
                                "Cable current capacity cannot be negative."
                        )
                }

                /*
                 * Zero capacity is NOT an adequate cable.
                 *
                 * It means that the cable has not yet been verified
                 * or sized.
                 */
                if (
                    connection.currentCapacityA <= 0.0
                ) {

                    warnings +=
                        Issue(
                            severity = Severity.WARNING,
                            code = "CABLE_CAPACITY_NOT_VERIFIED",
                            elementId = connection.id,
                            message =
                                "Cable current capacity is not verified. Cable adequacy cannot be confirmed."
                        )
                }

                /*
                 * Zero cable size is not an engineering verification.
                 */
                if (
                    connection.cableSizeMm2 <= 0.0
                ) {

                    warnings +=
                        Issue(
                            severity = Severity.WARNING,
                            code = "CABLE_SIZE_NOT_VERIFIED",
                            elementId = connection.id,
                            message =
                                "Cable cross-sectional area is not verified."
                        )
                }

                /*
                 * Zero length may be valid for a temporary/modeling
                 * condition, so it is not an error.
                 */
                if (
                    connection.lengthMeters == 0.0
                ) {

                    warnings +=
                        Issue(
                            severity = Severity.WARNING,
                            code = "CABLE_LENGTH_NOT_VERIFIED",
                            elementId = connection.id,
                            message =
                                "Cable feeder length is zero; voltage-drop and impedance verification may be incomplete."
                        )
                }
            }

            /*
             * --------------------------------------------------------
             * TOPOLOGY COUNTERS
             * --------------------------------------------------------
             *
             * Only count existing nodes. Invalid references must not
             * corrupt the graph counters.
             */
            if (
                fromNode != null &&
                fromNode.id in outgoing
            ) {

                outgoing[
                    fromNode.id
                ] =
                    outgoing[
                        fromNode.id
                    ]!! + 1
            }

            if (
                toNode != null &&
                toNode.id in incoming
            ) {

                incoming[
                    toNode.id
                ] =
                    incoming[
                        toNode.id
                    ]!! + 1
            }
        }

        /*
         * ------------------------------------------------------------
         * NODE ENGINEERING INPUT VALIDATION
         * ------------------------------------------------------------
         */
        network.nodes.forEach { node ->

            /*
             * Voltage.
             */
            if (
                node.voltage <= 0.0
            ) {

                errors +=
                    Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_VOLTAGE",
                        elementId = node.id,
                        message =
                            "${node.name}: invalid voltage."
                    )
            }

            /*
             * Power factor.
             *
             * Non-load equipment may still carry the default PF,
             * therefore this remains a general input validation.
             */
            if (
                node.powerFactor <= 0.0 ||
                node.powerFactor > 1.0
            ) {

                errors +=
                    Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_POWER_FACTOR",
                        elementId = node.id,
                        message =
                            "${node.name}: power factor must be between 0 and 1."
                    )
            }

            /*
             * Demand factor.
             */
            if (
                node.demandFactor < 0.0 ||
                node.demandFactor > 1.0
            ) {

                errors +=
                    Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_DEMAND_FACTOR",
                        elementId = node.id,
                        message =
                            "${node.name}: demand factor must be between 0 and 1."
                    )
            }

            /*
             * Load.
             */
            if (
                node.loadKw < 0.0
            ) {

                errors +=
                    Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_LOAD",
                        elementId = node.id,
                        message =
                            "${node.name}: load cannot be negative."
                    )
            }

            /*
             * Rated kVA.
             */
            if (
                node.ratedKva < 0.0
            ) {

                errors +=
                    Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_RATED_KVA",
                        elementId = node.id,
                        message =
                            "${node.name}: rated kVA cannot be negative."
                    )
            }

            /*
             * --------------------------------------------------------
             * FEEDING TOPOLOGY
             * --------------------------------------------------------
             */
            if (
                node.type != SldNodeType.SOURCE &&
                incoming[node.id] == 0
            ) {

                warnings +=
                    Issue(
                        severity = Severity.WARNING,
                        code = "UNFED_EQUIPMENT",
                        elementId = node.id,
                        message =
                            "${node.name}: no upstream connection."
                    )
            }

            /*
             * Loads are terminal elements.
             *
             * SOURCE is excluded because it is an upstream source.
             */
            if (
                node.type != SldNodeType.LOAD &&
                node.type != SldNodeType.SOURCE &&
                outgoing[node.id] == 0
            ) {

                warnings +=
                    Issue(
                        severity = Severity.WARNING,
                        code = "DOWNSTREAM_OPEN",
                        elementId = node.id,
                        message =
                            "${node.name}: no downstream connection."
                    )
            }

            /*
             * --------------------------------------------------------
             * TYPE-SPECIFIC VALIDATION
             * --------------------------------------------------------
             */
            when (node.type) {

                /*
                 * ----------------------------------------------------
                 * SOURCE
                 * ----------------------------------------------------
                 */
                SldNodeType.SOURCE -> {

                    if (
                        node.sourceShortCircuitMva <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity = Severity.WARNING,
                                code = "SOURCE_FAULT_LEVEL_MISSING",
                                elementId = node.id,
                                message =
                                    "${node.name}: source short-circuit level is not defined."
                            )
                    }
                }

                /*
                 * ----------------------------------------------------
                 * TRANSFORMER
                 * ----------------------------------------------------
                 */
                SldNodeType.TRANSFORMER -> {

                    if (
                        node.ratedKva <= 0.0
                    ) {

                        errors +=
                            Issue(
                                severity = Severity.ERROR,
                                code = "TRANSFORMER_RATING_MISSING",
                                elementId = node.id,
                                message =
                                    "${node.name}: transformer rating is missing."
                            )
                    }

                    if (
                        node.transformerPercentZ <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity = Severity.WARNING,
                                code = "TRANSFORMER_Z_MISSING",
                                elementId = node.id,
                                message =
                                    "${node.name}: transformer impedance (%Z) is missing."
                            )
                    }
                }

                /*
                 * ----------------------------------------------------
                 * GENERATOR
                 * ----------------------------------------------------
                 */
                SldNodeType.GENERATOR -> {

                    if (
                        node.ratedKva <= 0.0
                    ) {

                        errors +=
                            Issue(
                                severity = Severity.ERROR,
                                code = "GENERATOR_RATING_MISSING",
                                elementId = node.id,
                                message =
                                    "${node.name}: generator rating is missing."
                            )
                    }

                    if (
                        node.generatorXdSubtransient <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity = Severity.WARNING,
                                code = "GENERATOR_XD_MISSING",
                                elementId = node.id,
                                message =
                                    "${node.name}: generator Xd'' is missing."
                            )
                    }
                }

                /*
                 * ----------------------------------------------------
                 * BREAKER
                 * ----------------------------------------------------
                 */
                SldNodeType.BREAKER -> {

                    if (
                        node.ratedKva <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity = Severity.WARNING,
                                code = "BREAKER_RATING_MISSING",
                                elementId = node.id,
                                message =
                                    "${node.name}: breaker rating has not been defined."
                            )
                    }
                }

                /*
                 * ----------------------------------------------------
                 * PANEL
                 * ----------------------------------------------------
                 */
                SldNodeType.PANEL -> {

                    if (
                        node.ratedKva <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity = Severity.WARNING,
                                code = "PANEL_RATING_MISSING",
                                elementId = node.id,
                                message =
                                    "${node.name}: panel rating has not been defined."
                            )
                    }
                }

                /*
                 * ----------------------------------------------------
                 * LOAD
                 * ----------------------------------------------------
                 */
                SldNodeType.LOAD -> {

                    if (
                        node.loadKw <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity = Severity.WARNING,
                                code = "ZERO_LOAD",
                                elementId = node.id,
                                message =
                                    "${node.name}: load is zero."
                            )
                    }
                }

                /*
                 * ----------------------------------------------------
                 * BUS
                 * ----------------------------------------------------
                 */
                SldNodeType.BUS -> Unit
            }
        }

        /*
         * ------------------------------------------------------------
         * DUPLICATE DIRECTED FEEDERS
         * ------------------------------------------------------------
         *
         * Multiple feeders between the same two elements are
         * reported as information because parallel feeder modeling
         * can be intentional.
         */
        network.connections
            .groupBy {
                "${it.fromNodeId}->${it.toNodeId}"
            }
            .filterValues {
                it.size > 1
            }
            .forEach { (_, connections) ->

                information +=
                    Issue(
                        severity = Severity.INFORMATION,
                        code = "PARALLEL_FEEDERS",
                        elementId = connections.first().id,
                        message =
                            "Multiple feeders exist between the same two SLD elements."
                    )
            }

        /*
         * ------------------------------------------------------------
         * REVERSE-DIRECTION DUPLICATE
         * ------------------------------------------------------------
         *
         * A radial SLD should normally have one directed feeder
         * between two nodes. A reverse pair is suspicious and can
         * also indicate an unintended loop.
         */
        network.connections
            .forEach { connection ->

                val reverseExists =
                    network.connections.any {
                        it.id != connection.id &&
                            it.fromNodeId ==
                                connection.toNodeId &&
                            it.toNodeId ==
                                connection.fromNodeId
                    }

                if (reverseExists) {

                    warnings +=
                        Issue(
                            severity = Severity.WARNING,
                            code = "REVERSE_FEEDER_PAIR",
                            elementId = connection.id,
                            message =
                                "A reverse-direction feeder exists between the same two SLD elements. Verify the intended electrical direction."
                        )
                }
            }

        /*
         * ------------------------------------------------------------
         * GRAPH
         * ------------------------------------------------------------
         */
        val children =
            mutableMapOf<
                String,
                MutableList<String>
            >()

        network.nodes.forEach { node ->

            children[node.id] =
                mutableListOf()
        }

        /*
         * Only valid node references are inserted into the graph.
         */
        network.connections.forEach { connection ->

            if (
                connection.fromNodeId in nodeMap &&
                connection.toNodeId in nodeMap &&
                connection.fromNodeId !=
                    connection.toNodeId
            ) {

                children[
                    connection.fromNodeId
                ]?.add(
                    connection.toNodeId
                )
            }
        }

        /*
         * ------------------------------------------------------------
         * CYCLE DETECTION
         * ------------------------------------------------------------
         */
        val visiting =
            mutableSetOf<String>()

        val visited =
            mutableSetOf<String>()

        val reportedCycles =
            mutableSetOf<String>()

        fun visit(
            id: String
        ) {

            if (
                id in visiting
            ) {

                if (
                    reportedCycles.add(id)
                ) {

                    errors +=
                        Issue(
                            severity = Severity.ERROR,
                            code = "NETWORK_CYCLE",
                            elementId = id,
                            message =
                                "Circular electrical topology detected."
                        )
                }

                return
            }

            if (
                id in visited
            ) {
                return
            }

            visiting += id

            children[id]
                .orEmpty()
                .forEach(
                    ::visit
                )

            visiting -= id
            visited += id
        }

        network.nodes.forEach { node ->

            visit(node.id)
        }

        /*
         * ------------------------------------------------------------
         * SOURCE REACHABILITY
         * ------------------------------------------------------------
         */
        if (
            sources.isNotEmpty()
        ) {

            val reachable =
                mutableSetOf<String>()

            fun walk(
                id: String
            ) {

                if (
                    !reachable.add(id)
                ) {
                    return
                }

                children[id]
                    .orEmpty()
                    .forEach(
                        ::walk
                    )
            }

            sources.forEach { source ->

                walk(source.id)
            }

            network.nodes
                .filter {
                    it.id !in reachable
                }
                .forEach { node ->

                    warnings +=
                        Issue(
                            severity = Severity.WARNING,
                            code = "UNREACHABLE_ELEMENT",
                            elementId = node.id,
                            message =
                                "${node.name}: element is not connected to an electrical source."
                        )
                }
        }

        /*
         * ------------------------------------------------------------
         * TOPOLOGY QUALITY INFORMATION
         * ------------------------------------------------------------
         */
        val loadCount =
            network.nodes.count {
                it.type == SldNodeType.LOAD
            }

        val panelCount =
            network.nodes.count {
                it.type == SldNodeType.PANEL
            }

        val breakerCount =
            network.nodes.count {
                it.type == SldNodeType.BREAKER
            }

        val busbarCount =
            network.connections.count {
                it.connectionType ==
                    SldConnectionType.BUSBAR
            }

        val cableCount =
            network.connections.count {
                it.connectionType ==
                    SldConnectionType.CABLE
            }

        information +=
            Issue(
                severity = Severity.INFORMATION,
                code = "NETWORK_SUMMARY",
                elementId = null,
                message =
                    "SLD contains ${network.nodes.size} equipment elements, " +
                        "$loadCount loads, " +
                        "$panelCount panels, " +
                        "$breakerCount breakers, " +
                        "$cableCount cable feeders and " +
                        "$busbarCount busbar connections."
            )

        /*
         * ------------------------------------------------------------
         * FINAL RESULT
         * ------------------------------------------------------------
         *
         * Warnings do not make the topology invalid.
         *
         * Errors prevent the engineering calculation pipeline from
         * treating the SLD as structurally valid.
         */
        return Result(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            information = information
        )
    }
}
