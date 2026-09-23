package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.CalculationHistory
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.EngineeringCalculationPackage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult

/**
 * ================================================================
 * PROFESSIONAL DESIGN
 * Core Integration
 * ================================================================
 *
 * Integration point between:
 *
 * UI
 *   ↓
 * Project
 *   ↓
 * Professional Core
 *
 * This class does NOT duplicate engineering calculations.
 * All calculations remain inside ElectricalCalculations.
 * ================================================================
 */

object DesignProjectCoreBridge {

    /**
     * Return active project.
     */
    fun activeProject(): DesignProject? =
        DesignProjects.getActive()

    /**
     * Create a new design and make it active.
     */
    fun createProject(
        projectName: String,
        projectNumber: String = "",
        clientName: String = "",
        consultantName: String = "",
        location: String = "",
        description: String = "",
        standard: Standard = Standard.IEC
    ): DesignProject {

        return DesignProjects.create(
            projectName = projectName,
            projectNumber = projectNumber,
            clientName = clientName,
            consultantName = consultantName,
            location = location,
            description = description,
            standard = standard
        )
    }

    /**
     * Save the project.
     */
    fun saveProject(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(project)

    /**
     * Select active project.
     */
    fun selectProject(
        projectId: String
    ): Boolean =
        DesignProjects.setActive(projectId)

    /**
     * Execute pump engineering through the
     * existing Professional Core.
     *
     * No pump formula is implemented here.
     */
    fun calculatePump(
        input: PumpCalculationInput
    ): PumpCalculationResult =
        ElectricalCalculations.calculatePump(
            input
        )

    /**
     * Store a completed engineering calculation
     * in the calculation history.
     */
    fun saveCalculation(
        calculationType: String,
        standard: Standard,
        summary: String,
        result: EngineeringCalculationPackage
    ) =
        CalculationHistory.saveToActiveProject(
            calculationType = calculationType,
            standard = standard,
            summary = summary,
            result = result
        )
}
