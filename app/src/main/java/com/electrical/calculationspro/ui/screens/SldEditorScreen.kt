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
import androidx.compose.foundation.layout.fillMaxHeight
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
import kotlin.math.min

private val Background = Color(0xFF070D12)
private val CanvasBackground = Color(0xFF081016)
private val CardColor = Color(0xFF151D24)
private val PrimaryText = Color(0xFFF2F5F7)
private val SecondaryText = Color(0xFF9BA8B2)
private val Accent = Color(0xFF00BCD4)
private val Danger = Color(0xFFE53935)
private val Success = Color(0xFF43A047)
private val BusbarColor = Color(0xFFFFC107)
private val CableColor = Color(0xFF90A4AE)

private const val NODE_WIDTH = 150f
private const val NODE_HEIGHT = 86f
private const val NODE_TOUCH_PADDING = 35f
private const val CANVAS_WIDTH_DP = 1900
private const val BUSBAR_WIDTH = 210f
private const val BUSBAR_HEIGHT = 14f

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
                    x = 70f,
                    y = 250f,
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
        nodeKw = if (type == SldNodeType.LOAD) "100" else "0"
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
                x = (nodes.maxOfOrNull { it.x } ?: 70f) + 190f,
                y = nodes.lastOrNull()?.y ?: 250f,
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

    /*
     * Generates a complete professional starter SLD.
     *
     * Source
     *   ↓
     * Main Breaker
     *   ↓
     * Transformer
     *   ↓
     * Main Bus
     *   ↓
     * Main Breaker
     *   ↓
     * Panel
     *   ↓
     * Loads
     */
    fun generateCompleteSld() {
        val source =
            SldNode(
                id = "source-1",
                name = "UTILITY SOURCE",
                type = SldNodeType.SOURCE,
                x = 60f,
                y = 260f,
                voltage = 400.0,
                sourceShortCircuitMva = 500.0
            )

        val sourceBreaker =
            SldNode(
                id = "auto-cb-source",
                name = "MAIN INCOMER",
                type = SldNodeType.BREAKER,
                x = 260f,
                y = 260f,
                voltage = 400.0
            )

        val transformer =
            SldNode(
                id = "auto-tr-1",
                name = "TR-1",
                type = SldNodeType.TRANSFORMER,
                x = 480f,
                y = 260f,
                voltage = 400.0,
                ratedKva = 1000.0,
                transformerPercentZ = 6.0
            )

        val bus =
            SldNode(
                id = "auto-bus-1",
                name = "MAIN BUS",
                type = SldNodeType.BUS,
                x = 710f,
                y = 260f,
                voltage = 400.0
            )

        val outgoingBreaker =
            SldNode(
                id = "auto-cb-panel",
                name = "PANEL INCOMER",
                type = SldNodeType.BREAKER,
                x = 930f,
                y = 260f,
                voltage = 400.0
            )

        val panel =
            SldNode(
                id = "auto-panel-1",
                name = "MDB-1",
                type = SldNodeType.PANEL,
                x = 1150f,
                y = 260f,
                voltage = 400.0
            )

        val load1 =
            SldNode(
                id = "auto-load-1",
                name = "LOAD-1",
                type = SldNodeType.LOAD,
                x = 1390f,
                y = 160f,
                voltage = 400.0,
                loadKw = 100.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            )

        val load2 =
            SldNode(
                id = "auto-load-2",
                name = "LOAD-2",
                type = SldNodeType.LOAD,
                x = 1390f,
                y = 280f,
                voltage = 400.0,
                loadKw = 75.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            )

        val load3 =
            SldNode(
                id = "auto-load-3",
                name = "LOAD-3",
                type = SldNodeType.LOAD,
                x = 1390f,
                y = 400f,
                voltage = 400.0,
                loadKw = 50.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            )

        val generatedNodes =
            listOf(
                source,
                sourceBreaker,
                transformer,
                bus,
                outgoingBreaker,
                panel,
                load1,
                load2,
                load3
            )

        val generatedConnections =
            listOf(
                SldConnection(
                    id = "auto-c1",
                    fromNodeId = source.id,
                    toNodeId = sourceBreaker.id,
                    lengthMeters = 5.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080
                ),
                SldConnection(
                    id = "auto-c2",
                    fromNodeId = sourceBreaker.id,
                    toNodeId = transformer.id,
                    lengthMeters = 10.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080
                ),
                SldConnection(
                    id = "auto-c3",
                    fromNodeId = transformer.id,
                    toNodeId = bus.id,
                    lengthMeters = 5.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080
                ),
                SldConnection(
                    id = "auto-c4",
                    fromNodeId = bus.id,
                    toNodeId = outgoingBreaker.id,
                    lengthMeters = 3.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080
                ),
                SldConnection(
                    id = "auto-c5",
                    fromNodeId = outgoingBreaker.id,
                    toNodeId = panel.id,
                    lengthMeters = 5.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080
                ),
                SldConnection(
                    id = "auto-c6",
                    fromNodeId = panel.id,
                    toNodeId = load1.id,
                    lengthMeters = 20.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080,
                    cableSizeMm2 = 70.0,
                    parallelRuns = 1
                ),
                SldConnection(
                    id = "auto-c7",
                    fromNodeId = panel.id,
                    toNodeId = load2.id,
                    lengthMeters = 25.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080,
                    cableSizeMm2 = 50.0,
                    parallelRuns = 1
                ),
                SldConnection(
                    id = "auto-c8",
                    fromNodeId = panel.id,
                    toNodeId = load3.id,
                    lengthMeters = 30.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080,
                    cableSizeMm2 = 35.0,
                    parallelRuns = 1
                )
            )

        nodes = generatedNodes
        connections = generatedConnections
        selectedNodeId = panel.id
        selectedConnectionId = null
        connectionStartId = null
    }

    fun showCalculation(
        title: String,
        block: () -> Any
    ) {
        try {
            resultTitle = title
            resultText = formatEngineeringResult(
                block()
            )
        } catch (e: Exception) {
            resultTitle =
                if (arabic) "خطأ في الحساب" else "Calculation Error"

            resultText =
                e.message ?: "Calculation error"
        }

        showResultDialog = true
    }

    fun generateCompleteReport() {
        try {
            val n = network()

            val upstream =
                SldEngineeringEngine.calculateUpstream(n)

            val shortCircuit =
                SldShortCircuitEngine.calculate(n)

            val cableSizing =
                SldCableSizingEngine.calculate(
                    network = n,
                    shortCircuitStudy = shortCircuit
                )

            val protection =
                SldProtectionCoordinationEngine.calculate(
                    network = n,
                    shortCircuitStudy = shortCircuit,
                    cableSizingStudy = cableSizing
                )

            val selectedPanel =
                nodes.firstOrNull {
                    it.type == SldNodeType.PANEL
                }

            val panelSchedule =
                selectedPanel?.let {
                    SldPanelScheduleEngine.calculate(
                        network = n,
                        panelNodeId = it.id,
                        cableSizingStudy = cableSizing
                    )
                }

            resultTitle =
                if (arabic) {
                    "التقرير الهندسي الكامل"
                } else {
                    "Complete Engineering Report"
                }

            resultText =
                buildString {
                    appendLine(
                        if (arabic) {
                            "════════ التقرير الهندسي ════════"
                        } else {
                            "════════ ENGINEERING REPORT ════════"
                        }
                    )
                    appendLine()

                    appendLine(
                        if (arabic) {
                            "1 - منظومة التغذية"
                        } else {
                            "1 - SUPPLY SYSTEM"
                        }
                    )
                    appendLine(
                        formatNetworkSummary(
                            n,
                            arabic
                        )
                    )

                    appendLine()
                    appendLine(
                        if (arabic) {
                            "2 - تيارات القصر"
                        } else {
                            "2 - SHORT CIRCUIT"
                        }
                    )
                    appendLine(
                        formatEngineeringResult(
                            shortCircuit
                        )
                    )

                    appendLine()
                    appendLine(
                        if (arabic) {
                            "3 - اختيار الكابلات"
                        } else {
                            "3 - CABLE SIZING"
                        }
                    )
                    appendLine(
                        formatEngineeringResult(
                            cableSizing
                        )
                    )

                    appendLine()
                    appendLine(
                        if (arabic) {
                            "4 - تنسيق الحمايات"
                        } else {
                            "4 - PROTECTION COORDINATION"
                        }
                    )
                    appendLine(
                        formatEngineeringResult(
                            protection
                        )
                    )

                    appendLine()
                    appendLine(
                        if (arabic) {
                            "5 - حسابات أعلى الشبكة"
                        } else {
                            "5 - UPSTREAM CALCULATIONS"
                        }
                    )
                    appendLine(
                        formatEngineeringResult(
                            upstream
                        )
                    )

                    if (panelSchedule != null) {
                        appendLine()
                        appendLine(
                            if (arabic) {
                                "6 - Panel Schedule"
                            } else {
                                "6 - PANEL SCHEDULE"
                            }
                        )
                        appendLine(
                            formatEngineeringResult(
                                panelSchedule
                            )
                        )
                    }
                }

            showResultDialog = true
        } catch (e: Exception) {
            resultTitle =
                if (arabic) "خطأ" else "Error"

            resultText =
                e.message ?: "Complete SLD calculation error"

            showResultDialog = true
        }
    }

    val shortCircuitForCanvas =
        remember(nodes, connections) {
            try {
                SldShortCircuitEngine.calculate(
                    SldNetwork(
                        nodes = nodes,
                        connections = connections
                    )
                )
            } catch (_: Exception) {
                null
            }
        }

    val faultByNode =
        remember(shortCircuitForCanvas) {
            extractShortCircuitMap(
                shortCircuitForCanvas
            )
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
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                TextButton(
                    onClick = it
                ) {
                    Text(
                        text =
                            if (arabic) {
                                "رجوع"
                            } else {
                                "Back"
                            },
                        color = Accent
                    )
                }
            }

            Text(
                modifier = Modifier.weight(1f),
                text =
                    if (arabic) {
                        "المخطط الأحادي الاحترافي SLD"
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
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(horizontal = 12.dp),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            ToolButton(
                text =
                    if (arabic) "مصدر" else "Source"
            ) {
                openAdd(
                    SldNodeType.SOURCE
                )
            }

            ToolButton(
                text =
                    if (arabic) "محول" else "Transformer"
            ) {
                openAdd(
                    SldNodeType.TRANSFORMER
                )
            }

            ToolButton(
                text =
                    if (arabic) "مولد" else "Generator"
            ) {
                openAdd(
                    SldNodeType.GENERATOR
                )
            }

            ToolButton(
                text =
                    if (arabic) "باسبار" else "Busbar"
            ) {
                openAdd(
                    SldNodeType.BUS
                )
            }

            ToolButton(
                text =
                    if (arabic) "قاطع" else "Breaker"
            ) {
                openAdd(
                    SldNodeType.BREAKER
                )
            }

            ToolButton(
                text =
                    if (arabic) "لوحة" else "Panel"
            ) {
                openAdd(
                    SldNodeType.PANEL
                )
            }

            ToolButton(
                text =
                    if (arabic) "حمل" else "Load"
            ) {
                openAdd(
                    SldNodeType.LOAD
                )
            }

            ToolButton(
                text =
                    if (arabic) "ربط" else "Connect"
            ) {
                connectSelected()
            }

            ToolButton(
                text =
                    if (arabic) "حذف" else "Delete"
            ) {
                deleteSelected()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            ToolButton(
                text =
                    if (arabic) {
                        "توليد SLD كامل"
                    } else {
                        "Generate Complete SLD"
                    }
            ) {
                generateCompleteSld()
            }

            ToolButton(
                text =
                    if (arabic) {
                        "التقرير الكامل"
                    } else {
                        "Complete Report"
                    }
            ) {
                generateCompleteReport()
            }

            ToolButton(
                text =
                    if (arabic) {
                        "تيار القصر"
                    } else {
                        "Short Circuit"
                    }
            ) {
                showCalculation(
                    if (arabic) {
                        "دراسة تيارات القصر"
                    } else {
                        "Short Circuit Study"
                    }
                ) {
                    SldShortCircuitEngine.calculate(
                        network()
                    )
                }
            }

            ToolButton(
                text =
                    if (arabic) {
                        "الكابلات"
                    } else {
                        "Cable Sizing"
                    }
            ) {
                showCalculation(
                    if (arabic) {
                        "اختيار الكابلات"
                    } else {
                        "Cable Sizing"
                    }
                ) {
                    val sc =
                        SldShortCircuitEngine.calculate(
                            network()
                        )

                    SldCableSizingEngine.calculate(
                        network = network(),
                        shortCircuitStudy = sc
                    )
                }
            }

            ToolButton(
                text =
                    if (arabic) {
                        "الحماية"
                    } else {
                        "Protection"
                    }
            ) {
                showCalculation(
                    if (arabic) {
                        "تنسيق الحمايات"
                    } else {
                        "Protection Coordination"
                    }
                ) {
                    val sc =
                        SldShortCircuitEngine.calculate(
                            network()
                        )

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
                text = "Panel Schedule"
            ) {
                val panel =
                    selectedNodeId
                        ?: nodes.firstOrNull {
                            it.type == SldNodeType.PANEL
                        }?.id

                if (panel != null) {
                    showCalculation(
                        "Panel Schedule"
                    ) {
                        val sc =
                            SldShortCircuitEngine.calculate(
                                network()
                            )

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(
                        rememberScrollState()
                    )
            ) {
                SldCanvas(
                    nodes = nodes,
                    connections = connections,
                    selectedNodeId = selectedNodeId,
                    selectedConnectionId =
                        selectedConnectionId,
                    connectionStartId =
                        connectionStartId,
                    faultByNode =
                        faultByNode,
                    modifier =
                        Modifier
                            .width(
                                CANVAS_WIDTH_DP.dp
                            )
                            .fillMaxHeight(),
                    onSelectNode = {
                        selectedNodeId = it
                        selectedConnectionId = null
                    },
                    onMoveNode = { id, x, y ->
                        nodes =
                            nodes.map { node ->
                                if (node.id == id) {
                                    node.copy(
                                        x = max(
                                            10f,
                                            x
                                        ),
                                        y = max(
                                            10f,
                                            y
                                        )
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
    }

    if (showNodeDialog) {
        NodeEditorDialog(
            arabic = arabic,
            editing =
                editingNodeId != null,
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

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
            },
            title = {
                Text(
                    text = resultTitle,
                    fontWeight =
                        FontWeight.Bold
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
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = resultText,
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
                        if (arabic) {
                            "إغلاق"
                        } else {
                            "Close"
                        }
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
            text = text,
            fontSize = 11.sp
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
    faultByNode: Map<String, FaultDisplay>,
    modifier: Modifier,
    onSelectNode: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onSelectConnection: (String) -> Unit,
    onEditNode: (SldNode) -> Unit,
    onEditConnection: (SldConnection) -> Unit
) {
    val currentNodes by rememberUpdatedState(nodes)
    val currentConnections by rememberUpdatedState(
        connections
    )

    val currentOnSelectNode by rememberUpdatedState(
        onSelectNode
    )

    val currentOnMoveNode by rememberUpdatedState(
        onMoveNode
    )

    val currentOnSelectConnection by rememberUpdatedState(
        onSelectConnection
    )

    val currentOnEditNode by rememberUpdatedState(
        onEditNode
    )

    val currentOnEditConnection by rememberUpdatedState(
        onEditConnection
    )

    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    Canvas(
        modifier = modifier
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
                            currentOnSelectNode(
                                node.id
                            )
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

                        draggedNodeId =
                            node?.id

                        if (node != null) {
                            currentOnSelectNode(
                                node.id
                            )
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val id =
                            draggedNodeId

                        if (id != null) {
                            val node =
                                currentNodes.firstOrNull {
                                    it.id == id
                                }

                            if (node != null) {
                                currentOnMoveNode(
                                    id,
                                    node.x +
                                        dragAmount.x,
                                    node.y +
                                        dragAmount.y
                                )
                            }
                        }

                        change.consume()
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
                    it.id ==
                        connection.fromNodeId
                }

            val to =
                nodes.firstOrNull {
                    it.id ==
                        connection.toNodeId
                }

            if (from != null && to != null) {
                drawProfessionalConnection(
                    from = from,
                    to = to,
                    connection = connection,
                    selected =
                        connection.id ==
                            selectedConnectionId
                )
            }
        }

        nodes.forEach { node ->
            drawProfessionalNode(
                node = node,
                selected =
                    node.id ==
                        selectedNodeId,
                connectionStart =
                    node.id ==
                        connectionStartId,
                fault =
                    faultByNode[node.id]
            )
        })
    }
}

private fun DrawScope.drawGrid() {
    val step = 40f

    var x = 0f

    while (x < size.width) {
        drawLine(
            color = Color(0xFF101A21),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )

        x += step
    }

    var y = 0f

    while (y < size.height) {
        drawLine(
            color = Color(0xFF101A21),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )

        y += step
    }
}

private fun DrawScope.drawProfessionalConnection(
    from: SldNode,
    to: SldNode,
    connection: SldConnection,
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
        if (selected) {
            Accent
        } else {
            CableColor
        }

    val middleX =
        (start.x + end.x) / 2f

    val path =
        Path().apply {
            moveTo(
                start.x,
                start.y
            )

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
        style =
            Stroke(
                width =
                    if (selected) {
                        5f
                    } else {
                        3f
                    }
            )
    )

    drawLine(
        color = color,
        start =
            Offset(
                end.x - 12f,
                end.y - 6f
            ),
        end =
            Offset(
                end.x,
                end.y
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = color,
        start =
            Offset(
                end.x - 12f,
                end.y + 6f
            ),
        end =
            Offset(
                end.x,
                end.y
            ),
        strokeWidth = 3f
    )

    if (
        connection.cableSizeMm2 > 0.0 ||
        connection.parallelRuns > 1
    ) {
        drawConnectionLabel(
            connection = connection,
            x = middleX,
            y =
                (start.y + end.y) /
                    2f - 8f
        )
    }
}

private fun DrawScope.drawConnectionLabel(
    connection: SldConnection,
    x: Float,
    y: Float
) {
    val paint =
        android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.WHITE
            textSize = 12f
            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
        }

    val cableText =
        if (connection.cableSizeMm2 > 0.0) {
            "${number(connection.cableSizeMm2)} mm²"
        } else {
            "AUTO"
        }

    val runsText =
        "${max(1, connection.parallelRuns)} run"

    val text =
        "$cableText × $runsText"

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            text,
            x + 6f,
            y,
            paint
        )
    }
}

private fun DrawScope.drawProfessionalNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    fault: FaultDisplay?
) {
    val left = node.x
    val top = node.y

    when (node.type) {
        SldNodeType.BUS -> {
            drawBusbarNode(
                node = node,
                selected = selected
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawTransformerNode(
                node = node,
                selected = selected
            )
        }

        SldNodeType.GENERATOR -> {
            drawGeneratorNode(
                node = node,
                selected = selected
            )
        }

        SldNodeType.BREAKER -> {
            drawBreakerNode(
                node = node,
                selected = selected
            )
        }

        SldNodeType.SOURCE -> {
            drawSourceNode(
                node = node,
                selected = selected
            )
        }

        SldNodeType.PANEL -> {
            drawPanelNode(
                node = node,
                selected = selected
            )
        }

        SldNodeType.LOAD -> {
            drawLoadNode(
                node = node,
                selected = selected
            )
        }
    }

    if (connectionStart) {
        drawRoundRect(
            color = Accent,
            topLeft =
                Offset(
                    left - 5f,
                    top - 5f
                ),
            size =
                Size(
                    NODE_WIDTH + 10f,
                    NODE_HEIGHT + 10f
                ),
            cornerRadius =
                CornerRadius(
                    12f,
                    12f
                ),
            style =
                Stroke(4f)
        )
    }

    drawNodeInformation(
        node = node,
        fault = fault
    )
}

private fun DrawScope.drawSourceNode(
    node: SldNode,
    selected: Boolean
) {
    val color =
        if (selected) {
            Accent
        } else {
            Color(0xFF1565C0)
        }

    drawRoundRect(
        color = color,
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
                12f,
                12f
            )
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                node.x + 25f,
                node.y + 18f
            ),
        end =
            Offset(
                node.x + 25f,
                node.y + 52f
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                node.x + 15f,
                node.y + 35f
            ),
        end =
            Offset(
                node.x + 35f,
                node.y + 35f
            ),
        strokeWidth = 3f
    )

    drawNodeText(
        node.name,
        node.x + 45f,
        node.y + 27f
    )

    drawNodeText(
        "SOURCE",
        node.x + 45f,
        node.y + 47f,
        small = true
    )
}

private fun DrawScope.drawTransformerNode(
    node: SldNode,
    selected: Boolean
) {
    val color =
        if (selected) {
            Accent
        } else {
            Color(0xFFEF6C00)
        }

    drawRoundRect(
        color = color,
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
                12f,
                12f
            )
    )

    drawCircle(
        color = Color.White,
        radius = 17f,
        center =
            Offset(
                node.x + 43f,
                node.y + 43f
            ),
        style =
            Stroke(3f)
    )

    drawCircle(
        color = Color.White,
        radius = 17f,
        center =
            Offset(
                node.x + 65f,
                node.y + 43f
            ),
        style =
            Stroke(3f)
    )

    drawNodeText(
        node.name,
        node.x + 82f,
        node.y + 25f
    )

    drawNodeText(
        "TR",
        node.x + 82f,
        node.y + 45f,
        small = true
    )
}

private fun DrawScope.drawGeneratorNode(
    node: SldNode,
    selected: Boolean
) {
    val color =
        if (selected) {
            Accent
        } else {
            Color(0xFF2E7D32)
        }

    drawRoundRect(
        color = color,
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
                12f,
                12f
            )
    )

    drawCircle(
        color = Color.White,
        radius = 24f,
        center =
            Offset(
                node.x + 35f,
                node.y + 43f
            ),
        style =
            Stroke(3f)
    )

    drawNodeText(
        "G",
        node.x + 29f,
        node.y + 49f
    )

    drawNodeText(
        node.name,
        node.x + 67f,
        node.y + 28f
    )

    drawNodeText(
        "GENERATOR",
        node.x + 67f,
        node.y + 48f,
        small = true
    )
}

private fun DrawScope.drawBreakerNode(
    node: SldNode,
    selected: Boolean
) {
    val color =
        if (selected) {
            Accent
        } else {
            Color(0xFF455A64)
        }

    drawRoundRect(
        color = color,
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
                12f,
                12f
            )
    )

    val cx = node.x + 35f
    val cy = node.y + 43f

    drawLine(
        color = Color.White,
        start =
            Offset(
                cx - 15f,
                cy
            ),
        end =
            Offset(
                cx - 4f,
                cy
            ),
        strokeWidth = 4f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                cx + 4f,
                cy
            ),
        end =
            Offset(
                cx + 15f,
                cy
            ),
        strokeWidth = 4f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                cx - 4f,
                cy
            ),
        end =
            Offset(
                cx + 5f,
                cy - 14f
            ),
        strokeWidth = 3f
    )

    drawNodeText(
        node.name,
        node.x + 58f,
        node.y + 26f
    )

    drawNodeText(
        "BREAKER",
        node.x + 58f,
        node.y + 45f,
        small = true
    )
}

private fun DrawScope.drawPanelNode(
    node: SldNode,
    selected: Boolean
) {
    val color =
        if (selected) {
            Accent
        } else {
            Color(0xFF00838F)
        }

    drawRoundRect(
        color = color,
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

    drawRoundRect(
        color = Color.White,
        topLeft =
            Offset(
                node.x + 15f,
                node.y + 15f
            ),
        size =
            Size(
                35f,
                52f
            ),
        cornerRadius =
            CornerRadius(
                3f,
                3f
            ),
        style =
            Stroke(2f)
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                node.x + 22f,
                node.y + 28f
            ),
        end =
            Offset(
                node.x + 43f,
                node.y + 28f
            ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                node.x + 22f,
                node.y + 39f
            ),
        end =
            Offset(
                node.x + 43f,
                node.y + 39f
            ),
        strokeWidth = 2f
    )

    drawLine(
        color = Color.White,
        start =
            Offset(
                node.x + 22f,
                node.y + 50f
            ),
        end =
            Offset(
                node.x + 43f,
                node.y + 50f
            ),
        strokeWidth = 2f
    )

    drawNodeText(
        node.name,
        node.x + 60f,
        node.y + 27f
    )

    drawNodeText(
        "PANEL",
        node.x + 60f,
        node.y + 47f,
        small = true
    )
}

private fun DrawScope.drawLoadNode(
    node: SldNode,
    selected: Boolean
) {
    val color =
        if (selected) {
            Accent
        } else {
            Color(0xFF37474F)
        }

    drawRoundRect(
        color = color,
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
                12f,
                12f
            )
    )

    val path =
        Path().apply {
            moveTo(
                node.x + 18f,
                node.y + 56f
            )

            lineTo(
                node.x + 35f,
                node.y + 25f
            )

            lineTo(
                node.x + 52f,
                node.y + 56f
            )

            close()
        }

    drawPath(
        path = path,
        color = Color.White
    )

    drawNodeText(
        node.name,
        node.x + 65f,
        node.y + 27f
    )

    drawNodeText(
        "LOAD",
        node.x + 65f,
        node.y + 47f,
        small = true
    )
}

private fun DrawScope.drawBusbarNode(
    node: SldNode,
    selected: Boolean
) {
    val color =
        if (selected) {
            Accent
        } else {
            BusbarColor
        }

    val y =
        node.y +
            NODE_HEIGHT / 2f -
            BUSBAR_HEIGHT / 2f

    drawRoundRect(
        color = color,
        topLeft =
            Offset(
                node.x,
                y
            ),
        size =
            Size(
                BUSBAR_WIDTH,
                BUSBAR_HEIGHT
            ),
        cornerRadius =
            CornerRadius(
                5f,
                5f
            )
    )

    drawNodeText(
        node.name,
        node.x,
        node.y + 20f
    )

    drawNodeText(
        "BUSBAR",
        node.x,
        node.y + 72f,
        small = true
    )
}

private fun DrawScope.drawNodeInformation(
    node: SldNode,
    fault: FaultDisplay?
) {
    val lines =
        mutableListOf<String>()

    when (node.type) {
        SldNodeType.SOURCE -> {
            lines +=
                "V = ${number(node.voltage)} V"

            lines +=
                "Ssc = ${number(node.sourceShortCircuitMva)} MVA"
        }

        SldNodeType.TRANSFORMER -> {
            lines +=
                "V = ${number(node.voltage)} V"

            lines +=
                "S = ${number(node.ratedKva)} kVA"

            lines +=
                "%Z = ${number(node.transformerPercentZ)} %"
        }

        SldNodeType.GENERATOR -> {
            lines +=
                "V = ${number(node.voltage)} V"

            lines +=
                "S = ${number(node.ratedKva)} kVA"

            lines +=
                "Xd'' = ${number(node.generatorXdSubtransient)} %"
        }

        SldNodeType.BREAKER -> {
            if (fault != null) {
                lines +=
                    "Icu ≥ ${number(fault.icu)} kA"

                lines +=
                    "Ics ≥ ${number(fault.ics)} kA"
            } else {
                lines +=
                    "Icu = AUTO"

                lines +=
                    "Ics = AUTO"
            }
        }

        SldNodeType.PANEL -> {
            lines +=
                "V = ${number(node.voltage)} V"

            if (node.loadKw > 0.0) {
                lines +=
                    "Load = ${number(node.loadKw)} kW"
            }
        }

        SldNodeType.LOAD -> {
            val current =
                calculateLoadCurrent(
                    node
                )

            lines +=
                "P = ${number(node.loadKw)} kW"

            lines +=
                "I = ${number(current)} A"
        }

        SldNodeType.BUS -> {
            lines +=
                "V = ${number(node.voltage)} V"
        }
    }

    if (fault != null) {
        lines +=
            "Ik'' = ${number(fault.ik)} kA"
    }

    val x =
        node.x + NODE_WIDTH + 8f

    val y =
        node.y + 12f

    drawIntoCanvas { canvas ->
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.LTGRAY
                textSize = 11f
                typeface =
                    android.graphics.Typeface.DEFAULT
            }

        lines.forEachIndexed { index, text ->
            canvas.nativeCanvas.drawText(
                text,
                x,
                y + index * 15f,
                paint
            )
        }
    }
}

