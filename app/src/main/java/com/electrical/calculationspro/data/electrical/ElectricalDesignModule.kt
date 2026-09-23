package com.electrical.calculationspro.data.electrical

import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.DesignProjectValidator
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.ElectricalLoad
import com.electrical.calculationspro.data.project.ElectricalPanel

/**
 * Electrical project orchestration layer.
 *
 * Engineering calculations are delegated to the Core facade.
 * This module owns project state only.
 */
object ElectricalDesignModule {

    fun addPanel(
        project: DesignProject,
        panel: ElectricalPanel
    ): DesignProject {

        val updatedPanels =
            project.electrical.panels
                .filterNot { it.id == panel.id } + panel

        val design =
            project.electrical.copy(
                panels = updatedPanels,
                status = DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withElectrical(design)
        )
    }

    fun addLoad(
        project: DesignProject,
        load: ElectricalLoad
    ): DesignProject {

        val updatedLoads =
            project.electrical.loads
                .filterNot { it.id == load.id } + load

        val design =
            project.electrical.copy(
                loads = updatedLoads,
                status = DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withElectrical(design)
        )
    }

    fun calculateLoadCurrent(
        load: ElectricalLoad
    ): Double {

        val designLoadKw =
            load.designLoadKw
                .takeIf { it > 0.0 }
                ?: load.connectedLoadKw

        val currentType =
            when (load.phases) {
                1 -> CurrentType.SINGLE_PHASE
                else -> CurrentType.THREE_PHASE
            }

        return DesignProjectCoreBridge.calculateDesignCurrentFromKw(
            loadKw = designLoadKw,
            voltage = load.voltageV,
            powerFactor = load.powerFactor,
            currentType = currentType
        )
    }

    fun updateLoadCalculatedValues(
        project: DesignProject,
        loadId: String
    ): DesignProject {

        val oldLoad =
            project.electrical.loads
                .firstOrNull { it.id == loadId }
                ?: return project

        val designLoadKw =
            oldLoad.designLoadKw
                .takeIf { it > 0.0 }
                ?: oldLoad.connectedLoadKw

        val current =
            calculateLoadCurrent(oldLoad)

        val updatedLoad =
            oldLoad.copy(
                designLoadKw = designLoadKw,
                designCurrentA = current,
                status = DesignCalculationStatus.CALCULATED
            )

        val updatedLoads =
            project.electrical.loads.map {
                if (it.id == loadId) {
                    updatedLoad
                } else {
                    it
                }
            }

        val updatedDesign =
            project.electrical.copy(
                loads = updatedLoads,
                status = DesignCalculationStatus.CALCULATED
            )

        return DesignProjects.save(
            project.withElectrical(updatedDesign)
        )
    }

    fun updateLoadStatus(
        project: DesignProject,
        loadId: String,
        status: DesignCalculationStatus
    ): DesignProject {

        val updatedLoads =
            project.electrical.loads.map {
                if (it.id == loadId) {
                    it.copy(status = status)
                } else {
                    it
                }
            }

        return DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    loads = updatedLoads
                )
            )
        )
    }

    fun updatePanelCalculatedValues(
        project: DesignProject,
        panelId: String,
        designLoadKw: Double,
        designCurrentA: Double,
        shortCircuitKA: Double
    ): DesignProject {

        if (!designLoadKw.isFinite() ||
            !designCurrentA.isFinite() ||
            !shortCircuitKA.isFinite()
        ) {
            return project
        }

        val updatedPanels =
            project.electrical.panels.map {
                if (it.id == panelId) {
                    it.copy(
                        designLoadKw = designLoadKw,
                        designCurrentA = designCurrentA,
                        shortCircuitKA = shortCircuitKA,
                        status = DesignCalculationStatus.CALCULATED
                    )
                } else {
                    it
                }
            }

        return DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    panels = updatedPanels,
                    status = DesignCalculationStatus.CALCULATED
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
                    status = DesignCalculationStatus.IN_PROGRESS
                )
            )
        )

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    status = DesignCalculationStatus.CALCULATED
                )
            )
        )

    fun markDataIncomplete(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    status = DesignCalculationStatus.DATA_INCOMPLETE
                )
            )
        )

    fun markInvalid(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withElectrical(
                project.electrical.copy(
                    status = DesignCalculationStatus.INVALID
                )
            )
        )

    fun validate(
        project: DesignProject
    ) =
        DesignProjectValidator.validate(project)
}
