package com.electrical.calculationspro.data

/**
 * Controlled IEC 60364-5-52 engineering datasets.
 *
 * Sources used for the populated numerical datasets:
 * - IEC 60364-5-52 Table B.52.3
 * - IEC 60364-5-52 Table B.52.4
 * - IEC 60364-5-52 Table B.52.5
 * - Schneider Electric Electrical Installation Guide
 * - TiSoft IEC 60364-5-52 engineering tables
 *
 * No unsupported combination is replaced by a guessed value.
 * A missing value returns null.
 */
object IecTables {

    const val SOURCE_STANDARD = "IEC 60364-5-52"
    const val SOURCE_EDITION = "Project controlled IEC dataset"

    const val SOURCE_PRIMARY_URL =
        "https://www.electrical-installation.org/enwiki/General_method_for_cable_sizing"

    const val SOURCE_XLPE_URL =
        "https://www.ti-soft.com/en/support/help/electricaldesign/standards/iec-60364-5-52/current-carrying-capacity/table_b_52_5"

    private val installationMethods =
        setOf(
            "A1",
            "A2",
            "B1",
            "B2",
            "C",
            "D1",
            "D2"
        )

    /*
     * IEC 60364-5-52 Table B.52.3
     * XLPE/EPR, two loaded conductors.
     */
    val xlpeCopper2Loaded = mapOf(
        1.5 to mapOf(
            "A1" to 19.0,
            "A2" to 18.5,
            "B1" to 23.0,
            "B2" to 22.0,
            "C" to 24.0,
            "D1" to 25.0,
            "D2" to 27.0
        ),
        2.5 to mapOf(
            "A1" to 26.0,
            "A2" to 25.0,
            "B1" to 31.0,
            "B2" to 30.0,
            "C" to 33.0,
            "D1" to 33.0,
            "D2" to 35.0
        ),
        4.0 to mapOf(
            "A1" to 35.0,
            "A2" to 33.0,
            "B1" to 42.0,
            "B2" to 40.0,
            "C" to 45.0,
            "D1" to 43.0,
            "D2" to 46.0
        ),
        6.0 to mapOf(
            "A1" to 45.0,
            "A2" to 42.0,
            "B1" to 54.0,
            "B2" to 51.0,
            "C" to 58.0,
            "D1" to 53.0,
            "D2" to 58.0
        ),
        10.0 to mapOf(
            "A1" to 61.0,
            "A2" to 57.0,
            "B1" to 75.0,
            "B2" to 69.0,
            "C" to 80.0,
            "D1" to 71.0,
            "D2" to 77.0
        ),
        16.0 to mapOf(
            "A1" to 81.0,
            "A2" to 76.0,
            "B1" to 100.0,
            "B2" to 91.0,
            "C" to 107.0,
            "D1" to 91.0,
            "D2" to 100.0
        ),
        25.0 to mapOf(
            "A1" to 106.0,
            "A2" to 99.0,
            "B1" to 133.0,
            "B2" to 119.0,
            "C" to 138.0,
            "D1" to 116.0,
            "D2" to 129.0
        ),
        35.0 to mapOf(
            "A1" to 131.0,
            "A2" to 121.0,
            "B1" to 164.0,
            "B2" to 146.0,
            "C" to 171.0,
            "D1" to 139.0,
            "D2" to 155.0
        ),
        50.0 to mapOf(
            "A1" to 158.0,
            "A2" to 145.0,
            "B1" to 198.0,
            "B2" to 175.0,
            "C" to 209.0,
            "D1" to 164.0,
            "D2" to 183.0
        ),
        70.0 to mapOf(
            "A1" to 200.0,
            "A2" to 183.0,
            "B1" to 253.0,
            "B2" to 221.0,
            "C" to 269.0,
            "D1" to 203.0,
            "D2" to 225.0
        ),
        95.0 to mapOf(
            "A1" to 241.0,
            "A2" to 220.0,
            "B1" to 306.0,
            "B2" to 265.0,
            "C" to 328.0,
            "D1" to 239.0,
            "D2" to 270.0
        ),
        120.0 to mapOf(
            "A1" to 278.0,
            "A2" to 253.0,
            "B1" to 354.0,
            "B2" to 305.0,
            "C" to 382.0,
            "D1" to 271.0,
            "D2" to 306.0
        ),
        150.0 to mapOf(
            "A1" to 318.0,
            "A2" to 290.0,
            "B1" to 393.0,
            "B2" to 334.0,
            "C" to 441.0,
            "D1" to 306.0,
            "D2" to 343.0
        ),
        185.0 to mapOf(
            "A1" to 362.0,
            "A2" to 329.0,
            "B1" to 449.0,
            "B2" to 384.0,
            "C" to 506.0,
            "D1" to 343.0,
            "D2" to 387.0
        ),
        240.0 to mapOf(
            "A1" to 424.0,
            "A2" to 386.0,
            "B1" to 528.0,
            "B2" to 459.0,
            "C" to 599.0,
            "D1" to 395.0,
            "D2" to 448.0
        ),
        300.0 to mapOf(
            "A1" to 486.0,
            "A2" to 442.0,
            "B1" to 603.0,
            "B2" to 532.0,
            "C" to 693.0,
            "D1" to 446.0,
            "D2" to 502.0
        )
    )

