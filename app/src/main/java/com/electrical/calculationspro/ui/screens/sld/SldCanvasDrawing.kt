package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

const val NODE_WIDTH = 180f
const val NODE_HEIGHT = 150f

private const val SYMBOL_Y = 30f

private const val CABLE_WIDTH = 3f
private const val BUSBAR_WIDTH = 8f
private const val BUSBAR_TAP_WIDTH = 5f
private const val SELECTED_WIDTH = 11f

private val BACKGROUND = Color(0xFFF7F9FB)
private val GRID = Color(0xFFE3E8EC)
private val GRID_MAJOR = Color(0xFFD0D8DE)

private val BLACK = Color(0xFF172027)
private val CABLE = Color(0xFF37474F)
private val BUSBAR = Color(0xFF263238)
private val TEXT = Color(0xFF172027)
private val SECONDARY = Color(0xFF60717A)

private val SELECTED = Color(0xFF1565C0)
private val START = Color(0xFFFF9800)
private val OK = Color(0xFF087F5B)
private val WARNING = Color(0xFFE67700)
private val FAULT = Color(0xFFC62828)

private val PANEL_FILL =
    Color.White.copy(alpha = 0.96f)

private val LABEL_BG =
    Color.White.copy(alpha = 0.97f)

private enum class Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

// ============================================================
// BASIC GEOMETRY
// ============================================================

private fun center(node: SldNode): Offset {
    return Offset(
        node.x + NODE_WIDTH / 2f,
        node.y + SYMBOL_Y
    )
}

private fun directionBetween(
    from: SldNode,
    to: SldNode
): Direction {

    val a = center(from)
    val b = center(to)

    val dx = b.x - a.x
    val dy = b.y - a.y

    return if (abs(dx) >= abs(dy)) {
        if (dx >= 0f) {
            Direction.RIGHT
        } else {
            Direction.LEFT
        }
    } else {
        if (dy >= 0f) {
            Direction.DOWN
        } else {
            Direction.UP
        }
    }
}

private fun standardPort(
    node: SldNode,
    direction: Direction
): Offset {

    return when (direction) {

        Direction.LEFT ->
            Offset(
                node.x,
                node.y + SYMBOL_Y
            )

        Direction.RIGHT ->
            Offset(
                node.x + NODE_WIDTH,
                node.y + SYMBOL_Y
            )

        Direction.UP ->
            Offset(
                node.x + NODE_WIDTH / 2f,
                node.y
            )

        Direction.DOWN ->
            Offset(
                node.x + NODE_WIDTH / 2f,
                node.y + NODE_HEIGHT
            )
    }
}

/*
 * The breaker symbol is intentionally asymmetric.
 * These coordinates MUST remain synchronized with drawBreaker().
 */
private fun breakerPort(
    node: SldNode,
    direction: Direction
): Offset {

    val x =
        node.x + NODE_WIDTH / 2f

    val y =
        node.y + SYMBOL_Y

    return when (direction) {

        Direction.UP ->
            Offset(
                x + 17f,
                y - 34f
            )

        Direction.DOWN ->
            Offset(
                x + 17f,
                y + 34f
            )

        Direction.RIGHT ->
            Offset(
                x + 34f,
                y + 17f
            )

        Direction.LEFT ->
            Offset(
                x - 34f,
                y + 17f
            )
    }
}

// ============================================================
// TOPOLOGY CLASSIFICATION
// ============================================================

private fun isPanelBreaker(
    a: SldNode,
    b: SldNode
): Boolean {

    return (
        a.type == SldNodeType.PANEL &&
            b.type == SldNodeType.BREAKER
        ) || (
        a.type == SldNodeType.BREAKER &&
            b.type == SldNodeType.PANEL
        )
}

private fun isPanelBus(
    a: SldNode,
    b: SldNode
): Boolean {

    return (
        a.type == SldNodeType.PANEL &&
            b.type == SldNodeType.BUS
        ) || (
        a.type == SldNodeType.BUS &&
            b.type == SldNodeType.PANEL
        )
}

private fun isBusBreaker(
    a: SldNode,
    b: SldNode
): Boolean {

    return (
        a.type == SldNodeType.BUS &&
            b.type == SldNodeType.BREAKER
        ) || (
        a.type == SldNodeType.BREAKER &&
            b.type == SldNodeType.BUS
        )
}

private fun isBusbarConnection(
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

    return isPanelBreaker(from, to) ||
        isPanelBus(from, to) ||
        isBusBreaker(from, to)
}

private fun isBusbarConnectionForNode(
    connection: SldConnection,
    nodeId: String,
    nodes: List<SldNode>
): Boolean {

    if (
        connection.connectionType !=
        SldConnectionType.BUSBAR
    ) {
        return false
    }

    val otherId =
        when (nodeId) {

            connection.fromNodeId ->
                connection.toNodeId

            connection.toNodeId ->
                connection.fromNodeId

            else ->
                return false
        }

    return nodes.firstOrNull {
        it.id == otherId
    }?.type == SldNodeType.BREAKER
}

// ============================================================
// CONNECTION PORTS
// ============================================================

