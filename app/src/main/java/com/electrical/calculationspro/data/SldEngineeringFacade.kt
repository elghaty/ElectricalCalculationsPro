package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL ENGINEERING CORE
 * SLD ENGINEERING FACADE
 * ================================================================
 *
 * UI
 *  ↓
 * ElectricalCalculations
 *  ↓
 * SldEngineeringFacade
 *  ↓
 * ┌───────────────────────────────────────────────┐
 * │ SLD Validation                                │
 * │ Upstream Engineering                          │
 * │ Short Circuit                                 │
 * │ Cable Sizing                                  │
 * │ Protection Coordination                       │
 * │ Panel Schedule                                │
 * └───────────────────────────────────────────────┘
 *
 * The UI must not access engineering engines directly.
 *
 * ================================================================
 */
object SldEngineeringFacade {

    /**
     * ------------------------------------------------------------
     * VALIDATION
     * ------------------------------------------------------------
     */
    fun validate(
        network: SldNetwork
    ): SldDesignValidator.Result {

        return SldDesignValidator.validate(
            network
        )
    }

    /**
     * ------------------------------------------------------------
     * UPSTREAM ENGINEERING
     * ------------------------------------------------------------
     *
     * Calculates downstream demand propagated toward the source.
     *
     * LOAD
     *   ↓
     * FEEDER
     *   ↓
     * PANEL
     *   ↓
     * BUS
     *   ↓
     * MAIN FEEDER
     *   ↓
     * TRANSFORMER / GENERATOR
     *   ↓
     * SOURCE
     */
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

        return SldUpstreamEngineering.calculate(
            network
        )
    }

    /**
     * ------------------------------------------------------------
     * SHORT CIRCUIT
     * ------------------------------------------------------------
     */
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

    /**
     * ------------------------------------------------------------
     * CABLE SIZING
     * ------------------------------------------------------------
     */
    fun calculateCableSizing(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldCableSizingStudy {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val validation =
            SldDesignValidator.validate(network)

        require(validation.valid) {
            buildValidationMessage(validation)
        }

        val upstream =
            calculateUpstream(network)

        return SldCableSizingEngine.calculate(
            network = network,
            shortCircuitStudy = shortCircuitStudy,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds
        )
    }

    /**
     * ------------------------------------------------------------
     * PANEL SCHEDULE
     * ------------------------------------------------------------
     */
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

        return SldPanelScheduleEngine.calculate(
            network = network,
            panelNodeId = panelNodeId,
            cableSizingStudy = cableSizingStudy,
            shortCircuitStudy = shortCircuitStudy
        )
    }

    /**
     * ------------------------------------------------------------
     * PROTECTION COORDINATION
     * ------------------------------------------------------------
     */
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

        return SldProtectionCoordinationEngine.calculate(
            network = network,
            shortCircuitStudy = shortCircuitStudy,
            cableSizingStudy = cableSizingStudy
        )
    }

    /**
     * ------------------------------------------------------------
     * COMPLETE SLD ENGINEERING STUDY
     * ------------------------------------------------------------
     *
     * Unified engineering sequence:
     *
     * 1. Validate topology
     * 2. Upstream load aggregation
     * 3. Short circuit
     * 4. Cable sizing
     * 5. Protection coordination
     * 6. Panel schedule
     *
     * This makes the SLD a real engineering network rather than
     * a drawing-only object.
     */
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

        /**
         * --------------------------------------------------------
         * 1. UPSTREAM
         * --------------------------------------------------------
         */
        val upstream =
            SldUpstreamEngineering.calculate(
                network
            )

        /**
         * --------------------------------------------------------
         * 2. SHORT CIRCUIT
         * --------------------------------------------------------
         */
        val shortCircuitStudy =
            calculateShortCircuit(
                network = network,
                voltageFactor = voltageFactor
            )

        /**
         * --------------------------------------------------------
         * 3. CABLE SIZING
         * --------------------------------------------------------
         */
        val cableSizingStudy =
            SldCableSizingEngine.calculate(
                network = network,
                shortCircuitStudy =
                    shortCircuitStudy,
                voltageDropLimitPercent =
                    voltageDropLimitPercent,
                shortCircuitTimeSeconds =
                    shortCircuitTimeSeconds
            )

        /**
         * --------------------------------------------------------
         * 4. PROTECTION
         * --------------------------------------------------------
         */
        val protectionStudy =
            SldProtectionCoordinationEngine.calculate(
                network = network,
                shortCircuitStudy =
                    shortCircuitStudy,
                cableSizingStudy =
                    cableSizingStudy
            )

        /**
         * --------------------------------------------------------
         * 5. PANEL SCHEDULE
         * --------------------------------------------------------
         */
        val panelSchedule =
            panelNodeId?.let { id ->

                SldPanelScheduleEngine.calculate(
                    network = network,
                    panelNodeId = id,
                    cableSizingStudy =
                        cableSizingStudy,
                    shortCircuitStudy =
                        shortCircuitStudy
                )
            }

        /**
         * --------------------------------------------------------
         * 6. COMPLETE RESULT
         * --------------------------------------------------------
         */
        return SldEngineeringPackage(
            upstream = upstream,
            shortCircuit = shortCircuitStudy,
            cableSizing = cableSizingStudy,
            protectionCoordination =
                protectionStudy,
            panelSchedule =
                panelSchedule
        )
    }

    /**
     * ------------------------------------------------------------
     * VALIDATION MESSAGE
     * ------------------------------------------------------------
     */
    private fun buildValidationMessage(
        validation: SldDesignValidator.Result
    ): String {

        return buildString {

            appendLine(
                "SLD topology validation failed."
            )

            if (validation.errors.isNotEmpty()) {

                appendLine()

                appendLine(
                    "Errors:"
                )

                validation.errors.forEach {
                    appendLine(
                        "- [${it.code}] ${it.message}"
                    )
                }
            }

            if (validation.warnings.isNotEmpty()) {

                appendLine()

                appendLine(
                    "Warnings:"
                )

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
 */
data class SldEngineeringPackage(
    val upstream: SldUpstreamEngineering.Result,
    val shortCircuit: SldShortCircuitStudy,
    val cableSizing: SldCableSizingStudy,
    val protectionCoordination: SldProtectionCoordinationResult,
    val panelSchedule: SldPanelSchedule?
)
