package com.electrical.calculationspro.ui.screens.sld

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldAutoLayoutEngine
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldDesignValidator
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

    private fun currentNetwork(): SldNetwork =
        SldNetwork(
            nodes = state.nodes,
            connections = state.connections
        )

    fun loadProjectNetwork() {

        val project =
            DesignProjects.getActive()
                ?: return

        val stored =
            DesignProjectCoreBridge.getProjectSld(
                project
            )

        if (stored.nodes.isEmpty()) {

            val generated =
                DesignProjectCoreBridge.buildProjectSld(
                    project
                )

            if (generated.nodes.isNotEmpty()) {

                val arranged =
                    SldAutoLayoutEngine
                        .arrange(generated)
                        .network

                state.nodes =
                    arranged.nodes

                state.connections =
                    arranged.connections
            }

            return
        }

        state.nodes =
            stored.nodes

        state.connections =
            stored.connections

        state.selectedNodeId =
            null

        state.selectedConnectionId =
            null

        state.connectionStartId =
            null
    }

    fun saveProjectNetwork() {

        val network =
            currentNetwork()

        if (network.nodes.isEmpty()) {
            return
        }

        DesignProjectCoreBridge.saveActiveProjectSld(
            network = network,
            name = "Main SLD",
            source = "Electrical Design"
        )
    }

    fun autoLayout() {

        val network =
            currentNetwork()

        if (network.nodes.isEmpty()) {
            return
        }

        val validation =
            SldDesignValidator.validate(
                network
            )

        if (!validation.valid) {

            showValidation(
                validation
            )

            return
        }

        val arranged =
            SldAutoLayoutEngine
                .arrange(network)
                .network

        state.nodes =
            arranged.nodes

        state.connections =
            arranged.connections

        DesignProjectCoreBridge.saveActiveProjectSld(
            network = arranged,
            name = "Main SLD",
            source = "Electrical Design - Auto Layout"
        )
    }

    fun resetNodeEditor(
        type: SldNodeType
    ) {

        state.editingNodeId =
            null

        state.nodeType =
            type

        state.name =
            defaultName(type)

        state.voltage =
            "400"

        state.loadKw =
            "100"

        state.pf =
            "0.90"

        state.demand =
            "0.80"

        state.kva =
            when (type) {
                SldNodeType.TRANSFORMER,
                SldNodeType.GENERATOR,
                SldNodeType.PANEL ->
                    "500"

                else ->
                    "0"
            }

        state.transformerZ =
            "6"

        state.generatorXd =
            "15"

        state.sourceMva =
            "500"

        state.showNodeDialog =
            true
    }

    fun editNode(
        node: SldNode
    ) {

        state.editingNodeId =
            node.id

        state.nodeType =
            node.type

        state.name =
            node.name

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

        state.showNodeDialog =
            true
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
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val pf =
            state.pf
                .toDoubleOrNull()
                ?.coerceIn(0.01, 1.0)
                ?: 0.90

        val demand =
            state.demand
                .toDoubleOrNull()
                ?.coerceIn(0.0, 1.0)
                ?: 1.0

        val kva =
            state.kva
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val transformerZ =
            state.transformerZ
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val generatorXd =
            state.generatorXd
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val sourceMva =
            state.sourceMva
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val existingId =
            state.editingNodeId

        if (existingId == null) {

            val index =
                state.nodes.size

            val node =
                SldNode(
                    id =
                        "node-${System.currentTimeMillis()}",
                    name =
                        state.name.ifBlank {
                            defaultName(
                                state.nodeType
                            )
                        },
                    type =
                        state.nodeType,
                    x =
                        100f +
                            (index % 5) * 300f,
                    y =
                        150f +
                            (index / 5) * 210f,
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

                    if (node.id != existingId) {
                        node
                    } else {

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
                    }
                }
        }

        state.showNodeDialog =
            false

        state.editingNodeId =
            null

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

    fun startOrCompleteConnection() {

        val selected =
            state.selectedNodeId
                ?: return

        val start =
            state.connectionStartId

        if (start == null) {

            state.connectionStartId =
                selected

            state.selectedConnectionId =
                null

            return
        }

        if (start == selected) {

            state.connectionStartId =
                null

            return
        }

        state.editingConnectionId =
            null

        state.showConnectionDialog =
            true
    }

    fun saveConnection() {

        val start =
            state.connectionStartId
                ?: return

        val end =
            state.selectedNodeId
                ?: return

        if (start == end) {
            return
        }

        val length =
            state.length
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val resistance =
            state.resistance
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val reactance =
            state.reactance
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val cableSize =
            state.cableSize
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val parallel =
            state.parallelRuns
                .toIntOrNull()
                ?.coerceAtLeast(1)
                ?: 1

        val capacity =
            state.capacity
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val existing =
            state.editingConnectionId

        if (existing != null) {

            state.connections =
                state.connections.map { connection ->

                    if (
                        connection.id ==
                        existing
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
                                parallel,
                            currentCapacityA =
                                capacity
                        )

                    } else {
                        connection
                    }
                }

        } else {

            val duplicate =
                state.connections.any {

                    (
                        it.fromNodeId == start &&
                            it.toNodeId == end
                        ) ||
                        (
                            it.fromNodeId == end &&
                                it.toNodeId == start
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
                                parallel,
                            currentCapacityA =
                                capacity
                        )
            }
        }

        state.editingConnectionId =
            null

        state.connectionStartId =
            null

        state.showConnectionDialog =
            false

        saveProjectNetwork()
    }

    fun deleteSelected() {

        val connectionId =
            state.selectedConnectionId

        if (connectionId != null) {

            state.connections =
                state.connections.filter {
                    it.id != connectionId
                }

            state.selectedConnectionId =
                null

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

        state.selectedNodeId =
            null

        state.connectionStartId =
            null

        saveProjectNetwork()
    }

    fun validateDesign() {

        showValidation(
            SldDesignValidator.validate(
                currentNetwork()
            )
        )
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

            val network =
                currentNetwork()

            val validation =
                SldDesignValidator.validate(
                    network
                )

            if (!validation.valid) {

                showValidation(
                    validation
                )

                return
            }

            val result =
                DesignProjectCoreBridge.calculateProjectSld(
                    project = project,
                    panelNodeId = null
                )

            state.reportTitle =
                if (arabic) {
                    "تيارات القصر"
                } else {
                    "Short Circuit Study"
                }

            state.reportText =
                buildString {

                    appendLine(
                        if (arabic) {
                            "دراسة تيارات القصر"
                        } else {
                            "Short Circuit Study"
                        }
                    )

                    appendLine()

                    appendLine(
                        "Maximum fault current = " +
                            "%.3f kA".format(
                                result
                                    .shortCircuit
                                    .maximumFaultCurrentKa
                            )
                    )

                    appendLine(
                        "Maximum peak current = " +
                            "%.3f kA".format(
                                result
                                    .shortCircuit
                                    .maximumPeakCurrentKa
                            )
                    )

                    appendLine(
                        "Maximum fault level = " +
                            "%.3f MVA".format(
                                result
                                    .shortCircuit
                                    .maximumFaultMva
                            )
                    )

                    result.shortCircuit.notes.forEach {
                        appendLine()
                        appendLine("• $it")
                    }
                }

            state.showReport =
                true

        } catch (e: Exception) {

            showError(
                e.message
                    ?: "Short circuit calculation failed."
            )
        }
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

            val result =
                DesignProjectCoreBridge.calculateProjectSld(
                    project = project,
                    panelNodeId = panel.id
                )

            val schedule =
                result.panelSchedule
                    ?: throw IllegalStateException(
                        if (arabic) {
                            "تعذر إنشاء جدول اللوحة."
                        } else {
                            "Panel schedule could not be generated."
                        }
                    )

            state.reportTitle =
                if (arabic) {
                    "جدول اللوحة"
                } else {
                    "Panel Schedule"
                }

            state.reportText =
                buildString {

                    appendLine(
                        schedule.panelName
                    )

                    appendLine(
                        "Voltage = %.0f V"
                            .format(
                                schedule.panelVoltageV
                            )
                    )

                    appendLine(
                        "Connected Load = %.2f kW"
                            .format(
                                schedule.totalConnectedLoadKw
                            )
                    )

                    appendLine(
                        "Demand Load = %.2f kW"
                            .format(
                                schedule.totalDemandLoadKw
                            )
                    )

                    appendLine(
                        "Demand Current = %.2f A"
                            .format(
                                schedule.totalDemandCurrentA
                            )
                    )

                    appendLine()

                    schedule.rows.forEachIndexed {
                        index,
                        row ->

                        appendLine(
                            "${index + 1}. ${row.feederName}"
                        )

                        appendLine(
                            "   Load = %.2f kW"
                                .format(
                                    row.loadKw
                                )
                        )

                        appendLine(
                            "   Current = %.2f A"
                                .format(
                                    row.designCurrentA
                                )
                        )

                        appendLine(
                            "   Breaker = %.0f A"
                                .format(
                                    row.recommendedBreakerA
                                )
                        )

                        appendLine(
                            "   Cable = %.1f mm² × %d"
                                .format(
                                    row.cableSizeMm2,
                                    row.parallelRuns
                                )
                        )

                        appendLine(
                            "   Capacity = %.1f A"
                                .format(
                                    row.currentCapacityA
                                )
                        )

                        appendLine(
                            "   Voltage Drop = %.2f %%"
                                .format(
                                    row.voltageDropPercent
                                )
                        )

                        appendLine(
                            "   Short Circuit = %.3f kA"
                                .format(
                                    row.shortCircuitCurrentKa
                                )
                        )

                        appendLine(
                            "   Status = ${row.status}"
                        )

                        row.notes.forEach {
                            appendLine(
                                "   Note: $it"
                            )
                        }

                        appendLine()
                    }

                    schedule.notes.forEach {
                        appendLine(
                            "Note: $it"
                        )
                    }
                }

            state.showReport =
                true

        } catch (e: Exception) {

            showError(
                e.message
                    ?: "Panel schedule calculation failed."
            )
        }
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

            val stored =
                DesignProjectCoreBridge.getProjectSld(
                    project
                )

            val network =
                if (stored.nodes.isNotEmpty()) {

                    /*
                     * EXISTING DRAWING:
                     * preserve manual positions.
                     */
                    stored

                } else {

                    val generated =
                        DesignProjectCoreBridge
                            .buildProjectSld(
                                project
                            )

                    if (generated.nodes.isEmpty()) {

                        throw IllegalStateException(
                            if (arabic) {
                                "لا توجد بيانات كهربائية كافية لإنشاء المخطط."
                            } else {
                                "There is not enough electrical data to build the SLD."
                            }
                        )
                    }

                    /*
                     * Initial layout only.
                     */
                    SldAutoLayoutEngine
                        .arrange(
                            generated
                        )
                        .network
                }

            val validation =
                SldDesignValidator.validate(
                    network
                )

            if (!validation.valid) {

                state.nodes =
                    network.nodes

                state.connections =
                    network.connections

                showValidation(
                    validation
                )

                return
            }

            state.nodes =
                network.nodes

            state.connections =
                network.connections

            state.selectedNodeId =
                null

            state.selectedConnectionId =
                null

            state.connectionStartId =
                null

            DesignProjectCoreBridge.saveActiveProjectSld(
                network = network,
                name = "Main SLD",
                source = "Electrical Design"
            )

            val panelId =
                network.nodes
                    .firstOrNull {
                        it.type ==
                            SldNodeType.PANEL
                    }
                    ?.id

            val engineering =
                DesignProjectCoreBridge.calculateProjectSld(
                    project = project,
                    panelNodeId = panelId
                )

            val report =
                SldEngineeringReportEngine.build(
                    network = network,
                    engineering = engineering
                )

            state.reportTitle =
                if (arabic) {
                    "التقرير الهندسي الكامل"
                } else {
                    "Complete Engineering Report"
                }

            state.reportText =
                report.asText()

            state.showReport =
                true

        } catch (e: Exception) {

            showError(
                e.message
                    ?: "Complete SLD calculation failed."
            )
        }
    }

    private fun showValidation(
        result: SldDesignValidator.Result
    ) {

        state.reportTitle =
            if (arabic) {
                "مراجعة SLD"
            } else {
                "SLD Validation"
            }

        state.reportText =
            buildString {

                appendLine(
                    if (result.valid) {
                        if (arabic) {
                            "الشبكة صالحة مبدئيًا."
                        } else {
                            "The SLD topology is valid."
                        }
                    } else {
                        if (arabic) {
                            "يوجد أخطاء في الشبكة."
                        } else {
                            "The SLD topology contains errors."
                        }
                    }
                )

                if (result.errors.isNotEmpty()) {

                    appendLine()
                    appendLine(
                        if (arabic) {
                            "الأخطاء:"
                        } else {
                            "Errors:"
                        }
                    )

                    result.errors.forEach {
                        appendLine(
                            "• [${it.code}] ${it.message}"
                        )
                    }
                }

                if (result.warnings.isNotEmpty()) {

                    appendLine()
                    appendLine(
                        if (arabic) {
                            "التحذيرات:"
                        } else {
                            "Warnings:"
                        }
                    )

                    result.warnings.forEach {
                        appendLine(
                            "• [${it.code}] ${it.message}"
                        )
                    }
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

        state.showReport =
            true
    }

    private fun showError(
        message: String
    ) {

        state.reportTitle =
            if (arabic) {
                "خطأ"
            } else {
                "Error"
            }

        state.reportText =
            message

        state.showReport =
            true
    }

    private fun defaultName(
        type: SldNodeType
    ): String {

        return when (type) {

            SldNodeType.SOURCE ->
                if (arabic) "مصدر التغذية" else "MAIN SOURCE"

            SldNodeType.TRANSFORMER ->
                if (arabic) "المحول" else "TRANSFORMER"

            SldNodeType.GENERATOR ->
                if (arabic) "المولد" else "GENERATOR"

            SldNodeType.BUS ->
                if (arabic) "القضبان الرئيسية" else "MAIN BUS"

            SldNodeType.BREAKER ->
                if (arabic) "القاطع" else "BREAKER"

            SldNodeType.PANEL ->
                if (arabic) "اللوحة" else "PANEL"

            SldNodeType.LOAD ->
                if (arabic) "الحمل" else "LOAD"
        }
    }
}