private fun DrawScope.drawNodeText(
    text: String,
    x: Float,
    y: Float,
    small: Boolean = false
) {
    drawIntoCanvas { canvas ->
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.WHITE

                textSize =
                    if (small) {
                        10f
                    } else {
                        13f
                    }

                typeface =
                    if (small) {
                        android.graphics.Typeface.DEFAULT
                    } else {
                        android.graphics.Typeface.DEFAULT_BOLD
                    }
            }

        canvas.nativeCanvas.drawText(
            text.take(22),
            x,
            y,
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
                node.x -
                    NODE_TOUCH_PADDING &&
                position.x <=
                node.x +
                    NODE_WIDTH +
                    NODE_TOUCH_PADDING &&
                position.y >=
                node.y -
                    NODE_TOUCH_PADDING &&
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
                it.id ==
                    connection.fromNodeId
            }

        val to =
            nodes.firstOrNull {
                it.id ==
                    connection.toNodeId
            }

        if (from == null || to == null) {
            false
        } else {
            val start =
                Offset(
                    from.x +
                        NODE_WIDTH / 2f,
                    from.y +
                        NODE_HEIGHT / 2f
                )

            val end =
                Offset(
                    to.x +
                        NODE_WIDTH / 2f,
                    to.y +
                        NODE_HEIGHT / 2f
                )

            distanceToSegment(
                position,
                start,
                end
            ) <= 22f
        }
    }
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

    if (
        dx == 0f &&
        dy == 0f
    ) {
        return hypot(
            p.x - a.x,
            p.y - a.y
        )
    }

    val t =
        (
            (
                (p.x - a.x) * dx +
                    (p.y - a.y) * dy
                ) /
                (
                    dx * dx +
                        dy * dy
                    )
            )
            .coerceIn(
                0f,
                1f
            )

    val x =
        a.x +
            t * dx

    val y =
        a.y +
            t * dy

    return hypot(
        p.x - x,
        p.y - y
    )
}

