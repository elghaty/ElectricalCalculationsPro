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

private const val NODE_CENTER_Y = 75f
private const val TAP_SLOP = 6f

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

    fun safeZoom(): Float {
        return zoom
            .takeIf {
                it.isFinite() &&
                    it > 0f
            }
            ?: 1f
    }

    fun screenToLogical(
        point: Offset
    ): Offset {

        val currentZoom =
            safeZoom()

        return Offset(
            x =
                (
                    point.x -
                        panX
                    ) /
                    currentZoom,

            y =
                (
                    point.y -
                        panY
                    ) /
                    currentZoom
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
            safeZoom()
                .coerceIn(
                    MIN_ZOOM,
                    MAX_ZOOM
                )

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
        zoom = 1f
        panX = 0f
        panY = 0f
    }

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

                        var lastTapTime = 0L

                        var lastTapPosition =
                            Offset.Unspecified

                        awaitEachGesture {

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

                            /*
                             * Connection hit testing has priority.
                             */
                            val firstConnection =
                                findConnection(
                                    point =
                                        firstLogicalPosition,
                                    nodes =
                                        currentNodes,
                                    connections =
                                        currentConnections
                                )

                            val firstNode =
                                if (
                                    firstConnection == null
                                ) {
                                    findNode(
                                        point =
                                            firstLogicalPosition,
                                        nodes =
                                            currentNodes
                                    )
                                } else {
                                    null
                                }

                            var activeNodeId =
                                firstNode?.id

                            var draggingNode =
                                firstNode != null &&
                                    currentConnectionStartId == null

                            var dragStarted = false
                            var moved = false
                            var multiTouch = false
                            var pointerCount = 1

                            var accumulatedDrag =
                                Offset.Zero

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

                                if (
                                    pressed.isEmpty()
                                ) {
                                    break
                                }

                                /*
                                 * ==================================================
                                 * MULTI TOUCH
                                 * ==================================================
                                 */
                                if (
                                    pressed.size >= 2
                                ) {

                                    multiTouch = true
                                    draggingNode = false
                                    dragStarted = false
                                    moved = true

                                    val centroid =
                                        event.calculateCentroid(
                                            useCurrent = false
                                        )

                                    val pan =
                                        event.calculatePan()

                                    val gestureZoom =
                                        event.calculateZoom()

                                    val safeGestureZoom =
                                        if (
                                            gestureZoom.isFinite() &&
                                            gestureZoom > 0f
                                        ) {
                                            gestureZoom
                                        } else {
                                            1f
                                        }

                                    val oldZoom =
                                        safeZoom()
                                            .coerceIn(
                                                MIN_ZOOM,
                                                MAX_ZOOM
                                            )

                                    val newZoom =
                                        (
                                            oldZoom *
                                                safeGestureZoom
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

                                    if (
                                        pan != Offset.Zero
                                    ) {
                                        panX += pan.x
                                        panY += pan.y
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
                                        delta != Offset.Zero
                                    ) {

                                        accumulatedDrag +=
                                            delta

                                        val distance =
                                            accumulatedDrag
                                                .getDistance()

                                        if (
                                            distance >
                                            TAP_SLOP
                                        ) {

                                            moved = true

                                            if (
                                                draggingNode &&
                                                activeNodeId != null
                                            ) {

                                                dragStarted = true

                                                val currentZoom =
                                                    safeZoom()

                                                val logicalDx =
                                                    delta.x /
                                                        currentZoom

                                                val logicalDy =
                                                    delta.y /
                                                        currentZoom

                                                currentOnMoveNode(
                                                    activeNodeId!!,
                                                    logicalDx,
                                                    logicalDy
                                                )

                                            } else {

                                                panX += delta.x
                                                panY += delta.y
                                            }
                                        }

                                        change.consume()
                                    }
                                }
                            }

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

                                if (
                                    isDoubleTap
                                ) {

                                    if (
                                        currentConnectionStartId ==
                                        null
                                    ) {

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
                 * ==========================================================
                 * EXTERNAL FEEDERS
                 * ==========================================================
                 *
                 * drawConnection() intentionally ignores BUSBAR
                 * connections. Only real external feeders are drawn here.
                 */
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
                 * ==========================================================
                 * REAL PANEL ENCLOSURES + REAL INTERNAL BUSBARS
                 * ==========================================================
                 *
                 * This layer is deliberately between feeders and
                 * equipment symbols.
                 *
                 * It owns:
                 * - panel border
                 * - internal busbar
                 * - breaker taps
                 * - panel/bus/breaker enclosure grouping
                 */
                drawPanelEnclosures(
                    nodes =
                        currentNodes,

                    connections =
                        currentConnections,

                    textMeasurer =
                        textMeasurer
                )

                /*
                 * ==========================================================
                 * EQUIPMENT SYMBOLS
                 * ==========================================================
                 */
                currentNodes.forEach {
                    node ->

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
                 * ==========================================================
                 * CONNECTION START INDICATOR
                 * ==========================================================
                 */
                currentConnectionStartId
                    ?.let { startId ->

                        currentNodes
                            .firstOrNull {
                                it.id == startId
                            }
                            ?.let { node ->

                                val center =
                                    Offset(
                                        x =
                                            node.x +
                                                NODE_WIDTH /
                                                2f,

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
                                                width = 3f
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

            FloatingActionButton(
                onClick = {

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
