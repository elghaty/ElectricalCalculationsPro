package com.electrical.calculationspro.data.sewage

import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects
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

        return DesignProjects.save(
            project.withSewage(
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
            )
        )
    }

    fun setStaticHead(
        project: DesignProject,
        staticHeadM: Double
    ): DesignProject {

        return DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    staticHeadM =
                        staticHeadM.coerceAtLeast(0.0),
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun setWetWell(
        project: DesignProject,
        wetWell: WetWellDesign
    ): DesignProject {

        return DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    wetWell = wetWell,
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun setRisingMain(
        project: DesignProject,
        risingMain: RisingMainDesign
    ): DesignProject {

        return DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    risingMain = risingMain,
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun addPump(
        project: DesignProject,
        pump: SewagePump
    ): DesignProject {

        val pumps =
            project.sewage.pumps
                .filterNot { it.id == pump.id } +
                pump

        return DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    pumps = pumps,
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun recalculate(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateSewage(project)

    fun updateRisingMainCalculatedValues(
        project: DesignProject,
        risingMainId: String,
        flowM3PerHour: Double,
        velocityMPerS: Double,
        frictionLossM: Double,
        minorLossHeadM: Double
    ): DesignProject {

        if (
            !flowM3PerHour.isFinite() ||
            !velocityMPerS.isFinite() ||
            !frictionLossM.isFinite() ||
            !minorLossHeadM.isFinite()
        ) {
            return project
        }

        val main =
            project.sewage.risingMain
                ?.takeIf {
                    it.id == risingMainId
                }
                ?: return project

        return DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    risingMain =
                        main.copy(
                            flowM3PerHour =
                                flowM3PerHour,
                            velocityMPerS =
                                velocityMPerS,
                            frictionLossM =
                                frictionLossM,
                            minorLossHeadM =
                                minorLossHeadM,
                            status =
                                DesignCalculationStatus.CALCULATED
                        ),
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun updatePumpCalculatedValues(
        project: DesignProject,
        pumpId: String,
        motorPowerKw: Double,
        yearlyEnergyKwh: Double
    ): DesignProject {

        if (
            !motorPowerKw.isFinite() ||
            !yearlyEnergyKwh.isFinite()
        ) {
            return project
        }

        return DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    pumps =
                        project.sewage.pumps.map {
                            if (it.id == pumpId) {
                                it.copy(
                                    motorPowerKw =
                                        motorPowerKw,
                                    yearlyEnergyKwh =
                                        yearlyEnergyKwh,
                                    status =
                                        DesignCalculationStatus
                                            .CALCULATED
                                )
                            } else {
                                it
                            }
                        }
                )
            )
        )
    }

    fun markInProgress(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    status =
                        DesignCalculationStatus.CALCULATED
                )
            )
        )

    fun markDataIncomplete(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    status =
                        DesignCalculationStatus.DATA_INCOMPLETE
                )
            )
        )

    fun markInvalid(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    status =
                        DesignCalculationStatus.INVALID
                )
            )
        )
}