    val xlpeAluminium2Loaded = mapOf(
        2.5 to mapOf(
            "A1" to 20.0,
            "A2" to 19.5,
            "B1" to 25.0,
            "B2" to 23.0,
            "C" to 26.0,
            "D1" to 26.0,
            "D2" to 28.0
        ),
        4.0 to mapOf(
            "A1" to 27.0,
            "A2" to 26.0,
            "B1" to 33.0,
            "B2" to 31.0,
            "C" to 35.0,
            "D1" to 33.0,
            "D2" to 36.0
        ),
        6.0 to mapOf(
            "A1" to 35.0,
            "A2" to 33.0,
            "B1" to 43.0,
            "B2" to 40.0,
            "C" to 45.0,
            "D1" to 42.0,
            "D2" to 47.0
        ),
        10.0 to mapOf(
            "A1" to 48.0,
            "A2" to 45.0,
            "B1" to 59.0,
            "B2" to 54.0,
            "C" to 62.0,
            "D1" to 55.0,
            "D2" to 62.0
        ),
        16.0 to mapOf(
            "A1" to 64.0,
            "A2" to 60.0,
            "B1" to 79.0,
            "B2" to 72.0,
            "C" to 84.0,
            "D1" to 71.0,
            "D2" to 76.0
        ),
        25.0 to mapOf(
            "A1" to 84.0,
            "A2" to 78.0,
            "B1" to 105.0,
            "B2" to 94.0,
            "C" to 101.0,
            "D1" to 90.0,
            "D2" to 98.0
        ),
        35.0 to mapOf(
            "A1" to 103.0,
            "A2" to 96.0,
            "B1" to 130.0,
            "B2" to 115.0,
            "C" to 126.0,
            "D1" to 108.0,
            "D2" to 117.0
        ),
        50.0 to mapOf(
            "A1" to 125.0,
            "A2" to 115.0,
            "B1" to 157.0,
            "B2" to 138.0,
            "C" to 154.0,
            "D1" to 128.0,
            "D2" to 139.0
        ),
        70.0 to mapOf(
            "A1" to 158.0,
            "A2" to 145.0,
            "B1" to 200.0,
            "B2" to 175.0,
            "C" to 198.0,
            "D1" to 158.0,
            "D2" to 170.0
        ),
        95.0 to mapOf(
            "A1" to 191.0,
            "A2" to 175.0,
            "B1" to 242.0,
            "B2" to 210.0,
            "C" to 241.0,
            "D1" to 186.0,
            "D2" to 204.0
        ),
        120.0 to mapOf(
            "A1" to 220.0,
            "A2" to 201.0,
            "B1" to 281.0,
            "B2" to 242.0,
            "C" to 280.0,
            "D1" to 211.0,
            "D2" to 233.0
        ),
        150.0 to mapOf(
            "A1" to 253.0,
            "A2" to 230.0,
            "B1" to 307.0,
            "B2" to 261.0,
            "C" to 324.0,
            "D1" to 238.0,
            "D2" to 261.0
        ),
        185.0 to mapOf(
            "A1" to 288.0,
            "A2" to 262.0,
            "B1" to 351.0,
            "B2" to 300.0,
            "C" to 371.0,
            "D1" to 267.0,
            "D2" to 296.0
        ),
        240.0 to mapOf(
            "A1" to 338.0,
            "A2" to 307.0,
            "B1" to 412.0,
            "B2" to 358.0,
            "C" to 439.0,
            "D1" to 307.0,
            "D2" to 343.0
        ),
        300.0 to mapOf(
            "A1" to 387.0,
            "A2" to 352.0,
            "B1" to 471.0,
            "B2" to 415.0,
            "C" to 508.0,
            "D1" to 346.0,
            "D2" to 386.0
        )
    )

