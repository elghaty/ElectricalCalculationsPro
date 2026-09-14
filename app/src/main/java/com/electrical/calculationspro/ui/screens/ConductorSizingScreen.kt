package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import com.electrical.calculationspro.data.standardSections

@Composable
fun ConductorSizingScreen(
    language: AppLanguage,
    standard: String
) {
    val arabic = language == AppLanguage.ARABIC

    var loadKw by remember { mutableStateOf("100") }
    var voltage by remember { mutableStateOf("400") }
    var powerFactor by remember { mutableStateOf("0.90") }
    var length by remember { mutableStateOf("50") }
    var parallelRuns by remember { mutableStateOf("1") }

    var conductorType by remember {
        mutableStateOf(
            if (arabic) "نحاس" else "Copper"
        )
    }

    var installationMethod by remember {
        mutableStateOf(
            if (arabic) "داخل مواسير" else "Conduit"
        )
    }

    var insulation by remember {
        mutableStateOf(
            if (arabic) "XLPE" else "XLPE"
        )
    }

    var selectedSection by remember {
        mutableStateOf(0.0)
    }

    var result by remember {
        mutableStateOf<ElectricalCalculations.ConductorSizingResult?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1116))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (arabic) "اختيار مقطع الكابل" else "Conductor Sizing",
            color = Color.White,
            fontSize = 22.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InputField(
                value = loadKw,
                onValueChange = { loadKw = it },
                label = if (arabic) "الحمل kW" else "Load kW",
                modifier = Modifier.weight(1f)
            )

            InputField(
                value = voltage,
                onValueChange = { voltage = it },
                label = if (arabic) "الجهد V" else "Voltage V",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InputField(
                value = powerFactor,
                onValueChange = { powerFactor = it },
                label = if (arabic) "معامل القدرة" else "Power Factor",
                modifier = Modifier.weight(1f)
            )

            InputField(
                value = length,
                onValueChange = { length = it },
                label = if (arabic) "الطول m" else "Length m",
                modifier = Modifier.weight(1f)
            )

            InputField(
                value = parallelRuns,
                onValueChange = { parallelRuns = it },
                label = if (arabic) "عدد المسارات" else "Runs",
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = if (arabic) "بيانات الموصل" else "Conductor Data",
            color = Color.White,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionField(
                value = conductorType,
                label = if (arabic) "نوع الموصل" else "Conductor",
                options = if (arabic) {
                    listOf("نحاس", "ألومنيوم")
                } else {
                    listOf("Copper", "Aluminium")
                },
                onSelected = { conductorType = it },
                modifier = Modifier.weight(1f)
            )

            SelectionField(
                value = installationMethod,
                label = if (arabic) "طريقة التركيب" else "Installation",
                options = if (arabic) {
                    listOf(
                        "داخل مواسير",
                        "على حوامل كابلات",
                        "مدفون مباشرة",
                        "داخل مجاري"
                    )
                } else {
                    listOf(
                        "Conduit",
                        "Cable Tray",
                        "Direct Buried",
                        "Duct"
                    )
                },
                onSelected = { installationMethod = it },
                modifier = Modifier.weight(1f)
            )

            SelectionField(
                value = insulation,
                label = if (arabic) "العزل" else "Insulation",
                options = listOf("PVC", "XLPE", "EPR"),
                onSelected = { insulation = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        androidx.compose.material3.Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val kw = loadKw.toDoubleOrNull() ?: 0.0
                val v = voltage.toDoubleOrNull() ?: 400.0
                val pf = powerFactor.toDoubleOrNull() ?: 0.90
                val m = length.toDoubleOrNull() ?: 0.0
                val runs = parallelRuns.toIntOrNull() ?: 1

                result = ElectricalCalculations.sizeConductor(
                    loadKw = kw,
                    voltage = v,
                    powerFactor = pf,
                    lengthMeters = m,
                    parallelRuns = runs,
                    conductorType = conductorType,
                    installationMethod = installationMethod,
                    insulation = insulation,
                    standard = standard
                )

                selectedSection =
                    result?.recommendedSectionMm2 ?: 0.0
            }
        ) {
            Text(
                text = if (arabic) "احسب" else "Calculate"
            )
        }

        result?.let { calculated ->

            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (arabic) {
                            "نتائج الحساب"
                        } else {
                            "Calculation Results"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    ResultRow(
                        title = if (arabic) {
                            "تيار الحمل"
                        } else {
                            "Load Current"
                        },
                        value = "%.2f A".format(
                            calculated.designCurrentA
                        )
                    )

                    ResultRow(
                        title = if (arabic) {
                            "المقطع المقترح"
                        } else {
                            "Recommended Section"
                        },
                        value = "%.1f mm²".format(
                            calculated.recommendedSectionMm2
                        )
                    )

                    ResultRow(
                        title = if (arabic) {
                            "سعة التيار"
                        } else {
                            "Current Capacity"
                        },
                        value = "%.2f A".format(
                            calculated.currentCapacityA
                        )
                    )

                    ResultRow(
                        title = if (arabic) {
                            "هبوط الجهد"
                        } else {
                            "Voltage Drop"
                        },
                        value = "%.2f %%".format(
                            calculated.voltageDropPercent
                        )
                    )

                    ResultRow(
                        title = if (arabic) {
                            "تيار القصر"
                        } else {
                            "Short Circuit Current"
                        },
                        value = "%.2f kA".format(
                            calculated.shortCircuitCurrentKA
                        )
                    )

                    ResultRow(
                        title = if (arabic) {
                            "هبوط الجهد ضمن الحد"
                        } else {
                            "Voltage Drop Limit"
                        },
                        value = if (
                            calculated.voltageDropWithinLimit
                        ) {
                            "PASS"
                        } else {
                            "CHECK"
                        }
                    )

                    Text(
                        text = if (arabic) {
                            "اختيار مقطع بديل"
                        } else {
                            "Select Alternative Section"
                        },
                        color = Color.DarkGray
                    )

                    SectionSelector(
                        selectedSection = selectedSection,
                        onSelected = {
                            selectedSection = it

                            result =
                                ElectricalCalculations.evaluateSelectedSection(
                                    baseResult = calculated,
                                    selectedSection = it
                                )
                        }
                    )

                    if (calculated.notes.isNotEmpty()) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            calculated.notes.forEach { note ->
                                Text(
                                    text = "• $note",
                                    color = Color.DarkGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        singleLine = true
    )
}

@Composable
private fun SelectionField(
    value: String,
    label: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = {},
            label = {
                Text(label)
            },
            readOnly = true,
            singleLine = true
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    expanded = true
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionSelector(
    selectedSection: Double,
    onSelected: (Double) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = "%.1f mm²".format(selectedSection),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = Color(0xFF00BCD4),
                    fontSize = 12.sp
                )
            }
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    expanded = true
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            standardSections.forEach { section ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "%.1f mm²".format(section)
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(section)
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Color.DarkGray,
            fontSize = 13.sp
        )

        Text(
            text = value,
            color = Color.Black,
            fontSize = 13.sp
        )
    }
}
