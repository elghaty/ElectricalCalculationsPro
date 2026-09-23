package com.electrical.calculationspro.data.sewage

import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.RisingMainDesign
import com.electrical.calculationspro.data.project.SewagePump
import com.electrical.calculationspro.data.project.WetWellDesign

/**
 * Sewage project orchestration layer.
 *
 * No hydraulic formulas are implemented here.
 * Hydraulic calculations belong to dedicated engineering engines.
 */
object SewageDesignModule {

    fun updateFlows(
        project: DesignProject,
        averageFlowM3PerDay: Double,
        peakFlowM3PerDay: Double,
        minimumFlowM3PerDay: Double
    ): DesignProject {

        val design =
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

        return DesignProjects.save(
            project.withSewage(design)
        )
    }

    fun setWetWell(
        project: DesignProject,
        wetWell: WetWellDesign
    ): DesignProject {

        val design =
            project.sewage.copy(
                wetWell = wetWell,
                status = DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withSewage(design)
        )
    }

    fun setRisingMain(
        project: DesignProject,
        risingMain: RisingMainDesign
    ): DesignProject {

        val design =
            project.sewage.copy(
                risingMain = risingMain,
                status = DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withSewage(design)
        )
    }

    fun addPump(
        project: DesignProject,
        pump: SewagePump
    ): DesignProject {

        val updatedPumps =
            project.sewage.pumps
                .filterNot { it.id == pump.id } + pump

        val design =
            project.sewage.copy(
                pumps = updatedPumps,
                status = DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withSewage(design)
        )
    }

    fun updateRisingMainCalculatedValues(
        project: DesignProject,
        risingMainId: String,
        flowM3PerHour: Double,
        velocityMPerS: Double,
        frictionLossM: Double,
        minorLossHeadM: Double
    ): DesignProject {

        if (!flowM3PerHour.isFinite() ||
            !velocityMPerS.isFinite() ||
            !frictionLossM.isFinite() ||
            !minorLossHeadM.isFinite()
        ) {
            return project
        }

        val updatedRisingMain =
            project.sewage.risingMain
                ?.takeIf { it.id == risingMainId }
                ?.copy(
                    flowM3PerHour = flowM3PerHour,
                    velocityMPerS = velocityMPerS,
                    frictionLossM = frictionLossM,
                    minorLossHeadM = minorLossHeadM,
                    status = DesignCalculationStatus.CALCULATED
                )

        if (updatedRisingMain == null) {
            return project
        }

        return DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    risingMain = updatedRisingMain,
                    status = DesignCalculationStatus.CALCULATED
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

        if (!motorPowerKw.isFinite() ||
            !yearlyEnergyKwh.isFinite()
        ) {
            return project
        }

        val updatedPumps =
            project.sewage.pumps.map {
                if (it.id == pumpId) {
                    it.copy(
                        motorPowerKw = motorPowerKw,
                        yearlyEnergyKwh = yearlyEnergyKwh,
                        status = DesignCalculationStatus.CALCULATED
                    )
                } else {
                    it
                }
            }

        return DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    pumps = updatedPumps,
                    status = DesignCalculationStatus.CALCULATED
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
                    status = DesignCalculationStatus.IN_PROGRESS
                )
            )
        )

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    status = DesignCalculationStatus.CALCULATED
                )
            )
        )

    fun markDataIncomplete(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    status = DesignCalculationStatus.DATA_INCOMPLETE
                )
            )
        )

    fun markInvalid(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    status = DesignCalculationStatus.INVALID
                )
            )
        )
}
