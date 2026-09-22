package com.electrical.calculationspro.data.standards.iec

/**
 * IEC 60364 engineering reference layer.
 *
 * Scope:
 * - Low-voltage electrical installations
 * - Design principles
 * - Protection
 * - Voltage-drop assessment
 * - Conductor selection interface
 *
 * This file contains the calculation framework and identifiers.
 * It does not reproduce copyrighted IEC tables.
 */
object Iec60364 {

    const val STANDARD = "IEC 60364"

    const val PART_1 = "IEC 60364-1"
    const val PART_4_41 = "IEC 60364-4-41"
    const val PART_4_43 = "IEC 60364-4-43"
    const val PART_5_52 = "IEC 60364-5-52"
    const val PART_5_53 = "IEC 60364-5-53"
    const val PART_6 = "IEC 60364-6"

    const val CURRENT_DESIGN_EDITION = "Current project reference"

    enum class InstallationType {
        BUILDING,
        INDUSTRIAL,
        COMMERCIAL,
        INFRASTRUCTURE,
        OUTDOOR,
        SPECIAL_INSTALLATION
    }

    enum class ProtectionMethod {
        OVERCURRENT,
        SHORT_CIRCUIT,
        EARTH_FAULT,
        RESIDUAL_CURRENT,
        OVERVOLTAGE,
        THERMAL
    }

    data class DesignParameters(
        val voltageV: Double,
        val frequencyHz: Double = 50.0,
        val phases: Int,
        val powerFactor: Double,
        val installationType: InstallationType
    )

    data class VoltageDropRequirement(
        val maximumPercent: Double,
        val application: String
    )

    fun defaultVoltageDropRequirement(
        application: String
    ): VoltageDropRequirement {

        val normalized = application.trim().lowercase()

        return when {
            normalized.contains("lighting") ->
                VoltageDropRequirement(
                    maximumPercent = 3.0,
                    application = application
                )

            normalized.contains("motor") ->
                VoltageDropRequirement(
                    maximumPercent = 5.0,
                    application = application
                )

            else ->
                VoltageDropRequirement(
                    maximumPercent = 4.0,
                    application = application
                )
        }
    }

    fun validateDesign(
        parameters: DesignParameters
    ): List<String> {

        val errors = mutableListOf<String>()

        if (parameters.voltageV <= 0.0) {
            errors += "Voltage must be greater than zero."
        }

        if (parameters.frequencyHz <= 0.0) {
            errors += "Frequency must be greater than zero."
        }

        if (parameters.phases !in 1..3) {
            errors += "Number of phases must be 1, 2, or 3."
        }

        if (parameters.powerFactor <= 0.0 ||
            parameters.powerFactor > 1.0
        ) {
            errors += "Power factor must be greater than 0 and not greater than 1."
        }

        return errors
    }

    fun isLowVoltage(
        voltageV: Double
    ): Boolean =
        voltageV > 0.0 && voltageV <= 1000.0
}
