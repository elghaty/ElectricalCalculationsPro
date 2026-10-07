package com.electrical.calculationspro.data.catalog

/**
 * Professional manufacturer breaker catalogue.
 *
 * IMPORTANT:
 * - No synthetic manufacturer model numbers are generated.
 * - No unknown breaking capacity is invented.
 * - Automatic short-circuit selection is allowed only when Icu is verified.
 */
object BreakerCatalog {

    private val verifiedProducts: List<BreakerCatalogItem> =
        listOf(

            BreakerCatalogItem(
                manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
                family = "ComPact NSX",
                model = "C10H3TM025",
                type = EquipmentType.MCCB,
                poles = 3,
                ratedCurrentA = 25.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.schneiderC10H3TM025
            ),

            BreakerCatalogItem(
                manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
                family = "ComPact NSX",
                model = "C10H3TM063",
                type = EquipmentType.MCCB,
                poles = 3,
                ratedCurrentA = 63.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.schneiderC10H3TM063
            ),

            BreakerCatalogItem(
                manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
                family = "ComPact NSX",
                model = "C16B42D160",
                type = EquipmentType.MCCB,
                poles = 4,
                ratedCurrentA = 160.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.schneiderC16B42D160
            ),

            BreakerCatalogItem(
                manufacturer = Manufacturer.SCHNEIDER_ELECTRIC,
                family = "ComPact NSX",
                model = "C40F42D400",
                type = EquipmentType.MCCB,
                poles = 4,
                ratedCurrentA = 400.0,
                voltageV = 415.0,
                breakingCapacityKA = null,
                standard = "IEC 60947-2",
                source = CatalogSources.schneiderC40F42D400
            ),

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
                source = CatalogSources.siemens3VM11103ED220AA0
            )
        )

    /**
     * Returns only verified commercial products.
     */
    fun all(): List<BreakerCatalogItem> =
        verifiedProducts

    /**
     * Returns products belonging to one manufacturer.
     */
    fun byManufacturer(
        manufacturer: Manufacturer
    ): List<BreakerCatalogItem> =
        verifiedProducts.filter {
            it.manufacturer == manufacturer
        }

    /**
     * Standard MCB current ratings.
     *
     * These are ratings only, not manufacturer products.
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

    /**
     * Selects verified breakers according to design current
     * and, when supplied, prospective short-circuit current.
     *
     * A product with unknown Icu is rejected when short-circuit
     * verification is required.
     */
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

        return verifiedProducts
            .filter { product ->
                product.ratedCurrentA >= requiredCurrentA
            }
            .filter { product ->

                if (requiredBreakingCapacityKA <= 0.0) {
                    true
                } else {

                    val icu =
                        product.breakingCapacityKA

                    icu != null &&
                        icu >= requiredBreakingCapacityKA
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

    /**
     * Returns only products whose breaking capacity
     * is actually verified.
     */
    fun withVerifiedBreakingCapacity():
        List<BreakerCatalogItem> =
        verifiedProducts.filter {
            it.breakingCapacityKA != null
        }

    /**
     * Selects the smallest verified breaker capable of
     * carrying the design current and interrupting the
     * calculated fault current.
     */
    fun selectForShortCircuit(
        requiredCurrentA: Double,
        prospectiveShortCircuitKA: Double
    ): BreakerCatalogItem? {

        require(requiredCurrentA >= 0.0) {
            "Required breaker current cannot be negative."
        }

        require(prospectiveShortCircuitKA >= 0.0) {
            "Prospective short-circuit current cannot be negative."
        }

        return verifiedProducts
            .filter {
                it.ratedCurrentA >= requiredCurrentA
            }
            .filter {
                val icu = it.breakingCapacityKA

                icu != null &&
                    icu >= prospectiveShortCircuitKA
            }
            .minWithOrNull(
                compareBy<BreakerCatalogItem> {
                    it.ratedCurrentA
                }.thenBy {
                    it.breakingCapacityKA ?: Double.MAX_VALUE
                }
            )
    }
}
