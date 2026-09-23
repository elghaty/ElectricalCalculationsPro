package com.electrical.calculationspro.data

data class EngineeringValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

enum class CalculationStatus {
    SUCCESS,
    WARNING,
    INVALID_INPUT,
    NOT_IMPLEMENTED
}

data class CalculationMetadata(
    val status: CalculationStatus,
    val standard: Standard,
    val codeName: String,
    val codeRevision: String,
    val notes: List<String> = emptyList()
)

data class ShortCircuitResult(
    val sourceShortCircuitCurrentKA: Double = 0.0,
    val cableResistanceOhm: Double = 0.0,
    val cableReactanceOhm: Double = 0.0,
    val totalResistanceOhm: Double = 0.0,
    val totalReactanceOhm: Double = 0.0,
    val totalImpedanceOhm: Double = 0.0,
    val shortCircuitCurrentKA: Double = 0.0,
    val shortCircuitMva: Double = 0.0,
    val xrRatio: Double = 0.0,
    val valid: Boolean = false,
    val notes: List<String> = emptyList()
)

data class PowerCalculationResult(
    val activePowerKw: Double = 0.0,
    val apparentPowerKva: Double = 0.0,
    val reactivePowerKvar: Double = 0.0,
    val powerFactor: Double = 0.0,
    val valid: Boolean = false,
    val notes: List<String> = emptyList()
)

data class LoadCalculationResult(
    val connectedLoadKw: Double = 0.0,
    val demandFactor: Double = 1.0,
    val diversityFactor: Double = 1.0,
    val demandLoadKw: Double = 0.0,
    val diversifiedLoadKw: Double = 0.0,
    val finalDesignLoadKw: Double = 0.0,
    val designCurrentA: Double = 0.0,
    val valid: Boolean = false,
    val notes: List<String> = emptyList()
)

data class VoltageDropResult(
    val voltageDropVolts: Double = 0.0,
    val voltageDropPercent: Double = 0.0,
    val receivingEndVoltageV: Double = 0.0,
    val withinLimit: Boolean = false,
    val valid: Boolean = false,
    val notes: List<String> = emptyList()
)

data class BreakerSelectionResult(
    val designCurrentA: Double = 0.0,
    val cableAmpacityA: Double = 0.0,
    val selectedRatingA: Double = 0.0,
    val breakingCapacityKA: Double = 0.0,
    val coordinationValid: Boolean = false,
    val breakingCapacityValid: Boolean = false,
    val valid: Boolean = false,
    val notes: List<String> = emptyList()
)

data class TransformerSizingResult(
    val loadKw: Double = 0.0,
    val powerFactor: Double = 1.0,
    val growthFactor: Double = 1.0,
    val requiredKva: Double = 0.0,
    val selectedKva: Double = 0.0,
    val fullLoadCurrentA: Double = 0.0,
    val impedancePercent: Double = 0.0,
    val shortCircuitCurrentKA: Double = 0.0,
    val valid: Boolean = false,
    val notes: List<String> = emptyList()
)

data class ConductorSizingStudy(
    val input: ConductorSizingInput,
    val result: ConductorSizingResult,
    val standard: Standard,
    val codeName: String,
    val codeRevision: String,
    val status: CalculationStatus,
    val warnings: List<String> = emptyList(),
    val notes: List<String> = emptyList()
)

data class EngineeringCalculationPackage(
    val load: LoadCalculationResult? = null,
    val power: PowerCalculationResult? = null,
    val voltageDrop: VoltageDropResult? = null,
    val shortCircuit: ShortCircuitResult? = null,
    val conductorSizing: ConductorSizingStudy? = null,
    val breaker: BreakerSelectionResult? = null,
    val transformer: TransformerSizingResult? = null,
    val metadata: CalculationMetadata? = null
)

data class CalculationHistoryItem(
    val id: String,
    val calculationType: String,
    val timestampMillis: Long,
    val standard: Standard,
    val summary: String,
    val result: EngineeringCalculationPackage,
    val projectId: String? = null
)
