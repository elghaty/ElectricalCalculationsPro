package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.catalog.BreakerCatalogItem
import com.electrical.calculationspro.data.catalog.BusbarCatalogItem
import com.electrical.calculationspro.data.catalog.CableCatalogItem
import com.electrical.calculationspro.data.catalog.ContactorCatalogItem
import com.electrical.calculationspro.data.catalog.EquipmentCatalog
import com.electrical.calculationspro.data.catalog.EquipmentSelectionResult
import com.electrical.calculationspro.data.catalog.GeneratorCatalogItem
import com.electrical.calculationspro.data.catalog.Manufacturer
import com.electrical.calculationspro.data.catalog.PanelCatalogItem
import com.electrical.calculationspro.data.catalog.TransformerCatalogItem

/**
 * Professional Engineering Core - Equipment Selection Layer.
 *
 * Responsibility:
 * - Receive engineering selection requirements from the Core.
 * - Apply the selected electrical code where applicable.
 * - Search the equipment catalog.
 * - Return the selected real/catalogued equipment item together with alternatives.
 *
 * UI must NOT call EquipmentCatalog or CatalogSelector directly.
 *
 * Architecture:
 *
 * UI
 *   ↓
 * ElectricalCalculations
 *   ↓
 * EquipmentSelectionCalculator
 *   ↓
 * CatalogSelector / EquipmentCatalog
 */
object EquipmentSelectionCalculator {

    /**
     * Standardized result returned by the Professional Engineering Core.
     */
    data class EquipmentCatalogResult<T>(
        val selected: T?,
        val alternatives: List<T>,
        val valid: Boolean,
        val message: String,
        val standard: Standard? = null
    )

    // -------------------------------------------------------------------------
    // BREAKER
    // -------------------------------------------------------------------------

