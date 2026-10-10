package com.electrical.calculationspro.data.catalog

/**
 * Engineering reference ratings for distribution transformers.
 *
 * These entries are generic sizing candidates, not verified commercial
 * products. Impedance is intentionally left unspecified until supported
 * by exact manufacturer data.
 */
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

    private fun createGeneric(
        family: String,
        kva: Double,
        primaryV: Double,
        secondaryV: Double,
        frequencyHz: Double = 50.0
    ): TransformerCatalogItem =
        TransformerCatalogItem(
            manufacturer = Manufacturer.GENERIC,
            family = family,
            model = "$family ${kva.toInt()} kVA",
            ratedPowerKva = kva,
            primaryVoltageV = primaryV,
            secondaryVoltageV = secondaryV,
            frequencyHz = frequencyHz,
            impedancePercent = null,
            source = CatalogSources.generic
        )

    /**
     * Generic reference ratings.
     *
     * A non-generic manufacturer is not returned as generic data must
     * never be labelled as a specific manufacturer's product.
     */
    fun genericDistribution(
        manufacturer: Manufacturer = Manufacturer.GENERIC
    ): List<TransformerCatalogItem> {
        if (manufacturer != Manufacturer.GENERIC) {
            return emptyList()
        }

        return standardRatingsKva.map { kva ->
            createGeneric(
                family = "Distribution Transformer",
                kva = kva,
                primaryV = 11000.0,
                secondaryV = 400.0
            )
        }
    }

    /**
     * Common 11 kV / 0.4 kV reference ratings for preliminary
     * engineering in Egypt. These are not verified manufacturer models.
     */
    fun egyptianTypical11kV400V(): List<TransformerCatalogItem> =
        standardRatingsKva.map { kva ->
            createGeneric(
                family = "11/0.4 kV Distribution Transformer",
                kva = kva,
                primaryV = 11000.0,
                secondaryV = 400.0
            )
        }

    /**
     * Returns preliminary candidates matching the requested rating
     * and voltage pair.
     *
     * Final compliance still requires exact verified product data,
     * including transformer impedance.
     */
    fun select(
        requiredKva: Double,
        primaryVoltageV: Double = 11000.0,
        secondaryVoltageV: Double = 400.0
    ): List<TransformerCatalogItem> {

        if (!requiredKva.isFinite() || requiredKva <= 0.0) {
            return emptyList()
        }

        if (!primaryVoltageV.isFinite() || primaryVoltageV <= 0.0) {
            return emptyList()
        }

        if (!secondaryVoltageV.isFinite() || secondaryVoltageV <= 0.0) {
            return emptyList()
        }

        return egyptianTypical11kV400V()
            .filter { item ->
                item.ratedPowerKva >= requiredKva
            }
            .filter { item ->
                sameVoltage(item.primaryVoltageV, primaryVoltageV)
            }
            .filter { item ->
                sameVoltage(item.secondaryVoltageV, secondaryVoltageV)
            }
            .sortedBy { item ->
                item.ratedPowerKva
            }
    }

    fun standardRatingsKva(): List<Double> =
        standardRatingsKva.toList()

    private fun sameVoltage(
        actual: Double,
        required: Double
    ): Boolean =
        actual.isFinite() &&
            required.isFinite() &&
            kotlin.math.abs(actual - required) <=
            maxOf(1.0e-6, kotlin.math.abs(required) * 1.0e-6)
}
