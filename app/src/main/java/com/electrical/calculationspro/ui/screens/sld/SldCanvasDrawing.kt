package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldUpstreamEngineering
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val NODE_WIDTH = 180f
const val NODE_HEIGHT = 118f

private const val SYMBOL_OFFSET_Y = 31f

private const val NORMAL_CONNECTION_WIDTH = 3.5f
private const val SELECTED_CONNECTION_WIDTH = 6f
private const val BUSBAR_WIDTH = 8f

private val Background = Color(0xFFF8FAFC)
private val GridColor = Color(0xFFE3E8EC)
private val NodeBorder = Color(0xFF263238)
private val TextPrimary = Color(0xFF172027)
private val TextSecondary = Color(0xFF60717A)
private val CableColor = Color(0xFF37474F)
private val BusbarColor = Color(0xFF263238)
private val SelectedColor = Color(0xFF1565C0)
private val StartColor = Color(0xFFFF9800)
private val EngineeringColor = Color(0xFF087F5B)
private val WarningColor = Color(0xFFE67700)
private val FaultColor = Color(0xFFC62828)

fun DrawScope.drawSldEngineeringBackground() {
    drawRect(Background)

    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = GridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += 40f
    }

    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = GridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += 40f
    }
}

private fun nodeCenter(node: SldNode): Offset =
    Offset(
        node.x + NODE_WIDTH / 2f,
        node.y + NODE_HEIGHT / 2f
    )

private fun nodeDirection(
    from: SldNode,
    to: SldNode
): Direction {
    val a = nodeCenter(from)
    val b = nodeCenter(to)

    return if (abs(b.x - a.x) >= abs(b.y - a.y)) {
        if (b.x >= a.x) Direction.RIGHT else Direction.LEFT
    } else {
        if (b.y >= a.y) Direction.DOWN else Direction.UP
    }
}

private enum class Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

private fun standardPort(
    node: SldNode,
    direction: Direction,
    inset: Float = 0f
): Offset {
    return when (direction) {
        Direction.LEFT ->
            Offset(
                node.x - inset,
                node.y + NODE_HEIGHT / 2f
            )

        Direction.RIGHT ->
            Offset(
                node.x + NODE_WIDTH + inset,
                node.y + NODE_HEIGHT / 2f
            )

        Direction.UP ->
            Offset(
                node.x + NODE_WIDTH / 2f,
                node.y - inset
            )

        Direction.DOWN ->
            Offset(
                node.x + NODE_WIDTH / 2f,
                node.y + NODE_HEIGHT + inset
            )
    }
}

private fun panelBusbarPort(
    panel: SldNode,
    connection: SldConnection,
    connections: List<SldConnection>
): Offset {

    val outgoing =
        connections
            .filter {
                it.fromNodeId == panel.id &&
                    it.connectionType == SldConnectionType.BUSBAR
            }

    val index =
        outgoing.indexOfFirst { it.id == connection.id }
            .coerceAtLeast(0)

    val count = max(1, outgoing.size)

    val usableWidth =
        min(
            NODE_WIDTH - 34f,
            max(48f, count * 34f)
        )

    val left =
        panel.x +
            NODE_WIDTH / 2f -
            usableWidth / 2f

    val x =
        if (count == 1) {
            panel.x + NODE_WIDTH / 2f
        } else {
            left +
                index.toFloat() /
                (count - 1).toFloat() *
                usableWidth
        }

    return Offset(
        x,
        panel.y + SYMBOL_OFFSET_Y + 27f
    )
}

private fun connectionStartPort(
    node: SldNode,
    connection: SldConnection,
    connections: List<SldConnection>,
    nodes: List<SldNode>
): Offset {

    if (
        node.type == SldNodeType.PANEL &&
        connection.connectionType == SldConnectionType.BUSBAR
    ) {
        return panelBusbarPort(
            panel = node,
            connection = connection,
            connections = connections
        )
    }

    val target =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        }

    if (target == null) {
        return standardPort(node, Direction.DOWN)
    }

    return standardPort(
        node,
        nodeDirection(node, target)
    )
}

private fun connectionEndPort(
    node: SldNode,
    connection: SldConnection,
    nodes: List<SldNode>
): Offset {

    val source =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        }

    if (source == null) {
        return standardPort(node, Direction.UP)
    }

    return standardPort(
        node,
        nodeDirection(node, source)
    )
}

