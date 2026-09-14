package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldCableSizingEngine
import com.electrical.calculationspro.data.SldCableSizingStudy
import com.electrical.calculationspro.data.SldCalculationResult
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringEngine
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldPanelSchedule
import com.electrical.calculationspro.data.SldPanelScheduleEngine
import com.electrical.calculationspro.data.SldProtectionCoordinationEngine
import com.electrical.calculationspro.data.SldProtectionCoordinationResult
import com.electrical.calculationspro.data.SldShortCircuitEngine
import com.electrical.calculationspro.data.SldShortCircuitStudy
import kotlin.math.hypot

private val Background = Color(0xFF0B1116)
private val CardColor = Color(0xFF151D24)
private val PrimaryText = Color(0xFFF2F5F7)
private val SecondaryText = Color(0xFFAAB7C0)
private val Accent = Color(0xFF00BCD4)

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {

    val arabic = language == AppLanguage.ARABIC

    val languageCode =
        if (arabic) "ar" else "en"

    var nodes by remember {
        mutableStateOf(
            listOf(
                SldNode(
                    id = "source-1",
                    name = "MAIN SOURCE",
                    type = SldNodeType.SOURCE,
                    x = 80f,
                    y = 160f,
                    voltage = 400.0,
                    sourceShortCircuitMva = 500.0
                )
            )
        )
    }

    var connections by remember {
        mutableStateOf(emptyList<SldConnection>())
    }

    var selectedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    var selectedConnectionId by remember {
        mutableStateOf<String?>(null)
    }

    var connectionStartId by remember {
        mutableStateOf<String?>(null)
    }

    var showNodeDialog by remember {
        mutableStateOf(false)
    }

    var showConnectionDialog by remember {
        mutableStateOf(false)
    }

    var showUpstreamResults by remember {
        mutableStateOf(false)
    }

    var showShortCircuitResults by remember {
        mutableStateOf(false)
    }

    var showCableSizingResults by remember {
        mutableStateOf(false)
    }

    var showProtectionResults by remember {
        mutableStateOf(false)
    }

    var showPanelSchedule by remember {
        mutableStateOf(false)
    }

    var editingNodeId by remember {
        mutableStateOf<String?>(null)
    }

    var editingConnectionId by remember {
        mutableStateOf<String?>(null)
    }

    var pendingNodeType by remember {
        mutableStateOf(SldNodeType.LOAD)
    }

    var nodeName by remember { mutableStateOf("") }
    var nodeVoltage by remember { mutableStateOf("400") }
    var nodeKw by remember { mutableStateOf("0") }
    var nodePf by remember { mutableStateOf("0.90") }
    var nodeDemand by remember { mutableStateOf("1.0") }
    var nodeKva by remember { mutableStateOf("0") }
    var nodeTransformerZ by remember { mutableStateOf("6") }
    var nodeGeneratorXd by remember { mutableStateOf("15") }
    var nodeSourceMva by remember { mutableStateOf("500") }

    var connectionLength by remember { mutableStateOf("10") }
    var connectionResistance by remember { mutableStateOf("0.125") }
    var connectionReactance by remember { mutableStateOf("0.080") }
    var connectionCableSize by remember { mutableStateOf("0") }
    var connectionParallelRuns by remember { mutableStateOf("1") }
    var connectionCurrentCapacity by remember { mutableStateOf("0") }

    var calculationResult by remember {
        mutableStateOf<SldCalculationResult?>(null)
    }

    var shortCircuitStudy by remember {
        mutableStateOf<SldShortCircuitStudy?>(null)
    }

    var cableSizingStudy by remember {
        mutableStateOf<SldCableSizingStudy?>(null)
    }

    var protectionResult by remember {
        mutableStateOf<SldProtectionCoordinationResult?>(null)
    }

    var panelSchedule by remember {
        mutableStateOf<SldPanelSchedule?>(null)
    }

    fun network(): SldNetwork =
        SldNetwork(
            nodes = nodes,
            connections = connections
        )

    fun invalidateStudies() {
        calculationResult = null
        shortCircuitStudy = null
        cableSizingStudy = null
        protectionResult = null
        panelSchedule = null
    }

    fun defaultName(
        type: SldNodeType
    ): String {

        val prefix =
            when (type) {
                SldNodeType.SOURCE -> "SOURCE"
                SldNodeType.BUS -> "BUS"
                SldNodeType.TRANSFORMER -> "TR"
                SldNodeType.GENERATOR -> "GEN"
                SldNodeType.BREAKER -> "CB"
                SldNodeType.PANEL -> "PANEL"
                SldNodeType.LOAD -> "LOAD"
            }

        val number =
            nodes.count {
                it.type == type
            } + 1

        return "$prefix-$number"
    }

    fun openAdd(
        type: SldNodeType
    ) {
        editingNodeId = null
        pendingNodeType = type
        nodeName = defaultName(type)
        nodeVoltage = "400"
        nodeKw = "0"
        nodePf = "0.90"
        nodeDemand = "1.0"
        nodeKva = "0"
        nodeTransformerZ = "6"
        nodeGeneratorXd = "15"
        nodeSourceMva = "500"
        showNodeDialog = true
    }

    fun openEdit(
        node: SldNode
    ) {
        editingNodeId = node.id
        pendingNodeType = node.type
        nodeName = node.name
        nodeVoltage = node.voltage.toString()
        nodeKw = node.loadKw.toString()
        nodePf = node.powerFactor.toString()
        nodeDemand = node.demandFactor.toString()
        nodeKva = node.ratedKva.toString()
        nodeTransformerZ = node.transformerPercentZ.toString()
        nodeGeneratorXd = node.generatorXdSubtransient.toString()
        nodeSourceMva = node.sourceShortCircuitMva.toString()
        showNodeDialog = true
    }

    fun openConnection(
        connection: SldConnection
    ) {
        editingConnectionId = connection.id
        connectionLength = connection.lengthMeters.toString()
        connectionResistance =
            connection.resistanceOhmPerKm.toString()
        connectionReactance =
            connection.reactanceOhmPerKm.toString()
        connectionCableSize =
            connection.cableSizeMm2.toString()
        connectionParallelRuns =
            connection.parallelRuns.toString()
        connectionCurrentCapacity =
            connection.currentCapacityA.toString()
        showConnectionDialog = true
    }

    fun saveNode() {

        val voltage =
            nodeVoltage.toDoubleOrNull() ?: 400.0

        val kw =
            nodeKw.toDoubleOrNull() ?: 0.0

        val pf =
            (nodePf.toDoubleOrNull() ?: 0.90)
                .coerceIn(0.01, 1.0)

        val demand =
            (nodeDemand.toDoubleOrNull() ?: 1.0)
                .coerceIn(0.0, 1.0)

        val kva =
            nodeKva.toDoubleOrNull() ?: 0.0

        val transformerZ =
            nodeTransformerZ.toDoubleOrNull() ?: 0.0

        val generatorXd =
            nodeGeneratorXd.toDoubleOrNull() ?: 0.0

        val sourceMva =
            nodeSourceMva.toDoubleOrNull() ?: 0.0

        if (editingNodeId == null) {

            val newNode =
                SldNode(
                    id = "node-${System.currentTimeMillis()}",
                    name =
                        nodeName.ifBlank {
                            defaultName(pendingNodeType)
                        },
                    type = pendingNodeType,
                    x =
                        (nodes.maxOfOrNull {
                            it.x
                        } ?: 80f) + 180f,
                    y =
                        nodes.maxOfOrNull {
                            it.y
                        } ?: 160f,
                    voltage = voltage,
                    loadKw = kw,
                    powerFactor = pf,
                    demandFactor = demand,
                    ratedKva = kva,
                    transformerPercentZ = transformerZ,
                    generatorXdSubtransient = generatorXd,
                    sourceShortCircuitMva = sourceMva
                )

            nodes =
                nodes + newNode

            selectedNodeId =
                newNode.id

        } else {

            val id =
                editingNodeId!!

            nodes =
                nodes.map { node ->

                    if (node.id == id) {
                        node.copy(
                            name =
                                nodeName.ifBlank {
                                    node.name
                                },
                            type = pendingNodeType,
                            voltage = voltage,
                            loadKw = kw,
                            powerFactor = pf,
                            demandFactor = demand,
                            ratedKva = kva,
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

        invalidateStudies()
        showNodeDialog = false
    }

    fun saveConnection() {

        val length =
            (connectionLength.toDoubleOrNull() ?: 0.0)
                .coerceAtLeast(0.0)

        val resistance =
            connectionResistance.toDoubleOrNull()
                ?: 0.0

        val reactance =
            connectionReactance.toDoubleOrNull()
                ?: 0.0

        val cableSize =
            connectionCableSize.toDoubleOrNull()
                ?: 0.0

        val runs =
            (connectionParallelRuns.toIntOrNull() ?: 1)
                .coerceAtLeast(1)

        val capacity =
            connectionCurrentCapacity.toDoubleOrNull()
                ?: 0.0

        if (editingConnectionId == null) {

            val from =
                connectionStartId
                    ?: return

            val to =
                selectedNodeId
                    ?: return

            if (from == to) {
                connectionStartId = null
                return
            }

            val exists =
                connections.any {
                    it.fromNodeId == from &&
                        it.toNodeId == to
                }

            if (!exists) {

                val connection =
                    SldConnection(
                        id =
                            "connection-${System.currentTimeMillis()}",
                        fromNodeId = from,
                        toNodeId = to,
                        lengthMeters = length,
                        resistanceOhmPerKm =
                            resistance,
                        reactanceOhmPerKm =
                            reactance,
                        cableSizeMm2 =
                            cableSize,
                        parallelRuns = runs,
                        currentCapacityA =
                            capacity
                    )

                connections =
                    connections + connection

                selectedConnectionId =
                    connection.id
            }

        } else {

            val id =
                editingConnectionId!!

            connections =
                connections.map { connection ->

                    if (connection.id == id) {
                        connection.copy(
                            lengthMeters = length,
                            resistanceOhmPerKm =
                                resistance,
                            reactanceOhmPerKm =
                                reactance,
                            cableSizeMm2 =
                                cableSize,
                            parallelRuns = runs,
                            currentCapacityA =
                                capacity
                        )
                    } else {
                        connection
                    }
                }
        }

        connectionStartId = null
        invalidateStudies()
        showConnectionDialog = false
    }

    fun deleteSelected() {

        selectedNodeId?.let { id ->

            if (
                nodes.size > 1 &&
                id != "source-1"
            ) {

                nodes =
                    nodes.filterNot {
                        it.id == id
                    }

                connections =
                    connections.filter {
                        it.fromNodeId != id &&
                            it.toNodeId != id
                    }

                selectedNodeId = null
                selectedConnectionId = null
                connectionStartId = null

                invalidateStudies()

                return@let
            }
        }

        selectedConnectionId?.let { id ->

            connections =
                connections.filterNot {
                    it.id == id
                }

            selectedConnectionId = null

            invalidateStudies()
        }
    }

    fun connect() {

        val selected =
            selectedNodeId
                ?: return

        if (connectionStartId == null) {
            connectionStartId = selected
            return
        }

        if (connectionStartId == selected) {
            connectionStartId = null
            return
        }

        val from =
            connectionStartId!!

        val exists =
            connections.any {
                it.fromNodeId == from &&
                    it.toNodeId == selected
            }

        if (!exists) {

            val connection =
                SldConnection(
                    id =
                        "connection-${System.currentTimeMillis()}",
                    fromNodeId = from,
                    toNodeId = selected
                )

            connections =
                connections + connection

            selectedConnectionId =
                connection.id

            invalidateStudies()
        }

        connectionStartId = null
    }

    fun runUpstream() {

        calculationResult =
            try {
                SldEngineeringEngine
                    .calculateUpstream(
                        network()
                    )
            } catch (_: Exception) {
                null
            }

        showUpstreamResults =
            calculationResult != null
    }

    fun runShortCircuit() {

        shortCircuitStudy =
            try {
                SldShortCircuitEngine
                    .calculate(
                        network()
                    )
            } catch (_: Exception) {
                null
            }

        showShortCircuitResults =
            shortCircuitStudy != null
    }

    fun runCableSizing() {

        if (shortCircuitStudy == null) {
            shortCircuitStudy =
                try {
                    SldShortCircuitEngine
                        .calculate(network())
                } catch (_: Exception) {
                    null
                }
        }

        cableSizingStudy =
            try {
                SldCableSizingEngine.calculate(
                    network = network(),
                    shortCircuitStudy =
                        shortCircuitStudy
                )
            } catch (_: Exception) {
                null
            }

        showCableSizingResults =
            cableSizingStudy != null
    }

    fun runProtection() {

        if (shortCircuitStudy == null) {
            shortCircuitStudy =
                try {
                    SldShortCircuitEngine
                        .calculate(network())
                } catch (_: Exception) {
                    null
                }
        }

        if (cableSizingStudy == null) {
            cableSizingStudy =
                try {
                    SldCableSizingEngine.calculate(
                        network = network(),
                        shortCircuitStudy =
                            shortCircuitStudy
                    )
                } catch (_: Exception) {
                    null
                }
        }

        protectionResult =
            try {
                SldProtectionCoordinationEngine
                    .calculate(
                        network = network(),
                        shortCircuitStudy =
                            shortCircuitStudy,
                        cableSizingStudy =
                            cableSizingStudy
                    )
            } catch (_: Exception) {
                null
            }

        showProtectionResults =
            protectionResult != null
    }

    fun runPanelSchedule() {

        val selected =
            selectedNodeId
                ?: return

        if (cableSizingStudy == null) {
            cableSizingStudy =
                try {
                    SldCableSizingEngine.calculate(
                        network = network(),
                        shortCircuitStudy =
                            shortCircuitStudy
                    )
                } catch (_: Exception) {
                    null
                }
        }

        panelSchedule =
            try {
                SldPanelScheduleEngine.calculate(
                    network = network(),
                    panelNodeId = selected,
                    cableSizingStudy =
                        cableSizingStudy
                )
            } catch (_: Exception) {
                null
            }

        showPanelSchedule =
            panelSchedule != null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(10.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (onBack != null) {
                OutlinedButton(
                    onClick = onBack
                ) {
                    Text(
                        if (arabic) "رجوع" else "Back"
                    )
                }
            }

            Text(
                text =
                    if (arabic) {
                        "المخطط الأحادي SLD"
                    } else {
                        "Single Line Diagram"
                    },
                modifier =
                    Modifier.padding(
                        start = 12.dp
                    ),
                color = PrimaryText,
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        ToolBar(
            language = languageCode,
            onAdd = ::openAdd,
            onConnect = ::connect,
            onEdit = {
                selectedNodeId?.let { id ->
                    nodes.firstOrNull {
                        it.id == id
                    }?.let(::openEdit)

                } ?: selectedConnectionId?.let { id ->
                    connections.firstOrNull {
                        it.id == id
                    }?.let(::openConnection)
                }
            },
            onDelete = ::deleteSelected,
            onUpstream = ::runUpstream,
            onShortCircuit = ::runShortCircuit,
            onCableSizing = ::runCableSizing,
            onProtection = ::runProtection,
            onPanelSchedule = ::runPanelSchedule,
            panelScheduleEnabled =
                selectedNodeId != null
        )

        SldCanvas(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            nodes = nodes,
            connections = connections,
            selectedNodeId =
                selectedNodeId,
            selectedConnectionId =
                selectedConnectionId,
            connectionStartId =
                connectionStartId,
            onNodeSelected = {
                selectedNodeId = it
                selectedConnectionId = null
            },
            onConnectionSelected = {
                selectedConnectionId = it
                selectedNodeId = null
            },
            onEmptySelected = {
                selectedNodeId = null
                selectedConnectionId = null
            },
            onNodeMoved = { id, x, y ->

                nodes =
                    nodes.map { node ->
                        if (node.id == id) {
                            node.copy(
                                x = x,
                                y = y
                            )
                        } else {
                            node
                        }
                    }
            }
        )
    }

    if (showNodeDialog) {

        NodeEditorDialog(
            arabic = arabic,
            editing = editingNodeId != null,
            type = pendingNodeType,
            name = nodeName,
            voltage = nodeVoltage,
            kw = nodeKw,
            pf = nodePf,
            demand = nodeDemand,
            kva = nodeKva,
            transformerZ = nodeTransformerZ,
            generatorXd = nodeGeneratorXd,
            sourceMva = nodeSourceMva,
            onName = { nodeName = it },
            onVoltage = { nodeVoltage = it },
            onKw = { nodeKw = it },
            onPf = { nodePf = it },
            onDemand = { nodeDemand = it },
            onKva = { nodeKva = it },
            onTransformerZ = {
                nodeTransformerZ = it
            },
            onGeneratorXd = {
                nodeGeneratorXd = it
            },
            onSourceMva = {
                nodeSourceMva = it
            },
            onSave = ::saveNode,
            onDismiss = {
                showNodeDialog = false
            }
        )
    }

    if (showConnectionDialog) {

        ConnectionDialog(
            arabic = arabic,
            length = connectionLength,
            resistance = connectionResistance,
            reactance = connectionReactance,
            cableSize = connectionCableSize,
            runs = connectionParallelRuns,
            capacity = connectionCurrentCapacity,
            onLength = {
                connectionLength = it
            },
            onResistance = {
                connectionResistance = it
            },
            onReactance = {
                connectionReactance = it
            },
            onCableSize = {
                connectionCableSize = it
            },
            onRuns = {
                connectionParallelRuns = it
            },
            onCapacity = {
                connectionCurrentCapacity = it
            },
            onSave = ::saveConnection,
            onDismiss = {
                showConnectionDialog = false
            }
        )
    }

    if (showUpstreamResults) {

        val result =
            calculationResult

        AlertDialog(
            onDismissRequest = {
                showUpstreamResults = false
            },
            title = {
                Text(
                    if (arabic) {
                        "نتائج Upstream"
                    } else {
                        "Upstream Results"
                    }
                )
            },
            text = {

                Column {

                    Text(
                        "Total Connected Load: " +
                            "${result?.totalConnectedLoadKw ?: 0.0} kW"
                    )

                    Text(
                        "Demand Load: " +
                            "${result?.totalDemandLoadKw ?: 0.0} kW"
                    )

                    Text(
                        "Required kVA: " +
                            "${result?.totalRequiredKva ?: 0.0}"
                    )

                    Text(
                        "Main Current: " +
                            "${result?.mainCurrentA ?: 0.0} A"
                    )

                    Text(
                        "Main Breaker: " +
                            "${result?.mainBreakerA ?: 0.0} A"
                    )

                    Text(
                        "Transformer: " +
                            "${result?.requiredTransformerKva ?: 0.0} kVA"
                    )

                    Text(
                        "Voltage Drop: " +
                            "${result?.totalVoltageDropPercent ?: 0.0} %"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpstreamResults = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showShortCircuitResults) {

        SldShortCircuitResultsDialog(
            language = language,
            study = shortCircuitStudy,
            onDismiss = {
                showShortCircuitResults = false
            }
        )
    }

    if (showCableSizingResults) {

        cableSizingStudy?.let { study ->

            SldCableSizingResultsDialog(
                language = language,
                study = study,
                onDismiss = {
                    showCableSizingResults = false
                }
            )
        }
    }

    if (showProtectionResults) {

        protectionResult?.let { result ->

            SldProtectionCoordinationResultsDialog(
                language = language,
                result = result,
                onDismiss = {
                    showProtectionResults = false
                }
            )
        }
    }

    if (showPanelSchedule) {

        panelSchedule?.let { schedule ->

            SldPanelScheduleResultsDialog(
                language = languageCode,
                schedule = schedule,
                onDismiss = {
                    showPanelSchedule = false
                }
            )
        }
    }
}

@Composable
private fun ToolBar(
    language: String,
    onAdd: (SldNodeType) -> Unit,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpstream: () -> Unit,
    onShortCircuit: () -> Unit,
    onCableSizing: () -> Unit,
    onProtection: () -> Unit,
    onPanelSchedule: () -> Unit,
    panelScheduleEnabled: Boolean
) {

    val arabic =
        language.equals(
            "ar",
            true
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(vertical = 6.dp)
    ) {

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            SmallButton(
                if (arabic) "حمل" else "Load"
            ) {
                onAdd(
                    SldNodeType.LOAD
                )
            }

            SmallButton(
                if (arabic) "لوحة" else "Panel"
            ) {
                onAdd(
                    SldNodeType.PANEL
                )
            }

            SmallButton(
                "Bus"
            ) {
                onAdd(
                    SldNodeType.BUS
                )
            }

            SmallButton(
                if (arabic) "محول" else "Transformer"
            ) {
                onAdd(
                    SldNodeType.TRANSFORMER
                )
            }

            SmallButton(
                if (arabic) "مولد" else "Generator"
            ) {
                onAdd(
                    SldNodeType.GENERATOR
                )
            }

            SmallButton(
                if (arabic) "قاطع" else "Breaker"
            ) {
                onAdd(
                    SldNodeType.BREAKER
                )
            }

            SmallButton(
                if (arabic) "توصيل" else "Connect"
            ) {
                onConnect()
            }

            SmallButton(
                if (arabic) "تعديل" else "Edit"
            ) {
                onEdit()
            }

            SmallButton(
                if (arabic) "حذف" else "Delete"
            ) {
                onDelete()
            }

            SmallButton(
                "Upstream"
            ) {
                onUpstream()
            }

            SmallButton(
                if (arabic) "قصر" else "Short Circuit"
            ) {
                onShortCircuit()
            }

            SmallButton(
                if (arabic) "كابلات" else "Cable Sizing"
            ) {
                onCableSizing()
            }

            SmallButton(
                if (arabic) "حماية" else "Protection"
            ) {
                onProtection()
            }

            SmallButton(
                if (arabic) "جدول لوحة" else "Panel Schedule"
            ) {
                if (panelScheduleEnabled) {
                    onPanelSchedule()
                }
            }
        }
    }
}

@Composable
private fun SmallButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier.height(38.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun NodeEditorDialog(
    arabic: Boolean,
    editing: Boolean,
    type: SldNodeType,
    name: String,
    voltage: String,
    kw: String,
    pf: String,
    demand: String,
    kva: String,
    transformerZ: String,
    generatorXd: String,
    sourceMva: String,
    onName: (String) -> Unit,
    onVoltage: (String) -> Unit,
    onKw: (String) -> Unit,
    onPf: (String) -> Unit,
    onDemand: (String) -> Unit,
    onKva: (String) -> Unit,
    onTransformerZ: (String) -> Unit,
    onGeneratorXd: (String) -> Unit,
    onSourceMva: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editing) {
                    if (arabic) "تعديل عنصر" else "Edit Element"
                } else {
                    if (arabic) "إضافة عنصر" else "Add Element"
                }
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = type.name,
                    color = Accent
                )

                TextField(
                    value = name,
                    onValueChange = onName,
                    label = {
                        Text("Name")
                    },
                    singleLine = true
                )

                TextField(
                    value = voltage,
                    onValueChange = onVoltage,
                    label = {
                        Text("Voltage V")
                    },
                    singleLine = true
                )

                TextField(
                    value = kw,
                    onValueChange = onKw,
                    label = {
                        Text("Load kW")
                    },
                    singleLine = true
                )

                TextField(
                    value = pf,
                    onValueChange = onPf,
                    label = {
                        Text("Power Factor")
                    },
                    singleLine = true
                )

                TextField(
                    value = demand,
                    onValueChange = onDemand,
                    label = {
                        Text("Demand Factor")
                    },
                    singleLine = true
                )

                TextField(
                    value = kva,
                    onValueChange = onKva,
                    label = {
                        Text("Rated kVA")
                    },
                    singleLine = true
                )

                TextField(
                    value = transformerZ,
                    onValueChange = onTransformerZ,
                    label = {
                        Text("Transformer %Z")
                    },
                    singleLine = true
                )

                TextField(
                    value = generatorXd,
                    onValueChange = onGeneratorXd,
                    label = {
                        Text("Generator Xd'' %")
                    },
                    singleLine = true
                )

                TextField(
                    value = sourceMva,
                    onValueChange = onSourceMva,
                    label = {
                        Text("Source Short Circuit MVA")
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    if (arabic) "حفظ" else "Save"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (arabic) "إلغاء" else "Cancel"
                )
            }
        }
    )
}

@Composable
private fun ConnectionDialog(
    arabic: Boolean,
    length: String,
    resistance: String,
    reactance: String,
    cableSize: String,
    runs: String,
    capacity: String,
    onLength: (String) -> Unit,
    onResistance: (String) -> Unit,
    onReactance: (String) -> Unit,
    onCableSize: (String) -> Unit,
    onRuns: (String) -> Unit,
    onCapacity: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (arabic) {
                    "بيانات الكابل"
                } else {
                    "Feeder / Cable Data"
                }
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                TextField(
                    value = length,
                    onValueChange = onLength,
                    label = {
                        Text("Length m")
                    },
                    singleLine = true
                )

                TextField(
                    value = resistance,
                    onValueChange = onResistance,
                    label = {
                        Text("R Ω/km")
                    },
                    singleLine = true
                )

                TextField(
                    value = reactance,
                    onValueChange = onReactance,
                    label = {
                        Text("X Ω/km")
                    },
                    singleLine = true
                )

                TextField(
                    value = cableSize,
                    onValueChange = onCableSize,
                    label = {
                        Text("Cable mm²")
                    },
                    singleLine = true
                )

                TextField(
                    value = runs,
                    onValueChange = onRuns,
                    label = {
                        Text("Parallel Runs")
                    },
                    singleLine = true
                )

                TextField(
                    value = capacity,
                    onValueChange = onCapacity,
                    label = {
                        Text("Current Capacity A")
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    if (arabic) "حفظ" else "Save"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (arabic) "إلغاء" else "Cancel"
                )
            }
        }
    )
}

/*
 * ============================================================
 * PROFESSIONAL SLD CANVAS
 * ============================================================
 *
 * Important change:
 *
 * The old implementation used two independent pointerInput
 * blocks:
 *
 * 1. detectTapGestures
 * 2. detectDragGestures
 *
 * They could compete for the same pointer event.
 *
 * This implementation uses ONE gesture pipeline for:
 *
 * - node selection
 * - node dragging
 * - connection selection
 * - empty canvas selection
 *
 * This makes node movement considerably more reliable.
 */

@Composable
private fun SldCanvas(
    modifier: Modifier,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    connectionStartId: String?,
    onNodeSelected: (String) -> Unit,
    onConnectionSelected: (String) -> Unit,
    onEmptySelected: () -> Unit,
    onNodeMoved: (String, Float, Float) -> Unit
) {

    val textMeasurer =
        rememberTextMeasurer()

    val nodeWidth = 120f
    val nodeHeight = 64f

    /*
     * Larger touch target than the visible element.
     *
     * This is especially useful on tablets/phones where
     * the SLD element is visually small.
     */
    val touchPadding = 45f

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                Color(0xFF0A1015),
                RoundedCornerShape(12.dp)
            )
    ) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes, connections) {

                    awaitEachGesture {

                        val down =
                            awaitFirstDown(
                                requireUnconsumed = false
                            )

                        val downPosition =
                            down.position

                        /*
                         * Find the node under the finger.
                         *
                         * Reverse order means the visually
                         * topmost node gets priority.
                         */
                        val touchedNode =
                            nodes
                                .asReversed()
                                .firstOrNull { node ->

                                    downPosition.x >=
                                        node.x -
                                            touchPadding &&

                                    downPosition.x <=
                                        node.x +
                                            nodeWidth +
                                            touchPadding &&

                                    downPosition.y >=
                                        node.y -
                                            touchPadding &&

                                    downPosition.y <=
                                        node.y +
                                            nodeHeight +
                                            touchPadding
                                }

                        var draggedNodeId =
                            touchedNode?.id

                        var dragging = false

                        var lastPosition =
                            downPosition

                        /*
                         * Select immediately when the finger
                         * touches a node.
                         */
                        if (touchedNode != null) {

                            onNodeSelected(
                                touchedNode.id
                            )
                        }

                        /*
                         * Wait for movement/release.
                         *
                         * We intentionally use the touch slop
                         * so a normal tap is not interpreted as
                         * a drag.
                         */
                        while (true) {

                            val event =
                                awaitPointerEvent()

                            val change =
                                event.changes
                                    .firstOrNull()
                                    ?: break

                            if (!change.pressed) {

                                /*
                                 * Finger released.
                                 *
                                 * If no drag occurred, check
                                 * whether the user tapped a
                                 * connection or empty canvas.
                                 */
                                if (!dragging) {

                                    if (touchedNode == null) {

                                        val connection =
                                            findConnectionAtPoint(
                                                point = downPosition,
                                                nodes = nodes,
                                                connections = connections
                                            )

                                        if (connection != null) {

                                            onConnectionSelected(
                                                connection.id
                                            )

                                        } else {

                                            onEmptySelected()
                                        }
                                    }
                                }

                                break
                            }

                            val currentPosition =
                                change.position

                            val dx =
                                currentPosition.x -
                                    downPosition.x

                            val dy =
                                currentPosition.y -
                                    downPosition.y

                            val distance =
                                hypot(
                                    dx,
                                    dy
                                )

                            /*
                             * Start dragging only after
                             * passing the platform touch slop.
                             */
                            if (
                                !dragging &&
                                draggedNodeId != null &&
                                distance >
                                    viewConfiguration.touchSlop
                            ) {

                                dragging = true

                                change.consume()
                            }

                            if (
                                dragging &&
                                draggedNodeId != null
                            ) {

                                val id =
                                    draggedNodeId!!

                                val node =
                                    nodes.firstOrNull {
                                        it.id == id
                                    }

                                if (node != null) {

                                    val moveX =
                                        currentPosition.x -
                                            lastPosition.x

                                    val moveY =
                                        currentPosition.y -
                                            lastPosition.y

                                    val maxX =
                                        (
                                            size.width -
                                                nodeWidth
                                        )
                                            .coerceAtLeast(
                                                0f
                                            )

                                    val maxY =
                                        (
                                            size.height -
                                                nodeHeight
                                        )
                                            .coerceAtLeast(
                                                0f
                                            )

                                    val newX =
                                        (
                                            node.x +
                                                moveX
                                        ).coerceIn(
                                            0f,
                                            maxX
                                        )

                                    val newY =
                                        (
                                            node.y +
                                                moveY
                                        ).coerceIn(
                                            0f,
                                            maxY
                                        )

                                    onNodeMoved(
                                        id,
                                        newX,
                                        newY
                                    )

                                    change.consume()
                                }
                            }

                            lastPosition =
                                currentPosition
                        }

                        draggedNodeId = null
                    }
                }
        ) {

            drawGrid()

            /*
             * ==================================================
             * CONNECTIONS
             * ==================================================
             */
            connections.forEach { connection ->

                val from =
                    nodes.firstOrNull {
                        it.id ==
                            connection.fromNodeId
                    }

                val to =
                    nodes.firstOrNull {
                        it.id ==
                            connection.toNodeId
                    }

                if (
                    from != null &&
                    to != null
                ) {

                    val start =
                        Offset(
                            from.x + nodeWidth,
                            from.y +
                                nodeHeight / 2f
                        )

                    val end =
                        Offset(
                            to.x,
                            to.y +
                                nodeHeight / 2f
                        )

                    val selected =
                        connection.id ==
                            selectedConnectionId

                    val color =
                        if (selected) {
                            Accent
                        } else {
                            Color(0xFF78909C)
                        }

                    /*
                     * Draw a slightly wider invisible-looking
                     * base line first to make the connection
                     * visually clearer and easier to hit.
                     */
                    drawLine(
                        color =
                            color.copy(
                                alpha =
                                    if (selected) {
                                        1f
                                    } else {
                                        0.75f
                                    }
                            ),
                        start = start,
                        end = end,
                        strokeWidth =
                            if (selected) {
                                5f
                            } else {
                                3f
                            }
                    )

                    drawArrow(
                        start = start,
                        end = end,
                        color = color
                    )
                }
            }

            /*
             * ==================================================
             * NODES
             * ==================================================
             */
            nodes.forEach { node ->

                drawNode(
                    node = node,
                    selected =
                        node.id ==
                            selectedNodeId,
                    connecting =
                        node.id ==
                            connectionStartId
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = node.name.take(17),
                    topLeft =
                        Offset(
                            node.x + 8f,
                            node.y + 7f
                        ),
                    style =
                        TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight =
                                FontWeight.Bold
                        ),
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = node.type.name,
                    topLeft =
                        Offset(
                            node.x + 8f,
                            node.y + 29f
                        ),
                    style =
                        TextStyle(
                            color = SecondaryText,
                            fontSize = 10.sp
                        ),
                    maxLines = 1
                )

                if (node.loadKw > 0.0) {

                    drawText(
                        textMeasurer = textMeasurer,
                        text =
                            "${node.loadKw} kW",
                        topLeft =
                            Offset(
                                node.x + 8f,
                                node.y + 47f
                            ),
                        style =
                            TextStyle(
                                color = Accent,
                                fontSize = 9.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            ),
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = "SLD",
            color = SecondaryText,
            modifier =
                Modifier
                    .align(
                        Alignment.BottomEnd
                    )
                    .padding(8.dp),
            fontSize = 11.sp
        )
    }
}

private fun DrawScope.drawGrid() {

    val step = 40f

    var x = 0f

    while (x <= size.width) {

        drawLine(
            color = Color(0xFF18242C),
            start = Offset(x, 0f),
            end =
                Offset(
                    x,
                    size.height
                ),
            strokeWidth = 1f
        )

        x += step
    }

    var y = 0f

    while (y <= size.height) {

        drawLine(
            color = Color(0xFF18242C),
            start = Offset(0f, y),
            end =
                Offset(
                    size.width,
                    y
                ),
            strokeWidth = 1f
        )

        y += step
    }
}

private fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connecting: Boolean
) {

    val fill =
        when (node.type) {

            SldNodeType.SOURCE ->
                Color(0xFF263238)

            SldNodeType.TRANSFORMER ->
                Color(0xFF263A46)

            SldNodeType.GENERATOR ->
                Color(0xFF30442E)

            SldNodeType.BUS ->
                Color(0xFF3A303F)

            SldNodeType.PANEL ->
                Color(0xFF263E48)

            SldNodeType.BREAKER ->
                Color(0xFF403A2D)

            SldNodeType.LOAD ->
                Color(0xFF303840)
        }

    val border =
        when {

            connecting ->
                Accent

            selected ->
                Color.White

            else ->
                Color(0xFF607D8B)
        }

    drawRoundRect(
        color = fill,
        topLeft =
            Offset(
                node.x,
                node.y
            ),
        size =
            androidx.compose.ui.geometry.Size(
                120f,
                64f
            ),
        cornerRadius =
            androidx.compose.ui.geometry
                .CornerRadius(
                    10f,
                    10f
                )
    )

    drawRoundRect(
        color = border,
        topLeft =
            Offset(
                node.x,
                node.y
            ),
        size =
            androidx.compose.ui.geometry.Size(
                120f,
                64f
            ),
        cornerRadius =
            androidx.compose.ui.geometry
                .CornerRadius(
                    10f,
                    10f
                ),
        style =
            Stroke(
                width =
                    if (
                        selected ||
                        connecting
                    ) {
                        3f
                    } else {
                        1.5f
                    }
            )
    )
}

