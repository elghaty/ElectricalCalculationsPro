package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD ENGINEERING REPORT ENGINE
 * ================================================================
 *
 * Converts engineering results into a structured report.
 *
 * This layer is deliberately independent from Compose UI.
 * The same report can later be exported to:
 *
 * PDF
 * Excel
 * Word
 * Project database
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
                    "================================================"
                )

                sections.forEachIndexed {
                        index,
                        section
                    ->

                    appendLine()
                    appendLine(
                        "${index + 1}. ${section.title}"
                    )

                    appendLine(
                        "------------------------------------------------"
                    )

                    section.lines.forEach {
                        appendLine(it)
                    }
                }
            }
        }
    }

    fun build(
        network: SldNetwork,
        engineering: SldEngineeringPackage
    ): Report {

        val sections =
            mutableListOf<ReportSection>()

        sections +=
            ReportSection(
                title =
                    "Network Summary",
                lines =
                    listOf(
                        "Equipment count = ${network.nodes.size}",
                        "Feeder count = ${network.connections.size}",
                        "Source count = " +
                            network.nodes.count {
                                it.type ==
                                    SldNodeType.SOURCE
                            },
                        "Panel count = " +
                            network.nodes.count {
                                it.type ==
                                    SldNodeType.PANEL
                            },
                        "Load count = " +
                            network.nodes.count {
                                it.type ==
                                    SldNodeType.LOAD
                            }
                    )
            )

        sections +=
            ReportSection(
                title =
                    "Short Circuit",
                lines =
                    listOf(
                        "Maximum fault current = " +
                            "%.3f kA".format(
                                engineering
                                    .shortCircuit
                                    .maximumFaultCurrentKa
                            ),
                        "Maximum peak current = " +
                            "%.3f kA".format(
                                engineering
                                    .shortCircuit
                                    .maximumPeakCurrentKa
                            ),
                        "Maximum fault level = " +
                            "%.3f MVA".format(
                                engineering
                                    .shortCircuit
                                    .maximumFaultMva
                            )
                    ) +
                        engineering
                            .shortCircuit
                            .notes
                            .map {
                                "Note: $it"
                            }
            )

        sections +=
            ReportSection(
                title =
                    "Cable Sizing",
                lines =
                    listOf(
                        "Successful feeders = " +
                            engineering
                                .cableSizing
                                .successfulFeeders,
                        "Feeders requiring review = " +
                            engineering
                                .cableSizing
                                .failedFeeders
                    ) +
                        engineering
                            .cableSizing
                            .notes
                            .map {
                                "Note: $it"
                            }
            )

        sections +=
            ReportSection(
                title =
                    "Protection Coordination",
                lines =
                    listOf(
                        "Coordinated pairs = " +
                            engineering
                                .protectionCoordination
                                .coordinatedPairs,
                        "Warning pairs = " +
                            engineering
                                .protectionCoordination
                                .warningPairs,
                        "Failed pairs = " +
                            engineering
                                .protectionCoordination
                                .failedPairs
                    ) +
                        engineering
                            .protectionCoordination
                            .notes
                            .map {
                                "Note: $it"
                            }
            )

        engineering.panelSchedule?.let { panel ->

            sections +=
                ReportSection(
                    title =
                        "Panel Schedule",
                    lines =
                        listOf(
                            "Panel = ${panel.panelName}",
                            "Connected load = " +
                                "%.2f kW".format(
                                    panel.totalConnectedLoadKw
                                ),
                            "Demand load = " +
                                "%.2f kW".format(
                                    panel.totalDemandLoadKw
                                ),
                            "Demand current = " +
                                "%.2f A".format(
                                    panel.totalDemandCurrentA
                                )
                        ) +
                            panel.notes.map {
                                "Note: $it"
                            }
                )
        }

        return Report(
            title =
                "Electrical SLD Engineering Report",
            sections =
                sections
        )
    }
}
