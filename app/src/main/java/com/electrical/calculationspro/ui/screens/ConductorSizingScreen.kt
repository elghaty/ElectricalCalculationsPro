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
import androidx.compose.foundation.layout.weight
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
    standard: Standard
) {

    fun t(key: String): String =
        Strings.get(key, language)

    var currentType by remember {
        mutableStateOf(
            CurrentType.AlternatingSinglePhase
        )
    }

    var voltage by remember {
        mutableStateOf("230")
    }

    var load by remember {
        mutableStateOf("5000")
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
                load.toDoubleOrNull()
                    ?: 0.0,

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
                ElectricalCalculations.sizeConductor(
                    input = buildInput(),
                    standard = standard
                )

            result = calculated

            selectedSection =
                calculated.selectedSection

            error = null

        } catch (e: Exception) {

            result = null

            error =
                e.message
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
                        input = buildInput(),
                        selectedSection = section,
                        standard = standard
                    )

            result = recalculated

            selectedSection = section

            error = null

        } catch (e: Exception) {

            error =
                e.message
                    ?: "Unable to evaluate selected section."
        }
    }

    Box(

        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)

    ) {

        Column(

            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)

        ) {

            Text(

                text =
                    t("conductor_sizing_protection"),

                color =
                    TextPrimary,

                fontSize = 20.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Text(

                text = when (standard) {

                    Standard.IEC ->
                        "IEC 60364-5-52"

                    Standard.EGYPTIAN ->
                        t("egyptian_code")

                    Standard.CEI ->
                        "CEI 64-8"

                    Standard.NEC ->
                        "NEC / NFPA 70"

                    Standard.CEC ->
                        "Canadian Electrical Code"
                },

                color =
                    PrimaryTeal,

                fontSize = 13.sp
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

                    Text(

                        text =
                            t("current_type"),

                        color =
                            PrimaryTeal,

                        fontWeight =
                            FontWeight.Bold
                    )

                    SelectionDropdown(

                        value =
                            currentType,

                        values =
                            CurrentType.values()
                                .toList(),

                        text = {

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

                    TwoColumnRow {

                        NumberField(
                            modifier =
                                Modifier.weight(1f),
                            label =
                                t("voltage"),
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
                                t("load"),
                            value =
                                load,
                            onValueChange = {
                                load = it
                            }
                        )
                    }

                    TwoColumnRow {

                        NumberField(
                            modifier =
                                Modifier.weight(1f),
                            label =
                                t("power_factor_label"),
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
                                t("line_length"),
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
                                t("max_voltage_drop"),
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

                            Text(
                                text =
                                    t("conductor"),
                                color =
                                    TextSecondary,
                                fontSize =
                                    12.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            SelectionDropdown(
                                value =
                                    conductor,

                                values =
                                    ConductorMaterial
                                        .values()
                                        .toList(),

                                text = {

                                    when (it) {

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

                    Column {

                        Text(
                            text =
                                t("insulation"),
                            color =
                                TextSecondary,
                            fontSize =
                                12.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        SelectionDropdown(

                            value =
                                insulation,

                            values =
                                InsulationType
                                    .values()
                                    .toList(),

                            text = {

                                when (it) {

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
                    }

                    Column {

                        Text(
                            text =
                                t("method_installation"),
                            color =
                                TextSecondary,
                            fontSize =
                                12.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        SelectionDropdown(

                            value =
                                method,

                            values =
                                iecInstallationMethods,

                            text = {
                                "${it.code} — ${it.description}"
                            },

                            onSelected = {
                                method = it
                            }
                        )
                    }

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
                    }
                }
            }

            result?.let { calculation ->

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
                            Modifier.padding(16.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)

                    ) {

                        Text(

                            text =
                                t("results"),

                            color =
                                PrimaryTeal,

                            fontSize =
                                18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        ResultRow(
                            t("design_current"),
                            "%.2f A".format(
                                calculation.designCurrent
                            )
                        )

                        ResultRow(
                            t("recommended_section"),
                            "%.1f mm²".format(
                                calculation
                                    .recommendedSection
                            )
                        )

                        ResultRow(
                            t("ampacity"),
                            "%.1f A".format(
                                calculation.ampacity
                            )
                        )

                        ResultRow(
                            t("voltage_drop_result"),
                            "%.2f %% (%.2f V)".format(
                                calculation
                                    .voltageDropPercent,
                                calculation
                                    .voltageDropVolts
                            )
                        )

                        ResultRow(

                            t("protective_device"),

                            if (
                                calculation
                                    .protectiveDevice > 0.0
                            ) {

                                "%.0f A".format(
                                    calculation
                                        .protectiveDevice
                                )

                            } else {

                                "NOT VALID"
                            }
                        )

                        ResultRow(

                            "Short Circuit",

                            "%.2f kA".format(
                                calculation
                                    .shortCircuitCurrentKA
                            )
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(

                            text =
                                t("selected_section"),

                            color =
                                TextPrimary,

                            fontWeight =
                                FontWeight.SemiBold
                        )

                        SelectionDropdown(

                            value =
                                selectedSection
                                    ?: calculation
                                        .selectedSection,

                            values =
                                standardSections,

                            text = {
                                "%.1f mm²".format(it)
                            },

                            onSelected = {
                                selectSection(it)
                            }
                        )

                        StatusRow(

                            title =
                                "Voltage Drop",

                            valid =
                                calculation
                                    .voltageDropWithinLimit,

                            validText =
                                "PASS",

                            invalidText =
                                "FAIL"
                        )

                        StatusRow(

                            title =
                                "Protection Coordination",

                            valid =
                                calculation
                                    .breakerWithinCableCapacity,

                            validText =
                                "Ib ≤ In ≤ Iz",

                            invalidText =
                                "CHECK"
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(

                            text =
                                t("notes"),

                            color =
                                PrimaryTeal,

                            fontWeight =
                                FontWeight.Bold
                        )

                        calculation.notes.forEach { note ->

                            Text(

                                text =
                                    "• $note",

                                color =
                                    TextSecondary,

                                fontSize =
                                    13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoColumnRow(
    content: @Composable RowScope.() -> Unit
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp),

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        content()
    }
}

@Composable
private fun NumberField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(

        value =
            value,

        onValueChange = { newValue ->

            if (
                newValue.isEmpty() ||
                newValue.matches(
                    Regex(
                        "^-?\\d*(\\.\\d*)?$"
                    )
                )
            ) {

                onValueChange(
                    newValue
                )
            }
        },

        modifier =
            modifier.fillMaxWidth(),

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

    Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(

            value =
                text(value),

            onValueChange = {},

            readOnly = true,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = true
                    },

            singleLine = true
        )

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
private fun ResultRow(
    title: String,
    value: String
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Text(
            text =
                title,
            color =
                TextSecondary
        )

        Text(
            text =
                value,
            color =
                TextPrimary,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusRow(
    title: String,
    valid: Boolean,
    validText: String,
    invalidText: String
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Text(
            text =
                title,
            color =
                TextSecondary
        )

        Text(

            text =
                if (valid) {
                    validText
                } else {
                    invalidText
                },

            color =
                if (valid) {
                    PrimaryTeal
                } else {
                    MaterialTheme
                        .colorScheme
                        .error
                },

            fontWeight =
                FontWeight.Bold
        )
    }
}
