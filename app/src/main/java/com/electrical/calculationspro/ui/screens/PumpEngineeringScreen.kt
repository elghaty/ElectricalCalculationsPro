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
import androidx.compose.foundation.layout.width
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
import com.electrical.calculationspro.data.pumps.PumpFlow
import com.electrical.calculationspro.data.pumps.PumpFlowUnit
import com.electrical.calculationspro.data.pumps.PumpHead
import com.electrical.calculationspro.data.pumps.PumpSystemType
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.DarkSurface
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@Composable
fun PumpEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    var systemType by remember {
        mutableStateOf(PumpSystemType.WATER)
    }

    var flow by remember {
        mutableStateOf("")
    }

    var flowUnit by remember {
        mutableStateOf(
            PumpFlowUnit.LITERS_PER_SECOND
        )
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
        mutableStateOf<
            com.electrical.calculationspro.data.pumps.PumpCalculationResult?
        >(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val scrollState =
        rememberScrollState()

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
                text =
                    if (arabic)
                        "حسابات الطلمبات"
                    else
                        "Pump Engineering",

                color = TextPrimary,
                fontSize = 24.sp
            )

            OutlinedButton(
                onClick = onBack
            ) {
                Text(
                    if (arabic)
                        "رجوع"
                    else
                        "Back"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text =
                        if (arabic)
                            "نوع النظام"
                        else
                            "System Type",

                    color = TextSecondary
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedButton(
                        onClick = {
                            systemType =
                                PumpSystemType.WATER
                        }
                    ) {
                        Text(
                            if (arabic)
                                "مياه"
                            else
                                "Water"
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            systemType =
                                PumpSystemType.SEWAGE
                        }
                    ) {
                        Text(
                            if (arabic)
                                "صرف"
                            else
                                "Sewage"
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        InputCard(
            title =
                if (arabic)
                    "التصرف"
                else
                    "Flow"
        ) {

            OutlinedTextField(
                value = flow,
                onValueChange = {
                    flow = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        if (arabic)
                            "التصرف"
                        else
                            "Flow"
                    )
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            FlowUnitSelector(
                selected = flowUnit,
                arabic = arabic,
                onSelected = {
                    flowUnit = it
                }
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        InputCard(
            title =
                if (arabic)
                    "الرأس الهيدروليكي"
                else
                    "Hydraulic Head"
        ) {

            NumberField(
                value = staticHead,
                label =
                    if (arabic)
                        "الرفع الاستاتيكي H"
                    else
                        "Static Head H",
                onValueChange = {
                    staticHead = it
                }
            )

            NumberField(
                value = frictionHead,
                label =
                    if (arabic)
                        "فاقد الاحتكاك"
                    else
                        "Friction Head",
                onValueChange = {
                    frictionHead = it
                }
            )

            NumberField(
                value = minorHead,
                label =
                    if (arabic)
                        "الفواقد الثانوية"
                    else
                        "Minor Losses",
                onValueChange = {
                    minorHead = it
                }
            )

            NumberField(
                value = pressureHead,
                label =
                    if (arabic)
                        "رأس الضغط المطلوب"
                    else
                        "Required Pressure Head",
                onValueChange = {
                    pressureHead = it
                }
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        InputCard(
            title =
                if (arabic)
                    "الكفاءة"
                else
                    "Efficiency"
        ) {

            NumberField(
                value = pumpEfficiency,
                label =
                    if (arabic)
                        "كفاءة الطلمبة %"
                    else
                        "Pump Efficiency %",
                onValueChange = {
                    pumpEfficiency = it
                }
            )

            NumberField(
                value = motorEfficiency,
                label =
                    if (arabic)
                        "كفاءة الموتور %"
                    else
                        "Motor Efficiency %",
                onValueChange = {
                    motorEfficiency = it
                }
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        InputCard(
            title =
                if (arabic)
                    "استهلاك الطاقة"
                else
                    "Energy Consumption"
        ) {

            NumberField(
                value = operatingHours,
                label =
                    if (arabic)
                        "ساعات التشغيل / يوم"
                    else
                        "Operating Hours / Day",
                onValueChange = {
                    operatingHours = it
                }
            )

            NumberField(
                value = operatingDaysMonth,
                label =
                    if (arabic)
                        "أيام التشغيل / شهر"
                    else
                        "Operating Days / Month",
                onValueChange = {
                    operatingDaysMonth = it
                }
            )

            NumberField(
                value = operatingDaysYear,
                label =
                    if (arabic)
                        "أيام التشغيل / سنة"
                    else
                        "Operating Days / Year",
                onValueChange = {
                    operatingDaysYear = it
                }
            )

            NumberField(
                value = tariff,
                label =
                    if (arabic)
                        "تعريفة الكهرباء / kWh"
                    else
                        "Energy Tariff / kWh",
                onValueChange = {
                    tariff = it
                }
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {

                try {

                    val input =
                        PumpCalculationInput(

                            systemType =
                                systemType,

                            flow =
                                PumpFlow(
                                    value =
                                        flow.toDouble(),
                                    unit =
                                        flowUnit
                                ),

                            head =
                                PumpHead(
                                    staticHeadM =
                                        staticHead.toDoubleOrNull()
                                            ?: 0.0,

                                    frictionHeadM =
                                        frictionHead.toDoubleOrNull()
                                            ?: 0.0,

                                    minorLossHeadM =
                                        minorHead.toDoubleOrNull()
                                            ?: 0.0,

                                    requiredPressureHeadM =
                                        pressureHead.toDoubleOrNull()
                                            ?: 0.0
                                ),

                            pumpEfficiency =
                                pumpEfficiency.toDouble() / 100.0,

                            motorEfficiency =
                                motorEfficiency.toDouble() / 100.0,

                            operatingHoursPerDay =
                                operatingHours.toDouble(),

                            operatingDaysPerMonth =
                                operatingDaysMonth.toDouble(),

                            operatingDaysPerYear =
                                operatingDaysYear.toDouble(),

                            energyTariffPerKwh =
                                tariff.toDouble()
                        )

                    result =
                        ElectricalCalculations.calculatePump(
                            input
                        )

                    error = null

                } catch (exception: Exception) {

                    result = null

                    error =
                        exception.message
                            ?: "Invalid input."
                }
            }
        ) {

            Text(
                if (arabic)
                    "احسب الطلمبة"
                else
                    "Calculate Pump"
            )
        }

        error?.let {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF5A1F1F)
                    )
            ) {

                Text(
                    text = it,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        result?.let {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            PumpResultCard(
                result = it,
                arabic = arabic
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )
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
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
               
