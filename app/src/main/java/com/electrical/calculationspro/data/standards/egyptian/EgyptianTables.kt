package com.electrical.calculationspro.data.standards.egyptian

/**
 * Egyptian engineering-reference metadata.
 *
 * Project inputs such as voltage, frequency and permissible voltage
 * drop are NOT represented as hidden defaults.
 */
object EgyptianTables {

    const val CODE_ID =
        "EGYPTIAN_ELECTRICAL_BUILDINGS"

    const val DESIGN_VOLUME =
        "D17"

    const val EXECUTION_VOLUME =
        "D19"

    const val TESTING_VOLUME =
        "D18"

    const val DESIGN_YEAR =
        2012

    const val EXECUTION_YEAR =
        2012

    const val TESTING_YEAR =
        2013

    const val SOURCE_ORGANIZATION =
        "Housing and Building Research Center / Egyptian Code"

    const val EOS_STANDARD_1576_1 =
        "Egyptian Standard 1576-1/2019"

    const val EOS_STANDARD_1576_7 =
        "Egyptian Standard 1576-7/2019"

    const val EOS_STANDARD_1576_8 =
        "Egyptian Standard 1576-8/1992 reference"

    const val EOS_STANDARD_1576_10 =
        "Egyptian Standard 1576-10/2019"

    const val UTILITY_CONNECTION_GUIDE =
        "Egyptian electricity supply connection guide"

    data class VoltageSystem(
        val name: String,
        val lineToNeutralV: Double,
        val lineToLineV: Double,
        val frequencyHz: Double
    )

    enum class EgyptianApplicationType {
        GENERAL_BUILDING,
        LIGHTING,
        MOTOR,
        CRITICAL_LOAD
    }

    /**
     * There is intentionally no default voltage system.
     *
     * The project must supply the actual design voltage/frequency.
     */
    fun validateVoltage(
        voltage: Double
    ): List<String> =
        buildList {

            if (voltage <= 0.0) {
                add(
                    "Voltage must be greater than zero."
                )
            }

            if (voltage > 1500.0) {
                add(
                    "Voltage is outside the LV building-installation " +
                        "scope of this dataset."
                )
            }
        }

    fun validateFrequency(
        frequencyHz: Double
    ): List<String> =
        if (frequencyHz <= 0.0) {
            listOf(
                "Frequency must be greater than zero."
            )
        } else {
            emptyList()
        }

    fun validatePowerFactor(
        powerFactor: Double
    ): List<String> =
        if (
            powerFactor <= 0.0 ||
            powerFactor > 1.0
        ) {
            listOf(
                "Power factor must be greater than 0 and not greater than 1."
            )
        } else {
            emptyList()
        }

    fun validateLength(
        lengthMeters: Double
    ): List<String> =
        if (lengthMeters <= 0.0) {
            listOf(
                "Cable length must be greater than zero."
            )
        } else {
            emptyList()
        }
}
