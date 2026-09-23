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

        val errors =
            mutableListOf<String>()

        val warnings =
            mutableListOf<String>()

        if (
            project.projectName
                .trim()
                .isEmpty()
        ) {
            errors +=
                "Project name is required."
        }

        if (
            project.projectNumber
                .trim()
                .isEmpty()
        ) {
            warnings +=
                "Project number is not defined."
        }

        if (
            project.clientName
                .trim()
                .isEmpty()
        ) {
            warnings +=
                "Client name is not defined."
        }

        if (
            project.electrical.panels.isEmpty() &&
            project.electrical.loads.isEmpty() &&
            project.water.pipes.isEmpty() &&
            project.water.pumps.isEmpty() &&
            project.sewage.pumps.isEmpty() &&
            project.sewage.risingMain == null
        ) {
            warnings +=
                "No engineering design elements have been added."
        }

        validateElectrical(
            project,
            errors,
            warnings
        )

        validateWater(
            project,
            errors,
            warnings
        )

        validateSewage(
            project,
            errors,
            warnings
        )

        return DesignProjectValidationResult(
            valid =
                errors.isEmpty(),
            errors =
                errors.distinct(),
            warnings =
                warnings.distinct()
        )
    }

    private fun validateElectrical(
        project: DesignProject,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {

        val electrical =
            project.electrical

        electrical.panels.forEach { panel ->

            if (panel.name.isBlank()) {
                errors +=
                    "Electrical panel name is required."
            }

            if (panel.voltageV <= 0.0) {
                errors +=
                    "Panel ${panel.name}: invalid voltage."
            }

            if (panel.phases !in 1..3) {
                errors +=
                    "Panel ${panel.name}: invalid phase count."
            }

            if (panel.frequencyHz <= 0.0) {
                errors +=
                    "Panel ${panel.name}: invalid frequency."
            }

            if (panel.designLoadKw < 0.0) {
                errors +=
                    "Panel ${panel.name}: invalid design load."
            }

            if (panel.designCurrentA < 0.0) {
                errors +=
                    "Panel ${panel.name}: invalid design current."
            }

            if (panel.shortCircuitKA < 0.0) {
                errors +=
                    "Panel ${panel.name}: invalid short-circuit current."
            }
        }

        electrical.loads.forEach { load ->

            if (load.name.isBlank()) {
                errors +=
                    "Electrical load name is required."
            }

            if (load.quantity <= 0) {
                errors +=
                    "Load ${load.name}: quantity must be greater than zero."
            }

            if (load.connectedLoadKw < 0.0) {
                errors +=
                    "Load ${load.name}: connected load cannot be negative."
            }

            if (
                load.demandFactor !in 0.0..1.0
            ) {
                errors +=
                    "Load ${load.name}: invalid demand factor."
            }

            if (
                load.diversityFactor !in 0.0..1.0
            ) {
                errors +=
                    "Load ${load.name}: invalid diversity factor."
            }

            if (
                load.powerFactor <= 0.0 ||
                load.powerFactor > 1.0
            ) {
                errors +=
                    "Load ${load.name}: invalid power factor."
            }

            if (load.voltageV <= 0.0) {
                errors +=
                    "Load ${load.name}: invalid voltage."
            }

            if (load.phases !in 1..3) {
                errors +=
                    "Load ${load.name}: invalid phase count."
            }

            load.sourcePanelId?.let { panelId ->

                if (
                    electrical.panels.none {
                        it.id == panelId
                    }
                ) {
                    errors +=
                        "Load ${load.name}: referenced panel does not exist."
                }
            }
        }

        electrical.cables.forEach { cable ->

            if (cable.name.isBlank()) {
                errors +=
                    "Cable name is required."
            }

            if (cable.lengthM < 0.0) {
                errors +=
                    "Cable ${cable.name}: invalid length."
            }

            if (cable.sectionMm2 < 0.0) {
                errors +=
                    "Cable ${cable.name}: invalid section."
            }

            if (cable.cores < 0) {
                errors +=
                    "Cable ${cable.name}: invalid core count."
            }

            if (cable.designCurrentA < 0.0) {
                errors +=
                    "Cable ${cable.name}: invalid design current."
            }

            if (cable.ampacityA < 0.0) {
                errors +=
                    "Cable ${cable.name}: invalid ampacity."
            }

            if (cable.voltageDropPercent < 0.0) {
                errors +=
                    "Cable ${cable.name}: invalid voltage drop."
            }
        }

        electrical.breakers.forEach { breaker ->

            if (breaker.name.isBlank()) {
                errors +=
                    "Breaker name is required."
            }

            if (breaker.ratingA < 0.0) {
                errors +=
                    "Breaker ${breaker.name}: invalid rating."
            }

            if (breaker.breakingCapacityKA < 0.0) {
                errors +=
                    "Breaker ${breaker.name}: invalid breaking capacity."
            }

            if (breaker.poles !in 1..4) {
                errors +=
                    "Breaker ${breaker.name}: invalid pole count."
            }

            breaker.protectedElementId?.let { id ->

                val exists =
                    electrical.loads.any {
                        it.id == id
                    } ||
                        electrical.cables.any {
                            it.id == id
                        } ||
                        electrical.panels.any {
                            it.id == id
                        }

                if (!exists) {
                    warnings +=
                        "Breaker ${breaker.name}: protected element reference was not found."
                }
            }
        }

        electrical.transformers.forEach { transformer ->

            if (transformer.name.isBlank()) {
                errors +=
                    "Transformer name is required."
            }

            if (transformer.ratingKva <= 0.0) {
                errors +=
                    "Transformer ${transformer.name}: invalid rating."
            }

            if (transformer.primaryVoltageV <= 0.0) {
                errors +=
                    "Transformer ${transformer.name}: invalid primary voltage."
            }

            if (transformer.secondaryVoltageV <= 0.0) {
                errors +=
                    "Transformer ${transformer.name}: invalid secondary voltage."
            }

            if (transformer.impedancePercent < 0.0) {
                errors +=
                    "Transformer ${transformer.name}: invalid impedance."
            }
        }

        electrical.generators.forEach { generator ->

            if (generator.name.isBlank()) {
                errors +=
                    "Generator name is required."
            }

            if (generator.ratingKva <= 0.0) {
                errors +=
                    "Generator ${generator.name}: invalid rating."
            }

            if (
                generator.powerFactor <= 0.0 ||
                generator.powerFactor > 1.0
            ) {
                errors +=
                    "Generator ${generator.name}: invalid power factor."
            }
        }

        if (
            electrical.panels.isNotEmpty() &&
            electrical.sld == null
        ) {
            warnings +=
                "Electrical panels exist without an SLD."
        }
    }

    private fun validateWater(
        project: DesignProject,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {

        val water =
            project.water

        if (water.requiredFlowM3PerHour < 0.0) {
            errors +=
                "Water flow cannot be negative."
        }

        if (water.staticHeadM < 0.0) {
            errors +=
                "Water static head cannot be negative."
        }

        if (water.frictionHeadM < 0.0) {
            errors +=
                "Water friction head cannot be negative."
        }

        if (water.minorLossHeadM < 0.0) {
            errors +=
                "Water minor-loss head cannot be negative."
        }

        if (water.requiredPressureHeadM < 0.0) {
            errors +=
                "Water required pressure head cannot be negative."
        }

        if (water.tdhM < 0.0) {
            errors +=
                "Water TDH cannot be negative."
        }

        water.pipes.forEach { pipe ->

            if (pipe.name.isBlank()) {
                errors +=
                    "Water pipe name is required."
            }

            if (pipe.diameterMm <= 0.0) {
                errors +=
                    "Water pipe ${pipe.name}: invalid diameter."
            }

            if (pipe.lengthM < 0.0) {
                errors +=
                    "Water pipe ${pipe.name}: invalid length."
            }

            if (pipe.flowM3PerHour < 0.0) {
                errors +=
                    "Water pipe ${pipe.name}: invalid flow."
            }

            if (pipe.velocityMPerS < 0.0) {
                errors +=
                    "Water pipe ${pipe.name}: invalid velocity."
            }

            if (pipe.frictionLossM < 0.0) {
                errors +=
                    "Water pipe ${pipe.name}: invalid friction loss."
            }
        }

        water.pumps.forEach { pump ->

            if (pump.name.isBlank()) {
                errors +=
                    "Water pump name is required."
            }

            if (pump.flowM3PerHour < 0.0) {
                errors +=
                    "Water pump ${pump.name}: invalid flow."
            }

            if (pump.headM < 0.0) {
                errors +=
                    "Water pump ${pump.name}: invalid head."
            }

            if (
                pump.pumpEfficiency !in 0.0..1.0
            ) {
                errors +=
                    "Water pump ${pump.name}: invalid pump efficiency."
            }

            if (
                pump.motorEfficiency !in 0.0..1.0
            ) {
                errors +=
                    "Water pump ${pump.name}: invalid motor efficiency."
            }

            if (pump.motorPowerKw < 0.0) {
                errors +=
                    "Water pump ${pump.name}: invalid motor power."
            }

            if (
                pump.manufacturer.isBlank() ||
                pump.model.isBlank()
            ) {
                warnings +=
                    "Water pump ${pump.name}: manufacturer/model is not defined."
            }
        }
    }

    private fun validateSewage(
        project: DesignProject,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {

        val sewage =
            project.sewage

        if (sewage.averageFlowM3PerDay < 0.0) {
            errors +=
                "Average sewage flow cannot be negative."
        }

        if (sewage.peakFlowM3PerDay < 0.0) {
            errors +=
                "Peak sewage flow cannot be negative."
        }

        if (sewage.minimumFlowM3PerDay < 0.0) {
            errors +=
                "Minimum sewage flow cannot be negative."
        }

        if (
            sewage.peakFlowM3PerDay <
            sewage.averageFlowM3PerDay
        ) {
            errors +=
                "Peak sewage flow cannot be lower than average flow."
        }

        if (
            sewage.minimumFlowM3PerDay >
            sewage.averageFlowM3PerDay
        ) {
            warnings +=
                "Minimum sewage flow is greater than average flow."
        }

        if (sewage.staticHeadM < 0.0) {
            errors +=
                "Sewage static head cannot be negative."
        }

        if (sewage.tdhM < 0.0) {
            errors +=
                "Sewage TDH cannot be negative."
        }

        sewage.wetWell?.let { wetWell ->

            if (wetWell.name.isBlank()) {
                errors +=
                    "Wet well name is required."
            }

            if (wetWell.diameterM <= 0.0) {
                errors +=
                    "Wet well ${wetWell.name}: invalid diameter."
            }

            if (wetWell.effectiveDepthM <= 0.0) {
                errors +=
                    "Wet well ${wetWell.name}: invalid depth."
            }

            if (wetWell.operatingVolumeM3 < 0.0) {
                errors +=
                    "Wet well ${wetWell.name}: invalid operating volume."
            }
        }

        sewage.risingMain?.let { main ->

            if (main.name.isBlank()) {
                errors +=
                    "Rising main name is required."
            }

            if (main.diameterMm <= 0.0) {
                errors +=
                    "Rising main ${main.name}: invalid diameter."
            }

            if (main.lengthM < 0.0) {
                errors +=
                    "Rising main ${main.name}: invalid length."
            }

            if (main.flowM3PerHour < 0.0) {
                errors +=
                    "Rising main ${main.name}: invalid flow."
            }

            if (main.velocityMPerS < 0.0) {
                errors +=
                    "Rising main ${main.name}: invalid velocity."
            }

            if (main.frictionLossM < 0.0) {
                errors +=
                    "Rising main ${main.name}: invalid friction loss."
            }

            if (main.minorLossHeadM < 0.0) {
                errors +=
                    "Rising main ${main.name}: invalid minor loss."
            }
        }

        sewage.pumps.forEach { pump ->

            if (pump.name.isBlank()) {
                errors +=
                    "Sewage pump name is required."
            }

            if (pump.flowM3PerHour < 0.0) {
                errors +=
                    "Sewage pump ${pump.name}: invalid flow."
            }

            if (pump.headM < 0.0) {
                errors +=
                    "Sewage pump ${pump.name}: invalid head."
            }

            if (
                pump.pumpEfficiency !in 0.0..1.0
            ) {
                errors +=
                    "Sewage pump ${pump.name}: invalid pump efficiency."
            }

            if (
                pump.motorEfficiency !in 0.0..1.0
            ) {
                errors +=
                    "Sewage pump ${pump.name}: invalid motor efficiency."
            }

            if (pump.motorPowerKw < 0.0) {
                errors +=
                    "Sewage pump ${pump.name}: invalid motor power."
            }

            if (
                pump.manufacturer.isBlank() ||
                pump.model.isBlank()
            ) {
                warnings +=
                    "Sewage pump ${pump.name}: manufacturer/model is not defined."
            }
        }

        if (
            sewage.pumps.isNotEmpty() &&
            sewage.pumps.none {
                it.duty
            }
        ) {
            errors +=
                "Sewage pumps exist but no duty pump is defined."
        }
    }
}
