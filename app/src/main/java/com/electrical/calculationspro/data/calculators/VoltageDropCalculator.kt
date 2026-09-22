package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import kotlin.math.sqrt

/**
 * Professional voltage-drop mathematical engine.
 *
 * This class performs only the electrical calculation.
 *
 * Final design verification should use manufacturer/catalogue
 * R and X values for the selected cable.
 */
object VoltageDropCalculator {

    private const val EPSILON = 1.0e-9

    /**
     * Legacy/public API preserved for UI compatibility.
     *
     * Uses engineering default R/X values when exact manufacturer
     * impedance data are not supplied.
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

        val resistanceOhmPerKm =
            when (material) {
                ConductorMaterial.Copper -> 22.5
                ConductorMaterial.Aluminum -> 36.0
            }

        val reactanceOhmPerKm =
            when (currentType) {
                CurrentType.DirectCurrent -> 0.0
                else -> 0.08
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
     * Professional catalogue-data calculation.
     *
     * R and X are supplied directly from the selected cable
     * manufacturer catalogue in ohm/km.
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
                (length / 1000.0)

        val reactanceOhm =
            reactanceOhmPerKm *
                (length / 1000.0)

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
}
