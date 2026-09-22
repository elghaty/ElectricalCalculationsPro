package com.electrical.calculationspro.data.catalog

object GeneratorCatalog {

    private val standardKva = listOf(
        10.0,
        15.0,
        20.0,
        30.0,
        40.0,
        50.0,
        60.0,
        80.0,
        100.0,
        125.0,
        150.0,
        200.0,
        250.0,
        300.0,
        350.0,
        400.0,
        500.0,
        600.0,
        750.0,
        800.0,
        1000.0,
        1250.0,
        1500.0,
        1750.0,
        2000.0,
        2250.0,
        2500.0
    )

    private fun create(
        manufacturer: Manufacturer,
        family: String,
        kva: Double
    ): GeneratorCatalogItem {

        return GeneratorCatalogItem(
            manufacturer = manufacturer,
            family = family,
            model = "$family ${kva.toInt()} kVA",
            ratedPowerKva = kva,
            ratedPowerKw = kva * 0.8,
            voltageV = 400.0,
            frequencyHz = 50.0,
            powerFactor = 0.8,
            source = CatalogSources.generic
        )
    }

    fun generic(): List<GeneratorCatalogItem> =
        standardKva.map {
            create(
                Manufacturer.GENERIC,
                "Diesel Generator",
                it
            )
        }

    fun allFamilies(): List<GeneratorCatalogItem> =
        standardKva.flatMap { kva ->
            listOf(
                create(
                    Manufacturer.SIEMENS,
                    "Generator Package",
                    kva
                ),
                create(
                    Manufacturer.GENERIC,
                    "Diesel Generator",
                    kva
                )
            )
        }

    fun select(
        requiredKva: Double
    ): List<GeneratorCatalogItem> =
        allFamilies()
            .filter {
                it.ratedPowerKva >= requiredKva
            }
            .sortedBy {
                it.ratedPowerKva
            }
}
