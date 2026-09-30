package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.SldConnection
import com.electrical.calculationspro.data.SldConnectionType
import com.electrical.calculationspro.data.SldNode
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.data.SldUpstreamEngineering
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

const val NODE_WIDTH = 180f
const val NODE_HEIGHT = 118f

private const val SYMBOL_Y = 30f

private const val CABLE_WIDTH = 3f
private const val SELECTED_CABLE_WIDTH = 5.5f
private const val BUSBAR_WIDTH = 7f

private val BACKGROUND = Color(0xFFF7F9FB)
private val GRID = Color(0xFFE3E8EC)
private val GRID_MAJOR = Color(0xFFD0D8DE)

private val BLACK = Color(0xFF172027)
private val CABLE = Color(0xFF37474F)
private val BUSBAR = Color(0xFF202B32)

private val TEXT = Color(0xFF172027)
private val TEXT_SECONDARY = Color(0xFF60717A)

private val SELECTED = Color(0xFF1565C0)
private val START = Color(0xFFFF9800)

private val OK = Color(0xFF087F5B)
private val WARNING = Color(0xFFE67700)
private val FAULT = Color(0xFFC62828)

private val LABEL_BG = Color.White.copy(alpha = 0.96f)

private enum class Direction {
LEFT,
RIGHT,
UP,
DOWN
}

private fun nodeCenter(
node: SldNode
): Offset =
Offset(
node.x + NODE_WIDTH / 2f,
node.y + NODE_HEIGHT / 2f
)

private fun directionBetween(
from: SldNode,
to: SldNode
): Direction {

val a = nodeCenter(from)
val b = nodeCenter(to)

val dx = b.x - a.x
val dy = b.y - a.y

return if (abs(dx) >= abs(dy)) {
    if (dx >= 0f) {
        Direction.RIGHT
    } else {
        Direction.LEFT
    }
} else {
    if (dy >= 0f) {
        Direction.DOWN
    } else {
        Direction.UP
    }
}

}

private fun standardPort(
node: SldNode,
direction: Direction
): Offset =
when (direction) {

    Direction.LEFT ->
        Offset(
            node.x,
            node.y + NODE_HEIGHT / 2f
        )

    Direction.RIGHT ->
        Offset(
            node.x + NODE_WIDTH,
            node.y + NODE_HEIGHT / 2f
        )

    Direction.UP ->
        Offset(
            node.x + NODE_WIDTH / 2f,
            node.y
        )

    Direction.DOWN ->
        Offset(
            node.x + NODE_WIDTH / 2f,
            node.y + NODE_HEIGHT
        )
}

/*

* PANEL BUSBAR

* 

* All PANEL -> BREAKER busbar connections terminate on the

* physical panel busbar rather than on an arbitrary point.
  */
  private fun panelBusbarGeometry(
  panel: SldNode,
  connections: List<SldConnection>
  ): Pair<Float, List<Float>> {
  
  val outgoing =
  connections.filter {
  it.fromNodeId == panel.id &&
  it.connectionType == SldConnectionType.BUSBAR
  }
  
  val count = max(1, outgoing.size)
  
  val width =
  max(
  76f,
  min(
  NODE_WIDTH - 14f,
  34f * count
  )
  )
  
  val left =
  panel.x +
  NODE_WIDTH / 2f -
  width / 2f
  
  val positions =
  if (outgoing.size <= 1) {
  listOf(panel.x + NODE_WIDTH / 2f)
  } else {
  outgoing.indices.map { index ->
  left +
  index.toFloat() /
  (outgoing.lastIndex).toFloat() *
  width
  }
  }
  
  return width to positions
  }

private fun panelBusbarPort(
panel: SldNode,
connection: SldConnection,
connections: List<SldConnection>
): Offset {

val outgoing =
    connections.filter {
        it.fromNodeId == panel.id &&
            it.connectionType == SldConnectionType.BUSBAR
    }

val (_, positions) =
    panelBusbarGeometry(
        panel,
        connections
    )

val index =
    outgoing.indexOfFirst {
        it.id == connection.id
    }.coerceAtLeast(0)

val x =
    positions.getOrElse(index) {
        panel.x + NODE_WIDTH / 2f
    }

return Offset(
    x,
    panel.y + SYMBOL_Y + 27f
)

}

