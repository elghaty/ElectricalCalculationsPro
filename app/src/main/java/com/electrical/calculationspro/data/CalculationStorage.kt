package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL ENGINEERING CORE
 * Calculation Storage
 * ================================================================
 *
 * Local in-memory calculation history.
 *
 * This class intentionally contains NO Compose/UI code.
 *
 * A persistent Room implementation can replace this repository later
 * without changing the calculation engines or the UI facade.
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

        val item = CalculationHistoryItem(
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
     * Return all calculations.
     *
     * Newest calculation first.
     */
    fun getAll(): List<CalculationHistoryItem> =
        history
            .sortedByDescending {
                it.timestampMillis
            }
            .toList()

    /**
     * Find a calculation by ID.
     */
    fun getById(
        id: String
    ): CalculationHistoryItem? =
        history.firstOrNull {
            it.id == id
        }

    /**
     * Find calculations by calculation type.
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
     * Find calculations using a particular standard.
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
     * Delete all history.
     */
    fun clear() {
        history.clear()
    }

    /**
     * Number of stored calculations.
     */
    fun size(): Int =
        history.size

    /**
     * Whether storage contains no calculations.
     */
    fun isEmpty(): Boolean =
        history.isEmpty()

    /**
     * Export a simple text summary.
     */
    fun exportSummary(): String {

        if (history.isEmpty()) {
            return "No calculation history."
        }

        val builder = StringBuilder()

        getAll().forEachIndexed { index, item ->

            builder.appendLine(
                "=================================================="
            )

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
            append(System.currentTimeMillis())
            append("-")
            append(history.size + 1)
        }
    }
}

/**
 * Shared application-level calculation storage.
 *
 * The object is intentionally separate from ElectricalCalculations.
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
    ): CalculationHistoryItem {

        return storage.save(
            calculationType = calculationType,
            standard = standard,
            summary = summary,
            result = result
        )
    }

    fun getAll(): List<CalculationHistoryItem> =
        storage.getAll()

    fun getById(
        id: String
    ): CalculationHistoryItem? =
        storage.getById(id)

    fun delete(
        id: String
    ): Boolean =
        storage.delete(id)

    fun clear() {
        storage.clear()
    }

    fun size(): Int =
        storage.size()

    fun exportSummary(): String =
        storage.exportSummary()
}
