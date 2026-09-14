package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.Strings
import com.electrical.calculationspro.data.iecInstallationMethods
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.DarkSurface
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@Composable
fun ConductorSizingScreen(
    language: AppLanguage,
    standard: Standard
) {

    fun t(key: String): String =
        Strings.get(
            key,
            language
        )

    var currentType by remember {
        mutableStateOf(
            CurrentType.AlternatingSinglePhase
        )
    }

    var voltage by remember {
        mutableStateOf("230")
    }

    var loadKw by remember {
        mutableStateOf("5")
    }

    var powerFactor by remember {
        mutableStateOf("0.90")
    }

    var lineLength by remember {
        mutableStateOf("60")
    }

    var ambientTemp by remember {
        mutableStateOf("30")
    }

    var circuits by remember {
        mutableStateOf("1")
    }

    var maxDrop by remember {
        mutableStateOf("4")
    }

    var conductor by remember {
        mutableStateOf(
            ConductorMaterial.Copper
        )
    }

    var insulation by remember {
        mutableStateOf(
            InsulationType.PVC
        )
    }

    var method by remember {
        mutableStateOf(
            iecInstallationMethods.first()
        )
    }

    var result by remember {
        mutableStateOf<ConductorSizingResult?>(null)
    }

    var selectedSection by remember {
        mutableStateOf<Double?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    fun buildInput(): ConductorSizingInput {

        return ConductorSizingInput(

            currentType =
                currentType,

            voltage =
                voltage.toDoubleOrNull()
                    ?: 0.0,

            load =
                (loadKw.toDoubleOrNull()
                    ?: 0.0) * 1000.0,

            powerFactor =
                powerFactor.toDoubleOrNull()
                    ?: 0.0,

            lineLength =
                lineLength.toDoubleOrNull()
                    ?: 0.0,

            installationMethod =
                method,

            ambientTemp =
                ambientTemp.toDoubleOrNull()
                    ?: 0.0,

            conductor =
                conductor,

            insulation =
                insulation,

            circuitsInConduit =
                circuits.toIntOrNull()
                    ?: 0,

            maxVoltageDrop =
                maxDrop.toDoubleOrNull()
                    ?: 0.0
        )
    }

    fun calculate() {

        try {

            val calculated =
                ElectricalCalculations
                    .sizeConductor(
                        input =
                            buildInput(),
                        standard =
                            standard
                    )

            result =
                calculated

            selectedSection =
                calculated.selectedSection

            error = null

        } catch (exception: Exception) {

            result = null

            error =
                exception.message
                    ?: "Invalid engineering input."
        }
    }

    fun selectSection(
        section: Double
    ) {

        try {

            val recalculated =
                ElectricalCalculations
                    .evaluateSelectedSection(
                        input =
                            buildInput(),
                        selectedSection =
                            section,
                        standard =
                            standard
                    )

            result =
                recalculated

            selectedSection =
                section

            error = null

        } catch (exception: Exception) {

            error =
                exception.message
                    ?: "Unable to evaluate selected section."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                DarkBackground
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text =
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "اختيار مقطع الموصل"
                } else {
                    "Conductor Sizing"
                },
            color =
                TextPrimary,
            fontSize = 21.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                when (standard) {

                    Standard.IEC ->
                        "IEC 60364-5-52"

                    Standard.EGYPTIAN ->
                        "Egyptian Code"

                    Standard.CEI ->
                        "CEI 64-8"

                    Standard.NEC ->
                        "NEC / NFPA 70"

                    Standard.CEC ->
                        "Canadian Electrical Code"
                },
            color =
                PrimaryTeal,
            fontSize = 13.sp,
            fontWeight =
                FontWeight.SemiBold
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        DarkSurface
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(12.dp),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                FieldLabel(
                    t("current_type")
                )

                SelectionDropdown(
                    value =
                        currentType,
                    values =
                        CurrentType.values()
                            .toList(),
                    text = { item ->

                        when (item) {

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

                TwoColumnRow {

                    NumberField(
                        modifier =
                            Modifier.weight(1f),
                        label =
                            "${t("voltage")} (V)",
                        value =
                            voltage,
                        onValueChange = {
                            voltage = it
                        }
                    )

                    NumberField(
                        modifier =
                            Modifier.weight(1f),
                        label =
                            if (
                                language ==
                                AppLanguage.ARABIC
                            ) {
                                "الحمل (kW)"
                            } else {
                                "Load (kW)"
                            },
                        value =
                            loadKw,
                        onValueChange = {
                            loadKw = it
                        }
                    )
                }

                TwoColumnRow {

                    NumberField(
                        modifier =
                            Modifier.weight(1f),
                        label =
                            t(
                                "power_factor_label"
                            ),
                        value =
                            powerFactor,
                        onValueChange = {
                            powerFactor = it
                        }
                    )

                    NumberField(
                        modifier =
                            Modifier.weight(1f),
                        label =
                            "${t("line_length")} (m)",
                        value =
                            lineLength,
                        onValueChange = {
                            lineLength = it
                        }
                    )
                }

                TwoColumnRow {

                    NumberField(
                        modifier =
                            Modifier.weight(1f),
                        label =
                            t("ambient_temp"),
                        value =
                            ambientTemp,
                        onValueChange = {
                            ambientTemp = it
                        }
                    )

                    NumberField(
                        modifier =
                            Modifier.weight(1f),
                        label =
                            t("circuits_conduit"),
                        value =
                            circuits,
                        onValueChange = {
                            circuits = it
                        }
                    )
                }

                TwoColumnRow {

                    NumberField(
                        modifier =
                            Modifier.weight(1f),
                        label =
                            "${t("max_voltage_drop")} (%)",
                        value =
                            maxDrop,
                        onValueChange = {
                            maxDrop = it
                        }
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        FieldLabel(
                            t("conductor")
                        )

                        SelectionDropdown(
                            value =
                                conductor,
                            values =
                                ConductorMaterial
                                    .values()
                                    .toList(),
                            text = { item ->

                                when (item) {

                                    ConductorMaterial.Copper ->
                                        t("copper")

                                    ConductorMaterial.Aluminum ->
                                        t("aluminum")
                                }
                            },
                            onSelected = {
                                conductor = it
                            }
                        )
                    }
                }

                FieldLabel(
                    t("insulation")
                )

                SelectionDropdown(
                    value =
                        insulation,
                    values =
                        InsulationType
                            .values()
                            .toList(),
                    text = { item ->

                        when (item) {

                            InsulationType.PVC ->
                                t("pvc")

                            InsulationType.XLPE ->
                                t("xlpe")

                            InsulationType.EPR ->
                                t("epr")

                            InsulationType.Rubber ->
                                t("rubber")
                        }
                    },
                    onSelected = {
                        insulation = it
                    }
                )

                FieldLabel(
                    t("method_installation")
                )

                SelectionDropdown(
                    value =
                        method,
                    values =
                        iecInstallationMethods,
                    text = { item ->
                        "${item.code} — ${item.description}"
                    },
                    onSelected = {
                        method = it
                    }
                )

                Button(
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick = {
                        calculate()
                    }
                ) {

                    Text(
                        text =
                            t("calculate"),
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        error?.let { message ->

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .errorContainer
                    )
            ) {

                Text(
                    text =
                        message,
                    modifier =
                        Modifier.padding(16.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onErrorContainer
                )
            }
        }

        result?.let { calculation ->

            ResultCard(
                language =
                    language,
                result =
                    calculation,
                selectedSection =
                    selectedSection
                        ?: calculation.selectedSection,
                onSectionSelected =
                    ::selectSection
            )
        }
    }
}

@Composable
private fun FieldLabel(
    text: String
) {

    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight =
            FontWeight.SemiBold
    )
}

@Composable
private fun NumberField(
    modifier: Modifier =
        Modifier.fillMaxWidth(),
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(
        modifier =
            modifier.fillMaxWidth(),
        value = value,
        onValueChange = { valueText ->

            if (
                valueText.isEmpty() ||
                valueText.matches(
                    Regex(
                        """^-?\d*(\.\d*)?$"""
                    )
                )
            ) {
                onValueChange(valueText)
            }
        },
        label = {
            Text(label)
        },
        singleLine = true
    )
}

@Composable
private fun <T> SelectionDropdown(
    value: T,
    values: List<T>,
    text: (T) -> String,
    onSelected: (T) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column {

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                text(value),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                Text(
                    text =
                        if (expanded) {
                            "▲"
                        } else {
                            "▼"
                        },
                    color =
                        PrimaryTeal
                )
            }
        )

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {
                expanded = true
            }
        ) {

            Text(
                if (expanded) {
                    "▲"
                } else {
                    "▼"
                }
            )
        }

        DropdownMenu(
            expanded =
                expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            values.forEach { item ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text(item)
                        )
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
private fun TwoColumnRow(
    content:
        @Composable RowScope.() -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(10.dp),
        content =
            content
    )
}

@Composable
private fun ResultCard(
    language: AppLanguage,
    result: ConductorSizingResult,
    selectedSection: Double,
    onSectionSelected:
        (Double) -> Unit
) {

    fun t(key: String): String =
        Strings.get(
            key,
            language
        )

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    DarkSurface
            )
    ) {

        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "نتيجة اختيار الموصل"
                    } else {
                        "Conductor Sizing Result"
                    },
                color =
                    TextPrimary,
                fontSize =
                    17.sp,
                fontWeight =
                    FontWeight.Bold
            )

            ResultRow(
                label =
                    t("design_current"),
                value =
                    "${format(result.designCurrent)} A"
            )

            ResultRow(
                label =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "المقطع المقترح"
                    } else {
                        "Recommended Section"
                    },
                value =
                    "${format(result.selectedSection)} mm²"
            )

            ResultRow(
                label =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "سعة حمل التيار"
                    } else {
                        "Current Capacity"
                    },
                value =
                    "${format(result.currentCapacity)} A"
            )

            ResultRow(
                label =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "هبوط الجهد"
                    } else {
                        "Voltage Drop"
                    },
                value =
                    "${format(result.voltageDropPercent)} %"
            )

            ResultRow(
                label =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "القاطع المقترح"
                    } else {
                        "Recommended Breaker"
                    },
                value =
                    "${format(result.recommendedBreakerRating)} A"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "اختر مقطعاً بديلاً للمراجعة"
                    } else {
                        "Select an alternative section"
                    },
                color =
                    TextSecondary,
                fontSize =
                    12.sp
            )

            SectionSelector(
                selectedSection =
                    selectedSection,
                sections =
                    result.availableSections,
                onSelected =
                    onSectionSelected
            )
        }
    }
}

@Composable
private fun SectionSelector(
    selectedSection: Double,
    sections: List<Double>,
    onSelected: (Double) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    BoxWithDropdown(
        expanded =
            expanded,
        onExpandedChange = {
            expanded = it
        }
    ) {

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                "${format(selectedSection)} mm²",
            onValueChange = {},
            readOnly = true,
            singleLine = true
        )

        DropdownMenu(
            expanded =
                expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            sections.forEach { section ->

                DropdownMenuItem(
                    text = {
                        Text(
                            "${format(section)} mm²"
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
private fun BoxWithDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {

    androidx.compose.foundation.layout.Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onExpandedChange(
                        !expanded
                    )
                }
    ) {

        content()
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                ),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text =
                label,
            color =
                TextSecondary,
            fontSize =
                12.sp
        )

        Text(
            text =
                value,
            color =
                TextPrimary,
            fontSize =
                12.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

private fun format(
    value: Double
): String {

    return when {

        value >= 1000.0 ->
            "%.0f".format(value)

        value >= 100.0 ->
            "%.0f".format(value)

        value >= 10.0 ->
            "%.1f".format(value)

        else ->
            "%.2f".format(value)
    }
}
