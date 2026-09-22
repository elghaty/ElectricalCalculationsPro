package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.egyptian.EgyptianCableTables
import com.electrical.calculationspro.data.standards.egyptian.EgyptianInstallationRules
import com.electrical.calculationspro.data.standards.egyptian.EgyptianTables
import kotlin.math.abs

/**
 * Egyptian Electrical Code engine.
 *
 * This engine is intentionally isolated from IEC and NEC.
 *
 * IMPORTANT:
 * The Egyptian cable ampacity dataset is not considered complete until
 * verified/current Egyptian Code data is populated.
 */
class EgyptianCodeEngine : StandardEngine {

    override val standard: Standard =
        Standard.EGYPTIAN

    override val codeName: String =
        "Egyptian Electrical Code"

    override val codeRevision: String =
        "HBRC D17 / D19 / D18 - controlled implementation"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double {

        val application =
            when (
                circuitCategory
                    .trim()
                    .lowercase()
            ) {

                "lighting",
                "light",
                "lighting circuit" ->
                    EgyptianTables.EgyptianApplicationType.LIGHTING

                "motor",
                "motor circuit" ->
                    EgyptianTables.EgyptianApplicationType.MOTOR

                "critical",
                "critical load",
                "critical_load" ->
                    EgyptianTables.EgyptianApplicationType.CRITICAL_LOAD

                else ->
                    EgyptianTables.EgyptianApplicationType.GENERAL_BUILDING
            }

        return EgyptianTables
            .maximumVoltageDropPercent(application)
    }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {

        /*
         * The current EgyptianCableTables does not contain a verified
         * Egyptian temperature-correction table.
         *
         * Therefore no IEC correction factor is substituted here.
         *
         * Returning 1.0 keeps the calculation deterministic while the
         * implementation status remains incomplete.
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
         * The current Egyptian dataset does not contain a verified
         * grouping-factor table.
         *
         * Do NOT substitute IEC values.
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
                request =
                    EgyptianCableTables.CableAmpacityRequest(
                        sectionMm2 = sectionMm2,
                        conductor = material,
                        insulation = insulation,
                        installationMethod =
                            installationMethod.code,
                        ambientTemperatureC = 30.0,
                        loadedConductors = loadedConductors
                    )
            )

        return if (result.available) {
            result.ampacityA
        } else {
            null
        }
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
        "Egyptian Electrical Code engine is active, but verified/current " +
            "Egyptian cable ampacity, temperature correction and grouping " +
            "datasets are not yet populated. IEC tables are NOT substituted."

    /**
     * Exposes the currently supported Egyptian installation methods.
     */
    fun installationMethods(): List<EgyptianInstallationRules.InstallationMethod> =
        EgyptianInstallationRules.methods

    /**
     * Validates an Egyptian installation method.
     */
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

    /**
     * Verifies that a section is one of the standard Egyptian-engine
     * sections currently supported by the application.
     */
    fun isStandardSection(
        sectionMm2: Double
    ): Boolean =
        EgyptianCableTables
            .standardSections()
            .any {
                abs(it - sectionMm2) < 0.0001
            }
}
