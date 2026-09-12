package com.electrical.calculationspro.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt

object ElectricalCalculations {

    private const val EPSILON = 1.0e-9

    private val standardBreakers = listOf(
        6.0, 10.0, 16.0, 20.0, 25.0, 32.0, 40.0,
        50.0, 63.0, 80.0, 100.0, 125.0, 160.0,
        200.0, 250.0, 315.0, 400.0, 500.0, 630.0
    )

    fun calculateDesignCurrent(
        loadWatts: Double,
        voltage: Double,
        powerFactor: Double,
        currentType: CurrentType
    ): Double {
        require(loadWatts >= 0.0)
        require(voltage > EPSILON)
        require(powerFactor > 0.0 && powerFactor <= 1.0)

        return when (currentType) {
            CurrentType.DirectCurrent ->
                loadWatts / voltage

            CurrentType.AlternatingSinglePhase ->
                loadWatts / (voltage * powerFactor)

            CurrentType.AlternatingTwoPhase ->
                loadWatts / (2.0 * voltage * powerFactor)

            CurrentType.AlternatingThreePhase ->
                loadWatts /
                    (sqrt(3.0) * voltage * powerFactor)
        }
    }

    fun applyDemandAndDiversity(
        ib: Double,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): Double {
        require(ib >= 0.0)
        require(demandFactor in 0.0..1.0)
        require(diversityFactor in 0.0..1.0)

        return ib * demandFactor * diversityFactor
    }

    fun calculateVoltageDrop(
        current: Double,
        length: Double,
        sectionMm2: Double,
        powerFactor: Double,
        currentType: CurrentType,
        material: ConductorMaterial,
        voltage: Double
    ): Pair<Double, Double> {

        require(current >= 0.0)
        require(length >= 0.0)
        require(sectionMm2 > EPSILON)
        require(voltage > EPSILON)
        require(powerFactor > 0.0 && powerFactor <= 1.0)

        val resistivity = when (material) {
            ConductorMaterial.Copper -> 0.0225
            ConductorMaterial.Aluminum -> 0.0360
        }

        val resistancePerMeter =
            resistivity / sectionMm2

        val reactancePerMeter =
            0.08 / 1000.0

        val cosPhi = powerFactor

        val sinPhi = sqrt(
            (1.0 - cosPhi * cosPhi)
                .coerceAtLeast(0.0)
        )

        val factor = when (currentType) {
            CurrentType.DirectCurrent -> 2.0
            CurrentType.AlternatingSinglePhase -> 2.0
            CurrentType.AlternatingTwoPhase -> 2.0
            CurrentType.AlternatingThreePhase -> sqrt(3.0)
        }

        val dropVolts =
            factor *
                current *
                length *
                (
                    resistancePerMeter * cosPhi +
                        reactancePerMeter * sinPhi
                    )

        val dropPercent =
            dropVolts / voltage * 100.0

        return dropPercent to dropVolts
    }

