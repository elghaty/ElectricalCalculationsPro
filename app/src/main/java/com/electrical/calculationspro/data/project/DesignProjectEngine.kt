package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.electrical.ElectricalDesignEngine
import com.electrical.calculationspro.data.sewage.SewageDesignEngine
import com.electrical.calculationspro.data.water.WaterDesignEngine

/**
 * Central orchestration layer for the complete engineering project.
 *
 * Dependency order:
 *
 * WATER
 *   -> Pump hydraulic duty
 *   -> Motor input power
 *
 * SEWAGE
 *   -> Sewage pump hydraulic duty
 *   -> Motor input power
 *
 * ELECTRICAL
 *   -> Loads
 *   -> Currents
 *   -> Panels
 *   -> Protection
 *
 * SLD is intentionally NOT rebuilt here.
 */
object DesignProjectEngine {

    fun recalculateElectrical(
        project: DesignProject
    ): DesignProject {

        val result =
            ElectricalDesignEngine.recalculate(
                project
            )

        return DesignProjects.save(result)
    }

    fun recalculateWater(
        project: DesignProject
    ): DesignProject {

        val result =
            WaterDesignEngine.recalculate(
                project
            )

        return DesignProjects.save(result)
    }

    fun recalculateSewage(
        project: DesignProject
    ): DesignProject {

        val result =
            SewageDesignEngine.recalculate(
                project
            )

        return DesignProjects.save(result)
    }

    /**
     * Complete engineering calculation.
     *
     * Hydraulic systems are always calculated first because
     * their pump motor powers can become electrical loads.
     */
    fun recalculateAll(
        project: DesignProject
    ): DesignProject {

        var result = project

        result =
            WaterDesignEngine.recalculate(
                result
            )

        result =
            SewageDesignEngine.recalculate(
                result
            )

        result =
            ElectricalDesignEngine.recalculate(
                result
            )

        return DesignProjects.save(
            result.updateTimestamp()
        )
    }

    fun recalculate(
        project: DesignProject,
        discipline: DesignDiscipline
    ): DesignProject {

        return when (discipline) {

            DesignDiscipline.ELECTRICAL ->
                recalculateElectrical(project)

            DesignDiscipline.WATER ->
                recalculateWater(project)

            DesignDiscipline.SEWAGE ->
                recalculateSewage(project)
        }
    }

    fun updateProject(
        project: DesignProject,
        transform: (DesignProject) -> DesignProject
    ): DesignProject {

        val updated =
            transform(project)
                .updateTimestamp()

        return DesignProjects.save(
            updated
        )
    }

    fun updateAndRecalculate(
        project: DesignProject,
        discipline: DesignDiscipline,
        transform: (DesignProject) -> DesignProject
    ): DesignProject {

        val updated =
            transform(project)
                .updateTimestamp()

        return recalculate(
            updated,
            discipline
        )
    }

    fun calculateAndValidate(
        project: DesignProject
    ): ProjectCalculationResult {

        val calculated =
            recalculateAll(project)

        val validation =
            DesignProjectValidator.validate(
                calculated
            )

        val finalProject =
            DesignProjects.save(
                calculated
            )

        return ProjectCalculationResult(
            project = finalProject,
            validation = validation
        )
    }

    fun validate(
        project: DesignProject
    ): DesignProjectValidationResult =
        DesignProjectValidator.validate(
            project
        )

    fun validateAndSave(
        project: DesignProject
    ): Pair<
        DesignProject,
        DesignProjectValidationResult
    > {

        val updated =
            project.updateTimestamp()

        val validation =
            DesignProjectValidator.validate(
                updated
            )

        val saved =
            DesignProjects.save(
                updated
            )

        return saved to validation
    }

    fun start(
        project: DesignProject
    ): DesignProject {

        val started =
            project.start()

        return DesignProjects.save(
            started
        )
    }

    fun complete(
        project: DesignProject
    ): DesignProject {

        val validation =
            DesignProjectValidator.validate(
                project
            )

        if (!validation.valid) {

            return DesignProjects.save(
                project.copy(
                    status =
                        DesignStatus.IN_PROGRESS
                )
            )
        }

        return DesignProjects.save(
            project.complete()
        )
    }

    fun archive(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project.archive()
        )
}

data class ProjectCalculationResult(
    val project: DesignProject,
    val validation: DesignProjectValidationResult
)
