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

/**
 * Complete sewage hydraulic input.
 */
data class SewageDesignInput(
    val averageFlowM3PerDay: Double,
    val peakFlowM3PerDay: Double,
    val minimumFlowM3PerDay: Double,
    val flowM3PerHour: Double?,
    val diameterMm: Double?,
    val velocityMPerS: Double?,
    val pipeLengthM: Double,
    val material: String,
    val staticHeadM: Double,
    val minorLossHeadM: Double
)

/**
 * Structured sewage design result.
 */
data class SewageDesignCalculationResult(
    val project: DesignProject,
    val averageFlowM3PerDay: Double,
    val peakFlowM3PerDay: Double,
    val minimumFlowM3PerDay: Double,
    val risingMainFlowM3PerHour: Double,
    val diameterMm: Double,
    val velocityMPerS: Double,
    val frictionLossM: Double,
    val tdhM: Double
)

/**
 * Performs the sewage hydraulic workflow in the domain layer.
 *
 * The UI supplies inputs only. All hydraulic calculations and
 * project persistence are handled here.
 */
fun SewageDesignModule.calculateAndSave(
    project: DesignProject,
    input: SewageDesignInput
): SewageDesignCalculationResult {

    require(input.averageFlowM3PerDay > 0.0) {
        "Average flow must be greater than zero."
    }

    require(
        input.peakFlowM3PerDay >=
            input.averageFlowM3PerDay
    ) {
        "Peak flow must be greater than or equal to average flow."
    }

    require(input.minimumFlowM3PerDay >= 0.0) {
        "Minimum flow cannot be negative."
    }

    require(
        input.minimumFlowM3PerDay <=
            input.peakFlowM3PerDay
    ) {
        "Minimum flow cannot exceed peak flow."
    }

    require(input.pipeLengthM > 0.0) {
        "Rising main length must be greater than zero."
    }

    require(input.staticHeadM >= 0.0) {
        "Static head cannot be negative."
    }

    require(input.minorLossHeadM >= 0.0) {
        "Minor loss cannot be negative."
    }

    val inputFlow =
        input.flowM3PerHour?.takeIf {
            it > 0.0
        }

    val inputDiameter =
        input.diameterMm?.takeIf {
            it > 0.0
        }

    val inputVelocity =
        input.velocityMPerS?.takeIf {
            it > 0.0
        }

    val peakFlowM3PerHour =
        input.peakFlowM3PerDay / 24.0

    val resolvedFlow =
        inputFlow ?: peakFlowM3PerHour

    require(resolvedFlow > 0.0) {
        "Rising main flow must be greater than zero."
    }

    val finalFlow: Double
    val finalDiameter: Double
    val finalVelocity: Double

    when {

        inputDiameter != null -> {

            finalFlow =
                resolvedFlow

            finalDiameter =
                inputDiameter

            finalVelocity =
                SewageDesignEngine.calculateVelocity(
                    flowM3PerHour = finalFlow,
                    diameterMm = finalDiameter
                )
        }

        inputVelocity != null -> {

            finalFlow =
                resolvedFlow

            finalVelocity =
                inputVelocity

            finalDiameter =
                SewageDesignEngine.calculateDiameter(
                    flowM3PerHour = finalFlow,
                    velocityMPerS = finalVelocity
                )
        }

        else -> {
            throw IllegalArgumentException(
                "Enter diameter or velocity for the rising main."
            )
        }
    }

    require(
        finalFlow > 0.0 &&
            finalDiameter > 0.0 &&
            finalVelocity > 0.0
    ) {
        "Calculated rising-main values are invalid."
    }

    val frictionLoss =
        SewageDesignEngine.calculateFrictionLoss(
            flowM3PerHour = finalFlow,
            diameterMm = finalDiameter,
            lengthM = input.pipeLengthM,
            material = input.material
        )

    require(frictionLoss >= 0.0) {
        "Calculated friction loss is invalid."
    }

    val tdh =
        SewageDesignEngine.calculateTdh(
            staticHeadM = input.staticHeadM,
            frictionHeadM = frictionLoss,
            minorLossHeadM = input.minorLossHeadM
        )

    require(tdh >= 0.0) {
        "Calculated TDH is invalid."
    }

    var updatedProject =
        SewageDesignModule.updateFlows(
            project = project,
            averageFlowM3PerDay =
                input.averageFlowM3PerDay,
            peakFlowM3PerDay =
                input.peakFlowM3PerDay,
            minimumFlowM3PerDay =
                input.minimumFlowM3PerDay
        )

    updatedProject =
        SewageDesignModule.setRisingMain(
            project = updatedProject,
            risingMain =
                RisingMainDesign(
                    name = "Main Rising Main",
                    diameterMm = finalDiameter,
                    lengthM = input.pipeLengthM,
                    material = input.material,
                    flowM3PerHour = finalFlow,
                    velocityMPerS = finalVelocity,
                    frictionLossM = frictionLoss,
                    minorLossHeadM =
                        input.minorLossHeadM
                )
        )

    updatedProject =
        SewageDesignModule.setStaticHead(
            project = updatedProject,
            staticHeadM = input.staticHeadM
        )

    return SewageDesignCalculationResult(
        project = updatedProject,
        averageFlowM3PerDay =
            input.averageFlowM3PerDay,
        peakFlowM3PerDay =
            input.peakFlowM3PerDay,
        minimumFlowM3PerDay =
            input.minimumFlowM3PerDay,
        risingMainFlowM3PerHour =
            finalFlow,
        diameterMm =
            finalDiameter,
        velocityMPerS =
            finalVelocity,
        frictionLossM =
            frictionLoss,
        tdhM =
            tdh
    )
}
