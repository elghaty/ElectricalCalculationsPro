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

    override val standard: Standard =
        standardOverride

    override val codeName: String =
        codeNameOverride

    override val codeRevision: String =
        "IEC 60364-5-52 controlled engineering dataset"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double =
        when (
            circuitCategory.trim().lowercase()
        ) {
            "lighting",
            "light",
            "lighting circuit" -> 3.0

            "motor",
            "motor circuit",
            "power",
            "power circuit" -> 5.0

            "critical",
            "critical load",
            "critical_load" -> 3.0

            else -> 0.0
        }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {

        if (standard != Standard.IEC) {
            return 0.0
        }

        return when (insulation) {

            InsulationType.PVC ->
                IecTables
                    .ambientCorrectionPvc(
                        ambientTemperatureC
                    )
                    ?: 0.0

            InsulationType.XLPE,
            InsulationType.EPR ->
                IecTables
                    .ambientCorrectionXlpe(
                        ambientTemperatureC
                    )
                    ?: 0.0

            InsulationType.Rubber ->
                0.0
        }
    }

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {

        if (numberOfCircuits < 1) {
            return 0.0
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

        if (standard != Standard.IEC) {
            return null
        }

        if (sectionMm2 <= 0.0) {
            return null
        }

        if (loadedConductors !in 2..3) {
            return null
        }

        return IecTables.getBaseAmpacity(
            section = sectionMm2,
            method =
                IecTables.methodToKey(
                    installationMethod.code
                ),
            loadedConductors = loadedConductors,
            material = material,
            insulation = insulation
        )
    }

    override fun standardConductorSections(): List<Double> =
        IecTables.standardConductorSections()

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
        """
        IEC 60364 engineering engine is active.

        Verified numerical datasets currently integrated:
        - IEC 60364-5-52 PVC two-loaded conductors.
        - IEC 60364-5-52 PVC three-loaded conductors.
        - IEC 60364-5-52 XLPE/EPR two-loaded conductors.
        - IEC 60364-5-52 XLPE/EPR three-loaded conductors.
        - IEC ambient correction factors for PVC.
        - IEC ambient correction factors for XLPE/EPR.
        - IEC reference installation methods A1, A2, B1, B2, C, D1 and D2.

        Arrangement-dependent grouping is not represented by a single
        universal factor. Unsupported arrangements return unavailable data.

        Full IEC professional compliance is not claimed because the
        complete verification scope includes additional installation,
        protection, fault-current, earthing, coordination and equipment
        datasets.
        """.trimIndent()
}
