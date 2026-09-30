package com.electrical.calculationspro.data

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

data class SldShortCircuitResult(
    val nodeId: String,
    val nodeName: String,
    val voltageV: Double,
    val resistanceOhm: Double,
    val reactanceOhm: Double,
    val impedanceOhm: Double,
    val xrRatio: Double,
    val shortCircuitMva: Double,
    val initialSymmetricalCurrentKa: Double,
    val peakCurrentKa: Double,
    val thermalCurrentKa: Double,
    val breakerRequiredKa: Double,
    val notes: List<String> = emptyList()
)

data class SldShortCircuitStudy(
    val results: Map<String, SldShortCircuitResult>,
    val maximumFaultCurrentKa: Double,
    val maximumPeakCurrentKa: Double,
    val maximumFaultMva: Double,
    val notes: List<String>
)

object SldShortCircuitEngine {

    private const val SQRT_3 = 1.7320508075688772
    private const val SQRT_2 = 1.4142135623730951
    private const val EPS = 1.0e-9

    /**
     * Preliminary impedance-based short-circuit calculation.
     *
     * The topology direction is always taken from SldTopologyEngine.
     *
     * The calculation currently supports:
     *
     * - Utility source fault level
     * - Source fault current
     * - Transformer %Z
     * - Generator sub-transient reactance
     * - Feeder R/X
     * - Parallel feeder runs
     * - Ik''
     * - X/R
     * - Peak current Ip
     * - Preliminary thermal current
     * - Preliminary breaker short-circuit rating
     *
     * This is intentionally not presented as a complete IEC 60909
     * implementation. Full compliance requires additional network,
     * equipment and correction-factor data.
     */
    fun calculate(
        network: SldNetwork,
        voltageFactor: Double = 1.05
    ): SldShortCircuitStudy {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        require(voltageFactor > 0.0) {
            "Voltage factor must be greater than zero."
        }

        val topology =
            SldTopologyEngine.build(network)

        val nodeMap =
            network.nodes.associateBy {
                it.id
            }

        val children =
            mutableMapOf<String, MutableList<SldConnection>>()

        network.nodes.forEach { node ->
            children[node.id] =
                mutableListOf()
        }

        topology.connections.forEach { connection ->

            children[
                connection.fromNodeId
            ]?.add(connection)
        }

        val results =
            linkedMapOf<String, SldShortCircuitResult>()

        /*
         * --------------------------------------------------------
         * SOURCE IMPEDANCE
         * --------------------------------------------------------
         *
         * Prefer source short-circuit MVA.
         *
         * If MVA is unavailable, use the explicit source short-
         * circuit current when supplied by the project.
         */
        fun sourceImpedance(
            source: SldNode
        ): Pair<Double, Double> {

            val voltage =
                source.voltage
                    .coerceAtLeast(1.0)

            val faultMva =
                when {

                    source.sourceShortCircuitMva > EPS ->
                        source.sourceShortCircuitMva

                    source.sourceShortCircuitKA > EPS -> {

                        SQRT_3 *
                            voltage *
                            source.sourceShortCircuitKA *
                            1000.0 /
                            1_000_000.0
                    }

                    else ->
                        0.0
                }

            if (faultMva <= EPS) {
                return 0.0 to 0.0
            }

            val z =
                voltage * voltage /
                    (
                        faultMva *
                            1_000_000.0
                        )

            /*
             * The available source fault level defines |Z|.
             *
             * A predominantly-reactive source model is retained
             * because an explicit source R/X ratio is not yet
             * represented in SldNode.
             */
            val x =
                z * 0.995

            val r =
                sqrt(
                    max(
                        0.0,
                        z * z -
                            x * x
                    )
                )

            return r to x
        }

        /*
         * --------------------------------------------------------
         * TRANSFORMER IMPEDANCE
         * --------------------------------------------------------
         */
        fun transformerImpedance(
            transformer: SldNode
        ): Pair<Double, Double> {

            val ratedKva =
                transformer.ratedKva

            val percentZ =
                transformer.transformerPercentZ

            if (
                ratedKva <= EPS ||
                percentZ <= EPS
            ) {
                return 0.0 to 0.0
            }

            /*
             * Prefer explicit primary voltage.
             *
             * If not available, preserve compatibility by using
             * the node voltage.
             */
            val voltage =
                when {

                    transformer
                        .transformerPrimaryVoltage >
                        EPS ->

                        transformer
                            .transformerPrimaryVoltage

                    else ->
                        transformer.voltage
                }
                    .coerceAtLeast(1.0)

            val baseZ =
                voltage * voltage /
                    (
                        ratedKva *
                            1000.0
                        )

            val z =
                baseZ *
                    percentZ /
                    100.0

            /*
             * No transformer R/X data exists in the current model.
             * Therefore retain the established predominantly
             * reactive approximation.
             */
            val x =
                z * 0.995

            val r =
                sqrt(
                    max(
                        0.0,
                        z * z -
                            x * x
                    )
                )

            return r to x
        }

        /*
         * --------------------------------------------------------
         * GENERATOR IMPEDANCE
         * --------------------------------------------------------
         */
        fun generatorImpedance(
            generator: SldNode
        ): Pair<Double, Double> {

            val ratedKva =
                when {

                    generator.generatorRatedKva >
                        EPS ->

                        generator.generatorRatedKva

                    generator.ratedKva >
                        EPS ->

                        generator.ratedKva

                    else ->
                        0.0
                }

            val xdSubtransient =
                generator.generatorXdSubtransient

            if (
                ratedKva <= EPS ||
                xdSubtransient <= EPS
            ) {
                return 0.0 to 0.0
            }

            val voltage =
                generator.voltage
                    .coerceAtLeast(1.0)

            val baseZ =
                voltage * voltage /
                    (
                        ratedKva *
                            1000.0
                        )

            val x =
                baseZ *
                    xdSubtransient /
                    100.0

            /*
             * Generator R/X is not explicitly stored in the current
             * SLD model.
             *
             * Keep the established preliminary R approximation.
             */
            val r =
                x * 0.10

            return r to x
        }

        /*
         * --------------------------------------------------------
         * FEEDER IMPEDANCE
         * --------------------------------------------------------
         *
         * BUSBAR is intentionally treated as zero feeder impedance.
         *
         * Cable impedance is divided by the number of parallel runs.
         */
        fun feederImpedance(
            connection: SldConnection
        ): Pair<Double, Double> {

            if (
                connection.connectionType ==
                SldConnectionType.BUSBAR
            ) {
                return 0.0 to 0.0
            }

            val lengthKm =
                connection.lengthMeters
                    .coerceAtLeast(0.0) /
                    1000.0

            if (lengthKm <= EPS) {
                return 0.0 to 0.0
            }

            val runs =
                connection.parallelRuns
                    .coerceAtLeast(1)

            val resistance =
                connection.resistanceOhmPerKm *
                    lengthKm /
                    runs

            val reactance =
                connection.reactanceOhmPerKm *
                    lengthKm /
                    runs

            return resistance to reactance
        }

        fun addSeries(
            first: Pair<Double, Double>,
            second: Pair<Double, Double>
        ): Pair<Double, Double> {

            return (
                first.first +
                    second.first
                ) to (
                first.second +
                    second.second
                )
        }

        /*
         * --------------------------------------------------------
         * NODE CALCULATION
         * --------------------------------------------------------
         */
        fun calculateNode(
            nodeId: String,
            upstreamR: Double,
            upstreamX: Double,
            visited: MutableSet<String>
        ) {

            /*
             * Defensive cycle protection.
             *
             * SldTopologyEngine already validates topology.
             */
            if (!visited.add(nodeId)) {
                return
            }

            val node =
                nodeMap[nodeId]
                    ?: return

            var resistance =
                upstreamR

            var reactance =
                upstreamX

            /*
             * Add equipment impedance only where the equipment
             * itself contributes a source/series impedance.
             */
            when (node.type) {

                SldNodeType.SOURCE -> {

                    val impedance =
                        sourceImpedance(node)

                    resistance +=
                        impedance.first

                    reactance +=
                        impedance.second
                }

                SldNodeType.TRANSFORMER -> {

                    val impedance =
                        transformerImpedance(node)

                    resistance +=
                        impedance.first

                    reactance +=
                        impedance.second
                }

                SldNodeType.GENERATOR -> {

                    val impedance =
                        generatorImpedance(node)

                    resistance +=
                        impedance.first

                    reactance +=
                        impedance.second
                }

                SldNodeType.BUS,
                SldNodeType.PANEL,
                SldNodeType.BREAKER,
                SldNodeType.LOAD -> Unit
            }

            val impedanceMagnitude =
                sqrt(
                    resistance * resistance +
                        reactance * reactance
                )

            val voltage =
                node.voltage
                    .coerceAtLeast(1.0)

            val faultVoltage =
                voltageFactor *
                    voltage

            /*
             * ----------------------------------------------------
             * INITIAL SYMMETRICAL SHORT-CIRCUIT CURRENT
             * ----------------------------------------------------
             *
             * Ik'' = c * Un / (sqrt(3) * |Z|)
             *
             * The current is reported in amperes internally and
             * converted to kA in the result.
             */
            val initialCurrentA =
                if (
                    impedanceMagnitude >
                    EPS
                ) {

                    faultVoltage /
                        (
                            SQRT_3 *
                                impedanceMagnitude
                            )

                } else {
                    0.0
                }

            /*
             * Fault level is calculated using the actual system
             * voltage, not the voltage factor.
             */
            val faultMva =
                if (
                    initialCurrentA >
                    EPS
                ) {

                    SQRT_3 *
                        voltage *
                        initialCurrentA /
                        1_000_000.0

                } else {
                    0.0
                }

            val xrRatio =
                when {

                    resistance >
                        EPS ->

                        reactance /
                            resistance

                    reactance >
                        EPS ->

                        999.0

                    else ->
                        0.0
                }

            val kappa =
                calculateKappa(
                    xrRatio
                )

            val peakCurrentA =
                kappa *
                    SQRT_2 *
                    initialCurrentA

            /*
             * Current model does not yet contain clearing-time
             * dependent thermal decay / Ith parameters.
             *
             * Therefore retain Ik'' as the preliminary thermal
             * current indicator and clearly identify it in notes.
             */
            val thermalCurrentA =
                initialCurrentA

            val breakerRequiredKa =
                nextShortCircuitRating(
                    initialCurrentA /
                        1000.0
                )

            val notes =
                buildList {

                    add(
                        "R = %.6f Ω"
                            .format(
                                resistance
                            )
                    )

                    add(
                        "X = %.6f Ω"
                            .format(
                                reactance
                            )
                    )

                    add(
                        "Z = %.6f Ω"
                            .format(
                                impedanceMagnitude
                            )
                    )

                    add(
                        "X/R = %.3f"
                            .format(
                                xrRatio
                            )
                    )

                    add(
                        "Ik'' = %.3f kA"
                            .format(
                                initialCurrentA /
                                    1000.0
                            )
                    )

                    add(
                        "Ip = %.3f kA"
                            .format(
                                peakCurrentA /
                                    1000.0
                            )
                    )

                    add(
                        "Fault level = %.3f MVA"
                            .format(
                                faultMva
                            )
                    )

                    add(
                        "Preliminary breaker short-circuit rating >= %.1f kA"
                            .format(
                                breakerRequiredKa
                            )
                    )

                    if (
                        node.type ==
                        SldNodeType.SOURCE &&
                        node.sourceShortCircuitMva <= EPS &&
                        node.sourceShortCircuitKA <= EPS
                    ) {
                        add(
                            "Source short-circuit level is not provided; fault current cannot be verified."
                        )
                    }

                    if (
                        node.type ==
                        SldNodeType.TRANSFORMER &&
                        (
                            node.ratedKva <= EPS ||
                                node.transformerPercentZ <= EPS
                            )
                    ) {
                        add(
                            "Transformer rated kVA and %Z are incomplete; transformer impedance contribution is not verified."
                        )
                    }

                    if (
                        node.type ==
                        SldNodeType.GENERATOR &&
                        (
                            (
                                node.generatorRatedKva <= EPS &&
                                    node.ratedKva <= EPS
                                ) ||
                                node.generatorXdSubtransient <= EPS
                            )
                    ) {
                        add(
                            "Generator rated kVA and Xd'' are incomplete; generator fault contribution is not verified."
                        )
                    }
                }

            results[node.id] =
                SldShortCircuitResult(

                    nodeId =
                        node.id,

                    nodeName =
                        node.name,

                    voltageV =
                        voltage,

                    resistanceOhm =
                        resistance,

                    reactanceOhm =
                        reactance,

                    impedanceOhm =
                        impedanceMagnitude,

                    xrRatio =
                        xrRatio,

                    shortCircuitMva =
                        faultMva,

                    initialSymmetricalCurrentKa =
                        initialCurrentA /
                            1000.0,

                    peakCurrentKa =
                        peakCurrentA /
                            1000.0,

                    thermalCurrentKa =
                        thermalCurrentA /
                            1000.0,

                    breakerRequiredKa =
                        breakerRequiredKa,

                    notes =
                        notes
                )

            /*
             * ----------------------------------------------------
             * PROPAGATE TOWARD DOWNSTREAM NODES
             * ----------------------------------------------------
             */
            children[node.id]
                ?.forEach { connection ->

                    val child =
                        nodeMap[
                            connection.toNodeId
                        ]
                            ?: return@forEach

                    val feeder =
                        feederImpedance(
                            connection
                        )

                    val nextImpedance =
                        addSeries(
                            resistance to
                                reactance,
                            feeder
                        )

                    calculateNode(
                        nodeId =
                            child.id,

                        upstreamR =
                            nextImpedance.first,

                        upstreamX =
                            nextImpedance.second,

                        visited =
                            visited.toMutableSet()
                    )
                }
        }

        /*
         * SldTopologyEngine guarantees one valid source for the
         * current radial SLD model.
         */
        calculateNode(
            nodeId =
                topology.source.id,

            upstreamR =
                0.0,

            upstreamX =
                0.0,

            visited =
                mutableSetOf()
        )

        require(
            results.size ==
                network.nodes.size
        ) {
            "Short-circuit calculation did not reach all SLD nodes."
        }

        val maximumFaultCurrent =
            results.values
                .maxOfOrNull {
                    it.initialSymmetricalCurrentKa
                }
                ?: 0.0

        val maximumPeakCurrent =
            results.values
                .maxOfOrNull {
                    it.peakCurrentKa
                }
                ?: 0.0

        val maximumFaultMva =
            results.values
                .maxOfOrNull {
                    it.shortCircuitMva
                }
                ?: 0.0

        val incompleteSourceData =
            network.nodes.any { node ->

                node.type ==
                    SldNodeType.SOURCE &&
                    node.sourceShortCircuitMva <= EPS &&
                    node.sourceShortCircuitKA <= EPS
            }

        return SldShortCircuitStudy(

            results =
                results.toMap(),

            maximumFaultCurrentKa =
                maximumFaultCurrent,

            maximumPeakCurrentKa =
                maximumPeakCurrent,

            maximumFaultMva =
                maximumFaultMva,

            notes =
                buildList {

                    add(
                        "Three-phase short-circuit study completed."
                    )

                    add(
                        "Electrical direction is taken from SldTopologyEngine."
                    )

                    add(
                        "External CABLE impedance is included; BUSBAR impedance is treated as zero in the current model."
                    )

                    add(
                        "Voltage factor c = %.3f"
                            .format(
                                voltageFactor
                            )
                    )

                    add(
                        "Maximum Ik'' = %.3f kA"
                            .format(
                                maximumFaultCurrent
                            )
                    )

                    add(
                        "Maximum peak current = %.3f kA"
                            .format(
                                maximumPeakCurrent
                            )
                    )

                    add(
                        "Maximum fault level = %.3f MVA"
                            .format(
                                maximumFaultMva
                            )
                    )

                    add(
                        "Method: impedance-based IEC 60909 style preliminary calculation."
                    )

                    add(
                        "Peak current uses an X/R-dependent preliminary kappa factor."
                    )

                    add(
                        "Thermal current currently represents preliminary Ik''; final Ith requires clearing-time and equipment-specific data."
                    )

                    if (
                        incompleteSourceData
                    ) {
                        add(
                            "WARNING: source short-circuit level/current is missing; maximum fault results are not fully verified."
                        )
                    }
                }
        )
    }