private fun calculateLoadCurrent(
    node: SldNode
): Double {
    val pf =
        node.powerFactor
            .coerceIn(
                0.01,
                1.0
            )

    val demand =
        node.demandFactor
            .coerceIn(
                0.0,
                1.0
            )

    val voltage =
        node.voltage
            .coerceAtLeast(
                1.0
            )

    return (
        node.loadKw *
            demand *
            1000.0
        ) /
            (
                1.7320508075688772 *
                    voltage *
                    pf
                )
}

private data class FaultDisplay(
    val ik: Double,
    val ip: Double,
    val ith: Double,
    val faultMva: Double,
    val xr: Double,
    val voltage: Double,
    val icu: Double,
    val ics: Double
)

private fun extractShortCircuitMap(
    study: Any?
): Map<String, FaultDisplay> {
    if (study == null) {
        return emptyMap()
    }

    val results =
        readProperty(
            study,
            "results"
        ) as? Iterable<*>
            ?: return emptyMap()

    val output =
        mutableMapOf<String, FaultDisplay>()

    results.forEach { item ->
        if (item == null) {
            return@forEach
        }

        val nodeId =
            readProperty(
                item,
                "nodeId"
            )?.toString()
                ?: return@forEach

        val ik =
            readDouble(
                item,
                "initialSymmetricalCurrentKa",
                "ikPrimeDoubleKa",
                "ikKa",
                "initialCurrentKa"
            )

        val ip =
            readDouble(
                item,
                "peakCurrentKa",
                "ipKa",
                "maximumPeakCurrentKa"
            )

        val ith =
            readDouble(
                item,
                "thermalCurrentKa",
                "ithKa",
                "maximumThermalCurrentKa"
            )

        val faultMva =
            readDouble(
                item,
                "shortCircuitMva",
                "faultLevelMva",
                "maximumFaultMva"
            )

        val xr =
            readDouble(
                item,
                "xrRatio",
                "xRRatio"
            )

        val voltage =
            readDouble(
                item,
                "voltageV",
                "voltage"
            )

        val breaker =
            readDouble(
                item,
                "breakerRequiredShortCircuitRatingKa",
                "requiredBreakerKa",
                "breakerRatingKa"
            )

        val icu =
            if (breaker > 0.0) {
                selectBreakerRating(
                    breaker
                )
            } else {
                selectBreakerRating(
                    max(
                        ik,
                        ith
                    )
                )
            }

        val ics =
            if (icu > 0.0) {
                icu * 0.75
            } else {
                0.0
            }

        output[nodeId] =
            FaultDisplay(
                ik = ik,
                ip = ip,
                ith = ith,
                faultMva = faultMva,
                xr = xr,
                voltage = voltage,
                icu = icu,
                ics = ics
            )
    }

    return output
}

