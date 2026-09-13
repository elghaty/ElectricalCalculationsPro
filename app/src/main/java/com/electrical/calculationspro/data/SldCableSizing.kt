package com.electrical.calculationspro.data

import kotlin.math.sqrt

data class SldCableOption(
    val sizeMm2: Double,
    val material: String,
    val cores: Int,
    val currentCapacityA: Double,
    val resistanceOhmPerKm: Double,
    val reactanceOhmPerKm: Double,
    val voltageDropPercent: Double,
    val shortCircuitWithstandKa: Double,
    val parallelRuns: Int,
    val totalCurrentCapacityA: Double,
    val acceptable: Boolean,
    val reasons: List<String> = emptyList()
)

data class SldCableSizingResult(
    val connectionId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val designCurrentA: Double,
    val shortCircuitCurrentKa: Double,
    val requiredCurrentCapacityA: Double,
    val maximumVoltageDropPercent: Double,
    val recommendedSizeMm2: Double,
    val recommendedParallelRuns: Int,
    val recommendedMaterial: String,
    val recommendedCores: Int,
    val recommendedCurrentCapacityA: Double,
    val recommendedVoltageDropPercent: Double,
    val recommendedShortCircuitWithstandKa: Double,
    val options: List<SldCableOption>,
    val notes: List<String>
)

data class SldCableSizingStudy(
    val results: Map<String, SldCableSizingResult>,
    val successfulFeeders: Int,
    val failedFeeders: Int,
    val notes: List<String>
)

object SldCableSizingEngine {

    private const val SQRT_3 = 1.7320508075688772
    private const val DEFAULT_VOLTAGE_DROP_LIMIT = 3.0
    private const val DEFAULT_SHORT_CIRCUIT_TIME_SECONDS = 1.0
    private const val DEFAULT_K_FACTOR_CU = 143.0
    private const val DEFAULT_K_FACTOR_AL = 94.0

    private data class CableCatalogEntry(
        val sizeMm2: Double,
        val copperCapacityA: Double,
        val aluminiumCapacityA: Double,
        val copperR: Double,
        val aluminiumR: Double,
        val x: Double
    )

    private val catalog = listOf(
        CableCatalogEntry(1.5, 18.0, 0.0, 12.1, 0.0, 0.080),
        CableCatalogEntry(2.5, 24.0, 0.0, 7.41, 0.0, 0.080),
        CableCatalogEntry(4.0, 32.0, 0.0, 4.61, 0.0, 0.079),
        CableCatalogEntry(6.0, 41.0, 0.0, 3.08, 0.0, 0.078),
        CableCatalogEntry(10.0, 57.0, 50.0, 1.83, 3.08, 0.076),
        CableCatalogEntry(16.0, 76.0, 65.0, 1.15, 1.91, 0.074),
        CableCatalogEntry(25.0, 101.0, 85.0, 0.727, 1.20, 0.073),
        CableCatalogEntry(35.0, 125.0, 105.0, 0.524, 0.868, 0.072),
        CableCatalogEntry(50.0, 150.0, 125.0, 0.387, 0.641, 0.071),
        CableCatalogEntry(70.0, 195.0, 160.0, 0.268, 0.443, 0.070),
        CableCatalogEntry(95.0, 235.0, 195.0, 0.193, 0.320, 0.069),
        CableCatalogEntry(120.0, 270.0, 225.0, 0.153, 0.253, 0.068),
        CableCatalogEntry(150.0, 305.0, 255.0, 0.124, 0.206, 0.067),
        CableCatalogEntry(185.0, 345.0, 285.0, 0.099, 0.164, 0.066),
        CableCatalogEntry(240.0, 400.0, 335.0, 0.0754, 0.125, 0.065),
        CableCatalogEntry(300.0, 455.0, 380.0, 0.0601, 0.100, 0.064),
        CableCatalogEntry(400.0, 530.0, 440.0, 0.0470, 0.0778, 0.063),
        CableCatalogEntry(500.0, 610.0, 505.0, 0.0366, 0.0605, 0.062),
        CableCatalogEntry(630.0, 690.0, 575.0, 0.0283, 0.0469, 0.061)
    )