private fun startPort(
node: SldNode,
connection: SldConnection,
connections: List<SldConnection>,
nodes: List<SldNode>
): Offset {

if (
    node.type == SldNodeType.PANEL &&
    connection.connectionType == SldConnectionType.BUSBAR
) {
    return panelBusbarPort(
        panel = node,
        connection = connection,
        connections = connections
    )
}

val target =
    nodes.firstOrNull {
        it.id == connection.toNodeId
    }

if (target == null) {
    return standardPort(
        node,
        Direction.DOWN
    )
}

return standardPort(
    node,
    directionBetween(
        node,
        target
    )
)

}

private fun endPort(
node: SldNode,
connection: SldConnection,
nodes: List<SldNode>
): Offset {

val source =
    nodes.firstOrNull {
        it.id == connection.fromNodeId
    }

if (source == null) {
    return standardPort(
        node,
        Direction.UP
    )
}

return standardPort(
    node,
    directionBetween(
        node,
        source
    )
)

}

/*

* Orthogonal routing.

* 

* The route leaves the source port and approaches the destination

* port without diagonal electrical lines.
  */
  private fun route(
  start: Offset,
  end: Offset
  ): List<Offset> {
  
  if (
  abs(start.x - end.x) < 2f &&
  abs(start.y - end.y) < 2f
  ) {
  return listOf(
  start,
  end
  )
  }
  
  val horizontal =
  abs(end.x - start.x) >=
  abs(end.y - start.y)
  
  return if (horizontal) {
  
   val midX =
     (start.x + end.x) / 2f

 listOf(
     start,
     Offset(
         midX,
         start.y
     ),
     Offset(
         midX,
         end.y
     ),
     end
 )
  
  } else {
  
   val midY =
     (start.y + end.y) / 2f

 listOf(
     start,
     Offset(
         start.x,
         midY
     ),
     Offset(
         end.x,
         midY
     ),
     end
 )
  
  }
  }

fun DrawScope.drawSldEngineeringBackground() {

drawRect(
    color = BACKGROUND
)

var x = 0f

while (x <= size.width) {

    val major =
        x.toInt() % 200 == 0

    drawLine(
        color =
            if (major) {
                GRID_MAJOR
            } else {
                GRID
            },
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
            if (major) {
                1.4f
            } else {
                0.7f
            }
    )

    x += 40f
}

var y = 0f

while (y <= size.height) {

    val major =
        y.toInt() % 200 == 0

    drawLine(
        color =
            if (major) {
                GRID_MAJOR
            } else {
                GRID
            },
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
            if (major) {
                1.4f
            } else {
                0.7f
            }
    )

    y += 40f
}

}

