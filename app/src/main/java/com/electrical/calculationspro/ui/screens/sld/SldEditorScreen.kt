// app/src/main/java/com/electrical/calculationspro/ui/screens/sld/SldEditorScreen.kt

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val SLD_NODE_WIDTH = 180f
private const val SLD_NODE_HEIGHT = 100f
private const val SLD_CANVAS_WIDTH = 2600
private const val SLD_CANVAS_HEIGHT = 1800

private const val SLD_SYMBOL_CENTER_Y = 25f
private const val SLD_PANEL_MARGIN_X = 24f
private const val SLD_PANEL_MARGIN_Y = 28f
private const val SLD_PANEL_MIN_WIDTH = 240f
private const val SLD_PANEL_MIN_HEIGHT = 190f

private data class PanelDrawingGeometry(
    val panelId: String,
    val memberIds: Set<String>,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val busY: Float,
    val busStartX: Float,
    val busEndX: Float,
    val breakerIds: List<String>
)

private data class SymbolBounds(
    val center: Offset,
    val halfWidth: Float,
    val halfHeight: Float
)

private fun buildPanelDrawingGeometry(
    network: SldNetwork
): List<PanelDrawingGeometry> {
    val nodesById = network.nodes.associateBy { it.id }

    val busbarConnections = network.connections.filter {
        it.connectionType == SldConnectionType.BUSBAR
    }

    return network.nodes
        .filter { it.type == SldNodeType.PANEL }
        .mapNotNull { panel ->
            val members = linkedSetOf(panel.id)
            var changed: Boolean

            do {
                changed = false

                busbarConnections.forEach { connection ->
                    val from = nodesById[connection.fromNodeId]
                        ?: return@forEach

                    val to = nodesById[connection.toNodeId]
                        ?: return@forEach

                    val allowedPair =
                        (from.type == SldNodeType.PANEL &&
                            to.type == SldNodeType.BUS) ||
                        (from.type == SldNodeType.BUS &&
                            to.type == SldNodeType.PANEL) ||
                        (from.type == SldNodeType.BUS &&
                            to.type == SldNodeType.BREAKER) ||
                        (from.type == SldNodeType.BREAKER &&
                            to.type == SldNodeType.BUS) ||
                        (from.type == SldNodeType.PANEL &&
                            to.type == SldNodeType.BREAKER) ||
                        (from.type == SldNodeType.BREAKER &&
                            to.type == SldNodeType.PANEL)

                    if (!allowedPair) return@forEach

                    if (from.id in members && to.id !in members) {
                        members.add(to.id)
                        changed = true
                    }

                    if (to.id in members && from.id !in members) {
                        members.add(from.id)
                        changed = true
                    }
                }
            } while (changed)

            val memberNodes = members.mapNotNull(nodesById::get)

            if (memberNodes.isEmpty()) {
                return@mapNotNull null
            }

            val breakers = memberNodes
                .filter { it.type == SldNodeType.BREAKER }
                .sortedWith(
                    compareBy<SldNode> { it.x }
                        .thenBy { it.y }
                        .thenBy { it.name }
                )

            val busNodes = memberNodes.filter {
                it.type == SldNodeType.BUS
            }

            val minX = memberNodes.minOf { it.x }
            val minY = memberNodes.minOf { it.y }

            val maxX = memberNodes.maxOf {
                it.x + SLD_NODE_WIDTH
            }

            val maxY = memberNodes.maxOf {
                it.y + SLD_NODE_HEIGHT
            }

            val busY = when {
                busNodes.isNotEmpty() ->
                    busNodes.map {
                        it.y + SLD_SYMBOL_CENTER_Y
                    }.average().toFloat()

                breakers.isNotEmpty() ->
                    breakers.minOf {
                        it.y + SLD_SYMBOL_CENTER_Y
                    }

                else ->
                    panel.y + SLD_SYMBOL_CENTER_Y
            }

            val busReferenceXs =
                listOf(panel.x + SLD_NODE_WIDTH / 2f) +
                    busNodes.map {
                        it.x + SLD_NODE_WIDTH / 2f
                    } +
                    breakers.map {
                        it.x + SLD_NODE_WIDTH / 2f
                    }

            PanelDrawingGeometry(
                panelId = panel.id,
                memberIds = members,
                left = minX - SLD_PANEL_MARGIN_X,
                top = minY - SLD_PANEL_MARGIN_Y,
                right = maxOf(
                    minX - SLD_PANEL_MARGIN_X +
                        SLD_PANEL_MIN_WIDTH,
                    maxX + SLD_PANEL_MARGIN_X
                ),
                bottom = maxOf(
                    minY - SLD_PANEL_MARGIN_Y +
                        SLD_PANEL_MIN_HEIGHT,
                    maxY + SLD_PANEL_MARGIN_Y
                ),
                busY = busY,
                busStartX = minOf(
                    busReferenceXs.minOrNull() ?: panel.x,
                    minX + SLD_PANEL_MARGIN_X
                ),
                busEndX = maxOf(
                    busReferenceXs.maxOrNull()
                        ?: (panel.x + SLD_NODE_WIDTH),
                    maxX - SLD_PANEL_MARGIN_X
                ),
                breakerIds = breakers.map { it.id }
            )
        }
}

/**
 * The connection point is the center of the equipment symbol,
 * not the center of the entire label area.
 *
 * Node coordinates are stored in dp units. Conversion to pixels
 * happens only when drawing on the Canvas.
 */
