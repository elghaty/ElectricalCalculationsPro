package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.*
import com.electrical.calculationspro.ui.theme.*

@Composable
fun ConductorSizingScreen(
    language: AppLanguage = AppLanguage.ARABIC,
    standard: Standard = Standard.IEC
) {
    fun t(key: String) = Strings.get(key, language)

    var currentType by remember { mutableStateOf(CurrentType.AlternatingSinglePhase) }
    var voltage by remember { mutableStateOf("230") }
    var load by remember { mutableStateOf("5000") }
    var powerFactor by remember { mutableStateOf("0.9") }
    var lineLength by remember { mutableStateOf("60") }
    var ambientTemp by remember { mutableStateOf("30") }
    var maxVd by remember { mutableStateOf("4") }
    var conductor by remember { mutableStateOf(ConductorMaterial.Copper) }
    var insulation by remember { mutableStateOf(InsulationType.PVC) }
    var circuits by remember { mutableStateOf("1") }
    var demandFactor by remember { mutableStateOf("1.0") }
    var diversityFactor by remember { mutableStateOf("1.0") }
    var selectedMethod by remember { mutableStateOf(iecInstallationMethods[0]) }

    var result by remember { mutableStateOf<ConductorSizingResult?>(null) }
    val scrollState = rememberScrollState()

    val standardNote = when (standard) {
        Standard.IEC -> "IEC 60364-5-52"
        Standard.EGYPTIAN -> if (language == AppLanguage.ARABIC) "الكود المصري (IEC 60364)" else "Egyptian Code (IEC 60364)"
        Standard.CEI -> "CEI 64-8"
        Standard.NEC -> "NEC (NFPA 70)"
        Standard.CEC -> "CEC"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(standardNote, color = PrimaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp))

        Text(t("current_type"), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        CurrentType.values().forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = currentType == type, onClick = { currentType = type })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentType == type,
                    onClick = { currentType = type },
                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal, unselectedColor = TextSecondary)
                )
                Text(
                    text = when (type) {
                        CurrentType.DirectCurrent -> t("direct_current")
                        CurrentType.AlternatingSinglePhase -> t("alternating_single")
                        CurrentType.AlternatingTwoPhase -> t("alternating_two")
                        CurrentType.AlternatingThreePhase -> t("alternating_three")
                    },
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(12.dp))

        NumberInputRow(t("voltage"), voltage, { voltage = it }, "V")
        NumberInputRow(t("load"), load, { load = it }, "W")
        NumberInputRow(t("power_factor_label"), powerFactor, { powerFactor = it }, "")
        NumberInputRow(t("line_length"), lineLength, { lineLength = it }, "m")
        NumberInputRow(t("ambient_temp"), ambientTemp, { ambientTemp = it }, "°C")
        NumberInputRow(t("circuits_conduit"), circuits, { circuits = it }, "")
        NumberInputRow(t("max_voltage_drop"), maxVd, { maxVd = it }, "%")
        NumberInputRow("Demand Factor", demandFactor, { demandFactor = it }, "")
        NumberInputRow("Diversity Factor", diversityFactor, { diversityFactor = it }, "")

        Spacer(modifier = Modifier.height(8.dp))
        Text(t("conductor"), color = TextSecondary, fontSize = 14.sp)
        Row {
            listOf(ConductorMaterial.Copper, ConductorMaterial.Aluminum).forEach { mat ->
                FilterChip(
                    selected = conductor == mat,
                    onClick = { conductor = mat },
                    label = { Text(if (mat == ConductorMaterial.Copper) t("copper") else t("aluminum")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryTeal.copy(alpha = 0.3f),
                        selectedLabelColor = PrimaryTeal
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Text(t("insulation"), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Row {
            listOf(InsulationType.PVC, InsulationType.XLPE).forEach { ins ->
                FilterChip(
                    selected = insulation == ins,
                    onClick = { insulation = ins },
                    label = { Text(if (ins == InsulationType.PVC) t("pvc") else t("xlpe")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryTeal.copy(alpha = 0.3f),
                        selectedLabelColor = PrimaryTeal
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val input = ConductorSizingInput(
                    currentType = currentType,
                    voltage = voltage.toDoubleOrNull() ?: 230.0,
                    load = load.toDoubleOrNull() ?: 5000.0,
                    powerFactor = powerFactor.toDoubleOrNull() ?: 0.9,
                    lineLength = lineLength.toDoubleOrNull() ?: 60.0,
                    installationMethod = selectedMethod,
                    ambientTemp = ambientTemp.toDoubleOrNull() ?: 30.0,
                    conductor = conductor,
                    insulation = insulation,
                    circuitsInConduit = circuits.toIntOrNull() ?: 1,
                    maxVoltageDrop = maxVd.toDoubleOrNull() ?: 4.0
                )
                result = ElectricalCalculations.sizeConductor(
                    input = input,
                    standard = standard,
                    demandFactor = demandFactor.toDoubleOrNull() ?: 1.0,
                    diversityFactor = diversityFactor.toDoubleOrNull() ?: 1.0
                )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(t("calculate"), fontWeight = FontWeight.Bold)
        }

        result?.let { res ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(t("results"), color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    ResultRow(t("design_current"), "${"%.2f".format(res.designCurrent)} A")
                    ResultRow(t("recommended_section"), "${res.selectedSection} mm²")
                    ResultRow(t("ampacity"), "${"%.1f".format(res.ampacity)} A")
                    ResultRow(t("voltage_drop_result"), "\( {"%.2f".format(res.voltageDropPercent)} % ( \){"%.2f".format(res.voltageDropVolts)} V)")
                    ResultRow(t("protective_device"), "${"%.0f".format(res.protectiveDevice)} A")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(t("notes"), color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    res.notes.forEach { note ->
                        Text("• $note", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberInputRow(label: String, value: String, onValueChange: (String) -> Unit, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(150.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = DividerColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryTeal,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
        )
        if (unit.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(unit, color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
