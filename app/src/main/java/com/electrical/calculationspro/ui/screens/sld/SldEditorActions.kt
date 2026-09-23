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

    fun validateDesign() {
        val current = network()

        val result =
            SldDesignValidator.validate(current)

        state.reportTitle =
            if (arabic) {
                "مراجعة تصميم SLD"
            } else {
                "SLD Design Validation"
            }

        state.reportText =
            buildString {

                appendLine(
                    if (result.valid) {
                        if (arabic) {
                            "التصميم صالح مبدئياً للحسابات الهندسية."
                        } else {
                            "The topology is valid for engineering calculations."
                        }
                    } else {
                        if (arabic) {
                            "يوجد أخطاء يجب إصلاحها قبل اعتماد الحسابات."
                        } else {
                            "Design errors must be corrected before engineering approval."
                        }
                    }
                )

                appendLine()

                appendLine(
                    if (arabic) {
                        "الأخطاء: ${result.errors.size}"
                    } else {
                        "Errors: ${result.errors.size}"
                    }
                )

                result.errors.forEach {
                    appendLine(
                        "• [${it.code}] ${it.message}"
                    )
                }

                appendLine()

                appendLine(
                    if (arabic) {
                        "التحذيرات: ${result.warnings.size}"
                    } else {
                        "Warnings: ${result.warnings.size}"
                    }
                )

                result.warnings.forEach {
                    appendLine(
                        "• [${it.code}] ${it.message}"
                    )
                }

                if (result.information.isNotEmpty()) {

                    appendLine()

                    appendLine(
                        if (arabic) {
                            "معلومات:"
                        } else {
                            "Information:"
                        }
                    )

                    result.information.forEach {
                        appendLine(
                            "• [${it.code}] ${it.message}"
                        )
                    }
                }
            }

        state.showReport = true
    }

    fun autoLayout() {

        val current = network()

        if (current.nodes.isEmpty()) {
            return
        }

        val validation =
            SldDesignValidator.validate(current)

        if (!validation.valid) {
            validateDesign()
            return
        }

        val arranged =
            SldAutoLayoutEngine
                .arrange(current)
                .network

        state.nodes =
            arranged.nodes

        state.connections =
            arranged.connections

        state.selectedNodeId =
            arranged.nodes
                .firstOrNull()
                ?.id

        state.selectedConnectionId = null
        state.connectionStartId = null

        DesignProjectCoreBridge.saveActiveProjectSld(
            network = arranged,
            name = "Main SLD",
            source = "Electrical Design - Auto Layout"
        )
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

        state.voltage =
            node.voltage.toString()

        state.loadKw =
            node.loadKw.toString()

        state.pf =
            node.powerFactor.toString()

        state.demand =
            node.demandFactor.toString()

        state.kva =
            node.ratedKva.toString()

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
            state.voltage
                .toDoubleOrNull()
                ?.takeIf {
                    it > 0.0
                }
                ?: return

        val load =
            state.loadKw
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
                ?: 0.0

        val pf =
            state.pf
                .toDoubleOrNull()
                ?.takeIf {
                    it in 0.01..1.0
                }
                ?: 0.90

        val demand =
            state.demand
                .toDoubleOrNull()
                ?.takeIf {
                    it in 0.0..1.0
                }
                ?: 0.80

        val kva =
            state.kva
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
                ?: 0.0

        val transformerZ =
            state.transformerZ
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
                ?: 0.0

        val generatorXd =
            state.generatorXd
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
                ?: 0.0

        val sourceMva =
            state.sourceMva
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
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
                            (state.nodes.size % 6) *
                            260f,
                    y =
                        180f +
                            (state.nodes.size / 6) *
                            180f,
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

            state.selectedConnectionId = null

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

        state.showConnectionDialog = true
    }

    fun saveConnection() {

        val length =
            state.length
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
                ?: return

        val resistance =
            state.resistance
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
                ?: return

        val reactance =
            state.reactance
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
                ?: return

        val cableSize =
            state.cableSize
                .toDoubleOrNull()
                ?.takeIf {
                    it > 0.0
                }
                ?: return

        val parallelRuns =
            state.parallelRuns
                .toIntOrNull()
                ?.takeIf {
                    it > 0
                }
                ?: return

        val capacity =
            state.capacity
                .toDoubleOrNull()
                ?.takeIf {
                    it >= 0.0
                }
                ?: return

        val editingId =
            state.editingConnectionId

        if (editingId != null) {

            state.connections =
                state.connections.map { connection ->

                    if (connection.id == editingId) {

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
                    state.connections.any {
                        connection ->

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

        if (
            state.connectionStartId == null
        ) {

            state.connectionStartId =
                nodeId

            state.selectedConnectionId =
                null

            return
        }

        if (
            state.connectionStartId == nodeId
        ) {

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

            val project =
                DesignProjects.getActive()
                    ?: throw IllegalStateException(
                        if (arabic) {
                            "لا يوجد مشروع نشط."
                        } else {
                            "No active project."
                        }
                    )

            val current =
                network()

            val validation =
                SldDesignValidator.validate(
                    current
                )

            if (!validation.valid) {
                validateDesign()
                return
            }

            val packageResult =
                DesignProjectCoreBridge.calculateProjectSld(
                    project = project,
                    panelNodeId =
                        state.nodes
                            .firstOrNull {
                                it.id ==
                                    state.selectedNodeId &&
                                    it.type ==
                                    SldNodeType.PANEL
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
                    study =
                        packageResult.shortCircuit,
                    arabic =
                        arabic
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
                    it.id ==
                        state.selectedNodeId &&
                        it.type ==
                        SldNodeType.PANEL
                }
                    ?: state.nodes.firstOrNull {
                        it.type ==
                            SldNodeType.PANEL
                    }
                    ?: throw IllegalStateException(
                        if (arabic) {
                            "لا توجد لوحة في الـ SLD."
                        } else {
                            "No panel exists in the SLD."
                        }
                    )

            val current =
                network()

            val validation =
                SldDesignValidator.validate(
                    current
                )

            if (!validation.valid) {
                validateDesign()
                return
            }

            val result =
                DesignProjectCoreBridge.calculateProjectSld(
                    project = project,
                    panelNodeId =
                        panel.id
                )

            state.reportTitle =
                if (arabic) {
                    "جدول اللوحة"
                } else {
                    "Panel Schedule"
                }

            state.reportText =
                buildPanelSchedule(
                    panel =
                        panel,
                    nodes =
                        state.nodes,
                    connections =
                        state.connections,
                    arabic =
                        arabic
                ) +
                    "\n\n" +
                    buildPanelEngineeringSummary(
                        result =
                            result,
                        arabic =
                            arabic
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
                DesignProjectCoreBridge.buildProjectSld(
                    project
                )

            val validation =
                SldDesignValidator.validate(
                    generated
                )

            if (!validation.valid) {

                state.nodes =
                    generated.nodes

                state.connections =
                    generated.connections

                validateDesignNetwork(
                    generated,
                    validation
                )

                return
            }

            val arranged =
                SldAutoLayoutEngine
                    .arrange(
                        generated
                    )
                    .network

            if (arranged.nodes.isEmpty()) {

                throw IllegalStateException(
                    if (arabic) {
                        "لا توجد بيانات كهربائية كافية لإنشاء SLD."
                    } else {
                        "There is not enough electrical project data to build the SLD."
                    }
                )
            }

            state.nodes =
                arranged.nodes

            state.connections =
                arranged.connections

            state.selectedNodeId =
                arranged.nodes
                    .firstOrNull()
                    ?.id

            state.selectedConnectionId = null

            state.connectionStartId = null

            DesignProjectCoreBridge.saveActiveProjectSld(
                network =
                    arranged,
                name =
                    "Main SLD",
                source =
                    "Electrical Design - Auto Layout"
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
                exception.message
                    ?: "SLD generation error."

            state.showReport = true
        }
    }

    private fun validateDesignNetwork(
        network: SldNetwork,
        validation: SldDesignValidator.Result
    ) {

        state.reportTitle =
            if (arabic) {
                "أخطاء تصميم SLD"
            } else {
                "SLD Design Errors"
            }

        state.reportText =
            buildString {

                appendLine(
                    if (arabic) {
                        "لم يتم اعتماد الحسابات لأن الشبكة غير صالحة."
                    } else {
                        "Engineering calculations were not approved because the topology is invalid."
                    }
                )

                appendLine()

                validation.errors.forEach {
                    appendLine(
                        "• [${it.code}] ${it.message}"
                    )
                }

                if (
                    validation.warnings.isNotEmpty()
                ) {

                    appendLine()

                    validation.warnings.forEach {
                        appendLine(
                            "• [${it.code}] ${it.message}"
                        )
                    }
                }
            }

        state.showReport = true
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
                    project =
                        project
                )

            val projectNetwork =
                DesignProjectCoreBridge.getProjectSld(
                    project
                )

            val validation =
                SldDesignValidator.validate(
                    projectNetwork
                )

            if (!validation.valid) {

                validateDesignNetwork(
                    projectNetwork,
                    validation
                )

                return
            }

            val report =
                SldEngineeringReportEngine.build(
                    network =
                        projectNetwork,
                    engineering =
                        result
                )

            state.reportTitle =
                if (arabic) {
                    "SLD - نتائج التصميم الهندسي"
                } else {
                    "SLD - Engineering Design Results"
                }

            state.reportText =
                report.asText()

        } catch (exception: Exception) {

            state.reportTitle =
                if (arabic) {
                    "خطأ"
                } else {
                    "Error"
                }

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

        val shortCircuit =
            result.shortCircuit

        val cableSizing =
            result.cableSizing

        val protection =
            result.protectionCoordination

        val panelSchedule =
            result.panelSchedule

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
                            shortCircuit
                                .maximumFaultCurrentKa
                        )
                } else {
                    "Maximum Short Circuit Current: " +
                        "%.3f kA".format(
                            shortCircuit
                                .maximumFaultCurrentKa
                        )
                }
            )

            appendLine(
                if (arabic) {
                    "أقصى تيار قمة: " +
                        "%.3f kA".format(
                            shortCircuit
                                .maximumPeakCurrentKa
                        )
                } else {
                    "Maximum Peak Current: " +
                        "%.3f kA".format(
                            shortCircuit
                                .maximumPeakCurrentKa
                        )
                }
            )

            appendLine(
                if (arabic) {
                    "أقصى قدرة قصر: " +
                        "%.3f MVA".format(
                            shortCircuit
                                .maximumFaultMva
                        )
                } else {
                    "Maximum Fault Level: " +
                        "%.3f MVA".format(
                            shortCircuit
                                .maximumFaultMva
                        )
                }
            )

            appendLine()

            appendLine(
                if (arabic) {
                    "المغذيات المقبولة: " +
                        cableSizing
                            .successfulFeeders
                } else {
                    "Successful Feeders: " +
                        cableSizing
                            .successfulFeeders
                }
            )

            appendLine(
                if (arabic) {
                    "المغذيات التي تحتاج مراجعة: " +
                        cableSizing
                            .failedFeeders
                } else {
                    "Feeders Requiring Review: " +
                        cableSizing
                            .failedFeeders
                }
            )

            appendLine()

            appendLine(
                if (arabic) {
                    "أزواج التنسيق المقبولة: " +
                        protection
                            .coordinatedPairs
                } else {
                    "Coordinated Pairs: " +
                        protection
                            .coordinatedPairs
                }
            )

            appendLine(
                if (arabic) {
                    "أزواج تحتاج مراجعة: " +
                        protection
                            .warningPairs
                } else {
                    "Pairs Requiring Review: " +
                        protection
                            .warningPairs
                }
            )

            appendLine(
                if (arabic) {
                    "أزواج غير مقبولة: " +
                        protection
                            .failedPairs
                } else {
                    "Failed Coordination Pairs: " +
                        protection
                            .failedPairs
                }
            )

            panelSchedule?.let {

                appendLine()

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

                    it.notes.forEach { note ->
                        appendLine(
                            "• $note"
                        )
                    }
                }
            }

            appendLine()

            shortCircuit.notes.forEach {
                appendLine("• $it")
            }

            cableSizing.notes.forEach {
                appendLine("• $it")
            }

            protection.notes.forEach {
                appendLine("• $it")
            }
        }
    }

    private fun buildShortCircuitReport(
        study: Any,
        arabic: Boolean
    ): String {

        return study.toString()
    }

    private fun buildPanelSchedule(
        panel: SldNode,
        nodes: List<SldNode>,
        connections: List<SldConnection>,
        arabic: Boolean
    ): String {

        val children =
            connections
                .filter {
                    it.fromNodeId == panel.id
                }

        return buildString {

            appendLine(
                if (arabic) {
                    "جدول اللوحة: ${panel.name}"
                } else {
                    "Panel Schedule: ${panel.name}"
                }
            )

            appendLine()

            children.forEachIndexed { index, connection ->

                val node =
                    nodes.firstOrNull {
                        it.id ==
                            connection.toNodeId
                    }

                appendLine(
                    "${index + 1}. " +
                        (node?.name ?: connection.toNodeId) +
                        " | " +
                        "%.2f kW".format(
                            node?.loadKw ?: 0.0
                        ) +
                        " | " +
                        "%.1f A".format(
                            connection.currentCapacityA
                        )
                )
            }
        }
    }

    private fun buildCompleteEngineeringReport(
        result: SldEngineeringPackage,
        arabic: Boolean
    ): String {

        return SldEngineeringReportEngine
            .build(
                network(),
                result
            )
            .asText()
    }
}

بعد النسخ اعمل Build فقط. إذا نجح، ننتقل مباشرة للمرحلة التالية: تطوير رموز الـSLD والمعدات الكهربائية الحقيقية وربط كل عنصر بالحسابات Upstream/Downstream.
