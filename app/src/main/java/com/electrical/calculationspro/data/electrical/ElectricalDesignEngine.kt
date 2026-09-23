package com.electrical.calculationspro.data.electrical

import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.calculators.LoadCalculator
import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.ElectricalDesign
import com.electrical.calculationspro.data.project.ElectricalLoad
import com.electrical.calculationspro.data.project.ElectricalPanel

object ElectricalDesignEngine {

    fun calculateLoadCurrent(
        load: ElectricalLoad
    ): Double? {

        val loadKw =
            load.designLoadKw
                .takeIf { it > 0.0 }
                ?: load.connectedLoadKw

        if (
            loadKw <= 0.0 ||
            load.voltageV <= 0.0 ||
            load.powerFactor <= 0.0 ||
            load.powerFactor > 1.0 ||
            load.phases !in 1..3
        ) {
            return null
        }

        val currentType =
            when (load.phases) {
                1 -> CurrentType.AlternatingSinglePhase
                2 -> CurrentType.AlternatingTwoPhase
                else -> CurrentType.AlternatingThreePhase
            }

        return LoadCalculator.designCurrentFromKw(
            loadKw = loadKw,
            voltage = load.voltageV,
            powerFactor = load.powerFactor,
            currentType = currentType
        )
    }

    fun recalculate(
        project: DesignProject
    ): DesignProject {

        val calculatedLoads =
            project.electrical.loads.map { load ->

                val loadKw =
                    load.designLoadKw
                        .takeIf { it > 0.0 }
                        ?: load.connectedLoadKw

                val current =
                    calculateLoadCurrent(load)

                if (
                    loadKw <= 0.0 ||
                    current == null
                ) {
                    load.copy(
                        status =
                            DesignCalculationStatus.DATA_INCOMPLETE
                    )
                } else {

                    val calculatedCurrent =
                        current *
                            load.demandFactor
                                .coerceIn(0.0, 1.0) *
                            load.diversityFactor
                                .coerceIn(0.0, 1.0)

                    load.copy(
                        designLoadKw = loadKw,
                        designCurrentA = calculatedCurrent,
                        status =
                            DesignCalculationStatus.CALCULATED
                    )
                }
            }

        val calculatedPanels =
            project.electrical.panels.map { panel ->

                val panelLoads =
                    calculatedLoads.filter {
                        it.sourcePanelId == panel.id
                    }

                val totalLoadKw =
                    panelLoads.sumOf {
                        it.designLoadKw *
                            it.quantity.coerceAtLeast(1)
                    }

                val totalCurrentA =
                    panelLoads.sumOf {
                        it.designCurrentA *
                            it.quantity.coerceAtLeast(1)
                    }

                if (panelLoads.isEmpty()) {
                    panel.copy(
                        designLoadKw = 0.0,
                        designCurrentA = 0.0,
                        status =
                            DesignCalculationStatus.DATA_INCOMPLETE
                    )
                } else {
                    panel.copy(
                        designLoadKw = totalLoadKw,
                        designCurrentA = totalCurrentA,
                        status =
                            DesignCalculationStatus.CALCULATED
                    )
                }
            }

        val electrical =
            project.electrical.copy(
                loads = calculatedLoads,
                panels = calculatedPanels,
                status = calculateStatus(
                    calculatedLoads,
                    calculatedPanels
                )
            )

        return project.withElectrical(electrical)
    }

    private fun calculateStatus(
        loads: List<ElectricalLoad>,
        panels: List<ElectricalPanel>
    ): DesignCalculationStatus {

        if (
            loads.any {
                it.status == DesignCalculationStatus.INVALID
            } ||
            panels.any {
                it.status == DesignCalculationStatus.INVALID
            }
        ) {
            return DesignCalculationStatus.INVALID
        }

        if (
            loads.any {
                it.status ==
                    DesignCalculationStatus.DATA_INCOMPLETE
            } ||
            panels.any {
                it.status ==
                    DesignCalculationStatus.DATA_INCOMPLETE
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

    fun calculatePanelTotals(
        panel: ElectricalPanel,
        loads: List<ElectricalLoad>
    ): ElectricalPanel {

        val panelLoads =
            loads.filter {
                it.sourcePanelId == panel.id
            }

        return panel.copy(
            designLoadKw =
                panelLoads.sumOf {
                    it.designLoadKw *
                        it.quantity.coerceAtLeast(1)
                },
            designCurrentA =
                panelLoads.sumOf {
                    it.designCurrentA *
                        it.quantity.coerceAtLeast(1)
                },
            status =
                if (panelLoads.isEmpty()) {
                    DesignCalculationStatus.DATA_INCOMPLETE
                } else {
                    DesignCalculationStatus.CALCULATED
                }
        )
    }
}
