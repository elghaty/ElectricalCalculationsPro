package com.electrical.calculationspro.data

import kotlin.math.sqrt

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
    val reactanceOhmPerKm: Double = 0.0
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

    val notes: List<String> = emptyList()
)

data class SldCalculationResult(
    val nodeResults: Map<String, UpstreamResult>,
    val totalConnectedLoadKw: Double,
    val totalDemandLoadKw: Double,
    val totalRequiredKva: Double,
    val mainCurrentA: Double,
    val mainBreakerA: Double,
    val requiredTransformerKva: Double,
    val notes: List<String>
)

object SldEngineeringEngine {

    private const val EPSILON = 1.0e-9

    private val standardBreakers = listOf(
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
        4000.0
    )

    private val standardTransformersKva = listOf(
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
        5000.0
    )

    fun calculateUpstream(
        network: SldNetwork
    ): SldCalculationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        validateNetwork(network)

        val children = mutableMapOf<String, MutableList<SldNode>>()

        network.nodes.forEach { node ->
            children[node.id] = mutableListOf()
        }

        network.connections.forEach { connection ->

            val child = network.nodes.firstOrNull {
                it.id == connection.toNodeId
            }

            if (child != null) {
                children[connection.fromNodeId]?.add(child)
            }
        }

        val resultMap = mutableMapOf<String, UpstreamResult>()

        fun calculateNode(
            node: SldNode,
            visiting: MutableSet<String>
        ): Pair<Double, Double> {

            if (!visiting.add(node.id)) {
                throw IllegalArgumentException(
                    "SLD contains a circular connection around ${node.name}."
                )
            }

            val nodeChildren =
                children[node.id].orEmpty()

            if (node.type == SldNodeType.LOAD) {

                val connectedKw =
                    node.loadKw.coerceAtLeast(0.0)

                val demandKw =
                    connectedKw *
                        node.demandFactor.coerceIn(
                            0.0,
                            1.0
                        )

                val kva =
                    if (
                        node.powerFactor >
                        EPSILON
                    ) {
                        demandKw /
                            node.powerFactor.coerceIn(
                                0.01,
                                1.0
                            )
                    } else {
                        demandKw
                    }

                val current =
                    threePhaseCurrent(
                        kva = kva,
                        voltage = node.voltage
                    )

                val breaker =
                    nextStandardBreaker(
                        current
                    )

                resultMap[node.id] =
                    UpstreamResult(
                        nodeId = node.id,
                        nodeName = node.name,
                        connectedLoadKw = connectedKw,
                        demandLoadKw = demandKw,
                        apparentPowerKva = kva,
                        currentA = current,
                        voltage = node.voltage,
                        requiredBreakerA = breaker,
                        requiredTransformerKva = 0.0,
                        diversityFactor = 1.0,
                        childrenCount = 0,
                        notes = listOf(
                            "Load node",
                            "Demand factor = %.3f"
                                .format(
                                    node.demandFactor
                                )
                        )
                    )

                visiting.remove(node.id)

                return connectedKw to demandKw
            }

            var connectedKw = 0.0
            var demandKw = 0.0

            nodeChildren.forEach { child ->

                val childResult =
                    calculateNode(
                        child,
                        visiting
                    )

                connectedKw += childResult.first
                demandKw += childResult.second
            }

            val localLoad =
                node.loadKw.coerceAtLeast(0.0)

            val localDemand =
                localLoad *
                    node.demandFactor.coerceIn(
                        0.0,
                        1.0
                    )

            connectedKw += localLoad
            demandKw += localDemand

            val diversityFactor =
                if (
                    demandKw > EPSILON &&
                    connectedKw > demandKw
                ) {
                    (
                        connectedKw /
                            demandKw
                        ).coerceAtLeast(1.0)
                } else {
                    1.0
                }

            val effectivePf =
                nodeChildren
                    .mapNotNull {
                        resultMap[it.id]
                    }
                    .map {
                        it.apparentPowerKva
                    }
                    .sum()
                    .let { totalKva ->

                        if (
                            demandKw >
                            EPSILON &&
                            totalKva >
                            EPSILON
                        ) {
                            (
                                demandKw /
                                    totalKva
                            ).coerceIn(
                                0.01,
                                1.0
                            )
                        } else {
                            node.powerFactor
                                .coerceIn(
                                    0.01,
                                    1.0
                                )
                        }
                    }

            val apparentPowerKva =
                if (
                    effectivePf >
                    EPSILON
                ) {
                    demandKw /
                        effectivePf
                } else {
                    demandKw
                }

            val current =
                threePhaseCurrent(
                    kva = apparentPowerKva,
                    voltage = node.voltage
                )

            val breaker =
                nextStandardBreaker(
                    current
                )

            val requiredTransformer =
                if (
                    node.type ==
                    SldNodeType.TRANSFORMER
                ) {
                    nextStandardTransformer(
                        apparentPowerKva
                    )
                } else {
                    0.0
                }

            val notes =
                mutableListOf<String>()

            notes +=
                "Connected load = %.2f kW"
                    .format(connectedKw)

            notes +=
                "Demand load = %.2f kW"
                    .format(demandKw)

            notes +=
                "Required apparent power = %.2f kVA"
                    .format(apparentPowerKva)

            notes +=
                "Current = %.2f A"
                    .format(current)

            notes +=
                "Breaker = %.0f A"
                    .format(breaker)

            if (
                diversityFactor >
                1.0 + EPSILON
            ) {
                notes +=
                    "Diversity factor = %.3f"
                        .format(
                            diversityFactor
                        )
            }

            if (
                requiredTransformer >
                0.0
            ) {
                notes +=
                    "Required transformer = %.0f kVA"
                        .format(
                            requiredTransformer
                        )
            }

            resultMap[node.id] =
                UpstreamResult(
                    nodeId = node.id,
                    nodeName = node.name,
                    connectedLoadKw = connectedKw,
                    demandLoadKw = demandKw,
                    apparentPowerKva = apparentPowerKva,
                    currentA = current,
                    voltage = node.voltage,
                    requiredBreakerA = breaker,
                    requiredTransformerKva =
                        requiredTransformer,
                    diversityFactor =
                        diversityFactor,
                    childrenCount =
                        nodeChildren.size,
                    notes = notes
                )

            visiting.remove(node.id)

            return connectedKw to demandKw
        }

