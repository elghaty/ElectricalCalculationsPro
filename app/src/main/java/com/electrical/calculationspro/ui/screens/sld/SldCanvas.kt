package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNode

private const val CANVAS_WIDTH_DP = 3000
private const val CANVAS_HEIGHT_DP = 1600

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
    onEditNode: (SldNode) -> Unit,
    onEditConnection: (SldConnection) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7F9))
            .horizontalScroll(horizontalScrollState)
            .verticalScroll(verticalScrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(CANVAS_WIDTH_DP.dp)
                .height(CANVAS_HEIGHT_DP.dp)
                .pointerInput(nodes, connections) {

                    detectTapGestures(
                        onDoubleTap = { point ->

                            val node = findNode(
                                point = point,
                                nodes = nodes
                            )

                            if (node != null) {
                                onEditNode(node)
                            } else {
                                val connection = findConnection(
                                    point = point,
                                    nodes = nodes,
                                    connections = connections
                                )

                                if (connection != null) {
                                    onEditConnection(connection)
                                }
                            }
                        },

                        onTap = { point ->

                            val node = findNode(
                                point = point,
                                nodes = nodes
                            )

                            if (node != null) {
                                onSelectNode(node.id)
                            } else {

                                val connection = findConnection(
                                    point = point,
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
                .pointerInput(nodes) {

                    var draggingNodeId: String? = null

                    detectDragGestures(

                        onDragStart = { point ->
                            draggingNodeId =
                                findNode(
                                    point = point,
                                    nodes = nodes
                                )?.id
                        },

                        onDragEnd = {
                            draggingNodeId = null
                        },

                        onDragCancel = {
                            draggingNodeId = null
                        },

                        onDrag = { _, dragAmount ->

                            val id =
                                draggingNodeId
                                    ?: return@detectDragGestures

                            onMoveNode(
                                id,
                                dragAmount.x,
                                dragAmount.y
                            )
                        }
                    )
                }
        ) {

            drawSldEngineeringBackground()

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
                        connection.id == selectedConnectionId,
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
                        node.id == selectedNodeId,
                    connectionStart =
                        node.id == connectionStartId,
                    textMeasurer = textMeasurer,
                    engineeringResult = engineeringResult
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope
    .drawSldEngineeringBackground() {

    drawRect(
        color = Color(0xFFF4F7F9)
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
        textMeasurer: androidx.compose.ui.text.TextMeasurer,
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
        style = TextStyle(
            color = Color(0xFF172027),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = "PROFESSIONAL ELECTRICAL DESIGN",
        topLeft = Offset(left, top + 30f),
        style = TextStyle(
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
        style = TextStyle(
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
        style = TextStyle(
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
