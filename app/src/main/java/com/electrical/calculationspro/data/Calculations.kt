package com.electrical.calculationspro.data

import kotlin.math.sqrt

/**
 * Single calculation engine for the application.
 *
 * All electrical calculations used by the UI must pass through this object.
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

        val pf = powerFactor.coerceIn(MIN_POSITIVE, 1.0)

        return when (currentType) {

            CurrentType.DirectCurrent ->
                loadWatts / voltage

            CurrentType.AlternatingSinglePhase ->
                loadWatts / (voltage * pf)

            CurrentType.AlternatingTwoPhase ->
                loadWatts / (voltage * pf * sqrt(2.0))

            CurrentType.AlternatingThreePhase ->
                loadWatts / (voltage * pf * sqrt(3.0))
        }
    }

    fun applyDemandAndDiversity(
        ib: Double,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): Double {

        require(ib >= 0.0) {
            "Design current cannot be negative."
        }

        val demand =
            demandFactor.coerceIn(0.0, 1.0)

        val diversity =
            diversityFactor.coerceIn(0.0, 1.0)

        return ib * demand * diversity
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

        require(current >= 0.0)
        require(length >= 0.0)
        require(sectionMm2 > MIN_POSITIVE)
        require(voltage > MIN_POSITIVE)

        val resistivity =
            when (material) {
                ConductorMaterial.Copper -> 0.0225
                ConductorMaterial.Aluminum -> 0.036
            }

        val resistance =
            resistivity / sectionMm2

        val reactance =
            0.08 / 1000.0

        val cosPhi =
            powerFactor.coerceIn(0.0, 1.0)

        val sinPhi =
            sqrt(
                (1.0 - cosPhi * cosPhi)
                    .coerceAtLeast(0.0)
            )

        val systemFactor =
            when (currentType) {

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
                (resistance * cosPhi + reactance * sinPhi)

        val dropPercent =
            (dropVolts / voltage) * 100.0

        return dropPercent to dropVolts
    }

    fun calculateShortCircuitCurrent(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 50.0
    ): ShortCircuitResult {

        require(voltage > MIN_POSITIVE)
        require(length >= 0.0)
        require(sectionMm2 > MIN_POSITIVE)

        val resistivity =
            when (material) {
                ConductorMaterial.Copper -> 0.018
                ConductorMaterial.Aluminum -> 0.029
            }

        val cableResistance =
            (resistivity * length * 2.0) /
                sectionMm2

        val cableReactance =
            (0.08 * length / 1000.0) * 2.0

        val cableImpedance =
            sqrt(
                cableResistance * cableResistance +
                    cableReactance * cableReactance
            )

        val sourceImpedance =
            if (sourceIkKA > MIN_POSITIVE) {
                (voltage / sqrt(3.0)) /
                    (sourceIkKA * 1000.0)
            } else {
                0.0
            }

        val totalImpedance =
            (sourceImpedance + cableImpedance)
                .coerceAtLeast(MIN_POSITIVE)

        val faultCurrent =
            when (currentType) {

                CurrentType.AlternatingThreePhase ->
                    (voltage / sqrt(3.0)) /
                        totalImpedance

                else ->
                    (voltage / 2.0) /
                        totalImpedance
            }

        val faultCurrentKA =
            faultCurrent / 1000.0

        val i2t =
            faultCurrent *
                faultCurrent *
                0.1

        return ShortCircuitResult(
            ikAmps = faultCurrent,
            ikKA = faultCurrentKA,
            cableImpedance = cableImpedance,
            sourceImpedance = sourceImpedance,
            i2t = i2t,
            notes = listOf(
                "Ik ≈ ${"%.2f".format(faultCurrentKA)} kA",
                "Z cable ≈ ${"%.4f".format(cableImpedance)} Ω",
                "Z source ≈ ${"%.4f".format(sourceImpedance)} Ω",
                "I²t (0.1s) ≈ ${"%.0f".format(i2t)}"
            )
        )
    }

    fun sizeConductor(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        require(input.voltage > MIN_POSITIVE)
        require(input.load >= 0.0)
        require(input.lineLength >= 0.0)
        require(input.powerFactor > 0.0)
        require(input.circuitsInConduit > 0)
        require(input.maxVoltageDrop > 0.0)

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

        val methodKey =
            IecTables.methodToKey(
                input.installationMethod.code
            )

        val loadedConductors =
            when (input.currentType) {

                CurrentType.DirectCurrent ->
                    2

                CurrentType.AlternatingSinglePhase ->
                    2

                CurrentType.AlternatingTwoPhase ->
                    2

                CurrentType.AlternatingThreePhase ->
                    3
            }

        val temperatureFactor =
            when (input.insulation) {

                InsulationType.XLPE,
                InsulationType.EPR ->
                    IecTables.ambientCorrectionXlpe(
                        input.ambientTemp
                    )

                InsulationType.PVC,
                InsulationType.Rubber ->
                    IecTables.ambientCorrectionPvc(
                        input.ambientTemp
                    )
            }

        val groupingFactor =
            IecTables.groupingFactor(
                input.circuitsInConduit
            )

        val combinedFactor =
            (temperatureFactor * groupingFactor)
                .coerceAtLeast(MIN_POSITIVE)

        val requiredIz =
            designCurrent / combinedFactor

        var selectedSection =
            standardSections.last()

        var selectedAmpacity =
            0.0

        var voltageDropPercent =
            0.0

        var voltageDropVolts =
            0.0

        var foundSection =
            false

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

            if (correctedAmpacity < requiredIz) {
                continue
            }

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

            if (voltageDrop.first <= input.maxVoltageDrop) {

                selectedSection =
                    section

                selectedAmpacity =
                    correctedAmpacity

                voltageDropPercent =
                    voltageDrop.first

                voltageDropVolts =
                    voltageDrop.second

                foundSection =
                    true

                break
            }
        }

        if (!foundSection) {

            val ampacityCandidate =
                standardSections.lastOrNull { section ->

                    val baseAmpacity =
                        IecTables.getBaseAmpacity(
                            section = section,
                            methodKey = methodKey,
                            loadedConductors = loadedConductors,
                            conductor = input.conductor,
                            insulation = input.insulation
                        )

                    baseAmpacity *
                        temperatureFactor *
                        groupingFactor >= requiredIz
                }

            if (ampacityCandidate != null) {

                selectedSection =
                    ampacityCandidate

                val baseAmpacity =
                    IecTables.getBaseAmpacity(
                        section = selectedSection,
                        methodKey = methodKey,
                        loadedConductors = loadedConductors,
                        conductor = input.conductor,
                        insulation = input.insulation
                    )

                selectedAmpacity =
                    baseAmpacity *
                        temperatureFactor *
                        groupingFactor

                val voltageDrop =
                    calculateVoltageDrop(
                        current = designCurrent,
                        length = input.lineLength,
                        sectionMm2 = selectedSection,
                        powerFactor = input.powerFactor,
                        currentType = input.currentType,
                        material = input.conductor,
                        voltage = input.voltage
                    )

                voltageDropPercent =
                    voltageDrop.first

                voltageDropVolts =
                    voltageDrop.second
            }
        }

        val protectiveDevice =
            standardBreakers.firstOrNull {
                it >= designCurrent
            } ?: (designCurrent * 1.25)

        val shortCircuit =
            calculateShortCircuitCurrent(
                voltage = input.voltage,
                length = input.lineLength,
                sectionMm2 = selectedSection,
                material = input.conductor,
                currentType = input.currentType
            )

        val notes =
            mutableListOf<String>()

        notes +=
            "Ib raw = ${"%.2f".format(rawDesignCurrent)} A"

        notes +=
            "Demand = ${"%.2f".format(demandFactor)} | Diversity = ${"%.2f".format(diversityFactor)}"

        notes +=
            "Ib corrected = ${"%.2f".format(designCurrent)} A"

        notes +=
            "Required Iz ≈ ${"%.1f".format(requiredIz)} A"

        notes +=
            "Selected S = $selectedSection mm²"

        notes +=
            "Iz after factors ≈ ${"%.1f".format(selectedAmpacity)} A"

        notes +=
            "ΔU = ${"%.2f".format(voltageDropPercent)} % (${ "%.2f".format(voltageDropVolts) } V)"

        notes +=
            "Method: ${input.installationMethod.code} → $methodKey"

        notes +=
            "Ca = ${"%.2f".format(temperatureFactor)} | Cg = ${"%.2f".format(groupingFactor)}"

        notes +=
            "Ik ≈ ${"%.2f".format(shortCircuit.ikKA)} kA"

        when (standard) {

            Standard.IEC -> {
                notes +=
                    "IEC 60364-5-52"
            }

            Standard.EGYPTIAN -> {
                notes +=
                    "الكود المصري المبني على IEC 60364"
            }

            Standard.CEI -> {
                notes +=
                    "CEI 64-8"
            }

            Standard.NEC -> {
                notes +=
                    "NEC / NFPA 70"
            }

            Standard.CEC -> {
                notes +=
                    "Canadian Electrical Code"
            }
        }

        if (voltageDropPercent > input.maxVoltageDrop) {

            notes +=
                "⚠ Voltage drop exceeds the configured limit."
        }

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = selectedSection,
            selectedSection = selectedSection,
            ampacity = selectedAmpacity,
            voltageDropPercent = voltageDropPercent,
            voltageDropVolts = voltageDropVolts,
            protectiveDevice = protectiveDevice,
            notes = notes
        )
    }

    fun calculateActivePower(
        voltage: Double,
        current: Double,
        pf: Double,
        phases: Int
    ): Double {

        val powerFactor =
            pf.coerceIn(0.0, 1.0)

        return when (phases) {

            1 ->
                voltage * current * powerFactor

            3 ->
                sqrt(3.0) *
                    voltage *
                    current *
                    powerFactor

            else ->
                voltage *
                    current *
                    powerFactor
        }
    }

    fun calculateApparentPower(
        voltage: Double,
        current: Double,
        phases: Int
    ): Double {

        return when (phases) {

            1 ->
                voltage * current

            3 ->
                sqrt(3.0) *
                    voltage *
                    current

            else ->
                voltage * current
        }
    }

    fun calculateReactivePower(
        active: Double,
        apparent: Double
    ): Double {

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