private fun selectBreakerRating(
    faultKa: Double
): Double {
    if (faultKa <= 0.0) {
        return 0.0
    }

    val ratings =
        listOf(
            6.0,
            10.0,
            16.0,
            25.0,
            36.0,
            50.0,
            65.0,
            80.0,
            100.0,
            120.0,
            150.0,
            200.0,
            250.0,
            300.0,
            400.0,
            500.0,
            630.0,
            800.0
        )

    return ratings.firstOrNull {
        it >= faultKa
    } ?: faultKa
}

private fun formatNetworkSummary(
    network: SldNetwork,
    arabic: Boolean
): String {
    return buildString {
        network.nodes.forEach { node ->
            appendLine(
                "${node.name} | " +
                    "${node.type.name} | " +
                    "V=${number(node.voltage)} V"
            )

            when (node.type) {
                SldNodeType.SOURCE -> {
                    appendLine(
                        "  Ssc = " +
                            "${number(node.sourceShortCircuitMva)} MVA"
                    )
                }

                SldNodeType.TRANSFORMER -> {
                    appendLine(
                        "  S = " +
                            "${number(node.ratedKva)} kVA | " +
                            "%Z = " +
                            "${number(node.transformerPercentZ)} %"
                    )
                }

                SldNodeType.GENERATOR -> {
                    appendLine(
                        "  S = " +
                            "${number(node.ratedKva)} kVA | " +
                            "Xd'' = " +
                            "${number(node.generatorXdSubtransient)} %"
                    )
                }

                SldNodeType.LOAD -> {
                    appendLine(
                        "  P = " +
                            "${number(node.loadKw)} kW | " +
                            "I = " +
                            "${number(calculateLoadCurrent(node))} A"
                    )
                }

                else -> Unit
            }
        }

        appendLine()
        appendLine(
            if (arabic) {
                "عدد العناصر = ${network.nodes.size}"
            } else {
                "Elements = ${network.nodes.size}"
            }
        )

        appendLine(
            if (arabic) {
                "عدد التوصيلات = ${network.connections.size}"
            } else {
                "Connections = ${network.connections.size}"
            }
        )
    }
}

