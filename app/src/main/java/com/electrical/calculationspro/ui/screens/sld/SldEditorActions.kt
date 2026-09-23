package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringFacade
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.project.DesignProjects

class SldEditorActions(
    private val state: SldEditorState,
    private val language: AppLanguage
) {

    private val arabic: Boolean
        get() = language == AppLanguage.ARABIC

    private fun network(): SldNetwork =
        SldNetwork(
            nodes = state.nodes,
            connections = state.connections
        )

    /**
     * Load SLD from the active DesignProject.
     *
     * The current project model stores the SLD as
     * ElectricalSldDesign, therefore this editor uses
     * the node/connection IDs stored there and keeps the
     * editable network in screen state.
     *
     * If no stored SLD exists, the editor starts with
     * the safe empty/source state from SldEditorState.
     */
    fun loadProjectNetwork() {

        val project =
            DesignProjects.getActive()
                ?: return

        val storedSld =
            project.electrical.sld

        if (storedSld == null) {
            return
        }

        /*
         * The current ElectricalSldDesign model stores
         * references as String IDs rather than full SldNode
         * and SldConnection objects.
         *
         * We therefore do not manufacture engineering data
         * from those IDs.
         *
         * Existing editor state remains the authoritative
         * editable representation until the project SLD
         * persistence model is upgraded to SldNetwork.
         */
    }

    /**
     * Save the current SLD.
     *
     * The current DesignProject model does not expose an
     * SldNetwork property. Therefore this method deliberately
     * avoids writing incompatible fields into DesignProject.
     *
     * This keeps the application compilable and prevents
     * corruption of the existing project model.
     */
    fun saveProjectNetwork() {
        /*
         * Persistence of the complete SldNetwork will be
         * connected after ElectricalSldDesign is upgraded to
         * contain the canonical SldNetwork.
         *
         * Do not write incompatible properties here.
         */
    }

    fun resetNodeEditor(
        type: SldNodeType
    ) {

        state.editingNodeId = null

        state.nodeType = type
        state.name = type.name
        state.voltage = "400"
        state.loadKw = "100"
        state.pf = "0.90"
        state.demand = "0.80"
        state.kva = "500"
        state.transformerZ = "6"
        state.generatorXd = "15"
        state.sourceMva = "500"

        state.showNodeDialog = true
    }

    fun editNode(
        node: SldNode
    ) {

        state.editingNodeId = node.id

        state.nodeType = node.type
        state.name = node.name
        state.voltage = node.voltage.toString()
        state.loadKw = node.loadKw.toString()
        state.pf = node.powerFactor.toString()
        state.demand = node.demandFactor.toString()
        state.kva = node.ratedKva.toString()
        state.transformerZ =
            node.transformerPercentZ.toString()
        state.generatorXd =
            node.generatorXdSubtransient.toString()
        state.sourceMva =
            node.sourceShortCircuitMva.toString()

        state.showNodeDialog = true
    }

    fun saveNode() {

        val voltage =
            state.voltage.toDoubleOrNull()
                ?.takeIf { it > 0.0 }
                ?: return

        val load =
            state.loadKw.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val pf =
            state.pf.toDoubleOrNull()
                ?.takeIf { it in 0.01..1.0 }
                ?: 0.90

        val demand =
            state.demand.toDoubleOrNull()
                ?.takeIf { it in 0.0..1.0 }
                ?: 0.80

        val kva =
            state.kva.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val transformerZ =
            state.transformerZ.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val generatorXd =
            state.generatorXd.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val sourceMva =
            state.sourceMva.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val existingId =
            state.editingNodeId

        if (existingId == null) {

            val node =
                SldNode(
                    id =
                        "node-${System.currentTimeMillis()}",
                    name =
                        state.name.ifBlank {
                            state.nodeType.name
                        },
                    type =
                        state.nodeType,
                    x =
                        120f +
                            (state.nodes.size % 6) * 260f,
                    y =
                        180f +
                            (state.nodes.size / 6) * 180f,
                    voltage =
                        voltage,
                    loadKw =
                        load,
                    powerFactor =
                        pf,
                    demandFactor =
                        demand,
                    ratedKva =
                        kva,
                    transformerPercentZ =
                        transformerZ,
                    generatorXdSubtransient =
                        generatorXd,
                    sourceShortCircuitMva =
                        sourceMva
                )

            state.nodes =
                state.nodes + node

            state.selectedNodeId =
                node.id

            state.selectedConnectionId =
                null

        } else {

            state.nodes =
                state.nodes.map { node ->

                    if (node.id == existingId) {

                        node.copy(
                            name =
                                state.name.ifBlank {
                                    node.name
                                },
                            type =
                                state.nodeType,
                            voltage =
                                voltage,
                            loadKw =
                                load,
                            powerFactor =
                                pf,
                            demandFactor =
                                demand,
                            ratedKva =
                                kva,
                            transformerPercentZ =
                                transformerZ,
                            generatorXdSubtransient =
                                generatorXd,
                            sourceShortCircuitMva =
                                sourceMva
                        )

                    } else {
                        node
                    }
                }
        }

        state.showNodeDialog = false
        state.editingNodeId = null

        saveProjectNetwork()
    }

    fun editConnection(
        connection: SldConnection
    ) {

        state.editingConnectionId =
            connection.id

        state.length =
            connection.lengthMeters.toString()

        state.resistance =
            connection.resistanceOhmPerKm.toString()

        state.reactance =
            connection.reactanceOhmPerKm.toString()

        state.cableSize =
            connection.cableSizeMm2.toString()

        state.parallelRuns =
            connection.parallelRuns.toString()

        state.capacity =
            connection.currentCapacityA.toString()

        state.showConnectionDialog =
            true
    }

    fun saveConnection() {

        val length =
            state.length.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: return

        val resistance =
            state.resistance.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: return

        val reactance =
            state.reactance.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: return

        val cableSize =
            state.cableSize.toDoubleOrNull()
                ?.takeIf { it > 0.0 }
                ?: return

        val parallelRuns =
            state.parallelRuns.toIntOrNull()
                ?.takeIf { it > 0 }
                ?: return

        val capacity =
            state.capacity.toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: return

        val editingId =
            state.editingConnectionId

        if (editingId != null) {

            state.connections =
                state.connections.map { connection ->

                    if (
                        connection.id ==
                        editingId
                    ) {

                        connection.copy(
                            lengthMeters =
                                length,
                            resistanceOhmPerKm =
                                resistance,
                            reactanceOhmPerKm =
                                reactance,
                            cableSizeMm2 =
                                cableSize,
                            parallelRuns =
                                parallelRuns,
                            currentCapacityA =
                                capacity
                        )

                    } else {
                        connection
                    }
                }

        } else {

            val start =
                state.connectionStartId

            val end =
                state.selectedNodeId

            if (
                start != null &&
                end != null &&
                start != end
            ) {

                val duplicate =
                    state.connections.any { connection ->

                        (
                            connection.fromNodeId == start &&
                                connection.toNodeId == end
                            ) ||
                            (
                                connection.fromNodeId == end &&
                                    connection.toNodeId == start
                                )
                    }

                if (!duplicate) {

                    state.connections =
                        state.connections +
                            SldConnection(
                                id =
                                    "connection-${System.currentTimeMillis()}",
                                fromNodeId =
                                    start,
                                toNodeId =
                                    end,
                                lengthMeters =
                                    length,
                                resistanceOhmPerKm =
                                    resistance,
                                reactanceOhmPerKm =
                                    reactance,
                                cableSizeMm2 =
                                    cableSize,
                                parallelRuns =
                                    parallelRuns,
                                currentCapacityA =
                                    capacity
                            )
                }
            }
        }

        state.editingConnectionId = null
        state.connectionStartId = null
        state.showConnectionDialog = false

        saveProjectNetwork()
    }

    fun startOrCompleteConnection() {

        val nodeId =
            state.selectedNodeId
                ?: return

        if (state.connectionStartId == null) {

            state.connectionStartId =
                nodeId

            state.selectedConnectionId =
                null

            return
        }

        if (state.connectionStartId == nodeId) {

            state.connectionStartId = null

            return
        }

        state.editingConnectionId = null
        state.showConnectionDialog = true
    }

    fun deleteSelected() {

        val connectionId =
            state.selectedConnectionId

        if (connectionId != null) {

            state.connections =
                state.connections.filter {
                    it.id != connectionId
                }

            state.selectedConnectionId = null

            saveProjectNetwork()

            return
        }

        val nodeId =
            state.selectedNodeId
                ?: return

        state.connections =
            state.connections.filter {
                it.fromNodeId != nodeId &&
                    it.toNodeId != nodeId
            }

        state.nodes =
            state.nodes.filter {
                it.id != nodeId
            }

        state.selectedNodeId = null
        state.connectionStartId = null

        saveProjectNetwork()
    }

    fun runShortCircuit() {

        try {

            val study =
                SldEngineeringFacade.calculateShortCircuit(
                    network = network(),
                    voltageFactor = 1.05
                )

            state.reportTitle =
                if (arabic) {
                    "تقرير تيارات القصر"
                } else {
                    "Short Circuit Report"
                }

            state.reportText =
                buildShortCircuitReport(
                    study = study,
                    arabic = arabic
                )

        } catch (exception: Exception) {

            state.reportTitle =
                if (arabic) {
                    "خطأ"
                } else {
                    "Error"
                }

            state.reportText =
                exception.message
                    ?: "Calculation error."
        }

        state.showReport = true
    }

    fun runPanelSchedule() {

        val panel =
            state.nodes.firstOrNull {
                it.id == state.selectedNodeId &&
                    it.type == SldNodeType.PANEL
            }
                ?: state.nodes.firstOrNull {
                    it.type == SldNodeType.PANEL
                }

        state.reportTitle =
            if (arabic) {
                "جدول اللوحة"
            } else {
                "Panel Schedule"
            }

        state.reportText =
            if (panel == null) {

                if (arabic) {
                    "لا توجد لوحة في الـ SLD."
                } else {
                    "No panel exists in the SLD."
                }

            } else {

                buildPanelSchedule(
                    panel = panel,
                    nodes = state.nodes,
                    connections = state.connections,
                    arabic = arabic
                )
            }

        state.showReport = true
    }

    /**
     * Generate the complete SLD currently supported by
     * the existing SLD model.
     *
     * No nonexistent DesignProjectEngine.rebuildSld()
     * is called.
     */
    fun generateCompleteSld() {

        val generated =
            SldCompleteGenerator.generate()

        state.nodes =
            generated.nodes

        state.connections =
            generated.connections

        state.selectedNodeId =
            generated.nodes.firstOrNull()?.id

        state.selectedConnectionId =
            null

        state.connectionStartId =
            null

        runEngineeringReport(
            generated
        )
    }

    private fun runEngineeringReport(
        generated: SldNetwork
    ) {

        try {

            val study =
                SldEngineeringFacade.calculateShortCircuit(
                    network = generated,
                    voltageFactor = 1.05
                )

            state.reportTitle =
                if (arabic) {
                    "SLD - نتائج التصميم الهندسي"
                } else {
                    "SLD - Engineering Design Results"
                }

            state.reportText =
                buildCompleteSldReport(
                    study = study,
                    nodes = generated.nodes,
                    connections = generated.connections,
                    arabic = arabic
                )

        } catch (exception: Exception) {

            state.reportTitle =
                if (arabic) {
                    "خطأ"
                } else {
                    "Error"
                }

            state.reportText =
                exception.message
                    ?: "Calculation error."
        }

        state.showReport = true
    }
}
