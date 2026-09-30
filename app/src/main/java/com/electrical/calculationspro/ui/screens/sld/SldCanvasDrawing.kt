package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import kotlin.math.sqrt

const val NODE_WIDTH = 180f
const val NODE_HEIGHT = 118f

private const val SYMBOL_OFFSET_Y = 30f

private const val NORMAL_CONNECTION_WIDTH = 3.0f
private const val SELECTED_CONNECTION_WIDTH = 5.5f
private const val BUSBAR_WIDTH = 7f

private val Background = Color(0xFFF7F9FB)
private val GridColor = Color(0xFFE2E7EB)
private val MajorGridColor = Color(0xFFD1D8DE)

private val ElectricalBlack = Color(0xFF172027)
private val CableColor = Color(0xFF37474F)
private val BusbarColor = Color(0xFF202B32)

private val TextPrimary = Color(0xFF172027)
private val TextSecondary = Color(0xFF60717A)

private val SelectedColor = Color(0xFF1565C0)
private val StartColor = Color(0xFFFF9800)

private val EngineeringColor = Color(0xFF087F5B)
private val WarningColor = Color(0xFFE67700)
private val FaultColor = Color(0xFFC62828)

private val LabelBackground = Color.White.copy(alpha = 0.96f)

/*

* ================================================================
* BACKGROUND
* ================================================================
  */

fun DrawScope.drawSldEngineeringBackground() {

drawRect(
    color = Background
)

var x = 0f

while (x <= size.width) {

    val major =
        x.toInt() % 200 == 0

    drawLine(
        color =
            if (major) {
                MajorGridColor
            } else {
                GridColor
            },

        start =
            Offset(
                x,
                0f
            ),

        end =
            Offset(
                x,
                size.height
            ),

        strokeWidth =
            if (major) {
                1.4f
            } else {
                0.7f
            }
    )

    x += 40f
}

var y = 0f

while (y <= size.height) {

    val major =
        y.toInt() % 200 == 0

    drawLine(
        color =
            if (major) {
                MajorGridColor
            } else {
                GridColor
            },

        start =
            Offset(
                0f,
                y
            ),

        end =
            Offset(
                size.width,
                y
            ),

        strokeWidth =
            if (major) {
                1.4f
            } else {
                0.7f
            }
    )

    y += 40f
}

}

/*

* ================================================================
* GEOMETRY
* ================================================================
  */

private fun nodeCenter(
node: SldNode
): Offset =
Offset(
node.x + NODE_WIDTH / 2f,
node.y + NODE_HEIGHT / 2f
)

private enum class Direction {
LEFT,
RIGHT,
UP,
DOWN
}

