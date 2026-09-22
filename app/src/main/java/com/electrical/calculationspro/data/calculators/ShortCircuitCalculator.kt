package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ShortCircuitResult
import kotlin.math.sqrt

/**
 * Preliminary network short-circuit calculator.
 *
 * This calculator is deliberately separated from the UI.
 *
 * The complete IEC 60909 network implementation will later handle:
 * - c factor
 * - maximum/minimum fault cases
 * - transformer impedance
 * - generator contribution
 * - motor contribution
 * - positive/negative/zero sequence networks
 * - correction factors
 *
 * Until those datasets are implemented, this calculator is explicitly
 * marked as a preliminary engineering calculation.
 */
object ShortCircuitCalculator {

    private const val EPSILON = 1.0e-9

    fun calculate(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 50.0
    ): ShortCircuitResult {

        require(voltage > EPSILON) {
            "Voltage must be greater than zero."
        }

        require(length >= 0.0) {
            "Cable length cannot be negative."
        }

        require(sectionMm2 > EPSILON) {
            "Cable section must be greater than zero."
        }

        require(sourceIkKA > EPSILON) {
            "Source short-circuit current must be greater than zero."
        }

        val resistivityOhmMm2PerM =
            when (material) {
                ConductorMaterial.Copper -> 0.018
                ConductorMaterial.Aluminum -> 0.029
            }

        val resistancePerMeter =
            resistivityOhmMm2PerM / sectionMm2

        val reactancePerMeter =
            when (currentType) {
                CurrentType.DirectCurrent -> 0.0
                else -> 0.08 / 1000.0
            }

        val loopFactor =
            when (currentType) {
                CurrentType.DirectCurrent -> 2.0
                CurrentType.AlternatingSinglePhase -> 2.0
                CurrentType.AlternatingTwoPhase -> 2.0
                CurrentType.AlternatingThreePhase -> 1.0
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
            ).coerceAtLeast(EPSILON)

        val faultCurrentA =
            faultVoltage /
                totalImpedance

        val faultCurrentKA =
            faultCurrentA / 1000.0

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
            sourceShortCircuitCurrentKA = sourceIkKA,
            cableResistanceOhm = cableResistance,
            cableReactanceOhm = cableReactance,
            totalResistanceOhm = totalResistance,
            totalReactanceOhm = totalReactance,
            totalImpedanceOhm = totalImpedance,
            shortCircuitCurrentKA = faultCurrentKA,
            shortCircuitMva = shortCircuitMva,
            xrRatio = xrRatio,
            valid = true,
            notes = listOf(
                "Preliminary short-circuit calculation.",
                "Source Ik = %.3f kA".format(sourceIkKA),
                "Cable R = %.6f Ω".format(cableResistance),
                "Cable X = %.6f Ω".format(cableReactance),
                "Total Z = %.6f Ω".format(totalImpedance),
                "Fault current Ik = %.3f kA".format(faultCurrentKA),
                "Short-circuit level = %.3f MVA".format(shortCircuitMva),
                "X/R = %.3f".format(xrRatio),
                "Full IEC 60909 network modelling is not yet enabled."
            )
        )
    }
}
