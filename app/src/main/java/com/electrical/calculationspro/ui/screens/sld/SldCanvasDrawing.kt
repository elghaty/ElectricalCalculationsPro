package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.CornerRadius
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

const val NODE_WIDTH = 180f
const val NODE_HEIGHT = 118f

private val Background =
    Color(0xFFF7F9FA)

private val GridColor =
    Color(0xFFE2E7EA)

private val NodeFill =
    Color.White

private val NodeBorder =
    Color(0xFF263238)

private val TextPrimary =
    Color(0xFF172027)

private val TextSecondary =
    Color(0xFF60717A)

private val CableColor =
    Color(0xFF455A64)

private val BusbarColor =
    Color(0xFF00838F)

private val SelectedColor =
    Color(0xFF00838F)

private val StartColor =
    Color(0xFF1565C0)

private val EngineeringColor =
    Color(0xFF0277BD)

private val WarningColor =
    Color(0xFFEF6C00)

private val FaultColor =
    Color(0xFFC62828)


// ================================================================
// BACKGROUND
// ================================================================

fun DrawScope.drawSldEngineeringBackground() {

    drawRect(
        color = Background
    )

    var x = 0f

    while (x <= size.width) {

        drawLine(
            color = GridColor,
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
            strokeWidth = 1f
        )

        x += 40f
    }

    var y = 0f

    while (y <= size.height) {

        drawLine(
            color = GridColor,
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
            strokeWidth = 1f
        )

        y += 40f
    }
}


// ================================================================
// CONNECTION
// ================================================================

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

    // ------------------------------------------------------------
    // VERTICAL CONNECTION
    // ------------------------------------------------------------

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
                end.x + 12f,
                (start.y + end.y) / 2f
            )

    } else {

        // --------------------------------------------------------
        // ORTHOGONAL CONNECTION
        // --------------------------------------------------------

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

    val busbar =
        connection.connectionType ==
            SldConnectionType.BUSBAR

    val color =
        when {

            selected ->
                SelectedColor

            busbar ->
                BusbarColor

            feederResult != null &&
                !feederResult.cableAdequate ->
                FaultColor

            else ->
                CableColor
        }

    drawPath(
        path =
            path,

        color =
            color,

        style =
            Stroke(
                width =
                    when {
                        selected ->
                            7f

                        busbar ->
                            8f

                        else ->
                            4f
                    }
            )
    )

    if (busbar) {

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


// ================================================================
// BUSBAR LABEL
// ================================================================

private fun DrawScope.drawBusbarLabel(
    textMeasurer: TextMeasurer,
    point: Offset,
    connection: SldConnection
) {

    val text =
        buildString {

            append(
                "BUSBAR"
            )

            if (
                connection.busbarRatedCurrentA >
                    0.0
            ) {

                append(
                    "  ${
                        fmt(
                            connection.busbarRatedCurrentA
                        )
                    } A"
                )
            }

            if (
                connection.busbarShortCircuitKA >
                    0.0
            ) {

                append(
                    "  ${
                        fmt(
                            connection.busbarShortCircuitKA
                        )
                    } kA"
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


// ================================================================
// CABLE LABEL
// ================================================================

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
                    "  ${
                        fmt(
                            connection.cableSizeMm2
                        )
                    } mm²"
                )

                if (
                    connection.parallelRuns >
                        1
                ) {

                    append(
                        " × ${
                            connection.parallelRuns
                        }"
                    )
                }
            }

            if (
                connection.lengthMeters >
                    0.0
            ) {

                append(
                    "  ${
                        fmt(
                            connection.lengthMeters
                        )
                    } m"
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
            "Ib=${
                fmt(
                    feederResult.currentA
                )
            } A   " +
                "S=${
                    fmt(
                        feederResult.kva
                    )
                } kVA   " +
                "ΔV=${
                    fmt(
                        feederResult.voltageDropPercent
                    )
                }%"

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


// ================================================================
// ENGINEERING LABEL
// ================================================================

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

    drawRoundRect(
        color =
            Color.White.copy(
                alpha = 0.94f
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
                4f,
                4f
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


// ================================================================
// NODE
// ================================================================

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
            32f

    if (
        selected ||
        connectionStart
    ) {

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
                CornerRadius(
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
        color =
            NodeFill,

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
            CornerRadius(
                9f,
                9f
            )
    )

    drawRoundRect(
        color =
            NodeBorder,

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
            CornerRadius(
                9f,
                9f
            ),

        style =
            Stroke(
                width = 2f
            )
    )

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

    drawCenteredText(
        textMeasurer,
        equipmentTypeLabel(
            node.type
        ),
        centerX,
        node.y + 49f,

        TextStyle(
            color =
                TextSecondary,

            fontSize =
                7.5.sp,

            fontWeight =
                FontWeight.Bold
        )
    )

    drawCenteredText(
        textMeasurer,
        node.name.take(22),
        centerX,
        node.y + 64f,

        TextStyle(
            color =
                TextPrimary,

            fontSize =
                11.sp,

            fontWeight =
                FontWeight.Bold
        )
    )

    val electrical =
        buildString {

            append(
                "${fmt(node.voltage)} V"
            )

            if (
                node.loadKw >
                    0.0
            ) {

                append(
                    "   ${
                        fmt(
                            node.loadKw
                        )
                    } kW"
                )
            }

            if (
                node.ratedKva >
                    0.0
            ) {

                append(
                    "   ${
                        fmt(
                            node.ratedKva
                        )
                    } kVA"
                )
            }
        }

    drawCenteredText(
        textMeasurer,
        electrical,
        centerX,
        node.y + 81f,

        TextStyle(
            color =
                TextSecondary,

            fontSize =
                8.sp
        )
    )

    if (
        engineeringResult != null
    ) {

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
            textMeasurer,
            engineering,
            centerX,
            node.y + 96f,

            TextStyle(
                color =
                    color,

                fontSize =
                    8.sp,

                fontWeight =
                    FontWeight.Bold
            )
        )
    }
}


