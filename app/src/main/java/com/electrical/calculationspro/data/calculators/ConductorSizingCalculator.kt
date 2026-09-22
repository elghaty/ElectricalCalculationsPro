package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standardSections
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import kotlin.math.abs

/**
 * PROFESSIONAL ENGINEERING CORE
 *
 * Conductor sizing orchestration layer.
 *
 * Architecture:
 *
 * UI
 *   ↓
 * ElectricalCalculations
 *   ↓
 * ConductorSizingCalculator
 *   ↓
 * CodeEngineFactory
 *   ↓
 * Selected StandardEngine
 *
 * Supported standards:
 * - Egyptian
 * - IEC
 * - NEC
 * - CEI
 * - CEC
 *
 * IMPORTANT:
 * This calculator never accesses IecTables directly.
 *
 * All code-dependent data must come from the selected
 * StandardEngine.
 *
 * Therefore:
 *
 * NEC → NecEngine
 * IEC → IecEngine
 * Egyptian → EgyptianCodeEngine
 *
 * This prevents NEC calculations from silently using IEC tables.
 */
object ConductorSizingCalculator {

    private const val EPSILON = 1.0e-9

    /**
     * Main conductor sizing calculation.
     */
    fun size(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        validateInput(input)

        val engine = CodeEngineFactory.get(standard)

        /*
         * ---------------------------------------------------------
         * 1. RAW DESIGN CURRENT
         * ---------------------------------------------------------
         */

        val rawCurrent =
            LoadCalculator.designCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        /*
         * ---------------------------------------------------------
         * 2. DEMAND + DIVERSITY
         * ---------------------------------------------------------
         */

        val designCurrent =
            LoadCalculator.applyDemandAndDiversity(
                current = rawCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        /*
         * ---------------------------------------------------------
         * 3. CODE-SPECIFIC CORRECTION FACTORS
         * ---------------------------------------------------------
         */

        val temperatureFactor =
            engine
                .ambientTemperatureFactor(
                    insulation = input.insulation,
                    ambientTemperatureC = input.ambientTemp
                )
                .coerceAtLeast(EPSILON)

        val groupingFactor =
            engine
                .groupingFactor(
                    numberOfCircuits = input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

        /*
         * Required base ampacity before correction factors.
         *
         * Iz(base) >= Ib / (Ca × Cg)
         */
        val requiredBaseIz =
            designCurrent /
                (temperatureFactor * groupingFactor)

        /*
         * ---------------------------------------------------------
         * 4. CURRENT-CARRYING CONDUCTORS
         * ---------------------------------------------------------
         */

        val loadedConductors =
            loadedConductorCount(
                input.currentType
            )

        /*
         * ---------------------------------------------------------
         * 5. CODE-SPECIFIC STANDARD SECTIONS
         * ---------------------------------------------------------
         *
         * NEVER use the global IEC list as the primary source.
         */

        val sections =
            engine
                .standardConductorSections()
                .ifEmpty {
                    standardSections
                }

        var lastSection =
            sections.lastOrNull()
                ?: standardSections.last()

        /*
         * ---------------------------------------------------------
         * 6. SELECT FIRST VALID SECTION
         * ---------------------------------------------------------
         */

        for (section in sections) {

            lastSection = section

            val baseAmpacity =
                engine.conductorAmpacity(
                    sectionMm2 = section,
                    material = input.conductor,
                    insulation = input.insulation,
                    installationMethod = input.installationMethod,
                    loadedConductors = loadedConductors
                )

            /*
             * null means the selected code does not currently
             * contain verified ampacity data for this combination.
             *
             * DO NOT substitute IEC data.
             */
            if (baseAmpacity == null) {
                continue
            }

            val correctedAmpacity =
                baseAmpacity *
                    temperatureFactor *
                    groupingFactor

            /*
             * Ampacity requirement.
             */
            if (
                correctedAmpacity + EPSILON <
                designCurrent
            ) {
                continue
            }

            /*
             * Voltage-drop requirement.
             */
            val voltageDrop =
                VoltageDropCalculator.calculate(
                    current = designCurrent,
                    length = input.lineLength,
                    sectionMm2 = section,
                    powerFactor = input.powerFactor,
                    currentType = input.currentType,
                    material = input.conductor,
                    voltage = input.voltage
                )

            if (
                voltageDrop.first <=
                input.maxVoltageDrop
            ) {

                return evaluateSection(
                    input = input,
                    section = section,
                    designCurrent = designCurrent,
                    rawDesignCurrent = rawCurrent,
                    standard = standard,
                    requiredBaseIz = requiredBaseIz
                )
            }
        }

        /*
         * ---------------------------------------------------------
         * 7. NO FULLY VALID SECTION FOUND
         * ---------------------------------------------------------
         */

        return evaluateSection(
            input = input,
            section = lastSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            forceVoltageDropWarning = true,
            forceNoCodeDataWarning = true
        )
    }

    /**
     * Evaluate a user-selected conductor section.
     */
    fun evaluateSelectedSection(
        input: ConductorSizingInput,
        selectedSection: Double,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        validateInput(input)

        val engine =
            CodeEngineFactory.get(standard)

        val sections =
            engine.standardConductorSections()

        require(
            sections.any {
                abs(it - selectedSection) < EPSILON
            }
        ) {
            "Selected conductor section is not available in the selected standard."
        }

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
            engine
                .ambientTemperatureFactor(
                    insulation = input.insulation,
                    ambientTemperatureC = input.ambientTemp
                )
                .coerceAtLeast(EPSILON)

        val groupingFactor =
            engine
                .groupingFactor(
                    numberOfCircuits = input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

        val requiredBaseIz =
            designCurrent /
                (temperatureFactor * groupingFactor)

        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            forceNoCodeDataWarning = true
        )
    }

    /**
     * Evaluate one conductor section.
     */
    private fun evaluateSection(
        input: ConductorSizingInput,
        section: Double,
        designCurrent: Double,
        rawDesignCurrent: Double,
        standard: Standard,
        requiredBaseIz: Double,
        forceVoltageDropWarning: Boolean = false,
        forceNoCodeDataWarning: Boolean = false
    ): ConductorSizingResult {

        val engine =
            CodeEngineFactory.get(standard)

        val loadedConductors =
            loadedConductorCount(
                input.currentType
            )

        /*
         * ---------------------------------------------------------
         * CODE CORRECTION FACTORS
         * ---------------------------------------------------------
         */

        val temperatureFactor =
            engine
                .ambientTemperatureFactor(
                    insulation = input.insulation,
                    ambientTemperatureC = input.ambientTemp
                )
                .coerceAtLeast(EPSILON)

        val groupingFactor =
            engine
                .groupingFactor(
                    numberOfCircuits = input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

        /*
         * ---------------------------------------------------------
         * CODE-SPECIFIC AMPACITY
         * ---------------------------------------------------------
         */

        val baseAmpacity =
            engine.conductorAmpacity(
                sectionMm2 = section,
                material = input.conductor,
                insulation = input.insulation,
                installationMethod = input.installationMethod,
                loadedConductors = loadedConductors
            )

        val correctedAmpacity =
            baseAmpacity?.let {
                it *
                    temperatureFactor *
                    groupingFactor
            } ?: 0.0

        /*
         * ---------------------------------------------------------
         * VOLTAGE DROP
         * ---------------------------------------------------------
         */

        val voltageDrop =
            VoltageDropCalculator.calculate(
                current = designCurrent,
                length = input.lineLength,
                sectionMm2 = section,
                powerFactor = input.powerFactor,
                currentType = input.currentType,
                material = input.conductor,
                voltage = input.voltage
            )

        val voltageDropOk =
            voltageDrop.first <=
                input.maxVoltageDrop

        /*
         * ---------------------------------------------------------
         * BREAKER SELECTION
         * ---------------------------------------------------------
         */

        val breaker =
            if (baseAmpacity != null) {

                BreakerSelectionCalculator.selectRating(
                    designCurrentA = designCurrent,
                    cableAmpacityA = correctedAmpacity
                )

            } else {
                0.0
            }

        val breakerOk =
            baseAmpacity != null &&
                breaker > 0.0 &&
                BreakerSelectionCalculator.satisfiesCoordination(
                    designCurrentA = designCurrent,
                    breakerRatingA = breaker,
                    cableAmpacityA = correctedAmpacity
                )

        /*
         * ---------------------------------------------------------
         * SHORT CIRCUIT
         * ---------------------------------------------------------
         *
         * This is the existing cable-end calculation.
         * Full IEC 60909 / NEC fault methodology is handled
         * separately by the corresponding standard layers.
         */

        val shortCircuit =
            ShortCircuitCalculator.calculate(
                voltage = input.voltage,
                length = input.lineLength,
                sectionMm2 = section,
                material = input.conductor,
                currentType = input.currentType
            )

        /*
         * ---------------------------------------------------------
         * ENGINEERING NOTES
         * ---------------------------------------------------------
         */

        val notes =
            mutableListOf<String>()

        notes +=
            "Selected code: " +
                engine.codeName

        notes +=
            "Code revision: " +
                engine.codeRevision

        notes +=
            "Implementation status: " +
                engine.implementationStatus()

        notes +=
            "Ib raw = %.2f A"
                .format(rawDesignCurrent)

        notes +=
            "Ib design = %.2f A"
                .format(designCurrent)

        notes +=
            "Required base Iz = %.1f A"
                .format(requiredBaseIz)

        /*
         * ---------------------------------------------------------
         * AMPACITY RESULT
         * ---------------------------------------------------------
         */

        if (baseAmpacity != null) {

            notes +=
                "Base Iz = %.1f A"
                    .format(baseAmpacity)

            notes +=
                "Corrected Iz = %.1f A"
                    .format(correctedAmpacity)

        } else {

            notes +=
                "CODE DATA UNAVAILABLE: verified conductor ampacity data is not available for this section under the selected standard."

            notes +=
                "No IEC table has been substituted."
        }

        /*
         * ---------------------------------------------------------
         * CONDUCTOR
         * ---------------------------------------------------------
         */

        notes +=
            "Conductor = %.1f mm² %s"
                .format(
                    section,
                    input.conductor.name
                )

        /*
         * ---------------------------------------------------------
         * CORRECTION FACTORS
         * ---------------------------------------------------------
         */

        notes +=
            "Temperature correction factor Ca = %.3f"
                .format(temperatureFactor)

        notes +=
            "Grouping correction factor Cg = %.3f"
                .format(groupingFactor)

        notes +=
            "Installation method = " +
                input.installationMethod.code

        /*
         * ---------------------------------------------------------
         * VOLTAGE DROP
         * ---------------------------------------------------------
         */

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

        /*
         * ---------------------------------------------------------
         * AMPACITY CHECK
         * ---------------------------------------------------------
         */

        if (
            baseAmpacity != null &&
            correctedAmpacity <
            designCurrent
        ) {

            notes +=
                "WARNING: corrected conductor ampacity is below design current."
        }

        /*
         * ---------------------------------------------------------
         * PROTECTION
         * ---------------------------------------------------------
         */

        if (breakerOk) {

            notes +=
                "Selected protective device rating = %.0f A"
                    .format(breaker)

        } else {

            notes +=
                "WARNING: no standard breaker rating satisfies Ib ≤ In ≤ Iz."
        }

        /*
         * ---------------------------------------------------------
         * CODE IMPLEMENTATION WARNING
         * ---------------------------------------------------------
         */

        if (
            forceNoCodeDataWarning ||
            baseAmpacity == null
        ) {

            notes +=
                "WARNING: this result must not be presented as fully code-compliant because the selected code dataset is incomplete for this calculation."
        }

        if (
            !engine.isFullyImplemented()
        ) {

            notes +=
                "WARNING: selected standard engine is not compliance-complete."
        }

        /*
         * ---------------------------------------------------------
         * NEC-SPECIFIC SAFETY
         * ---------------------------------------------------------
         */

        if (
            standard ==
            Standard.NEC
        ) {

            notes +=
                "NEC Article 310 conductor ampacity data must be used for NEC compliance."

            notes +=
                "IEC conductor ampacity tables are not used as a substitute for NEC."
        }

        /*
         * ---------------------------------------------------------
         * SHORT CIRCUIT NOTE
         * ---------------------------------------------------------
         */

        notes +=
            "Estimated cable-end short-circuit current = %.2f kA"
                .format(shortCircuit.ikKA)

        /*
         * ---------------------------------------------------------
         * FINAL RESULT
         * ---------------------------------------------------------
         */

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = section,
            selectedSection = section,
            ampacity = correctedAmpacity,
            voltageDropPercent = voltageDrop.first,
            voltageDropVolts = voltageDrop.second,
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

    /**
     * Number of current-carrying conductors.
     */
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

    /**
     * Input validation.
     */
    private fun validateInput(
        input: ConductorSizingInput
    ) {

        require(
            input.voltage >
                EPSILON
        ) {
            "Voltage must be greater than zero."
        }

        require(
            input.load >= 0.0
        ) {
            "Load cannot be negative."
        }

        require(
            input.lineLength >= 0.0
        ) {
            "Line length cannot be negative."
        }

        require(
            input.powerFactor > 0.0 &&
                input.powerFactor <= 1.0
        ) {
            "Power factor must be greater than 0 and not greater than 1."
        }

        require(
            input.circuitsInConduit > 0
        ) {
            "Number of circuits must be greater than zero."
        }

        require(
            input.maxVoltageDrop > 0.0
        ) {
            "Maximum voltage drop must be greater than zero."
        }

        require(
            input.ambientTemp in -50.0..100.0
        ) {
            "Ambient temperature is outside the supported input range."
        }
    }
}
