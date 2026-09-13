package com.electrical.calculationspro.data

import kotlin.math.sqrt

data class SldPanelScheduleRow(
    val connectionId: String,
    val feederName: String,
    val fromNodeId: String,
    val toNodeId: String,
    val loadKw: Double,
    val powerFactor: Double,
    val demandFactor: Double,
    val designCurrentA: Double,
    val recommendedBreakerA: Double,
    val cableSizeMm2: Double,
    val parallelRuns: Int,
    val currentCapacityA: Double,
    val voltageDropPercent: Double,
    val shortCircuitCurrentKa: Double,
    val status: PanelScheduleStatus,
    val notes: List<String> = emptyList()
)

enum class PanelScheduleStatus {
    PASS,
    WARNING,
    FAIL
}

data class SldPanelSchedule(
    val panelNodeId: String,
    val panelName: String,
    val panelVoltageV: Double,
    val totalConnectedLoadKw: Double,
    val totalDemandLoadKw: Double,
    val totalDemandCurrentA: Double,
    val rows: List<SldPanelScheduleRow>,
    val notes: List<String>
)

object SldPanelScheduleEngine {

    private val breakerRatings = listOf(
        16.0,
        20.0,
        25.0,
        32.0,
        40.0,
        50.0,
        63.0,
        80.0,
        100.0,
        125.0,
        160.0,
        200.0,
        250.0,
        320.0,
        400.0,
        500.0,
        630.0,
        800.0,
        1000.0,
        1250.0,
        1600.0,
        2000.0,
        2500.0,
        3200.0,
        4000.0,
        5000.0,
        6300.0
    )

