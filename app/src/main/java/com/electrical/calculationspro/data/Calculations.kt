package com.electrical.calculationspro.data

import kotlin.math.sqrt

/**
 * Comprehensive Electrical Calculations
 * Includes: Conductor sizing, Demand/Diversity factors, Voltage drop, Short circuit
 * Based on IEC 60364 + Egyptian Code practices
 */
object ElectricalCalculations {

    // ==================== BASIC CURRENT ====================

    fun calculateDesignCurrent(
        loadWatts: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double {
        return when (currentType) {
            CurrentType.DirectCurrent -> loadWatts / voltage
            CurrentType.AlternatingSinglePhase -> loadWatts / (voltage * powerFactor)
            CurrentType.AlternatingTwoPhase -> loadWatts / (voltage * powerFactor * sqrt(2.0))
            CurrentType.AlternatingThreePhase -> loadWatts / (voltage * powerFactor * sqrt(3.0))
        }
    }

    /**
     * Apply Demand Factor + Diversity Factor
     * Ib_corrected = Ib × DemandFactor × DiversityFactor
     */
    fun applyDemandAndDiversity(
        ib: Double,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): Double {
        return ib * demandFactor.coerceIn(0.1, 1.0) * diversityFactor.coerceIn(0.1, 1.0)
    }

    // ==================== VOLTAGE DROP ====================

    fun calculateVoltageDrop(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> {
        val resistivity = if (material == ConductorMaterial.Copper) 0.0225 else 0.036
        val r = resistivity / sectionMm2
        val x = 0.08 / 1000.0

        val cosPhi = powerFactor.coerceIn(0.0, 1.0)
        val sinPhi = sqrt((1 - cosPhi * cosPhi).coerceAtLeast(0.0))

        val factor = when (currentType) {
            CurrentType.DirectCurrent -> 2.0
            CurrentType.AlternatingSinglePhase -> 2.0
            CurrentType.AlternatingTwoPhase -> 2.0
            CurrentType.AlternatingThreePhase -> sqrt(3.0)
        }

        val dropVolts = factor * current * length * (r * cosPhi + x * sinPhi)
        val dropPercent = (dropVolts / voltage) * 100.0
        return dropPercent to dropVolts
    }

    // ==================== SHORT CIRCUIT ====================

    fun calculateShortCircuitCurrent(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 50.0
    ): ShortCircuitResult {
        val resistivity = if (material == ConductorMaterial.Copper) 0.018 else 0.029
        val rCable = (resistivity * length * 2) / sectionMm2
        val xCable = (0.08 * length) / 1000.0 * 2

        val zCable = sqrt(rCable * rCable + xCable * xCable)

        val zSource = if (sourceIkKA > 0) {
            (voltage / sqrt(3.0)) / (sourceIkKA * 1000.0)
        } else 0.0

        val zTotal = zSource + zCable

        val ik = when (currentType) {
            CurrentType.AlternatingThreePhase -> (voltage / sqrt(3.0)) / zTotal
            else -> (voltage / 2.0) / zTotal
        }

        val ikKA = ik / 1000.0
        val i2t = ik * ik * 0.1

        return ShortCircuitResult(
            ikAmps = ik,
            ikKA = ikKA,
            cableImpedance = zCable,
            sourceImpedance = zSource,
            i2t = i2t,
            notes = listOf(
                "Ik ≈ ${"%.2f".format(ikKA)} kA",
                "Z cable ≈ ${"%.4f".format(zCable)} Ω",
                "Z source ≈ ${"%.4f".format(zSource)} Ω",
                "I²t (0.1s) ≈ ${"%.0f".format(i2t)}"
            )
        )
    }

    // ==================== CONDUCTOR SIZING (FULL) ====================

    fun sizeConductor(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        val ibRaw = calculateDesignCurrent(
            input.load, input.voltage, input.powerFactor, input.currentType
        )

        val ib = applyDemandAndDiversity(ibRaw, demandFactor, diversityFactor)

        val methodKey = IecTables.methodToKey(input.installationMethod.code)

        val loadedConductors = when (input.currentType) {
            CurrentType.DirectCurrent, CurrentType.AlternatingSinglePhase -> 2
            CurrentType.AlternatingTwoPhase -> 2
            CurrentType.AlternatingThreePhase -> 3
        }

        val tempFactor = if (input.insulation == InsulationType.XLPE || input.insulation == InsulationType.EPR)
            IecTables.ambientCorrectionXlpe(input.ambientTemp)
        else
            IecTables.ambientCorrectionPvc(input.ambientTemp)

        val groupFactor = IecTables.groupingFactor(input.circuitsInConduit)
        val requiredIz = ib / (tempFactor * groupFactor)

        var selectedSection = 1.5
        var ampacity = 0.0
        var vdPercent = 100.0
        var vdVolts = 0.0
        var found = false

        for (section in standardSections) {
            val baseIz = IecTables.getBaseAmpacity(
                section, methodKey, loadedConductors, input.conductor, input.insulation
            )
            ampacity = baseIz * tempFactor * groupFactor
            if (ampacity < requiredIz) continue

            val (percent, volts) = calculateVoltageDrop(
                ib, input.lineLength, section, input.powerFactor,
                input.currentType, input.conductor, input.voltage
            )
            if (percent <= input.maxVoltageDrop) {
                selectedSection = section
                vdPercent = percent
                vdVolts = volts
                found = true
                break
            }
        }

        if (!found) {
            for (section in standardSections.reversed()) {
                val baseIz = IecTables.getBaseAmpacity(section, methodKey, loadedConductors, input.conductor, input.insulation)
                ampacity = baseIz * tempFactor * groupFactor
                if (ampacity >= requiredIz) {
                    selectedSection = section
                    val (percent, volts) = calculateVoltageDrop(
                        ib, input.lineLength, section, input.powerFactor,
                        input.currentType, input.conductor, input.voltage
                    )
                    vdPercent = percent
                    vdVolts = volts
                    break
                }
            }
        }

        val standardBreakers = listOf(
            6.0, 10.0, 16.0, 20.0, 25.0, 32.0, 40.0, 50.0,
            63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0, 315.0, 400.0, 500.0, 630.0
        )
        val protective = standardBreakers.firstOrNull { it >= ib } ?: (ib * 1.25)

        val sc = calculateShortCircuitCurrent(
            input.voltage, input.lineLength, selectedSection,
            input.conductor, input.currentType
        )

        val notes = mutableListOf<String>()
        notes.add("Ib raw = ${"%.2f".format(ibRaw)} A")
        notes.add("Demand = ${"%.2f".format(demandFactor)} | Diversity = ${"%.2f".format(diversityFactor)}")
        notes.add("Ib corrected = ${"%.2f".format(ib)} A")
        notes.add("Required Iz ≈ ${"%.1f".format(requiredIz)} A")
        notes.add("Selected S = $selectedSection mm²")
        notes.add("Iz (after factors) ≈ ${"%.1f".format(ampacity)} A")
        notes.add("ΔU = \( {"%.2f".format(vdPercent)} % ( \){"%.2f".format(vdVolts)} V)")
        notes.add("Method: ${input.installationMethod.code} → $methodKey")
        notes.add("Ca = ${"%.2f".format(tempFactor)} | Cg = ${"%.2f".format(groupFactor)}")
        notes.add("Ik ≈ ${"%.2f".format(sc.ikKA)} kA")

        when (standard) {
            Standard.EGYPTIAN -> {
                notes.add("حسب الكود المصري (IEC 60364-5-52)")
                notes.add("يشمل: طلب + تشتت + قصر + هبوط جهد")
            }
            Standard.IEC -> {
                notes.add("IEC 60364-5-52 full tables")
                notes.add("Includes: Demand + Diversity + Short circuit")
            }
            else -> notes.add("Verify with official standard tables")
        }

        if (vdPercent > input.maxVoltageDrop) {
            notes.add("⚠ هبوط الجهد أكبر من الحد المسموح")
        }

        return ConductorSizingResult(
            designCurrent = ib,
            recommendedSection = selectedSection,
            selectedSection = selectedSection,
            ampacity = ampacity,
            voltageDropPercent = vdPercent,
            voltageDropVolts = vdVolts,
            protectiveDevice = protective,
            notes = notes
        )
    }

    // ==================== POWER CALCULATIONS ====================

    fun calculateActivePower(voltage: Double, current: Double, pf: Double, phases: Int): Double {
        return when (phases) {
            1 -> voltage * current * pf
            3 -> sqrt(3.0) * voltage * current * pf
            else -> voltage * current * pf
        }
    }

    fun calculateApparentPower(voltage: Double, current: Double, phases: Int): Double {
        return when (phases) {
            1 -> voltage * current
            3 -> sqrt(3.0) * voltage * current
            else -> voltage * current
        }
    }

    fun calculateReactivePower(active: Double, apparent: Double): Double {
        return sqrt((apparent * apparent - active * active).coerceAtLeast(0.0))
    }

    fun calculatePowerFactor(active: Double, apparent: Double): Double {
        return if (apparent > 0) (active / apparent).coerceIn(0.0, 1.0) else 0.0
    }
}

data class ShortCircuitResult(
    val ikAmps: Double,
    val ikKA: Double,
    val cableImpedance: Double,
    val sourceImpedance: Double,
    val i2t: Double,
    val notes: List<String>
)
