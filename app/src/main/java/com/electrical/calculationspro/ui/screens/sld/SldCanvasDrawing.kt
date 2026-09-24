package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import kotlin.math.abs
import kotlin.math.min

const val NODE_WIDTH = 190f
const val NODE_HEIGHT = 126f

private val PrimaryColor = Color(0xFF172027)
private val SecondaryColor = Color(0xFF60717B)
private val SymbolColor = Color(0xFF263238)
private val ConnectionColor = Color(0xFF546E7A)
private val SelectedColor = Color(0xFF1565C0)
private val StartColor = Color(0xFF00897B)
private val BackgroundColor = Color.White

fun DrawScope.drawConnection(
    connection: SldConnection,
    nodes: List<SldNode>,
    selected: Boolean,
    textMeasurer: TextMeasurer
) {
    val from = nodes.firstOrNull {
        it.id == connection.fromNodeId
    } ?: return

    val to = nodes.firstOrNull {
        it.id == connection.toNodeId
    } ?: return

    val start = connectionStart(from, to)
    val end = connectionEnd(from, to)

    val horizontal =
        abs(end.x - start.x) >= abs(end.y - start.y)

    val path = Path()

    if (horizontal) {
        val middleX = (start.x + end.x) / 2f

        path.moveTo(start.x, start.y)
        path.lineTo(middleX, start.y)
        path.lineTo(middleX, end.y)
        path.lineTo(end.x, end.y)
    } else {
        val middleY = (start.y + end.y) / 2f

        path.moveTo(start.x, start.y)
        path.lineTo(start.x, middleY)
        path.lineTo(end.x, middleY)
        path.lineTo(end.x, end.y)
    }

    drawPath(
        path = path,
        color = if (selected) {
            SelectedColor
        } else {
            ConnectionColor
        },
        style = Stroke(
            width = if (selected) 7f else 4f
        )
    )

    drawConnectionArrow(
        start = start,
        end = end,
        horizontal = horizontal,
        selected = selected
    )

    val label = buildString {
        if (connection.cableSizeMm2 > 0.0) {
            append(fmtCanvas(connection.cableSizeMm2))
            append(" mm²")

            if (connection.parallelRuns > 1) {
                append(" × ")
                append(connection.parallelRuns)
            }
        }

        if (connection.lengthMeters > 0.0) {
            if (isNotEmpty()) {
                append(" | ")
            }

            append(fmtCanvas(connection.lengthMeters))
            append(" m")
        }

        if (connection.currentCapacityA > 0.0) {
            if (isNotEmpty()) {
                append(" | ")
            }

            append(fmtCanvas(connection.currentCapacityA))
            append(" A")
        }

        if (connection.voltageDropPercent > 0.0) {
            if (isNotEmpty()) {
                append(" | ")
            }

            append("ΔV=")
            append(fmtCanvas(connection.voltageDropPercent))
            append("%")
        }
    }

    if (label.isNotBlank()) {
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(
                min(start.x, end.x) +
                    abs(end.x - start.x) / 2f -
                    65f,
                min(start.y, end.y) - 26f
            ),
            style = TextStyle(
                color = SecondaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun DrawScope.drawConnectionArrow(
    start: Offset,
    end: Offset,
    horizontal: Boolean,
    selected: Boolean
) {
    val color = if (selected) {
        SelectedColor
    } else {
        ConnectionColor
    }

    val x = if (horizontal) {
        (start.x + end.x) / 2f
    } else {
        end.x
    }

    val y = if (horizontal) {
        end.y
    } else {
        (start.y + end.y) / 2f
    }

    if (horizontal) {
        val direction =
            if (end.x >= start.x) 1f else -1f

        drawLine(
            color = color,
            start = Offset(
                x - direction * 9f,
                y - 7f
            ),
            end = Offset(x, y),
            strokeWidth = 3f
        )

        drawLine(
            color = color,
            start = Offset(
                x - direction * 9f,
                y + 7f
            ),
            end = Offset(x, y),
            strokeWidth = 3f
        )
    } else {
        val direction =
            if (end.y >= start.y) 1f else -1f

        drawLine(
            color = color,
            start = Offset(
                x - 7f,
                y - direction * 9f
            ),
            end = Offset(x, y),
            strokeWidth = 3f
        )

        drawLine(
            color = color,
            start = Offset(
                x + 7f,
                y - direction * 9f
            ),
            end = Offset(x, y),
            strokeWidth = 3f
        )
    }
}

fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: TextMeasurer
) {
    if (selected || connectionStart) {
        drawRect(
            color = if (connectionStart) {
                StartColor
            } else {
                SelectedColor
            },
            topLeft = Offset(
                node.x - 7f,
                node.y - 7f
            ),
            size = Size(
                NODE_WIDTH + 14f,
                NODE_HEIGHT + 14f
            ),
            style = Stroke(width = 4f)
        )
    }

    drawRect(
        color = BackgroundColor,
        topLeft = Offset(node.x, node.y),
        size = Size(
            NODE_WIDTH,
            NODE_HEIGHT
        )
    )

    when (node.type) {
        SldNodeType.SOURCE ->
            drawSourceSymbol(
                node.x + NODE_WIDTH / 2f,
                node.y + 47f
            )

        SldNodeType.TRANSFORMER ->
            drawTransformerSymbol(
                node.x + NODE_WIDTH / 2f,
                node.y + 47f
            )

        SldNodeType.GENERATOR ->
            drawGeneratorSymbol(
                node.x + NODE_WIDTH / 2f,
                node.y + 47f
            )

        SldNodeType.BUS ->
            drawBusbarSymbol(
                node.x + NODE_WIDTH / 2f,
                node.y + 47f
            )

        SldNodeType.PANEL ->
            drawPanelSymbol(
                node.x + NODE_WIDTH / 2f,
                node.y + 47f
            )

        SldNodeType.BREAKER ->
            drawBreakerSymbol(
                node.x + NODE_WIDTH / 2f,
                node.y + 47f
            )

        SldNodeType.LOAD ->
            drawLoadSymbol(
                node.x + NODE_WIDTH / 2f,
                node.y + 47f
            )
    }

    drawText(
        textMeasurer = textMeasurer,
        text = node.name,
        topLeft = Offset(
            node.x + 8f,
            node.y + 84f
        ),
        style = TextStyle(
            color = PrimaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = engineeringText(node),
        topLeft = Offset(
            node.x + 8f,
            node.y + 102f
        ),
        style = TextStyle(
            color = SecondaryColor,
            fontSize = 10.sp
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = equipmentTypeLabel(node.type),
        topLeft = Offset(
            node.x + 8f,
            node.y + 116f
        ),
        style = TextStyle(
            color = SecondaryColor,
            fontSize = 9.sp
        )
    )
}

private fun engineeringText(
    node: SldNode
): String {
    return buildString {
        append("V=")
        append(fmtCanvas(node.voltage))
        append(" V")

        if (node.loadKw > 0.0) {
            append("   P=")
            append(fmtCanvas(node.loadKw))
            append(" kW")
        }

        if (node.ratedKva > 0.0) {
            append("   S=")
            append(fmtCanvas(node.ratedKva))
            append(" kVA")
        }
    }
}

private fun DrawScope.drawSourceSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        SymbolColor,
        Offset(x, y - 45f),
        Offset(x, y - 24f),
        4f
    )

    drawCircle(
        color = BackgroundColor,
        radius = 23f,
        center = Offset(x, y),
        style = Stroke(3.5f)
    )

    drawLine(
        SymbolColor,
        Offset(x - 13f, y - 13f),
        Offset(x + 13f, y + 13f),
        3f
    )

    drawLine(
        SymbolColor,
        Offset(x + 13f, y - 13f),
        Offset(x - 13f, y + 13f),
        3f
    )

    drawLine(
        SymbolColor,
        Offset(x, y + 23f),
        Offset(x, y + 45f),
        4f
    )
}

private fun DrawScope.drawTransformerSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        SymbolColor,
        Offset(x, y - 48f),
        Offset(x, y - 28f),
        4f
    )

    drawArc(
        color = SymbolColor,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(
            x - 29f,
            y - 29f
        ),
        size = Size(58f, 58f),
        style = Stroke(3.5f)
    )

    drawArc(
        color = SymbolColor,
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(
            x + 1f,
            y - 29f
        ),
        size = Size(58f, 58f),
        style = Stroke(3.5f)
    )

    drawLine(
        SymbolColor,
        Offset(x, y + 29f),
        Offset(x, y + 48f),
        4f
    )
}

