package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.BreakerSelectionResult
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.CodeEngineFactory

/**
 * Protective-device preliminary selection.
 *
 * Engineering rule:
 *
 *      Ib <= In <= Iz
 *
 * Manufacturer/model selection is performed by the equipment catalog
 * layer after the required electrical ratings are known.
 */
object BreakerSelectionCalculator {

    private const val EPSILON = 1.0e-9

    fun selectRating(
        designCurrentA: Double,
        cableAmpacityA: Double,
        standard: Standard = Standard.IEC
    ): Double {

        require(designCurrentA >= 0.0) {
            "Design current cannot be negative."
        }

        require(cableAmpacityA >= 0.0) {
            "Cable ampacity cannot be negative."
        }

        val ratings =
            CodeEngineFactory
                .get(standard)
                .standardBreakerRatings()
                .sorted()

        return ratings.firstOrNull { rating ->
            rating + EPSILON >= designCurrentA &&
                rating <= cableAmpacityA + EPSILON
        } ?: 0.0
    }

    fun satisfiesCoordination(
        designCurrentA: Double,
        breakerRatingA: Double,
        cableAmpacityA: Double
    ): Boolean {

        require(designCurrentA >= 0.0)
        require(breakerRatingA >= 0.0)
        require(cableAmpacityA >= 0.0)

        return designCurrentA <= breakerRatingA &&
            breakerRatingA <= cableAmpacityA
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

    fun availableRatings(
        standard: Standard = Standard.IEC
    ): List<Double> {

        return CodeEngineFactory
            .get(standard)
            .standardBreakerRatings()
            .sorted()
    }

    fun calculate(
        designCurrentA: Double,
        cableAmpacityA: Double,
        prospectiveFaultCurrentKA: Double = 0.0,
        breakerBreakingCapacityKA: Double = 0.0,
        standard: Standard = Standard.IEC
    ): BreakerSelectionResult {

        val selected =
            selectRating(
                designCurrentA = designCurrentA,
                cableAmpacityA = cableAmpacityA,
                standard = standard
            )

        val coordination =
            selected > 0.0 &&
                satisfiesCoordination(
                    designCurrentA = designCurrentA,
                    breakerRatingA = selected,
                    cableAmpacityA = cableAmpacityA
                )

        val breakingCapacityValid =
            if (prospectiveFaultCurrentKA > 0.0 &&
                breakerBreakingCapacityKA > 0.0
            ) {
                isBreakingCapacityAdequate(
                    prospectiveFaultCurrentKA =
                        prospectiveFaultCurrentKA,
                    breakerBreakingCapacityKA =
                        breakerBreakingCapacityKA
                )
            } else {
                false
            }

        val engine =
            CodeEngineFactory.get(standard)

        val notes =
            buildList {

                add(
                    "Standard: ${engine.codeName}"
                )

                add(
                    "Design current = %.2f A"
                        .format(designCurrentA)
                )

                add(
                    "Cable ampacity = %.2f A"
                        .format(cableAmpacityA)
                )

                if (selected > 0.0) {
                    add(
                        "Selected nominal rating = %.0f A"
                            .format(selected)
                    )
                } else {
                    add(
                        "No standard breaker rating satisfies Ib <= In <= Iz."
                    )
                }

                add(
                    if (coordination) {
                        "Coordination Ib <= In <= Iz: PASS"
                    } else {
                        "Coordination Ib <= In <= Iz: FAIL"
                    }
                )

                if (!engine.isFullyImplemented()) {
                    add(
                        engine.implementationStatus()
                    )
                }
            }

        return BreakerSelectionResult(
            designCurrentA = designCurrentA,
            cableAmpacityA = cableAmpacityA,
            selectedRatingA = selected,
            breakingCapacityKA = breakerBreakingCapacityKA,
            coordinationValid = coordination,
            breakingCapacityValid = breakingCapacityValid,
            valid = selected > 0.0 && coordination,
            notes = notes
        )
    }
}
