package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.Standard
import kotlin.math.max

enum class EngineeringModule {
    LOAD,
    BREAKER,
    TRANSFORMER,
    GENERATOR,
    PANEL,
    SHORT_CIRCUIT,
    PROTECTION,
    REPORT
}

@Composable
fun EngineeringModuleScreen(
    module: EngineeringModule,
    language: AppLanguage,
    standard: Standard = Standard.IEC,
    onBack: () -> Unit,
    onOpenSld: (() -> Unit)? = null
) {
    when (module) {
        EngineeringModule.LOAD ->
            LoadEngineeringScreen(
                language = language,
                onBack = onBack
            )

        EngineeringModule.BREAKER ->
            BreakerEngineeringScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )

        EngineeringModule.TRANSFORMER ->
            TransformerEngineeringScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )

        EngineeringModule.GENERATOR ->
            GeneratorEngineeringScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )

        EngineeringModule.PANEL ->
            PanelEngineeringScreen(
                language = language,
                standard = standard,
                onBack = onBack,
                onOpenSld = onOpenSld
            )

        EngineeringModule.SHORT_CIRCUIT ->
            ShortCircuitEngineeringScreen(
                language = language,
                onBack = onBack
            )

        EngineeringModule.PROTECTION ->
            ProtectionEngineeringScreen(
                language = language,
                onBack = onBack
            )

        EngineeringModule.REPORT ->
            EngineeringReportModuleScreen(
                language = language,
                onBack = onBack
            )
    }
}

@Composable
private fun ModuleLayout(
    title: String,
    subtitle: String,
    icon: ImageVector,
    language: AppLanguage,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text(
                    text = if (arabic) "‹" else "‹",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        content()

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic) "رجوع" else "Back"
            )
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
        onValueChange = {
            onValueChange(
                it.filter { c ->
                    c.isDigit() || c == '.' || c == '-'
                }
            )
        },
        label = {
            Text(label)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun ResultCard(
    title: String,
    value: String,
    success: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                if (success)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun LoadEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var loadKw by remember { mutableStateOf("100") }
    var voltage by remember { mutableStateOf("400") }
    var pf by remember { mutableStateOf("0.90") }

    var currentType by remember {
        mutableStateOf(
            CurrentType.AlternatingThreePhase
        )
    }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    ModuleLayout(
        title = if (arabic) "حساب الأحمال" else "Load Calculation",
        subtitle = if (arabic)
            "حساب تيار التصميم للحمل الكهربائي"
        else
            "Design current calculation",
        icon = Icons.Outlined.Speed,
        language = language,
        onBack = onBack
    ) {
        NumberField(
            label = if (arabic) "الحمل (kW)" else "Load (kW)",
            value = loadKw,
            onValueChange = { loadKw = it }
        )

        NumberField(
            label = if (arabic) "الجهد (V)" else "Voltage (V)",
            value = voltage,
            onValueChange = { voltage = it }
        )

        NumberField(
            label = "Power Factor",
            value = pf,
            onValueChange = { pf = it }
        )

        CurrentTypeSelector(
            language = language,
            value = currentType,
            onChange = { currentType = it }
        )

        Button(
            onClick = {
                result = runCatching {
                    ElectricalCalculations.calculateDesignCurrentFromKw(
                        loadKw = loadKw.toDouble(),
                        voltage = voltage.toDouble(),
                        powerFactor = pf.toDouble(),
                        currentType = currentType
                    )
                }.getOrNull()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic) "احسب تيار التصميم"
                else "Calculate Design Current"
            )
        }

        result?.let {
            ResultCard(
                title = if (arabic)
                    "تيار التصميم"
                else
                    "Design Current",
                value = "%.2f A".format(it)
            )
        }
    }
}