private fun DrawScope.drawGeneratorSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        SymbolColor,
        Offset(x, y - 48f),
        Offset(x, y - 25f),
        4f
    )

    drawCircle(
        color = BackgroundColor,
        radius = 25f,
        center = Offset(x, y),
        style = Stroke(3.5f)
    )

    drawArc(
        color = SymbolColor,
        startAngle = 25f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(
            x - 15f,
            y - 15f
        ),
        size = Size(30f, 30f),
        style = Stroke(3f)
    )

    drawLine(
        SymbolColor,
        Offset(x, y + 25f),
        Offset(x, y + 48f),
        4f
    )
}

private fun DrawScope.drawBusbarSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        SymbolColor,
        Offset(x - 58f, y),
        Offset(x + 58f, y),
        8f
    )

    drawLine(
        SymbolColor,
        Offset(x, y - 45f),
        Offset(x, y),
        4f
    )

    drawLine(
        SymbolColor,
        Offset(x, y),
        Offset(x, y + 45f),
        4f
    )

    drawCircle(
        color = SymbolColor,
        radius = 5f,
        center = Offset(x, y - 45f)
    )

    drawCircle(
        color = SymbolColor,
        radius = 5f,
        center = Offset(x, y + 45f)
    )
}

private fun DrawScope.drawPanelSymbol(
    x: Float,
    y: Float
) {
    drawRect(
        color = BackgroundColor,
        topLeft = Offset(
            x - 38f,
            y - 30f
        ),
        size = Size(76f, 60f),
        style = Stroke(3.5f)
    )

    drawLine(
        SymbolColor,
        Offset(x, y - 30f),
        Offset(x, y + 30f),
        2.5f
    )

    drawLine(
        SymbolColor,
        Offset(x - 38f, y - 10f),
        Offset(x + 38f, y - 10f),
        2f
    )

    drawLine(
        SymbolColor,
        Offset(x - 38f, y + 10f),
        Offset(x + 38f, y + 10f),
        2f
    )
}

