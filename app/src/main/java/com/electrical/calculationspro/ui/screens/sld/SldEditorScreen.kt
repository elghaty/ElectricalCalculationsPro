
package com.electrical.calculationspro.ui.screens.sld

import android.graphics.Paint as AndroidPaint
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldAutoLayoutEngine
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldDesignValidator
import com.electrical.calculationspro.data.SldEngineeringFacade
import com.electrical.calculationspro.data.SldNetwork
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import java.util.UUID
import kotlin.math.abs

private const val SLD_NODE_WIDTH = 180f
private const val SLD_NODE_HEIGHT = 100f
private const val SLD_CANVAS_WIDTH = 2600
private const val SLD_CANVAS_HEIGHT = 1800

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {
    val arabic = language == AppLanguage.ARABIC

    var network by remember { mutableStateOf(SldNetwork()) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var connectionStartId by remember { mutableStateOf<String?>(null) }
    var connectMode by remember { mutableStateOf(false) }
    var connectionType by remember {
        mutableStateOf(SldConnectionType.CABLE)
    }
    var showConnectionMenu by remember { mutableStateOf(false) }
    var showNodeDialog by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(
            if (arabic) {
                "أنشئ مصدر التغذية ثم أضف العناصر ووصلها."
            } else {
                "Add a source, place equipment, and connect the network."
            }
        )
    }
    var calculationSummary by remember { mutableStateOf<String?>(null) }

    val selectedNode = network.nodes.firstOrNull {
        it.id == selectedNodeId
    }

    val validation = remember(network) {
        SldDesignValidator.validate(network)
    }

    fun addNode(type: SldNodeType) {
        if (
            type == SldNodeType.SOURCE &&
            network.nodes.any { it.type == SldNodeType.SOURCE }
        ) {
            status = if (arabic) {
                "يوجد مصدر بالفعل. لا تضف مصدرًا آخر قبل تحديد نظام تشغيل المصادر."
            } else {
                "A source already exists. Define source operating modes before adding another."
            }
            return
        }

        val index = network.nodes.size
        val column = index % 4
        val row = index / 4

        val nameBase = when (type) {
            SldNodeType.SOURCE -> if (arabic) "مصدر" else "SOURCE"
            SldNodeType.TRANSFORMER -> if (arabic) "محول" else "TR"
            SldNodeType.GENERATOR -> if (arabic) "مولد" else "GEN"
            SldNodeType.BUS -> if (arabic) "قضبان" else "BUS"
            SldNodeType.PANEL -> if (arabic) "لوحة" else "MDB"
            SldNodeType.BREAKER -> if (arabic) "قاطع" else "CB"
            SldNodeType.LOAD -> if (arabic) "حمل" else "LOAD"
        }

        val sameTypeCount = network.nodes.count { it.type == type } + 1
        val node = SldNode(
            id = UUID.randomUUID().toString(),
            name = "$nameBase-$sameTypeCount",
            type = type,
            x = 120f + column * 300f,
            y = 120f + row * 220f,
            voltage = network.nodes.firstOrNull()?.voltage ?: 400.0,
            loadKw = if (type == SldNodeType.LOAD) 10.0 else 0.0,
            ratedKva = if (type == SldNodeType.TRANSFORMER) 500.0 else 0.0,
            ratedCurrentA = if (type == SldNodeType.BREAKER) 100.0 else 0.0,
            breakerFrameA = if (type == SldNodeType.BREAKER) 100.0 else 0.0,
            shortCircuitRatingKA = if (type == SldNodeType.BREAKER) 25.0 else 0.0
        )

        network = network.copy(nodes = network.nodes + node)
        selectedNodeId = node.id
        connectionStartId = null
        calculationSummary = null
        status = if (arabic) {
            "تمت إضافة ${node.name}. اختر العنصر ثم عدّل بياناته أو ابدأ التوصيل."
        } else {
            "${node.name} added. Select it to edit its data or start connecting."
        }
    }

    fun connectNodes(firstId: String, secondId: String) {
        if (firstId == secondId) {
            status = if (arabic) "لا يمكن توصيل العنصر بنفسه." else "An element cannot connect to itself."
            return
        }

        val first = network.nodes.firstOrNull { it.id == firstId }
        val second = network.nodes.firstOrNull { it.id == secondId }

        if (first == null || second == null) {
            status = if (arabic) "العنصر غير موجود." else "Equipment was not found."
            return
        }

        if (
            network.connections.any {
                it.fromNodeId == firstId &&
                    it.toNodeId == secondId &&
                    it.connectionType == connectionType
            }
        ) {
            status = if (arabic) "هذا التوصيل موجود بالفعل." else "This connection already exists."
            return
        }

        if (connectionType == SldConnectionType.BUSBAR) {
            val validPair =
                (first.type == SldNodeType.PANEL && second.type == SldNodeType.BUS) ||
                (first.type == SldNodeType.BUS && second.type == SldNodeType.PANEL) ||
                (first.type == SldNodeType.PANEL && second.type == SldNodeType.BREAKER) ||
                (first.type == SldNodeType.BREAKER && second.type == SldNodeType.PANEL) ||
                (first.type == SldNodeType.BUS && second.type == SldNodeType.BREAKER) ||
                (first.type == SldNodeType.BREAKER && second.type == SldNodeType.BUS)

            if (!validPair) {
                status = if (arabic) {
                    "توصيل BUSBAR مسموح داخل اللوحة فقط: PANEL–BUS أو BUS–BREAKER أو PANEL–BREAKER."
                } else {
                    "BUSBAR is only valid for PANEL–BUS, BUS–BREAKER, or PANEL–BREAKER."
                }
                return
            }
        }

        val connection = SldConnection(
            id = UUID.randomUUID().toString(),
            fromNodeId = firstId,
            toNodeId = secondId,
            connectionType = connectionType,
            busbarMaterial = "Copper",
            busbarRatedCurrentA = if (connectionType == SldConnectionType.BUSBAR) 400.0 else 0.0,
            busbarShortCircuitKA = if (connectionType == SldConnectionType.BUSBAR) 25.0 else 0.0
        )

        network = network.copy(connections = network.connections + connection)
        calculationSummary = null
        status = if (arabic) {
            "تم التوصيل من ${first.name} إلى ${second.name}. اتجاه التوصيل محفوظ كما اخترته."
        } else {
            "Connected ${first.name} to ${second.name}. The selected direction is preserved."
        }
    }

    fun handleNodeTap(nodeId: String) {
        selectedNodeId = nodeId

        if (!connectMode) {
            return
        }

        val start = connectionStartId
        if (start == null) {
            connectionStartId = nodeId
            status = if (arabic) {
                "تم اختيار بداية التوصيل. اختر العنصر التالي لتحديد اتجاه المغذي."
            } else {
                "Start selected. Select the downstream element to set feeder direction."
            }
        } else {
            connectNodes(start, nodeId)
            connectionStartId = null
        }
    }

    fun moveNode(nodeId: String, dx: Float, dy: Float) {
        network = network.copy(
            nodes = network.nodes.map { node ->
                if (node.id == nodeId) {
                    node.copy(
                        x = (node.x + dx).coerceAtLeast(20f),
                        y = (node.y + dy).coerceAtLeast(20f)
                    )
                } else {
                    node
                }
            }
        )
        calculationSummary = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = if (arabic) "رجوع" else "Back"
                    )
                }
            }

            Icon(
                Icons.Outlined.AccountTree,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (arabic) "مصمم المخطط الأحادي SLD" else "Single Line Diagram Designer",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    if (arabic) "محرر الرسم والتوصيلات الهندسية" else "Diagram and electrical topology editor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    val gate = SldEngineeringFacade.checkEngineeringTopology(network)
                    if (!gate.valid) {
                        calculationSummary = null
                        status = gate.errorMessage ?: if (arabic) {
                            "الشبكة غير جاهزة للحساب."
                        } else {
                            "The network is not ready for engineering."
                        }
                    } else {
                        try {
                            val result = SldEngineeringFacade.calculateUpstream(network)
                            calculationSummary = buildString {
                                appendLine(if (arabic) "نتائج الحساب الأولي" else "Preliminary calculation")
                                appendLine("${if (arabic) "الحمل المتصل" else "Connected load"}: %.2f kW".format(result.totalConnectedKw))
                                appendLine("${if (arabic) "حمل الطلب" else "Demand load"}: %.2f kW".format(result.totalDemandKw))
                                appendLine("${if (arabic) "القدرة الظاهرية" else "Apparent power"}: %.2f kVA".format(result.totalKva))
                                appendLine("${if (arabic) "تيار المصدر" else "Source current"}: %.2f A".format(result.sourceCurrentA))
                                appendLine("${if (arabic) "القاطع الرئيسي المقترح" else "Recommended main breaker"}: %.0f A".format(result.recommendedMainBreakerA))
                                appendLine("${if (arabic) "المحول المقترح" else "Recommended transformer"}: %.0f kVA".format(result.recommendedTransformerKva))
                                appendLine("${if (arabic) "أقصى هبوط جهد" else "Maximum voltage drop"}: %.3f %%".format(result.maximumVoltageDropPercent))
                                if (result.warnings.isNotEmpty()) {
                                    appendLine()
                                    appendLine(if (arabic) "ملاحظات هندسية:" else "Engineering notes:")
                                    result.warnings.forEach { appendLine("• $it") }
                                }
                            }
                            status = if (arabic) {
                                "اكتمل حساب الحمل المبدئي. راجع الملاحظات قبل اعتماد التصميم."
                            } else {
                                "Preliminary load calculation completed. Review warnings before design approval."
                            }
                        } catch (error: Exception) {
                            calculationSummary = null
                            status = error.message ?: if (arabic) {
                                "تعذر إكمال الحساب."
                            } else {
                                "Engineering calculation failed."
                            }
                        }
                    }
                }
            ) {
                Icon(Icons.Outlined.Calculate, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (arabic) "حساب" else "Calculate")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SldNodeType.values().forEach { type ->
                OutlinedButton(onClick = { addNode(type) }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(sldTypeLabel(type, arabic))
                }
            }

            Box {
                OutlinedButton(onClick = { showConnectionMenu = true }) {
                    Icon(Icons.Outlined.AccountTree, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when (connectionType) {
                            SldConnectionType.CABLE -> if (arabic) "نوع التوصيل: كابل" else "Connection: Cable"
                            SldConnectionType.BUSBAR -> if (arabic) "نوع التوصيل: Busbar" else "Connection: Busbar"
                        }
                    )
                }

                DropdownMenu(
                    expanded = showConnectionMenu,
                    onDismissRequest = { showConnectionMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (arabic) "مغذي كابل" else "Cable feeder") },
                        onClick = {
                            connectionType = SldConnectionType.CABLE
                            showConnectionMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (arabic) "توصيل داخلي Busbar" else "Internal busbar") },
                        onClick = {
                            connectionType = SldConnectionType.BUSBAR
                            showConnectionMenu = false
                        }
                    )
                }
            }

            Button(
                onClick = {
                    connectMode = !connectMode
                    connectionStartId = null
                    status = if (connectMode) {
                        if (arabic) "تم إيقاف وضع التوصيل." else "Connection mode disabled."
                    } else {
                        if (arabic) "وضع التوصيل: اختر المصدر أولًا ثم العنصر التالي." else "Connection mode: select upstream first, then downstream."
                    }
                }
            ) {
                Text(
                    if (connectMode) {
                        if (arabic) "إنهاء التوصيل" else "Finish connecting"
                    } else {
                        if (arabic) "بدء التوصيل" else "Connect"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    if (network.nodes.isNotEmpty()) {
                        val arranged = SldAutoLayoutEngine.arrange(network)
                        network = arranged.network
                        calculationSummary = null
                        status = if (arabic) {
                            "تم ترتيب العناصر آليًا. راجع مواقع اللوحات والتوصيلات."
                        } else {
                            "Automatic layout applied. Review panel and connection placement."
                        }
                    }
                }
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (arabic) "ترتيب آلي" else "Auto layout")
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (arabic) {
                        "العناصر: ${network.nodes.size}  |  التوصيلات: ${network.connections.size}  |  أخطاء التحقق: ${validation.errors.size}"
                    } else {
                        "Equipment: ${network.nodes.size}  |  Connections: ${network.connections.size}  |  Validation errors: ${validation.errors.size}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (calculationSummary != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    calculationSummary!!,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (arabic) {
                    "المحدد: ${selectedNode?.name ?: "لا يوجد"}"
                } else {
                    "Selected: ${selectedNode?.name ?: "None"}"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                enabled = selectedNode != null,
                onClick = { showNodeDialog = true }
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (arabic) "تعديل" else "Edit")
            }

            OutlinedButton(
                enabled = selectedNode != null,
                onClick = {
                    val id = selectedNodeId
                    if (id != null) {
                        network = network.copy(
                            nodes = network.nodes.filterNot { it.id == id },
                            connections = network.connections.filterNot {
                                it.fromNodeId == id || it.toNodeId == id
                            }
                        )
                        selectedNodeId = null
                        connectionStartId = null
                        calculationSummary = null
                        status = if (arabic) "تم حذف العنصر وتوصيلاته." else "Equipment and its connections removed."
                    }
                }
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (arabic) "حذف" else "Delete")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp)
                )
        ) {
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
            ) {
                Canvas(
                    modifier = Modifier
                        .width(SLD_CANVAS_WIDTH.dp)
                        .height(SLD_CANVAS_HEIGHT.dp)
                        .pointerInput(network.nodes, connectMode, connectionStartId) {
                            detectTapGestures { tap ->
                                val tapX = tap.x / density.density
                                val tapY = tap.y / density.density
                                val hit = network.nodes
                                    .asReversed()
                                    .firstOrNull { node ->
                                        tapX >= node.x - 20f &&
                                            tapX <= node.x + SLD_NODE_WIDTH + 20f &&
                                            tapY >= node.y - 20f &&
                                            tapY <= node.y + SLD_NODE_HEIGHT + 20f
                                    }
                                if (hit != null) {
                                    handleNodeTap(hit.id)
                                }
                            }
                        }
                        .pointerInput(network.nodes, selectedNodeId) {
                            detectDragGestures(
                                onDragStart = { point ->
                                    val x = point.x / density.density
                                    val y = point.y / density.density
                                    val hit = network.nodes
                                        .asReversed()
                                        .firstOrNull { node ->
                                            x >= node.x - 20f &&
                                                x <= node.x + SLD_NODE_WIDTH + 20f &&
                                                y >= node.y - 20f &&
                                                y <= node.y + SLD_NODE_HEIGHT + 20f
                                        }
                                    if (hit != null) selectedNodeId = hit.id
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val id = selectedNodeId
                                    if (id != null) {
                                        moveNode(
                                            id,
                                            dragAmount.x / density.density,
                                            dragAmount.y / density.density
                                        )
                                    }
                                }
                            )
                        }
                ) {
                    val gridColor = Color(0xFFE0E5EB)
                    val gridStep = 40.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += gridStep
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += gridStep
                    }

                    val nodesById = network.nodes.associateBy { it.id }

                    network.connections.forEach { connection ->
                        val from = nodesById[connection.fromNodeId] ?: return@forEach
                        val to = nodesById[connection.toNodeId] ?: return@forEach

                        val start = Offset(
                            (from.x + SLD_NODE_WIDTH / 2f).dp.toPx(),
                            (from.y + SLD_NODE_HEIGHT / 2f).dp.toPx()
                        )
                        val end = Offset(
                            (to.x + SLD_NODE_WIDTH / 2f).dp.toPx(),
                            (to.y + SLD_NODE_HEIGHT / 2f).dp.toPx()
                        )

                        val color = if (connection.connectionType == SldConnectionType.BUSBAR) {
                            Color(0xFFB66A16)
                        } else {
                            Color(0xFF263746)
                        }

                        val midX = (start.x + end.x) / 2f
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            lineTo(midX, start.y)
                            lineTo(midX, end.y)
                            lineTo(end.x, end.y)
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = if (connection.connectionType == SldConnectionType.BUSBAR) 4.dp.toPx() else 2.dp.toPx()
                            )
                        )
                    }

                    network.nodes.forEach { node ->
                        val left = node.x.dp.toPx()
                        val top = node.y.dp.toPx()
                        val w = SLD_NODE_WIDTH.dp.toPx()
                        val h = SLD_NODE_HEIGHT.dp.toPx()
                        val isSelected = node.id == selectedNodeId
                        val borderColor = if (isSelected) {
                            Color(0xFF1976D2)
                        } else {
                            Color(0xFF34495E)
                        }
                        val fillColor = if (isSelected) {
                            Color(0xFFE3F2FD)
                        } else {
                            Color.White
                        }

                        if (node.type == SldNodeType.PANEL) {
                            drawRect(
                                color = Color(0xFF687987),
                                topLeft = Offset(left - 20.dp.toPx(), top - 20.dp.toPx()),
                                size = Size(w + 40.dp.toPx(), h + 40.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        when (node.type) {
                            SldNodeType.SOURCE -> {
                                drawCircle(
                                    color = fillColor,
                                    radius = 25.dp.toPx(),
                                    center = Offset(left + w / 2f, top + 25.dp.toPx())
                                )
                                drawCircle(
                                    color = borderColor,
                                    radius = 25.dp.toPx(),
                                    center = Offset(left + w / 2f, top + 25.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawLine(
                                    borderColor,
                                    Offset(left + w / 2f - 12.dp.toPx(), top + 25.dp.toPx()),
                                    Offset(left + w / 2f + 12.dp.toPx(), top + 25.dp.toPx()),
                                    2.dp.toPx()
                                )
                                drawLine(
                                    borderColor,
                                    Offset(left + w / 2f, top + 13.dp.toPx()),
                                    Offset(left + w / 2f, top + 37.dp.toPx()),
                                    2.dp.toPx()
                                )
                            }

                            SldNodeType.TRANSFORMER -> {
                                drawCircle(
                                    borderColor,
                                    22.dp.toPx(),
                                    Offset(left + w / 2f - 13.dp.toPx(), top + 25.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    borderColor,
                                    22.dp.toPx(),
                                    Offset(left + w / 2f + 13.dp.toPx(), top + 25.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            SldNodeType.BUS -> {
                                drawLine(
                                    Color(0xFFB66A16),
                                    Offset(left + 15.dp.toPx(), top + 25.dp.toPx()),
                                    Offset(left + w - 15.dp.toPx(), top + 25.dp.toPx()),
                                    6.dp.toPx()
                                )
                            }

                            SldNodeType.BREAKER -> {
                                drawRect(
                                    fillColor,
                                    Offset(left + w / 2f - 25.dp.toPx(), top + 7.dp.toPx()),
                                    Size(50.dp.toPx(), 38.dp.toPx())
                                )
                                drawRect(
                                    borderColor,
                                    Offset(left + w / 2f - 25.dp.toPx(), top + 7.dp.toPx()),
                                    Size(50.dp.toPx(), 38.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawLine(
                                    borderColor,
                                    Offset(left + w / 2f - 13.dp.toPx(), top + 33.dp.toPx()),
                                    Offset(left + w / 2f + 13.dp.toPx(), top + 15.dp.toPx()),
                                    2.dp.toPx()
                                )
                            }

                            SldNodeType.GENERATOR -> {
                                drawCircle(
                                    fillColor,
                                    25.dp.toPx(),
                                    Offset(left + w / 2f, top + 25.dp.toPx())
                                )
                                drawCircle(
                                    borderColor,
                                    25.dp.toPx(),
                                    Offset(left + w / 2f, top + 25.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawLine(
                                    borderColor,
                                    Offset(left + w / 2f - 12.dp.toPx(), top + 25.dp.toPx()),
                                    Offset(left + w / 2f + 12.dp.toPx(), top + 25.dp.toPx()),
                                    2.dp.toPx()
                                )
                            }

                            SldNodeType.PANEL,
                            SldNodeType.LOAD -> {
                                drawRect(
                                    color = fillColor,
                                    topLeft = Offset(left + 20.dp.toPx(), top + 4.dp.toPx()),
                                    size = Size(w - 40.dp.toPx(), 42.dp.toPx())
                                )
                                drawRect(
                                    color = borderColor,
                                    topLeft = Offset(left + 20.dp.toPx(), top + 4.dp.toPx()),
                                    size = Size(w - 40.dp.toPx(), 42.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }

                        val paint = AndroidPaint().apply {
                            color = android.graphics.Color.rgb(30, 41, 51)
                            textSize = 13.dp.toPx()
                            textAlign = AndroidPaint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            node.name.take(22),
                            left + w / 2f,
                            top + 65.dp.toPx(),
                            paint
                        )

                        val detail = when (node.type) {
                            SldNodeType.LOAD -> "%.1f kW".format(node.loadKw)
                            SldNodeType.TRANSFORMER -> if (node.ratedKva > 0) "${node.ratedKva.toInt()} kVA" else "${node.voltage.toInt()} V"
                            SldNodeType.BREAKER -> if (node.ratedCurrentA > 0) "${node.ratedCurrentA.toInt()} A" else "Rating not set"
                            else -> "${node.voltage.toInt()} V"
                        }
                        paint.textSize = 11.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(
                            detail,
                            left + w / 2f,
                            top + 84.dp.toPx(),
                            paint
                        )

                        if (isSelected) {
                            drawRect(
                                color = Color(0xFF1976D2),
                                topLeft = Offset(left - 4.dp.toPx(), top - 4.dp.toPx()),
                                size = Size(w + 8.dp.toPx(), h + 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNodeDialog && selectedNode != null) {
        SldNodePropertiesDialog(
            node = selectedNode,
            arabic = arabic,
            onDismiss = { showNodeDialog = false },
            onSave = { updated ->
                network = network.copy(
                    nodes = network.nodes.map {
                        if (it.id == updated.id) updated else it
                    }
                )
                calculationSummary = null
                status = if (arabic) {
                    "تم تحديث بيانات العنصر. أعد الحساب لمراجعة النتائج."
                } else {
                    "Equipment updated. Recalculate to refresh the results."
                }
                showNodeDialog = false
            }
        )
    }
}

@Composable
private fun SldNodePropertiesDialog(
    node: SldNode,
    arabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (SldNode) -> Unit
) {
    var name by remember(node.id) { mutableStateOf(node.name) }
    var voltage by remember(node.id) { mutableStateOf(node.voltage.toString()) }
    var loadKw by remember(node.id) { mutableStateOf(node.loadKw.toString()) }
    var powerFactor by remember(node.id) { mutableStateOf(node.powerFactor.toString()) }
    var ratedKva by remember(node.id) { mutableStateOf(node.ratedKva.toString()) }
    var ratedCurrent by remember(node.id) { mutableStateOf(node.ratedCurrentA.toString()) }
    var faultRating by remember(node.id) { mutableStateOf(node.shortCircuitRatingKA.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (arabic) "بيانات العنصر" else "Equipment properties")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (arabic) "الاسم / Tag" else "Name / Tag") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = voltage,
                    onValueChange = { voltage = it },
                    label = { Text(if (arabic) "الجهد (V)" else "Voltage (V)") },
                    singleLine = true
                )
                if (node.type == SldNodeType.LOAD) {
                    OutlinedTextField(
                        value = loadKw,
                        onValueChange = { loadKw = it },
                        label = { Text(if (arabic) "الحمل (kW)" else "Load (kW)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = powerFactor,
                        onValueChange = { powerFactor = it },
                        label = { Text(if (arabic) "معامل القدرة" else "Power factor") },
                        singleLine = true
                    )
                }
                if (node.type == SldNodeType.TRANSFORMER) {
                    OutlinedTextField(
                        value = ratedKva,
                        onValueChange = { ratedKva = it },
                        label = { Text(if (arabic) "قدرة المحول (kVA)" else "Transformer rating (kVA)") },
                        singleLine = true
                    )
                }
                if (node.type == SldNodeType.BREAKER) {
                    OutlinedTextField(
                        value = ratedCurrent,
                        onValueChange = { ratedCurrent = it },
                        label = { Text(if (arabic) "تيار القاطع (A)" else "Breaker rating (A)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = faultRating,
                        onValueChange = { faultRating = it },
                        label = { Text(if (arabic) "قدرة القطع (kA)" else "Breaking capacity (kA)") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedVoltage = voltage.toDoubleOrNull()
                    val parsedLoad = loadKw.toDoubleOrNull()
                    val parsedPf = powerFactor.toDoubleOrNull()
                    val parsedKva = ratedKva.toDoubleOrNull()
                    val parsedCurrent = ratedCurrent.toDoubleOrNull()
                    val parsedFault = faultRating.toDoubleOrNull()

                    if (
                        name.isBlank() ||
                        parsedVoltage == null || parsedVoltage <= 0.0 ||
                        (node.type == SldNodeType.LOAD &&
                            (parsedLoad == null || parsedLoad < 0.0 ||
                                parsedPf == null || parsedPf <= 0.0 || parsedPf > 1.0)) ||
                        (node.type == SldNodeType.TRANSFORMER &&
                            (parsedKva == null || parsedKva <= 0.0)) ||
                        (node.type == SldNodeType.BREAKER &&
                            (parsedCurrent == null || parsedCurrent <= 0.0 ||
                                parsedFault == null || parsedFault <= 0.0))
                    ) {
                        return@TextButton
                    }

                    onSave(
                        node.copy(
                            name = name.trim(),
                            tag = name.trim(),
                            voltage = parsedVoltage,
                            loadKw = parsedLoad ?: node.loadKw,
                            powerFactor = parsedPf ?: node.powerFactor,
                            ratedKva = parsedKva ?: node.ratedKva,
                            ratedCurrentA = parsedCurrent ?: node.ratedCurrentA,
                            breakerFrameA = parsedCurrent ?: node.breakerFrameA,
                            shortCircuitRatingKA = parsedFault ?: node.shortCircuitRatingKA
                        )
                    )
                }
            ) {
                Text(if (arabic) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (arabic) "إلغاء" else "Cancel")
            }
        }
    )
}

private fun sldTypeLabel(
    type: SldNodeType,
    arabic: Boolean
): String {
    return when (type) {
        SldNodeType.SOURCE -> if (arabic) "مصدر" else "Source"
        SldNodeType.TRANSFORMER -> if (arabic) "محول" else "Transformer"
        SldNodeType.GENERATOR -> if (arabic) "مولد" else "Generator"
        SldNodeType.BUS -> if (arabic) "Busbar" else "Busbar"
        SldNodeType.PANEL -> if (arabic) "لوحة" else "Panel"
        SldNodeType.BREAKER -> if (arabic) "قاطع" else "Breaker"
        SldNodeType.LOAD -> if (arabic) "حمل" else "Load"
    }
}
