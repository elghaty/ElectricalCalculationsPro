package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldCableSizingEngine
import com.electrical.calculationspro.data.SldCableSizingResult
import com.electrical.calculationspro.data.SldCableSizingStudy
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringEngine
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldProtectionCoordinationEngine
import com.electrical.calculationspro.data.SldProtectionCoordinationResult
import com.electrical.calculationspro.data.SldShortCircuitEngine
import com.electrical.calculationspro.data.SldShortCircuitStudy
import com.electrical.calculationspro.data.SldCalculationResult
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary
import java.util.UUID
import kotlin.math.sqrt

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: () -> Unit = {}
) {
    val arabic = language == AppLanguage.ARABIC

    var nodes by remember {
        mutableStateOf(
            listOf(
                SldNode(
                    id = "source-1",
                    name = if (arabic) "المصدر الرئيسي" else "Main Source",
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

    var showProtectionResults by remember {
        mutableStateOf(false)
    }

    var editingNodeId by remember {
        mutableStateOf<String?>(null)
    }

    var editingConnectionId by remember {
        mutableStateOf<String?>(null)
    }

    var pendingNodeType by remember {
        mutableStateOf<SldNodeType?>(null)
    }

    var nodeName by remember {
        mutableStateOf("")
    }

    var nodeVoltage by remember {
        mutableStateOf("400")
    }

    var nodeKw by remember {
        mutableStateOf("0")
    }

    var nodePf by remember {
        mutableStateOf("0.90")
    }

    var nodeDemand by remember {
        mutableStateOf("1.0")
    }

    var nodeKva by remember {
        mutableStateOf("0")
    }

    var nodeTransformerZ by remember {
        mutableStateOf("0")
    }

    var nodeGeneratorXd by remember {
        mutableStateOf("0")
    }

    var nodeSourceMva by remember {
        mutableStateOf("500")
    }

    var connectionLength by remember {
        mutableStateOf("10")
    }

    var connectionResistance by remember {
        mutableStateOf("0.125")
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

    var shortCircuitStudy by remember {
        mutableStateOf<SldShortCircuitStudy?>(null)
    }

    var cableSizingStudy by remember {
        mutableStateOf<SldCableSizingStudy?>(null)
    }

    var protectionResult by remember {
        mutableStateOf<SldProtectionCoordinationResult?>(null)
    }

    fun network(): SldNetwork {
        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }

    fun invalidateStudies() {
        calculationResult = null
        shortCircuitStudy = null
        cableSizingStudy = null
        protectionResult = null
    }

    fun defaultName(type: SldNodeType): String {
        val prefix = when (type) {
            SldNodeType.SOURCE -> if (arabic) "مصدر" else "Source"
            SldNodeType.BUS -> if (arabic) "باص" else "Bus"
            SldNodeType.LOAD -> if (arabic) "حمل" else "Load"
            SldNodeType.TRANSFORMER -> if (arabic) "محول" else "Transformer"
            SldNodeType.GENERATOR -> if (arabic) "مولد" else "Generator"
            SldNodeType.BREAKER -> if (arabic) "قاطع" else "Breaker"
            SldNodeType.PANEL -> if (arabic) "لوحة" else "Panel"
        }

        return "$prefix ${nodes.count { it.type == type } + 1}"
    }

    fun openAdd(type: SldNodeType) {
        editingNodeId = null
        pendingNodeType = type

        nodeName = defaultName(type)
        nodeVoltage = "400"
        nodeKw = if (type == SldNodeType.LOAD) "100" else "0"
        nodePf = "0.90"
        nodeDemand = "1.0"
        nodeKva = "0"
        nodeTransformerZ = if (type == SldNodeType.TRANSFORMER) "6" else "0"
        nodeGeneratorXd = if (type == SldNodeType.GENERATOR) "15" else "0"
        nodeSourceMva = if (type == SldNodeType.SOURCE) "500" else "0"

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
        val type = pendingNodeType ?: return

        val voltage = nodeVoltage.toDoubleOrNull() ?: 400.0
        val kw = nodeKw.toDoubleOrNull() ?: 0.0
        val pf = nodePf.toDoubleOrNull()?.coerceIn(0.01, 1.0) ?: 0.90
        val demand = nodeDemand.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
        val kva = nodeKva.toDoubleOrNull() ?: 0.0
        val transformerZ = nodeTransformerZ.toDoubleOrNull() ?: 0.0
        val generatorXd = nodeGeneratorXd.toDoubleOrNull() ?: 0.0
        val sourceMva = nodeSourceMva.toDoubleOrNull() ?: 0.0

        if (editingNodeId == null) {
            val maxX = nodes.maxOfOrNull { it.x } ?: 80f

            val newNode = SldNode(
                id = UUID.randomUUID().toString(),
                name = nodeName.ifBlank { defaultName(type) },
                type = type,
                x = maxX + 180f,
                y = 180f + (nodes.size % 4) * 100f,
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
        } else {
            nodes = nodes.map { node ->
                if (node.id == editingNodeId) {
                    node.copy(
                        name = nodeName.ifBlank { node.name },
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
        val id = editingConnectionId ?: return

        val length = connectionLength.toDoubleOrNull() ?: 0.0
        val resistance = connectionResistance.toDoubleOrNull() ?: 0.0
        val reactance = connectionReactance.toDoubleOrNull() ?: 0.0
        val cableSize = connectionCableSize.toDoubleOrNull() ?: 0.0
        val parallelRuns =
            connectionParallelRuns.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val currentCapacity =
            connectionCurrentCapacity.toDoubleOrNull() ?: 0.0

        connections = connections.map { connection ->
            if (connection.id == id) {
                connection.copy(
                    lengthMeters = length,
                    resistanceOhmPerKm = resistance,
                    reactanceOhmPerKm = reactance,
                    cableSizeMm2 = cableSize,
                    parallelRuns = parallelRuns,
                    currentCapacityA = currentCapacity
                )
            } else {
                connection
            }
        }

        invalidateStudies()
        showConnectionDialog = false
    }

    fun deleteSelected() {
        selectedNodeId?.let { nodeId ->
            nodes = nodes.filterNot { it.id == nodeId }
            connections = connections.filter {
                it.fromNodeId != nodeId &&
                    it.toNodeId != nodeId
            }
            selectedNodeId = null
        }

        selectedConnectionId?.let { connectionId ->
            connections = connections.filterNot {
                it.id == connectionId
            }
            selectedConnectionId = null
        }

        connectionStartId = null
        invalidateStudies()
    }

    fun connect() {
        val start = connectionStartId
        val end = selectedNodeId

        if (start == null || end == null || start == end) {
            return
        }

        val exists = connections.any {
            (it.fromNodeId == start && it.toNodeId == end) ||
                (it.fromNodeId == end && it.toNodeId == start)
        }

        if (!exists) {
            connections = connections + SldConnection(
                id = UUID.randomUUID().toString(),
                fromNodeId = start,
                toNodeId = end,
                lengthMeters = 10.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.08,
                cableSizeMm2 = 0.0,
                parallelRuns = 1,
                currentCapacityA = 0.0
            )
        }

        connectionStartId = null
        invalidateStudies()
    }

    fun calculateUpstream() {
        try {
            calculationResult =
                SldEngineeringEngine.calculateUpstream(
                    network()
                )

            showUpstreamResults = true
        } catch (_: Exception) {
            calculationResult = null
        }
    }

    fun calculateShortCircuit(showDialog: Boolean = true): SldShortCircuitStudy? {
        return try {
            val study =
                SldShortCircuitEngine.calculate(
                    network()
                )

            shortCircuitStudy = study

            if (showDialog) {
                showShortCircuitResults = true
            }

            study
        } catch (_: Exception) {
            null
        }
    }

    fun calculateCableSizing(showDialog: Boolean = true): SldCableSizingStudy? {
        val currentShortCircuit =
            shortCircuitStudy
                ?: calculateShortCircuit(showDialog = false)
                ?: return null

        return try {
            val study =
                SldCableSizingEngine.calculate(
                    network(),
                    shortCircuitStudy = currentShortCircuit
                )

            cableSizingStudy = study

            connections = connections.map { connection ->
                val result =
                    study.results[connection.id]

                if (
                    result != null &&
                    result.recommendedSizeMm2 > 0.0
                ) {
                    connection.copy(
                        cableSizeMm2 = result.recommendedSizeMm2,
                        parallelRuns = result.recommendedParallelRuns,
                        currentCapacityA =
                            result.recommendedCurrentCapacityA
                    )
                } else {
                    connection
                }
            }

            protectionResult = null

            if (showDialog) {
                showCableSizingResults = true
            }

            study
        } catch (_: Exception) {
            null
        }
    }

    fun calculateProtection() {
        val currentShortCircuit =
            shortCircuitStudy
                ?: calculateShortCircuit(showDialog = false)

        val currentCableSizing =
            cableSizingStudy
                ?: calculateCableSizing(showDialog = false)

        try {
            protectionResult =
                SldProtectionCoordinationEngine.calculate(
                    network = network(),
                    shortCircuitStudy = currentShortCircuit,
                    cableSizingStudy = currentCableSizing
                )

            showProtectionResults = true
        } catch (_: Exception) {
            protectionResult = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = onBack
                ) {
                    Text(
                        text = if (arabic) "رجوع" else "Back",
                        color = TextPrimary
                    )
                }

                Text(
                    text = if (arabic) {
                        "SLD المهندس الكهربائي"
                    } else {
                        "Electrical Engineer SLD"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallActionButton(
                    text = if (arabic) "حمل" else "Load",
                    onClick = {
                        openAdd(SldNodeType.LOAD)
                    }
                )

                SmallActionButton(
                    text = if (arabic) "باص" else "Bus",
                    onClick = {
                        openAdd(SldNodeType.BUS)
                    }
                )

                SmallActionButton(
                    text = if (arabic) "محول" else "Transformer",
                    onClick = {
                        openAdd(SldNodeType.TRANSFORMER)
                    }
                )

                SmallActionButton(
                    text = if (arabic) "مولد" else "Generator",
                    onClick = {
                        openAdd(SldNodeType.GENERATOR)
                    }
                )

                SmallActionButton(
                    text = if (arabic) "قاطع" else "Breaker",
                    onClick = {
                        openAdd(SldNodeType.BREAKER)
                    }
                )

                SmallActionButton(
                    text = if (arabic) "توصيل" else "Connect",
                    onClick = {
                        connectionStartId = selectedNodeId
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallActionButton(
                    text = if (arabic) "تعديل" else "Edit",
                    onClick = {
                        selectedNodeId?.let { id ->
                            nodes.find { it.id == id }?.let {
                                openEdit(it)
                            }
                        } ?: selectedConnectionId?.let { id ->
                            connections.find { it.id == id }?.let {
                                openConnectionEdit(it)
                            }
                        }
                    }
                )

                SmallActionButton(
                    text = if (arabic) "حذف" else "Delete",
                    onClick = {
                        deleteSelected()
                    }
                )

                SmallActionButton(
                    text = if (arabic) "Upstream" else "Upstream",
                    onClick = {
                        calculateUpstream()
                    }
                )

                SmallActionButton(
                    text = if (arabic) "قصر" else "Short Circuit",
                    onClick = {
                        calculateShortCircuit()
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallActionButton(
                    text = if (arabic) "اختيار الكابلات" else "Cable Sizing",
                    onClick = {
                        calculateCableSizing()
                    }
                )

                SmallActionButton(
                    text = if (arabic) "تنسيق الحماية" else "Protection",
                    onClick = {
                        calculateProtection()
                    }
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    SldCanvas(
                        nodes = nodes,
                        connections = connections,
                        selectedNodeId = selectedNodeId,
                        selectedConnectionId = selectedConnectionId,
                        connectionStartId = connectionStartId,
                        onNodeSelected = { id ->
                            if (connectionStartId != null) {
                                selectedNodeId = id
                                connect()
                            } else {
                                selectedNodeId = id
                                selectedConnectionId = null
                            }
                        },
                        onConnectionSelected = { id ->
                            selectedConnectionId = id
                            selectedNodeId = null
                        },
                        onNodeDragged = { id, dx, dy ->
                            draggedNodeId = id

                            nodes = nodes.map { node ->
                                if (node.id == id) {
                                    node.copy(
                                        x = (node.x + dx)
                                            .coerceAtLeast(10f),
                                        y = (node.y + dy)
                                            .coerceAtLeast(10f)
                                    )
                                } else {
                                    node
                                }
                            }
                        },
                        onEmptyTap = {
                            selectedNodeId = null
                            selectedConnectionId = null
                        }
                    )
                }

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                SldElementsPanel(
                    arabic = arabic,
                    nodes = nodes,
                    connections = connections,
                    shortCircuitStudy = shortCircuitStudy,
                    cableSizingStudy = cableSizingStudy,
                    protectionResult = protectionResult,
                    selectedNodeId = selectedNodeId,
                    selectedConnectionId = selectedConnectionId,
                    onNodeSelected = {
                        selectedNodeId = it
                        selectedConnectionId = null
                    },
                    onConnectionSelected = {
                        selectedConnectionId = it
                        selectedNodeId = null
                    }
                )
            }
        }
    }

    if (showNodeDialog && pendingNodeType != null) {
        NodeEditorDialog(
            arabic = arabic,
            type = pendingNodeType!!,
            editing = editingNodeId != null,
            name = nodeName,
            voltage = nodeVoltage,
            kw = nodeKw,
            pf = nodePf,
            demand = nodeDemand,
            kva = nodeKva,
            transformerZ = nodeTransformerZ,
            generatorXd = nodeGeneratorXd,
            sourceMva = nodeSourceMva,
            onNameChange = { nodeName = it },
            onVoltageChange = { nodeVoltage = it },
            onKwChange = { nodeKw = it },
            onPfChange = { nodePf = it },
            onDemandChange = { nodeDemand = it },
            onKvaChange = { nodeKva = it },
            onTransformerZChange = { nodeTransformerZ = it },
            onGeneratorXdChange = { nodeGeneratorXd = it },
            onSourceMvaChange = { nodeSourceMva = it },
            onDismiss = {
                showNodeDialog = false
            },
            onSave = {
                saveNode()
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
            currentCapacity = connectionCurrentCapacity,
            onLengthChange = { connectionLength = it },
            onResistanceChange = { connectionResistance = it },
            onReactanceChange = { connectionReactance = it },
            onCableSizeChange = { connectionCableSize = it },
            onParallelRunsChange = { connectionParallelRuns = it },
            onCurrentCapacityChange = {
                connectionCurrentCapacity = it
            },
            onDismiss = {
                showConnectionDialog = false
            },
            onSave = {
                saveConnection()
            }
        )
    }

    if (showUpstreamResults && calculationResult != null) {
        UpstreamResultsDialog(
            language = language,
            result = calculationResult!!,
            onDismiss = {
                showUpstreamResults = false
            }
        )
    }

    if (showShortCircuitResults && shortCircuitStudy != null) {
        SldShortCircuitResultsDialog(
            language = language,
            study = shortCircuitStudy!!,
            onDismiss = {
                showShortCircuitResults = false
            }
        )
    }

    if (showCableSizingResults && cableSizingStudy != null) {
        SldCableSizingResultsDialog(
            language = language,
            study = cableSizingStudy!!,
            onDismiss = {
                showCableSizingResults = false
            }
        )
    }

    if (showProtectionResults && protectionResult != null) {
        SldProtectionCoordinationResultsDialog(
            language = language,
            result = protectionResult!!,
            onDismiss = {
                showProtectionResults = false
            }
        )
    }
}

@Composable
private fun SmallActionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(38.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp
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
    onNodeDragged: (String, Float, Float) -> Unit,
    onEmptyTap: () -> Unit
) {
    val nodeWidth = 130f
    val nodeHeight = 60f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101418))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    nodes,
                    connections
                ) {
                    detectTapGestures { offset ->

                        var selectedNode: SldNode? = null

                        nodes.forEach { node ->
                            val inside =
                                offset.x >= node.x &&
                                    offset.x <= node.x + nodeWidth &&
                                    offset.y >= node.y &&
                                    offset.y <= node.y + nodeHeight

                            if (inside) {
                                selectedNode = node
                            }
                        }

                        if (selectedNode != null) {
                            onNodeSelected(
                                selectedNode!!.id
                            )
                            return@detectTapGestures
                        }

                        var selectedConnection: SldConnection? = null
                        var bestDistance = Float.MAX_VALUE

                        connections.forEach { connection ->
                            val from =
                                nodes.find {
                                    it.id == connection.fromNodeId
                                }

                            val to =
                                nodes.find {
                                    it.id == connection.toNodeId
                                }

                            if (from == null || to == null) {
                                return@forEach
                            }

                            val start =
                                Offset(
                                    from.x + nodeWidth,
                                    from.y + nodeHeight / 2f
                                )

                            val end =
                                Offset(
                                    to.x,
                                    to.y + nodeHeight / 2f
                                )

                            val distance =
                                distanceToSegment(
                                    offset,
                                    start,
                                    end
                                )

                            if (
                                distance < 18f &&
                                distance < bestDistance
                            ) {
                                bestDistance = distance
                                selectedConnection = connection
                            }
                        }

                        if (selectedConnection != null) {
                            onConnectionSelected(
                                selectedConnection!!.id
                            )
                        } else {
                            onEmptyTap()
                        }
                    }
                }
                .pointerInput(nodes) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            draggedNodeId = null

                            nodes.forEach { node ->
                                val inside =
                                    offset.x >= node.x &&
                                        offset.x <= node.x + nodeWidth &&
                                        offset.y >= node.y &&
                                        offset.y <= node.y + nodeHeight

                                if (inside) {
                                    draggedNodeId = node.id
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            draggedNodeId?.let {
                                onNodeDragged(
                                    it,
                                    dragAmount.x,
                                    dragAmount.y
                                )
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
            val gridStep = 40f

            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color(0xFF1B2228),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridStep
            }

            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color(0xFF1B2228),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            connections.forEach { connection ->
                val from =
                    nodes.find {
                        it.id == connection.fromNodeId
                    }

                val to =
                    nodes.find {
                        it.id == connection.toNodeId
                    }

                if (from == null || to == null) {
                    return@forEach
                }

                val start =
                    Offset(
                        from.x + nodeWidth,
                        from.y + nodeHeight / 2f
                    )

                val end =
                    Offset(
                        to.x,
                        to.y + nodeHeight / 2f
                    )

                val selected =
                    connection.id == selectedConnectionId

                drawLine(
                    color = if (selected) {
                        PrimaryTeal
                    } else {
                        Color(0xFF8A969F)
                    },
                    start = start,
                    end = end,
                    strokeWidth = if (selected) 5f else 3f,
                    cap = StrokeCap.Round
                )

                val arrowDirection =
                    if (end.x >= start.x) 1f else -1f

                val arrowPoint =
                    Offset(
                        end.x - 5f * arrowDirection,
                        end.y
                    )

                drawLine(
                    color = if (selected) {
                        PrimaryTeal
                    } else {
                        Color(0xFF8A969F)
                    },
                    start = arrowPoint,
                    end = Offset(
                        arrowPoint.x - 10f * arrowDirection,
                        arrowPoint.y - 7f
                    ),
                    strokeWidth = 2f
                )

                drawLine(
                    color = if (selected) {
                        PrimaryTeal
                    } else {
                        Color(0xFF8A969F)
                    },
                    start = arrowPoint,
                    end = Offset(
                        arrowPoint.x - 10f * arrowDirection,
                        arrowPoint.y + 7f
                    ),
                    strokeWidth = 2f
                )
            }

            nodes.forEach { node ->
                val selected =
                    node.id == selectedNodeId

                val startConnection =
                    node.id == connectionStartId

                val borderColor =
                    when {
                        startConnection ->
                            Color(0xFF00BCD4)

                        selected ->
                            PrimaryTeal

                        else ->
                            Color(0xFF68757E)
                    }

                drawRoundRect(
                    color = Color(0xFF182027),
                    topLeft = Offset(node.x, node.y),
                    size = androidx.compose.ui.geometry.Size(
                        nodeWidth,
                        nodeHeight
                    ),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            10f,
                            10f
                        )
                )

                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(node.x, node.y),
                    size = androidx.compose.ui.geometry.Size(
                        nodeWidth,
                        nodeHeight
                    ),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            10f,
                            10f
                        ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (selected || startConnection) {
                            3f
                        } else {
                            1.5f
                        }
                    )
                )
            }
        }

        nodes.forEach { node ->
            NodeLabel(
                node = node,
                modifier = Modifier
                    .padding(
                        start = node.x.dp,
                        top = node.y.dp
                    )
            )
        }

        if (connectionStartId != null) {
            Text(
                text = "Select destination",
                color = PrimaryTeal,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun NodeLabel(
    node: SldNode,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .width(130.dp)
            .height(60.dp)
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            )
    ) {
        Text(
            text = node.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Text(
            text = node.type.name,
            color = TextSecondary,
            fontSize = 9.sp,
            maxLines = 1
        )

        if (node.loadKw > 0.0) {
            Text(
                text = "${formatNumber(node.loadKw)} kW",
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun SldElementsPanel(
    arabic: Boolean,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    shortCircuitStudy: SldShortCircuitStudy?,
    cableSizingStudy: SldCableSizingStudy?,
    protectionResult: SldProtectionCoordinationResult?,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    onNodeSelected: (String) -> Unit,
    onConnectionSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151B20)
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = if (arabic) {
                        "عناصر المخطط"
                    } else {
                        "SLD Elements"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                HorizontalDivider()
            }

            items(
                items = nodes,
                key = { it.id }
            ) { node ->
                Card(
                    onClick = {
                        onNodeSelected(node.id)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (node.id == selectedNodeId) {
                                Color(0xFF20383D)
                            } else {
                                Color(0xFF1A2127)
                            }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = node.name,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = node.type.name,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )

                        if (node.loadKw > 0.0) {
                            Text(
                                text = "${formatNumber(node.loadKw)} kW",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        val fault =
                            shortCircuitStudy
                                ?.results
                                ?.get(node.id)

                        if (fault != null) {
                            Text(
                                text = "Ik'' ${formatNumber(
                                    fault.initialSymmetricalCurrentKa
                                )} kA",
                                color = faultColor(
                                    fault.initialSymmetricalCurrentKa
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val protection =
                            protectionResult
                                ?.devices
                                ?.get(node.id)

                        if (protection != null) {
                            Text(
                                text =
                                    "CB ${formatNumber(
                                        protection.recommendedRatingA
                                    )} A",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = if (arabic) {
                        "المغذيات"
                    } else {
                        "Feeders"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = connections,
                key = { it.id }
            ) { connection ->
                val from =
                    nodes.find {
                        it.id == connection.fromNodeId
                    }

                val to =
                    nodes.find {
                        it.id == connection.toNodeId
                    }

                val cable =
                    cableSizingStudy
                        ?.results
                        ?.get(connection.id)

                Card(
                    onClick = {
                        onConnectionSelected(connection.id)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (connection.id == selectedConnectionId) {
                                Color(0xFF20383D)
                            } else {
                                Color(0xFF1A2127)
                            }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text =
                                "${from?.name ?: connection.fromNodeId} → " +
                                    "${to?.name ?: connection.toNodeId}",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${formatNumber(
                                connection.lengthMeters
                            )} m",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )

                        if (connection.cableSizeMm2 > 0.0) {
                            Text(
                                text =
                                    "Cable ${formatNumber(
                                        connection.cableSizeMm2
                                    )} mm² × " +
                                        connection.parallelRuns,
                                color = PrimaryTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (
                            cable != null &&
                            cable.recommendedSizeMm2 > 0.0
                        ) {
                            Text(
                                text =
                                    "Recommended ${formatNumber(
                                        cable.recommendedSizeMm2
                                    )} mm² × " +
                                        cable.recommendedParallelRuns,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeEditorDialog(
    arabic: Boolean,
    type: SldNodeType,
    editing: Boolean,
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
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = {
            Text(
                text = if (editing) {
                    if (arabic) "تعديل العنصر" else "Edit Element"
                } else {
                    if (arabic) "إضافة عنصر" else "Add Element"
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    EditorField(
                        label = if (arabic) "الاسم" else "Name",
                        value = name,
                        onValueChange = onNameChange
                    )
                }

                item {
                    EditorField(
                        label = if (arabic) "الجهد V" else "Voltage V",
                        value = voltage,
                        onValueChange = onVoltageChange
                    )
                }

                if (
                    type == SldNodeType.LOAD ||
                    type == SldNodeType.PANEL
                ) {
                    item {
                        EditorField(
                            label = if (arabic) "الحمل kW" else "Load kW",
                            value = kw,
                            onValueChange = onKwChange
                        )
                    }

                    item {
                        EditorField(
                            label = if (arabic) "معامل القدرة" else "Power Factor",
                            value = pf,
                            onValueChange = onPfChange
                        )
                    }

                    item {
                        EditorField(
                            label = if (arabic) "معامل الطلب" else "Demand Factor",
                            value = demand,
                            onValueChange = onDemandChange
                        )
                    }
                }

                if (
                    type == SldNodeType.TRANSFORMER ||
                    type == SldNodeType.GENERATOR
                ) {
                    item {
                        EditorField(
                            label = if (arabic) "القدرة kVA" else "Rated kVA",
                            value = kva,
                            onValueChange = onKvaChange
                        )
                    }
                }

                if (type == SldNodeType.TRANSFORMER) {
                    item {
                        EditorField(
                            label = if (arabic) "%Z للمحول" else "Transformer %Z",
                            value = transformerZ,
                            onValueChange = onTransformerZChange
                        )
                    }
                }

                if (type == SldNodeType.GENERATOR) {
                    item {
                        EditorField(
                            label = if (arabic) "Xd'' %" else "Generator Xd'' %",
                            value = generatorXd,
                            onValueChange = onGeneratorXdChange
                        )
                    }
                }

                if (type == SldNodeType.SOURCE) {
                    item {
                        EditorField(
                            label = if (arabic) "قدرة القصر MVA" else "Source Scc MVA",
                            value = sourceMva,
                            onValueChange = onSourceMvaChange
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
                    text = if (arabic) "حفظ" else "Save",
                    color = PrimaryTeal
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = if (arabic) "إلغاء" else "Cancel",
                    color = TextSecondary
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
        containerColor = DarkBackground,
        title = {
            Text(
                text = if (arabic) {
                    "بيانات المغذي"
                } else {
                    "Feeder Data"
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    EditorField(
                        label = if (arabic) "الطول m" else "Length m",
                        value = length,
                        onValueChange = onLengthChange
                    )
                }

                item {
                    EditorField(
                        label = if (arabic) "المقاومة Ω/km" else "Resistance Ω/km",
                        value = resistance,
                        onValueChange = onResistanceChange
                    )
                }

                item {
                    EditorField(
                        label = if (arabic) "المفاعلة Ω/km" else "Reactance Ω/km",
                        value = reactance,
                        onValueChange = onReactanceChange
                    )
                }

                item {
                    EditorField(
                        label = if (arabic) "مقطع الكابل mm²" else "Cable size mm²",
                        value = cableSize,
                        onValueChange = onCableSizeChange
                    )
                }

                item {
                    EditorField(
                        label = if (arabic) "المسارات المتوازية" else "Parallel runs",
                        value = parallelRuns,
                        onValueChange = onParallelRunsChange
                    )
                }

                item {
                    EditorField(
                        label = if (arabic) "سعة التيار A" else "Current capacity A",
                        value = currentCapacity,
                        onValueChange = onCurrentCapacityChange
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave
            ) {
                Text(
                    text = if (arabic) "حفظ" else "Save",
                    color = PrimaryTeal
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = if (arabic) "إلغاء" else "Cancel",
                    color = TextSecondary
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
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun UpstreamResultsDialog(
    language: AppLanguage,
    result: SldCalculationResult,
    onDismiss: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = {
            Text(
                text = if (arabic) {
                    "نتائج حسابات Upstream"
                } else {
                    "Upstream Calculation"
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    ResultLine(
                        if (arabic) "الحمل المتصل" else "Connected load",
                        "${formatNumber(result.totalConnectedLoadKw)} kW"
                    )
                }

                item {
                    ResultLine(
                        if (arabic) "الحمل المطلوب" else "Demand load",
                        "${formatNumber(result.totalDemandLoadKw)} kW"
                    )
                }

                item {
                    ResultLine(
                        if (arabic) "القدرة المطلوبة" else "Required kVA",
                        "${formatNumber(result.totalRequiredKva)} kVA"
                    )
                }

                item {
                    ResultLine(
                        if (arabic) "تيار الدخول الرئيسي" else "Main current",
                        "${formatNumber(result.mainCurrentA)} A"
                    )
                }

                item {
                    ResultLine(
                        if (arabic) "القاطع الرئيسي" else "Main breaker",
                        "${formatNumber(result.mainBreakerA)} A"
                    )
                }

                item {
                    ResultLine(
                        if (arabic) "المحول المطلوب" else "Required transformer",
                        "${formatNumber(result.requiredTransformerKva)} kVA"
                    )
                }

                item {
                    ResultLine(
                        if (arabic) "هبوط الجهد" else "Voltage drop",
                        "${formatNumber(result.totalVoltageDropPercent)} %"
                    )
                }

                if (result.notes.isNotEmpty()) {
                    item {
                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        result.notes.forEach {
                            Text(
                                text = "• $it",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
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
                    text = if (arabic) "إغلاق" else "Close",
                    color = TextPrimary
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

private fun distanceToSegment(
    point: Offset,
    start: Offset,
    end: Offset
): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y

    if (dx == 0f && dy == 0f) {
        return sqrt(
            (point.x - start.x) * (point.x - start.x) +
                (point.y - start.y) * (point.y - start.y)
        )
    }

    val t =
        (
            (point.x - start.x) * dx +
                (point.y - start.y) * dy
            ) /
            (dx * dx + dy * dy)

    val clamped =
        t.coerceIn(0f, 1f)

    val projection =
        Offset(
            start.x + clamped * dx,
            start.y + clamped * dy
        )

    return sqrt(
        (point.x - projection.x) *
            (point.x - projection.x) +
            (point.y - projection.y) *
            (point.y - projection.y)
    )
}

private fun faultColor(
    currentKa: Double
): Color =
    when {
        currentKa <= 10.0 ->
            Color(0xFF4CAF50)

        currentKa <= 25.0 ->
            Color(0xFFFFC107)

        else ->
            Color(0xFFF44336)
    }

private fun formatNumber(
    value: Double
): String =
    when {
        value >= 1000.0 ->
            "%.0f".format(value)

        value >= 100.0 ->
            "%.0f".format(value)

        value >= 10.0 ->
            "%.1f".format(value)

        else ->
            "%.2f".format(value)
    }
