package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD PROTECTION COORDINATION
 * ================================================================
 *
 * Protection coordination engine for the interactive SLD.
 *
 * Engineering flow:
 *
 * SLD
 *   ↓
 * Topology
 *   ↓
 * Upstream demand/current
 *   ↓
 * Cable sizing
 *   ↓
 * Short circuit
 *   ↓
 * Protection selection / preliminary coordination
 *
 * Important:
 *
 * This engine performs a PRELIMINARY engineering coordination study.
 *
 * Final protection coordination requires:
 *
 * - Actual manufacturer device
 * - Trip-unit type
 * - Long-time setting
 * - Short-time setting
 * - Instantaneous setting
 * - Ground-fault setting where applicable
 * - Time-current curves
 * - Icu / Ics
 * - Manufacturer discrimination tables
 * - Actual transformer impedance and source data
 *
 * The engine therefore does not claim final manufacturer selectivity.
 *
 * ================================================================
 */

data class SldProtectionDevice(
    val nodeId: String,
    val nodeName: String,
    val deviceType: String,

    /**
     * Calculated current carried by this protection point.
     */
    val downstreamCurrentA: Double,

    /**
     * Preliminary selected nominal breaker rating.
     */
    val recommendedRatingA: Double,

    /**
     * Required short-circuit breaking capacity from the
     * short-circuit study.
     */
    val shortCircuitRatingKa: Double,

    /**
     * Preliminary long-time pickup.
     */
    val longTimePickupA: Double,

    /**
     * Preliminary instantaneous pickup.
     */
    val instantaneousPickupA: Double,

    /**
     * Preliminary upstream/downstream selectivity margin.
     */
    val selectivityMarginA: Double,

    val status: ProtectionStatus,

    val notes: List<String> = emptyList()
)

enum class ProtectionStatus {
    PASS,
    WARNING,
    FAIL
}

data class SldProtectionCoordinationResult(
    val devices: Map<String, SldProtectionDevice>,
    val coordinatedPairs: Int,
    val warningPairs: Int,
    val failedPairs: Int,
    val notes: List<String>
)

object SldProtectionCoordinationEngine {

    private const val SQRT_3 = 1.7320508075688772

    /**
     * Preliminary design margin between calculated current and
     * selected breaker rating.
     */
    private const val BREAKER_MARGIN = 1.15

    /**
     * Preliminary long-time pickup.
     *
     * This is NOT a manufacturer trip-unit setting.
     */
    private const val LONG_TIME_FACTOR = 0.90

    /**
     * Preliminary instantaneous pickup.
     *
     * This is NOT a final manufacturer setting.
     */
    private const val INSTANTANEOUS_FACTOR = 8.0

    /**
     * Preliminary current-rating ratio used as a screening check.
     *
     * Final selectivity must be checked from actual TCC curves
     * and manufacturer discrimination tables.
     */
    private const val COORDINATED_RATIO = 1.60

    private const val WARNING_RATIO = 1.25

    private const val EPS = 1.0e-9

    /**
     * Compatibility fallback when no explicit StandardEngine
     * is supplied.
     */
    private val fallbackBreakerRatings =
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

    /**
     * Main protection coordination calculation.
     *
     * Existing upstream, cable and short-circuit studies are reused
     * whenever supplied by the facade.
     */
    fun calculate(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        cableSizingStudy: SldCableSizingStudy? = null,
        upstreamEngineering: SldUpstreamEngineering.Result? = null,
        engineeringContext: SldEngineeringContext? = null
    ): SldProtectionCoordinationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        /*
         * Validate engineering context inputs.
         *
         * Do not force a fully implemented standard here.
         * The implementation status is retained in the result and
         * report so the user can see whether the dataset is complete.
         */
        engineeringContext?.let { context ->

            context
                .copy(
                    requireImplementedStandard = false
                )
                .validate()
        }

        /*
         * Central topology authority.
         */
        val topology =
            SldTopologyEngine.build(network)

        /*
         * Reuse authoritative upstream result whenever available.
         *
         * This is important because the complete engineering facade
         * calculates upstream before protection.
         */
        val upstream =
            upstreamEngineering
                ?: SldUpstreamEngineering.calculate(
                    network = network,
                    engineeringContext = engineeringContext
                )

