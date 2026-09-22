package com.electrical.calculationspro.data.standards.nec

/**
 * NEC Article 215
 *
 * Feeders.
 */
object NecArticle215 {

    data class FeederInput(
        val calculatedLoadA: Double,
        val feederRatingA: Double
    )

    data class Assessment(
        val valid: Boolean,
        val notes: List<String>
    )

    fun assess(
        input: FeederInput
    ): Assessment {

        val notes = mutableListOf<String>()
        var valid = true

        if (input.calculatedLoadA <= 0.0) {
            notes += "Calculated feeder load must be greater than zero."
            valid = false
        }

        if (input.feederRatingA <= 0.0) {
            notes += "Feeder rating must be greater than zero."
            valid = false
        }

        if (input.feederRatingA < input.calculatedLoadA) {
            notes +=
                "Feeder rating is below the calculated load."
            valid = false
        }

        return Assessment(
            valid = valid,
            notes = notes
        )
    }
}
