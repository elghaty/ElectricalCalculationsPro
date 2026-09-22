package com.electrical.calculationspro.data

import com.electrical.calculationspro.data.catalog.BreakerCatalogItem
import com.electrical.calculationspro.data.catalog.CableCatalogItem

enum class CurrentType {
    DirectCurrent,
    AlternatingSinglePhase,
    AlternatingTwoPhase,
    AlternatingThreePhase
}

enum class ConductorMaterial {
    Copper,
    Aluminum
}

enum class InsulationType {
    PVC,
    XLPE,
    EPR,
    Rubber
}

data class InstallationMethod(
    val code: String,
    val description: String
)

val iecInstallationMethods =
    IecTables.allInstallationMethods

data class ConductorSizingInput(
    val currentType: CurrentType =
        CurrentType.AlternatingSinglePhase,

    val voltage: Double = 230.0,

    val load: Double = 5000.0,

    val powerFactor: Double = 0.90,

    val lineLength: Double = 60.0,

    val installationMethod: InstallationMethod =
        iecInstallationMethods.first(),

    val ambientTemp: Double = 30.0,

    val conductor: ConductorMaterial =
        ConductorMaterial.Copper,

    val insulation: InsulationType =
        InsulationType.PVC,

    val circuitsInConduit: Int = 1,

    val maxVoltageDrop: Double = 4.0
)

data class ConductorSizingResult(
    val designCurrent: Double,

    val recommendedSection: Double,

    val selectedSection: Double,

    val ampacity: Double,

    val voltageDropPercent: Double,

    val voltageDropVolts: Double,

    val protectiveDevice: Double,

    val shortCircuitCurrentKA: Double = 0.0,

    val breakerWithinCableCapacity: Boolean = false,

    val voltageDropWithinLimit: Boolean = false,

    val notes: List<String>,

    /**
     * Selected cable from the equipment catalog.
     *
     * Nullable because a verified manufacturer catalog
     * item may not be available for every combination.
     */
    val catalogCable: CableCatalogItem? = null,

    /**
     * Selected breaker from the equipment catalog.
     *
     * Nullable because exact Icu/Ics data may require
     * the final manufacturer configuration.
     */
    val catalogBreaker: BreakerCatalogItem? = null
)

val standardSections = listOf(
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
