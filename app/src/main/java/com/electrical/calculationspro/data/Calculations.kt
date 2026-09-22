package com.electrical.calculationspro.data

import com.electrical.calculationspro.data.calculators.BreakerSelectionCalculator
import com.electrical.calculationspro.data.calculators.ConductorSizingCalculator
import com.electrical.calculationspro.data.calculators.EquipmentSelectionCalculator
import com.electrical.calculationspro.data.calculators.LoadCalculator
import com.electrical.calculationspro.data.calculators.PowerCalculator
import com.electrical.calculationspro.data.calculators.ShortCircuitCalculator
import com.electrical.calculationspro.data.calculators.TransformerSizingCalculator
import com.electrical.calculationspro.data.calculators.VoltageDropCalculator
import com.electrical.calculationspro.data.catalog.BreakerCatalogItem
import com.electrical.calculationspro.data.catalog.BusbarCatalogItem
import com.electrical.calculationspro.data.catalog.CableCatalogItem
import com.electrical.calculationspro.data.catalog.ContactorCatalogItem
import com.electrical.calculationspro.data.catalog.GeneratorCatalogItem
import com.electrical.calculationspro.data.catalog.Manufacturer
import com.electrical.calculationspro.data.catalog.PanelCatalogItem
import com.electrical.calculationspro.data.catalog.TransformerCatalogItem
import com.electrical.calculationspro.data.standards.CodeEngineFactory

/**
 * ================================================================
 * PROFESSIONAL ENGINEERING CORE / FACADE
 * ================================================================
 *
 * UI
 *  ↓
 * ElectricalCalculations
 *  ↓
 * Engineering Calculators
 *  ↓
 * Standard Engine
 *  ↓
 * Equipment Catalog
 *
 * This object is the single public entry point for:
 *
 * - Engineering calculations
 * - Standard selection
 * - Equipment catalog selection
 *
 * UI must not call calculators or catalogs directly.
 * ================================================================
 */
object ElectricalCalculations {

    // ============================================================
    // LOAD
    // ============================================================

