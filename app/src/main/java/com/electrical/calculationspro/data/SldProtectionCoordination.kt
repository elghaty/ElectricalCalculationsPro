package com.electrical.calculationspro.data

data class SldProtectionDevice(
    val nodeId: String,
    val nodeName: String,
    val deviceType: String,
    val downstreamCurrentA: Double,
    val recommendedRatingA: Double,
    val shortCircuitRatingKa: Double,
    val longTimePickupA: Double,
    val instantaneousPickupA: Double,
    val selectivityMarginA: Double,
    val status: ProtectionStatus,
    val notes: List<String> = emptyList()
)

enum class ProtectionStatus {
    PASS,
    WARNING,
    FAIL
}

data class SldProtectionCoordinationResult(
    val devices: Map<String, SldProtectionDevice>,
    val coordinatedPairs: Int,
    val warningPairs: Int,
    val failedPairs: Int,
    val notes: List<String>
)

object SldProtectionCoordinationEngine {

    private const val SQRT_3 = 1.7320508075688772

    private const val BREAKER_MARGIN = 1.15
    private const val LONG_TIME_FACTOR = 0.90
    private const val INSTANTANEOUS_FACTOR = 8.0

    private const val COORDINATED_RATIO = 1.60
    private const val WARNING_RATIO = 1.25

    private val fallbackBreakerRatings = listOf(
        16.0,
        20.0,
        25.0,
        32.0,
        40.0,
        50.0,
        63.0,
        80.0,
        100.0,
        125.0,
        160.0,
        200.0,
        250.0,
        320.0,
        400.0,
        500.0,
        630.0,
        800.0,
        1000.0,
        1250.0,
        1600.0,
        2000.0,
        2500.0,
        3200.0,
        4000.0,
        5000.0,
        6300.0
    )

    fun calculate(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        cableSizingStudy: SldCableSizingStudy? = null,
        upstreamEngineering: SldUpstreamEngineering.Result? = null,
        engineeringContext: SldEngineeringContext? = null
    ): SldProtectionCoordinationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        engineeringContext?.let {
            it.copy(
                requireImplementedStandard = false
            ).validate()
        }

        val topology =
            SldTopologyEngine.build(network)

        val upstream =
            upstreamEngineering
                ?: SldUpstreamEngineering.calculate(network)

        val breakerRatings =
            engineeringContext
                ?.standardEngine
                ?.standardBreakerRatings()
                ?.filter { it > 0.0 }
                ?.distinct()
                ?.sorted()
                ?.takeIf { it.isNotEmpty() }
                ?: fallbackBreakerRatings

        val devices =
            linkedMapOf<String, SldProtectionDevice>()

