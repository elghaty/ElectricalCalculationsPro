package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.Strings
import kotlin.math.sqrt

@Composable
fun EngineeringCalculatorScreen(
    calculation: String,
    language: AppLanguage
) {
    when (calculation) {
        "voltage_drop" -> VoltageDropCalculator(language)
        "current" -> CurrentCalculator(language)
        "voltage" -> VoltageCalculator(language)
        "active_power" -> ActivePowerCalculator(language)
        "apparent_power" -> ApparentPowerCalculator(language)
        "reactive_power" -> ReactivePowerCalculator(language)
        "power_factor" -> PowerFactorCalculator(language)
        "resistance" -> ResistanceCalculator(language)
        "impedance" -> ImpedanceCalculator(language)
    }
}

@Composable
private fun CurrentCalculator(language: AppLanguage) {
    var load by remember { mutableStateOf("5000") }
    var voltage by remember { mutableStateOf("230") }
    var pf by remember { mutableStateOf("0.90") }
    var type by remember {
        mutableStateOf(CurrentType.AlternatingSinglePhase)
    }
    var result by remember { mutableStateOf<Double?>(null) }

    CalculatorLayout(
        title = Strings.get("current", language)
    ) {
        NumberField(
            label = Strings.get("load", language) + " W",
            value = load,
            onValueChange = { load = it }
        )

        NumberField(
            label = Strings.get("voltage", language) + " V",
            value = voltage,
            onValueChange = { voltage = it }
        )

        NumberField(
            label = Strings.get("power_factor_label", language),
            value = pf,
            onValueChange = { pf = it }
        )

        CurrentTypeDropdown(
            value = type,
            language = language,
            onSelect = { type = it }
        )

        CalculateButton(language) {
            result = runCatching {
                ElectricalCalculations.calculateDesignCurrent(
                    loadWatts = load.toDouble(),
                    voltage = voltage.toDouble(),
                    powerFactor = pf.toDouble(),
                    currentType = type
                )
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.3f A".format(it)
            )
        }
    }
}

@Composable
private fun VoltageCalculator(language: AppLanguage) {
    var load by remember { mutableStateOf("5000") }
    var current by remember { mutableStateOf("25") }
    var pf by remember { mutableStateOf("0.90") }
    var type by remember {
        mutableStateOf(CurrentType.AlternatingSinglePhase)
    }
    var result by remember { mutableStateOf<Double?>(null) }

    CalculatorLayout(
        title = Strings.get("voltage", language)
    ) {
        NumberField(
            label = Strings.get("load", language) + " W",
            value = load,
            onValueChange = { load = it }
        )

        NumberField(
            label = "Current / التيار (A)",
            value = current,
            onValueChange = { current = it }
        )

        NumberField(
            label = Strings.get("power_factor_label", language),
            value = pf,
            onValueChange = { pf = it }
        )

        CurrentTypeDropdown(
            value = type,
            language = language,
            onSelect = { type = it }
        )

        CalculateButton(language) {
            result = runCatching {
                val p = load.toDouble()
                val i = current.toDouble()
                val factor = pf.toDouble()

                require(p >= 0.0)
                require(i > 0.0)
                require(factor > 0.0 && factor <= 1.0)

                when (type) {
                    CurrentType.DirectCurrent ->
                        p / i

                    CurrentType.AlternatingSinglePhase ->
                        p / (i * factor)

                    CurrentType.AlternatingTwoPhase ->
                        p / (2.0 * i * factor)

                    CurrentType.AlternatingThreePhase ->
                        p / (sqrt(3.0) * i * factor)
                }
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.3f V".format(it)
            )
        }
    }
}

@Composable
private fun ActivePowerCalculator(language: AppLanguage) {
    var voltage by remember { mutableStateOf("230") }
    var current by remember { mutableStateOf("20") }
    var pf by remember { mutableStateOf("0.90") }
    var phases by remember { mutableStateOf(1) }
    var result by remember { mutableStateOf<Double?>(null) }

    CalculatorLayout(
        title = Strings.get("active_power", language)
    ) {
        NumberField(
            label = "Voltage / الجهد (V)",
            value = voltage,
            onValueChange = { voltage = it }
        )

        NumberField(
            label = "Current / التيار (A)",
            value = current,
            onValueChange = { current = it }
        )

        NumberField(
            label = Strings.get("power_factor_label", language),
            value = pf,
            onValueChange = { pf = it }
        )

        PhaseDropdown(
            phases = phases,
            language = language,
            onSelect = { phases = it }
        )

        CalculateButton(language) {
            result = runCatching {
                ElectricalCalculations.calculateActivePower(
                    voltage = voltage.toDouble(),
                    current = current.toDouble(),
                    pf = pf.toDouble(),
                    phases = phases
                )
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.3f W".format(it)
            )
        }
    }
}

