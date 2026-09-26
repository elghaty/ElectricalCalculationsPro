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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import kotlin.math.abs

@Composable
fun CurrentCalculationScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic =
        language == AppLanguage.ARABIC

    var loadKw by remember {
        mutableStateOf("100")
    }

    var voltage by remember {
        mutableStateOf("400")
    }

    var powerFactor by remember {
        mutableStateOf("0.90")
    }

    var currentType by remember {
        mutableStateOf(
            CurrentType.AlternatingThreePhase
        )
    }

    var result by remember {
        mutableStateOf<Double?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(18.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector =
                        Icons.Outlined.ArrowBack,
                    contentDescription =
                        if (arabic)
                            "رجوع"
                        else
                            "Back"
                )
            }

            Text(
                text =
                    if (arabic)
                        "حساب تيار التصميم"
                    else
                        "Design Current Calculation",
                style =
                    MaterialTheme.typography.headlineSmall,
                modifier =
                    Modifier.padding(
                        top = 12.dp
                    )
            )
        }

        Text(
            text =
                if (arabic) {
                    "الحساب يتم من خلال محرك الحسابات المركزي للمشروع."
                } else {
                    "Calculation is performed through the central project engineering engine."
                }
        )

        OutlinedTextField(
            value = loadKw,
            onValueChange = {
                loadKw = sanitizeNumber(it)
                error = null
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "الحمل (kW)"
                    else
                        "Load (kW)"
                )
            },
            singleLine = true
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = {
                voltage = sanitizeNumber(it)
                error = null
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "الجهد (V)"
                    else
                        "Voltage (V)"
                )
            },
            singleLine = true
        )

        OutlinedTextField(
            value = powerFactor,
            onValueChange = {
                powerFactor = sanitizeNumber(it)
                error = null
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text(
                    if (arabic)
                        "معامل القدرة PF"
                    else
                        "Power Factor PF"
                )
            },
            singleLine = true
        )

        CurrentTypeSelector(
            value = currentType,
            arabic = arabic,
            onSelect = {
                currentType = it
                error = null
            }
        )

        Button(
            onClick = {

                val load =
                    loadKw.toDoubleOrNull()

                val v =
                    voltage.toDoubleOrNull()

                val pf =
                    powerFactor.toDoubleOrNull()

                if (
                    load == null ||
                    v == null ||
                    pf == null
                ) {
                    result = null

                    error =
                        if (arabic)
                            "أدخل قيمًا رقمية صحيحة."
                        else
                            "Enter valid numeric values."

                    return@Button
                }

                if (load < 0.0) {
                    result = null

                    error =
                        if (arabic)
                            "الحمل لا يمكن أن يكون سالبًا."
                        else
                            "Load cannot be negative."

                    return@Button
                }

                if (v <= 0.0) {
                    result = null

                    error =
                        if (arabic)
                            "الجهد يجب أن يكون أكبر من صفر."
                        else
                            "Voltage must be greater than zero."

                    return@Button
                }

                if (pf <= 0.0 || pf > 1.0) {
                    result = null

                    error =
                        if (arabic)
                            "معامل القدرة يجب أن يكون أكبر من صفر وأقصى قيمة 1."
                        else
                            "Power factor must be greater than zero and no more than 1."

                    return@Button
                }

                try {

                    result =
                        DesignProjectCoreBridge
                            .calculateDesignCurrentFromKw(
                                loadKw = load,
                                voltage = v,
                                powerFactor = pf,
                                currentType = currentType
                            )

                    error = null

                } catch (exception: Exception) {

                    result = null

                    error =
                        exception.message
                            ?: if (arabic)
                                "تعذر تنفيذ الحساب."
                            else
                                "Calculation failed."
                }
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic)
                    "احسب التيار"
                else
                    "Calculate Current"
            )
        }

        error?.let {
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    color =
                        MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.padding(16.dp)
                )
            }
        }

        result?.let { current ->

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text =
                            if (arabic)
                                "نتيجة الحساب"
                            else
                                "Calculation Result",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text =
                            formatCurrent(current),
                        style =
                            MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text =
                            if (arabic) {
                                "تيار التصميم"
                            } else {
                                "Design Current"
                            }
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = onBack,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                if (arabic)
                    "رجوع"
                else
                    "Back"
            )
        }
    }
}

@Composable
private fun CurrentTypeSelector(
    value: CurrentType,
    arabic: Boolean,
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
                currentTypeTitle(
                    value,
                    arabic
                )
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            CurrentType.values()
                .forEach { type ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                currentTypeTitle(
                                    type,
                                    arabic
                                )
                            )
                        },
                        onClick = {
                            onSelect(type)
                            expanded = false
                        }
                    )
                }
        }
    }
}

private fun currentTypeTitle(
    type: CurrentType,
    arabic: Boolean
): String =
    when (type) {

        CurrentType.DirectCurrent ->
            if (arabic)
                "تيار مستمر DC"
            else
                "Direct Current DC"

        CurrentType.AlternatingSinglePhase ->
            if (arabic)
                "تيار متردد أحادي الطور"
            else
                "AC Single Phase"

        CurrentType.AlternatingTwoPhase ->
            if (arabic)
                "تيار متردد ثنائي الطور"
            else
                "AC Two Phase"

        CurrentType.AlternatingThreePhase ->
            if (arabic)
                "تيار متردد ثلاثي الطور"
            else
                "AC Three Phase"
    }

private fun sanitizeNumber(
    value: String
): String =
    value.filter { character ->
        character.isDigit() ||
            character == '.' ||
            character == '-'
    }

private fun formatCurrent(
    value: Double
): String {

    if (!value.isFinite()) {
        return "0.00 A"
    }

    val normalized =
        if (abs(value) < 0.000001)
            0.0
        else
            value

    return "%.3f A".format(
        normalized
    )
}
