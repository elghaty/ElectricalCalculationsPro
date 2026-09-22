package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL ENGINEERING CORE
 * SLD FACADE
 * ================================================================
 *
 * UI
 *  ↓
 * ElectricalCalculations
 *  ↓
 * SldEngineeringFacade
 *  ↓
 * SLD Engineering Engines
 *
 * The UI must not access SLD engines directly.
 * ================================================================
 */
object SldEngineeringFacade {

    /**
     * ------------------------------------------------------------
     * SHORT CIRCUIT
     * ------------------------------------------------------------
     */
    fun calculateShortCircuit(
        network: SldNetwork,
        voltageFactor: Double = 1.05
    ): SldShortCircuitStudy {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        return SldShortCircuitEngine.calculate(
            network = network,
            voltageFactor = voltageFactor
        )
    }

    /**
     * ------------------------------------------------------------
     * CABLE SIZING
     * ------------------------------------------------------------
     */
    fun calculateCableSizing(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldCableSizingStudy {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        return SldCableSizingEngine.calculate(
            network = network,
            shortCircuitStudy = shortCircuitStudy,
            voltageDropLimitPercent = voltageDropLimitPercent,
            shortCircuitTimeSeconds = shortCircuitTimeSeconds
        )
    }

    /**
     * ------------------------------------------------------------
     * PANEL SCHEDULE
     * ------------------------------------------------------------
     */
    fun calculatePanelSchedule(
        network: SldNetwork,
        panelNodeId: String,
        cableSizingStudy: SldCableSizingStudy? = null,
        shortCircuitStudy: SldShortCircuitStudy? = null
    ): SldPanelSchedule {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        return SldPanelScheduleEngine.calculate(
            network = network,
            panelNodeId = panelNodeId,
            cableSizingStudy = cableSizingStudy,
            shortCircuitStudy = shortCircuitStudy
        )
    }

    /**
     * ------------------------------------------------------------
     * PROTECTION COORDINATION
     * ------------------------------------------------------------
     */
    fun calculateProtectionCoordination(
        network: SldNetwork,
        shortCircuitStudy: SldShortCircuitStudy? = null,
        cableSizingStudy: SldCableSizingStudy? = null
    ): SldProtectionCoordinationResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        return SldProtectionCoordinationEngine.calculate(
            network = network,
            shortCircuitStudy = shortCircuitStudy,
            cableSizingStudy = cableSizingStudy
        )
    }

    /**
     * ------------------------------------------------------------
     * COMPLETE SLD ENGINEERING STUDY
     * ------------------------------------------------------------
     *
     * The complete workflow is:
     *
     * 1. Short Circuit
     * 2. Cable Sizing
     * 3. Protection Coordination
     * 4. Panel Schedule when a panel is supplied
     */
    fun calculateComplete(
        network: SldNetwork,
        panelNodeId: String? = null,
        voltageFactor: Double = 1.05,
        voltageDropLimitPercent: Double = 3.0,
        shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val shortCircuitStudy =
            calculateShortCircuit(
                network = network,
                voltageFactor = voltageFactor
            )

        val cableSizingStudy =
            calculateCableSizing(
                network = network,
                shortCircuitStudy = shortCircuitStudy,
                voltageDropLimitPercent =
                    voltageDropLimitPercent,
                shortCircuitTimeSeconds =
                    shortCircuitTimeSeconds
            )

        val protectionStudy =
            calculateProtectionCoordination(
                network = network,
                shortCircuitStudy = shortCircuitStudy,
                cableSizingStudy = cableSizingStudy
            )

        val panelSchedule =
            panelNodeId?.let { id ->

                calculatePanelSchedule(
                    network = network,
                    panelNodeId = id,
                    cableSizingStudy =
                        cableSizingStudy,
                    shortCircuitStudy =
                        shortCircuitStudy
                )
            }

        return SldEngineeringPackage(
            shortCircuit =
                shortCircuitStudy,
            cableSizing =
                cableSizingStudy,
            protectionCoordination =
                protectionStudy,
            panelSchedule =
                panelSchedule
        )
    }
}

/**
 * ================================================================
 * COMPLETE SLD ENGINEERING RESULT
 * ================================================================
 */
data class SldEngineeringPackage(
    val shortCircuit: SldShortCircuitStudy,
    val cableSizing: SldCableSizingStudy,
    val protectionCoordination: SldProtectionCoordinationResult,
    val panelSchedule: SldPanelSchedule?
)
