
package com.electrical.calculationspro.data

/**
 * Professional Single Line Diagram design validator.
 *
 * Responsibilities:
 * - Validate equipment and connection identifiers.
 * - Validate electrical ratings and engineering input values.
 * - Validate cable and internal busbar relationships.
 * - Detect duplicate connections, invalid directions and cycles.
 * - Check source reachability without crashing the editor.
 * - Preserve support for multiple sources while reporting
 *   source coordination and interlocking requirements.
 *
 * This validator does not modify the network and does not perform
 * electrical calculations.
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

    private fun isFinite(value: Double): Boolean =
        value.isFinite()

    private fun isPositiveFinite(value: Double): Boolean =
        value.isFinite() && value > 0.0

    private fun isNonNegativeFinite(value: Double): Boolean =
        value.isFinite() && value >= 0.0

    fun validate(
        network: SldNetwork
    ): Result {
        val errors = mutableListOf<Issue>()
        val warnings = mutableListOf<Issue>()
        val information = mutableListOf<Issue>()

        fun error(
            code: String,
            elementId: String? = null,
            message: String
        ) {
            errors += Issue(
                severity = Severity.ERROR,
                code = code,
                elementId = elementId,
                message = message
            )
        }

        fun warning(
            code: String,
            elementId: String? = null,
            message: String
        ) {
            warnings += Issue(
                severity = Severity.WARNING,
                code = code,
                elementId = elementId,
                message = message
            )
        }

        fun info(
            code: String,
            elementId: String? = null,
            message: String
        ) {
            information += Issue(
                severity = Severity.INFORMATION,
                code = code,
                elementId = elementId,
                message = message
            )
        }

        if (network.nodes.isEmpty()) {
            error(
                code = "SLD_EMPTY",
                message = "SLD network contains no equipment."
            )

            return Result(
                valid = false,
                errors = errors,
                warnings = warnings,
                information = information
            )
        }

        /*
         * Equipment identifiers.
         */
        network.nodes
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { id ->
                error(
                    code = "DUPLICATE_NODE_ID",
                    elementId = id,
                    message = "Duplicate SLD node ID: $id."
                )
            }

        val nodeMap = network.nodes.associateBy { it.id }

        /*
         * Connection identifiers.
         */
        network.connections
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { id ->
                error(
                    code = "DUPLICATE_CONNECTION_ID",
                    elementId = id,
                    message = "Duplicate SLD connection ID: $id."
                )
            }

        val sources = network.nodes.filter {
            it.type == SldNodeType.SOURCE
        }

        if (sources.isEmpty()) {
            error(
                code = "NO_SOURCE",
                message = "The electrical network has no explicit SOURCE."
            )
        }

        if (sources.size > 1) {
            warning(
                code = "MULTIPLE_SOURCES",
                message =
                    "Multiple sources are present. Define the operating modes, source interlocking, transfer arrangement and protection coordination before approving the design."
            )
        }

        /*
         * Validate every equipment item.
         *
         * Finite-value checks are intentional: NaN and infinity
         * must not pass engineering input validation.
         */
        network.nodes.forEach { node ->
            val name = node.name.ifBlank { node.id }

            if (node.id.isBlank()) {
                error(
                    code = "EMPTY_NODE_ID",
                    elementId = node.id,
                    message = "$name: equipment ID is empty."
                )
            }

            if (node.name.isBlank()) {
                warning(
                    code = "EMPTY_NODE_NAME",
                    elementId = node.id,
                    message = "Equipment has no display name."
                )
            }

            if (!isPositiveFinite(node.voltage)) {
                error(
                    code = "INVALID_VOLTAGE",
                    elementId = node.id,
                    message = "$name: voltage must be finite and greater than zero."
                )
            }

            if (
                !isFinite(node.powerFactor) ||
                node.powerFactor <= 0.0 ||
                node.powerFactor > 1.0
            ) {
                error(
                    code = "INVALID_POWER_FACTOR",
                    elementId = node.id,
                    message = "$name: power factor must be greater than zero and no greater than one."
                )
            }

            if (
                !isFinite(node.demandFactor) ||
                node.demandFactor < 0.0 ||
                node.demandFactor > 1.0
            ) {
                error(
                    code = "INVALID_DEMAND_FACTOR",
                    elementId = node.id,
                    message = "$name: demand factor must be between zero and one."
                )
            }

            if (!isNonNegativeFinite(node.loadKw)) {
                error(
                    code = "INVALID_LOAD",
                    elementId = node.id,
                    message = "$name: load must be finite and non-negative."
                )
            }

            if (!isNonNegativeFinite(node.ratedKva)) {
                error(
                    code = "INVALID_RATED_KVA",
                    elementId = node.id,
                    message = "$name: rated kVA must be finite and non-negative."
                )
            }

            if (!isNonNegativeFinite(node.transformerPercentZ)) {
                error(
                    code = "INVALID_TRANSFORMER_IMPEDANCE",
                    elementId = node.id,
                    message = "$name: transformer impedance must be finite and non-negative."
                )
            }

            if (!isNonNegativeFinite(node.generatorXdSubtransient)) {
                error(
                    code = "INVALID_GENERATOR_REACTANCE",
                    elementId = node.id,
                    message = "$name: generator sub-transient reactance must be finite and non-negative."
                )
            }

            if (!isNonNegativeFinite(node.sourceShortCircuitMva)) {
                error(
                    code = "INVALID_SOURCE_SHORT_CIRCUIT",
                    elementId = node.id,
                    message = "$name: source short-circuit MVA must be finite and non-negative."
                )
            }

            when (node.type) {
                SldNodeType.TRANSFORMER -> {
                    if (!isPositiveFinite(node.ratedKva)) {
                        warning(
                            code = "TRANSFORMER_RATING_NOT_DEFINED",
                            elementId = node.id,
                            message = "$name: transformer rating is not defined."
                        )
                    }

                    if (!isPositiveFinite(node.transformerPercentZ)) {
                        warning(
                            code = "TRANSFORMER_IMPEDANCE_NOT_DEFINED",
                            elementId = node.id,
                            message = "$name: transformer impedance is not defined."
                        )
                    }
                }

                SldNodeType.GENERATOR -> {
                    if (!isPositiveFinite(node.ratedKva)) {
                        warning(
                            code = "GENERATOR_RATING_NOT_DEFINED",
                            elementId = node.id,
                            message = "$name: generator rating is not defined."
                        )
                    }

                    if (!isPositiveFinite(node.generatorXdSubtransient)) {
                        warning(
                            code = "GENERATOR_XD_NOT_DEFINED",
                            elementId = node.id,
                            message = "$name: generator sub-transient reactance is not defined."
                        )
                    }
                }

                SldNodeType.SOURCE -> {
                    if (node.sourceShortCircuitMva == 0.0) {
                        warning(
                            code = "SOURCE_FAULT_LEVEL_NOT_DEFINED",
                            elementId = node.id,
                            message = "$name: source fault level is zero or has not been entered."
                        )
                    }
                }

                SldNodeType.LOAD -> {
                    if (node.loadKw == 0.0) {
                        warning(
                            code = "LOAD_POWER_NOT_DEFINED",
                            elementId = node.id,
                            message = "$name: load power is zero or has not been entered."
                        )
                    }
                }

                else -> Unit
            }
        }

        /*
         * Connection accounting.
         *
         * Only connections whose endpoints exist are included
         * in the graph used for reachability and cycle detection.
         */
        val incoming = mutableMapOf<String, Int>()
        val outgoing = mutableMapOf<String, Int>()

        network.nodes.forEach { node ->
            incoming[node.id] = 0
            outgoing[node.id] = 0
        }

        val directedEdges = mutableSetOf<Pair<String, String>>()
        val children = mutableMapOf<String, MutableSet<String>>()
        val parents = mutableMapOf<String, MutableSet<String>>()

        network.nodes.forEach { node ->
            children[node.id] = linkedSetOf()
            parents[node.id] = linkedSetOf()
        }

        network.connections.forEach { connection ->
            val from = nodeMap[connection.fromNodeId]
            val to = nodeMap[connection.toNodeId]

            if (from == null) {
                error(
                    code = "INVALID_FROM_NODE",
                    elementId = connection.id,
                    message = "Connection source node does not exist."
                )
            }

            if (to == null) {
                error(
                    code = "INVALID_TO_NODE",
                    elementId = connection.id,
                    message = "Connection destination node does not exist."
                )
            }

            if (
                connection.fromNodeId.isBlank() ||
                connection.toNodeId.isBlank()
            ) {
                error(
                    code = "EMPTY_CONNECTION_ENDPOINT",
                    elementId = connection.id,
                    message = "Connection contains an empty endpoint ID."
                )
            }

            if (connection.fromNodeId == connection.toNodeId) {
                error(
                    code = "SELF_CONNECTION",
                    elementId = connection.id,
                    message = "An SLD element cannot connect to itself."
                )
            }

            val edge = connection.fromNodeId to connection.toNodeId

            if (!directedEdges.add(edge)) {
                error(
                    code = "DUPLICATE_DIRECTED_CONNECTION",
                    elementId = connection.id,
                    message =
                        "Duplicate directed connection from ${connection.fromNodeId} to ${connection.toNodeId}."
                )
            }

            if (from != null && to != null) {
                outgoing[from.id] = (outgoing[from.id] ?: 0) + 1
                incoming[to.id] = (incoming[to.id] ?: 0) + 1

                children.getValue(from.id).add(to.id)
                parents.getValue(to.id).add(from.id)
            }

            /*
             * Connection electrical data must be finite.
             */
            val numericConnectionValues = listOf(
                "length" to connection.lengthMeters,
                "cable size" to connection.cableSizeMm2,
                "current capacity" to connection.currentCapacityA,
                "resistance" to connection.resistanceOhmPerKm,
                "reactance" to connection.reactanceOhmPerKm,
                "voltage drop" to connection.voltageDropPercent,
                "busbar current rating" to connection.busbarRatedCurrentA,
                "busbar short-circuit rating" to connection.busbarShortCircuitKA
            )

            numericConnectionValues.forEach { (field, value) ->
                if (!isFinite(value)) {
                    error(
                        code = "NON_FINITE_CONNECTION_VALUE",
                        elementId = connection.id,
                        message = "Connection $field must be a finite number."
                    )
                }
            }

            /*
             * BUSBAR is reserved for internal switchgear
             * relationships. It is not a cable feeder.
             */
            if (
                connection.connectionType ==
                SldConnectionType.BUSBAR
            ) {
                if (
                    from != null &&
                    to != null &&
                    !isInternalBusbarPair(from.type, to.type)
                ) {
                    error(
                        code = "INVALID_BUSBAR_TOPOLOGY",
                        elementId = connection.id,
                        message =
                            "BUSBAR is only valid for PANEL-BUS, BUS-BREAKER, or PANEL-BREAKER relationships."
                    )
                }

                if (connection.lengthMeters != 0.0) {
                    error(
                        code = "BUSBAR_HAS_CABLE_LENGTH",
                        elementId = connection.id,
                        message = "BUSBAR must not contain cable length."
                    )
                }

                if (connection.cableSizeMm2 != 0.0) {
                    error(
                        code = "BUSBAR_HAS_CABLE_SIZE",
                        elementId = connection.id,
                        message = "BUSBAR must not contain cable cross-sectional area."
                    )
                }

                if (connection.currentCapacityA != 0.0) {
                    error(
                        code = "BUSBAR_HAS_CABLE_CAPACITY",
                        elementId = connection.id,
                        message = "BUSBAR must not contain cable current capacity."
                    )
                }

                if (connection.parallelRuns != 1) {
                    error(
                        code = "BUSBAR_HAS_PARALLEL_RUNS",
                        elementId = connection.id,
                        message = "BUSBAR must not contain cable parallel-run data."
                    )
                }

                if (!isNonNegativeFinite(connection.busbarRatedCurrentA)) {
                    error(
                        code = "INVALID_BUSBAR_CURRENT_RATING",
                        elementId = connection.id,
                        message = "BUSBAR current rating must be finite and non-negative."
                    )
                } else if (connection.busbarRatedCurrentA <= 0.0) {
                    warning(
                        code = "BUSBAR_RATING_NOT_VERIFIED",
                        elementId = connection.id,
                        message = "BUSBAR current rating is not defined."
                    )
                }

                if (!isNonNegativeFinite(connection.busbarShortCircuitKA)) {
                    error(
                        code = "INVALID_BUSBAR_SHORT_CIRCUIT_RATING",
                        elementId = connection.id,
                        message = "BUSBAR short-circuit rating must be finite and non-negative."
                    )
                } else if (connection.busbarShortCircuitKA <= 0.0) {
                    warning(
                        code = "BUSBAR_SHORT_CIRCUIT_NOT_VERIFIED",
                        elementId = connection.id,
                        message = "BUSBAR short-circuit withstand rating is not defined."
                    )
                }
            }

            /*
             * CABLE is the external feeder relationship.
             */
            if (
                connection.connectionType ==
                SldConnectionType.CABLE
            ) {
                if (connection.parallelRuns < 1) {
                    error(
                        code = "INVALID_PARALLEL_RUNS",
                        elementId = connection.id,
                        message = "Parallel cable runs must be at least one."
                    )
                }

                if (!isNonNegativeFinite(connection.lengthMeters)) {
                    error(
                        code = "INVALID_LENGTH",
                        elementId = connection.id,
                        message = "Cable length must be finite and non-negative."
                    )
                } else if (connection.lengthMeters == 0.0) {
                    warning(
                        code = "CABLE_LENGTH_NOT_VERIFIED",
                        elementId = connection.id,
                        message =
                            "Cable length is zero; voltage-drop and impedance verification may be incomplete."
                    )
                }

                if (!isNonNegativeFinite(connection.cableSizeMm2)) {
                    error(
                        code = "INVALID_CABLE_SIZE",
                        elementId = connection.id,
                        message = "Cable cross-sectional area must be finite and non-negative."
                    )
                } else if (connection.cableSizeMm2 <= 0.0) {
                    warning(
                        code = "CABLE_SIZE_NOT_VERIFIED",
                        elementId = connection.id,
                        message = "Cable cross-sectional area is not verified."
                    )
                }

                if (!isNonNegativeFinite(connection.currentCapacityA)) {
                    error(
                        code = "INVALID_CABLE_CAPACITY",
                        elementId = connection.id,
                        message = "Cable current capacity must be finite and non-negative."
                    )
                } else if (connection.currentCapacityA <= 0.0) {
                    warning(
                        code = "CABLE_CAPACITY_NOT_VERIFIED",
                        elementId = connection.id,
                        message = "Cable current capacity is not verified."
                    )
                }

                if (!isNonNegativeFinite(connection.resistanceOhmPerKm)) {
                    error(
                        code = "INVALID_CABLE_RESISTANCE",
                        elementId = connection.id,
                        message = "Cable resistance must be finite and non-negative."
                    )
                }

                if (!isNonNegativeFinite(connection.reactanceOhmPerKm)) {
                    error(
                        code = "INVALID_CABLE_REACTANCE",
                        elementId = connection.id,
                        message = "Cable reactance must be finite and non-negative."
                    )
                }

                if (!isNonNegativeFinite(connection.voltageDropPercent)) {
                    error(
                        code = "INVALID_VOLTAGE_DROP",
                        elementId = connection.id,
                        message = "Voltage drop must be finite and non-negative."
                    )
                }
            }
        }

        /*
         * Directed graph cycle detection.
         *
         * An iterative depth-first traversal avoids stack overflow
         * on large drawings.
         */
        val visitState = mutableMapOf<String, Int>()
        // 0 = unvisited, 1 = visiting, 2 = visited

        network.nodes.forEach { node ->
            visitState[node.id] = 0
        }

        data class Frame(
            val nodeId: String,
            val iterator: Iterator<String>
        )

        network.nodes.forEach { root ->
            if (visitState[root.id] != 0) {
                return@forEach
            }

            visitState[root.id] = 1

            val stack = ArrayDeque<Frame>()
            stack.addLast(
                Frame(
                    nodeId = root.id,
                    iterator = children[root.id].orEmpty().iterator()
                )
            )

            while (stack.isNotEmpty()) {
                val frame = stack.last()

                if (!frame.iterator.hasNext()) {
                    visitState[frame.nodeId] = 2
                    stack.removeLast()
                    continue
                }

                val childId = frame.iterator.next()
                val childState = visitState[childId] ?: 0

                when (childState) {
                    1 -> {
                        error(
                            code = "DIRECTED_CYCLE",
                            elementId = childId,
                            message =
                                "Circular electrical connection detected near ${nodeMap[childId]?.name ?: childId}."
                        )
                    }

                    0 -> {
                        visitState[childId] = 1
                        stack.addLast(
                            Frame(
                                nodeId = childId,
                                iterator = children[childId].orEmpty().iterator()
                            )
                        )
                    }

                    else -> Unit
                }
            }
        }

        /*
         * Source reachability.
         *
         * Multiple sources are permitted for validation purposes.
         * Reachability from any declared source is checked; source
         * selection and parallel operation still require engineering
         * coordination.
         */
        val reachable = mutableSetOf<String>()
        val queue = ArrayDeque<String>()

        sources.forEach { source ->
            queue.addLast(source.id)
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (!reachable.add(current)) {
                continue
            }

            children[current].orEmpty().forEach { childId ->
                if (childId !in reachable) {
                    queue.addLast(childId)
                }
            }
        }

        network.nodes.forEach { node ->
            if (
                node.type != SldNodeType.SOURCE &&
                node.id !in reachable
            ) {
                warning(
                    code = "NODE_NOT_SOURCE_REACHABLE",
                    elementId = node.id,
                    message =
                        "${node.name.ifBlank { node.id }} is not reachable from any declared electrical source. Check connection direction and upstream feeders."
                )
            }
        }

        /*
         * Topology-specific equipment checks.
         */
        network.nodes.forEach { node ->
            val inCount = incoming[node.id] ?: 0
            val outCount = outgoing[node.id] ?: 0
            val name = node.name.ifBlank { node.id }

            when (node.type) {
                SldNodeType.SOURCE -> {
                    if (inCount > 0) {
                        warning(
                            code = "SOURCE_HAS_INCOMING",
                            elementId = node.id,
                            message = "$name has an incoming connection."
                        )
                    }

                    if (outCount == 0) {
                        warning(
                            code = "SOURCE_HAS_NO_OUTPUT",
                            elementId = node.id,
                            message = "$name has no downstream connection."
                        )
                    }
                }

                SldNodeType.LOAD -> {
                    if (inCount == 0) {
                        warning(
                            code = "LOAD_NOT_CONNECTED",
                            elementId = node.id,
                            message = "$name is not connected to an upstream element."
                        )
                    }

                    if (outCount > 0) {
                        warning(
                            code = "LOAD_HAS_DOWNSTREAM",
                            elementId = node.id,
                            message =
                                "$name is configured as a load but also feeds downstream equipment. Confirm the intended topology."
                        )
                    }
                }

                SldNodeType.BREAKER -> {
                    if (inCount == 0 && outCount == 0) {
                        info(
                            code = "BREAKER_ORPHAN",
                            elementId = node.id,
                            message = "$name is currently unconnected."
                        )
                    } else if (inCount == 0) {
                        warning(
                            code = "BREAKER_NO_UPSTREAM",
                            elementId = node.id,
                            message = "$name has no upstream feeder."
                        )
                    } else if (outCount == 0) {
                        warning(
                            code = "BREAKER_NO_DOWNSTREAM",
                            elementId = node.id,
                            message = "$name has no downstream connection."
                        )
                    }
                }

                SldNodeType.PANEL -> {
                    if (inCount == 0 && outCount == 0) {
                        info(
                            code = "PANEL_ORPHAN",
                            elementId = node.id,
                            message = "$name is currently unconnected."
                        )
                    } else if (inCount == 0) {
                        warning(
                            code = "PANEL_NO_UPSTREAM",
                            elementId = node.id,
                            message = "$name has no upstream feeder."
                        )
                    }
                }

                SldNodeType.BUS -> {
                    if (inCount == 0 && outCount == 0) {
                        info(
                            code = "BUS_ORPHAN",
                            elementId = node.id,
                            message = "$name is currently unconnected."
                        )
                    }
                }

                SldNodeType.TRANSFORMER,
                SldNodeType.GENERATOR -> {
                    if (inCount == 0 && outCount == 0) {
                        info(
                            code = "SOURCE_EQUIPMENT_ORPHAN",
                            elementId = node.id,
                            message = "$name is currently unconnected."
                        )
                    }
                }
            }
        }

        /*
         * The editor may continue displaying an incomplete drawing.
         * Only actual ERROR-severity issues make this validation fail.
         * Warnings identify items requiring engineering review.
         */
        return Result(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            information = information
        )
    }
}
