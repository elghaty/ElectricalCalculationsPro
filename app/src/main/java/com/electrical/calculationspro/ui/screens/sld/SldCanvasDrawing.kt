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
import com.electrical.calculationspro.data.SldUpstreamEngineering
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

const val NODE_WIDTH = 190f
const val NODE_HEIGHT = 126f

private val PrimaryColor = Color(0xFF172027)
private val SecondaryColor = Color(0xFF60717A)
private val EngineeringColor = Color(0xFF1976D2)
private val WarningColor = Color(0xFFE69500)
private val FaultColor = Color(0xFFD32F2F)
private val SuccessColor = Color(0xFF2E7D32)
private val SymbolColor = Color(0xFF263238)
private val ConnectionColor = Color(0xFF607D8B)
private val SelectedColor = Color(0xFF00A86B)
private val ConnectionSelectedColor = Color(0xFF00838F)
private val SymbolBackground = Color(0xFFFFFFFF)

/**
 * Draw one SLD connection.
 *
 * Engineering values are supplied by SldUpstreamEngineering.
 * No engineering calculation is performed here.
 */
fun DrawScope.drawConnection(
    connection: SldConnection,
    nodes: List<SldNode>,
    selected: Boolean,
    textMeasurer: TextMeasurer,
    feederResult: SldUpstreamEngineering.FeederResult? = null
) {
    val from = nodes.firstOrNull {
        it.id == connection.fromNodeId
    } ?: return

    val to = nodes.firstOrNull {
        it.id == connection.toNodeId
    } ?: return

    val start = Offset(
        from.x + NODE_WIDTH,
        from.y + NODE_HEIGHT / 2f
    )

    val end = Offset(
        to.x,
        to.y + NODE_HEIGHT / 2f
    )

    val middleX = (start.x + end.x) / 2f

    val path = Path().apply {
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

    val connectionColor = when {
        selected -> ConnectionSelectedColor

        feederResult != null &&
            !feederResult.cableAdequate ->
            FaultColor

        else ->
            ConnectionColor
    }

    drawPath(
        path = path,
        color = connectionColor,
        style = Stroke(
            width = when {
                selected -> 6f

                feederResult != null &&
                    !feederResult.cableAdequate ->
                    5f

                else ->
                    4f
            }
        )
    )

    /*
     * ============================================================
     * FEEDER ID
     * ============================================================
     */

    drawText(
        textMeasurer = textMeasurer,
        text = connection.id,
        topLeft = Offset(
            middleX - 85f,
            min(
                start.y,
                end.y
            ) - 50f
        ),
        style = TextStyle(
            color = PrimaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    )

    /*
     * ============================================================
     * CABLE INFORMATION
     * ============================================================
     */

    val cableLabel = buildString {

        if (connection.cableSizeMm2 > 0.0) {

            append(
                fmt(
                    connection.cableSizeMm2
                )
            )

            append(" mm²")

            if (connection.parallelRuns > 1) {

                append(" × ")

                append(
                    connection.parallelRuns
                )
            }
        }

        if (connection.lengthMeters > 0.0) {

            if (isNotEmpty()) {
                append(" | ")
            }

            append("L=")

            append(
                fmt(
                    connection.lengthMeters
                )
            )

            append(" m")
        }

        if (connection.currentCapacityA > 0.0) {

            if (isNotEmpty()) {
                append(" | ")
            }

            append("Iz=")

            append(
                fmt(
                    connection.currentCapacityA
                )
            )

            append(" A")
        }
    }

    if (cableLabel.isNotBlank()) {

        drawText(
            textMeasurer = textMeasurer,
            text = cableLabel,
            topLeft = Offset(
                middleX - 85f,
                min(
                    start.y,
                    end.y
                ) - 33f
            ),
            style = TextStyle(
                color =
                    if (
                        feederResult != null &&
                        !feederResult.cableAdequate
                    ) {
                        FaultColor
                    } else {
                        SecondaryColor
                    },
                fontSize = 9.sp,
                fontWeight =
                    if (
                        feederResult != null &&
                        !feederResult.cableAdequate
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
            )
        )
    }

    /*
     * ============================================================
     * ENGINEERING FEEDER DATA
     * ============================================================
     */

    if (feederResult != null) {

        val engineeringLabel =
            "Ib=${fmt(feederResult.currentA)} A  " +
                "S=${fmt(feederResult.kva)} kVA  " +
                "ΔV=${fmt(feederResult.voltageDropPercent)}%"

        drawText(
            textMeasurer = textMeasurer,
            text = engineeringLabel,
            topLeft = Offset(
                middleX - 85f,
                min(
                    start.y,
                    end.y
                ) - 16f
            ),
            style = TextStyle(
                color =
                    if (
                        feederResult.voltageDropPercent > 3.0
                    ) {
                        WarningColor
                    } else {
                        EngineeringColor
                    },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )

        val adequacyText =
            if (feederResult.cableAdequate) {
                "CABLE OK"
            } else {
                "CABLE UNDERSIZED"
            }

        drawText(
            textMeasurer = textMeasurer,
            text = adequacyText,
            topLeft = Offset(
                middleX - 85f,
                max(
                    start.y,
                    end.y
                ) + 14f
            ),
            style = TextStyle(
                color =
                    if (
                        feederResult.cableAdequate
                    ) {
                        SuccessColor
                    } else {
                        FaultColor
                    },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )

        if (
            feederResult.recommendedBreakerA > 0.0
        ) {

            drawText(
                textMeasurer = textMeasurer,
                text =
                    "Breaker≈${fmt(feederResult.recommendedBreakerA)} A",
                topLeft = Offset(
                    middleX - 85f,
                    max(
                        start.y,
                        end.y
                    ) + 30f
                ),
                style = TextStyle(
                    color = SecondaryColor,
                    fontSize = 9.sp
                )
            )
        }
    }
}

/**
 * Draw one SLD equipment node.
 *
 * Engineering calculation remains outside the drawing layer.
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
        node.x + NODE_WIDTH / 2f

    val centerY =
        node.y + 50f

    /*
     * ============================================================
     * SELECTION
     * ============================================================
     */

    if (selected || connectionStart) {

        drawRect(
            color =
                if (connectionStart) {
                    ConnectionSelectedColor
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
            style = Stroke(
                width = 4f
            )
        )
    }

    /*
     * ============================================================
     * SYMBOL
     * ============================================================
     */

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

    /*
     * ============================================================
     * NODE NAME
     * ============================================================
     */

    drawText(
        textMeasurer = textMeasurer,
        text = node.name,
        topLeft = Offset(
            node.x,
            node.y + 84f
        ),
        style = TextStyle(
            color = PrimaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    )

    /*
     * ============================================================
     * BASIC DATA
     * ============================================================
     */

    val basicText = buildString {

        append("V=")

        append(
            fmt(
                node.voltage
            )
        )

        append(" V")

        if (node.loadKw > 0.0) {

            append("   P=")

            append(
                fmt(
                    node.loadKw
                )
            )

            append(" kW")
        }

        if (node.ratedKva > 0.0) {

            append("   S=")

            append(
                fmt(
                    node.ratedKva
                )
            )

            append(" kVA")
        }
    }

    drawText(
        textMeasurer = textMeasurer,
        text = basicText,
        topLeft = Offset(
            node.x,
            node.y + 103f
        ),
        style = TextStyle(
            color = SecondaryColor,
            fontSize = 9.sp
        )
    )

    /*
     * ============================================================
     * ENGINEERING DATA
     * ============================================================
     */

    if (engineeringResult != null) {

        val engineeringText =
            "Ib=${fmt(engineeringResult.currentA)} A  " +
                "S=${fmt(engineeringResult.kva)} kVA"

        drawText(
            textMeasurer = textMeasurer,
            text = engineeringText,
            topLeft = Offset(
                node.x,
                node.y + NODE_HEIGHT + 10f
            ),
            style = TextStyle(
                color =
                    when {

                        engineeringResult.loadingPercent >
                            100.0 ->
                            FaultColor

                        engineeringResult.voltageDropPercent >
                            3.0 ->
                            WarningColor

                        else ->
                            EngineeringColor
                    },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )

        drawText(
            textMeasurer = textMeasurer,
            text =
                "Pdem=${fmt(engineeringResult.demandKw)} kW",
            topLeft = Offset(
                node.x,
                node.y + NODE_HEIGHT + 26f
            ),
            style = TextStyle(
                color = SecondaryColor,
                fontSize = 9.sp
            )
        )

        if (node.ratedKva > 0.0) {

            val loadingColor =
                when {

                    engineeringResult.loadingPercent >
                        100.0 ->
                        FaultColor

                    engineeringResult.loadingPercent >
                        80.0 ->
                        WarningColor

                    else ->
                        SuccessColor
                }

            drawText(
                textMeasurer = textMeasurer,
                text =
                    "Loading=" +
                        fmt(
                            engineeringResult.loadingPercent
                        ) +
                        "%",
                topLeft = Offset(
                    node.x,
                    node.y + NODE_HEIGHT + 42f
                ),
                style = TextStyle(
                    color = loadingColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (
            engineeringResult.recommendedBreakerA > 0.0
        ) {

            drawText(
                textMeasurer = textMeasurer,
                text =
                    "Breaker≈" +
                        fmt(
                            engineeringResult.recommendedBreakerA
                        ) +
                        " A",
                topLeft = Offset(
                    node.x,
                    node.y + NODE_HEIGHT + 58f
                ),
                style = TextStyle(
                    color = SecondaryColor,
                    fontSize = 9.sp
                )
            )
        }

        drawText(
            textMeasurer = textMeasurer,
            text =
                "ΔV=" +
                    fmt(
                        engineeringResult.voltageDropPercent
                    ) +
                    "%",
            topLeft = Offset(
                node.x,
                node.y + NODE_HEIGHT + 74f
            ),
            style = TextStyle(
                color =
                    if (
                        engineeringResult.voltageDropPercent > 3.0
                    ) {
                        WarningColor
                    } else {
                        SuccessColor
                    },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )

    } else {

        drawText(
            textMeasurer = textMeasurer,
            text =
                equipmentTypeLabel(
                    node.type
                ),
            topLeft = Offset(
                node.x,
                node.y + NODE_HEIGHT + 10f
            ),
            style = TextStyle(
                color = SecondaryColor,
                fontSize = 9.sp
            )
        )
    }
}

/*
 * ================================================================
 * EQUIPMENT SYMBOLS
 * ================================================================
 */

private fun DrawScope.drawSourceSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y - 45f
        ),
        end = Offset(
            x,
            y - 20f
        ),
        strokeWidth = 4f
    )

    drawCircle(
        color = SymbolBackground,
        radius = 22f,
        center = Offset(
            x,
            y
        ),
        style = Stroke(
            width = 3.5f
        )
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x - 12f,
            y + 12f
        ),
        end = Offset(
            x + 12f,
            y - 12f
        ),
        strokeWidth = 3f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x - 12f,
            y - 12f
        ),
        end = Offset(
            x + 12f,
            y + 12f
        ),
        strokeWidth = 3f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y + 22f
        ),
        end = Offset(
            x,
            y + 45f
        ),
        strokeWidth = 4f
    )
}

private fun DrawScope.drawTransformerSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y - 50f
        ),
        end = Offset(
            x,
            y - 30f
        ),
        strokeWidth = 4f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y + 30f
        ),
        end = Offset(
            x,
            y + 50f
        ),
        strokeWidth = 4f
    )

    drawArc(
        color = SymbolColor,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(
            x - 28f,
            y - 28f
        ),
        size = Size(
            56f,
            56f
        ),
        style = Stroke(
            width = 3.5f
        )
    )

    drawArc(
        color = SymbolColor,
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(
            x + 2f,
            y - 28f
        ),
        size = Size(
            56f,
            56f
        ),
        style = Stroke(
            width = 3.5f
        )
    )
}

private fun DrawScope.drawGeneratorSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y - 48f
        ),
        end = Offset(
            x,
            y - 24f
        ),
        strokeWidth = 4f
    )

    drawCircle(
        color = SymbolBackground,
        radius = 25f,
        center = Offset(
            x,
            y
        ),
        style = Stroke(
            width = 3.5f
        )
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
        size = Size(
            30f,
            30f
        ),
        style = Stroke(
            width = 3f
        )
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y + 25f
        ),
        end = Offset(
            x,
            y + 48f
        ),
        strokeWidth = 4f
    )
}

