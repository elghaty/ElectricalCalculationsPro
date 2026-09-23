package com.electrical.calculationspro.data.project

data class DesignProjectValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

object DesignProjectValidator {

    fun validate(
        project: DesignProject
    ): DesignProjectValidationResult {

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (project.projectName.isBlank()) {
            errors += "Project name is required."
        }

        if (project.projectNumber.isBlank()) {
            warnings += "Project number has not been defined."
        }

        if (project.clientName.isBlank()) {
            warnings += "Client name has not been defined."
        }

        validateElectrical(
            project.electrical,
            errors,
            warnings
        )

        validateWater(
            project.water,
            errors,
            warnings
        )

        validateSewage(
            project.sewage,
            errors,
            warnings
        )

        if (
            project.electrical.panels.isEmpty() &&
            project.electrical.loads.isEmpty() &&
            project.water.pumps.isEmpty() &&
            project.water.pipes.isEmpty() &&
            project.sewage.pumps.isEmpty() &&
            project.sewage.risingMain == null
        ) {
            warnings +=
                "The project does not contain engineering design elements."
        }

        return DesignProjectValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateElectrical(
        design: ElectricalDesign,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {

        design.panels.forEach { panel ->

            if (panel.name.isBlank()) {
                errors +=
                    "Electrical panel name is required."
            }

            if (panel.voltageV <= 0.0) {
                errors +=
                    "Panel ${panel.name} has invalid voltage."
            }

            if (panel.phases !in 1..3) {
                errors +=
                    "Panel ${panel.name} has invalid phase count."
            }
        }

        design.loads.forEach { load ->

            if (load.name.isBlank()) {
                errors +=
                    "Electrical load name is required."
            }

            if (load.quantity <= 0) {
                errors +=
                    "Load ${load.name} has invalid quantity."
            }

            if (load.connectedLoadKw < 0.0) {
                errors +=
                    "Load ${load.name} has invalid connected load."
            }

            if (
                load.powerFactor <= 0.0 ||
                load.powerFactor > 1.0
            ) {
                errors +=
                    "Load ${load.name} has invalid power factor."
            }
        }

        design.cables.forEach { cable ->

            if (cable.name.isBlank()) {
                errors +=
                    "Cable name is required."
            }

            if (cable.lengthM < 0.0) {
                errors +=
                    "Cable ${cable.name} has invalid length."
            }

            if (cable.sectionMm2 < 0.0) {
                errors +=
                    "Cable ${cable.name} has invalid section."
            }
        }

        design.breakers.forEach { breaker ->

            if (breaker.name.isBlank()) {
                errors +=
                    "Breaker name is required."
            }

            if (breaker.ratingA < 0.0) {
                errors +=
                    "Breaker ${breaker.name} has invalid rating."
            }

            if (breaker.breakingCapacityKA < 0.0) {
                errors +=
                    "Breaker ${breaker.name} has invalid breaking capacity."
            }
        }

        if (
            design.panels.isNotEmpty() &&
            design.sld == null
        ) {
            warnings +=
                "Electrical panels exist but no SLD has been created."
        }
    }

    private fun validateWater(
        design: WaterDesign,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {

        if (design.requiredFlowM3PerHour < 0.0) {
            errors +=
                "Water design flow cannot be negative."
        }

        if (
            design.staticHeadM < 0.0 ||
            design.frictionHeadM < 0.0 ||
            design.minorLossHeadM < 0.0 ||
            design.requiredPressureHeadM < 0.0
        ) {
            errors +=
                "Water head values cannot be negative."
        }

        design.pipes.forEach { pipe ->

            if (pipe.name.isBlank()) {
                errors +=
                    "Water pipe name is required."
            }

            if (pipe.diameterMm <= 0.0) {
                errors +=
                    "Water pipe ${pipe.name} has invalid diameter."
            }

            if (pipe.lengthM < 0.0) {
                errors +=
                    "Water pipe ${pipe.name} has invalid length."
            }
        }

        design.pumps.forEach { pump ->

            if (pump.name.isBlank()) {
                errors +=
                    "Water pump name is required."
            }

            if (pump.flowM3PerHour <= 0.0) {
                warnings +=
                    "Water pump ${pump.name} has no valid design flow."
            }

            if (pump.headM <= 0.0) {
                warnings +=
                    "Water pump ${pump.name} has no valid design head."
            }

            if (
                pump.manufacturer.isBlank() ||
                pump.model.isBlank()
            ) {
                warnings +=
                    "Water pump ${pump.name} has no verified manufacturer/model data."
            }
        }
    }

    private fun validateSewage(
        design: SewageDesign,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {

        if (design.averageFlowM3PerDay < 0.0) {
            errors +=
                "Average sewage flow cannot be negative."
        }

        if (design.peakFlowM3PerDay < 0.0) {
            errors +=
                "Peak sewage flow cannot be negative."
        }

        if (design.minimumFlowM3PerDay < 0.0) {
            errors +=
                "Minimum sewage flow cannot be negative."
        }

        if (
            design.peakFlowM3PerDay <
            design.averageFlowM3PerDay
        ) {
            warnings +=
                "Peak sewage flow is lower than average flow."
        }

        design.wetWell?.let { wetWell ->

            if (wetWell.name.isBlank()) {
                errors +=
                    "Wet well name is required."
            }

            if (wetWell.diameterM <= 0.0) {
                errors +=
                    "Wet well ${wetWell.name} has invalid diameter."
            }

            if (wetWell.effectiveDepthM <= 0.0) {
                errors +=
                    "Wet well ${wetWell.name} has invalid effective depth."
            }
        }

        design.risingMain?.let { main ->

            if (main.name.isBlank()) {
                errors +=
                    "Rising main name is required."
            }

            if (main.diameterMm <= 0.0) {
                errors +=
                    "Rising main ${main.name} has invalid diameter."
            }

            if (main.lengthM < 0.0) {
                errors +=
                    "Rising main ${main.name} has invalid length."
            }
        }

        val dutyCount =
            design.pumps.count { it.duty }

        if (
            design.pumps.isNotEmpty() &&
            dutyCount == 0
        ) {
            errors +=
                "Sewage design has pumps but no duty pump."
        }

        design.pumps.forEach { pump ->

            if (pump.name.isBlank()) {
                errors +=
                    "Sewage pump name is required."
            }

            if (pump.flowM3PerHour <= 0.0) {
                warnings +=
                    "Sewage pump ${pump.name} has no valid design flow."
            }

            if (pump.headM <= 0.0) {
                warnings +=
                    "Sewage pump ${pump.name} has no valid design head."
            }

            if (
                pump.manufacturer.isBlank() ||
                pump.model.isBlank()
            ) {
                warnings +=
                    "Sewage pump ${pump.name} has no verified manufacturer/model data."
            }
        }
    }
}
