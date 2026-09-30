package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD ENGINEERING REPORT ENGINE
 * ================================================================
 *
 * Single report source for the complete SLD engineering package.
 *
 * IMPORTANT:
 *
 * This class does NOT perform engineering calculations.
 *
 * It consumes the already calculated:
 *
 * SldEngineeringPackage
 *
 * and converts it into a structured report that can be used by:
 *
 * - SLD UI
 * - Engineering report dialog
 * - PDF exporter
 * - Future Excel exporter
 * - Future Word exporter
 *
 * Engineering calculation flow remains:
 *
 * Network
 *   ↓
 * Validation
 *   ↓
 * Upstream
 *   ↓
 * Short Circuit
 *   ↓
 * Cable Sizing
 *   ↓
 * Protection
 *   ↓
 * Panel Schedule
 *   ↓
 * Report
 *
 * ================================================================
 */
object SldEngineeringReportEngine {

    data class ReportSection(
        val title: String,
        val lines: List<String>
    )

    data class Report(
        val title: String,
        val sections: List<ReportSection>
    ) {

        fun asText(): String {

            return buildString {

                appendLine(title)
                appendLine(
                    "============================================================"
                )

                sections.forEachIndexed { index, section ->

                    appendLine()

                    appendLine(
                        "${index + 1}. ${section.title}"
                    )

                    appendLine(
                        "------------------------------------------------------------"
                    )

                    section.lines.forEach { line ->
                        appendLine(line)
                    }
                }
            }
        }
    }

    /**
     * Builds a complete engineering report from an already
     * calculated engineering package.
     */
    fun build(
        network: SldNetwork,
        engineering: SldEngineeringPackage
    ): Report {

        val sections =
            mutableListOf<ReportSection>()

        /*
         * ==========================================================
         * 1. DESIGN BASIS
         * ==========================================================
         */
        sections += ReportSection(
            title = "Design Basis",
            lines = buildDesignBasis(
                engineering = engineering
            )
        )

        /*
         * ==========================================================
         * 2. NETWORK SUMMARY
         * ==========================================================
         */
        sections += ReportSection(
            title = "Network Summary",
            lines = buildNetworkSummary(
                network = network
            )
        )

        /*
         * ==========================================================
         * 3. SLD TOPOLOGY
         * ==========================================================
         */
        sections += ReportSection(
            title = "SLD Topology",
            lines = buildTopology(
                network = network
            )
        )

        /*
         * ==========================================================
         * 4. UPSTREAM ENGINEERING
         * ==========================================================
         */
        sections += ReportSection(
            title = "Upstream Load & Feeder Engineering",
            lines = buildUpstream(
                engineering.upstream
            )
        )

        /*
         * ==========================================================
         * 5. SHORT CIRCUIT
         * ==========================================================
         */
        sections += ReportSection(
            title = "Short Circuit Study",
            lines = buildShortCircuit(
                engineering.shortCircuit
            )
        )

        /*
         * ==========================================================
         * 6. CABLE SIZING
         * ==========================================================
         */
        sections += ReportSection(
            title = "Cable Sizing Study",
            lines = buildCableSizing(
                network = network,
                study = engineering.cableSizing
            )
        )

        /*
         * ==========================================================
         * 7. PROTECTION
         * ==========================================================
         */
        sections += ReportSection(
            title = "Protection Coordination",
            lines = buildProtection(
                engineering.protectionCoordination
            )
        )

        /*
         * ==========================================================
         * 8. PANEL SCHEDULE
         * ==========================================================
         */
        engineering.panelSchedule?.let { panel ->

            sections += ReportSection(
                title = "Panel Schedule",
                lines = buildPanelSchedule(
                    panel = panel
                )
            )
        }

        /*
         * ==========================================================
         * 9. ENGINEERING WARNINGS
         * ==========================================================
         */
        val warnings =
            buildWarnings(
                engineering = engineering
            )

        if (warnings.isNotEmpty()) {

            sections += ReportSection(
                title = "Engineering Warnings",
                lines = warnings
            )
        }

        /*
         * ==========================================================
         * 10. FINAL ENGINEERING SUMMARY
         * ==========================================================
         */
        sections += ReportSection(
            title = "Final Engineering Summary",
            lines = buildFinalSummary(
                engineering = engineering
            )
        )

        return Report(
            title = "Electrical SLD Engineering Report",
            sections = sections
        )
    }

