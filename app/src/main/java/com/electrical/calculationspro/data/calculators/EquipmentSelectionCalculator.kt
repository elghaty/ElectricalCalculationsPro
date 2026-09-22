package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.catalog.BreakerCatalogItem
import com.electrical.calculationspro.data.catalog.BusbarCatalogItem
import com.electrical.calculationspro.data.catalog.CableCatalogItem
import com.electrical.calculationspro.data.catalog.CatalogSelector
import com.electrical.calculationspro.data.catalog.ContactorCatalogItem
import com.electrical.calculationspro.data.catalog.GeneratorCatalogItem
import com.electrical.calculationspro.data.catalog.PanelCatalogItem
import com.electrical.calculationspro.data.catalog.TransformerCatalogItem
import com.electrical.calculationspro.data.catalog.Manufacturer

/**
 * ================================================================
 * PROFESSIONAL EQUIPMENT SELECTION ENGINE
 * ================================================================
 *
 * Engineering calculations
 *        ↓
 * EquipmentSelectionCalculator
 *        ↓
 * CatalogSelector
 *        ↓
 * Manufacturer Catalog
 *
 * UI must NOT access catalog classes directly.
 * ================================================================
 */

object EquipmentSelectionCalculator {

    fun selectCable(
        requiredSectionMm2: Double,
        manufacturer: Manufacturer? = null
    ): EquipmentCatalogResult<CableCatalogItem> {

        require(requiredSectionMm2 >= 0.0) {
            "Required cable section cannot be negative."
        }

        val result =
            CatalogSelector.selectCable(
                requiredSectionMm2 = requiredSectionMm2,
                manufacturer = manufacturer
            )

        return EquipmentCatalogResult(
            selected = result.selected,
            alternatives = result.alternatives,
            valid = result.valid,
            message = result.message
        )
    }

    fun selectBreaker(
        designCurrentA: Double,
        shortCircuitKA: Double = 0.0,
        manufacturer: Manufacturer? = null
    ): EquipmentCatalogResult<BreakerCatalogItem> {

        require(designCurrentA >= 0.0) {
            "Design current cannot be negative."
        }

        require(shortCircuitKA >= 0.0) {
            "Short-circuit current cannot be negative."
        }

        val result =
            CatalogSelector.selectBreaker(
                designCurrentA = designCurrentA,
                shortCircuitKA = shortCircuitKA,
                manufacturer = manufacturer
            )

        return EquipmentCatalogResult(
            selected = result.selected,
            alternatives = result.alternatives,
            valid = result.valid,
            message = result.message
        )
    }

    fun selectTransformer(
        requiredKva: Double
    ): EquipmentCatalogResult<TransformerCatalogItem> {

        require(requiredKva >= 0.0) {
            "Required transformer capacity cannot be negative."
        }

        val result =
            CatalogSelector.selectTransformer(
                requiredKva = requiredKva
            )

        return EquipmentCatalogResult(
            selected = result.selected,
            alternatives = result.alternatives,
            valid = result.valid,
            message = result.message
        )
    }

    fun selectGenerator(
        requiredKva: Double
    ): EquipmentCatalogResult<GeneratorCatalogItem> {

        require(requiredKva >= 0.0) {
            "Required generator capacity cannot be negative."
        }

        val result =
            CatalogSelector.selectGenerator(
                requiredKva = requiredKva
            )

        return EquipmentCatalogResult(
            selected = result.selected,
            alternatives = result.alternatives,
            valid = result.valid,
            message = result.message
        )
    }

    fun selectBusbar(
        currentA: Double
    ): EquipmentCatalogResult<BusbarCatalogItem> {

        require(currentA >= 0.0) {
            "Busbar current cannot be negative."
        }

        val result =
            CatalogSelector.selectBusbar(
                currentA = currentA
            )

        return EquipmentCatalogResult(
            selected = result.selected,
            alternatives = result.alternatives,
            valid = result.valid,
            message = result.message
        )
    }

    fun selectContactor(
        motorCurrentA: Double
    ): EquipmentCatalogResult<ContactorCatalogItem> {

        require(motorCurrentA >= 0.0) {
            "Motor current cannot be negative."
        }

        val result =
            CatalogSelector.selectContactor(
                motorCurrentA = motorCurrentA
            )

        return EquipmentCatalogResult(
            selected = result.selected,
            alternatives = result.alternatives,
            valid = result.valid,
            message = result.message
        )
    }

    fun selectPanel(
        currentA: Double
    ): EquipmentCatalogResult<PanelCatalogItem> {

        require(currentA >= 0.0) {
            "Panel current cannot be negative."
        }

        val result =
            CatalogSelector.selectPanel(
                currentA = currentA
            )

        return EquipmentCatalogResult(
            selected = result.selected,
            alternatives = result.alternatives,
            valid = result.valid,
            message = result.message
        )
    }
}

data class EquipmentCatalogResult<T>(
    val selected: T?,
    val alternatives: List<T>,
    val valid: Boolean,
    val message: String
)
