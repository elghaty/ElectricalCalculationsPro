package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
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
const val NODE_HEIGHT = 150f

private const val SYMBOL_Y = 30f

private const val CABLE_WIDTH = 3f
private const val SELECTED_CABLE_WIDTH = 5.5f

private const val BUSBAR_WIDTH = 8f

private val BACKGROUND =
    Color(0xFFF7F9FB)

private val GRID =
    Color(0xFFE3E8EC)

private val GRID_MAJOR =
    Color(0xFFD0D8DE)

private val BLACK =
    Color(0xFF172027)

private val CABLE =
    Color(0xFF37474F)

private val TEXT =
    Color(0xFF172027)

private val TEXT_SECONDARY =
    Color(0xFF60717A)

private val SELECTED =
    Color(0xFF1565C0)

private val START =
    Color(0xFFFF9800)

private val OK =
    Color(0xFF087F5B)

private val WARNING =
    Color(0xFFE67700)

private val FAULT =
    Color(0xFFC62828)

private val LABEL_BG =
    Color.White.copy(
        alpha = 0.97f
    )

private enum class Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

private data class PanelBreakerPair(
    val panel: SldNode,
    val breaker: SldNode
)

/*
 * ================================================================
 * INTERNAL PANEL DETECTION
 * ================================================================
 */

private fun isInternalPanelType(
    type: SldNodeType
): Boolean {

    return type ==
        SldNodeType.PANEL ||
        type ==
        SldNodeType.BUS ||
        type ==
        SldNodeType.BREAKER
}

private fun isPanelBreakerPair(
    first: SldNode,
    second: SldNode
): Boolean {

    return (
        first.type ==
            SldNodeType.PANEL &&
            second.type ==
            SldNodeType.BREAKER
        ) ||
        (
            first.type ==
                SldNodeType.BREAKER &&
                second.type ==
                    SldNodeType.PANEL
            )
}

private fun panelBreakerPair(
    connection: SldConnection,
    nodes: List<SldNode>
): PanelBreakerPair? {

    val from =
        nodes.firstOrNull {
            it.id ==
                connection.fromNodeId
        } ?: return null

    val to =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
        } ?: return null

    if (
        !isPanelBreakerPair(
            from,
            to
        )
    ) {
        return null
    }

    return if (
        from.type ==
            SldNodeType.PANEL
    ) {

        PanelBreakerPair(
            panel = from,
            breaker = to
        )

    } else {

        PanelBreakerPair(
            panel = to,
            breaker = from
        )
    }
}

private fun isBusbarConnection(
    connection: SldConnection,
    nodes: List<SldNode>
): Boolean {

    if (
        connection.connectionType ==
            SldConnectionType.BUSBAR
    ) {
        return true
    }

    val from =
        nodes.firstOrNull {
            it.id ==
                connection.fromNodeId
        } ?: return false

    val to =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
        } ?: return false

    return (
        isInternalPanelType(
            from.type
        ) &&
        isInternalPanelType(
            to.type
        )
        )
}

/*
 * ================================================================
 * GEOMETRY
 * ================================================================
 */

private fun nodeCenter(
    node: SldNode
): Offset {

    return Offset(
        node.x +
            NODE_WIDTH /
            2f,

        node.y +
            SYMBOL_Y
    )
}

private fun directionBetween(
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
        abs(dx) >=
            abs(dy)
    ) {

        if (
            dx >= 0f
        ) {
            Direction.RIGHT
        } else {
            Direction.LEFT
        }

    } else {

        if (
            dy >= 0f
        ) {
            Direction.DOWN
        } else {
            Direction.UP
        }
    }
}

private fun standardPort(
    node: SldNode,
    direction: Direction
): Offset {

    return when (
        direction
    ) {

        Direction.LEFT ->
            Offset(
                node.x,
                node.y +
                    SYMBOL_Y
            )

        Direction.RIGHT ->
            Offset(
                node.x +
                    NODE_WIDTH,
                node.y +
                    SYMBOL_Y
            )

        Direction.UP ->
            Offset(
                node.x +
                    NODE_WIDTH /
                    2f,

                node.y
            )

        Direction.DOWN ->
            Offset(
                node.x +
                    NODE_WIDTH /
                    2f,

                node.y +
                    NODE_HEIGHT
            )
    }
}

private fun startPort(
    node: SldNode,
    connection: SldConnection,
    nodes: List<SldNode>
): Offset {

    val target =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
        }

    if (
        target == null
    ) {
        return standardPort(
            node,
            Direction.DOWN
        )
    }

    return standardPort(
        node,
        directionBetween(
            node,
            target
        )
    )
}

