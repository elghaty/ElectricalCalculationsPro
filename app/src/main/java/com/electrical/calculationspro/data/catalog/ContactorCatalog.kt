package com.electrical.calculationspro.data.catalog

/**
 * Contactor catalogue.
 *
 * Only the exact Schneider LC1D25D7 reference is represented as
 * a verified commercial product in this catalogue.
 *
 * Other ratings are generic engineering references and cannot
 * pass final CodeCompliance evaluation.
 */
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

    private fun createGeneric(
        current: Double
    ): ContactorCatalogItem =
        ContactorCatalogItem(
            manufacturer = Manufacturer.GENERIC,
            family = "AC-3 Contactor - Engineering Reference",
            model = "Generic AC-3 Contactor ${current.toInt()} A - Not a Verified Product",
            ratedCurrentA = current,
            voltageV = 400.0,
            utilizationCategory = "AC-3",
            source = CatalogSources.generic
        )

    /**
     * Exact manufacturer reference:
     * Schneider Electric TeSys D LC1D25D7.
     *
     * The coil code is part of the commercial reference.
     * Verify coil voltage and application compatibility before use.
     */
    fun schneider(): List<ContactorCatalogItem> =
        listOf(
            ContactorCatalogItem(
                manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
                family = "TeSys D",
                model = "LC1D25D7",
                ratedCurrentA = 25.0,
                voltageV = 440.0,
                utilizationCategory = "AC-3",
                source = CatalogSources.schneiderLc1d25d7
            )
        )

    /**
     * Generic ratings for preliminary engineering selection only.
     */
    fun generic(): List<ContactorCatalogItem> =
        standardRatings.map(::createGeneric)

    /**
     * All available records: verified exact product plus generic
     * reference ratings. Generic records are not approved products.
     */
    fun all(): List<ContactorCatalogItem> =
        schneider() + generic()

    /**
     * Preliminary selection by current rating.
     * A returned candidate is not automatically compliant.
     */
    fun select(
        motorCurrentA: Double
    ): List<ContactorCatalogItem> {

        if (!motorCurrentA.isFinite() || motorCurrentA <= 0.0) {
            return emptyList()
        }

        return all()
            .filter { item ->
                item.ratedCurrentA >= motorCurrentA
            }
            .sortedWith(
                compareBy<ContactorCatalogItem> {
                    it.ratedCurrentA
                }.thenBy {
                    if (
                        it.source.status ==
                        ProductFamilyStatus.VERIFIED_PRODUCT
                    ) {
                        0
                    } else {
                        1
                    }
                }.thenBy {
                    it.manufacturer.name
                }.thenBy {
                    it.model
                }
            )
    }

    /**
     * Exposes nominal reference ratings for preliminary sizing.
     */
    fun standardRatingsA(): List<Double> =
        standardRatings.toList()
}
