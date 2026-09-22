package com.electrical.calculationspro.data.calculators

import kotlin.math.sqrt

/**
 * Transformer preliminary sizing.
 *
 * Final transformer selection will be performed against the
 * manufacturer catalogue after the required kVA, voltages, impedance,
 * vector group and other project requirements are known.
 */
object TransformerSizingCalculator {

    private val standardRatingsKva = listOf(
        25.0,
        50.0,
        63.0,
        100.0,
        160.0,
        200.0,
        250.0,
        315.0,
        400.0,
        500.0,
        630.0,
        800.0,
        1000.0,
        1250.0,
        1600.0,
        2000.0,
        2500.0,
        3150.0,
        4000.0,
        5000.0,
        6300.0
    )

    fun requiredKva(
        activePowerKw: Double,
        powerFactor: Double,
        designMargin: Double = 1.0
    ): Double {

        require(activePowerKw >= 0.0)
        require(powerFactor > 0.0)
        require(powerFactor <= 1.0)
        require(designMargin >= 1.0)

        return (
            activePowerKw /
                powerFactor
            ) *
            designMargin
    }

    fun selectStandardRating(
        requiredKva: Double
    ): Double? {

        require(requiredKva >= 0.0)

        return standardRatingsKva.firstOrNull {
            it >= requiredKva
        }
    }

    fun fullLoadCurrent(
        kva: Double,
        voltage: Double,
        phases: Int = 3
    ): Double {

        require(kva >= 0.0)
        require(voltage > 0.0)

        return when (phases) {

            1 ->
                kva * 1000.0 /
                    voltage

            3 ->
                kva * 1000.0 /
                    (
                        sqrt(3.0) *
                            voltage
                        )

            else ->
                error(
                    "Supported phase systems are 1 or 3."
                )
        }
    }

    fun shortCircuitCurrentFromImpedance(
        ratedKva: Double,
        voltage: Double,
        percentImpedance: Double,
        phases: Int = 3
    ): Double {

        require(ratedKva > 0.0)
        require(voltage > 0.0)
        require(percentImpedance > 0.0)

        val ratedCurrent =
            fullLoadCurrent(
                kva = ratedKva,
                voltage = voltage,
                phases = phases
            )

        return ratedCurrent *
            (100.0 / percentImpedance)
    }

    fun standardRatings(): List<Double> =
        standardRatingsKva
}
