package com.electrical.calculationspro.data

import kotlin.math.max
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

        val nodeMap = network.nodes.associateBy { it.id }

        val children = mutableMapOf<String, MutableList<SldNode>>()

        network.nodes.forEach { node ->
            children[node.id] = mutableListOf()
        }

        network.connections.forEach { connection ->
            val child = nodeMap[connection.toNodeId]
                ?: return@forEach

            children[connection.fromNodeId]?.add(child)
        }

        val resultMap = mutableMapOf<String, UpstreamResult>()

        fun calculateNode(
            node: SldNode,
            visiting: MutableSet<String>
        ): Pair<Double, Double> {

            if (!visiting.add(node.id)) {
                throw IllegalArgumentException(
                    "Circular SLD connection at ${node.name}."
                )
            }

            val nodeChildren = children[node.id].orEmpty()

            if (node.type == SldNodeType.LOAD) {

                val connectedKw =
                    node.loadKw.coerceAtLeast(0.0)

                val demandKw =
                    connectedKw *
                        node.demandFactor.coerceIn(
                            0.0,
                            1.0
                        )

                val pf =
                    node.powerFactor.coerceIn(
                        0.01,
                        1.0
                    )

                val kva =
                    demandKw / pf

                val current =
                    threePhaseCurrent(
                        kva,
                        node.voltage
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
                        requiredBreakerA =
                            nextBreaker(current),
                        requiredTransformerKva = 0.0,
                        diversityFactor = 1.0,
                        childrenCount = 0,
                        notes = listOf(
                            "Load",
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

                val result =
                    calculateNode(
                        child,
                        visiting
                    )

                connectedKw += result.first
                demandKw += result.second
            }

            if (node.loadKw > 0.0) {

                connectedKw += node.loadKw

                demandKw +=
                    node.loadKw *
                        node.demandFactor.coerceIn(
                            0.0,
                            1.0
                        )
            }

            val pf =
                node.powerFactor.coerceIn(
                    0.01,
                    1.0
                )

            val apparentKva =
                demandKw / pf

            val current =
                threePhaseCurrent(
                    apparentKva,
                    node.voltage
                )

            val breaker =
                nextBreaker(current)

            val transformer =
                if (
                    node.type ==
                    SldNodeType.TRANSFORMER
                ) {
                    nextTransformer(
                        apparentKva
                    )
                } else {
                    0.0
                }

            val diversity =
                if (
                    demandKw > EPSILON &&
                    connectedKw > demandKw
                ) {
                    max(
                        1.0,
                        connectedKw / demandKw
                    )
                } else {
                    1.0
                }

            resultMap[node.id] =
                UpstreamResult(
                    nodeId = node.id,
                    nodeName = node.name,
                    connectedLoadKw = connectedKw,
                    demandLoadKw = demandKw,
                    apparentPowerKva = apparentKva,
                    currentA = current,
                    voltage = node.voltage,
                    requiredBreakerA = breaker,
                    requiredTransformerKva = transformer,
                    diversityFactor = diversity,
                    childrenCount = nodeChildren.size,
                    notes = listOf(
                        "Connected = %.2f kW"
                            .format(
                                connectedKw
                            ),
                        "Demand = %.2f kW"
                            .format(
                                demandKw
                            ),
                        "Required = %.2f kVA"
                            .format(
                                apparentKva
                            ),
                        "Current = %.2f A"
                            .format(
                                current
                            ),
                        "Breaker = %.0f A"
                            .format(
                                breaker
                            )
                    )
                )

            visiting.remove(node.id)

            return connectedKw to demandKw
        }

        val roots =
            network.nodes.filter {
                it.type == SldNodeType.SOURCE
            }

        val calculationRoots =
            if (roots.isNotEmpty()) {
                roots
            } else {
                network.nodes.filter { node ->
                    network.connections.none {
                        it.toNodeId == node.id
                    }
                }
            }

        calculationRoots.forEach { root ->
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

        val main =
            calculationRoots
                .firstOrNull()
                ?.let {
                    resultMap[it.id]
                }
                ?: resultMap.values.first()

        val transformerKva =
            nextTransformer(
                main.apparentPowerKva
            )

        return SldCalculationResult(
            nodeResults = resultMap,
            totalConnectedLoadKw =
                main.connectedLoadKw,
            totalDemandLoadKw =
                main.demandLoadKw,
            totalRequiredKva =
                main.apparentPowerKva,
            mainCurrentA =
                main.currentA,
            mainBreakerA =
                main.requiredBreakerA,
            requiredTransformerKva =
                transformerKva,
            notes = listOf(
                "SLD upstream calculation completed.",
                "Connected load = %.2f kW"
                    .format(
                        main.connectedLoadKw
                    ),
                "Demand load = %.2f kW"
                    .format(
                        main.demandLoadKw
                    ),
                "Required apparent power = %.2f kVA"
                    .format(
                        main.apparentPowerKva
                    ),
                "Main current = %.2f A"
                    .format(
                        main.currentA
                    ),
                "Main breaker = %.0f A"
                    .format(
                        main.requiredBreakerA
                    ),
                "Recommended transformer = %.0f kVA"
                    .format(
                        transformerKva
                    )
            )
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
        ) / (
            sqrt(3.0) * voltage
        )
    }

    private fun nextBreaker(
        current: Double
    ): Double {

        if (current <= 0.0) {
            return 0.0
        }

        return standardBreakers.firstOrNull {
            it >= current
        } ?: standardBreakers.last()
    }

    private fun nextTransformer(
        kva: Double
    ): Double {

        if (kva <= 0.0) {
            return 0.0
        }

        return standardTransformersKva.firstOrNull {
            it >= kva
        } ?: standardTransformersKva.last()
    }

    private fun validateNetwork(
        network: SldNetwork
    ) {

        val ids =
            network.nodes.map {
                it.id
            }

        require(
            ids.size == ids.toSet().size
        ) {
            "Duplicate SLD node IDs."
        }

        network.connections.forEach { connection ->

            require(
                connection.fromNodeId in ids
            ) {
                "Connection source not found."
            }

            require(
                connection.toNodeId in ids
            ) {
                "Connection destination not found."
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

            require(
                node.loadKw >= 0.0
            ) {
                "Invalid load at ${node.name}."
            }
        }
    }
}

