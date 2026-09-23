package com.electrical.calculationspro.data.sewage

import com.electrical.calculationspro.data.project.DesignCalculationStatus
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects
import com.electrical.calculationspro.data.project.RisingMainDesign
import com.electrical.calculationspro.data.project.SewagePump
import com.electrical.calculationspro.data.project.WetWellDesign

object SewageDesignModule {

    fun updateFlows(
        project: DesignProject,
        averageFlowM3PerDay: Double,
        peakFlowM3PerDay: Double,
        minimumFlowM3PerDay: Double
    ): DesignProject {

        val design =
            project.sewage.copy(
                averageFlowM3PerDay =
                    averageFlowM3PerDay,
                peakFlowM3PerDay =
                    peakFlowM3PerDay,
                minimumFlowM3PerDay =
                    minimumFlowM3PerDay,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withSewage(design)
        )
    }

    fun setWetWell(
        project: DesignProject,
        wetWell: WetWellDesign
    ): DesignProject {

        val design =
            project.sewage.copy(
                wetWell = wetWell,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withSewage(design)
        )
    }

    fun setRisingMain(
        project: DesignProject,
        risingMain: RisingMainDesign
    ): DesignProject {

        val design =
            project.sewage.copy(
                risingMain = risingMain,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withSewage(design)
        )
    }

    fun addPump(
        project: DesignProject,
        pump: SewagePump
    ): DesignProject {

        val design =
            project.sewage.copy(
                pumps =
                    project.sewage.pumps
                        .filterNot { it.id == pump.id } +
                        pump,
                status =
                    DesignCalculationStatus.IN_PROGRESS
            )

        return DesignProjects.save(
            project.withSewage(design)
        )
    }

    fun markCalculated(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.withSewage(
                project.sewage.copy(
                    status =
                        DesignCalculationStatus.CALCULATED
                )
            )
        )
}
