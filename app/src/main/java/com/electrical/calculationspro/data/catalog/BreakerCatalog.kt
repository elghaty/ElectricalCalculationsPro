package com.electrical.calculationspro.data.catalog

/**
 * Professional LV breaker catalogue layer.
 *
 * Important:
 * Family existence and exact product verification are different.
 * A breaker without voltage-specific verified Icu is not accepted
 * when a prospective short-circuit current is supplied.
 */
object BreakerCatalog {

    private val commonRatings =
        listOf(
            2.0, 4.0, 6.0, 10.0, 13.0, 16.0, 20.0, 25.0,
            32.0, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0,
            160.0, 200.0, 250.0, 320.0, 400.0, 500.0,
            630.0, 800.0, 1000.0, 1250.0, 1600.0,
            2000.0, 2500.0, 3200.0, 4000.0, 5000.0, 6300.0
        )

    private fun schneider(
        family: String,
        model: String,
        rating: Double,
        breakingCapacityKA: Double? = null
    ) =
        BreakerCatalogItem(
            manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
            family = family,
            model = model,
            type = EquipmentType.MCCB,
            poles = 3,
            ratedCurrentA = rating,
            voltageV = 415.0,
            breakingCapacityKA = breakingCapacityKA,
            standard = "IEC 60947-2",
            source =
                if (family == "ComPact NSX") {
                    CatalogSources.schneiderCompactNsx
                } else {
                    CatalogSources.schneiderEasyPactCvs
                }
        )

    private fun abb(
        rating: Double
    ) =
        BreakerCatalogItem(
            manufacturer = Manufacturer.ABB,
            family = "SACE Tmax XT",
            model = "Tmax XT ${rating.toInt()} A",
            type = EquipmentType.MCCB,
            poles = 3,
            ratedCurrentA = rating,
            voltageV = 415.0,
            breakingCapacityKA = null,
            standard = "IEC 60947-2",
            source = CatalogSources.abbTmaxXt
        )

    private fun siemens(
        rating: Double
    ) =
        BreakerCatalogItem(
            manufacturer = Manufacturer.SIEMENS,
            family = "SENTRON",
            model = "SENTRON ${rating.toInt()} A",
            type = EquipmentType.MCCB,
            poles = 3,
            ratedCurrentA = rating,
            voltageV = 415.0,
            breakingCapacityKA = null,
            standard = "IEC 60947-2",
            source = CatalogSources.siemensSentron
        )

    private fun siemens3vmVerified() =
        BreakerCatalogItem(
            manufacturer = Manufacturer.SIEMENS,
            family = "SENTRON 3VM",
            model = "3VM11103ED220AA0",
            type = EquipmentType.MCCB,
            poles = 3,
            ratedCurrentA = 100.0,
            voltageV = 415.0,
            breakingCapacityKA = 25.0,
            standard = "IEC 60947-2",
            source = CatalogSources.siemens3vm
        )

    private fun legrand(
        rating: Double
    ) =
        BreakerCatalogItem(
            manufacturer = Manufacturer.LEGRAND,
            family = "DPX",
            model = "DPX ${rating.toInt()} A",
            type = EquipmentType.MCCB,
            poles = 3,
            ratedCurrentA = rating,
            voltageV = 415.0,
            breakingCapacityKA = null,
            standard = "IEC 60947-2",
            source = CatalogSources.legrandDpx
        )

    private fun ls(
        rating: Double
    ) =
        BreakerCatalogItem(
            manufacturer = Manufacturer.LS_ELECTRIC,
            family = "Susol",
            model = "Susol ${rating.toInt()} A",
            type = EquipmentType.MCCB,
            poles = 3,
            ratedCurrentA = rating,
            voltageV = 415.0,
            breakingCapacityKA = null,
            standard = "IEC 60947-2",
            source = CatalogSources.lsSusol
        )

    fun schneiderCompactNsx(): List<BreakerCatalogItem> =
        listOf(
            schneider(
                family = "ComPact NSX",
                model = "C25B32D250",
                rating = 250.0,
                breakingCapacityKA = 25.0
            ),
            schneider(
                family = "ComPact NSX",
                model = "ComPact NSX 100-250 A",
                rating = 250.0
            ),
            schneider(
                family = "ComPact NSX",
                model = "ComPact NSX 400 A",
                rating = 400.0
            ),
            schneider(
                family = "ComPact NSX",
                model = "ComPact NSX 630 A",
                rating = 630.0
            )
        )

    fun schneiderEasyPactCvs(): List<BreakerCatalogItem> =
        listOf(
            100.0,
            160.0,
            250.0,
            400.0,
            630.0
        ).map {
            schneider(
                family = "EasyPact CVS",
                model = "EasyPact CVS ${it.toInt()} A",
                rating = it
            )
        }

    fun abbTmaxXt(): List<BreakerCatalogItem> =
        listOf(
            160.0, 250.0, 400.0, 630.0,
            800.0, 1000.0, 1250.0, 1600.0
        ).map(::abb)

    fun siemensSentron(): List<BreakerCatalogItem> =
        listOf(
            160.0, 250.0, 400.0, 630.0,
            800.0, 1000.0, 1250.0, 1600.0
        ).map(::siemens) + siemens3vmVerified()

    fun legrandDpx(): List<BreakerCatalogItem> =
        listOf(
            160.0, 250.0, 400.0, 630.0
        ).map(::legrand)

    fun lsSusol(): List<BreakerCatalogItem> =
        listOf(
            160.0, 250.0, 400.0, 630.0,
            800.0, 1000.0, 1250.0, 1600.0
        ).map(::ls)

    fun mcbRatings(): List<Double> =
        commonRatings.filter { it <= 63.0 }

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
        all().filter { it.manufacturer == manufacturer }

    fun select(
        requiredCurrentA: Double,
        requiredBreakingCapacityKA: Double = 0.0
    ): List<BreakerCatalogItem> {

        require(requiredCurrentA >= 0.0)
        require(requiredBreakingCapacityKA >= 0.0)

        return all()
            .filter { it.ratedCurrentA >= requiredCurrentA }
            .filter { item ->
                if (requiredBreakingCapacityKA <= 0.0) {
                    true
                } else {
                    item.breakingCapacityKA
                        ?.let { it >= requiredBreakingCapacityKA }
                        ?: false
                }
            }
            .sortedWith(
                compareBy<BreakerCatalogItem> { it.ratedCurrentA }
                    .thenByDescending {
                        it.source.status == ProductFamilyStatus.VERIFIED_FAMILY
                    }
                    .thenBy { it.manufacturer.name }
            )
    }
}
