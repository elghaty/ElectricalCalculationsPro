package com.electrical.calculationspro.data.catalog

object PanelCatalog {

    private val standardRatings = listOf(
        100.0,
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

    fun generic(): List<PanelCatalogItem> =
        standardRatings.map {
            PanelCatalogItem(
                manufacturer = Manufacturer.GENERIC,
                family = "LV Distribution Panel",
                model = "LV Panel ${it.toInt()} A",
                ratedCurrentA = it,
                voltageV = 415.0,
                poles = 4,
                source = CatalogSources.generic
            )
        }

    fun select(
        requiredCurrentA: Double
    ): List<PanelCatalogItem> =
        generic()
            .filter {
                it.ratedCurrentA >= requiredCurrentA
            }
            .sortedBy {
                it.ratedCurrentA
            }
}
