package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldCalculationResult
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringEngine
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary
import java.util.UUID
import kotlin.math.max
import kotlin.math.sqrt

private const val NODE_WIDTH = 150f
private const val NODE_HEIGHT = 82f

@Composable
fun SldEditorScreen(
    language: AppLanguage
) {
    val arabic = language == AppLanguage.ARABIC

    var nodes by remember {
        mutableStateOf(
            listOf(
                SldNode(
                    id = "source-1",
                    name = if (arabic) "المصدر" else "SOURCE",
                    type = SldNodeType.SOURCE,
                    x = 80f,
                    y = 180f,
                    voltage = 400.0
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

    var showResults by remember {
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

    var nodeName by remember {
        mutableStateOf("")
    }

    var nodeVoltage by remember {
        mutableStateOf("400")
    }

    var nodeKw by remember {
        mutableStateOf("10")
    }

    var nodePf by remember {
        mutableStateOf("0.90")
    }

    var nodeDemand by remember {
        mutableStateOf("1.0")
    }

    var nodeKva by remember {
        mutableStateOf("1000")
    }

    var nodeTransformerZ by remember {
        mutableStateOf("6.0")
    }

    var nodeGeneratorXd by remember {
        mutableStateOf("15.0")
    }

    var nodeSourceMva by remember {
        mutableStateOf("500")
    }

    var connectionLength by remember {
        mutableStateOf("10")
    }

    var connectionResistance by remember {
        mutableStateOf("0.08")
    }

    var connectionReactance by remember {
        mutableStateOf("0.08")
    }

    var connectionCableSize by remember {
        mutableStateOf("0")
    }

    var connectionParallelRuns by remember {
        mutableStateOf("1")
    }

    var connectionCurrentCapacity by remember {
        mutableStateOf("0")
    }

    var calculationResult by remember {
        mutableStateOf<SldCalculationResult?>(null)
    }

    fun defaultName(type: SldNodeType): String {
        return when (type) {
            SldNodeType.SOURCE ->
                if (arabic) "مصدر" else "Source"

            SldNodeType.TRANSFORMER ->
                if (arabic) "محول" else "Transformer"

            SldNodeType.GENERATOR ->
                if (arabic) "مولد" else "Generator"

            SldNodeType.BUS ->
                "Bus"

            SldNodeType.PANEL ->
                if (arabic) "لوحة" else "Panel"

            SldNodeType.BREAKER ->
                if (arabic) "قاطع" else "Breaker"

            SldNodeType.LOAD ->
                if (arabic) "حمل" else "Load"
        }
    }

    fun openAdd(type: SldNodeType) {
        editingNodeId = null
        pendingNodeType = type

        nodeName = defaultName(type)
        nodeVoltage = "400"
        nodeKw = "10"
        nodePf = "0.90"
        nodeDemand = "1.0"
        nodeKva = "1000"
        nodeTransformerZ = "6.0"
        nodeGeneratorXd = "15.0"
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

    fun openConnectionEdit(connection: SldConnection) {
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
        val voltage =
            nodeVoltage.toDoubleOrNull()
                ?.coerceAtLeast(1.0)
                ?: 400.0

        val kw =
            nodeKw.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val pf =
            nodePf.toDoubleOrNull()
                ?.coerceIn(0.01, 1.0)
                ?: 0.90

        val demand =
            nodeDemand.toDoubleOrNull()
                ?.coerceIn(0.0, 1.0)
                ?: 1.0

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

        val normalizedName =
            nodeName.ifBlank {
                defaultName(pendingNodeType)
            }

        if (editingNodeId != null) {
            nodes = nodes.map { node ->
                if (node.id == editingNodeId) {
                    node.copy(
                        name = normalizedName,
                        voltage = voltage,
                        loadKw =
                            if (pendingNodeType == SldNodeType.LOAD) {
                                kw
                            } else {
                                0.0
                            },
                        powerFactor = pf,
                        demandFactor = demand,
                        ratedKva =
                            if (
                                pendingNodeType == SldNodeType.TRANSFORMER ||
                                pendingNodeType == SldNodeType.GENERATOR
                            ) {
                                kva
                            } else {
                                0.0
                            },
                        transformerPercentZ =
                            if (pendingNodeType == SldNodeType.TRANSFORMER) {
                                transformerZ
                            } else {
                                0.0
                            },
                        generatorXdSubtransient =
                            if (pendingNodeType == SldNodeType.GENERATOR) {
                                generatorXd
                            } else {
                                0.0
                            },
                        sourceShortCircuitMva =
                            if (pendingNodeType == SldNodeType.SOURCE) {
                                sourceMva
                            } else {
                                0.0
                            }
                    )
                } else {
                    node
                }
            }
        } else {
            val index = nodes.size

            nodes = nodes + SldNode(
                id = UUID.randomUUID().toString(),
                name = normalizedName,
                type = pendingNodeType,
                x = 280f + (index % 4) * 190f,
                y = 120f + (index / 4) * 140f,
                voltage = voltage,
                loadKw =
                    if (pendingNodeType == SldNodeType.LOAD) {
                        kw
                    } else {
                        0.0
                    },
                powerFactor = pf,
                demandFactor = demand,
                ratedKva =
                    if (
                        pendingNodeType == SldNodeType.TRANSFORMER ||
                        pendingNodeType == SldNodeType.GENERATOR
                    ) {
                        kva
                    } else {
                        0.0
                    },
                transformerPercentZ =
                    if (pendingNodeType == SldNodeType.TRANSFORMER) {
                        transformerZ
                    } else {
                        0.0
                    },
                generatorXdSubtransient =
                    if (pendingNodeType == SldNodeType.GENERATOR) {
                        generatorXd
                    } else {
                        0.0
                    },
                sourceShortCircuitMva =
                    if (pendingNodeType == SldNodeType.SOURCE) {
                        sourceMva
                    } else {
                        0.0
                    }
            )
        }

        showNodeDialog = false
        editingNodeId = null
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
            connectionParallelRuns.toIntOrNull()
                ?.coerceAtLeast(1)
                ?: 1

        val capacity =
            connectionCurrentCapacity.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        connections = connections.map { connection ->
            if (connection.id == editingConnectionId) {
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

        showConnectionDialog = false
        editingConnectionId = null
    }

    fun deleteSelected() {
        val nodeId = selectedNodeId

        if (nodeId != null) {
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

    fun connect(first: String, second: String) {
        if (first == second) {
            connectionStartId = null
            return
        }

        val exists = connections.any {
            (
                it.fromNodeId == first &&
                    it.toNodeId == second
                ) ||
                (
                    it.fromNodeId == second &&
                        it.toNodeId == first
                    )
        }

        if (!exists) {
            connections = connections + SldConnection(
                id = UUID.randomUUID().toString(),
                fromNodeId = first,
                toNodeId = second
            )
        }

        connectionStartId = null
    }

    fun calculate() {
        calculationResult =
            try {
                SldEngineeringEngine.calculateUpstream(
                    SldNetwork(
                        nodes = nodes,
                        connections = connections
                    )
                )
            } catch (e: Exception) {
                SldCalculationResult(
                    nodeResults = emptyMap(),
                    totalConnectedLoadKw = 0.0,
                    totalDemandLoadKw = 0.0,
                    totalRequiredKva = 0.0,
                    mainCurrentA = 0.0,
                    mainBreakerA = 0.0,
                    requiredTransformerKva = 0.0,
                    totalVoltageDropPercent = 0.0,
                    notes = listOf(
                        e.message ?: "SLD calculation error"
                    )
                )
            }

        showResults = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        SldToolbar(
            arabic = arabic,
            hasSelection =
                selectedNodeId != null ||
                    selectedConnectionId != null,
            connecting = connectionStartId != null,
            onAdd = ::openAdd,
            onConnect = {
                selectedNodeId?.let {
                    connectionStartId = it
                }
            },
            onEdit = {
                selectedNodeId?.let { id ->
                    nodes.firstOrNull {
                        it.id == id
                    }?.let(::openEdit)
                } ?: selectedConnectionId?.let { id ->
                    connections.firstOrNull {
                        it.id == id
                    }?.let(::openConnectionEdit)
                }
            },
            onDelete = ::deleteSelected,
            onCalculate = ::calculate
        )

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .border(
                        width = 1.dp,
                        color = Color(0xFF30465A),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                SldCanvas(
                    nodes = nodes,
                    connections = connections,
                    selectedNodeId = selectedNodeId,
                    selectedConnectionId = selectedConnectionId,
                    connectionStartId = connectionStartId,
                    onSelectNode = { id ->
                        selectedConnectionId = null

                        if (connectionStartId != null) {
                            val start = connectionStartId

                            if (start != null && start != id) {
                                connect(start, id)
                            }

                            selectedNodeId = id
                        } else {
                            selectedNodeId = id
                        }
                    },
                    onSelectConnection = { id ->
                        selectedNodeId = null
                        connectionStartId = null
                        selectedConnectionId = id
                    },
                    onMoveNode = { id, dx, dy ->
                        nodes = nodes.map { node ->
                            if (node.id == id) {
                                node.copy(
                                    x = max(10f, node.x + dx),
                                    y = max(10f, node.y + dy)
                                )
                            } else {
                                node
                            }
                        }
                    },
                    onDoubleClickNode = { id ->
                        nodes.firstOrNull {
                            it.id == id
                        }?.let(::openEdit)
                    },
                    onDoubleClickConnection = { id ->
                        connections.firstOrNull {
                            it.id == id
                        }?.let {
                            selectedNodeId = null
                            selectedConnectionId = id
                            openConnectionEdit(it)
                        }
                    }
                )

                if (connectionStartId != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF17343D)
                        )
                    ) {
                        Text(
                            text =
                                if (arabic) {
                                    "اختر العنصر المراد توصيله"
                                } else {
                                    "Select the element to connect"
                                },
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 9.dp
                            )
                        )
                    }
                }
            }

            SldSidePanel(
                arabic = arabic,
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                result = calculationResult,
                onSelectNode = {
                    selectedConnectionId = null
                    selectedNodeId = it
                },
                onSelectConnection = {
                    selectedNodeId = null
                    selectedConnectionId = it
                },
                onResults = {
                    showResults = true
                }
            )
        }
    }

    if (showNodeDialog) {
        SldNodeDialog(
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
            onSave = ::saveNode,
            onDismiss = {
                showNodeDialog = false
            }
        )
    }

    if (showConnectionDialog) {
        SldConnectionDialog(
            arabic = arabic,
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
            onSave = ::saveConnection,
            onDismiss = {
                showConnectionDialog = false
            }
        )
    }

    if (showResults) {
        SldResultsDialog(
            arabic = arabic,
            result = calculationResult,
            onDismiss = {
                showResults = false
            }
        )
    }
}

@Composable
private fun SldToolbar(
    arabic: Boolean,
    hasSelection: Boolean,
    connecting: Boolean,
    onAdd: (SldNodeType) -> Unit,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCalculate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101B24))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ToolButton(
                text = if (arabic) "مصدر" else "Source"
            ) {
                onAdd(SldNodeType.SOURCE)
            }

            ToolButton(
                text = if (arabic) "محول" else "Transformer"
            ) {
                onAdd(SldNodeType.TRANSFORMER)
            }

            ToolButton(
                text = if (arabic) "مولد" else "Generator"
            ) {
                onAdd(SldNodeType.GENERATOR)
            }

            ToolButton(text = "Bus") {
                onAdd(SldNodeType.BUS)
            }

            ToolButton(
                text = if (arabic) "لوحة" else "Panel"
            ) {
                onAdd(SldNodeType.PANEL)
            }

            ToolButton(
                text = if (arabic) "قاطع" else "Breaker"
            ) {
                onAdd(SldNodeType.BREAKER)
            }

            ToolButton(
                text = if (arabic) "حمل" else "Load"
            ) {
                onAdd(SldNodeType.LOAD)
            }
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onConnect,
                enabled = hasSelection && !connecting
            ) {
                Text(
                    if (connecting) {
                        if (arabic) "اختر العنصر" else "Select"
                    } else {
                        if (arabic) "توصيل" else "Connect"
                    }
                )
            }

            Button(
                onClick = onEdit,
                enabled = hasSelection
            ) {
                Text(
                    if (arabic) "تعديل" else "Edit"
                )
            }

            Button(
                onClick = onDelete,
                enabled = hasSelection
            ) {
                Text(
                    if (arabic) "حذف" else "Delete"
                )
            }

            Button(
                onClick = onCalculate
            ) {
                Text(
                    if (arabic) {
                        "حساب Upstream"
                    } else {
                        "Calculate Upstream"
                    }
                )
            }
        }
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
            fontSize = 10.sp,
            maxLines = 1
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
    onDoubleClickNode: (String) -> Unit,
    onDoubleClickConnection: (String) -> Unit
) {
    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1218))
            .pointerInput(nodes, connections) {
                detectTapGestures(
                    onTap = { offset ->
                        val node = findNode(
                            nodes,
                            offset.x,
                            offset.y
                        )

                        if (node != null) {
                            onSelectNode(node.id)
                            return@detectTapGestures
                        }

                        val connection = findConnection(
                            nodes,
                            connections,
                            offset.x,
                            offset.y
                        )

                        if (connection != null) {
                            onSelectConnection(connection.id)
                        }
                    },
                    onDoubleTap = { offset ->
                        val node = findNode(
                            nodes,
                            offset.x,
                            offset.y
                        )

                        if (node != null) {
                            onDoubleClickNode(node.id)
                            return@detectTapGestures
                        }

                        findConnection(
                            nodes,
                            connections,
                            offset.x,
                            offset.y
                        )?.let {
                            onDoubleClickConnection(it.id)
                        }
                    }
                )
            }
            .pointerInput(nodes) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggedNodeId =
                            findNode(
                                nodes,
                                offset.x,
                                offset.y
                            )?.id
                    },
                    onDragEnd = {
                        draggedNodeId = null
                    },
                    onDragCancel = {
                        draggedNodeId = null
                    },
                    onDrag = { change, amount ->
                        change.consume()

                        draggedNodeId?.let { id ->
                            onMoveNode(
                                id,
                                amount.x,
                                amount.y
                            )
                        }
                    }
                )
            }
    ) {
        drawGrid()

        val nodeMap = nodes.associateBy {
            it.id
        }

        connections.forEach { connection ->
            val from = nodeMap[connection.fromNodeId]
            val to = nodeMap[connection.toNodeId]

            if (from != null && to != null) {
                drawConnection(
                    start = nodeCenter(from),
                    end = nodeCenter(to),
                    selected =
                        connection.id ==
                            selectedConnectionId,
                    connection = connection
                )
            }
        }

        nodes.forEach { node ->
            drawNode(
                node = node,
                selected = node.id == selectedNodeId,
                connectionStart =
                    node.id == connectionStartId
            )
        }
    }
}

