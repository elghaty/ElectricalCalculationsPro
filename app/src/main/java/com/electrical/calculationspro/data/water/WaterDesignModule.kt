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

        val design =
            project.water.copy(
                pipes =
                    project.water.pipes
                        .filterNot { it.id == pipe.id } +
                        pipe,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withWater(design)
        )
    }

    fun addPump(
        project: DesignProject,
        pump: WaterPump
    ): DesignProject {

        val design =
            project.water.copy(
                pumps =
                    project.water.pumps
                        .filterNot { it.id == pump.id } +
                        pump,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withWater(design)
        )
    }

    fun updateHydraulicDesign(
        project: DesignProject,
        flowM3PerHour: Double,
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double,
        requiredPressureHeadM: Double
    ): DesignProject {

        val tdh =
            staticHeadM +
                frictionHeadM +
                minorLossHeadM +
                requiredPressureHeadM

        val design =
            project.water.copy(
                requiredFlowM3PerHour = flowM3PerHour,
                staticHeadM = staticHeadM,
                frictionHeadM = frictionHeadM,
                minorLossHeadM = minorLossHeadM,
                requiredPressureHeadM = requiredPressureHeadM,
                tdhM = tdh,
                status = DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withWater(design)
        )
    }

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withWater(
                project.water.copy(
                    status = DesignCalculationStatus.CALCULATED
                )
            )
        )
}
