package com.electrical.calculationspro.data.catalog

/**
 * Generator catalogue.
 *
 * Standard ratings are engineering reference values only.
 * They are not verified commercial products and must not be
 * represented as products of a named manufacturer.
 *
 * Final compliance requires an exact verified product reference,
 * official manufacturer data, and sufficient technical ratings.
 */
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

    private fun createReference(
        kva: Double
    ): GeneratorCatalogItem {

        return GeneratorCatalogItem(
            manufacturer = Manufacturer.GENERIC,
            family = "Diesel Generator - Reference Rating",
            model = "Generic Generator ${kva.toInt()} kVA - Not a Verified Product",
            ratedPowerKva = kva,
            ratedPowerKw = kva * 0.8,
            voltageV = 400.0,
            frequencyHz = 50.0,
            powerFactor = 0.8,
            source = CatalogSources.generic
        )
    }

    /**
     * Generic engineering reference ratings.
     *
     * These records support preliminary sizing only.
     * They must not be treated as manufacturer-certified equipment.
     */
    fun generic(): List<GeneratorCatalogItem> =
        standardKva.map(::createReference)

    /**
     * Backward-compatible method.
     *
     * No manufacturer-specific products are asserted here because
     * exact commercial references and their technical documents
     * have not been verified.
     */
    fun allFamilies(): List<GeneratorCatalogItem> =
        generic()

    /**
     * Returns preliminary sizing candidates whose nominal ratings
     * meet or exceed the requested apparent power.
     *
     * This method does not establish final catalogue compliance.
     */
    fun select(
        requiredKva: Double
    ): List<GeneratorCatalogItem> {

        if (!requiredKva.isFinite() || requiredKva <= 0.0) {
            return emptyList()
        }

        return generic()
            .filter {
                it.ratedPowerKva >= requiredKva
            }
            .sortedBy {
                it.ratedPowerKva
            }
    }

    /**
     * Exposes the available nominal reference ratings.
     */
    fun standardRatingsKva(): List<Double> =
        standardKva.toList()
}
