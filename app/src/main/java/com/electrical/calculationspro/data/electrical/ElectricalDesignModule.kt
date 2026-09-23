package com.electrical.calculationspro.data.electrical

import com.electrical.calculationspro.data.CalculationHistoryItem
import com.electrical.calculationspro.data.CalculationModels
import com.electrical.calculationspro.data.CalculationStatus
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.EngineeringCalculationPackage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.ElectricalDesign
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
                        .filterNot { it.id == panel.id } +
                        panel,
                status =
                    com.electrical.calculationspro.data.project.DesignCalculationStatus.IN_PROGRESS
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
                        .filterNot { it.id == load.id } +
                        load,
                status =
                    com.electrical.calculationspro.data.project.DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withElectrical(design)
        )
    }

    fun calculateLoadCurrent(
        project: DesignProject,
        load: ElectricalLoad
    ): Double {

        val standard =
            project.electricalStandard
                ?: Standard.IEC

        return DesignProjectCoreBridge.calculateDesignCurrentFromKw(
            loadKw = load.designLoadKw.takeIf { it > 0.0 }
                ?: load.connectedLoadKw,
            voltage = load.voltageV,
            powerFactor = load.powerFactor,
            currentType =
                if (load.phases == 1)
                    CurrentType.SINGLE_PHASE
                else
                    CurrentType.THREE_PHASE
        )
    }

    fun markCalculated(
        project: DesignProject
    ): DesignProject {

        val design =
            project.electrical.copy(
                status =
                    com.electrical.calculationspro.data.project.DesignCalculationStatus.CALCULATED
            )

        return DesignProjects.save(
            project.withElectrical(design)
        )
    }

    fun validate(
        project: DesignProject
    ) =
        com.electrical.calculationspro.data.project.DesignProjectValidator
            .validate(project)
}
