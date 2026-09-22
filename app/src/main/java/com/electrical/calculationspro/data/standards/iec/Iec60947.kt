package com.electrical.calculationspro.data.standards.iec

/**
 * IEC 60947 low-voltage switchgear and controlgear selection layer.
 *
 * Product-specific data belongs to the equipment catalog layer.
 */
object Iec60947 {

    const val STANDARD = "IEC 60947"

    enum class DeviceType {
        MCCB,
        ACB,
        MCB,
        SWITCH_DISCONNECTOR,
        CONTACTOR,
        MOTOR_PROTECTOR
    }

    data class DeviceRequirements(
        val type: DeviceType,
        val ratedVoltageV: Double,
        val designCurrentA: Double,
        val prospectiveShortCircuitKA: Double,
        val poles: Int = 3
    )

    data class DeviceAssessment(
        val acceptable: Boolean,
        val minimumCurrentRatingA: Double,
        val minimumBreakingCapacityKA: Double,
        val notes: List<String>
    )

    fun assess(
        requirements: DeviceRequirements,
        selectedCurrentRatingA: Double,
        selectedBreakingCapacityKA: Double
    ): DeviceAssessment {

        val notes = mutableListOf<String>()
        var acceptable = true

        if (requirements.ratedVoltageV <= 0.0) {
            notes += "Rated voltage must be greater than zero."
            acceptable = false
        }

        if (requirements.designCurrentA <= 0.0) {
            notes += "Design current must be greater than zero."
            acceptable = false
        }

        if (requirements.prospectiveShortCircuitKA < 0.0) {
            notes += "Prospective short-circuit current cannot be negative."
            acceptable = false
        }

        if (selectedCurrentRatingA < requirements.designCurrentA) {
            notes +=
                "Selected device current rating is below the design current."
            acceptable = false
        }

        if (selectedBreakingCapacityKA <
            requirements.prospectiveShortCircuitKA
        ) {
            notes +=
                "Selected device breaking capacity is below the prospective fault current."
            acceptable = false
        }

        if (requirements.poles !in 1..4) {
            notes += "Pole count must be between 1 and 4."
            acceptable = false
        }

        return DeviceAssessment(
            acceptable = acceptable,
            minimumCurrentRatingA = requirements.designCurrentA,
            minimumBreakingCapacityKA =
                requirements.prospectiveShortCircuitKA,
            notes = notes
        )
    }

    fun minimumCurrentRating(
        designCurrentA: Double,
        availableRatingsA: List<Double>
    ): Double? {

        if (designCurrentA <= 0.0) {
            return null
        }

        return availableRatingsA
            .filter { it > 0.0 }
            .sorted()
            .firstOrNull {
                it >= designCurrentA
            }
    }
}
