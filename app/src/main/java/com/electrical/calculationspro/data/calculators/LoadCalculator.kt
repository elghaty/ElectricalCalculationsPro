package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.CurrentType
import kotlin.math.sqrt

/**
 * Professional electrical load calculator.
 *
 * Mathematical calculation layer only.
 *
 * Code-specific requirements belong to the Standards layer.
 */
object LoadCalculator {

    private const val EPSILON = 1.0e-9

    fun designCurrent(
        loadWatts: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double {

        require(loadWatts >= 0.0) {
            "Load cannot be negative."
        }

        require(voltage > EPSILON) {
            "Voltage must be greater than zero."
        }

        require(powerFactor > 0.0) {
            "Power factor must be greater than zero."
        }

        require(powerFactor <= 1.0) {
            "Power factor cannot exceed 1.0."
        }

        return when (currentType) {

            CurrentType.DirectCurrent ->
                loadWatts / voltage

            CurrentType.AlternatingSinglePhase ->
                loadWatts /
                    (
                        voltage *
                            powerFactor
                        )

            CurrentType.AlternatingTwoPhase ->
                loadWatts /
                    (
                        2.0 *
                            voltage *
                            powerFactor
                        )

            CurrentType.AlternatingThreePhase ->
                loadWatts /
                    (
                        sqrt(3.0) *
                            voltage *
                            powerFactor
                        )
        }
    }

    fun designCurrentFromKw(
        loadKw: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double {

        require(loadKw >= 0.0) {
            "Load cannot be negative."
        }

        return designCurrent(
            loadWatts = loadKw * 1000.0,
            voltage = voltage,
            powerFactor = powerFactor,
            currentType = currentType
        )
    }

    fun applyDemandFactor(
        current: Double,
        demandFactor: Double
    ): Double {

        require(current >= 0.0) {
            "Current cannot be negative."
        }

        require(demandFactor in 0.0..1.0) {
            "Demand factor must be between 0 and 1."
        }

        return current *
            demandFactor
    }

    fun applyDiversityFactor(
        current: Double,
        diversityFactor: Double
    ): Double {

        require(current >= 0.0) {
            "Current cannot be negative."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity factor must be between 0 and 1."
        }

        return current *
            diversityFactor
    }

    fun applyDemandAndDiversity(
        current: Double,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): Double {

        require(current >= 0.0) {
            "Current cannot be negative."
        }

        require(demandFactor in 0.0..1.0) {
            "Demand factor must be between 0 and 1."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity factor must be between 0 and 1."
        }

        return current *
            demandFactor *
            diversityFactor
    }

    fun loadAfterDemand(
        loadWatts: Double,
        demandFactor: Double
    ): Double {

        require(loadWatts >= 0.0) {
            "Load cannot be negative."
        }

        require(demandFactor in 0.0..1.0) {
            "Demand factor must be between 0 and 1."
        }

        return loadWatts *
            demandFactor
    }

    fun loadAfterDiversity(
        loadWatts: Double,
        diversityFactor: Double
    ): Double {

        require(loadWatts >= 0.0) {
            "Load cannot be negative."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity factor must be between 0 and 1."
        }

        return loadWatts *
            diversityFactor
    }
}
