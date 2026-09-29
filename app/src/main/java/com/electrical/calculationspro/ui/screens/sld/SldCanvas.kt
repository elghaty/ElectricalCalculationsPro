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
    val textMeasurer = rememberTextMeasurer()

    /*
     * Keep callbacks and data current without forcing the gesture detector
     * to restart every time the editor state changes.
     */
    val currentNodes by rememberUpdatedState(nodes)
    val currentConnections by rememberUpdatedState(connections)

    val currentSelectedNodeId by rememberUpdatedState(
        selectedNodeId
    )

    val currentSelectedConnectionId by rememberUpdatedState(
        selectedConnectionId
    )

    val currentConnectionStartId by rememberUpdatedState(
        connectionStartId
    )

    val currentEngineering by rememberUpdatedState(
        engineering
    )

    val currentOnSelectNode by rememberUpdatedState(
        onSelectNode
    )

    val currentOnMoveNode by rememberUpdatedState(
        onMoveNode
    )

    val currentOnMoveNodeEnd by rememberUpdatedState(
        onMoveNodeEnd
    )

    val currentOnSelectConnection by rememberUpdatedState(
        onSelectConnection
    )

    val currentOnEditNode by rememberUpdatedState(
        onEditNode
    )

    val currentOnEditConnection by rememberUpdatedState(
        onEditConnection
    )

    /*
     * View transformation.
     *
     * panX / panY are screen-space offsets.
     * Node coordinates remain in logical SLD coordinates.
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
     * Converts screen coordinates into the logical SLD coordinate system.
     */
    fun screenToLogical(
        point: Offset
    ): Offset {
        return Offset(
            x = (point.x - panX) / zoom,
            y = (point.y - panY) / zoom
        )
    }

    /*
     * Zoom around a specific screen point.
     *
     * This is important for professional SLD navigation:
     * when the engineer pinches or presses +/- the drawing should remain
     * visually anchored instead of jumping toward the origin.
     */
    fun zoomAt(
        factor: Float,
        center: Offset
    ) {
        if (!factor.isFinite() || factor <= 0f) {
            return
        }

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

        val logicalPoint =
            Offset(
                x = (center.x - panX) / oldZoom,
                y = (center.y - panY) / oldZoom
            )

        zoom = newZoom

        panX =
            center.x -
                logicalPoint.x * newZoom

        panY =
            center.y -
                logicalPoint.y * newZoom
    }

    /*
     * Reset the viewport only.
     *
     * It does NOT change the SLD nodes or connections.
     */
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

                        var lastTapPosition =
                            Offset.Unspecified

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

                            /*
                             * Node has priority over connection.
                             * This prevents selecting a feeder underneath a node.
                             */
                            val firstNode =
                                findNode(
                                    point = firstLogical,
                                    nodes = currentNodes
                                )

                            val firstConnection =
                                if (firstNode == null) {
                                    findConnection(
                                        point = firstLogical,
                                        nodes = currentNodes,
                                        connections = currentConnections
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

                            } else if (
                                firstConnection != null
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

                                /*
                                 * All pointers released.
                                 */
                                if (pressed.isEmpty()) {
                                    break
                                }

                                /*
                                 * Two or more pointers:
                                 *
                                 * - disable node dragging
                                 * - calculate pinch zoom
                                 * - calculate pan
                                 * - keep the pinch centroid anchored
                                 */
                                if (pressed.size >= 2) {

                                    multiTouch = true
                                    draggingNode = false
                                    moved = true

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

                                    val safeGestureZoom =
                                        if (
                                            gestureZoom.isFinite() &&
                                                gestureZoom > 0f
                                        ) {
                                            gestureZoom
                                        } else {
                                            1f
                                        }

                                    val newZoom =
                                        (
                                            oldZoom *
                                                safeGestureZoom
                                            ).coerceIn(
                                                MIN_ZOOM,
                                                MAX_ZOOM
                                            )

                                    /*
                                     * Apply zoom around the current pinch
                                     * centroid first.
                                     */
                                    if (
                                        newZoom != oldZoom
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
                                     * Then apply two-finger pan.
                                     */
                                    panX += pan.x
                                    panY += pan.y

                                    pressed.forEach { change ->
                                        change.consume()
                                    }

                                    continue
                                }

                                /*
                                 * One-finger interaction.
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

                                        /*
                                         * Move the selected node in logical
                                         * coordinates. Dividing by zoom keeps
                                         * node movement independent of zoom.
                                         */
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

                                            /*
                                             * Empty canvas drag = pan.
                                             */
                                            panX += delta.x
                                            panY += delta.y
                                        }

                                        change.consume()
                                    }
                                }
                            }

                            /*
                             * Notify editor that node dragging has ended.
                             * This is where the state layer can finalize
                             * automatic engineering recalculation.
                             */
                            if (
                                draggingNode &&
                                    !multiTouch &&
                                    moved
                            ) {

                                currentOnMoveNodeEnd()
                            }

                            /*
                             * A tap with no movement:
                             *
                             * - first tap selects
                             * - second tap edits
                             */
                            if (
                                !moved &&
                                    !multiTouch
                            ) {

                                val now =
                                    firstDown.uptimeMillis

                                val isDoubleTap =
                                    lastTapTime > 0L &&
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

                                if (isDoubleTap) {

                                    if (
                                        firstNode != null
                                    ) {

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

            /*
             * Fixed screen background/grid.
             */
            drawSldEngineeringBackground()

            /*
             * All SLD geometry is rendered in logical coordinates and then
             * transformed by the current viewport.
             */
            withTransform({

                translate(
                    left = panX,
                    top = panY
                )

                scale(
                    scaleX = zoom,
                    scaleY = zoom,
                    pivot = Offset.Zero
                )

            }) {

                /*
                 * Connections are deliberately drawn before nodes so that
                 * feeders and busbars remain behind the IEC symbols.
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
                 * Nodes are rendered after connections.
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
            }
        }

        /*
         * View controls.
         *
         * They are deliberately outside the Canvas transform, so they stay
         * fixed on the screen while the SLD is zoomed/panned.
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
                     * Zoom around the center of the available canvas rather
                     * than around logical origin (0,0).
                     *
                     * This prevents the drawing from flying away when the
                     * engineer uses the +/- buttons.
                     */
                    zoomAt(
                        factor =
                            BUTTON_ZOOM_FACTOR,

                        center =
                            Offset(
                                x = sizeSafeCenterX(
                                    panX = panX,
                                    zoom = zoom
                                ),

                                y = sizeSafeCenterY(
                                    panY = panY,
                                    zoom = zoom
                                )
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
                                x = sizeSafeCenterX(
                                    panX = panX,
                                    zoom = zoom
                                ),

                                y = sizeSafeCenterY(
                                    panY = panY,
                                    zoom = zoom
                                )
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

/*
 * The floating buttons need a stable point around which to zoom.
 *
 * Because the buttons are outside Canvas and this composable does not expose
 * the Canvas size directly, use the current transformed origin as a safe
 * anchor. The value is intentionally kept independent of SLD model data.
 */
private fun sizeSafeCenterX(
    panX: Float,
    zoom: Float
): Float {
    return if (
        panX.isFinite() &&
            zoom.isFinite() &&
            zoom > 0f
    ) {
        panX.coerceIn(
            -100000f,
            100000f
        )
    } else {
        0f
    }
}

private fun sizeSafeCenterY(
    panY: Float,
    zoom: Float
): Float {
    return if (
        panY.isFinite() &&
            zoom.isFinite() &&
            zoom > 0f
    ) {
        panY.coerceIn(
            -100000f,
            100000f
        )
    } else {
        0f
    }
}
