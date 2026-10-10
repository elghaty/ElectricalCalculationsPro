
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

        val isExactSchneider25A =
            manufacturer == Manufacturer.SCHNEIDER_ELECTRIC &&
                current == 25.0

        val source =
            if (isExactSchneider25A) {
                CatalogSources.schneiderLc1d25d7
            } else {
                CatalogSources.generic
            }

        val model =
            if (isExactSchneider25A) {
                "LC1D25D7"
            } else {
                "$family $current A - exact reference required"
            }

        return ContactorCatalogItem(
            manufacturer = manufacturer,
            family = family,
            model = model,
            ratedCurrentA = current,
            voltageV = 400.0,
            utilizationCategory = "AC-3",
            source = source
        )
    }

    fun schneider(): List<ContactorCatalogItem> =
        standardRatings.map { current ->
            create(
                manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
                family = "TeSys",
                current = current
            )
        }

    fun generic(): List<ContactorCatalogItem> =
        standardRatings.map { current ->
            create(
                manufacturer = Manufacturer.GENERIC,
                family = "AC-3 Contactor",
                current = current
            )
        }

    fun all(): List<ContactorCatalogItem> =
        schneider() + generic()

    fun select(
        motorCurrentA: Double
    ): List<ContactorCatalogItem> {
        require(motorCurrentA >= 0.0) {
            "Motor current cannot be negative."
        }

        return all()
            .filter {
                it.ratedCurrentA >= motorCurrentA
            }
            .sortedBy {
                it.ratedCurrentA
            }
    }
}
