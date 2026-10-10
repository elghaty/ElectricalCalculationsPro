
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
        if (!designCurrentA.isFinite() || designCurrentA <= 0.0) {
            return invalid("Breaker design current must be finite and greater than zero.")
        }
        if (!shortCircuitKA.isFinite() || shortCircuitKA < 0.0) {
            return invalid("Required short-circuit current must be finite and cannot be negative.")
        }

        val candidates = EquipmentCatalog.selectBreaker(
            currentA = designCurrentA,
            shortCircuitKA = shortCircuitKA
        ).filter { manufacturer == null || it.manufacturer == manufacturer }

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
        val breakingCapacityVerified =
            shortCircuitKA > 0.0 &&
                selected.breakingCapacityKA != null &&
                selected.breakingCapacityKA >= shortCircuitKA &&
                selected.source.status == ProductFamilyStatus.VERIFIED_PRODUCT &&
                selected.source.officialUrl.isNotBlank()

        return EquipmentSelectionResult(
            selected = selected,
            alternatives = candidates.drop(1),
            valid = breakingCapacityVerified,
            message = if (breakingCapacityVerified) {
                "Breaker current and voltage-specific breaking capacity are supported by an exact manufacturer product source."
            } else {
                "Preliminary breaker candidate only. Final selection is NOT VERIFIED until prospective fault current and exact manufacturer breaking-capacity data are confirmed."
            }
        )
    }

    fun selectCable(
        requiredSectionMm2: Double,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<CableCatalogItem> {
        if (!requiredSectionMm2.isFinite() || requiredSectionMm2 <= 0.0) {
            return invalid("Required cable section must be finite and greater than zero.")
        }

        val candidates = EquipmentCatalog.selectCable(
            currentA = 0.0,
            sectionMm2 = requiredSectionMm2,
            manufacturer = manufacturer
        ).sortedBy { it.sectionMm2 }

        if (candidates.isEmpty()) {
            return invalid("No catalog cable exists for the requested section.")
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = false,
            message = "Preliminary cable reference only. Final cable selection is NOT VERIFIED until ampacity, installation method, correction factors, voltage drop and exact manufacturer data are checked."
        )
    }

    fun selectTransformer(
        requiredKva: Double
    ): EquipmentSelectionResult<TransformerCatalogItem> {
        if (!requiredKva.isFinite() || requiredKva <= 0.0) {
            return invalid("Required transformer rating must be finite and greater than zero.")
        }

        val candidates = EquipmentCatalog.selectTransformer(requiredKva)
            .sortedBy { it.ratedPowerKva }

        if (candidates.isEmpty()) {
            return invalid("No catalog transformer satisfies the required rating.")
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = false,
            message = "Preliminary transformer rating only. Final selection is NOT VERIFIED until an exact product, impedance, vector group, losses and manufacturer data are confirmed."
        )
    }

    fun selectGenerator(
        requiredKva: Double
    ): EquipmentSelectionResult<GeneratorCatalogItem> {
        if (!requiredKva.isFinite() || requiredKva <= 0.0) {
            return invalid("Required generator rating must be finite and greater than zero.")
        }

        val candidates = EquipmentCatalog.selectGenerator(requiredKva)
            .sortedBy { it.ratedPowerKva }

        if (candidates.isEmpty()) {
            return invalid("No catalog generator satisfies the required rating.")
        }

        return EquipmentSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = false,
            message = "Preliminary generator rating only. Final selection is NOT VERIFIED until exact alternator, transient response, fault contribution and manufacturer data are confirmed."
        )
    }

    fun selectBusbar(
        currentA: Double
    ): EquipmentSelectionResult<BusbarCatalogItem> {
        if (!currentA.isFinite() || currentA <= 0.0) {
            return invalid("Required busbar current must be finite and greater than zero.")
        }

        val candidates = EquipmentCatalog.selectBusbar(currentA)
            .sortedBy { it.ratedCurrentA }

        if (candidates.isEmpty()) {
            return invalid("No catalog busbar satisfies the required current.")
        }

        val selected = candidates.first()
        val busbarVerified =
            selected.shortCircuitKA != null &&
                selected.source.status == ProductFamilyStatus.VERIFIED_PRODUCT &&
                selected.source.officialUrl.isNotBlank()

        return EquipmentSelectionResult(
            selected = selected,
            alternatives = candidates.drop(1),
            valid = busbarVerified,
            message = if (busbarVerified) {
                "Busbar current and short-circuit withstand data have an exact manufacturer product source."
            } else {
                "Preliminary busbar candidate only. Exact assembly rating and short-circuit withstand are NOT VERIFIED."
            }
        )
    }

    fun selectContactor(
        motorCurrentA: Double
    ): EquipmentSelectionResult<ContactorCatalogItem> {
        if (!motorCurrentA.isFinite() || motorCurrentA <= 0.0) {
            return invalid("Motor current must be finite and greater than zero.")
        }

        val candidates = EquipmentCatalog.selectContactor(motorCurrentA)
            .sortedBy { it.ratedCurrentA }

        if (candidates.isEmpty()) {
            return invalid("No catalog contactor satisfies the required motor current.")
        }

        val selected = candidates.first()
        val contactorVerified =
            selected.source.status == ProductFamilyStatus.VERIFIED_PRODUCT &&
                selected.source.officialUrl.isNotBlank() &&
                selected.utilizationCategory.equals("AC-3", ignoreCase = true)

        return EquipmentSelectionResult(
            selected = selected,
            alternatives = candidates.drop(1),
            valid = contactorVerified,
            message = if (contactorVerified) {
                "Exact contactor product source found; confirm motor duty and coordination before approval."
            } else {
                "Preliminary contactor candidate only. Exact commercial reference and AC-3 motor-duty coordination are NOT VERIFIED."
            }
        )
    }

    fun selectPanel(
        currentA: Double
    ): EquipmentSelectionResult<PanelCatalogItem> {
        if (!currentA.isFinite() || currentA <= 0.0) {
            return invalid("Required panel current must be finite and greater than zero.")
        }

        val candidates = EquipmentCatalog.selectPanel(currentA)
            .sortedBy { it.ratedCurrentA }

        if (candidates.isEmpty()) {
            return invalid("No catalog panel satisfies the required current.")
        }

        val selected = candidates.first()
        val panelVerified =
            selected.source.status == ProductFamilyStatus.VERIFIED_PRODUCT &&
                selected.source.officialUrl.isNotBlank()

        return EquipmentSelectionResult(
            selected = selected,
            alternatives = candidates.drop(1),
            valid = panelVerified,
            message = if (panelVerified) {
                "Exact panel product source found; complete assembly verification remains required."
            } else {
                "Preliminary panel candidate only. Enclosure, IP, temperature rise, short-circuit withstand and IEC 61439 assembly verification are NOT VERIFIED."
            }
        )
    }

    /**
     * A candidate passes this method only when the complete technical
     * requirement and exact-product source checks pass.
     */
    fun selectCompliantCable(
        requirement: TechnicalRequirement.Cable,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<CableCatalogItem> {
        val candidates = EquipmentCatalog.cables()
            .asSequence()
            .filter { manufacturer == null || it.manufacturer == manufacturer }
            .filter { CodeCompliance.evaluate(requirement, it).compliant }
            .sortedWith(
                compareBy<CableCatalogItem> { it.sectionMm2 }
                    .thenBy { it.manufacturer.name }
                    .thenBy { it.model }
            )
            .toList()

        return selectionResult(
            candidates,
            "Catalog cable satisfies the complete technical requirement and has a verified exact-product source.",
            "No catalog cable satisfies the complete technical requirement with verified exact-product data."
        )
    }

    fun selectCompliantBreaker(
        requirement: TechnicalRequirement.Breaker,
        manufacturer: Manufacturer? = null
    ): EquipmentSelectionResult<BreakerCatalogItem> {
        val candidates = EquipmentCatalog.breakers()
            .asSequence()
            .filter { manufacturer == null || it.manufacturer == manufacturer }
            .filter { CodeCompliance.evaluate(requirement, it).compliant }
            .sortedWith(
                compareBy<BreakerCatalogItem> { it.ratedCurrentA }
                    .thenBy { it.breakingCapacityKA ?: Double.MAX_VALUE }
                    .thenBy { it.manufacturer.name }
            )
            .toList()

        return selectionResult(
            candidates,
            "Catalog breaker satisfies the complete technical requirement and has a verified exact-product source.",
            "No catalog breaker satisfies the complete technical requirement with verified exact-product and fault-capacity data."
        )
    }

    fun selectCompliantTransformer(
        requirement: TechnicalRequirement.Transformer
    ): EquipmentSelectionResult<TransformerCatalogItem> {
        val candidates = EquipmentCatalog.transformers()
            .filter { CodeCompliance.evaluate(requirement, it).compliant }
            .sortedBy { it.ratedPowerKva }

        return selectionResult(
            candidates,
            "Transformer satisfies the technical requirement and has verified exact-product data.",
            "No transformer record satisfies the complete technical requirement with verified product and impedance data."
        )
    }

    fun selectCompliantGenerator(
        requirement: TechnicalRequirement.Generator
    ): EquipmentSelectionResult<GeneratorCatalogItem> {
        val candidates = EquipmentCatalog.generators()
            .filter { CodeCompliance.evaluate(requirement, it).compliant }
            .sortedBy { it.ratedPowerKva }

        return selectionResult(
            candidates,
            "Generator satisfies the technical requirement and has verified exact-product data.",
            "No generator record satisfies the complete technical requirement with verified product data."
        )
    }

    fun selectCompliantBusbar(
        requirement: TechnicalRequirement.Busbar
    ): EquipmentSelectionResult<BusbarCatalogItem> {
        val candidates = EquipmentCatalog.busbars()
            .filter { CodeCompliance.evaluate(requirement, it).compliant }
            .sortedBy { it.ratedCurrentA }

        return selectionResult(
            candidates,
            "Busbar satisfies the technical requirement and has verified exact-product data.",
            "No busbar record satisfies the complete technical requirement with verified assembly and short-circuit data."
        )
    }

    fun selectCompliantContactor(
        requirement: TechnicalRequirement.Contactor
    ): EquipmentSelectionResult<ContactorCatalogItem> {
        val candidates = EquipmentCatalog.contactors()
            .filter { CodeCompliance.evaluate(requirement, it).compliant }
            .sortedBy { it.ratedCurrentA }

        return selectionResult(
            candidates,
            "Contactor satisfies the technical requirement and has verified exact-product data.",
            "No contactor record satisfies the complete technical requirement with verified product and utilization-category data."
        )
    }

    fun selectCompliantPanel(
        requirement: TechnicalRequirement.Panel
    ): EquipmentSelectionResult<PanelCatalogItem> {
        val candidates = EquipmentCatalog.panels()
            .filter { CodeCompliance.evaluate(requirement, it).compliant }
            .sortedBy { it.ratedCurrentA }

        return selectionResult(
            candidates,
            "Panel satisfies the technical requirement and has verified exact-product data.",
            "No panel record satisfies the complete technical requirement with verified assembly data."
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
