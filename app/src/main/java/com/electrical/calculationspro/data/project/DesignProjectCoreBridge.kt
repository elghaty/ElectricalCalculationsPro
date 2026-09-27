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
     * Persist the exact network currently edited by the engineer.
     */
    saveProjectSld(
        project = project,
        network = network,
        name = "Main SLD",
        source = "Electrical Design - Interactive SLD"
    )

    /*
     * Use the explicitly selected panel when supplied.
     * Otherwise use the first panel in the current topology.
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
     * IMPORTANT:
     *
     * The project's electrical standard is now the source of truth
     * for the SLD engineering cycle.
     *
     * No SLD topology is rebuilt here.
     */
    val selectedStandard =
        project.electricalStandard
            ?: Standard.EGYPTIAN

    val engineeringContext =
        com.electrical.calculationspro.data.SldEngineeringContext(
            standard = selectedStandard,
            voltageFactor = voltageFactor,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds
        )

    return SldEngineeringFacade.calculateComplete(
        network = network,
        panelNodeId = selectedPanel,
        engineeringContext = engineeringContext
    )
}


/**
 * Calculates the project's stored SLD using the project's
 * selected electrical standard.
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

    val selectedStandard =
        project.electricalStandard
            ?: Standard.EGYPTIAN

    val engineeringContext =
        com.electrical.calculationspro.data.SldEngineeringContext(
            standard = selectedStandard,
            voltageFactor = voltageFactor,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds
        )

    return SldEngineeringFacade.calculateComplete(
        network = network,
        panelNodeId = selectedPanel,
        engineeringContext = engineeringContext
    )
}


/**
 * Saves and calculates an explicitly supplied SLD network.
 *
 * The project's selected electrical standard is preserved.
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
