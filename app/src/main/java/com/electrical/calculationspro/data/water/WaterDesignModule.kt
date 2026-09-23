package com.electrical.calculationspro.data.water

import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.WaterPipe
import com.electrical.calculationspro.data.project.WaterPump

object WaterDesignModule {

    fun addPipe(
        project: DesignProject,
        pipe: WaterPipe
    ): DesignProject {

        val pipes =
            project.water.pipes
                .filterNot { it.id == pipe.id } +
                pipe

        return DesignProjects.save(
            project.withWater(
                project.water.copy(
                    pipes = pipes,
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun addPump(
        project: DesignProject,
        pump: WaterPump
    ): DesignProject {

        val pumps =
            project.water.pumps
                .filterNot { it.id == pump.id } +
                pump

        return DesignProjects.save(
            project.withWater(
                project.water.copy(
                    pumps = pumps,
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun updateHydraulicDesign(
        project: DesignProject,
        flowM3PerHour: Double,
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double,
        requiredPressureHeadM: Double,
        tdhM: Double? = null
    ): DesignProject {

        val calculatedTdh =
            tdhM
                ?.takeIf {
                    it.isFinite() && it >= 0.0
                }
                ?: WaterDesignEngine.calculateTdh(
                    staticHeadM =
                        staticHeadM.coerceAtLeast(0.0),
                    frictionHeadM =
                        frictionHeadM.coerceAtLeast(0.0),
                    minorLossHeadM =
                        minorLossHeadM.coerceAtLeast(0.0),
                    requiredPressureHeadM =
                        requiredPressureHeadM.coerceAtLeast(0.0)
                )

        return DesignProjects.save(
            project.withWater(
                project.water.copy(
                    requiredFlowM3PerHour =
                        flowM3PerHour.coerceAtLeast(0.0),
                    staticHeadM =
                        staticHeadM.coerceAtLeast(0.0),
                    frictionHeadM =
                        frictionHeadM.coerceAtLeast(0.0),
                    minorLossHeadM =
                        minorLossHeadM.coerceAtLeast(0.0),
                    requiredPressureHeadM =
                        requiredPressureHeadM.coerceAtLeast(0.0),
                    tdhM = calculatedTdh,
                    status =
                        DesignCalculationStatus.CALCULATED
                )
            )
        )
    }

    fun recalculate(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateWater(project)

    fun updateCalculatedTdh(
        project: DesignProject,
        tdhM: Double
    ): DesignProject {

        if (
            !tdhM.isFinite() ||
            tdhM < 0.0
        ) {
            return project
        }

        return DesignProjects.save(
            project.withWater(
                project.water.copy(
                    tdhM = tdhM,
                    status =
                        DesignCalculationStatus.CALCULATED
                )
            )
        )
    }

    fun markInProgress(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withWater(
                project.water.copy(
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withWater(
                project.water.copy(
                    status =
                        DesignCalculationStatus.CALCULATED
                )
            )
        )

    fun markDataIncomplete(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withWater(
                project.water.copy(
                    status =
                        DesignCalculationStatus.DATA_INCOMPLETE
                )
            )
        )

    fun markInvalid(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withWater(
                project.water.copy(
                    status =
                        DesignCalculationStatus.INVALID
                )
            )
        )
}
