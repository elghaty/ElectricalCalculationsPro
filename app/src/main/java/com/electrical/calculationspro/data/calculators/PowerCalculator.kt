package com.electrical.calculationspro.data.calculators

import kotlin.math.sqrt

/**
 * Professional electrical power calculator.
 *
 * All returned power values use the same unit as the input
 * voltage/current combination:
 *
 * V × A = VA
 * W / VA / var relationships are preserved.
 */
object PowerCalculator {

    private const val EPSILON = 1.0e-9

    fun activePower(
        voltage: Double,
        current: Double,
        powerFactor: Double,
        phases: Int
    ): Double {

        require(voltage >= 0.0) {
            "Voltage cannot be negative."
        }

        require(current >= 0.0) {
            "Current cannot be negative."
        }

        require(powerFactor in 0.0..1.0) {
            "Power factor must be between 0 and 1."
        }

        return when (phases) {

            1 ->
                voltage *
                    current *
                    powerFactor

            3 ->
                sqrt(3.0) *
                    voltage *
                    current *
                    powerFactor

            else ->
                throw IllegalArgumentException(
                    "Only 1-phase and 3-phase systems are supported."
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
                voltage *
                    current

            3 ->
                sqrt(3.0) *
                    voltage *
                    current

            else ->
                throw IllegalArgumentException(
                    "Only 1-phase and 3-phase systems are supported."
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
        ) {
            "Active power cannot exceed apparent power."
        }

        return sqrt(
            (
                apparentPower *
                    apparentPower -
                    activePower *
                    activePower
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
        ) {
            "Active power cannot exceed apparent power."
        }

        return (
            activePower /
                apparentPower
            ).coerceIn(0.0, 1.0)
    }

    fun kvaFromKw(
        kw: Double,
        powerFactor: Double
    ): Double {

        require(kw >= 0.0)
        require(powerFactor > EPSILON)
        require(powerFactor <= 1.0)

        return kw /
            powerFactor
    }

    fun kvarFromKw(
        kw: Double,
        powerFactor: Double
    ): Double {

        require(kw >= 0.0)
        require(powerFactor > EPSILON)
        require(powerFactor <= 1.0)

        val sinPhi =
            sqrt(
                (
                    1.0 -
                        powerFactor *
                        powerFactor
                    ).coerceAtLeast(0.0)
            )

        return kw *
            sinPhi /
            powerFactor
    }

    fun kwFromKva(
        kva: Double,
        powerFactor: Double
    ): Double {

        require(kva >= 0.0)
        require(powerFactor in 0.0..1.0)

        return kva *
            powerFactor
    }
}
