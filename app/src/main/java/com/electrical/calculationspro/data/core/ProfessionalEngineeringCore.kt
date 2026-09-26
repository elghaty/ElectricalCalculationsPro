package com.electrical.calculationspro.data.core

import com.electrical.calculationspro.data.BreakerSelectionResult
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ShortCircuitResult
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.TransformerSizingResult
import com.electrical.calculationspro.data.calculators.BreakerSelectionCalculator
import com.electrical.calculationspro.data.calculators.ConductorSizingCalculator
import com.electrical.calculationspro.data.calculators.LoadCalculator
import com.electrical.calculationspro.data.calculators.ShortCircuitCalculator
import com.electrical.calculationspro.data.calculators.TransformerSizingCalculator
import com.electrical.calculationspro.data.calculators.VoltageDropCalculator
import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult
import com.electrical.calculationspro.data.pumps.PumpCalculator

/**
 * ================================================================
 * PROFESSIONAL ENGINEERING CORE
 * ================================================================
 *
 * Single application-level engineering facade.
 *
 * Android UI
 *      ↓
 * ProfessionalEngineeringCore
 *      ↓
 * Dedicated Calculators / Engineering Engines
 *      ↓
 * Standards / Catalogs / Engineering Models
 *
 * IMPORTANT:
 *
 * This facade contains NO engineering formulas.
 *
 * All engineering calculations remain inside their dedicated
 * calculator or engineering-engine classes.
 *
 * The purpose of this class is to provide one stable public
 * entry point for the application.
 *
 * Future calculation modules should be exposed here instead of
 * allowing the UI to call calculation implementations directly.
 * ================================================================
 */
object ProfessionalEngineeringCore {

    // ============================================================
    // LOAD
    // ============================================================

    fun calculateDesignCurrentFromKw(
        loadKw: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double =
        LoadCalculator.designCurrentFromKw(
            loadKw = loadKw,
            voltage = voltage,
            powerFactor = powerFactor,
            currentType = currentType
        )

    // ============================================================
    // VOLTAGE DROP
    // ============================================================

    fun calculateVoltageDrop(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> =
        VoltageDropCalculator.calculate(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        )

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

    fun calculateShortCircuitCurrent(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 50.0
    ): ShortCircuitResult =
        ShortCircuitCalculator.calculate(
            voltage = voltage,
            length = length,
            sectionMm2 = sectionMm2,
            material = material,
            currentType = currentType,
            sourceIkKA = sourceIkKA
        )

    // ============================================================
    // BREAKER
    // ============================================================

    fun calculateBreakerSelection(
        designCurrentA: Double,
        cableAmpacityA: Double,
        prospectiveFaultCurrentKA: Double = 0.0,
        breakerBreakingCapacityKA: Double = 0.0,
        standard: Standard = Standard.IEC
    ): BreakerSelectionResult =
        BreakerSelectionCalculator.calculate(
            designCurrentA = designCurrentA,
            cableAmpacityA = cableAmpacityA,
            prospectiveFaultCurrentKA = prospectiveFaultCurrentKA,
            breakerBreakingCapacityKA = breakerBreakingCapacityKA,
            standard = standard
        )

    // ============================================================
    // CONDUCTOR
    // ============================================================

    fun sizeConductor(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult =
        ConductorSizingCalculator.size(
            input = input,
            standard = standard,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor
        )

    fun evaluateSelectedConductor(
        input: ConductorSizingInput,
        selectedSection: Double,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult =
        ConductorSizingCalculator.evaluateSelectedSection(
            input = input,
            selectedSection = selectedSection,
            standard = standard,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor
        )

    // ============================================================
    // TRANSFORMER
    // ============================================================

    fun calculateTransformerSizing(
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double = 1.0,
        voltage: Double,
        phases: Int = 3,
        impedancePercent: Double = 6.0
    ): TransformerSizingResult =
        TransformerSizingCalculator.calculate(
            loadKw = loadKw,
            powerFactor = powerFactor,
            growthFactor = growthFactor,
            voltage = voltage,
            phases = phases,
            impedancePercent = impedancePercent
        )

    // ============================================================
    // PUMP
    // ============================================================

    fun calculatePump(
        input: PumpCalculationInput
    ): PumpCalculationResult =
        PumpCalculator.calculate(
            input = input
        )
}
