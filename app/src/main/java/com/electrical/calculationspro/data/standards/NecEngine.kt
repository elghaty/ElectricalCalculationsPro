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

    override val standard: Standard =
        standardOverride

    override val codeName: String =
        codeNameOverride

    override val codeRevision: String =
        "NFPA 70 NEC 2026 - Tables 310.16 / 310.15"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double =
        0.0

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {

        /*
         * The existing StandardEngine contract does not expose the
         * NEC conductor temperature column. Therefore this method
         * cannot safely select 60/75/90°C from InsulationType alone.
         *
         * Returning 0.0 explicitly means unavailable rather than
         * silently selecting a temperature column.
         */
        return 0.0
    }

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {

        if (numberOfCircuits < 1) {
            return 0.0
        }

        return NecTables
            .adjustmentFactor(numberOfCircuits)
            ?: 0.0
    }

    override fun conductorAmpacity(
        sectionMm2: Double,
        material: ConductorMaterial,
        insulation: InsulationType,
        installationMethod: InstallationMethod,
        loadedConductors: Int
    ): Double? {

        if (standard != Standard.NEC) {
            return null
        }

        if (sectionMm2 <= 0.0) {
            return null
        }

        if (loadedConductors < 1) {
            return null
        }

        /*
         * NEC Table 310.16 requires selection of a conductor
         * temperature column. InsulationType in the current
         * project model does not encode the NEC conductor type
         * / temperature rating.
         *
         * Returning null is therefore mandatory.
         */
        return null
    }

    override fun standardConductorSections(): List<Double> =
        NecTables.standardMetricEquivalentSections()

    override fun standardBreakerRatings(): List<Double> =
        emptyList()

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        """
        NEC 2026 reference dataset is installed for:
        - Table 310.16 copper and aluminum ampacity.
        - 60°C, 75°C and 90°C conductor columns.
        - Table 310.15(B)(1) ambient correction factors.
        - Table 310.15(C)(1) current-carrying-conductor adjustment factors.
        - AWG/kcmil designations and metric area equivalents.

        NEC ampacity calculation remains unavailable through the
        current StandardEngine conductor API because that API does
        not carry the NEC conductor temperature/type required to
        select the correct Table 310.16 column.

        No IEC value is substituted.
        No assumed NEC temperature column is selected.
        No fabricated breaker rating dataset is returned.
        """.trimIndent()
}
