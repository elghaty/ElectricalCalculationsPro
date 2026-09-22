package com.electrical.calculationspro.data.standards.iec

import kotlin.math.sqrt

/**
 * IEC 60909 short-circuit calculation framework.
 *
 * This layer is kept independent from UI and SLD.
 */
object Iec60909 {

    const val STANDARD = "IEC 60909"

    data class Source(
        val voltageV: Double,
        val shortCircuitMva: Double,
        val voltageFactor: Double = 1.0
    )

    data class Impedance(
        val resistanceOhm: Double,
        val reactanceOhm: Double
    ) {
        val magnitudeOhm: Double
            get() = sqrt(
                resistanceOhm * resistanceOhm +
                    reactanceOhm * reactanceOhm
            )
    }

    data class ShortCircuitResult(
        val initialCurrentKA: Double,
        val shortCircuitMva: Double,
        val impedanceOhm: Double,
        val resistanceOhm: Double,
        val reactanceOhm: Double,
        val valid: Boolean,
        val notes: List<String>
    )

    fun sourceImpedance(
        voltageV: Double,
        shortCircuitMva: Double
    ): Impedance {

        require(voltageV > 0.0)
        require(shortCircuitMva > 0.0)

        val z =
            voltageV * voltageV /
                (shortCircuitMva * 1_000_000.0)

        return Impedance(
            resistanceOhm = 0.0,
            reactanceOhm = z
        )
    }

    fun initialSymmetricalCurrent(
        voltageV: Double,
        impedanceOhm: Double,
        voltageFactor: Double = 1.0
    ): Double {

        require(voltageV > 0.0)
        require(impedanceOhm > 0.0)
        require(voltageFactor > 0.0)

        return voltageFactor *
            voltageV /
            (sqrt(3.0) * impedanceOhm)
    }

    fun calculate(
        voltageV: Double,
        resistanceOhm: Double,
        reactanceOhm: Double,
        voltageFactor: Double = 1.0
    ): ShortCircuitResult {

        val notes = mutableListOf<String>()

        if (voltageV <= 0.0) {
            return invalid("Voltage must be greater than zero.")
        }

        if (voltageFactor <= 0.0) {
            return invalid("Voltage factor must be greater than zero.")
        }

        val z =
            sqrt(
                resistanceOhm * resistanceOhm +
                    reactanceOhm * reactanceOhm
            )

        if (z <= 0.0) {
            return invalid("System impedance must be greater than zero.")
        }

        val currentA =
            initialSymmetricalCurrent(
                voltageV = voltageV,
                impedanceOhm = z,
                voltageFactor = voltageFactor
            )

        val currentKA = currentA / 1000.0

        val mva =
            sqrt(3.0) *
                voltageV *
                currentA /
                1_000_000.0

        notes +=
            "Initial symmetrical short-circuit current calculated using the IEC 60909 framework."

        notes +=
            "Complete IEC 60909 studies require the complete network sequence/source data and applicable correction factors."

        return ShortCircuitResult(
            initialCurrentKA = currentKA,
            shortCircuitMva = mva,
            impedanceOhm = z,
            resistanceOhm = resistanceOhm,
            reactanceOhm = reactanceOhm,
            valid = true,
            notes = notes
        )
    }

    private fun invalid(
        message: String
    ): ShortCircuitResult =
        ShortCircuitResult(
            initialCurrentKA = 0.0,
            shortCircuitMva = 0.0,
            impedanceOhm = 0.0,
            resistanceOhm = 0.0,
            reactanceOhm = 0.0,
            valid = false,
            notes = listOf(message)
        )
}
