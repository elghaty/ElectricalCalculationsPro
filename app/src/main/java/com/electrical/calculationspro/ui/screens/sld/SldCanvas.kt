package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNode

@Composable
fun SldCanvas(
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    connectionStartId: String?,
    engineering: SldEngineeringPackage? = null,
    onSelectNode: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onSelectConnection: (String) -> Unit,
    onEditNode: (String) -> Unit,
    onEditConnection: (String) -> Unit
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    val textMeasurer = rememberTextMeasurer()

    val canvasWidth = 3000.dp
    val canvasHeight = 1600.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8))
            .horizontalScroll(horizontalScroll)
            .verticalScroll(verticalScroll)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    Modifier.pointerInput(
                        nodes,
                        connections,
                        selectedNodeId,
                        selectedConnectionId
                    ) {
                        detectDragGestures(
                            onDragStart = { offset ->

                                val node = findNode(
                                    nodes = nodes,
                                    offset = offset
                                )

                                if (node != null) {
                                    onSelectNode(node.id)
                                    onMoveNode(
                                        node.id,
                                        offset.x,
                                        offset.y
                                    )
                                    return@detectDragGestures
                                }

                                val connection = findConnection(
                                    connections = connections,
                                    nodes = nodes,
                                    offset = offset
                                )

                                if (connection != null) {
                                    onSelectConnection(connection.id)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                val node = findNode(
                                    nodes = nodes,
                                    offset = change.position
                                )

                                if (node != null) {
                                    onMoveNode(
                                        node.id,
                                        node.x + dragAmount.x,
                                        node.y + dragAmount.y
                                    )
                                }
                            },
                            onDragEnd = {},
                            onDragCancel = {}
                        )
                    }
                )
        ) {
            drawSldEngineeringBackground(
                width = size.width,
                height = size.height
            )

            connections.forEach { connection ->

                val feederResult =
                    engineering
                        ?.cableSizing
                        ?.feeders
                        ?.get(connection.id)

                drawConnection(
                    connection = connection,
                    nodes = nodes,
                    selected = connection.id == selectedConnectionId,
                    textMeasurer = textMeasurer,
                    feederResult = feederResult
                )
            }

            nodes.forEach { node ->

                val engineeringResult =
                    engineering
                        ?.upstream
                        ?.nodeResults
                        ?.get(node.id)

                val protectionDevice =
                    engineering
                        ?.protectionCoordination
                        ?.devices
                        ?.get(node.id)

                val shortCircuitResult =
                    engineering
                        ?.shortCircuit
                        ?.results
                        ?.get(node.id)

                drawNode(
                    node = node,
                    selected = node.id == selectedNodeId,
                    connectionStart = node.id == connectionStartId,
                    textMeasurer = textMeasurer,
                    engineeringResult = engineeringResult,
                    protectionDevice = protectionDevice,
                    shortCircuitResult = shortCircuitResult
                )
            }

            drawSldTitleBlock(
                width = size.width,
                height = size.height,
                nodeCount = nodes.size,
                feederCount = connections.size,
                engineeringAvailable = engineering != null
            )
        }
    }
}

private fun findNode(
    nodes: List<SldNode>,
    offset: Offset
): SldNode? {
    return nodes
        .asSequence()
        .mapNotNull { node ->
            val left = node.x
            val top = node.y
            val right = node.x + NODE_WIDTH
            val bottom = node.y + NODE_HEIGHT

            if (
                offset.x in left..right &&
                offset.y in top..bottom
            ) {
                node
            } else {
                null
            }
        }
        .firstOrNull()
}

private fun findConnection(
    connections: List<SldConnection>,
    nodes: List<SldNode>,
    offset: Offset
): SldConnection? {

    connections.forEach { connection ->

        val from = nodes.firstOrNull {
            it.id == connection.fromNodeId
        }

        val to = nodes.firstOrNull {
            it.id == connection.toNodeId
        }

        if (from == null || to == null) {
            return@forEach
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

        val d1 = segmentDistance(
            point = offset,
            start = start,
            end = p1
        )

        val d2 = segmentDistance(
            point = offset,
            start = p1,
            end = p2
        )

        val d3 = segmentDistance(
            point = offset,
            start = p2,
            end = end
        )

        if (
            minOf(d1, d2, d3) <= 22f
        ) {
            return connection
        }
    }

    return null
}

private fun segmentDistance(
    point: Offset,
    start: Offset,
    end: Offset
): Float {

    val dx = end.x - start.x
    val dy = end.y - start.y

    if (dx == 0f && dy == 0f) {
        return distance(
            point,
            start
        )
    }

    val t = (
        (point.x - start.x) * dx +
            (point.y - start.y) * dy
        ) / (
            dx * dx +
                dy * dy
            )

    val clamped = t.coerceIn(0f, 1f)

    val projection = Offset(
        start.x + clamped * dx,
        start.y + clamped * dy
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

    val dx = a.x - b.x
    val dy = a.y - b.y

    return kotlin.math.sqrt(
        dx * dx + dy * dy
    )
}
