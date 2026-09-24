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

    private val breakerRatings = listOf(
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
        cableSizingStudy: SldCableSizingStudy? = null
    ): SldProtectionCoordinationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val upstream: SldUpstreamEngineering.Result? =
            try {
                SldUpstreamEngineering.calculate(network)
            } catch (_: Exception) {
                null
            }

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
                    currentA = downstreamCurrent * 1.15
                )

            val shortCircuitRating =
                shortCircuitStudy
                    ?.results
                    ?.get(node.id)
                    ?.breakerRequiredKa
                    ?: 0.0

            val initialSymmetricalCurrentKa =
                shortCircuitStudy
                    ?.results
                    ?.get(node.id)
                    ?.initialSymmetricalCurrentKa
                    ?: 0.0

            val longTimePickup =
                recommendedRating * 0.90

            val instantaneousPickup =
                recommendedRating * 8.0

            val notes =
                mutableListOf<String>()

            var status =
                ProtectionStatus.PASS

            if (recommendedRating <= downstreamCurrent) {
                status = ProtectionStatus.WARNING

                notes.add(
                    "Breaker rating is close to the calculated feeder current."
                )
            }

            if (
                shortCircuitRating > 0.0 &&
                shortCircuitRating < initialSymmetricalCurrentKa
            ) {
                status = ProtectionStatus.FAIL

                notes.add(
                    "Required short-circuit breaking capacity is not satisfied."
                )
            }

            if (shortCircuitRating <= 0.0) {
                notes.add(
                    "Short-circuit study is not available for this device."
                )
            }

            if (
                cableSizingStudy != null &&
                node.type != SldNodeType.SOURCE
            ) {

                val downstreamConnections =
                    network.connections.filter {
                        it.toNodeId == node.id
                    }

                downstreamConnections.forEach { connection ->

                    val cable =
                        cableSizingStudy.results[connection.id]

                    if (
                        cable != null &&
                        cable.recommendedCurrentCapacityA > 0.0 &&
                        recommendedRating >
                        cable.recommendedCurrentCapacityA
                    ) {

                        status =
                            ProtectionStatus.FAIL

                        notes.add(
                            "Breaker rating exceeds the recommended cable current capacity."
                        )
                    }
                }
            }

            devices[node.id] =
                SldProtectionDevice(
                    nodeId = node.id,
                    nodeName = node.name,
                    deviceType = protectionDeviceType(node.type),
                    downstreamCurrentA = downstreamCurrent,
                    recommendedRatingA = recommendedRating,
                    shortCircuitRatingKa = shortCircuitRating,
                    longTimePickupA = longTimePickup,
                    instantaneousPickupA = instantaneousPickup,
                    selectivityMarginA = 0.0,
                    status = status,
                    notes = notes
                )
        }

        var coordinatedPairs = 0
        var warningPairs = 0
        var failedPairs = 0

        network.connections.forEach { connection ->

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
                upstreamRating / downstreamRating

            when {
                ratio >= 1.60 -> {
                    coordinatedPairs++
                }

                ratio >= 1.25 -> {
                    warningPairs++
                }

                else -> {
                    failedPairs++
                }
            }
        }

        val notes =
            mutableListOf<String>()

        notes.add(
            "Protection coordination is a preliminary engineering study."
        )

        notes.add(
            "Final selectivity requires manufacturer time-current curves and actual trip-unit settings."
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
            devices = devices,
            coordinatedPairs = coordinatedPairs,
            warningPairs = warningPairs,
            failedPairs = failedPairs,
            notes = notes
        )
    }

    private fun determineDownstreamCurrent(
        node: SldNode,
        upstream: SldUpstreamEngineering.Result?,
        cableSizingStudy: SldCableSizingStudy?
    ): Double {

        val upstreamCurrent =
            upstream
                ?.nodes
                ?.firstOrNull {
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
                1.7320508075688772 *
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
                1.7320508075688772 *
                    node.voltage
                )
        }

        return 0.0
    }

    private fun selectBreakerRating(
        currentA: Double
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
}
