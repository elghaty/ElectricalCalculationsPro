package com.electrical.calculationspro.data

import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.standards.StandardEngine

/**
 * ================================================================
 * SLD ENGINEERING CONTEXT
 * ================================================================
 *
 * Shared engineering configuration for one SLD calculation cycle.
 *
 * Architecture:
 *
 * Android / SLD UI
 *        |
 *        v
 * SldEngineeringFacade
 *        |
 *        v
 * SldEngineeringContext
 *        |
 *        v
 * StandardEngine
 *
 * This class contains engineering configuration only.
 *
 * NO electrical calculation formulas belong here.
 *
 * IMPORTANT PROFESSIONAL-DESIGN RULE
 *
 * An engineering calculation must NEVER use an incomplete or
 * unverified standard dataset.
 *
 * The standard-readiness gate is therefore enforced by this class
 * itself and cannot be disabled by copying the context with
 * requireImplementedStandard = false.
 *
 * The legacy requireImplementedStandard property is retained for
 * source compatibility with existing callers, but it is no longer
 * allowed to weaken professional engineering validation.
 *
 * ================================================================
 */
data class SldEngineeringContext(

    /**
     * Electrical design standard selected for this calculation.
     */
    val standard: Standard = Standard.EGYPTIAN,

    /**
     * Voltage factor used by short-circuit calculations.
     *
     * This is an engineering input and is intentionally not
     * hard-coded inside individual calculation engines.
     */
    val voltageFactor: Double = 1.05,

    /**
     * Maximum permitted voltage drop for the circuit category.
     *
     * The selected StandardEngine may provide a more specific
     * standard-defined limit. This value remains the fallback.
     */
    val voltageDropLimitPercent: Double = 3.0,

    /**
     * Short-circuit clearing time used for thermal withstand checks.
     */
    val shortCircuitTimeSeconds: Double = 1.0,

    /**
     * Circuit category passed to StandardEngine.
     *
     * Examples:
     * - GENERAL
     * - LIGHTING
     * - MOTOR
     * - FEEDER
     */
    val circuitCategory: String = "FEEDER",

    /**
     * Ambient design temperature.
     */
    val ambientTemperatureC: Double = 30.0,

    /**
     * Number of loaded circuits used for grouping calculations.
     */
    val numberOfCircuits: Int = 1,

    /**
     * Legacy compatibility property.
     *
     * IMPORTANT:
     *
     * This property MUST NOT be used to bypass standard readiness.
     *
     * Existing code may still create:
     *
     * copy(requireImplementedStandard = false)
     *
     * but validate() and validateForEngineering() always enforce
     * standard readiness.
     *
     * This keeps source compatibility while closing the professional
     * engineering validation bypass.
     */
    val requireImplementedStandard: Boolean = true

) {

    // ============================================================
    // STANDARD ENGINE
    // ============================================================

    /**
     * Selected engineering-code engine.
     *
     * All code-dependent calculations must obtain their
     * standard-dependent data through this engine.
     */
    val standardEngine: StandardEngine
        get() =
            CodeEngineFactory.get(
                standard
            )

    /**
     * Code name exposed to reports and UI.
     */
    val codeName: String
        get() =
            standardEngine.codeName

    /**
     * Code revision exposed to reports and engineering records.
     */
    val codeRevision: String
        get() =
            standardEngine.codeRevision

    /**
     * Indicates whether the selected standard has a fully
     * implemented and verified engineering dataset.
     */
    val standardImplemented: Boolean
        get() =
            standardEngine.isFullyImplemented()

    /**
     * Human-readable implementation status.
     */
    val standardImplementationStatus: String
        get() =
            standardEngine.implementationStatus()

    // ============================================================
    // VOLTAGE DROP
    // ============================================================

    /**
     * Returns the standard-defined voltage-drop limit when the
     * selected StandardEngine provides one.
     *
     * Otherwise the explicitly supplied context value is used.
     *
     * IMPORTANT:
     *
     * This method does NOT mean that an incomplete standard is
     * acceptable. Standard readiness is checked separately by
     * validateForEngineering().
     */
    fun effectiveVoltageDropLimitPercent(): Double {

        require(
            voltageDropLimitPercent > 0.0
        ) {
            "Voltage drop limit must be greater than zero."
        }

        val standardLimit =
            standardEngine.maximumVoltageDropPercent(
                circuitCategory
            )

        return if (
            standardLimit > 0.0
        ) {
            standardLimit
        } else {
            voltageDropLimitPercent
        }
    }

    // ============================================================
    // AMBIENT TEMPERATURE
    // ============================================================

    /**
     * Returns the ambient-temperature correction factor supplied
     * by the selected standard.
     *
     * No conductor properties or correction tables are embedded
     * in this context.
     */
    fun ambientTemperatureFactor(
        insulation: InsulationType
    ): Double {

        return standardEngine.ambientTemperatureFactor(
            insulation = insulation,
            ambientTemperatureC = ambientTemperatureC
        )
    }

    // ============================================================
    // GROUPING
    // ============================================================

    /**
     * Returns the grouping factor supplied by the selected
     * standard.
     */
    fun groupingFactor(): Double {

        require(
            numberOfCircuits >= 1
        ) {
            "Number of circuits must be at least 1."
        }

        return standardEngine.groupingFactor(
            numberOfCircuits = numberOfCircuits
        )
    }

    // ============================================================
    // BASIC INPUT VALIDATION
    // ============================================================

    /**
     * Validates engineering inputs.
     *
     * This method intentionally performs NO electrical calculations.
     *
     * IMPORTANT:
     *
     * Standard readiness is ALWAYS enforced.
     *
     * The legacy requireImplementedStandard flag is deliberately
     * ignored as a bypass mechanism.
     *
     * This means all existing calls such as:
     *
     * engineeringContext
     *     .copy(requireImplementedStandard = false)
     *     .validate()
     *
     * are still source-compatible but will correctly reject an
     * incomplete standard dataset.
     */
    fun validate(): SldEngineeringContext {

        validateEngineeringInputs()

        requireStandardReadiness()

        return this
    }

    /**
     * Explicit professional-engineering validation entry point.
     *
     * New engineering code should call this method when it needs
     * to make the intent clear.
     *
     * Unlike editor/preview state, this method represents an actual
     * engineering calculation request.
     */
    fun validateForEngineering(): SldEngineeringContext {

        validateEngineeringInputs()

        requireStandardReadiness()

        return this
    }

    /**
     * Validates only the numerical/configuration inputs.
     *
     * This method exists so future editor-preview workflows can
     * validate basic user input without pretending that a standard
     * dataset is engineering-ready.
     *
     * It MUST NOT be used as authorization to run professional
     * engineering calculations.
     */
    fun validateInputsOnly(): SldEngineeringContext {

        validateEngineeringInputs()

        return this
    }

    /**
     * Internal validation of context values.
     *
     * No standard readiness decision is made here.
     */
    private fun validateEngineeringInputs() {

        require(
            voltageFactor > 0.0
        ) {
            "Voltage factor must be greater than zero."
        }

        require(
            voltageDropLimitPercent > 0.0
        ) {
            "Voltage drop limit must be greater than zero."
        }

        require(
            shortCircuitTimeSeconds > 0.0
        ) {
            "Short-circuit clearing time must be greater than zero."
        }

        require(
            circuitCategory.isNotBlank()
        ) {
            "Circuit category must not be blank."
        }

        require(
            ambientTemperatureC > -50.0
        ) {
            "Ambient temperature is outside the supported engineering range."
        }

        require(
            numberOfCircuits >= 1
        ) {
            "Number of circuits must be at least 1."
        }
    }

    /**
     * HARD PROFESSIONAL STANDARD GATE
     *
     * This method intentionally contains NO condition based on
     * requireImplementedStandard.
     *
     * Therefore there is no lower-level switch that can disable
     * standard readiness for professional engineering.
     */
    private fun requireStandardReadiness() {

        require(
            standardImplemented
        ) {
            buildStandardReadinessError()
        }
    }

    /**
     * Creates one consistent standard-readiness error message.
     *
     * Keeping the message here prevents the different SLD engines
     * from generating contradictory readiness messages.
     */
    private fun buildStandardReadinessError(): String {

        return buildString {

            append(
                "Selected standard '"
            )

            append(
                standard.displayName
            )

            append(
                "' is not ready for professional engineering calculation. "
            )

            append(
                "The selected standard does not have a fully implemented "
            )

            append(
                "and verified engineering dataset. "
            )

            append(
                standardImplementationStatus
            )
        }
    }

    // ============================================================
    // COPY HELPERS
    // ============================================================

    /**
     * Returns a copy with another standard.
     *
     * The returned context still requires standard readiness before
     * professional engineering calculations.
     */
    fun withStandard(
        value: Standard
    ): SldEngineeringContext =
        copy(
            standard = value
        )

    /**
     * Returns a copy with another voltage-drop limit.
     */
    fun withVoltageDropLimit(
        value: Double
    ): SldEngineeringContext =
        copy(
            voltageDropLimitPercent = value
        )

    /**
     * Returns a copy with another short-circuit clearing time.
     */
    fun withShortCircuitTime(
        value: Double
    ): SldEngineeringContext =
        copy(
            shortCircuitTimeSeconds = value
        )

    /**
     * Returns a copy with another circuit category.
     */
    fun withCircuitCategory(
        value: String
    ): SldEngineeringContext =
        copy(
            circuitCategory = value
        )

    /**
     * Returns a copy with another ambient temperature.
     */
    fun withAmbientTemperature(
        value: Double
    ): SldEngineeringContext =
        copy(
            ambientTemperatureC = value
        )

    /**
     * Returns a copy with another grouping-circuit count.
     */
    fun withNumberOfCircuits(
        value: Int
    ): SldEngineeringContext =
        copy(
            numberOfCircuits = value
        )

    // ============================================================
    // COMPANION
    // ============================================================

    companion object {

        /**
         * Default context for the normal SLD engineering workflow.
         *
         * Egyptian standard is selected explicitly.
         *
         * IMPORTANT:
         *
         * The default context does NOT claim that the Egyptian
         * dataset is fully implemented. Therefore the context will
         * correctly reject a professional calculation until the
         * selected StandardEngine reports a fully implemented
         * verified dataset.
         */
        fun default(): SldEngineeringContext =
            SldEngineeringContext(
                standard =
                    Standard.EGYPTIAN,

                voltageFactor =
                    1.05,

                voltageDropLimitPercent =
                    3.0,

                shortCircuitTimeSeconds =
                    1.0,

                circuitCategory =
                    "FEEDER",

                ambientTemperatureC =
                    30.0,

                numberOfCircuits =
                    1,

                requireImplementedStandard =
                    true
            )
    }
}
