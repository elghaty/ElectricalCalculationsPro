package com.electrical.calculationspro.data.standards.nec

/**
 * NEC Article 220
 *
 * Branch-circuit, feeder and service load calculations.
 *
 * Demand factors and optional-load treatment are kept here rather
 * than inside the generic load calculator.
 */
object NecArticle220 {

    data class LoadInput(
        val connectedLoadKw: Double,
        val demandFactor: Double = 1.0,
        val diversityFactor: Double = 1.0
    )

    data class LoadResult(
        val connectedLoadKw: Double,
        val demandLoadKw: Double,
        val diversifiedLoadKw: Double,
        val valid: Boolean,
        val notes: List<String>
    )

    fun calculate(
        input: LoadInput
    ): LoadResult {

        val notes = mutableListOf<String>()

        if (input.connectedLoadKw < 0.0) {
            return LoadResult(
                connectedLoadKw = input.connectedLoadKw,
                demandLoadKw = 0.0,
                diversifiedLoadKw = 0.0,
                valid = false,
                notes = listOf(
                    "Connected load cannot be negative."
                )
            )
        }

        if (input.demandFactor <= 0.0 ||
            input.demandFactor > 1.0
        ) {
            notes +=
                "Demand factor must be greater than 0 and not greater than 1."
        }

        if (input.diversityFactor <= 0.0) {
            notes +=
                "Diversity factor must be greater than zero."
        }

        if (notes.isNotEmpty()) {
            return LoadResult(
                connectedLoadKw = input.connectedLoadKw,
                demandLoadKw = 0.0,
                diversifiedLoadKw = 0.0,
                valid = false,
                notes = notes
            )
        }

        val demandLoad =
            input.connectedLoadKw *
                input.demandFactor

        val diversifiedLoad =
            demandLoad /
                input.diversityFactor

        return LoadResult(
            connectedLoadKw = input.connectedLoadKw,
            demandLoadKw = demandLoad,
            diversifiedLoadKw = diversifiedLoad,
            valid = true,
            notes = listOf(
                "NEC Article 220 load-calculation framework applied."
            )
        )
    }
}
