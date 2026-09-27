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
import kotlin.math.sqrt

/*
 * ================================================================
 * PROFESSIONAL SLD DRAWING
 * ================================================================
 *
 * Drawing layer only.
 *
 * Rules:
 * 1. No engineering calculations here.
 * 2. No duplicate fmt() function.
 * 3. Text formatting is provided by SldFormatter.kt.
 * 4. Compatible with SldCanvas.kt.
 * 5. No large decorative equipment cards.
 * 6. Electrical symbols are drawn directly on the canvas.
 * 7. Selection uses a clean engineering highlight.
 * 8. Hit testing remains available to SldCanvas.
 *
 * ================================================================
 */

/*
 * These constants are intentionally public because
 * SldCanvas and SldEngineeringOverlay use them.
 */
const val NODE_WIDTH = 180f
const val NODE_HEIGHT = 118f

private const val SYMBOL_OFFSET_Y = 31f

private const val NORMAL_CONNECTION_WIDTH = 4f
private const val SELECTED_CONNECTION_WIDTH = 7f
private const val BUSBAR_WIDTH = 8f

private val Background =
    Color(0xFFF8FAFC)

private val GridColor =
    Color(0xFFE4E9EE)

private val NodeBorder =
    Color(0xFF263238)

private val TextPrimary =
    Color(0xFF172027)

private val TextSecondary =
    Color(0xFF60717A)

private val CableColor =
    Color(0xFF37474F)

private val BusbarColor =
    Color(0xFF263238)

private val SelectedColor =
    Color(0xFF1976D2)

private val StartColor =
    Color(0xFFFF9800)

private val EngineeringColor =
    Color(0xFF087F5B)

private val WarningColor =
    Color(0xFFE67700)

private val FaultColor =
    Color(0xFFC92A2A)


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

        drawLine(
            color = GridColor,
            start = Offset(
                x,
                0f
            ),
            end = Offset(
                x,
                size.height
            ),
            strokeWidth = 1f
        )

        x += 40f
    }

    var y = 0f

    while (y <= size.height) {

        drawLine(
            color = GridColor,
            start = Offset(
                0f,
                y
            ),
            end = Offset(
                size.width,
                y
            ),
            strokeWidth = 1f
        )

        y += 40f
    }
}


/*
 * ================================================================
 * CONNECTION
 * ================================================================
 */

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
            it.id ==
                connection.fromNodeId
        }
            ?: return

    val to =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
        }
            ?: return

    val fromCenter =
        Offset(
            from.x +
                NODE_WIDTH / 2f,

            from.y +
                NODE_HEIGHT / 2f
        )

    val toCenter =
        Offset(
            to.x +
                NODE_WIDTH / 2f,

            to.y +
                NODE_HEIGHT / 2f
        )

    val dx =
        toCenter.x -
            fromCenter.x

    val dy =
        toCenter.y -
            fromCenter.y

    val path =
        Path()

    val labelPoint: Offset

    /*
     * ------------------------------------------------------------
     * Vertical connection
     * ------------------------------------------------------------
     */

    if (abs(dx) < 35f) {

        val down =
            dy >= 0f

        val start =
            if (down) {

                Offset(
                    fromCenter.x,
                    from.y +
                        NODE_HEIGHT
                )

            } else {

                Offset(
                    fromCenter.x,
                    from.y
                )
            }

        val end =
            if (down) {

                Offset(
                    toCenter.x,
                    to.y
                )

            } else {

                Offset(
                    toCenter.x,
                    to.y +
                        NODE_HEIGHT
                )
            }

        path.moveTo(
            start.x,
            start.y
        )

        path.lineTo(
            end.x,
            end.y
        )

        labelPoint =
            Offset(
                end.x + 18f,
                (start.y + end.y) / 2f
            )

    } else {

        /*
         * --------------------------------------------------------
         * Orthogonal engineering connection
         * --------------------------------------------------------
         */

        val start =
            Offset(
                fromCenter.x,
                from.y +
                    NODE_HEIGHT
            )

        val end =
            Offset(
                toCenter.x,
                to.y
            )

        val middleY =
            (
                start.y +
                    end.y
                ) / 2f

        path.moveTo(
            start.x,
            start.y
        )

        path.lineTo(
            start.x,
            middleY
        )

        path.lineTo(
            end.x,
            middleY
        )

        path.lineTo(
            end.x,
            end.y
        )

        labelPoint =
            Offset(
                (start.x + end.x) / 2f,
                middleY - 22f
            )
    }

    val isBusbar =
        connection.connectionType ==
            SldConnectionType.BUSBAR

    val cableFault =
        feederResult != null &&
            !feederResult.cableAdequate

    val color =
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

    val width =
        when {

            selected ->
                if (isBusbar) {
                    BUSBAR_WIDTH + 2f
                } else {
                    SELECTED_CONNECTION_WIDTH
                }

            isBusbar ->
                BUSBAR_WIDTH

            else ->
                NORMAL_CONNECTION_WIDTH
        }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = width,
            cap =
                if (isBusbar) {
                    StrokeCap.Square
                } else {
                    StrokeCap.Round
                }
        )
    )

    if (isBusbar) {

        drawBusbarLabel(
            textMeasurer =
                textMeasurer,

            point =
                labelPoint,

            connection =
                connection
        )

    } else {

        drawCableLabel(
            textMeasurer =
                textMeasurer,

            point =
                labelPoint,

            connection =
                connection,

            feederResult =
                feederResult
        )
    }
}


