package com.electrical.calculationspro.data.standards.egyptian

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType

/**
 * Egyptian cable-engineering data access layer.
 *
 * The application must NOT pretend that IEC ampacity tables are Egyptian
 * tables. Until the licensed/current Egyptian tables are entered from HBRC,
 * this layer reports that the exact Egyptian ampacity dataset is unavailable.
 */
object EgyptianCableTables {

    data class CableAmpacityRequest(
        val sectionMm2: Double,
        val conductor: ConductorMaterial,
        val insulation: InsulationType,
        val installationMethod: String,
        val ambientTemperatureC: Double,
        val loadedConductors: Int
    )

    data class CableAmpacityResult(
        val available: Boolean,
        val ampacityA: Double?,
        val source: String,
        val notes: List<String>
    )

    private val standardSections = listOf(
        1.5,
        2.5,
        4.0,
        6.0,
        10.0,
        16.0,
        25.0,
        35.0,
        50.0,
        70.0,
        95.0,
        120.0,
        150.0,
        185.0,
        240.0,
        300.0
    )

    fun standardSections(): List<Double> =
        standardSections

    /**
     * Exact Egyptian ampacity lookup.
     *
     * Intentionally does not fall back to IEC.
     */
    fun ampacity(
        request: CableAmpacityRequest
    ): CableAmpacityResult {

        val notes = mutableListOf<String>()

        if (request.sectionMm2 <= 0.0) {
            notes += "Cable section must be greater than zero."

            return CableAmpacityResult(
                available = false,
                ampacityA = null,
                source = "Egyptian Code / HBRC",
                notes = notes
            )
        }

        if (request.ambientTemperatureC < -50.0 ||
            request.ambientTemperatureC > 100.0
        ) {
            notes += "Ambient temperature is outside the supported engineering input range."
        }

        if (request.loadedConductors <= 0) {
            notes += "Loaded conductor count must be greater than zero."
        }

        notes +=
            "Exact Egyptian cable ampacity tables must be populated from the licensed/current HBRC Egyptian Electrical Code."

        notes +=
            "IEC ampacity data must not be substituted and presented as Egyptian-code compliance."

        return CableAmpacityResult(
            available = false,
            ampacityA = null,
            source = "HBRC Egyptian Electrical Code",
            notes = notes
        )
    }

    fun isStandardSection(
        sectionMm2: Double
    ): Boolean =
        standardSections.any {
            kotlin.math.abs(it - sectionMm2) < 0.0001
        }
}
