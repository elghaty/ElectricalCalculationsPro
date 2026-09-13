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
    val nodeResults: Map<String, UpstreamResult>,

    val totalConnectedLoadKw: Double,

    val totalDemandLoadKw: Double,

    val totalRequiredKva: Double,

    val mainCurrentA: Double,

    val mainBreakerA: Double,

    val requiredTransformerKva: Double,

    val totalVoltageDropPercent: Double = 0.0,

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
        4000.0,
        5000.0,
        6300.0
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
        5000.0,
        6300.0,
        8000.0,
        10000.0,
        12500.0,
        16000.0,
        20000.0
    )

    fun calculateUpstream(
        network: SldNetwork
    ): SldCalculationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        validateNetwork(network)

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val children =
            mutableMapOf<String, MutableList<SldNode>>()

        network.nodes.forEach { node ->
            children[node.id] =
                mutableListOf()
        }

        network.connections.forEach { connection ->

            val child =
                nodeMap[connection.toNodeId]
                    ?: return@forEach

            children[
                connection.fromNodeId
            ]?.add(child)
        }

        val resultMap =
            mutableMapOf<String, UpstreamResult>()

        val pathConnections =
            network.connections.associateBy {
                "${it.fromNodeId}->${it.toNodeId}"
            }

        fun calculateNode(
            node: SldNode,
            visiting: MutableSet<String>
        ): Pair<Double, Double> {

            if (!visiting.add(node.id)) {
                throw IllegalArgumentException(
                    "Circular SLD connection at ${node.name}."
                )
            }

            val nodeChildren =
                children[node.id].orEmpty()

            if (
                node.type ==
                SldNodeType.LOAD
            ) {

                val connectedKw =
                    node.loadKw
                        .coerceAtLeast(0.0)

                val demandKw =
                    connectedKw *
                        node.demandFactor
                            .coerceIn(
                                0.0,
                                1.0
                            )

                val pf =
                    node.powerFactor
                        .coerceIn(
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
                        feederRequiredCurrentA =
                            current,
                        notes = listOf(
                            "Load",
                            "Connected = %.2f kW"
                                .format(
                                    connectedKw
                                ),
                            "Demand = %.2f kW"
                                .format(
                                    demandKw
                                ),
                            "PF = %.3f"
                                .format(
                                    pf
                                ),
                            "Current = %.2f A"
                                .format(
                                    current
                                ),
                            "Breaker = %.0f A"
                                .format(
                                    nextBreaker(
                                        current
                                    )
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

                connectedKw +=
                    result.first

                demandKw +=
                    result.second
            }

            if (node.loadKw > 0.0) {

                connectedKw +=
                    node.loadKw

                demandKw +=
                    node.loadKw *
                        node.demandFactor
                            .coerceIn(
                                0.0,
                                1.0
                            )
            }

            val pf =
                node.powerFactor
                    .coerceIn(
                        0.01,
                        1.0
                    )

            val apparentKva =
                if (demandKw > EPSILON) {
                    demandKw / pf
                } else {
                    0.0
                }

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
                    if (
                        node.ratedKva > 0.0
                    ) {
                        node.ratedKva
                    } else {
                        nextTransformer(
                            apparentKva
                        )
                    }
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

            val feederCurrent =
                current

            val downstreamDrop =
                calculateDownstreamVoltageDrop(
                    node = node,
                    children = nodeChildren,
                    nodeMap = nodeMap,
                    connectionMap = pathConnections,
                    resultMap = resultMap
                )

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
                    childrenCount =
                        nodeChildren.size,
                    voltageDropPercent =
                        downstreamDrop,
                    feederRequiredCurrentA =
                        feederCurrent,
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
                            ),
                        "Voltage Drop = %.2f %%"
                            .format(
                                downstreamDrop
                            )
                    )
                )

            visiting.remove(node.id)

            return connectedKw to demandKw
        }

        val roots =
            network.nodes.filter {
                it.type ==
                    SldNodeType.SOURCE
            }

        val calculationRoots =
            if (roots.isNotEmpty()) {
                roots
            } else {
                network.nodes.filter { node ->

                    network.connections.none {
                        it.toNodeId ==
                            node.id
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
                resultMap.containsKey(
                    it.id
                )
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
            when {

                main.requiredTransformerKva >
                    0.0 ->
                    main.requiredTransformerKva

                else ->
                    nextTransformer(
                        main.apparentPowerKva
                    )
            }

        val maximumVoltageDrop =
            resultMap.values
                .maxOfOrNull {
                    it.voltageDropPercent
                }
                ?: 0.0

        return SldCalculationResult(

            nodeResults =
                resultMap.toMap(),

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

            totalVoltageDropPercent =
                maximumVoltageDrop,

            notes = buildList {

                add(
                    "SLD upstream calculation completed."
                )

                add(
                    "Connected load = %.2f kW"
                        .format(
                            main.connectedLoadKw
                        )
                )

                add(
                    "Demand load = %.2f kW"
                        .format(
                            main.demandLoadKw
                        )
                )

                add(
                    "Required apparent power = %.2f kVA"
                        .format(
                            main.apparentPowerKva
                        )
                )

                add(
                    "Main current = %.2f A"
                        .format(
                            main.currentA
                        )
                )

                add(
                    "Main breaker = %.0f A"
                        .format(
                            main.requiredBreakerA
                        )
                )

                add(
                    "Recommended transformer = %.0f kVA"
                        .format(
                            transformerKva
                        )
                )

                add(
                    "Maximum voltage drop = %.2f %%"
                        .format(
                            maximumVoltageDrop
                        )
                )
            }
        )
    }

    private fun calculateDownstreamVoltageDrop(
        node: SldNode,
        children: List<SldNode>,
        nodeMap: Map<String, SldNode>,
        connectionMap: Map<String, SldConnection>,
        resultMap: Map<String, UpstreamResult>
    ): Double {

        if (children.isEmpty()) {
            return 0.0
        }

        var maximumDrop = 0.0

        children.forEach { child ->

            val connection =
                connectionMap[
                    "${node.id}->${child.id}"
                ]

            val childResult =
                resultMap[child.id]

            if (
                connection != null &&
                childResult != null
            ) {

                val drop =
                    calculateConnectionVoltageDrop(
                        connection = connection,
                        currentA =
                            childResult.currentA,
                        voltage =
                            node.voltage
                    )

                val childDrop =
                    resultMap[child.id]
                        ?.voltageDropPercent
                        ?: 0.0

                maximumDrop =
                    max(
                        maximumDrop,
                        drop + childDrop
                    )
            }
        }

        return maximumDrop
    }

    private fun calculateConnectionVoltageDrop(
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

        val lengthKm =
            connection.lengthMeters /
                1000.0

        val impedance =
            sqrt(
                resistance * resistance +
                    reactance * reactance
            )

        val voltageDrop =
            sqrt(3.0) *
                currentA *
                impedance *
                lengthKm

        return (
            voltageDrop /
                voltage
            ) * 100.0
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
            sqrt(3.0) *
                voltage
        )
    }

    private fun nextBreaker(
        current: Double
    ): Double {

        if (current <= 0.0) {
            return 0.0
        }

        return standardBreakers
            .firstOrNull {
                it >= current
            }
            ?: standardBreakers.last()
    }

    private fun nextTransformer(
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

            require(
                connection.lengthMeters >= 0.0
            ) {
                "Invalid feeder length."
            }

            require(
                connection.resistanceOhmPerKm >= 0.0
            ) {
                "Invalid feeder resistance."
            }

            require(
                connection.reactanceOhmPerKm >= 0.0
            ) {
                "Invalid feeder reactance."
            }

            require(
                connection.parallelRuns >= 1
            ) {
                "Parallel cable runs must be at least 1."
            }

            require(
                connection.currentCapacityA >= 0.0
            ) {
                "Invalid feeder current capacity."
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

            require(
                node.ratedKva >= 0.0
            ) {
                "Invalid rated kVA at ${node.name}."
            }

            require(
                node.transformerPercentZ >= 0.0
            ) {
                "Invalid transformer impedance at ${node.name}."
            }

            require(
                node.generatorXdSubtransient >= 0.0
            ) {
                "Invalid generator subtransient reactance at ${node.name}."
            }

            require(
                node.sourceShortCircuitMva >= 0.0
            ) {
                "Invalid source short-circuit MVA at ${node.name}."
            }
        }

        validateNoCycles(network)
    }

    private fun validateNoCycles(
        network: SldNetwork
    ) {

        val children =
            mutableMapOf<String, MutableList<String>>()

        network.nodes.forEach {
            children[it.id] =
                mutableListOf()
        }

        network.connections.forEach {
            children[
                it.fromNodeId
            ]?.add(
                it.toNodeId
            )
        }

        val visiting =
            mutableSetOf<String>()

        val visited =
            mutableSetOf<String>()

        fun visit(id: String) {

            if (id in visiting) {
                throw IllegalArgumentException(
                    "Circular SLD network detected."
                )
            }

            if (id in visited) {
                return
            }

            visiting.add(id)

            children[id]
                .orEmpty()
                .forEach(::visit)

            visiting.remove(id)

            visited.add(id)
        }

        network.nodes.forEach {
            visit(it.id)
        }
    }
}