private fun connectionPorts(
    connection: SldConnection,
    nodes: List<SldNode>
): Pair<Offset, Offset>? {

    val from =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        } ?: return null

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return null

    /*
     * BUS <-> BREAKER is not a cable.
     * The real graphical connection is drawn by the BUS
     * assembly using a vertical breaker tap.
     */
    if (isBusBreaker(from, to)) {
        val bus =
            if (from.type == SldNodeType.BUS) {
                from
            } else {
                to
            }

        val breaker =
            if (from.type == SldNodeType.BREAKER) {
                from
            } else {
                to
            }

        val direction =
            directionBetween(
                breaker,
                bus
            )

        val breakerTerminal =
            breakerPort(
                breaker,
                direction
            )

        val busTerminal =
            Offset(
                breakerTerminal.x,
                bus.y + SYMBOL_Y
            )

        return if (from.type == SldNodeType.BUS) {
            busTerminal to breakerTerminal
        } else {
            breakerTerminal to busTerminal
        }
    }

    /*
     * PANEL <-> BREAKER is an internal panel busbar tap.
     */
    if (isPanelBreaker(from, to)) {

        val panel =
            if (from.type == SldNodeType.PANEL) {
                from
            } else {
                to
            }

        val breaker =
            if (from.type == SldNodeType.BREAKER) {
                from
            } else {
                to
            }

        val direction =
            directionBetween(
                breaker,
                panel
            )

        val breakerTerminal =
            breakerPort(
                breaker,
                direction
            )

        val busPoint =
            Offset(
                breakerTerminal.x,
                panel.y + 30f
            )

        return if (from.type == SldNodeType.PANEL) {
            busPoint to breakerTerminal
        } else {
            breakerTerminal to busPoint
        }
    }

    /*
     * PANEL <-> BUS is a real busbar connection between
     * two busbar assemblies.
     */
    if (isPanelBus(from, to)) {

        val panel =
            if (from.type == SldNodeType.PANEL) {
                from
            } else {
                to
            }

        val bus =
            if (from.type == SldNodeType.BUS) {
                from
            } else {
                to
            }

        val panelPoint =
            Offset(
                panel.x + NODE_WIDTH / 2f,
                panel.y + 30f
            )

        val busPoint =
            Offset(
                bus.x + NODE_WIDTH / 2f,
                bus.y + SYMBOL_Y
            )

        return if (from.type == SldNodeType.PANEL) {
            panelPoint to busPoint
        } else {
            busPoint to panelPoint
        }
    }

    val fromDirection =
        directionBetween(
            from,
            to
        )

    val toDirection =
        directionBetween(
            to,
            from
        )

    val fromPort =
        if (from.type == SldNodeType.BREAKER) {
            breakerPort(
                from,
                fromDirection
            )
        } else {
            standardPort(
                from,
                fromDirection
            )
        }

    val toPort =
        if (to.type == SldNodeType.BREAKER) {
            breakerPort(
                to,
                toDirection
            )
        } else {
            standardPort(
                to,
                toDirection
            )
        }

    return fromPort to toPort
}

// ============================================================
// BACKGROUND
// ============================================================

fun DrawScope.drawSldEngineeringBackground() {

    drawRect(
        color = BACKGROUND
    )

    var x = 0f

    while (x <= size.width) {

        val major =
            x.toInt() % 200 == 0

        drawLine(
            color =
                if (major) {
                    GRID_MAJOR
                } else {
                    GRID
                },
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth =
                if (major) {
                    1.4f
                } else {
                    0.7f
                }
        )

        x += 40f
    }

    var y = 0f

    while (y <= size.height) {

        val major =
            y.toInt() % 200 == 0

        drawLine(
            color =
                if (major) {
                    GRID_MAJOR
                } else {
                    GRID
                },
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth =
                if (major) {
                    1.4f
                } else {
                    0.7f
                }
        )

        y += 40f
    }
}

// ============================================================
// ORTHOGONAL ROUTING
// ============================================================

private fun orthogonal(
    start: Offset,
    end: Offset
): List<Offset> {

    if (abs(start.x - end.x) < 2f) {
        return listOf(
            start,
            end
        )
    }

    if (abs(start.y - end.y) < 2f) {
        return listOf(
            start,
            end
        )
    }

    val dx =
        abs(end.x - start.x)

    val dy =
        abs(end.y - start.y)

    return if (dx >= dy) {

        val x =
            (start.x + end.x) / 2f

        listOf(
            start,
            Offset(x, start.y),
            Offset(x, end.y),
            end
        )

    } else {

        val y =
            (start.y + end.y) / 2f

        listOf(
            start,
            Offset(start.x, y),
            Offset(end.x, y),
            end
        )
    }
}

private fun routePoints(
    connection: SldConnection,
    nodes: List<SldNode>
): List<Offset>? {

    val ports =
        connectionPorts(
            connection,
            nodes
        ) ?: return null

    val result =
        mutableListOf<Offset>()

    result += ports.first

    connection.routePoints.forEach {
        point ->
        result += Offset(
            point.x,
            point.y
        )
    }

    result += ports.second

    if (result.size < 2) {
        return null
    }

    return result
}

// ============================================================
// CONNECTION DRAWING
// ============================================================

