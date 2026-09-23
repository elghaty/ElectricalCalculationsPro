package com.electrical.calculationspro.data.pumps

import kotlin.math.ceil

/**
 * Professional Pump Engineering Calculator.
 *
 * Hydraulic power:
 *
 * P_h = rho * g * Q * H
 *
 * Shaft power:
 *
 * P_shaft = P_h / eta_pump
 *
 * Motor input:
 *
 * P_motor = P_shaft / eta_motor
 *
 * Q is converted internally to m3/s.
 */

object PumpCalculator {

    private const val WATER_DENSITY_KG_PER_M3 = 1000.0
    private const val GRAVITY_M_PER_S2 = 9.80665

    private const val EPSILON = 1e-9

    fun calculate(
        input: PumpCalculationInput
    ): PumpCalculationResult {

        validate(input)

        val flowM3PerSecond =
            flowToM3PerSecond(input.flow)

        val flowM3PerHour =
            flowM3PerSecond * 3600.0

        val totalDynamicHeadM =
            input.head.totalDynamicHeadM

        val hydraulicPowerKw =
            WATER_DENSITY_KG_PER_M3 *
                GRAVITY_M_PER_S2 *
                flowM3PerSecond *
                totalDynamicHeadM /
                1000.0

        val shaftPowerKw =
            hydraulicPowerKw /
                input.pumpEfficiency

        val motorInputPowerKw =
            shaftPowerKw /
                input.motorEfficiency

        val dailyEnergyKwh =
            motorInputPowerKw *
                input.operatingHoursPerDay

        val monthlyEnergyKwh =
            motorInputPowerKw *
                input.operatingHoursPerDay *
                input.operatingDaysPerMonth

        val yearlyEnergyKwh =
            motorInputPowerKw *
                input.operatingHoursPerDay *
                input.operatingDaysPerYear

        val dailyEnergyCost =
            dailyEnergyKwh *
                input.energyTariffPerKwh

        val monthlyEnergyCost =
            monthlyEnergyKwh *
                input.energyTariffPerKwh

        val yearlyEnergyCost =
            yearlyEnergyKwh *
                input.energyTariffPerKwh

        val recommendedMotorRatingKw =
            standardMotorRating(
                motorInputPowerKw
            )

        val warnings =
            buildWarnings(
                input = input,
                motorInputPowerKw = motorInputPowerKw
            )

        val notes =
            buildList {

                add(
                    "Flow = %.3f m³/h"
                        .format(flowM3PerHour)
                )

                add(
                    "TDH = %.3f m"
                        .format(totalDynamicHeadM)
                )

                add(
                    "Hydraulic power = %.3f kW"
                        .format(hydraulicPowerKw)
                )

                add(
                    "Pump shaft power = %.3f kW"
                        .format(shaftPowerKw)
                )

                add(
                    "Motor input power = %.3f kW"
                        .format(motorInputPowerKw)
                )

                add(
                    "Recommended preliminary motor rating = %.1f kW"
                        .format(recommendedMotorRatingKw)
                )

                add(
                    "Pump selection requires a verified manufacturer Q-H curve and duty-point check."
                )

                if (input.systemType == PumpSystemType.SEWAGE) {

                    add(
                        "Sewage application requires verification of solids passage, impeller type and clogging characteristics."
                    )
                }
            }

        return PumpCalculationResult(
            systemType = input.systemType,

            flowM3PerSecond =
                flowM3PerSecond,

            flowM3PerHour =
                flowM3PerHour,

            totalDynamicHeadM =
                totalDynamicHeadM,

            hydraulicPowerKw =
                hydraulicPowerKw,

            shaftPowerKw =
                shaftPowerKw,

            motorInputPowerKw =
                motorInputPowerKw,

            dailyEnergyKwh =
                dailyEnergyKwh,

            monthlyEnergyKwh =
                monthlyEnergyKwh,

            yearlyEnergyKwh =
                yearlyEnergyKwh,

            dailyEnergyCost =
                dailyEnergyCost,

            monthlyEnergyCost =
                monthlyEnergyCost,

            yearlyEnergyCost =
                yearlyEnergyCost,

            recommendedMotorRatingKw =
                recommendedMotorRatingKw,

            valid =
                warnings.none {
                    it.startsWith("ERROR:")
                },

            warnings =
                warnings,

            notes =
                notes
        )
    }

