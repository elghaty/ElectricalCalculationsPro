package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

/**
 * ================================================================
 * STANDARD ENGINE
 * ================================================================
 *
 * Common contract for all electrical-code engineering engines.
 *
 * Architecture:
 *
 * SLD / Project
 *      |
 *      v
 * SldEngineeringContext
 *      |
 *      v
 * CodeEngineFactory
 *      |
 *      +---- EgyptianCodeEngine
 *      +---- IecEngine
 *      +---- NecEngine
 *      +---- dedicated future engines
 *
 * IMPORTANT:
 *
 * A StandardEngine is the ONLY source for code-dependent values.
 *
 * Calculation engines must NOT:
 *
 * - hard-code IEC assumptions
 * - hard-code Egyptian assumptions
 * - copy values from another standard
 * - silently substitute another code
 * - manufacture missing code data
 *
 * ================================================================
 */
interface StandardEngine {

    /**
     * Standard represented by this engine.
     */
    val standard: Standard

    /**
     * Human-readable code name.
     */
    val codeName: String

    /**
     * Exact controlled revision/dataset identifier.
     */
    val codeRevision: String

    /**
     * ============================================================
     * VOLTAGE DROP
     * ============================================================
     *
     * Returns a verified voltage-drop design limit for the requested
     * circuit category.
     *
     * Return value:
     *
     * > 0.0
     *     Verified value is available.
     *
     * 0.0
     *     This standard does not provide a verified value through
     *     this engine for the requested category.
     *
     * A value of 0.0 MUST NOT be interpreted as a zero permitted
     * voltage drop.
     */
    fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double

    /**
     * ============================================================
     * AMBIENT TEMPERATURE FACTOR
     * ============================================================
     *
     * Returns the verified correction factor applicable to the
     * requested conductor insulation and ambient temperature.
     *
     * If the required table entry is not implemented, the engine
     * must NOT substitute another standard.
     */
    fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double

    /**
     * ============================================================
     * GROUPING / ADJUSTMENT FACTOR
     * ============================================================
     *
     * Returns the verified adjustment factor for the applicable
     * number of loaded/current-carrying circuits.
     */
    fun groupingFactor(
        numberOfCircuits: Int
    ): Double

    /**
     * ============================================================
     * CONDUCTOR AMPACITY
     * ============================================================
     *
     * Returns the verified base ampacity for the exact combination
     * requested by the calculation engine.
     *
     * null means the required dataset combination is unavailable.
     *
     * IMPORTANT:
     *
     * null MUST remain null.
     *
     * The caller must not silently replace it with data belonging
     * to another standard.
     */
    fun conductorAmpacity(
        sectionMm2: Double,
        material: ConductorMaterial,
        insulation: InsulationType,
        installationMethod: InstallationMethod,
        loadedConductors: Int
    ): Double?

    /**
     * ============================================================
     * STANDARD CONDUCTOR SECTIONS
     * ============================================================
     *
     * Returns only conductor sizes actually represented by the
     * selected standard dataset.
     *
     * The list must never be populated by copying another code's
     * conductor-size table.
     */
    fun standardConductorSections(): List<Double>

    /**
     * ============================================================
     * STANDARD BREAKER RATINGS
     * ============================================================
     *
     * Returns nominal device ratings supported by the selected
     * standard/device dataset.
     *
     * These values are NOT a substitute for a manufacturer
     * equipment catalogue.
     *
     * Final equipment selection must eventually use a verified
     * manufacturer catalogue.
     */
    fun standardBreakerRatings(): List<Double>

    /**
     * ============================================================
     * PROFESSIONAL READINESS
     * ============================================================
     *
     * True only when the engine has the complete verified dataset
     * required for the professional engineering scope claimed by
     * the application.
     *
     * False means:
     *
     * - the standard is registered but incomplete
     * - one or more required datasets are missing
     * - engineering compliance cannot be claimed
     *
     * This flag is consumed by SldEngineeringContext.
     */
    fun isFullyImplemented(): Boolean

    /**
     * ============================================================
     * IMPLEMENTATION STATUS
     * ============================================================
     *
     * Returns an explicit explanation of the current dataset
     * readiness.
     *
     * This text is intended for:
     *
     * - engineering diagnostics
     * - UI status
     * - reports
     * - validation errors
     *
     * It must never claim full compliance when the dataset is
     * incomplete.
     */
    fun implementationStatus(): String
}
