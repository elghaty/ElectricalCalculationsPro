package com.electrical.calculationspro.data.catalog

object CatalogSelector {

    fun selectBreaker(
        designCurrentA: Double,
        shortCircuitKA: Double = 0.0,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<BreakerCatalogItem> {

        if (designCurrentA <= 0.0) {
            return invalid(
                "Breaker design current must be greater than zero."
            )
        }

        if (shortCircuitKA < 0.0) {
            return invalid(
                "Required short-circuit current cannot be negative."
            )
        }

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

        if (candidates.isEmpty()) {
            return invalid(
                if (shortCircuitKA > 0.0) {
                    "No catalog breaker has verified breaking capacity sufficient for the required fault current."
                } else {
                    "No catalog breaker satisfies the required rated current."
                }
            )
        }

        val selected = candidates.first()

        return EquipmentSelectionResult(
            selected = selected,
            alternatives = candidates.drop(1),
            valid = true,
            message =
                if (selected.breakingCapacityKA != null) {
                    "Catalog breaker selected with available voltage-specific breaking-capacity data."
                } else {
                    "Catalog breaker rating found. Breaking capacity requires verification before final design."
                }
        )
    }

    fun selectCable(
        requiredSectionMm2: Double,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<CableCatalogItem> {

        if (requiredSectionMm2 <= 0.0) {
            return invalid(
                "Required cable section must be greater than zero."
            )
        }

        val candidates =
            EquipmentCatalog
                .selectCable(
                    currentA = 0.0,
                    sectionMm2 = requiredSectionMm2,
                    manufacturer = manufacturer
                )
                .sortedBy {
                    it.sectionMm2
                }

        if (candidates.isEmpty()) {
            return invalid(
                "No catalog cable exists for the requested section."
            )
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = true,
            message =
                "Catalog cable reference found. Final ampacity, installation correction factors and manufacturer electrical data require verification."
        )
    }

    fun selectTransformer(
        requiredKva: Double
    ): EquipmentSelectionResult<TransformerCatalogItem> {

        if (requiredKva <= 0.0) {
            return invalid(
                "Required transformer rating must be greater than zero."
            )
        }

        val candidates =
            EquipmentCatalog
                .selectTransformer(requiredKva)
                .sortedBy {
                    it.ratedPowerKva
                }

        if (candidates.isEmpty()) {
            return invalid(
                "No catalog transformer satisfies the required rating."
            )
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = true,
            message =
                "Transformer rating found. Manufacturer impedance, vector group and complete technical data require verification."
        )
    }

    fun selectGenerator(
        requiredKva: Double
    ): EquipmentSelectionResult<GeneratorCatalogItem> {

        if (requiredKva <= 0.0) {
            return invalid(
                "Required generator rating must be greater than zero."
            )
        }

        val candidates =
            EquipmentCatalog
                .selectGenerator(requiredKva)
                .sortedBy {
                    it.ratedPowerKva
                }

        if (candidates.isEmpty()) {
            return invalid(
                "No catalog generator satisfies the required rating."
            )
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = true,
            message =
                "Generator rating found. Manufacturer alternator, transient and short-circuit data require verification."
        )
    }

    fun selectBusbar(
        currentA: Double
    ): EquipmentSelectionResult<BusbarCatalogItem> {

        if (currentA <= 0.0) {
            return invalid(
                "Required busbar current must be greater than zero."
            )
        }

        val candidates =
            EquipmentCatalog
                .selectBusbar(currentA)
                .sortedBy {
                    it.ratedCurrentA
                }

        if (candidates.isEmpty()) {
            return invalid(
                "No catalog busbar satisfies the required current."
            )
        }

        val selected = candidates.first()

        return EquipmentSelectionResult(
            selected = selected,
            alternatives = candidates.drop(1),
            valid = true,
            message =
                if (selected.shortCircuitKA != null) {
                    "Busbar rating found with available short-circuit withstand data."
                } else {
                    "Busbar rating found. Short-circuit withstand requires manufacturer verification."
                }
        )
    }

    fun selectContactor(
        motorCurrentA: Double
    ): EquipmentSelectionResult<ContactorCatalogItem> {

        if (motorCurrentA <= 0.0) {
            return invalid(
                "Motor current must be greater than zero."
            )
        }

        val candidates =
            EquipmentCatalog
                .selectContactor(motorCurrentA)
                .sortedBy {
                    it.ratedCurrentA
                }

        if (candidates.isEmpty()) {
            return invalid(
                "No catalog contactor satisfies the required motor current."
            )
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = true,
            message =
                "Contactor rating found. Final AC-3 coordination and manufacturer data require verification."
        )
    }

    fun selectPanel(
        currentA: Double
    ): EquipmentSelectionResult<PanelCatalogItem> {

        if (currentA <= 0.0) {
            return invalid(
                "Required panel current must be greater than zero."
            )
        }

        val candidates =
            EquipmentCatalog
                .selectPanel(currentA)
                .sortedBy {
                    it.ratedCurrentA
                }

        if (candidates.isEmpty()) {
            return invalid(
                "No catalog panel satisfies the required current."
            )
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = true,
            message =
                "Panel rating found. Final enclosure, IP, short-circuit withstand and assembly verification are required."
        )
    }

    private fun <T> invalid(
        message: String
    ): EquipmentSelectionResult<T> =
        EquipmentSelectionResult(
            selected = null,
            alternatives = emptyList(),
            valid = false,
            message = message
        )
}
