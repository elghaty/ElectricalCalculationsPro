package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.CurrentType
import com.electrical.calculationspro.data.ElectricalCalculations
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.iecInstallationMethods
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.sewage.SewageDesignInput
import com.electrical.calculationspro.data.sewage.SewageDesignModule
import com.electrical.calculationspro.data.water.WaterDesignInput
import com.electrical.calculationspro.data.water.WaterDesignModule
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
    WATER,
    SEWAGE,
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

        EngineeringModule.WATER ->
            WaterEngineeringModuleScreen(
                language = language,
                onBack = onBack
            )

        EngineeringModule.SEWAGE ->
            SewageEngineeringModuleScreen(
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

    var loadKw by remember { mutableStateOf("100") }
    var voltage by remember { mutableStateOf("400") }
    var pf by remember { mutableStateOf("0.90") }

    var currentType by remember {
        mutableStateOf(CurrentType.AlternatingThreePhase)
    }

    var result by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "حساب الأحمال" else "Load Calculation",
        subtitle = if (arabic) {
            "حساب تيار التصميم للحمل الكهربائي"
        } else {
            "Design current calculation"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات الحمل" else "Load Data"
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
                label = "Voltage (V)",
                value = voltage,
                onValueChange = {
                    voltage = it
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

            CurrentTypeSelector(
                language = language,
                value = currentType,
                onChange = {
                    currentType = it
                    result = null
                    error = null
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

                    require(load > 0.0)
                    require(v > 0.0)
                    require(factor > 0.0 && factor <= 1.0)

                    result =
                        ElectricalCalculations.calculateDesignCurrentFromKw(
                            loadKw = load,
                            voltage = v,
                            powerFactor = factor,
                            currentType = currentType
                        )

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
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
                title = if (arabic) "تيار التصميم" else "Design Current",
                value = "%.2f A".format(it)
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل البيانات ثم اضغط حساب."
                } else {
                    "Enter the data and calculate."
                }
            )
        } else {
            Unit
        }
    }
}

