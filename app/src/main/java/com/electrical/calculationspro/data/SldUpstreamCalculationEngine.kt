package com.electrical.calculationspro.data

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Professional SLD upstream calculation engine.
 *
 * Responsibilities:
 * - Traverse the electrical topology.
 * - Aggregate downstream connected load.
 * - Aggregate downstream demand load.
 * - Calculate kVA.
 * - Calculate three-phase current.
 * - Select preliminary standard breaker rating.
 * - Select preliminary transformer rating.
 * - Calculate feeder voltage drop.
 *
 * The UI must not contain engineering calculations.
 */
object SldUpstreamCalculationEngine {

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

    fun calculate(
        network: SldNetwork
    ): SldCalculationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        validateNetwork(network)

        val nodeMap =
            network.nodes.associateBy { it.id }

        val children =
            mutableMapOf<String, MutableList<SldNode>>()

        network.nodes.forEach { node ->
            children[node.id] = mutableListOf()
        }

        network.connections.forEach { connection ->

            val child =
                nodeMap[connection.toNodeId]
                    ?: return@forEach

            children[
                connection.fromNodeId
            ]?.add(child)
        }

        val connectionMap =
            network.connections.associateBy {
                "${it.fromNodeId}->${it.toNodeId}"
            }

        val results =
            mutableMapOf<String, UpstreamResult>()

        fun calculateNode(
            node: SldNode,
            visiting: MutableSet<String>
        ): Pair<Double, Double> {

            if (!visiting.add(node.id)) {
                throw IllegalArgumentException(
                    "Circular SLD connection detected at ${node.name}."
                )
            }

            val nodeChildren =
                children[node.id].orEmpty()

            /*
             * A LOAD is a terminal engineering load.
             */
            if (node.type == SldNodeType.LOAD) {

                val connectedKw =
                    node.loadKw.coerceAtLeast(0.0)

                val demandFactor =
                    node.demandFactor.coerceIn(
                        0.0,
                        1.0
                    )

                val demandKw =
                    connectedKw * demandFactor

                val pf =
                    node.powerFactor.coerceIn(
                        0.01,
                        1.0
                    )

                val kva =
                    if (demandKw > EPSILON) {
                        demandKw / pf
                    } else {
                        0.0
                    }

                val current =
                    threePhaseCurrent(
                        kva = kva,
                        voltage = node.voltage
                    )

                val breaker =
                    nextBreaker(current)

                results[node.id] =
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
                        voltageDropPercent = 0.0,
                        feederRequiredCurrentA = current,
                        notes = listOf(
                            "Terminal load.",
                            "Connected load = ${format(connectedKw)} kW",
                            "Demand load = ${format(demandKw)} kW",
                            "Power factor = ${format(pf)}",
                            "Design current = ${format(current)} A",
                            "Preliminary breaker = ${format(breaker)} A"
                        )
                    )

                visiting.remove(node.id)

                return connectedKw to demandKw
            }

            var connectedKw = 0.0
            var demandKw = 0.0

            /*
             * First calculate all downstream branches.
             */
            nodeChildren.forEach { child ->

                val childResult =
                    calculateNode(
                        child,
                        visiting
                    )

                connectedKw += childResult.first
                demandKw += childResult.second
            }

            /*
             * A non-LOAD node may itself carry a local load.
             */
            if (node.loadKw > EPSILON) {

                connectedKw +=
                    node.loadKw.coerceAtLeast(0.0)

                demandKw +=
                    node.loadKw.coerceAtLeast(0.0) *
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
                if (demandKw > EPSILON) {
                    demandKw / pf
                } else {
                    0.0
                }

            val current =
                threePhaseCurrent(
                    kva = apparentKva,
                    voltage = node.voltage
                )

            val breaker =
                nextBreaker(current)

            val transformer =
                if (node.type == SldNodeType.TRANSFORMER) {

                    if (node.ratedKva > EPSILON) {
                        node.ratedKva
                    } else {
                        nextTransformer(apparentKva)
                    }

                } else {
                    0.0
                }

            val diversityFactor =
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

            val voltageDrop =
                calculateMaximumDownstreamVoltageDrop(
                    node = node,
                    children = nodeChildren,
                    nodeMap = nodeMap,
                    connectionMap = connectionMap,
                    resultMap = results
                )

            results[node.id] =
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
                    diversityFactor = diversityFactor,
                    childrenCount = nodeChildren.size,
                    voltageDropPercent = voltageDrop,
                    feederRequiredCurrentA = current,
                    notes = listOf(
                        "Connected load = ${format(connectedKw)} kW",
                        "Demand load = ${format(demandKw)} kW",
                        "Required power = ${format(apparentKva)} kVA",
                        "Design current = ${format(current)} A",
                        "Preliminary breaker = ${format(breaker)} A",
                        "Voltage drop = ${format(voltageDrop)} %"
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
                node = root,
                visiting = mutableSetOf()
            )
        }

