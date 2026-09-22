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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.iecInstallationMethods
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.DarkSurface
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary
import java.util.Locale

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
        mutableStateOf(
            iecInstallationMethods.firstOrNull()
                ?: InstallationMethod(
                    code = "B1",
                    description = "Reference installation method"
                )
        )
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
                ?: error(
                    if (arabic) "أدخل جهدًا صحيحًا"
                    else "Enter a valid voltage"
                )

        val loadKw =
            loadKwText.toDoubleOrNull()
                ?: error(
                    if (arabic) "أدخل الحمل بالكيلووات"
                    else "Enter the load in kW"
                )

        require(loadKw > 0.0) {
            if (arabic) "الحمل يجب أن يكون أكبر من صفر"
            else "Load must be greater than zero"
        }

        val powerFactor =
            powerFactorText.toDoubleOrNull()
                ?: error(
                    if (arabic) "أدخل معامل قدرة صحيح"
                    else "Enter a valid power factor"
                )

        require(powerFactor > 0.0 && powerFactor <= 1.0) {
            if (arabic) {
                "معامل القدرة يجب أن يكون بين 0 و 1"
            } else {
                "Power factor must be between 0 and 1"
            }
        }

        val length =
            lineLengthText.toDoubleOrNull()
                ?: error(
                    if (arabic) "أدخل طولًا صحيحًا"
                    else "Enter a valid line length"
                )

        require(length > 0.0) {
            if (arabic) "طول الخط يجب أن يكون أكبر من صفر"
            else "Line length must be greater than zero"
        }

        val ambient =
            ambientTempText.toDoubleOrNull()
                ?: error(
                    if (arabic) "أدخل درجة حرارة صحيحة"
                    else "Enter a valid ambient temperature"
                )

        val circuits =
            circuitsText.toIntOrNull()
                ?: error(
                    if (arabic) "أدخل عدد دوائر صحيح"
                    else "Enter a valid number of circuits"
                )

        require(circuits > 0) {
            if (arabic) "عدد الدوائر يجب أن يكون أكبر من صفر"
            else "Number of circuits must be greater than zero"
        }

        val maxDrop =
            maxDropText.toDoubleOrNull()
                ?: error(
                    if (arabic) "أدخل حد هبوط جهد صحيح"
                    else "Enter a valid voltage-drop limit"
                )

        return ConductorSizingInput(
            currentType = currentType,
            voltage = voltage,
            load = loadKw * 1000.0,
            powerFactor = powerFactor,
            lineLength = length,
            installationMethod = installationMethod,
            ambientTemp = ambient,
            conductor = conductor,
            insulation = insulation,
            circuitsInConduit = circuits,
            maxVoltageDrop = maxDrop
        )
    }

    fun calculate() {
        errorMessage = null
        result = null
        selectedSectionResult = null

        try {
            val input = buildInput()

            result =
                if (autoSelect) {
                    ElectricalCalculations.sizeConductor(
                        input = input,
                        standard = standard
                    )
                } else {
                    val section =
                        selectedSectionText.toDoubleOrNull()
                            ?: error(
                                if (arabic) {
                                    "أدخل قطاعًا صحيحًا"
                                } else {
                                    "Enter a valid section"
                                }
                            )

                    ElectricalCalculations.evaluateSelectedSection(
                        input = input,
                        selectedSectionMm2 = section,
                        standard = standard
                    )
                }
        } catch (e: Exception) {
            errorMessage =
                e.message
                    ?: if (arabic) {
                        "حدث خطأ أثناء الحساب"
                    } else {
                        "Calculation error"
                    }
        }
    }

    fun evaluateSelected() {
        errorMessage = null
        selectedSectionResult = null

        try {
            val input = buildInput()

            val section =
                selectedSectionText.toDoubleOrNull()
                    ?: error(
                        if (arabic) {
                            "أدخل قطاعًا صحيحًا"
                        } else {
                            "Enter a valid section"
                        }
                    )

            selectedSectionResult =
                ElectricalCalculations.evaluateSelectedSection(
                    input = input,
                    selectedSectionMm2 = section,
                    standard = standard
                )
        } catch (e: Exception) {
            errorMessage =
                e.message
                    ?: if (arabic) {
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
                    text =
                        if (arabic) "رجوع" else "Back",
                    color =
                        MaterialTheme.colorScheme.primary
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text =
                if (arabic) {
                    "حساب واختيار الموصل"
                } else {
                    "Conductor Sizing"
                },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text =
                "${standard.shortName} • " +
                    ElectricalCalculations
                        .standardCodeName(standard),
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

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
                    text =
                        if (arabic) {
                            "بيانات الدائرة"
                        } else {
                            "Circuit Data"
                        },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                FourColumnRow(
                    first = {
                        SelectionField(
                            label =
                                if (arabic) {
                                    "نوع التيار"
                                } else {
                                    "Current Type"
                                },
                            value =
                                currentTypeLabel(
                                    currentType,
                                    arabic
                                ),
                            options =
                                CurrentType.entries.map {
                                    currentTypeLabel(
                                        it,
                                        arabic
                                    )
                                },
                            onSelected = { selected ->
                                currentType =
                                    CurrentType.entries.first {
                                        currentTypeLabel(
                                            it,
                                            arabic
                                        ) == selected
                                    }
                            }
                        )
                    },
                    second = {
                        NumberField(
                            label =
                                if (arabic) {
                                    "الجهد V"
                                } else {
                                    "Voltage V"
                                },
                            value = voltageText,
                            onValueChange = {
                                voltageText = it
                            }
                        )
                    },
                    third = {
                        NumberField(
                            label =
                                if (arabic) {
                                    "الحمل kW"
                                } else {
                                    "Load kW"
                                },
                            value = loadKwText,
                            onValueChange = {
                                loadKwText = it
                            }
                        )
                    },
                    fourth = {
                        NumberField(
                            label =
                                if (arabic) {
                                    "معامل القدرة"
                                } else {
                                    "Power Factor"
                                },
                            value = powerFactorText,
                            onValueChange = {
                                powerFactorText = it
                            }
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                FourColumnRow(
                    first = {
                        NumberField(
                            label =
                                if (arabic) {
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
                            label =
                                if (arabic) {
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
                            label =
                                if (arabic) {
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
                            label =
                                if (arabic) {
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

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                FourColumnRow(
                    first = {
                        SelectionField(
                            label =
                                if (arabic) {
                                    "الموصل"
                                } else {
                                    "Conductor"
                                },
                            value =
                                conductorLabel(
                                    conductor,
                                    arabic
                                ),
                            options =
                                ConductorMaterial.entries.map {
                                    conductorLabel(
                                        it,
                                        arabic
                                    )
                                },
                            onSelected = { selected ->
                                conductor =
                                    ConductorMaterial.entries.first {
                                        conductorLabel(
                                            it,
                                            arabic
                                        ) == selected
                                    }
                            }
                        )
                    },
                    second = {
                        SelectionField(
                            label =
                                if (arabic) {
                                    "العازل"
                                } else {
                                    "Insulation"
                                },
                            value =
                                insulationLabel(
                                    insulation
                                ),
                            options =
                                InsulationType.entries.map {
                                    insulationLabel(it)
                                },
                            onSelected = { selected ->
                                insulation =
                                    InsulationType.entries.first {
                                        insulationLabel(it) ==
                                            selected
                                    }
                            }
                        )
                    },
                    third = {
                        SelectionField(
                            label =
                                if (arabic) {
                                    "طريقة التركيب"
                                } else {
                                    "Installation"
                                },
                            value =
                                installationLabel(
                                    installationMethod
                                ),
                            options =
                                iecInstallationMethods.map {
                                    installationLabel(it)
                                },
                            onSelected = { selected ->
                                installationMethod =
                                    iecInstallationMethods.first {
                                        installationLabel(it) ==
                                            selected
                                    }
                            }
                        )
                    },
                    fourth = {
                        SelectionField(
                            label =
                                if (arabic) {
                                    "طريقة اختيار القطاع"
                                } else {
                                    "Section Mode"
                                },
                            value =
                                if (autoSelect) {
                                    if (arabic) {
                                        "تلقائي"
                                    } else {
                                        "Automatic"
                                    }
                                } else {
                                    if (arabic) {
                                        "يدوي"
                                    } else {
                                        "Manual"
                                    }
                                },
                            options =
                                listOf(
                                    if (arabic) {
                                        "تلقائي"
                                    } else {
                                        "Automatic"
                                    },
                                    if (arabic) {
                                        "يدوي"
                                    } else {
                                        "Manual"
                                    }
                                ),
                            onSelected = { selected ->
                                autoSelect =
                                    selected ==
                                        if (arabic) {
                                            "تلقائي"
                                        } else {
                                            "Automatic"
                                        }
                            }
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                FourColumnRow(
                    first = {
                        NumberField(
                            label =
                                if (arabic) {
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

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        calculate()
                    }
                ) {
                    Text(
                        if (arabic) {
                            "احسب"
                        } else {
                            "Calculate"
                        }
                    )
                }

                if (!autoSelect) {
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            evaluateSelected()
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
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        errorMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .errorContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onErrorContainer
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        result?.let { data ->
            ResultCard(
                title =
                    if (arabic) {
                        "نتيجة حساب الموصل"
                    } else {
                        "Conductor Sizing Result"
                    }
            ) {
                FourColumnRow(
                    first = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "تيار التصميم"
                                } else {
                                    "Design Current"
                                },
                            value =
                                formatNumber(
                                    data.designCurrent
                                ) + " A"
                        )
                    },
                    second = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "القطاع المقترح"
                                } else {
                                    "Recommended Section"
                                },
                            value =
                                formatNumber(
                                    data.recommendedSection
                                ) + " mm²"
                        )
                    },
                    third = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "القطاع المختار"
                                } else {
                                    "Selected Section"
                                },
                            value =
                                formatNumber(
                                    data.selectedSection
                                ) + " mm²"
                        )
                    },
                    fourth = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "السعة"
                                } else {
                                    "Ampacity"
                                },
                            value =
                                formatNumber(
                                    data.ampacity
                                ) + " A"
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                FourColumnRow(
                    first = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "هبوط الجهد"
                                } else {
                                    "Voltage Drop"
                                },
                            value =
                                formatNumber(
                                    data.voltageDropPercent
                                ) + " %"
                        )
                    },
                    second = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "هبوط الجهد بالفولت"
                                } else {
                                    "Voltage Drop V"
                                },
                            value =
                                formatNumber(
                                    data.voltageDropVolts
                                ) + " V"
                        )
                    },
                    third = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "القاطع"
                                } else {
                                    "Protective Device"
                                },
                            value =
                                if (data.protectiveDevice > 0.0) {
                                    formatNumber(
                                        data.protectiveDevice
                                    ) + " A"
                                } else {
                                    if (arabic) {
                                        "غير متاح"
                                    } else {
                                        "Not Available"
                                    }
                                }
                        )
                    },
                    fourth = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "تيار القصر"
                                } else {
                                    "Short Circuit"
                                },
                            value =
                                formatNumber(
                                    data.shortCircuitCurrentKA
                                ) + " kA"
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                FourColumnRow(
                    first = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "تنسيق القاطع والكابل"
                                } else {
                                    "Breaker / Cable"
                                },
                            value =
                                if (
                                    data.breakerWithinCableCapacity
                                ) {
                                    "PASS"
                                } else {
                                    "CHECK"
                                }
                        )
                    },
                    second = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "حد هبوط الجهد"
                                } else {
                                    "Voltage Drop Limit"
                                },
                            value =
                                if (
                                    data.voltageDropWithinLimit
                                ) {
                                    "PASS"
                                } else {
                                    "CHECK"
                                }
                        )
                    },
                    third = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "الكابل"
                                } else {
                                    "Catalog Cable"
                                },
                            value =
                                data.catalogCable?.model
                                    ?: "—"
                        )
                    },
                    fourth = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "القاطع المختار"
                                } else {
                                    "Catalog Breaker"
                                },
                            value =
                                data.catalogBreaker?.model
                                    ?: "—"
                        )
                    }
                )

                if (data.notes.isNotEmpty()) {
                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            if (arabic) {
                                "ملاحظات هندسية"
                            } else {
                                "Engineering Notes"
                            },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    data.notes.forEach { note ->
                        Text(
                            text = "• $note",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier =
                                Modifier.padding(
                                    vertical = 2.dp
                                )
                        )
                    }
                }
            }
        }

        selectedSectionResult?.let { data ->
            Spacer(
                modifier = Modifier.height(16.dp)
            )

            ResultCard(
                title =
                    if (arabic) {
                        "تقييم القطاع المختار"
                    } else {
                        "Selected Section Evaluation"
                    }
            ) {
                FourColumnRow(
                    first = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "القطاع"
                                } else {
                                    "Section"
                                },
                            value =
                                formatNumber(
                                    data.selectedSection
                                ) + " mm²"
                        )
                    },
                    second = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "السعة"
                                } else {
                                    "Ampacity"
                                },
                            value =
                                formatNumber(
                                    data.ampacity
                                ) + " A"
                        )
                    },
                    third = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "هبوط الجهد"
                                } else {
                                    "Voltage Drop"
                                },
                            value =
                                formatNumber(
                                    data.voltageDropPercent
                                ) + " %"
                        )
                    },
                    fourth = {
                        ResultRow(
                            label =
                                if (arabic) {
                                    "الحالة"
                                } else {
                                    "Status"
                                },
                            value =
                                if (
                                    data.breakerWithinCableCapacity &&
                                        data.voltageDropWithinLimit
                                ) {
                                    "PASS"
                                } else {
                                    "CHECK"
                                }
                        )
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
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
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
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

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (
                    newValue.isEmpty() ||
                    newValue.matches(
                        Regex(
                            """^-?\d*(\.\d*)?$"""
                        )
                    )
                ) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

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

            Spacer(
                modifier = Modifier.height(12.dp)
            )

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
        CurrentType.DirectCurrent ->
            if (arabic) "تيار مستمر" else "DC"

        CurrentType.AlternatingSinglePhase ->
            if (arabic) "أحادي فاز" else "Single Phase"

        CurrentType.AlternatingTwoPhase ->
            if (arabic) "ثنائي فاز" else "Two Phase"

        CurrentType.AlternatingThreePhase ->
            if (arabic) "ثلاثي فاز" else "Three Phase"
    }
}

private fun conductorLabel(
    value: ConductorMaterial,
    arabic: Boolean
): String {
    return when (value) {
        ConductorMaterial.Copper ->
            if (arabic) "نحاس" else "Copper"

        ConductorMaterial.Aluminum ->
            if (arabic) "ألومنيوم" else "Aluminum"
    }
}

private fun insulationLabel(
    value: InsulationType
): String {
    return value.name
}

private fun installationLabel(
    value: InstallationMethod
): String {
    return "${value.code} • ${value.description}"
}

private fun formatNumber(
    value: Double
): String {
    return String.format(
        Locale.US,
        "%.2f",
        value
    )
}