private fun DrawScope.drawBreakerSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        SymbolColor,
        Offset(x, y - 48f),
        Offset(x, y - 24f),
        4f
    )

    drawRect(
        color = BackgroundColor,
        topLeft = Offset(
            x - 24f,
            y - 24f
        ),
        size = Size(48f, 48f),
        style = Stroke(3.5f)
    )

    drawLine(
        SymbolColor,
        Offset(x - 14f, y + 12f),
        Offset(x + 13f, y - 12f),
        4f
    )

    drawLine(
        SymbolColor,
        Offset(x, y + 24f),
        Offset(x, y + 48f),
        4f
    )
}

private fun DrawScope.drawLoadSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        SymbolColor,
        Offset(x, y - 48f),
        Offset(x, y - 23f),
        4f
    )

    drawCircle(
        color = BackgroundColor,
        radius = 23f,
        center = Offset(x, y),
        style = Stroke(3.5f)
    )

    drawLine(
        SymbolColor,
        Offset(x - 14f, y + 14f),
        Offset(x + 14f, y - 14f),
        3f
    )

    drawLine(
        SymbolColor,
        Offset(x - 8f, y + 20f),
        Offset(x + 20f, y - 8f),
        2f
    )

    drawLine(
        SymbolColor,
        Offset(x, y + 23f),
        Offset(x, y + 48f),
        4f
    )
}

