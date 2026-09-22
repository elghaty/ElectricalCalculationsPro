package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.Standard

/**
 * Metadata attached to engineering calculation results.
 *
 * This allows reports and the UI to show exactly which code was
 * selected and whether the underlying dataset is complete.
 */
data class StandardResultMetadata(
    val standard: Standard,
    val codeName: String,
    val codeRevision: String,
    val fullyImplemented: Boolean,
    val implementationStatus: String
)
