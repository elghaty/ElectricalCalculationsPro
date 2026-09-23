package com.electrical.calculationspro.data.project

/**
 * ================================================================
 * PROFESSIONAL DESIGN
 * Design Types
 * ================================================================
 *
 * Domain definitions only.
 * No UI.
 * No Compose.
 * No calculation formulas.
 * ================================================================
 */

enum class DesignDiscipline {
    ELECTRICAL,
    WATER,
    SEWAGE
}

enum class DesignStatus {
    DRAFT,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED
}

enum class DesignCalculationStatus {
    NOT_STARTED,
    IN_PROGRESS,
    CALCULATED,
    VERIFIED,
    DATA_INCOMPLETE,
    INVALID
}

enum class DesignElementType {
    PROJECT,
    ELECTRICAL_PANEL,
    ELECTRICAL_LOAD,
    CABLE,
    BREAKER,
    TRANSFORMER,
    GENERATOR,
    PROTECTION,
    SLD,
    WATER_NETWORK,
    SEWAGE_NETWORK,
    PIPE,
    PUMP,
    WET_WELL,
    RISING_MAIN
}