    /**
     * Select a breaker from the equipment catalog.
     *
     * ratedCurrentA:
     * Required continuous/design current.
     *
     * voltageV:
     * System voltage.
     *
     * poles:
     * Number of poles.
     *
     * breakingCapacityKA:
     * Required short-circuit breaking capacity.
     *
     * manufacturer:
     * Optional manufacturer filter.
     *
     * standard:
     * Selected engineering standard.
     */
    fun selectBreaker(
        ratedCurrentA: Double,
        voltageV: Double,
        poles: Int,
        breakingCapacityKA: Double,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BreakerCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                message = "Breaker rated current must be greater than zero.",
                standard = standard
            )
        }

        if (voltageV <= 0.0) {
            return invalid(
                message = "Breaker voltage must be greater than zero.",
                standard = standard
            )
        }

        if (poles <= 0) {
            return invalid(
                message = "Breaker pole count must be greater than zero.",
                standard = standard
            )
        }

        if (breakingCapacityKA < 0.0) {
            return invalid(
                message = "Breaking capacity cannot be negative.",
                standard = standard
            )
        }

        val result = EquipmentCatalog.selectBreaker(
            ratedCurrentA = ratedCurrentA,
            voltageV = voltageV,
            poles = poles,
            breakingCapacityKA = breakingCapacityKA,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    /**
     * Convenience overload when short-circuit breaking capacity is not yet known.
     *
     * The returned catalog item must NOT be interpreted as having a verified
     * short-circuit rating when the catalog does not provide one.
     */
    fun selectBreaker(
        ratedCurrentA: Double,
        voltageV: Double,
        poles: Int,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BreakerCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                message = "Breaker rated current must be greater than zero.",
                standard = standard
            )
        }

        if (voltageV <= 0.0) {
            return invalid(
                message = "Breaker voltage must be greater than zero.",
                standard = standard
            )
        }

        if (poles <= 0) {
            return invalid(
                message = "Breaker pole count must be greater than zero.",
                standard = standard
            )
        }

        val result = EquipmentCatalog.selectBreaker(
            ratedCurrentA = ratedCurrentA,
            voltageV = voltageV,
            poles = poles,
            breakingCapacityKA = null,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    // -------------------------------------------------------------------------
    // CABLE
    // -------------------------------------------------------------------------

    /**
     * Select a cable from the catalog.
     */
    fun selectCable(
        sectionMm2: Double,
        material: String,
        insulation: String,
        cores: Int,
        voltageV: Int,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<CableCatalogItem> {

        if (sectionMm2 <= 0.0) {
            return invalid(
                message = "Cable section must be greater than zero.",
                standard = standard
            )
        }

        if (cores <= 0) {
            return invalid(
                message = "Cable core count must be greater than zero.",
                standard = standard
            )
        }

        if (voltageV <= 0) {
            return invalid(
                message = "Cable voltage must be greater than zero.",
                standard = standard
            )
        }

        val result = EquipmentCatalog.selectCable(
            sectionMm2 = sectionMm2,
            material = material,
            insulation = insulation,
            cores = cores,
            voltageV = voltageV,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    /**
     * Simplified cable selection using engineering values.
     */
    fun selectCable(
        sectionMm2: Double,
        material: String,
        insulation: String,
        cores: Int = 1,
        voltageV: Int = 400,
        manufacturer: Manufacturer? = null
    ): EquipmentCatalogResult<CableCatalogItem> {

        return selectCable(
            sectionMm2 = sectionMm2,
            material = material,
            insulation = insulation,
            cores = cores,
            voltageV = voltageV,
            manufacturer = manufacturer,
            standard = Standard.IEC
        )
    }

    // -------------------------------------------------------------------------
    // TRANSFORMER
    // -------------------------------------------------------------------------

    /**
     * Select a transformer from the catalog.
     */
    fun selectTransformer(
        ratedPowerKva: Double,
        primaryVoltageV: Double,
        secondaryVoltageV: Double,
        frequencyHz: Double = 50.0,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<TransformerCatalogItem> {

        if (ratedPowerKva <= 0.0) {
            return invalid(
                message = "Transformer rated power must be greater than zero.",
                standard = standard
            )
        }

        if (primaryVoltageV <= 0.0) {
            return invalid(
                message = "Transformer primary voltage must be greater than zero.",
                standard = standard
            )
        }

        if (secondaryVoltageV <= 0.0) {
            return invalid(
                message = "Transformer secondary voltage must be greater than zero.",
                standard = standard
            )
        }

        if (frequencyHz <= 0.0) {
            return invalid(
                message = "Transformer frequency must be greater than zero.",
                standard = standard
            )
        }

        val result = EquipmentCatalog.selectTransformer(
            ratedPowerKva = ratedPowerKva,
            primaryVoltageV = primaryVoltageV,
            secondaryVoltageV = secondaryVoltageV,
            frequencyHz = frequencyHz,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    // -------------------------------------------------------------------------
    // GENERATOR
    // -------------------------------------------------------------------------

    /**
     * Select a generator from the catalog.
     */
    fun selectGenerator(
        ratedPowerKva: Double,
        voltageV: Double,
        frequencyHz: Double = 50.0,
        powerFactor: Double = 0.8,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<GeneratorCatalogItem> {

        if (ratedPowerKva <= 0.0) {
            return invalid(
                message = "Generator rated power must be greater than zero.",
                standard = standard
            )
        }

        if (voltageV <= 0.0) {
            return invalid(
                message = "Generator voltage must be greater than zero.",
                standard = standard
            )
        }

        if (frequencyHz <= 0.0) {
            return invalid(
                message = "Generator frequency must be greater than zero.",
                standard = standard
            )
        }

        if (powerFactor <= 0.0 || powerFactor > 1.0) {
            return invalid(
                message = "Generator power factor must be greater than 0 and not greater than 1.",
                standard = standard
            )
        }

        val result = EquipmentCatalog.selectGenerator(
            ratedPowerKva = ratedPowerKva,
            voltageV = voltageV,
            frequencyHz = frequencyHz,
            powerFactor = powerFactor,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    // -------------------------------------------------------------------------
    // BUSBAR
    // -------------------------------------------------------------------------

    /**
     * Select an LV busbar system.
     */
    fun selectBusbar(
        ratedCurrentA: Double,
        voltageV: Double,
        poles: Int,
        shortCircuitKA: Double? = null,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<BusbarCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                message = "Busbar rated current must be greater than zero.",
                standard = standard
            )
        }

        if (voltageV <= 0.0) {
            return invalid(
                message = "Busbar voltage must be greater than zero.",
                standard = standard
            )
        }

        if (poles <= 0) {
            return invalid(
                message = "Busbar pole count must be greater than zero.",
                standard = standard
            )
        }

        if (shortCircuitKA != null && shortCircuitKA < 0.0) {
            return invalid(
                message = "Busbar short-circuit current cannot be negative.",
                standard = standard
            )
        }

        val result = EquipmentCatalog.selectBusbar(
            ratedCurrentA = ratedCurrentA,
            voltageV = voltageV,
            poles = poles,
            shortCircuitKA = shortCircuitKA,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    // -------------------------------------------------------------------------
    // CONTACTOR
    // -------------------------------------------------------------------------

    /**
     * Select a contactor.
     */
    fun selectContactor(
        ratedCurrentA: Double,
        voltageV: Double,
        utilizationCategory: String = "AC-3",
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<ContactorCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                message = "Contactor rated current must be greater than zero.",
                standard = standard
            )
        }

        if (voltageV <= 0.0) {
            return invalid(
                message = "Contactor voltage must be greater than zero.",
                standard = standard
            )
        }

        if (utilizationCategory.isBlank()) {
            return invalid(
                message = "Contactor utilization category cannot be blank.",
                standard = standard
            )
        }

        val result = EquipmentCatalog.selectContactor(
            ratedCurrentA = ratedCurrentA,
            voltageV = voltageV,
            utilizationCategory = utilizationCategory,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    // -------------------------------------------------------------------------
    // PANEL
    // -------------------------------------------------------------------------

    /**
     * Select an LV panel.
     */
    fun selectPanel(
        ratedCurrentA: Double,
        voltageV: Double,
        poles: Int,
        manufacturer: Manufacturer? = null,
        standard: Standard = Standard.IEC
    ): EquipmentCatalogResult<PanelCatalogItem> {

        if (ratedCurrentA <= 0.0) {
            return invalid(
                message = "Panel rated current must be greater than zero.",
                standard = standard
            )
        }

        if (voltageV <= 0.0) {
            return invalid(
                message = "Panel voltage must be greater than zero.",
                standard = standard
            )
        }

        if (poles <= 0) {
            return invalid(
                message = "Panel pole count must be greater than zero.",
                standard = standard
            )
        }

        val result = EquipmentCatalog.selectPanel(
            ratedCurrentA = ratedCurrentA,
            voltageV = voltageV,
            poles = poles,
            manufacturer = manufacturer
        )

        return result.toCoreResult(standard)
    }

    // -------------------------------------------------------------------------
    // INTERNAL RESULT CONVERSION
    // -------------------------------------------------------------------------

    /**
     * Convert the catalog-layer result into the Professional Core result.
     *
     * Keeping this conversion here prevents the UI and higher layers from
     * depending on CatalogSelector implementation details.
     */
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
