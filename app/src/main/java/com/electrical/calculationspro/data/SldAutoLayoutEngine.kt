package com.electrical.calculationspro.data

/**

* Professional topology-aware SLD auto-layout engine.

* 

* Rules:

* - Stored connection direction is authoritative.

* - Layout NEVER reverses or modifies connections.

* - Electrical topology determines vertical levels.

* - Branches are distributed around their upstream parent.

* - Nodes are kept separated.

* - Disconnected nodes are placed in a safe secondary area.

* - Layout changes coordinates only.

* - No electrical calculations are performed here.
    */
    object SldAutoLayoutEngine {
  
  private const val ROOT_X = 720f
  private const val ROOT_Y = 80f
  
  private const val LEVEL_GAP = 250f
  private const val BRANCH_GAP = 300f
  
  private const val NODE_WIDTH = 180f
  private const val NODE_HEIGHT = 120f
  
  private const val CLEARANCE_X = 100f
  private const val CLEARANCE_Y = 90f
  
  private const val MIN_X = 60f
  private const val MIN_Y = 60f
  
  private const val DISCONNECTED_GAP = 340f
  
  private const val MAX_COLLISION_SEARCH = 250
  
  data class LayoutResult(
  val network: SldNetwork,
  val levels: Map<String, Int>
  )
  
  fun arrange(
  network: SldNetwork
  ): LayoutResult {
  
   if (network.nodes.isEmpty()) {
     return LayoutResult(
         network = network,
         levels = emptyMap()
     )
 }

 val nodesById =
     network.nodes.associateBy { it.id }

 /*
  * ============================================================
  * TOPOLOGY
  * ============================================================
  */

 val children =
     network.nodes.associate { node ->
         node.id to mutableListOf<String>()
     }.toMutableMap()

 val parents =
     network.nodes.associate { node ->
         node.id to mutableListOf<String>()
     }.toMutableMap()

 network.connections.forEach { connection ->

     val fromId = connection.fromNodeId
     val toId = connection.toNodeId

     if (
         fromId == toId ||
         !nodesById.containsKey(fromId) ||
         !nodesById.containsKey(toId)
     ) {
         return@forEach
     }

     val childList =
         children.getValue(fromId)

     if (toId !in childList) {
         childList += toId
     }

     val parentList =
         parents.getValue(toId)

     if (fromId !in parentList) {
         parentList += fromId
     }
 }

 /*
  * ============================================================
  * ROOT SELECTION
  * ============================================================
  *
  * Prefer SOURCE.
  * Other nodes without an upstream parent are secondary roots.
  */

 val sourceRoots =
     network.nodes
         .filter {
             it.type == SldNodeType.SOURCE
         }
         .sortedBy {
             it.name.uppercase()
         }

 val naturalRoots =
     network.nodes
         .filter {
             parents[it.id].orEmpty().isEmpty()
         }
         .sortedWith(
             compareBy<SldNode> {
                 typeOrder(it.type)
             }.thenBy {
                 it.name.uppercase()
             }
         )

 val roots =
     (
         sourceRoots + naturalRoots
     )
         .distinctBy {
             it.id
         }

 /*
  * ============================================================
  * LEVEL CALCULATION
  * ============================================================
  *
  * The stored connection direction defines upstream/downstream.
  */
 val levels =
     mutableMapOf<String, Int>()

 fun assignLevels(
     nodeId: String,
     level: Int,
     visiting: MutableSet<String>
 ) {

     if (!visiting.add(nodeId)) {
         return
     }

     val oldLevel =
         levels[nodeId]

     if (
         oldLevel == null ||
         level < oldLevel
     ) {
         levels[nodeId] = level
     }

     val actualLevel =
         levels[nodeId] ?: level

     val orderedChildren =
         children[nodeId]
             .orEmpty()
             .sortedWith(
                 compareBy<String> {
                     typeOrder(
                         nodesById[it]?.type
                     )
                 }.thenBy {
                     nodesById[it]
                         ?.name
                         ?.uppercase()
                         ?: ""
                 }
             )

     orderedChildren.forEach { childId ->

         assignLevels(
             nodeId = childId,
             level = actualLevel + 1,
             visiting = HashSet(visiting)
         )
     }
 }

 roots.forEach { root ->

     assignLevels(
         nodeId = root.id,
         level = 0,
         visiting = mutableSetOf()
     )
 }

 /*
  * Any isolated/unreachable node receives its own safe level.
  */
 var extraLevel =
     (levels.values.maxOrNull() ?: 0) + 2

 network.nodes
     .filter {
         it.id !in levels
     }
     .sortedWith(
         compareBy<SldNode> {
             typeOrder(it.type)
         }.thenBy {
             it.name.uppercase()
         }
     )
     .forEach { node ->

         levels[node.id] =
             extraLevel

         extraLevel++
     }

 /*
  * ============================================================
  * POSITIONING
  * ============================================================
  */

 val positions =
     mutableMapOf<String, Pair<Float, Float>>()

 /*
  * Place roots.
  */
 val rootIds =
     roots
         .map { it.id }
         .filter { it in levels }

 rootIds.forEachIndexed { index, rootId ->

     val x =
         if (index == 0) {
             ROOT_X
         } else {
             ROOT_X +
                 (index * BRANCH_GAP * 2f)
         }

     positions[rootId] =
         x to ROOT_Y
 }

 /*
  * ============================================================
  * SUBTREE WIDTH
  * ============================================================
  *
  * Calculate how much horizontal space a branch needs.
  * This prevents several downstream branches from collapsing
  * into one another.
  */

 val subtreeWidth =
     mutableMapOf<String, Float>()

 fun calculateWidth(
     nodeId: String,
     visiting: MutableSet<String>
 ): Float {

     if (!visiting.add(nodeId)) {
         return NODE_WIDTH + CLEARANCE_X
     }

     val childIds =
         children[nodeId]
             .orEmpty()
             .filter {
                 it in levels
             }
             .sortedWith(
                 compareBy<String> {
                     typeOrder(
                         nodesById[it]?.type
                     )
                 }.thenBy {
                     nodesById[it]
                         ?.name
                         ?.uppercase()
                         ?: ""
                 }
             )

     if (childIds.isEmpty()) {

         val width =
             NODE_WIDTH +
                 CLEARANCE_X

         subtreeWidth[nodeId] =
             width

         return width
     }

     val totalChildrenWidth =
         childIds.sumOf { childId ->

             calculateWidth(
                 nodeId = childId,
                 visiting = HashSet(visiting)
             ).toDouble()
         }.toFloat()

     val ownWidth =
         NODE_WIDTH +
             CLEARANCE_X

     val width =
         maxOf(
             ownWidth,
             totalChildrenWidth
         )

     subtreeWidth[nodeId] =
         width

     return width
 }

 rootIds.forEach {
     calculateWidth(
         nodeId = it,
         visiting = mutableSetOf()
     )
 }

 /*
  * ============================================================
  * CHILD PLACEMENT
  * ============================================================
  */

 fun placeChildren(
     parentId: String
 ) {

     val parent =
         positions[parentId]
             ?: return

     val childIds =
         children[parentId]
             .orEmpty()
             .filter {
                 it in levels
             }
             .sortedWith(
                 compareBy<String> {
                     typeOrder(
                         nodesById[it]?.type
                     )
                 }.thenBy {
                     nodesById[it]
                         ?.name
                         ?.uppercase()
                         ?: ""
                 }
             )

     if (childIds.isEmpty()) {
         return
     }

     val widths =
         childIds.map { childId ->
             maxOf(
                 subtreeWidth[childId]
                     ?: (
                         NODE_WIDTH +
                             CLEARANCE_X
                         ),
                 NODE_WIDTH +
                     CLEARANCE_X
             )
         }

     val totalWidth =
         widths.sum()

     var cursorX =
         parent.first -
             totalWidth / 2f

     childIds.forEachIndexed { index, childId ->

         val width =
             widths[index]

         val childX =
             cursorX +
                 width / 2f -
                 NODE_WIDTH / 2f

         val childLevel =
             levels[childId]
                 ?: (
                     levels[parentId]
                         ?: 0
                     ) + 1

         val childY =
             ROOT_Y +
                 childLevel *
                 LEVEL_GAP

         /*
          * If the child is already positioned by another root
          * or parent, do not overwrite it unnecessarily.
          */
         if (childId !in positions) {

             positions[childId] =
                 childX to childY
         }

         cursorX +=
             width

         placeChildren(
             parentId = childId
         )
     }
 }

 rootIds.forEach {
     placeChildren(
         parentId = it
     )
 }

 /*
  * ============================================================
  * SECONDARY / DISCONNECTED NODES
  * ============================================================
  */

 val unplaced =
     network.nodes
         .filter {
             it.id !in positions
         }
         .sortedWith(
             compareBy<SldNode> {
                 levels[it.id] ?: Int.MAX_VALUE
             }.thenBy {
                 typeOrder(it.type)
             }.thenBy {
                 it.name.uppercase()
             }
         )

 unplaced.forEachIndexed { index, node ->

     val row =
         index / 4

     val column =
         index % 4

     positions[node.id] =
         (
             MIN_X +
                 column *
                 DISCONNECTED_GAP
             ) to
             (
                 ROOT_Y +
                     (
                         levels[node.id]
                             ?: 0
                         ) *
                     LEVEL_GAP +
                     row *
                     DISCONNECTED_GAP
                 )
 }

 /*
  * ============================================================
  * SPECIAL PANEL / BREAKER / LOAD ALIGNMENT
  * ============================================================
  *
  * Keep breaker-to-load feeders visually clean.
  */
 network.connections
     .filter { connection ->

         val from =
             nodesById[
                 connection.fromNodeId
             ]

         val to =
             nodesById[
                 connection.toNodeId
             ]

         from?.type ==
             SldNodeType.BREAKER &&
             to?.type ==
             SldNodeType.LOAD
     }
     .forEach { connection ->

         val breakerPosition =
             positions[
                 connection.fromNodeId
             ]

         if (breakerPosition != null) {

             val loadId =
                 connection.toNodeId

             /*
              * Keep the load to the right of the breaker.
              * If that space is occupied, collision resolution
              * will move it safely.
              */
             positions[loadId] =
                 (
                     breakerPosition.first +
                         BRANCH_GAP
                     ) to
                     breakerPosition.second
         }
     }

 /*
  * ============================================================
  * COLLISION RESOLUTION
  * ============================================================
  */

 val finalPositions =
     mutableMapOf<String, Pair<Float, Float>>()

 val orderedIds =
     levels.entries
         .sortedWith(
             compareBy<Map.Entry<String, Int>> {
                 it.value
             }.thenBy {
                 positions[it.key]
                     ?.first
                     ?: ROOT_X
             }.thenBy {
                 nodesById[it.key]
                     ?.name
                     ?.uppercase()
                     ?: ""
             }
         )
         .map {
             it.key
         }

 orderedIds.forEach { nodeId ->

     val preferred =
         positions[nodeId]
             ?: (
                 ROOT_X to ROOT_Y
                 )

     finalPositions[nodeId] =
         resolveCollision(
             preferred = preferred,
             occupied = finalPositions.values
         )
 }

 /*
  * ============================================================
  * FINAL NORMALIZATION
  * ============================================================
  */

 val minX =
     finalPositions.values
         .minOfOrNull {
             it.first
         }
         ?: ROOT_X

 val minY =
     finalPositions.values
         .minOfOrNull {
             it.second
         }
         ?: ROOT_Y

 val shiftX =
     if (minX < MIN_X) {
         MIN_X - minX
     } else {
         0f
     }

 val shiftY =
     if (minY < MIN_Y) {
         MIN_Y - minY
     } else {
         0f
     }

 val normalized =
     finalPositions.mapValues { entry ->

         val p =
             entry.value

         (
             p.first +
                 shiftX
             ) to
             (
                 p.second +
                     shiftY
                 )
     }

 /*
  * ============================================================
  * APPLY COORDINATES ONLY
  * ============================================================
  */

 val arrangedNodes =
     network.nodes.map { node ->

         val position =
             normalized[node.id]

         if (position == null) {

             node

         } else {

             node.copy(
                 x = position.first,
                 y = position.second
             )
         }
     }

 return LayoutResult(
     network =
         network.copy(
             nodes = arrangedNodes
         ),
     levels =
         levels.toMap()
 )
  
  }
  
  /*
  
  * ================================================================
  * COLLISION ENGINE
  * ================================================================
    */
  
  private fun resolveCollision(
  preferred: Pair<Float, Float>,
  occupied: Collection<Pair<Float, Float>>
  ): Pair<Float, Float> {
  
   if (
     occupied.none {
         overlaps(
             preferred,
             it
         )
     }
 ) {
     return preferred
 }

 /*
  * First try vertical movement.
  */
 for (step in 1..MAX_COLLISION_SEARCH) {

     val candidate =
         preferred.first to
             (
                 preferred.second +
                     step *
                     (
                         NODE_HEIGHT +
                             CLEARANCE_Y
                         )
                 )

     if (
         occupied.none {
             overlaps(
                 candidate,
                 it
             )
         }
     ) {
         return candidate
     }
 }

 /*
  * Then try right and left.
  */
 for (step in 1..MAX_COLLISION_SEARCH) {

     val distance =
         step *
             (
                 NODE_WIDTH +
                     CLEARANCE_X
                 )

     val right =
         (
             preferred.first +
                 distance
             ) to
             preferred.second

     if (
         occupied.none {
             overlaps(
                 right,
                 it
             )
         }
     ) {
         return right
     }

     val left =
         (
             preferred.first -
                 distance
             ) to
             preferred.second

     if (
         occupied.none {
             overlaps(
                 left,
                 it
             )
         }
     ) {
         return left
     }
 }

 /*
  * Guaranteed fallback.
  */
 var candidate =
     preferred

 var iteration =
     0

 while (
     occupied.any {
         overlaps(
             candidate,
             it
         )
     } &&
     iteration < 2000
 ) {

     candidate =
         (
             candidate.first +
                 NODE_WIDTH +
                 CLEARANCE_X
             ) to
             (
                 candidate.second +
                     NODE_HEIGHT +
                     CLEARANCE_Y
                 )

     iteration++
 }

 return candidate
  
  }
  
  private fun overlaps(
  a: Pair<Float, Float>,
  b: Pair<Float, Float>
  ): Boolean {
  
   val aLeft =
     a.first

 val aTop =
     a.second

 val aRight =
     a.first +
         NODE_WIDTH +
         CLEARANCE_X

 val aBottom =
     a.second +
         NODE_HEIGHT +
         CLEARANCE_Y

 val bLeft =
     b.first

 val bTop =
     b.second

 val bRight =
     b.first +
         NODE_WIDTH +
         CLEARANCE_X

 val bBottom =
     b.second +
         NODE_HEIGHT +
         CLEARANCE_Y

 return (
     aLeft < bRight &&
         aRight > bLeft &&
         aTop < bBottom &&
         aBottom > bTop
     )
  
  }
  
  /*
  
  * ================================================================
  * NODE VISUAL ORDER
  * ================================================================
  * 
  * This is used only for deterministic branch ordering.
  * It does NOT define electrical priority.
    */
  
  private fun typeOrder(
  type: SldNodeType?
  ): Int {
  
   return when (type) {

     SldNodeType.SOURCE ->
         0

     SldNodeType.GENERATOR ->
         1

     SldNodeType.BREAKER ->
         2

     SldNodeType.TRANSFORMER ->
         3

     SldNodeType.BUS ->
         4

     SldNodeType.PANEL ->
         5

     SldNodeType.LOAD ->
         6

     null ->
         99
 }
  
  }
  }
