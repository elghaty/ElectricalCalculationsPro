
package com.electrical.calculationspro.data.catalog

/**
 * Code/catalog compliance evaluation.
 *
 * A numerical fit is not enough for final product approval.
 * Final compliance requires an exact commercial product reference
 * and an official manufacturer source.
 */
object CodeCompliance {

    data class Result(
        val compliant: Boolean,
        val standard: String,
        val product: String?,
        val reasons: List<String>
    )

    fun evaluate(
        requirement: TechnicalRequirement.Cable,
        item: CableCatalogItem
    ): Result {
        val reasons = mutableListOf<String>()
        validateVerifiedProductSource(item.source, reasons)

        if (item.sectionMm2 < requirement.requiredSectionMm2) {
            reasons += "Cable section is below the calculated required section."
        }
        if (item.voltageClassV < requirement.requiredVoltageV) {
            reasons += "Cable voltage class is below the required system voltage."
        }
        if (item.cores != requirement.cores) {
            reasons += "Cable core count does not match the engineering requirement."
        }
        if (!item.conductorMaterial.equals(requirement.material.name, ignoreCase = true)) {
            reasons += "Cable conductor material does not match the engineering requirement."
        }
        if (!item.insulation.equals(requirement.insulation.name, ignoreCase = true)) {
            reasons += "Cable insulation does not match the engineering requirement."
        }

        return result(requirement.standard.name, item.model, reasons)
    }

    fun evaluate(
        requirement: TechnicalRequirement.Breaker,
        item: BreakerCatalogItem
    ): Result {
        val reasons = mutableListOf<String>()
        validateVerifiedProductSource(item.source, reasons)

        if (item.ratedCurrentA < requirement.requiredCurrentA) {
            reasons += "Breaker rated current is below the calculated design current."
        }
        if (item.voltageV < requirement.requiredVoltageV) {
            reasons += "Breaker rated voltage is below the required system voltage."
        }
        if (item.poles != requirement.poles) {
            reasons += "Breaker pole count does not match the engineering requirement."
        }

        if (requirement.requiredBreakingCapacityKA > 0.0) {
            val icu = item.breakingCapacityKA
            if (icu == null) {
                reasons += "Verified breaking-capacity data is unavailable for this catalog item."
            } else if (icu < requirement.requiredBreakingCapacityKA) {
                reasons += "Breaker breaking capacity is below the calculated prospective fault current."
            }
        }

        return result(requirement.standard.name, item.model, reasons)
    }

    fun evaluate(
        requirement: TechnicalRequirement.Transformer,
        item: TransformerCatalogItem
    ): Result {
        val reasons = mutableListOf<String>()
        validateVerifiedProductSource(item.source, reasons)

        if (item.ratedPowerKva < requirement.requiredKva) {
            reasons += "Transformer rated power is below the calculated requirement."
        }
        if (item.primaryVoltageV != requirement.primaryVoltageV) {
            reasons += "Transformer primary voltage does not match the engineering requirement."
        }
        if (item.secondaryVoltageV != requirement.secondaryVoltageV) {
            reasons += "Transformer secondary voltage does not match the engineering requirement."
        }
        if (item.frequencyHz != requirement.frequencyHz) {
            reasons += "Transformer frequency does not match the engineering requirement."
        }
        if (item.impedancePercent == null || item.impedancePercent <= 0.0) {
            reasons += "Verified transformer impedance is unavailable."
        }

        return result(requirement.standard.name, item.model, reasons)
    }

    fun evaluate(
        requirement: TechnicalRequirement.Generator,
        item: GeneratorCatalogItem
    ): Result {
        val reasons = mutableListOf<String>()
        validateVerifiedProductSource(item.source, reasons)

        if (item.ratedPowerKva < requirement.requiredKva) {
            reasons += "Generator rated power is below the calculated requirement."
        }
        if (item.voltageV < requirement.requiredVoltageV) {
            reasons += "Generator voltage is below the required system voltage."
        }
        if (item.frequencyHz != requirement.frequencyHz) {
            reasons += "Generator frequency does not match the engineering requirement."
        }
        if (item.powerFactor < requirement.powerFactor) {
            reasons += "Generator power factor is below the required value."
        }

        return result(requirement.standard.name, item.model, reasons)
    }

    fun evaluate(
        requirement: TechnicalRequirement.Busbar,
        item: BusbarCatalogItem
    ): Result {
        val reasons = mutableListOf<String>()
        validateVerifiedProductSource(item.source, reasons)

        if (item.ratedCurrentA < requirement.requiredCurrentA) {
            reasons += "Busbar rated current is below the calculated requirement."
        }
        if (item.voltageV < requirement.requiredVoltageV) {
            reasons += "Busbar rated voltage is below the required system voltage."
        }
        if (item.poles != requirement.poles) {
            reasons += "Busbar pole count does not match the engineering requirement."
        }

        if (requirement.requiredShortCircuitKA > 0.0) {
            val withstand = item.shortCircuitKA
            if (withstand == null) {
                reasons += "Verified short-circuit withstand data is unavailable."
            } else if (withstand < requirement.requiredShortCircuitKA) {
                reasons += "Busbar short-circuit withstand is below the calculated fault current."
            }
        }

        return result(requirement.standard.name, item.model, reasons)
    }

    fun evaluate(
        requirement: TechnicalRequirement.Contactor,
        item: ContactorCatalogItem
    ): Result {
        val reasons = mutableListOf<String>()
        validateVerifiedProductSource(item.source, reasons)

        if (item.ratedCurrentA < requirement.requiredCurrentA) {
            reasons += "Contactor rated current is below the calculated motor current."
        }
        if (item.voltageV < requirement.requiredVoltageV) {
            reasons += "Contactor rated voltage is below the required voltage."
        }
        if (!item.utilizationCategory.equals(
                requirement.utilizationCategory,
                ignoreCase = true
            )
        ) {
            reasons += "Contactor utilization category does not match the requirement."
        }

        return result(requirement.standard.name, item.model, reasons)
    }

    fun evaluate(
        requirement: TechnicalRequirement.Panel,
        item: PanelCatalogItem
    ): Result {
        val reasons = mutableListOf<String>()
        validateVerifiedProductSource(item.source, reasons)

        if (item.ratedCurrentA < requirement.requiredCurrentA) {
            reasons += "Panel rated current is below the calculated requirement."
        }
        if (item.voltageV < requirement.requiredVoltageV) {
            reasons += "Panel rated voltage is below the required system voltage."
        }
        if (item.poles != requirement.poles) {
            reasons += "Panel pole count does not match the engineering requirement."
        }

        return result(requirement.standard.name, item.model, reasons)
    }

    /**
     * Family-level and generic records can support preliminary design,
     * but cannot pass final product compliance.
     */
    private fun validateVerifiedProductSource(
        source: CatalogSource,
        reasons: MutableList<String>
    ) {
        if (source.status != ProductFamilyStatus.VERIFIED_PRODUCT) {
            reasons +=
                "Catalogue record is not an exact verified commercial product; family/generic records cannot pass final compliance."
        }

        if (source.officialUrl.isBlank()) {
            reasons +=
                "Official manufacturer source URL is missing; product data cannot be treated as verified."
        }

        if (
            source.sourceReference.isBlank() ||
            source.sourceReference.equals("NO COMMERCIAL PRODUCT", ignoreCase = true)
        ) {
            reasons += "Exact commercial product reference is missing."
        }
    }

    private fun result(
        standard: String,
        product: String,
        reasons: List<String>
    ): Result =
        Result(
            compliant = reasons.isEmpty(),
            standard = standard,
            product = product,
            reasons = reasons
        )
}
