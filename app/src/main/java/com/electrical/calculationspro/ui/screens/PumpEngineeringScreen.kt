package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.pumps.PumpCalculationInput
import com.electrical.calculationspro.data.pumps.PumpCalculationResult
import com.electrical.calculationspro.data.pumps.PumpFlow
import com.electrical.calculationspro.data.pumps.PumpFlowUnit
import com.electrical.calculationspro.data.pumps.PumpHead
import com.electrical.calculationspro.data.pumps.PumpSystemType
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.DarkSurface
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@Composable
fun PumpEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var systemType by remember {
        mutableStateOf(PumpSystemType.WATER)
    }

    var flow by remember {
        mutableStateOf("")
    }

    var flowUnit by remember {
        mutableStateOf(PumpFlowUnit.LITERS_PER_SECOND)
    }

    var staticHead by remember {
        mutableStateOf("")
    }

    var frictionHead by remember {
        mutableStateOf("")
    }

    var minorHead by remember {
        mutableStateOf("")
    }

    var pressureHead by remember {
        mutableStateOf("")
    }

    var pumpEfficiency by remember {
        mutableStateOf("75")
    }

    var motorEfficiency by remember {
        mutableStateOf("92")
    }

    var operatingHours by remember {
        mutableStateOf("8")
    }

    var operatingDaysMonth by remember {
        mutableStateOf("30")
    }

    var operatingDaysYear by remember {
        mutableStateOf("365")
    }

    var tariff by remember {
        mutableStateOf("0")
    }

    var result by remember {
        mutableStateOf<PumpCalculationResult?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (arabic) {
                    "حسابات الطلمبات"
                } else {
                    "Pump Engineering"
                },
                color = TextPrimary,
                fontSize = 24.sp
            )

            OutlinedButton(
                onClick = onBack
            ) {
                Text(
                    text = if (arabic) {
                        "رجوع"
                    } else {
                        "Back"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        InputCard(
            title = if (arabic) {
                "نوع النظام"
            } else {
                "System Type"
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        systemType = PumpSystemType.WATER
                    }
                ) {
                    Text(
                        text = if (arabic) {
                            "مياه"
                        } else {
                            "Water"
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        systemType = PumpSystemType.SEWAGE
                    }
                ) {
                    Text(
                        text = if (arabic) {
                            "صرف"
                        } else {
                            "Sewage"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InputCard(
            title = if (arabic) {
                "التصرف"
            } else {
                "Flow"
            }
        ) {
            OutlinedTextField(
                value = flow,
                onValueChange = {
                    flow = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = if (arabic) {
                            "التصرف"
                        } else {
                            "Flow"
                        }
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowUnitSelector(
                selected = flowUnit,
                arabic = arabic,
                onSelected = {
                    flowUnit = it
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InputCard(
            title = if (arabic) {
                "الرأس الهيدروليكي"
            } else {
                "Hydraulic Head"
            }
        ) {
            NumberField(
                value = staticHead,
                label = if (arabic) {
                    "الرفع الاستاتيكي H"
                } else {
                    "Static Head H"
                },
                onValueChange = {
                    staticHead = it
                }
            )

            NumberField(
                value = frictionHead,
                label = if (arabic) {
                    "فاقد الاحتكاك"
                } else {
                    "Friction Head"
                },
                onValueChange = {
                    frictionHead = it
                }
            )

            NumberField(
                value = minorHead,
                label = if (arabic) {
                    "الفواقد الثانوية"
                } else {
                    "Minor Losses"
                },
                onValueChange = {
                    minorHead = it
                }
            )

            NumberField(
                value = pressureHead,
                label = if (arabic) {
                    "رأس الضغط المطلوب"
                } else {
                    "Required Pressure Head"
                },
                onValueChange = {
                    pressureHead = it
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InputCard(
            title = if (arabic) {
                "الكفاءة"
            } else {
                "Efficiency"
            }
        ) {
            NumberField(
                value = pumpEfficiency,
                label = if (arabic) {
                    "كفاءة الطلمبة %"
                } else {
                    "Pump Efficiency %"
                },
                onValueChange = {
                    pumpEfficiency = it
                }
            )

            NumberField(
                value = motorEfficiency,
                label = if (arabic) {
                    "كفاءة الموتور %"
                } else {
                    "Motor Efficiency %"
                },
                onValueChange = {
                    motorEfficiency = it
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InputCard(
            title = if (arabic) {
                "استهلاك الطاقة"
            } else {
                "Energy Consumption"
            }
        ) {
            NumberField(
                value = operatingHours,
                label = if (arabic) {
                    "ساعات التشغيل / يوم"
                } else {
                    "Operating Hours / Day"
                },
                onValueChange = {
                    operatingHours = it
                }
            )

            NumberField(
                value = operatingDaysMonth,
                label = if (arabic) {
                    "أيام التشغيل / شهر"
                } else {
                    "Operating Days / Month"
                },
                onValueChange = {
                    operatingDaysMonth = it
                }
            )

            NumberField(
                value = operatingDaysYear,
                label = if (arabic) {
                    "أيام التشغيل / سنة"
                } else {
                    "Operating Days / Year"
                },
                onValueChange = {
                    operatingDaysYear = it
                }
            )

            NumberField(
                value = tariff,
                label = if (arabic) {
                    "تعريفة الكهرباء / kWh"
                } else {
                    "Energy Tariff / kWh"
                },
                onValueChange = {
                    tariff = it
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                try {
                    val flowValue = flow.toDoubleOrNull()

                    if (flowValue == null || flowValue <= 0.0) {
                        result = null
                        error = if (arabic) {
                            "أدخل قيمة صحيحة للتصرف."
                        } else {
                            "Enter a valid flow value."
                        }
                        return@Button
                    }

                    val pumpEfficiencyValue =
                        pumpEfficiency.toDoubleOrNull()?.div(100.0)

                    val motorEfficiencyValue =
                        motorEfficiency.toDoubleOrNull()?.div(100.0)

                    if (
                        pumpEfficiencyValue == null ||
                        pumpEfficiencyValue <= 0.0 ||
                        pumpEfficiencyValue > 1.0
                    ) {
                        result = null
                        error = if (arabic) {
                            "كفاءة الطلمبة يجب أن تكون بين 0 و100%."
                        } else {
                            "Pump efficiency must be between 0 and 100%."
                        }
                        return@Button
                    }

                    if (
                        motorEfficiencyValue == null ||
                        motorEfficiencyValue <= 0.0 ||
                        motorEfficiencyValue > 1.0
                    ) {
                        result = null
                        error = if (arabic) {
                            "كفاءة الموتور يجب أن تكون بين 0 و100%."
                        } else {
                            "Motor efficiency must be between 0 and 100%."
                        }
                        return@Button
                    }

                    val input = PumpCalculationInput(
                        systemType = systemType,
                        flow = PumpFlow(
                            value = flowValue,
                            unit = flowUnit
                        ),
                        head = PumpHead(
                            staticHeadM =
                                staticHead.toDoubleOrNull() ?: 0.0,
                            frictionHeadM =
                                frictionHead.toDoubleOrNull() ?: 0.0,
                            minorLossHeadM =
                                minorHead.toDoubleOrNull() ?: 0.0,
                            requiredPressureHeadM =
                                pressureHead.toDoubleOrNull() ?: 0.0
                        ),
                        pumpEfficiency = pumpEfficiencyValue,
                        motorEfficiency = motorEfficiencyValue,
                        operatingHoursPerDay =
                            operatingHours.toDoubleOrNull() ?: 0.0,
                        operatingDaysPerMonth =
                            operatingDaysMonth.toDoubleOrNull() ?: 30.0,
                        operatingDaysPerYear =
                            operatingDaysYear.toDoubleOrNull() ?: 365.0,
                        energyTariffPerKwh =
                            tariff.toDoubleOrNull() ?: 0.0
                    )

                    result = ElectricalCalculations.calculatePump(input)
                    error = null
                } catch (exception: Exception) {
                    result = null
                    error = exception.message
                        ?: if (arabic) {
                            "بيانات الإدخال غير صحيحة."
                        } else {
                            "Invalid input."
                        }
                }
            }
        ) {
            Text(
                text = if (arabic) {
                    "احسب الطلمبة"
                } else {
                    "Calculate Pump"
                }
            )
        }

        error?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF5A1F1F)
                )
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        result?.let { calculationResult ->
            Spacer(modifier = Modifier.height(16.dp))

            PumpResultCard(
                result = calculationResult,
                arabic = arabic
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun InputCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        label = {
            Text(text = label)
        },
        singleLine = true
    )
}

@Composable
private fun FlowUnitSelector(
    selected: PumpFlowUnit,
    arabic: Boolean,
    onSelected: (PumpFlowUnit) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                expanded = true
            }
        ) {
            Text(
                text = flowUnitLabel(
                    unit = selected,
                    arabic = arabic
                )
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            PumpFlowUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = flowUnitLabel(
                                unit = unit,
                                arabic = arabic
                            )
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
): String {
    return when (unit) {
        PumpFlowUnit.LITERS_PER_SECOND ->
            if (arabic) "لتر / ثانية" else "L/s"

        PumpFlowUnit.CUBIC_METERS_PER_HOUR ->
            if (arabic) "م³ / ساعة" else "m³/h"

        PumpFlowUnit.CUBIC_METERS_PER_SECOND ->
            if (arabic) "م³ / ثانية" else "m³/s"

        PumpFlowUnit.LITERS_PER_MINUTE ->
            if (arabic) "لتر / دقيقة" else "L/min"
    }
}

@Composable
private fun PumpResultCard(
    result: PumpCalculationResult,
    arabic: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (arabic) {
                    "نتائج الحساب"
                } else {
                    "Calculation Results"
                },
                color = TextPrimary,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            ResultRow(
                label = if (arabic) "التصرف m³/s" else "Flow m³/s",
                value = formatValue(result.flowM3PerSecond)
            )

            ResultRow(
                label = if (arabic) "التصرف m³/h" else "Flow m³/h",
                value = formatValue(result.flowM3PerHour)
            )

            ResultRow(
                label = if (arabic) "الرفع الكلي TDH" else "Total Dynamic Head",
                value = "${formatValue(result.totalDynamicHeadM)} m"
            )

            ResultRow(
                label = if (arabic) "القدرة الهيدروليكية" else "Hydraulic Power",
                value = "${formatValue(result.hydraulicPowerKw)} kW"
            )

            ResultRow(
                label = if (arabic) "قدرة العمود" else "Shaft Power",
                value = "${formatValue(result.shaftPowerKw)} kW"
            )

            ResultRow(
                label = if (arabic) "قدرة دخل الموتور" else "Motor Input Power",
                value = "${formatValue(result.motorInputPowerKw)} kW"
            )

            ResultRow(
                label = if (arabic) "قدرة الموتور المقترحة" else "Recommended Motor",
                value = "${formatValue(result.recommendedMotorRatingKw)} kW"
            )

            ResultRow(
                label = if (arabic) "الاستهلاك اليومي" else "Daily Energy",
                value = "${formatValue(result.dailyEnergyKwh)} kWh"
            )

            ResultRow(
                label = if (arabic) "الاستهلاك الشهري" else "Monthly Energy",
                value = "${formatValue(result.monthlyEnergyKwh)} kWh"
            )

            ResultRow(
                label = if (arabic) "الاستهلاك السنوي" else "Yearly Energy",
                value = "${formatValue(result.yearlyEnergyKwh)} kWh"
            )

            if (result.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (arabic) "تحذيرات" else "Warnings",
                    color = TextPrimary,
                    fontSize = 17.sp
                )

                result.warnings.forEach { warning ->
                    Text(
                        text = "• $warning",
                        color = Color(0xFFFFCC80),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (result.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (arabic) "ملاحظات" else "Notes",
                    color = TextPrimary,
                    fontSize = 17.sp
                )

                result.notes.forEach { note ->
                    Text(
                        text = "• $note",
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary
        )

        Text(
            text = value,
            color = TextPrimary
        )
    }
}

private fun formatValue(value: Double): String {
    return "%.3f".format(value)
}
