package com.electrical.calculationspro.data.sewage

import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculator
import com.electrical.calculationspro.data.pumps.PumpFlow
import com.electrical.calculationspro.data.pumps.PumpFlowUnit
import com.electrical.calculationspro.data.pumps.PumpHead
import com.electrical.calculationspro.data.pumps.PumpSystemType
import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

data class SewageHydraulicAutoResult(
    val flowM3PerHour: Double,
    val diameterMm: Double,
    val velocityMPerS: Double,
    val headLossM: Double,
    val calculatedField: SewageHydraulicField?,
    val valid: Boolean,
    val message: String
)

enum class SewageHydraulicField {
    FLOW,
    DIAMETER,
    VELOCITY,
    HEAD_LOSS
}

object SewageDesignEngine {

    fun calculatePeakFactor(
        averageFlowM3PerDay: Double,
        peakFlowM3PerDay: Double
    ): Double {
        if (
            averageFlowM3PerDay <= 0.0 ||
            peakFlowM3PerDay <= 0.0
        ) {
            return 0.0
        }

        return peakFlowM3PerDay / averageFlowM3PerDay
    }

    fun calculatePeakFlow(
        averageFlowM3PerDay: Double,
        peakFactor: Double
    ): Double {
        if (
            averageFlowM3PerDay <= 0.0 ||
            peakFactor <= 0.0
        ) {
            return 0.0
        }

        return averageFlowM3PerDay * peakFactor
    }

    fun calculateVelocity(
        flowM3PerHour: Double,
        diameterMm: Double
    ): Double {
        if (
            !flowM3PerHour.isFinite() ||
            !diameterMm.isFinite() ||
            flowM3PerHour <= 0.0 ||
            diameterMm <= 0.0
        ) {
            return 0.0
        }

        val qM3PerSecond =
            flowM3PerHour / 3600.0

        val diameterM =
            diameterMm / 1000.0

        val area =
            PI * diameterM.pow(2.0) / 4.0

        return if (area > 0.0) {
            qM3PerSecond / area
        } else {
            0.0
        }
    }

    fun calculateFlow(
        diameterMm: Double,
        velocityMPerS: Double
    ): Double {
        if (
            !diameterMm.isFinite() ||
            !velocityMPerS.isFinite() ||
            diameterMm <= 0.0 ||
            velocityMPerS <= 0.0
        ) {
            return 0.0
        }

        val diameterM =
            diameterMm / 1000.0

        val area =
            PI * diameterM.pow(2.0) / 4.0

        return if (area > 0.0) {
            velocityMPerS * area * 3600.0
        } else {
            0.0
        }
    }

    fun calculateDiameter(
        flowM3PerHour: Double,
        velocityMPerS: Double
    ): Double {
        if (
            !flowM3PerHour.isFinite() ||
            !velocityMPerS.isFinite() ||
            flowM3PerHour <= 0.0 ||
            velocityMPerS <= 0.0
        ) {
            return 0.0
        }

        val flowM3PerSecond =
            flowM3PerHour / 3600.0

        val area =
            flowM3PerSecond / velocityMPerS

        if (area <= 0.0) {
            return 0.0
        }

        return sqrt(
            4.0 * area / PI
        ) * 1000.0
    }

    fun calculateFrictionLoss(
        flowM3PerHour: Double,
        diameterMm: Double,
        lengthM: Double,
        material: String
    ): Double {
        if (
            !flowM3PerHour.isFinite() ||
            !diameterMm.isFinite() ||
            !lengthM.isFinite() ||
            flowM3PerHour <= 0.0 ||
            diameterMm <= 0.0 ||
            lengthM <= 0.0
        ) {
            return 0.0
        }

        val q =
            flowM3PerHour / 3600.0

        val diameterM =
            diameterMm / 1000.0

        val c =
            hazenWilliamsCoefficient(material)

        if (
            q <= 0.0 ||
            diameterM <= 0.0 ||
            c <= 0.0
        ) {
            return 0.0
        }

        return (
            10.67 *
                lengthM *
                q.pow(1.852) /
                (
                    c.pow(1.852) *
                        diameterM.pow(4.87)
                    )
            ).coerceAtLeast(0.0)
    }

