package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.CurrentType
import kotlin.math.sqrt

/**
 * Electrical load and design-current calculations.
 *
 * Demand/diversity factors are deliberately kept explicit.
 * The selected engineering standard may impose additional rules
 * before these values are finally accepted for equipment selection.
 */
object LoadCalculator {

    private const val EPSILON = 1.0e-9

    fun designCurrent(
        loadWatts: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double {

        require(loadWatts >= 0.0)
        require(voltage > EPSILON)
        require(powerFactor > 0.0)
        require(powerFactor <= 1.0)

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

        require(loadKw >= 0.0)

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

        require(current >= 0.0)
        require(demandFactor in 0.0..1.0)

        return current *
            demandFactor
    }

    fun applyDiversityFactor(
        current: Double,
        diversityFactor: Double
    ): Double {

        require(current >= 0.0)
        require(diversityFactor in 0.0..1.0)

        return current *
            diversityFactor
    }

    fun applyDemandAndDiversity(
        current: Double,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): Double {

        require(current >= 0.0)
        require(demandFactor in 0.0..1.0)
        require(diversityFactor in 0.0..1.0)

        return current *
            demandFactor *
            diversityFactor
    }

    fun loadAfterDemand(
        loadWatts: Double,
        demandFactor: Double
    ): Double {

        require(loadWatts >= 0.0)
        require(demandFactor in 0.0..1.0)

        return loadWatts *
            demandFactor
    }

    fun loadAfterDiversity(
        loadWatts: Double,
        diversityFactor: Double
    ): Double {

        require(loadWatts >= 0.0)
        require(diversityFactor in 0.0..1.0)

        return loadWatts *
            diversityFactor
    }
}
