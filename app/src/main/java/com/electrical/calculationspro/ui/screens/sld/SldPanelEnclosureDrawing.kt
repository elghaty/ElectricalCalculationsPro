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
 * PROFESSIONAL PANEL ENCLOSURE
 * ================================================================
 *
 * PANEL is the physical enclosure.
 *
 * BUS is represented by the real internal busbar.
 *
 * BREAKER is installed inside the enclosure and connected to the
 * busbar through a physical tap.
 *
 * No cable is used to represent the internal busbar.
 */

private const val ENCLOSURE_LEFT_MARGIN = 65f
private const val ENCLOSURE_RIGHT_MARGIN = 65f
private const val ENCLOSURE_TOP = 48f
private const val ENCLOSURE_BOTTOM = 58f

private const val PANEL_HEADER_HEIGHT = 34f

private const val BUSBAR_OFFSET_Y = 145f
private const val BUSBAR_WIDTH = 9f

private const val BREAKER_TAP_WIDTH = 3.2f

private const val BREAKER_TERMINAL_OFFSET = 31f

private const val MIN_BUSBAR_WIDTH = 180f
private const val BREAKER_SLOT = 220f

private val ENCLOSURE_BORDER =
    Color(
        0xFF263238
    )

private val ENCLOSURE_FILL =
    Color(
        0x0A263238
    )

private val BUSBAR_COLOR =
    Color(
        0xFF202B32
    )

private val HEADER_COLOR =
    Color(
        0xFF455A64
    )

private val TERMINAL_COLOR =
    Color(
        0xFF37474F
    )

/*
 * ================================================================
 * INTERNAL RELATIONSHIP
 * ================================================================
 */

private fun isInternalPanelType(
    type: SldNodeType
): Boolean {

    return type ==
        SldNodeType.PANEL ||
        type ==
        SldNodeType.BUS ||
        type ==
        SldNodeType.BREAKER
}

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
            it.id ==
                connection.fromNodeId
        } ?: return false

    val to =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
        } ?: return false

    return isInternalPanelType(
        from.type
    ) &&
        isInternalPanelType(
            to.type
        )
}

/*
 * ================================================================
 * PANEL MEMBERS
 * ================================================================
 */

private fun panelMembers(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): Set<String> {

    val members =
        mutableSetOf<String>()

    members +=
        panel.id

    var changed = true

    while (
        changed
    ) {

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
                connection.fromNodeId in
                    members

            val toInside =
                connection.toNodeId in
                    members

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

                    members +=
                        candidate.id

                    changed = true
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

                    members +=
                        candidate.id

                    changed = true
                }
            }
        }
    }

    return members
}

