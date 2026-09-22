package com.electrical.calculationspro.data.standards.nec

/**
 * NEC Article 240
 *
 * Overcurrent protection.
 */
object NecArticle240 {

    data class ProtectionInput(
        val loadCurrentA: Double,
        val conductorAmpacityA: Double,
        val selectedProtectionA: Double,
        val interruptingRatingKA: Double,
        val availableFaultCurrentKA: Double
    )

    data class Assessment(
        val valid: Boolean,
        val coordinationValid: Boolean,
        val interruptingRatingValid: Boolean,
        val notes: List<String>
    )

    fun assess(
        input: ProtectionInput
    ): Assessment {

        val notes = mutableListOf<String>()

        val coordinationValid =
            input.loadCurrentA > 0.0 &&
                input.conductorAmpacityA > 0.0 &&
                input.selectedProtectionA >= input.loadCurrentA &&
                input.selectedProtectionA <= input.conductorAmpacityA

        if (!coordinationValid) {
            notes +=
                "Entered protection rating does not satisfy the basic load/conductor coordination check."
        }

        val interruptingValid =
            input.interruptingRatingKA >=
                input.availableFaultCurrentKA

        if (!interruptingValid) {
            notes +=
                "Interrupting rating is below the available fault current."
        }

        val inputValid =
            input.loadCurrentA > 0.0 &&
                input.conductorAmpacityA > 0.0 &&
                input.selectedProtectionA > 0.0 &&
                input.interruptingRatingKA > 0.0 &&
                input.availableFaultCurrentKA >= 0.0

        if (!inputValid) {
            notes +=
                "One or more protection inputs are invalid."
        }

        return Assessment(
            valid =
                inputValid &&
                    coordinationValid &&
                    interruptingValid,
            coordinationValid = coordinationValid,
            interruptingRatingValid = interruptingValid,
            notes = notes
        )
    }

    fun nextStandardRating(
        requiredCurrentA: Double
    ): Double? {

        if (requiredCurrentA <= 0.0) {
            return null
        }

        return standardRatingsA.firstOrNull {
            it >= requiredCurrentA
        }
    }

    /**
     * Nominal engineering selection values.
     *
     * These are not manufacturer-specific catalog data.
     */
    val standardRatingsA: List<Double> =
        listOf(
            15.0,
            20.0,
            25.0,
            30.0,
            35.0,
            40.0,
            45.0,
            50.0,
            60.0,
            70.0,
            80.0,
            90.0,
            100.0,
            110.0,
            125.0,
            150.0,
            175.0,
            200.0,
            225.0,
            250.0,
            300.0,
            350.0,
            400.0,
            450.0,
            500.0,
            600.0,
            700.0,
            800.0,
            1000.0,
            1200.0,
            1600.0,
            2000.0,
            2500.0,
            3000.0,
            4000.0,
            5000.0,
            6000.0
        )
}
