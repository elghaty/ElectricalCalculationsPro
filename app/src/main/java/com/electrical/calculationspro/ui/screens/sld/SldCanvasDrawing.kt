package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldUpstreamEngineering
import kotlin.math.max

/*
 * ================================================================
 * SLD CANVAS DRAWING
 * ================================================================
 *
 * Important design rules:
 *
 * 1. No decorative Card/Box is drawn around electrical equipment.
 * 2. Electrical symbols are drawn directly on the canvas.
 * 3. Labels are positioned below the symbol.
 * 4. Connections are drawn first, then equipment symbols.
 * 5. Busbars are calculated from their connected feeders.
 *
 * This file contains DRAWING ONLY.
 * No calculation logic is implemented here.
 */

/*
 * Canvas-space dimensions.
 */
private const val NODE_WIDTH = 150f
private const val NODE_HEIGHT = 120f

private const val MAIN_BUS_MIN_HALF_WIDTH = 90f
private const val MAIN_BUS_EXTRA_PER_FEEDER = 42f

private const val CONNECTION_STROKE = 3.5f
private const val BUSBAR_STROKE = 8f

private val TextPrimary = Color(0xFF17212B)
private val TextSecondary = Color(0xFF52606D)
private val NodeBorder = Color(0xFF263238)
private val BusbarColor = Color(0xFF263238)
private val CableColor = Color(0xFF37474F)
private val SelectedColor = Color(0xFF1976D2)
private val StartColor = Color(0xFFFF9800)
private val EngineeringColor = Color(0xFF2E7D32)
private val WarningColor = Color(0xFFEF6C00)
private val FaultColor = Color(0xFFC62828)

/**
 * Draw complete SLD.
 */
fun DrawScope.drawSldNetwork(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    connectionStartId: String?,
    engineeringResults:
        Map<String, SldUpstreamEngineering.NodeResult>,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
) {
    /*
     * Connections must be behind symbols.
     */
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
            drawConnection(
                connection = connection,
                from = from,
                to = to,
                selected =
                    connection.id ==
                        selectedConnectionId
            )
        }
    }

    /*
     * Equipment is drawn after cables.
     */
    nodes.forEach { node ->
        drawNode(
            node = node,
            selected =
                node.id ==
                    selectedNodeId,
            connectionStart =
                node.id ==
                    connectionStartId,
            textMeasurer = textMeasurer,
            engineeringResult =
                engineeringResults[node.id],
            connectedConnections =
                connections.count {
                    it.fromNodeId == node.id ||
                        it.toNodeId == node.id
                }
        )
    }
}

/**
 * Draw one electrical connection.
 */
