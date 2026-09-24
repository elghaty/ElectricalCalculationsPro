package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldEngineeringPackage

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEngineeringNodeOverlay(
    nodeId: String,
    nodeX: Float,
    nodeY: Float,
    engineering: SldEngineeringPackage?,
    textMeasurer: TextMeasurer
) {
    val result =
        engineering
            ?.upstream
            ?.nodes
            ?.firstOrNull {
                it.nodeId == nodeId
            }
            ?: return

    val text =
        buildString {
            append("Ib=")
            append(fmt(result.currentA))
            append(" A")

            append(" | S=")
            append(fmt(result.kva))
            append(" kVA")

            append(" | VD=")
            append(fmt(result.voltageDropPercent))
            append("%")

            if (result.loadingPercent > 0.0) {
                append(" | Load=")
                append(fmt(result.loadingPercent))
                append("%")
            }
        }

    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft = Offset(
            nodeX,
            nodeY + NODE_HEIGHT + 2f
        ),
        style = TextStyle(
            color = Color(0xFF00695C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEngineeringFeederOverlay(
    connectionId: String,
    centerX: Float,
    y: Float,
    engineering: SldEngineeringPackage?,
    textMeasurer: TextMeasurer
) {
    val upstream =
        engineering
            ?.upstream
            ?.feeders
            ?.firstOrNull {
                it.connectionId == connectionId
            }

    val cable =
        engineering
            ?.cableSizing
            ?.results
            ?.get(connectionId)

    if (upstream == null && cable == null) {
        return
    }

    val text =
        buildString {
            val current =
                cable?.designCurrentA
                    ?: upstream?.currentA
                    ?: 0.0

            append("Ib=")
            append(fmt(current))
            append(" A")

            val breaker =
                upstream?.recommendedBreakerA
                    ?: 0.0

            if (breaker > 0.0) {
                append(" | CB=")
                append(fmt(breaker))
                append(" A")
            }

            cable?.let {
                if (it.recommendedSizeMm2 > 0.0) {
                    append(" | Cable=")
                    append(fmt(it.recommendedSizeMm2))
                    append(" mm²")

                    if (it.recommendedParallelRuns > 1) {
                        append(" × ")
                        append(it.recommendedParallelRuns)
                    }
                }

                append(" | VD=")
                append(fmt(it.recommendedVoltageDropPercent))
                append("%")
            } ?: upstream?.let {
                append(" | VD=")
                append(fmt(it.voltageDropPercent))
                append("%")
            }
        }

    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft = Offset(
            centerX - 75f,
            y
        ),
        style = TextStyle(
            color = Color(0xFF00838F),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    )
}