private fun findNode(
    nodes: List<SldNode>,
    x: Float,
    y: Float
): SldNode? {
    return nodes.lastOrNull { node ->
        x >= node.x &&
            x <= node.x + NODE_WIDTH &&
            y >= node.y &&
            y <= node.y + NODE_HEIGHT
    }
}

private fun findConnection(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    x: Float,
    y: Float
): SldConnection? {
    val nodeMap = nodes.associateBy {
        it.id
    }

    val tolerance = 18f

    return connections.lastOrNull { connection ->
        val from = nodeMap[connection.fromNodeId]
        val to = nodeMap[connection.toNodeId]

        if (from == null || to == null) {
            false
        } else {
            val start = nodeCenter(from)
            val end = nodeCenter(to)
            val midX = (start.x + end.x) / 2f

            val d1 = distanceToSegment(
                x,
                y,
                start.x,
                start.y,
                midX,
                start.y
            )

            val d2 = distanceToSegment(
                x,
                y,
                midX,
                start.y,
                midX,
                end.y
            )

            val d3 = distanceToSegment(
                x,
                y,
                midX,
                end.y,
                end.x,
                end.y
            )

            minOf(d1, d2, d3) <= tolerance
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
        return sqrt(
            (px - x1) * (px - x1) +
                (py - y1) * (py - y1)
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

    val cx = x1 + clamped * dx
    val cy = y1 + clamped * dy

    return sqrt(
        (px - cx) * (px - cx) +
            (py - cy) * (py - cy)
    )
}

private fun nodeCenter(
    node: SldNode
): Offset {
    return Offset(
        node.x + NODE_WIDTH / 2f,
        node.y + NODE_HEIGHT / 2f
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid() {
    val step = 40f

    var x = 0f

    while (x <= size.width) {
        drawLine(
            color = Color(0xFF182630),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )

        x += step
    }

    var y = 0f

    while (y <= size.height) {
        drawLine(
            color = Color(0xFF182630),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )

        y += step
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnection(
    start: Offset,
    end: Offset,
    selected: Boolean,
    connection: SldConnection
) {
    val lineColor =
        if (selected) {
            Color(0xFFFFC107)
        } else {
            Color(0xFF66D9EF)
        }

    val midX = (start.x + end.x) / 2f

    drawLine(
        color = lineColor,
        start = start,
        end = Offset(midX, start.y),
        strokeWidth =
            if (selected) 7f else 5f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = lineColor,
        start = Offset(midX, start.y),
        end = Offset(midX, end.y),
        strokeWidth =
            if (selected) 7f else 5f
    )

    drawLine(
        color = lineColor,
        start = Offset(midX, end.y),
        end = end,
        strokeWidth =
            if (selected) 7f else 5f,
        cap = StrokeCap.Round
    )

    val arrowY =
        if (end.y >= start.y) {
            end.y - 18f
        } else {
            end.y + 18f
        }

    drawCircle(
        color = lineColor,
        radius = 4f,
        center = Offset(
            midX,
            arrowY
        )
    )

    if (connection.lengthMeters > 0.0) {
        drawCircle(
            color = lineColor,
            radius = 3f,
            center = Offset(
                midX,
                (start.y + end.y) / 2f
            )
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    val borderColor =
        when {
            connectionStart ->
                Color(0xFFFFC107)

            selected ->
                PrimaryTeal

            else ->
                Color(0xFF526777)
        }

    drawRoundRect(
        color = Color(0xFF14212B),
        topLeft = Offset(
            node.x,
            node.y
        ),
        size = Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius = CornerRadius(10f)
    )

    drawRoundRect(
        color = borderColor,
        topLeft = Offset(
            node.x,
            node.y
        ),
        size = Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius = CornerRadius(10f),
        style = Stroke(
            width =
                if (selected) {
                    4f
                } else {
                    2f
                }
        )
    )

    val center = nodeCenter(node)

    drawSymbol(
        type = node.type,
        center = Offset(
            center.x,
            center.y - 10f
        )
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSymbol(
    type: SldNodeType,
    center: Offset
) {
    val color = Color(0xFF66D9EF)

    when (type) {
        SldNodeType.SOURCE -> {
            drawCircle(
                color = color,
                radius = 18f,
                center = center,
                style = Stroke(3f)
            )

            drawLine(
                color = color,
                start = Offset(
                    center.x,
                    center.y + 18f
                ),
                end = Offset(
                    center.x,
                    center.y + 30f
                ),
                strokeWidth = 3f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawCircle(
                color = color,
                radius = 14f,
                center = Offset(
                    center.x - 9f,
                    center.y
                ),
                style = Stroke(3f)
            )

            drawCircle(
                color = color,
                radius = 14f,
                center = Offset(
                    center.x + 9f,
                    center.y
                ),
                style = Stroke(3f)
            )
        }

        SldNodeType.GENERATOR -> {
            drawCircle(
                color = color,
                radius = 18f,
                center = center,
                style = Stroke(3f)
            )

            drawLine(
                color = color,
                start = Offset(
                    center.x - 8f,
                    center.y
                ),
                end = Offset(
                    center.x + 8f,
                    center.y
                ),
                strokeWidth = 3f
            )
        }

        SldNodeType.BUS -> {
            drawLine(
                color = color,
                start = Offset(
                    center.x - 45f,
                    center.y
                ),
                end = Offset(
                    center.x + 45f,
                    center.y
                ),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        SldNodeType.PANEL -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(
                    center.x - 25f,
                    center.y - 18f
                ),
                size = Size(
                    50f,
                    36f
                ),
                cornerRadius = CornerRadius(3f),
                style = Stroke(3f)
            )
        }

        SldNodeType.BREAKER -> {
            drawLine(
                color = color,
                start = Offset(
                    center.x - 30f,
                    center.y
                ),
                end = Offset(
                    center.x - 7f,
                    center.y
                ),
                strokeWidth = 4f
            )

            drawLine(
                color = color,
                start = Offset(
                    center.x + 7f,
                    center.y - 12f
                ),
                end = Offset(
                    center.x + 30f,
                    center.y
                ),
                strokeWidth = 4f
            )
        }

        SldNodeType.LOAD -> {
            drawRect(
                color = color,
                topLeft = Offset(
                    center.x - 23f,
                    center.y - 17f
                ),
                size = Size(
                    46f,
                    34f
                ),
                style = Stroke(3f)
            )
        }
    }
}

@Composable
private fun SldSidePanel(
    arabic: Boolean,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    result: SldCalculationResult?,
    onSelectNode: (String) -> Unit,
    onSelectConnection: (String) -> Unit,
    onResults: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .padding(
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF101B24)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(
                text =
                    if (arabic) {
                        "مخطط SLD"
                    } else {
                        "SLD SYSTEM"
                    },
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    if (arabic) {
                        "العناصر: ${nodes.size}   التوصيلات: ${connections.size}"
                    } else {
                        "Nodes: ${nodes.size}   Connections: ${connections.size}"
                    },
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(
                    items = nodes,
                    key = {
                        it.id
                    }
                ) { node ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectNode(node.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (node.id == selectedNodeId) {
                                    Color(0xFF174C57)
                                } else {
                                    Color(0xFF182630)
                                }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = node.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Text(
                                text = nodeTypeName(
                                    node.type,
                                    arabic
                                ),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            Text(
                                text =
                                    "%.0f V"
                                        .format(node.voltage),
                                color = TextSecondary,
                                fontSize = 10.sp
                            )

                            if (node.type == SldNodeType.LOAD) {
                                Text(
                                    text =
                                        "%.2f kW | PF %.2f | DF %.2f"
                                            .format(
                                                node.loadKw,
                                                node.powerFactor,
                                                node.demandFactor
                                            ),
                                    color = PrimaryTeal,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (connections.isNotEmpty()) {
                    item {
                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                if (arabic) {
                                    "المغذيات"
                                } else {
                                    "Feeders"
                                },
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(
                        items = connections,
                        key = {
                            it.id
                        }
                    ) { connection ->
                        val from =
                            nodes.firstOrNull {
                                it.id == connection.fromNodeId
                            }

                        val to =
                            nodes.firstOrNull {
                                it.id == connection.toNodeId
                            }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectConnection(
                                        connection.id
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    if (
                                        connection.id ==
                                        selectedConnectionId
                                    ) {
                                        Color(0xFF174C57)
                                    } else {
                                        Color(0xFF182630)
                                    }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text =
                                        "${from?.name ?: "?"} → ${to?.name ?: "?"}",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text =
                                        "%.1f m | %.1f mm² | %d run"
                                            .format(
                                                connection.lengthMeters,
                                                connection.cableSizeMm2,
                                                connection.parallelRuns
                                            ),
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )

                                Text(
                                    text =
                                        "R %.4f Ω/km | X %.4f Ω/km"
                                            .format(
                                                connection.resistanceOhmPerKm,
                                                connection.reactanceOhmPerKm
                                            ),
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            if (result != null) {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = onResults,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (arabic) {
                            "عرض النتائج"
                        } else {
                            "View Results"
                        }
                    )
                }
            }
        }
    }
}

private fun nodeTypeName(
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
            "Bus"

        SldNodeType.PANEL ->
            if (arabic) "لوحة" else "Panel"

        SldNodeType.BREAKER ->
            if (arabic) "قاطع" else "Breaker"

        SldNodeType.LOAD ->
            if (arabic) "حمل" else "Load"
    }
}

@Composable
private fun SldNodeDialog(
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = {
                            Text(
                                if (arabic) {
                                    "الاسم"
                                } else {
                                    "Name"
                                }
                            )
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = voltage,
                        onValueChange = onVoltageChange,
                        label = {
                            Text("Voltage V")
                        },
                        singleLine = true
                    )
                }

                if (type == SldNodeType.LOAD) {
                    item {
                        OutlinedTextField(
                            value = kw,
                            onValueChange = onKwChange,
                            label = {
                                Text("Load kW")
                            },
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = pf,
                            onValueChange = onPfChange,
                            label = {
                                Text("Power Factor")
                            },
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = demand,
                            onValueChange = onDemandChange,
                            label = {
                                Text("Demand Factor")
                            },
                            singleLine = true
                        )
                    }
                }

                if (
                    type == SldNodeType.TRANSFORMER ||
                    type == SldNodeType.GENERATOR
                ) {
                    item {
                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            label = {
                                Text("Rated kVA")
                            },
                            singleLine = true
                        )
                    }
                }

                if (type == SldNodeType.TRANSFORMER) {
                    item {
                        OutlinedTextField(
                            value = transformerZ,
                            onValueChange = onTransformerZChange,
                            label = {
                                Text("Transformer %Z")
                            },
                            singleLine = true
                        )
                    }
                }

                if (type == SldNodeType.GENERATOR) {
                    item {
                        OutlinedTextField(
                            value = generatorXd,
                            onValueChange = onGeneratorXdChange,
                            label = {
                                Text("Generator Xd'' %")
                            },
                            singleLine = true
                        )
                    }
                }

                if (type == SldNodeType.SOURCE) {
                    item {
                        OutlinedTextField(
                            value = sourceMva,
                            onValueChange = onSourceMvaChange,
                            label = {
                                Text("Source Short-Circuit MVA")
                            },
                            singleLine = true
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
private fun SldConnectionDialog(
    arabic: Boolean,
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
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (arabic) {
                    "بيانات المغذي"
                } else {
                    "Feeder Data"
                }
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = length,
                        onValueChange = onLengthChange,
                        label = {
                            Text(
                                if (arabic) {
                                    "الطول m"
                                } else {
                                    "Length m"
                                }
                            )
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = resistance,
                        onValueChange = onResistanceChange,
                        label = {
                            Text("R Ω/km")
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = reactance,
                        onValueChange = onReactanceChange,
                        label = {
                            Text("X Ω/km")
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = cableSize,
                        onValueChange = onCableSizeChange,
                        label = {
                            Text(
                                if (arabic) {
                                    "مقطع الكابل mm²"
                                } else {
                                    "Cable Size mm²"
                                }
                            )
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = parallelRuns,
                        onValueChange = onParallelRunsChange,
                        label = {
                            Text(
                                if (arabic) {
                                    "عدد المسارات المتوازية"
                                } else {
                                    "Parallel Runs"
                                }
                            )
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = currentCapacity,
                        onValueChange = onCurrentCapacityChange,
                        label = {
                            Text(
                                if (arabic) {
                                    "تحمل التيار A"
                                } else {
                                    "Current Capacity A"
                                }
                            )
                        },
                        singleLine = true
                    )
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
private fun SldResultsDialog(
    arabic: Boolean,
    result: SldCalculationResult?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (arabic) {
                    "نتائج دراسة SLD"
                } else {
                    "SLD Study Results"
                }
            )
        },
        text = {
            if (result == null) {
                Text(
                    if (arabic) {
                        "لا توجد نتائج."
                    } else {
                        "No results."
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    item {
                        ResultRow(
                            if (arabic) {
                                "الحمل المتصل"
                            } else {
                                "Connected Load"
                            },
                            "%.2f kW".format(
                                result.totalConnectedLoadKw
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) {
                                "أقصى طلب"
                            } else {
                                "Maximum Demand"
                            },
                            "%.2f kW".format(
                                result.totalDemandLoadKw
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) {
                                "القدرة الظاهرية"
                            } else {
                                "Required kVA"
                            },
                            "%.2f kVA".format(
                                result.totalRequiredKva
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) {
                                "التيار الرئيسي"
                            } else {
                                "Main Current"
                            },
                            "%.2f A".format(
                                result.mainCurrentA
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) {
                                "القاطع الرئيسي"
                            } else {
                                "Main Breaker"
                            },
                            "%.0f A".format(
                                result.mainBreakerA
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) {
                                "المحول المقترح"
                            } else {
                                "Transformer"
                            },
                            "%.0f kVA".format(
                                result.requiredTransformerKva
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) {
                                "أقصى هبوط جهد"
                            } else {
                                "Maximum Voltage Drop"
                            },
                            "%.2f %%".format(
                                result.totalVoltageDropPercent
                            )
                        )
                    }

                    item {
                        Text(
                            text =
                                if (arabic) {
                                    "نتائج العناصر"
                                } else {
                                    "Element Results"
                                },
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(
                        result.nodeResults.values.toList(),
                        key = {
                            it.nodeId
                        }
                    ) { item ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF182630)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = item.nodeName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text =
                                        "%.2f kW | %.2f kVA | %.2f A"
                                            .format(
                                                item.demandLoadKw,
                                                item.apparentPowerKva,
                                                item.currentA
                                            ),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )

                                Text(
                                    text =
                                        "Breaker %.0f A"
                                            .format(
                                                item.requiredBreakerA
                                            ),
                                    color = PrimaryTeal,
                                    fontSize = 11.sp
                                )

                                Text(
                                    text =
                                        "Feeder %.2f A | VD %.2f %%"
                                            .format(
                                                item.feederRequiredCurrentA,
                                                item.voltageDropPercent
                                            ),
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    items(
                        result.notes
                    ) { note ->
                        Text(
                            text = note,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (arabic) "إغلاق" else "Close"
                )
            }
        }
    )
}

@Composable
private fun ResultRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
