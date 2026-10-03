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
 * =====================================================================
 * PROFESSIONAL SLD PANEL ENCLOSURE + REAL BUSBAR
 * =====================================================================
 *
 * Drawing responsibility only.
 *
 * PANEL:
 *     Physical enclosure.
 *
 * BUS:
 *     Logical bus node.
 *
 * BREAKER:
 *     Physical breaker installed inside the panel enclosure when it has
 *     a real BUSBAR connection to the panel.
 *
 * BUSBAR:
 *     Physical horizontal busbar drawn inside the enclosure.
 *
 * CABLE:
 *     Never drawn by this file.
 *
 * Engineering calculations:
 *     Never performed here.
 *
 * Important:
 *     BUSBAR connections are intentionally NOT rendered by
 *     drawConnection().
 *
 * This file is therefore the single visual owner of:
 *
 *     PANEL enclosure
 *     INTERNAL BUSBAR
 *     PANEL feed to BUSBAR
 *     BREAKER busbar taps
 *     breaker feeder identifiers
 *
 * No electrical calculation is performed here.
 * =====================================================================
 */


/* =====================================================================
 * GEOMETRY
 * ===================================================================== */

private const val LOCAL_SYMBOL_Y = 30f

private const val ENCLOSURE_LEFT_MARGIN = 70f
private const val ENCLOSURE_RIGHT_MARGIN = 70f
private const val ENCLOSURE_TOP = 55f
private const val ENCLOSURE_BOTTOM = 65f

private const val PANEL_HEADER_HEIGHT = 38f

/*
 * The physical busbar is positioned relative to the panel.
 *
 * This is intentionally independent of BUS node coordinates.
 */
private const val BUSBAR_OFFSET_Y = 150f

private const val BUSBAR_WIDTH = 9f

private const val BREAKER_TAP_WIDTH = 3.5f
private const val BREAKER_TERMINAL_OFFSET = 34f

private const val MIN_BUSBAR_WIDTH = 220f

private const val MIN_ENCLOSURE_WIDTH = 360f
private const val MIN_ENCLOSURE_HEIGHT = 300f

private const val BUSBAR_SIDE_MARGIN = 45f

private const val TERMINAL_RADIUS = 4f

private const val ENCLOSURE_CORNER_RADIUS = 10f


/* =====================================================================
 * COLORS
 * ===================================================================== */

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


/* =====================================================================
 * INTERNAL NODE TYPES
 * ===================================================================== */

private fun isInternalPanelType(
    type: SldNodeType
): Boolean {

    return type == SldNodeType.PANEL ||
        type == SldNodeType.BUS ||
        type == SldNodeType.BREAKER
}


/* =====================================================================
 * PANEL / BREAKER PAIR
 * ===================================================================== */

private fun isPanelBreakerPair(
    first: SldNode,
    second: SldNode
): Boolean {

    return (
        first.type == SldNodeType.PANEL &&
            second.type == SldNodeType.BREAKER
        ) || (
        first.type == SldNodeType.BREAKER &&
            second.type == SldNodeType.PANEL
        )
}


/* =====================================================================
 * PANEL / BUS PAIR
 * ===================================================================== */

private fun isPanelBusPair(
    first: SldNode,
    second: SldNode
): Boolean {

    return (
        first.type == SldNodeType.PANEL &&
            second.type == SldNodeType.BUS
        ) || (
        first.type == SldNodeType.BUS &&
            second.type == SldNodeType.PANEL
        )
}


/* =====================================================================
 * BUS / BREAKER PAIR
 * ===================================================================== */

private fun isBusBreakerPair(
    first: SldNode,
    second: SldNode
): Boolean {

    return (
        first.type == SldNodeType.BUS &&
            second.type == SldNodeType.BREAKER
        ) || (
        first.type == SldNodeType.BREAKER &&
            second.type == SldNodeType.BUS
        )
}


/* =====================================================================
 * STRICT INTERNAL CONNECTION
 * ===================================================================== */

private fun isInternalConnection(
    connection: SldConnection,
    nodes: List<SldNode>
): Boolean {

    /*
     * Explicit BUSBAR always wins.
     */
    if (
        connection.connectionType ==
            SldConnectionType.BUSBAR
    ) {
        return true
    }

    val from =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        }
            ?: return false

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        }
            ?: return false

    return isPanelBreakerPair(
        first = from,
        second = to
    ) ||
        isPanelBusPair(
            first = from,
            second = to
        ) ||
        isBusBreakerPair(
            first = from,
            second = to
        )
}


/* =====================================================================
 * FIND PANEL-BUSBAR MEMBERS
 * ===================================================================== */