fun DrawScope.drawConnection(
    connection: SldConnection,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selected: Boolean,
    textMeasurer: TextMeasurer,
    feederResult:
        SldUpstreamEngineering.FeederResult? = null
) {

    val from =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        } ?: return

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return

    /*
     * PANEL <-> BREAKER and BUS <-> BREAKER are physically
     * rendered with the enclosure/busbar assembly.
     * Never draw them as external cables.
     */
    if (
        isPanelBreaker(from, to) ||
        isBusBreaker(from, to)
    ) {
        return
    }

    /*
     * PANEL <-> BUS is a busbar link, not a cable.
     */
    if (isPanelBus(from, to)) {

        drawPanelBusLink(
            a = from,
            b = to,
            selected = selected
        )

        return
    }

    val points =
        routePoints(
            connection,
            nodes
        ) ?: return

    val path =
        Path().apply {

            moveTo(
                points.first().x,
                points.first().y
            )

            var index = 1

            while (index < points.size) {

                val point =
                    points[index]

                lineTo(
                    point.x,
                    point.y
                )

                index++
            }
        }

    val busbar =
        isBusbarConnection(
            connection,
            nodes
        )

    val color =
        when {

            selected ->
                SELECTED

            busbar ->
                BUSBAR

            feederResult != null &&
                !feederResult.cableAdequate ->
                FAULT

            else ->
                CABLE
        }

    drawPath(
        path = path,
        color = color,
        style =
            Stroke(
                width =
                    when {
                        selected ->
                            SELECTED_WIDTH

                        busbar ->
                            BUSBAR_WIDTH

                        else ->
                            CABLE_WIDTH
                    },
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter
            )
    )

    if (busbar) {
        return
    }

    drawFlowArrow(
        points = points,
        color = color
    )

    val middle =
        points[points.size / 2]

    val cableText =
        buildString {

            if (
                connection.cableSizeMm2 > 0.0
            ) {
                append(
                    formatEngineeringValue(
                        connection.cableSizeMm2
                    )
                )
                append(" mm²")
            }

            if (
                connection.lengthMeters > 0.0
            ) {

                if (isNotEmpty()) {
                    append("  ")
                }

                append(
                    formatEngineeringValue(
                        connection.lengthMeters
                    )
                )

                append(" m")
            }

            if (isEmpty()) {
                append("FEEDER")
            }
        }

    drawEngineeringLabel(
        textMeasurer = textMeasurer,
        text = cableText,
        point =
            Offset(
                middle.x,
                middle.y - 14f
            ),
        color = SECONDARY,
        fontSize = 7.5f
    )

    if (feederResult != null) {

        val engineering =
            buildString {

                if (
                    feederResult.currentA > 0.0
                ) {
                    append("Ib ")
                    append(
                        formatEngineeringValue(
                            feederResult.currentA
                        )
                    )
                    append(" A")
                }

                if (
                    feederResult.kva > 0.0
                ) {

                    if (isNotEmpty()) {
                        append("  ")
                    }

                    append(
                        formatEngineeringValue(
                            feederResult.kva
                        )
                    )

                    append(" kVA")
                }

                if (
                    feederResult.voltageDropPercent > 0.0
                ) {

                    if (isNotEmpty()) {
                        append("  ")
                    }

                    append("ΔV ")

                    append(
                        formatEngineeringValue(
                            feederResult.voltageDropPercent
                        )
                    )

                    append("%")
                }
            }

        if (engineering.isNotBlank()) {

            drawEngineeringLabel(
                textMeasurer = textMeasurer,
                text = engineering,
                point =
                    Offset(
                        middle.x,
                        middle.y + 16f
                    ),
                color =
                    if (
                        feederResult.cableAdequate
                    ) {
                        OK
                    } else {
                        FAULT
                    },
                fontSize = 7f
            )
        }
    }
}

// ============================================================
// PANEL <-> BUS
// ============================================================

private fun DrawScope.drawPanelBusLink(
    a: SldNode,
    b: SldNode,
    selected: Boolean
) {

    val panel =
        if (a.type == SldNodeType.PANEL) {
            a
        } else {
            b
        }

    val bus =
        if (a.type == SldNodeType.BUS) {
            a
        } else {
            b
        }

    val start =
        Offset(
            panel.x + NODE_WIDTH / 2f,
            panel.y + 30f
        )

    val end =
        Offset(
            bus.x + NODE_WIDTH / 2f,
            bus.y + SYMBOL_Y
        )

    val points =
        orthogonal(
            start,
            end
        )

    val path =
        Path().apply {

            moveTo(
                points.first().x,
                points.first().y
            )

            var index = 1

            while (index < points.size) {
                lineTo(
                    points[index].x,
                    points[index].y
                )
                index++
            }
        }

    drawPath(
        path = path,
        color =
            if (selected) {
                SELECTED
            } else {
                BUSBAR
            },
        style =
            Stroke(
                width =
                    if (selected) {
                        SELECTED_WIDTH
                    } else {
                        BUSBAR_WIDTH
                    },
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter
            )
    )
}

// ============================================================
// PANEL ASSEMBLY
// ============================================================

