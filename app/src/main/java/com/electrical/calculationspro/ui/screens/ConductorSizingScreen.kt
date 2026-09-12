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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.electrical.calculationspro.data.Strings
import com.electrical.calculationspro.data.iecInstallationMethods
import com.electrical.calculationspro.data.standardSections
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.DarkSurface
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorSizingScreen(
    language: AppLanguage,
    standard: Standard
) {
    fun t(key: String): String = Strings.get(key, language)

    var currentType by remember { mutableStateOf(CurrentType.AlternatingSinglePhase) }
    var voltage by remember { mutableStateOf("230") }
    var load by remember { mutableStateOf("5000") }
    var powerFactor by remember { mutableStateOf("0.90") }
    var lineLength by remember { mutableStateOf("60") }
    var ambientTemp by remember { mutableStateOf("30") }
    var circuits by remember { mutableStateOf("1") }
    var maxDrop by remember { mutableStateOf("4") }

    var conductor by remember { mutableStateOf(ConductorMaterial.Copper) }
    var insulation by remember { mutableStateOf(InsulationType.PVC) }
    var method by remember { mutableStateOf(iecInstallationMethods.first()) }

    var result by remember { mutableStateOf<ConductorSizingResult?>(null) }
    var selectedSection by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = t("conductor_sizing_protection"),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when (standard) {
                    Standard.IEC -> "IEC 60364-5-52"
                    Standard.EGYPTIAN -> t("egyptian_code")
                    Standard.CEI -> "CEI 64-8"
                    Standard.NEC -> "NEC / NFPA 70"
                    Standard.CEC -> "Canadian Electrical Code"
                },
                color = PrimaryTeal,
                fontSize = 13.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = t("current_type"),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    EnumDropdown(
                        value = currentType,
                        values = CurrentType.entries,
                        label = {
                            when (it) {
                                CurrentType.DirectCurrent ->
                                    t("direct_current")

                                CurrentType.AlternatingSinglePhase ->
                                    t("alternating_single")

                                CurrentType.AlternatingTwoPhase ->
                                    t("alternating_two")

                                CurrentType.AlternatingThreePhase ->
                                    t("alternating_three")
                            }
                        },
                        onSelected = {
                            currentType = it
                        }
                    )

                    NumberField(
                        label = t("voltage"),
                        value = voltage,
                        onValueChange = { voltage = it }
                    )

                    NumberField(
                        label = t("load"),
                        value = load,
                        onValueChange = { load = it }
                    )

                    NumberField(
                        label = t("power_factor_label"),
                        value = powerFactor,
                        onValueChange = { powerFactor = it }
                    )

                    NumberField(
                        label = t("line_length"),
                        value = lineLength,
                        onValueChange = { lineLength = it }
                    )

                    NumberField(
                        label = t("ambient_temp"),
                        value = ambientTemp,
                        onValueChange = { ambientTemp = it }
                    )

                    NumberField(
                        label = t("circuits_conduit"),
                        value = circuits,
                        onValueChange = { circuits = it }
                    )

                    NumberField(
                        label = t("max_voltage_drop"),
                        value = maxDrop,
                        onValueChange = { maxDrop = it }
                    )

                    Text(
                        text = t("method_installation"),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    InstallationDropdown(
                        value = method,
                        onSelected = {
                            method = it
                        }
                    )

                    Text(
                        text = t("conductor"),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    EnumDropdown(
                        value = conductor,
                        values = ConductorMaterial.entries,
                        label = {
                            when (it) {
                                ConductorMaterial.Copper -> t("copper")
                                ConductorMaterial.Aluminum -> t("aluminum")
                            }
                        },
                        onSelected = {
                            conductor = it
                        }
                    )

                    Text(
                        text = t("insulation"),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    EnumDropdown(
                        value = insulation,
                        values = InsulationType.entries,
                        label = {
                            when (it) {
                                InsulationType.PVC -> t("pvc")
                                InsulationType.XLPE -> t("xlpe")
                                InsulationType.EPR -> "EPR"
                                InsulationType.Rubber -> "Rubber"
                            }
                        },
                        onSelected = {
                            insulation = it
                        }
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            result = null
                            selectedSection = null
                            error = null

                            try {
                                val input = ConductorSizingInput(
                                    currentType = currentType,
                                    voltage = voltage.toDouble(),
                                    load = load.toDouble(),
                                    powerFactor = powerFactor.toDouble(),
                                    lineLength = lineLength.toDouble(),
                                    installationMethod = method,
                                    ambientTemp = ambientTemp.toDouble(),
                                    conductor = conductor,
                                    insulation = insulation,
                                    circuitsInConduit = circuits.toInt(),
                                    maxVoltageDrop = maxDrop.toDouble()
                                )

                                val calculated =
                                    ElectricalCalculations.sizeConductor(
                                        input = input,
                                        standard = standard
                                    )

                                result = calculated
                                selectedSection = calculated.selectedSection
                            } catch (e: Exception) {
                                error = e.message ?: "Invalid input"
                            }
                        }
                    ) {
                        Text(
                            text = t("calculate"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            error?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            result?.let { calculation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = t("results"),
                            color = PrimaryTeal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        ResultRow(
                            title = t("design_current"),
                            value = "%.2f A".format(calculation.designCurrent)
                        )

                        ResultRow(
                            title = t("recommended_section"),
                            value = "%.1f mm²".format(
                                calculation.recommendedSection
                            )
                        )

                        ResultRow(
                            title = t("ampacity"),
                            value = "%.1f A".format(calculation.ampacity)
                        )

                        ResultRow(
                            title = t("voltage_drop_result"),
                            value = "%.2f %% (%.2f V)".format(
                                calculation.voltageDropPercent,
                                calculation.voltageDropVolts
                            )
                        )

                        ResultRow(
                            title = t("protective_device"),
                            value = "%.0f A".format(
                                calculation.protectiveDevice
                            )
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = t("selected_section"),
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        EnumDropdown(
                            value = selectedSection
                                ?: calculation.selectedSection,
                            values = standardSections,
                            label = {
                                "%.1f mm²".format(it)
                            },
                            onSelected = {
                                selectedSection = it
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = t("notes"),
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )

                        calculation.notes.forEach { note ->
                            Text(
                                text = "• $note",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (
                newValue.isEmpty() ||
                newValue.matches(
                    Regex("^-?\\d*(\\.\\d*)?$")
                )
            ) {
                onValueChange(newValue)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    value: T,
    values: List<T>,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = label(value),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            singleLine = true
        )

        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            values.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(label(item))
                    },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallationDropdown(
    value: InstallationMethod,
    onSelected: (InstallationMethod) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = "${value.code} — ${value.description}",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            }
        )

        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            iecInstallationMethods.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = item.code,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    },
                    onClick = {
                        onSelected(item)
                        expanded = false
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}
