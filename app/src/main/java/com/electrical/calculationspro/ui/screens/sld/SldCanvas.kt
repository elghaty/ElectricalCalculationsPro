package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
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

    val currentNodes by rememberUpdatedState(nodes)
    val currentConnections by rememberUpdatedState(connections)
    val currentSelectedNodeId by rememberUpdatedState(selectedNodeId)
    val currentSelectedConnectionId by rememberUpdatedState(selectedConnectionId)
    val currentConnectionStartId by rememberUpdatedState(connectionStartId)
    val currentEngineering by rememberUpdatedState(engineering)

    val currentOnSelectNode by rememberUpdatedState(onSelectNode)
    val currentOnMoveNode by rememberUpdatedState(onMoveNode)
    val currentOnMoveNodeEnd by rememberUpdatedState(onMoveNodeEnd)
    val currentOnSelectConnection by rememberUpdatedState(onSelectConnection)
    val currentOnEditNode by rememberUpdatedState(onEditNode)
    val currentOnEditConnection by rememberUpdatedState(onEditConnection)

    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }

    fun screenToLogical(point: Offset): Offset {
        return Offset(
            x = (point.x - panX) / zoom,
            y = (point.y - panY) / zoom
        )
    }

    fun zoomAt(
        factor: Float,
        center: Offset
    ) {
        val oldZoom = zoom

        val newZoom =
            (oldZoom * factor)
                .coerceIn(0.25f, 4.0f)

        if (newZoom == oldZoom) {
            return
        }

        val logicalX =
            (center.x - panX) / oldZoom

        val logicalY =
            (center.y - panY) / oldZoom

        zoom = newZoom

        panX =
            center.x -
                logicalX * newZoom

        panY =
            center.y -
                logicalY * newZoom
    }

    fun resetView() {
        zoom = 1f
        panX = 0f
        panY = 0f
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF7F9FA)
                )
    ) {

        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()

                    // -------------------------------------------------
                    // TAP / DOUBLE TAP
                    // -------------------------------------------------

                    .pointerInput(Unit) {

                        androidx.compose.foundation.gestures.detectTapGestures(

                            onDoubleTap = { point ->

                                val logical =
                                    screenToLogical(point)

                                val node =
                                    findNode(
                                        logical,
                                        currentNodes
                                    )

                                if (node != null) {

                                    currentOnEditNode(
                                        node
                                    )

                                } else {

                                    findConnection(
                                        logical,
                                        currentNodes,
                                        currentConnections
                                    )?.let {

                                        currentOnEditConnection(
                                            it
                                        )
                                    }
                                }
                            },

                            onTap = { point ->

                                val logical =
                                    screenToLogical(point)

                                val node =
                                    findNode(
                                        logical,
                                        currentNodes
                                    )

                                if (node != null) {

                                    currentOnSelectNode(
                                        node.id
                                    )

                                    return@detectTapGestures
                                }

                                findConnection(
                                    logical,
                                    currentNodes,
                                    currentConnections
                                )?.let {

                                    currentOnSelectConnection(
                                        it.id
                                    )
                                }
                            }
                        )
                    }

                    // -------------------------------------------------
                    // ONE / TWO FINGER GESTURES
                    // -------------------------------------------------

                    .pointerInput(Unit) {

                        awaitEachGesture {

                            val firstDown =
                                awaitFirstDown(
                                    requireUnconsumed = false
                                )

                            var dragNodeId: String? =
                                null

                            var draggingNode =
                                false

                            var multiTouch =
                                false

                            val firstLogical =
                                screenToLogical(
                                    firstDown.position
                                )

                            val firstNode =
                                findNode(
                                    firstLogical,
                                    currentNodes
                                )

                            if (firstNode != null) {

                                dragNodeId =
                                    firstNode.id

                                draggingNode =
                                    true

                                currentOnSelectNode(
                                    firstNode.id
                                )
                            }

                            while (true) {

                                val event =
                                    awaitPointerEvent()

                                val pressedCount =
                                    event.changes.count {
                                        it.pressed
                                    }

                                if (pressedCount == 0) {
                                    break
                                }

                                // -------------------------------------
                                // TWO FINGERS = PAN + PINCH ZOOM
                                // -------------------------------------

                                if (pressedCount >= 2) {

                                    multiTouch =
                                        true

                                    draggingNode =
                                        false

                                    val centroid =
                                        event.calculateCentroid(
                                            useCurrent = false
                                        )

                                    val pan =
                                        event.calculatePan()

                                    val gestureZoom =
                                        event.calculateZoom()

                                    val oldZoom =
                                        zoom

                                    val newZoom =
                                        (
                                            oldZoom *
                                                gestureZoom
                                            )
                                            .coerceIn(
                                                0.25f,
                                                4.0f
                                            )

                                    if (
                                        newZoom !=
                                            oldZoom
                                    ) {

                                        val logicalX =
                                            (
                                                centroid.x -
                                                    panX
                                                ) /
                                                oldZoom

                                        val logicalY =
                                            (
                                                centroid.y -
                                                    panY
                                                ) /
                                                oldZoom

                                        panX =
                                            centroid.x -
                                                logicalX *
                                                newZoom

                                        panY =
                                            centroid.y -
                                                logicalY *
                                                newZoom

                                        zoom =
                                            newZoom
                                    }

                                    panX +=
                                        pan.x

                                    panY +=
                                        pan.y

                                    event.changes
                                        .forEach {
                                            it.consume()
                                        }

                                } else if (
                                    !multiTouch
                                ) {

                                    val change =
                                        event.changes
                                            .firstOrNull {
                                                it.pressed
                                            }

                                    if (change != null) {

                                        val delta =
                                            change.positionChange()

                                        if (
                                            delta !=
                                                Offset.Zero
                                        ) {

                                            if (
                                                draggingNode &&
                                                    dragNodeId !=
                                                    null
                                            ) {

                                                currentOnMoveNode(
                                                    dragNodeId!!,
                                                    delta.x / zoom,
                                                    delta.y / zoom
                                                )

                                            } else {

                                                panX +=
                                                    delta.x

                                                panY +=
                                                    delta.y
                                            }

                                            change.consume()
                                        }
                                    }
                                }
                            }

                            if (
                                draggingNode &&
                                !multiTouch
                            ) {

                                currentOnMoveNodeEnd()
                            }
                        }
                    }
        ) {

            drawSldEngineeringBackground()

            withTransform(
                {
                    translate(
                        panX,
                        panY
                    )

                    scale(
                        zoom,
                        zoom,
                        Offset.Zero
                    )
                }
            ) {

                // ---------------------------------------------
                // CONNECTIONS FIRST
                // ---------------------------------------------

                currentConnections.forEach { connection ->

                    val feederResult =
                        currentEngineering
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
                            currentNodes,

                        selected =
                            connection.id ==
                                currentSelectedConnectionId,

                        textMeasurer =
                            textMeasurer,

                        feederResult =
                            feederResult
                    )
                }

                // ---------------------------------------------
                // NODES
                // ---------------------------------------------

                currentNodes.forEach { node ->

                    val result =
                        currentEngineering
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
                                currentSelectedNodeId,

                        connectionStart =
                            node.id ==
                                currentConnectionStartId,

                        textMeasurer =
                            textMeasurer,

                        engineeringResult =
                            result
                    )
                }
            }
        }

        // =========================================================
        // ZOOM CONTROLS
        // =========================================================

        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .padding(
                        12.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            FloatingActionButton(
                onClick = {

                    zoomAt(
                        factor = 1.20f,
                        center =
                            Offset.Zero
                    )
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Add,

                    contentDescription =
                        "Zoom In"
                )
            }

            FloatingActionButton(
                onClick = {

                    zoomAt(
                        factor = 0.8333333f,
                        center =
                            Offset.Zero
                    )
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Remove,

                    contentDescription =
                        "Zoom Out"
                )
            }

            FloatingActionButton(
                onClick = {
                    resetView()
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.CenterFocusStrong,

                    contentDescription =
                        "Reset View"
                )
            }
        }
    }
}