private fun DrawScope.drawPanel(
    node: SldNode,
    breakers: List<SldNode>,
    selected: Boolean,
    connectionStart: Boolean
) {

    val left =
        node.x + 8f

    val top =
        node.y + 8f

    val width =
        NODE_WIDTH - 16f

    val height =
        NODE_HEIGHT - 16f

    val border =
        when {

            connectionStart ->
                START

            selected ->
                SELECTED

            else ->
                BLACK
        }

    /*
     * Real enclosure.
     */
    drawRoundRect(
        color = PANEL_FILL,
        topLeft =
            Offset(
                left,
                top
            ),
        size =
            Size(
                width,
                height
            ),
        cornerRadius =
            CornerRadius(
                8f,
                8f
            )
    )

    drawRoundRect(
        color = border,
        topLeft =
            Offset(
                left,
                top
            ),
        size =
            Size(
                width,
                height
            ),
        cornerRadius =
            CornerRadius(
                8f,
                8f
            ),
        style =
            Stroke(
                width =
                    if (
                        selected ||
                        connectionStart
                    ) {
                        4.5f
                    } else {
                        2.8f
                    }
            )
    )

    /*
     * Main internal busbar.
     */
    val busY =
        node.y + 30f

    val busLeft =
        node.x + 28f

    val busRight =
        node.x + NODE_WIDTH - 28f

    drawLine(
        color =
            if (selected) {
                SELECTED
            } else {
                BUSBAR
            },
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
            if (selected) {
                SELECTED_WIDTH
            } else {
                BUSBAR_WIDTH
            },
        cap = StrokeCap.Square
    )

    drawCircle(
        color =
            if (selected) {
                SELECTED
            } else {
                BUSBAR
            },
        radius = 4.5f,
        center =
            Offset(
                busLeft,
                busY
            )
    )

    drawCircle(
        color =
            if (selected) {
                SELECTED
            } else {
                BUSBAR
            },
        radius = 4.5f,
        center =
            Offset(
                busRight,
                busY
            )
    )

    /*
     * Each breaker gets its own physical tap.
     * The breaker terminal is calculated from its actual
     * orientation, so a breaker above the panel is connected
     * to its DOWN terminal and a breaker below the panel is
     * connected to its UP terminal.
     */
    breakers.forEach { breaker ->

        val direction =
            directionBetween(
                breaker,
                node
            )

        val terminal =
            breakerPort(
                breaker,
                direction
            )

        val tapX =
            terminal.x

        drawLine(
            color =
                if (selected) {
                    SELECTED
                } else {
                    BUSBAR
                },
            start =
                Offset(
                    tapX,
                    busY
                ),
            end = terminal,
            strokeWidth = BUSBAR_TAP_WIDTH,
            cap = StrokeCap.Square
        )

        drawCircle(
            color =
                if (selected) {
                    SELECTED
                } else {
                    BUSBAR
                },
            radius = 4f,
            center =
                Offset(
                    tapX,
                    busY
                )
        )
    }
}

// ============================================================
// STANDALONE BUSBAR
// ============================================================

private fun DrawScope.drawStandaloneBus(
    node: SldNode,
    breakers: List<SldNode>,
    selected: Boolean,
    connectionStart: Boolean
) {

    val color =
        when {

            connectionStart ->
                START

            selected ->
                SELECTED

            else ->
                BUSBAR
        }

    val y =
        node.y + SYMBOL_Y

    /*
     * The busbar dynamically expands to contain every
     * connected breaker tap.
     */
    val terminalXs =
        breakers.map {
            breaker ->

            val direction =
                directionBetween(
                    breaker,
                    node
                )

            breakerPort(
                breaker,
                direction
            ).x
        }

    val left =
        if (terminalXs.isEmpty()) {
            node.x + 25f
        } else {
            minOf(
                node.x + 20f,
                terminalXs.minOrNull()!! - 45f
            )
        }

    val right =
        if (terminalXs.isEmpty()) {
            node.x + NODE_WIDTH - 25f
        } else {
            maxOf(
                node.x + NODE_WIDTH - 20f,
                terminalXs.maxOrNull()!! + 45f
            )
        }

    drawLine(
        color = color,
        start =
            Offset(
                left,
                y
            ),
        end =
            Offset(
                right,
                y
            ),
        strokeWidth =
            if (
                selected ||
                connectionStart
            ) {
                SELECTED_WIDTH
            } else {
                BUSBAR_WIDTH
            },
        cap = StrokeCap.Square
    )

    drawCircle(
        color = color,
        radius = 5f,
        center =
            Offset(
                left,
                y
            )
    )

    drawCircle(
        color = color,
        radius = 5f,
        center =
            Offset(
                right,
                y
            )
    )

    breakers.forEach { breaker ->

        val direction =
            directionBetween(
                breaker,
                node
            )

        val terminal =
            breakerPort(
                breaker,
                direction
            )

        val tapX =
            terminal.x

        drawLine(
            color = color,
            start =
                Offset(
                    tapX,
                    y
                ),
            end = terminal,
            strokeWidth = BUSBAR_TAP_WIDTH,
            cap = StrokeCap.Square
        )

        drawCircle(
            color = color,
            radius = 4f,
            center =
                Offset(
                    tapX,
                    y
                )
        )
    }
}

