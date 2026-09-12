package com.electrical.calculationspro.data

object IecTables {

    val pvcCopper2Loaded = mapOf(
        1.5 to mapOf("A1" to 14.5, "A2" to 14.0, "B1" to 17.5, "B2" to 16.5, "C" to 19.5, "D1" to 22.0, "D2" to 22.0),
        2.5 to mapOf("A1" to 19.5, "A2" to 18.5, "B1" to 24.0, "B2" to 23.0, "C" to 27.0, "D1" to 29.0, "D2" to 28.0),
        4.0 to mapOf("A1" to 26.0, "A2" to 25.0, "B1" to 32.0, "B2" to 30.0, "C" to 36.0, "D1" to 37.0, "D2" to 38.0),
        6.0 to mapOf("A1" to 34.0, "A2" to 32.0, "B1" to 41.0, "B2" to 38.0, "C" to 46.0, "D1" to 46.0, "D2" to 48.0),
        10.0 to mapOf("A1" to 46.0, "A2" to 43.0, "B1" to 57.0, "B2" to 52.0, "C" to 63.0, "D1" to 60.0, "D2" to 64.0),
        16.0 to mapOf("A1" to 61.0, "A2" to 57.0, "B1" to 76.0, "B2" to 69.0, "C" to 85.0, "D1" to 78.0, "D2" to 83.0),
        25.0 to mapOf("A1" to 80.0, "A2" to 75.0, "B1" to 101.0, "B2" to 90.0, "C" to 112.0, "D1" to 99.0, "D2" to 110.0),
        35.0 to mapOf("A1" to 99.0, "A2" to 92.0, "B1" to 125.0, "B2" to 111.0, "C" to 138.0, "D1" to 119.0, "D2" to 132.0),
        50.0 to mapOf("A1" to 119.0, "A2" to 110.0, "B1" to 151.0, "B2" to 133.0, "C" to 168.0, "D1" to 140.0, "D2" to 156.0),
        70.0 to mapOf("A1" to 151.0, "A2" to 139.0, "B1" to 192.0, "B2" to 168.0, "C" to 213.0, "D1" to 173.0, "D2" to 192.0),
        95.0 to mapOf("A1" to 182.0, "A2" to 167.0, "B1" to 232.0, "B2" to 201.0, "C" to 258.0, "D1" to 204.0, "D2" to 230.0),
        120.0 to mapOf("A1" to 210.0, "A2" to 192.0, "B1" to 269.0, "B2" to 232.0, "C" to 299.0, "D1" to 231.0, "D2" to 261.0),
        150.0 to mapOf("A1" to 240.0, "A2" to 219.0, "B1" to 300.0, "B2" to 258.0, "C" to 344.0, "D1" to 261.0, "D2" to 293.0),
        185.0 to mapOf("A1" to 273.0, "A2" to 248.0, "B1" to 341.0, "B2" to 294.0, "C" to 392.0, "D1" to 292.0, "D2" to 331.0),
        240.0 to mapOf("A1" to 321.0, "A2" to 291.0, "B1" to 400.0, "B2" to 344.0, "C" to 461.0, "D1" to 336.0, "D2" to 382.0),
        300.0 to mapOf("A1" to 367.0, "A2" to 334.0, "B1" to 458.0, "B2" to 394.0, "C" to 530.0, "D1" to 379.0, "D2" to 427.0)
    )

