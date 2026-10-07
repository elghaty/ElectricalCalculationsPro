package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.standards.egyptian.EgyptianCableTables
import com.electrical.calculationspro.data.standards.egyptian.EgyptianInstallationRules
import kotlin.math.abs

class EgyptianCodeEngine : StandardEngine {

    override val standard: Standard =
        Standard.EGYPTIAN

    override val codeName: String =
        "Egyptian Electrical Code"

    override val codeRevision: String =
        "Egyptian controlled project dataset"

    override fun maximumVoltageDropPercent(
        circuitCategory: String
    ): Double =
        0.0

    override fun ambientTemperatureFactor(
        insulation: InsulationType,
        ambientTemperatureC: Double
    ): Double =
        0.0

    override fun groupingFactor(
        numberOfCircuits: Int
    ): Double =
        0.0

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
                    ambientTemperatureC = null,
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
        emptyList()

    override fun isFullyImplemented(): Boolean =
        false

    override fun implementationStatus(): String =
        """
        Egyptian engineering-code engine is active.

        The Egyptian standard registry is separated from IEC and NEC.
        Egyptian adopted IEC references are recorded separately from
        Egyptian regulatory requirements.

        No IEC ampacity, grouping factor, ambient factor or voltage-drop
        value is silently substituted as an Egyptian-code value.

        Egyptian numerical cable data are returned only when the
        EgyptianCableTables dataset explicitly contains the requested
        combination.

        A controlled current Egyptian-code dataset is required before
        the application can claim full Egyptian-code compliance.
        """.trimIndent()

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
