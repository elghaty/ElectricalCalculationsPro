package com.electrical.calculationspro.data

import kotlin.math.sqrt

/**
 * SINGLE ENGINEERING CALCULATION CORE.
 *
 * All electrical calculations in the application must pass through this object.
 *
 * The engine performs:
 * - Design current
 * - Demand/diversity application
 * - Voltage drop
 * - Conductor ampacity selection
 * - Protective-device selection
 * - Short-circuit estimation
 * - Power calculations
 *
 * IMPORTANT:
 * The ampacity tables themselves are supplied by IecTables.
 * Standard selection currently controls the engineering reference notes.
 * It does not magically convert IEC ampacity tables into NEC/CEC tables.
 */
object ElectricalCalculations {

    private const val MIN_POSITIVE = 1.0e-9

    private val standardBreakers = listOf(
        6.0,
        10.0,
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
        315.0,
        400.0,
        500.0,
        630.0
    )

    fun calculateDesignCurrent(
        loadWatts: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double {

        require(loadWatts >= 0.0) {
            "Load cannot be negative."
        }

        require(voltage > MIN_POSITIVE) {
            "Voltage must be greater than zero."
        }

        require(powerFactor > 0.0 && powerFactor <= 1.0) {
            "Power factor must be greater than 0 and not greater than 1."
        }

        return when (currentType) {

            CurrentType.DirectCurrent -> {
                loadWatts / voltage
            }

            CurrentType.AlternatingSinglePhase -> {
                loadWatts / (voltage * powerFactor)
            }

            CurrentType.AlternatingTwoPhase -> {
                /*
                 * Two-phase calculation assumes voltage is the voltage
                 * of each phase and the two phases carry equal current.
                 */
                loadWatts /
                    (2.0 * voltage * powerFactor)
            }

            CurrentType.AlternatingThreePhase -> {
                loadWatts /
                    (sqrt(3.0) * voltage * powerFactor)
            }
        }
    }

    /**
     * Demand factor is normally <= 1.
     *
     * Diversity factor in conventional electrical terminology is normally
     * greater than or equal to 1 and is NOT multiplied directly into Ib.
     *
     * Therefore the optional diversity input here represents a
     * "diversity utilization factor" / coincidence factor <= 1.
     */
    fun applyDemandAndDiversity(
        ib: Double,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): Double {

        require(ib >= 0.0) {
            "Design current cannot be negative."
        }

        require(demandFactor in 0.0..1.0) {
            "Demand factor must be between 0 and 1."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity/coincidence factor must be between 0 and 1."
        }

        return ib *
            demandFactor *
            diversityFactor
    }

    fun calculateVoltageDrop(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> {

        require(current >= 0.0) {
            "Current cannot be negative."
        }

        require(length >= 0.0) {
            "Length cannot be negative."
        }

        require(sectionMm2 > MIN_POSITIVE) {
            "Conductor section must be greater than zero."
        }

        require(voltage > MIN_POSITIVE) {
            "Voltage must be greater than zero."
        }

        require(powerFactor > 0.0 && powerFactor <= 1.0) {
            "Power factor must be greater than 0 and not greater than 1."
        }

        /*
         * Approximate conductor resistivity at normal operating conditions.
         *
         * Copper: Ω·mm²/m
         * Aluminium: Ω·mm²/m
         */
        val resistivity = when (material) {
            ConductorMaterial.Copper -> 0.0225
            ConductorMaterial.Aluminum -> 0.0360
        }

        val resistancePerMeter =
            resistivity / sectionMm2

        /*
         * Approximate cable reactance:
         * 0.08 Ω/km.
         */
        val reactancePerMeter =
            0.08 / 1000.0

        val cosPhi =
            powerFactor.coerceIn(
                MIN_POSITIVE,
                1.0
            )

        val sinPhi =
            sqrt(
                (1.0 - cosPhi * cosPhi)
                    .coerceAtLeast(0.0)
            )

        val systemFactor = when (currentType) {

            CurrentType.DirectCurrent ->
                2.0

            CurrentType.AlternatingSinglePhase ->
                2.0

            CurrentType.AlternatingTwoPhase ->
                2.0

            CurrentType.AlternatingThreePhase ->
                sqrt(3.0)
        }

        val dropVolts =
            systemFactor *
                current *
                length *
                (
                    resistancePerMeter * cosPhi +
                        reactancePerMeter * sinPhi
                    )

        val dropPercent =
            dropVolts /
                voltage *
                100.0

        return dropPercent to dropVolts
    }

    /**
     * Short-circuit estimation at the end of the cable.
     *
     * sourceIkKA is treated as the available 3-phase source short-circuit
     * current at the origin.
     *
     * The result is an engineering estimate and must not be presented as
     * a complete IEC 60909 study without transformer/source data,
     * temperature, X/R ratio and the required fault type.
     */
    fun calculateShortCircuitCurrent(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 50.0
    ): ShortCircuitResult {

        require(voltage > MIN_POSITIVE) {
            "Voltage must be greater than zero."
        }

        require(length >= 0.0) {
            "Length cannot be negative."
        }

        require(sectionMm2 > MIN_POSITIVE) {
            "Conductor section must be greater than zero."
        }

        require(sourceIkKA > MIN_POSITIVE) {
            "Source short-circuit current must be greater than zero."
        }

        val resistivity = when (material) {
            ConductorMaterial.Copper -> 0.018
            ConductorMaterial.Aluminum -> 0.029
        }

        /*
         * For a fault-loop estimate:
         * Rloop = rho × L × 2 / S
         */
        val cableResistance =
            resistivity *
                length *
                2.0 /
                sectionMm2

        /*
         * Approximate loop reactance:
         * X ≈ 0.08 Ω/km × 2L
         */
        val cableReactance =
            0.08 *
                length /
                1000.0 *
                2.0

        /*
         * The source Ik value is assumed to be 3-phase.
         */
        val sourceThreePhaseImpedance =
            (
                voltage /
                    sqrt(3.0)
                ) /
                (sourceIkKA * 1000.0)

        /*
         * For a single-phase/two-wire fault, a conservative engineering
         * estimate is obtained by using the phase loop voltage and the
         * corresponding source impedance.
         */
        val sourceImpedance =
            when (currentType) {

                CurrentType.AlternatingThreePhase ->
                    sourceThreePhaseImpedance

                CurrentType.AlternatingSinglePhase,
                CurrentType.AlternatingTwoPhase,
                CurrentType.DirectCurrent ->
                    sourceThreePhaseImpedance
            }

        val totalResistance =
            sourceImpedance +
                cableResistance

        val totalImpedance =
            sqrt(
                totalResistance * totalResistance +
                    cableReactance * cableReactance
            ).coerceAtLeast(MIN_POSITIVE)

        val faultVoltage =
            when (currentType) {

                CurrentType.AlternatingThreePhase ->
                    voltage / sqrt(3.0)

                CurrentType.AlternatingSinglePhase ->
                    voltage

                CurrentType.AlternatingTwoPhase ->
                    voltage

                CurrentType.DirectCurrent ->
                    voltage
            }

        val faultCurrent =
            faultVoltage /
                totalImpedance

        val faultCurrentKA =
            faultCurrent / 1000.0

        /*
         * I²t is explicitly calculated for 0.1 s only.
         * It is NOT a breaker manufacturer's let-through I²t.
         */
        val i2tAt100ms =
            faultCurrent *
                faultCurrent *
                0.1

        return ShortCircuitResult(
            ikAmps = faultCurrent,
            ikKA = faultCurrentKA,
            cableImpedance = totalImpedance,
            sourceImpedance = sourceImpedance,
            i2t = i2tAt100ms,
            notes = listOf(
                "Estimated fault current = %.2f kA"
                    .format(faultCurrentKA),

                "Estimated cable/source impedance = %.4f Ω"
                    .format(totalImpedance),

                "Source impedance = %.4f Ω"
                    .format(sourceImpedance),

                "I²t at assumed 0.10 s = %.0f A²s"
                    .format(i2tAt100ms)
            )
        )
    }

    fun sizeConductor(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        validateInput(input)

        val rawDesignCurrent =
            calculateDesignCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        val designCurrent =
            applyDemandAndDiversity(
                ib = rawDesignCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        val result =
            findBestSection(
                input = input,
                designCurrent = designCurrent,
                rawDesignCurrent = rawDesignCurrent,
                standard = standard
            )

        return result
    }

    /**
     * Re-evaluates a manually selected cable section.
     *
     * This prevents the UI from displaying a manually selected section
     * while keeping the ampacity/voltage-drop results from another section.
     */
    fun evaluateSelectedSection(
        input: ConductorSizingInput,
        selectedSection: Double,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        validateInput(input)

        require(selectedSection in standardSections) {
            "Selected section is not a standard available section."
        }

        val rawDesignCurrent =
            calculateDesignCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        val designCurrent =
            applyDemandAndDiversity(
                ib = rawDesignCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawDesignCurrent,
            standard = standard
        )
    }

    private fun findBestSection(
        input: ConductorSizingInput,
        designCurrent: Double,
        rawDesignCurrent: Double,
        standard: Standard
    ): ConductorSizingResult {

        val methodKey =
            IecTables.methodToKey(
                input.installationMethod.code
            )

        val loadedConductors =
            loadedConductorCount(input.currentType)

        val temperatureFactor =
            temperatureCorrection(
                input.insulation,
                input.ambientTemp
            )

        val groupingFactor =
            IecTables.groupingFactor(
                input.circuitsInConduit
            ).coerceAtLeast(MIN_POSITIVE)

        val requiredIz =
            designCurrent /
                (
                    temperatureFactor *
                        groupingFactor
                    )

        var ampacityCandidate: Double? = null

        for (section in standardSections) {

            val baseAmpacity =
                IecTables.getBaseAmpacity(
                    section = section,
                    methodKey = methodKey,
                    loadedConductors = loadedConductors,
                    conductor = input.conductor,
                    insulation = input.insulation
                )

            val correctedAmpacity =
                baseAmpacity *
                    temperatureFactor *
                    groupingFactor

            if (
                correctedAmpacity >= requiredIz &&
                ampacityCandidate == null
            ) {
                ampacityCandidate = section
            }

            if (correctedAmpacity < requiredIz) {
                continue
            }

            val drop =
                calculateVoltageDrop(
                    current = designCurrent,
                    length = input.lineLength,
                    sectionMm2 = section,
                    powerFactor = input.powerFactor,
                    currentType = input.currentType,
                    material = input.conductor,
                    voltage = input.voltage
                )

            if (drop.first <= input.maxVoltageDrop) {

                return evaluateSection(
                    input = input,
                    section = section,
                    designCurrent = designCurrent,
                    rawDesignCurrent = rawDesignCurrent,
                    standard = standard
                )
            }
        }

        /*
         * No section satisfies both ampacity and voltage-drop requirements.
         *
         * If an ampacity-valid section exists, return it explicitly marked
         * as a voltage-drop failure.
         *
         * Otherwise return the largest available section with a clear
         * engineering failure note.
         */
        val fallback =
            ampacityCandidate
                ?: standardSections.last()

        return evaluateSection(
            input = input,
            section = fallback,
            designCurrent = designCurrent,
            rawDesignCurrent = rawDesignCurrent,
            standard = standard,
            forceVoltageDropWarning = true,
            requiredIzOverride = requiredIz
        )
    }

    private fun evaluateSection(
        input: ConductorSizingInput,
        section: Double,
        designCurrent: Double,
        rawDesignCurrent: Double,
        standard: Standard,
        forceVoltageDropWarning: Boolean = false,
        requiredIzOverride: Double? = null
    ): ConductorSizingResult {

        val methodKey =
            IecTables.methodToKey(
                input.installationMethod.code
            )

        val loadedConductors =
            loadedConductorCount(input.currentType)

        val temperatureFactor =
            temperatureCorrection(
                input.insulation,
                input.ambientTemp
            )

        val groupingFactor =
            IecTables.groupingFactor(
                input.circuitsInConduit
            ).coerceAtLeast(MIN_POSITIVE)

        val requiredIz =
            requiredIzOverride
                ?: (
                    designCurrent /
                        (
                            temperatureFactor *
                                groupingFactor
                            )
                    )

        val baseAmpacity =
            IecTables.getBaseAmpacity(
                section = section,
                methodKey = methodKey,
                loadedConductors = loadedConductors,
                conductor = input.conductor,
                insulation = input.insulation
            )

        val correctedAmpacity =
            baseAmpacity *
                temperatureFactor *
                groupingFactor

        val voltageDrop =
            calculateVoltageDrop(
                current = designCurrent,
                length = input.lineLength,
                sectionMm2 = section,
                powerFactor = input.powerFactor,
                currentType = input.currentType,
                material = input.conductor,
                voltage = input.voltage
            )

        val voltageDropWithinLimit =
            voltageDrop.first <= input.maxVoltageDrop

        /*
         * IEC-style protection relationship:
         *
         * Ib <= In <= Iz
         *
         * Select the smallest standard protective device satisfying
         * both conditions.
         */
        val protectiveDevice =
            standardBreakers.firstOrNull { breaker ->
                breaker >= designCurrent &&
                    breaker <= correctedAmpacity + 1.0e-6
            }
                ?: 0.0

        val breakerValid =
            protectiveDevice > 0.0 &&
                protectiveDevice >= designCurrent &&
                protectiveDevice <= correctedAmpacity + 1.0e-6

        val shortCircuit =
            calculateShortCircuitCurrent(
                voltage = input.voltage,
                length = input.lineLength,
                sectionMm2 = section,
                material = input.conductor,
                currentType = input.currentType
            )

        val notes =
            mutableListOf<String>()

        notes +=
            "Ib raw = %.2f A"
                .format(rawDesignCurrent)

        notes +=
            "Ib design = %.2f A"
                .format(designCurrent)

        notes +=
            "Required Iz = %.1f A"
                .format(requiredIz)

        notes +=
            "Base Iz = %.1f A"
                .format(baseAmpacity)

        notes +=
            "Corrected Iz = %.1f A"
                .format(correctedAmpacity)

        notes +=
            "Cable = %.1f mm² %s"
                .format(
                    section,
                    input.conductor.name
                )

        notes +=
            "Ca = %.3f | Cg = %.3f"
                .format(
                    temperatureFactor,
                    groupingFactor
                )

        notes +=
            "Installation method = ${input.installationMethod.code}"

        notes +=
            "Voltage drop = %.2f %% (%.2f V)"
                .format(
                    voltageDrop.first,
                    voltageDrop.second
                )

        if (!voltageDropWithinLimit || forceVoltageDropWarning) {
            notes +=
                "WARNING: voltage drop exceeds the configured limit."
        }

        if (breakerValid) {
            notes +=
                "Protective device = %.0f A"
                    .format(protectiveDevice)
        } else {
            notes +=
                "WARNING: no standard protective-device rating satisfies Ib ≤ In ≤ Iz."
        }

        notes +=
            "Estimated end-of-line short-circuit current = %.2f kA"
                .format(shortCircuit.ikKA)

        when (standard) {

            Standard.IEC -> {
                notes +=
                    "Reference: IEC 60364-5-52"
            }

            Standard.EGYPTIAN -> {
                notes +=
                    "Reference: Egyptian Electrical Code / IEC-based design basis"
            }

            Standard.CEI -> {
                notes +=
                    "Reference: CEI 64-8"
            }

            Standard.NEC -> {
                notes +=
                    "Reference selected: NEC / NFPA 70"
                notes +=
                    "WARNING: current ampacity dataset is IEC-based; NEC table compliance requires NEC conductor tables."
            }

            Standard.CEC -> {
                notes +=
                    "Reference selected: Canadian Electrical Code"
                notes +=
                    "WARNING: current ampacity dataset is IEC-based; CEC table compliance requires CEC conductor tables."
            }
        }

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = section,
            selectedSection = section,
            ampacity = correctedAmpacity,
            voltageDropPercent = voltageDrop.first,
            voltageDropVolts = voltageDrop.second,
            protectiveDevice = protectiveDevice,
            shortCircuitCurrentKA = shortCircuit.ikKA,
            breakerWithinCableCapacity = breakerValid,
            voltageDropWithinLimit = voltageDropWithinLimit,
            notes = notes
        )
    }

    private fun validateInput(
        input: ConductorSizingInput
    ) {

        require(input.voltage > MIN_POSITIVE) {
            "Voltage must be greater than zero."
        }

        require(input.load >= 0.0) {
            "Load cannot be negative."
        }

        require(input.lineLength >= 0.0) {
            "Line length cannot be negative."
        }

        require(
            input.powerFactor > 0.0 &&
                input.powerFactor <= 1.0
        ) {
            "Power factor must be between 0 and 1."
        }

        require(input.ambientTemp in -50.0..100.0) {
            "Ambient temperature is outside the supported range."
        }

        require(input.circuitsInConduit > 0) {
            "Number of circuits must be greater than zero."
        }

        require(input.maxVoltageDrop > 0.0) {
            "Maximum voltage drop must be greater than zero."
        }
    }

    private fun loadedConductorCount(
        currentType: CurrentType
    ): Int {

        return when (currentType) {

            CurrentType.DirectCurrent ->
                2

            CurrentType.AlternatingSinglePhase ->
                2

            CurrentType.AlternatingTwoPhase ->
                2

            CurrentType.AlternatingThreePhase ->
                3
        }
    }

    private fun temperatureCorrection(
        insulation: InsulationType,
        ambientTemperature: Double
    ): Double {

        return when (insulation) {

            InsulationType.XLPE,
            InsulationType.EPR ->
                IecTables.ambientCorrectionXlpe(
                    ambientTemperature
                )

            InsulationType.PVC,
            InsulationType.Rubber ->
                IecTables.ambientCorrectionPvc(
                    ambientTemperature
                )
        }.coerceAtLeast(MIN_POSITIVE)
    }

    fun calculateActivePower(
        voltage: Double,
        current: Double,
        pf: Double,
        phases: Int
    ): Double {

        require(voltage >= 0.0)
        require(current >= 0.0)
        require(pf in 0.0..1.0)

        return when (phases) {

            1 ->
                voltage *
                    current *
                    pf

            3 ->
                sqrt(3.0) *
                    voltage *
                    current *
                    pf

            else ->
                voltage *
                    current *
                    pf
        }
    }

    fun calculateApparentPower(
        voltage: Double,
        current: Double,
        phases: Int
    ): Double {

        require(voltage >= 0.0)
        require(current >= 0.0)

        return when (phases) {

            1 ->
                voltage *
                    current

            3 ->
                sqrt(3.0) *
                    voltage *
                    current

            else ->
                voltage *
                    current
        }
    }

    fun calculateReactivePower(
        active: Double,
        apparent: Double
    ): Double {

        require(active >= 0.0)
        require(apparent >= 0.0)

        return sqrt(
            (
                apparent * apparent -
                    active * active
                ).coerceAtLeast(0.0)
        )
    }

    fun calculatePowerFactor(
        active: Double,
        apparent: Double
    ): Double {

        require(active >= 0.0)
        require(apparent >= 0.0)

        return if (apparent > MIN_POSITIVE) {
            (active / apparent)
                .coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    }
}

data class ShortCircuitResult(
    val ikAmps: Double,
    val ikKA: Double,
    val cableImpedance: Double,
    val sourceImpedance: Double,
    val i2t: Double,
    val notes: List<String>
)
