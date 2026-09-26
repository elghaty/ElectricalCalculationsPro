package com.electrical.calculationspro.data

import kotlin.math.sqrt

/**

* PROFESSIONAL SLD UPSTREAM ENGINE

* 

* Electrical flow:

* 

* SOURCE

* ↓

* TRANSFORMER / GENERATOR

* ↓

* PANEL

* ↓

* BUS / BREAKER

* ↓

* LOAD

* 

* The engine aggregates real and reactive power independently.

* 

* Therefore:

* 

* P = Σ kW

* Q = Σ kVAr

* S = √(P² + Q²)

* PF = P / S

* I = S × 1000 / (√3 × V)

* 

* This avoids the previous error of calculating upstream kVA

* using only the local node power factor.

* 

* Voltage drop is calculated cumulatively through the downstream

* radial path.
  */
  object SldUpstreamEngineering {
  
  data class FeederResult(
  val connectionId: String,
  val fromNodeId: String,
  val toNodeId: String,
  val connectedKw: Double,
  val demandKw: Double,
  val kva: Double,
  val currentA: Double,
  val recommendedBreakerA: Double,
  val voltageDropPercent: Double,
  val cableAdequate: Boolean,
  val notes: List<String>
  )
  
  data class NodeResult(
  val nodeId: String,
  val nodeName: String,
  val connectedKw: Double,
  val demandKw: Double,
  val kva: Double,
  val currentA: Double,
  val recommendedBreakerA: Double,
  val voltageDropPercent: Double,
  val loadingPercent: Double,
  val notes: List<String>
  )
  
  data class Result(
  val sourceNodeId: String,
  val sourceName: String,
  val totalConnectedKw: Double,
  val totalDemandKw: Double,
  val totalKva: Double,
  val sourceCurrentA: Double,
  val recommendedMainBreakerA: Double,
  val recommendedTransformerKva: Double,
  val maximumVoltageDropPercent: Double,
  val nodes: List<NodeResult>,
  val feeders: List<FeederResult>,
  val warnings: List<String>
  )
  
  private data class PowerResult(
  val connectedKw: Double,
  val demandKw: Double,
  val reactiveKvar: Double
  )
  
  private val breakerRatings =
  listOf(
  6.0,
  10.0,
  16.0,
  20.0,
  25.0,
  32.0,
  40.0,
  50.0,
  63.0,
  80.0,
  100.0,
  125.0,
  160.0,
  200.0,
  250.0,
  315.0,
  400.0,
  500.0,
  630.0,
  800.0,
  1000.0,
  1250.0,
  1600.0,
  2000.0,
  2500.0,
  3200.0,
  4000.0,
  5000.0,
  6300.0
  )
  
  private val transformerRatings =
  listOf(
  25.0,
  50.0,
  75.0,
  100.0,
  160.0,
  200.0,
  250.0,
  315.0,
  400.0,
  500.0,
  630.0,
  800.0,
  1000.0,
  1250.0,
  1600.0,
  2000.0,
  2500.0,
  3150.0,
  4000.0,
  5000.0,
  6300.0,
  8000.0,
  10000.0,
  12500.0,
  16000.0,
  20000.0
  )
  
  fun calculate(
  network: SldNetwork
  ): Result {
  
   require(network.nodes.isNotEmpty()) {
     "SLD network is empty."
 }

 validate(network)

 val topology =
     SldTopologyEngine.build(network)

 val nodeMap =
     network.nodes.associateBy {
         it.id
     }

 /*
  * ---------------------------------------------------------
  * DOWNSTREAM POWER AGGREGATION
  * ---------------------------------------------------------
  *
  * Every terminal load contributes:
  *
  * connected kW
  * demand kW
  * demand kVAr
  *
  * kVAr is calculated from the node's actual PF.
  *
  * Passive SLD elements do not contribute load.
  */
 val powerCache =
     mutableMapOf<String, PowerResult>()

 fun calculateNodePower(
     node: SldNode,
     stack: MutableSet<String>
 ): PowerResult {

     powerCache[node.id]?.let {
         return it
     }

     require(
         stack.add(node.id)
     ) {
         "Circular upstream path at ${node.name}."
     }

     val ownConnectedKw =
         when (node.type) {
             SldNodeType.BUS,
             SldNodeType.BREAKER,
             SldNodeType.SOURCE,
             SldNodeType.TRANSFORMER,
             SldNodeType.GENERATOR ->
                 0.0

             else ->
                 node.loadKw.coerceAtLeast(0.0)
         }

     val ownDemandKw =
         when (node.type) {
             SldNodeType.BUS,
             SldNodeType.BREAKER,
             SldNodeType.SOURCE,
             SldNodeType.TRANSFORMER,
             SldNodeType.GENERATOR ->
                 0.0

             else ->
                 ownConnectedKw *
                     node.demandFactor.coerceIn(
                         0.0,
                         1.0
                     )
         }

     val ownReactiveKvar =
         calculateReactivePower(
             activeKw = ownDemandKw,
             powerFactor = node.powerFactor
         )

     var connectedKw =
         ownConnectedKw

     var demandKw =
         ownDemandKw

     var reactiveKvar =
         ownReactiveKvar

     topology.children[node.id]
         .orEmpty()
         .forEach { child ->

             val childResult =
                 calculateNodePower(
                     child,
                     stack
                 )

             connectedKw +=
                 childResult.connectedKw

             demandKw +=
                 childResult.demandKw

             reactiveKvar +=
                 childResult.reactiveKvar
         }

     stack.remove(node.id)

     val result =
         PowerResult(
             connectedKw =
                 connectedKw,

             demandKw =
                 demandKw,

             reactiveKvar =
                 reactiveKvar
         )

     powerCache[node.id] =
         result

     return result
 }

 /*
  * Start at source to calculate the complete network.
  */
 calculateNodePower(
     topology.source,
     mutableSetOf()
 )

 /*
  * Make sure every node has a result.
  *
  * Topology validation already rejects disconnected nodes,
  * but this also protects against future topology changes.
  */
 network.nodes.forEach { node ->
     calculateNodePower(
         node,
         mutableSetOf()
     )
 }

 /*
  * ---------------------------------------------------------
  * CUMULATIVE VOLTAGE DROP
  * ---------------------------------------------------------
  *
  * For each path:
  *
  * VD(total) =
  * VD(section 1) +
  * VD(section 2) +
  * ...
  *
  * The maximum downstream path is reported for each node.
  */
 fun cumulativeVoltageDrop(
     nodeId: String,
     upstreamVoltage: Double
 ): Double {

     val children =
         topology.children[nodeId]
             .orEmpty()

     if (children.isEmpty()) {
         return 0.0
     }

     var maximum =
         0.0

     children.forEach { child ->

         val connection =
             topology.connections.firstOrNull {
                 it.fromNodeId == nodeId &&
                     it.toNodeId == child.id
             }
                 ?: return@forEach

         val childPower =
             powerCache[child.id]
                 ?: return@forEach

         val childKva =
             apparentPower(
                 activeKw =
                     childPower.demandKw,
                 reactiveKvar =
                     childPower.reactiveKvar
             )

         val current =
             threePhaseCurrent(
                 kva =
                     childKva,
                 voltage =
                     upstreamVoltage
             )

         val sectionDrop =
             calculateVoltageDrop(
                 connection =
                     connection,
                 currentA =
                     current,
                 voltage =
                     upstreamVoltage
             )

         val downstreamDrop =
             cumulativeVoltageDrop(
                 nodeId =
                     child.id,
                 upstreamVoltage =
                     child.voltage
                         .takeIf {
                             it > 0.0
                         }
                         ?: upstreamVoltage
             )

         maximum =
             maxOf(
                 maximum,
                 sectionDrop +
                     downstreamDrop
             )
     }

     return maximum
 }

 val nodeResults =
     network.nodes.map { node ->

         val power =
             powerCache[node.id]
                 ?: PowerResult(
                     connectedKw = 0.0,
                     demandKw = 0.0,
                     reactiveKvar = 0.0
                 )

         val kva =
             apparentPower(
                 activeKw =
                     power.demandKw,
                 reactiveKvar =
                     power.reactiveKvar
             )

         val current =
             threePhaseCurrent(
                 kva =
                     kva,
                 voltage =
                     node.voltage
             )

         val breaker =
             nextBreaker(current)

         val voltageDrop =
             cumulativeVoltageDrop(
                 nodeId =
                     node.id,
                 upstreamVoltage =
                     node.voltage
             )

         val loading =
             if (node.ratedKva > 0.0) {
                 kva /
                     node.ratedKva *
                     100.0
             } else {
                 0.0
             }

         val calculatedPf =
             if (kva > 0.0) {
                 power.demandKw /
                     kva
             } else {
                 1.0
             }

         NodeResult(
             nodeId =
                 node.id,

             nodeName =
                 node.name,

             connectedKw =
                 power.connectedKw,

             demandKw =
                 power.demandKw,

             kva =
                 kva,

             currentA =
                 current,

             recommendedBreakerA =
                 breaker,

             voltageDropPercent =
                 voltageDrop,

             loadingPercent =
                 loading,

             notes =
                 buildList {

                     add(
                         "Connected load = " +
                             "%.2f kW".format(
                                 power.connectedKw
                             )
                     )

                     add(
                         "Demand load = " +
                             "%.2f kW".format(
                                 power.demandKw
                             )
                     )

                     add(
                         "Required apparent power = " +
                             "%.2f kVA".format(
                                 kva
                             )
                     )

                     add(
                         "Calculated demand PF = " +
                             "%.3f".format(
                                 calculatedPf
                             )
                     )

                     add(
                         "Design current = " +
                             "%.2f A".format(
                                 current
                             )
                     )

                     add(
                         "Recommended breaker = " +
                             "%.0f A".format(
                                 breaker
                             )
                     )

                     if (node.ratedKva > 0.0) {
                         add(
                             "Equipment loading = " +
                                 "%.1f %%".format(
                                     loading
                                 )
                         )
                     }

                     add(
                         "Maximum downstream voltage drop = " +
                             "%.2f %%".format(
                                 voltageDrop
                             )
                     )
                 }
         )
     }

 val nodeResultMap =
     nodeResults.associateBy {
         it.nodeId
     }

 /*
  * ---------------------------------------------------------
  * FEEDER RESULTS
  * ---------------------------------------------------------
  */
 val feederResults =
     topology.connections.map { connection ->

         val child =
             nodeMap[
                 connection.toNodeId
             ]

         val childResult =
             nodeResultMap[
                 connection.toNodeId
             ]

         val connected =
             childResult?.connectedKw
                 ?: 0.0

         val demand =
             childResult?.demandKw
                 ?: 0.0

         val kva =
             childResult?.kva
                 ?: 0.0

         val current =
             childResult?.currentA
                 ?: 0.0

         val voltage =
             child?.voltage
                 ?.takeIf {
                     it > 0.0
                 }
                 ?: 400.0

         val sectionDrop =
             calculateVoltageDrop(
                 connection =
                     connection,
                 currentA =
                     current,
                 voltage =
                     voltage
             )

         val adequate =
             connection.currentCapacityA <= 0.0 ||
                 connection.currentCapacityA >= current

         FeederResult(
             connectionId =
                 connection.id,

             fromNodeId =
                 connection.fromNodeId,

             toNodeId =
                 connection.toNodeId,

             connectedKw =
                 connected,

             demandKw =
                 demand,

             kva =
                 kva,

             currentA =
                 current,

             recommendedBreakerA =
                 nextBreaker(current),

             voltageDropPercent =
                 sectionDrop,

             cableAdequate =
                 adequate,

             notes =
                 buildList {

                     add(
                         "Connected load = " +
                             "%.2f kW".format(
                                 connected
                             )
                     )

                     add(
                         "Demand load = " +
                             "%.2f kW".format(
                                 demand
                             )
                     )

                     add(
                         "Required apparent power = " +
                             "%.2f kVA".format(
                                 kva
                             )
                     )

                     add(
                         "Design current = " +
                             "%.2f A".format(
                                 current
                             )
                     )

                     if (
                         connection.currentCapacityA > 0.0
                     ) {
                         add(
                             "Cable capacity = " +
                                 "%.2f A".format(
                                     connection.currentCapacityA
                                 )
                         )
                     }

                     add(
                         "Section voltage drop = " +
                             "%.2f %%".format(
                                 sectionDrop
                             )
                     )

                     if (!adequate) {
                         add(
                             "WARNING: cable capacity is below design current."
                         )
                     }
                 }
         )
     }

 val sourceResult =
     nodeResultMap[
         topology.source.id
     ]
         ?: error(
             "Unable to calculate source engineering result."
         )

 val recommendedTransformer =
     if (
         topology.source.type ==
             SldNodeType.TRANSFORMER &&
         topology.source.ratedKva > 0.0
     ) {
         topology.source.ratedKva
     } else {
         nextTransformer(
             sourceResult.kva
         )
     }

 /*
  * ---------------------------------------------------------
  * WARNINGS
  * ---------------------------------------------------------
  */
 val warnings =
     buildList {

         feederResults
             .filter {
                 !it.cableAdequate
             }
             .forEach {
                 add(
                     "Cable capacity warning at connection ${it.connectionId}."
                 )
             }

         nodeResults
             .filter {
                 it.loadingPercent > 100.0
             }
             .forEach {
                 add(
                     "Equipment overload at ${it.nodeName}."
                 )
             }

         nodeResults
             .filter {
                 it.voltageDropPercent > 3.0
             }
             .forEach {
                 add(
                     "Voltage drop above 3% at ${it.nodeName}."
                 )
             }

         if (
             sourceResult.kva > 0.0 &&
             recommendedTransformer <
             sourceResult.kva
         ) {
             add(
                 "Recommended transformer capacity is below calculated demand."
             )
         }
     }

 return Result(
     sourceNodeId =
         topology.source.id,

     sourceName =
         topology.source.name,

     totalConnectedKw =
         sourceResult.connectedKw,

     totalDemandKw =
         sourceResult.demandKw,

     totalKva =
         sourceResult.kva,

     sourceCurrentA =
         sourceResult.currentA,

     recommendedMainBreakerA =
         sourceResult.recommendedBreakerA,

     recommendedTransformerKva =
         recommendedTransformer,

     maximumVoltageDropPercent =
         nodeResults.maxOfOrNull {
             it.voltageDropPercent
         } ?: 0.0,

     nodes =
         nodeResults,

     feeders =
         feederResults,

     warnings =
         warnings
 )
  
  }
  
  /**
  
  * Q = P × tan(acos(PF))
    */
    private fun calculateReactivePower(
    activeKw: Double,
    powerFactor: Double
    ): Double {
    
    if (activeKw <= 0.0) {
    return 0.0
    }
    
    val pf =
    powerFactor.coerceIn(
    0.01,
    1.0
    )
    
    val angle =
    kotlin.math.acos(pf)
    
    return activeKw *
    kotlin.math.tan(angle)
    }
  
  /**
  
  * S = √(P² + Q²)
    */
    private fun apparentPower(
    activeKw: Double,
    reactiveKvar: Double
    ): Double {
    
    if (
    activeKw <= 0.0 &&
    reactiveKvar <= 0.0
    ) {
    return 0.0
    }
    
    return sqrt(
    activeKw * activeKw +
    reactiveKvar * reactiveKvar
    )
    }
  
  /**
  
  * Three-phase current:
  
  * 
  
  * I = S × 1000 / (√3 × V)
    */
    private fun threePhaseCurrent(
    kva: Double,
    voltage: Double
    ): Double {
    
    if (
    kva <= 0.0 ||
    voltage <= 0.0
    ) {
    return 0.0
    }
    
    return (
    kva * 1000.0
    ) / (
    sqrt(3.0) *
    voltage
    )
    }
  
  /**
  
  * Voltage drop using:
  
  * 
  
  * ΔV = √3 × I × Z × L
  
  * 
  
  * where R and X are given in ohm/km.
    */
    private fun calculateVoltageDrop(
    connection: SldConnection,
    currentA: Double,
    voltage: Double
    ): Double {
    
    if (
    connection.lengthMeters <= 0.0 ||
    currentA <= 0.0 ||
    voltage <= 0.0
    ) {
    return 0.0
    }
    
    val runs =
    connection.parallelRuns
    .coerceAtLeast(1)
    
    val resistance =
    connection.resistanceOhmPerKm
    .coerceAtLeast(0.0) /
    runs
    
    val reactance =
    connection.reactanceOhmPerKm
    .coerceAtLeast(0.0) /
    runs
    
    val impedance =
    sqrt(
    resistance * resistance +
    reactance * reactance
    )
    
    val dropVolts =
    sqrt(3.0) *
    currentA *
    impedance *
    connection.lengthMeters /
    1000.0
    
    return (
    dropVolts /
    voltage
    ) * 100.0
    }
  
  private fun nextBreaker(
  current: Double
  ): Double {
  
   if (current <= 0.0) {
     return 0.0
 }

 return breakerRatings.firstOrNull {
     it >= current
 } ?: breakerRatings.last()
  
  }
  
  private fun nextTransformer(
  kva: Double
  ): Double {
  
   if (kva <= 0.0) {
     return 0.0
 }

 return transformerRatings.firstOrNull {
     it >= kva
 } ?: transformerRatings.last()
  
  }
  
  private fun validate(
  network: SldNetwork
  ) {
  
   val ids =
     network.nodes.map {
         it.id
     }

 require(
     ids.size ==
         ids.toSet().size
 ) {
     "Duplicate SLD node IDs."
 }

 network.nodes.forEach { node ->

     require(
         node.voltage > 0.0
     ) {
         "Invalid voltage at ${node.name}."
     }

     require(
         node.powerFactor > 0.0 &&
             node.powerFactor <= 1.0
     ) {
         "Invalid power factor at ${node.name}."
     }

     require(
         node.demandFactor >= 0.0 &&
             node.demandFactor <= 1.0
     ) {
         "Invalid demand factor at ${node.name}."
     }

     require(
         node.loadKw >= 0.0
     ) {
         "Invalid load at ${node.name}."
     }
 }

 network.connections.forEach { connection ->

     require(
         connection.fromNodeId in ids
     ) {
         "Connection ${connection.id}: source node does not exist."
     }

     require(
         connection.toNodeId in ids
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

     require(
         connection.lengthMeters >= 0.0
     ) {
         "Connection ${connection.id}: cable length cannot be negative."
     }

     require(
         connection.resistanceOhmPerKm >= 0.0
     ) {
         "Connection ${connection.id}: cable resistance cannot be negative."
     }

     require(
         connection.reactanceOhmPerKm >= 0.0
     ) {
         "Connection ${connection.id}: cable reactance cannot be negative."
     }

     require(
         connection.currentCapacityA >= 0.0
     ) {
         "Connection ${connection.id}: cable current capacity cannot be negative."
     }
 }
  
  }
  }
