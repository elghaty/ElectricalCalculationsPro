package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
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
const val NODE_HEIGHT = 150f

private const val SYMBOL_Y = 30f

private const val CABLE_WIDTH = 3f
private const val SELECTED_CABLE_WIDTH = 5.5f
private const val BUSBAR_WIDTH = 8f

private const val NODE_CLEARANCE = 18f
private const val JUNCTION_RADIUS = 4f

private const val BUSBAR_MIN_WIDTH = 120f
private const val BUSBAR_SIDE_MARGIN = 45f
private const val BREAKER_SLOT_MIN = 90f

private val BACKGROUND = Color(0xFFF7F9FB)
private val GRID = Color(0xFFE3E8EC)
private val GRID_MAJOR = Color(0xFFD0D8DE)

private val BLACK = Color(0xFF172027)
private val CABLE = Color(0xFF37474F)
private val BUSBAR = Color(0xFF202B32)

private val TEXT = Color(0xFF172027)
private val TEXT_SECONDARY = Color(0xFF60717A)

private val SELECTED = Color(0xFF1565C0)
private val START = Color(0xFFFF9800)

private val OK = Color(0xFF087F5B)
private val WARNING = Color(0xFFE67700)
private val FAULT = Color(0xFFC62828)

private val LABEL_BG =
    Color.White.copy(alpha = 0.97f)

private enum class Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

private data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

private data class PanelBusbarGeometry(
    val left: Float,
    val right: Float,
    val y: Float,
    val tapPositions: Map<String, Float>
)

/*
 * ============================================================
 * CONNECTION TYPE HELPERS
 * ============================================================
 *
 * PANEL <-> BREAKER is physically an internal panel busbar
 * connection, regardless of how an old persisted record was
 * stored.
 */

private fun isPanelBreakerPair(
    first: SldNode,
    second: SldNode
): Boolean {

    return (
        first.type == SldNodeType.PANEL &&
            second.type == SldNodeType.BREAKER
        ) ||
        (
            first.type == SldNodeType.BREAKER &&
                second.type == SldNodeType.PANEL
            )
}

private fun isPanelBreakerConnection(
    connection: SldConnection,
    nodes: List<SldNode>
): Boolean {

    val from =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        } ?: return false

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return false

    return isPanelBreakerPair(
        from,
        to
    )
}

private fun isBusbarConnection(
    connection: SldConnection,
    nodes: List<SldNode>
): Boolean {

    return connection.connectionType ==
        SldConnectionType.BUSBAR ||
        isPanelBreakerConnection(
            connection,
            nodes
        )
}

/*
 * Always resolve PANEL/BREAKER direction from topology,
 * not from the old stored connection direction.
 */
private fun panelBreakerNodes(
    connection: SldConnection,
    nodes: List<SldNode>
): Pair<SldNode, SldNode>? {

    val from =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        } ?: return null

    val to =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        } ?: return null

    if (
        !isPanelBreakerPair(
            from,
            to
        )
    ) {
        return null
    }

    val panel =
        if (
            from.type == SldNodeType.PANEL
        ) {
            from
        } else {
            to
        }

    val breaker =
        if (
            from.type == SldNodeType.BREAKER
        ) {
            from
        } else {
            to
        }

    return panel to breaker
}

private fun nodeCenter(
    node: SldNode
): Offset =
    Offset(
        node.x + NODE_WIDTH / 2f,
        node.y + SYMBOL_Y
    )