        /*
         * Resolve breaker ratings from the selected standard.
         */
        val breakerRatings =
            resolveBreakerRatings(
                engineeringContext = engineeringContext
            )

        val devices =
            linkedMapOf<String, SldProtectionDevice>()

        /*
         * ==========================================================
         * DEVICE CALCULATION
         * ==========================================================
         */
        network.nodes.forEach { node ->

            /*
             * Only actual protection/equipment protection points are
             * represented as protection devices.
             *
             * BUS nodes and LOAD nodes are not treated as breakers.
             *
             * LOAD protection is represented by its upstream BREAKER.
             */
            if (!isProtectionPoint(node.type)) {
                return@forEach
            }

            val downstreamCurrent =
                determineDownstreamCurrent(
                    node = node,
                    upstream = upstream,
                    cableSizingStudy = cableSizingStudy
                )

            /*
             * A protection device without a calculable current cannot
             * receive a false PASS status.
             */
            if (downstreamCurrent <= EPS) {

                devices[node.id] =
                    SldProtectionDevice(
                        nodeId =
                            node.id,
                        nodeName =
                            node.name,
                        deviceType =
                            protectionDeviceType(node.type),
                        downstreamCurrentA =
                            0.0,
                        recommendedRatingA =
                            0.0,
                        shortCircuitRatingKa =
                            0.0,
                        longTimePickupA =
                            0.0,
                        instantaneousPickupA =
                            0.0,
                        selectivityMarginA =
                            0.0,
                        status =
                            ProtectionStatus.WARNING,
                        notes =
                            listOf(
                                "No positive design current is available for this protection point."
                            )
                    )

                return@forEach
            }

            /*
             * Required nominal rating.
             */
            val requiredBreakerCurrent =
                downstreamCurrent *
                    BREAKER_MARGIN

            /*
             * If a breaker frame is explicitly entered and it is
             * sufficient, use it as the upper equipment constraint.
             *
             * breakerFrameA represents the equipment frame, not the
             * final trip setting.
             */
            val selectedByStandard =
                selectBreakerRating(
                    currentA =
                        requiredBreakerCurrent,
                    breakerRatings =
                        breakerRatings
                )

            val recommendedRating =
                if (
                    node.breakerFrameA > EPS &&
                    node.breakerFrameA >=
                    requiredBreakerCurrent
                ) {

                    /*
                     * Do not automatically claim that a frame rating is
                     * the trip rating. We only use it as an available
                     * equipment rating when it is explicitly provided.
                     */
                    node.breakerFrameA
                } else {

                    selectedByStandard
                }

            /*
             * Short-circuit result for this node.
             */
            val shortCircuitResult =
                shortCircuitStudy
                    ?.results
                    ?.get(node.id)

            val requiredShortCircuitKa =
                shortCircuitResult
                    ?.breakerRequiredKa
                    ?: 0.0

            val initialSymmetricalCurrentKa =
                shortCircuitResult
                    ?.initialSymmetricalCurrentKa
                    ?: 0.0

            /*
             * Actual device short-circuit rating entered by the user.
             *
             * A zero value means that the actual device rating has not
             * been verified/entered.
             */
            val actualDeviceShortCircuitKa =
                node.shortCircuitRatingKA
                    .coerceAtLeast(0.0)

            val longTimePickup =
                recommendedRating *
                    LONG_TIME_FACTOR

            val instantaneousPickup =
                recommendedRating *
                    INSTANTANEOUS_FACTOR

            val notes =
                mutableListOf<String>()

            var status =
                ProtectionStatus.PASS

            /*
             * --------------------------------------------------------
             * CURRENT CHECK
             * --------------------------------------------------------
             */
            if (
                recommendedRating + EPS <
                downstreamCurrent
            ) {

                status =
                    ProtectionStatus.FAIL

                notes.add(
                    "Selected breaker rating is below the calculated design current."
                )
            } else {

                notes.add(
                    "Calculated design current: " +
                        formatA(downstreamCurrent) +
                        " A."
                )

                notes.add(
                    "Selected nominal rating: " +
                        formatA(recommendedRating) +
                        " A."
                )
            }

            /*
             * --------------------------------------------------------
             * CABLE / BREAKER CHECK
             * --------------------------------------------------------
             *
             * Only CABLE connections are considered.
             *
             * BUSBAR has no cable ampacity and therefore must never
             * cause a cable protection failure.
             */
            val incomingConnections =
                topology.connections.filter {
                    it.toNodeId == node.id &&
                        it.connectionType ==
                        SldConnectionType.CABLE
                }

            incomingConnections.forEach { connection ->

                val cableResult =
                    cableSizingStudy
                        ?.results
                        ?.get(connection.id)

                if (cableResult == null) {

                    /*
                     * Missing cable study is a verification warning,
                     * not a false PASS.
                     */
                    if (cableSizingStudy != null) {

                        status =
                            if (
                                status ==
                                ProtectionStatus.PASS
                            ) {
                                ProtectionStatus.WARNING
                            } else {
                                status
                            }

                        notes.add(
                            "Cable sizing result is not available for feeder " +
                                connection.id +
                                "."
                        )
                    }

                    return@forEach
                }

                val cableCapacity =
                    cableResult
                        .recommendedCurrentCapacityA
                        .coerceAtLeast(0.0)

                if (cableCapacity <= EPS) {

                    status =
                        ProtectionStatus.FAIL

                    notes.add(
                        "Cable current capacity is not verified."
                    )

                    return@forEach
                }

                if (
                    recommendedRating >
                    cableCapacity + EPS
                ) {

                    status =
                        ProtectionStatus.FAIL

                    notes.add(
                        "Breaker rating " +
                            formatA(recommendedRating) +
                            " A exceeds the selected cable current capacity " +
                            formatA(cableCapacity) +
                            " A."
                    )
                } else {

                    notes.add(
                        "Cable current capacity: " +
                            formatA(cableCapacity) +
                            " A."
                    )
                }
            }

            /*
             * --------------------------------------------------------
             * SHORT-CIRCUIT CHECK
             * --------------------------------------------------------
             */
            if (
                requiredShortCircuitKa > EPS
            ) {

                notes.add(
                    "Required preliminary short-circuit breaking capacity: " +
                        formatKa(requiredShortCircuitKa) +
                        " kA."
                )

                if (
                    actualDeviceShortCircuitKa <= EPS
                ) {

                    /*
                     * No actual device Icu/Ics has been supplied.
                     * This cannot be accepted as verified.
                     */
                    if (
                        status ==
                        ProtectionStatus.PASS
                    ) {
                        status =
                            ProtectionStatus.WARNING
                    }

                    notes.add(
                        "Actual breaker short-circuit rating (Icu/Ics) is not entered; final verification is required."
                    )

                } else if (
                    actualDeviceShortCircuitKa + EPS <
                    requiredShortCircuitKa
                ) {

                    status =
                        ProtectionStatus.FAIL

                    notes.add(
                        "Actual breaker short-circuit rating " +
                            formatKa(actualDeviceShortCircuitKa) +
                            " kA is below the required " +
                            formatKa(requiredShortCircuitKa) +
                            " kA."
                    )

                } else {

                    notes.add(
                        "Actual breaker short-circuit rating: " +
                            formatKa(actualDeviceShortCircuitKa) +
                            " kA."
                    )
                }

                if (
                    initialSymmetricalCurrentKa > EPS
                ) {

                    notes.add(
                        "Initial symmetrical fault current: " +
                            formatKa(initialSymmetricalCurrentKa) +
                            " kA."
                    )
                }

            } else {

                if (
                    status ==
                    ProtectionStatus.PASS
                ) {
                    status =
                        ProtectionStatus.WARNING
                }

                notes.add(
                    "Short-circuit study is not available or contains insufficient data."
                )
            }

            /*
             * --------------------------------------------------------
             * NODE-SPECIFIC INFORMATION
             * --------------------------------------------------------
             */
            if (
                node.breakerFrameA > EPS
            ) {

                notes.add(
                    "Breaker frame rating entered: " +
                        formatA(node.breakerFrameA) +
                        " A."
                )
            }

            if (
                node.poles > 0
            ) {

                notes.add(
                    "Poles: ${node.poles}."
                )
            }

            /*
             * --------------------------------------------------------
             * STANDARD INFORMATION
             * --------------------------------------------------------
             */
            if (engineeringContext != null) {

                notes.add(
                    "Standard: " +
                        engineeringContext.codeName +
                        "."
                )

                notes.add(
                    "Code revision: " +
                        engineeringContext.codeRevision +
                        "."
                )

                notes.add(
                    "Breaker ratings supplied by the selected StandardEngine."
                )

                notes.add(
                    "Standard implementation status: " +
                        engineeringContext.standardImplementationStatus +
                        "."
                )

            } else {

                notes.add(
                    "Backward-compatible protection mode: no explicit engineering context supplied."
                )
            }

            /*
             * --------------------------------------------------------
             * DEVICE RESULT
             * --------------------------------------------------------
             */
            devices[node.id] =
                SldProtectionDevice(
                    nodeId =
                        node.id,
                    nodeName =
                        node.name,
                    deviceType =
                        protectionDeviceType(node.type),
                    downstreamCurrentA =
                        downstreamCurrent,
                    recommendedRatingA =
                        recommendedRating,
                    shortCircuitRatingKa =
                        requiredShortCircuitKa,
                    longTimePickupA =
                        longTimePickup,
                    instantaneousPickupA =
                        instantaneousPickup,
                    selectivityMarginA =
                        0.0,
                    status =
                        status,
                    notes =
                        notes.distinct()
                )
        }

