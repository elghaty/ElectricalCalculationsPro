package com.electrical.calculationspro.data.calculators

/**
 * Protective-device preliminary rating selection.
 *
 * This class selects a nominal rating only.
 * Manufacturer/model selection belongs to EquipmentCatalog.
 *
 * Coordination principle:
 *
 *      Ib <= In <= Iz
 *
 * Breaking-capacity verification is deliberately separated and will
 * use the calculated prospective short-circuit current and real
 * manufacturer catalogue data.
 */
object BreakerSelectionCalculator {

    private val standardRatings = listOf(
        2.0,
        4.0,
        6.0,
        10.0,
        13.0,
        16.0,
        20.0,
        25.0,
        32.0,
        40.0,
        50.0,
        63.0,
        80.0,
        100.0,
        125.0,
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
        3200.0,
        4000.0,
        5000.0,
        6300.0
    )

    fun selectRating(
        designCurrentA: Double,
        cableAmpacityA: Double
    ): Double? {

        require(designCurrentA >= 0.0)
        require(cableAmpacityA >= 0.0)

        return standardRatings.firstOrNull { rating ->
            rating >= designCurrentA &&
                rating <= cableAmpacityA
        }
    }

    fun satisfiesCoordination(
        designCurrentA: Double,
        breakerRatingA: Double,
        cableAmpacityA: Double
    ): Boolean {

        require(designCurrentA >= 0.0)
        require(breakerRatingA >= 0.0)
        require(cableAmpacityA >= 0.0)

        return designCurrentA <=
            breakerRatingA &&
            breakerRatingA <=
            cableAmpacityA
    }

    fun isBreakingCapacityAdequate(
        prospectiveFaultCurrentKA: Double,
        breakerBreakingCapacityKA: Double
    ): Boolean {

        require(prospectiveFaultCurrentKA >= 0.0)
        require(breakerBreakingCapacityKA >= 0.0)

        return breakerBreakingCapacityKA >=
            prospectiveFaultCurrentKA
    }

    fun availableRatings(): List<Double> =
        standardRatings
}
