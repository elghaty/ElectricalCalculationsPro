package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import kotlin.math.sqrt

/**
 * Professional voltage-drop mathematical engine.
 *
 * The section-based overload derives conductor resistance from:
 *
 *      R = rho × L / S
 *
 * Manufacturer/catalogue data should be preferred for final design.
 *
 * Catalogue-data overload accepts R and X directly in ohm/km.
 */
object VoltageDropCalculator {

    private const val EPSILON = 1.0e-9

    /*
     * Approximate conductor resistivity at 20 °C.
     *
     * These values are used only when manufacturer R/X data
     * are not supplied through the catalogue-data overload.
     */
    private const val COPPER_RESISTIVITY_OHM_MM2_PER_M = 0.0175
    private const val ALUMINUM_RESISTIVITY_OHM_MM2_PER_M = 0.0282

    /*
     * Typical engineering reactance used only by the legacy
     * section/material overload.
     *
     * Final design should use the manufacturer's X value.
     */
    private const val DEFAULT_AC_REACTANCE_OHM_PER_KM = 0.08

    /**
     * Legacy/public API.
     *
     * Section size is now actually used in the resistance calculation.
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

        val resistivityOhmMm2PerM =
            when (material) {
                ConductorMaterial.Copper ->
                    COPPER_RESISTIVITY_OHM_MM2_PER_M

                ConductorMaterial.Aluminum ->
                    ALUMINUM_RESISTIVITY_OHM_MM2_PER_M
            }

        /*
         * rho is in ohm.mm²/m.
         *
         * Convert the result to ohm/km:
         *
         * R/km = rho × 1000 / S
         */
        val resistanceOhmPerKm =
            resistivityOhmMm2PerM *
                1000.0 /
                sectionMm2

        val reactanceOhmPerKm =
            when (currentType) {
                CurrentType.DirectCurrent ->
                    0.0

                CurrentType.AlternatingSinglePhase,
                CurrentType.AlternatingTwoPhase,
                CurrentType.AlternatingThreePhase ->
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
     * Professional catalogue-data calculation.
     *
     * R and X must be supplied in ohm/km.
     *
     * For final engineering design these values should preferably
     * come from the selected cable manufacturer's catalogue.
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

        /*
         * Voltage-drop loop factors:
         *
         * DC              -> 2
         * Single phase    -> 2
         * Two phase       -> 2
         * Three phase     -> sqrt(3)
         */
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

        /*
         * Input length is metres.
         *
         * R/X are ohm/km.
         */
        val resistanceOhm =
            resistanceOhmPerKm *
                (length / 1000.0)

        val reactanceOhm =
            reactanceOhmPerKm *
                (length / 1000.0)

        /*
         * ΔV = K × I × (R cosφ + X sinφ)
         */
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

    fun voltageDropPercent(
        current: Double,
        length: Double,
        powerFactor: Double,
        currentType: CurrentType,
        voltage: Double,
        resistanceOhmPerKm: Double,
        reactanceOhmPerKm: Double
    ): Double {

        return calculate(
            current = current,
            length = length,
            powerFactor = powerFactor,
            currentType = currentType,
            voltage = voltage,
            resistanceOhmPerKm = resistanceOhmPerKm,
            reactanceOhmPerKm = reactanceOhmPerKm
        ).first
    }

    fun voltageDropVolts(
        current: Double,
        length: Double,
        powerFactor: Double,
        currentType: CurrentType,
        voltage: Double,
        resistanceOhmPerKm: Double,
        reactanceOhmPerKm: Double
    ): Double {

        return calculate(
            current = current,
            length = length,
            powerFactor = powerFactor,
            currentType = currentType,
            voltage = voltage,
            resistanceOhmPerKm = resistanceOhmPerKm,
            reactanceOhmPerKm = reactanceOhmPerKm
        ).second
    }
}
