package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldCalculationResult
import com.electrical.calculationspro.data.SldCableSizingEngine
import com.electrical.calculationspro.data.SldCableSizingStudy
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringEngine
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldShortCircuitEngine
import com.electrical.calculationspro.data.SldShortCircuitResult
import com.electrical.calculationspro.data.SldShortCircuitStudy
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary
import java.util.UUID
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

    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    var showNodeDialog by remember {
        mutableStateOf(false)
    }

    var showConnectionDialog by remember {
        mutableStateOf(false)
    }

    var showUpstreamResults by remember {
        mutableStateOf(false)
    }

    var showShortCircuitResults by remember {
        mutableStateOf(false)
    }

    var showCableSizingResults by remember {
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

    var nodeName by remember { mutableStateOf("") }
    var nodeVoltage by remember { mutableStateOf("400") }
    var nodeKw by remember { mutableStateOf("10") }
    var nodePf by remember { mutableStateOf("0.90") }
    var nodeDemand by remember { mutableStateOf("1.0") }
    var nodeKva by remember { mutableStateOf("1000") }
    var nodeTransformerZ by remember { mutableStateOf("6.0") }
    var nodeGeneratorXd by remember { mutableStateOf("15.0") }
    var nodeSourceMva by remember { mutableStateOf("500") }

    var connectionLength by remember { mutableStateOf("10") }
    var connectionResistance by remember { mutableStateOf("0.08") }
    var connectionReactance by remember { mutableStateOf("0.08") }
    var connectionCableSize by remember { mutableStateOf("0") }
    var connectionParallelRuns by remember { mutableStateOf("1") }
    var connectionCurrentCapacity by remember { mutableStateOf("0") }

    var calculationResult by remember {
        mutableStateOf<SldCalculationResult?>(null)
    }

    var shortCircuitStudy by remember {
        mutableStateOf<SldShortCircuitStudy?>(null)
    }

    var cableSizingStudy by remember {
        mutableStateOf<SldCableSizingStudy?>(null)
    }

    fun defaultName(type: SldNodeType): String =
        when (type) {
            SldNodeType.SOURCE ->
                if (arabic) "مصدر" else "Source"

            SldNodeType.TRANSFORMER ->
                if (arabic) "محول" else "Transformer"

            SldNodeType.GENERATOR ->
                if (arabic) "مولد" else "Generator"

            SldNodeType.BUS -> "Bus"

            SldNodeType.PANEL ->
                if (arabic) "لوحة" else "Panel"

            SldNodeType.BREAKER ->
                if (arabic) "قاطع" else "Breaker"

            SldNodeType.LOAD ->
                if (arabic) "حمل" else "Load"
        }

    fun invalidateStudies() {
        calculationResult = null
        shortCircuitStudy = null
        cableSizingStudy = null
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
            nodeVoltage.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 400.0

        val kw =
            nodeKw.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val pf =
            nodePf.toDoubleOrNull()?.coerceIn(0.01, 1.0) ?: 0.90

        val demand =
            nodeDemand.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0

        val kva =
            nodeKva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val transformerZ =
            nodeTransformerZ.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val generatorXd =
            nodeGeneratorXd.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val sourceMva =
            nodeSourceMva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        val name = nodeName.ifBlank {
            defaultName(pendingNodeType)
        }

        val updated = SldNode(
            id = editingNodeId ?: UUID.randomUUID().toString(),
            name = name,
            type = pendingNodeType,
            x = nodes.firstOrNull {
                it.id == editingNodeId
            }?.x ?: (280f + (nodes.size % 4) * 190f),
            y = nodes.firstOrNull {
                it.id == editingNodeId
            }?.y ?: (120f + (nodes.size / 4) * 140f),
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

        nodes =
            if (editingNodeId == null) {
                nodes + updated
            } else {
                nodes.map {
                    if (it.id == editingNodeId) {
                        updated
                    } else {
                        it
                    }
                }
            }

        invalidateStudies()
        showNodeDialog = false
    }

    fun saveConnection() {
        val updated = connections.map {
            if (it.id == editingConnectionId) {
                it.copy(
                    lengthMeters =
                        connectionLength
                            .toDoubleOrNull()
                            ?.coerceAtLeast(0.0)
                            ?: 0.0,
                    resistanceOhmPerKm =
                        connectionResistance
                            .toDoubleOrNull()
                            ?.coerceAtLeast(0.0)
                            ?: 0.0,
                    reactanceOhmPerKm =
                        connectionReactance
                            .toDoubleOrNull()
                            ?.coerceAtLeast(0.0)
                            ?: 0.0,
                    cableSizeMm2 =
                        connectionCableSize
                            .toDoubleOrNull()
                            ?.coerceAtLeast(0.0)
                            ?: 0.0,
                    parallelRuns =
                        connectionParallelRuns
                            .toIntOrNull()
                            ?.coerceAtLeast(1)
                            ?: 1,
                    currentCapacityA =
                        connectionCurrentCapacity
                            .toDoubleOrNull()
                            ?.coerceAtLeast(0.0)
                            ?: 0.0
                )
            } else {
                it
            }
        }

        connections = updated
        invalidateStudies()
        showConnectionDialog = false
    }

    fun deleteSelected() {
        selectedNodeId?.let { nodeId ->
            nodes = nodes.filterNot {
                it.id == nodeId
            }

            connections = connections.filter {
                it.fromNodeId != nodeId &&
                    it.toNodeId != nodeId
            }

            selectedNodeId = null
            connectionStartId = null
            invalidateStudies()
            return
        }

        selectedConnectionId?.let { connectionId ->
            connections = connections.filterNot {
                it.id == connectionId
            }

            selectedConnectionId = null
            invalidateStudies()
        }
    }

    fun connect(
        first: String,
        second: String
    ) {
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
            connections =
                connections + SldConnection(
                    id = UUID.randomUUID().toString(),
                    fromNodeId = first,
                    toNodeId = second
                )
        }

        connectionStartId = null
        invalidateStudies()
    }

    fun calculateUpstream() {
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

        showUpstreamResults = true
    }

    fun calculateShortCircuit() {
        shortCircuitStudy =
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

        showShortCircuitResults = true
    }

    fun calculateCableSizing() {
        val network = SldNetwork(
            nodes = nodes,
            connections = connections
        )

        val study =
            try {
                SldShortCircuitEngine.calculate(network)
            } catch (_: Exception) {
                null
            }

        shortCircuitStudy = study

        val sizing =
            try {
                SldCableSizingEngine.calculate(
                    network = network,
                    shortCircuitStudy = study
                )
            } catch (_: Exception) {
                null
            }

        cableSizingStudy = sizing

        if (sizing != null) {
            connections =
                connections.map { connection ->
                    val result =
                        sizing.results[connection.id]

                    if (
                        result != null &&
                        result.recommendedSizeMm2 > 0.0
                    ) {
                        connection.copy(
                            cableSizeMm2 =
                                result.recommendedSizeMm2,
                            parallelRuns =
                                result.recommendedParallelRuns
                                    .coerceAtLeast(1),
                            currentCapacityA =
                                result.recommendedCurrentCapacityA
                        )
                    } else {
                        connection
                    }
                }
        }

        showCableSizingResults = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    openAdd(SldNodeType.LOAD)
                }
            ) {
                Text(
                    if (arabic) "إضافة حمل" else "Add Load",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.BUS)
                }
            ) {
                Text(
                    if (arabic) "Bus" else "Bus",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.TRANSFORMER)
                }
            ) {
                Text(
                    if (arabic) "محول" else "Transformer",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.GENERATOR)
                }
            ) {
                Text(
                    if (arabic) "مولد" else "Generator",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    selectedNodeId?.let {
                        connectionStartId = it
                    }
                }
            ) {
                Text(
                    if (arabic) "توصيل" else "Connect",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    selectedNodeId?.let { id ->
                        nodes.firstOrNull {
                            it.id == id
                        }?.let(::openEdit)
                    } ?: selectedConnectionId?.let { id ->
                        connections.firstOrNull {
                            it.id == id
                        }?.let(::openConnectionEdit)
                    }
                }
            ) {
                Text(
                    if (arabic) "تعديل" else "Edit",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = ::deleteSelected
            ) {
                Text(
                    if (arabic) "حذف" else "Delete",
                    fontSize = 11.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = ::calculateUpstream
            ) {
                Text(
                    if (arabic) "حساب Upstream" else "Calculate Upstream",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = ::calculateShortCircuit
            ) {
                Text(
                    if (arabic) "حساب القصر" else "Short Circuit",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = ::calculateCableSizing
            ) {
                Text(
                    if (arabic) "اختيار الكابلات" else "Cable Sizing",
                    fontSize = 11.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(6.dp)
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
                        1.dp,
                        Color(0xFF30465A),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(nodes, connections) {
                            detectTapGestures(
                                onTap = { position ->
                                    var foundNode: SldNode? = null

                                    for (node in nodes.asReversed()) {
                                        if (
                                            position.x >= node.x &&
                                            position.x <=
                                            node.x + NODE_WIDTH &&
                                            position.y >= node.y &&
                                            position.y <=
                                            node.y + NODE_HEIGHT
                                        ) {
                                            foundNode = node
                                            break
                                        }
                                    }

                                    if (foundNode != null) {
                                        if (connectionStartId != null) {
                                            connect(
                                                connectionStartId!!,
                                                foundNode.id
                                            )
                                        } else {
                                            selectedNodeId =
                                                foundNode.id
                                            selectedConnectionId = null
                                        }

                                        return@detectTapGestures
                                    }

                                    selectedNodeId = null
                                    selectedConnectionId = null
                                }
                            )
                        }
                        .pointerInput(nodes) {
                            detectDragGestures(
                                onDragStart = { position ->
                                    draggedNodeId =
                                        nodes
                                            .asReversed()
                                            .firstOrNull { node ->
                                                position.x >= node.x &&
                                                    position.x <=
                                                    node.x + NODE_WIDTH &&
                                                    position.y >= node.y &&
                                                    position.y <=
                                                    node.y + NODE_HEIGHT
                                            }
                                            ?.id
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    val id =
                                        draggedNodeId
                                            ?: return@detectDragGestures

                                    nodes =
                                        nodes.map { node ->
                                            if (node.id == id) {
                                                node.copy(
                                                    x = (
                                                        node.x +
                                                            dragAmount.x
                                                        ).coerceAtLeast(0f),
                                                    y = (
                                                        node.y +
                                                            dragAmount.y
                                                        ).coerceAtLeast(0f)
                                                )
                                            } else {
                                                node
                                            }
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
                    val maxX = size.width
                    val maxY = size.height

                    var x = 0f
                    while (x < maxX) {
                        drawLine(
                            Color(0xFF1B2A35),
                            Offset(x, 0f),
                            Offset(x, maxY),
                            1f
                        )
                        x += 40f
                    }

                    var y = 0f
                    while (y < maxY) {
                        drawLine(
                            Color(0xFF1B2A35),
                            Offset(0f, y),
                            Offset(maxX, y),
                            1f
                        )
                        y += 40f
                    }

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
                                    from.x + NODE_WIDTH,
                                    from.y + NODE_HEIGHT / 2f
                                )

                            val end =
                                Offset(
                                    to.x,
                                    to.y + NODE_HEIGHT / 2f
                                )

                            drawLine(
                                color =
                                    if (
                                        selectedConnectionId ==
                                        connection.id
                                    ) {
                                        PrimaryTeal
                                    } else {
                                        Color(0xFF8CA4B5)
                                    },
                                start = start,
                                end = end,
                                strokeWidth = 4f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    nodes.forEach { node ->
                        val shortCircuit =
                            shortCircuitStudy
                                ?.results
                                ?.get(node.id)

                        val borderColor =
                            when {
                                shortCircuit == null ->
                                    Color(0xFF607D8B)

                                shortCircuit
                                    .initialSymmetricalCurrentKa <=
                                    10.0 ->
                                    Color(0xFF4CAF50)

                                shortCircuit
                                    .initialSymmetricalCurrentKa <=
                                    25.0 ->
                                    Color(0xFFFFC107)

                                else ->
                                    Color(0xFFF44336)
                            }

                        val selected =
                            selectedNodeId == node.id

                        drawRoundRect(
                            color = Color(0xFF172630),
                            topLeft = Offset(
                                node.x,
                                node.y
                            ),
                            size = Size(
                                NODE_WIDTH,
                                NODE_HEIGHT
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
                                if (selected) {
                                    PrimaryTeal
                                } else {
                                    borderColor
                                },
                            topLeft = Offset(
                                node.x,
                                node.y
                            ),
                            size = Size(
                                NODE_WIDTH,
                                NODE_HEIGHT
                            ),
                            cornerRadius =
                                androidx.compose.ui.geometry
                                    .CornerRadius(
                                        10f,
                                        10f
                                    ),
                            style = Stroke(
                                width =
                                    if (selected) {
                                        4f
                                    } else {
                                        2f
                                    }
                            )
                        )
                    }
                }

                nodes.forEach { node ->
                    NodeLabel(
                        node = node,
                        shortCircuit =
                            shortCircuitStudy
                                ?.results
                                ?.get(node.id),
                        modifier = Modifier
                            .padding(
                                start = node.x.dp,
                                top = node.y.dp
                            )
                    )
                }

                cableSizingStudy?.let { study ->
                    connections.forEach { connection ->
                        val result =
                            study.results[connection.id]

                        val from =
                            nodes.firstOrNull {
                                it.id == connection.fromNodeId
                            }

                        val to =
                            nodes.firstOrNull {
                                it.id == connection.toNodeId
                            }

                        if (
                            result != null &&
                            from != null &&
                            to != null &&
                            result.recommendedSizeMm2 > 0.0
                        ) {
                            CableLabel(
                                result = result,
                                arabic = arabic,
                                modifier = Modifier
                                    .padding(
                                        start =
                                            (
                                                (
                                                    from.x +
                                                        NODE_WIDTH +
                                                        to.x
                                                    ) / 2f
                                                ).dp - 45.dp,
                                        top =
                                            (
                                                (
                                                    from.y +
                                                        to.y +
                                                        NODE_HEIGHT
                                                    ) / 2f
                                                ).dp - 12.dp
                                    )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .padding(
                        top = 8.dp,
                        end = 8.dp,
                        bottom = 8.dp
                    )
            ) {
                Text(
                    text =
                        if (arabic) {
                            "عناصر المخطط"
                        } else {
                            "SLD Elements"
                        },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    items(
                        nodes,
                        key = { it.id }
                    ) { node ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (
                                        selectedNodeId ==
                                        node.id
                                    ) {
                                        PrimaryTeal
                                    } else {
                                        Color.Transparent
                                    },
                                    RoundedCornerShape(8.dp)
                                ),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color(0xFF172630)
                                ),
                            onClick = {
                                selectedNodeId = node.id
                                selectedConnectionId = null
                            }
                        ) {
                            Column(
                                modifier =
                                    Modifier.padding(8.dp)
                            ) {
                                Text(
                                    node.name,
                                    color = TextPrimary,
                                    fontWeight =
                                        FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                Text(
                                    node.type.name,
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )

                                shortCircuitStudy
                                    ?.results
                                    ?.get(node.id)
                                    ?.let {
                                        Text(
                                            text =
                                                "Ik'' %.2f kA"
                                                    .format(
                                                        it.initialSymmetricalCurrentKa
                                                    ),
                                            color =
                                                shortCircuitColor(
                                                    it
                                                ),
                                            fontSize = 10.sp,
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }
                            }
                        }
                    }

                    if (
                        cableSizingStudy != null &&
                        cableSizingStudy!!
                            .results
                            .isNotEmpty()
                    ) {
                        item {
                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    if (arabic) {
                                        "الكابلات المقترحة"
                                    } else {
                                        "Recommended Cables"
                                    },
                                color = TextPrimary,
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        items(
                            cableSizingStudy!!
                                .results
                                .values
                                .toList(),
                            key = {
                                it.connectionId
                            }
                        ) { result ->
                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            Color(0xFF172630)
                                    )
                            ) {
                                Column(
                                    modifier =
                                        Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text =
                                            "${result.fromNodeId} → ${result.toNodeId}",
                                        color =
                                            TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    if (
                                        result.recommendedSizeMm2 >
                                        0.0
                                    ) {
                                        Text(
                                            text =
                                                "${formatValue(result.recommendedSizeMm2)} mm² " +
                                                    "${result.recommendedMaterial} × " +
                                                    "${result.recommendedParallelRuns}",
                                            color =
                                                PrimaryTeal,
                                            fontSize = 10.sp,
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            text =
                                                "${formatValue(result.designCurrentA)} A | " +
                                                    "ΔV ${formatValue(result.recommendedVoltageDropPercent)}%",
                                            color =
                                                TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    } else {
                                        Text(
                                            text =
                                                if (arabic) {
                                                    "لا يوجد اختيار مناسب"
                                                } else {
                                                    "No acceptable cable"
                                                },
                                            color =
                                                Color(0xFFF44336),
                                            fontSize = 9.sp,
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNodeDialog) {
        NodeEditorDialog(
            arabic = arabic,
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
        ConnectionEditorDialog(
            arabic = arabic,
            length = connectionLength,
            resistance = connectionResistance,
            reactance = connectionReactance,
            cableSize = connectionCableSize,
            parallelRuns = connectionParallelRuns,
            capacity = connectionCurrentCapacity,
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
            onCapacityChange = {
                connectionCurrentCapacity = it
            },
            onSave = ::saveConnection,
            onDismiss = {
                showConnectionDialog = false
            }
        )
    }

    if (showUpstreamResults) {
        calculationResult?.let {
            UpstreamResultsDialog(
                arabic = arabic,
                result = it,
                onDismiss = {
                    showUpstreamResults = false
                }
            )
        }
    }

    if (showShortCircuitResults) {
        shortCircuitStudy?.let { study ->
            SldShortCircuitResultsDialog(
                language = language,
                study = study,
                onDismiss = {
                    showShortCircuitResults = false
                }
            )
        }
    }

    if (showCableSizingResults) {
        cableSizingStudy?.let { study ->
            SldCableSizingResultsDialog(
                language = language,
                study = study,
                onDismiss = {
                    showCableSizingResults = false
                }
            )
        }
    }
}

@Composable
private fun NodeLabel(
    node: SldNode,
    shortCircuit: SldShortCircuitResult?,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .width(NODE_WIDTH.dp)
            .height(NODE_HEIGHT.dp)
            .padding(8.dp),
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = node.name,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        Text(
            text = node.type.name,
            color = TextSecondary,
            fontSize = 9.sp
        )

        shortCircuit?.let {
            Text(
                text =
                    "Ik'' %.2f kA".format(
                        it.initialSymmetricalCurrentKa
                    ),
                color =
                    shortCircuitColor(it),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CableLabel(
    result: com.electrical.calculationspro.data.SldCableSizingResult,
    arabic: Boolean,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .width(100.dp)
            .height(28.dp)
            .background(
                Color(0xFF102027),
                RoundedCornerShape(5.dp)
            )
            .border(
                1.dp,
                PrimaryTeal,
                RoundedCornerShape(5.dp)
            )
            .padding(
                horizontal = 4.dp,
                vertical = 2.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text =
                if (arabic) {
                    "${formatValue(result.recommendedSizeMm2)} mm² × " +
                        "${result.recommendedParallelRuns}"
                } else {
                    "${formatValue(result.recommendedSizeMm2)} mm² × " +
                        "${result.recommendedParallelRuns}"
                },
            color = PrimaryTeal,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun shortCircuitColor(
    result: SldShortCircuitResult
): Color =
    when {
        result.initialSymmetricalCurrentKa <= 10.0 ->
            Color(0xFF4CAF50)

        result.initialSymmetricalCurrentKa <= 25.0 ->
            Color(0xFFFFC107)

        else ->
            Color(0xFFF44336)
    }

private fun formatValue(
    value: Double
): String =
    when {
        value >= 1000.0 ->
            "%.0f".format(value)

        value >= 100.0 ->
            "%.1f".format(value)

        value >= 10.0 ->
            "%.1f".format(value)

        else ->
            "%.2f".format(value)
    }

@Composable
private fun NodeEditorDialog(
    arabic: Boolean,
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
                if (arabic) {
                    "بيانات العنصر"
                } else {
                    "Element Data"
                }
            )
        },
        text = {
            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        type.name,
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                                Text("%Z")
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
                                Text("Xd'' %")
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
                                Text(
                                    "Source Short-Circuit MVA"
                                )
                            },
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
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
    parallelRuns: String,
    capacity: String,
    onLengthChange: (String) -> Unit,
    onResistanceChange: (String) -> Unit,
    onReactanceChange: (String) -> Unit,
    onCableSizeChange: (String) -> Unit,
    onParallelRunsChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
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
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = length,
                        onValueChange = onLengthChange,
                        label = {
                            Text("Length m")
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
                            Text("Cable mm²")
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = parallelRuns,
                        onValueChange = onParallelRunsChange,
                        label = {
                            Text("Parallel Runs")
                        },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = onCapacityChange,
                        label = {
                            Text("Current Capacity A")
                        },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
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
private fun UpstreamResultsDialog(
    arabic: Boolean,
    result: SldCalculationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (arabic) {
                    "نتائج Upstream"
                } else {
                    "Upstream Results"
                }
            )
        },
        text = {
            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                item {
                    ResultLine(
                        "Connected Load",
                        "%.2f kW".format(
                            result.totalConnectedLoadKw
                        )
                    )
                }

                item {
                    ResultLine(
                        "Maximum Demand",
                        "%.2f kW".format(
                            result.totalDemandLoadKw
                        )
                    )
                }

                item {
                    ResultLine(
                        "Required kVA",
                        "%.2f kVA".format(
                            result.totalRequiredKva
                        )
                    )
                }

                item {
                    ResultLine(
                        "Main Current",
                        "%.2f A".format(
                            result.mainCurrentA
                        )
                    )
                }

                item {
                    ResultLine(
                        "Main Breaker",
                        "%.0f A".format(
                            result.mainBreakerA
                        )
                    )
                }

                item {
                    ResultLine(
                        "Transformer",
                        "%.0f kVA".format(
                            result.requiredTransformerKva
                        )
                    )
                }

                item {
                    ResultLine(
                        "Voltage Drop",
                        "%.2f %%".format(
                            result.totalVoltageDropPercent
                        )
                    )
                }

                items(result.notes) {
                    Text(
                        it,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = TextSecondary,
            fontSize = 11.sp
        )

        Text(
            value,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