private fun nodeDirection(
from: SldNode,
to: SldNode
): Direction {

val a =
    nodeCenter(from)

val b =
    nodeCenter(to)

val dx =
    b.x - a.x

val dy =
    b.y - a.y

return if (
    abs(dx) >= abs(dy)
) {

    if (dx >= 0f) {
        Direction.RIGHT
    } else {
        Direction.LEFT
    }

} else {

    if (dy >= 0f) {
        Direction.DOWN
    } else {
        Direction.UP
    }
}

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

/*

* ================================================================
* PANEL BUSBAR PORT
* ================================================================
  */

private fun panelBusbarPort(
panel: SldNode,
connection: SldConnection,
connections: List<SldConnection>
): Offset {

val outgoing =
    connections.filter {
        it.fromNodeId == panel.id &&
            it.connectionType ==
            SldConnectionType.BUSBAR
    }

val index =
    outgoing.indexOfFirst {
        it.id == connection.id
    }
        .coerceAtLeast(0)

val count =
    max(
        1,
        outgoing.size
    )

val usableWidth =
    min(
        NODE_WIDTH - 30f,
        max(
            50f,
            count * 34f
        )
    )

val left =
    panel.x +
        NODE_WIDTH / 2f -
        usableWidth / 2f

val x =
    if (count == 1) {

        panel.x +
            NODE_WIDTH / 2f

    } else {

        left +
            index.toFloat() /
            (count - 1).toFloat() *
            usableWidth
    }

return Offset(
    x,
    panel.y +
        SYMBOL_OFFSET_Y +
        26f
)

}

/*

* ================================================================
* CONNECTION PORTS
* ================================================================
  */

private fun connectionStartPort(
node: SldNode,
connection: SldConnection,
connections: List<SldConnection>,
nodes: List<SldNode>
): Offset {

if (
    node.type ==
    SldNodeType.PANEL &&
    connection.connectionType ==
    SldConnectionType.BUSBAR
) {

    return panelBusbarPort(
        panel = node,
        connection = connection,
        connections = connections
    )
}

val target =
    nodes.firstOrNull {
        it.id ==
            connection.toNodeId
    }

if (target == null) {

    return standardPort(
        node,
        Direction.DOWN
    )
}

return standardPort(
    node,
    nodeDirection(
        node,
        target
    )
)

}

private fun connectionEndPort(
node: SldNode,
connection: SldConnection,
nodes: List<SldNode>
): Offset {

val source =
    nodes.firstOrNull {
        it.id ==
            connection.fromNodeId
    }

if (source == null) {

    return standardPort(
        node,
        Direction.UP
    )
}

return standardPort(
    node,
    nodeDirection(
        node,
        source
    )
)

}

/*

* ================================================================
* ORTHOGONAL ROUTING
* ================================================================
  */

private fun orthogonalPoints(
start: Offset,
end: Offset
): List<Offset> {

if (
    abs(start.x - end.x) < 2f ||
    abs(start.y - end.y) < 2f
) {

    return listOf(
        start,
        end
    )
}

val horizontalFirst =
    abs(end.x - start.x) >=
        abs(end.y - start.y)

return if (horizontalFirst) {

    val midX =
        (start.x + end.x) / 2f

    listOf(
        start,
        Offset(
            midX,
            start.y
        ),
        Offset(
            midX,
            end.y
        ),
        end
    )

} else {

    val midY =
        (start.y + end.y) / 2f

    listOf(
        start,
        Offset(
            start.x,
            midY
        ),
        Offset(
            end.x,
            midY
        ),
        end
    )
}

}

/*

* ================================================================
* CONNECTION DRAWING
* ================================================================
  */

fun DrawScope.drawConnection(
connection: SldConnection,
nodes: List<SldNode>,
connections: List<SldConnection>,
selected: Boolean,
textMeasurer: TextMeasurer,
feederResult:
SldUpstreamEngineering.FeederResult? = null
) {

val from =
    nodes.firstOrNull {
        it.id ==
            connection.fromNodeId
    } ?: return

val to =
    nodes.firstOrNull {
        it.id ==
            connection.toNodeId
    } ?: return

val start =
    connectionStartPort(
        node = from,
        connection = connection,
        connections = connections,
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
        start,
        end
    )

val path =
    Path().apply {

        moveTo(
            points.first().x,
            points.first().y
        )

        points.drop(1).forEach {
            lineTo(
                it.x,
                it.y
            )
        }
    }

val isBusbar =
    connection.connectionType ==
        SldConnectionType.BUSBAR

val cableFault =
    feederResult != null &&
        !feederResult.cableAdequate

val lineColor =
    when {

        selected ->
            SelectedColor

        cableFault ->
            FaultColor

        isBusbar ->
            BusbarColor

        else ->
            CableColor
    }

val lineWidth =
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
    color = lineColor,
    style =
        Stroke(
            width = lineWidth,
            cap = StrokeCap.Square,
            join = StrokeJoin.Miter
        )
)

/*
 * Electrical flow arrow.
 *
 * BUSBAR itself is represented as a physical bus and does not
 * receive a directional arrow.
 */

if (
    !isBusbar
) {

    drawFlowArrowOnPath(
        points = points,
        color = lineColor
    )
}

val labelPoint =
    calculateLabelPoint(
        points
    )

if (isBusbar) {

    val busText =
        buildString {

            append("BUS")

            if (
                connection.busbarRatedCurrentA >
                0.0
            ) {

                append("  ")
                append(
                    fmt(
                        connection.busbarRatedCurrentA
                    )
                )
                append(" A")
            }

            if (
                connection.busbarShortCircuitKA >
                0.0
            ) {

                append("  ")
                append(
                    fmt(
                        connection.busbarShortCircuitKA
                    )
                )
                append(" kA")
            }
        }

    drawEngineeringLabel(
        textMeasurer =
            textMeasurer,

        text =
            busText,

        point =
            labelPoint.copy(
                y =
                    labelPoint.y -
                        13f
            ),

        color =
            BusbarColor,

        fontSize =
            8.5f
    )

} else {

    val cableText =
        buildString {

            if (
                connection.cableSizeMm2 >
                0.0
            ) {

                append(
                    fmt(
                        connection.cableSizeMm2
                    )
                )

                append(" mm²")

                if (
                    connection.parallelRuns >
                    1
                ) {

                    append(" × ")
                    append(
                        connection.parallelRuns
                    )
                }
            }

            if (
                connection.lengthMeters >
                0.0
            ) {

                if (isNotEmpty()) {
                    append("  ")
                }

                append(
                    fmt(
                        connection.lengthMeters
                    )
                )

                append(" m")
            }

            if (isEmpty()) {
                append("FEEDER")
            }
        }

    drawEngineeringLabel(
        textMeasurer =
            textMeasurer,

        text =
            cableText,

        point =
            labelPoint.copy(
                y =
                    labelPoint.y -
                        13f
            ),

        color =
            if (cableFault) {
                FaultColor
            } else {
                TextSecondary
            },

        fontSize =
            8.5f
    )

    feederResult?.let {
        result ->

        val engineeringColor =
            when {

                !result.cableAdequate ->
                    FaultColor

                result.voltageDropPercent >
                    3.0 ->
                    WarningColor

                else ->
                    EngineeringColor
            }

        drawEngineeringLabel(
            textMeasurer =
                textMeasurer,

            text =
                "Ib=${fmt(result.currentA)} A   " +
                    "S=${fmt(result.kva)} kVA   " +
                    "ΔV=${fmt(result.voltageDropPercent)}%",

            point =
                labelPoint.copy(
                    y =
                        labelPoint.y +
                            5f
                ),

            color =
                engineeringColor,

            fontSize =
                7.5f
        )
    }
}

}

