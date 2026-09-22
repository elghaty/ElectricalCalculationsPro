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
 * Professional Engineering Core - Equipment Selection Calculator.
 *
 * Architecture:
 *
 * UI
 *   ↓
 * ElectricalCalculations
 *   ↓
 * EquipmentSelectionCalculator
 *   ↓
 * CatalogSelector
 *   ↓
 * EquipmentCatalog
 *
 * This class is the engineering-facing adapter between the
 * Professional Engineering Core and the equipment catalog.
 *
 * IMPORTANT:
 * The calculator does not fabricate manufacturer data.
 * It only selects from the catalog dataset currently available
 * in the application.
 */
object EquipmentSelectionCalculator {

    /**
     * Unified result exposed to the Professional Engineering Core.
     */
    data class EquipmentCatalogResult<T>(
        val selected: T?,
        val alternatives: List<T>,
        val valid: Boolean,
        val message: String,
        val standard: Standard = Standard.IEC
    )

    // ========================================================================
    // BREAKER
    // ========================================================================

    /**
     * Select a breaker using design current and short-circuit current.
     *
     * The catalog selector is responsible for matching the available
     * breaker families and ratings.
     */
    fun selectBreaker(
        ratedCurrentA: Double,
        breakingCapacityKA: Double = 0.0,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BreakerCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                "Breaker design current must be greater than zero.",
                standard
            )
        }

        if (breakingCapacityKA < 0.0) {
            return invalid(
                "Breaker breaking capacity cannot be negative.",
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

    /**
     * Breaker selection using only design current.
     */
    fun selectBreaker(
        ratedCurrentA: Double,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BreakerCatalogItem> {

        return selectBreaker(
            ratedCurrentA = ratedCurrentA,
            breakingCapacityKA = 0.0,
            manufacturer = manufacturer,
            standard = standard
        )
    }

    // ========================================================================
    // CABLE
    // ========================================================================

    /**
     * Select a cable by the minimum engineering section.
     *
     * Manufacturer is optional and is used as a catalog filter.
     */
    fun selectCable(
        sectionMm2: Double,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<CableCatalogItem> {

        if (sectionMm2 <= 0.0) {
            return invalid(
                "Cable section must be greater than zero.",
                standard
            )
        }

        val result = CatalogSelector.selectCable(
            requiredSectionMm2 = sectionMm2,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    /**
     * Cable selection with engineering metadata.
     *
     * Material, insulation, core count and voltage are validated here
     * and retained as engineering requirements. The current catalog
     * selector selects by section/manufacturer because those are the
     * currently exposed catalog-selector parameters.
     *
     * The metadata is deliberately not used to fabricate a product match.
     */
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
                "Cable section must be greater than zero.",
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
                "Cable insulation type is required.",
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

        val filtered = result.filterCableEngineeringRequirements(
            material = material,
            insulation = insulation,
            cores = cores,
            voltageV = voltageV
        )

        return filtered.toCoreResult(standard)
    }

    // ========================================================================
    // TRANSFORMER
    // ========================================================================

    /**
     * Select the next suitable transformer rating.
     */
    fun selectTransformer(
        requiredKva: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<TransformerCatalogItem> {

        if (requiredKva <= 0.0) {
            return invalid(
                "Required transformer power must be greater than zero.",
                standard
            )
        }

        val result = CatalogSelector.selectTransformer(
            requiredKva = requiredKva
        )

        return result.toCoreResult(standard)
    }

    // ========================================================================
    // GENERATOR
    // ========================================================================

    /**
     * Select the next suitable generator rating.
     */
    fun selectGenerator(
        requiredKva: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<GeneratorCatalogItem> {

        if (requiredKva <= 0.0) {
            return invalid(
                "Required generator power must be greater than zero.",
                standard
            )
        }

        val result = CatalogSelector.selectGenerator(
            requiredKva = requiredKva
        )

        return result.toCoreResult(standard)
    }

    // ========================================================================
    // BUSBAR
    // ========================================================================

    /**
     * Select a busbar based on required current.
     */
    fun selectBusbar(
        ratedCurrentA: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BusbarCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                "Busbar rated current must be greater than zero.",
                standard
            )
        }

        val result = CatalogSelector.selectBusbar(
            currentA = ratedCurrentA
        )

        return result.toCoreResult(standard)
    }

    // ========================================================================
    // CONTACTOR
    // ========================================================================

    /**
     * Select a contactor based on motor current.
     */
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

        val result = CatalogSelector.selectContactor(
            motorCurrentA = motorCurrentA
        )

        return result.toCoreResult(standard)
    }

    // ========================================================================
    // PANEL
    // ========================================================================

    /**
     * Select a panel based on required current.
     */
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

        val result = CatalogSelector.selectPanel(
            currentA = ratedCurrentA
        )

        return result.toCoreResult(standard)
    }

    // ========================================================================
    // RESULT ADAPTER
    // ========================================================================

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

    /**
     * Apply engineering cable filters to the catalog result without
     * inventing a match when the available catalog data does not support it.
     */
    private fun EquipmentSelectionResult<CableCatalogItem>
        .filterCableEngineeringRequirements(
            material: String,
            insulation: String,
            cores: Int,
            voltageV: Int
        ): EquipmentSelectionResult<CableCatalogItem> {

        val normalizedMaterial = material.trim().lowercase()
        val normalizedInsulation = insulation.trim().lowercase()

        val candidates = (
            listOfNotNull(selected) + alternatives
            )
            .filter { item ->
                item.conductorMaterial
                    .trim()
                    .lowercase()
                    .contains(normalizedMaterial)
            }
            .filter { item ->
                item.insulation
                    .trim()
                    .lowercase()
                    .contains(normalizedInsulation)
            }
            .filter { item ->
                item.cores == cores
            }
            .filter { item ->
                item.voltageClassV >= voltageV
            }
            .sortedBy { item ->
                item.sectionMm2
            }

        return EquipmentSelectionResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = candidates.isNotEmpty(),
            message = if (candidates.isNotEmpty()) {
                "Suitable cable catalog item found for the requested engineering requirements."
            } else {
                "No catalog cable item satisfies material, insulation, core count and voltage requirements."
            }
        )
    }

    // ========================================================================
    // INVALID RESULT
    // ========================================================================

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