    fun calculateTdh(
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double
    ): Double {
        return staticHeadM.coerceAtLeast(0.0) +
            frictionHeadM.coerceAtLeast(0.0) +
            minorLossHeadM.coerceAtLeast(0.0)
    }

    fun calculateWetWellVolume(
        diameterM: Double,
        effectiveDepthM: Double
    ): Double {
        if (
            diameterM <= 0.0 ||
            effectiveDepthM <= 0.0
        ) {
            return 0.0
        }

        return (
            PI *
                diameterM.pow(2.0) /
                4.0 *
                effectiveDepthM
            ).coerceAtLeast(0.0)
    }

    /*
     * Automatic hydraulic solver.
     *
     * Q = V × A
     *
     * Any two of Q / D / V determine the third.
     *
     * Hf is calculated independently from:
     * Q + D + L + material
     *
     * If Hf is supplied together with two hydraulic variables,
     * Hf is retained as the supplied/check value.
     */

    fun calculateAuto(
        flowM3PerHour: Double?,
        diameterMm: Double?,
        velocityMPerS: Double?,
        headLossM: Double?,
        lengthM: Double,
        material: String
    ): SewageHydraulicAutoResult {

        val flow =
            flowM3PerHour?.takeIf {
                it.isFinite() && it > 0.0
            }

        val diameter =
            diameterMm?.takeIf {
                it.isFinite() && it > 0.0
            }

        val velocity =
            velocityMPerS?.takeIf {
                it.isFinite() && it > 0.0
            }

        val suppliedHeadLoss =
            headLossM?.takeIf {
                it.isFinite() && it >= 0.0
            }

        var q = flow ?: 0.0
        var d = diameter ?: 0.0
        var v = velocity ?: 0.0
        var h = suppliedHeadLoss ?: 0.0

        val calculatedField: SewageHydraulicField?

        when {
            flow == null &&
                diameter != null &&
                velocity != null -> {

                q = calculateFlow(
                    diameterMm = d,
                    velocityMPerS = v
                )

                calculatedField =
                    SewageHydraulicField.FLOW

                if (suppliedHeadLoss == null) {
                    h = calculateFrictionLoss(
                        flowM3PerHour = q,
                        diameterMm = d,
                        lengthM = lengthM,
                        material = material
                    )
                }
            }

            diameter == null &&
                flow != null &&
                velocity != null -> {

                d = calculateDiameter(
                    flowM3PerHour = q,
                    velocityMPerS = v
                )

                calculatedField =
                    SewageHydraulicField.DIAMETER

                if (suppliedHeadLoss == null) {
                    h = calculateFrictionLoss(
                        flowM3PerHour = q,
                        diameterMm = d,
                        lengthM = lengthM,
                        material = material
                    )
                }
            }

            velocity == null &&
                flow != null &&
                diameter != null -> {

                v = calculateVelocity(
                    flowM3PerHour = q,
                    diameterMm = d
                )

                calculatedField =
                    SewageHydraulicField.VELOCITY

                if (suppliedHeadLoss == null) {
                    h = calculateFrictionLoss(
                        flowM3PerHour = q,
                        diameterMm = d,
                        lengthM = lengthM,
                        material = material
                    )
                }
            }

            headLossM == null &&
                flow != null &&
                diameter != null &&
                velocity != null -> {

                h = calculateFrictionLoss(
                    flowM3PerHour = q,
                    diameterMm = d,
                    lengthM = lengthM,
                    material = material
                )

                calculatedField =
                    SewageHydraulicField.HEAD_LOSS
            }

            else -> {
                calculatedField = null
            }
        }

        val valid =
            q.isFinite() &&
                d.isFinite() &&
                v.isFinite() &&
                h.isFinite() &&
                q > 0.0 &&
                d > 0.0 &&
                v > 0.0 &&
                h >= 0.0

        return SewageHydraulicAutoResult(
            flowM3PerHour = q,
            diameterMm = d,
            velocityMPerS = v,
            headLossM = h,
            calculatedField = calculatedField,
            valid = valid,
            message =
                if (valid) {
                    "Hydraulic calculation completed."
                } else {
                    "Hydraulic input is incomplete or invalid."
                }
        )
    }