        network.nodes.forEach { node ->

            val downstreamCurrent =
                determineDownstreamCurrent(
                    node = node,
                    upstream = upstream,
                    cableSizingStudy = cableSizingStudy
                )

            if (downstreamCurrent <= 0.0) {
                return@forEach
            }

            val recommendedRating =
                selectBreakerRating(
                    currentA = downstreamCurrent * BREAKER_MARGIN,
                    breakerRatings = breakerRatings
                )

            val shortCircuitResult =
                shortCircuitStudy
                    ?.results
                    ?.get(node.id)

            val shortCircuitRating =
                shortCircuitResult
                    ?.breakerRequiredKa
                    ?: 0.0

            val initialSymmetricalCurrentKa =
                shortCircuitResult
                    ?.initialSymmetricalCurrentKa
                    ?: 0.0

            val longTimePickup =
                recommendedRating *
                    LONG_TIME_FACTOR

            val instantaneousPickup =
                recommendedRating *
                    INSTANTANEOUS_FACTOR

            val notes =
                mutableListOf<String>()

            var status =
                ProtectionStatus.PASS

            if (recommendedRating <= downstreamCurrent) {

                status =
                    ProtectionStatus.WARNING

                notes.add(
                    "Selected breaker rating is not above the calculated design current."
                )
            }

            if (
                shortCircuitRating > 0.0 &&
                initialSymmetricalCurrentKa > 0.0
            ) {

                notes.add(
                    "Required short-circuit breaking capacity: " +
                        formatKa(shortCircuitRating) +
                        " kA."
                )

                notes.add(
                    "Calculated initial symmetrical fault current: " +
                        formatKa(initialSymmetricalCurrentKa) +
                        " kA."
                )
            } else {

                notes.add(
                    "Short-circuit study is not available for this device."
                )
            }

            if (
                cableSizingStudy != null &&
                node.type != SldNodeType.SOURCE
            ) {

                val incomingConnections =
                    topology.connections.filter {
                        it.toNodeId == node.id
                    }

                incomingConnections.forEach { connection ->

                    val cable =
                        cableSizingStudy.results[
                            connection.id
                        ]

                    if (
                        cable != null &&
                        cable.recommendedCurrentCapacityA > 0.0 &&
                        recommendedRating >
                        cable.recommendedCurrentCapacityA
                    ) {

                        status =
                            ProtectionStatus.FAIL

                        notes.add(
                            "Breaker rating exceeds the selected cable current capacity."
                        )
                    }
                }
            }

            if (engineeringContext != null) {

                notes.add(
                    "Standard: ${engineeringContext.codeName}."
                )

                notes.add(
                    "Code revision: ${engineeringContext.codeRevision}."
                )

                notes.add(
                    "Breaker ratings supplied by the selected StandardEngine."
                )

                notes.add(
                    "Standard implementation status: " +
                        engineeringContext.standardImplementationStatus
                )
            } else {

                notes.add(
                    "Backward-compatible protection mode: no explicit engineering context supplied."
                )
            }

            devices[node.id] =
                SldProtectionDevice(
                    nodeId =
                        node.id,
                    nodeName =
                        node.name,
                    deviceType =
                        protectionDeviceType(node.type),
                    downstreamCurrentA =
                        downstreamCurrent,
                    recommendedRatingA =
                        recommendedRating,
                    shortCircuitRatingKa =
                        shortCircuitRating,
                    longTimePickupA =
                        longTimePickup,
                    instantaneousPickupA =
                        instantaneousPickup,
                    selectivityMarginA =
                        0.0,
                    status =
                        status,
                    notes =
                        notes
                )
        }

        var coordinatedPairs = 0
        var warningPairs = 0
        var failedPairs = 0

        topology.connections.forEach { connection ->

            val upstreamDevice =
                devices[connection.fromNodeId]

            val downstreamDevice =
                devices[connection.toNodeId]

            if (
                upstreamDevice == null ||
                downstreamDevice == null
            ) {
                return@forEach
            }

            val upstreamRating =
                upstreamDevice.recommendedRatingA

            val downstreamRating =
                downstreamDevice.recommendedRatingA

            if (
                upstreamRating <= 0.0 ||
                downstreamRating <= 0.0
            ) {
                return@forEach
            }

            val ratio =
                upstreamRating /
                    downstreamRating

            val selectivityMargin =
                upstreamRating -
                    downstreamRating

            when {

                ratio >= COORDINATED_RATIO -> {
                    coordinatedPairs++
                }

                ratio >= WARNING_RATIO -> {
                    warningPairs++
                }

                else -> {
                    failedPairs++
                }
            }

            val existingUpstream =
                devices[connection.fromNodeId]

            if (existingUpstream != null) {

                val currentNotes =
                    existingUpstream.notes.toMutableList()

                currentNotes.add(
                    "Downstream breaker ${formatA(downstreamRating)} A; " +
                        "selectivity margin ${formatA(selectivityMargin)} A."
                )

                devices[connection.fromNodeId] =
                    existingUpstream.copy(
                        selectivityMarginA =
                            maxOf(
                                existingUpstream.selectivityMarginA,
                                selectivityMargin
                            ),
                        notes =
                            currentNotes.distinct()
                    )
            }
        }

