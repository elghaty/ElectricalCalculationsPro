package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import kotlin.math.sqrt

/**
 * Voltage-drop mathematical engine.
 *
 * This class does not select cables.
 * Cable selection and manufacturer data belong to the catalog layer.
 *
 * The calculation is preliminary when manufacturer R/X data are
 * not available for the selected cable.
 */
object VoltageDropCalculator {

    private const val EPSILON = 1.0e-9

    fun calculate(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> {

        require(current >= 0.0) {
            "Current cannot be negative."
        }

        require(length >= 0.0) {
            "Length cannot be negative."
        }

        require(sectionMm2 > EPSILON) {
            "Conductor section must be greater than zero."
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

        val resistivity =
            when (material) {

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
}
