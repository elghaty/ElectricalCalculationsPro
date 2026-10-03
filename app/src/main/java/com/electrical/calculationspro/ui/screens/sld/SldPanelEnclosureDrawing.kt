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
import kotlin.math.max
import kotlin.math.min

/*
 * ================================================================
 * PROFESSIONAL SLD PANEL ENCLOSURE / BUSBAR DRAWING
 * ================================================================
 *
 * PANEL
 * -----
 * Represents the physical enclosure.
 *
 * BUS
 * ---
 * Represents the logical engineering bus.
 *
 * BREAKER
 * -------
 * Represents the physical breaker installed inside the enclosure.
 *
 * BUSBAR
 * ------
 * The physical internal busbar is drawn directly.
 *
 * IMPORTANT:
 * - BUSBAR is never drawn as a cable.
 * - BUSBAR does not participate in cable drawing.
 * - PANEL/BREAKER internal geometry is independent from cable routing.
 * - The enclosure remains visible even when topology is incomplete.
 * - A minimum real busbar is always drawn for every PANEL.
 *
 * This file is drawing-only.
 * No engineering calculations are performed here.
 */

/* =================================================================
 * GEOMETRY
 * ================================================================= */

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

private const val BREAKER_TOP_CLEARANCE = 75f
private const val BREAKER_BOTTOM_CLEARANCE = 65f

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

private val BUS_LABEL_COLOR =
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
 * INTERNAL CONNECTION
 * =================================================================
 *
 * Do NOT classify every connection between PANEL/BUS/BREAKER as a
 * busbar connection.
 *
 * Only the following are internal:
 *
 * PANEL <-> BREAKER
 * PANEL <-> BUS
 * BUS   <-> BREAKER
 *
 * or an explicitly stored BUSBAR connection.
 *
 * This prevents accidental internal classification of unrelated
 * SLD links.
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
        }
            ?: return false

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        }
            ?: return false

    val fromType = from.type
    val toType = to.type

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
 * =================================================================
 *
 * Finds BUS and BREAKER objects physically belonging to this PANEL.
 *
 * Explicit BUSBAR connections are respected.
 *
 * Legacy projects where the BUSBAR connection was not saved are
 * also handled safely by the geometric fallback below.
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
     * Topology traversal
     * --------------------------------------------------------------
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

    /*
     * --------------------------------------------------------------
     * Legacy / incomplete topology fallback
     * --------------------------------------------------------------
     *
     * A breaker that was created for this panel may temporarily have
     * no persisted BUSBAR relationship.
     *
     * In that case the drawing must not disappear.
     *
     * A breaker positioned below the panel is considered visually
     * associated with the enclosure.
     *
     * This affects drawing only.
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

        val breakerDeltaX =
            kotlin.math.abs(
                breakerCenterX -
                    panelCenterX
            )

        val breakerBelow =
            node.y >
                panel.y

        /*
         * Conservative geometric association.
         */
        if (
            breakerBelow &&
            breakerDeltaX <
                900f
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

        val busDeltaX =
            kotlin.math.abs(
                busCenterX -
                    panelCenterX
            )

        val busBelow =
            node.y >
                panel.y

        if (
            busBelow &&
            busDeltaX <
                900f
        ) {

            members +=
                node.id
        }
    }

    return members
}