private fun formatEngineeringResult(
    result: Any
): String {
    val results =
        readProperty(
            result,
            "results"
        ) as? Iterable<*>

    if (results != null) {
        return buildString {
            results.forEachIndexed { index, item ->
                if (item != null) {
                    appendLine(
                        "────────────────────────"
                    )

                    appendLine(
                        "POINT ${index + 1}"
                    )

                    appendLine(
                        formatShortCircuitPoint(
                            item
                        )
                    )
                }
            }

            if (isEmpty()) {
                appendLine(
                    formatStudyObject(
                        result
                    )
                )
            }
        }.trim()
    }

    return formatStudyObject(
        result
    )
}

private fun formatShortCircuitPoint(
    item: Any
): String {
    val nodeName =
        readProperty(
            item,
            "nodeName"
        )?.toString()
            ?: readProperty(
                item,
                "name"
            )?.toString()
            ?: readProperty(
                item,
                "nodeId"
            )?.toString()
            ?: "UNKNOWN"

    val voltage =
        readDouble(
            item,
            "voltageV",
            "voltage"
        )

    val ik =
        readDouble(
            item,
            "initialSymmetricalCurrentKa",
            "ikPrimeDoubleKa",
            "ikKa",
            "initialCurrentKa"
        )

    val ip =
        readDouble(
            item,
            "peakCurrentKa",
            "ipKa",
            "maximumPeakCurrentKa"
        )

    val ith =
        readDouble(
            item,
            "thermalCurrentKa",
            "ithKa",
            "maximumThermalCurrentKa"
        )

    val faultMva =
        readDouble(
            item,
            "shortCircuitMva",
            "faultLevelMva",
            "maximumFaultMva"
        )

    val xr =
        readDouble(
            item,
            "xrRatio",
            "xRRatio"
        )

    val breaker =
        readDouble(
            item,
            "breakerRequiredShortCircuitRatingKa",
            "requiredBreakerKa",
            "breakerRatingKa"
        )

    val icu =
        if (breaker > 0.0) {
            selectBreakerRating(
                breaker
            )
        } else {
            selectBreakerRating(
                max(
                    ik,
                    ith
                )
            )
        }

    val ics =
        if (icu > 0.0) {
            icu * 0.75
        } else {
            0.0
        }

    return buildString {
        appendLine(
            "Bus / Point : $nodeName"
        )

        appendLine(
            "Voltage     : ${number(voltage)} V"
        )

        appendLine(
            "Ik''        : ${number(ik)} kA"
        )

        appendLine(
            "Ip peak     : ${number(ip)} kA"
        )

        appendLine(
            "Ith         : ${number(ith)} kA"
        )

        appendLine(
            "Fault MVA   : ${number(faultMva)} MVA"
        )

        appendLine(
            "X/R         : ${number(xr)}"
        )

        appendLine(
            "Breaker Icu : ${number(icu)} kA"
        )

        appendLine(
            "Breaker Ics : ${number(ics)} kA"
        )
    }
}

