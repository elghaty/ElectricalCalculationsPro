package com.electrical.calculationspro.data

import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * ================================================================
 * PROFESSIONAL SLD UPSTREAM ENGINE
 * ================================================================
 *
 * Authoritative upstream electrical calculation engine.
 *
 * Engineering flow:
 *
 * SOURCE
 *   ↓
 * TRANSFORMER / GENERATOR
 *   ↓
 * PANEL
 *   ↓
 * BUS / BREAKER
 *   ↓
 * LOAD
 *
 * Engineering rules:
 *
 * 1. LOAD nodes carry actual local load.
 * 2. Intermediate equipment does not duplicate downstream load.
 * 3. Downstream demand propagates automatically upstream.
 * 4. BUSBAR is an internal panel connection and is not a cable.
 * 5. BUSBAR has zero cable voltage drop.
 * 6. Cable adequacy is verified only for CABLE connections.
 * 7. Zero cable capacity means NOT VERIFIED, not adequate.
 * 8. Selected EngineeringContext is preserved.
 * 9. No silent standard substitution when context is supplied.
 * 10. Breaker selection uses the selected standard dataset when
 *     available.
 * 11. Transformer recommendation is based on calculated demand.
 *
 * ================================================================
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

    private data class PowerResult(
        val connectedKw: Double,
        val demandKw: Double,
        val reactiveKvar: Double
    )

    /**
     * Compatibility fallback.
     *
     * Used only when no engineering context is supplied through
     * the legacy API.
     */
    private val defaultBreakerRatings =
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

    /**
     * Legacy API.
     */
    fun calculate(
        network: SldNetwork
    ): Result {
        return calculate(
            network = network,
            engineeringContext = null
        )
    }

    /**
     * Authoritative context-aware calculation.
     */
    fun calculate(
        network: SldNetwork,
        engineeringContext: SldEngineeringContext?
    ): Result {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        /*
         * Basic engineering input validation.
         */
        validate(network)

        /*
         * Topology validation is centralized in one engine.
         *
         * No second topology implementation is created here.
         */
        val topology =
            SldTopologyEngine.build(network)

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val breakerRatings =
            resolveBreakerRatings(
                engineeringContext = engineeringContext
            )

        val voltageDropLimitPercent =
            engineeringContext
                ?.effectiveVoltageDropLimitPercent()
                ?.takeIf {
                    it > 0.0
                }
                ?: 3.0

        /*
         * ==========================================================
         * DOWNSTREAM POWER AGGREGATION
         * ==========================================================
         *
         * Only LOAD nodes contribute local power.
         *
         * Therefore:
         *
         * SOURCE
         * TRANSFORMER
         * GENERATOR
         * PANEL
         * BUS
         * BREAKER
         *
         * do not receive duplicated local load.
         */

        val powerCache =
            mutableMapOf<String, PowerResult>()

        fun calculateNodePower(
            node: SldNode,
            stack: MutableSet<String>
        ): PowerResult {

            powerCache[node.id]?.let {
                return it
            }

            require(
                stack.add(node.id)
            ) {
                "Circular upstream path at ${node.name}."
            }

            val ownConnectedKw =
                when (node.type) {

                    SldNodeType.LOAD ->
                        node.loadKw.coerceAtLeast(0.0)

                    SldNodeType.BUS,
                    SldNodeType.BREAKER,
                    SldNodeType.SOURCE,
                    SldNodeType.TRANSFORMER,
                    SldNodeType.GENERATOR,
                    SldNodeType.PANEL ->
                        0.0

                    else ->
                        0.0
                }

            val ownDemandKw =
                when (node.type) {

                    SldNodeType.LOAD ->
                        ownConnectedKw *
                            node.demandFactor.coerceIn(
                                0.0,
                                1.0
                            )

                    else ->
                        0.0
                }

            val ownReactiveKvar =
                calculateReactivePower(
                    activeKw = ownDemandKw,
                    powerFactor = node.powerFactor
                )

            var connectedKw =
                ownConnectedKw

            var demandKw =
                ownDemandKw

            var reactiveKvar =
                ownReactiveKvar

            topology.children[node.id]
                .orEmpty()
                .forEach { child ->

                    val childResult =
                        calculateNodePower(
                            node = child,
                            stack = stack
                        )

                    connectedKw +=
                        childResult.connectedKw

                    demandKw +=
                        childResult.demandKw

                    reactiveKvar +=
                        childResult.reactiveKvar
                }

            stack.remove(node.id)

            return PowerResult(
                connectedKw = connectedKw,
                demandKw = demandKw,
                reactiveKvar = reactiveKvar
            ).also {
                powerCache[node.id] = it
            }
        }

        /*
         * Calculate the complete system from the actual source.
         */
        calculateNodePower(
            node = topology.source,
            stack = mutableSetOf()
        )

        /*
         * Populate all node results.
         */
        network.nodes.forEach { node ->

            calculateNodePower(
                node = node,
                stack = mutableSetOf()
            )
        }

        /*
         * ==========================================================
         * CUMULATIVE VOLTAGE DROP
         * ==========================================================
         *
         * Voltage drop is accumulated along actual directed feeder
         * sections.
         *
         * BUSBAR:
         *
         *     ΔV = 0
         *
         * CABLE:
         *
         *     ΔV is calculated from the feeder impedance.
         */

        fun cumulativeVoltageDrop(
            nodeId: String,
            upstreamVoltage: Double
        ): Double {

            val children =
                topology.children[nodeId]
                    .orEmpty()

            if (children.isEmpty()) {
                return 0.0
            }

            var maximum =
                0.0

            children.forEach { child ->

                val connection =
                    topology.connections.firstOrNull {
                        it.fromNodeId == nodeId &&
                            it.toNodeId == child.id
                    }
                        ?: return@forEach

                val downstreamVoltage =
                    child.voltage
                        .takeIf {
                            it > 0.0
                        }
                        ?: upstreamVoltage

                /*
                 * BUSBAR has no cable voltage drop.
                 */
                if (
                    connection.connectionType ==
                    SldConnectionType.BUSBAR
                ) {

                    val downstreamDrop =
                        cumulativeVoltageDrop(
                            nodeId = child.id,
                            upstreamVoltage =
                                downstreamVoltage
                        )

                    maximum =
                        maxOf(
                            maximum,
                            downstreamDrop
                        )

                    return@forEach
                }

                val childPower =
                    powerCache[
                        child.id
                    ]
                        ?: return@forEach

                val childKva =
                    apparentPower(
                        activeKw =
                            childPower.demandKw,
                        reactiveKvar =
                            childPower.reactiveKvar
                    )

                /*
                 * Current is based on the sending-end voltage.
                 */
                val current =
                    threePhaseCurrent(
                        kva = childKva,
                        voltage =
                            upstreamVoltage
                    )

                val sectionDrop =
                    calculateVoltageDrop(
                        connection = connection,
                        currentA = current,
                        voltage =
                            upstreamVoltage
                    )

                val downstreamDrop =
                    cumulativeVoltageDrop(
                        nodeId = child.id,
                        upstreamVoltage =
                            downstreamVoltage
                    )

                maximum =
                    maxOf(
                        maximum,
                        sectionDrop +
                            downstreamDrop
                    )
            }

            return maximum
        }

        /*
         * ==========================================================
         * NODE RESULTS
         * ==========================================================
         */

        val nodeResults =
            network.nodes.map { node ->

                val power =
                    powerCache[node.id]
                        ?: PowerResult(
                            connectedKw = 0.0,
                            demandKw = 0.0,
                            reactiveKvar = 0.0
                        )

                val kva =
                    apparentPower(
                        activeKw =
                            power.demandKw,
                        reactiveKvar =
                            power.reactiveKvar
                    )

                val current =
                    threePhaseCurrent(
                        kva = kva,
                        voltage = node.voltage
                    )

                val breaker =
                    nextBreaker(
                        current = current,
                        breakerRatings =
                            breakerRatings
                    )

                val voltageDrop =
                    cumulativeVoltageDrop(
                        nodeId = node.id,
                        upstreamVoltage =
                            node.voltage
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

                val calculatedPf =
                    if (kva > 0.0) {
                        power.demandKw /
                            kva
                    } else {
                        1.0
                    }

                NodeResult(
                    nodeId = node.id,
                    nodeName = node.name,
                    connectedKw =
                        power.connectedKw,
                    demandKw =
                        power.demandKw,
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
                                    "%.2f kW".format(
                                        power.connectedKw
                                    )
                            )

                            add(
                                "Demand load = " +
                                    "%.2f kW".format(
                                        power.demandKw
                                    )
                            )

                            add(
                                "Required apparent power = " +
                                    "%.2f kVA".format(
                                        kva
                                    )
                            )

                            add(
                                "Calculated demand PF = " +
                                    "%.3f".format(
                                        calculatedPf
                                    )
                            )

                            add(
                                "Design current = " +
                                    "%.2f A".format(
                                        current
                                    )
                            )

                            add(
                                "Recommended breaker = " +
                                    "%.0f A".format(
                                        breaker
                                    )
                            )

                            if (
                                node.ratedKva > 0.0
                            ) {
                                add(
                                    "Equipment loading = " +
                                        "%.1f %%".format(
                                            loading
                                        )
                                )
                            }

                            add(
                                "Maximum downstream voltage drop = " +
                                    "%.2f %%".format(
                                        voltageDrop
                                    )
                            )

                            if (
                                voltageDrop >
                                voltageDropLimitPercent
                            ) {
                                add(
                                    "WARNING: voltage drop exceeds " +
                                        "%.2f %%".format(
                                            voltageDropLimitPercent
                                        ) +
                                        " limit."
                                )
                            }

                            if (
                                node.ratedKva > 0.0 &&
                                loading > 100.0
                            ) {
                                add(
                                    "WARNING: equipment loading exceeds 100%."
                                )
                            }

                            engineeringContext?.let {
                                context ->

                                add(
                                    "Engineering standard = " +
                                        context.codeName
                                )

                                add(
                                    "Standard revision = " +
                                        context.codeRevision
                                )
                            }
                        }
                )
            }

        val nodeResultMap =
            nodeResults.associateBy {
                it.nodeId
            }

        /*
         * ==========================================================
         * FEEDER RESULTS
         * ==========================================================
         */

        val feederResults =
            topology.connections.map { connection ->

                val parent =
                    nodeMap[
                        connection.fromNodeId
                    ]

                val child =
                    nodeMap[
                        connection.toNodeId
                    ]

                val childResult =
                    nodeResultMap[
                        connection.toNodeId
                    ]

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

                val sendingVoltage =
                    parent
                        ?.voltage
                        ?.takeIf {
                            it > 0.0
                        }
                        ?: child
                            ?.voltage
                            ?.takeIf {
                                it > 0.0
                            }
                        ?: 400.0

                val isBusbar =
                    connection.connectionType ==
                        SldConnectionType.BUSBAR

                val sectionDrop =
                    if (isBusbar) {
                        0.0
                    } else {
                        calculateVoltageDrop(
                            connection =
                                connection,
                            currentA =
                                current,
                            voltage =
                                sendingVoltage
                        )
                    }

                /*
                 * IMPORTANT:
                 *
                 * For a cable:
                 *
                 * capacity <= 0
                 *
                 * means NOT VERIFIED.
                 *
                 * It must not be reported as adequate.
                 *
                 * Boolean compatibility is preserved:
                 * false = not verified / not adequate.
                 */
                val cableAdequate =
                    if (isBusbar) {
                        true
                    } else {
                        connection.currentCapacityA > 0.0 &&
                            connection.currentCapacityA >= current
                    }

                val recommendedBreaker =
                    nextBreaker(
                        current = current,
                        breakerRatings =
                            breakerRatings
                    )

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
                        recommendedBreaker,
                    voltageDropPercent =
                        sectionDrop,
                    cableAdequate =
                        cableAdequate,
                    notes =
                        buildList {

                            add(
                                if (isBusbar) {
                                    "Connection type = BUSBAR"
                                } else {
                                    "Connection type = CABLE"
                                }
                            )

                            add(
                                "Connected load = " +
                                    "%.2f kW".format(
                                        connected
                                    )
                            )

                            add(
                                "Demand load = " +
                                    "%.2f kW".format(
                                        demand
                                    )
                            )

                            add(
                                "Required apparent power = " +
                                    "%.2f kVA".format(
                                        kva
                                    )
                            )

                            add(
                                "Design current = " +
                                    "%.2f A".format(
                                        current
                                    )
                            )

                            if (isBusbar) {

                                add(
                                    "BUSBAR: cable length, " +
                                        "cable size, cable capacity " +
                                        "and cable impedance are not applicable."
                                )

                                add(
                                    "BUSBAR voltage drop = 0.00 %."
                                )

                                if (
                                    connection.busbarRatedCurrentA <=
                                    0.0
                                ) {
                                    add(
                                        "WARNING: BUSBAR rated current is not verified."
                                    )
                                }

                                if (
                                    connection.busbarShortCircuitKA <=
                                    0.0
                                ) {
                                    add(
                                        "WARNING: BUSBAR short-circuit withstand is not verified."
                                    )
                                }

                            } else {

                                if (
                                    connection.currentCapacityA >
                                    0.0
                                ) {

                                    add(
                                        "Cable capacity = " +
                                            "%.2f A".format(
                                                connection.currentCapacityA
                                            )
                                    )

                                    if (!cableAdequate) {
                                        add(
                                            "WARNING: cable capacity is below design current."
                                        )
                                    }

                                } else {

                                    add(
                                        "NOT VERIFIED: cable current capacity has not been established."
                                    )
                                }

                                if (
                                    connection.cableSizeMm2 >
                                    0.0
                                ) {

                                    add(
                                        "Cable size = " +
                                            "%.2f mm²".format(
                                                connection.cableSizeMm2
                                            )
                                    )

                                } else {

                                    add(
                                        "NOT VERIFIED: cable size has not been established."
                                    )
                                }

                                if (
                                    connection.lengthMeters >
                                    0.0
                                ) {

                                    add(
                                        "Cable length = " +
                                            "%.2f m".format(
                                                connection.lengthMeters
                                            )
                                    )

                                } else {

                                    add(
                                        "WARNING: cable length is zero."
                                    )
                                }

                                add(
                                    "Cable section voltage drop = " +
                                        "%.2f %%".format(
                                            sectionDrop
                                        )
                                )

                                if (
                                    sectionDrop >
                                    voltageDropLimitPercent
                                ) {

                                    add(
                                        "WARNING: section voltage drop exceeds " +
                                            "%.2f %%".format(
                                                voltageDropLimitPercent
                                            ) +
                                            " limit."
                                    )
                                }
                            }

                            add(
                                "Recommended breaker = " +
                                    "%.0f A".format(
                                        recommendedBreaker
                                    )
                            )

                            engineeringContext?.let {
                                context ->

                                add(
                                    "Engineering standard = " +
                                        context.codeName
                                )

                                add(
                                    "Standard revision = " +
                                        context.codeRevision
                                )
                            }
                        }
                )
            }

        /*
         * ==========================================================
         * SOURCE RESULT
         * ==========================================================
         */

        val sourceResult =
            nodeResultMap[
                topology.source.id
            ]
                ?: error(
                    "Unable to calculate source engineering result."
                )

        /*
         * ==========================================================
         * TRANSFORMER RECOMMENDATION
         * ==========================================================
         */

        val recommendedTransformer =
            if (
                topology.source.type ==
                SldNodeType.TRANSFORMER &&
                topology.source.ratedKva > 0.0
            ) {

                topology.source.ratedKva

            } else {

                nextTransformer(
                    sourceResult.kva
                )
            }

        /*
         * ==========================================================
         * ENGINEERING WARNINGS
         * ==========================================================
         */

        val warnings =
            buildList {

                feederResults
                    .filter {
                        !it.cableAdequate &&
                            !isBusbarConnection(
                                network =
                                    network,
                                connectionId =
                                    it.connectionId
                            )
                    }
                    .forEach {

                        add(
                            "Cable capacity not verified or inadequate at connection " +
                                "${it.connectionId}."
                        )
                    }

                feederResults
                    .filter {
                        isBusbarConnection(
                            network =
                                network,
                            connectionId =
                                it.connectionId
                        )
                    }
                    .forEach { feeder ->

                        val connection =
                            network.connections
                                .firstOrNull {
                                    it.id ==
                                        feeder.connectionId
                                }

                        if (
                            connection != null &&
                            connection.busbarRatedCurrentA <=
                            0.0
                        ) {

                            add(
                                "BUSBAR current rating not verified at connection " +
                                    "${feeder.connectionId}."
                            )
                        }

                        if (
                            connection != null &&
                            connection.busbarShortCircuitKA <=
                            0.0
                        ) {

                            add(
                                "BUSBAR short-circuit rating not verified at connection " +
                                    "${feeder.connectionId}."
                            )
                        }
                    }

                nodeResults
                    .filter {
                        it.loadingPercent > 100.0
                    }
                    .forEach {

                        add(
                            "Equipment overload at " +
                                "${it.nodeName}."
                        )
                    }

                nodeResults
                    .filter {
                        it.voltageDropPercent >
                            voltageDropLimitPercent
                    }
                    .forEach {

                        add(
                            "Voltage drop above " +
                                "%.2f %%".format(
                                    voltageDropLimitPercent
                                ) +
                                " at ${it.nodeName}."
                        )
                    }

                if (
                    sourceResult.kva > 0.0 &&
                    recommendedTransformer > 0.0 &&
                    recommendedTransformer <
                    sourceResult.kva
                ) {

                    add(
                        "Recommended transformer capacity is below calculated demand."
                    )
                }

                engineeringContext?.let {
                    context ->

                    if (
                        !context.standardImplemented
                    ) {

                        add(
                            "WARNING: selected engineering standard " +
                                "${context.codeName} is not fully implemented " +
                                "with verified datasets."
                        )
                    }

                    if (
                        breakerRatings.isEmpty()
                    ) {

                        add(
                            "WARNING: selected engineering standard " +
                                "${context.codeName} provides no verified " +
                                "breaker ratings. No standard substitution was made."
                        )
                    }
                }
            }

        /*
         * ==========================================================
         * FINAL RESULT
         * ==========================================================
         */

        return Result(
            sourceNodeId =
                topology.source.id,

            sourceName =
                topology.source.name,

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

    /**
     * Resolve breaker ratings.
     *
     * When EngineeringContext exists, the selected standard is
     * authoritative.
     *
     * Legacy fallback exists only for the old calculate(network)
     * API.
     */
    private fun resolveBreakerRatings(
        engineeringContext:
            SldEngineeringContext?
    ): List<Double> {

        if (
            engineeringContext == null
        ) {
            return defaultBreakerRatings
        }

        return engineeringContext
            .standardEngine
            .standardBreakerRatings()
            .filter {
                it.isFinite() &&
                    it > 0.0
            }
            .distinct()
            .sorted()
    }

    /**
     * Reactive power:
     *
     * Q = P × tan(acos(PF))
     */
    private fun calculateReactivePower(
        activeKw: Double,
        powerFactor: Double
    ): Double {

        if (
            activeKw <= 0.0
        ) {
            return 0.0
        }

        val pf =
            powerFactor.coerceIn(
                0.01,
                1.0
            )

        val angle =
            acos(pf)

        return activeKw *
            tan(angle)
    }

    /**
     * Apparent power:
     *
     * S = √(P² + Q²)
     */
    private fun apparentPower(
        activeKw: Double,
        reactiveKvar: Double
    ): Double {

        if (
            activeKw <= 0.0 &&
            reactiveKvar <= 0.0
        ) {
            return 0.0
        }

        return sqrt(
            activeKw * activeKw +
                reactiveKvar * reactiveKvar
        )
    }

    /**
     * Three-phase current:
     *
     * I = S × 1000 / (√3 × V)
     */
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
            sqrt(3.0) * voltage
            )
    }

    /**
     * Cable voltage drop.
     *
     * R and X are ohm/km.
     *
     * Parallel cable runs reduce effective impedance.
     *
     * BUSBAR is explicitly excluded.
     */
    private fun calculateVoltageDrop(
        connection: SldConnection,
        currentA: Double,
        voltage: Double
    ): Double {

        if (
            connection.connectionType ==
            SldConnectionType.BUSBAR
        ) {
            return 0.0
        }

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

        val impedance =
            sqrt(
                resistance * resistance +
                    reactance * reactance
            )

        val dropVolts =
            sqrt(3.0) *
                currentA *
                impedance *
                connection.lengthMeters /
                1000.0

        return (
            dropVolts /
                voltage
            ) * 100.0
    }

    /**
     * Smallest available breaker >= design current.
     */
    private fun nextBreaker(
        current: Double,
        breakerRatings: List<Double>
    ): Double {

        if (
            current <= 0.0 ||
            breakerRatings.isEmpty()
        ) {
            return 0.0
        }

        return breakerRatings.firstOrNull {
            it >= current
        } ?: breakerRatings.last()
    }

    /**
     * Smallest standard transformer rating >= demand.
     */
    private fun nextTransformer(
        kva: Double
    ): Double {

        if (
            kva <= 0.0
        ) {
            return 0.0
        }

        return transformerRatings.firstOrNull {
            it >= kva
        } ?: transformerRatings.last()
    }

    /**
     * BUSBAR helper.
     */
    private fun isBusbarConnection(
        network: SldNetwork,
        connectionId: String
    ): Boolean {

        return network.connections
            .firstOrNull {
                it.id == connectionId
            }
            ?.connectionType ==
            SldConnectionType.BUSBAR
    }

    /**
     * Basic numerical/input validation.
     *
     * Topology validation remains centralized in
     * SldTopologyEngine.
     */
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

        val connectionIds =
            network.connections.map {
                it.id
            }

        require(
            connectionIds.size ==
                connectionIds.toSet().size
        ) {
            "Duplicate SLD connection IDs."
        }

        network.nodes.forEach { node ->

            require(
                node.id.isNotBlank()
            ) {
                "SLD node ID cannot be blank."
            }

            require(
                node.name.isNotBlank()
            ) {
                "SLD node name cannot be blank."
            }

            require(
                node.voltage.isFinite() &&
                    node.voltage > 0.0
            ) {
                "Invalid voltage at ${node.name}."
            }

            require(
                node.powerFactor.isFinite() &&
                    node.powerFactor > 0.0 &&
                    node.powerFactor <= 1.0
            ) {
                "Invalid power factor at ${node.name}."
            }

            require(
                node.demandFactor.isFinite() &&
                    node.demandFactor >= 0.0 &&
                    node.demandFactor <= 1.0
            ) {
                "Invalid demand factor at ${node.name}."
            }

            require(
                node.loadKw.isFinite() &&
                    node.loadKw >= 0.0
            ) {
                "Invalid load at ${node.name}."
            }

            require(
                node.ratedKva.isFinite() &&
                    node.ratedKva >= 0.0
            ) {
                "Invalid rated kVA at ${node.name}."
            }
        }

        network.connections.forEach { connection ->

            require(
                connection.id.isNotBlank()
            ) {
                "SLD connection ID cannot be blank."
            }

            require(
                connection.fromNodeId in ids
            ) {
                "Connection ${connection.id}: source node does not exist."
            }

            require(
                connection.toNodeId in ids
            ) {
                "Connection ${connection.id}: destination node does not exist."
            }

            require(
                connection.fromNodeId !=
                    connection.toNodeId
            ) {
                "Connection ${connection.id}: node cannot connect to itself."
            }

            require(
                connection.parallelRuns >= 1
            ) {
                "Connection ${connection.id}: parallel cable runs must be at least 1."
            }

            require(
                connection.lengthMeters.isFinite() &&
                    connection.lengthMeters >= 0.0
            ) {
                "Connection ${connection.id}: cable length cannot be negative."
            }

            require(
                connection.cableSizeMm2.isFinite() &&
                    connection.cableSizeMm2 >= 0.0
            ) {
                "Connection ${connection.id}: cable size cannot be negative."
            }

            require(
                connection.currentCapacityA.isFinite() &&
                    connection.currentCapacityA >= 0.0
            ) {
                "Connection ${connection.id}: cable current capacity cannot be negative."
            }

            require(
                connection.resistanceOhmPerKm.isFinite() &&
                    connection.resistanceOhmPerKm >= 0.0
            ) {
                "Connection ${connection.id}: cable resistance cannot be negative."
            }

            require(
                connection.reactanceOhmPerKm.isFinite() &&
                    connection.reactanceOhmPerKm >= 0.0
            ) {
                "Connection ${connection.id}: cable reactance cannot be negative."
            }

            require(
                connection.busbarRatedCurrentA.isFinite() &&
                    connection.busbarRatedCurrentA >= 0.0
            ) {
                "Connection ${connection.id}: BUSBAR rated current cannot be negative."
            }

            require(
                connection.busbarShortCircuitKA.isFinite() &&
                    connection.busbarShortCircuitKA >= 0.0
            ) {
                "Connection ${connection.id}: BUSBAR short-circuit rating cannot be negative."
            }
        }
    }
}