    fun calculate(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        voltageDropLimitPercent: Double = DEFAULT_VOLTAGE_DROP_LIMIT,
        shortCircuitTimeSeconds: Double = DEFAULT_SHORT_CIRCUIT_TIME_SECONDS
    ): SldCableSizingStudy {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        require(voltageDropLimitPercent > 0.0) {
            "Voltage drop limit must be greater than zero."
        }

        require(shortCircuitTimeSeconds > 0.0) {
            "Short-circuit clearing time must be greater than zero."
        }

        val nodeMap = network.nodes.associateBy { it.id }

        val upstream =
            try {
                SldEngineeringEngine.calculateUpstream(network)
            } catch (_: Exception) {
                null
            }

        val results = linkedMapOf<String, SldCableSizingResult>()

        network.connections.forEach { connection ->

            val fromNode = nodeMap[connection.fromNodeId]
            val toNode = nodeMap[connection.toNodeId]

            if (fromNode == null || toNode == null) {
                results[connection.id] =
                    SldCableSizingResult(
                        connectionId = connection.id,
                        fromNodeId = connection.fromNodeId,
                        toNodeId = connection.toNodeId,
                        designCurrentA = 0.0,
                        shortCircuitCurrentKa = 0.0,
                        requiredCurrentCapacityA = 0.0,
                        maximumVoltageDropPercent =
                            voltageDropLimitPercent,
                        recommendedSizeMm2 = 0.0,
                        recommendedParallelRuns = 0,
                        recommendedMaterial = "N/A",
                        recommendedCores = 0,
                        recommendedCurrentCapacityA = 0.0,
                        recommendedVoltageDropPercent = 0.0,
                        recommendedShortCircuitWithstandKa = 0.0,
                        options = emptyList(),
                        notes = listOf(
                            "Invalid feeder connection."
                        )
                    )

                return@forEach
            }

            val designCurrent = calculateDesignCurrent(
                connection = connection,
                toNode = toNode,
                upstream = upstream
            )

            val faultCurrentKa =
                shortCircuitStudy
                    ?.results
                    ?.get(toNode.id)
                    ?.initialSymmetricalCurrentKa
                    ?: 0.0

            val requiredCurrent =
                designCurrent * 1.25

            val options = mutableListOf<SldCableOption>()

            listOf("Copper", "Aluminium").forEach { material ->

                catalog.forEach { entry ->

                    val baseCapacity =
                        if (material == "Copper") {
                            entry.copperCapacityA
                        } else {
                            entry.aluminiumCapacityA
                        }

                    if (baseCapacity <= 0.0) {
                        return@forEach
                    }

                    for (runs in 1..8) {

                        val totalCapacity =
                            baseCapacity * runs

                        if (totalCapacity + 0.001 <
                            requiredCurrent
                        ) {
                            continue
                        }

                        val resistance =
                            if (material == "Copper") {
                                entry.copperR
                            } else {
                                entry.aluminiumR
                            }

                        val effectiveR =
                            resistance / runs

                        val effectiveX =
                            entry.x / runs

                        val voltageDrop =
                            calculateVoltageDropPercent(
                                currentA = designCurrent,
                                resistanceOhmPerKm =
                                    effectiveR,
                                reactanceOhmPerKm =
                                    effectiveX,
                                lengthMeters =
                                    connection.lengthMeters,
                                voltageV =
                                    toNode.voltage
                            )

                        val shortCircuitWithstandKa =
                            calculateShortCircuitWithstand(
                                sizeMm2 = entry.sizeMm2 * runs,
                                material = material,
                                durationSeconds =
                                    shortCircuitTimeSeconds
                            )

                        val reasons = mutableListOf<String>()

                        if (
                            voltageDrop >
                            voltageDropLimitPercent
                        ) {
                            reasons.add(
                                "Voltage drop exceeds the permitted limit."
                            )
                        }

                        if (
                            faultCurrentKa > 0.0 &&
                            shortCircuitWithstandKa <
                            faultCurrentKa
                        ) {
                            reasons.add(
                                "Short-circuit thermal withstand is insufficient."
                            )
                        }

                        val acceptable =
                            totalCapacity >= requiredCurrent &&
                                voltageDrop <=
                                voltageDropLimitPercent &&
                                (
                                    faultCurrentKa <= 0.0 ||
                                        shortCircuitWithstandKa >=
                                        faultCurrentKa
                                    )

                        options.add(
                            SldCableOption(
                                sizeMm2 = entry.sizeMm2,
                                material = material,
                                cores = 3,
                                currentCapacityA =
                                    baseCapacity,
                                resistanceOhmPerKm =
                                    resistance,
                                reactanceOhmPerKm =
                                    entry.x,
                                voltageDropPercent =
                                    voltageDrop,
                                shortCircuitWithstandKa =
                                    shortCircuitWithstandKa,
                                parallelRuns = runs,
                                totalCurrentCapacityA =
                                    totalCapacity,
                                acceptable = acceptable,
                                reasons = reasons
                            )
                        )
                    }
                }
            }

            val acceptableOptions =
                options.filter { it.acceptable }

            val recommended =
                acceptableOptions.minWithOrNull(
                    compareBy<SldCableOption>(
                        {
                            it.sizeMm2 *
                                it.parallelRuns
                        },
                        {
                            if (
                                it.material == "Copper"
                            ) 0 else 1
                        },
                        {
                            it.voltageDropPercent
                        }
                    )
                )

            val notes = mutableListOf<String>()

            if (designCurrent <= 0.0) {
                notes.add(
                    "No valid design current was obtained from the SLD."
                )
            }

            if (
                connection.lengthMeters <= 0.0
            ) {
                notes.add(
                    "Feeder length is zero; enter the actual cable length."
                )
            }

            if (faultCurrentKa <= 0.0) {
                notes.add(
                    "Short-circuit result is not available for this feeder."
                )
            }

            if (recommended == null) {
                notes.add(
                    "No catalog option satisfies ampacity, voltage-drop and short-circuit criteria."
                )
            }

            val selected =
                recommended

            results[connection.id] =
                SldCableSizingResult(
                    connectionId = connection.id,
                    fromNodeId = connection.fromNodeId,
                    toNodeId = connection.toNodeId,
                    designCurrentA = designCurrent,
                    shortCircuitCurrentKa =
                        faultCurrentKa,
                    requiredCurrentCapacityA =
                        requiredCurrent,
                    maximumVoltageDropPercent =
                        voltageDropLimitPercent,
                    recommendedSizeMm2 =
                        selected?.sizeMm2 ?: 0.0,
                    recommendedParallelRuns =
                        selected?.parallelRuns ?: 0,
                    recommendedMaterial =
                        selected?.material ?: "N/A",
                    recommendedCores =
                        selected?.cores ?: 0,
                    recommendedCurrentCapacityA =
                        selected?.totalCurrentCapacityA ?: 0.0,
                    recommendedVoltageDropPercent =
                        selected?.voltageDropPercent ?: 0.0,
                    recommendedShortCircuitWithstandKa =
                        selected?.shortCircuitWithstandKa ?: 0.0,
                    options = options,
                    notes = notes
                )
        }

        val successful =
            results.values.count {
                it.recommendedSizeMm2 > 0.0
            }

        val failed =
            results.size - successful

        val notes = mutableListOf<String>()

        notes.add(
            "Cable sizing evaluates ampacity, voltage drop and short-circuit thermal withstand."
        )

        notes.add(
            "A 25% design-current margin is applied before cable selection."
        )

        if (failed > 0) {
            notes.add(
                "$failed feeder(s) require additional design data or a larger cable arrangement."
            )
        }

        return SldCableSizingStudy(
            results = results,
            successfulFeeders = successful,
            failedFeeders = failed,
            notes = notes
        )
    }

