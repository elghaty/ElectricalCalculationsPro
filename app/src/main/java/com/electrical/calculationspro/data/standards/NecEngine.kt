package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

class NecEngine(
    private val standardOverride: Standard = Standard.NEC,
    private val codeNameOverride: String =
        "NFPA 70 - National Electrical Code"
) : StandardEngine {

    override val standard: Standard = standardOverride

    override val codeName: String = codeNameOverride

    override val codeRevision: String =
        "Controlled NEC dataset - incomplete"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double =
        when (circuitCategory.trim().lowercase()) {
            "feeder",
            "branch" -> 3.0

            "total" -> 5.0

            else -> 5.0
        }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {
        /*
         * NEC temperature correction data is not currently populated.
         * Never substitute IEC correction factors.
         */
        return 1.0
    }

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {
        require(numberOfCircuits >= 1) {
            "Number of circuits must be at least 1."
        }

        /*
         * NEC adjustment-factor dataset is not currently populated.
         */
        return 1.0
    }

    override fun conductorAmpacity(
        sectionMm2: Double,
        material: ConductorMaterial,
        insulation: InsulationType,
        installationMethod: InstallationMethod,
        loadedConductors: Int
    ): Double? {
        /*
         * No IEC fallback.
         */
        return null
    }

    override fun standardConductorSections(): List<Double> =
        emptyList()

    override fun standardBreakerRatings(): List<Double> =
        emptyList()

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        if (standard == Standard.NEC) {
            "NEC engine is registered but the verified NEC Article 310 " +
                "ampacity, adjustment-factor and device-rating datasets " +
                "are not populated. No IEC data is substituted."
        } else {
            "This engine is not an implementation of ${standard.shortName}. " +
                "A dedicated verified dataset is required."
        }
}
