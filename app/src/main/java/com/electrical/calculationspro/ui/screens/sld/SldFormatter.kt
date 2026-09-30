package com.electrical.calculationspro.ui.screens.sld

import java.util.Locale

/**
 * Single formatter for all SLD UI/drawing/report values.
 *
 * Keep formatting out of:
 * - SldCanvasDrawing
 * - SldCanvas
 * - SldEditorScreen
 * - Engineering engines
 *
 * This file is presentation-only.
 */

fun fmt(
    value: Double
): String {
    return if (value.isFinite()) {
        String.format(
            Locale.US,
            "%.2f",
            value
        )
    } else {
        "0.00"
    }
}

fun fmt(
    value: Float
): String {
    return if (value.isFinite()) {
        String.format(
            Locale.US,
            "%.2f",
            value
        )
    } else {
        "0.00"
    }
}

fun fmtPercent(
    value: Double
): String {
    return "${fmt(value)}%"
}

fun fmtKw(
    value: Double
): String {
    return "${fmt(value)} kW"
}

fun fmtKva(
    value: Double
): String {
    return "${fmt(value)} kVA"
}

fun fmtCurrent(
    value: Double
): String {
    return "${fmt(value)} A"
}

fun fmtVoltage(
    value: Double
): String {
    return "${fmt(value)} V"
}

fun fmtCableSize(
    sizeMm2: Double,
    parallelRuns: Int = 1
): String {

    if (sizeMm2 <= 0.0) {
        return "N/A"
    }

    val base =
        "${fmt(sizeMm2)} mm²"

    return if (parallelRuns > 1) {
        "$base × $parallelRuns"
    } else {
        base
    }
}

fun fmtLength(
    meters: Double
): String {
    return "${fmt(meters)} m"
}

fun fmtShortCircuit(
    valueKa: Double
): String {
    return "${fmt(valueKa)} kA"
}
