package com.electrical.calculationspro.data.catalog

enum class Manufacturer {
    SCHNEIDER_ELECTRIC,
    ABB,
    SIEMENS,
    LEGRAND,
    LS_ELECTRIC,
    ELSEWEDY_ELECTRIC,
    NEXANS,
    GIZA_CABLES,
    EGYPTIAN_CABLES,
    GENERIC
}

enum class EquipmentType {
    CABLE,
    MCB,
    MCCB,
    ACB,
    CONTACTOR,
    TRANSFORMER,
    GENERATOR,
    BUSBAR,
    PANEL
}

enum class ProductFamilyStatus {
    VERIFIED_FAMILY,
    ENGINEERING_DATA_REQUIRED,
    LEGACY_OR_REGIONAL,
    GENERIC
}

data class CatalogSource(
    val manufacturer: Manufacturer,
    val sourceName: String,
    val sourceReference: String,
    val status: ProductFamilyStatus
)

data class EquipmentSelectionResult<T>(
    val selected: T?,
    val alternatives: List<T> = emptyList(),
    val valid: Boolean,
    val message: String
)

data class CableCatalogItem(
    val manufacturer: Manufacturer,
    val family: String,
    val model: String,
    val conductorMaterial: String,
    val insulation: String,
    val voltageClassV: Int,
    val cores: Int,
    val sectionMm2: Double,
    val application: String,
    val source: CatalogSource
)

data class BreakerCatalogItem(
    val manufacturer: Manufacturer,
    val family: String,
    val model: String,
    val type: EquipmentType,
    val poles: Int,
    val ratedCurrentA: Double,
    val voltageV: Double,
    val breakingCapacityKA: Double?,
    val standard: String,
    val source: CatalogSource
)

data class TransformerCatalogItem(
    val manufacturer: Manufacturer,
    val family: String,
    val model: String,
    val ratedPowerKva: Double,
    val primaryVoltageV: Double,
    val secondaryVoltageV: Double,
    val frequencyHz: Double,
    val impedancePercent: Double?,
    val source: CatalogSource
)

data class GeneratorCatalogItem(
    val manufacturer: Manufacturer,
    val family: String,
    val model: String,
    val ratedPowerKva: Double,
    val ratedPowerKw: Double,
    val voltageV: Double,
    val frequencyHz: Double,
    val powerFactor: Double,
    val source: CatalogSource
)

data class BusbarCatalogItem(
    val manufacturer: Manufacturer,
    val family: String,
    val model: String,
    val ratedCurrentA: Double,
    val voltageV: Double,
    val shortCircuitKA: Double?,
    val poles: Int,
    val source: CatalogSource
)

data class ContactorCatalogItem(
    val manufacturer: Manufacturer,
    val family: String,
    val model: String,
    val ratedCurrentA: Double,
    val voltageV: Double,
    val utilizationCategory: String,
    val source: CatalogSource
)

data class PanelCatalogItem(
    val manufacturer: Manufacturer,
    val family: String,
    val model: String,
    val ratedCurrentA: Double,
    val voltageV: Double,
    val poles: Int,
    val source: CatalogSource
)
