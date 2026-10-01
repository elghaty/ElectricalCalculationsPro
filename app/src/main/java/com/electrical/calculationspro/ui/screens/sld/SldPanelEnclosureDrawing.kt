package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import kotlin.math.max
import kotlin.math.min

/**
 * ================================================================
 * PANEL ENCLOSURE / INTERNAL BUSBAR PRESENTATION
 * ================================================================
 *
 * Presentation-only layer.
 *
 * It does NOT modify the engineering model.
 *
 * It provides:
 *
 * - real panel enclosure border
 * - internal busbar
 * - automatic busbar extension
 * - breaker taps
 * - panel/bus/breaker grouping
 *
 * ================================================================
 */

private const val ENCLOSURE_MARGIN = 42f
private const val ENCLOSURE_TOP = 38f
private const val ENCLOSURE_BOTTOM = 42f
private const val ENCLOSURE_RADIUS = 8f

private const val NODE_WIDTH_LOCAL = 180f
private const val NODE_HEIGHT_LOCAL = 150f
private const val SYMBOL_Y_LOCAL = 75f

private const val BUSBAR_WIDTH_LOCAL = 7f
private const val BUSBAR_MIN_WIDTH = 150f
private const val BUSBAR_SIDE_MARGIN = 55f
private const val BREAKER_TAP_WIDTH = 2.8f

private val ENCLOSURE_STROKE =
    Color(
        0xFF263238
    )

private val BUSBAR_COLOR =
    Color(
        0xFF263238
    )

private val ENCLOSURE_FILL =
    Color(
        0x08FFFFFF
    )

private val ENCLOSURE_LABEL =
    Color(
        0xFF455A64
    )

/**
 * ================================================================
 * INTERNAL BUSBAR RELATION
 * ================================================================
 */
private fun isInternalBusbarPair(
    a: SldNodeType,
    b: SldNodeType
): Boolean {

    return (
        a == SldNodeType.PANEL &&
            b == SldNodeType.BREAKER
        ) ||
        (
            a == SldNodeType.BREAKER &&
                b == SldNodeType.PANEL
            ) ||
        (
            a == SldNodeType.PANEL &&
                b == SldNodeType.BUS
            ) ||
        (
            a == SldNodeType.BUS &&
                b == SldNodeType.PANEL
            ) ||
        (
            a == SldNodeType.BUS &&
                b == SldNodeType.BREAKER
            ) ||
        (
            a == SldNodeType.BREAKER &&
                b == SldNodeType.BUS
            )
}

/**
 * ================================================================
 * INTERNAL CONNECTION
 * ================================================================
 */
private fun isInternalBusbarConnection(
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
            it.id == connection.fromNodeId
        } ?: return false

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return false

    return isInternalBusbarPair(
        from.type,
        to.type
    )
}

/**
 * ================================================================
 * GET INTERNAL PANEL MEMBERS
 * ================================================================
 */
private fun panelMembers(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): Set<String> {

    val members =
        mutableSetOf<String>()

    members.add(
        panel.id
    )

    var changed = true

    while (changed) {

        changed = false

        connections.forEach { connection ->

            if (
                !isInternalBusbarConnection(
                    connection,
                    nodes
                )
            ) {
                return@forEach
            }

            val fromInside =
                connection.fromNodeId in
                    members

            val toInside =
                connection.toNodeId in
                    members

            if (
                fromInside &&
                !toInside
            ) {

                members.add(
                    connection.toNodeId
                )

                changed = true
            }

            if (
                toInside &&
                !fromInside
            ) {

                members.add(
                    connection.fromNodeId
                )

                changed = true
            }
        }
    }

    return members
}

/**
 * ================================================================
 * DRAW ALL PANEL ENCLOSURES
 * ================================================================
 */
fun DrawScope.drawPanelEnclosures(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    textMeasurer: TextMeasurer
) {

    nodes
        .filter {
            it.type ==
                SldNodeType.PANEL
        }
        .forEach { panel ->

            drawSinglePanelEnclosure(
                panel = panel,
                nodes = nodes,
                connections = connections,
                textMeasurer = textMeasurer
            )
        }
}

/**
 * ================================================================
 * DRAW ONE ENCLOSURE
 * ================================================================
 */
