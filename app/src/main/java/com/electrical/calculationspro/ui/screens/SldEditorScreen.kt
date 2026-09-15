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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldShortCircuitEngine
import com.electrical.calculationspro.data.SldShortCircuitStudy
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private val Background = Color(0xFF070C10)
private val CanvasBackground = Color(0xFF081217)
private val CardColor = Color(0xFF131D24)
private val PrimaryText = Color(0xFFF2F5F7)
private val SecondaryText = Color(0xFF9BA8B2)
private val Accent = Color(0xFF00BCD4)
private val SourceColor = Color(0xFF1565C0)
private val TransformerColor = Color(0xFFEF6C00)
private val GeneratorColor = Color(0xFF2E7D32)
private val BreakerColor = Color(0xFF455A64)
private val BusColor = Color(0xFF7B1FA2)
private val PanelColor = Color(0xFF00838F)
private val LoadColor = Color(0xFF37474F)
private val CableColor = Color(0xFFB0BEC5)
private val Danger = Color(0xFFE53935)
private val Success = Color(0xFF43A047)

private const val NODE_WIDTH = 170f
private const val NODE_HEIGHT = 112f
private const val NODE_TOUCH_PADDING = 32f

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
                    x = 60f,
                    y = 180f,
                    voltage = 400.0,
                    sourceShortCircuitMva = 500.0
                )
            )
        )
    }

    var connections by remember {
        mutableStateOf(emptyList<SldConnection>())
    }

    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var selectedConnectionId by remember { mutableStateOf<String?>(null) }
    var connectionStartId by remember { mutableStateOf<String?>(null) }

    var editingNodeId by remember { mutableStateOf<String?>(null) }
    var editingConnectionId by remember { mutableStateOf<String?>(null) }

    var showNodeDialog by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }

    var resultTitle by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    var pendingNodeType by remember {
        mutableStateOf(SldNodeType.LOAD)
    }

    var nodeName by remember { mutableStateOf("") }
    var nodeVoltage by remember { mutableStateOf("400") }
    var nodeKw by remember { mutableStateOf("50") }
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
            SldNodeType.TRANSFORMER -> "TR"
            SldNodeType.GENERATOR -> "GEN"
            SldNodeType.BUS -> "BUS"
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
        nodeKw = if (type == SldNodeType.LOAD) "50" else "0"
        nodePf = "0.90"
        nodeDemand = "1.0"
        nodeKva = if (type == SldNodeType.TRANSFORMER) "1000" else "0"
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
        val z = nodeTransformerZ.toDoubleOrNull() ?: 0.0
        val xd = nodeGeneratorXd.toDoubleOrNull() ?: 0.0
        val sourceMva = nodeSourceMva.toDoubleOrNull() ?: 0.0

        if (editingNodeId == null) {
            val newIndex = nodes.size

            val newNode = SldNode(
                id = "node-${System.nanoTime()}",
                name = nodeName.ifBlank {
                    defaultName(pendingNodeType)
                },
                type = pendingNodeType,
                x = 100f + newIndex * 210f,
                y = 180f,
                voltage = voltage,
                loadKw = kw,
                powerFactor = pf,
                demandFactor = demand,
                ratedKva = kva,
                transformerPercentZ = z,
                generatorXdSubtransient = xd,
                sourceShortCircuitMva = sourceMva
            )

            nodes = nodes + newNode
            selectedNodeId = newNode.id
            selectedConnectionId = null
        } else {
            val id = editingNodeId!!

            nodes = nodes.map {
                if (it.id == id) {
                    it.copy(
                        name = nodeName.ifBlank { it.name },
                        voltage = voltage,
                        loadKw = kw,
                        powerFactor = pf,
                        demandFactor = demand,
                        ratedKva = kva,
                        transformerPercentZ = z,
                        generatorXdSubtransient = xd,
                        sourceShortCircuitMva = sourceMva
                    )
                } else {
                    it
                }
            }
        }

        showNodeDialog = false
    }

    fun saveConnection() {
        val length =
            (connectionLength.toDoubleOrNull() ?: 10.0)
                .coerceAtLeast(0.0)

        val resistance =
            connectionResistance.toDoubleOrNull() ?: 0.0

        val reactance =
            connectionReactance.toDoubleOrNull() ?: 0.0

        val size =
            connectionCableSize.toDoubleOrNull() ?: 0.0

        val runs =
            (connectionParallelRuns.toIntOrNull() ?: 1)
                .coerceAtLeast(1)

        val capacity =
            connectionCurrentCapacity.toDoubleOrNull() ?: 0.0

        if (editingConnectionId == null) {
            val from = connectionStartId ?: return
            val to = selectedNodeId ?: return

            if (from == to) return

            val exists = connections.any {
                (it.fromNodeId == from && it.toNodeId == to) ||
                    (it.fromNodeId == to && it.toNodeId == from)
            }

            if (!exists) {
                val connection = SldConnection(
                    id = "connection-${System.nanoTime()}",
                    fromNodeId = from,
                    toNodeId = to,
                    lengthMeters = length,
                    resistanceOhmPerKm = resistance,
                    reactanceOhmPerKm = reactance,
                    cableSizeMm2 = size,
                    parallelRuns = runs,
                    currentCapacityA = capacity
                )

                connections = connections + connection
                selectedConnectionId = connection.id
            }
        } else {
            val id = editingConnectionId!!

            connections = connections.map {
                if (it.id == id) {
                    it.copy(
                        lengthMeters = length,
                        resistanceOhmPerKm = resistance,
                        reactanceOhmPerKm = reactance,
                        cableSizeMm2 = size,
                        parallelRuns = runs,
                        currentCapacityA = capacity
                    )
                } else {
                    it
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

        val from = connectionStartId!!

        if (from == selected) {
            connectionStartId = null
            return
        }

        val exists = connections.any {
            (it.fromNodeId == from && it.toNodeId == selected) ||
                (it.fromNodeId == selected && it.toNodeId == from)
        }

        if (!exists) {
            val connection = SldConnection(
                id = "connection-${System.nanoTime()}",
                fromNodeId = from,
                toNodeId = selected,
                lengthMeters = 10.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080
            )

            connections = connections + connection
            selectedConnectionId = connection.id
        }

        connectionStartId = null
    }

    fun deleteSelected() {
        val nodeId = selectedNodeId

        if (nodeId != null && nodeId != "source-1") {
            nodes = nodes.filterNot {
                it.id == nodeId
            }

            connections = connections.filter {
                it.fromNodeId != nodeId &&
                    it.toNodeId != nodeId
            }

            selectedNodeId = null
            selectedConnectionId = null
            connectionStartId = null
            return
        }

        val connectionId = selectedConnectionId

        if (connectionId != null) {
            connections = connections.filterNot {
                it.id == connectionId
            }

            selectedConnectionId = null
        }
    }

    fun calculateShortCircuit() {
        try {
            val study = SldShortCircuitEngine.calculate(
                network = network(),
                voltageFactor = 1.05
            )

            resultTitle =
                if (arabic)
                    "تقرير تيارات القصر"
                else
                    "SHORT CIRCUIT ENGINEERING REPORT"

            resultText =
                formatShortCircuitStudy(
                    study = study,
                    nodes = nodes,
                    arabic = arabic
                )

            showResultDialog = true
        } catch (e: Exception) {
            resultTitle =
                if (arabic) "خطأ في الحساب" else "CALCULATION ERROR"

            resultText =
                e.message ?: "Short-circuit calculation failed."

            showResultDialog = true
        }
    }

    fun generateCompleteSld() {
        val source =
            nodes.firstOrNull {
                it.type == SldNodeType.SOURCE
            }
                ?: SldNode(
                    id = "source-1",
                    name = "MAIN SOURCE",
                    type = SldNodeType.SOURCE,
                    x = 60f,
                    y = 180f,
                    voltage = 400.0,
                    sourceShortCircuitMva = 500.0
                )

        val generatedNodes = mutableListOf<SldNode>()

        generatedNodes += source.copy(
            x = 60f,
            y = 220f
        )

        val breaker1 = SldNode(
            id = "auto-cb-1",
            name = "MAIN ACB",
            type = SldNodeType.BREAKER,
            x = 280f,
            y = 220f,
            voltage = source.voltage
        )

        val transformer = SldNode(
            id = "auto-tr-1",
            name = "TR-01",
            type = SldNodeType.TRANSFORMER,
            x = 500f,
            y = 220f,
            voltage = 400.0,
            ratedKva = 1000.0,
            transformerPercentZ = 6.0
        )

        val bus = SldNode(
            id = "auto-bus-1",
            name = "LV BUS-01",
            type = SldNodeType.BUS,
            x = 720f,
            y = 220f,
            voltage = 400.0
        )

        val breaker2 = SldNode(
            id = "auto-cb-2",
            name = "INCOMER ACB",
            type = SldNodeType.BREAKER,
            x = 940f,
            y = 220f,
            voltage = 400.0
        )

        val panel = SldNode(
            id = "auto-panel-1",
            name = "MDB-01",
            type = SldNodeType.PANEL,
            x = 1160f,
            y = 220f,
            voltage = 400.0,
            ratedKva = 1000.0
        )

        val load1 = SldNode(
            id = "auto-load-1",
            name = "LOAD-01",
            type = SldNodeType.LOAD,
            x = 1380f,
            y = 80f,
            voltage = 400.0,
            loadKw = 100.0,
            powerFactor = 0.90,
            demandFactor = 1.0
        )

        val load2 = SldNode(
            id = "auto-load-2",
            name = "LOAD-02",
            type = SldNodeType.LOAD,
            x = 1380f,
            y = 220f,
            voltage = 400.0,
            loadKw = 150.0,
            powerFactor = 0.90,
            demandFactor = 1.0
        )

        val load3 = SldNode(
            id = "auto-load-3",
            name = "LOAD-03",
            type = SldNodeType.LOAD,
            x = 1380f,
            y = 360f,
            voltage = 400.0,
            loadKw = 200.0,
            powerFactor = 0.90,
            demandFactor = 1.0
        )

        generatedNodes += listOf(
            breaker1,
            transformer,
            bus,
            breaker2,
            panel,
            load1,
            load2,
            load3
        )

        fun c(
            id: String,
            from: String,
            to: String,
            length: Double = 10.0,
            r: Double = 0.125,
            x: Double = 0.080
        ) =
            SldConnection(
                id = id,
                fromNodeId = from,
                toNodeId = to,
                lengthMeters = length,
                resistanceOhmPerKm = r,
                reactanceOhmPerKm = x,
                cableSizeMm2 = 0.0,
                parallelRuns = 1,
                currentCapacityA = 0.0
            )

        val generatedConnections =
            listOf(
                c(
                    "auto-c-1",
                    source.id,
                    breaker1.id
                ),
                c(
                    "auto-c-2",
                    breaker1.id,
                    transformer.id
                ),
                c(
                    "auto-c-3",
                    transformer.id,
                    bus.id
                ),
                c(
                    "auto-c-4",
                    bus.id,
                    breaker2.id
                ),
                c(
                    "auto-c-5",
                    breaker2.id,
                    panel.id
                ),
                c(
                    "auto-c-6",
                    panel.id,
                    load1.id
                ),
                c(
                    "auto-c-7",
                    panel.id,
                    load2.id
                ),
                c(
                    "auto-c-8",
                    panel.id,
                    load3.id
                )
            )

        nodes = generatedNodes
        connections = generatedConnections

        selectedNodeId = null
        selectedConnectionId = null
        connectionStartId = null

        try {
            val study =
                SldShortCircuitEngine.calculate(
                    network = network(),
                    voltageFactor = 1.05
                )

            resultTitle =
                if (arabic)
                    "SLD كامل - نتائج الحساب"
                else
                    "COMPLETE SLD - ENGINEERING RESULTS"

            resultText =
                formatShortCircuitStudy(
                    study = study,
                    nodes = nodes,
                    arabic = arabic
                )

            showResultDialog = true
        } catch (_: Exception) {
            resultTitle =
                if (arabic)
                    "تم إنشاء SLD"
                else
                    "COMPLETE SLD GENERATED"

            resultText =
                if (arabic)
                    "تم إنشاء الشبكة كاملة."
                else
                    "Complete electrical SLD generated successfully."

            showResultDialog = true
        }
    }

    val scrollX = rememberScrollState()
    val scrollY = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (onBack != null) {
                Button(
                    onClick = onBack
                ) {
                    Text(
                        if (arabic) "رجوع" else "Back"
                    )
                }
            }

            ToolButton(
                text = if (arabic) "مصدر" else "Source"
            ) {
                openAdd(SldNodeType.SOURCE)
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
                text = if (arabic) "باسبار" else "Busbar"
            ) {
                openAdd(SldNodeType.BUS)
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
                text =
                    if (connectionStartId == null)
                        if (arabic) "ربط" else "Connect"
                    else
                        if (arabic) "اختر النهاية" else "Select End"
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
                .horizontalScroll(scrollX)
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            ToolButton(
                text =
                    if (arabic)
                        "تيارات القصر"
                    else
                        "Short Circuit"
            ) {
                calculateShortCircuit()
            }

            ToolButton(
                text =
                    if (arabic)
                        "Generate Complete SLD"
                    else
                        "Generate Complete SLD"
            ) {
                generateCompleteSld()
            }

            ToolButton(
                text =
                    if (arabic)
                        "فتح Schedule"
                    else
                        "Panel Schedule"
            ) {
                val panel =
                    selectedNodeId?.let { id ->
                        nodes.firstOrNull {
                            it.id == id &&
                                it.type == SldNodeType.PANEL
                        }
                    }

                if (panel == null) {
                    resultTitle =
                        if (arabic)
                            "Panel Schedule"
                        else
                            "Panel Schedule"

                    resultText =
                        if (arabic)
                            "اختر لوحة PANEL أولاً."
                        else
                            "Select a PANEL first."

                    showResultDialog = true
                } else {
                    resultTitle =
                        "PANEL SCHEDULE - ${panel.name}"

                    resultText =
                        formatPanelSchedule(
                            panel = panel,
                            nodes = nodes,
                            connections = connections,
                            arabic = arabic
                        )

                    showResultDialog = true
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollX)
                .verticalScroll(scrollY)
        ) {

            SldCanvas(
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                onSelectNode = {
                    selectedNodeId = it
                    selectedConnectionId = null
                },
                onSelectConnection = {
                    selectedConnectionId = it
                    selectedNodeId = null
                },
                onMoveNode = { id, dx, dy ->
                    nodes = nodes.map {
                        if (it.id == id) {
                            it.copy(
                                x = max(
                                    0f,
                                    it.x + dx
                                ),
                                y = max(
                                    0f,
                                    it.y + dy
                                )
                            )
                        } else {
                            it
                        }
                    }
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
            onNameChange = {
                nodeName = it
            },
            onVoltageChange = {
                nodeVoltage = it
            },
            onKwChange = {
                nodeKw = it
            },
            onPfChange = {
                nodePf = it
            },
            onDemandChange = {
                nodeDemand = it
            },
            onKvaChange = {
                nodeKva = it
            },
            onTransformerZChange = {
                nodeTransformerZ = it
            },
            onGeneratorXdChange = {
                nodeGeneratorXd = it
            },
            onSourceMvaChange = {
                nodeSourceMva = it
            },
            onSave = {
                saveNode()
            },
            onCancel = {
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
            size = connectionCableSize,
            runs = connectionParallelRuns,
            capacity = connectionCurrentCapacity,
            onLengthChange = {
                connectionLength = it
            },
            onResistanceChange = {
                connectionResistance = it
            },
            onReactanceChange = {
                connectionReactance = it
            },
            onSizeChange = {
                connectionCableSize = it
            },
            onRunsChange = {
                connectionParallelRuns = it
            },
            onCapacityChange = {
                connectionCurrentCapacity = it
            },
            onSave = {
                saveConnection()
            },
            onCancel = {
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
                    resultTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    Text(
                        resultText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
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
                        if (arabic) "إغلاق" else "Close"
                    )
                }
            }
        )
    }
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
            text,
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
    onSelectNode: (String) -> Unit,
    onSelectConnection: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onEditNode: (SldNode) -> Unit,
    onEditConnection: (SldConnection) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    val currentNodes by rememberUpdatedState(nodes)
    val currentOnSelectNode by rememberUpdatedState(onSelectNode)
    val currentOnSelectConnection by rememberUpdatedState(onSelectConnection)
    val currentOnMoveNode by rememberUpdatedState(onMoveNode)
    val currentOnEditNode by rememberUpdatedState(onEditNode)
    val currentOnEditConnection by rememberUpdatedState(onEditConnection)

    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    Canvas(
        modifier = Modifier
            .width(1700.dp)
            .height(650.dp)
            .background(CanvasBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { position ->
                        val node =
                            findNode(
                                position,
                                currentNodes
                            )

                        if (node != null) {
                            currentOnSelectNode(node.id)
                            currentOnEditNode(node)
                            return@detectTapGestures
                        }

                        val connection =
                            findConnection(
                                position,
                                currentNodes,
                                connections
                            )

                        if (connection != null) {
                            currentOnSelectConnection(
                                connection.id
                            )
                            currentOnEditConnection(
                                connection
                            )
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
                        } else {
                            val connection =
                                findConnection(
                                    position,
                                    currentNodes,
                                    connections
                                )

                            if (connection != null) {
                                currentOnSelectConnection(
                                    connection.id
                                )
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        val node =
                            findNode(
                                position,
                                currentNodes
                            )

                        draggedNodeId = node?.id

                        node?.let {
                            currentOnSelectNode(it.id)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val id =
                            draggedNodeId ?: return@detectDragGestures

                        currentOnMoveNode(
                            id,
                            dragAmount.x,
                            dragAmount.y
                        )
                    },
                    onDragEnd = {
                        draggedNodeId = null
                    },
                    onDragCancel = {
                        draggedNodeId = null
                    }
                )
            }
    ) {

        drawGrid()

        connections.forEach { connection ->

            val from =
                nodes.firstOrNull {
                    it.id == connection.fromNodeId
                }

            val to =
                nodes.firstOrNull {
                    it.id == connection.toNodeId
                }

            if (from != null && to != null) {
                drawConnection(
                    connection = connection,
                    from = from,
                    to = to,
                    selected =
                        connection.id ==
                            selectedConnectionId,
                    textMeasurer = textMeasurer
                )
            }
        }

        nodes.forEach { node ->

            drawNode(
                node = node,
                selected =
                    node.id ==
                        selectedNodeId,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawGrid() {
    val grid = 40f

    var x = 0f

    while (x < size.width) {
        drawLine(
            color = Color(0xFF17242C),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )

        x += grid
    }

    var y = 0f

    while (y < size.height) {
        drawLine(
            color = Color(0xFF17242C),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )

        y += grid
    }
}

private fun DrawScope.drawConnection(
    connection: SldConnection,
    from: SldNode,
    to: SldNode,
    selected: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val start =
        Offset(
            from.x + NODE_WIDTH,
            from.y + NODE_HEIGHT / 2f
        )

    val end =
        Offset(
            to.x,
            to.y + NODE_HEIGHT / 2f
        )

    val middleX =
        (start.x + end.x) / 2f

    val path = Path().apply {
        moveTo(start.x, start.y)
        lineTo(middleX, start.y)
        lineTo(middleX, end.y)
        lineTo(end.x, end.y)
    }

    drawPath(
        path = path,
        color =
            if (selected)
                Accent
            else
                CableColor,
        style = Stroke(
            width =
                if (selected)
                    5f
                else
                    3f
        )
    )

    drawLine(
        color = CableColor,
        start = Offset(end.x - 12f, end.y - 7f),
        end = end,
        strokeWidth = 3f
    )

    drawLine(
        color = CableColor,
        start = Offset(end.x - 12f, end.y + 7f),
        end = end,
        strokeWidth = 3f
    )

    val cableLabel =
        buildString {
            if (connection.cableSizeMm2 > 0.0) {
                append(
                    "%.0f mm²".format(
                        connection.cableSizeMm2
                    )
                )
            } else {
                append("Cable")
            }

            append(
                " / ${connection.parallelRuns.coerceAtLeast(1)} run"
            )

            if (connection.lengthMeters > 0) {
                append(
                    " / %.0f m".format(
                        connection.lengthMeters
                    )
                )
            }
        }

    drawTextAt(
        textMeasurer = textMeasurer,
        text = cableLabel,
        position =
            Offset(
                middleX - 45f,
                min(start.y, end.y) - 12f
            ),
        style = TextStyle(
            color = PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

private fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val topLeft =
        Offset(
            node.x,
            node.y
        )

    val nodeColor =
        when (node.type) {
            SldNodeType.SOURCE -> SourceColor
            SldNodeType.TRANSFORMER -> TransformerColor
            SldNodeType.GENERATOR -> GeneratorColor
            SldNodeType.BREAKER -> BreakerColor
            SldNodeType.BUS -> BusColor
            SldNodeType.PANEL -> PanelColor
            SldNodeType.LOAD -> LoadColor
        }

    if (node.type == SldNodeType.BUS) {

        drawRect(
            color = Color(0xFF9C27B0),
            topLeft =
                Offset(
                    node.x,
                    node.y + 42f
                ),
            size =
                Size(
                    NODE_WIDTH,
                    14f
                )
        )

        drawTextAt(
            textMeasurer = textMeasurer,
            text = node.name,
            position =
                Offset(
                    node.x + 8f,
                    node.y + 18f
                ),
            style = TextStyle(
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )

        drawTextAt(
            textMeasurer = textMeasurer,
            text = "%.0f V".format(node.voltage),
            position =
                Offset(
                    node.x + 8f,
                    node.y + 78f
                ),
            style = TextStyle(
                color = SecondaryText,
                fontSize = 11.sp
            )
        )

        if (selected) {
            drawRect(
                color = Accent,
                topLeft = topLeft,
                size =
                    Size(
                        NODE_WIDTH,
                        NODE_HEIGHT
                    ),
                style = Stroke(
                    width = 4f
                )
            )
        }

        return
    }

    drawRoundRect(
        color = nodeColor,
        topLeft = topLeft,
        size =
            Size(
                NODE_WIDTH,
                NODE_HEIGHT
            ),
        cornerRadius =
            CornerRadius(
                12f,
                12f
            )
    )

    if (selected) {
        drawRoundRect(
            color = Accent,
            topLeft = topLeft,
            size =
                Size(
                    NODE_WIDTH,
                    NODE_HEIGHT
                ),
            cornerRadius =
                CornerRadius(
                    12f,
                    12f
                ),
            style = Stroke(
                width = 4f
            )
        )
    }

    when (node.type) {

        SldNodeType.SOURCE -> {
            drawCircle(
                color = PrimaryText,
                radius = 18f,
                center =
                    Offset(
                        node.x + 30f,
                        node.y + 40f
                    ),
                style = Stroke(
                    width = 3f
                )
            )

            drawLine(
                color = PrimaryText,
                start =
                    Offset(
                        node.x + 20f,
                        node.y + 40f
                    ),
                end =
                    Offset(
                        node.x + 40f,
                        node.y + 40f
                    ),
                strokeWidth = 3f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawCircle(
                color = PrimaryText,
                radius = 19f,
                center =
                    Offset(
                        node.x + 30f,
                        node.y + 42f
                    ),
                style = Stroke(
                    width = 3f
                )
            )

            drawCircle(
                color = PrimaryText,
                radius = 19f,
                center =
                    Offset(
                        node.x + 55f,
                        node.y + 42f
                    ),
                style = Stroke(
                    width = 3f
                )
            )

            drawLine(
                color = PrimaryText,
                start =
                    Offset(
                        node.x + 42f,
                        node.y + 20f
                    ),
                end =
                    Offset(
                        node.x + 42f,
                        node.y + 64f
                    ),
                strokeWidth = 2f
            )
        }

        SldNodeType.GENERATOR -> {
            drawCircle(
                color = PrimaryText,
                radius = 23f,
                center =
                    Offset(
                        node.x + 38f,
                        node.y + 42f
                    ),
                style = Stroke(
                    width = 3f
                )
            )

            drawTextAt(
                textMeasurer = textMeasurer,
                text = "G",
                position =
                    Offset(
                        node.x + 31f,
                        node.y + 49f
                    ),
                style = TextStyle(
                    color = PrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        SldNodeType.BREAKER -> {
            drawRect(
                color = PrimaryText,
                topLeft =
                    Offset(
                        node.x + 18f,
                        node.y + 23f
                    ),
                size =
                    Size(
                        42f,
                        38f
                    ),
                style = Stroke(
                    width = 3f
                )
            )

            drawLine(
                color = PrimaryText,
                start =
                    Offset(
                        node.x + 25f,
                        node.y + 54f
                    ),
                end =
                    Offset(
                        node.x + 54f,
                        node.y + 30f
                    ),
                strokeWidth = 3f
            )
        }

        SldNodeType.PANEL -> {
            drawRect(
                color = PrimaryText,
                topLeft =
                    Offset(
                        node.x + 18f,
                        node.y + 20f
                    ),
                size =
                    Size(
                        46f,
                        45f
                    ),
                style = Stroke(
                    width = 3f
                )
            )

            drawLine(
                color = PrimaryText,
                start =
                    Offset(
                        node.x + 27f,
                        node.y + 30f
                    ),
                end =
                    Offset(
                        node.x + 55f,
                        node.y + 30f
                    ),
                strokeWidth = 3f
            )

            drawLine(
                color = PrimaryText,
                start =
                    Offset(
                        node.x + 27f,
                        node.y + 43f
                    ),
                end =
                    Offset(
                        node.x + 55f,
                        node.y + 43f
                    ),
                strokeWidth = 3f
            )

            drawLine(
                color = PrimaryText,
                start =
                    Offset(
                        node.x + 27f,
                        node.y + 56f
                    ),
                end =
                    Offset(
                        node.x + 55f,
                        node.y + 56f
                    ),
                strokeWidth = 3f
            )
        }

        SldNodeType.LOAD -> {
            drawCircle(
                color = PrimaryText,
                radius = 22f,
                center =
                    Offset(
                        node.x + 38f,
                        node.y + 42f
                    ),
                style = Stroke(
                    width = 3f
                )
            )

            drawLine(
                color = PrimaryText,
                start =
                    Offset(
                        node.x + 28f,
                        node.y + 43f
                    ),
                end =
                    Offset(
                        node.x + 37f,
                        node.y + 30f
                    ),
                strokeWidth = 3f
            )

            drawLine(
                color = PrimaryText,
                start =
                    Offset(
                        node.x + 37f,
                        node.y + 30f
                    ),
                end =
                    Offset(
                        node.x + 48f,
                        node.y + 53f
                    ),
                strokeWidth = 3f
            )
        }

        SldNodeType.BUS -> Unit

        else -> Unit
    }

    drawTextAt(
        textMeasurer = textMeasurer,
        text = node.name,
        position =
            Offset(
                node.x + 78f,
                node.y + 25f
            ),
        style = TextStyle(
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    )

    drawTextAt(
        textMeasurer = textMeasurer,
        text = nodeTypeLabel(node.type),
        position =
            Offset(
                node.x + 78f,
                node.y + 47f
            ),
        style = TextStyle(
            color = SecondaryText,
            fontSize = 10.sp
        )
    )

    val valueText =
        when (node.type) {
            SldNodeType.SOURCE ->
                "%.0f MVA / %.0f V".format(
                    node.sourceShortCircuitMva,
                    node.voltage
                )

            SldNodeType.TRANSFORMER ->
                "%.0f kVA / Z %.1f%%".format(
                    node.ratedKva,
                    node.transformerPercentZ
                )

            SldNodeType.GENERATOR ->
                "%.0f kVA / Xd'' %.1f%%".format(
                    node.ratedKva,
                    node.generatorXdSubtransient
                )

            SldNodeType.LOAD ->
                "%.1f kW / %.0f V".format(
                    node.loadKw,
                    node.voltage
                )

            else ->
                "%.0f V".format(
                    node.voltage
                )
        }

    drawTextAt(
        textMeasurer = textMeasurer,
        text = valueText,
        position =
            Offset(
                node.x + 78f,
                node.y + 72f
            ),
        style = TextStyle(
            color = PrimaryText,
            fontSize = 10.sp
        )
    )

    val current =
        calculateNodeCurrent(node)

    if (current > 0.0) {
        drawTextAt(
            textMeasurer = textMeasurer,
            text =
                "I = %.1f A".format(
                    current
                ),
            position =
                Offset(
                    node.x + 78f,
                    node.y + 92f
                ),
            style = TextStyle(
                color = Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun DrawScope.drawTextAt(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    position: Offset,
    style: TextStyle
) {
    val layout =
        textMeasurer.measure(
            text = text,
            style = style
        )

    drawText(
        textLayoutResult = layout,
        topLeft = position
    )
}

private fun calculateNodeCurrent(
    node: SldNode
): Double {
    if (node.type != SldNodeType.LOAD) {
        return 0.0
    }

    if (node.voltage <= 0.0 ||
        node.powerFactor <= 0.0 ||
        node.loadKw <= 0.0
    ) {
        return 0.0
    }

    return node.loadKw * 1000.0 /
        (
            kotlin.math.sqrt(3.0) *
                node.voltage *
                node.powerFactor
            )
}

private fun nodeTypeLabel(
    type: SldNodeType
): String =
    when (type) {
        SldNodeType.SOURCE -> "SOURCE"
        SldNodeType.TRANSFORMER -> "TRANSFORMER"
        SldNodeType.GENERATOR -> "GENERATOR"
        SldNodeType.BUS -> "BUSBAR"
        SldNodeType.BREAKER -> "BREAKER"
        SldNodeType.PANEL -> "PANEL"
        SldNodeType.LOAD -> "LOAD"
    }

private fun findNode(
    position: Offset,
    nodes: List<SldNode>
): SldNode? =
    nodes.asReversed().firstOrNull { node ->
        position.x >=
            node.x - NODE_TOUCH_PADDING &&
            position.x <=
            node.x + NODE_WIDTH +
            NODE_TOUCH_PADDING &&
            position.y >=
            node.y - NODE_TOUCH_PADDING &&
            position.y <=
            node.y + NODE_HEIGHT +
            NODE_TOUCH_PADDING
    }

private fun findConnection(
    position: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {

    return connections.asReversed().firstOrNull { connection ->

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
                    from.x + NODE_WIDTH,
                    from.y + NODE_HEIGHT / 2f
                )

            val end =
                Offset(
                    to.x,
                    to.y + NODE_HEIGHT / 2f
                )

            distanceToOrthogonalPath(
                position,
                start,
                end
            ) <= 25f
        }
    }
}

private fun distanceToOrthogonalPath(
    p: Offset,
    start: Offset,
    end: Offset
): Float {

    val middleX =
        (start.x + end.x) / 2f

    val d1 =
        distanceToSegment(
            p,
            start,
            Offset(
                middleX,
                start.y
            )
        )

    val d2 =
        distanceToSegment(
            p,
            Offset(
                middleX,
                start.y
            ),
            Offset(
                middleX,
                end.y
            )
        )

    val d3 =
        distanceToSegment(
            p,
            Offset(
                middleX,
                end.y
            ),
            end
        )

    return min(
        d1,
        min(d2, d3)
    )
}

private fun distanceToSegment(
    p: Offset,
    a: Offset,
    b: Offset
): Float {

    val dx =
        b.x - a.x

    val dy =
        b.y - a.y

    if (dx == 0f && dy == 0f) {
        return hypot(
            p.x - a.x,
            p.y - a.y
        )
    }

    val t =
        (
            (p.x - a.x) * dx +
                (p.y - a.y) * dy
            ) /
            (dx * dx + dy * dy)

    val clamped =
        t.coerceIn(
            0f,
            1f
        )

    val projection =
        Offset(
            a.x + clamped * dx,
            a.y + clamped * dy
        )

    return hypot(
        p.x - projection.x,
        p.y - projection.y
    )
}

private fun formatShortCircuitStudy(
    study: SldShortCircuitStudy,
    nodes: List<SldNode>,
    arabic: Boolean
): String {

    val sb = StringBuilder()

    sb.appendLine(
        if (arabic)
            "تقرير هندسي منظم لدراسة القصر"
        else
            "SHORT-CIRCUIT ENGINEERING REPORT"
    )

    sb.appendLine(
        "=================================================="
    )

    sb.appendLine()

    sb.appendLine(
        if (arabic)
            "أقصى تيار قصر Ik'' : %.3f kA"
                .format(study.maximumFaultCurrentKa)
        else
            "Maximum Ik''        : %.3f kA"
                .format(study.maximumFaultCurrentKa)
    )

    sb.appendLine(
        if (arabic)
            "أقصى تيار Peak Ip : %.3f kA"
                .format(study.maximumPeakCurrentKa)
        else
            "Maximum Peak Ip     : %.3f kA"
                .format(study.maximumPeakCurrentKa)
    )

    sb.appendLine(
        if (arabic)
            "أقصى Fault MVA : %.3f MVA"
                .format(study.maximumFaultMva)
        else
            "Maximum Fault MVA   : %.3f MVA"
                .format(study.maximumFaultMva)
    )

    sb.appendLine()

    study.results.values.forEachIndexed { index, result ->

        val node =
            nodes.firstOrNull {
                it.id == result.nodeId
            }

        val requiredBreaker =
            selectBreakerRating(
                result.initialSymmetricalCurrentKa
            )

        sb.appendLine(
            "${index + 1}. ${result.nodeName}"
        )

        sb.appendLine(
            "--------------------------------------------------"
        )

        sb.appendLine(
            "Voltage             : %.1f V"
                .format(result.voltageV)
        )

        sb.appendLine(
            "Ik'' Initial         : %.3f kA"
                .format(
                    result.initialSymmetricalCurrentKa
                )
        )

        sb.appendLine(
            "Ip Peak              : %.3f kA"
                .format(
                    result.peakCurrentKa
                )
        )

        sb.appendLine(
            "Ith Thermal          : %.3f kA"
                .format(
                    result.thermalCurrentKa
                )
        )

        sb.appendLine(
            "Fault MVA            : %.3f MVA"
                .format(
                    result.shortCircuitMva
                )
        )

        sb.appendLine(
            "X/R                  : %.3f"
                .format(
                    result.xrRatio
                )
        )

        sb.appendLine(
            "R                    : %.6f ohm"
                .format(
                    result.resistanceOhm
                )
        )

        sb.appendLine(
            "X                    : %.6f ohm"
                .format(
                    result.reactanceOhm
                )
        )

        sb.appendLine(
            "Required breaker Icu : $requiredBreaker kA"
        )

        sb.appendLine(
            "Recommended Ics      : >= %.1f kA"
                .format(
                    requiredBreaker * 0.75
                )
        )

        if (node != null) {

            if (node.type ==
                SldNodeType.TRANSFORMER
            ) {
                sb.appendLine(
                    "Transformer          : %.0f kVA"
                        .format(
                            node.ratedKva
                        )
                )

                sb.appendLine(
                    "Transformer %Z       : %.2f %%"
                        .format(
                            node.transformerPercentZ
                        )
                )
            }

            if (node.type ==
                SldNodeType.SOURCE
            ) {
                sb.appendLine(
                    "Source Fault Level   : %.3f MVA"
                        .format(
                            node.sourceShortCircuitMva
                        )
                )
            }
        }

        sb.appendLine()
    }

    return sb.toString()
}

private fun selectBreakerRating(
    faultCurrentKa: Double
): Int {

    val standard =
        listOf(
            10,
            16,
            25,
            36,
            50,
            65,
            80,
            100,
            120,
            150,
            200
        )

    return standard.firstOrNull {
        it.toDouble() >= faultCurrentKa
    } ?: 200
}

private fun formatPanelSchedule(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    arabic: Boolean
): String {

    val sb = StringBuilder()

    sb.appendLine(
        if (arabic)
            "PANEL SCHEDULE"
        else
            "PANEL SCHEDULE"
    )

    sb.appendLine(
        "============================================================"
    )

    sb.appendLine(
        "Panel : ${panel.name}"
    )

    sb.appendLine(
        "Voltage : %.0f V"
            .format(panel.voltage)
    )

    sb.appendLine()

    sb.appendLine(
        "Circuit | Load | kW | PF | Demand | Current | Cable | Runs"
    )

    sb.appendLine(
        "------------------------------------------------------------"
    )

    var circuit = 1

    connections
        .filter {
            it.fromNodeId == panel.id
        }
        .forEach { connection ->

            val load =
                nodes.firstOrNull {
                    it.id ==
                        connection.toNodeId
                }

            if (load != null) {

                val current =
                    calculateNodeCurrent(
                        load
                    )

                val cable =
                    if (connection.cableSizeMm2 > 0.0)
                        "%.0f mm²"
                            .format(
                                connection.cableSizeMm2
                            )
                    else
                        "AUTO"

                sb.appendLine(
                    "%-7d | %-10s | %5.1f | %.2f | %.2f | %7.1f A | %-8s | %d"
                        .format(
                            circuit,
                            load.name,
                            load.loadKw,
                            load.powerFactor,
                            load.demandFactor,
                            current,
                            cable,
                            connection.parallelRuns
                        )
                )

                circuit++
            }
        }

    val loads =
        nodes.filter {
            it.type == SldNodeType.LOAD
        }

    val connectedKw =
        loads.sumOf {
            it.loadKw
        }

    val demandKw =
        loads.sumOf {
            it.loadKw *
                it.demandFactor
        }

    sb.appendLine()
    sb.appendLine(
        "Connected Load : %.1f kW"
            .format(connectedKw)
    )

    sb.appendLine(
        "Demand Load    : %.1f kW"
            .format(demandKw)
    )

    sb.appendLine(
        "Panel Rating   : %.1f kVA"
            .format(panel.ratedKva)
    )

    return sb.toString()
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
    onNameChange: (String) -> Unit,
    onVoltageChange: (String) -> Unit,
    onKwChange: (String) -> Unit,
    onPfChange: (String) -> Unit,
    onDemandChange: (String) -> Unit,
    onKvaChange: (String) -> Unit,
    onTransformerZChange: (String) -> Unit,
    onGeneratorXdChange: (String) -> Unit,
    onSourceMvaChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onCancel,

        title = {
            Text(
                when (type) {
                    SldNodeType.SOURCE ->
                        if (arabic) "مصدر التغذية" else "SOURCE"

                    SldNodeType.TRANSFORMER ->
                        if (arabic) "المحول" else "TRANSFORMER"

                    SldNodeType.GENERATOR ->
                        if (arabic) "المولد" else "GENERATOR"

                    SldNodeType.BUS ->
                        if (arabic) "الباسبار" else "BUSBAR"

                    SldNodeType.BREAKER ->
                        if (arabic) "القاطع" else "BREAKER"

                    SldNodeType.PANEL ->
                        if (arabic) "اللوحة" else "PANEL"

                    SldNodeType.LOAD ->
                        if (arabic) "الحمل" else "LOAD"
                }
            )
        },

        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (arabic)
                                "الاسم"
                            else
                                "Name"
                        )
                    }
                )

                OutlinedTextField(
                    value = voltage,
                    onValueChange = onVoltageChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (arabic)
                                "الجهد V"
                            else
                                "Voltage V"
                        )
                    }
                )

                when (type) {

                    SldNodeType.SOURCE -> {

                        OutlinedTextField(
                            value = sourceMva,
                            onValueChange =
                                onSourceMvaChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "قدرة القصر MVA"
                                    else
                                        "Source Short-Circuit MVA"
                                )
                            }
                        )
                    }

                    SldNodeType.TRANSFORMER -> {

                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "قدرة المحول kVA"
                                    else
                                        "Transformer Rating kVA"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = transformerZ,
                            onValueChange =
                                onTransformerZChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "%Z المحول"
                                    else
                                        "Transformer %Z"
                                )
                            }
                        )
                    }

                    SldNodeType.GENERATOR -> {

                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "قدرة المولد kVA"
                                    else
                                        "Generator Rating kVA"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = generatorXd,
                            onValueChange =
                                onGeneratorXdChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "Xd'' %"
                                    else
                                        "Xd'' %"
                                )
                            }
                        )
                    }

                    SldNodeType.PANEL -> {

                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "تصنيف اللوحة kVA"
                                    else
                                        "Panel Rating kVA"
                                )
                            }
                        )
                    }

                    SldNodeType.LOAD -> {

                        OutlinedTextField(
                            value = kw,
                            onValueChange = onKwChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "القدرة kW"
                                    else
                                        "Load kW"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = pf,
                            onValueChange = onPfChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "معامل القدرة PF"
                                    else
                                        "Power Factor PF"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = demand,
                            onValueChange =
                                onDemandChange,
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    if (arabic)
                                        "معامل الطلب"
                                    else
                                        "Demand Factor"
                                )
                            }
                        )
                    }

                    else -> Unit
                }
            }
        },

        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    if (editing)
                        if (arabic) "تحديث" else "Update"
                    else
                        if (arabic) "إضافة" else "Add"
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    if (arabic) "إلغاء" else "Cancel"
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
    size: String,
    runs: String,
    capacity: String,
    onLengthChange: (String) -> Unit,
    onResistanceChange: (String) -> Unit,
    onReactanceChange: (String) -> Unit,
    onSizeChange: (String) -> Unit,
    onRunsChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onCancel,

        title = {
            Text(
                if (arabic)
                    "بيانات الكابل والربط"
                else
                    "CABLE / CONNECTION DATA"
            )
        },

        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = length,
                    onValueChange = onLengthChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (arabic)
                                "الطول m"
                            else
                                "Length m"
                        )
                    }
                )

                OutlinedTextField(
                    value = resistance,
                    onValueChange =
                        onResistanceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (arabic)
                                "R Ω/km"
                            else
                                "Resistance Ω/km"
                        )
                    }
                )

                OutlinedTextField(
                    value = reactance,
                    onValueChange =
                        onReactanceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (arabic)
                                "X Ω/km"
                            else
                                "Reactance Ω/km"
                        )
                    }
                )

                OutlinedTextField(
                    value = size,
                    onValueChange = onSizeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (arabic)
                                "المقطع mm²"
                            else
                                "Cable Size mm²"
                        )
                    }
                )

                OutlinedTextField(
                    value = runs,
                    onValueChange = onRunsChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (arabic)
                                "عدد المسارات"
                            else
                                "Parallel Runs"
                        )
                    }
                )

                OutlinedTextField(
                    value = capacity,
                    onValueChange = onCapacityChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (arabic)
                                "سعة التيار A"
                            else
                                "Current Capacity A"
                        )
                    }
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
                onClick = onCancel
            ) {
                Text(
                    if (arabic) "إلغاء" else "Cancel"
                )
            }
        }
    )
}
