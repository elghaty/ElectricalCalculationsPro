package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * ================================================================
 * PROFESSIONAL PANEL ENCLOSURE / REAL BUSBAR DRAWING
 * ================================================================
 *
 * PANEL  = physical enclosure
 * BUS    = logical engineering bus
 * BREAKER = physical breaker installed in enclosure
 * BUSBAR = physical internal busbar
 *
 * This file is DRAWING ONLY.
 *
 * No electrical calculation is performed here.
 * No cable is created here.
 *
 * BUSBAR is never drawn by drawConnection().
 */

/* =================================================================
 * LOCAL GEOMETRY
 * ================================================================= */

private const val LOCAL_SYMBOL_Y = 30f

private const val ENCLOSURE_LEFT_MARGIN = 70f
private const val ENCLOSURE_RIGHT_MARGIN = 70f
private const val ENCLOSURE_TOP = 55f
private const val ENCLOSURE_BOTTOM = 65f

private const val PANEL_HEADER_HEIGHT = 38f

private const val BUSBAR_OFFSET_Y = 150f
private const val BUSBAR_WIDTH = 9f

private const val BREAKER_TAP_WIDTH = 3.5f
private const val BREAKER_TERMINAL_OFFSET = 34f

private const val MIN_BUSBAR_WIDTH = 220f
private const val MIN_ENCLOSURE_WIDTH = 360f
private const val MIN_ENCLOSURE_HEIGHT = 300f

private const val BUSBAR_SIDE_MARGIN = 45f
private const val TERMINAL_RADIUS = 4f

/* =================================================================
 * COLORS
 * ================================================================= */

private val ENCLOSURE_BORDER =
    Color(0xFF263238)

private val ENCLOSURE_FILL =
    Color(0x0A263238)

private val HEADER_COLOR =
    Color(0xFF455A64)

private val BUSBAR_COLOR =
    Color(0xFF202B32)

private val TERMINAL_COLOR =
    Color(0xFF37474F)

private val LABEL_COLOR =
    Color(0xFF263238)

/* =================================================================
 * INTERNAL NODE TYPE
 * ================================================================= */

private fun isInternalPanelType(
    type: SldNodeType
): Boolean {

    return type == SldNodeType.PANEL ||
        type == SldNodeType.BUS ||
        type == SldNodeType.BREAKER
}

/* =================================================================
 * STRICT INTERNAL CONNECTION
 * ================================================================= */

private fun isInternalConnection(
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

    val fromType =
        from.type

    val toType =
        to.type

    return (
        fromType == SldNodeType.PANEL &&
            (
                toType == SldNodeType.BREAKER ||
                    toType == SldNodeType.BUS
                )
        ) ||
        (
            toType == SldNodeType.PANEL &&
                (
                    fromType == SldNodeType.BREAKER ||
                        fromType == SldNodeType.BUS
                    )
            ) ||
        (
            fromType == SldNodeType.BUS &&
                toType == SldNodeType.BREAKER
            ) ||
        (
            toType == SldNodeType.BUS &&
                fromType == SldNodeType.BREAKER
            )
}

/* =================================================================
 * PANEL MEMBERS
 * ================================================================= */

