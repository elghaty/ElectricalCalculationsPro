package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringPackage
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
        val project = DesignProjects.getActive() ?: return

        val projectNetwork =
            DesignProjectCoreBridge.getProjectSld(project)

        if (projectNetwork.nodes.isEmpty()) {
            return
        }

        state.nodes = projectNetwork.nodes
        state.connections = projectNetwork.connections
        state.selectedNodeId =
            projectNetwork.nodes.firstOrNull()?.id
        state.selectedConnectionId = null
        state.connectionStartId = null
    }

    fun saveProjectNetwork() {
        val current = network()

        if (current.nodes.isEmpty()) {
            return
        }

        DesignProjectCoreBridge.saveActiveProjectSld(
            network = current,
            name = "Main SLD",
            source = "Electrical Design"
        )
    }

    fun resetNodeEditor(type: SldNodeType) {
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
        val voltage =
            state.voltage
                .toDoubleOrNull()
                ?.takeIf { it > 0.0 }
                ?: return

        val load =
            state.loadKw
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val pf =
            state.pf
                .toDoubleOrNull()
                ?.takeIf { it in 0.01..1.0 }
                ?: 0.90

        val demand =
            state.demand
                .toDoubleOrNull()
                ?.takeIf { it in 0.0..1.0 }
                ?: 0.80

        val kva =
            state.kva
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val transformerZ =
            state.transformerZ
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val generatorXd =
            state.generatorXd
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val sourceMva =
            state.sourceMva
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: 0.0

        val existingId = state.editingNodeId

        if (existingId == null) {
            val node =
                SldNode(
                    id = "node-${System.currentTimeMillis()}",
                    name = state.name.ifBlank {
                        state.nodeType.name
                    },
                    type = state.nodeType,
                    x = 120f +
                        (state.nodes.size % 6) * 260f,
                    y = 180f +
                        (state.nodes.size / 6) * 180f,
                    voltage = voltage,
                    loadKw = load,
                    powerFactor = pf,
                    demandFactor = demand,
                    ratedKva = kva,
                    transformerPercentZ = transformerZ,
                    generatorXdSubtransient = generatorXd,
                    sourceShortCircuitMva = sourceMva
                )

            state.nodes = state.nodes + node
            state.selectedNodeId = node.id
            state.selectedConnectionId = null
        } else {
            state.nodes =
                state.nodes.map { node ->
                    if (node.id == existingId) {
                        node.copy(
                            name = state.name.ifBlank {
                                node.name
                            },
                            type = state.nodeType,
                            voltage = voltage,
                            loadKw = load,
                            powerFactor = pf,
                            demandFactor = demand,
                            ratedKva = kva,
                            transformerPercentZ = transformerZ,
                            generatorXdSubtransient = generatorXd,
                            sourceShortCircuitMva = sourceMva
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

    fun editConnection(connection: SldConnection) {
        state.editingConnectionId = connection.id
        state.length = connection.lengthMeters.toString()
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
        state.showConnectionDialog = true
    }

    fun saveConnection() {
        val length =
            state.length
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: return

        val resistance =
            state.resistance
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: return

        val reactance =
            state.reactance
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: return

        val cableSize =
            state.cableSize
                .toDoubleOrNull()
                ?.takeIf { it > 0.0 }
                ?: return

        val parallelRuns =
            state.parallelRuns
                .toIntOrNull()
                ?.takeIf { it > 0 }
                ?: return

        val capacity =
            state.capacity
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }
                ?: return

        val editingId = state.editingConnectionId

        if (editingId != null) {
            state.connections =
                state.connections.map { connection ->
                    if (connection.id == editingId) {
                        connection.copy(
                            lengthMeters = length,
                            resistanceOhmPerKm = resistance,
                            reactanceOhmPerKm = reactance,
                            cableSizeMm2 = cableSize,
                            parallelRuns = parallelRuns,
                            currentCapacityA = capacity
                        )
                    } else {
                        connection
                    }
                }
        } else {
            val start = state.connectionStartId
            val end = state.selectedNodeId

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
                                fromNodeId = start,
                                toNodeId = end,
                                lengthMeters = length,
                                resistanceOhmPerKm = resistance,
                                reactanceOhmPerKm = reactance,
                                cableSizeMm2 = cableSize,
                                parallelRuns = parallelRuns,
                                currentCapacityA = capacity
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
            state.connectionStartId = nodeId
            state.selectedConnectionId = null
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
        val connectionId = state.selectedConnectionId

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
            val project =
                DesignProjects.getActive()
                    ?: throw IllegalStateException(
                        if (arabic) {
                            "لا يوجد مشروع نشط."
                        } else {
                            "No active project."
                        }
                    )

            val packageResult =
                DesignProjectCoreBridge.calculateProjectSld(
                    project = project,
                    panelNodeId =
                        state.nodes
                            .firstOrNull {
                                it.id == state.selectedNodeId &&
                                    it.type == SldNodeType.PANEL
                            }
                            ?.id
                )

            state.reportTitle =
                if (arabic) {
                    "تقرير تيارات القصر"
                } else {
                    "Short Circuit Report"
                }

            state.reportText =
                buildShortCircuitReport(
                    study = packageResult.shortCircuit,
                    arabic = arabic
                )
        } catch (exception: Exception) {
            state.reportTitle =
                if (arabic) "خطأ" else "Error"

            state.reportText =
                exception.message ?: "Calculation error."
        }

        state.showReport = true
    }

    fun runPanelSchedule() {
        try {
            val project =
                DesignProjects.getActive()
                    ?: throw IllegalStateException(
                        if (arabic) {
                            "لا يوجد مشروع نشط."
                        } else {
                            "No active project."
                        }
                    )

            val panel =
                state.nodes.firstOrNull {
                    it.id == state.selectedNodeId &&
                        it.type == SldNodeType.PANEL
                }
                    ?: state.nodes.firstOrNull {
                        it.type == SldNodeType.PANEL
                    }
                    ?: throw IllegalStateException(
                        if (arabic) {
                            "لا توجد لوحة في الـ SLD."
                        } else {
                            "No panel exists in the SLD."
                        }
                    )

            val result =
                DesignProjectCoreBridge.calculateProjectSld(
                    project = project,
                    panelNodeId = panel.id
                )

            state.reportTitle =
                if (arabic) {
                    "جدول اللوحة"
                } else {
                    "Panel Schedule"
                }

            state.reportText =
                buildPanelSchedule(
                    panel = panel,
                    nodes = state.nodes,
                    connections = state.connections,
                    arabic = arabic
                ) +
                    "\n\n" +
                    buildPanelEngineeringSummary(
                        result = result,
                        arabic = arabic
                    )
        } catch (exception: Exception) {
            state.reportTitle =
                if (arabic) "خطأ" else "Error"

            state.reportText =
                exception.message ?: "Calculation error."
        }

        state.showReport = true
    }

    fun generateCompleteSld() {
        try {
            val project =
                DesignProjects.getActive()
                    ?: throw IllegalStateException(
                        if (arabic) {
                            "لا يوجد مشروع نشط."
                        } else {
                            "No active project."
                        }
                    )

            val generated =
                DesignProjectCoreBridge.buildProjectSld(project)

            if (generated.nodes.isEmpty()) {
                throw IllegalStateException(
                    if (arabic) {
                        "لا توجد بيانات كهربائية كافية لإنشاء SLD."
                    } else {
                        "There is not enough electrical project data to build the SLD."
                    }
                )
            }

            state.nodes = generated.nodes
            state.connections = generated.connections
            state.selectedNodeId =
                generated.nodes.firstOrNull()?.id
            state.selectedConnectionId = null
            state.connectionStartId = null

            DesignProjectCoreBridge.saveActiveProjectSld(
                network = generated,
                name = "Main SLD",
                source = "Electrical Design"
            )

            runCompleteEngineeringReport()
        } catch (exception: Exception) {
            state.reportTitle =
                if (arabic) {
                    "خطأ في إنشاء SLD"
                } else {
                    "SLD Generation Error"
                }

            state.reportText =
                exception.message ?: "SLD generation error."

            state.showReport = true
        }
    }

    private fun runCompleteEngineeringReport() {
        try {
            val project =
                DesignProjects.getActive()
                    ?: throw IllegalStateException(
                        "No active project."
                    )

            val result =
                DesignProjectCoreBridge.calculateProjectSld(
                    project = project
                )

            state.reportTitle =
                if (arabic) {
                    "SLD - نتائج التصميم الهندسي"
                } else {
                    "SLD - Engineering Design Results"
                }

            state.reportText =
                buildCompleteEngineeringReport(
                    result = result,
                    arabic = arabic
                )
        } catch (exception: Exception) {
            state.reportTitle =
                if (arabic) "خطأ" else "Error"

            state.reportText =
                exception.message
                    ?: "Engineering calculation error."
        }

        state.showReport = true
    }

    private fun buildPanelEngineeringSummary(
        result: SldEngineeringPackage,
        arabic: Boolean
    ): String {
        val shortCircuit = result.shortCircuit
        val cableSizing = result.cableSizing
        val protection = result.protectionCoordination
        val panelSchedule = result.panelSchedule

        return buildString {
            appendLine(
                if (arabic) {
                    "ملخص الدراسة الهندسية"
                } else {
                    "Engineering Study Summary"
                }
            )

            appendLine()

            appendLine(
                if (arabic) {
                    "تيار القصر الأقصى: " +
                        "%.3f kA".format(
                            shortCircuit.maximumFaultCurrentKa
                        )
                } else {
                    "Maximum Short Circuit Current: " +
                        "%.3f kA".format(
                            shortCircuit.maximumFaultCurrentKa
                        )
                }
            )

            appendLine(
                if (arabic) {
                    "أقصى تيار قمة: " +
                        "%.3f kA".format(
                            shortCircuit.maximumPeakCurrentKa
                        )
                } else {
                    "Maximum Peak Current: " +
                        "%.3f kA".format(
                            shortCircuit.maximumPeakCurrentKa
                        )
                }
            )

            appendLine(
                if (arabic) {
                    "أقصى قدرة قصر: " +
                        "%.3f MVA".format(
                            shortCircuit.maximumFaultMva
                        )
                } else {
                    "Maximum Fault Level: " +
                        "%.3f MVA".format(
                            shortCircuit.maximumFaultMva
                        )
                }
            )

            appendLine()

            appendLine(
                if (arabic) {
                    "المغذيات المقبولة: " +
                        cableSizing.successfulFeeders
                } else {
                    "Successful Feeders: " +
                        cableSizing.successfulFeeders
                }
            )

            appendLine(
                if (arabic) {
                    "المغذيات التي تحتاج مراجعة: " +
                        cableSizing.failedFeeders
                } else {
                    "Feeders Requiring Review: " +
                        cableSizing.failedFeeders
                }
            )

            appendLine()

            appendLine(
                if (arabic) {
                    "التنسيق المقبول: " +
                        protection.coordinatedPairs
                } else {
                    "Coordinated Pairs: " +
                        protection.coordinatedPairs
                }
            )

            appendLine(
                if (arabic) {
                    "تنسيق يحتاج مراجعة: " +
                        protection.warningPairs
                } else {
                    "Pairs Requiring Review: " +
                        protection.warningPairs
                }
            )

            appendLine(
                if (arabic) {
                    "تنسيق غير مقبول: " +
                        protection.failedPairs
                } else {
                    "Failed Coordination Pairs: " +
                        protection.failedPairs
                }
            )

            panelSchedule?.let {
                appendLine()

                appendLine(
                    if (arabic) {
                        "اللوحة: ${it.panelName}"
                    } else {
                        "Panel: ${it.panelName}"
                    }
                )

                appendLine(
                    if (arabic) {
                        "الحمل المتصل: " +
                            "%.2f kW".format(
                                it.totalConnectedLoadKw
                            )
                    } else {
                        "Connected Load: " +
                            "%.2f kW".format(
                                it.totalConnectedLoadKw
                            )
                    }
                )

                appendLine(
                    if (arabic) {
                        "حمل الطلب: " +
                            "%.2f kW".format(
                                it.totalDemandLoadKw
                            )
                    } else {
                        "Demand Load: " +
                            "%.2f kW".format(
                                it.totalDemandLoadKw
                            )
                    }
                )

                appendLine(
                    if (arabic) {
                        "تيار الطلب: " +
                            "%.2f A".format(
                                it.totalDemandCurrentA
                            )
                    } else {
                        "Demand Current: " +
                            "%.2f A".format(
                                it.totalDemandCurrentA
                            )
                    }
                )

                if (it.notes.isNotEmpty()) {
                    appendLine()

                    appendLine(
                        if (arabic) {
                            "ملاحظات جدول اللوحة:"
                        } else {
                            "Panel Schedule Notes:"
                        }
                    )

                    it.notes.forEach { note ->
                        appendLine("• $note")
                    }
                }
            }

            if (shortCircuit.notes.isNotEmpty()) {
                appendLine()

                appendLine(
                    if (arabic) {
                        "ملاحظات تيارات القصر:"
                    } else {
                        "Short Circuit Notes:"
                    }
                )

                shortCircuit.notes.forEach { note ->
                    appendLine("• $note")
                }
            }

            if (cableSizing.notes.isNotEmpty()) {
                appendLine()

                appendLine(
                    if (arabic) {
                        "ملاحظات الكابلات:"
                    } else {
                        "Cable Sizing Notes:"
                    }
                )

                cableSizing.notes.forEach { note ->
                    appendLine("• $note")
                }
            }

            if (protection.notes.isNotEmpty()) {
                appendLine()

                appendLine(
                    if (arabic) {
                        "ملاحظات الحماية:"
                    } else {
                        "Protection Notes:"
                    }
                )

                protection.notes.forEach { note ->
                    appendLine("• $note")
                }
            }
        }
    }

    private fun buildCompleteEngineeringReport(
        result: SldEngineeringPackage,
        arabic: Boolean
    ): String {
        val shortCircuit = result.shortCircuit
        val cableSizing = result.cableSizing
        val protection = result.protectionCoordination
        val panelSchedule = result.panelSchedule

        return buildString {
            appendLine(
                if (arabic) {
                    "الدراسة الهندسية المتكاملة"
                } else {
                    "Complete Engineering Study"
                }
            )

            appendLine()

            appendLine(
                if (arabic) {
                    "1. تيارات القصر"
                } else {
                    "1. Short Circuit"
                }
            )

            appendLine(
                if (arabic) {
                    "أقصى تيار قصر: " +
                        "%.3f kA".format(
                            shortCircuit.maximumFaultCurrentKa
                        )
                } else {
                    "Maximum Fault Current: " +
                        "%.3f kA".format(
                            shortCircuit.maximumFaultCurrentKa
                        )
                }
            )

            appendLine(
                if (arabic) {
                    "أقصى تيار قمة: " +
                        "%.3f kA".format(
                            shortCircuit.maximumPeakCurrentKa
                        )
                } else {
                    "Maximum Peak Current: " +
                        "%.3f kA".format(
                            shortCircuit.maximumPeakCurrentKa
                        )
                }
            )

            appendLine(
                if (arabic) {
                    "أقصى قدرة قصر: " +
                        "%.3f MVA".format(
                            shortCircuit.maximumFaultMva
                        )
                } else {
                    "Maximum Fault Level: " +
                        "%.3f MVA".format(
                            shortCircuit.maximumFaultMva
                        )
                }
            )

            appendLine()

            appendLine(
                if (arabic) {
                    "2. اختيار الكابلات وهبوط الجهد"
                } else {
                    "2. Cable Sizing & Voltage Drop"
                }
            )

            appendLine(
                if (arabic) {
                    "المغذيات المقبولة: " +
                        cableSizing.successfulFeeders
                } else {
                    "Successful Feeders: " +
                        cableSizing.successfulFeeders
                }
            )

            appendLine(
                if (arabic) {
                    "المغذيات التي تحتاج مراجعة: " +
                        cableSizing.failedFeeders
                } else {
                    "Feeders Requiring Review: " +
                        cableSizing.failedFeeders
                }
            )

            appendLine()

            appendLine(
                if (arabic) {
                    "3. التنسيق والحماية"
                } else {
                    "3. Protection Coordination"
                }
            )

            appendLine(
                if (arabic) {
                    "أزواج التنسيق المقبولة: " +
                        protection.coordinatedPairs
                } else {
                    "Coordinated Pairs: " +
                        protection.coordinatedPairs
                }
            )

            appendLine(
                if (arabic) {
                    "أزواج تحتاج مراجعة: " +
                        protection.warningPairs
                } else {
                    "Pairs Requiring Review: " +
                        protection.warningPairs
                }
            )

            appendLine(
                if (arabic) {
                    "أزواج غير مقبولة: " +
                        protection.failedPairs
                } else {
                    "Failed Coordination Pairs: " +
                        protection.failedPairs
                }
            )

            panelSchedule?.let {
                appendLine()

                appendLine(
                    if (arabic) {
                        "4. جدول اللوحة"
                    } else {
                        "4. Panel Schedule"
                    }
                )

                appendLine(
                    if (arabic) {
                        "اسم اللوحة: ${it.panelName}"
                    } else {
                        "Panel Name: ${it.panelName}"
                    }
                )

                appendLine(
                    if (arabic) {
                        "عدد المغذيات: ${it.rows.size}"
                    } else {
                        "Number of Feeders: ${it.rows.size}"
                    }
                )

                appendLine(
                    if (arabic) {
                        "الحمل المتصل: " +
                            "%.2f kW".format(
                                it.totalConnectedLoadKw
                            )
                    } else {
                        "Connected Load: " +
                            "%.2f kW".format(
                                it.totalConnectedLoadKw
                            )
                    }
                )

                appendLine(
                    if (arabic) {
                        "حمل الطلب: " +
                            "%.2f kW".format(
                                it.totalDemandLoadKw
                            )
                    } else {
                        "Demand Load: " +
                            "%.2f kW".format(
                                it.totalDemandLoadKw
                            )
                    }
                )

                appendLine(
                    if (arabic) {
                        "تيار الطلب: " +
                            "%.2f A".format(
                                it.totalDemandCurrentA
                            )
                    } else {
                        "Demand Current: " +
                            "%.2f A".format(
                                it.totalDemandCurrentA
                            )
                    }
                )

                if (it.notes.isNotEmpty()) {
                    appendLine()

                    appendLine(
                        if (arabic) {
                            "ملاحظات جدول اللوحة:"
                        } else {
                            "Panel Schedule Notes:"
                        }
                    )

                    it.notes.forEach { note ->
                        appendLine("• $note")
                    }
                }
            }

            appendLine()

            appendLine(
                if (arabic) {
                    "ملاحظات عامة:"
                } else {
                    "General Notes:"
                }
            )

            shortCircuit.notes.forEach { note ->
                appendLine("• $note")
            }

            cableSizing.notes.forEach { note ->
                appendLine("• $note")
            }

            protection.notes.forEach { note ->
                appendLine("• $note")
            }
        }
    }
}
