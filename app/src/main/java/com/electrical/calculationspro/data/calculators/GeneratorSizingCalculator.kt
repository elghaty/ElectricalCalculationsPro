
package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.catalog.GeneratorCatalogItem
import com.electrical.calculationspro.data.catalog.TechnicalRequirement
import kotlin.math.sqrt

/**
 * Generator sizing and catalogue integration.
 *
 * Engineering sizing is separated from final product approval.
 * Generic reference ratings are never reported as verified products.
 */
object GeneratorSizingCalculator {

    private const val EPSILON = 1.0e-9

    data class Input(
        val loadKw: Double,
        val loadPowerFactor: Double = 0.90,
        val demandFactor: Double = 1.0,
        val diversityFactor: Double = 1.0,
        val reserveFactor: Double = 1.20,
        val voltageV: Double = 400.0,
        val frequencyHz: Double = 50.0,
        val generatorPowerFactor: Double = 0.80,
        val currentType: CurrentType =
            CurrentType.AlternatingThreePhase
    )

    data class Result(
        val connectedLoadKw: Double,
        val designLoadKw: Double,
        val requiredKva: Double,
        val requiredCurrentA: Double,
        val selectedRatingKva: Double,
        val selectedGenerator: GeneratorCatalogItem?,
        val alternatives: List<GeneratorCatalogItem>,
        val standard: Standard,
        val preliminarySizingValid: Boolean,
        val catalogueComplianceVerified: Boolean,
        val notes: List<String>
    )

    fun calculate(
        input: Input,
        standard: Standard = Standard.IEC
    ): Result {
        validate(input)

        val designLoadKw =
            input.loadKw *
                input.demandFactor *
                input.diversityFactor

        val requiredKva =
            designLoadKw /
                input.loadPowerFactor *
                input.reserveFactor

        val requiredCurrentA = calculateCurrent(
            kva = requiredKva,
            voltageV = input.voltageV,
            currentType = input.currentType
        )

        val requirement = TechnicalRequirement.Generator(
            requiredKva = requiredKva,
            requiredVoltageV = input.voltageV,
            frequencyHz = input.frequencyHz,
            powerFactor = input.generatorPowerFactor,
            standard = standard
        )

        val catalogueResult =
            EquipmentSelectionCalculator.selectCompliantGenerator(
                requirement = requirement
            )

        val selected = catalogueResult.selected

        val ratingAdequate =
            selected != null &&
                selected.ratedPowerKva.isFinite() &&
                selected.ratedPowerKva + EPSILON >= requiredKva

        val notes = buildList {
            add("GENERATOR SIZING")
            add("Selected standard: $standard")
            add("Connected load: %.3f kW".format(input.loadKw))
            add("Demand factor: %.3f".format(input.demandFactor))
            add("Diversity multiplier: %.3f".format(input.diversityFactor))
            add("Design load: %.3f kW".format(designLoadKw))
            add("Load power factor: %.3f".format(input.loadPowerFactor))
            add("Reserve factor: %.3f".format(input.reserveFactor))
            add("Required generator capacity: %.3f kVA".format(requiredKva))
            add("Required output current: %.2f A".format(requiredCurrentA))
            add("Rated voltage: %.1f V".format(input.voltageV))
            add("Frequency: %.1f Hz".format(input.frequencyHz))
            add(
                "Generator rated power factor: %.3f"
                    .format(input.generatorPowerFactor)
            )

            if (selected != null) {
                add("Selected catalogue candidate: ${selected.model}")
                add("Candidate manufacturer: ${selected.manufacturer}")
                add(
                    "Candidate rated capacity: %.1f kVA"
                        .format(selected.ratedPowerKva)
                )
                add(
                    if (ratingAdequate) {
                        "Nominal capacity comparison: PASS"
                    } else {
                        "Nominal capacity comparison: FAIL"
                    }
                )
            } else {
                add("No generator candidate satisfies the catalogue query.")
            }

            add("Catalogue status: ${catalogueResult.message}")

            if (catalogueResult.valid) {
                add("Catalogue compliance: VERIFIED BY SELECTION PIPELINE")
            } else {
                add(
                    "Catalogue compliance: NOT VERIFIED. " +
                        "A reference rating is not proof of an approved commercial product."
                )
            }

            add(
                "Final approval requires verified manufacturer data, " +
                    "site conditions, altitude and temperature derating, " +
                    "load-step response, motor starting, harmonic loading, " +
                    "and applicable fault-level studies."
            )
        }

        return Result(
            connectedLoadKw = input.loadKw,
            designLoadKw = designLoadKw,
            requiredKva = requiredKva,
            requiredCurrentA = requiredCurrentA,
            selectedRatingKva = selected?.ratedPowerKva ?: 0.0,
            selectedGenerator = selected,
            alternatives = catalogueResult.alternatives,
            standard = standard,
            preliminarySizingValid =
                requiredKva > EPSILON && ratingAdequate,
            catalogueComplianceVerified = catalogueResult.valid,
            notes = notes
        )
    }

    private fun calculateCurrent(
        kva: Double,
        voltageV: Double,
        currentType: CurrentType
    ): Double {
        require(kva.isFinite() && kva >= 0.0) {
            "Generator capacity must be finite and non-negative."
        }
        require(voltageV.isFinite() && voltageV > EPSILON) {
            "Generator voltage must be finite and greater than zero."
        }

        val denominator = when (currentType) {
            CurrentType.DirectCurrent -> voltageV
            CurrentType.AlternatingSinglePhase -> voltageV
            CurrentType.AlternatingTwoPhase -> 2.0 * voltageV
            CurrentType.AlternatingThreePhase -> sqrt(3.0) * voltageV
        }

        return kva * 1000.0 / denominator
    }

    private fun validate(input: Input) {
        require(input.loadKw.isFinite() && input.loadKw > EPSILON) {
            "Load must be finite and greater than zero kW."
        }

        require(
            input.loadPowerFactor.isFinite() &&
                input.loadPowerFactor > EPSILON &&
                input.loadPowerFactor <= 1.0
        ) {
            "Load power factor must be greater than zero and not exceed 1.0."
        }

        require(
            input.demandFactor.isFinite() &&
                input.demandFactor in 0.0..1.0
        ) {
            "Demand factor must be between 0 and 1."
        }

        require(
            input.diversityFactor.isFinite() &&
                input.diversityFactor in 0.0..1.0
        ) {
            "Diversity multiplier must be between 0 and 1."
        }

        require(
            input.reserveFactor.isFinite() &&
                input.reserveFactor >= 1.0
        ) {
            "Reserve factor must be finite and at least 1.0."
        }

        require(input.voltageV.isFinite() && input.voltageV > EPSILON) {
            "Voltage must be finite and greater than zero."
        }

        require(input.frequencyHz.isFinite() && input.frequencyHz > EPSILON) {
            "Frequency must be finite and greater than zero."
        }

        require(
            input.generatorPowerFactor.isFinite() &&
                input.generatorPowerFactor > EPSILON &&
                input.generatorPowerFactor <= 1.0
        ) {
            "Generator power factor must be greater than zero and not exceed 1.0."
        }
    }
}
