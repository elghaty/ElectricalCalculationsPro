package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldCableSizingEngine
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringEngine
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldPanelScheduleEngine
import com.electrical.calculationspro.data.SldProtectionCoordinationEngine
import com.electrical.calculationspro.data.SldShortCircuitEngine
import kotlin.math.hypot
import kotlin.math.max

private val Background = Color(0xFF0B1116)
private val CanvasBackground = Color(0xFF081016)
private val CardColor = Color(0xFF151D24)
private val PrimaryText = Color(0xFFF2F5F7)
private val SecondaryText = Color(0xFF9BA8B2)
private val Accent = Color(0xFF00BCD4)
private val Danger = Color(0xFFE53935)
private val Success = Color(0xFF43A047)
private val LineColor = Color(0xFF90A4AE)
private val BusbarColor = Color(0xFFE0E0E0)

private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 64f
private const val NODE_TOUCH_PADDING = 45f

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {
    val arabic = language == AppLanguage.ARABIC

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

    var showResultDialog by remember {
        mutableStateOf(false)
    }

    var resultTitle by remember {
        mutableStateOf("")
    }

    var resultText by remember {
        mutableStateOf("")
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

    fun network(): SldNetwork =
        SldNetwork(
            nodes = nodes,
            connections = connections
        )

    fun defaultName(type: SldNodeType): String {
        val prefix = when (type) {
            SldNodeType.SOURCE -> "SOURCE"
            SldNodeType.BUS -> "BUS"
            SldNodeType.TRANSFORMER -> "TR"
            SldNodeType.GENERATOR -> "GEN"
            SldNodeType.BREAKER -> "CB"
            SldNodeType.PANEL -> "PANEL"
            SldNodeType.LOAD -> "LOAD"
        }

        return "$prefix-${nodes.count { it.type == type } + 1}"
    }

    fun openAdd(type: SldNodeType) {
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

    fun openEdit(node: SldNode) {
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

    fun openConnection(connection: SldConnection) {
        editingConnectionId = connection.id

        connectionLength = connection.lengthMeters.toString()
        connectionResistance = connection.resistanceOhmPerKm.toString()
        connectionReactance = connection.reactanceOhmPerKm.toString()
        connectionCableSize = connection.cableSizeMm2.toString()
        connectionParallelRuns = connection.parallelRuns.toString()
        connectionCurrentCapacity = connection.currentCapacityA.toString()

        showConnectionDialog = true
    }

    fun saveNode() {
        val voltage = nodeVoltage.toDoubleOrNull() ?: 400.0
        val kw = nodeKw.toDoubleOrNull() ?: 0.0
        val pf = (nodePf.toDoubleOrNull() ?: 0.90).coerceIn(0.01, 1.0)
        val demand = (nodeDemand.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0)
        val kva = nodeKva.toDoubleOrNull() ?: 0.0
        val transformerZ = nodeTransformerZ.toDoubleOrNull() ?: 0.0
        val generatorXd = nodeGeneratorXd.toDoubleOrNull() ?: 0.0
        val sourceMva = nodeSourceMva.toDoubleOrNull() ?: 0.0

        if (editingNodeId == null) {
            val newNode = SldNode(
                id = "node-${System.currentTimeMillis()}",
                name = nodeName.ifBlank {
                    defaultName(pendingNodeType)
                },
                type = pendingNodeType,
                x = (nodes.maxOfOrNull { it.x } ?: 80f) + 180f,
                y = nodes.lastOrNull()?.y ?: 160f,
                voltage = voltage,
                loadKw = kw,
                powerFactor = pf,
                demandFactor = demand,
                ratedKva = kva,
                transformerPercentZ = transformerZ,
                generatorXdSubtransient = generatorXd,
                sourceShortCircuitMva = sourceMva
            )

            nodes = nodes + newNode
            selectedNodeId = newNode.id
            selectedConnectionId = null
        } else {
            val id = editingNodeId!!

            nodes = nodes.map { node ->
                if (node.id == id) {
                    node.copy(
                        name = nodeName.ifBlank { node.name },
                        type = pendingNodeType,
                        voltage = voltage,
                        loadKw = kw,
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

        showNodeDialog = false
    }

    fun saveConnection() {
        val length =
            (connectionLength.toDoubleOrNull() ?: 0.0)
                .coerceAtLeast(0.0)

        val resistance =
            connectionResistance.toDoubleOrNull() ?: 0.0

        val reactance =
            connectionReactance.toDoubleOrNull() ?: 0.0

        val cableSize =
            connectionCableSize.toDoubleOrNull() ?: 0.0

        val runs =
            (connectionParallelRuns.toIntOrNull() ?: 1)
                .coerceAtLeast(1)

        val capacity =
            connectionCurrentCapacity.toDoubleOrNull() ?: 0.0

        if (editingConnectionId == null) {
            val from = connectionStartId ?: return
            val to = selectedNodeId ?: return

            if (from == to) {
                connectionStartId = null
                return
            }

            val exists = connections.any {
                (it.fromNodeId == from && it.toNodeId == to) ||
                    (it.fromNodeId == to && it.toNodeId == from)
            }

            if (!exists) {
                val connection = SldConnection(
                    id = "connection-${System.currentTimeMillis()}",
                    fromNodeId = from,
                    toNodeId = to,
                    lengthMeters = length,
                    resistanceOhmPerKm = resistance,
                    reactanceOhmPerKm = reactance,
                    cableSizeMm2 = cableSize,
                    parallelRuns = runs,
                    currentCapacityA = capacity
                )

                connections = connections + connection
                selectedConnectionId = connection.id
                selectedNodeId = null
            }
        } else {
            val id = editingConnectionId!!

            connections = connections.map { connection ->
                if (connection.id == id) {
                    connection.copy(
                        lengthMeters = length,
                        resistanceOhmPerKm = resistance,
                        reactanceOhmPerKm = reactance,
                        cableSizeMm2 = cableSize,
                        parallelRuns = runs,
                        currentCapacityA = capacity
                    )
                } else {
                    connection
                }
            }
        }

        connectionStartId = null
        showConnectionDialog = false
    }

    fun connectSelected() {
        val selected = selectedNodeId ?: return

        if (connectionStartId == null) {
            connectionStartId = selected
            selectedConnectionId = null
            return
        }

        if (connectionStartId == selected) {
            connectionStartId = null
            return
        }

        val from = connectionStartId!!

        val exists = connections.any {
            (it.fromNodeId == from && it.toNodeId == selected) ||
                (it.fromNodeId == selected && it.toNodeId == from)
        }

        if (!exists) {
            val connection = SldConnection(
                id = "connection-${System.currentTimeMillis()}",
                fromNodeId = from,
                toNodeId = selected
            )

            connections = connections + connection
            selectedConnectionId = connection.id
        }

        selectedNodeId = null
        connectionStartId = null
    }

    fun deleteSelected() {
        selectedNodeId?.let { id ->
            if (id != "source-1") {
                nodes = nodes.filterNot { it.id == id }

                connections = connections.filter {
                    it.fromNodeId != id &&
                        it.toNodeId != id
                }

                selectedNodeId = null
                selectedConnectionId = null
                connectionStartId = null
                return
            }
        }

        selectedConnectionId?.let { id ->
            connections = connections.filterNot {
                it.id == id
            }

            selectedConnectionId = null
        }
    }

    fun showCalculation(title: String, block: () -> Any) {
        try {
            resultTitle = title
            resultText = formatEngineeringResult(
                block().toString()
            )
        } catch (e: Exception) {
            resultTitle = if (arabic) "خطأ" else "Error"
            resultText = e.message ?: "Calculation error"
        }

        showResultDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                TextButton(onClick = it) {
                    Text(
                        text = if (arabic) "رجوع" else "Back",
                        color = Accent
                    )
                }
            }

            Text(
                modifier = Modifier.weight(1f),
                text = if (arabic) {
                    "المخطط الأحادي SLD الاحترافي"
                } else {
                    "Professional Single Line Diagram"
                },
                color = PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButton(
                text = if (arabic) "مصدر" else "Source"
            ) {
                openAdd(SldNodeType.SOURCE)
            }

            ToolButton(
                text = if (arabic) "باسبار" else "Busbar"
            ) {
                openAdd(SldNodeType.BUS)
            }

            ToolButton(
                text = if (arabic) "محول" else "Transformer"
            ) {
                openAdd(SldNodeType.TRANSFORMER)
            }

            ToolButton(
                text = if (arabic) "مولد" else "Generator"
            ) {
                openAdd(SldNodeType.GENERATOR)
            }

            ToolButton(
                text = if (arabic) "قاطع" else "Breaker"
            ) {
                openAdd(SldNodeType.BREAKER)
            }

            ToolButton(
                text = if (arabic) "لوحة" else "Panel"
            ) {
                openAdd(SldNodeType.PANEL)
            }

            ToolButton(
                text = if (arabic) "حمل" else "Load"
            ) {
                openAdd(SldNodeType.LOAD)
            }

            ToolButton(
                text = if (arabic) "ربط" else "Connect"
            ) {
                connectSelected()
            }

            ToolButton(
                text = if (arabic) "حذف" else "Delete"
            ) {
                deleteSelected()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButton(
                text = if (arabic) "الحسابات" else "Upstream"
            ) {
                showCalculation(
                    if (arabic) "الحسابات" else "Upstream Calculation"
                ) {
                    SldEngineeringEngine.calculateUpstream(network())
                }
            }

            ToolButton(
                text = if (arabic) "تيار القصر" else "Short Circuit"
            ) {
                showCalculation(
                    if (arabic) "تيار القصر" else "Short Circuit"
                ) {
                    SldShortCircuitEngine.calculate(network())
                }
            }

            ToolButton(
                text = if (arabic) "الكابلات" else "Cable Sizing"
            ) {
                showCalculation(
                    if (arabic) "اختيار الكابلات" else "Cable Sizing"
                ) {
                    val sc =
                        SldShortCircuitEngine.calculate(network())

                    SldCableSizingEngine.calculate(
                        network = network(),
                        shortCircuitStudy = sc
                    )
                }
            }

            ToolButton(
                text = if (arabic) "الحماية" else "Protection"
            ) {
                showCalculation(
                    if (arabic) "تنسيق الحمايات" else "Protection"
                ) {
                    val sc =
                        SldShortCircuitEngine.calculate(network())

                    val cable =
                        SldCableSizingEngine.calculate(
                            network = network(),
                            shortCircuitStudy = sc
                        )

                    SldProtectionCoordinationEngine.calculate(
                        network = network(),
                        shortCircuitStudy = sc,
                        cableSizingStudy = cable
                    )
                }
            }

            ToolButton(
                text = if (arabic) "جدول اللوحة" else "Panel Schedule"
            ) {
                val panel = selectedNodeId

                if (panel != null) {
                    showCalculation(
                        if (arabic) "جدول اللوحة" else "Panel Schedule"
                    ) {
                        val sc =
                            SldShortCircuitEngine.calculate(network())

                        val cable =
                            SldCableSizingEngine.calculate(
                                network = network(),
                                shortCircuitStudy = sc
                            )

                        SldPanelScheduleEngine.calculate(
                            network = network(),
                            panelNodeId = panel,
                            cableSizingStudy = cable
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            SldCanvas(
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                connectionStartId = connectionStartId,
                onSelectNode = {
                    selectedNodeId = it
                    selectedConnectionId = null
                },
                onMoveNode = { id, x, y ->
                    nodes = nodes.map { node ->
                        if (node.id == id) {
                            node.copy(
                                x = max(0f, x),
                                y = max(0f, y)
                            )
                        } else {
                            node
                        }
                    }
                },
                onSelectConnection = {
                    selectedConnectionId = it
                    selectedNodeId = null
                },
                onEditNode = {
                    openEdit(it)
                },
                onEditConnection = {
                    openConnection(it)
                }
            )
        }
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
            onType = { pendingNodeType = it },
            onName = { nodeName = it },
            onVoltage = { nodeVoltage = it },
            onKw = { nodeKw = it },
            onPf = { nodePf = it },
            onDemand = { nodeDemand = it },
            onKva = { nodeKva = it },
            onTransformerZ = { nodeTransformerZ = it },
            onGeneratorXd = { nodeGeneratorXd = it },
            onSourceMva = { nodeSourceMva = it },
            onSave = ::saveNode,
            onDismiss = {
                showNodeDialog = false
            }
        )
    }

    if (showConnectionDialog) {
        ConnectionEditorDialog(
            arabic = arabic,
            length = connectionLength,
            resistance = connectionResistance,
            reactance = connectionReactance,
            cableSize = connectionCableSize,
            runs = connectionParallelRuns,
            capacity = connectionCurrentCapacity,
            onLength = { connectionLength = it },
            onResistance = { connectionResistance = it },
            onReactance = { connectionReactance = it },
            onCableSize = { connectionCableSize = it },
            onRuns = { connectionParallelRuns = it },
            onCapacity = { connectionCurrentCapacity = it },
            onSave = ::saveConnection,
            onDismiss = {
                showConnectionDialog = false
            }
        )
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
            },
            title = {
                Text(
                    text = resultTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    Text(
                        text = resultText,
                        color = PrimaryText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResultDialog = false
                    }
                ) {
                    Text(
                        if (arabic) "إغلاق" else "Close",
                        color = Accent
                    )
                }
            }
        )
    }
}

private fun formatEngineeringResult(raw: String): String {
    return raw
        .replace(
            "SldShortCircuitStudy(",
            "SHORT-CIRCUIT STUDY\n\n"
        )
        .replace(
            "SldShortCircuitResult(",
            "\n━━━━━━━━━━━━━━━━━━━━━━━━━━\nBUS / NODE RESULT\n"
        )
        .replace(
            "nodeId=",
            "\nNode ID       : "
        )
        .replace(
            "nodeName=",
            "\nNode Name     : "
        )
        .replace(
            "voltageV=",
            "\nVoltage        : "
        )
        .replace(
            "resistanceOhm=",
            "\nResistance     : "
        )
        .replace(
            "reactanceOhm=",
            "\nReactance      : "
        )
        .replace(
            "impedanceOhm=",
            "\nImpedance      : "
        )
        .replace(
            "xrRatio=",
            "\nX/R Ratio      : "
        )
        .replace(
            "shortCircuitMva=",
            "\nFault Level    : "
        )
        .replace(
            "initialSymmetricalCurrentKa=",
            "\nIk''           : "
        )
        .replace(
            "peakCurrentKa=",
            "\nIp Peak        : "
        )
        .replace(
            "thermalCurrentKa=",
            "\nIth Thermal    : "
        )
        .replace(
            "breakerRatedCurrentKa=",
            "\nBreaker Rating : "
        )
        .replace(
            "maximumFaultCurrentKa=",
            "\nMaximum Ik''   : "
        )
        .replace(
            "maximumPeakCurrentKa=",
            "\nMaximum Ip     : "
        )
        .replace(
            "maximumFaultMva=",
            "\nMaximum Fault  : "
        )
        .replace(
            "nodes=",
            "\nNodes          : "
        )
        .replace(
            "style=",
            "\nCalculation    : "
        )
        .replace(
            "Voltage factor",
            "\nVoltage factor"
        )
        .replace(
            "Maximum Ik''",
            "\nMaximum Ik''"
        )
        .replace(
            "Maximum peak current",
            "\nMaximum peak current"
        )
        .replace(
            "Maximum fault level",
            "\nMaximum fault level"
        )
        .replace(
            "method=",
            "\nMethod         : "
        )
        .replace(
            "results={",
            "\n"
        )
        .replace(
            "})",
            ""
        )
}

@Composable
private fun ToolButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(42.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SldCanvas(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    connectionStartId: String?,
    onSelectNode: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onSelectConnection: (String) -> Unit,
    onEditNode: (SldNode) -> Unit,
    onEditConnection: (SldConnection) -> Unit
) {
    val currentNodes by rememberUpdatedState(nodes)
    val currentConnections by rememberUpdatedState(connections)
    val currentOnSelectNode by rememberUpdatedState(onSelectNode)
    val currentOnMoveNode by rememberUpdatedState(onMoveNode)
    val currentOnSelectConnection by rememberUpdatedState(onSelectConnection)
    val currentOnEditNode by rememberUpdatedState(onEditNode)
    val currentOnEditConnection by rememberUpdatedState(onEditConnection)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { position ->
                            val node =
                                findNode(
                                    position,
                                    currentNodes
                                )

                            if (node != null) {
                                currentOnEditNode(node)
                                return@detectTapGestures
                            }

                            val connection =
                                findConnection(
                                    position,
                                    currentConnections,
                                    currentNodes
                                )

                            if (connection != null) {
                                currentOnEditConnection(connection)
                            }
                        },
                        onTap = { position ->
                            val node =
                                findNode(
                                    position,
                                    currentNodes
                                )

                            if (node != null) {
                                currentOnSelectNode(node.id)
                                return@detectTapGestures
                            }

                            val connection =
                                findConnection(
                                    position,
                                    currentConnections,
                                    currentNodes
                                )

                            if (connection != null) {
                                currentOnSelectConnection(
                                    connection.id
                                )
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    var draggedNodeId: String? = null

                    detectDragGestures(
                        onDragStart = { position ->
                            val node =
                                findNode(
                                    position,
                                    currentNodes
                                )

                            draggedNodeId = node?.id

                            if (node != null) {
                                currentOnSelectNode(node.id)
                            }
                        },
                        onDragEnd = {
                            draggedNodeId = null
                        },
                        onDragCancel = {
                            draggedNodeId = null
                        },
                        onDrag = { change, dragAmount ->
                            val id = draggedNodeId

                            if (id != null) {
                                val node =
                                    currentNodes.firstOrNull {
                                        it.id == id
                                    }

                                if (node != null) {
                                    currentOnMoveNode(
                                        id,
                                        node.x + dragAmount.x,
                                        node.y + dragAmount.y
                                    )
                                }
                            }

                            change.consume()
                        }
                    )
                }
        ) {
            drawGrid()

            currentConnections.forEach { connection ->
                val from =
                    currentNodes.firstOrNull {
                        it.id == connection.fromNodeId
                    }

                val to =
                    currentNodes.firstOrNull {
                        it.id == connection.toNodeId
                    }

                if (from != null && to != null) {
                    drawConnection(
                        from = from,
                        to = to,
                        selected =
                            connection.id ==
                                selectedConnectionId
                    )
                }
            }

            currentNodes.forEach { node ->
                drawNode(
                    node = node,
                    selected =
                        node.id == selectedNodeId,
                    connectionStart =
                        node.id == connectionStartId
                )
            }
        }
    }
}

private fun DrawScope.drawGrid() {
    val spacing = 40f

    var x = 0f
    while (x < size.width) {
        drawLine(
            color = Color(0xFF101A21),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += spacing
    }

    var y = 0f
    while (y < size.height) {
        drawLine(
            color = Color(0xFF101A21),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += spacing
    }
}

private fun findNode(
    position: Offset,
    nodes: List<SldNode>
): SldNode? {
    return nodes.asReversed().firstOrNull { node ->
        position.x >= node.x - NODE_TOUCH_PADDING &&
            position.x <=
            node.x + NODE_WIDTH + NODE_TOUCH_PADDING &&
            position.y >= node.y - NODE_TOUCH_PADDING &&
            position.y <=
            node.y + NODE_HEIGHT + NODE_TOUCH_PADDING
    }
}

private fun findConnection(
    position: Offset,
    connections: List<SldConnection>,
    nodes: List<SldNode>
): SldConnection? {
    return connections.firstOrNull { connection ->
        val from =
            nodes.firstOrNull {
                it.id == connection.fromNodeId
            }

        val to =
            nodes.firstOrNull {
                it.id == connection.toNodeId
            }

        if (from == null || to == null) {
            false
        } else {
            val start =
                Offset(
                    from.x + NODE_WIDTH / 2f,
                    from.y + NODE_HEIGHT / 2f
                )

            val end =
                Offset(
                    to.x + NODE_WIDTH / 2f,
                    to.y + NODE_HEIGHT / 2f
                )

            distanceToSegment(
                position,
                start,
                end
            ) <= 18f
        }
    }
}

private fun distanceToSegment(
    p: Offset,
    a: Offset,
    b: Offset
): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y

    if (dx == 0f && dy == 0f) {
        return hypot(
            p.x - a.x,
            p.y - a.y
        )
    }

    val t =
        (
            ((p.x - a.x) * dx) +
                ((p.y - a.y) * dy)
            ) /
            ((dx * dx) + (dy * dy))

    val clamped =
        t.coerceIn(0f, 1f)

    val x =
        a.x + clamped * dx

    val y =
        a.y + clamped * dy

    return hypot(
        p.x - x,
        p.y - y
    )
}

private fun DrawScope.drawConnection(
    from: SldNode,
    to: SldNode,
    selected: Boolean
) {
    val start =
        Offset(
            from.x + NODE_WIDTH / 2f,
            from.y + NODE_HEIGHT / 2f
        )

    val end =
        Offset(
            to.x + NODE_WIDTH / 2f,
            to.y + NODE_HEIGHT / 2f
        )

    val color =
        if (selected) Accent else LineColor

    val path =
        Path().apply {
            moveTo(start.x, start.y)

            val middleX =
                (start.x + end.x) / 2f

            lineTo(
                middleX,
                start.y
            )

            lineTo(
                middleX,
                end.y
            )

            lineTo(
                end.x,
                end.y
            )
        }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width =
                if (selected) {
                    5f
                } else {
                    3f
                }
        )
    )

    drawConnectionLabel(
        from = from,
        to = to,
        connection = null
    )
}

private fun DrawScope.drawConnectionLabel(
    from: SldNode,
    to: SldNode,
    connection: SldConnection?
) {
    val centerX =
        (from.x + to.x + NODE_WIDTH) / 2f

    val centerY =
        (from.y + to.y + NODE_HEIGHT) / 2f

    if (connection == null) {
        return
    }

    drawIntoCanvas { canvas ->
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.LTGRAY
                textSize = 10f
                typeface =
                    android.graphics.Typeface.DEFAULT
            }

        val label =
            buildString {
                if (connection.cableSizeMm2 > 0.0) {
                    append(
                        "${connection.cableSizeMm2} mm²"
                    )
                }

                if (connection.parallelRuns > 1) {
                    append(
                        " × ${connection.parallelRuns}"
                    )
                }
            }

        if (label.isNotBlank()) {
            canvas.nativeCanvas.drawText(
                label,
                centerX,
                centerY,
                paint
            )
        }
    }
}

private fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    when (node.type) {
        SldNodeType.SOURCE ->
            drawSourceSymbol(
                node,
                selected,
                connectionStart
            )

        SldNodeType.BUS ->
            drawBusbarSymbol(
                node,
                selected,
                connectionStart
            )

        SldNodeType.TRANSFORMER ->
            drawTransformerSymbol(
                node,
                selected,
                connectionStart
            )

        SldNodeType.GENERATOR ->
            drawGeneratorSymbol(
                node,
                selected,
                connectionStart
            )

        SldNodeType.BREAKER ->
            drawBreakerSymbol(
                node,
                selected,
                connectionStart
            )

        SldNodeType.PANEL ->
            drawPanelSymbol(
                node,
                selected,
                connectionStart
            )

        SldNodeType.LOAD ->
            drawLoadSymbol(
                node,
                selected,
                connectionStart
            )
    }
}

private fun DrawScope.drawSymbolFrame(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    fill: Color
) {
    drawRoundRect(
        color =
            if (connectionStart) {
                Accent
            } else {
                fill
            },
        topLeft =
            Offset(
                node.x,
                node.y
            ),
        size =
            Size(
                NODE_WIDTH,
                NODE_HEIGHT
            ),
        cornerRadius =
            CornerRadius(
                8f,
                8f
            )
    )

    if (selected) {
        drawRoundRect(
            color = Accent,
            topLeft =
                Offset(
                    node.x - 3f,
                    node.y - 3f
                ),
            size =
                Size(
                    NODE_WIDTH + 6f,
                    NODE_HEIGHT + 6f
                ),
            cornerRadius =
                CornerRadius(
                    10f,
                    10f
                ),
            style = Stroke(3f)
        )
    }
}

private fun DrawScope.drawSourceSymbol(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    drawSymbolFrame(
        node,
        selected,
        connectionStart,
        Color(0xFF1565C0)
    )

    val center =
        Offset(
            node.x + NODE_WIDTH / 2f,
            node.y + 29f
        )

    drawCircle(
        color = Color.White,
        radius = 17f,
        center = center,
        style = Stroke(2.5f)
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                center.x - 10f,
                center.y
            ),
        end =
            Offset(
                center.x + 10f,
                center.y
            ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                center.x,
                center.y - 10f
            ),
        end =
            Offset(
                center.x,
                center.y + 10f
            ),
        strokeWidth = 2f
    )

    drawNodeText(
        node,
        subtitle = "SOURCE"
    )
}

private fun DrawScope.drawBusbarSymbol(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    drawSymbolFrame(
        node,
        selected,
        connectionStart,
        Color(0xFF6A1B9A)
    )

    val left = node.x + 12f
    val right = node.x + NODE_WIDTH - 12f
    val y = node.y + 28f

    drawLine(
        color =
            if (connectionStart) {
                Color.White
            } else {
                BusbarColor
            },
        start = Offset(left, y),
        end = Offset(right, y),
        strokeWidth = 9f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                node.x + 25f,
                y - 8f
            ),
        end =
            Offset(
                node.x + 25f,
                y + 8f
            ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                node.x + 60f,
                y - 8f
            ),
        end =
            Offset(
                node.x + 60f,
                y + 8f
            ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                node.x + 95f,
                y - 8f
            ),
        end =
            Offset(
                node.x + 95f,
                y + 8f
            ),
        strokeWidth = 2f
    )

    drawNodeText(
        node,
        subtitle = "BUSBAR"
    )
}

private fun DrawScope.drawTransformerSymbol(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    drawSymbolFrame(
        node,
        selected,
        connectionStart,
        Color(0xFFEF6C00)
    )

    val center =
        Offset(
            node.x + NODE_WIDTH / 2f,
            node.y + 29f
        )

    drawCircle(
        color = Color.White,
        radius = 16f,
        center =
            Offset(
                center.x - 11f,
                center.y
            ),
        style = Stroke(2.5f)
    )

    drawCircle(
        color = Color.White,
        radius = 16f,
        center =
            Offset(
                center.x + 11f,
                center.y
            ),
        style = Stroke(2.5f)
    )

    drawNodeText(
        node,
        subtitle = "TRANSFORMER"
    )
}

private fun DrawScope.drawGeneratorSymbol(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    drawSymbolFrame(
        node,
        selected,
        connectionStart,
        Color(0xFF2E7D32)
    )

    val center =
        Offset(
            node.x + NODE_WIDTH / 2f,
            node.y + 29f
        )

    drawCircle(
        color = Color.White,
        radius = 19f,
        center = center,
        style = Stroke(2.5f)
    )

    drawIntoCanvas { canvas ->
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.WHITE
                textSize = 17f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                textAlign =
                    android.graphics.Paint.Align.CENTER
            }

        canvas.nativeCanvas.drawText(
            "G",
            center.x,
            center.y + 6f,
            paint
        )
    }

    drawNodeText(
        node,
        subtitle = "GENERATOR"
    )
}

private fun DrawScope.drawBreakerSymbol(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    drawSymbolFrame(
        node,
        selected,
        connectionStart,
        Color(0xFF455A64)
    )

    val cx = node.x + NODE_WIDTH / 2f
    val cy = node.y + 29f

    drawLine(
        color = Color.White,
        start =
            Offset(
                cx - 30f,
                cy
            ),
        end =
            Offset(
                cx - 10f,
                cy
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                cx + 10f,
                cy
            ),
        end =
            Offset(
                cx + 30f,
                cy
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                cx - 10f,
                cy
            ),
        end =
            Offset(
                cx + 8f,
                cy - 16f
            ),
        strokeWidth = 3f
    )

    drawCircle(
        color = Color.White,
        radius = 3f,
        center =
            Offset(
                cx - 10f,
                cy
            )
    )

    drawCircle(
        color = Color.White,
        radius = 3f,
        center =
            Offset(
                cx + 10f,
                cy
            )
    )

    drawNodeText(
        node,
        subtitle = "BREAKER"
    )
}

private fun DrawScope.drawPanelSymbol(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    drawSymbolFrame(
        node,
        selected,
        connectionStart,
        Color(0xFF00838F)
    )

    val left = node.x + 18f
    val top = node.y + 10f

    drawRect(
        color = Color.White,
        topLeft =
            Offset(
                left,
                top
            ),
        size =
            Size(
                84f,
                38f
            ),
        style = Stroke(2.5f)
    )

    for (i in 0..2) {
        val yy =
            top + 10f + i * 9f

        drawLine(
            color = Color.White,
            start =
                Offset(
                    left + 12f,
                    yy
                ),
            end =
                Offset(
                    left + 72f,
                    yy
                ),
            strokeWidth = 2f
        )
    }

    drawNodeText(
        node,
        subtitle = "PANEL"
    )
}

private fun DrawScope.drawLoadSymbol(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    drawSymbolFrame(
        node,
        selected,
        connectionStart,
        Color(0xFF37474F)
    )

    val center =
        Offset(
            node.x + NODE_WIDTH / 2f,
            node.y + 29f
        )

    drawCircle(
        color = Color.White,
        radius = 17f,
        center = center,
        style = Stroke(2.5f)
    )

    val path =
        Path().apply {
            moveTo(
                center.x - 9f,
                center.y + 2f
            )
            lineTo(
                center.x - 2f,
                center.y - 9f
            )
            lineTo(
                center.x + 1f,
                center.y - 1f
            )
            lineTo(
                center.x + 9f,
                center.y - 1f
            )
            lineTo(
                center.x + 2f,
                center.y + 10f
            )
            lineTo(
                center.x - 1f,
                center.y + 2f
            )
            close()
        }

    drawPath(
        path = path,
        color = Color.White,
        style = Stroke(2f)
    )

    drawNodeText(
        node,
        subtitle = "LOAD"
    )
}

private fun DrawScope.drawNodeText(
    node: SldNode,
    subtitle: String
) {
    drawIntoCanvas { canvas ->
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.WHITE
                textSize = 12f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
                textAlign =
                    android.graphics.Paint.Align.CENTER
            }

        canvas.nativeCanvas.drawText(
            node.name.take(18),
            node.x + NODE_WIDTH / 2f,
            node.y + 58f,
            paint
        )

        paint.textSize = 8f
        paint.typeface =
            android.graphics.Typeface.DEFAULT

        canvas.nativeCanvas.drawText(
            subtitle,
            node.x + NODE_WIDTH / 2f,
            node.y + 8f,
            paint
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
    onType: (SldNodeType) -> Unit,
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
                    if (arabic) {
                        "تعديل العنصر"
                    } else {
                        "Edit Element"
                    }
                } else {
                    if (arabic) {
                        "إضافة عنصر"
                    } else {
                        "Add Element"
                    }
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                TypeSelector(
                    type = type,
                    arabic = arabic,
                    onType = onType
                )

                EditorField(
                    value = name,
                    label =
                        if (arabic) {
                            "الاسم"
                        } else {
                            "Name"
                        },
                    onValueChange = onName
                )

                EditorField(
                    value = voltage,
                    label =
                        if (arabic) {
                            "الجهد V"
                        } else {
                            "Voltage V"
                        },
                    onValueChange = onVoltage
                )

                EditorField(
                    value = kw,
                    label =
                        if (arabic) {
                            "الحمل kW"
                        } else {
                            "Load kW"
                        },
                    onValueChange = onKw
                )

                EditorField(
                    value = pf,
                    label =
                        if (arabic) {
                            "معامل القدرة"
                        } else {
                            "Power Factor"
                        },
                    onValueChange = onPf
                )

                EditorField(
                    value = demand,
                    label =
                        if (arabic) {
                            "معامل الطلب"
                        } else {
                            "Demand Factor"
                        },
                    onValueChange = onDemand
                )

                EditorField(
                    value = kva,
                    label =
                        if (arabic) {
                            "القدرة kVA"
                        } else {
                            "Rated kVA"
                        },
                    onValueChange = onKva
                )

                if (type == SldNodeType.TRANSFORMER) {
                    EditorField(
                        value = transformerZ,
                        label =
                            if (arabic) {
                                "الممانعة %Z"
                            } else {
                                "Transformer %Z"
                            },
                        onValueChange =
                            onTransformerZ
                    )
                }

                if (type == SldNodeType.GENERATOR) {
                    EditorField(
                        value = generatorXd,
                        label = "Xd'' %",
                        onValueChange =
                            onGeneratorXd
                    )
                }

                if (type == SldNodeType.SOURCE) {
                    EditorField(
                        value = sourceMva,
                        label =
                            if (arabic) {
                                "قدرة القصر MVA"
                            } else {
                                "Source Short Circuit MVA"
                            },
                        onValueChange =
                            onSourceMva
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave
            ) {
                Text(
                    if (arabic) {
                        "حفظ"
                    } else {
                        "Save"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (arabic) {
                        "إلغاء"
                    } else {
                        "Cancel"
                    }
                )
            }
        }
    )
}

@Composable
private fun ConnectionEditorDialog(
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
                    "بيانات التوصيل"
                } else {
                    "Connection Data"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                EditorField(
                    value = length,
                    label =
                        if (arabic) {
                            "الطول m"
                        } else {
                            "Length m"
                        },
                    onValueChange = onLength
                )

                EditorField(
                    value = resistance,
                    label =
                        if (arabic) {
                            "المقاومة Ω/km"
                        } else {
                            "Resistance Ω/km"
                        },
                    onValueChange = onResistance
                )

                EditorField(
                    value = reactance,
                    label =
                        if (arabic) {
                            "المفاعلة Ω/km"
                        } else {
                            "Reactance Ω/km"
                        },
                    onValueChange = onReactance
                )

                EditorField(
                    value = cableSize,
                    label =
                        if (arabic) {
                            "مقطع الكابل mm²"
                        } else {
                            "Cable Section mm²"
                        },
                    onValueChange = onCableSize
                )

                EditorField(
                    value = runs,
                    label =
                        if (arabic) {
                            "عدد المسارات"
                        } else {
                            "Parallel Runs"
                        },
                    onValueChange = onRuns
                )

                EditorField(
                    value = capacity,
                    label =
                        if (arabic) {
                            "سعة التيار A"
                        } else {
                            "Current Capacity A"
                        },
                    onValueChange = onCapacity
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave
            ) {
                Text(
                    if (arabic) {
                        "حفظ"
                    } else {
                        "Save"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (arabic) {
                        "إلغاء"
                    } else {
                        "Cancel"
                    }
                )
            }
        }
    )
}

@Composable
private fun EditorField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        singleLine = true
    )
}

@Composable
private fun TypeSelector(
    type: SldNodeType,
    arabic: Boolean,
    onType: (SldNodeType) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = type.name,
            onValueChange = {},
            label = {
                Text(
                    if (arabic) {
                        "نوع العنصر"
                    } else {
                        "Element Type"
                    }
                )
            },
            readOnly = true
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Color.Transparent
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        expanded = true
                    }
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            SldNodeType.entries.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.name)
                    },
                    onClick = {
                        onType(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
