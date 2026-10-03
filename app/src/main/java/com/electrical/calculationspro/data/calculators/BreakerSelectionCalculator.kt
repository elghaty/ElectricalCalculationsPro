package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.BreakerSelectionResult
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.CodeEngineFactory

/**
 * Protective-device engineering selection.
 *
 * Basic coordination rule:
 *
 *      Ib <= In <= Iz
 *
 * Final verification also requires adequate breaking capacity
 * against the prospective short-circuit current.
 */
object BreakerSelectionCalculator {

    private const val EPSILON = 1.0e-9

    /**
     * Select the smallest standard breaker rating satisfying:
     *
     *      Ib <= In <= Iz
     */
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

            rating + EPSILON >=
                designCurrentA &&

                rating <=
                cableAmpacityA + EPSILON

        } ?: 0.0
    }

    /**
     * Verify:
     *
     *      Ib <= In <= Iz
     */
    fun satisfiesCoordination(
        designCurrentA: Double,
        breakerRatingA: Double,
        cableAmpacityA: Double
    ): Boolean {

        require(designCurrentA >= 0.0) {
            "Design current cannot be negative."
        }

        require(breakerRatingA >= 0.0) {
            "Breaker rating cannot be negative."
        }

        require(cableAmpacityA >= 0.0) {
            "Cable ampacity cannot be negative."
        }

        return designCurrentA <=
            breakerRatingA &&

            breakerRatingA <=
            cableAmpacityA
    }

    /**
     * Verify breaker breaking capacity against
     * prospective short-circuit current.
     */
    fun isBreakingCapacityAdequate(
        prospectiveFaultCurrentKA: Double,
        breakerBreakingCapacityKA: Double
    ): Boolean {

        require(prospectiveFaultCurrentKA >= 0.0) {
            "Prospective fault current cannot be negative."
        }

        require(breakerBreakingCapacityKA >= 0.0) {
            "Breaker breaking capacity cannot be negative."
        }

        return breakerBreakingCapacityKA +
            EPSILON >=
            prospectiveFaultCurrentKA
    }

    /**
     * Available standard nominal breaker ratings.
     */
    fun availableRatings(
        standard: Standard = Standard.IEC
    ): List<Double> {

        return CodeEngineFactory
            .get(standard)
            .standardBreakerRatings()
            .sorted()
    }

    /**
     * Complete preliminary breaker verification.
     *
     * A result is considered final-valid only when:
     *
     * 1. A standard breaker rating exists.
     * 2. Ib <= In <= Iz.
     * 3. Prospective fault current is supplied.
     * 4. Breaker breaking capacity is supplied.
     * 5. Breaking capacity >= prospective fault current.
     *
     * This prevents an incomplete breaker check from being
     * presented as a fully verified engineering result.
     */
    fun calculate(
        designCurrentA: Double,
        cableAmpacityA: Double,
        prospectiveFaultCurrentKA: Double = 0.0,
        breakerBreakingCapacityKA: Double = 0.0,
        standard: Standard = Standard.IEC
    ): BreakerSelectionResult {

        require(designCurrentA >= 0.0) {
            "Design current cannot be negative."
        }

        require(cableAmpacityA >= 0.0) {
            "Cable ampacity cannot be negative."
        }

        require(prospectiveFaultCurrentKA >= 0.0) {
            "Prospective fault current cannot be negative."
        }

        require(breakerBreakingCapacityKA >= 0.0) {
            "Breaker breaking capacity cannot be negative."
        }

        val selected =
            selectRating(
                designCurrentA =
                    designCurrentA,
                cableAmpacityA =
                    cableAmpacityA,
                standard =
                    standard
            )

        val coordination =
            selected > EPSILON &&
                satisfiesCoordination(
                    designCurrentA =
                        designCurrentA,
                    breakerRatingA =
                        selected,
                    cableAmpacityA =
                        cableAmpacityA
                )

        /*
         * Zero is treated as "not supplied", not as
         * a valid short-circuit value.
         */
        val faultDataProvided =
            prospectiveFaultCurrentKA >
                EPSILON &&

                breakerBreakingCapacityKA >
                EPSILON

        val breakingCapacityValid =
            faultDataProvided &&
                isBreakingCapacityAdequate(
                    prospectiveFaultCurrentKA =
                        prospectiveFaultCurrentKA,
                    breakerBreakingCapacityKA =
                        breakerBreakingCapacityKA
                )

        val engine =
            CodeEngineFactory.get(standard)

        val notes =
            buildList {

                add(
                    "Standard: ${engine.codeName}"
                )

                add(
                    "Design current = %.2f A"
                        .format(
                            designCurrentA
                        )
                )

                add(
                    "Cable ampacity = %.2f A"
                        .format(
                            cableAmpacityA
                        )
                )

                if (selected > EPSILON) {

                    add(
                        "Selected nominal rating = %.0f A"
                            .format(
                                selected
                            )
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

                if (faultDataProvided) {

                    add(
                        "Breaking capacity = %.1f kA; prospective fault current = %.1f kA"
                            .format(
                                breakerBreakingCapacityKA,
                                prospectiveFaultCurrentKA
                            )
                    )

                    add(
                        if (breakingCapacityValid) {

                            "Breaking capacity check: PASS"

                        } else {

                            "Breaking capacity check: FAIL"
                        }
                    )

                } else {

                    add(
                        "Breaking capacity verification INCOMPLETE: prospective fault current and selected device breaking capacity are required."
                    )
                }

                if (!engine.isFullyImplemented()) {

                    add(
                        engine.implementationStatus()
                    )
                }
            }

        /*
         * Final validity deliberately requires the complete
         * electrical verification chain.
         */
        val valid =
            selected > EPSILON &&
                coordination &&
                faultDataProvided &&
                breakingCapacityValid

        return BreakerSelectionResult(
            designCurrentA =
                designCurrentA,

            cableAmpacityA =
                cableAmpacityA,

            selectedRatingA =
                selected,

            breakingCapacityKA =
                breakerBreakingCapacityKA,

            coordinationValid =
                coordination,

            breakingCapacityValid =
                breakingCapacityValid,

            valid =
                valid,

            notes =
                notes
        )
    }
}
