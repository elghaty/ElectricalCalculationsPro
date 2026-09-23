package com.electrical.calculationspro.data.project

import com.electrical.calculationspro.data.Standard
import java.util.UUID

data class DesignProject(
    val id: String = UUID.randomUUID().toString(),
    val projectName: String = "",
    val projectNumber: String = "",
    val clientName: String = "",
    val consultantName: String = "",
    val location: String = "",
    val description: String = "",

    val electricalStandard: Standard? = Standard.IEC,

    val status: DesignStatus = DesignStatus.DRAFT,

    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),

    val electrical: ElectricalDesign = ElectricalDesign(),
    val water: WaterDesign = WaterDesign(),
    val sewage: SewageDesign = SewageDesign()
) {

    fun updateTimestamp(
        timestampMillis: Long = System.currentTimeMillis()
    ): DesignProject =
        copy(updatedAtMillis = timestampMillis)

    fun start(): DesignProject =
        copy(
            status = DesignStatus.IN_PROGRESS,
            updatedAtMillis = System.currentTimeMillis()
        )

    fun complete(): DesignProject =
        copy(
            status = DesignStatus.COMPLETED,
            updatedAtMillis = System.currentTimeMillis()
        )

    fun archive(): DesignProject =
        copy(
            status = DesignStatus.ARCHIVED,
            updatedAtMillis = System.currentTimeMillis()
        )

    fun withElectrical(
        design: ElectricalDesign
    ): DesignProject =
        copy(
            electrical = design,
            status = if (status == DesignStatus.DRAFT)
                DesignStatus.IN_PROGRESS
            else status,
            updatedAtMillis = System.currentTimeMillis()
        )

    fun withWater(
        design: WaterDesign
    ): DesignProject =
        copy(
            water = design,
            status = if (status == DesignStatus.DRAFT)
                DesignStatus.IN_PROGRESS
            else status,
            updatedAtMillis = System.currentTimeMillis()
        )

    fun withSewage(
        design: SewageDesign
    ): DesignProject =
        copy(
            sewage = design,
            status = if (status == DesignStatus.DRAFT)
                DesignStatus.IN_PROGRESS
            else status,
            updatedAtMillis = System.currentTimeMillis()
        )
}

data class ElectricalDesign(
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED,

    val panels: List<ElectricalPanel> = emptyList(),
    val loads: List<ElectricalLoad> = emptyList(),
    val cables: List<ElectricalCable> = emptyList(),
    val breakers: List<ElectricalBreaker> = emptyList(),
    val transformers: List<ElectricalTransformer> = emptyList(),
    val generators: List<ElectricalGenerator> = emptyList(),
    val protections: List<ElectricalProtection> = emptyList(),

    val sld: ElectricalSldDesign? = null
) {

    fun withStatus(
        value: DesignCalculationStatus
    ): ElectricalDesign =
        copy(status = value)
}