/*

* CONNECTION DRAWING
  */
  fun DrawScope.drawConnection(
  connection: SldConnection,
  nodes: List<SldNode>,
  connections: List<SldConnection>,
  selected: Boolean,
  textMeasurer: TextMeasurer,
  feederResult:
  SldUpstreamEngineering.FeederResult? = null
  ) {
  
  val from =
  nodes.firstOrNull {
  it.id == connection.fromNodeId
  } ?: return
  
  val to =
  nodes.firstOrNull {
  it.id == connection.toNodeId
  } ?: return
  
  val start =
  startPort(
  node = from,
  connection = connection,
  connections = connections,
  nodes = nodes
  )
  
  val end =
  endPort(
  node = to,
  connection = connection,
  nodes = nodes
  )
  
  val points =
  route(
  start,
  end
  )
  
  val path =
  Path().apply {
  
       moveTo(
         points.first().x,
         points.first().y
     )

     points.drop(1).forEach {
         lineTo(
             it.x,
             it.y
         )
     }
 }
  
  val isBusbar =
  connection.connectionType ==
  SldConnectionType.BUSBAR
  
  val inadequate =
  feederResult != null &&
  !feederResult.cableAdequate
  
  val lineColor =
  when {
  
       selected ->
         SELECTED

     inadequate ->
         FAULT

     isBusbar ->
         BUSBAR

     else ->
         CABLE
 }
  
  val width =
  if (isBusbar) {
  if (selected) {
  BUSBAR_WIDTH + 2f
  } else {
  BUSBAR_WIDTH
  }
  } else {
  if (selected) {
  SELECTED_CABLE_WIDTH
  } else {
  CABLE_WIDTH
  }
  }
  
  drawPath(
  path = path,
  color = lineColor,
  style =
  Stroke(
  width = width,
  cap = StrokeCap.Square,
  join = StrokeJoin.Miter
  )
  )
  
  if (!isBusbar) {
  
   drawFlowArrow(
     points = points,
     color = lineColor
 )
  
  }
  
  val label =
  routeLabelPoint(
  points
  )
  
  if (isBusbar) {
  
   val text =
     buildString {

         append("BUS")

         if (
             connection.busbarRatedCurrentA >
             0.0
         ) {
             append(" ")
             append(
                 fmt(
                     connection.busbarRatedCurrentA
                 )
             )
             append(" A")
         }

         if (
             connection.busbarShortCircuitKA >
             0.0
         ) {
             append(" ")
             append(
                 fmt(
                     connection.busbarShortCircuitKA
                 )
             )
             append(" kA")
         }
     }

 drawEngineeringLabel(
     textMeasurer = textMeasurer,
     text = text,
     point =
         label.copy(
             y = label.y - 14f
         ),
     color = BUSBAR,
     fontSize = 8.5f
 )
  
  } else {
  
   val cableText =
     buildString {

         if (
             connection.cableSizeMm2 >
             0.0
         ) {

             append(
                 fmt(
                     connection.cableSizeMm2
                 )
             )

             append(" mm²")

             if (
                 connection.parallelRuns > 1
             ) {
                 append(" × ")
                 append(
                     connection.parallelRuns
                 )
             }
         }

         if (
             connection.lengthMeters >
             0.0
         ) {

             if (isNotEmpty()) {
                 append("  ")
             }

             append(
                 fmt(
                     connection.lengthMeters
                 )
             )

             append(" m")
         }

         if (isEmpty()) {
             append("FEEDER")
         }
     }

 drawEngineeringLabel(
     textMeasurer = textMeasurer,
     text = cableText,
     point =
         label.copy(
             y = label.y - 14f
         ),
     color =
         if (inadequate) {
             FAULT
         } else {
             TEXT_SECONDARY
         },
     fontSize = 8.5f
 )

 feederResult?.let {

     val resultColor =
         when {

             !it.cableAdequate ->
                 FAULT

             it.voltageDropPercent > 3.0 ->
                 WARNING

             else ->
                 OK
         }

     drawEngineeringLabel(
         textMeasurer = textMeasurer,
         text =
             "Ib=${fmt(it.currentA)} A   " +
                 "S=${fmt(it.kva)} kVA   " +
                 "ΔV=${fmt(it.voltageDropPercent)}%",
         point =
             label.copy(
                 y = label.y + 5f
             ),
         color = resultColor,
         fontSize = 7.5f
     )
 }
  
  }
  }

private fun routeLabelPoint(
points: List<Offset>
): Offset {

if (points.size <= 2) {

    return Offset(
        (
            points.first().x +
                points.last().x
        ) / 2f,
        (
            points.first().y +
                points.last().y
        ) / 2f
    )
}

var bestLength = 0f
var bestPoint =
    points[points.size / 2]

for (
    i in 0 until points.lastIndex
) {

    val a = points[i]
    val b = points[i + 1]

    val length =
        sqrt(
            (b.x - a.x) *
                (b.x - a.x) +
                (b.y - a.y) *
                (b.y - a.y)
        )

    if (length > bestLength) {

        bestLength = length

        bestPoint =
            Offset(
                (a.x + b.x) / 2f,
                (a.y + b.y) / 2f
            )
    }
}

return bestPoint

}

private fun DrawScope.drawFlowArrow(
points: List<Offset>,
color: Color
) {

if (points.size < 2) {
    return
}

/*
 * Use the final segment so the arrow always points
 * from fromNodeId toward toNodeId.
 */
val a =
    points[
        points.lastIndex - 1
    ]

val b =
    points.last()

val dx =
    b.x - a.x

val dy =
    b.y - a.y

val length =
    sqrt(
        dx * dx +
            dy * dy
    )

if (length < 12f) {
    return
}

val ux =
    dx / length

val uy =
    dy / length

val arrowLength = 9f
val arrowWidth = 5f

val base =
    Offset(
        b.x -
            ux * arrowLength,
        b.y -
            uy * arrowLength
    )

val px = -uy
val py = ux

val path =
    Path().apply {

        moveTo(
            b.x,
            b.y
        )

        lineTo(
            base.x +
                px * arrowWidth,
            base.y +
                py * arrowWidth
        )

        lineTo(
            base.x -
                px * arrowWidth,
            base.y -
                py * arrowWidth
        )

        close()
    }

drawPath(
    path = path,
    color = color
)

}

