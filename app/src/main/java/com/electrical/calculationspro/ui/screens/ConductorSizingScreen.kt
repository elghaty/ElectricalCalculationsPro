package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
                (
                    loadKw.toDoubleOrNull()
                        ?: 0.0
                    ) * 1000.0,

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

            error =
                null

        } catch (exception: Exception) {

            result =
                null

            error =
                exception.message
                    ?: "Invalid engineering input."
        }
    }

    fun selectSection(
        section: Double
    ) {

        try {

            val calculated =
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
                calculated

            selectedSection =
                section

            error =
                null

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

            fontSize =
                21.sp,

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

            fontSize =
                13.sp,

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
                    Modifier.padding(14.dp),

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
                        currentType =
                            it
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
                            voltage =
                                it
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
                            loadKw =
                                it
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
                            powerFactor =
                                it
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
                            lineLength =
                                it
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
                            ambientTemp =
                                it
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
                            circuits =
                                it
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
                            maxDrop =
                                it
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
                                conductor =
                                    it
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
                        insulation =
                            it
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
                        method =
                            it
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

                onSectionSelected = {
                    selectSection(it)
                }
            )
        }
    }
}

@Composable
private fun FieldLabel(
    text: String
) {

    Text(
        text =
            text,

        color =
            TextSecondary,

        fontSize =
            12.sp,

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

    onValueChange:
        (String) -> Unit
) {

    OutlinedTextField(
        modifier =
            modifier.fillMaxWidth(),

        value =
            value,

        onValueChange = { newValue ->

            if (
                newValue.isEmpty() ||
                newValue.matches(
                    Regex(
                        """^-?\d*(\.\d*)?$"""
                    )
                )
            ) {

                onValueChange(
                    newValue
                )
            }
        },

        label = {
            Text(label)
        },

        singleLine =
            true
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded =
                            true
                    },

            value =
                text(value),

            onValueChange = {},

            readOnly =
                true,

            singleLine =
                true,

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

        DropdownMenu(
            expanded =
                expanded,

            onDismissRequest = {
                expanded =
                    false
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

                        onSelected(
                            item
                        )

                        expanded =
                            false
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

        verticalAlignment =
            Alignment.CenterVertically,

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
                Modifier.padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "نتائج اختيار الموصل"
                    } else {
                        "Conductor Sizing Results"
                    },

                color =
                    PrimaryTeal,

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold
            )

            ResultRow(
                title =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "تيار التصميم"
                    } else {
                        "Design Current"
                    },

                value =
                    "%.2f A".format(
                        result.designCurrent
                    )
            )

            ResultRow(
                title =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "المقطع المقترح"
                    } else {
                        "Recommended Section"
                    },

                value =
                    "%.1f mm²".format(
                        result.recommendedSection
                    )
            )

            ResultRow(
                title =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "سعة حمل الكابل"
                    } else {
                        "Cable Ampacity"
                    },

                value =
                    "%.1f A".format(
                        result.ampacity
                    )
            )

            ResultRow(
                title =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "هبوط الجهد"
                    } else {
                        "Voltage Drop"
                    },

                value =
                    "%.2f %% / %.2f V".format(
                        result.voltageDropPercent,
                        result.voltageDropVolts
                    )
            )

            ResultRow(
                title =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "القاطع المقترح"
                    } else {
                        "Protective Device"
                    },

                value =
                    if (
                        result.protectiveDevice >
                        0.0
                    ) {

                        "%.0f A".format(
                            result.protectiveDevice
                        )

                    } else {

                        "NOT VALID"
                    }
            )

            ResultRow(
                title =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "تيار القصر"
                    } else {
                        "Short Circuit Current"
                    },

                value =
                    "%.2f kA".format(
                        result.shortCircuitCurrentKA
                    )
            )

            ResultRow(
                title =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "تنسيق القاطع مع الكابل"
                    } else {
                        "Breaker / Cable Coordination"
                    },

                value =
                    if (
                        result.breakerWithinCableCapacity
                    ) {
                        "PASS"
                    } else {
                        "CHECK"
                    }
            )

            ResultRow(
                title =
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "هبوط الجهد ضمن الحد"
                    } else {
                        "Voltage Drop Limit"
                    },

                value =
                    if (
                        result.voltageDropWithinLimit
                    ) {
                        "PASS"
                    } else {
                        "CHECK"
                    }
            )

            FieldLabel(
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "اختيار مقطع بديل"
                } else {
                    "Select Alternative Section"
                }
            )

            SectionSelector(
                selectedSection =
                    selectedSection,

                onSelected =
                    onSectionSelected
            )

            if (
                result.notes.isNotEmpty()
            ) {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    result.notes.forEach { note ->

                        Text(
                            text =
                                "• $note",

                            color =
                                TextSecondary,

                            fontSize =
                                12.sp
                        )
                    }
                }
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
        modifier =
            Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded =
                            true
                    },

            value =
                "%.1f mm²".format(
                    selectedSection
                ),

            onValueChange = {},

            readOnly =
                true,

            singleLine =
                true,

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

        DropdownMenu(
            expanded =
                expanded,

            onDismissRequest = {
                expanded =
                    false
            }
        ) {

            standardSections.forEach { section ->

                DropdownMenuItem(
                    text = {

                        Text(
                            "%.1f mm²".format(
                                section
                            )
                        )
                    },

                    onClick = {

                        expanded =
                            false

                        onSelected(
                            section
                        )
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
