package com.electrical.calculationspro.data.standards.egyptian

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType

/**
 * Egyptian cable data access layer.
 *
 * This module deliberately refuses to fabricate ampacity values.
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

    private val standardSections =
        listOf(
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

    fun ampacity(
        request: CableAmpacityRequest
    ): CableAmpacityResult {

        val notes = mutableListOf<String>()

        if (request.sectionMm2 <= 0.0) {
            notes += "Cable section must be greater than zero."

            return unavailable(notes)
        }

        if (request.loadedConductors <= 0) {
            notes += "Loaded conductor count must be greater than zero."

            return unavailable(notes)
        }

        if (
            request.ambientTemperatureC < -50.0 ||
            request.ambientTemperatureC > 100.0
        ) {
            notes +=
                "Ambient temperature is outside the supported input range."

            return unavailable(notes)
        }

        notes +=
            "Verified/current Egyptian cable ampacity tables have not " +
                "yet been populated in this application."

        notes +=
            "IEC ampacity data is deliberately not substituted."

        return unavailable(notes)
    }

    private fun unavailable(
        notes: List<String>
    ): CableAmpacityResult =
        CableAmpacityResult(
            available = false,
            ampacityA = null,
            source = "HBRC Egyptian Electrical Code",
            notes = notes
        )

    fun isStandardSection(
        sectionMm2: Double
    ): Boolean =
        standardSections.any {
            kotlin.math.abs(it - sectionMm2) < 0.0001
        }
}