    fun calculateShortCircuitCurrent(
        voltage: Double,
        length: Double,
        sectionMm2: Double,
        material: ConductorMaterial,
        currentType: CurrentType,
        sourceIkKA: Double = 50.0
    ): ShortCircuitResult {

        require(voltage > EPSILON)
        require(length >= 0.0)
        require(sectionMm2 > EPSILON)
        require(sourceIkKA > EPSILON)

        val resistivity = when (material) {
            ConductorMaterial.Copper -> 0.018
            ConductorMaterial.Aluminum -> 0.029
        }

        val cableResistance =
            resistivity *
                length *
                2.0 /
                sectionMm2

        val cableReactance =
            0.08 *
                length /
                1000.0 *
                2.0

        val sourceImpedance =
            (
                voltage / sqrt(3.0)
                ) /
                (sourceIkKA * 1000.0)

        val totalResistance =
            sourceImpedance + cableResistance

        val totalImpedance =
            sqrt(
                totalResistance * totalResistance +
                    cableReactance * cableReactance
            ).coerceAtLeast(EPSILON)

        val faultVoltage = when (currentType) {
            CurrentType.AlternatingThreePhase ->
                voltage / sqrt(3.0)

            CurrentType.AlternatingSinglePhase,
            CurrentType.AlternatingTwoPhase,
            CurrentType.DirectCurrent ->
                voltage
        }

        val faultCurrent =
            faultVoltage / totalImpedance

        val faultCurrentKA =
            faultCurrent / 1000.0

        val i2t =
            faultCurrent *
                faultCurrent *
                0.1

        return ShortCircuitResult(
            ikAmps = faultCurrent,
            ikKA = faultCurrentKA,
            cableImpedance = totalImpedance,
            sourceImpedance = sourceImpedance,
            i2t = i2t,
            notes = listOf(
                "Ik = %.2f kA".format(faultCurrentKA),
                "Cable/source impedance = %.4f Ω"
                    .format(totalImpedance),
                "Source impedance = %.4f Ω"
                    .format(sourceImpedance),
                "I²t at 0.10 s = %.0f A²s"
                    .format(i2t)
            )
        )
    }