private fun directionBetween(
    from: SldNode,
    to: SldNode
): Direction {

    val a = nodeCenter(from)
    val b = nodeCenter(to)

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
): Offset =
    when (direction) {
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

private fun startPort(
    node: SldNode,
    connection: SldConnection,
    connections: List<SldConnection>,
    nodes: List<SldNode>
): Offset {

    if (
        isBusbarConnection(
            connection,
            nodes
        ) &&
        node.type == SldNodeType.PANEL
    ) {

        val geometry =
            panelBusbarGeometry(
                node,
                nodes,
                connections
            )

        return Offset(
            geometry.tapPositions[
                connection.toNodeId
            ] ?: (
                node.x +
                    NODE_WIDTH / 2f
                ),
            geometry.y
        )
    }

    val target =
        nodes.firstOrNull {
            it.id == connection.toNodeId
        }

    if (target == null) {
        return standardPort(
            node,
            Direction.DOWN
        )
    }

    return standardPort(
        node,
        directionBetween(
            node,
            target
        )
    )
}

private fun endPort(
    node: SldNode,
    connection: SldConnection,
    nodes: List<SldNode>
): Offset {

    if (
        isBusbarConnection(
            connection,
            nodes
        ) &&
        node.type == SldNodeType.BREAKER
    ) {
        return Offset(
            node.x +
                NODE_WIDTH / 2f,
            node.y
        )
    }

    val source =
        nodes.firstOrNull {
            it.id == connection.fromNodeId
        }

    if (source == null) {
        return standardPort(
            node,
            Direction.UP
        )
    }

    return standardPort(
        node,
        directionBetween(
            node,
            source
        )
    )
}

private fun panelBusbarGeometry(
    panel: SldNode,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): PanelBusbarGeometry {

    /*
     * Do NOT rely only on connectionType here.
     * Legacy PANEL/BREAKER cable records are treated as busbar.
     */
    val busbarConnections =
        connections.filter { connection ->

            val pair =
                panelBreakerNodes(
                    connection,
                    nodes
                )

            (
                connection.connectionType ==
                    SldConnectionType.BUSBAR &&
                    connection.fromNodeId ==
                    panel.id
                ) ||
                (
                    pair != null &&
                        pair.first.id ==
                        panel.id
                    )
        }

    val breakerCenters =
        busbarConnections
            .mapNotNull { connection ->

                val pair =
                    panelBreakerNodes(
                        connection,
                        nodes
                    )

                val breaker =
                    if (pair != null) {
                        pair.second
                    } else {
                        nodes.firstOrNull {
                            it.id ==
                                connection.toNodeId &&
                                it.type ==
                                SldNodeType.BREAKER
                        }
                    }

                breaker?.let {
                    it.id to
                        (
                            it.x +
                                NODE_WIDTH / 2f
                            )
                }
            }
            .toMap()

    val panelCenter =
        panel.x +
            NODE_WIDTH / 2f

    if (breakerCenters.isEmpty()) {

        return PanelBusbarGeometry(
            left =
                panelCenter -
                    BUSBAR_MIN_WIDTH / 2f,
            right =
                panelCenter +
                    BUSBAR_MIN_WIDTH / 2f,
            y =
                panel.y +
                    SYMBOL_Y,
            tapPositions =
                emptyMap()
        )
    }

    val minX =
        breakerCenters.values.minOrNull()
            ?: panelCenter

    val maxX =
        breakerCenters.values.maxOrNull()
            ?: panelCenter

    val span =
        maxX - minX

    val halfWidth =
        maxOf(
            BUSBAR_MIN_WIDTH / 2f,
            span / 2f +
                BUSBAR_SIDE_MARGIN
        )

    val left =
        min(
            panelCenter -
                halfWidth,
            minX -
                BUSBAR_SIDE_MARGIN
        )

    val right =
        max(
            panelCenter +
                halfWidth,
            maxX +
                BUSBAR_SIDE_MARGIN
        )

    val ordered =
        breakerCenters.entries
            .sortedWith(
                compareBy<Map.Entry<String, Float>> {
                    it.value
                }.thenBy {
                    it.key
                }
            )

    val tapMap =
        mutableMapOf<String, Float>()

    if (ordered.size == 1) {

        tapMap[
            ordered.first().key
        ] =
            ordered.first().value

    } else {

        val available =
            maxOf(
                0f,
                right -
                    left -
                    2f *
                    BUSBAR_SIDE_MARGIN
            )

        val spacing =
            maxOf(
                BREAKER_SLOT_MIN,
                available /
                    (ordered.size - 1)
            )

        val total =
            spacing *
                (ordered.size - 1)

        val start =
            panelCenter -
                total / 2f

        ordered.forEachIndexed {
            index,
            entry ->

            tapMap[
                entry.key
            ] =
                start +
                    index *
                    spacing
        }
    }

    return PanelBusbarGeometry(
        left = left,
        right = right,
        y =
            panel.y +
                SYMBOL_Y,
        tapPositions =
            tapMap
    )
}

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
            it.id ==
                connection.fromNodeId
        } ?: return

    val to =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
        } ?: return

    /*
     * BUSBAR is owned by the panel-enclosure drawing layer.
     *
     * It must never be rendered here as a normal connection.
     * This also prevents legacy PANEL/BREAKER cable records
     * from appearing as feeder cables.
     */
    val isBusbar =
        isBusbarConnection(
            connection,
            nodes
        )

    if (isBusbar) {
        return
    }

    val start =
        startPort(
            node = from,
            connection = connection,
            connections = connections,
            nodes = nodes
        )

    val end =
        endPort(
            node = to,
            connection = connection,
            nodes = nodes
        )

    val points =
        if (connection.routeAuto) {

            professionalRoute(
                start = start,
                end = end,
                nodes = nodes,
                fromId =
                    connection.fromNodeId,
                toId =
                    connection.toNodeId
            )

        } else if (
            connection.routePoints.isNotEmpty()
        ) {

            buildList {
                add(start)

                addAll(
                    connection.routePoints.map {
                        Offset(
                            it.x,
                            it.y
                        )
                    }
                )

                add(end)
            }

        } else {

            route(
                start,
                end
            )
        }

    if (points.size < 2) {
        return
    }

    val path =
        Path().apply {

            moveTo(
                points.first().x,
                points.first().y
            )

            points.drop(1).forEach {
                lineTo(
                    it.x,
                    it.y
                )
            }
        }

    val inadequate =
        feederResult != null &&
            !feederResult.cableAdequate

    val lineColor =
        when {
            selected -> SELECTED
            inadequate -> FAULT
            else -> CABLE
        }

    val width =
        if (selected) {
            SELECTED_CABLE_WIDTH
        } else {
            CABLE_WIDTH
        }

    drawPath(
        path = path,
        color = lineColor,
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = width,
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter
            )
    )

    drawFlowArrow(
        points = points,
        color = lineColor
    )

    val label =
        routeLabelPoint(points)

    val cableText =
        buildString {

            if (
                connection.cableSizeMm2 > 0.0
            ) {

                append(
                    fmt(
                        connection.cableSizeMm2
                    )
                )

                append(" mm²")

                if (
                    connection.parallelRuns > 1
                ) {

                    append(" × ")
                    append(
                        connection.parallelRuns
                    )
                }
            }

            if (
                connection.lengthMeters > 0.0
            ) {

                if (isNotEmpty()) {
                    append("  ")
                }

                append(
                    fmt(
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
            label.copy(
                y = label.y - 14f
            ),
        color =
            if (inadequate) {
                FAULT
            } else {
                TEXT_SECONDARY
            },
        fontSize = 8.5f
    )

    feederResult?.let {

        val resultColor =
            when {
                !it.cableAdequate -> FAULT
                it.voltageDropPercent > 3.0 ->
                    WARNING
                else -> OK
            }

        drawEngineeringLabel(
            textMeasurer = textMeasurer,
            text =
                "Ib=${fmt(it.currentA)} A   " +
                    "S=${fmt(it.kva)} kVA   " +
                    "ΔV=${fmt(it.voltageDropPercent)}%",
            point =
                label.copy(
                    y = label.y + 5f
                ),
            color = resultColor,
            fontSize = 7.5f
        )
    }
}

private fun nodeRect(
    node: SldNode
): Rect =
    Rect(
        left =
            node.x -
                NODE_CLEARANCE,
        top =
            node.y -
                NODE_CLEARANCE,
        right =
            node.x +
                NODE_WIDTH +
                NODE_CLEARANCE,
        bottom =
            node.y +
                NODE_HEIGHT +
                NODE_CLEARANCE
    )

private fun segmentIntersectsRect(
    a: Offset,
    b: Offset,
    rect: Rect
): Boolean {

    if (
        abs(a.x - b.x) < 0.5f
    ) {

        val x = a.x

        if (
            x < rect.left ||
            x > rect.right
        ) {
            return false
        }

        val minY =
            min(a.y, b.y)

        val maxY =
            max(a.y, b.y)

        return maxY >= rect.top &&
            minY <= rect.bottom
    }

    if (
        abs(a.y - b.y) < 0.5f
    ) {

        val y = a.y

        if (
            y < rect.top ||
            y > rect.bottom
        ) {
            return false
        }

        val minX =
            min(a.x, b.x)

        val maxX =
            max(a.x, b.x)

        return maxX >= rect.left &&
            minX <= rect.right
    }

    return true
}

private fun routeClear(
    points: List<Offset>,
    obstacles: List<Rect>
): Boolean {

    if (points.size < 2) {
        return true
    }

    for (
        i in 0 until points.lastIndex
    ) {

        if (
            obstacles.any {
                segmentIntersectsRect(
                    points[i],
                    points[i + 1],
                    it
                )
            }
        ) {
            return false
        }
    }

    return true
}

private fun routeLength(
    points: List<Offset>
): Float {

    var length = 0f

    for (
        i in 0 until points.lastIndex
    ) {

        val a = points[i]
        val b = points[i + 1]

        length += sqrt(
            (b.x - a.x) *
                (b.x - a.x) +
                (b.y - a.y) *
                (b.y - a.y)
        )
    }

    return length
}

private fun simplifyRoute(
    points: List<Offset>
): List<Offset> {

    if (points.size <= 2) {
        return points
    }

    val result =
        mutableListOf<Offset>()

    result += points.first()

    for (
        i in 1 until points.lastIndex
    ) {

        val previous =
            result.last()

        val current =
            points[i]

        val next =
            points[i + 1]

        val vertical =
            abs(
                previous.x -
                    current.x
            ) < 1f &&
                abs(
                    current.x -
                        next.x
                ) < 1f

        val horizontal =
            abs(
                previous.y -
                    current.y
            ) < 1f &&
                abs(
                    current.y -
                        next.y
                ) < 1f

        if (
            !vertical &&
            !horizontal
        ) {
            result += current
        }
    }

    result += points.last()

    return result
}

private fun route(
    start: Offset,
    end: Offset
): List<Offset> {

    if (
        abs(start.x - end.x) < 1f ||
        abs(start.y - end.y) < 1f
    ) {

        return listOf(
            start,
            end
        )
    }

    val midY =
        (start.y + end.y) / 2f

    return listOf(
        start,
        Offset(
            start.x,
            midY
        ),
        Offset(
            end.x,
            midY
        ),
        end
    )
}

private fun professionalRoute(
    start: Offset,
    end: Offset,
    nodes: List<SldNode>,
    fromId: String,
    toId: String
): List<Offset> {

    val obstacles =
        nodes
            .filter {
                it.id != fromId &&
                    it.id != toId
            }
            .map {
                nodeRect(it)
            }

    val midX =
        (start.x + end.x) / 2f

    val midY =
        (start.y + end.y) / 2f

    val candidates =
        listOf(

            listOf(
                start,
                Offset(
                    midX,
                    start.y
                ),
                Offset(
                    midX,
                    end.y
                ),
                end
            ),

            listOf(
                start,
                Offset(
                    start.x,
                    midY
                ),
                Offset(
                    end.x,
                    midY
                ),
                end
            ),

            listOf(
                start,
                Offset(
                    start.x,
                    end.y
                ),
                end
            ),

            listOf(
                start,
                Offset(
                    end.x,
                    start.y
                ),
                end
            )
        )
            .map {
                simplifyRoute(it)
            }
            .filter {
                routeClear(
                    it,
                    obstacles
                )
            }

    return candidates.minByOrNull {
        routeLength(it)
    } ?: route(
        start,
        end
    )
}

private fun routeLabelPoint(
    points: List<Offset>
): Offset {

    if (points.size <= 2) {

        return Offset(
            (
                points.first().x +
                    points.last().x
                ) / 2f,
            (
                points.first().y +
                    points.last().y
                ) / 2f
        )
    }

    var bestLength = 0f

    var best =
        points[
            points.size / 2
        ]

    for (
        i in 0 until points.lastIndex
    ) {

        val a = points[i]
        val b = points[i + 1]

        val length =
            sqrt(
                (b.x - a.x) *
                    (b.x - a.x) +
                    (b.y - a.y) *
                    (b.y - a.y)
            )

        if (length > bestLength) {

            bestLength = length

            best =
                Offset(
                    (a.x + b.x) / 2f,
                    (a.y + b.y) / 2f
                )
        }
    }

    return best
}

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

    val centerX =
        node.x +
            NODE_WIDTH / 2f

    val symbolY =
        node.y +
            SYMBOL_Y

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
            radius = 39f,
            center =
                Offset(
                    centerX,
                    symbolY
                ),
            style =
                androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3f
                )
        )
    }

    when (node.type) {

        SldNodeType.SOURCE ->
            drawSource(
                centerX,
                symbolY
            )

        SldNodeType.TRANSFORMER ->
            drawTransformer(
                centerX,
                symbolY
            )

        SldNodeType.GENERATOR ->
            drawGenerator(
                centerX,
                symbolY
            )

        SldNodeType.BUS ->
            drawBus(
                centerX,
                symbolY
            )

        SldNodeType.PANEL ->
            drawPanel(
                centerX,
                symbolY
            )

        SldNodeType.BREAKER -> {

            val incoming =
                connections.firstOrNull {
                    it.toNodeId ==
                        node.id
                }

            val outgoing =
                connections.firstOrNull {
                    it.fromNodeId ==
                        node.id
                }

            val connected =
                incoming?.let {
                    nodes.firstOrNull {
                        it.id ==
                            incoming.fromNodeId
                    }
                } ?: outgoing?.let {
                    nodes.firstOrNull {
                        it.id ==
                            outgoing.toNodeId
                    }
                }

            val incomingIsBusbar =
                incoming?.let {
                    isBusbarConnection(
                        it,
                        nodes
                    )
                } == true

            val direction =
                if (incomingIsBusbar) {
                    Direction.DOWN
                } else {
                    connected?.let {
                        directionBetween(
                            node,
                            it
                        )
                    } ?: Direction.DOWN
                }

            drawBreaker(
                centerX,
                symbolY,
                direction
            )
        }

        SldNodeType.LOAD ->
            drawLoad(
                centerX,
                symbolY
            )
    }

    val equipmentY =
        node.y + 70f

    val nameY =
        node.y + 84f

    val electricalY =
        node.y + 100f

    val engineeringY =
        node.y + 116f

    val resultY =
        node.y + 132f

    drawCenteredText(
        textMeasurer = textMeasurer,
        text = equipmentLabel(node.type),
        centerX = centerX,
        y = equipmentY,
        style =
            TextStyle(
                color = TEXT_SECONDARY,
                fontSize = 7.5.sp,
                fontWeight =
                    FontWeight.Bold
            )
    )

    val displayName =
        if (node.tag.isNotBlank()) {
            "${node.tag}  ${node.name}"
        } else {
            node.name
        }

    drawCenteredText(
        textMeasurer = textMeasurer,
        text = displayName.take(28),
        centerX = centerX,
        y = nameY,
        style =
            TextStyle(
                color = TEXT,
                fontSize = 10.sp,
                fontWeight =
                    FontWeight.Bold
            )
    )

    val electrical =
        buildString {

            append("V=")
            append(
                fmt(
                    node.voltage
                )
            )
            append(" V")

            append("  ")

            append(
                when (
                    node.phaseSystem.name
                ) {
                    "THREE_PHASE" -> "3Φ"
                    "SINGLE_PHASE" -> "1Φ"
                    else -> "DC"
                }
            )

            if (
                node.loadKw > 0.0
            ) {

                append("  P=")
                append(
                    fmt(
                        node.loadKw
                    )
                )
                append(" kW")
            }

            if (
                node.ratedKva > 0.0
            ) {

                append("  R=")
                append(
                    fmt(
                        node.ratedKva
                    )
                )
                append(" kVA")
            }
        }

    drawCenteredText(
        textMeasurer = textMeasurer,
        text = electrical,
        centerX = centerX,
        y = electricalY,
        style =
            TextStyle(
                color = TEXT_SECONDARY,
                fontSize = 7.2.sp
            )
    )

    engineeringResult?.let {

        drawCenteredText(
            textMeasurer = textMeasurer,
            text =
                "Pdem=${fmt(it.demandKw)} kW  " +
                    "S=${fmt(it.kva)} kVA",
            centerX = centerX,
            y = engineeringY,
            style =
                TextStyle(
                    color = TEXT_SECONDARY,
                    fontSize = 7.2.sp
                )
        )

        val resultColor =
            when {
                it.loadingPercent > 100.0 ->
                    FAULT

                it.voltageDropPercent > 3.0 ->
                    WARNING

                else ->
                    OK
            }

        drawCenteredText(
            textMeasurer = textMeasurer,
            text =
                "Ib=${fmt(it.currentA)} A  " +
                    "CB=${fmt(it.recommendedBreakerA)} A  " +
                    "ΔV=${fmt(it.voltageDropPercent)}%",
            centerX = centerX,
            y = resultY,
            style =
                TextStyle(
                    color = resultColor,
                    fontSize = 7.2.sp,
                    fontWeight =
                        FontWeight.Bold
                )
        )
    }
}

