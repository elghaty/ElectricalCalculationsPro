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
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldUpstreamEngineering
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

const val NODE_WIDTH = 190f
const val NODE_HEIGHT = 126f

private val BackgroundGrid = Color(0xFFE1E7EB)
private val NodeFill = Color(0xFFFFFFFF)
private val NodeBorder = Color(0xFF263238)
private val TextPrimary = Color(0xFF172027)
private val TextSecondary = Color(0xFF60717A)
private val CableColor = Color(0xFF455A64)
private val BusbarColor = Color(0xFF00838F)
private val SelectedColor = Color(0xFF00838F)
private val StartColor = Color(0xFF1565C0)
private val EngineeringColor = Color(0xFF0277BD)
private val WarningColor = Color(0xFFEF6C00)
private val FaultColor = Color(0xFFC62828)
private val SuccessColor = Color(0xFF2E7D32)

fun DrawScope.drawSldEngineeringBackground() {
drawRect(
color = Color(0xFFF6F8FA)
)

var x = 0f

while (x <= size.width) {
    drawLine(
        color = BackgroundGrid,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1f
    )

    x += 40f
}

var y = 0f

while (y <= size.height) {
    drawLine(
        color = BackgroundGrid,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1f
    )

    y += 40f
}

}

fun DrawScope.drawConnection(
connection: SldConnection,
nodes: List<SldNode>,
selected: Boolean,
textMeasurer: TextMeasurer,
feederResult: SldUpstreamEngineering.FeederResult? = null
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

val isBusbar =
    connection.connectionType ==
        SldConnectionType.BUSBAR

val lineColor =
    when {
        selected ->
            SelectedColor

        isBusbar ->
            BusbarColor

        feederResult != null &&
            !feederResult.cableAdequate ->
            FaultColor

        else ->
            CableColor
    }

drawPath(
    path = path,
    color = lineColor,
    style =
        Stroke(
            width =
                when {
                    selected -> 7f
                    isBusbar -> 9f
                    else -> 4f
                }
        )
)

if (isBusbar) {
    drawBusbarConnectionLabel(
        textMeasurer = textMeasurer,
        x = middleX,
        y = min(start.y, end.y) - 28f,
        connection = connection
    )
} else {
    drawCableConnectionLabel(
        textMeasurer = textMeasurer,
        x = middleX,
        y = min(start.y, end.y) - 30f,
        connection = connection,
        feederResult = feederResult
    )
}

}

