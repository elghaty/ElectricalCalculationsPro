package com.electrical.calculationspro.data

/**
 * Single engineering entry point for the SLD.
 *
 * The SLD UI must communicate with the engineering layer
 * through this facade instead of performing calculations itself.
 */
object SldEngineeringFacade {

    fun calculateUpstream(
        network: SldNetwork
    ): SldCalculationResult {
        return SldUpstreamCalculationEngine.calculate(
            network
        )
    }

    fun calculate(
        network: SldNetwork
    ): SldCalculationResult {
        return calculateUpstream(network)
    }

    fun resultForNode(
        result: SldCalculationResult,
        nodeId: String
    ): UpstreamResult? {
        return result.nodeResults[nodeId]
    }

    fun totalConnectedLoadKw(
        result: SldCalculationResult
    ): Double {
        return result.totalConnectedLoadKw
    }

    fun totalDemandLoadKw(
        result: SldCalculationResult
    ): Double {
        return result.totalDemandLoadKw
    }

    fun totalRequiredKva(
        result: SldCalculationResult
    ): Double {
        return result.totalRequiredKva
    }

    fun mainCurrentA(
        result: SldCalculationResult
    ): Double {
        return result.mainCurrentA
    }

    fun mainBreakerA(
        result: SldCalculationResult
    ): Double {
        return result.mainBreakerA
    }

    fun requiredTransformerKva(
        result: SldCalculationResult
    ): Double {
        return result.requiredTransformerKva
    }
}
