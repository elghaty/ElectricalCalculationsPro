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

    fun calculateVelocity(
        flowM3PerHour: Double,
        diameterMm: Double
    ): Double {

        if (
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
            PI * diameterM.pow(2.0) / 4.0

        return if (area > 0.0) {
            q / area
        } else {
            0.0
        }
    }

    fun calculateFrictionLoss(
        flowM3PerHour: Double,
        diameterMm: Double,
        lengthM: Double,
        material: String
    ): Double {

        if (
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

        return PI *
            diameterM.pow(2.0) /
            4.0 *
            effectiveDepthM
    }

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
                        wetWell.diameterM,
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
            risingMain?.flowM3PerHour
                ?.takeIf { it > 0.0 }
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
                        velocity > 0.0

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
                    pump.flowM3PerHour
                        .takeIf {
                            it.isFinite() && it > 0.0
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
                                        requiredPressureHeadM = 0.0
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
