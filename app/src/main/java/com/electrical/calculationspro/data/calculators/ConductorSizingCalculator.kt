package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
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
 *
 * Code-dependent data is never read directly from IEC/NEC/Egyptian
 * tables here.
 */
object ConductorSizingCalculator {

    private const val EPSILON = 1.0e-9

    fun size(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
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
                .sorted()

        val candidate =
            sections.firstOrNull { section ->

                val baseAmpacity =
                    engine.conductorAmpacity(
                        sectionMm2 = section,
                        material = input.conductor,
                        insulation = input.insulation,
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
            candidate ?: sections.lastOrNull()
                ?: 300.0

        return evaluateSection(
            input = input,
            section = selected,
            designCurrent = designCurrent,
            rawDesignCurrent = rawDesignCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            forceNoCodeDataWarning =
                candidate == null
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
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawDesignCurrent,
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
        forceNoCodeDataWarning: Boolean = false
    ): ConductorSizingResult {

        val engine =
            CodeEngineFactory.get(standard)

        val temperatureFactor =
            engine.ambientTemperatureFactor(
                insulation = input.insulation,
                ambientTemperatureC =
                    input.ambientTemp
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
                current = designCurrent,
                length = input.lineLength,
                sectionMm2 = section,
                powerFactor = input.powerFactor,
                currentType = input.currentType,
                material = input.conductor,
                voltage = input.voltage
            )

        val protectiveDevice =
            BreakerSelectionCalculator.selectRating(
                designCurrentA = designCurrent,
                cableAmpacityA = ampacity,
                standard = standard
            )

        val breakerWithinCapacity =
            protectiveDevice > 0.0 &&
                BreakerSelectionCalculator
                    .satisfiesCoordination(
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

        val voltageDropWithinLimit =
            voltageDrop.first <=
                input.maxVoltageDrop +
                EPSILON

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
                        "Selected breaker rating = %.0f A"
                            .format(protectiveDevice)
                    )

                } else {

                    add(
                        "No standard breaker rating satisfies Ib <= In <= Iz."
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
            }

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = section,
            selectedSection = section,
            ampacity = ampacity,
            voltageDropPercent = voltageDrop.first,
            voltageDropVolts = voltageDrop.second,
            protectiveDevice = protectiveDevice,
            shortCircuitCurrentKA = shortCircuitKA,
            breakerWithinCableCapacity =
                breakerWithinCapacity,
            voltageDropWithinLimit =
                voltageDropWithinLimit,
            notes = notes
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
