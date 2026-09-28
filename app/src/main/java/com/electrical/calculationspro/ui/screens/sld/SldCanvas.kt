package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CenterFocusStrong
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNode

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
    val textMeasurer =
        rememberTextMeasurer()

    val currentNodes by
        rememberUpdatedState(nodes)

    val currentConnections by
        rememberUpdatedState(connections)

    val currentSelectedNodeId by
        rememberUpdatedState(selectedNodeId)

    val currentSelectedConnectionId by
        rememberUpdatedState(selectedConnectionId)

    val currentConnectionStartId by
        rememberUpdatedState(connectionStartId)

    val currentEngineering by
        rememberUpdatedState(engineering)

    val currentOnSelectNode by
        rememberUpdatedState(onSelectNode)

    val currentOnMoveNode by
        rememberUpdatedState(onMoveNode)

    val currentOnMoveNodeEnd by
        rememberUpdatedState(onMoveNodeEnd)

    val currentOnSelectConnection by
        rememberUpdatedState(onSelectConnection)

    val currentOnEditNode by
        rememberUpdatedState(onEditNode)

    val currentOnEditConnection by
        rememberUpdatedState(onEditConnection)

    var panX by
        remember {
            mutableFloatStateOf(0f)
        }

    var panY by
        remember {
            mutableFloatStateOf(0f)
        }

    var zoom by
        remember {
            mutableFloatStateOf(1f)
        }

    fun screenToLogical(
        point: Offset
    ): Offset =
        Offset(
            (point.x - panX) / zoom,
            (point.y - panY) / zoom
        )

    fun zoomAt(
        factor: Float,
        center: Offset
    ) {
        val oldZoom = zoom

        val newZoom =
            (
                oldZoom * factor
                ).coerceIn(
                    0.25f,
                    4.0f
                )

        if (newZoom == oldZoom) {
            return
        }

        val logical =
            Offset(
                (center.x - panX) / oldZoom,
                (center.y - panY) / oldZoom
            )

        zoom = newZoom

        panX =
            center.x -
                logical.x * newZoom

        panY =
            center.y -
                logical.y * newZoom
    }

    fun resetView() {
        panX = 0f
        panY = 0f
        zoom = 1f
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
                    .pointerInput(Unit) {

                        awaitEachGesture {

                            val down =
                                awaitFirstDown(
                                    requireUnconsumed = false
                                )

                            val firstLogical =
                                screenToLogical(
                                    down.position
                                )

                            val firstNode =
                                findNode(
                                    firstLogical,
                                    currentNodes
                                )

                            var dragNodeId =
                                firstNode?.id

                            var nodeDrag =
                                firstNode != null

                            var multiTouch =
                                false

                            var moved =
                                false

                            firstNode?.let {
                                currentOnSelectNode(
                                    it.id
                                )
                            }

                            while (true) {

                                val event =
                                    awaitPointerEvent()

                                val pressed =
                                    event.changes.filter {
                                        it.pressed
                                    }

                                if (
                                    pressed.isEmpty()
                                ) {
                                    break
                                }

                                if (
                                    pressed.size >= 2
                                ) {
                                    multiTouch = true
                                    nodeDrag = false

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
                                            ).coerceIn(
                                                0.25f,
                                                4f
                                            )

                                    if (
                                        newZoom !=
                                            oldZoom
                                    ) {
                                        val logical =
                                            Offset(
                                                (
                                                    centroid.x -
                                                        panX
                                                    ) /
                                                    oldZoom,

                                                (
                                                    centroid.y -
                                                        panY
                                                    ) /
                                                    oldZoom
                                            )

                                        zoom =
                                            newZoom

                                        panX =
                                            centroid.x -
                                                logical.x *
                                                newZoom

                                        panY =
                                            centroid.y -
                                                logical.y *
                                                newZoom
                                    }

                                    panX += pan.x
                                    panY += pan.y

                                    pressed.forEach {
                                        it.consume()
                                    }

                                    continue
                                }

                                if (!multiTouch) {

                                    val change =
                                        pressed.first()

                                    val delta =
                                        change.position -
                                            change.previousPosition

                                    if (
                                        delta !=
                                            Offset.Zero
                                    ) {
                                        moved = true

                                        if (
                                            nodeDrag &&
                                                dragNodeId != null
                                        ) {
                                            currentOnMoveNode(
                                                dragNodeId!!,
                                                delta.x / zoom,
                                                delta.y / zoom
                                            )
                                        } else {
                                            panX += delta.x
                                            panY += delta.y
                                        }

                                        change.consume()
                                    }
                                }
                            }

                            if (
                                nodeDrag &&
                                    !multiTouch &&
                                    moved
                            ) {
                                currentOnMoveNodeEnd()
                            }

                            dragNodeId = null
                        }
                    }
                    .pointerInput(Unit) {

                        awaitEachGesture {

                            val down =
                                awaitFirstDown(
                                    requireUnconsumed = false
                                )

                            val logical =
                                screenToLogical(
                                    down.position
                                )

                            val node =
                                findNode(
                                    logical,
                                    currentNodes
                                )

                            if (node != null) {
                                currentOnSelectNode(
                                    node.id
                                )
                            } else {
                                val connection =
                                    findConnection(
                                        logical,
                                        currentNodes,
                                        currentConnections
                                    )

                                if (
                                    connection != null
                                ) {
                                    currentOnSelectConnection(
                                        connection.id
                                    )
                                }
                            }
                        }
                    }
        ) {

            drawSldEngineeringBackground()

            withTransform({

                translate(
                    panX,
                    panY
                )

                scale(
                    zoom,
                    zoom,
                    Offset.Zero
                )

            }) {

                currentConnections.forEach {
                    connection ->

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

                currentNodes.forEach {
                    node ->

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
                            result,

                        nodes =
                            currentNodes,

                        connections =
                            currentConnections
                    )
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .padding(12.dp),

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            FloatingActionButton(
                onClick = {
                    zoomAt(
                        1.20f,
                        Offset.Zero
                    )
                }
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Zoom In"
                )
            }

            FloatingActionButton(
                onClick = {
                    zoomAt(
                        1f / 1.20f,
                        Offset.Zero
                    )
                }
            ) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = "Zoom Out"
                )
            }

            FloatingActionButton(
                onClick = {
                    resetView()
                }
            ) {
                Icon(
                    Icons.Outlined.CenterFocusStrong,
                    contentDescription = "Reset View"
                )
            }
        }
    }
}
