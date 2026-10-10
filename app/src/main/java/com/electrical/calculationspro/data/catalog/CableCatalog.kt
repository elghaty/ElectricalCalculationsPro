
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

    /**
     * Exact product backed by the manufacturer certificate
     * registered in CatalogSources.
     *
     * Only the documented 3-core, 1.5 mm² configuration is
     * represented here. Other sizes are not inferred from it.
     */
    fun verifiedProducts(): List<CableCatalogItem> =
        listOf(
            CableCatalogItem(
                manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
                family = "N2XH-J",
                model = "40035652 / N2XH-J 3 x 1.5 RM",
                conductorMaterial = "Copper",
                insulation = "XLPE",
                voltageClassV = 1000,
                cores = 3,
                sectionMm2 = 1.5,
                application = "Low Voltage Power",
                source = CatalogSources.elsewedyN2xhJ3x15
            )
        )

    fun elsewedyCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            create(
                manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
                family = "Elsewedy LV Cable",
                section = it,
                insulation = "PVC",
                voltageClassV = 600
            )
        }

    fun elsewedyCopperXlpe(): List<CableCatalogItem> =
        standardSections.map {
            create(
                manufacturer = Manufacturer.ELSEWEDY_ELECTRIC,
                family = "Elsewedy LV Cable",
                section = it,
                insulation = "XLPE",
                voltageClassV = 1000
            )
        }

    fun nexansCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            create(
                manufacturer = Manufacturer.NEXANS,
                family = "Nexans LV Cable",
                section = it,
                insulation = "PVC",
                voltageClassV = 600
            )
        }

    fun gizaCopperPvc(): List<CableCatalogItem> =
        standardSections.map {
            create(
                manufacturer = Manufacturer.GIZA_CABLES,
                family = "Giza Cables LV",
                section = it,
                insulation = "PVC",
                voltageClassV = 600
            )
        }

    /**
     * Includes exact documented products and family-level
     * engineering candidates. Callers must use CodeCompliance
     * before treating any candidate as finally compliant.
     */
    fun all(): List<CableCatalogItem> =
        verifiedProducts() +
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

    fun sections(): List<Double> =
        standardSections

    fun nextStandardSection(
        requiredSectionMm2: Double
    ): Double? =
        standardSections.firstOrNull {
            it >= requiredSectionMm2
        }
}
