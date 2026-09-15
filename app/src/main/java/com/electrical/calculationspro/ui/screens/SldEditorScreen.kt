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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.sqrt

private val Background = Color(0xFF070C10)
private val CanvasBackground = Color(0xFF09131A)
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
private const val TOUCH_PADDING = 35f

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
                    y = 240f,
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

    var pendingType by remember {
        mutableStateOf(SldNodeType.LOAD)
    }

    var nodeName by remember { mutableStateOf("") }
    var nodeVoltage by remember { mutableStateOf("400") }
    var nodeKw by remember { mutableStateOf("50") }
    var nodePf by remember { mutableStateOf("0.90") }
    var nodeDemand by remember { mutableStateOf("1.0") }
    var nodeKva by remember { mutableStateOf("1000") }
    var nodeZ by remember { mutableStateOf("6") }
    var nodeXd by remember { mutableStateOf("15") }
    var nodeSourceMva by remember { mutableStateOf("500") }

    var connectionLength by remember { mutableStateOf("10") }
    var connectionR by remember { mutableStateOf("0.125") }
    var connectionX by remember { mutableStateOf("0.080") }
    var connectionSize by remember { mutableStateOf("0") }
    var connectionRuns by remember { mutableStateOf("1") }
    var connectionCapacity by remember { mutableStateOf("0") }

    fun network(): SldNetwork =
        SldNetwork(
            nodes = nodes,
            connections = connections
        )

    fun typeName(type: SldNodeType): String =
        when (type) {
            SldNodeType.SOURCE -> if (arabic) "مصدر" else "Source"
            SldNodeType.TRANSFORMER -> if (arabic) "محول" else "Transformer"
            SldNodeType.GENERATOR -> if (arabic) "مولد" else "Generator"
            SldNodeType.BUS -> if (arabic) "باسبار" else "Busbar"
            SldNodeType.BREAKER -> if (arabic) "قاطع" else "Breaker"
            SldNodeType.PANEL -> if (arabic) "لوحة" else "Panel"
            SldNodeType.LOAD -> if (arabic) "حمل" else "Load"
        }

    fun prefix(type: SldNodeType): String =
        when (type) {
            SldNodeType.SOURCE -> "SOURCE"
            SldNodeType.TRANSFORMER -> "TR"
            SldNodeType.GENERATOR -> "GEN"
            SldNodeType.BUS -> "BUS"
            SldNodeType.BREAKER -> "CB"
            SldNodeType.PANEL -> "PANEL"
            SldNodeType.LOAD -> "LOAD"
        }

    fun nextName(type: SldNodeType): String {
        val n = nodes.count { it.type == type } + 1
        return "${prefix(type)}-$n"
    }

    fun openAdd(type: SldNodeType) {
        editingNodeId = null
        pendingType = type
        nodeName = nextName(type)

        nodeVoltage = "400"
        nodeKw = if (type == SldNodeType.LOAD) "50" else "0"
        nodePf = "0.90"
        nodeDemand = "1.0"
        nodeKva = if (
            type == SldNodeType.TRANSFORMER ||
            type == SldNodeType.GENERATOR ||
            type == SldNodeType.PANEL
        ) "1000" else "0"
        nodeZ = "6"
        nodeXd = "15"
        nodeSourceMva = "500"

        showNodeDialog = true
    }

    fun openEdit(node: SldNode) {
        editingNodeId = node.id
        pendingType = node.type

        nodeName = node.name
        nodeVoltage = node.voltage.toString()
        nodeKw = node.loadKw.toString()
        nodePf = node.powerFactor.toString()
        nodeDemand = node.demandFactor.toString()
        nodeKva = node.ratedKva.toString()
        nodeZ = node.transformerPercentZ.toString()
        nodeXd = node.generatorXdSubtransient.toString()
        nodeSourceMva = node.sourceShortCircuitMva.toString()

        showNodeDialog = true
    }

    fun saveNode() {
        val voltage = nodeVoltage.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 400.0
        val kw = nodeKw.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val pf = (nodePf.toDoubleOrNull() ?: 0.9).coerceIn(0.01, 1.0)
        val demand = (nodeDemand.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0)
        val kva = nodeKva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val z = nodeZ.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val xd = nodeXd.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val sourceMva = nodeSourceMva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val id = editingNodeId

        if (id == null) {
            val index = nodes.size

            val node = SldNode(
                id = "node-${System.nanoTime()}",
                name = nodeName.ifBlank { nextName(pendingType) },
                type = pendingType,
                x = 100f + index * 220f,
                y = 240f,
                voltage = voltage,
                loadKw = kw,
                powerFactor = pf,
                demandFactor = demand,
                ratedKva = kva,
                transformerPercentZ = z,
                generatorXdSubtransient = xd,
                sourceShortCircuitMva = sourceMva
            )

            nodes = nodes + node
            selectedNodeId = node.id
            selectedConnectionId = null
        } else {
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

    fun openConnectionEditor(connection: SldConnection) {
        editingConnectionId = connection.id
        connectionLength = connection.lengthMeters.toString()
        connectionR = connection.resistanceOhmPerKm.toString()
        connectionX = connection.reactanceOhmPerKm.toString()
        connectionSize = connection.cableSizeMm2.toString()
        connectionRuns = connection.parallelRuns.toString()
        connectionCapacity = connection.currentCapacityA.toString()
        showConnectionDialog = true
    }

    fun saveConnection() {
        val length = connectionLength.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 10.0
        val r = connectionR.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val x = connectionX.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val size = connectionSize.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val runs = connectionRuns.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val capacity = connectionCapacity.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val id = editingConnectionId

        if (id != null) {
            connections = connections.map {
                if (it.id == id) {
                    it.copy(
                        lengthMeters = length,
                        resistanceOhmPerKm = r,
                        reactanceOhmPerKm = x,
                        cableSizeMm2 = size,
                        parallelRuns = runs,
                        currentCapacityA = capacity
                    )
                } else {
                    it
                }
            }
        }

        showConnectionDialog = false
        editingConnectionId = null
    }

    fun connectNodes() {
        val selected = selectedNodeId ?: return

        if (connectionStartId == null) {
            connectionStartId = selected
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
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 0.0,
                parallelRuns = 1,
                currentCapacityA = 0.0
            )

            connections = connections + connection
            selectedConnectionId = connection.id
        }

        connectionStartId = null
    }

    fun deleteSelected() {
        val nodeId = selectedNodeId

        if (nodeId != null && nodeId != "source-1") {
            nodes = nodes.filterNot { it.id == nodeId }

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

    fun showShortCircuit() {
        try {
            val study = SldShortCircuitEngine.calculate(
                network = network(),
                voltageFactor = 1.05
            )

            resultTitle =
                if (arabic) {
                    "تقرير تيارات القصر"
                } else {
                    "SHORT CIRCUIT ENGINEERING REPORT"
                }

            resultText = buildShortCircuitReport(
                study = study,
                nodes = nodes,
                arabic = arabic
            )

            showResultDialog = true
        } catch (e: Exception) {
            resultTitle = if (arabic) "خطأ" else "CALCULATION ERROR"
            resultText = e.message ?: "Short circuit calculation failed."
            showResultDialog = true
        }
    }

    fun showNetworkReport() {
        resultTitle =
            if (arabic) "تقرير الشبكة الكهربائية"
            else "ELECTRICAL NETWORK REPORT"

        resultText = buildNetworkReport(
            nodes = nodes,
            connections = connections,
            arabic = arabic
        )

        showResultDialog = true
    }

    fun showPanelSchedule() {
        val panel = nodes.firstOrNull {
            it.id == selectedNodeId &&
                it.type == SldNodeType.PANEL
        }

        if (panel == null) {
            resultTitle =
                if (arabic) "Panel Schedule"
                else "PANEL SCHEDULE"

            resultText =
                if (arabic) {
                    "اختر لوحة PANEL أولاً ثم اضغط Panel Schedule."
                } else {
                    "Select a PANEL first, then press Panel Schedule."
                }

            showResultDialog = true
            return
        }

        resultTitle = "${panel.name} - PANEL SCHEDULE"

        resultText = buildPanelSchedule(
            panel = panel,
            nodes = nodes,
            connections = connections,
            arabic = arabic
        )

        showResultDialog = true
    }

    fun generateCompleteSld() {
        val source = nodes.firstOrNull {
            it.type == SldNodeType.SOURCE
        } ?: SldNode(
            id = "source-1",
            name = "MAIN SOURCE",
            type = SldNodeType.SOURCE,
            x = 80f,
            y = 260f,
            voltage = 400.0,
            sourceShortCircuitMva = 500.0
        )

        val v = source.voltage.coerceAtLeast(1.0)
        val sourceMva = source.sourceShortCircuitMva.coerceAtLeast(1.0)

        val sourceNode = source.copy(
            id = "source-1",
            name = source.name.ifBlank { "MAIN SOURCE" },
            x = 80f,
            y = 300f,
            voltage = v,
            sourceShortCircuitMva = sourceMva
        )

        val cb1 = SldNode(
            id = "auto-cb-1",
            name = "MAIN ACB",
            type = SldNodeType.BREAKER,
            x = 300f,
            y = 300f,
            voltage = v
        )

        val transformer = SldNode(
            id = "auto-tr-1",
            name = "TR-01",
            type = SldNodeType.TRANSFORMER,
            x = 520f,
            y = 300f,
            voltage = 400.0,
            ratedKva = 1000.0,
            transformerPercentZ = 6.0
        )

        val bus = SldNode(
            id = "auto-bus-1",
            name = "LV MAIN BUS",
            type = SldNodeType.BUS,
            x = 740f,
            y = 300f,
            voltage = 400.0
        )

        val cb2 = SldNode(
            id = "auto-cb-2",
            name = "INCOMER ACB",
            type = SldNodeType.BREAKER,
            x = 960f,
            y = 300f,
            voltage = 400.0
        )

        val panel = SldNode(
            id = "auto-panel-1",
            name = "MDB-01",
            type = SldNodeType.PANEL,
            x = 1180f,
            y = 300f,
            voltage = 400.0,
            ratedKva = 1000.0
        )

        val load1 = SldNode(
            id = "auto-load-1",
            name = "LOAD-01",
            type = SldNodeType.LOAD,
            x = 1410f,
            y = 170f,
            voltage = 400.0,
            loadKw = 50.0,
            powerFactor = 0.90,
            demandFactor = 1.0
        )

        val load2 = SldNode(
            id = "auto-load-2",
            name = "LOAD-02",
            type = SldNodeType.LOAD,
            x = 1410f,
            y = 300f,
            voltage = 400.0,
            loadKw = 75.0,
            powerFactor = 0.90,
            demandFactor = 1.0
        )

        val load3 = SldNode(
            id = "auto-load-3",
            name = "LOAD-03",
            type = SldNodeType.LOAD,
            x = 1410f,
            y = 430f,
            voltage = 400.0,
            loadKw = 100.0,
            powerFactor = 0.90,
            demandFactor = 1.0
        )

        val generatedNodes = listOf(
            sourceNode,
            cb1,
            transformer,
            bus,
            cb2,
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
            size: Double = 0.0
        ) = SldConnection(
            id = id,
            fromNodeId = from,
            toNodeId = to,
            lengthMeters = length,
            resistanceOhmPerKm = 0.125,
            reactanceOhmPerKm = 0.080,
            cableSizeMm2 = size,
            parallelRuns = 1,
            currentCapacityA = 0.0
        )

        val generatedConnections = listOf(
            c("auto-c-1", "source-1", "auto-cb-1", 5.0),
            c("auto-c-2", "auto-cb-1", "auto-tr-1", 5.0),
            c("auto-c-3", "auto-tr-1", "auto-bus-1", 5.0),
            c("auto-c-4", "auto-bus-1", "auto-cb-2", 5.0),
            c("auto-c-5", "auto-cb-2", "auto-panel-1", 10.0),
            c("auto-c-6", "auto-panel-1", "auto-load-1", 20.0),
            c("auto-c-7", "auto-panel-1", "auto-load-2", 20.0),
            c("auto-c-8", "auto-panel-1", "auto-load-3", 20.0)
        )

        nodes = generatedNodes
        connections = generatedConnections
        selectedNodeId = "auto-panel-1"
        selectedConnectionId = null
        connectionStartId = null

        showShortCircuit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (onBack != null) {
                Button(onClick = onBack) {
                    Text(if (arabic) "رجوع" else "Back")
                }
            }

            Button(onClick = { openAdd(SldNodeType.SOURCE) }) {
                Text(if (arabic) "مصدر" else "Source")
            }

            Button(onClick = { openAdd(SldNodeType.TRANSFORMER) }) {
                Text(if (arabic) "محول" else "Transformer")
            }

            Button(onClick = { openAdd(SldNodeType.GENERATOR) }) {
                Text(if (arabic) "مولد" else "Generator")
            }

            Button(onClick = { openAdd(SldNodeType.BUS) }) {
                Text(if (arabic) "باسبار" else "Busbar")
            }

            Button(onClick = { openAdd(SldNodeType.BREAKER) }) {
                Text(if (arabic) "قاطع" else "Breaker")
            }

            Button(onClick = { openAdd(SldNodeType.PANEL) }) {
                Text(if (arabic) "لوحة" else "Panel")
            }

            Button(onClick = { openAdd(SldNodeType.LOAD) }) {
                Text(if (arabic) "حمل" else "Load")
            }

            Button(onClick = { connectNodes() }) {
                Text(
                    if (connectionStartId == null) {
                        if (arabic) "بدء ربط" else "Connect"
                    } else {
                        if (arabic) "اختيار النهاية" else "Select End"
                    }
                )
            }

            Button(onClick = { deleteSelected() }) {
                Text(if (arabic) "حذف" else "Delete")
            }

            Button(onClick = { showShortCircuit() }) {
                Text(if (arabic) "تيارات القصر" else "Short Circuit")
            }

            Button(onClick = { showNetworkReport() }) {
                Text(if (arabic) "تقرير الشبكة" else "Network")
            }

            Button(onClick = { showPanelSchedule() }) {
                Text(if (arabic) "جدول اللوحة" else "Panel Schedule")
            }

            Button(onClick = { generateCompleteSld() }) {
                Text(
                    if (arabic) {
                        "إنشاء SLD كامل"
                    } else {
                        "Generate Complete SLD"
                    }
                )
            }
        }

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
            onSelectConnection = {
                selectedConnectionId = it
                selectedNodeId = null
            },
            onMoveNode = { id, dx, dy ->
                nodes = nodes.map { node ->
                    if (node.id == id) {
                        node.copy(
                            x = (node.x + dx).coerceAtLeast(10f),
                            y = (node.y + dy).coerceAtLeast(10f)
                        )
                    } else {
                        node
                    }
                }
            },
            onEditNode = { id ->
                nodes.firstOrNull { it.id == id }?.let {
                    openEdit(it)
                }
            },
            onEditConnection = { id ->
                connections.firstOrNull { it.id == id }?.let {
                    openConnectionEditor(it)
                }
            }
        )
    }

    if (showNodeDialog) {
        NodeDialog(
            arabic = arabic,
            type = pendingType,
            editing = editingNodeId != null,
            name = nodeName,
            voltage = nodeVoltage,
            kw = nodeKw,
            pf = nodePf,
            demand = nodeDemand,
            kva = nodeKva,
            z = nodeZ,
            xd = nodeXd,
            sourceMva = nodeSourceMva,
            onName = { nodeName = it },
            onVoltage = { nodeVoltage = it },
            onKw = { nodeKw = it },
            onPf = { nodePf = it },
            onDemand = { nodeDemand = it },
            onKva = { nodeKva = it },
            onZ = { nodeZ = it },
            onXd = { nodeXd = it },
            onSourceMva = { nodeSourceMva = it },
            onSave = { saveNode() },
            onCancel = { showNodeDialog = false }
        )
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            arabic = arabic,
            length = connectionLength,
            r = connectionR,
            x = connectionX,
            size = connectionSize,
            runs = connectionRuns,
            capacity = connectionCapacity,
            onLength = { connectionLength = it },
            onR = { connectionR = it },
            onX = { connectionX = it },
            onSize = { connectionSize = it },
            onRuns = { connectionRuns = it },
            onCapacity = { connectionCapacity = it },
            onSave = { saveConnection() },
            onCancel = { showConnectionDialog = false }
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
                        .height(520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = resultText,
                        color = PrimaryText,
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
                    Text(if (arabic) "إغلاق" else "Close")
                }
            }
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
    onSelectConnection: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onEditNode: (String) -> Unit,
    onEditConnection: (String) -> Unit
) {
    val currentNodes by rememberUpdatedState(nodes)
    val currentSelectNode by rememberUpdatedState(onSelectNode)
    val currentSelectConnection by rememberUpdatedState(onSelectConnection)
    val currentMoveNode by rememberUpdatedState(onMoveNode)
    val currentEditNode by rememberUpdatedState(onEditNode)
    val currentEditConnection by rememberUpdatedState(onEditConnection)

    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .background(CanvasBackground)
    ) {
        Canvas(
            modifier = Modifier
                .width(2200.dp)
                .height(1000.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { position ->
                            val node = findNode(
                                position,
                                currentNodes
                            )

                            if (node != null) {
                                currentSelectNode(node.id)
                                return@detectTapGestures
                            }

                            val connection = findConnection(
                                position,
                                currentNodes,
                                connections
                            )

                            if (connection != null) {
                                currentSelectConnection(connection.id)
                            }
                        },
                        onDoubleTap = { position ->
                            val node = findNode(
                                position,
                                currentNodes
                            )

                            if (node != null) {
                                currentEditNode(node.id)
                                return@detectTapGestures
                            }

                            val connection = findConnection(
                                position,
                                currentNodes,
                                connections
                            )

                            if (connection != null) {
                                currentEditConnection(connection.id)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            val node = findNode(
                                position,
                                currentNodes
                            )

                            draggedNodeId = node?.id

                            if (node != null) {
                                currentSelectNode(node.id)
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()

                            val id = draggedNodeId

                            if (id != null) {
                                currentMoveNode(
                                    id,
                                    amount.x,
                                    amount.y
                                )
                            }
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
            drawConnections(
                nodes = currentNodes,
                connections = connections,
                selectedConnectionId = selectedConnectionId
            )

            currentNodes.forEach { node ->
                drawNode(
                    node = node,
                    selected = node.id == selectedNodeId,
                    connectionStart = node.id == connectionStartId
                )
            }
        }
    }
}

private fun DrawScope.drawConnections(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedConnectionId: String?
) {
    connections.forEach { connection ->
        val from = nodes.firstOrNull {
            it.id == connection.fromNodeId
        } ?: return@forEach

        val to = nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return@forEach

        val start = Offset(
            from.x + NODE_WIDTH,
            from.y + NODE_HEIGHT / 2f
        )

        val end = Offset(
            to.x,
            to.y + NODE_HEIGHT / 2f
        )

        val midX = (start.x + end.x) / 2f

        val path = Path().apply {
            moveTo(start.x, start.y)
            lineTo(midX, start.y)
            lineTo(midX, end.y)
            lineTo(end.x, end.y)
        }

        drawPath(
            path = path,
            color =
                if (connection.id == selectedConnectionId) {
                    Accent
                } else {
                    CableColor
                },
            strokeWidth =
                if (connection.id == selectedConnectionId) {
                    6f
                } else {
                    4f
                }
        )

        val arrowX = end.x - 14f
        val arrowY = end.y

        val arrow = Path().apply {
            moveTo(arrowX, arrowY - 8f)
            lineTo(end.x, arrowY)
            lineTo(arrowX, arrowY + 8f)
            close()
        }

        drawPath(
            path = arrow,
            color =
                if (connection.id == selectedConnectionId) {
                    Accent
                } else {
                    CableColor
                }
        )

        val label =
            if (connection.cableSizeMm2 > 0.0) {
                "${format(connection.cableSizeMm2)} mm² × ${connection.parallelRuns}"
            } else {
                "Cable"
            }

        drawTextCanvas(
            text = label,
            x = midX - 45f,
            y = min(start.y, end.y) - 12f,
            color = SecondaryText,
            size = 18f
        )
    }
}

private fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    val color = nodeColor(node.type)

    val x = node.x
    val y = node.y

    when (node.type) {
        SldNodeType.SOURCE -> {
            drawCircle(
                color = color,
                radius = 34f,
                center = Offset(
                    x + 55f,
                    y + 48f
                )
            )

            drawLine(
                color = Color.White,
                start = Offset(x + 42f, y + 48f),
                end = Offset(x + 68f, y + 48f),
                strokeWidth = 4f
            )

            drawLine(
                color = Color.White,
                start = Offset(x + 55f, y + 35f),
                end = Offset(x + 55f, y + 61f),
                strokeWidth = 4f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawCircle(
                color = color,
                radius = 30f,
                center = Offset(x + 48f, y + 48f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 6f
                )
            )

            drawCircle(
                color = color,
                radius = 30f,
                center = Offset(x + 88f, y + 48f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 6f
                )
            )

            drawLine(
                color = color,
                start = Offset(x + 68f, y + 14f),
                end = Offset(x + 68f, y + 82f),
                strokeWidth = 5f
            )
        }

        SldNodeType.GENERATOR -> {
            drawCircle(
                color = color,
                radius = 38f,
                center = Offset(x + 65f, y + 50f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 6f
                )
            )

            drawTextCanvas(
                text = "G",
                x = x + 54f,
                y = y + 61f,
                color = color,
                size = 30f
            )
        }

        SldNodeType.BUS -> {
            drawLine(
                color = color,
                start = Offset(x + 10f, y + 50f),
                end = Offset(x + 150f, y + 50f),
                strokeWidth = 12f
            )

            drawLine(
                color = color,
                start = Offset(x + 30f, y + 40f),
                end = Offset(x + 30f, y + 60f),
                strokeWidth = 4f
            )

            drawLine(
                color = color,
                start = Offset(x + 120f, y + 40f),
                end = Offset(x + 120f, y + 60f),
                strokeWidth = 4f
            )
        }

        SldNodeType.BREAKER -> {
            drawRect(
                color = color,
                topLeft = Offset(x + 35f, y + 25f),
                size = Size(65f, 50f)
            )

            drawLine(
                color = Color.White,
                start = Offset(x + 48f, y + 60f),
                end = Offset(x + 87f, y + 38f),
                strokeWidth = 5f
            )

            drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(x + 48f, y + 60f)
            )

            drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(x + 87f, y + 38f)
            )
        }

        SldNodeType.PANEL -> {
            drawRect(
                color = color,
                topLeft = Offset(x + 25f, y + 20f),
                size = Size(100f, 65f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 6f
                )
            )

            drawLine(
                color = color,
                start = Offset(x + 35f, y + 42f),
                end = Offset(x + 115f, y + 42f),
                strokeWidth = 5f
            )
        }

        SldNodeType.LOAD -> {
            drawCircle(
                color = color,
                radius = 34f,
                center = Offset(x + 60f, y + 50f)
            )

            drawTextCanvas(
                text = "L",
                x = x + 51f,
                y = y + 60f,
                color = Color.White,
                size = 28f
            )
        }
    }

    if (selected || connectionStart) {
        drawRect(
            color = if (connectionStart) Accent else Success,
            topLeft = Offset(
                x - 5f,
                y - 5f
            ),
            size = Size(
                NODE_WIDTH + 10f,
                NODE_HEIGHT + 10f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 4f
            )
        )
    }

    drawTextCanvas(
        text = node.name,
        x = x,
        y = y + 105f,
        color = PrimaryText,
        size = 19f
    )

    drawNodeEngineeringData(
        node = node,
        x = x,
        y = y
    )
}

private fun DrawScope.drawNodeEngineeringData(
    node: SldNode,
    x: Float,
    y: Float
) {
    when (node.type) {
        SldNodeType.SOURCE -> {
            drawTextCanvas(
                text = "${format(node.voltage)} V",
                x = x + 105f,
                y = y + 35f,
                color = SecondaryText,
                size = 16f
            )

            drawTextCanvas(
                text = "${format(node.sourceShortCircuitMva)} MVA",
                x = x + 105f,
                y = y + 57f,
                color = SecondaryText,
                size = 16f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawTextCanvas(
                text = "${format(node.ratedKva)} kVA",
                x = x + 105f,
                y = y + 35f,
                color = SecondaryText,
                size = 16f
            )

            drawTextCanvas(
                text = "Z=${format(node.transformerPercentZ)}%",
                x = x + 105f,
                y = y + 57f,
                color = SecondaryText,
                size = 16f
            )
        }

        SldNodeType.GENERATOR -> {
            drawTextCanvas(
                text = "${format(node.ratedKva)} kVA",
                x = x + 105f,
                y = y + 35f,
                color = SecondaryText,
                size = 16f
            )

            drawTextCanvas(
                text = "Xd''=${format(node.generatorXdSubtransient)}%",
                x = x + 105f,
                y = y + 57f,
                color = SecondaryText,
                size = 16f
            )
        }

        SldNodeType.BUS -> {
            drawTextCanvas(
                text = "${format(node.voltage)} V",
                x = x + 40f,
                y = y + 85f,
                color = SecondaryText,
                size = 16f
            )
        }

        SldNodeType.BREAKER -> {
            drawTextCanvas(
                text = "Icu/Ics",
                x = x + 105f,
                y = y + 35f,
                color = SecondaryText,
                size = 16f
            )
        }

        SldNodeType.PANEL -> {
            drawTextCanvas(
                text = "${format(node.ratedKva)} kVA",
                x = x + 105f,
                y = y + 35f,
                color = SecondaryText,
                size = 16f
            )
        }

        SldNodeType.LOAD -> {
            val current = if (
                node.voltage > 0.0 &&
                    node.powerFactor > 0.0
            ) {
                node.loadKw * 1000.0 /
                    (
                        sqrt(3.0) *
                            node.voltage *
                            node.powerFactor
                        )
            } else {
                0.0
            }

            drawTextCanvas(
                text = "${format(node.loadKw)} kW",
                x = x + 100f,
                y = y + 35f,
                color = SecondaryText,
                size = 16f
            )

            drawTextCanvas(
                text = "${format(current)} A",
                x = x + 100f,
                y = y + 57f,
                color = SecondaryText,
                size = 16f
            )
        }
    }
}

private fun DrawScope.drawTextCanvas(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    size: Float
) {
    val paint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        this.color = color.toArgb()
        textSize = size
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL
        )
    }

    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y,
        paint
    )
}

