package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD DATA MODEL
 * ================================================================
 *
 * Single source of truth for the interactive Single Line Diagram.
 *
 * The model intentionally contains:
 *
 * - Engineering identity
 * - Electrical design data
 * - Graphic position/orientation
 * - Equipment rating
 * - Manufacturer/catalog information
 * - Connection engineering data
 * - BUSBAR information
 *
 * All newly introduced properties have safe defaults so existing
 * project data and existing constructors remain compatible.
 *
 * ================================================================
 */

enum class SldNodeType {
    SOURCE,
    TRANSFORMER,
    GENERATOR,
    BUS,
    PANEL,
    BREAKER,
    LOAD
}

/**
 * Graphic orientation used by the SLD renderer.
 *
 * The drawing layer decides the actual symbol geometry.
 * The model stores the engineering/editorial orientation only.
 */
enum class SldOrientation {
    AUTO,
    LEFT,
    RIGHT,
    UP,
    DOWN
}

/**
 * Electrical system type.
 */
enum class SldPhaseSystem {
    THREE_PHASE,
    SINGLE_PHASE,
    DC
}

/**
 * Connection classification.
 *
 * BUSBAR:
 * Internal panel/busbar connection.
 *
 * CABLE:
 * External feeder.
 */
enum class SldConnectionType {
    BUSBAR,
    CABLE
}

/**
 * ================================================================
 * SLD NODE
 * ================================================================
 */
data class SldNode(

    /*
     * Identity
     */
    val id: String,
    val name: String,
    val type: SldNodeType,

    /*
     * Graphic position.
     *
     * Coordinates are stored in SLD logical canvas space.
     */
    val x: Float,
    val y: Float,

    /*
     * Basic electrical system
     */
    val voltage: Double = 400.0,

    val phaseSystem: SldPhaseSystem =
        SldPhaseSystem.THREE_PHASE,

    val frequencyHz: Double = 50.0,

    /*
     * Load data.
     *
     * Only LOAD nodes represent actual local load in the
     * authoritative upstream engine.
     */
    val loadKw: Double = 0.0,

    val powerFactor: Double = 0.90,

    val demandFactor: Double = 1.0,

    /*
     * Equipment rating
     */
    val ratedKva: Double = 0.0,

    val ratedCurrentA: Double = 0.0,

    /*
     * Transformer data
     */
    val transformerPercentZ: Double = 0.0,

    val transformerPrimaryVoltage: Double = 0.0,

    val transformerSecondaryVoltage: Double = 0.0,

    /*
     * Generator data
     */
    val generatorXdSubtransient: Double = 0.0,

    val generatorRatedKva: Double = 0.0,

    /*
     * Source short-circuit data
     */
    val sourceShortCircuitMva: Double = 0.0,

    val sourceShortCircuitKA: Double = 0.0,

    /*
     * Protection / equipment data
     */
    val poles: Int = 3,

    val shortCircuitRatingKA: Double = 0.0,

    val breakerFrameA: Double = 0.0,

    /*
     * SLD graphical orientation.
     *
     * AUTO allows the drawing/layout engine to determine the
     * appropriate orientation from topology.
     */
    val orientation: SldOrientation =
        SldOrientation.AUTO,

    /*
     * Engineering identification.
     */
    val tag: String = "",

    val description: String = "",

    /*
     * Manufacturer/catalog information.
     *
     * These fields are descriptive only at this model level.
     * Verified catalog selection remains the responsibility of
     * the catalog/engineering layer.
     */
    val manufacturer: String = "",

    val model: String = "",

    val catalogReference: String = "",

    /*
     * Optional engineering notes.
     */
    val notes: String = ""
)

/**
 * ================================================================
 * SLD CONNECTION
 * ================================================================
 */
data class SldConnection(

    /*
     * Identity
     */
    val id: String,

    /*
     * IMPORTANT:
     *
     * fromNodeId -> toNodeId is the actual engineering direction.
     *
     * The topology engine and upstream calculation engine preserve
     * this direction.
     */
    val fromNodeId: String,

    val toNodeId: String,

    /*
     * Connection classification.
     */
    val connectionType: SldConnectionType =
        SldConnectionType.CABLE,

    /*
     * Cable engineering data.
     *
     * These values are ignored for BUSBAR connections.
     */
    val lengthMeters: Double = 0.0,

    val resistanceOhmPerKm: Double = 0.0,

    val reactanceOhmPerKm: Double = 0.0,

    val cableSizeMm2: Double = 0.0,

    val parallelRuns: Int = 1,

    val voltageDropPercent: Double = 0.0,

    val currentCapacityA: Double = 0.0,

    /*
     * Cable construction.
     */
    val conductorMaterial: String = "Copper",

    val insulationType: String = "PVC",

    val installationMethodCode: String = "B1",

    /*
     * Number of loaded conductors.
     *
     * Default keeps compatibility with existing projects.
     */
    val loadedConductors: Int = 3,

    /*
     * Optional cable designation.
     */
    val cableDesignation: String = "",

    val cableManufacturer: String = "",

    val cableModel: String = "",

    /*
     * Protection / feeder identification.
     */
    val breakerTag: String = "",

    val feederTag: String = "",

    /*
     * BUSBAR engineering data.
     *
     * BUSBAR connections do not use cable length/type.
     */
    val busbarMaterial: String = "Copper",

    val busbarRatedCurrentA: Double = 0.0,

    val busbarShortCircuitKA: Double = 0.0,

    /*
     * Graphic routing information.
     *
     * AUTO means the SLD routing engine chooses the path.
     */
    val routeAuto: Boolean = true,

    /*
     * Optional manual routing points.
     *
     * Kept empty by default so the existing auto-routing remains
     * fully compatible.
     */
    val routePoints: List<SldRoutePoint> = emptyList(),

    /*
     * Engineering / editorial note.
     */
    val notes: String = ""
)

/**
 * A logical routing point for professional SLD feeder routing.
 *
 * Coordinates use the same logical canvas coordinate system as
 * SldNode.x / SldNode.y.
 */
data class SldRoutePoint(
    val x: Float,
    val y: Float
)

/**
 * ================================================================
 * SLD NETWORK
 * ================================================================
 *
 * The complete diagram state.
 */
data class SldNetwork(
    val nodes: List<SldNode> = emptyList(),
    val connections: List<SldConnection> = emptyList()
)

/**
 * ================================================================
 * LEGACY-COMPATIBLE UPSTREAM RESULT
 * ================================================================
 *
 * Kept for older UI/report code.
 *
 * New engineering code should use:
 *
 *     SldUpstreamEngineering.Result
 *
 * The authoritative calculation remains in:
 *
 *     SldUpstreamEngineering
 * ================================================================
 */
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

/**
 * ================================================================
 * LEGACY-COMPATIBLE CALCULATION RESULT
 * ================================================================
 *
 * Retained so older report/UI components continue to compile.
 *
 * The authoritative calculation is performed by
 * SldUpstreamEngineering and adapted by
 * SldUpstreamCalculationEngine.
 * ================================================================
 */
data class SldCalculationResult(

    val nodeResults: Map<String, UpstreamResult> =
        emptyMap(),

    val totalConnectedLoadKw: Double = 0.0,

    val totalDemandLoadKw: Double = 0.0,

    val totalRequiredKva: Double = 0.0,

    val mainCurrentA: Double = 0.0,

    val mainBreakerA: Double = 0.0,

    val requiredTransformerKva: Double = 0.0,

    val totalVoltageDropPercent: Double = 0.0,

    val notes: List<String> = emptyList()
)
