package com.electrical.calculationspro.data

/**

* ================================================================
* SLD ENGINEERING FACADE
* ================================================================
* 
* Single entry point between the SLD/UI layer and engineering
* calculation engines.
* 
* UI
* ↓
* SldEngineeringFacade
* ↓
* Validation
* ↓
* Topology
* ↓
* Upstream Engineering
* ↓
* Short Circuit
* ↓
* Cable Sizing
* ↓
* Protection Coordination
* ↓
* Panel Schedule
* 
* This class contains orchestration only.
* 
* No engineering formulas are implemented here.
* 
* ================================================================
  */

object SldEngineeringFacade {

// ============================================================
// VALIDATION
// ============================================================

fun validate(
    network: SldNetwork
): SldDesignValidator.Result {

    return SldDesignValidator.validate(
        network
    )
}

// ============================================================
// UPSTREAM ENGINEERING
// ============================================================

fun calculateUpstream(
    network: SldNetwork
): SldUpstreamEngineering.Result {

    validateNetwork(
        network
    )

    return SldUpstreamEngineering.calculate(
        network
    )
}

// ============================================================
// SHORT CIRCUIT
// ============================================================

fun calculateShortCircuit(
    network: SldNetwork,
    voltageFactor: Double = 1.05
): SldShortCircuitStudy {

    validateNetwork(
        network
    )

    require(
        voltageFactor > 0.0
    ) {
        "Voltage factor must be greater than zero."
    }

    return SldShortCircuitEngine.calculate(
        network = network,
        voltageFactor = voltageFactor
    )
}

// ============================================================
// CABLE SIZING
// ============================================================

fun calculateCableSizing(
    network: SldNetwork,
    shortCircuitStudy: SldShortCircuitStudy? = null,
    voltageDropLimitPercent: Double = 3.0,
    shortCircuitTimeSeconds: Double = 1.0
): SldCableSizingStudy {

    validateNetwork(
        network
    )

    require(
        voltageDropLimitPercent > 0.0
    ) {
        "Voltage drop limit must be greater than zero."
    }

    require(
        shortCircuitTimeSeconds > 0.0
    ) {
        "Short-circuit clearing time must be greater than zero."
    }

    val shortCircuit =
        shortCircuitStudy
            ?: SldShortCircuitEngine.calculate(
                network = network
            )

    return SldCableSizingEngine.calculate(
        network = network,
        shortCircuitStudy =
            shortCircuit,
        voltageDropLimitPercent =
            voltageDropLimitPercent,
        shortCircuitTimeSeconds =
            shortCircuitTimeSeconds
    )
}

// ============================================================
// PANEL SCHEDULE
// ============================================================

fun calculatePanelSchedule(
    network: SldNetwork,
    panelNodeId: String,
    cableSizingStudy: SldCableSizingStudy? = null,
    shortCircuitStudy: SldShortCircuitStudy? = null
): SldPanelSchedule {

    validateNetwork(
        network
    )

    require(
        network.nodes.any {
            it.id == panelNodeId &&
                it.type == SldNodeType.PANEL
        }
    ) {
        "Panel node '$panelNodeId' was not found."
    }

    return SldPanelScheduleEngine.calculate(
        network = network,
        panelNodeId = panelNodeId,
        cableSizingStudy =
            cableSizingStudy,
        shortCircuitStudy =
            shortCircuitStudy
    )
}

// ============================================================
// PROTECTION COORDINATION
// ============================================================

fun calculateProtectionCoordination(
    network: SldNetwork,
    shortCircuitStudy: SldShortCircuitStudy? = null,
    cableSizingStudy: SldCableSizingStudy? = null
): SldProtectionCoordinationResult {

    validateNetwork(
        network
    )

    val shortCircuit =
        shortCircuitStudy
            ?: SldShortCircuitEngine.calculate(
                network = network
            )

    val cableSizing =
        cableSizingStudy
            ?: SldCableSizingEngine.calculate(
                network = network,
                shortCircuitStudy =
                    shortCircuit
            )

    return SldProtectionCoordinationEngine.calculate(
        network = network,
        shortCircuitStudy =
            shortCircuit,
        cableSizingStudy =
            cableSizing
    )
}

// ============================================================
// COMPLETE ENGINEERING STUDY
// ============================================================

/**
 * Performs one complete engineering calculation cycle.
 *
 * Calculation order:
 *
 * 1. Validation
 * 2. Upstream engineering
 * 3. Short circuit
 * 4. Cable sizing
 * 5. Protection coordination
 * 6. Panel schedule
 *
 * The resulting studies are assembled into one immutable
 * SldEngineeringPackage.
 *
 * This function is the preferred entry point for the SLD editor
 * after a node, connection, cable, source or load is modified.
 */
fun calculateComplete(
    network: SldNetwork,
    panelNodeId: String? = null,
    voltageFactor: Double = 1.05,
    voltageDropLimitPercent: Double = 3.0,
    shortCircuitTimeSeconds: Double = 1.0
): SldEngineeringPackage {

    validateNetwork(
        network
    )

    // --------------------------------------------------------
    // 1. UPSTREAM ENGINEERING
    // --------------------------------------------------------

    val upstream =
        SldUpstreamEngineering.calculate(
            network
        )

    // --------------------------------------------------------
    // 2. SHORT CIRCUIT
    // --------------------------------------------------------

    val shortCircuit =
        SldShortCircuitEngine.calculate(
            network = network,
            voltageFactor =
                voltageFactor
        )

    // --------------------------------------------------------
    // 3. CABLE SIZING
    // --------------------------------------------------------

    val cableSizing =
        SldCableSizingEngine.calculate(
            network = network,
            shortCircuitStudy =
                shortCircuit,
            voltageDropLimitPercent =
                voltageDropLimitPercent,
            shortCircuitTimeSeconds =
                shortCircuitTimeSeconds
        )

    // --------------------------------------------------------
    // 4. PROTECTION COORDINATION
    // --------------------------------------------------------

    val protection =
        SldProtectionCoordinationEngine.calculate(
            network = network,
            shortCircuitStudy =
                shortCircuit,
            cableSizingStudy =
                cableSizing
        )

    // --------------------------------------------------------
    // 5. PANEL SCHEDULE
    // --------------------------------------------------------

    val panelSchedule =
        panelNodeId?.let { id ->

            calculatePanelSchedule(
                network = network,
                panelNodeId = id,
                cableSizingStudy =
                    cableSizing,
                shortCircuitStudy =
                    shortCircuit
            )
        }

    // --------------------------------------------------------
    // 6. FINAL ENGINEERING PACKAGE
    // --------------------------------------------------------

    return SldEngineeringPackage(
        upstream =
            upstream,

        shortCircuit =
            shortCircuit,

        cableSizing =
            cableSizing,

        protectionCoordination =
            protection,

        panelSchedule =
            panelSchedule
    )
}

// ============================================================
// NETWORK VALIDATION
// ============================================================

private fun validateNetwork(
    network: SldNetwork
) {

    require(
        network.nodes.isNotEmpty()
    ) {
        "SLD network is empty."
    }

    val validation =
        SldDesignValidator.validate(
            network
        )

    require(
        validation.valid
    ) {
        buildValidationMessage(
            validation
        )
    }
}

// ============================================================
// VALIDATION MESSAGE
// ============================================================

private fun buildValidationMessage(
    validation:
        SldDesignValidator.Result
): String {

    return buildString {

        appendLine(
            "SLD topology validation failed."
        )

        if (
            validation.errors.isNotEmpty()
        ) {

            appendLine()

            appendLine(
                "Errors:"
            )

            validation.errors.forEach {
                appendLine(
                    "- [${it.code}] ${it.message}"
                )
            }
        }

        if (
            validation.warnings.isNotEmpty()
        ) {

            appendLine()

            appendLine(
                "Warnings:"
            )

            validation.warnings.forEach {
                appendLine(
                    "- [${it.code}] ${it.message}"
                )
            }
        }
    }
}

}

/**

* ================================================================
* COMPLETE SLD ENGINEERING RESULT
* ================================================================
* 
* This is the single engineering package consumed by the SLD UI,
* reports and project bridge.
* 
* Do not rename or remove these fields without updating every
* consumer in the same change.
* 
* ================================================================
  */

data class SldEngineeringPackage(

val upstream:
    SldUpstreamEngineering.Result,

val shortCircuit:
    SldShortCircuitStudy,

val cableSizing:
    SldCableSizingStudy,

val protectionCoordination:
    SldProtectionCoordinationResult,

val panelSchedule:
    SldPanelSchedule?

)