/*
 * ================================================================
 * BUSBAR LABEL
 * ================================================================
 */

private fun DrawScope.drawBusbarLabel(
    textMeasurer: TextMeasurer,
    point: Offset,
    connection: SldConnection
) {

    val text =
        buildString {

            append(
                "BUS"
            )

            if (
                connection.busbarRatedCurrentA >
                    0.0
            ) {

                append(
                    "  "
                )

                append(
                    fmt(
                        connection.busbarRatedCurrentA
                    )
                )

                append(
                    " A"
                )
            }

            if (
                connection.busbarShortCircuitKA >
                    0.0
            ) {

                append(
                    "  "
                )

                append(
                    fmt(
                        connection.busbarShortCircuitKA
                    )
                )

                append(
                    " kA"
                )
            }
        }

    drawEngineeringLabel(
        textMeasurer =
            textMeasurer,

        text =
            text,

        point =
            point,

        color =
            BusbarColor
    )
}


/*
 * ================================================================
 * CABLE LABEL
 * ================================================================
 */

private fun DrawScope.drawCableLabel(
    textMeasurer: TextMeasurer,
    point: Offset,
    connection: SldConnection,
    feederResult:
        SldUpstreamEngineering.FeederResult?
) {

    val cableText =
        buildString {

            append(
                "CABLE"
            )

            if (
                connection.cableSizeMm2 >
                    0.0
            ) {

                append(
                    "  "
                )

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

                append(
                    "  "
                )

                append(
                    fmt(
                        connection.lengthMeters
                    )
                )

                append(
                    " m"
                )
            }
        }

    val cableColor =
        if (
            feederResult != null &&
            !feederResult.cableAdequate
        ) {

            FaultColor

        } else {

            TextSecondary
        }

    drawEngineeringLabel(
        textMeasurer =
            textMeasurer,

        text =
            cableText,

        point =
            point,

        color =
            cableColor
    )

    if (
        feederResult != null
    ) {

        val resultText =
            buildString {

                append(
                    "Ib="
                )

                append(
                    fmt(
                        feederResult.currentA
                    )
                )

                append(
                    " A"
                )

                append(
                    "   S="
                )

                append(
                    fmt(
                        feederResult.kva
                    )
                )

                append(
                    " kVA"
                )

                append(
                    "   ΔV="
                )

                append(
                    fmt(
                        feederResult.voltageDropPercent
                    )
                )

                append(
                    "%"
                )
            }

        val resultColor =
            when {

                !feederResult.cableAdequate ->
                    FaultColor

                feederResult.voltageDropPercent >
                    3.0 ->
                    WarningColor

                else ->
                    EngineeringColor
            }

        drawEngineeringLabel(
            textMeasurer =
                textMeasurer,

            text =
                resultText,

            point =
                point.copy(
                    y =
                        point.y +
                            17f
                ),

            color =
                resultColor,

            fontSize =
                8f
        )
    }
}


