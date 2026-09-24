package com.electrical.calculationspro.data

/**
 * ================================================================
 * SLD ENGINEERING FACADE
 * ================================================================
 *
 * Single entry point between the SLD/UI layer and the engineering
 * engines.
 *
 * UI
 *  ↓
 * SldEngineeringFacade
 *  ↓
 * ┌───────────────────────────────────────────────┐
 * │ Validation                                    │
 * │ Upstream Engineering                          │
 * │ Short Circuit                                 │
 * │ Cable Sizing                                  │
 * │ Protection Coordination                       │
 * │ Panel Schedule                                │
 * └───────────────────────────────────────────────┘
 *
 * No engineering formulas are implemented here.
 *
 * ================================================================
 */
object SldEngineeringFacade {

    // ============================================================
    // VALIDATION
    // ============================================================

    fun validate(
        network: SldNetwork
    ): SldDesignValidator.Result {

        return SldDesignValidator.validate(network)
    }

    // ============================================================
    // UPSTREAM ENGINEERING
    // ============================================================

    fun calculateUpstream(
        network: SldNetwork
    ): SldUpstreamEngineering.Result {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val validation =
            SldDesignValidator.validate(network)

        require(validation.valid) {
            buildValidationMessage(validation)
        }

        return SldUpstreamEngineering.calculate(network)
    }

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

    fun calculateShortCircuit(
        network: SldNetwork,
        voltageFactor: Double = 1.05
    ): SldShortCircuitStudy {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val validation =
            SldDesignValidator.validate(network)

        require(validation.valid) {
            buildValidationMessage(validation)
        }

        return SldShortCircuitEngine.calculate(
            network = network,
            voltageFactor = voltageFactor
        )
    }

    // ============================================================
    // CABLE SIZING
    // ============================================================

    fun calculateCableSizing(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldCableSizingStudy {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        require(voltageDropLimitPercent > 0.0) {
            "Voltage drop limit must be greater than zero."
        }

        require(shortCircuitTimeSeconds > 0.0) {
            "Short-circuit clearing time must be greater than zero."
        }

        val validation =
            SldDesignValidator.validate(network)

        require(validation.valid) {
            buildValidationMessage(validation)
        }

        val shortCircuit =
            shortCircuitStudy
                ?: calculateShortCircuit(network)

        return SldCableSizingEngine.calculate(
            network = network,
            shortCircuitStudy = shortCircuit,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds
        )
    }

    // ============================================================
    // PANEL SCHEDULE
    // ============================================================

    fun calculatePanelSchedule(
        network: SldNetwork,
        panelNodeId: String,
        cableSizingStudy: SldCableSizingStudy? = null,
        shortCircuitStudy: SldShortCircuitStudy? = null
    ): SldPanelSchedule {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val validation =
            SldDesignValidator.validate(network)

        require(validation.valid) {
            buildValidationMessage(validation)
        }

        require(
            network.nodes.any {
                it.id == panelNodeId &&
                    it.type == SldNodeType.PANEL
            }
        ) {
            "Panel node '$panelNodeId' was not found."
        }

        return SldPanelScheduleEngine.calculate(
            network = network,
            panelNodeId = panelNodeId,
            cableSizingStudy = cableSizingStudy,
            shortCircuitStudy = shortCircuitStudy
        )
    }

    // ============================================================
    // PROTECTION COORDINATION
    // ============================================================

    fun calculateProtectionCoordination(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        cableSizingStudy: SldCableSizingStudy? = null
    ): SldProtectionCoordinationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val validation =
            SldDesignValidator.validate(network)

        require(validation.valid) {
            buildValidationMessage(validation)
        }

        val shortCircuit =
            shortCircuitStudy
                ?: calculateShortCircuit(network)

        val cableSizing =
            cableSizingStudy
                ?: calculateCableSizing(
                    network = network,
                    shortCircuitStudy = shortCircuit
                )

        return SldProtectionCoordinationEngine.calculate(
            network = network,
            shortCircuitStudy = shortCircuit,
            cableSizingStudy = cableSizing
        )
    }

    // ============================================================
    // COMPLETE ENGINEERING STUDY
    // ============================================================

    fun calculateComplete(
        network: SldNetwork,
        panelNodeId: String? = null,
        voltageFactor: Double = 1.05,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val validation =
            SldDesignValidator.validate(network)

        require(validation.valid) {
            buildValidationMessage(validation)
        }

        /*
         * 1. UPSTREAM
         */
        val upstream =
            calculateUpstream(network)

        /*
         * 2. SHORT CIRCUIT
         */
        val shortCircuit =
            calculateShortCircuit(
                network = network,
                voltageFactor = voltageFactor
            )

        /*
         * 3. CABLE SIZING
         */
        val cableSizing =
            calculateCableSizing(
                network = network,
                shortCircuitStudy = shortCircuit,
                voltageDropLimitPercent =
                    voltageDropLimitPercent,
                shortCircuitTimeSeconds =
                    shortCircuitTimeSeconds
            )

        /*
         * 4. PROTECTION
         */
        val protection =
            calculateProtectionCoordination(
                network = network,
                shortCircuitStudy = shortCircuit,
                cableSizingStudy = cableSizing
            )

        /*
         * 5. PANEL SCHEDULE
         */
        val panelSchedule =
            panelNodeId?.let { id ->
                calculatePanelSchedule(
                    network = network,
                    panelNodeId = id,
                    cableSizingStudy = cableSizing,
                    shortCircuitStudy = shortCircuit
                )
            }

        /*
         * 6. COMPLETE PACKAGE
         */
        return SldEngineeringPackage(
            upstream = upstream,
            shortCircuit = shortCircuit,
            cableSizing = cableSizing,
            protectionCoordination = protection,
            panelSchedule = panelSchedule
        )
    }

    // ============================================================
    // VALIDATION MESSAGE
    // ============================================================

    private fun buildValidationMessage(
        validation: SldDesignValidator.Result
    ): String {

        return buildString {

            appendLine(
                "SLD topology validation failed."
            )

            if (validation.errors.isNotEmpty()) {

                appendLine()
                appendLine("Errors:")

                validation.errors.forEach {
                    appendLine(
                        "- [${it.code}] ${it.message}"
                    )
                }
            }

            if (validation.warnings.isNotEmpty()) {

                appendLine()
                appendLine("Warnings:")

                validation.warnings.forEach {
                    appendLine(
                        "- [${it.code}] ${it.message}"
                    )
                }
            }
        }
    }
}

/**
 * ================================================================
 * COMPLETE SLD ENGINEERING RESULT
 * ================================================================
 *
 * This is the contract consumed by:
 *
 * DesignProjectCoreBridge
 * SldEditorActions
 * SldEngineeringReportEngine
 *
 * Do not remove or rename these fields without updating all
 * consumers in the same change.
 */
data class SldEngineeringPackage(
    val upstream: SldUpstreamEngineering.Result,
    val shortCircuit: SldShortCircuitStudy,
    val cableSizing: SldCableSizingStudy,
    val protectionCoordination: SldProtectionCoordinationResult,
    val panelSchedule: SldPanelSchedule?
)
