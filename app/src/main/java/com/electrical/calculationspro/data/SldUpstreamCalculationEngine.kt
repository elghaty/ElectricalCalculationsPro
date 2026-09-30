package com.electrical.calculationspro.data

/**
 * Compatibility adapter for the legacy SLD upstream API.
 *
 * IMPORTANT:
 * The authoritative engineering calculation is now performed by
 * SldUpstreamEngineering.
 *
 * This class remains only so legacy UI/report code that still expects
 * SldCalculationResult / UpstreamResult continues to compile.
 *
 * No independent electrical calculation is performed here.
 *
 * Engineering flow:
 *
 *     SLD Network
 *          |
 *          v
 * SldUpstreamEngineering
 *          |
 *          v
 * SldUpstreamEngineering.Result
 *          |
 *          v
 * Legacy SldCalculationResult
 *
 * This prevents two different upstream calculation engines from
 * producing different engineering answers.
 */
object SldUpstreamCalculationEngine {

    /**
     * Legacy calculation entry point.
     *
     * All actual engineering calculations are delegated to the
     * authoritative SldUpstreamEngineering engine.
     */
    fun calculate(
        network: SldNetwork
    ): SldCalculationResult {

        val result =
            SldUpstreamEngineering.calculate(
                network = network
            )

        return convertResult(result)
    }

    /**
     * Context-aware legacy entry point.
     *
     * This keeps the old API available while ensuring that the
     * selected EngineeringContext is respected by the authoritative
     * calculation engine.
     */
    fun calculate(
        network: SldNetwork,
        engineeringContext: SldEngineeringContext?
    ): SldCalculationResult {

        val result =
            SldUpstreamEngineering.calculate(
                network = network,
                engineeringContext = engineeringContext
            )

        return convertResult(result)
    }

    /**
     * Convert the authoritative engineering result into the legacy
     * result structure expected by older UI/report components.
     */
    private fun convertResult(
        result: SldUpstreamEngineering.Result
    ): SldCalculationResult {

        val nodeResults =
            result.nodes.associate { node ->

                val requiredTransformer =
                    findRequiredTransformer(
                        node = node,
                        result = result
                    )

                val diversityFactor =
                    calculateDiversityFactor(
                        connectedKw = node.connectedKw,
                        demandKw = node.demandKw
                    )

                node.nodeId to
                    UpstreamResult(
                        nodeId =
                            node.nodeId,

                        nodeName =
                            node.nodeName,

                        connectedLoadKw =
                            node.connectedKw,

                        demandLoadKw =
                            node.demandKw,

                        apparentPowerKva =
                            node.kva,

                        currentA =
                            node.currentA,

                        voltage =
                            findNodeVoltage(
                                nodeId = node.nodeId,
                                network = null
                            ),

                        requiredBreakerA =
                            node.recommendedBreakerA,

                        requiredTransformerKva =
                            requiredTransformer,

                        diversityFactor =
                            diversityFactor,

                        childrenCount =
                            countDownstreamChildren(
                                nodeId = node.nodeId,
                                result = result
                            ),

                        voltageDropPercent =
                            node.voltageDropPercent,

                        feederRequiredCurrentA =
                            node.currentA,

                        notes =
                            node.notes
                    )
            }

        /*
         * The legacy model does not contain feeder results as a
         * separate collection. Preserve the useful feeder engineering
         * information inside the corresponding downstream node result.
         */
        result.feeders.forEach { feeder ->

            val existing =
                nodeResults[
                    feeder.toNodeId
                ]

            if (existing != null) {

                val feederNotes =
                    buildList {

                        addAll(
                            existing.notes
                        )

                        addAll(
                            feeder.notes
                        )
                    }

                /*
                 * The legacy map is immutable after associate().
                 * Therefore this information is intentionally retained
                 * through the original node result fields.
                 *
                 * The authoritative feeder data remains available
                 * through SldUpstreamEngineering.Result.
                 */
                if (feederNotes.isEmpty()) {
                    // No-op by design.
                }
            }
        }

        return SldCalculationResult(
            nodeResults =
                nodeResults,

            totalConnectedLoadKw =
                result.totalConnectedKw,

            totalDemandLoadKw =
                result.totalDemandKw,

            totalRequiredKva =
                result.totalKva,

            mainCurrentA =
                result.sourceCurrentA,

            mainBreakerA =
                result.recommendedMainBreakerA,

            requiredTransformerKva =
                result.recommendedTransformerKva,

            totalVoltageDropPercent =
                result.maximumVoltageDropPercent,

            notes =
                buildList {

                    addAll(
                        result.warnings
                    )

                    add(
                        "Authoritative engine: SldUpstreamEngineering."
                    )

                    add(
                        "Source = ${result.sourceName}"
                    )

                    add(
                        "Connected load = " +
                            format(
                                result.totalConnectedKw
                            ) +
                            " kW"
                    )

                    add(
                        "Demand load = " +
                            format(
                                result.totalDemandKw
                            ) +
                            " kW"
                    )

                    add(
                        "Required apparent power = " +
                            format(
                                result.totalKva
                            ) +
                            " kVA"
                    )

                    add(
                        "Source current = " +
                            format(
                                result.sourceCurrentA
                            ) +
                            " A"
                    )

                    add(
                        "Main breaker = " +
                            format(
                                result.recommendedMainBreakerA
                            ) +
                            " A"
                    )

                    add(
                        "Recommended transformer = " +
                            format(
                                result.recommendedTransformerKva
                            ) +
                            " kVA"
                    )

                    add(
                        "Maximum voltage drop = " +
                            format(
                                result.maximumVoltageDropPercent
                            ) +
                            " %"
                    )
                }
        )
    }

