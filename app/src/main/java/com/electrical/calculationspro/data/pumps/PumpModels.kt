package com.electrical.calculationspro.data.pumps

/**
 * Professional Pump Engineering Models
 *
 * Models only.
 * No UI.
 * No Compose.
 * No catalog assumptions.
 */

enum class PumpSystemType {
    WATER,
    SEWAGE
}

enum class PumpFlowUnit {
    LITERS_PER_SECOND,
    CUBIC_METERS_PER_HOUR,
    CUBIC_METERS_PER_SECOND,
    LITERS_PER_MINUTE
}

enum class PumpHeadUnit {
    METER
}

data class PumpFlow(
    val value: Double,
    val unit: PumpFlowUnit
)

data class PumpHead(
    val staticHeadM: Double = 0.0,
    val frictionHeadM: Double = 0.0,
    val minorLossHeadM: Double = 0.0,
    val requiredPressureHeadM: Double = 0.0
) {

    val totalDynamicHeadM: Double
        get() =
            staticHeadM +
                frictionHeadM +
                minorLossHeadM +
                requiredPressureHeadM
}

data class PumpEfficiency(
    val pumpEfficiency: Double,
    val motorEfficiency: Double
)

data class PumpEnergyInput(
    val operatingHoursPerDay: Double = 0.0,
    val operatingDaysPerMonth: Double = 30.0,
    val operatingDaysPerYear: Double = 365.0,
    val energyTariffPerKwh: Double = 0.0
)

data class PumpCalculationInput(
    val systemType: PumpSystemType,
    val flow: PumpFlow,
    val head: PumpHead,
    val pumpEfficiency: Double,
    val motorEfficiency: Double,
    val operatingHoursPerDay: Double = 0.0,
    val operatingDaysPerMonth: Double = 30.0,
    val operatingDaysPerYear: Double = 365.0,
    val energyTariffPerKwh: Double = 0.0
)

data class PumpCalculationResult(
    val systemType: PumpSystemType,

    val flowM3PerSecond: Double,
    val flowM3PerHour: Double,
    val totalDynamicHeadM: Double,

    val hydraulicPowerKw: Double,
    val shaftPowerKw: Double,
    val motorInputPowerKw: Double,

    val dailyEnergyKwh: Double,
    val monthlyEnergyKwh: Double,
    val yearlyEnergyKwh: Double,

    val dailyEnergyCost: Double,
    val monthlyEnergyCost: Double,
    val yearlyEnergyCost: Double,

    val recommendedMotorRatingKw: Double,

    val valid: Boolean,

    val warnings: List<String> = emptyList(),
    val notes: List<String> = emptyList()
)