    /*
     * ------------------------------------------------------------
     * KAPPA
     * ------------------------------------------------------------
     *
     * Preliminary X/R-dependent peak-current factor.
     *
     * This keeps the current implementation numerically stable
     * while the complete IEC 60909 correction-factor model is
     * introduced.
     */
    private fun calculateKappa(
        xr: Double
    ): Double {

        if (
            xr <= EPS
        ) {
            return 1.02
        }

        val value =
            1.02 +
                0.98 *
                exp(
                    -3.0 /
                        xr
                )

        return value.coerceIn(
            1.02,
            2.0
        )
    }

    /*
     * ------------------------------------------------------------
     * BREAKER SHORT-CIRCUIT RATING
     * ------------------------------------------------------------
     *
     * Preliminary nominal selection only.
     *
     * Final Icu/Ics selection must come from the selected
     * manufacturer catalog and the applicable standard.
     */
    private fun nextShortCircuitRating(
        faultKa: Double
    ): Double {

        if (
            faultKa <= 0.0
        ) {
            return 0.0
        }

        val standardRatings =
            listOf(
                3.0,
                6.0,
                10.0,
                15.0,
                16.0,
                20.0,
                25.0,
                30.0,
                36.0,
                40.0,
                50.0,
                63.0,
                80.0,
                100.0
            )

        return standardRatings.firstOrNull {
            it >= faultKa
        } ?: 100.0
    }
}