        /*
         * ==========================================================
         * PRELIMINARY SELECTIVITY SCREENING
         * ==========================================================
         *
         * Only CABLE feeders are considered.
         *
         * BUSBAR connections are internal panel connections and do
         * not represent separate feeder protection coordination.
         */
        var coordinatedPairs =
            0

        var warningPairs =
            0

        var failedPairs =
            0

        topology.connections
            .filter {
                it.connectionType ==
                    SldConnectionType.CABLE
            }
            .forEach { connection ->

                val upstreamDevice =
                    devices[
                        connection.fromNodeId
                    ]

                val downstreamDevice =
                    devices[
                        connection.toNodeId
                    ]

                /*
                 * A pair is meaningful only when both endpoints are
                 * actual protection points.
                 */
                if (
                    upstreamDevice == null ||
                    downstreamDevice == null
                ) {
                    return@forEach
                }

                val upstreamRating =
                    upstreamDevice
                        .recommendedRatingA

                val downstreamRating =
                    downstreamDevice
                        .recommendedRatingA

                if (
                    upstreamRating <= EPS ||
                    downstreamRating <= EPS
                ) {
                    return@forEach
                }

                /*
                 * A downstream breaker must normally be smaller than
                 * the upstream breaker. The ratio is only a preliminary
                 * screening indicator.
                 */
                val ratio =
                    upstreamRating /
                        downstreamRating

                val selectivityMargin =
                    upstreamRating -
                        downstreamRating

                val pairStatus =
                    when {

                        ratio >=
                            COORDINATED_RATIO -> {
                            coordinatedPairs++
                            ProtectionStatus.PASS
                        }

                        ratio >=
                            WARNING_RATIO -> {
                            warningPairs++
                            ProtectionStatus.WARNING
                        }

                        else -> {
                            failedPairs++
                            ProtectionStatus.FAIL
                        }
                    }

                /*
                 * Store the maximum observed margin at the upstream
                 * protection point.
                 */
                val existingUpstream =
                    devices[
                        connection.fromNodeId
                    ]

                if (existingUpstream != null) {

                    val pairNote =
                        "Downstream protection " +
                            downstreamDevice.nodeName +
                            " (" +
                            formatA(downstreamRating) +
                            " A); preliminary selectivity ratio " +
                            formatRatio(ratio) +
                            ", margin " +
                            formatA(selectivityMargin) +
                            " A."

                    val currentNotes =
                        existingUpstream
                            .notes
                            .toMutableList()

                    currentNotes.add(
                        pairNote
                    )

                    /*
                     * Pair status must be reflected in the upstream
                     * device without hiding a previously existing FAIL.
                     */
                    val resultingStatus =
                        mergeStatus(
                            existing =
                                existingUpstream.status,
                            pair =
                                pairStatus
                        )

                    devices[
                        connection.fromNodeId
                    ] =
                        existingUpstream.copy(
                            selectivityMarginA =
                                maxOf(
                                    existingUpstream
                                        .selectivityMarginA,
                                    selectivityMargin
                                ),
                            status =
                                resultingStatus,
                            notes =
                                currentNotes.distinct()
                        )
                }
            }