    val pvcCopper3Loaded = mapOf(
        1.5 to mapOf("A1" to 13.5, "A2" to 13.0, "B1" to 15.5, "B2" to 15.0, "C" to 17.5, "D1" to 18.0, "D2" to 19.0),
        2.5 to mapOf("A1" to 18.0, "A2" to 17.5, "B1" to 21.0, "B2" to 20.0, "C" to 24.0, "D1" to 24.0, "D2" to 24.0),
        4.0 to mapOf("A1" to 24.0, "A2" to 23.0, "B1" to 28.0, "B2" to 27.0, "C" to 32.0, "D1" to 30.0, "D2" to 33.0),
        6.0 to mapOf("A1" to 31.0, "A2" to 29.0, "B1" to 36.0, "B2" to 34.0, "C" to 41.0, "D1" to 38.0, "D2" to 41.0),
        10.0 to mapOf("A1" to 42.0, "A2" to 39.0, "B1" to 50.0, "B2" to 46.0, "C" to 57.0, "D1" to 50.0, "D2" to 54.0),
        16.0 to mapOf("A1" to 56.0, "A2" to 52.0, "B1" to 68.0, "B2" to 62.0, "C" to 76.0, "D1" to 64.0, "D2" to 70.0),
        25.0 to mapOf("A1" to 73.0, "A2" to 68.0, "B1" to 89.0, "B2" to 80.0, "C" to 96.0, "D1" to 82.0, "D2" to 92.0),
        35.0 to mapOf("A1" to 89.0, "A2" to 83.0, "B1" to 110.0, "B2" to 99.0, "C" to 119.0, "D1" to 98.0, "D2" to 110.0),
        50.0 to mapOf("A1" to 108.0, "A2" to 99.0, "B1" to 134.0, "B2" to 118.0, "C" to 144.0, "D1" to 116.0, "D2" to 130.0),
        70.0 to mapOf("A1" to 136.0, "A2" to 125.0, "B1" to 171.0, "B2" to 149.0, "C" to 184.0, "D1" to 143.0, "D2" to 162.0),
        95.0 to mapOf("A1" to 164.0, "A2" to 150.0, "B1" to 207.0, "B2" to 179.0, "C" to 223.0, "D1" to 169.0, "D2" to 193.0),
        120.0 to mapOf("A1" to 188.0, "A2" to 172.0, "B1" to 239.0, "B2" to 206.0, "C" to 259.0, "D1" to 192.0, "D2" to 220.0),
        150.0 to mapOf("A1" to 216.0, "A2" to 196.0, "B1" to 262.0, "B2" to 225.0, "C" to 299.0, "D1" to 217.0, "D2" to 246.0),
        185.0 to mapOf("A1" to 245.0, "A2" to 223.0, "B1" to 296.0, "B2" to 255.0, "C" to 341.0, "D1" to 243.0, "D2" to 278.0),
        240.0 to mapOf("A1" to 286.0, "A2" to 261.0, "B1" to 346.0, "B2" to 297.0, "C" to 403.0, "D1" to 280.0, "D2" to 320.0),
        300.0 to mapOf("A1" to 328.0, "A2" to 298.0, "B1" to 394.0, "B2" to 339.0, "C" to 464.0, "D1" to 316.0, "D2" to 359.0)
    )

    val pvcAluminium2Loaded = mapOf(
        2.5 to mapOf("A1" to 15.0, "A2" to 14.5, "B1" to 18.5, "B2" to 17.5, "C" to 21.0, "D1" to 22.0, "D2" to 22.0),
        4.0 to mapOf("A1" to 20.0, "A2" to 19.5, "B1" to 25.0, "B2" to 24.0, "C" to 28.0, "D1" to 29.0, "D2" to 29.0),
        6.0 to mapOf("A1" to 26.0, "A2" to 25.0, "B1" to 32.0, "B2" to 30.0, "C" to 36.0, "D1" to 36.0, "D2" to 36.0),
        10.0 to mapOf("A1" to 36.0, "A2" to 33.0, "B1" to 44.0, "B2" to 41.0, "C" to 49.0, "D1" to 47.0, "D2" to 47.0),
        16.0 to mapOf("A1" to 48.0, "A2" to 44.0, "B1" to 60.0, "B2" to 54.0, "C" to 66.0, "D1" to 61.0, "D2" to 63.0),
        25.0 to mapOf("A1" to 63.0, "A2" to 58.0, "B1" to 79.0, "B2" to 71.0, "C" to 83.0, "D1" to 77.0, "D2" to 82.0),
        35.0 to mapOf("A1" to 77.0, "A2" to 71.0, "B1" to 97.0, "B2" to 86.0, "C" to 103.0, "D1" to 93.0, "D2" to 98.0),
        50.0 to mapOf("A1" to 93.0, "A2" to 86.0, "B1" to 118.0, "B2" to 104.0, "C" to 125.0, "D1" to 109.0, "D2" to 117.0),
        70.0 to mapOf("A1" to 118.0, "A2" to 108.0, "B1" to 150.0, "B2" to 131.0, "C" to 160.0, "D1" to 135.0, "D2" to 145.0),
        95.0 to mapOf("A1" to 142.0, "A2" to 130.0, "B1" to 181.0, "B2" to 157.0, "C" to 195.0, "D1" to 159.0, "D2" to 173.0),
        120.0 to mapOf("A1" to 164.0, "A2" to 150.0, "B1" to 210.0, "B2" to 181.0, "C" to 226.0, "D1" to 180.0, "D2" to 200.0),
        150.0 to mapOf("A1" to 189.0, "A2" to 172.0, "B1" to 234.0, "B2" to 201.0, "C" to 261.0, "D1" to 204.0, "D2" to 224.0),
        185.0 to mapOf("A1" to 215.0, "A2" to 195.0, "B1" to 266.0, "B2" to 230.0, "C" to 298.0, "D1" to 228.0, "D2" to 255.0),
        240.0 to mapOf("A1" to 252.0, "A2" to 229.0, "B1" to 312.0, "B2" to 269.0, "C" to 352.0, "D1" to 262.0, "D2" to 298.0),
        300.0 to mapOf("A1" to 289.0, "A2" to 263.0, "B1" to 358.0, "B2" to 308.0, "C" to 406.0, "D1" to 296.0, "D2" to 336.0)
    )