        val notes =
            mutableListOf<String>()

        notes.add(
            "Protection coordination is a preliminary engineering study."
        )

        notes.add(
            "Electrical feeder direction is determined by SldTopologyEngine."
        )

        notes.add(
            "Upstream engineering is reused when supplied by the facade."
        )

        if (engineeringContext != null) {

            notes.add(
                "Active standard: " +
                    "${engineeringContext.codeName} " +
                    "(${engineeringContext.codeRevision})."
            )

            notes.add(
                "Breaker ratings were obtained from the selected StandardEngine."
            )

            notes.add(
                "Standard implementation status: " +
                    engineeringContext.standardImplementationStatus
            )
        } else {

            notes.add(
                "Fallback breaker rating sequence is used because no engineering context was supplied."
            )
        }

        notes.add(
            "Long-time pickup is preliminarily set to 90% of breaker rating."
        )

        notes.add(
            "Instantaneous pickup is preliminarily set to 8 × breaker rating."
        )

        notes.add(
            "Final selectivity requires manufacturer time-current curves, trip-unit settings and actual device characteristics."
        )

        if (warningPairs > 0) {

            notes.add(
                "$warningPairs feeder pair(s) require detailed time-current verification."
            )
        }

        if (failedPairs > 0) {

            notes.add(
                "$failedPairs feeder pair(s) do not provide the required preliminary selectivity margin."
            )
        }

        return SldProtectionCoordinationResult(
            devices =
                devices,
            coordinatedPairs =
                coordinatedPairs,
            warningPairs =
                warningPairs,
            failedPairs =
                failedPairs,
            notes =
                notes
        )
    }

    private fun determineDownstreamCurrent(
        node: SldNode,
        upstream: SldUpstreamEngineering.Result,
        cableSizingStudy: SldCableSizingStudy?
    ): Double {

        val upstreamCurrent =
            upstream.nodes
                .firstOrNull {
                    it.nodeId == node.id
                }
                ?.currentA
                ?: 0.0

        if (upstreamCurrent > 0.0) {
            return upstreamCurrent
        }

        cableSizingStudy
            ?.results
            ?.values
            ?.firstOrNull {
                it.toNodeId == node.id
            }
            ?.designCurrentA
            ?.let { current ->

                if (current > 0.0) {
                    return current
                }
            }

        if (
            node.loadKw > 0.0 &&
            node.powerFactor > 0.0 &&
            node.voltage > 0.0
        ) {

            return (
                node.loadKw * 1000.0
                ) / (
                SQRT_3 *
                    node.voltage *
                    node.powerFactor
                )
        }

        if (
            node.ratedKva > 0.0 &&
            node.voltage > 0.0
        ) {

            return (
                node.ratedKva * 1000.0
                ) / (
                SQRT_3 *
                    node.voltage
                )
        }

        return 0.0
    }

    private fun selectBreakerRating(
        currentA: Double,
        breakerRatings: List<Double>
    ): Double {

        if (currentA <= 0.0) {
            return 0.0
        }

        return breakerRatings.firstOrNull {
            it >= currentA
        } ?: breakerRatings.last()
    }

    private fun protectionDeviceType(
        type: SldNodeType
    ): String =
        when (type) {

            SldNodeType.BREAKER ->
                "ACB/MCCB"

            SldNodeType.PANEL ->
                "Panel Incomer"

            SldNodeType.TRANSFORMER ->
                "Transformer Protection"

            SldNodeType.GENERATOR ->
                "Generator Protection"

            SldNodeType.LOAD ->
                "Load Feeder"

            SldNodeType.BUS ->
                "Bus Protection"

            SldNodeType.SOURCE ->
                "Main Incomer"
        }

    private fun formatA(
        value: Double
    ): String =
        if (value >= 100.0) {
            "%.0f".format(value)
        } else {
            "%.1f".format(value)
        }

    private fun formatKa(
        value: Double
    ): String =
        "%.2f".format(value)
}
