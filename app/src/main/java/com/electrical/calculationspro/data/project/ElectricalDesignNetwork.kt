package com.electrical.calculationspro.data.project

import java.util.UUID

/**

* Professional electrical design network model.
* 
* This file contains design data only.
* Engineering calculations remain in the Engineering Core.
* 
* Topology:
* 
* Source
* ↓
* Transformer / Generator
* ↓
* Main Panel
* ↓
* Feeder
* ↓
* Sub Panel
* ↓
* Feeder
* ↓
* Load / Motor
  */
  enum class ElectricalSourceType {
  UTILITY,
  TRANSFORMER,
  GENERATOR,
  PANEL
  }

enum class ElectricalEquipmentType {
SOURCE,
TRANSFORMER,
GENERATOR,
PANEL,
LOAD,
MOTOR
}

enum class ElectricalFeederType {
INCOMING,
OUTGOING,
INTER_PANEL,
LOAD_FEEDER,
MOTOR_FEEDER
}

/**

* Root electrical design network.

* 

* This model is intentionally calculation-free.

* It represents the actual electrical topology.
  */
  data class ElectricalDesignNetwork(
  
  val id: String =
  UUID.randomUUID().toString(),
  
  val name: String =
  "Main Electrical Network",
  
  val voltageV: Double =
  400.0,
  
  val frequencyHz: Double =
  50.0,
  
  val phases: Int =
  3,
  
  val sourceNodes: List<ElectricalSourceNode> =
  emptyList(),
  
  val transformerNodes: List<ElectricalTransformerNode> =
  emptyList(),
  
  val generatorNodes: List<ElectricalGeneratorNode> =
  emptyList(),
  
  val panels: List<ElectricalDesignPanelNode> =
  emptyList(),
  
  val loads: List<ElectricalDesignLoadNode> =
  emptyList(),
  
  val feeders: List<ElectricalDesignFeeder> =
  emptyList(),
  
  val status: DesignCalculationStatus =
  DesignCalculationStatus.NOT_STARTED
  )

/**

* Electrical source.

* 

* A source may represent:

* - Utility

* - Transformer

* - Generator

* - Another panel
    */
    data class ElectricalSourceNode(
  
  val id: String =
  UUID.randomUUID().toString(),
  
  val name: String,
  
  val sourceType: ElectricalSourceType,
  
  val voltageV: Double =
  400.0,
  
  val phases: Int =
  3,
  
  val frequencyHz: Double =
  50.0,
  
  /**
  
  * Reference to an existing project equipment,
  * when applicable.
    */
    val equipmentId: String? =
    null,
  
  /**
  
  * Optional upstream source reference.
    */
    val upstreamSourceId: String? =
    null,
  
  val status: DesignCalculationStatus =
  DesignCalculationStatus.NOT_STARTED
  )

/**

* Transformer design object.
  */
  data class ElectricalTransformerNode(
  
  val id: String =
  UUID.randomUUID().toString(),
  
  val name: String,
  
  val ratingKva: Double =
  0.0,
  
  val primaryVoltageV: Double =
  11000.0,
  
  val secondaryVoltageV: Double =
  400.0,
  
  val impedancePercent: Double =
  0.0,
  
  val frequencyHz: Double =
  50.0,
  
  val sourceId: String? =
  null,
  
  val status: DesignCalculationStatus =
  DesignCalculationStatus.NOT_STARTED
  )

/**

* Generator design object.
  */
  data class ElectricalGeneratorNode(
  
  val id: String =
  UUID.randomUUID().toString(),
  
  val name: String,
  
  val ratingKva: Double =
  0.0,
  
  val voltageV: Double =
  400.0,
  
  val powerFactor: Double =
  0.80,
  
  val frequencyHz: Double =
  50.0,
  
  val sourceId: String? =
  null,
  
  val status: DesignCalculationStatus =
  DesignCalculationStatus.NOT_STARTED
  )

