package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.catalog.Manufacturer
import kotlin.math.abs

/**
 * ================================================================
 * PROFESSIONAL CONDUCTOR SIZING ENGINE
 * ================================================================
 *
 * UI
 *  ↓
 * ElectricalCalculations
 *  ↓
 * ConductorSizingCalculator
 *  ↓
 * StandardEngine
 *  ↓
 * EquipmentSelectionCalculator
 *  ↓
 * Cable / Breaker Catalog
 *
 * The engineering calculation and manufacturer catalog selection
 * are intentionally separated.
 * ================================================================
 */
object ConductorSizingCalculator {

    private const val EPSILON = 1.0e-9

    // ============================================================
    // AUTOMATIC SIZING
    // ============================================================

    fun size(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0,
        manufacturer: Manufacturer? = null
    ): ConductorSizingResult {

        validateInput(input)

        require(demandFactor in 0.0..1.0) {
            "Demand factor must be between 0 and 1."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity factor must be between 0 and 1."
        }

        val engine =
            CodeEngineFactory.get(standard)

        val rawDesignCurrent =
            LoadCalculator.designCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        val designCurrent =
            LoadCalculator.applyDemandAndDiversity(
                current = rawDesignCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        val temperatureFactor =
            engine.ambientTemperatureFactor(
                insulation = input.insulation,
                ambientTemperatureC = input.ambientTemp
            ).coerceAtLeast(EPSILON)

        val groupingFactor =
            engine.groupingFactor(
                input.circuitsInConduit
            ).coerceAtLeast(EPSILON)

        val requiredBaseIz =
            designCurrent /
                temperatureFactor /
                groupingFactor

        val sections =
            engine.standardConductorSections()
                .filter { it > EPSILON }
                .sorted()

        val candidate =
            sections.firstOrNull { section ->

                val baseAmpacity =
                    engine.conductorAmpacity(
                        sectionMm2 = section,
                        material = input.conductor,
                        insulation = input.insulation,
                        installationMethod = input.installationMethod,
                        loadedConductors =
                            loadedConductorCount(
                                input.currentType
                            )
                    )

                baseAmpacity != null &&
                    baseAmpacity >= requiredBaseIz
            }

        val selected =
            candidate
                ?: sections.lastOrNull()
                ?: 300.0

        return evaluateSection(
            input = input,
            section = selected,
            designCurrent = designCurrent,
            rawDesignCurrent = rawDesignCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            manufacturer = manufacturer,
            forceNoCodeDataWarning = candidate == null
        )
    }

    // ============================================================
    // MANUAL SECTION EVALUATION
    // ============================================================

    fun evaluateSelectedSection(
        input: ConductorSizingInput,
        selectedSection: Double,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0,
        manufacturer: Manufacturer? = null
    ): ConductorSizingResult {

        validateInput(input)

        require(selectedSection > EPSILON) {
            "Selected conductor section must be greater than zero."
        }

        require(demandFactor in 0.0..1.0) {
            "Demand factor must be between 0 and 1."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity factor must be between 0 and 1."
        }

        val engine =
            CodeEngineFactory.get(standard)

        val rawDesignCurrent =
            LoadCalculator.designCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        val designCurrent =
            LoadCalculator.applyDemandAndDiversity(
                current = rawDesignCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        val temperatureFactor =
            engine.ambientTemperatureFactor(
                insulation = input.insulation,
                ambientTemperatureC = input.ambientTemp
            ).coerceAtLeast(EPSILON)

        val groupingFactor =
            engine.groupingFactor(
                input.circuitsInConduit
            ).coerceAtLeast(EPSILON)

        val requiredBaseIz =
            designCurrent /
                temperatureFactor /
                groupingFactor

        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawDesignCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            manufacturer = manufacturer
        )
    }

    // ============================================================
    // CORE EVALUATION
    // ============================================================

    private fun evaluateSection(
        input: ConductorSizingInput,
        section: Double,
        designCurrent: Double,
        rawDesignCurrent: Double,
        standard: Standard,
        requiredBaseIz: Double,
        manufacturer: Manufacturer? = null,
        forceNoCodeDataWarning: Boolean = false
    ): ConductorSizingResult {

        val engine =
            CodeEngineFactory.get(standard)

        val temperatureFactor =
            engine.ambientTemperatureFactor(
                insulation = input.insulation,
                ambientTemperatureC = input.ambientTemp
            ).coerceAtLeast(EPSILON)

        val groupingFactor =
            engine.groupingFactor(
                input.circuitsInConduit
            ).coerceAtLeast(EPSILON)

        val baseAmpacity =
            engine.conductorAmpacity(
                sectionMm2 = section,
                material = input.conductor,
                insulation = input.insulation,
                installationMethod = input.installationMethod,
                loadedConductors =
                    loadedConductorCount(
                        input.currentType
                    )
            )

        val ampacity =
            baseAmpacity?.let {
                it *
                    temperatureFactor *
                    groupingFactor
            } ?: 0.0

        // --------------------------------------------------------
        // VOLTAGE DROP
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // ENGINEERING BREAKER SELECTION
        // --------------------------------------------------------

        val protectiveDevice =
            BreakerSelectionCalculator.selectRating(
                designCurrentA = designCurrent,
                cableAmpacityA = ampacity,
                standard = standard
            )

        val breakerWithinCapacity =
            protectiveDevice > 0.0 &&
                BreakerSelectionCalculator.satisfiesCoordination(
                    designCurrentA = designCurrent,
                    breakerRatingA = protectiveDevice,
                    cableAmpacityA = ampacity
                )

        // --------------------------------------------------------
        // SHORT CIRCUIT
        // --------------------------------------------------------

        val shortCircuit =
            runCatching {

                ShortCircuitCalculator.calculate(
                    voltage = input.voltage,
                    length = input.lineLength,
                    sectionMm2 = section,
                    material = input.conductor,
                    currentType = input.currentType
                )

            }.getOrNull()

        val shortCircuitKA =
            shortCircuit
                ?.shortCircuitCurrentKA
                ?: 0.0

        // --------------------------------------------------------
        // CATALOG CABLE SELECTION
        // --------------------------------------------------------

        val catalogCableResult =
            EquipmentSelectionCalculator.selectCable(
                sectionMm2 = section,
                manufacturer = manufacturer,
                standard = standard
            )

        val catalogCable =
            catalogCableResult.selected

        // --------------------------------------------------------
        // CATALOG BREAKER SELECTION
        // --------------------------------------------------------

        val catalogBreakerResult =
            if (protectiveDevice > 0.0) {

                EquipmentSelectionCalculator.selectBreaker(
                    ratedCurrentA = protectiveDevice,
                    breakingCapacityKA = shortCircuitKA,
                    manufacturer = manufacturer,
                    standard = standard
                )

            } else {
                null
            }

        val catalogBreaker =
            catalogBreakerResult?.selected

        // --------------------------------------------------------
        // VOLTAGE DROP VALIDATION
        // --------------------------------------------------------

        val voltageDropWithinLimit =
            voltageDrop.first <=
                input.maxVoltageDrop +
                EPSILON

        // --------------------------------------------------------
        // NOTES
        // --------------------------------------------------------

        val notes =
            buildList {

                add(
                    "Code: ${engine.codeName}"
                )

                add(
                    "Code revision: ${engine.codeRevision}"
                )

                add(
                    "Design current = %.2f A"
                        .format(designCurrent)
                )

                add(
                    "Raw design current = %.2f A"
                        .format(rawDesignCurrent)
                )

                add(
                    "Selected section = %.1f mm²"
                        .format(section)
                )

                if (baseAmpacity != null) {

                    add(
                        "Base ampacity = %.2f A"
                            .format(baseAmpacity)
                    )

                    add(
                        "Ambient correction factor = %.3f"
                            .format(temperatureFactor)
                    )

                    add(
                        "Grouping factor = %.3f"
                            .format(groupingFactor)
                    )

                    add(
                        "Corrected ampacity = %.2f A"
                            .format(ampacity)
                    )

                } else {

                    add(
                        "Verified ampacity data is unavailable for this code and cable combination."
                    )
                }

                add(
                    "Required base Iz = %.2f A"
                        .format(requiredBaseIz)
                )

                add(
                    "Voltage drop = %.3f V"
                        .format(voltageDrop.second)
                )

                add(
                    "Voltage drop = %.3f %%"
                        .format(voltageDrop.first)
                )

                if (protectiveDevice > 0.0) {

                    add(
                        "Engineering breaker rating = %.0f A"
                            .format(protectiveDevice)
                    )

                } else {

                    add(
                        "No standard breaker rating satisfies Ib <= In <= Iz."
                    )
                }

                if (catalogCable != null) {

                    add(
                        "Catalog cable = ${catalogCable.model}"
                    )

                    add(
                        "Cable manufacturer = ${catalogCable.manufacturer}"
                    )

                    add(
                        "Cable family = ${catalogCable.family}"
                    )

                } else {

                    add(
                        "No catalog cable was found for the selected section."
                    )
                }

                if (catalogBreaker != null) {

                    add(
                        "Catalog breaker = ${catalogBreaker.model}"
                    )

                    add(
                        "Breaker manufacturer = ${catalogBreaker.manufacturer}"
                    )

                    add(
                        "Breaker family = ${catalogBreaker.family}"
                    )

                } else if (protectiveDevice > 0.0) {

                    add(
                        "No catalog breaker was found for the selected engineering rating."
                    )
                }

                if (shortCircuit != null) {

                    add(
                        "Preliminary short-circuit current = %.3f kA"
                            .format(shortCircuitKA)
                    )
                }

                if (!engine.isFullyImplemented()) {

                    add(
                        engine.implementationStatus()
                    )
                }

                if (forceNoCodeDataWarning) {

                    add(
                        "Automatic cable sizing could not verify a complete code dataset."
                    )
                }

                if (!catalogCableResult.valid) {

                    add(
                        "Catalog cable selection requires verified manufacturer data."
                    )
                }

                if (catalogBreakerResult != null &&
                    !catalogBreakerResult.valid
                ) {

                    add(
                        "Catalog breaker selection requires verified manufacturer configuration and Icu/Ics data."
                    )
                }
            }

        return ConductorSizingResult(

            designCurrent =
                designCurrent,

            recommendedSection =
                section,

            selectedSection =
                section,

            ampacity =
                ampacity,

            voltageDropPercent =
                voltageDrop.first,

            voltageDropVolts =
                voltageDrop.second,

            protectiveDevice =
                protectiveDevice,

            shortCircuitCurrentKA =
                shortCircuitKA,

            breakerWithinCableCapacity =
                breakerWithinCapacity,

            voltageDropWithinLimit =
                voltageDropWithinLimit,

            notes =
                notes,

            catalogCable =
                catalogCable,

            catalogBreaker =
                catalogBreaker
        )
    }

    // ============================================================
    // CONDUCTOR COUNT
    // ============================================================

    private fun loadedConductorCount(
        currentType: CurrentType
    ): Int =
        when (currentType) {

            CurrentType.DirectCurrent ->
                2

            CurrentType.AlternatingSinglePhase ->
                2

            CurrentType.AlternatingTwoPhase ->
                2

            CurrentType.AlternatingThreePhase ->
                3
        }

    // ============================================================
    // INPUT VALIDATION
    // ============================================================

    private fun validateInput(
        input: ConductorSizingInput
    ) {

        require(input.voltage > EPSILON) {
            "Voltage must be greater than zero."
        }

        require(input.load >= 0.0) {
            "Load cannot be negative."
        }

        require(
            input.powerFactor > 0.0 &&
                input.powerFactor <= 1.0
        ) {
            "Power factor must be greater than 0 and not greater than 1."
        }

        require(input.lineLength >= 0.0) {
            "Line length cannot be negative."
        }

        require(input.ambientTemp > -50.0) {
            "Ambient temperature is invalid."
        }

        require(input.circuitsInConduit >= 1) {
            "Number of circuits must be at least 1."
        }

        require(input.maxVoltageDrop > 0.0) {
            "Maximum voltage drop must be greater than zero."
        }
    }
}