    private fun calculateDesignCurrent(
        connection: SldConnection,
        toNode: SldNode,
        upstream: SldCalculationResult?
    ): Double {

        val upstreamCurrent =
            upstream
                ?.nodeResults
                ?.get(toNode.id)
                ?.feederRequiredCurrentA
                ?: 0.0

        if (upstreamCurrent > 0.0) {
            return upstreamCurrent
        }

        val kva =
            if (toNode.loadKw > 0.0) {
                toNode.loadKw /
                    toNode.powerFactor.coerceIn(
                        0.01,
                        1.0
                    )
            } else {
                toNode.ratedKva
            }

        if (kva <= 0.0) {
            return connection.currentCapacityA
                .coerceAtLeast(0.0)
        }

        return (
            kva * 1000.0
            ) / (
            SQRT_3 *
                toNode.voltage.coerceAtLeast(1.0)
            )
    }

    private fun calculateVoltageDropPercent(
        currentA: Double,
        resistanceOhmPerKm: Double,
        reactanceOhmPerKm: Double,
        lengthMeters: Double,
        voltageV: Double
    ): Double {

        if (
            currentA <= 0.0 ||
            lengthMeters <= 0.0 ||
            voltageV <= 0.0
        ) {
            return 0.0
        }

        val lengthKm =
            lengthMeters / 1000.0

        val impedance =
            sqrt(
                (
                    resistanceOhmPerKm *
                        lengthKm
                    ).let {
                        it * it
                    } +
                    (
                        reactanceOhmPerKm *
                            lengthKm
                        ).let {
                            it * it
                        }
            )

        return (
            SQRT_3 *
                currentA *
                impedance /
                voltageV
            ) * 100.0
    }

    private fun calculateShortCircuitWithstand(
        sizeMm2: Double,
        material: String,
        durationSeconds: Double
    ): Double {

        if (
            sizeMm2 <= 0.0 ||
            durationSeconds <= 0.0
        ) {
            return 0.0
        }

        val k =
            if (material == "Copper") {
                DEFAULT_K_FACTOR_CU
            } else {
                DEFAULT_K_FACTOR_AL
            }

        val currentA =
            k *
                sizeMm2 /
                sqrt(durationSeconds)

        return currentA / 1000.0
    }
}
