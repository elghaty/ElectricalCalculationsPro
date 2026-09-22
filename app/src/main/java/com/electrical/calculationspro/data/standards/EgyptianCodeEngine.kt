package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.egyptian.EgyptianCableTables
import com.electrical.calculationspro.data.standards.egyptian.EgyptianInstallationRules
import com.electrical.calculationspro.data.standards.egyptian.EgyptianTables
import kotlin.math.abs

class EgyptianCodeEngine : StandardEngine {

    override val standard: Standard =
        Standard.EGYPTIAN

    override val codeName: String =
        "Egyptian Electrical Code"

    override val codeRevision: String =
        "HBRC D17 / D19 / D18 - controlled implementation"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double =
        when (circuitCategory.trim().lowercase()) {
            "lighting",
            "light",
            "lighting circuit" ->
                EgyptianTables.maximumVoltageDropPercent(
                    EgyptianTables.EgyptianApplicationType.LIGHTING
                )

            "motor",
            "motor circuit" ->
                EgyptianTables.maximumVoltageDropPercent(
                    EgyptianTables.EgyptianApplicationType.MOTOR
                )

            "critical",
            "critical load",
            "critical_load" ->
                EgyptianTables.maximumVoltageDropPercent(
                    EgyptianTables.EgyptianApplicationType.CRITICAL_LOAD
                )

            else ->
                EgyptianTables.maximumVoltageDropPercent(
                    EgyptianTables.EgyptianApplicationType.GENERAL_BUILDING
                )
        }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {
        /*
         * Verified Egyptian correction-factor dataset is not populated.
         * Do not substitute IEC values.
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
         * Verified Egyptian grouping-factor dataset is not populated.
         * Do not substitute IEC values.
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

        val result =
            EgyptianCableTables.ampacity(
                EgyptianCableTables.CableAmpacityRequest(
                    sectionMm2 = sectionMm2,
                    conductor = material,
                    insulation = insulation,
                    installationMethod = installationMethod.code,
                    ambientTemperatureC = 30.0,
                    loadedConductors = loadedConductors
                )
            )

        return result.ampacityA
            ?.takeIf { result.available }
    }

    override fun standardConductorSections(): List<Double> =
        EgyptianCableTables
            .standardSections()
            .sorted()

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
        "Egyptian Electrical Code engine is active. Verified/current " +
            "Egyptian cable ampacity, temperature-correction and grouping " +
            "datasets are not yet completely populated. IEC data is never " +
            "used as an Egyptian-code substitute."

    fun installationMethods():
        List<EgyptianInstallationRules.InstallationMethod> =
        EgyptianInstallationRules.methods

    fun validateInstallation(
        method: EgyptianInstallationRules.InstallationMethod,
        ambientTemperatureC: Double,
        circuits: Int
    ): List<String> =
        EgyptianInstallationRules.validate(
            method = method,
            ambientTemperatureC = ambientTemperatureC,
            circuits = circuits
        )

    fun isStandardSection(
        sectionMm2: Double
    ): Boolean =
        EgyptianCableTables
            .standardSections()
            .any {
                abs(it - sectionMm2) < 0.0001
            }
}