private fun calculateLabelPoint(
points: List<Offset>
): Offset {

if (points.size <= 2) {

    return Offset(
        (
            points.first().x +
                points.last().x
            ) / 2f,

        (
            points.first().y +
                points.last().y
            ) / 2f
    )
}

val middle =
    points.size / 2

return points[middle]

}

/*

* ================================================================
* FLOW ARROW
* ================================================================
  */

private fun DrawScope.drawFlowArrowOnPath(
points: List<Offset>,
color: Color
) {

if (
    points.size < 2
) {
    return
}

/*
 * Use the final route segment so the arrow always points toward
 * the electrical destination.
 */

val endIndex =
    points.lastIndex

val from =
    points[
        endIndex - 1
    ]

val to =
    points[
        endIndex
    ]

drawFlowArrow(
    from = from,
    to = to,
    color = color
)

}

private fun DrawScope.drawFlowArrow(
from: Offset,
to: Offset,
color: Color
) {

val dx =
    to.x - from.x

val dy =
    to.y - from.y

val length =
    sqrt(
        dx * dx +
            dy * dy
    )

if (
    length < 8f
) {
    return
}

val ux =
    dx / length

val uy =
    dy / length

val arrowLength =
    9f

val arrowWidth =
    5f

val base =
    Offset(
        to.x -
            ux * arrowLength,

        to.y -
            uy * arrowLength
    )

val px =
    -uy

val py =
    ux

val path =
    Path().apply {

        moveTo(
            to.x,
            to.y
        )

        lineTo(
            base.x +
                px * arrowWidth,

            base.y +
                py * arrowWidth
        )

        lineTo(
            base.x -
                px * arrowWidth,

            base.y -
                py * arrowWidth
        )

        close()
    }

drawPath(
    path = path,
    color = color
)

}

