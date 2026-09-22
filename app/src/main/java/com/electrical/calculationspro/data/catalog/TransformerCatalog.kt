package com.electrical.calculationspro.data.catalog

object TransformerCatalog {

    private val standardRatingsKva = listOf(
        25.0,
        50.0,
        63.0,
        100.0,
        160.0,
        200.0,
        250.0,
        315.0,
        400.0,
        500.0,
        630.0,
        800.0,
        1000.0,
        1250.0,
        1600.0,
        2000.0,
        2500.0,
        3150.0,
        4000.0,
        5000.0,
        6300.0
    )

    private fun create(
        manufacturer: Manufacturer,
        family: String,
        kva: Double,
        primaryV: Double = 11000.0,
        secondaryV: Double = 400.0
    ): TransformerCatalogItem {

        return TransformerCatalogItem(
            manufacturer = manufacturer,
            family = family,
            model = "$family ${kva.toInt()} kVA",
            ratedPowerKva = kva,
            primaryVoltageV = primaryV,
            secondaryVoltageV = secondaryV,
            frequencyHz = 50.0,
            impedancePercent = null,
            source = CatalogSources.generic
        )
    }

    fun genericDistribution(
        manufacturer: Manufacturer = Manufacturer.GENERIC
    ): List<TransformerCatalogItem> =
        standardRatingsKva.map {
            create(
                manufacturer = manufacturer,
                family = "Distribution Transformer",
                kva = it
            )
        }

    fun egyptianTypical11kV400V(): List<TransformerCatalogItem> =
        standardRatingsKva.map {
            create(
                manufacturer = Manufacturer.GENERIC,
                family = "11/0.4 kV Distribution Transformer",
                kva = it,
                primaryV = 11000.0,
                secondaryV = 400.0
            )
        }

    fun select(
        requiredKva: Double,
        primaryVoltageV: Double = 11000.0,
        secondaryVoltageV: Double = 400.0
    ): List<TransformerCatalogItem> {

        return egyptianTypical11kV400V()
            .filter {
                it.ratedPowerKva >= requiredKva
            }
            .filter {
                it.primaryVoltageV == primaryVoltageV
            }
            .filter {
                it.secondaryVoltageV == secondaryVoltageV
            }
            .sortedBy {
                it.ratedPowerKva
            }
    }
}
