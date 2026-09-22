package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.Standard

/**
 * Single factory responsible for resolving the engineering
 * standard selected by the user.
 *
 * The rest of the application should not instantiate individual
 * code engines directly.
 */
object CodeEngineFactory {

    private val egyptianEngine =
        EgyptianCodeEngine()

    private val iecEngine =
        IecEngine()

    private val necEngine =
        NecEngine()

    private val ceiEngine =
        IecEngine(
            standardOverride = Standard.CEI,
            codeNameOverride = "CEI 64-8"
        )

    private val cecEngine =
        NecEngine(
            standardOverride = Standard.CEC,
            codeNameOverride = "Canadian Electrical Code"
        )

    fun get(
        standard: Standard
    ): StandardEngine {

        return when (standard) {

            Standard.EGYPTIAN ->
                egyptianEngine

            Standard.IEC ->
                iecEngine

            Standard.NEC ->
                necEngine

            Standard.CEI ->
                ceiEngine

            Standard.CEC ->
                cecEngine
        }
    }

    fun default(): StandardEngine =
        egyptianEngine
}
