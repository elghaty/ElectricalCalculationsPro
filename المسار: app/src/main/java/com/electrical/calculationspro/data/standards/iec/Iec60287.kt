package com.electrical.calculationspro.data.standards.iec

import kotlin.math.sqrt

/**
 * IEC 60287 cable current-rating calculation framework.
 *
 * IEC 60287 is used for cable current-carrying-capacity calculations.
 *
 * The complete cable construction constants and manufacturer-specific
 * parameters must come from the applicable cable standard/manufacturer
 * data. They are therefore represented explicitly rather than invented.
 */
object Iec60287 {

    const val STANDARD = "IEC 60287"

    data class CableParameters(
        val conductorAreaMm2: Double,
        val conductorResistanceOhmPerKm: Double,
        val acResistanceOhmPerKm: Double,
        val dielectricLossWPerM: Double = 0.0,
        val installationThermalResistance: Double,
        val ambientTemperatureC: Double,
        val conductorMaximumTemperatureC: Double,
        val voltageV: Double,
        val frequencyHz: Double = 50.0
    )

    data class RatingResult(
        val currentA: Double,
        val valid: Boolean,
        val notes: List<String>
    )

    /**
     * Thermal-limit approximation used only as an engineering framework.
     *
     * It must not be represented as a replacement for a complete
     * IEC 60287 cable calculation without all cable construction data.
     */
    fun thermalCurrentApproximation(
        parameters: CableParameters
    ): RatingResult {

        val notes = mutableListOf<String>()

        if (parameters.conductorAreaMm2 <= 0.0) {
            notes += "Conductor area must be greater than zero."
        }

        if (parameters.conductorResistanceOhmPerKm <= 0.0) {
            notes += "Conductor resistance must be greater than zero."
        }

        if (parameters.installationThermalResistance <= 0.0) {
            notes += "Installation thermal resistance must be greater than zero."
        }

        if (parameters.conductorMaximumTemperatureC <=
            parameters.ambientTemperatureC
        ) {
            notes +=
                "Maximum conductor temperature must be above ambient temperature."
        }

        if (notes.isNotEmpty()) {
            return RatingResult(
                currentA = 0.0,
                valid = false,
                notes = notes
            )
        }

        val temperatureDifference =
            parameters.conductorMaximumTemperatureC -
                parameters.ambientTemperatureC

        /*
         * This is deliberately a framework-level thermal approximation.
         * The complete IEC 60287 formulation requires cable-specific
         * losses, thermal resistances, screen/sheath effects and installation
         * construction parameters.
         */
        val thermalPowerLimit =
            temperatureDifference /
                parameters.installationThermalResistance

        val resistanceOhmPerMeter =
            parameters.acResistanceOhmPerKm / 1000.0

        if (resistanceOhmPerMeter <= 0.0) {
            return RatingResult(
                currentA = 0.0,
                valid = false,
                notes = listOf(
                    "AC conductor resistance must be greater than zero."
                )
            )
        }

        val current =
            sqrt(
                thermalPowerLimit /
                    resistanceOhmPerMeter
            )

        notes +=
            "Result is an engineering approximation. Complete IEC 60287 cable data is required for final cable rating."

        return RatingResult(
            currentA = current,
            valid = current > 0.0,
            notes = notes
        )
    }

    fun conductorLoss(
        currentA: Double,
        resistanceOhmPerKm: Double,
        lengthKm: Double
    ): Double {

        require(currentA >= 0.0)
        require(resistanceOhmPerKm >= 0.0)
        require(lengthKm >= 0.0)

        return currentA *
            currentA *
            resistanceOhmPerKm *
            lengthKm
    }

    fun validate(
        parameters: CableParameters
    ): List<String> {

        val errors = mutableListOf<String>()

        if (parameters.conductorAreaMm2 <= 0.0) {
            errors += "Conductor area must be greater than zero."
        }

        if (parameters.conductorResistanceOhmPerKm <= 0.0) {
            errors += "Conductor resistance must be greater than zero."
        }

        if (parameters.acResistanceOhmPerKm <= 0.0) {
            errors += "AC resistance must be greater than zero."
        }

        if (parameters.installationThermalResistance <= 0.0) {
            errors += "Thermal resistance must be greater than zero."
        }

        if (parameters.voltageV <= 0.0) {
            errors += "Voltage must be greater than zero."
        }

        if (parameters.frequencyHz <= 0.0) {
            errors += "Frequency must be greater than zero."
        }

        return errors
    }
}
