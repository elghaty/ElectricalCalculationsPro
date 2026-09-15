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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private val Background = Color(0xFF070C10)
private val CanvasBackground = Color(0xFF09131A)
private val Primary = Color(0xFFF2F5F7)
private val Secondary = Color(0xFF9BA8B2)
private val Accent = Color(0xFF00BCD4)
private val Selected = Color(0xFF00E676)
private val SourceColor = Color(0xFF1976D2)
private val TransformerColor = Color(0xFFFF9800)
private val GeneratorColor = Color(0xFF43A047)
private val BreakerColor = Color(0xFF607D8B)
private val BusColor = Color(0xFF9C27B0)
private val PanelColor = Color(0xFF00838F)
private val LoadColor = Color(0xFF455A64)
private val CableColor = Color(0xFFB0BEC5)

private const val NODE_WIDTH = 170f
private const val NODE_HEIGHT = 112f
private const val TOUCH = 45f

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
                    y = 280f,
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
    var showReport by remember { mutableStateOf(false) }

    var reportTitle by remember { mutableStateOf("") }
    var reportText by remember { mutableStateOf("") }

    var nodeType by remember { mutableStateOf(SldNodeType.LOAD) }
    var name by remember { mutableStateOf("") }
    var voltage by remember { mutableStateOf("400") }
    var loadKw by remember { mutableStateOf("50") }
    var pf by remember { mutableStateOf("0.90") }
    var demand by remember { mutableStateOf("1.0") }
    var kva by remember { mutableStateOf("1000") }
    var transformerZ by remember { mutableStateOf("6") }
    var generatorXd by remember { mutableStateOf("15") }
    var sourceMva by remember { mutableStateOf("500") }

    var length by remember { mutableStateOf("10") }
    var resistance by remember { mutableStateOf("0.125") }
    var reactance by remember { mutableStateOf("0.080") }
    var cableSize by remember { mutableStateOf("0") }
    var parallelRuns by remember { mutableStateOf("1") }
    var capacity by remember { mutableStateOf("0") }

    fun network() = SldNetwork(
        nodes = nodes,
        connections = connections
    )

    fun nextName(type: SldNodeType): String {
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

    fun resetFields(type: SldNodeType) {
        nodeType = type
        editingNodeId = null
        name = nextName(type)
        voltage = "400"
        loadKw = if (type == SldNodeType.LOAD) "50" else "0"
        pf = "0.90"
        demand = "1.0"
        kva = when (type) {
            SldNodeType.TRANSFORMER,
            SldNodeType.GENERATOR,
            SldNodeType.PANEL -> "1000"

            else -> "0"
        }
        transformerZ = "6"
        generatorXd = "15"
        sourceMva = "500"
        showNodeDialog = true
    }

    fun editNode(node: SldNode) {
        editingNodeId = node.id
        nodeType = node.type
        name = node.name
        voltage = node.voltage.toString()
        loadKw = node.loadKw.toString()
        pf = node.powerFactor.toString()
        demand = node.demandFactor.toString()
        kva = node.ratedKva.toString()
        transformerZ = node.transformerPercentZ.toString()
        generatorXd = node.generatorXdSubtransient.toString()
        sourceMva = node.sourceShortCircuitMva.toString()
        showNodeDialog = true
    }

    fun saveNode() {
        val v = voltage.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 400.0
        val kw = loadKw.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val p = pf.toDoubleOrNull()?.coerceIn(0.01, 1.0) ?: 0.9
        val d = demand.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
        val s = kva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val z = transformerZ.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val xd = generatorXd.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val mva = sourceMva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val id = editingNodeId

        if (id == null) {
            val index = nodes.size
            val newNode = SldNode(
                id = "node-${System.nanoTime()}",
                name = name.ifBlank { nextName(nodeType) },
                type = nodeType,
                x = 100f + index * 210f,
                y = 280f,
                voltage = v,
                loadKw = kw,
                powerFactor = p,
                demandFactor = d,
                ratedKva = s,
                transformerPercentZ = z,
                generatorXdSubtransient = xd,
                sourceShortCircuitMva = mva
            )

            nodes = nodes + newNode
            selectedNodeId = newNode.id
            selectedConnectionId = null
        } else {
            nodes = nodes.map {
                if (it.id == id) {
                    it.copy(
                        name = name.ifBlank { it.name },
                        type = nodeType,
                        voltage = v,
                        loadKw = kw,
                        powerFactor = p,
                        demandFactor = d,
                        ratedKva = s,
                        transformerPercentZ = z,
                        generatorXdSubtransient = xd,
                        sourceShortCircuitMva = mva
                    )
                } else {
                    it
                }
            }
        }

        showNodeDialog = false
    }

    fun editConnection(connection: SldConnection) {
        editingConnectionId = connection.id
        length = connection.lengthMeters.toString()
        resistance = connection.resistanceOhmPerKm.toString()
        reactance = connection.reactanceOhmPerKm.toString()
        cableSize = connection.cableSizeMm2.toString()
        parallelRuns = connection.parallelRuns.toString()
        capacity = connection.currentCapacityA.toString()
        showConnectionDialog = true
    }

    fun saveConnection() {
        val id = editingConnectionId ?: return

        val l = length.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 10.0
        val r = resistance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.125
        val x = reactance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.080
        val cs = cableSize.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val runs = parallelRuns.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val amp = capacity.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        connections = connections.map {
            if (it.id == id) {
                it.copy(
                    lengthMeters = l,
                    resistanceOhmPerKm = r,
                    reactanceOhmPerKm = x,
                    cableSizeMm2 = cs,
                    parallelRuns = runs,
                    currentCapacityA = amp
                )
            } else {
                it
            }
        }

        showConnectionDialog = false
    }

    fun connect() {
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
            connections = connections + SldConnection(
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
        }

        connectionStartId = null
    }

    fun deleteSelected() {
        selectedNodeId?.let { id ->
            if (id != "source-1") {
                nodes = nodes.filterNot { it.id == id }
                connections = connections.filter {
                    it.fromNodeId != id && it.toNodeId != id
                }
                selectedNodeId = null
                selectedConnectionId = null
                connectionStartId = null
                return
            }
        }

        selectedConnectionId?.let { id ->
            connections = connections.filterNot { it.id == id }
            selectedConnectionId = null
        }
    }

    fun showShortCircuit() {
        try {
            val study = SldShortCircuitEngine.calculate(
                network = network(),
                voltageFactor = 1.05
            )

            reportTitle = if (arabic) {
                "تقرير تيارات القصر"
            } else {
                "SHORT CIRCUIT ENGINEERING REPORT"
            }

            reportText = buildShortCircuitReport(
                study = study,
                nodes = nodes,
                arabic = arabic
            )

            showReport = true
        } catch (e: Exception) {
            reportTitle = if (arabic) "خطأ في الحساب" else "CALCULATION ERROR"
            reportText = e.message ?: "Calculation failed."
            showReport = true
        }
    }

    fun showPanelSchedule() {
        val panel = nodes.firstOrNull {
            it.id == selectedNodeId && it.type == SldNodeType.PANEL
        }

        reportTitle = if (arabic) "جدول اللوحة" else "PANEL SCHEDULE"

        reportText = if (panel == null) {
            if (arabic) {
                "اختر لوحة PANEL أولاً."
            } else {
                "Select a PANEL first."
            }
        } else {
            buildPanelSchedule(
                panel = panel,
                nodes = nodes,
                connections = connections,
                arabic = arabic
            )
        }

        showReport = true
    }

    fun generateCompleteSld() {
        val source = nodes.firstOrNull {
            it.type == SldNodeType.SOURCE
        } ?: SldNode(
            id = "source-1",
            name = "MAIN SOURCE",
            type = SldNodeType.SOURCE,
            x = 80f,
            y = 280f,
            voltage = 400.0,
            sourceShortCircuitMva = 500.0
        )

        val src = source.copy(
            id = "source-1",
            x = 80f,
            y = 280f
        )

        val cb1 = SldNode(
            id = "auto-cb-1",
            name = "MAIN ACB",
            type = SldNodeType.BREAKER,
            x = 300f,
            y = 280f,
            voltage = src.voltage
        )

        val tr = SldNode(
            id = "auto-tr-1",
            name = "TR-01",
            type = SldNodeType.TRANSFORMER,
            x = 520f,
            y = 280f,
            voltage = 400.0,
            ratedKva = 1000.0,
            transformerPercentZ = 6.0
        )

        val bus = SldNode(
            id = "auto-bus-1",
            name = "LV MAIN BUS",
            type = SldNodeType.BUS,
            x = 740f,
            y = 280f,
            voltage = 400.0
        )

        val cb2 = SldNode(
            id = "auto-cb-2",
            name = "INCOMER ACB",
            type = SldNodeType.BREAKER,
            x = 960f,
            y = 280f,
            voltage = 400.0
        )

        val panel = SldNode(
            id = "auto-panel-1",
            name = "MDB-01",
            type = SldNodeType.PANEL,
            x = 1180f,
            y = 280f,
            voltage = 400.0,
            ratedKva = 1000.0
        )

        val loads = listOf(
            SldNode(
                id = "auto-load-1",
                name = "LOAD-01",
                type = SldNodeType.LOAD,
                x = 1430f,
                y = 120f,
                voltage = 400.0,
                loadKw = 50.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            ),
            SldNode(
                id = "auto-load-2",
                name = "LOAD-02",
                type = SldNodeType.LOAD,
                x = 1430f,
                y = 280f,
                voltage = 400.0,
                loadKw = 75.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            ),
            SldNode(
                id = "auto-load-3",
                name = "LOAD-03",
                type = SldNodeType.LOAD,
                x = 1430f,
                y = 440f,
                voltage = 400.0,
                loadKw = 100.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            )
        )

        nodes = listOf(src, cb1, tr, bus, cb2, panel) + loads

        fun cable(
            id: String,
            from: String,
            to: String,
            meters: Double
        ) = SldConnection(
            id = id,
            fromNodeId = from,
            toNodeId = to,
            lengthMeters = meters,
            resistanceOhmPerKm = 0.125,
            reactanceOhmPerKm = 0.080,
            cableSizeMm2 = 0.0,
            parallelRuns = 1,
            currentCapacityA = 0.0
        )

        connections = listOf(
            cable("auto-c1", "source-1", "auto-cb-1", 5.0),
            cable("auto-c2", "auto-cb-1", "auto-tr-1", 5.0),
            cable("auto-c3", "auto-tr-1", "auto-bus-1", 5.0),
            cable("auto-c4", "auto-bus-1", "auto-cb-2", 5.0),
            cable("auto-c5", "auto-cb-2", "auto-panel-1", 10.0),
            cable("auto-c6", "auto-panel-1", "auto-load-1", 20.0),
            cable("auto-c7", "auto-panel-1", "auto-load-2", 20.0),
            cable("auto-c8", "auto-panel-1", "auto-load-3", 20.0)
        )

        selectedNodeId = panel.id
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
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (onBack != null) {
                Button(onClick = onBack) {
                    Text(if (arabic) "رجوع" else "Back")
                }
            }

            Button(onClick = { resetFields(SldNodeType.SOURCE) }) {
                Text(if (arabic) "مصدر" else "Source")
            }

            Button(onClick = { resetFields(SldNodeType.TRANSFORMER) }) {
                Text(if (arabic) "محول" else "Transformer")
            }

            Button(onClick = { resetFields(SldNodeType.GENERATOR) }) {
                Text(if (arabic) "مولد" else "Generator")
            }

            Button(onClick = { resetFields(SldNodeType.BUS) }) {
                Text(if (arabic) "باسبار" else "Busbar")
            }

            Button(onClick = { resetFields(SldNodeType.BREAKER) }) {
                Text(if (arabic) "قاطع" else "Breaker")
            }

            Button(onClick = { resetFields(SldNodeType.PANEL) }) {
                Text(if (arabic) "لوحة" else "Panel")
            }

            Button(onClick = { resetFields(SldNodeType.LOAD) }) {
                Text(if (arabic) "حمل" else "Load")
            }

            Button(onClick = { connect() }) {
                Text(
                    if (connectionStartId == null) {
                        if (arabic) "ربط" else "Connect"
                    } else {
                        if (arabic) "اختر النهاية" else "Select End"
                    }
                )
            }

            Button(onClick = { deleteSelected() }) {
                Text(if (arabic) "حذف" else "Delete")
            }

            Button(onClick = { showShortCircuit() }) {
                Text(if (arabic) "تيارات القصر" else "Short Circuit")
            }

            Button(onClick = { showPanelSchedule() }) {
                Text("Panel Schedule")
            }

            Button(onClick = { generateCompleteSld() }) {
                Text("Generate Complete SLD")
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
                nodes = nodes.map {
                    if (it.id == id) {
                        it.copy(
                            x = max(10f, it.x + dx),
                            y = max(10f, it.y + dy)
                        )
                    } else {
                        it
                    }
                }
            },
            onEditNode = { id ->
                nodes.firstOrNull { it.id == id }?.let {
                    editNode(it)
                }
            },
            onEditConnection = { id ->
                connections.firstOrNull { it.id == id }?.let {
                    editConnection(it)
                }
            }
        )
    }

    if (showNodeDialog) {
        NodeDialog(
            arabic = arabic,
            type = nodeType,
            name = name,
            voltage = voltage,
            loadKw = loadKw,
            pf = pf,
            demand = demand,
            kva = kva,
            transformerZ = transformerZ,
            generatorXd = generatorXd,
            sourceMva = sourceMva,
            onName = { name = it },
            onVoltage = { voltage = it },
            onLoadKw = { loadKw = it },
            onPf = { pf = it },
            onDemand = { demand = it },
            onKva = { kva = it },
            onTransformerZ = { transformerZ = it },
            onGeneratorXd = { generatorXd = it },
            onSourceMva = { sourceMva = it },
            onSave = { saveNode() },
            onCancel = { showNodeDialog = false }
        )
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            arabic = arabic,
            length = length,
            resistance = resistance,
            reactance = reactance,
            cableSize = cableSize,
            parallelRuns = parallelRuns,
            capacity = capacity,
            onLength = { length = it },
            onResistance = { resistance = it },
            onReactance = { reactance = it },
            onCableSize = { cableSize = it },
            onParallelRuns = { parallelRuns = it },
            onCapacity = { capacity = it },
            onSave = { saveConnection() },
            onCancel = { showConnectionDialog = false }
        )
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = {
                Text(
                    reportTitle,
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
                        text = reportText,
                        color = Primary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showReport = false }) {
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
    val textMeasurer = rememberTextMeasurer()

    val currentNodes by rememberUpdatedState(nodes)
    val currentConnections by rememberUpdatedState(connections)
    val selectNode by rememberUpdatedState(onSelectNode)
    val selectConnection by rememberUpdatedState(onSelectConnection)
    val moveNode by rememberUpdatedState(onMoveNode)
    val editNode by rememberUpdatedState(onEditNode)
    val editConnection by rememberUpdatedState(onEditConnection)

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
                .width(2400.dp)
                .height(1100.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { position ->
                            val node = findNode(position, currentNodes)

                            if (node != null) {
                                selectNode(node.id)
                            } else {
                                findConnection(
                                    position,
                                    currentNodes,
                                    currentConnections
                                )?.let {
                                    selectConnection(it.id)
                                }
                            }
                        },
                        onDoubleTap = { position ->
                            val node = findNode(position, currentNodes)

                            if (node != null) {
                                editNode(node.id)
                            } else {
                                findConnection(
                                    position,
                                    currentNodes,
                                    currentConnections
                                )?.let {
                                    editConnection(it.id)
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            val node = findNode(position, currentNodes)
                            draggedNodeId = node?.id

                            if (node != null) {
                                selectNode(node.id)
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()

                            val id = draggedNodeId
                            if (id != null) {
                                moveNode(
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
                connections = currentConnections,
                selectedConnectionId = selectedConnectionId,
                textMeasurer = textMeasurer
            )

            currentNodes.forEach { node ->
                drawNode(
                    node = node,
                    selected = node.id == selectedNodeId,
                    connectionStart = node.id == connectionStartId,
                    textMeasurer = textMeasurer
                )
            }
        }
    }
}

private fun DrawScope.drawConnections(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedConnectionId: String?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
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

        val selected = connection.id == selectedConnectionId

        drawPath(
            path = path,
            color = if (selected) Accent else CableColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = if (selected) 7f else 4f
            )
        )

        val arrow = Path().apply {
            moveTo(end.x - 16f, end.y - 9f)
            lineTo(end.x, end.y)
            lineTo(end.x - 16f, end.y + 9f)
            close()
        }

        drawPath(
            path = arrow,
            color = if (selected) Accent else CableColor
        )

        val cableLabel =
            if (connection.cableSizeMm2 > 0.0) {
                "${fmt(connection.cableSizeMm2)} mm² x ${connection.parallelRuns}"
            } else {
                "CABLE"
            }

        drawText(
            textMeasurer = textMeasurer,
            text = cableLabel,
            topLeft = Offset(
                midX - 45f,
                min(start.y, end.y) - 25f
            ),
            style = TextStyle(
                color = Secondary,
                fontSize = 13.sp
            )
        )
    }
}

private fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val x = node.x
    val y = node.y
    val color = nodeColor(node.type)

    when (node.type) {
        SldNodeType.SOURCE -> {
            drawCircle(
                color = color,
                radius = 36f,
                center = Offset(x + 65f, y + 48f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(6f)
            )

            drawLine(
                color = color,
                start = Offset(x + 65f, y + 12f),
                end = Offset(x + 65f, y + 84f),
                strokeWidth = 4f
            )

            drawLine(
                color = color,
                start = Offset(x + 29f, y + 48f),
                end = Offset(x + 101f, y + 48f),
                strokeWidth = 4f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawCircle(
                color = color,
                radius = 29f,
                center = Offset(x + 48f, y + 48f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(6f)
            )

            drawCircle(
                color = color,
                radius = 29f,
                center = Offset(x + 91f, y + 48f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(6f)
            )

            drawLine(
                color = color,
                start = Offset(x + 70f, y + 12f),
                end = Offset(x + 70f, y + 84f),
                strokeWidth = 4f
            )
        }

        SldNodeType.GENERATOR -> {
            drawCircle(
                color = color,
                radius = 38f,
                center = Offset(x + 65f, y + 48f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(6f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "G",
                topLeft = Offset(x + 55f, y + 30f),
                style = TextStyle(
                    color = color,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        SldNodeType.BUS -> {
            drawLine(
                color = color,
                start = Offset(x + 8f, y + 50f),
                end = Offset(x + 158f, y + 50f),
                strokeWidth = 14f
            )
        }

        SldNodeType.BREAKER -> {
            drawRect(
                color = color,
                topLeft = Offset(x + 38f, y + 23f),
                size = Size(60f, 52f)
            )

            drawLine(
                color = Color.White,
                start = Offset(x + 50f, y + 63f),
                end = Offset(x + 87f, y + 38f),
                strokeWidth = 5f
            )
        }

        SldNodeType.PANEL -> {
            drawRect(
                color = color,
                topLeft = Offset(x + 25f, y + 20f),
                size = Size(105f, 66f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(6f)
            )

            drawLine(
                color = color,
                start = Offset(x + 35f, y + 43f),
                end = Offset(x + 120f, y + 43f),
                strokeWidth = 5f
            )
        }

        SldNodeType.LOAD -> {
            drawCircle(
                color = color,
                radius = 34f,
                center = Offset(x + 65f, y + 48f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "L",
                topLeft = Offset(x + 56f, y + 29f),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    if (selected || connectionStart) {
        drawRect(
            color = if (connectionStart) Accent else Selected,
            topLeft = Offset(x - 5f, y - 5f),
            size = Size(
                NODE_WIDTH + 10f,
                NODE_HEIGHT + 10f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(4f)
        )
    }

    drawText(
        textMeasurer = textMeasurer,
        text = node.name,
        topLeft = Offset(x, y + 91f),
        style = TextStyle(
            color = Primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    )

    val lines = when (node.type) {
        SldNodeType.SOURCE -> listOf(
            "V = ${fmt(node.voltage)} V",
            "Fault = ${fmt(node.sourceShortCircuitMva)} MVA"
        )

        SldNodeType.TRANSFORMER -> listOf(
            "S = ${fmt(node.ratedKva)} kVA",
            "Z = ${fmt(node.transformerPercentZ)} %"
        )

        SldNodeType.GENERATOR -> listOf(
            "S = ${fmt(node.ratedKva)} kVA",
            "Xd'' = ${fmt(node.generatorXdSubtransient)} %"
        )

        SldNodeType.BREAKER -> listOf(
            "BREAKER",
            "Icu / Ics → SC"
        )

        SldNodeType.BUS -> listOf(
            "V = ${fmt(node.voltage)} V",
            "Fault → SC"
        )

        SldNodeType.PANEL -> listOf(
            "V = ${fmt(node.voltage)} V",
            "S = ${fmt(node.ratedKva)} kVA"
        )

        SldNodeType.LOAD -> listOf(
            "P = ${fmt(node.loadKw)} kW",
            "PF = ${fmt(node.powerFactor)}"
        )
    }

    lines.forEachIndexed { index, line ->
        drawText(
            textMeasurer = textMeasurer,
            text = line,
            topLeft = Offset(
                x + 4f,
                y + 114f + index * 18f
            ),
            style = TextStyle(
                color = Secondary,
                fontSize = 12.sp
            )
        )
    }
}

private fun nodeColor(type: SldNodeType): Color {
    return when (type) {
        SldNodeType.SOURCE -> SourceColor
        SldNodeType.TRANSFORMER -> TransformerColor
        SldNodeType.GENERATOR -> GeneratorColor
        SldNodeType.BREAKER -> BreakerColor
        SldNodeType.BUS -> BusColor
        SldNodeType.PANEL -> PanelColor
        SldNodeType.LOAD -> LoadColor
    }
}

private fun findNode(
    position: Offset,
    nodes: List<SldNode>
): SldNode? {
    return nodes.asReversed().firstOrNull {
        position.x >= it.x - TOUCH &&
            position.x <= it.x + NODE_WIDTH + TOUCH &&
            position.y >= it.y - TOUCH &&
            position.y <= it.y + NODE_HEIGHT + TOUCH
    }
}

private fun findConnection(
    position: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {
    var best: SldConnection? = null
    var bestDistance = Float.MAX_VALUE

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

        val d1 = segmentDistance(
            position,
            start,
            Offset(midX, start.y)
        )

        val d2 = segmentDistance(
            position,
            Offset(midX, start.y),
            Offset(midX, end.y)
        )

        val d3 = segmentDistance(
            position,
            Offset(midX, end.y),
            end
        )

        val distance = min(d1, min(d2, d3))

        if (distance <= 28f && distance < bestDistance) {
            bestDistance = distance
            best = connection
        }
    }

    return best
}

private fun segmentDistance(
    p: Offset,
    a: Offset,
    b: Offset
): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val len = abx * abx + aby * aby

    if (len < 0.000001f) {
        return sqrt(
            (p.x - a.x) * (p.x - a.x) +
                (p.y - a.y) * (p.y - a.y)
        )
    }

    val t = (
        (p.x - a.x) * abx +
            (p.y - a.y) * aby
        ) / len

    val c = t.coerceIn(0f, 1f)

    val x = a.x + c * abx
    val y = a.y + c * aby

    return sqrt(
        (p.x - x) * (p.x - x) +
            (p.y - y) * (p.y - y)
    )
}

private fun fmt(value: Double): String {
    val v = if (abs(value) < 0.000001) 0.0 else value

    return if (abs(v) >= 1000.0) {
        "%.1f".format(v)
    } else {
        "%.3f".format(v)
            .trimEnd('0')
            .trimEnd('.')
    }
}

private fun buildShortCircuitReport(
    study: SldShortCircuitStudy,
    nodes: List<SldNode>,
    arabic: Boolean
): String {
    val b = StringBuilder()

    b.append(
        if (arabic) {
            "تقرير هندسي لحساب تيارات القصر\n"
        } else {
            "SHORT CIRCUIT ENGINEERING REPORT\n"
        }
    )

    b.append("========================================\n\n")

    b.append(
        if (arabic) "أقصى Ik'' = "
        else "Maximum Ik'' = "
    )
    b.append("${fmt(study.maximumFaultCurrentKa)} kA\n")

    b.append(
        if (arabic) "أقصى Ip = "
        else "Maximum Ip = "
    )
    b.append("${fmt(study.maximumPeakCurrentKa)} kA\n")

    b.append(
        if (arabic) "أقصى Fault MVA = "
        else "Maximum Fault MVA = "
    )
    b.append("${fmt(study.maximumFaultMva)} MVA\n\n")

    study.results.values.forEachIndexed { index, result ->
        val node = nodes.firstOrNull {
            it.id == result.nodeId
        }

        b.append("POINT ${index + 1}: ${result.nodeName}\n")
        b.append("----------------------------------------\n")

        b.append("Type: ${node?.type ?: "BUS"}\n")
        b.append("Voltage: ${fmt(result.voltageV)} V\n")
        b.append(
            "Ik'' initial symmetrical: " +
                "${fmt(result.initialSymmetricalCurrentKa)} kA\n"
        )
        b.append(
            "Ip peak: ${fmt(result.peakCurrentKa)} kA\n"
        )
        b.append(
            "Ith thermal: ${fmt(result.thermalCurrentKa)} kA\n"
        )
        b.append(
            "Fault MVA: ${fmt(result.shortCircuitMva)} MVA\n"
        )
        b.append(
            "R: ${fmt(result.resistanceOhm)} Ω\n"
        )
        b.append(
            "X: ${fmt(result.reactanceOhm)} Ω\n"
        )
        b.append(
            "Z: ${fmt(result.impedanceOhm)} Ω\n"
        )
        b.append(
            "X/R: ${fmt(result.xrRatio)}\n"
        )
        b.append(
            "Required breaker: " +
                "${fmt(result.breakerRequiredKa)} kA\n"
        )

        if (result.notes.isNotEmpty()) {
            b.append("Notes:\n")
            result.notes.forEach {
                b.append("• $it\n")
            }
        }

        b.append("\n")
    }

    if (study.notes.isNotEmpty()) {
        b.append("STUDY NOTES\n")
        b.append("----------------------------------------\n")
        study.notes.forEach {
            b.append("• $it\n")
        }
    }

    return b.toString()
}

private fun buildPanelSchedule(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    arabic: Boolean
): String {
    val b = StringBuilder()

    b.append("PANEL SCHEDULE\n")
    b.append("========================================\n")
    b.append("Panel: ${panel.name}\n")
    b.append("Voltage: ${fmt(panel.voltage)} V\n")
    b.append("Rating: ${fmt(panel.ratedKva)} kVA\n\n")

    b.append(
        "No | LOAD | kW | PF | DF | CABLE | RUNS\n"
    )
    b.append("----------------------------------------\n")

    val outgoing = connections.filter {
        it.fromNodeId == panel.id
    }

    if (outgoing.isEmpty()) {
        b.append(
            if (arabic) {
                "لا توجد دوائر خارجة متصلة باللوحة.\n"
            } else {
                "No outgoing circuits connected.\n"
            }
        )

        return b.toString()
    }

    outgoing.forEachIndexed { index, connection ->
        val load = nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return@forEachIndexed

        b.append(
            "${index + 1} | " +
                "${load.name} | " +
                "${fmt(load.loadKw)} | " +
                "${fmt(load.powerFactor)} | " +
                "${fmt(load.demandFactor)} | " +
                "${fmt(connection.cableSizeMm2)} mm² | " +
                "${connection.parallelRuns}\n"
        )
    }

    return b.toString()
}

@Composable
private fun NodeDialog(
    arabic: Boolean,
    type: SldNodeType,
    name: String,
    voltage: String,
    loadKw: String,
    pf: String,
    demand: String,
    kva: String,
    transformerZ: String,
    generatorXd: String,
    sourceMva: String,
    onName: (String) -> Unit,
    onVoltage: (String) -> Unit,
    onLoadKw: (String) -> Unit,
    onPf: (String) -> Unit,
    onDemand: (String) -> Unit,
    onKva: (String) -> Unit,
    onTransformerZ: (String) -> Unit,
    onGeneratorXd: (String) -> Unit,
    onSourceMva: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (arabic) {
                    "بيانات ${nodeTypeLabel(type, true)}"
                } else {
                    "${nodeTypeLabel(type, false)} Data"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState()
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onName,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (arabic) "الاسم" else "Name")
                    }
                )

                OutlinedTextField(
                    value = voltage,
                    onValueChange = onVoltage,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Voltage (V)")
                    }
                )

                when (type) {
                    SldNodeType.SOURCE -> {
                        OutlinedTextField(
                            value = sourceMva,
                            onValueChange = onSourceMva,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Source Fault Level (MVA)")
                            }
                        )
                    }

                    SldNodeType.TRANSFORMER -> {
                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKva,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Transformer Rating (kVA)")
                            }
                        )

                        OutlinedTextField(
                            value = transformerZ,
                            onValueChange = onTransformerZ,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Transformer %Z")
                            }
                        )
                    }

                    SldNodeType.GENERATOR -> {
                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKva,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Generator Rating (kVA)")
                            }
                        )

                        OutlinedTextField(
                            value = generatorXd,
                            onValueChange = onGeneratorXd,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Xd'' (%)")
                            }
                        )
                    }

                    SldNodeType.PANEL -> {
                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKva,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Panel Rating (kVA)")
                            }
                        )
                    }

                    SldNodeType.LOAD -> {
                        OutlinedTextField(
                            value = loadKw,
                            onValueChange = onLoadKw,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Load (kW)")
                            }
                        )

                        OutlinedTextField(
                            value = pf,
                            onValueChange = onPf,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Power Factor")
                            }
                        )

                        OutlinedTextField(
                            value = demand,
                            onValueChange = onDemand,
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Demand Factor")
                            }
                        )
                    }

                    SldNodeType.BUS,
                    SldNodeType.BREAKER -> {
                        Spacer(modifier = Modifier.height(1.dp))
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

private fun nodeTypeLabel(
    type: SldNodeType,
    arabic: Boolean
): String {
    return when (type) {
        SldNodeType.SOURCE ->
            if (arabic) "مصدر" else "Source"

        SldNodeType.TRANSFORMER ->
            if (arabic) "محول" else "Transformer"

        SldNodeType.GENERATOR ->
            if (arabic) "مولد" else "Generator"

        SldNodeType.BUS ->
            if (arabic) "باسبار" else "Busbar"

        SldNodeType.BREAKER ->
            if (arabic) "قاطع" else "Breaker"

        SldNodeType.PANEL ->
            if (arabic) "لوحة" else "Panel"

        SldNodeType.LOAD ->
            if (arabic) "حمل" else "Load"
    }
}

@Composable
private fun ConnectionDialog(
    arabic: Boolean,
    length: String,
    resistance: String,
    reactance: String,
    cableSize: String,
    parallelRuns: String,
    capacity: String,
    onLength: (String) -> Unit,
    onResistance: (String) -> Unit,
    onReactance: (String) -> Unit,
    onCableSize: (String) -> Unit,
    onParallelRuns: (String) -> Unit,
    onCapacity: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (arabic) {
                    "بيانات الكابل"
                } else {
                    "Cable / Connection Data"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState()
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = length,
                    onValueChange = onLength,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Length (m)")
                    }
                )

                OutlinedTextField(
                    value = resistance,
                    onValueChange = onResistance,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("R (Ω/km)")
                    }
                )

                OutlinedTextField(
                    value = reactance,
                    onValueChange = onReactance,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("X (Ω/km)")
                    }
                )

                OutlinedTextField(
                    value = cableSize,
                    onValueChange = onCableSize,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Cable Size (mm²)")
                    }
                )

                OutlinedTextField(
                    value = parallelRuns,
                    onValueChange = onParallelRuns,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Parallel Runs")
                    }
                )

                OutlinedTextField(
                    value = capacity,
                    onValueChange = onCapacity,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Current Capacity (A)")
                    }
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