/*

* NODE DRAWING
  */
  fun DrawScope.drawNode(
  node: SldNode,
  selected: Boolean,
  connectionStart: Boolean,
  textMeasurer: TextMeasurer,
  engineeringResult:
  SldUpstreamEngineering.NodeResult? = null,
  nodes: List<SldNode> = emptyList(),
  connections: List<SldConnection> = emptyList()
  ) {
  
  val centerX =
  node.x +
  NODE_WIDTH / 2f
  
  val symbolY =
  node.y +
  SYMBOL_Y
  
  if (
  selected ||
  connectionStart
  ) {
  
   drawCircle(
     color =
         if (connectionStart) {
             START
         } else {
             SELECTED
         },
     radius = 39f,
     center =
         Offset(
             centerX,
             symbolY
         ),
     style =
         Stroke(
             width = 3f
         )
 )
  
  }
  
  when (node.type) {
  
   SldNodeType.SOURCE ->
     drawSource(
         centerX,
         symbolY
     )

 SldNodeType.TRANSFORMER ->
     drawTransformer(
         centerX,
         symbolY
     )

 SldNodeType.GENERATOR ->
     drawGenerator(
         centerX,
         symbolY
     )

 SldNodeType.BUS ->
     drawBus(
         centerX,
         symbolY
     )

 SldNodeType.PANEL ->
     drawPanel(
         centerX,
         symbolY,
         node,
         connections
     )

 SldNodeType.BREAKER -> {

     val outgoing =
         connections.firstOrNull {
             it.fromNodeId == node.id
         }

     val incoming =
         connections.firstOrNull {
             it.toNodeId == node.id
         }

     val connected =
         when {

             outgoing != null ->
                 nodes.firstOrNull {
                     it.id ==
                         outgoing.toNodeId
                 }

             incoming != null ->
                 nodes.firstOrNull {
                     it.id ==
                         incoming.fromNodeId
                 }

             else ->
                 null
         }

     val direction =
         connected?.let {
             directionBetween(
                 node,
                 it
             )
         } ?: Direction.DOWN

     drawBreaker(
         centerX,
         symbolY,
         direction
     )
 }

 SldNodeType.LOAD ->
     drawLoad(
         centerX,
         symbolY
     )
  
  }
  
  drawCenteredText(
  textMeasurer = textMeasurer,
  text =
  equipmentLabel(
  node.type
  ),
  centerX = centerX,
  y =
  node.y +
  51f,
  style =
  TextStyle(
  color = TEXT_SECONDARY,
  fontSize = 7.5.sp,
  fontWeight = FontWeight.Bold
  )
  )
  
  drawCenteredText(
  textMeasurer = textMeasurer,
  text =
  node.name.take(24),
  centerX = centerX,
  y =
  node.y +
  64f,
  style =
  TextStyle(
  color = TEXT,
  fontSize = 10.sp,
  fontWeight = FontWeight.Bold
  )
  )
  
  val electrical =
  buildString {
  
       append("V=")
     append(
         fmt(
             node.voltage
         )
     )
     append(" V")

     if (
         node.loadKw > 0.0
     ) {

         append("  P=")
         append(
             fmt(
                 node.loadKw
             )
         )
         append(" kW")
     }

     if (
         node.ratedKva > 0.0
     ) {

         append("  R=")
         append(
             fmt(
                 node.ratedKva
             )
         )
         append(" kVA")
     }
 }
  
  drawCenteredText(
  textMeasurer = textMeasurer,
  text = electrical,
  centerX = centerX,
  y =
  node.y +
  79f,
  style =
  TextStyle(
  color = TEXT_SECONDARY,
  fontSize = 7.2.sp
  )
  )
  
  engineeringResult?.let {
  
   drawCenteredText(
     textMeasurer = textMeasurer,
     text =
         "Pdem=${fmt(it.demandKw)} kW  " +
             "S=${fmt(it.kva)} kVA",
     centerX = centerX,
     y =
         node.y +
             92f,
     style =
         TextStyle(
             color = TEXT_SECONDARY,
             fontSize = 7.2.sp
         )
 )

 val resultColor =
     when {

         it.loadingPercent > 100.0 ->
             FAULT

         it.voltageDropPercent > 3.0 ->
             WARNING

         else ->
             OK
     }

 drawCenteredText(
     textMeasurer = textMeasurer,
     text =
         "Ib=${fmt(it.currentA)} A  " +
             "CB=${fmt(it.recommendedBreakerA)} A  " +
             "ΔV=${fmt(it.voltageDropPercent)}%",
     centerX = centerX,
     y =
         node.y +
             106f,
     style =
         TextStyle(
             color = resultColor,
             fontSize = 7.2.sp,
             fontWeight = FontWeight.Bold
         )
 )
  
  } ?: run {
  
   if (
     node.type ==
         SldNodeType.TRANSFORMER &&
     node.ratedKva > 0.0
 ) {

     drawCenteredText(
         textMeasurer = textMeasurer,
         text =
             "Z=${fmt(node.transformerPercentZ)}%",
         centerX = centerX,
         y =
             node.y +
                 95f,
         style =
             TextStyle(
                 color = TEXT_SECONDARY,
                 fontSize = 7.sp
             )
     )
 }
  
  }
  }

