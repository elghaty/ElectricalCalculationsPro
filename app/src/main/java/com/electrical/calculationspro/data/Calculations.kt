package com.electrical.calculationspro.data

import com.electrical.calculationspro.data.calculators.BreakerSelectionCalculator
import com.electrical.calculationspro.data.calculators.ConductorSizingCalculator
import com.electrical.calculationspro.data.calculators.LoadCalculator
import com.electrical.calculationspro.data.calculators.PowerCalculator
import com.electrical.calculationspro.data.calculators.ShortCircuitCalculator
import com.electrical.calculationspro.data.calculators.TransformerSizingCalculator
import com.electrical.calculationspro.data.calculators.VoltageDropCalculator

/**
 * Professional Engineering Core
 *
 * This object is the single public calculation facade used by the UI.
 *
 * Architecture:
 *
 * UI
 *  ↓
 * ElectricalCalculations
 *  ↓
 * ┌──────────────────────────────────────┐
 * │ LoadCalculator                       │
 * │ PowerCalculator                      │
 * │ VoltageDropCalculator                │
 * │ ShortCircuitCalculator               │
 * │ ConductorSizingCalculator            │
 * │ BreakerSelectionCalculator           │
 * │ TransformerSizingCalculator          │
 * └──────────────────────────────────────┘
 *
 * The UI must not call individual calculation engines directly.
 */
object ElectricalCalculations {

    // ============================================================
    // LOAD CALCULATIONS
    // ============================================================

