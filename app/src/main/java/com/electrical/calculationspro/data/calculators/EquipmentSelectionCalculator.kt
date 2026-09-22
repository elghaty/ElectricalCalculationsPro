package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.catalog.BreakerCatalogItem
import com.electrical.calculationspro.data.catalog.BusbarCatalogItem
import com.electrical.calculationspro.data.catalog.CableCatalogItem
import com.electrical.calculationspro.data.catalog.CatalogSelector
import com.electrical.calculationspro.data.catalog.ContactorCatalogItem
import com.electrical.calculationspro.data.catalog.EquipmentSelectionResult
import com.electrical.calculationspro.data.catalog.GeneratorCatalogItem
import com.electrical.calculationspro.data.catalog.Manufacturer
import com.electrical.calculationspro.data.catalog.PanelCatalogItem
import com.electrical.calculationspro.data.catalog.TransformerCatalogItem

/**
 * Engineering-to-catalog adapter.
 *
 * This layer never invents manufacturer data.
 * A catalog result is considered valid only when the available
 * catalog dataset actually satisfies the requested requirements.
 */
object EquipmentSelectionCalculator {

    data class EquipmentCatalogResult<T>(
        val selected: T?,
        val alternatives: List<T>,
        val valid: Boolean,
        val message: String,
        val standard: Standard = Standard.IEC
    )

    // ------------------------------------------------------------------------
    // BREAKER
    // ------------------------------------------------------------------------

    fun selectBreaker(
        ratedCurrentA: Double,
        breakingCapacityKA: Double = 0.0,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BreakerCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                "Breaker rated current must be greater than zero.",
                standard
            )
        }

        if (breakingCapacityKA < 0.0) {
            return invalid(
                "Required breaking capacity cannot be negative.",
                standard
            )
        }

        val result = CatalogSelector.selectBreaker(
            designCurrentA = ratedCurrentA,
            shortCircuitKA = breakingCapacityKA,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    fun selectBreaker(
        ratedCurrentA: Double,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BreakerCatalogItem> =
        selectBreaker(
            ratedCurrentA = ratedCurrentA,
            breakingCapacityKA = 0.0,
            manufacturer = manufacturer,
            standard = standard
        )

    // ------------------------------------------------------------------------
    // CABLE
    // ------------------------------------------------------------------------

    fun selectCable(
        sectionMm2: Double,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<CableCatalogItem> {

        if (sectionMm2 <= 0.0) {
            return invalid(
                "Required cable section must be greater than zero.",
                standard
            )
        }

        val result = CatalogSelector.selectCable(
            requiredSectionMm2 = sectionMm2,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    fun selectCable(
        sectionMm2: Double,
        material: String,
        insulation: String,
        cores: Int = 1,
        voltageV: Int = 400,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<CableCatalogItem> {

        if (sectionMm2 <= 0.0) {
            return invalid(
                "Required cable section must be greater than zero.",
                standard
            )
        }

        if (material.isBlank()) {
            return invalid(
                "Cable conductor material is required.",
                standard
            )
        }

        if (insulation.isBlank()) {
            return invalid(
                "Cable insulation is required.",
                standard
            )
        }

        if (cores <= 0) {
            return invalid(
                "Cable core count must be greater than zero.",
                standard
            )
        }

        if (voltageV <= 0) {
            return invalid(
                "Cable voltage must be greater than zero.",
                standard
            )
        }

        val result = CatalogSelector.selectCable(
            requiredSectionMm2 = sectionMm2,
            manufacturer = manufacturer
        )

        val candidates =
            (listOfNotNull(result.selected) + result.alternatives)
                .asSequence()
                .filter {
                    it.conductorMaterial.equals(
                        material.trim(),
                        ignoreCase = true
                    )
                }
                .filter {
                    it.insulation.equals(
                        insulation.trim(),
                        ignoreCase = true
                    )
                }
                .filter {
                    it.cores == cores
                }
                .filter {
                    it.voltageClassV >= voltageV
                }
                .sortedBy {
                    it.sectionMm2
                }
                .toList()

        return EquipmentCatalogResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message =
                if (candidates.isNotEmpty()) {
                    "Catalog cable satisfies the requested material, insulation, core count and voltage requirements."
                } else {
                    "No catalog cable satisfies all requested engineering requirements."
                },
            standard = standard
        )
    }

    // ------------------------------------------------------------------------
    // TRANSFORMER
    // ------------------------------------------------------------------------

    fun selectTransformer(
        requiredKva: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<TransformerCatalogItem> {

        if (requiredKva <= 0.0) {
            return invalid(
                "Required transformer rating must be greater than zero.",
                standard
            )
        }

        return CatalogSelector
            .selectTransformer(requiredKva)
            .toCoreResult(standard)
    }

    // ------------------------------------------------------------------------
    // GENERATOR
    // ------------------------------------------------------------------------

    fun selectGenerator(
        requiredKva: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<GeneratorCatalogItem> {

        if (requiredKva <= 0.0) {
            return invalid(
                "Required generator rating must be greater than zero.",
                standard
            )
        }

        return CatalogSelector
            .selectGenerator(requiredKva)
            .toCoreResult(standard)
    }

    // ------------------------------------------------------------------------
    // BUSBAR
    // ------------------------------------------------------------------------

    fun selectBusbar(
        ratedCurrentA: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BusbarCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                "Required busbar current must be greater than zero.",
                standard
            )
        }

        return CatalogSelector
            .selectBusbar(ratedCurrentA)
            .toCoreResult(standard)
    }

    // ------------------------------------------------------------------------
    // CONTACTOR
    // ------------------------------------------------------------------------

    fun selectContactor(
        motorCurrentA: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<ContactorCatalogItem> {

        if (motorCurrentA <= 0.0) {
            return invalid(
                "Motor current must be greater than zero.",
                standard
            )
        }

        return CatalogSelector
            .selectContactor(motorCurrentA)
            .toCoreResult(standard)
    }

    // ------------------------------------------------------------------------
    // PANEL
    // ------------------------------------------------------------------------

    fun selectPanel(
        ratedCurrentA: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<PanelCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                "Panel rated current must be greater than zero.",
                standard
            )
        }

        return CatalogSelector
            .selectPanel(ratedCurrentA)
            .toCoreResult(standard)
    }

    // ------------------------------------------------------------------------
    // RESULT ADAPTER
    // ------------------------------------------------------------------------

    private fun <T> EquipmentSelectionResult<T>.toCoreResult(
        standard: Standard
    ): EquipmentCatalogResult<T> {

        return EquipmentCatalogResult(
            selected = selected,
            alternatives = alternatives,
            valid = valid,
            message = message,
            standard = standard
        )
    }

    // ------------------------------------------------------------------------
    // INVALID
    // ------------------------------------------------------------------------

    private fun <T> invalid(
        message: String,
        standard: Standard
    ): EquipmentCatalogResult<T> {

        return EquipmentCatalogResult(
            selected = null,
            alternatives = emptyList(),
            valid = false,
            message = message,
            standard = standard
        )
    }
}
