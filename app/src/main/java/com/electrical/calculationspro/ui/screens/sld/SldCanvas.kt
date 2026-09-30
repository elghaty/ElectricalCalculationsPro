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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldEngineeringPackage
import com.electrical.calculationspro.data.SldNode
import kotlin.math.abs

private const val MIN_ZOOM = 0.25f
private const val MAX_ZOOM = 4.0f
private const val BUTTON_ZOOM_FACTOR = 1.20f

private const val DOUBLE_TAP_TIMEOUT = 350L
private const val DOUBLE_TAP_DISTANCE = 48f

private const val NODE_CENTER_X = 90f
private const val NODE_CENTER_Y = 59f

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

/*
 * ================================================================
 * VIEW STATE
 * ================================================================
 */

var panX by remember {
    mutableFloatStateOf(0f)
}

var panY by remember {
    mutableFloatStateOf(0f)
}

var zoom by remember {
    mutableFloatStateOf(1f)
}

/*
 * ================================================================
 * COORDINATE CONVERSION
 * ================================================================
 */

fun screenToLogical(
    point: Offset
): Offset {

    return Offset(
        x =
            (
                point.x -
                    panX
                ) /
                zoom,

        y =
            (
                point.y -
                    panY
                ) /
                zoom
    )
}

fun logicalToScreen(
    point: Offset
): Offset {

    return Offset(
        x =
            point.x *
                zoom +
                panX,

        y =
            point.y *
                zoom +
                panY
    )
}

/*
 * ================================================================
 * ZOOM AROUND FINGER / CURSOR
 * ================================================================
 */

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
            oldZoom *
                factor
            ).coerceIn(
                MIN_ZOOM,
                MAX_ZOOM
            )

    if (
        abs(
            newZoom -
                oldZoom
        ) < 0.0001f
    ) {
        return
    }

    /*
     * Keep the logical point beneath the user's finger
     * stationary on screen.
     */
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

    zoom =
        1f

    panX =
        0f

    panY =
        0f
}

/*
 * ================================================================
 * CANVAS
 * ================================================================
 */