    /*
     * ============================================================
     * DESIGN BASIS
     * ============================================================
     */
    private fun buildDesignBasis(
        engineering: SldEngineeringPackage
    ): List<String> {

        val context =
            engineering.engineeringContext

        return listOf(
            "Electrical standard = ${context.codeName}",
            "Code revision = ${context.codeRevision}",
            "Circuit category = ${context.circuitCategory}",
            "Voltage factor = %.3f".format(
                context.voltageFactor
            ),
            "Voltage-drop design limit = %.2f %%".format(
                context.effectiveVoltageDropLimitPercent()
            ),
            "Short-circuit clearing time = %.2f s".format(
                context.shortCircuitTimeSeconds
            ),
            "Ambient design temperature = %.1f °C".format(
                context.ambientTemperatureC
            ),
            "Number of circuits for grouping = ${context.numberOfCircuits}",
            "Standard implementation status = ${context.standardImplementationStatus}"
        )
    }

    /*
     * ============================================================
     * NETWORK SUMMARY
     * ============================================================
     */
    private fun buildNetworkSummary(
        network: SldNetwork
    ): List<String> {

        return listOf(
            "Equipment count = ${network.nodes.size}",
            "Connection count = ${network.connections.size}",
            "Cable feeder count = ${
                network.connections.count {
                    it.connectionType ==
                        SldConnectionType.CABLE
                }
            }",
            "BUSBAR connection count = ${
                network.connections.count {
                    it.connectionType ==
                        SldConnectionType.BUSBAR
                }
            }",
            "Source count = ${
                network.nodes.count {
                    it.type ==
                        SldNodeType.SOURCE
                }
            }",
            "Transformer count = ${
                network.nodes.count {
                    it.type ==
                        SldNodeType.TRANSFORMER
                }
            }",
            "Generator count = ${
                network.nodes.count {
                    it.type ==
                        SldNodeType.GENERATOR
                }
            }",
            "Panel count = ${
                network.nodes.count {
                    it.type ==
                        SldNodeType.PANEL
                }
            }",
            "Breaker count = ${
                network.nodes.count {
                    it.type ==
                        SldNodeType.BREAKER
                }
            }",
            "Load count = ${
                network.nodes.count {
                    it.type ==
                        SldNodeType.LOAD
                }
            }"
        )
    }

    /*
     * ============================================================
     * TOPOLOGY
     * ============================================================
     */
    private fun buildTopology(
        network: SldNetwork
    ): List<String> {

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val lines =
            mutableListOf<String>()

        network.connections.forEachIndexed { index, connection ->

            val from =
                nodeMap[
                    connection.fromNodeId
                ]

            val to =
                nodeMap[
                    connection.toNodeId
                ]

            val type =
                connection.connectionType.name

            lines +=
                "${index + 1}. " +
                    "${from?.name ?: connection.fromNodeId} " +
                    "→ " +
                    "${to?.name ?: connection.toNodeId} " +
                    "[$type]"

            if (
                connection.connectionType ==
                SldConnectionType.CABLE
            ) {

                lines +=
                    "   Length = %.2f m".format(
                        connection.lengthMeters
                    )

                lines +=
                    "   Cable size entered = %.2f mm²".format(
                        connection.cableSizeMm2
                    )

                lines +=
                    "   Parallel runs = ${connection.parallelRuns}"

            } else {

                lines +=
                    "   Internal panel BUSBAR connection"

                lines +=
                    "   Busbar material = ${connection.busbarMaterial}"

                lines +=
                    "   Busbar rated current = %.0f A".format(
                        connection.busbarRatedCurrentA
                    )

                lines +=
                    "   Busbar short-circuit rating = %.2f kA".format(
                        connection.busbarShortCircuitKA
                    )
            }
        }

        if (lines.isEmpty()) {

            lines +=
                "No connections are currently defined."
        }

        return lines
    }

    /*
     * ============================================================
     * UPSTREAM
     * ============================================================
     */
    private fun buildUpstream(
        upstream: SldUpstreamEngineering.Result
    ): List<String> {

        val lines =
            mutableListOf<String>()

        lines +=
            "Source = ${upstream.sourceName}"

        lines +=
            "Total connected load = %.2f kW".format(
                upstream.totalConnectedKw
            )

        lines +=
            "Total demand load = %.2f kW".format(
                upstream.totalDemandKw
            )

        lines +=
            "Total demand apparent power = %.2f kVA".format(
                upstream.totalKva
            )

        lines +=
            "Source current = %.2f A".format(
                upstream.sourceCurrentA
            )

        lines +=
            "Recommended main breaker = %.0f A".format(
                upstream.recommendedMainBreakerA
            )

        lines +=
            "Recommended transformer capacity = %.0f kVA".format(
                upstream.recommendedTransformerKva
            )

        lines +=
            "Maximum voltage drop = %.3f %%".format(
                upstream.maximumVoltageDropPercent
            )

        lines += ""

        lines += "Node engineering results:"

        upstream.nodes.forEach { node ->

            lines +=
                "Node = ${node.nodeName}"

            lines +=
                "  Connected load = %.2f kW".format(
                    node.connectedKw
                )

            lines +=
                "  Demand load = %.2f kW".format(
                    node.demandKw
                )

            lines +=
                "  Apparent power = %.2f kVA".format(
                    node.kva
                )

            lines +=
                "  Current = %.2f A".format(
                    node.currentA
                )

            lines +=
                "  Recommended breaker = %.0f A".format(
                    node.recommendedBreakerA
                )

            lines +=
                "  Voltage drop = %.3f %%".format(
                    node.voltageDropPercent
                )

            lines +=
                "  Loading = %.2f %%".format(
                    node.loadingPercent
                )

            node.notes.forEach { note ->

                lines +=
                    "  Note: $note"
            }
        }

        lines += ""
        lines += "Feeder engineering results:"

        upstream.feeders.forEach { feeder ->

            lines +=
                "Feeder = ${feeder.fromNodeId} → ${feeder.toNodeId}"

            lines +=
                "  Connected load = %.2f kW".format(
                    feeder.connectedKw
                )

            lines +=
                "  Demand load = %.2f kW".format(
                    feeder.demandKw
                )

            lines +=
                "  Apparent power = %.2f kVA".format(
                    feeder.kva
                )

            lines +=
                "  Current = %.2f A".format(
                    feeder.currentA
                )

            lines +=
                "  Recommended breaker = %.0f A".format(
                    feeder.recommendedBreakerA
                )

            lines +=
                "  Voltage drop = %.3f %%".format(
                    feeder.voltageDropPercent
                )

            lines +=
                "  Cable adequate = ${feeder.cableAdequate}"

            feeder.notes.forEach { note ->

                lines +=
                    "  Note: $note"
            }
        }

        upstream.warnings.forEach { warning ->

            lines +=
                "WARNING: $warning"
        }

        return lines
    }

    /*
     * ============================================================
     * SHORT CIRCUIT
     * ============================================================
     */
    private fun buildShortCircuit(
        study: SldShortCircuitStudy
    ): List<String> {

        val lines =
            mutableListOf<String>()

        lines +=
            "Maximum fault current = %.3f kA".format(
                study.maximumFaultCurrentKa
            )

        lines +=
            "Maximum peak current = %.3f kA".format(
                study.maximumPeakCurrentKa
            )

        lines +=
            "Maximum fault level = %.3f MVA".format(
                study.maximumFaultMva
            )

        lines += ""

        study.results.values.forEach { result ->

            lines +=
                "Node = ${result.nodeName}"

            lines +=
                "  Voltage = %.2f V".format(
                    result.voltageV
                )

            lines +=
                "  R = %.6f Ω".format(
                    result.resistanceOhm
                )

            lines +=
                "  X = %.6f Ω".format(
                    result.reactanceOhm
                )

            lines +=
                "  Z = %.6f Ω".format(
                    result.impedanceOhm
                )

            lines +=
                "  X/R = %.3f".format(
                    result.xrRatio
                )

            lines +=
                "  Ik'' = %.3f kA".format(
                    result.initialSymmetricalCurrentKa
                )

            lines +=
                "  Ip = %.3f kA".format(
                    result.peakCurrentKa
                )

            lines +=
                "  Ith = %.3f kA".format(
                    result.thermalCurrentKa
                )

            lines +=
                "  Fault level = %.3f MVA".format(
                    result.shortCircuitMva
                )

            lines +=
                "  Preliminary breaker requirement = %.3f kA".format(
                    result.breakerRequiredKa
                )

            result.notes.forEach { note ->

                lines +=
                    "  Note: $note"
            }
        }

        study.notes.forEach { note ->

            lines +=
                "Study note: $note"
        }

        return lines
    }

    /*
     * ============================================================
     * CABLE SIZING
     * ============================================================
     */
    private fun buildCableSizing(
        network: SldNetwork,
        study: SldCableSizingStudy
    ): List<String> {

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val lines =
            mutableListOf<String>()

        lines +=
            "Successful feeders = ${study.successfulFeeders}"

        lines +=
            "Feeders requiring review = ${study.failedFeeders}"

        lines += ""

        study.results.values.forEach { result ->

            val from =
                nodeMap[
                    result.fromNodeId
                ]?.name
                    ?: result.fromNodeId

            val to =
                nodeMap[
                    result.toNodeId
                ]?.name
                    ?: result.toNodeId

            lines +=
                "Feeder = $from → $to"

            lines +=
                "  Design current = %.2f A".format(
                    result.designCurrentA
                )

            lines +=
                "  Required current capacity = %.2f A".format(
                    result.requiredCurrentCapacityA
                )

            lines +=
                "  Short-circuit current = %.3f kA".format(
                    result.shortCircuitCurrentKa
                )

            lines +=
                "  Maximum voltage-drop limit = %.2f %%".format(
                    result.maximumVoltageDropPercent
                )

            if (
                result.recommendedSizeMm2 > 0.0
            ) {

                lines +=
                    "  Recommended cable = %.2f mm²".format(
                        result.recommendedSizeMm2
                    )

                lines +=
                    "  Material = ${result.recommendedMaterial}"

                lines +=
                    "  Cores = ${result.recommendedCores}"

                lines +=
                    "  Parallel runs = ${result.recommendedParallelRuns}"

                lines +=
                    "  Current capacity = %.2f A".format(
                        result.recommendedCurrentCapacityA
                    )

                lines +=
                    "  Voltage drop = %.3f %%".format(
                        result.recommendedVoltageDropPercent
                    )

                lines +=
                    "  Short-circuit withstand = %.3f kA".format(
                        result.recommendedShortCircuitWithstandKa
                    )

            } else {

                lines +=
                    "  RECOMMENDATION = NO VERIFIED CABLE ARRANGEMENT"
            }

            result.notes.forEach { note ->

                lines +=
                    "  Note: $note"
            }
        }

        study.notes.forEach { note ->

            lines +=
                "Study note: $note"
        }

        return lines
    }

    /*
     * ============================================================
     * PROTECTION
     * ============================================================
     */
    private fun buildProtection(
        study: SldProtectionCoordinationResult
    ): List<String> {

        val lines =
            mutableListOf<String>()

        lines +=
            "Preliminary coordinated pairs = ${study.coordinatedPairs}"

        lines +=
            "Pairs requiring verification = ${study.warningPairs}"

        lines +=
            "Pairs failing preliminary screening = ${study.failedPairs}"

        lines += ""

        study.devices.values.forEach { device ->

            lines +=
                "Device = ${device.nodeName}"

            lines +=
                "  Type = ${device.deviceType}"

            lines +=
                "  Downstream current = %.2f A".format(
                    device.downstreamCurrentA
                )

            lines +=
                "  Recommended rating = %.0f A".format(
                    device.recommendedRatingA
                )

            lines +=
                "  Required short-circuit rating = %.3f kA".format(
                    device.shortCircuitRatingKa
                )

            lines +=
                "  Preliminary long-time pickup = %.1f A".format(
                    device.longTimePickupA
                )

            lines +=
                "  Preliminary instantaneous pickup = %.1f A".format(
                    device.instantaneousPickupA
                )

            lines +=
                "  Selectivity margin = %.1f A".format(
                    device.selectivityMarginA
                )

            lines +=
                "  Status = ${device.status}"

            device.notes.forEach { note ->

                lines +=
                    "  Note: $note"
            }
        }

        study.notes.forEach { note ->

            lines +=
                "Study note: $note"
        }

        return lines
    }

    /*
     * ============================================================
     * PANEL SCHEDULE
     * ============================================================
     */
    private fun buildPanelSchedule(
        panel: SldPanelSchedule
    ): List<String> {

        val lines =
            mutableListOf<String>()

        lines +=
            "Panel = ${panel.panelName}"

        lines +=
            "Panel voltage = %.2f V".format(
                panel.panelVoltageV
            )

        lines +=
            "Total connected load = %.2f kW".format(
                panel.totalConnectedLoadKw
            )

        lines +=
            "Total demand load = %.2f kW".format(
                panel.totalDemandLoadKw
            )

        lines +=
            "Total demand current = %.2f A".format(
                panel.totalDemandCurrentA
            )

        lines += ""

        panel.rows.forEachIndexed { index, row ->

            lines +=
                "${index + 1}. ${row.feederName}"

            lines +=
                "  Design current = %.2f A".format(
                    row.designCurrentA
                )

            lines +=
                "  Recommended breaker = %.0f A".format(
                    row.recommendedBreakerA
                )

            lines +=
                "  Cable size = %.2f mm²".format(
                    row.cableSizeMm2
                )

            lines +=
                "  Parallel runs = ${row.parallelRuns}"

            lines +=
                "  Current capacity = %.2f A".format(
                    row.currentCapacityA
                )

            lines +=
                "  Voltage drop = %.3f %%".format(
                    row.voltageDropPercent
                )

            lines +=
                "  Short-circuit current = %.3f kA".format(
                    row.shortCircuitCurrentKa
                )

            lines +=
                "  Status = ${row.status}"

            row.notes.forEach { note ->

                lines +=
                    "  Note: $note"
            }
        }

        panel.notes.forEach { note ->

            lines +=
                "Panel note: $note"
        }

        return lines
    }

    /*
     * ============================================================
     * WARNINGS
     * ============================================================
     */
    private fun buildWarnings(
        engineering: SldEngineeringPackage
    ): List<String> {

        val warnings =
            linkedSetOf<String>()

        engineering.upstream.warnings.forEach {
            warnings +=
                "Upstream: $it"
        }

        engineering.shortCircuit.notes
            .filter {
                it.contains(
                    "missing",
                    ignoreCase = true
                ) ||
                    it.contains(
                        "not available",
                        ignoreCase = true
                    ) ||
                    it.contains(
                        "insufficient",
                        ignoreCase = true
                    )
            }
            .forEach {
                warnings +=
                    "Short circuit: $it"
            }

        engineering.cableSizing.notes.forEach {
            warnings +=
                "Cable sizing: $it"
        }

        engineering.protectionCoordination
            .notes
            .filter {
                it.contains(
                    "require",
                    ignoreCase = true
                ) ||
                    it.contains(
                        "warning",
                        ignoreCase = true
                    ) ||
                    it.contains(
                        "failed",
                        ignoreCase = true
                    )
            }
            .forEach {
                warnings +=
                    "Protection: $it"
            }

        engineering.panelSchedule
            ?.notes
            ?.forEach {
                warnings +=
                    "Panel schedule: $it"
            }

        if (
            !engineering.engineeringContext.standardImplemented
        ) {

            warnings +=
                "Selected engineering standard reports an incomplete implementation dataset: " +
                    engineering.engineeringContext.standardImplementationStatus
        }

        return warnings
            .map {
                "WARNING: $it"
            }
    }

    /*
     * ============================================================
     * FINAL SUMMARY
     * ============================================================
     */
    private fun buildFinalSummary(
        engineering: SldEngineeringPackage
    ): List<String> {

        val lines =
            mutableListOf<String>()

        lines +=
            "Total connected load = %.2f kW".format(
                engineering
                    .upstream
                    .totalConnectedKw
            )

        lines +=
            "Total demand load = %.2f kW".format(
                engineering
                    .upstream
                    .totalDemandKw
            )

        lines +=
            "Total demand apparent power = %.2f kVA".format(
                engineering
                    .upstream
                    .totalKva
            )

        lines +=
            "Source current = %.2f A".format(
                engineering
                    .upstream
                    .sourceCurrentA
            )

        lines +=
            "Recommended main breaker = %.0f A".format(
                engineering
                    .upstream
                    .recommendedMainBreakerA
            )

        lines +=
            "Recommended transformer capacity = %.0f kVA".format(
                engineering
                    .upstream
                    .recommendedTransformerKva
            )

        lines +=
            "Maximum voltage drop = %.3f %%".format(
                engineering
                    .upstream
                    .maximumVoltageDropPercent
            )

        lines +=
            "Maximum short-circuit current = %.3f kA".format(
                engineering
                    .shortCircuit
                    .maximumFaultCurrentKa
            )

        lines +=
            "Cable feeders successfully sized = ${
                engineering
                    .cableSizing
                    .successfulFeeders
            }"

        lines +=
            "Cable feeders requiring review = ${
                engineering
                    .cableSizing
                    .failedFeeders
            }"

        lines +=
            "Preliminary coordinated protection pairs = ${
                engineering
                    .protectionCoordination
                    .coordinatedPairs
            }"

        lines +=
            "Protection pairs requiring verification = ${
                engineering
                    .protectionCoordination
                    .warningPairs
            }"

        lines +=
            "Protection pairs failing preliminary screening = ${
                engineering
                    .protectionCoordination
                    .failedPairs
            }"

        lines += ""

        lines +=
            "FINAL STATUS = " +
                determineFinalStatus(
                    engineering
                )

        lines += ""

        lines +=
            "This report is generated from the current SLD engineering package."
        
        lines +=
            "Changing the SLD requires a new engineering calculation cycle before issuing the report."

        return lines
    }

    private fun determineFinalStatus(
        engineering: SldEngineeringPackage
    ): String {

        if (
            engineering
                .cableSizing
                .failedFeeders > 0
        ) {
            return "REVIEW REQUIRED"
        }

        if (
            engineering
                .protectionCoordination
                .failedPairs > 0
        ) {
            return "REVIEW REQUIRED"
        }

        if (
            !engineering
                .engineeringContext
                .standardImplemented
        ) {
            return "REVIEW REQUIRED"
        }

        return "PRELIMINARY ENGINEERING STUDY COMPLETE"
    }
}