// ============================================================
// NODE DRAWING
// ============================================================

fun DrawScope.drawNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: TextMeasurer,
    engineeringResult:
        SldUpstreamEngineering.NodeResult? = null,
    nodes: List<SldNode> = emptyList(),
    connections: List<SldConnection> = emptyList()
) {

    val cx =
        node.x + NODE_WIDTH / 2f

    val sy =
        node.y + SYMBOL_Y

    if (
        selected ||
        connectionStart
    ) {

        drawCircle(
            color =
                if (connectionStart) {
                    START
                } else {
                    SELECTED
                },
            radius =
                when (node.type) {

                    SldNodeType.PANEL ->
                        91f

                    SldNodeType.BUS ->
                        60f

                    else ->
                        40f
                },
            center =
                Offset(
                    cx,
                    sy
                ),
            style =
                Stroke(
                    width = 3f
                )
        )
    }

    when (node.type) {

        SldNodeType.SOURCE ->
            drawSource(
                cx,
                sy
            )

        SldNodeType.TRANSFORMER ->
            drawTransformer(
                cx,
                sy
            )

        SldNodeType.GENERATOR ->
            drawGenerator(
                cx,
                sy
            )

        SldNodeType.PANEL -> {

            val breakerIds =
                connections
                    .asSequence()
                    .filter {
                        connection ->
                        isPanelBreakerConnectionForPanel(
                            connection,
                            node.id,
                            nodes
                        )
                    }
                    .mapNotNull {
                        connection ->
                        if (
                            connection.fromNodeId ==
                            node.id
                        ) {
                            connection.toNodeId
                        } else {
                            connection.fromNodeId
                        }
                    }
                    .toSet()

            val breakers =
                nodes.filter {
                    it.id in breakerIds &&
                        it.type ==
                        SldNodeType.BREAKER
                }

            drawPanel(
                node = node,
                breakers = breakers,
                selected = selected,
                connectionStart = connectionStart
            )
        }

        SldNodeType.BUS -> {

            val breakerIds =
                connections
                    .asSequence()
                    .filter {
                        isBusbarConnectionForNode(
                            it,
                            node.id,
                            nodes
                        )
                    }
                    .mapNotNull {
                        connection ->

                        if (
                            connection.fromNodeId ==
                            node.id
                        ) {
                            connection.toNodeId
                        } else if (
                            connection.toNodeId ==
                            node.id
                        ) {
                            connection.fromNodeId
                        } else {
                            null
                        }
                    }
                    .toSet()

            val breakers =
                nodes.filter {
                    it.id in breakerIds &&
                        it.type ==
                        SldNodeType.BREAKER
                }

            drawStandaloneBus(
                node = node,
                breakers = breakers,
                selected = selected,
                connectionStart = connectionStart
            )
        }

        SldNodeType.BREAKER -> {

            val connected =
                connections
                    .asSequence()
                    .mapNotNull {
                        connection ->

                        if (
                            connection.fromNodeId ==
                            node.id
                        ) {

                            nodes.firstOrNull {
                                it.id ==
                                    connection.toNodeId
                            }

                        } else if (
                            connection.toNodeId ==
                            node.id
                        ) {

                            nodes.firstOrNull {
                                it.id ==
                                    connection.fromNodeId
                            }

                        } else {
                            null
                        }
                    }
                    .firstOrNull()

            val direction =
                connected?.let {
                    directionBetween(
                        node,
                        it
                    )
                } ?: Direction.UP

            drawBreaker(
                x = cx,
                y = sy,
                direction = direction
            )
        }

        SldNodeType.LOAD ->
            drawLoad(
                x = cx,
                y = sy
            )
    }

    /*
     * Equipment name.
     */
    drawCenteredText(
        textMeasurer = textMeasurer,
        text =
            node.name
                .trim()
                .ifBlank {
                    equipmentLabel(
                        node.type
                    )
                },
        centerX = cx,
        y =
            when (node.type) {

                SldNodeType.PANEL ->
                    node.y + 112f

                SldNodeType.BUS ->
                    node.y + 62f

                else ->
                    node.y + 65f
            },
        style =
            TextStyle(
                color = TEXT,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
    )

    /*
     * Equipment type.
     */
    drawCenteredText(
        textMeasurer = textMeasurer,
        text =
            equipmentLabel(
                node.type
            ),
        centerX = cx,
        y =
            when (node.type) {

                SldNodeType.PANEL ->
                    node.y + 126f

                SldNodeType.BUS ->
                    node.y + 76f

                else ->
                    node.y + 83f
            },
        style =
            TextStyle(
                color = SECONDARY,
                fontSize = 7.sp
            )
    )

    engineeringResult?.let { result ->

        val line =
            buildString {

                if (
                    result.demandKw > 0.0
                ) {
                    append(
                        formatEngineeringValue(
                            result.demandKw
                        )
                    )
                    append(" kW")
                }

                if (
                    result.kva > 0.0
                ) {

                    if (isNotEmpty()) {
                        append("  ")
                    }

                    append(
                        formatEngineeringValue(
                            result.kva
                        )
                    )

                    append(" kVA")
                }

                if (
                    result.currentA > 0.0
                ) {

                    if (isNotEmpty()) {
                        append("  ")
                    }

                    append("Ib ")

                    append(
                        formatEngineeringValue(
                            result.currentA
                        )
                    )

                    append(" A")
                }
            }

        if (line.isNotBlank()) {

            drawCenteredText(
                textMeasurer = textMeasurer,
                text = line,
                centerX = cx,
                y =
                    node.y +
                        when (node.type) {

                            SldNodeType.PANEL ->
                                140f

                            SldNodeType.BUS ->
                                91f

                            else ->
                                99f
                        },
                style =
                    TextStyle(
                        color =
                            if (
                                result.voltageDropPercent >
                                3.0
                            ) {
                                WARNING
                            } else {
                                OK
                            },
                        fontSize = 7.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
            )
        }

        if (
            result.recommendedBreakerA > 0.0
        ) {

            drawCenteredText(
                textMeasurer = textMeasurer,
                text =
                    "CB " +
                        formatEngineeringValue(
                            result.recommendedBreakerA
                        ) +
                        " A",
                centerX = cx,
                y =
                    node.y +
                        when (node.type) {

                            SldNodeType.PANEL ->
                                154f

                            SldNodeType.BUS ->
                                105f

                            else ->
                                113f
                        },
                style =
                    TextStyle(
                        color = SECONDARY,
                        fontSize = 7.sp
                    )
            )
        }

        if (
            result.voltageDropPercent > 0.0
        ) {

            drawCenteredText(
                textMeasurer = textMeasurer,
                text =
                    "ΔV " +
                        formatEngineeringValue(
                            result.voltageDropPercent
                        ) +
                        "%",
                centerX = cx,
                y =
                    node.y +
                        when (node.type) {

                            SldNodeType.PANEL ->
                                168f

                            SldNodeType.BUS ->
                                119f

                            else ->
                                127f
                        },
                style =
                    TextStyle(
                        color =
                            if (
                                result.voltageDropPercent >
                                3.0
                            ) {
                                FAULT
                            } else {
                                OK
                            },
                        fontSize = 7.sp
                    )
            )
        }
    }
}

private fun isPanelBreakerConnectionForPanel(
    connection: SldConnection,
    panelId: String,
    nodes: List<SldNode>
): Boolean {

    if (
        connection.connectionType !=
        SldConnectionType.BUSBAR
    ) {
        return false
    }

    if (
        connection.fromNodeId != panelId &&
        connection.toNodeId != panelId
    ) {
        return false
    }

    val otherId =
        if (
            connection.fromNodeId ==
            panelId
        ) {
            connection.toNodeId
        } else {
            connection.fromNodeId
        }

    return nodes.firstOrNull {
        it.id == otherId
    }?.type == SldNodeType.BREAKER
}

// ============================================================
// SYMBOLS
// ============================================================

private fun DrawScope.drawSource(
    x: Float,
    y: Float
) {

    drawCircle(
        color = BLACK,
        radius = 25f,
        center =
            Offset(
                x,
                y
            ),
        style =
            Stroke(
                width = 2.8f
            )
    )

    drawLine(
        color = BLACK,
        start =
            Offset(
                x - 11f,
                y - 8f
            ),
        end =
            Offset(
                x,
                y + 9f
            ),
        strokeWidth = 2.5f
    )

    drawLine(
        color = BLACK,
        start =
            Offset(
                x,
                y + 9f
            ),
        end =
            Offset(
                x + 11f,
                y - 8f
            ),
        strokeWidth = 2.5f
    )
}

private fun DrawScope.drawTransformer(
    x: Float,
    y: Float
) {

    drawCircle(
        color = BLACK,
        radius = 19f,
        center =
            Offset(
                x - 12f,
                y
            ),
        style =
            Stroke(
                width = 2.4f
            )
    )

    drawCircle(
        color = BLACK,
        radius = 19f,
        center =
            Offset(
                x + 12f,
                y
            ),
        style =
            Stroke(
                width = 2.4f
            )
    )

    drawLine(
        color = BLACK,
        start =
            Offset(
                x - 40f,
                y
            ),
        end =
            Offset(
                x - 31f,
                y
            ),
        strokeWidth = 2.5f
    )

    drawLine(
        color = BLACK,
        start =
            Offset(
                x + 31f,
                y
            ),
        end =
            Offset(
                x + 40f,
                y
            ),
        strokeWidth = 2.5f
    )
}

private fun DrawScope.drawGenerator(
    x: Float,
    y: Float
) {

    drawCircle(
        color = BLACK,
        radius = 27f,
        center =
            Offset(
                x,
                y
            ),
        style =
            Stroke(
                width = 2.5f
            )
    )

    drawLine(
        color = BLACK,
        start =
            Offset(
                x - 12f,
                y - 10f
            ),
        end =
            Offset(
                x + 12f,
                y + 10f
            ),
        strokeWidth = 2f
    )

    drawLine(
        color = BLACK,
        start =
            Offset(
                x - 12f,
                y + 10f
            ),
        end =
            Offset(
                x + 12f,
                y - 10f
            ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawBreaker(
    x: Float,
    y: Float,
    direction: Direction
) {

    when (direction) {

        Direction.UP -> {

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x,
                        y + 34f
                    ),
                end =
                    Offset(
                        x,
                        y + 9f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x,
                        y + 9f
                    )
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x,
                        y + 9f
                    ),
                end =
                    Offset(
                        x + 17f,
                        y - 10f
                    ),
                strokeWidth = 3.2f
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x + 17f,
                        y - 10f
                    ),
                end =
                    Offset(
                        x + 17f,
                        y - 34f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x + 17f,
                        y - 34f
                    )
            )
        }

        Direction.DOWN -> {

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x,
                        y - 34f
                    ),
                end =
                    Offset(
                        x,
                        y - 9f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x,
                        y - 9f
                    )
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x,
                        y - 9f
                    ),
                end =
                    Offset(
                        x + 17f,
                        y + 10f
                    ),
                strokeWidth = 3.2f
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x + 17f,
                        y + 10f
                    ),
                end =
                    Offset(
                        x + 17f,
                        y + 34f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x + 17f,
                        y + 34f
                    )
            )
        }

        Direction.RIGHT -> {

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x - 34f,
                        y
                    ),
                end =
                    Offset(
                        x - 9f,
                        y
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x - 9f,
                        y
                    )
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x - 9f,
                        y
                    ),
                end =
                    Offset(
                        x + 10f,
                        y + 17f
                    ),
                strokeWidth = 3.2f
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x + 10f,
                        y + 17f
                    ),
                end =
                    Offset(
                        x + 34f,
                        y + 17f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x + 34f,
                        y + 17f
                    )
            )
        }

        Direction.LEFT -> {

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x + 34f,
                        y
                    ),
                end =
                    Offset(
                        x + 9f,
                        y
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x + 9f,
                        y
                    )
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x + 9f,
                        y
                    ),
                end =
                    Offset(
                        x - 10f,
                        y + 17f
                    ),
                strokeWidth = 3.2f
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x - 10f,
                        y + 17f
                    ),
                end =
                    Offset(
                        x - 34f,
                        y + 17f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x - 34f,
                        y + 17f
                    )
            )
        }
    }
}

