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

/**
 * Central hydraulic calculation engine for water systems.
 *
 * Calculation chain:
 *
 * Flow
 * -> Pipe velocity
 * -> Pipe friction loss
 * -> Total friction loss
 * -> TDH
 * -> Pump duty
 * -> Hydraulic power
 * -> Shaft power
 * -> Motor input power
 * -> Energy
 *
 * Pipe friction uses Hazen-Williams SI formulation.
 *
 * hf = 10.67 L Q^1.852 / (C^1.852 d^4.87)
 */
object WaterDesignEngine {

    private const val WATER_DENSITY_KG_M3 = 1000.0
    private const val GRAVITY_M_S2 = 9.81

    private const val MIN_DIAMETER_M = 0.001
    private const val MIN_EFFICIENCY = 0.01
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

        if (diameterM < MIN_DIAMETER_M || q <= 0.0) {
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

    fun calculateTdh(
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double,
        requiredPressureHeadM: Double
    ): Double {

        val static =
            safePositive(staticHeadM)

        val friction =
            safePositive(frictionHeadM)

        val minor =
            safePositive(minorLossHeadM)

        val pressure =
            safePositive(requiredPressureHeadM)

        return static + friction + minor + pressure
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
                .takeIf { it.isFinite() && it > 0.0 }
                ?: defaultFlowM3PerHour

        val input =
            PumpCalculationInput(
                systemType = PumpSystemType.WATER,
                flow = PumpFlow(
                    value = flow,
                    unit = PumpFlowUnit.CUBIC_METERS_PER_HOUR
                ),
                head = PumpHead(
                    staticHeadM = safePositive(staticHeadM),
                    frictionHeadM = safePositive(frictionHeadM),
                    minorLossHeadM = safePositive(minorLossHeadM),
                    requiredPressureHeadM =
                        safePositive(requiredPressureHeadM)
                ),
                pumpEfficiency =
                    pump.pumpEfficiency
                        .coerceIn(0.0, MAX_EFFICIENCY),
                motorEfficiency =
                    pump.motorEfficiency
                        .coerceIn(0.0, MAX_EFFICIENCY)
            )

        return PumpCalculator.calculate(input)
    }

    fun calculatePipe(
        pipe: WaterPipe
    ): WaterPipe {

        val velocity =
            calculateVelocity(
                flowM3PerHour = pipe.flowM3PerHour,
                diameterMm = pipe.diameterMm
            )

        val friction =
            calculateFrictionLoss(
                flowM3PerHour = pipe.flowM3PerHour,
                diameterMm = pipe.diameterMm,
                lengthM = pipe.lengthM,
                material = pipe.material
            )

        val valid =
            pipe.flowM3PerHour > 0.0 &&
                pipe.diameterMm > 0.0 &&
                pipe.lengthM > 0.0 &&
                velocity > 0.0 &&
                friction >= 0.0

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
                safePositive(water.frictionHeadM)
            }

        val tdh =
            calculateTdh(
                staticHeadM = water.staticHeadM,
                frictionHeadM = frictionHead,
                minorLossHeadM = water.minorLossHeadM,
                requiredPressureHeadM =
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
                            pump = pump,
                            defaultFlowM3PerHour =
                                water.requiredFlowM3PerHour,
                            staticHeadM =
                                water.staticHeadM,
                            frictionHeadM =
                                frictionHead,
                            minorLossHeadM =
                                water.minorLossHeadM,
                            requiredPressureHeadM =
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

        val pipesIncomplete =
            calculatedPipes.any {
                it.status ==
                    DesignCalculationStatus.DATA_INCOMPLETE
            }

        val pumpsInvalid =
            calculatedPumps.any {
                it.status ==
                    DesignCalculationStatus.INVALID
            }

        val pumpsIncomplete =
            calculatedPumps.any {
                it.status ==
                    DesignCalculationStatus.DATA_INCOMPLETE
            }

        val status =
            when {
                water.requiredFlowM3PerHour <= 0.0 ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                pipesIncomplete ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                pumpsInvalid ->
                    DesignCalculationStatus.INVALID

                pumpsIncomplete ->
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

    private fun safePositive(
        value: Double
    ): Double =
        if (value.isFinite() && value > 0.0) {
            value
        } else {
            0.0
        }

    private fun hazenWilliamsCoefficient(
        material: String
    ): Double {

        return when (
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
            "frp" ->
                150.0

            "ductile iron",
            "ductile",
            "di" ->
                130.0

            "steel",
            "carbon steel",
            "steel pipe" ->
                120.0

            "galvanized steel",
            "galvanized" ->
                120.0

            "concrete",
            "reinforced concrete",
            "rc" ->
                120.0

            "cast iron",
            "cast-iron" ->
                100.0

            else ->
                120.0
        }
    }
}