Box(
    modifier =
        modifier
            .fillMaxSize()
            .background(
                Color(
                    0xFFF7F9FA
                )
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

                        /*
                         * ==================================================
                         * FIRST TOUCH
                         * ==================================================
                         */

                        val firstDown =
                            awaitFirstDown(
                                pass =
                                    PointerEventPass.Main,

                                requireUnconsumed =
                                    false
                            )

                        val firstPosition =
                            firstDown.position

                        val firstLogicalPosition =
                            screenToLogical(
                                firstPosition
                            )

                        val firstNode =
                            findNode(
                                point =
                                    firstLogicalPosition,

                                nodes =
                                    currentNodes
                            )

                        val firstConnection =
                            if (
                                firstNode == null
                            ) {

                                findConnection(
                                    point =
                                        firstLogicalPosition,

                                    nodes =
                                        currentNodes,

                                    connections =
                                        currentConnections
                                )

                            } else {
                                null
                            }

                        /*
                         * ==================================================
                         * GESTURE STATE
                         * ==================================================
                         */

                        var activeNodeId =
                            firstNode?.id

                        var draggingNode =
                            firstNode != null &&
                                currentConnectionStartId ==
                                    null

                        var moved =
                            false

                        var multiTouch =
                            false

                        var pointerCount =
                            1

                        /*
                         * Distance threshold prevents an accidental
                         * one-pixel movement from becoming a drag.
                         */
                        var dragStarted =
                            false

                        var accumulatedDrag =
                            Offset.Zero

                        /*
                         * Select immediately.
                         */
                        if (
                            firstNode != null
                        ) {

                            currentOnSelectNode(
                                firstNode.id
                            )

                        } else if (
                            firstConnection != null
                        ) {

                            currentOnSelectConnection(
                                firstConnection.id
                            )
                        }

                        /*
                         * ==================================================
                         * EVENT LOOP
                         * ==================================================
                         */

                        while (true) {

                            val event =
                                awaitPointerEvent(
                                    pass =
                                        PointerEventPass.Main
                                )

                            val pressed =
                                event.changes
                                    .filter {
                                        it.pressed
                                    }

                            pointerCount =
                                pressed.size

                            /*
                             * All pointers released.
                             */
                            if (
                                pressed.isEmpty()
                            ) {
                                break
                            }

                            /*
                             * ==================================================
                             * TWO OR MORE FINGERS
                             * ==================================================
                             */

                            if (
                                pressed.size >= 2
                            ) {

                                multiTouch =
                                    true

                                /*
                                 * Once a second finger enters,
                                 * never continue dragging the node
                                 * during this gesture.
                                 */
                                draggingNode =
                                    false

                                dragStarted =
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

                                /*
                                 * Zoom around the actual centroid.
                                 */
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

                                val oldZoom =
                                    zoom

                                val newZoom =
                                    (
                                        oldZoom *
                                            safeZoom
                                        ).coerceIn(
                                            MIN_ZOOM,
                                            MAX_ZOOM
                                        )

                                if (
                                    abs(
                                        newZoom -
                                            oldZoom
                                    ) > 0.0001f
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

                                /*
                                 * Two-finger translation.
                                 */
                                if (
                                    pan !=
                                        Offset.Zero
                                ) {

                                    panX +=
                                        pan.x

                                    panY +=
                                        pan.y
                                }

                                pressed.forEach {
                                    it.consume()
                                }

                                continue
                            }

                            /*
                             * ==================================================
                             * ONE FINGER
                             * ==================================================
                             */

                            if (
                                pressed.size == 1 &&
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

                                    accumulatedDrag +=
                                        delta

                                    /*
                                     * Ignore tiny finger jitter.
                                     */
                                    val distance =
                                        accumulatedDrag
                                            .getDistance()

                                    if (
                                        distance >
                                            5f
                                    ) {

                                        moved =
                                            true

                                        if (
                                            draggingNode &&
                                                activeNodeId !=
                                                null
                                        ) {

                                            dragStarted =
                                                true

                                            /*
                                             * Convert screen movement
                                             * into logical SLD movement.
                                             */
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
                                             * pan the drawing.
                                             */
                                            panX +=
                                                delta.x

                                            panY +=
                                                delta.y
                                        }
                                    }

                                    change.consume()
                                }
                            }
                        }

                        /*
                         * ==================================================
                         * END NODE DRAG
                         * ==================================================
                         */

                        if (
                            dragStarted &&
                                !multiTouch &&
                                activeNodeId != null
                        ) {

                            currentOnMoveNodeEnd()
                        }

                        /*
                         * ==================================================
                         * TAP / DOUBLE TAP
                         * ==================================================
                         */

                        if (
                            !moved &&
                                !multiTouch &&
                                pointerCount <= 1
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

                        /*
                         * Prevent stale node references.
                         */
                        activeNodeId =
                            null
                    }
                }
    ) {

        /*
         * ============================================================
         * ENGINEERING BACKGROUND
         * ============================================================
         */

        drawSldEngineeringBackground()

        /*
         * ============================================================
         * WORLD TRANSFORM
         * ============================================================
         */

        withTransform({

            translate(
                left =
                    panX,

                top =
                    panY
            )

            scale(
                scaleX =
                    zoom,

                scaleY =
                    zoom,

                pivot =
                    Offset.Zero
            )

        }) {

            /*
             * ========================================================
             * CONNECTIONS
             * ========================================================
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
             * ========================================================
             * NODES
             * ========================================================
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
             * ========================================================
             * CONNECTION START INDICATOR
             * ========================================================
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
                                    x =
                                        node.x +
                                            NODE_CENTER_X,

                                    y =
                                        node.y +
                                            NODE_CENTER_Y
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
                                    androidx.compose.ui.graphics
                                        .drawscope.Stroke(
                                            width =
                                                3f
                                        )
                            )
                        }
                }
        }
    }

    /*
     * ================================================================
     * VIEW CONTROLS
     * ================================================================
     */

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

        /*
         * ZOOM IN
         */
        FloatingActionButton(
            onClick = {

                /*
                 * Zoom around the center of the visible canvas.
                 * The actual pinch gesture always zooms around
                 * the fingers.
                 */
                zoomAt(
                    factor =
                        BUTTON_ZOOM_FACTOR,

                    center =
                        Offset(
                            x =
                                500f,

                            y =
                                350f
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

        /*
         * ZOOM OUT
         */
        FloatingActionButton(
            onClick = {

                zoomAt(
                    factor =
                        1f /
                            BUTTON_ZOOM_FACTOR,

                    center =
                        Offset(
                            x =
                                500f,

                            y =
                                350f
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

        /*
         * RESET VIEW
         */
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