@Composable
private fun ApparentPowerCalculator(language: AppLanguage) {
    var voltage by remember { mutableStateOf("230") }
    var current by remember { mutableStateOf("20") }
    var phases by remember { mutableStateOf(1) }
    var result by remember { mutableStateOf<Double?>(null) }

    CalculatorLayout(
        title = Strings.get("apparent_power", language)
    ) {
        NumberField(
            label = "Voltage / الجهد (V)",
            value = voltage,
            onValueChange = { voltage = it }
        )

        NumberField(
            label = "Current / التيار (A)",
            value = current,
            onValueChange = { current = it }
        )

        PhaseDropdown(
            phases = phases,
            language = language,
            onSelect = { phases = it }
        )

        CalculateButton(language) {
            result = runCatching {
                ElectricalCalculations.calculateApparentPower(
                    voltage = voltage.toDouble(),
                    current = current.toDouble(),
                    phases = phases
                )
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.3f VA".format(it)
            )
        }
    }
}

@Composable
private fun ReactivePowerCalculator(language: AppLanguage) {
    var active by remember { mutableStateOf("5000") }
    var apparent by remember { mutableStateOf("6000") }
    var result by remember { mutableStateOf<Double?>(null) }

    CalculatorLayout(
        title = Strings.get("reactive_power", language)
    ) {
        NumberField(
            label = "Active Power / القدرة الفعالة (W)",
            value = active,
            onValueChange = { active = it }
        )

        NumberField(
            label = "Apparent Power / القدرة الظاهرية (VA)",
            value = apparent,
            onValueChange = { apparent = it }
        )

        CalculateButton(language) {
            result = runCatching {
                ElectricalCalculations.calculateReactivePower(
                    active = active.toDouble(),
                    apparent = apparent.toDouble()
                )
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.3f VAR".format(it)
            )
        }
    }
}

@Composable
private fun PowerFactorCalculator(language: AppLanguage) {
    var active by remember { mutableStateOf("5000") }
    var apparent by remember { mutableStateOf("6000") }
    var result by remember { mutableStateOf<Double?>(null) }

    CalculatorLayout(
        title = Strings.get("power_factor", language)
    ) {
        NumberField(
            label = "Active Power / القدرة الفعالة (W)",
            value = active,
            onValueChange = { active = it }
        )

        NumberField(
            label = "Apparent Power / القدرة الظاهرية (VA)",
            value = apparent,
            onValueChange = { apparent = it }
        )

        CalculateButton(language) {
            result = runCatching {
                ElectricalCalculations.calculatePowerFactor(
                    active = active.toDouble(),
                    apparent = apparent.toDouble()
                )
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.4f".format(it)
            )
        }
    }
}

@Composable
private fun ResistanceCalculator(language: AppLanguage) {
    var voltage by remember { mutableStateOf("230") }
    var current by remember { mutableStateOf("10") }
    var result by remember { mutableStateOf<Double?>(null) }

    CalculatorLayout(
        title = Strings.get("resistance", language)
    ) {
        NumberField(
            label = "Voltage / الجهد (V)",
            value = voltage,
            onValueChange = { voltage = it }
        )

        NumberField(
            label = "Current / التيار (A)",
            value = current,
            onValueChange = { current = it }
        )

        CalculateButton(language) {
            result = runCatching {
                val v = voltage.toDouble()
                val i = current.toDouble()

                require(i > 0.0)

                v / i
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.6f Ω".format(it)
            )
        }
    }
}

@Composable
private fun ImpedanceCalculator(language: AppLanguage) {
    var resistance by remember { mutableStateOf("5") }
    var reactance by remember { mutableStateOf("3") }
    var result by remember { mutableStateOf<Double?>(null) }

    CalculatorLayout(
        title = Strings.get("impedance", language)
    ) {
        NumberField(
            label = "Resistance / المقاومة (Ω)",
            value = resistance,
            onValueChange = { resistance = it }
        )

        NumberField(
            label = "Reactance / المفاعلة (Ω)",
            value = reactance,
            onValueChange = { reactance = it }
        )

        CalculateButton(language) {
            result = runCatching {
                val r = resistance.toDouble()
                val x = reactance.toDouble()

                require(r >= 0.0)
                require(x >= 0.0)

                sqrt(r * r + x * x)
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.6f Ω".format(it)
            )
        }
    }
}

