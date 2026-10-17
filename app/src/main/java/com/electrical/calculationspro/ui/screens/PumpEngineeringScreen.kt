
package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult
import com.electrical.calculationspro.data.pumps.PumpFlow
import com.electrical.calculationspro.data.pumps.PumpFlowUnit
import com.electrical.calculationspro.data.pumps.PumpHead
import com.electrical.calculationspro.data.pumps.PumpSystemType
import com.electrical.calculationspro.data.water.WaterDesignEngine
import com.electrical.calculationspro.ui.components.EngineeringEmptyState
import com.electrical.calculationspro.ui.components.EngineeringInput
import com.electrical.calculationspro.ui.components.EngineeringPage
import com.electrical.calculationspro.ui.components.EngineeringPrimaryButton
import com.electrical.calculationspro.ui.components.EngineeringResult
import com.electrical.calculationspro.ui.components.EngineeringSecondaryButton
import com.electrical.calculationspro.ui.components.EngineeringSection
import com.electrical.calculationspro.ui.components.EngineeringSectionTitle
import com.electrical.calculationspro.ui.components.EngineeringStatus
import com.electrical.calculationspro.ui.components.EngineeringValueRow
import com.electrical.calculationspro.ui.components.FourColumnFields

private data class SuctionPipeMaterial(
    val name: String,
    val labelAr: String,
    val labelEn: String
)

private val suctionPipeMaterials = listOf(
    SuctionPipeMaterial("HDPE", "بولي إيثيلين HDPE", "HDPE"),
    SuctionPipeMaterial("UPVC", "يو بي في سي UPVC", "UPVC"),
    SuctionPipeMaterial("Ductile iron", "حديد دكتايل", "Ductile iron"),
    SuctionPipeMaterial("Steel", "حديد صلب", "Steel"),
    SuctionPipeMaterial("Cast iron", "حديد زهر", "Cast iron"),
    SuctionPipeMaterial("Concrete", "خرسانة", "Concrete")
)

