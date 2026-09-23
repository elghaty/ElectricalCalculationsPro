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
                .filter { item ->
                    manufacturer == null ||
                        item.manufacturer == manufacturer
                }

        if (candidates.isEmpty()) {
            return invalid(
                if (shortCircuitKA > 0.0) {
                    "No catalog breaker has sufficient rated current and verified breaking capacity for the required fault current."
                } else {
                    "No catalog breaker satisfies the required rated current."
                }
            )
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = true,
            message =
                if (shortCircuitKA > 0.0) {
                    "Catalog breaker satisfies the requested current and available breaking-capacity data."
                } else {
                    "Catalog breaker rating found. Breaking capacity still requires verification against the prospective fault current."
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
                .sortedBy { item ->
                    item.sectionMm2
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
                "Catalog cable family found. Electrical ampacity and manufacturer R/X data must be verified for the final design."
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
                .sortedBy { item ->
                    item.ratedPowerKva
                }

        return if (candidates.isEmpty()) {
            invalid(
                "No catalog transformer satisfies the required rating."
            )
        } else {
            EquipmentSelectionResult(
                selected = candidates.first(),
                alternatives = candidates.drop(1),
                valid = true,
                message =
                    "Catalog transformer rating found. Final impedance, voltage, vector group and manufacturer data require verification."
            )
        }
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
                .sortedBy { item ->
                    item.ratedPowerKva
                }

        return if (candidates.isEmpty()) {
            invalid(
                "No catalog generator satisfies the required rating."
            )
        } else {
            EquipmentSelectionResult(
                selected = candidates.first(),
                alternatives = candidates.drop(1),
                valid = true,
                message =
                    "Catalog generator rating found. Final alternator short-circuit and transient data require manufacturer verification."
            )
        }
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
                .sortedBy { item ->
                    item.ratedCurrentA
                }

        return if (candidates.isEmpty()) {
            invalid(
                "No catalog busbar satisfies the required current."
            )
        } else {
            EquipmentSelectionResult(
                selected = candidates.first(),
                alternatives = candidates.drop(1),
                valid = true,
                message =
                    if (candidates.first().shortCircuitKA != null) {
                        "Catalog busbar found with available short-circuit data."
                    } else {
                        "Catalog busbar rating found. Short-circuit withstand requires manufacturer verification."
                    }
            )
        }
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
                .sortedBy { item ->
                    item.ratedCurrentA
                }

        return if (candidates.isEmpty()) {
            invalid(
                "No catalog contactor satisfies the required motor current."
            )
        } else {
            EquipmentSelectionResult(
                selected = candidates.first(),
                alternatives = candidates.drop(1),
                valid = true,
                message =
                    "Catalog contactor found. Final utilization category and motor-duty coordination require verification."
            )
        }
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
                .sortedBy { item ->
                    item.ratedCurrentA
                }

        return if (candidates.isEmpty()) {
            invalid(
                "No catalog panel satisfies the required current."
            )
        } else {
            EquipmentSelectionResult(
                selected = candidates.first(),
                alternatives = candidates.drop(1),
                valid = true,
                message =
                    "Catalog panel rating found. Final enclosure, IP, short-circuit withstand and assembly verification are required."
            )
        }
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
