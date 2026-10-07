package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ShortCircuitResult
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.standards.CodeRuleRegistry
import com.electrical.calculationspro.data.standards.EngineeringRuleBook
import kotlin.math.sqrt

/**
 * PROFESSIONAL SHORT-CIRCUIT ENGINE
 *
 * Current legacy model represents a preliminary network model.
 *
 * The application therefore distinguishes:
 *
 *     Mathematical result
 *          from
 *     Code-verified fault study
 *
 * The legacy API remains compatible.
 *
 * The code-driven API records IEC 60909 / applicable-code traceability
 * and never claims a complete fault study when required network data
 * are missing.
 */
object ShortCircuitCalculator {

    private const val EPSILON = 1.0e-9

    /**
     * Legacy API.
     *
     * Default source Ik is intentionally zero.
     * Therefore the caller must explicitly provide source fault level.
     */
    fun calculate(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 0.0
    ): ShortCircuitResult =
        calculateInternal(
            voltage = voltage,
            length = length,
            sectionMm2 = sectionMm2,
            material = material,
            currentType = currentType,
            sourceIkKA = sourceIkKA,
            standard = null
        )

    /**
     * CODE-DRIVEN SHORT-CIRCUIT API.
     *
     * Standard is an engineering input, not just metadata.
     */
    fun calculate(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double,
        standard: Standard
    ): ShortCircuitResult {

        val rule =
            EngineeringRuleBook.requireReference(
                standard = standard,
                domain =
                    CodeRuleRegistry.RuleDomain.SHORT_CIRCUIT
            )

        val result =
            calculateInternal(
                voltage = voltage,
                length = length,
                sectionMm2 = sectionMm2,
                material = material,
                currentType = currentType,
                sourceIkKA = sourceIkKA,
                standard = standard
            )

        val engine =
            CodeEngineFactory.get(standard)

        return result.copy(
            valid =
                false,
            notes =
                result.notes +
                    listOf(
                        "Code: ${engine.codeName}",
                        "Code revision: ${engine.codeRevision}",
                        "Rule domain: SHORT_CIRCUIT",
                        "Code reference verification = ${rule.verified}",
                        "This legacy network model is not sufficient for a complete IEC 60909 final fault study.",
                        "Final design requires complete source/network impedance, transformer/generator/motor contribution and applicable maximum/minimum fault cases."
                    ) +
                    EngineeringRuleBook.auditTrail(
                        standard = standard,
                        domain =
                            CodeRuleRegistry.RuleDomain.SHORT_CIRCUIT
                    )
        )
    }

    /**
     * Professional result carrying explicit code state.
     */
    data class CodeDrivenResult(
        val result: ShortCircuitResult,
        val standard: Standard,
        val codeName: String,
        val codeRevision: String,
        val codeVerified: Boolean,
        val ruleReferences: List<String>,
        val notes: List<String>
    )

    fun calculateCodeDriven(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double,
        standard: Standard = Standard.IEC
    ): CodeDrivenResult {

        val rule =
            EngineeringRuleBook.requireReference(
                standard = standard,
                domain =
                    CodeRuleRegistry.RuleDomain.SHORT_CIRCUIT
            )

        val engine =
            CodeEngineFactory.get(standard)

        val result =
            calculate(
                voltage = voltage,
                length = length,
                sectionMm2 = sectionMm2,
                material = material,
                currentType = currentType,
                sourceIkKA = sourceIkKA,
                standard = standard
            )

        val references =
            EngineeringRuleBook.referenceIds(
                standard = standard,
                domain =
                    CodeRuleRegistry.RuleDomain.SHORT_CIRCUIT
            )

        val notes =
            buildList {
                add("Standard: ${engine.codeName}")
                add("Code revision: ${engine.codeRevision}")
                add("Rule domain: SHORT_CIRCUIT")

                addAll(result.notes)

                if (rule.verified) {
                    add(
                        "Controlled short-circuit reference is registered."
                    )
                } else {
                    add(
                        "Short-circuit numerical/design dataset is incomplete; result remains preliminary."
                    )
                }

                add(
                    "Final cable impedance should come from the selected manufacturer catalogue."
                )

                add(
                    "Final professional study must distinguish maximum and minimum fault cases."
                )

                addAll(
                    EngineeringRuleBook.auditTrail(
                        standard = standard,
                        domain =
                            CodeRuleRegistry.RuleDomain.SHORT_CIRCUIT
                    )
                )
            }

        return CodeDrivenResult(
            result =
                result.copy(
                    valid = false,
                    notes = notes
                ),
            standard = standard,
            codeName = engine.codeName,
            codeRevision = engine.codeRevision,
            codeVerified = false,
            ruleReferences = references,
            notes = notes
        )
    }

