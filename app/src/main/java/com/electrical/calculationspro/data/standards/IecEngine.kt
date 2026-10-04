package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.IecTables
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

/**
 * IEC 60364 engineering standard engine.
 *
 * Design rules:
 *
 * 1. IEC data is used only when the selected standard is actually IEC.
 * 2. No missing IEC dataset is replaced by data from another standard.
 * 3. No insulation type is silently mapped to another insulation type.
 * 4. A factor of 0.0 means that the requested engineering dataset
 *    is unavailable and must not be interpreted as a valid correction
 *    factor of zero.
 * 5. Full IEC compliance is NOT claimed until the controlled dataset
 *    has been completely verified.
 */
class IecEngine(
    private val standardOverride: Standard = Standard.IEC,
    private val codeNameOverride: String = "IEC 60364"
) : StandardEngine {

    override val standard: Standard =
        standardOverride

    override val codeName: String =
        codeNameOverride

    override val codeRevision: String =
        "IEC 60364 - controlled project dataset"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double {

        return when (
            circuitCategory
                .trim()
                .lowercase()
        ) {

            "lighting",
            "light",
            "lighting circuit" ->
                3.0

            "motor",
            "motor circuit",
            "power",
            "power circuit" ->
                5.0

            "critical",
            "critical load",
            "critical_load" ->
                3.0

            else ->
                5.0
        }
    }

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double {

        /*
         * Validate the input before consulting the dataset.
         *
         * IecTables intentionally owns the actual correction curves.
         */
        require(
            ambientTemperatureC >= -50.0 &&
                ambientTemperatureC <= 100.0
        ) {
            "Ambient temperature is outside the supported IEC input range."
        }

        /*
         * IMPORTANT:
         *
         * The currently controlled IecTables dataset contains explicit
         * PVC and XLPE correction data.
         *
         * Rubber and EPR must NOT inherit PVC or XLPE factors.
         *
         * Returning 0.0 marks the requested dataset as unavailable.
         * Callers must treat this as incomplete engineering data.
         */
        return when (insulation) {

            InsulationType.PVC ->
                IecTables.ambientCorrectionPvc(
                    ambientTemperatureC
                )

            InsulationType.XLPE ->
                IecTables.ambientCorrectionXlpe(
                    ambientTemperatureC
                )

            InsulationType.Rubber,
            InsulationType.EPR ->
                0.0
        }
    }

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double {

        require(
            numberOfCircuits >= 1
        ) {
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
         * This engine must never become an alias for CEI/CEC or
         * another standard.
         */
        if (standard != Standard.IEC) {
            return null
        }

        if (sectionMm2 <= 0.0) {
            return null
        }

        if (loadedConductors <= 0) {
            return null
        }

        /*
         * IecTables currently contains verified/controlled PVC
         * ampacity combinations only.
         *
         * getBaseAmpacity() itself refuses unsupported insulation
         * combinations and returns null.
         */
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

    override fun implementationStatus(): String {

        return if (
            standard == Standard.IEC
        ) {

            buildString {

                append(
                    "IEC 60364 engine is active with the project's " +
                        "controlled IEC dataset. "
                )

                append(
                    "Explicit ampacity data are currently available " +
                        "only for populated conductor/material/" +
                        "insulation/installation combinations. "
                )

                append(
                    "PVC and XLPE ambient correction datasets are " +
                        "available through the controlled tables. "
                )

                append(
                    "Rubber and EPR ambient correction datasets are " +
                        "not yet populated and are therefore not " +
                        "substituted with PVC or XLPE values. "
                )

                append(
                    "Missing combinations return unavailable data " +
                        "rather than fabricated values. "
                )

                append(
                    "Full IEC compliance is not claimed."
                )
            }

        } else {

            "This engine is not an implementation of " +
                "${standard.shortName}. " +
                "A dedicated verified dataset is required."
        }
    }
}
