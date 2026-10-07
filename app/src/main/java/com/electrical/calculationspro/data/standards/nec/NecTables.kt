package com.electrical.calculationspro.data.standards

/**
 * NEC / NFPA 70 numerical reference dataset.
 *
 * Sources:
 * - NFPA public committee material for Table 310.16.
 * - Southwire NEC installation guide.
 * - Schneider Electric NEC correction/adjustment reference.
 * - Public engineering references cross-checked against the NEC table.
 *
 * The application must supply the conductor temperature rating explicitly.
 */
object NecTables {

    const val SOURCE_STANDARD = "NFPA 70"
    const val SOURCE_EDITION = "2026"
    const val TABLE_AMPACITY = "310.16"
    const val TABLE_CORRECTION = "310.15(B)(1)"
    const val TABLE_ADJUSTMENT = "310.15(C)(1)"

    data class ConductorEntry(
        val designation: String,
        val awgOrKcmil: String,
        val areaMm2: Double,
        val copper60C: Double?,
        val copper75C: Double?,
        val copper90C: Double?,
        val aluminum60C: Double?,
        val aluminum75C: Double?,
        val aluminum90C: Double?
    )

    /*
     * NEC 310.16 nominal conductor areas and ampacities.
     *
     * Values are represented using their corresponding AWG/kcmil
     * conductor area; the designation remains authoritative.
     */
    val table31016: List<ConductorEntry> =
        listOf(
            ConductorEntry("18 AWG", "18", 0.823, null, null, 14.0, null, null, null),
            ConductorEntry("16 AWG", "16", 1.307, null, null, 18.0, null, null, null),
            ConductorEntry("14 AWG", "14", 2.081, 15.0, 20.0, 25.0, null, null, null),
            ConductorEntry("12 AWG", "12", 3.309, 20.0, 25.0, 30.0, 15.0, 20.0, 25.0),
            ConductorEntry("10 AWG", "10", 5.261, 30.0, 35.0, 40.0, 25.0, 30.0, 35.0),
            ConductorEntry("8 AWG", "8", 8.367, 40.0, 50.0, 55.0, 35.0, 40.0, 45.0),
            ConductorEntry("6 AWG", "6", 13.30, 55.0, 65.0, 75.0, 40.0, 50.0, 55.0),
            ConductorEntry("4 AWG", "4", 21.15, 70.0, 85.0, 95.0, 55.0, 65.0, 75.0),
            ConductorEntry("3 AWG", "3", 26.67, 85.0, 100.0, 115.0, 65.0, 75.0, 85.0),
            ConductorEntry("2 AWG", "2", 33.62, 95.0, 115.0, 130.0, 75.0, 90.0, 100.0),
            ConductorEntry("1 AWG", "1", 42.41, 110.0, 130.0, 145.0, 85.0, 100.0, 115.0),
            ConductorEntry("1/0 AWG", "1/0", 53.48, 125.0, 150.0, 170.0, 100.0, 120.0, 135.0),
            ConductorEntry("2/0 AWG", "2/0", 67.43, 145.0, 175.0, 195.0, 115.0, 135.0, 150.0),
            ConductorEntry("3/0 AWG", "3/0", 85.01, 165.0, 200.0, 225.0, 130.0, 155.0, 175.0),
            ConductorEntry("4/0 AWG", "4/0", 107.2, 195.0, 230.0, 260.0, 150.0, 180.0, 205.0),
            ConductorEntry("250 kcmil", "250", 126.7, 215.0, 255.0, 290.0, 170.0, 205.0, 230.0),
            ConductorEntry("300 kcmil", "300", 152.0, 240.0, 285.0, 320.0, 195.0, 230.0, 260.0),
            ConductorEntry("350 kcmil", "350", 177.3, 260.0, 310.0, 350.0, 210.0, 250.0, 280.0),
            ConductorEntry("400 kcmil", "400", 202.7, 280.0, 335.0, 380.0, 225.0, 270.0, 305.0),
            ConductorEntry("500 kcmil", "500", 253.4, 320.0, 380.0, 430.0, 260.0, 310.0, 350.0),
            ConductorEntry("600 kcmil", "600", 304.0, 350.0, 420.0, 475.0, 285.0, 340.0, 385.0),
            ConductorEntry("700 kcmil", "700", 354.7, 385.0, 460.0, 520.0, 315.0, 375.0, 425.0),
            ConductorEntry("750 kcmil", "750", 380.0, 400.0, 475.0, 535.0, 320.0, 385.0, 435.0),
            ConductorEntry("800 kcmil", "800", 405.4, 410.0, 490.0, 555.0, 330.0, 395.0, 445.0),
            ConductorEntry("900 kcmil", "900", 456.0, 435.0, 520.0, 585.0, 355.0, 425.0, 480.0),
            ConductorEntry("1000 kcmil", "1000", 506.7, 455.0, 545.0, 615.0, 375.0, 445.0, 500.0),
            ConductorEntry("1250 kcmil", "1250", 633.4, 495.0, 590.0, 665.0, 405.0, 485.0, 545.0),
            ConductorEntry("1500 kcmil", "1500", 760.1, 525.0, 625.0, 705.0, 435.0, 520.0, 585.0),
            ConductorEntry("1750 kcmil", "1750", 886.7, 545.0, 650.0, 735.0, 455.0, 545.0, 615.0),
            ConductorEntry("2000 kcmil", "2000", 1013.0, 555.0, 665.0, 750.0, 470.0, 560.0, 630.0)
        )

