package com.electrical.calculationspro.ui.screens.sld

import java.util.Locale

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
