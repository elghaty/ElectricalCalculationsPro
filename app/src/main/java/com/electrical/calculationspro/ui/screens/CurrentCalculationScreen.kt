
package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
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
import com.electrical.calculationspro.ui.components.EngineeringInput
import com.electrical.calculationspro.ui.components.EngineeringPage
import com.electrical.calculationspro.ui.components.EngineeringPrimaryButton
import com.electrical.calculationspro.ui.components.EngineeringResult
import com.electrical.calculationspro.ui.components.EngineeringSection
import com.electrical.calculationspro.ui.components.EngineeringStatus
import com.electrical.calculationspro.ui.components.FourColumnFields
import java.util.Locale
import kotlin.math.abs

@Composable
fun CurrentCalculationScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var loadKw by remember { mutableStateOf("100") }
    var voltage by remember { mutableStateOf("400") }
    var powerFactor by remember { mutableStateOf("0.90") }

    var currentType by remember {
        mutableStateOf(CurrentType.AlternatingThreePhase)
    }

    var result by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun clearResult() {
        result = null
        error = null
    }

    EngineeringPage(
        title = if (arabic) "حساب تيار التصميم" else "Design Current",
        subtitle = if (arabic) {
            "حساب التيار من الحمل والجهد ومعامل القدرة"
        } else {
            "Calculate current from load, voltage and power factor"
        },
        onBack = onBack
    ) {
        EngineeringSection(
            title = if (arabic) "بيانات التصميم" else "Design Inputs"
        ) {
            FourColumnFields {
                item {
                    EngineeringInput(
                        value = loadKw,
                        label = if (arabic) {
                            "الحمل الفعال (kW)"
                        } else {
                            "Active Load (kW)"
                        },
                        onValueChange = {
                            loadKw = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = voltage,
                        label = if (arabic) {
                            "الجهد (V)"
                        } else {
                            "Voltage (V)"
                        },
                        onValueChange = {
                            voltage = it
                            clearResult()
                        }
                    )
                }

                item {
                    EngineeringInput(
                        value = powerFactor,
                        label = if (arabic) {
                            "معامل القدرة (PF)"
                        } else {
                            "Power Factor (PF)"
                        },
                        onValueChange = {
                            powerFactor = it
                            clearResult()
                        }
                    )
                }

                item {
                    CurrentTypeSelector(
                        value = currentType,
                        arabic = arabic,
                        onSelect = {
                            currentType = it
                            clearResult()
                        }
                    )
                }
            }
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "احسب تيار التصميم"
            } else {
                "Calculate Design Current"
            },
            onClick = {
                val load = loadKw.toDoubleOrNull()
                val v = voltage.toDoubleOrNull()
                val pf = powerFactor.toDoubleOrNull()

                val validationError = when {
                    load == null || v == null || pf == null -> {
                        if (arabic) {
                            "أدخل قيمًا رقمية صحيحة."
                        } else {
                            "Enter valid numeric values."
                        }
                    }

                    !load.isFinite() ||
                        !v.isFinite() ||
                        !pf.isFinite() -> {
                        if (arabic) {
                            "القيم المدخلة غير صالحة."
                        } else {
                            "The entered values are invalid."
                        }
                    }

                    load < 0.0 -> {
                        if (arabic) {
                            "الحمل لا يمكن أن يكون سالبًا."
                        } else {
                            "Load cannot be negative."
                        }
                    }

                    v <= 0.0 -> {
                        if (arabic) {
                            "الجهد يجب أن يكون أكبر من صفر."
                        } else {
                            "Voltage must be greater than zero."
                        }
                    }

                    pf <= 0.0 || pf > 1.0 -> {
                        if (arabic) {
                            "معامل القدرة يجب أن يكون أكبر من صفر ولا يتجاوز 1."
                        } else {
                            "Power factor must be greater than zero and no more than 1."
                        }
                    }

                    else -> null
                }

                if (validationError != null) {
                    result = null
                    error = validationError
                } else {
                    try {
                        val calculatedCurrent =
                            DesignProjectCoreBridge.calculateDesignCurrentFromKw(
                                loadKw = load!!,
                                voltage = v!!,
                                powerFactor = pf!!,
                                currentType = currentType
                            )

                        if (
                            !calculatedCurrent.isFinite() ||
                            calculatedCurrent < 0.0
                        ) {
                            result = null
                            error = if (arabic) {
                                "أعاد محرك الحساب قيمة غير صالحة."
                            } else {
                                "The calculation engine returned an invalid value."
                            }
                        } else {
                            result = calculatedCurrent
                            error = null
                        }
                    } catch (exception: Exception) {
                        result = null
                        error = exception.message
                            ?: if (arabic) {
                                "تعذر تنفيذ الحساب."
                            } else {
                                "Calculation failed."
                            }
                    }
                }
            }
        )

        error?.let { message ->
            EngineeringStatus(
                text = message,
                success = false
            )
        }

        result?.let { current ->
            EngineeringSection(
                title = if (arabic) {
                    "نتيجة الحساب"
                } else {
                    "Calculation Result"
                }
            ) {
                EngineeringResult(
                    title = if (arabic) {
                        "تيار التصميم"
                    } else {
                        "Design Current"
                    },
                    value = formatCurrent(current)
                )

                Text(
                    text = if (arabic) {
                        "نوع النظام: ${currentTypeTitle(currentType, true)}"
                    } else {
                        "System type: ${currentTypeTitle(currentType, false)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (arabic) {
                        "الحمل: $loadKw kW | الجهد: $voltage V | معامل القدرة: $powerFactor"
                    } else {
                        "Load: $loadKw kW | Voltage: $voltage V | Power factor: $powerFactor"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CurrentTypeSelector(
    value: CurrentType,
    arabic: Boolean,
    onSelect: (CurrentType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (arabic) "نوع التيار" else "Current Type",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(currentTypeTitle(value, arabic))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CurrentType.values().forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(currentTypeTitle(type, arabic))
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
): String {
    return when (type) {
        CurrentType.DirectCurrent ->
            if (arabic) "تيار مستمر DC" else "Direct Current (DC)"

        CurrentType.AlternatingSinglePhase ->
            if (arabic) "تيار متردد أحادي الطور" else "Single-Phase AC"

        CurrentType.AlternatingTwoPhase ->
            if (arabic) "تيار متردد ثنائي الطور" else "Two-Phase AC"

        CurrentType.AlternatingThreePhase ->
            if (arabic) "تيار متردد ثلاثي الطور" else "Three-Phase AC"
    }
}

private fun formatCurrent(value: Double): String {
    if (!value.isFinite()) return "—"

    val normalized =
        if (abs(value) < 0.000001) 0.0 else value

    return String.format(Locale.US, "%.3f A", normalized)
}
