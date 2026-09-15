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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldShortCircuitEngine
import com.electrical.calculationspro.data.SldShortCircuitStudy
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private val Background = Color(0xFF070C10)
private val CanvasBackground = Color(0xFF09131A)
private val TextPrimary = Color(0xFFF2F5F7)
private val TextSecondary = Color(0xFF9BA8B2)
private val Accent = Color(0xFF00BCD4)
private val SourceColor = Color(0xFF1976D2)
private val TransformerColor = Color(0xFFFF8F00)
private val GeneratorColor = Color(0xFF43A047)
private val BreakerColor = Color(0xFF607D8B)
private val BusColor = Color(0xFF9C27B0)
private val PanelColor = Color(0xFF00838F)
private val LoadColor = Color(0xFF455A64)
private val CableColor = Color(0xFFB0BEC5)
private val SelectedColor = Color(0xFF00E676)

private const val NODE_WIDTH = 170f
private const val NODE_HEIGHT = 112f
private const val TOUCH_PADDING = 35f

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {
    val arabic = language == AppLanguage.ARABIC

    var nodes by remember {
        mutableStateOf(
            listOf(
                SldNode(
                    id = "source-1",
                    name = "MAIN SOURCE",
                    type = SldNodeType.SOURCE,
                    x = 80f,
                    y = 280f,
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

    var editingNodeId by remember {
        mutableStateOf<String?>(null)
    }

    var editingConnectionId by remember {
        mutableStateOf<String?>(null)
    }

    var showNodeDialog by remember {
        mutableStateOf(false)
    }

    var showConnectionDialog by remember {
        mutableStateOf(false)
    }

    var showReport by remember {
        mutableStateOf(false)
    }

    var reportTitle by remember {
        mutableStateOf("")
    }

    var reportText by remember {
        mutableStateOf("")
    }

    var nodeType by remember {
        mutableStateOf(SldNodeType.LOAD)
    }

    var name by remember { mutableStateOf("") }
    var voltage by remember { mutableStateOf("400") }
    var loadKw by remember { mutableStateOf("50") }
    var pf by remember { mutableStateOf("0.90") }
    var demand by remember { mutableStateOf("1.0") }
    var kva by remember { mutableStateOf("1000") }
    var transformerZ by remember { mutableStateOf("6") }
    var generatorXd by remember { mutableStateOf("15") }
    var sourceMva by remember { mutableStateOf("500") }

    var length by remember { mutableStateOf("10") }
    var resistance by remember { mutableStateOf("0.125") }
    var reactance by remember { mutableStateOf("0.080") }
    var cableSize by remember { mutableStateOf("0") }
    var parallelRuns by remember { mutableStateOf("1") }
    var capacity by remember { mutableStateOf("0") }

    fun network(): SldNetwork {
        return SldNetwork(
            nodes = nodes,
            connections = connections
        )
    }

    fun typeLabel(type: SldNodeType): String {
        return when (type) {
            SldNodeType.SOURCE ->
                if (arabic) "مصدر" else "Source"

            SldNodeType.TRANSFORMER ->
                if (arabic) "محول" else "Transformer"

            SldNodeType.GENERATOR ->
                if (arabic) "مولد" else "Generator"

            SldNodeType.BUS ->
                if (arabic) "باسبار" else "Busbar"

            SldNodeType.BREAKER ->
                if (arabic) "قاطع" else "Breaker"

            SldNodeType.PANEL ->
                if (arabic) "لوحة" else "Panel"

            SldNodeType.LOAD ->
                if (arabic) "حمل" else "Load"
        }
    }

    fun nextName(type: SldNodeType): String {
        val prefix = when (type) {
            SldNodeType.SOURCE -> "SOURCE"
            SldNodeType.TRANSFORMER -> "TR"
            SldNodeType.GENERATOR -> "GEN"
            SldNodeType.BUS -> "BUS"
            SldNodeType.BREAKER -> "CB"
            SldNodeType.PANEL -> "PANEL"
            SldNodeType.LOAD -> "LOAD"
        }

        val number =
            nodes.count { it.type == type } + 1

        return "$prefix-$number"
    }

    fun openAdd(type: SldNodeType) {
        editingNodeId = null
        nodeType = type
        name = nextName(type)
        voltage = "400"
        loadKw = "50"
        pf = "0.90"
        demand = "1.0"
        kva = "1000"
        transformerZ = "6"
        generatorXd = "15"
        sourceMva = "500"

        showNodeDialog = true
    }

    fun openEdit(node: SldNode) {
        editingNodeId = node.id
        nodeType = node.type

        name = node.name
        voltage = node.voltage.toString()
        loadKw = node.loadKw.toString()
        pf = node.powerFactor.toString()
        demand = node.demandFactor.toString()
        kva = node.ratedKva.toString()
        transformerZ = node.transformerPercentZ.toString()
        generatorXd = node.generatorXdSubtransient.toString()
        sourceMva = node.sourceShortCircuitMva.toString()

        showNodeDialog = true
    }

    fun saveNode() {
        val v =
            voltage.toDoubleOrNull()
                ?.coerceAtLeast(1.0)
                ?: 400.0

        val kw =
            loadKw.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val powerFactor =
            pf.toDoubleOrNull()
                ?.coerceIn(0.01, 1.0)
                ?: 0.9

        val demandFactor =
            demand.toDoubleOrNull()
                ?.coerceIn(0.0, 1.0)
                ?: 1.0

        val rating =
            kva.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val z =
            transformerZ.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val xd =
            generatorXd.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val mva =
            sourceMva.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        if (editingNodeId == null) {
            val index = nodes.size

            val newNode = SldNode(
                id = "node-${System.nanoTime()}",
                name = name.ifBlank {
                    nextName(nodeType)
                },
                type = nodeType,
                x = 80f + index * 210f,
                y = 280f,
                voltage = v,
                loadKw = kw,
                powerFactor = powerFactor,
                demandFactor = demandFactor,
                ratedKva = rating,
                transformerPercentZ = z,
                generatorXdSubtransient = xd,
                sourceShortCircuitMva = mva
            )

            nodes = nodes + newNode
            selectedNodeId = newNode.id
            selectedConnectionId = null
        } else {
            nodes = nodes.map { old ->
                if (old.id == editingNodeId) {
                    old.copy(
                        name = name.ifBlank { old.name },
                        voltage = v,
                        loadKw = kw,
                        powerFactor = powerFactor,
                        demandFactor = demandFactor,
                        ratedKva = rating,
                        transformerPercentZ = z,
                        generatorXdSubtransient = xd,
                        sourceShortCircuitMva = mva
                    )
                } else {
                    old
                }
            }
        }

        showNodeDialog = false
    }

    fun openConnection(connection: SldConnection) {
        editingConnectionId = connection.id
        length = connection.lengthMeters.toString()
        resistance =
            connection.resistanceOhmPerKm.toString()
        reactance =
            connection.reactanceOhmPerKm.toString()
        cableSize =
            connection.cableSizeMm2.toString()
        parallelRuns =
            connection.parallelRuns.toString()
        capacity =
            connection.currentCapacityA.toString()

        showConnectionDialog = true
    }

    fun saveConnection() {
        val l =
            length.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 10.0

        val r =
            resistance.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.125

        val x =
            reactance.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.080

        val size =
            cableSize.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        val runs =
            parallelRuns.toIntOrNull()
                ?.coerceAtLeast(1)
                ?: 1

        val ampacity =
            capacity.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0

        if (editingConnectionId != null) {
            connections = connections.map {
                if (it.id == editingConnectionId) {
                    it.copy(
                        lengthMeters = l,
                        resistanceOhmPerKm = r,
                        reactanceOhmPerKm = x,
                        cableSizeMm2 = size,
                        parallelRuns = runs,
                        currentCapacityA = ampacity
                    )
                } else {
                    it
                }
            }
        }

        showConnectionDialog = false
    }

    fun connect() {
        val selected = selectedNodeId ?: return

        if (connectionStartId == null) {
            connectionStartId = selected
            return
        }

        val from = connectionStartId!!

        if (from == selected) {
            connectionStartId = null
            return
        }

        val exists = connections.any {
            (
                it.fromNodeId == from &&
                    it.toNodeId == selected
                ) ||
                (
                    it.fromNodeId == selected &&
                        it.toNodeId == from
                    )
        }

        if (!exists) {
            val connection = SldConnection(
                id = "connection-${System.nanoTime()}",
                fromNodeId = from,
                toNodeId = selected,
                lengthMeters = 10.0,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 0.0,
                parallelRuns = 1,
                currentCapacityA = 0.0
            )

            connections =
                connections + connection

            selectedConnectionId =
                connection.id
        }

        connectionStartId = null
    }

    fun deleteSelected() {
        val node = selectedNodeId

        if (
            node != null &&
            node != "source-1"
        ) {
            nodes =
                nodes.filterNot {
                    it.id == node
                }

            connections =
                connections.filter {
                    it.fromNodeId != node &&
                        it.toNodeId != node
                }

            selectedNodeId = null
            selectedConnectionId = null
            connectionStartId = null

            return
        }

        val connection =
            selectedConnectionId

        if (connection != null) {
            connections =
                connections.filterNot {
                    it.id == connection
                }

            selectedConnectionId = null
        }
    }

    fun shortCircuitReport() {
        try {
            val study =
                SldShortCircuitEngine.calculate(
                    network = network(),
                    voltageFactor = 1.05
                )

            reportTitle =
                if (arabic) {
                    "تقرير تيارات القصر"
                } else {
                    "SHORT CIRCUIT ENGINEERING REPORT"
                }

            reportText =
                buildShortCircuitReport(
                    study = study,
                    nodes = nodes,
                    arabic = arabic
                )

            showReport = true
        } catch (e: Exception) {
            reportTitle =
                if (arabic) "خطأ في الحساب"
                else "CALCULATION ERROR"

            reportText =
                e.message
                    ?: "Short circuit calculation failed."

            showReport = true
        }
    }

    fun panelSchedule() {
        val panel =
            nodes.firstOrNull {
                it.id == selectedNodeId &&
                    it.type == SldNodeType.PANEL
            }

        if (panel == null) {
            reportTitle =
                if (arabic)
                    "جدول اللوحة"
                else
                    "PANEL SCHEDULE"

            reportText =
                if (arabic)
                    "اختر لوحة أولاً."
                else
                    "Select a PANEL first."

            showReport = true
            return
        }

        reportTitle =
            "${panel.name} - PANEL SCHEDULE"

        reportText =
            buildPanelSchedule(
                panel = panel,
                nodes = nodes,
                connections = connections
            )

        showReport = true
    }

    fun generateCompleteSld() {
        val source =
            nodes.firstOrNull {
                it.type == SldNodeType.SOURCE
            }
                ?: SldNode(
                    id = "source-1",
                    name = "MAIN SOURCE",
                    type = SldNodeType.SOURCE,
                    x = 80f,
                    y = 280f,
                    voltage = 400.0,
                    sourceShortCircuitMva = 500.0
                )

        val v =
            source.voltage
                .coerceAtLeast(1.0)

        val sourceNode =
            source.copy(
                id = "source-1",
                name = source.name.ifBlank {
                    "MAIN SOURCE"
                },
                x = 80f,
                y = 280f,
                voltage = v
            )

        val mainBreaker =
            SldNode(
                id = "auto-cb-1",
                name = "MAIN ACB",
                type = SldNodeType.BREAKER,
                x = 300f,
                y = 280f,
                voltage = v
            )

        val transformer =
            SldNode(
                id = "auto-tr-1",
                name = "TR-01",
                type = SldNodeType.TRANSFORMER,
                x = 520f,
                y = 280f,
                voltage = 400.0,
                ratedKva = 1000.0,
                transformerPercentZ = 6.0
            )

        val bus =
            SldNode(
                id = "auto-bus-1",
                name = "LV MAIN BUS",
                type = SldNodeType.BUS,
                x = 740f,
                y = 280f,
                voltage = 400.0
            )

        val incomer =
            SldNode(
                id = "auto-cb-2",
                name = "INCOMER ACB",
                type = SldNodeType.BREAKER,
                x = 960f,
                y = 280f,
                voltage = 400.0
            )

        val panel =
            SldNode(
                id = "auto-panel-1",
                name = "MDB-01",
                type = SldNodeType.PANEL,
                x = 1180f,
                y = 280f,
                voltage = 400.0,
                ratedKva = 1000.0
            )

        val load1 =
            SldNode(
                id = "auto-load-1",
                name = "LOAD-01",
                type = SldNodeType.LOAD,
                x = 1430f,
                y = 140f,
                voltage = 400.0,
                loadKw = 50.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            )

        val load2 =
            SldNode(
                id = "auto-load-2",
                name = "LOAD-02",
                type = SldNodeType.LOAD,
                x = 1430f,
                y = 280f,
                voltage = 400.0,
                loadKw = 75.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            )

        val load3 =
            SldNode(
                id = "auto-load-3",
                name = "LOAD-03",
                type = SldNodeType.LOAD,
                x = 1430f,
                y = 420f,
                voltage = 400.0,
                loadKw = 100.0,
                powerFactor = 0.90,
                demandFactor = 1.0
            )

        nodes =
            listOf(
                sourceNode,
                mainBreaker,
                transformer,
                bus,
                incomer,
                panel,
                load1,
                load2,
                load3
            )

        fun feeder(
            id: String,
            from: String,
            to: String,
            meters: Double
        ): SldConnection {
            return SldConnection(
                id = id,
                fromNodeId = from,
                toNodeId = to,
                lengthMeters = meters,
                resistanceOhmPerKm = 0.125,
                reactanceOhmPerKm = 0.080,
                cableSizeMm2 = 0.0,
                parallelRuns = 1,
                currentCapacityA = 0.0
            )
        }

        connections =
            listOf(
                feeder(
                    "auto-c1",
                    "source-1",
                    "auto-cb-1",
                    5.0
                ),
                feeder(
                    "auto-c2",
                    "auto-cb-1",
                    "auto-tr-1",
                    5.0
                ),
                feeder(
                    "auto-c3",
                    "auto-tr-1",
                    "auto-bus-1",
                    5.0
                ),
                feeder(
                    "auto-c4",
                    "auto-bus-1",
                    "auto-cb-2",
                    5.0
                ),
                feeder(
                    "auto-c5",
                    "auto-cb-2",
                    "auto-panel-1",
                    10.0
                ),
                feeder(
                    "auto-c6",
                    "auto-panel-1",
                    "auto-load-1",
                    20.0
                ),
                feeder(
                    "auto-c7",
                    "auto-panel-1",
                    "auto-load-2",
                    20.0
                ),
                feeder(
                    "auto-c8",
                    "auto-panel-1",
                    "auto-load-3",
                    20.0
                )
            )

        selectedNodeId =
            "auto-panel-1"

        selectedConnectionId = null
        connectionStartId = null

        shortCircuitReport()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
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
                Button(onClick = onBack) {
                    Text(
                        if (arabic)
                            "رجوع"
                        else
                            "Back"
                    )
                }
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.SOURCE)
                }
            ) {
                Text(
                    if (arabic)
                        "مصدر"
                    else
                        "Source"
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.TRANSFORMER)
                }
            ) {
                Text(
                    if (arabic)
                        "محول"
                    else
                        "Transformer"
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.GENERATOR)
                }
            ) {
                Text(
                    if (arabic)
                        "مولد"
                    else
                        "Generator"
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.BUS)
                }
            ) {
                Text(
                    if (arabic)
                        "باسبار"
                    else
                        "Busbar"
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.BREAKER)
                }
            ) {
                Text(
                    if (arabic)
                        "قاطع"
                    else
                        "Breaker"
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.PANEL)
                }
            ) {
                Text(
                    if (arabic)
                        "لوحة"
                    else
                        "Panel"
                )
            }

            Button(
                onClick = {
                    openAdd(SldNodeType.LOAD)
                }
            ) {
                Text(
                    if (arabic)
                        "حمل"
                    else
                        "Load"
                )
            }

            Button(
                onClick = {
                    connect()
                }
            ) {
                Text(
                    if (connectionStartId == null) {
                        if (arabic)
                            "ربط"
                        else
                            "Connect"
                    } else {
                        if (arabic)
                            "اختر النهاية"
                        else
                            "Select End"
                    }
                )
            }

            Button(
                onClick = {
                    deleteSelected()
                }
            ) {
                Text(
                    if (arabic)
                        "حذف"
                    else
                        "Delete"
                )
            }

            Button(
                onClick = {
                    shortCircuitReport()
                }
            ) {
                Text(
                    if (arabic)
                        "تيارات القصر"
                    else
                        "Short Circuit"
                )
            }

            Button(
                onClick = {
                    panelSchedule()
                }
            ) {
                Text(
                    if (arabic)
                        "Panel Schedule"
                    else
                        "Panel Schedule"
                )
            }

            Button(
                onClick = {
                    generateCompleteSld()
                }
            ) {
                Text(
                    if (arabic)
                        "Generate Complete SLD"
                    else
                        "Generate Complete SLD"
                )
            }
        }

        SldCanvas(
            nodes = nodes,
            connections = connections,
            selectedNodeId = selectedNodeId,
            selectedConnectionId = selectedConnectionId,
            connectionStartId = connectionStartId,
            onSelectNode = {
                selectedNodeId = it
                selectedConnectionId = null
            },
            onSelectConnection = {
                selectedConnectionId = it
                selectedNodeId = null
            },
            onMoveNode = { id, dx, dy ->
                nodes =
                    nodes.map {
                        if (it.id == id) {
                            it.copy(
                                x = max(
                                    10f,
                                    it.x + dx
                                ),
                                y = max(
                                    10f,
                                    it.y + dy
                                )
                            )
                        } else {
                            it
                        }
                    }
            },
            onEditNode = { id ->
                nodes.firstOrNull {
                    it.id == id
                }?.let {
                    openEdit(it)
                }
            },
            onEditConnection = { id ->
                connections.firstOrNull {
                    it.id == id
                }?.let {
                    openConnection(it)
                }
            }
        )
    }

    if (showNodeDialog) {
        NodeEditorDialog(
            arabic = arabic,
            type = nodeType,
            name = name,
            voltage = voltage,
            loadKw = loadKw,
            pf = pf,
            demand = demand,
            kva = kva,
            transformerZ = transformerZ,
            generatorXd = generatorXd,
            sourceMva = sourceMva,
            onName = { name = it },
            onVoltage = { voltage = it },
            onLoadKw = { loadKw = it },
            onPf = { pf = it },
            onDemand = { demand = it },
            onKva = { kva = it },
            onTransformerZ = {
                transformerZ = it
            },
            onGeneratorXd = {
                generatorXd = it
            },
            onSourceMva = {
                sourceMva = it
            },
            onSave = {
                saveNode()
            },
            onCancel = {
                showNodeDialog = false
            }
        )
    }

    if (showConnectionDialog) {
        ConnectionEditorDialog(
            arabic = arabic,
            length = length,
            resistance = resistance,
            reactance = reactance,
            cableSize = cableSize,
            parallelRuns = parallelRuns,
            capacity = capacity,
            onLength = { length = it },
            onResistance = {
                resistance = it
            },
            onReactance = {
                reactance = it
            },
            onCableSize = {
                cableSize = it
            },
            onParallelRuns = {
                parallelRuns = it
            },
            onCapacity = {
                capacity = it
            },
            onSave = {
                saveConnection()
            },
            onCancel = {
                showConnectionDialog = false
            }
        )
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = {
                showReport = false
            },
            title = {
                Text(
                    reportTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    Text(
                        text = reportText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReport = false
                    }
                ) {
                    Text(
                        if (arabic)
                            "إغلاق"
                        else
                            "Close"
                    )
                }
            }
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
    onEditNode: (String) -> Unit,
    onEditConnection: (String) -> Unit
) {
    val textMeasurer =
        rememberTextMeasurer()

    val currentNodes by rememberUpdatedState(nodes)
    val currentSelectNode by
        rememberUpdatedState(onSelectNode)
    val currentSelectConnection by
        rememberUpdatedState(onSelectConnection)
    val currentMoveNode by
        rememberUpdatedState(onMoveNode)
    val currentEditNode by
        rememberUpdatedState(onEditNode)
    val currentEditConnection by
        rememberUpdatedState(onEditConnection)

    var draggedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(
                rememberScrollState()
            )
            .verticalScroll(
                rememberScrollState()
            )
            .background(CanvasBackground)
    ) {
        Canvas(
            modifier = Modifier
                .width(2300.dp)
                .height(1000.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { position ->
                            val node =
                                findNode(
                                    position,
                                    currentNodes
                                )

                            if (node != null) {
                                currentSelectNode(
                                    node.id
                                )
                                return@detectTapGestures
                            }

                            val connection =
                                findConnection(
                                    position,
                                    currentNodes,
                                    connections
                                )

                            if (connection != null) {
                                currentSelectConnection(
                                    connection.id
                                )
                            }
                        },
                        onDoubleTap = { position ->
                            val node =
                                findNode(
                                    position,
                                    currentNodes
                                )

                            if (node != null) {
                                currentEditNode(
                                    node.id
                                )
                                return@detectTapGestures
                            }

                            val connection =
                                findConnection(
                                    position,
                                    currentNodes,
                                    connections
                                )

                            if (connection != null) {
                                currentEditConnection(
                                    connection.id
                                )
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            val node =
                                findNode(
                                    position,
                                    currentNodes
                                )

                            draggedNodeId =
                                node?.id

                            if (node != null) {
                                currentSelectNode(
                                    node.id
                                )
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()

                            draggedNodeId?.let {
                                currentMoveNode(
                                    it,
                                    amount.x,
                                    amount.y
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
            drawConnections(
                nodes = currentNodes,
                connections = connections,
                selectedConnectionId =
                    selectedConnectionId,
                textMeasurer = textMeasurer
            )

            currentNodes.forEach { node ->
                drawNode(
                    node = node,
                    selected =
                        node.id == selectedNodeId,
                    connectionStart =
                        node.id == connectionStartId,
                    textMeasurer =
                        textMeasurer
                )
            }
        }
    }
}

private fun DrawScope.drawConnections(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedConnectionId: String?,
    textMeasurer:
        androidx.compose.ui.text.TextMeasurer
) {
    connections.forEach { connection ->
        val from =
            nodes.firstOrNull {
                it.id == connection.fromNodeId
            } ?: return@forEach

        val to =
            nodes.firstOrNull {
                it.id == connection.toNodeId
            } ?: return@forEach

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

        val midX =
            (start.x + end.x) / 2f

        val path =
            Path().apply {
                moveTo(
                    start.x,
                    start.y
                )
                lineTo(
                    midX,
                    start.y
                )
                lineTo(
                    midX,
                    end.y
                )
                lineTo(
                    end.x,
                    end.y
                )
            }

        val selected =
            connection.id ==
                selectedConnectionId

        drawPath(
            path = path,
            color =
                if (selected)
                    Accent
                else
                    CableColor,
            strokeWidth =
                if (selected)
                    7f
                else
                    4f
        )

        val arrow =
            Path().apply {
                moveTo(
                    end.x - 14f,
                    end.y - 8f
                )
                lineTo(
                    end.x,
                    end.y
                )
                lineTo(
                    end.x - 14f,
                    end.y + 8f
                )
                close()
            }

        drawPath(
            path = arrow,
            color =
                if (selected)
                    Accent
                else
                    CableColor
        )

        val cableText =
            if (connection.cableSizeMm2 > 0.0) {
                "${format(connection.cableSizeMm2)} mm² × ${connection.parallelRuns}"
            } else {
                "Cable"
            }

        drawText(
            textMeasurer = textMeasurer,
            text = cableText,
            topLeft = Offset(
                midX - 45f,
                min(
                    start.y,
                    end.y
                ) - 28f
            ),
            style = TextStyle(
                color = TextSecondary,
                fontSize = 13.sp
            )
        )
    }
}

private fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer:
        androidx.compose.ui.text.TextMeasurer
) {
    val x = node.x
    val y = node.y
    val color =
        nodeColor(node.type)

    when (node.type) {
        SldNodeType.SOURCE -> {
            drawCircle(
                color = color,
                radius = 34f,
                center = Offset(
                    x + 55f,
                    y + 48f
                )
            )

            drawLine(
                color = Color.White,
                start = Offset(
                    x + 42f,
                    y + 48f
                ),
                end = Offset(
                    x + 68f,
                    y + 48f
                ),
                strokeWidth = 4f
            )

            drawLine(
                color = Color.White,
                start = Offset(
                    x + 55f,
                    y + 35f
                ),
                end = Offset(
                    x + 55f,
                    y + 61f
                ),
                strokeWidth = 4f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawCircle(
                color = color,
                radius = 30f,
                center = Offset(
                    x + 48f,
                    y + 48f
                ),
                style =
                    androidx.compose.ui.graphics
                        .drawscope.Stroke(
                            width = 6f
                        )
            )

            drawCircle(
                color = color,
                radius = 30f,
                center = Offset(
                    x + 88f,
                    y + 48f
                ),
                style =
                    androidx.compose.ui.graphics
                        .drawscope.Stroke(
                            width = 6f
                        )
            )

            drawLine(
                color = color,
                start = Offset(
                    x + 68f,
                    y + 14f
                ),
                end = Offset(
                    x + 68f,
                    y + 82f
                ),
                strokeWidth = 5f
            )
        }

        SldNodeType.GENERATOR -> {
            drawCircle(
                color = color,
                radius = 38f,
                center = Offset(
                    x + 65f,
                    y + 50f
                ),
                style =
                    androidx.compose.ui.graphics
                        .drawscope.Stroke(
                            width = 6f
                        )
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "G",
                topLeft = Offset(
                    x + 55f,
                    y + 31f
                ),
                style = TextStyle(
                    color = color,
                    fontSize = 28.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            )
        }

        SldNodeType.BUS -> {
            drawLine(
                color = color,
                start = Offset(
                    x + 10f,
                    y + 50f
                ),
                end = Offset(
                    x + 150f,
                    y + 50f
                ),
                strokeWidth = 12f
            )
        }

        SldNodeType.BREAKER -> {
            drawRect(
                color = color,
                topLeft = Offset(
                    x + 35f,
                    y + 25f
                ),
                size = Size(
                    65f,
                    50f
                )
            )

            drawLine(
                color = Color.White,
                start = Offset(
                    x + 48f,
                    y + 60f
                ),
                end = Offset(
                    x + 87f,
                    y + 38f
                ),
                strokeWidth = 5f
            )
        }

        SldNodeType.PANEL -> {
            drawRect(
                color = color,
                topLeft = Offset(
                    x + 25f,
                    y + 20f
                ),
                size = Size(
                    100f,
                    65f
                ),
                style =
                    androidx.compose.ui.graphics
                        .drawscope.Stroke(
                            width = 6f
                        )
            )

            drawLine(
                color = color,
                start = Offset(
                    x + 35f,
                    y + 42f
                ),
                end = Offset(
                    x + 115f,
                    y + 42f
                ),
                strokeWidth = 5f
            )
        }

        SldNodeType.LOAD -> {
            drawCircle(
                color = color,
                radius = 34f,
                center = Offset(
                    x + 60f,
                    y + 50f
                )
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "L",
                topLeft = Offset(
                    x + 51f,
                    y + 30f
                ),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            )
        }
    }

    if (selected || connectionStart) {
        drawRect(
            color =
                if (connectionStart)
                    Accent
                else
                    SelectedColor,
            topLeft = Offset(
                x - 5f,
                y - 5f
            ),
            size = Size(
                NODE_WIDTH + 10f,
                NODE_HEIGHT + 10f
            ),
            style =
                androidx.compose.ui.graphics
                    .drawscope.Stroke(
                        width = 4f
                    )
        )
    }

    drawText(
        textMeasurer = textMeasurer,
        text = node.name,
        topLeft = Offset(
            x,
            y + 94f
        ),
        style = TextStyle(
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight =
                FontWeight.Bold
        )
    )

    drawNodeData(
        node = node,
        textMeasurer = textMeasurer
    )
}

private fun DrawScope.drawNodeData(
    node: SldNode,
    textMeasurer:
        androidx.compose.ui.text.TextMeasurer
) {
    val x = node.x
    val y = node.y

    when (node.type) {
        SldNodeType.SOURCE -> {
            text(
                textMeasurer,
                "${format(node.voltage)} V",
                x + 105f,
                y + 30f
            )

            text(
                textMeasurer,
                "${format(node.sourceShortCircuitMva)} MVA",
                x + 105f,
                y + 52f
            )
        }

        SldNodeType.TRANSFORMER -> {
            text(
                textMeasurer,
                "${format(node.ratedKva)} kVA",
                x + 105f,
                y + 30f
            )

            text(
                textMeasurer,
                "Z = ${format(node.transformerPercentZ)}%",
                x + 105f,
                y + 52f
            )
        }

        SldNodeType.GENERATOR -> {
            text(
                textMeasurer,
                "${format(node.ratedKva)} kVA",
                x + 105f,
                y + 30f
            )

            text(
                textMeasurer,
                "Xd'' = ${format(node.generatorXdSubtransient)}%",
                x + 105f,
                y + 52f
            )
        }

        SldNodeType.BUS -> {
            text(
                textMeasurer,
                "${format(node.voltage)} V",
                x + 40f,
                y + 84f
            )
        }

        SldNodeType.BREAKER -> {
            text(
                textMeasurer,
                "Icu / Ics",
                x + 105f,
                y + 40f
            )
        }

        SldNodeType.PANEL -> {
            text(
                textMeasurer,
                "${format(node.ratedKva)} kVA",
                x + 105f,
                y + 40f
            )
        }

        SldNodeType.LOAD -> {
            val current =
                if (
                    node.voltage > 0.0 &&
                    node.powerFactor > 0.0
                ) {
                    node.loadKw * 1000.0 /
                        (
                            sqrt(3.0) *
                                node.voltage *
                                node.powerFactor
                            )
                } else {
                    0.0
                }

            text(
                textMeasurer,
                "${format(node.loadKw)} kW",
                x + 100f,
                y + 30f
            )

            text(
                textMeasurer,
                "${format(current)} A",
                x + 100f,
                y + 52f
            )
        }
    }
}

private fun DrawScope.text(
    textMeasurer:
        androidx.compose.ui.text.TextMeasurer,
    value: String,
    x: Float,
    y: Float
) {
    drawText(
        textMeasurer = textMeasurer,
        text = value,
        topLeft = Offset(
            x,
            y
        ),
        style = TextStyle(
            color = TextSecondary,
            fontSize = 12.sp
        )
    )
}

private fun findNode(
    position: Offset,
    nodes: List<SldNode>
): SldNode? {
    return nodes
        .asReversed()
        .firstOrNull {
            position.x >=
                it.x - TOUCH_PADDING &&
                position.x <=
                it.x +
                    NODE_WIDTH +
                    TOUCH_PADDING &&
                position.y >=
                it.y - TOUCH_PADDING &&
                position.y <=
                it.y +
                    NODE_HEIGHT +
                    TOUCH_PADDING
        }
}

private fun findConnection(
    position: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {
    var result: SldConnection? = null
    var best = 20f

    connections.forEach { connection ->
        val from =
            nodes.firstOrNull {
                it.id == connection.fromNodeId
            } ?: return@forEach

        val to =
            nodes.firstOrNull {
                it.id == connection.toNodeId
            } ?: return@forEach

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

        val mid =
            (start.x + end.x) / 2f

        val d1 =
            distanceToSegment(
                position,
                start,
                Offset(mid, start.y)
            )

        val d2 =
            distanceToSegment(
                position,
                Offset(mid, start.y),
                Offset(mid, end.y)
            )

        val d3 =
            distanceToSegment(
                position,
                Offset(mid, end.y),
                end
            )

        val d =
            min(
                d1,
                min(d2, d3)
            )

        if (d < best) {
            best = d
            result = connection
        }
    }

    return result
}

private fun distanceToSegment(
    p: Offset,
    a: Offset,
    b: Offset
): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y

    if (
        dx == 0f &&
        dy == 0f
    ) {
        return hypot(
            p.x - a.x,
            p.y - a.y
        )
    }

    val t =
        (
            (p.x - a.x) * dx +
                (p.y - a.y) * dy
            ) /
            (
                dx * dx +
                    dy * dy
                )

    val u =
        t.coerceIn(
            0f,
            1f
        )

    val x =
        a.x + u * dx

    val y =
        a.y + u * dy

    return hypot(
        p.x - x,
        p.y - y
    )
}

private fun nodeColor(
    type: SldNodeType
): Color {
    return when (type) {
        SldNodeType.SOURCE ->
            SourceColor

        SldNodeType.TRANSFORMER ->
            TransformerColor

        SldNodeType.GENERATOR ->
            GeneratorColor

        SldNodeType.BUS ->
            BusColor

        SldNodeType.BREAKER ->
            BreakerColor

        SldNodeType.PANEL ->
            PanelColor

        SldNodeType.LOAD ->
            LoadColor
    }
}

private fun format(
    value: Double
): String {
    return if (
        value.isFinite()
    ) {
        if (
            kotlin.math.abs(value) >= 100.0
        ) {
            "%.1f".format(value)
        } else {
            "%.2f".format(value)
        }
    } else {
        "0.00"
    }
}

private fun buildShortCircuitReport(
    study: SldShortCircuitStudy,
    nodes: List<SldNode>,
    arabic: Boolean
): String {
    val sb = StringBuilder()

    sb.appendLine(
        "================================================"
    )

    sb.appendLine(
        if (arabic)
            "تقرير هندسي لتيارات القصر"
        else
            "SHORT CIRCUIT ENGINEERING REPORT"
    )

    sb.appendLine(
        "================================================"
    )

    sb.appendLine()

    sb.appendLine(
        "MAXIMUM FAULT LEVEL"
    )

    sb.appendLine(
        "Ik'' max       = ${format(study.maximumFaultCurrentKa)} kA"
    )

    sb.appendLine(
        "Ip peak max    = ${format(study.maximumPeakCurrentKa)} kA"
    )

    sb.appendLine(
        "Fault MVA max  = ${format(study.maximumFaultMva)} MVA"
    )

    sb.appendLine()

    sb.appendLine(
        "FAULT RESULTS BY POINT"
    )

    sb.appendLine(
        "------------------------------------------------"
    )

    study.results.values.forEach { result ->

        sb.appendLine(
            "POINT : ${result.nodeName}"
        )

        sb.appendLine(
            "Voltage              : ${format(result.voltageV)} V"
        )

        sb.appendLine(
            "R                    : ${format(result.resistanceOhm)} Ω"
        )

        sb.appendLine(
            "X                    : ${format(result.reactanceOhm)} Ω"
        )

        sb.appendLine(
            "Z                    : ${format(result.impedanceOhm)} Ω"
        )

        sb.appendLine(
            "X/R                  : ${format(result.xrRatio)}"
        )

        sb.appendLine(
            "Ik''                  : ${format(result.initialSymmetricalCurrentKa)} kA"
        )

        sb.appendLine(
            "Ip peak              : ${format(result.peakCurrentKa)} kA"
        )

        sb.appendLine(
            "Ith                  : ${format(result.thermalCurrentKa)} kA"
        )

        sb.appendLine(
            "Fault MVA            : ${format(result.shortCircuitMva)} MVA"
        )

        sb.appendLine(
            "Required breaker Icu : ${format(result.breakerRequiredKa)} kA"
        )

        sb.appendLine(
            "Recommended Ics      : >= ${
                format(
                    result.breakerRequiredKa * 0.75
                )
            } kA*"
        )

        sb.appendLine()

        if (result.notes.isNotEmpty()) {
            sb.appendLine("Calculation Notes:")

            result.notes.forEach {
                sb.appendLine("  - $it")
            }

            sb.appendLine()
        }

        sb.appendLine(
            "------------------------------------------------"
        )
    }

    sb.appendLine()

    sb.appendLine(
        "SOURCE / TRANSFORMER DATA"
    )

    nodes.forEach { node ->
        when (node.type) {
            SldNodeType.SOURCE -> {
                sb.appendLine(
                    "${node.name}: " +
                        "${format(node.sourceShortCircuitMva)} MVA, " +
                        "${format(node.voltage)} V"
                )
            }

            SldNodeType.TRANSFORMER -> {
                sb.appendLine(
                    "${node.name}: " +
                        "${format(node.ratedKva)} kVA, " +
                        "Z=${format(node.transformerPercentZ)}%"
                )
            }

            SldNodeType.GENERATOR -> {
                sb.appendLine(
                    "${node.name}: " +
                        "${format(node.ratedKva)} kVA, " +
                        "Xd''=${format(node.generatorXdSubtransient)}%"
                )
            }

            else -> Unit
        }
    }

    sb.appendLine()

    sb.appendLine(
        "* Ics is manufacturer dependent and shall be verified against the selected breaker catalogue."
    )

    return sb.toString()
}

private fun buildPanelSchedule(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): String {
    val sb = StringBuilder()

    sb.appendLine(
        "================================================"
    )

    sb.appendLine(
        "${panel.name} - PANEL SCHEDULE"
    )

    sb.appendLine(
        "================================================"
    )

    sb.appendLine(
        "Voltage : ${format(panel.voltage)} V"
    )

    sb.appendLine(
        "Rating  : ${format(panel.ratedKva)} kVA"
    )

    sb.appendLine()

    sb.appendLine(
        "Ckt | Load | kW | PF | DF | Current | Cable | Runs"
    )

    sb.appendLine(
        "------------------------------------------------"
    )

    var connectedKw = 0.0
    var demandKw = 0.0

    connections
        .filter {
            it.fromNodeId == panel.id
        }
        .forEachIndexed { index, connection ->

            val load =
                nodes.firstOrNull {
                    it.id == connection.toNodeId
                }

            if (load != null) {
                val current =
                    if (
                        load.voltage > 0.0 &&
                        load.powerFactor > 0.0
                    ) {
                        load.loadKw * 1000.0 /
                            (
                                sqrt(3.0) *
                                    load.voltage *
                                    load.powerFactor
                                )
                    } else {
                        0.0
                    }

                connectedKw +=
                    load.loadKw

                demandKw +=
                    load.loadKw *
                        load.demandFactor

                val cable =
                    if (
                        connection.cableSizeMm2 > 0.0
                    ) {
                        format(
                            connection.cableSizeMm2
                        )
                    } else {
                        "AUTO"
                    }

                sb.appendLine(
                    "${index + 1} | " +
                        "${load.name} | " +
                        "${format(load.loadKw)} | " +
                        "${format(load.powerFactor)} | " +
                        "${format(load.demandFactor)} | " +
                        "${format(current)} A | " +
                        "$cable | " +
                        "${connection.parallelRuns}"
                )
            }
        }

    sb.appendLine(
        "------------------------------------------------"
    )

    sb.appendLine(
        "Connected Load = ${format(connectedKw)} kW"
    )

    sb.appendLine(
        "Demand Load    = ${format(demandKw)} kW"
    )

    val estimatedKva =
        demandKw / 0.90

    sb.appendLine(
        "Estimated Demand = ${format(estimatedKva)} kVA"
    )

    if (panel.ratedKva > 0.0) {
        val utilization =
            estimatedKva /
                panel.ratedKva *
                100.0

        sb.appendLine(
            "Panel Utilization = ${format(utilization)} %"
        )
    }

    return sb.toString()
}

@Composable
private fun NodeEditorDialog(
    arabic: Boolean,
    type: SldNodeType,
    name: String,
    voltage: String,
    loadKw: String,
    pf: String,
    demand: String,
    kva: String,
    transformerZ: String,
    generatorXd: String,
    sourceMva: String,
    onName: (String) -> Unit,
    onVoltage: (String) -> Unit,
    onLoadKw: (String) -> Unit,
    onPf: (String) -> Unit,
    onDemand: (String) -> Unit,
    onKva: (String) -> Unit,
    onTransformerZ: (String) -> Unit,
    onGeneratorXd: (String) -> Unit,
    onSourceMva: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (arabic)
                    "بيانات ${typeLabelArabic(type)}"
                else
                    "${typeLabelEnglish(type)} Data"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Field(
                    if (arabic)
                        "الاسم"
                    else
                        "Name",
                    name,
                    onName
                )

                Field(
                    if (arabic)
                        "الجهد V"
                    else
                        "Voltage V",
                    voltage,
                    onVoltage
                )

                when (type) {
                    SldNodeType.SOURCE -> {
                        Field(
                            if (arabic)
                                "مستوى القصر MVA"
                            else
                                "Source Short Circuit MVA",
                            sourceMva,
                            onSourceMva
                        )
                    }

                    SldNodeType.TRANSFORMER -> {
                        Field(
                            if (arabic)
                                "قدرة المحول kVA"
                            else
                                "Transformer Rating kVA",
                            kva,
                            onKva
                        )

                        Field(
                            if (arabic)
                                "%Z"
                            else
                                "Transformer %Z",
                            transformerZ,
                            onTransformerZ
                        )
                    }

                    SldNodeType.GENERATOR -> {
                        Field(
                            if (arabic)
                                "قدرة المولد kVA"
                            else
                                "Generator Rating kVA",
                            kva,
                            onKva
                        )

                        Field(
                            "Xd'' %",
                            generatorXd,
                            onGeneratorXd
                        )
                    }

                    SldNodeType.PANEL -> {
                        Field(
                            if (arabic)
                                "قدرة اللوحة kVA"
                            else
                                "Panel Rating kVA",
                            kva,
                            onKva
                        )
                    }

                    SldNodeType.LOAD -> {
                        Field(
                            if (arabic)
                                "الحمل kW"
                            else
                                "Load kW",
                            loadKw,
                            onLoadKw
                        )

                        Field(
                            "Power Factor",
                            pf,
                            onPf
                        )

                        Field(
                            if (arabic)
                                "Demand Factor"
                            else
                                "Demand Factor",
                            demand,
                            onDemand
                        )
                    }

                    SldNodeType.BUS,
                    SldNodeType.BREAKER -> Unit
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    if (arabic)
                        "حفظ"
                    else
                        "Save"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    if (arabic)
                        "إلغاء"
                    else
                        "Cancel"
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
    onLength: (String) -> Unit,
    onResistance: (String) -> Unit,
    onReactance: (String) -> Unit,
    onCableSize: (String) -> Unit,
    onParallelRuns: (String) -> Unit,
    onCapacity: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (arabic)
                    "بيانات الكابل"
                else
                    "Cable / Feeder Data"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Field(
                    if (arabic)
                        "الطول m"
                    else
                        "Length m",
                    length,
                    onLength
                )

                Field(
                    "R Ω/km",
                    resistance,
                    onResistance
                )

                Field(
                    "X Ω/km",
                    reactance,
                    onReactance
                )

                Field(
                    if (arabic)
                        "مقطع الكابل mm²"
                    else
                        "Cable Section mm²",
                    cableSize,
                    onCableSize
                )

                Field(
                    if (arabic)
                        "عدد المسارات"
                    else
                        "Parallel Runs",
                    parallelRuns,
                    onParallelRuns
                )

                Field(
                    if (arabic)
                        "التيار المسموح A"
                    else
                        "Current Capacity A",
                    capacity,
                    onCapacity
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    if (arabic)
                        "حفظ"
                    else
                        "Save"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    if (arabic)
                        "إلغاء"
                    else
                        "Cancel"
                )
            }
        }
    )
}

@Composable
private fun Field(
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

private fun typeLabelArabic(
    type: SldNodeType
): String {
    return when (type) {
        SldNodeType.SOURCE -> "المصدر"
        SldNodeType.TRANSFORMER -> "المحول"
        SldNodeType.GENERATOR -> "المولد"
        SldNodeType.BUS -> "الباسبار"
        SldNodeType.BREAKER -> "القاطع"
        SldNodeType.PANEL -> "اللوحة"
        SldNodeType.LOAD -> "الحمل"
    }
}

private fun typeLabelEnglish(
    type: SldNodeType
): String {
    return when (type) {
        SldNodeType.SOURCE -> "Source"
        SldNodeType.TRANSFORMER -> "Transformer"
        SldNodeType.GENERATOR -> "Generator"
        SldNodeType.BUS -> "Busbar"
        SldNodeType.BREAKER -> "Breaker"
        SldNodeType.PANEL -> "Panel"
        SldNodeType.LOAD -> "Load"
    }
}