/*
 * ================================================================
 * PUBLIC DRAW FUNCTION
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
                panel =
                    panel,

                nodes =
                    nodes,

                connections =
                    connections,

                textMeasurer =
                    textMeasurer
            )
        }
}

/*
 * ================================================================
 * ONE ENCLOSURE
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
            panel =
                panel,

            nodes =
                nodes,

            connections =
                connections
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
                }.thenBy {
                    it.y
                }.thenBy {
                    it.name
                }
            )

    val buses =
        members.filter {
            it.type ==
                SldNodeType.BUS
        }

    /*
     * ============================================================
     * ENCLOSURE GEOMETRY
     * ============================================================
     *
     * The PANEL node coordinate is now the enclosure anchor.
     * The panel itself is NOT drawn as a small equipment symbol.
     */
    val panelCenterX =
        panel.x +
            NODE_WIDTH /
            2f

    val breakerCenters =
        breakers.map {
            it.x +
                NODE_WIDTH /
                2f
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

    val requiredBusWidth =
        maxOf(
            MIN_BUSBAR_WIDTH,
            breakerSpan +
                2f *
                60f
        )

    val enclosureWidth =
        maxOf(
            NODE_WIDTH +
                ENCLOSURE_LEFT_MARGIN +
                ENCLOSURE_RIGHT_MARGIN,

            requiredBusWidth +
                2f *
                25f
        )

    val left =
        min(
            panelCenterX -
                enclosureWidth /
                2f,

            minBreakerX -
                ENCLOSURE_LEFT_MARGIN
        )

    val right =
        max(
            panelCenterX +
                enclosureWidth /
                2f,

            maxBreakerX +
                ENCLOSURE_RIGHT_MARGIN
        )

    val top =
        panel.y -
            ENCLOSURE_TOP

    val breakerBottom =
        breakers.maxOfOrNull {
            it.y +
                NODE_HEIGHT
        }
            ?: (
                panel.y +
                    BUSBAR_OFFSET_Y +
                    120f
                )

    val bottom =
        breakerBottom +
            ENCLOSURE_BOTTOM

    /*
     * ============================================================
     * ENCLOSURE BODY
     * ============================================================
     */

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
                10f,
                10f
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
                10f,
                10f
            ),

        style =
            Stroke(
                width =
                    2.5f
            )
    )

    /*
     * ============================================================
     * PANEL HEADER
     * ============================================================
     */

    val title =
        panel.name
            .trim()
            .ifBlank {
                "PANEL"
            }

    drawText(
        textMeasurer =
            textMeasurer,

        text =
            title,

        topLeft =
            Offset(
                left + 14f,
                top + 10f
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
                top + 24f
            ),

        style =
            TextStyle(
                color =
                    HEADER_COLOR,

                fontSize =
                    7.sp
            )
    )

    /*
     * ============================================================
     * BUSBAR
     * ============================================================
     *
     * A BUS node is a logical engineering object.
     *
     * The physical representation is this bar.
     */
    val busY =
        if (
            buses.isNotEmpty()
        ) {

            buses
                .map {
                    it.y
                }
                .average()
                .toFloat() +
                30f

        } else {

            panel.y +
                BUSBAR_OFFSET_Y
        }

    val busLeft =
        max(
            left + 30f,
            panelCenterX -
                requiredBusWidth /
                2f
        )

    val busRight =
        min(
            right - 30f,
            panelCenterX +
                requiredBusWidth /
                2f
        )

    drawLine(
        color =
            BUSBAR_COLOR,

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
            BUSBAR_WIDTH
    )

    /*
     * ============================================================
     * BUSBAR TERMINAL MARKERS
     * ============================================================
     */

    drawCircle(
        color =
            BUSBAR_COLOR,

        radius =
            4f,

        center =
            Offset(
                busLeft,
                busY
            )
    )

    drawCircle(
        color =
            BUSBAR_COLOR,

        radius =
            4f,

        center =
            Offset(
                busRight,
                busY
            )
    )

    /*
     * ============================================================
     * PANEL / BUS INTERNAL CONNECTION
     * ============================================================
     *
     * If there is an explicit BUS node, visually connect the
     * panel anchor to the busbar.
     */
    if (
        buses.isNotEmpty()
    ) {

        val panelTerminalX =
            panelCenterX

        val panelTerminalY =
            panel.y +
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

            strokeWidth =
                3f
        )
    }

    /*
     * ============================================================
     * BREAKER TAPS
     * ============================================================
     *
     * Every breaker installed in this enclosure gets a direct
     * physical tap from the busbar.
     */
    breakers.forEach { breaker ->

        val breakerX =
            breaker.x +
                NODE_WIDTH /
                2f

        val breakerCenterY =
            breaker.y +
                30f

        val upperTerminalY =
            breakerCenterY -
                BREAKER_TERMINAL_OFFSET

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

        drawCircle(
            color =
                BUSBAR_COLOR,

            radius =
                4f,

            center =
                Offset(
                    breakerX,
                    busY
                )
        )

        /*
         * Small vertical terminal marker immediately above
         * the breaker.
         */
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
                    upperTerminalY +
                        7f
                ),

            strokeWidth =
                2.2f
        )
    }
}
