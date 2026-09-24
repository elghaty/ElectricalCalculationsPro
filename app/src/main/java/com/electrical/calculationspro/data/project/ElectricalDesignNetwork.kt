package com.electrical.calculationspro.data.project

import java.util.UUID

/**

================================================================ PROFESSIONAL ELECTRICAL DESIGN NETWORK ================================================================ This file contains the engineering DESIGN MODEL only. It does not perform calculations. It does not contain electrical equations. The network represents the actual designed system: Source ↓ Transformer / Generator ↓ Main Panel ↓ Feeder ↓ Sub Panel ↓ Feeder ↓ Load / Motor Calculations are performed by the existing Engineering Core. ================================================================ */ 

enum class ElectricalSourceType { UTILITY, TRANSFORMER, GENERATOR, PANEL }

enum class ElectricalEquipmentType { SOURCE, TRANSFORMER, GENERATOR, PANEL, LOAD, MOTOR }

enum class ElectricalFeederType { INCOMING, OUTGOING, INTER_PANEL, LOAD_FEEDER, MOTOR_FEEDER }

/**

Root electrical design network. */ data class ElectricalDesignNetwork( val id: String = UUID.randomUUID().toString(),

val name: String = "Main Electrical Network",

val voltageV: Double = 400.0,

val frequencyHz: Double = 50.0,

val phases: Int = 3,

val sourceNodes: List = emptyList(),

val transformerNodes: List = emptyList(),

val generatorNodes: List = emptyList(),

val panels: List = emptyList(),

val loads: List = emptyList(),

val feeders: List = emptyList(),

val status: DesignCalculationStatus = DesignCalculationStatus.NOT_STARTED )

/**

Electrical source.

A source may be:

Utility

Transformer

Generator

or another Panel. */ data class ElectricalSourceNode( val id: String = UUID.randomUUID().toString(),

val name: String,

val sourceType: ElectricalSourceType,

val voltageV: Double = 400.0,

val phases: Int = 3,

val frequencyHz: Double = 50.0,

/**

Reference to an existing project equipment where applicable. */ val equipmentId: String? = null, 

val upstreamSourceId: String? = null,

val status: DesignCalculationStatus = DesignCalculationStatus.NOT_STARTED )

/**

Transformer as a design object. */ data class ElectricalTransformerNode( val id: String = UUID.randomUUID().toString(),

val name: String,

val ratingKva: Double = 0.0,

val primaryVoltageV: Double = 11000.0,

val secondaryVoltageV: Double = 400.0,

val impedancePercent: Double = 0.0,

val frequencyHz: Double = 50.0,

val sourceId: String? = null,

val status: DesignCalculationStatus = DesignCalculationStatus.NOT_STARTED )

/**

Generator as a design object. */ data class ElectricalGeneratorNode( val id: String = UUID.randomUUID().toString(),

val name: String,

val ratingKva: Double = 0.0,

val voltageV: Double = 400.0,

val powerFactor: Double = 0.80,

val frequencyHz: Double = 50.0,

val sourceId: String? = null,

val status: DesignCalculationStatus = DesignCalculationStatus.NOT_STARTED )

/**

Design panel.

This is the actual panel in the electrical network,

not merely a panel calculation result. */ data class ElectricalDesignPanelNode( val id: String = UUID.randomUUID().toString(),

val name: String,

val description: String = "",

val panelType: String = "DB",

val voltageV: Double = 400.0,

val phases: Int = 3,

val frequencyHz: Double = 50.0,

/**

Upstream object. This can point to: source transformer generator another panel */ val upstreamNodeId: String? = null, 

val incomingFeederId: String? = null,

val mainBreakerId: String? = null,

val busbarRatingA: Double = 0.0,

val designLoadKw: Double = 0.0,

val designCurrentA: Double = 0.0,

val shortCircuitKA: Double = 0.0,

val status: DesignCalculationStatus = DesignCalculationStatus.NOT_STARTED )

/**

Actual electrical load in the design. */ data class ElectricalDesignLoadNode( val id: String = UUID.randomUUID().toString(),

val name: String,

val description: String = "",

val loadType: String = "GENERAL",

val quantity: Int = 1,

val connectedLoadKw: Double = 0.0,

val demandFactor: Double = 1.0,

val diversityFactor: Double = 1.0,

val powerFactor: Double = 0.90,

val voltageV: Double = 400.0,

val phases: Int = 3,

/**

Panel supplying this load. */ val sourcePanelId: String? = null, 

/**

Reference to equipment from another discipline. Example: Water Pump ↓ Electrical Design Load */ val sourceEquipmentId: String? = null, 

val sourceSystem: String? = null,

/**

Engineering results. These are populated by the Engineering Core. */ val designLoadKw: Double = 0.0, 

val designCurrentA: Double = 0.0,

val status: DesignCalculationStatus = DesignCalculationStatus.NOT_STARTED )

/**

Actual feeder / cable route in the design.

Example:

MDB → SMDB

or:

Pump Panel → Pump */ data class ElectricalDesignFeeder( val id: String = UUID.randomUUID().toString(),

val name: String,

val feederType: ElectricalFeederType = ElectricalFeederType.OUTGOING,

/**

Source node in the electrical network. */ val fromNodeId: String, 

/**

Destination node in the electrical network. */ val toNodeId: String, 

/**

Physical cable route. */ val lengthM: Double = 0.0, 

val installationMethod: String = "",

val cableMaterial: String = "COPPER",

val insulation: String = "XLPE",

val cores: Int = 0,

val sectionMm2: Double = 0.0,

val parallelRuns: Int = 1,

/**

Engineering results populated by Core. */ val designCurrentA: Double = 0.0, 

val ampacityA: Double = 0.0,

val voltageDropPercent: Double = 0.0,

val shortCircuitWithstandKA: Double = 0.0,

val breakerId: String? = null,

val protectionId: String? = null,

val status: DesignCalculationStatus = DesignCalculationStatus.NOT_STARTED )

/**

A lightweight reference used by design navigation,

SLD generation and engineering traceability. */ data class ElectricalNetworkReference( val id: String = UUID.randomUUID().toString(),

val type: ElectricalEquipmentType,

val elementId: String,

val name: String )