private fun panelMembers(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): Set<String> {

    val members =
        mutableSetOf<String>()

    members += panel.id

    /*
     * --------------------------------------------------------------
     * Real topology traversal
     * --------------------------------------------------------------
     */

    var changed = true

    while (changed) {

        changed = false

        connections.forEach { connection ->

            if (
                !isInternalConnection(
                    connection,
                    nodes
                )
            ) {
                return@forEach
            }

            val fromInside =
                connection.fromNodeId in members

            val toInside =
                connection.toNodeId in members

            if (
                fromInside &&
                !toInside
            ) {

                val candidate =
                    nodes.firstOrNull {
                        it.id ==
                            connection.toNodeId
                    }

                if (
                    candidate != null &&
                    isInternalPanelType(
                        candidate.type
                    )
                ) {

                    if (
                        members.add(
                            candidate.id
                        )
                    ) {
                        changed = true
                    }
                }
            }

            if (
                toInside &&
                !fromInside
            ) {

                val candidate =
                    nodes.firstOrNull {
                        it.id ==
                            connection.fromNodeId
                    }

                if (
                    candidate != null &&
                    isInternalPanelType(
                        candidate.type
                    )
                ) {

                    if (
                        members.add(
                            candidate.id
                        )
                    ) {
                        changed = true
                    }
                }
            }
        }
    }

    /*
     * --------------------------------------------------------------
     * Safe drawing fallback
     * --------------------------------------------------------------
     *
     * If a breaker was created but BUSBAR connection has not yet been
     * persisted, it must still be visible inside the panel.
     *
     * This fallback changes DRAWING only.
     * It does NOT create an engineering connection.
     * --------------------------------------------------------------
     */

    val panelCenterX =
        panel.x +
            NODE_WIDTH / 2f

    nodes.forEach { node ->

        if (
            node.type !=
                SldNodeType.BREAKER
        ) {
            return@forEach
        }

        if (
            node.id in members
        ) {
            return@forEach
        }

        val breakerCenterX =
            node.x +
                NODE_WIDTH / 2f

        val dx =
            abs(
                breakerCenterX -
                    panelCenterX
            )

        if (
            node.y > panel.y &&
            dx <= 900f
        ) {

            members +=
                node.id
        }
    }

    /*
     * --------------------------------------------------------------
     * BUS fallback
     * --------------------------------------------------------------
     */

    nodes.forEach { node ->

        if (
            node.type !=
                SldNodeType.BUS
        ) {
            return@forEach
        }

        if (
            node.id in members
        ) {
            return@forEach
        }

        val busCenterX =
            node.x +
                NODE_WIDTH / 2f

        val dx =
            abs(
                busCenterX -
                    panelCenterX
            )

        if (
            node.y > panel.y &&
            dx <= 900f
        ) {

            members +=
                node.id
        }
    }

    return members
}

/* =================================================================
 * PUBLIC DRAW FUNCTION
 * ================================================================= */

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

