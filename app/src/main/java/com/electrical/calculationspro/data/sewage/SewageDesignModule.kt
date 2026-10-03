package com.electrical.calculationspro.data.sewage

import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectEngine
import com.electrical.calculationspro.data.project.RisingMainDesign
import com.electrical.calculationspro.data.project.SewagePump
import com.electrical.calculationspro.data.project.WetWellDesign

object SewageDesignModule {

    fun updateFlows(
        project: DesignProject,
        averageFlowM3PerDay: Double,
        peakFlowM3PerDay: Double,
        minimumFlowM3PerDay: Double
    ): DesignProject {

        val sewage =
            project.sewage.copy(
                averageFlowM3PerDay =
                    averageFlowM3PerDay.coerceAtLeast(0.0),
                peakFlowM3PerDay =
                    peakFlowM3PerDay.coerceAtLeast(0.0),
                minimumFlowM3PerDay =
                    minimumFlowM3PerDay.coerceAtLeast(0.0),
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjectEngine.recalculateSewage(
            project.withSewage(sewage)
        )
    }

    fun setStaticHead(
        project: DesignProject,
        staticHeadM: Double
    ): DesignProject {

        val sewage =
            project.sewage.copy(
                staticHeadM =
                    staticHeadM.coerceAtLeast(0.0),
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjectEngine.recalculateSewage(
            project.withSewage(sewage)
        )
    }

    fun setWetWell(
        project: DesignProject,
        wetWell: WetWellDesign
    ): DesignProject {

        val sewage =
            project.sewage.copy(
                wetWell = wetWell,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjectEngine.recalculateSewage(
            project.withSewage(sewage)
        )
    }

    fun setRisingMain(
        project: DesignProject,
        risingMain: RisingMainDesign
    ): DesignProject {

        val sewage =
            project.sewage.copy(
                risingMain = risingMain,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjectEngine.recalculateSewage(
            project.withSewage(sewage)
        )
    }

    fun addPump(
        project: DesignProject,
        pump: SewagePump
    ): DesignProject {

        val sewage =
            project.sewage.copy(
                pumps =
                    project.sewage.pumps
                        .filterNot { it.id == pump.id } +
                        pump,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjectEngine.recalculateSewage(
            project.withSewage(sewage)
        )
    }

    fun recalculate(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateSewage(
            project
        )

    fun updateRisingMainCalculatedValues(
        project: DesignProject,
        flowM3PerHour: Double,
        velocityMPerS: Double,
        frictionLossM: Double,
        minorLossHeadM: Double
    ): DesignProject {

        val current =
            project.sewage.risingMain

        val updated =
            current?.copy(
                flowM3PerHour =
                    flowM3PerHour.coerceAtLeast(0.0),
                velocityMPerS =
                    velocityMPerS.coerceAtLeast(0.0),
                frictionLossM =
                    frictionLossM.coerceAtLeast(0.0),
                minorLossHeadM =
                    minorLossHeadM.coerceAtLeast(0.0),
                status =
                    DesignCalculationStatus.CALCULATED
            )

        return if (updated == null) {
            project
        } else {
            DesignProjectEngine.recalculateSewage(
                project.withSewage(
                    project.sewage.copy(
                        risingMain = updated
                    )
                )
            )
        }
    }

    fun updatePumpCalculatedValues(
        project: DesignProject,
        pumpId: String,
        motorPowerKw: Double,
        yearlyEnergyKwh: Double
    ): DesignProject {

        val pumps =
            project.sewage.pumps.map { pump ->

                if (pump.id != pumpId) {
                    pump
                } else {
                    pump.copy(
                        motorPowerKw =
                            motorPowerKw.coerceAtLeast(0.0),
                        yearlyEnergyKwh =
                            yearlyEnergyKwh.coerceAtLeast(0.0),
                        status =
                            DesignCalculationStatus.CALCULATED
                    )
                }
            }

        return DesignProjectEngine.recalculateSewage(
            project.withSewage(
                project.sewage.copy(
                    pumps = pumps
                )
            )
        )
    }

    fun markInProgress(
        project: DesignProject
    ): DesignProject =
        project.withSewage(
            project.sewage.copy(
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )
        )

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        project.withSewage(
            project.sewage.copy(
                status =
                    DesignCalculationStatus.CALCULATED
            )
        )

    fun markDataIncomplete(
        project: DesignProject
    ): DesignProject =
        project.withSewage(
            project.sewage.copy(
                status =
                    DesignCalculationStatus.DATA_INCOMPLETE
            )
        )

    fun markInvalid(
        project: DesignProject
    ): DesignProject =
        project.withSewage(
            project.sewage.copy(
                status =
                    DesignCalculationStatus.INVALID
            )
        )
}
