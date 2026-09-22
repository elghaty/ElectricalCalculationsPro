package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.TransformerSizingResult
import kotlin.math.sqrt

/**
 * Transformer preliminary sizing.
 *
 * Final equipment selection belongs to the manufacturer catalog layer.
 */
object TransformerSizingCalculator {

    private const val EPSILON = 1.0e-9

    private val standardRatingsKva =
        listOf(
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
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double = 1.0
    ): Double {

        require(loadKw >= 0.0) {
            "Load cannot be negative."
        }

        require(
            powerFactor > EPSILON &&
                powerFactor <= 1.0
        ) {
            "Power factor must be > 0 and <= 1."
        }

        require(growthFactor >= 1.0) {
            "Growth factor must be >= 1.0."
        }

        return (
            loadKw /
                powerFactor
            ) *
            growthFactor
    }

    fun selectStandardRating(
        requiredKva: Double
    ): Double {

        require(requiredKva >= 0.0)

        return standardRatingsKva
            .firstOrNull {
                it >= requiredKva
            }
            ?: standardRatingsKva.last()
    }

    fun fullLoadCurrent(
        kva: Double,
        voltage: Double,
        phases: Int = 3
    ): Double {

        require(kva >= 0.0)
        require(voltage > EPSILON)

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
                    "Only single-phase and three-phase systems are supported."
                )
        }
    }

    fun shortCircuitCurrentFromImpedance(
        kva: Double,
        voltage: Double,
        impedancePercent: Double,
        phases: Int = 3
    ): Double {

        require(kva > EPSILON)
        require(voltage > EPSILON)
        require(impedancePercent > EPSILON)

        val ratedCurrent =
            fullLoadCurrent(
                kva = kva,
                voltage = voltage,
                phases = phases
            )

        return ratedCurrent *
            100.0 /
            impedancePercent
    }

    fun calculate(
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double = 1.0,
        voltage: Double,
        phases: Int = 3,
        impedancePercent: Double = 6.0
    ): TransformerSizingResult {

        val required =
            requiredKva(
                loadKw = loadKw,
                powerFactor = powerFactor,
                growthFactor = growthFactor
            )

        val selected =
            selectStandardRating(
                requiredKva = required
            )

        val fullLoadCurrent =
            fullLoadCurrent(
                kva = selected,
                voltage = voltage,
                phases = phases
            )

        val shortCircuitKA =
            shortCircuitCurrentFromImpedance(
                kva = selected,
                voltage = voltage,
                impedancePercent = impedancePercent,
                phases = phases
            ) / 1000.0

        return TransformerSizingResult(
            loadKw = loadKw,
            powerFactor = powerFactor,
            growthFactor = growthFactor,
            requiredKva = required,
            selectedKva = selected,
            fullLoadCurrentA = fullLoadCurrent,
            impedancePercent = impedancePercent,
            shortCircuitCurrentKA = shortCircuitKA,
            valid = true,
            notes = listOf(
                "Required transformer capacity = %.2f kVA"
                    .format(required),
                "Selected standard capacity = %.0f kVA"
                    .format(selected),
                "Full-load current = %.2f A"
                    .format(fullLoadCurrent),
                "Transformer impedance = %.2f %%"
                    .format(impedancePercent),
                "Estimated transformer terminal fault current = %.3f kA"
                    .format(shortCircuitKA)
            )
        )
    }

    fun standardRatings(): List<Double> =
        standardRatingsKva
}