    /*
     * IEC 60364-5-52 Table B.52.4
     * PVC, two loaded conductors.
     */
    val pvcCopper2Loaded = mapOf(
        1.5 to mapOf(
            "A1" to 14.5,
            "A2" to 14.0,
            "B1" to 17.5,
            "B2" to 16.5,
            "C" to 19.5,
            "D1" to 22.0,
            "D2" to 22.0
        ),
        2.5 to mapOf(
            "A1" to 19.5,
            "A2" to 18.5,
            "B1" to 24.0,
            "B2" to 23.0,
            "C" to 27.0,
            "D1" to 29.0,
            "D2" to 28.0
        ),
        4.0 to mapOf(
            "A1" to 26.0,
            "A2" to 25.0,
            "B1" to 32.0,
            "B2" to 30.0,
            "C" to 36.0,
            "D1" to 37.0,
            "D2" to 38.0
        ),
        6.0 to mapOf(
            "A1" to 34.0,
            "A2" to 32.0,
            "B1" to 41.0,
            "B2" to 38.0,
            "C" to 46.0,
            "D1" to 46.0,
            "D2" to 48.0
        ),
        10.0 to mapOf(
            "A1" to 46.0,
            "A2" to 43.0,
            "B1" to 57.0,
            "B2" to 52.0,
            "C" to 63.0,
            "D1" to 60.0,
            "D2" to 64.0
        ),
        16.0 to mapOf(
            "A1" to 61.0,
            "A2" to 57.0,
            "B1" to 76.0,
            "B2" to 69.0,
            "C" to 85.0,
            "D1" to 78.0,
            "D2" to 83.0
        ),
        25.0 to mapOf(
            "A1" to 80.0,
            "A2" to 75.0,
            "B1" to 101.0,
            "B2" to 90.0,
            "C" to 112.0,
            "D1" to 99.0,
            "D2" to 110.0
        ),
        35.0 to mapOf(
            "A1" to 99.0,
            "A2" to 92.0,
            "B1" to 125.0,
            "B2" to 111.0,
            "C" to 138.0,
            "D1" to 119.0,
            "D2" to 132.0
        ),
        50.0 to mapOf(
            "A1" to 119.0,
            "A2" to 110.0,
            "B1" to 151.0,
            "B2" to 133.0,
            "C" to 168.0,
            "D1" to 140.0,
            "D2" to 156.0
        ),
        70.0 to mapOf(
            "A1" to 151.0,
            "A2" to 139.0,
            "B1" to 192.0,
            "B2" to 168.0,
            "C" to 213.0,
            "D1" to 173.0,
            "D2" to 192.0
        ),
        95.0 to mapOf(
            "A1" to 182.0,
            "A2" to 167.0,
            "B1" to 232.0,
            "B2" to 201.0,
            "C" to 258.0,
            "D1" to 204.0,
            "D2" to 230.0
        ),
        120.0 to mapOf(
            "A1" to 210.0,
            "A2" to 192.0,
            "B1" to 269.0,
            "B2" to 232.0,
            "C" to 299.0,
            "D1" to 231.0,
            "D2" to 261.0
        ),
        150.0 to mapOf(
            "A1" to 240.0,
            "A2" to 219.0,
            "B1" to 300.0,
            "B2" to 258.0,
            "C" to 344.0,
            "D1" to 261.0,
            "D2" to 293.0
        ),
        185.0 to mapOf(
            "A1" to 273.0,
            "A2" to 248.0,
            "B1" to 341.0,
            "B2" to 294.0,
            "C" to 392.0,
            "D1" to 292.0,
            "D2" to 331.0
        ),
        240.0 to mapOf(
            "A1" to 321.0,
            "A2" to 291.0,
            "B1" to 400.0,
            "B2" to 344.0,
            "C" to 461.0,
            "D1" to 336.0,
            "D2" to 382.0
        ),
        300.0 to mapOf(
            "A1" to 367.0,
            "A2" to 334.0,
            "B1" to 458.0,
            "B2" to 394.0,
            "C" to 530.0,
            "D1" to 379.0,
            "D2" to 427.0
        )
    )