private fun DrawScope.drawLoad(
    x: Float,
    y: Float
) {

    drawCircle(
        color = BLACK,
        radius = 22f,
        center =
            Offset(
                x,
                y
            ),
        style =
            Stroke(
                width = 2.8f
            )
    )

    drawLine(
        color = BLACK,
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
        strokeWidth = 2.8f
    )

    drawLine(
        color = BLACK,
        start =
            Offset(
                x - 12f,
                y - 12f
            ),
        end =
            Offset(
                x + 12f,
                y + 12f
            ),
        strokeWidth = 1.8f
    )
}

// ============================================================
// LABELS
// ============================================================

private fun equipmentLabel(
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
            "PANEL"

        SldNodeType.BREAKER ->
            "BREAKER"

        SldNodeType.LOAD ->
            "LOAD"
    }
}

private fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    text: String,
    centerX: Float,
    y: Float,
    style: TextStyle
) {

    if (text.isBlank()) {
        return
    }

    val measured =
        textMeasurer.measure(
            text = text,
            style = style
        )

    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft =
            Offset(
                centerX -
                    measured.size.width / 2f,
                y
            ),
        style = style
    )
}

private fun DrawScope.drawEngineeringLabel(
    textMeasurer: TextMeasurer,
    text: String,
    point: Offset,
    color: Color,
    fontSize: Float
) {

    if (text.isBlank()) {
        return
    }

    val style =
        TextStyle(
            color = color,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )

    val measured =
        textMeasurer.measure(
            text = text,
            style = style
        )

    drawRoundRect(
        color = LABEL_BG,
        topLeft =
            Offset(
                point.x -
                    measured.size.width / 2f -
                    5f,
                point.y - 3f
            ),
        size =
            Size(
                measured.size.width + 10f,
                measured.size.height + 7f
            ),
        cornerRadius =
            CornerRadius(
                3f,
                3f
            )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = text,
        topLeft =
            Offset(
                point.x -
                    measured.size.width / 2f,
                point.y
            ),
        style = style
    )
}