private fun endPort(
    node: SldNode,
    connection: SldConnection,
    nodes: List<SldNode>
): Offset {

    val source =
        nodes.firstOrNull {
            it.id ==
                connection.fromNodeId
        }

    if (
        source == null
    ) {
        return standardPort(
            node,
            Direction.UP
        )
    }

    return standardPort(
        node,
        directionBetween(
            node,
            source
        )
    )
}

/*
 * ================================================================
 * BACKGROUND
 * ================================================================
 */

fun DrawScope.drawSldEngineeringBackground() {

    drawRect(
        color =
            BACKGROUND
    )

    var x = 0f

    while (
        x <= size.width
    ) {

        val major =
            x.toInt() %
                200 ==
                0

        drawLine(
            color =
                if (
                    major
                ) {
                    GRID_MAJOR
                } else {
                    GRID
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
                if (
                    major
                ) {
                    1.4f
                } else {
                    0.7f
                }
        )

        x += 40f
    }

    var y = 0f

    while (
        y <= size.height
    ) {

        val major =
            y.toInt() %
                200 ==
                0

        drawLine(
            color =
                if (
                    major
                ) {
                    GRID_MAJOR
                } else {
                    GRID
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
                if (
                    major
                ) {
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
 * ORTHOGONAL ROUTING
 * ================================================================
 */

private fun route(
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

    val middleX =
        (
            start.x +
                end.x
            ) / 2f

    return listOf(
        start,
        Offset(
            middleX,
            start.y
        ),
        Offset(
            middleX,
            end.y
        ),
        end
    )
}

private fun professionalRoute(
    start: Offset,
    end: Offset
): List<Offset> {

    if (
        abs(start.x - end.x) < 2f
    ) {
        return listOf(
            start,
            end
        )
    }

    if (
        abs(start.y - end.y) < 2f
    ) {
        return listOf(
            start,
            end
        )
    }

    val dx =
        abs(
            end.x -
                start.x
        )

    val dy =
        abs(
            end.y -
                start.y
        )

    return if (
        dx >= dy
    ) {

        val midX =
            (
                start.x +
                    end.x
                ) / 2f

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
            (
                start.y +
                    end.y
                ) / 2f

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
 * EXTERNAL FEEDER
 * ================================================================
 *
 * BUSBAR is NEVER drawn here.
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

    if (
        isBusbarConnection(
            connection,
            nodes
        )
    ) {
        return
    }

    val start =
        startPort(
            node =
                from,

            connection =
                connection,

            nodes =
                nodes
        )

    val end =
        endPort(
            node =
                to,

            connection =
                connection,

            nodes =
                nodes
        )

    val points =
        if (
            connection.routeAuto
        ) {

            professionalRoute(
                start,
                end
            )

        } else if (
            connection.routePoints
                .isNotEmpty()
        ) {

            buildList {

                add(start)

                addAll(
                    connection.routePoints
                        .map {
                            Offset(
                                it.x,
                                it.y
                            )
                        }
                )

                add(end)
            }

        } else {

            route(
                start,
                end
            )
        }

    if (
        points.size < 2
    ) {
        return
    }

    val path =
        Path().apply {

            moveTo(
                points.first().x,
                points.first().y
            )

            points
                .drop(1)
                .forEach {
                    lineTo(
                        it.x,
                        it.y
                    )
                }
        }

    val inadequate =
        feederResult != null &&
            !feederResult.cableAdequate

    val lineColor =
        when {

            selected ->
                SELECTED

            inadequate ->
                FAULT

            else ->
                CABLE
        }

    drawPath(
        path =
            path,

        color =
            lineColor,

        style =
            androidx.compose.ui.graphics
                .drawscope
                .Stroke(
                    width =
                        if (
                            selected
                        ) {
                            SELECTED_CABLE_WIDTH
                        } else {
                            CABLE_WIDTH
                        },

                    cap =
                        StrokeCap.Square,

                    join =
                        StrokeJoin.Miter
                )
    )

    drawFlowArrow(
        points =
            points,

        color =
            lineColor
    )

    val labelPoint =
        points[
            points.size /
                2
        ]

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

                append(
                    " mm²"
                )

                if (
                    connection.parallelRuns >
                        1
                ) {

                    append(
                        " × "
                    )

                    append(
                        connection.parallelRuns
                    )
                }
            }

            if (
                connection.lengthMeters >
                    0.0
            ) {

                if (
                    isNotEmpty()
                ) {
                    append(
                        "  "
                    )
                }

                append(
                    fmt(
                        connection.lengthMeters
                    )
                )

                append(
                    " m"
                )
            }

            if (
                isEmpty()
            ) {
                append(
                    "FEEDER"
                )
            }
        }

    drawEngineeringLabel(
        textMeasurer =
            textMeasurer,

        text =
            cableText,

        point =
            Offset(
                labelPoint.x,
                labelPoint.y - 14f
            ),

        color =
            if (
                inadequate
            ) {
                FAULT
            } else {
                TEXT_SECONDARY
            },

        fontSize =
            7.5f
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

    /*
     * CRITICAL:
     *
     * PANEL and BUS are NOT equipment symbols anymore.
     *
     * PANEL:
     *   rendered by drawPanelEnclosures().
     *
     * BUS:
     *   rendered as a real busbar by drawPanelEnclosures().
     *
     * This prevents:
     *
     *   small PANEL rectangle inside PANEL enclosure
     *   BUS circle/line + cable
     *
     * from appearing together.
     */
    if (
        node.type ==
            SldNodeType.PANEL ||
        node.type ==
            SldNodeType.BUS
    ) {
        return
    }

    val centerX =
        node.x +
            NODE_WIDTH /
            2f

    val symbolY =
        node.y +
            SYMBOL_Y

    if (
        selected ||
        connectionStart
    ) {

        drawCircle(
            color =
                if (
                    connectionStart
                ) {
                    START
                } else {
                    SELECTED
                },

            radius =
                39f,

            center =
                Offset(
                    centerX,
                    symbolY
                ),

            style =
                androidx.compose.ui.graphics
                    .drawscope
                    .Stroke(
                        width =
                            3f
                    )
        )
    }

    when (
        node.type
    ) {

        SldNodeType.SOURCE ->
            drawSource(
                centerX,
                symbolY
            )

        SldNodeType.TRANSFORMER ->
            drawTransformer(
                centerX,
                symbolY
            )

        SldNodeType.GENERATOR ->
            drawGenerator(
                centerX,
                symbolY
            )

        SldNodeType.BREAKER -> {

            val incoming =
                connections.firstOrNull {
                    it.toNodeId ==
                        node.id
                }

            val outgoing =
                connections.firstOrNull {
                    it.fromNodeId ==
                        node.id
                }

            val connected =
                incoming?.let {
                    nodes.firstOrNull {
                        it.id ==
                            incoming.fromNodeId
                    }
                } ?: outgoing?.let {
                    nodes.firstOrNull {
                        it.id ==
                            outgoing.toNodeId
                    }
                }

            val incomingBusbar =
                incoming?.let {
                    isBusbarConnection(
                        it,
                        nodes
                    )
                } == true

            val direction =
                if (
                    incomingBusbar
                ) {

                    Direction.DOWN

                } else {

                    connected?.let {
                        directionBetween(
                            node,
                            it
                        )
                    } ?: Direction.DOWN
                }

            drawBreaker(
                centerX,
                symbolY,
                direction
            )
        }

        SldNodeType.LOAD ->
            drawLoad(
                centerX,
                symbolY
            )

        SldNodeType.PANEL,
        SldNodeType.BUS -> {
            return
        }
    }

    val equipmentY =
        node.y +
            70f

    val nameY =
        node.y +
            84f

    val electricalY =
        node.y +
            100f

    val engineeringY =
        node.y +
            116f

    val resultY =
        node.y +
            132f

    drawCenteredText(
        textMeasurer =
            textMeasurer,

        text =
            equipmentLabel(
                node.type
            ),

        centerX =
            centerX,

        y =
            equipmentY,

        style =
            TextStyle(
                color =
                    TEXT_SECONDARY,

                fontSize =
                    7.5.sp,

                fontWeight =
                    FontWeight.Bold
            )
    )

    val displayName =
        if (
            node.tag.isNotBlank()
        ) {
            "${node.tag}  ${node.name}"
        } else {
            node.name
        }

    drawCenteredText(
        textMeasurer =
            textMeasurer,

        text =
            displayName.take(
                28
            ),

        centerX =
            centerX,

        y =
            nameY,

        style =
            TextStyle(
                color =
                    TEXT,

                fontSize =
                    10.sp,

                fontWeight =
                    FontWeight.Bold
            )
    )

    val electrical =
        buildString {

            append(
                "V="
            )

            append(
                fmt(
                    node.voltage
                )
            )

            append(
                " V  "
            )

            append(
                when (
                    node.phaseSystem.name
                ) {

                    "THREE_PHASE" ->
                        "3Φ"

                    "SINGLE_PHASE" ->
                        "1Φ"

                    else ->
                        "DC"
                }
            )

            if (
                node.loadKw >
                    0.0
            ) {

                append(
                    "  P="
                )

                append(
                    fmt(
                        node.loadKw
                    )
                )

                append(
                    " kW"
                )
            }

            if (
                node.ratedKva >
                    0.0
            ) {

                append(
                    "  R="
                )

                append(
                    fmt(
                        node.ratedKva
                    )
                )

                append(
                    " kVA"
                )
            }
        }

    drawCenteredText(
        textMeasurer =
            textMeasurer,

        text =
            electrical,

        centerX =
            centerX,

        y =
            electricalY,

        style =
            TextStyle(
                color =
                    TEXT_SECONDARY,

                fontSize =
                    7.2.sp
            )
    )

    engineeringResult?.let {

        drawCenteredText(
            textMeasurer =
                textMeasurer,

            text =
                "Pdem=${fmt(it.demandKw)} kW  " +
                    "S=${fmt(it.kva)} kVA",

            centerX =
                centerX,

            y =
                engineeringY,

            style =
                TextStyle(
                    color =
                        TEXT_SECONDARY,

                    fontSize =
                        7.2.sp
                )
        )

        val resultColor =
            when {

                it.loadingPercent >
                    100.0 ->
                    FAULT

                it.voltageDropPercent >
                    3.0 ->
                    WARNING

                else ->
                    OK
            }

        drawCenteredText(
            textMeasurer =
                textMeasurer,

            text =
                "Ib=${fmt(it.currentA)} A  " +
                    "CB=${fmt(it.recommendedBreakerA)} A  " +
                    "ΔV=${fmt(it.voltageDropPercent)}%",

            centerX =
                centerX,

            y =
                resultY,

            style =
                TextStyle(
                    color =
                        resultColor,

                    fontSize =
                        7.2.sp,

                    fontWeight =
                        FontWeight.Bold
                )
        )
    }
}

/*
 * ================================================================
 * SOURCE
 * ================================================================
 */

private fun DrawScope.drawSource(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            BLACK,

        radius =
            25f,

        center =
            Offset(
                x,
                y
            ),

        style =
            androidx.compose.ui.graphics
                .drawscope
                .Stroke(
                    width =
                        2.8f
                )
    )

    val wave =
        Path().apply {

            moveTo(
                x - 14f,
                y
            )

            cubicTo(
                x - 9f,
                y - 11f,
                x - 3f,
                y - 11f,
                x,
                y
            )

            cubicTo(
                x + 3f,
                y + 11f,
                x + 9f,
                y + 11f,
                x + 14f,
                y
            )
        }

    drawPath(
        path =
            wave,

        color =
            BLACK,

        style =
            androidx.compose.ui.graphics
                .drawscope
                .Stroke(
                    width =
                        2.4f,

                    cap =
                        StrokeCap.Round
                )
    )
}

/*
 * ================================================================
 * TRANSFORMER
 * ================================================================
 */

private fun DrawScope.drawTransformer(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            BLACK,

        radius =
            18f,

        center =
            Offset(
                x - 12f,
                y
            ),

        style =
            androidx.compose.ui.graphics
                .drawscope
                .Stroke(
                    width =
                        2.8f
                )
    )

    drawCircle(
        color =
            BLACK,

        radius =
            18f,

        center =
            Offset(
                x + 12f,
                y
            ),

        style =
            androidx.compose.ui.graphics
                .drawscope
                .Stroke(
                    width =
                        2.8f
                )
    )

    drawLine(
        color =
            BLACK,

        start =
            Offset(
                x,
                y - 25f
            ),

        end =
            Offset(
                x,
                y + 25f
            ),

        strokeWidth =
            1.6f
    )
}

/*
 * ================================================================
 * GENERATOR
 * ================================================================
 */

private fun DrawScope.drawGenerator(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            BLACK,

        radius =
            25f,

        center =
            Offset(
                x,
                y
            ),

        style =
            androidx.compose.ui.graphics
                .drawscope
                .Stroke(
                    width =
                        2.8f
                )
    )

    drawArc(
        color =
            BLACK,

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
            androidx.compose.ui.graphics
                .drawscope
                .Stroke(
                    width =
                        2.2f
                )
    )

    drawLine(
        color =
            BLACK,

        start =
            Offset(
                x - 9f,
                y
            ),

        end =
            Offset(
                x + 9f,
                y
            ),

        strokeWidth =
            2f
    )
}

/*
 * ================================================================
 * BREAKER
 * ================================================================
 */

private fun DrawScope.drawBreaker(
    x: Float,
    y: Float,
    direction: Direction
) {

    when (
        direction
    ) {

        Direction.DOWN -> {

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x,
                        y - 31f
                    ),

                end =
                    Offset(
                        x,
                        y - 8f
                    ),

                strokeWidth =
                    2.8f
            )

            drawCircle(
                color =
                    BLACK,

                radius =
                    3.2f,

                center =
                    Offset(
                        x,
                        y - 8f
                    )
            )

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x,
                        y - 8f
                    ),

                end =
                    Offset(
                        x + 17f,
                        y + 10f
                    ),

                strokeWidth =
                    3.2f
            )

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x + 17f,
                        y + 10f
                    ),

                end =
                    Offset(
                        x + 17f,
                        y + 31f
                    ),

                strokeWidth =
                    2.8f
            )

            drawCircle(
                color =
                    BLACK,

                radius =
                    3.2f,

                center =
                    Offset(
                        x + 17f,
                        y + 31f
                    )
            )
        }

        Direction.UP -> {

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x,
                        y + 31f
                    ),

                end =
                    Offset(
                        x,
                        y + 8f
                    ),

                strokeWidth =
                    2.8f
            )

            drawCircle(
                color =
                    BLACK,

                radius =
                    3.2f,

                center =
                    Offset(
                        x,
                        y + 8f
                    )
            )

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x,
                        y + 8f
                    ),

                end =
                    Offset(
                        x + 17f,
                        y - 10f
                    ),

                strokeWidth =
                    3.2f
            )

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x + 17f,
                        y - 10f
                    ),

                end =
                    Offset(
                        x + 17f,
                        y - 31f
                    ),

                strokeWidth =
                    2.8f
            )

            drawCircle(
                color =
                    BLACK,

                radius =
                    3.2f,

                center =
                    Offset(
                        x + 17f,
                        y - 31f
                    )
            )
        }

        Direction.RIGHT -> {

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x - 31f,
                        y
                    ),

                end =
                    Offset(
                        x - 8f,
                        y
                    ),

                strokeWidth =
                    2.8f
            )

            drawCircle(
                color =
                    BLACK,

                radius =
                    3.2f,

                center =
                    Offset(
                        x - 8f,
                        y
                    )
            )

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x - 8f,
                        y
                    ),

                end =
                    Offset(
                        x + 10f,
                        y - 17f
                    ),

                strokeWidth =
                    3.2f
            )

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x + 10f,
                        y - 17f
                    ),

                end =
                    Offset(
                        x + 31f,
                        y - 17f
                    ),

                strokeWidth =
                    2.8f
            )

            drawCircle(
                color =
                    BLACK,

                radius =
                    3.2f,

                center =
                    Offset(
                        x + 31f,
                        y - 17f
                    )
            )
        }

        Direction.LEFT -> {

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x + 31f,
                        y
                    ),

                end =
                    Offset(
                        x + 8f,
                        y
                    ),

                strokeWidth =
                    2.8f
            )

            drawCircle(
                color =
                    BLACK,

                radius =
                    3.2f,

                center =
                    Offset(
                        x + 8f,
                        y
                    )
            )

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x + 8f,
                        y
                    ),

                end =
                    Offset(
                        x - 10f,
                        y - 17f
                    ),

                strokeWidth =
                    3.2f
            )

            drawLine(
                color =
                    BLACK,

                start =
                    Offset(
                        x - 10f,
                        y - 17f
                    ),

                end =
                    Offset(
                        x - 31f,
                        y - 17f
                    ),

                strokeWidth =
                    2.8f
            )

            drawCircle(
                color =
                    BLACK,

                radius =
                    3.2f,

                center =
                    Offset(
                        x - 31f,
                        y - 17f
                    )
            )
        }
    }
}

