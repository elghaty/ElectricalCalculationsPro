package com.electrical.calculationspro.data.sewage

import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpFlow
import com.electrical.calculationspro.data.pumps.PumpFlowUnit
import com.electrical.calculationspro.data.pumps.PumpHead
import com.electrical.calculationspro.data.pumps.PumpSystemType
import com.electrical.calculationspro.data.pumps.PumpCalculator
import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject

object SewageDesignEngine {

    fun calculateTdh(
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double
    ): Double {

        require(staticHeadM >= 0.0)
        require(frictionHeadM >= 0.0)
        require(minorLossHeadM >= 0.0)

        return staticHeadM +
            frictionHeadM +
            minorLossHeadM
    }

    fun recalculate(
        project: DesignProject
    ): DesignProject {

        val sewage =
            project.sewage

        val risingMain =
            sewage.risingMain

        val frictionHead =
            risingMain?.frictionLossM
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val minorLossHead =
            risingMain?.minorLossHeadM
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val tdh =
            calculateTdh(
                staticHeadM = sewage.staticHeadM,
                frictionHeadM = frictionHead,
                minorLossHeadM = minorLossHead
            )

        val pumps =
            sewage.pumps.map { pump ->

                val flow =
                    pump.flowM3PerHour

                if (
                    flow <= 0.0 ||
                    tdh <= 0.0 ||
                    pump.pumpEfficiency <= 0.0 ||
                    pump.motorEfficiency <= 0.0
                ) {
                    pump.copy(
                        status =
                            DesignCalculationStatus.DATA_INCOMPLETE
                    )
                } else {

                    val result =
                        PumpCalculator.calculate(
                            PumpCalculationInput(
                                systemType =
                                    PumpSystemType.SEWAGE,
                                flow =
                                    PumpFlow(
                                        value = flow,
                                        unit =
                                            PumpFlowUnit
                                                .CUBIC_METERS_PER_HOUR
                                    ),
                                head =
                                    PumpHead(
                                        staticHeadM =
                                            tdh
                                    ),
                                pumpEfficiency =
                                    pump.pumpEfficiency,
                                motorEfficiency =
                                    pump.motorEfficiency
                            )
                        )

                    pump.copy(
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
                sewage.averageFlowM3PerDay <= 0.0 ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                sewage.peakFlowM3PerDay <
                    sewage.averageFlowM3PerDay ->
                    DesignCalculationStatus.INVALID

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

        return project.withSewage(
            sewage.copy(
                tdhM = tdh,
                pumps = pumps,
                status = status
            )
        )
    }
}
