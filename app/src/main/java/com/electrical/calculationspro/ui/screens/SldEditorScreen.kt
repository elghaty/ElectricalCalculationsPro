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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldCableSizingEngine
import com.electrical.calculationspro.data.SldCableSizingResult
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringEngine
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldPanelScheduleEngine
import com.electrical.calculationspro.data.SldProtectionCoordinationEngine
import com.electrical.calculationspro.data.SldShortCircuitEngine
import com.electrical.calculationspro.data.SldShortCircuitResult
import com.electrical.calculationspro.data.SldShortCircuitStudy
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
private val Danger = Color(0xFFE53935)
private val Success = Color(0xFF43A047)
private val CableColor = Color(0xFFB0BEC5)

private const val NODE_WIDTH = 150f
private const val NODE_HEIGHT = 92f
private const val NODE_TOUCH_PADDING = 35f

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
                    y = 100f,
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

    var showCompleteDialog by remember {
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
            SldNodeType.TRANSFORMER -> "TR"
            SldNodeType.GENERATOR -> "GEN"
            SldNodeType.BUS -> "BUS"
            SldNodeType.PANEL -> "PANEL"
            SldNodeType.BREAKER -> "CB"
            SldNodeType.LOAD -> "LOAD"
        }

        return "$prefix-${nodes.count { it.type == type } + 1}"
    }

    fun openAdd(type: SldNodeType) {
        editingNodeId = null
        pendingNodeType = type

        nodeName = defaultName(type)
        nodeVoltage = when (type) {
            SldNodeType.SOURCE,
            SldNodeType.TRANSFORMER,
            SldNodeType.GENERATOR,
            SldNodeType.BUS,
            SldNodeType.PANEL,
            SldNodeType.BREAKER -> "400"

            SldNodeType.LOAD -> "400"
        }

        nodeKw = if (type == SldNodeType.LOAD) "50" else "0"
        nodePf = "0.90"
        nodeDemand = "1.0"
        nodeKva = when (type) {
            SldNodeType.TRANSFORMER -> "1000"
            SldNodeType.GENERATOR -> "500"
            else -> "0"
        }
        nodeTransformerZ = "6"
        nodeGeneratorXd = "15"
        nodeSourceMva = "500"

        showNodeDialog = true
    }

    fun openEdit(node: SldNode) {
        editingNodeId = node.id
        pendingNodeType = node.type

        nodeName = node.name
        nodeVoltage = formatNumber(node.voltage)
        nodeKw = formatNumber(node.loadKw)
        nodePf = formatNumber(node.powerFactor)
        nodeDemand = formatNumber(node.demandFactor)
        nodeKva = formatNumber(node.ratedKva)
        nodeTransformerZ = formatNumber(node.transformerPercentZ)
        nodeGeneratorXd = formatNumber(node.generatorXdSubtransient)
        nodeSourceMva = formatNumber(node.sourceShortCircuitMva)

        showNodeDialog = true
    }

    fun openConnection(connection: SldConnection) {
        editingConnectionId = connection.id

        connectionLength = formatNumber(connection.lengthMeters)
        connectionResistance = formatNumber(connection.resistanceOhmPerKm)
        connectionReactance = formatNumber(connection.reactanceOhmPerKm)
        connectionCableSize = formatNumber(connection.cableSizeMm2)
        connectionParallelRuns = connection.parallelRuns.toString()
        connectionCurrentCapacity = formatNumber(connection.currentCapacityA)

        showConnectionDialog = true
    }

    fun saveNode() {
        val voltage =
            nodeVoltage.toDoubleOrNull()
                ?.coerceAtLeast(1.0)
                ?: 400.0

        val kw =
            nodeKw.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val pf =
            (nodePf.toDoubleOrNull() ?: 0.90)
                .coerceIn(0.01, 1.0)

        val demand =
            (nodeDemand.toDoubleOrNull() ?: 1.0)
                .coerceIn(0.0, 1.0)

        val kva =
            nodeKva.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val transformerZ =
            nodeTransformerZ.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val generatorXd =
            nodeGeneratorXd.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val sourceMva =
            nodeSourceMva.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        if (editingNodeId == null) {
            val newNode = SldNode(
                id = "node-${System.currentTimeMillis()}",
                name = nodeName.ifBlank {
                    defaultName(pendingNodeType)
                },
                type = pendingNodeType,
                x = (nodes.maxOfOrNull { it.x } ?: 80f) + 220f,
                y = nodes.lastOrNull()?.y ?: 100f,
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
            connectionLength.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val resistance =
            connectionResistance.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val reactance =
            connectionReactance.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val cableSize =
            connectionCableSize.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val runs =
            (connectionParallelRuns.toIntOrNull() ?: 1)
                .coerceAtLeast(1)

        val capacity =
            connectionCurrentCapacity.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

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

    fun runCompleteStudy() {
        try {
            val currentNetwork = network()

            val upstream =
                SldEngineeringEngine.calculateUpstream(
                    currentNetwork
                )

            val shortCircuit =
                SldShortCircuitEngine.calculate(
                    currentNetwork
                )

            val cableSizing =
                SldCableSizingEngine.calculate(
                    network = currentNetwork,
                    shortCircuitStudy = shortCircuit
                )

            val protection =
                SldProtectionCoordinationEngine.calculate(
                    network = currentNetwork,
                    shortCircuitStudy = shortCircuit,
                    cableSizingStudy = cableSizing
                )

            val panel =
                selectedNodeId?.let { panelId ->
                    currentNetwork.nodes
                        .firstOrNull { it.id == panelId }
                        ?.takeIf { it.type == SldNodeType.PANEL }
                        ?.let {
                            SldPanelScheduleEngine.calculate(
                                network = currentNetwork,
                                panelNodeId = it.id,
                                cableSizingStudy = cableSizing,
                                shortCircuitStudy = shortCircuit
                            )
                        }
                }

            resultTitle =
                if (arabic) {
                    "التقرير الهندسي الكامل"
                } else {
                    "Complete Engineering Study"
                }

            resultText =
                buildCompleteReport(
                    network = currentNetwork,
                    shortCircuit = shortCircuit,
                    cableSizing = cableSizing,
                    panelText = panel?.let {
                        buildPanelScheduleReport(it)
                    },
                    upstreamText = buildUpstreamReport(upstream),
                    protectionText = protection.toEngineeringText()
                )

            showResultDialog = true
        } catch (e: Exception) {
            resultTitle =
                if (arabic) "خطأ في الحسابات" else "Calculation Error"

            resultText =
                e.message
                    ?: if (arabic) {
                        "تعذر تنفيذ الدراسة."
                    } else {
                        "Unable to complete the study."
                    }

            showResultDialog = true
        }
    }

    fun generateCompleteSld() {
        val source =
            nodes.firstOrNull {
                it.type == SldNodeType.SOURCE
            } ?: SldNode(
                id = "source-1",
                name = "MAIN SOURCE",
                type = SldNodeType.SOURCE,
                x = 80f,
                y = 110f,
                voltage = 400.0,
                sourceShortCircuitMva = 500.0
            )

        val sourceVoltage = source.voltage.coerceAtLeast(1.0)
        val sourceMva = source.sourceShortCircuitMva.coerceAtLeast(1.0)

        val generatedNodes = listOf(
            source.copy(
                x = 60f,
                y = 180f,
                name = source.name.ifBlank { "MAIN SOURCE" }
            ),

            SldNode(
                id = "auto-cb-1",
                name = "CB-MAIN",
                type = SldNodeType.BREAKER,
                x = 270f,
                y = 180f,
                voltage = sourceVoltage
            ),

            SldNode(
                id = "auto-tr-1",
                name = "TR-1",
                type = SldNodeType.TRANSFORMER,
                x = 480f,
                y = 180f,
                voltage = sourceVoltage,
                ratedKva = 1000.0,
                transformerPercentZ = 6.0
            ),

            SldNode(
                id = "auto-bus-1",
                name = "BUS-1",
                type = SldNodeType.BUS,
                x = 690f,
                y = 180f,
                voltage = sourceVoltage
            ),

            SldNode(
                id = "auto-cb-2",
                name = "CB-PANEL",
                type = SldNodeType.BREAKER,
                x = 900f,
                y = 180f,
                voltage = sourceVoltage
            ),

            SldNode(
                id = "auto-panel-1",
                name = "MDB-1",
                type = SldNodeType.PANEL,
                x = 1110f,
                y = 180f,
                voltage = sourceVoltage
            ),

            SldNode(
                id = "auto-load-1",
                name = "LOAD-1",
                type = SldNodeType.LOAD,
                x = 1320f,
                y = 90f,
                voltage = sourceVoltage,
                loadKw = 100.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            ),

            SldNode(
                id = "auto-load-2",
                name = "LOAD-2",
                type = SldNodeType.LOAD,
                x = 1320f,
                y = 260f,
                voltage = sourceVoltage,
                loadKw = 75.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            )
        )

        val generatedConnections = listOf(
            SldConnection(
                id = "auto-c-1",
                fromNodeId = "auto-cb-1".let {
                    source.id
                },
                toNodeId = "auto-cb-1",
                lengthMeters = 5.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 240.0,
                parallelRuns = 2,
                currentCapacityA = 800.0
            ),

            SldConnection(
                id = "auto-c-2",
                fromNodeId = "auto-cb-1",
                toNodeId = "auto-tr-1",
                lengthMeters = 10.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 240.0,
                parallelRuns = 2,
                currentCapacityA = 800.0
            ),

            SldConnection(
                id = "auto-c-3",
                fromNodeId = "auto-tr-1",
                toNodeId = "auto-bus-1",
                lengthMeters = 8.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 240.0,
                parallelRuns = 2,
                currentCapacityA = 800.0
            ),

            SldConnection(
                id = "auto-c-4",
                fromNodeId = "auto-bus-1",
                toNodeId = "auto-cb-2",
                lengthMeters = 5.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 185.0,
                parallelRuns = 2,
                currentCapacityA = 690.0
            ),

            SldConnection(
                id = "auto-c-5",
                fromNodeId = "auto-cb-2",
                toNodeId = "auto-panel-1",
                lengthMeters = 5.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 185.0,
                parallelRuns = 2,
                currentCapacityA = 690.0
            ),

            SldConnection(
                id = "auto-c-6",
                fromNodeId = "auto-panel-1",
                toNodeId = "auto-load-1",
                lengthMeters = 30.0,
                resistanceOhmPerKm = 0.727,
                reactanceOhmPerKm = 0.073,
                cableSizeMm2 = 25.0,
                parallelRuns = 1,
                currentCapacityA = 101.0
            ),

            SldConnection(
                id = "auto-c-7",
                fromNodeId = "auto-panel-1",
                toNodeId = "auto-load-2",
                lengthMeters = 35.0,
                resistanceOhmPerKm = 0.727,
                reactanceOhmPerKm = 0.073,
                cableSizeMm2 = 25.0,
                parallelRuns = 1,
                currentCapacityA = 101.0
            )
        )

        nodes = generatedNodes
        connections = generatedConnections
        selectedNodeId = "auto-panel-1"
        selectedConnectionId = null
        connectionStartId = null

        val finalNetwork = SldNetwork(
            nodes = generatedNodes,
            connections = generatedConnections
        )

        try {
            val sc =
                SldShortCircuitEngine.calculate(
                    finalNetwork
                )

            resultTitle =
                if (arabic) {
                    "SLD كامل تم إنشاؤه"
                } else {
                    "Complete SLD Generated"
                }

            resultText =
                buildCompleteReport(
                    network = finalNetwork,
                    shortCircuit = sc,
                    cableSizing =
                        SldCableSizingEngine.calculate(
                            network = finalNetwork,
                            shortCircuitStudy = sc
                        ),
                    panelText = null,
                    upstreamText = null,
                    protectionText = null
                )

            showCompleteDialog = true
        } catch (e: Exception) {
            resultTitle =
                if (arabic) "تم إنشاء SLD" else "SLD Generated"

            resultText =
                if (arabic) {
                    "تم إنشاء المخطط. أعد تشغيل الحسابات لإظهار الدراسة الكاملة."
                } else {
                    "The SLD was generated. Run the engineering study to display the complete calculation."
                }

            showCompleteDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 10.dp,
                    bottom = 4.dp
                ),
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
                    "المخطط الأحادي الاحترافي SLD"
                } else {
                    "Professional Single Line Diagram"
                },
                color = PrimaryText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    if (arabic) {
                        "${nodes.size} عنصر"
                    } else {
                        "${nodes.size} elements"
                    },
                color = SecondaryText,
                fontSize = 12.sp
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
                text = if (arabic) "توليد SLD كامل" else "Generate Complete SLD",
                accent = true
            ) {
                generateCompleteSld()
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
                text = if (arabic) "Busbar" else "Busbar"
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButton(
                text = if (arabic) "ربط عنصرين" else "Connect"
            ) {
                connectSelected()
            }

            ToolButton(
                text = if (arabic) "حذف" else "Delete"
            ) {
                deleteSelected()
            }

            ToolButton(
                text = if (arabic) "الحسابات" else "Engineering"
            ) {
                showCalculation(
                    title =
                        if (arabic) {
                            "الحسابات الهندسية"
                        } else {
                            "Engineering Calculation"
                        }
                ) {
                    SldEngineeringEngine.calculateUpstream(
                        network()
                    )
                }
            }

            ToolButton(
                text = if (arabic) "تيار القصر" else "Short Circuit"
            ) {
                try {
                    val sc =
                        SldShortCircuitEngine.calculate(
                            network()
                        )

                    resultTitle =
                        if (arabic) {
                            "دراسة تيارات القصر"
                        } else {
                            "Short Circuit Study"
                        }

                    resultText =
                        buildShortCircuitReport(
                            network(),
                            sc
                        )

                    showResultDialog = true
                } catch (e: Exception) {
                    resultTitle =
                        if (arabic) "خطأ" else "Error"

                    resultText =
                        e.message
                            ?: "Short circuit calculation error"

                    showResultDialog = true
                }
            }

            ToolButton(
                text = if (arabic) "الكابلات" else "Cable Sizing"
            ) {
                try {
                    val sc =
                        SldShortCircuitEngine.calculate(
                            network()
                        )

                    val cable =
                        SldCableSizingEngine.calculate(
                            network = network(),
                            shortCircuitStudy = sc
                        )

                    resultTitle =
                        if (arabic) {
                            "اختيار الكابلات"
                        } else {
                            "Cable Sizing"
                        }

                    resultText =
                        buildCableReport(
                            network(),
                            cable
                        )

                    showResultDialog = true
                } catch (e: Exception) {
                    resultTitle =
                        if (arabic) "خطأ" else "Error"

                    resultText =
                        e.message ?: "Cable sizing error"

                    showResultDialog = true
                }
            }

            ToolButton(
                text = if (arabic) "الحماية" else "Protection"
            ) {
                try {
                    val sc =
                        SldShortCircuitEngine.calculate(
                            network()
                        )

                    val cable =
                        SldCableSizingEngine.calculate(
                            network = network(),
                            shortCircuitStudy = sc
                        )

                    val protection =
                        SldProtectionCoordinationEngine.calculate(
                            network = network(),
                            shortCircuitStudy = sc,
                            cableSizingStudy = cable
                        )

                    resultTitle =
                        if (arabic) {
                            "تنسيق الحمايات"
                        } else {
                            "Protection Coordination"
                        }

                    resultText =
                        protection.toEngineeringText()

                    showResultDialog = true
                } catch (e: Exception) {
                    resultTitle =
                        if (arabic) "خطأ" else "Error"

                    resultText =
                        e.message ?: "Protection calculation error"

                    showResultDialog = true
                }
            }

            ToolButton(
                text = if (arabic) "Panel Schedule" else "Panel Schedule"
            ) {
                val panelId = selectedNodeId

                if (panelId != null) {
                    try {
                        val panel =
                            nodes.firstOrNull {
                                it.id == panelId &&
                                    it.type == SldNodeType.PANEL
                            }

                        if (panel != null) {
                            val sc =
                                SldShortCircuitEngine.calculate(
                                    network()
                                )

                            val cable =
                                SldCableSizingEngine.calculate(
                                    network = network(),
                                    shortCircuitStudy = sc
                                )

                            val schedule =
                                SldPanelScheduleEngine.calculate(
                                    network = network(),
                                    panelNodeId = panel.id,
                                    cableSizingStudy = cable,
                                    shortCircuitStudy = sc
                                )

                            resultTitle =
                                "Panel Schedule - ${panel.name}"

                            resultText =
                                buildPanelScheduleReport(
                                    schedule
                                )

                            showResultDialog = true
                        }
                    } catch (e: Exception) {
                        resultTitle =
                            if (arabic) "خطأ" else "Error"

                        resultText =
                            e.message ?: "Panel schedule error"

                        showResultDialog = true
                    }
                }
            }

            ToolButton(
                text =
                    if (arabic) {
                        "Generate Complete Study"
                    } else {
                        "Complete Study"
                    },
                accent = true
            ) {
                runCompleteStudy()
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            SldCanvas(
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                connectionStartId = connectionStartId,
                shortCircuitStudy =
                    remember(nodes, connections) {
                        runCatching {
                            SldShortCircuitEngine.calculate(
                                SldNetwork(
                                    nodes = nodes,
                                    connections = connections
                                )
                            )
                        }.getOrNull()
                    },
                cableSizingStudy =
                    remember(nodes, connections) {
                        runCatching {
                            val n =
                                SldNetwork(
                                    nodes = nodes,
                                    connections = connections
                                )

                            val sc =
                                SldShortCircuitEngine.calculate(n)

                            SldCableSizingEngine.calculate(
                                network = n,
                                shortCircuitStudy = sc
                            )
                        }.getOrNull()
                    },
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
            onType = {
                pendingNodeType = it
            },
            onName = {
                nodeName = it
            },
            onVoltage = {
                nodeVoltage = it
            },
            onKw = {
                nodeKw = it
            },
            onPf = {
                nodePf = it
            },
            onDemand = {
                nodeDemand = it
            },
            onKva = {
                nodeKva = it
            },
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
        ConnectionEditorDialog(
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

    if (showResultDialog || showCompleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                showCompleteDialog = false
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
                        showCompleteDialog = false
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

@Composable
private fun ToolButton(
    text: String,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (accent) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
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
    shortCircuitStudy: SldShortCircuitStudy?,
    cableSizingStudy: com.electrical.calculationspro.data.SldCableSizingStudy?,
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

    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

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
                    detectDragGestures(
                        onDragStart = { position ->
                            val node =
                                findNode(
                                    position,
                                    currentNodes
                                )

                            draggedNodeId = node?.id

                            if (node != null) {
                                currentOnSelectNode(
                                    node.id
                                )
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
                    drawProfessionalConnection(
                        from = from,
                        to = to,
                        connection = connection,
                        selected =
                            connection.id ==
                                selectedConnectionId,
                        cableSizingStudy =
                            cableSizingStudy
                    )
                }
            }

            nodes.forEach { node ->
                drawProfessionalNode(
                    node = node,
                    selected =
                        node.id == selectedNodeId,
                    connectionStart =
                        node.id == connectionStartId,
                    shortCircuit =
                        shortCircuitStudy
                            ?.results
                            ?.get(node.id)
                )
            }
        }
    }
}

private fun DrawScope.drawGrid() {
    val grid = Color(0xFF14232B)

    var x = 0f

    while (x < size.width) {
        drawLine(
            color = grid,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )

        x += 50f
    }

    var y = 0f

    while (y < size.height) {
        drawLine(
            color = grid,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )

        y += 50f
    }
}

private fun DrawScope.drawProfessionalConnection(
    from: SldNode,
    to: SldNode,
    connection: SldConnection,
    selected: Boolean,
    cableSizingStudy: com.electrical.calculationspro.data.SldCableSizingStudy?
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

    val color =
        if (selected) {
            Accent
        } else {
            CableColor
        }

    val path = Path().apply {
        moveTo(start.x, start.y)

        val middleX =
            (start.x + end.x) / 2f

        lineTo(middleX, start.y)
        lineTo(middleX, end.y)
        lineTo(end.x, end.y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = if (selected) 6f else 3f
        )
    )

    drawCircle(
        color = color,
        radius = 5f,
        center = start
    )

    drawCircle(
        color = color,
        radius = 5f,
        center = end
    )

    val cableResult =
        cableSizingStudy
            ?.results
            ?.get(connection.id)

    val label =
        if (
            cableResult != null &&
            cableResult.recommendedSizeMm2 > 0.0
        ) {
            "${formatNumber(cableResult.recommendedSizeMm2)} mm² × ${cableResult.recommendedParallelRuns}"
        } else if (connection.cableSizeMm2 > 0.0) {
            "${formatNumber(connection.cableSizeMm2)} mm² × ${connection.parallelRuns}"
        } else {
            "Cable"
        }

    val midX =
        (start.x + end.x) / 2f

    val midY =
        (start.y + end.y) / 2f

    drawLabelBox(
        text = label,
        center = Offset(midX, midY - 13f),
        textColor = PrimaryText
    )

    if (connection.lengthMeters > 0.0) {
        drawLabelBox(
            text = "${formatNumber(connection.lengthMeters)} m",
            center = Offset(midX, midY + 15f),
            textColor = SecondaryText
        )
    }
}

private fun DrawScope.drawProfessionalNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    shortCircuit: SldShortCircuitResult?
) {
    val nodeColor =
        when (node.type) {
            SldNodeType.SOURCE -> SourceColor
            SldNodeType.TRANSFORMER -> TransformerColor
            SldNodeType.GENERATOR -> GeneratorColor
            SldNodeType.BUS -> BusColor
            SldNodeType.BREAKER -> BreakerColor
            SldNodeType.PANEL -> PanelColor
            SldNodeType.LOAD -> LoadColor
        }

    val actualColor =
        if (connectionStart) {
            Accent
        } else {
            nodeColor
        }

    drawRoundRect(
        color = actualColor,
        topLeft = Offset(node.x, node.y),
        size = Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius = CornerRadius(
            10f,
            10f
        )
    )

    if (selected) {
        drawRoundRect(
            color = Accent,
            topLeft = Offset(
                node.x - 4f,
                node.y - 4f
            ),
            size = Size(
                NODE_WIDTH + 8f,
                NODE_HEIGHT + 8f
            ),
            cornerRadius = CornerRadius(
                13f,
                13f
            ),
            style = Stroke(3f)
        )
    }

    when (node.type) {
        SldNodeType.SOURCE -> {
            drawSourceSymbol(node)
        }

        SldNodeType.TRANSFORMER -> {
            drawTransformerSymbol(node)
        }

        SldNodeType.GENERATOR -> {
            drawGeneratorSymbol(node)
        }

        SldNodeType.BUS -> {
            drawBusSymbol(node)
        }

        SldNodeType.BREAKER -> {
            drawBreakerSymbol(node)
        }

        SldNodeType.PANEL -> {
            drawPanelSymbol(node)
        }

        SldNodeType.LOAD -> {
            drawLoadSymbol(node)
        }
    }

    drawIntoCanvas { canvas ->
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = android.graphics.Color.WHITE
                textSize = 13f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
            }

        canvas.nativeCanvas.drawText(
            node.name.take(18),
            node.x + 42f,
            node.y + 20f,
            paint
        )

        paint.textSize = 10f
        paint.typeface =
            android.graphics.Typeface.DEFAULT

        canvas.nativeCanvas.drawText(
            node.type.name,
            node.x + 42f,
            node.y + 36f,
            paint
        )

        val dataLine =
            when (node.type) {
                SldNodeType.SOURCE ->
                    "${formatNumber(node.sourceShortCircuitMva)} MVA | ${formatNumber(node.voltage)} V"

                SldNodeType.TRANSFORMER ->
                    "${formatNumber(node.ratedKva)} kVA | Z ${formatNumber(node.transformerPercentZ)}%"

                SldNodeType.GENERATOR ->
                    "${formatNumber(node.ratedKva)} kVA | Xd'' ${formatNumber(node.generatorXdSubtransient)}%"

                SldNodeType.BUS ->
                    "${formatNumber(node.voltage)} V | ${shortCircuit?.initialSymmetricalCurrentKa?.let { formatKa(it) } ?: "--"}"

                SldNodeType.BREAKER ->
                    "Icu ${shortCircuit?.breakerRequiredKa?.let { formatKa(it) } ?: "--"}"

                SldNodeType.PANEL ->
                    "${formatNumber(node.voltage)} V | ${shortCircuit?.initialSymmetricalCurrentKa?.let { formatKa(it) } ?: "--"}"

                SldNodeType.LOAD ->
                    "${formatNumber(node.loadKw)} kW | PF ${formatNumber(node.powerFactor)}"
            }

        paint.textSize = 9f

        canvas.nativeCanvas.drawText(
            dataLine.take(30),
            node.x + 42f,
            node.y + 53f,
            paint
        )

        val faultLine =
            shortCircuit?.let {
                "Ik ${formatKa(it.initialSymmetricalCurrentKa)} | Ip ${formatKa(it.peakCurrentKa)}"
            }

        if (faultLine != null) {
            paint.textSize = 8.5f

            canvas.nativeCanvas.drawText(
                faultLine.take(35),
                node.x + 42f,
                node.y + 70f,
                paint
            )
        }
    }
}

private fun DrawScope.drawSourceSymbol(
    node: SldNode
) {
    val center =
        Offset(
            node.x + 20f,
            node.y + 43f
        )

    drawCircle(
        color = Color.White,
        radius = 13f,
        center = center,
        style = Stroke(2f)
    )

    drawLine(
        color = Color.White,
        start = Offset(
            center.x - 7f,
            center.y
        ),
        end = Offset(
            center.x + 7f,
            center.y
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start = Offset(
            center.x,
            center.y - 7f
        ),
        end = Offset(
            center.x,
            center.y + 7f
        ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawTransformerSymbol(
    node: SldNode
) {
    val c1 =
        Offset(
            node.x + 17f,
            node.y + 37f
        )

    val c2 =
        Offset(
            node.x + 28f,
            node.y + 49f
        )

    drawCircle(
        color = Color.White,
        radius = 11f,
        center = c1,
        style = Stroke(2f)
    )

    drawCircle(
        color = Color.White,
        radius = 11f,
        center = c2,
        style = Stroke(2f)
    )

    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 3f,
            node.y + 43f
        ),
        end = Offset(
            node.x + 6f,
            node.y + 43f
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 39f,
            node.y + 43f
        ),
        end = Offset(
            node.x + 47f,
            node.y + 43f
        ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawGeneratorSymbol(
    node: SldNode
) {
    val center =
        Offset(
            node.x + 22f,
            node.y + 43f
        )

    drawCircle(
        color = Color.White,
        radius = 15f,
        center = center,
        style = Stroke(2f)
    )

    drawLine(
        color = Color.White,
        start = Offset(
            center.x - 7f,
            center.y
        ),
        end = Offset(
            center.x + 7f,
            center.y
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start = Offset(
            center.x - 5f,
            center.y - 5f
        ),
        end = Offset(
            center.x + 5f,
            center.y + 5f
        ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawBusSymbol(
    node: SldNode
) {
    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 7f,
            node.y + 43f
        ),
        end = Offset(
            node.x + 37f,
            node.y + 43f
        ),
        strokeWidth = 7f
    )

    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 12f,
            node.y + 25f
        ),
        end = Offset(
            node.x + 12f,
            node.y + 60f
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 32f,
            node.y + 25f
        ),
        end = Offset(
            node.x + 32f,
            node.y + 60f
        ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawBreakerSymbol(
    node: SldNode
) {
    drawRect(
        color = Color.White,
        topLeft = Offset(
            node.x + 8f,
            node.y + 27f
        ),
        size = Size(
            29f,
            30f
        ),
        style = Stroke(2f)
    )

    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 13f,
            node.y + 50f
        ),
        end = Offset(
            node.x + 32f,
            node.y + 34f
        ),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawPanelSymbol(
    node: SldNode
) {
    drawRect(
        color = Color.White,
        topLeft = Offset(
            node.x + 8f,
            node.y + 23f
        ),
        size = Size(
            30f,
            38f
        ),
        style = Stroke(2f)
    )

    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 15f,
            node.y + 30f
        ),
        end = Offset(
            node.x + 15f,
            node.y + 54f
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 23f,
            node.y + 30f
        ),
        end = Offset(
            node.x + 23f,
            node.y + 54f
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start = Offset(
            node.x + 31f,
            node.y + 30f
        ),
        end = Offset(
            node.x + 31f,
            node.y + 54f
        ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawLoadSymbol(
    node: SldNode
) {
    val center =
        Offset(
            node.x + 22f,
            node.y + 43f
        )

    drawCircle(
        color = Color.White,
        radius = 14f,
        center = center,
        style = Stroke(2f)
    )

    drawLine(
        color = Color.White,
        start = Offset(
            center.x,
            center.y - 9f
        ),
        end = Offset(
            center.x,
            center.y + 9f
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start = Offset(
            center.x - 8f,
            center.y
        ),
        end = Offset(
            center.x + 8f,
            center.y
        ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawLabelBox(
    text: String,
    center: Offset,
    textColor: Color
) {
    val width =
        (text.length * 6.0f)
            .coerceIn(45f, 160f)

    drawRoundRect(
        color = Color(0xDD101820),
        topLeft = Offset(
            center.x - width / 2f,
            center.y - 9f
        ),
        size = Size(
            width,
            18f
        ),
        cornerRadius = CornerRadius(
            5f,
            5f
        )
    )

    drawIntoCanvas { canvas ->
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = textColor.toArgbInt()
                textSize = 9f
                textAlign =
                    android.graphics.Paint.Align.CENTER
            }

        canvas.nativeCanvas.drawText(
            text,
            center.x,
            center.y + 3f,
            paint
        )
    }
}

private fun findNode(
    position: Offset,
    nodes: List<SldNode>
): SldNode? {
    return nodes
        .asReversed()
        .firstOrNull { node ->
            position.x >=
                node.x - NODE_TOUCH_PADDING &&
                position.x <=
                node.x +
                NODE_WIDTH +
                NODE_TOUCH_PADDING &&
                position.y >=
                node.y - NODE_TOUCH_PADDING &&
                position.y <=
                node.y +
                NODE_HEIGHT +
                NODE_TOUCH_PADDING
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
            ) <= 20f
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

    val segments =
        listOf(
            Pair(
                Offset(start.x, start.y),
                Offset(middleX, start.y)
            ),
            Pair(
                Offset(middleX, start.y),
                Offset(middleX, end.y)
            ),
            Pair(
                Offset(middleX, end.y),
                Offset(end.x, end.y)
            )
        )

    return segments.minOf {
        distanceToSegment(
            p,
            it.first,
            it.second
        )
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
        t.coerceIn(
            0f,
            1f
        )

    val x =
        a.x + clamped * dx

    val y =
        a.y + clamped * dy

    return hypot(
        p.x - x,
        p.y - y
    )
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
                        if (arabic) "الاسم" else "Name",
                    onValueChange = onName
                )

                EditorField(
                    value = voltage,
                    label =
                        if (arabic) "الجهد V" else "Voltage V",
                    onValueChange = onVoltage
                )

                when (type) {
                    SldNodeType.SOURCE -> {
                        EditorField(
                            value = sourceMva,
                            label =
                                if (arabic) {
                                    "مستوى القصر MVA"
                                } else {
                                    "Fault Level MVA"
                                },
                            onValueChange = onSourceMva
                        )
                    }

                    SldNodeType.TRANSFORMER -> {
                        EditorField(
                            value = kva,
                            label =
                                if (arabic) {
                                    "قدرة المحول kVA"
                                } else {
                                    "Transformer Rating kVA"
                                },
                            onValueChange = onKva
                        )

                        EditorField(
                            value = transformerZ,
                            label =
                                if (arabic) {
                                    "%Z للمحول"
                                } else {
                                    "Transformer %Z"
                                },
                            onValueChange = onTransformerZ
                        )
                    }

                    SldNodeType.GENERATOR -> {
                        EditorField(
                            value = kva,
                            label =
                                if (arabic) {
                                    "قدرة المولد kVA"
                                } else {
                                    "Generator Rating kVA"
                                },
                            onValueChange = onKva
                        )

                        EditorField(
                            value = generatorXd,
                            label = "Xd'' %",
                            onValueChange = onGeneratorXd
                        )
                    }

                    SldNodeType.LOAD -> {
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
                    }

                    SldNodeType.BUS,
                    SldNodeType.PANEL,
                    SldNodeType.BREAKER -> {
                        EditorField(
                            value = kva,
                            label =
                                if (arabic) {
                                    "القدرة الاسمية kVA"
                                } else {
                                    "Rated kVA"
                                },
                            onValueChange = onKva
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
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
                    "بيانات الكابل والتوصيل"
                } else {
                    "Cable / Connection Data"
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
                            "طول الكابل m"
                        } else {
                            "Cable Length m"
                        },
                    onValueChange = onLength
                )

                EditorField(
                    value = resistance,
                    label =
                        if (arabic) {
                            "R Ω/km"
                        } else {
                            "Resistance Ω/km"
                        },
                    onValueChange = onResistance
                )

                EditorField(
                    value = reactance,
                    label =
                        if (arabic) {
                            "X Ω/km"
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
                            "السعة A"
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
            value = typeLabel(
                type,
                arabic
            ),
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
                        Text(
                            typeLabel(
                                item,
                                arabic
                            )
                        )
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

private fun typeLabel(
    type: SldNodeType,
    arabic: Boolean
): String {
    if (!arabic) {
        return type.name
    }

    return when (type) {
        SldNodeType.SOURCE -> "مصدر"
        SldNodeType.TRANSFORMER -> "محول"
        SldNodeType.GENERATOR -> "مولد"
        SldNodeType.BUS -> "باسبار"
        SldNodeType.PANEL -> "لوحة"
        SldNodeType.BREAKER -> "قاطع"
        SldNodeType.LOAD -> "حمل"
    }
}

private fun buildShortCircuitReport(
    network: SldNetwork,
    study: SldShortCircuitStudy
): String {
    val builder = StringBuilder()

    builder.appendLine("SHORT CIRCUIT ENGINEERING REPORT")
    builder.appendLine("================================")
    builder.appendLine()

    builder.appendLine(
        "Maximum Ik''  : ${formatKa(study.maximumFaultCurrentKa)}"
    )

    builder.appendLine(
        "Maximum Ip    : ${formatKa(study.maximumPeakCurrentKa)}"
    )

    builder.appendLine(
        "Maximum Fault : ${formatMva(study.maximumFaultMva)}"
    )

    builder.appendLine()

    network.nodes.forEach { node ->
        val result =
            study.results[node.id]

        if (result != null) {
            builder.appendLine(
                "----------------------------------------"
            )

            builder.appendLine(
                "${node.name}  [${node.type.name}]"
            )

            builder.appendLine(
                "Voltage              : ${formatNumber(result.voltageV)} V"
            )

            builder.appendLine(
                "Ik'' Initial          : ${formatKa(result.initialSymmetricalCurrentKa)}"
            )

            builder.appendLine(
                "Ip Peak              : ${formatKa(result.peakCurrentKa)}"
            )

            builder.appendLine(
                "Ith Thermal          : ${formatKa(result.thermalCurrentKa)}"
            )

            builder.appendLine(
                "Fault MVA            : ${formatMva(result.shortCircuitMva)}"
            )

            builder.appendLine(
                "X/R                  : ${formatNumber(result.xrRatio)}"
            )

            builder.appendLine(
                "R                    : ${formatResistance(result.resistanceOhm)}"
            )

            builder.appendLine(
                "X                    : ${formatResistance(result.reactanceOhm)}"
            )

            builder.appendLine(
                "Z                    : ${formatResistance(result.impedanceOhm)}"
            )

            builder.appendLine(
                "Breaker Required     : ${formatKa(result.breakerRequiredKa)}"
            )

            builder.appendLine(
                "Recommended Icu      : ${recommendedIcu(result.breakerRequiredKa)}"
            )

            builder.appendLine(
                "Recommended Ics      : ${recommendedIcs(result.breakerRequiredKa)}"
            )

            if (result.notes.isNotEmpty()) {
                builder.appendLine(
                    "Notes:"
                )

                result.notes.forEach {
                    builder.appendLine(
                        "  - $it"
                    )
                }
            }

            builder.appendLine()
        }
    }

    return builder.toString().trim()
}

private fun buildCableReport(
    network: SldNetwork,
    study: com.electrical.calculationspro.data.SldCableSizingStudy
): String {
    val builder = StringBuilder()

    builder.appendLine("CABLE SIZING ENGINEERING REPORT")
    builder.appendLine("================================")
    builder.appendLine()
    builder.appendLine(
        "Successful Feeders : ${study.successfulFeeders}"
    )
    builder.appendLine(
        "Failed Feeders     : ${study.failedFeeders}"
    )
    builder.appendLine()

    network.connections.forEach { connection ->
        val result =
            study.results[connection.id]

        if (result != null) {
            val from =
                network.nodes.firstOrNull {
                    it.id == connection.fromNodeId
                }

            val to =
                network.nodes.firstOrNull {
                    it.id == connection.toNodeId
                }

            builder.appendLine(
                "----------------------------------------"
            )

            builder.appendLine(
                "${from?.name ?: connection.fromNodeId} -> ${to?.name ?: connection.toNodeId}"
            )

            builder.appendLine(
                "Design Current       : ${formatA(result.designCurrentA)}"
            )

            builder.appendLine(
                "Required Capacity    : ${formatA(result.requiredCurrentCapacityA)}"
            )

            builder.appendLine(
                "Short Circuit        : ${formatKa(result.shortCircuitCurrentKa)}"
            )

            builder.appendLine(
                "Recommended Cable    : ${formatNumber(result.recommendedSizeMm2)} mm²"
            )

            builder.appendLine(
                "Parallel Runs        : ${result.recommendedParallelRuns}"
            )

            builder.appendLine(
                "Material             : ${result.recommendedMaterial}"
            )

            builder.appendLine(
                "Cores                : ${result.recommendedCores}"
            )

            builder.appendLine(
                "Current Capacity     : ${formatA(result.recommendedCurrentCapacityA)}"
            )

            builder.appendLine(
                "Voltage Drop         : ${formatNumber(result.recommendedVoltageDropPercent)} %"
            )

            builder.appendLine(
                "SC Withstand         : ${formatKa(result.recommendedShortCircuitWithstandKa)}"
            )
        }
    }

    return builder.toString().trim()
}

private fun buildPanelScheduleReport(
    schedule: com.electrical.calculationspro.data.SldPanelSchedule
): String {
    val builder = StringBuilder()

    builder.appendLine(
        "PANEL SCHEDULE - ${schedule.panelName}"
    )
    builder.appendLine(
        "================================"
    )
    builder.appendLine()

    builder.appendLine(
        "Panel Voltage        : ${formatNumber(schedule.panelVoltageV)} V"
    )

    builder.appendLine(
        "Connected Load       : ${formatNumber(schedule.totalConnectedLoadKw)} kW"
    )

    builder.appendLine(
        "Demand Load          : ${formatNumber(schedule.totalDemandLoadKw)} kW"
    )

    builder.appendLine(
        "Demand Current       : ${formatA(schedule.totalDemandCurrentA)}"
    )

    builder.appendLine()

    schedule.rows.forEachIndexed { index, row ->
        builder.appendLine(
            "----------------------------------------"
        )

        builder.appendLine(
            "Circuit ${index + 1} : ${row.feederName}"
        )

        builder.appendLine(
            "Load                 : ${formatNumber(row.loadKw)} kW"
        )

        builder.appendLine(
            "Design Current       : ${formatA(row.designCurrentA)}"
        )

        builder.appendLine(
            "Breaker              : ${formatA(row.recommendedBreakerA)}"
        )

        builder.appendLine(
            "Cable                : ${formatNumber(row.cableSizeMm2)} mm² x ${row.parallelRuns}"
        )

        builder.appendLine(
            "Cable Capacity       : ${formatA(row.currentCapacityA)}"
        )

        builder.appendLine(
            "Voltage Drop         : ${formatNumber(row.voltageDropPercent)} %"
        )

        builder.appendLine(
            "Short Circuit        : ${formatKa(row.shortCircuitCurrentKa)}"
        )

        builder.appendLine(
            "Status               : ${row.status.name}"
        )
    }

    return builder.toString().trim()
}

private fun buildUpstreamReport(
    result: Any
): String {
    return buildString {
        appendLine("UPSTREAM ENGINEERING STUDY")
        appendLine("==========================")
        appendLine()
        appendLine(
            "Automatic upstream load, demand, current and diversity calculation completed."
        )
        appendLine()
        appendLine(
            "Engineering result generated from SLD network."
        )
    }
}

private fun buildCompleteReport(
    network: SldNetwork,
    shortCircuit: SldShortCircuitStudy,
    cableSizing: com.electrical.calculationspro.data.SldCableSizingStudy,
    panelText: String?,
    upstreamText: String?,
    protectionText: String?
): String {
    val builder = StringBuilder()

    builder.appendLine("COMPLETE ELECTRICAL DESIGN REPORT")
    builder.appendLine("=================================")
    builder.appendLine()

    builder.appendLine(
        "NETWORK"
    )

    builder.appendLine(
        "Elements             : ${network.nodes.size}"
    )

    builder.appendLine(
        "Connections          : ${network.connections.size}"
    )

    builder.appendLine()

    builder.appendLine(
        "FAULT LEVEL SUMMARY"
    )

    builder.appendLine(
        "Maximum Ik''         : ${formatKa(shortCircuit.maximumFaultCurrentKa)}"
    )

    builder.appendLine(
        "Maximum Ip           : ${formatKa(shortCircuit.maximumPeakCurrentKa)}"
    )

    builder.appendLine(
        "Maximum Fault MVA    : ${formatMva(shortCircuit.maximumFaultMva)}"
    )

    builder.appendLine()

    builder.append(
        buildShortCircuitReport(
            network,
            shortCircuit
        )
    )

    builder.appendLine()
    builder.appendLine()

    builder.append(
        buildCableReport(
            network,
            cableSizing
        )
    )

    if (!upstreamText.isNullOrBlank()) {
        builder.appendLine()
        builder.appendLine()
        builder.append(upstreamText)
    }

    if (!protectionText.isNullOrBlank()) {
        builder.appendLine()
        builder.appendLine()
        builder.appendLine(
            "PROTECTION COORDINATION"
        )
        builder.appendLine(
            "======================="
        )
        builder.appendLine(
            protectionText
        )
    }

    if (!panelText.isNullOrBlank()) {
        builder.appendLine()
        builder.appendLine()
        builder.append(panelText)
    }

    return builder.toString().trim()
}

private fun Any.toEngineeringText(): String {
    val text =
        this.toString()

    if (
        text.startsWith(
            "SldProtectionCoordinationResult"
        )
    ) {
        return text
            .replace(
                "SldProtectionCoordinationResult(",
                "Protection coordination study completed: "
            )
            .replace(
                ")",
                ""
            )
            .replace(
                ", ",
                "\n"
            )
    }

    return text
        .replace(
            "SldProtectionCoordinationResult(",
            "Protection Coordination\n"
        )
        .replace(
            ")",
            ""
        )
        .replace(
            ", ",
            "\n"
        )
}

private fun formatNumber(
    value: Double
): String {
    if (!value.isFinite()) {
        return "--"
    }

    val rounded =
        (value * 100.0).roundToInt() / 100.0

    return if (
        rounded == rounded.toLong().toDouble()
    ) {
        rounded.toLong().toString()
    } else {
        "%.2f".format(
            java.util.Locale.US,
            rounded
        )
    }
}

private fun formatA(
    value: Double
): String {
    return "${formatNumber(value)} A"
}

private fun formatKa(
    value: Double
): String {
    return "${formatNumber(value)} kA"
}

private fun formatMva(
    value: Double
): String {
    return "${formatNumber(value)} MVA"
}

private fun formatResistance(
    value: Double
): String {
    return "${"%.6f".format(java.util.Locale.US, value)} Ω"
}

private fun recommendedIcu(
    faultKa: Double
): String {
    val standard =
        listOf(
            6.0,
            10.0,
            15.0,
            18.0,
            25.0,
            36.0,
            50.0,
            65.0,
            80.0,
            100.0,
            120.0,
            150.0
        )

    val value =
        standard.firstOrNull {
            it >= faultKa
        } ?: (faultKa * 1.15)

    return "${formatNumber(value)} kA"
}

private fun recommendedIcs(
    faultKa: Double
): String {
    val icu =
        recommendedIcuValue(
            faultKa
        )

    val ics =
        icu * 0.75

    return "${formatNumber(ics)} kA"
}

private fun recommendedIcuValue(
    faultKa: Double
): Double {
    val standard =
        listOf(
            6.0,
            10.0,
            15.0,
            18.0,
            25.0,
            36.0,
            50.0,
            65.0,
            80.0,
            100.0,
            120.0,
            150.0
        )

    return standard.firstOrNull {
        it >= faultKa
    } ?: faultKa * 1.15
}

private fun Color.toArgbInt(): Int {
    val a =
        (alpha * 255f)
            .roundToInt()
            .coerceIn(0, 255)

    val r =
        (red * 255f)
            .roundToInt()
            .coerceIn(0, 255)

    val g =
        (green * 255f)
            .roundToInt()
            .coerceIn(0, 255)

    val b =
        (blue * 255f)
            .roundToInt()
            .coerceIn(0, 255)

    return android.graphics.Color.argb(
        a,
        r,
        g,
        b
    )
}