        /*
         * Handle isolated/disconnected nodes so every object
         * still receives an engineering result.
         */
        network.nodes
            .filterNot {
                results.containsKey(it.id)
            }
            .forEach { node ->

                calculateNode(
                    node = node,
                    visiting = mutableSetOf()
                )
            }

        val mainResult =
            calculationRoots
                .firstOrNull()
                ?.let { results[it.id] }
                ?: results.values.first()

        val requiredTransformer =
            if (mainResult.requiredTransformerKva > EPSILON) {
                mainResult.requiredTransformerKva
            } else {
                nextTransformer(
                    mainResult.apparentPowerKva
                )
            }

        val maximumVoltageDrop =
            results.values.maxOfOrNull {
                it.voltageDropPercent
            } ?: 0.0

        return SldCalculationResult(
            nodeResults = results.toMap(),

            totalConnectedLoadKw =
                mainResult.connectedLoadKw,

            totalDemandLoadKw =
                mainResult.demandLoadKw,

            totalRequiredKva =
                mainResult.apparentPowerKva,

            mainCurrentA =
                mainResult.currentA,

            mainBreakerA =
                mainResult.requiredBreakerA,

            requiredTransformerKva =
                requiredTransformer,

            totalVoltageDropPercent =
                maximumVoltageDrop,

            notes = listOf(
                "SLD upstream engineering completed.",
                "Connected load = ${format(mainResult.connectedLoadKw)} kW",
                "Demand load = ${format(mainResult.demandLoadKw)} kW",
                "Required power = ${format(mainResult.apparentPowerKva)} kVA",
                "Main current = ${format(mainResult.currentA)} A",
                "Main preliminary breaker = ${format(mainResult.requiredBreakerA)} A",
                "Required transformer = ${format(requiredTransformer)} kVA",
                "Maximum voltage drop = ${format(maximumVoltageDrop)} %"
            )
        )
    }

    private fun calculateMaximumDownstreamVoltageDrop(
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

                val localDrop =
                    calculateConnectionVoltageDrop(
                        connection = connection,
                        currentA = childResult.currentA,
                        voltage = node.voltage
                    )

                val childDrop =
                    childResult.voltageDropPercent

                maximumDrop =
                    max(
                        maximumDrop,
                        localDrop + childDrop
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
            connection.lengthMeters / 1000.0

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
            voltageDrop / voltage
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
            sqrt(3.0) * voltage
            )
    }

    private fun nextBreaker(
        current: Double
    ): Double {

        if (current <= EPSILON) {
            return 0.0
        }

        return standardBreakers.firstOrNull {
            it >= current
        } ?: standardBreakers.last()
    }

    private fun nextTransformer(
        kva: Double
    ): Double {

        if (kva <= EPSILON) {
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

        val nodeIds =
            ids.toSet()

        network.connections.forEach { connection ->

            require(
                connection.fromNodeId in nodeIds
            ) {
                "Unknown source node: ${connection.fromNodeId}"
            }

            require(
                connection.toNodeId in nodeIds
            ) {
                "Unknown destination node: ${connection.toNodeId}"
            }

            require(
                connection.fromNodeId !=
                    connection.toNodeId
            ) {
                "A node cannot connect to itself."
            }

            require(
                connection.parallelRuns >= 1
            ) {
                "Parallel cable runs must be at least 1."
            }
        }
    }

    private fun format(
        value: Double
    ): String {

        return if (value.isFinite()) {
            "%.2f".format(value)
        } else {
            "0.00"
        }
    }
}
