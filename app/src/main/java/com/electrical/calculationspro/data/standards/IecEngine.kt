package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.IecTables
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

class IecEngine(
    private val standardOverride: Standard = Standard.IEC,
    private val codeNameOverride: String = "IEC 60364"
) : StandardEngine {

    override val standard: Standard = standardOverride

    override val codeName: String = codeNameOverride

    override val codeRevision: String =
        "IEC 60364 - controlled project dataset"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double =
        when (circuitCategory.trim().lowercase()) {
            "lighting",
            "light" -> 3.0

            "motor",
            "motor circuit" -> 5.0

            "power" -> 5.0

            else -> 5.0
        }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double =
        when (insulation) {
            InsulationType.PVC,
            InsulationType.Rubber ->
                IecTables.ambientCorrectionPvc(
                    ambientTemperatureC
                )

            InsulationType.XLPE,
            InsulationType.EPR ->
                IecTables.ambientCorrectionXlpe(
                    ambientTemperatureC
                )
        }

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {
        require(numberOfCircuits >= 1) {
            "Number of circuits must be at least 1."
        }

        return IecTables.groupingFactor(
            numberOfCircuits
        )
    }

    override fun conductorAmpacity(
        sectionMm2: Double,
        material: ConductorMaterial,
        insulation: InsulationType,
        installationMethod: InstallationMethod,
        loadedConductors: Int
    ): Double? {

        /*
         * CEI must not inherit IEC data merely because the implementation
         * class is reusable. The factory controls which standards may use
         * this engine.
         */
        if (standard != Standard.IEC) {
            return null
        }

        return IecTables.getBaseAmpacity(
            section = sectionMm2,
            method = IecTables.methodToKey(
                installationMethod.code
            ),
            loadedConductors = loadedConductors,
            material = material,
            insulation = insulation
        )
    }

    override fun standardConductorSections(): List<Double> =
        listOf(
            1.5,
            2.5,
            4.0,
            6.0,
            10.0,
            16.0,
            25.0,
            35.0,
            50.0,
            70.0,
            95.0,
            120.0,
            150.0,
            185.0,
            240.0,
            300.0
        )

    override fun standardBreakerRatings(): List<Double> =
        listOf(
            6.0,
            10.0,
            16.0,
            20.0,
            25.0,
            32.0,
            40.0,
            50.0,
            63.0,
            80.0,
            100.0,
            125.0,
            160.0,
            200.0,
            250.0,
            315.0,
            400.0,
            500.0,
            630.0,
            800.0,
            1000.0,
            1250.0,
            1600.0,
            2000.0,
            2500.0,
            3200.0,
            4000.0,
            5000.0,
            6300.0
        )

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        if (standard == Standard.IEC) {
            "IEC engine is active with the project's controlled IEC " +
                "dataset. Ampacity data is available only for the " +
                "explicitly populated combinations. Missing combinations " +
                "are not replaced by approximations. Full IEC compliance " +
                "is not claimed."
        } else {
            "This engine is not an implementation of ${standard.shortName}. " +
                "A dedicated verified dataset is required."
        }
}