/*
 * ================================================================
 * ENGINEERING LABEL
 * ================================================================
 */

private fun DrawScope.drawEngineeringLabel(
    textMeasurer: TextMeasurer,
    text: String,
    point: Offset,
    color: Color,
    fontSize: Float = 8.5f
) {

    val style =
        TextStyle(
            color =
                color,

            fontSize =
                fontSize.sp,

            fontWeight =
                FontWeight.Bold
        )

    val result =
        textMeasurer.measure(
            text,
            style
        )

    val left =
        point.x -
            result.size.width / 2f -
            5f

    val top =
        point.y -
            3f

    /*
     * Small white label backing only.
     *
     * This is NOT an equipment card.
     */
    drawRoundRect(
        color =
            Color.White.copy(
                alpha = 0.92f
            ),

        topLeft =
            Offset(
                left,
                top
            ),

        size =
            Size(
                result.size.width +
                    10f,

                result.size.height +
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
                    result.size.width / 2f,

                point.y
            ),

        style =
            style
    )
}


/*
 * ================================================================
 * NODE
 * ================================================================
 */

fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: TextMeasurer,
    engineeringResult:
        SldUpstreamEngineering.NodeResult? = null
) {

    val centerX =
        node.x +
            NODE_WIDTH / 2f

    val symbolY =
        node.y +
            SYMBOL_OFFSET_Y

    /*
     * Selection is a ring around the actual electrical symbol.
     * No rectangular card is drawn.
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
                38f,

            center =
                Offset(
                    centerX,
                    symbolY
                ),

            style =
                Stroke(
                    width = 3f
                )
        )
    }

    /*
     * Electrical symbol.
     */
    when (
        node.type
    ) {

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
                symbolY
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

    /*
     * Equipment type.
     */
    drawCenteredText(
        textMeasurer,
        equipmentTypeLabel(
            node.type
        ),
        centerX,
        node.y + 51f,

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
     * Equipment name.
     */
    drawCenteredText(
        textMeasurer,
        node.name.take(24),
        centerX,
        node.y + 65f,

        TextStyle(
            color =
                TextPrimary,

            fontSize =
                10.5.sp,

            fontWeight =
                FontWeight.Bold
        )
    )

    /*
     * Electrical data.
     */
    val electrical =
        buildString {

            append(
                fmt(
                    node.voltage
                )
            )

            append(
                " V"
            )

            if (
                node.loadKw >
                    0.0
            ) {

                append(
                    "   "
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
                    "   "
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
        textMeasurer,
        electrical,
        centerX,
        node.y + 82f,

        TextStyle(
            color =
                TextSecondary,

            fontSize =
                7.5.sp
        )
    )

    /*
     * Engineering result.
     */
    if (
        engineeringResult != null
    ) {

        val engineering =
            buildString {

                append(
                    "Ib "
                )

                append(
                    fmt(
                        engineeringResult.currentA
                    )
                )

                append(
                    " A"
                )

                append(
                    "   ΔV "
                )

                append(
                    fmt(
                        engineeringResult.voltageDropPercent
                    )
                )

                append(
                    "%"
                )
            }

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
            textMeasurer,
            engineering,
            centerX,
            node.y + 98f,

            TextStyle(
                color =
                    color,

                fontSize =
                    7.5.sp,

                fontWeight =
                    FontWeight.Bold
            )
        )
    }
}


/*
 * ================================================================
 * TEXT
 * ================================================================
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
            text,
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
                    result.size.width / 2f,

                topY
            ),

        style =
            style
    )
}


/*
 * ================================================================
 * EQUIPMENT LABELS
 * ================================================================
 */

private fun equipmentTypeLabel(
    type: SldNodeType
): String =
    when (
        type
    ) {

        SldNodeType.SOURCE ->
            "SOURCE"

        SldNodeType.TRANSFORMER ->
            "TRANSFORMER"

        SldNodeType.GENERATOR ->
            "GENERATOR"

        SldNodeType.BUS ->
            "BUSBAR"

        SldNodeType.PANEL ->
            "PANEL"

        SldNodeType.BREAKER ->
            "CIRCUIT BREAKER"

        SldNodeType.LOAD ->
            "LOAD"
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
            NodeBorder,

        radius =
            23f,

        center =
            Offset(
                x,
                y
            ),

        style =
            Stroke(
                width = 3f
            )
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x - 12f,
                y
            ),

        end =
            Offset(
                x + 12f,
                y
            ),

        strokeWidth =
            3f
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x,
                y - 12f
            ),

        end =
            Offset(
                x,
                y + 12f
            ),

        strokeWidth =
            3f
    )
}


