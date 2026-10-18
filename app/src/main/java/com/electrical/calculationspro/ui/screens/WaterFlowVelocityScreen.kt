package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.water.WaterDesignEngine
import com.electrical.calculationspro.ui.components.EngineeringEmptyState
import com.electrical.calculationspro.ui.components.EngineeringInput
import com.electrical.calculationspro.ui.components.EngineeringPage
import com.electrical.calculationspro.ui.components.EngineeringPrimaryButton
import com.electrical.calculationspro.ui.components.EngineeringResult
import com.electrical.calculationspro.ui.components.EngineeringSecondaryButton
import com.electrical.calculationspro.ui.components.EngineeringSection
import com.electrical.calculationspro.ui.components.EngineeringStatus
import com.electrical.calculationspro.ui.components.FourColumnFields

private enum class HydraulicCalculationMode {
    VELOCITY_FROM_FLOW,
    FLOW_FROM_VELOCITY,
    DIAMETER_FROM_FLOW_AND_VELOCITY
}

@Composable
fun WaterFlowVelocityScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var mode by remember {
        mutableStateOf(
            HydraulicCalculationMode.VELOCITY_FROM_FLOW
        )
    }

    var flow by remember { mutableStateOf("") }
    var diameter by remember { mutableStateOf("") }
    var velocity by remember { mutableStateOf("") }
    var resultTitle by remember { mutableStateOf<String?>(null) }
    var resultValue by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun clearResult() {
        resultTitle = null
        resultValue = null
        error = null
    }

    EngineeringPage(
        title = if (arabic) {
            "حسابات التصرف والسرعة والقطر"
        } else {
            "Flow, Velocity & Diameter Calculator"
        },
        subtitle = if (arabic) {
            "حساب أي قيمة من القيم الثلاث باستخدام القيمتين الأخريين"
        } else {
            "Calculate any one value from the other two"
        },
        onBack = onBack
    ) {
        EngineeringSection(
            title = if (arabic) "نوع الحساب" else "Calculation Mode"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineeringSecondaryButton(
                    text = if (arabic) {
                        "حساب السرعة"
                    } else {
                        "Calculate Velocity"
                    },
                    modifier = Modifier.weight(1f),
                    enabled = mode !=
                        HydraulicCalculationMode.VELOCITY_FROM_FLOW,
                    onClick = {
                        mode =
                            HydraulicCalculationMode.VELOCITY_FROM_FLOW
                        clearResult()
                    }
                )

                EngineeringSecondaryButton(
                    text = if (arabic) {
                        "حساب التصرف"
                    } else {
                        "Calculate Flow"
                    },
                    modifier = Modifier.weight(1f),
                    enabled = mode !=
                        HydraulicCalculationMode.FLOW_FROM_VELOCITY,
                    onClick = {
                        mode =
                            HydraulicCalculationMode.FLOW_FROM_VELOCITY
                        clearResult()
                    }
                )
            }

            EngineeringSecondaryButton(
                text = if (arabic) {
                    "حساب القطر الداخلي"
                } else {
                    "Calculate Internal Diameter"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = mode !=
                    HydraulicCalculationMode.DIAMETER_FROM_FLOW_AND_VELOCITY,
                onClick = {
                    mode =
                        HydraulicCalculationMode.DIAMETER_FROM_FLOW_AND_VELOCITY
                    clearResult()
                }
            )

            EngineeringStatus(
                text = when (mode) {
                    HydraulicCalculationMode.VELOCITY_FROM_FLOW ->
                        if (arabic) {
                            "أدخل التصرف والقطر الداخلي لحساب السرعة."
                        } else {
                            "Enter flow and internal diameter to calculate velocity."
                        }

                    HydraulicCalculationMode.FLOW_FROM_VELOCITY ->
                        if (arabic) {
                            "أدخل السرعة والقطر الداخلي لحساب التصرف."
                        } else {
                            "Enter velocity and internal diameter to calculate flow."
                        }

                    HydraulicCalculationMode.DIAMETER_FROM_FLOW_AND_VELOCITY ->
                        if (arabic) {
                            "أدخل التصرف والسرعة لحساب القطر الداخلي."
                        } else {
                            "Enter flow and velocity to calculate internal diameter."
                        }
                }
            )
        }

        EngineeringSection(
            title = if (arabic) "بيانات الماسورة" else "Pipe Data"
        ) {
            FourColumnFields {
                if (
                    mode !=
                    HydraulicCalculationMode.FLOW_FROM_VELOCITY
                ) {
                    item {
                        EngineeringInput(
                            value = flow,
                            label = if (arabic) {
                                "التصرف Q (م³/ساعة)"
                            } else {
                                "Flow Q (m³/h)"
                            },
                            onValueChange = {
                                flow = it
                                clearResult()
                            }
                        )
                    }
                }

                if (
                    mode !=
                    HydraulicCalculationMode.VELOCITY_FROM_FLOW
                ) {
                    item {
                        EngineeringInput(
                            value = velocity,
                            label = if (arabic) {
                                "السرعة V (م/ث)"
                            } else {
                                "Velocity V (m/s)"
                            },
                            onValueChange = {
                                velocity = it
                                clearResult()
                            }
                        )
                    }
                }

                if (
                    mode !=
                    HydraulicCalculationMode.DIAMETER_FROM_FLOW_AND_VELOCITY
                ) {
                    item {
                        EngineeringInput(
                            value = diameter,
                            label = if (arabic) {
                                "القطر الداخلي D (مم)"
                            } else {
                                "Internal Diameter D (mm)"
                            },
                            onValueChange = {
                                diameter = it
                                clearResult()
                            }
                        )
                    }
                }
            }

            EngineeringStatus(
                text = if (arabic) {
                    "استخدم القطر الداخلي الفعلي للماسورة، وليس القطر الاسمي."
                } else {
                    "Use the actual internal pipe diameter, not the nominal diameter."
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) "احسب" else "Calculate",
            onClick = {
                clearResult()

                when (mode) {
                    HydraulicCalculationMode.VELOCITY_FROM_FLOW -> {
                        val flowValue = flow.toDoubleOrNull()
                        val diameterValue = diameter.toDoubleOrNull()

                        if (
                            flowValue == null ||
                            !flowValue.isFinite() ||
                            flowValue <= 0.0 ||
                            diameterValue == null ||
                            !diameterValue.isFinite() ||
                            diameterValue <= 0.0
                        ) {
                            error = if (arabic) {
                                "أدخل تصرفًا وقطرًا داخليًا صحيحين أكبر من صفر."
                            } else {
                                "Enter valid positive flow and internal diameter values."
                            }
                            return@EngineeringPrimaryButton
                        }

                        val calculatedVelocity =
                            WaterDesignEngine.calculateVelocity(
                                flowM3PerHour = flowValue,
                                diameterMm = diameterValue
                            )

                        if (
                            !calculatedVelocity.isFinite() ||
                            calculatedVelocity <= 0.0
                        ) {
                            error = if (arabic) {
                                "تعذر حساب السرعة. راجع التصرف والقطر."
                            } else {
                                "Velocity calculation failed. Check flow and diameter."
                            }
                            return@EngineeringPrimaryButton
                        }

                        velocity = formatHydraulicValue(calculatedVelocity)

                        resultTitle = if (arabic) {
                            "السرعة المحسوبة"
                        } else {
                            "Calculated Velocity"
                        }

                        resultValue =
                            "${formatHydraulicValue(calculatedVelocity)} m/s"
                    }

                    HydraulicCalculationMode.FLOW_FROM_VELOCITY -> {
                        val velocityValue = velocity.toDoubleOrNull()
                        val diameterValue = diameter.toDoubleOrNull()

                        if (
                            velocityValue == null ||
                            !velocityValue.isFinite() ||
                            velocityValue <= 0.0 ||
                            diameterValue == null ||
                            !diameterValue.isFinite() ||
                            diameterValue <= 0.0
                        ) {
                            error = if (arabic) {
                                "أدخل سرعة وقطرًا داخليًا صحيحين أكبر من صفر."
                            } else {
                                "Enter valid positive velocity and internal diameter values."
                            }
                            return@EngineeringPrimaryButton
                        }

                        val calculatedFlow =
                            WaterDesignEngine.calculateFlow(
                                diameterMm = diameterValue,
                                velocityMPerS = velocityValue
                            )

                        if (
                            !calculatedFlow.isFinite() ||
                            calculatedFlow <= 0.0
                        ) {
                            error = if (arabic) {
                                "تعذر حساب التصرف. راجع السرعة والقطر."
                            } else {
                                "Flow calculation failed. Check velocity and diameter."
                            }
                            return@EngineeringPrimaryButton
                        }

                        flow = formatHydraulicValue(calculatedFlow)

                        resultTitle = if (arabic) {
                            "التصرف المحسوب"
                        } else {
                            "Calculated Flow"
                        }

                        resultValue =
                            "${formatHydraulicValue(calculatedFlow)} m³/h"
                    }

                    HydraulicCalculationMode.DIAMETER_FROM_FLOW_AND_VELOCITY -> {
                        val flowValue = flow.toDoubleOrNull()
                        val velocityValue = velocity.toDoubleOrNull()

                        if (
                            flowValue == null ||
                            !flowValue.isFinite() ||
                            flowValue <= 0.0 ||
                            velocityValue == null ||
                            !velocityValue.isFinite() ||
                            velocityValue <= 0.0
                        ) {
                            error = if (arabic) {
                                "أدخل تصرفًا وسرعة صحيحين أكبر من صفر."
                            } else {
                                "Enter valid positive flow and velocity values."
                            }
                            return@EngineeringPrimaryButton
                        }

                        val calculatedDiameter =
                            WaterDesignEngine.calculateDiameter(
                                flowM3PerHour = flowValue,
                                velocityMPerS = velocityValue
                            )

                        if (
                            !calculatedDiameter.isFinite() ||
                            calculatedDiameter <= 0.0
                        ) {
                            error = if (arabic) {
                                "تعذر حساب القطر. راجع التصرف والسرعة."
                            } else {
                                "Diameter calculation failed. Check flow and velocity."
                            }
                            return@EngineeringPrimaryButton
                        }

                        diameter = formatHydraulicValue(calculatedDiameter)

                        resultTitle = if (arabic) {
                            "القطر الداخلي المحسوب"
                        } else {
                            "Calculated Internal Diameter"
                        }

                        resultValue =
                            "${formatHydraulicValue(calculatedDiameter)} mm"
                    }
                }
            }
        )

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        if (resultTitle != null && resultValue != null) {
            EngineeringSection(
                title = if (arabic) {
                    "نتائج الحساب"
                } else {
                    "Calculation Results"
                }
            ) {
                EngineeringResult(
                    title = resultTitle!!,
                    value = resultValue!!
                )

                EngineeringResult(
                    title = if (arabic) "التصرف Q" else "Flow Q",
                    value = "${flow.ifBlank { "—" }} m³/h"
                )

                EngineeringResult(
                    title = if (arabic) "السرعة V" else "Velocity V",
                    value = "${velocity.ifBlank { "—" }} m/s"
                )

                EngineeringResult(
                    title = if (arabic) {
                        "القطر الداخلي D"
                    } else {
                        "Internal Diameter D"
                    },
                    value = "${diameter.ifBlank { "—" }} mm"
                )
            }
        } else if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) {
                    "لا توجد نتيجة بعد"
                } else {
                    "No Result Yet"
                },
                message = if (arabic) {
                    "اختر نوع الحساب وأدخل القيم المطلوبة ثم اضغط احسب."
                } else {
                    "Select a calculation mode, enter the required values, and calculate."
                }
            )
        }
    }
}

private fun formatHydraulicValue(value: Double): String =
    String.format(java.util.Locale.US, "%.4f", value)
