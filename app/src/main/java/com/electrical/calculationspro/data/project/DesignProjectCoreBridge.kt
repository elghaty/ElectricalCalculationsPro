package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.CalculationHistory
import com.electrical.calculationspro.data.CalculationHistoryItem
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.EngineeringCalculationPackage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult

/**
 * Single application bridge between UI and engineering/project layers.
 *
 * UI must use this facade instead of directly coordinating
 * discipline engines or individual calculators.
 */
object DesignProjectCoreBridge {

    fun getActiveProject(): DesignProject? =
        DesignProjects.getActive()

    fun getProject(
        projectId: String
    ): DesignProject? =
        DesignProjects.getById(
            projectId
        )

    fun getProjects(): List<DesignProject> =
        DesignProjects.getAll()

    fun createProject(
        projectName: String,
        projectNumber: String = "",
        clientName: String = "",
        consultantName: String = "",
        location: String = "",
        description: String = "",
        electricalStandard: Standard? = Standard.IEC
    ): DesignProject =
        DesignProjects.create(
            projectName = projectName,
            projectNumber = projectNumber,
            clientName = clientName,
            consultantName = consultantName,
            location = location,
            description = description,
            electricalStandard = electricalStandard
        )

    fun saveProject(
        project: DesignProject
    ): DesignProject =
        DesignProjects.save(
            project
        )

    fun updateProject(
        projectId: String,
        transform: (DesignProject) -> DesignProject
    ): DesignProject? =
        DesignProjects.update(
            projectId = projectId,
            transform = transform
        )

    fun updateProject(
        project: DesignProject,
        transform: (DesignProject) -> DesignProject
    ): DesignProject =
        DesignProjectEngine.updateProject(
            project = project,
            transform = transform
        )

    fun selectProject(
        projectId: String
    ): Boolean =
        DesignProjects.setActive(
            projectId
        )

    fun deleteProject(
        projectId: String
    ): Boolean =
        DesignProjects.delete(
            projectId
        )

    fun startProject(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.start(
            project
        )

    fun completeProject(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.complete(
            project
        )

    fun archiveProject(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.archive(
            project
        )

    fun recalculateElectrical(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateElectrical(
            project
        )

    fun recalculateWater(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateWater(
            project
        )

    fun recalculateSewage(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateSewage(
            project
        )

    fun recalculateAll(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateAll(
            project
        )

    fun recalculate(
        project: DesignProject,
        discipline: DesignDiscipline
    ): DesignProject =
        DesignProjectEngine.recalculate(
            project = project,
            discipline = discipline
        )

    fun calculateAndValidate(
        project: DesignProject
    ): ProjectCalculationResult =
        DesignProjectEngine.calculateAndValidate(
            project
        )

    fun validateProject(
        project: DesignProject
    ): DesignProjectValidationResult =
        DesignProjectEngine.validate(
            project
        )

    fun validateAndSave(
        project: DesignProject
    ): Pair<
        DesignProject,
        DesignProjectValidationResult
        > =
        DesignProjectEngine.validateAndSave(
            project
        )

    fun calculatePump(
        input: PumpCalculationInput
    ): PumpCalculationResult =
        ElectricalCalculations.calculatePump(
            input
        )

    fun calculateDesignCurrentFromKw(
        loadKw: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double =
        ElectricalCalculations.calculateDesignCurrentFromKw(
            loadKw = loadKw,
            voltage = voltage,
            powerFactor = powerFactor,
            currentType = currentType
        )

    fun calculateVoltageDrop(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> =
        ElectricalCalculations.calculateVoltageDrop(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        )

    fun calculateShortCircuitCurrent(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 50.0
    ) =
        ElectricalCalculations.calculateShortCircuitCurrent(
            voltage = voltage,
            length = length,
            sectionMm2 = sectionMm2,
            material = material,
            currentType = currentType,
            sourceIkKA = sourceIkKA
        )

    fun calculateBreakerSelection(
        designCurrentA: Double,
        cableAmpacityA: Double,
        prospectiveFaultCurrentKA: Double = 0.0,
        breakerBreakingCapacityKA: Double = 0.0,
        standard: Standard = Standard.IEC
    ) =
        ElectricalCalculations.calculateBreakerSelection(
            designCurrentA = designCurrentA,
            cableAmpacityA = cableAmpacityA,
            prospectiveFaultCurrentKA =
                prospectiveFaultCurrentKA,
            breakerBreakingCapacityKA =
                breakerBreakingCapacityKA,
            standard = standard
        )

    fun saveCalculation(
        calculationType: String,
        standard: Standard,
        summary: String,
        result: EngineeringCalculationPackage
    ): CalculationHistoryItem =
        CalculationHistory.saveToActiveProject(
            calculationType = calculationType,
            standard = standard,
            summary = summary,
            result = result
        )
}