/*

* SOURCE
  */
  private fun DrawScope.drawSource(
  x: Float,
  y: Float
  ) {
  
  drawCircle(
  color = BLACK,
  radius = 25f,
  center =
  Offset(
  x,
  y
  ),
  style =
  Stroke(
  width = 2.8f
  )
  )
  
  drawArc(
  color = BLACK,
  startAngle = 205f,
  sweepAngle = 130f,
  useCenter = false,
  topLeft =
  Offset(
  x - 14f,
  y - 10f
  ),
  size =
  Size(
  28f,
  20f
  ),
  style =
  Stroke(
  width = 2.5f
  )
  )
  
  drawLine(
  color = BLACK,
  start =
  Offset(
  x - 14f,
  y + 10f
  ),
  end =
  Offset(
  x + 14f,
  y + 10f
  ),
  strokeWidth = 2.5f
  )
  }

/*

* TRANSFORMER
  */
  private fun DrawScope.drawTransformer(
  x: Float,
  y: Float
  ) {
  
  drawCircle(
  color = BLACK,
  radius = 18f,
  center =
  Offset(
  x - 12f,
  y
  ),
  style =
  Stroke(
  width = 2.8f
  )
  )
  
  drawCircle(
  color = BLACK,
  radius = 18f,
  center =
  Offset(
  x + 12f,
  y
  ),
  style =
  Stroke(
  width = 2.8f
  )
  )
  
  drawLine(
  color = BLACK,
  start =
  Offset(
  x - 34f,
  y
  ),
  end =
  Offset(
  x - 30f,
  y
  ),
  strokeWidth = 2.8f
  )
  
  drawLine(
  color = BLACK,
  start =
  Offset(
  x + 30f,
  y
  ),
  end =
  Offset(
  x + 34f,
  y
  ),
  strokeWidth = 2.8f
  )
  }

/*

* GENERATOR
  */
  private fun DrawScope.drawGenerator(
  x: Float,
  y: Float
  ) {
  
  drawCircle(
  color = BLACK,
  radius = 25f,
  center =
  Offset(
  x,
  y
  ),
  style =
  Stroke(
  width = 2.8f
  )
  )
  
  drawArc(
  color = BLACK,
  startAngle = -55f,
  sweepAngle = 290f,
  useCenter = false,
  topLeft =
  Offset(
  x - 15f,
  y - 15f
  ),
  size =
  Size(
  30f,
  30f
  ),
  style =
  Stroke(
  width = 2.2f
  )
  )
  }

/*

* BUS
  */
  private fun DrawScope.drawBus(
  x: Float,
  y: Float
  ) {
  
  drawLine(
  color = BUSBAR,
  start =
  Offset(
  x - 42f,
  y
  ),
  end =
  Offset(
  x + 42f,
  y
  ),
  strokeWidth =
  BUSBAR_WIDTH,
  cap =
  StrokeCap.Square
  )
  }

