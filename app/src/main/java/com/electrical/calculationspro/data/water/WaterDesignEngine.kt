package com.electrical.calculationspro.data.water

import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult
import com.electrical.calculationspro.data.pumps.PumpCalculator
import com.electrical.calculationspro.data.pumps.PumpFlow
import com.electrical.calculationspro.data.pumps.PumpFlowUnit
import com.electrical.calculationspro.data.pumps.PumpHead
import com.electrical.calculationspro.data.pumps.PumpSystemType
import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.WaterPipe
import com.electrical.calculationspro.data.project.WaterPump
import kotlin.math.PI
import kotlin.math.pow

data class WaterHydraulicAutoResult(
    val flowM3PerHour: Double,
    val diameterMm: Double,
    val velocityMPerS: Double,
    val headLossM: Double,
    val calculatedField: WaterHydraulicField?,
    val valid: Boolean,
    val message: String
)

enum class WaterHydraulicField {
    FLOW,
    DIAMETER,
    VELOCITY,
    HEAD_LOSS
}

object WaterDesignEngine {

    private const val MIN_DIAMETER_M = 0.001
    private const val MAX_EFFICIENCY = 1.0

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

        val flowM3PerSecond =
            flowM3PerHour / 3600.0

        val diameterM =
            diameterMm / 1000.0

        if (diameterM < MIN_DIAMETER_M) {
            return 0.0
        }

        val area =
            PI * diameterM.pow(2.0) / 4.0

        if (area <= 0.0) {
            return 0.0
        }

