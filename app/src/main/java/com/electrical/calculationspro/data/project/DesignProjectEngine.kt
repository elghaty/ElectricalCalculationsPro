package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.calculators.LoadCalculator

/**
 * Single project-level orchestration boundary.
 *
 * UI must change DesignProject through this engine instead of maintaining
 * independent calculation state.
 *
 * Mathematical, code and catalogue calculations remain in their
 * dedicated engineering layers.
 */
object DesignProjectEngine {

    fun recalculateElectrical(
        project: DesignProject
    ): DesignProject {

        val loads =
            project.electrical.loads.map { load ->

                val baseKw =
                    if (load.designLoadKw > 0.0) {
                        load.designLoadKw
                    } else {
                        load.connectedLoadKw
                    }

                if (
                    baseKw <= 0.0 ||
                    load.voltageV <= 0.0 ||
                    load.powerFactor <= 0.0 ||
                    load.powerFactor > 1.0
                ) {

                    load.copy(
                        status =
                            DesignCalculationStatus.DATA_INCOMPLETE
                    )

                } else {

                    val currentType =
                        when (load.phases) {

                            1 ->
                                CurrentType.AlternatingSinglePhase

                            2 ->
                                CurrentType.AlternatingTwoPhase

                            else ->
                                CurrentType.AlternatingThreePhase
                        }

                    val rawCurrent =
                        LoadCalculator.designCurrentFromKw(
                            loadKw = baseKw,
                            voltage = load.voltageV,
                            powerFactor = load.powerFactor,
                            currentType = currentType
                        )

                    val designCurrent =
                        LoadCalculator.applyDemandAndDiversity(
                            current = rawCurrent,
                            demandFactor =
                                load.demandFactor
                                    .coerceIn(0.0, 1.0),
                            diversityFactor =
                                load.diversityFactor
                                    .coerceIn(0.0, 1.0)
                        )

                    load.copy(
                        designLoadKw = baseKw,
                        designCurrentA = designCurrent,
                        status =
                            DesignCalculationStatus.CALCULATED
                    )
                }
            }

        val panels =
            project.electrical.panels.map { panel ->

                val panelLoads =
                    loads.filter {
                        it.sourcePanelId == panel.id
                    }

                val totalKw =
                    panelLoads.sumOf {
                        it.designLoadKw *
                            it.quantity.coerceAtLeast(1)
                    }

                val totalCurrent =
                    panelLoads.sumOf {
                        it.designCurrentA *
                            it.quantity.coerceAtLeast(1)
                    }

                panel.copy(
                    designLoadKw = totalKw,
                    designCurrentA = totalCurrent,
                    status =
                        if (panelLoads.isEmpty()) {
                            DesignCalculationStatus.DATA_INCOMPLETE
                        } else {
                            DesignCalculationStatus.CALCULATED
                        }
                )
            }

        val electricalStatus =
            when {

                loads.any {
                    it.status ==
                        DesignCalculationStatus.INVALID
                } ||
                    panels.any {
                        it.status ==
                            DesignCalculationStatus.INVALID
                    } ->
                    DesignCalculationStatus.INVALID

                loads.any {
                    it.status ==
                        DesignCalculationStatus.DATA_INCOMPLETE
                } ||
                    panels.any {
                        it.status ==
                            DesignCalculationStatus.DATA_INCOMPLETE
                    } ->
                    DesignCalculationStatus.DATA_INCOMPLETE

                loads.isNotEmpty() ||
                    panels.isNotEmpty() ->
                    DesignCalculationStatus.CALCULATED

                else ->
                    DesignCalculationStatus.NOT_STARTED
            }

        return DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    loads = loads,
                    panels = panels,
                    status = electricalStatus
                )
            )
        )
    }

    fun updateProject(
        project: DesignProject,
        transform: (DesignProject) -> DesignProject
    ): DesignProject {

        val updated =
            transform(project)
                .updateTimestamp()

        return DesignProjects.save(updated)
    }

    fun validateAndSave(
        project: DesignProject
    ): Pair<
        DesignProject,
        DesignProjectValidationResult
        > {

        val updated =
            project.updateTimestamp()

        val validation =
            DesignProjectValidator.validate(updated)

        val saved =
            DesignProjects.save(updated)

        return saved to validation
    }
}