    fun flowToM3PerSecond(
        flow: PumpFlow
    ): Double {

        require(flow.value >= 0.0) {
            "Flow cannot be negative."
        }

        return when (flow.unit) {

            PumpFlowUnit.LITERS_PER_SECOND ->
                flow.value / 1000.0

            PumpFlowUnit.CUBIC_METERS_PER_HOUR ->
                flow.value / 3600.0

            PumpFlowUnit.CUBIC_METERS_PER_SECOND ->
                flow.value

            PumpFlowUnit.LITERS_PER_MINUTE ->
                flow.value / 60000.0
        }
    }

    fun flowToM3PerHour(
        flow: PumpFlow
    ): Double =
        flowToM3PerSecond(flow) * 3600.0

    private fun standardMotorRating(
        requiredKw: Double
    ): Double {

        if (requiredKw <= EPSILON) {
            return 0.0
        }

        val standardRatings =
            listOf(
                0.37,
                0.55,
                0.75,
                1.1,
                1.5,
                2.2,
                3.0,
                4.0,
                5.5,
                7.5,
                11.0,
                15.0,
                18.5,
                22.0,
                30.0,
                37.0,
                45.0,
                55.0,
                75.0,
                90.0,
                110.0,
                132.0,
                160.0,
                200.0,
                250.0,
                315.0,
                355.0,
                400.0,
                450.0,
                500.0
            )

        return standardRatings
            .firstOrNull {
                it >= requiredKw
            }
            ?: ceil(requiredKw)
    }

    private fun buildWarnings(
        input: PumpCalculationInput,
        motorInputPowerKw: Double
    ): List<String> {

        val warnings =
            mutableListOf<String>()

        if (input.systemType == PumpSystemType.SEWAGE) {

            warnings.add(
                "Sewage pump selection must verify solids handling and impeller suitability."
            )
        }

        if (motorInputPowerKw > 0.0) {

            warnings.add(
                "Motor rating is preliminary and must be checked against the selected manufacturer's motor data."
            )
        }

        if (input.operatingHoursPerDay <= 0.0) {

            warnings.add(
                "Energy consumption is zero because operating hours/day is zero."
            )
        }

        if (input.energyTariffPerKwh < 0.0) {

            warnings.add(
                "ERROR: Energy tariff cannot be negative."
            )
        }

        return warnings
    }

    private fun validate(
        input: PumpCalculationInput
    ) {

        require(input.flow.value >= 0.0) {
            "Flow cannot be negative."
        }

        require(
            input.head.staticHeadM >= 0.0
        ) {
            "Static head cannot be negative."
        }

        require(
            input.head.frictionHeadM >= 0.0
        ) {
            "Friction head cannot be negative."
        }

        require(
            input.head.minorLossHeadM >= 0.0
        ) {
            "Minor-loss head cannot be negative."
        }

        require(
            input.head.requiredPressureHeadM >= 0.0
        ) {
            "Required pressure head cannot be negative."
        }

        require(
            input.pumpEfficiency > 0.0 &&
                input.pumpEfficiency <= 1.0
        ) {
            "Pump efficiency must be greater than 0 and not greater than 1."
        }

        require(
            input.motorEfficiency > 0.0 &&
                input.motorEfficiency <= 1.0
        ) {
            "Motor efficiency must be greater than 0 and not greater than 1."
        }

        require(
            input.operatingHoursPerDay >= 0.0 &&
                input.operatingHoursPerDay <= 24.0
        ) {
            "Operating hours/day must be between 0 and 24."
        }

        require(
            input.operatingDaysPerMonth >= 0.0
        ) {
            "Operating days/month cannot be negative."
        }

        require(
            input.operatingDaysPerYear >= 0.0
        ) {
            "Operating days/year cannot be negative."
        }

        require(
            input.energyTariffPerKwh >= 0.0
        ) {
            "Energy tariff cannot be negative."
        }
    }
}
