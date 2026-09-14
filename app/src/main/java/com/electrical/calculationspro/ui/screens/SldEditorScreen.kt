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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Path
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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val DarkBackground = Color(0xFF0B1116)
private val DarkCard = Color(0xFF151D24)
private val DarkCard2 = Color(0xFF1C2730)
private val TextPrimary = Color(0xFFF2F5F7)
private val TextSecondary = Color(0xFFAAB7C0)
private val Accent = Color(0xFF00BCD4)
private val Success = Color(0xFF4CAF50)
private val Warning = Color(0xFFFFC107)
private val Error = Color(0xFFF44336)

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
        panelSchedule = null
    }

    fun defaultName(type: SldNodeType): String {
        val prefix =
            when (type) {
                SldNodeType.SOURCE -> "SOURCE"
                SldNodeType.BUS -> "BUS"
                SldNodeType.TRANSFORMER -> "TR"
                SldNodeType.GENERATOR -> "GEN"
                SldNodeType.BREAKER -> "CB"
                SldNodeType.PANEL -> "PANEL"
                SldNodeType.LOAD -> "LOAD"
            }

        val count =
            nodes.count { it.type == type } + 1

        return "$prefix-$count"
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
        val voltage = nodeVoltage.toDoubleOrNull() ?: 400.0
        val kw = nodeKw.toDoubleOrNull() ?: 0.0
        val pf = (nodePf.toDoubleOrNull() ?: 0.90).coerceIn(0.01, 1.0)
        val demand = (nodeDemand.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0)
        val kva = nodeKva.toDoubleOrNull() ?: 0.0
        val transformerZ = nodeTransformerZ.toDoubleOrNull() ?: 0.0
        val generatorXd = nodeGeneratorXd.toDoubleOrNull() ?: 0.0
        val sourceMva = nodeSourceMva.toDoubleOrNull() ?: 0.0

        if (editingNodeId == null) {
            val maxX =
                nodes.maxOfOrNull { it.x } ?: 80f

            val maxY =
                nodes.maxOfOrNull { it.y } ?: 180f

            val newNode =
                SldNode(
                    id = "node-${System.currentTimeMillis()}",
                    name = nodeName.ifBlank {
                        defaultName(pendingNodeType)
                    },
                    type = pendingNodeType,
                    x = maxX + 180f,
                    y = maxY,
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
        } else {
            val id = editingNodeId!!

            nodes =
                nodes.map { node ->
                    if (node.id == id) {
                        node.copy(
                            name = nodeName.ifBlank {
                                node.name
                            },
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

        val currentCapacity =
            connectionCurrentCapacity.toDoubleOrNull()
                ?: 0.0

        if (editingConnectionId == null) {
            val from =
                connectionStartId ?: return

            val to =
                selectedNodeId ?: return

            if (from == to) {
                connectionStartId = null
                return
            }

            val exists =
                connections.any {
                    it.fromNodeId == from &&
                        it.toNodeId == to
                }

            if (exists) {
                showConnectionDialog = false
                connectionStartId = null
                return
            }

            val newConnection =
                SldConnection(
                    id = "connection-${System.currentTimeMillis()}",
                    fromNodeId = from,
                    toNodeId = to,
                    lengthMeters = length,
                    resistanceOhmPerKm = resistance,
                    reactanceOhmPerKm = reactance,
                    cableSizeMm2 = cableSize,
                    parallelRuns = runs,
                    currentCapacityA = currentCapacity
                )

            connections =
                connections + newConnection

            selectedConnectionId = newConnection.id
            selectedNodeId = to
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
                            currentCapacityA = currentCapacity
                        )
                    } else {
                        connection
                    }
                }
        }

        invalidateStudies()
        connectionStartId = null
        showConnectionDialog = false
    }

    fun deleteSelected() {
        selectedNodeId?.let { nodeId ->
            if (nodes.size > 1) {
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
                invalidateStudies()
                return
            }
        }

        selectedConnectionId?.let { connectionId ->
            connections =
                connections.filterNot {
                    it.id == connectionId
                }

            selectedConnectionId = null
            invalidateStudies()
        }
    }

    fun connect() {
        val selected = selectedNodeId

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

        val exists =
            connections.any {
                it.fromNodeId == connectionStartId &&
                    it.toNodeId == selected
            }

        if (!exists) {
            val newConnection =
                SldConnection(
                    id = "connection-${System.currentTimeMillis()}",
                    fromNodeId = connectionStartId!!,
                    toNodeId = selected
                )

            connections =
                connections + newConnection

            selectedConnectionId = newConnection.id
            selectedNodeId = selected
            invalidateStudies()
        }

        connectionStartId = null
    }

    fun calculateUpstream() {
        if (nodes.isEmpty()) return

        calculationResult =
            try {
                SldEngineeringEngine.calculateUpstream(
                    network()
                )
            } catch (_: Exception) {
                null
            }

        showUpstreamResults =
            calculationResult != null
    }

    fun calculateShortCircuit(
        showDialog: Boolean = true
    ) {
        shortCircuitStudy =
            try {
                SldShortCircuitEngine.calculate(
                    network()
                )
            } catch (_: Exception) {
                null
            }

        if (showDialog) {
            showShortCircuitResults =
                shortCircuitStudy != null
        }
    }

    fun calculateCableSizing(
        showDialog: Boolean = true
    ) {
        if (shortCircuitStudy == null) {
            calculateShortCircuit(false)
        }

        cableSizingStudy =
            try {
                SldCableSizingEngine.calculate(
                    network(),
                    shortCircuitStudy = shortCircuitStudy
                )
            } catch (_: Exception) {
                null
            }

        cableSizingStudy?.let { study ->
            val updated =
                connections.map { connection ->
                    val result =
                        study.results[connection.id]

                    if (result != null) {
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
                    } else {
                        connection
                    }
                }

            connections = updated
        }

        if (showDialog) {
            showCableSizingResults =
                cableSizingStudy != null
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
                    network(),
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
        val selected =
            selectedNodeId ?: return

        val selectedNode =
            nodes.firstOrNull {
                it.id == selected
            } ?: return

        if (
            selectedNode.type != SldNodeType.PANEL &&
            selectedNode.type != SldNodeType.BUS &&
            selectedNode.type != SldNodeType.SOURCE
        ) {
            return
        }

        if (cableSizingStudy == null) {
            calculateCableSizing(false)
        }

        if (shortCircuitStudy == null) {
            calculateShortCircuit(false)
        }

        panelSchedule =
            try {
                SldPanelScheduleEngine.calculate(
                    network = network(),
                    panelNodeId = selected,
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
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                SmallActionButton(
                    text = "Back",
                    onClick = it
                )
            }

            SmallActionButton(
                text = "Load",
                onClick = {
                    openAdd(SldNodeType.LOAD)
                }
            )

            SmallActionButton(
                text = "Bus",
                onClick = {
                    openAdd(SldNodeType.BUS)
                }
            )

            SmallActionButton(
                text = "Transformer",
                onClick = {
                    openAdd(SldNodeType.TRANSFORMER)
                }
            )

            SmallActionButton(
                text = "Generator",
                onClick = {
                    openAdd(SldNodeType.GENERATOR)
                }
            )

            SmallActionButton(
                text = "Breaker",
                onClick = {
                    openAdd(SldNodeType.BREAKER)
                }
            )

            SmallActionButton(
                text = "Panel",
                onClick = {
                    openAdd(SldNodeType.PANEL)
                }
            )

            SmallActionButton(
                text = "Connect",
                selected = connectionStartId != null,
                onClick = ::connect
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
                    vertical = 4.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallActionButton(
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

            SmallActionButton(
                text = "Edit Feeder",
                enabled = selectedConnectionId != null,
                onClick = {
                    selectedConnectionId?.let { id ->
                        connections.firstOrNull {
                            it.id == id
                        }?.let(::openConnectionEdit)
                    }
                }
            )

            SmallActionButton(
                text = "Delete",
                enabled =
                    selectedNodeId != null ||
                        selectedConnectionId != null,
                onClick = ::deleteSelected
            )

            SmallActionButton(
                text = "Upstream",
                onClick = ::calculateUpstream
            )

            SmallActionButton(
                text = "Short Circuit",
                onClick = {
                    calculateShortCircuit(true)
                }
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
                    vertical = 4.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallActionButton(
                text = "Cable Sizing",
                onClick = {
                    calculateCableSizing(true)
                }
            )

            SmallActionButton(
                text = "Protection",
                onClick = ::calculateProtection
            )

            SmallActionButton(
                text = "Panel Schedule",
                enabled = selectedNodeId != null,
                onClick = ::calculatePanelSchedule
            )
        }

        HorizontalDivider(
            color = DarkCard2
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
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

                    invalidateStudies()
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

            SldElementsPanel(
                modifier = Modifier
                    .widthIn(
                        min = 250.dp,
                        max = 330.dp
                    )
                    .fillMaxHeight(),
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                panelSchedule = panelSchedule,
                onNodeClick = { id ->
                    selectedNodeId = id
                    selectedConnectionId = null
                },
                onConnectionClick = { id ->
                    selectedConnectionId = id
                    selectedNodeId = null
                }
            )
        }
    }

    if (showNodeDialog) {
        NodeEditorDialog(
            nodeType = pendingNodeType,
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
                connectionStartId = null
            },
            onSave = ::saveConnection
        )
    }

    if (showUpstreamResults && calculationResult != null) {
        UpstreamResultsDialog(
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

    Box(
        modifier = modifier
            .background(DarkBackground)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes) {
                    detectTapGestures(
                        onTap = { position ->
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
                    )
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
                        onDrag = { change, dragAmount ->
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
                                    30f,
                                    node.x + dragAmount.x
                                ),
                                max(
                                    50f,
                                    node.y + dragAmount.y
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

            val nodeMap =
                nodes.associateBy { it.id }

            connections.forEach { connection ->
                val from =
                    nodeMap[connection.fromNodeId]

                val to =
                    nodeMap[connection.toNodeId]

                if (from != null && to != null) {
                    drawConnection(
                        from = from,
                        to = to,
                        selected =
                            connection.id ==
                                selectedConnectionId
                    )
                }
            }

            nodes.forEach { node ->
                drawNode(
                    node = node,
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
    val nodeMap =
        nodes.associateBy { it.id }

    return connections.firstOrNull { connection ->
        val from =
            nodeMap[connection.fromNodeId]
                ?: return@firstOrNull false

        val to =
            nodeMap[connection.toNodeId]
                ?: return@firstOrNull false

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

        distanceToSegment(
            position,
            start,
            end
        ) <= 18f
    }
}

private fun drawConnection(
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

    val middleX =
        (start.x + end.x) / 2f

    val path =
        Path().apply {
            moveTo(
                start.x,
                start.y
            )

            cubicTo(
                middleX,
                start.y,
                middleX,
                end.y,
                end.x,
                end.y
            )
        }

    drawPath(
        path = path,
        color =
            if (selected) {
                Accent
            } else {
                Color(0xFF8EA0AA)
            },
        style = Stroke(
            width =
                if (selected) {
                    5f
                } else {
                    3f
                }
        )
    )

    val arrowX =
        middleX

    val arrowY =
        (start.y + end.y) / 2f

    val arrow =
        Path().apply {
            moveTo(
                arrowX,
                arrowY
            )

            lineTo(
                arrowX - 10f,
                arrowY - 7f
            )

            lineTo(
                arrowX - 10f,
                arrowY + 7f
            )

            close()
        }

    drawPath(
        path = arrow,
        color =
            if (selected) {
                Accent
            } else {
                Color(0xFF8EA0AA)
            }
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean
) {
    val nodeColor =
        when (node.type) {
            SldNodeType.SOURCE -> Color(0xFF1565C0)
            SldNodeType.BUS -> Color(0xFF455A64)
            SldNodeType.TRANSFORMER -> Color(0xFF6A1B9A)
            SldNodeType.GENERATOR -> Color(0xFF2E7D32)
            SldNodeType.BREAKER -> Color(0xFFEF6C00)
            SldNodeType.PANEL -> Color(0xFF00695C)
            SldNodeType.LOAD -> Color(0xFF37474F)
        }

    drawRoundRect(
        color = nodeColor,
        topLeft = Offset(node.x, node.y),
        size = Size(150f, 70f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            12f,
            12f
        )
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
        size = Size(
            154f,
            74f
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            14f,
            14f
        ),
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

@Composable
private fun SmallActionButton(
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
            fontSize = 12.sp,
            color =
                if (selected) {
                    Accent
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
        )
    }
}

@Composable
private fun SldElementsPanel(
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
                .fillMaxWidth()
                .weight(1f, fill = true)
                .horizontalScroll(
                    rememberScrollState()
                )
        ) {
            nodes.forEach { node ->
                Card(
                    onClick = {
                        onNodeClick(node.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 3.dp
                        ),
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
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = node.type.name,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )

                        Text(
                            text =
                                "${formatNumber(node.loadKw)} kW  |  ${formatNumber(node.voltage)} V",
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
                    onClick = {
                        onConnectionClick(
                            connection.id
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 3.dp
                        ),
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
                        modifier = Modifier.padding(9.dp)
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

                        if (
                            connection.cableSizeMm2 >
                            0.0
                        ) {
                            Text(
                                text =
                                    "${formatNumber(connection.cableSizeMm2)} mm² × ${connection.parallelRuns}",
                                color = Accent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        panelSchedule?.let { schedule ->
            Spacer(
                modifier = Modifier.height(8.dp)
            )

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
                text = schedule.panelName,
                color = Accent,
                fontSize = 12.sp
            )

            Text(
                text =
                    "${schedule.rows.size} feeders | ${formatNumber(schedule.totalDemandLoadKw)} kW",
                color = TextSecondary,
                fontSize = 10.sp
            )

            val failures =
                schedule.rows.count {
                    it.status ==
                        PanelScheduleStatus.FAIL
                }

            if (failures > 0) {
                Text(
                    text = "$failures feeder(s) require correction",
                    color = Error,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun NodeEditorDialog(
    nodeType: SldNodeType,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                EditorField(
                    label = "Name",
                    value = nodeName,
                    onValueChange = onNameChange
                )

                EditorField(
                    label = "Voltage V",
                    value = nodeVoltage,
                    onValueChange = onVoltageChange
                )

                EditorField(
                    label = "Load kW",
                    value = nodeKw,
                    onValueChange = onKwChange
                )

                EditorField(
                    label = "Power Factor",
                    value = nodePf,
                    onValueChange = onPfChange
                )

                EditorField(
                    label = "Demand Factor",
                    value = nodeDemand,
                    onValueChange = onDemandChange
                )

                EditorField(
                    label = "Rated kVA",
                    value = nodeKva,
                    onValueChange = onKvaChange
                )

                if (
                    nodeType ==
                        SldNodeType.TRANSFORMER
                ) {
                    EditorField(
                        label = "Transformer %Z",
                        value = nodeTransformerZ,
                        onValueChange =
                            onTransformerZChange
                    )
                }

                if (
                    nodeType ==
                        SldNodeType.GENERATOR
                ) {
                    EditorField(
                        label = "Generator Xd'' %",
                        value = nodeGeneratorXd,
                        onValueChange =
                            onGeneratorXdChange
                    )
                }

                if (
                    nodeType ==
                        SldNodeType.SOURCE
                ) {
                    EditorField(
                        label = "Source Short Circuit MVA",
                        value = nodeSourceMva,
                        onValueChange =
                            onSourceMvaChange
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
private fun ConnectionEditorDialog(
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
                    Arrangement.spacedBy(8.dp)
            ) {
                EditorField(
                    label = "Length m",
                    value = length,
                    onValueChange = onLengthChange
                )

                EditorField(
                    label = "R Ω/km",
                    value = resistance,
                    onValueChange = onResistanceChange
                )

                EditorField(
                    label = "X Ω/km",
                    value = reactance,
                    onValueChange = onReactanceChange
                )

                EditorField(
                    label = "Cable Size mm²",
                    value = cableSize,
                    onValueChange = onCableSizeChange
                )

                EditorField(
                    label = "Parallel Runs",
                    value = parallelRuns,
                    onValueChange = onParallelRunsChange
                )

                EditorField(
                    label = "Current Capacity A",
                    value = currentCapacity,
                    onValueChange = onCurrentCapacityChange
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
private fun UpstreamResultsDialog(
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
                    Arrangement.spacedBy(7.dp)
            ) {
                ResultLine(
                    "Connected Load",
                    "${formatNumber(result.totalConnectedLoadKw)} kW"
                )

                ResultLine(
                    "Demand Load",
                    "${formatNumber(result.totalDemandLoadKw)} kW"
                )

                ResultLine(
                    "Required kVA",
                    "${formatNumber(result.totalRequiredKva)} kVA"
                )

                ResultLine(
                    "Main Current",
                    "${formatNumber(result.mainCurrentA)} A"
                )

                ResultLine(
                    "Main Breaker",
                    "${formatNumber(result.mainBreakerA)} A"
                )

                ResultLine(
                    "Transformer",
                    "${formatNumber(result.requiredTransformerKva)} kVA"
                )

                ResultLine(
                    "Voltage Drop",
                    "${formatNumber(result.totalVoltageDropPercent)} %"
                )

                result.notes.forEach { note ->
                    Text(
                        text = "• $note",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
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
private fun ResultLine(
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
            fontWeight = FontWeight.SemiBold
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
