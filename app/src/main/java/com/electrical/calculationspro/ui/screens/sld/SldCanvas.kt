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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
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

/*
 * Stable gesture state.
 *
 * The previous implementation used nodes / pan values as
 * pointerInput keys. That can restart the gesture while the
 * node is being moved.
 *
 * rememberUpdatedState keeps the gesture detector alive while
 * always exposing the latest state and callbacks.
 */

val currentNodes =
    rememberUpdatedState(nodes)

val currentConnections =
    rememberUpdatedState(connections)

val currentSelectedNodeId =
    rememberUpdatedState(selectedNodeId)

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

val currentPanX =
    rememberUpdatedState(panX)

val currentPanY =
    rememberUpdatedState(panY)

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

                /*
                 * Tap / double tap detector.
                 *
                 * Stable key = Unit.
                 */
                .pointerInput(Unit) {

                    detectTapGestures(

                        onDoubleTap = { point ->

                            val logicalPoint =
                                Offset(
                                    x =
                                        point.x -
                                            currentPanX.value,

                                    y =
                                        point.y -
                                            currentPanY.value
                                )

                            val node =
                                findNode(
                                    point =
                                        logicalPoint,

                                    nodes =
                                        currentNodes.value
                                )

                            if (node != null) {

                                currentOnEditNode.value(
                                    node
                                )

                            } else {

                                val connection =
                                    findConnection(
                                        point =
                                            logicalPoint,

                                        nodes =
                                            currentNodes.value,

                                        connections =
                                            currentConnections.value
                                    )

                                if (connection != null) {

                                    currentOnEditConnection.value(
                                        connection
                                    )
                                }
                            }
                        },

                        onTap = { point ->

                            val logicalPoint =
                                Offset(
                                    x =
                                        point.x -
                                            currentPanX.value,

                                    y =
                                        point.y -
                                            currentPanY.value
                                )

                            val node =
                                findNode(
                                    point =
                                        logicalPoint,

                                    nodes =
                                        currentNodes.value
                                )

                            if (node != null) {

                                currentOnSelectNode.value(
                                    node.id
                                )

                            } else {

                                val connection =
                                    findConnection(
                                        point =
                                            logicalPoint,

                                        nodes =
                                            currentNodes.value,

                                        connections =
                                            currentConnections.value
                                    )

                                if (connection != null) {

                                    currentOnSelectConnection.value(
                                        connection.id
                                    )
                                }
                            }
                        }
                    )
                }

                /*
                 * Stable drag detector.
                 *
                 * Node movement:
                 * finger movement -> geometry only
                 *
                 * finger released:
                 * one save + one engineering recalculation
                 *
                 * Empty canvas:
                 * pan viewport only
                 */
                .pointerInput(Unit) {

                    var draggingNodeId:
                        String? = null

                    var draggingNode =
                        false

                    detectDragGestures(

                        onDragStart = { point ->

                            val logicalPoint =
                                Offset(
                                    x =
                                        point.x -
                                            currentPanX.value,

                                    y =
                                        point.y -
                                            currentPanY.value
                                )

                            val node =
                                findNode(
                                    point =
                                        logicalPoint,

                                    nodes =
                                        currentNodes.value
                                )

                            draggingNodeId =
                                node?.id

                            draggingNode =
                                node != null

                            if (node != null) {

                                currentOnSelectNode.value(
                                    node.id
                                )
                            }
                        },

                        onDrag = {
                                change,
                                dragAmount ->

                            change.consume()

                            val nodeId =
                                draggingNodeId

                            if (
                                draggingNode &&
                                nodeId != null
                            ) {

                                /*
                                 * IMPORTANT:
                                 *
                                 * Do not recalculate engineering
                                 * values during every pointer event.
                                 *
                                 * Only move the geometry here.
                                 */
                                currentOnMoveNode.value(
                                    nodeId,
                                    dragAmount.x,
                                    dragAmount.y
                                )

                            } else {

                                /*
                                 * Empty area drag = viewport pan.
                                 */
                                panX +=
                                    dragAmount.x

                                panY +=
                                    dragAmount.y
                            }
                        },

                        onDragEnd = {

                            if (draggingNode) {

                                /*
                                 * SldEditorScreen currently connects
                                 * this callback to:
                                 *
                                 * actions.saveAndRecalculate()
                                 *
                                 * Therefore upstream propagation
                                 * occurs once after the movement.
                                 */
                                currentOnMoveNodeEnd.value()
                            }

                            draggingNodeId =
                                null

                            draggingNode =
                                false
                        },

                        onDragCancel = {

                            if (draggingNode) {

                                currentOnMoveNodeEnd.value()
                            }

                            draggingNodeId =
                                null

                            draggingNode =
                                false
                        }
                    )
                }
    ) {

        drawSldEngineeringBackground()

        withTransform({

            translate(
                left =
                    panX,

                top =
                    panY
            )

        }) {

            /*
             * Connections are drawn first so that equipment
             * symbols remain visually above feeders.
             */
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
                    connection =
                        connection,

                    nodes =
                        currentNodes.value,

                    selected =
                        connection.id ==
                            currentSelectedConnectionId.value,

                    textMeasurer =
                        textMeasurer,

                    feederResult =
                        feederResult
                )
            }

            /*
             * Equipment / nodes.
             */
            currentNodes.value.forEach { node ->

                val engineeringResult =
                    currentEngineering
                        .value
                        ?.upstream
                        ?.nodes
                        ?.firstOrNull {
                            it.nodeId ==
                                node.id
                        }

                drawNode(
                    node =
                        node,

                    selected =
                        node.id ==
                            currentSelectedNodeId.value,

                    connectionStart =
                        node.id ==
                            currentConnectionStartId.value,

                    textMeasurer =
                        textMeasurer,

                    engineeringResult =
                        engineeringResult
                )
            }

            drawSldTitleBlock(
                textMeasurer =
                    textMeasurer,

                nodes =
                    currentNodes.value,

                connections =
                    currentConnections.value,

                engineering =
                    currentEngineering.value
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

var x =
    0f

while (x <= size.width) {

    drawLine(
        color =
            Color(0xFFE1E7EB),

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
            1f
    )

    x +=
        50f
}

var y =
    0f

while (y <= size.height) {

    drawLine(
        color =
            Color(0xFFE1E7EB),

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
            1f
    )

    y +=
        50f
}

}