    fun calculateDesignCurrent(
        loadWatts: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double =
        LoadCalculator.designCurrent(
            loadWatts = loadWatts,
            voltage = voltage,
            powerFactor = powerFactor,
            currentType = currentType
        )

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

    fun applyDemandFactor(
        current: Double,
        demandFactor: Double
    ): Double =
        LoadCalculator.applyDemandFactor(
            current = current,
            demandFactor = demandFactor
        )

    fun applyDiversityFactor(
        current: Double,
        diversityFactor: Double
    ): Double =
        LoadCalculator.applyDiversityFactor(
            current = current,
            diversityFactor = diversityFactor
        )

    fun applyDemandAndDiversity(
        current: Double,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): Double =
        LoadCalculator.applyDemandAndDiversity(
            current = current,
            demandFactor = demandFactor,
            diversityFactor = diversityFactor
        )

    fun loadAfterDemand(
        loadWatts: Double,
        demandFactor: Double
    ): Double =
        LoadCalculator.loadAfterDemand(
            loadWatts = loadWatts,
            demandFactor = demandFactor
        )

    fun loadAfterDiversity(
        loadWatts: Double,
        diversityFactor: Double
    ): Double =
        LoadCalculator.loadAfterDiversity(
            loadWatts = loadWatts,
            diversityFactor = diversityFactor
        )

    // ============================================================
    // POWER
    // ============================================================

    fun calculateActivePower(
        voltage: Double,
        current: Double,
        pf: Double,
        phases: Int
    ): Double =
        PowerCalculator.activePower(
            voltage = voltage,
            current = current,
            powerFactor = pf,
            phases = phases
        )

    fun calculateApparentPower(
        voltage: Double,
        current: Double,
        phases: Int
    ): Double =
        PowerCalculator.apparentPower(
            voltage = voltage,
            current = current,
            phases = phases
        )

    fun calculateReactivePower(
        active: Double,
        apparent: Double
    ): Double =
        PowerCalculator.reactivePower(
            activePower = active,
            apparentPower = apparent
        )

    fun calculatePowerFactor(
        active: Double,
        apparent: Double
    ): Double =
        PowerCalculator.powerFactor(
            activePower = active,
            apparentPower = apparent
        )

    fun calculateKvaFromKw(
        kw: Double,
        powerFactor: Double
    ): Double =
        PowerCalculator.kvaFromKw(
            kw = kw,
            powerFactor = powerFactor
        )

    fun calculateKvarFromKw(
        kw: Double,
        powerFactor: Double
    ): Double =
        PowerCalculator.kvarFromKw(
            kw = kw,
            powerFactor = powerFactor
        )

    fun calculateKwFromKva(
        kva: Double,
        powerFactor: Double
    ): Double =
        PowerCalculator.kwFromKva(
            kva = kva,
            powerFactor = powerFactor
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

    fun calculateVoltageDropPercent(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double =
        VoltageDropCalculator.voltageDropPercent(
            current = current,
            length = length,
            sectionMm2 = sectionMm2,
            powerFactor = powerFactor,
            currentType = currentType,
            material = material,
            voltage = voltage
        )

    fun calculateVoltageDropVolts(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Double =
        VoltageDropCalculator.voltageDropVolts(
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
    // CONDUCTOR SIZING
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

    fun evaluateSelectedSection(
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
    // BREAKER ENGINEERING SELECTION
    // ============================================================

    fun selectBreakerRating(
        designCurrentA: Double,
        cableAmpacityA: Double,
        standard: Standard = Standard.IEC
    ): Double =
        BreakerSelectionCalculator.selectRating(
            designCurrentA = designCurrentA,
            cableAmpacityA = cableAmpacityA,
            standard = standard
        )

    fun checkBreakerCoordination(
        designCurrentA: Double,
        breakerRatingA: Double,
        cableAmpacityA: Double
    ): Boolean =
        BreakerSelectionCalculator.satisfiesCoordination(
            designCurrentA = designCurrentA,
            breakerRatingA = breakerRatingA,
            cableAmpacityA = cableAmpacityA
        )

    fun checkBreakingCapacity(
        prospectiveFaultCurrentKA: Double,
        breakerBreakingCapacityKA: Double
    ): Boolean =
        BreakerSelectionCalculator.isBreakingCapacityAdequate(
            prospectiveFaultCurrentKA =
                prospectiveFaultCurrentKA,
            breakerBreakingCapacityKA =
                breakerBreakingCapacityKA
        )

    fun availableBreakerRatings(
        standard: Standard = Standard.IEC
    ): List<Double> =
        BreakerSelectionCalculator.availableRatings(
            standard = standard
        )

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
            prospectiveFaultCurrentKA =
                prospectiveFaultCurrentKA,
            breakerBreakingCapacityKA =
                breakerBreakingCapacityKA,
            standard = standard
        )

    // ============================================================
    // BREAKER CATALOG
    // ============================================================

    fun selectBreakerFromCatalog(
        designCurrentA: Double,
        shortCircuitKA: Double = 0.0,
        manufacturer: Manufacturer? = null
    ): EquipmentCatalogResult<BreakerCatalogItem> =
        EquipmentSelectionCalculator.selectBreaker(
            designCurrentA = designCurrentA,
            shortCircuitKA = shortCircuitKA,
            manufacturer = manufacturer
        )

    // ============================================================
    // CABLE CATALOG
    // ============================================================

    fun selectCableFromCatalog(
        requiredSectionMm2: Double,
        manufacturer: Manufacturer? = null
    ): EquipmentCatalogResult<CableCatalogItem> =
        EquipmentSelectionCalculator.selectCable(
            requiredSectionMm2 = requiredSectionMm2,
            manufacturer = manufacturer
        )

    // ============================================================
    // TRANSFORMER
    // ============================================================

    fun calculateRequiredTransformerKva(
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double = 1.0
    ): Double =
        TransformerSizingCalculator.requiredKva(
            loadKw = loadKw,
            powerFactor = powerFactor,
            growthFactor = growthFactor
        )

    fun selectTransformerRating(
        requiredKva: Double
    ): Double =
        TransformerSizingCalculator.selectStandardRating(
            requiredKva = requiredKva
        )

    fun calculateTransformerFullLoadCurrent(
        kva: Double,
        voltage: Double,
        phases: Int = 3
    ): Double =
        TransformerSizingCalculator.fullLoadCurrent(
            kva = kva,
            voltage = voltage,
            phases = phases
        )

    fun calculateTransformerShortCircuitCurrent(
        kva: Double,
        voltage: Double,
        impedancePercent: Double,
        phases: Int = 3
    ): Double =
        TransformerSizingCalculator
            .shortCircuitCurrentFromImpedance(
                kva = kva,
                voltage = voltage,
                impedancePercent = impedancePercent,
                phases = phases
            )

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

    fun availableTransformerRatings(): List<Double> =
        TransformerSizingCalculator.standardRatings()

    // ============================================================
    // TRANSFORMER CATALOG
    // ============================================================

    fun selectTransformerFromCatalog(
        requiredKva: Double
    ): EquipmentCatalogResult<TransformerCatalogItem> =
        EquipmentSelectionCalculator.selectTransformer(
            requiredKva = requiredKva
        )

    // ============================================================
    // GENERATOR CATALOG
    // ============================================================

    fun selectGeneratorFromCatalog(
        requiredKva: Double
    ): EquipmentCatalogResult<GeneratorCatalogItem> =
        EquipmentSelectionCalculator.selectGenerator(
            requiredKva = requiredKva
        )

    // ============================================================
    // BUSBAR CATALOG
    // ============================================================

    fun selectBusbarFromCatalog(
        currentA: Double
    ): EquipmentCatalogResult<BusbarCatalogItem> =
        EquipmentSelectionCalculator.selectBusbar(
            currentA = currentA
        )

    // ============================================================
    // CONTACTOR CATALOG
    // ============================================================

    fun selectContactorFromCatalog(
        motorCurrentA: Double
    ): EquipmentCatalogResult<ContactorCatalogItem> =
        EquipmentSelectionCalculator.selectContactor(
            motorCurrentA = motorCurrentA
        )

    // ============================================================
    // PANEL CATALOG
    // ============================================================

    fun selectPanelFromCatalog(
        currentA: Double
    ): EquipmentCatalogResult<PanelCatalogItem> =
        EquipmentSelectionCalculator.selectPanel(
            currentA = currentA
        )

    // ============================================================
    // STANDARD
    // ============================================================

    fun availableStandards(): List<Standard> =
        Standard.entries.toList()

    fun standardDisplayName(
        standard: Standard
    ): String =
        standard.displayName

    fun standardShortName(
        standard: Standard
    ): String =
        standard.shortName

    fun standardDescription(
        standard: Standard
    ): String =
        standard.description

    fun standardEngine(
        standard: Standard
    ): StandardEngine =
        CodeEngineFactory.get(standard)

    // ============================================================
    // ENGINEERING + CATALOG WORKFLOW
    // ============================================================

    /**
     * Performs preliminary engineering selection and then
     * exposes catalog selection through the same facade.
     *
     * The UI can therefore use:
     *
     * ElectricalCalculations
     *
     * without knowing where the calculation or catalog lives.
     */

    fun calculateAndSelectBreaker(
        designCurrentA: Double,
        cableAmpacityA: Double,
        shortCircuitKA: Double = 0.0,
        standard: Standard = Standard.IEC,
        manufacturer: Manufacturer? = null
    ): BreakerEngineeringCatalogResult {

        val engineering =
            calculateBreakerSelection(
                designCurrentA = designCurrentA,
                cableAmpacityA = cableAmpacityA,
                prospectiveFaultCurrentKA = shortCircuitKA,
                standard = standard
            )

        val catalog =
            selectBreakerFromCatalog(
                designCurrentA = designCurrentA,
                shortCircuitKA = shortCircuitKA,
                manufacturer = manufacturer
            )

        return BreakerEngineeringCatalogResult(
            engineering = engineering,
            catalog = catalog
        )
    }

    fun calculateAndSelectTransformer(
        loadKw: Double,
        powerFactor: Double,
        growthFactor: Double = 1.0,
        voltage: Double,
        phases: Int = 3
    ): TransformerEngineeringCatalogResult {

        val engineering =
            calculateTransformerSizing(
                loadKw = loadKw,
                powerFactor = powerFactor,
                growthFactor = growthFactor,
                voltage = voltage,
                phases = phases
            )

        val catalog =
            selectTransformerFromCatalog(
                requiredKva = engineering.requiredKva
            )

        return TransformerEngineeringCatalogResult(
            engineering = engineering,
            catalog = catalog
        )
    }

    fun calculateAndSelectGenerator(
        requiredKva: Double
    ): EquipmentCatalogResult<GeneratorCatalogItem> =
        selectGeneratorFromCatalog(
            requiredKva = requiredKva
        )

    fun calculateAndSelectBusbar(
        currentA: Double
    ): EquipmentCatalogResult<BusbarCatalogItem> =
        selectBusbarFromCatalog(
            currentA = currentA
        )

    fun calculateAndSelectPanel(
        currentA: Double
    ): EquipmentCatalogResult<PanelCatalogItem> =
        selectPanelFromCatalog(
            currentA = currentA
        )
}

// ================================================================
// COMBINED RESULTS
// ================================================================

data class BreakerEngineeringCatalogResult(
    val engineering: BreakerSelectionResult,
    val catalog: EquipmentCatalogResult<BreakerCatalogItem>
)

data class TransformerEngineeringCatalogResult(
    val engineering: TransformerSizingResult,
    val catalog: EquipmentCatalogResult<TransformerCatalogItem>
)