// ================================================================
// TEXT
// ================================================================

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


// ================================================================
// SYMBOLS
// ================================================================

private fun DrawScope.drawSourceSymbol(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            NodeBorder,

        radius =
            19f,

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
                x - 10f,
                y - 10f
            ),

        end =
            Offset(
                x + 10f,
                y + 10f
            ),

        strokeWidth =
            2.5f
    )

    drawLine(
        color =
            NodeBorder,

        start =
            Offset(
                x - 10f,
                y + 10f
            ),

        end =
            Offset(
                x + 10f,
                y - 10f
            ),

        strokeWidth =
            2.5f
    )
}


private fun DrawScope.drawTransformerSymbol(
    x: Float,
    y: Float
) {

    drawArc(
        color =
            NodeBorder,

        startAngle =
            -90f,

        sweepAngle =
            180f,

        useCenter =
            false,

        topLeft =
            Offset(
                x - 25f,
                y - 25f
            ),

        size =
            Size(
                50f,
                50f
            ),

        style =
            Stroke(
                width = 3f
            )
    )

    drawArc(
        color =
            NodeBorder,

        startAngle =
            90f,

        sweepAngle =
            180f,

        useCenter =
            false,

        topLeft =
            Offset(
                x,
                y - 25f
            ),

        size =
            Size(
                50f,
                50f
            ),

        style =
            Stroke(
                width = 3f
            )
    )
}


private fun DrawScope.drawGeneratorSymbol(
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

    drawArc(
        color =
            NodeBorder,

        startAngle =
            25f,

        sweepAngle =
            130f,

        useCenter =
            false,

        topLeft =
            Offset(
                x - 13f,
                y - 13f
            ),

        size =
            Size(
                26f,
                26f
            ),

        style =
            Stroke(
                width = 2.5f
            )
    )
}


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
            8f
    )
}


private fun DrawScope.drawPanelSymbol(
    x: Float,
    y: Float
) {

    drawRect(
        color =
            Color.White,

        topLeft =
            Offset(
                x - 34f,
                y - 24f
            ),

        size =
            Size(
                68f,
                48f
            )
    )

    drawRect(
        color =
            NodeBorder,

        topLeft =
            Offset(
                x - 34f,
                y - 24f
            ),

        size =
            Size(
                68f,
                48f
            ),

        style =
            Stroke(
                width = 3f
            )
    )

    listOf(
        -10f,
        0f,
        10f
    ).forEach { offset ->

        drawLine(
            color =
                NodeBorder,

            start =
                Offset(
                    x - 22f,
                    y + offset
                ),

            end =
                Offset(
                    x + 22f,
                    y + offset
                ),

            strokeWidth =
                2f
        )
    }
}


private fun DrawScope.drawBreakerSymbol(
    x: Float,
    y: Float
) {

    drawRect(
        color =
            Color.White,

        topLeft =
            Offset(
                x - 22f,
                y - 18f
            ),

        size =
            Size(
                44f,
                36f
            )
    )

    drawRect(
        color =
            NodeBorder,

        topLeft =
            Offset(
                x - 22f,
                y - 18f
            ),

        size =
            Size(
                44f,
                36f
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
                y + 9f
            ),

        end =
            Offset(
                x + 12f,
                y - 9f
            ),

        strokeWidth =
            3f
    )
}


private fun DrawScope.drawLoadSymbol(
    x: Float,
    y: Float
) {

    drawCircle(
        color =
            NodeBorder,

        radius =
            21f,

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


// ================================================================
// LABELS / HIT TEST
// ================================================================

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
            "LOAD / MOTOR"
    }


fun findNode(
    point: Offset,
    nodes: List<SldNode>
): SldNode? =

    nodes.lastOrNull { node ->

        point.x >= node.x &&
            point.x <=
                node.x +
                    NODE_WIDTH &&

            point.y >= node.y &&
            point.y <=
                node.y +
                    NODE_HEIGHT
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

        val points =
            mutableListOf<Offset>()

        if (
            abs(
                fromCenter.x -
                    toCenter.x
            ) < 35f
        ) {

            points +=
                if (
                    toCenter.y >
                    fromCenter.y
                ) {

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

            points +=
                if (
                    toCenter.y >
                    fromCenter.y
                ) {

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

            points +=
                start

            points +=
                Offset(
                    start.x,
                    middleY
                )

            points +=
                Offset(
                    end.x,
                    middleY
                )

            points +=
                end
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
        bestDistance < 30f
    ) {
        best
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
