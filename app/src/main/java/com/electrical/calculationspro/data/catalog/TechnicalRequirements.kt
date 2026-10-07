package com.electrical.calculationspro.data.catalog

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard

/**
 * Engineering requirements produced by the calculation layer.
 *
 * This layer contains requirements only.
 * It does not perform electrical calculations.
 * It does not contain manufacturer data.
 */
sealed interface TechnicalRequirement {

    val standard: Standard

    data class Cable(
        val requiredSectionMm2: Double,
        val requiredVoltageV: Int,
        val cores: Int,
        val material: ConductorMaterial,
        val insulation: InsulationType,
        override val standard: Standard = Standard.IEC
    ) : TechnicalRequirement

    data class Breaker(
        val requiredCurrentA: Double,
        val requiredVoltageV: Double,
        val requiredBreakingCapacityKA: Double = 0.0,
        val poles: Int = 3,
        override val standard: Standard = Standard.IEC
    ) : TechnicalRequirement

    data class Transformer(
        val requiredKva: Double,
        val primaryVoltageV: Double,
        val secondaryVoltageV: Double,
        val frequencyHz: Double = 50.0,
        override val standard: Standard = Standard.IEC
    ) : TechnicalRequirement

    data class Generator(
        val requiredKva: Double,
        val requiredVoltageV: Double = 400.0,
        val frequencyHz: Double = 50.0,
        val powerFactor: Double = 0.8,
        override val standard: Standard = Standard.IEC
    ) : TechnicalRequirement

    data class Busbar(
        val requiredCurrentA: Double,
        val requiredVoltageV: Double = 415.0,
        val requiredShortCircuitKA: Double = 0.0,
        val poles: Int = 4,
        override val standard: Standard = Standard.IEC
    ) : TechnicalRequirement

    data class Contactor(
        val requiredCurrentA: Double,
        val requiredVoltageV: Double = 400.0,
        val utilizationCategory: String = "AC-3",
        override val standard: Standard = Standard.IEC
    ) : TechnicalRequirement

    data class Panel(
        val requiredCurrentA: Double,
        val requiredVoltageV: Double = 415.0,
        val poles: Int = 4,
        override val standard: Standard = Standard.IEC
    ) : TechnicalRequirement
}