/* =================================================================
 * PUBLIC DRAW ENTRY
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
     * PANEL ANCHOR
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
     * =================================================================
     *
     * Busbar follows actual breaker positions.
     *
     * It is never represented as a cable.
     * ================================================================= */

    val requiredBusWidth =
        max(
            MIN_BUSBAR_WIDTH,
            breakerSpan +
                2f *
                BUSBAR_SIDE_MARGIN
        )

    /* =================================================================
     * ENCLOSURE WIDTH
     * ================================================================= */

    val calculatedEnclosureWidth =
        max(
            MIN_ENCLOSURE_WIDTH,
            requiredBusWidth +
                70f
        )

    /*
     * If breaker positions are spread widely, the enclosure expands
     * around them instead of clipping the physical busbar.
     */
    val leftFromPanel =
        panelCenterX -
            calculatedEnclosureWidth / 2f

    val rightFromPanel =
        panelCenterX +
            calculatedEnclosureWidth / 2f

    val leftFromBreakers =
        minBreakerX -
            ENCLOSURE_LEFT_MARGIN

    val rightFromBreakers =
        maxBreakerX +
            ENCLOSURE_RIGHT_MARGIN

    val left =
        min(
            leftFromPanel,
            leftFromBreakers
        )

    val right =
        max(
            rightFromPanel,
            rightFromBreakers
        )

    /* =================================================================
     * ENCLOSURE TOP / BOTTOM
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
     * HEADER
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

    val title =
        panel.name
            .trim()
            .ifBlank {
                "PANEL"
            }

    drawText(
        textMeasurer = textMeasurer,
        text = title,
        topLeft = Offset(
            left + 14f,
            top + 8f
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
     * BUSBAR Y POSITION
     * =================================================================
     *
     * The physical busbar is anchored to the PANEL enclosure.
     *
     * BUS node coordinates are not allowed to make the busbar vanish
     * or move outside the enclosure.
     * ================================================================= */

    val busY =
        panel.y +
            BUSBAR_OFFSET_Y

    /* =================================================================
     * BUSBAR X POSITION
     * ================================================================= */

    val busLeft =
        max(
            left + 30f,
            panelCenterX -
                requiredBusWidth / 2f
        )

    val busRight =
        min(
            right - 30f,
            panelCenterX +
                requiredBusWidth / 2f
        )

    /*
     * Safety correction for extremely small enclosure geometry.
     */
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
     * MAIN PHYSICAL BUSBAR
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
            color = BUS_LABEL_COLOR,
            fontSize = 7.sp
        )
    )

    /*
     * If a logical BUS object exists, show its name near the physical
     * busbar. This is a label only; the BUS node is not drawn as a
     * second busbar.
     */
    val primaryBus =
        buses
            .minByOrNull {
                kotlin.math.abs(
                    (
                        it.x +
                            NODE_WIDTH / 2f
                        ) -
                        panelCenterX
                )
            }

    if (
        primaryBus != null &&
        primaryBus.name
            .trim()
            .isNotBlank()
    ) {

        drawText(
            textMeasurer = textMeasurer,
            text = primaryBus.name.trim(),
            topLeft = Offset(
                safeBusRight - 90f,
                busY + 20f
            ),
            style = TextStyle(
                color = BUS_LABEL_COLOR,
                fontSize = 7.sp
            )
        )
    }

    /* =================================================================
     * PANEL FEED TO BUSBAR
     * =================================================================
     *
     * This is a short internal conductor, not a cable object.
     * ================================================================= */

    val panelTerminalX =
        panelCenterX

    val panelTerminalY =
        top +
            PANEL_HEADER_HEIGHT

    /*
     * Vertical internal feed.
     */
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
     * BREAKER TAPS
     * =================================================================
     *
     * Every breaker belonging to this enclosure receives a direct
     * physical connection to the busbar.
     *
     * This drawing does not require a cable record.
     * ================================================================= */

    breakers.forEachIndexed { index, breaker ->

        val breakerX =
            breaker.x +
                NODE_WIDTH / 2f

        /*
         * The breaker symbol is expected to be rendered by the main
         * SLD node renderer.
         *
         * We therefore connect the busbar to the upper terminal area
         * of the breaker rather than drawing another breaker here.
         */

        val breakerCenterY =
            breaker.y +
                SYMBOL_Y

        val upperTerminalY =
            breakerCenterY -
                BREAKER_TERMINAL_OFFSET

        /* -------------------------------------------------------------
         * Orthogonal tap
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
         * Busbar take-off marker
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
         * Breaker terminal marker
         * ------------------------------------------------------------- */

        drawLine(
            color = TERMINAL_COLOR,
            start = Offset(
                breakerX,
                upperTerminalY
            ),
            end = Offset(
                breakerX,
                upperTerminalY +
                    8f
            ),
            strokeWidth = 2.2f
        )

        /*
         * Small feeder index beside the take-off.
         * It is deliberately subtle and does not replace the breaker
         * label rendered by the main node renderer.
         */
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
     * EMPTY PANEL
     * =================================================================
     *
     * If there are no breakers, the enclosure and busbar remain
     * visible. This is intentional: adding the PANEL must immediately
     * produce a professional enclosure instead of an invisible object.
     * ================================================================= */

    if (breakers.isEmpty()) {

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
