package com.electrical.calculationspro.data.standards.nec

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType

/**
 * NEC Article 310
 *
 * Conductors for general wiring.
 *
 * Exact ampacity tables must be populated from the applicable
 * NEC edition and verified source data.
 */
object NecArticle310 {

    data class AmpacityInput(
        val sectionMm2: Double,
        val conductor: ConductorMaterial,
        val insulation: InsulationType,
        val ambientTemperatureC: Double,
        val loadedConductors: Int
    )

    data class AmpacityResult(
        val available: Boolean,
        val ampacityA: Double?,
        val notes: List<String>
    )

    /**
     * Returns NEC conductor ampacity.
     *
     * Deliberately does NOT fall back to IEC tables.
     */
    fun ampacity(
        input: AmpacityInput
    ): AmpacityResult {

        val notes = mutableListOf<String>()

        if (input.sectionMm2 <= 0.0) {
            notes +=
                "Conductor size must be greater than zero."
        }

        if (input.loadedConductors <= 0) {
            notes +=
                "Number of current-carrying conductors must be greater than zero."
        }

        if (input.ambientTemperatureC < -50.0 ||
            input.ambientTemperatureC > 100.0
        ) {
            notes +=
                "Ambient temperature is outside the supported input range."
        }

        notes +=
            "Exact NEC Article 310 ampacity data must be populated from the selected NEC edition."

        notes +=
            "IEC ampacity tables must not be substituted for NEC compliance."

        return AmpacityResult(
            available = false,
            ampacityA = null,
            notes = notes
        )
    }
}
