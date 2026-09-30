package com.electrical.calculationspro.data

/**
 * ================================================================
 * SLD ENGINEERING FACADE
 * ================================================================
 *
 * Single orchestration point between the SLD editor and engineering
 * calculation engines.
 *
 * IMPORTANT ARCHITECTURE RULE
 *
 * The SLD editor and the engineering study are two different states.
 *
 * DRAWING STATE
 *      |
 *      | may contain disconnected / incomplete elements
 *      v
 * TOPOLOGY GATE
 *      |
 *      | SldTopologyEngine.build()
 *      |
 *      +---- FAIL ----> no engineering calculation
 *      |
 *      v
 * ENGINEERING STUDY
 *
 * The editor is therefore allowed to contain:
 *
 * - unconnected LOAD
 * - unconnected BREAKER
 * - incomplete PANEL
 * - temporary drawing elements
 *
 * None of those states may reach an engineering engine.
 *
 * This class is deliberately orchestration-only.
 * No engineering formulas belong here.
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

        return SldDesignValidator.validate(
            network
        )
    }

    // ============================================================
    // TOPOLOGY GATE
    // ============================================================

    /**
     * Result of the engineering topology gate.
     *
     * This is intentionally separate from SldDesignValidator.
     *
     * SldDesignValidator is useful for editor diagnostics.
     *
     * SldTopologyEngine is authoritative for deciding whether
     * engineering calculations are allowed to start.
     */
    data class TopologyGateResult(
        val valid: Boolean,
        val topology: SldTopologyEngine.Topology? = null,
        val errorMessage: String? = null
    )

    /**
     * Tests whether the current drawing is ready for engineering.
     *
     * IMPORTANT:
     *
     * This method NEVER modifies the network.
     * It NEVER deletes nodes.
     * It NEVER changes connections.
     * It NEVER navigates.
     *
     * Failure is a normal editor state.
     */
    fun checkEngineeringTopology(
        network: SldNetwork
    ): TopologyGateResult {

        if (network.nodes.isEmpty()) {

            return TopologyGateResult(
                valid = false,
                errorMessage =
                    "SLD network is empty."
            )
        }

        return try {

            val topology =
                SldTopologyEngine.build(
                    network
                )

            TopologyGateResult(
                valid = true,
                topology = topology
            )

        } catch (error: Throwable) {

            /*
             * IMPORTANT:
             *
             * Topology failure is NOT an application failure.
             *
             * It means the drawing is currently incomplete.
             *
             * Catch Throwable here deliberately because the topology
             * engine uses require()/error()/IllegalArgumentException
             * internally and the editor must never be terminated by
             * those validation failures.
             */
            TopologyGateResult(
                valid = false,
                errorMessage =
                    error.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "SLD topology is not ready for engineering."
            )
        }
    }

    /**
     * Hard engineering gate.
     *
     * Every engineering entry point must pass through this method.
     *
     * This prevents a disconnected drawing from ever reaching:
     *
     * - upstream engineering
     * - short circuit
     * - cable sizing
     * - protection
     * - panel schedule
     */
    private fun requireEngineeringTopology(
        network: SldNetwork
    ): SldTopologyEngine.Topology {

        val result =
            checkEngineeringTopology(
                network
            )

        require(
            result.valid &&
                result.topology != null
        ) {
            result.errorMessage
                ?: "SLD topology is not ready for engineering."
        }

        return result.topology
    }

    // ============================================================
    // UPSTREAM ENGINEERING
    // ============================================================

    fun calculateUpstream(
        network: SldNetwork
    ): SldUpstreamEngineering.Result {

        requireEngineeringTopology(
            network
        )

        return SldUpstreamEngineering.calculate(
            network = network
        )
    }

    /**
     * Context-aware upstream entry point.
     */
    fun calculateUpstream(
        network: SldNetwork,
        engineeringContext: SldEngineeringContext
    ): SldUpstreamEngineering.Result {

        requireEngineeringTopology(
            network
        )

        validateContext(
            engineeringContext
        )

        return SldUpstreamEngineering.calculate(
            network = network,
            engineeringContext = engineeringContext
        )
    }

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

    fun calculateShortCircuit(
        network: SldNetwork,
        voltageFactor: Double = 1.05
    ): SldShortCircuitStudy {

        requireEngineeringTopology(
            network
        )

        require(
            voltageFactor > 0.0
        ) {
            "Voltage factor must be greater than zero."
        }

        return SldShortCircuitEngine.calculate(
            network = network,
            voltageFactor = voltageFactor
        )
    }

    /**
     * Context-aware short-circuit calculation.
     */
    fun calculateShortCircuit(
        network: SldNetwork,
        engineeringContext: SldEngineeringContext
    ): SldShortCircuitStudy {

        requireEngineeringTopology(
            network
        )

        validateContext(
            engineeringContext
        )

        return SldShortCircuitEngine.calculate(
            network = network,
            voltageFactor =
                engineeringContext.voltageFactor
        )
    }

    // ============================================================
    // CABLE SIZING
    // ============================================================

    fun calculateCableSizing(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0,
        upstreamEngineering:
            SldUpstreamEngineering.Result? = null
    ): SldCableSizingStudy {

        requireEngineeringTopology(
            network
        )

        require(
            voltageDropLimitPercent > 0.0
        ) {
            "Voltage drop limit must be greater than zero."
        }

        require(
            shortCircuitTimeSeconds > 0.0
        ) {
            "Short-circuit clearing time must be greater than zero."
        }

        val upstream =
            upstreamEngineering
                ?: SldUpstreamEngineering.calculate(
                    network = network
                )

        val shortCircuit =
            shortCircuitStudy
                ?: SldShortCircuitEngine.calculate(
                    network = network
                )

        return SldCableSizingEngine.calculate(
            network = network,
            shortCircuitStudy = shortCircuit,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds,
            upstreamEngineering =
                upstream
        )
    }

    /**
     * Context-aware cable sizing.
     */
    fun calculateCableSizing(
        network: SldNetwork,
        engineeringContext:
            SldEngineeringContext,
        shortCircuitStudy:
            SldShortCircuitStudy? = null,
        upstreamEngineering:
            SldUpstreamEngineering.Result? = null
    ): SldCableSizingStudy {

        requireEngineeringTopology(
            network
        )

        validateContext(
            engineeringContext
        )

        val upstream =
            upstreamEngineering
                ?: SldUpstreamEngineering.calculate(
                    network = network,
                    engineeringContext =
                        engineeringContext
                )

        val shortCircuit =
            shortCircuitStudy
                ?: SldShortCircuitEngine.calculate(
                    network = network,
                    voltageFactor =
                        engineeringContext.voltageFactor
                )

        return SldCableSizingEngine.calculate(
            network = network,
            shortCircuitStudy =
                shortCircuit,
            voltageDropLimitPercent =
                engineeringContext
                    .effectiveVoltageDropLimitPercent(),
            shortCircuitTimeSeconds =
                engineeringContext
                    .shortCircuitTimeSeconds,
            upstreamEngineering =
                upstream,
            engineeringContext =
                engineeringContext
        )
    }

    // ============================================================
    // PANEL SCHEDULE
    // ============================================================

    fun calculatePanelSchedule(
        network: SldNetwork,
        panelNodeId: String,
        cableSizingStudy:
            SldCableSizingStudy? = null,
        shortCircuitStudy:
            SldShortCircuitStudy? = null
    ): SldPanelSchedule {

        requireEngineeringTopology(
            network
        )

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
            cableSizingStudy =
                cableSizingStudy,
            shortCircuitStudy =
                shortCircuitStudy
        )
    }

    /**
     * Context-aware panel schedule.
     */
    fun calculatePanelSchedule(
        network: SldNetwork,
        panelNodeId: String,
        engineeringContext:
            SldEngineeringContext,
        cableSizingStudy:
            SldCableSizingStudy? = null,
        shortCircuitStudy:
            SldShortCircuitStudy? = null
    ): SldPanelSchedule {

        requireEngineeringTopology(
            network
        )

        validateContext(
            engineeringContext
        )

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
            cableSizingStudy =
                cableSizingStudy,
            shortCircuitStudy =
                shortCircuitStudy
        )
    }

    // ============================================================
    // PROTECTION COORDINATION
    // ============================================================

    fun calculateProtectionCoordination(
        network: SldNetwork,
        shortCircuitStudy:
            SldShortCircuitStudy? = null,
        cableSizingStudy:
            SldCableSizingStudy? = null,
        upstreamEngineering:
            SldUpstreamEngineering.Result? = null
    ): SldProtectionCoordinationResult {

        requireEngineeringTopology(
            network
        )

        val upstream =
            upstreamEngineering
                ?: SldUpstreamEngineering.calculate(
                    network = network
                )

        val shortCircuit =
            shortCircuitStudy
                ?: SldShortCircuitEngine.calculate(
                    network = network
                )

        val cableSizing =
            cableSizingStudy
                ?: SldCableSizingEngine.calculate(
                    network = network,
                    shortCircuitStudy =
                        shortCircuit,
                    upstreamEngineering =
                        upstream
                )

        return SldProtectionCoordinationEngine.calculate(
            network = network,
            shortCircuitStudy =
                shortCircuit,
            cableSizingStudy =
                cableSizing,
            upstreamEngineering =
                upstream
        )
    }

    /**
     * Context-aware protection coordination.
     */
    fun calculateProtectionCoordination(
        network: SldNetwork,
        engineeringContext:
            SldEngineeringContext,
        shortCircuitStudy:
            SldShortCircuitStudy? = null,
        cableSizingStudy:
            SldCableSizingStudy? = null,
        upstreamEngineering:
            SldUpstreamEngineering.Result? = null
    ): SldProtectionCoordinationResult {

        requireEngineeringTopology(
            network
        )

        validateContext(
            engineeringContext
        )

        val upstream =
            upstreamEngineering
                ?: SldUpstreamEngineering.calculate(
                    network = network,
                    engineeringContext =
                        engineeringContext
                )

        val shortCircuit =
            shortCircuitStudy
                ?: SldShortCircuitEngine.calculate(
                    network = network,
                    voltageFactor =
                        engineeringContext.voltageFactor
                )

        val cableSizing =
            cableSizingStudy
                ?: SldCableSizingEngine.calculate(
                    network = network,
                    shortCircuitStudy =
                        shortCircuit,
                    voltageDropLimitPercent =
                        engineeringContext
                            .effectiveVoltageDropLimitPercent(),
                    shortCircuitTimeSeconds =
                        engineeringContext
                            .shortCircuitTimeSeconds,
                    upstreamEngineering =
                        upstream,
                    engineeringContext =
                        engineeringContext
                )

        return SldProtectionCoordinationEngine.calculate(
            network = network,
            shortCircuitStudy =
                shortCircuit,
            cableSizingStudy =
                cableSizing,
            upstreamEngineering =
                upstream,
            engineeringContext =
                engineeringContext
        )
    }

    // ============================================================
    // COMPLETE ENGINEERING STUDY
    // ============================================================

    /**
     * Backward-compatible complete engineering entry point.
     *
     * The topology gate is executed INSIDE the engineering facade.
     *
     * Therefore even an external caller such as
     * DesignProjectCoreBridge cannot accidentally send an incomplete
     * drawing into the engineering engines.
     */
    fun calculateComplete(
        network: SldNetwork,
        panelNodeId: String? = null,
        voltageFactor: Double = 1.05,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {

        val context =
            SldEngineeringContext(
                voltageFactor =
                    voltageFactor,
                voltageDropLimitPercent =
                    voltageDropLimitPercent,
                shortCircuitTimeSeconds =
                    shortCircuitTimeSeconds
            )

        return calculateComplete(
            network = network,
            panelNodeId = panelNodeId,
            engineeringContext = context
        )
    }

    /**
     * Complete context-aware engineering study.
     *
     * HARD ORDER:
     *
     * 1. topology gate
     * 2. context validation
     * 3. upstream
     * 4. short circuit
     * 5. cable sizing
     * 6. protection
     * 7. panel schedule
     */
    fun calculateComplete(
        network: SldNetwork,
        panelNodeId: String? = null,
        engineeringContext:
            SldEngineeringContext
    ): SldEngineeringPackage {

        /*
         * ========================================================
         * HARD TOPOLOGY GATE
         * ========================================================
         *
         * THIS IS THE IMPORTANT FIX.
         *
         * No engineering engine is allowed to run before this
         * succeeds.
         *
         * A disconnected LOAD/BREAKER therefore becomes an editor
         * state instead of an application crash.
         */
        requireEngineeringTopology(
            network
        )

        validateContext(
            engineeringContext
        )

        // --------------------------------------------------------
        // 1. UPSTREAM ENGINEERING
        // --------------------------------------------------------

        val upstream =
            SldUpstreamEngineering.calculate(
                network = network,
                engineeringContext =
                    engineeringContext
            )

        // --------------------------------------------------------
        // 2. SHORT CIRCUIT
        // --------------------------------------------------------

        val shortCircuit =
            SldShortCircuitEngine.calculate(
                network = network,
                voltageFactor =
                    engineeringContext.voltageFactor
            )

        // --------------------------------------------------------
        // 3. CABLE SIZING
        // --------------------------------------------------------

        val cableSizing =
            SldCableSizingEngine.calculate(
                network = network,
                shortCircuitStudy =
                    shortCircuit,
                voltageDropLimitPercent =
                    engineeringContext
                        .effectiveVoltageDropLimitPercent(),
                shortCircuitTimeSeconds =
                    engineeringContext
                        .shortCircuitTimeSeconds,
                upstreamEngineering =
                    upstream,
                engineeringContext =
                    engineeringContext
            )

        // --------------------------------------------------------
        // 4. PROTECTION COORDINATION
        // --------------------------------------------------------

        val protection =
            SldProtectionCoordinationEngine.calculate(
                network = network,
                shortCircuitStudy =
                    shortCircuit,
                cableSizingStudy =
                    cableSizing,
                upstreamEngineering =
                    upstream,
                engineeringContext =
                    engineeringContext
            )

        // --------------------------------------------------------
        // 5. PANEL SCHEDULE
        // --------------------------------------------------------

        val panelSchedule =
            panelNodeId?.let { id ->

                calculatePanelSchedule(
                    network = network,
                    panelNodeId = id,
                    engineeringContext =
                        engineeringContext,
                    cableSizingStudy =
                        cableSizing,
                    shortCircuitStudy =
                        shortCircuit
                )
            }

        // --------------------------------------------------------
        // 6. FINAL PACKAGE
        // --------------------------------------------------------

        return SldEngineeringPackage(
            upstream =
                upstream,

            shortCircuit =
                shortCircuit,

            cableSizing =
                cableSizing,

            protectionCoordination =
                protection,

            panelSchedule =
                panelSchedule,

            engineeringContext =
                engineeringContext
        )
    }

    // ============================================================
    // CONTEXT VALIDATION
    // ============================================================

    private fun validateContext(
        engineeringContext:
            SldEngineeringContext
    ) {

        engineeringContext
            .copy(
                requireImplementedStandard =
                    false
            )
            .validate()
    }

    // ============================================================
    // LEGACY NETWORK VALIDATION
    // ============================================================

    /**
     * Kept for compatibility with any existing code that may use
     * the previous private validation structure.
     *
     * IMPORTANT:
     *
     * This validator is diagnostic validation.
     *
     * Engineering readiness is decided by SldTopologyEngine.
     */
    private fun validateNetwork(
        network: SldNetwork
    ) {

        require(
            network.nodes.isNotEmpty()
        ) {
            "SLD network is empty."
        }

        val validation =
            SldDesignValidator.validate(
                network
            )

        require(
            validation.valid
        ) {
            buildValidationMessage(
                validation
            )
        }
    }

    // ============================================================
    // VALIDATION MESSAGE
    // ============================================================

    private fun buildValidationMessage(
        validation:
            SldDesignValidator.Result
    ): String {

        return buildString {

            appendLine(
                "SLD topology validation failed."
            )

            if (
                validation.errors.isNotEmpty()
            ) {

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

            if (
                validation.warnings.isNotEmpty()
            ) {

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

    val upstream:
        SldUpstreamEngineering.Result,

    val shortCircuit:
        SldShortCircuitStudy,

    val cableSizing:
        SldCableSizingStudy,

    val protectionCoordination:
        SldProtectionCoordinationResult,

    val panelSchedule:
        SldPanelSchedule?,

    val engineeringContext:
        SldEngineeringContext =
            SldEngineeringContext.default()
)
