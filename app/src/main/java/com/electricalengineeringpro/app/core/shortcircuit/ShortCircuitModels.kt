package com.electricalengineeringpro.app.core.shortcircuit

enum class FaultType {
    THREE_PHASE,
    LINE_TO_LINE,
    LINE_TO_GROUND,
    DOUBLE_LINE_TO_GROUND
}

enum class ShortCircuitStandard {
    IEC_60909_0_2026
}

enum class CalculationStatus {
    VERIFIED,
    DATA_INCOMPLETE,
    UNSUPPORTED,
    INVALID_INPUT
}

data class ComplexImpedance(
    val rOhm: Double,
    val xOhm: Double
) {
    init {
        require(rOhm >= 0.0) {
            "Resistance cannot be negative."
        }

        require(xOhm >= 0.0) {
            "Reactance cannot be negative."
        }
    }

    val magnitudeOhm: Double
        get() = kotlin.math.hypot(rOhm, xOhm)

    operator fun plus(
        other: ComplexImpedance
    ): ComplexImpedance {
        return ComplexImpedance(
            rOhm = rOhm + other.rOhm,
            xOhm = xOhm + other.xOhm
        )
    }
}

data class SourceCatalogData(
    val manufacturer: String,
    val model: String,
    val ratedVoltageV: Double,
    val shortCircuitMva: Double,
    val xrRatio: Double? = null,
    val positiveSequence: ComplexImpedance? = null,
    val zeroSequence: ComplexImpedance? = null,
    val catalogReference: String
)

data class TransformerCatalogData(
    val manufacturer: String,
    val model: String,
    val ratedPowerKva: Double,
    val primaryVoltageV: Double,
    val secondaryVoltageV: Double,
    val ukPercent: Double,
    val xrRatio: Double? = null,
    val positiveSequence: ComplexImpedance? = null,
    val zeroSequence: ComplexImpedance? = null,
    val vectorGroup: String? = null,
    val catalogReference: String
)

data class CableCatalogData(
    val manufacturer: String,
    val model: String,
    val conductorMaterial: String,
    val conductorSizeMm2: Double,
    val cores: Int,
    val ratedVoltageV: Double,
    val resistanceOhmPerKm: Double,
    val reactanceOhmPerKm: Double,
    val zeroSequenceResistanceOhmPerKm: Double? = null,
    val zeroSequenceReactanceOhmPerKm: Double? = null,
    val catalogReference: String
)

data class ShortCircuitSegment(
    val cable: CableCatalogData,
    val lengthM: Double,
    val parallelRuns: Int = 1
)

data class CatalogShortCircuitInput(
    val standard: ShortCircuitStandard =
        ShortCircuitStandard.IEC_60909_0_2026,

    val faultType: FaultType =
        FaultType.THREE_PHASE,

    val voltageV: Double,

    val voltageFactorC: Double = 1.0,

    val source: SourceCatalogData,

    val transformer: TransformerCatalogData? = null,

    val cableSegments: List<ShortCircuitSegment> =
        emptyList(),

    val includeMotorContributionA: Double = 0.0
)

data class ShortCircuitResult(
    val status: CalculationStatus,

    val standard: ShortCircuitStandard,

    val faultType: FaultType,

    val initialSymmetricalCurrentA: Double? = null,

    val initialSymmetricalCurrentKA: Double? = null,

    val peakCurrentKA: Double? = null,

    val thermalEquivalentCurrentKA: Double? = null,

    val faultMva: Double? = null,

    val positiveSequenceImpedanceOhm:
        ComplexImpedance? = null,

    val zeroSequenceImpedanceOhm:
        ComplexImpedance? = null,

    val warnings: List<String> = emptyList(),

    val errors: List<String> = emptyList(),

    val calculationTrace: List<String> =
        emptyList(),

    val sourceReferences: List<String> =
        emptyList()
)
