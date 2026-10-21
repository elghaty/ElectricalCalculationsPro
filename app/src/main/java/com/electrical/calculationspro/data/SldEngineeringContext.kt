
package com.electrical.calculationspro.data

import com.electrical.calculationspro.data.standards.CodeEngineFactory
import com.electrical.calculationspro.data.standards.StandardEngine

/**
 * Shared engineering configuration for the SLD calculation cycle.
 *
 * Architecture:
 *
 * SLD UI
 *   -> SldEngineeringFacade
 *   -> SldEngineeringContext
 *   -> StandardEngine
 *
 * This class contains configuration and validation only.
 * Electrical calculation formulas belong to engineering engines.
 *
 * Professional calculations require a fully implemented and
 * verified engineering-standard dataset.
 *
 * The legacy requireImplementedStandard property is retained for
 * source compatibility. It cannot bypass standard readiness.
 */
data class SldEngineeringContext(

    val standard: Standard = Standard.EGYPTIAN,

    val voltageFactor: Double = 1.05,

    val voltageDropLimitPercent: Double = 3.0,

    val shortCircuitTimeSeconds: Double = 1.0,

    val circuitCategory: String = "FEEDER",

    val ambientTemperatureC: Double = 30.0,

    val numberOfCircuits: Int = 1,

    /**
     * Legacy compatibility property.
     *
     * This value never disables standard-readiness validation.
     */
    val requireImplementedStandard: Boolean = true

) {

    /**
     * Selected engineering-code engine.
     */
    val standardEngine: StandardEngine
        get() = CodeEngineFactory.get(standard)

    /**
     * Code name exposed to reports and UI.
     */
    val codeName: String
        get() = standardEngine.codeName

    /**
     * Code revision exposed to engineering records.
     */
    val codeRevision: String
        get() = standardEngine.codeRevision

    /**
     * True only when the selected standard reports a fully
     * implemented engineering dataset.
     */
    val standardImplemented: Boolean
        get() = standardEngine.isFullyImplemented()

    /**
     * Human-readable standard implementation status.
     */
    val standardImplementationStatus: String
        get() = standardEngine.implementationStatus()

    /**
     * Resolves the effective voltage-drop limit.
     *
     * A valid positive standard-specific limit takes precedence
     * over the explicitly configured fallback.
     */
    fun effectiveVoltageDropLimitPercent(): Double {
        validateFinitePositive(
            value = voltageDropLimitPercent,
            fieldName = "Voltage drop limit"
        )

        val standardLimit =
            standardEngine.maximumVoltageDropPercent(
                circuitCategory
            )

        return if (
            standardLimit.isFinite() &&
            standardLimit > 0.0
        ) {
            standardLimit
        } else {
            voltageDropLimitPercent
        }
    }

    /**
     * Returns the ambient-temperature correction factor provided
     * by the selected engineering standard.
     *
     * No correction tables are embedded in this context.
     */
    fun ambientTemperatureFactor(
        insulation: InsulationType
    ): Double {
        validateEngineeringInputs()

        requireStandardReadiness()

        val factor =
            standardEngine.ambientTemperatureFactor(
                insulation = insulation,
                ambientTemperatureC = ambientTemperatureC
            )

        require(
            factor.isFinite() && factor > 0.0
        ) {
            "The selected standard returned an invalid ambient-temperature correction factor."
        }

        return factor
    }

    /**
     * Returns the grouping factor provided by the selected standard.
     */
    fun groupingFactor(): Double {
        validateEngineeringInputs()

        requireStandardReadiness()

        val factor =
            standardEngine.groupingFactor(
                numberOfCircuits = numberOfCircuits
            )

        require(
            factor.isFinite() &&
                factor > 0.0 &&
                factor <= 1.0
        ) {
            "The selected standard returned an invalid grouping factor."
        }

        return factor
    }

    /**
     * Validates all inputs and confirms that the selected standard
     * is ready for professional engineering calculations.
     */
    fun validate(): SldEngineeringContext {
        validateEngineeringInputs()
        requireStandardReadiness()
        return this
    }

    /**
     * Explicit validation entry point for actual engineering studies.
     */
    fun validateForEngineering(): SldEngineeringContext {
        validateEngineeringInputs()
        requireStandardReadiness()
        return this
    }

    /**
     * Validates configuration values without authorizing engineering
     * calculations against an incomplete standard.
     */
    fun validateInputsOnly(): SldEngineeringContext {
        validateEngineeringInputs()
        return this
    }

    /**
     * Validates numerical and configuration inputs.
     *
     * All floating-point values are checked for finiteness.
     * Positive infinity and NaN must never pass engineering checks.
     */
    private fun validateEngineeringInputs() {
        validateFinitePositive(
            value = voltageFactor,
            fieldName = "Voltage factor"
        )

        validateFinitePositive(
            value = voltageDropLimitPercent,
            fieldName = "Voltage drop limit"
        )

        validateFinitePositive(
            value = shortCircuitTimeSeconds,
            fieldName = "Short-circuit clearing time"
        )

        require(
            circuitCategory.isNotBlank()
        ) {
            "Circuit category must not be blank."
        }

        require(
            ambientTemperatureC.isFinite() &&
                ambientTemperatureC > -50.0 &&
                ambientTemperatureC <= 200.0
        ) {
            "Ambient temperature must be finite and within the supported range (-50 to 200 °C)."
        }

        require(
            numberOfCircuits >= 1
        ) {
            "Number of circuits must be at least 1."
        }
    }

    /**
     * Shared finite-positive input validation.
     */
    private fun validateFinitePositive(
        value: Double,
        fieldName: String
    ) {
        require(
            value.isFinite() && value > 0.0
        ) {
            "$fieldName must be finite and greater than zero."
        }
    }

    /**
     * Hard professional-standard readiness gate.
     *
     * This check deliberately ignores requireImplementedStandard.
     */
    private fun requireStandardReadiness() {
        require(
            standardImplemented
        ) {
            buildStandardReadinessError()
        }
    }

    /**
     * Builds a consistent error message for an incomplete dataset.
     */
    private fun buildStandardReadinessError(): String {
        return buildString {
            append("Selected standard '")
            append(standard.displayName)
            append(
                "' is not ready for professional engineering calculation. "
            )
            append(
                "The selected standard does not have a fully implemented "
            )
            append(
                "and verified engineering dataset. "
            )
            append(standardImplementationStatus)
        }
    }

    /**
     * Returns a copy configured for another engineering standard.
     */
    fun withStandard(
        value: Standard
    ): SldEngineeringContext =
        copy(standard = value)

    /**
     * Returns a copy with another voltage-drop limit.
     */
    fun withVoltageDropLimit(
        value: Double
    ): SldEngineeringContext =
        copy(voltageDropLimitPercent = value)

    /**
     * Returns a copy with another short-circuit clearing time.
     */
    fun withShortCircuitTime(
        value: Double
    ): SldEngineeringContext =
        copy(shortCircuitTimeSeconds = value)

    /**
     * Returns a copy with another circuit category.
     */
    fun withCircuitCategory(
        value: String
    ): SldEngineeringContext =
        copy(circuitCategory = value)

    /**
     * Returns a copy with another ambient temperature.
     */
    fun withAmbientTemperature(
        value: Double
    ): SldEngineeringContext =
        copy(ambientTemperatureC = value)

    /**
     * Returns a copy with another grouping-circuit count.
     */
    fun withNumberOfCircuits(
        value: Int
    ): SldEngineeringContext =
        copy(numberOfCircuits = value)

    companion object {

        /**
         * Default configuration for the SLD engineering workflow.
         *
         * Selecting the Egyptian standard does not imply that its
         * engineering dataset is complete. The readiness gate remains
         * authoritative.
         */
        fun default(): SldEngineeringContext =
            SldEngineeringContext(
                standard = Standard.EGYPTIAN,
                voltageFactor = 1.05,
                voltageDropLimitPercent = 3.0,
                shortCircuitTimeSeconds = 1.0,
                circuitCategory = "FEEDER",
                ambientTemperatureC = 30.0,
                numberOfCircuits = 1,
                requireImplementedStandard = true
            )
    }
}
