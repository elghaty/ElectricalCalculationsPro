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

private const val MIN_ZOOM = 0.25f
private const val MAX_ZOOM = 4.0f
private const val DOUBLE_TAP_TIMEOUT = 350L
private const val DOUBLE_TAP_DISTANCE = 48f

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

    var panX by remember {
        mutableFloatStateOf(0f)
    }

    var panY by remember {
        mutableFloatStateOf(0f)
    }

    var zoom by remember {
        mutableFloatStateOf(1f)
    }

    fun screenToLogical(
        point: Offset
    ): Offset {
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
                .coerceIn(
                    MIN_ZOOM,
                    MAX_ZOOM
                )

        if (newZoom == oldZoom) {
            return
        }

        val logical =
            Offset(
                x = (center.x - panX) / oldZoom,
                y = (center.y - panY) / oldZoom
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

                        var lastTapTime = 0L
                        var lastTapPosition = Offset.Unspecified

                        awaitEachGesture {

                            val firstDown =
                                awaitFirstDown(
                                    requireUnconsumed = false
                                )

                            val firstPosition =
                                firstDown.position

                            val firstLogical =
                                screenToLogical(
                                    firstPosition
                                )

                            val firstNode =
                                findNode(
                                    firstLogical,
                                    currentNodes
                                )

                            val firstConnection =
                                if (firstNode == null) {
                                    findConnection(
                                        firstLogical,
                                        currentNodes,
                                        currentConnections
                                    )
                                } else {
                                    null
                                }

                            var activeNodeId =
                                firstNode?.id

                            var draggingNode =
                                firstNode != null

                            var moved =
                                false

                            var multiTouch =
                                false

                            if (firstNode != null) {
                                currentOnSelectNode(
                                    firstNode.id
                                )
                            } else if (firstConnection != null) {
                                currentOnSelectConnection(
                                    firstConnection.id
                                )
                            }

                            while (true) {

                                val event =
                                    awaitPointerEvent()

                                val pressed =
                                    event.changes.filter {
                                        it.pressed
                                    }

                                if (pressed.isEmpty()) {
                                    break
                                }

                                /*
                                 * =================================================
                                 * TWO FINGER GESTURE
                                 * =================================================
                                 */

                                if (pressed.size >= 2) {

                                    multiTouch = true
                                    draggingNode = false

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
                                                MIN_ZOOM,
                                                MAX_ZOOM
                                            )

                                    if (
                                        newZoom !=
                                            oldZoom
                                    ) {

                                        val logicalCentroid =
                                            Offset(
                                                x =
                                                    (
                                                        centroid.x -
                                                            panX
                                                        ) /
                                                        oldZoom,

                                                y =
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
                                                logicalCentroid.x *
                                                newZoom

                                        panY =
                                            centroid.y -
                                                logicalCentroid.y *
                                                newZoom
                                    }

                                    panX += pan.x
                                    panY += pan.y

                                    pressed.forEach {
                                        it.consume()
                                    }

                                    continue
                                }

                                /*
                                 * =================================================
                                 * SINGLE FINGER
                                 * =================================================
                                 */

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
                                            draggingNode &&
                                                activeNodeId != null
                                        ) {

                                            currentOnMoveNode(
                                                activeNodeId!!,
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

                            /*
                             * =====================================================
                             * GESTURE END
                             * =====================================================
                             */

                            if (
                                draggingNode &&
                                    !multiTouch &&
                                    moved
                            ) {
                                currentOnMoveNodeEnd()
                            }

                            /*
                             * =====================================================
                             * DOUBLE TAP
                             * =====================================================
                             */

                            if (
                                !moved &&
                                    !multiTouch
                            ) {

                                val now =
                                    firstDown.uptimeMillis

                                val isDoubleTap =
                                    lastTapTime > 0L &&
                                        now - lastTapTime <=
                                            DOUBLE_TAP_TIMEOUT &&
                                        lastTapPosition !=
                                            Offset.Unspecified &&
                                        (
                                            firstPosition -
                                                lastTapPosition
                                            ).getDistance() <=
                                            DOUBLE_TAP_DISTANCE

                                if (isDoubleTap) {

                                    if (firstNode != null) {

                                        currentOnEditNode(
                                            firstNode
                                        )

                                    } else if (
                                        firstConnection != null
                                    ) {

                                        currentOnEditConnection(
                                            firstConnection
                                        )
                                    }

                                    lastTapTime = 0L
                                    lastTapPosition =
                                        Offset.Unspecified

                                } else {

                                    lastTapTime = now
                                    lastTapPosition =
                                        firstPosition
                                }
                            }

                            activeNodeId = null
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

                /*
                 * =========================================================
                 * CONNECTIONS
                 * =========================================================
                 */

                currentConnections.forEach { connection ->

                    val feederResult =
                        currentEngineering
                            ?.upstream
                            ?.feeders
                            ?.firstOrNull {
                                it.connectionId ==
                                    connection.id
                            }

                    /*
                     * IMPORTANT:
                     *
                     * Pass the complete connection list.
                     * drawConnection needs it for:
                     *
                     * - panel/busbar ports
                     * - correct feeder routing
                     * - connection direction
                     * - busbar distribution
                     */

                    drawConnection(
                        connection =
                            connection,

                        nodes =
                            currentNodes,

                        connections =
                            currentConnections,

                        selected =
                            connection.id ==
                                currentSelectedConnectionId,

                        textMeasurer =
                            textMeasurer,

                        feederResult =
                            feederResult
                    )
                }

                /*
                 * =========================================================
                 * NODES
                 * =========================================================
                 */

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
                            result,

                        nodes =
                            currentNodes,

                        connections =
                            currentConnections
                    )
                }
            }
        }

        /*
         * =============================================================
         * VIEW CONTROLS
         * =============================================================
         */

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
                        factor = 1.20f,
                        center =
                            Offset(
                                x = 0f,
                                y = 0f
                            )
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
                        factor = 1f / 1.20f,
                        center =
                            Offset(
                                x = 0f,
                                y = 0f
                            )
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