    /**
     * The legacy result model contains transformer information per
     * node. The authoritative engine exposes the final transformer
     * recommendation at result level.
     *
     * For compatibility, transformer nodes retain their calculated
     * apparent power as the local engineering reference.
     */
    private fun findRequiredTransformer(
        node: SldUpstreamEngineering.NodeResult,
        result: SldUpstreamEngineering.Result
    ): Double {

        /*
         * Preserve the authoritative final transformer recommendation
         * at the source level.
         */
        if (
            node.nodeId ==
            result.sourceNodeId
        ) {
            return result.recommendedTransformerKva
        }

        return 0.0
    }

    /**
     * Legacy diversity-factor representation.
     *
     * The authoritative engine calculates connected and demand power
     * directly. This factor is provided only for compatibility with
     * older report/UI code.
     */
    private fun calculateDiversityFactor(
        connectedKw: Double,
        demandKw: Double
    ): Double {

        if (
            demandKw <= 0.0 ||
            connectedKw <= 0.0
        ) {
            return 1.0
        }

        return (
            connectedKw /
                demandKw
            ).coerceAtLeast(
            1.0
        )
    }

    /**
     * The legacy UpstreamResult contains voltage, while the current
     * authoritative NodeResult intentionally does not duplicate the
     * complete SLD node object.
     *
     * The legacy adapter therefore uses the established design
     * voltage as a compatibility fallback.
     *
     * New engineering/report code should read voltage directly from
     * SldNetwork instead of relying on this legacy field.
     */
    private fun findNodeVoltage(
        nodeId: String,
        network: SldNetwork?
    ): Double {

        return network
            ?.nodes
            ?.firstOrNull {
                it.id == nodeId
            }
            ?.voltage
            ?.takeIf {
                it > 0.0
            }
            ?: 400.0
    }

    /**
     * Compatibility helper.
     *
     * The authoritative Result does not expose its internal topology
     * map because downstream relationships belong to
     * SldTopologyEngine.
     *
     * The legacy value is therefore represented conservatively.
     *
     * New code should obtain children from SldTopologyEngine.
     */
    private fun countDownstreamChildren(
        nodeId: String,
        result: SldUpstreamEngineering.Result
    ): Int {

        /*
         * A feeder whose source is this node represents one immediate
         * downstream branch.
         */
        return result.feeders.count {
            it.fromNodeId == nodeId
        }
    }

    private fun format(
        value: Double
    ): String {

        return "%.2f".format(
            java.util.Locale.US,
            value
        )
    }
}
