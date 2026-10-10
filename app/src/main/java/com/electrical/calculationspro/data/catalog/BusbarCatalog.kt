package com.electrical.calculationspro.data.catalog

/**
 * Low-voltage busbar engineering reference catalogue.
 *
 * Current ratings are preliminary sizing references, not verified
 * manufacturer products. Short-circuit withstand is intentionally
 * unavailable until supported by verified assembly documentation.
 *
 * Generic records must never pass final CodeCompliance evaluation.
 */
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

    /**
     * Returns generic reference ratings for preliminary design only.
     *
     * The 4-pole value is a nominal configuration reference.
     * Actual neutral sizing and assembly configuration require
     * project-specific engineering verification.
     */
    fun generic(): List<BusbarCatalogItem> =
        standardRatings.map { current ->
            BusbarCatalogItem(
                manufacturer = Manufacturer.GENERIC,
                family = "LV Busbar - Engineering Reference",
                model = "Generic LV Busbar ${current.toInt()} A - Not a Verified Product",
                ratedCurrentA = current,
                voltageV = 415.0,
                shortCircuitKA = null,
                poles = 4,
                source = CatalogSources.generic
            )
        }

    /**
     * Preliminary current-based selection only.
     *
     * This method does not verify short-circuit withstand,
     * temperature rise, enclosure compatibility, or IEC 61439
     * assembly compliance.
     */
    fun select(
        requiredCurrentA: Double
    ): List<BusbarCatalogItem> {

        if (!requiredCurrentA.isFinite() ||
            requiredCurrentA <= 0.0
        ) {
            return emptyList()
        }

        return generic()
            .filter { item ->
                item.ratedCurrentA >= requiredCurrentA
            }
            .sortedBy { item ->
                item.ratedCurrentA
            }
    }

    /**
     * Exposes the nominal reference rating series.
     */
    fun standardRatingsA(): List<Double> =
        standardRatings.toList()
}