@Composable
private fun VoltageDropCalculator(language: AppLanguage) {
    var current by remember { mutableStateOf("20") }
    var length by remember { mutableStateOf("60") }
    var section by remember { mutableStateOf("6") }
    var voltage by remember { mutableStateOf("230") }
    var pf by remember { mutableStateOf("0.90") }

    var currentType by remember {
        mutableStateOf(CurrentType.AlternatingSinglePhase)
    }

    var material by remember {
        mutableStateOf(ConductorMaterial.Copper)
    }

    var result by remember {
        mutableStateOf<Pair<Double, Double>?>(null)
    }

    CalculatorLayout(
        title = Strings.get("voltage_drop", language)
    ) {
        NumberField(
            label = "Current / التيار (A)",
            value = current,
            onValueChange = { current = it }
        )

        NumberField(
            label = Strings.get("line_length", language) + " m",
            value = length,
            onValueChange = { length = it }
        )

        NumberField(
            label = "Section / المقطع (mm²)",
            value = section,
            onValueChange = { section = it }
        )

        NumberField(
            label = "Voltage / الجهد (V)",
            value = voltage,
            onValueChange = { voltage = it }
        )

        NumberField(
            label = Strings.get("power_factor_label", language),
            value = pf,
            onValueChange = { pf = it }
        )

        CurrentTypeDropdown(
            value = currentType,
            language = language,
            onSelect = { currentType = it }
        )

        MaterialDropdown(
            value = material,
            language = language,
            onSelect = { material = it }
        )

        CalculateButton(language) {
            result = runCatching {
                ElectricalCalculations.calculateVoltageDrop(
                    current = current.toDouble(),
                    length = length.toDouble(),
                    sectionMm2 = section.toDouble(),
                    powerFactor = pf.toDouble(),
                    currentType = currentType,
                    material = material,
                    voltage = voltage.toDouble()
                )
            }.getOrNull()
        }

        result?.let {
            ResultCard(
                title = Strings.get("results", language),
                value = "%.3f V\n%.3f %%".format(
                    it.second,
                    it.first
                )
            )
        }
    }
}

@Composable
private fun CalculatorLayout(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )

        content()
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
                it.filter { char ->
                    char.isDigit() ||
                        char == '.' ||
                        char == '-'
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
private fun CalculateButton(
    language: AppLanguage,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            Strings.get("calculate", language)
        )
    }
}

@Composable
private fun ResultCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun CurrentTypeDropdown(
    value: CurrentType,
    language: AppLanguage,
    onSelect: (CurrentType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when (value) {
                    CurrentType.DirectCurrent ->
                        Strings.get("direct_current", language)

                    CurrentType.AlternatingSinglePhase ->
                        Strings.get("alternating_single", language)

                    CurrentType.AlternatingTwoPhase ->
                        Strings.get("alternating_two", language)

                    CurrentType.AlternatingThreePhase ->
                        Strings.get("alternating_three", language)
                }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            CurrentType.values().forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (item) {
                                CurrentType.DirectCurrent ->
                                    Strings.get(
                                        "direct_current",
                                        language
                                    )

                                CurrentType.AlternatingSinglePhase ->
                                    Strings.get(
                                        "alternating_single",
                                        language
                                    )

                                CurrentType.AlternatingTwoPhase ->
                                    Strings.get(
                                        "alternating_two",
                                        language
                                    )

                                CurrentType.AlternatingThreePhase ->
                                    Strings.get(
                                        "alternating_three",
                                        language
                                    )
                            }
                        )
                    },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PhaseDropdown(
    phases: Int,
    language: AppLanguage,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (phases == 1) {
                    "1 Phase / أحادي الطور"
                } else {
                    "3 Phase / ثلاثي الطور"
                }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text("1 Phase / أحادي الطور")
                },
                onClick = {
                    onSelect(1)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text("3 Phase / ثلاثي الطور")
                },
                onClick = {
                    onSelect(3)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun MaterialDropdown(
    value: ConductorMaterial,
    language: AppLanguage,
    onSelect: (ConductorMaterial) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when (value) {
                    ConductorMaterial.Copper ->
                        Strings.get("copper", language)

                    ConductorMaterial.Aluminum ->
                        Strings.get("aluminum", language)
                }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            ConductorMaterial.values().forEach { material ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (material) {
                                ConductorMaterial.Copper ->
                                    Strings.get(
                                        "copper",
                                        language
                                    )

                                ConductorMaterial.Aluminum ->
                                    Strings.get(
                                        "aluminum",
                                        language
                                    )
                            }
                        )
                    },
                    onClick = {
                        onSelect(material)
                        expanded = false
                    }
                )
            }
        }
    }
}
