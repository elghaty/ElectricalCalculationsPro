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

enum class SelectionFailureReason {
    INVALID_INPUT,
    NO_CATALOG_MATCH,
    PRELIMINARY_ONLY,
    CODE_COMPLIANCE_NOT_VERIFIED
}

object EquipmentSelectionCalculator {

    data class EquipmentCatalogResult<T>(
        val selected: T?,
        val alternatives: List<T>,
        val valid: Boolean,
        val message: String,
        val standard: Standard = Standard.IEC,
        val failureReason: SelectionFailureReason? = null
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
                "Required breaking capacity must be finite and non-negative.",
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

        val allCandidates = buildList {
            catalogResult.selected?.let(::add)
            addAll(catalogResult.alternatives)
        }.distinctBy { "${it.manufacturer}:${it.model}" }

        val sectionCandidates = allCandidates.filter {
            it.sectionMm2.isFinite() && it.sectionMm2 >= sectionMm2
        }

        val materialCandidates = sectionCandidates.filter {
            it.conductorMaterial.equals(material.trim(), ignoreCase = true)
        }

        val insulationCandidates = materialCandidates.filter {
            it.insulation.equals(insulation.trim(), ignoreCase = true)
        }

        val coreCandidates = insulationCandidates.filter {
            it.cores == cores
        }

        val voltageCandidates = coreCandidates.filter {
            it.voltageClassV >= voltageV
        }.sortedWith(
            compareBy<CableCatalogItem> { it.sectionMm2 }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

        val reason = when {
            sectionCandidates.isEmpty() ->
                "No catalog item has the required minimum section."

            materialCandidates.isEmpty() ->
                "No catalog item matches conductor material '$material'."

            insulationCandidates.isEmpty() ->
                "No catalog item matches insulation '$insulation'."

            coreCandidates.isEmpty() ->
                "No catalog item matches the requested core count: $cores."

            voltageCandidates.isEmpty() ->
                "No catalog item meets the minimum voltage class of ${voltageV}V."

            else ->
                "A preliminary catalog match was found, but final code compliance is not verified."
        }

        val matched = voltageCandidates.isNotEmpty()

        return EquipmentCatalogResult(
            selected = voltageCandidates.firstOrNull(),
            alternatives = voltageCandidates.drop(1),
            valid = false,
            message = if (matched) {
                "PRELIMINARY ONLY. Cable filters matched. Verify corrected ampacity, installation method, voltage drop, short-circuit withstand and exact manufacturer documentation before approval."
            } else {
                reason
            },
            standard = standard,
            failureReason = if (matched) {
                SelectionFailureReason.PRELIMINARY_ONLY
            } else {
                SelectionFailureReason.NO_CATALOG_MATCH
            }
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
    ): EquipmentCatalogResult<T> {
        val reason = when {
            valid -> null
            selected == null ->
                SelectionFailureReason.NO_CATALOG_MATCH
            message.contains("PRELIMINARY", ignoreCase = true) ->
                SelectionFailureReason.PRELIMINARY_ONLY
            message.contains("NOT VERIFIED", ignoreCase = true) ->
                SelectionFailureReason.CODE_COMPLIANCE_NOT_VERIFIED
            else ->
                SelectionFailureReason.NO_CATALOG_MATCH
        }

        return EquipmentCatalogResult(
            selected = selected,
            alternatives = alternatives,
            valid = valid,
            message = message,
            standard = standard,
            failureReason = reason
        )
    }

    private fun <T> invalid(
        message: String,
        standard: Standard
    ): EquipmentCatalogResult<T> =
        EquipmentCatalogResult(
            selected = null,
            alternatives = emptyList(),
            valid = false,
            message = message,
            standard = standard,
            failureReason = SelectionFailureReason.INVALID_INPUT
        )
}
