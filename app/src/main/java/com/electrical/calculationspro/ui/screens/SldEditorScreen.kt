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
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
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
                    x = 50f,
                    y = 80f,
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

    var editingNodeId by remember {
        mutableStateOf<String?>(null)
    }

    var editingConnectionId by remember {
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
            val lastX = nodes.maxOfOrNull { it.x } ?: 50f
            val lastY = nodes.lastOrNull()?.y ?: 80f

            val newNode = SldNode(
                id = "node-${System.nanoTime()}",
                name = nodeName.ifBlank { defaultName(pendingNodeType) },
                type = pendingNodeType,
                x = lastX + 210f,
                y = lastY,
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
                        type = pendingNodeType,
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
        val length = (connectionLength.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        val resistance = connectionResistance.toDoubleOrNull() ?: 0.0
        val reactance = connectionReactance.toDoubleOrNull() ?: 0.0
        val size = connectionCableSize.toDoubleOrNull() ?: 0.0
        val runs = (connectionParallelRuns.toIntOrNull() ?: 1).coerceAtLeast(1)
        val capacity = connectionCurrentCapacity.toDoubleOrNull() ?: 0.0

        if (editingConnectionId == null) {
            val from = connectionStartId ?: return
            val to = selectedNodeId ?: return

            if (from == to) return

            val exists = connections.any {
                (it.fromNodeId == from && it.toNodeId == to) ||
                    (it.fromNodeId == to && it.toNodeId == from)
            }

            if (!exists) {
                val c = SldConnection(
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

                connections = connections + c
                selectedConnectionId = c.id
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
            val c = SldConnection(
                id = "connection-${System.nanoTime()}",
                fromNodeId = from,
                toNodeId = selected,
                lengthMeters = 10.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080
            )

            connections = connections + c
            selectedConnectionId = c.id
        }

        connectionStartId = null
        selectedNodeId = null
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

    fun shortCircuitStudy(): SldShortCircuitStudy =
        SldShortCircuitEngine.calculate(
            network = network(),
            voltageFactor = 1.05
        )

    fun formatShortCircuit(study: SldShortCircuitStudy): String {
        val sb = StringBuilder()

        sb.appendLine(
            if (arabic)
                "تقرير دراسة تيارات القصر"
            else
                "SHORT-CIRCUIT ENGINEERING REPORT"
        )

        sb.appendLine("=".repeat(58))
        sb.appendLine()

        sb.appendLine(
            "Maximum Ik'' = %.3f kA".format(
                study.maximumFaultCurrentKa
            )
        )

        sb.appendLine(
            "Maximum Ip = %.3f kA".format(
                study.maximumPeakCurrentKa
            )
        )

        sb.appendLine(
            "Maximum Fault MVA = %.3f MVA".format(
                study.maximumFaultMva
            )
        )

        sb.appendLine()

        study.results.values.forEachIndexed { index, result ->

            sb.appendLine(
                "${index + 1}. ${result.nodeName}"
            )
            sb.appendLine("-".repeat(58))

            sb.appendLine(
                "Bus Voltage       : %.1f V".format(
                    result.voltageV
                )
            )

            sb.appendLine(
                "Ik'' Initial       : %.3f kA".format(
                    result.initialSymmetricalCurrentKa
                )
            )

            sb.appendLine(
                "Ip Peak            : %.3f kA".format(
                    result.peakCurrentKa
                )
            )

            sb.appendLine(
                "Ith Thermal        : %.3f kA".format(
                    result.thermalCurrentKa
                )
            )

            sb.appendLine(
                "Fault MVA          : %.3f MVA".format(
                    result.shortCircuitMva
                )
            )

            sb.appendLine(
                "X/R                : %.3f".format(
                    result.xrRatio
                )
            )

            sb.appendLine(
                "R                  : %.6f Ω".format(
                    result.resistanceOhm
                )
            )

            sb.appendLine(
                "X                  : %.6f Ω".format(
                    result.reactanceOhm
                )
            )

            sb.appendLine(
                "Z                  : %.6f Ω".format(
                    result.impedanceOhm
                )
            )

            sb.appendLine(
                "Breaker Icu        : %.1f kA".format(
                    result.breakerRequiredKa
                )
            )

            sb.appendLine(
                "Breaker Ics        : %.1f kA".format(
                    result.breakerRequiredKa * 0.75
                )
            )

            sb.appendLine()
        }

        return sb.toString()
    }

    fun nodeResult(
        nodeId: String,
        study: SldShortCircuitStudy
    ): SldShortCircuitResult? =
        study.results[nodeId]

    fun loadCurrent(node: SldNode): Double {
        if (node.loadKw <= 0.0) return 0.0

        val pf = node.powerFactor.coerceIn(0.01, 1.0)
        val voltage = node.voltage.coerceAtLeast(1.0)

        return node.loadKw * 1000.0 /
            (1.7320508075688772 * voltage * pf)
    }

    fun standardBreaker(currentA: Double): Double {
        val values = listOf(
            6.0,
            10.0,
            16.0,
            20.0,
            25.0,
            32.0,
            40.0,
            50.0,
            63.0,
            80.0,
            100.0,
            125.0,
            160.0,
            200.0,
            250.0,
            315.0,
            400.0,
            500.0,
            630.0,
            800.0,
            1000.0,
            1250.0,
            1600.0,
            2000.0,
            2500.0,
            3200.0,
            4000.0,
            5000.0,
            6300.0
        )

        return values.firstOrNull { it >= currentA }
            ?: 6300.0
    }

    fun cableCurrent(connection: SldConnection): Double {
        if (connection.currentCapacityA > 0.0) {
            return connection.currentCapacityA
        }

        return 0.0
    }

    fun generateCompleteSld() {

        val source = nodes.firstOrNull {
            it.type == SldNodeType.SOURCE
        } ?: return

        var x = source.x + 230f
        val y = source.y

        val orderedTypes = listOf(
            SldNodeType.TRANSFORMER,
            SldNodeType.GENERATOR,
            SldNodeType.BUS,
            SldNodeType.BREAKER,
            SldNodeType.PANEL,
            SldNodeType.LOAD
        )

        var previousId = source.id

        orderedTypes.forEach { type ->

            val existing = nodes.firstOrNull {
                it.type == type &&
                    it.id != source.id
            }

            val node = existing ?: run {

                val n = when (type) {
                    SldNodeType.TRANSFORMER ->
                        SldNode(
                            id = "auto-transformer-${System.nanoTime()}",
                            name = "TR-1",
                            type = type,
                            x = x,
                            y = y,
                            voltage = 400.0,
                            ratedKva = 1000.0,
                            transformerPercentZ = 6.0
                        )

                    SldNodeType.GENERATOR ->
                        SldNode(
                            id = "auto-generator-${System.nanoTime()}",
                            name = "GEN-1",
                            type = type,
                            x = x,
                            y = y,
                            voltage = 400.0,
                            ratedKva = 500.0,
                            generatorXdSubtransient = 15.0
                        )

                    SldNodeType.BUS ->
                        SldNode(
                            id = "auto-bus-${System.nanoTime()}",
                            name = "BUS-1",
                            type = type,
                            x = x,
                            y = y,
                            voltage = 400.0
                        )

                    SldNodeType.BREAKER ->
                        SldNode(
                            id = "auto-breaker-${System.nanoTime()}",
                            name = "CB-1",
                            type = type,
                            x = x,
                            y = y,
                            voltage = 400.0
                        )

                    SldNodeType.PANEL ->
                        SldNode(
                            id = "auto-panel-${System.nanoTime()}",
                            name = "PANEL-1",
                            type = type,
                            x = x,
                            y = y,
                            voltage = 400.0
                        )

                    SldNodeType.LOAD ->
                        SldNode(
                            id = "auto-load-${System.nanoTime()}",
                            name = "LOAD-1",
                            type = type,
                            x = x,
                            y = y,
                            voltage = 400.0,
                            loadKw = 50.0,
                            powerFactor = 0.90,
                            demandFactor = 1.0
                        )

                    else -> return@forEach
                }

                nodes = nodes + n
                n
            }

            if (connections.none {
                    it.fromNodeId == previousId &&
                        it.toNodeId == node.id
                }
            ) {
                connections = connections + SldConnection(
                    id = "auto-connection-${System.nanoTime()}",
                    fromNodeId = previousId,
                    toNodeId = node.id,
                    lengthMeters = 10.0,
                    resistanceOhmPerKm = 0.125,
                    reactanceOhmPerKm = 0.080,
                    cableSizeMm2 = 240.0,
                    parallelRuns = 1
                )
            }

            previousId = node.id
            x += 230f
        }

        selectedNodeId = null
        selectedConnectionId = null
        connectionStartId = null

        showCompleteDialog = true
    }

    val study = remember(
        nodes,
        connections
    ) {
        runCatching {
            SldShortCircuitEngine.calculate(
                SldNetwork(
                    nodes = nodes,
                    connections = connections
                ),
                1.05
            )
        }.getOrNull()
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
                TextButton(onClick = it) {
                    Text(
                        text = if (arabic) "رجوع" else "Back",
                        color = Accent
                    )
                }
            }

            Text(
                modifier = Modifier.weight(1f),
                text = if (arabic)
                    "المخطط الأحادي الاحترافي SLD"
                else
                    "Professional Single Line Diagram",
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
                .padding(horizontal = 10.dp),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            SldToolButton(
                "Source"
            ) {
                openAdd(SldNodeType.SOURCE)
            }

            SldToolButton(
                "Transformer"
            ) {
                openAdd(SldNodeType.TRANSFORMER)
            }

            SldToolButton(
                "Generator"
            ) {
                openAdd(SldNodeType.GENERATOR)
            }

            SldToolButton(
                "Bus"
            ) {
                openAdd(SldNodeType.BUS)
            }

            SldToolButton(
                "Breaker"
            ) {
                openAdd(SldNodeType.BREAKER)
            }

            SldToolButton(
                "Panel"
            ) {
                openAdd(SldNodeType.PANEL)
            }

            SldToolButton(
                "Load"
            ) {
                openAdd(SldNodeType.LOAD)
            }

            SldToolButton(
                if (arabic) "ربط" else "Connect"
            ) {
                connectSelected()
            }

            SldToolButton(
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
                .padding(10.dp),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            SldToolButton(
                if (arabic)
                    "تيارات القصر"
                else
                    "Short Circuit"
            ) {
                study?.let {
                    resultTitle =
                        if (arabic)
                            "تقرير تيارات القصر"
                        else
                            "Short Circuit Report"

                    resultText =
                        formatShortCircuit(it)

                    showResultDialog = true
                }
            }

            SldToolButton(
                if (arabic)
                    "Panel Schedule"
                else
                    "Panel Schedule"
            ) {

                val panel =
                    nodes.firstOrNull {
                        it.id == selectedNodeId &&
                            it.type == SldNodeType.PANEL
                    }

                if (panel != null) {
                    resultTitle =
                        "Panel Schedule - ${panel.name}"

                    resultText =
                        buildPanelSchedule(
                            panel,
                            nodes,
                            connections,
                            study
                        )

                    showResultDialog = true
                }
            }

            SldToolButton(
                if (arabic)
                    "التقرير الكامل"
                else
                    "Complete Report"
            ) {
                resultTitle =
                    if (arabic)
                        "التقرير الهندسي الكامل"
                    else
                        "Complete Engineering Report"

                resultText =
                    buildCompleteReport(
                        nodes,
                        connections,
                        study
                    )

                showResultDialog = true
            }

            Button(
                onClick = {
                    generateCompleteSld()
                },
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (arabic)
                        "Generate Complete SLD"
                    else
                        "Generate Complete SLD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp),
            shape = RoundedCornerShape(12.dp)
        ) {

            SldProfessionalCanvas(
                nodes = nodes,
                connections = connections,
                study = study,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                connectionStartId = connectionStartId,

                onSelectNode = {
                    selectedNodeId = it
                    selectedConnectionId = null
                },

                onMoveNode = { id, x, y ->
                    nodes = nodes.map {
                        if (it.id == id) {
                            it.copy(
                                x = max(0f, x),
                                y = max(0f, y)
                            )
                        } else {
                            it
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
                        fontSize = 12.sp
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
                        if (arabic)
                            "إغلاق"
                        else
                            "Close"
                    )
                }
            }
        )
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showCompleteDialog = false
            },

            title = {
                Text(
                    if (arabic)
                        "تم إنشاء المخطط الكامل"
                    else
                        "Complete SLD Generated"
                )
            },

            text = {
                Text(
                    if (arabic) {
                        "تم إنشاء وتسلسل عناصر مصدر التغذية → المحول/المولد → Bus → القاطع → اللوحة → الحمل، وتم إنشاء التوصيلات تلقائياً وإعادة حساب دراسة القصر."
                    } else {
                        "The complete electrical chain Source → Transformer/Generator → Bus → Breaker → Panel → Load has been generated. Connections were created automatically and the short-circuit study was recalculated."
                    }
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showCompleteDialog = false
                    }
                ) {
                    Text(
                        if (arabic) "تم" else "OK"
                    )
                }
            }
        )
    }
}

@Composable
private fun SldToolButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SldProfessionalCanvas(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    study: SldShortCircuitStudy?,
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
    val currentSelectNode by rememberUpdatedState(onSelectNode)
    val currentMoveNode by rememberUpdatedState(onMoveNode)
    val currentSelectConnection by rememberUpdatedState(onSelectConnection)
    val currentEditNode by rememberUpdatedState(onEditNode)
    val currentEditConnection by rememberUpdatedState(onEditConnection)

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
                                currentEditNode(node)
                                return@detectTapGestures
                            }

                            val connection =
                                findConnection(
                                    position,
                                    currentConnections,
                                    currentNodes
                                )

                            if (connection != null) {
                                currentEditConnection(connection)
                            }
                        },

                        onTap = { position ->

                            val node =
                                findNode(
                                    position,
                                    currentNodes
                                )

                            if (node != null) {
                                currentSelectNode(node.id)
                                return@detectTapGestures
                            }

                            val connection =
                                findConnection(
                                    position,
                                    currentConnections,
                                    currentNodes
                                )

                            if (connection != null) {
                                currentSelectConnection(
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

                            node?.let {
                                currentSelectNode(it.id)
                            }
                        },

                        onDrag = { change, amount ->

                            val id =
                                draggedNodeId

                            if (id != null) {

                                val node =
                                    currentNodes.firstOrNull {
                                        it.id == id
                                    }

                                if (node != null) {

                                    currentMoveNode(
                                        id,
                                        node.x + amount.x,
                                        node.y + amount.y
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
                                selectedConnectionId
                    )
                }
            }

            nodes.forEach { node ->

                drawProfessionalNode(
                    node = node,
                    result =
                        study?.results?.get(
                            node.id
                        ),
                    selected =
                        node.id ==
                            selectedNodeId,
                    connectionStart =
                        node.id ==
                            connectionStartId
                )
            }
        }
    }
}

private fun findNode(
    position: Offset,
    nodes: List<SldNode>
): SldNode? {

    return nodes.asReversed().firstOrNull { node ->

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

    val c =
        t.coerceIn(
            0f,
            1f
        )

    val x =
        a.x + c * dx

    val y =
        a.y + c * dy

    return hypot(
        p.x - x,
        p.y - y
    )
}

private fun DrawScope.drawProfessionalConnection(
    from: SldNode,
    to: SldNode,
    connection: SldConnection,
    selected: Boolean
) {

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

    val color =
        if (selected)
            Accent
        else
            CableColor

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
        style = Stroke(
            width =
                if (selected)
                    6f
                else
                    3f
        )
    )

    val dx = end.x - middleX
    val dy = end.y - end.y

    val arrow =
        9f

    drawLine(
        color = color,
        start =
            Offset(
                end.x - arrow,
                end.y - arrow
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
                end.x - arrow,
                end.y + arrow
            ),
        end =
            Offset(
                end.x,
                end.y
            ),
        strokeWidth = 3f
    )

    drawIntoCanvas { canvas ->

        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.LTGRAY
                textSize = 12f
                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
            }

        val cableText =
            buildString {

                if (connection.cableSizeMm2 > 0.0) {
                    append(
                        "${connection.cableSizeMm2.roundToInt()} mm²"
                    )
                }

                if (connection.parallelRuns > 1) {
                    append(
                        " x ${connection.parallelRuns}"
                    )
                }

                if (connection.lengthMeters > 0.0) {
                    append(
                        "   ${connection.lengthMeters.roundToInt()} m"
                    )
                }
            }

        if (cableText.isNotBlank()) {

            canvas.nativeCanvas.drawText(
                cableText,
                middleX + 8f,
                (start.y + end.y) / 2f - 8f,
                paint
            )
        }
    }
}

private fun DrawScope.drawProfessionalNode(
    node: SldNode,
    result: SldShortCircuitResult?,
    selected: Boolean,
    connectionStart: Boolean
) {

    val nodeColor =
        when (node.type) {
            SldNodeType.SOURCE ->
                SourceColor

            SldNodeType.TRANSFORMER ->
                TransformerColor

            SldNodeType.GENERATOR ->
                GeneratorColor

            SldNodeType.BUS ->
                BusColor

            SldNodeType.BREAKER ->
                BreakerColor

            SldNodeType.PANEL ->
                PanelColor

            SldNodeType.LOAD ->
                LoadColor
        }

    drawRoundRect(
        color =
            if (connectionStart)
                Accent
            else
                nodeColor,

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
                10f,
                10f
            )
    )

    if (selected) {

        drawRoundRect(
            color = Accent,

            topLeft =
                Offset(
                    node.x - 4f,
                    node.y - 4f
                ),

            size =
                Size(
                    NODE_WIDTH + 8f,
                    NODE_HEIGHT + 8f
                ),

            cornerRadius =
                CornerRadius(
                    12f,
                    12f
                ),

            style =
                Stroke(
                    3f
                )
        )
    }

    drawEngineeringSymbol(
        node = node
    )

    drawIntoCanvas { canvas ->

        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    android.graphics.Color.WHITE

                textSize =
                    13f

                typeface =
                    android.graphics.Typeface.DEFAULT_BOLD
            }

        canvas.nativeCanvas.drawText(
            node.name.take(20),
            node.x + 38f,
            node.y + 21f,
            paint
        )

        paint.textSize = 10f
        paint.typeface =
            android.graphics.Typeface.DEFAULT

        canvas.nativeCanvas.drawText(
            node.type.name,
            node.x + 38f,
            node.y + 37f,
            paint
        )

        var lineY =
            node.y + 53f

        fun text(
            value: String
        ) {
            canvas.nativeCanvas.drawText(
                value,
                node.x + 8f,
                lineY,
                paint
            )
            lineY += 13f
        }

        when (node.type) {

            SldNodeType.SOURCE -> {

                text(
                    "V = %.0f V".format(
                        node.voltage
                    )
                )

                text(
                    "Scc = %.1f MVA".format(
                        node.sourceShortCircuitMva
                    )
                )

                result?.let {
                    text(
                        "Ik'' = %.1f kA".format(
                            it.initialSymmetricalCurrentKa
                        )
                    )
                }
            }

            SldNodeType.TRANSFORMER -> {

                text(
                    "S = %.0f kVA".format(
                        node.ratedKva
                    )
                )

                text(
                    "V = %.0f V".format(
                        node.voltage
                    )
                )

                text(
                    "Z = %.1f %%".format(
                        node.transformerPercentZ
                    )
                )

                result?.let {
                    text(
                        "Ik'' %.1f kA".format(
                            it.initialSymmetricalCurrentKa
                        )
                    )
                }
            }

            SldNodeType.GENERATOR -> {

                text(
                    "S = %.0f kVA".format(
                        node.ratedKva
                    )
                )

                text(
                    "V = %.0f V".format(
                        node.voltage
                    )
                )

                text(
                    "Xd'' = %.1f %%".format(
                        node.generatorXdSubtransient
                    )
                )

                result?.let {
                    text(
                        "Ik'' %.1f kA".format(
                            it.initialSymmetricalCurrentKa
                        )
                    )
                }
            }

            SldNodeType.BUS -> {

                text(
                    "V = %.0f V".format(
                        node.voltage
                    )
                )

                result?.let {
                    text(
                        "Ik'' = %.1f kA".format(
                            it.initialSymmetricalCurrentKa
                        )
                    )

                    text(
                        "Fault = %.1f MVA".format(
                            it.shortCircuitMva
                        )
                    )
                }
            }

            SldNodeType.BREAKER -> {

                result?.let {

                    text(
                        "Icu ≥ %.1f kA".format(
                            it.breakerRequiredKa
                        )
                    )

                    text(
                        "Ics ≥ %.1f kA".format(
                            it.breakerRequiredKa *
                                0.75
                        )
                    )
                }

                text(
                    "V = %.0f V".format(
                        node.voltage
                    )
                )
            }

            SldNodeType.PANEL -> {

                text(
                    "V = %.0f V".format(
                        node.voltage
                    )
                )

                result?.let {

                    text(
                        "Ik'' = %.1f kA".format(
                            it.initialSymmetricalCurrentKa
                        )
                    )
                }
            }

            SldNodeType.LOAD -> {

                val current =
                    calculateLoadCurrent(
                        node
                    )

                text(
                    "P = %.1f kW".format(
                        node.loadKw
                    )
                )

                text(
                    "I = %.1f A".format(
                        current
                    )
                )

                text(
                    "PF = %.2f".format(
                        node.powerFactor
                    )
                )

                result?.let {
                    text(
                        "Ik'' = %.1f kA".format(
                            it.initialSymmetricalCurrentKa
                        )
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawEngineeringSymbol(
    node: SldNode
) {

    val centerX =
        node.x + 18f

    val centerY =
        node.y + 30f

    when (node.type) {

        SldNodeType.SOURCE -> {

            drawCircle(
                color = Color.White,
                radius = 12f,
                center =
                    Offset(
                        centerX,
                        centerY
                    ),
                style =
                    Stroke(2f)
            )

            drawLine(
                color = Color.White,
                start =
                    Offset(
                        centerX - 7f,
                        centerY
                    ),
                end =
                    Offset(
                        centerX + 7f,
                        centerY
                    ),
                strokeWidth = 2f
            )

            drawLine(
                color = Color.White,
                start =
                    Offset(
                        centerX,
                        centerY - 7f
                    ),
                end =
                    Offset(
                        centerX,
                        centerY + 7f
                    ),
                strokeWidth = 2f
            )
        }

        SldNodeType.TRANSFORMER -> {

            drawCircle(
                color = Color.White,
                radius = 10f,
                center =
                    Offset(
                        centerX - 5f,
                        centerY
                    ),
                style =
                    Stroke(2f)
            )

            drawCircle(
                color = Color.White,
                radius = 10f,
                center =
                    Offset(
                        centerX + 5f,
                        centerY
                    ),
                style =
                    Stroke(2f)
            )
        }

        SldNodeType.GENERATOR -> {

            drawCircle(
                color = Color.White,
                radius = 13f,
                center =
                    Offset(
                        centerX,
                        centerY
                    ),
                style =
                    Stroke(2f)
            )

            drawLine(
                color = Color.White,
                start =
                    Offset(
                        centerX - 7f,
                        centerY
                    ),
                end =
                    Offset(
                        centerX + 7f,
                        centerY
                    ),
                strokeWidth = 2f
            )
        }

        SldNodeType.BUS -> {

            drawLine(
                color = Color.White,
                start =
                    Offset(
                        centerX - 13f,
                        centerY
                    ),
                end =
                    Offset(
                        centerX + 13f,
                        centerY
                    ),
                strokeWidth = 5f
            )
        }

        SldNodeType.BREAKER -> {

            drawRect(
                color = Color.White,
                topLeft =
                    Offset(
                        centerX - 9f,
                        centerY - 9f
                    ),
                size =
                    Size(
                        18f,
                        18f
                    ),
                style =
                    Stroke(2f)
            )

            drawLine(
                color = Color.White,
                start =
                    Offset(
                        centerX - 5f,
                        centerY + 5f
                    ),
                end =
                    Offset(
                        centerX + 6f,
                        centerY - 6f
                    ),
                strokeWidth = 2f
            )
        }

        SldNodeType.PANEL -> {

            drawRect(
                color = Color.White,
                topLeft =
                    Offset(
                        centerX - 10f,
                        centerY - 13f
                    ),
                size =
                    Size(
                        20f,
                        26f
                    ),
                style =
                    Stroke(2f)
            )

            drawLine(
                color = Color.White,
                start =
                    Offset(
                        centerX,
                        centerY - 8f
                    ),
                end =
                    Offset(
                        centerX,
                        centerY + 8f
                    ),
                strokeWidth = 2f
            )
        }

        SldNodeType.LOAD -> {

            drawCircle(
                color = Color.White,
                radius = 12f,
                center =
                    Offset(
                        centerX,
                        centerY
                    ),
                style =
                    Stroke(2f)
            )

            drawLine(
                color = Color.White,
                start =
                    Offset(
                        centerX - 6f,
                        centerY
                    ),
                end =
                    Offset(
                        centerX + 6f,
                        centerY
                    ),
                strokeWidth = 2f
            )
        }
    }
}

private fun calculateLoadCurrent(
    node: SldNode
): Double {

    if (node.loadKw <= 0.0) {
        return 0.0
    }

    val voltage =
        node.voltage.coerceAtLeast(
            1.0
        )

    val pf =
        node.powerFactor.coerceIn(
            0.01,
            1.0
        )

    return node.loadKw * 1000.0 /
        (1.7320508075688772 *
            voltage *
            pf)
}

private fun buildPanelSchedule(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    study: SldShortCircuitStudy?
): String {

    val children =
        connections
            .filter {
                it.fromNodeId ==
                    panel.id
            }
            .mapNotNull { connection ->
                nodes.firstOrNull {
                    it.id ==
                        connection.toNodeId
                }
            }

    val sb =
        StringBuilder()

    sb.appendLine(
        "PANEL SCHEDULE"
    )

    sb.appendLine(
        "Panel: ${panel.name}"
    )

    sb.appendLine(
        "Voltage: %.0f V".format(
            panel.voltage
        )
    )

    sb.appendLine(
        "=".repeat(90)
    )

    sb.appendLine(
        "%-4s %-20s %-10s %-10s %-10s %-10s".format(
            "No.",
            "Load",
            "kW",
            "PF",
            "Current",
            "Breaker"
        )
    )

    sb.appendLine(
        "-".repeat(90)
    )

    var totalKw = 0.0
    var totalCurrent = 0.0

    children.forEachIndexed { index, child ->

        val current =
            calculateLoadCurrent(
                child
            )

        val breaker =
            standardBreaker(
                current
            )

        totalKw +=
            child.loadKw

        totalCurrent +=
            current

        sb.appendLine(
            "%-4d %-20s %-10.1f %-10.2f %-10.1f %-10.0f A".format(
                index + 1,
                child.name.take(20),
                child.loadKw,
                child.powerFactor,
                current,
                breaker
            )
        )
    }

    sb.appendLine()
    sb.appendLine(
        "TOTAL CONNECTED LOAD = %.1f kW"
            .format(totalKw)
    )

    sb.appendLine(
        "TOTAL CURRENT = %.1f A"
            .format(totalCurrent)
    )

    sb.appendLine(
        "MAIN BREAKER = %.0f A"
            .format(
                standardBreaker(
                    totalCurrent
                )
            )
    )

    study?.results?.get(
        panel.id
    )?.let {

        sb.appendLine()
        sb.appendLine(
            "SHORT CIRCUIT AT PANEL"
        )
        sb.appendLine(
            "Ik'' = %.3f kA"
                .format(
                    it.initialSymmetricalCurrentKa
                )
        )
        sb.appendLine(
            "Ip = %.3f kA"
                .format(
                    it.peakCurrentKa
                )
        )
        sb.appendLine(
            "Ith = %.3f kA"
                .format(
                    it.thermalCurrentKa
                )
        )
        sb.appendLine(
            "Fault MVA = %.3f"
                .format(
                    it.shortCircuitMva
                )
        )
        sb.appendLine(
            "X/R = %.3f"
                .format(
                    it.xrRatio
                )
        )
        sb.appendLine(
            "Required Icu >= %.1f kA"
                .format(
                    it.breakerRequiredKa
                )
        )
        sb.appendLine(
            "Required Ics >= %.1f kA"
                .format(
                    it.breakerRequiredKa *
                        0.75
                )
        )
    }

    return sb.toString()
}

private fun buildCompleteReport(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    study: SldShortCircuitStudy?
): String {

    val sb =
        StringBuilder()

    sb.appendLine(
        "COMPLETE ELECTRICAL SLD ENGINEERING REPORT"
    )

    sb.appendLine(
        "=".repeat(70)
    )

    sb.appendLine()

    sb.appendLine(
        "SYSTEM ELEMENTS"
    )

    sb.appendLine(
        "Nodes      : ${nodes.size}"
    )

    sb.appendLine(
        "Connections: ${connections.size}"
    )

    sb.appendLine()

    nodes.forEach { node ->

        sb.appendLine(
            "[${node.type}] ${node.name}"
        )

        sb.appendLine(
            "  Voltage : %.0f V"
                .format(
                    node.voltage
                )
        )

        when (node.type) {

            SldNodeType.SOURCE -> {

                sb.appendLine(
                    "  Source Fault Level : %.2f MVA"
                        .format(
                            node.sourceShortCircuitMva
                        )
                )
            }

            SldNodeType.TRANSFORMER -> {

                sb.appendLine(
                    "  Rating : %.0f kVA"
                        .format(
                            node.ratedKva
                        )
                )

                sb.appendLine(
                    "  Z%     : %.2f %%"
                        .format(
                            node.transformerPercentZ
                        )
                )
            }

            SldNodeType.GENERATOR -> {

                sb.appendLine(
                    "  Rating : %.0f kVA"
                        .format(
                            node.ratedKva
                        )
                )

                sb.appendLine(
                    "  Xd''   : %.2f %%"
                        .format(
                            node.generatorXdSubtransient
                        )
                )
            }

            SldNodeType.LOAD -> {

                sb.appendLine(
                    "  Load : %.2f kW"
                        .format(
                            node.loadKw
                        )
                )

                sb.appendLine(
                    "  Current : %.2f A"
                        .format(
                            calculateLoadCurrent(
                                node
                            )
                        )
                )
            }

            else -> Unit
        }

        study?.results?.get(
            node.id
        )?.let {

            sb.appendLine(
                "  Ik'' : %.3f kA"
                    .format(
                        it.initialSymmetricalCurrentKa
                    )
            )

            sb.appendLine(
                "  Ip   : %.3f kA"
                    .format(
                        it.peakCurrentKa
                    )
            )

            sb.appendLine(
                "  Ith  : %.3f kA"
                    .format(
                        it.thermalCurrentKa
                    )
            )

            sb.appendLine(
                "  Fault: %.3f MVA"
                    .format(
                        it.shortCircuitMva
                    )
            )

            sb.appendLine(
