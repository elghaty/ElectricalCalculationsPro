package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.electrical.ElectricalDesignEngine
import com.electrical.calculationspro.data.sewage.SewageDesignEngine
import com.electrical.calculationspro.data.water.WaterDesignEngine

/**
 * Central project orchestration layer.
 *
 * UI must never orchestrate individual engineering calculations.
 *
 * UI
 *  ↓
 * DesignProjectCoreBridge
 *  ↓
 * DesignProjectEngine
 *  ↓
 * Discipline Engines
 *  ↓
 * Calculators / Standards / Catalogs
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
     * Recalculate the complete project in engineering order.
     */
    fun recalculateAll(
        project: DesignProject
    ): DesignProject {

        var result =
            ElectricalDesignEngine.recalculate(
                project
            )

        result =
            WaterDesignEngine.recalculate(
                result
            )

        result =
            SewageDesignEngine.recalculate(
                result
            )

        return DesignProjects.save(result)
    }

    /**
     * Recalculate only the requested discipline.
     */
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

    /**
     * Project-level update.
     */
    fun updateProject(
        project: DesignProject,
        transform: (DesignProject) -> DesignProject
    ): DesignProject {

        val updated =
            transform(project)
                .updateTimestamp()

        return DesignProjects.save(updated)
    }

    /**
     * Update project and recalculate the selected discipline.
     */
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

    /**
     * Complete project calculation followed by validation.
     */
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

    /**
     * Validate without changing engineering results.
     */
    fun validate(
        project: DesignProject
    ): DesignProjectValidationResult =
        DesignProjectValidator.validate(project)

    /**
     * Validate and persist the project.
     */
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

    /**
     * Start a project.
     */
    fun start(
        project: DesignProject
    ): DesignProject {

        val started =
            project.start()

        return DesignProjects.save(
            started
        )
    }

    /**
     * Complete only after project validation.
     *
     * A project containing validation errors remains
     * IN_PROGRESS and is not marked COMPLETED.
     */
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

    /**
     * Archive the project without modifying engineering data.
     */
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
