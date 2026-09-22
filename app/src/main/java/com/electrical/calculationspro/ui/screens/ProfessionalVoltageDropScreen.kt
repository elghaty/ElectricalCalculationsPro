package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.iecInstallationMethods

private data class VoltageDropStudy(
    val currentA: Double,
    val requiredAmpacityA: Double,
    val selectedSectionMm2: Double?,
    val ampacityA: Double?,
    val temperatureFactor: Double,
    val groupingFactor: Double,
    val voltageDropV: Double,
    val voltageDropPercent: Double,
    val receivingVoltageV: Double,
    val allowedVoltageDropPercent: Double,
    val voltageDropPass: Boolean,
    val ampacityPass: Boolean,
    val notes: List<String>
)

@Composable
fun ProfessionalVoltageDropScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: (() -> Unit)? = null
) {
    val arabic = language == AppLanguage.ARABIC

    var currentType by remember {
        mutableStateOf(CurrentType.AlternatingThreePhase)
    }

    var voltage by remember { mutableStateOf("400") }
    var loadKw by remember { mutableStateOf("100") }
    var current by remember { mutableStateOf("") }
    var pf by remember { mutableStateOf("0.90") }
    var length by remember { mutableStateOf("50") }
    var section by remember { mutableStateOf("70") }
    var parallelRuns by remember { mutableStateOf("1") }
    var ambient by remember { mutableStateOf("30") }
    var circuits by remember { mutableStateOf("1") }
    var maxDrop by remember { mutableStateOf("5") }

    var material by remember {
        mutableStateOf(ConductorMaterial.Copper)
    }

    var insulation by remember {
        mutableStateOf(InsulationType.XLPE)
    }

    var installation by remember {
        mutableStateOf(
            iecInstallationMethods.firstOrNull()
                ?: InstallationMethod(
                    "DEFAULT",
                    "Default installation"
                )
        )
    }

    var study by remember {
        mutableStateOf<VoltageDropStudy?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )

                TextButton(onClick = it) {
                    Text(
                        if (arabic) "رجوع" else "Back"
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (arabic) {
                        "دراسة هبوط الجهد"
                    } else {
                        "Voltage Drop Study"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${standard.shortName} • ${
                        ElectricalCalculations.standardCodeName(standard)
                    }",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Outlined.ElectricalServices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                if (arabic) {
                    "نظام التغذية والحمل"
                } else {
                    "SYSTEM & LOAD"
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FourColumnRow(
                        first = {
                            DropdownField(
                                label = if (arabic) {
                                    "نوع النظام"
                                } else {
                                    "System Type"
                                },
                                value = currentTypeLabel(
                                    currentType,
                                    arabic
                                ),
                                items = CurrentType.entries.map {
                                    currentTypeLabel(it, arabic)
                                },
                                onSelect = { index ->
                                    currentType =
                                        CurrentType.entries[index]
                                }
                            )
                        },
                        second = {
                            NumberField(
                                label = if (arabic) {
                                    "جهد النظام (V)"
                                } else {
                                    "System Voltage (V)"
                                },
                                value = voltage,
                                onValueChange = {
                                    voltage = it
                                }
                            )
                        },
                        third = {
                            NumberField(
                                label = if (arabic) {
                                    "الحمل (kW)"
                                } else {
                                    "Load (kW)"
                                },
                                value = loadKw,
                                onValueChange = {
                                    loadKw = it
                                }
                            )
                        },
                        fourth = {
                            NumberField(
                                label = if (arabic) {
                                    "التيار A"
                                } else {
                                    "Current A"
                                },
                                value = current,
                                onValueChange = {
                                    current = it
                                }
                            )
                        }
                    )

                    FourColumnRow(
                        first = {
                            NumberField(
                                label = if (arabic) {
                                    "معامل القدرة"
                                } else {
                                    "Power Factor"
                                },
                                value = pf,
                                onValueChange = {
                                    pf = it
                                }
                            )
                        },
                        second = {},
                        third = {},
                        fourth = {}
                    )
                }
            }

            SectionTitle(
                if (arabic) {
                    "بيانات الكابل"
                } else {
                    "CABLE DATA"
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FourColumnRow(
                        first = {
                            NumberField(
                                label = if (arabic) {
                                    "الطول m"
                                } else {
                                    "Length m"
                                },
                                value = length,
                                onValueChange = {
                                    length = it
                                }
                            )
                        },
                        second = {
                            NumberField(
                                label = if (arabic) {
                                    "المقطع mm²"
                                } else {
                                    "Section mm²"
                                },
                                value = section,
                                onValueChange = {
                                    section = it
                                }
                            )
                        },
                        third = {
                            NumberField(
                                label = if (arabic) {
                                    "عدد المسارات"
                                } else {
                                    "Parallel Runs"
                                },
                                value = parallelRuns,
                                onValueChange = {
                                    parallelRuns = it
                                }
                            )
                        },
                        fourth = {
                            NumberField(
                                label = if (arabic) {
                                    "درجة الحرارة °C"
                                } else {
                                    "Ambient °C"
                                },
                                value = ambient,
                                onValueChange = {
                                    ambient = it
                                }
                            )
                        }
                    )

                    FourColumnRow(
                        first = {
                            NumberField(
                                label = if (arabic) {
                                    "عدد الدوائر"
                                } else {
                                    "Circuits"
                                },
                                value = circuits,
                                onValueChange = {
                                    circuits = it
                                }
                            )
                        },
                        second = {
                            DropdownField(
                                label = if (arabic) {
                                    "مادة الموصل"
                                } else {
                                    "Conductor Material"
                                },
                                value = material.name,
                                items = ConductorMaterial.entries.map {
                                    it.name
                                },
                                onSelect = { index ->
                                    material =
                                        ConductorMaterial.entries[index]
                                }
                            )
                        },
                        third = {
                            DropdownField(
                                label = if (arabic) {
                                    "العازل"
                                } else {
                                    "Insulation"
                                },
                                value = insulation.name,
                                items = InsulationType.entries.map {
                                    it.name
                                },
                                onSelect = { index ->
                                    insulation =
                                        InsulationType.entries[index]
                                }
                            )
                        },
                        fourth = {
                            DropdownField(
                                label = if (arabic) {
                                    "طريقة التركيب"
                                } else {
                                    "Installation Method"
                                },
                                value = installation.description,
                                items = iecInstallationMethods.map {
                                    it.description
                                },
                                onSelect = { index ->
                                    installation =
                                        iecInstallationMethods[index]
                                }
                            )
                        }
                    )
                }
            }

            SectionTitle(
                if (arabic) {
                    "حدود التصميم"
                } else {
                    "DESIGN LIMIT"
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    FourColumnRow(
                        first = {
                            NumberField(
                                label = if (arabic) {
                                    "أقصى هبوط جهد %"
                                } else {
                                    "Maximum Voltage Drop %"
                                },
                                value = maxDrop,
                                onValueChange = {
                                    maxDrop = it
                                }
                            )
                        },
                        second = {},
                        third = {},
                        fourth = {}
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        study = runCatching {
                            calculateStudy(
                                standard = standard,
                                currentType = currentType,
                                voltage = voltage.toDouble(),
                                loadKw = loadKw.toDoubleOrNull(),
                                currentInput = current.toDoubleOrNull(),
                                pf = pf.toDouble(),
                                length = length.toDouble(),
                                section = section.toDouble(),
                                parallelRuns = parallelRuns.toInt(),
                                ambient = ambient.toDouble(),
                                circuits = circuits.toInt(),
                                maxDrop = maxDrop.toDouble(),
                                material = material,
                                insulation = insulation,
                                installation = installation
                            )
                        }.getOrNull()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Calculate,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        if (arabic) {
                            "إجراء الدراسة"
                        } else {
                            "RUN STUDY"
                        }
                    )
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        study = runCatching {
                            calculateStudy(
                                standard = standard,
                                currentType = currentType,
                                voltage = voltage.toDouble(),
                                loadKw = loadKw.toDoubleOrNull(),
                                currentInput = current.toDoubleOrNull(),
                                pf = pf.toDouble(),
                                length = length.toDouble(),
                                section = section.toDouble(),
                                parallelRuns = parallelRuns.toInt(),
                                ambient = ambient.toDouble(),
                                circuits = circuits.toInt(),
                                maxDrop = maxDrop.toDouble(),
                                material = material,
                                insulation = insulation,
                                installation = installation,
                                autoSelect = true
                            )
                        }.getOrNull()
                    }
                ) {
                    Text(
                        if (arabic) {
                            "اختيار المقطع"
                        } else {
                            "AUTO SIZE"
                        }
                    )
                }
            }

            study?.let {
                StudyResultCard(
                    study = it,
                    arabic = arabic
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

private fun calculateStudy(
    standard: Standard,
    currentType: CurrentType,
    voltage: Double,
    loadKw: Double?,
    currentInput: Double?,
    pf: Double,
    length: Double,
    section: Double,
    parallelRuns: Int,
    ambient: Double,
    circuits: Int,
    maxDrop: Double,
    material: ConductorMaterial,
    insulation: InsulationType,
    installation: InstallationMethod,
    autoSelect: Boolean = false
): VoltageDropStudy {
    require(voltage > 0.0)
    require(pf > 0.0 && pf <= 1.0)
    require(length >= 0.0)
    require(section > 0.0)
    require(parallelRuns >= 1)
    require(circuits >= 1)
    require(maxDrop > 0.0)

    val calculatedCurrent =
        currentInput?.takeIf { it > 0.0 }
            ?: run {
                require(loadKw != null)
                require(loadKw > 0.0)

                ElectricalCalculations.calculateDesignCurrent(
                    loadWatts = loadKw * 1000.0,
                    voltage = voltage,
                    powerFactor = pf,
                    currentType = currentType
                )
            }

    val temperatureFactor =
        runCatching {
            ElectricalCalculations.ambientTemperatureFactor(
                standard = standard,
                insulation = insulation,
                ambientTemperatureC = ambient
            )
        }.getOrDefault(1.0)

    val groupingFactor =
        runCatching {
            ElectricalCalculations.groupingFactor(
                standard = standard,
                numberOfCircuits = circuits
            )
        }.getOrDefault(1.0)

    val effectiveCurrent =
        calculatedCurrent / parallelRuns

    val correctionFactor =
        (temperatureFactor * groupingFactor)
            .coerceAtLeast(0.01)

    val requiredAmpacity =
        effectiveCurrent / correctionFactor

    val sections =
        ElectricalCalculations
            .standardConductorSections(standard)
            .ifEmpty {
                listOf(
                    1.5,
                    2.5,
                    4.0,
                    6.0,
                    10.0,
                    16.0,
                    25.0,
                    35.0,
                    50.0,
                    70.0,
                    95.0,
                    120.0,
                    150.0,
                    185.0,
                    240.0,
                    300.0
                )
            }

    var selected =
        if (autoSelect) {
            null
        } else {
            section
        }

    var selectedAmpacity: Double? = null

    for (candidate in sections) {
        val ampacity =
            runCatching {
                ElectricalCalculations.conductorAmpacity(
                    standard = standard,
                    sectionMm2 = candidate,
                    material = material,
                    insulation = insulation,
                    installationMethod = installation,
                    loadedConductors =
                        when (currentType) {
                            CurrentType.AlternatingThreePhase -> 3
                            else -> 2
                        }
                )
            }.getOrNull()

        if (
            autoSelect &&
            ampacity != null &&
            ampacity * correctionFactor >= effectiveCurrent
        ) {
            selected = candidate
            selectedAmpacity = ampacity
            break
        }

        if (
            !autoSelect &&
            candidate == section
        ) {
            selectedAmpacity = ampacity
        }
    }

    if (selected == null) {
        selected = sections.lastOrNull()
    }

    require(selected != null)

    val actualSection = selected

    if (selectedAmpacity == null) {
        selectedAmpacity =
            runCatching {
                ElectricalCalculations.conductorAmpacity(
                    standard = standard,
                    sectionMm2 = actualSection,
                    material = material,
                    insulation = insulation,
                    installationMethod = installation,
                    loadedConductors =
                        when (currentType) {
                            CurrentType.AlternatingThreePhase -> 3
                            else -> 2
                        }
                )
            }.getOrNull()
    }

    val vd =
        ElectricalCalculations.calculateVoltageDrop(
            current = effectiveCurrent,
            length = length,
            sectionMm2 = actualSection,
            powerFactor = pf,
            currentType = currentType,
            material = material,
            voltage = voltage
        )

    val vdVolts = vd.second
    val vdPercent = vd.first

    val correctedAmpacity =
        selectedAmpacity?.let {
            it * temperatureFactor * groupingFactor
        }

    val totalAmpacity =
        correctedAmpacity?.let {
            it * parallelRuns
        }

    val ampacityPass =
        totalAmpacity == null ||
            totalAmpacity >= calculatedCurrent

    val voltageDropPass =
        vdPercent <= maxDrop

    val notes = mutableListOf<String>()

    if (selectedAmpacity == null) {
        notes +=
            "Verified ampacity data is not available for this exact standard/material/insulation/installation combination."
    }

    if (!ampacityPass) {
        notes +=
            "Selected conductor does not satisfy the calculated thermal current requirement."
    }

    if (!voltageDropPass) {
        notes +=
            "Voltage drop exceeds the specified design limit."
    }

    if (parallelRuns > 1) {
        notes +=
            "Parallel runs were included in the current and ampacity assessment."
    }

    if (autoSelect) {
        notes +=
            "Automatic sizing selected the first standard conductor section satisfying the available ampacity data."
    }

    return VoltageDropStudy(
        currentA = calculatedCurrent,
        requiredAmpacityA = requiredAmpacity,
        selectedSectionMm2 = actualSection,
        ampacityA = totalAmpacity,
        temperatureFactor = temperatureFactor,
        groupingFactor = groupingFactor,
        voltageDropV = vdVolts,
        voltageDropPercent = vdPercent,
        receivingVoltageV = voltage - vdVolts,
        allowedVoltageDropPercent = maxDrop,
        voltageDropPass = voltageDropPass,
        ampacityPass = ampacityPass,
        notes = notes
    )
}

@Composable
private fun StudyResultCard(
    study: VoltageDropStudy,
    arabic: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (arabic) {
                    "نتيجة الدراسة الهندسية"
                } else {
                    "ENGINEERING STUDY RESULT"
                },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            ResultRow(
                if (arabic) "تيار التصميم" else "Design Current",
                "%.3f A".format(study.currentA)
            )

            ResultRow(
                if (arabic) "المقطع المختار" else "Selected Section",
                "${study.selectedSectionMm2} mm²"
            )

            ResultRow(
                if (arabic) "Iz بعد معاملات التصحيح" else "Corrected Iz",
                study.ampacityA?.let {
                    "%.2f A".format(it)
                } ?: "N/A"
            )

            ResultRow(
                if (arabic) "معامل الحرارة" else "Temperature Factor",
                "%.3f".format(study.temperatureFactor)
            )

            ResultRow(
                if (arabic) "معامل التجميع" else "Grouping Factor",
                "%.3f".format(study.groupingFactor)
            )

            ResultRow(
                if (arabic) "هبوط الجهد" else "Voltage Drop",
                "%.3f V".format(study.voltageDropV)
            )

            ResultRow(
                if (arabic) "هبوط الجهد %" else "Voltage Drop %",
                "%.3f %%".format(study.voltageDropPercent)
            )

            ResultRow(
                if (arabic) "جهد الاستقبال" else "Receiving Voltage",
                "%.3f V".format(study.receivingVoltageV)
            )

            ResultRow(
                if (arabic) "الحد المسموح" else "Allowed Limit",
                "%.2f %%".format(
                    study.allowedVoltageDropPercent
                )
            )

            StatusRow(
                title = if (arabic) {
                    "اختبار هبوط الجهد"
                } else {
                    "Voltage Drop Check"
                },
                passValue = study.voltageDropPass,
                arabic = arabic
            )

            StatusRow(
                title = if (arabic) {
                    "اختبار سعة الكابل"
                } else {
                    "Cable Ampacity Check"
                },
                passValue = study.ampacityPass,
                arabic = arabic
            )

            if (study.notes.isNotEmpty()) {
                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                study.notes.forEach { note ->
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.width(20.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = note,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    passValue: Boolean,
    arabic: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector =
                if (passValue) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.Warning
                },
            contentDescription = null,
            tint =
                if (passValue) {
                    Color(0xFF2E7D32)
                } else {
                    Color(0xFFE65100)
                },
            modifier = Modifier.width(24.dp)
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text =
                if (passValue) {
                    if (arabic) "مطابق" else "PASS"
                } else {
                    if (arabic) "غير مطابق" else "FAIL"
                },
            color =
                if (passValue) {
                    Color(0xFF2E7D32)
                } else {
                    Color(0xFFE65100)
                },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )

        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionTitle(
    text: String
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = { input ->
            onValueChange(
                input.filter {
                    it.isDigit() ||
                        it == '.' ||
                        it == '-'
                }
            )
        },
        label = {
            Text(label)
        },
        singleLine = true
    )
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    items: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                expanded = true
            }
        ) {
            Text(
                text = "$label: $value",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(item)
                    },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            first()
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {
            second()
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {
            third()
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {
            fourth()
        }
    }
}

private fun currentTypeLabel(
    type: CurrentType,
    arabic: Boolean
): String {
    return when (type) {
        CurrentType.DirectCurrent ->
            if (arabic) {
                "تيار مستمر DC"
            } else {
                "DC"
            }

        CurrentType.AlternatingSinglePhase ->
            if (arabic) {
                "أحادي الطور 1Φ"
            } else {
                "Single Phase"
            }

        CurrentType.AlternatingTwoPhase ->
            if (arabic) {
                "ثنائي الطور 2Φ"
            } else {
                "Two Phase"
            }

        CurrentType.AlternatingThreePhase ->
            if (arabic) {
                "ثلاثي الطور 3Φ"
            } else {
                "Three Phase"
            }
    }
}
