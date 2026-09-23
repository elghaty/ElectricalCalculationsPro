package com.electrical.calculationspro.data

/**

* ================================================================

* PROFESSIONAL SLD ENGINEERING REPORT ENGINE

* ================================================================

* 

* Converts the complete engineering calculation package into

* a structured engineering report.

* 

* The report layer contains no Compose/UI logic and can later

* be exported to:

* 

* - PDF

* - Excel

* - Word

* - Project database

* 

* Engineering flow:

* 

* SLD Network

*  ↓

* Validation

*  ↓

* Upstream Load Aggregation

*  ↓

* Short Circuit

*  ↓

* Cable Sizing

*  ↓

* Protection Coordination

*  ↓

* Panel Schedule

*  ↓

* Engineering Report
  */
  object SldEngineeringReportEngine {
  
  data class ReportSection(
  val title: String,
  val lines: List<String>
  )
  
  data class Report(
  val title: String,
  val sections: List<ReportSection>
  ) {
  
   fun asText(): String {
     return buildString {

         appendLine(title)
         appendLine(
             "================================================"
         )

         sections.forEachIndexed { index, section ->

             appendLine()
             appendLine(
                 "${index + 1}. ${section.title}"
             )

             appendLine(
                 "------------------------------------------------"
             )

             section.lines.forEach { line ->
                 appendLine(line)
             }
         }
     }
 }
  
  }
  
  fun build(
  network: SldNetwork,
  engineering: SldEngineeringPackage
  ): Report {
  
   val sections = mutableListOf<ReportSection>()

 /*
  * ============================================================
  * 1. NETWORK SUMMARY
  * ============================================================
  */
 sections += ReportSection(
     title = "Network Summary",
     lines = listOf(
         "Equipment count = ${network.nodes.size}",
         "Feeder count = ${network.connections.size}",
         "Source count = ${
             network.nodes.count {
                 it.type == SldNodeType.SOURCE
             }
         }",
         "Transformer count = ${
             network.nodes.count {
                 it.type == SldNodeType.TRANSFORMER
             }
         }",
         "Generator count = ${
             network.nodes.count {
                 it.type == SldNodeType.GENERATOR
             }
         }",
         "Panel count = ${
             network.nodes.count {
                 it.type == SldNodeType.PANEL
             }
         }",
         "Breaker count = ${
             network.nodes.count {
                 it.type == SldNodeType.BREAKER
             }
         }",
         "Load count = ${
             network.nodes.count {
                 it.type == SldNodeType.LOAD
             }
         }"
     )
 )

 /*
  * ============================================================
  * 2. UPSTREAM ENGINEERING
  * ============================================================
  */
 val upstream = engineering.upstream

 val upstreamLines = mutableListOf<String>()

 upstreamLines +=
     "Source = ${upstream.sourceName}"

 upstreamLines +=
     "Total connected load = %.2f kW".format(
         upstream.totalConnectedKw
     )

 upstreamLines +=
     "Total demand load = %.2f kW".format(
         upstream.totalDemandKw
     )

 upstreamLines +=
     "Total demand apparent power = %.2f kVA".format(
         upstream.totalKva
     )

 upstreamLines +=
     "Source current = %.2f A".format(
         upstream.sourceCurrentA
     )

 upstreamLines +=
     "Recommended main breaker = %.0f A".format(
         upstream.recommendedMainBreakerA
     )

 upstreamLines +=
     "Recommended transformer capacity = %.0f kVA".format(
         upstream.recommendedTransformerKva
     )

 upstreamLines +=
     "Maximum voltage drop = %.3f %%".format(
         upstream.maximumVoltageDropPercent
     )

 if (upstream.nodes.isNotEmpty()) {

     upstreamLines += ""
     upstreamLines += "Node engineering results:"

     upstream.nodes.forEach { node ->

         upstreamLines +=
             "Node: ${node.nodeName}"

         upstreamLines +=
             "  Connected load = %.2f kW".format(
                 node.connectedKw
             )

         upstreamLines +=
             "  Demand load = %.2f kW".format(
                 node.demandKw
             )

         upstreamLines +=
             "  Apparent power = %.2f kVA".format(
                 node.kva
             )

         upstreamLines +=
             "  Current = %.2f A".format(
                 node.currentA
             )

         upstreamLines +=
             "  Recommended breaker = %.0f A".format(
                 node.recommendedBreakerA
             )

         upstreamLines +=
             "  Voltage drop = %.3f %%".format(
                 node.voltageDropPercent
             )

         upstreamLines +=
             "  Loading = %.2f %%".format(
                 node.loadingPercent
             )

         node.notes.forEach { note ->
             upstreamLines +=
                 "  Note: $note"
         }
     }
 }

 if (upstream.feeders.isNotEmpty()) {

     upstreamLines += ""
     upstreamLines += "Feeder engineering results:"

     upstream.feeders.forEach { feeder ->

         upstreamLines +=
             "Feeder: ${feeder.fromNodeId} → ${feeder.toNodeId}"

         upstreamLines +=
             "  Connected load = %.2f kW".format(
                 feeder.connectedKw
             )

         upstreamLines +=
             "  Demand load = %.2f kW".format(
                 feeder.demandKw
             )

         upstreamLines +=
             "  Apparent power = %.2f kVA".format(
                 feeder.kva
             )

         upstreamLines +=
             "  Current = %.2f A".format(
                 feeder.currentA
             )

         upstreamLines +=
             "  Recommended breaker = %.0f A".format(
                 feeder.recommendedBreakerA
             )

         upstreamLines +=
             "  Voltage drop = %.3f %%".format(
                 feeder.voltageDropPercent
             )

         upstreamLines +=
             "  Cable adequate = ${feeder.cableAdequate}"

         feeder.notes.forEach { note ->
             upstreamLines +=
                 "  Note: $note"
         }
     }
 }

 upstream.warnings.forEach { warning ->
     upstreamLines += "Warning: $warning"
 }

 sections += ReportSection(
     title = "Upstream Load & Feeder Engineering",
     lines = upstreamLines
 )

 /*
  * ============================================================
  * 3. SHORT CIRCUIT
  * ============================================================
  */
 sections += ReportSection(
     title = "Short Circuit",
     lines =
         listOf(
             "Maximum fault current = %.3f kA".format(
                 engineering
                     .shortCircuit
                     .maximumFaultCurrentKa
             ),
             "Maximum peak current = %.3f kA".format(
                 engineering
                     .shortCircuit
                     .maximumPeakCurrentKa
             ),
             "Maximum fault level = %.3f MVA".format(
                 engineering
                     .shortCircuit
                     .maximumFaultMva
             )
         ) +
             engineering
                 .shortCircuit
                 .notes
                 .map {
                     "Note: $it"
                 }
 )

 /*
  * ============================================================
  * 4. CABLE SIZING
  * ============================================================
  */
 sections += ReportSection(
     title = "Cable Sizing",
     lines =
         listOf(
             "Successful feeders = ${
                 engineering
                     .cableSizing
                     .successfulFeeders
             }",
             "Feeders requiring review = ${
                 engineering
                     .cableSizing
                     .failedFeeders
             }"
         ) +
             engineering
                 .cableSizing
                 .notes
                 .map {
                     "Note: $it"
                 }
 )

 /*
  * ============================================================
  * 5. PROTECTION COORDINATION
  * ============================================================
  */
 sections += ReportSection(
     title = "Protection Coordination",
     lines =
         listOf(
             "Coordinated pairs = ${
                 engineering
                     .protectionCoordination
                     .coordinatedPairs
             }",
             "Warning pairs = ${
                 engineering
                     .protectionCoordination
                     .warningPairs
             }",
             "Failed pairs = ${
                 engineering
                     .protectionCoordination
                     .failedPairs
             }"
         ) +
             engineering
                 .protectionCoordination
                 .notes
                 .map {
                     "Note: $it"
                 }
 )

 /*
  * ============================================================
  * 6. PANEL SCHEDULE
  * ============================================================
  */
 engineering.panelSchedule?.let { panel ->

     sections += ReportSection(
         title = "Panel Schedule",
         lines =
             listOf(
                 "Panel = ${panel.panelName}",
                 "Connected load = %.2f kW".format(
                     panel.totalConnectedLoadKw
                 ),
                 "Demand load = %.2f kW".format(
                     panel.totalDemandLoadKw
                 ),
                 "Demand current = %.2f A".format(
                     panel.totalDemandCurrentA
                 )
             ) +
                 panel.notes.map {
                     "Note: $it"
                 }
     )
 }

 /*
  * ============================================================
  * 7. ENGINEERING WARNINGS
  * ============================================================
  */
 val globalWarnings = mutableListOf<String>()

 globalWarnings += upstream.warnings

 if (
     engineering
         .shortCircuit
         .maximumFaultCurrentKa <= 0.0
 ) {
     globalWarnings +=
         "Short-circuit study did not produce a positive fault current."
 }

 if (
     engineering
         .cableSizing
         .failedFeeders > 0
 ) {
     globalWarnings +=
         "${engineering.cableSizing.failedFeeders} feeder(s) require cable sizing review."
 }

 if (
     engineering
         .protectionCoordination
         .failedPairs > 0
 ) {
     globalWarnings +=
         "${engineering.protectionCoordination.failedPairs} protection pair(s) require coordination review."
 }

 if (globalWarnings.isNotEmpty()) {

     sections += ReportSection(
         title = "Engineering Warnings",
         lines = globalWarnings
             .distinct()
             .map {
                 "WARNING: $it"
             }
     )
 }

 /*
  * ============================================================
  * FINAL REPORT
  * ============================================================
  */
 return Report(
     title = "Electrical SLD Engineering Report",
     sections = sections
 )
  
  }
  }
