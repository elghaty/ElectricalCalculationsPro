package com.electrical.calculationspro.data.catalog

object BusbarCatalog {

    private val standardRatings = listOf(
        160.0,
        250.0,
        400.0,
        630.0,
        800.0,
        1000.0,
        1250.0,
        1600.0,
        2000.0,
        2500.0,
        3200.0,
        4000.0,
        5000.0,
        6300.0
    )

    fun generic(): List<BusbarCatalogItem> =
        standardRatings.map {
            BusbarCatalogItem(
                manufacturer = Manufacturer.GENERIC,
                family = "LV Busbar",
                model = "LV Busbar ${it.toInt()} A",
                ratedCurrentA = it,
                voltageV = 415.0,
                shortCircuitKA = null,
                poles = 4,
                source = CatalogSources.generic
            )
        }

    fun select(
        requiredCurrentA: Double
    ): List<BusbarCatalogItem> =
        generic()
            .filter {
                it.ratedCurrentA >= requiredCurrentA
            }
            .sortedBy {
                it.ratedCurrentA
            }
}
