package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.electrical.ElectricalDesignEngine
import com.electrical.calculationspro.data.sewage.SewageDesignEngine
import com.electrical.calculationspro.data.water.WaterDesignEngine

/**
 * ================================================================
 * PROFESSIONAL DESIGN PROJECT ENGINE
 * ================================================================
 *
 * Central orchestration layer for the complete engineering project.
 *
 * Engineering dependency order:
 *
 * WATER
 *   ↓
 * Pump Hydraulic Calculation
 *   ↓
 * Motor kW
 *   ↓
 * SEWAGE
 *   ↓
 * Sewage Pump Motor kW
 *   ↓
 * ELECTRICAL LOAD INTEGRATION
 *   ↓
 * Load Current
 *   ↓
 * Panel Totals
 *   ↓
 * SLD / Protection / Reports
 *
 * UI must never orchestrate individual engineering calculations.
 *
 * UI
 *   ↓
 * DesignProjectCoreBridge
 *   ↓
 * DesignProjectEngine
 *   ↓
 * Discipline Engines
 *   ↓
 * Calculators / Standards / Catalogs
 *
 * IMPORTANT:
 *
 * Manual SLD data are not overwritten by normal project
 * recalculation. SLD rebuilding remains an explicit operation.
 *
 * ================================================================
 */
object DesignProjectEngine {

    /**
     * Recalculate the complete electrical discipline only.
     *
     * This does not recalculate hydraulic disciplines.
     *
     * Use this when the user changes electrical data only.
     */
    fun recalculateElectrical(
        project: DesignProject
    ): DesignProject {

        val result =
            ElectricalDesignEngine.recalculate(
                project
            )

        return DesignProjects.save(result)
    }

    /**
     * Recalculate the complete water discipline.
     *
     * Pump motor power is calculated here.
     */
    fun recalculateWater(
        project: DesignProject
    ): DesignProject {

        val result =
            WaterDesignEngine.recalculate(
                project
            )

        return DesignProjects.save(result)
    }

    /**
     * Recalculate the complete sewage discipline.
     *
     * Sewage pump motor power is calculated here.
     */
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
     * ============================================================
     * COMPLETE PROJECT CALCULATION
     * ============================================================
     *
     * Correct engineering dependency order:
     *
     * 1. Water
     * 2. Sewage
     * 3. Electrical
     *
     * Water/Sewage pumps produce motorPowerKw.
     *
     * ElectricalDesignEngine then converts those motor powers
     * into electrical loads.
     *
     * Therefore electrical calculation MUST occur after
     * hydraulic calculations.
     */
    fun recalculateAll(
        project: DesignProject
    ): DesignProject {

        var result = project

        /**
         * --------------------------------------------------------
         * STEP 1
         * WATER ENGINEERING
         * --------------------------------------------------------
         *
         * Calculates:
         * - Flow
         * - TDH
         * - Hydraulic power
         * - Shaft power
         * - Motor input power
         * - Energy
         */
        result =
            WaterDesignEngine.recalculate(
                result
            )

        /**
         * --------------------------------------------------------
         * STEP 2
         * SEWAGE ENGINEERING
         * --------------------------------------------------------
         *
         * Calculates:
         * - Sewage TDH
         * - Pump motor power
         * - Energy
         */
        result =
            SewageDesignEngine.recalculate(
                result
            )

        /**
         * --------------------------------------------------------
         * STEP 3
         * ELECTRICAL ENGINEERING
         * --------------------------------------------------------
         *
         * ElectricalDesignEngine now sees the calculated
         * water/sewage pump motor powers.
         *
         * It creates synchronized electrical loads and then
         * calculates their currents and panel totals.
         */
        result =
            ElectricalDesignEngine.recalculate(
                result
            )

        /**
         * Save only the final project state.
         *
         * This prevents unnecessary intermediate persistence
         * during a complete calculation.
         */
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
     * ============================================================
     * COMPLETE CALCULATION + VALIDATION
     * ============================================================
     */
    fun calculateAndValidate(
        project: DesignProject
    ): ProjectCalculationResult {

        /**
         * Full dependency-aware calculation.
         */
        val calculated =
            recalculateAll(project)

        /**
         * Project validation is intentionally performed after
         * all discipline calculations have completed.
         */
        val validation =
            DesignProjectValidator.validate(
                calculated
            )

        /**
         * Persist the complete calculated project.
         */
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
                    status = DesignStatus.IN_PROGRESS
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

/**
 * Result of a complete project calculation and validation.
 */
data class ProjectCalculationResult(
    val project: DesignProject,
    val validation: DesignProjectValidationResult
)