    private fun calculateInternal(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double,
        standard: Standard?
    ): ShortCircuitResult {

        if (voltage <= EPSILON) {
            return invalid(
                "System voltage must be greater than zero."
            )
        }

        if (length < 0.0) {
            return invalid(
                "Cable length cannot be negative."
            )
        }

        if (sectionMm2 <= EPSILON) {
            return invalid(
                "Cable section must be greater than zero."
            )
        }

        if (sourceIkKA <= EPSILON) {
            return incomplete(
                "A verified source short-circuit level is required."
            )
        }

        val resistivity =
            when (material) {
                ConductorMaterial.Copper ->
                    0.018

                ConductorMaterial.Aluminum ->
                    0.029
            }

        val resistancePerMeter =
            resistivity / sectionMm2

        val reactancePerMeter =
            when (currentType) {
                CurrentType.DirectCurrent ->
                    0.0

                else ->
                    0.08 / 1000.0
            }

        val loopFactor =
            when (currentType) {
                CurrentType.DirectCurrent ->
                    2.0

                CurrentType.AlternatingSinglePhase ->
                    2.0

                CurrentType.AlternatingTwoPhase ->
                    2.0

                CurrentType.AlternatingThreePhase ->
                    1.0
            }

        val cableResistance =
            resistancePerMeter *
                length *
                loopFactor

        val cableReactance =
            reactancePerMeter *
                length *
                loopFactor

        val faultVoltage =
            when (currentType) {
                CurrentType.AlternatingThreePhase ->
                    voltage / sqrt(3.0)

                else ->
                    voltage
            }

        /*
         * Simplified source impedance representation.
         *
         * This is intentionally NOT presented as a complete
         * IEC 60909 network model.
         */
        val sourceImpedance =
            faultVoltage /
                (sourceIkKA * 1000.0)

        val totalResistance =
            sourceImpedance +
                cableResistance

        val totalReactance =
            cableReactance

        val totalImpedance =
            sqrt(
                totalResistance *
                    totalResistance +
                    totalReactance *
                    totalReactance
            )

        if (totalImpedance <= EPSILON) {
            return invalid(
                "Total network impedance must be greater than zero."
            )
        }

        val faultCurrentA =
            faultVoltage /
                totalImpedance

        val faultCurrentKA =
            faultCurrentA /
                1000.0

        val shortCircuitMva =
            when (currentType) {
                CurrentType.AlternatingThreePhase ->
                    sqrt(3.0) *
                        voltage *
                        faultCurrentA /
                        1_000_000.0

                else ->
                    voltage *
                        faultCurrentA /
                        1_000_000.0
            }

        val xrRatio =
            if (totalResistance > EPSILON) {
                totalReactance /
                    totalResistance
            } else {
                0.0
            }

        return ShortCircuitResult(
            sourceShortCircuitCurrentKA =
                sourceIkKA,

            cableResistanceOhm =
                cableResistance,

            cableReactanceOhm =
                cableReactance,

            totalResistanceOhm =
                totalResistance,

            totalReactanceOhm =
                totalReactance,

            totalImpedanceOhm =
                totalImpedance,

            shortCircuitCurrentKA =
                faultCurrentKA,

            shortCircuitMva =
                shortCircuitMva,

            xrRatio =
                xrRatio,

            /*
             * Mathematical result only.
             *
             * The current legacy input model cannot establish
             * complete code compliance.
             */
            valid = false,

            notes =
                buildList {
                    add(
                        "PRELIMINARY SHORT-CIRCUIT RESULT - NOT VERIFIED FOR FINAL DESIGN."
                    )

                    add(
                        "Source short-circuit current = %.3f kA."
                            .format(sourceIkKA)
                    )

                    add(
                        "Cable resistance = %.6f Ω."
                            .format(cableResistance)
                    )

                    add(
                        "Cable reactance = %.6f Ω."
                            .format(cableReactance)
                    )

                    add(
                        "Total impedance = %.6f Ω."
                            .format(totalImpedance)
                    )

                    add(
                        "Calculated preliminary fault current = %.3f kA."
                            .format(faultCurrentKA)
                    )

                    add(
                        "Short-circuit level = %.3f MVA."
                            .format(shortCircuitMva)
                    )

                    add(
                        "X/R = %.3f."
                            .format(xrRatio)
                    )

                    add(
                        "Final cable impedance should preferably be taken from the selected manufacturer catalogue."
                    )

                    add(
                        "The current API does not contain complete source X/R, transformer, generator, motor and sequence-network data."
                    )
                }
        )
    }

    private fun incomplete(
        message: String
    ): ShortCircuitResult =
        ShortCircuitResult(
            valid = false,
            notes =
                listOf(
                    "DATA INCOMPLETE.",
                    message,
                    "No verified short-circuit result is available."
                )
        )

    private fun invalid(
        message: String
    ): ShortCircuitResult =
        ShortCircuitResult(
            valid = false,
            notes =
                listOf(
                    "INVALID INPUT.",
                    message
                )
        )
}
