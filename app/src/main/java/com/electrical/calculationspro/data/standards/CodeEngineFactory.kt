package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

/**
 * Single entry point for engineering-code engines.
 *
 * No standard may silently inherit numerical data from another standard.
 * Missing correction-factor data is represented by 0.0.
 * Callers must treat 0.0 as unavailable, never as a valid design factor.
 */
object CodeEngineFactory {

    private val egyptianEngine: StandardEngine =
        EgyptianCodeEngine()

    private val iecEngine: StandardEngine =
        IecEngine()

    private val necEngine: StandardEngine =
        NecEngine()

    private val ceiEngine: StandardEngine =
        UnsupportedStandardEngine(
            standard = Standard.CEI,
            codeName = "CEI 64-8",
            description =
                "CEI 64-8 requires a dedicated verified CEI dataset."
        )

    private val cecEngine: StandardEngine =
        UnsupportedStandardEngine(
            standard = Standard.CEC,
            codeName = "Canadian Electrical Code",
            description =
                "CEC requires a dedicated verified CEC dataset."
        )

    fun get(standard: Standard): StandardEngine =
        when (standard) {
            Standard.EGYPTIAN -> egyptianEngine
            Standard.IEC -> iecEngine
            Standard.NEC -> necEngine
            Standard.CEI -> ceiEngine
            Standard.CEC -> cecEngine
        }

    /**
     * The application default is explicitly Egyptian.
     * Callers needing another code must pass it explicitly.
     */
    fun default(): StandardEngine = egyptianEngine
}

/**
 * Placeholder for standards whose dedicated numerical datasets
 * have not been implemented and verified.
 *
 * This engine deliberately returns unavailable values. It must not
 * use IEC, NEC, Egyptian or any other code as a substitute.
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
    ): Double = 0.0

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double = 0.0

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {
        require(numberOfCircuits >= 1) {
            "Number of circuits must be at least 1."
        }

        return 0.0
    }

    override fun conductorAmpacity(
        sectionMm2: Double,
        material: ConductorMaterial,
        insulation: InsulationType,
        installationMethod: InstallationMethod,
        loadedConductors: Int
    ): Double? = null

    override fun standardConductorSections(): List<Double> =
        emptyList()

    override fun standardBreakerRatings(): List<Double> =
        emptyList()

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        buildString {
            append(description)
            append(' ')
            append(
                "No numerical data from another standard is substituted. "
            )
            append(
                "Voltage-drop limits, ambient correction, grouping correction, "
            )
            append(
                "conductor ampacity and breaker ratings are unavailable. "
            )
            append(
                "Engineering calculations requiring these datasets must not "
            )
            append(
                "issue a verified design recommendation."
            )
        }
}
