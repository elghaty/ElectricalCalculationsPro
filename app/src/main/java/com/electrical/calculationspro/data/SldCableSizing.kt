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

    /*
     * This margin is retained for compatibility with the existing
     * SLD cable-sizing workflow.
     *
     * The selected StandardEngine remains authoritative for:
     *
     * - conductor sections
     * - ampacity
     * - temperature correction
     * - grouping correction
     *
     * The margin is applied only to the calculated design current
     * before selecting a conductor arrangement.
     */
    private const val DESIGN_CURRENT_MARGIN = 1.25

    private const val DEFAULT_INSTALLATION_METHOD = "B1"
    private const val DEFAULT_LOADED_CONDUCTORS = 3

    /*
     * Resistance/reactance values are used ONLY for voltage-drop
     * evaluation.
     *
     * They are not used as the source of standard ampacity.
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

        /*
         * SldTopologyEngine is the authoritative direction/topology
         * layer.
         */
        val topology =
            SldTopologyEngine.build(network)

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        /*
         * Reuse the already calculated upstream result whenever
         * supplied by SldEngineeringFacade.
         *
         * This prevents duplicate upstream calculations during
         * one engineering cycle.
         */
        val upstream =
            upstreamEngineering
                ?: SldUpstreamEngineering.calculate(
                    network = network,
                    engineeringContext = engineeringContext
                )

        val effectiveVoltageDropLimit =
            engineeringContext
                ?.effectiveVoltageDropLimitPercent()
                ?: voltageDropLimitPercent

        val engine =
            engineeringContext?.standardEngine

        val standardSections =
            engine
                ?.standardConductorSections()
                ?.filter {
                    it > 0.0
                }
                ?.distinct()
                ?.sorted()
                ?: impedanceCatalog
                    .map {
                        it.sizeMm2
                    }

        val results =
            linkedMapOf<String, SldCableSizingResult>()

        /*
         * IMPORTANT:
         *
         * BUSBAR connections are internal panel connections.
         *
         * They are NOT cable feeders and therefore must never enter
         * cable sizing, cable length, voltage-drop or cable selection.
         */
        topology.connections
            .filter {
                it.connectionType == SldConnectionType.CABLE
            }
            .forEach { connection ->

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

                /*
                 * Respect connection-level cable settings where
                 * supplied. Otherwise use engineering defaults.
                 */
                val material =
                    parseConductorMaterial(
                        connection.conductorMaterial
                    )

                val insulation =
                    parseInsulationType(
                        connection.insulationType
                    )

                val installationMethod =
                    InstallationMethod(
                        code =
                            connection.installationMethodCode
                                .ifBlank {
                                    DEFAULT_INSTALLATION_METHOD
                                },
                        description =
                            "SLD feeder installation method"
                    )

                val loadedConductors =
                    connection.loadedConductors
                        .coerceAtLeast(1)

                val configuredParallelRuns =
                    connection.parallelRuns
                        .coerceAtLeast(1)

                val temperatureFactor =
                    engineeringContext
                        ?.ambientTemperatureFactor(
                            insulation
                        )
                        ?: 1.0

                val groupingFactor =
                    engineeringContext
                        ?.groupingFactor()
                        ?: 1.0

                val options =
                    mutableListOf<SldCableOption>()

                /*
                 * If the connection already specifies a conductor
                 * material, use that material as the first candidate.
                 *
                 * Existing projects default to Copper, so Copper remains
                 * the normal selection.
                 */
                val materials =
                    if (
                        connection.conductorMaterial
                            .isBlank()
                    ) {
                        listOf(
                            ConductorMaterial.Copper,
                            ConductorMaterial.Aluminum
                        )
                    } else {
                        listOf(material)
                    }

                materials.forEach { candidateMaterial ->

                    standardSections.forEach { section ->

                        val baseAmpacity =
                            if (engine != null) {
                                engine.conductorAmpacity(
                                    sectionMm2 = section,
                                    material = candidateMaterial,
                                    insulation = insulation,
                                    installationMethod =
                                        installationMethod,
                                    loadedConductors =
                                        loadedConductors
                                )
                            } else {
                                legacyAmpacity(
                                    section = section,
                                    material = candidateMaterial
                                )
                            }

                        /*
                         * Missing standard ampacity must never be
                         * interpreted as zero/acceptable.
                         */
                        if (
                            baseAmpacity == null ||
                            baseAmpacity <= 0.0
                        ) {
                            return@forEach
                        }

                        val correctedAmpacity =
                            baseAmpacity *
                                temperatureFactor *
                                groupingFactor

                        if (correctedAmpacity <= 0.0) {
                            return@forEach
                        }

                        /*
                         * Evaluate from one run upward.
                         *
                         * If the user already supplied a parallel-run
                         * arrangement, make that arrangement the first
                         * evaluated option but still allow engineering
                         * selection to increase it where necessary.
                         */
                        val minimumRuns =
                            configuredParallelRuns
                                .coerceIn(1, 8)

                        for (
                            runs in
                            minimumRuns..8
                        ) {

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
                                when (candidateMaterial) {

                                    ConductorMaterial.Copper ->
                                        impedance?.copperR
                                            ?: 0.0

                                    ConductorMaterial.Aluminum ->
                                        impedance?.aluminiumR
                                            ?: 0.0
                                }

                            val reactance =
                                impedance?.x
                                    ?: 0.0

                            val effectiveR =
                                resistance /
                                    runs

                            val effectiveX =
                                reactance /
                                    runs

                            val voltageDrop =
                                calculateVoltageDropPercent(
                                    currentA =
                                        designCurrent,
                                    resistanceOhmPerKm =
                                        effectiveR,
                                    reactanceOhmPerKm =
                                        effectiveX,
                                    lengthMeters =
                                        connection.lengthMeters,
                                    voltageV =
                                        toNode.voltage,
                                    phaseSystem =
                                        toNode.phaseSystem
                                )

                            val shortCircuitWithstandKa =
                                calculateShortCircuitWithstand(
                                    sizeMm2 =
                                        section * runs,
                                    material =
                                        candidateMaterial,
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
                                connection.lengthMeters > 0.0 &&
                                impedance == null
                            ) {
                                reasons.add(
                                    "Voltage-drop impedance data is unavailable for this conductor section."
                                )
                            }

                            if (
                                connection.lengthMeters <= 0.0
                            ) {
                                reasons.add(
                                    "Feeder length is zero; voltage-drop verification requires the actual cable length."
                                )
                            }

                            val acceptable =
                                totalCapacity >=
                                    requiredCurrent &&
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
                                    sizeMm2 =
                                        section,

                                    material =
                                        candidateMaterial
                                            .displayName(),

                                    cores =
                                        loadedConductors,

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

                                    parallelRuns =
                                        runs,

                                    totalCurrentCapacityA =
                                        totalCapacity,

                                    acceptable =
                                        acceptable,

                                    reasons =
                                        reasons
                                )
                            )
                        }
                    }
                }

                val acceptableOptions =
                    options.filter {
                        it.acceptable
                    }

                /*
                 * Selection priority:
                 *
                 * 1. Minimum total installed conductor area.
                 * 2. Copper before Aluminium when equivalent.
                 * 3. Lower voltage drop.
                 * 4. Lower number of parallel runs.
                 */
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
                            },
                            {
                                it.parallelRuns
                            }
                        )
                    )

                val notes =
                    mutableListOf<String>()

                if (designCurrent <= 0.0) {
                    notes.add(
                        "No valid design current was obtained from the SLD upstream study."
                    )
                }

                if (connection.lengthMeters <= 0.0) {
                    notes.add(
                        "Feeder length is zero; voltage-drop verification is not complete until the actual length is entered."
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
                            engineeringContext
                                .standardImplementationStatus
                    )

                    notes.add(
                        "Installation method: " +
                            installationMethod.code
                    )

                    notes.add(
                        "Loaded conductors: " +
                            loadedConductors
                    )

                    notes.add(
                        "Ambient temperature: " +
                            engineeringContext
                                .ambientTemperatureC +
                            " °C."
                    )

                    notes.add(
                        "Grouping circuits: " +
                            engineeringContext
                                .numberOfCircuits +
                            "."
                    )

                    notes.add(
                        "Temperature correction factor: " +
                            temperatureFactor
                    )

                    notes.add(
                        "Grouping correction factor: " +
                            groupingFactor
                    )
                } else {

                    notes.add(
                        "Backward-compatible cable sizing mode used without an explicit engineering context."
                    )
                }

                notes.add(
                    "Connection type: CABLE."
                )

                notes.add(
                    "Feeder direction was normalized and preserved by SldTopologyEngine."
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
                            recommended
                                ?.sizeMm2
                                ?: 0.0,

                        recommendedParallelRuns =
                            recommended
                                ?.parallelRuns
                                ?: 0,

                        recommendedMaterial =
                            recommended
                                ?.material
                                ?: "N/A",

                        recommendedCores =
                            recommended
                                ?.cores
                                ?: 0,

                        recommendedCurrentCapacityA =
                            recommended
                                ?.totalCurrentCapacityA
                                ?: 0.0,

                        recommendedVoltageDropPercent =
                            recommended
                                ?.voltageDropPercent
                                ?: 0.0,

                        recommendedShortCircuitWithstandKa =
                            recommended
                                ?.shortCircuitWithstandKa
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
            results.size -
                successful

        val notes =
            mutableListOf<String>()

        notes.add(
            "Cable sizing evaluates external CABLE feeders only."
        )

        notes.add(
            "BUSBAR connections are intentionally excluded from cable sizing."
        )

        notes.add(
            "Cable ampacity is obtained from the selected StandardEngine when an engineering context is supplied."
        )

        notes.add(
            "Conductor sections are obtained from the selected StandardEngine when an engineering context is supplied."
        )

        notes.add(
            "Ambient-temperature and grouping correction factors are obtained from the selected StandardEngine."
        )

        notes.add(
            "Connection-level conductor material, insulation, installation method and loaded-conductor count are respected."
        )

        notes.add(
            "Short-circuit thermal withstand is checked using the configured clearing time."
        )

        notes.add(
            "The selected project electrical standard controls the standard-engine cable dataset."
        )

        if (engineeringContext != null) {

            notes.add(
                "Active standard: " +
                    "${engineeringContext.codeName} " +
                    "(${engineeringContext.codeRevision})."
            )
        }

        if (failed > 0) {

            notes.add(
                "$failed feeder(s) require additional design data or a larger cable arrangement."
            )
        }

        return SldCableSizingStudy(
            results =
                results,
            successfulFeeders =
                successful,
            failedFeeders =
                failed,
            notes =
                notes
        )
    }

    private fun calculateDesignCurrent(
        connection: SldConnection,
        toNode: SldNode,
        upstream: SldUpstreamEngineering.Result
    ): Double {

        /*
         * Prefer the authoritative upstream feeder current.
         *
         * This is essential for automatic upstream propagation.
         */
        val upstreamCurrent =
            upstream
                .feeders
                .firstOrNull {
                    it.connectionId ==
                        connection.id
                }
                ?.currentA
                ?: 0.0

        if (upstreamCurrent > 0.0) {
            return upstreamCurrent
        }

        /*
         * Compatibility fallback for a feeder that has not yet
         * received a usable upstream result.
         */
        val powerFactor =
            toNode.powerFactor
                .coerceIn(
                    0.01,
                    1.0
                )

        val kva =
            if (toNode.loadKw > 0.0) {

                toNode.loadKw /
                    powerFactor

            } else {

                toNode.ratedKva
            }

        if (kva <= 0.0) {

            return connection
                .currentCapacityA
                .coerceAtLeast(0.0)
        }

        return when (toNode.phaseSystem) {

            SldPhaseSystem.THREE_PHASE -> {

                (
                    kva * 1000.0
                    ) / (
                    SQRT_3 *
                        toNode.voltage
                            .coerceAtLeast(1.0)
                    )
            }

            SldPhaseSystem.SINGLE_PHASE -> {

                (
                    kva * 1000.0
                    ) / (
                    toNode.voltage
                        .coerceAtLeast(1.0)
                    )
            }

            SldPhaseSystem.DC -> {

                (
                    kva * 1000.0
                    ) / (
                    toNode.voltage
                        .coerceAtLeast(1.0)
                    )
            }
        }
    }

    private fun legacyAmpacity(
        section: Double,
        material: ConductorMaterial
    ): Double? {

        /*
         * Keep the legacy path only for compatibility when no
         * engineering context is supplied.
         *
         * StandardEngine is authoritative whenever context exists.
         */
        val entry =
            impedanceCatalog.firstOrNull {
                approximatelyEqual(
                    it.sizeMm2,
                    section
                )
            }

        if (entry == null) {
            return null
        }

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
        voltageV: Double,
        phaseSystem: SldPhaseSystem
    ): Double {

        if (
            currentA <= 0.0 ||
            lengthMeters <= 0.0 ||
            voltageV <= 0.0
        ) {
            return 0.0
        }

        val lengthKm =
            lengthMeters /
                1000.0

        val r =
            resistanceOhmPerKm *
                lengthKm

        val x =
            reactanceOhmPerKm *
                lengthKm

        /*
         * The existing SLD voltage-drop engine uses the impedance
         * magnitude as its engineering approximation.
         *
         * Preserve that behavior while respecting the phase-system
         * voltage relationship.
         */
        val impedance =
            sqrt(
                r * r +
                    x * x
            )

        val voltageDropV =
            when (phaseSystem) {

                SldPhaseSystem.THREE_PHASE -> {

                    SQRT_3 *
                        currentA *
                        impedance
                }

                SldPhaseSystem.SINGLE_PHASE -> {

                    2.0 *
                        currentA *
                        impedance
                }

                SldPhaseSystem.DC -> {

                    2.0 *
                        currentA *
                        resistanceOhmPerKm *
                        lengthKm
                }
            }

        return (
            voltageDropV /
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
                sqrt(
                    durationSeconds
                )

        return currentA /
            1000.0
    }

    private fun parseConductorMaterial(
        value: String
    ): ConductorMaterial {

        return when (
            value.trim().lowercase()
        ) {

            "aluminium",
            "aluminum",
            "alu" ->
                ConductorMaterial.Aluminum

            else ->
                ConductorMaterial.Copper
        }
    }

    private fun parseInsulationType(
        value: String
    ): InsulationType {

        return when (
            value.trim().uppercase()
        ) {

            "XLPE" ->
                InsulationType.XLPE

            "EPR" ->
                InsulationType.EPR

            "RUBBER" ->
                InsulationType.Rubber

            else ->
                InsulationType.PVC
        }
    }

    private fun approximatelyEqual(
        first: Double,
        second: Double
    ): Boolean =
        kotlin.math.abs(
            first - second
        ) < 0.0001

    private fun ConductorMaterial.displayName(): String =
        when (this) {

            ConductorMaterial.Copper ->
                "Copper"

            ConductorMaterial.Aluminum ->
                "Aluminium"
        }
}