/**

* Actual electrical panel in the design network.
  */
  data class ElectricalDesignPanelNode(
  
  val id: String =
  UUID.randomUUID().toString(),
  
  val name: String,
  
  val description: String =
  "",
  
  val panelType: String =
  "DB",
  
  val voltageV: Double =
  400.0,
  
  val phases: Int =
  3,
  
  val frequencyHz: Double =
  50.0,
  
  /**
  
  * Upstream object.
  * 
  * Can reference:
  * - source
  * - transformer
  * - generator
  * - another panel
      */
      val upstreamNodeId: String? =
      null,
  
  val incomingFeederId: String? =
  null,
  
  val mainBreakerId: String? =
  null,
  
  val busbarRatingA: Double =
  0.0,
  
  val designLoadKw: Double =
  0.0,
  
  val designCurrentA: Double =
  0.0,
  
  val shortCircuitKA: Double =
  0.0,
  
  val status: DesignCalculationStatus =
  DesignCalculationStatus.NOT_STARTED
  )

/**

* Actual electrical load in the design.
  */
  data class ElectricalDesignLoadNode(
  
  val id: String =
  UUID.randomUUID().toString(),
  
  val name: String,
  
  val description: String =
  "",
  
  val loadType: String =
  "GENERAL",
  
  val quantity: Int =
  1,
  
  val connectedLoadKw: Double =
  0.0,
  
  val demandFactor: Double =
  1.0,
  
  val diversityFactor: Double =
  1.0,
  
  val powerFactor: Double =
  0.90,
  
  val voltageV: Double =
  400.0,
  
  val phases: Int =
  3,
  
  /**
  
  * Panel supplying this load.
    */
    val sourcePanelId: String? =
    null,
  
  /**
  
  * Reference to equipment from another discipline.
  * 
  * Example:
  * 
  * Water Pump
  *  ↓
  * Electrical Design Load
    */
    val sourceEquipmentId: String? =
    null,
  
  val sourceSystem: String? =
  null,
  
  /**
  
  * Engineering results populated by the Core.
    */
    val designLoadKw: Double =
    0.0,
  
  val designCurrentA: Double =
  0.0,
  
  val status: DesignCalculationStatus =
  DesignCalculationStatus.NOT_STARTED
  )

/**

* Actual feeder / cable route.

* 

* Examples:

* 

* MDB → SMDB

* 

* Pump Panel → Pump
  */
  data class ElectricalDesignFeeder(
  
  val id: String =
  UUID.randomUUID().toString(),
  
  val name: String,
  
  val feederType: ElectricalFeederType =
  ElectricalFeederType.OUTGOING,
  
  /**
  
  * Source node.
    */
    val fromNodeId: String,
  
  /**
  
  * Destination node.
    */
    val toNodeId: String,
  
  /**
  
  * Physical cable route length.
    */
    val lengthM: Double =
    0.0,
  
  val installationMethod: String =
  "",
  
  val cableMaterial: String =
  "COPPER",
  
  val insulation: String =
  "XLPE",
  
  val cores: Int =
  0,
  
  val sectionMm2: Double =
  0.0,
  
  val parallelRuns: Int =
  1,
  
  /**
  
  * Engineering results populated by the Core.
    */
    val designCurrentA: Double =
    0.0,
  
  val ampacityA: Double =
  0.0,
  
  val voltageDropPercent: Double =
  0.0,
  
  val shortCircuitWithstandKA: Double =
  0.0,
  
  val breakerId: String? =
  null,
  
  val protectionId: String? =
  null,
  
  val status: DesignCalculationStatus =
  DesignCalculationStatus.NOT_STARTED
  )

/**

* Lightweight engineering reference.

* 

* Used for:

* - design navigation

* - SLD generation

* - engineering traceability
    */
    data class ElectricalNetworkReference(
  
  val id: String =
  UUID.randomUUID().toString(),
  
  val type: ElectricalEquipmentType,
  
  val elementId: String,
  
  val name: String
  )
