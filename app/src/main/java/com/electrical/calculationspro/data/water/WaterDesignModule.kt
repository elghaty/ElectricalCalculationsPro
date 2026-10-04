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
         * TDH is not treated as an authoritative UI result.
         * The engineering engine remains responsible for the
         * final hydraulic calculation.
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

/**
 * Complete hydraulic input for a water design calculation.
 *
 * Flow, diameter and velocity are intentionally nullable because
 * the engineering calculation can determine one missing hydraulic
 * variable from the other two.
 */
data class WaterDesignInput(
    val flowM3PerHour: Double?,
    val diameterMm: Double?,
    val velocityMPerS: Double?,
    val pipeLengthM: Double,
    val material: String,
    val staticHeadM: Double,
    val minorLossHeadM: Double,
    val requiredPressureHeadM: Double
)

/**
 * Structured result returned to the UI.
 *
 * The UI should display this result and must not repeat the
 * hydraulic calculations itself.
 */
data class WaterDesignCalculationResult(
    val project: DesignProject,
    val flowM3PerHour: Double,
    val diameterMm: Double,
    val velocityMPerS: Double,
    val frictionLossM: Double,
    val tdhM: Double
)

/**
 * Performs the complete water hydraulic calculation and saves
 * the resulting design through the project/module architecture.
 */
fun WaterDesignModule.calculateAndSave(
    project: DesignProject,
    input: WaterDesignInput
): WaterDesignCalculationResult {

    require(input.pipeLengthM > 0.0) {
        "Pipe length must be greater than zero."
    }

    require(input.staticHeadM >= 0.0) {
        "Static head cannot be negative."
    }

    require(input.minorLossHeadM >= 0.0) {
        "Minor loss cannot be negative."
    }

    require(input.requiredPressureHeadM >= 0.0) {
        "Required pressure head cannot be negative."
    }

    val flow =
        input.flowM3PerHour?.takeIf { it > 0.0 }

    val diameter =
        input.diameterMm?.takeIf { it > 0.0 }

    val velocity =
        input.velocityMPerS?.takeIf { it > 0.0 }

    require(flow != null || velocity != null) {
        "Enter flow or velocity."
    }

    require(diameter != null || velocity != null) {
        "Enter diameter or velocity."
    }

    val resolvedFlow: Double
    val resolvedDiameter: Double
    val resolvedVelocity: Double

    when {
        flow != null && diameter != null -> {

            resolvedFlow = flow
            resolvedDiameter = diameter

            resolvedVelocity =
                WaterDesignEngine.calculateVelocity(
                    flowM3PerHour = flow,
                    diameterMm = diameter
                )
        }

        diameter != null && velocity != null -> {

            resolvedDiameter = diameter
            resolvedVelocity = velocity

            resolvedFlow =
                WaterDesignEngine.calculateFlow(
                    diameterMm = diameter,
                    velocityMPerS = velocity
                )
        }

        flow != null && velocity != null -> {

            resolvedFlow = flow
            resolvedVelocity = velocity

            resolvedDiameter =
                WaterDesignEngine.calculateDiameter(
                    flowM3PerHour = flow,
                    velocityMPerS = velocity
                )
        }

        else -> {
            throw IllegalArgumentException(
                "Enter any two of flow, diameter and velocity."
            )
        }
    }

    require(
        resolvedFlow > 0.0 &&
            resolvedDiameter > 0.0 &&
            resolvedVelocity > 0.0
    ) {
        "Calculated hydraulic values are invalid."
    }

    val frictionLoss =
        WaterDesignEngine.calculateFrictionLoss(
            flowM3PerHour = resolvedFlow,
            diameterMm = resolvedDiameter,
            lengthM = input.pipeLengthM,
            material = input.material
        )

    require(frictionLoss >= 0.0) {
        "Calculated friction loss is invalid."
    }

    val tdh =
        WaterDesignEngine.calculateTdh(
            staticHeadM = input.staticHeadM,
            frictionHeadM = frictionLoss,
            minorLossHeadM = input.minorLossHeadM,
            requiredPressureHeadM = input.requiredPressureHeadM
        )

    require(tdh >= 0.0) {
        "Calculated TDH is invalid."
    }

    val pipe =
        WaterPipe(
            name = "Main Water Pipe",
            diameterMm = resolvedDiameter,
            lengthM = input.pipeLengthM,
            material = input.material,
            flowM3PerHour = resolvedFlow,
            velocityMPerS = resolvedVelocity,
            frictionLossM = frictionLoss
        )

    var updatedProject =
        WaterDesignModule.addPipe(
            project = project,
            pipe = pipe
        )

    updatedProject =
        WaterDesignModule.updateHydraulicDesign(
            project = updatedProject,
            flowM3PerHour = resolvedFlow,
            staticHeadM = input.staticHeadM,
            frictionHeadM = frictionLoss,
            minorLossHeadM = input.minorLossHeadM,
            requiredPressureHeadM = input.requiredPressureHeadM,
            tdhM = tdh
        )

    return WaterDesignCalculationResult(
        project = updatedProject,
        flowM3PerHour = resolvedFlow,
        diameterMm = resolvedDiameter,
        velocityMPerS = resolvedVelocity,
        frictionLossM = frictionLoss,
        tdhM = tdh
    )
}
