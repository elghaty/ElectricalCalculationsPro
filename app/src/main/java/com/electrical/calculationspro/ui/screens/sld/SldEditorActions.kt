package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldAutoLayoutEngine
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldDesignValidator
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldEngineeringReportEngine
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
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

    fun loadProjectNetwork() {
        try {
            val project = DesignProjects.getActive()

            if (project != null) {
                val loaded = DesignProjectCoreBridge.loadSldNetwork(project)

                if (loaded != null) {
                    state.nodes = loaded.nodes
                    state.connections = loaded.connections
                }
            }

            state.engineeringError = null
        } catch (e: Exception) {
            state.engineeringError = e.message ?: "Unable to load SLD project"
        }
    }

    fun saveProjectNetwork() {
        try {
            val project = DesignProjects.getActive()

            if (project != null) {
                DesignProjectCoreBridge.saveSldNetwork(
                    project = project,
                    network = network()
                )
            }
        } catch (e: Exception) {
            state.engineeringError = e.message ?: "Unable to save SLD project"
        }
    }

    fun recalculateEngineering() {
        try {
            state.engineeringError = null

            val project = DesignProjects.getActive()

            if (project == null) {
                state.engineeringError =
                    if (arabic) {
                        "لا يوجد مشروع نشط."
                    } else {
                        "No active design project."
                    }
                return
            }

            val result =
                DesignProjectCoreBridge.calculateSldEngineering(
                    project = project,
                    network = network()
                )

            state.engineeringPackage = result
        } catch (e: Exception) {
            state.engineeringPackage = null
            state.engineeringError =
                e.message
                    ?: if (arabic) {
                        "تعذر تنفيذ الحسابات الهندسية."
                    } else {
                        "Engineering calculation failed."
                    }
        }
    }

    fun validateDesign() {
        try {
            val result =
                SldDesignValidator.validate(
                    network()
                )

            state.reportTitle =
                if (arabic) {
                    "فحص التصميم"
                } else {
                    "DESIGN VALIDATION"
                }

            state.reportText =
                buildValidationReport(result)

            state.showReport = true
        } catch (e: Exception) {
            state.engineeringError =
                e.message ?: "Validation failed"
        }
    }

    private fun buildValidationReport(result: Any): String {
        return result.toString()
    }

    fun autoLayout() {
        try {
            val result =
                SldAutoLayoutEngine.layout(
                    network()
                )

            state.nodes = result.nodes
            state.connections = result.connections

            saveProjectNetwork()
        } catch (e: Exception) {
            state.engineeringError =
                e.message ?: "Auto layout failed"
        }
    }

    fun resetNodeEditor(type: SldNodeType) {
        state.editingNodeId = null
        state.nodeType = type

        state.name =
            when (type) {
                SldNodeType.SOURCE -> "Utility Source"
                SldNodeType.TRANSFORMER -> "Transformer"
                SldNodeType.GENERATOR -> "Generator"
                SldNodeType.BUS -> "Main Bus"
                SldNodeType.PANEL -> "Panel"
                SldNodeType.BREAKER -> "Breaker"
                SldNodeType.LOAD -> "Load"
            }

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

    fun editNode(node: SldNode) {
        state.editingNodeId = node.id
        state.nodeType = node.type
        state.name = node.name
        state.voltage = node.voltage.toString()
        state.loadKw = node.loadKw.toString()
        state.pf = node.powerFactor.toString()
        state.demand = node.demandFactor.toString()
        state.kva = node.ratedKva.toString()
        state.transformerZ = node.transformerPercentZ.toString()
        state.generatorXd = node.generatorXdSubtransient.toString()
        state.sourceMva = node.sourceShortCircuitMva.toString()
        state.showNodeDialog = true
    }

    fun saveNode() {
        val name =
            state.name.trim().ifEmpty {
                if (arabic) "عنصر" else "Element"
            }

        val voltage =
            state.voltage.toDoubleOrNull()
                ?.coerceAtLeast(1.0)
                ?: 400.0

        val loadKw =
            state.loadKw.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val pf =
            state.pf.toDoubleOrNull()
                ?.coerceIn(0.1, 1.0)
                ?: 0.90

        val demand =
            state.demand.toDoubleOrNull()
                ?.coerceIn(0.0, 1.0)
                ?: 1.0

        val kva =
            state.kva.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val transformerZ =
            state.transformerZ.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val generatorXd =
            state.generatorXd.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val sourceMva =
            state.sourceMva.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val existingId = state.editingNodeId

        if (existingId != null) {
            state.nodes =
                state.nodes.map { old ->
                    if (old.id == existingId) {
                        old.copy(
                            name = name,
                            type = state.nodeType,
                            voltage = voltage,
                            loadKw = loadKw,
                            powerFactor = pf,
                            demandFactor = demand,
                            ratedKva = kva,
                            transformerPercentZ = transformerZ,
                            generatorXdSubtransient = generatorXd,
                            sourceShortCircuitMva = sourceMva
                        )
                    } else {
                        old
                    }
                }
        } else {
            val maxX =
                state.nodes.maxOfOrNull { it.x }
                    ?: 500f

            val maxY =
                state.nodes.maxOfOrNull { it.y }
                    ?: 150f

            val node =
                SldNode(
                    id =
                        "node-" +
                            System.currentTimeMillis(),
                    name = name,
                    type = state.nodeType,
                    x = maxX + 40f,
                    y = maxY + 40f,
                    voltage = voltage,
                    loadKw = loadKw,
                    powerFactor = pf,
                    demandFactor = demand,
                    ratedKva = kva,
                    transformerPercentZ = transformerZ,
                    generatorXdSubtransient = generatorXd,
                    sourceShortCircuitMva = sourceMva
                )

            state.nodes =
                state.nodes + node
        }

        state.clearDialogs()
        saveProjectNetwork()
    }

    fun editConnection(connection: SldConnection) {
        state.editingConnectionId = connection.id
        state.length = connection.lengthMeters.toString()
        state.resistance = connection.resistanceOhmPerKm.toString()
        state.reactance = connection.reactanceOhmPerKm.toString()
        state.cableSize = connection.cableSizeMm2.toString()
        state.parallelRuns =
            connection.parallelRuns.toString()
        state.capacity =
            connection.currentCapacityA.toString()
        state.showConnectionDialog = true
    }

    fun saveConnection() {
        val length =
            state.length.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val resistance =
            state.resistance.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val reactance =
            state.reactance.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val cableSize =
            state.cableSize.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val runs =
            state.parallelRuns.toIntOrNull()
                ?.coerceAtLeast(1)
                ?: 1

        val capacity =
            state.capacity.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val existingId =
            state.editingConnectionId

        if (existingId != null) {
            state.connections =
                state.connections.map { old ->
                    if (old.id == existingId) {
                        old.copy(
                            lengthMeters = length,
                            resistanceOhmPerKm = resistance,
                            reactanceOhmPerKm = reactance,
                            cableSizeMm2 = cableSize,
                            parallelRuns = runs,
                            currentCapacityA = capacity
                        )
                    } else {
                        old
                    }
                }
        } else {
            val fromId =
                state.connectionStartId

            val toId =
                state.selectedNodeId

            if (fromId != null && toId != null && fromId != toId) {
                state.connections =
                    state.connections +
                        SldConnection(
                            id =
                                "connection-" +
                                    System.currentTimeMillis(),
                            fromNodeId = fromId,
                            toNodeId = toId,
                            lengthMeters = length,
                            resistanceOhmPerKm = resistance,
                            reactanceOhmPerKm = reactance,
                            cableSizeMm2 = cableSize,
                            parallelRuns = runs,
                            currentCapacityA = capacity
                        )
            }
        }

        state.clearDialogs()
        saveProjectNetwork()
    }

    fun startOrCompleteConnection() {
        val selected =
            state.selectedNodeId ?: return

        val start =
            state.connectionStartId

        if (start == null) {
            state.connectionStartId = selected
            return
        }

        if (start == selected) {
            state.connectionStartId = null
            return
        }

        state.editingConnectionId = null
        state.length = "50"
        state.resistance = "0.125"
        state.reactance = "0.080"
        state.cableSize = "240"
        state.parallelRuns = "1"
        state.capacity = "350"

        state.showConnectionDialog = true
    }

    fun deleteSelected() {
        val nodeId =
            state.selectedNodeId

        val connectionId =
            state.selectedConnectionId

        if (nodeId != null) {
            state.nodes =
                state.nodes.filterNot {
                    it.id == nodeId
                }

            state.connections =
                state.connections.filter {
                    it.fromNodeId != nodeId &&
                        it.toNodeId != nodeId
                }
        }

        if (connectionId != null) {
            state.connections =
                state.connections.filterNot {
                    it.id == connectionId
                }
        }

        state.clearSelection()
        saveProjectNetwork()
    }

    fun runShortCircuit() {
        try {
            recalculateEngineering()

            state.reportTitle =
                if (arabic) {
                    "دراسة تيار القصر"
                } else {
                    "SHORT CIRCUIT STUDY"
                }

            state.reportText =
                buildShortCircuitReportText()

            state.showReport = true
        } catch (e: Exception) {
            state.engineeringError =
                e.message ?: "Short circuit calculation failed"
        }
    }

    private fun buildShortCircuitReportText(): String {
        val builder = StringBuilder()

        builder.appendLine(
            if (arabic) {
                "دراسة تيارات القصر"
            } else {
                "SHORT CIRCUIT STUDY"
            }
        )

        builder.appendLine("--------------------------------")

        state.nodes.forEach { node ->
            builder.appendLine(
                "${node.name} | " +
                    "${node.voltage.toEngineeringString()} V"
            )

            if (node.sourceShortCircuitMva > 0.0) {
                builder.appendLine(
                    "Fault Level = " +
                        "${node.sourceShortCircuitMva.toEngineeringString()} MVA"
                )
            }

            if (node.transformerPercentZ > 0.0) {
                builder.appendLine(
                    "Transformer Z = " +
                        "${node.transformerPercentZ.toEngineeringString()} %"
                )
            }

            if (node.generatorXdSubtransient > 0.0) {
                builder.appendLine(
                    "Generator Xd'' = " +
                        "${node.generatorXdSubtransient.toEngineeringString()} %"
                )
            }
        }

        return builder.toString()
    }

    fun runPanelSchedule() {
        try {
            state.reportTitle =
                if (arabic) {
                    "جدول اللوحات"
                } else {
                    "PANEL SCHEDULE"
                }

            state.reportText =
                buildPanelSchedule()

            state.showReport = true
        } catch (e: Exception) {
            state.engineeringError =
                e.message ?: "Panel schedule failed"
        }
    }

    private fun buildPanelSchedule(): String {
        val builder = StringBuilder()

        builder.appendLine(
            if (arabic) {
                "جدول اللوحات"
            } else {
                "PANEL SCHEDULE"
            }
        )

        builder.appendLine("--------------------------------")

        state.nodes
            .filter {
                it.type == SldNodeType.PANEL
            }
            .forEach { panel ->

                builder.appendLine(
                    "Panel: ${panel.name}"
                )

                builder.appendLine(
                    "Voltage: " +
                        "${panel.voltage.toEngineeringString()} V"
                )

                builder.appendLine(
                    "Rating: " +
                        "${panel.ratedKva.toEngineeringString()} kVA"
                )

                state.connections
                    .filter {
                        it.fromNodeId == panel.id
                    }
                    .forEach { feeder ->

                        val load =
                            state.nodes.firstOrNull {
                                it.id == feeder.toNodeId
                            }

                        if (load != null) {
                            builder.appendLine(
                                "  -> ${load.name} | " +
                                    "${load.loadKw.toEngineeringString()} kW"
                            )
                        }
                    }

                builder.appendLine()
            }

        return builder.toString()
    }

    fun generateCompleteSld() {
        try {
            recalculateEngineering()

            state.reportTitle =
                if (arabic) {
                    "الدراسة الهندسية الكاملة"
                } else {
                    "COMPLETE ENGINEERING STUDY"
                }

            state.reportText =
                SldEngineeringReportEngine.generate(
                    network = network(),
                    engineering = state.engineeringPackage,
                    arabic = arabic
                )

            state.showReport = true
        } catch (e: Exception) {
            state.engineeringError =
                e.message ?: "Complete SLD study failed"
        }
    }

    fun runCompleteEngineeringReport() {
        generateCompleteSld()
    }

    private fun Double.toEngineeringString(): String =
        "%.2f".format(this)
}