    val pvcAluminium3Loaded = mapOf(
        2.5 to mapOf("A1" to 14.0, "A2" to 13.5, "B1" to 16.5, "B2" to 15.5, "C" to 18.5, "D1" to 18.5, "D2" to 18.5),
        4.0 to mapOf("A1" to 18.5, "A2" to 17.5, "B1" to 22.0, "B2" to 21.0, "C" to 25.0, "D1" to 24.0, "D2" to 24.0),
        6.0 to mapOf("A1" to 24.0, "A2" to 23.0, "B1" to 28.0, "B2" to 27.0, "C" to 32.0, "D1" to 30.0, "D2" to 30.0),
        10.0 to mapOf("A1" to 32.0, "A2" to 31.0, "B1" to 39.0, "B2" to 36.0, "C" to 44.0, "D1" to 40.0, "D2" to 41.0),
        16.0 to mapOf("A1" to 43.0, "A2" to 41.0, "B1" to 53.0, "B2" to 48.0, "C" to 59.0, "D1" to 53.0, "D2" to 54.0),
        25.0 to mapOf("A1" to 57.0, "A2" to 53.0, "B1" to 70.0, "B2" to 62.0, "C" to 73.0, "D1" to 67.0, "D2" to 69.0),
        35.0 to mapOf("A1" to 70.0, "A2" to 65.0, "B1" to 86.0, "B2" to 77.0, "C" to 90.0, "D1" to 80.0, "D2" to 83.0),
        50.0 to mapOf("A1" to 84.0, "A2" to 78.0, "B1" to 104.0, "B2" to 92.0, "C" to 110.0, "D1" to 94.0, "D2" to 99.0),
        70.0 to mapOf("A1" to 107.0, "A2" to 98.0, "B1" to 133.0, "B2" to 116.0, "C" to 140.0, "D1" to 116.0, "D2" to 122.0),
        95.0 to mapOf("A1" to 129.0, "A2" to 118.0, "B1" to 161.0, "B2" to 139.0, "C" to 170.0, "D1" to 137.0, "D2" to 148.0),
        120.0 to mapOf("A1" to 149.0, "A2" to 135.0, "B1" to 186.0, "B2" to 160.0, "C" to 197.0, "D1" to 155.0, "D2" to 169.0),
        150.0 to mapOf("A1" to 170.0, "A2" to 155.0, "B1" to 204.0, "B2" to 176.0, "C" to 227.0, "D1" to 175.0, "D2" to 189.0),
        185.0 to mapOf("A1" to 194.0, "A2" to 176.0, "B1" to 230.0, "B2" to 199.0, "C" to 259.0, "D1" to 195.0, "D2" to 214.0),
        240.0 to mapOf("A1" to 227.0, "A2" to 207.0, "B1" to 269.0, "B2" to 232.0, "C" to 305.0, "D1" to 225.0, "D2" to 250.0),
        300.0 to mapOf("A1" to 261.0, "A2" to 237.0, "B1" to 306.0, "B2" to 265.0, "C" to 351.0, "D1" to 254.0, "D2" to 282.0)
    )