        /*
         * ==========================================================
         * STUDY NOTES
         * ==========================================================
         */
        val notes =
            mutableListOf<String>()

        notes.add(
            "Protection coordination is a preliminary engineering study."
        )

        notes.add(
            "Electrical feeder direction is determined exclusively by SldTopologyEngine."
        )

        notes.add(
            "Upstream engineering results are reused when supplied by SldEngineeringFacade."
        )

        notes.add(
            "BUSBAR connections are excluded from cable protection and feeder selectivity checks."
        )

        if (engineeringContext != null) {

            notes.add(
                "Active standard: " +
                    engineeringContext.codeName +
                    " (" +
                    engineeringContext.codeRevision +
                    ")."
            )

            notes.add(
                "Breaker ratings are obtained from the selected StandardEngine."
            )

            notes.add(
                "Standard implementation status: " +
                    engineeringContext.standardImplementationStatus +
                    "."
            )

        } else {

            notes.add(
                "Fallback breaker rating sequence is used because no engineering context was supplied."
            )
        }

        notes.add(
            "Long-time pickup is preliminarily represented as 90% of nominal breaker rating."
        )

        notes.add(
            "Instantaneous pickup is preliminarily represented as 8 × nominal breaker rating."
        )

        notes.add(
            "These pickup values are engineering placeholders and must be replaced by actual trip-unit settings."
        )

