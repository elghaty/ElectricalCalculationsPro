package com.electrical.calculationspro.data.catalog

object CableCatalog {

    private val standardSections = listOf(
        1.5,
        2.5,
        4.0,
        6.0,
        10.0,
        16.0,
        25.0,
        35.0,
        50.0,
        70.0,
        95.0,
        120.0,
        150.0,
        185.0,
        240.0,
        300.0,
        400.0,
        500.0,
        630.0
    )

    private fun copperPvc(
        manufacturer: Manufacturer,
        family: String,
        section: Double,
        cores: Int = 4
    ): CableCatalogItem {
        val source = when (manufacturer) {
            Manufacturer.ELSEWEDY_ELECTRIC ->
                CatalogSources.elsewedyCable

            Manufacturer.NEXANS ->
                CatalogSources.nexansCable

            Manufacturer.GIZA_CABLES ->
                CatalogSources.gizaCable

            else ->
                CatalogSources.generic
        }

        return CableCatalogItem(
            manufacturer = manufacturer,
            family = family,
            model = "$family Cu/PVC ${cores}C ${section}mm²",
            conductorMaterial = "Copper",
            insulation = "PVC",
            voltageClassV = 600,
            cores = cores,
            sectionMm2 = section,
            application = "Low Voltage Power",
            source = source
        )
    }

    private fun copperXlpe(
        manufacturer: Manufacturer,
        family: String,
        section: Double,
        cores: Int = 4
    ): CableCatalogItem {

        val source = when (manufacturer) {
            Manufacturer.ELSEWEDY_ELECTRIC ->
                CatalogSources.elsewedyCable

            Manufacturer.NEXANS ->
                CatalogSources.nexansCable

            Manufacturer.GIZA_CABLES ->
                CatalogSources.gizaCable

            else ->
                CatalogSources.generic
        }

        return CableCatalogItem(
            manufacturer = manufacturer,
            family = family,
            model = "$family Cu/XLPE ${cores}C ${section}mm²",
            conductorMaterial = "Copper",
            insulation = "XLPE",
            voltageClassV = 1000,
            cores = cores,
            sectionMm2 = section,
            application = "Low Voltage Power",
            source = source
        )
    }

    val sections: List<Double>
        get() = standardSections

    fun elsewedyCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            copperPvc(
                manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
                family = "Elsewedy LV Cable",
                section = it
            )
        }

    fun elsewedyCopperXlpe(): List<CableCatalogItem> =
        standardSections.map {
            copperXlpe(
                manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
                family = "Elsewedy LV Cable",
                section = it
            )
        }

    fun nexansCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            copperPvc(
                manufacturer = Manufacturer.NEXANS,
                family = "Nexans LV Cable",
                section = it
            )
        }

    fun gizaCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            copperPvc(
                manufacturer = Manufacturer.GIZA_CABLES,
                family = "Giza Cables LV",
                section = it
            )
        }

    fun all(): List<CableCatalogItem> =
        elsewedyCopperPvc() +
            elsewedyCopperXlpe() +
            nexansCopperPvc() +
            gizaCopperPvc()

    fun byManufacturer(
        manufacturer: Manufacturer
    ): List<CableCatalogItem> =
        all().filter {
            it.manufacturer == manufacturer
        }

    fun bySection(
        sectionMm2: Double
    ): List<CableCatalogItem> =
        all().filter {
            it.sectionMm2 == sectionMm2
        }

    fun byMaterialAndInsulation(
        material: String,
        insulation: String
    ): List<CableCatalogItem> =
        all().filter {
            it.conductorMaterial.equals(
                material,
                ignoreCase = true
            ) &&
                it.insulation.equals(
                    insulation,
                    ignoreCase = true
                )
        }
}
