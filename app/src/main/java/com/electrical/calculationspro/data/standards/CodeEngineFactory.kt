package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.Standard

/**
 * Single entry point for engineering-code engines.
 *
 * A standard is never represented by another standard's engine.
 */
object CodeEngineFactory {

    private val egyptianEngine =
        EgyptianCodeEngine()

    private val iecEngine =
        IecEngine()

    private val necEngine =
        NecEngine()

    private val ceiEngine =
        UnsupportedStandardEngine(
            standard = Standard.CEI,
            codeName = "CEI 64-8",
            description =
                "CEI 64-8 requires a dedicated verified CEI dataset."
        )

    private val cecEngine =
        UnsupportedStandardEngine(
            standard = Standard.CEC,
            codeName = "Canadian Electrical Code",
            description =
                "CEC requires a dedicated verified CEC dataset."
        )

    fun get(
        standard: Standard
    ): StandardEngine =
        when (standard) {
            Standard.EGYPTIAN -> egyptianEngine
            Standard.IEC -> iecEngine
            Standard.NEC -> necEngine
            Standard.CEI -> ceiEngine
            Standard.CEC -> cecEngine
        }

    fun default(): StandardEngine =
        egyptianEngine
}

/**
 * Explicit placeholder for standards whose dedicated verified dataset
 * has not yet been implemented.
 *
 * This prevents accidental inheritance of another country's/code's data.
 */
private class UnsupportedStandardEngine(
    override val standard: Standard,
    override val codeName: String,
    private val description: String
) : StandardEngine {

    override val codeRevision: String =
        "Dataset not implemented"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double =
        0.0

    override fun ambientTemperatureFactor(
        insulation: com.electrical.calculationspro.data.InsulationType,
        ambientTemperatureC: Double
    ): Double =
        1.0

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {
        require(numberOfCircuits >= 1) {
            "Number of circuits must be at least 1."
        }

        return 1.0
    }

    override fun conductorAmpacity(
        sectionMm2: Double,
        material: com.electrical.calculationspro.data.ConductorMaterial,
        insulation: com.electrical.calculationspro.data.InsulationType,
        installationMethod: com.electrical.calculationspro.data.InstallationMethod,
        loadedConductors: Int
    ): Double? =
        null

    override fun standardConductorSections(): List<Double> =
        emptyList()

    override fun standardBreakerRatings(): List<Double> =
        emptyList()

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        "$description No data from IEC, NEC, or another standard is substituted."
}