/*

* ================================================================
* NODE DRAWING
* ================================================================
  */

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
    node.x +
        NODE_WIDTH / 2f

val symbolY =
    node.y +
        SYMBOL_OFFSET_Y

/*
 * Selection halo.
 *
 * No rectangular boxes are drawn around equipment.
 */

if (
    selected ||
    connectionStart
) {

    drawCircle(
        color =
            if (connectionStart) {
                StartColor
            } else {
                SelectedColor
            },

        radius =
            39f,

        center =
            Offset(
                centerX,
                symbolY
            ),

        style =
            Stroke(
                width =
                    3f
            )
    )
}

/*
 * Equipment symbol.
 */

when (node.type) {

    SldNodeType.SOURCE ->
        drawSourceSymbol(
            centerX,
            symbolY
        )

    SldNodeType.TRANSFORMER ->
        drawTransformerSymbol(
            centerX,
            symbolY
        )

    SldNodeType.GENERATOR ->
        drawGeneratorSymbol(
            centerX,
            symbolY
        )

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
                it.fromNodeId ==
                    node.id
            }

        val incoming =
            connections.firstOrNull {
                it.toNodeId ==
                    node.id
            }

        val connected =
            when {

                outgoing != null ->
                    nodes.firstOrNull {
                        it.id ==
                            outgoing.toNodeId
                    }

                incoming != null ->
                    nodes.firstOrNull {
                        it.id ==
                            incoming.fromNodeId
                    }

                else ->
                    null
            }

        val direction =
            connected?.let {
                nodeDirection(
                    node,
                    it
                )
            }
                ?: Direction.DOWN

        drawBreakerSymbol(
            centerX,
            symbolY,
            direction
        )
    }

    SldNodeType.LOAD ->
        drawLoadSymbol(
            centerX,
            symbolY
        )
}

/*
 * Equipment type.
 */

drawCenteredText(
    textMeasurer =
        textMeasurer,

    text =
        equipmentTypeLabel(
            node.type
        ),

    centerX =
        centerX,

    y =
        node.y +
            51f,

    style =
        TextStyle(
            color =
                TextSecondary,

            fontSize =
                7.5.sp,

            fontWeight =
                FontWeight.Bold
        )
)

/*
 * Equipment tag/name.
 */

drawCenteredText(
    textMeasurer =
        textMeasurer,

    text =
        node.name
            .take(24),

    centerX =
        centerX,

    y =
        node.y +
            64f,

    style =
        TextStyle(
            color =
                TextPrimary,

            fontSize =
                10.sp,

            fontWeight =
                FontWeight.Bold
        )
)

/*
 * Primary electrical data.
 */

val baseElectrical =
    buildString {

        append("V=")
        append(
            fmt(
                node.voltage
            )
        )
        append(" V")

        if (
            node.loadKw >
            0.0
        ) {

            append("  P=")
            append(
                fmt(
                    node.loadKw
                )
            )
            append(" kW")
        }

        if (
            node.ratedKva >
            0.0
        ) {

            append("  R=")
            append(
                fmt(
                    node.ratedKva
                )
            )
            append(" kVA")
        }
    }

drawCenteredText(
    textMeasurer =
        textMeasurer,

    text =
        baseElectrical,

    centerX =
        centerX,

    y =
        node.y +
            79f,

    style =
        TextStyle(
            color =
                TextSecondary,

            fontSize =
                7.2.sp
        )
)

/*
 * Engineering result.
 */