        val sourceNodes =
            network.nodes.filter {
                it.type == SldNodeType.SOURCE ||
                    it.type == SldNodeType.TRANSFORMER ||
                    it.type == SldNodeType.GENERATOR
            }

        val roots =
            if (sourceNodes.isNotEmpty()) {
                sourceNodes
            } else {
                network.nodes.filter { node ->
                    network.connections.none {
                        it.toNodeId ==
                            node.id
                    }
                }
            }

        roots.forEach { root ->
            calculateNode(
                root,
                mutableSetOf()
            )
        }

        network.nodes
            .filterNot {
                resultMap.containsKey(it.id)
            }
            .forEach { node ->
                calculateNode(
                    node,
                    mutableSetOf()
                )
            }

        val mainResult =
            roots
                .firstOrNull()
                ?.let {
                    resultMap[it.id]
                }
                ?: resultMap.values.first()

        val totalConnected =
            mainResult.connectedLoadKw

        val totalDemand =
            mainResult.demandLoadKw

        val totalKva =
            mainResult.apparentPowerKva

        val mainCurrent =
            mainResult.currentA

        val mainBreaker =
            mainResult.requiredBreakerA

        val transformerKva =
            nextStandardTransformer(
                totalKva
            )

        val notes =
            mutableListOf<String>()

        notes +=
            "Upstream calculation completed."

        notes +=
            "Connected load = %.2f kW"
                .format(
                    totalConnected
                )

        notes +=
            "Maximum demand = %.2f kW"
                .format(
                    totalDemand
                )

        notes +=
            "Required apparent power = %.2f kVA"
                .format(
                    totalKva
                )

        notes +=
            "Main current = %.2f A"
                .format(
                    mainCurrent
                )

        notes +=
            "Main breaker = %.0f A"
                .format(
                    mainBreaker
                )

        notes +=
            "Recommended transformer = %.0f kVA"
                .format(
                    transformerKva
                )

        return SldCalculationResult(
            nodeResults = resultMap,
            totalConnectedLoadKw =
                totalConnected,
            totalDemandLoadKw =
                totalDemand,
            totalRequiredKva =
                totalKva,
            mainCurrentA =
                mainCurrent,
            mainBreakerA =
                mainBreaker,
            requiredTransformerKva =
                transformerKva,
            notes = notes
        )
    }

    private fun threePhaseCurrent(
        kva: Double,
        voltage: Double
    ): Double {

        if (
            kva <= EPSILON ||
            voltage <= EPSILON
        ) {
            return 0.0
        }

        return (
            kva * 1000.0
        ) /
            (
                sqrt(3.0) *
                    voltage
            )
    }

    private fun nextStandardBreaker(
        current: Double
    ): Double {

        if (current <= 0.0) {
            return 0.0
        }

        return standardBreakers.firstOrNull {
            it >= current
        } ?: standardBreakers.last()
    }

    private fun nextStandardTransformer(
        kva: Double
    ): Double {

        if (kva <= 0.0) {
            return 0.0
        }

        return standardTransformersKva
            .firstOrNull {
                it >= kva
            }
            ?: standardTransformersKva.last()
    }

    private fun validateNetwork(
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

        network.connections.forEach {
            connection ->

            require(
                connection.fromNodeId in ids
            ) {
                "Connection source not found: ${connection.fromNodeId}"
            }

            require(
                connection.toNodeId in ids
            ) {
                "Connection destination not found: ${connection.toNodeId}"
            }

            require(
                connection.fromNodeId !=
                    connection.toNodeId
            ) {
                "A node cannot connect to itself."
            }
        }

        network.nodes.forEach { node ->

            require(
                node.voltage > EPSILON
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

            if (
                node.type ==
                SldNodeType.LOAD
            ) {
                require(
                    node.loadKw >= 0.0
                ) {
                    "Invalid load at ${node.name}."
                }
            }
        }
    }
}