    fun entryForArea(
        sectionMm2: Double
    ): ConductorEntry? =
        table31016.minByOrNull {
            kotlin.math.abs(it.areaMm2 - sectionMm2)
        }?.takeIf {
            kotlin.math.abs(it.areaMm2 - sectionMm2) < 0.001
        }

    fun ampacity(
        sectionMm2: Double,
        material: com.electrical.calculationspro.data.ConductorMaterial,
        temperatureRatingC: Int
    ): Double? {

        val entry =
            entryForArea(sectionMm2)
                ?: return null

        return when (material) {
            com.electrical.calculationspro.data.ConductorMaterial.Copper ->
                when (temperatureRatingC) {
                    60 -> entry.copper60C
                    75 -> entry.copper75C
                    90 -> entry.copper90C
                    else -> null
                }

            com.electrical.calculationspro.data.ConductorMaterial.Aluminum ->
                when (temperatureRatingC) {
                    60 -> entry.aluminum60C
                    75 -> entry.aluminum75C
                    90 -> entry.aluminum90C
                    else -> null
                }
        }
    }

    fun ambientCorrectionFactor(
        ambientTemperatureC: Double,
        conductorTemperatureC: Int
    ): Double? {

        val row =
            when {
                ambientTemperatureC <= 10.0 ->
                    doubleArrayOf(1.29, 1.20, 1.15)

                ambientTemperatureC <= 15.0 ->
                    doubleArrayOf(1.22, 1.15, 1.12)

                ambientTemperatureC <= 20.0 ->
                    doubleArrayOf(1.15, 1.11, 1.08)

                ambientTemperatureC <= 25.0 ->
                    doubleArrayOf(1.08, 1.05, 1.04)

                ambientTemperatureC <= 30.0 ->
                    doubleArrayOf(1.00, 1.00, 1.00)

                ambientTemperatureC <= 35.0 ->
                    doubleArrayOf(0.91, 0.94, 0.96)

                ambientTemperatureC <= 40.0 ->
                    doubleArrayOf(0.82, 0.88, 0.91)

                ambientTemperatureC <= 45.0 ->
                    doubleArrayOf(0.71, 0.82, 0.87)

                ambientTemperatureC <= 50.0 ->
                    doubleArrayOf(0.58, 0.75, 0.82)

                ambientTemperatureC <= 55.0 ->
                    doubleArrayOf(0.41, 0.67, 0.76)

                ambientTemperatureC <= 60.0 ->
                    doubleArrayOf(Double.NaN, 0.58, 0.71)

                ambientTemperatureC <= 65.0 ->
                    doubleArrayOf(Double.NaN, 0.47, 0.65)

                ambientTemperatureC <= 70.0 ->
                    doubleArrayOf(Double.NaN, 0.33, 0.58)

                ambientTemperatureC <= 75.0 ->
                    doubleArrayOf(Double.NaN, Double.NaN, 0.50)

                ambientTemperatureC <= 80.0 ->
                    doubleArrayOf(Double.NaN, Double.NaN, 0.41)

                ambientTemperatureC <= 85.0 ->
                    doubleArrayOf(Double.NaN, Double.NaN, 0.29)

                else ->
                    return null
            }

        val index =
            when (conductorTemperatureC) {
                60 -> 0
                75 -> 1
                90 -> 2
                else -> return null
            }

        return row[index]
            .takeUnless { it.isNaN() }
    }

    fun adjustmentFactor(
        currentCarryingConductors: Int
    ): Double? =
        when {
            currentCarryingConductors <= 3 -> 1.00
            currentCarryingConductors <= 6 -> 0.80
            currentCarryingConductors <= 9 -> 0.70
            currentCarryingConductors <= 20 -> 0.50
            currentCarryingConductors <= 30 -> 0.45
            currentCarryingConductors <= 40 -> 0.40
            currentCarryingConductors >= 41 -> 0.35
            else -> null
        }

    fun standardMetricEquivalentSections(): List<Double> =
        table31016
            .map { it.areaMm2 }
            .sorted()
}