engineeringResult?.let {
    result ->

    drawCenteredText(
        textMeasurer =
            textMeasurer,

        text =
            "Pdem=" +
                fmt(
                    result.demandKw
                ) +
                " kW  S=" +
                fmt(
                    result.kva
                ) +
                " kVA",

        centerX =
            centerX,

        y =
            node.y +
                92f,

        style =
            TextStyle(
                color =
                    TextSecondary,

                fontSize =
                    7.2.sp
            )
    )

    val engineeringColor =
        when {

            result.loadingPercent >
                100.0 ->
                FaultColor

            result.voltageDropPercent >
                3.0 ->
                WarningColor

            else ->
                EngineeringColor
        }

    drawCenteredText(
        textMeasurer =
            textMeasurer,

        text =
            "Ib=" +
                fmt(
                    result.currentA
                ) +
                " A  CB=" +
                fmt(
                    result.recommendedBreakerA
                ) +
                " A  ΔV=" +
                fmt(
                    result.voltageDropPercent
                ) +
                "%",

        centerX =
            centerX,

        y =
            node.y +
                106f,

        style =
            TextStyle(
                color =
                    engineeringColor,

                fontSize =
                    7.2.sp,

                fontWeight =
                    FontWeight.Bold
            )
    )

} ?: run {

    if (
        node.type ==
            SldNodeType.TRANSFORMER &&
        node.ratedKva >
            0.0
    ) {

        drawCenteredText(
            textMeasurer =
                textMeasurer,

            text =
                "Z=" +
                    fmt(
                        node.transformerPercentZ
                    ) +
                    "%",

            centerX =
                centerX,

            y =
                node.y +
                    95f,

            style =
                TextStyle(
                    color =
                        TextSecondary,

                    fontSize =
                        7.sp
                )
        )
    }
}

}

/*

* ================================================================
* SOURCE SYMBOL
* ================================================================
  */

private fun DrawScope.drawSourceSymbol(
x: Float,
y: Float
) {

drawCircle(
    color =
        ElectricalBlack,

    radius =
        25f,

    center =
        Offset(
            x,
            y
        ),

    style =
        Stroke(
            width =
                2.8f
        )
)

/*
 * Utility source AC symbol.
 */

drawArc(
    color =
        ElectricalBlack,

    startAngle =
        205f,

    sweepAngle =
        130f,

    useCenter =
        false,

    topLeft =
        Offset(
            x - 14f,
            y - 10f
        ),

    size =
        Size(
            28f,
            20f
        ),

    style =
        Stroke(
            width =
                2.5f
        )
)

drawLine(
    color =
        ElectricalBlack,

    start =
        Offset(
            x - 14f,
            y + 10f
        ),

    end =
        Offset(
            x + 14f,
            y + 10f
        ),

    strokeWidth =
        2.5f
)

}

/*

* ================================================================
* TRANSFORMER
* ================================================================
  */

private fun DrawScope.drawTransformerSymbol(
x: Float,
y: Float
) {

drawCircle(
    color =
        ElectricalBlack,

    radius =
        18f,

    center =
        Offset(
            x - 12f,
            y
        ),

    style =
        Stroke(
            width =
                2.8f
        )
)

drawCircle(
    color =
        ElectricalBlack,

    radius =
        18f,

    center =
        Offset(
            x + 12f,
            y
        ),

    style =
        Stroke(
            width =
                2.8f
        )
)

/*
 * Short primary/secondary terminals.
 */

drawLine(
    color =
        ElectricalBlack,

    start =
        Offset(
            x - 34f,
            y
        ),

    end =
        Offset(
            x - 30f,
            y
        ),

    strokeWidth =
        2.8f
)

drawLine(
    color =
        ElectricalBlack,

    start =
        Offset(
            x + 30f,
            y
        ),

    end =
        Offset(
            x + 34f,
            y
        ),

    strokeWidth =
        2.8f
)

}

/*

* ================================================================
* GENERATOR
* ================================================================
  */

