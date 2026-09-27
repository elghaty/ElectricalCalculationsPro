package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNode
import kotlin.math.max
import kotlin.math.min

@Composable
fun SldCanvas(
    modifier: Modifier = Modifier,
    nodes: List<SldNode>,
    connections: List<SldConnection>,
    selectedNodeId: String?,
    selectedConnectionId: String?,
    connectionStartId: String?,
    engineering: SldEngineeringPackage? = null,
    onSelectNode: (String) -> Unit,
    onMoveNode: (String, Float, Float) -> Unit,
    onMoveNodeEnd: () -> Unit,
    onSelectConnection: (String) -> Unit,
    onEditNode: (SldNode) -> Unit,
    onEditConnection: (SldConnection) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    val currentNodes = rememberUpdatedState(nodes)
    val currentConnections = rememberUpdatedState(connections)
    val currentSelectedNodeId = rememberUpdatedState(selectedNodeId)
    val currentSelectedConnectionId =
        rememberUpdatedState(selectedConnectionId)
    val currentConnectionStartId =
        rememberUpdatedState(connectionStartId)
    val currentEngineering =
        rememberUpdatedState(engineering)

    val currentOnSelectNode =
        rememberUpdatedState(onSelectNode)
    val currentOnMoveNode =
        rememberUpdatedState(onMoveNode)
    val currentOnMoveNodeEnd =
        rememberUpdatedState(onMoveNodeEnd)
    val currentOnSelectConnection =
        rememberUpdatedState(onSelectConnection)
    val currentOnEditNode =
        rememberUpdatedState(onEditNode)
    val currentOnEditConnection =
        rememberUpdatedState(onEditConnection)

    var panX by remember {
        mutableFloatStateOf(0f)
    }

    var panY by remember {
        mutableFloatStateOf(0f)
    }

    var zoom by remember {
        mutableFloatStateOf(1f)
    }

    var fitted by remember {
        mutableStateOf(false)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF6F8FA)
                )
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { point ->
                                val logical =
                                    screenToLogical(
                                        point = point,
                                        panX = panX,
                                        panY = panY,
                                        zoom = zoom
                                    )

                                val node =
                                    findNode(
                                        point = logical,
                                        nodes = currentNodes.value
                                    )

                                if (node != null) {
                                    currentOnEditNode.value(node)
                                } else {
                                    val connection =
                                        findConnection(
                                            point = logical,
                                            nodes =
                                                currentNodes.value,
                                            connections =
                                                currentConnections.value
                                        )

                                    if (connection != null) {
                                        currentOnEditConnection
                                            .value(connection)
                                    }
                                }
                            },

                            onTap = { point ->
                                val logical =
                                    screenToLogical(
                                        point = point,
                                        panX = panX,
                                        panY = panY,
                                        zoom = zoom
                                    )

                                val node =
                                    findNode(
                                        point = logical,
                                        nodes = currentNodes.value
                                    )

                                if (node != null) {
                                    currentOnSelectNode
                                        .value(node.id)
                                } else {
                                    val connection =
                                        findConnection(
                                            point = logical,
                                            nodes =
                                                currentNodes.value,
                                            connections =
                                                currentConnections.value
                                        )

                                    if (connection != null) {
                                        currentOnSelectConnection
                                            .value(connection.id)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        var draggingNodeId: String? = null
                        var draggingNode = false

                        detectDragGestures(
                            onDragStart = { point ->
                                val logical =
                                    screenToLogical(
                                        point = point,
                                        panX = panX,
                                        panY = panY,
                                        zoom = zoom
                                    )

                                val node =
                                    findNode(
                                        point = logical,
                                        nodes = currentNodes.value
                                    )

                                draggingNodeId =
                                    node?.id

                                draggingNode =
                                    node != null

                                if (node != null) {
                                    currentOnSelectNode
                                        .value(node.id)
                                }
                            },

                            onDrag = { change, amount ->
                                change.consume()

                                val nodeId =
                                    draggingNodeId

                                if (
                                    draggingNode &&
                                    nodeId != null
                                ) {
                                    currentOnMoveNode.value(
                                        nodeId,
                                        amount.x / zoom,
                                        amount.y / zoom
                                    )
                                } else {
                                    panX += amount.x
                                    panY += amount.y
                                }
                            },

                            onDragEnd = {
                                if (draggingNode) {
                                    currentOnMoveNodeEnd.value()
                                }

                                draggingNodeId = null
                                draggingNode = false
                            },

                            onDragCancel = {
                                draggingNodeId = null
                                draggingNode = false
                            }
                        )
                    }
        ) {
            drawSldEngineeringBackground()

            if (
                !fitted &&
                currentNodes.value.isNotEmpty()
            ) {
                val bounds =
                    calculateNodeBounds(
                        currentNodes.value
                    )

                val contentWidth =
                    max(
                        bounds.width,
                        NODE_WIDTH
                    )

                val contentHeight =
                    max(
                        bounds.height,
                        NODE_HEIGHT
                    )

                val availableWidth =
                    size.width - 80f

                val availableHeight =
                    size.height - 80f

                val fittedZoom =
                    min(
                        availableWidth / contentWidth,
                        availableHeight / contentHeight
                    ).coerceIn(
                        0.45f,
                        1.0f
                    )

                zoom = fittedZoom

                panX =
                    size.width / 2f -
                        (
                            bounds.left +
                                bounds.right
                            ) /
                            2f *
                            zoom

                panY =
                    size.height / 2f -
                        (
                            bounds.top +
                                bounds.bottom
                            ) /
                            2f *
                            zoom

                fitted = true
            }

            withTransform({
                translate(
                    left = panX,
                    top = panY
                )

                scale(
                    scaleX = zoom,
                    scaleY = zoom,
                    pivot = Offset.Zero
                )
            }) {
                currentConnections.value.forEach { connection ->
                    val feederResult =
                        currentEngineering
                            .value
                            ?.upstream
                            ?.feeders
                            ?.firstOrNull {
                                it.connectionId ==
                                    connection.id
                            }

                    drawConnection(
                        connection = connection,
                        nodes = currentNodes.value,
                        selected =
                            connection.id ==
                                currentSelectedConnectionId
                                    .value,
                        textMeasurer = textMeasurer,
                        feederResult = feederResult
                    )
                }

                currentNodes.value.forEach { node ->
                    val engineeringResult =
                        currentEngineering
                            .value
                            ?.upstream
                            ?.nodes
                            ?.firstOrNull {
                                it.nodeId == node.id
                            }

                    drawNode(
                        node = node,
                        selected =
                            node.id ==
                                currentSelectedNodeId.value,
                        connectionStart =
                            node.id ==
                                currentConnectionStartId.value,
                        textMeasurer = textMeasurer,
                        engineeringResult =
                            engineeringResult
                    )
                }
            }
        }
    }
}

private data class NodeBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top
}

private fun calculateNodeBounds(
    nodes: List<SldNode>
): NodeBounds {
    val left =
        nodes.minOf { it.x }

    val top =
        nodes.minOf { it.y }

    val right =
        nodes.maxOf {
            it.x + NODE_WIDTH
        }

    val bottom =
        nodes.maxOf {
            it.y +
                NODE_HEIGHT +
                55f
        }

    return NodeBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}

private fun screenToLogical(
    point: Offset,
    panX: Float,
    panY: Float,
    zoom: Float
): Offset {
    return Offset(
        x = (point.x - panX) / zoom,
        y = (point.y - panY) / zoom
    )
}
