package com.electrical.calculationspro.ui.screens.sld

import java.util.Locale

/**
 * Single formatter for the complete SLD package.
 *
 * No other SLD file should declare fmt().
 */
fun fmt(value: Double): String {
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