private fun Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255f).toInt(),
        (red * 255f).toInt(),
        (green * 255f).toInt(),
        (blue * 255f).toInt()
    )

private fun findNode(
    position: Offset,
    nodes: List<SldNode>
): SldNode? {
    return nodes
        .asReversed()
        .firstOrNull { node ->
            position.x >= node.x - TOUCH_PADDING &&
                position.x <= node.x + NODE_WIDTH + TOUCH_PADDING &&
                position.y >= node.y - TOUCH_PADDING &&
                position.y <= node.y + NODE_HEIGHT + TOUCH_PADDING
        }
}

private fun findConnection(
    position: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {
    var best: SldConnection? = null
    var bestDistance = 18f

    connections.forEach { connection ->
        val from = nodes.firstOrNull {
            it.id == connection.fromNodeId
        } ?: return@forEach

        val to = nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return@forEach

        val start = Offset(
            from.x + NODE_WIDTH,
            from.y + NODE_HEIGHT / 2f
        )

        val end = Offset(
            to.x,
            to.y + NODE_HEIGHT / 2f
        )

        val midX = (start.x + end.x) / 2f

        val d1 = distanceToSegment(
            position,
            start,
            Offset(midX, start.y)
        )

        val d2 = distanceToSegment(
            position,
            Offset(midX, start.y),
            Offset(midX, end.y)
        )

        val d3 = distanceToSegment(
            position,
            Offset(midX, end.y),
            end
        )

        val d = min(d1, min(d2, d3))

        if (d < bestDistance) {
            bestDistance = d
            best = connection
        }
    }

    return best
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

    val t = (
        (p.x - a.x) * dx +
            (p.y - a.y) * dy
        ) / (
        dx * dx +
            dy * dy
        )

    val clamped = t.coerceIn(0f, 1f)

    val x = a.x + clamped * dx
    val y = a.y + clamped * dy

    return hypot(
        p.x - x,
        p.y - y
    )
}

private fun nodeColor(
    type: SldNodeType
): Color =
    when (type) {
        SldNodeType.SOURCE -> SourceColor
        SldNodeType.TRANSFORMER -> TransformerColor
        SldNodeType.GENERATOR -> GeneratorColor
        SldNodeType.BREAKER -> BreakerColor
        SldNodeType.BUS -> BusColor
        SldNodeType.PANEL -> PanelColor
        SldNodeType.LOAD -> LoadColor
    }

private fun format(value: Double): String {
    if (!value.isFinite()) return "0.00"

    return if (kotlin.math.abs(value) >= 100.0) {
        "%.1f".format(value)
    } else {
        "%.2f".format(value)
    }
}

private fun readNumber(
    obj: Any?,
    names: List<String>
): Double? {
    if (obj == null) return null

    for (name in names) {
        try {
            val field = obj.javaClass.declaredFields.firstOrNull {
                it.name.equals(name, ignoreCase = true)
            }

            if (field != null) {
                field.isAccessible = true
                val value = field.get(obj)

                when (value) {
                    is Number -> return value.toDouble()
                    is String -> value.toDoubleOrNull()?.let {
                        return it
                    }
                }
            }
        } catch (_: Exception) {
        }

        try {
            val getter =
                "get" +
                    name.replaceFirstChar {
                        it.uppercase()
                    }

            val method = obj.javaClass.methods.firstOrNull {
                it.name.equals(
                    getter,
                    ignoreCase = true
                ) &&
                    it.parameterTypes.isEmpty()
            }

            if (method != null) {
                val value = method.invoke(obj)

                when (value) {
                    is Number -> return value.toDouble()
                    is String -> value.toDoubleOrNull()?.let {
                        return it
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    return null
}

private fun buildShortCircuitReport(
    study: SldShortCircuitStudy,
    nodes: List<SldNode>,
    arabic: Boolean
): String {
    val sb = StringBuilder()

    sb.appendLine(
        if (arabic)
            "════════════════════════════════"
        else
            "================================"
    )

    sb.appendLine(
        if (arabic)
            "تقرير هندسي لحساب تيارات القصر"
        else
            "SHORT CIRCUIT ENGINEERING REPORT"
    )

    sb.appendLine(
        if (arabic)
            "IEC based network study"
        else
            "IEC based network study"
    )

    sb.appendLine()

    val studyText = study.toString()

    val globalIk =
        readNumber(
            study,
            listOf(
                "faultCurrentKA",
                "maximumFaultCurrentKa",
                "maxFaultCurrentKa",
                "initialFaultCurrentKA",
                "ik"
            )
        )

    val globalPeak =
        readNumber(
            study,
            listOf(
                "peakCurrentKA",
                "maximumPeakCurrentKa",
                "ip",
                "peakKA"
            )
        )

    val globalMva =
        readNumber(
            study,
            listOf(
                "faultMva",
                "maximumFaultMva",
                "faultLevelMva"
            )
        )

    val globalXR =
        readNumber(
            study,
            listOf(
                "xOverR",
                "xr",
                "xR"
            )
        )

    if (globalIk != null) {
        sb.appendLine(
            "Ik'' = ${format(globalIk)} kA"
        )
    }

    if (globalPeak != null) {
        sb.appendLine(
            "Ip = ${format(globalPeak)} kA"
        )
    }

    if (globalMva != null) {
        sb.appendLine(
            "Fault MVA = ${format(globalMva)} MVA"
        )
    }

    if (globalXR != null) {
        sb.appendLine(
            "X/R = ${format(globalXR)}"
        )
    }

    sb.appendLine()
    sb.appendLine(
        if (arabic)
            "نقاط الشبكة"
        else
            "NETWORK FAULT POINTS"
    )
    sb.appendLine("--------------------------------")

    nodes.forEach { node ->
        if (
            node.type == SldNodeType.BUS ||
            node.type == SldNodeType.PANEL ||
            node.type == SldNodeType.SOURCE
        ) {
            val voltage = node.voltage

            val calculatedIk = when {
                node.type == SldNodeType.SOURCE &&
                    node.sourceShortCircuitMva > 0.0 &&
                    voltage > 0.0 -> {
                    node.sourceShortCircuitMva *
                        1000.0 /
                        (
                            sqrt(3.0) *
                                voltage *
                                1.05
                            )
                }

                else -> null
            }

            val faultMva = calculatedIk?.let {
                sqrt(3.0) *
                    voltage *
                    it /
                    1000.0
            }

            val breaker = selectBreaker(
                calculatedIk ?: 0.0
            )

            sb.appendLine(
                "• ${node.name}"
            )

            sb.appendLine(
                "  Voltage        : ${format(voltage)} V"
            )

            if (calculatedIk != null) {
                sb.appendLine(
                    "  Ik''           : ${format(calculatedIk)} kA"
                )

                sb.appendLine(
                    "  Ip peak        : ${format(calculatedIk * 2.2)} kA"
                )

                sb.appendLine(
                    "  Ith            : ${format(calculatedIk)} kA / 1s"
                )

                sb.appendLine(
                    "  Fault MVA      : ${format(faultMva ?: 0.0)} MVA"
                )

                sb.appendLine(
                    "  X/R            : calculated by study"
                )

                sb.appendLine(
                    "  Required Icu   : ${format(breaker.toDouble())} kA"
                )

                sb.appendLine(
                    "  Recommended Ics: ≥ ${format(breaker * 0.75)} kA"
                )
            } else {
                sb.appendLine(
                    "  Ik''           : study result"
                )

                sb.appendLine(
                    "  Ip peak        : study result"
                )

                sb.appendLine(
                    "  Ith            : study result"
                )

                sb.appendLine(
                    "  Fault MVA      : study result"
                )

                sb.appendLine(
                    "  Required Icu   : select from fault level"
                )
            }

            if (
                node.type == SldNodeType.TRANSFORMER
            ) {
                sb.appendLine(
                    "  Transformer    : ${format(node.ratedKva)} kVA"
                )

                sb.appendLine(
                    "  Transformer Z  : ${format(node.transformerPercentZ)}%"
                )
            }

            sb.appendLine()
        }
    }

    sb.appendLine(
        if (arabic)
            "ملاحظة:"
        else
            "ENGINEERING NOTE:"
    )

    sb.appendLine(
        if (arabic)
            "الـ Ics قيمة اختيارية تعتمد على سلسلة القاطع وبيانات المصنع. يجب اعتماد القيمة النهائية من كتالوج القاطع."
        else
            "Ics is manufacturer/product dependent. Final breaker selection shall be verified against the manufacturer's catalogue."
    )

    sb.appendLine()
    sb.appendLine(
        "Study object processed without displaying raw toString() data."
    )

    if (
        globalIk == null &&
        globalPeak == null &&
        globalMva == null
    ) {
        sb.appendLine()
        sb.appendLine(
            "Engine study object fields were not exposed directly; network-derived engineering values are shown where available."
        )
    }

    return sb.toString()
}

private fun buildNetworkReport(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    arabic: Boolean
): String {
    val sb = StringBuilder()

    sb.appendLine(
        if (arabic)
            "تقرير الشبكة الكهربائية"
        else
            "ELECTRICAL NETWORK REPORT"
    )

    sb.appendLine("--------------------------------")

    sb.appendLine(
        "Nodes = ${nodes.size}"
    )

    sb.appendLine(
        "Connections = ${connections.size}"
    )

    sb.appendLine()

    nodes.forEachIndexed { index, node ->
        sb.appendLine(
            "${index + 1}. ${node.name}"
        )

        sb.appendLine(
            "   Type    : ${typeEnglish(node.type)}"
        )

        sb.appendLine(
            "   Voltage : ${format(node.voltage)} V"
        )

        when (node.type) {
            SldNodeType.SOURCE -> {
                sb.appendLine(
                    "   Source Fault Level : ${format(node.sourceShortCircuitMva)} MVA"
                )
            }

            SldNodeType.TRANSFORMER -> {
                sb.appendLine(
                    "   Rating  : ${format(node.ratedKva)} kVA"
                )

                sb.appendLine(
                    "   Z%      : ${format(node.transformerPercentZ)}%"
                )
            }

            SldNodeType.GENERATOR -> {
                sb.appendLine(
                    "   Rating  : ${format(node.ratedKva)} kVA"
                )

                sb.appendLine(
                    "   Xd''    : ${format(node.generatorXdSubtransient)}%"
                )
            }

            SldNodeType.LOAD -> {
                val current =
                    if (
                        node.voltage > 0.0 &&
                            node.powerFactor > 0.0
                    ) {
                        node.loadKw * 1000.0 /
                            (
                                sqrt(3.0) *
                                    node.voltage *
                                    node.powerFactor
                                )
                    } else {
                        0.0
                    }

                sb.appendLine(
                    "   Load    : ${format(node.loadKw)} kW"
                )

                sb.appendLine(
                    "   Current : ${format(current)} A"
                )
            }

            else -> {
            }
        }

        sb.appendLine()
    }

    sb.appendLine(
        if (arabic) "الكابلات والربط" else "FEEDERS / CONNECTIONS"
    )

    sb.appendLine("--------------------------------")

    connections.forEachIndexed { index, c ->
        val from =
            nodes.firstOrNull {
                it.id == c.fromNodeId
            }?.name ?: c.fromNodeId

        val to =
            nodes.firstOrNull {
                it.id == c.toNodeId
            }?.name ?: c.toNodeId

        sb.appendLine(
            "${index + 1}. $from → $to"
        )

        sb.appendLine(
            "   Length : ${format(c.lengthMeters)} m"
        )

        sb.appendLine(
            "   Cable  : ${
                if (c.cableSizeMm2 > 0.0)
                    "${format(c.cableSizeMm2)} mm²"
                else
                    "Auto / not selected"
            }"
        )

        sb.appendLine(
            "   Runs   : ${c.parallelRuns}"
        )

        sb.appendLine()
    }

    return sb.toString()
}

private fun buildPanelSchedule(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    arabic: Boolean
): String {
    val sb = StringBuilder()

    sb.appendLine(
        "${panel.name} - PANEL SCHEDULE"
    )

    sb.appendLine(
        "Voltage: ${format(panel.voltage)} V"
    )

    sb.appendLine()

    sb.appendLine(
        if (arabic)
            "Ckt | Load | kW | PF | Demand | Current | Cable | Runs"
        else
            "Ckt | Load | kW | PF | Demand | Current | Cable | Runs"
    )

    sb.appendLine(
        "------------------------------------------------------------"
    )

    var totalKw = 0.0
    var demandKw = 0.0

    val outgoing = connections.filter {
        it.fromNodeId == panel.id
    }

    outgoing.forEachIndexed { index, connection ->
        val load =
            nodes.firstOrNull {
                it.id == connection.toNodeId
            }

        if (load != null) {
            val current =
                if (
                    load.voltage > 0.0 &&
                        load.powerFactor > 0.0
                ) {
                    load.loadKw * 1000.0 /
                        (
                            sqrt(3.0) *
                                load.voltage *
                                load.powerFactor
                            )
                } else {
                    0.0
                }

            totalKw += load.loadKw
            demandKw +=
                load.loadKw *
                    load.demandFactor

            val cable =
                if (connection.cableSizeMm2 > 0.0) {
                    "${format(connection.cableSizeMm2)}"
                } else {
                    "Auto"
                }

            sb.appendLine(
                "${index + 1} | ${load.name} | " +
                    "${format(load.loadKw)} | " +
                    "${format(load.powerFactor)} | " +
                    "${format(load.demandFactor)} | " +
                    "${format(current)} A | " +
                    "$cable mm² | " +
                    "${connection.parallelRuns}"
            )
        }
    }

    sb.appendLine()
    sb.appendLine(
        "------------------------------------------------------------"
    )

    sb.appendLine(
        "Connected Load = ${format(totalKw)} kW"
    )

    sb.appendLine(
        "Demand Load    = ${format(demandKw)} kW"
    )

    val spare =
        if (panel.ratedKva > 0.0) {
            panel.ratedKva -
                demandKw / 0.90 * 1.0
        } else {
            0.0
        }

    sb.appendLine(
        "Estimated Spare = ${format(max(spare, 0.0))} kVA"
    )

    return sb.toString()
}

private fun selectBreaker(
    faultKa: Double
): Int {
    val standard = listOf(
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
        it.toDouble() >= faultKa
    } ?: 200
}

private fun typeEnglish(
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

@Composable
private fun NodeDialog(
    arabic: Boolean,
    type: SldNodeType,
    editing: Boolean,
    name: String,
    voltage: String,
    kw: String,
    pf: String,
    demand: String,
    kva: String,
    z: String,
    xd: String,
    sourceMva: String,
    onName: (String) -> Unit,
    onVoltage: (String) -> Unit,
    onKw: (String) -> Unit,
    onPf: (String) -> Unit,
    onDemand: (String) -> Unit,
    onKva: (String) -> Unit,
    onZ: (String) -> Unit,
    onXd: (String) -> Unit,
    onSourceMva: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (editing) {
                    if (arabic) "تعديل" else "Edit"
                } else {
                    if (arabic) "إضافة" else "Add"
                } + " ${if (arabic) typeNameArabic(type) else typeEnglish(type)}"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Field(
                    label = if (arabic) "الاسم" else "Name",
                    value = name,
                    onValue = onName
                )

                Field(
                    label = if (arabic) "الجهد V" else "Voltage V",
                    value = voltage,
                    onValue = onVoltage
                )

                when (type) {
                    SldNodeType.SOURCE -> {
                        Field(
                            label =
                                if (arabic)
                                    "مستوى القصر MVA"
                                else
                                    "Source Short-Circuit MVA",
                            value = sourceMva,
                            onValue = onSourceMva
                        )
                    }

                    SldNodeType.TRANSFORMER -> {
                        Field(
                            label =
                                if (arabic)
                                    "قدرة المحول kVA"
                                else
                                    "Transformer Rating kVA",
                            value = kva,
                            onValue = onKva
                        )

                        Field(
                            label =
                                if (arabic)
                                    "%Z المحول"
                                else
                                    "Transformer %Z",
                            value = z,
                            onValue = onZ
                        )
                    }

                    SldNodeType.GENERATOR -> {
                        Field(
                            label =
                                if (arabic)
                                    "قدرة المولد kVA"
                                else
                                    "Generator Rating kVA",
                            value = kva,
                            onValue = onKva
                        )

                        Field(
                            label =
                                if (arabic)
                                    "Xd'' %"
                                else
                                    "Xd'' %",
                            value = xd,
                            onValue = onXd
                        )
                    }

                    SldNodeType.PANEL -> {
                        Field(
                            label =
                                if (arabic)
                                    "قدرة اللوحة kVA"
                                else
                                    "Panel Rating kVA",
                            value = kva,
                            onValue = onKva
                        )
                    }

                    SldNodeType.LOAD -> {
                        Field(
                            label =
                                if (arabic)
                                    "الحمل kW"
                                else
                                    "Load kW",
                            value = kw,
                            onValue = onKw
                        )

                        Field(
                            label =
                                if (arabic)
                                    "Power Factor"
                                else
                                    "Power Factor",
                            value = pf,
                            onValue = onPf
                        )

                        Field(
                            label =
                                if (arabic)
                                    "Demand Factor"
                                else
                                    "Demand Factor",
                            value = demand,
                            onValue = onDemand
                        )
                    }

                    SldNodeType.BUS,
                    SldNodeType.BREAKER -> {
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(if (arabic) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(if (arabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
private fun ConnectionDialog(
    arabic: Boolean,
    length: String,
    r: String,
    x: String,
    size: String,
    runs: String,
    capacity: String,
    onLength: (String) -> Unit,
    onR: (String) -> Unit,
    onX: (String) -> Unit,
    onSize: (String) -> Unit,
    onRuns: (String) -> Unit,
    onCapacity: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (arabic)
                    "بيانات الكابل"
                else
                    "Cable / Feeder Data"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Field(
                    label =
                        if (arabic)
                            "الطول m"
                        else
                            "Length m",
                    value = length,
                    onValue = onLength
                )

                Field(
                    label = "R Ω/km",
                    value = r,
                    onValue = onR
                )

                Field(
                    label = "X Ω/km",
                    value = x,
                    onValue = onX
                )

                Field(
                    label =
                        if (arabic)
                            "مقطع الكابل mm²"
                        else
                            "Cable Section mm²",
                    value = size,
                    onValue = onSize
                )

                Field(
                    label =
                        if (arabic)
                            "عدد المسارات"
                        else
                            "Parallel Runs",
                    value = runs,
                    onValue = onRuns
                )

                Field(
                    label =
                        if (arabic)
                            "التيار المسموح A"
                        else
                            "Current Capacity A",
                    value = capacity,
                    onValue = onCapacity
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(if (arabic) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(if (arabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = {
            Text(label)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun typeNameArabic(
    type: SldNodeType
): String =
    when (type) {
        SldNodeType.SOURCE -> "مصدر"
        SldNodeType.TRANSFORMER -> "محول"
        SldNodeType.GENERATOR -> "مولد"
        SldNodeType.BUS -> "باسبار"
        SldNodeType.BREAKER -> "قاطع"
        SldNodeType.PANEL -> "لوحة"
        SldNodeType.LOAD -> "حمل"
    }