/*

* PANEL
  */
  private fun DrawScope.drawPanel(
  x: Float,
  y: Float,
  node: SldNode,
  connections: List<SldConnection>
  ) {
  
  val (width, positions) =
  panelBusbarGeometry(
  panel = node,
  connections = connections
  )
  
  /*
  
  * Main internal busbar.
    */
    drawLine(
    color = BUSBAR,
    start =
    Offset(
    x - width / 2f,
    y
    ),
    end =
    Offset(
    x + width / 2f,
    y
    ),
    strokeWidth = BUSBAR_WIDTH,
    cap =
    StrokeCap.Square
    )
  
  /*
  
  * Incoming feeder terminal.
    */
    drawLine(
    color = BLACK,
    start =
    Offset(
    x,
    y - 28f
    ),
    end =
    Offset(
    x,
    y
    ),
    strokeWidth = 2.8f
    )
  
  /*
  
  * Busbar outgoing taps.
    */
    positions.forEach { px ->
    
    drawLine(
    color = BLACK,
    start =
    Offset(
    px,
    y
    ),
    end =
    Offset(
    px,
    y + 27f
    ),
    strokeWidth = 2.8f
    )
    }
    }

/*

* BREAKER
  */
  private fun DrawScope.drawBreaker(
  x: Float,
  y: Float,
  direction: Direction
  ) {
  
  when (direction) {
  
   Direction.RIGHT,
 Direction.LEFT -> {

     val sign =
         if (
             direction ==
                 Direction.RIGHT
         ) {
             1f
         } else {
             -1f
         }

     val terminalA =
         Offset(
             x - 31f * sign,
             y
         )

     val pivot =
         Offset(
             x - 8f * sign,
             y
         )

     val blade =
         Offset(
             x + 17f * sign,
             y - 17f
         )

     val terminalB =
         Offset(
             x + 31f * sign,
             y - 17f
         )

     drawLine(
         color = BLACK,
         start = terminalA,
         end = pivot,
         strokeWidth = 2.8f
     )

     drawCircle(
         color = BLACK,
         radius = 3.2f,
         center = pivot
     )

     drawLine(
         color = BLACK,
         start = pivot,
         end = blade,
         strokeWidth = 3.2f
     )

     drawLine(
         color = BLACK,
         start = blade,
         end = terminalB,
         strokeWidth = 2.8f
     )

     drawCircle(
         color = BLACK,
         radius = 3.2f,
         center = terminalB
     )
 }

 Direction.DOWN,
 Direction.UP -> {

     val sign =
         if (
             direction ==
                 Direction.DOWN
         ) {
             1f
         } else {
             -1f
         }

     val terminalA =
         Offset(
             x,
             y - 31f * sign
         )

     val pivot =
         Offset(
             x,
             y - 8f * sign
         )

     val blade =
         Offset(
             x + 17f,
             y + 17f * sign
         )

     val terminalB =
         Offset(
             x + 17f,
             y + 31f * sign
         )

     drawLine(
         color = BLACK,
         start = terminalA,
         end = pivot,
         strokeWidth = 2.8f
     )

     drawCircle(
         color = BLACK,
         radius = 3.2f,
         center = pivot
     )

     drawLine(
         color = BLACK,
         start = pivot,
         end = blade,
         strokeWidth = 3.2f
     )

     drawLine(
         color = BLACK,
         start = blade,
         end = terminalB,
         strokeWidth = 2.8f
     )

     drawCircle(
         color = BLACK,
         radius = 3.2f,
         center = terminalB
     )
 }
  
  }
  }

/*

* LOAD / MOTOR
  */
  private fun DrawScope.drawLoad(
  x: Float,
  y: Float
  ) {
  
  drawCircle(
  color = BLACK,
  radius = 22f,
  center =
  Offset(
  x,
  y
  ),
  style =
  Stroke(
  width = 2.8f
  )
  )
  
  drawLine(
  color = BLACK,
  start =
  Offset(
  x - 12f,
  y + 12f
  ),
  end =
  Offset(
  x + 12f,
  y - 12f
  ),
  strokeWidth = 2.8f
  )
  
  drawLine(
  color = BLACK,
  start =
  Offset(
  x - 12f,
  y - 12f
  ),
  end =
  Offset(
  x + 12f,
  y + 12f
  ),
  strokeWidth = 1.8f
  )
  }

