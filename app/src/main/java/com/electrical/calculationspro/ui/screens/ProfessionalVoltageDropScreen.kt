
package com.electrical.calculationspro.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.iecInstallationMethods
import com.electrical.calculationspro.ui.components.EngineeringInput
import com.electrical.calculationspro.ui.components.EngineeringPage
import com.electrical.calculationspro.ui.components.EngineeringPrimaryButton
import com.electrical.calculationspro.ui.components.EngineeringResult
import com.electrical.calculationspro.ui.components.EngineeringSecondaryButton
import com.electrical.calculationspro.ui.components.EngineeringSection
import com.electrical.calculationspro.ui.components.EngineeringStatus
import com.electrical.calculationspro.ui.components.EngineeringValueRow
import java.util.Locale

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
    val context = LocalContext.current

    val backAction: () -> Unit = {
        if (onBack != null) {
            onBack()
        } else {
            (context as? Activity)?.finish()
        }
    }

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
                ?: InstallationMethod("DEFAULT", "Default installation")
        )
    }

    var study by remember {
        mutableStateOf<VoltageDropStudy?>(null)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }

    fun invalidateResult() {
        study = null
        error = null
    }

    fun runStudy(autoSelect: Boolean) {
        try {
            val parsedVoltage = voltage.toDoubleOrNull()
            val parsedLoad = loadKw.toDoubleOrNull()
            val parsedCurrent = current.toDoubleOrNull()
            val parsedPf = pf.toDoubleOrNull()
            val parsedLength = length.toDoubleOrNull()
            val parsedSection = section.toDoubleOrNull()
            val parsedRuns = parallelRuns.toIntOrNull()
            val parsedAmbient = ambient.toDoubleOrNull()
            val parsedCircuits = circuits.toIntOrNull()
            val parsedMaxDrop = maxDrop.toDoubleOrNull()

            when {
                parsedVoltage == null || !parsedVoltage.isFinite() ||
                    parsedVoltage <= 0.0 ->
                    throw IllegalArgumentException(
                        if (arabic) "جهد النظام يجب أن يكون أكبر من صفر." else
                            "System voltage must be greater than zero."
                    )

                parsedPf == null || !parsedPf.isFinite() ||
                    parsedPf <= 0.0 || parsedPf > 1.0 ->
                    throw IllegalArgumentException(
                        if (arabic) "معامل القدرة يجب أن يكون أكبر من صفر ولا يتجاوز 1." else
                            "Power factor must be greater than zero and no more than 1."
                    )

                parsedLength == null || !parsedLength.isFinite() ||
                    parsedLength < 0.0 ->
                    throw IllegalArgumentException(
                        if (arabic) "طول الكابل لا يمكن أن يكون سالبًا." else
                            "Cable length cannot be negative."
                    )

                parsedRuns == null || parsedRuns < 1 ->
                    throw IllegalArgumentException(
                        if (arabic) "عدد المسارات المتوازية يجب أن يكون 1 على الأقل." else
                            "Parallel runs must be at least 1."
                    )

                parsedAmbient == null || !parsedAmbient.isFinite() ->
                    throw IllegalArgumentException(
                        if (arabic) "أدخل درجة حرارة صحيحة." else
                            "Enter a valid ambient temperature."
                    )

                parsedCircuits == null || parsedCircuits < 1 ->
                    throw IllegalArgumentException(
                        if (arabic) "عدد الدوائر يجب أن يكون 1 على الأقل." else
                            "Number of circuits must be at least 1."
                    )

                parsedMaxDrop == null || !parsedMaxDrop.isFinite() ||
                    parsedMaxDrop <= 0.0 ->
                    throw IllegalArgumentException(
                        if (arabic) "الحد المسموح لهبوط الجهد يجب أن يكون أكبر من صفر." else
                            "Maximum voltage drop must be greater than zero."
                    )

                parsedCurrent != null &&
                    (!parsedCurrent.isFinite() || parsedCurrent < 0.0) ->
                    throw IllegalArgumentException(
                        if (arabic) "قيمة التيار غير صالحة." else
                            "Current value is invalid."
                    )

                parsedCurrent == null &&
                    (parsedLoad == null || !parsedLoad.isFinite() ||
                        parsedLoad <= 0.0) ->
                    throw IllegalArgumentException(
                        if (arabic) "أدخل حملًا موجبًا أو تيارًا صالحًا." else
                            "Enter a positive load or a valid current."
                    )

                !autoSelect &&
                    (parsedSection == null || !parsedSection.isFinite() ||
                        parsedSection <= 0.0) ->
                    throw IllegalArgumentException(
                        if (arabic) "مقطع الكابل يجب أن يكون أكبر من صفر." else
                            "Cable section must be greater than zero."
                    )
            }

            study = calculateStudy(
                standard = standard,
                currentType = currentType,
                voltage = parsedVoltage!!,
                loadKw = parsedLoad,
                currentInput = parsedCurrent,
                pf = parsedPf!!,
                length = parsedLength!!,
                section = parsedSection ?: 70.0,
                parallelRuns = parsedRuns!!,
                ambient = parsedAmbient!!,
                circuits = parsedCircuits!!,
                maxDrop = parsedMaxDrop!!,
                material = material,
                insulation = insulation,
                installation = installation,
                autoSelect = autoSelect
            )
            error = null

            val result = study
            if (result != null && result.selectedSectionMm2 != null) {
                section = formatNumber(result.selectedSectionMm2)
            }
        } catch (exception: Exception) {
            study = null
            error = exception.message
                ?: if (arabic) "تعذر تنفيذ الدراسة الهندسية." else
                    "The engineering study could not be completed."
        }
    }

    EngineeringPage(
        title = if (arabic) "دراسة هبوط الجهد" else "Voltage Drop Study",
        subtitle = if (arabic) {
            "حساب هبوط الجهد والتحقق من سعة الكابل وفق المعيار المختار"
        } else {
            "Voltage-drop calculation and cable ampacity verification"
        } + "\n${standard.shortName} • ${ElectricalCalculations.standardCodeName(standard)}",
        onBack = backAction
    ) {
        EngineeringSection(
            title = if (arabic) "نظام التغذية والحمل" else "System and Load"
        ) {
            ChoiceField(
                label = if (arabic) "نوع النظام" else "System Type",
                value = currentTypeLabel(currentType, arabic),
                items = CurrentType.entries.map {
                    currentTypeLabel(it, arabic)
                },
                onSelect = {
                    currentType = CurrentType.entries[it]
                    invalidateResult()
                }
            )

            EngineeringInput(
                value = voltage,
                label = if (arabic) "جهد النظام (V)" else "System Voltage (V)",
                onValueChange = {
                    voltage = it
                    invalidateResult()
                }
            )

            EngineeringInput(
                value = loadKw,
                label = if (arabic) "الحمل الفعال (kW)" else "Active Load (kW)",
                onValueChange = {
                    loadKw = it
                    invalidateResult()
                }
            )

            EngineeringInput(
                value = current,
                label = if (arabic) "التيار المحسوب مسبقًا (A) - اختياري" else
                    "Known Current (A) - optional",
                onValueChange = {
                    current = it
                    invalidateResult()
                }
            )

            EngineeringInput(
                value = pf,
                label = if (arabic) "معامل القدرة (PF)" else "Power Factor (PF)",
                onValueChange = {
                    pf = it
                    invalidateResult()
                }
            )

            Text(
                text = if (arabic) {
                    "إذا أدخلت تيارًا موجبًا، فسيُستخدم بدلًا من التيار المستنتج من الحمل."
                } else {
                    "A positive known current takes precedence over current calculated from load."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        EngineeringSection(
            title = if (arabic) "بيانات الكابل والتركيب" else "Cable and Installation"
        ) {
            EngineeringInput(
                value = length,
                label = if (arabic) "طول المسار في اتجاه واحد (m)" else
                    "One-way Route Length (m)",
                onValueChange = {
                    length = it
                    invalidateResult()
                }
            )

            EngineeringInput(
                value = section,
                label = if (arabic) "مقطع الموصل (mm²)" else "Conductor Section (mm²)",
                onValueChange = {
                    section = it
                    invalidateResult()
                }
            )

            EngineeringInput(
                value = parallelRuns,
                label = if (arabic) "عدد المسارات المتوازية" else "Parallel Runs",
                onValueChange = {
                    parallelRuns = it
                    invalidateResult()
                }
            )

            EngineeringInput(
                value = ambient,
                label = if (arabic) "درجة الحرارة المحيطة (°C)" else
                    "Ambient Temperature (°C)",
                onValueChange = {
                    ambient = it
                    invalidateResult()
                }
            )

            EngineeringInput(
                value = circuits,
                label = if (arabic) "عدد الدوائر المجمعة" else
                    "Number of Grouped Circuits",
                onValueChange = {
                    circuits = it
                    invalidateResult()
                }
            )

            ChoiceField(
                label = if (arabic) "مادة الموصل" else "Conductor Material",
                value = materialLabel(material, arabic),
                items = ConductorMaterial.entries.map {
                    materialLabel(it, arabic)
                },
                onSelect = {
                    material = ConductorMaterial.entries[it]
                    invalidateResult()
                }
            )

            ChoiceField(
                label = if (arabic) "نوع العازل" else "Insulation",
                value = insulationLabel(insulation, arabic),
                items = InsulationType.entries.map {
                    insulationLabel(it, arabic)
                },
                onSelect = {
                    insulation = InsulationType.entries[it]
                    invalidateResult()
                }
            )

            ChoiceField(
                label = if (arabic) "طريقة التركيب" else "Installation Method",
                value = installation.description,
                items = iecInstallationMethods.map { it.description },
                onSelect = {
                    installation = iecInstallationMethods[it]
                    invalidateResult()
                }
            )
        }

        EngineeringSection(
            title = if (arabic) "حدود التصميم" else "Design Limits"
        ) {
            EngineeringInput(
                value = maxDrop,
                label = if (arabic) "أقصى هبوط جهد مسموح (%)" else
                    "Maximum Allowed Voltage Drop (%)",
                onValueChange = {
                    maxDrop = it
                    invalidateResult()
                }
            )

            Text(
                text = if (arabic) {
                    "يجب تحديد حد هبوط الجهد طبقًا لمتطلبات المشروع والكود المعتمد؛ القيمة الافتراضية 5% ليست اعتمادًا تلقائيًا."
                } else {
                    "Set the voltage-drop limit from project requirements and the adopted code. The default 5% is not automatic code approval."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EngineeringPrimaryButton(
                text = if (arabic) "إجراء الدراسة" else "Run Study",
                onClick = { runStudy(autoSelect = false) },
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Calculate,
                        contentDescription = null
                    )
                }
            )

            EngineeringSecondaryButton(
                text = if (arabic) "اختيار المقطع" else "Auto Size",
                onClick = { runStudy(autoSelect = true) },
                modifier = Modifier.weight(1f)
            )
        }

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        study?.let {
            StudyResultSection(
                study = it,
                arabic = arabic
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ChoiceField(
    label: String,
    value: String,
    items: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                modifier = Modifier.fillMaxWidth()
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
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
    require(voltage.isFinite() && voltage > 0.0)
    require(pf.isFinite() && pf > 0.0 && pf <= 1.0)
    require(length.isFinite() && length >= 0.0)
    require(section.isFinite() && section > 0.0)
    require(parallelRuns >= 1)
    require(circuits >= 1)
    require(ambient.isFinite())
    require(maxDrop.isFinite() && maxDrop > 0.0)

    val calculatedCurrent = currentInput?.takeIf { it > 0.0 } ?: run {
        require(loadKw != null && loadKw.isFinite() && loadKw > 0.0)

        ElectricalCalculations.calculateDesignCurrent(
            loadWatts = loadKw * 1000.0,
            voltage = voltage,
            powerFactor = pf,
            currentType = currentType
        )
    }

    require(calculatedCurrent.isFinite() && calculatedCurrent > 0.0) {
        "Calculated current must be greater than zero."
    }

    val temperatureFactor = runCatching {
        ElectricalCalculations.ambientTemperatureFactor(
            standard = standard,
            insulation = insulation,
            ambientTemperatureC = ambient
        )
    }.getOrDefault(1.0)

    val groupingFactor = runCatching {
        ElectricalCalculations.groupingFactor(
            standard = standard,
            numberOfCircuits = circuits
        )
    }.getOrDefault(1.0)

    val effectiveCurrent = calculatedCurrent / parallelRuns
    val correctionFactor =
        (temperatureFactor * groupingFactor).coerceAtLeast(0.01)
    val requiredAmpacity = effectiveCurrent / correctionFactor

    val sections = ElectricalCalculations.standardConductorSections(standard)
        .ifEmpty {
            listOf(
                1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0,
                50.0, 70.0, 95.0, 120.0, 150.0, 185.0, 240.0, 300.0
            )
        }

    var selected: Double? = if (autoSelect) null else section
    var selectedAmpacity: Double? = null

    for (candidate in sections) {
        val ampacity = runCatching {
            ElectricalCalculations.conductorAmpacity(
                standard = standard,
                sectionMm2 = candidate,
                material = material,
                insulation = insulation,
                installationMethod = installation,
                loadedConductors = if (
                    currentType == CurrentType.AlternatingThreePhase
                ) 3 else 2
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

        if (!autoSelect && candidate == section) {
            selectedAmpacity = ampacity
        }
    }

    if (selected == null) {
        selected = sections.lastOrNull()
    }

    require(selected != null) {
        "No conductor sections are available for the selected standard."
    }

    val actualSection = selected

    if (selectedAmpacity == null) {
        selectedAmpacity = runCatching {
            ElectricalCalculations.conductorAmpacity(
                standard = standard,
                sectionMm2 = actualSection,
                material = material,
                insulation = insulation,
                installationMethod = installation,
                loadedConductors = if (
                    currentType == CurrentType.AlternatingThreePhase
                ) 3 else 2
            )
        }.getOrNull()
    }

    val voltageDrop = ElectricalCalculations.calculateVoltageDrop(
        current = effectiveCurrent,
        length = length,
        sectionMm2 = actualSection,
        powerFactor = pf,
        currentType = currentType,
        material = material,
        voltage = voltage
    )

    val voltageDropPercent = voltageDrop.first
    val voltageDropVolts = voltageDrop.second

    require(voltageDropPercent.isFinite() && voltageDropVolts.isFinite()) {
        "The voltage-drop calculation returned an invalid value."
    }

    val correctedAmpacity = selectedAmpacity?.let {
        it * temperatureFactor * groupingFactor
    }
    val totalAmpacity = correctedAmpacity?.let {
        it * parallelRuns
    }

    val ampacityPass = totalAmpacity != null &&
        totalAmpacity >= calculatedCurrent
    val voltageDropPass = voltageDropPercent <= maxDrop

    val notes = mutableListOf<String>()

    if (selectedAmpacity == null) {
        notes += if (true) {
            "Verified ampacity data is not available for this exact standard/material/insulation/installation combination."
        } else {
            ""
        }
    }
    if (!ampacityPass) {
        notes += "Selected conductor does not satisfy the calculated thermal current requirement."
    }
    if (!voltageDropPass) {
        notes += "Voltage drop exceeds the specified design limit."
    }
    if (parallelRuns > 1) {
        notes += "Parallel runs were included in the current and ampacity assessment."
    }
    if (autoSelect) {
        notes += "Automatic sizing selected the first standard conductor section satisfying the available ampacity data."
    }

    return VoltageDropStudy(
        currentA = calculatedCurrent,
        requiredAmpacityA = requiredAmpacity,
        selectedSectionMm2 = actualSection,
        ampacityA = totalAmpacity,
        temperatureFactor = temperatureFactor,
        groupingFactor = groupingFactor,
        voltageDropV = voltageDropVolts,
        voltageDropPercent = voltageDropPercent,
        receivingVoltageV = voltage - voltageDropVolts,
        allowedVoltageDropPercent = maxDrop,
        voltageDropPass = voltageDropPass,
        ampacityPass = ampacityPass,
        notes = notes
    )
}

@Composable
private fun StudyResultSection(
    study: VoltageDropStudy,
    arabic: Boolean
) {
    EngineeringSection(
        title = if (arabic) "نتيجة الدراسة الهندسية" else "Engineering Results"
    ) {
        EngineeringResult(
            title = if (arabic) "هبوط الجهد" else "Voltage Drop",
            value = "${formatNumber(study.voltageDropV)} V  (${formatNumber(study.voltageDropPercent)}%)",
            success = study.voltageDropPass
        )

        EngineeringResult(
            title = if (arabic) "المقطع المختار" else "Selected Conductor Section",
            value = "${formatNumber(study.selectedSectionMm2 ?: 0.0)} mm²",
            success = study.ampacityPass
        )

        EngineeringValueRow(
            label = if (arabic) "تيار التصميم" else "Design Current",
            value = "${formatNumber(study.currentA)} A"
        )
        EngineeringValueRow(
            label = if (arabic) "السعة بعد التصحيح" else "Corrected Ampacity",
            value = study.ampacityA?.let { "${formatNumber(it)} A" }
                ?: if (arabic) "غير متاحة" else "Unavailable"
        )
        EngineeringValueRow(
            label = if (arabic) "الحد المطلوب للسعة" else "Required Ampacity",
            value = "${formatNumber(study.requiredAmpacityA)} A"
        )
        EngineeringValueRow(
            label = if (arabic) "معامل الحرارة" else "Temperature Factor",
            value = formatNumber(study.temperatureFactor)
        )
        EngineeringValueRow(
            label = if (arabic) "معامل التجميع" else "Grouping Factor",
            value = formatNumber(study.groupingFactor)
        )
        EngineeringValueRow(
            label = if (arabic) "جهد الاستقبال" else "Receiving Voltage",
            value = "${formatNumber(study.receivingVoltageV)} V"
        )
        EngineeringValueRow(
            label = if (arabic) "الحد المسموح لهبوط الجهد" else "Allowed Voltage Drop",
            value = "${formatNumber(study.allowedVoltageDropPercent)}%"
        )

        EngineeringStatus(
            text = if (study.voltageDropPass) {
                if (arabic) "اختبار هبوط الجهد: مطابق للحد المدخل." else
                    "Voltage-drop check: within the entered limit."
            } else {
                if (arabic) "اختبار هبوط الجهد: غير مطابق للحد المدخل." else
                    "Voltage-drop check: exceeds the entered limit."
            },
            success = study.voltageDropPass
        )

        EngineeringStatus(
            text = if (study.ampacityPass) {
                if (arabic) "اختبار سعة الكابل: مطابق للتيار المحسوب." else
                    "Cable ampacity check: satisfies the calculated current."
            } else {
                if (arabic) "اختبار سعة الكابل: غير مطابق أو بيانات السعة غير متاحة." else
                    "Cable ampacity check: fails or verified ampacity data is unavailable."
            },
            success = study.ampacityPass
        )

        study.notes.forEach { note ->
            EngineeringStatus(
                text = note,
                success = false
            )
        }

        Text(
            text = if (arabic) {
                "هذه النتيجة تعتمد على بيانات السعة المتاحة في التطبيق وإعدادات الدراسة. راجع مرجع الكود وجداول الشركة المصنعة قبل اعتماد التصميم."
            } else {
                "Results depend on the ampacity data available in the app and the study inputs. Verify code references and manufacturer tables before design approval."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun currentTypeLabel(
    type: CurrentType,
    arabic: Boolean
): String = when (type) {
    CurrentType.DirectCurrent ->
        if (arabic) "تيار مستمر DC" else "Direct Current (DC)"

    CurrentType.AlternatingSinglePhase ->
        if (arabic) "متردد أحادي الطور" else "Single-Phase AC"

    CurrentType.AlternatingTwoPhase ->
        if (arabic) "متردد ثنائي الطور" else "Two-Phase AC"

    CurrentType.AlternatingThreePhase ->
        if (arabic) "متردد ثلاثي الطور" else "Three-Phase AC"
}

private fun materialLabel(
    material: ConductorMaterial,
    arabic: Boolean
): String = when (material) {
    ConductorMaterial.Copper ->
        if (arabic) "نحاس" else "Copper"

    ConductorMaterial.Aluminium ->
        if (arabic) "ألومنيوم" else "Aluminium"

    else -> material.name
}

private fun insulationLabel(
    insulation: InsulationType,
    arabic: Boolean
): String = when (insulation) {
    InsulationType.XLPE ->
        if (arabic) "XLPE" else "XLPE"

    else -> insulation.name
}

private fun formatNumber(value: Double?): String {
    if (value == null || !value.isFinite()) return "—"
    return String.format(Locale.US, "%.3f", value)
}