private fun formatStudyObject(
    value: Any
): String {
    val fields =
        value.javaClass.declaredFields

    val useful =
        fields.filter {
            !it.isSynthetic &&
                it.name != "Companion"
        }

    if (useful.isEmpty()) {
        return value.toString()
    }

    return buildString {
        useful.forEach { field ->
            try {
                field.isAccessible = true

                val fieldValue =
                    field.get(value)

                if (
                    fieldValue is Iterable<*>
                ) {
                    return@forEach
                }

                appendLine(
                    "${humanize(field.name)} : " +
                        formatValue(fieldValue)
                )
            } catch (_: Exception) {
                Unit
            }
        }
    }.trim().ifBlank {
        value.toString()
    }
}

private fun readProperty(
    value: Any,
    vararg names: String
): Any? {
    names.forEach { name ->
        try {
            val field =
                value.javaClass
                    .declaredFields
                    .firstOrNull {
                        it.name == name
                    }

            if (field != null) {
                field.isAccessible = true
                return field.get(value)
            }
        } catch (_: Exception) {
            Unit
        }

        try {
            val getterName =
                "get" +
                    name.replaceFirstChar {
                        it.uppercase()
                    }

            val method =
                value.javaClass.methods
                    .firstOrNull {
                        it.name ==
                            getterName &&
                            it.parameterTypes.isEmpty()
                    }

            if (method != null) {
                return method.invoke(value)
            }
        } catch (_: Exception) {
            Unit
        }
    }

    return null
}

