package com.electrical.calculationspro.data.catalog

object EquipmentCatalog {

    fun cables(): List<CableCatalogItem> =
        CableCatalog.all()

    fun breakers(): List<BreakerCatalogItem> =
        BreakerCatalog.all()

    fun transformers(): List<TransformerCatalogItem> =
        TransformerCatalog.egyptianTypical11kV400V()

    fun generators(): List<GeneratorCatalogItem> =
        GeneratorCatalog.generic()

    fun busbars(): List<BusbarCatalogItem> =
        BusbarCatalog.generic()

    fun contactors(): List<ContactorCatalogItem> =
        ContactorCatalog.all()

    fun panels(): List<PanelCatalogItem> =
        PanelCatalog.generic()

    fun selectBreaker(
        currentA: Double,
        shortCircuitKA: Double = 0.0
    ): List<BreakerCatalogItem> =
        BreakerCatalog.select(
            requiredCurrentA = currentA,
            requiredBreakingCapacityKA = shortCircuitKA
        )

    fun selectCable(
        currentA: Double,
        sectionMm2: Double? = null,
        manufacturer: Manufacturer? = null
    ): List<CableCatalogItem> {

        return CableCatalog.all()
            .filter {
                sectionMm2 == null ||
                    it.sectionMm2 >= sectionMm2
            }
            .filter {
                manufacturer == null ||
                    it.manufacturer == manufacturer
            }
            .sortedBy {
                it.sectionMm2
            }
    }

    fun selectTransformer(
        requiredKva: Double
    ): List<TransformerCatalogItem> =
        TransformerCatalog.select(
            requiredKva = requiredKva
        )

    fun selectGenerator(
        requiredKva: Double
    ): List<GeneratorCatalogItem> =
        GeneratorCatalog.select(
            requiredKva = requiredKva
        )

    fun selectBusbar(
        currentA: Double
    ): List<BusbarCatalogItem> =
        BusbarCatalog.select(
            requiredCurrentA = currentA
        )

    fun selectContactor(
        motorCurrentA: Double
    ): List<ContactorCatalogItem> =
        ContactorCatalog.select(
            motorCurrentA = motorCurrentA
        )

    fun selectPanel(
        currentA: Double
    ): List<PanelCatalogItem> =
        PanelCatalog.select(
            requiredCurrentA = currentA
        )
}
