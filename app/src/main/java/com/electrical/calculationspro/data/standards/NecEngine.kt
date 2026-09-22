package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

/**
 * NEC engine.
 *
 * IMPORTANT:
 * IEC ampacity tables are NEVER substituted for NEC tables.
 *
 * Until verified NEC Article 310 data is populated, conductor
 * ampacity is intentionally unavailable.
 */
class NecEngine(
    private val standardOverride: Standard =
        Standard.NEC,
    private val codeNameOverride: String =
        "NFPA 70 - National Electrical Code"
) : StandardEngine {

    override val standard: Standard =
        standardOverride

    override val codeName: String =
        codeNameOverride

    override val codeRevision: String =
        "Controlled NEC dataset - incomplete"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double {

        return when (
            circuitCategory
                .trim()
                .lowercase()
        ) {
            "feeder" -> 3.0
            "branch" -> 3.0
            "total" -> 5.0
            else -> 5.0
        }
    }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {

        /*
         * NEC Article 310 correction data is intentionally not
         * approximated using IEC tables.
         */
        return 1.0
    }

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {

        /*
         * NEC adjustment factors will be supplied by the dedicated
         * NEC Article 310 dataset.
         */
        return 1.0
    }

    override fun conductorAmpacity(
        sectionMm2: Double,
        material: ConductorMaterial,
        insulation: InsulationType,
        installationMethod: InstallationMethod,
        loadedConductors: Int
    ): Double? {

        /*
         * No IEC fallback.
         *
         * Returning null prevents the application from claiming
         * NEC compliance using non-NEC data.
         */
        return null
    }

    override fun standardConductorSections(): List<Double> =
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

    override fun standardBreakerRatings(): List<Double> =
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

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        "NEC calculation engine is active, but verified NEC Article 310 " +
            "ampacity and adjustment datasets are not yet populated. " +
            "IEC ampacity data is deliberately NOT used as a substitute."
}
