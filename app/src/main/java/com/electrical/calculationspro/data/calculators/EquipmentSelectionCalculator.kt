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

object EquipmentSelectionCalculator {

    data class EquipmentCatalogResult<T>(
        val selected: T?,
        val alternatives: List<T>,
        val valid: Boolean,
        val message: String,
        val standard: Standard = Standard.IEC
    )

    fun selectBreaker(
        ratedCurrentA: Double,
        breakingCapacityKA: Double = 0.0,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BreakerCatalogItem> {
        if (!ratedCurrentA.isFinite() || ratedCurrentA <= 0.0) {
            return invalid(
                "Breaker rated current must be finite and greater than zero.",
                standard
            )
        }

        if (!breakingCapacityKA.isFinite() || breakingCapacityKA < 0.0) {
            return invalid(
                "Required breaking capacity must be finite and cannot be negative.",
                standard
            )
        }

        return CatalogSelector.selectBreaker(
            designCurrentA = ratedCurrentA,
            shortCircuitKA = breakingCapacityKA,
            manufacturer = manufacturer
        ).toCoreResult(standard)
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

    fun selectCable(
        sectionMm2: Double,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<CableCatalogItem> {
        if (!sectionMm2.isFinite() || sectionMm2 <= 0.0) {
            return invalid(
                "Required cable section must be finite and greater than zero.",
                standard
            )
        }

        return CatalogSelector.selectCable(
            requiredSectionMm2 = sectionMm2,
            manufacturer = manufacturer
        ).toCoreResult(standard)
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
        if (!sectionMm2.isFinite() || sectionMm2 <= 0.0) {
            return invalid(
                "Required cable section must be finite and greater than zero.",
                standard
            )
        }

        if (material.isBlank()) {
            return invalid("Cable conductor material is required.", standard)
        }

        if (insulation.isBlank()) {
            return invalid("Cable insulation is required.", standard)
        }

        if (cores <= 0) {
            return invalid("Cable core count must be greater than zero.", standard)
        }

        if (voltageV <= 0) {
            return invalid("Cable voltage must be greater than zero.", standard)
        }

        val catalogResult = CatalogSelector.selectCable(
            requiredSectionMm2 = sectionMm2,
            manufacturer = manufacturer
        )

        val candidates = buildList {
            catalogResult.selected?.let(::add)
            addAll(catalogResult.alternatives)
        }
            .distinctBy { "${it.manufacturer}:${it.model}" }
            .asSequence()
            .filter { it.sectionMm2.isFinite() && it.sectionMm2 >= sectionMm2 }
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
            .filter { it.cores == cores }
            .filter { it.voltageClassV >= voltageV }
            .sortedWith(
                compareBy<CableCatalogItem> { it.sectionMm2 }
                    .thenBy { it.manufacturer.name }
                    .thenBy { it.model }
            )
            .toList()

        return EquipmentCatalogResult(
            selected = candidates.firstOrNull(),
            alternatives = candidates.drop(1),
            valid = false,
            message = if (candidates.isNotEmpty()) {
                "PRELIMINARY CATALOG MATCH ONLY. Section, conductor material, insulation, core count and voltage class have been filtered. Final approval still requires code-based ampacity, installation correction factors, voltage-drop verification, short-circuit withstand and exact-product documentation."
            } else {
                "NO MATCHING CATALOG ITEM. No listed item meets the requested minimum section, material, insulation, core count and voltage class."
            },
            standard = standard
        )
    }

    fun selectTransformer(
        requiredKva: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<TransformerCatalogItem> {
        if (!requiredKva.isFinite() || requiredKva <= 0.0) {
            return invalid(
                "Required transformer rating must be finite and greater than zero.",
                standard
            )
        }

        return CatalogSelector.selectTransformer(requiredKva)
            .toCoreResult(standard)
    }

    fun selectGenerator(
        requiredKva: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<GeneratorCatalogItem> {
        if (!requiredKva.isFinite() || requiredKva <= 0.0) {
            return invalid(
                "Required generator rating must be finite and greater than zero.",
                standard
            )
        }

        return CatalogSelector.selectGenerator(requiredKva)
            .toCoreResult(standard)
    }

    fun selectBusbar(
        ratedCurrentA: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BusbarCatalogItem> {
        if (!ratedCurrentA.isFinite() || ratedCurrentA <= 0.0) {
            return invalid(
                "Required busbar current must be finite and greater than zero.",
                standard
            )
        }

        return CatalogSelector.selectBusbar(ratedCurrentA)
            .toCoreResult(standard)
    }

    fun selectContactor(
        motorCurrentA: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<ContactorCatalogItem> {
        if (!motorCurrentA.isFinite() || motorCurrentA <= 0.0) {
            return invalid(
                "Motor current must be finite and greater than zero.",
                standard
            )
        }

        return CatalogSelector.selectContactor(motorCurrentA)
            .toCoreResult(standard)
    }

    fun selectPanel(
        ratedCurrentA: Double,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<PanelCatalogItem> {
        if (!ratedCurrentA.isFinite() || ratedCurrentA <= 0.0) {
            return invalid(
                "Panel rated current must be finite and greater than zero.",
                standard
            )
        }

        return CatalogSelector.selectPanel(ratedCurrentA)
            .toCoreResult(standard)
    }

    private fun <T> EquipmentSelectionResult<T>.toCoreResult(
        standard: Standard
    ): EquipmentCatalogResult<T> =
        EquipmentCatalogResult(
            selected = selected,
            alternatives = alternatives,
            valid = valid,
            message = message,
            standard = standard
        )

    private fun <T> invalid(
        message: String,
        standard: Standard
    ): EquipmentCatalogResult<T> =
        EquipmentCatalogResult(
            selected = null,
            alternatives = emptyList(),
            valid = false,
            message = message,
            standard = standard
        )
}
