package com.electrical.calculationspro.data.standards.egyptian

/**
 * Installation-method abstraction for the Egyptian code engine.
 *
 * This keeps installation rules separate from cable calculations.
 */
object EgyptianInstallationRules {

    enum class InstallationCategory {
        CONDUIT,
        TRUNKING,
        CABLE_TRAY,
        DIRECT_BURIED,
        FREE_AIR,
        DUCT,
        OTHER
    }

    data class InstallationMethod(
        val id: String,
        val name: String,
        val category: InstallationCategory,
        val description: String
    )

    val methods: List<InstallationMethod> = listOf(
        InstallationMethod(
            id = "EG-CONDUIT",
            name = "Conduit",
            category = InstallationCategory.CONDUIT,
            description = "Cable/conductor installed in conduit."
        ),
        InstallationMethod(
            id = "EG-TRUNKING",
            name = "Trunking",
            category = InstallationCategory.TRUNKING,
            description = "Conductors/cables installed in trunking."
        ),
        InstallationMethod(
            id = "EG-TRAY",
            name = "Cable Tray",
            category = InstallationCategory.CABLE_TRAY,
            description = "Cables installed on cable tray."
        ),
        InstallationMethod(
            id = "EG-DIRECT-BURIED",
            name = "Direct Buried",
            category = InstallationCategory.DIRECT_BURIED,
            description = "Cable installed directly underground."
        ),
        InstallationMethod(
            id = "EG-FREE-AIR",
            name = "Free Air",
            category = InstallationCategory.FREE_AIR,
            description = "Cable installed in free air."
        ),
        InstallationMethod(
            id = "EG-DUCT",
            name = "Duct",
            category = InstallationCategory.DUCT,
            description = "Cable installed in duct."
        )
    )

    fun find(
        id: String
    ): InstallationMethod? =
        methods.firstOrNull {
            it.id.equals(id, ignoreCase = true)
        }

    fun validate(
        method: InstallationMethod,
        ambientTemperatureC: Double,
        circuits: Int
    ): List<String> {

        val notes = mutableListOf<String>()

        if (ambientTemperatureC < -50.0 ||
            ambientTemperatureC > 100.0
        ) {
            notes += "Ambient temperature is outside the supported input range."
        }

        if (circuits <= 0) {
            notes += "Number of circuits must be greater than zero."
        }

        if (method.id.isBlank()) {
            notes += "Installation method identifier is missing."
        }

        return notes
    }
}
