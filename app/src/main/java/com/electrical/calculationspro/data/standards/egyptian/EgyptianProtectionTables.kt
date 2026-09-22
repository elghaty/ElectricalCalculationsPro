package com.electrical.calculationspro.data.standards.egyptian

/**
 * Egyptian protection-device engineering layer.
 *
 * Device catalogue ratings are deliberately separated from code rules.
 * Manufacturer selection belongs to the catalog package.
 */
object EgyptianProtectionTables {

    data class ProtectionRule(
        val minimumRatingA: Double,
        val maximumRatingA: Double,
        val notes: List<String> = emptyList()
    )

    data class ProtectionAssessment(
        val valid: Boolean,
        val notes: List<String>
    )

    /**
     * Nominal ratings commonly used as selectable engineering values.
     *
     * These are NOT manufacturer catalogue data.
     */
    val nominalRatingsA: List<Double> = listOf(
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

    fun nextStandardRating(
        requiredCurrentA: Double
    ): Double? {
        if (requiredCurrentA <= 0.0) {
            return null
        }

        return nominalRatingsA.firstOrNull {
            it >= requiredCurrentA
        }
    }

    fun assess(
        designCurrentA: Double,
        cableAmpacityA: Double,
        breakerRatingA: Double,
        breakingCapacityKA: Double,
        prospectiveFaultKA: Double
    ): ProtectionAssessment {

        val notes = mutableListOf<String>()
        var valid = true

        if (designCurrentA <= 0.0) {
            notes += "Design current must be greater than zero."
            valid = false
        }

        if (cableAmpacityA <= 0.0) {
            notes += "Cable ampacity must be greater than zero."
            valid = false
        }

        if (breakerRatingA <= 0.0) {
            notes += "Breaker rating must be greater than zero."
            valid = false
        }

        if (breakerRatingA < designCurrentA) {
            notes += "Selected protective-device rating is below the design current."
            valid = false
        }

        if (cableAmpacityA > 0.0 &&
            breakerRatingA > cableAmpacityA
        ) {
            notes += "Protective-device rating exceeds cable ampacity."
            valid = false
        }

        if (prospectiveFaultKA > 0.0 &&
            breakingCapacityKA < prospectiveFaultKA
        ) {
            notes += "Breaker breaking capacity is below prospective short-circuit current."
            valid = false
        }

        return ProtectionAssessment(
            valid = valid,
            notes = notes
        )
    }
}
