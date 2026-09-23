package com.electrical.calculationspro.data

import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects

class CalculationStorage {

    private val history =
        mutableListOf<CalculationHistoryItem>()

    fun add(
        item: CalculationHistoryItem
    ) {
        history.removeAll { it.id == item.id }
        history.add(item)
    }

    fun save(
        calculationType: String,
        standard: Standard,
        summary: String,
        result: EngineeringCalculationPackage,
        timestampMillis: Long = System.currentTimeMillis(),
        projectId: String? = DesignProjects.getActive()?.id
    ): CalculationHistoryItem {

        val item =
            CalculationHistoryItem(
                id = createId(),
                calculationType = calculationType,
                timestampMillis = timestampMillis,
                standard = standard,
                summary = summary,
                result = result,
                projectId = projectId
            )

        add(item)

        return item
    }

    fun saveToActiveProject(
        calculationType: String,
        standard: Standard,
        summary: String,
        result: EngineeringCalculationPackage
    ): CalculationHistoryItem =
        save(
            calculationType = calculationType,
            standard = standard,
            summary = summary,
            result = result,
            projectId = DesignProjects.getActive()?.id
        )

    fun getAll(): List<CalculationHistoryItem> =
        history
            .sortedByDescending { it.timestampMillis }
            .toList()

    fun getById(
        id: String
    ): CalculationHistoryItem? =
        history.firstOrNull { it.id == id }

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

    fun findByStandard(
        standard: Standard
    ): List<CalculationHistoryItem> =
        history
            .filter { it.standard == standard }
            .sortedByDescending {
                it.timestampMillis
            }

    fun findByProject(
        projectId: String
    ): List<CalculationHistoryItem> =
        history
            .filter { it.projectId == projectId }
            .sortedByDescending {
                it.timestampMillis
            }

    fun delete(
        id: String
    ): Boolean =
        history.removeAll {
            it.id == id
        }

    fun clear() {
        history.clear()
    }

    fun size(): Int =
        history.size

    fun isEmpty(): Boolean =
        history.isEmpty()

    fun getActiveProject(): DesignProject? =
        DesignProjects.getActive()

    fun setActiveProject(
        projectId: String
    ): Boolean =
        DesignProjects.setActive(projectId)

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

        activeProject?.let {

            builder.appendLine(
                "Project: ${it.projectName}"
            )

            builder.appendLine(
                "Project Number: ${it.projectNumber}"
            )

            builder.appendLine(
                "Client: ${it.clientName}"
            )

            it.electricalStandard?.let { standard ->
                builder.appendLine(
                    "Electrical Standard: ${standard.displayName}"
                )
            }
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
                "Project ID: ${item.projectId ?: "-"}"
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

    private fun createId(): String =
        "${System.currentTimeMillis()}-${history.size + 1}"
}

object CalculationHistory {

    private val storage =
        CalculationStorage()

    fun add(
        item: CalculationHistoryItem
    ) =
        storage.add(item)

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
        storage.findByType(calculationType)

    fun findByStandard(
        standard: Standard
    ): List<CalculationHistoryItem> =
        storage.findByStandard(standard)

    fun findByProject(
        projectId: String
    ): List<CalculationHistoryItem> =
        storage.findByProject(projectId)

    fun delete(
        id: String
    ): Boolean =
        storage.delete(id)

    fun clear() =
        storage.clear()

    fun size(): Int =
        storage.size()

    fun isEmpty(): Boolean =
        storage.isEmpty()

    fun getActiveProject(): DesignProject? =
        storage.getActiveProject()

    fun setActiveProject(
        projectId: String
    ): Boolean =
        storage.setActiveProject(projectId)

    fun exportSummary(): String =
        storage.exportSummary()
}