    /*
     * IEC 60364-5-52 Table B.52.4
     * PVC, three loaded conductors.
     */
    val pvcCopper3Loaded = mapOf(
        1.5 to mapOf(
            "A1" to 13.5,
            "A2" to 13.0,
            "B1" to 15.5,
            "B2" to 15.0,
            "C" to 17.5,
            "D1" to 18.0,
            "D2" to 19.0
        ),
        2.5 to mapOf(
            "A1" to 18.0,
            "A2" to 17.5,
            "B1" to 21.0,
            "B2" to 20.0,
            "C" to 24.0,
            "D1" to 24.0,
            "D2" to 24.0
        ),
        4.0 to mapOf(
            "A1" to 24.0,
            "A2" to 23.0,
            "B1" to 28.0,
            "B2" to 27.0,
            "C" to 32.0,
            "D1" to 30.0,
            "D2" to 33.0
        ),
        6.0 to mapOf(
            "A1" to 31.0,
            "A2" to 29.0,
            "B1" to 36.0,
            "B2" to 34.0,
            "C" to 41.0,
            "D1" to 38.0,
            "D2" to 41.0
        ),
        10.0 to mapOf(
            "A1" to 42.0,
            "A2" to 39.0,
            "B1" to 50.0,
            "B2" to 46.0,
            "C" to 57.0,
            "D1" to 50.0,
            "D2" to 54.0
        ),
        16.0 to mapOf(
            "A1" to 56.0,
            "A2" to 52.0,
            "B1" to 68.0,
            "B2" to 62.0,
            "C" to 76.0,
            "D1" to 64.0,
            "D2" to 70.0
        ),
        25.0 to mapOf(
            "A1" to 73.0,
            "A2" to 68.0,
            "B1" to 89.0,
            "B2" to 80.0,
            "C" to 96.0,
            "D1" to 82.0,
            "D2" to 92.0
        ),
        35.0 to mapOf(
            "A1" to 89.0,
            "A2" to 83.0,
            "B1" to 110.0,
            "B2" to 99.0,
            "C" to 119.0,
            "D1" to 98.0,
            "D2" to 110.0
        ),
        50.0 to mapOf(
            "A1" to 108.0,
            "A2" to 99.0,
            "B1" to 134.0,
            "B2" to 118.0,
            "C" to 144.0,
            "D1" to 116.0,
            "D2" to 130.0
        ),
        70.0 to mapOf(
            "A1" to 136.0,
            "A2" to 125.0,
            "B1" to 171.0,
            "B2" to 149.0,
            "C" to 184.0,
            "D1" to 143.0,
            "D2" to 162.0
        ),
        95.0 to mapOf(
            "A1" to 164.0,
            "A2" to 150.0,
            "B1" to 207.0,
            "B2" to 179.0,
            "C" to 223.0,
            "D1" to 169.0,
            "D2" to 193.0
        ),
        120.0 to mapOf(
            "A1" to 188.0,
            "A2" to 172.0,
            "B1" to 239.0,
            "B2" to 206.0,
            "C" to 259.0,
            "D1" to 192.0,
            "D2" to 220.0
        ),
        150.0 to mapOf(
            "A1" to 216.0,
            "A2" to 196.0,
            "B1" to 262.0,
            "B2" to 225.0,
            "C" to 299.0,
            "D1" to 217.0,
            "D2" to 246.0
        ),
        185.0 to mapOf(
            "A1" to 245.0,
            "A2" to 223.0,
            "B1" to 296.0,
            "B2" to 255.0,
            "C" to 341.0,
            "D1" to 243.0,
            "D2" to 278.0
        ),
        240.0 to mapOf(
            "A1" to 286.0,
            "A2" to 261.0,
            "B1" to 346.0,
            "B2" to 297.0,
            "C" to 403.0,
            "D1" to 280.0,
            "D2" to 320.0
        ),
        300.0 to mapOf(
            "A1" to 328.0,
            "A2" to 298.0,
            "B1" to 394.0,
            "B2" to 339.0,
            "C" to 464.0,
            "D1" to 316.0,
            "D2" to 359.0
        )
    )

    val pvcAluminium2Loaded = mapOf(
        2.5 to mapOf(
            "A1" to 15.0,
            "A2" to 14.5,
            "B1" to 18.5,
            "B2" to 17.5,
            "C" to 21.0,
            "D1" to 22.0,
            "D2" to 22.0
        ),
        4.0 to mapOf(
            "A1" to 20.0,
            "A2" to 19.5,
            "B1" to 25.0,
            "B2" to 24.0,
            "C" to 28.0,
            "D1" to 29.0,
            "D2" to 29.0
        ),
        6.0 to mapOf(
            "A1" to 26.0,
            "A2" to 25.0,
            "B1" to 32.0,
            "B2" to 30.0,
            "C" to 36.0,
            "D1" to 36.0,
            "D2" to 36.0
        ),
        10.0 to mapOf(
            "A1" to 36.0,
            "A2" to 33.0,
            "B1" to 44.0,
            "B2" to 41.0,
            "C" to 49.0,
            "D1" to 47.0,
            "D2" to 47.0
        ),
        16.0 to mapOf(
            "A1" to 48.0,
            "A2" to 44.0,
            "B1" to 60.0,
            "B2" to 54.0,
            "C" to 66.0,
            "D1" to 61.0,
            "D2" to 63.0
        ),
        25.0 to mapOf(
            "A1" to 63.0,
            "A2" to 58.0,
            "B1" to 79.0,
            "B2" to 71.0,
            "C" to 83.0,
            "D1" to 77.0,
            "D2" to 82.0
        ),
        35.0 to mapOf(
            "A1" to 77.0,
            "A2" to 71.0,
            "B1" to 97.0,
            "B2" to 86.0,
            "C" to 103.0,
            "D1" to 93.0,
            "D2" to 98.0
        ),
        50.0 to mapOf(
            "A1" to 93.0,
            "A2" to 86.0,
            "B1" to 118.0,
            "B2" to 104.0,
            "C" to 125.0,
            "D1" to 109.0,
            "D2" to 117.0
        ),
        70.0 to mapOf(
            "A1" to 118.0,
            "A2" to 108.0,
            "B1" to 150.0,
            "B2" to 131.0,
            "C" to 160.0,
            "D1" to 135.0,
            "D2" to 145.0
        ),
        95.0 to mapOf(
            "A1" to 142.0,
            "A2" to 130.0,
            "B1" to 181.0,
            "B2" to 157.0,
            "C" to 195.0,
            "D1" to 159.0,
            "D2" to 173.0
        ),
        120.0 to mapOf(
            "A1" to 164.0,
            "A2" to 150.0,
            "B1" to 210.0,
            "B2" to 181.0,
            "C" to 226.0,
            "D1" to 180.0,
            "D2" to 200.0
        ),
        150.0 to mapOf(
            "A1" to 189.0,
            "A2" to 172.0,
            "B1" to 234.0,
            "B2" to 201.0,
            "C" to 261.0,
            "D1" to 204.0,
            "D2" to 224.0
        ),
        185.0 to mapOf(
            "A1" to 215.0,
            "A2" to 195.0,
            "B1" to 266.0,
            "B2" to 230.0,
            "C" to 298.0,
            "D1" to 228.0,
            "D2" to 255.0
        ),
        240.0 to mapOf(
            "A1" to 252.0,
            "A2" to 229.0,
            "B1" to 312.0,
            "B2" to 269.0,
            "C" to 352.0,
            "D1" to 262.0,
            "D2" to 298.0
        ),
        300.0 to mapOf(
            "A1" to 289.0,
            "A2" to 263.0,
            "B1" to 358.0,
            "B2" to 308.0,
            "C" to 406.0,
            "D1" to 296.0,
            "D2" to 336.0
        )
    )

