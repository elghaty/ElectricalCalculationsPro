
package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    var pumpEfficiency by remember { mutableStateOf("75") }
    var motorEfficiency by remember { mutableStateOf("92") }
    var operatingHours by remember { mutableStateOf("8") }
    var operatingDaysMonth by remember { mutableStateOf("30") }
    var operatingDaysYear by remember { mutableStateOf("365") }
    var tariff by remember { mutableStateOf("0") }
    var result by remember {
        mutableStateOf<PumpCalculationResult?>(null)
    }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "تصميم الطلمبات" else "Pump Engineering",
        subtitle = if (arabic) {
            "حساب التصرف والرأس الكلي والقدرة واستهلاك الطاقة"
        } else {
            "Flow, total dynamic head, power and energy analysis"
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
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = systemType != PumpSystemType.WATER
                )

                EngineeringSecondaryButton(
                    text = if (arabic) "صرف صحي" else "Sewage",
                    onClick = {
                        systemType = PumpSystemType.SEWAGE
                        result = null
                        error = null
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
            title = if (arabic) "بيانات التصرف" else "Flow Data"
        ) {
            EngineeringInput(
                value = flow,
                label = if (arabic) "التصرف" else "Flow",
                onValueChange = {
                    flow = it
                    result = null
                    error = null
                }
            )

            FlowUnitSelector(
                selected = flowUnit,
                arabic = arabic,
                onSelected = {
                    flowUnit = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringSection(
            title = if (arabic) "الرأس الهيدروليكي" else "Hydraulic Head"
        ) {
            EngineeringInput(
                value = staticHead,
                label = if (arabic) "الرفع الاستاتيكي H (m)" else "Static Head H (m)",
                onValueChange = {
                    staticHead = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                value = frictionHead,
                label = if (arabic) "فاقد الاحتكاك (m)" else "Friction Head (m)",
                onValueChange = {
                    frictionHead = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                value = minorHead,
                label = if (arabic) "الفواقد الثانوية (m)" else "Minor Losses (m)",
                onValueChange = {
                    minorHead = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                value = pressureHead,
                label = if (arabic) "رأس الضغط المطلوب (m)" else "Required Pressure Head (m)",
                onValueChange = {
                    pressureHead = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringSection(
            title = if (arabic) "الكفاءات" else "Efficiencies"
        ) {
            EngineeringInput(
                value = pumpEfficiency,
                label = if (arabic) "كفاءة الطلمبة (%)" else "Pump Efficiency (%)",
                onValueChange = {
                    pumpEfficiency = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                value = motorEfficiency,
                label = if (arabic) "كفاءة الموتور (%)" else "Motor Efficiency (%)",
                onValueChange = {
                    motorEfficiency = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringSection(
            title = if (arabic) "بيانات التشغيل والطاقة" else "Operating & Energy Data"
        ) {
            EngineeringInput(
                value = operatingHours,
                label = if (arabic) "ساعات التشغيل / يوم" else "Operating Hours / Day",
                onValueChange = {
                    operatingHours = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                value = operatingDaysMonth,
                label = if (arabic) "أيام التشغيل / شهر" else "Operating Days / Month",
                onValueChange = {
                    operatingDaysMonth = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                value = operatingDaysYear,
                label = if (arabic) "أيام التشغيل / سنة" else "Operating Days / Year",
                onValueChange = {
                    operatingDaysYear = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                value = tariff,
                label = if (arabic) "تعريفة الكهرباء / kWh" else "Energy Tariff / kWh",
                onValueChange = {
                    tariff = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) "حساب التصميم" else "Calculate Design",
            onClick = {
                try {
                    val flowValue = flow.toDoubleOrNull()

                    if (flowValue == null || !flowValue.isFinite() || flowValue <= 0.0) {
                        result = null
                        error = if (arabic) {
                            "أدخل قيمة صحيحة وموجبة للتصرف."
                        } else {
                            "Enter a valid positive flow value."
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
                        result = null
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
                        result = null
                        error = if (arabic) {
                            "كفاءة الموتور يجب أن تكون أكبر من 0 ولا تتجاوز 100%."
                        } else {
                            "Motor efficiency must be greater than 0 and no more than 100%."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val staticHeadValue = staticHead.toDoubleOrNull() ?: 0.0
                    val frictionHeadValue = frictionHead.toDoubleOrNull() ?: 0.0
                    val minorHeadValue = minorHead.toDoubleOrNull() ?: 0.0
                    val pressureHeadValue = pressureHead.toDoubleOrNull() ?: 0.0
                    val operatingHoursValue = operatingHours.toDoubleOrNull() ?: 0.0
                    val operatingDaysMonthValue = operatingDaysMonth.toDoubleOrNull() ?: 30.0
                    val operatingDaysYearValue = operatingDaysYear.toDoubleOrNull() ?: 365.0
                    val tariffValue = tariff.toDoubleOrNull() ?: 0.0

                    val numericValues = listOf(
                        staticHeadValue,
                        frictionHeadValue,
                        minorHeadValue,
                        pressureHeadValue,
                        operatingHoursValue,
                        operatingDaysMonthValue,
                        operatingDaysYearValue,
                        tariffValue
                    )

                    if (numericValues.any { !it.isFinite() || it < 0.0 }) {
                        result = null
                        error = if (arabic) {
                            "تأكد من أن قيم الرفع والفواقد والتشغيل والتعريفة أرقام غير سالبة."
                        } else {
                            "Head, losses, operating data and tariff must be valid non-negative numbers."
                        }
                        return@EngineeringPrimaryButton
                    }

                    if (
                        operatingHoursValue > 24.0 ||
                        operatingDaysMonthValue > 31.0 ||
                        operatingDaysYearValue > 366.0
                    ) {
                        result = null
                        error = if (arabic) {
                            "راجع ساعات التشغيل وأيام التشغيل المدخلة."
                        } else {
                            "Check the entered operating hours and operating days."
                        }
                        return@EngineeringPrimaryButton
                    }

                    val input = PumpCalculationInput(
                        systemType = systemType,
                        flow = PumpFlow(
                            value = flowValue,
                            unit = flowUnit
                        ),
                        head = PumpHead(
                            staticHeadM = staticHeadValue,
                            frictionHeadM = frictionHeadValue,
                            minorLossHeadM = minorHeadValue,
                            requiredPressureHeadM = pressureHeadValue
                        ),
                        pumpEfficiency = pumpEfficiencyValue,
                        motorEfficiency = motorEfficiencyValue,
                        operatingHoursPerDay = operatingHoursValue,
                        operatingDaysPerMonth = operatingDaysMonthValue,
                        operatingDaysPerYear = operatingDaysYearValue,
                        energyTariffPerKwh = tariffValue
                    )

                    result = ElectricalCalculations.calculatePump(input)
                    error = null
                } catch (exception: Exception) {
                    result = null
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

        result?.let { calculationResult ->
            PumpResultCard(
                result = calculationResult,
                arabic = arabic
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                    message = if (arabic) {
                        "أدخل بيانات التصميم ثم اضغط حساب التصميم لعرض النتائج."
                    } else {
                        "Enter the design data and calculate to display the engineering results."
                    }
                )
            }
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
                    text = {
                        androidx.compose.material3.Text(
                            text = flowUnitLabel(unit, arabic)
                        )
                    },
                    onClick = {
                        onSelected(unit)
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

@Composable
private fun PumpResultCard(
    result: PumpCalculationResult,
    arabic: Boolean
) {
    EngineeringSection(
        title = if (arabic) "نتائج التصميم" else "Design Results"
    ) {
        EngineeringResult(
            title = if (arabic) "التصرف" else "Flow",
            value = "${formatValue(result.flowM3PerHour)} m³/h  |  " +
                "${formatValue(result.flowM3PerSecond)} m³/s"
        )

        EngineeringResult(
            title = if (arabic) "الرأس الديناميكي الكلي TDH" else "Total Dynamic Head — TDH",
            value = "${formatValue(result.totalDynamicHeadM)} m"
        )

        EngineeringResult(
            title = if (arabic) "القدرة الهيدروليكية" else "Hydraulic Power",
            value = "${formatValue(result.hydraulicPowerKw)} kW"
        )

        EngineeringResult(
            title = if (arabic) "قدرة العمود" else "Shaft Power",
            value = "${formatValue(result.shaftPowerKw)} kW"
        )

        EngineeringResult(
            title = if (arabic) "قدرة دخل الموتور" else "Motor Input Power",
            value = "${formatValue(result.motorInputPowerKw)} kW"
        )

        EngineeringResult(
            title = if (arabic) "قدرة الموتور المقترحة" else "Recommended Motor Rating",
            value = "${formatValue(result.recommendedMotorRatingKw)} kW"
        )

        EngineeringSectionTitle(
            text = if (arabic) "استهلاك الطاقة" else "Energy Consumption"
        )

        EngineeringValueRow(
            label = if (arabic) "يومي" else "Daily",
            value = "${formatValue(result.dailyEnergyKwh)} kWh"
        )

        EngineeringValueRow(
            label = if (arabic) "شهري" else "Monthly",
            value = "${formatValue(result.monthlyEnergyKwh)} kWh"
        )

        EngineeringValueRow(
            label = if (arabic) "سنوي" else "Yearly",
            value = "${formatValue(result.yearlyEnergyKwh)} kWh"
        )

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

private fun formatValue(value: Double): String {
    return "%.3f".format(value)
}
