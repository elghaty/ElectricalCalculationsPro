package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.IecTables
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standardSections
import kotlin.math.abs

/**
 * Conductor sizing orchestration layer.
 *
 * NOTE:
 * This class currently delegates ampacity tables to IecTables because
 * those tables already exist in the project.
 *
 * The next standards phase will replace that dependency with:
 *
 * EgyptianCodeTables
 * IecCodeTables
 * NecCodeTables
 *
 * Therefore NEC will never silently reuse IEC data in the final system.
 */
object ConductorSizingCalculator {

    private const val EPSILON = 1.0e-9

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

    fun size(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        validateInput(input)

        val rawCurrent =
            LoadCalculator.designCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        val designCurrent =
            LoadCalculator.applyDemandAndDiversity(
                current = rawCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        val temperatureFactor =
            temperatureCorrection(
                input.insulation,
                input.ambientTemp
            )

        val groupingFactor =
            IecTables
                .groupingFactor(
                    input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

        val requiredBaseIz =
            designCurrent /
                (
                    temperatureFactor *
                        groupingFactor
                    )

        val loadedConductors =
            loadedConductorCount(
                input.currentType
            )

        val methodKey =
            IecTables.methodToKey(
                input.installationMethod.code
            )

        for (section in standardSections) {

            val baseAmpacity =
                IecTables.getBaseAmpacity(
                    section = section,
                    method = methodKey,
                    loadedConductors =
                        loadedConductors,
                    material = input.conductor,
                    insulation = input.insulation
                )

            val correctedAmpacity =
                baseAmpacity *
                    temperatureFactor *
                    groupingFactor

            if (
                correctedAmpacity +
                    EPSILON <
                    designCurrent
            ) {
                continue
            }

            val voltageDrop =
                VoltageDropCalculator.calculate(
                    current = designCurrent,
                    length = input.lineLength,
                    sectionMm2 = section,
                    powerFactor =
                        input.powerFactor,
                    currentType =
                        input.currentType,
                    material =
                        input.conductor,
                    voltage =
                        input.voltage
                )

            if (
                voltageDrop.first <=
                    input.maxVoltageDrop
            ) {

                return evaluateSection(
                    input = input,
                    section = section,
                    designCurrent =
                        designCurrent,
                    rawDesignCurrent =
                        rawCurrent,
                    standard = standard,
                    requiredBaseIz =
                        requiredBaseIz
                )
            }
        }

        return evaluateSection(
            input = input,
            section = standardSections.last(),
            designCurrent = designCurrent,
            rawDesignCurrent = rawCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            forceVoltageDropWarning = true
        )
    }

    fun evaluateSelectedSection(
        input: ConductorSizingInput,
        selectedSection: Double,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        validateInput(input)

        require(
            standardSections.any {
                abs(it - selectedSection) <
                    EPSILON
            }
        )

        val rawCurrent =
            LoadCalculator.designCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        val designCurrent =
            LoadCalculator.applyDemandAndDiversity(
                current = rawCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        val temperatureFactor =
            temperatureCorrection(
                input.insulation,
                input.ambientTemp
            )

        val groupingFactor =
            IecTables
                .groupingFactor(
                    input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

        val requiredBaseIz =
            designCurrent /
                (
                    temperatureFactor *
                        groupingFactor
                    )

        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz
        )
    }

    private fun evaluateSection(
        input: ConductorSizingInput,
        section: Double,
        designCurrent: Double,
        rawDesignCurrent: Double,
        standard: Standard,
        requiredBaseIz: Double,
        forceVoltageDropWarning: Boolean = false
    ): ConductorSizingResult {

        val loadedConductors =
            loadedConductorCount(
                input.currentType
            )

        val methodKey =
            IecTables.methodToKey(
                input.installationMethod.code
            )

        val temperatureFactor =
            temperatureCorrection(
                input.insulation,
                input.ambientTemp
            )

        val groupingFactor =
            IecTables
                .groupingFactor(
                    input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

        val baseAmpacity =
            IecTables.getBaseAmpacity(
                section = section,
                method = methodKey,
                loadedConductors =
                    loadedConductors,
                material =
                    input.conductor,
                insulation =
                    input.insulation
            )

        val correctedAmpacity =
            baseAmpacity *
                temperatureFactor *
                groupingFactor

        val voltageDrop =
            VoltageDropCalculator.calculate(
                current = designCurrent,
                length = input.lineLength,
                sectionMm2 = section,
                powerFactor =
                    input.powerFactor,
                currentType =
                    input.currentType,
                material =
                    input.conductor,
                voltage =
                    input.voltage
            )

        val voltageDropOk =
            voltageDrop.first <=
                input.maxVoltageDrop

        val breaker =
            BreakerSelectionCalculator
                .selectRating(
                    designCurrentA =
                        designCurrent,
                    cableAmpacityA =
                        correctedAmpacity
                )
                ?: 0.0

        val breakerOk =
            BreakerSelectionCalculator
                .satisfiesCoordination(
                    designCurrentA =
                        designCurrent,
                    breakerRatingA =
                        breaker,
                    cableAmpacityA =
                        correctedAmpacity
                )

        val shortCircuit =
            ShortCircuitCalculator.calculate(
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
            "Required base Iz = %.1f A"
                .format(requiredBaseIz)

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
            "Installation = " +
                input.installationMethod.code

        notes +=
            "Voltage drop = %.2f %% (%.2f V)"
                .format(
                    voltageDrop.first,
                    voltageDrop.second
                )

        if (
            !voltageDropOk ||
            forceVoltageDropWarning
        ) {

            notes +=
                "WARNING: voltage drop exceeds the configured limit."
        }

        if (
            correctedAmpacity <
            designCurrent
        ) {

            notes +=
                "WARNING: cable ampacity is below design current."
        }

        if (breakerOk) {

            notes +=
                "Protective device nominal rating = %.0f A"
                    .format(breaker)

        } else {

            notes +=
                "WARNING: no standard breaker rating satisfies Ib ≤ In ≤ Iz."
        }

        notes +=
            "Estimated end fault current = %.2f kA"
                .format(shortCircuit.ikKA)

        notes +=
            when (standard) {

                Standard.IEC ->
                    "Reference family: IEC 60364"

                Standard.EGYPTIAN ->
                    "Reference: Egyptian Electrical Code / applicable Egyptian specifications"

                Standard.NEC ->
                    "STATUS: NEC-specific ampacity tables must be applied before declaring NEC compliance."

                Standard.CEI ->
                    "Reference: CEI 64-8"

                Standard.CEC ->
                    "Reference: CEC"
            }

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = section,
            selectedSection = section,
            ampacity = correctedAmpacity,
            voltageDropPercent =
                voltageDrop.first,
            voltageDropVolts =
                voltageDrop.second,
            protectiveDevice = breaker,
            shortCircuitCurrentKA =
                shortCircuit.ikKA,
            breakerWithinCableCapacity =
                breakerOk,
            voltageDropWithinLimit =
                voltageDropOk,
            notes = notes
        )
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
        }.coerceAtLeast(EPSILON)
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

    private fun validateInput(
        input: ConductorSizingInput
    ) {

        require(input.voltage > EPSILON)
        require(input.load >= 0.0)
        require(input.lineLength >= 0.0)

        require(
            input.powerFactor >
                0.0 &&
                input.powerFactor <=
                1.0
        )

        require(
            input.circuitsInConduit >
                0
        )

        require(
            input.maxVoltageDrop >
                0.0
        )

        require(
            input.ambientTemp in
                -50.0..100.0
        )
    }
}
