package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import kotlin.math.abs
import kotlin.math.sqrt

private const val NODE_WIDTH = 150f
private const val NODE_HEIGHT = 72f
private const val NODE_RADIUS = 10f
private const val HIT_RADIUS = 85f

@Composable
fun SldCanvas(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    connectionStartId: String?,
    onSelectNode: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onSelectConnection: (String) -> Unit,
    onEditNode: (SldNode) -> Unit,
    onEditConnection: (SldConnection) -> Unit
) {

    val textMeasurer =
        rememberTextMeasurer()

    val horizontalScroll =
        rememberScrollState()

    val verticalScroll =
        rememberScrollState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .horizontalScroll(
                    horizontalScroll
                )
                .verticalScroll(
                    verticalScroll
                )
                .background(
                    Color(0xFFF7F9FA)
                )
    ) {

        Canvas(
            modifier =
                Modifier
                    .width(3000.dp)
                    .height(1800.dp)
                    .pointerInput(
                        nodes,
                        connections
                    ) {

                        detectTapGestures(

                            onDoubleTap = { point ->

                                val node =
                                    findNode(
                                        point,
                                        nodes
                                    )

                                if (node != null) {

                                    onEditNode(
                                        node
                                    )

                                    return@detectTapGestures
                                }

                                val connection =
                                    findConnection(
                                        point,
                                        nodes,
                                        connections
                                    )

                                if (
                                    connection != null
                                ) {

                                    onEditConnection(
                                        connection
                                    )
                                }
                            },

                            onTap = { point ->

                                val node =
                                    findNode(
                                        point,
                                        nodes
                                    )

                                if (node != null) {

                                    onSelectNode(
                                        node.id
                                    )

                                    return@detectTapGestures
                                }

                                val connection =
                                    findConnection(
                                        point,
                                        nodes,
                                        connections
                                    )

                                if (
                                    connection != null
                                ) {

                                    onSelectConnection(
                                        connection.id
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(
                        nodes
                    ) {

                        var draggingNodeId:
                            String? = null

                        detectDragGestures(

                            onDragStart = { point ->

                                draggingNodeId =
                                    findNode(
                                        point,
                                        nodes
                                    )?.id
                            },

                            onDrag = {
                                change,
                                dragAmount ->

                                change.consume()

                                val id =
                                    draggingNodeId

                                if (
                                    id != null
                                ) {

                                    onMoveNode(
                                        id,
                                        dragAmount.x,
                                        dragAmount.y
                                    )
                                }
                            },

                            onDragEnd = {
                                draggingNodeId =
                                    null
                            },

                            onDragCancel = {
                                draggingNodeId =
                                    null
                            }
                        )
                    }
        ) {

            drawGrid()

            connections.forEach { connection ->

                drawSldConnection(
                    connection =
                        connection,
                    nodes =
                        nodes,
                    selected =
                        connection.id ==
                            selectedConnectionId,
                    textMeasurer =
                        textMeasurer
                )
            }

            nodes.forEach { node ->

                drawSldNode(
                    node =
                        node,
                    selected =
                        node.id ==
                            selectedNodeId,
                    connectionStart =
                        node.id ==
                            connectionStartId,
                    textMeasurer =
                        textMeasurer
                )
            }
        }
    }
}

/* ================================================================
 * GRID
 * ================================================================ */

private fun DrawScope.drawGrid() {

    val minorStep = 25f
    val majorStep = 100f

    var x = 0f

    while (x <= size.width) {

        val major =
            x % majorStep == 0f

        drawLine(
            color =
                if (major) {
                    Color(0xFFD5DDE2)
                } else {
                    Color(0xFFE9EEF1)
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

        x += minorStep
    }

    var y = 0f

    while (y <= size.height) {

        val major =
            y % majorStep == 0f

        drawLine(
            color =
                if (major) {
                    Color(0xFFD5DDE2)
                } else {
                    Color(0xFFE9EEF1)
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

        y += minorStep
    }
}

/* ================================================================
 * NODE
 * ================================================================ */

private fun DrawScope.drawSldNode(
    node: SldNode,
    selected: Boolean,
    connectionStart: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {

    val center =
        Offset(
            node.x,
            node.y
        )

    val left =
        center.x -
            NODE_WIDTH / 2f

    val top =
        center.y -
            NODE_HEIGHT / 2f

    val rect =
        Rect(
            left,
            top,
            left + NODE_WIDTH,
            top + NODE_HEIGHT
        )

    val borderColor =
        when {

            connectionStart ->
                Color(0xFFFF9800)

            selected ->
                Color(0xFF1565C0)

            else ->
                Color(0xFF37474F)
        }

    val borderWidth =
        when {

            connectionStart ->
                4f

            selected ->
                4f

            else ->
                2.2f
        }

    drawRoundRect(
        color =
            nodeFillColor(
                node.type
            ),
        topLeft =
            Offset(
                left,
                top
            ),
        size =
            androidx.compose.ui.geometry.Size(
                NODE_WIDTH,
                NODE_HEIGHT
            ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                NODE_RADIUS,
                NODE_RADIUS
            )
    )

    drawRoundRect(
        color =
            borderColor,
        topLeft =
            Offset(
                left,
                top
            ),
        size =
            androidx.compose.ui.geometry.Size(
                NODE_WIDTH,
                NODE_HEIGHT
            ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                NODE_RADIUS,
                NODE_RADIUS
            ),
        style =
            androidx.compose.ui.graphics.drawscope
                .Stroke(
                    width =
                        borderWidth
                )
    )

    drawNodeSymbol(
        node =
            node,
        center =
            Offset(
                left + 25f,
                center.y
            )
    )

    val title =
        node.name
            .ifBlank {
                node.type.name
            }

    val titleLayout =
        textMeasurer.measure(
            text =
                title,
            style =
                TextStyle(
                    fontSize =
                        12.sp,
                    color =
                        Color(0xFF172027)
                )
        )

    drawText(
        textLayoutResult =
            titleLayout,
        topLeft =
            Offset(
                left + 45f,
                top + 11f
            )
    )

    val typeLayout =
        textMeasurer.measure(
            text =
                node.type.name,
            style =
                TextStyle(
                    fontSize =
                        9.sp,
                    color =
                        Color(0xFF607078)
                )
        )

    drawText(
        textLayoutResult =
            typeLayout,
        topLeft =
            Offset(
                left + 45f,
                top + 30f
            )
    )

    val engineeringText =
        buildEngineeringText(
            node
        )

    val engineeringLayout =
        textMeasurer.measure(
            text =
                engineeringText,
            style =
                TextStyle(
                    fontSize =
                        8.sp,
                    color =
                        Color(0xFF455A64)
                )
        )

    drawText(
        textLayoutResult =
            engineeringLayout,
        topLeft =
            Offset(
                left + 45f,
                top + 47f
            )
    )
}

/* ================================================================
 * NODE SYMBOLS
 * ================================================================ */

private fun DrawScope.drawNodeSymbol(
    node: SldNode,
    center: Offset
) {

    when (node.type) {

        SldNodeType.SOURCE -> {

            drawCircle(
                color =
                    Color(0xFF263238),
                radius =
                    13f,
                center =
                    center,
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(
                            width = 2.5f
                        )
            )

            drawLine(
                color =
                    Color(0xFF263238),
                start =
                    Offset(
                        center.x - 7f,
                        center.y
                    ),
                end =
                    Offset(
                        center.x + 7f,
                        center.y
                    ),
                strokeWidth =
                    2f
            )

            drawLine(
                color =
                    Color(0xFF263238),
                start =
                    Offset(
                        center.x,
                        center.y - 7f
                    ),
                end =
                    Offset(
                        center.x,
                        center.y + 7f
                    ),
                strokeWidth =
                    2f
            )
        }

        SldNodeType.TRANSFORMER -> {

            drawCircle(
                color =
                    Color(0xFF37474F),
                radius =
                    11f,
                center =
                    Offset(
                        center.x - 7f,
                        center.y
                    ),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(
                            width = 2f
                        )
            )

            drawCircle(
                color =
                    Color(0xFF37474F),
                radius =
                    11f,
                center =
                    Offset(
                        center.x + 7f,
                        center.y
                    ),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(
                            width = 2f
                        )
            )
        }

        SldNodeType.GENERATOR -> {

            drawCircle(
                color =
                    Color(0xFF37474F),
                radius =
                    13f,
                center =
                    center,
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(
                            width = 2f
                        )
            )

            drawLine(
                color =
                    Color(0xFF37474F),
                start =
                    Offset(
                        center.x - 7f,
                        center.y
                    ),
                end =
                    Offset(
                        center.x + 7f,
                        center.y
                    ),
                strokeWidth =
                    2f
            )
        }

        SldNodeType.BUS -> {

            drawLine(
                color =
                    Color(0xFF263238),
                start =
                    Offset(
                        center.x - 14f,
                        center.y
                    ),
                end =
                    Offset(
                        center.x + 14f,
                        center.y
                    ),
                strokeWidth =
                    6f,
                cap =
                    StrokeCap.Round
            )
        }

        SldNodeType.BREAKER -> {

            drawRect(
                color =
                    Color(0xFF263238),
                topLeft =
                    Offset(
                        center.x - 10f,
                        center.y - 10f
                    ),
                size =
                    androidx.compose.ui.geometry.Size(
                        20f,
                        20f
                    ),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(
                            width = 2f
                        )
            )

            drawLine(
                color =
                    Color(0xFF263238),
                start =
                    Offset(
                        center.x - 7f,
                        center.y + 7f
                    ),
                end =
                    Offset(
                        center.x + 7f,
                        center.y - 7f
                    ),
                strokeWidth =
                    2f
            )
        }

        SldNodeType.PANEL -> {

            drawRect(
                color =
                    Color(0xFF37474F),
                topLeft =
                    Offset(
                        center.x - 11f,
                        center.y - 14f
                    ),
                size =
                    androidx.compose.ui.geometry.Size(
                        22f,
                        28f
                    ),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(
                            width = 2f
                        )
            )

            drawLine(
                color =
                    Color(0xFF37474F),
                start =
                    Offset(
                        center.x,
                        center.y - 10f
                    ),
                end =
                    Offset(
                        center.x,
                        center.y + 10f
                    ),
                strokeWidth =
                    1.5f
            )
        }

        SldNodeType.LOAD -> {

            drawCircle(
                color =
                    Color(0xFF37474F),
                radius =
                    13f,
                center =
                    center,
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(
                            width = 2f
                        )
            )

            drawLine(
                color =
                    Color(0xFF37474F),
                start =
                    Offset(
                        center.x - 8f,
                        center.y + 5f
                    ),
                end =
                    Offset(
                        center.x + 8f,
                        center.y - 5f
                    ),
                strokeWidth =
                    2f
            )

            drawLine(
                color =
                    Color(0xFF37474F),
                start =
                    Offset(
                        center.x - 8f,
                        center.y - 5f
                    ),
                end =
                    Offset(
                        center.x + 8f,
                        center.y + 5f
                    ),
                strokeWidth =
                    2f
            )
        }
    }
}

/* ================================================================
 * CONNECTION
 * ================================================================ */

private fun DrawScope.drawSldConnection(
    connection: SldConnection,
    nodes: List<SldNode>,
    selected: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {

    val from =
        nodes.firstOrNull {
            it.id ==
                connection.fromNodeId
        }
            ?: return

    val to =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
        }
            ?: return

    val start =
        connectionPoint(
            from,
            to
        )

    val end =
        connectionPoint(
            to,
            from
        )

    val midX =
        (start.x + end.x) / 2f

    val path =
        Path().apply {

            moveTo(
                start.x,
                start.y
            )

            lineTo(
                midX,
                start.y
            )

            lineTo(
                midX,
                end.y
            )

            lineTo(
                end.x,
                end.y
            )
        }

    drawPath(
        path =
            path,
        color =
            if (selected) {
                Color(0xFF1565C0)
            } else {
                Color(0xFF455A64)
            },
        style =
            androidx.compose.ui.graphics.drawscope
                .Stroke(
                    width =
                        if (selected) {
                            4f
                        } else {
                            2.5f
                        },
                    join =
                        StrokeJoin.Round
                )
    )

    drawArrow(
        start =
            Offset(
                midX,
                end.y
            ),
        end =
            end,
        color =
            if (selected) {
                Color(0xFF1565C0)
            } else {
                Color(0xFF455A64)
            }
    )

    drawFeederLabel(
        connection =
            connection,
        center =
            Offset(
                midX,
                (start.y + end.y) / 2f
            ),
        textMeasurer =
            textMeasurer
    )
}

private fun connectionPoint(
    node: SldNode,
    other: SldNode
): Offset {

    val dx =
        other.x - node.x

    val dy =
        other.y - node.y

    return when {

        abs(dx) >= abs(dy) -> {

            Offset(
                x =
                    node.x +
                        if (dx >= 0f) {
                            NODE_WIDTH / 2f
                        } else {
                            -NODE_WIDTH / 2f
                        },
                y =
                    node.y
            )
        }

        else -> {

            Offset(
                x =
                    node.x,
                y =
                    node.y +
                        if (dy >= 0f) {
                            NODE_HEIGHT / 2f
                        } else {
                            -NODE_HEIGHT / 2f
                        }
            )
        }
    }
}

private fun DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color
) {

    val dx =
        end.x - start.x

    val dy =
        end.y - start.y

    val length =
        sqrt(
            dx * dx +
                dy * dy
        )

    if (length < 1f) {
        return
    }

    val ux =
        dx / length

    val uy =
        dy / length

    val size =
        9f

    val left =
        Offset(
            end.x -
                ux * size -
                uy * size * 0.55f,
            end.y -
                uy * size +
                ux * size * 0.55f
        )

    val right =
        Offset(
            end.x -
                ux * size +
                uy * size * 0.55f,
            end.y -
                uy * size -
                ux * size * 0.55f
        )

    val path =
        Path().apply {

            moveTo(
                end.x,
                end.y
            )

            lineTo(
                left.x,
                left.y
            )

            lineTo(
                right.x,
                right.y
            )

            close()
        }

    drawPath(
        path =
            path,
        color =
            color
    )
}

private fun DrawScope.drawFeederLabel(
    connection: SldConnection,
    center: Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {

    val labels =
        mutableListOf<String>()

    if (
        connection.lengthMeters > 0.0
    ) {

        labels +=
            "%.0f m".format(
                connection.lengthMeters
            )
    }

    if (
        connection.cableSizeMm2 > 0.0
    ) {

        labels +=
            "%.0f mm²".format(
                connection.cableSizeMm2
            )
    }

    if (
        connection.parallelRuns > 1
    ) {

        labels +=
            "×${connection.parallelRuns}"
    }

    if (
        connection.currentCapacityA > 0.0
    ) {

        labels +=
            "%.0f A".format(
                connection.currentCapacityA
            )
    }

    if (
        connection.voltageDropPercent > 0.0
    ) {

        labels +=
            "ΔV %.2f%%".format(
                connection.voltageDropPercent
            )
    }

    if (labels.isEmpty()) {
        return
    }

    val label =
        labels.joinToString(
            separator = "  "
        )

    val layout =
        textMeasurer.measure(
            text =
                label,
            style =
                TextStyle(
                    fontSize =
                        8.sp,
                    color =
                        Color(0xFF37474F)
                )
        )

    val width =
        layout.size.width
            .toFloat()

    val height =
        layout.size.height
            .toFloat()

    drawRoundRect(
        color =
            Color.White,
        topLeft =
            Offset(
                center.x -
                    width / 2f -
                    5f,
                center.y -
                    height / 2f -
                    3f
            ),
        size =
            androidx.compose.ui.geometry.Size(
                width + 10f,
                height + 6f
            ),
        cornerRadius =
            androidx.compose.ui.geometry.CornerRadius(
                5f,
                5f
            )
    )

    drawText(
        textLayoutResult =
            layout,
        topLeft =
            Offset(
                center.x -
                    width / 2f,
                center.y -
                    height / 2f
            )
    )
}

/* ================================================================
 * HIT TEST
 * ================================================================ */

private fun findNode(
    point: Offset,
    nodes: List<SldNode>
): SldNode? {

    return nodes
        .asReversed()
        .firstOrNull { node ->

            point.x >=
                node.x -
                NODE_WIDTH / 2f -
                HIT_RADIUS / 2f &&

            point.x <=
                node.x +
                NODE_WIDTH / 2f +
                HIT_RADIUS / 2f &&

            point.y >=
                node.y -
                NODE_HEIGHT / 2f -
                HIT_RADIUS / 2f &&

            point.y <=
                node.y +
                NODE_HEIGHT / 2f +
                HIT_RADIUS / 2f
        }
}

private fun findConnection(
    point: Offset,
    nodes: List<SldNode>,
    connections: List<SldConnection>
): SldConnection? {

    return connections
        .asReversed()
        .firstOrNull { connection ->

            val from =
                nodes.firstOrNull {
                    it.id ==
                        connection.fromNodeId
                }

            val to =
                nodes.firstOrNull {
                    it.id ==
                        connection.toNodeId
                }

            if (
                from == null ||
                to == null
            ) {
                false
            } else {

                val start =
                    connectionPoint(
                        from,
                        to
                    )

                val end =
                    connectionPoint(
                        to,
                        from
                    )

                distanceToOrthogonalPath(
                    point,
                    start,
                    end
                ) <= 18f
            }
        }
}

private fun distanceToOrthogonalPath(
    point: Offset,
    start: Offset,
    end: Offset
): Float {

    val midX =
        (start.x + end.x) / 2f

    val d1 =
        distanceToSegment(
            point,
            start,
            Offset(
                midX,
                start.y
            )
        )

    val d2 =
        distanceToSegment(
            point,
            Offset(
                midX,
                start.y
            ),
            Offset(
                midX,
                end.y
            )
        )

    val d3 =
        distanceToSegment(
            point,
            Offset(
                midX,
                end.y
            ),
            end
        )

    return minOf(
        d1,
        d2,
        d3
    )
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
        dx == 0f &&
        dy == 0f
    ) {

        return distance(
            point,
            a
        )
    }

    val t =
        (
            (
                point.x - a.x
            ) * dx +
                (
                    point.y - a.y
                ) * dy
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

    val projection =
        Offset(
            a.x +
                clamped * dx,
            a.y +
                clamped * dy
        )

    return distance(
        point,
        projection
    )
}

private fun distance(
    a: Offset,
    b: Offset
): Float {

    val dx =
        a.x - b.x

    val dy =
        a.y - b.y

    return sqrt(
        dx * dx +
            dy * dy
    )
}

/* ================================================================
 * VISUAL HELPERS
 * ================================================================ */

private fun nodeFillColor(
    type: SldNodeType
): Color {

    return when (type) {

        SldNodeType.SOURCE ->
            Color(0xFFE8F5E9)

        SldNodeType.TRANSFORMER ->
            Color(0xFFFFF8E1)

        SldNodeType.GENERATOR ->
            Color(0xFFE3F2FD)

        SldNodeType.BUS ->
            Color(0xFFECEFF1)

        SldNodeType.PANEL ->
            Color(0xFFE8EAF6)

        SldNodeType.BREAKER ->
            Color(0xFFFFEBEE)

        SldNodeType.LOAD ->
            Color(0xFFF3E5F5)
    }
}

private fun buildEngineeringText(
    node: SldNode
): String {

    return when {

        node.type ==
            SldNodeType.SOURCE &&
            node.sourceShortCircuitMva > 0.0 -> {

            "SCC %.1f MVA".format(
                node.sourceShortCircuitMva
            )
        }

        node.type ==
            SldNodeType.TRANSFORMER &&
            node.ratedKva > 0.0 -> {

            "%.0f kVA  Z %.1f%%".format(
                node.ratedKva,
                node.transformerPercentZ
            )
        }

        node.type ==
            SldNodeType.GENERATOR &&
            node.ratedKva > 0.0 -> {

            "%.0f kVA  Xd %.1f%%".format(
                node.ratedKva,
                node.generatorXdSubtransient
            )
        }

        node.loadKw > 0.0 -> {

            "%.1f kW  %.2f pf".format(
                node.loadKw,
                node.powerFactor
            )
        }

        node.ratedKva > 0.0 -> {

            "%.0f kVA".format(
                node.ratedKva
            )
        }

        else -> {

            "%.0f V".format(
                node.voltage
            )
        }
    }
}
