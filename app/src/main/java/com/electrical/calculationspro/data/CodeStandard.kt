package com.electrical.calculationspro.data

/**
 * Engineering design standards supported by the application.
 *
 * EGYPTIAN:
 * Egyptian Electrical Code / Egyptian requirements.
 *
 * IEC:
 * International Electrotechnical Commission requirements.
 *
 * NEC:
 * National Electrical Code (NFPA 70).
 *
 * CEI / CEC are retained for backward compatibility with the
 * existing application model. They are not aliases for IEC/NEC.
 */
enum class Standard(
    val displayName: String,
    val shortName: String,
    val description: String
) {
    EGYPTIAN(
        displayName = "Egyptian Electrical Code",
        shortName = "EEC",
        description =
            "Egyptian electrical design requirements and applicable Egyptian specifications."
    ),

    IEC(
        displayName = "IEC",
        shortName = "IEC",
        description =
            "International Electrotechnical Commission standards."
    ),

    NEC(
        displayName = "NFPA 70 - NEC",
        shortName = "NEC",
        description =
            "National Electrical Code requirements."
    ),

    CEI(
        displayName = "CEI 64-8",
        shortName = "CEI",
        description =
            "Italian electrical installation standard."
    ),

    CEC(
        displayName = "Canadian Electrical Code",
        shortName = "CEC",
        description =
            "Canadian electrical installation requirements."
    );

    companion object {

        fun fromName(
            value: String
        ): Standard? {

            return entries.firstOrNull {
                it.name.equals(
                    value.trim(),
                    ignoreCase = true
                ) ||
                    it.shortName.equals(
                        value.trim(),
                        ignoreCase = true
                    )
            }
        }

        fun selectableStandards(): List<Standard> =
            entries.toList()
    }
}
