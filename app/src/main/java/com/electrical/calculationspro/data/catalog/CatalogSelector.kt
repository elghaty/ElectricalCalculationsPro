package com.electrical.calculationspro.data.catalog

data class EquipmentSelectionResult<T>(
    val selected: T?,
    val alternatives: List<T>,
    val valid: Boolean,
    val message: String
)

object CatalogSelector {

    fun selectBreaker(
        designCurrentA: Double,
        shortCircuitKA: Double = 0.0,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<BreakerCatalogItem> {

        val candidates =
            EquipmentCatalog
                .selectBreaker(
                    currentA = designCurrentA,
                    shortCircuitKA = shortCircuitKA
                )
                .filter {
                    manufacturer == null ||
                        it.manufacturer == manufacturer
                }

        return EquipmentSelectionResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message =
                if (candidates.isNotEmpty()) {
                    "Suitable breaker family found."
                } else {
                    "No catalog breaker satisfies the requested current and available breaking-capacity data."
                }
        )
    }

    fun selectCable(
        requiredSectionMm2: Double,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<CableCatalogItem> {

        val candidates =
            EquipmentCatalog.selectCable(
                currentA = 0.0,
                sectionMm2 = requiredSectionMm2,
                manufacturer = manufacturer
            )

        return EquipmentSelectionResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message =
                if (candidates.isNotEmpty()) {
                    "Suitable cable catalog family found."
                } else {
                    "No cable catalog item found."
                }
        )
    }

    fun selectTransformer(
        requiredKva: Double
    ): EquipmentSelectionResult<TransformerCatalogItem> {

        val candidates =
            EquipmentCatalog.selectTransformer(
                requiredKva
            )

        return EquipmentSelectionResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message =
                if (candidates.isNotEmpty()) {
                    "Suitable transformer rating found."
                } else {
                    "No transformer rating found."
                }
        )
    }

    fun selectGenerator(
        requiredKva: Double
    ): EquipmentSelectionResult<GeneratorCatalogItem> {

        val candidates =
            EquipmentCatalog.selectGenerator(
                requiredKva
            )

        return EquipmentSelectionResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message =
                if (candidates.isNotEmpty()) {
                    "Suitable generator rating found."
                } else {
                    "No generator rating found."
                }
        )
    }

    fun selectBusbar(
        currentA: Double
    ): EquipmentSelectionResult<BusbarCatalogItem> {

        val candidates =
            EquipmentCatalog.selectBusbar(
                currentA
            )

        return EquipmentSelectionResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message =
                if (candidates.isNotEmpty()) {
                    "Suitable busbar rating found."
                } else {
                    "No busbar rating found."
                }
        )
    }

    fun selectContactor(
        motorCurrentA: Double
    ): EquipmentSelectionResult<ContactorCatalogItem> {

        val candidates =
            EquipmentCatalog.selectContactor(
                motorCurrentA
            )

        return EquipmentSelectionResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message =
                if (candidates.isNotEmpty()) {
                    "Suitable contactor found."
                } else {
                    "No suitable contactor found."
                }
        )
    }

    fun selectPanel(
        currentA: Double
    ): EquipmentSelectionResult<PanelCatalogItem> {

        val candidates =
            EquipmentCatalog.selectPanel(
                currentA
            )

        return EquipmentSelectionResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message =
                if (candidates.isNotEmpty()) {
                    "Suitable panel rating found."
                } else {
                    "No suitable panel rating found."
                }
        )
    }
}