/*
 * ================================================================
 * LOAD
 * ================================================================
 */

private fun DrawScope.drawLoad(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            BLACK,

        radius =
            22f,

        center =
            Offset(
                x,
                y
            ),

        style =
            androidx.compose.ui.graphics
                .drawscope
                .Stroke(
                    width =
                        2.8f
                )
    )

    drawLine(
        color =
            BLACK,

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
            BLACK,

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

private fun equipmentLabel(
    type: SldNodeType
): String {

    return when (
        type
    ) {

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
}

private fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    text: String,
    centerX: Float,
    y: Float,
    style: TextStyle
) {

    if (
        text.isBlank()
    ) {
        return
    }

    val measured =
        textMeasurer.measure(
            text =
                text,

            style =
                style
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
    fontSize: Float
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
            LABEL_BG,

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
 * FLOW ARROW
 * ================================================================
 */

private fun DrawScope.drawFlowArrow(
    points: List<Offset>,
    color: Color
) {

    if (
        points.size < 2
    ) {
        return
    }

    val a =
        points[
            points.lastIndex - 1
        ]

    val b =
        points.last()

    val dx =
        b.x - a.x

    val dy =
        b.y - a.y

    val length =
        sqrt(
            dx * dx +
                dy * dy
        )

    if (
        length < 14f
    ) {
        return
    }

    val ux =
        dx /
            length

    val uy =
        dy /
            length

    val arrowLength =
        10f

    val arrowWidth =
        5f

    val base =
        Offset(
            b.x -
                ux *
                arrowLength,

            b.y -
                uy *
                arrowLength
        )

    val px =
        -uy

    val py =
        ux

    val path =
        Path().apply {

            moveTo(
                b.x,
                b.y
            )

            lineTo(
                base.x +
                    px *
                    arrowWidth,

                base.y +
                    py *
                    arrowWidth
            )

            lineTo(
                base.x -
                    px *
                    arrowWidth,

                base.y -
                    py *
                    arrowWidth
            )

            close()
        }

    drawPath(
        path =
            path,

        color =
            color
    )
}

/*
 * ================================================================
 * HIT TEST - NODE
 * ================================================================
 */

fun findNode(
    point: Offset,
    nodes: List<SldNode>
): SldNode? {

    return nodes
        .asSequence()
        .filter { node ->

            point.x >=
                node.x -
                12f &&

                point.x <=
                node.x +
                NODE_WIDTH +
                12f &&

                point.y >=
                node.y -
                12f &&

                point.y <=
                node.y +
                NODE_HEIGHT +
                12f
        }
        .minByOrNull { node ->

            val center =
                nodeCenter(
                    node
                )

            val dx =
                point.x -
                    center.x

            val dy =
                point.y -
                    center.y

            dx * dx +
                dy * dy
        }
}

/*
 * ================================================================
 * HIT TEST - CONNECTION
 * ================================================================
 *
 * Internal BUSBAR is intentionally not treated as a cable.
 *
 * External feeder remains selectable.
 */
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

        if (
            isBusbarConnection(
                connection,
                nodes
            )
        ) {
            return@forEach
        }

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

        val points =
            if (
                connection.routePoints
                    .isNotEmpty()
            ) {

                buildList {

                    add(
                        startPort(
                            from,
                            connection,
                            nodes
                        )
                    )

                    addAll(
                        connection.routePoints
                            .map {
                                Offset(
                                    it.x,
                                    it.y
                                )
                            }
                    )

                    add(
                        endPort(
                            to,
                            connection,
                            nodes
                        )
                    )
                }

            } else {

                professionalRoute(
                    startPort(
                        from,
                        connection,
                        nodes
                    ),

                    endPort(
                        to,
                        connection,
                        nodes
                    )
                )
            }

        for (
            index in 0 until
                points.lastIndex
        ) {

            val distance =
                distanceToSegment(
                    point,
                    points[index],
                    points[index + 1]
                )

            if (
                distance < 18f &&
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

    return best
}

private fun distanceToSegment(
    point: Offset,
    a: Offset,
    b: Offset
): Float {

    val dx =
        b.x - a.x

    val dy =
        b.y - a.y

    if (
        abs(dx) < 0.001f &&
        abs(dy) < 0.001f
    ) {

        return sqrt(
            (point.x - a.x) *
                (point.x - a.x) +

                (point.y - a.y) *
                (point.y - a.y)
        )
    }

    val denominator =
        dx * dx +
            dy * dy

    val t =
        (
            (point.x - a.x) *
                dx +

                (point.y - a.y) *
                dy
            ) /
            denominator

    val clamped =
        t.coerceIn(
            0f,
            1f
        )

    val px =
        a.x +
            clamped *
            dx

    val py =
        a.y +
            clamped *
            dy

    return sqrt(
        (point.x - px) *
            (point.x - px) +

            (point.y - py) *
            (point.y - py)
    )
}