@Composable
fun PumpEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var systemType by remember {
        mutableStateOf(PumpSystemType.WATER)
    }
    var flow by remember { mutableStateOf("") }
    var flowUnit by remember {
        mutableStateOf(PumpFlowUnit.LITERS_PER_SECOND)
    }

    var staticHead by remember { mutableStateOf("") }
    var frictionHead by remember { mutableStateOf("") }
    var minorHead by remember { mutableStateOf("") }
    var pressureHead by remember { mutableStateOf("") }

    // Suction-line inputs. The suction friction loss is calculated automatically.
    var suctionLength by remember { mutableStateOf("") }
    var suctionDiameter by remember { mutableStateOf("") }
    var suctionMaterial by remember {
        mutableStateOf(suctionPipeMaterials.first())
    }

    var pumpEfficiency by remember { mutableStateOf("75") }
    var motorEfficiency by remember { mutableStateOf("92") }
    var operatingHours by remember { mutableStateOf("8") }
    var operatingDaysMonth by remember { mutableStateOf("30") }
    var operatingDaysYear by remember { mutableStateOf("365") }
    var tariff by remember { mutableStateOf("0") }

    var result by remember {
        mutableStateOf<PumpCalculationResult?>(null)
    }
    var calculatedSuctionLoss by remember { mutableStateOf<Double?>(null) }
    var calculatedTotalFriction by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun clearResult() {
        result = null
        calculatedSuctionLoss = null
        calculatedTotalFriction = null
        error = null
    }

    EngineeringPage(
        title = if (arabic) "تصميم الطلمبات" else "Pump Engineering",
        subtitle = if (arabic) {
            "التصرف والهيد والفواقد والقدرة واستهلاك الطاقة"
        } else {
            "Flow, head losses, power and energy analysis"
        },
        onBack = onBack
    ) {
        EngineeringSection(
            title = if (arabic) "نوع النظام" else "System Type"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringSecondaryButton(
                    text = if (arabic) "مياه" else "Water",
                    onClick = {
                        systemType = PumpSystemType.WATER
                        clearResult()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = systemType != PumpSystemType.WATER
                )

                EngineeringSecondaryButton(
                    text = if (arabic) "صرف صحي" else "Sewage",
                    onClick = {
                        systemType = PumpSystemType.SEWAGE
                        clearResult()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = systemType != PumpSystemType.SEWAGE
                )
            }

            EngineeringStatus(
                text = if (systemType == PumpSystemType.WATER) {
                    if (arabic) "النظام الحالي: مياه"
                    else "Current system: Water"
                } else {
                    if (arabic) "النظام الحالي: صرف صحي"
                    else "Current system: Sewage"
                }
            )
        }

        EngineeringSection(
            title = if (arabic) "التصرف المطلوب" else "Required Flow"
        ) {
            FourColumnFields {
                item {
                    EngineeringInput(
                        value = flow,
                        label = if (arabic) "التصرف" else "Flow",
                        onValueChange = {
                            flow = it
                            clearResult()
                        }
                    )
                }

                item {
                    FlowUnitSelector(
                        selected = flowUnit,
                        arabic = arabic,
                        onSelected = {
                            flowUnit = it
                            clearResult()
                        }
                    )
                }
            }
        }

        EngineeringSection(
            title = if (arabic) {
                "بيانات خط السحب"
            } else {
                "Suction Pipe Data"
            }
        ) {
            EngineeringStatus(
                text = if (arabic) {
                    "أدخل طول خط السحب والقطر الداخلي الفعلي. سيحسب البرنامج فاقد الاحتكاك ويضيفه إلى الفواقد الكلية مرة واحدة."
                } else {
                    "Enter suction-line length and actual internal diameter. Suction friction loss is calculated and included once in total losses."
                }
            )

            FourColumnFields {
                item {
                    EngineeringInput(
                        value = suctionLength,
                        label = if (arabic) {
                            "طول خط السحب (m)"
                        } else {
                            "Suction Length (m)"
                        },
                        onValueChange = {
                            suctionLength = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = suctionDiameter,
                        label = if (arabic) {
                            "القطر الداخلي (mm)"
                        } else {
                            "Internal Diameter (mm)"
                        },
                        onValueChange = {
                            suctionDiameter = it
                            clearResult()
                        }
                    )
                }

                item {
                    SuctionPipeMaterialSelector(
                        selected = suctionMaterial,
                        arabic = arabic,
                        onSelected = {
                            suctionMaterial = it
                            clearResult()
                        }
                    )
                }
            }
        }

        EngineeringSection(
            title = if (arabic) {
                "الرأس الهيدروليكي والفواقد الأخرى"
            } else {
                "Hydraulic Head and Other Losses"
            }
        ) {
            EngineeringStatus(
                text = if (arabic) {
                    "فاقد خط السحب سيُحسب تلقائيًا. حقل فاقد الاحتكاك هنا مخصص لباقي الخطوط فقط، ولا تكرر فيه فاقد السحب."
                } else {
                    "Suction loss is calculated automatically. Enter other-line friction losses here; do not include suction loss again."
                }
            )

            FourColumnFields {
                item {
                    EngineeringInput(
                        value = staticHead,
                        label = if (arabic) {
                            "الرفع الاستاتيكي (m)"
                        } else {
                            "Static Head (m)"
                        },
                        onValueChange = {
                            staticHead = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = frictionHead,
                        label = if (arabic) {
                            "فاقد باقي الخطوط (m)"
                        } else {
                            "Other Friction Loss (m)"
                        },
                        onValueChange = {
                            frictionHead = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = minorHead,
                        label = if (arabic) {
                            "الفواقد الموضعية (m)"
                        } else {
                            "Minor Losses (m)"
                        },
                        onValueChange = {
                            minorHead = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = pressureHead,
                        label = if (arabic) {
                            "رأس الضغط المطلوب (m)"
                        } else {
                            "Required Pressure Head (m)"
                        },
                        onValueChange = {
                            pressureHead = it
                            clearResult()
                        }
                    )
                }
            }
        }

        EngineeringSection(
            title = if (arabic) "الكفاءات" else "Efficiencies"
        ) {
            FourColumnFields {
                item {
                    EngineeringInput(
                        value = pumpEfficiency,
                        label = if (arabic) {
                            "كفاءة الطلمبة (%)"
                        } else {
                            "Pump Efficiency (%)"
                        },
                        onValueChange = {
                            pumpEfficiency = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = motorEfficiency,
                        label = if (arabic) {
                            "كفاءة الموتور (%)"
                        } else {
                            "Motor Efficiency (%)"
                        },
                        onValueChange = {
                            motorEfficiency = it
                            clearResult()
                        }
                    )
                }
            }
        }

        EngineeringSection(
            title = if (arabic) {
                "بيانات التشغيل والطاقة"
            } else {
                "Operating and Energy Data"
            }
        ) {
            FourColumnFields {
                item {
                    EngineeringInput(
                        value = operatingHours,
                        label = if (arabic) {
                            "ساعات التشغيل / يوم"
                        } else {
                            "Operating Hours / Day"
                        },
                        onValueChange = {
                            operatingHours = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = operatingDaysMonth,
                        label = if (arabic) {
                            "أيام التشغيل / شهر"
                        } else {
                            "Operating Days / Month"
                        },
                        onValueChange = {
                            operatingDaysMonth = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = operatingDaysYear,
                        label = if (arabic) {
                            "أيام التشغيل / سنة"
                        } else {
                            "Operating Days / Year"
                        },
                        onValueChange = {
                            operatingDaysYear = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = tariff,
                        label = if (arabic) {
                            "تعريفة الكهرباء / kWh"
                        } else {
                            "Energy Tariff / kWh"
                        },
                        onValueChange = {
                            tariff = it
                            clearResult()
                        }
                    )
                }
            }
        }

        EngineeringPrimaryButton(
            text = if (arabic) "حساب التصميم" else "Calculate Design",
            onClick = {
                try {
                    val flowValue = flow.toDoubleOrNull()

                    if (
                        flowValue == null ||
                        !flowValue.isFinite() ||
                        flowValue <= 0.0
                    ) {
                        clearResult()
                        error = if (arabic) {
                            "أدخل تصرفًا صحيحًا وموجبًا."
                        } else {
                            "Enter a valid positive flow."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val flowM3PerHour = flowToM3PerHour(
                        value = flowValue,
                        unit = flowUnit
                    )

                    if (!flowM3PerHour.isFinite() || flowM3PerHour <= 0.0) {
                        clearResult()
                        error = if (arabic) {
                            "تعذر تحويل التصرف إلى م³/ساعة."
                        } else {
                            "Could not convert flow to m³/h."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val suctionLengthValue =
                        parseOptionalNonNegative(suctionLength)
                    val suctionDiameterValue =
                        parseOptionalNonNegative(suctionDiameter)

                    if (suctionLengthValue == null || suctionDiameterValue == null) {
                        clearResult()
                        error = if (arabic) {
                            "بيانات خط السحب يجب أن تكون أرقامًا صحيحة غير سالبة."
                        } else {
                            "Suction pipe data must be valid non-negative numbers."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val hasSuctionLength = suctionLengthValue > 0.0
                    val hasSuctionDiameter = suctionDiameterValue > 0.0

                    if (hasSuctionLength != hasSuctionDiameter) {
                        clearResult()
                        error = if (arabic) {
                            "أدخل طول خط السحب والقطر الداخلي معًا، أو اترك الاثنين فارغين/صفرًا."
                        } else {
                            "Enter both suction length and internal diameter, or leave both empty/zero."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val suctionLoss = if (hasSuctionLength && hasSuctionDiameter) {
                        WaterDesignEngine.calculateFrictionLoss(
                            flowM3PerHour = flowM3PerHour,
                            diameterMm = suctionDiameterValue,
                            lengthM = suctionLengthValue,
                            material = suctionMaterial.name
                        )
                    } else {
                        0.0
                    }

                    if (!suctionLoss.isFinite() || suctionLoss < 0.0) {
                        clearResult()
                        error = if (arabic) {
                            "تعذر حساب فاقد خط السحب. راجع التصرف والقطر والطول."
                        } else {
                            "Could not calculate suction loss. Check flow, diameter and length."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val pumpEfficiencyValue =
                        pumpEfficiency.toDoubleOrNull()?.div(100.0)
                    val motorEfficiencyValue =
                        motorEfficiency.toDoubleOrNull()?.div(100.0)

                    if (
                        pumpEfficiencyValue == null ||
                        !pumpEfficiencyValue.isFinite() ||
                        pumpEfficiencyValue <= 0.0 ||
                        pumpEfficiencyValue > 1.0
                    ) {
                        clearResult()
                        error = if (arabic) {
                            "كفاءة الطلمبة يجب أن تكون أكبر من 0 ولا تتجاوز 100%."
                        } else {
                            "Pump efficiency must be greater than 0 and no more than 100%."
                        }
                        return@EngineeringPrimaryButton
                    }

                    if (
                        motorEfficiencyValue == null ||
                        !motorEfficiencyValue.isFinite() ||
                        motorEfficiencyValue <= 0.0 ||
                        motorEfficiencyValue > 1.0
                    ) {
                        clearResult()
                        error = if (arabic) {
                            "كفاءة الموتور يجب أن تكون أكبر من 0 ولا تتجاوز 100%."
                        } else {
                            "Motor efficiency must be greater than 0 and no more than 100%."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val staticHeadValue =
                        parseNonNegative(staticHead)
                    val otherFrictionHeadValue =
                        parseNonNegative(frictionHead)
                    val minorHeadValue =
                        parseNonNegative(minorHead)
                    val pressureHeadValue =
                        parseNonNegative(pressureHead)
                    val operatingHoursValue =
                        parseNonNegative(operatingHours)
                    val operatingDaysMonthValue =
                        parseNonNegative(operatingDaysMonth)
                    val operatingDaysYearValue =
                        parseNonNegative(operatingDaysYear)
                    val tariffValue =
                        parseNonNegative(tariff)

                    val numericValues = listOf(
                        staticHeadValue,
                        otherFrictionHeadValue,
                        minorHeadValue,
                        pressureHeadValue,
                        operatingHoursValue,
                        operatingDaysMonthValue,
                        operatingDaysYearValue,
                        tariffValue
                    )

                    if (numericValues.any { it == null }) {
                        clearResult()
                        error = if (arabic) {
                            "راجع قيم الرفع والفواقد والتشغيل والتعريفة؛ يجب أن تكون أرقامًا غير سالبة."
                        } else {
                            "Head, losses, operating data and tariff must be valid non-negative numbers."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val staticHeadM = staticHeadValue!!
                    val otherFrictionHeadM = otherFrictionHeadValue!!
                    val minorLossM = minorHeadValue!!
                    val pressureHeadM = pressureHeadValue!!
                    val operatingHoursPerDay = operatingHoursValue!!
                    val operatingDaysPerMonthValue = operatingDaysMonthValue!!
                    val operatingDaysPerYearValue = operatingDaysYearValue!!
                    val tariffPerKwh = tariffValue!!

                    if (
                        operatingHoursPerDay > 24.0 ||
                        operatingDaysPerMonthValue > 31.0 ||
                        operatingDaysPerYearValue > 366.0
                    ) {
                        clearResult()
                        error = if (arabic) {
                            "راجع ساعات التشغيل وأيام التشغيل المدخلة."
                        } else {
                            "Check the entered operating hours and operating days."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val totalFrictionHeadM =
                        otherFrictionHeadM + suctionLoss

                    val input = PumpCalculationInput(
                        systemType = systemType,
                        flow = PumpFlow(
                            value = flowValue,
                            unit = flowUnit
                        ),
                        head = PumpHead(
                            staticHeadM = staticHeadM,
                            frictionHeadM = totalFrictionHeadM,
                            minorLossHeadM = minorLossM,
                            requiredPressureHeadM = pressureHeadM
                        ),
                        pumpEfficiency = pumpEfficiencyValue,
                        motorEfficiency = motorEfficiencyValue,
                        operatingHoursPerDay = operatingHoursPerDay,
                        operatingDaysPerMonth = operatingDaysPerMonthValue,
                        operatingDaysPerYear = operatingDaysPerYearValue,
                        energyTariffPerKwh = tariffPerKwh
                    )

                    result = ElectricalCalculations.calculatePump(input)
                    calculatedSuctionLoss = suctionLoss
                    calculatedTotalFriction = totalFrictionHeadM
                    error = null
                } catch (exception: Exception) {
                    result = null
                    calculatedSuctionLoss = null
                    calculatedTotalFriction = null
                    error = exception.message
                        ?: if (arabic) {
                            "تعذر إجراء الحساب. راجع بيانات الإدخال."
                        } else {
                            "Calculation failed. Check the entered data."
                        }
                }
            }
        )

        error?.let { message ->
            EngineeringStatus(
                text = message,
                success = false
            )
        }

        if (result != null) {
            EngineeringSection(
                title = if (arabic) {
                    "نتائج خط السحب"
                } else {
                    "Suction Pipe Results"
                }
            ) {
                EngineeringValueRow(
                    label = if (arabic) {
                        "فاقد الاحتكاك بخط السحب"
                    } else {
                        "Suction Friction Loss"
                    },
                    value = "${formatValue(calculatedSuctionLoss ?: 0.0)} m"
                )
                EngineeringValueRow(
                    label = if (arabic) {
                        "إجمالي فاقد الاحتكاك المستخدم في الحساب"
                    } else {
                        "Total Friction Loss Used"
                    },
                    value = "${formatValue(calculatedTotalFriction ?: 0.0)} m"
                )

                if (
                    suctionLength.toDoubleOrNull()?.let { it > 0.0 } == true &&
                    suctionDiameter.toDoubleOrNull()?.let { it > 0.0 } == true
                ) {
                    val velocity = WaterDesignEngine.calculateVelocity(
                        flowM3PerHour = flowToM3PerHour(
                            flow.toDoubleOrNull() ?: 0.0,
                            flowUnit
                        ),
                        diameterMm = suctionDiameter.toDoubleOrNull() ?: 0.0
                    )

                    EngineeringValueRow(
                        label = if (arabic) {
                            "سرعة المياه بخط السحب"
                        } else {
                            "Suction Water Velocity"
                        },
                        value = "${formatValue(velocity)} m/s"
                    )

                    EngineeringStatus(
                        text = if (arabic) {
                            "راجع ملاءمة سرعة السحب وNPSH من بيانات الموقع وكتالوج الطلمبة؛ حساب فاقد الاحتكاك وحده لا يثبت سلامة السحب."
                        } else {
                            "Verify suction velocity and NPSH using site data and the pump curve; friction loss alone does not confirm safe suction conditions."
                        },
                        success = false
                    )
                }
            }

            result?.let { calculationResult ->
                PumpResultCard(
                    result = calculationResult,
                    arabic = arabic
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
                    "أدخل التصرف والهيد والفواقد وبيانات خط السحب، ثم اضغط حساب التصميم."
                } else {
                    "Enter flow, head, losses and suction pipe data, then calculate."
                }
            )
        }
    }
}

@Composable
private fun FlowUnitSelector(
    selected: PumpFlowUnit,
    arabic: Boolean,
    onSelected: (PumpFlowUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        EngineeringSecondaryButton(
            text = flowUnitLabel(selected, arabic),
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PumpFlowUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(text = flowUnitLabel(unit, arabic)) },
                    onClick = {
                        onSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SuctionPipeMaterialSelector(
    selected: SuctionPipeMaterial,
    arabic: Boolean,
    onSelected: (SuctionPipeMaterial) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        EngineeringSecondaryButton(
            text = if (arabic) selected.labelAr else selected.labelEn,
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suctionPipeMaterials.forEach { material ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (arabic) {
                                material.labelAr
                            } else {
                                material.labelEn
                            }
                        )
                    },
                    onClick = {
                        onSelected(material)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun flowUnitLabel(
    unit: PumpFlowUnit,
    arabic: Boolean
): String = when (unit) {
    PumpFlowUnit.LITERS_PER_SECOND ->
        if (arabic) "لتر / ثانية — L/s" else "L/s"

    PumpFlowUnit.CUBIC_METERS_PER_HOUR ->
        if (arabic) "م³ / ساعة — m³/h" else "m³/h"

    PumpFlowUnit.CUBIC_METERS_PER_SECOND ->
        if (arabic) "م³ / ثانية — m³/s" else "m³/s"

    PumpFlowUnit.LITERS_PER_MINUTE ->
        if (arabic) "لتر / دقيقة — L/min" else "L/min"
}

private fun flowToM3PerHour(
    value: Double,
    unit: PumpFlowUnit
): Double = when (unit) {
    PumpFlowUnit.LITERS_PER_SECOND -> value * 3.6
    PumpFlowUnit.CUBIC_METERS_PER_HOUR -> value
    PumpFlowUnit.CUBIC_METERS_PER_SECOND -> value * 3600.0
    PumpFlowUnit.LITERS_PER_MINUTE -> value * 0.06
}

private fun parseOptionalNonNegative(value: String): Double? {
    if (value.isBlank()) return 0.0
    val parsed = value.toDoubleOrNull() ?: return null
    return parsed.takeIf { it.isFinite() && it >= 0.0 }
}

private fun parseNonNegative(value: String): Double? {
    val parsed = value.toDoubleOrNull() ?: return null
    return parsed.takeIf { it.isFinite() && it >= 0.0 }
}

@Composable
private fun PumpResultCard(
    result: PumpCalculationResult,
    arabic: Boolean
) {
    EngineeringSection(
        title = if (arabic) "نتائج التصميم" else "Design Results"
    ) {
        FourColumnFields {
            item {
                EngineeringResult(
                    title = if (arabic) "التصرف" else "Flow",
                    value = "${formatValue(result.flowM3PerHour)} m³/h | " +
                        "${formatValue(result.flowM3PerSecond)} m³/s"
                )
            }

            item {
                EngineeringResult(
                    title = if (arabic) {
                        "الرأس الديناميكي الكلي TDH"
                    } else {
                        "Total Dynamic Head — TDH"
                    },
                    value = "${formatValue(result.totalDynamicHeadM)} m"
                )
            }

            item {
                EngineeringResult(
                    title = if (arabic) {
                        "القدرة الهيدروليكية"
                    } else {
                        "Hydraulic Power"
                    },
                    value = "${formatValue(result.hydraulicPowerKw)} kW"
                )
            }

            item {
                EngineeringResult(
                    title = if (arabic) "قدرة العمود" else "Shaft Power",
                    value = "${formatValue(result.shaftPowerKw)} kW"
                )
            }

            item {
                EngineeringResult(
                    title = if (arabic) {
                        "قدرة دخل الموتور"
                    } else {
                        "Motor Input Power"
                    },
                    value = "${formatValue(result.motorInputPowerKw)} kW"
                )
            }

            item {
                EngineeringResult(
                    title = if (arabic) {
                        "قدرة الموتور المقترحة"
                    } else {
                        "Recommended Motor Rating"
                    },
                    value = "${formatValue(result.recommendedMotorRatingKw)} kW"
                )
            }
        }

        EngineeringSectionTitle(
            text = if (arabic) "استهلاك الطاقة" else "Energy Consumption"
        )

        FourColumnFields {
            item {
                EngineeringValueRow(
                    label = if (arabic) "يومي" else "Daily",
                    value = "${formatValue(result.dailyEnergyKwh)} kWh"
                )
            }

            item {
                EngineeringValueRow(
                    label = if (arabic) "شهري" else "Monthly",
                    value = "${formatValue(result.monthlyEnergyKwh)} kWh"
                )
            }

            item {
                EngineeringValueRow(
                    label = if (arabic) "سنوي" else "Yearly",
                    value = "${formatValue(result.yearlyEnergyKwh)} kWh"
                )
            }
        }

        if (result.warnings.isNotEmpty()) {
            EngineeringSectionTitle(
                text = if (arabic) "تحذيرات هندسية" else "Engineering Warnings"
            )

            result.warnings.forEach { warning ->
                EngineeringStatus(
                    text = warning,
                    success = false
                )
            }
        }

        if (result.notes.isNotEmpty()) {
            EngineeringSectionTitle(
                text = if (arabic) "ملاحظات" else "Notes"
            )

            result.notes.forEach { note ->
                EngineeringStatus(
                    text = note,
                    success = true
                )
            }
        }
    }
}

private fun formatValue(value: Double): String =
    String.format(java.util.Locale.US, "%.3f", value)