private fun DrawScope.drawSinglePanelEnclosure(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    textMeasurer: TextMeasurer
) {

    val memberIds =
        panelMembers(
            panel = panel,
            nodes = nodes,
            connections = connections
        )

    val members =
        nodes.filter {
            it.id in memberIds
        }

    if (
        members.isEmpty()
    ) {
        return
    }

    /*
     * ------------------------------------------------------------
     * ENCLOSURE BOUNDS
     * ------------------------------------------------------------
     */

    val left =
        members.minOf {
            it.x
        } -
            ENCLOSURE_MARGIN

    val top =
        members.minOf {
            it.y
        } -
            ENCLOSURE_TOP

    val right =
        members.maxOf {
            it.x +
                NODE_WIDTH_LOCAL
        } +
            ENCLOSURE_MARGIN

    val bottom =
        members.maxOf {
            it.y +
                NODE_HEIGHT_LOCAL
        } +
            ENCLOSURE_BOTTOM

    drawRoundRect(
        color = ENCLOSURE_FILL,
        topLeft =
            Offset(
                left,
                top
            ),
        size =
            Size(
                right - left,
                bottom - top
            ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                ENCLOSURE_RADIUS,
                ENCLOSURE_RADIUS
            )
    )

    drawRoundRect(
        color = ENCLOSURE_STROKE,
        topLeft =
            Offset(
                left,
                top
            ),
        size =
            Size(
                right - left,
                bottom - top
            ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                ENCLOSURE_RADIUS,
                ENCLOSURE_RADIUS
            ),
        style =
            Stroke(
                width = 2.2f
            )
    )

    /*
     * ------------------------------------------------------------
     * ENCLOSURE TITLE
     * ------------------------------------------------------------
     */

    val title =
        panel.name
            .ifBlank {
                "PANEL"
            }

    val titleStyle =
        TextStyle(
            color = ENCLOSURE_LABEL,
            fontSize = 9.sp
        )

    val measured =
        textMeasurer.measure(
            text = title,
            style = titleStyle
        )

    drawText(
        textMeasurer = textMeasurer,
        text = title,
        topLeft =
            Offset(
                left + 14f,
                top + 8f
            ),
        style = titleStyle
    )

    /*
     * ------------------------------------------------------------
     * INTERNAL BUS NODES
     * ------------------------------------------------------------
     */

    val busNodes =
        members.filter {
            it.type ==
                SldNodeType.BUS
        }

    /*
     * ------------------------------------------------------------
     * BREAKERS CONNECTED INTERNALLY
     * ------------------------------------------------------------
     */

    val breakerNodes =
        members
            .filter {
                it.type ==
                    SldNodeType.BREAKER
            }
            .sortedWith(
                compareBy<SldNode> {
                    it.x
                }.thenBy {
                    it.y
                }
            )

    if (
        breakerNodes.isEmpty()
    ) {
        return
    }

    /*
     * ------------------------------------------------------------
     * BUS Y
     * ------------------------------------------------------------
     */

    val busY =
        busNodes
            .firstOrNull()
            ?.let {
                it.y +
                    SYMBOL_Y_LOCAL
            }
            ?: panel.y +
                SYMBOL_Y_LOCAL

    /*
     * ------------------------------------------------------------
     * BUS X RANGE
     * ------------------------------------------------------------
     */

    val breakerCenters =
        breakerNodes.map {
            it.x +
                NODE_WIDTH_LOCAL /
                2f
        }

    val minBreakerX =
        breakerCenters.minOrNull()
            ?: (
                panel.x +
                    NODE_WIDTH_LOCAL /
                    2f
                )

    val maxBreakerX =
        breakerCenters.maxOrNull()
            ?: minBreakerX

    val panelCenter =
        panel.x +
            NODE_WIDTH_LOCAL /
            2f

    val halfWidth =
        max(
            BUSBAR_MIN_WIDTH /
                2f,

            (
                maxBreakerX -
                    minBreakerX
                ) /
                2f +
                BUSBAR_SIDE_MARGIN
        )

    val busLeft =
        min(
            panelCenter -
                halfWidth,

            minBreakerX -
                BUSBAR_SIDE_MARGIN
        )

    val busRight =
        max(
            panelCenter +
                halfWidth,

            maxBreakerX +
                BUSBAR_SIDE_MARGIN
        )

    /*
     * ------------------------------------------------------------
     * REAL BUSBAR
     * ------------------------------------------------------------
     */

    drawLine(
        color = BUSBAR_COLOR,
        start =
            Offset(
                busLeft,
                busY
            ),
        end =
            Offset(
                busRight,
                busY
            ),
        strokeWidth =
            BUSBAR_WIDTH_LOCAL
    )

    /*
     * ------------------------------------------------------------
     * BREAKER TAPS
     * ------------------------------------------------------------
     */

    breakerNodes.forEach { breaker ->

        val x =
            breaker.x +
                NODE_WIDTH_LOCAL /
                2f

        val breakerCenter =
            breaker.y +
                SYMBOL_Y_LOCAL

        drawLine(
            color = BUSBAR_COLOR,
            start =
                Offset(
                    x,
                    busY
                ),
            end =
                Offset(
                    x,
                    breakerCenter
                ),
            strokeWidth =
                BREAKER_TAP_WIDTH
        )

        drawCircle(
            color = BUSBAR_COLOR,
            radius = 3.2f,
            center =
                Offset(
                    x,
                    busY
                )
        )
    }

    /*
     * ------------------------------------------------------------
     * PANEL CONNECTION TO INTERNAL BUS
     * ------------------------------------------------------------
     */

    if (
        busNodes.isNotEmpty()
    ) {

        val panelCenterX =
            panel.x +
                NODE_WIDTH_LOCAL /
                2f

        val panelTerminalY =
            panel.y +
                SYMBOL_Y_LOCAL

        drawLine(
            color = BUSBAR_COLOR,
            start =
                Offset(
                    panelCenterX,
                    panelTerminalY
                ),
            end =
                Offset(
                    panelCenterX,
                    busY
                ),
            strokeWidth = 2.8f
        )
    }
}