    val pvcAluminium3Loaded = mapOf(
        2.5 to mapOf(
            "A1" to 14.0,
            "A2" to 13.5,
            "B1" to 16.5,
            "B2" to 15.5,
            "C" to 18.5,
            "D1" to 18.5,
            "D2" to 18.5
        ),
        4.0 to mapOf(
            "A1" to 18.5,
            "A2" to 17.5,
            "B1" to 22.0,
            "B2" to 21.0,
            "C" to 25.0,
            "D1" to 24.0,
            "D2" to 24.0
        ),
        6.0 to mapOf(
            "A1" to 24.0,
            "A2" to 23.0,
            "B1" to 28.0,
            "B2" to 27.0,
            "C" to 32.0,
            "D1" to 30.0,
            "D2" to 30.0
        ),
        10.0 to mapOf(
            "A1" to 32.0,
            "A2" to 31.0,
            "B1" to 39.0,
            "B2" to 36.0,
            "C" to 44.0,
            "D1" to 40.0,
            "D2" to 41.0
        ),
        16.0 to mapOf(
            "A1" to 43.0,
            "A2" to 41.0,
            "B1" to 53.0,
            "B2" to 48.0,
            "C" to 59.0,
            "D1" to 53.0,
            "D2" to 54.0
        ),
        25.0 to mapOf(
            "A1" to 57.0,
            "A2" to 53.0,
            "B1" to 70.0,
            "B2" to 62.0,
            "C" to 73.0,
            "D1" to 67.0,
            "D2" to 69.0
        ),
        35.0 to mapOf(
            "A1" to 70.0,
            "A2" to 65.0,
            "B1" to 86.0,
            "B2" to 77.0,
            "C" to 90.0,
            "D1" to 80.0,
            "D2" to 83.0
        ),
        50.0 to mapOf(
            "A1" to 84.0,
            "A2" to 78.0,
            "B1" to 104.0,
            "B2" to 92.0,
            "C" to 110.0,
            "D1" to 94.0,
            "D2" to 99.0
        ),
        70.0 to mapOf(
            "A1" to 107.0,
            "A2" to 98.0,
            "B1" to 133.0,
            "B2" to 116.0,
            "C" to 140.0,
            "D1" to 116.0,
            "D2" to 122.0
        ),
        95.0 to mapOf(
            "A1" to 129.0,
            "A2" to 118.0,
            "B1" to 161.0,
            "B2" to 139.0,
            "C" to 170.0,
            "D1" to 137.0,
            "D2" to 148.0
        ),
        120.0 to mapOf(
            "A1" to 149.0,
            "A2" to 135.0,
            "B1" to 186.0,
            "B2" to 160.0,
            "C" to 197.0,
            "D1" to 155.0,
            "D2" to 169.0
        ),
        150.0 to mapOf(
            "A1" to 170.0,
            "A2" to 155.0,
            "B1" to 204.0,
            "B2" to 176.0,
            "C" to 227.0,
            "D1" to 175.0,
            "D2" to 189.0
        ),
        185.0 to mapOf(
            "A1" to 194.0,
            "A2" to 176.0,
            "B1" to 230.0,
            "B2" to 199.0,
            "C" to 259.0,
            "D1" to 195.0,
            "D2" to 214.0
        ),
        240.0 to mapOf(
            "A1" to 227.0,
            "A2" to 207.0,
            "B1" to 269.0,
            "B2" to 232.0,
            "C" to 305.0,
            "D1" to 225.0,
            "D2" to 250.0
        ),
        300.0 to mapOf(
            "A1" to 261.0,
            "A2" to 237.0,
            "B1" to 306.0,
            "B2" to 265.0,
            "C" to 351.0,
            "D1" to 254.0,
            "D2" to 282.0
        )
    )