        return flowM3PerSecond / area
    }

    fun calculateFlow(
        diameterMm: Double,
        velocityMPerS: Double
    ): Double {

        if (
            diameterMm <= 0.0 ||
            velocityMPerS <= 0.0
        ) {
            return 0.0
        }

        val diameterM =
            diameterMm / 1000.0

        val area =
            PI * diameterM.pow(2.0) / 4.0

        return velocityMPerS *
            area *
            3600.0
    }

    fun calculateDiameter(
        flowM3PerHour: Double,
        velocityMPerS: Double
    ): Double {

        if (
            flowM3PerHour <= 0.0 ||
            velocityMPerS <= 0.0
        ) {
            return 0.0
        }

        val flowM3PerSecond =
            flowM3PerHour / 3600.0

        val area =
            flowM3PerSecond /
                velocityMPerS

        if (area <= 0.0) {
            return 0.0
        }

        val diameterM =
            kotlin.math.sqrt(
                4.0 * area / PI
            )

        return diameterM * 1000.0
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

        if (
            diameterM < MIN_DIAMETER_M ||
            q <= 0.0
        ) {
            return 0.0
        }

        val c =
            hazenWilliamsCoefficient(material)

        return (
            10.67 *
                lengthM *
                q.pow(1.852)
                /
                (
                    c.pow(1.852) *
                        diameterM.pow(4.87)
                )
            ).coerceAtLeast(0.0)
    }

    fun calculateFlowFromDiameterAndVelocity(
        diameterMm: Double,
        velocityMPerS: Double
    ): Double =
        calculateFlow(
            diameterMm,
            velocityMPerS
        )

    fun calculateHeadLossFromFlowDiameter(
        flowM3PerHour: Double,
        diameterMm: Double,
        lengthM: Double,
        material: String
    ): Double =
        calculateFrictionLoss(
            flowM3PerHour,
            diameterMm,
            lengthM,
            material
        )

    fun calculateAuto(
        flowM3PerHour: Double?,
        diameterMm: Double?,
        velocityMPerS: Double?,
        headLossM: Double?,
        lengthM: Double,
        material: String
    ): WaterHydraulicAutoResult {

        val flow =
            flowM3PerHour
                ?.takeIf { it.isFinite() && it > 0.0 }

        val diameter =
            diameterMm
                ?.takeIf { it.isFinite() && it > 0.0 }

        val velocity =
            velocityMPerS
                ?.takeIf { it.isFinite() && it > 0.0 }

        val head =
            headLossM
                ?.takeIf { it.isFinite() && it >= 0.0 }

        val knownCount =
            listOf(
                flow != null,
                diameter != null,
                velocity != null,
                head != null
            ).count { it }

        if (knownCount < 3) {
            return WaterHydraulicAutoResult(
                flowM3PerHour = flow ?: 0.0,
                diameterMm = diameter ?: 0.0,
                velocityMPerS = velocity ?: 0.0,
                headLossM = head ?: 0.0,
                calculatedField = null,
                valid = false,
                message = "Enter any three hydraulic values."
            )
        }

        var q = flow ?: 0.0
        var d = diameter ?: 0.0
        var v = velocity ?: 0.0
        var h = head ?: 0.0

        val calculatedField =
            when {
                flow == null &&
                    diameter != null &&
                    velocity != null -> {

                    q =
                        calculateFlow(
                            d,
                            v
                        )

                    WaterHydraulicField.FLOW
                }

                diameter == null &&
                    flow != null &&
                    velocity != null -> {

                    d =
                        calculateDiameter(
                            q,
                            v
                        )

                    WaterHydraulicField.DIAMETER
                }

                velocity == null &&
                    flow != null &&
                    diameter != null -> {

                    v =
                        calculateVelocity(
                            q,
                            d
                        )

                    WaterHydraulicField.VELOCITY
                }

                head == null &&
                    flow != null &&
                    diameter != null -> {

                    h =
                        calculateFrictionLoss(
                            q,
                            d,
                            lengthM,
                            material
                        )

                    WaterHydraulicField.HEAD_LOSS
                }

                else -> null
            }

        val valid =
            q > 0.0 &&
                d > 0.0 &&
                v > 0.0 &&
                h >= 0.0

        return WaterHydraulicAutoResult(
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
                    "Hydraulic input is incomplete."
                }
        )
    }

    fun calculateTdh(
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double,
        requiredPressureHeadM: Double
    ): Double {

        return staticHeadM.coerceAtLeast(0.0) +
            frictionHeadM.coerceAtLeast(0.0) +
            minorLossHeadM.coerceAtLeast(0.0) +
            requiredPressureHeadM.coerceAtLeast(0.0)
    }

    fun calculatePump(
        pump: WaterPump,
        defaultFlowM3PerHour: Double,
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double,
        requiredPressureHeadM: Double
    ): PumpCalculationResult {

        val flow =
            pump.flowM3PerHour
                .takeIf {
                    it.isFinite() && it > 0.0
                }
                ?: defaultFlowM3PerHour

        return PumpCalculator.calculate(
            PumpCalculationInput(
                systemType = PumpSystemType.WATER,
                flow = PumpFlow(
                    value = flow,
                    unit =
                        PumpFlowUnit
                            .CUBIC_METERS_PER_HOUR
                ),
                head = PumpHead(
                    staticHeadM =
                        staticHeadM.coerceAtLeast(0.0),
                    frictionHeadM =
                        frictionHeadM.coerceAtLeast(0.0),
                    minorLossHeadM =
                        minorLossHeadM.coerceAtLeast(0.0),
                    requiredPressureHeadM =
                        requiredPressureHeadM
                            .coerceAtLeast(0.0)
                ),
                pumpEfficiency =
                    pump.pumpEfficiency
                        .coerceIn(
                            0.0,
                            MAX_EFFICIENCY
                        ),
                motorEfficiency =
                    pump.motorEfficiency
                        .coerceIn(
                            0.0,
                            MAX_EFFICIENCY
                        )
            )
        )
    }

    fun calculatePipe(
        pipe: WaterPipe
    ): WaterPipe {

        val velocity =
            calculateVelocity(
                pipe.flowM3PerHour,
                pipe.diameterMm
            )

        val friction =
            calculateFrictionLoss(
                pipe.flowM3PerHour,
                pipe.diameterMm,
                pipe.lengthM,
                pipe.material
            )

        val valid =
            pipe.flowM3PerHour > 0.0 &&
                pipe.diameterMm > 0.0 &&
                pipe.lengthM > 0.0 &&
                velocity > 0.0

        return pipe.copy(
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

    fun recalculate(
        project: DesignProject
    ): DesignProject {

        val water =
            project.water

        val calculatedPipes =
            water.pipes.map(::calculatePipe)

        val pipeFriction =
            calculatedPipes
                .map { it.frictionLossM }
                .filter { it.isFinite() }
                .sum()
                .coerceAtLeast(0.0)

        val frictionHead =
            if (calculatedPipes.isNotEmpty()) {
                pipeFriction
            } else {
                water.frictionHeadM
                    .coerceAtLeast(0.0)
            }

        val tdh =
            calculateTdh(
                water.staticHeadM,
                frictionHead,
                water.minorLossHeadM,
                water.requiredPressureHeadM
            )

        val calculatedPumps =
            water.pumps.map { pump ->

                if (
                    pump.pumpEfficiency <= 0.0 ||
                    pump.pumpEfficiency > 1.0 ||
                    pump.motorEfficiency <= 0.0 ||
                    pump.motorEfficiency > 1.0 ||
                    tdh <= 0.0
                ) {
                    pump.copy(
                        status =
                            DesignCalculationStatus.DATA_INCOMPLETE
                    )
                } else {

                    val result =
                        calculatePump(
                            pump,
                            water.requiredFlowM3PerHour,
                            water.staticHeadM,
                            frictionHead,
                            water.minorLossHeadM,
                            water.requiredPressureHeadM
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
                water.requiredFlowM3PerHour <= 0.0 ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                calculatedPipes.any {
                    it.status ==
                        DesignCalculationStatus.DATA_INCOMPLETE
                } ->
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

        return project.withWater(
            water.copy(
                frictionHeadM = frictionHead,
                tdhM = tdh,
                pipes = calculatedPipes,
                pumps = calculatedPumps,
                status = status
            )
        )
    }

    private fun hazenWilliamsCoefficient(
        material: String
    ): Double =
        when (
            material
                .trim()
                .lowercase()
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