private fun DrawScope.drawFlowArrow(
    points: List<Offset>,
    color: Color
) {

    if (points.size < 2) {
        return
    }

    val a =
        points[
            points.lastIndex - 1
        ]

    val b =
        points.last()

    val dx =
        b.x - a.x

    val dy =
        b.y - a.y

    val length =
        sqrt(
            dx * dx +
                dy * dy
        )

    if (length < 14f) {
        return
    }

    val ux =
        dx / length

    val uy =
        dy / length

    val base =
        Offset(
            b.x - ux * 10f,
            b.y - uy * 10f
        )

    val px =
        -uy

    val py =
        ux

    val path =
        Path().apply {

            moveTo(
                b.x,
                b.y
            )

            lineTo(
                base.x + px * 5f,
                base.y + py * 5f
            )

            lineTo(
                base.x - px * 5f,
                base.y - py * 5f
            )

            close()
        }

    drawPath(
        path = path,
        color = color
    )
}

// ============================================================
// HIT TEST
// ============================================================

fun findNode(
    point: Offset,
    nodes: List<SldNode>
): SldNode? {

    return nodes
        .asSequence()
        .filter { node ->

            point.x >=
                node.x - 15f &&
                point.x <=
                node.x +
                NODE_WIDTH +
                15f &&
                point.y >=
                node.y - 15f &&
                point.y <=
                node.y +
                NODE_HEIGHT +
                15f
        }
        .minByOrNull { node ->

            val c =
                center(node)

            val dx =
                point.x - c.x

            val dy =
                point.y - c.y

            dx * dx +
                dy * dy
        }
}