private fun orthogonalPoints(
    start: Offset,
    end: Offset
): List<Offset> {

    if (
        abs(start.x - end.x) < 2f ||
        abs(start.y - end.y) < 2f
    ) {
        return listOf(start, end)
    }

    val horizontalFirst =
        abs(end.x - start.x) >=
            abs(end.y - start.y)

    return if (horizontalFirst) {
        val midX =
            (start.x + end.x) / 2f

        listOf(
            start,
            Offset(midX, start.y),
            Offset(midX, end.y),
            end
        )
    } else {
        val midY =
            (start.y + end.y) / 2f

        listOf(
            start,
            Offset(start.x, midY),
            Offset(end.x, midY),
            end
        )
    }
}

fun DrawScope.drawConnection(
    connection: SldConnection,
    nodes: List<SldNode>,
    selected: Boolean,
    textMeasurer: TextMeasurer,
    feederResult:
        SldUpstreamEngineering.FeederResult? = null
) {

    val from =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        } ?: return

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return

    val start =
        connectionStartPort(
            node = from,
            connection = connection,
            connections = emptyList(),
            nodes = nodes
        )

    val end =
        connectionEndPort(
            node = to,
            connection = connection,
            nodes = nodes
        )

    val points =
        orthogonalPoints(
            start = start,
            end = end
        )

    val path = Path()

    path.moveTo(
        points.first().x,
        points.first().y
    )

    points.drop(1).forEach {
        path.lineTo(it.x, it.y)
    }

    val isBusbar =
        connection.connectionType ==
            SldConnectionType.BUSBAR

    val cableFault =
        feederResult != null &&
            !feederResult.cableAdequate

    val color =
        when {
            selected -> SelectedColor
            cableFault -> FaultColor
            isBusbar -> BusbarColor
            else -> CableColor
        }

    val width =
        if (isBusbar) {
            if (selected) {
                BUSBAR_WIDTH + 2f
            } else {
                BUSBAR_WIDTH
            }
        } else {
            if (selected) {
                SELECTED_CONNECTION_WIDTH
            } else {
                NORMAL_CONNECTION_WIDTH
            }
        }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = width,
            cap = StrokeCap.Square
        )
    )

    val labelPoint =
        if (points.size >= 3) {
            points[points.size / 2]
        } else {
            Offset(
                (start.x + end.x) / 2f,
                (start.y + end.y) / 2f
            )
        }

    if (isBusbar) {
        drawEngineeringLabel(
            textMeasurer = textMeasurer,
            text = buildString {
                append("BUS")
                if (connection.busbarRatedCurrentA > 0.0) {
                    append(" ")
                    append(fmt(connection.busbarRatedCurrentA))
                    append(" A")
                }
                if (connection.busbarShortCircuitKA > 0.0) {
                    append(" ")
                    append(fmt(connection.busbarShortCircuitKA))
                    append(" kA")
                }
            },
            point = labelPoint.copy(
                y = labelPoint.y - 12f
            ),
            color = BusbarColor
        )
    } else {
        drawEngineeringLabel(
            textMeasurer = textMeasurer,
            text = buildString {
                append("CABLE")
                if (connection.cableSizeMm2 > 0.0) {
                    append(" ")
                    append(fmt(connection.cableSizeMm2))
                    append(" mm²")
                    if (connection.parallelRuns > 1) {
                        append(" × ")
                        append(connection.parallelRuns)
                    }
                }
                if (connection.lengthMeters > 0.0) {
                    append(" ")
                    append(fmt(connection.lengthMeters))
                    append(" m")
                }
            },
            point = labelPoint.copy(
                y = labelPoint.y - 12f
            ),
            color =
                if (cableFault) FaultColor
                else TextSecondary
        )

        feederResult?.let { result ->
            drawEngineeringLabel(
                textMeasurer = textMeasurer,
                text =
                    "Ib=${fmt(result.currentA)} A  " +
                        "S=${fmt(result.kva)} kVA  " +
                        "ΔV=${fmt(result.voltageDropPercent)}%",
                point = labelPoint.copy(
                    y = labelPoint.y + 5f
                ),
                color =
                    when {
                        !result.cableAdequate ->
                            FaultColor
                        result.voltageDropPercent > 3.0 ->
                            WarningColor
                        else ->
                            EngineeringColor
                    },
                fontSize = 7.5f
            )
        }
    }
}

fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: TextMeasurer,
    engineeringResult:
        SldUpstreamEngineering.NodeResult? = null,
    nodes: List<SldNode> = emptyList(),
    connections: List<SldConnection> = emptyList()
) {

    val centerX =
        node.x + NODE_WIDTH / 2f

    val symbolY =
        node.y + SYMBOL_OFFSET_Y

    if (selected || connectionStart) {
        drawCircle(
            color =
                if (connectionStart) {
                    StartColor
                } else {
                    SelectedColor
                },
            radius = 39f,
            center = Offset(centerX, symbolY),
            style = Stroke(width = 3f)
        )
    }

    when (node.type) {
        SldNodeType.SOURCE ->
            drawSourceSymbol(centerX, symbolY)

        SldNodeType.TRANSFORMER ->
            drawTransformerSymbol(centerX, symbolY)

        SldNodeType.GENERATOR ->
            drawGeneratorSymbol(centerX, symbolY)

        SldNodeType.BUS ->
            drawBusbarSymbol(
                centerX,
                symbolY,
                node,
                connections
            )

        SldNodeType.PANEL ->
            drawPanelSymbol(
                centerX,
                symbolY,
                node,
                connections
            )

        SldNodeType.BREAKER -> {
            val outgoing =
                connections.firstOrNull {
                    it.fromNodeId == node.id
                }

            val target =
                outgoing?.let {
                    nodes.firstOrNull { n ->
                        n.id == it.toNodeId
                    }
                }

            val direction =
                if (target != null) {
                    nodeDirection(
                        node,
                        target
                    )
                } else {
                    Direction.DOWN
                }

            drawBreakerSymbol(
                centerX,
                symbolY,
                direction
            )
        }

        SldNodeType.LOAD ->
            drawLoadSymbol(centerX, symbolY)
    }

    drawCenteredText(
        textMeasurer,
        equipmentTypeLabel(node.type),
        centerX,
        node.y + 51f,
        TextStyle(
            color = TextSecondary,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold
        )
    )

    drawCenteredText(
        textMeasurer,
        node.name.take(25),
        centerX,
        node.y + 65f,
        TextStyle(
            color = TextPrimary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    )

    val electrical =
        buildString {
            append(fmt(node.voltage))
            append(" V")

            if (node.loadKw > 0.0) {
                append("  ")
                append(fmt(node.loadKw))
                append(" kW")
            }

            if (node.ratedKva > 0.0) {
                append("  ")
                append(fmt(node.ratedKva))
                append(" kVA")
            }
        }

    drawCenteredText(
        textMeasurer,
        electrical,
        centerX,
        node.y + 82f,
        TextStyle(
            color = TextSecondary,
            fontSize = 7.5.sp
        )
    )

    engineeringResult?.let { result ->
        val text =
            "Ib ${fmt(result.currentA)} A   " +
                "ΔV ${fmt(result.voltageDropPercent)}%"

        val color =
            when {
                result.loadingPercent > 100.0 ->
                    FaultColor
                result.voltageDropPercent > 3.0 ->
                    WarningColor
                else ->
                    EngineeringColor
            }

        drawCenteredText(
            textMeasurer,
            text,
            centerX,
            node.y + 98f,
            TextStyle(
                color = color,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun DrawScope.drawSourceSymbol(
    x: Float,
    y: Float
) {
    drawCircle(
        color = NodeBorder,
        radius = 24f,
        center = Offset(x, y),
        style = Stroke(width = 3f)
    )

    drawLine(
        NodeBorder,
        Offset(x, y - 15f),
        Offset(x, y + 15f),
        strokeWidth = 3f
    )

    drawLine(
        NodeBorder,
        Offset(x - 12f, y - 9f),
        Offset(x + 12f, y - 9f),
        strokeWidth = 3f
    )

    drawLine(
        NodeBorder,
        Offset(x - 12f, y + 9f),
        Offset(x + 12f, y + 9f),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawTransformerSymbol(
    x: Float,
    y: Float
) {
    drawCircle(
        NodeBorder,
        17f,
        Offset(x - 12f, y),
        style = Stroke(width = 3f)
    )

    drawCircle(
        NodeBorder,
        17f,
        Offset(x + 12f, y),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawGeneratorSymbol(
    x: Float,
    y: Float
) {
    drawCircle(
        NodeBorder,
        25f,
        Offset(x, y),
        style = Stroke(width = 3f)
    )

    drawArc(
        color = NodeBorder,
        startAngle = -50f,
        sweepAngle = 280f,
        useCenter = false,
        topLeft = Offset(x - 15f, y - 15f),
        size = Size(30f, 30f),
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawBusbarSymbol(
    x: Float,
    y: Float,
    node: SldNode,
    connections: List<SldConnection>
) {
    val outgoing =
        connections.filter {
            it.fromNodeId == node.id &&
                it.connectionType == SldConnectionType.BUSBAR
        }

    val feederCount =
        max(1, outgoing.size)

    val busWidth =
        max(
            70f,
            min(
                NODE_WIDTH - 18f,
                32f * feederCount.toFloat()
            )
        )

    drawLine(
        color = BusbarColor,
        start = Offset(x - busWidth / 2f, y),
        end = Offset(x + busWidth / 2f, y),
        strokeWidth = BUSBAR_WIDTH,
        cap = StrokeCap.Square
    )

    outgoing.forEachIndexed { index, _ ->
        val px =
            if (outgoing.size == 1) {
                x
            } else {
                x -
                    busWidth / 2f +
                    index.toFloat() /
                    (outgoing.size - 1).toFloat() *
                    busWidth
            }

        drawLine(
            color = BusbarColor,
            start = Offset(px, y),
            end = Offset(px, y + 22f),
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawPanelSymbol(
    x: Float,
    y: Float,
    node: SldNode,
    connections: List<SldConnection>
) {
    val outgoing =
        connections.filter {
            it.fromNodeId == node.id &&
                it.connectionType == SldConnectionType.BUSBAR
        }

    val count =
        max(1, outgoing.size)

    val width =
        max(
            70f,
            min(
                NODE_WIDTH - 14f,
                count * 32f
            )
        )

    drawLine(
        color = NodeBorder,
        start = Offset(x - width / 2f, y),
        end = Offset(x + width / 2f, y),
        strokeWidth = 7f,
        cap = StrokeCap.Square
    )

    outgoing.forEachIndexed { index, _ ->
        val px =
            if (outgoing.size == 1) {
                x
            } else {
                x -
                    width / 2f +
                    index.toFloat() /
                    (outgoing.size - 1).toFloat() *
                    width
            }

        drawLine(
            color = NodeBorder,
            start = Offset(px, y),
            end = Offset(px, y + 27f),
            strokeWidth = 3f
        )
    }

    drawLine(
        color = NodeBorder,
        start = Offset(x, y - 28f),
        end = Offset(x, y),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawBreakerSymbol(
    x: Float,
    y: Float,
    direction: Direction
) {
    when (direction) {
        Direction.RIGHT,
        Direction.LEFT -> {
            val sign =
                if (direction == Direction.RIGHT) 1f else -1f

            val a =
                Offset(
                    x - 30f * sign,
                    y
                )

            val pivot =
                Offset(
                    x - 8f * sign,
                    y
                )

            val blade =
                Offset(
                    x + 18f * sign,
                    y - 17f
                )

            val terminal =
                Offset(
                    x + 32f * sign,
                    y - 17f
                )

            drawLine(
                NodeBorder,
                a,
                pivot,
                strokeWidth = 3f
            )

            drawCircle(
                NodeBorder,
                3.5f,
                pivot
            )

            drawLine(
                NodeBorder,
                pivot,
                blade,
                strokeWidth = 3f
            )

            drawLine(
                NodeBorder,
                blade,
                terminal,
                strokeWidth = 3f
            )

            drawCircle(
                NodeBorder,
                3.5f,
                terminal
            )
        }

        Direction.DOWN,
        Direction.UP -> {
            val sign =
                if (direction == Direction.DOWN) 1f else -1f

            val a =
                Offset(
                    x,
                    y - 30f * sign
                )

            val pivot =
                Offset(
                    x,
                    y - 8f * sign
                )

            val blade =
                Offset(
                    x + 17f,
                    y + 18f * sign
                )

            val terminal =
                Offset(
                    x + 17f,
                    y + 32f * sign
                )

            drawLine(
                NodeBorder,
                a,
                pivot,
                strokeWidth = 3f
            )

            drawCircle(
                NodeBorder,
                3.5f,
                pivot
            )

            drawLine(
                NodeBorder,
                pivot,
                blade,
                strokeWidth = 3f
            )

            drawLine(
                NodeBorder,
                blade,
                terminal,
                strokeWidth = 3f
            )

            drawCircle(
                NodeBorder,
                3.5f,
                terminal
            )
        }
    }
}

private fun DrawScope.drawLoadSymbol(
    x: Float,
    y: Float
) {
    drawCircle(
        color = NodeBorder,
        radius = 22f,
        center = Offset(x, y),
        style = Stroke(width = 3f)
    )

    drawLine(
        NodeBorder,
        Offset(x - 12f, y + 12f),
        Offset(x + 12f, y - 12f),
        strokeWidth = 3f
    )
}

private fun equipmentTypeLabel(
    type: SldNodeType
): String =
    when (type) {
        SldNodeType.SOURCE -> "UTILITY"
        SldNodeType.TRANSFORMER -> "TRANSFORMER"
        SldNodeType.GENERATOR -> "GENERATOR"
        SldNodeType.BUS -> "BUSBAR"
        SldNodeType.PANEL -> "PANEL"
        SldNodeType.BREAKER -> "BREAKER"
        SldNodeType.LOAD -> "LOAD"
    }

private fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    text: String,
    centerX: Float,
    y: Float,
    style: TextStyle
) {
    val measured =
        textMeasurer.measure(
            text = text,
            style = style
        )

    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft =
            Offset(
                centerX -
                    measured.size.width / 2f,
                y
            ),
        style = style
    )
}

private fun DrawScope.drawEngineeringLabel(
    textMeasurer: TextMeasurer,
    text: String,
    point: Offset,
    color: Color,
    fontSize: Float = 8.5f
) {
    if (text.isBlank()) return

    val style =
        TextStyle(
            color = color,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )

    val measured =
        textMeasurer.measure(
            text = text,
            style = style
        )

    drawRoundRect(
        color = Color.White.copy(alpha = 0.94f),
        topLeft =
            Offset(
                point.x -
                    measured.size.width / 2f -
                    5f,
                point.y - 3f
            ),
        size =
            Size(
                measured.size.width + 10f,
                measured.size.height + 7f
            ),
        cornerRadius =
            CornerRadius(3f, 3f)
    )

    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft =
            Offset(
                point.x -
                    measured.size.width / 2f,
                point.y
            ),
        style = style
    )
}

fun findNode(
    point: Offset,
    nodes: List<SldNode>
): SldNode? {
    return nodes.lastOrNull { node ->
        point.x >= node.x - 12f &&
            point.x <= node.x + NODE_WIDTH + 12f &&
            point.y >= node.y - 12f &&
            point.y <= node.y + NODE_HEIGHT + 12f
    }
}

fun findConnection(
    point: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {

    var best: SldConnection? = null
    var bestDistance = Float.MAX_VALUE

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
            connectionStartPort(
                from,
                connection,
                connections,
                nodes
            )

        val end =
            connectionEndPort(
                to,
                connection,
                nodes
            )

        val points =
            orthogonalPoints(
                start,
                end
            )

        for (i in 0 until points.lastIndex) {
            val distance =
                segmentDistance(
                    point,
                    points[i],
                    points[i + 1]
                )

            if (distance < bestDistance) {
                bestDistance = distance
                best = connection
            }
        }
    }

    return if (bestDistance <= 26f) best else null
}

private fun segmentDistance(
    point: Offset,
    a: Offset,
    b: Offset
): Float {

    val dx = b.x - a.x
    val dy = b.y - a.y

    if (dx == 0f && dy == 0f) {
        return distance(point, a)
    }

    val t =
        (
            (point.x - a.x) * dx +
                (point.y - a.y) * dy
            ) /
            (dx * dx + dy * dy)

    val clamped =
        t.coerceIn(0f, 1f)

    val closest =
        Offset(
            a.x + clamped * dx,
            a.y + clamped * dy
        )

    return distance(point, closest)
}

private fun distance(
    a: Offset,
    b: Offset
): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return kotlin.math.sqrt(
        dx * dx + dy * dy
    )
}