/*
 * ================================================================
 * TRANSFORMER SYMBOL
 * ================================================================
 */

private fun DrawScope.drawTransformerSymbol(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            NodeBorder,

        radius =
            17f,

        center =
            Offset(
                x - 11f,
                y
            ),

        style =
            Stroke(
                width = 3f
            )
    )

    drawCircle(
        color =
            NodeBorder,

        radius =
            17f,

        center =
            Offset(
                x + 11f,
                y
            ),

        style =
            Stroke(
                width = 3f
            )
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x - 35f,
                y
            ),

        end =
            Offset(
                x - 28f,
                y
            ),

        strokeWidth =
            3f
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x + 28f,
                y
            ),

        end =
            Offset(
                x + 35f,
                y
            ),

        strokeWidth =
            3f
    )
}


/*
 * ================================================================
 * GENERATOR SYMBOL
 * ================================================================
 */

private fun DrawScope.drawGeneratorSymbol(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            NodeBorder,

        radius =
            23f,

        center =
            Offset(
                x,
                y
            ),

        style =
            Stroke(
                width = 3f
            )
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x - 13f,
                y + 9f
            ),

        end =
            Offset(
                x - 5f,
                y - 9f
            ),

        strokeWidth =
            3f
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x - 5f,
                y - 9f
            ),

        end =
            Offset(
                x + 5f,
                y + 9f
            ),

        strokeWidth =
            3f
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x + 5f,
                y + 9f
            ),

        end =
            Offset(
                x + 13f,
                y - 9f
            ),

        strokeWidth =
            3f
    )
}


/*
 * ================================================================
 * BUSBAR SYMBOL
 * ================================================================
 */

private fun DrawScope.drawBusbarSymbol(
    x: Float,
    y: Float
) {

    drawLine(
        color =
            BusbarColor,

        start =
            Offset(
                x - 58f,
                y
            ),

        end =
            Offset(
                x + 58f,
                y
            ),

        strokeWidth =
            BUSBAR_WIDTH,

        cap =
            StrokeCap.Square
    )

    drawLine(
        color =
            BusbarColor,

        start =
            Offset(
                x - 58f,
                y - 10f
            ),

        end =
            Offset(
                x - 58f,
                y + 10f
            ),

        strokeWidth =
            2.5f
    )

    drawLine(
        color =
            BusbarColor,

        start =
            Offset(
                x + 58f,
                y - 10f
            ),

        end =
            Offset(
                x + 58f,
                y + 10f
            ),

        strokeWidth =
            2.5f
    )
}


/*
 * ================================================================
 * PANEL SYMBOL
 * ================================================================
 */

private fun DrawScope.drawPanelSymbol(
    x: Float,
    y: Float
) {

    /*
     * Main vertical panel bus.
     */
    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x,
                y - 29f
            ),

        end =
            Offset(
                x,
                y + 29f
            ),

        strokeWidth =
            7f
    )

    /*
     * Outgoing feeder positions.
     */
    listOf(
        -18f,
        0f,
        18f
    ).forEach { offset ->

        drawLine(
            color =
                NodeBorder,

            start =
                Offset(
                    x - 18f,
                    y + offset
                ),

            end =
                Offset(
                    x + 18f,
                    y + offset
                ),

            strokeWidth =
                2.5f
        )
    }
}


/*
 * ================================================================
 * BREAKER SYMBOL
 * ================================================================
 */

