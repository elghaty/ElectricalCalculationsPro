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
    private const val DESIGN_CURRENT_MARGIN = 1.25

    private const val DEFAULT_INSTALLATION_METHOD = "B1"
    private const val DEFAULT_LOADED_CONDUCTORS = 3

    /*
     * Resistance/reactance data is retained only for voltage-drop
     * evaluation. Ampacity and standard cable sections are NOT
     * taken from this table anymore.
     *
     * The selected StandardEngine is now authoritative for:
     *
     * - standard conductor sections
     * - conductor ampacity
     * - correction factors
     * - selected engineering standard
     */
    private data class ImpedanceEntry(
        val sizeMm2: Double,
        val copperR: Double,
        val aluminiumR: Double,
        val x: Double
    )

    private val impedanceCatalog = listOf(
        ImpedanceEntry(1.5, 12.1, 0.0, 0.080),
        ImpedanceEntry(2.5, 7.41, 0.0, 0.080),
        ImpedanceEntry(4.0, 4.61, 0.0, 0.079),
        ImpedanceEntry(6.0, 3.08, 0.0, 0.078),
        ImpedanceEntry(10.0, 1.83, 3.08, 0.076),
        ImpedanceEntry(16.0, 1.15, 1.91, 0.074),
        ImpedanceEntry(25.0, 0.727, 1.20, 0.073),
        ImpedanceEntry(35.0, 0.524, 0.868, 0.072),
        ImpedanceEntry(50.0, 0.387, 0.641, 0.071),
        ImpedanceEntry(70.0, 0.268, 0.443, 0.070),
        ImpedanceEntry(95.0, 0.193, 0.320, 0.069),
        ImpedanceEntry(120.0, 0.153, 0.253, 0.068),
        ImpedanceEntry(150.0, 0.124, 0.206, 0.067),
        ImpedanceEntry(185.0, 0.099, 0.164, 0.066),
        ImpedanceEntry(240.0, 0.0754, 0.125, 0.065),
        ImpedanceEntry(300.0, 0.0601, 0.100, 0.064),
        ImpedanceEntry(400.0, 0.0470, 0.0778, 0.063),
        ImpedanceEntry(500.0, 0.0366, 0.0605, 0.062),
        ImpedanceEntry(630.0, 0.0283, 0.0469, 0.061)
    )

    fun calculate(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        voltageDropLimitPercent: Double = DEFAULT_VOLTAGE_DROP_LIMIT,
        shortCircuitTimeSeconds: Double = DEFAULT_SHORT_CIRCUIT_TIME_SECONDS,
        upstreamEngineering: SldUpstreamEngineering.Result? = null,
        engineeringContext: SldEngineeringContext? = null
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

        engineeringContext?.let {
            it.copy(
                requireImplementedStandard = false
            ).validate()
        }

        val topology =
            SldTopologyEngine.build(network)

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val upstream =
            upstreamEngineering
                ?: SldUpstreamEngineering.calculate(network)

        val effectiveVoltageDropLimit =
            engineeringContext?.effectiveVoltageDropLimitPercent()
                ?: voltageDropLimitPercent

        val engine =
            engineeringContext?.standardEngine

        val standardSections =
            engine
                ?.standardConductorSections()
                ?.filter { it > 0.0 }
                ?.distinct()
                ?.sorted()
                ?: impedanceCatalog.map { it.sizeMm2 }

        val results =
            linkedMapOf<String, SldCableSizingResult>()

        topology.connections.forEach { connection ->

            val fromNode =
                nodeMap[connection.fromNodeId]

            val toNode =
                nodeMap[connection.toNodeId]

            require(fromNode != null) {
                "Connection ${connection.id}: upstream node does not exist."
            }

            require(toNode != null) {
                "Connection ${connection.id}: downstream node does not exist."
            }

            val designCurrent =
                calculateDesignCurrent(
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
                designCurrent * DESIGN_CURRENT_MARGIN

            val options =
                mutableListOf<SldCableOption>()

            val materials =
                listOf(
                    ConductorMaterial.Copper,
                    ConductorMaterial.Aluminum
                )

            materials.forEach { material ->

                standardSections.forEach { section ->

                    val baseAmpacity =
                        if (engine != null) {
                            engine.conductorAmpacity(
                                sectionMm2 = section,
                                material = material,
                                insulation = InsulationType.PVC,
                                installationMethod =
                                    InstallationMethod(
                                        DEFAULT_INSTALLATION_METHOD,
                                        "Default engineering installation method"
                                    ),
                                loadedConductors =
                                    DEFAULT_LOADED_CONDUCTORS
                            )
                        } else {
                            legacyAmpacity(
                                section = section,
                                material = material
                            )
                        }

                    if (baseAmpacity == null || baseAmpacity <= 0.0) {
                        return@forEach
                    }

                    val temperatureFactor =
                        engineeringContext
                            ?.ambientTemperatureFactor(
                                InsulationType.PVC
                            )
                            ?: 1.0

                    val groupingFactor =
                        engineeringContext
                            ?.groupingFactor()
                            ?: 1.0

                    val correctedAmpacity =
                        baseAmpacity *
                            temperatureFactor *
                            groupingFactor

                    if (correctedAmpacity <= 0.0) {
                        return@forEach
                    }

                    for (runs in 1..8) {

                        val totalCapacity =
                            correctedAmpacity * runs

                        if (
                            totalCapacity + 0.001 <
                            requiredCurrent
                        ) {
                            continue
                        }

                        val impedance =
                            impedanceCatalog.firstOrNull {
                                approximatelyEqual(
                                    it.sizeMm2,
                                    section
                                )
                            }

                        val resistance =
                            when (material) {
                                ConductorMaterial.Copper ->
                                    impedance?.copperR ?: 0.0

                                ConductorMaterial.Aluminum ->
                                    impedance?.aluminiumR ?: 0.0
                            }

                        val reactance =
                            impedance?.x ?: 0.0

                        val effectiveR =
                            if (runs > 0) {
                                resistance / runs
                            } else {
                                resistance
                            }

                        val effectiveX =
                            if (runs > 0) {
                                reactance / runs
                            } else {
                                reactance
                            }

                        val voltageDrop =
                            calculateVoltageDropPercent(
                                currentA = designCurrent,
                                resistanceOhmPerKm = effectiveR,
                                reactanceOhmPerKm = effectiveX,
                                lengthMeters =
                                    connection.lengthMeters,
                                voltageV =
                                    toNode.voltage
                            )

                        val shortCircuitWithstandKa =
                            calculateShortCircuitWithstand(
                                sizeMm2 =
                                    section * runs,
                                material =
                                    material,
                                durationSeconds =
                                    shortCircuitTimeSeconds
                            )

                        val reasons =
                            mutableListOf<String>()

                        if (
                            voltageDrop >
                            effectiveVoltageDropLimit
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

                        if (
                            impedance == null &&
                            connection.lengthMeters > 0.0
                        ) {
                            reasons.add(
                                "Voltage-drop impedance data is unavailable for this conductor section."
                            )
                        }

                        val acceptable =
                            totalCapacity >= requiredCurrent &&
                                voltageDrop <=
                                effectiveVoltageDropLimit &&
                                (
                                    faultCurrentKa <= 0.0 ||
                                        shortCircuitWithstandKa >=
                                        faultCurrentKa
                                    ) &&
                                (
                                    connection.lengthMeters <= 0.0 ||
                                        impedance != null
                                    )

                        options.add(
                            SldCableOption(
                                sizeMm2 = section,
                                material =
                                    material.displayName(),
                                cores = 3,
                                currentCapacityA =
                                    correctedAmpacity,
                                resistanceOhmPerKm =
                                    resistance,
                                reactanceOhmPerKm =
                                    reactance,
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
                options.filter {
                    it.acceptable
                }

            val recommended =
                acceptableOptions.minWithOrNull(
                    compareBy<SldCableOption>(
                        {
                            it.sizeMm2 *
                                it.parallelRuns
                        },
                        {
                            if (
                                it.material.equals(
                                    "Copper",
                                    ignoreCase = true
                                )
                            ) {
                                0
                            } else {
                                1
                            }
                        },
                        {
                            it.voltageDropPercent
                        }
                    )
                )

            val notes =
                mutableListOf<String>()

            if (designCurrent <= 0.0) {
                notes.add(
                    "No valid design current was obtained from the SLD."
                )
            }

            if (connection.lengthMeters <= 0.0) {
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
                    "No available cable option satisfies all active design criteria."
                )
            }

            if (engineeringContext != null) {
                notes.add(
                    "Standard: ${engineeringContext.codeName}."
                )

                notes.add(
                    "Code revision: ${engineeringContext.codeRevision}."
                )

                notes.add(
                    "Standard implementation status: " +
                        engineeringContext.standardImplementationStatus
                )

                notes.add(
                    "Installation method used by the standard engine: " +
                        DEFAULT_INSTALLATION_METHOD
                )

                notes.add(
                    "Loaded conductors used by the standard engine: " +
                        DEFAULT_LOADED_CONDUCTORS
                )

                notes.add(
                    "Ambient temperature: " +
                        engineeringContext.ambientTemperatureC +
                        " °C."
                )

                notes.add(
                    "Grouping circuits: " +
                        engineeringContext.numberOfCircuits +
                        "."
                )
            } else {
                notes.add(
                    "Backward-compatible cable sizing mode used without an explicit engineering context."
                )
            }

            notes.add(
                "Electrical feeder direction was normalized by SldTopologyEngine."
            )

            notes.add(
                "Shared upstream engineering results were reused when supplied."
            )

            results[connection.id] =
                SldCableSizingResult(

                    connectionId =
                        connection.id,

                    fromNodeId =
                        connection.fromNodeId,

                    toNodeId =
                        connection.toNodeId,

                    designCurrentA =
                        designCurrent,

                    shortCircuitCurrentKa =
                        faultCurrentKa,

                    requiredCurrentCapacityA =
                        requiredCurrent,

                    maximumVoltageDropPercent =
                        effectiveVoltageDropLimit,

                    recommendedSizeMm2 =
                        recommended?.sizeMm2
                            ?: 0.0,

                    recommendedParallelRuns =
                        recommended?.parallelRuns
                            ?: 0,

                    recommendedMaterial =
                        recommended?.material
                            ?: "N/A",

                    recommendedCores =
                        recommended?.cores
                            ?: 0,

                    recommendedCurrentCapacityA =
                        recommended?.totalCurrentCapacityA
                            ?: 0.0,

                    recommendedVoltageDropPercent =
                        recommended?.voltageDropPercent
                            ?: 0.0,

                    recommendedShortCircuitWithstandKa =
                        recommended?.shortCircuitWithstandKa
                            ?: 0.0,

                    options =
                        options,

                    notes =
                        notes
                )
        }

        val successful =
            results.values.count {
                it.recommendedSizeMm2 > 0.0
            }

        val failed =
            results.size - successful

        val notes =
            mutableListOf<String>()

        notes.add(
            "Cable sizing evaluates ampacity, voltage drop and short-circuit thermal withstand."
        )

        notes.add(
            "A ${(DESIGN_CURRENT_MARGIN - 1.0) * 100.0}% design-current margin is applied before cable selection."
        )

        notes.add(
            "Conductor sections are obtained from the selected StandardEngine when an engineering context is supplied."
        )

        notes.add(
            "Conductor ampacity is obtained from the selected StandardEngine when an engineering context is supplied."
        )

        notes.add(
            "Ambient-temperature and grouping factors are obtained from the selected StandardEngine."
        )

        notes.add(
            "The selected project electrical standard therefore controls the cable-sizing dataset."
        )

        if (engineeringContext != null) {
            notes.add(
                "Active standard: ${engineeringContext.codeName} (${engineeringContext.codeRevision})."
            )
        }

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
        upstream: SldUpstreamEngineering.Result
    ): Double {

        val upstreamCurrent =
            upstream
                .feeders
                .firstOrNull {
                    it.connectionId == connection.id
                }
                ?.currentA
                ?: 0.0

        if (upstreamCurrent > 0.0) {
            return upstreamCurrent
        }

        val powerFactor =
            toNode.powerFactor
                .coerceIn(0.01, 1.0)

        val kva =
            if (toNode.loadKw > 0.0) {
                toNode.loadKw / powerFactor
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

    private fun legacyAmpacity(
        section: Double,
        material: ConductorMaterial
    ): Double? {

        val entry =
            impedanceCatalog.firstOrNull {
                approximatelyEqual(
                    it.sizeMm2,
                    section
                )
            } ?: return null

        return when (material) {
            ConductorMaterial.Copper ->
                legacyCopperAmpacity(section)

            ConductorMaterial.Aluminum ->
                legacyAluminiumAmpacity(section)
        }
    }

    private fun legacyCopperAmpacity(
        section: Double
    ): Double? =
        when (section) {
            1.5 -> 18.0
            2.5 -> 24.0
            4.0 -> 32.0
            6.0 -> 41.0
            10.0 -> 57.0
            16.0 -> 76.0
            25.0 -> 101.0
            35.0 -> 125.0
            50.0 -> 150.0
            70.0 -> 195.0
            95.0 -> 235.0
            120.0 -> 270.0
            150.0 -> 305.0
            185.0 -> 345.0
            240.0 -> 400.0
            300.0 -> 455.0
            400.0 -> 530.0
            500.0 -> 610.0
            630.0 -> 690.0
            else -> null
        }

    private fun legacyAluminiumAmpacity(
        section: Double
    ): Double? =
        when (section) {
            10.0 -> 50.0
            16.0 -> 65.0
            25.0 -> 85.0
            35.0 -> 105.0
            50.0 -> 125.0
            70.0 -> 160.0
            95.0 -> 195.0
            120.0 -> 225.0
            150.0 -> 255.0
            185.0 -> 285.0
            240.0 -> 335.0
            300.0 -> 380.0
            400.0 -> 440.0
            500.0 -> 505.0
            630.0 -> 575.0
            else -> null
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

        val r =
            resistanceOhmPerKm *
                lengthKm

        val x =
            reactanceOhmPerKm *
                lengthKm

        val impedance =
            sqrt(
                r * r +
                    x * x
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
        material: ConductorMaterial,
        durationSeconds: Double
    ): Double {

        if (
            sizeMm2 <= 0.0 ||
            durationSeconds <= 0.0
        ) {
            return 0.0
        }

        val k =
            when (material) {
                ConductorMaterial.Copper ->
                    DEFAULT_K_FACTOR_CU

                ConductorMaterial.Aluminum ->
                    DEFAULT_K_FACTOR_AL
            }

        val currentA =
            k *
                sizeMm2 /
                sqrt(durationSeconds)

        return currentA / 1000.0
    }

    private fun approximatelyEqual(
        first: Double,
        second: Double
    ): Boolean =
        kotlin.math.abs(first - second) < 0.0001

    private fun ConductorMaterial.displayName(): String =
        when (this) {
            ConductorMaterial.Copper ->
                "Copper"

            ConductorMaterial.Aluminum ->
                "Aluminium"
        }
}
