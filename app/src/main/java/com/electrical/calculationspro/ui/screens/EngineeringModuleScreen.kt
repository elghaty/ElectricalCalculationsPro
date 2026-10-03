package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.ui.components.EngineeringCard
import com.electrical.calculationspro.ui.components.EngineeringEmptyState
import com.electrical.calculationspro.ui.components.EngineeringInput
import com.electrical.calculationspro.ui.components.EngineeringPage
import com.electrical.calculationspro.ui.components.EngineeringPrimaryButton
import com.electrical.calculationspro.ui.components.EngineeringResult
import com.electrical.calculationspro.ui.components.EngineeringSecondaryButton
import com.electrical.calculationspro.ui.components.EngineeringSectionTitle
import com.electrical.calculationspro.ui.components.EngineeringStatus
import com.electrical.calculationspro.ui.components.EngineeringValueRow

enum class EngineeringModule {
    LOAD,
    CURRENT,
    CABLE,
    VOLTAGE_DROP,
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

        EngineeringModule.CURRENT ->
            CurrentCalculationScreen(
                language = language,
                onBack = onBack
            )

        EngineeringModule.CABLE ->
            ConductorSizingScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )

        EngineeringModule.VOLTAGE_DROP ->
            ProfessionalVoltageDropScreen(
                language = language,
                standard = standard,
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
private fun LoadEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var loadKw by remember {
        mutableStateOf("100")
    }

    var voltage by remember {
        mutableStateOf("400")
    }

    var pf by remember {
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

    EngineeringPage(
        title = if (arabic) {
            "حساب الأحمال"
        } else {
            "Load Calculation"
        },
        subtitle = if (arabic) {
            "حساب تيار التصميم للحمل الكهربائي"
        } else {
            "Design current calculation"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) {
                "بيانات الحمل"
            } else {
                "Load Data"
            }
        ) {
            EngineeringInput(
                label = if (arabic) {
                    "الحمل (kW)"
                } else {
                    "Load (kW)"
                },
                value = loadKw,
                onValueChange = {
                    loadKw = it
                    error = null
                    result = null
                }
            )

            EngineeringInput(
                label = if (arabic) {
                    "الجهد (V)"
                } else {
                    "Voltage (V)"
                },
                value = voltage,
                onValueChange = {
                    voltage = it
                    error = null
                    result = null
                }
            )

            EngineeringInput(
                label = "Power Factor",
                value = pf,
                onValueChange = {
                    pf = it
                    error = null
                    result = null
                }
            )

            CurrentTypeSelector(
                language = language,
                value = currentType,
                onChange = {
                    currentType = it
                    error = null
                    result = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "احسب تيار التصميم"
            } else {
                "Calculate Design Current"
            },
            onClick = {
                try {
                    val load = loadKw.toDouble()
                    val v = voltage.toDouble()
                    val factor = pf.toDouble()

                    if (load <= 0.0 || v <= 0.0) {
                        throw IllegalArgumentException(
                            if (arabic) {
                                "الحمل والجهد يجب أن يكونا أكبر من صفر."
                            } else {
                                "Load and voltage must be greater than zero."
                            }
                        )
                    }

                    if (factor <= 0.0 || factor > 1.0) {
                        throw IllegalArgumentException(
                            if (arabic) {
                                "Power Factor يجب أن يكون بين 0 و1."
                            } else {
                                "Power Factor must be between 0 and 1."
                            }
                        )
                    }

                    result =
                        ElectricalCalculations
                            .calculateDesignCurrentFromKw(
                                loadKw = load,
                                voltage = v,
                                powerFactor = factor,
                                currentType = currentType
                            )

                    error = null
                } catch (exception: Exception) {
                    result = null
                    error =
                        exception.message
                            ?: if (arabic) {
                                "بيانات الإدخال غير صحيحة."
                            } else {
                                "Invalid input data."
                            }
                }
            }
        )

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        result?.let {
            EngineeringResult(
                title = if (arabic) {
                    "تيار التصميم"
                } else {
                    "Design Current"
                },
                value = "%.2f A".format(it)
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) {
                        "لا توجد نتيجة بعد"
                    } else {
                        "No Result Yet"
                    },
                    message = if (arabic) {
                        "أدخل بيانات الحمل ثم اضغط حساب."
                    } else {
                        "Enter the load data and calculate."
                    }
                )
            }
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

    var designCurrent by remember {
        mutableStateOf("100")
    }

    var cableAmpacity by remember {
        mutableStateOf("125")
    }

    var shortCircuit by remember {
        mutableStateOf("10")
    }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    EngineeringPage(
        title = if (arabic) {
            "اختيار القاطع"
        } else {
            "Breaker Selection"
        },
        subtitle = if (arabic) {
            "اختيار القاطع والتحقق من التنسيق وقدرة القطع"
        } else {
            "Breaker rating and breaking-capacity verification"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) {
                "بيانات القاطع"
            } else {
                "Breaker Data"
            }
        ) {
            EngineeringInput(
                label = "Design Current (A)",
                value = designCurrent,
                onValueChange = {
                    designCurrent = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Cable Ampacity (A)",
                value = cableAmpacity,
                onValueChange = {
                    cableAmpacity = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Prospective Fault (kA)",
                value = shortCircuit,
                onValueChange = {
                    shortCircuit = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "اختيار القاطع"
            } else {
                "Select Breaker"
            },
            onClick = {
                try {
                    val current = designCurrent.toDouble()
                    val ampacity = cableAmpacity.toDouble()
                    val fault = shortCircuit.toDouble()

                    if (current <= 0.0 || ampacity <= 0.0) {
                        throw IllegalArgumentException(
                            if (arabic) {
                                "القيم يجب أن تكون أكبر من صفر."
                            } else {
                                "Values must be greater than zero."
                            }
                        )
                    }

                    val rating =
                        ElectricalCalculations
                            .selectBreakerRating(
                                designCurrentA = current,
                                cableAmpacityA = ampacity,
                                standard = standard
                            )

                    val coordination =
                        ElectricalCalculations
                            .checkBreakerCoordination(
                                designCurrentA = current,
                                breakerRatingA = rating,
                                cableAmpacityA = ampacity
                            )

                    result =
                        "Breaker = %.0f A\nCoordination = %s\nBreaking capacity input = %.2f kA"
                            .format(
                                rating,
                                if (coordination) {
                                    "PASS"
                                } else {
                                    "CHECK"
                                },
                                fault
                            )

                    error = null
                } catch (exception: Exception) {
                    result = null
                    error =
                        exception.message
                            ?: if (arabic) {
                                "بيانات الإدخال غير صحيحة."
                            } else {
                                "Invalid input data."
                            }
                }
            }
        )

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        result?.let {
            EngineeringResult(
                title = if (arabic) {
                    "نتيجة اختيار القاطع"
                } else {
                    "Breaker Selection Result"
                },
                value = it
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) {
                        "لا توجد نتيجة بعد"
                    } else {
                        "No Result Yet"
                    },
                    message = if (arabic) {
                        "أدخل بيانات القاطع ثم نفذ الاختيار."
                    } else {
                        "Enter breaker data and run the selection."
                    }
                )
            }
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

    var loadKw by remember {
        mutableStateOf("500")
    }

    var pf by remember {
        mutableStateOf("0.90")
    }

    var growth by remember {
        mutableStateOf("1.15")
    }

    var voltage by remember {
        mutableStateOf("400")
    }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    EngineeringPage(
        title = if (arabic) {
            "اختيار المحول"
        } else {
            "Transformer Sizing"
        },
        subtitle = if (arabic) {
            "حساب القدرة المطلوبة واختيار أقرب مقاس قياسي"
        } else {
            "Required transformer capacity and standard rating"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) {
                "بيانات المحول"
            } else {
                "Transformer Data"
            }
        ) {
            EngineeringInput(
                label = "Load (kW)",
                value = loadKw,
                onValueChange = {
                    loadKw = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Power Factor",
                value = pf,
                onValueChange = {
                    pf = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Growth Factor",
                value = growth,
                onValueChange = {
                    growth = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "LV Voltage (V)",
                value = voltage,
                onValueChange = {
                    voltage = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "احسب المحول"
            } else {
                "Calculate Transformer"
            },
            onClick = {
                try {
                    val load = loadKw.toDouble()
                    val factor = pf.toDouble()
                    val growthFactor = growth.toDouble()
                    voltage.toDouble()

                    if (
                        load <= 0.0 ||
                        factor <= 0.0 ||
                        factor > 1.0 ||
                        growthFactor <= 0.0
                    ) {
                        throw IllegalArgumentException(
                            if (arabic) {
                                "راجع قيم الحمل وPower Factor ومعامل النمو."
                            } else {
                                "Check load, power factor and growth factor."
                            }
                        )
                    }

                    val required =
                        ElectricalCalculations
                            .calculateRequiredTransformerKva(
                                loadKw = load,
                                powerFactor = factor,
                                growthFactor = growthFactor
                            )

                    val selected =
                        ElectricalCalculations
                            .selectTransformerRating(
                                requiredKva = required
                            )

                    result =
                        "Required = %.1f kVA\nStandard Selection = %.0f kVA"
                            .format(
                                required,
                                selected
                            )

                    error = null
                } catch (exception: Exception) {
                    result = null
                    error =
                        exception.message
                            ?: if (arabic) {
                                "بيانات الإدخال غير صحيحة."
                            } else {
                                "Invalid input data."
                            }
                }
            }
        )

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        result?.let {
            EngineeringResult(
                title = if (arabic) {
                    "نتيجة المحول"
                } else {
                    "Transformer Result"
                },
                value = it
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) {
                        "لا توجد نتيجة بعد"
                    } else {
                        "No Result Yet"
                    },
                    message = if (arabic) {
                        "أدخل بيانات المحول ثم اضغط حساب."
                    } else {
                        "Enter transformer data and calculate."
                    }
                )
            }
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

    var requiredKva by remember {
        mutableStateOf("500")
    }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    EngineeringPage(
        title = if (arabic) {
            "اختيار المولد"
        } else {
            "Generator Selection"
        },
        subtitle = if (arabic) {
            "اختيار مولد من كتالوج المعدات"
        } else {
            "Generator selection from the equipment catalog"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) {
                "بيانات المولد"
            } else {
                "Generator Data"
            }
        ) {
            EngineeringInput(
                label = "Required Generator (kVA)",
                value = requiredKva,
                onValueChange = {
                    requiredKva = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "اختيار المولد"
            } else {
                "Select Generator"
            },
            onClick = {
                try {
                    val kva = requiredKva.toDouble()

                    if (kva <= 0.0) {
                        throw IllegalArgumentException(
                            if (arabic) {
                                "قدرة المولد يجب أن تكون أكبر من صفر."
                            } else {
                                "Generator capacity must be greater than zero."
                            }
                        )
                    }

                    val selection =
                        ElectricalCalculations
                            .selectGeneratorFromCatalog(
                                requiredKva = kva,
                                standard = standard
                            )

                    result = selection.toString()
                    error = null
                } catch (exception: Exception) {
                    result = null
                    error =
                        exception.message
                            ?: if (arabic) {
                                "تعذر تنفيذ الاختيار."
                            } else {
                                "Selection could not be completed."
                            }
                }
            }
        )

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        result?.let {
            EngineeringResult(
                title = if (arabic) {
                    "نتيجة اختيار المولد"
                } else {
                    "Generator Selection Result"
                },
                value = it
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) {
                        "لا توجد نتيجة بعد"
                    } else {
                        "No Result Yet"
                    },
                    message = if (arabic) {
                        "أدخل القدرة المطلوبة ثم نفذ الاختيار."
                    } else {
                        "Enter the required capacity and run the selection."
                    }
                )
            }
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

    var current by remember {
        mutableStateOf("400")
    }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    EngineeringPage(
        title = if (arabic) {
            "تصميم اللوحة"
        } else {
            "Panel Design"
        },
        subtitle = if (arabic) {
            "اختيار اللوحة طبقًا للتيار التصميمي"
        } else {
            "Panel selection based on design current"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) {
                "بيانات اللوحة"
            } else {
                "Panel Data"
            }
        ) {
            EngineeringInput(
                label = "Design Current (A)",
                value = current,
                onValueChange = {
                    current = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "اختيار اللوحة"
            } else {
                "Select Panel"
            },
            onClick = {
                try {
                    val currentValue = current.toDouble()

                    if (currentValue <= 0.0) {
                        throw IllegalArgumentException(
                            if (arabic) {
                                "تيار التصميم يجب أن يكون أكبر من صفر."
                            } else {
                                "Design current must be greater than zero."
                            }
                        )
                    }

                    result =
                        ElectricalCalculations
                            .selectPanelFromCatalog(
                                currentA = currentValue,
                                standard = standard
                            )
                            .toString()

                    error = null
                } catch (exception: Exception) {
                    result = null
                    error =
                        exception.message
                            ?: if (arabic) {
                                "تعذر تنفيذ اختيار اللوحة."
                            } else {
                                "Panel selection could not be completed."
                            }
                }
            }
        )

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        result?.let {
            EngineeringResult(
                title = if (arabic) {
                    "نتيجة اللوحة"
                } else {
                    "Panel Result"
                },
                value = it
            )
        }

        onOpenSld?.let { openSld ->
            EngineeringCard(
                title = if (arabic) {
                    "المخطط الأحادي"
                } else {
                    "Single Line Diagram"
                }
            ) {
                EngineeringSecondaryButton(
                    text = if (arabic) {
                        "فتح مصمم SLD"
                    } else {
                        "Open SLD Designer"
                    },
                    onClick = openSld,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AccountTree,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        if (result == null && error == null) {
            EngineeringEmptyState(
                title = if (arabic) {
                    "لا توجد نتيجة بعد"
                } else {
                    "No Result Yet"
                },
                message = if (arabic) {
                    "أدخل تيار اللوحة ثم اضغط اختيار اللوحة."
                } else {
                    "Enter the panel current and run the selection."
                }
            )
        }
    }
}

@Composable
private fun ShortCircuitEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var voltage by remember {
        mutableStateOf("400")
    }

    var length by remember {
        mutableStateOf("30")
    }

    var section by remember {
        mutableStateOf("70")
    }

    var sourceIk by remember {
        mutableStateOf("25")
    }

    var material by remember {
        mutableStateOf(
            ConductorMaterial.Copper
        )
    }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    EngineeringPage(
        title = if (arabic) {
            "تيار القصر"
        } else {
            "Short Circuit"
        },
        subtitle = if (arabic) {
            "حساب تيار القصر عند نقطة الدائرة"
        } else {
            "Prospective short-circuit current calculation"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) {
                "بيانات القصر"
            } else {
                "Short-Circuit Data"
            }
        ) {
            EngineeringInput(
                label = "Voltage (V)",
                value = voltage,
                onValueChange = {
                    voltage = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Cable Length (m)",
                value = length,
                onValueChange = {
                    length = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Cable Section (mm²)",
                value = section,
                onValueChange = {
                    section = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Source Ik (kA)",
                value = sourceIk,
                onValueChange = {
                    sourceIk = it
                    result = null
                    error = null
                }
            )

            MaterialSelector(
                language = language,
                value = material,
                onChange = {
                    material = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "احسب تيار القصر"
            } else {
                "Calculate Short Circuit"
            },
            onClick = {
                try {
                    val v = voltage.toDouble()
                    val cableLength = length.toDouble()
                    val cableSection = section.toDouble()
                    val source = sourceIk.toDouble()

                    if (
                        v <= 0.0 ||
                        cableLength < 0.0 ||
                        cableSection <= 0.0 ||
                        source <= 0.0
                    ) {
                        throw IllegalArgumentException(
                            if (arabic) {
                                "راجع قيم الجهد والكابل وتيار المصدر."
                            } else {
                                "Check voltage, cable and source fault current."
                            }
                        )
                    }

                    val calculation =
                        ElectricalCalculations
                            .calculateShortCircuitCurrent(
                                voltage = v,
                                length = cableLength,
                                sectionMm2 = cableSection,
                                material = material,
                                currentType =
                                    CurrentType.AlternatingThreePhase,
                                sourceIkKA = source
                            )

                    result = calculation.toString()
                    error = null
                } catch (exception: Exception) {
                    result = null
                    error =
                        exception.message
                            ?: if (arabic) {
                                "تعذر تنفيذ الحساب."
                            } else {
                                "Calculation could not be completed."
                            }
                }
            }
        )

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        result?.let {
            EngineeringResult(
                title = if (arabic) {
                    "نتيجة دراسة القصر"
                } else {
                    "Short-Circuit Study Result"
                },
                value = it
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) {
                        "لا توجد نتيجة بعد"
                    } else {
                        "No Result Yet"
                    },
                    message = if (arabic) {
                        "أدخل بيانات القصر ثم اضغط حساب."
                    } else {
                        "Enter the short-circuit data and calculate."
                    }
                )
            }
        }
    }
}

@Composable
private fun ProtectionEngineeringScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var designCurrent by remember {
        mutableStateOf("100")
    }

    var breaker by remember {
        mutableStateOf("125")
    }

    var cable by remember {
        mutableStateOf("150")
    }

    var fault by remember {
        mutableStateOf("10")
    }

    var breakingCapacity by remember {
        mutableStateOf("25")
    }

    var result by remember {
        mutableStateOf<String?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    EngineeringPage(
        title = if (arabic) {
            "الحماية والتنسيق"
        } else {
            "Protection & Coordination"
        },
        subtitle = if (arabic) {
            "فحص تنسيق القاطع وقدرة القطع"
        } else {
            "Breaker coordination and breaking-capacity checks"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) {
                "بيانات الحماية"
            } else {
                "Protection Data"
            }
        ) {
            EngineeringInput(
                label = "Design Current (A)",
                value = designCurrent,
                onValueChange = {
                    designCurrent = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Breaker Rating (A)",
                value = breaker,
                onValueChange = {
                    breaker = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Cable Ampacity (A)",
                value = cable,
                onValueChange = {
                    cable = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Fault Current (kA)",
                value = fault,
                onValueChange = {
                    fault = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Breaker Breaking Capacity (kA)",
                value = breakingCapacity,
                onValueChange = {
                    breakingCapacity = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "فحص الحماية"
            } else {
                "Validate Protection"
            },
            onClick = {
                try {
                    val coordination =
                        ElectricalCalculations
                            .checkBreakerCoordination(
                                designCurrentA =
                                    designCurrent.toDouble(),
                                breakerRatingA =
                                    breaker.toDouble(),
                                cableAmpacityA =
                                    cable.toDouble()
                            )

                    val breaking =
                        ElectricalCalculations
                            .checkBreakingCapacity(
                                prospectiveFaultCurrentKA =
                                    fault.toDouble(),
                                breakerBreakingCapacityKA =
                                    breakingCapacity.toDouble()
                            )

                    result =
                        "Coordination = ${
                            if (coordination) {
                                "PASS"
                            } else {
                                "CHECK"
                            }
                        }\nBreaking Capacity = ${
                            if (breaking) {
                                "PASS"
                            } else {
                                "CHECK"
                            }
                        }"

                    error = null
                } catch (exception: Exception) {
                    result = null
                    error =
                        exception.message
                            ?: if (arabic) {
                                "تعذر تنفيذ فحص الحماية."
                            } else {
                                "Protection validation could not be completed."
                            }
                }
            }
        )

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        result?.let {
            EngineeringResult(
                title = if (arabic) {
                    "حالة الحماية"
                } else {
                    "Protection Status"
                },
                value = it
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) {
                        "لا توجد نتيجة بعد"
                    } else {
                        "No Result Yet"
                    },
                    message = if (arabic) {
                        "أدخل بيانات الحماية ثم نفذ الفحص."
                    } else {
                        "Enter protection data and validate."
                    }
                )
            }
        }
    }
}

@Composable
private fun EngineeringReportModuleScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    EngineeringPage(
        title = if (arabic) {
            "التقرير الهندسي"
        } else {
            "Engineering Report"
        },
        subtitle = if (arabic) {
            "مركز مخرجات التصميم والحسابات"
        } else {
            "Engineering calculation and design deliverables"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) {
                "مخرجات المشروع"
            } else {
                "Project Deliverables"
            }
        ) {
            EngineeringStatus(
                text = if (arabic) {
                    "الحسابات الأساسية مرتبطة بالـ Core.\n" +
                        "مصمم SLD يعمل من مساره المستقل."
                } else {
                    "Core-connected engineering calculations.\n" +
                        "SLD remains on its dedicated route."
                }
            )

            EngineeringSectionTitle(
                text = if (arabic) {
                    "المخرجات المستهدفة"
                } else {
                    "Target Deliverables"
                }
            )

            EngineeringValueRow(
                label = if (arabic) {
                    "الحسابات"
                } else {
                    "Calculations"
                },
                value = if (arabic) {
                    "Electrical / Water / Sewage"
                } else {
                    "Electrical / Water / Sewage"
                }
            )

            EngineeringValueRow(
                label = "SLD",
                value = if (arabic) {
                    "Interactive Engineering Diagram"
                } else {
                    "Interactive Engineering Diagram"
                }
            )

            EngineeringValueRow(
                label = "PDF",
                value = if (arabic) {
                    "Engineering Report"
                } else {
                    "Engineering Report"
                }
            )
        }

        EngineeringEmptyState(
            title = if (arabic) {
                "التقرير الكامل مرتبط بالمشروع"
            } else {
                "Full Report Is Project-Based"
            },
            message = if (arabic) {
                "سيتم ربط التقرير النهائي بنتائج المشروع وSLD بعد تثبيت واجهة النظام الموحدة."
            } else {
                "The final report will consume project and SLD results after the unified interface is stabilized."
            }
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

    var expanded by remember {
        mutableStateOf(false)
    }

    EngineeringSecondaryButton(
        text = when (value) {
            CurrentType.DirectCurrent ->
                if (arabic) "تيار مستمر — DC" else "DC"

            CurrentType.AlternatingSinglePhase ->
                if (arabic) "أحادي الطور — 1 Phase" else "1 Phase"

            CurrentType.AlternatingTwoPhase ->
                if (arabic) "ثنائي الطور — 2 Phase" else "2 Phase"

            CurrentType.AlternatingThreePhase ->
                if (arabic) "ثلاثي الطور — 3 Phase" else "3 Phase"
        },
        onClick = {
            expanded = true
        }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {
        listOf(
            CurrentType.DirectCurrent,
            CurrentType.AlternatingSinglePhase,
            CurrentType.AlternatingTwoPhase,
            CurrentType.AlternatingThreePhase
        ).forEach { type ->
            DropdownMenuItem(
                text = {
                    androidx.compose.material3.Text(
                        when (type) {
                            CurrentType.DirectCurrent ->
                                if (arabic) {
                                    "تيار مستمر — DC"
                                } else {
                                    "DC"
                                }

                            CurrentType.AlternatingSinglePhase ->
                                if (arabic) {
                                    "أحادي الطور — 1 Phase"
                                } else {
                                    "1 Phase"
                                }

                            CurrentType.AlternatingTwoPhase ->
                                if (arabic) {
                                    "ثنائي الطور — 2 Phase"
                                } else {
                                    "2 Phase"
                                }

                            CurrentType.AlternatingThreePhase ->
                                if (arabic) {
                                    "ثلاثي الطور — 3 Phase"
                                } else {
                                    "3 Phase"
                                }
                        }
                    )
                },
                onClick = {
                    onChange(type)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun MaterialSelector(
    language: AppLanguage,
    value: ConductorMaterial,
    onChange: (ConductorMaterial) -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var expanded by remember {
        mutableStateOf(false)
    }

    EngineeringSecondaryButton(
        text = if (value == ConductorMaterial.Copper) {
            if (arabic) {
                "موصل نحاس — Copper"
            } else {
                "Copper"
            }
        } else {
            if (arabic) {
                "موصل ألومنيوم — Aluminum"
            } else {
                "Aluminum"
            }
        },
        onClick = {
            expanded = true
        }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {
        DropdownMenuItem(
            text = {
                androidx.compose.material3.Text(
                    if (arabic) {
                        "موصل نحاس — Copper"
                    } else {
                        "Copper"
                    }
                )
            },
            onClick = {
                onChange(ConductorMaterial.Copper)
                expanded = false
            }
        )

        DropdownMenuItem(
            text = {
                androidx.compose.material3.Text(
                    if (arabic) {
                        "موصل ألومنيوم — Aluminum"
                    } else {
                        "Aluminum"
                    }
                )
            },
            onClick = {
                onChange(ConductorMaterial.Aluminum)
                expanded = false
            }
        )
    }
}
