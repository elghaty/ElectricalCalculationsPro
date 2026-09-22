package com.electrical.calculationspro.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.ConductorSizingResult
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.Strings
import com.electrical.calculationspro.data.iecInstallationMethods
import com.electrical.calculationspro.data.standardSections
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.DarkSurface
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@Composable
fun ConductorSizingScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: (() -> Unit)? = null
) {
    val arabic = language == AppLanguage.ARABIC
    val context = LocalContext.current

    val goBack: () -> Unit = {
        if (onBack != null) {
            onBack.invoke()
        } else {
            (context as? Activity)?.finish()
        }
    }

    var currentType by remember {
        mutableStateOf(CurrentType.AlternatingThreePhase)
    }

    var voltageText by remember {
        mutableStateOf("400")
    }

    var loadKwText by remember {
        mutableStateOf("")
    }

    var powerFactorText by remember {
        mutableStateOf("0.85")
    }

    var lineLengthText by remember {
        mutableStateOf("50")
    }

    var ambientTempText by remember {
        mutableStateOf("30")
    }

    var circuitsText by remember {
        mutableStateOf("1")
    }

    var maxDropText by remember {
        mutableStateOf(
            ElectricalCalculations
                .maximumVoltageDropPercent(
                    standard = standard,
                    circuitCategory = "power"
                )
                .toString()
        )
    }

    var conductor by remember {
        mutableStateOf(ConductorMaterial.Copper)
    }

    var insulation by remember {
        mutableStateOf(InsulationType.XLPE)
    }

    var installationMethod by remember {
        mutableStateOf(iecInstallationMethods.firstOrNull() ?: "")
    }

    var autoSelect by remember {
        mutableStateOf(true)
    }

    var selectedSectionText by remember {
        mutableStateOf("16")
    }

    var result by remember {
        mutableStateOf<ConductorSizingResult?>(null)
    }

    var selectedSectionResult by remember {
        mutableStateOf<ConductorSizingResult?>(null)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    fun buildInput(): ConductorSizingInput {
        val voltage =
            voltageText.toDoubleOrNull()
                ?: error("Invalid voltage")

        val loadKw =
            loadKwText.toDoubleOrNull()
                ?: error("Invalid load")

        val powerFactor =
            powerFactorText.toDoubleOrNull()
                ?: error("Invalid power factor")

        val length =
            lineLengthText.toDoubleOrNull()
                ?: error("Invalid length")

        val ambient =
            ambientTempText.toDoubleOrNull()
                ?: error("Invalid ambient temperature")

        val circuits =
            circuitsText.toIntOrNull()
                ?: error("Invalid number of circuits")

        val maxDrop =
            maxDropText.toDoubleOrNull()
                ?: error("Invalid voltage drop")

        return ConductorSizingInput(
            currentType = currentType,
            voltage = voltage,
            loadKw = loadKw,
            powerFactor = powerFactor,
            lineLength = length,
            ambientTemperatureC = ambient,
            numberOfCircuits = circuits,
            maxVoltageDropPercent = maxDrop,
            conductorMaterial = conductor,
            insulationType = insulation,
            installationMethod = installationMethod,
            autoSelect = autoSelect,
            selectedSectionMm2 =
                selectedSectionText.toDoubleOrNull()
        )
    }

    fun calculate() {
        errorMessage = null
        result = null
        selectedSectionResult = null

        try {
            val input = buildInput()

            result =
                ElectricalCalculations.sizeConductor(
                    input = input,
                    standard = standard
                )
        } catch (e: Exception) {
            errorMessage =
                e.message ?: if (arabic) {
                    "حدث خطأ أثناء الحساب"
                } else {
                    "Calculation error"
                }
        }
    }

    fun selectSection() {
        errorMessage = null
        selectedSectionResult = null

        try {
            val input = buildInput()

            val section =
                selectedSectionText
                    .toDoubleOrNull()
                    ?: error("Invalid section")

            selectedSectionResult =
                ElectricalCalculations.evaluateSelectedSection(
                    input = input,
                    selectedSectionMm2 = section,
                    standard = standard
                )
        } catch (e: Exception) {
            errorMessage =
                e.message ?: if (arabic) {
                    "حدث خطأ أثناء تقييم القطاع"
                } else {
                    "Section evaluation error"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = goBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription =
                        if (arabic) "رجوع" else "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            TextButton(onClick = goBack) {
                Text(
                    text = if (arabic) "رجوع" else "Back",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            text = if (arabic) {
                "حساب واختيار الكابل"
            } else {
                "Conductor Sizing"
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text =
                "${standard.shortName} • " +
                    ElectricalCalculations.standardCodeName(standard),
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text = if (arabic) {
                        "بيانات الدائرة"
                    } else {
                        "Circuit Data"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )

                Spacer(modifier = Modifier.height(12.dp))

                FourColumnRow(
                    first = {
                        DropdownColumn(
                            label = if (arabic) "نوع التيار" else "Current Type",
                            value = currentTypeLabel(currentType, arabic),
                            options = CurrentType.entries.map {
                                currentTypeLabel(it, arabic)
                            },
                            onSelected = { value ->
                                currentType =
                                    CurrentType.entries.first {
                                        currentTypeLabel(it, arabic) == value
                                    }
                            }
                        )
                    },
                    second = {
                        NumberField(
                            label = if (arabic) "الجهد V" else "Voltage V",
                            value = voltageText,
                            onValueChange = {
                                voltageText = it
                            }
                        )
                    },
                    third = {
                        NumberField(
                            label = if (arabic) "الحمل kW" else "Load kW",
                            value = loadKwText,
                            onValueChange = {
                                loadKwText = it
                            }
                        )
                    },
                    fourth = {
                        NumberField(
                            label = if (arabic) "معامل القدرة" else "Power Factor",
                            value = powerFactorText,
                            onValueChange = {
                                powerFactorText = it
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FourColumnRow(
                    first = {
                        NumberField(
                            label = if (arabic) {
                                "طول الخط m"
                            } else {
                                "Length m"
                            },
                            value = lineLengthText,
                            onValueChange = {
                                lineLengthText = it
                            }
                        )
                    },
                    second = {
                        NumberField(
                            label = if (arabic) {
                                "درجة الحرارة °C"
                            } else {
                                "Ambient °C"
                            },
                            value = ambientTempText,
                            onValueChange = {
                                ambientTempText = it
                            }
                        )
                    },
                    third = {
                        NumberField(
                            label = if (arabic) {
                                "عدد الدوائر"
                            } else {
                                "Circuits"
                            },
                            value = circuitsText,
                            onValueChange = {
                                circuitsText = it
                            }
                        )
                    },
                    fourth = {
                        NumberField(
                            label = if (arabic) {
                                "أقصى هبوط %"
                            } else {
                                "Max Drop %"
                            },
                            value = maxDropText,
                            onValueChange = {
                                maxDropText = it
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FourColumnRow(
                    first = {
                        DropdownColumn(
                            label = if (arabic) "الموصل" else "Conductor",
                            value = conductorLabel(conductor, arabic),
                            options = ConductorMaterial.entries.map {
                                conductorLabel(it, arabic)
                            },
                            onSelected = { value ->
                                conductor =
                                    ConductorMaterial.entries.first {
                                        conductorLabel(it, arabic) == value
                                    }
                            }
                        )
                    },
                    second = {
                        DropdownColumn(
                            label = if (arabic) "العازل" else "Insulation",
                            value = insulationLabel(insulation, arabic),
                            options = InsulationType.entries.map {
                                insulationLabel(it, arabic)
                            },
                            onSelected = { value ->
                                insulation =
                                    InsulationType.entries.first {
                                        insulationLabel(it, arabic) == value
                                    }
                            }
                        )
                    },
                    third = {
                        DropdownColumn(
                            label = if (arabic) {
                                "طريقة التركيب"
                            } else {
                                "Installation"
                            },
                            value = installationMethod,
                            options = iecInstallationMethods,
                            onSelected = {
                                installationMethod = it
                            }
                        )
                    },
                    fourth = {
                        DropdownColumn(
                            label = if (arabic) {
                                "اختيار القطاع"
                            } else {
                                "Section Mode"
                            },
                            value = if (autoSelect) {
                                if (arabic) "تلقائي" else "Automatic"
                            } else {
                                if (arabic) "يدوي" else "Manual"
                            },
                            options = listOf(
                                if (arabic) "تلقائي" else "Automatic",
                                if (arabic) "يدوي" else "Manual"
                            ),
                            onSelected = {
                                autoSelect =
                                    it ==
                                        if (arabic) {
                                            "تلقائي"
                                        } else {
                                            "Automatic"
                                        }
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FourColumnRow(
                    first = {
                        NumberField(
                            label = if (arabic) {
                                "القطاع المختار mm²"
                            } else {
                                "Selected Section mm²"
                            },
                            value = selectedSectionText,
                            onValueChange = {
                                selectedSectionText = it
                            }
                        )
                    },
                    second = {},
                    third = {},
                    fourth = {}
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        calculate()
                    }
                ) {
                    Text(
                        if (arabic) {
                            "احسب واختَر القطاع"
                        } else {
                            "Calculate & Select"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color =
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        result?.let { data ->
            ResultCard(
                title = if (arabic) {
                    "نتيجة اختيار الكابل"
                } else {
                    "Cable Selection Result"
                }
            ) {
                FourColumnRow(
                    first = {
                        ResultRow(
                            if (arabic) "تيار التصميم" else "Design Current",
                            formatNumber(data.designCurrentA) + " A"
                        )
                    },
                    second = {
                        ResultRow(
                            if (arabic) "القطاع" else "Section",
                            formatNumber(data.selectedSectionMm2) + " mm²"
                        )
                    },
                    third = {
                        ResultRow(
                            if (arabic) "السعة" else "Ampacity",
                            data.ampacityA?.let {
                                formatNumber(it) + " A"
                            } ?: "—"
                        )
                    },
                    fourth = {
                        ResultRow(
                            if (arabic) "هبوط الجهد" else "Voltage Drop",
                            formatNumber(data.voltageDropPercent) + " %"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FourColumnRow(
                    first = {
                        ResultRow(
                            if (arabic) "الطول" else "Length",
                            formatNumber(data.lineLengthM) + " m"
                        )
                    },
                    second = {
                        ResultRow(
                            if (arabic) "درجة الحرارة" else "Ambient",
                            formatNumber(data.ambientTemperatureC) + " °C"
                        )
                    },
                    third = {
                        ResultRow(
                            if (arabic) "معامل التجميع" else "Grouping Factor",
                            formatNumber(data.groupingFactor)
                        )
                    },
                    fourth = {
                        ResultRow(
                            if (arabic) "معامل الحرارة" else "Temperature Factor",
                            formatNumber(data.temperatureFactor)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectSection()
                    }
                ) {
                    Text(
                        if (arabic) {
                            "تقييم القطاع المختار"
                        } else {
                            "Evaluate Selected Section"
                        }
                    )
                }
            }
        }

        selectedSectionResult?.let { data ->
            Spacer(modifier = Modifier.height(16.dp))

            ResultCard(
                title = if (arabic) {
                    "تقييم القطاع"
                } else {
                    "Section Evaluation"
                }
            ) {
                FourColumnRow(
                    first = {
                        ResultRow(
                            if (arabic) "القطاع" else "Section",
                            formatNumber(data.selectedSectionMm2) + " mm²"
                        )
                    },
                    second = {
                        ResultRow(
                            if (arabic) "السعة" else "Ampacity",
                            data.ampacityA?.let {
                                formatNumber(it) + " A"
                            } ?: "—"
                        )
                    },
                    third = {
                        ResultRow(
                            if (arabic) "هبوط الجهد" else "Voltage Drop",
                            formatNumber(data.voltageDropPercent) + " %"
                        )
                    },
                    fourth = {
                        ResultRow(
                            if (arabic) "الحالة" else "Status",
                            if (data.isAcceptable) {
                                if (arabic) "مقبول" else "PASS"
                            } else {
                                if (arabic) "غير مقبول" else "FAIL"
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FourColumnRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
    fourth: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            first()
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            second()
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            third()
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            fourth()
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun <T> SelectionDropdown(
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = true
                },
            readOnly = true,
            singleLine = true
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
private fun DropdownColumn(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        SelectionDropdown(
            value = value,
            options = options,
            onSelected = onSelected
        )
    }
}

@Composable
private fun ResultCard(
    title: String,
    content: @Composable Column.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryTeal
            )

            Spacer(modifier = Modifier.height(12.dp))

            content(this)
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

private fun currentTypeLabel(
    value: CurrentType,
    arabic: Boolean
): String {
    return when (value) {
        CurrentType.AlternatingSinglePhase ->
            if (arabic) "أحادي فاز" else "Single Phase"

        CurrentType.AlternatingThreePhase ->
            if (arabic) "ثلاثي فاز" else "Three Phase"

        CurrentType.DirectCurrent ->
            if (arabic) "تيار مستمر" else "DC"
    }
}

private fun conductorLabel(
    value: ConductorMaterial,
    arabic: Boolean
): String {
    return when (value) {
        ConductorMaterial.Copper ->
            if (arabic) "نحاس" else "Copper"

        ConductorMaterial.Aluminium ->
            if (arabic) "ألومنيوم" else "Aluminium"
    }
}

private fun insulationLabel(
    value: InsulationType,
    arabic: Boolean
): String {
    return when (value) {
        InsulationType.PVC -> "PVC"
        InsulationType.XLPE -> "XLPE"
        InsulationType.EPR -> "EPR"
        InsulationType.Rubber -> "Rubber"
    }
}

private fun formatNumber(value: Double): String {
    return String.format(
        java.util.Locale.US,
        "%.2f",
        value
    )
}
