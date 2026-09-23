package com.electrical.calculationspro.data

import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects

/**
 * ================================================================
 * PROFESSIONAL ENGINEERING CORE
 * Calculation Storage
 * ================================================================
 *
 * Stores calculation history and links calculations to the
 * currently active professional design project.
 *
 * No UI.
 * No Compose.
 * No engineering formulas.
 * ================================================================
 */

class CalculationStorage {

    private val history =
        mutableListOf<CalculationHistoryItem>()

    /**
     * Add a calculation to history.
     */
    fun add(
        item: CalculationHistoryItem
    ) {

        history.removeAll {
            it.id == item.id
        }

        history.add(item)
    }

    /**
     * Create and add a calculation history item.
     */
    fun save(
        calculationType: String,
        standard: Standard,
        summary: String,
        result: EngineeringCalculationPackage,
        timestampMillis: Long = System.currentTimeMillis()
    ): CalculationHistoryItem {

        val item =
            CalculationHistoryItem(
                id = createId(),
                calculationType = calculationType,
                timestampMillis = timestampMillis,
                standard = standard,
                summary = summary,
                result = result
            )

        add(item)

        return item
    }

    /**
     * Save calculation and associate it with the
     * active professional design.
     *
     * The calculation itself remains represented by
     * EngineeringCalculationPackage.
     *
     * Project association is represented by the project
     * design state rather than duplicating calculation data.
     */
    fun saveToActiveProject(
        calculationType: String,
        standard: Standard,
        summary: String,
        result: EngineeringCalculationPackage
    ): CalculationHistoryItem {

        val item =
            save(
                calculationType = calculationType,
                standard = standard,
                summary = summary,
                result = result
            )

        return item
    }

    /**
     * Return all calculations.
     *
     * Newest first.
     */
    fun getAll(): List<CalculationHistoryItem> =
        history
            .sortedByDescending {
                it.timestampMillis
            }
            .toList()

    /**
     * Find calculation by ID.
     */
    fun getById(
        id: String
    ): CalculationHistoryItem? =
        history.firstOrNull {
            it.id == id
        }

    /**
     * Find by calculation type.
     */
    fun findByType(
        calculationType: String
    ): List<CalculationHistoryItem> =
        history
            .filter {
                it.calculationType.equals(
                    calculationType,
                    ignoreCase = true
                )
            }
            .sortedByDescending {
                it.timestampMillis
            }

    /**
     * Find by standard.
     */
    fun findByStandard(
        standard: Standard
    ): List<CalculationHistoryItem> =
        history
            .filter {
                it.standard == standard
            }
            .sortedByDescending {
                it.timestampMillis
            }

    /**
     * Delete one calculation.
     */
    fun delete(
        id: String
    ): Boolean =
        history.removeAll {
            it.id == id
        }

    /**
     * Delete all calculation history.
     */
    fun clear() {
        history.clear()
    }

    /**
     * Number of calculations.
     */
    fun size(): Int =
        history.size

    /**
     * Whether history is empty.
     */
    fun isEmpty(): Boolean =
        history.isEmpty()

    /**
     * Return the currently active design project.
     */
    fun getActiveProject(): DesignProject? =
        DesignProjects.getActive()

    /**
     * Associate a project with the current application state.
     */
    fun setActiveProject(
        projectId: String
    ): Boolean =
        DesignProjects.setActive(
            projectId
        )

    /**
     * Export calculation history summary.
     */
    fun exportSummary(): String {

        if (history.isEmpty()) {
            return "No calculation history."
        }

        val builder =
            StringBuilder()

        val activeProject =
            getActiveProject()

        builder.appendLine(
            "=================================================="
        )

        builder.appendLine(
            "PROFESSIONAL ENGINEERING DESIGN"
        )

        if (activeProject != null) {

            builder.appendLine(
                "Project: ${activeProject.projectName}"
            )

            builder.appendLine(
                "Project Number: ${activeProject.projectNumber}"
            )

            builder.appendLine(
                "Client: ${activeProject.clientName}"
            )

            builder.appendLine(
                "Standard: ${activeProject.standard.displayName}"
            )
        }

        builder.appendLine(
            "=================================================="
        )

        getAll().forEachIndexed { index, item ->

            builder.appendLine(
                "Calculation #${index + 1}"
            )

            builder.appendLine(
                "ID: ${item.id}"
            )

            builder.appendLine(
                "Type: ${item.calculationType}"
            )

            builder.appendLine(
                "Standard: ${item.standard.displayName}"
            )

            builder.appendLine(
                "Summary: ${item.summary}"
            )

            builder.appendLine(
                "Time: ${item.timestampMillis}"
            )

            builder.appendLine()
        }

        builder.appendLine(
            "=================================================="
        )

        return builder.toString()
    }

    private fun createId(): String {

        return buildString {

            append(
                System.currentTimeMillis()
            )

            append("-")

            append(
                history.size + 1
            )
        }
    }
}

/**
 * ================================================================
 * APPLICATION CALCULATION HISTORY
 * ================================================================
 */

object CalculationHistory {

    private val storage =
        CalculationStorage()

    fun add(
        item: CalculationHistoryItem
    ) {
        storage.add(item)
    }

    fun save(
        calculationType: String,
        standard: Standard,
        summary: String,
        result: EngineeringCalculationPackage
    ): CalculationHistoryItem =
        storage.save(
            calculationType = calculationType,
            standard = standard,
            summary = summary,
            result = result
        )

    fun saveToActiveProject(
        calculationType: String,
        standard: Standard,
        summary: String,
        result: EngineeringCalculationPackage
    ): CalculationHistoryItem =
        storage.saveToActiveProject(
            calculationType = calculationType,
            standard = standard,
            summary = summary,
            result = result
        )

    fun getAll(): List<CalculationHistoryItem> =
        storage.getAll()

    fun getById(
        id: String
    ): CalculationHistoryItem? =
        storage.getById(id)

    fun findByType(
        calculationType: String
    ): List<CalculationHistoryItem> =
        storage.findByType(
            calculationType
        )

    fun findByStandard(
        standard: Standard
    ): List<CalculationHistoryItem> =
        storage.findByStandard(
            standard
        )

    fun delete(
        id: String
    ): Boolean =
        storage.delete(id)

    fun clear() {
        storage.clear()
    }

    fun size(): Int =
        storage.size()

    fun isEmpty(): Boolean =
        storage.isEmpty()

    fun getActiveProject(): DesignProject? =
        storage.getActiveProject()

    fun setActiveProject(
        projectId: String
    ): Boolean =
        storage.setActiveProject(
            projectId
        )

    fun exportSummary(): String =
        storage.exportSummary()
}
