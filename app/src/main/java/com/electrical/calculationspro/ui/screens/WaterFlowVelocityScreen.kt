
package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

private enum class HydraulicCalculationMode {
    VELOCITY_FROM_FLOW,
    FLOW_FROM_VELOCITY
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
            "حساب التصرف والسرعة"
        } else {
            "Flow and Velocity Calculator"
        },
        subtitle = if (arabic) {
            "حساب هيدروليكي باستخدام التصرف والقطر أو السرعة والقطر"
        } else {
            "Hydraulic calculation from flow and diameter, or velocity and diameter"
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
                        mode = HydraulicCalculationMode.VELOCITY_FROM_FLOW
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
                        mode = HydraulicCalculationMode.FLOW_FROM_VELOCITY
                        clearResult()
                    }
                )
            }

            EngineeringStatus(
                text = when (mode) {
                    HydraulicCalculationMode.VELOCITY_FROM_FLOW ->
                        if (arabic) {
                            "أدخل التصرف والقطر لحساب السرعة."
                        } else {
                            "Enter flow and diameter to calculate velocity."
                        }

                    HydraulicCalculationMode.FLOW_FROM_VELOCITY ->
                        if (arabic) {
                            "أدخل السرعة والقطر لحساب التصرف."
                        } else {
                            "Enter velocity and diameter to calculate flow."
                        }
                }
            )
        }

        EngineeringSection(
            title = if (arabic) "بيانات الماسورة" else "Pipe Data"
        ) {
            if (
                mode ==
                HydraulicCalculationMode.VELOCITY_FROM_FLOW
            ) {
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
            } else {
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
                val diameterValue = diameter.toDoubleOrNull()

                if (
                    diameterValue == null ||
                    !diameterValue.isFinite() ||
                    diameterValue <= 0.0
                ) {
                    clearResult()
                    error = if (arabic) {
                        "أدخل قطرًا داخليًا صحيحًا أكبر من صفر."
                    } else {
                        "Enter a valid internal diameter greater than zero."
                    }
                    return@EngineeringPrimaryButton
                }

                when (mode) {
                    HydraulicCalculationMode.VELOCITY_FROM_FLOW -> {
                        val flowValue = flow.toDoubleOrNull()

                        if (
                            flowValue == null ||
                            !flowValue.isFinite() ||
                            flowValue <= 0.0
                        ) {
                            clearResult()
                            error = if (arabic) {
                                "أدخل تصرفًا صحيحًا أكبر من صفر."
                            } else {
                                "Enter a valid flow greater than zero."
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
                            clearResult()
                            error = if (arabic) {
                                "تعذر حساب السرعة. راجع التصرف والقطر."
                            } else {
                                "Velocity calculation failed. Check flow and diameter."
                            }
                            return@EngineeringPrimaryButton
                        }

                        velocity = formatHydraulicValue(
                            calculatedVelocity
                        )
                        resultTitle = if (arabic) {
                            "السرعة المحسوبة"
                        } else {
                            "Calculated Velocity"
                        }
                        resultValue =
                            "${formatHydraulicValue(calculatedVelocity)} m/s"
                        error = null
                    }

                    HydraulicCalculationMode.FLOW_FROM_VELOCITY -> {
                        val velocityValue = velocity.toDoubleOrNull()

                        if (
                            velocityValue == null ||
                            !velocityValue.isFinite() ||
                            velocityValue <= 0.0
                        ) {
                            clearResult()
                            error = if (arabic) {
                                "أدخل سرعة صحيحة أكبر من صفر."
                            } else {
                                "Enter a valid velocity greater than zero."
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
                            clearResult()
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
                        error = null
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
                title = if (arabic) "نتيجة الحساب" else "Calculation Result"
            ) {
                EngineeringResult(
                    title = resultTitle!!,
                    value = resultValue!!
                )

                if (
                    mode ==
                    HydraulicCalculationMode.VELOCITY_FROM_FLOW
                ) {
                    EngineeringResult(
                        title = if (arabic) {
                            "التصرف المستخدم"
                        } else {
                            "Input Flow"
                        },
                        value = "${flow} m³/h"
                    )
                } else {
                    EngineeringResult(
                        title = if (arabic) {
                            "السرعة المستخدمة"
                        } else {
                            "Input Velocity"
                        },
                        value = "${velocity} m/s"
                    )
                }

                EngineeringResult(
                    title = if (arabic) {
                        "القطر الداخلي المستخدم"
                    } else {
                        "Internal Diameter"
                    },
                    value = "$diameter mm"
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
                    "حدد نوع الحساب، وأدخل القيم المطلوبة، ثم اضغط احسب."
                } else {
                    "Select a calculation mode, enter the required values, and calculate."
                }
            )
        }
    }
}

private fun formatHydraulicValue(value: Double): String =
    String.format(java.util.Locale.US, "%.4f", value)