data class ElectricalPanel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val voltageV: Double = 400.0,
    val phases: Int = 3,
    val frequencyHz: Double = 50.0,
    val sourceType: String = "",
    val sourceId: String? = null,
    val designLoadKw: Double = 0.0,
    val designCurrentA: Double = 0.0,
    val shortCircuitKA: Double = 0.0,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class ElectricalLoad(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val quantity: Int = 1,
    val connectedLoadKw: Double = 0.0,
    val demandFactor: Double = 1.0,
    val diversityFactor: Double = 1.0,
    val powerFactor: Double = 0.90,
    val voltageV: Double = 400.0,
    val phases: Int = 3,
    val sourcePanelId: String? = null,
    val designLoadKw: Double = 0.0,
    val designCurrentA: Double = 0.0,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class ElectricalCable(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val from: String = "",
    val to: String = "",
    val lengthM: Double = 0.0,
    val sectionMm2: Double = 0.0,
    val cores: Int = 0,
    val material: String = "",
    val insulation: String = "",
    val installationMethod: String = "",
    val designCurrentA: Double = 0.0,
    val ampacityA: Double = 0.0,
    val voltageDropPercent: Double = 0.0,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class ElectricalBreaker(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ratingA: Double = 0.0,
    val poles: Int = 3,
    val breakingCapacityKA: Double = 0.0,
    val utilizationVoltageV: Double = 400.0,
    val tripUnit: String = "",
    val protectedElementId: String? = null,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class ElectricalTransformer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ratingKva: Double = 0.0,
    val primaryVoltageV: Double = 11000.0,
    val secondaryVoltageV: Double = 400.0,
    val impedancePercent: Double = 0.0,
    val frequencyHz: Double = 50.0,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class ElectricalGenerator(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ratingKva: Double = 0.0,
    val voltageV: Double = 400.0,
    val powerFactor: Double = 0.80,
    val frequencyHz: Double = 50.0,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class ElectricalProtection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val deviceType: String = "",
    val upstreamDevice: String = "",
    val downstreamDevice: String = "",
    val selectivityRequired: Boolean = true,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class ElectricalSldDesign(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Main SLD",
    val source: String = "",
    val nodes: List<String> = emptyList(),
    val connections: List<String> = emptyList(),
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class WaterDesign(
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED,

    val requiredFlowM3PerHour: Double = 0.0,
    val staticHeadM: Double = 0.0,
    val frictionHeadM: Double = 0.0,
    val minorLossHeadM: Double = 0.0,
    val requiredPressureHeadM: Double = 0.0,
    val tdhM: Double = 0.0,

    val pipes: List<WaterPipe> = emptyList(),
    val pumps: List<WaterPump> = emptyList()
)

data class WaterPipe(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val diameterMm: Double = 0.0,
    val lengthM: Double = 0.0,
    val material: String = "",
    val flowM3PerHour: Double = 0.0,
    val velocityMPerS: Double = 0.0,
    val frictionLossM: Double = 0.0,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class WaterPump(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val flowM3PerHour: Double = 0.0,
    val headM: Double = 0.0,
    val pumpEfficiency: Double = 0.0,
    val motorEfficiency: Double = 0.0,
    val motorPowerKw: Double = 0.0,
    val yearlyEnergyKwh: Double = 0.0,
    val manufacturer: String = "",
    val model: String = "",
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class SewageDesign(
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED,

    val averageFlowM3PerDay: Double = 0.0,
    val peakFlowM3PerDay: Double = 0.0,
    val minimumFlowM3PerDay: Double = 0.0,

    val wetWell: WetWellDesign? = null,
    val risingMain: RisingMainDesign? = null,
    val pumps: List<SewagePump> = emptyList()
)

data class WetWellDesign(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val diameterM: Double = 0.0,
    val effectiveDepthM: Double = 0.0,
    val operatingVolumeM3: Double = 0.0,
    val startLevelM: Double = 0.0,
    val stopLevelM: Double = 0.0,
    val highLevelM: Double = 0.0,
    val emergencyLevelM: Double = 0.0,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class RisingMainDesign(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val diameterMm: Double = 0.0,
    val lengthM: Double = 0.0,
    val material: String = "",
    val flowM3PerHour: Double = 0.0,
    val velocityMPerS: Double = 0.0,
    val frictionLossM: Double = 0.0,
    val minorLossHeadM: Double = 0.0,
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)

data class SewagePump(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val duty: Boolean = true,
    val standby: Boolean = false,
    val flowM3PerHour: Double = 0.0,
    val headM: Double = 0.0,
    val pumpEfficiency: Double = 0.0,
    val motorEfficiency: Double = 0.0,
    val motorPowerKw: Double = 0.0,
    val yearlyEnergyKwh: Double = 0.0,
    val manufacturer: String = "",
    val model: String = "",
    val status: DesignCalculationStatus =
        DesignCalculationStatus.NOT_STARTED
)
