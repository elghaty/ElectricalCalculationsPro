package com.electrical.calculationspro.data.catalog

object ContactorCatalog {

    private val standardRatings = listOf(
        6.0,
        9.0,
        12.0,
        18.0,
        25.0,
        32.0,
        40.0,
        50.0,
        65.0,
        80.0,
        95.0,
        115.0,
        150.0,
        185.0,
        225.0,
        265.0,
        330.0,
        400.0
    )

    private fun create(
        manufacturer: Manufacturer,
        family: String,
        current: Double
    ): ContactorCatalogItem {

        val source = when (manufacturer) {
            Manufacturer.SCHNEIDER_ELECTRIC ->
                CatalogSources.schneiderCompactNsx

            else ->
                CatalogSources.generic
        }

        return ContactorCatalogItem(
            manufacturer = manufacturer,
            family = family,
            model = "$family $current A",
            ratedCurrentA = current,
            voltageV = 400.0,
            utilizationCategory = "AC-3",
            source = source
        )
    }

    fun schneider(): List<ContactorCatalogItem> =
        standardRatings.map {
            create(
                Manufacturer.SCHNEIDER_ELECTRIC,
                "TeSys",
                it
            )
        }

    fun generic(): List<ContactorCatalogItem> =
        standardRatings.map {
            create(
                Manufacturer.GENERIC,
                "AC-3 Contactor",
                it
            )
        }

    fun all(): List<ContactorCatalogItem> =
        schneider() + generic()

    fun select(
        motorCurrentA: Double
    ): List<ContactorCatalogItem> =
        all()
            .filter {
                it.ratedCurrentA >= motorCurrentA
            }
            .sortedBy {
                it.ratedCurrentA
            }
}
