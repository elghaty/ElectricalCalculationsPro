package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.electrical.ElectricalDesignEngine
import com.electrical.calculationspro.data.water.WaterDesignEngine
import com.electrical.calculationspro.data.sewage.SewageDesignEngine

object DesignProjectEngine {

    fun recalculateElectrical(
        project: DesignProject
    ): DesignProject {

        val calculated =
            ElectricalDesignEngine.recalculate(project)

        return DesignProjects.save(calculated)
    }

    fun recalculateWater(
        project: DesignProject
    ): DesignProject {

        val calculated =
            WaterDesignEngine.recalculate(project)

        return DesignProjects.save(calculated)
    }

    fun recalculateSewage(
        project: DesignProject
    ): DesignProject {

        val calculated =
            SewageDesignEngine.recalculate(project)

        return DesignProjects.save(calculated)
    }

    fun recalculateAll(
        project: DesignProject
    ): DesignProject {

        val electrical =
            ElectricalDesignEngine.recalculate(project)

        val water =
            WaterDesignEngine.recalculate(electrical)

        val sewage =
            SewageDesignEngine.recalculate(water)

        return DesignProjects.save(
            sewage
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