private fun DrawScope.drawGeneratorSymbol(
x: Float,
y: Float
) {

drawCircle(
    color =
        ElectricalBlack,

    radius =
        25f,

    center =
        Offset(
            x,
            y
        ),

    style =
        Stroke(
            width =
                2.8f
        )
)

drawArc(
    color =
        ElectricalBlack,

    startAngle =
        -55f,

    sweepAngle =
        290f,

    useCenter =
        false,

    topLeft =
        Offset(
            x - 15f,
            y - 15f
        ),

    size =
        Size(
            30f,
            30f
        ),

    style =
        Stroke(
            width =
                2.2f
        )
)

drawLine(
    color =
        ElectricalBlack,

    start =
        Offset(
            x - 10f,
            y
        ),

    end =
        Offset(
            x + 10f,
            y
        ),

    strokeWidth =
        2.2f
)

}

/*

* ================================================================
* BUSBAR
* ================================================================
  */

private fun DrawScope.drawBusbarSymbol(
x: Float,
y: Float,
node: SldNode,
connections: List<SldConnection>
) {

val outgoing =
    connections.filter {
        it.fromNodeId ==
            node.id &&
            it.connectionType ==
            SldConnectionType.BUSBAR
    }

val feederCount =
    max(
        1,
        outgoing.size
    )

/*
 * The bus physically expands with the number of outgoing feeders.
 */

val busWidth =
    max(
        72f,
        min(
            NODE_WIDTH - 16f,
            32f *
                feederCount.toFloat()
        )
    )

drawLine(
    color =
        BusbarColor,

    start =
        Offset(
            x -
                busWidth / 2f,

            y
        ),

    end =
        Offset(
            x +
                busWidth / 2f,

            y
        ),

    strokeWidth =
        BUSBAR_WIDTH,

    cap =
        StrokeCap.Square
)

outgoing.forEachIndexed {
    index,
    _
->

    val px =
        if (
            outgoing.size == 1
        ) {

            x

        } else {

            x -
                busWidth / 2f +
                index.toFloat() /
                (
                    outgoing.size -
                        1
                    ).toFloat() *
                busWidth
        }

    drawLine(
        color =
            BusbarColor,

        start =
            Offset(
                px,
                y
            ),

        end =
            Offset(
                px,
                y + 23f
            ),

        strokeWidth =
            2.8f
    )
}

}

/*

* ================================================================
* PANEL
* ================================================================
  */

private fun DrawScope.drawPanelSymbol(
x: Float,
y: Float,
node: SldNode,
connections: List<SldConnection>
) {

val outgoing =
    connections.filter {
        it.fromNodeId ==
            node.id &&
            it.connectionType ==
            SldConnectionType.BUSBAR
    }

val count =
    max(
        1,
        outgoing.size
    )

/*
 * Main panel bus.
 */

val width =
    max(
        76f,
        min(
            NODE_WIDTH - 12f,
            count * 34f
        )
    )

drawLine(
    color =
        ElectricalBlack,

    start =
        Offset(
            x -
                width / 2f,

            y
        ),

    end =
        Offset(
            x +
                width / 2f,

            y
        ),

    strokeWidth =
        7f,

    cap =
        StrokeCap.Square
)

/*
 * Incoming feeder.
 */

drawLine(
    color =
        ElectricalBlack,

    start =
        Offset(
            x,
            y - 28f
        ),

    end =
        Offset(
            x,
            y
        ),

    strokeWidth =
        2.8f
)

/*
 * Outgoing breaker positions.
 */

outgoing.forEachIndexed {
    index,
    _
->

    val px =
        if (
            outgoing.size == 1
        ) {

            x

        } else {

            x -
                width / 2f +
                index.toFloat() /
                (
                    outgoing.size -
                        1
                    ).toFloat() *
                width
        }

    drawLine(
        color =
            ElectricalBlack,

        start =
            Offset(
                px,
                y
            ),

        end =
            Offset(
                px,
                y + 27f
            ),

        strokeWidth =
            2.8f
    )
}

}