private fun DrawScope.drawBusbarSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        color = SymbolColor,
        start = Offset(
            x - 58f,
            y
        ),
        end = Offset(
            x + 58f,
            y
        ),
        strokeWidth = 8f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y - 45f
        ),
        end = Offset(
            x,
            y
        ),
        strokeWidth = 4f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y
        ),
        end = Offset(
            x,
            y + 45f
        ),
        strokeWidth = 4f
    )

    drawCircle(
        color = SymbolColor,
        radius = 5f,
        center = Offset(
            x,
            y - 45f
        )
    )

    drawCircle(
        color = SymbolColor,
        radius = 5f,
        center = Offset(
            x,
            y + 45f
        )
    )
}

private fun DrawScope.drawPanelSymbol(
    x: Float,
    y: Float
) {
    drawRect(
        color = SymbolBackground,
        topLeft = Offset(
            x - 38f,
            y - 30f
        ),
        size = Size(
            76f,
            60f
        ),
        style = Stroke(
            width = 3.5f
        )
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y - 30f
        ),
        end = Offset(
            x,
            y + 30f
        ),
        strokeWidth = 2.5f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x - 38f,
            y - 10f
        ),
        end = Offset(
            x + 38f,
            y - 10f
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x - 38f,
            y + 10f
        ),
        end = Offset(
            x + 38f,
            y + 10f
        ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawBreakerSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y - 48f
        ),
        end = Offset(
            x,
            y - 23f
        ),
        strokeWidth = 4f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y + 23f
        ),
        end = Offset(
            x,
            y + 48f
        ),
        strokeWidth = 4f
    )

    drawRect(
        color = SymbolBackground,
        topLeft = Offset(
            x - 24f,
            y - 24f
        ),
        size = Size(
            48f,
            48f
        ),
        style = Stroke(
            width = 3.5f
        )
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x - 14f,
            y + 12f
        ),
        end = Offset(
            x + 13f,
            y - 12f
        ),
        strokeWidth = 4f
    )
}

