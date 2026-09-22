package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ShortCircuitResult
import kotlin.math.sqrt

/**
 * Preliminary short-circuit calculation engine.
 *
 * This engine keeps the source fault level explicit.
 * A complete IEC 60909 implementation will later be provided as
 * a dedicated standard engine, including c-factors, source types,
 * transformer impedance, generator contribution and motor contribution.
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

        require(voltage > EPSILON)
        require(length >= 0.0)
        require(sectionMm2 > EPSILON)
        require(sourceIkKA > EPSILON)

        val resistivity = when (material) {

            ConductorMaterial.Copper ->
                0.018

            ConductorMaterial.Aluminum ->
                0.029
        }

        val resistancePerMeter =
            resistivity /
                sectionMm2

        val reactancePerMeter =
            if (
                currentType ==
                CurrentType.DirectCurrent
            ) {
                0.0
            } else {
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
                    voltage /
                        sqrt(3.0)

                CurrentType.DirectCurrent,
                CurrentType.AlternatingSinglePhase,
                CurrentType.AlternatingTwoPhase ->
                    voltage
            }

        val sourceImpedance =
            faultVoltage /
                (
                    sourceIkKA *
                        1000.0
                    )

        val totalResistance =
            sourceImpedance +
                cableResistance

        val totalImpedance =
            sqrt(
                totalResistance *
                    totalResistance +
                    cableReactance *
                    cableReactance
            ).coerceAtLeast(EPSILON)

        val faultCurrent =
            faultVoltage /
                totalImpedance

        val faultCurrentKA =
            faultCurrent /
                1000.0

        val i2t =
            faultCurrent *
                faultCurrent *
                0.1

        return ShortCircuitResult(
            ikAmps = faultCurrent,
            ikKA = faultCurrentKA,
            cableImpedance = totalImpedance,
            sourceImpedance = sourceImpedance,
            i2t = i2t,
            notes = listOf(
                "Ik = %.2f kA"
                    .format(faultCurrentKA),

                "Total impedance = %.6f Ω"
                    .format(totalImpedance),

                "Source impedance = %.6f Ω"
                    .format(sourceImpedance),

                "Cable resistance = %.6f Ω"
                    .format(cableResistance),

                "Cable reactance = %.6f Ω"
                    .format(cableReactance),

                "I²t at 0.10 s = %.0f A²s"
                    .format(i2t)
            )
        )
    }
}