fun DrawScope.drawConnection(
    connection: SldConnection,
    from: SldNode,
    to: SldNode,
    selected: Boolean
) {
    val start =
        nodeConnectionPoint(
            from,
            to
        )

    val end =
        nodeConnectionPoint(
            to,
            from
        )

    val color =
        if (selected) {
            SelectedColor
        } else {
            CableColor
        }

    when (connection.connectionType) {

        SldConnectionType.BUSBAR -> {
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth =
                    if (selected)
                        BUSBAR_STROKE + 2f
                    else
                        BUSBAR_STROKE,
                cap = StrokeCap.Square
            )
        }

        SldConnectionType.CABLE -> {
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth =
                    if (selected)
                        CONNECTION_STROKE + 2f
                    else
                        CONNECTION_STROKE,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Find the point on the symbol boundary that should receive a feeder.
 */
private fun nodeConnectionPoint(
    node: SldNode,
    other: SldNode
): Offset {
    val center =
        Offset(
            node.x + NODE_WIDTH / 2f,
            node.y + 28f
        )

    val otherCenter =
        Offset(
            other.x + NODE_WIDTH / 2f,
            other.y + 28f
        )

    val dx =
        otherCenter.x -
            center.x

    val dy =
        otherCenter.y -
            center.y

    /*
     * Prefer orthogonal electrical representation.
     */
    return if (
        kotlin.math.abs(dx) >=
            kotlin.math.abs(dy)
    ) {
        Offset(
            x =
                if (dx >= 0)
                    center.x + 32f
                else
                    center.x - 32f,
            y = center.y
        )
    } else {
        Offset(
            x = center.x,
            y =
                if (dy >= 0)
                    center.y + 28f
                else
                    center.y - 28f
        )
    }
}

/**
 * Draw one SLD node.
 *
 * There is deliberately NO surrounding rectangle.
 */
fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: TextMeasurer,
    engineeringResult:
        SldUpstreamEngineering.NodeResult? = null,
    connectedConnections: Int = 0
) {
    val centerX =
        node.x +
            NODE_WIDTH / 2f

    val symbolY =
        node.y + 28f

    /*
     * Selection marker only.
     * This is NOT an equipment box.
     */
    if (selected || connectionStart) {
        drawCircle(
            color =
                if (connectionStart)
                    StartColor
                else
                    SelectedColor,
            radius = 34f,
            center =
                Offset(
                    centerX,
                    symbolY
                ),
            style =
                androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.5f
                )
        )
    }

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
                x = centerX,
                y = symbolY,
                connectedConnections =
                    connectedConnections
            )

        SldNodeType.PANEL ->
            drawPanelSymbol(
                centerX,
                symbolY
            )

        SldNodeType.BREAKER ->
            drawBreakerSymbol(
                centerX,
                symbolY
            )

        SldNodeType.LOAD ->
            drawLoadSymbol(
                centerX,
                symbolY
            )
    }

    drawCenteredText(
        textMeasurer = textMeasurer,
        text =
            equipmentTypeLabel(
                node.type
            ),
        centerX = centerX,
        topY = node.y + 58f,
        style =
            TextStyle(
                color = TextSecondary,
                fontSize = 7.5.sp
            )
    )

    drawCenteredText(
        textMeasurer = textMeasurer,
        text =
            node.name
                .take(24),
        centerX = centerX,
        topY = node.y + 72f,
        style =
            TextStyle(
                color = TextPrimary,
                fontSize = 11.sp
            )
    )

    val electrical =
        buildString {

            append(
                "${fmt(node.voltage)} V"
            )

            if (node.loadKw > 0.0) {
                append(
                    "   ${fmt(node.loadKw)} kW"
                )
            }

            if (node.ratedKva > 0.0) {
                append(
                    "   ${fmt(node.ratedKva)} kVA"
                )
            }
        }

    drawCenteredText(
        textMeasurer = textMeasurer,
        text = electrical,
        centerX = centerX,
        topY = node.y + 88f,
        style =
            TextStyle(
                color = TextSecondary,
                fontSize = 8.sp
            )
    )

    if (engineeringResult != null) {

        val engineering =
            "Ib ${
                fmt(
                    engineeringResult.currentA
                )
            } A   ΔV ${
                fmt(
                    engineeringResult.voltageDropPercent
                )
            }%"

        val color =
            when {

                engineeringResult.loadingPercent >
                    100.0 ->
                    FaultColor

                engineeringResult.voltageDropPercent >
                    3.0 ->
                    WarningColor

                else ->
                    EngineeringColor
            }

        drawCenteredText(
            textMeasurer = textMeasurer,
            text = engineering,
            centerX = centerX,
            topY = node.y + 103f,
            style =
                TextStyle(
                    color = color,
                    fontSize = 8.sp
                )
        )
    }
}

/**
 * Dynamic busbar symbol.
 *
 * The width is based on the number of connected feeders.
 * It therefore grows as additional loads/feeders are added.
 */
private fun DrawScope.drawBusbarSymbol(
    x: Float,
    y: Float,
    connectedConnections: Int
) {
    val feederCount =
        max(
            1,
            connectedConnections
        )

    val halfWidth =
        max(
            MAIN_BUS_MIN_HALF_WIDTH,
            MAIN_BUS_MIN_HALF_WIDTH +
                (
                    feederCount - 2
                ).coerceAtLeast(0) *
                MAIN_BUS_EXTRA_PER_FEEDER
        )

    drawLine(
        color = BusbarColor,
        start =
            Offset(
                x - halfWidth,
                y
            ),
        end =
            Offset(
                x + halfWidth,
                y
            ),
        strokeWidth =
            BUSBAR_STROKE,
        cap = StrokeCap.Square
    )

    /*
     * Small end caps make the busbar visually clear.
     */
    drawLine(
        color = BusbarColor,
        start =
            Offset(
                x - halfWidth,
                y - 10f
            ),
        end =
            Offset(
                x - halfWidth,
                y + 10f
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = BusbarColor,
        start =
            Offset(
                x + halfWidth,
                y - 10f
            ),
        end =
            Offset(
                x + halfWidth,
                y + 10f
            ),
        strokeWidth = 3f
    )
}

/**
 * Panelboard.
 *
 * No enclosing rectangle.
 */
private fun DrawScope.drawPanelSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x,
                y - 28f
            ),
        end =
            Offset(
                x,
                y + 28f
            ),
        strokeWidth = 7f
    )

    listOf(
        -18f,
        0f,
        18f
    ).forEach { offset ->

        drawLine(
            color = NodeBorder,
            start =
                Offset(
                    x - 17f,
                    y + offset
                ),
            end =
                Offset(
                    x + 17f,
                    y + offset
                ),
            strokeWidth = 2.5f
        )
    }
}

