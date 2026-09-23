package com.electrical.calculationspro.data.project

/**
 * ================================================================
 * PROFESSIONAL DESIGN
 * Project Validation
 * ================================================================
 *
 * Validation only.
 * No UI.
 * No Compose.
 * No engineering calculation formulas.
 * ================================================================
 */

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
            errors.add("Project name is required.")
        }

        if (project.projectNumber.isBlank()) {
            warnings.add("Project number has not been defined.")
        }

        if (project.clientName.isBlank()) {
            warnings.add("Client name has not been defined.")
        }

        if (project.electrical.panels.isEmpty() &&
            project.water.pumps.isEmpty() &&
            project.sewage.pumps.isEmpty()
        ) {
            warnings.add(
                "No engineering equipment has been added to the project."
            )
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
                errors.add("Electrical panel name is required.")
            }

            if (panel.voltageV <= 0.0) {
                errors.add(
                    "Panel ${panel.name} has an invalid voltage."
                )
            }

            if (panel.phases !in 1..3) {
                errors.add(
                    "Panel ${panel.name} has an invalid phase count."
                )
            }
        }

        design.loads.forEach { load ->

            if (load.name.isBlank()) {
                errors.add("Electrical load name is required.")
            }

            if (load.connectedLoadKw < 0.0) {
                errors.add(
                    "Load ${load.name} has an invalid connected load."
                )
            }

            if (load.powerFactor <= 0.0 ||
                load.powerFactor > 1.0
            ) {
                errors.add(
                    "Load ${load.name} has an invalid power factor."
                )
            }
        }

        design.cables.forEach { cable ->

            if (cable.name.isBlank()) {
                errors.add("Cable name is required.")
            }

            if (cable.lengthM < 0.0) {
                errors.add(
                    "Cable ${cable.name} has an invalid length."
                )
            }

            if (cable.sectionMm2 < 0.0) {
                errors.add(
                    "Cable ${cable.name} has an invalid section."
                )
            }
        }

        if (design.sld == null &&
            design.panels.isNotEmpty()
        ) {
            warnings.add(
                "Electrical panels exist but no SLD has been created."
            )
        }
    }

    private fun validateWater(
        design: WaterDesign,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {

        if (design.requiredFlowM3PerHour < 0.0) {
            errors.add(
                "Water design flow cannot be negative."
            )
        }

        if (design.staticHeadM < 0.0 ||
            design.frictionHeadM < 0.0 ||
            design.minorLossHeadM < 0.0 ||
            design.requiredPressureHeadM < 0.0
        ) {
            errors.add(
                "Water hydraulic head values cannot be negative."
            )
        }

        design.pipes.forEach { pipe ->

            if (pipe.diameterMm <= 0.0) {
                errors.add(
                    "Water pipe ${pipe.name} has an invalid diameter."
                )
            }

            if (pipe.lengthM < 0.0) {
                errors.add(
                    "Water pipe ${pipe.name} has an invalid length."
                )
            }
        }

        design.pumps.forEach { pump ->

            if (pump.flowM3PerHour <= 0.0) {
                warnings.add(
                    "Water pump ${pump.name} has no design flow."
                )
            }

            if (pump.headM <= 0.0) {
                warnings.add(
                    "Water pump ${pump.name} has no design head."
                )
            }

            if (pump.manufacturer.isBlank() ||
                pump.model.isBlank()
            ) {
                warnings.add(
                    "Water pump ${pump.name} has no verified manufacturer/model data."
                )
            }
        }
    }

    private fun validateSewage(
        design: SewageDesign,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {

        if (design.averageFlowM3PerDay < 0.0) {
            errors.add(
                "Average sewage flow cannot be negative."
            )
        }

        if (design.peakFlowM3PerDay < 0.0) {
            errors.add(
                "Peak sewage flow cannot be negative."
            )
        }

        if (design.minimumFlowM3PerDay < 0.0) {
            errors.add(
                "Minimum sewage flow cannot be negative."
            )
        }

        if (design.peakFlowM3PerDay <
            design.averageFlowM3PerDay
        ) {
            warnings.add(
                "Peak sewage flow is lower than average flow."
            )
        }

        design.wetWell?.let { wetWell ->

            if (wetWell.diameterM <= 0.0) {
                errors.add(
                    "Wet well ${wetWell.name} has an invalid diameter."
                )
            }

            if (wetWell.effectiveDepthM <= 0.0) {
                errors.add(
                    "Wet well ${wetWell.name} has an invalid effective depth."
                )
            }
        }

        design.risingMain?.let { risingMain ->

            if (risingMain.diameterMm <= 0.0) {
                errors.add(
                    "Rising main ${risingMain.name} has an invalid diameter."
                )
            }

            if (risingMain.lengthM < 0.0) {
                errors.add(
                    "Rising main ${risingMain.name} has an invalid length."
                )
            }
        }

        if (design.pumps.isNotEmpty()) {

            val dutyCount =
                design.pumps.count {
                    it.duty
                }

            if (dutyCount == 0) {
                errors.add(
                    "Sewage design has pumps but no duty pump."
                )
            }

            design.pumps.forEach { pump ->

                if (pump.flowM3PerHour <= 0.0) {
                    warnings.add(
                        "Sewage pump ${pump.name} has no design flow."
                    )
                }

                if (pump.headM <= 0.0) {
                    warnings.add(
                        "Sewage pump ${pump.name} has no design head."
                    )
                }

                if (pump.manufacturer.isBlank() ||
                    pump.model.isBlank()
                ) {
                    warnings.add(
                        "Sewage pump ${pump.name} has no verified manufacturer/model data."
                    )
                }
            }
        }
    }
}
