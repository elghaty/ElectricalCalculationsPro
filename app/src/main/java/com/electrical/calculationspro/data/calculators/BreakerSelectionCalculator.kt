package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.BreakerSelectionResult
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.standards.CodeRuleRegistry
import com.electrical.calculationspro.data.standards.EngineeringRuleBook

/**
 * PROFESSIONAL BREAKER SELECTION ENGINE
 *
 * Engineering chain:
 *
 *     Ib
 *      ↓
 * Standard breaker ratings
 *      ↓
 * Ib <= In <= Iz
 *      ↓
 * Prospective Ik
 *      ↓
 * Icu/Ics >= Ik
 *      ↓
 * Manufacturer catalogue verification
 *
 * This calculator determines engineering requirements.
 * Manufacturer catalogue selection remains in EquipmentSelectionCalculator.
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

        EngineeringRuleBook.requireReference(
            standard = standard,
            domain = CodeRuleRegistry.RuleDomain.OVERCURRENT_PROTECTION
        )

        EngineeringRuleBook.requireReference(
            standard = standard,
            domain = CodeRuleRegistry.RuleDomain.BREAKER
        )

        return CodeEngineFactory
            .get(standard)
            .standardBreakerRatings()
            .filter { it > EPSILON }
            .sorted()
            .firstOrNull { rating ->
                rating + EPSILON >= designCurrentA &&
                    rating <= cableAmpacityA + EPSILON
            }
            ?: 0.0
    }

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

    fun availableRatings(
        standard: Standard = Standard.IEC
    ): List<Double> {

        EngineeringRuleBook.requireReference(
            standard = standard,
            domain = CodeRuleRegistry.RuleDomain.BREAKER
        )

        return CodeEngineFactory
            .get(standard)
            .standardBreakerRatings()
            .filter { it > EPSILON }
            .sorted()
    }

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

        val protectionRule =
            EngineeringRuleBook.requireReference(
                standard = standard,
                domain =
                    CodeRuleRegistry.RuleDomain.OVERCURRENT_PROTECTION
            )

        val breakerRule =
            EngineeringRuleBook.requireReference(
                standard = standard,
                domain =
                    CodeRuleRegistry.RuleDomain.BREAKER
            )

        val engine =
            CodeEngineFactory.get(standard)

        val selected =
            selectRating(
                designCurrentA = designCurrentA,
                cableAmpacityA = cableAmpacityA,
                standard = standard
            )

        val coordination =
            selected > EPSILON &&
                satisfiesCoordination(
                    designCurrentA = designCurrentA,
                    breakerRatingA = selected,
                    cableAmpacityA = cableAmpacityA
                )

        val faultDataProvided =
            prospectiveFaultCurrentKA > EPSILON &&
                breakerBreakingCapacityKA > EPSILON

        val breakingCapacityValid =
            faultDataProvided &&
                isBreakingCapacityAdequate(
                    prospectiveFaultCurrentKA =
                        prospectiveFaultCurrentKA,
                    breakerBreakingCapacityKA =
                        breakerBreakingCapacityKA
                )

        /*
         * A calculation can only be called professionally verified
         * when the applicable rule references are verified AND
         * all required numerical inputs exist.
         */
        val codeRulesVerified =
            protectionRule.verified &&
                breakerRule.verified

        val valid =
            selected > EPSILON &&
                coordination &&
                faultDataProvided &&
                breakingCapacityValid &&
                codeRulesVerified

        val references =
            (
                EngineeringRuleBook.referenceIds(
                    standard,
                    CodeRuleRegistry.RuleDomain.OVERCURRENT_PROTECTION
                ) +
                    EngineeringRuleBook.referenceIds(
                        standard,
                        CodeRuleRegistry.RuleDomain.BREAKER
                    )
                ).distinct()

        val notes =
            buildList {

                add("Standard: ${engine.codeName}")
                add("Code revision: ${engine.codeRevision}")
                add("Rule domains: OVERCURRENT_PROTECTION, BREAKER")

                add(
                    "Design current Ib = %.2f A"
                        .format(designCurrentA)
                )

                add(
                    "Cable ampacity Iz = %.2f A"
                        .format(cableAmpacityA)
                )

                if (selected > EPSILON) {
                    add(
                        "Selected nominal breaker current In = %.0f A"
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

                if (faultDataProvided) {
                    add(
                        "Prospective fault current Ik = %.3f kA"
                            .format(
                                prospectiveFaultCurrentKA
                            )
                    )

                    add(
                        "Breaker breaking capacity = %.3f kA"
                            .format(
                                breakerBreakingCapacityKA
                            )
                    )

                    add(
                        if (breakingCapacityValid) {
                            "Breaking-capacity verification: PASS"
                        } else {
                            "Breaking-capacity verification: FAIL"
                        }
                    )
                } else {
                    add(
                        "Breaking-capacity verification INCOMPLETE: verified prospective fault current and breaker breaking capacity are required."
                    )
                }

                if (!protectionRule.verified) {
                    add(
                        "Overcurrent protection numerical/design dataset is not fully verified."
                    )
                }

                if (!breakerRule.verified) {
                    add(
                        "Breaker rule numerical/design dataset is not fully verified."
                    )
                }

                add(
                    "Final manufacturer selection must be verified against the equipment catalogue."
                )

                addAll(
                    EngineeringRuleBook.auditTrail(
                        standard,
                        CodeRuleRegistry.RuleDomain.OVERCURRENT_PROTECTION
                    )
                )

                addAll(
                    EngineeringRuleBook.auditTrail(
                        standard,
                        CodeRuleRegistry.RuleDomain.BREAKER
                    )
                )
            }

        return BreakerSelectionResult(
            designCurrentA = designCurrentA,
            cableAmpacityA = cableAmpacityA,
            selectedRatingA = selected,
            breakingCapacityKA =
                breakerBreakingCapacityKA,
            coordinationValid = coordination,
            breakingCapacityValid =
                breakingCapacityValid,
            valid = valid,
            notes = notes
        )
    }
}