private fun DrawScope.drawLoadSymbol(
    x: Float,
    y: Float
) {
    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y - 48f
        ),
        end = Offset(
            x,
            y - 23f
        ),
        strokeWidth = 4f
    )

    drawCircle(
        color = SymbolBackground,
        radius = 23f,
        center = Offset(
            x,
            y
        ),
        style = Stroke(
            width = 3.5f
        )
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x - 14f,
            y + 14f
        ),
        end = Offset(
            x + 14f,
            y - 14f
        ),
        strokeWidth = 3f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x - 8f,
            y + 20f
        ),
        end = Offset(
            x + 20f,
            y - 8f
        ),
        strokeWidth = 2f
    )

    drawLine(
        color = SymbolColor,
        start = Offset(
            x,
            y + 23f
        ),
        end = Offset(
            x,
            y + 48f
        ),
        strokeWidth = 4f
    )
}

/*
 * ================================================================
 * LABELS
 * ================================================================
 */

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

        point.x >= node.x &&
            point.x <=
            node.x + NODE_WIDTH &&
            point.y >= node.y &&
            point.y <=
            node.y + NODE_HEIGHT
    }
}

fun findConnection(
    point: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {

    var bestConnection: SldConnection? = null

    var bestDistance =
        Float.MAX_VALUE

    connections.forEach { connection ->

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

        val d1 =
            segmentDistance(
                point,
                start,
                p1
            )

        val d2 =
            segmentDistance(
                point,
                p1,
                p2
            )

        val d3 =
            segmentDistance(
                point,
                p2,
                end
            )

        val distance =
            min(
                d1,
                min(
                    d2,
                    d3
                )
            )

        if (
            distance <
            bestDistance
        ) {
            bestDistance =
                distance

            bestConnection =
                connection
        }
    }

    return if (
        bestDistance < 40f
    ) {
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
