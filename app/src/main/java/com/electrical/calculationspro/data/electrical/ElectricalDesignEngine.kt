package com.electrical.calculationspro.data.electrical

import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.calculators.LoadCalculator
import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.ElectricalLoad
import com.electrical.calculationspro.data.project.ElectricalPanel

/**
 * Central electrical project calculation engine.
 *
 * Responsibilities:
 * - Calculate load currents.
 * - Apply project demand/diversity factors consistently.
 * - Aggregate loads into panels.
 * - Maintain calculation status.
 *
 * UI must not contain electrical calculation logic.
 */
object ElectricalDesignEngine {

    private const val EPSILON = 1.0e-9

    /**
     * Returns the current before demand/diversity factors.
     */
    fun calculateLoadCurrent(
        load: ElectricalLoad
    ): Double? {

        val loadKw =
            load.designLoadKw
                .takeIf { it > EPSILON }
                ?: load.connectedLoadKw

        if (
            loadKw <= EPSILON ||
            load.voltageV <= EPSILON ||
            load.powerFactor <= EPSILON ||
            load.powerFactor > 1.0 ||
            load.phases !in 1..3
        ) {
            return null
        }

        val currentType =
            when (load.phases) {
                1 ->
                    CurrentType.AlternatingSinglePhase

                2 ->
                    CurrentType.AlternatingTwoPhase

                else ->
                    CurrentType.AlternatingThreePhase
            }

        return runCatching {
            LoadCalculator.designCurrentFromKw(
                loadKw = loadKw,
                voltage = load.voltageV,
                powerFactor = load.powerFactor,
                currentType = currentType
            )
        }.getOrNull()
    }

    /**
     * Returns the final design current after the factors
     * already stored on the load.
     */
    fun calculateFinalLoadCurrent(
        load: ElectricalLoad
    ): Double? {

        val current =
            calculateLoadCurrent(load)
                ?: return null

        return runCatching {
            LoadCalculator.applyDemandAndDiversity(
                current = current,
                demandFactor =
                    load.demandFactor
                        .coerceIn(0.0, 1.0),
                diversityFactor =
                    load.diversityFactor
                        .coerceIn(0.0, 1.0)
            )
        }.getOrNull()
    }

    /**
     * Recalculate all electrical loads and panels.
     *
     * Quantity is applied only at panel aggregation level.
     * This prevents multiplying a single-load current twice.
     */
    fun recalculate(
        project: DesignProject
    ): DesignProject {

        val calculatedLoads =
            project.electrical.loads.map { load ->

                val loadKw =
                    load.designLoadKw
                        .takeIf { it > EPSILON }
                        ?: load.connectedLoadKw

                val calculatedCurrent =
                    calculateFinalLoadCurrent(load)

                if (
                    loadKw <= EPSILON ||
                    calculatedCurrent == null
                ) {

                    load.copy(
                        status =
                            DesignCalculationStatus
                                .DATA_INCOMPLETE
                    )

                } else {

                    load.copy(
                        designLoadKw =
                            loadKw,

                        designCurrentA =
                            calculatedCurrent,

                        status =
                            DesignCalculationStatus
                                .CALCULATED
                    )
                }
            }

        val calculatedPanels =
            project.electrical.panels.map { panel ->

                calculatePanelTotals(
                    panel = panel,
                    loads = calculatedLoads
                )
            }

        val electrical =
            project.electrical.copy(
                loads =
                    calculatedLoads,

                panels =
                    calculatedPanels,

                status =
                    calculateStatus(
                        loads =
                            calculatedLoads,
                        panels =
                            calculatedPanels
                    )
            )

        return project.withElectrical(electrical)
    }

    /**
     * Calculate one panel from its connected loads.
     */
    fun calculatePanelTotals(
        panel: ElectricalPanel,
        loads: List<ElectricalLoad>
    ): ElectricalPanel {

        val panelLoads =
            loads.filter {
                it.sourcePanelId == panel.id
            }

        if (panelLoads.isEmpty()) {

            return panel.copy(
                designLoadKw = 0.0,
                designCurrentA = 0.0,
                status =
                    DesignCalculationStatus
                        .DATA_INCOMPLETE
            )
        }

        val validLoads =
            panelLoads.filter {
                it.status !=
                    DesignCalculationStatus.INVALID
            }

        if (validLoads.isEmpty()) {

            return panel.copy(
                designLoadKw = 0.0,
                designCurrentA = 0.0,
                status =
                    DesignCalculationStatus.INVALID
            )
        }

        val totalLoadKw =
            validLoads.sumOf { load ->

                load.designLoadKw *
                    load.quantity
                        .coerceAtLeast(1)
            }

        val totalCurrentA =
            validLoads.sumOf { load ->

                load.designCurrentA *
                    load.quantity
                        .coerceAtLeast(1)
            }

        val hasIncompleteData =
            validLoads.any {
                it.status ==
                    DesignCalculationStatus
                        .DATA_INCOMPLETE
            }

        return panel.copy(
            designLoadKw =
                totalLoadKw,

            designCurrentA =
                totalCurrentA,

            status =
                if (hasIncompleteData) {
                    DesignCalculationStatus
                        .DATA_INCOMPLETE
                } else {
                    DesignCalculationStatus
                        .CALCULATED
                }
        )
    }

    private fun calculateStatus(
        loads: List<ElectricalLoad>,
        panels: List<ElectricalPanel>
    ): DesignCalculationStatus {

        if (
            loads.any {
                it.status ==
                    DesignCalculationStatus.INVALID
            } ||
            panels.any {
                it.status ==
                    DesignCalculationStatus.INVALID
            }
        ) {
            return DesignCalculationStatus.INVALID
        }

        if (
            loads.any {
                it.status ==
                    DesignCalculationStatus
                        .DATA_INCOMPLETE
            } ||
            panels.any {
                it.status ==
                    DesignCalculationStatus
                        .DATA_INCOMPLETE
            }
        ) {
            return DesignCalculationStatus.DATA_INCOMPLETE
        }

        if (
            loads.isNotEmpty() ||
            panels.isNotEmpty()
        ) {
            return DesignCalculationStatus.CALCULATED
        }

        return DesignCalculationStatus.NOT_STARTED
    }
}