    /**
     * Calculate design current from load.
     *
     * loadWatts:
     *     Active electrical load in watts.
     *
     * voltage:
     *     System voltage.
     *
     * powerFactor:
     *     Power factor, 0 < PF <= 1.
     */
    fun calculateDesignCurrent(
        loadWatts: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double {

        return LoadCalculator.designCurrent(
            loadWatts = loadWatts,
            voltage = voltage,
            powerFactor = powerFactor,
            currentType = currentType
        )
    }

    /**
     * Apply demand and diversity factors.
     */
    fun applyDemandAndDiversity(
        ib: Double,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): Double {

        return LoadCalculator.applyDemandAndDiversity(
            ib = ib,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor
        )
    }

    /**
     * Apply demand factor only.
     */
    fun applyDemandFactor(
        load: Double,
        demandFactor: Double
    ): Double {

        return LoadCalculator.applyDemandFactor(
            load = load,
            demandFactor = demandFactor
        )
    }

    /**
     * Apply diversity factor only.
     */
    fun applyDiversityFactor(
        load: Double,
        diversityFactor: Double
    ): Double {

        return LoadCalculator.applyDiversityFactor(
            load = load,
            diversityFactor = diversityFactor
        )
    }

    /**
     * Load after demand factor.
     */
    fun loadAfterDemand(
        load: Double,
        demandFactor: Double
    ): Double {

        return LoadCalculator.loadAfterDemand(
            load = load,
            demandFactor = demandFactor
        )
    }

    /**
     * Load after diversity factor.
     */
    fun loadAfterDiversity(
        load: Double,
        diversityFactor: Double
    ): Double {

        return LoadCalculator.loadAfterDiversity(
            load = load,
            diversityFactor = diversityFactor
        )
    }

    // ============================================================
    // POWER CALCULATIONS
    // ============================================================

    /**
     * Calculate active power.
     *
     * Result is in watts.
     */
    fun calculateActivePower(
        voltage: Double,
        current: Double,
        pf: Double,
        phases: Int
    ): Double {

        return PowerCalculator.activePower(
            voltage = voltage,
            current = current,
            powerFactor = pf,
            phases = phases
        )
    }

    /**
     * Calculate apparent power.
     *
     * Result is in VA.
     */
    fun calculateApparentPower(
        voltage: Double,
        current: Double,
        phases: Int
    ): Double {

        return PowerCalculator.apparentPower(
            voltage = voltage,
            current = current,
            phases = phases
        )
    }

    /**
     * Calculate reactive power.
     *
     * Result is in VAR.
     */
    fun calculateReactivePower(
        active: Double,
        apparent: Double
    ): Double {

        return PowerCalculator.reactivePower(
            active = active,
            apparent = apparent
        )
    }

    /**
     * Calculate power factor.
     */
    fun calculatePowerFactor(
        active: Double,
        apparent: Double
    ): Double {

        return PowerCalculator.powerFactor(
            active = active,
            apparent = apparent
        )
    }

    /**
     * Convert kW to kVA.
     */
    fun calculateKvaFromKw(
        kw: Double,
        powerFactor: Double
    ): Double {

        return PowerCalculator.kvaFromKw(
            kw = kw,
            powerFactor = powerFactor
        )
    }

    /**
     * Convert kW to kVAR.
     */
    fun calculateKvarFromKw(
        kw: Double,
        powerFactor: Double
    ): Double {

        return PowerCalculator.kvarFromKw(
            kw = kw,
            powerFactor = powerFactor
        )
    }

    /**
     * Convert kVA to kW.
     */
    fun calculateKwFromKva(
        kva: Double,
        powerFactor: Double
    ): Double {

        return PowerCalculator.kwFromKva(
            kva = kva,
            powerFactor = powerFactor
        )
    }

    // ============================================================
    // VOLTAGE DROP
    // ============================================================

    /**
     * Calculate voltage drop.
     *
     * Returns:
     * Pair(
     *     voltageDropPercent,
     *     voltageDropVolts
     * )
     */
    fun calculateVoltageDrop(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> {

        return VoltageDropCalculator.calculate(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        )
    }

    /**
     * Voltage drop percentage only.
     */
    fun calculateVoltageDropPercent(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double {

        return VoltageDropCalculator.voltageDropPercent(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        )
    }

    /**
     * Voltage drop in volts only.
     */
    fun calculateVoltageDropVolts(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double {

        return VoltageDropCalculator.voltageDropVolts(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        )
    }

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

    /**
     * Calculate short-circuit current.
     *
     * The calculation engine is isolated from the UI.
     */
    fun calculateShortCircuitCurrent(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 50.0
    ): ShortCircuitResult {

        return ShortCircuitCalculator.calculate(
            voltage = voltage,
            length = length,
            sectionMm2 = sectionMm2,
            material = material,
            currentType = currentType,
            sourceIkKA = sourceIkKA
        )
    }

    // ============================================================
    // CONDUCTOR SIZING
    // ============================================================

    /**
     * Automatic conductor sizing.
     *
     * The conductor calculator is responsible for:
     *
     * - design current
     * - installation method
     * - temperature correction
     * - grouping correction
     * - ampacity
     * - voltage drop
     * - protective-device coordination
     * - short-circuit check
     */
    fun sizeConductor(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        return ConductorSizingCalculator.size(
            input = input,
            standard = standard,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor
        )
    }

    /**
     * Evaluate a user-selected conductor section.
     */
    fun evaluateSelectedSection(
        input: ConductorSizingInput,
        selectedSection: Double,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        return ConductorSizingCalculator.evaluateSelectedSection(
            input = input,
            selectedSection = selectedSection,
            standard = standard,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor
        )
    }

    // ============================================================
    // BREAKER SELECTION
    // ============================================================

    /**
     * Select the next available nominal protective-device rating.
     */
    fun selectBreakerRating(
        designCurrentA: Double,
        cableAmpacityA: Double
    ): Double {

        return BreakerSelectionCalculator.selectRating(
            designCurrentA = designCurrentA,
            cableAmpacityA = cableAmpacityA
        )
    }

    /**
     * Check:
     *
     * Ib <= In <= Iz
     */
    fun checkBreakerCoordination(
        designCurrentA: Double,
        breakerRatingA: Double,
        cableAmpacityA: Double
    ): Boolean {

        return BreakerSelectionCalculator.satisfiesCoordination(
            designCurrentA = designCurrentA,
            breakerRatingA = breakerRatingA,
            cableAmpacityA = cableAmpacityA
        )
    }

    /**
     * Check breaker short-circuit breaking capacity.
     */
    fun checkBreakingCapacity(
        prospectiveFaultCurrentKA: Double,
        breakerBreakingCapacityKA: Double
    ): Boolean {

        return BreakerSelectionCalculator.isBreakingCapacityAdequate(
            prospectiveFaultCurrentKA = prospectiveFaultCurrentKA,
            breakerBreakingCapacityKA = breakerBreakingCapacityKA
        )
    }

    /**
     * Available nominal breaker ratings.
     */
    fun availableBreakerRatings(): List<Double> {

        return BreakerSelectionCalculator.availableRatings()
    }

    // ============================================================
    // TRANSFORMER
    // ============================================================

    /**
     * Calculate required transformer rating in kVA.
     */
    fun calculateRequiredTransformerKva(
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double = 1.0
    ): Double {

        return TransformerSizingCalculator.requiredKva(
            loadKw = loadKw,
            powerFactor = powerFactor,
            growthFactor = growthFactor
        )
    }

    /**
     * Select the next standard transformer rating.
     */
    fun selectTransformerRating(
        requiredKva: Double
    ): Double {

        return TransformerSizingCalculator.selectStandardRating(
            requiredKva = requiredKva
        )
    }

    /**
     * Transformer full-load current.
     */
    fun calculateTransformerFullLoadCurrent(
        kva: Double,
        voltage: Double,
        phases: Int = 3
    ): Double {

        return TransformerSizingCalculator.fullLoadCurrent(
            kva = kva,
            voltage = voltage,
            phases = phases
        )
    }

    /**
     * Transformer short-circuit current from impedance.
     */
    fun calculateTransformerShortCircuitCurrent(
        kva: Double,
        voltage: Double,
        impedancePercent: Double,
        phases: Int = 3
    ): Double {

        return TransformerSizingCalculator.shortCircuitCurrentFromImpedance(
            kva = kva,
            voltage = voltage,
            impedancePercent = impedancePercent,
            phases = phases
        )
    }

    /**
     * Standard transformer ratings available to the application.
     */
    fun availableTransformerRatings(): List<Double> {

        return TransformerSizingCalculator.standardRatings()
    }

    // ============================================================
    // STANDARD / CODE INFORMATION
    // ============================================================

    /**
     * Return available engineering standards.
     *
     * The actual code engine is selected elsewhere through
     * CodeEngineFactory.
     */
    fun availableStandards(): List<Standard> {

        return Standard.entries.toList()
    }

    /**
     * Display name for selected standard.
     */
    fun standardDisplayName(
        standard: Standard
    ): String {

        return standard.displayName
    }

    /**
     * Short code of selected standard.
     */
    fun standardShortName(
        standard: Standard
    ): String {

        return standard.shortName
    }

    /**
     * Description of selected standard.
     */
    fun standardDescription(
        standard: Standard
    ): String {

        return standard.description
    }
}