@Composable
private fun BreakerEngineeringScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var designCurrent by remember { mutableStateOf("100") }
    var cableAmpacity by remember { mutableStateOf("125") }
    var shortCircuit by remember { mutableStateOf("10") }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    ModuleLayout(
        title = if (arabic)
            "اختيار القاطع"
        else
            "Breaker Selection",
        subtitle = if (arabic)
            "اختيار القاطع والتحقق من التنسيق وقدرة القطع"
        else
            "Breaker rating and breaking-capacity verification",
        icon = Icons.Outlined.ElectricalServices,
        language = language,
        onBack = onBack
    ) {
        NumberField(
            label = "Design Current (A)",
            value = designCurrent,
            onValueChange = { designCurrent = it }
        )

        NumberField(
            label = "Cable Ampacity (A)",
            value = cableAmpacity,
            onValueChange = { cableAmpacity = it }
        )

        NumberField(
            label = "Prospective Fault (kA)",
            value = shortCircuit,
            onValueChange = { shortCircuit = it }
        )

        Button(
            onClick = {
                result = runCatching {
                    val current = designCurrent.toDouble()
                    val ampacity = cableAmpacity.toDouble()
                    val fault = shortCircuit.toDouble()

                    val rating =
                        ElectricalCalculations.selectBreakerRating(
                            designCurrentA = current,
                            cableAmpacityA = ampacity,
                            standard = standard
                        )

                    val coordination =
                        ElectricalCalculations.checkBreakerCoordination(
                            designCurrentA = current,
                            breakerRatingA = rating,
                            cableAmpacityA = ampacity
                        )

                    "Breaker = %.0f A\nCoordination = %s"
                        .format(
                            rating,
                            if (coordination) "PASS" else "CHECK"
                        ) +
                        "\nBreaking capacity input = %.2f kA"
                            .format(fault)
                }.getOrNull()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic) "اختيار القاطع"
                else "Select Breaker"
            )
        }

        result?.let {
            ResultCard(
                title = if (arabic) "النتيجة" else "Result",
                value = it
            )
        }
    }
}

@Composable
private fun TransformerEngineeringScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var loadKw by remember { mutableStateOf("500") }
    var pf by remember { mutableStateOf("0.90") }
    var growth by remember { mutableStateOf("1.15") }
    var voltage by remember { mutableStateOf("400") }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    ModuleLayout(
        title = if (arabic)
            "اختيار المحول"
        else
            "Transformer Sizing",
        subtitle = if (arabic)
            "حساب القدرة المطلوبة واختيار أقرب مقاس قياسي"
        else
            "Required transformer capacity and standard rating",
        icon = Icons.Outlined.Memory,
        language = language,
        onBack = onBack
    ) {
        NumberField(
            label = "Load (kW)",
            value = loadKw,
            onValueChange = { loadKw = it }
        )

        NumberField(
            label = "Power Factor",
            value = pf,
            onValueChange = { pf = it }
        )

        NumberField(
            label = "Growth Factor",
            value = growth,
            onValueChange = { growth = it }
        )

        NumberField(
            label = "LV Voltage (V)",
            value = voltage,
            onValueChange = { voltage = it }
        )

        Button(
            onClick = {
                result = runCatching {
                    val required =
                        ElectricalCalculations.calculateRequiredTransformerKva(
                            loadKw = loadKw.toDouble(),
                            powerFactor = pf.toDouble(),
                            growthFactor = growth.toDouble()
                        )

                    val selected =
                        ElectricalCalculations.selectTransformerRating(
                            requiredKva = required
                        )

                    "Required = %.1f kVA\nStandard Selection = %.0f kVA"
                        .format(required, selected)
                }.getOrNull()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic) "احسب المحول"
                else "Calculate Transformer"
            )
        }

        result?.let {
            ResultCard(
                title = if (arabic) "نتيجة المحول" else "Transformer Result",
                value = it
            )
        }
    }
}

@Composable
private fun GeneratorEngineeringScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var requiredKva by remember { mutableStateOf("500") }
    var result by remember {
        mutableStateOf<String?>(null)
    }

    ModuleLayout(
        title = if (arabic)
            "اختيار المولد"
        else
            "Generator Selection",
        subtitle = if (arabic)
            "اختيار مولد من كتالوج المعدات"
        else
            "Generator selection from the equipment catalog",
        icon = Icons.Outlined.Factory,
        language = language,
        onBack = onBack
    ) {
        NumberField(
            label = "Required Generator (kVA)",
            value = requiredKva,
            onValueChange = { requiredKva = it }
        )

        Button(
            onClick = {
                result = runCatching {
                    val selection =
                        ElectricalCalculations.selectGeneratorFromCatalog(
                            requiredKva = requiredKva.toDouble(),
                            standard = standard
                        )

                    selection.toString()
                }.getOrNull()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic) "اختيار المولد"
                else "Select Generator"
            )
        }

        result?.let {
            ResultCard(
                title = if (arabic) "نتيجة الاختيار" else "Selection Result",
                value = it
            )
        }
    }
}