private fun readDouble(
    value: Any,
    vararg names: String
): Double {
    val raw =
        readProperty(
            value,
            *names
        )

    return when (raw) {
        is Number -> raw.toDouble()
        else ->
            raw?.toString()
                ?.toDoubleOrNull()
                ?: 0.0
    }
}

private fun humanize(
    value: String
): String {
    return value
        .replace(
            Regex("([a-z])([A-Z])"),
            "$1 $2"
        )
        .replace(
            "_",
            " "
        )
        .replaceFirstChar {
            it.uppercase()
        }
}

private fun formatValue(
    value: Any?
): String {
    return when (value) {
        null -> "-"
        is Double ->
            number(value)

        is Float ->
            number(value.toDouble())

        is Number ->
            value.toString()

        else ->
            value.toString()
    }
}

private fun number(
    value: Double
): String {
    if (!value.isFinite()) {
        return "0"
    }

    return if (
        kotlin.math.abs(value) >= 1000.0
    ) {
        String.format(
            "%.1f",
            value
        )
    } else {
        String.format(
            "%.3f",
            value
        ).trimEnd('0')
            .trimEnd('.')
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
            }
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
                            "اسم العنصر"
                        } else {
                            "Element Name"
                        },
                    onValueChange = onName
                )

                when (type) {
                    SldNodeType.SOURCE -> {
                        EditorField(
                            value = voltage,
                            label =
                                if (arabic) {
                                    "جهد المصدر V"
                                } else {
                                    "Source Voltage V"
                                },
                            onValueChange =
                                onVoltage
                        )

                        EditorField(
                            value = sourceMva,
                            label =
                                if (arabic) {
                                    "قدرة القصر Ssc MVA"
                                } else {
                                    "Source Fault Level Ssc MVA"
                                },
                            onValueChange =
                                onSourceMva
                        )
                    }

                    SldNodeType.TRANSFORMER -> {
                        EditorField(
                            value = voltage,
                            label =
                                if (arabic) {
                                    "جهد المحول V"
                                } else {
                                    "Transformer Voltage V"
                                },
                            onValueChange =
                                onVoltage
                        )

                        EditorField(
                            value = kva,
                            label =
                                if (arabic) {
                                    "قدرة المحول kVA"
                                } else {
                                    "Transformer Rating kVA"
                                },
                            onValueChange =
                                onKva
                        )

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

                    SldNodeType.GENERATOR -> {
                        EditorField(
                            value = voltage,
                            label =
                                if (arabic) {
                                    "جهد المولد V"
                                } else {
                                    "Generator Voltage V"
                                },
                            onValueChange =
                                onVoltage
                        )

                        EditorField(
                            value = kva,
                            label =
                                if (arabic) {
                                    "قدرة المولد kVA"
                                } else {
                                    "Generator Rating kVA"
                                },
                            onValueChange =
                                onKva
                        )

                        EditorField(
                            value = generatorXd,
                            label =
                                if (arabic) {
                                    "Xd'' %"
                                } else {
                                    "Xd'' %"
                                },
                            onValueChange =
                                onGeneratorXd
                        )
                    }

                    SldNodeType.LOAD -> {
                        EditorField(
                            value = voltage,
                            label =
                                if (arabic) {
                                    "جهد الحمل V"
                                } else {
                                    "Load Voltage V"
                                },
                            onValueChange =
                                onVoltage
                        )

                        EditorField(
                            value = kw,
                            label =
                                if (arabic) {
                                    "الحمل kW"
                                } else {
                                    "Load kW"
                                },
                            onValueChange =
                                onKw
                        )

                        EditorField(
                            value = pf,
                            label =
                                if (arabic) {
                                    "معامل القدرة"
                                } else {
                                    "Power Factor"
                                },
                            onValueChange =
                                onPf
                        )

                        EditorField(
                            value = demand,
                            label =
                                if (arabic) {
                                    "معامل الطلب"
                                } else {
                                    "Demand Factor"
                                },
                            onValueChange =
                                onDemand
                        )
                    }

                    SldNodeType.PANEL -> {
                        EditorField(
                            value = voltage,
                            label =
                                if (arabic) {
                                    "جهد اللوحة V"
                                } else {
                                    "Panel Voltage V"
                                },
                            onValueChange =
                                onVoltage
                        )
                    }

                    SldNodeType.BUS -> {
                        EditorField(
                            value = voltage,
                            label =
                                if (arabic) {
                                    "جهد الباسبار V"
                                } else {
                                    "Bus Voltage V"
                                },
                            onValueChange =
                                onVoltage
                        )
                    }

                    SldNodeType.BREAKER -> {
                        EditorField(
                            value = voltage,
                            label =
                                if (arabic) {
                                    "جهد القاطع V"
                                } else {
                                    "Breaker Voltage V"
                                },
                            onValueChange =
                                onVoltage
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
                            "الطول m"
                        } else {
                            "Length m"
                        },
                    onValueChange =
                        onLength
                )

                EditorField(
                    value = resistance,
                    label =
                        if (arabic) {
                            "المقاومة Ω/km"
                        } else {
                            "Resistance Ω/km"
                        },
                    onValueChange =
                        onResistance
                )

                EditorField(
                    value = reactance,
                    label =
                        if (arabic) {
                            "المفاعلة Ω/km"
                        } else {
                            "Reactance Ω/km"
                        },
                    onValueChange =
                        onReactance
                )

                EditorField(
                    value = cableSize,
                    label =
                        if (arabic) {
                            "مقطع الكابل mm²"
                        } else {
                            "Cable Section mm²"
                        },
                    onValueChange =
                        onCableSize
                )

                EditorField(
                    value = runs,
                    label =
                        if (arabic) {
                            "عدد المسارات"
                        } else {
                            "Parallel Runs"
                        },
                    onValueChange =
                        onRuns
                )

                EditorField(
                    value = capacity,
                    label =
                        if (arabic) {
                            "سعة التيار A"
                        } else {
                            "Current Capacity A"
                        },
                    onValueChange =
                        onCapacity
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
        modifier =
            Modifier.fillMaxWidth(),
        value = value,
        onValueChange =
            onValueChange,
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
        modifier =
            Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
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
            modifier =
                Modifier
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
                            item.name
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
