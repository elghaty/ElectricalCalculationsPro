package com.electrical.calculationspro.data.standards.egyptian

/**
 * Controlled installation-method registry for the Egyptian
 * electrical engineering dataset.
 *
 * Detailed correction factors remain in the existing code engine;
 * this registry only identifies the installation method.
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

    val methods: List<InstallationMethod> =
        listOf(
            InstallationMethod(
                id = "EG-CONDUIT",
                name = "Conduit",
                category = InstallationCategory.CONDUIT,
                description = "Conductors/cables installed in conduit."
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
            ),
            InstallationMethod(
                id = "EG-OTHER",
                name = "Other",
                category = InstallationCategory.OTHER,
                description = "Other installation arrangement requiring engineering review."
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
    ): List<String> =
        buildList {
            if (ambientTemperatureC < -50.0 ||
                ambientTemperatureC > 100.0
            ) {
                add(
                    "Ambient temperature is outside the supported input range."
                )
            }

            if (circuits <= 0) {
                add("Number of circuits must be greater than zero.")
            }

            if (method.id.isBlank()) {
                add("Installation method identifier is missing.")
            }
        }
}
