
package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.standards.CodeRuleRegistry
import com.electrical.calculationspro.data.standards.EngineeringRuleBook
import kotlin.math.sqrt

/**
 * Professional voltage-drop calculation and verification.
 *
 * Project criteria are not treated as verified code limits.
 * Legacy calculation signatures are retained for compatibility.
 */
object VoltageDropCalculator {

    private const val EPSILON = 1.0e-9
    private const val COPPER_RESISTIVITY_OHM_MM2_PER_M = 0.0175
    private const val ALUMINUM_RESISTIVITY_OHM_MM2_PER_M = 0.0282
    private const val DEFAULT_AC_REACTANCE_OHM_PER_KM = 0.08

    fun calculate(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> {
        validateCommonInput(current, length, powerFactor, voltage)
        require(sectionMm2 > EPSILON) {
            "Conductor section must be greater than zero."
        }

        val resistivity = when (material) {
            ConductorMaterial.Copper -> COPPER_RESISTIVITY_OHM_MM2_PER_M
            ConductorMaterial.Aluminum -> ALUMINUM_RESISTIVITY_OHM_MM2_PER_M
        }

        val resistanceOhmPerKm = resistivity * 1000.0 / sectionMm2
        val reactanceOhmPerKm = when (currentType) {
            CurrentType.DirectCurrent -> 0.0
            else -> DEFAULT_AC_REACTANCE_OHM_PER_KM
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
        validateCommonInput(current, length, powerFactor, voltage)
        require(sectionMm2 > EPSILON) {
            "Conductor section must be greater than zero."
        }

        val rule = EngineeringRuleBook.resolve(
            standard = standard,
            domain = CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
        )
        require(rule.references.isNotEmpty()) {
            "No voltage-drop engineering rule is registered for $standard."
        }

        val resistivity = when (material) {
            ConductorMaterial.Copper -> COPPER_RESISTIVITY_OHM_MM2_PER_M
            ConductorMaterial.Aluminum -> ALUMINUM_RESISTIVITY_OHM_MM2_PER_M
        }

        val resistanceOhmPerKm = resistivity * 1000.0 / sectionMm2
        val reactanceOhmPerKm = when (currentType) {
            CurrentType.DirectCurrent -> 0.0
            else -> DEFAULT_AC_REACTANCE_OHM_PER_KM
        }

        val result = calculate(
            current = current,
            length = length,
            powerFactor = powerFactor,
            currentType = currentType,
            voltage = voltage,
            resistanceOhmPerKm = resistanceOhmPerKm,
            reactanceOhmPerKm = reactanceOhmPerKm
        )

        // Resolve the code limit for compatibility; this Pair API cannot
        // return verification metadata. Use calculateCodeDriven() for that.
        val codeLimit = CodeEngineFactory.get(standard)
            .maximumVoltageDropPercent(circuitCategory)

        @Suppress("UNUSED_VARIABLE")
        val effectiveLimit = when {
            maxVoltageDropPercent != null && codeLimit > EPSILON ->
                minOf(maxVoltageDropPercent, codeLimit)
            maxVoltageDropPercent != null -> maxVoltageDropPercent
            codeLimit > EPSILON -> codeLimit
            else -> null
        }

        return result
    }

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
        validateCommonInput(current, length, powerFactor, voltage)
        require(sectionMm2 > EPSILON) {
            "Conductor section must be greater than zero."
        }

        if (maxVoltageDropPercent != null) {
            require(maxVoltageDropPercent.isFinite() && maxVoltageDropPercent > 0.0) {
                "Maximum voltage-drop percentage must be finite and greater than zero."
            }
        }
        if (resistanceOhmPerKm != null) {
            require(resistanceOhmPerKm.isFinite() && resistanceOhmPerKm >= 0.0) {
                "Cable resistance must be finite and cannot be negative."
            }
        }
        if (reactanceOhmPerKm != null) {
            require(reactanceOhmPerKm.isFinite() && reactanceOhmPerKm >= 0.0) {
                "Cable reactance must be finite and cannot be negative."
            }
        }

        val rule = EngineeringRuleBook.requireReference(
            standard = standard,
            domain = CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
        )
        val engine = CodeEngineFactory.get(standard)

        val resistance = resistanceOhmPerKm ?: when (material) {
            ConductorMaterial.Copper ->
                COPPER_RESISTIVITY_OHM_MM2_PER_M * 1000.0 / sectionMm2
            ConductorMaterial.Aluminum ->
                ALUMINUM_RESISTIVITY_OHM_MM2_PER_M * 1000.0 / sectionMm2
        }

        val reactance = reactanceOhmPerKm ?: when (currentType) {
            CurrentType.DirectCurrent -> 0.0
            else -> DEFAULT_AC_REACTANCE_OHM_PER_KM
        }

        val calculation = calculate(
            current = current,
            length = length,
            powerFactor = powerFactor,
            currentType = currentType,
            voltage = voltage,
            resistanceOhmPerKm = resistance,
            reactanceOhmPerKm = reactance
        )

        val codeLimit = engine.maximumVoltageDropPercent(circuitCategory)
        val verifiedCodeLimit = codeLimit.takeIf {
            it.isFinite() && it > EPSILON
        }
        val projectLimit = maxVoltageDropPercent

        val effectiveLimit = when {
            verifiedCodeLimit != null && projectLimit != null ->
                minOf(verifiedCodeLimit, projectLimit)
            verifiedCodeLimit != null -> verifiedCodeLimit
            projectLimit != null -> projectLimit
            else -> null
        }

        val withinLimit = effectiveLimit != null &&
            calculation.first <= effectiveLimit + EPSILON

        // A user-entered project limit is never evidence of a code limit.
        val codeVerified = rule.verified && verifiedCodeLimit != null

        val references = EngineeringRuleBook.referenceIds(
            standard = standard,
            domain = CodeRuleRegistry.RuleDomain.VOLTAGE_DROP
        )

        val notes = buildList {
            add("Standard: ${engine.codeName}")
            add("Code revision: ${engine.codeRevision}")
            add("Rule domain: VOLTAGE_DROP")
            add("Circuit category: $circuitCategory")
            add(
                "Calculated voltage drop = %.3f V (%.3f %%)"
                    .format(calculation.second, calculation.first)
            )

            if (verifiedCodeLimit != null) {
                add(
                    "Verified code voltage-drop limit = %.3f %%"
                        .format(verifiedCodeLimit)
                )
            } else {
                add(
                    "No verified numerical voltage-drop limit is available for this code/category."
                )
            }

            if (projectLimit != null) {
                add("Project design criterion = %.3f %%".format(projectLimit))
            }

            if (effectiveLimit != null) {
                add("Effective assessment limit = %.3f %%".format(effectiveLimit))
                add(
                    if (withinLimit) {
                        "Voltage-drop assessment against available limit: PASS"
                    } else {
                        "Voltage-drop assessment against available limit: FAIL"
                    }
                )
            } else {
                add(
                    "Voltage-drop compliance cannot be assessed because neither a verified code limit nor a project criterion is available."
                )
            }

            add(
                if (codeVerified) {
                    "Code-limit verification status: VERIFIED"
                } else {
                    "Code-limit verification status: NOT VERIFIED; a project criterion cannot substitute for a verified code limit."
                }
            )

            add(
                if (resistanceOhmPerKm != null) {
                    "Cable resistance taken from manufacturer/catalogue data."
                } else {
                    "Cable resistance derived from conductor material and section; final design should prefer manufacturer catalogue data."
                }
            )
            add(
                if (reactanceOhmPerKm != null) {
                    "Cable reactance taken from manufacturer/catalogue data."
                } else {
                    "Cable reactance uses an engineering fallback; final design should prefer manufacturer catalogue data."
                }
            )

            if (!rule.verified) add(rule.message)

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
            receivingEndVoltageV = voltage - calculation.second,
            allowedVoltageDropPercent = effectiveLimit,
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
     * Catalogue-data calculation. R and X are in ohm/km.
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
        validateCommonInput(current, length, powerFactor, voltage)
        require(resistanceOhmPerKm.isFinite() && resistanceOhmPerKm >= 0.0) {
            "Cable resistance must be finite and cannot be negative."
        }
        require(reactanceOhmPerKm.isFinite() && reactanceOhmPerKm >= 0.0) {
            "Cable reactance must be finite and cannot be negative."
        }

        val sinPhi = sqrt((1.0 - powerFactor * powerFactor).coerceAtLeast(0.0))
        val loopFactor = when (currentType) {
            CurrentType.DirectCurrent -> 2.0
            CurrentType.AlternatingSinglePhase -> 2.0
            CurrentType.AlternatingTwoPhase -> 2.0
            CurrentType.AlternatingThreePhase -> sqrt(3.0)
        }

        val resistanceOhm = resistanceOhmPerKm * length / 1000.0
        val reactanceOhm = reactanceOhmPerKm * length / 1000.0
        val voltageDropVolts = loopFactor * current *
            (resistanceOhm * powerFactor + reactanceOhm * sinPhi)
        val voltageDropPercent = voltageDropVolts / voltage * 100.0

        return voltageDropPercent to voltageDropVolts
    }

    fun voltageDropPercent(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double = calculate(
        current, length, sectionMm2, powerFactor, currentType, material, voltage
    ).first

    fun voltageDropVolts(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double = calculate(
        current, length, sectionMm2, powerFactor, currentType, material, voltage
    ).second

    fun voltageDropPercent(
        current: Double,
        length: Double,
        powerFactor: Double,
        currentType: CurrentType,
        voltage: Double,
        resistanceOhmPerKm: Double,
        reactanceOhmPerKm: Double
    ): Double = calculate(
        current, length, powerFactor, currentType, voltage,
        resistanceOhmPerKm, reactanceOhmPerKm
    ).first

    fun voltageDropVolts(
        current: Double,
        length: Double,
        powerFactor: Double,
        currentType: CurrentType,
        voltage: Double,
        resistanceOhmPerKm: Double,
        reactanceOhmPerKm: Double
    ): Double = calculate(
        current, length, powerFactor, currentType, voltage,
        resistanceOhmPerKm, reactanceOhmPerKm
    ).second

    private fun validateCommonInput(
        current: Double,
        length: Double,
        powerFactor: Double,
        voltage: Double
    ) {
        require(current.isFinite() && current >= 0.0) {
            "Current must be finite and cannot be negative."
        }
        require(length.isFinite() && length >= 0.0) {
            "Length must be finite and cannot be negative."
        }
        require(voltage.isFinite() && voltage > EPSILON) {
            "Voltage must be finite and greater than zero."
        }
        require(powerFactor.isFinite() && powerFactor > EPSILON && powerFactor <= 1.0) {
            "Power factor must be finite, greater than 0 and not greater than 1."
        }
    }
}
