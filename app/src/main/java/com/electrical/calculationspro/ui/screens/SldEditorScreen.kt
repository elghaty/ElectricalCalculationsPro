package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
                    x = 100f,
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

    var connectionMode by remember {
        mutableStateOf(false)
    }

    var connectionStartId by remember {
        mutableStateOf<String?>(null)
    }

    var showNodeDialog by remember {
        mutableStateOf(false)
    }

    var showResults by remember {
        mutableStateOf(false)
    }

    var selectedType by remember {
        mutableStateOf(SldNodeType.LOAD)
    }

    var editingNodeId by remember {
        mutableStateOf<String?>(null)
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

    var calculationResult by remember {
        mutableStateOf<SldCalculationResult?>(null)
    }

    fun defaultName(type: SldNodeType): String {
        return when (type) {
            SldNodeType.SOURCE ->
                if (arabic) "المصدر" else "SOURCE"

            SldNodeType.TRANSFORMER ->
                if (arabic) "المحول" else "TRANSFORMER"

            SldNodeType.GENERATOR ->
                if (arabic) "المولد" else "GENERATOR"

            SldNodeType.BUS ->
                if (arabic) "الباص" else "BUS"

            SldNodeType.PANEL ->
                if (arabic) "لوحة" else "PANEL"

            SldNodeType.BREAKER ->
                if (arabic) "قاطع" else "BREAKER"

            SldNodeType.LOAD ->
                if (arabic) "حمل" else "LOAD"
        }
    }

    fun openAddDialog(type: SldNodeType) {
        editingNodeId = null
        selectedType = type
        nodeName = defaultName(type)
        nodeVoltage = "400"
        nodeKw = "10"
        nodePf = "0.90"
        nodeDemand = "1.0"
        nodeKva = "1000"
        showNodeDialog = true
    }

    fun openEditDialog(node: SldNode) {
        editingNodeId = node.id
        selectedType = node.type
        nodeName = node.name
        nodeVoltage = node.voltage.toString()
        nodeKw = node.loadKw.toString()
        nodePf = node.powerFactor.toString()
        nodeDemand = node.demandFactor.toString()
        nodeKva = node.ratedKva.toString()
        showNodeDialog = true
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

        val existingId = editingNodeId

        if (existingId != null) {
            nodes = nodes.map { node ->
                if (node.id == existingId) {
                    node.copy(
                        name = nodeName.ifBlank {
                            defaultName(selectedType)
                        },
                        voltage = voltage,
                        loadKw =
                            if (selectedType == SldNodeType.LOAD) {
                                kw
                            } else {
                                0.0
                            },
                        powerFactor = pf,
                        demandFactor = demand,
                        ratedKva =
                            if (
                                selectedType == SldNodeType.TRANSFORMER ||
                                selectedType == SldNodeType.GENERATOR
                            ) {
                                kva
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

            val newNode = SldNode(
                id = UUID.randomUUID().toString(),
                name = nodeName.ifBlank {
                    defaultName(selectedType)
                },
                type = selectedType,
                x = 220f + (index % 4) * 190f,
                y = 120f + (index / 4) * 140f,
                voltage = voltage,
                loadKw =
                    if (selectedType == SldNodeType.LOAD) {
                        kw
                    } else {
                        0.0
                    },
                powerFactor = pf,
                demandFactor = demand,
                ratedKva =
                    if (
                        selectedType == SldNodeType.TRANSFORMER ||
                        selectedType == SldNodeType.GENERATOR
                    ) {
                        kva
                    } else {
                        0.0
                    }
            )

            nodes = nodes + newNode
            selectedNodeId = newNode.id
        }

        showNodeDialog = false
        editingNodeId = null
    }

    fun deleteSelected() {
        val id = selectedNodeId ?: return

        nodes = nodes.filterNot {
            it.id == id
        }

        connections = connections.filter {
            it.fromNodeId != id &&
                it.toNodeId != id
        }

        selectedNodeId = null
        connectionStartId = null
        connectionMode = false
    }

    fun connectNodes(firstId: String, secondId: String) {
        if (firstId == secondId) {
            connectionStartId = null
            connectionMode = false
            return
        }

        val exists = connections.any {
            (
                it.fromNodeId == firstId &&
                    it.toNodeId == secondId
                ) ||
                (
                    it.fromNodeId == secondId &&
                        it.toNodeId == firstId
                    )
        }

        if (!exists) {
            connections = connections + SldConnection(
                id = UUID.randomUUID().toString(),
                fromNodeId = firstId,
                toNodeId = secondId
            )
        }

        connectionStartId = null
        connectionMode = false
    }

    fun calculate() {
        if (nodes.isEmpty()) {
            return
        }

        calculationResult =
            try {
                SldEngineeringEngine.calculateUpstream(
                    SldNetwork(
                        nodes = nodes,
                        connections = connections
                    )
                )
            } catch (e: Exception) {
                null
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
            connectionMode = connectionMode,
            hasSelection = selectedNodeId != null,
            onAdd = ::openAddDialog,
            onConnect = {
                val selected = selectedNodeId

                if (selected != null) {
                    connectionMode = true
                    connectionStartId = selected
                }
            },
            onEdit = {
                selectedNodeId?.let { id ->
                    nodes.firstOrNull {
                        it.id == id
                    }?.let(::openEditDialog)
                }
            },
            onDelete = ::deleteSelected,
            onCalculate = ::calculate
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        1.dp,
                        Color(0xFF30465A),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                SldCanvas(
                    nodes = nodes,
                    connections = connections,
                    selectedNodeId = selectedNodeId,
                    connectionStartId = connectionStartId,
                    connectionMode = connectionMode,
                    onSelectNode = { id ->
                        if (connectionMode) {
                            val start = connectionStartId

                            if (start == null) {
                                connectionStartId = id
                                selectedNodeId = id
                            } else {
                                connectNodes(start, id)
                                selectedNodeId = id
                            }
                        } else {
                            selectedNodeId = id
                        }
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
                    onDoubleClickNode = { id ->
                        nodes.firstOrNull {
                            it.id == id
                        }?.let(::openEditDialog)
                    }
                )

                if (connectionMode) {
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
                                    "وضع التوصيل: اختر العنصر التالي"
                                } else {
                                    "Connection mode: select the next element"
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

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            SldSidePanel(
                arabic = arabic,
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                result = calculationResult,
                onSelect = {
                    selectedNodeId = it
                },
                onCalculate = ::calculate,
                onShowResults = {
                    showResults = true
                }
            )
        }
    }

    if (showNodeDialog) {
        SldNodeDialog(
            arabic = arabic,
            type = selectedType,
            name = nodeName,
            voltage = nodeVoltage,
            kw = nodeKw,
            pf = nodePf,
            demand = nodeDemand,
            kva = nodeKva,
            editing = editingNodeId != null,
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
            onSave = ::saveNode,
            onDismiss = {
                showNodeDialog = false
                editingNodeId = null
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
    connectionMode: Boolean,
    hasSelection: Boolean,
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SldToolButton(
                text = if (arabic) "مصدر" else "Source",
                onClick = {
                    onAdd(SldNodeType.SOURCE)
                }
            )

            SldToolButton(
                text = if (arabic) "محول" else "Transformer",
                onClick = {
                    onAdd(SldNodeType.TRANSFORMER)
                }
            )

            SldToolButton(
                text = if (arabic) "مولد" else "Generator",
                onClick = {
                    onAdd(SldNodeType.GENERATOR)
                }
            )

            SldToolButton(
                text = if (arabic) "Bus" else "Bus",
                onClick = {
                    onAdd(SldNodeType.BUS)
                }
            )

            SldToolButton(
                text = if (arabic) "لوحة" else "Panel",
                onClick = {
                    onAdd(SldNodeType.PANEL)
                }
            )

            SldToolButton(
                text = if (arabic) "قاطع" else "Breaker",
                onClick = {
                    onAdd(SldNodeType.BREAKER)
                }
            )

            SldToolButton(
                text = if (arabic) "حمل" else "Load",
                onClick = {
                    onAdd(SldNodeType.LOAD)
                }
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onConnect,
                enabled = hasSelection
            ) {
                Text(
                    if (connectionMode) {
                        if (arabic) "اختر العنصر" else "Select element"
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
private fun SldToolButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(42.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SldCanvas(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    connectionStartId: String?,
    connectionMode: Boolean,
    onSelectNode: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onDoubleClickNode: (String) -> Unit
) {
    val nodeMap = nodes.associateBy {
        it.id
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1218))
            .pointerInput(
                nodes,
                connections,
                selectedNodeId,
                connectionStartId,
                connectionMode
            ) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val node = findNodeAt(
                            nodes,
                            offset.x,
                            offset.y
                        )

                        if (node != null) {
                            onSelectNode(node.id)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val node =
                            findNodeAt(
                                nodes,
                                change.position.x,
                                change.position.y
                            )

                        if (node != null) {
                            onMoveNode(
                                node.id,
                                dragAmount.x,
                                dragAmount.y
                            )
                        }
                    }
                )
            }
            .pointerInput(nodes, connectionMode) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onDoubleTap = { offset ->
                        findNodeAt(
                            nodes,
                            offset.x,
                            offset.y
                        )?.let {
                            onDoubleClickNode(it.id)
                        }
                    },
                    onTap = { offset ->
                        findNodeAt(
                            nodes,
                            offset.x,
                            offset.y
                        )?.let {
                            onSelectNode(it.id)
                        }
                    }
                )
            }
    ) {
        drawGrid()

        connections.forEach { connection ->
            val from = nodeMap[connection.fromNodeId]
            val to = nodeMap[connection.toNodeId]

            if (from != null && to != null) {
                val start = nodeCenter(from)
                val end = nodeCenter(to)

                drawConnection(
                    start = start,
                    end = end
                )
            }
        }

        nodes.forEach { node ->
            drawSldNode(
                node = node,
                selected =
                    node.id == selectedNodeId,
                connectionStart =
                    node.id == connectionStartId
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid() {
    val grid = 40f

    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = Color(0xFF182630),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += grid
    }

    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = Color(0xFF182630),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += grid
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnection(
    start: Offset,
    end: Offset
) {
    val horizontal = abs(end.x - start.x) >= abs(end.y - start.y)

    if (horizontal) {
        val midX = (start.x + end.x) / 2f

        drawLine(
            color = Color(0xFF66D9EF),
            start = start,
            end = Offset(midX, start.y),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF66D9EF),
            start = Offset(midX, start.y),
            end = Offset(midX, end.y),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF66D9EF),
            start = Offset(midX, end.y),
            end = end,
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    } else {
        val midY = (start.y + end.y) / 2f

        drawLine(
            color = Color(0xFF66D9EF),
            start = start,
            end = Offset(start.x, midY),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF66D9EF),
            start = Offset(start.x, midY),
            end = Offset(end.x, midY),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = Color(0xFF66D9EF),
            start = Offset(end.x, midY),
            end = end,
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSldNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    val left = node.x
    val top = node.y
    val center = Offset(
        node.x + NODE_WIDTH / 2f,
        node.y + NODE_HEIGHT / 2f
    )

    val outline =
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
        topLeft = Offset(left, top),
        size = Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            10f,
            10f
        )
    )

    drawRoundRect(
        color = outline,
        topLeft = Offset(left, top),
        size = Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            10f,
            10f
        ),
        style = Stroke(
            width = if (selected) 4f else 2f
        )
    )

    drawNodeSymbol(
        node = node,
        center = center
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNodeSymbol(
    node: SldNode,
    center: Offset
) {
    val symbolColor = Color(0xFF66D9EF)

    when (node.type) {
        SldNodeType.SOURCE -> {
            drawCircle(
                color = symbolColor,
                radius = 18f,
                center = Offset(
                    center.x,
                    center.y - 10f
                ),
                style = Stroke(3f)
            )

            drawLine(
                color = symbolColor,
                start = Offset(
                    center.x,
                    center.y + 8f
                ),
                end = Offset(
                    center.x,
                    center.y + 27f
                ),
                strokeWidth = 3f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawCircle(
                color = symbolColor,
                radius = 14f,
                center = Offset(
                    center.x - 9f,
                    center.y - 10f
                ),
                style = Stroke(3f)
            )

            drawCircle(
                color = symbolColor,
                radius = 14f,
                center = Offset(
                    center.x + 9f,
                    center.y - 10f
                ),
                style = Stroke(3f)
            )
        }

        SldNodeType.GENERATOR -> {
            drawCircle(
                color = symbolColor,
                radius = 18f,
                center = Offset(
                    center.x,
                    center.y - 10f
                ),
                style = Stroke(3f)
            )

            drawLine(
                color = symbolColor,
                start = Offset(
                    center.x - 8f,
                    center.y - 10f
                ),
                end = Offset(
                    center.x + 8f,
                    center.y - 10f
                ),
                strokeWidth = 3f
            )
        }

        SldNodeType.BUS -> {
            drawLine(
                color = symbolColor,
                start = Offset(
                    center.x - 45f,
                    center.y - 10f
                ),
                end = Offset(
                    center.x + 45f,
                    center.y - 10f
                ),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        SldNodeType.PANEL -> {
            drawRect(
                color = symbolColor,
                topLeft = Offset(
                    center.x - 24f,
                    center.y - 25f
                ),
                size = Size(
                    48f,
                    30f
                ),
                style = Stroke(3f)
            )
        }

        SldNodeType.BREAKER -> {
            drawLine(
                color = symbolColor,
                start = Offset(
                    center.x - 25f,
                    center.y - 10f
                ),
                end = Offset(
                    center.x - 5f,
                    center.y - 10f
                ),
                strokeWidth = 4f
            )

            drawLine(
                color = symbolColor,
                start = Offset(
                    center.x + 5f,
                    center.y - 18f
                ),
                end = Offset(
                    center.x + 25f,
                    center.y - 10f
                ),
                strokeWidth = 4f
            )
        }

        SldNodeType.LOAD -> {
            drawRect(
                color = symbolColor,
                topLeft = Offset(
                    center.x - 22f,
                    center.y - 23f
                ),
                size = Size(
                    44f,
                    28f
                ),
                style = Stroke(3f)
            )
        }
    }
}

private fun findNodeAt(
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

private fun nodeCenter(node: SldNode): Offset {
    return Offset(
        node.x + NODE_WIDTH / 2f,
        node.y + NODE_HEIGHT / 2f
    )
}

@Composable
private fun SldSidePanel(
    arabic: Boolean,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    result: SldCalculationResult?,
    onSelect: (String) -> Unit,
    onCalculate: () -> Unit,
    onShowResults: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(310.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF101B24)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text =
                    if (arabic) {
                        "شبكة SLD"
                    } else {
                        "SLD NETWORK"
                    },
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    if (arabic) {
                        "العناصر: ${nodes.size}"
                    } else {
                        "Elements: ${nodes.size}"
                    },
                color = TextSecondary,
                fontSize = 13.sp
            )

            Text(
                text =
                    if (arabic) {
                        "التوصيلات: ${connections.size}"
                    } else {
                        "Connections: ${connections.size}"
                    },
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                onSelect(node.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (
                                    node.id ==
                                    selectedNodeId
                                ) {
                                    Color(0xFF174C57)
                                } else {
                                    Color(0xFF182630)
                                }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(9.dp)
                        ) {
                            Text(
                                text = node.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Text(
                                text =
                                    nodeTypeText(
                                        node.type,
                                        arabic
                                    ),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            if (
                                node.type ==
                                SldNodeType.LOAD
                            ) {
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
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = onCalculate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (arabic) {
                        "احسب الشبكة"
                    } else {
                        "Calculate Network"
                    }
                )
            }

            if (result != null) {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = onShowResults,
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

private fun nodeTypeText(
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
    type: SldNodeType,
    name: String,
    voltage: String,
    kw: String,
    pf: String,
    demand: String,
    kva: String,
    editing: Boolean,
    onNameChange: (String) -> Unit,
    onVoltageChange: (String) -> Unit,
    onKwChange: (String) -> Unit,
    onPfChange: (String) -> Unit,
    onDemandChange: (String) -> Unit,
    onKvaChange: (String) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text(
                            if (arabic) "الاسم" else "Name"
                        )
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

                if (type == SldNodeType.LOAD) {
                    OutlinedTextField(
                        value = kw,
                        onValueChange = onKwChange,
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

                if (
                    type == SldNodeType.TRANSFORMER ||
                    type == SldNodeType.GENERATOR
                ) {
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
                        "تعذر إجراء الحساب. راجع توصيلات المخطط والبيانات."
                    } else {
                        "Calculation failed. Check the SLD connections and data."
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    item {
                        ResultLine(
                            label =
                                if (arabic) {
                                    "الحمل المتصل"
                                } else {
                                    "Connected Load"
                                },
                            value =
                                "%.2f kW"
                                    .format(
                                        result.totalConnectedLoadKw
                                    )
                        )
                    }

                    item {
                        ResultLine(
                            label =
                                if (arabic) {
                                    "أقصى طلب"
                                } else {
                                    "Maximum Demand"
                                },
                            value =
                                "%.2f kW"
                                    .format(
                                        result.totalDemandLoadKw
                                    )
                        )
                    }

                    item {
                        ResultLine(
                            label =
                                if (arabic) {
                                    "القدرة الظاهرية"
                                } else {
                                    "Required Apparent Power"
                                },
                            value =
                                "%.2f kVA"
                                    .format(
                                        result.totalRequiredKva
                                    )
                        )
                    }

                    item {
                        ResultLine(
                            label =
                                if (arabic) {
                                    "التيار الرئيسي"
                                } else {
                                    "Main Current"
                                },
                            value =
                                "%.2f A"
                                    .format(
                                        result.mainCurrentA
                                    )
                        )
                    }

                    item {
                        ResultLine(
                            label =
                                if (arabic) {
                                    "القاطع الرئيسي"
                                } else {
                                    "Main Breaker"
                                },
                            value =
                                "%.0f A"
                                    .format(
                                        result.mainBreakerA
                                    )
                        )
                    }

                    item {
                        ResultLine(
                            label =
                                if (arabic) {
                                    "المحول المقترح"
                                } else {
                                    "Recommended Transformer"
                                },
                            value =
                                "%.0f kVA"
                                    .format(
                                        result.requiredTransformerKva
                                    )
                        )
                    }

                    item {
                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

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
                    ) { nodeResult ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF182630)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(9.dp)
                            ) {
                                Text(
                                    text = nodeResult.nodeName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text =
                                        "%.2f kW | %.2f kVA | %.2f A"
                                            .format(
                                                nodeResult.demandLoadKw,
                                                nodeResult.apparentPowerKva,
                                                nodeResult.currentA
                                            ),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )

                                Text(
                                    text =
                                        "Breaker %.0f A"
                                            .format(
                                                nodeResult.requiredBreakerA
                                            ),
                                    color = PrimaryTeal,
                                    fontSize = 12.sp
                                )
                            }
                        }
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
private fun ResultLine(
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
            fontSize = 13.sp
        )

        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
