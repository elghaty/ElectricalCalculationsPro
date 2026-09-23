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
 * The validator does not calculate equipment ratings.
 * It verifies that the network is logically valid and ready
 * for engineering calculations.
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

    fun validate(
        network: SldNetwork
    ): Result {

        val errors =
            mutableListOf<Issue>()

        val warnings =
            mutableListOf<Issue>()

        val information =
            mutableListOf<Issue>()

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

        val nodeIds =
            network.nodes.map {
                it.id
            }

        val duplicateIds =
            nodeIds
                .groupingBy {
                    it
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys

        duplicateIds.forEach { id ->

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

        val sources =
            network.nodes.filter {
                it.type ==
                    SldNodeType.SOURCE
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

        val incoming =
            mutableMapOf<
                String,
                Int
            >()

        val outgoing =
            mutableMapOf<
                String,
                Int
            >()

        network.nodes.forEach { node ->

            incoming[node.id] = 0
            outgoing[node.id] = 0
        }

        network.connections.forEach { connection ->

            if (
                connection.fromNodeId !in nodeMap
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_FROM_NODE",
                        elementId =
                            connection.id,
                        message =
                            "Connection source node does not exist."
                    )
            }

            if (
                connection.toNodeId !in nodeMap
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_TO_NODE",
                        elementId =
                            connection.id,
                        message =
                            "Connection destination node does not exist."
                    )
            }

            if (
                connection.fromNodeId ==
                    connection.toNodeId
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "SELF_CONNECTION",
                        elementId =
                            connection.id,
                        message =
                            "An SLD element cannot be connected to itself."
                    )
            }

            if (
                connection.parallelRuns < 1
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_PARALLEL_RUNS",
                        elementId =
                            connection.id,
                        message =
                            "Parallel cable runs must be at least 1."
                    )
            }

            if (
                connection.lengthMeters < 0.0
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_LENGTH",
                        elementId =
                            connection.id,
                        message =
                            "Feeder length cannot be negative."
                    )
            }

            if (
                connection.cableSizeMm2 < 0.0
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_CABLE_SIZE",
                        elementId =
                            connection.id,
                        message =
                            "Cable cross-sectional area cannot be negative."
                    )
            }

            if (
                connection.currentCapacityA < 0.0
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_CABLE_CAPACITY",
                        elementId =
                            connection.id,
                        message =
                            "Cable current capacity cannot be negative."
                    )
            }

            if (
                connection.fromNodeId in nodeMap
            ) {

                outgoing[
                    connection.fromNodeId
                ] =
                    outgoing[
                        connection.fromNodeId
                    ]!! + 1
            }

            if (
                connection.toNodeId in nodeMap
            ) {

                incoming[
                    connection.toNodeId
                ] =
                    incoming[
                        connection.toNodeId
                    ]!! + 1
            }
        }

        network.nodes.forEach { node ->

            if (
                node.voltage <= 0.0
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_VOLTAGE",
                        elementId =
                            node.id,
                        message =
                            "${node.name}: invalid voltage."
                    )
            }

            if (
                node.powerFactor <= 0.0 ||
                node.powerFactor > 1.0
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_POWER_FACTOR",
                        elementId =
                            node.id,
                        message =
                            "${node.name}: power factor must be between 0 and 1."
                    )
            }

            if (
                node.demandFactor < 0.0 ||
                node.demandFactor > 1.0
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_DEMAND_FACTOR",
                        elementId =
                            node.id,
                        message =
                            "${node.name}: demand factor must be between 0 and 1."
                    )
            }

            if (
                node.loadKw < 0.0
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_LOAD",
                        elementId =
                            node.id,
                        message =
                            "${node.name}: load cannot be negative."
                    )
            }

            if (
                node.ratedKva < 0.0
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "INVALID_RATED_KVA",
                        elementId =
                            node.id,
                        message =
                            "${node.name}: rated kVA cannot be negative."
                    )
            }

            if (
                node.type !=
                    SldNodeType.SOURCE &&
                incoming[node.id] == 0
            ) {

                warnings +=
                    Issue(
                        severity =
                            Severity.WARNING,
                        code =
                            "UNFED_EQUIPMENT",
                        elementId =
                            node.id,
                        message =
                            "${node.name}: no upstream connection."
                    )
            }

            if (
                node.type !=
                    SldNodeType.LOAD &&
                outgoing[node.id] == 0 &&
                node.type !=
                    SldNodeType.SOURCE
            ) {

                warnings +=
                    Issue(
                        severity =
                            Severity.WARNING,
                        code =
                            "DOWNSTREAM_OPEN",
                        elementId =
                            node.id,
                        message =
                            "${node.name}: no downstream connection."
                    )
            }

            when (node.type) {

                SldNodeType.SOURCE -> {

                    if (
                        node.sourceShortCircuitMva <=
                        0.0
                    ) {

                        warnings +=
                            Issue(
                                severity =
                                    Severity.WARNING,
                                code =
                                    "SOURCE_FAULT_LEVEL_MISSING",
                                elementId =
                                    node.id,
                                message =
                                    "${node.name}: source short-circuit level is not defined."
                            )
                    }
                }

                SldNodeType.TRANSFORMER -> {

                    if (
                        node.ratedKva <= 0.0
                    ) {

                        errors +=
                            Issue(
                                severity =
                                    Severity.ERROR,
                                code =
                                    "TRANSFORMER_RATING_MISSING",
                                elementId =
                                    node.id,
                                message =
                                    "${node.name}: transformer rating is missing."
                            )
                    }

                    if (
                        node.transformerPercentZ <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity =
                                    Severity.WARNING,
                                code =
                                    "TRANSFORMER_Z_MISSING",
                                elementId =
                                    node.id,
                                message =
                                    "${node.name}: transformer impedance (%Z) is missing."
                            )
                    }
                }

                SldNodeType.GENERATOR -> {

                    if (
                        node.ratedKva <= 0.0
                    ) {

                        errors +=
                            Issue(
                                severity =
                                    Severity.ERROR,
                                code =
                                    "GENERATOR_RATING_MISSING",
                                elementId =
                                    node.id,
                                message =
                                    "${node.name}: generator rating is missing."
                            )
                    }

                    if (
                        node.generatorXdSubtransient <=
                        0.0
                    ) {

                        warnings +=
                            Issue(
                                severity =
                                    Severity.WARNING,
                                code =
                                    "GENERATOR_XD_MISSING",
                                elementId =
                                    node.id,
                                message =
                                    "${node.name}: generator Xd'' is missing."
                            )
                    }
                }

                SldNodeType.BREAKER -> {

                    if (
                        node.ratedKva <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity =
                                    Severity.WARNING,
                                code =
                                    "BREAKER_RATING_MISSING",
                                elementId =
                                    node.id,
                                message =
                                    "${node.name}: breaker rating has not been defined."
                            )
                    }
                }

                SldNodeType.PANEL -> {

                    if (
                        node.ratedKva <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity =
                                    Severity.WARNING,
                                code =
                                    "PANEL_RATING_MISSING",
                                elementId =
                                    node.id,
                                message =
                                    "${node.name}: panel rating has not been defined."
                            )
                    }
                }

                SldNodeType.LOAD -> {

                    if (
                        node.loadKw <= 0.0
                    ) {

                        warnings +=
                            Issue(
                                severity =
                                    Severity.WARNING,
                                code =
                                    "ZERO_LOAD",
                                elementId =
                                    node.id,
                                message =
                                    "${node.name}: load is zero."
                            )
                    }
                }

                SldNodeType.BUS -> Unit
            }
        }

        /*
         * Duplicate parallel topology.
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
                        severity =
                            Severity.INFORMATION,
                        code =
                            "PARALLEL_FEEDERS",
                        elementId =
                            connections.first().id,
                        message =
                            "Multiple feeders exist between the same two SLD elements."
                    )
            }

        /*
         * Cycle detection.
         */
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

        val visiting =
            mutableSetOf<String>()

        val visited =
            mutableSetOf<String>()

        fun visit(
            id: String
        ) {

            if (
                id in visiting
            ) {

                errors +=
                    Issue(
                        severity =
                            Severity.ERROR,
                        code =
                            "NETWORK_CYCLE",
                        elementId =
                            id,
                        message =
                            "Circular electrical topology detected."
                    )

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

        network.nodes.forEach {
            visit(it.id)
        }

        /*
         * Reachability from source.
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

            sources.forEach {
                walk(it.id)
            }

            network.nodes
                .filter {
                    it.id !in reachable
                }
                .forEach { node ->

                    warnings +=
                        Issue(
                            severity =
                                Severity.WARNING,
                            code =
                                "UNREACHABLE_ELEMENT",
                            elementId =
                                node.id,
                            message =
                                "${node.name}: element is not connected to an electrical source."
                        )
                }
        }

        information +=
            Issue(
                severity =
                    Severity.INFORMATION,
                code =
                    "NETWORK_SUMMARY",
                elementId =
                    null,
                message =
                    "SLD contains ${network.nodes.size} equipment elements and ${network.connections.size} feeders."
            )

        return Result(
            valid =
                errors.isEmpty(),
            errors =
                errors,
            warnings =
                warnings,
            information =
                information
        )
    }
}
