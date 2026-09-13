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

    var connectionStartId by remember {
        mutableStateOf<String?>(null)
    }

    var showNodeDialog by remember {
        mutableStateOf(false)
    }

    var showResults by remember {
        mutableStateOf(false)
    }

    var editingNodeId by remember {
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

        if (editingNodeId != null) {
            nodes = nodes.map { node ->
                if (node.id == editingNodeId) {
                    node.copy(
                        name = nodeName.ifBlank {
                            defaultName(pendingNodeType)
                        },
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
                name = nodeName.ifBlank {
                    defaultName(pendingNodeType)
                },
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
                    }
            )
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
        try {
            calculationResult =
                SldEngineeringEngine.calculateUpstream(
                    SldNetwork(
                        nodes = nodes,
                        connections = connections
                    )
                )

            showResults = true
        } catch (e: Exception) {
            calculationResult = SldCalculationResult(
                nodeResults = emptyMap(),
                totalConnectedLoadKw = 0.0,
                totalDemandLoadKw = 0.0,
                totalRequiredKva = 0.0,
                mainCurrentA = 0.0,
                mainBreakerA = 0.0,
                requiredTransformerKva = 0.0,
                notes = listOf(
                    e.message ?: "SLD calculation error"
                )
            )

            showResults = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        SldToolbar(
            arabic = arabic,
            hasSelection = selectedNodeId != null,
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
                    connectionStartId = connectionStartId,
                    onSelectNode = { id ->
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
                result = calculationResult,
                onSelect = {
                    selectedNodeId = it
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
                enabled = hasSelection
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
    connectionStartId: String?,
    onSelectNode: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onDoubleClickNode: (String) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1218))
            .pointerInput(nodes) {
                detectTapGestures(
                    onTap = { offset ->
                        findNode(
                            nodes,
                            offset.x,
                            offset.y
                        )?.let {
                            onSelectNode(it.id)
                        }
                    },
                    onDoubleTap = { offset ->
                        findNode(
                            nodes,
                            offset.x,
                            offset.y
                        )?.let {
                            onDoubleClickNode(it.id)
                        }
                    }
                )
            }
            .pointerInput(nodes) {
                detectDragGestures(
                    onDragStart = { offset ->
                        findNode(
                            nodes,
                            offset.x,
                            offset.y
                        )?.let {
                            onSelectNode(it.id)
                        }
                    },
                    onDrag = { change, amount ->
                        findNode(
                            nodes,
                            change.position.x,
                            change.position.y
                        )?.let {
                            onMoveNode(
                                it.id,
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
                    nodeCenter(from),
                    nodeCenter(to)
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
    end: Offset
) {
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
        strokeWidth = 5f
    )

    drawLine(
        color = Color(0xFF66D9EF),
        start = Offset(midX, end.y),
        end = end,
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
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
        topLeft = Offset(node.x, node.y),
        size = Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius = CornerRadius(10f)
    )

    drawRoundRect(
        color = borderColor,
        topLeft = Offset(node.x, node.y),
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
    result: SldCalculationResult?,
    onSelect: (String) -> Unit,
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
                                onSelect(node.id)
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
                verticalArrangement = Arrangement.spacedBy(7.dp)
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
                        Text("Voltage V")
                    },
                    singleLine = true
                )

                if (type == SldNodeType.LOAD) {
                    OutlinedTextField(
                        value = kw,
                        onValueChange = onKwChange,
                        label = {
                            Text("Load kW")
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
                            if (arabic) "الحمل المتصل" else "Connected Load",
                            "%.2f kW".format(
                                result.totalConnectedLoadKw
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) "أقصى طلب" else "Maximum Demand",
                            "%.2f kW".format(
                                result.totalDemandLoadKw
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) "القدرة الظاهرية" else "Required kVA",
                            "%.2f kVA".format(
                                result.totalRequiredKva
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) "التيار الرئيسي" else "Main Current",
                            "%.2f A".format(
                                result.mainCurrentA
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) "القاطع الرئيسي" else "Main Breaker",
                            "%.0f A".format(
                                result.mainBreakerA
                            )
                        )
                    }

                    item {
                        ResultRow(
                            if (arabic) "المحول المقترح" else "Transformer",
                            "%.0f kVA".format(
                                result.requiredTransformerKva
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
