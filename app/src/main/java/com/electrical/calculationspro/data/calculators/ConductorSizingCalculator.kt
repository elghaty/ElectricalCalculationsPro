package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import kotlin.math.abs

/**
 * Professional conductor sizing engine.
 *
 * All code-dependent data comes through StandardEngine.
 *
 * UI must never access IEC/NEC/Egyptian tables directly.
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

        require(demandFactor > 0.0) {
            "Demand factor must be greater than zero."
        }

        require(diversityFactor > 0.0) {
            "Diversity factor must be greater than zero."
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
                ib = rawDesignCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        val requiredBaseIz =
            if (input.ambientTemp > EPSILON) {
                designCurrent /
                    engine.ambientTemperatureFactor(
                        insulation = input.insulation,
                        ambientTemperatureC =
                            input.ambientTemp
                    ).coerceAtLeast(EPSILON) /
                    engine.groupingFactor(
                        input.circuitsInConduit
                    ).coerceAtLeast(EPSILON)
            } else {
                designCurrent
            }

        val sections =
            engine.standardConductorSections()
                .ifEmpty {
                    listOf(
                        1.5,
                        2.5,
                        4.0,
                        6.0,
                        10.0,
                        16.0,
                        25.0,
                        35.0,
                        50.0,
                        70.0,
                        95.0,
                        120.0,
                        150.0,
                        185.0,
                        240.0,
                        300.0
                    )
                }
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

                if (baseAmpacity == null) {
                    false
                } else {
                    baseAmpacity >=
                        requiredBaseIz
                }
            }

        val selected =
            candidate ?: sections.last()

        return evaluateSection(
            input = input,
            section = selected,
            designCurrent = designCurrent,
            rawDesignCurrent = rawDesignCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            forceNoCodeDataWarning = candidate == null
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
                ib = rawDesignCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        val requiredBaseIz =
            designCurrent /
                engine.ambientTemperatureFactor(
                    insulation = input.insulation,
                    ambientTemperatureC =
                        input.ambientTemp
                ).coerceAtLeast(EPSILON) /
                engine.groupingFactor(
                    input.circuitsInConduit
                ).coerceAtLeast(EPSILON)

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
                BreakerSelectionCalculator.satisfiesCoordination(
                    designCurrentA = designCurrent,
                    breakerRatingA = protectiveDevice,
                    cableAmpacityA = ampacity
                )

        val shortCircuit =
            try {
                ShortCircuitCalculator.calculate(
                    voltage = input.voltage,
                    length = input.lineLength,
                    sectionMm2 = section,
                    material = input.conductor,
                    currentType = input.currentType
                )
            } catch (_: Exception) {
                null
            }

        val shortCircuitKA =
            shortCircuit
                ?.shortCircuitCurrentKA
                ?: 0.0

        val voltageDropWithinLimit =
            voltageDrop.first <=
                input.maxVoltageDrop + EPSILON

        val ampacityValid =
            baseAmpacity != null &&
                ampacity + EPSILON >=
                designCurrent

        val notes =
            buildList {

                add(
                    "Code: ${engine.codeName}"
                )

                add(
                    "Design current = %.2f A"
                        .format(designCurrent)
                )

                add(
                    "Raw current = %.2f A"
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
                        "Temperature factor = %.3f"
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
                        "No verified ampacity data is available for this code/method/material combination."
                    )
                }

                add(
                    "Voltage drop = %.3f %%"
                        .format(voltageDrop.first)
                )

                add(
                    "Voltage drop = %.3f V"
                        .format(voltageDrop.second)
                )

                if (protectiveDevice > 0.0) {
                    add(
                        "Selected breaker rating = %.0f A"
                            .format(protectiveDevice)
                    )
                } else {
                    add(
                        "No breaker rating satisfies Ib <= In <= Iz."
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
                        "Automatic sizing could not verify a complete code dataset for the selected section."
                    )
                }

                if (abs(requiredBaseIz) > EPSILON) {
                    add(
                        "Required base Iz before correction = %.2f A"
                            .format(requiredBaseIz)
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
            breakerWithinCableCapacity = breakerWithinCapacity,
            voltageDropWithinLimit = voltageDropWithinLimit,
            notes = notes
        )
    }

    private fun loadedConductorCount(
        currentType: CurrentType
    ): Int {

        return when (currentType) {
            CurrentType.DirectCurrent -> 2
            CurrentType.AlternatingSinglePhase -> 2
            CurrentType.AlternatingTwoPhase -> 2
            CurrentType.AlternatingThreePhase -> 3
        }
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
            "Power factor must be > 0 and <= 1."
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
