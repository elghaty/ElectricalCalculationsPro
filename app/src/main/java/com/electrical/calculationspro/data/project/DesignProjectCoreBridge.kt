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
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult

/**
 * Central application bridge between the Android UI,
 * project layer and engineering calculation layer.
 *
 * UI
 *  ↓
 * DesignProjectCoreBridge
 *  ↓
 * DesignProjectEngine / Engineering Facades
 *  ↓
 * Calculators / Standards / Catalogs
 *
 * No engineering formulas are implemented here.
 *
 * IMPORTANT:
 *
 * The interactive SLD network is authoritative while the
 * engineer is editing the SLD.
 *
 * Therefore:
 *
 * SLD Editor
 *     ↓
 * current SldNetwork
 *     ↓
 * this bridge
 *     ↓
 * SldEngineeringFacade
 *     ↓
 * Engineering Package
 *
 * The bridge must not rebuild the SLD from project.electrical
 * when the engineer is calculating the currently edited SLD.
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
    // RECALCULATION
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
    ): Pair<DesignProject, DesignProjectValidationResult> =
        DesignProjectEngine.validateAndSave(project)

    // ============================================================
    // SLD ACCESS
    // ============================================================

    /**
     * Returns the stored SLD when available.
     *
     * If the project has no stored SLD, the initial SLD is generated
     * from the electrical project data.
     */
    fun getProjectSld(
        project: DesignProject
    ): SldNetwork =
        DesignProjectSldBridge.rebuildFromProject(
            project
        )

    fun getActiveProjectSld(): SldNetwork? {
        val project =
            getActiveProject()
                ?: return null

        return getProjectSld(
            project
        )
    }

    /**
     * Saves the exact network currently used by the interactive SLD.
     *
     * This is the authoritative network for the SLD editor.
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

        return DesignProjects.save(
            updatedProject
        )
    }

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
     * Builds the initial SLD from the electrical project.
     *
     * This should be used when no manually edited SLD exists yet.
     */
    fun buildProjectSld(
        project: DesignProject
    ): SldNetwork =
        DesignProjectSldBridge.buildFromElectricalDesign(
            project.electrical
        )

    // ============================================================
    // CURRENT INTERACTIVE SLD ENGINEERING
    // ============================================================

    /**
     * Calculates the exact SLD network supplied by the interactive
     * editor.
     *
     * IMPORTANT:
     *
     * This method does NOT call getProjectSld().
     *
     * The supplied network is considered the current authoritative
     * engineering model.
     *
     * Therefore, if the engineer changes:
     *
     * LOAD = 45 kW
     *
     * to:
     *
     * LOAD = 55 kW
     *
     * the calculation immediately uses 55 kW and propagates the
     * change upstream through the current topology.
     */
    fun calculateCurrentProjectSld(
        project: DesignProject,
        network: SldNetwork,
        panelNodeId: String? = null,
        voltageFactor: Double = 1.05,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {

        require(
            network.nodes.isNotEmpty()
        ) {
            "SLD network is empty."
        }

        /*
         * Persist the exact network being edited.
         *
         * This preserves manual SLD modifications and makes the
         * project and editor use the same topology.
         */
        saveProjectSld(
            project = project,
            network = network,
            name = "Main SLD",
            source = "Electrical Design - Interactive SLD"
        )

        /*
         * Use the selected panel when available.
         *
         * Otherwise use the first PANEL in the current network.
         */
        val selectedPanel =
            panelNodeId
                ?: network.nodes.firstOrNull {
                    it.type.name.equals(
                        "PANEL",
                        ignoreCase = true
                    )
                }?.id

        /*
         * CRITICAL:
         *
         * Calculate directly from the current interactive network.
         *
         * Do NOT rebuild the network from project.electrical here.
         */
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
     * Calculates the project's stored SLD.
     *
     * This method is retained for reports or operations where the
     * stored project SLD is intentionally requested.
     */
    fun calculateProjectSld(
        project: DesignProject,
        panelNodeId: String? = null,
        voltageFactor: Double = 1.05,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {

        val network =
            getProjectSld(
                project
            )

        val selectedPanel =
            panelNodeId
                ?: network.nodes.firstOrNull {
                    it.type.name.equals(
                        "PANEL",
                        ignoreCase = true
                    )
                }?.id

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
     * Saves and calculates an explicitly supplied SLD network.
     *
     * This method is useful when a caller has already modified the
     * network and wants both persistence and a complete engineering
     * study in one operation.
     */
    fun calculateAndSaveProjectSld(
        project: DesignProject,
        network: SldNetwork,
        panelNodeId: String? = null,
        voltageFactor: Double = 1.05,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {

        return calculateCurrentProjectSld(
            project = project,
            network = network,
            panelNodeId = panelNodeId,
            voltageFactor = voltageFactor,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds
        )
    }

    // ============================================================
    // PUMP
    // ============================================================

    fun calculatePump(
        input: PumpCalculationInput
    ): PumpCalculationResult =
        ElectricalCalculations.calculatePump(
            input
        )

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
    // HISTORY
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