private fun DrawScope.drawBusbarConnectionLabel(
textMeasurer: TextMeasurer,
x: Float,
y: Float,
connection: SldConnection
) {
val text =
buildString {
append("BUSBAR")

        if (connection.busbarRatedCurrentA > 0.0) {
            append(
                "  ${fmt(connection.busbarRatedCurrentA)} A"
            )
        }

        if (connection.busbarShortCircuitKA > 0.0) {
            append(
                "  ${fmt(connection.busbarShortCircuitKA)} kA"
            )
        }
    }

drawText(
    textMeasurer = textMeasurer,
    text = text,
    topLeft = Offset(x - 65f, y),
    style =
        TextStyle(
            color = BusbarColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
)

}

private fun DrawScope.drawCableConnectionLabel(
textMeasurer: TextMeasurer,
x: Float,
y: Float,
connection: SldConnection,
feederResult: SldUpstreamEngineering.FeederResult?
) {
val cableText =
buildString {
append("CABLE")

        if (connection.cableSizeMm2 > 0.0) {
            append(
                "  ${fmt(connection.cableSizeMm2)} mm²"
            )

            if (connection.parallelRuns > 1) {
                append(
                    " × ${connection.parallelRuns}"
                )
            }
        }

        if (connection.lengthMeters > 0.0) {
            append(
                "  ${fmt(connection.lengthMeters)} m"
            )
        }
    }

drawText(
    textMeasurer = textMeasurer,
    text = cableText,
    topLeft = Offset(x - 75f, y),
    style =
        TextStyle(
            color =
                if (
                    feederResult != null &&
                    !feederResult.cableAdequate
                ) {
                    FaultColor
                } else {
                    TextSecondary
                },
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
)

if (feederResult != null) {
    val engineeringText =
        "Ib=${fmt(feederResult.currentA)} A   " +
            "S=${fmt(feederResult.kva)} kVA   " +
            "ΔV=${fmt(feederResult.voltageDropPercent)}%"

    drawText(
        textMeasurer = textMeasurer,
        text = engineeringText,
        topLeft = Offset(x - 105f, y + 16f),
        style =
            TextStyle(
                color =
                    when {
                        !feederResult.cableAdequate ->
                            FaultColor

                        feederResult.voltageDropPercent > 3.0 ->
                            WarningColor

                        else ->
                            EngineeringColor
                    },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
    )
}

}

fun DrawScope.drawNode(
node: SldNode,
selected: Boolean,
connectionStart: Boolean,
textMeasurer: TextMeasurer,
engineeringResult:
SldUpstreamEngineering.NodeResult? = null
) {
val centerX =
node.x + NODE_WIDTH / 2f

val centerY =
    node.y + 48f

if (selected || connectionStart) {
    drawRoundRect(
        color =
            if (connectionStart) {
                StartColor
            } else {
                SelectedColor
            },
        topLeft =
            Offset(
                node.x - 6f,
                node.y - 6f
            ),
        size =
            Size(
                NODE_WIDTH + 12f,
                NODE_HEIGHT + 12f
            ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                10f,
                10f
            ),
        style =
            Stroke(
                width = 4f
            )
    )
}

drawRoundRect(
    color = NodeFill,
    topLeft =
        Offset(
            node.x,
            node.y
        ),
    size =
        Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
    cornerRadius =
        androidx.compose.ui.geometry.CornerRadius(
            8f,
            8f
        )
)

drawRoundRect(
    color = NodeBorder,
    topLeft =
        Offset(
            node.x,
            node.y
        ),
    size =
        Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
    cornerRadius =
        androidx.compose.ui.geometry.CornerRadius(
            8f,
            8f
        ),
    style =
        Stroke(
            width = 2.5f
        )
)

when (node.type) {
    SldNodeType.SOURCE ->
        drawSourceSymbol(
            centerX,
            centerY
        )

    SldNodeType.TRANSFORMER ->
        drawTransformerSymbol(
            centerX,
            centerY
        )

    SldNodeType.GENERATOR ->
        drawGeneratorSymbol(
            centerX,
            centerY
        )

    SldNodeType.BUS ->
        drawBusbarSymbol(
            centerX,
            centerY
        )

    SldNodeType.PANEL ->
        drawPanelSymbol(
            centerX,
            centerY
        )

    SldNodeType.BREAKER ->
        drawBreakerSymbol(
            centerX,
            centerY
        )

    SldNodeType.LOAD ->
        drawLoadSymbol(
            centerX,
            centerY
        )
}

drawCenteredText(
    textMeasurer = textMeasurer,
    text = node.name,
    centerX = centerX,
    topY = node.y + 70f,
    style =
        TextStyle(
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
)

val electrical =
    buildString {
        append(
            "V ${fmt(node.voltage)} V"
        )

        if (node.loadKw > 0.0) {
            append(
                "   P ${fmt(node.loadKw)} kW"
            )
        }

        if (node.ratedKva > 0.0) {
            append(
                "   S ${fmt(node.ratedKva)} kVA"
            )
        }
    }

drawCenteredText(
    textMeasurer = textMeasurer,
    text = electrical,
    centerX = centerX,
    topY = node.y + 90f,
    style =
        TextStyle(
            color = TextSecondary,
            fontSize = 9.sp
        )
)

if (engineeringResult != null) {
    val engineering =
        "Ib ${fmt(engineeringResult.currentA)} A"

    drawCenteredText(
        textMeasurer = textMeasurer,
        text = engineering,
        centerX = centerX,
        topY = node.y + 106f,
        style =
            TextStyle(
                color =
                    when {
                        engineeringResult.loadingPercent > 100.0 ->
                            FaultColor

                        engineeringResult.voltageDropPercent > 3.0 ->
                            WarningColor

                        else ->
                            EngineeringColor
                    },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
    )
}

}

private fun DrawScope.drawCenteredText(
textMeasurer: TextMeasurer,
text: String,
centerX: Float,
topY: Float,
style: TextStyle
) {
val result =
textMeasurer.measure(
text = text,
style = style
)

drawText(
    textMeasurer = textMeasurer,
    text = text,
    topLeft =
        Offset(
            centerX - result.size.width / 2f,
            topY
        ),
    style = style
)

}

private fun DrawScope.drawSourceSymbol(
x: Float,
y: Float
) {
drawLine(
color = NodeBorder,
start = Offset(x, y - 32f),
end = Offset(x, y - 18f),
strokeWidth = 3f
)

drawCircle(
    color = Color.White,
    radius = 19f,
    center = Offset(x, y),
    style = Stroke(width = 3f)
)

drawLine(
    color = NodeBorder,
    start = Offset(x - 10f, y + 10f),
    end = Offset(x + 10f, y - 10f),
    strokeWidth = 2.5f
)

drawLine(
    color = NodeBorder,
    start = Offset(x - 10f, y - 10f),
    end = Offset(x + 10f, y + 10f),
    strokeWidth = 2.5f
)

drawLine(
    color = NodeBorder,
    start = Offset(x, y + 19f),
    end = Offset(x, y + 32f),
    strokeWidth = 3f
)

}

private fun DrawScope.drawTransformerSymbol(
x: Float,
y: Float
) {
drawLine(
color = NodeBorder,
start = Offset(x, y - 34f),
end = Offset(x, y - 25f),
strokeWidth = 3f
)

drawArc(
    color = NodeBorder,
    startAngle = -90f,
    sweepAngle = 180f,
    useCenter = false,
    topLeft = Offset(x - 25f, y - 25f),
    size = Size(50f, 50f),
    style = Stroke(width = 3f)
)

drawArc(
    color = NodeBorder,
    startAngle = 90f,
    sweepAngle = 180f,
    useCenter = false,
    topLeft = Offset(x + 1f, y - 25f),
    size = Size(50f, 50f),
    style = Stroke(width = 3f)
)

drawLine(
    color = NodeBorder,
    start = Offset(x, y + 25f),
    end = Offset(x, y + 34f),
    strokeWidth = 3f
)

}

private fun DrawScope.drawGeneratorSymbol(
x: Float,
y: Float
) {
drawLine(
color = NodeBorder,
start = Offset(x, y - 34f),
end = Offset(x, y - 22f),
strokeWidth = 3f
)

drawCircle(
    color = Color.White,
    radius = 22f,
    center = Offset(x, y),
    style = Stroke(width = 3f)
)

drawArc(
    color = NodeBorder,
    startAngle = 25f,
    sweepAngle = 130f,
    useCenter = false,
    topLeft = Offset(x - 13f, y - 13f),
    size = Size(26f, 26f),
    style = Stroke(width = 2.5f)
)

drawLine(
    color = NodeBorder,
    start = Offset(x, y + 22f),
    end = Offset(x, y + 34f),
    strokeWidth = 3f
)

}

private fun DrawScope.drawBusbarSymbol(
x: Float,
y: Float
) {
drawLine(
color = BusbarColor,
start = Offset(x - 58f, y),
end = Offset(x + 58f, y),
strokeWidth = 8f
)

drawLine(
    color = BusbarColor,
    start = Offset(x, y - 30f),
    end = Offset(x, y),
    strokeWidth = 4f
)

drawLine(
    color = BusbarColor,
    start = Offset(x, y),
    end = Offset(x, y + 30f),
    strokeWidth = 4f
)

}

private fun DrawScope.drawPanelSymbol(
x: Float,
y: Float
) {
drawRect(
color = Color.White,
topLeft = Offset(x - 34f, y - 25f),
size = Size(68f, 50f)
)

drawRect(
    color = NodeBorder,
    topLeft = Offset(x - 34f, y - 25f),
    size = Size(68f, 50f),
    style = Stroke(width = 3f)
)

drawLine(
    color = NodeBorder,
    start = Offset(x - 22f, y - 10f),
    end = Offset(x + 22f, y - 10f),
    strokeWidth = 2f
)

drawLine(
    color = NodeBorder,
    start = Offset(x - 22f, y),
    end = Offset(x + 22f, y),
    strokeWidth = 2f
)

drawLine(
    color = NodeBorder,
    start = Offset(x - 22f, y + 10f),
    end = Offset(x + 22f, y + 10f),
    strokeWidth = 2f
)

}

private fun DrawScope.drawBreakerSymbol(
x: Float,
y: Float
) {
drawLine(
color = NodeBorder,
start = Offset(x, y - 32f),
end = Offset(x, y - 18f),
strokeWidth = 3f
)

drawRect(
    color = Color.White,
    topLeft = Offset(x - 22f, y - 18f),
    size = Size(44f, 36f)
)

drawRect(
    color = NodeBorder,
    topLeft = Offset(x - 22f, y - 18f),
    size = Size(44f, 36f),
    style = Stroke(width = 3f)
)

drawLine(
    color = NodeBorder,
    start = Offset(x - 12f, y + 9f),
    end = Offset(x + 12f, y - 9f),
    strokeWidth = 3f
)

drawLine(
    color = NodeBorder,
    start = Offset(x, y + 18f),
    end = Offset(x, y + 32f),
    strokeWidth = 3f
)

}

private fun DrawScope.drawLoadSymbol(
x: Float,
y: Float
) {
drawLine(
color = NodeBorder,
start = Offset(x, y - 32f),
end = Offset(x, y - 22f),
strokeWidth = 3f
)

drawCircle(
    color = Color.White,
    radius = 21f,
    center = Offset(x, y),
    style = Stroke(width = 3f)
)

drawLine(
    color = NodeBorder,
    start = Offset(x - 12f, y + 12f),
    end = Offset(x + 12f, y - 12f),
    strokeWidth = 3f
)

drawLine(
    color = NodeBorder,
    start = Offset(x - 6f, y + 18f),
    end = Offset(x + 18f, y - 6f),
    strokeWidth = 2f
)

drawLine(
    color = NodeBorder,
    start = Offset(x, y + 21f),
    end = Offset(x, y + 32f),
    strokeWidth = 3f
)

}

private fun equipmentTypeLabel(
type: SldNodeType
): String =
when (type) {
SldNodeType.SOURCE -> "UTILITY SOURCE"
SldNodeType.TRANSFORMER -> "TRANSFORMER"
SldNodeType.GENERATOR -> "GENERATOR"
SldNodeType.BUS -> "BUSBAR"
SldNodeType.PANEL -> "PANELBOARD"
SldNodeType.BREAKER -> "CIRCUIT BREAKER"
SldNodeType.LOAD -> "LOAD / MOTOR"
}

fun findNode(
point: Offset,
nodes: List<SldNode>
): SldNode? =
nodes.lastOrNull { node ->
point.x >= node.x &&
point.x <= node.x + NODE_WIDTH &&
point.y >= node.y &&
point.y <= node.y + NODE_HEIGHT
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

    val middleX =
        (start.x + end.x) / 2f

    val p1 =
        Offset(
            middleX,
            start.y
        )

    val p2 =
        Offset(
            middleX,
            end.y
        )

    val distance =
        min(
            segmentDistance(
                point,
                start,
                p1
            ),
            min(
                segmentDistance(
                    point,
                    p1,
                    p2
                ),
                segmentDistance(
                    point,
                    p2,
                    end
                )
            )
        )

    if (distance < bestDistance) {
        bestDistance = distance
        bestConnection = connection
    }
}

return if (bestDistance < 35f) {
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

if (dx == 0f && dy == 0f) {
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
        min(
            1f,
            t
        )
    )

val closestX =
    start.x + clamped * dx

val closestY =
    start.y + clamped * dy

return sqrt(
    (point.x - closestX) *
        (point.x - closestX) +
        (point.y - closestY) *
        (point.y - closestY)
)

}
