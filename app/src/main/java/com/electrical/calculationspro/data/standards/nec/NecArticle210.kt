package com.electrical.calculationspro.data.standards.nec

/**
 * NEC Article 210
 *
 * Branch circuits.
 *
 * This file provides the application rule layer.
 * It does not reproduce the copyrighted NEC text/tables.
 */
object NecArticle210 {

    const val ARTICLE = "210"

    enum class BranchCircuitType {
        GENERAL,
        LIGHTING,
        RECEPTACLE,
        MOTOR,
        DEDICATED_EQUIPMENT,
        OTHER
    }

    data class BranchCircuitInput(
        val loadA: Double,
        val branchCircuitRatingA: Double,
        val type: BranchCircuitType
    )

    data class Assessment(
        val valid: Boolean,
        val notes: List<String>
    )

    fun assess(
        input: BranchCircuitInput
    ): Assessment {

        val notes = mutableListOf<String>()
        var valid = true

        if (input.loadA <= 0.0) {
            notes += "Branch-circuit load must be greater than zero."
            valid = false
        }

        if (input.branchCircuitRatingA <= 0.0) {
            notes += "Branch-circuit rating must be greater than zero."
            valid = false
        }

        if (input.branchCircuitRatingA < input.loadA) {
            notes +=
                "Branch-circuit rating is below the entered load."
            valid = false
        }

        return Assessment(
            valid = valid,
            notes = notes
        )
    }
}