private fun nodeCenter(node: SldNode): Offset {
    return Offset(
        x = node.x + SLD_NODE_WIDTH / 2f,
        y = node.y + SLD_SYMBOL_CENTER_Y
    )
}

private fun isBreakerHorizontal(
    node: SldNode,
    network: SldNetwork
): Boolean {
    if (node.type != SldNodeType.BREAKER) return false

    val nodesById = network.nodes.associateBy { it.id }

    val connectedNodes = network.connections
        .filter {
            it.connectionType == SldConnectionType.CABLE &&
                (
                    it.fromNodeId == node.id ||
                        it.toNodeId == node.id
                    )
        }
        .mapNotNull { connection ->
            val otherId = if (connection.fromNodeId == node.id) {
                connection.toNodeId
            } else {
                connection.fromNodeId
            }

            nodesById[otherId]
        }

    if (connectedNodes.isEmpty()) return false

    val center = nodeCenter(node)

    val horizontalCount = connectedNodes.count { connected ->
        val other = nodeCenter(connected)

        abs(other.x - center.x) >=
            abs(other.y - center.y)
    }

    val verticalCount = connectedNodes.size - horizontalCount

    return horizontalCount >= verticalCount
}

private fun symbolBounds(
    node: SldNode,
    network: SldNetwork
): SymbolBounds {
    val center = nodeCenter(node)

    return when (node.type) {
        SldNodeType.SOURCE,
        SldNodeType.GENERATOR -> {
            SymbolBounds(
                center = center,
                halfWidth = 25f,
                halfHeight = 25f
            )
        }

        SldNodeType.TRANSFORMER -> {
            SymbolBounds(
                center = center,
                halfWidth = 35f,
                halfHeight = 22f
            )
        }

        SldNodeType.BUS -> {
            SymbolBounds(
                center = center,
                halfWidth = SLD_NODE_WIDTH / 2f - 15f,
                halfHeight = 4f
            )
        }

        SldNodeType.PANEL,
        SldNodeType.LOAD -> {
            SymbolBounds(
                center = center,
                halfWidth = SLD_NODE_WIDTH / 2f - 20f,
                halfHeight = 21f
            )
        }

        SldNodeType.BREAKER -> {
            if (isBreakerHorizontal(node, network)) {
                SymbolBounds(
                    center = center,
                    halfWidth = 25f,
                    halfHeight = 19f
                )
            } else {
                SymbolBounds(
                    center = center,
                    halfWidth = 19f,
                    halfHeight = 24f
                )
            }
        }
    }
}

/**
 * Returns the intersection of the line between two symbol centers
 * and the visible boundary of the first symbol.
 */
