
package com.electrical.calculationspro.data.catalog

/**
 * Verified manufacturer breaker catalog.
 *
 * Only products with explicitly verified manufacturer data are assigned
 * a non-null breaking capacity.
 *
 * Family-level records remain available for engineering discovery, but
 * they must not be treated as verified commercial selections when the
 * required short-circuit capacity is known.
 */
object BreakerCatalog {

    private fun verifiedSchneider(
        family: String,
        model: String,
        ratedCurrentA: Double,
        breakingCapacityKA: Double,
        source: CatalogSource
    ): BreakerCatalogItem =
        BreakerCatalogItem(
            manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
            family = family,
            model = model,
            type = EquipmentType.MCCB,
            poles = 3,
            ratedCurrentA = ratedCurrentA,
            voltageV = 415.0,
            breakingCapacityKA = breakingCapacityKA,
            standard = "IEC 60947-2",
            source = source
        )

    private fun verifiedSiemens(
        model: String,
        ratedCurrentA: Double,
        breakingCapacityKA: Double
    ): BreakerCatalogItem =
        BreakerCatalogItem(
            manufacturer = Manufacturer.SIEMENS,
            family = "SENTRON 3VM",
            model = model,
            type = EquipmentType.MCCB,
            poles = 3,
            ratedCurrentA = ratedCurrentA,
            voltageV = 415.0,
            breakingCapacityKA = breakingCapacityKA,
            standard = "IEC 60947-2",
            source = CatalogSources.siemens3vm11103ed220aa0
        )

    /**
     * Exact Schneider 250 A NSX250B reference.
     * C25B32D250: 3P, 250 A, 415 V AC, 25 kA class at 415 V AC.
     */
    fun schneiderCompactNsx250B(): List<BreakerCatalogItem> =
        listOf(
            verifiedSchneider(
                family = "ComPacT NSX250B",
                model = "C25B32D250",
                ratedCurrentA = 250.0,
                breakingCapacityKA = 25.0,
                source = CatalogSources.schneiderCompactNsxC25B32D250
            )
        )

    /**
     * Exact Schneider 250 A NSX250N reference.
     * C25N32D250: 3P, 250 A, 415 V AC, 50 kA class at 415 V AC.
     */
    fun schneiderCompactNsx250N(): List<BreakerCatalogItem> =
        listOf(
            verifiedSchneider(
                family = "ComPacT NSX250N",
                model = "C25N32D250",
                ratedCurrentA = 250.0,
                breakingCapacityKA = 50.0,
                source = CatalogSources.schneiderCompactNsxC25N32D250
            )
        )

    /**
     * Exact Schneider 250 A NSX250H reference.
     * C25H32D250: 3P, 250 A, 415 V AC, 70 kA class at 415 V AC.
     */
    fun schneiderCompactNsx250H(): List<BreakerCatalogItem> =
        listOf(
            verifiedSchneider(
                family = "ComPacT NSX250H",
                model = "C25H32D250",
                ratedCurrentA = 250.0,
                breakingCapacityKA = 70.0,
                source = CatalogSources.schneiderCompactNsxC25H32D250
            )
        )

    /**
     * Siemens exact reference retained in the controlled catalogue.
     * Final breaking-capacity selection must be checked against the
     * current manufacturer data for the actual operating voltage.
     */
    fun siemens3vmVerified(): List<BreakerCatalogItem> =
        listOf(
            verifiedSiemens(
                model = "3VM11103ED220AA0",
                ratedCurrentA = 100.0,
                breakingCapacityKA = 25.0
            )
        )

    /**
     * Family-level references have no assigned breaking capacity.
     */
    fun schneiderCompactNsxFamily(): List<BreakerCatalogItem> =
        listOf(
            BreakerCatalogItem(
                manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
                family = "ComPacT NSX",
                model = "ComPacT NSX - exact reference required",
                type = EquipmentType.MCCB,
                poles = 3,
                ratedCurrentA = 100.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.schneiderCompactNsxFamily
            )
        )

    fun abbTmaxXtFamily(): List<BreakerCatalogItem> =
        listOf(
            BreakerCatalogItem(
                manufacturer = Manufacturer.ABB,
                family = "SACE Tmax XT",
                model = "Tmax XT - exact order number required",
                type = EquipmentType.MCCB,
                poles = 3,
                ratedCurrentA = 160.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.abbTmaxXt
            )
        )

    fun siemensSentronFamily(): List<BreakerCatalogItem> =
        listOf(
            BreakerCatalogItem(
                manufacturer = Manufacturer.SIEMENS,
                family = "SENTRON 3VA",
                model = "3VA - exact order number required",
                type = EquipmentType.MCCB,
                poles = 3,
                ratedCurrentA = 100.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.siemensSentronFamily
            )
        )

    fun legrandDpxFamily(): List<BreakerCatalogItem> =
        listOf(
            BreakerCatalogItem(
                manufacturer = Manufacturer.LEGRAND,
                family = "DPX",
                model = "DPX - exact commercial reference required",
                type = EquipmentType.MCCB,
                poles = 3,
                ratedCurrentA = 160.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.legrandDpx
            )
        )

    fun lsSusolFamily(): List<BreakerCatalogItem> =
        listOf(
            BreakerCatalogItem(
                manufacturer = Manufacturer.LS_ELECTRIC,
                family = "Susol",
                model = "Susol - exact commercial reference required",
                type = EquipmentType.MCCB,
                poles = 3,
                ratedCurrentA = 160.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.lsSusol
            )
        )

    /**
     * Standard MCB nominal-current series.
     * These are nominal ratings only, not manufacturer products.
     */
    fun mcbRatings(): List<Double> =
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
            63.0
        )

    fun all(): List<BreakerCatalogItem> =
        schneiderCompactNsx250B() +
            schneiderCompactNsx250N() +
            schneiderCompactNsx250H() +
            siemens3vmVerified() +
            schneiderCompactNsxFamily() +
            abbTmaxXtFamily() +
            siemensSentronFamily() +
            legrandDpxFamily() +
            lsSusolFamily()

    fun verified(): List<BreakerCatalogItem> =
        schneiderCompactNsx250B() +
            schneiderCompactNsx250N() +
            schneiderCompactNsx250H() +
            siemens3vmVerified()

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

        val source =
            if (requiredBreakingCapacityKA > 0.0) {
                verified()
            } else {
                all()
            }

        return source
            .filter {
                it.ratedCurrentA >= requiredCurrentA
            }
            .filter {
                if (requiredBreakingCapacityKA <= 0.0) {
                    true
                } else {
                    val breakingCapacity = it.breakingCapacityKA

                    breakingCapacity != null &&
                        breakingCapacity >= requiredBreakingCapacityKA
                }
            }
            .sortedWith(
                compareBy<BreakerCatalogItem> {
                    it.ratedCurrentA
                }.thenBy {
                    it.breakingCapacityKA ?: Double.MAX_VALUE
                }.thenBy {
                    it.manufacturer.name
                }
            )
    }
}