/*

* ================================================================
* BREAKER
* ================================================================
  */

private fun DrawScope.drawBreakerSymbol(
x: Float,
y: Float,
direction: Direction
) {

when (
    direction
) {

    Direction.RIGHT,
    Direction.LEFT -> {

        val sign =
            if (
                direction ==
                Direction.RIGHT
            ) {
                1f
            } else {
                -1f
            }

        val leftTerminal =
            Offset(
                x -
                    30f * sign,
                y
            )

        val pivot =
            Offset(
                x -
                    8f * sign,
                y
            )

        val blade =
            Offset(
                x +
                    17f * sign,
                y -
                    17f
            )

        val rightTerminal =
            Offset(
                x +
                    31f * sign,
                y -
                    17f
            )

        drawLine(
            color =
                ElectricalBlack,

            start =
                leftTerminal,

            end =
                pivot,

            strokeWidth =
                2.8f
        )

        drawCircle(
            color =
                ElectricalBlack,

            radius =
                3.2f,

            center =
                pivot
        )

        drawLine(
            color =
                ElectricalBlack,

            start =
                pivot,

            end =
                blade,

            strokeWidth =
                3.2f
        )

        drawLine(
            color =
                ElectricalBlack,

            start =
                blade,

            end =
                rightTerminal,

            strokeWidth =
                2.8f
        )

        drawCircle(
            color =
                ElectricalBlack,

            radius =
                3.2f,

            center =
                rightTerminal
        )
    }

    Direction.DOWN,
    Direction.UP -> {

        val sign =
            if (
                direction ==
                Direction.DOWN
            ) {
                1f
            } else {
                -1f
            }

        val upperTerminal =
            Offset(
                x,
                y -
                    30f * sign
            )

        val pivot =
            Offset(
                x,
                y -
                    8f * sign
            )

        val blade =
            Offset(
                x +
                    17f,
                y +
                    17f * sign
            )

        val lowerTerminal =
            Offset(
                x +
                    17f,
                y +
                    31f * sign
            )

        drawLine(
            color =
                ElectricalBlack,

            start =
                upperTerminal,

            end =
                pivot,

            strokeWidth =
                2.8f
        )

        drawCircle(
            color =
                ElectricalBlack,

            radius =
                3.2f,

            center =
                pivot
        )

        drawLine(
            color =
                ElectricalBlack,

            start =
                pivot,

            end =
                blade,

            strokeWidth =
                3.2f
        )

        drawLine(
            color =
                ElectricalBlack,

            start =
                blade,

            end =
                lowerTerminal,

            strokeWidth =
                2.8f
        )

        drawCircle(
            color =
                ElectricalBlack,

            radius =
                3.2f,

            center =
                lowerTerminal
        )
    }
}

}

/*

* ================================================================
* LOAD
* ================================================================
  */

private fun DrawScope.drawLoadSymbol(
x: Float,
y: Float
) {

drawCircle(
    color =
        ElectricalBlack,

    radius =
        22f,

    center =
        Offset(
            x,
            y
        ),

    style =
        Stroke(
            width =
                2.8f
        )
)

/*
 * Motor/load symbol.
 */

drawLine(
    color =
        ElectricalBlack,

    start =
        Offset(
            x - 12f,
            y + 12f
        ),

    end =
        Offset(
            x + 12f,
            y - 12f
        ),

    strokeWidth =
        2.8f
)

drawLine(
    color =
        ElectricalBlack,

    start =
        Offset(
            x - 12f,
            y - 12f
        ),

    end =
        Offset(
            x + 12f,
            y + 12f
        ),

    strokeWidth =
        1.8f
)

}

/*

* ================================================================
* LABELS
* ================================================================
  */

private fun equipmentTypeLabel(
type: SldNodeType
): String =

