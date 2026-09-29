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
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNode
import kotlin.math.max

private const val MIN_ZOOM = 0.25f
private const val MAX_ZOOM = 4.0f
private const val BUTTON_ZOOM_FACTOR = 1.20f

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
            x =
                (point.x - panX) /
                    zoom,

            y =
                (point.y - panY) /
                    zoom
        )
    }

    fun zoomAt(
        factor: Float,
        center: Offset
    ) {

        if (
            !factor.isFinite() ||
                factor <= 0f
        ) {
            return
        }

        val oldZoom =
            zoom

        val newZoom =
            (
                oldZoom * factor
            ).coerceIn(
                MIN_ZOOM,
                MAX_ZOOM
            )

        if (
            newZoom ==
                oldZoom
        ) {
            return
        }

        val logicalPoint =
            Offset(
                x =
                    (
                        center.x -
                            panX
                        ) /
                        oldZoom,

                y =
                    (
                        center.y -
                            panY
                        ) /
                        oldZoom
            )

        zoom =
            newZoom

        panX =
            center.x -
                logicalPoint.x *
                newZoom

        panY =
            center.y -
                logicalPoint.y *
                newZoom
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

                        var lastTapTime =
                            0L

                        var lastTapPosition =
                            Offset.Unspecified

                        awaitEachGesture {

                            val firstDown =
                                awaitFirstDown(
                                    requireUnconsumed =
                                        false
                                )

                            val firstPosition =
                                firstDown.position

                            val firstLogical =
                                screenToLogical(
                                    firstPosition
                                )

                            val firstNode =
                                findNode(
                                    point =
                                        firstLogical,
                                    nodes =
                                        currentNodes
                                )

                            val firstConnection =
                                if (
                                    firstNode ==
                                        null
                                ) {

                                    findConnection(
                                        point =
                                            firstLogical,
                                        nodes =
                                            currentNodes,
                                        connections =
                                            currentConnections
                                    )

                                } else {
                                    null
                                }

                            var activeNodeId =
                                firstNode?.id

                            /*
                             * IMPORTANT:
                             *
                             * Do not allow node dragging while
                             * a connection is being created.
                             */
                            var draggingNode =
                                firstNode != null &&
                                    currentConnectionStartId ==
                                        null

                            var moved =
                                false

                            var multiTouch =
                                false

                            if (
                                firstNode !=
                                    null
                            ) {

                                /*
                                 * This callback is now also responsible
                                 * for completing a connection when the
                                 * editor is in connection mode.
                                 */
                                currentOnSelectNode(
                                    firstNode.id
                                )

                            } else if (
                                firstConnection !=
                                    null
                            ) {

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

                                if (
                                    pressed.isEmpty()
                                ) {
                                    break
                                }

                                /*
                                 * =================================================
                                 * TWO-FINGER NAVIGATION
                                 * =================================================
                                 */

                                if (
                                    pressed.size >=
                                        2
                                ) {

                                    multiTouch =
                                        true

                                    draggingNode =
                                        false

                                    moved =
                                        true

                                    val centroid =
                                        event.calculateCentroid(
                                            useCurrent =
                                                false
                                        )

                                    val pan =
                                        event.calculatePan()

                                    val gestureZoom =
                                        event.calculateZoom()

                                    val oldZoom =
                                        zoom

                                    val safeZoom =
                                        if (
                                            gestureZoom.isFinite() &&
                                                gestureZoom >
                                                0f
                                        ) {
                                            gestureZoom
                                        } else {
                                            1f
                                        }

                                    val newZoom =
                                        (
                                            oldZoom *
                                                safeZoom
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

                                    panX +=
                                        pan.x

                                    panY +=
                                        pan.y

                                    pressed.forEach {
                                        it.consume()
                                    }

                                    continue
                                }

                                /*
                                 * =================================================
                                 * ONE-FINGER
                                 * =================================================
                                 */

                                if (
                                    !multiTouch
                                ) {

                                    val change =
                                        pressed.first()

                                    val delta =
                                        change.position -
                                            change.previousPosition

                                    if (
                                        delta !=
                                            Offset.Zero
                                    ) {

                                        moved =
                                            true

                                        if (
                                            draggingNode &&
                                                activeNodeId !=
                                                null
                                        ) {

                                            currentOnMoveNode(
                                                activeNodeId!!,
                                                delta.x /
                                                    zoom,
                                                delta.y /
                                                    zoom
                                            )

                                        } else {

                                            /*
                                             * Empty canvas =
                                             * pan.
                                             */
                                            panX +=
                                                delta.x

                                            panY +=
                                                delta.y
                                        }

                                        change.consume()
                                    }
                                }
                            }

                            if (
                                draggingNode &&
                                    !multiTouch &&
                                    moved
                            ) {

                                currentOnMoveNodeEnd()
                            }

                            /*
                             * =================================================
                             * SINGLE / DOUBLE TAP
                             * =================================================
                             */

                            if (
                                !moved &&
                                    !multiTouch
                            ) {

                                val now =
                                    firstDown.uptimeMillis

                                val isDoubleTap =
                                    lastTapTime >
                                        0L &&
                                        now -
                                            lastTapTime <=
                                            DOUBLE_TAP_TIMEOUT &&
                                        lastTapPosition !=
                                            Offset.Unspecified &&
                                        (
                                            firstPosition -
                                                lastTapPosition
                                            ).getDistance() <=
                                            DOUBLE_TAP_DISTANCE

                                if (
                                    isDoubleTap
                                ) {

                                    /*
                                     * Do not open the editor when
                                     * the user is in connection mode.
                                     */
                                    if (
                                        currentConnectionStartId ==
                                            null
                                    ) {

                                        if (
                                            firstNode !=
                                                null
                                        ) {

                                            currentOnEditNode(
                                                firstNode
                                            )

                                        } else if (
                                            firstConnection !=
                                                null
                                        ) {

                                            currentOnEditConnection(
                                                firstConnection
                                            )
                                        }
                                    }

                                    lastTapTime =
                                        0L

                                    lastTapPosition =
                                        Offset.Unspecified

                                } else {

                                    lastTapTime =
                                        now

                                    lastTapPosition =
                                        firstPosition
                                }
                            }

                            activeNodeId =
                                null
                        }
                    }
        ) {

            /*
             * =========================================================
             * ENGINEERING BACKGROUND
             * =========================================================
             */

            drawSldEngineeringBackground()

            withTransform({

                translate(
                    left = panX,
                    top = panY
                )

                scale(
                    scaleX = zoom,
                    scaleY = zoom,
                    pivot =
                        Offset.Zero
                )
            }) {

                /*
                 * Connections first.
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
                 * Nodes second.
                 */
                currentNodes.forEach { node ->

                    val engineeringResult =
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
                            engineeringResult,

                        nodes =
                            currentNodes,

                        connections =
                            currentConnections
                    )
                }

                /*
                 * =====================================================
                 * CONNECTION START GUIDE
                 * =====================================================
                 *
                 * Draw a small crosshair/guide around the selected
                 * starting element. This gives the engineer a clear
                 * visual indication that the editor is waiting for
                 * the destination.
                 */
                currentConnectionStartId
                    ?.let { startId ->

                        currentNodes
                            .firstOrNull {
                                it.id ==
                                    startId
                            }
                            ?.let { node ->

                                val center =
                                    Offset(
                                        node.x +
                                            90f,

                                        node.y +
                                            59f
                                    )

                                drawCircle(
                                    color =
                                        Color(
                                            0xFFFF9800
                                        ),

                                    radius =
                                        92f,

                                    center =
                                        center,

                                    style =
                                        androidx.compose.ui.graphics.drawscope.Stroke(
                                            width =
                                                3f
                                        )
                                )
                            }
                    }
            }
        }

        /*
         * ============================================================
         * FIXED VIEW CONTROLS
         * ============================================================
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

                    /*
                     * Use the center of the visible viewport.
                     * The Canvas is behind the controls and fills
                     * the parent, so a normalized center is sufficient.
                     */
                    zoomAt(
                        factor =
                            BUTTON_ZOOM_FACTOR,

                        center =
                            Offset(
                                x = 500f,
                                y = 350f
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
                        factor =
                            1f /
                                BUTTON_ZOOM_FACTOR,

                        center =
                            Offset(
                                x = 500f,
                                y = 350f
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
