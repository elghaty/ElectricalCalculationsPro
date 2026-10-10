package com.electrical.calculationspro.data.calculators

import com.electrical.calculationspro.data.TransformerSizingResult
import com.electrical.calculationspro.data.catalog.TransformerCatalog
import kotlin.math.sqrt

object TransformerSizingCalculator {

    private const val EPSILON = 1.0e-9

    private fun ratings(): List<Double> =
        TransformerCatalog.standardRatingsKva()
            .filter { it.isFinite() && it > EPSILON }
            .distinct()
            .sorted()

    fun requiredKva(
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double = 1.0
    ): Double {
        require(loadKw.isFinite() && loadKw >= 0.0) {
            "Load must be finite and non-negative."
        }
        require(
            powerFactor.isFinite() &&
                powerFactor > EPSILON &&
                powerFactor <= 1.0
        ) {
            "Power factor must be greater than zero and not greater than 1."
        }
        require(growthFactor.isFinite() && growthFactor >= 1.0) {
            "Growth factor must be finite and at least 1.0."
        }

        val result = loadKw / powerFactor * growthFactor
        require(result.isFinite()) {
            "Calculated transformer demand exceeds the supported numeric range."
        }
        return result
    }

    fun selectStandardRating(requiredKva: Double): Double {
        require(requiredKva.isFinite() && requiredKva >= 0.0) {
            "Required transformer capacity must be finite and non-negative."
        }

        if (requiredKva <= EPSILON) return 0.0

        return ratings().firstOrNull { it + EPSILON >= requiredKva } ?: 0.0
    }

    fun fullLoadCurrent(
        kva: Double,
        voltage: Double,
        phases: Int = 3
    ): Double {
        require(kva.isFinite() && kva >= 0.0) {
            "Transformer capacity must be finite and non-negative."
        }
        require(voltage.isFinite() && voltage > EPSILON) {
            "Voltage must be finite and greater than zero."
        }

        val current = when (phases) {
            1 -> kva * 1000.0 / voltage
            3 -> kva * 1000.0 / (sqrt(3.0) * voltage)
            else -> throw IllegalArgumentException(
                "Only single-phase and three-phase systems are supported."
            )
        }

        require(current.isFinite()) {
            "Calculated full-load current exceeds the supported numeric range."
        }
        return current
    }

    /**
     * Transformer-terminal fault current based on transformer impedance.
     * This is not a complete network short-circuit calculation.
     */
    fun shortCircuitCurrentFromImpedance(
        kva: Double,
        voltage: Double,
        impedancePercent: Double,
        phases: Int = 3
    ): Double {
        require(kva.isFinite() && kva > EPSILON) {
            "Transformer capacity must be finite and greater than zero."
        }
        require(voltage.isFinite() && voltage > EPSILON) {
            "Voltage must be finite and greater than zero."
        }
        require(
            impedancePercent.isFinite() && impedancePercent > EPSILON
        ) {
            "Transformer impedance must be finite and greater than zero."
        }

        val result = fullLoadCurrent(kva, voltage, phases) *
            100.0 / impedancePercent

        require(result.isFinite()) {
            "Calculated short-circuit current exceeds the supported numeric range."
        }
        return result
    }

    fun calculate(
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double = 1.0,
        voltage: Double,
        phases: Int = 3,
        impedancePercent: Double = 6.0
    ): TransformerSizingResult {
        require(voltage.isFinite() && voltage > EPSILON) {
            "Voltage must be finite and greater than zero."
        }
        require(
            impedancePercent.isFinite() && impedancePercent > EPSILON
        ) {
            "Transformer impedance must be finite and greater than zero."
        }

        val required = requiredKva(
            loadKw = loadKw,
            powerFactor = powerFactor,
            growthFactor = growthFactor
        )

        val availableRatings = ratings()

        if (availableRatings.isEmpty()) {
            return failure(
                loadKw = loadKw,
                powerFactor = powerFactor,
                growthFactor = growthFactor,
                requiredKva = required,
                impedancePercent = impedancePercent,
                reason = "Transformer catalog is empty or contains no valid standard ratings."
            )
        }

        if (required <= EPSILON) {
            return failure(
                loadKw = loadKw,
                powerFactor = powerFactor,
                growthFactor = growthFactor,
                requiredKva = required,
                impedancePercent = impedancePercent,
                reason = "Calculated demand is zero; no transformer rating was selected."
            )
        }

        val selected = availableRatings.firstOrNull {
            it + EPSILON >= required
        }

        if (selected == null) {
            return failure(
                loadKw = loadKw,
                powerFactor = powerFactor,
                growthFactor = growthFactor,
                requiredKva = required,
                impedancePercent = impedancePercent,
                reason = "Required capacity exceeds the maximum available catalog rating of %.0f kVA."
                    .format(availableRatings.last())
            )
        }

        val current = fullLoadCurrent(
            kva = selected,
            voltage = voltage,
            phases = phases
        )

        val shortCircuitKA = shortCircuitCurrentFromImpedance(
            kva = selected,
            voltage = voltage,
            impedancePercent = impedancePercent,
            phases = phases
        ) / 1000.0

        return TransformerSizingResult(
            loadKw = loadKw,
            powerFactor = powerFactor,
            growthFactor = growthFactor,
            requiredKva = required,
            selectedKva = selected,
            fullLoadCurrentA = current,
            impedancePercent = impedancePercent,
            shortCircuitCurrentKA = shortCircuitKA,
            valid = true,
            notes = listOf(
                "PRELIMINARY TRANSFORMER SIZING",
                "Required capacity = %.2f kVA".format(required),
                "Selected catalog rating = %.0f kVA".format(selected),
                "Full-load current = %.2f A".format(current),
                "Impedance used = %.2f %%".format(impedancePercent),
                "Estimated transformer-terminal fault current = %.3f kA"
                    .format(shortCircuitKA),
                "The selected rating is a generic engineering reference, not manufacturer approval.",
                "Verify actual impedance, voltage ratio, vector group, frequency, losses, temperature rise and protection coordination before final approval."
            )
        )
    }

    fun standardRatings(): List<Double> = ratings()

    private fun failure(
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double,
        requiredKva: Double,
        impedancePercent: Double,
        reason: String
    ) = TransformerSizingResult(
        loadKw = loadKw,
        powerFactor = powerFactor,
        growthFactor = growthFactor,
        requiredKva = requiredKva,
        selectedKva = 0.0,
        fullLoadCurrentA = 0.0,
        impedancePercent = impedancePercent,
        shortCircuitCurrentKA = 0.0,
        valid = false,
        notes = listOf(
            "TRANSFORMER SIZING NOT VERIFIED",
            reason,
            "No valid transformer selection was returned.",
            "Check input values and catalog coverage."
        )
    )
}