private fun panelMembers(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): Set<String> {

    val members =
        mutableSetOf<String>()

    /*
     * The panel itself is always a member.
     */
    members += panel.id

    /*
     * ---------------------------------------------------------------
     * REAL TOPOLOGY ONLY
     * ---------------------------------------------------------------
     *
     * Walk only real internal connections.
     *
     * No geometric guessing.
     */
    var changed = true

    while (changed) {

        changed = false

        connections.forEach { connection ->

            if (
                !isInternalConnection(
                    connection = connection,
                    nodes = nodes
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

    return members
}


/* =====================================================================
 * GET BREAKERS CONNECTED DIRECTLY TO PANEL BUSBAR
 * ===================================================================== */

private fun panelBreakers(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): List<SldNode> {

    val result =
        mutableListOf<SldNode>()

    connections.forEach { connection ->

        if (
            connection.connectionType !=
                SldConnectionType.BUSBAR
        ) {
            return@forEach
        }

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

        val breaker =
            when {

                from.id == panel.id &&
                    to.type ==
                        SldNodeType.BREAKER ->
                    to

                to.id == panel.id &&
                    from.type ==
                        SldNodeType.BREAKER ->
                    from

                else ->
                    null
            }

        if (
            breaker != null &&
            result.none {
                it.id ==
                    breaker.id
            }
        ) {

            result += breaker
        }
    }

    return result
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
}


/* =====================================================================
 * PUBLIC DRAW ENTRY
 * ===================================================================== */

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


/* =====================================================================
 * DRAW ONE PANEL ENCLOSURE
 * ===================================================================== */

private fun DrawScope.drawSinglePanelEnclosure(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    textMeasurer: TextMeasurer
) {

    /*
     * ---------------------------------------------------------------
     * REAL TOPOLOGY
     * ---------------------------------------------------------------
     */

    val memberIds =
        panelMembers(
            panel = panel,
            nodes = nodes,
            connections = connections
        )

    /*
     * Keep the member calculation available for future BUS grouping.
     */
    val members =
        nodes.filter {
            it.id in memberIds
        }

    /*
     * ---------------------------------------------------------------
     * REAL PANEL BREAKERS
     * ---------------------------------------------------------------
     *
     * Only breakers connected through BUSBAR are considered installed
     * in this enclosure.
     */
    val breakers =
        panelBreakers(
            panel = panel,
            nodes = nodes,
            connections = connections
        )


    /*
     * ---------------------------------------------------------------
     * LOGICAL BUS MEMBERS
     * ---------------------------------------------------------------
     */
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
     * DYNAMIC BUSBAR WIDTH
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

    /*
     * If breakers exist, make sure their complete symbols remain
     * inside the physical enclosure.
     */
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
        color =
            ENCLOSURE_FILL,

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
            CornerRadius(
                ENCLOSURE_CORNER_RADIUS,
                ENCLOSURE_CORNER_RADIUS
            )
    )


    drawRoundRect(
        color =
            ENCLOSURE_BORDER,

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
            CornerRadius(
                ENCLOSURE_CORNER_RADIUS,
                ENCLOSURE_CORNER_RADIUS
            ),

        style =
            Stroke(
                width = 2.5f
            )
    )


    /* =================================================================
     * HEADER SEPARATOR
     * ================================================================= */

    drawLine(
        color =
            ENCLOSURE_BORDER,

        start =
            Offset(
                left,
                top +
                    PANEL_HEADER_HEIGHT
            ),

        end =
            Offset(
                right,
                top +
                    PANEL_HEADER_HEIGHT
            ),

        strokeWidth = 1.5f
    )


    /* =================================================================
     * PANEL HEADER
     * ================================================================= */

    val panelTitle =
        panel.name
            .trim()
            .ifBlank {
                "PANEL"
            }

    drawText(
        textMeasurer =
            textMeasurer,

        text =
            panelTitle,

        topLeft =
            Offset(
                left + 14f,
                top + 7f
            ),

        style =
            TextStyle(
                color =
                    HEADER_COLOR,

                fontSize =
                    10.sp
            )
    )


    drawText(
        textMeasurer =
            textMeasurer,

        text =
            "ENCLOSURE",

        topLeft =
            Offset(
                left + 14f,
                top + 23f
            ),

        style =
            TextStyle(
                color =
                    HEADER_COLOR,

                fontSize =
                    7.sp
            )
    )


    /* =================================================================
     * PHYSICAL BUSBAR POSITION
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
     * REAL HORIZONTAL BUSBAR
     * ================================================================= */

    drawLine(
        color =
            BUSBAR_COLOR,

        start =
            Offset(
                safeBusLeft,
                busY
            ),

        end =
            Offset(
                safeBusRight,
                busY
            ),

        strokeWidth =
            BUSBAR_WIDTH
    )


    /* =================================================================
     * BUSBAR END TERMINALS
     * ================================================================= */

    drawCircle(
        color =
            BUSBAR_COLOR,

        radius =
            TERMINAL_RADIUS,

        center =
            Offset(
                safeBusLeft,
                busY
            )
    )

    drawCircle(
        color =
            BUSBAR_COLOR,

        radius =
            TERMINAL_RADIUS,

        center =
            Offset(
                safeBusRight,
                busY
            )
    )


    /* =================================================================
     * BUSBAR LABEL
     * ================================================================= */

    drawText(
        textMeasurer =
            textMeasurer,

        text =
            "MAIN BUSBAR",

        topLeft =
            Offset(
                safeBusLeft,
                busY - 25f
            ),

        style =
            TextStyle(
                color =
                    LABEL_COLOR,

                fontSize =
                    7.sp
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
                textMeasurer =
                    textMeasurer,

                text =
                    busName,

                topLeft =
                    Offset(
                        safeBusRight - 90f,
                        busY + 20f
                    ),

                style =
                    TextStyle(
                        color =
                            LABEL_COLOR,

                        fontSize =
                            7.sp
                    )
            )
        }
    }


    /* =================================================================
     * PANEL FEED TO PHYSICAL BUSBAR
     * ================================================================= */

    val panelTerminalX =
        panelCenterX

    val panelTerminalY =
        top +
            PANEL_HEADER_HEIGHT

    drawLine(
        color =
            TERMINAL_COLOR,

        start =
            Offset(
                panelTerminalX,
                panelTerminalY
            ),

        end =
            Offset(
                panelTerminalX,
                busY
            ),

        strokeWidth = 3f
    )

    drawCircle(
        color =
            TERMINAL_COLOR,

        radius =
            TERMINAL_RADIUS,

        center =
            Offset(
                panelTerminalX,
                busY
            )
    )


    /* =================================================================
     * REAL BREAKER BUSBAR TAPS
     * ================================================================= */

    breakers.forEachIndexed { index, breaker ->

        val breakerX =
            breaker.x +
                NODE_WIDTH / 2f

        /*
         * Existing breaker symbol coordinate system.
         */
        val breakerCenterY =
            breaker.y +
                LOCAL_SYMBOL_Y

        /*
         * Upper electrical terminal of breaker.
         */
        val upperTerminalY =
            breakerCenterY -
                BREAKER_TERMINAL_OFFSET


        /* -------------------------------------------------------------
         * BUSBAR TAP
         * ------------------------------------------------------------- */

        drawLine(
            color =
                BUSBAR_COLOR,

            start =
                Offset(
                    breakerX,
                    busY
                ),

            end =
                Offset(
                    breakerX,
                    upperTerminalY
                ),

            strokeWidth =
                BREAKER_TAP_WIDTH
        )


        /* -------------------------------------------------------------
         * BUSBAR TAKE-OFF TERMINAL
         * ------------------------------------------------------------- */

        drawCircle(
            color =
                BUSBAR_COLOR,

            radius =
                TERMINAL_RADIUS,

            center =
                Offset(
                    breakerX,
                    busY
                )
        )


        /* -------------------------------------------------------------
         * BREAKER TERMINAL
         * ------------------------------------------------------------- */

        drawLine(
            color =
                TERMINAL_COLOR,

            start =
                Offset(
                    breakerX,
                    upperTerminalY
                ),

            end =
                Offset(
                    breakerX,
                    upperTerminalY + 8f
                ),

            strokeWidth = 2.2f
        )


        /* -------------------------------------------------------------
         * FEEDER IDENTIFIER
         * ------------------------------------------------------------- */

        drawText(
            textMeasurer =
                textMeasurer,

            text =
                "F${index + 1}",

            topLeft =
                Offset(
                    breakerX + 8f,
                    busY + 7f
                ),

            style =
                TextStyle(
                    color =
                        HEADER_COLOR,

                    fontSize =
                        6.sp
                )
        )
    }


    /* =================================================================
     * EMPTY PANEL
     * ================================================================= */

    if (
        breakers.isEmpty()
    ) {

        drawText(
            textMeasurer =
                textMeasurer,

            text =
                "BUSBAR",

            topLeft =
                Offset(
                    panelCenterX - 25f,
                    busY + 20f
                ),

            style =
                TextStyle(
                    color =
                        HEADER_COLOR,

                    fontSize =
                        7.sp
                )
        )
    }
}