@Composable
private fun ConductorSizingScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var load by remember { mutableStateOf("100") }
    var voltage by remember { mutableStateOf("400") }
    var pf by remember { mutableStateOf("0.90") }
    var length by remember { mutableStateOf("50") }
    var ambient by remember { mutableStateOf("30") }
    var circuits by remember { mutableStateOf("1") }
    var maxDrop by remember { mutableStateOf("4") }

    var currentType by remember {
        mutableStateOf(CurrentType.AlternatingThreePhase)
    }

    var material by remember {
        mutableStateOf(ConductorMaterial.Copper)
    }

    var insulation by remember {
        mutableStateOf(InsulationType.XLPE)
    }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "اختيار الكابل" else "Cable Sizing",
        subtitle = if (arabic) {
            "اختيار الموصل طبقًا لتيار التصميم والهبوط والقواعد الهندسية"
        } else {
            "Professional conductor sizing"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات الكابل" else "Cable Data"
        ) {
            EngineeringInput(
                label = "Load (kW)",
                value = load,
                onValueChange = {
                    load = it
                    result = null
                    error = null
                }
            )

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
                label = "Power Factor",
                value = pf,
                onValueChange = {
                    pf = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Length (m)",
                value = length,
                onValueChange = {
                    length = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Ambient Temperature (°C)",
                value = ambient,
                onValueChange = {
                    ambient = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Circuits in Conduit",
                value = circuits,
                onValueChange = {
                    circuits = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Maximum Voltage Drop (%)",
                value = maxDrop,
                onValueChange = {
                    maxDrop = it
                    result = null
                    error = null
                }
            )

            CurrentTypeSelector(
                language = language,
                value = currentType,
                onChange = {
                    currentType = it
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
            text = if (arabic) "حساب واختيار الكابل" else "Calculate & Select Cable",
            onClick = {
                try {
                    val input = ConductorSizingInput(
                        currentType = currentType,
                        voltage = voltage.toDouble(),
                        load = load.toDouble(),
                        powerFactor = pf.toDouble(),
                        lineLength = length.toDouble(),
                        installationMethod = iecInstallationMethods.first(),
                        ambientTemp = ambient.toDouble(),
                        conductor = material,
                        insulation = insulation,
                        circuitsInConduit = circuits.toInt(),
                        maxVoltageDrop = maxDrop.toDouble()
                    )

                    val calculation =
                        ElectricalCalculations.sizeConductor(
                            input = input,
                            standard = standard
                        )

                    result = buildString {
                        appendLine(
                            "Design Current = %.2f A"
                                .format(calculation.designCurrent)
                        )
                        appendLine(
                            "Recommended Section = %.1f mm²"
                                .format(calculation.recommendedSection)
                        )
                        appendLine(
                            "Selected Section = %.1f mm²"
                                .format(calculation.selectedSection)
                        )
                        appendLine(
                            "Ampacity = %.2f A"
                                .format(calculation.ampacity)
                        )
                        appendLine(
                            "Voltage Drop = %.2f %%"
                                .format(calculation.voltageDropPercent)
                        )
                        appendLine(
                            "Breaker = %.0f A"
                                .format(calculation.protectiveDevice)
                        )
                        appendLine(
                            "Short Circuit = %.2f kA"
                                .format(calculation.shortCircuitCurrentKA)
                        )
                        appendLine(
                            "Cable Capacity = ${
                                if (calculation.breakerWithinCableCapacity) {
                                    "PASS"
                                } else {
                                    "CHECK"
                                }
                            }"
                        )
                        appendLine(
                            "Voltage Drop = ${
                                if (calculation.voltageDropWithinLimit) {
                                    "PASS"
                                } else {
                                    "CHECK"
                                }
                            }"
                        )
                    }

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر تنفيذ حساب الكابل."
                    } else {
                        "Cable calculation failed."
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
                title = if (arabic) "نتيجة الكابل" else "Cable Result",
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل بيانات الكابل ثم نفذ الحساب."
                } else {
                    "Enter cable data and calculate."
                }
            )
        } else {
            Unit
        }
    }
}

@Composable
private fun ProfessionalVoltageDropScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var current by remember { mutableStateOf("100") }
    var length by remember { mutableStateOf("50") }
    var section by remember { mutableStateOf("35") }
    var pf by remember { mutableStateOf("0.90") }
    var voltage by remember { mutableStateOf("400") }

    var currentType by remember {
        mutableStateOf(CurrentType.AlternatingThreePhase)
    }

    var material by remember {
        mutableStateOf(ConductorMaterial.Copper)
    }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "هبوط الجهد" else "Voltage Drop",
        subtitle = if (arabic) {
            "حساب هبوط الجهد بالنسبة المئوية والفولت"
        } else {
            "Voltage-drop calculation"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات الدائرة" else "Circuit Data"
        ) {
            EngineeringInput(
                label = "Current (A)",
                value = current,
                onValueChange = {
                    current = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Length (m)",
                value = length,
                onValueChange = {
                    length = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = "Section (mm²)",
                value = section,
                onValueChange = {
                    section = it
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
                label = "Voltage (V)",
                value = voltage,
                onValueChange = {
                    voltage = it
                    result = null
                    error = null
                }
            )

            CurrentTypeSelector(
                language = language,
                value = currentType,
                onChange = {
                    currentType = it
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
            text = if (arabic) "احسب هبوط الجهد" else "Calculate Voltage Drop",
            onClick = {
                try {
                    val values =
                        ElectricalCalculations.calculateVoltageDrop(
                            current = current.toDouble(),
                            length = length.toDouble(),
                            sectionMm2 = section.toDouble(),
                            powerFactor = pf.toDouble(),
                            currentType = currentType,
                            material = material,
                            voltage = voltage.toDouble()
                        )

                    result =
                        "Voltage Drop = %.3f V\nVoltage Drop = %.3f %%"
                            .format(values.first, values.second)

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر حساب هبوط الجهد."
                    } else {
                        "Voltage-drop calculation failed."
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
                title = if (arabic) "نتيجة هبوط الجهد" else "Voltage Drop Result",
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل بيانات الدائرة ثم اضغط حساب."
                } else {
                    "Enter circuit data and calculate."
                }
            )
        } else {
            Unit
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

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "اختيار القاطع" else "Breaker Selection",
        subtitle = if (arabic) {
            "اختيار القاطع والتحقق من التنسيق"
        } else {
            "Breaker selection and coordination"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات القاطع" else "Breaker Data"
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
            text = if (arabic) "اختيار القاطع" else "Select Breaker",
            onClick = {
                try {
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

                    val breaking =
                        ElectricalCalculations.checkBreakingCapacity(
                            prospectiveFaultCurrentKA = fault,
                            breakerBreakingCapacityKA = fault
                        )

                    result =
                        "Breaker = %.0f A\nCoordination = %s\nFault = %.2f kA\nBreaking Capacity Check = %s"
                            .format(
                                rating,
                                if (coordination) "PASS" else "CHECK",
                                fault,
                                if (breaking) "PASS" else "CHECK"
                            )

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر اختيار القاطع."
                    } else {
                        "Breaker selection failed."
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
                title = if (arabic) "نتيجة القاطع" else "Breaker Result",
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل بيانات القاطع ثم نفذ الاختيار."
                } else {
                    "Enter breaker data and run the selection."
                }
            )
        } else {
            Unit
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

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "اختيار المحول" else "Transformer Sizing",
        subtitle = if (arabic) {
            "حساب القدرة المطلوبة واختيار المقاس القياسي"
        } else {
            "Transformer sizing and standard selection"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات المحول" else "Transformer Data"
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
        }

        EngineeringPrimaryButton(
            text = if (arabic) "احسب المحول" else "Calculate Transformer",
            onClick = {
                try {
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

                    result =
                        "Required = %.1f kVA\nSelected = %.0f kVA"
                            .format(required, selected)

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر حساب المحول."
                    } else {
                        "Transformer calculation failed."
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
                title = if (arabic) "نتيجة المحول" else "Transformer Result",
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل بيانات المحول ثم اضغط حساب."
                } else {
                    "Enter transformer data and calculate."
                }
            )
        } else {
            Unit
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
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "اختيار المولد" else "Generator Selection",
        subtitle = if (arabic) {
            "اختيار مولد من كتالوج المعدات"
        } else {
            "Generator selection from the equipment catalog"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات المولد" else "Generator Data"
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
            text = if (arabic) "اختيار المولد" else "Select Generator",
            onClick = {
                try {
                    result =
                        ElectricalCalculations
                            .selectGeneratorFromCatalog(
                                requiredKva = requiredKva.toDouble(),
                                standard = standard
                            )
                            .toString()

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر اختيار المولد."
                    } else {
                        "Generator selection failed."
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
                title = if (arabic) "نتيجة المولد" else "Generator Result",
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل القدرة المطلوبة ثم نفذ الاختيار."
                } else {
                    "Enter the required capacity and select."
                }
            )
        } else {
            Unit
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
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "تصميم اللوحة" else "Panel Design",
        subtitle = if (arabic) {
            "اختيار اللوحة طبقًا للتيار التصميمي"
        } else {
            "Panel selection based on design current"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات اللوحة" else "Panel Data"
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
            text = if (arabic) "اختيار اللوحة" else "Select Panel",
            onClick = {
                try {
                    result =
                        ElectricalCalculations
                            .selectPanelFromCatalog(
                                currentA = current.toDouble(),
                                standard = standard
                            )
                            .toString()

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر اختيار اللوحة."
                    } else {
                        "Panel selection failed."
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
                title = if (arabic) "نتيجة اللوحة" else "Panel Result",
                value = it
            )
        }

        onOpenSld?.let {
            EngineeringCard(
                title = if (arabic) {
                    "المخطط الأحادي"
                } else {
                    "Single Line Diagram"
                }
            ) {
                EngineeringSecondaryButton(
                    text = if (arabic) "فتح مصمم SLD" else "Open SLD Designer",
                    onClick = it,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AccountTree,
                            contentDescription = null
                        )
                    }
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

    var material by remember {
        mutableStateOf(ConductorMaterial.Copper)
    }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "تيار القصر" else "Short Circuit",
        subtitle = if (arabic) {
            "حساب تيار القصر عند نقطة الدائرة"
        } else {
            "Prospective short-circuit calculation"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات القصر" else "Short-Circuit Data"
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
            text = if (arabic) "احسب تيار القصر" else "Calculate Short Circuit",
            onClick = {
                try {
                    val calculation =
                        ElectricalCalculations.calculateShortCircuitCurrent(
                            voltage = voltage.toDouble(),
                            length = length.toDouble(),
                            sectionMm2 = section.toDouble(),
                            material = material,
                            currentType = CurrentType.AlternatingThreePhase,
                            sourceIkKA = sourceIk.toDouble()
                        )

                    result = calculation.toString()
                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر تنفيذ حساب القصر."
                    } else {
                        "Short-circuit calculation failed."
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
                title = if (arabic) "نتيجة القصر" else "Short-Circuit Result",
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل بيانات القصر ثم اضغط حساب."
                } else {
                    "Enter short-circuit data and calculate."
                }
            )
        } else {
            Unit
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

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "الحماية والتنسيق" else "Protection & Coordination",
        subtitle = if (arabic) {
            "فحص تنسيق القاطع وقدرة القطع"
        } else {
            "Breaker coordination and breaking capacity"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات الحماية" else "Protection Data"
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
                label = "Breaking Capacity (kA)",
                value = breakingCapacity,
                onValueChange = {
                    breakingCapacity = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) "فحص الحماية" else "Validate Protection",
            onClick = {
                try {
                    val coordination =
                        ElectricalCalculations.checkBreakerCoordination(
                            designCurrentA = designCurrent.toDouble(),
                            breakerRatingA = breaker.toDouble(),
                            cableAmpacityA = cable.toDouble()
                        )

                    val breaking =
                        ElectricalCalculations.checkBreakingCapacity(
                            prospectiveFaultCurrentKA = fault.toDouble(),
                            breakerBreakingCapacityKA =
                                breakingCapacity.toDouble()
                        )

                    result =
                        "Coordination = ${
                            if (coordination) "PASS" else "CHECK"
                        }\nBreaking Capacity = ${
                            if (breaking) "PASS" else "CHECK"
                        }"

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر تنفيذ فحص الحماية."
                    } else {
                        "Protection validation failed."
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
                title = if (arabic) "حالة الحماية" else "Protection Status",
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل بيانات الحماية ثم نفذ الفحص."
                } else {
                    "Enter protection data and validate."
                }
            )
        } else {
            Unit
        }
    }
}

@Composable
private fun WaterEngineeringModuleScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var flow by remember { mutableStateOf("100") }
    var diameter by remember { mutableStateOf("150") }
    var velocity by remember { mutableStateOf("") }
    var headLoss by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("100") }
    var material by remember { mutableStateOf("Ductile Iron") }
    var staticHead by remember { mutableStateOf("20") }
    var minorLoss by remember { mutableStateOf("0") }
    var pressureHead by remember { mutableStateOf("0") }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "تصميم المياه" else "Water Design",
        subtitle = if (arabic) {
            "التدفق والقطر والسرعة والفواقد وTDH وتصميم المضخة"
        } else {
            "Flow, diameter, velocity, losses, TDH and pump design"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "البيانات الهيدروليكية" else "Hydraulic Data"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Flow (m³/h)",
                    value = flow,
                    onValueChange = {
                        flow = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Diameter (mm)",
                    value = diameter,
                    onValueChange = {
                        diameter = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Velocity (m/s)",
                    value = velocity,
                    onValueChange = {
                        velocity = it
                        result = null
                        error = null
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Head Loss (m)",
                    value = headLoss,
                    enabled = false,
                    onValueChange = {}
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Pipe Length (m)",
                    value = length,
                    onValueChange = {
                        length = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Material",
                    value = material,
                    isNumeric = false,
                    onValueChange = {
                        material = it
                        result = null
                        error = null
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Static Head (m)",
                    value = staticHead,
                    onValueChange = {
                        staticHead = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Minor Loss (m)",
                    value = minorLoss,
                    onValueChange = {
                        minorLoss = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Pressure Head (m)",
                    value = pressureHead,
                    onValueChange = {
                        pressureHead = it
                        result = null
                        error = null
                    }
                )
            }
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "احسب واحفظ بالمشروع"
            } else {
                "Calculate & Save to Project"
            },
            onClick = {
                try {
                    val activeProject =
                        DesignProjectCoreBridge.getActiveProject()
                            ?: throw IllegalStateException(
                                if (arabic) {
                                    "لا يوجد مشروع نشط."
                                } else {
                                    "No active project."
                                }
                            )

                    val calculation =
                        WaterDesignModule.calculateAndSave(
                            project = activeProject,
                            input = WaterDesignInput(
                                flowM3PerHour =
                                    flow.toDoubleOrNull(),
                                diameterMm =
                                    diameter.toDoubleOrNull(),
                                velocityMPerS =
                                    velocity.toDoubleOrNull(),
                                pipeLengthM =
                                    length.toDouble(),
                                material =
                                    material,
                                staticHeadM =
                                    staticHead.toDouble(),
                                minorLossHeadM =
                                    minorLoss.toDouble(),
                                requiredPressureHeadM =
                                    pressureHead.toDouble()
                            )
                        )

                    flow =
                        "%.3f".format(
                            calculation.flowM3PerHour
                        )

                    diameter =
                        "%.1f".format(
                            calculation.diameterMm
                        )

                    velocity =
                        "%.3f".format(
                            calculation.velocityMPerS
                        )

                    headLoss =
                        "%.3f".format(
                            calculation.frictionLossM
                        )

                    result =
                        buildString {
                            appendLine(
                                "Flow = %.3f m³/h"
                                    .format(
                                        calculation.flowM3PerHour
                                    )
                            )
                            appendLine(
                                "Diameter = %.1f mm"
                                    .format(
                                        calculation.diameterMm
                                    )
                            )
                            appendLine(
                                "Velocity = %.3f m/s"
                                    .format(
                                        calculation.velocityMPerS
                                    )
                            )
                            appendLine(
                                "Friction Loss = %.3f m"
                                    .format(
                                        calculation.frictionLossM
                                    )
                            )
                            appendLine(
                                "Minor Loss = %.3f m"
                                    .format(
                                        minorLoss.toDouble()
                                    )
                            )
                            appendLine(
                                "Pressure Head = %.3f m"
                                    .format(
                                        pressureHead.toDouble()
                                    )
                            )
                            appendLine(
                                "TDH = %.3f m"
                                    .format(
                                        calculation.tdhM
                                    )
                            )
                        }

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر تنفيذ حساب المياه."
                    } else {
                        "Water calculation failed."
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
                    "نتيجة تصميم المياه"
                } else {
                    "Water Design Result"
                },
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل أي قيمتين من التدفق والقطر والسرعة ثم اضغط احسب."
                } else {
                    "Enter any two of flow, diameter and velocity, then calculate."
                }
            )
        } else {
            Unit
        }
    }
}

@Composable
private fun SewageEngineeringModuleScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var averageFlow by remember { mutableStateOf("240") }
    var peakFlow by remember { mutableStateOf("600") }
    var minimumFlow by remember { mutableStateOf("120") }

    var flow by remember { mutableStateOf("25") }
    var diameter by remember { mutableStateOf("200") }
    var velocity by remember { mutableStateOf("") }
    var headLoss by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("100") }
    var material by remember { mutableStateOf("Ductile Iron") }

    var staticHead by remember { mutableStateOf("20") }
    var minorLoss by remember { mutableStateOf("0") }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "تصميم الصرف الصحي" else "Sewage Design",
        subtitle = if (arabic) {
            "التدفقات وخط الطرد والسرعة والفواقد وTDH وربطها بالمشروع"
        } else {
            "Flows, rising main, velocity, losses, TDH and project integration"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "التدفقات" else "Flow Data"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Average Flow (m³/day)",
                    value = averageFlow,
                    onValueChange = {
                        averageFlow = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Peak Flow (m³/day)",
                    value = peakFlow,
                    onValueChange = {
                        peakFlow = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Minimum Flow (m³/day)",
                    value = minimumFlow,
                    onValueChange = {
                        minimumFlow = it
                        result = null
                        error = null
                    }
                )
            }
        }

        EngineeringCard(
            title = if (arabic) {
                "الخط الهيدروليكي"
            } else {
                "Rising Main Hydraulic Data"
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Flow (m³/h)",
                    value = flow,
                    onValueChange = {
                        flow = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Diameter (mm)",
                    value = diameter,
                    onValueChange = {
                        diameter = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Velocity (m/s)",
                    value = velocity,
                    onValueChange = {
                        velocity = it
                        result = null
                        error = null
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Head Loss (m)",
                    value = headLoss,
                    enabled = false,
                    onValueChange = {}
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Length (m)",
                    value = length,
                    onValueChange = {
                        length = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Material",
                    value = material,
                    isNumeric = false,
                    onValueChange = {
                        material = it
                        result = null
                        error = null
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Static Head (m)",
                    value = staticHead,
                    onValueChange = {
                        staticHead = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Minor Loss (m)",
                    value = minorLoss,
                    onValueChange = {
                        minorLoss = it
                        result = null
                        error = null
                    }
                )

                EngineeringInput(
                    modifier = Modifier.weight(1f),
                    label = "Peak Flow (m³/h)",
                    value = "%.3f".format(
                        peakFlow.toDoubleOrNull()?.div(24.0) ?: 0.0
                    ),
                    enabled = false,
                    onValueChange = {}
                )
            }
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "احسب واحفظ بالمشروع"
            } else {
                "Calculate & Save to Project"
            },
            onClick = {
                try {
                    val activeProject =
                        DesignProjectCoreBridge.getActiveProject()
                            ?: throw IllegalStateException(
                                if (arabic) {
                                    "لا يوجد مشروع نشط."
                                } else {
                                    "No active project."
                                }
                            )

                    val avg = averageFlow.toDouble()
                    val peak = peakFlow.toDouble()
                    val minimum = minimumFlow.toDouble()

                    val calculation =
                        SewageDesignModule.calculateAndSave(
                            project = activeProject,
                            input = SewageDesignInput(
                                averageFlowM3PerDay = avg,
                                peakFlowM3PerDay = peak,
                                minimumFlowM3PerDay = minimum,
                                flowM3PerHour =
                                    flow.toDoubleOrNull(),
                                diameterMm =
                                    diameter.toDoubleOrNull(),
                                velocityMPerS =
                                    velocity.toDoubleOrNull(),
                                pipeLengthM =
                                    length.toDouble(),
                                material =
                                    material,
                                staticHeadM =
                                    staticHead.toDouble(),
                                minorLossHeadM =
                                    minorLoss.toDouble()
                            )
                        )

                    flow =
                        "%.3f".format(
                            calculation.risingMainFlowM3PerHour
                        )

                    diameter =
                        "%.1f".format(
                            calculation.diameterMm
                        )

                    velocity =
                        "%.3f".format(
                            calculation.velocityMPerS
                        )

                    headLoss =
                        "%.3f".format(
                            calculation.frictionLossM
                        )

                    result =
                        buildString {
                            appendLine(
                                "Average Flow = %.3f m³/day"
                                    .format(
                                        calculation.averageFlowM3PerDay
                                    )
                            )

                            appendLine(
                                "Peak Flow = %.3f m³/day"
                                    .format(
                                        calculation.peakFlowM3PerDay
                                    )
                            )

                            appendLine(
                                "Minimum Flow = %.3f m³/day"
                                    .format(
                                        calculation.minimumFlowM3PerDay
                                    )
                            )

                            appendLine(
                                "Peak Factor = %.3f"
                                    .format(
                                        calculation.peakFlowM3PerDay /
                                            calculation.averageFlowM3PerDay
                                    )
                            )

                            appendLine(
                                "Rising Main Flow = %.3f m³/h"
                                    .format(
                                        calculation.risingMainFlowM3PerHour
                                    )
                            )

                            appendLine(
                                "Diameter = %.1f mm"
                                    .format(
                                        calculation.diameterMm
                                    )
                            )

                            appendLine(
                                "Velocity = %.3f m/s"
                                    .format(
                                        calculation.velocityMPerS
                                    )
                            )

                            appendLine(
                                "Friction Loss = %.3f m"
                                    .format(
                                        calculation.frictionLossM
                                    )
                            )

                            appendLine(
                                "Minor Loss = %.3f m"
                                    .format(
                                        minorLoss.toDouble()
                                    )
                            )

                            appendLine(
                                "TDH = %.3f m"
                                    .format(
                                        calculation.tdhM
                                    )
                            )
                        }

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message ?: if (arabic) {
                        "تعذر تنفيذ حساب الصرف."
                    } else {
                        "Sewage calculation failed."
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
                    "نتيجة تصميم الصرف"
                } else {
                    "Sewage Design Result"
                },
                value = it
            )
        } ?: if (error == null) {
            EngineeringEmptyState(
                title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                message = if (arabic) {
                    "أدخل بيانات التدفق والقطر أو السرعة ثم اضغط احسب."
                } else {
                    "Enter flow and diameter or velocity, then calculate."
                }
            )
        } else {
            Unit
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
        title = if (arabic) "التقرير الهندسي" else "Engineering Report",
        subtitle = if (arabic) {
            "مركز مخرجات التصميم والحسابات"
        } else {
            "Engineering calculation and design deliverables"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "مخرجات المشروع" else "Project Deliverables"
        ) {
            EngineeringStatus(
                text = if (arabic) {
                    "الحسابات الكهربائية والمياه والصرف مرتبطة بمحركات التصميم."
                } else {
                    "Electrical, water and sewage calculations are connected to the design engines."
                }
            )

            EngineeringSectionTitle(
                text = if (arabic) "المخرجات" else "Deliverables"
            )

            EngineeringValueRow(
                label = if (arabic) "الكهرباء" else "Electrical",
                value = "Load / Cable / Breaker / Transformer / Generator"
            )

            EngineeringValueRow(
                label = if (arabic) "المياه" else "Water",
                value = "Flow / Pipe / Loss / TDH / Pump"
            )

            EngineeringValueRow(
                label = if (arabic) "الصرف" else "Sewage",
                value = "Flow / Rising Main / Loss / TDH / Pump"
            )

            EngineeringValueRow(
                label = "SLD",
                value = "Interactive Engineering Diagram"
            )
        }

        EngineeringEmptyState(
            title = if (arabic) {
                "التقرير الكامل مرتبط بالمشروع"
            } else {
                "Full Report Is Project-Based"
            },
            message = if (arabic) {
                "نتائج التصميم تحفظ داخل المشروع وتستخدم في مخرجات التقرير."
            } else {
                "Design results are stored in the project and consumed by the report."
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

    var expanded by remember { mutableStateOf(false) }

    EngineeringSecondaryButton(
        text = when (value) {
            CurrentType.DirectCurrent ->
                if (arabic) "تيار مستمر — DC" else "DC"

            CurrentType.AlternatingSinglePhase ->
                if (arabic) "أحادي الطور" else "1 Phase"

            CurrentType.AlternatingTwoPhase ->
                if (arabic) "ثنائي الطور" else "2 Phase"

            CurrentType.AlternatingThreePhase ->
                if (arabic) "ثلاثي الطور" else "3 Phase"
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
                    Text(
                        when (type) {
                            CurrentType.DirectCurrent ->
                                if (arabic) "تيار مستمر — DC" else "DC"

                            CurrentType.AlternatingSinglePhase ->
                                if (arabic) "أحادي الطور" else "1 Phase"

                            CurrentType.AlternatingTwoPhase ->
                                if (arabic) "ثنائي الطور" else "2 Phase"

                            CurrentType.AlternatingThreePhase ->
                                if (arabic) "ثلاثي الطور" else "3 Phase"
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

    var expanded by remember { mutableStateOf(false) }

    EngineeringSecondaryButton(
        text = if (value == ConductorMaterial.Copper) {
            if (arabic) "نحاس — Copper" else "Copper"
        } else {
            if (arabic) "ألومنيوم — Aluminum" else "Aluminum"
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
                Text(
                    if (arabic) "نحاس — Copper" else "Copper"
                )
            },
            onClick = {
                onChange(ConductorMaterial.Copper)
                expanded = false
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    if (arabic) "ألومنيوم — Aluminum" else "Aluminum"
                )
            },
            onClick = {
                onChange(ConductorMaterial.Aluminum)
                expanded = false
            }
        )
    }
}
