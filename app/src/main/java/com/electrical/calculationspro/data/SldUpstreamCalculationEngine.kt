package com.electrical.calculationspro.data

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Professional SLD upstream calculation engine.
 *
 * The calculation direction is taken from SldTopologyEngine.
 *
 * Engineering truth:
 *
 *     SOURCE
 *        ↓
 *     BREAKER
 *        ↓
 *      BUS
 *        ↓
 *   TRANSFORMER
 *        ↓
 *      PANEL
 *        ↓
 *     BREAKER
 *        ↓
 *      LOAD
 *
 * The physical component type does NOT determine direction.
 * The electrical topology does.
 *
 * UI is not responsible for engineering calculations.
 */
object SldUpstreamCalculationEngine {

    private const val EPSILON = 1.0e-9

    private val standardBreakers =
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

    private val standardTransformersKva =
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
    ): SldCalculationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        /*
         * ============================================================
         * TOPOLOGY IS THE SINGLE SOURCE OF ELECTRICAL DIRECTION
         * ============================================================
         *
         * Never calculate upstream directly from the raw UI connection
         * direction.
         *
         * SldTopologyEngine discovers the electrical source and orients
         * every connection from upstream to downstream.
         */
        val topology =
            SldTopologyEngine.build(
                network
            )

        val orientedNetwork =
            SldNetwork(
                nodes = network.nodes,
                connections = topology.connections
            )

        val nodeMap =
            orientedNetwork.nodes.associateBy {
                it.id
            }

        /*
         * ============================================================
         * CHILDREN MAP
         * ============================================================
         */
        val children =
            mutableMapOf<
                String,
                MutableList<SldNode>
            >()

        orientedNetwork.nodes.forEach { node ->
            children[node.id] =
                mutableListOf()
        }

        topology.connections.forEach { connection ->

            val child =
                nodeMap[
                    connection.toNodeId
                ]

            if (child != null) {

                children[
                    connection.fromNodeId
                ]?.add(child)
            }
        }

        /*
         * ============================================================
         * CONNECTION LOOKUP
         * ============================================================
         */
        val connectionMap =
            topology.connections.associateBy {
                "${it.fromNodeId}->${it.toNodeId}"
            }

        /*
         * ============================================================
         * RESULT STORAGE
         * ============================================================
         */
        val results =
            mutableMapOf<
                String,
                UpstreamResult
            >()

        /*
         * ============================================================
         * RECURSIVE DOWNSTREAM CALCULATION
         * ============================================================
         */
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
                children[
                    node.id
                ].orEmpty()

            /*
             * --------------------------------------------------------
             * TERMINAL LOAD
             * --------------------------------------------------------
             */
            if (
                node.type ==
                SldNodeType.LOAD
            ) {

                val connectedKw =
                    node.loadKw
                        .coerceAtLeast(0.0)

                val demandFactor =
                    node.demandFactor
                        .coerceIn(
                            0.0,
                            1.0
                        )

                val demandKw =
                    connectedKw *
                        demandFactor

                val pf =
                    node.powerFactor
                        .coerceIn(
                            0.01,
                            1.0
                        )

                val kva =
                    if (
                        demandKw >
                        EPSILON
                    ) {
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
                    nextBreaker(
                        current
                    )

                results[node.id] =
                    UpstreamResult(
                        nodeId =
                            node.id,

                        nodeName =
                            node.name,

                        connectedLoadKw =
                            connectedKw,

                        demandLoadKw =
                            demandKw,

                        apparentPowerKva =
                            kva,

                        currentA =
                            current,

                        voltage =
                            node.voltage,

                        requiredBreakerA =
                            breaker,

                        requiredTransformerKva =
                            0.0,

                        diversityFactor =
                            1.0,

                        childrenCount =
                            0,

                        voltageDropPercent =
                            0.0,

                        feederRequiredCurrentA =
                            current,

                        notes =
                            listOf(
                                "Terminal load.",
                                "Connected load = ${format(connectedKw)} kW",
                                "Demand load = ${format(demandKw)} kW",
                                "Power factor = ${format(pf)}",
                                "Design current = ${format(current)} A",
                                "Preliminary breaker = ${format(breaker)} A"
                            )
                    )

                visiting.remove(
                    node.id
                )

                return connectedKw to demandKw
            }

            /*
             * --------------------------------------------------------
             * DOWNSTREAM AGGREGATION
             * --------------------------------------------------------
             */
            var connectedKw =
                0.0

            var demandKw =
                0.0

            nodeChildren.forEach { child ->

                val childResult =
                    calculateNode(
                        node = child,
                        visiting = visiting
                    )

                connectedKw +=
                    childResult.first

                demandKw +=
                    childResult.second
            }

            /*
             * --------------------------------------------------------
             * LOCAL LOAD
             *
             * A BUS/BREAKER/PANEL should normally have zero local
             * load when downstream LOAD nodes already represent the
             * actual loads.
             *
             * The value is still supported for legitimate equipment
             * carrying an explicit local load.
             * --------------------------------------------------------
             */
            if (
                node.loadKw >
                EPSILON
            ) {

                connectedKw +=
                    node.loadKw
                        .coerceAtLeast(0.0)

                demandKw +=
                    node.loadKw
                        .coerceAtLeast(0.0) *
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
                if (
                    demandKw >
                    EPSILON
                ) {
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
                nextBreaker(
                    current
                )

            val transformer =
                if (
                    node.type ==
                    SldNodeType.TRANSFORMER
                ) {

                    if (
                        node.ratedKva >
                        EPSILON
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

            val diversityFactor =
                if (
                    demandKw >
                    EPSILON &&
                    connectedKw >
                    demandKw
                ) {

                    max(
                        1.0,
                        connectedKw /
                            demandKw
                    )

                } else {
                    1.0
                }

            /*
             * --------------------------------------------------------
             * VOLTAGE DROP
             * --------------------------------------------------------
             *
             * The child result already contains the downstream
             * voltage drop. The current connection is added here.
             */
            val voltageDrop =
                calculateMaximumDownstreamVoltageDrop(
                    node =
                        node,

                    children =
                        nodeChildren,

                    nodeMap =
                        nodeMap,

                    connectionMap =
                        connectionMap,

                    resultMap =
                        results
                )

            results[node.id] =
                UpstreamResult(
                    nodeId =
                        node.id,

                    nodeName =
                        node.name,

                    connectedLoadKw =
                        connectedKw,

                    demandLoadKw =
                        demandKw,

                    apparentPowerKva =
                        apparentKva,

                    currentA =
                        current,

                    voltage =
                        node.voltage,

                    requiredBreakerA =
                        breaker,

                    requiredTransformerKva =
                        transformer,

                    diversityFactor =
                        diversityFactor,

                    childrenCount =
                        nodeChildren.size,

                    voltageDropPercent =
                        voltageDrop,

                    feederRequiredCurrentA =
                        current,

                    notes =
                        listOf(
                            "Connected load = ${format(connectedKw)} kW",
                            "Demand load = ${format(demandKw)} kW",
                            "Required power = ${format(apparentKva)} kVA",
                            "Design current = ${format(current)} A",
                            "Preliminary breaker = ${format(breaker)} A",
                            "Voltage drop = ${format(voltageDrop)} %"
                        )
                )

            visiting.remove(
                node.id
            )

            return connectedKw to demandKw
        }

        /*
         * ============================================================
         * ELECTRICAL ROOT
         * ============================================================
         *
         * SldTopologyEngine has already identified the actual source.
         * Start calculation from that source.
         */
        val calculationRoot =
            topology.source

        calculateNode(
            node =
                calculationRoot,

            visiting =
                mutableSetOf()
        )

        /*
         * ============================================================
         * SAFETY NET
         * ============================================================
         *
         * Normally every node is reached from the source because
         * SldTopologyEngine rejects disconnected nodes.
         *
         * Keep this protection so the calculation remains robust if
         * topology behavior changes later.
         */
        orientedNetwork.nodes
            .filterNot {
                results.containsKey(
                    it.id
                )
            }
            .forEach { node ->

                calculateNode(
                    node =
                        node,

                    visiting =
                        mutableSetOf()
                )
            }

        val mainResult =
            results[
                calculationRoot.id
            ]
                ?: results.values.firstOrNull()
                ?: error(
                    "No SLD engineering result was produced."
                )

        val requiredTransformer =
            if (
                mainResult.requiredTransformerKva >
                EPSILON
            ) {

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
            nodeResults =
                results.toMap(),

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

            notes =
                listOf(
                    "SLD topology-driven upstream engineering completed.",
                    "Electrical source = ${calculationRoot.name}",
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

    /*
     * ================================================================
     * VOLTAGE DROP
     * ================================================================
     */
    private fun calculateMaximumDownstreamVoltageDrop(
        node: SldNode,
        children: List<SldNode>,
        nodeMap: Map<String, SldNode>,
        connectionMap: Map<String, SldConnection>,
        resultMap: Map<String, UpstreamResult>
    ): Double {

        if (
            children.isEmpty()
        ) {
            return 0.0
        }

        var maximumDrop =
            0.0

        children.forEach { child ->

            val connection =
                connectionMap[
                    "${node.id}->${child.id}"
                ]

            val childResult =
                resultMap[
                    child.id
                ]

            if (
                connection != null &&
                childResult != null
            ) {

                val localDrop =
                    calculateConnectionVoltageDrop(
                        connection =
                            connection,

                        currentA =
                            childResult.currentA,

                        voltage =
                            node.voltage
                    )

                val childDrop =
                    childResult.voltageDropPercent

                maximumDrop =
                    max(
                        maximumDrop,
                        localDrop +
                            childDrop
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

        /*
         * BUSBAR has no cable length and therefore no cable voltage
         * drop.
         */
        if (
            connection.connectionType ==
            SldConnectionType.BUSBAR
        ) {
            return 0.0
        }

        if (
            connection.lengthMeters <=
            0.0 ||
            currentA <=
            0.0 ||
            voltage <=
            0.0
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
            ) *
            100.0
    }

    /*
     * ================================================================
     * THREE-PHASE CURRENT
     * ================================================================
     */
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
            kva *
                1000.0
            ) /
            (
                sqrt(3.0) *
                    voltage
            )
    }

    /*
     * ================================================================
     * BREAKER SELECTION
     * ================================================================
     */
    private fun nextBreaker(
        current: Double
    ): Double {

        if (
            current <= EPSILON
        ) {
            return 0.0
        }

        return standardBreakers
            .firstOrNull {
                it >= current
            }
            ?: standardBreakers.last()
    }

    /*
     * ================================================================
     * TRANSFORMER SELECTION
     * ================================================================
     */
    private fun nextTransformer(
        kva: Double
    ): Double {

        if (
            kva <= EPSILON
        ) {
            return 0.0
        }

        return standardTransformersKva
            .firstOrNull {
                it >= kva
            }
            ?: standardTransformersKva.last()
    }

    /*
     * ================================================================
     * DEFENSIVE NETWORK VALIDATION
     * ================================================================
     */
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

        val nodeIds =
            ids.toSet()

        network.connections.forEach { connection ->

            require(
                connection.fromNodeId in
                    nodeIds
            ) {
                "Unknown source node: ${connection.fromNodeId}"
            }

            require(
                connection.toNodeId in
                    nodeIds
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

    /*
     * ================================================================
     * FORMAT
     * ================================================================
     */
    private fun format(
        value: Double
    ): String {

        return if (
            value.isFinite()
        ) {

            "%.2f".format(
                value
            )

        } else {
            "0.00"
        }
    }
}
