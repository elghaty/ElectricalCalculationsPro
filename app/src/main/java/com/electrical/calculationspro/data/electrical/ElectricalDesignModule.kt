package com.electrical.calculationspro.data.electrical

import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.DesignProjectValidator
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.ElectricalLoad
import com.electrical.calculationspro.data.project.ElectricalPanel

object ElectricalDesignModule {

    fun addPanel(
        project: DesignProject,
        panel: ElectricalPanel
    ): DesignProject {

        val design =
            project.electrical.copy(
                panels =
                    project.electrical.panels
                        .filterNot { it.id == panel.id } + panel,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withElectrical(design)
        )
    }

    fun addLoad(
        project: DesignProject,
        load: ElectricalLoad
    ): DesignProject {

        val design =
            project.electrical.copy(
                loads =
                    project.electrical.loads
                        .filterNot { it.id == load.id } + load,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withElectrical(design)
        )
    }

    fun calculateLoadCurrent(
        load: ElectricalLoad
    ): Double =
        DesignProjectCoreBridge.calculateDesignCurrentFromKw(
            loadKw =
                load.designLoadKw
                    .takeIf { it > 0.0 }
                    ?: load.connectedLoadKw,
            voltage = load.voltageV,
            powerFactor = load.powerFactor,
            currentType =
                if (load.phases == 1)
                    CurrentType.SINGLE_PHASE
                else
                    CurrentType.THREE_PHASE
        )

    fun updateLoadCalculatedValues(
        project: DesignProject,
        loadId: String
    ): DesignProject {

        val oldLoad =
            project.electrical.loads
                .firstOrNull { it.id == loadId }
                ?: return project

        val current =
            calculateLoadCurrent(oldLoad)

        val designLoad =
            oldLoad.designLoadKw
                .takeIf { it > 0.0 }
                ?: oldLoad.connectedLoadKw

        val updatedLoad =
            oldLoad.copy(
                designLoadKw = designLoad,
                designCurrentA = current,
                status = DesignCalculationStatus.CALCULATED
            )

        val updatedDesign =
            project.electrical.copy(
                loads =
                    project.electrical.loads.map {
                        if (it.id == loadId)
                            updatedLoad
                        else
                            it
                    },
                status =
                    DesignCalculationStatus.CALCULATED
            )

        return DesignProjects.save(
            project.withElectrical(updatedDesign)
        )
    }

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

    fun validate(
        project: DesignProject
    ) =
        DesignProjectValidator.validate(project)
}
