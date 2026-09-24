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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNode

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
val textMeasurer = rememberTextMeasurer()

val horizontalScrollState =
    rememberScrollState()

val verticalScrollState =
    rememberScrollState()

Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            Color(0xFFF7F9FA)
        )
        .horizontalScroll(
            horizontalScrollState
        )
        .verticalScroll(
            verticalScrollState
        )
) {
    Canvas(
        modifier = Modifier
            .width(3000.dp)
            .height(1800.dp)
            .pointerInput(
                nodes,
                connections
            ) {
                detectTapGestures(
                    onTap = { point ->

                        val node =
                            findNode(
                                point = point,
                                nodes = nodes
                            )

                        if (node != null) {
                            onSelectNode(
                                node.id
                            )
                            return@detectTapGestures
                        }

                        val connection =
                            findConnection(
                                point = point,
                                nodes = nodes,
                                connections = connections
                            )

                        if (connection != null) {
                            onSelectConnection(
                                connection.id
                            )
                        }
                    },

                    onDoubleTap = { point ->

                        val node =
                            findNode(
                                point = point,
                                nodes = nodes
                            )

                        if (node != null) {
                            onSelectNode(
                                node.id
                            )
                            onEditNode(
                                node
                            )
                            return@detectTapGestures
                        }

                        val connection =
                            findConnection(
                                point = point,
                                nodes = nodes,
                                connections = connections
                            )

                        if (connection != null) {
                            onSelectConnection(
                                connection.id
                            )
                            onEditConnection(
                                connection
                            )
                        }
                    }
                )
            }
            .pointerInput(nodes) {

                var draggingNodeId: String? =
                    null

                detectDragGestures(

                    onDragStart = { point ->

                        draggingNodeId =
                            findNode(
                                point = point,
                                nodes = nodes
                            )?.id
                    },

                    onDrag = { change, dragAmount ->

                        change.consume()

                        val nodeId =
                            draggingNodeId
                                ?: return@detectDragGestures

                        onMoveNode(
                            nodeId,
                            dragAmount.x,
                            dragAmount.y
                        )
                    },

                    onDragEnd = {
                        draggingNodeId = null
                    },

                    onDragCancel = {
                        draggingNodeId = null
                    }
                )
            }
    ) {

        drawGrid()

        connections.forEach { connection ->

            drawConnection(
                connection = connection,
                nodes = nodes,
                selected =
                    connection.id ==
                        selectedConnectionId,
                textMeasurer =
                    textMeasurer
            )
        }

        nodes.forEach { node ->

            drawNode(
                node = node,
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
