package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

/**
 * Common contract for engineering-code engines.
 *
 * The calculation layer must ask the selected code engine for
 * code-dependent values instead of hard-coding IEC assumptions.
 */
interface StandardEngine {

    val standard: Standard

    val codeName: String

    val codeRevision: String

    /**
     * Maximum recommended voltage-drop percentage for the
     * requested circuit category.
     *
     * The exact category mapping is intentionally delegated
     * to each code engine.
     */
    fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double

    /**
     * Returns the correction factor for ambient temperature.
     */
    fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double

    /**
     * Returns the grouping / adjustment factor applicable to
     * the requested number of circuits.
     */
    fun groupingFactor(
        numberOfCircuits: Int
    ): Double

    /**
     * Returns the base current-carrying capacity for a conductor.
     *
     * The implementation must use the tables belonging to the
     * selected standard.
     */
    fun conductorAmpacity(
        sectionMm2: Double,
        material: ConductorMaterial,
        insulation: InsulationType,
        installationMethod: InstallationMethod,
        loadedConductors: Int
    ): Double?

    /**
     * Returns the standard conductor sizes available for the
     * selected code.
     */
    fun standardConductorSections(): List<Double>

    /**
     * Returns nominal protective-device ratings accepted by
     * this standard.
     */
    fun standardBreakerRatings(): List<Double>

    /**
     * Indicates whether a result is based on a fully implemented
     * standard dataset.
     *
     * This prevents the UI from falsely presenting an incomplete
     * dataset as full code compliance.
     */
    fun isFullyImplemented(): Boolean

    /**
     * Human-readable implementation status.
     */
    fun implementationStatus(): String
}