    /*
     * IEC ambient correction factors.
     *
     * Values outside the published range return null.
     * No extrapolation is performed.
     */
    fun ambientCorrectionPvc(
        ambientTemp: Double
    ): Double? =
        when {
            ambientTemp <= 10.0 -> 1.22
            ambientTemp <= 15.0 -> 1.17
            ambientTemp <= 20.0 -> 1.12
            ambientTemp <= 25.0 -> 1.06
            ambientTemp <= 30.0 -> 1.00
            ambientTemp <= 35.0 -> 0.94
            ambientTemp <= 40.0 -> 0.87
            ambientTemp <= 45.0 -> 0.79
            ambientTemp <= 50.0 -> 0.71
            ambientTemp <= 55.0 -> 0.61
            ambientTemp <= 60.0 -> 0.50
            else -> null
        }

    fun ambientCorrectionXlpe(
        ambientTemp: Double
    ): Double? =
        when {
            ambientTemp <= 10.0 -> 1.15
            ambientTemp <= 15.0 -> 1.12
            ambientTemp <= 20.0 -> 1.08
            ambientTemp <= 25.0 -> 1.04
            ambientTemp <= 30.0 -> 1.00
            ambientTemp <= 35.0 -> 0.96
            ambientTemp <= 40.0 -> 0.91
            ambientTemp <= 45.0 -> 0.87
            ambientTemp <= 50.0 -> 0.82
            ambientTemp <= 55.0 -> 0.76
            ambientTemp <= 60.0 -> 0.71
            ambientTemp <= 65.0 -> 0.65
            ambientTemp <= 70.0 -> 0.58
            ambientTemp <= 75.0 -> 0.50
            ambientTemp <= 80.0 -> 0.41
            else -> null
        }

    /*
     * IEC grouping factors are arrangement-dependent.
     *
     * The dataset below represents only the explicitly encoded
     * embedded/enclosed reference arrangement.
     *
     * Unsupported circuit counts return null.
     */
    fun groupingFactorEmbeddedEnclosed(
        numberOfCircuits: Int
    ): Double? =
        when (numberOfCircuits) {
            1 -> 1.00
            2 -> 0.80
            3 -> 0.70
            4 -> 0.70
            6 -> 0.55
            9 -> 0.50
            12 -> 0.45
            16 -> 0.40
            20 -> 0.40
            else -> null
        }

    /*
     * Legacy API retained for source compatibility.
     *
     * 0.0 means unavailable.
     * It must never be interpreted as a permitted correction factor.
     */
    fun groupingFactor(
        numberOfCircuits: Int
    ): Double =
        groupingFactorEmbeddedEnclosed(numberOfCircuits) ?: 0.0

    /*
     * Returns the base ampacity for an exact supported combination.
     *
     * No interpolation.
     * No extrapolation.
     * No substitution between installation methods.
     * No substitution between conductor materials.
     * No substitution between insulation systems.
     */
    fun getBaseAmpacity(
        section: Double,
        method: String,
        loadedConductors: Int = 2,
        material: ConductorMaterial = ConductorMaterial.Copper,
        insulation: InsulationType = InsulationType.PVC
    ): Double? {

        if (section <= 0.0) {
            return null
        }

        if (loadedConductors !in 2..3) {
            return null
        }

        val key =
            methodToKey(method)

        if (key !in installationMethods) {
            return null
        }

        val table =
            when (insulation) {

                InsulationType.PVC ->
                    when {
                        material == ConductorMaterial.Copper &&
                            loadedConductors == 2 ->
                            pvcCopper2Loaded

                        material == ConductorMaterial.Copper &&
                            loadedConductors == 3 ->
                            pvcCopper3Loaded

                        material == ConductorMaterial.Aluminum &&
                            loadedConductors == 2 ->
                            pvcAluminium2Loaded

                        material == ConductorMaterial.Aluminum &&
                            loadedConductors == 3 ->
                            pvcAluminium3Loaded

                        else ->
                            return null
                    }

                InsulationType.XLPE,
                InsulationType.EPR ->
                    when {
                        material == ConductorMaterial.Copper &&
                            loadedConductors == 2 ->
                            xlpeCopper2Loaded

                        material == ConductorMaterial.Copper &&
                            loadedConductors == 3 ->
                            xlpeCopper3Loaded

                        material == ConductorMaterial.Aluminum &&
                            loadedConductors == 2 ->
                            xlpeAluminium2Loaded

                        material == ConductorMaterial.Aluminum &&
                            loadedConductors == 3 ->
                            xlpeAluminium3Loaded

                        else ->
                            return null
                    }

                InsulationType.Rubber ->
                    return null
            }

        return table[section]?.get(key)
    }