private fun equipmentTypeLabel(
    type: SldNodeType
): String {
    return when (type) {
        SldNodeType.SOURCE ->
            "UTILITY SOURCE"

        SldNodeType.TRANSFORMER ->
            "TRANSFORMER"

        SldNodeType.GENERATOR ->
            "GENERATOR"

        SldNodeType.BUS ->
            "BUSBAR"

        SldNodeType.PANEL ->
            "PANELBOARD"

        SldNodeType.BREAKER ->
            "CIRCUIT BREAKER"

        SldNodeType.LOAD ->
            "LOAD / MOTOR"
    }
}

fun findNode(
    point: Offset,
    nodes: List<SldNode>
): SldNode? {
    return nodes.lastOrNull { node ->
        point.x >= node.x &&
            point.x <= node.x + NODE_WIDTH &&
            point.y >= node.y &&
            point.y <= node.y + NODE_HEIGHT
    }
}

fun findConnection(
    point: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {
    var best: SldConnection? = null
    var distance = Float.MAX_VALUE

    connections.forEach { connection ->
        val from = nodes.firstOrNull {
            it.id == connection.fromNodeId
        } ?: return@forEach

        val to = nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return@forEach

        val start = connectionStart(from, to)
        val end = connectionEnd(from, to)

        val horizontal =
            abs(end.x - start.x) >=
                abs(end.y - start.y)

        val d =
            if (horizontal) {
                val middleX =
                    (start.x + end.x) / 2f

                min(
                    min(
                        segmentDistance(
                            point,
                            start,
                            Offset(
                                middleX,
                                start.y
                            )
                        ),
                        segmentDistance(
                            point,
                            Offset(
                                middleX,
                                start.y
                            ),
                            Offset(
                                middleX,
                                end.y
                            )
                        )
                    ),
                    segmentDistance(
                        point,
                        Offset(
                            middleX,
                            end.y
                        ),
                        end
                    )
                )
            } else {
                val middleY =
                    (start.y + end.y) / 2f

                min(
                    min(
                        segmentDistance(
                            point,
                            start,
                            Offset(
                                start.x,
                                middleY
                            )
                        ),
                        segmentDistance(
                            point,
                            Offset(
                                start.x,
                                middleY
                            ),
                            Offset(
                                end.x,
                                middleY
                            )
                        )
                    ),
                    segmentDistance(
                        point,
                        Offset(
                            end.x,
                            middleY
                        ),
                        end
                    )
                )
            }

        if (d < distance) {
            distance = d
            best = connection
        }
    }

    return best?.takeIf {
        distance <= 24f
    }
}

private fun connectionStart(
    from: SldNode,
    to: SldNode
): Offset {
    return if (to.x >= from.x) {
        Offset(
            from.x + NODE_WIDTH,
            from.y + NODE_HEIGHT / 2f
        )
    } else {
        Offset(
            from.x,
            from.y + NODE_HEIGHT / 2f
        )
    }
}

private fun connectionEnd(
    from: SldNode,
    to: SldNode
): Offset {
    return if (to.x >= from.x) {
        Offset(
            to.x,
            to.y + NODE_HEIGHT / 2f
        )
    } else {
        Offset(
            to.x + NODE_WIDTH,
            to.y + NODE_HEIGHT / 2f
        )
    }
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

    val clamped = t.coerceIn(0f, 1f)

    val projection = Offset(
        a.x + clamped * dx,
        a.y + clamped * dy
    )

    return distance(
        point,
        projection
    )
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

/*
 * SLD canvas has its own formatter so it does not collide
 * with the report formatter in SldReports.kt.
 */
private fun fmtCanvas(
    value: Double
): String {
    return when {
        !value.isFinite() -> "0"
        value == 0.0 -> "0"
        value % 1.0 == 0.0 ->
            value.toInt().toString()
        else ->
            "%.2f".format(value)
    }
}