    fun ambientCorrectionPvc(ambientTemp: Double): Double {
        return when {
            ambientTemp <= 10 -> 1.22
            ambientTemp <= 15 -> 1.17
            ambientTemp <= 20 -> 1.12
            ambientTemp <= 25 -> 1.06
            ambientTemp <= 30 -> 1.00
            ambientTemp <= 35 -> 0.94
            ambientTemp <= 40 -> 0.87
            ambientTemp <= 45 -> 0.79
            ambientTemp <= 50 -> 0.71
            ambientTemp <= 55 -> 0.61
            ambientTemp <= 60 -> 0.50
            else -> 0.40
        }
    }

    fun ambientCorrectionXlpe(ambientTemp: Double): Double {
        return when {
            ambientTemp <= 10 -> 1.15
            ambientTemp <= 15 -> 1.12
            ambientTemp <= 20 -> 1.08
            ambientTemp <= 25 -> 1.04
            ambientTemp <= 30 -> 1.00
            ambientTemp <= 35 -> 0.96
            ambientTemp <= 40 -> 0.91
            ambientTemp <= 45 -> 0.87
            ambientTemp <= 50 -> 0.82
            ambientTemp <= 55 -> 0.76
            ambientTemp <= 60 -> 0.71
            ambientTemp <= 65 -> 0.65
            ambientTemp <= 70 -> 0.58
            ambientTemp <= 75 -> 0.50
            ambientTemp <= 80 -> 0.41
            else -> 0.35
        }
    }

    fun groupingFactor(numberOfCircuits: Int): Double {
        return when {
            numberOfCircuits <= 1 -> 1.00
            numberOfCircuits == 2 -> 0.80
            numberOfCircuits == 3 -> 0.70
            numberOfCircuits == 4 -> 0.65
            numberOfCircuits == 5 -> 0.60
            numberOfCircuits == 6 -> 0.57
            numberOfCircuits <= 7 -> 0.54
            numberOfCircuits <= 8 -> 0.52
            numberOfCircuits <= 9 -> 0.50
            numberOfCircuits <= 12 -> 0.45
            numberOfCircuits <= 16 -> 0.41
            numberOfCircuits <= 20 -> 0.38
            else -> 0.35
        }
    }

    fun getBaseAmpacity(
        section: Double,
        method: String,
        loadedConductors: Int = 2,
        material: ConductorMaterial = ConductorMaterial.Copper,
        insulation: InsulationType = InsulationType.PVC
    ): Double {
        val table = when {
            material == ConductorMaterial.Copper && loadedConductors >= 3 -> pvcCopper3Loaded
            material == ConductorMaterial.Copper -> pvcCopper2Loaded
            material == ConductorMaterial.Aluminum && loadedConductors >= 3 -> pvcAluminium3Loaded
            else -> pvcAluminium2Loaded
        }
        val row = table[section] ?: return if (material == ConductorMaterial.Copper) section * 6.5 else section * 5.0
        return row[method] ?: row["B1"] ?: (if (material == ConductorMaterial.Copper) section * 6.5 else section * 5.0)
    }

    fun methodToKey(methodCode: String): String {
        return when {
            methodCode.contains("A1") -> "A1"
            methodCode.contains("A2") -> "A2"
            methodCode.contains("B1") -> "B1"
            methodCode.contains("B2") -> "B2"
            methodCode.contains("C") -> "C"
            methodCode.contains("D1") -> "D1"
            methodCode.contains("D2") -> "D2"
            methodCode.contains("E") -> "C"
            methodCode.contains("F") -> "C"
            methodCode.contains("G") -> "C"
            else -> "B1"
        }
    }

    val allInstallationMethods = listOf(
        InstallationMethod("1 - A1", "Insulated conductors in conduit in thermally insulated wall"),
        InstallationMethod("1 - A2", "Multi-core cable in conduit in thermally insulated wall"),
        InstallationMethod("2 - B1", "Insulated conductors in conduit on wall"),
        InstallationMethod("2 - B2", "Multi-core cable in conduit on wall"),
        InstallationMethod("3 - C", "Multi-core cable on wall / clipped direct"),
        InstallationMethod("4 - D1", "Multi-core cable in ducts in the ground"),
        InstallationMethod("4 - D2", "Multi-core cable direct in the ground"),
        InstallationMethod("5 - E", "Multi-core cable in free air"),
        InstallationMethod("6 - F", "Single-core cables on perforated tray"),
        InstallationMethod("7 - G", "Single-core cables on ladder / supports")
    )
}
