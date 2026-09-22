package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ShortCircuitResult
import kotlin.math.sqrt

/**
 * Short-circuit calculation engine.
 *
 * The legacy API is preserved for application compatibility.
 *
 * Important:
 * The legacy input model does not contain sufficient information
 * for a fully verified IEC 60909 study. Therefore results produced
 * through this API are explicitly marked preliminary.
 *
 * A verified study requires:
 * - source short-circuit level
 * - source X/R or source sequence impedance
 * - transformer Uk% and X/R where applicable
 * - generator contribution where applicable
 * - motor contribution where applicable
 * - positive/negative/zero sequence data
 * - manufacturer cable R/X data
 * - maximum and minimum fault cases
 * - applicable IEC 60909 correction factors
 */
object ShortCircuitCalculator {

    private const val EPSILON = 1.0e-9

    fun calculate(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 0.0
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

        val resistivityOhmMm2PerM =
            when (material) {

                ConductorMaterial.Copper ->
                    0.018

                ConductorMaterial.Aluminum ->
                    0.029
            }

        val resistancePerMeter =
            resistivityOhmMm2PerM /
                sectionMm2

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
         * Source impedance derived from the supplied source
         * short-circuit current.
         *
         * This is a simplified source representation because
         * the legacy API does not provide source X/R.
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
                totalResistance * totalResistance +
                    totalReactance * totalReactance
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
             * The mathematical calculation exists, but the
             * legacy input set is insufficient for a verified
             * IEC 60909 result.
             */
            valid =
                false,

            notes =
                listOf(
                    "PRELIMINARY RESULT - NOT VERIFIED FOR FINAL DESIGN.",
                    "Source short-circuit current was supplied as %.3f kA."
                        .format(sourceIkKA),
                    "Cable resistance = %.6f Ω."
                        .format(cableResistance),
                    "Cable reactance = %.6f Ω."
                        .format(cableReactance),
                    "Total impedance = %.6f Ω."
                        .format(totalImpedance),
                    "Calculated preliminary fault current = %.3f kA."
                        .format(faultCurrentKA),
                    "Short-circuit level = %.3f MVA."
                        .format(shortCircuitMva),
                    "X/R = %.3f."
                        .format(xrRatio),
                    "Final IEC 60909 verification requires complete source, transformer, generator, motor and sequence-network data.",
                    "Final cable impedance must be taken from the selected manufacturer catalogue.",
                    "Maximum and minimum fault cases are not represented by this legacy API."
                )
        )
    }

    private fun incomplete(
        message: String
    ): ShortCircuitResult =
        ShortCircuitResult(
            valid = false,
            notes = listOf(
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
            notes = listOf(
                "INVALID INPUT.",
                message
            )
        )
}