@Composable
private fun PanelEngineeringScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: () -> Unit,
    onOpenSld: (() -> Unit)?
) {
    val arabic = language == AppLanguage.ARABIC

    var current by remember { mutableStateOf("400") }
    var result by remember {
        mutableStateOf<String?>(null)
    }

    ModuleLayout(
        title = if (arabic)
            "تصميم اللوحة"
        else
            "Panel Design",
        subtitle = if (arabic)
            "اختيار اللوحة طبقًا للتيار التصميمي"
        else
            "Panel selection based on design current",
        icon = Icons.Outlined.Power,
        language = language,
        onBack = onBack
    ) {
        NumberField(
            label = "Design Current (A)",
            value = current,
            onValueChange = { current = it }
        )

        Button(
            onClick = {
                result = runCatching {
                    ElectricalCalculations
                        .selectPanelFromCatalog(
                            currentA = current.toDouble(),
                            standard = standard
                        )
                        .toString()
                }.getOrNull()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic) "اختيار اللوحة"
                else "Select Panel"
            )
        }

        result?.let {
            ResultCard(
                title = if (arabic) "نتيجة اللوحة" else "Panel Result",
                value = it
            )
        }

        onOpenSld?.let { openSld ->
            Button(
                onClick = openSld,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Text(
                    if (arabic)
                        "فتح المخطط الأحادي SLD"
                    else
                        "Open Single Line Diagram"
                )
            }
        }
    }
}

@Composable
private fun ShortCircuitEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var voltage by remember { mutableStateOf("400") }
    var length by remember { mutableStateOf("30") }
    var section by remember { mutableStateOf("70") }
    var sourceIk by remember { mutableStateOf("25") }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    var material by remember {
        mutableStateOf(
            ConductorMaterial.Copper
        )
    }

    ModuleLayout(
        title = if (arabic)
            "تيار القصر"
        else
            "Short Circuit",
        subtitle = if (arabic)
            "حساب تيار القصر عند نقطة الدائرة"
        else
            "Prospective short-circuit current calculation",
        icon = Icons.Outlined.Bolt,
        language = language,
        onBack = onBack
    ) {
        NumberField(
            label = "Voltage (V)",
            value = voltage,
            onValueChange = { voltage = it }
        )

        NumberField(
            label = "Cable Length (m)",
            value = length,
            onValueChange = { length = it }
        )

        NumberField(
            label = "Cable Section (mm²)",
            value = section,
            onValueChange = { section = it }
        )

        NumberField(
            label = "Source Ik (kA)",
            value = sourceIk,
            onValueChange = { sourceIk = it }
        )

        MaterialSelector(
            language = language,
            value = material,
            onChange = { material = it }
        )

        Button(
            onClick = {
                result = runCatching {
                    val r =
                        ElectricalCalculations
                            .calculateShortCircuitCurrent(
                                voltage = voltage.toDouble(),
                                length = length.toDouble(),
                                sectionMm2 = section.toDouble(),
                                material = material,
                                currentType =
                                    CurrentType.AlternatingThreePhase,
                                sourceIkKA =
                                    sourceIk.toDouble()
                            )

                    r.toString()
                }.getOrNull()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic) "احسب تيار القصر"
                else "Calculate Short Circuit"
            )
        }

        result?.let {
            ResultCard(
                title = if (arabic) "نتيجة الدراسة" else "Study Result",
                value = it
            )
        }
    }
}

