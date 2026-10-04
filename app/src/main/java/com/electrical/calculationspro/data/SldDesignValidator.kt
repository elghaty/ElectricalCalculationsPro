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
 * BUSBAR is an internal switchgear relationship:
 *   PANEL ↔ BUS
 *   BUS ↔ BREAKER
 *   PANEL ↔ BREAKER
 *
 * BUSBAR never carries cable engineering data.
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
            get() = errors + warnings + information
    }

    private fun isInternalBusbarPair(
        first: SldNodeType,
        second: SldNodeType
    ): Boolean {
        return (
            first == SldNodeType.PANEL &&
                second == SldNodeType.BUS
            ) || (
            first == SldNodeType.BUS &&
                second == SldNodeType.PANEL
            ) || (
            first == SldNodeType.BUS &&
                second == SldNodeType.BREAKER
            ) || (
            first == SldNodeType.BREAKER &&
                second == SldNodeType.BUS
            ) || (
            first == SldNodeType.PANEL &&
                second == SldNodeType.BREAKER
            ) || (
            first == SldNodeType.BREAKER &&
                second == SldNodeType.PANEL
            )
    }

    fun validate(
        network: SldNetwork
    ): Result {

        val errors = mutableListOf<Issue>()
        val warnings = mutableListOf<Issue>()
        val information = mutableListOf<Issue>()

        if (network.nodes.isEmpty()) {
            errors += Issue(
                severity = Severity.ERROR,
                code = "SLD_EMPTY",
                elementId = null,
                message = "SLD network contains no equipment."
            )

            return Result(
                valid = false,
                errors = errors,
                warnings = warnings,
                information = information
            )
        }

        val nodeIds = network.nodes.map { it.id }

        nodeIds
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { id ->
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "DUPLICATE_NODE_ID",
                    elementId = id,
                    message = "Duplicate SLD node ID: $id"
                )
            }

        val nodeMap =
            network.nodes.associateBy { it.id }

        network.connections
            .map { it.id }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { id ->
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "DUPLICATE_CONNECTION_ID",
                    elementId = id,
                    message = "Duplicate SLD connection ID: $id"
                )
            }

        val sources =
            network.nodes.filter {
                it.type == SldNodeType.SOURCE
            }

        if (sources.isEmpty()) {
            errors += Issue(
                severity = Severity.ERROR,
                code = "NO_SOURCE",
                elementId = null,
                message = "The electrical network has no source."
            )
        }

        if (sources.size > 1) {
            warnings += Issue(
                severity = Severity.WARNING,
                code = "MULTIPLE_SOURCES",
                elementId = null,
                message =
                    "Multiple electrical sources detected. Operating mode and source interlocking must be defined."
            )
        }

        val incoming = mutableMapOf<String, Int>()
        val outgoing = mutableMapOf<String, Int>()

        network.nodes.forEach { node ->
            incoming[node.id] = 0
            outgoing[node.id] = 0
        }

        network.connections.forEach { connection ->

            val fromNode =
                nodeMap[connection.fromNodeId]

            val toNode =
                nodeMap[connection.toNodeId]

            if (fromNode == null) {
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "INVALID_FROM_NODE",
                    elementId = connection.id,
                    message = "Connection source node does not exist."
                )
            }

            if (toNode == null) {
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "INVALID_TO_NODE",
                    elementId = connection.id,
                    message = "Connection destination node does not exist."
                )
            }

            if (
                connection.fromNodeId ==
                    connection.toNodeId
            ) {
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "SELF_CONNECTION",
                    elementId = connection.id,
                    message =
                        "An SLD element cannot be connected to itself."
                )
            }

            if (
                connection.connectionType ==
                    SldConnectionType.BUSBAR
            ) {

                if (
                    fromNode != null &&
                    toNode != null &&
                    !isInternalBusbarPair(
                        fromNode.type,
                        toNode.type
                    )
                ) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_BUSBAR_TOPOLOGY",
                        elementId = connection.id,
                        message =
                            "BUSBAR connection is only valid inside panel switchgear: PANEL-BUS, BUS-BREAKER, or PANEL-BREAKER."
                    )
                }

                if (connection.lengthMeters != 0.0) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "BUSBAR_HAS_CABLE_LENGTH",
                        elementId = connection.id,
                        message =
                            "BUSBAR connection must not contain cable length."
                    )
                }

                if (connection.cableSizeMm2 != 0.0) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "BUSBAR_HAS_CABLE_SIZE",
                        elementId = connection.id,
                        message =
                            "BUSBAR connection must not contain cable cross-sectional area."
                    )
                }

                if (connection.currentCapacityA != 0.0) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "BUSBAR_HAS_CABLE_CAPACITY",
                        elementId = connection.id,
                        message =
                            "BUSBAR connection must not contain cable current capacity."
                    )
                }

                if (connection.parallelRuns != 1) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "BUSBAR_HAS_PARALLEL_RUNS",
                        elementId = connection.id,
                        message =
                            "BUSBAR connection must not contain cable parallel-run data."
                    )
                }

                if (connection.busbarRatedCurrentA <= 0.0) {
                    warnings += Issue(
                        severity = Severity.WARNING,
                        code = "BUSBAR_RATING_NOT_VERIFIED",
                        elementId = connection.id,
                        message =
                            "BUSBAR rated current is not defined; busbar adequacy cannot be verified."
                    )
                }

                if (connection.busbarShortCircuitKA <= 0.0) {
                    warnings += Issue(
                        severity = Severity.WARNING,
                        code = "BUSBAR_SHORT_CIRCUIT_NOT_VERIFIED",
                        elementId = connection.id,
                        message =
                            "BUSBAR short-circuit withstand rating is not defined; busbar fault-duty adequacy cannot be verified."
                    )
                }
            }

            if (
                connection.connectionType ==
                    SldConnectionType.CABLE
            ) {

                if (connection.parallelRuns < 1) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_PARALLEL_RUNS",
                        elementId = connection.id,
                        message =
                            "Parallel cable runs must be at least 1."
                    )
                }

                if (connection.lengthMeters < 0.0) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_LENGTH",
                        elementId = connection.id,
                        message =
                            "Feeder cable length cannot be negative."
                    )
                }

                if (connection.cableSizeMm2 < 0.0) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_CABLE_SIZE",
                        elementId = connection.id,
                        message =
                            "Cable cross-sectional area cannot be negative."
                    )
                }

                if (connection.currentCapacityA < 0.0) {
                    errors += Issue(
                        severity = Severity.ERROR,
                        code = "INVALID_CABLE_CAPACITY",
                        elementId = connection.id,
                        message =
                            "Cable current capacity cannot be negative."
                    )
                }

                if (connection.currentCapacityA <= 0.0) {
                    warnings += Issue(
                        severity = Severity.WARNING,
                        code = "CABLE_CAPACITY_NOT_VERIFIED",
                        elementId = connection.id,
                        message =
                            "Cable current capacity is not verified. Cable adequacy cannot be confirmed."
                    )
                }

                if (connection.cableSizeMm2 <= 0.0) {
                    warnings += Issue(
                        severity = Severity.WARNING,
                        code = "CABLE_SIZE_NOT_VERIFIED",
                        elementId = connection.id,
                        message =
                            "Cable cross-sectional area is not verified."
                    )
                }

                if (connection.lengthMeters == 0.0) {
                    warnings += Issue(
                        severity = Severity.WARNING,
                        code = "CABLE_LENGTH_NOT_VERIFIED",
                        elementId = connection.id,
                        message =
                            "Cable feeder length is zero; voltage-drop and impedance verification may be incomplete."
                    )
                }
            }

            if (fromNode != null) {
                outgoing[fromNode.id] =
                    (outgoing[fromNode.id] ?: 0) + 1
            }

            if (toNode != null) {
                incoming[toNode.id] =
                    (incoming[toNode.id] ?: 0) + 1
            }
        }

        network.nodes.forEach { node ->

            if (node.voltage <= 0.0) {
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "INVALID_VOLTAGE",
                    elementId = node.id,
                    message = "${node.name}: invalid voltage."
                )
            }

            if (
                node.powerFactor <= 0.0 ||
                node.powerFactor > 1.0
            ) {
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "INVALID_POWER_FACTOR",
                    elementId = node.id,
                    message =
                        "${node.name}: power factor must be between 0 and 1."
                )
            }

            if (
                node.demandFactor < 0.0 ||
                node.demandFactor > 1.0
            ) {
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "INVALID_DEMAND_FACTOR",
                    elementId = node.id,
                    message =
                        "${node.name}: demand factor must be between 0 and 1."
                )
            }

            if (node.loadKw < 0.0) {
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "INVALID_LOAD",
                    elementId = node.id,
                    message =
                        "${node.name}: load cannot be negative."
                )
            }

            if (node.ratedKva < 0.0) {
                errors += Issue(
                    severity = Severity.ERROR,
                    code = "INVALID_RATED_KVA",
                    elementId = node.id,
                    message =
                        "${node.name}: rated kVA cannot be negative."
                )
            }

            when (node.type) {

                SldNodeType.TRANSFORMER -> {
                    if (
                        node.ratedKva <= 0.0
                    ) {
                        warnings += Issue(
                            severity = Severity.WARNING,
                            code = "TRANSFORMER_RATING_NOT_DEFINED",
                            elementId = node.id,
                            message =
                                "${node.name}: transformer rating is not defined."
                        )
                    }

                    if (
                        node.transformerPercentZ <= 0.0
                    ) {
                        warnings += Issue(
                            severity = Severity.WARNING,
                            code = "TRANSFORMER_IMPEDANCE_NOT_DEFINED",
                            elementId = node.id,
                            message =
                                "${node.name}: transformer impedance is not defined."
                        )
                    }
                }

                SldNodeType.GENERATOR -> {
                    if (
                        node.ratedKva <= 0.0
                    ) {
                        warnings += Issue(
                            severity = Severity.WARNING,
                            code = "GENERATOR_RATING_NOT_DEFINED",
                            elementId = node.id,
                            message =
                                "${node.name}: generator rating is not defined."
                        )
                    }

                    if (
                        node.generatorXdSubtransient <= 0.0
                    ) {
                        warnings += Issue(
                            severity = Severity.WARNING,
                            code = "GENERATOR_XD_NOT_DEFINED",
                            elementId = node.id,
                            message =
                                "${node.name}: generator sub-transient reactance is not defined."
                        )
                    }
                }

                SldNodeType.SOURCE -> {
                    if (
                        node.sourceShortCircuitMva < 0.0
                    ) {
                        errors += Issue(
                            severity = Severity.ERROR,
                            code = "INVALID_SOURCE_SHORT_CIRCUIT",
                            elementId = node.id,
                            message =
                                "${node.name}: source short-circuit MVA cannot be negative."
                        )
                    }
                }

                else -> Unit
            }
        }

        network.nodes.forEach { node ->

            val inCount =
                incoming[node.id] ?: 0

            val outCount =
                outgoing[node.id] ?: 0

            when (node.type) {

                SldNodeType.SOURCE -> {
                    if (inCount > 0) {
                        warnings += Issue(
                            severity = Severity.WARNING,
                            code = "SOURCE_HAS_INCOMING",
                            elementId = node.id,
                            message =
                                "${node.name}: source has an incoming connection."
                        )
                    }
                }

                SldNodeType.LOAD -> {
                    if (inCount == 0) {
                        warnings += Issue(
                            severity = Severity.WARNING,
                            code = "LOAD_NOT_CONNECTED",
                            elementId = node.id,
                            message =
                                "${node.name}: load is not connected to an upstream element."
                        )
                    }
                }

                SldNodeType.BREAKER -> {
                    if (inCount == 0 && outCount == 0) {
                        information += Issue(
                            severity = Severity.INFORMATION,
                            code = "BREAKER_ORPHAN",
                            elementId = node.id,
                            message =
                                "${node.name}: breaker is currently unconnected."
                        )
                    }
                }

                SldNodeType.PANEL -> {
                    if (inCount == 0 && outCount == 0) {
                        information += Issue(
                            severity = Severity.INFORMATION,
                            code = "PANEL_ORPHAN",
                            elementId = node.id,
                            message =
                                "${node.name}: panel is currently unconnected."
                        )
                    }
                }

                SldNodeType.BUS -> {
                    if (inCount == 0 && outCount == 0) {
                        information += Issue(
                            severity = Severity.INFORMATION,
                            code = "BUS_ORPHAN",
                            elementId = node.id,
                            message =
                                "${node.name}: busbar is currently unconnected."
                        )
                    }
                }

                else -> Unit
            }
        }

        val fatalErrors =
            errors.filter {
                it.code != "BREAKER_ORPHAN"
            }

        return Result(
            valid = fatalErrors.isEmpty(),
            errors = errors,
            warnings = warnings,
            information = information
        )
    }
}
