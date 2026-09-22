package com.electrical.calculationspro.data.standards.egyptian

/**
 * Egyptian Electrical Code reference layer.
 *
 * IMPORTANT:
 * This file intentionally contains engineering rules/metadata and not
 * copyrighted reproductions of the complete Egyptian Code tables.
 *
 * Primary reference family:
 * - Egyptian Code for Design and Execution of Electrical Installations
 *   in Buildings
 * - HBRC volumes D17 / D19 / D18
 *
 * Full code text/tables must be obtained from the official HBRC publication.
 */
object EgyptianTables {

    const val CODE_ID = "EGYPTIAN_ELECTRICAL_BUILDINGS"

    const val DESIGN_VOLUME = "D17"
    const val EXECUTION_VOLUME = "D19"
    const val TESTING_VOLUME = "D18"

    const val DESIGN_YEAR = 2012
    const val EXECUTION_YEAR = 2012
    const val TESTING_YEAR = 2013

    const val DEFAULT_FREQUENCY_HZ = 50.0

    const val DEFAULT_SINGLE_PHASE_VOLTAGE = 230.0
    const val DEFAULT_THREE_PHASE_VOLTAGE = 400.0

    const val DEFAULT_MAX_VOLTAGE_DROP_PERCENT = 4.0

    /**
     * Standard LV system used as the application default.
     *
     * This is a project default, not a universal statement that every
     * Egyptian installation uses the same supply arrangement.
     */
    data class VoltageSystem(
        val name: String,
        val lineToNeutralV: Double,
        val lineToLineV: Double,
        val frequencyHz: Double
    )

    val defaultVoltageSystem = VoltageSystem(
        name = "LV 400/230 V - 50 Hz",
        lineToNeutralV = DEFAULT_SINGLE_PHASE_VOLTAGE,
        lineToLineV = DEFAULT_THREE_PHASE_VOLTAGE,
        frequencyHz = DEFAULT_FREQUENCY_HZ
    )

    /**
     * Maximum design voltage-drop target used by the application.
     *
     * The final allowable value should be selected according to the
     * installation/application and applicable project requirements.
     */
    fun maximumVoltageDropPercent(
        application: EgyptianApplicationType
    ): Double {
        return when (application) {
            EgyptianApplicationType.GENERAL_BUILDING -> 4.0
            EgyptianApplicationType.LIGHTING -> 3.0
            EgyptianApplicationType.MOTOR -> 5.0
            EgyptianApplicationType.CRITICAL_LOAD -> 3.0
        }
    }

    enum class EgyptianApplicationType {
        GENERAL_BUILDING,
        LIGHTING,
        MOTOR,
        CRITICAL_LOAD
    }

    /**
     * Basic design checks.
     */
    fun validateVoltage(
        voltage: Double
    ): List<String> {
        val notes = mutableListOf<String>()

        if (voltage <= 0.0) {
            notes += "Voltage must be greater than zero."
        }

        if (voltage > 1500.0) {
            notes += "This LV building-installation engine is intended for LV applications."
        }

        return notes
    }

    fun validatePowerFactor(
        powerFactor: Double
    ): List<String> {
        val notes = mutableListOf<String>()

        if (powerFactor <= 0.0 || powerFactor > 1.0) {
            notes += "Power factor must be greater than 0 and not greater than 1."
        }

        return notes
    }

    fun validateLength(
        lengthMeters: Double
    ): List<String> {
        return if (lengthMeters <= 0.0) {
            listOf("Cable length must be greater than zero.")
        } else {
            emptyList()
        }
    }
}
