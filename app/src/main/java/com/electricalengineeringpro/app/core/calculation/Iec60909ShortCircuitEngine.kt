package com.electricalengineeringpro.app.core.calculation

import com.electricalengineeringpro.app.core.shortcircuit.CalculationStatus
import com.electricalengineeringpro.app.core.shortcircuit.CatalogShortCircuitInput
import com.electricalengineeringpro.app.core.shortcircuit.ComplexImpedance
import com.electricalengineeringpro.app.core.shortcircuit.FaultType
import com.electricalengineeringpro.app.core.shortcircuit.ShortCircuitResult
import com.electricalengineeringpro.app.core.shortcircuit.TransformerCatalogData
import kotlin.math.sqrt

class Iec60909ShortCircuitEngine {

    fun calculate(
        input: CatalogShortCircuitInput
    ): ShortCircuitResult {

        val errors = mutableListOf<String>()

        val warnings = mutableListOf<String>()

        val trace = mutableListOf<String>()

        if (input.voltageV <= 0.0) {
            errors +=
                "System voltage must be greater than zero."
        }

        if (input.voltageFactorC <= 0.0) {
            errors +=
                "Voltage factor c must be greater than zero."
        }

        if (input.source.ratedVoltageV <= 0.0) {
            errors +=
                "Source rated voltage is invalid."
        }

        if (errors.isNotEmpty()) {
            return invalid(
                input = input,
                errors = errors
            )
        }

        val zSource =
            sourceZ1(
                input = input,
                trace = trace
            )
                ?: return incomplete(
                    input,
                    "Verified source short-circuit level plus X/R, or direct source Z1, is required.",
                    trace
                )

        val zTransformer =
            input.transformer?.let {

                transformerZ1(
                    transformer = it,
                    voltageV = input.voltageV,
                    trace = trace
                )
                    ?: return incomplete(
                        input,
                        "Transformer Uk% plus X/R, or direct transformer Z1, is required.",
                        trace
                    )

            } ?: ComplexImpedance(
                rOhm = 0.0,
                xOhm = 0.0
            )

        var zCable =
            ComplexImpedance(
                rOhm = 0.0,
                xOhm = 0.0
            )

        for (segment in input.cableSegments) {

            if (
                segment.lengthM <= 0.0 ||
                segment.parallelRuns <= 0
            ) {
                return incomplete(
                    input,
                    "Cable length and parallel runs must be positive.",
                    trace
                )
            }

            val r =
                segment.cable.resistanceOhmPerKm *
                    segment.lengthM /
                    1000.0 /
                    segment.parallelRuns

            val x =
                segment.cable.reactanceOhmPerKm *
                    segment.lengthM /
                    1000.0 /
                    segment.parallelRuns

            zCable += ComplexImpedance(
                rOhm = r,
                xOhm = x
            )

            trace +=
                "Cable catalog: " +
                    segment.cable.manufacturer +
                    " " +
                    segment.cable.model

            trace +=
                "Cable impedance: R=" +
                    r +
                    " ohm, X=" +
                    x +
                    " ohm"
        }

        val z1 =
            zSource +
                zTransformer +
                zCable

        if (z1.magnitudeOhm <= 0.0) {

            return incomplete(
                input,
                "Total positive-sequence impedance is zero.",
                trace
            )
        }

        val z0 =
            zeroSequence(
                input = input,
                trace = trace
            )

        val initialCurrentA =
            when (input.faultType) {

                FaultType.THREE_PHASE -> {

                    trace +=
                        "3-phase IEC network calculation."

                    input.voltageFactorC *
                        input.voltageV /
                        (
                            sqrt(3.0) *
                                z1.magnitudeOhm
                            )
                }

                FaultType.LINE_TO_LINE -> {

                    warnings +=
                        "Z2 is not separately supplied; Z2 = Z1 is used."

                    input.voltageFactorC *
                        input.voltageV /
                        (
                            sqrt(3.0) *
                                (
                                    z1.magnitudeOhm +
                                        z1.magnitudeOhm
                                    )
                            )
                }

                FaultType.LINE_TO_GROUND -> {

                    val z0Value =
                        z0
                            ?: return incomplete(
                                input,
                                "L-G fault requires complete zero-sequence catalog data.",
                                trace
                            )

                    sqrt(3.0) *
                        input.voltageFactorC *
                        input.voltageV /
                        (
                            z1.magnitudeOhm +
                                z1.magnitudeOhm +
                                z0Value.magnitudeOhm
                            )
                }

                FaultType.DOUBLE_LINE_TO_GROUND -> {

                    return ShortCircuitResult(
                        status =
                            CalculationStatus.UNSUPPORTED,

                        standard =
                            input.standard,

                        faultType =
                            input.faultType,

                        errors =
                            listOf(
                                "Complete positive-, negative- and zero-sequence network data are required for L-L-G."
                            ),

                        sourceReferences =
                            references(input)
                    )
                }
            }

        val motorContribution =
            input.includeMotorContributionA
                .coerceAtLeast(0.0)

        val totalCurrent =
            initialCurrentA +
                motorContribution

        val faultMva =
            sqrt(3.0) *
                input.voltageV *
                totalCurrent /
                1_000_000.0

        return ShortCircuitResult(
            status =
                CalculationStatus.VERIFIED,

            standard =
                input.standard,

            faultType =
                input.faultType,

            initialSymmetricalCurrentA =
                totalCurrent,

            initialSymmetricalCurrentKA =
                totalCurrent / 1000.0,

            faultMva =
                faultMva,

            positiveSequenceImpedanceOhm =
                z1,

            zeroSequenceImpedanceOhm =
                z0,

            warnings =
                warnings,

            calculationTrace =
                trace,

            sourceReferences =
                references(input)
        )
    }

