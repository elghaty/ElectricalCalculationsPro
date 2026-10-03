package com.electrical.calculationspro.data.water

import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectEngine
import com.electrical.calculationspro.data.project.WaterPipe
import com.electrical.calculationspro.data.project.WaterPump

object WaterDesignModule {

    fun addPipe(
        project: DesignProject,
        pipe: WaterPipe
    ): DesignProject {

        val updatedWater =
            project.water.copy(
                pipes =
                    project.water.pipes
                        .filterNot { it.id == pipe.id } +
                        pipe,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjectEngine.recalculateWater(
            project.withWater(updatedWater)
        )
    }

    fun addPump(
        project: DesignProject,
        pump: WaterPump
    ): DesignProject {

        val updatedWater =
            project.water.copy(
                pumps =
                    project.water.pumps
                        .filterNot { it.id == pump.id } +
                        pump,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjectEngine.recalculateWater(
            project.withWater(updatedWater)
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

        /*
         * tdhM is intentionally retained for API compatibility.
         *
         * TDH is no longer accepted as an authoritative UI result.
         * The engineering engine recalculates it from the hydraulic
         * components.
         */

        val updatedWater =
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
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjectEngine.recalculateWater(
            project.withWater(updatedWater)
        )
    }

    fun updateCalculatedTdh(
        project: DesignProject,
        tdhM: Double
    ): DesignProject {

        val updated =
            project.withWater(
                project.water.copy(
                    tdhM = tdhM.coerceAtLeast(0.0)
                )
            )

        return DesignProjectEngine.recalculateWater(
            updated
        )
    }

    fun recalculate(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateWater(
            project
        )

    fun markInProgress(
        project: DesignProject
    ): DesignProject =
        project.withWater(
            project.water.copy(
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )
        )

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        project.withWater(
            project.water.copy(
                status =
                    DesignCalculationStatus.CALCULATED
            )
        )

    fun markDataIncomplete(
        project: DesignProject
    ): DesignProject =
        project.withWater(
            project.water.copy(
                status =
                    DesignCalculationStatus.DATA_INCOMPLETE
            )
        )

    fun markInvalid(
        project: DesignProject
    ): DesignProject =
        project.withWater(
            project.water.copy(
                status =
                    DesignCalculationStatus.INVALID
            )
        )
}
