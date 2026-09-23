package com.electrical.calculationspro.data.water

import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.WaterPipe
import com.electrical.calculationspro.data.project.WaterPump

/**
 * Water design project module.
 *
 * This class is an orchestration layer only.
 * Engineering formulas must remain in dedicated calculators/engines.
 */
object WaterDesignModule {

    fun addPipe(
        project: DesignProject,
        pipe: WaterPipe
    ): DesignProject {

        val updatedPipes =
            project.water.pipes
                .filterNot { it.id == pipe.id } + pipe

        val design =
            project.water.copy(
                pipes = updatedPipes,
                status = DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withWater(design)
        )
    }

    fun addPump(
        project: DesignProject,
        pump: WaterPump
    ): DesignProject {

        val updatedPumps =
            project.water.pumps
                .filterNot { it.id == pump.id } + pump

        val design =
            project.water.copy(
                pumps = updatedPumps,
                status = DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withWater(design)
        )
    }

    /**
     * Stores hydraulic design inputs.
     *
     * TDH is deliberately NOT calculated here.
     * The hydraulic engine/calculator is responsible for that calculation.
     */
    fun updateHydraulicDesign(
        project: DesignProject,
        flowM3PerHour: Double,
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double,
        requiredPressureHeadM: Double,
        tdhM: Double? = null
    ): DesignProject {

        val calculationStatus =
            if (tdhM != null && tdhM.isFinite() && tdhM >= 0.0) {
                DesignCalculationStatus.CALCULATED
            } else {
                DesignCalculationStatus.IN_PROGRESS
            }

        val design =
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

                tdhM =
                    tdhM?.takeIf {
                        it.isFinite() && it >= 0.0
                    } ?: project.water.tdhM,

                status = calculationStatus
            )

        return DesignProjects.save(
            project.withWater(design)
        )
    }

    fun updateCalculatedTdh(
        project: DesignProject,
        tdhM: Double
    ): DesignProject {

        if (!tdhM.isFinite() || tdhM < 0.0) {
            return project
        }

        val design =
            project.water.copy(
                tdhM = tdhM,
                status = DesignCalculationStatus.CALCULATED
            )

        return DesignProjects.save(
            project.withWater(design)
        )
    }

    fun markInProgress(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withWater(
                project.water.copy(
                    status = DesignCalculationStatus.IN_PROGRESS
                )
            )
        )

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

    fun markDataIncomplete(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withWater(
                project.water.copy(
                    status = DesignCalculationStatus.DATA_INCOMPLETE
                )
            )
        )

    fun markInvalid(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withWater(
                project.water.copy(
                    status = DesignCalculationStatus.INVALID
                )
            )
        )
}
