package com.electrical.calculationspro.data.catalog

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.standards.CodeEngineFactory

data class CableEngineeringCandidate(
    val cable: CableCatalogItem,
    val baseAmpacityA: Double,
    val ambientFactor: Double,
    val groupingFactor: Double,
    val correctedAmpacityA: Double,
    val requiredCurrentA: Double,
    val currentCapacityAdequate: Boolean
)

data class CableEngineeringSelectionResult(
    val selected: CableEngineeringCandidate?,
    val alternatives: List<CableEngineeringCandidate>,
    val valid: Boolean,
    val message: String,
    val standardName: String,
    val standardRevision: String
)

object CableEngineeringSelector {

    /**
     * Selects catalog cable candidates using the selected code engine.
     *
     * Ampacity is calculated from the existing standard dataset.
     * Missing engineering data is never replaced by an estimate.
     *
     * This method does not replace voltage-drop, short-circuit,
     * protective-device coordination or installation verification.
     */
    fun select(
        requirement: TechnicalRequirement.Cable,
        designCurrentA: Double,
        installationMethod: InstallationMethod,
        ambientTemperatureC: Double,
        numberOfCircuits: Int,
        loadedConductors: Int = 3,
        manufacturer: Manufacturer? = null
    ): CableEngineeringSelectionResult {

        val engine = CodeEngineFactory.get(requirement.standard)

        fun failure(message: String) =
            CableEngineeringSelectionResult(
                selected = null,
                alternatives = emptyList(),
                valid = false,
                message = message,
                standardName = engine.codeName,
                standardRevision = engine.codeRevision
            )

        if (!designCurrentA.isFinite() || designCurrentA <= 0.0) {
            return failure(
                "Design current must be finite and greater than zero."
            )
        }

        if (!requirement.requiredSectionMm2.isFinite() ||
            requirement.requiredSectionMm2 <= 0.0
        ) {
            return failure(
                "Required cable section must be finite and greater than zero."
            )
        }

        if (!ambientTemperatureC.isFinite()) {
            return failure(
                "Ambient temperature must be finite."
            )
        }

        if (numberOfCircuits < 1) {
            return failure(
                "Number of circuits must be at least one."
            )
        }

        if (loadedConductors !in 2..3) {
            return failure(
                "The available ampacity dataset supports two or three loaded conductors only."
            )
        }

        if (installationMethod.code.isBlank()) {
            return failure(
                "An installation method is required."
            )
        }

        val ambientFactor = engine.ambientTemperatureFactor(
            insulation = requirement.insulation,
            ambientTemperatureC = ambientTemperatureC
        )

        if (!ambientFactor.isFinite() || ambientFactor <= 0.0) {
            return failure(
                "Ambient correction data is unavailable for the selected standard and conditions."
            )
        }

        val groupingFactor = engine.groupingFactor(
            numberOfCircuits = numberOfCircuits
        )

        if (!groupingFactor.isFinite() || groupingFactor <= 0.0) {
            return failure(
                "Grouping correction data is unavailable for the selected standard and circuit arrangement."
            )
        }

        val candidates = EquipmentCatalog.cables()
            .asSequence()
            .filter {
                manufacturer == null ||
                    it.manufacturer == manufacturer
            }
            .filter {
                CodeCompliance.evaluate(
                    requirement,
                    it
                ).compliant
            }
            .mapNotNull { cable ->

                val material = when (
                    cable.conductorMaterial.trim().lowercase()
                ) {
                    "copper" -> ConductorMaterial.Copper
                    "aluminium",
                    "aluminum" -> ConductorMaterial.Aluminum
                    else -> return@mapNotNull null
                }

                val insulation = when (
                    cable.insulation.trim().uppercase()
                ) {
                    "PVC" -> InsulationType.PVC
                    "XLPE" -> InsulationType.XLPE
                    "EPR" -> InsulationType.EPR
                    "RUBBER" -> InsulationType.Rubber
                    else -> return@mapNotNull null
                }

                if (material != requirement.material ||
                    insulation != requirement.insulation
                ) {
                    return@mapNotNull null
                }

                val baseAmpacity = engine.conductorAmpacity(
                    sectionMm2 = cable.sectionMm2,
                    material = material,
                    insulation = insulation,
                    installationMethod = installationMethod,
                    loadedConductors = loadedConductors
                ) ?: return@mapNotNull null

                if (!baseAmpacity.isFinite() ||
                    baseAmpacity <= 0.0
                ) {
                    return@mapNotNull null
                }

                val correctedAmpacity =
                    baseAmpacity * ambientFactor * groupingFactor

                if (!correctedAmpacity.isFinite() ||
                    correctedAmpacity <= 0.0
                ) {
                    return@mapNotNull null
                }

                CableEngineeringCandidate(
                    cable = cable,
                    baseAmpacityA = baseAmpacity,
                    ambientFactor = ambientFactor,
                    groupingFactor = groupingFactor,
                    correctedAmpacityA = correctedAmpacity,
                    requiredCurrentA = designCurrentA,
                    currentCapacityAdequate =
                        correctedAmpacity >= designCurrentA
                )
            }
            .filter {
                it.currentCapacityAdequate
            }
            .sortedWith(
                compareBy<CableEngineeringCandidate> {
                    it.cable.sectionMm2
                }.thenBy {
                    it.cable.manufacturer.name
                }.thenBy {
                    it.cable.model
                }
            )
            .toList()

        if (candidates.isEmpty()) {
            return failure(
                "No cable candidate passed both catalog compliance checks and corrected ampacity for the supplied conditions. Check the selected standard, installation method, conductor count, temperature, grouping and verified product data."
            )
        }

        return CableEngineeringSelectionResult(
            selected = candidates.first(),
            alternatives = candidates.drop(1),
            valid = true,
            message = "Preliminary ampacity selection passed using the selected standard engine and its available correction factors. Voltage drop, short-circuit withstand, protective-device coordination and installation requirements must also be verified before final approval.",
            standardName = engine.codeName,
            standardRevision = engine.codeRevision
        )
    }
}