private fun connectionBoundaryPoint(
    node: SldNode,
    target: SldNode,
    network: SldNetwork
): Offset {
    val bounds = symbolBounds(node, network)
    val center = bounds.center
    val targetCenter = nodeCenter(target)

    val dx = targetCenter.x - center.x
    val dy = targetCenter.y - center.y

    if (dx == 0f && dy == 0f) return center

    val scaleX = if (abs(dx) > 0.0001f) {
        bounds.halfWidth / abs(dx)
    } else {
        Float.POSITIVE_INFINITY
    }

    val scaleY = if (abs(dy) > 0.0001f) {
        bounds.halfHeight / abs(dy)
    } else {
        Float.POSITIVE_INFINITY
    }

    val scale = minOf(scaleX, scaleY)

    return Offset(
        x = center.x + dx * scale,
        y = center.y + dy * scale
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(
    tip: Offset,
    previous: Offset,
    color: Color,
    strokeWidth: Float
) {
    val angle = atan2(
        tip.y - previous.y,
        tip.x - previous.x
    )

    val arrowLength = 10.dp.toPx()
    val wingAngle = Math.toRadians(150.0).toFloat()

    val left = Offset(
        x = tip.x + arrowLength * cos(angle + wingAngle),
        y = tip.y + arrowLength * sin(angle + wingAngle)
    )

    val right = Offset(
        x = tip.x + arrowLength * cos(angle - wingAngle),
        y = tip.y + arrowLength * sin(angle - wingAngle)
    )

    drawLine(
        color = color,
        start = tip,
        end = left,
        strokeWidth = strokeWidth
    )

    drawLine(
        color = color,
        start = tip,
        end = right,
        strokeWidth = strokeWidth
    )
}

/**
 * Draws a directed cable from fromNodeId to toNodeId.
 * The arrow always points to the destination node.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDirectedCable(
    from: SldNode,
    to: SldNode,
    network: SldNetwork
) {
    val startDp = connectionBoundaryPoint(from, to, network)
    val endDp = connectionBoundaryPoint(to, from, network)

    val start = Offset(
        x = startDp.x.dp.toPx(),
        y = startDp.y.dp.toPx()
    )

    val end = Offset(
        x = endDp.x.dp.toPx(),
        y = endDp.y.dp.toPx()
    )

    val dx = end.x - start.x
    val dy = end.y - start.y

    val strokeWidth = 2.dp.toPx()
    val color = Color(0xFF263746)

    val path = Path()
    val arrowPrevious: Offset

    if (abs(dx) >= abs(dy)) {
        val midX = (start.x + end.x) / 2f

        path.moveTo(start.x, start.y)
        path.lineTo(midX, start.y)
        path.lineTo(midX, end.y)
        path.lineTo(end.x, end.y)

        arrowPrevious = if (abs(end.x - midX) > 1f) {
            Offset(midX, end.y)
        } else {
            Offset(midX, start.y)
        }
    } else {
        val midY = (start.y + end.y) / 2f

        path.moveTo(start.x, start.y)
        path.lineTo(start.x, midY)
        path.lineTo(end.x, midY)
        path.lineTo(end.x, end.y)

        arrowPrevious = if (abs(end.y - midY) > 1f) {
            Offset(end.x, midY)
        } else {
            Offset(start.x, midY)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )

    drawArrowHead(
        tip = end,
        previous = arrowPrevious,
        color = color,
        strokeWidth = strokeWidth
    )
}

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {
    val arabic = language == AppLanguage.ARABIC

    var network by remember {
        mutableStateOf(SldNetwork())
    }

    var selectedNodeId by remember {
        mutableStateOf<String?>(null)
    }

    var connectionStartId by remember {
        mutableStateOf<String?>(null)
    }

    var connectMode by remember {
        mutableStateOf(false)
    }

    var connectionType by remember {
        mutableStateOf(SldConnectionType.CABLE)
    }

    var showConnectionMenu by remember {
        mutableStateOf(false)
    }

    var showNodeDialog by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf(
            if (arabic) {
                "أنشئ مصدر التغذية ثم أضف العناصر ووصلها."
            } else {
                "Add a source, place equipment, and connect the network."
            }
        )
    }

    var calculationSummary by remember {
        mutableStateOf<String?>(null)
    }

    val selectedNode = network.nodes.firstOrNull {
        it.id == selectedNodeId
    }

    val validation = remember(network) {
        SldDesignValidator.validate(network)
    }

    fun addNode(type: SldNodeType) {
        if (
            type == SldNodeType.SOURCE &&
            network.nodes.any {
                it.type == SldNodeType.SOURCE
            }
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
            SldNodeType.SOURCE ->
                if (arabic) "مصدر" else "SOURCE"

            SldNodeType.TRANSFORMER ->
                if (arabic) "محول" else "TR"

            SldNodeType.GENERATOR ->
                if (arabic) "مولد" else "GEN"

            SldNodeType.BUS ->
                if (arabic) "قضبان" else "BUS"

            SldNodeType.PANEL ->
                if (arabic) "لوحة" else "MDB"

            SldNodeType.BREAKER ->
                if (arabic) "قاطع" else "CB"

            SldNodeType.LOAD ->
                if (arabic) "حمل" else "LOAD"
        }

        val sameTypeCount =
            network.nodes.count {
                it.type == type
            } + 1

        val node = SldNode(
            id = UUID.randomUUID().toString(),
            name = "$nameBase-$sameTypeCount",
            type = type,
            x = 120f + column * 300f,
            y = 120f + row * 220f,
            voltage = network.nodes.firstOrNull()?.voltage ?: 400.0,
            loadKw = if (type == SldNodeType.LOAD) {
                10.0
            } else {
                0.0
            },
            ratedKva = if (type == SldNodeType.TRANSFORMER) {
                500.0
            } else {
                0.0
            },
            ratedCurrentA = if (type == SldNodeType.BREAKER) {
                100.0
            } else {
                0.0
            },
            breakerFrameA = if (type == SldNodeType.BREAKER) {
                100.0
            } else {
                0.0
            },
            shortCircuitRatingKA = if (type == SldNodeType.BREAKER) {
                25.0
            } else {
                0.0
            }
        )

        network = network.copy(
            nodes = network.nodes + node
        )

        selectedNodeId = node.id
        connectionStartId = null
        calculationSummary = null

        status = if (arabic) {
            "تمت إضافة ${node.name}. اختر العنصر ثم عدّل بياناته أو ابدأ التوصيل."
        } else {
            "${node.name} added. Select it to edit its data or start connecting."
        }
    }

    fun connectNodes(
        firstId: String,
        secondId: String
    ) {
        if (firstId == secondId) {
            status = if (arabic) {
                "لا يمكن توصيل العنصر بنفسه."
            } else {
                "An element cannot connect to itself."
            }
            return
        }

        val first = network.nodes.firstOrNull {
            it.id == firstId
        }

        val second = network.nodes.firstOrNull {
            it.id == secondId
        }

        if (first == null || second == null) {
            status = if (arabic) {
                "العنصر غير موجود."
            } else {
                "Equipment was not found."
            }
            return
        }

        /*
         * A physical connection must not be duplicated simply
         * by reversing the order in which its endpoints are selected.
         *
         * The stored direction of an existing connection is preserved.
         */
        val duplicateConnection = network.connections.any { existing ->
            existing.connectionType == connectionType &&
                (
                    (
                        existing.fromNodeId == firstId &&
                            existing.toNodeId == secondId
                        ) ||
                    (
                        existing.fromNodeId == secondId &&
                            existing.toNodeId == firstId
                        )
                    )
        }

        if (duplicateConnection) {
            status = if (arabic) {
                "هذا التوصيل موجود بالفعل بين العنصرين."
            } else {
                "A connection of this type already exists between these elements."
            }
            return
        }

        if (connectionType == SldConnectionType.BUSBAR) {
            val validPair =
                (first.type == SldNodeType.PANEL &&
                    second.type == SldNodeType.BUS) ||
                (first.type == SldNodeType.BUS &&
                    second.type == SldNodeType.PANEL) ||
                (first.type == SldNodeType.PANEL &&
                    second.type == SldNodeType.BREAKER) ||
                (first.type == SldNodeType.BREAKER &&
                    second.type == SldNodeType.PANEL) ||
                (first.type == SldNodeType.BUS &&
                    second.type == SldNodeType.BREAKER) ||
                (first.type == SldNodeType.BREAKER &&
                    second.type == SldNodeType.BUS)

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
            busbarRatedCurrentA = if (
                connectionType == SldConnectionType.BUSBAR
            ) {
                400.0
            } else {
                0.0
            },
            busbarShortCircuitKA = if (
                connectionType == SldConnectionType.BUSBAR
            ) {
                25.0
            } else {
                0.0
            }
        )

        network = network.copy(
            connections = network.connections + connection
        )

        calculationSummary = null

        status = if (arabic) {
            "تم التوصيل من ${first.name} إلى ${second.name}. اتجاه السهم من الأول إلى الثاني."
        } else {
            "Connected ${first.name} to ${second.name}. Arrow direction is first to second."
        }
    }

    fun handleNodeTap(nodeId: String) {
        selectedNodeId = nodeId

        if (!connectMode) return

        val start = connectionStartId

        if (start == null) {
            connectionStartId = nodeId

            val startNode = network.nodes.firstOrNull {
                it.id == nodeId
            }

            status = if (arabic) {
                "بداية التوصيل: ${startNode?.name ?: "العنصر"}. اختر العنصر التالي."
            } else {
                "Connection start: ${startNode?.name ?: "element"}. Select the next element."
            }

            return
        }

        if (start == nodeId) {
            status = if (arabic) {
                "اختر عنصرًا مختلفًا لإكمال التوصيل."
            } else {
                "Select a different element to complete the connection."
            }
            return
        }

        connectNodes(start, nodeId)
        connectionStartId = null
    }

    fun moveNode(
        nodeId: String,
        dx: Float,
        dy: Float
    ) {
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

    val currentNetwork by rememberUpdatedState(network)

    val currentMoveNode by rememberUpdatedState<
        (String, Float, Float) -> Unit
    >(
        { nodeId, dx, dy ->
            moveNode(nodeId, dx, dy)
        }
    )

    val currentHandleNodeTap by rememberUpdatedState<
        (String) -> Unit
    >(
        { nodeId ->
            handleNodeTap(nodeId)
        }
    )

    val currentConnectMode by rememberUpdatedState(connectMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = if (arabic) {
                            "رجوع"
                        } else {
                            "Back"
                        }
                    )
                }
            }

            Icon(
                Icons.Outlined.AccountTree,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (arabic) {
                        "مصمم المخطط الأحادي SLD"
                    } else {
                        "Single Line Diagram Designer"
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    if (arabic) {
                        "محرر الرسم والتوصيلات الهندسية"
                    } else {
                        "Diagram and electrical topology editor"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    val gate =
                        SldEngineeringFacade.checkEngineeringTopology(
                            network
                        )

                    if (!gate.valid) {
                        calculationSummary = null

                        status = gate.errorMessage ?: if (arabic) {
                            "الشبكة غير جاهزة للحساب."
                        } else {
                            "The network is not ready for engineering."
                        }
                    } else {
                        try {
                            val result =
                                SldEngineeringFacade.calculateUpstream(
                                    network
                                )

                            calculationSummary = buildString {
                                appendLine(
                                    if (arabic) {
                                        "نتائج الحساب الأولي"
                                    } else {
                                        "Preliminary calculation"
                                    }
                                )

                                appendLine(
                                    "${if (arabic) "الحمل المتصل" else "Connected load"}: %.2f kW"
                                        .format(result.totalConnectedKw)
                                )

                                appendLine(
                                    "${if (arabic) "حمل الطلب" else "Demand load"}: %.2f kW"
                                        .format(result.totalDemandKw)
                                )

                                appendLine(
                                    "${if (arabic) "القدرة الظاهرية" else "Apparent power"}: %.2f kVA"
                                        .format(result.totalKva)
                                )

                                appendLine(
                                    "${if (arabic) "تيار المصدر" else "Source current"}: %.2f A"
                                        .format(result.sourceCurrentA)
                                )

                                appendLine(
                                    "${if (arabic) "القاطع الرئيسي المقترح" else "Recommended main breaker"}: %.0f A"
                                        .format(result.recommendedMainBreakerA)
                                )

                                appendLine(
                                    "${if (arabic) "المحول المقترح" else "Recommended transformer"}: %.0f kVA"
                                        .format(result.recommendedTransformerKva)
                                )

                                appendLine(
                                    "${if (arabic) "أقصى هبوط جهد" else "Maximum voltage drop"}: %.3f %%"
                                        .format(result.maximumVoltageDropPercent)
                                )

                                if (result.warnings.isNotEmpty()) {
                                    appendLine()
                                    appendLine(
                                        if (arabic) {
                                            "ملاحظات هندسية:"
                                        } else {
                                            "Engineering notes:"
                                        }
                                    )

                                    result.warnings.forEach {
                                        appendLine("• $it")
                                    }
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
                Icon(
                    Icons.Outlined.Calculate,
                    contentDescription = null
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    if (arabic) "حساب" else "Calculate"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SldNodeType.values().forEach { type ->
                OutlinedButton(
                    onClick = {
                        addNode(type)
                    }
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        sldTypeLabel(type, arabic)
                    )
                }
            }

            Box {
                OutlinedButton(
                    onClick = {
                        showConnectionMenu = true
                    }
                ) {
                    Icon(
                        Icons.Outlined.AccountTree,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        when (connectionType) {
                            SldConnectionType.CABLE ->
                                if (arabic) {
                                    "نوع التوصيل: كابل"
                                } else {
                                    "Connection: Cable"
                                }

                            SldConnectionType.BUSBAR ->
                                if (arabic) {
                                    "نوع التوصيل: Busbar"
                                } else {
                                    "Connection: Busbar"
                                }
                        }
                    )
                }

                DropdownMenu(
                    expanded = showConnectionMenu,
                    onDismissRequest = {
                        showConnectionMenu = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (arabic) {
                                    "مغذي كابل"
                                } else {
                                    "Cable feeder"
                                }
                            )
                        },
                        onClick = {
                            connectionType =
                                SldConnectionType.CABLE

                            showConnectionMenu = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (arabic) {
                                    "توصيل داخلي Busbar"
                                } else {
                                    "Internal busbar"
                                }
                            )
                        },
                        onClick = {
                            connectionType =
                                SldConnectionType.BUSBAR

                            showConnectionMenu = false
                        }
                    )
                }
            }

            Button(
                onClick = {
                    val enablingConnectionMode = !connectMode

                    connectMode = enablingConnectionMode
                    connectionStartId = null

                    status = if (enablingConnectionMode) {
                        if (arabic) {
                            "وضع التوصيل مفعل: اضغط العنصر الأول ثم العنصر الثاني. اتجاه السهم من الأول إلى الثاني."
                        } else {
                            "Connection mode enabled: tap the first element, then the second. Arrow points from first to second."
                        }
                    } else {
                        if (arabic) {
                            "تم إيقاف وضع التوصيل."
                        } else {
                            "Connection mode disabled."
                        }
                    }
                }
            ) {
                Text(
                    if (connectMode) {
                        if (arabic) {
                            "إنهاء التوصيل"
                        } else {
                            "Finish connecting"
                        }
                    } else {
                        if (arabic) {
                            "بدء التوصيل"
                        } else {
                            "Connect"
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    if (network.nodes.isNotEmpty()) {
                        val arranged =
                            SldAutoLayoutEngine.arrange(network)

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
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    if (arabic) {
                        "ترتيب آلي"
                    } else {
                        "Auto layout"
                    }
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                Modifier.padding(10.dp)
            ) {
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

        calculationSummary?.let { summary ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    summary,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 2.dp
                ),
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
                onClick = {
                    showNodeDialog = true
                }
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    if (arabic) "تعديل" else "Edit"
                )
            }

            OutlinedButton(
                enabled = selectedNode != null,
                onClick = {
                    val id = selectedNodeId

                    if (id != null) {
                        network = network.copy(
                            nodes = network.nodes.filterNot {
                                it.id == id
                            },
                            connections = network.connections.filterNot {
                                it.fromNodeId == id ||
                                    it.toNodeId == id
                            }
                        )

                        selectedNodeId = null
                        connectionStartId = null
                        calculationSummary = null

                        status = if (arabic) {
                            "تم حذف العنصر وتوصيلاته."
                        } else {
                            "Equipment and its connections removed."
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    if (arabic) "حذف" else "Delete"
                )
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
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                Canvas(
                    modifier = Modifier
                        .width(SLD_CANVAS_WIDTH.dp)
                        .height(SLD_CANVAS_HEIGHT.dp)
                        .pointerInput(density.density) {
                            detectTapGestures { tap ->
                                val tapX =
                                    tap.x / density.density

                                val tapY =
                                    tap.y / density.density

                                val hit = currentNetwork.nodes
                                    .asReversed()
                                    .firstOrNull { node ->
                                        tapX >= node.x - 8f &&
                                            tapX <= node.x +
                                            SLD_NODE_WIDTH + 8f &&
                                            tapY >= node.y - 8f &&
                                            tapY <= node.y +
                                            SLD_NODE_HEIGHT + 8f
                                    }

                                if (hit != null) {
                                    currentHandleNodeTap(hit.id)
                                }
                            }
                        }
                        .pointerInput(density.density) {
                            var dragNodeId: String? = null

                            detectDragGestures(
                                onDragStart = { point ->
                                    val x =
                                        point.x / density.density

                                    val y =
                                        point.y / density.density

                                    val hit = currentNetwork.nodes
                                        .asReversed()
                                        .firstOrNull { node ->
                                            x >= node.x - 8f &&
                                                x <= node.x +
                                                SLD_NODE_WIDTH + 8f &&
                                                y >= node.y - 8f &&
                                                y <= node.y +
                                                SLD_NODE_HEIGHT + 8f
                                        }

                                    dragNodeId = hit?.id

                                    if (
                                        hit != null &&
                                        !currentConnectMode
                                    ) {
                                        selectedNodeId = hit.id
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    if (!currentConnectMode) {
                                        dragNodeId?.let { id ->
                                            currentMoveNode(
                                                id,
                                                dragAmount.x /
                                                    density.density,
                                                dragAmount.y /
                                                    density.density
                                            )
                                        }
                                    }
                                },
                                onDragEnd = {
                                    dragNodeId = null
                                },
                                onDragCancel = {
                                    dragNodeId = null
                                }
                            )
                        }
                ) {
                    val gridColor = Color(0xFFE0E5EB)
                    val gridStep = 40.dp.toPx()

                    var gridX = 0f

                    while (gridX < size.width) {
                        drawLine(
                            color = gridColor,
                            start = Offset(gridX, 0f),
                            end = Offset(gridX, size.height),
                            strokeWidth = 1f
                        )

                        gridX += gridStep
                    }

                    var gridY = 0f

                    while (gridY < size.height) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, gridY),
                            end = Offset(size.width, gridY),
                            strokeWidth = 1f
                        )

                        gridY += gridStep
                    }

                    val nodesById =
                        network.nodes.associateBy { it.id }

                    val panelGeometries =
                        buildPanelDrawingGeometry(network)

                    panelGeometries.forEach { geometry ->
                        drawRect(
                            color = Color(0xFF687987),
                            topLeft = Offset(
                                geometry.left.dp.toPx(),
                                geometry.top.dp.toPx()
                            ),
                            size = Size(
                                (geometry.right -
                                    geometry.left).dp.toPx(),
                                (geometry.bottom -
                                    geometry.top).dp.toPx()
                            ),
                            style = Stroke(
                                width = 2.dp.toPx()
                            )
                        )

                        drawLine(
                            color = Color(0xFFB66A16),
                            start = Offset(
                                geometry.busStartX.dp.toPx(),
                                geometry.busY.dp.toPx()
                            ),
                            end = Offset(
                                geometry.busEndX.dp.toPx(),
                                geometry.busY.dp.toPx()
                            ),
                            strokeWidth = 5.dp.toPx()
                        )

                        geometry.breakerIds.forEach { breakerId ->
                            val breaker =
                                nodesById[breakerId]
                                    ?: return@forEach

                            val branchX =
                                (
                                    breaker.x +
                                        SLD_NODE_WIDTH / 2f
                                    ).dp.toPx()

                            val branchY =
                                (
                                    breaker.y +
                                        SLD_SYMBOL_CENTER_Y
                                    ).dp.toPx()

                            drawLine(
                                color = Color(0xFFB66A16),
                                start = Offset(
                                    branchX,
                                    geometry.busY.dp.toPx()
                                ),
                                end = Offset(
                                    branchX,
                                    branchY
                                ),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }

                    /*
                     * BUSBAR connections are represented by the
                     * panel bus and branches. Cable connections are
                     * rendered independently with their stored direction.
                     */
                    network.connections.forEach connectionLoop@{ connection ->
                        if (
                            connection.connectionType ==
                            SldConnectionType.BUSBAR
                        ) {
                            return@connectionLoop
                        }

                        val from =
                            nodesById[connection.fromNodeId]
                                ?: return@connectionLoop

                        val to =
                            nodesById[connection.toNodeId]
                                ?: return@connectionLoop

                        drawDirectedCable(
                            from = from,
                            to = to,
                            network = network
                        )
                    }

                    network.nodes.forEach { node ->
                        val left = node.x.dp.toPx()
                        val top = node.y.dp.toPx()
                        val w = SLD_NODE_WIDTH.dp.toPx()
                        val h = SLD_NODE_HEIGHT.dp.toPx()

                        val symbolCenter = Offset(
                            left + w / 2f,
                            top + SLD_SYMBOL_CENTER_Y.dp.toPx()
                        )

                        val isSelected =
                            node.id == selectedNodeId

                        val isConnectionStart =
                            node.id == connectionStartId

                        val borderColor = when {
                            isConnectionStart ->
                                Color(0xFF008577)

                            isSelected ->
                                Color(0xFF1976D2)

                            else ->
                                Color(0xFF34495E)
                        }

                        val fillColor = when {
                            isConnectionStart ->
                                Color(0xFFD5F5F0)

                            isSelected ->
                                Color(0xFFE3F2FD)

                            else ->
                                Color.White
                        }

                        when (node.type) {
                            SldNodeType.SOURCE -> {
                                drawCircle(
                                    color = fillColor,
                                    radius = 25.dp.toPx(),
                                    center = symbolCenter
                                )

                                drawCircle(
                                    color = borderColor,
                                    radius = 25.dp.toPx(),
                                    center = symbolCenter,
                                    style = Stroke(
                                        width = 2.dp.toPx()
                                    )
                                )

                                drawLine(
                                    color = borderColor,
                                    start = Offset(
                                        symbolCenter.x -
                                            12.dp.toPx(),
                                        symbolCenter.y
                                    ),
                                    end = Offset(
                                        symbolCenter.x +
                                            12.dp.toPx(),
                                        symbolCenter.y
                                    ),
                                    strokeWidth = 2.dp.toPx()
                                )

                                drawLine(
                                    color = borderColor,
                                    start = Offset(
                                        symbolCenter.x,
                                        symbolCenter.y -
                                            12.dp.toPx()
                                    ),
                                    end = Offset(
                                        symbolCenter.x,
                                        symbolCenter.y +
                                            12.dp.toPx()
                                    ),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            SldNodeType.TRANSFORMER -> {
                                drawCircle(
                                    color = borderColor,
                                    radius = 22.dp.toPx(),
                                    center = Offset(
                                        symbolCenter.x -
                                            13.dp.toPx(),
                                        symbolCenter.y
                                    ),
                                    style = Stroke(
                                        width = 2.dp.toPx()
                                    )
                                )

                                drawCircle(
                                    color = borderColor,
                                    radius = 22.dp.toPx(),
                                    center = Offset(
                                        symbolCenter.x +
                                            13.dp.toPx(),
                                        symbolCenter.y
                                    ),
                                    style = Stroke(
                                        width = 2.dp.toPx()
                                    )
                                )
                            }

                            SldNodeType.BUS -> {
                                drawLine(
                                    color = Color(0xFFB66A16),
                                    start = Offset(
                                        left + 15.dp.toPx(),
                                        symbolCenter.y
                                    ),
                                    end = Offset(
                                        left + w -
                                            15.dp.toPx(),
                                        symbolCenter.y
                                    ),
                                    strokeWidth = 6.dp.toPx()
                                )
                            }

                            SldNodeType.BREAKER -> {
                                val horizontal =
                                    isBreakerHorizontal(
                                        node,
                                        network
                                    )

                                if (horizontal) {
                                    val breakerLeft =
                                        symbolCenter.x -
                                            25.dp.toPx()

                                    val breakerTop =
                                        symbolCenter.y -
                                            19.dp.toPx()

                                    drawRect(
                                        color = fillColor,
                                        topLeft = Offset(
                                            breakerLeft,
                                            breakerTop
                                        ),
                                        size = Size(
                                            50.dp.toPx(),
                                            38.dp.toPx()
                                        )
                                    )

                                    drawRect(
                                        color = borderColor,
                                        topLeft = Offset(
                                            breakerLeft,
                                            breakerTop
                                        ),
                                        size = Size(
                                            50.dp.toPx(),
                                            38.dp.toPx()
                                        ),
                                        style = Stroke(
                                            width = 2.dp.toPx()
                                        )
                                    )

                                    drawLine(
                                        color = borderColor,
                                        start = Offset(
                                            symbolCenter.x -
                                                13.dp.toPx(),
                                            symbolCenter.y +
                                                8.dp.toPx()
                                        ),
                                        end = Offset(
                                            symbolCenter.x +
                                                13.dp.toPx(),
                                            symbolCenter.y -
                                                10.dp.toPx()
                                        ),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                } else {
                                    val breakerLeft =
                                        symbolCenter.x -
                                            19.dp.toPx()

                                    val breakerTop =
                                        symbolCenter.y -
                                            24.dp.toPx()

                                    drawRect(
                                        color = fillColor,
                                        topLeft = Offset(
                                            breakerLeft,
                                            breakerTop
                                        ),
                                        size = Size(
                                            38.dp.toPx(),
                                            48.dp.toPx()
                                        )
                                    )

                                    drawRect(
                                        color = borderColor,
                                        topLeft = Offset(
                                            breakerLeft,
                                            breakerTop
                                        ),
                                        size = Size(
                                            38.dp.toPx(),
                                            48.dp.toPx()
                                        ),
                                        style = Stroke(
                                            width = 2.dp.toPx()
                                        )
                                    )

                                    drawLine(
                                        color = borderColor,
                                        start = Offset(
                                            symbolCenter.x -
                                                12.dp.toPx(),
                                            symbolCenter.y +
                                                12.dp.toPx()
                                        ),
                                        end = Offset(
                                            symbolCenter.x +
                                                12.dp.toPx(),
                                            symbolCenter.y -
                                                12.dp.toPx()
                                        ),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }

                            SldNodeType.GENERATOR -> {
                                drawCircle(
                                    color = fillColor,
                                    radius = 25.dp.toPx(),
                                    center = symbolCenter
                                )

                                drawCircle(
                                    color = borderColor,
                                    radius = 25.dp.toPx(),
                                    center = symbolCenter,
                                    style = Stroke(
                                        width = 2.dp.toPx()
                                    )
                                )

                                drawLine(
                                    color = borderColor,
                                    start = Offset(
                                        symbolCenter.x -
                                            12.dp.toPx(),
                                        symbolCenter.y
                                    ),
                                    end = Offset(
                                        symbolCenter.x +
                                            12.dp.toPx(),
                                        symbolCenter.y
                                    ),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            SldNodeType.PANEL,
                            SldNodeType.LOAD -> {
                                drawRect(
                                    color = fillColor,
                                    topLeft = Offset(
                                        left + 20.dp.toPx(),
                                        top + 4.dp.toPx()
                                    ),
                                    size = Size(
                                        w - 40.dp.toPx(),
                                        42.dp.toPx()
                                    )
                                )

                                drawRect(
                                    color = borderColor,
                                    topLeft = Offset(
                                        left + 20.dp.toPx(),
                                        top + 4.dp.toPx()
                                    ),
                                    size = Size(
                                        w - 40.dp.toPx(),
                                        42.dp.toPx()
                                    ),
                                    style = Stroke(
                                        width = 2.dp.toPx()
                                    )
                                )
                            }
                        }

                        val paint = AndroidPaint().apply {
                            color = android.graphics.Color.rgb(
                                30,
                                41,
                                51
                            )
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
                            SldNodeType.LOAD ->
                                "%.1f kW".format(node.loadKw)

                            SldNodeType.TRANSFORMER ->
                                if (node.ratedKva > 0) {
                                    "${node.ratedKva.toInt()} kVA"
                                } else {
                                    "${node.voltage.toInt()} V"
                                }

                            SldNodeType.BREAKER ->
                                if (node.ratedCurrentA > 0) {
                                    "${node.ratedCurrentA.toInt()} A"
                                } else {
                                    "Rating not set"
                                }

                            else ->
                                "${node.voltage.toInt()} V"
                        }

                        paint.textSize = 11.dp.toPx()

                        drawContext.canvas.nativeCanvas.drawText(
                            detail,
                            left + w / 2f,
                            top + 84.dp.toPx(),
                            paint
                        )

                        if (isSelected || isConnectionStart) {
                            drawRect(
                                color = borderColor,
                                topLeft = Offset(
                                    left - 4.dp.toPx(),
                                    top - 4.dp.toPx()
                                ),
                                size = Size(
                                    w + 8.dp.toPx(),
                                    h + 8.dp.toPx()
                                ),
                                style = Stroke(
                                    width = 2.dp.toPx()
                                )
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
            onDismiss = {
                showNodeDialog = false
            },
            onSave = { updated ->
                network = network.copy(
                    nodes = network.nodes.map {
                        if (it.id == updated.id) {
                            updated
                        } else {
                            it
                        }
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
    var name by remember(node.id) {
        mutableStateOf(node.name)
    }

    var voltage by remember(node.id) {
        mutableStateOf(node.voltage.toString())
    }

    var loadKw by remember(node.id) {
        mutableStateOf(node.loadKw.toString())
    }

    var powerFactor by remember(node.id) {
        mutableStateOf(node.powerFactor.toString())
    }

    var ratedKva by remember(node.id) {
        mutableStateOf(node.ratedKva.toString())
    }

    var ratedCurrent by remember(node.id) {
        mutableStateOf(node.ratedCurrentA.toString())
    }

    var faultRating by remember(node.id) {
        mutableStateOf(node.shortCircuitRatingKA.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (arabic) {
                    "بيانات العنصر"
                } else {
                    "Equipment properties"
                }
            )
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
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text(
                            if (arabic) {
                                "الاسم / Tag"
                            } else {
                                "Name / Tag"
                            }
                        )
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = voltage,
                    onValueChange = {
                        voltage = it
                    },
                    label = {
                        Text(
                            if (arabic) {
                                "الجهد (V)"
                            } else {
                                "Voltage (V)"
                            }
                        )
                    },
                    singleLine = true
                )

                if (node.type == SldNodeType.LOAD) {
                    OutlinedTextField(
                        value = loadKw,
                        onValueChange = {
                            loadKw = it
                        },
                        label = {
                            Text(
                                if (arabic) {
                                    "الحمل (kW)"
                                } else {
                                    "Load (kW)"
                                }
                            )
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = powerFactor,
                        onValueChange = {
                            powerFactor = it
                        },
                        label = {
                            Text(
                                if (arabic) {
                                    "معامل القدرة"
                                } else {
                                    "Power factor"
                                }
                            )
                        },
                        singleLine = true
                    )
                }

                if (node.type == SldNodeType.TRANSFORMER) {
                    OutlinedTextField(
                        value = ratedKva,
                        onValueChange = {
                            ratedKva = it
                        },
                        label = {
                            Text(
                                if (arabic) {
                                    "قدرة المحول (kVA)"
                                } else {
                                    "Transformer rating (kVA)"
                                }
                            )
                        },
                        singleLine = true
                    )
                }

                if (node.type == SldNodeType.BREAKER) {
                    OutlinedTextField(
                        value = ratedCurrent,
                        onValueChange = {
                            ratedCurrent = it
                        },
                        label = {
                            Text(
                                if (arabic) {
                                    "تيار القاطع (A)"
                                } else {
                                    "Breaker rating (A)"
                                }
                            )
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = faultRating,
                        onValueChange = {
                            faultRating = it
                        },
                        label = {
                            Text(
                                if (arabic) {
                                    "قدرة القطع (kA)"
                                } else {
                                    "Breaking capacity (kA)"
                                }
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
                    val parsedVoltage =
                        voltage.toDoubleOrNull()

                    val parsedLoad =
                        loadKw.toDoubleOrNull()

                    val parsedPf =
                        powerFactor.toDoubleOrNull()

                    val parsedKva =
                        ratedKva.toDoubleOrNull()

                    val parsedCurrent =
                        ratedCurrent.toDoubleOrNull()

                    val parsedFault =
                        faultRating.toDoubleOrNull()

                    if (
                        name.isBlank() ||
                        parsedVoltage == null ||
                        !parsedVoltage.isFinite() ||
                        parsedVoltage <= 0.0 ||
                        (
                            node.type == SldNodeType.LOAD &&
                                (
                                    parsedLoad == null ||
                                        !parsedLoad.isFinite() ||
                                        parsedLoad < 0.0 ||
                                        parsedPf == null ||
                                        !parsedPf.isFinite() ||
                                        parsedPf <= 0.0 ||
                                        parsedPf > 1.0
                                    )
                            ) ||
                        (
                            node.type == SldNodeType.TRANSFORMER &&
                                (
                                    parsedKva == null ||
                                        !parsedKva.isFinite() ||
                                        parsedKva <= 0.0
                                    )
                            ) ||
                        (
                            node.type == SldNodeType.BREAKER &&
                                (
                                    parsedCurrent == null ||
                                        !parsedCurrent.isFinite() ||
                                        parsedCurrent <= 0.0 ||
                                        parsedFault == null ||
                                        !parsedFault.isFinite() ||
                                        parsedFault <= 0.0
                                    )
                            )
                    ) {
                        return@TextButton
                    }

                    onSave(
                        node.copy(
                            name = name.trim(),
                            tag = name.trim(),
                            voltage = parsedVoltage,
                            loadKw = parsedLoad ?: node.loadKw,
                            powerFactor = parsedPf
                                ?: node.powerFactor,
                            ratedKva = parsedKva
                                ?: node.ratedKva,
                            ratedCurrentA = parsedCurrent
                                ?: node.ratedCurrentA,
                            breakerFrameA = parsedCurrent
                                ?: node.breakerFrameA,
                            shortCircuitRatingKA = parsedFault
                                ?: node.shortCircuitRatingKA
                        )
                    )
                }
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

private fun sldTypeLabel(
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
            if (arabic) "Busbar" else "Busbar"

        SldNodeType.PANEL ->
            if (arabic) "لوحة" else "Panel"

        SldNodeType.BREAKER ->
            if (arabic) "قاطع" else "Breaker"

        SldNodeType.LOAD ->
            if (arabic) "حمل" else "Load"
    }
}