    private fun sourceZ1(
        input: CatalogShortCircuitInput,
        trace: MutableList<String>
    ): ComplexImpedance? {

        input.source.positiveSequence?.let {

            trace +=
                "Source Z1 taken directly from catalog data."

            return it
        }

        if (
            input.source.shortCircuitMva <= 0.0
        ) {
            return null
        }

        val z =
            input.voltageV *
                input.voltageV /
                (
                    input.source.shortCircuitMva *
                        1_000_000.0
                    )

        val xr =
            input.source.xrRatio
                ?: return null

        val r =
            z /
                sqrt(
                    1.0 +
                        xr * xr
                )

        val x =
            r * xr

        trace +=
            "Source Z1 derived from catalog Sk and X/R."

        return ComplexImpedance(
            rOhm = r,
            xOhm = x
        )
    }

    private fun transformerZ1(
        transformer: TransformerCatalogData,
        voltageV: Double,
        trace: MutableList<String>
    ): ComplexImpedance? {

        transformer.positiveSequence?.let {

            trace +=
                "Transformer Z1 taken directly from catalog data."

            return it
        }

        if (
            transformer.ratedPowerKva <= 0.0 ||
            transformer.ukPercent <= 0.0
        ) {
            return null
        }

        val xr =
            transformer.xrRatio
                ?: return null

        val z =
            (
                transformer.ukPercent /
                    100.0
                ) *
                voltageV *
                voltageV /
                (
                    transformer.ratedPowerKva *
                        1000.0
                    )

        val r =
            z /
                sqrt(
                    1.0 +
                        xr * xr
                )

        val x =
            r * xr

        trace +=
            "Transformer Z1 derived from catalog Uk and X/R."

        return ComplexImpedance(
            rOhm = r,
            xOhm = x
        )
    }

    private fun zeroSequence(
        input: CatalogShortCircuitInput,
        trace: MutableList<String>
    ): ComplexImpedance? {

        val source =
            input.source.zeroSequence
                ?: return null

        val transformer =
            input.transformer
                ?.zeroSequence
                ?: return null

        var cable =
            ComplexImpedance(
                rOhm = 0.0,
                xOhm = 0.0
            )

        for (segment in input.cableSegments) {

            val r =
                segment.cable
                    .zeroSequenceResistanceOhmPerKm
                    ?: return null

            val x =
                segment.cable
                    .zeroSequenceReactanceOhmPerKm
                    ?: return null

            cable +=
                ComplexImpedance(
                    rOhm =
                        r *
                            segment.lengthM /
                            1000.0 /
                            segment.parallelRuns,

                    xOhm =
                        x *
                            segment.lengthM /
                            1000.0 /
                            segment.parallelRuns
                )
        }

        trace +=
            "Zero-sequence network assembled from catalog data."

        return source +
            transformer +
            cable
    }

    private fun references(
        input: CatalogShortCircuitInput
    ): List<String> {

        return buildList {

            add(
                "IEC 60909-0:2026"
            )

            add(
                input.source.catalogReference
            )

            input.transformer?.let {
                add(
                    it.catalogReference
                )
            }

            input.cableSegments.forEach {
                add(
                    it.cable.catalogReference
                )
            }

        }.distinct()
    }

    private fun incomplete(
        input: CatalogShortCircuitInput,
        message: String,
        trace: List<String>
    ): ShortCircuitResult {

        return ShortCircuitResult(
            status =
                CalculationStatus.DATA_INCOMPLETE,

            standard =
                input.standard,

            faultType =
                input.faultType,

            errors =
                listOf(message),

            calculationTrace =
                trace,

            sourceReferences =
                references(input)
        )
    }

    private fun invalid(
        input: CatalogShortCircuitInput,
        errors: List<String>
    ): ShortCircuitResult {

        return ShortCircuitResult(
            status =
                CalculationStatus.INVALID_INPUT,

            standard =
                input.standard,

            faultType =
                input.faultType,

            errors =
                errors,

            sourceReferences =
                references(input)
        )
    }
}
