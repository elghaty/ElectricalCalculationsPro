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

* The context is intentionally independent from the calculation

* engines. It carries project-level engineering decisions so that

* Upstream, Short Circuit, Cable Sizing, Protection Coordination

* and Panel Schedule can eventually consume the same configuration.

* 

* No engineering calculation formulas belong in this class.

* 

* ================================================================
  */
  data class SldEngineeringContext(
  
  /**
  
  * Electrical design standard selected for this calculation.
    */
    val standard: Standard = Standard.EGYPTIAN,
  
  /**
  
  * Voltage factor used by the short-circuit calculation.
  * 
  * This is a calculation input, not a hard-coded assumption
  * inside individual SLD engines.
    */
    val voltageFactor: Double = 1.05,
  
  /**
  
  * Maximum permitted voltage drop for the circuit category.
  * 
  * This remains configurable until the complete circuit-category
  * mapping is connected to StandardEngine.
    */
    val voltageDropLimitPercent: Double = 3.0,
  
  /**
  
  * Short-circuit clearing time used for thermal withstand checks.
    */
    val shortCircuitTimeSeconds: Double = 1.0,
  
  /**
  
  * Circuit category passed to the selected StandardEngine.
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
  
  * Whether the engineering study must reject incomplete
  * standard datasets.
  * 
  * This is intentionally true for professional design work.
    */
    val requireImplementedStandard: Boolean = true

) {

/**
 * Selected engineering-code engine.
 *
 * All code-dependent calculations should eventually obtain
 * their standard data through this engine.
 */
val standardEngine: StandardEngine
    get() = CodeEngineFactory.get(standard)

/**
 * Code name exposed to reports and UI.
 */
val codeName: String
    get() = standardEngine.codeName

/**
 * Code revision exposed to reports and engineering records.
 */
val codeRevision: String
    get() = standardEngine.codeRevision

/**
 * Indicates whether the selected standard has a dedicated
 * implemented dataset.
 */
val standardImplemented: Boolean
    get() = standardEngine.isFullyImplemented()

/**
 * Human-readable implementation status.
 */
val standardImplementationStatus: String
    get() = standardEngine.implementationStatus()

/**
 * Returns the standard-defined voltage-drop limit when the
 * selected code provides one.
 *
 * Until all circuit categories are fully mapped, the context
 * keeps the explicitly supplied design limit as the fallback.
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

/**
 * Returns the ambient-temperature correction factor supplied
 * by the selected standard.
 *
 * The actual conductor properties are intentionally not embedded
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

/**
 * Returns the grouping factor supplied by the selected standard.
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

/**
 * Creates a validated context.
 *
 * Validation is deliberately limited to engineering-input
 * consistency. It does not perform electrical calculations.
 */
fun validate(): SldEngineeringContext {

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

    if (
        requireImplementedStandard &&
        !standardImplemented
    ) {
        throw IllegalArgumentException(
            "Selected standard '${standard.displayName}' does not have a fully implemented verified dataset. " +
                standardImplementationStatus
        )
    }

    return this
}

/**
 * Returns a copy with another standard.
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

companion object {

    /**
     * Default context for the application's normal SLD workflow.
     *
     * Egyptian standard is selected explicitly rather than
     * silently inheriting IEC assumptions.
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
