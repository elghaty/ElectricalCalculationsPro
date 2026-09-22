package com.electrical.calculationspro.data.catalog

/**
 * Manufacturer breaker catalog.
 *
 * IMPORTANT:
 * A breaker with unknown breaking capacity cannot be considered
 * suitable for a known prospective short-circuit current.
 *
 * Missing manufacturer data therefore causes the catalog selection
 * to return no verified breaker rather than inventing a rating.
 */
object BreakerCatalog {

    private val commonRatings =
        listOf(
            2.0,
            4.0,
            6.0,
            10.0,
            13.0,
            16.0,
            20.0,
            25.0,
            32.0,
            40.0,
            50.0,
            63.0,
            80.0,
            100.0,
            125.0,
            160.0,
            200.0,
            250.0,
            320.0,
            400.0,
            500.0,
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

    private fun schneiderMccb(
        family: String,
        rating: Double
    ): BreakerCatalogItem =
        BreakerCatalogItem(
            manufacturer =
                Manufacturer.SCHNEIDER_ELECTRIC,
            family =
                family,
            model =
                "$family $rating A",
            type =
                EquipmentType.MCCB,
            poles =
                3,
            ratedCurrentA =
                rating,
            voltageV =
                415.0,
            breakingCapacityKA =
                null,
            standard =
                "IEC 60947-2",
            source =
                if (family == "ComPact NSX") {
                    CatalogSources.schneiderCompactNsx
                } else {
                    CatalogSources.schneiderEasyPactCvs
                }
        )

    private fun abbMccb(
        rating: Double
    ): BreakerCatalogItem =
        BreakerCatalogItem(
            manufacturer =
                Manufacturer.ABB,
            family =
                "SACE Tmax XT",
            model =
                "Tmax XT $rating A",
            type =
                EquipmentType.MCCB,
            poles =
                3,
            ratedCurrentA =
                rating,
            voltageV =
                415.0,
            breakingCapacityKA =
                null,
            standard =
                "IEC 60947-2",
            source =
                CatalogSources.abbTmaxXt
        )

    private fun siemensMccb(
        rating: Double
    ): BreakerCatalogItem =
        BreakerCatalogItem(
            manufacturer =
                Manufacturer.SIEMENS,
            family =
                "SENTRON",
            model =
                "SENTRON $rating A",
            type =
                EquipmentType.MCCB,
            poles =
                3,
            ratedCurrentA =
                rating,
            voltageV =
                415.0,
            breakingCapacityKA =
                null,
            standard =
                "IEC 60947-2",
            source =
                CatalogSources.siemensSentron
        )

    private fun legrandMccb(
        rating: Double
    ): BreakerCatalogItem =
        BreakerCatalogItem(
            manufacturer =
                Manufacturer.LEGRAND,
            family =
                "DPX",
            model =
                "DPX $rating A",
            type =
                EquipmentType.MCCB,
            poles =
                3,
            ratedCurrentA =
                rating,
            voltageV =
                415.0,
            breakingCapacityKA =
                null,
            standard =
                "IEC 60947-2",
            source =
                CatalogSources.legrandDpx
        )

    private fun lsMccb(
        rating: Double
    ): BreakerCatalogItem =
        BreakerCatalogItem(
            manufacturer =
                Manufacturer.LS_ELECTRIC,
            family =
                "Susol",
            model =
                "Susol $rating A",
            type =
                EquipmentType.MCCB,
            poles =
                3,
            ratedCurrentA =
                rating,
            voltageV =
                415.0,
            breakingCapacityKA =
                null,
            standard =
                "IEC 60947-2",
            source =
                CatalogSources.lsSusol
        )

    fun schneiderCompactNsx(): List<BreakerCatalogItem> =
        listOf(
            100.0,
            160.0,
            250.0,
            400.0,
            630.0
        ).map {
            schneiderMccb(
                family = "ComPact NSX",
                rating = it
            )
        }

    fun schneiderEasyPactCvs(): List<BreakerCatalogItem> =
        listOf(
            100.0,
            160.0,
            250.0,
            400.0,
            630.0
        ).map {
            schneiderMccb(
                family = "EasyPact CVS",
                rating = it
            )
        }

    fun abbTmaxXt(): List<BreakerCatalogItem> =
        listOf(
            160.0,
            250.0,
            400.0,
            630.0,
            800.0,
            1000.0,
            1250.0,
            1600.0
        ).map {
            abbMccb(it)
        }

    fun siemensSentron(): List<BreakerCatalogItem> =
        listOf(
            160.0,
            250.0,
            400.0,
            630.0,
            800.0,
            1000.0,
            1250.0,
            1600.0
        ).map {
            siemensMccb(it)
        }

    fun legrandDpx(): List<BreakerCatalogItem> =
        listOf(
            160.0,
            250.0,
            400.0,
            630.0
        ).map {
            legrandMccb(it)
        }

    fun lsSusol(): List<BreakerCatalogItem> =
        listOf(
            160.0,
            250.0,
            400.0,
            630.0,
            800.0,
            1000.0,
            1250.0,
            1600.0
        ).map {
            lsMccb(it)
        }

    fun mcbRatings(): List<Double> =
        commonRatings.filter {
            it <= 63.0
        }

    fun all(): List<BreakerCatalogItem> =
        schneiderCompactNsx() +
            schneiderEasyPactCvs() +
            abbTmaxXt() +
            siemensSentron() +
            legrandDpx() +
            lsSusol()

    fun byManufacturer(
        manufacturer: Manufacturer
    ): List<BreakerCatalogItem> =
        all().filter {
            it.manufacturer == manufacturer
        }

    fun select(
        requiredCurrentA: Double,
        requiredBreakingCapacityKA: Double = 0.0
    ): List<BreakerCatalogItem> {

        require(requiredCurrentA >= 0.0) {
            "Required breaker current cannot be negative."
        }

        require(requiredBreakingCapacityKA >= 0.0) {
            "Required breaking capacity cannot be negative."
        }

        return all()
            .filter {
                it.ratedCurrentA >=
                    requiredCurrentA
            }
            .filter {
                if (requiredBreakingCapacityKA <= 0.0) {
                    true
                } else {
                    val breakingCapacity =
                        it.breakingCapacityKA

                    breakingCapacity != null &&
                        breakingCapacity >=
                            requiredBreakingCapacityKA
                }
            }
            .sortedWith(
                compareBy<BreakerCatalogItem> {
                    it.ratedCurrentA
                }.thenBy {
                    it.manufacturer.name
                }
            )
    }
}
