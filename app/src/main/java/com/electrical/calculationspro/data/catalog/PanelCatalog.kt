package com.electrical.calculationspro.data.catalog

/**
 * Low-voltage panel engineering reference catalogue.
 *
 * Generic current ratings are preliminary design references only.
 * They are not manufacturer-certified assemblies.
 *
 * Final panel approval requires verified product/assembly data,
 * including applicable IEC 61439 verification and short-circuit
 * withstand for the actual assembly configuration.
 */
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

    /**
     * Generic reference panels for preliminary sizing.
     * Four poles are a nominal reference configuration only.
     */
    fun generic(): List<PanelCatalogItem> =
        standardRatings.map { current ->
            PanelCatalogItem(
                manufacturer = Manufacturer.GENERIC,
                family = "LV Distribution Panel - Engineering Reference",
                model = "Generic LV Panel ${current.toInt()} A - Not a Verified Product",
                ratedCurrentA = current,
                voltageV = 415.0,
                poles = 4,
                source = CatalogSources.generic
            )
        }

    /**
     * Preliminary selection by rated current.
     *
     * This does not certify enclosure protection, temperature rise,
     * internal separation, short-circuit withstand, or assembly design.
     */
    fun select(
        requiredCurrentA: Double
    ): List<PanelCatalogItem> {

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
     * Returns the nominal reference rating series.
     */
    fun standardRatingsA(): List<Double> =
        standardRatings.toList()
}
