package com.electrical.calculationspro.data

import kotlin.math.sqrt

/**
 * PROFESSIONAL SLD UPSTREAM ENGINE
 *
 * Calculates the electrical demand propagated from downstream
 * loads toward the source.
 *
 * Flow:
 *
 * LOAD
 *  ↓
 * FEEDER
 *  ↓
 * PANEL
 *  ↓
 * BUSBAR
 *  ↓
 * MAIN FEEDER
 *  ↓
 * TRANSFORMER / GENERATOR
 *  ↓
 * SOURCE
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

        require(
            network.nodes.isNotEmpty()
        ) {
            "SLD network is empty."
        }

        validate(network)

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val children =
            network.nodes.associate {
                it.id to
                    network.connections
                        .filter { connection ->
                            connection.fromNodeId ==
                                it.id
                        }
                        .mapNotNull { connection ->
                            nodeMap[
                                connection.toNodeId
                            ]
                        }
            }

        val cache =
            mutableMapOf<
                String,
                Pair<Double, Double>
            >()

        fun calculateNode(
            node: SldNode,
            stack: MutableSet<String>
        ): Pair<Double, Double> {

            cache[node.id]?.let {
                return it
            }

            require(
                stack.add(node.id)
            ) {
                "Circular upstream path at ${node.name}."
            }

            if (
                node.type ==
                SldNodeType.LOAD
            ) {

                val connected =
                    node.loadKw
                        .coerceAtLeast(0.0)

                val demand =
                    connected *
                        node.demandFactor
                            .coerceIn(
                                0.0,
                                1.0
                            )

                val result =
                    connected to demand

                cache[node.id] =
                    result

                stack.remove(node.id)

                return result
            }

            var connected =
                node.loadKw
                    .coerceAtLeast(0.0)

            var demand =
                node.loadKw.coerceAtLeast(0.0) *
                    node.demandFactor.coerceIn(
                        0.0,
                        1.0
                    )

            children[node.id]
                .orEmpty()
                .forEach { child ->

                    val childResult =
                        calculateNode(
                            child,
                            stack
                        )

                    connected +=
                        childResult.first

                    demand +=
                        childResult.second
                }

            val result =
                connected to demand

            cache[node.id] =
                result

            stack.remove(node.id)

            return result
        }

        val source =
            network.nodes.firstOrNull {
                it.type ==
                    SldNodeType.SOURCE
            }
                ?: network.nodes.firstOrNull {
                    network.connections.none { connection ->
                        connection.toNodeId ==
                            it.id
                    }
                }
                ?: network.nodes.first()

        calculateNode(
            source,
            mutableSetOf()
        )

        val nodeResults =
            network.nodes.map { node ->

                val values =
                    calculateNode(
                        node,
                        mutableSetOf()
                    )

                val connected =
                    values.first

                val demand =
                    values.second

                val pf =
                    node.powerFactor
                        .coerceIn(
                            0.01,
                            1.0
                        )

                val kva =
                    if (
                        demand > 0.0
                    ) {
                        demand / pf
                    } else {
                        0.0
                    }

                val current =
                    threePhaseCurrent(
                        kva,
                        node.voltage
                    )

                val breaker =
                    nextBreaker(
                        current
                    )

                val voltageDrop =
                    downstreamVoltageDrop(
                        node,
                        current,
                        network,
                        nodeMap
                    )

                val loading =
                    if (
                        node.ratedKva > 0.0
                    ) {
                        kva /
                            node.ratedKva *
                            100.0
                    } else {
                        0.0
                    }

                NodeResult(
                    nodeId =
                        node.id,
                    nodeName =
                        node.name,
                    connectedKw =
                        connected,
                    demandKw =
                        demand,
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
                                    "%.2f kW"
                                        .format(
                                            connected
                                        )
                            )

                            add(
                                "Demand load = " +
                                    "%.2f kW"
                                        .format(
                                            demand
                                        )
                            )

                            add(
                                "Required apparent power = " +
                                    "%.2f kVA"
                                        .format(
                                            kva
                                        )
                            )

                            add(
                                "Design current = " +
                                    "%.2f A"
                                        .format(
                                            current
                                        )
                            )

                            add(
                                "Recommended breaker = " +
                                    "%.0f A"
                                        .format(
                                            breaker
                                        )
                            )

                            if (
                                node.ratedKva > 0.0
                            ) {
                                add(
                                    "Equipment loading = " +
                                        "%.1f %%"
                                            .format(
                                                loading
                                            )
                                )
                            }

                            add(
                                "Downstream voltage drop = " +
                                    "%.2f %%"
                                        .format(
                                            voltageDrop
                                        )
                            )
                        }
                )
            }

        val feederResults =
            network.connections.map { connection ->

                val child =
                    nodeMap[
                        connection.toNodeId
                    ]

                val childResult =
                    nodeResults.firstOrNull {
                        it.nodeId ==
                            connection.toNodeId
                    }

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
                        ?: 400.0

                val voltageDrop =
                    calculateVoltageDrop(
                        connection,
                        current,
                        voltage
                    )

                val adequate =
                    connection.currentCapacityA <= 0.0 ||
                        connection.currentCapacityA >=
                        current

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
                        nextBreaker(
                            current
                        ),
                    voltageDropPercent =
                        voltageDrop,
                    cableAdequate =
                        adequate,
                    notes =
                        buildList {

                            add(
                                "Design current = " +
                                    "%.2f A"
                                        .format(
                                            current
                                        )
                            )

                            if (
                                connection.currentCapacityA > 0.0
                            ) {
                                add(
                                    "Cable capacity = " +
                                        "%.2f A"
                                            .format(
                                                connection.currentCapacityA
                                            )
                                )
                            }

                            add(
                                "Voltage drop = " +
                                    "%.2f %%"
                                        .format(
                                            voltageDrop
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
            nodeResults.first {
                it.nodeId ==
                    source.id
            }

        val recommendedTransformer =
            if (
                source.type ==
                SldNodeType.TRANSFORMER &&
                source.ratedKva > 0.0
            ) {
                source.ratedKva
            } else {
                nextTransformer(
                    sourceResult.kva
                )
            }

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
            }

        return Result(
            sourceNodeId =
                source.id,
            sourceName =
                source.name,
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

    private fun downstreamVoltageDrop(
        node: SldNode,
        currentA: Double,
        network: SldNetwork,
        nodeMap: Map<String, SldNode>
    ): Double {

        val direct =
            network.connections
                .filter {
                    it.fromNodeId ==
                        node.id
                }

        if (
            direct.isEmpty()
        ) {
            return 0.0
        }

        var maximum =
            0.0

        direct.forEach { connection ->

            val child =
                nodeMap[
                    connection.toNodeId
                ] ?: return@forEach

            val childCurrent =
                if (
                    child.loadKw > 0.0
                ) {
                    threePhaseCurrent(
                        child.loadKw /
                            child.powerFactor
                                .coerceIn(
                                    0.01,
                                    1.0
                                ),
                        child.voltage
                    )
                } else {
                    currentA
                }

            val drop =
                calculateVoltageDrop(
                    connection,
                    childCurrent,
                    node.voltage
                )

            maximum =
                maxOf(
                    maximum,
                    drop
                )
        }

        return maximum
    }

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

        val r =
            connection.resistanceOhmPerKm
                .coerceAtLeast(0.0) /
                runs

        val x =
            connection.reactanceOhmPerKm
                .coerceAtLeast(0.0) /
                runs

        val z =
            sqrt(
                r * r +
                    x * x
            )

        val dropVolts =
            sqrt(3.0) *
                currentA *
                z *
                connection.lengthMeters /
                1000.0

        return (
            dropVolts /
                voltage
            ) * 100.0
    }

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

    private fun nextBreaker(
        current: Double
    ): Double {

        return breakerRatings
            .firstOrNull {
                it >= current
            }
            ?: breakerRatings.last()
    }

    private fun nextTransformer(
        kva: Double
    ): Double {

        return transformerRatings
            .firstOrNull {
                it >= kva
            }
            ?: transformerRatings.last()
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
                "SLD connection source does not exist."
            }

            require(
                connection.toNodeId in ids
            ) {
                "SLD connection destination does not exist."
            }

            require(
                connection.fromNodeId !=
                    connection.toNodeId
            ) {
                "SLD element cannot connect to itself."
            }

            require(
                connection.parallelRuns >= 1
            ) {
                "Parallel cable runs must be at least 1."
            }
        }
    }
}
