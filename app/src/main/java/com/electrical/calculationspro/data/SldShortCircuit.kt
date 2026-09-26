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
        network.nodes.associateBy { it.id }

    val children =
        mutableMapOf<String, MutableList<SldConnection>>()

    network.nodes.forEach {
        children[it.id] = mutableListOf()
    }

    topology.connections.forEach { connection ->
        children[connection.fromNodeId]
            ?.add(connection)
    }

    val roots =
        listOf(topology.source)

    val results =
        linkedMapOf<String, SldShortCircuitResult>()

    fun sourceImpedance(
        source: SldNode
    ): Pair<Double, Double> {

        val voltage =
            source.voltage.coerceAtLeast(1.0)

        val faultMva =
            source.sourceShortCircuitMva

        if (faultMva <= EPS) {
            return 0.0 to 0.0
        }

        val z =
            voltage * voltage /
                (faultMva * 1_000_000.0)

        /*
         * Initial source impedance approximation.
         *
         * The source fault level defines the magnitude.
         * A small R/X component is retained so peak-current
         * calculation remains numerically defined.
         */
        val x =
            z * 0.995

        val r =
            sqrt(
                max(
                    0.0,
                    z * z - x * x
                )
            )

        return r to x
    }

    fun transformerImpedance(
        transformer: SldNode
    ): Pair<Double, Double> {

        if (
            transformer.ratedKva <= EPS ||
            transformer.transformerPercentZ <= EPS
        ) {
            return 0.0 to 0.0
        }

        val voltage =
            transformer.voltage.coerceAtLeast(1.0)

        val baseZ =
            voltage * voltage /
                (transformer.ratedKva * 1000.0)

        val z =
            baseZ *
                transformer.transformerPercentZ /
                100.0

        /*
         * Transformer impedance is normally predominantly
         * reactive for this preliminary model.
         */
        val x =
            z * 0.995

        val r =
            sqrt(
                max(
                    0.0,
                    z * z - x * x
                )
            )

        return r to x
    }

    fun generatorImpedance(
        generator: SldNode
    ): Pair<Double, Double> {

        if (
            generator.ratedKva <= EPS ||
            generator.generatorXdSubtransient <= EPS
        ) {
            return 0.0 to 0.0
        }

        val voltage =
            generator.voltage.coerceAtLeast(1.0)

        val baseZ =
            voltage * voltage /
                (generator.ratedKva * 1000.0)

        val x =
            baseZ *
                generator.generatorXdSubtransient /
                100.0

        val r =
            x * 0.10

        return r to x
    }

    fun feederImpedance(
        connection: SldConnection
    ): Pair<Double, Double> {

        val lengthKm =
            connection.lengthMeters.coerceAtLeast(0.0) /
                1000.0

        if (lengthKm <= EPS) {
            return 0.0 to 0.0
        }

        val runs =
            connection.parallelRuns.coerceAtLeast(1)

        val r =
            connection.resistanceOhmPerKm *
                lengthKm /
                runs

        val x =
            connection.reactanceOhmPerKm *
                lengthKm /
                runs

        return r to x
    }

    fun addSeries(
        first: Pair<Double, Double>,
        second: Pair<Double, Double>
    ): Pair<Double, Double> =
        (first.first + second.first) to
            (first.second + second.second)

    fun calculateNode(
        nodeId: String,
        upstreamR: Double,
        upstreamX: Double,
        visited: MutableSet<String>
    ) {

        if (!visited.add(nodeId)) {
            return
        }

        val node =
            nodeMap[nodeId]
                ?: return

        var r = upstreamR
        var x = upstreamX

        when (node.type) {

            SldNodeType.SOURCE -> {
                val z =
                    sourceImpedance(node)

                r += z.first
                x += z.second
            }

            SldNodeType.TRANSFORMER -> {
                val z =
                    transformerImpedance(node)

                r += z.first
                x += z.second
            }

            SldNodeType.GENERATOR -> {
                val z =
                    generatorImpedance(node)

                r += z.first
                x += z.second
            }

            else -> Unit
        }

        val impedance =
            sqrt(
                r * r +
                    x * x
            )

        val voltage =
            node.voltage.coerceAtLeast(1.0)

        val faultVoltage =
            voltageFactor * voltage

        /*
         * Three-phase initial symmetrical fault current:
         *
         * Ik'' = c * Un / (sqrt(3) * |Z|)
         */
        val initialCurrentA =
            if (impedance > EPS) {
                faultVoltage /
                    (SQRT_3 * impedance)
            } else {
                0.0
            }

        val faultMva =
            if (initialCurrentA > EPS) {
                SQRT_3 *
                    voltage *
                    initialCurrentA /
                    1_000_000.0
            } else {
                0.0
            }

        val xr =
            if (r > EPS) {
                x / r
            } else {
                999.0
            }

        val kappa =
            calculateKappa(xr)

        val peakA =
            kappa *
                SQRT_2 *
                initialCurrentA

        val thermalA =
            initialCurrentA

        val breakerKa =
            nextShortCircuitRating(
                initialCurrentA / 1000.0
            )

        val notes =
            buildList {

                add(
                    "R = %.6f Ω".format(r)
                )

                add(
                    "X = %.6f Ω".format(x)
                )

                add(
                    "Z = %.6f Ω".format(impedance)
                )

                add(
                    "X/R = %.3f".format(xr)
                )

                add(
                    "Ik'' = %.3f kA".format(
                        initialCurrentA / 1000.0
                    )
                )

                add(
                    "Ip = %.3f kA".format(
                        peakA / 1000.0
                    )
                )

                add(
                    "Fault level = %.3f MVA".format(
                        faultMva
                    )
                )

                add(
                    "Required breaker short-circuit rating >= %.1f kA"
                        .format(breakerKa)
                )
            }

        results[node.id] =
            SldShortCircuitResult(
                nodeId = node.id,
                nodeName = node.name,
                voltageV = voltage,
                resistanceOhm = r,
                reactanceOhm = x,
                impedanceOhm = impedance,
                xrRatio = xr,
                shortCircuitMva = faultMva,
                initialSymmetricalCurrentKa =
                    initialCurrentA / 1000.0,
                peakCurrentKa =
                    peakA / 1000.0,
                thermalCurrentKa =
                    thermalA / 1000.0,
                breakerRequiredKa =
                    breakerKa,
                notes = notes
            )

        children[node.id]
            ?.forEach { connection ->

                val child =
                    nodeMap[connection.toNodeId]
                        ?: return@forEach

                val feeder =
                    feederImpedance(connection)

                val next =
                    addSeries(
                        r to x,
                        feeder
                    )

                calculateNode(
                    nodeId = child.id,
                    upstreamR = next.first,
                    upstreamX = next.second,
                    visited = visited.toMutableSet()
                )
            }
    }

    roots.forEach { root ->
        calculateNode(
            nodeId = root.id,
            upstreamR = 0.0,
            upstreamX = 0.0,
            visited = mutableSetOf()
        )
    }

    require(results.size == network.nodes.size) {
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

    return SldShortCircuitStudy(
        results = results.toMap(),
        maximumFaultCurrentKa =
            maximumFaultCurrent,
        maximumPeakCurrentKa =
            maximumPeakCurrent,
        maximumFaultMva =
            maximumFaultMva,
        notes = buildList {

            add(
                "Three-phase short-circuit study completed."
            )

            add(
                "Electrical direction is taken from SldTopologyEngine."
            )

            add(
                "Method: impedance-based IEC 60909 style preliminary calculation."
            )

            add(
                "Voltage factor c = %.3f"
                    .format(voltageFactor)
            )

            add(
                "Maximum Ik'' = %.3f kA"
                    .format(maximumFaultCurrent)
            )

            add(
                "Maximum peak current = %.3f kA"
                    .format(maximumPeakCurrent)
            )

            add(
                "Maximum fault level = %.3f MVA"
                    .format(maximumFaultMva)
            )
        }
    )
}

private fun calculateKappa(
    xr: Double
): Double {

    if (xr <= EPS) {
        return 1.02
    }

    val value =
        1.02 +
            0.98 *
            exp(-3.0 / xr)

    return value.coerceIn(
        1.02,
        2.0
    )
}

private fun nextShortCircuitRating(
    faultKa: Double
): Double {

    if (faultKa <= 0.0) {
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
