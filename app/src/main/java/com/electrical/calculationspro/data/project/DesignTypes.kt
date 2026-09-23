package com.electrical.calculationspro.data.project

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
    WATER_PIPE,
    WATER_PUMP,

    SEWAGE_NETWORK,
    SEWAGE_PIPE,
    SEWAGE_PUMP,
    WET_WELL,
    RISING_MAIN
}