private fun DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color
) {

    val dx =
        end.x - start.x

    val dy =
        end.y - start.y

    val length =
        hypot(dx, dy)

    if (length < 1f) return

    val ux =
        dx / length

    val uy =
        dy / length

    val px =
        -uy

    val py =
        ux

    val tip =
        Offset(
            start.x + dx * 0.72f,
            start.y + dy * 0.72f
        )

    val arrowSize = 8f

    val path =
        Path().apply {

            moveTo(
                tip.x,
                tip.y
            )

            lineTo(
                tip.x -
                    ux * arrowSize +
                    px * arrowSize * 0.55f,
                tip.y -
                    uy * arrowSize +
                    py * arrowSize * 0.55f
            )

            lineTo(
                tip.x -
                    ux * arrowSize -
                    px * arrowSize * 0.55f,
                tip.y -
                    uy * arrowSize -
                    py * arrowSize * 0.55f
            )

            close()
        }

    drawPath(
        path = path,
        color = color
    )
}

private fun findConnectionAtPoint(
    point: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {

    return connections.firstOrNull { connection ->

        val from =
            nodes.firstOrNull {
                it.id ==
                    connection.fromNodeId
            }

        val to =
            nodes.firstOrNull {
                it.id ==
                    connection.toNodeId
            }

        if (
            from == null ||
            to == null
        ) {
            false
        } else {

            val start =
                Offset(
                    from.x + 120f,
                    from.y + 32f
                )

            val end =
                Offset(
                    to.x,
                    to.y + 32f
                )

            distanceToSegment(
                point,
                start,
                end
            ) <= 18f
        }
    }
}

private fun distanceToSegment(
    point: Offset,
    start: Offset,
    end: Offset
): Float {

    val dx =
        end.x - start.x

    val dy =
        end.y - start.y

    if (
        dx == 0f &&
        dy == 0f
    ) {
        return hypot(
            point.x - start.x,
            point.y - start.y
        )
    }

    val t =
        (
            (
                point.x - start.x
            ) * dx +
                (
                    point.y - start.y
                ) * dy
        ) /
            (
                dx * dx +
                    dy * dy
            )

    val clamped =
        t.coerceIn(
            0f,
            1f
        )

    val x =
        start.x +
            clamped * dx

    val y =
        start.y +
            clamped * dy

    return hypot(
        point.x - x,
        point.y - y
    )
}
