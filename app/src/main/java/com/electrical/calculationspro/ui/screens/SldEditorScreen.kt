import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.max

@Composable
fun SldEditorScreen(
    language: AppLanguage
) {

    val arabic =
        language == AppLanguage.ARABIC

    var nodes by remember {
        mutableStateOf(
            listOf(
                SldNode(
                    id = "source-1",
                    name =
                        if (arabic) {
                            "المصدر"
                        } else {
                            "SOURCE"
                        },
                    type =
                        SldNodeType.SOURCE,
                    x = 180f,
                    y = 180f,
                    voltage = 400.0
                )
            )
        )
    }

    var connections by remember {
        mutableStateOf(
            emptyList<SldConnection>()
        )
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

    var pendingNodeType by remember {
        mutableStateOf(
            SldNodeType.LOAD
        )
    }

    var calculationResult by remember {
        mutableStateOf<SldCalculationResult?>(null)
    }

    var showResults by remember {
        mutableStateOf(false)
    }

    var nodeName by remember {
        mutableStateOf("")
    }

    var nodeKw by remember {
        mutableStateOf("10")
    }

    var nodeVoltage by remember {
        mutableStateOf("400")
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

    fun addNode(
        type: SldNodeType
    ) {

        pendingNodeType = type

        nodeName =
            when (type) {

                SldNodeType.SOURCE ->
                    if (arabic) {
                        "مصدر"
                    } else {
                        "Source"
                    }

                SldNodeType.TRANSFORMER ->
                    if (arabic) {
                        "محول"
                    } else {
                        "Transformer"
                    }

                SldNodeType.GENERATOR ->
                    if (arabic) {
                        "مولد"
                    } else {
                        "Generator"
                    }

                SldNodeType.BUS ->
                    if (arabic) {
                        "Bus"
                    } else {
                        "Bus"
                    }

                SldNodeType.PANEL ->
                    if (arabic) {
                        "لوحة"
                    } else {
                        "Panel"
                    }

                SldNodeType.BREAKER ->
                    if (arabic) {
                        "قاطع"
                    } else {
                        "Breaker"
                    }

                SldNodeType.LOAD ->
                    if (arabic) {
                        "حمل"
                    } else {
                        "Load"
                    }
            }

        nodeKw = "10"
        nodeVoltage = "400"
        nodePf = "0.90"
        nodeDemand = "1.0"
        nodeKva = "1000"

        showNodeDialog = true
    }

    fun confirmNode() {

        val newNode =
            SldNode(
                id =
                    UUID.randomUUID()
                        .toString(),
                name =
                    nodeName.ifBlank {
                        pendingNodeType.name
                    },
                type =
                    pendingNodeType,
                x =
                    220f +
                        (
                            nodes.size % 4
                        ) * 150f,
                y =
                    150f +
                        (
                            nodes.size / 4
                        ) * 130f,
                voltage =
                    nodeVoltage
                        .toDoubleOrNull()
                        ?.coerceAtLeast(1.0)
                        ?: 400.0,
                loadKw =
                    if (
                        pendingNodeType ==
                        SldNodeType.LOAD
                    ) {
                        nodeKw
                            .toDoubleOrNull()
                            ?.coerceAtLeast(
                                0.0
                            )
                            ?: 0.0
                    } else {
                        0.0
                    },
                powerFactor =
                    nodePf
                        .toDoubleOrNull()
                        ?.coerceIn(
                            0.01,
                            1.0
                        )
                        ?: 0.90,
                demandFactor =
                    nodeDemand
                        .toDoubleOrNull()
                        ?.coerceIn(
                            0.0,
                            1.0
                        )
                        ?: 1.0,
                ratedKva =
                    nodeKva
                        .toDoubleOrNull()
                        ?.coerceAtLeast(
                            0.0
                        )
                        ?: 0.0
            )

        nodes =
            nodes + newNode

        showNodeDialog = false
    }

    fun deleteSelectedNode() {

        val selected =
            selectedNodeId
                ?: return

        nodes =
            nodes.filter {
                it.id != selected
            }

        connections =
            connections.filter {
                it.fromNodeId != selected &&
                    it.toNodeId != selected
            }

        selectedNodeId = null
        connectionStartId = null
    }

    fun connectNodes(
        from: String,
        to: String
    ) {

        if (from == to) {
            return
        }

        val exists =
            connections.any {
                (
                    it.fromNodeId == from &&
                        it.toNodeId == to
                    ) ||
                    (
                        it.fromNodeId == to &&
                            it.toNodeId == from
                        )
            }

        if (!exists) {

            connections =
                connections +
                    SldConnection(
                        id =
                            UUID.randomUUID()
                                .toString(),
                        fromNodeId = from,
                        toNodeId = to
                    )
        }

        connectionStartId = null
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    DarkBackground
                )
    ) {

        SldToolbar(
            arabic = arabic,
            onAddSource = {
                addNode(
                    SldNodeType.SOURCE
                )
            },
            onAddTransformer = {
                addNode(
                    SldNodeType.TRANSFORMER
                )
            },
            onAddGenerator = {
                addNode(
                    SldNodeType.GENERATOR
                )
            },
            onAddBus = {
                addNode(
                    SldNodeType.BUS
                )
            },
            onAddPanel = {
                addNode(
                    SldNodeType.PANEL
                )
            },
            onAddBreaker = {
                addNode(
                    SldNodeType.BREAKER
                )
            },
            onAddLoad = {
                addNode(
                    SldNodeType.LOAD
                )
            },
            onConnect = {

                if (
                    selectedNodeId != null
                ) {
                    connectionStartId =
                        selectedNodeId
                }
            },
            onCalculate = {

                try {

                    calculationResult =
                        SldEngineeringEngine
                            .calculateUpstream(
                                SldNetwork(
                                    nodes = nodes,
                                    connections =
                                        connections
                                )
                            )

                    showResults = true

                } catch (
                    exception: Exception
                ) {

                    calculationResult =
                        SldCalculationResult(
                            nodeResults =
                                emptyMap(),
                            totalConnectedLoadKw =
                                0.0,
                            totalDemandLoadKw =
                                0.0,
                            totalRequiredKva =
                                0.0,
                            mainCurrentA =
                                0.0,
                            mainBreakerA =
                                0.0,
                            requiredTransformerKva =
                                0.0,
                            notes =
                                listOf(
                                    exception.message
                                        ?: "Calculation error"
                                )
                        )

                    showResults = true
                }
            },
            onDelete = {
                deleteSelectedNode()
            }
        )

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
        ) {

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                        .border(
                            width = 1.dp,
                            color =
                                Color(
                                    0xFF30465A
                                ),
                            shape =
                                RoundedCornerShape(
                                    10.dp
                                )
                        )
            ) {

                SldCanvas(
                    nodes = nodes,
                    connections =
                        connections,
                    selectedNodeId =
                        selectedNodeId,
                    connectionStartId =
                        connectionStartId,
                    onSelectNode = { nodeId ->

                        if (
                            connectionStartId !=
                            null
                        ) {

                            val start =
                                connectionStartId

                            if (
                                start !=
                                nodeId
                            ) {
                                connectNodes(
                                    start,
                                    nodeId
                                )
                            }

                        } else {

                            selectedNodeId =
                                nodeId
                        }
                    },
                    onMoveNode = {
                        nodeId,
                        dx,
                        dy ->

                        nodes =
                            nodes.map { node ->

                                if (
                                    node.id ==
                                    nodeId
                                ) {

                                    node.copy(
                                        x =
                                            max(
                                                50f,
                                                node.x +
                                                    dx
                                            ),
                                        y =
                                            max(
                                                50f,
                                                node.y +
                                                    dy
                                            )
                                    )

                                } else {
                                    node
                                }
                            }
                    }
                )
            }

            SldSidePanel(
                arabic = arabic,
                nodes = nodes,
                selectedNodeId =
                    selectedNodeId,
                onSelect = {
                    selectedNodeId = it
                },
                result =
                    calculationResult,
                onShowResults = {
                    showResults = true
                }
            )
        }
    }

    if (showNodeDialog) {

        AlertDialog(
            onDismissRequest = {
                showNodeDialog = false
            },
            title = {
                Text(
                    when {
                        pendingNodeType ==
                            SldNodeType.LOAD &&
                            arabic ->
                            "إضافة حمل"

                        pendingNodeType ==
                            SldNodeType.LOAD ->
                            "Add Load"

                        arabic ->
                            "إضافة عنصر"

                        else ->
                            "Add Element"
                    }
                )
            },
            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    OutlinedTextField(
                        value = nodeName,
                        onValueChange = {
                            nodeName = it
                        },
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

                    OutlinedTextField(
                        value = nodeVoltage,
                        onValueChange = {
                            nodeVoltage = it
                        },
                        label = {
                            Text(
                                "Voltage V"
                            )
                        },
                        singleLine = true
                    )

                    if (
                        pendingNodeType ==
                        SldNodeType.LOAD
                    ) {

                        OutlinedTextField(
                            value = nodeKw,
                            onValueChange = {
                                nodeKw = it
                            },
                            label = {
                                Text(
                                    "Load kW"
                                )
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = nodePf,
                            onValueChange = {
                                nodePf = it
                            },
                            label = {
                                Text(
                                    "Power Factor"
                                )
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = nodeDemand,
                            onValueChange = {
                                nodeDemand = it
                            },
                            label = {
                                Text(
                                    if (arabic) {
                                        "Demand Factor"
                                    } else {
                                        "Demand Factor"
                                    }
                                )
                            },
                            singleLine = true
                        )
                    }

                    if (
                        pendingNodeType ==
                            SldNodeType.TRANSFORMER ||
                        pendingNodeType ==
                            SldNodeType.GENERATOR
                    ) {

                        OutlinedTextField(
                            value = nodeKva,
                            onValueChange = {
                                nodeKva = it
                            },
                            label = {
                                Text(
                                    "Rated kVA"
                                )
                            },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        confirmNode()
                    }
                ) {
                    Text(
                        if (arabic) {
                            "إضافة"
                        } else {
                            "Add"
                        }
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showNodeDialog = false
                    }
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

    if (showResults) {

        UpstreamResultsDialog(
            arabic = arabic,
            result =
                calculationResult,
            onClose = {
                showResults = false
            }
        )
    }
}

@Composable
private fun SldToolbar(
    arabic: Boolean,
    onAddSource: () -> Unit,
    onAddTransformer: () -> Unit,
    onAddGenerator: () -> Unit,
    onAddBus: () -> Unit,
    onAddPanel: () -> Unit,
    onAddBreaker: () -> Unit,
    onAddLoad: () -> Unit,
    onConnect: () -> Unit,
    onCalculate: () -> Unit,
    onDelete: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF182536)
                )
                .padding(8.dp),
        horizontalArrangement =
            Arrangement.spacedBy(6.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        ToolButton(
            "SOURCE",
            onAddSource
        )

        ToolButton(
            "TR",
            onAddTransformer
        )

        ToolButton(
            "GEN",
            onAddGenerator
        )

        ToolButton(
            "BUS",
            onAddBus
        )

        ToolButton(
            "DB",
            onAddPanel
        )

        ToolButton(
            "CB",
            onAddBreaker
        )

        ToolButton(
            "LOAD",
            onAddLoad
        )

        ToolButton(
            if (arabic) {
                "ربط"
            } else {
                "CONNECT"
            },
            onConnect
        )

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

        TextButton(
            onClick = onDelete
        ) {
            Text(
                if (arabic) {
                    "حذف"
                } else {
                    "Delete"
                },
                color = Color(0xFFFF8A80)
            )
        }
    }
}

@Composable
private fun ToolButton(
    text: String,
    onClick: () -> Unit
) {

    TextButton(
        onClick = onClick
    ) {

        Text(
            text = text,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
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
    onMoveNode: (
        String,
        Float,
        Float
    ) -> Unit
) {

    val nodeWidth = 100f
    val nodeHeight = 64f

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF0B1520)
                )
                .pointerInput(
                    nodes
                ) {

                    detectTapGestures { offset ->

                        val node =
                            findNodeAt(
                                nodes,
                                offset,
                                nodeWidth,
                                nodeHeight
                            )

                        if (
                            node != null
                        ) {
                            onSelectNode(
                                node.id
                            )
                        }
                    }
                }
                .pointerInput(
                    nodes
                ) {

                    detectDragGestures(
                        onDrag = { change, dragAmount ->

                            val node =
                                findNodeAt(
                                    nodes,
                                    change.position,
                                    nodeWidth,
                                    nodeHeight
                                )

                            if (
                                node != null
                            ) {

                                onMoveNode(
                                    node.id,
                                    dragAmount.x,
                                    dragAmount.y
                                )
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
                    it.id ==
                        connection.fromNodeId
                }

            val to =
                nodes.firstOrNull {
                    it.id ==
                        connection.toNodeId
                }

            if (
                from != null &&
                to != null
            ) {

                val start =
                    Offset(
                        from.x +
                            nodeWidth / 2f,
                        from.y +
                            nodeHeight / 2f
                    )

                val end =
                    Offset(
                        to.x +
                            nodeWidth / 2f,
                        to.y +
                            nodeHeight / 2f
                    )

                drawLine(
                    color =
                        PrimaryTeal,
                    start = start,
                    end = end,
                    strokeWidth = 5f
                )
            }
        }

        nodes.forEach { node ->

            val selected =
                node.id ==
                    selectedNodeId

            val connecting =
                node.id ==
                    connectionStartId

            val fill =
                when {

                    connecting ->
                        Color(
                            0xFF9A6A16
                        )

                    selected ->
                        Color(
                            0xFF164E55
                        )

                    else ->
                        Color(
                            0xFF1C2A3A
                        )
                }

            drawRoundRect(
                color = fill,
                topLeft =
                    Offset(
                        node.x,
                        node.y
                    ),
                size =
                    androidx.compose.ui.geometry
                        .Size(
                            nodeWidth,
                            nodeHeight
                        ),
                cornerRadius =
                    androidx.compose.ui.geometry
                        .CornerRadius(
                            10f,
                            10f
                        )
            )

            drawRoundRect(
                color =
                    PrimaryTeal,
                topLeft =
                    Offset(
                        node.x,
                        node.y
                    ),
                size =
                    androidx.compose.ui.geometry
                        .Size(
                            nodeWidth,
                            nodeHeight
                        ),
                cornerRadius =
                    androidx.compose.ui.geometry
                        .CornerRadius(
                            10f,
                            10f
                        ),
                style =
                    androidx.compose.ui.graphics
                        .drawscope
                        .Stroke(
                            width = 2f
                        )
            )
        }
    }

    nodes.forEach { node ->

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
        ) {

            Text(
                text =
                    "${node.type.name}\n${node.name}",
                color =
                    TextPrimary,
                fontSize = 10.sp,
                textAlign =
                    TextAlign.Center,
                modifier =
                    Modifier
                        .offset(
                            x =
                                node.x.dp,
                            y =
                                node.y.dp
                        )
                        .width(
                            nodeWidth.dp
                        )
                        .height(
                            nodeHeight.dp
                        )
                        .padding(
                            top = 12.dp
                        )
            )
        }
    }
}

private fun findNodeAt(
    nodes: List<SldNode>,
    offset: Offset,
    width: Float,
    height: Float
): SldNode? {

    return nodes.lastOrNull { node ->

        offset.x >= node.x &&
            offset.x <=
            node.x + width &&
            offset.y >= node.y &&
            offset.y <=
            node.y + height
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawGrid() {

    val spacing = 40f

    var x = 0f

    while (
        x < size.width
    ) {

        drawLine(
            color =
                Color(
                    0xFF182536
                ),
            start =
                Offset(
                    x,
                    0f
                ),
            end =
                Offset(
                    x,
                    size.height
                ),
            strokeWidth = 1f
        )

        x += spacing
    }

    var y = 0f

    while (
        y < size.height
    ) {

        drawLine(
            color =
                Color(
                    0xFF182536
                ),
            start =
                Offset(
                    0f,
                    y
                ),
            end =
                Offset(
                    size.width,
                    y
                ),
            strokeWidth = 1f
        )

        y += spacing
    }
}

@Composable
private fun SldSidePanel(
    arabic: Boolean,
    nodes: List<SldNode>,
    selectedNodeId: String?,
    onSelect: (String) -> Unit,
    result: SldCalculationResult?,
    onShowResults: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .width(270.dp)
                .fillMaxHeight()
                .background(
                    Color(0xFF111C29)
                )
                .padding(12.dp)
    ) {

        Text(
            text =
                if (arabic) {
                    "مخطط النظام"
                } else {
                    "System Model"
                },
            color =
                PrimaryTeal,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        LazyColumn(
            modifier =
                Modifier.weight(1f)
        ) {

            items(nodes) { node ->

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = 6.dp
                            )
                            .clickable {
                                onSelect(
                                    node.id
                                )
                            },
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    if (
                                        node.id ==
                                        selectedNodeId
                                    ) {
                                        Color(
                                            0xFF164E55
                                        )
                                    } else {
                                        Color(
                                            0xFF1C2A3A
                                        )
                                    }
                            )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .padding(
                                    10.dp
                                )
                    ) {

                        Text(
                            text =
                                node.name,
                            color =
                                TextPrimary,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                node.type.name,
                            color =
                                TextSecondary,
                            fontSize =
                                11.sp
                        )

                        if (
                            node.type ==
                            SldNodeType.LOAD
                        ) {

                            Text(
                                text =
                                    "%.2f kW | PF %.2f"
                                        .format(
                                            node.loadKw,
                                            node.powerFactor
                                        ),
                                color =
                                    TextSecondary,
                                fontSize =
                                    11.sp
                            )
                        }
                    }
                }
            }
        }

        if (
            result != null
        ) {

            Button(
                onClick =
                    onShowResults,
                modifier =
                    Modifier.fillMaxWidth()
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

@Composable
private fun UpstreamResultsDialog(
    arabic: Boolean,
    result: SldCalculationResult?,
    onClose: () -> Unit
) {

    AlertDialog(
        onDismissRequest =
            onClose,
        title = {

            Text(
                if (arabic) {
                    "نتائج الحساب من الأحمال إلى المصدر"
                } else {
                    "Upstream Calculation Results"
                }
            )
        },
        text = {

            LazyColumn(
                modifier =
                    Modifier.height(
                        500.dp
                    )
            ) {

                if (
                    result != null
                ) {

                    item {

                        ResultLine(
                            "Connected Load",
                            "%.2f kW"
                                .format(
                                    result.totalConnectedLoadKw
                                )
                        )

                        ResultLine(
                            "Maximum Demand",
                            "%.2f kW"
                                .format(
                                    result.totalDemandLoadKw
                                )
                        )

                        ResultLine(
                            "Required kVA",
                            "%.2f kVA"
                                .format(
                                    result.totalRequiredKva
                                )
                        )

                        ResultLine(
                            "Main Current",
                            "%.2f A"
                                .format(
                                    result.mainCurrentA
                                )
                        )

                        ResultLine(
                            "Main Breaker",
                            "%.0f A"
                                .format(
                                    result.mainBreakerA
                                )
                        )

                        ResultLine(
                            "Transformer",
                            "%.0f kVA"
                                .format(
                                    result.requiredTransformerKva
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        Text(
                            if (arabic) {
                                "تفاصيل العناصر"
                            } else {
                                "Node Results"
                            },
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                PrimaryTeal
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )
                    }

                    items(
                        result
                            .nodeResults
                            .values
                            .toList()
                    ) { nodeResult ->

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        bottom = 8.dp
                                    ),
                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            Color(
                                                0xFF1C2A3A
                                            )
                                    )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        10.dp
                                    )
                            ) {

                                Text(
                                    text =
                                        nodeResult.nodeName,
                                    color =
                                        PrimaryTeal,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                ResultLine(
                                    "Connected",
                                    "%.2f kW"
                                        .format(
                                            nodeResult
                                                .connectedLoadKw
                                        )
                                )

                                ResultLine(
                                    "Demand",
                                    "%.2f kW"
                                        .format(
                                            nodeResult
                                                .demandLoadKw
                                        )
                                )

                                ResultLine(
                                    "kVA",
                                    "%.2f"
                                        .format(
                                            nodeResult
                                                .apparentPowerKva
                                        )
                                )

                                ResultLine(
                                    "Current",
                                    "%.2f A"
                                        .format(
                                            nodeResult
                                                .currentA
                                        )
                                )

                                ResultLine(
                                    "Breaker",
                                    "%.0f A"
                                        .format(
                                            nodeResult
                                                .requiredBreakerA
                                        )
                                )

                                if (
                                    nodeResult
                                        .diversityFactor >
                                    1.0
                                ) {

                                    ResultLine(
                                        "Diversity",
                                        "%.3f"
                                            .format(
                                                nodeResult
                                                    .diversityFactor
                                            )
                                    )
                                }
                            }
                        }
                    }

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )

                        result.notes.forEach {
                            note ->

                            Text(
                                text =
                                    "• $note",
                                color =
                                    TextSecondary,
                                fontSize =
                                    12.sp,
                                modifier =
                                    Modifier.padding(
                                        bottom = 4.dp
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {

            TextButton(
                onClick =
                    onClose
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

@Composable
private fun ResultLine(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                ),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            color =
                TextSecondary,
            fontSize =
                12.sp
        )

        Text(
            text = value,
            color =
                TextPrimary,
            fontSize =
                13.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}
