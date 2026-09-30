package com.electrical.calculationspro.data

import java.util.ArrayDeque

/**

* Professional SLD topology engine.

* 

* RESPONSIBILITY

* ---

* This class validates the electrical topology used by the

* engineering calculation engines.

* 

* IMPORTANT:

* The stored connection direction is the electrical direction:

* 

* fromNodeId -> toNodeId

* 

* The topology engine MUST NOT silently reverse a connection based

* on node type, screen position, or BFS distance.

* 

* Direction is established by the SLD editor when the connection

* is created and is then treated as engineering data.

* 

* Example valid topology:

* 

* SOURCE

*   |

* BREAKER

*   |

*  BUS

*   |

* TRANSFORMER

*   |

* BREAKER

*   |

* PANEL

*   |

* BREAKER

*   |

*  LOAD

* 

* Other valid arrangements are also supported, for example:

* 

* SOURCE -> BREAKER -> BUS

* BUS -> TRANSFORMER

* TRANSFORMER -> BREAKER -> BUS

* PANEL -> BREAKER -> LOAD

* 

* Component type does NOT determine electrical direction.

* 

* The engine validates:

* 

* 1. Node IDs

* 2. Connection endpoints

* 3. Self connections

* 4. Duplicate directed connections

* 5. Cycles

* 6. Source reachability

* 7. One upstream parent per node

* 8. Radial electrical topology

* 

* UI is not responsible for engineering calculations.
  */
  object SldTopologyEngine {
  
  data class Topology(
  val source: SldNode,
  val connections: List<SldConnection>,
  val children: Map<String, List<SldNode>>,
  val parents: Map<String, List<SldNode>>
  )
  
  /**
  
  * Build and validate the electrical topology.
  
  * 
  
  * The connection direction stored in SldNetwork is preserved.
    */
    fun build(
    network: SldNetwork
    ): Topology {
    
    require(network.nodes.isNotEmpty()) {
    "SLD network is empty."
    }
    
    /*
    
    * ============================================================
    * NODE INDEX
    * ============================================================
      */
    
    val nodeMap =
    network.nodes.associateBy {
    it.id
    }
    
    require(
    nodeMap.size == network.nodes.size
    ) {
    "Duplicate SLD node IDs."
    }
    
    /*
    
    * ============================================================
    * CONNECTION BASIC VALIDATION
    * ============================================================
      */
    
    network.connections.forEach { connection ->
    
     require(
     connection.fromNodeId in nodeMap
 ) {
     "Connection ${connection.id}: source node does not exist."
 }

 require(
     connection.toNodeId in nodeMap
 ) {
     "Connection ${connection.id}: destination node does not exist."
 }

 require(
     connection.fromNodeId !=
         connection.toNodeId
 ) {
     "Connection ${connection.id}: node cannot connect to itself."
 }

 require(
     connection.parallelRuns >= 1
 ) {
     "Connection ${connection.id}: parallel cable runs must be at least 1."
 }
    
    }
    
    /*
    
    * ============================================================
    * DUPLICATE DIRECTED CONNECTION DETECTION
    * ============================================================
    * 
    * A->B and another A->B are not two independent feeders.
    * They are a duplicated SLD connection and must be rejected.
    * 
    * A->B and B->A are handled later as a cycle.
      */
    
    val directedConnectionKeys =
    mutableSetOf<String>()
    
    network.connections.forEach { connection ->
    
     val key =
     "${connection.fromNodeId}->${connection.toNodeId}"

 require(
     directedConnectionKeys.add(key)
 ) {
     "Duplicate SLD connection detected: $key"
 }
    
    }
    
    /*
    
    * ============================================================
    * ELECTRICAL SOURCE
    * ============================================================
    * 
    * Prefer an explicit SOURCE node.
    * 
    * A fallback is retained for legacy projects that may not
    * contain a SOURCE node yet.
      */
    
    val explicitSource =
    network.nodes.firstOrNull {
    it.type == SldNodeType.SOURCE
    }
    
    val source =
    explicitSource
    ?: network.nodes.firstOrNull { node ->
    network.connections.none { connection ->
    connection.toNodeId ==
    node.id
    }
    }
    ?: network.nodes.first()
    
    /*
    
    * ============================================================
    * SOURCE COUNT
    * ============================================================
    * 
    * The current SLD calculation engine is radial and expects
    * one electrical source.
    * 
    * Multiple SOURCE nodes are therefore rejected.
      */
    
    val sourceNodes =
    network.nodes.filter {
    it.type == SldNodeType.SOURCE
    }
    
    require(
    sourceNodes.size <= 1
    ) {
    "SLD must contain one electrical SOURCE for radial calculation."
    }
    
    /*
    
    * ============================================================
    * DIRECTED GRAPH
    * ============================================================
    * 
    * This is the actual electrical graph.
    * 
    * fromNodeId -> toNodeId
    * 
    * DO NOT convert it to an undirected graph.
      */
    
    val childrenIds =
    mutableMapOf<
    String,
    MutableList<String>
    >()
    
    val parentIds =
    mutableMapOf<
    String,
    MutableList<String>
    >()
    
    network.nodes.forEach { node ->
    
     childrenIds[node.id] =
     mutableListOf()

 parentIds[node.id] =
     mutableListOf()
    
    }
    
    network.connections.forEach { connection ->
    
     childrenIds[
     connection.fromNodeId
 ]!!
     .add(
         connection.toNodeId
     )

 parentIds[
     connection.toNodeId
 ]!!
     .add(
         connection.fromNodeId
     )
    
    }
    
    /*
    
    * ============================================================
    * RADIAL PARENT VALIDATION
    * ============================================================
    * 
    * Every downstream node may have only one upstream feeder.
    * 
    * This prevents:
    * 
    * BUS A ----\
    *             > PANEL
    * BUS B ----/
    * 
    * which would require a transfer/changeover/synchronizing
    * model that is outside the current radial SLD engine.
      */
    
    val multiParentNodes =
    parentIds.filter {
    it.value.size > 1
    }
    
    require(
    multiParentNodes.isEmpty()
    ) {
    val names =
    multiParentNodes.keys.mapNotNull { id ->
    nodeMap[id]?.name
    }
    
     "Invalid radial SLD: node(s) have multiple upstream feeders: " +
     names.joinToString()
    
    }
    
    /*
    
    * ============================================================
    * CYCLE DETECTION
    * ============================================================
    * 
    * A directed DFS is used instead of BFS distance.
    * 
    * This is important because a valid engineering graph can
    * contain component types in many different sequences.
    * 
    * The only thing that matters here is the actual direction
    * of the electrical connections.
      */
    
    val visitState =
    mutableMapOf<
    String,
    VisitState
    >()
    
    network.nodes.forEach { node ->
    visitState[node.id] =
    VisitState.UNVISITED
    }
    
    fun dfs(
    nodeId: String
    ) {
    
     when (
     visitState[nodeId]
 ) {

     VisitState.VISITING -> {
         val nodeName =
             nodeMap[nodeId]?.name
                 ?: nodeId

         throw IllegalArgumentException(
             "Circular SLD connection detected at $nodeName."
         )
     }

     VisitState.VISITED -> {
         return
     }

     else -> Unit
 }

 visitState[nodeId] =
     VisitState.VISITING

 childrenIds[
     nodeId
 ]
     .orEmpty()
     .forEach { childId ->

         dfs(
             childId
         )
     }

 visitState[nodeId] =
     VisitState.VISITED
    
    }
    
    /*
    
    * Run DFS for the complete graph so that a cycle cannot hide
    * inside an otherwise valid source network.
      */
    
    network.nodes.forEach { node ->
    
     if (
     visitState[node.id] ==
     VisitState.UNVISITED
 ) {
     dfs(node.id)
 }
    
    }
    
    /*
    
    * ============================================================
    * SOURCE REACHABILITY
    * ============================================================
    * 
    * The source must be able to reach every engineering node
    * through the STORED electrical direction.
    * 
    * This is deliberately directed.
    * 
    * Example:
    * 
    * SOURCE -> BUS -> PANEL
    * 
    * is valid.
    * 
    * But:
    * 
    * PANEL -> BUS
    * 
    * is NOT silently corrected to BUS -> PANEL.
    * 
    * The editor must create the correct direction.
      */
    
    val reachable =
    mutableSetOf<String>()
    
    val queue =
    ArrayDeque<String>()
    
    queue.add(
    source.id
    )
    
    while (
    queue.isNotEmpty()
    ) {
    
     val current =
     queue.removeFirst()

 if (
     !reachable.add(
         current
     )
 ) {
     continue
 }

 childrenIds[
     current
 ]
     .orEmpty()
     .forEach { childId ->

         if (
             childId !in
             reachable
         ) {
             queue.add(
                 childId
             )
         }
     }
    
    }
    
    /*
    
    * Every node must be reachable from the electrical source.
      */
    
    val disconnected =
    network.nodes.filter {
    it.id !in reachable
    }
    
    require(
    disconnected.isEmpty()
    ) {
    
     val disconnectedNames =
     disconnected.joinToString {
         it.name
     }

 "Disconnected or incorrectly directed SLD nodes: " +
     disconnectedNames +
     ". Connections must point from upstream to downstream."
    
    }
    
    /*
    
    * ============================================================
    * SOURCE PARENT VALIDATION
    * ============================================================
    * 
    * The electrical source must not itself be downstream of
    * another node.
      */
    
    val sourceParents =
    parentIds[
    source.id
    ].orEmpty()
    
    require(
    sourceParents.isEmpty()
    ) {
    "Electrical SOURCE cannot have an upstream feeder."
    }
    
    /*
    
    * ============================================================
    * NON-SOURCE NODE PARENT VALIDATION
    * ============================================================
    * 
    * Every reachable node other than SOURCE must have exactly
    * one parent in the radial model.
      */
    
    val missingParents =
    network.nodes.filter { node ->
    
         node.id != source.id &&
         parentIds[
             node.id
         ].orEmpty().isEmpty()
 }
    
    require(
    missingParents.isEmpty()
    ) {
    
     val names =
     missingParents.joinToString {
         it.name
     }

 "SLD nodes without an upstream feeder: $names"
    
    }
    
    /*
    
    * ============================================================
    * FINAL CHILDREN / PARENTS OBJECTS
    * ============================================================
      */
    
    val finalChildren =
    mutableMapOf<
    String,
    MutableList<SldNode>
    >()
    
    val finalParents =
    mutableMapOf<
    String,
    MutableList<SldNode>
    >()
    
    network.nodes.forEach { node ->
    
     finalChildren[node.id] =
     mutableListOf()

 finalParents[node.id] =
     mutableListOf()
    
    }
    
    network.connections.forEach { connection ->
    
     val from =
     nodeMap[
         connection.fromNodeId
     ]!!

 val to =
     nodeMap[
         connection.toNodeId
     ]!!

 finalChildren[
     from.id
 ]!!
     .add(
         to
     )

 finalParents[
     to.id
 ]!!
     .add(
         from
     )
    
    }
    
    /*
    
    * ============================================================
    * FINAL TOPOLOGY
    * ============================================================
    * 
    * No connection has been reversed.
    * 
    * This is intentional.
    * 
    * SldEditorActions establishes direction.
    * SldTopologyEngine validates it.
    * SldUpstreamCalculationEngine calculates it.
    * 
    * This creates one coherent engineering chain:
    * 
    * SLD EDITOR
    *      ↓
    * STORED DIRECTION
    *      ↓
    * TOPOLOGY VALIDATION
    *      ↓
    * UPSTREAM CALCULATION
    
    */
    
    return Topology(
    source =
    source,
    
     connections =
     network.connections,

 children =
     finalChildren.mapValues {
         it.value.toList()
     },

 parents =
     finalParents.mapValues {
         it.value.toList()
     }
    
    )
    }
  
  private enum class VisitState {
  UNVISITED,
  VISITING,
  VISITED
  }
  }
