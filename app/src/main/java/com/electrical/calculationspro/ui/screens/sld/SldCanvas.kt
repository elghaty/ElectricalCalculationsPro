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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    onMoveNodeEnd: () -> Unit,
    onSelectConnection: (String) -> Unit,
    onEditNode: (SldNode) -> Unit,
    onEditConnection: (SldConnection) -> Unit
) {

    val textMeasurer =
        rememberTextMeasurer()

    var panX by remember {
        mutableFloatStateOf(0f)
    }

    var panY by remember {
        mutableFloatStateOf(0f)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF4F7F9)
                )
    ) {

        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(
                        nodes,
                        connections,
                        panX,
                        panY
                    ) {

                        detectTapGestures(

                            onDoubleTap = { point ->

                                val logicalPoint =
                                    Offset(
                                        x =
                                            point.x - panX,
                                        y =
                                            point.y - panY
                                    )

                                val node =
                                    findNode(
                                        point = logicalPoint,
                                        nodes = nodes
                                    )

                                if (node != null) {

                                    onEditNode(node)

                                } else {

                                    val connection =
                                        findConnection(
                                            point = logicalPoint,
                                            nodes = nodes,
                                            connections = connections
                                        )

                                    if (connection != null) {
                                        onEditConnection(connection)
                                    }
                                }
                            },

                            onTap = { point ->

                                val logicalPoint =
                                    Offset(
                                        x =
                                            point.x - panX,
                                        y =
                                            point.y - panY
                                    )

                                val node =
                                    findNode(
                                        point = logicalPoint,
                                        nodes = nodes
                                    )

                                if (node != null) {

                                    onSelectNode(node.id)

                                } else {

                                    val connection =
                                        findConnection(
                                            point = logicalPoint,
                                            nodes = nodes,
                                            connections = connections
                                        )

                                    if (connection != null) {
                                        onSelectConnection(connection.id)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(
                        nodes,
                        panX,
                        panY
                    ) {

                        var draggingNodeId: String? = null
                        var draggingNode = false

                        detectDragGestures(

                            onDragStart = { point ->

                                val logicalPoint =
                                    Offset(
                                        x =
                                            point.x - panX,
                                        y =
                                            point.y - panY
                                    )

                                draggingNodeId =
                                    findNode(
                                        point = logicalPoint,
                                        nodes = nodes
                                    )?.id

                                draggingNode =
                                    draggingNodeId != null

                                if (draggingNode) {

                                    draggingNodeId?.let {
                                        onSelectNode(it)
                                    }
                                }
                            },

                            onDragEnd = {

                                if (draggingNode) {
                                    onMoveNodeEnd()
                                }

                                draggingNodeId = null
                                draggingNode = false
                            },

                            onDragCancel = {

                                if (draggingNode) {
                                    onMoveNodeEnd()
                                }

                                draggingNodeId = null
                                draggingNode = false
                            },

                            onDrag = {
                                    change,
                                    dragAmount ->

                                change.consume()

                                val nodeId =
                                    draggingNodeId

                                if (
                                    nodeId != null &&
                                    draggingNode
                                ) {

                                    onMoveNode(
                                        nodeId,
                                        dragAmount.x,
                                        dragAmount.y
                                    )

                                } else {

                                    panX += dragAmount.x
                                    panY += dragAmount.y
                                }
                            }
                        )
                    }
        ) {

            drawSldEngineeringBackground()

            withTransform({

                translate(
                    left = panX,
                    top = panY
                )

            }) {

                connections.forEach { connection ->

                    val feederResult =
                        engineering
                            ?.upstream
                            ?.feeders
                            ?.firstOrNull {
                                it.connectionId == connection.id
                            }

                    drawConnection(
                        connection = connection,
                        nodes = nodes,
                        selected =
                            connection.id ==
                                selectedConnectionId,
                        textMeasurer = textMeasurer,
                        feederResult = feederResult
                    )
                }

                nodes.forEach { node ->

                    val engineeringResult =
                        engineering
                            ?.upstream
                            ?.nodes
                            ?.firstOrNull {
                                it.nodeId == node.id
                            }

                    drawNode(
                        node = node,
                        selected =
                            node.id ==
                                selectedNodeId,
                        connectionStart =
                            node.id ==
                                connectionStartId,
                        textMeasurer = textMeasurer,
                        engineeringResult =
                            engineeringResult
                    )
                }

                drawSldTitleBlock(
                    textMeasurer = textMeasurer,
                    nodes = nodes,
                    connections = connections,
                    engineering = engineering
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawSldEngineeringBackground() {

    drawRect(
        color =
            Color(0xFFF4F7F9)
    )

    var x = 0f

    while (x <= size.width) {

        drawLine(
            color = Color(0xFFE1E7EB),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )

        x += 50f
    }

    var y = 0f

    while (y <= size.height) {

        drawLine(
            color = Color(0xFFE1E7EB),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )

        y += 50f
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawSldTitleBlock(
        textMeasurer:
            androidx.compose.ui.text.TextMeasurer,
        nodes: List<SldNode>,
        connections: List<SldConnection>,
        engineering: SldEngineeringPackage?
    ) {

    val left = 60f
    val top = 60f

    drawText(
        textMeasurer = textMeasurer,
        text = "SINGLE LINE DIAGRAM",
        topLeft = Offset(left, top),
        style =
            TextStyle(
                color = Color(0xFF172027),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = "PROFESSIONAL ELECTRICAL DESIGN",
        topLeft = Offset(left, top + 30f),
        style =
            TextStyle(
                color = Color(0xFF60717A),
                fontSize = 11.sp
            )
    )

    drawLine(
        color = Color(0xFF60717A),
        start = Offset(left, top + 50f),
        end = Offset(left + 360f, top + 50f),
        strokeWidth = 2f
    )

    drawText(
        textMeasurer = textMeasurer,
        text =
            "Nodes: ${nodes.size}    Feeders: ${connections.size}",
        topLeft = Offset(left, top + 68f),
        style =
            TextStyle(
                color = Color(0xFF60717A),
                fontSize = 10.sp
            )
    )

    drawText(
        textMeasurer = textMeasurer,
        text =
            if (engineering != null) {
                "ENGINEERING STUDY AVAILABLE"
            } else {
                "ENGINEERING STUDY NOT CALCULATED"
            },
        topLeft = Offset(left, top + 86f),
        style =
            TextStyle(
                color =
                    if (engineering != null) {
                        Color(0xFF1976D2)
                    } else {
                        Color(0xFF996C00)
                    },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
    )
}
