package com.electrical.calculationspro.data.standards.nec

/**
 * NEC Article 250
 *
 * Grounding and bonding engineering layer.
 */
object NecArticle250 {

    enum class GroundingSystem {
        TN,
        TT,
        IT,
        OTHER
    }

    data class GroundingInput(
        val system: GroundingSystem,
        val faultCurrentA: Double,
        val clearingTimeSeconds: Double,
        val groundingConductorAreaMm2: Double
    )

    data class Assessment(
        val valid: Boolean,
        val notes: List<String>
    )

    fun assess(
        input: GroundingInput
    ): Assessment {

        val notes = mutableListOf<String>()
        var valid = true

        if (input.faultCurrentA <= 0.0) {
            notes +=
                "Fault current must be greater than zero."
            valid = false
        }

        if (input.clearingTimeSeconds <= 0.0) {
            notes +=
                "Fault-clearing time must be greater than zero."
            valid = false
        }

        if (input.groundingConductorAreaMm2 <= 0.0) {
            notes +=
                "Grounding-conductor area must be greater than zero."
            valid = false
        }

        notes +=
            "Final grounding-conductor sizing must use the applicable NEC Article 250 method and verified NEC tables."

        return Assessment(
            valid = valid,
            notes = notes
        )
    }
}