    /*
     * IEC 60364-5-52 Table B.52.5
     * XLPE/EPR, three loaded conductors.
     */
    val xlpeCopper3Loaded = mapOf(
        1.5 to mapOf(
            "A1" to 17.0,
            "A2" to 16.5,
            "B1" to 20.0,
            "B2" to 19.5,
            "C" to 22.0,
            "D1" to 21.0,
            "D2" to 23.0
        ),
        2.5 to mapOf(
            "A1" to 23.0,
            "A2" to 22.0,
            "B1" to 28.0,
            "B2" to 26.0,
            "C" to 30.0,
            "D1" to 28.0,
            "D2" to 30.0
        ),
        4.0 to mapOf(
            "A1" to 31.0,
            "A2" to 30.0,
            "B1" to 37.0,
            "B2" to 35.0,
            "C" to 40.0,
            "D1" to 36.0,
            "D2" to 39.0
        ),
        6.0 to mapOf(
            "A1" to 40.0,
            "A2" to 38.0,
            "B1" to 48.0,
            "B2" to 44.0,
            "C" to 52.0,
            "D1" to 44.0,
            "D2" to 49.0
        ),
        10.0 to mapOf(
            "A1" to 54.0,
            "A2" to 51.0,
            "B1" to 66.0,
            "B2" to 60.0,
            "C" to 71.0,
            "D1" to 58.0,
            "D2" to 65.0
        ),
        16.0 to mapOf(
            "A1" to 73.0,
            "A2" to 68.0,
            "B1" to 88.0,
            "B2" to 80.0,
            "C" to 96.0,
            "D1" to 75.0,
            "D2" to 84.0
        ),
        25.0 to mapOf(
            "A1" to 95.0,
            "A2" to 89.0,
            "B1" to 117.0,
            "B2" to 105.0,
            "C" to 119.0,
            "D1" to 96.0,
            "D2" to 107.0
        ),
        35.0 to mapOf(
            "A1" to 117.0,
            "A2" to 109.0,
            "B1" to 144.0,
            "B2" to 128.0,
            "C" to 147.0,
            "D1" to 115.0,
            "D2" to 129.0
        ),
        50.0 to mapOf(
            "A1" to 141.0,
            "A2" to 130.0,
            "B1" to 175.0,
            "B2" to 154.0,
            "C" to 179.0,
            "D1" to 135.0,
            "D2" to 153.0
        ),
        70.0 to mapOf(
            "A1" to 179.0,
            "A2" to 164.0,
            "B1" to 222.0,
            "B2" to 194.0,
            "C" to 229.0,
            "D1" to 167.0,
            "D2" to 188.0
        ),
        95.0 to mapOf(
            "A1" to 216.0,
            "A2" to 197.0,
            "B1" to 269.0,
            "B2" to 233.0,
            "C" to 278.0,
            "D1" to 197.0,
            "D2" to 226.0
        ),
        120.0 to mapOf(
            "A1" to 249.0,
            "A2" to 227.0,
            "B1" to 312.0,
            "B2" to 268.0,
            "C" to 322.0,
            "D1" to 223.0,
            "D2" to 257.0
        ),
        150.0 to mapOf(
            "A1" to 285.0,
            "A2" to 259.0,
            "B1" to 342.0,
            "B2" to 300.0,
            "C" to 371.0,
            "D1" to 251.0,
            "D2" to 287.0
        ),
        185.0 to mapOf(
            "A1" to 324.0,
            "A2" to 295.0,
            "B1" to 384.0,
            "B2" to 340.0,
            "C" to 424.0,
            "D1" to 281.0,
            "D2" to 324.0
        ),
        240.0 to mapOf(
            "A1" to 380.0,
            "A2" to 346.0,
            "B1" to 450.0,
            "B2" to 398.0,
            "C" to 500.0,
            "D1" to 324.0,
            "D2" to 375.0
        ),
        300.0 to mapOf(
            "A1" to 435.0,
            "A2" to 396.0,
            "B1" to 514.0,
            "B2" to 455.0,
            "C" to 576.0,
            "D1" to 365.0,
            "D2" to 419.0
        )
    )

