package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.CurrentType
import kotlin.math.sqrt

/**
 * Fundamental electrical-load calculations.
 *
 * This class contains mathematical relationships only.
 * Code tables, correction factors and equipment catalogs are handled
 * by their dedicated layers.
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
            "Load power cannot be negative."
        }

        require(voltage > EPSILON) {
            "Voltage must be greater than zero."
        }

        require(powerFactor > EPSILON) {
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
                    (voltage * powerFactor)

            CurrentType.AlternatingTwoPhase ->
                loadWatts /
                    (2.0 * voltage * powerFactor)

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
            "Load power cannot be negative."
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

        return current * demandFactor
    }

    /**
     * Historical API retained for compatibility.
     *
     * The value supplied here is treated as a utilization multiplier,
     * not as the conventional diversity factor definition (which is
     * normally >= 1). Do not use this function when a formal diversity
     * factor from a code or design standard is intended.
     */
    fun applyDiversityFactor(
        current: Double,
        diversityFactor: Double
    ): Double {

        require(current >= 0.0) {
            "Current cannot be negative."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity multiplier must be between 0 and 1."
        }

        return current * diversityFactor
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
            "Diversity multiplier must be between 0 and 1."
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

        return loadWatts * demandFactor
    }

    fun loadAfterDiversity(
        loadWatts: Double,
        diversityFactor: Double
    ): Double {

        require(loadWatts >= 0.0) {
            "Load cannot be negative."
        }

        require(diversityFactor in 0.0..1.0) {
            "Diversity multiplier must be between 0 and 1."
        }

        return loadWatts * diversityFactor
    }
}
