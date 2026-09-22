package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.catalog.Manufacturer
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import kotlin.math.abs

/**
 * Professional conductor sizing engine.
 *
 * Architecture:
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
 * Manufacturer Catalog
 *
 * No UI logic is contained here.
 */
object ConductorSizingCalculator {

    private const val EPSILON = 1.0e-9

    fun size(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0,
        manufacturer: Manufacturer? = null
    ): ConductorSizingResult {

        validateInput(input)

        require(
            demandFactor in 0.0..1.0
        ) {
            "Demand factor must be between 0 and 1."
        }

        require(
            diversityFactor in 0.0..1.0
        ) {
            "Diversity factor must be between 0 and 1."
        }

        val engine =
            CodeEngineFactory.get(standard)

        val rawDesignCurrent =
            LoadCalculator.designCurrent(
                loadWatts =
                    input.load,
                voltage =
                    input.voltage,
                powerFactor =
                    input.powerFactor,
                currentType =
                    input.currentType
            )

        val designCurrent =
            LoadCalculator.applyDemandAndDiversity(
                current =
                    rawDesignCurrent,
                demandFactor =
                    demandFactor,
                diversityFactor =
                    diversityFactor
            )

        val temperatureFactor =
            engine.ambientTemperatureFactor(
                insulation =
                    input.insulation,
                ambientTemperatureC =
                    input.ambientTemp
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
                .filter {
                    it > EPSILON
                }
                .sorted()

        val candidate =
            sections.firstOrNull { section ->

                val baseAmpacity =
                    engine.conductorAmpacity(
                        sectionMm2 =
                            section,
                        material =
                            input.conductor,
                        insulation =
                            input.insulation,
                        installationMethod =
                            input.installationMethod,
                        loadedConductors =
                            loadedConductorCount(
                                input.currentType
                            )
                    )

                baseAmpacity != null &&
                    baseAmpacity >=
                        requiredBaseIz
            }

        val selected =
            candidate
                ?: sections.lastOrNull()
                ?: 0.0

        if (selected <= EPSILON) {

            throw IllegalStateException(
                "No standard conductor section is available for the selected engineering code."
            )
        }

        return evaluateSection(
            input =
                input,
            section =
                selected,
            designCurrent =
                designCurrent,
            rawDesignCurrent =
                rawDesignCurrent,
            standard =
                standard,
            requiredBaseIz =
                requiredBaseIz,
            manufacturer =
                manufacturer,
            forceNoCodeDataWarning =
                candidate == null
        )
    }

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

        require(
            demandFactor in 0.0..1.0
        ) {
            "Demand factor must be between 0 and 1."
        }

        require(
            diversityFactor in 0.0..1.0
        ) {
            "Diversity factor must be between 0 and 1."
        }

        val engine =
            CodeEngineFactory.get(standard)

        val rawDesignCurrent =
            LoadCalculator.designCurrent(
                loadWatts =
                    input.load,
                voltage =
                    input.voltage,
                powerFactor =
                    input.powerFactor,
                currentType =
                    input.currentType
            )

        val designCurrent =
            LoadCalculator.applyDemandAndDiversity(
                current =
                    rawDesignCurrent,
                demandFactor =
                    demandFactor,
                diversityFactor =
                    diversityFactor
            )

        val temperatureFactor =
            engine.ambientTemperatureFactor(
                insulation =
                    input.insulation,
                ambientTemperatureC =
                    input.ambientTemp
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
            input =
                input,
            section =
                selectedSection,
            designCurrent =
                designCurrent,
            rawDesignCurrent =
                rawDesignCurrent,
            standard =
                standard,
            requiredBaseIz =
                requiredBaseIz,
            manufacturer =
                manufacturer
        )
    }

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
                insulation =
                    input.insulation,
                ambientTemperatureC =
                    input.ambientTemp
            ).coerceAtLeast(EPSILON)

        val groupingFactor =
            engine.groupingFactor(
                input.circuitsInConduit
            ).coerceAtLeast(EPSILON)

        val baseAmpacity =
            engine.conductorAmpacity(
                sectionMm2 =
                    section,
                material =
                    input.conductor,
                insulation =
                    input.insulation,
                installationMethod =
                    input.installationMethod,
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

        val voltageDrop =
            VoltageDropCalculator.calculate(
                current =
                    designCurrent,
                length =
                    input.lineLength,
                sectionMm2 =
                    section,
                powerFactor =
                    input.powerFactor,
                currentType =
                    input.currentType,
                material =
                    input.conductor,
                voltage =
                    input.voltage
            )

        val protectiveDevice =
            if (ampacity > EPSILON) {

                BreakerSelectionCalculator.selectRating(
                    designCurrentA =
                        designCurrent,
                    cableAmpacityA =
                        ampacity,
                    standard =
                        standard
                )

            } else {
                0.0
            }

        val breakerWithinCapacity =
            protectiveDevice > EPSILON &&
                ampacity > EPSILON &&
                BreakerSelectionCalculator.satisfiesCoordination(
                    designCurrentA =
                        designCurrent,
                    breakerRatingA =
                        protectiveDevice,
                    cableAmpacityA =
                        ampacity
                )

        val shortCircuit =
            runCatching {

                ShortCircuitCalculator.calculate(
                    voltage =
                        input.voltage,
                    length =
                        input.lineLength,
                    sectionMm2 =
                        section,
                    material =
                        input.conductor,
                    currentType =
                        input.currentType
                )

            }.getOrNull()

        val shortCircuitKA =
            shortCircuit
                ?.shortCircuitCurrentKA
                ?: 0.0

        val catalogCableResult =
            EquipmentSelectionCalculator.selectCable(
                sectionMm2 =
                    section,
                manufacturer =
                    manufacturer,
                standard =
                    standard
            )

        val catalogCable =
            catalogCableResult.selected

        val catalogBreakerResult =
            if (protectiveDevice > EPSILON) {

                EquipmentSelectionCalculator.selectBreaker(
                    ratedCurrentA =
                        protectiveDevice,
                    breakingCapacityKA =
                        shortCircuitKA,
                    manufacturer =
                        manufacturer,
                    standard =
                        standard
                )

            } else {
                null
            }

        val catalogBreaker =
            catalogBreakerResult?.selected

        val voltageDropWithinLimit =
            voltageDrop.first <=
                input.maxVoltageDrop +
                EPSILON

        val ampacityVerified =
            baseAmpacity != null

        val ampacityWithinLimit =
            ampacityVerified &&
                ampacity + EPSILON >=
                    designCurrent

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
                        "DATA INCOMPLETE: verified code ampacity data are unavailable for this conductor configuration."
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

                if (protectiveDevice > EPSILON) {

                    add(
                        "Engineering breaker rating = %.0f A"
                            .format(protectiveDevice)
                    )

                } else {

                    add(
                        "No verified standard breaker rating satisfies the engineering current/cable relationship."
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
                        "No matching manufacturer catalog cable was found."
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

                } else if (protectiveDevice > EPSILON) {

                    add(
                        "No verified manufacturer breaker was found for the calculated fault-duty requirement."
                    )
                }

                if (shortCircuit != null) {

                    add(
                        "Preliminary short-circuit current = %.3f kA"
                            .format(shortCircuitKA)
                    )

                    add(
                        "Short-circuit result is preliminary until complete source/network data are supplied."
                    )
                }

                if (!engine.isFullyImplemented()) {

                    add(
                        engine.implementationStatus()
                    )
                }

                if (forceNoCodeDataWarning) {

                    add(
                        "Automatic sizing could not verify a complete code ampacity dataset."
                    )
                }

                if (!catalogCableResult.valid) {

                    add(
                        "Cable catalog selection is NOT VERIFIED."
                    )
                }

                if (
                    catalogBreakerResult != null &&
                    !catalogBreakerResult.valid
                ) {

                    add(
                        "Breaker catalog selection is NOT VERIFIED because required manufacturer breaking-capacity data are incomplete."
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
