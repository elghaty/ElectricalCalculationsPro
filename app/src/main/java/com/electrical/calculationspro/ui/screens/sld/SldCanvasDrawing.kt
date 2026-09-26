package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldShortCircuitResult
import com.electrical.calculationspro.data.SldUpstreamEngineering
import com.electrical.calculationspro.data.SldProtectionDevice
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal const val NODE_WIDTH = 190f
internal const val NODE_HEIGHT = 126f

private val PrimaryColor = Color(0xFF163A5F)
private val SecondaryColor = Color(0xFF496A83)
private val EngineeringColor = Color(0xFF245F3F)
private val WarningColor = Color(0xFF996C00)
private val FaultColor = Color(0xFF9B2C2C)
private val SuccessColor = Color(0xFF24733F)
private val SymbolColor = Color(0xFF203040)
private val ConnectionColor = Color(0xFF425466)
private val SelectedColor = Color(0xFF1565C0)
private val ConnectionSelectedColor = Color(0xFF00838F)
private val SymbolBackground = Color(0xFFEAF0F5)

internal fun DrawScope.drawSldEngineeringBackground(
    width: Float,
    height: Float
) {
    val grid = 50f

    var x = 0f
    while (x <= width) {
        drawLine(
            color = Color(0xFFE3E8ED),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += grid
    }

    var y = 0f
    while (y <= height) {
        drawLine(
            color = Color(0xFFE3E8ED),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += grid
    }
}

internal fun DrawScope.drawConnection(
    connection: SldConnection,
    nodes: List<SldNode>,
    selected: Boolean,
    textMeasurer: TextMeasurer,
    feederResult: SldUpstreamEngineering.FeederResult?
) {
    val from = nodes.firstOrNull {
        it.id == connection.fromNodeId
    }

    val to = nodes.firstOrNull {
        it.id == connection.toNodeId
    }

    if (from == null || to == null) {
        return
    }

    val start = Offset(
        from.x + NODE_WIDTH,
        from.y + NODE_HEIGHT / 2f
    )

    val end = Offset(
        to.x,
        to.y + NODE_HEIGHT / 2f
    )

    val middleX = (start.x + end.x) / 2f

    val p1 = Offset(
        middleX,
        start.y
    )

    val p2 = Offset(
        middleX,
        end.y
    )

    val undersized =
        feederResult?.isAdequate == false

    val lineColor = when {
        selected -> ConnectionSelectedColor
        undersized -> FaultColor
        else -> ConnectionColor
    }

    val lineWidth = when {
        selected -> 5f
        undersized -> 4f
        else -> 3f
    }

    val path = Path().apply {
        moveTo(start.x, start.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(end.x, end.y)
    }

    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = lineWidth)
    )

    drawCircle(
        color = lineColor,
        radius = 5f,
        center = start
    )

    drawCircle(
        color = lineColor,
        radius = 5f,
        center = end
    )

    val labelX = middleX + 10f
    val labelY = min(
        start.y,
        end.y
    ) - 48f

    drawText(
        textMeasurer = textMeasurer,
        text = connection.id,
        topLeft = Offset(
            labelX,
            labelY
        ),
        style = TextStyle(
            color = PrimaryColor,
            fontSize = 13.sp
        )
    )

    val cableLabel = buildString {

        if (connection.cableSizeMm2 > 0.0) {
            append(
                "Cable ${fmt(connection.cableSizeMm2)} mm²"
            )
        }

        if (connection.parallelRuns > 1) {
            append(
                " × ${connection.parallelRuns}"
            )
        }

        if (connection.lengthMeters > 0.0) {
            append(
                " | L=${fmt(connection.lengthMeters)} m"
            )
        }

        if (connection.currentCapacityA > 0.0) {
            append(
                " | Iz=${fmt(connection.currentCapacityA)} A"
            )
        }
    }

    if (cableLabel.isNotBlank()) {
        drawText(
            textMeasurer = textMeasurer,
            text = cableLabel,
            topLeft = Offset(
                labelX,
                labelY + 18f
            ),
            style = TextStyle(
                color = SecondaryColor,
                fontSize = 11.sp
            )
        )
    }

    if (feederResult != null) {

        drawText(
            textMeasurer = textMeasurer,
            text =
                "Ib=${fmt(feederResult.designCurrentA)} A" +
                    " | S=${fmt(feederResult.apparentPowerKva)} kVA",
            topLeft = Offset(
                labelX,
                labelY + 35f
            ),
            style = TextStyle(
                color = EngineeringColor,
                fontSize = 11.sp
            )
        )

        drawText(
            textMeasurer = textMeasurer,
            text =
                "ΔV=${fmt(feederResult.voltageDropPercent)} %",
            topLeft = Offset(
                labelX,
                labelY + 50f
            ),
            style = TextStyle(
                color = EngineeringColor,
                fontSize = 11.sp
            )
        )

        drawText(
            textMeasurer = textMeasurer,
            text = if (feederResult.isAdequate) {
                "CABLE OK"
            } else {
                "CABLE UNDERSIZED"
            },
            topLeft = Offset(
                labelX,
                labelY + 65f
            ),
            style = TextStyle(
                color = if (feederResult.isAdequate) {
                    SuccessColor
                } else {
                    FaultColor
                },
                fontSize = 11.sp
            )
        )

        if (feederResult.recommendedBreakerA > 0.0) {

            drawText(
                textMeasurer = textMeasurer,
                text =
                    "Breaker ≈ " +
                        "${fmt(feederResult.recommendedBreakerA)} A",
                topLeft = Offset(
                    labelX,
                    labelY + 80f
                ),
                style = TextStyle(
                    color = PrimaryColor,
                    fontSize = 11.sp
                )
            )
        }
    }
}

internal fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: TextMeasurer,
    engineeringResult: SldUpstreamEngineering.NodeResult?,
    protectionDevice: SldProtectionDevice? = null,
    shortCircuitResult: SldShortCircuitResult? = null
) {
    val left = node.x
    val top = node.y

    val borderColor = when {
        selected -> SelectedColor
        protectionDevice?.status?.name == "FAIL" -> FaultColor
        protectionDevice?.status?.name == "WARNING" -> WarningColor
        else -> PrimaryColor
    }

    drawRoundRect(
        color = Color.White,
        topLeft = Offset(
            left,
            top
        ),
        size = androidx.compose.ui.geometry.Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            10f,
            10f
        )
    )

    drawRoundRect(
        color = borderColor,
        topLeft = Offset(
            left,
            top
        ),
        size = androidx.compose.ui.geometry.Size(
            NODE_WIDTH,
            NODE_HEIGHT
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            10f,
            10f
        ),
        style = Stroke(
            width = if (selected) 5f else 2.5f
        )
    )

    if (connectionStart) {
        drawRoundRect(
            color = ConnectionSelectedColor,
            topLeft = Offset(
                left - 5f,
                top - 5f
            ),
            size = androidx.compose.ui.geometry.Size(
                NODE_WIDTH + 10f,
                NODE_HEIGHT + 10f
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                13f,
                13f
            ),
            style = Stroke(
                width = 3f
            )
        )
    }

    drawNodeSymbol(
        node = node,
        center = Offset(
            left + 34f,
            top + 36f
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = node.name,
        topLeft = Offset(
            left + 60f,
            top + 13f
        ),
        style = TextStyle(
            color = PrimaryColor,
            fontSize = 14.sp
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = node.type.name,
        topLeft = Offset(
            left + 60f,
            top + 33f
        ),
        style = TextStyle(
            color = SecondaryColor,
            fontSize = 10.sp
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = "V ${fmt(node.voltage)} V",
        topLeft = Offset(
            left + 12f,
            top + 73f
        ),
        style = TextStyle(
            color = SymbolColor,
            fontSize = 10.sp
        )
    )

    if (node.loadKw > 0.0) {

        drawText(
            textMeasurer = textMeasurer,
            text =
                "P ${fmt(node.loadKw)} kW",
            topLeft = Offset(
                left + 90f,
                top + 73f
            ),
            style = TextStyle(
                color = SymbolColor,
                fontSize = 10.sp
            )
        )
    }

    if (node.ratedKva > 0.0) {

        drawText(
            textMeasurer = textMeasurer,
            text =
                "S ${fmt(node.ratedKva)} kVA",
            topLeft = Offset(
                left + 12f,
                top + 91f
            ),
            style = TextStyle(
                color = SymbolColor,
                fontSize = 10.sp
            )
        )
    }

    if (engineeringResult != null) {

        drawText(
            textMeasurer = textMeasurer,
            text =
                "Ib=${fmt(engineeringResult.designCurrentA)} A",
            topLeft = Offset(
                left + 90f,
                top + 91f
            ),
            style = TextStyle(
                color = EngineeringColor,
                fontSize = 10.sp
            )
        )

        drawText(
            textMeasurer = textMeasurer,
            text =
                "Demand=${fmt(engineeringResult.demandPowerKw)} kW",
            topLeft = Offset(
                left + 12f,
                top + 108f
            ),
            style = TextStyle(
                color = EngineeringColor,
                fontSize = 9.sp
            )
        )
    }

    if (protectionDevice != null) {

        drawText(
            textMeasurer = textMeasurer,
            text =
                "${protectionDevice.deviceType} " +
                    "${fmt(protectionDevice.recommendedRatingA)} A",
            topLeft = Offset(
                left + 90f,
                top + 108f
            ),
            style = TextStyle(
                color = when (
                    protectionDevice.status.name
                ) {
                    "PASS" -> SuccessColor
                    "WARNING" -> WarningColor
                    else -> FaultColor
                },
                fontSize = 9.sp
            )
        )
    }

    if (shortCircuitResult != null) {

        val faultText =
            "Ik''=${fmt(shortCircuitResult.initialSymmetricalCurrentKa)} kA"

        drawText(
            textMeasurer = textMeasurer,
            text = faultText,
            topLeft = Offset(
                left + 12f,
                top + NODE_HEIGHT + 15f
            ),
            style = TextStyle(
                color = FaultColor,
                fontSize = 10.sp
            )
        )

        drawText(
            textMeasurer = textMeasurer,
            text =
                "Ipeak=${fmt(shortCircuitResult.peakCurrentKa)} kA",
            topLeft = Offset(
                left + 12f,
                top + NODE_HEIGHT + 31f
            ),
            style = TextStyle(
                color = FaultColor,
                fontSize = 10.sp
            )
        )

        drawText(
            textMeasurer = textMeasurer,
            text =
                "Icu req.=${fmt(shortCircuitResult.breakerRequiredKa)} kA",
            topLeft = Offset(
                left + 12f,
                top + NODE_HEIGHT + 47f
            ),
            style = TextStyle(
                color = FaultColor,
                fontSize = 10.sp
            )
        )
    }
}

private fun DrawScope.drawNodeSymbol(
    node: SldNode,
    center: Offset
) {
    drawCircle(
        color = SymbolBackground,
        radius = 25f,
        center = center
    )

    when (node.type) {

        SldNodeType.SOURCE -> {
            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x,
                    center.y - 14f
                ),
                end = Offset(
                    center.x,
                    center.y + 14f
                ),
                strokeWidth = 3f
            )

            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x - 14f,
                    center.y
                ),
                end = Offset(
                    center.x + 14f,
                    center.y
                ),
                strokeWidth = 3f
            )
        }

        SldNodeType.TRANSFORMER -> {
            drawCircle(
                color = SymbolColor,
                radius = 11f,
                center = Offset(
                    center.x - 8f,
                    center.y
                ),
                style = Stroke(3f)
            )

            drawCircle(
                color = SymbolColor,
                radius = 11f,
                center = Offset(
                    center.x + 8f,
                    center.y
                ),
                style = Stroke(3f)
            )
        }

        SldNodeType.GENERATOR -> {
            drawCircle(
                color = SymbolColor,
                radius = 16f,
                center = center,
                style = Stroke(3f)
            )

            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x - 9f,
                    center.y + 7f
                ),
                end = Offset(
                    center.x,
                    center.y - 7f
                ),
                strokeWidth = 3f
            )

            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x,
                    center.y - 7f
                ),
                end = Offset(
                    center.x + 9f,
                    center.y + 7f
                ),
                strokeWidth = 3f
            )
        }

        SldNodeType.BUS -> {
            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x - 18f,
                    center.y
                ),
                end = Offset(
                    center.x + 18f,
                    center.y
                ),
                strokeWidth = 6f
            )
        }

        SldNodeType.PANEL -> {
            drawRect(
                color = SymbolColor,
                topLeft = Offset(
                    center.x - 15f,
                    center.y - 17f
                ),
                size = androidx.compose.ui.geometry.Size(
                    30f,
                    34f
                ),
                style = Stroke(3f)
            )

            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x,
                    center.y - 13f
                ),
                end = Offset(
                    center.x,
                    center.y + 13f
                ),
                strokeWidth = 2f
            )
        }

        SldNodeType.BREAKER -> {
            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x - 17f,
                    center.y
                ),
                end = Offset(
                    center.x - 5f,
                    center.y
                ),
                strokeWidth = 3f
            )

            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x - 5f,
                    center.y
                ),
                end = Offset(
                    center.x + 12f,
                    center.y - 13f
                ),
                strokeWidth = 3f
            )

            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x + 12f,
                    center.y - 13f
                ),
                end = Offset(
                    center.x + 17f,
                    center.y - 13f
                ),
                strokeWidth = 3f
            )
        }

        SldNodeType.LOAD -> {
            drawRect(
                color = SymbolColor,
                topLeft = Offset(
                    center.x - 15f,
                    center.y - 15f
                ),
                size = androidx.compose.ui.geometry.Size(
                    30f,
                    30f
                ),
                style = Stroke(3f)
            )

            drawLine(
                color = SymbolColor,
                start = Offset(
                    center.x - 8f,
                    center.y
                ),
                end = Offset(
                    center.x + 8f,
                    center.y
                ),
                strokeWidth = 3f
            )
        }
    }
}