when (type) {

    SldNodeType.SOURCE ->
        "UTILITY SOURCE"

    SldNodeType.TRANSFORMER ->
        "TRANSFORMER"

    SldNodeType.GENERATOR ->
        "GENERATOR"

    SldNodeType.BUS ->
        "BUS"

    SldNodeType.PANEL ->
        "PANEL"

    SldNodeType.BREAKER ->
        "BREAKER"

    SldNodeType.LOAD ->
        "LOAD"
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
    textMeasurer =
        textMeasurer,

    text =
        text,

    topLeft =
        Offset(
            centerX -
                measured.size.width /
                2f,

            y
        ),

    style =
        style
)

}

private fun DrawScope.drawEngineeringLabel(
textMeasurer: TextMeasurer,
text: String,
point: Offset,
color: Color,
fontSize: Float = 9f
) {

if (
    text.isBlank()
) {
    return
}

val style =
    TextStyle(
        color =
            color,

        fontSize =
            fontSize.sp,

        fontWeight =
            FontWeight.Bold
    )

val measured =
    textMeasurer.measure(
        text =
            text,

        style =
            style
    )

drawRoundRect(
    color =
        LabelBackground,

    topLeft =
        Offset(
            point.x -
                measured.size.width /
                2f -
                5f,

            point.y -
                3f
        ),

    size =
        Size(
            measured.size.width +
                10f,

            measured.size.height +
                7f
        ),

    cornerRadius =
        CornerRadius(
            3f,
            3f
        )
)

drawText(
    textMeasurer =
        textMeasurer,

    text =
        text,

    topLeft =
        Offset(
            point.x -
                measured.size.width /
                2f,

            point.y
        ),

    style =
        style
)

}

/*

* ================================================================
* HIT TESTING
* ================================================================
  */

fun findNode(
point: Offset,
nodes: List<SldNode>
): SldNode? {

return nodes.lastOrNull { node ->

    point.x >=
        node.x - 12f &&

        point.x <=
        node.x +
            NODE_WIDTH +
            12f &&

        point.y >=
        node.y - 12f &&

        point.y <=
        node.y +
            NODE_HEIGHT +
            12f
}

}

fun findConnection(
point: Offset,
nodes: List<SldNode>,
connections: List<SldConnection>
): SldConnection? {

var best:
    SldConnection? =
    null

var bestDistance =
    Float.MAX_VALUE

connections.forEach {
    connection ->

    val from =
        nodes.firstOrNull {
            it.id ==
                connection.fromNodeId
        } ?: return@forEach

    val to =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
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

    for (
        index in
        0 until
            points.lastIndex
    ) {

        val distance =
            segmentDistance(
                point,
                points[index],
                points[index + 1]
            )

        if (
            distance <
            bestDistance
        ) {

            bestDistance =
                distance

            best =
                connection
        }
    }
}

return if (
    bestDistance <=
    26f
) {

    best

} else {

    null
}

}

private fun segmentDistance(
point: Offset,
a: Offset,
b: Offset
): Float {

val dx =
    b.x - a.x

val dy =
    b.y - a.y

if (
    dx == 0f &&
    dy == 0f
) {

    return distance(
        point,
        a
    )
}

val t =
    (
        (point.x - a.x) * dx +
            (point.y - a.y) * dy
        ) /
        (
            dx * dx +
                dy * dy
            )

val clamped =
    t.coerceIn(
        0f,
        1f
    )

val closest =
    Offset(
        a.x +
            clamped * dx,

        a.y +
            clamped * dy
    )

return distance(
    point,
    closest
)

}

private fun distance(
a: Offset,
b: Offset
): Float {

val dx =
    a.x - b.x

val dy =
    a.y - b.y

return sqrt(
    dx * dx +
        dy * dy
)

}

/*

* ================================================================
* FORMATTER
* ================================================================
* 
* Keep one local formatter here to avoid introducing any new
* conflicting package-level fmt() function.
  */

private fun fmt(
value: Double
): String {

return if (
    value.isFinite()
) {

    "%.2f".format(
        value
    )

} else {

    "0.00"
}

}
