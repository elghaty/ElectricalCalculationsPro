package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.IecTables
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

/**
 * NEC engine.
 *
 * This class intentionally does NOT claim that IEC ampacity tables
 * constitute NEC compliance.
 *
 * It provides the application contract now, while the dedicated
 * NEC Article/Table datasets are added separately.
 */
class NecEngine(
    private val standardOverride: Standard =
        Standard.NEC,

    private val codeNameOverride: String =
        "NFPA 70 - National Electrical Code"
) : StandardEngine {

    override val standard: Standard =
        standardOverride

    override val codeName: String =
        codeNameOverride

    override val codeRevision: String =
        "Controlled implementation"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double {

        /*
         * NEC informational notes commonly use voltage-drop
         * recommendations rather than treating them as a universal
         * mandatory branch-circuit limit.
         *
         * The final NEC engine will distinguish mandatory rules
         * from informational recommendations.
         */
        return when (
            circuitCategory
                .trim()
                .lowercase()
        ) {

            "feeder" ->
                3.0

            "branch" ->
                3.0

            "total" ->
                5.0

            else ->
                5.0
        }
    }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {

        /*
         * Placeholder only until NEC 310 correction tables are
         * populated from the controlled NEC dataset.
         *
         * We intentionally return the existing project's value
         * but mark the engine as incomplete.
         */
        return when (insulation) {

            InsulationType.XLPE,
            InsulationType.EPR ->
                IecTables.ambientCorrectionXlpe(
                    ambientTemperatureC
                )

            InsulationType.PVC,
            InsulationType.Rubber ->
                IecTables.ambientCorrectionPvc(
                    ambientTemperatureC
                )
        }
    }

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {

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
         * NEVER label this result NEC compliant.
         *
         * NEC Article 310 tables will replace this delegation.
         */
        return IecTables.getBaseAmpacity(
            section = sectionMm2,
            method =
                IecTables.methodToKey(
                    installationMethod.code
                ),
            loadedConductors =
                loadedConductors,
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
            15.0,
            20.0,
            25.0,
            30.0,
            35.0,
            40.0,
            45.0,
            50.0,
            60.0,
            70.0,
            80.0,
            90.0,
            100.0,
            110.0,
            125.0,
            150.0,
            175.0,
            200.0,
            225.0,
            250.0,
            300.0,
            350.0,
            400.0,
            450.0,
            500.0,
            600.0,
            700.0,
            800.0,
            1000.0,
            1200.0,
            1600.0,
            2000.0,
            2500.0,
            3000.0,
            4000.0,
            5000.0,
            6000.0
        )

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        "NEC engine is not yet compliance-complete. " +
            "Dedicated NEC Articles 210, 215, 220, 240, 250 and 310 " +
            "datasets must be populated before NEC compliance is declared."
}
