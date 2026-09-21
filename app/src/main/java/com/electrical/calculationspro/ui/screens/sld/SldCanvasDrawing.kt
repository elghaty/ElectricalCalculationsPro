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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

const val NODE_WIDTH = 170f
const val NODE_HEIGHT = 112f

private val PrimaryColor =
    Color(0xFFF2F5F7)

private val SecondaryColor =
    Color(0xFF9BA8B2)

private val SelectedColor =
    Color(0xFF00E676)

private val ConnectionColor =
    Color(0xFFB0BEC5)

private val ConnectionSelectedColor =
    Color(0xFF00BCD4)

fun DrawScope.drawConnection(
    connection: SldConnection,
    nodes: List<SldNode>,
    selected: Boolean,
    textMeasurer: TextMeasurer
) {

    val from =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        }
            ?: return

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        }
            ?: return

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

    val middleX =
        (start.x + end.x) / 2f

    val path =
        Path().apply {

            moveTo(
                start.x,
                start.y
            )

            lineTo(
                middleX,
                start.y
            )

            lineTo(
                middleX,
                end.y
            )

            lineTo(
                end.x,
                end.y
            )
        }

    drawPath(
        path = path,
        color =
            if (selected) {
                ConnectionSelectedColor
            } else {
                ConnectionColor
            },
        style =
            Stroke(
                width =
                    if (selected) {
                        7f
                    } else {
                        4f
                    }
            )
    )

    drawText(
        textMeasurer = textMeasurer,
        text =
            "${fmt(connection.cableSizeMm2)} mm² x " +
                connection.parallelRuns,
        topLeft =
            Offset(
                middleX - 40f,
                min(start.y, end.y) - 28f
            ),
        style =
            TextStyle(
                color = SecondaryColor,
                fontSize = 13.sp
            )
    )
}

fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: TextMeasurer
) {

    val nodeColor =
        when (node.type) {

            SldNodeType.SOURCE ->
                Color(0xFF1976D2)

            SldNodeType.TRANSFORMER ->
                Color(0xFFFF9800)

            SldNodeType.GENERATOR ->
                Color(0xFF43A047)

            SldNodeType.BREAKER ->
                Color(0xFF607D8B)

            SldNodeType.BUS ->
                Color(0xFF9C27B0)

            SldNodeType.PANEL ->
                Color(0xFF00838F)

            SldNodeType.LOAD ->
                Color(0xFF455A64)
        }

    drawRect(
        color = nodeColor,
        topLeft =
            Offset(
                node.x + 20f,
                node.y + 20f
            ),
        size =
            Size(
                width = 110f,
                height = 60f
            ),
        style =
            Stroke(
                width = 5f
            )
    )

    if (selected || connectionStart) {

        drawRect(
            color =
                if (connectionStart) {
                    Color(0xFF00BCD4)
                } else {
                    SelectedColor
                },
            topLeft =
                Offset(
                    node.x - 5f,
                    node.y - 5f
                ),
            size =
                Size(
                    width = NODE_WIDTH + 10f,
                    height = NODE_HEIGHT + 10f
                ),
            style =
                Stroke(
                    width = 4f
                )
        )
    }

    drawText(
        textMeasurer = textMeasurer,
        text = node.name,
        topLeft =
            Offset(
                node.x,
                node.y + 92f
            ),
        style =
            TextStyle(
                color = PrimaryColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
    )

    drawText(
        textMeasurer = textMeasurer,
        text =
            "V = ${fmt(node.voltage)} V",
        topLeft =
            Offset(
                node.x,
                node.y + 110f
            ),
        style =
            TextStyle(
                color = SecondaryColor,
                fontSize = 12.sp
            )
    )
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

    var bestConnection: SldConnection? = null
    var bestDistance = Float.MAX_VALUE

    connections.forEach { connection ->

        val from =
            nodes.firstOrNull {
                it.id == connection.fromNodeId
            }
                ?: return@forEach

        val to =
            nodes.firstOrNull {
                it.id == connection.toNodeId
            }
                ?: return@forEach

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

        val middleX =
            (start.x + end.x) / 2f

        val d1 =
            segmentDistance(
                point,
                start,
                Offset(
                    middleX,
                    start.y
                )
            )

        val d2 =
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

        val d3 =
            segmentDistance(
                point,
                Offset(
                    middleX,
                    end.y
                ),
                end
            )

        val distance =
            min(
                d1,
                min(d2, d3)
            )

        if (distance < bestDistance) {

            bestDistance = distance
            bestConnection = connection
        }
    }

    return if (bestDistance < 40f) {
        bestConnection
    } else {
        null
    }
}

private fun segmentDistance(
    point: Offset,
    start: Offset,
    end: Offset
): Float {

    val dx =
        end.x - start.x

    val dy =
        end.y - start.y

    if (
        dx == 0f &&
        dy == 0f
    ) {

        return sqrt(
            (point.x - start.x) *
                (point.x - start.x) +
                (point.y - start.y) *
                (point.y - start.y)
        )
    }

    val t =
        (
            (point.x - start.x) * dx +
                (point.y - start.y) * dy
            ) /
            (
                dx * dx +
                    dy * dy
                )

    val clamped =
        max(
            0f,
            min(1f, t)
        )

    val closestX =
        start.x +
            clamped * dx

    val closestY =
        start.y +
            clamped * dy

    return sqrt(
        (point.x - closestX) *
            (point.x - closestX) +
            (point.y - closestY) *
            (point.y - closestY)
    )
}
