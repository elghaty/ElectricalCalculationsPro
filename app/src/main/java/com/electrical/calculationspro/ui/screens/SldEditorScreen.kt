package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.roundToInt

private val Background = Color(0xFF0B1116)
private val CardColor = Color(0xFF151D24)
private val CardColor2 = Color(0xFF1C2730)
private val PrimaryText = Color(0xFFF2F5F7)
private val SecondaryText = Color(0xFFAAB7C0)
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

    fun currentNetwork(): SldNetwork =
        SldNetwork(
            nodes = nodes,
            connections = connections
        )

    fun invalidateStudies() {
        calculationResult = null
        shortCircuitStudy = null
        cableSizingStudy = null
        protectionResult = null
        panelSchedule = null
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
        val loadKw = nodeKw.toDoubleOrNull() ?: 0.0
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

        invalidateStudies()
        showNodeDialog = false
    }

    fun saveConnection() {
        val length =
            (connectionLength.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)

        val resistance =
            connectionResistance.toDoubleOrNull() ?: 0.0

        val reactance =
            connectionReactance.toDoubleOrNull() ?: 0.0

        val cableSize =
            connectionCableSize.toDoubleOrNull() ?: 0.0

        val runs =
            (connectionParallelRuns.toIntOrNull() ?: 1).coerceAtLeast(1)

        val capacity =
            connectionCurrentCapacity.toDoubleOrNull() ?: 0.0

        if (editingConnectionId == null) {
            val from = connectionStartId ?: return
            val to = selectedNodeId ?: return

            if (from == to) {
                connectionStartId = null
                return
            }

            val duplicate = connections.any {
                it.fromNodeId == from && it.toNodeId == to
            }

            if (!duplicate) {
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
        val nodeId = selectedNodeId

        if (nodeId != null && nodes.size > 1) {
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
            invalidateStudies()
            return
        }

        val connectionId = selectedConnectionId

        if (connectionId != null) {
            connections = connections.filterNot {
                it.id == connectionId
            }

            selectedConnectionId = null
            invalidateStudies()
        }
    }

    fun connectNodes() {
        val selected = selectedNodeId ?: return

        if (connectionStartId == null) {
            connectionStartId = selected
            return
        }

        if (connectionStartId == selected) {
            connectionStartId = null
            return
        }

        val start = connectionStartId!!

        val duplicate = connections.any {
            it.fromNodeId == start && it.toNodeId == selected
        }

        if (!duplicate) {
            val connection = SldConnection(
                id = "connection-${System.currentTimeMillis()}",
                fromNodeId = start,
                toNodeId = selected
            )

            connections = connections + connection
            selectedConnectionId = connection.id
            invalidateStudies()
        }

        connectionStartId = null
    }

    fun calculateUpstream() {
        calculationResult = try {
            SldEngineeringEngine.calculateUpstream(
                currentNetwork()
            )
        } catch (_: Exception) {
            null
        }

        showUpstreamResults = calculationResult != null
    }

    fun calculateShortCircuit(
        openDialog: Boolean = true
    ) {
        shortCircuitStudy = try {
            SldShortCircuitEngine.calculate(
                currentNetwork()
            )
        } catch (_: Exception) {
            null
        }

        if (openDialog) {
            showShortCircuitResults = shortCircuitStudy != null
        }
    }

    fun calculateCableSizing(
        openDialog: Boolean = true
    ) {
        if (shortCircuitStudy == null) {
            calculateShortCircuit(false)
        }

        val study = try {
            SldCableSizingEngine.calculate(
                network = currentNetwork(),
                shortCircuitStudy = shortCircuitStudy
            )
        } catch (_: Exception) {
            null
        }

        cableSizingStudy = study

        if (study != null) {
            connections = connections.map { connection ->
                val result = study.results[connection.id]

                if (result != null) {
                    connection.copy(
                        cableSizeMm2 = result.recommendedSizeMm2,
                        parallelRuns = result.recommendedParallelRuns,
                        currentCapacityA = result.recommendedCurrentCapacityA,
                        voltageDropPercent =
                            result.recommendedVoltageDropPercent
                    )
                } else {
                    connection
                }
            }
        }

        if (openDialog) {
            showCableSizingResults = study != null
        }
    }

    fun calculateProtection() {
        if (shortCircuitStudy == null) {
            calculateShortCircuit(false)
        }

        if (cableSizingStudy == null) {
            calculateCableSizing(false)
        }

        protectionResult = try {
            SldProtectionCoordinationEngine.calculate(
                network = currentNetwork(),
                shortCircuitStudy = shortCircuitStudy,
                cableSizingStudy = cableSizingStudy
            )
        } catch (_: Exception) {
            null
        }

        showProtectionResults = protectionResult != null
    }

    fun calculatePanelSchedule() {
        val selected = selectedNodeId ?: return

        val selectedNode =
            nodes.firstOrNull { it.id == selected }
                ?: return

        if (
            selectedNode.type != SldNodeType.PANEL &&
            selectedNode.type != SldNodeType.BUS &&
            selectedNode.type != SldNodeType.SOURCE
        ) {
            return
        }

        if (shortCircuitStudy == null) {
            calculateShortCircuit(false)
        }

        if (cableSizingStudy == null) {
            calculateCableSizing(false)
        }

        panelSchedule = try {
            SldPanelScheduleEngine.calculate(
                network = currentNetwork(),
                panelNodeId = selected,
                cableSizingStudy = cableSizingStudy,
                shortCircuitStudy = shortCircuitStudy
            )
        } catch (_: Exception) {
            null
        }

        showPanelSchedule = panelSchedule != null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(8.dp)
    ) {
        TopBar(
            language = language,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(6.dp))

        ToolBar(
            language = language,
            onAdd = ::openAdd,
            onConnect = ::connectNodes,
            onEdit = {
                selectedNodeId?.let { id ->
                    nodes.firstOrNull { it.id == id }?.let(::openEdit)
                } ?: run {
                    selectedConnectionId?.let { id ->
                        connections.firstOrNull { it.id == id }
                            ?.let(::openConnectionEdit)
                    }
                }
            },
            onDelete = ::deleteSelected,
            onUpstream = ::calculateUpstream,
            onShortCircuit = {
                calculateShortCircuit(true)
            },
            onCableSizing = {
                calculateCableSizing(true)
            },
            onProtection = ::calculateProtection,
            onPanelSchedule = ::calculatePanelSchedule,
            panelScheduleEnabled = selectedNodeId?.let { id ->
                nodes.firstOrNull { it.id == id }?.type in
                    setOf(
                        SldNodeType.PANEL,
                        SldNodeType.BUS,
                        SldNodeType.SOURCE
                    )
            } == true
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            SldCanvas(
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                connectionStartId = connectionStartId,
                onNodeSelected = {
                    selectedNodeId = it
                    selectedConnectionId = null
                },
                onConnectionSelected = {
                    selectedConnectionId = it
                    selectedNodeId = null
                },
                onEmptySelected = {
                    selectedNodeId = null
                    selectedConnectionId = null
                    connectionStartId = null
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

                    invalidateStudies()
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            SldSidePanel(
                language = language,
                nodes = nodes,
                connections = connections,
                selectedNodeId = selectedNodeId,
                selectedConnectionId = selectedConnectionId,
                calculationResult = calculationResult,
                cableSizingStudy = cableSizingStudy,
                protectionResult = protectionResult,
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
        NodeEditorDialog(
            language = language,
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
            onSave = ::saveNode
        )
    }

    if (showConnectionDialog) {
        ConnectionEditorDialog(
            language = language,
            editing = editingConnectionId != null,
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
                connectionStartId = null
            },
            onSave = ::saveConnection
        )
    }

    calculationResult?.let { result ->
        if (showUpstreamResults) {
            UpstreamResultsDialog(
                language = language,
                result = result,
                onDismiss = {
                    showUpstreamResults = false
                }
            )
        }
    }

    shortCircuitStudy?.let { study ->
        if (showShortCircuitResults) {
            SldShortCircuitResultsDialog(
                language = language,
                study = study,
                onDismiss = {
                    showShortCircuitResults = false
                }
            )
        }
    }

    cableSizingStudy?.let { study ->
        if (showCableSizingResults) {
            SldCableSizingResultsDialog(
                language = language,
                study = study,
                onDismiss = {
                    showCableSizingResults = false
                }
            )
        }
    }

    protectionResult?.let { result ->
        if (showProtectionResults) {
            SldProtectionCoordinationResultsDialog(
                language = language,
                result = result,
                onDismiss = {
                    showProtectionResults = false
                }
            )
        }
    }

    panelSchedule?.let { schedule ->
        if (showPanelSchedule) {
            SldPanelScheduleResultsDialog(
                language = language,
                schedule = schedule,
                onDismiss = {
                    showPanelSchedule = false
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    language: String,
    onBack: (() -> Unit)?
) {
    val arabic =
        language.equals("ar", true) ||
            language.equals("arabic", true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) {
                Text(
                    if (arabic) "رجوع" else "Back",
                    color = Accent
                )
            }
        }

        Text(
            text = if (arabic) {
                "المخطط الأحادي SLD"
            } else {
                "Single Line Diagram"
            },
            color = PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ToolBar(
    language: String,
    onAdd: (SldNodeType) -> Unit,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpstream: () -> Unit,
    onShortCircuit: () -> Unit,
    onCableSizing: () -> Unit,
    onProtection: () -> Unit,
    onPanelSchedule: () -> Unit,
    panelScheduleEnabled: Boolean
) {
    val arabic =
        language.equals("ar", true) ||
            language.equals("arabic", true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallButton(
                if (arabic) "حمل" else "Load"
            ) {
                onAdd(SldNodeType.LOAD)
            }

            SmallButton(
                if (arabic) "لوحة" else "Panel"
            ) {
                onAdd(SldNodeType.PANEL)
            }

            SmallButton(
                if (arabic) "Bus" else "Bus"
            ) {
                onAdd(SldNodeType.BUS)
            }

            SmallButton(
                if (arabic) "محول" else "Transformer"
            ) {
                onAdd(SldNodeType.TRANSFORMER)
            }

            SmallButton(
                if (arabic) "مولد" else "Generator"
            ) {
                onAdd(SldNodeType.GENERATOR)
            }

            SmallButton(
                if (arabic) "قاطع" else "Breaker"
            ) {
                onAdd(SldNodeType.BREAKER)
            }

            SmallButton(
                if (arabic) "توصيل" else "Connect"
            ) {
                onConnect()
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallButton(
                if (arabic) "تعديل" else "Edit"
            ) {
                onEdit()
            }

            SmallButton(
                if (arabic) "حذف" else "Delete"
            ) {
                onDelete()
            }

            SmallButton(
                if (arabic) "Upstream" else "Upstream"
            ) {
                onUpstream()
            }

            SmallButton(
                if (arabic) "قصر" else "Short Circuit"
            ) {
                onShortCircuit()
            }

            SmallButton(
                if (arabic) "الكابلات" else "Cable Sizing"
            ) {
                onCableSizing()
            }

            SmallButton(
                if (arabic) "الحماية" else "Protection"
            ) {
                onProtection()
            }

            SmallButton(
                text = if (arabic) "جدول اللوحة" else "Panel Schedule",
                enabled = panelScheduleEnabled
            ) {
                onPanelSchedule()
            }
        }
    }
}

@Composable
private fun SmallButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.height(38.dp)
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
    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .background(
                CardColor,
                RoundedCornerShape(10.dp)
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes, connections) {
                    detectTapGestures(
                        onTap = { position ->
                            val node =
                                nodes.firstOrNull {
                                    position.x >= it.x &&
                                        position.x <= it.x + 120f &&
                                        position.y >= it.y &&
                                        position.y <= it.y + 64f
                                }

                            if (node != null) {
                                onNodeSelected(node.id)
                                return@detectTapGestures
                            }

                            val connection =
                                connections.firstOrNull { connection ->
                                    val from =
                                        nodes.firstOrNull {
                                            it.id == connection.fromNodeId
                                        }

                                    val to =
                                        nodes.firstOrNull {
                                            it.id == connection.toNodeId
                                        }

                                    if (from == null || to == null) {
                                        false
                                    } else {
                                        val start = Offset(
                                            from.x + 120f,
                                            from.y + 32f
                                        )

                                        val end = Offset(
                                            to.x,
                                            to.y + 32f
                                        )

                                        distanceToSegment(
                                            position,
                                            start,
                                            end
                                        ) <= 14f
                                    }
                                }

                            if (connection != null) {
                                onConnectionSelected(connection.id)
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
                                nodes.firstOrNull {
                                    position.x >= it.x &&
                                        position.x <= it.x + 120f &&
                                        position.y >= it.y &&
                                        position.y <= it.y + 64f
                                }?.id
                        },
                        onDrag = { change, dragAmount ->
                            val id = draggedNodeId

                            if (id != null) {
                                val node =
                                    nodes.firstOrNull {
                                        it.id == id
                                    }

                                if (node != null) {
                                    val newX =
                                        (node.x + dragAmount.x)
                                            .coerceAtLeast(10f)

                                    val newY =
                                        (node.y + dragAmount.y)
                                            .coerceAtLeast(10f)

                                    onNodeMoved(
                                        id,
                                        newX,
                                        newY
                                    )
                                }

                                change.consume()
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
            drawGrid()

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
                    val start = Offset(
                        from.x + 120f,
                        from.y + 32f
                    )

                    val end = Offset(
                        to.x,
                        to.y + 32f
                    )

                    val selected =
                        connection.id == selectedConnectionId

                    drawLine(
                        color =
                            if (selected) Accent
                            else SecondaryText,
                        start = start,
                        end = end,
                        strokeWidth =
                            if (selected) 5f else 3f
                    )

                    drawArrow(
                        start = start,
                        end = end,
                        color =
                            if (selected) Accent
                            else SecondaryText
                    )
                }
            }

            nodes.forEach { node ->
                val selected =
                    node.id == selectedNodeId

                val connecting =
                    node.id == connectionStartId

                drawNode(
                    node = node,
                    selected = selected,
                    connecting = connecting
                )
            }
        }

        if (nodes.isEmpty()) {
            Text(
                text = "SLD",
                color = SecondaryText,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid() {
    val step = 40f

    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = Color(0xFF18242C),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += step
    }

    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = Color(0xFF18242C),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += step
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connecting: Boolean
) {
    val fill =
        when (node.type) {
            SldNodeType.SOURCE -> Color(0xFF263238)
            SldNodeType.TRANSFORMER -> Color(0xFF263A46)
            SldNodeType.GENERATOR -> Color(0xFF30442E)
            SldNodeType.BUS -> Color(0xFF3A303F)
            SldNodeType.PANEL -> Color(0xFF263E48)
            SldNodeType.BREAKER -> Color(0xFF403A2D)
            SldNodeType.LOAD -> Color(0xFF303840)
        }

    val border =
        when {
            connecting -> Accent
            selected -> Color.White
            else -> Color(0xFF607D8B)
        }

    drawRoundRect(
        color = fill,
        topLeft = Offset(node.x, node.y),
        size = androidx.compose.ui.geometry.Size(
            120f,
            64f
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            10f,
            10f
        )
    )

    drawRoundRect(
        color = border,
        topLeft = Offset(node.x, node.y),
        size = androidx.compose.ui.geometry.Size(
            120f,
            64f
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            10f,
            10f
        ),
        style = Stroke(
            width = if (selected || connecting) 3f else 1.5f
        )
    )

    val label = node.name.take(17)

    drawContext.canvas.nativeCanvas.apply {
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = android.graphics.Color.WHITE
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

        drawText(
            label,
            node.x + 8f,
            node.y + 24f,
            paint
        )

        paint.textSize = 10f
        paint.typeface = android.graphics.Typeface.DEFAULT

        drawText(
            node.type.name,
            node.x + 8f,
            node.y + 44f,
            paint
        )

        if (node.loadKw > 0.0) {
            drawText(
                "${formatNumber(node.loadKw)} kW",
                node.x + 72f,
                node.y + 44f,
                paint
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = hypot(dx, dy)

    if (length < 1f) return

    val ux = dx / length
    val uy = dy / length

    val px = -uy
    val py = ux

    val tip = Offset(
        start.x + dx * 0.72f,
        start.y + dy * 0.72f
    )

    val left = Offset(
        tip.x - ux * 12f + px * 6f,
        tip.y - uy * 12f + py * 6f
    )

    val right = Offset(
        tip.x - ux * 12f - px * 6f,
        tip.y - uy * 12f - py * 6f
    )

    val path = Path()

    path.moveTo(tip.x, tip.y)
    path.lineTo(left.x, left.y)
    path.lineTo(right.x, right.y)
    path.close()

    drawPath(
        path = path,
        color = color
    )
}

@Composable
private fun SldSidePanel(
    language: String,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    calculationResult: SldCalculationResult?,
    cableSizingStudy: SldCableSizingStudy?,
    protectionResult: SldProtectionCoordinationResult?,
    onNodeClick: (String) -> Unit,
    onConnectionClick: (String) -> Unit
) {
    val arabic =
        language.equals("ar", true) ||
            language.equals("arabic", true)

    Column(
        modifier = Modifier
            .widthIn(min = 220.dp, max = 300.dp)
            .fillMaxHeight()
            .background(
                CardColor,
                RoundedCornerShape(10.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            text = if (arabic) "عناصر المخطط" else "SLD Elements",
            color = PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            nodes.forEach { node ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp)
                        .clickable {
                            onNodeClick(node.id)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (node.id == selectedNodeId) {
                                Color(0xFF164A55)
                            } else {
                                CardColor2
                            }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = node.name,
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "${node.type.name} • " +
                                    "${formatNumber(node.voltage)} V",
                            color = SecondaryText,
                            fontSize = 10.sp
                        )

                        if (node.loadKw > 0.0) {
                            Text(
                                text =
                                    "${formatNumber(node.loadKw)} kW",
                                color = Accent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = CardColor2,
                modifier = Modifier.padding(vertical = 5.dp)
            )

            connections.forEach { connection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .clickable {
                            onConnectionClick(connection.id)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (
                                connection.id ==
                                selectedConnectionId
                            ) {
                                Color(0xFF164A55)
                            } else {
                                CardColor2
                            }
                    )
                ) {
                    Text(
                        text =
                            "${connection.fromNodeId} → " +
                                connection.toNodeId,
                        color = PrimaryText,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        calculationResult?.let {
            SummaryCard(
                title = if (arabic) "Upstream" else "Upstream",
                value =
                    "${formatNumber(it.mainCurrentA)} A"
            )
        }

        cableSizingStudy?.let {
            SummaryCard(
                title =
                    if (arabic) "الكابلات" else "Cable Sizing",
                value = "${it.results.size}"
            )
        }

        protectionResult?.let {
            SummaryCard(
                title =
                    if (arabic) "الحماية" else "Protection",
                value =
                    "${it.failedPairs} FAIL"
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor2
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = SecondaryText,
                fontSize = 10.sp
            )

            Text(
                text = value,
                color = Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NodeEditorDialog(
    language: String,
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
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val arabic =
        language.equals("ar", true) ||
            language.equals("arabic", true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText,
        title = {
            Text(
                if (editing) {
                    if (arabic) "تعديل العنصر" else "Edit Element"
                } else {
                    if (arabic) "إضافة عنصر" else "Add Element"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
            ) {
                EditorField(
                    if (arabic) "الاسم" else "Name",
                    name,
                    onNameChange
                )

                EditorField(
                    if (arabic) "الجهد V" else "Voltage V",
                    voltage,
                    onVoltageChange
                )

                EditorField(
                    if (arabic) "الحمل kW" else "Load kW",
                    kw,
                    onKwChange
                )

                EditorField(
                    if (arabic) "معامل القدرة" else "Power Factor",
                    pf,
                    onPfChange
                )

                EditorField(
                    if (arabic) "معامل الطلب" else "Demand Factor",
                    demand,
                    onDemandChange
                )

                EditorField(
                    if (arabic) "القدرة kVA" else "Rated kVA",
                    kva,
                    onKvaChange
                )

                if (type == SldNodeType.TRANSFORMER) {
                    EditorField(
                        if (arabic) "%Z للمحول" else "Transformer %Z",
                        transformerZ,
                        onTransformerZChange
                    )
                }

                if (type == SldNodeType.GENERATOR) {
                    EditorField(
                        if (arabic) "Xd'' %" else "Xd'' %",
                        generatorXd,
                        onGeneratorXdChange
                    )
                }

                if (type == SldNodeType.SOURCE) {
                    EditorField(
                        if (arabic) "Scc MVA" else "Source Scc MVA",
                        sourceMva,
                        onSourceMvaChange
                    )
                }
            }
        },
        confirmButton = {
            Button(
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
private fun ConnectionEditorDialog(
    language: String,
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
    val arabic =
        language.equals("ar", true) ||
            language.equals("arabic", true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText,
        title = {
            Text(
                if (editing) {
                    if (arabic) "تعديل المغذي" else "Edit Feeder"
                } else {
                    if (arabic) "بيانات المغذي" else "Feeder Data"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                EditorField(
                    if (arabic) "الطول m" else "Length m",
                    length,
                    onLengthChange
                )

                EditorField(
                    if (arabic) "R Ω/km" else "R Ω/km",
                    resistance,
                    onResistanceChange
                )

                EditorField(
                    if (arabic) "X Ω/km" else "X Ω/km",
                    reactance,
                    onReactanceChange
                )

                EditorField(
                    if (arabic) "مقطع الكابل mm²" else "Cable mm²",
                    cableSize,
                    onCableSizeChange
                )

                EditorField(
                    if (arabic) "عدد المسارات" else "Parallel Runs",
                    parallelRuns,
                    onParallelRunsChange
                )

                EditorField(
                    if (arabic) "سعة التيار A" else "Current Capacity A",
                    currentCapacity,
                    onCurrentCapacityChange
                )
            }
        },
        confirmButton = {
            Button(
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
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    )
}

@Composable
private fun UpstreamResultsDialog(
    language: String,
    result: SldCalculationResult,
    onDismiss: () -> Unit
) {
    val arabic =
        language.equals("ar", true) ||
            language.equals("arabic", true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText,
        title = {
            Text(
                if (arabic) {
                    "نتائج حسابات Upstream"
                } else {
                    "Upstream Calculation"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ResultLine(
                    if (arabic) "الحمل المتصل" else "Connected Load",
                    "${formatNumber(result.totalConnectedLoadKw)} kW"
                )

                ResultLine(
                    if (arabic) "حمل الطلب" else "Demand Load",
                    "${formatNumber(result.totalDemandLoadKw)} kW"
                )

                ResultLine(
                    if (arabic) "kVA المطلوب" else "Required kVA",
                    "${formatNumber(result.totalRequiredKva)} kVA"
                )

                ResultLine(
                    if (arabic) "التيار الرئيسي" else "Main Current",
                    "${formatNumber(result.mainCurrentA)} A"
                )

                ResultLine(
                    if (arabic) "القاطع الرئيسي" else "Main Breaker",
                    "${formatNumber(result.mainBreakerA)} A"
                )

                ResultLine(
                    if (arabic) "المحول المطلوب" else "Transformer",
                    "${formatNumber(result.requiredTransformerKva)} kVA"
                )

                ResultLine(
                    if (arabic) "هبوط الجهد" else "Voltage Drop",
                    "${formatNumber(result.totalVoltageDropPercent)} %"
                )

                if (result.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    result.notes.forEach {
                        Text(
                            text = "• $it",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (arabic) "إغلاق" else "Close",
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = SecondaryText,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = PrimaryText,
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
        return hypot(
            point.x - start.x,
            point.y - start.y
        )
    }

    val t =
        (
            ((point.x - start.x) * dx) +
                ((point.y - start.y) * dy)
            ) / (dx * dx + dy * dy)

    val clamped = t.coerceIn(0f, 1f)

    val closest = Offset(
        start.x + clamped * dx,
        start.y + clamped * dy
    )

    return hypot(
        point.x - closest.x,
        point.y - closest.y
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
