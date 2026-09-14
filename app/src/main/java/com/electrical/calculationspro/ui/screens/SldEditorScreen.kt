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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.PanelScheduleStatus
import com.electrical.calculationspro.data.SldCalculationResult
import com.electrical.calculationspro.data.SldCableSizingEngine
import com.electrical.calculationspro.data.SldCableSizingStudy
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringEngine
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldPanelSchedule
import com.electrical.calculationspro.data.SldPanelScheduleEngine
import com.electrical.calculationspro.data.SldProtectionCoordinationEngine
import com.electrical.calculationspro.data.SldProtectionCoordinationResult
import com.electrical.calculationspro.data.SldShortCircuitEngine
import com.electrical.calculationspro.data.SldShortCircuitStudy
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private val DarkBackground = Color(0xFF0B1116)
private val DarkCard = Color(0xFF151D24)
private val DarkCard2 = Color(0xFF1C2730)
private val TextPrimary = Color(0xFFF2F5F7)
private val TextSecondary = Color(0xFFAAB7C0)
private val Accent = Color(0xFF00BCD4)
private val ErrorColor = Color(0xFFF44336)

@Composable
fun SldEditorScreen(
    language: String = "en",
    onBack: (() -> Unit)? = null
) {
    var nodes by remember {
        mutableStateOf(
            listOf(
                SldNode(
                    id = "source-1",
                    name = "MAIN SOURCE",
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

    var showPanelSchedule by remember {
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
        mutableStateOf("6")
    }

    var nodeGeneratorXd by remember {
        mutableStateOf("15")
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
        mutableStateOf("0.080")
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

    var panelSchedule by remember {
        mutableStateOf<SldPanelSchedule?>(null)
    }

    fun currentNetwork(): SldNetwork {
        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }

    fun invalidateResults() {
        calculationResult = null
        shortCircuitStudy = null
        cableSizingStudy = null
        protectionResult = null
        panelSchedule = null
    }

    fun defaultName(type: SldNodeType): String {
        val prefix =
            when (type) {
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
            nodeVoltage.toDoubleOrNull() ?: 400.0

        val loadKw =
            nodeKw.toDoubleOrNull() ?: 0.0

        val pf =
            (nodePf.toDoubleOrNull() ?: 0.90)
                .coerceIn(0.01, 1.0)

        val demand =
            (nodeDemand.toDoubleOrNull() ?: 1.0)
                .coerceIn(0.0, 1.0)

        val kva =
            nodeKva.toDoubleOrNull() ?: 0.0

        val transformerZ =
            nodeTransformerZ.toDoubleOrNull() ?: 0.0

        val generatorXd =
            nodeGeneratorXd.toDoubleOrNull() ?: 0.0

        val sourceMva =
            nodeSourceMva.toDoubleOrNull() ?: 0.0

        if (editingNodeId == null) {
            val newNode =
                SldNode(
                    id = "node-${System.currentTimeMillis()}",
                    name = nodeName.ifBlank {
                        defaultName(pendingNodeType)
                    },
                    type = pendingNodeType,
                    x = (nodes.maxOfOrNull { it.x } ?: 80f) + 180f,
                    y = nodes.maxOfOrNull { it.y } ?: 180f,
                    voltage = voltage,
                    loadKw = loadKw,
                    powerFactor = pf,
                    demandFactor = demand,
                    ratedKva = kva,
                    transformerPercentZ = transformerZ,
                    generatorXdSubtransient = generatorXd,
                    sourceShortCircuitMva = sourceMva
                )

            nodes = nodes + newNode
            selectedNodeId = newNode.id
        } else {
            val id = editingNodeId!!

            nodes = nodes.map { node ->
                if (node.id == id) {
                    node.copy(
                        name = nodeName.ifBlank { node.name },
                        type = pendingNodeType,
                        voltage = voltage,
                        loadKw = loadKw,
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

        invalidateResults()
        showNodeDialog = false
    }

    fun saveConnection() {
        val from =
            connectionStartId

        val to =
            selectedNodeId

        if (
            editingConnectionId == null &&
            (from == null || to == null || from == to)
        ) {
            connectionStartId = null
            showConnectionDialog = false
            return
        }

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
            val duplicate =
                connections.any {
                    it.fromNodeId == from &&
                        it.toNodeId == to
                }

            if (!duplicate) {
                val newConnection =
                    SldConnection(
                        id = "connection-${System.currentTimeMillis()}",
                        fromNodeId = from!!,
                        toNodeId = to!!,
                        lengthMeters = length,
                        resistanceOhmPerKm = resistance,
                        reactanceOhmPerKm = reactance,
                        cableSizeMm2 = cableSize,
                        parallelRuns = runs,
                        currentCapacityA = capacity
                    )

                connections =
                    connections + newConnection

                selectedConnectionId =
                    newConnection.id
            }
        } else {
            val id = editingConnectionId!!

            connections =
                connections.map { connection ->
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
        invalidateResults()
        showConnectionDialog = false
    }

    fun deleteSelected() {
        val nodeId = selectedNodeId

        if (nodeId != null && nodes.size > 1) {
            nodes =
                nodes.filterNot {
                    it.id == nodeId
                }

            connections =
                connections.filter {
                    it.fromNodeId != nodeId &&
                        it.toNodeId != nodeId
                }

            selectedNodeId = null
            selectedConnectionId = null
            connectionStartId = null
            invalidateResults()
            return
        }

        val connectionId = selectedConnectionId

        if (connectionId != null) {
            connections =
                connections.filterNot {
                    it.id == connectionId
                }

            selectedConnectionId = null
            invalidateResults()
        }
    }

    fun toggleConnectionMode() {
        val selected =
            selectedNodeId

        if (selected == null) {
            connectionStartId = null
            return
        }

        if (connectionStartId == null) {
            connectionStartId = selected
            return
        }

        if (connectionStartId == selected) {
            connectionStartId = null
            return
        }

        val duplicate =
            connections.any {
                it.fromNodeId == connectionStartId &&
                    it.toNodeId == selected
            }

        if (!duplicate) {
            val connection =
                SldConnection(
                    id = "connection-${System.currentTimeMillis()}",
                    fromNodeId = connectionStartId!!,
                    toNodeId = selected
                )

            connections =
                connections + connection

            selectedConnectionId =
                connection.id
        }

        connectionStartId = null
        invalidateResults()
    }

    fun calculateUpstream() {
        calculationResult =
            try {
                SldEngineeringEngine.calculateUpstream(
                    currentNetwork()
                )
            } catch (_: Exception) {
                null
            }

        showUpstreamResults =
            calculationResult != null
    }

    fun calculateShortCircuit(
        openDialog: Boolean
    ) {
        shortCircuitStudy =
            try {
                SldShortCircuitEngine.calculate(
                    currentNetwork()
                )
            } catch (_: Exception) {
                null
            }

        if (openDialog) {
            showShortCircuitResults =
                shortCircuitStudy != null
        }
    }

    fun calculateCableSizing(
        openDialog: Boolean
    ) {
        if (shortCircuitStudy == null) {
            calculateShortCircuit(false)
        }

        val study =
            try {
                SldCableSizingEngine.calculate(
                    currentNetwork(),
                    shortCircuitStudy = shortCircuitStudy
                )
            } catch (_: Exception) {
                null
            }

        cableSizingStudy = study

        if (study != null) {
            connections =
                connections.map { connection ->
                    val result =
                        study.results[connection.id]

                    if (result == null) {
                        connection
                    } else {
                        connection.copy(
                            cableSizeMm2 =
                                result.recommendedSizeMm2,
                            parallelRuns =
                                result.recommendedParallelRuns,
                            currentCapacityA =
                                result.recommendedCurrentCapacityA,
                            voltageDropPercent =
                                result.recommendedVoltageDropPercent
                        )
                    }
                }
        }

        if (openDialog) {
            showCableSizingResults =
                study != null
        }
    }

    fun calculateProtection() {
        if (shortCircuitStudy == null) {
            calculateShortCircuit(false)
        }

        if (cableSizingStudy == null) {
            calculateCableSizing(false)
        }

        protectionResult =
            try {
                SldProtectionCoordinationEngine.calculate(
                    currentNetwork(),
                    shortCircuitStudy = shortCircuitStudy,
                    cableSizingStudy = cableSizingStudy
                )
            } catch (_: Exception) {
                null
            }

        showProtectionResults =
            protectionResult != null
    }

    fun calculatePanelSchedule() {
        val panelId =
            selectedNodeId ?: return

        val panel =
            nodes.firstOrNull {
                it.id == panelId
            } ?: return

        if (
            panel.type != SldNodeType.PANEL &&
            panel.type != SldNodeType.BUS &&
            panel.type != SldNodeType.SOURCE
        ) {
            return
        }

        if (shortCircuitStudy == null) {
            calculateShortCircuit(false)
        }

        if (cableSizingStudy == null) {
            calculateCableSizing(false)
        }

        panelSchedule =
            try {
                SldPanelScheduleEngine.calculate(
                    network = currentNetwork(),
                    panelNodeId = panelId,
                    cableSizingStudy = cableSizingStudy,
                    shortCircuitStudy = shortCircuitStudy
                )
            } catch (_: Exception) {
                null
            }

        showPanelSchedule =
            panelSchedule != null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(8.dp),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            if (onBack != null) {
                ActionButton(
                    text = "Back",
                    onClick = onBack
                )
            }

            ActionButton(
                text = "Load",
                onClick = {
                    openAdd(SldNodeType.LOAD)
                }
            )

            ActionButton(
                text = "Bus",
                onClick = {
                    openAdd(SldNodeType.BUS)
                }
            )

            ActionButton(
                text = "Panel",
                onClick = {
                    openAdd(SldNodeType.PANEL)
                }
            )

            ActionButton(
                text = "Transformer",
                onClick = {
                    openAdd(SldNodeType.TRANSFORMER)
                }
            )

            ActionButton(
                text = "Generator",
                onClick = {
                    openAdd(SldNodeType.GENERATOR)
                }
            )

            ActionButton(
                text = "Breaker",
                onClick = {
                    openAdd(SldNodeType.BREAKER)
                }
            )

            ActionButton(
                text = "Connect",
                selected = connectionStartId != null,
                onClick = ::toggleConnectionMode
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 2.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            ActionButton(
                text = "Edit",
                enabled = selectedNodeId != null,
                onClick = {
                    selectedNodeId?.let { id ->
                        nodes.firstOrNull {
                            it.id == id
                        }?.let(::openEdit)
                    }
                }
            )

            ActionButton(
                text = "Feeder",
                enabled = selectedConnectionId != null,
                onClick = {
                    selectedConnectionId?.let { id ->
                        connections.firstOrNull {
                            it.id == id
                        }?.let(::openConnectionEdit)
                    }
                }
            )

            ActionButton(
                text = "Delete",
                enabled =
                    selectedNodeId != null ||
                        selectedConnectionId != null,
                onClick = ::deleteSelected
            )

            ActionButton(
                text = "Upstream",
                onClick = ::calculateUpstream
            )

            ActionButton(
                text = "Short Circuit",
                onClick = {
                    calculateShortCircuit(true)
                }
            )

            ActionButton(
                text = "Cable Sizing",
                onClick = {
                    calculateCableSizing(true)
                }
            )

            ActionButton(
                text = "Protection",
                onClick = ::calculateProtection
            )

            ActionButton(
                text = "Panel Schedule",
                enabled = selectedNodeId != null,
                onClick = ::calculatePanelSchedule
            )
        }

        HorizontalDivider(
            color = DarkCard2
        )

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            SldCanvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                connectionStartId = connectionStartId,
                onNodeSelected = { id ->
                    selectedNodeId = id
                    selectedConnectionId = null
                },
                onNodeMoved = { id, x, y ->
                    nodes =
                        nodes.map { node ->
                            if (node.id == id) {
                                node.copy(
                                    x = x,
                                    y = y
                                )
                            } else {
                                node
                            }
                        }

                    invalidateResults()
                },
                onConnectionSelected = { id ->
                    selectedConnectionId = id
                    selectedNodeId = null
                },
                onEmptySelected = {
                    selectedNodeId = null
                    selectedConnectionId = null
                    connectionStartId = null
                }
            )

            ElementsPanel(
                modifier = Modifier
                    .widthIn(
                        min = 240.dp,
                        max = 310.dp
                    )
                    .fillMaxHeight(),
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                panelSchedule = panelSchedule,
                onNodeClick = {
                    selectedNodeId = it
                    selectedConnectionId = null
                },
                onConnectionClick = {
                    selectedConnectionId = it
                    selectedNodeId = null
                }
            )
        }
    }

    if (showNodeDialog) {
        NodeDialog(
            editing = editingNodeId != null,
            nodeType = pendingNodeType,
            name = nodeName,
            voltage = nodeVoltage,
            kw = nodeKw,
            pf = nodePf,
            demand = nodeDemand,
            kva = nodeKva,
            transformerZ = nodeTransformerZ,
            generatorXd = nodeGeneratorXd,
            sourceMva = nodeSourceMva,
            onName = { nodeName = it },
            onVoltage = { nodeVoltage = it },
            onKw = { nodeKw = it },
            onPf = { nodePf = it },
            onDemand = { nodeDemand = it },
            onKva = { nodeKva = it },
            onTransformerZ = { nodeTransformerZ = it },
            onGeneratorXd = { nodeGeneratorXd = it },
            onSourceMva = { nodeSourceMva = it },
            onDismiss = {
                showNodeDialog = false
            },
            onSave = ::saveNode
        )
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            length = connectionLength,
            resistance = connectionResistance,
            reactance = connectionReactance,
            cableSize = connectionCableSize,
            runs = connectionParallelRuns,
            capacity = connectionCurrentCapacity,
            onLength = { connectionLength = it },
            onResistance = { connectionResistance = it },
            onReactance = { connectionReactance = it },
            onCableSize = { connectionCableSize = it },
            onRuns = { connectionParallelRuns = it },
            onCapacity = { connectionCurrentCapacity = it },
            onDismiss = {
                showConnectionDialog = false
                connectionStartId = null
            },
            onSave = ::saveConnection
        )
    }

    if (showUpstreamResults && calculationResult != null) {
        UpstreamDialog(
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

    if (showPanelSchedule && panelSchedule != null) {
        SldPanelScheduleResultsDialog(
            language = language,
            schedule = panelSchedule!!,
            onDismiss = {
                showPanelSchedule = false
            }
        )
    }
}

@Composable
private fun SldCanvas(
    modifier: Modifier,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    connectionStartId: String?,
    onNodeSelected: (String) -> Unit,
    onNodeMoved: (String, Float, Float) -> Unit,
    onConnectionSelected: (String) -> Unit,
    onEmptySelected: () -> Unit
) {
    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    Canvas(
        modifier = modifier
            .background(DarkBackground)
            .pointerInput(nodes) {
                detectTapGestures { position ->
                    val node =
                        findNodeAt(
                            nodes,
                            position
                        )

                    if (node != null) {
                        onNodeSelected(node.id)
                        return@detectTapGestures
                    }

                    val connection =
                        findConnectionAt(
                            nodes,
                            connections,
                            position
                        )

                    if (connection != null) {
                        onConnectionSelected(
                            connection.id
                        )
                    } else {
                        onEmptySelected()
                    }
                }
            }
            .pointerInput(nodes) {
                detectDragGestures(
                    onDragStart = { position ->
                        draggedNodeId =
                            findNodeAt(
                                nodes,
                                position
                            )?.id
                    },
                    onDrag = { change, amount ->
                        val id =
                            draggedNodeId
                                ?: return@detectDragGestures

                        change.consume()

                        val node =
                            nodes.firstOrNull {
                                it.id == id
                            } ?: return@detectDragGestures

                        onNodeMoved(
                            id,
                            max(
                                20f,
                                node.x + amount.x
                            ),
                            max(
                                20f,
                                node.y + amount.y
                            )
                        )
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

        val nodeMap =
            nodes.associateBy { it.id }

        connections.forEach { connection ->
            val from =
                nodeMap[connection.fromNodeId]

            val to =
                nodeMap[connection.toNodeId]

            if (from != null && to != null) {
                drawConnection(
                    from,
                    to,
                    connection.id == selectedConnectionId
                )
            }
        }

        nodes.forEach { node ->
            drawNode(
                node,
                node.id == selectedNodeId,
                node.id == connectionStartId
            )
        }
    }
}

private fun DrawScope.drawGrid() {
    val grid = 40f

    var x = 0f
    while (x < size.width) {
        drawLine(
            color = Color(0xFF182229),
            start = Offset(x, 0f),
            end = Offset(x, size.height)
        )
        x += grid
    }

    var y = 0f
    while (y < size.height) {
        drawLine(
            color = Color(0xFF182229),
            start = Offset(0f, y),
            end = Offset(size.width, y)
        )
        y += grid
    }
}

private fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    val color =
        when (node.type) {
            SldNodeType.SOURCE -> Color(0xFF1565C0)
            SldNodeType.TRANSFORMER -> Color(0xFF6A1B9A)
            SldNodeType.GENERATOR -> Color(0xFF2E7D32)
            SldNodeType.BUS -> Color(0xFF455A64)
            SldNodeType.PANEL -> Color(0xFF00695C)
            SldNodeType.BREAKER -> Color(0xFFEF6C00)
            SldNodeType.LOAD -> Color(0xFF37474F)
        }

    drawRoundRect(
        color = color,
        topLeft = Offset(node.x, node.y),
        size = Size(150f, 70f),
        cornerRadius = CornerRadius(12f)
    )

    drawRoundRect(
        color =
            when {
                connectionStart -> Accent
                selected -> Color.White
                else -> Color(0xFF667781)
            },
        topLeft = Offset(
            node.x - 2f,
            node.y - 2f
        ),
        size = Size(154f, 74f),
        cornerRadius = CornerRadius(14f),
        style = Stroke(
            width =
                if (
                    connectionStart ||
                    selected
                ) {
                    3f
                } else {
                    1.5f
                }
        )
    )
}

private fun DrawScope.drawConnection(
    from: SldNode,
    to: SldNode,
    selected: Boolean
) {
    val start =
        Offset(
            from.x + 150f,
            from.y + 35f
        )

    val end =
        Offset(
            to.x,
            to.y + 35f
        )

    val middle =
        (start.x + end.x) / 2f

    val path =
        Path().apply {
            moveTo(
                start.x,
                start.y
            )

            cubicTo(
                middle,
                start.y,
                middle,
                end.y,
                end.x,
                end.y
            )
        }

    val color =
        if (selected) {
            Accent
        } else {
            Color(0xFF8EA0AA)
        }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width =
                if (selected) 5f else 3f
        )
    )

    drawCircle(
        color = color,
        radius = 5f,
        center = end
    )
}

private fun findNodeAt(
    nodes: List<SldNode>,
    position: Offset
): SldNode? {
    return nodes.lastOrNull { node ->
        position.x >= node.x &&
            position.x <= node.x + 150f &&
            position.y >= node.y &&
            position.y <= node.y + 70f
    }
}

private fun findConnectionAt(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    position: Offset
): SldConnection? {
    val map =
        nodes.associateBy { it.id }

    return connections.firstOrNull { connection ->
        val from =
            map[connection.fromNodeId]
                ?: return@firstOrNull false

        val to =
            map[connection.toNodeId]
                ?: return@firstOrNull false

        distanceToSegment(
            position,
            Offset(
                from.x + 150f,
                from.y + 35f
            ),
            Offset(
                to.x,
                to.y + 35f
            )
        ) <= 18f
    }
}

private fun distanceToSegment(
    point: Offset,
    start: Offset,
    end: Offset
): Float {
    val dx =
        end.x - start.x

    val dy =
        end.y - start.y

    if (dx == 0f && dy == 0f) {
        return hypot(
            point.x - start.x,
            point.y - start.y
        )
    }

    val t =
        (
            (
                (point.x - start.x) * dx +
                    (point.y - start.y) * dy
                ) /
                (dx * dx + dy * dy)
            )
            .coerceIn(0f, 1f)

    val projection =
        Offset(
            start.x + t * dx,
            start.y + t * dy
        )

    return hypot(
        point.x - projection.x,
        point.y - projection.y
    )
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color =
                if (selected) {
                    Accent
                } else {
                    TextPrimary
                },
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ElementsPanel(
    modifier: Modifier,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    panelSchedule: SldPanelSchedule?,
    onNodeClick: (String) -> Unit,
    onConnectionClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .background(DarkCard)
            .padding(10.dp)
    ) {
        Text(
            text = "SLD ELEMENTS",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            nodes.forEach { node ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            onNodeClick(node.id)
                        },
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (
                                    node.id ==
                                    selectedNodeId
                                ) {
                                    Color(0xFF263A43)
                                } else {
                                    DarkCard2
                                }
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(9.dp)
                    ) {
                        Text(
                            text = node.name,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = node.type.name,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )

                        Text(
                            text =
                                "${formatNumber(node.loadKw)} kW | ${formatNumber(node.voltage)} V",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "FEEDERS",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            connections.forEach { connection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            onConnectionClick(
                                connection.id
                            )
                        },
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (
                                    connection.id ==
                                    selectedConnectionId
                                ) {
                                    Color(0xFF263A43)
                                } else {
                                    DarkCard2
                                }
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text =
                                "${connection.fromNodeId} → ${connection.toNodeId}",
                            color = TextPrimary,
                            fontSize = 11.sp
                        )

                        Text(
                            text =
                                "${formatNumber(connection.lengthMeters)} m",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        if (panelSchedule != null) {
            HorizontalDivider(
                color = DarkCard2
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "LAST PANEL SCHEDULE",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            Text(
                text = panelSchedule.panelName,
                color = Accent,
                fontSize = 12.sp
            )

            Text(
                text =
                    "${panelSchedule.rows.size} feeders | ${formatNumber(panelSchedule.totalDemandLoadKw)} kW",
                color = TextSecondary,
                fontSize = 10.sp
            )

            val failures =
                panelSchedule.rows.count {
                    it.status ==
                        PanelScheduleStatus.FAIL
                }

            if (failures > 0) {
                Text(
                    text =
                        "$failures feeder(s) require correction",
                    color = ErrorColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun NodeDialog(
    editing: Boolean,
    nodeType: SldNodeType,
    name: String,
    voltage: String,
    kw: String,
    pf: String,
    demand: String,
    kva: String,
    transformerZ: String,
    generatorXd: String,
    sourceMva: String,
    onName: (String) -> Unit,
    onVoltage: (String) -> Unit,
    onKw: (String) -> Unit,
    onPf: (String) -> Unit,
    onDemand: (String) -> Unit,
    onKva: (String) -> Unit,
    onTransformerZ: (String) -> Unit,
    onGeneratorXd: (String) -> Unit,
    onSourceMva: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text =
                    if (editing) {
                        "Edit ${nodeType.name}"
                    } else {
                        "Add ${nodeType.name}"
                    },
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                EditorField(
                    "Name",
                    name,
                    onName
                )

                EditorField(
                    "Voltage V",
                    voltage,
                    onVoltage
                )

                EditorField(
                    "Load kW",
                    kw,
                    onKw
                )

                EditorField(
                    "Power Factor",
                    pf,
                    onPf
                )

                EditorField(
                    "Demand Factor",
                    demand,
                    onDemand
                )

                EditorField(
                    "Rated kVA",
                    kva,
                    onKva
                )

                if (
                    nodeType ==
                    SldNodeType.TRANSFORMER
                ) {
                    EditorField(
                        "Transformer %Z",
                        transformerZ,
                        onTransformerZ
                    )
                }

                if (
                    nodeType ==
                    SldNodeType.GENERATOR
                ) {
                    EditorField(
                        "Generator Xd'' %",
                        generatorXd,
                        onGeneratorXd
                    )
                }

                if (
                    nodeType ==
                    SldNodeType.SOURCE
                ) {
                    EditorField(
                        "Source Short Circuit MVA",
                        sourceMva,
                        onSourceMva
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text("Save")
            }
        }
    )
}

@Composable
private fun ConnectionDialog(
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
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = "Feeder Parameters",
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                EditorField(
                    "Length m",
                    length,
                    onLength
                )

                EditorField(
                    "R Ω/km",
                    resistance,
                    onResistance
                )

                EditorField(
                    "X Ω/km",
                    reactance,
                    onReactance
                )

                EditorField(
                    "Cable Size mm²",
                    cableSize,
                    onCableSize
                )

                EditorField(
                    "Parallel Runs",
                    runs,
                    onRuns
                )

                EditorField(
                    "Current Capacity A",
                    capacity,
                    onCapacity
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text("Save")
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
    TextField(
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
private fun UpstreamDialog(
    result: SldCalculationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = "Upstream Calculation",
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                ResultRow(
                    "Connected Load",
                    "${formatNumber(result.totalConnectedLoadKw)} kW"
                )

                ResultRow(
                    "Demand Load",
                    "${formatNumber(result.totalDemandLoadKw)} kW"
                )

                ResultRow(
                    "Required kVA",
                    "${formatNumber(result.totalRequiredKva)} kVA"
                )

                ResultRow(
                    "Main Current",
                    "${formatNumber(result.mainCurrentA)} A"
                )

                ResultRow(
                    "Main Breaker",
                    "${formatNumber(result.mainBreakerA)} A"
                )

                ResultRow(
                    "Transformer",
                    "${formatNumber(result.requiredTransformerKva)} kVA"
                )

                ResultRow(
                    "Voltage Drop",
                    "${formatNumber(result.totalVoltageDropPercent)} %"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Close",
                    color = Accent
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
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

private fun formatNumber(
    value: Double
): String {
    if (!value.isFinite()) {
        return "0"
    }

    val rounded =
        (value * 100.0).roundToInt() / 100.0

    return String.format(
        Locale.US,
        "%.2f",
        rounded
    )
        .trimEnd('0')
        .trimEnd('.')
}