    fun calculate(
        network: SldNetwork,
        panelNodeId: String,
        cableSizingStudy: SldCableSizingStudy? = null,
        shortCircuitStudy: SldShortCircuitStudy? = null
    ): SldPanelSchedule {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val panel =
            network.nodes.firstOrNull {
                it.id == panelNodeId
            } ?: throw IllegalArgumentException(
                "Panel node was not found."
            )

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val outgoing =
            network.connections.filter {
                it.fromNodeId == panelNodeId
            }

        val rows =
            outgoing.mapNotNull { connection ->

                val destination =
                    nodeMap[connection.toNodeId]
                        ?: return@mapNotNull null

                val loadKw =
                    calculateConnectedLoad(
                        destination,
                        network,
                        nodeMap
                    )

                val demandKw =
                    calculateDemandLoad(
                        destination,
                        network,
                        nodeMap
                    )

                val voltage =
                    if (panel.voltage > 0.0) {
                        panel.voltage
                    } else {
                        destination.voltage
                    }

                val pf =
                    destination.powerFactor
                        .coerceIn(0.01, 1.0)

                val designCurrent =
                    if (
                        demandKw > 0.0 &&
                        voltage > 0.0
                    ) {
                        demandKw * 1000.0 /
                            (
                                sqrt(3.0) *
                                    voltage *
                                    pf
                            )
                    } else {
                        0.0
                    }

                val breaker =
                    selectBreaker(
                        designCurrent * 1.15
                    )

                val cable =
                    cableSizingStudy
                        ?.results
                        ?.get(connection.id)

                val shortCircuit =
                    shortCircuitStudy
                        ?.results
                        ?.get(destination.id)

                val notes =
                    mutableListOf<String>()

                var status =
                    PanelScheduleStatus.PASS

                if (designCurrent <= 0.0) {
                    status =
                        PanelScheduleStatus.WARNING

                    notes.add(
                        "No positive calculated design current."
                    )
                }

                if (
                    cable != null &&
                    cable.recommendedSizeMm2 <= 0.0
                ) {
                    status =
                        PanelScheduleStatus.FAIL

                    notes.add(
                        "No cable arrangement satisfies the sizing criteria."
                    )
                }

                if (
                    cable != null &&
                    cable.recommendedCurrentCapacityA > 0.0 &&
                    breaker >
                        cable.recommendedCurrentCapacityA
                ) {
                    status =
                        PanelScheduleStatus.FAIL

                    notes.add(
                        "Breaker rating exceeds cable current capacity."
                    )
                }

                if (
                    cable != null &&
                    cable.recommendedVoltageDropPercent > 3.0
                ) {
                    status =
                        PanelScheduleStatus.FAIL

                    notes.add(
                        "Voltage drop exceeds the preliminary 3% limit."
                    )
                }

                if (
                    shortCircuit != null &&
                    shortCircuit.breakerRequiredKa >
                        0.0
                ) {
                    if (
                        shortCircuit.breakerRequiredKa >
                        100.0
                    ) {
                        status =
                            PanelScheduleStatus.FAIL

                        notes.add(
                            "Required short-circuit breaking capacity exceeds the catalog range."
                        )
                    }
                } else {
                    notes.add(
                        "Short-circuit study is not available."
                    )
                }

                SldPanelScheduleRow(
                    connectionId =
                        connection.id,
                    feederName =
                        destination.name,
                    fromNodeId =
                        connection.fromNodeId,
                    toNodeId =
                        connection.toNodeId,
                    loadKw =
                        loadKw,
                    powerFactor =
                        pf,
                    demandFactor =
                        destination.demandFactor,
                    designCurrentA =
                        designCurrent,
                    recommendedBreakerA =
                        breaker,
                    cableSizeMm2 =
                        cable?.recommendedSizeMm2
                            ?: connection.cableSizeMm2,
                    parallelRuns =
                        cable?.recommendedParallelRuns
                            ?: connection.parallelRuns,
                    currentCapacityA =
                        cable?.recommendedCurrentCapacityA
                            ?: connection.currentCapacityA,
                    voltageDropPercent =
                        cable?.recommendedVoltageDropPercent
                            ?: connection.voltageDropPercent,
                    shortCircuitCurrentKa =
                        shortCircuit
                            ?.initialSymmetricalCurrentKa
                            ?: 0.0,
                    status =
                        status,
                    notes =
                        notes
                )
            }

        val totalConnectedLoad =
            rows.sumOf {
                it.loadKw
            }

        val totalDemandLoad =
            rows.sumOf {
                it.loadKw *
                    it.demandFactor
            }

        val panelPf =
            panel.powerFactor
                .coerceIn(0.01, 1.0)

        val totalDemandCurrent =
            if (
                totalDemandLoad > 0.0 &&
                panel.voltage > 0.0
            ) {
                totalDemandLoad * 1000.0 /
                    (
                        sqrt(3.0) *
                            panel.voltage *
                            panelPf
                    )
            } else {
                0.0
            }

        val notes =
            mutableListOf<String>()

        if (outgoing.isEmpty()) {
            notes.add(
                "The selected panel has no outgoing feeders."
            )
        }

        if (
            rows.any {
                it.status ==
                    PanelScheduleStatus.FAIL
            }
        ) {
            notes.add(
                "One or more feeders require engineering correction."
            )
        }

        if (
            rows.any {
                it.status ==
                    PanelScheduleStatus.WARNING
            }
        ) {
            notes.add(
                "One or more feeders require additional engineering verification."
            )
        }

        notes.add(
            "Panel schedule is generated from the current SLD topology."
        )

        return SldPanelSchedule(
            panelNodeId =
                panel.id,
            panelName =
                panel.name,
            panelVoltageV =
                panel.voltage,
            totalConnectedLoadKw =
                totalConnectedLoad,
            totalDemandLoadKw =
                totalDemandLoad,
            totalDemandCurrentA =
                totalDemandCurrent,
            rows =
                rows,
            notes =
                notes
        )
    }

    private fun calculateConnectedLoad(
        node: SldNode,
        network: SldNetwork,
        nodeMap: Map<String, SldNode>
    ): Double {

        var total =
            node.loadKw.coerceAtLeast(0.0)

        network.connections
            .filter {
                it.fromNodeId == node.id
            }
            .forEach { connection ->

                val child =
                    nodeMap[connection.toNodeId]
                        ?: return@forEach

                total +=
                    calculateConnectedLoad(
                        child,
                        network,
                        nodeMap
                    )
            }

        return total
    }

    private fun calculateDemandLoad(
        node: SldNode,
        network: SldNetwork,
        nodeMap: Map<String, SldNode>
    ): Double {

        var total =
            node.loadKw.coerceAtLeast(0.0) *
                node.demandFactor
                    .coerceIn(0.0, 1.0)

        network.connections
            .filter {
                it.fromNodeId == node.id
            }
            .forEach { connection ->

                val child =
                    nodeMap[connection.toNodeId]
                        ?: return@forEach

                total +=
                    calculateDemandLoad(
                        child,
                        network,
                        nodeMap
                    )
            }

        return total
    }

    private fun selectBreaker(
        currentA: Double
    ): Double {

        if (currentA <= 0.0) {
            return 0.0
        }

        return breakerRatings.firstOrNull {
            it >= currentA
        } ?: breakerRatings.last()
    }
}
