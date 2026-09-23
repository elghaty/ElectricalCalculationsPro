package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.CalculationHistory
import com.electrical.calculationspro.data.CalculationHistoryItem
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.EngineeringCalculationPackage
import com.electrical.calculationspro.data.SldEngineeringFacade
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult

/**
 * Single application bridge between UI and engineering/project layers.
 *
 * UI
 *  ↓
 * DesignProjectCoreBridge
 *  ↓
 * Project Engine / Engineering Facades
 *  ↓
 * Calculators / Standards / Catalogs
 *
 * This class contains orchestration only.
 * Engineering formulas remain inside the calculation engines.
 */
object DesignProjectCoreBridge {

    // ============================================================
    // PROJECT ACCESS
    // ============================================================

    fun getActiveProject(): DesignProject? =
        DesignProjects.getActive()

    fun getProject(
        projectId: String
    ): DesignProject? =
        DesignProjects.getById(projectId)

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
        DesignProjects.save(project)

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
        DesignProjects.setActive(projectId)

    // ============================================================
    // PROJECT LIFECYCLE
    // ============================================================

    fun startProject(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.start(project)

    fun completeProject(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.complete(project)

    fun archiveProject(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.archive(project)

    // ============================================================
    // PROJECT RECALCULATION
    // ============================================================

    fun recalculateElectrical(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateElectrical(project)

    fun recalculateWater(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateWater(project)

    fun recalculateSewage(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateSewage(project)

    fun recalculateAll(
        project: DesignProject
    ): DesignProject =
        DesignProjectEngine.recalculateAll(project)

    fun recalculate(
        project: DesignProject,
        discipline: DesignDiscipline
    ): DesignProject =
        DesignProjectEngine.recalculate(
            project = project,
            discipline = discipline
        )

    // ============================================================
    // VALIDATION
    // ============================================================

    fun calculateAndValidate(
        project: DesignProject
    ): ProjectCalculationResult =
        DesignProjectEngine.calculateAndValidate(project)

    fun validateProject(
        project: DesignProject
    ): DesignProjectValidationResult =
        DesignProjectEngine.validate(project)

    fun validateAndSave(
        project: DesignProject
    ): Pair<
        DesignProject,
        DesignProjectValidationResult
        > =
        DesignProjectEngine.validateAndSave(project)

    // ============================================================
    // SLD PROJECT WORKFLOW
    // ============================================================

    /**
     * Returns the canonical SLD network.
     *
     * If the project already contains a saved SLD,
     * the stored network is returned.
     *
     * Otherwise the network is generated from the
     * actual electrical project entities.
     */
    fun getProjectSld(
        project: DesignProject
    ): SldNetwork =
        DesignProjectSldBridge.rebuildFromProject(project)

    /**
     * Returns the active project's SLD.
     */
    fun getActiveProjectSld(): SldNetwork? {

        val project =
            getActiveProject()
                ?: return null

        return getProjectSld(project)
    }

    /**
     * Saves an SLD network into the supplied project
     * and persists the updated project.
     */
    fun saveProjectSld(
        project: DesignProject,
        network: SldNetwork,
        name: String = "Main SLD",
        source: String = "Electrical Design"
    ): DesignProject {

        val updatedProject =
            DesignProjectSldBridge.saveNetwork(
                project = project,
                network = network,
                name = name,
                source = source
            )

        return DesignProjects.save(updatedProject)
    }

    /**
     * Saves an SLD network into the active project.
     */
    fun saveActiveProjectSld(
        network: SldNetwork,
        name: String = "Main SLD",
        source: String = "Electrical Design"
    ): DesignProject? =
        DesignProjectSldBridge.saveNetworkToActiveProject(
            network = network,
            name = name,
            source = source
        )

    /**
     * Builds a new SLD directly from the actual
     * electrical design entities.
     */
    fun buildProjectSld(
        project: DesignProject
    ): SldNetwork =
        DesignProjectSldBridge.buildFromElectricalDesign(
            project.electrical
        )

    /**
     * Calculates the complete SLD engineering package.
     *
     * Workflow:
     *
     * SLD
     *  ↓
     * Short Circuit
     *  ↓
     * Cable Sizing
     *  ↓
     * Protection Coordination
     *  ↓
     * Panel Schedule
     */
    fun calculateProjectSld(
        project: DesignProject,
        panelNodeId: String? = null,
        voltageFactor: Double = 1.05,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {

        val network =
            getProjectSld(project)

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val selectedPanel =
            panelNodeId
                ?: network.nodes
                    .firstOrNull {
                        it.type == SldNodeType.PANEL
                    }
                    ?.id

        return SldEngineeringFacade.calculateComplete(
            network = network,
            panelNodeId = selectedPanel,
            voltageFactor = voltageFactor,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds
        )
    }

    /**
     * Calculates and saves the supplied SLD network.
     */
    fun calculateAndSaveProjectSld(
        project: DesignProject,
        network: SldNetwork,
        panelNodeId: String? = null,
        voltageFactor: Double = 1.05,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {

        saveProjectSld(
            project = project,
            network = network
        )

        val selectedPanel =
            panelNodeId
                ?: network.nodes
                    .firstOrNull {
                        it.type == SldNodeType.PANEL
                    }
                    ?.id

        return SldEngineeringFacade.calculateComplete(
            network = network,
            panelNodeId = selectedPanel,
            voltageFactor = voltageFactor,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds
        )
    }

    // ============================================================
    // PUMP ENGINEERING
    // ============================================================

    fun calculatePump(
        input: PumpCalculationInput
    ): PumpCalculationResult =
        ElectricalCalculations.calculatePump(input)

    // ============================================================
    // ELECTRICAL CALCULATIONS
    // ============================================================

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

    // ============================================================
    // CALCULATION HISTORY
    // ============================================================

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
