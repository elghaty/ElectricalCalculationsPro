package com.electrical.calculationspro.ui.screens

import android.app.Activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack

import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.Strings

import kotlin.math.sqrt


private const val WATTS_PER_KW = 1000.0
private const val VA_PER_KVA = 1000.0


/* -------------------------------------------------------------------------- */
/* MAIN SCREEN                                                                */
/* -------------------------------------------------------------------------- */

@Composable
fun EngineeringCalculatorScreen(
    calculation: String,
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {

    val context = LocalContext.current

    val goBack: () -> Unit = {
        if (onBack != null) {
            onBack.invoke()
        } else {
            (context as? Activity)?.finish()
        }
    }

    when (calculation) {

        "voltage_drop" ->
            VoltageDropCalculator(
                language = language,
                onBack = goBack
            )

        "current" ->
            CurrentCalculator(
                language = language,
                onBack = goBack
            )

        "voltage" ->
            VoltageCalculator(
                language = language,
                onBack = goBack
            )

        "active_power" ->
            ActivePowerCalculator(
                language = language,
                onBack = goBack
            )

        "apparent_power" ->
            ApparentPowerCalculator(
                language = language,
                onBack = goBack
            )

        "reactive_power" ->
            ReactivePowerCalculator(
                language = language,
                onBack = goBack
            )

        "power_factor" ->
            PowerFactorCalculator(
                language = language,
                onBack = goBack
            )

        "resistance" ->
            ResistanceCalculator(
                language = language,
                onBack = goBack
            )

        "impedance" ->
            ImpedanceCalculator(
                language = language,
                onBack = goBack
            )
    }
}


/* -------------------------------------------------------------------------- */
/* CURRENT                                                                    */
/* -------------------------------------------------------------------------- */

@Composable
private fun CurrentCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var loadKw by remember { mutableStateOf("5") }
    var voltage by remember { mutableStateOf("230") }
    var pf by remember { mutableStateOf("0.90") }

    var type by remember {
        mutableStateOf(
            CurrentType.AlternatingSinglePhase
        )
    }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    CalculatorLayout(
        title = Strings.get("current", language),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label =
                        if (language == AppLanguage.ARABIC)
                            "الحمل (kW)"
                        else
                            "Load (kW)",
                    value = loadKw,
                    onValueChange = {
                        loadKw = it
                    }
                )
            },

            second = {
                NumberField(
                    label =
                        Strings.get(
                            "voltage",
                            language
                        ) + " V",
                    value = voltage,
                    onValueChange = {
                        voltage = it
                    }
                )
            },

            third = {
                NumberField(
                    label =
                        Strings.get(
                            "power_factor_label",
                            language
                        ),
                    value = pf,
                    onValueChange = {
                        pf = it
                    }
                )
            },

            fourth = {
                CurrentTypeDropdown(
                    value = type,
                    language = language,
                    onSelect = {
                        type = it
                    }
                )
            }
        )

        CalculateButton(language) {

            result = runCatching {

                val powerWatts =
                    loadKw.toDouble() *
                        WATTS_PER_KW

                ElectricalCalculations
                    .calculateDesignCurrent(
                        loadWatts = powerWatts,
                        voltage = voltage.toDouble(),
                        powerFactor = pf.toDouble(),
                        currentType = type
                    )

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.3f A".format(it)
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* VOLTAGE                                                                     */
/* -------------------------------------------------------------------------- */

@Composable
private fun VoltageCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var loadKw by remember { mutableStateOf("5") }
    var current by remember { mutableStateOf("25") }
    var pf by remember { mutableStateOf("0.90") }

    var type by remember {
        mutableStateOf(
            CurrentType.AlternatingSinglePhase
        )
    }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    CalculatorLayout(
        title = Strings.get("voltage", language),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label =
                        if (language == AppLanguage.ARABIC)
                            "الحمل (kW)"
                        else
                            "Load (kW)",
                    value = loadKw,
                    onValueChange = {
                        loadKw = it
                    }
                )
            },

            second = {
                NumberField(
                    label = "Current / التيار (A)",
                    value = current,
                    onValueChange = {
                        current = it
                    }
                )
            },

            third = {
                NumberField(
                    label =
                        Strings.get(
                            "power_factor_label",
                            language
                        ),
                    value = pf,
                    onValueChange = {
                        pf = it
                    }
                )
            },

            fourth = {
                CurrentTypeDropdown(
                    value = type,
                    language = language,
                    onSelect = {
                        type = it
                    }
                )
            }
        )

        CalculateButton(language) {

            result = runCatching {

                val powerWatts =
                    loadKw.toDouble() *
                        WATTS_PER_KW

                val i =
                    current.toDouble()

                val factor =
                    pf.toDouble()

                require(powerWatts >= 0.0)
                require(i > 0.0)

                require(
                    factor > 0.0 &&
                        factor <= 1.0
                )

                when (type) {

                    CurrentType.DirectCurrent ->
                        powerWatts / i

                    CurrentType.AlternatingSinglePhase ->
                        powerWatts /
                            (i * factor)

                    CurrentType.AlternatingTwoPhase ->
                        powerWatts /
                            (2.0 * i * factor)

                    CurrentType.AlternatingThreePhase ->
                        powerWatts /
                            (
                                sqrt(3.0) *
                                    i *
                                    factor
                            )
                }

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.3f V".format(it)
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* ACTIVE POWER                                                               */
/* -------------------------------------------------------------------------- */

@Composable
private fun ActivePowerCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var voltage by remember { mutableStateOf("230") }
    var current by remember { mutableStateOf("20") }
    var pf by remember { mutableStateOf("0.90") }
    var phases by remember { mutableStateOf(1) }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    CalculatorLayout(
        title = Strings.get(
            "active_power",
            language
        ),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label = "Voltage / الجهد (V)",
                    value = voltage,
                    onValueChange = {
                        voltage = it
                    }
                )
            },

            second = {
                NumberField(
                    label = "Current / التيار (A)",
                    value = current,
                    onValueChange = {
                        current = it
                    }
                )
            },

            third = {
                NumberField(
                    label =
                        Strings.get(
                            "power_factor_label",
                            language
                        ),
                    value = pf,
                    onValueChange = {
                        pf = it
                    }
                )
            },

            fourth = {
                PhaseDropdown(
                    phases = phases,
                    language = language,
                    onSelect = {
                        phases = it
                    }
                )
            }
        )

        CalculateButton(language) {

            result = runCatching {

                ElectricalCalculations
                    .calculateActivePower(
                        voltage = voltage.toDouble(),
                        current = current.toDouble(),
                        pf = pf.toDouble(),
                        phases = phases
                    )

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.3f kW".format(
                        it / WATTS_PER_KW
                    )
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* APPARENT POWER                                                             */
/* -------------------------------------------------------------------------- */

@Composable
private fun ApparentPowerCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var voltage by remember { mutableStateOf("230") }
    var current by remember { mutableStateOf("20") }
    var phases by remember { mutableStateOf(1) }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    CalculatorLayout(
        title = Strings.get(
            "apparent_power",
            language
        ),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label = "Voltage / الجهد (V)",
                    value = voltage,
                    onValueChange = {
                        voltage = it
                    }
                )
            },

            second = {
                NumberField(
                    label = "Current / التيار (A)",
                    value = current,
                    onValueChange = {
                        current = it
                    }
                )
            },

            third = {
                PhaseDropdown(
                    phases = phases,
                    language = language,
                    onSelect = {
                        phases = it
                    }
                )
            },

            fourth = {}
        )

        CalculateButton(language) {

            result = runCatching {

                ElectricalCalculations
                    .calculateApparentPower(
                        voltage = voltage.toDouble(),
                        current = current.toDouble(),
                        phases = phases
                    )

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.3f kVA".format(
                        it / VA_PER_KVA
                    )
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* REACTIVE POWER                                                             */
/* -------------------------------------------------------------------------- */

@Composable
private fun ReactivePowerCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var activeKw by remember { mutableStateOf("5") }
    var apparentKva by remember { mutableStateOf("6") }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    CalculatorLayout(
        title = Strings.get(
            "reactive_power",
            language
        ),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label =
                        if (language == AppLanguage.ARABIC)
                            "القدرة الفعالة (kW)"
                        else
                            "Active Power (kW)",
                    value = activeKw,
                    onValueChange = {
                        activeKw = it
                    }
                )
            },

            second = {
                NumberField(
                    label =
                        if (language == AppLanguage.ARABIC)
                            "القدرة الظاهرية (kVA)"
                        else
                            "Apparent Power (kVA)",
                    value = apparentKva,
                    onValueChange = {
                        apparentKva = it
                    }
                )
            },

            third = {},
            fourth = {}
        )

        CalculateButton(language) {

            result = runCatching {

                ElectricalCalculations
                    .calculateReactivePower(
                        active =
                            activeKw.toDouble() *
                                WATTS_PER_KW,

                        apparent =
                            apparentKva.toDouble() *
                                VA_PER_KVA
                    )

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.3f kvar".format(
                        it / VA_PER_KVA
                    )
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* POWER FACTOR                                                               */
/* -------------------------------------------------------------------------- */

@Composable
private fun PowerFactorCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var activeKw by remember { mutableStateOf("5") }
    var apparentKva by remember { mutableStateOf("6") }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    CalculatorLayout(
        title = Strings.get(
            "power_factor",
            language
        ),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label =
                        if (language == AppLanguage.ARABIC)
                            "القدرة الفعالة (kW)"
                        else
                            "Active Power (kW)",
                    value = activeKw,
                    onValueChange = {
                        activeKw = it
                    }
                )
            },

            second = {
                NumberField(
                    label =
                        if (language == AppLanguage.ARABIC)
                            "القدرة الظاهرية (kVA)"
                        else
                            "Apparent Power (kVA)",
                    value = apparentKva,
                    onValueChange = {
                        apparentKva = it
                    }
                )
            },

            third = {},
            fourth = {}
        )

        CalculateButton(language) {

            result = runCatching {

                ElectricalCalculations
                    .calculatePowerFactor(
                        active =
                            activeKw.toDouble() *
                                WATTS_PER_KW,

                        apparent =
                            apparentKva.toDouble() *
                                VA_PER_KVA
                    )

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.4f".format(it)
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* RESISTANCE                                                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun ResistanceCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var voltage by remember { mutableStateOf("230") }
    var current by remember { mutableStateOf("10") }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    CalculatorLayout(
        title = Strings.get(
            "resistance",
            language
        ),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label = "Voltage / الجهد (V)",
                    value = voltage,
                    onValueChange = {
                        voltage = it
                    }
                )
            },

            second = {
                NumberField(
                    label = "Current / التيار (A)",
                    value = current,
                    onValueChange = {
                        current = it
                    }
                )
            },

            third = {},
            fourth = {}
        )

        CalculateButton(language) {

            result = runCatching {

                val v =
                    voltage.toDouble()

                val i =
                    current.toDouble()

                require(i > 0.0)

                v / i

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.6f Ω".format(it)
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* IMPEDANCE                                                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun ImpedanceCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var resistance by remember { mutableStateOf("5") }
    var reactance by remember { mutableStateOf("3") }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    CalculatorLayout(
        title = Strings.get(
            "impedance",
            language
        ),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label =
                        "Resistance / المقاومة (Ω)",
                    value = resistance,
                    onValueChange = {
                        resistance = it
                    }
                )
            },

            second = {
                NumberField(
                    label =
                        "Reactance / المفاعلة (Ω)",
                    value = reactance,
                    onValueChange = {
                        reactance = it
                    }
                )
            },

            third = {},
            fourth = {}
        )

        CalculateButton(language) {

            result = runCatching {

                val r =
                    resistance.toDouble()

                val x =
                    reactance.toDouble()

                require(r >= 0.0)
                require(x >= 0.0)

                sqrt(
                    r * r +
                        x * x
                )

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.6f Ω".format(it)
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* VOLTAGE DROP BASIC                                                         */
/* -------------------------------------------------------------------------- */

@Composable
private fun VoltageDropCalculator(
    language: AppLanguage,
    onBack: () -> Unit
) {

    var current by remember { mutableStateOf("20") }
    var length by remember { mutableStateOf("60") }
    var section by remember { mutableStateOf("6") }
    var voltage by remember { mutableStateOf("230") }
    var pf by remember { mutableStateOf("0.90") }

    var currentType by remember {
        mutableStateOf(
            CurrentType.AlternatingSinglePhase
        )
    }

    var material by remember {
        mutableStateOf(
            ConductorMaterial.Copper
        )
    }

    var result by remember {
        mutableStateOf<Pair<Double, Double>?>(null)
    }

    CalculatorLayout(
        title = Strings.get(
            "voltage_drop",
            language
        ),
        onBack = onBack
    ) {

        FourColumnRow(
            first = {
                NumberField(
                    label = "Current / التيار (A)",
                    value = current,
                    onValueChange = {
                        current = it
                    }
                )
            },

            second = {
                NumberField(
                    label =
                        Strings.get(
                            "line_length",
                            language
                        ) + " m",
                    value = length,
                    onValueChange = {
                        length = it
                    }
                )
            },

            third = {
                NumberField(
                    label =
                        "Section / المقطع (mm²)",
                    value = section,
                    onValueChange = {
                        section = it
                    }
                )
            },

            fourth = {
                NumberField(
                    label =
                        "Voltage / الجهد (V)",
                    value = voltage,
                    onValueChange = {
                        voltage = it
                    }
                )
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        FourColumnRow(
            first = {
                NumberField(
                    label =
                        Strings.get(
                            "power_factor_label",
                            language
                        ),
                    value = pf,
                    onValueChange = {
                        pf = it
                    }
                )
            },

            second = {
                CurrentTypeDropdown(
                    value = currentType,
                    language = language,
                    onSelect = {
                        currentType = it
                    }
                )
            },

            third = {
                MaterialDropdown(
                    value = material,
                    language = language,
                    onSelect = {
                        material = it
                    }
                )
            },

            fourth = {}
        )

        CalculateButton(language) {

            result = runCatching {

                ElectricalCalculations
                    .calculateVoltageDrop(
                        current =
                            current.toDouble(),

                        length =
                            length.toDouble(),

                        sectionMm2 =
                            section.toDouble(),

                        powerFactor =
                            pf.toDouble(),

                        currentType =
                            currentType,

                        material =
                            material,

                        voltage =
                            voltage.toDouble()
                    )

            }.getOrNull()
        }

        result?.let {

            ResultCard(
                title =
                    Strings.get(
                        "results",
                        language
                    ),
                value =
                    "%.3f V\n%.3f %%".format(
                        it.second,
                        it.first
                    )
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* CALCULATOR LAYOUT                                                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun CalculatorLayout(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
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

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.ArrowBack,

                    contentDescription =
                        "Back",

                    tint =
                        MaterialTheme.colorScheme.primary
                )
            }

            TextButton(
                onClick = onBack
            ) {

                Text(
                    text = "Back",
                    color =
                        MaterialTheme.colorScheme.primary
                )
            }

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )
        }

        Text(
            text = title,

            style =
                MaterialTheme
                    .typography
                    .headlineSmall,

            fontWeight =
                FontWeight.Bold
        )

        content()
    }
}


/* -------------------------------------------------------------------------- */
/* FOUR COLUMN ROW                                                            */
/* -------------------------------------------------------------------------- */

@Composable
private fun FourColumnRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
    fourth: @Composable () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(10.dp),

        verticalAlignment =
            Alignment.Top
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            first()
        }

        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            second()
        }

        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            third()
        }

        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            fourth()
        }
    }
}


