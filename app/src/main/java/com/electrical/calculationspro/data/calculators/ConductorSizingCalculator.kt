package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.catalog.Manufacturer
import com.electrical.calculationspro.data.catalog.CableCatalogItem
import com.electrical.calculationspro.data.catalog.BreakerCatalogItem
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.standards.CodeRuleRegistry
import com.electrical.calculationspro.data.standards.EngineeringRuleBook
import kotlin.math.abs

/**
 * PROFESSIONAL CODE-DRIVEN CONDUCTOR SIZING ENGINE
 *
 * Engineering chain:
 *
 * Load
 *   ↓
 * Design current
 *   ↓
 * Code demand/diversity
 *   ↓
 * Installation conditions
 *   ↓
 * Ambient correction
 *   ↓
 * Grouping correction
 *   ↓
 * Code ampacity
 *   ↓
 * Required Iz
 *   ↓
 * Standard section
 *   ↓
 * Voltage drop
 *   ↓
 * Short circuit
 *   ↓
 * Breaker coordination
 *   ↓
 * Manufacturer catalogue
 *   ↓
 * Engineering verification
 *
 * The calculator does not contain standard-specific tables.
 * All code-dependent data are obtained from StandardEngine and
 * EngineeringRuleBook.
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

        require(demandFactor in 0.0..1.0) {
            "Demand factor must be between 0 and 1."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity factor must be between 0 and 1."
        }

        /*
         * Resolve every code domain required by conductor sizing.
         *
         * A missing domain is a configuration problem, not a reason
         * to silently fall back to IEC/NEC data.
         */
        val conductorRule =
            EngineeringRuleBook.requireReference(
                standard,
                CodeRuleRegistry.RuleDomain.CONDUCTOR
            )

        val ampacityRule =
            EngineeringRuleBook.requireReference(
                standard,
                CodeRuleRegistry.RuleDomain.AMPACITY
            )

        val installationRule =
            EngineeringRuleBook.requireReference(
                standard,
                CodeRuleRegistry.RuleDomain.INSTALLATION_METHOD
            )

        val voltageDropRule =
            EngineeringRuleBook.requireReference(
                standard,
                CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
            )

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
            engine
                .ambientTemperatureFactor(
                    insulation = input.insulation,
                    ambientTemperatureC =
                        input.ambientTemp
                )
                .coerceAtLeast(EPSILON)

        val groupingFactor =
            engine
                .groupingFactor(
                    input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

        val requiredBaseIz =
            designCurrent /
                temperatureFactor /
                groupingFactor

        val sections =
            engine
                .standardConductorSections()
                .filter { it > EPSILON }
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
                    baseAmpacity + EPSILON >=
                    requiredBaseIz
            }

        val selected =
            candidate
                ?: sections.lastOrNull()
                ?: 0.0

        require(selected > EPSILON) {
            "No standard conductor section is available for the selected engineering code."
        }

        return evaluateSection(
            input = input,
            section = selected,
            designCurrent = designCurrent,
            rawDesignCurrent = rawDesignCurrent,
            standard = standard,
            requiredBaseIz = requiredBaseIz,
            manufacturer = manufacturer,
            forceNoCodeDataWarning =
                candidate == null,
            codeRulesVerified =
                conductorRule.verified &&
                    ampacityRule.verified &&
                    installationRule.verified &&
                    voltageDropRule.verified
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

        require(demandFactor in 0.0..1.0) {
            "Demand factor must be between 0 and 1."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity factor must be between 0 and 1."
        }

        val conductorRule =
            EngineeringRuleBook.requireReference(
                standard,
                CodeRuleRegistry.RuleDomain.CONDUCTOR
            )

        val ampacityRule =
            EngineeringRuleBook.requireReference(
                standard,
                CodeRuleRegistry.RuleDomain.AMPACITY
            )

        val installationRule =
            EngineeringRuleBook.requireReference(
                standard,
                CodeRuleRegistry.RuleDomain.INSTALLATION_METHOD
            )

        val voltageDropRule =
            EngineeringRuleBook.requireReference(
                standard,
                CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
            )

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

        val engine =
            CodeEngineFactory.get(standard)

        val temperatureFactor =
            engine
                .ambientTemperatureFactor(
                    insulation = input.insulation,
                    ambientTemperatureC =
                        input.ambientTemp
                )
                .coerceAtLeast(EPSILON)

        val groupingFactor =
            engine
                .groupingFactor(
                    input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

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
            manufacturer = manufacturer,
            codeRulesVerified =
                conductorRule.verified &&
                    ampacityRule.verified &&
                    installationRule.verified &&
                    voltageDropRule.verified
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
        forceNoCodeDataWarning: Boolean = false,
        codeRulesVerified: Boolean
    ): ConductorSizingResult {

        val engine =
            CodeEngineFactory.get(standard)

        val temperatureFactor =
            engine
                .ambientTemperatureFactor(
                    insulation = input.insulation,
                    ambientTemperatureC =
                        input.ambientTemp
                )
                .coerceAtLeast(EPSILON)

        val groupingFactor =
            engine
                .groupingFactor(
                    input.circuitsInConduit
                )
                .coerceAtLeast(EPSILON)

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

        /*
         * Code-driven voltage-drop calculation.
         *
         * The existing public input.maxVoltageDrop remains available
         * as project/design criterion.
         */
        val voltageDrop =
            VoltageDropCalculator.calculateCodeDriven(
                current = designCurrent,
                length = input.lineLength,
                sectionMm2 = section,
                powerFactor = input.powerFactor,
                currentType = input.currentType,
                material = input.conductor,
                voltage = input.voltage,
                standard = standard,
                circuitCategory = "general",
                maxVoltageDropPercent =
                    input.maxVoltageDrop
            )

        val protectiveDevice =
            if (ampacity > EPSILON) {
                BreakerSelectionCalculator.selectRating(
                    designCurrentA = designCurrent,
                    cableAmpacityA = ampacity,
                    standard = standard
                )
            } else {
                0.0
            }

        /*
         * Preliminary short-circuit calculation.
         *
         * The source Ik is intentionally not fabricated.
         */
        val shortCircuit =
            runCatching {
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
            shortCircuit
                ?.shortCircuitCurrentKA
                ?: 0.0

        /*
         * Catalogue selection is deliberately separated from the
         * engineering calculation.
         */
        val catalogCableResult =
            EquipmentSelectionCalculator.selectCable(
                sectionMm2 = section,
                manufacturer = manufacturer,
                standard = standard
            )

        val catalogCable =
            catalogCableResult.selected

        val catalogBreakerResult =
            if (protectiveDevice > EPSILON) {
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

        val voltageDropWithinLimit =
            voltageDrop.allowedVoltageDropPercent != null &&
                voltageDrop.withinCodeLimit

        val ampacityVerified =
            baseAmpacity != null

        val ampacityWithinLimit =
            ampacityVerified &&
                ampacity + EPSILON >=
                designCurrent

        val breakerWithinCapacity =
            protectiveDevice > EPSILON &&
                ampacity > EPSILON &&
                BreakerSelectionCalculator.satisfiesCoordination(
                    designCurrentA = designCurrent,
                    breakerRatingA = protectiveDevice,
                    cableAmpacityA = ampacity
                )

        val finalEngineeringValidity =
            codeRulesVerified &&
                ampacityWithinLimit &&
                voltageDrop.codeVerified &&
                voltageDropWithinLimit &&
                breakerWithinCapacity &&
                catalogCableResult.valid &&
                (
                    catalogBreakerResult == null ||
                        catalogBreakerResult.valid
                    )

        val notes =
            buildList {

                add(
                    "CODE-DRIVEN CONDUCTOR DESIGN"
                )

                add(
                    "Code: ${engine.codeName}"
                )

                add(
                    "Code revision: ${engine.codeRevision}"
                )

                add(
                    "Design current Ib = %.2f A"
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

                add(
                    "Required base Iz = %.2f A"
                        .format(requiredBaseIz)
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
                        "Corrected ampacity Iz = %.2f A"
                            .format(ampacity)
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
                        "CODE DATA INCOMPLETE: verified ampacity data are unavailable for this conductor configuration."
                    )
                }

                add(
                    "Voltage drop = %.3f V"
                        .format(
                            voltageDrop.voltageDropVolts
                        )
                )

                add(
                    "Voltage drop = %.3f %%"
                        .format(
                            voltageDrop.voltageDropPercent
                        )
                )

                if (
                    voltageDrop.allowedVoltageDropPercent !=
                    null
                ) {
                    add(
                        "Allowed voltage drop = %.3f %%"
                            .format(
                                voltageDrop.allowedVoltageDropPercent
                            )
                    )

                    add(
                        if (voltageDropWithinLimit) {
                            "Voltage-drop verification: PASS"
                        } else {
                            "Voltage-drop verification: FAIL"
                        }
                    )
                } else {
                    add(
                        "Voltage-drop code limit is unavailable as a verified numerical value."
                    )
                }

                if (protectiveDevice > EPSILON) {

                    add(
                        "Engineering breaker rating In = %.0f A"
                            .format(protectiveDevice)
                    )

                    add(
                        if (breakerWithinCapacity) {
                            "Breaker/cable coordination Ib <= In <= Iz: PASS"
                        } else {
                            "Breaker/cable coordination Ib <= In <= Iz: FAIL"
                        }
                    )

                } else {

                    add(
                        "No verified standard breaker rating satisfies the engineering current/cable relationship."
                    )
                }

                add(
                    "Short-circuit study status: preliminary/incomplete until source fault level and complete network data are supplied."
                )

                if (catalogCable != null) {

                    add(
                        "Selected catalog cable = ${catalogCable.model}"
                    )

                    add(
                        "Cable manufacturer = ${catalogCable.manufacturer}"
                    )

                    add(
                        "Cable family = ${catalogCable.family}"
                    )

                } else {

                    add(
                        "No matching manufacturer catalog cable was verified."
                    )
                }

                if (catalogBreaker != null) {

                    add(
                        "Selected catalog breaker = ${catalogBreaker.model}"
                    )

                    add(
                        "Breaker manufacturer = ${catalogBreaker.manufacturer}"
                    )

                    add(
                        "Breaker family = ${catalogBreaker.family}"
                    )

                } else if (protectiveDevice > EPSILON) {

                    add(
                        "No manufacturer breaker was verified for the calculated requirement."
                    )
                }

                if (!codeRulesVerified) {
                    add(
                        "FINAL CODE COMPLIANCE: NOT VERIFIED because one or more required numerical/code datasets are incomplete."
                    )
                }

                if (forceNoCodeDataWarning) {
                    add(
                        "Automatic sizing could not verify a complete code ampacity dataset."
                    )
                }

                if (!catalogCableResult.valid) {
                    add(
                        "CATALOG CABLE COMPLIANCE: NOT VERIFIED."
                    )
                }

                if (
                    catalogBreakerResult != null &&
                    !catalogBreakerResult.valid
                ) {
                    add(
                        "CATALOG BREAKER COMPLIANCE: NOT VERIFIED."
                    )
                }

                add(
                    if (finalEngineeringValidity) {
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
            ampacity = ampacity,
            voltageDropPercent =
                voltageDrop.voltageDropPercent,
            voltageDropVolts =
                voltageDrop.voltageDropVolts,
            protectiveDevice = protectiveDevice,
            shortCircuitCurrentKA = shortCircuitKA,
            breakerWithinCableCapacity =
                breakerWithinCapacity,
            voltageDropWithinLimit =
                voltageDropWithinLimit,
            notes = notes,
            catalogCable = catalogCable,
            catalogBreaker = catalogBreaker
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
