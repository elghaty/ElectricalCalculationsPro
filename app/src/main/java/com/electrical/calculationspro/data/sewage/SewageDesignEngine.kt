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

    /*
     * ============================================================
     * SEWAGE FLOW CALCULATIONS
     * ============================================================
     */

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

        return peakFlowM3PerDay /
            averageFlowM3PerDay
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

        return averageFlowM3PerDay *
            peakFactor
    }

    /*
     * ============================================================
     * BASIC HYDRAULIC RELATIONSHIPS
     *
     * Q = V × A
     * A = πD² / 4
     *
     * Flow:
     * m³/h
     *
     * Diameter:
     * mm
     *
     * Velocity:
     * m/s
     * ============================================================
     */

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

        val q =
            flowM3PerHour / 3600.0

        val diameterM =
            diameterMm / 1000.0

        if (diameterM <= 0.0) {
            return 0.0
        }

        val area =
            PI *
                diameterM.pow(2.0) /
                4.0

        return if (area > 0.0) {
            q / area
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
            PI *
                diameterM.pow(2.0) /
                4.0

        if (area <= 0.0) {
            return 0.0
        }

        return velocityMPerS *
            area *
            3600.0
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
            flowM3PerSecond /
                velocityMPerS

        if (area <= 0.0) {
            return 0.0
        }

        val diameterM =
            sqrt(
                4.0 *
                    area /
                    PI
            )

        return diameterM * 1000.0
    }

    /*
     * ============================================================
     * HAZEN-WILLIAMS FRICTION LOSS
     *
     * h = 10.67 L Q^1.852 /
     *     (C^1.852 D^4.87)
     *
     * Q = m³/s
     * D = m
     * L = m
     * ============================================================
     */

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
            q <= 0.0 ||
            diameterM <= 0.0
        ) {
            return 0.0
        }

        val c =
            hazenWilliamsCoefficient(
                material
            )

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

    /*
     * ============================================================
     * TOTAL DYNAMIC HEAD
     * ============================================================
     */

    fun calculateTdh(
        staticHeadM: Double,
        frictionHeadM: Double,
        minorLossHeadM: Double
    ): Double {

        return staticHeadM.coerceAtLeast(0.0) +
            frictionHeadM.coerceAtLeast(0.0) +
            minorLossHeadM.coerceAtLeast(0.0)
    }

    /*
     * ============================================================
     * WET WELL
     * ============================================================
     */

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

        return PI *
            diameterM.pow(2.0) /
            4.0 *
            effectiveDepthM
    }

    /*
     * ============================================================
     * AUTOMATIC HYDRAULIC CALCULATOR
     *
     * Any 3 known values calculate the 4th:
     *
     * Q + D + V  -> Head Loss
     * Q + D + H  -> Velocity
     * Q + V + H  -> Diameter
     * D + V + H  -> Flow
     *
     * IMPORTANT:
     * Head loss requires pipe length and material.
     * ============================================================
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
            flowM3PerHour
                ?.takeIf {
                    it.isFinite() &&
                        it > 0.0
                }

        val diameter =
            diameterMm
                ?.takeIf {
                    it.isFinite() &&
                        it > 0.0
                }

        val velocity =
            velocityMPerS
                ?.takeIf {
                    it.isFinite() &&
                        it > 0.0
                }

        val headLoss =
            headLossM
                ?.takeIf {
                    it.isFinite() &&
                        it >= 0.0
                }

        val knownCount =
            listOf(
                flow != null,
                diameter != null,
                velocity != null,
                headLoss != null
            ).count {
                it
            }

        /*
         * The automatic calculation starts only when
         * at least three valid quantities are supplied.
         */
        if (knownCount < 3) {

            return SewageHydraulicAutoResult(
                flowM3PerHour =
                    flow ?: 0.0,

                diameterMm =
                    diameter ?: 0.0,

                velocityMPerS =
                    velocity ?: 0.0,

                headLossM =
                    headLoss ?: 0.0,

                calculatedField =
                    null,

                valid =
                    false,

                message =
                    "Enter any three hydraulic values."
            )
        }

        var q =
            flow ?: 0.0

        var d =
            diameter ?: 0.0

        var v =
            velocity ?: 0.0

        var h =
            headLoss ?: 0.0

        val calculatedField =
            when {

                /*
                 * D + V + H -> Q
                 */
                flow == null &&
                    diameter != null &&
                    velocity != null &&
                    headLoss != null -> {

                    q =
                        calculateFlow(
                            diameterMm = d,
                            velocityMPerS = v
                        )

                    SewageHydraulicField.FLOW
                }

                /*
                 * Q + V + H -> D
                 */
                diameter == null &&
                    flow != null &&
                    velocity != null &&
                    headLoss != null -> {

                    d =
                        calculateDiameter(
                            flowM3PerHour = q,
                            velocityMPerS = v
                        )

                    SewageHydraulicField.DIAMETER
                }

                /*
                 * Q + D + H -> V
                 */
                velocity == null &&
                    flow != null &&
                    diameter != null &&
                    headLoss != null -> {

                    v =
                        calculateVelocity(
                            flowM3PerHour = q,
                            diameterMm = d
                        )

                    SewageHydraulicField.VELOCITY
                }

                /*
                 * Q + D + V -> Head Loss
                 */
                headLoss == null &&
                    flow != null &&
                    diameter != null &&
                    velocity != null -> {

                    h =
                        calculateFrictionLoss(
                            flowM3PerHour = q,
                            diameterMm = d,
                            lengthM = lengthM,
                            material = material
                        )

                    SewageHydraulicField.HEAD_LOSS
                }

                else -> {
                    null
                }
            }

        val valid =
            q > 0.0 &&
                d > 0.0 &&
                v > 0.0 &&
                h >= 0.0 &&
                q.isFinite() &&
                d.isFinite() &&
                v.isFinite() &&
                h.isFinite()

        return SewageHydraulicAutoResult(
            flowM3PerHour =
                q,

            diameterMm =
                d,

            velocityMPerS =
                v,

            headLossM =
                h,

            calculatedField =
                calculatedField,

            valid =
                valid,

            message =
                if (valid) {
                    "Hydraulic calculation completed."
                } else {
                    "Hydraulic input is invalid."
                }
        )
    }

    /*
     * ============================================================
     * CONVENIENCE METHODS
     * ============================================================
     */

    fun calculateFlowFromDiameterAndVelocity(
        diameterMm: Double,
        velocityMPerS: Double
    ): Double {

        return calculateFlow(
            diameterMm =
                diameterMm,
            velocityMPerS =
                velocityMPerS
        )
    }

    fun calculateDiameterFromFlowAndVelocity(
        flowM3PerHour: Double,
        velocityMPerS: Double
    ): Double {

        return calculateDiameter(
            flowM3PerHour =
                flowM3PerHour,
            velocityMPerS =
                velocityMPerS
        )
    }

    fun calculateVelocityFromFlowAndDiameter(
        flowM3PerHour: Double,
        diameterMm: Double
    ): Double {

        return calculateVelocity(
            flowM3PerHour =
                flowM3PerHour,
            diameterMm =
                diameterMm
        )
    }

    fun calculateHeadLossFromFlowAndDiameter(
        flowM3PerHour: Double,
        diameterMm: Double,
        lengthM: Double,
        material: String
    ): Double {

        return calculateFrictionLoss(
            flowM3PerHour =
                flowM3PerHour,
            diameterMm =
                diameterMm,
            lengthM =
                lengthM,
            material =
                material
        )
    }

    /*
     * ============================================================
     * FULL PROJECT RECALCULATION
     * ============================================================
     */

    fun recalculate(
        project: DesignProject
    ): DesignProject {

        val sewage =
            project.sewage

        val averageFlow =
            sewage.averageFlowM3PerDay
                .coerceAtLeast(0.0)

        val peakFlow =
            sewage.peakFlowM3PerDay
                .coerceAtLeast(0.0)

        /*
         * Peak flow is stored as m³/day.
         * Pump and rising-main calculations use m³/hour.
         */
        val peakHourlyFlow =
            if (peakFlow > 0.0) {
                peakFlow / 24.0
            } else {
                0.0
            }

        /*
         * ========================================================
         * WET WELL
         * ========================================================
         */

        val calculatedWetWell =
            sewage.wetWell?.let { wetWell ->

                val volume =
                    calculateWetWellVolume(
                        diameterM =
                            wetWell.diameterM,
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
                    operatingVolumeM3 =
                        volume,

                    status =
                        if (valid) {
                            DesignCalculationStatus.CALCULATED
                        } else {
                            DesignCalculationStatus.DATA_INCOMPLETE
                        }
                )
            }

        /*
         * ========================================================
         * RISING MAIN
         * ========================================================
         */

        val risingMain =
            sewage.risingMain

        val risingMainFlow =
            risingMain
                ?.flowM3PerHour
                ?.takeIf {
                    it.isFinite() &&
                        it > 0.0
                }
                ?: peakHourlyFlow

        val calculatedRisingMain =
            risingMain?.let { main ->

                val velocity =
                    calculateVelocity(
                        flowM3PerHour =
                            risingMainFlow,
                        diameterMm =
                            main.diameterMm
                    )

                val friction =
                    calculateFrictionLoss(
                        flowM3PerHour =
                            risingMainFlow,

                        diameterMm =
                            main.diameterMm,

                        lengthM =
                            main.lengthM,

                        material =
                            main.material
                    )

                val valid =
                    risingMainFlow > 0.0 &&
                        main.diameterMm > 0.0 &&
                        main.lengthM > 0.0 &&
                        velocity > 0.0 &&
                        velocity.isFinite() &&
                        friction.isFinite()

                main.copy(
                    flowM3PerHour =
                        risingMainFlow,

                    velocityMPerS =
                        velocity,

                    frictionLossM =
                        friction,

                    status =
                        if (valid) {
                            DesignCalculationStatus.CALCULATED
                        } else {
                            DesignCalculationStatus.DATA_INCOMPLETE
                        }
                )
            }

        /*
         * ========================================================
         * HEADS
         * ========================================================
         */

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
                staticHeadM =
                    sewage.staticHeadM,

                frictionHeadM =
                    frictionHead,

                minorLossHeadM =
                    minorLossHead
            )

        /*
         * ========================================================
         * PUMP DUTY / STANDBY
         * ========================================================
         */

        val dutyPumpCount =
            sewage.pumps
                .count {
                    it.duty &&
                        !it.standby
                }
                .coerceAtLeast(1)

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
                    pump.flowM3PerHour
                        .takeIf {
                            it.isFinite() &&
                                it > 0.0
                        }
                        ?: dutyFlowPerPump

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
                                        value =
                                            pumpFlow,

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

        /*
         * ========================================================
         * OVERALL STATUS
         * ========================================================
         */

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

        /*
         * ========================================================
         * SAVE CALCULATED PROJECT MODEL
         * ========================================================
         */

        return project.withSewage(
            sewage.copy(

                wetWell =
                    calculatedWetWell,

                risingMain =
                    calculatedRisingMain,

                tdhM =
                    tdh,

                pumps =
                    calculatedPumps,

                status =
                    status
            )
        )
    }

    /*
     * ============================================================
     * HAZEN-WILLIAMS C VALUES
     * ============================================================
     */

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
