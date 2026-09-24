package com.electrical.calculationspro.data

enum class SldNodeType {
    SOURCE,
    TRANSFORMER,
    GENERATOR,
    BUS,
    PANEL,
    BREAKER,
    LOAD
}

data class SldNode(
    val id: String,
    val name: String,
    val type: SldNodeType,
    val x: Float,
    val y: Float,

    val voltage: Double = 400.0,

    val loadKw: Double = 0.0,

    val powerFactor: Double = 0.90,

    val demandFactor: Double = 1.0,

    val ratedKva: Double = 0.0,

    val transformerPercentZ: Double = 0.0,

    val generatorXdSubtransient: Double = 0.0,

    val sourceShortCircuitMva: Double = 0.0
)

data class SldConnection(
    val id: String,

    val fromNodeId: String,

    val toNodeId: String,

    val lengthMeters: Double = 0.0,

    val resistanceOhmPerKm: Double = 0.0,

    val reactanceOhmPerKm: Double = 0.0,

    val cableSizeMm2: Double = 0.0,

    val parallelRuns: Int = 1,

    val voltageDropPercent: Double = 0.0,

    val currentCapacityA: Double = 0.0
)

data class SldNetwork(
    val nodes: List<SldNode> = emptyList(),
    val connections: List<SldConnection> = emptyList()
)

data class UpstreamResult(
    val nodeId: String,
    val nodeName: String,

    val connectedLoadKw: Double,
    val demandLoadKw: Double,

    val apparentPowerKva: Double,

    val currentA: Double,

    val voltage: Double,

    val requiredBreakerA: Double,

    val requiredTransformerKva: Double,

    val diversityFactor: Double,

    val childrenCount: Int,

    val voltageDropPercent: Double = 0.0,

    val feederRequiredCurrentA: Double = 0.0,

    val notes: List<String> = emptyList()
)

data class SldCalculationResult(
    val nodeResults: Map<String, UpstreamResult> = emptyMap(),

    val totalConnectedLoadKw: Double = 0.0,

    val totalDemandLoadKw: Double = 0.0,

    val totalRequiredKva: Double = 0.0,

    val mainCurrentA: Double = 0.0,

    val mainBreakerA: Double = 0.0,

    val requiredTransformerKva: Double = 0.0,

    val totalVoltageDropPercent: Double = 0.0,

    val notes: List<String> = emptyList()
)
