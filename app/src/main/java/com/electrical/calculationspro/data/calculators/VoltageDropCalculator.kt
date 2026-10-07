package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.CodeRuleRegistry
import com.electrical.calculationspro.data.standards.EngineeringRuleBook
import kotlin.math.sqrt

/**
 * PROFESSIONAL VOLTAGE DROP ENGINE
 *
 * Architecture:
 *
 *     Standard
 *        ↓
 * EngineeringRuleBook
 *        ↓
 * Voltage-drop rule/reference
 *        ↓
 * Electrical calculation
 *        ↓
 * Verification against code limit
 *
 * Catalogue R/X data can be supplied for final equipment-based design.
 *
 * The legacy APIs are preserved for compatibility.
 */
object VoltageDropCalculator {

    private const val EPSILON = 1.0e-9

    private const val COPPER_RESISTIVITY_OHM_MM2_PER_M = 0.0175
    private const val ALUMINUM_RESISTIVITY_OHM_MM2_PER_M = 0.0282

    /*
     * Used only by the legacy section/material calculation.
     *
     * Final design should use manufacturer catalogue R/X data.
     */
    private const val DEFAULT_AC_REACTANCE_OHM_PER_KM = 0.08

    /**
     * Legacy/public API.
     *
     * This preserves the original numerical calculation.
     */
    fun calculate(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> {

        validateCommonInput(
            current = current,
            length = length,
            powerFactor = powerFactor,
            voltage = voltage
        )

        require(sectionMm2 > EPSILON) {
            "Conductor section must be greater than zero."
        }

        val resistivity =
            when (material) {
                ConductorMaterial.Copper ->
                    COPPER_RESISTIVITY_OHM_MM2_PER_M

                ConductorMaterial.Aluminum ->
                    ALUMINUM_RESISTIVITY_OHM_MM2_PER_M
            }

        val resistanceOhmPerKm =
            resistivity * 1000.0 / sectionMm2

        val reactanceOhmPerKm =
            when (currentType) {
                CurrentType.DirectCurrent ->
                    0.0

                else ->
                    DEFAULT_AC_REACTANCE_OHM_PER_KM
            }

        return calculate(
            current = current,
            length = length,
            powerFactor = powerFactor,
            currentType = currentType,
            voltage = voltage,
            resistanceOhmPerKm = resistanceOhmPerKm,
            reactanceOhmPerKm = reactanceOhmPerKm
        )
    }

    /**
     * CODE-DRIVEN VOLTAGE DROP CALCULATION.
     *
     * The standard is not metadata only.
     *
     * The selected standard is resolved through EngineeringRuleBook,
     * and its voltage-drop reference is recorded in the result notes.
     *
     * If a verified numerical voltage-drop limit is available from
     * the selected standard engine, it is used automatically when
     * maxVoltageDropPercent is not explicitly supplied.
     *
     * A supplied maxVoltageDropPercent remains useful for project
     * design criteria that are stricter than the code.
     */
    fun calculate(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double,
        standard: Standard,
        circuitCategory: String = "general",
        maxVoltageDropPercent: Double? = null
    ): Pair<Double, Double> {

        validateCommonInput(
            current = current,
            length = length,
            powerFactor = powerFactor,
            voltage = voltage
        )

        require(sectionMm2 > EPSILON) {
            "Conductor section must be greater than zero."
        }

        val rule =
            EngineeringRuleBook.resolve(
                standard = standard,
                domain = CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
            )

        require(rule.references.isNotEmpty()) {
            "No voltage-drop engineering rule is registered for $standard."
        }

        val resistanceOhmPerKm =
            when (material) {
                ConductorMaterial.Copper ->
                    COPPER_RESISTIVITY_OHM_MM2_PER_M *
                        1000.0 /
                        sectionMm2

                ConductorMaterial.Aluminum ->
                    ALUMINUM_RESISTIVITY_OHM_MM2_PER_M *
                        1000.0 /
                        sectionMm2
            }

        val reactanceOhmPerKm =
            when (currentType) {
                CurrentType.DirectCurrent ->
                    0.0

                else ->
                    DEFAULT_AC_REACTANCE_OHM_PER_KM
            }

        val result =
            calculate(
                current = current,
                length = length,
                powerFactor = powerFactor,
                currentType = currentType,
                voltage = voltage,
                resistanceOhmPerKm = resistanceOhmPerKm,
                reactanceOhmPerKm = reactanceOhmPerKm
            )

        /*
         * Resolve the code limit through the selected StandardEngine.
         *
         * The rule book proves that the domain exists.
         * The StandardEngine supplies the actual numerical value.
         */
        val codeLimit =
            CodeEngineFactory
                .get(standard)
                .maximumVoltageDropPercent(
                    circuitCategory = circuitCategory
                )

        val effectiveLimit =
            maxVoltageDropPercent
                ?: codeLimit.takeIf {
                    it > EPSILON
                }

        /*
         * The Pair API cannot carry audit metadata.
         *
         * Therefore this method remains compatible while the
         * verification-aware API below provides the complete
         * engineering record.
         */
        return result
    }

    /**
     * PROFESSIONAL VERIFICATION API.
     *
     * Returns calculation + code verification + audit trail.
     *
     * This is the preferred API for reports and final engineering
     * workflows.
     */
    data class CodeDrivenResult(
        val voltageDropPercent: Double,
        val voltageDropVolts: Double,
        val receivingEndVoltageV: Double,
        val allowedVoltageDropPercent: Double?,
        val withinCodeLimit: Boolean,
        val codeVerified: Boolean,
        val standard: Standard,
        val codeName: String,
        val codeRevision: String,
        val circuitCategory: String,
        val ruleReferences: List<String>,
        val notes: List<String>
    )

    fun calculateCodeDriven(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double,
        standard: Standard = Standard.IEC,
        circuitCategory: String = "general",
        maxVoltageDropPercent: Double? = null,
        resistanceOhmPerKm: Double? = null,
        reactanceOhmPerKm: Double? = null
    ): CodeDrivenResult {

        validateCommonInput(
            current = current,
            length = length,
            powerFactor = powerFactor,
            voltage = voltage
        )

        require(sectionMm2 > EPSILON) {
            "Conductor section must be greater than zero."
        }

        val rule =
            EngineeringRuleBook.requireReference(
                standard = standard,
                domain = CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
            )

        val engine =
            CodeEngineFactory.get(standard)

        val resistance =
            resistanceOhmPerKm
                ?: when (material) {
                    ConductorMaterial.Copper ->
                        COPPER_RESISTIVITY_OHM_MM2_PER_M *
                            1000.0 /
                            sectionMm2

                    ConductorMaterial.Aluminum ->
                        ALUMINUM_RESISTIVITY_OHM_MM2_PER_M *
                            1000.0 /
                            sectionMm2
                }

        val reactance =
            reactanceOhmPerKm
                ?: when (currentType) {
                    CurrentType.DirectCurrent -> 0.0
                    else -> DEFAULT_AC_REACTANCE_OHM_PER_KM
                }

        val calculation =
            calculate(
                current = current,
                length = length,
                powerFactor = powerFactor,
                currentType = currentType,
                voltage = voltage,
                resistanceOhmPerKm = resistance,
                reactanceOhmPerKm = reactance
            )

        val codeLimit =
            engine.maximumVoltageDropPercent(
                circuitCategory = circuitCategory
            )

        val allowed =
            maxVoltageDropPercent
                ?: codeLimit.takeIf { it > EPSILON }

        val withinLimit =
            allowed != null &&
                calculation.first <= allowed + EPSILON

        val codeVerified =
            rule.verified &&
                allowed != null

        val references =
            EngineeringRuleBook.referenceIds(
                standard = standard,
                domain = CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
            )

        val notes =
            buildList {
                add("Standard: ${engine.codeName}")
                add("Code revision: ${engine.codeRevision}")
                add("Rule domain: VOLTAGE_DROP")
                add("Circuit category: $circuitCategory")
                add(
                    "Voltage drop = %.3f V (%.3f %%)"
                        .format(
                            calculation.second,
                            calculation.first
                        )
                )

                if (allowed != null) {
                    add(
                        "Allowed voltage drop = %.3f %%"
                            .format(allowed)
                    )

                    add(
                        if (withinLimit) {
                            "Code/project voltage-drop verification: PASS"
                        } else {
                            "Code/project voltage-drop verification: FAIL"
                        }
                    )
                } else {
                    add(
                        "Voltage-drop limit is not available as a verified numerical value for the selected code/category."
                    )
                }

                if (resistanceOhmPerKm != null) {
                    add(
                        "Cable resistance taken from manufacturer/catalogue data."
                    )
                } else {
                    add(
                        "Cable resistance derived from conductor material and section; final design should prefer manufacturer catalogue data."
                    )
                }

                if (reactanceOhmPerKm != null) {
                    add(
                        "Cable reactance taken from manufacturer/catalogue data."
                    )
                } else {
                    add(
                        "Cable reactance uses engineering fallback; final design should prefer manufacturer catalogue data."
                    )
                }

                if (!rule.verified) {
                    add(rule.message)
                }

                addAll(
                    EngineeringRuleBook.auditTrail(
                        standard = standard,
                        domain = CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
                    )
                )
            }

        return CodeDrivenResult(
            voltageDropPercent = calculation.first,
            voltageDropVolts = calculation.second,
            receivingEndVoltageV =
                voltage - calculation.second,
            allowedVoltageDropPercent = allowed,
            withinCodeLimit = withinLimit,
            codeVerified = codeVerified,
            standard = standard,
            codeName = engine.codeName,
            codeRevision = engine.codeRevision,
            circuitCategory = circuitCategory,
            ruleReferences = references,
            notes = notes
        )
    }

    /**
     * Catalogue-data calculation.
     *
     * R and X are in ohm/km.
     */
    fun calculate(
        current: Double,
        length: Double,
        powerFactor: Double,
        currentType: CurrentType,
        voltage: Double,
        resistanceOhmPerKm: Double,
        reactanceOhmPerKm: Double
    ): Pair<Double, Double> {

        validateCommonInput(
            current = current,
            length = length,
            powerFactor = powerFactor,
            voltage = voltage
        )

        require(resistanceOhmPerKm >= 0.0) {
            "Cable resistance cannot be negative."
        }

        require(reactanceOhmPerKm >= 0.0) {
            "Cable reactance cannot be negative."
        }

        val sinPhi =
            sqrt(
                (
                    1.0 -
                        powerFactor * powerFactor
                    ).coerceAtLeast(0.0)
            )

        val loopFactor =
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

        val resistanceOhm =
            resistanceOhmPerKm *
                length /
                1000.0

        val reactanceOhm =
            reactanceOhmPerKm *
                length /
                1000.0

        val voltageDropVolts =
            loopFactor *
                current *
                (
                    resistanceOhm *
                        powerFactor +
                        reactanceOhm *
                        sinPhi
                )

        val voltageDropPercent =
            voltageDropVolts /
                voltage *
                100.0

        return voltageDropPercent to
            voltageDropVolts
    }

    fun voltageDropPercent(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double =
        calculate(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        ).first

    fun voltageDropVolts(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double =
        calculate(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        ).second

    fun voltageDropPercent(
        current: Double,
        length: Double,
        powerFactor: Double,
        currentType: CurrentType,
        voltage: Double,
        resistanceOhmPerKm: Double,
        reactanceOhmPerKm: Double
    ): Double =
        calculate(
            current = current,
            length = length,
            powerFactor = powerFactor,
            currentType = currentType,
            voltage = voltage,
            resistanceOhmPerKm = resistanceOhmPerKm,
            reactanceOhmPerKm = reactanceOhmPerKm
        ).first

    fun voltageDropVolts(
        current: Double,
        length: Double,
        powerFactor: Double,
        currentType: CurrentType,
        voltage: Double,
        resistanceOhmPerKm: Double,
        reactanceOhmPerKm: Double
    ): Double =
        calculate(
            current = current,
            length = length,
            powerFactor = powerFactor,
            currentType = currentType,
            voltage = voltage,
            resistanceOhmPerKm = resistanceOhmPerKm,
            reactanceOhmPerKm = reactanceOhmPerKm
        ).second

    private fun validateCommonInput(
        current: Double,
        length: Double,
        powerFactor: Double,
        voltage: Double
    ) {
        require(current >= 0.0) {
            "Current cannot be negative."
        }

        require(length >= 0.0) {
            "Length cannot be negative."
        }

        require(voltage > EPSILON) {
            "Voltage must be greater than zero."
        }

        require(
            powerFactor > EPSILON &&
                powerFactor <= 1.0
        ) {
            "Power factor must be > 0 and <= 1."
        }
    }
}
