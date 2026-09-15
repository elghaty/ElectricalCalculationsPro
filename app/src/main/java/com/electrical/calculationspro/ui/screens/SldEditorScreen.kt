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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
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

    fun network(): SldNetwork {
        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }

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

    fun openNewNode(type: SldNodeType) {
        editingNodeId = null
        nodeType = type
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
        val powerFactor = pf.toDoubleOrNull()?.coerceIn(0.01, 1.0) ?: 0.90
        val demandFactor = demand.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
        val rated = kva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val z = transformerZ.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val xd = generatorXd.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val mva = sourceMva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val editId = editingNodeId

        if (editId == null) {
            val index = nodes.size

            val newNode = SldNode(
                id = "node-${System.nanoTime()}",
                name = name.ifBlank { nextName(nodeType) },
                type = nodeType,
                x = 100f + index * 210f,
                y = 280f,
                voltage = v,
                loadKw = kw,
                powerFactor = powerFactor,
                demandFactor = demandFactor,
                ratedKva = rated,
                transformerPercentZ = z,
                generatorXdSubtransient = xd,
                sourceShortCircuitMva = mva
            )

            nodes = nodes + newNode
            selectedNodeId = newNode.id
            selectedConnectionId = null
        } else {
            nodes = nodes.map {
                if (it.id == editId) {
                    it.copy(
                        name = name.ifBlank { it.name },
                        type = nodeType,
                        voltage = v,
                        loadKw = kw,
                        powerFactor = powerFactor,
                        demandFactor = demandFactor,
                        ratedKva = rated,
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
        val size = cableSize.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val runs = parallelRuns.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val ampacity = capacity.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        connections = connections.map {
            if (it.id == id) {
                it.copy(
                    lengthMeters = l,
                    resistanceOhmPerKm = r,
                    reactanceOhmPerKm = x,
                    cableSizeMm2 = size,
                    parallelRuns = runs,
                    currentCapacityA = ampacity
                )
            } else {
                it
            }
        }

        showConnectionDialog = false
    }

    fun connectSelected() {
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

        val alreadyExists = connections.any {
            (it.fromNodeId == from && it.toNodeId == selected) ||
                (it.fromNodeId == selected && it.toNodeId == from)
        }

        if (!alreadyExists) {
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

    fun runShortCircuit() {
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
            reportTitle = if (arabic) {
                "خطأ في الحساب"
            } else {
                "CALCULATION ERROR"
            }

            reportText = e.message ?: "Calculation failed."
            showReport = true
        }
    }

    fun runPanelSchedule() {
        val panel = nodes.firstOrNull {
            it.id == selectedNodeId &&
                it.type == SldNodeType.PANEL
        }

        reportTitle = if (arabic) {
            "جدول اللوحة"
        } else {
            "PANEL SCHEDULE"
        }

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

        val sourceNode = source.copy(
            id = "source-1",
            name = if (source.name.isBlank()) "MAIN SOURCE" else source.name,
            x = 80f,
            y = 280f
        )

        val breaker1 = SldNode(
            id = "auto-cb-1",
            name = "MAIN ACB",
            type = SldNodeType.BREAKER,
            x = 300f,
            y = 280f,
            voltage = sourceNode.voltage
        )

        val transformer = SldNode(
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

        val breaker2 = SldNode(
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

        val load1 = SldNode(
            id = "auto-load-1",
            name = "LOAD-01",
            type = SldNodeType.LOAD,
            x = 1430f,
            y = 100f,
            voltage = 400.0,
            loadKw = 100.0,
            powerFactor = 0.90,
            demandFactor = 1.0
        )

        val load2 = SldNode(
            id = "auto-load-2",
            name = "LOAD-02",
            type = SldNodeType.LOAD,
            x = 1430f,
            y = 280f,
            voltage = 400.0,
            loadKw = 150.0,
            powerFactor = 0.90,
            demandFactor = 0.85
        )

        val load3 = SldNode(
            id = "auto-load-3",
            name = "LOAD-03",
            type = SldNodeType.LOAD,
            x = 1430f,
            y = 460f,
            voltage = 400.0,
            loadKw = 200.0,
            powerFactor = 0.90,
            demandFactor = 0.80
        )

        nodes = listOf(
            sourceNode,
            breaker1,
            transformer,
            bus,
            breaker2,
            panel,
            load1,
            load2,
            load3
        )

        connections = listOf(
            SldConnection(
                id = "auto-c-1",
                fromNodeId = sourceNode.id,
                toNodeId = breaker1.id,
                lengthMeters = 1.0,
                resistanceOhmPerKm = 0.01,
                reactanceOhmPerKm = 0.01,
                cableSizeMm2 = 240.0,
                parallelRuns = 1,
                currentCapacityA = 630.0
            ),
            SldConnection(
                id = "auto-c-2",
                fromNodeId = breaker1.id,
                toNodeId = transformer.id,
                lengthMeters = 5.0,
                resistanceOhmPerKm = 0.01,
                reactanceOhmPerKm = 0.01,
                cableSizeMm2 = 0.0,
                parallelRuns = 1,
                currentCapacityA = 0.0
            ),
            SldConnection(
                id = "auto-c-3",
                fromNodeId = transformer.id,
                toNodeId = bus.id,
                lengthMeters = 2.0,
                resistanceOhmPerKm = 0.01,
                reactanceOhmPerKm = 0.01,
                cableSizeMm2 = 240.0,
                parallelRuns = 1,
                currentCapacityA = 630.0
            ),
            SldConnection(
                id = "auto-c-4",
                fromNodeId = bus.id,
                toNodeId = breaker2.id,
                lengthMeters = 1.0,
                resistanceOhmPerKm = 0.01,
                reactanceOhmPerKm = 0.01,
                cableSizeMm2 = 240.0,
                parallelRuns = 1,
                currentCapacityA = 630.0
            ),
            SldConnection(
                id = "auto-c-5",
                fromNodeId = breaker2.id,
                toNodeId = panel.id,
                lengthMeters = 5.0,
                resistanceOhmPerKm = 0.01,
                reactanceOhmPerKm = 0.01,
                cableSizeMm2 = 240.0,
                parallelRuns = 1,
                currentCapacityA = 630.0
            ),
            SldConnection(
                id = "auto-c-6",
                fromNodeId = panel.id,
                toNodeId = load1.id,
                lengthMeters = 30.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 70.0,
                parallelRuns = 1,
                currentCapacityA = 180.0
            ),
            SldConnection(
                id = "auto-c-7",
                fromNodeId = panel.id,
                toNodeId = load2.id,
                lengthMeters = 40.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 95.0,
                parallelRuns = 1,
                currentCapacityA = 220.0
            ),
            SldConnection(
                id = "auto-c-8",
                fromNodeId = panel.id,
                toNodeId = load3.id,
                lengthMeters = 50.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 120.0,
                parallelRuns = 1,
                currentCapacityA = 250.0
            )
        )

        selectedNodeId = bus.id
        selectedConnectionId = null
        connectionStartId = null

        runShortCircuit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            onBack?.let {
                Button(onClick = it) {
                    Text(if (arabic) "رجوع" else "Back")
                }
            }

            Button(
                onClick = { openNewNode(SldNodeType.SOURCE) }
            ) {
                Text("Source")
            }

            Button(
                onClick = { openNewNode(SldNodeType.TRANSFORMER) }
            ) {
                Text("Transformer")
            }

            Button(
                onClick = { openNewNode(SldNodeType.GENERATOR) }
            ) {
                Text("Generator")
            }

            Button(
                onClick = { openNewNode(SldNodeType.BUS) }
            ) {
                Text("Bus")
            }

            Button(
                onClick = { openNewNode(SldNodeType.BREAKER) }
            ) {
                Text("Breaker")
            }

            Button(
                onClick = { openNewNode(SldNodeType.PANEL) }
            ) {
                Text("Panel")
            }

            Button(
                onClick = { openNewNode(SldNodeType.LOAD) }
            ) {
                Text("Load")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { connectSelected() }
            ) {
                Text(
                    if (connectionStartId == null) {
                        "Connect"
                    } else {
                        "Select End"
                    }
                )
            }

            Button(
                onClick = { deleteSelected() }
            ) {
                Text("Delete")
            }

            Button(
                onClick = { runShortCircuit() }
            ) {
                Text("Short Circuit")
            }

            Button(
                onClick = { runPanelSchedule() }
            ) {
                Text("Panel Schedule")
            }

            Button(
                onClick = { generateCompleteSld() }
            ) {
                Text("Generate Complete SLD")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
            onMoveNode = { id, dx, dy ->
                nodes = nodes.map { node ->
                    if (node.id == id) {
                        node.copy(
                            x = max(20f, node.x + dx),
                            y = max(20f, node.y + dy)
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
            onEditNode = { node ->
                editNode(node)
            },
            onEditConnection = { connection ->
                editConnection(connection)
            }
        )
    }

    if (showNodeDialog) {
        NodeEditorDialog(
            arabic = arabic,
            editing = editingNodeId != null,
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
            onNameChange = { name = it },
            onVoltageChange = { voltage = it },
            onLoadKwChange = { loadKw = it },
            onPfChange = { pf = it },
            onDemandChange = { demand = it },
            onKvaChange = { kva = it },
            onTransformerZChange = { transformerZ = it },
            onGeneratorXdChange = { generatorXd = it },
            onSourceMvaChange = { sourceMva = it },
            onSave = { saveNode() },
            onCancel = {
                showNodeDialog = false
            }
        )
    }

    if (showConnectionDialog) {
        ConnectionEditorDialog(
            arabic = arabic,
            length = length,
            resistance = resistance,
            reactance = reactance,
            cableSize = cableSize,
            parallelRuns = parallelRuns,
            capacity = capacity,
            onLengthChange = { length = it },
            onResistanceChange = { resistance = it },
            onReactanceChange = { reactance = it },
            onCableSizeChange = { cableSize = it },
            onParallelRunsChange = { parallelRuns = it },
            onCapacityChange = { capacity = it },
            onSave = { saveConnection() },
            onCancel = {
                showConnectionDialog = false
            }
        )
    }

    if (showReport) {
        ReportDialog(
            title = reportTitle,
            text = reportText,
            onClose = {
                showReport = false
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
    onMoveNode: (String, Float, Float) -> Unit,
    onSelectConnection: (String) -> Unit,
    onEditNode: (SldNode) -> Unit,
    onEditConnection: (SldConnection) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    val currentNodes by rememberUpdatedState(nodes)
    val currentConnections by rememberUpdatedState(connections)
    val currentSelectNode by rememberUpdatedState(onSelectNode)
    val currentMoveNode by rememberUpdatedState(onMoveNode)
    val currentSelectConnection by rememberUpdatedState(onSelectConnection)
    val currentEditNode by rememberUpdatedState(onEditNode)
    val currentEditConnection by rememberUpdatedState(onEditConnection)

    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontal)
            .verticalScroll(vertical)
            .background(CanvasBackground)
    ) {
        Canvas(
            modifier = Modifier
                .width(2300.dp)
                .height(1000.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { position ->
                            findNode(
                                position,
                                currentNodes
                            )?.let {
                                currentEditNode(it)
                                return@detectTapGestures
                            }

                            findConnection(
                                position,
                                currentNodes,
                                currentConnections
                            )?.let {
                                currentEditConnection(it)
                            }
                        },
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
                                currentConnections
                            )

                            if (connection != null) {
                                currentSelectConnection(connection.id)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    var draggedNodeId: String? = null

                    detectDragGestures(
                        onDragStart = { position ->
                            draggedNodeId = findNode(
                                position,
                                currentNodes
                            )?.id
                        },
                        onDragCancel = {
                            draggedNodeId = null
                        },
                        onDragEnd = {
                            draggedNodeId = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            val id = draggedNodeId

                            if (id != null) {
                                currentMoveNode(
                                    id,
                                    dragAmount.x,
                                    dragAmount.y
                                )
                            }
                        }
                    )
                }
        ) {
            connections.forEach { connection ->
                drawConnection(
                    connection = connection,
                    nodes = nodes,
                    selected = connection.id == selectedConnectionId,
                    textMeasurer = textMeasurer
                )
            }

            nodes.forEach { node ->
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

private fun DrawScope.drawConnection(
    connection: SldConnection,
    nodes: List<SldNode>,
    selected: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val from = nodes.firstOrNull {
        it.id == connection.fromNodeId
    } ?: return

    val to = nodes.firstOrNull {
        it.id == connection.toNodeId
    } ?: return

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
        color = if (selected) Accent else CableColor,
        style = Stroke(
            width = if (selected) 7f else 4f
        )
    )

    drawLine(
        color = if (selected) Accent else CableColor,
        start = Offset(end.x - 18f, end.y - 9f),
        end = end,
        strokeWidth = 4f
    )

    drawLine(
        color = if (selected) Accent else CableColor,
        start = Offset(end.x - 18f, end.y + 9f),
        end = end,
        strokeWidth = 4f
    )

    val label = if (connection.cableSizeMm2 > 0.0) {
        "${fmt(connection.cableSizeMm2)} mm² × ${connection.parallelRuns}"
    } else {
        "CABLE"
    }

    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(
            midX - 45f,
            min(start.y, end.y) - 28f
        ),
        style = TextStyle(
            color = Secondary,
            fontSize = 13.sp
        )
    )
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
                radius = 38f,
                center = Offset(x + 65f, y + 48f),
                style = Stroke(6f)
            )

            drawLine(
                color = color,
                start = Offset(x + 65f, y + 10f),
                end = Offset(x + 65f, y + 86f),
                strokeWidth = 4f
            )

            drawLine(
                color = color,
                start = Offset(x + 27f, y + 48f),
                end = Offset(x + 103f, y + 48f),
                strokeWidth = 4f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawCircle(
                color = color,
                radius = 29f,
                center = Offset(x + 48f, y + 48f),
                style = Stroke(6f)
            )

            drawCircle(
                color = color,
                radius = 29f,
                center = Offset(x + 91f, y + 48f),
                style = Stroke(6f)
            )
        }

        SldNodeType.GENERATOR -> {
            drawCircle(
                color = color,
                radius = 38f,
                center = Offset(x + 65f, y + 48f),
                style = Stroke(6f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "G",
                topLeft = Offset(x + 55f, y + 29f),
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
                style = Stroke(6f)
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
            style = Stroke(4f)
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
            "FAULT → SC"
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

        val distance = min(
            d1,
            min(d2, d3)
        )

        if (distance < bestDistance) {
            bestDistance = distance
            best = connection
        }
    }

    return if (bestDistance <= 25f) best else null
}

private fun segmentDistance(
    p: Offset,
    a: Offset,
    b: Offset
): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y

    if (dx == 0f && dy == 0f) {
        return sqrt(
            (p.x - a.x) * (p.x - a.x) +
                (p.y - a.y) * (p.y - a.y)
        )
    }

    val t = (
        (p.x - a.x) * dx +
            (p.y - a.y) * dy
        ) / (dx * dx + dy * dy)

    val clamped = max(
        0f,
        min(1f, t)
    )

    val x = a.x + clamped * dx
    val y = a.y + clamped * dy

    return sqrt(
        (p.x - x) * (p.x - x) +
            (p.y - y) * (p.y - y)
    )
}

@Composable
private fun NodeEditorDialog(
    arabic: Boolean,
    editing: Boolean,
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
    onNameChange: (String) -> Unit,
    onVoltageChange: (String) -> Unit,
    onLoadKwChange: (String) -> Unit,
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
                if (editing) {
                    if (arabic) "تعديل العنصر" else "Edit Element"
                } else {
                    if (arabic) "إضافة عنصر" else "Add Element"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = type.name,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text("Name")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = voltage,
                    onValueChange = onVoltageChange,
                    label = {
                        Text("Voltage (V)")
                    },
                    singleLine = true
                )

                when (type) {
                    SldNodeType.SOURCE -> {
                        OutlinedTextField(
                            value = sourceMva,
                            onValueChange = onSourceMvaChange,
                            label = {
                                Text("Source Fault Level (MVA)")
                            },
                            singleLine = true
                        )
                    }

                    SldNodeType.TRANSFORMER -> {
                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            label = {
                                Text("Transformer Rating (kVA)")
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = transformerZ,
                            onValueChange = onTransformerZChange,
                            label = {
                                Text("Transformer %Z")
                            },
                            singleLine = true
                        )
                    }

                    SldNodeType.GENERATOR -> {
                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            label = {
                                Text("Generator Rating (kVA)")
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = generatorXd,
                            onValueChange = onGeneratorXdChange,
                            label = {
                                Text("Xd'' (%)")
                            },
                            singleLine = true
                        )
                    }

                    SldNodeType.PANEL -> {
                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            label = {
                                Text("Panel Rating (kVA)")
                            },
                            singleLine = true
                        )
                    }

                    SldNodeType.LOAD -> {
                        OutlinedTextField(
                            value = loadKw,
                            onValueChange = onLoadKwChange,
                            label = {
                                Text("Load (kW)")
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = pf,
                            onValueChange = onPfChange,
                            label = {
                                Text("Power Factor")
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = demand,
                            onValueChange = onDemandChange,
                            label = {
                                Text("Demand Factor")
                            },
                            singleLine = true
                        )
                    }

                    SldNodeType.BUS,
                    SldNodeType.BREAKER -> Unit
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
private fun ConnectionEditorDialog(
    arabic: Boolean,
    length: String,
    resistance: String,
    reactance: String,
    cableSize: String,
    parallelRuns: String,
    capacity: String,
    onLengthChange: (String) -> Unit,
    onResistanceChange: (String) -> Unit,
    onReactanceChange: (String) -> Unit,
    onCableSizeChange: (String) -> Unit,
    onParallelRunsChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
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
                    "Cable Data"
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
                    onValueChange = onLengthChange,
                    label = {
                        Text("Length (m)")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = resistance,
                    onValueChange = onResistanceChange,
                    label = {
                        Text("R (Ω/km)")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = reactance,
                    onValueChange = onReactanceChange,
                    label = {
                        Text("X (Ω/km)")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = cableSize,
                    onValueChange = onCableSizeChange,
                    label = {
                        Text("Cable Section (mm²)")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = parallelRuns,
                    onValueChange = onParallelRunsChange,
                    label = {
                        Text("Parallel Runs")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = capacity,
                    onValueChange = onCapacityChange,
                    label = {
                        Text("Current Capacity (A)")
                    },
                    singleLine = true
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
private fun ReportDialog(
    title: String,
    text: String,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(title)
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    Text(
                        text = text,
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = Primary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

private fun buildShortCircuitReport(
    study: SldShortCircuitStudy,
    nodes: List<SldNode>,
    arabic: Boolean
): String {
    val result = StringBuilder()

    if (arabic) {
        result.appendLine("تقرير هندسي لحساب تيارات القصر")
        result.appendLine("================================")
        result.appendLine()
        result.appendLine(
            "أقصى تيار قصر متماثل: " +
                "${fmt(study.maximumFaultCurrentKa)} kA"
        )
        result.appendLine(
            "أقصى تيار Peak: " +
                "${fmt(study.maximumPeakCurrentKa)} kA"
        )
        result.appendLine(
            "أقصى قدرة قصر: " +
                "${fmt(study.maximumFaultMva)} MVA"
        )
        result.appendLine()

        study.results.values.forEachIndexed { index, item ->
            result.appendLine(
                "${index + 1}. ${item.nodeName}"
            )
            result.appendLine(
                "   الجهد = ${fmt(item.voltageV)} V"
            )
            result.appendLine(
                "   Ik'' = ${fmt(item.initialSymmetricalCurrentKa)} kA"
            )
            result.appendLine(
                "   Ip = ${fmt(item.peakCurrentKa)} kA"
            )
            result.appendLine(
                "   Ith = ${fmt(item.thermalCurrentKa)} kA"
            )
            result.appendLine(
                "   Fault MVA = ${fmt(item.shortCircuitMva)} MVA"
            )
            result.appendLine(
                "   X/R = ${fmt(item.xrRatio)}"
            )
            result.appendLine(
                "   Breaker Required = " +
                    "${fmt(item.breakerRequiredKa)} kA"
            )

            if (item.notes.isNotBlank()) {
                result.appendLine(
                    "   ملاحظات: ${item.notes}"
                )
            }

            result.appendLine()
        }
    } else {
        result.appendLine("SHORT CIRCUIT ENGINEERING REPORT")
        result.appendLine("================================")
        result.appendLine()
        result.appendLine(
            "Maximum symmetrical fault current: " +
                "${fmt(study.maximumFaultCurrentKa)} kA"
        )
        result.appendLine(
            "Maximum peak current: " +
                "${fmt(study.maximumPeakCurrentKa)} kA"
        )
        result.appendLine(
            "Maximum fault level: " +
                "${fmt(study.maximumFaultMva)} MVA"
        )
        result.appendLine()

        study.results.values.forEachIndexed { index, item ->
            result.appendLine(
                "${index + 1}. ${item.nodeName}"
            )
            result.appendLine(
                "   Voltage = ${fmt(item.voltageV)} V"
            )
            result.appendLine(
                "   Ik'' = ${fmt(item.initialSymmetricalCurrentKa)} kA"
            )
            result.appendLine(
                "   Ip = ${fmt(item.peakCurrentKa)} kA"
            )
            result.appendLine(
                "   Ith = ${fmt(item.thermalCurrentKa)} kA"
            )
            result.appendLine(
                "   Fault MVA = ${fmt(item.shortCircuitMva)} MVA"
            )
            result.appendLine(
                "   X/R = ${fmt(item.xrRatio)}"
            )
            result.appendLine(
                "   Breaker Required = " +
                    "${fmt(item.breakerRequiredKa)} kA"
            )

            if (item.notes.isNotBlank()) {
                result.appendLine(
                    "   Notes: ${item.notes}"
                )
            }

            result.appendLine()
        }
    }

    return result.toString()
}

private fun buildPanelSchedule(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    arabic: Boolean
): String {
    val result = StringBuilder()

    val outgoing = connections.filter {
        it.fromNodeId == panel.id ||
            it.toNodeId == panel.id
    }

    if (arabic) {
        result.appendLine("PANEL SCHEDULE")
        result.appendLine("==============================")
        result.appendLine("Panel: ${panel.name}")
        result.appendLine("Voltage: ${fmt(panel.voltage)} V")
        result.appendLine(
            "Rating: ${fmt(panel.ratedKva)} kVA"
        )
        result.appendLine()
        result.appendLine(
            "No | Load | kW | PF | Cable | Runs | Capacity"
        )
        result.appendLine(
            "----------------------------------------------"
        )
    } else {
        result.appendLine("PANEL SCHEDULE")
        result.appendLine("==============================")
        result.appendLine("Panel: ${panel.name}")
        result.appendLine("Voltage: ${fmt(panel.voltage)} V")
        result.appendLine(
            "Rating: ${fmt(panel.ratedKva)} kVA"
        )
        result.appendLine()
        result.appendLine(
            "No | Load | kW | PF | Cable | Runs | Capacity"
        )
        result.appendLine(
            "----------------------------------------------"
        )
    }

    var counter = 1

    outgoing.forEach { connection ->
        val load = if (connection.fromNodeId == panel.id) {
            nodes.firstOrNull {
                it.id == connection.toNodeId
            }
        } else {
            nodes.firstOrNull {
                it.id == connection.fromNodeId
            }
        }

        if (load?.type == SldNodeType.LOAD) {
            result.appendLine(
                "$counter | ${load.name} | " +
                    "${fmt(load.loadKw)} | " +
                    "${fmt(load.powerFactor)} | " +
                    "${fmt(connection.cableSizeMm2)} | " +
                    "${connection.parallelRuns} | " +
                    "${fmt(connection.currentCapacityA)} A"
            )

            counter++
        }
    }

    if (counter == 1) {
        result.appendLine(
            if (arabic) {
                "لا توجد أحمال متصلة باللوحة."
            } else {
                "No loads connected to this panel."
            }
        )
    }

    return result.toString()
}

private fun fmt(value: Double): String {
    return if (value.isFinite()) {
        if (abs(value) >= 1000.0) {
            String.format("%.1f", value)
        } else {
            String.format("%.3f", value)
        }
    } else {
        "0"
    }
}