    fun sizeConductor(
        input: ConductorSizingInput,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        validateInput(input)

        val rawCurrent =
            calculateDesignCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        val designCurrent =
            applyDemandAndDiversity(
                ib = rawCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        val methodKey =
            IecTables.methodToKey(
                input.installationMethod.code
            )

        val loadedConductors =
            loadedConductorCount(input.currentType)

        val temperatureFactor =
            temperatureCorrection(
                input.insulation,
                input.ambientTemp
            )

        val groupingFactor =
            IecTables.groupingFactor(
                input.circuitsInConduit
            ).coerceAtLeast(EPSILON)

        val requiredIz =
            designCurrent /
                (temperatureFactor * groupingFactor)

        var ampacityCandidate: Double? = null

        for (section in standardSections) {

            val baseAmpacity =
                IecTables.getBaseAmpacity(
                    section = section,
                    method = methodKey,
                    loadedConductors = loadedConductors,
                    material = input.conductor,
                    insulation = input.insulation
                )

            val correctedAmpacity =
                baseAmpacity *
                    temperatureFactor *
                    groupingFactor

            if (
                correctedAmpacity >= requiredIz &&
                ampacityCandidate == null
            ) {
                ampacityCandidate = section
            }

            if (correctedAmpacity < requiredIz) {
                continue
            }

            val voltageDrop =
                calculateVoltageDrop(
                    current = designCurrent,
                    length = input.lineLength,
                    sectionMm2 = section,
                    powerFactor = input.powerFactor,
                    currentType = input.currentType,
                    material = input.conductor,
                    voltage = input.voltage
                )

            if (
                voltageDrop.first <=
                input.maxVoltageDrop
            ) {
                return evaluateSection(
                    input = input,
                    section = section,
                    designCurrent = designCurrent,
                    rawDesignCurrent = rawCurrent,
                    standard = standard
                )
            }
        }

        val fallback =
            ampacityCandidate
                ?: standardSections.last()

        return evaluateSection(
            input = input,
            section = fallback,
            designCurrent = designCurrent,
            rawDesignCurrent = rawCurrent,
            standard = standard,
            forceVoltageDropWarning = true,
            requiredIzOverride = requiredIz
        )
    }

    fun evaluateSelectedSection(
        input: ConductorSizingInput,
        selectedSection: Double,
        standard: Standard = Standard.IEC,
        demandFactor: Double = 1.0,
        diversityFactor: Double = 1.0
    ): ConductorSizingResult {

        validateInput(input)

        require(selectedSection in standardSections)

        val rawCurrent =
            calculateDesignCurrent(
                loadWatts = input.load,
                voltage = input.voltage,
                powerFactor = input.powerFactor,
                currentType = input.currentType
            )

        val designCurrent =
            applyDemandAndDiversity(
                ib = rawCurrent,
                demandFactor = demandFactor,
                diversityFactor = diversityFactor
            )

        return evaluateSection(
            input = input,
            section = selectedSection,
            designCurrent = designCurrent,
            rawDesignCurrent = rawCurrent,
            standard = standard
        )
    }

    private fun evaluateSection(
        input: ConductorSizingInput,
        section: Double,
        designCurrent: Double,
        rawDesignCurrent: Double,
        standard: Standard,
        forceVoltageDropWarning: Boolean = false,
        requiredIzOverride: Double? = null
    ): ConductorSizingResult {

        val methodKey =
            IecTables.methodToKey(
                input.installationMethod.code
            )

        val loadedConductors =
            loadedConductorCount(input.currentType)

        val temperatureFactor =
            temperatureCorrection(
                input.insulation,
                input.ambientTemp
            )

        val groupingFactor =
            IecTables.groupingFactor(
                input.circuitsInConduit
            ).coerceAtLeast(EPSILON)

        val requiredIz =
            requiredIzOverride
                ?: (
                    designCurrent /
                        (
                            temperatureFactor *
                                groupingFactor
                            )
                    )

        val baseAmpacity =
            IecTables.getBaseAmpacity(
                section = section,
                method = methodKey,
                loadedConductors = loadedConductors,
                material = input.conductor,
                insulation = input.insulation
            )

        val correctedAmpacity =
            baseAmpacity *
                temperatureFactor *
                groupingFactor

        val voltageDrop =
            calculateVoltageDrop(
                current = designCurrent,
                length = input.lineLength,
                sectionMm2 = section,
                powerFactor = input.powerFactor,
                currentType = input.currentType,
                material = input.conductor,
                voltage = input.voltage
            )

        val voltageDropOk =
            voltageDrop.first <=
                input.maxVoltageDrop

        val breaker =
            standardBreakers.firstOrNull {
                it >= designCurrent &&
                    it <= correctedAmpacity + EPSILON
            } ?: 0.0

        val breakerOk =
            breaker > 0.0 &&
                breaker >= designCurrent &&
                breaker <=
                    correctedAmpacity + EPSILON

        val shortCircuit =
            calculateShortCircuitCurrent(
                voltage = input.voltage,
                length = input.lineLength,
                sectionMm2 = section,
                material = input.conductor,
                currentType = input.currentType
            )

        val notes = mutableListOf<String>()

        notes +=
            "Ib raw = %.2f A"
                .format(rawDesignCurrent)

        notes +=
            "Ib design = %.2f A"
                .format(designCurrent)

        notes +=
            "Required Iz = %.1f A"
                .format(requiredIz)

        notes +=
            "Base Iz = %.1f A"
                .format(baseAmpacity)

        notes +=
            "Corrected Iz = %.1f A"
                .format(correctedAmpacity)

        notes +=
            "Cable = %.1f mm² %s"
                .format(
                    section,
                    input.conductor.name
                )

        notes +=
            "Ca = %.3f | Cg = %.3f"
                .format(
                    temperatureFactor,
                    groupingFactor
                )

        notes +=
            "Installation = ${input.installationMethod.code}"

        notes +=
            "Voltage drop = %.2f %% (%.2f V)"
                .format(
                    voltageDrop.first,
                    voltageDrop.second
                )

        if (
            !voltageDropOk ||
            forceVoltageDropWarning
        ) {
            notes +=
                "WARNING: voltage drop exceeds the configured limit."
        }

        if (breakerOk) {
            notes +=
                "Protective device = %.0f A"
                    .format(breaker)
        } else {
            notes +=
                "WARNING: no breaker satisfies Ib ≤ In ≤ Iz."
        }

        notes +=
            "Estimated end fault current = %.2f kA"
                .format(shortCircuit.ikKA)

        when (standard) {
            Standard.IEC ->
                notes +=
                    "Reference: IEC 60364-5-52"

            Standard.EGYPTIAN ->
                notes +=
                    "Reference: Egyptian Electrical Code / IEC basis"

            Standard.CEI ->
                notes +=
                    "Reference: CEI 64-8"

            Standard.NEC ->
                notes +=
                    "WARNING: IEC ampacity dataset is being used; NEC tables are required for NEC compliance."

            Standard.CEC ->
                notes +=
                    "WARNING: IEC ampacity dataset is being used; CEC tables are required for CEC compliance."
        }

        return ConductorSizingResult(
            designCurrent = designCurrent,
            recommendedSection = section,
            selectedSection = section,
            ampacity = correctedAmpacity,
            voltageDropPercent = voltageDrop.first,
            voltageDropVolts = voltageDrop.second,
            protectiveDevice = breaker,
            shortCircuitCurrentKA = shortCircuit.ikKA,
            breakerWithinCableCapacity = breakerOk,
            voltageDropWithinLimit = voltageDropOk,
            notes = notes
        )
    }

    private fun validateInput(
        input: ConductorSizingInput
    ) {
        require(input.voltage > EPSILON)
        require(input.load >= 0.0)
        require(input.lineLength >= 0.0)
        require(
            input.powerFactor > 0.0 &&
                input.powerFactor <= 1.0
        )
        require(input.circuitsInConduit > 0)
        require(input.maxVoltageDrop > 0.0)
    }

    private fun loadedConductorCount(
        currentType: CurrentType
    ): Int {
        return when (currentType) {
            CurrentType.DirectCurrent -> 2
            CurrentType.AlternatingSinglePhase -> 2
            CurrentType.AlternatingTwoPhase -> 2
            CurrentType.AlternatingThreePhase -> 3
        }
    }

    private fun temperatureCorrection(
        insulation: InsulationType,
        ambientTemperature: Double
    ): Double {
        return when (insulation) {
            InsulationType.XLPE,
            InsulationType.EPR ->
                IecTables.ambientCorrectionXlpe(
                    ambientTemperature
                )

            InsulationType.PVC,
            InsulationType.Rubber ->
                IecTables.ambientCorrectionPvc(
                    ambientTemperature
                )
        }.coerceAtLeast(EPSILON)
    }

    fun calculateActivePower(
        voltage: Double,
        current: Double,
        pf: Double,
        phases: Int
    ): Double {

        require(voltage >= 0.0)
        require(current >= 0.0)
        require(pf in 0.0..1.0)

        return when (phases) {
            1 ->
                voltage * current * pf

            3 ->
                sqrt(3.0) *
                    voltage *
                    current *
                    pf

            else ->
                voltage * current * pf
        }
    }

    fun calculateApparentPower(
        voltage: Double,
        current: Double,
        phases: Int
    ): Double {

        require(voltage >= 0.0)
        require(current >= 0.0)

        return when (phases) {
            1 ->
                voltage * current

            3 ->
                sqrt(3.0) *
                    voltage *
                    current

            else ->
                voltage * current
        }
    }

    fun calculateReactivePower(
        active: Double,
        apparent: Double
    ): Double {

        require(active >= 0.0)
        require(apparent >= 0.0)

        return sqrt(
            (
                apparent * apparent -
                    active * active
                ).coerceAtLeast(0.0)
        )
    }

    fun calculatePowerFactor(
        active: Double,
        apparent: Double
    ): Double {

        require(active >= 0.0)
        require(apparent >= 0.0)

        return if (apparent > EPSILON) {
            (active / apparent)
                .coerceIn(0.0, 1.0)
        } else {
            0.0
        }
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

/*
 * ============================================================
 * SAVED CALCULATIONS
 * ============================================================
 *
 * Storage is intentionally kept in this file so the project
 * does not need a separate CalculationStorage.kt file.
 */

data class SavedCalculation(
    val id: Long,
    val name: String,
    val standard: Standard,
    val currentType: CurrentType,
    val voltage: Double,
    val load: Double,
    val powerFactor: Double,
    val lineLength: Double,
    val ambientTemp: Double,
    val circuits: Int,
    val maxDrop: Double,
    val conductor: ConductorMaterial,
    val insulation: InsulationType,
    val installationMethodCode: String
)

object CalculationStorage {

    private const val PREFS_NAME =
        "electrical_calculations_storage"

    private const val KEY_CALCULATIONS =
        "saved_calculations"

    fun getAll(
        context: Context
    ): List<SavedCalculation> {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val raw =
            prefs.getString(
                KEY_CALCULATIONS,
                null
            ) ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            val result =
                mutableListOf<SavedCalculation>()

            for (index in 0 until array.length()) {

                val item =
                    array.getJSONObject(index)

                result +=
                    SavedCalculation(
                        id = item.optLong("id"),
                        name = item.optString(
                            "name",
                            "Calculation"
                        ),
                        standard =
                            enumValueOrDefault(
                                item.optString(
                                    "standard"
                                ),
                                Standard.IEC
                            ),
                        currentType =
                            enumValueOrDefault(
                                item.optString(
                                    "currentType"
                                ),
                                CurrentType.AlternatingSinglePhase
                            ),
                        voltage =
                            item.optDouble(
                                "voltage",
                                230.0
                            ),
                        load =
                            item.optDouble(
                                "load",
                                5000.0
                            ),
                        powerFactor =
                            item.optDouble(
                                "powerFactor",
                                0.90
                            ),
                        lineLength =
                            item.optDouble(
                                "lineLength",
                                60.0
                            ),
                        ambientTemp =
                            item.optDouble(
                                "ambientTemp",
                                30.0
                            ),
                        circuits =
                            item.optInt(
                                "circuits",
                                1
                            ),
                        maxDrop =
                            item.optDouble(
                                "maxDrop",
                                4.0
                            ),
                        conductor =
                            enumValueOrDefault(
                                item.optString(
                                    "conductor"
                                ),
                                ConductorMaterial.Copper
                            ),
                        insulation =
                            enumValueOrDefault(
                                item.optString(
                                    "insulation"
                                ),
                                InsulationType.PVC
                            ),
                        installationMethodCode =
                            item.optString(
                                "installationMethodCode"
                            )
                    )
            }

            result.sortedByDescending {
                it.id
            }

        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(
        context: Context,
        calculation: SavedCalculation
    ) {

        val existing =
            getAll(context)
                .filterNot {
                    it.id == calculation.id
                }
                .toMutableList()

        existing += calculation

        write(
            context = context,
            calculations = existing
        )
    }

    fun delete(
        context: Context,
        id: Long
    ) {

        val updated =
            getAll(context)
                .filterNot {
                    it.id == id
                }

        write(
            context = context,
            calculations = updated
        )
    }

    private fun write(
        context: Context,
        calculations: List<SavedCalculation>
    ) {

        val array =
            JSONArray()

        calculations.forEach { calculation ->

            val item =
                JSONObject()

            item.put(
                "id",
                calculation.id
            )

            item.put(
                "name",
                calculation.name
            )

            item.put(
                "standard",
                calculation.standard.name
            )

            item.put(
                "currentType",
                calculation.currentType.name
            )

            item.put(
                "voltage",
                calculation.voltage
            )

            item.put(
                "load",
                calculation.load
            )

            item.put(
                "powerFactor",
                calculation.powerFactor
            )

            item.put(
                "lineLength",
                calculation.lineLength
            )

            item.put(
                "ambientTemp",
                calculation.ambientTemp
            )

            item.put(
                "circuits",
                calculation.circuits
            )

            item.put(
                "maxDrop",
                calculation.maxDrop
            )

            item.put(
                "conductor",
                calculation.conductor.name
            )

            item.put(
                "insulation",
                calculation.insulation.name
            )

            item.put(
                "installationMethodCode",
                calculation.installationMethodCode
            )

            array.put(item)
        }

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_CALCULATIONS,
                array.toString()
            )
            .apply()
    }

    private inline fun <reified T : Enum<T>>
        enumValueOrDefault(
            value: String,
            default: T
        ): T {

        return try {
            enumValueOf<T>(value)
        } catch (_: Exception) {
            default
        }
    }
}