internal fun DrawScope.drawSldTitleBlock(
    width: Float,
    height: Float,
    nodeCount: Int,
    feederCount: Int,
    engineeringAvailable: Boolean
) {
    val blockWidth = 520f
    val blockHeight = 105f

    val left = max(
        20f,
        width - blockWidth - 25f
    )

    val top = max(
        20f,
        height - blockHeight - 25f
    )

    drawRect(
        color = Color.White,
        topLeft = Offset(
            left,
            top
        ),
        size = androidx.compose.ui.geometry.Size(
            blockWidth,
            blockHeight
        )
    )

    drawRect(
        color = PrimaryColor,
        topLeft = Offset(
            left,
            top
        ),
        size = androidx.compose.ui.geometry.Size(
            blockWidth,
            blockHeight
        ),
        style = Stroke(2f)
    )

    drawText(
        textMeasurer = rememberTextMeasurer(),
        text = "SINGLE LINE DIAGRAM",
        topLeft = Offset(
            left + 15f,
            top + 12f
        ),
        style = TextStyle(
            color = PrimaryColor,
            fontSize = 17.sp
        )
    )

    drawText(
        textMeasurer = rememberTextMeasurer(),
        text = "PROFESSIONAL ELECTRICAL DESIGN",
        topLeft = Offset(
            left + 15f,
            top + 35f
        ),
        style = TextStyle(
            color = SecondaryColor,
            fontSize = 11.sp
        )
    )

    drawText(
        textMeasurer = rememberTextMeasurer(),
        text =
            "Nodes: $nodeCount   |   Feeders: $feederCount",
        topLeft = Offset(
            left + 15f,
            top + 58f
        ),
        style = TextStyle(
            color = SymbolColor,
            fontSize = 10.sp
        )
    )

    drawText(
        textMeasurer = rememberTextMeasurer(),
        text = if (engineeringAvailable) {
            "ENGINEERING STUDY AVAILABLE"
        } else {
            "ENGINEERING STUDY NOT CALCULATED"
        },
        topLeft = Offset(
            left + 15f,
            top + 78f
        ),
        style = TextStyle(
            color = if (engineeringAvailable) {
                EngineeringColor
            } else {
                WarningColor
            },
            fontSize = 10.sp
        )
    )
}
