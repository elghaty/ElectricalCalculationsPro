package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import kotlin.math.sqrt

/**
 * Voltage-drop calculation engine.
 *
 * IMPORTANT:
 * The mathematical engine is independent from the selected standard.
 * Standard-specific permissible voltage-drop limits are handled by
 * the Standards layer.
 *
 * Resistance values here represent engineering calculation defaults.
 * Final production calculations should use conductor/manufacturer
 * catalogue electrical characteristics where available.
 */
object VoltageDropCalculator {

    private const val EPSILON = 1.0e-9

    /**
     * Returns Pair(
     *     voltageDropPercent,
     *     voltageDropVolts
     * )
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

        require(current >= 0.0)
        require(length >= 0.0)
        require(sectionMm2 > EPSILON)
        require(voltage > EPSILON)
        require(powerFactor > 0.0)
        require(powerFactor <= 1.0)

        val resistivity = when (material) {

            ConductorMaterial.Copper ->
                0.0225

            ConductorMaterial.Aluminum ->
                0.0360
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

        val sinPhi =
            sqrt(
                (
                    1.0 -
                        powerFactor *
                        powerFactor
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

        val voltageDropVolts =
            loopFactor *
                current *
                length *
                (
                    resistancePerMeter *
                        powerFactor +
                        reactancePerMeter *
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
    ): Double {

        return calculate(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        ).first
    }

    fun voltageDropVolts(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double {

        return calculate(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        ).second
    }
}