    val xlpeAluminium3Loaded = mapOf(
        2.5 to mapOf(
            "A1" to 19.0,
            "A2" to 18.0,
            "B1" to 22.0,
            "B2" to 21.0,
            "C" to 24.0,
            "D1" to 22.0,
            "D2" to 24.0
        ),
        4.0 to mapOf(
            "A1" to 25.0,
            "A2" to 24.0,
            "B1" to 29.0,
            "B2" to 28.0,
            "C" to 32.0,
            "D1" to 28.0,
            "D2" to 31.0
        ),
        6.0 to mapOf(
            "A1" to 32.0,
            "A2" to 30.0,
            "B1" to 37.0,
            "B2" to 35.0,
            "C" to 39.0,
            "D1" to 35.0,
            "D2" to 38.0
        ),
        10.0 to mapOf(
            "A1" to 43.0,
            "A2" to 40.0,
            "B1" to 54.0,
            "B2" to 49.0,
            "C" to 58.0,
            "D1" to 51.0,
            "D2" to 57.0
        ),
        16.0 to mapOf(
            "A1" to 57.0,
            "A2" to 53.0,
            "B1" to 71.0,
            "B2" to 64.0,
            "C" to 76.0,
            "D1" to 66.0,
            "D2" to 72.0
        ),
        25.0 to mapOf(
            "A1" to 76.0,
            "A2" to 70.0,
            "B1" to 94.0,
            "B2" to 84.0,
            "C" to 99.0,
            "D1" to 86.0,
            "D2" to 94.0
        ),
        35.0 to mapOf(
            "A1" to 94.0,
            "A2" to 87.0,
            "B1" to 116.0,
            "B2" to 103.0,
            "C" to 121.0,
            "D1" to 105.0,
            "D2" to 115.0
        ),
        50.0 to mapOf(
            "A1" to 115.0,
            "A2" to 106.0,
            "B1" to 143.0,
            "B2" to 126.0,
            "C" to 150.0,
            "D1" to 128.0,
            "D2" to 141.0
        ),
        70.0 to mapOf(
            "A1" to 146.0,
            "A2" to 134.0,
            "B1" to 182.0,
            "B2" to 159.0,
            "C" to 190.0,
            "D1" to 162.0,
            "D2" to 178.0
        ),
        95.0 to mapOf(
            "A1" to 176.0,
            "A2" to 162.0,
            "B1" to 221.0,
            "B2" to 192.0,
            "C" to 231.0,
            "D1" to 196.0,
            "D2" to 214.0
        ),
        120.0 to mapOf(
            "A1" to 204.0,
            "A2" to 187.0,
            "B1" to 255.0,
            "B2" to 220.0,
            "C" to 266.0,
            "D1" to 224.0,
            "D2" to 246.0
        ),
        150.0 to mapOf(
            "A1" to 235.0,
            "A2" to 215.0,
            "B1" to 288.0,
            "B2" to 248.0,
            "C" to 302.0,
            "D1" to 253.0,
            "D2" to 279.0
        ),
        185.0 to mapOf(
            "A1" to 268.0,
            "A2" to 245.0,
            "B1" to 330.0,
            "B2" to 284.0,
            "C" to 346.0,
            "D1" to 289.0,
            "D2" to 318.0
        ),
        240.0 to mapOf(
            "A1" to 313.0,
            "A2" to 286.0,
            "B1" to 386.0,
            "B2" to 332.0,
            "C" to 405.0,
            "D1" to 338.0,
            "D2" to 372.0
        ),
        300.0 to mapOf(
            "A1" to 356.0,
            "A2" to 326.0,
            "B1" to 440.0,
            "B2" to 378.0,
            "C" to 462.0,
            "D1" to 385.0,
            "D2" to 423.0
        )
    )

    fun standardConductorSections(): List<Double> =
        listOf(
            1.5,
            2.5,
            4.0,
            6.0,
            10.0,
            16.0,
            25.0,
            35.0,
            50.0,
            70.0,
            95.0,
            120.0,
            150.0,
            185.0,
            240.0,
            300.0
        )

    fun methodToKey(
        method: String
    ): String =
        when (
            method
                .trim()
                .uppercase()
        ) {
            "A1" -> "A1"
            "A2" -> "A2"
            "B1" -> "B1"
            "B2" -> "B2"
            "C" -> "C"
            "D1" -> "D1"
            "D2" -> "D2"

            "CONDUIT_THERMALLY_INSULATED_WALL" -> "A1"
            "CONDUIT_THERMALLY_INSULATED_MULTICORE" -> "A2"
            "CONDUIT_ON_WALL" -> "B1"
            "CONDUIT_MULTICORE_ON_WALL" -> "B2"
            "CLIPPED_DIRECT" -> "C"
            "UNDERGROUND_CONDUIT" -> "D1"
            "DIRECT_BURIED" -> "D2"

            else -> method
                .trim()
                .uppercase()
        )
}