private fun DrawScope.drawPanel(
    x: Float,
    y: Float
) {

    /*
     * The panel-enclosure layer owns:
     *
     * 1. The real enclosure/border.
     * 2. The internal busbar.
     * 3. Breaker busbar taps.
     *
     * This function intentionally draws only the IEC-style
     * PANEL terminal symbol.
     */

    drawLine(
        color = BLACK,
        start =
            Offset(
                x,
                y - 30f
            ),
        end =
            Offset(
                x,
                y
            ),
        strokeWidth = 2.8f
    )

    drawCircle(
        color = BLACK,
        radius = 3.2f,
        center =
            Offset(
                x,
                y
            )
    )

    drawRoundRect(
        color = BLACK,
        topLeft =
            Offset(
                x - 34f,
                y - 9f
            ),
        size =
            Size(
                68f,
                18f
            ),
        cornerRadius =
            CornerRadius(
                3f,
                3f
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.5f
            )
    )
}

private fun DrawScope.drawSource(
    x: Float,
    y: Float
) {

    drawCircle(
        color = BLACK,
        radius = 25f,
        center = Offset(x, y),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.8f
            )
    )

    val wave =
        Path().apply {

            moveTo(
                x - 14f,
                y
            )

            cubicTo(
                x - 9f,
                y - 11f,
                x - 3f,
                y - 11f,
                x,
                y
            )

            cubicTo(
                x + 3f,
                y + 11f,
                x + 9f,
                y + 11f,
                x + 14f,
                y
            )
        }

    drawPath(
        path = wave,
        color = BLACK,
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.4f,
                cap = StrokeCap.Round
            )
    )
}