/* =================================================================
 * SINGLE PANEL ENCLOSURE
 * ================================================================= */

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

    val breakers =
        members
            .filter {
                it.type ==
                    SldNodeType.BREAKER
            }
            .sortedWith(
                compareBy<SldNode> {
                    it.x
                }
                    .thenBy {
                        it.y
                    }
                    .thenBy {
                        it.name
                    }
            )

    val buses =
        members.filter {
            it.type ==
                SldNodeType.BUS
        }

    /* =================================================================
     * PANEL CENTER
     * ================================================================= */

    val panelCenterX =
        panel.x +
            NODE_WIDTH / 2f

    /* =================================================================
     * BREAKER CENTERS
     * ================================================================= */

    val breakerCenters =
        breakers.map {
            it.x +
                NODE_WIDTH / 2f
        }

    val minBreakerX =
        breakerCenters.minOrNull()
            ?: panelCenterX

    val maxBreakerX =
        breakerCenters.maxOrNull()
            ?: panelCenterX

    val breakerSpan =
        maxBreakerX -
            minBreakerX

    /* =================================================================
     * BUSBAR WIDTH
     * ================================================================= */

    val requiredBusWidth =
        max(
            MIN_BUSBAR_WIDTH,
            breakerSpan +
                BUSBAR_SIDE_MARGIN * 2f
        )

    /* =================================================================
     * ENCLOSURE WIDTH
     * ================================================================= */

    val calculatedWidth =
        max(
            MIN_ENCLOSURE_WIDTH,
            requiredBusWidth + 70f
        )

    val panelLeft =
        panelCenterX -
            calculatedWidth / 2f

    val panelRight =
        panelCenterX +
            calculatedWidth / 2f

    val breakerLeft =
        minBreakerX -
            ENCLOSURE_LEFT_MARGIN

    val breakerRight =
        maxBreakerX +
            ENCLOSURE_RIGHT_MARGIN

    val left =
        min(
            panelLeft,
            breakerLeft
        )

    val right =
        max(
            panelRight,
            breakerRight
        )

    /* =================================================================
     * ENCLOSURE HEIGHT
     * ================================================================= */

    val top =
        panel.y -
            ENCLOSURE_TOP

    val defaultBottom =
        panel.y +
            BUSBAR_OFFSET_Y +
            MIN_ENCLOSURE_HEIGHT

    val breakerBottom =
        breakers.maxOfOrNull {
            it.y +
                NODE_HEIGHT
        }
            ?: defaultBottom

    val bottom =
        max(
            defaultBottom,
            breakerBottom +
                ENCLOSURE_BOTTOM
        )

    /* =================================================================
     * ENCLOSURE BODY
     * ================================================================= */

    drawRoundRect(
        color = ENCLOSURE_FILL,
        topLeft = Offset(
            left,
            top
        ),
        size = Size(
            right - left,
            bottom - top
        ),
        cornerRadius = CornerRadius(
            10f,
            10f
        )
    )

    drawRoundRect(
        color = ENCLOSURE_BORDER,
        topLeft = Offset(
            left,
            top
        ),
        size = Size(
            right - left,
            bottom - top
        ),
        cornerRadius = CornerRadius(
            10f,
            10f
        ),
        style = Stroke(
            width = 2.5f
        )
    )

    /* =================================================================
     * HEADER SEPARATOR
     * ================================================================= */

    drawLine(
        color = ENCLOSURE_BORDER,
        start = Offset(
            left,
            top +
                PANEL_HEADER_HEIGHT
        ),
        end = Offset(
            right,
            top +
                PANEL_HEADER_HEIGHT
        ),
        strokeWidth = 1.5f
    )

    /* =================================================================
     * PANEL LABEL
     * ================================================================= */

    val panelTitle =
        panel.name
            .trim()
            .ifBlank {
                "PANEL"
            }

    drawText(
        textMeasurer = textMeasurer,
        text = panelTitle,
        topLeft = Offset(
            left + 14f,
            top + 7f
        ),
        style = TextStyle(
            color = HEADER_COLOR,
            fontSize = 10.sp
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = "ENCLOSURE",
        topLeft = Offset(
            left + 14f,
            top + 23f
        ),
        style = TextStyle(
            color = HEADER_COLOR,
            fontSize = 7.sp
        )
    )

    /* =================================================================
     * PHYSICAL BUSBAR POSITION
     * =================================================================
     *
     * IMPORTANT:
     *
     * The physical busbar is anchored to the PANEL.
     *
     * BUS node coordinates do not control the physical busbar.
     * This prevents the busbar from disappearing because of an
     * incomplete or malformed BUS topology.
     * ================================================================= */

    val busY =
        panel.y +
            BUSBAR_OFFSET_Y

    /* =================================================================
     * BUSBAR X RANGE
     * ================================================================= */

    val rawBusLeft =
        panelCenterX -
            requiredBusWidth / 2f

    val rawBusRight =
        panelCenterX +
            requiredBusWidth / 2f

    val busLeft =
        max(
            left + 30f,
            rawBusLeft
        )

    val busRight =
        min(
            right - 30f,
            rawBusRight
        )

    val safeBusLeft =
        min(
            busLeft,
            busRight
        )

    val safeBusRight =
        max(
            busLeft,
            busRight
        )

    /* =================================================================
     * MAIN REAL BUSBAR
     * ================================================================= */

    drawLine(
        color = BUSBAR_COLOR,
        start = Offset(
            safeBusLeft,
            busY
        ),
        end = Offset(
            safeBusRight,
            busY
        ),
        strokeWidth = BUSBAR_WIDTH
    )

    /* =================================================================
     * BUSBAR END TERMINALS
     * ================================================================= */

    drawCircle(
        color = BUSBAR_COLOR,
        radius = TERMINAL_RADIUS,
        center = Offset(
            safeBusLeft,
            busY
        )
    )

    drawCircle(
        color = BUSBAR_COLOR,
        radius = TERMINAL_RADIUS,
        center = Offset(
            safeBusRight,
            busY
        )
    )

    /* =================================================================
     * BUSBAR LABEL
     * ================================================================= */

    drawText(
        textMeasurer = textMeasurer,
        text = "MAIN BUSBAR",
        topLeft = Offset(
            safeBusLeft,
            busY - 25f
        ),
        style = TextStyle(
            color = LABEL_COLOR,
            fontSize = 7.sp
        )
    )

    /* =================================================================
     * LOGICAL BUS LABEL
     * ================================================================= */

    val primaryBus =
        buses.minByOrNull {
            abs(
                (
                    it.x +
                        NODE_WIDTH / 2f
                    ) -
                    panelCenterX
            )
        }

    if (
        primaryBus != null
    ) {

        val busName =
            primaryBus.name
                .trim()

        if (
            busName.isNotBlank()
        ) {

            drawText(
                textMeasurer = textMeasurer,
                text = busName,
                topLeft = Offset(
                    safeBusRight - 90f,
                    busY + 20f
                ),
                style = TextStyle(
                    color = LABEL_COLOR,
                    fontSize = 7.sp
                )
            )
        }
    }

    /* =================================================================
     * PANEL FEED TO BUSBAR
     * ================================================================= */

    val panelTerminalX =
        panelCenterX

    val panelTerminalY =
        top +
            PANEL_HEADER_HEIGHT

    drawLine(
        color = TERMINAL_COLOR,
        start = Offset(
            panelTerminalX,
            panelTerminalY
        ),
        end = Offset(
            panelTerminalX,
            busY
        ),
        strokeWidth = 3f
    )

    drawCircle(
        color = TERMINAL_COLOR,
        radius = TERMINAL_RADIUS,
        center = Offset(
            panelTerminalX,
            busY
        )
    )

    /* =================================================================
     * BREAKER BUSBAR TAPS
     * ================================================================= */

    breakers.forEachIndexed { index, breaker ->

        val breakerX =
            breaker.x +
                NODE_WIDTH / 2f

        /*
         * Keep the connection aligned with the existing breaker
         * symbol coordinate system.
         */
        val breakerCenterY =
            breaker.y +
                LOCAL_SYMBOL_Y

        val upperTerminalY =
            breakerCenterY -
                BREAKER_TERMINAL_OFFSET

        /* -------------------------------------------------------------
         * Vertical busbar tap
         * ------------------------------------------------------------- */

        drawLine(
            color = BUSBAR_COLOR,
            start = Offset(
                breakerX,
                busY
            ),
            end = Offset(
                breakerX,
                upperTerminalY
            ),
            strokeWidth = BREAKER_TAP_WIDTH
        )

        /* -------------------------------------------------------------
         * Busbar take-off point
         * ------------------------------------------------------------- */

        drawCircle(
            color = BUSBAR_COLOR,
            radius = TERMINAL_RADIUS,
            center = Offset(
                breakerX,
                busY
            )
        )

        /* -------------------------------------------------------------
         * Breaker terminal
         * ------------------------------------------------------------- */

        drawLine(
            color = TERMINAL_COLOR,
            start = Offset(
                breakerX,
                upperTerminalY
            ),
            end = Offset(
                breakerX,
                upperTerminalY + 8f
            ),
            strokeWidth = 2.2f
        )

        /* -------------------------------------------------------------
         * Feeder identifier
         * ------------------------------------------------------------- */

        drawText(
            textMeasurer = textMeasurer,
            text = "F${index + 1}",
            topLeft = Offset(
                breakerX + 8f,
                busY + 7f
            ),
            style = TextStyle(
                color = HEADER_COLOR,
                fontSize = 6.sp
            )
        )
    }

    /* =================================================================
     * EMPTY PANEL INDICATION
     * ================================================================= */

    if (
        breakers.isEmpty()
    ) {

        drawText(
            textMeasurer = textMeasurer,
            text = "BUSBAR",
            topLeft = Offset(
                panelCenterX - 25f,
                busY + 20f
            ),
            style = TextStyle(
                color = HEADER_COLOR,
                fontSize = 7.sp
            )
        )
    }
}
