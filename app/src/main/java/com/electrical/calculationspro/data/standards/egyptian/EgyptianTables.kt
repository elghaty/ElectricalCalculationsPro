package com.electrical.calculationspro.data.standards.egyptian

/**
 * Controlled Egyptian-code dataset metadata.
 *
 * This file does not reproduce copyrighted code tables.
 * It provides the verified project-level defaults and code references
 * consumed by the existing Egyptian engineering engine.
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

    const val SOURCE_ORGANIZATION =
        "Housing and Building Research Center / Egyptian Code"

    data class VoltageSystem(
        val name: String,
        val lineToNeutralV: Double,
        val lineToLineV: Double,
        val frequencyHz: Double
    )

    val defaultVoltageSystem =
        VoltageSystem(
            name = "LV 400/230 V - 50 Hz",
            lineToNeutralV = DEFAULT_SINGLE_PHASE_VOLTAGE,
            lineToLineV = DEFAULT_THREE_PHASE_VOLTAGE,
            frequencyHz = DEFAULT_FREQUENCY_HZ
        )

    enum class EgyptianApplicationType {
        GENERAL_BUILDING,
        LIGHTING,
        MOTOR,
        CRITICAL_LOAD
    }

    fun maximumVoltageDropPercent(
        application: EgyptianApplicationType
    ): Double =
        when (application) {
            EgyptianApplicationType.GENERAL_BUILDING -> 4.0
            EgyptianApplicationType.LIGHTING -> 3.0
            EgyptianApplicationType.MOTOR -> 5.0
            EgyptianApplicationType.CRITICAL_LOAD -> 3.0
        }

    fun validateVoltage(
        voltage: Double
    ): List<String> =
        buildList {
            if (voltage <= 0.0) {
                add("Voltage must be greater than zero.")
            }

            if (voltage > 1500.0) {
                add(
                    "Voltage is outside the LV building-installation scope " +
                        "of this dataset."
                )
            }
        }

    fun validatePowerFactor(
        powerFactor: Double
    ): List<String> =
        buildList {
            if (powerFactor <= 0.0 || powerFactor > 1.0) {
                add(
                    "Power factor must be greater than 0 and not greater than 1."
                )
            }
        }

    fun validateLength(
        lengthMeters: Double
    ): List<String> =
        if (lengthMeters <= 0.0) {
            listOf("Cable length must be greater than zero.")
        } else {
            emptyList()
        }
}
