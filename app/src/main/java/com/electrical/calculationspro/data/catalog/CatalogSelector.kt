package com.electrical.calculationspro.data.catalog

object CatalogSelector {

/**
 * Preliminary selection only.
 *
 * Final breaker approval must use selectCompliantBreaker()
 * with current, voltage, poles and prospective fault current.
 */
fun selectBreaker(
    designCurrentA: Double,
    shortCircuitKA: Double = 0.0,
    manufacturer: Manufacturer? = null
): EquipmentSelectionResult<BreakerCatalogItem> {
    if (!designCurrentA.isFinite() || designCurrentA <= 0.0) {
        return invalid(
            "Breaker design current must be finite and greater than zero."
        )
    }

    if (!shortCircuitKA.isFinite() || shortCircuitKA < 0.0) {
        return invalid(
            "Required short-circuit current must be finite and cannot be negative."
        )
    }

    val candidates = EquipmentCatalog.selectBreaker(
        currentA = designCurrentA,
        shortCircuitKA = shortCircuitKA
    )
        .filter {
            manufacturer == null || it.manufacturer == manufacturer
        }
        .sortedWith(
            compareBy<BreakerCatalogItem> { it.ratedCurrentA }
                .thenBy {
                    it.breakingCapacityKA ?: Double.MAX_VALUE
                }
        )

    if (candidates.isEmpty()) {
        return invalid(
            if (shortCircuitKA > 0.0) {
                "No breaker candidate satisfies the current and requested breaking-capacity filters."
            } else {
                "No breaker candidate satisfies the required rated current."
            }
        )
    }

    return EquipmentSelectionResult(
        selected = candidates.first(),
        alternatives = candidates.drop(1),
        valid = false,
        message = "PRELIMINARY ONLY. Final approval requires verified voltage, poles, prospective fault current, breaking capacity at the applicable voltage and exact manufacturer product data. Use selectCompliantBreaker() for complete requirement checks."
    )
}

/**
 * Preliminary cable reference only.
 *
 * Final approval requires ampacity, installation conditions,
 * correction factors, voltage drop and exact product verification.
 */
fun selectCable(
    requiredSectionMm2: Double,
    manufacturer: Manufacturer? = null
): EquipmentSelectionResult<CableCatalogItem> {
    if (!requiredSectionMm2.isFinite() || requiredSectionMm2 <= 0.0) {
        return invalid(
            "Required cable section must be finite and greater than zero."
        )
    }

    val candidates = EquipmentCatalog.selectCable(
        currentA = 0.0,
        sectionMm2 = requiredSectionMm2,
        manufacturer = manufacturer
    ).sortedWith(
        compareBy<CableCatalogItem> { it.sectionMm2 }
            .thenBy { it.manufacturer.name }
            .thenBy { it.model }
    )

    if (candidates.isEmpty()) {
        return invalid(
            "No catalog cable exists for the requested section."
        )
    }

    return EquipmentSelectionResult(
        selected = candidates.first(),
        alternatives = candidates.drop(1),
        valid = false,
        message = "PRELIMINARY ONLY. The section alone does not verify current capacity, voltage class, cores, installation method, correction factors or voltage drop. Use selectCompliantCable() with the complete technical requirement."
    )
}

/**
 * Preliminary transformer rating only.
 */
fun selectTransformer(
    requiredKva: Double
): EquipmentSelectionResult<TransformerCatalogItem> {
    if (!requiredKva.isFinite() || requiredKva <= 0.0) {
        return invalid(
            "Required transformer rating must be finite and greater than zero."
        )
    }

    val candidates = EquipmentCatalog.selectTransformer(requiredKva)
        .sortedWith(
            compareBy<TransformerCatalogItem> { it.ratedPowerKva }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    if (candidates.isEmpty()) {
        return invalid(
            "No catalog transformer satisfies the required rating."
        )
    }

    return EquipmentSelectionResult(
        selected = candidates.first(),
        alternatives = candidates.drop(1),
        valid = false,
        message = "PRELIMINARY ONLY. Final selection requires matching primary and secondary voltages, frequency, verified impedance, losses, vector group and exact manufacturer product data. Use a complete TechnicalRequirement.Transformer for compliance evaluation."
    )
}

/**
 * Preliminary generator rating only.
 */
fun selectGenerator(
    requiredKva: Double
): EquipmentSelectionResult<GeneratorCatalogItem> {
    if (!requiredKva.isFinite() || requiredKva <= 0.0) {
        return invalid(
            "Required generator rating must be finite and greater than zero."
        )
    }

    val candidates = EquipmentCatalog.selectGenerator(requiredKva)
        .sortedWith(
            compareBy<GeneratorCatalogItem> { it.ratedPowerKva }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    if (candidates.isEmpty()) {
        return invalid(
            "No catalog generator satisfies the required rating."
        )
    }

    return EquipmentSelectionResult(
        selected = candidates.first(),
        alternatives = candidates.drop(1),
        valid = false,
        message = "PRELIMINARY ONLY. Final selection requires voltage, frequency, power factor, alternator performance, transient response, fault contribution and exact manufacturer product data."
    )
}

/**
 * Preliminary busbar candidate.
 *
 * A busbar is not approved without a positive fault-current
 * requirement and a verified withstand rating that meets it.
 */
fun selectBusbar(
    currentA: Double,
    requiredShortCircuitKA: Double = 0.0
): EquipmentSelectionResult<BusbarCatalogItem> {
    if (!currentA.isFinite() || currentA <= 0.0) {
        return invalid(
            "Required busbar current must be finite and greater than zero."
        )
    }

    if (!requiredShortCircuitKA.isFinite() ||
        requiredShortCircuitKA < 0.0
    ) {
        return invalid(
            "Required busbar short-circuit current must be finite and cannot be negative."
        )
    }

    val candidates = EquipmentCatalog.selectBusbar(currentA)
        .filter { item ->
            requiredShortCircuitKA <= 0.0 ||
                (
                    item.shortCircuitKA != null &&
                        item.shortCircuitKA >= requiredShortCircuitKA
                )
        }
        .sortedWith(
            compareBy<BusbarCatalogItem> { it.ratedCurrentA }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    if (candidates.isEmpty()) {
        return invalid(
            if (requiredShortCircuitKA > 0.0) {
                "No busbar candidate satisfies both rated current and the required short-circuit withstand."
            } else {
                "No catalog busbar satisfies the required current."
            }
        )
    }

    return EquipmentSelectionResult(
        selected = candidates.first(),
        alternatives = candidates.drop(1),
        valid = false,
        message = "PRELIMINARY ONLY. Final approval requires a positive prospective fault-current requirement, sufficient verified short-circuit withstand, rated voltage, pole count and verified assembly/product data. Use selectCompliantBusbar() with a complete TechnicalRequirement.Busbar."
    )
}

/**
 * Preliminary contactor candidate.
 *
 * Current alone does not establish motor-duty suitability.
 */
fun selectContactor(
    motorCurrentA: Double
): EquipmentSelectionResult<ContactorCatalogItem> {
    if (!motorCurrentA.isFinite() || motorCurrentA <= 0.0) {
        return invalid(
            "Motor current must be finite and greater than zero."
        )
    }

    val candidates = EquipmentCatalog.selectContactor(motorCurrentA)
        .sortedWith(
            compareBy<ContactorCatalogItem> { it.ratedCurrentA }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    if (candidates.isEmpty()) {
        return invalid(
            "No catalog contactor satisfies the required motor current."
        )
    }

    return EquipmentSelectionResult(
        selected = candidates.first(),
        alternatives = candidates.drop(1),
        valid = false,
        message = "PRELIMINARY ONLY. Final selection requires motor-duty category, rated operational voltage, motor current, coordination and exact manufacturer product data. Use selectCompliantContactor() with a complete TechnicalRequirement.Contactor."
    )
}

/**
 * Preliminary panel candidate.
 *
 * Rated current and an official URL alone do not establish
 * compliance of a complete low-voltage assembly.
 */
fun selectPanel(
    currentA: Double
): EquipmentSelectionResult<PanelCatalogItem> {
    if (!currentA.isFinite() || currentA <= 0.0) {
        return invalid(
            "Required panel current must be finite and greater than zero."
        )
    }

    val candidates = EquipmentCatalog.selectPanel(currentA)
        .sortedWith(
            compareBy<PanelCatalogItem> { it.ratedCurrentA }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    if (candidates.isEmpty()) {
        return invalid(
            "No catalog panel satisfies the required current."
        )
    }

    return EquipmentSelectionResult(
        selected = candidates.first(),
        alternatives = candidates.drop(1),
        valid = false,
        message = "PRELIMINARY ONLY. Final assembly approval requires rated voltage, pole count, enclosure/IP rating, temperature-rise assessment, short-circuit withstand and applicable assembly verification. Use selectCompliantPanel() with a complete TechnicalRequirement.Panel."
    )
}

/**
 * Final cable compliance selection.
 * CodeCompliance must validate the full requirement and source.
 */
fun selectCompliantCable(
    requirement: TechnicalRequirement.Cable,
    manufacturer: Manufacturer? = null
): EquipmentSelectionResult<CableCatalogItem> {
    val candidates = EquipmentCatalog.cables()
        .asSequence()
        .filter {
            manufacturer == null || it.manufacturer == manufacturer
        }
        .filter {
            CodeCompliance.evaluate(requirement, it).compliant
        }
        .sortedWith(
            compareBy<CableCatalogItem> { it.sectionMm2 }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )
        .toList()

    return selectionResult(
        candidates = candidates,
        successMessage = "Catalog cable satisfies the supplied technical requirement and exact-product source checks.",
        failureMessage = "No catalog cable satisfies the complete technical requirement with verified exact-product data."
    )
}

/**
 * Final breaker compliance selection.
 * Voltage, poles and fault capacity must be specified by the caller.
 */
fun selectCompliantBreaker(
    requirement: TechnicalRequirement.Breaker,
    manufacturer: Manufacturer? = null
): EquipmentSelectionResult<BreakerCatalogItem> {
    if (!requirement.requiredCurrentA.isFinite() ||
        requirement.requiredCurrentA <= 0.0 ||
        !requirement.requiredVoltageV.isFinite() ||
        requirement.requiredVoltageV <= 0.0 ||
        !requirement.requiredBreakingCapacityKA.isFinite() ||
        requirement.requiredBreakingCapacityKA <= 0.0 ||
        requirement.poles <= 0
    ) {
        return invalid(
            "Final breaker selection requires positive current, voltage, breaking capacity and pole count."
        )
    }

    val candidates = EquipmentCatalog.breakers()
        .asSequence()
        .filter {
            manufacturer == null || it.manufacturer == manufacturer
        }
        .filter {
            CodeCompliance.evaluate(requirement, it).compliant
        }
        .sortedWith(
            compareBy<BreakerCatalogItem> { it.ratedCurrentA }
                .thenBy {
                    it.breakingCapacityKA ?: Double.MAX_VALUE
                }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )
        .toList()

    return selectionResult(
        candidates = candidates,
        successMessage = "Breaker satisfies the supplied current, voltage, pole-count and fault-capacity requirements with verified exact-product source data.",
        failureMessage = "No breaker satisfies the complete technical requirement with verified exact-product and fault-capacity data."
    )
}

/**
 * Final transformer compliance selection.
 */
fun selectCompliantTransformer(
    requirement: TechnicalRequirement.Transformer
): EquipmentSelectionResult<TransformerCatalogItem> {
    val candidates = EquipmentCatalog.transformers()
        .filter {
            CodeCompliance.evaluate(requirement, it).compliant
        }
        .sortedWith(
            compareBy<TransformerCatalogItem> { it.ratedPowerKva }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    return selectionResult(
        candidates = candidates,
        successMessage = "Transformer satisfies the supplied technical requirement and verified exact-product source checks.",
        failureMessage = "No transformer satisfies the complete technical requirement with verified product and impedance data."
    )
}

/**
 * Final generator compliance selection.
 */
fun selectCompliantGenerator(
    requirement: TechnicalRequirement.Generator
): EquipmentSelectionResult<GeneratorCatalogItem> {
    val candidates = EquipmentCatalog.generators()
        .filter {
            CodeCompliance.evaluate(requirement, it).compliant
        }
        .sortedWith(
            compareBy<GeneratorCatalogItem> { it.ratedPowerKva }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    return selectionResult(
        candidates = candidates,
        successMessage = "Generator satisfies the supplied technical requirement and verified exact-product source checks.",
        failureMessage = "No generator satisfies the complete technical requirement with verified product data."
    )
}

/**
 * Final busbar compliance selection.
 * A positive short-circuit withstand requirement is mandatory.
 */
fun selectCompliantBusbar(
    requirement: TechnicalRequirement.Busbar
): EquipmentSelectionResult<BusbarCatalogItem> {
    if (!requirement.requiredCurrentA.isFinite() ||
        requirement.requiredCurrentA <= 0.0 ||
        !requirement.requiredVoltageV.isFinite() ||
        requirement.requiredVoltageV <= 0.0 ||
        !requirement.requiredShortCircuitKA.isFinite() ||
        requirement.requiredShortCircuitKA <= 0.0 ||
        requirement.poles <= 0
    ) {
        return invalid(
            "Final busbar selection requires positive current, voltage, short-circuit withstand and pole count."
        )
    }

    val candidates = EquipmentCatalog.busbars()
        .filter {
            CodeCompliance.evaluate(requirement, it).compliant
        }
        .sortedWith(
            compareBy<BusbarCatalogItem> { it.ratedCurrentA }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    return selectionResult(
        candidates = candidates,
        successMessage = "Busbar satisfies the supplied current, voltage, pole-count and short-circuit requirements with verified exact-product source data.",
        failureMessage = "No busbar satisfies the complete technical requirement with verified assembly and short-circuit data."
    )
}

/**
 * Final contactor compliance selection.
 */
fun selectCompliantContactor(
    requirement: TechnicalRequirement.Contactor
): EquipmentSelectionResult<ContactorCatalogItem> {
    val candidates = EquipmentCatalog.contactors()
        .filter {
            CodeCompliance.evaluate(requirement, it).compliant
        }
        .sortedWith(
            compareBy<ContactorCatalogItem> { it.ratedCurrentA }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    return selectionResult(
        candidates = candidates,
        successMessage = "Contactor satisfies the supplied technical requirement and verified exact-product source checks. Coordination must still be confirmed for the selected motor and protective devices.",
        failureMessage = "No contactor satisfies the complete technical requirement with verified product and utilization-category data."
    )
}

/**
 * Final panel requirement selection.
 */
fun selectCompliantPanel(
    requirement: TechnicalRequirement.Panel
): EquipmentSelectionResult<PanelCatalogItem> {
    val candidates = EquipmentCatalog.panels()
        .filter {
            CodeCompliance.evaluate(requirement, it).compliant
        }
        .sortedWith(
            compareBy<PanelCatalogItem> { it.ratedCurrentA }
                .thenBy { it.manufacturer.name }
                .thenBy { it.model }
        )

    return selectionResult(
        candidates = candidates,
        successMessage = "Panel record satisfies the supplied technical requirement and verified exact-product source checks. Complete assembly verification is still required before project approval.",
        failureMessage = "No panel record satisfies the complete technical requirement with verified assembly data."
    )
}

private fun <T> selectionResult(
    candidates: List<T>,
    successMessage: String,
    failureMessage: String
): EquipmentSelectionResult<T> {
    if (candidates.isEmpty()) {
        return invalid(failureMessage)
    }

    return EquipmentSelectionResult(
        selected = candidates.first(),
        alternatives = candidates.drop(1),
        valid = true,
        message = successMessage
    )
}

private fun <T> invalid(
    message: String
): EquipmentSelectionResult<T> =
    EquipmentSelectionResult(
        selected = null,
        alternatives = emptyList(),
        valid = false,
        message = message
    )

}
