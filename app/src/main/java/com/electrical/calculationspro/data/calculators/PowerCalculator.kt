package com.electrical.calculationspro.data.calculators

import kotlin.math.sqrt

/**
 * Professional electrical power calculations.
 *
 * This class contains mathematical power relationships only.
 * Code-specific requirements belong to the Standards layer.
 */
object PowerCalculator {

    private const val EPSILON = 1.0e-9

    fun activePower(
        voltage: Double,
        current: Double,
        powerFactor: Double,
        phases: Int
    ): Double {

        require(voltage >= 0.0)
        require(current >= 0.0)
        require(powerFactor in 0.0..1.0)

        return when (phases) {
            1 ->
                voltage * current * powerFactor

            3 ->
                sqrt(3.0) *
                    voltage *
                    current *
                    powerFactor

            else ->
                throw IllegalArgumentException(
                    "Supported phase systems are 1 or 3."
                )
        }
    }

    fun apparentPower(
        voltage: Double,
        current: Double,
        phases: Int
    ): Double {

        require(voltage >= 0.0)
        require(current >= 0.0)

        return when (phases) {
            1 ->
                voltage * current

            3 ->
                sqrt(3.0) *
                    voltage *
                    current

            else ->
                throw IllegalArgumentException(
                    "Supported phase systems are 1 or 3."
                )
        }
    }

    fun reactivePower(
        activePower: Double,
        apparentPower: Double
    ): Double {

        require(activePower >= 0.0)
        require(apparentPower >= 0.0)

        require(
            activePower <=
                apparentPower + EPSILON
        )

        return sqrt(
            (
                apparentPower * apparentPower -
                    activePower * activePower
                ).coerceAtLeast(0.0)
        )
    }

    fun powerFactor(
        activePower: Double,
        apparentPower: Double
    ): Double {

        require(activePower >= 0.0)
        require(apparentPower >= 0.0)

        if (apparentPower <= EPSILON) {
            return 0.0
        }

        require(
            activePower <=
                apparentPower + EPSILON
        )

        return (
            activePower / apparentPower
            ).coerceIn(0.0, 1.0)
    }

    fun kvaFromKw(
        kw: Double,
        powerFactor: Double
    ): Double {

        require(kw >= 0.0)
        require(powerFactor > EPSILON)
        require(powerFactor <= 1.0)

        return kw / powerFactor
    }

    fun kvarFromKw(
        kw: Double,
        powerFactor: Double
    ): Double {

        require(kw >= 0.0)
        require(powerFactor > EPSILON)
        require(powerFactor <= 1.0)

        val angleSin = sqrt(
            (
                1.0 -
                    powerFactor * powerFactor
                ).coerceAtLeast(0.0)
        )

        return kw *
            angleSin /
            powerFactor
    }

    fun kwFromKva(
        kva: Double,
        powerFactor: Double
    ): Double {

        require(kva >= 0.0)
        require(powerFactor in 0.0..1.0)

        return kva * powerFactor
    }
}