/**
 * Breaker.
 *
 * Open switch representation, no rectangle.
 */
private fun DrawScope.drawBreakerSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x - 30f,
                y
            ),
        end =
            Offset(
                x - 9f,
                y
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x - 9f,
                y
            ),
        end =
            Offset(
                x + 18f,
                y - 17f
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x + 18f,
                y - 17f
            ),
        end =
            Offset(
                x + 30f,
                y - 17f
            ),
        strokeWidth = 3f
    )

    drawCircle(
        color = NodeBorder,
        radius = 3.5f,
        center =
            Offset(
                x - 9f,
                y
            )
    )
}

/**
 * Load symbol.
 */
private fun DrawScope.drawLoadSymbol(
    x: Float,
    y: Float
) {
    drawCircle(
        color = NodeBorder,
        radius = 22f,
        center =
            Offset(
                x,
                y
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3f
            )
    )

    drawLine(
        color = NodeBorder,
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
        strokeWidth = 3f
    )
}

/**
 * Source symbol.
 */
private fun DrawScope.drawSourceSymbol(
    x: Float,
    y: Float
) {
    drawCircle(
        color = NodeBorder,
        radius = 24f,
        center =
            Offset(
                x,
                y
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3f
            )
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x - 13f,
                y
            ),
        end =
            Offset(
                x + 13f,
                y
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x,
                y - 13f
            ),
        end =
            Offset(
                x,
                y + 13f
            ),
        strokeWidth = 3f
    )
}

/**
 * Transformer symbol.
 */
private fun DrawScope.drawTransformerSymbol(
    x: Float,
    y: Float
) {
    drawCircle(
        color = NodeBorder,
        radius = 17f,
        center =
            Offset(
                x - 11f,
                y
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3f
            )
    )

    drawCircle(
        color = NodeBorder,
        radius = 17f,
        center =
            Offset(
                x + 11f,
                y
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3f
            )
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x - 33f,
                y
            ),
        end =
            Offset(
                x - 28f,
                y
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x + 28f,
                y
            ),
        end =
            Offset(
                x + 33f,
                y
            ),
        strokeWidth = 3f
    )
}

/**
 * Generator symbol.
 */
private fun DrawScope.drawGeneratorSymbol(
    x: Float,
    y: Float
) {
    drawCircle(
        color = NodeBorder,
        radius = 24f,
        center =
            Offset(
                x,
                y
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3f
            )
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x - 13f,
                y + 8f
            ),
        end =
            Offset(
                x - 5f,
                y - 8f
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x - 5f,
                y - 8f
            ),
        end =
            Offset(
                x + 5f,
                y + 8f
            ),
        strokeWidth = 3f
    )

    drawLine(
        color = NodeBorder,
        start =
            Offset(
                x + 5f,
                y + 8f
            ),
        end =
            Offset(
                x + 13f,
                y - 8f
            ),
        strokeWidth = 3f
    )
}

/**
 * Equipment labels.
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
            "BUSBAR"

        SldNodeType.PANEL ->
            "PANELBOARD"

        SldNodeType.BREAKER ->
            "CIRCUIT BREAKER"

        SldNodeType.LOAD ->
            "LOAD"
    }

/**
 * Centered text helper.
 */
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
                centerX -
                    result.size.width / 2f,
                topY
            ),
        style = style
    )
}

/**
 * One consistent number formatter for this package.
 */
private fun fmt(
    value: Double
): String {
    return when {
        value.isNaN() ->
            "—"

        value.isInfinite() ->
            "—"

        kotlin.math.abs(value) >= 1000.0 ->
            "%.0f".format(value)

        kotlin.math.abs(value) >= 100.0 ->
            "%.1f".format(value)

        else ->
            "%.2f".format(value)
    }
}