    fun calculateFlowFromDiameterAndVelocity(
        diameterMm: Double,
        velocityMPerS: Double
    ): Double =
        calculateFlow(
            diameterMm = diameterMm,
            velocityMPerS = velocityMPerS
        )

    fun calculateDiameterFromFlowAndVelocity(
        flowM3PerHour: Double,
        velocityMPerS: Double
    ): Double =
        calculateDiameter(
            flowM3PerHour = flowM3PerHour,
            velocityMPerS = velocityMPerS
        )

    fun calculateVelocityFromFlowAndDiameter(
        flowM3PerHour: Double,
        diameterMm: Double
    ): Double =
        calculateVelocity(
            flowM3PerHour = flowM3PerHour,
            diameterMm = diameterMm
        )

    fun calculateHeadLossFromFlowAndDiameter(
        flowM3PerHour: Double,
        diameterMm: Double,
        lengthM: Double,
        material: String
    ): Double =
        calculateFrictionLoss(
            flowM3PerHour = flowM3PerHour,
            diameterMm = diameterMm,
            lengthM = lengthM,
            material = material
        )

    fun recalculate(
        project: DesignProject
    ): DesignProject {

        val sewage = project.sewage

        val averageFlow =
            sewage.averageFlowM3PerDay.coerceAtLeast(0.0)

        val peakFlow =
            sewage.peakFlowM3PerDay.coerceAtLeast(0.0)

        val peakHourlyFlow =
            if (peakFlow > 0.0) {
                peakFlow / 24.0
            } else {
                0.0
            }

        val calculatedWetWell =
            sewage.wetWell?.let { wetWell ->

                val volume =
                    calculateWetWellVolume(
                        diameterM = wetWell.diameterM,
                        effectiveDepthM =
                            wetWell.effectiveDepthM
                    )

                val valid =
                    wetWell.diameterM > 0.0 &&
                        wetWell.effectiveDepthM > 0.0 &&
                        volume > 0.0 &&
                        wetWell.stopLevelM >= 0.0 &&
                        wetWell.startLevelM >=
                            wetWell.stopLevelM

                wetWell.copy(
                    operatingVolumeM3 = volume,
                    status =
                        if (valid) {
                            DesignCalculationStatus.CALCULATED
                        } else {
                            DesignCalculationStatus.DATA_INCOMPLETE
                        }
                )
            }

        val risingMain =
            sewage.risingMain

        val risingMainFlow =
            risingMain
                ?.flowM3PerHour
                ?.takeIf {
                    it.isFinite() && it > 0.0
                }
                ?: peakHourlyFlow

        val calculatedRisingMain =
            risingMain?.let { main ->

                val velocity =
                    calculateVelocity(
                        flowM3PerHour = risingMainFlow,
                        diameterMm = main.diameterMm
                    )

                val friction =
                    calculateFrictionLoss(
                        flowM3PerHour = risingMainFlow,
                        diameterMm = main.diameterMm,
                        lengthM = main.lengthM,
                        material = main.material
                    )

                val valid =
                    risingMainFlow > 0.0 &&
                        main.diameterMm > 0.0 &&
                        main.lengthM > 0.0 &&
                        velocity > 0.0 &&
                        velocity.isFinite() &&
                        friction.isFinite()

                main.copy(
                    flowM3PerHour = risingMainFlow,
                    velocityMPerS = velocity,
                    frictionLossM = friction,
                    status =
                        if (valid) {
                            DesignCalculationStatus.CALCULATED
                        } else {
                            DesignCalculationStatus.DATA_INCOMPLETE
                        }
                )
            }

        val frictionHead =
            calculatedRisingMain
                ?.frictionLossM
                ?.coerceAtLeast(0.0)
                ?: sewage.risingMain
                    ?.frictionLossM
                    ?.coerceAtLeast(0.0)
                ?: 0.0

        val minorLossHead =
            calculatedRisingMain
                ?.minorLossHeadM
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val tdh =
            calculateTdh(
                staticHeadM = sewage.staticHeadM,
                frictionHeadM = frictionHead,
                minorLossHeadM = minorLossHead
            )

        val dutyPumpCount =
            sewage.pumps.count {
                it.duty && !it.standby
            }.coerceAtLeast(1)

        val dutyFlowPerPump =
            if (peakHourlyFlow > 0.0) {
                peakHourlyFlow /
                    dutyPumpCount.toDouble()
            } else {
                0.0
            }

        val calculatedPumps =
            sewage.pumps.map { pump ->

                val pumpFlow =
                    pump.flowM3PerHour.takeIf {
                        it.isFinite() && it > 0.0
                    } ?: dutyFlowPerPump

                if (
                    pumpFlow <= 0.0 ||
                    tdh <= 0.0 ||
                    pump.pumpEfficiency <= 0.0 ||
                    pump.pumpEfficiency > 1.0 ||
                    pump.motorEfficiency <= 0.0 ||
                    pump.motorEfficiency > 1.0
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
                                        value = pumpFlow,
                                        unit =
                                            PumpFlowUnit
                                                .CUBIC_METERS_PER_HOUR
                                    ),

                                head =
                                    PumpHead(
                                        staticHeadM =
                                            sewage.staticHeadM
                                                .coerceAtLeast(0.0),

                                        frictionHeadM =
                                            frictionHead,

                                        minorLossHeadM =
                                            minorLossHead,

                                        requiredPressureHeadM =
                                            0.0
                                    ),

                                pumpEfficiency =
                                    pump.pumpEfficiency,

                                motorEfficiency =
                                    pump.motorEfficiency
                            )
                        )