        notes.add(
            "Final selectivity requires manufacturer time-current curves, trip-unit settings and discrimination tables."
        )

        if (coordinatedPairs > 0) {

            notes.add(
                "$coordinatedPairs feeder pair(s) passed the preliminary rating-ratio screening."
            )
        }

        if (warningPairs > 0) {

            notes.add(
                "$warningPairs feeder pair(s) require detailed time-current verification."
            )
        }

        if (failedPairs > 0) {

            notes.add(
                "$failedPairs feeder pair(s) did not satisfy the preliminary selectivity ratio screening."
            )
        }

        return SldProtectionCoordinationResult(
            devices =
                devices,
            coordinatedPairs =
                coordinatedPairs,
            warningPairs =
                warningPairs,
            failedPairs =
                failedPairs,
            notes =
                notes.distinct()
        )
    }

    /**
     * ============================================================
     * BREAKER RATING SOURCE
     * ============================================================
     */
    private fun resolveBreakerRatings(
        engineeringContext: SldEngineeringContext?
    ): List<Double> {

        val standardRatings =
            engineeringContext
                ?.standardEngine
                ?.standardBreakerRatings()
                ?.filter {
                    it > EPS
                }
                ?.distinct()
                ?.sorted()
                ?.takeIf {
                    it.isNotEmpty()
                }

        return standardRatings
            ?: fallbackBreakerRatings
    }

    /**
     * ============================================================
     * CURRENT DETERMINATION
     * ============================================================
     *
     * Priority:
     *
     * 1. Authoritative upstream result
     * 2. Cable sizing result
     * 3. Local load fallback
     * 4. Rated kVA fallback
     */
    private fun determineDownstreamCurrent(
        node: SldNode,
        upstream: SldUpstreamEngineering.Result,
        cableSizingStudy: SldCableSizingStudy?
    ): Double {

        val upstreamCurrent =
            upstream.nodes
                .firstOrNull {
                    it.nodeId == node.id
                }
                ?.currentA
                ?: 0.0

        if (
            upstreamCurrent > EPS
        ) {

            return upstreamCurrent
        }

        /*
         * Cable sizing fallback is used only when upstream did not
         * provide a positive current.
         */
        cableSizingStudy
            ?.results
            ?.values
            ?.firstOrNull {
                it.toNodeId == node.id
            }
            ?.designCurrentA
            ?.let { current ->

                if (
                    current > EPS
                ) {

                    return current
                }
            }

        /*
         * Local load fallback.
         */
        if (
            node.loadKw > EPS &&
            node.powerFactor > EPS &&
            node.voltage > EPS
        ) {

            return when (node.phaseSystem) {

                SldPhaseSystem.THREE_PHASE ->

                    (
                        node.loadKw *
                            1000.0
                        ) / (
                        SQRT_3 *
                            node.voltage *
                            node.powerFactor
                        )

                SldPhaseSystem.SINGLE_PHASE ->

                    (
                        node.loadKw *
                            1000.0
                        ) / (
                        node.voltage *
                            node.powerFactor
                        )

                SldPhaseSystem.DC ->

                    (
                        node.loadKw *
                            1000.0
                        ) / node.voltage
            }
        }

        /*
         * Rated kVA fallback.
         */
        if (
            node.ratedKva > EPS &&
            node.voltage > EPS
        ) {

            return when (node.phaseSystem) {

                SldPhaseSystem.THREE_PHASE ->

                    (
                        node.ratedKva *
                            1000.0
                        ) / (
                        SQRT_3 *
                            node.voltage
                        )

                SldPhaseSystem.SINGLE_PHASE,
                SldPhaseSystem.DC ->

                    (
                        node.ratedKva *
                            1000.0
                        ) / node.voltage
            }
        }

        /*
         * Explicit rated current is the final fallback.
         */
        if (
            node.ratedCurrentA > EPS
        ) {

            return node.ratedCurrentA
        }

        return 0.0
    }

    /**
     * ============================================================
     * PROTECTION POINT CLASSIFICATION
     * ============================================================
     *
     * BREAKER:
     * Actual feeder protection point.
     *
     * PANEL:
     * Panel incomer protection point.
     *
     * SOURCE:
     * Main source protection point.
     *
     * TRANSFORMER:
     * Transformer protection point.
     *
     * GENERATOR:
     * Generator protection point.
     *
     * BUS and LOAD are not independent protection devices.
     */
    private fun isProtectionPoint(
        type: SldNodeType
    ): Boolean =
        when (type) {

            SldNodeType.SOURCE,
            SldNodeType.TRANSFORMER,
            SldNodeType.GENERATOR,
            SldNodeType.PANEL,
            SldNodeType.BREAKER -> true

            SldNodeType.BUS,
            SldNodeType.LOAD -> false
        }

    /**
     * ============================================================
     * BREAKER SELECTION
     * ============================================================
     */
    private fun selectBreakerRating(
        currentA: Double,
        breakerRatings: List<Double>
    ): Double {

        if (
            currentA <= EPS
        ) {
            return 0.0
        }

        return breakerRatings
            .firstOrNull {
                it + EPS >= currentA
            }
            ?: breakerRatings.lastOrNull()
            ?: 0.0
    }

    /**
     * ============================================================
     * DEVICE LABEL
     * ============================================================
     */
    private fun protectionDeviceType(
        type: SldNodeType
    ): String =
        when (type) {

            SldNodeType.BREAKER ->
                "ACB/MCCB Feeder"

            SldNodeType.PANEL ->
                "Panel Incomer"

            SldNodeType.TRANSFORMER ->
                "Transformer Protection"

            SldNodeType.GENERATOR ->
                "Generator Protection"

            SldNodeType.SOURCE ->
                "Main Incomer"

            SldNodeType.BUS ->
                "Bus"

            SldNodeType.LOAD ->
                "Load"
        }

    /**
     * ============================================================
     * STATUS MERGE
     * ============================================================
     *
     * FAIL always dominates WARNING and PASS.
     * WARNING dominates PASS.
     */
    private fun mergeStatus(
        existing: ProtectionStatus,
        pair: ProtectionStatus
    ): ProtectionStatus {

        return when {

            existing == ProtectionStatus.FAIL ||
                pair == ProtectionStatus.FAIL ->

                ProtectionStatus.FAIL

            existing == ProtectionStatus.WARNING ||
                pair == ProtectionStatus.WARNING ->

                ProtectionStatus.WARNING

            else ->
                ProtectionStatus.PASS
        }
    }

    /**
     * ============================================================
     * FORMATTING
     * ============================================================
     */
    private fun formatA(
        value: Double
    ): String =
        if (
            value >= 100.0
        ) {
            "%.0f".format(value)
        } else {
            "%.1f".format(value)
        }

    private fun formatKa(
        value: Double
    ): String =
        "%.2f".format(value)

    private fun formatRatio(
        value: Double
    ): String =
        "%.2f".format(value)
}
