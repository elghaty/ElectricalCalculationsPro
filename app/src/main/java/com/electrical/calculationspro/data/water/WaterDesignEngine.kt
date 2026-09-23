package com.electrical.calculationspro.data.water

import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult
import com.electrical.calculationspro.data.pumps.PumpFlow
import com.electrical.calculationspro.data.pumps.PumpFlowUnit
import com.electrical.calculationspro.data.pumps.PumpHead
import com.electrical.calculationspro.data.pumps.PumpSystemType
import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.WaterPump

object WaterDesignEngine {

    fun calculateTdh(
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double,
        requiredPressureHeadM: Double
    ): Double {

        require(staticHeadM >= 0.0)
        require(frictionHeadM >= 0.0)
        require(minorLossHeadM >= 0.0)
        require(requiredPressureHeadM >= 0.0)

        return staticHeadM +
            frictionHeadM +
            minorLossHeadM +
            requiredPressureHeadM
    }

    fun calculatePump(
        pump: WaterPump,
        defaultFlowM3PerHour: Double,
        defaultHeadM: Double
    ): PumpCalculationResult {

        val flow =
            pump.flowM3PerHour
                .takeIf { it > 0.0 }
                ?: defaultFlowM3PerHour

        val head =
            pump.headM
                .takeIf { it > 0.0 }
                ?: defaultHeadM

        return com.electrical.calculationspro.data.pumps.PumpCalculator.calculate(
            PumpCalculationInput(
                systemType = PumpSystemType.WATER,
                flow = PumpFlow(
                    value = flow,
                    unit = PumpFlowUnit.CUBIC_METERS_PER_HOUR
                ),
                head = PumpHead(
                    staticHeadM = head
                ),
                pumpEfficiency =
                    pump.pumpEfficiency,
                motorEfficiency =
                    pump.motorEfficiency
            )
        )
    }

    fun recalculate(
        project: DesignProject
    ): DesignProject {

        val water = project.water

        val tdh =
            calculateTdh(
                staticHeadM = water.staticHeadM,
                frictionHeadM = water.frictionHeadM,
                minorLossHeadM = water.minorLossHeadM,
                requiredPressureHeadM =
                    water.requiredPressureHeadM
            )

        val pumps =
            water.pumps.map { pump ->

                if (
                    pump.pumpEfficiency <= 0.0 ||
                    pump.motorEfficiency <= 0.0
                ) {
                    pump.copy(
                        status =
                            DesignCalculationStatus.DATA_INCOMPLETE
                    )
                } else {

                    val result =
                        calculatePump(
                            pump = pump,
                            defaultFlowM3PerHour =
                                water.requiredFlowM3PerHour,
                            defaultHeadM = tdh
                        )

                    pump.copy(
                        flowM3PerHour =
                            result.flowM3PerHour,
                        headM =
                            result.totalDynamicHeadM,
                        motorPowerKw =
                            result.motorInputPowerKw,
                        yearlyEnergyKwh =
                            result.yearlyEnergyKwh,
                        status =
                            if (result.valid) {
                                DesignCalculationStatus.CALCULATED
                            } else {
                                DesignCalculationStatus.INVALID
                            }
                    )
                }
            }

        val status =
            when {
                water.requiredFlowM3PerHour <= 0.0 ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                pumps.any {
                    it.status ==
                        DesignCalculationStatus.INVALID
                } ->
                    DesignCalculationStatus.INVALID

                pumps.any {
                    it.status ==
                        DesignCalculationStatus.DATA_INCOMPLETE
                } ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                else ->
                    DesignCalculationStatus.CALCULATED
            }

        return project.withWater(
            water.copy(
                tdhM = tdh,
                pumps = pumps,
                status = status
            )
        )
    }
}
