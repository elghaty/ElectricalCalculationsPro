package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.egyptian.EgyptianCableTables
import com.electrical.calculationspro.data.standards.egyptian.EgyptianInstallationRules
import com.electrical.calculationspro.data.standards.egyptian.EgyptianTables

/**
 * Egyptian Electrical Code engine.
 *
 * Egyptian data is deliberately isolated from IEC and NEC.
 */
class EgyptianCodeEngine : StandardEngine {

    override val standard: Standard =
        Standard.EGYPTIAN

    override val codeName: String =
        "Egyptian Electrical Code"

    override val codeRevision: String =
        EgyptianTables.codeReference

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double =
        EgyptianTables.maximumVoltageDropPercent(
            circuitCategory
        )

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {

        return EgyptianCableTables
            .ambientTemperatureFactor(
                insulation = insulation,
                ambientTemperatureC =
                    ambientTemperatureC
            )
    }

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {

        return EgyptianCableTables
            .groupingFactor(
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

        val method =
            EgyptianInstallationRules
                .resolveMethod(
                    installationMethod.code
                )

        return EgyptianCableTables
            .ampacity(
                sectionMm2 = sectionMm2,
                material = material,
                insulation = insulation,
                installationMethod = method,
                loadedConductors = loadedConductors
            )
    }

    override fun standardConductorSections(): List<Double> =
        EgyptianCableTables
            .standardConductorSections()

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
        EgyptianCableTables.isDatasetComplete

    override fun implementationStatus(): String =
        if (isFullyImplemented()) {
            "Verified Egyptian electrical-code dataset is active."
        } else {
            "Egyptian code engine is active, but the verified Egyptian " +
                "cable/installation datasets are not complete. " +
                "The application will not label incomplete data as full compliance."
        }
}