private fun DrawScope.drawBreakerSymbol(
    x: Float,
    y: Float
) {

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x - 32f,
                y
            ),

        end =
            Offset(
                x - 10f,
                y
            ),

        strokeWidth =
            3f
    )

    drawCircle(
        color =
            NodeBorder,

        radius =
            3.5f,

        center =
            Offset(
                x - 10f,
                y
            )
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x - 10f,
                y
            ),

        end =
            Offset(
                x + 18f,
                y - 17f
            ),

        strokeWidth =
            3f
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x + 18f,
                y - 17f
            ),

        end =
            Offset(
                x + 32f,
                y - 17f
            ),

        strokeWidth =
            3f
    )

    drawCircle(
        color =
            NodeBorder,

        radius =
            3.5f,

        center =
            Offset(
                x + 32f,
                y - 17f
            )
    )
}


/*
 * ================================================================
 * LOAD SYMBOL
 * ================================================================
 */

private fun DrawScope.drawLoadSymbol(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            NodeBorder,

        radius =
            22f,

        center =
            Offset(
                x,
                y
            ),

        style =
            Stroke(
                width = 3f
            )
    )

    drawLine(
        color =
            NodeBorder,

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
            3f
    )
}


/*
 * ================================================================
 * HIT TEST — NODE
 * ================================================================
 */

fun findNode(
    point: Offset,
    nodes: List<SldNode>
): SldNode? {

    return nodes.lastOrNull { node ->

        val centerX =
            node.x +
                NODE_WIDTH / 2f

        val centerY =
            node.y +
                SYMBOL_OFFSET_Y

        val dx =
            point.x -
                centerX

        val dy =
            point.y -
                centerY

        (
            dx * dx +
                dy * dy
            ) <=
            42f * 42f
    }
}


/*
 * ================================================================
 * HIT TEST — CONNECTION
 * ================================================================
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

    connections.forEach { connection ->

        val from =
            nodes.firstOrNull {
                it.id ==
                    connection.fromNodeId
            }
                ?: return@forEach

        val to =
            nodes.firstOrNull {
                it.id ==
                    connection.toNodeId
            }
                ?: return@forEach

        val fromCenter =
            Offset(
                from.x +
                    NODE_WIDTH / 2f,

                from.y +
                    NODE_HEIGHT / 2f
            )

        val toCenter =
            Offset(
                to.x +
                    NODE_WIDTH / 2f,

                to.y +
                    NODE_HEIGHT / 2f
            )

        val dx =
            toCenter.x -
                fromCenter.x

        val dy =
            toCenter.y -
                fromCenter.y

        val points =
            if (
                abs(dx) < 35f
            ) {

                val down =
                    dy >= 0f

                listOf(
                    if (down) {

                        Offset(
                            fromCenter.x,
                            from.y +
                                NODE_HEIGHT
                        )

                    } else {

                        Offset(
                            fromCenter.x,
                            from.y
                        )
                    },

                    if (down) {

                        Offset(
                            toCenter.x,
                            to.y
                        )

                    } else {

                        Offset(
                            toCenter.x,
                            to.y +
                                NODE_HEIGHT
                        )
                    }
                )

            } else {

                val start =
                    Offset(
                        fromCenter.x,
                        from.y +
                            NODE_HEIGHT
                    )

                val end =
                    Offset(
                        toCenter.x,
                        to.y
                    )

                val middleY =
                    (
                        start.y +
                            end.y
                        ) / 2f

                listOf(
                    start,

                    Offset(
                        start.x,
                        middleY
                    ),

                    Offset(
                        end.x,
                        middleY
                    ),

                    end
                )
            }

        for (
            i in 0 until
                points.lastIndex
        ) {

            val distance =
                segmentDistance(
                    point,
                    points[i],
                    points[i + 1]
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
            30f
    ) {

        best

    } else {

        null
    }
}


/*
 * ================================================================
 * GEOMETRY
 * ================================================================
 */

private fun segmentDistance(
    point: Offset,
    start: Offset,
    end: Offset
): Float {

    val dx =
        end.x -
            start.x

    val dy =
        end.y -
            start.y

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
            (point.x - start.x) *
                dx +

                (point.y - start.y) *
                dy
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
