package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.*
import kotlin.math.hypot

private val Background = Color(0xFF0B1116)
private val CardColor = Color(0xFF151D24)
private val PrimaryText = Color(0xFFF2F5F7)
private val Accent = Color(0xFF00BCD4)

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

    fun network(): SldNetwork {
        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }

    fun invalidateStudies() {
        // Intentionally left empty.
        // Calculation engines always calculate from the current network.
    }

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

        invalidateStudies()
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
        invalidateStudies()
        showConnectionDialog = false
    }

    fun deleteSelected() {

        selectedNodeId?.let { id ->

            if (id != "source-1") {

                nodes = nodes.filterNot {
                    it.id == id
                }

                connections = connections.filter {
                    it.fromNodeId != id &&
                        it.toNodeId != id
                }

                selectedNodeId = null
                selectedConnectionId = null
                connectionStartId = null

                invalidateStudies()

                return
            }
        }

        selectedConnectionId?.let { id ->

            connections = connections.filterNot {
                it.id == id
            }

            selectedConnectionId = null

            invalidateStudies()
        }
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
            selectedNodeId = null

            invalidateStudies()
        }

        connectionStartId = null
    }

    fun runUpstream() {
        try {
            val result =
                SldEngineeringEngine.calculateUpstream(network())

            resultTitle =
                if (arabic)
                    "نتائج الحسابات"
                else
                    "Calculation Results"

            resultText = result.toString()
            showResultDialog = true

        } catch (e: Exception) {

            resultTitle =
                if (arabic) "خطأ" else "Error"

            resultText =
                e.message ?: "Calculation error"

            showResultDialog = true
        }
    }

    fun runShortCircuit() {
        try {

            val result =
                SldShortCircuitEngine.calculate(network())

            resultTitle =
                if (arabic)
                    "حساب تيارات القصر"
                else
                    "Short Circuit"

            resultText = result.toString()
            showResultDialog = true

        } catch (e: Exception) {

            resultTitle =
                if (arabic) "خطأ" else "Error"

            resultText =
                e.message ?: "Short circuit calculation error"

            showResultDialog = true
        }
    }

    fun runCableSizing() {

        try {

            val shortCircuit =
                SldShortCircuitEngine.calculate(network())

            val result =
                SldCableSizingEngine.calculate(
                    network = network(),
                    shortCircuitStudy = shortCircuit
                )

            resultTitle =
                if (arabic)
                    "اختيار الكابلات"
                else
                    "Cable Sizing"

            resultText = result.toString()
            showResultDialog = true

        } catch (e: Exception) {

            resultTitle =
                if (arabic) "خطأ" else "Error"

            resultText =
                e.message ?: "Cable sizing calculation error"

            showResultDialog = true
        }
    }

    fun runProtection() {

        try {

            val shortCircuit =
                SldShortCircuitEngine.calculate(network())

            val cableSizing =
                SldCableSizingEngine.calculate(
                    network = network(),
                    shortCircuitStudy = shortCircuit
                )

            val result =
                SldProtectionCoordinationEngine.calculate(
                    network = network(),
                    shortCircuitStudy = shortCircuit,
                    cableSizingStudy = cableSizing
                )

            resultTitle =
                if (arabic)
                    "تنسيق الحمايات"
                else
                    "Protection Coordination"

            resultText = result.toString()
            showResultDialog = true

        } catch (e: Exception) {

            resultTitle =
                if (arabic) "خطأ" else "Error"

            resultText =
                e.message ?: "Protection calculation error"

            showResultDialog = true
        }
    }

    fun runPanelSchedule() {

        val panelId = selectedNodeId ?: return

        try {

            val shortCircuit =
                SldShortCircuitEngine.calculate(network())

            val cableSizing =
                SldCableSizingEngine.calculate(
                    network = network(),
                    shortCircuitStudy = shortCircuit
                )

            val result =
                SldPanelScheduleEngine.calculate(
                    network = network(),
                    panelNodeId = panelId,
                    cableSizingStudy = cableSizing
                )

            resultTitle =
                if (arabic)
                    "Panel Schedule"
                else
                    "Panel Schedule"

            resultText = result.toString()
            showResultDialog = true

        } catch (e: Exception) {

            resultTitle =
                if (arabic) "خطأ" else "Error"

            resultText =
                e.message ?: "Panel schedule calculation error"

            showResultDialog = true
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
                .background(CardColor)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (onBack != null) {

                OutlinedButton(
                    onClick = onBack
                ) {
                    Text(
                        if (arabic) "رجوع"
                        else "Back"
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text =
                    if (arabic)
                        "المخطط الأحادي SLD"
                    else
                        "Single Line Diagram",

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
                .background(CardColor)
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            ToolButton(
                text = if (arabic) "مصدر" else "Source"
            ) {
                openAdd(SldNodeType.SOURCE)
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
                text = if (arabic) "باص" else "Bus"
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
                text = if (arabic) "توصيل" else "Connect"
            ) {
                connectSelected()
            }

            ToolButton(
                text = if (arabic) "تعديل" else "Edit"
            ) {

                selectedNodeId?.let { id ->

                    nodes.firstOrNull {
                        it.id == id
                    }?.let {
                        openEdit(it)
                    }

                } ?: selectedConnectionId?.let { id ->

                    connections.firstOrNull {
                        it.id == id
                    }?.let {
                        openConnection(it)
                    }
                }
            }

            ToolButton(
                text = if (arabic) "حذف" else "Delete"
            ) {
                deleteSelected()
            }

            ToolButton(
                text = "Upstream"
            ) {
                runUpstream()
            }

            ToolButton(
                text = if (arabic) "قصر" else "Short Circuit"
            ) {
                runShortCircuit()
            }

            ToolButton(
                text = if (arabic) "كابلات" else "Cable Sizing"
            ) {
                runCableSizing()
            }

            ToolButton(
                text = if (arabic) "حماية" else "Protection"
            ) {
                runProtection()
            }

            ToolButton(
                text = "Panel Schedule"
            ) {
                runPanelSchedule()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {

            SldCanvas(
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                connectionStartId = connectionStartId,

                onNodeSelected = { id ->

                    selectedNodeId = id
                    selectedConnectionId = null
                },

                onConnectionSelected = { id ->

                    selectedConnectionId = id
                    selectedNodeId = null
                },

                onEmptySelected = {

                    selectedNodeId = null
                    selectedConnectionId = null
                },

                onNodeMoved = { id, x, y ->

                    nodes = nodes.map { node ->

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
    }

    if (showNodeDialog) {

        NodeEditorDialog(
            arabic = arabic,
            editing = editingNodeId != null,

            nodeName = nodeName,
            nodeVoltage = nodeVoltage,
            nodeKw = nodeKw,
            nodePf = nodePf,
            nodeDemand = nodeDemand,
            nodeKva = nodeKva,
            nodeTransformerZ = nodeTransformerZ,
            nodeGeneratorXd = nodeGeneratorXd,
            nodeSourceMva = nodeSourceMva,

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

            onDismiss = {
                showNodeDialog = false
            },

            onSave = ::saveNode
        )
    }

    if (showConnectionDialog) {

        ConnectionEditorDialog(
            arabic = arabic,
            editing = editingConnectionId != null,

            length = connectionLength,
            resistance = connectionResistance,
            reactance = connectionReactance,
            cableSize = connectionCableSize,
            parallelRuns = connectionParallelRuns,
            currentCapacity = connectionCurrentCapacity,

            onLengthChange = {
                connectionLength = it
            },

            onResistanceChange = {
                connectionResistance = it
            },

            onReactanceChange = {
                connectionReactance = it
            },

            onCableSizeChange = {
                connectionCableSize = it
            },

            onParallelRunsChange = {
                connectionParallelRuns = it
            },

            onCurrentCapacityChange = {
                connectionCurrentCapacity = it
            },

            onDismiss = {
                showConnectionDialog = false
            },

            onSave = ::saveConnection
        )
    }

    if (showResultDialog) {

        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
            },

            title = {
                Text(resultTitle)
            },

            text = {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = 100.dp,
                            max = 500.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    Text(
                        text = resultText,
                        color = PrimaryText,
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
                        if (arabic) "إغلاق"
                        else "Close"
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
        contentPadding = PaddingValues(
            horizontal = 10.dp,
            vertical = 4.dp
        )
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
    onNodeSelected: (String) -> Unit,
    onConnectionSelected: (String) -> Unit,
    onEmptySelected: () -> Unit,
    onNodeMoved: (String, Float, Float) -> Unit
) {

    var dragNodeId by remember {
        mutableStateOf<String?>(null)
    }

    var dragStartX by remember {
        mutableStateOf(0f)
    }

    var dragStartY by remember {
        mutableStateOf(0f)
    }

    var nodeStartX by remember {
        mutableStateOf(0f)
    }

    var nodeStartY by remember {
        mutableStateOf(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF101820),
                RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {

                detectTapGestures(
                    onTap = { position ->

                        val node =
                            nodes
                                .asReversed()
                                .firstOrNull { candidate ->

                                    position.x >=
                                        candidate.x -
                                        NODE_TOUCH_PADDING &&

                                    position.x <=
                                        candidate.x +
                                        NODE_WIDTH +
                                        NODE_TOUCH_PADDING &&

                                    position.y >=
                                        candidate.y -
                                        NODE_TOUCH_PADDING &&

                                    position.y <=
                                        candidate.y +
                                        NODE_HEIGHT +
                                        NODE_TOUCH_PADDING
                                }

                        if (node != null) {

                            onNodeSelected(node.id)

                        } else {

                            var selectedConnection:
                                SldConnection? = null

                            var bestDistance =
                                Float.MAX_VALUE

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

                                    val x1 =
                                        from.x +
                                            NODE_WIDTH / 2f

                                    val y1 =
                                        from.y +
                                            NODE_HEIGHT / 2f

                                    val x2 =
                                        to.x +
                                            NODE_WIDTH / 2f

                                    val y2 =
                                        to.y +
                                            NODE_HEIGHT / 2f

                                    val distance =
                                        distanceToSegment(
                                            position.x,
                                            position.y,
                                            x1,
                                            y1,
                                            x2,
                                            y2
                                        )

                                    if (
                                        distance < 30f &&
                                        distance < bestDistance
                                    ) {

                                        bestDistance =
                                            distance

                                        selectedConnection =
                                            connection
                                    }
                                }
                            }

                            if (
                                selectedConnection != null
                            ) {

                                onConnectionSelected(
                                    selectedConnection.id
                                )

                            } else {

                                onEmptySelected()
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {

                detectDragGestures(

                    onDragStart = { position ->

                        val node =
                            nodes
                                .asReversed()
                                .firstOrNull { candidate ->

                                    position.x >=
                                        candidate.x -
                                        NODE_TOUCH_PADDING &&

                                    position.x <=
                                        candidate.x +
                                        NODE_WIDTH +
                                        NODE_TOUCH_PADDING &&

                                    position.y >=
                                        candidate.y -
                                        NODE_TOUCH_PADDING &&

                                    position.y <=
                                        candidate.y +
                                        NODE_HEIGHT +
                                        NODE_TOUCH_PADDING
                                }

                        if (node != null) {

                            dragNodeId = node.id

                            dragStartX = position.x
                            dragStartY = position.y

                            nodeStartX = node.x
                            nodeStartY = node.y

                            onNodeSelected(node.id)
                        }
                    },

                    onDrag = { change, _ ->

                        val id =
                            dragNodeId
                                ?: return@detectDragGestures

                        val dx =
                            change.position.x -
                                dragStartX

                        val dy =
                            change.position.y -
                                dragStartY

                        val newX =
                            (nodeStartX + dx)
                                .coerceAtLeast(10f)

                        val newY =
                            (nodeStartY + dy)
                                .coerceAtLeast(10f)

                        onNodeMoved(
                            id,
                            newX,
                            newY
                        )

                        change.consume()
                    },

                    onDragEnd = {
                        dragNodeId = null
                    },

                    onDragCancel = {
                        dragNodeId = null
                    }
                )
            }
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
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

                    val selected =
                        connection.id ==
                            selectedConnectionId

                    drawLine(
                        color =
                            if (selected)
                                Accent
                            else
                                Color.LightGray,

                        start = start,
                        end = end,

                        strokeWidth =
                            if (selected)
                                6f
                            else
                                3f
                    )

                    drawConnectionArrow(
                        start = start,
                        end = end
                    )
                }
            }

            nodes.forEach { node ->

                val selected =
                    node.id == selectedNodeId ||
                        node.id == connectionStartId

                drawNode(
                    node = node,
                    selected = selected
                )
            }
        }
    }
}

private fun distanceToSegment(
    px: Float,
    py: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float
): Float {

    val dx = x2 - x1
    val dy = y2 - y1

    if (dx == 0f && dy == 0f) {

        return hypot(
            px - x1,
            py - y1
        )
    }

    val t =
        (
            (px - x1) * dx +
                (py - y1) * dy
            ) /
            (dx * dx + dy * dy)

    val clamped =
        t.coerceIn(0f, 1f)

    val cx =
        x1 + clamped * dx

    val cy =
        y1 + clamped * dy

    return hypot(
        px - cx,
        py - cy
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNode(
    node: SldNode,
    selected: Boolean
) {

    val color =
        when (node.type) {

            SldNodeType.SOURCE ->
                Color(0xFF1565C0)

            SldNodeType.BUS ->
                Color(0xFF6A1B9A)

            SldNodeType.TRANSFORMER ->
                Color(0xFFEF6C00)

            SldNodeType.GENERATOR ->
                Color(0xFF2E7D32)

            SldNodeType.BREAKER ->
                Color(0xFF455A64)

            SldNodeType.PANEL ->
                Color(0xFF00838F)

            SldNodeType.LOAD ->
                Color(0xFF37474F)
        }

    drawRoundRect(
        color = color,
        topLeft = Offset(
            node.x,
            node.y
        ),
        size = androidx.compose.ui.geometry.Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                8f,
                8f
            )
    )

    if (selected) {

        drawRoundRect(
            color = Accent,

            topLeft = Offset(
                node.x - 3f,
                node.y - 3f
            ),

            size = androidx.compose.ui.geometry.Size(
                NODE_WIDTH + 6f,
                NODE_HEIGHT + 6f
            ),

            cornerRadius =
                androidx.compose.ui.geometry.CornerRadius(
                    10f,
                    10f
                ),

            style = Stroke(
                width = 4f
            )
        )
    }

    drawContext.canvas.nativeCanvas.apply {

        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            )

        paint.color =
            android.graphics.Color.WHITE

        paint.textSize = 14f

        paint.typeface =
            android.graphics.Typeface.DEFAULT_BOLD

        drawText(
            node.name.take(18),
            node.x + 8f,
            node.y + 25f,
            paint
        )

        paint.textSize = 11f

        paint.typeface =
            android.graphics.Typeface.DEFAULT

        drawText(
            node.type.name,
            node.x + 8f,
            node.y + 45f,
            paint
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnectionArrow(
    start: Offset,
    end: Offset
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

    val arrowLength = 12f
    val arrowWidth = 6f

    val tip =
        Offset(
            end.x - ux * 8f,
            end.y - uy * 8f
        )

    val left =
        Offset(
            tip.x -
                ux * arrowLength +
                uy * arrowWidth,

            tip.y -
                uy * arrowLength -
                ux * arrowWidth
        )

    val right =
        Offset(
            tip.x -
                ux * arrowLength -
                uy * arrowWidth,

            tip.y -
                uy * arrowLength +
                ux * arrowWidth
        )

    val path = Path()

    path.moveTo(
        tip.x,
        tip.y
    )

    path.lineTo(
        left.x,
        left.y
    )

    path.lineTo(
        right.x,
        right.y
    )

    path.close()

    drawPath(
        path = path,
        color = Color.LightGray
    )
}

@Composable
private fun NodeEditorDialog(
    arabic: Boolean,
    editing: Boolean,

    nodeName: String,
    nodeVoltage: String,
    nodeKw: String,
    nodePf: String,
    nodeDemand: String,
    nodeKva: String,
    nodeTransformerZ: String,
    nodeGeneratorXd: String,
    nodeSourceMva: String,

    onNameChange: (String) -> Unit,
    onVoltageChange: (String) -> Unit,
    onKwChange: (String) -> Unit,
    onPfChange: (String) -> Unit,
    onDemandChange: (String) -> Unit,
    onKvaChange: (String) -> Unit,
    onTransformerZChange: (String) -> Unit,
    onGeneratorXdChange: (String) -> Unit,
    onSourceMvaChange: (String) -> Unit,

    onDismiss: () -> Unit,
    onSave: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                if (editing)
                    if (arabic)
                        "تعديل العنصر"
                    else
                        "Edit Element"
                else
                    if (arabic)
                        "إضافة عنصر"
                    else
                        "Add Element"
            )
        },

        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        max = 560.dp
                    )
                    .verticalScroll(
                        rememberScrollState()
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {

                EditorField(
                    label =
                        if (arabic)
                            "الاسم"
                        else
                            "Name",

                    value = nodeName,

                    onValueChange =
                        onNameChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "الجهد V"
                        else
                            "Voltage V",

                    value = nodeVoltage,

                    onValueChange =
                        onVoltageChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "الحمل kW"
                        else
                            "Load kW",

                    value = nodeKw,

                    onValueChange =
                        onKwChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "معامل القدرة"
                        else
                            "Power Factor",

                    value = nodePf,

                    onValueChange =
                        onPfChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "معامل الطلب"
                        else
                            "Demand Factor",

                    value = nodeDemand,

                    onValueChange =
                        onDemandChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "القدرة kVA"
                        else
                            "Rated kVA",

                    value = nodeKva,

                    onValueChange =
                        onKvaChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "% ممانعة المحول"
                        else
                            "Transformer %Z",

                    value = nodeTransformerZ,

                    onValueChange =
                        onTransformerZChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "Xd'' % للمولد"
                        else
                            "Generator Xd'' %",

                    value = nodeGeneratorXd,

                    onValueChange =
                        onGeneratorXdChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "قدرة القصر MVA"
                        else
                            "Source Short Circuit MVA",

                    value = nodeSourceMva,

                    onValueChange =
                        onSourceMvaChange
                )
            }
        },

        confirmButton = {

            Button(
                onClick = onSave
            ) {
                Text(
                    if (arabic)
                        "حفظ"
                    else
                        "Save"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (arabic)
                        "إلغاء"
                    else
                        "Cancel"
                )
            }
        }
    )
}

@Composable
private fun ConnectionEditorDialog(
    arabic: Boolean,
    editing: Boolean,

    length: String,
    resistance: String,
    reactance: String,
    cableSize: String,
    parallelRuns: String,
    currentCapacity: String,

    onLengthChange: (String) -> Unit,
    onResistanceChange: (String) -> Unit,
    onReactanceChange: (String) -> Unit,
    onCableSizeChange: (String) -> Unit,
    onParallelRunsChange: (String) -> Unit,
    onCurrentCapacityChange: (String) -> Unit,

    onDismiss: () -> Unit,
    onSave: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {

            Text(
                if (editing)
                    if (arabic)
                        "تعديل الكابل"
                    else
                        "Edit Connection"
                else
                    if (arabic)
                        "بيانات الكابل"
                    else
                        "Connection Data"
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
                    Arrangement.spacedBy(7.dp)
            ) {

                EditorField(
                    label =
                        if (arabic)
                            "الطول m"
                        else
                            "Length m",

                    value = length,

                    onValueChange =
                        onLengthChange
                )

                EditorField(
                    label = "R Ω/km",
                    value = resistance,
                    onValueChange =
                        onResistanceChange
                )

                EditorField(
                    label = "X Ω/km",
                    value = reactance,
                    onValueChange =
                        onReactanceChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "مقطع الكابل mm²"
                        else
                            "Cable Size mm²",

                    value = cableSize,

                    onValueChange =
                        onCableSizeChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "عدد المسارات"
                        else
                            "Parallel Runs",

                    value = parallelRuns,

                    onValueChange =
                        onParallelRunsChange
                )

                EditorField(
                    label =
                        if (arabic)
                            "سعة التيار A"
                        else
                            "Current Capacity A",

                    value = currentCapacity,

                    onValueChange =
                        onCurrentCapacityChange
                )
            }
        },

        confirmButton = {

            Button(
                onClick = onSave
            ) {
                Text(
                    if (arabic)
                        "حفظ"
                    else
                        "Save"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (arabic)
                        "إلغاء"
                    else
                        "Cancel"
                )
            }
        }
    )
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(
        value = value,

        onValueChange =
            onValueChange,

        label = {
            Text(label)
        },

        modifier =
            Modifier.fillMaxWidth(),

        singleLine = true
    )
}