fun findConnection(
    point: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {

    var best: SldConnection? = null

    var bestDistance =
        Float.MAX_VALUE

    connections.forEach { connection ->

        val from =
            nodes.firstOrNull {
                it.id == connection.fromNodeId
            } ?: return@forEach

        val to =
            nodes.firstOrNull {
                it.id == connection.toNodeId
            } ?: return@forEach

        /*
         * PANEL <-> BREAKER:
         * hit-test the actual physical tap.
         */
        if (isPanelBreaker(from, to)) {

            val panel =
                if (from.type == SldNodeType.PANEL) {
                    from
                } else {
                    to
                }

            val breaker =
                if (from.type == SldNodeType.BREAKER) {
                    from
                } else {
                    to
                }

            val direction =
                directionBetween(
                    breaker,
                    panel
                )

            val terminal =
                breakerPort(
                    breaker,
                    direction
                )

            val busPoint =
                Offset(
                    terminal.x,
                    panel.y + 30f
                )

            val distance =
                distanceToSegment(
                    point,
                    busPoint,
                    terminal
                )

            if (
                distance < 24f &&
                distance < bestDistance
            ) {

                bestDistance = distance
                best = connection
            }

            return@forEach
        }

        /*
         * BUS <-> BREAKER:
         * hit-test the actual busbar tap.
         */
        if (isBusBreaker(from, to)) {

            val bus =
                if (from.type == SldNodeType.BUS) {
                    from
                } else {
                    to
                }

            val breaker =
                if (from.type == SldNodeType.BREAKER) {
                    from
                } else {
                    to
                }

            val direction =
                directionBetween(
                    breaker,
                    bus
                )

            val terminal =
                breakerPort(
                    breaker,
                    direction
                )

            val busPoint =
                Offset(
                    terminal.x,
                    bus.y + SYMBOL_Y
                )

            val distance =
                distanceToSegment(
                    point,
                    busPoint,
                    terminal
                )

            if (
                distance < 24f &&
                distance < bestDistance
            ) {

                bestDistance = distance
                best = connection
            }

            return@forEach
        }

        val ports =
            connectionPorts(
                connection,
                nodes
            ) ?: return@forEach

        val points =
            if (
                connection.routePoints.isNotEmpty()
            ) {

                val result =
                    mutableListOf<Offset>()

                result += ports.first

                connection.routePoints.forEach {
                    result += Offset(
                        it.x,
                        it.y
                    )
                }

                result += ports.second

                result

            } else {
                orthogonal(
                    ports.first,
                    ports.second
                )
            }

        var index = 0

        while (
            index < points.lastIndex
        ) {

            val distance =
                distanceToSegment(
                    point,
                    points[index],
                    points[index + 1]
                )

            val tolerance =
                if (
                    isBusbarConnection(
                        connection,
                        nodes
                    )
                ) {
                    24f
                } else {
                    18f
                }

            if (
                distance < tolerance &&
                distance < bestDistance
            ) {

                bestDistance = distance
                best = connection
            }

            index++
        }
    }

    return best
}

private fun distanceToSegment(
    point: Offset,
    a: Offset,
    b: Offset
): Float {

    val dx =
        b.x - a.x

    val dy =
        b.y - a.y

    if (
        abs(dx) < 0.001f &&
        abs(dy) < 0.001f
    ) {

        return sqrt(
            (point.x - a.x) *
                (point.x - a.x) +
                (point.y - a.y) *
                (point.y - a.y)
        )
    }

    val t =
        (
            (point.x - a.x) * dx +
                (point.y - a.y) * dy
            ) /
            (dx * dx + dy * dy)

    val clamped =
        t.coerceIn(
            0f,
            1f
        )

    val px =
        a.x + clamped * dx

    val py =
        a.y + clamped * dy

    return sqrt(
        (point.x - px) *
            (point.x - px) +
            (point.y - py) *
            (point.y - py)
    )
}

// ============================================================
// ENGINEERING FORMATTER
// ============================================================

private fun formatEngineeringValue(
    value: Double
): String {

    if (!value.isFinite()) {
        return "0"
    }

    val absolute =
        abs(value)

    return when {

        absolute >= 1000.0 ->
            String.format(
                Locale.US,
                "%.0f",
                value
            )

        absolute >= 100.0 ->
            String.format(
                Locale.US,
                "%.1f",
                value
            )

        absolute >= 10.0 ->
            String.format(
                Locale.US,
                "%.2f",
                value
            )

        else ->
            String.format(
                Locale.US,
                "%.2f",
                value
            )
    }
}
