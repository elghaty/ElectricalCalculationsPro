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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldNode

/**

* ================================================================
* PROFESSIONAL SLD CANVAS
* ================================================================
* 
* Rendering is delegated to SldCanvasDrawing.kt.
* 
* Responsibilities:
* 
* - Canvas viewport
* - scrolling
* - node selection
* - connection selection
* - node dragging
* - double-click editing
* 
* Engineering calculations are NOT implemented here.
* 
* Coordinates are consistent with SldAutoLayoutEngine:
* 
* node.x = left
* node.y = top
* 
* ================================================================
  */

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

val horizontalScroll =
    androidx.compose.foundation.rememberScrollState()

val verticalScroll =
    androidx.compose.foundation.rememberScrollState()

val textMeasurer =
    androidx.compose.ui.text.rememberTextMeasurer()

Box(
    modifier =
        Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF4F7F9)
            )
            .horizontalScroll(
                horizontalScroll
            )
            .verticalScroll(
                verticalScroll
            )
) {

    Canvas(
        modifier =
            Modifier
                .width(3200.dp)
                .height(2000.dp)
                .pointerInput(
                    nodes,
                    connections,
                    selectedNodeId,
                    selectedConnectionId,
                    connectionStartId
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

                                onEditConnection(
                                    connection
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
                                    point = point,
                                    nodes = nodes
                                )?.id
                        },

                        onDrag = {
                            change,
                            dragAmount ->

                            change.consume()

                            val id =
                                draggingNodeId
                                    ?: return@detectDragGestures

                            onMoveNode(
                                id,
                                dragAmount.x,
                                dragAmount.y
                            )
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

        /*
         * ----------------------------------------------------
         * CONNECTIONS
         * ----------------------------------------------------
         */

        connections.forEach { connection ->

            drawConnection(
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

        /*
         * ----------------------------------------------------
         * NODES
         * ----------------------------------------------------
         */

        nodes.forEach { node ->

            drawNode(
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