private fun equipmentLabel(
type: SldNodeType
): String =
when (type) {

    SldNodeType.SOURCE ->
        "UTILITY SOURCE"

    SldNodeType.TRANSFORMER ->
        "TRANSFORMER"

    SldNodeType.GENERATOR ->
        "GENERATOR"

    SldNodeType.BUS ->
        "BUS"

    SldNodeType.PANEL ->
        "PANEL"

    SldNodeType.BREAKER ->
        "BREAKER"

    SldNodeType.LOAD ->
        "LOAD"
}

private fun DrawScope.drawCenteredText(
textMeasurer: TextMeasurer,
text: String,
centerX: Float,
y: Float,
style: TextStyle
) {

val measured =
    textMeasurer.measure(
        text = text,
        style = style
    )

drawText(
    textMeasurer = textMeasurer,
    text = text,
    topLeft =
        Offset(
            centerX -
                measured.size.width / 2f,
            y
        ),
    style = style
)

}

private fun DrawScope.drawEngineeringLabel(
textMeasurer: TextMeasurer,
text: String,
point: Offset,
color: Color,
fontSize: Float
) {

if (text.isBlank()) {
    return
}

val style =
    TextStyle(
        color = color,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold
    )

val measured =
    textMeasurer.measure(
        text = text,
        style = style
    )

drawRoundRect(
    color = LABEL_BG,
    topLeft =
        Offset(
            point.x -
                measured.size.width / 2f -
                5f,
            point.y - 3f
        ),
    size =
        Size(
            measured.size.width + 10f,
            measured.size.height + 7f
        ),
    cornerRadius =
        CornerRadius(
            3f,
            3f
        )
)

drawText(
    textMeasurer = textMeasurer,
    text = text,
    topLeft =
        Offset(
            point.x -
                measured.size.width / 2f,
            point.y
        ),
    style = style
)

}

/*

* HIT TESTING

* 

* The nearest matching node is returned rather than simply the

* last node in the list. This prevents a newly added node from

* making another nearby node difficult to select.
  */
  fun findNode(
  point: Offset,
  nodes: List<SldNode>
  ): SldNode? {
  
  return nodes
  .asSequence()
  .filter { node ->
  
       point.x >=
         node.x - 8f &&

         point.x <=
         node.x +
             NODE_WIDTH +
             8f &&

         point.y >=
         node.y - 8f &&

         point.y <=
         node.y +
             NODE_HEIGHT +
             8f
 }
 .minByOrNull { node ->

     val center =
         nodeCenter(
             node
         )

     val dx =
         point.x -
             center.x

     val dy =
         point.y -
             center.y

     dx * dx +
         dy * dy
 }

}

fun findConnection(
point: Offset,
nodes: List<SldNode>,
connections: List<SldConnection>
): SldConnection? {

var best:
    SldConnection? =
    null

var bestDistance =
    Float.MAX_VALUE

connections.forEach { connection ->

    val from =
        nodes.firstOrNull {
            it.id ==
                connection.fromNodeId
        } ?: return@forEach

    val to =
        nodes.firstOrNull {
            it.id ==
                connection.toNodeId
        } ?: return@forEach

    val start =
        startPort(
            node = from,
            connection = connection,
            connections = connections,
            nodes = nodes
        )

    val end =
        endPort(
            node = to,
            connection = connection,
            nodes = nodes
        )

    val points =
        route(
            start,
            end
        )

    for (
        index in
        0 until points.lastIndex
    ) {

        val distance =
            segmentDistance(
                point,
                points[index],
                points[index + 1]
            )

        if (
            distance <
            bestDistance
        ) {

            bestDistance =
                distance

            best =
                connection
        }
    }
}

return if (
    bestDistance <= 24f
) {
    best
} else {
    null
}

}

private fun segmentDistance(
point: Offset,
a: Offset,
b: Offset
): Float {

val dx =
    b.x - a.x

val dy =
    b.y - a.y

if (
    dx == 0f &&
    dy == 0f
) {
    return distance(
        point,
        a
    )
}

val t =
    (
        (point.x - a.x) * dx +
            (point.y - a.y) * dy
        ) /
        (
            dx * dx +
                dy * dy
            )

val clamped =
    t.coerceIn(
        0f,
        1f
    )

val closest =
    Offset(
        a.x +
            clamped * dx,
        a.y +
            clamped * dy
    )

return distance(
    point,
    closest
)

}

private fun distance(
a: Offset,
b: Offset
): Float {

val dx =
    a.x - b.x

val dy =
    a.y - b.y

return sqrt(
    dx * dx +
        dy * dy
)

}