/* -------------------------------------------------------------------------- */
/* NUMBER FIELD                                                               */
/* -------------------------------------------------------------------------- */

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

        modifier =
            Modifier.fillMaxWidth(),

        singleLine = true
    )
}


/* -------------------------------------------------------------------------- */
/* CALCULATE BUTTON                                                           */
/* -------------------------------------------------------------------------- */

@Composable
private fun CalculateButton(
    language: AppLanguage,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            Strings.get(
                "calculate",
                language
            )
        )
    }
}


/* -------------------------------------------------------------------------- */
/* RESULT CARD                                                                */
/* -------------------------------------------------------------------------- */

@Composable
private fun ResultCard(
    title: String,
    value: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = title,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                text = value,

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* CURRENT TYPE DROPDOWN                                                      */
/* -------------------------------------------------------------------------- */

@Composable
private fun CurrentTypeDropdown(
    value: CurrentType,
    language: AppLanguage,
    onSelect: (CurrentType) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column {

        Button(
            onClick = {
                expanded = true
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                when (value) {

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
        }

        DropdownMenu(
            expanded = expanded,

            onDismissRequest = {
                expanded = false
            }
        ) {

            CurrentType.values()
                .forEach { item ->

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


/* -------------------------------------------------------------------------- */
/* PHASE DROPDOWN                                                             */
/* -------------------------------------------------------------------------- */

@Composable
private fun PhaseDropdown(
    phases: Int,
    language: AppLanguage,
    onSelect: (Int) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column {

        Button(
            onClick = {
                expanded = true
            },

            modifier =
                Modifier.fillMaxWidth()
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
                    Text(
                        "1 Phase / أحادي الطور"
                    )
                },

                onClick = {
                    onSelect(1)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "3 Phase / ثلاثي الطور"
                    )
                },

                onClick = {
                    onSelect(3)
                    expanded = false
                }
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* MATERIAL DROPDOWN                                                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun MaterialDropdown(
    value: ConductorMaterial,
    language: AppLanguage,
    onSelect: (ConductorMaterial) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column {

        Button(
            onClick = {
                expanded = true
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                when (value) {

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
        }

        DropdownMenu(
            expanded = expanded,

            onDismissRequest = {
                expanded = false
            }
        ) {

            ConductorMaterial.values()
                .forEach { material ->

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