@Composable
private fun ProtectionEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var designCurrent by remember { mutableStateOf("100") }
    var breaker by remember { mutableStateOf("125") }
    var cable by remember { mutableStateOf("150") }
    var fault by remember { mutableStateOf("10") }
    var breakingCapacity by remember { mutableStateOf("25") }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    ModuleLayout(
        title = if (arabic)
            "الحماية والتنسيق"
        else
            "Protection & Coordination",
        subtitle = if (arabic)
            "فحص تنسيق القاطع وقدرة القطع"
        else
            "Breaker coordination and breaking-capacity checks",
        icon = Icons.Outlined.Security,
        language = language,
        onBack = onBack
    ) {
        NumberField(
            label = "Design Current (A)",
            value = designCurrent,
            onValueChange = { designCurrent = it }
        )

        NumberField(
            label = "Breaker Rating (A)",
            value = breaker,
            onValueChange = { breaker = it }
        )

        NumberField(
            label = "Cable Ampacity (A)",
            value = cable,
            onValueChange = { cable = it }
        )

        NumberField(
            label = "Fault Current (kA)",
            value = fault,
            onValueChange = { fault = it }
        )

        NumberField(
            label = "Breaker Breaking Capacity (kA)",
            value = breakingCapacity,
            onValueChange = { breakingCapacity = it }
        )

        Button(
            onClick = {
                result = runCatching {
                    val coordination =
                        ElectricalCalculations.checkBreakerCoordination(
                            designCurrentA =
                                designCurrent.toDouble(),
                            breakerRatingA =
                                breaker.toDouble(),
                            cableAmpacityA =
                                cable.toDouble()
                        )

                    val breaking =
                        ElectricalCalculations.checkBreakingCapacity(
                            prospectiveFaultCurrentKA =
                                fault.toDouble(),
                            breakerBreakingCapacityKA =
                                breakingCapacity.toDouble()
                        )

                    "Coordination = " +
                        if (coordination) "PASS" else "CHECK" +
                        "\nBreaking Capacity = " +
                        if (breaking) "PASS" else "CHECK"
                }.getOrNull()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic) "فحص الحماية"
                else "Validate Protection"
            )
        }

        result?.let {
            ResultCard(
                title = if (arabic) "حالة الحماية" else "Protection Status",
                value = it
            )
        }
    }
}

@Composable
private fun EngineeringReportModuleScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    ModuleLayout(
        title = if (arabic)
            "التقرير الهندسي"
        else
            "Engineering Report",
        subtitle = if (arabic)
            "مركز مخرجات التصميم والحسابات"
        else
            "Engineering calculation and design deliverables",
        icon = Icons.Outlined.Description,
        language = language,
        onBack = onBack
    ) {
        ResultCard(
            title = if (arabic)
                "حالة النظام"
            else
                "System Status",
            value = if (arabic)
                "الحسابات الأساسية مرتبطة بالـ Core.\nSLD يعمل من مساره المستقل."
            else
                "Core-connected engineering calculations.\nSLD remains on its dedicated route."
        )
    }
}

@Composable
private fun CurrentTypeSelector(
    language: AppLanguage,
    value: CurrentType,
    onChange: (CurrentType) -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    Button(
        onClick = {
            onChange(
                when (value) {
                    CurrentType.AlternatingThreePhase ->
                        CurrentType.AlternatingSinglePhase

                    CurrentType.AlternatingSinglePhase ->
                        CurrentType.DirectCurrent

                    CurrentType.DirectCurrent ->
                        CurrentType.AlternatingThreePhase

                    CurrentType.AlternatingTwoPhase ->
                        CurrentType.AlternatingThreePhase
                }
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            when (value) {
                CurrentType.DirectCurrent ->
                    if (arabic) "تيار مستمر" else "DC"

                CurrentType.AlternatingSinglePhase ->
                    if (arabic) "أحادي الطور" else "1 Phase"

                CurrentType.AlternatingTwoPhase ->
                    if (arabic) "ثنائي الطور" else "2 Phase"

                CurrentType.AlternatingThreePhase ->
                    if (arabic) "ثلاثي الطور" else "3 Phase"
            }
        )
    }
}

@Composable
private fun MaterialSelector(
    language: AppLanguage,
    value: ConductorMaterial,
    onChange: (ConductorMaterial) -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    Button(
        onClick = {
            onChange(
                if (value == ConductorMaterial.Copper)
                    ConductorMaterial.Aluminum
                else
                    ConductorMaterial.Copper
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (value == ConductorMaterial.Copper) {
                if (arabic) "موصل نحاس" else "Copper"
            } else {
                if (arabic) "موصل ألومنيوم" else "Aluminum"
            }
        )
    }
}