private fun DrawScope.drawTransformer(
    x: Float,
    y: Float
) {

    drawCircle(
        color = BLACK,
        radius = 18f,
        center =
            Offset(
                x - 12f,
                y
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.8f
            )
    )

    drawCircle(
        color = BLACK,
        radius = 18f,
        center =
            Offset(
                x + 12f,
                y
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.8f
            )
    )

    drawLine(
        color = BLACK,
        start =
            Offset(
                x,
                y - 25f
            ),
        end =
            Offset(
                x,
                y + 25f
            ),
        strokeWidth = 1.6f
    )
}

private fun DrawScope.drawGenerator(
    x: Float,
    y: Float
) {

    drawCircle(
        color = BLACK,
        radius = 25f,
        center = Offset(x, y),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.8f
            )
    )

    drawArc(
        color = BLACK,
        startAngle = -55f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft =
            Offset(
                x - 15f,
                y - 15f
            ),
        size =
            Size(
                30f,
                30f
            ),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.2f
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
                x + 9f,
                y
            ),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawBus(
    x: Float,
    y: Float
) {

    drawLine(
        color = BUSBAR,
        start =
            Offset(
                x - 42f,
                y
            ),
        end =
            Offset(
                x + 42f,
                y
            ),
        strokeWidth = BUSBAR_WIDTH,
        cap = StrokeCap.Square
    )
}

