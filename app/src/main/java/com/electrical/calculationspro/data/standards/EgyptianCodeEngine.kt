package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.IecTables
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

/**
 * Egyptian electrical-code engine.
 *
 * IMPORTANT:
 * Egyptian requirements are intentionally represented as their
 * own engine. The application must not silently equate "Egyptian"
 * with "IEC".
 *
 * Existing IEC-derived numerical tables are used only where the
 * current project already contains corresponding technical data.
 * The implementation flag remains false until the Egyptian dataset
 * has been independently populated and verified.
 */
class EgyptianCodeEngine : StandardEngine {

    override val standard: Standard =
        Standard.EGYPTIAN

    override val codeName: String =
        "Egyptian Electrical Code"

    override val codeRevision: String =
        "Project dataset - controlled implementation"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double {

        return when (
            circuitCategory
                .trim()
                .lowercase()
        ) {

            "lighting" ->
                3.0

            "motor" ->
                5.0

            "power" ->
                5.0

            "final" ->
                5.0

            else ->
                5.0
        }
    }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {

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
            630.0
        )

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        "Egyptian code engine is active, but the verified Egyptian " +
            "tables/datasets are not yet complete. IEC-derived " +
            "values must not be presented as full Egyptian-code compliance."
}
