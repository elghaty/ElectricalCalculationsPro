package com.electrical.calculationspro.data.electrical

import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.DesignProjectEngine
import com.electrical.calculationspro.data.project.DesignProjectValidator
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.ElectricalLoad
import com.electrical.calculationspro.data.project.ElectricalPanel

object ElectricalDesignModule {

    fun addPanel(
        project: DesignProject,
        panel: ElectricalPanel
    ): DesignProject {

        val panels =
            project.electrical.panels
                .filterNot { it.id == panel.id } +
                panel

        return DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    panels = panels,
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun addLoad(
        project: DesignProject,
        load: ElectricalLoad
    ): DesignProject {

        val loads =
            project.electrical.loads
                .filterNot { it.id == load.id } +
                load

        return DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    loads = loads,
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun calculateLoadCurrent(
        load: ElectricalLoad
    ): Double =
        ElectricalDesignEngine
            .calculateLoadCurrent(load)
            ?: 0.0

    fun updateLoadCalculatedValues(
        project: DesignProject,
        loadId: String
    ): DesignProject {

        val load =
            project.electrical.loads
                .firstOrNull {
                    it.id == loadId
                }
                ?: return project

        val current =
            ElectricalDesignEngine
                .calculateLoadCurrent(load)

        if (current == null) {

            return DesignProjects.save(
                project.withElectrical(
                    project.electrical.copy(
                        loads =
                            project.electrical.loads.map {
                                if (it.id == loadId) {
                                    it.copy(
                                        status =
                                            DesignCalculationStatus
                                                .DATA_INCOMPLETE
                                    )
                                } else {
                                    it
                                }
                            }
                    )
                )
            )
        }

        val loadKw =
            load.designLoadKw
                .takeIf { it > 0.0 }
                ?: load.connectedLoadKw

        val finalCurrent =
            current *
                load.demandFactor
                    .coerceIn(0.0, 1.0) *
                load.diversityFactor
                    .coerceIn(0.0, 1.0)

        return DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    loads =
                        project.electrical.loads.map {
                            if (it.id == loadId) {
                                it.copy(
                                    designLoadKw = loadKw,
                                    designCurrentA = finalCurrent,
                                    status =
                                        DesignCalculationStatus
                                            .CALCULATED
                                )
                            } else {
                                it
                            }
                        },
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )
    }

    fun recalculate(
        project: DesignProject
    ): DesignProject =
        DesignProjectCoreBridge
            .recalculateElectrical(project)

    fun recalculateAll(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine
            .recalculateAll(project)

    fun updatePanelCalculatedValues(
        project: DesignProject,
        panelId: String,
        designLoadKw: Double,
        designCurrentA: Double,
        shortCircuitKA: Double
    ): DesignProject {

        if (
            !designLoadKw.isFinite() ||
            !designCurrentA.isFinite() ||
            !shortCircuitKA.isFinite()
        ) {
            return project
        }

        return DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    panels =
                        project.electrical.panels.map {
                            if (it.id == panelId) {
                                it.copy(
                                    designLoadKw =
                                        designLoadKw,
                                    designCurrentA =
                                        designCurrentA,
                                    shortCircuitKA =
                                        shortCircuitKA,
                                    status =
                                        DesignCalculationStatus
                                            .CALCULATED
                                )
                            } else {
                                it
                            }
                        }
                )
            )
        )
    }

    fun markInProgress(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    status =
                        DesignCalculationStatus.IN_PROGRESS
                )
            )
        )

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    status =
                        DesignCalculationStatus.CALCULATED
                )
            )
        )

    fun markDataIncomplete(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    status =
                        DesignCalculationStatus.DATA_INCOMPLETE
                )
            )
        )

    fun markInvalid(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    status =
                        DesignCalculationStatus.INVALID
                )
            )
        )

    fun validate(
        project: DesignProject
    ) =
        DesignProjectValidator.validate(project)
}
