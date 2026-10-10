
package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.catalog.BreakerCatalogItem
import com.electrical.calculationspro.data.catalog.CableCatalogItem
import com.electrical.calculationspro.data.catalog.Manufacturer
import com.electrical.calculationspro.data.catalog.TechnicalRequirement
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.standards.CodeRuleRegistry
import com.electrical.calculationspro.data.standards.EngineeringRuleBook

/**
 * Code-driven conductor sizing integrated with the existing
 * project models, engineering calculators and equipment catalogues.
 *
 * Missing verified engineering data never produces a fabricated
 * conductor size or a false compliance status.
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
        validateDemandAndDiversity(demandFactor, diversityFactor)

        val rawCurrent = LoadCalculator.designCurrent(
            loadWatts = input.load,
            voltage = input.voltage,
            powerFactor = input.powerFactor,
            currentType = input.currentType
        )

        val designCurrent = LoadCalculator.applyDemandAndDiversity(
            current = rawCurrent,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor
        )

        val engine = CodeEngineFactory.get(standard)
        val codeRulesVerified = requiredRulesVerified(standard)

        val temperatureFactor = engine.ambientTemperatureFactor(
            insulation = input.insulation,
            ambientTemperatureC = input.ambientTemp
        )

        val groupingFactor = engine.groupingFactor(
            input.circuitsInConduit
        )

        if (!isValidTemperatureFactor(temperatureFactor)) {
            return unavailableResult(
                input = input,
                designCurrent = designCurrent,
                rawCurrent = rawCurrent,
                standard = standard,
                reason = "Verified ambient-temperature correction data are unavailable."
            )
        }

        if (!isValidGroupingFactor(groupingFactor)) {
            return unavailableResult(
                input = input,
                designCurrent = designCurrent,
                rawCurrent = rawCurrent,
                standard = standard,
                reason = "Verified grouping correction data are unavailable."
            )
        }

        val requiredBaseAmpacity =
            designCurrent / temperatureFactor / groupingFactor

        val sections = engine.standardConductorSections()
            .filter { it.isFinite() && it > EPSILON }
            .distinct()
            .sorted()

        if (sections.isEmpty()) {
            return unavailableResult(
                input = input,
                designCurrent = designCurrent,
                rawCurrent = rawCurrent,
                standard = standard,
                reason = "No conductor-section dataset is available for the selected standard."
            )
        }

        val selectedSection = sections.firstOrNull { section ->
            val baseAmpacity = engine.conductorAmpacity(
                sectionMm2 = section,
                material = input.conductor,
                insulation = input.insulation,
                installationMethod = input.installationMethod,
                loadedConductors = loadedConductorCount(input.currentType)
            )

            baseAmpacity != null &&
                baseAmpacity.isFinite() &&
                baseAmpacity > 0.0 &&
                baseAmpacity + EPSILON >= requiredBaseAmpacity
        }

        if (selectedSection == null) {
            return unavailableResult(
                input = input,
                designCurrent = designCurrent,
                rawCurrent = rawCurrent,
                standard = standard,
                reason = "No listed conductor section has verified ampacity sufficient for this design."
            )
        }

        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawCurrent = rawCurrent,
            standard = standard,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor,
            manufacturer = manufacturer,
            codeRulesVerified = codeRulesVerified
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

        require(selectedSection.isFinite() && selectedSection > EPSILON) {
            "Selected conductor section must be finite and greater than zero."
        }

        validateDemandAndDiversity(demandFactor, diversityFactor)

        val rawCurrent = LoadCalculator.designCurrent(
            loadWatts = input.load,
            voltage = input.voltage,
            powerFactor = input.powerFactor,
            currentType = input.currentType
        )

        val designCurrent = LoadCalculator.applyDemandAndDiversity(
            current = rawCurrent,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor
        )

        val engine = CodeEngineFactory.get(standard)

        val temperatureFactor = engine.ambientTemperatureFactor(
            insulation = input.insulation,
            ambientTemperatureC = input.ambientTemp
        )

        val groupingFactor = engine.groupingFactor(
            input.circuitsInConduit
        )

        if (!isValidTemperatureFactor(temperatureFactor) ||
            !isValidGroupingFactor(groupingFactor)
        ) {
            return unavailableSelectedSectionResult(
                input = input,
                section = selectedSection,
                designCurrent = designCurrent,
                standard = standard,
                reason = "Verified temperature or grouping correction data are unavailable."
            )
        }

        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawCurrent = rawCurrent,
            standard = standard,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor,
            manufacturer = manufacturer,
            codeRulesVerified = requiredRulesVerified(standard)
        )
    }

    private fun evaluateSection(
        input: ConductorSizingInput,
        section: Double,
        designCurrent: Double,
        rawCurrent: Double,
        standard: Standard,
        demandFactor: Double,
        diversityFactor: Double,
        manufacturer: Manufacturer?,
        codeRulesVerified: Boolean
    ): ConductorSizingResult {
        val engine = CodeEngineFactory.get(standard)

        val temperatureFactor = engine.ambientTemperatureFactor(
            insulation = input.insulation,
            ambientTemperatureC = input.ambientTemp
        )

        val groupingFactor = engine.groupingFactor(
            input.circuitsInConduit
        )

        val factorsVerified =
            isValidTemperatureFactor(temperatureFactor) &&
                isValidGroupingFactor(groupingFactor)

        val baseAmpacity = engine.conductorAmpacity(
            sectionMm2 = section,
            material = input.conductor,
            insulation = input.insulation,
            installationMethod = input.installationMethod,
            loadedConductors = loadedConductorCount(input.currentType)
        )?.takeIf { it.isFinite() && it > 0.0 }

        val correctedAmpacity =
            if (factorsVerified && baseAmpacity != null) {
                baseAmpacity * temperatureFactor * groupingFactor
            } else {
                0.0
            }

        val voltageDrop = VoltageDropCalculator.calculateCodeDriven(
            current = designCurrent,
            length = input.lineLength,
            sectionMm2 = section,
            powerFactor = input.powerFactor,
            currentType = input.currentType,
            material = input.conductor,
            voltage = input.voltage,
            standard = standard,
            circuitCategory = "general",
            maxVoltageDropPercent = input.maxVoltageDrop
        )

        val breakerRating = if (correctedAmpacity > EPSILON) {
            BreakerSelectionCalculator.selectRating(
                designCurrentA = designCurrent,
                cableAmpacityA = correctedAmpacity,
                standard = standard
            )
        } else {
            0.0
        }

        /*
         * Source fault level is deliberately zero until the actual
         * source/network fault level is supplied. This is not a
         * verified short-circuit study.
         */
        val shortCircuit = runCatching {
            ShortCircuitCalculator.calculate(
                voltage = input.voltage,
                length = input.lineLength,
                sectionMm2 = section,
                material = input.conductor,
                currentType = input.currentType,
                sourceIkKA = 0.0,
                standard = standard
            )
        }.getOrNull()

        val shortCircuitKA =
            shortCircuit?.shortCircuitCurrentKA ?: 0.0

        /*
         * Catalogue selection uses the actual engineering requirement,
         * including conductor material, insulation, voltage, core count
         * and selected code. A preliminary match is not treated as
         * final product compliance.
         */
        val cableRequirement = TechnicalRequirement.Cable(
            requiredSectionMm2 = section,
            requiredVoltageV = input.voltage.toInt(),
            cores = when (input.currentType) {
                CurrentType.DirectCurrent,
                CurrentType.AlternatingSinglePhase -> 2

                CurrentType.AlternatingTwoPhase,
                CurrentType.AlternatingThreePhase -> 3
            },
            material = input.conductor,
            insulation = input.insulation,
            standard = standard
        )

        val cableResult = EquipmentSelectionCalculator.selectCompliantCable(
            requirement = cableRequirement,
            manufacturer = manufacturer
        )

        val catalogCable: CableCatalogItem? = cableResult.selected

        /*
         * A final breaker selection requires a positive prospective
         * fault-current requirement. Until that value is supplied,
         * retain a preliminary candidate only; it cannot pass final
         * engineering verification.
         */
        val breakerResult =
            if (breakerRating > EPSILON) {
                if (shortCircuitKA > EPSILON) {
                    EquipmentSelectionCalculator.selectCompliantBreaker(
                        requirement = TechnicalRequirement.Breaker(
                            requiredCurrentA = breakerRating,
                            requiredVoltageV = input.voltage,
                            requiredBreakingCapacityKA = shortCircuitKA,
                            poles = when (input.currentType) {
                                CurrentType.DirectCurrent -> 2
                                CurrentType.AlternatingSinglePhase -> 2

                                CurrentType.AlternatingTwoPhase,
                                CurrentType.AlternatingThreePhase -> 3
                            },
                            standard = standard
                        ),
                        manufacturer = manufacturer
                    )
                } else {
                    EquipmentSelectionCalculator.selectBreaker(
                        ratedCurrentA = breakerRating,
                        breakingCapacityKA = 0.0,
                        manufacturer = manufacturer,
                        standard = standard
                    )
                }
            } else {
                null
            }

        val catalogBreaker: BreakerCatalogItem? =
            breakerResult?.selected

        val ampacityVerified =
            baseAmpacity != null && factorsVerified

        val ampacityWithinLimit =
            ampacityVerified &&
                correctedAmpacity + EPSILON >= designCurrent

        val breakerWithinCapacity =
            breakerRating > EPSILON &&
                correctedAmpacity > EPSILON &&
                BreakerSelectionCalculator.satisfiesCoordination(
                    designCurrentA = designCurrent,
                    breakerRatingA = breakerRating,
                    cableAmpacityA = correctedAmpacity
                )

        val voltageDropWithinLimit =
            voltageDrop.allowedVoltageDropPercent != null &&
                voltageDrop.withinCodeLimit

        val finalVerified =
            codeRulesVerified &&
                ampacityWithinLimit &&
                voltageDrop.codeVerified &&
                voltageDropWithinLimit &&
                breakerWithinCapacity &&
                cableResult.valid &&
                (breakerResult == null || breakerResult.valid)

        val notes = buildList {
            add("CODE-DRIVEN CONDUCTOR DESIGN")
            add("Code: ${engine.codeName}")
            add("Code revision: ${engine.codeRevision}")
            add("Raw design current = %.2f A".format(rawCurrent))
            add("Demand factor = %.3f".format(demandFactor))
            add("Diversity factor = %.3f".format(diversityFactor))
            add("Design current Ib = %.2f A".format(designCurrent))
            add("Selected section = %.1f mm²".format(section))

            if (baseAmpacity != null && factorsVerified) {
                val requiredBaseAmpacity =
                    designCurrent / temperatureFactor / groupingFactor

                add(
                    "Required base ampacity = %.2f A"
                        .format(requiredBaseAmpacity)
                )
                add("Base ampacity = %.2f A".format(baseAmpacity))
                add(
                    "Ambient correction factor = %.3f"
                        .format(temperatureFactor)
                )
                add(
                    "Grouping factor = %.3f"
                        .format(groupingFactor)
                )
                add(
                    "Corrected ampacity Iz = %.2f A"
                        .format(correctedAmpacity)
                )
                add(
                    if (ampacityWithinLimit) {
                        "Ampacity verification: PASS"
                    } else {
                        "Ampacity verification: FAIL"
                    }
                )
            } else {
                add(
                    "CODE DATA INCOMPLETE: verified ampacity or correction-factor data are unavailable."
                )
            }

            add(
                "Voltage drop = %.3f V"
                    .format(voltageDrop.voltageDropVolts)
            )
            add(
                "Voltage drop = %.3f %%"
                    .format(voltageDrop.voltageDropPercent)
            )
            addAll(voltageDrop.notes)

            if (breakerRating > EPSILON) {
                add(
                    "Engineering breaker rating In = %.0f A"
                        .format(breakerRating)
                )
                add(
                    if (breakerWithinCapacity) {
                        "Breaker/cable coordination Ib <= In <= Iz: PASS"
                    } else {
                        "Breaker/cable coordination Ib <= In <= Iz: NOT VERIFIED"
                    }
                )
            } else {
                add(
                    "No verified breaker rating was established from the available ampacity data."
                )
            }

            add(
                "Short-circuit assessment is preliminary: the source fault level has not been supplied."
            )

            if (catalogCable != null) {
                add("Catalogue cable model = ${catalogCable.model}")
                add("Cable manufacturer = ${catalogCable.manufacturer}")
                add("Cable catalogue status = ${cableResult.message}")
            } else {
                add(
                    "No matching cable catalogue item was found; catalogue compliance is NOT VERIFIED."
                )
            }

            if (catalogBreaker != null) {
                add("Catalogue breaker model = ${catalogBreaker.model}")
                add("Breaker manufacturer = ${catalogBreaker.manufacturer}")
                add("Breaker catalogue status = ${breakerResult?.message}")
            } else if (breakerRating > EPSILON) {
                add(
                    "No matching breaker catalogue item was found; catalogue compliance is NOT VERIFIED."
                )
            }

            if (!codeRulesVerified) {
                add(
                    "FINAL CODE COMPLIANCE: NOT VERIFIED; required code references are incomplete."
                )
            }

            if (!factorsVerified) {
                add(
                    "Correction factors are unavailable; corrected ampacity is NOT VERIFIED."
                )
            }

            if (!cableResult.valid) {
                add(
                    "CATALOGUE CABLE COMPLIANCE: NOT VERIFIED. ${cableResult.message}"
                )
            }

            if (breakerResult != null && !breakerResult.valid) {
                add(
                    "CATALOGUE BREAKER COMPLIANCE: NOT VERIFIED. ${breakerResult.message}"
                )
            }

            add(
                if (finalVerified) {
                    "OVERALL ENGINEERING VERIFICATION: PASS"
                } else {
                    "OVERALL ENGINEERING VERIFICATION: NOT VERIFIED"
                }
            )

            addAll(
                EngineeringRuleBook.auditTrail(
                    standard,
                    CodeRuleRegistry.RuleDomain.CONDUCTOR
                )
            )
            addAll(
                EngineeringRuleBook.auditTrail(
                    standard,
                    CodeRuleRegistry.RuleDomain.AMPACITY
                )
            )
            addAll(
                EngineeringRuleBook.auditTrail(
                    standard,
                    CodeRuleRegistry.RuleDomain.INSTALLATION_METHOD
                )
            )
            addAll(
                EngineeringRuleBook.auditTrail(
                    standard,
                    CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
                )
            )
        }

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = section,
            selectedSection = section,
            ampacity = correctedAmpacity,
            voltageDropPercent = voltageDrop.voltageDropPercent,
            voltageDropVolts = voltageDrop.voltageDropVolts,
            protectiveDevice = breakerRating,
            shortCircuitCurrentKA = shortCircuitKA,
            breakerWithinCableCapacity = breakerWithinCapacity,
            voltageDropWithinLimit = voltageDropWithinLimit,
            notes = notes,
            catalogCable = catalogCable,
            catalogBreaker = catalogBreaker
        )
    }

    private fun unavailableSelectedSectionResult(
        input: ConductorSizingInput,
        section: Double,
        designCurrent: Double,
        standard: Standard,
        reason: String
    ): ConductorSizingResult {
        val drop = VoltageDropCalculator.calculate(
            current = designCurrent,
            length = input.lineLength,
            sectionMm2 = section,
            powerFactor = input.powerFactor,
            currentType = input.currentType,
            material = input.conductor,
            voltage = input.voltage
        )

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = 0.0,
            selectedSection = section,
            ampacity = 0.0,
            voltageDropPercent = drop.first,
            voltageDropVolts = drop.second,
            protectiveDevice = 0.0,
            notes = listOf(
                "SELECTED SECTION — NOT VERIFIED FOR CODE COMPLIANCE",
                "Code: ${CodeEngineFactory.get(standard).codeName}",
                "Selected section = %.1f mm²".format(section),
                reason,
                "Corrected ampacity, breaker coordination and final code compliance were not verified."
            )
        )
    }

    private fun unavailableResult(
        input: ConductorSizingInput,
        designCurrent: Double,
        rawCurrent: Double,
        standard: Standard,
        reason: String
    ): ConductorSizingResult =
        ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = 0.0,
            selectedSection = 0.0,
            ampacity = 0.0,
            voltageDropPercent = 0.0,
            voltageDropVolts = 0.0,
            protectiveDevice = 0.0,
            notes = listOf(
                "AUTOMATIC CONDUCTOR SIZING: NOT VERIFIED",
                "Code: ${CodeEngineFactory.get(standard).codeName}",
                "Voltage = %.2f V".format(input.voltage),
                "Load = %.2f W".format(input.load),
                "Line length = %.2f m".format(input.lineLength),
                "Raw design current = %.2f A".format(rawCurrent),
                "Design current = %.2f A".format(designCurrent),
                reason,
                "No conductor recommendation has been issued because required verified data are unavailable."
            )
        )

    private fun requiredRulesVerified(standard: Standard): Boolean {
        val conductor = EngineeringRuleBook.requireReference(
            standard,
            CodeRuleRegistry.RuleDomain.CONDUCTOR
        )
        val ampacity = EngineeringRuleBook.requireReference(
            standard,
            CodeRuleRegistry.RuleDomain.AMPACITY
        )
        val installation = EngineeringRuleBook.requireReference(
            standard,
            CodeRuleRegistry.RuleDomain.INSTALLATION_METHOD
        )
        val voltageDrop = EngineeringRuleBook.requireReference(
            standard,
            CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
        )

        return conductor.verified &&
            ampacity.verified &&
            installation.verified &&
            voltageDrop.verified
    }

    private fun isValidTemperatureFactor(value: Double): Boolean =
        value.isFinite() && value > EPSILON

    private fun isValidGroupingFactor(value: Double): Boolean =
        value.isFinite() && value > EPSILON && value <= 1.0

    private fun loadedConductorCount(currentType: CurrentType): Int =
        when (currentType) {
            CurrentType.DirectCurrent -> 2
            CurrentType.AlternatingSinglePhase -> 2
            CurrentType.AlternatingTwoPhase -> 2
            CurrentType.AlternatingThreePhase -> 3
        }

    private fun validateDemandAndDiversity(
        demandFactor: Double,
        diversityFactor: Double
    ) {
        require(demandFactor.isFinite() && demandFactor in 0.0..1.0) {
            "Demand factor must be finite and between 0 and 1."
        }
        require(diversityFactor.isFinite() && diversityFactor in 0.0..1.0) {
            "Diversity factor must be finite and between 0 and 1."
        }
    }

    private fun validateInput(input: ConductorSizingInput) {
        require(input.voltage.isFinite() && input.voltage > EPSILON) {
            "Voltage must be finite and greater than zero."
        }
        require(input.load.isFinite() && input.load >= 0.0) {
            "Load must be finite and non-negative."
        }
        require(
            input.powerFactor.isFinite() &&
                input.powerFactor > 0.0 &&
                input.powerFactor <= 1.0
        ) {
            "Power factor must be finite, greater than zero and not greater than 1."
        }
        require(input.lineLength.isFinite() && input.lineLength >= 0.0) {
            "Line length must be finite and non-negative."
        }
        require(input.ambientTemp.isFinite() && input.ambientTemp > -50.0) {
            "Ambient temperature is invalid."
        }
        require(input.circuitsInConduit >= 1) {
            "Number of circuits must be at least 1."
        }
        require(input.maxVoltageDrop.isFinite() && input.maxVoltageDrop > 0.0) {
            "Maximum voltage drop must be finite and greater than zero."
        }
    }
}
