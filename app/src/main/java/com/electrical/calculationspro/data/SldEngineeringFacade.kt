package com.electrical.calculationspro.data

/**
 * ================================================================
 * PROFESSIONAL SLD ENGINEERING FACADE
 * ================================================================
 *
 * UI
 *  ↓
 * ElectricalCalculations
 *  ↓
 * SldEngineeringFacade
 *  ↓
 * ┌─────────────────────────────────────────────┐
 * │ SldShortCircuitEngine                       │
 * │ SldCableSizingEngine                        │
 * │ SldPanelScheduleEngine                      │
 * │ SldProtectionCoordinationEngine             │
 * └─────────────────────────────────────────────┘
 *
 * The UI must never call an SLD calculation engine directly.
 *
 * This facade is intentionally located in the Professional
 * Engineering Core package.
 * ================================================================
 */
object SldEngineeringFacade {

    /**
     * Complete SLD short-circuit study.
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
     * Complete SLD cable sizing study.
     */
    fun calculateCableSizing(
        network: SldNetwork
    ): SldCableSizingStudy {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        return SldCableSizingEngine.calculate(
            network = network
        )
    }

    /**
     * Complete panel schedule study.
     */
    fun calculatePanelSchedule(
        network: SldNetwork,
        panelNodeId: String? = null
    ): SldPanelScheduleResult {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        return if (panelNodeId != null) {
            SldPanelScheduleEngine.calculate(
                network = network,
                panelNodeId = panelNodeId
            )
        } else {
            SldPanelScheduleEngine.calculate(
                network = network
            )
        }
    }

    /**
     * Complete protection coordination study.
     */
    fun calculateProtectionCoordination(
        network: SldNetwork,
        voltageFactor: Double = 1.05
    ): SldProtectionCoordinationResult {

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
                network = network
            )

        return SldProtectionCoordinationEngine.calculate(
            network = network,
            shortCircuitStudy = shortCircuitStudy,
            cableSizingStudy = cableSizingStudy
        )
    }

    /**
     * Complete SLD engineering package.
     *
     * This is the main API that should be used by the UI.
     */
    fun calculateComplete(
        network: SldNetwork,
        voltageFactor: Double = 1.05,
        panelNodeId: String? = null
    ): SldEngineeringPackage {

        require(network.nodes.isNotEmpty()) {
            "SLD network is empty."
        }

        val shortCircuit =
            calculateShortCircuit(
                network = network,
                voltageFactor = voltageFactor
            )

        val cableSizing =
            calculateCableSizing(
                network = network
            )

        val panelSchedule =
            calculatePanelSchedule(
                network = network,
                panelNodeId = panelNodeId
            )

        val protection =
            SldProtectionCoordinationEngine.calculate(
                network = network,
                shortCircuitStudy = shortCircuit,
                cableSizingStudy = cableSizing
            )

        return SldEngineeringPackage(
            shortCircuit = shortCircuit,
            cableSizing = cableSizing,
            panelSchedule = panelSchedule,
            protectionCoordination = protection
        )
    }
}

/**
 * Unified result returned by the SLD Professional Core.
 */
data class SldEngineeringPackage(
    val shortCircuit: SldShortCircuitStudy,
    val cableSizing: SldCableSizingStudy,
    val panelSchedule: SldPanelScheduleResult,
    val protectionCoordination: SldProtectionCoordinationResult
)
