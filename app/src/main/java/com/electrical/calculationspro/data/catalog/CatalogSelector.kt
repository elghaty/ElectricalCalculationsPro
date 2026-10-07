package com.electrical.calculationspro.data.catalog

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard

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

    /**
     * Code-compliant cable selection.
     *
     * The calculation engine supplies the requirement.
     * The catalog supplies candidate products.
     * CodeCompliance performs the compatibility check.
     */
    fun selectCompliantCable(
        requirement: TechnicalRequirement.Cable,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<CableCatalogItem> {

        val candidates =
            EquipmentCatalog
                .cables()
                .asSequence()
                .filter {
                    manufacturer == null ||
                        it.manufacturer == manufacturer
                }
                .filter {
                    CodeCompliance
                        .evaluate(requirement, it)
                        .compliant
                }
                .sortedWith(
                    compareBy<CableCatalogItem> {
                        it.sectionMm2
                    }.thenBy {
                        it.manufacturer.name
                    }.thenBy {
                        it.model
                    }
                )
                .toList()

        return selectionResult(
            candidates = candidates,
            successMessage =
                "Catalog cable satisfies the complete technical requirement and selected standard.",
            failureMessage =
                "No catalog cable satisfies the complete technical requirement and selected standard."
        )
    }

    /**
     * Code-compliant breaker selection.
     */
    fun selectCompliantBreaker(
        requirement: TechnicalRequirement.Breaker,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<BreakerCatalogItem> {

        val candidates =
            EquipmentCatalog
                .breakers()
                .asSequence()
                .filter {
                    manufacturer == null ||
                        it.manufacturer == manufacturer
                }
                .filter {
                    CodeCompliance
                        .evaluate(requirement, it)
                        .compliant
                }
                .sortedWith(
                    compareBy<BreakerCatalogItem> {
                        it.ratedCurrentA
                    }.thenBy {
                        it.breakingCapacityKA
                            ?: Double.MAX_VALUE
                    }.thenBy {
                        it.manufacturer.name
                    }
                )
                .toList()

        return selectionResult(
            candidates = candidates,
            successMessage =
                "Catalog breaker satisfies the complete technical requirement and selected standard.",
            failureMessage =
                "No catalog breaker satisfies the complete technical requirement and selected standard."
        )
    }

    /**
     * Code-compliant transformer selection.
     */
    fun selectCompliantTransformer(
        requirement: TechnicalRequirement.Transformer
    ): EquipmentSelectionResult<TransformerCatalogItem> {

        val candidates =
            EquipmentCatalog
                .transformers()
                .filter {
                    CodeCompliance
                        .evaluate(requirement, it)
                        .compliant
                }
                .sortedBy {
                    it.ratedPowerKva
                }

        return selectionResult(
            candidates = candidates,
            successMessage =
                "Transformer rating satisfies the technical requirement.",
            failureMessage =
                "No transformer catalog record satisfies the complete technical requirement."
        )
    }

    /**
     * Code-compliant generator selection.
     */
    fun selectCompliantGenerator(
        requirement: TechnicalRequirement.Generator
    ): EquipmentSelectionResult<GeneratorCatalogItem> {

        val candidates =
            EquipmentCatalog
                .generators()
                .filter {
                    CodeCompliance
                        .evaluate(requirement, it)
                        .compliant
                }
                .sortedBy {
                    it.ratedPowerKva
                }

        return selectionResult(
            candidates = candidates,
            successMessage =
                "Generator rating satisfies the technical requirement.",
            failureMessage =
                "No generator catalog record satisfies the complete technical requirement."
        )
    }

    /**
     * Code-compliant busbar selection.
     */
    fun selectCompliantBusbar(
        requirement: TechnicalRequirement.Busbar
    ): EquipmentSelectionResult<BusbarCatalogItem> {

        val candidates =
            EquipmentCatalog
                .busbars()
                .filter {
                    CodeCompliance
                        .evaluate(requirement, it)
                        .compliant
                }
                .sortedBy {
                    it.ratedCurrentA
                }

        return selectionResult(
            candidates = candidates,
            successMessage =
                "Busbar catalog record satisfies the technical requirement.",
            failureMessage =
                "No busbar catalog record satisfies the complete technical requirement."
        )
    }

    /**
     * Code-compliant contactor selection.
     */
    fun selectCompliantContactor(
        requirement: TechnicalRequirement.Contactor
    ): EquipmentSelectionResult<ContactorCatalogItem> {

        val candidates =
            EquipmentCatalog
                .contactors()
                .filter {
                    CodeCompliance
                        .evaluate(requirement, it)
                        .compliant
                }
                .sortedBy {
                    it.ratedCurrentA
                }

        return selectionResult(
            candidates = candidates,
            successMessage =
                "Contactor catalog record satisfies the technical requirement.",
            failureMessage =
                "No contactor catalog record satisfies the complete technical requirement."
        )
    }

    /**
     * Code-compliant panel selection.
     */
    fun selectCompliantPanel(
        requirement: TechnicalRequirement.Panel
    ): EquipmentSelectionResult<PanelCatalogItem> {

        val candidates =
            EquipmentCatalog
                .panels()
                .filter {
                    CodeCompliance
                        .evaluate(requirement, it)
                        .compliant
                }
                .sortedBy {
                    it.ratedCurrentA
                }

        return selectionResult(
            candidates = candidates,
            successMessage =
                "Panel catalog record satisfies the technical requirement.",
            failureMessage =
                "No panel catalog record satisfies the complete technical requirement."
        )
    }

    private fun <T> selectionResult(
        candidates: List<T>,
        successMessage: String,
        failureMessage: String
    ): EquipmentSelectionResult<T> {

        if (candidates.isEmpty()) {
            return invalid(failureMessage)
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = true,
            message = successMessage
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