private fun DrawScope.drawBreaker(
    x: Float,
    y: Float,
    direction: Direction
) {

    when (direction) {

        Direction.DOWN -> {

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x,
                        y - 31f
                    ),
                end =
                    Offset(
                        x,
                        y - 8f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x,
                        y - 8f
                    )
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x,
                        y - 8f
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
                        y + 31f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x + 17f,
                        y + 31f
                    )
            )
        }

        Direction.UP -> {

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x,
                        y + 31f
                    ),
                end =
                    Offset(
                        x,
                        y + 8f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x,
                        y + 8f
                    )
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x,
                        y + 8f
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
                        y - 31f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x + 17f,
                        y - 31f
                    )
            )
        }

        Direction.RIGHT -> {

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x - 31f,
                        y
                    ),
                end =
                    Offset(
                        x - 8f,
                        y
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x - 8f,
                        y
                    )
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x - 8f,
                        y
                    ),
                end =
                    Offset(
                        x + 10f,
                        y - 17f
                    ),
                strokeWidth = 3.2f
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x + 10f,
                        y - 17f
                    ),
                end =
                    Offset(
                        x + 31f,
                        y - 17f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x + 31f,
                        y - 17f
                    )
            )
        }

        Direction.LEFT -> {

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x + 31f,
                        y
                    ),
                end =
                    Offset(
                        x + 8f,
                        y
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x + 8f,
                        y
                    )
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x + 8f,
                        y
                    ),
                end =
                    Offset(
                        x - 10f,
                        y - 17f
                    ),
                strokeWidth = 3.2f
            )

            drawLine(
                color = BLACK,
                start =
                    Offset(
                        x - 10f,
                        y - 17f
                    ),
                end =
                    Offset(
                        x - 31f,
                        y - 17f
                    ),
                strokeWidth = 2.8f
            )

            drawCircle(
                color = BLACK,
                radius = 3.2f,
                center =
                    Offset(
                        x - 31f,
                        y - 17f
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
        center = Offset(x, y),
        style =
            androidx.compose.ui.graphics.drawscope.Stroke(
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

private fun equipmentLabel(
    type: SldNodeType
): String =
    when (type) {
        SldNodeType.SOURCE -> "UTILITY SOURCE"
        SldNodeType.TRANSFORMER -> "TRANSFORMER"
        SldNodeType.GENERATOR -> "GENERATOR"
        SldNodeType.BUS -> "BUS"
        SldNodeType.PANEL -> "PANEL"
        SldNodeType.BREAKER -> "BREAKER"
        SldNodeType.LOAD -> "LOAD"
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
            fontWeight =
                FontWeight.Bold
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

    val ux = dx / length
    val uy = dy / length

    val arrowLength = 10f
    val arrowWidth = 5f

    val base =
        Offset(
            b.x -
                ux *
                arrowLength,
            b.y -
                uy *
                arrowLength
        )

    val px = -uy
    val py = ux

    val path =
        Path().apply {

            moveTo(
                b.x,
                b.y
            )

            lineTo(
                base.x +
                    px *
                    arrowWidth,
                base.y +
                    py *
                    arrowWidth
            )

            lineTo(
                base.x -
                    px *
                    arrowWidth,
                base.y -
                    py *
                    arrowWidth
            )

            close()
        }

    drawPath(
        path = path,
        color = color
    )
}

fun findNode(
    point: Offset,
    nodes: List<SldNode>
): SldNode? {

    return nodes
        .asSequence()
        .filter { node ->

            point.x >=
                node.x - 12f &&
                point.x <=
                node.x +
                    NODE_WIDTH +
                    12f &&
                point.y >=
                node.y - 12f &&
                point.y <=
                node.y +
                    NODE_HEIGHT +
                    12f
        }
        .minByOrNull { node ->

            val center =
                nodeCenter(node)

            val dx =
                point.x -
                    center.x

            val dy =
                point.y -
                    center.y

            dx * dx +
                dy * dy
        }
}

fun findConnection(
    point: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {

    var best:
        SldConnection? = null

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

        val isBusbar =
            isBusbarConnection(
                connection,
                nodes
            )

        val panelBreaker =
            panelBreakerNodes(
                connection,
                nodes
            )

        /*
         * BUSBAR is no longer a drawable connection in this layer.
         * It is still selectable using its logical geometry so
         * existing editor behaviour is preserved.
         */
        val points =
            if (
                isBusbar &&
                panelBreaker != null
            ) {

                val panel =
                    panelBreaker.first

                val breaker =
                    panelBreaker.second

                val geometry =
                    panelBusbarGeometry(
                        panel,
                        nodes,
                        connections
                    )

                val start =
                    Offset(
                        geometry.tapPositions[
                            breaker.id
                        ] ?: (
                            panel.x +
                                NODE_WIDTH / 2f
                            ),
                        geometry.y
                    )

                val end =
                    endPort(
                        breaker,
                        connection,
                        nodes
                    )

                if (
                    abs(
                        start.x -
                            end.x
                    ) < 1f
                ) {

                    listOf(
                        start,
                        end
                    )

                } else {

                    listOf(
                        start,
                        Offset(
                            start.x,
                            end.y
                        ),
                        end
                    )
                }

            } else if (connection.routeAuto) {

                professionalRoute(
                    start =
                        startPort(
                            from,
                            connection,
                            connections,
                            nodes
                        ),
                    end =
                        endPort(
                            to,
                            connection,
                            nodes
                        ),
                    nodes = nodes,
                    fromId =
                        connection.fromNodeId,
                    toId =
                        connection.toNodeId
                )

            } else if (
                connection.routePoints.isNotEmpty()
            ) {

                buildList {

                    add(
                        startPort(
                            from,
                            connection,
                            connections,
                            nodes
                        )
                    )

                    addAll(
                        connection.routePoints.map {
                            Offset(
                                it.x,
                                it.y
                            )
                        }
                    )

                    add(
                        endPort(
                            to,
                            connection,
                            nodes
                        )
                    )
                }

            } else {

                route(
                    start =
                        startPort(
                            from,
                            connection,
                            connections,
                            nodes
                        ),
                    end =
                        endPort(
                            to,
                            connection,
                            nodes
                        )
                )
            }

        for (
            i in 0 until points.lastIndex
        ) {

            val distance =
                distanceToSegment(
                    point,
                    points[i],
                    points[i + 1]
                )

            if (
                distance <
                    18f &&
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
            (
                dx * dx +
                    dy * dy
                )

    val clamped =
        t.coerceIn(
            0f,
            1f
        )

    val px =
        a.x +
            clamped * dx

    val py =
        a.y +
            clamped * dy

    return sqrt(
        (point.x - px) *
            (point.x - px) +
            (point.y - py) *
            (point.y - py)
    )
}
