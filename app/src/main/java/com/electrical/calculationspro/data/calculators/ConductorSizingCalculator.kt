
package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.catalog.BreakerCatalogItem
import com.electrical.calculationspro.data.catalog.CableCatalogItem
import com.electrical.calculationspro.data.catalog.Manufacturer
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.standards.CodeRuleRegistry
import com.electrical.calculationspro.data.standards.EngineeringRuleBook

/**
 * Code-driven conductor sizing.
 *
 * Missing numerical code data must never be converted into a fabricated
 * correction factor, ampacity or automatic conductor recommendation.
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

        val ruleStatus = resolveRequiredRules(standard)
        val engine = CodeEngineFactory.get(standard)

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

        val temperatureFactor = engine.ambientTemperatureFactor(
            insulation = input.insulation,
            ambientTemperatureC = input.ambientTemp
        )
        val groupingFactor = engine.groupingFactor(input.circuitsInConduit)

        if (!isValidTemperatureFactor(temperatureFactor)) {
            return unavailableResult(
                designCurrent,
                rawCurrent,
                standard,
                "Verified ambient-temperature correction data are unavailable for this selected code and insulation."
            )
        }

        if (!isValidGroupingFactor(groupingFactor)) {
            return unavailableResult(
                designCurrent,
                rawCurrent,
                standard,
                "Verified grouping correction data are unavailable for the specified installation arrangement."
            )
        }

        val requiredBaseIz = designCurrent / temperatureFactor / groupingFactor
        val sections = engine.standardConductorSections()
            .filter { it.isFinite() && it > EPSILON }
            .distinct()
            .sorted()

        if (sections.isEmpty()) {
            return unavailableResult(
                designCurrent,
                rawCurrent,
                standard,
                "The selected standard has no available conductor-section dataset."
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
                baseAmpacity + EPSILON >= requiredBaseIz
        }

        if (selectedSection == null) {
            return unavailableResult(
                designCurrent,
                rawCurrent,
                standard,
                "No listed conductor section has verified ampacity sufficient for this design. The largest listed section is not automatically recommended."
            )
        }

        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            manufacturer = manufacturer,
            codeRulesVerified = ruleStatus
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

        val ruleStatus = resolveRequiredRules(standard)
        val engine = CodeEngineFactory.get(standard)

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

        val temperatureFactor = engine.ambientTemperatureFactor(
            insulation = input.insulation,
            ambientTemperatureC = input.ambientTemp
        )
        val groupingFactor = engine.groupingFactor(input.circuitsInConduit)

        if (!isValidTemperatureFactor(temperatureFactor) ||
            !isValidGroupingFactor(groupingFactor)
        ) {
            val drop = VoltageDropCalculator.calculate(
                current = designCurrent,
                length = input.lineLength,
                sectionMm2 = selectedSection,
                powerFactor = input.powerFactor,
                currentType = input.currentType,
                material = input.conductor,
                voltage = input.voltage
            )
            return ConductorSizingResult(
                designCurrent = designCurrent,
                recommendedSection = 0.0,
                selectedSection = selectedSection,
                ampacity = 0.0,
                voltageDropPercent = drop.first,
                voltageDropVolts = drop.second,
                protectiveDevice = 0.0,
                notes = listOf(
                    "SELECTED SECTION — NOT VERIFIED FOR CODE COMPLIANCE",
                    "Code: ${engine.codeName}",
                    "Selected section: %.1f mm²".format(selectedSection),
                    "Voltage drop is a preliminary calculation only.",
                    "Verified ambient-temperature and/or grouping correction data are unavailable.",
                    "Corrected ampacity, breaker coordination and final code compliance were not verified."
                )
            )
        }

        val requiredBaseIz = designCurrent / temperatureFactor / groupingFactor
        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            manufacturer = manufacturer,
            codeRulesVerified = ruleStatus
        )
    }

    private fun evaluateSection(
        input: ConductorSizingInput,
        section: Double,
        designCurrent: Double,
        rawDesignCurrent: Double,
        standard: Standard,
        requiredBaseIz: Double,
        manufacturer: Manufacturer?,
        codeRulesVerified: Boolean
    ): ConductorSizingResult {
        val engine = CodeEngineFactory.get(standard)

        val temperatureFactor = engine.ambientTemperatureFactor(
            insulation = input.insulation,
            ambientTemperatureC = input.ambientTemp
        )
        val groupingFactor = engine.groupingFactor(input.circuitsInConduit)

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

        val correctedAmpacity = if (factorsVerified && baseAmpacity != null) {
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

        val protectiveDevice = if (correctedAmpacity > EPSILON) {
            BreakerSelectionCalculator.selectRating(
                designCurrentA = designCurrent,
                cableAmpacityA = correctedAmpacity,
                standard = standard
            )
        } else {
            0.0
        }

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

        val shortCircuitKA = shortCircuit?.shortCircuitCurrentKA ?: 0.0

        val cableResult = EquipmentSelectionCalculator.selectCable(
            sectionMm2 = section,
            manufacturer = manufacturer,
            standard = standard
        )
        val catalogCable: CableCatalogItem? = cableResult.selected

        val breakerResult = if (protectiveDevice > EPSILON) {
            EquipmentSelectionCalculator.selectBreaker(
                ratedCurrentA = protectiveDevice,
                breakingCapacityKA = shortCircuitKA,
                manufacturer = manufacturer,
                standard = standard
            )
        } else {
            null
        }
        val catalogBreaker: BreakerCatalogItem? = breakerResult?.selected

        val ampacityVerified = baseAmpacity != null && factorsVerified
        val ampacityWithinLimit =
            ampacityVerified && correctedAmpacity + EPSILON >= designCurrent

        val breakerWithinCapacity =
            protectiveDevice > EPSILON &&
                correctedAmpacity > EPSILON &&
                BreakerSelectionCalculator.satisfiesCoordination(
                    designCurrentA = designCurrent,
                    breakerRatingA = protectiveDevice,
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
            add("Raw design current = %.2f A".format(rawDesignCurrent))
            add("Design current Ib = %.2f A".format(designCurrent))
            add("Selected section = %.1f mm²".format(section))
            add("Required base Iz = %.2f A".format(requiredBaseIz))

            if (baseAmpacity != null && factorsVerified) {
                add("Base ampacity = %.2f A".format(baseAmpacity))
                add("Ambient correction factor = %.3f".format(temperatureFactor))
                add("Grouping factor = %.3f".format(groupingFactor))
                add("Corrected ampacity Iz = %.2f A".format(correctedAmpacity))
                add(
                    if (ampacityWithinLimit) {
                        "Ampacity verification: PASS"
                    } else {
                        "Ampacity verification: FAIL"
                    }
                )
            } else {
                add("CODE DATA INCOMPLETE: verified ampacity/correction data unavailable.")
            }

            add("Voltage drop = %.3f V".format(voltageDrop.voltageDropVolts))
            add("Voltage drop = %.3f %%".format(voltageDrop.voltageDropPercent))
            addAll(voltageDrop.notes)

            if (protectiveDevice > EPSILON) {
                add("Engineering breaker rating In = %.0f A".format(protectiveDevice))
                add(
                    if (breakerWithinCapacity) {
                        "Breaker/cable coordination Ib <= In <= Iz: PASS"
                    } else {
                        "Breaker/cable coordination Ib <= In <= Iz: NOT VERIFIED"
                    }
                )
            } else {
                add("No verified breaker rating was established from the available ampacity data.")
            }

            add(
                "Short-circuit study is preliminary until source fault level and complete network data are supplied."
            )

            if (catalogCable != null) {
                add("Preliminary catalog cable = ${catalogCable.model}")
                add("Cable manufacturer = ${catalogCable.manufacturer}")
                add("Cable catalog status: ${cableResult.message}")
            } else {
                add("No matching catalog cable reference was found.")
            }

            if (catalogBreaker != null) {
                add("Preliminary catalog breaker = ${catalogBreaker.model}")
                add("Breaker manufacturer = ${catalogBreaker.manufacturer}")
                add("Breaker catalog status = ${breakerResult?.message}")
            } else if (protectiveDevice > EPSILON) {
                add("No matching catalog breaker was found.")
            }

            if (!codeRulesVerified) {
                add("FINAL CODE COMPLIANCE: NOT VERIFIED; one or more code rules are incomplete.")
            }
            if (!factorsVerified) {
                add("Correction factors are unavailable; corrected ampacity is not verified.")
            }
            if (!cableResult.valid) {
                add("CATALOG CABLE COMPLIANCE: NOT VERIFIED.")
            }
            if (breakerResult != null && !breakerResult.valid) {
                add("CATALOG BREAKER COMPLIANCE: NOT VERIFIED.")
            }

            add(
                if (finalVerified) {
                    "OVERALL ENGINEERING VERIFICATION: PASS"
                } else {
                    "OVERALL ENGINEERING VERIFICATION: NOT VERIFIED"
                }
            )

            addAll(EngineeringRuleBook.auditTrail(standard, CodeRuleRegistry.RuleDomain.CONDUCTOR))
            addAll(EngineeringRuleBook.auditTrail(standard, CodeRuleRegistry.RuleDomain.AMPACITY))
            addAll(EngineeringRuleBook.auditTrail(standard, CodeRuleRegistry.RuleDomain.INSTALLATION_METHOD))
            addAll(EngineeringRuleBook.auditTrail(standard, CodeRuleRegistry.RuleDomain.VOLTAGE_DROP))
        }

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = section,
            selectedSection = section,
            ampacity = correctedAmpacity,
            voltageDropPercent = voltageDrop.voltageDropPercent,
            voltageDropVolts = voltageDrop.voltageDropVolts,
            protectiveDevice = protectiveDevice,
            shortCircuitCurrentKA = shortCircuitKA,
            breakerWithinCableCapacity = breakerWithinCapacity,
            voltageDropWithinLimit = voltageDropWithinLimit,
            notes = notes,
            catalogCable = catalogCable,
            catalogBreaker = catalogBreaker
        )
    }

    private fun resolveRequiredRules(standard: Standard): Boolean {
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

    private fun unavailableResult(
        designCurrent: Double,
        rawDesignCurrent: Double,
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
                "Raw design current: %.2f A".format(rawDesignCurrent),
                "Design current: %.2f A".format(designCurrent),
                reason,
                "A zero section means no recommendation was issued; it is not a valid cable size."
            )
        )

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
            "Load must be finite and cannot be negative."
        }
        require(input.powerFactor.isFinite() &&
            input.powerFactor > 0.0 &&
            input.powerFactor <= 1.0
        ) {
            "Power factor must be finite, greater than 0 and not greater than 1."
        }
        require(input.lineLength.isFinite() && input.lineLength >= 0.0) {
            "Line length must be finite and cannot be negative."
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
