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

object DesignProjectCoreBridge {

    fun getActiveProject(): DesignProject? =
        DesignProjects.getActive()

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
        DesignProjects.save(project)

    fun selectProject(
        projectId: String
    ): Boolean =
        DesignProjects.setActive(projectId)

    fun validateProject(
        project: DesignProject
    ): DesignProjectValidationResult =
        DesignProjectValidator.validate(project)

    fun calculatePump(
        input: PumpCalculationInput
    ): PumpCalculationResult =
        ElectricalCalculations.calculatePump(input)

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
            prospectiveFaultCurrentKA = prospectiveFaultCurrentKA,
            breakerBreakingCapacityKA = breakerBreakingCapacityKA,
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