private fun androidx.compose.ui.graphics.drawscope.DrawScope
.drawSldTitleBlock(
textMeasurer:
TextMeasurer,

    nodes:
        List<SldNode>,

    connections:
        List<SldConnection>,

    engineering:
        SldEngineeringPackage?
) {

val left =
    60f

val top =
    60f

drawText(
    textMeasurer =
        textMeasurer,

    text =
        "SINGLE LINE DIAGRAM",

    topLeft =
        Offset(
            left,
            top
        ),

    style =
        TextStyle(
            color =
                Color(0xFF172027),

            fontSize =
                20.sp,

            fontWeight =
                FontWeight.Bold
        )
)

drawText(
    textMeasurer =
        textMeasurer,

    text =
        "PROFESSIONAL ELECTRICAL DESIGN",

    topLeft =
        Offset(
            left,
            top + 30f
        ),

    style =
        TextStyle(
            color =
                Color(0xFF60717A),

            fontSize =
                11.sp
        )
)

drawLine(
    color =
        Color(0xFF60717A),

    start =
        Offset(
            left,
            top + 50f
        ),

    end =
        Offset(
            left + 360f,
            top + 50f
        ),

    strokeWidth =
        2f
)

drawText(
    textMeasurer =
        textMeasurer,

    text =
        "Nodes: ${nodes.size}    Feeders: ${connections.size}",

    topLeft =
        Offset(
            left,
            top + 68f
        ),

    style =
        TextStyle(
            color =
                Color(0xFF60717A),

            fontSize =
                10.sp
        )
)

drawText(
    textMeasurer =
        textMeasurer,

    text =
        if (engineering != null) {
            "ENGINEERING STUDY AVAILABLE"
        } else {
            "ENGINEERING STUDY NOT CALCULATED"
        },

    topLeft =
        Offset(
            left,
            top + 86f
        ),

    style =
        TextStyle(
            color =
                if (engineering != null) {
                    Color(0xFF1976D2)
                } else {
                    Color(0xFF996C00)
                },

            fontSize =
                10.sp,

            fontWeight =
                FontWeight.Bold
        )
)

}
