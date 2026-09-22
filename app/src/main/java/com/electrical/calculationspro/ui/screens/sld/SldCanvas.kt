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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
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

    val textMeasurer =
        androidx.compose.ui.text.rememberTextMeasurer()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .horizontalScroll(
                    rememberScrollState()
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .background(
                    Color(0xFF09131A)
                )
    ) {

        Canvas(
            modifier =
                Modifier
                    .width(2400.dp)
                    .height(1100.dp)
                    .pointerInput(nodes, connections) {

                        detectTapGestures(

                            onDoubleTap = { point ->

                                val node =
                                    findNode(
                                        point,
                                        nodes
                                    )

                                if (node != null) {

                                    onEditNode(node)

                                } else {

                                    findConnection(
                                        point,
                                        nodes,
                                        connections
                                    )?.let {
                                        onEditConnection(it)
                                    }
                                }
                            },

                            onTap = { point ->

                                val node =
                                    findNode(
                                        point,
                                        nodes
                                    )

                                if (node != null) {

                                    onSelectNode(node.id)

                                } else {

                                    findConnection(
                                        point,
                                        nodes,
                                        connections
                                    )?.let {
                                        onSelectConnection(it.id)
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
                                        point,
                                        nodes
                                    )?.id
                            },

                            onDragEnd = {
                                draggingNodeId = null
                            },

                            onDragCancel = {
                                draggingNodeId = null
                            },

                            onDrag = { change, dragAmount ->

                                draggingNodeId?.let { id ->

                                    onMoveNode(
                                        id,
                                        dragAmount.x,
                                        dragAmount.y
                                    )
                                }
                            }
                        )
                    }
        ) {

            connections.forEach { connection ->

                drawConnection(
                    connection = connection,
                    nodes = nodes,
                    selected =
                        connection.id ==
                            selectedConnectionId,
                    textMeasurer = textMeasurer
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
                    textMeasurer = textMeasurer
                )
            }
        }
    }
}