                    pump.copy(
                        flowM3PerHour =
                            result.flowM3PerHour,

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
                averageFlow <= 0.0 ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                peakFlow <= 0.0 ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                peakFlow < averageFlow ->
                    DesignCalculationStatus.INVALID

                calculatedRisingMain != null &&
                    calculatedRisingMain.status ==
                    DesignCalculationStatus.DATA_INCOMPLETE ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                calculatedPumps.any {
                    it.status ==
                        DesignCalculationStatus.INVALID
                } ->
                    DesignCalculationStatus.INVALID

                calculatedPumps.any {
                    it.status ==
                        DesignCalculationStatus.DATA_INCOMPLETE
                } ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                tdh <= 0.0 ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                else ->
                    DesignCalculationStatus.CALCULATED
            }

        return project.withSewage(
            sewage.copy(
                wetWell = calculatedWetWell,
                risingMain = calculatedRisingMain,
                tdhM = tdh,
                pumps = calculatedPumps,
                status = status
            )
        )
    }

    private fun hazenWilliamsCoefficient(
        material: String
    ): Double {

        return when (
            material.trim().lowercase()
        ) {
            "pvc",
            "u-pvc",
            "upvc",
            "plastic",
            "hdpe",
            "pe",
            "polyethylene",
            "grp",
            "frp" -> 150.0

            "ductile iron",
            "ductile",
            "di" -> 130.0

            "steel",
            "carbon steel",
            "steel pipe",
            "galvanized steel",
            "galvanized",
            "concrete",
            "reinforced concrete",
            "rc" -> 120.0

            "cast iron",
            "cast-iron" -> 100.0

            else -> 120.0
        }
    }
}
