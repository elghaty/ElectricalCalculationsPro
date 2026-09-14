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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
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
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {
    val languageCode =
        if (language == AppLanguage.ARABIC) "ar" else "en"

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

    fun calculateShortCircuit(openDialog: Boolean = true) {
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

    fun calculateCableSizing(openDialog: Boolean = true) {
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
            language = languageCode,
            onBack = onBack
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        ToolBar(
            language = languageCode,
            onAdd = ::openAdd,
            onConnect = ::connectNodes,
            onEdit = {
                selectedNodeId?.let { id ->
                    nodes.firstOrNull { it.id == id }
                        ?.let(::openEdit)
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

        Spacer(
            modifier = Modifier.height(6.dp)
        )

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

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            SldSidePanel(
                language = languageCode,
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
            language = languageCode,
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
            language = languageCode,
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
                language = languageCode,
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
                language = languageCode,
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
                language = languageCode,
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
                language = languageCode,
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
                language = languageCode,
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
            TextButton(
                onClick = onBack
            ) {
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
                "Bus"
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
                if (arabic) "كابلات" else "Cable Sizing"
            ) {
                onCableSizing()
            }

            SmallButton(
                if (arabic) "تنسيق حماية" else "Protection"
            ) {
                onProtection()
            }

            SmallButton(
                if (arabic) "جدول لوحة" else "Panel Schedule"
            ) {
                if (panelScheduleEnabled) {
                    onPanelSchedule()
                }
            }
        }
    }
}

@Composable
private fun SmallButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
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
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(
                Color(0xFF0A1015),
                RoundedCornerShape(12.dp)
            )
    ) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    nodes,
                    connections
                ) {
                    detectTapGestures(
                        onTap = { position ->

                            val node =
                                nodes.asReversed()
                                    .firstOrNull { node ->
                                        position.x >= node.x &&
                                            position.x <= node.x + 120f &&
                                            position.y >= node.y &&
                                            position.y <= node.y + 64f
                                    }

                            if (node != null) {
                                onNodeSelected(node.id)
                                return@detectTapGestures
                            }

                            val connection =
                                findConnectionAtPoint(
                                    position,
                                    nodes,
                                    connections
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

                            val node =
                                nodes.asReversed()
                                    .firstOrNull { candidate ->
                                        position.x >= candidate.x &&
                                            position.x <= candidate.x + 120f &&
                                            position.y >= candidate.y &&
                                            position.y <= candidate.y + 64f
                                    }

                            if (node != null) {
                                onNodeSelected(node.id)
                            }
                        },
                        onDrag = { change, dragAmount ->

                            val id = selectedNodeId
                                ?: return@detectDragGestures

                            val node =
                                nodes.firstOrNull {
                                    it.id == id
                                }
                                    ?: return@detectDragGestures

                            change.consume()

                            onNodeMoved(
                                id,
                                (node.x + dragAmount.x)
                                    .coerceAtLeast(0f),
                                (node.y + dragAmount.y)
                                    .coerceAtLeast(0f)
                            )
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
                        connection.id ==
                            selectedConnectionId

                    drawLine(
                        color = if (selected) {
                            Accent
                        } else {
                            Color(0xFF78909C)
                        },
                        start = start,
                        end = end,
                        strokeWidth = if (selected) {
                            4f
                        } else {
                            2f
                        }
                    )

                    drawArrow(
                        start = start,
                        end = end,
                        color = if (selected) {
                            Accent
                        } else {
                            Color(0xFF78909C)
                        }
                    )
                }
            }

            nodes.forEach { node ->

                drawNode(
                    node = node,
                    selected =
                        node.id == selectedNodeId,
                    connecting =
                        node.id == connectionStartId
                )

                val label =
                    node.name.take(17)

                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(
                        node.x + 8f,
                        node.y + 7f
                    ),
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = node.type.name,
                    topLeft = Offset(
                        node.x + 8f,
                        node.y + 29f
                    ),
                    style = androidx.compose.ui.text.TextStyle(
                        color = SecondaryText,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )

                if (node.loadKw > 0.0) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text =
                            "${formatNumber(node.loadKw)} kW",
                        topLeft = Offset(
                            node.x + 8f,
                            node.y + 47f
                        ),
                        style =
                            androidx.compose.ui.text.TextStyle(
                                color = Accent,
                                fontSize = 9.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            ),
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = "SLD",
            color = SecondaryText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            fontSize = 11.sp
        )
    }
}

private fun DrawScope.drawGrid() {
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

private fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connecting: Boolean
) {
    val fill =
        when (node.type) {
            SldNodeType.SOURCE ->
                Color(0xFF263238)

            SldNodeType.TRANSFORMER ->
                Color(0xFF263A46)

            SldNodeType.GENERATOR ->
                Color(0xFF30442E)

            SldNodeType.BUS ->
                Color(0xFF3A303F)

            SldNodeType.PANEL ->
                Color(0xFF263E48)

            SldNodeType.BREAKER ->
                Color(0xFF403A2D)

            SldNodeType.LOAD ->
                Color(0xFF303840)
        }

    val border =
        when {
            connecting -> Accent
            selected -> Color.White
            else -> Color(0xFF607D8B)
        }

    drawRoundRect(
        color = fill,
        topLeft = Offset(
            node.x,
            node.y
        ),
        size = androidx.compose.ui.geometry.Size(
            120f,
            64f
        ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                10f,
                10f
            )
    )

    drawRoundRect(
        color = border,
        topLeft = Offset(
            node.x,
            node.y
        ),
        size = androidx.compose.ui.geometry.Size(
            120f,
            64f
        ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                10f,
                10f
            ),
        style = Stroke(
            width =
                if (selected || connecting) {
                    3f
                } else {
                    1.5f
                }
        )
    )
}

private fun DrawScope.drawArrow(
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

    val size = 8f

    val path = Path().apply {
        moveTo(
            tip.x,
            tip.y
        )

        lineTo(
            tip.x - ux * size + px * size * 0.55f,
            tip.y - uy * size + py * size * 0.55f
        )

        lineTo(
            tip.x - ux * size - px * size * 0.55f,
            tip.y - uy * size - py * size * 0.55f
        )

        close()
    }

    drawPath(
        path = path,
        color = color
    )
}

private fun findConnectionAtPoint(
    point: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {

    return connections.firstOrNull { connection ->

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

            distancePointToSegment(
                point,
                start,
                end
            ) <= 14f
        }
    }
}

private fun distancePointToSegment(
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
            (point.x - start.x) * dx +
                (point.y - start.y) * dy
            ) /
            (dx * dx + dy * dy)

    val clamped =
        t.coerceIn(0f, 1f)

    val projection = Offset(
        start.x + clamped * dx,
        start.y + clamped * dy
    )

    return hypot(
        point.x - projection.x,
        point.y - projection.y
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

    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            Text(
                text = if (arabic) {
                    "شبكة SLD"
                } else {
                    "SLD Network"
                },
                color = PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "${nodes.size} nodes • ${connections.size} feeders",
                color = SecondaryText,
                fontSize = 12.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            HorizontalDivider(
                color = CardColor2
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                nodes.forEach { node ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                onNodeClick(node.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (node.id == selectedNodeId) {
                                    Accent.copy(alpha = 0.16f)
                                } else {
                                    CardColor2
                                }
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(9.dp)
                        ) {

                            Text(
                                text = node.name,
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Text(
                                text =
                                    "${node.type.name} • ${formatNumber(node.voltage)} V",
                                color = SecondaryText,
                                fontSize = 11.sp
                            )

                            if (node.loadKw > 0.0) {
                                Text(
                                    text =
                                        "${formatNumber(node.loadKw)} kW",
                                    color = Accent,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            calculationResult?.let { result ->

                HorizontalDivider(
                    color = CardColor2
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        if (arabic) "ملخص الحساب" else "Calculation Summary",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )

                SummarySideRow(
                    "Load",
                    "${formatNumber(result.totalConnectedLoadKw)} kW"
                )

                SummarySideRow(
                    "Demand",
                    "${formatNumber(result.totalDemandLoadKw)} kW"
                )

                SummarySideRow(
                    "Main Current",
                    "${formatNumber(result.mainCurrentA)} A"
                )

                SummarySideRow(
                    "Main Breaker",
                    "${formatNumber(result.mainBreakerA)} A"
                )

                SummarySideRow(
                    "Transformer",
                    "${formatNumber(result.requiredTransformerKva)} kVA"
                )
            }

            cableSizingStudy?.let { study ->

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                SummarySideRow(
                    "Cable Results",
                    study.results.size.toString()
                )
            }

            protectionResult?.let { result ->

                SummarySideRow(
                    "Protection PASS",
                    result.coordinatedPairs.toString()
                )

                SummarySideRow(
                    "Protection WARN",
                    result.warningPairs.toString()
                )

                SummarySideRow(
                    "Protection FAIL",
                    result.failedPairs.toString()
                )
            }
        }
    }
}

@Composable
private fun SummarySideRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = SecondaryText,
            fontSize = 11.sp
        )

        Text(
            text = value,
            color = PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
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
        title = {
            Text(
                if (editing) {
                    if (arabic) "تعديل العنصر" else "Edit Node"
                } else {
                    if (arabic) "إضافة عنصر" else "Add Node"
                }
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                TextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text(
                            if (arabic) "الاسم" else "Name"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = voltage,
                    onValueChange = onVoltageChange,
                    label = {
                        Text(
                            if (arabic) "الجهد V" else "Voltage V"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = kw,
                    onValueChange = onKwChange,
                    label = {
                        Text(
                            if (arabic) "الحمل kW" else "Load kW"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = pf,
                    onValueChange = onPfChange,
                    label = {
                        Text(
                            if (arabic) "معامل القدرة" else "Power Factor"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = demand,
                    onValueChange = onDemandChange,
                    label = {
                        Text(
                            if (arabic) "معامل الطلب" else "Demand Factor"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = kva,
                    onValueChange = onKvaChange,
                    label = {
                        Text(
                            if (arabic) "القدرة kVA" else "Rated kVA"
                        )
                    },
                    singleLine = true
                )

                if (type == SldNodeType.TRANSFORMER) {
                    TextField(
                        value = transformerZ,
                        onValueChange = onTransformerZChange,
                        label = {
                            Text(
                                if (arabic) "%Z المحول" else "Transformer %Z"
                            )
                        },
                        singleLine = true
                    )
                }

                if (type == SldNodeType.GENERATOR) {
                    TextField(
                        value = generatorXd,
                        onValueChange = onGeneratorXdChange,
                        label = {
                            Text(
                                if (arabic) "Xd'' %" else "Xd'' %"
                            )
                        },
                        singleLine = true
                    )
                }

                if (type == SldNodeType.SOURCE) {
                    TextField(
                        value = sourceMva,
                        onValueChange = onSourceMvaChange,
                        label = {
                            Text(
                                if (arabic) "Scc MVA" else "Source Scc MVA"
                            )
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
        title = {
            Text(
                if (editing) {
                    if (arabic) "تعديل المغذي" else "Edit Feeder"
                } else {
                    if (arabic) "إضافة مغذي" else "Add Feeder"
                }
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                TextField(
                    value = length,
                    onValueChange = onLengthChange,
                    label = {
                        Text(
                            if (arabic) "الطول m" else "Length m"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = resistance,
                    onValueChange = onResistanceChange,
                    label = {
                        Text(
                            if (arabic) "R Ω/km" else "R Ω/km"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = reactance,
                    onValueChange = onReactanceChange,
                    label = {
                        Text(
                            if (arabic) "X Ω/km" else "X Ω/km"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = cableSize,
                    onValueChange = onCableSizeChange,
                    label = {
                        Text(
                            if (arabic) "الكابل mm²" else "Cable mm²"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = parallelRuns,
                    onValueChange = onParallelRunsChange,
                    label = {
                        Text(
                            if (arabic) "عدد المسارات" else "Parallel Runs"
                        )
                    },
                    singleLine = true
                )

                TextField(
                    value = currentCapacity,
                    onValueChange = onCurrentCapacityChange,
                    label = {
                        Text(
                            if (arabic) "السعة A" else "Capacity A"
                        )
                    },
                    singleLine = true
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
        title = {
            Text(
                if (arabic) {
                    "نتائج الحسابات Upstream"
                } else {
                    "Upstream Results"
                }
            )
        },
        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                SummaryDialogRow(
                    if (arabic) "الحمل المتصل" else "Connected Load",
                    "${formatNumber(result.totalConnectedLoadKw)} kW"
                )

                SummaryDialogRow(
                    if (arabic) "حمل الطلب" else "Demand Load",
                    "${formatNumber(result.totalDemandLoadKw)} kW"
                )

                SummaryDialogRow(
                    if (arabic) "kVA المطلوب" else "Required kVA",
                    "${formatNumber(result.totalRequiredKva)} kVA"
                )

                SummaryDialogRow(
                    if (arabic) "التيار الرئيسي" else "Main Current",
                    "${formatNumber(result.mainCurrentA)} A"
                )

                SummaryDialogRow(
                    if (arabic) "القاطع الرئيسي" else "Main Breaker",
                    "${formatNumber(result.mainBreakerA)} A"
                )

                SummaryDialogRow(
                    if (arabic) "المحول" else "Transformer",
                    "${formatNumber(result.requiredTransformerKva)} kVA"
                )

                SummaryDialogRow(
                    if (arabic) "هبوط الجهد" else "Voltage Drop",
                    "${formatNumber(result.totalVoltageDropPercent)} %"
                )

                if (result.notes.isNotEmpty()) {

                    HorizontalDivider()

                    result.notes.forEach { note ->

                        Text(
                            text = "• $note",
                            color = SecondaryText,
                            fontSize = 12.sp
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
private fun SummaryDialogRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth()
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
            fontWeight = FontWeight.SemiBold
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
