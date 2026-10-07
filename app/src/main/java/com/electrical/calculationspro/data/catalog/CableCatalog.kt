package com.electrical.calculationspro.data.catalog

object CableCatalog {

    private val standardSections =
        listOf(
            1.5, 2.5, 4.0, 6.0, 10.0, 16.0,
            25.0, 35.0, 50.0, 70.0, 95.0,
            120.0, 150.0, 185.0, 240.0,
            300.0, 400.0, 500.0, 630.0
        )

    private fun sourceFor(
        manufacturer: Manufacturer
    ): CatalogSource =
        when (manufacturer) {
            Manufacturer.ELSEWEDY_ELECTRIC ->
                CatalogSources.elsewedyCable

            Manufacturer.NEXANS ->
                CatalogSources.nexansCable

            Manufacturer.GIZA_CABLES ->
                CatalogSources.gizaCable

            else ->
                CatalogSources.generic
        }

    private fun create(
        manufacturer: Manufacturer,
        family: String,
        section: Double,
        insulation: String,
        voltageClassV: Int,
        cores: Int = 4
    ): CableCatalogItem =
        CableCatalogItem(
            manufacturer = manufacturer,
            family = family,
            model =
                "$family ${if (insulation == "XLPE") "Cu/XLPE" else "Cu/PVC"} " +
                    "${cores}C ${section}mm²",
            conductorMaterial = "Copper",
            insulation = insulation,
            voltageClassV = voltageClassV,
            cores = cores,
            sectionMm2 = section,
            application = "Low Voltage Power",
            source = sourceFor(manufacturer)
        )

    fun elsewedyCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            create(
                Manufacturer.ELSEWEDY_ELECTRIC,
                "Elsewedy LV Cable",
                it,
                "PVC",
                600
            )
        }

    fun elsewedyCopperXlpe(): List<CableCatalogItem> =
        standardSections.map {
            create(
                Manufacturer.ELSEWEDY_ELECTRIC,
                "Elsewedy LV Cable",
                it,
                "XLPE",
                1000
            )
        }

    fun nexansCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            create(
                Manufacturer.NEXANS,
                "Nexans LV Cable",
                it,
                "PVC",
                600
            )
        }

    fun gizaCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            create(
                Manufacturer.GIZA_CABLES,
                "Giza Cables LV",
                it,
                "PVC",
                600
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
        all().filter { it.manufacturer == manufacturer }

    fun bySection(
        sectionMm2: Double
    ): List<CableCatalogItem> =
        all().filter { it.sectionMm2 == sectionMm2 }

    fun byMaterialAndInsulation(
        material: String,
        insulation: String
    ): List<CableCatalogItem> =
        all().filter {
            it.conductorMaterial.equals(material, true) &&
                it.insulation.equals(insulation, true)
        }
}
