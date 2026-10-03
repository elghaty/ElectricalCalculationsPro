package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.ConductorSizingInput
import com.electrical.calculationspro.data.iecInstallationMethods
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.RisingMainDesign
import com.electrical.calculationspro.data.project.WaterPipe
import com.electrical.calculationspro.data.sewage.SewageDesignEngine
import com.electrical.calculationspro.data.sewage.SewageDesignModule
import com.electrical.calculationspro.data.water.WaterDesignEngine
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
        EngineeringModule.LOAD -> {
            LoadEngineeringScreen(
                language = language,
                onBack = onBack
            )
        }

        EngineeringModule.CURRENT -> {
            CurrentCalculationScreen(
                language = language,
                onBack = onBack
            )
        }

        EngineeringModule.CABLE -> {
            ConductorSizingScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )
        }

        EngineeringModule.VOLTAGE_DROP -> {
            ProfessionalVoltageDropScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )
        }

        EngineeringModule.BREAKER -> {
            BreakerEngineeringScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )
        }

        EngineeringModule.TRANSFORMER -> {
            TransformerEngineeringScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )
        }

        EngineeringModule.GENERATOR -> {
            GeneratorEngineeringScreen(
                language = language,
                standard = standard,
                onBack = onBack
            )
        }

        EngineeringModule.PANEL -> {
            PanelEngineeringScreen(
                language = language,
                standard = standard,
                onBack = onBack,
                onOpenSld = onOpenSld
            )
        }

        EngineeringModule.SHORT_CIRCUIT -> {
            ShortCircuitEngineeringScreen(
                language = language,
                onBack = onBack
            )
        }

        EngineeringModule.PROTECTION -> {
            ProtectionEngineeringScreen(
                language = language,
                onBack = onBack
            )
        }

        EngineeringModule.WATER -> {
            WaterEngineeringModuleScreen(
                language = language,
                onBack = onBack
            )
        }

        EngineeringModule.SEWAGE -> {
            SewageEngineeringModuleScreen(
                language = language,
                onBack = onBack
            )
        }

        EngineeringModule.REPORT -> {
            EngineeringReportModuleScreen(
                language = language,
                onBack = onBack
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
                label = if (arabic) "الحمل (kW)" else "Load (kW)",
                value = loadKw,
                onValueChange = {
                    loadKw = it
                    result = null
                    error = null
                }
            )

            EngineeringInput(
                label = if (arabic) "الجهد (V)" else "Voltage (V)",
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
            text = if (arabic) "احسب تيار التصميم" else "Calculate Design Current",
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
                    error = e.message
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
                title = if (arabic) "تيار التصميم" else "Design Current",
                value = "%.2f A".format(it)
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
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
private fun ConductorSizingScreen(
    language: AppLanguage,
    standard: Standard,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var current by remember { mutableStateOf("100") }
    var length by remember { mutableStateOf("30") }
    var voltage by remember { mutableStateOf("400") }

    var material by remember {
        mutableStateOf(ConductorMaterial.Copper)
    }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "اختيار الكابل" else "Cable Sizing",
        subtitle = if (arabic) {
            "اختيار مقطع الموصل والتحقق من السعة وهبوط الجهد"
        } else {
            "Conductor sizing with ampacity and voltage-drop verification"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات الكابل" else "Cable Data"
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
                label = "Voltage (V)",
                value = voltage,
                onValueChange = {
                    voltage = it
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
            text = if (arabic) "اختيار مقطع الكابل" else "Select Cable Size",
            onClick = {
                try {
                    val designCurrent = current.toDouble()
                    val cableLength = length.toDouble()
                    val cableVoltage = voltage.toDouble()

                    require(designCurrent > 0.0)
                    require(cableLength >= 0.0)
                    require(cableVoltage > 0.0)

                    val input = ConductorSizingInput(
                        currentType = CurrentType.AlternatingThreePhase,
                        voltage = cableVoltage,
                        load = designCurrent * cableVoltage * 0.90,
                        powerFactor = 0.90,
                        lineLength = cableLength,
                        installationMethod = iecInstallationMethods.first(),
                        ambientTemp = 30.0,
                        conductor = material,
                        insulation = InsulationType.PVC,
                        circuitsInConduit = 1,
                        maxVoltageDrop = 4.0
                    )

                    val automatic =
                        ElectricalCalculations.sizeConductor(
                            input = input,
                            standard = standard
                        )

                    result =
                        "Required / Recommended = %.1f mm²\n".format(
                            automatic.recommendedSection
                        ) +
                        "Selected = %.1f mm²\n".format(
                            automatic.selectedSection
                        ) +
                        "Ampacity = %.1f A\n".format(
                            automatic.ampacity
                        ) +
                        "Voltage Drop = %.2f %% (%.2f V)\n".format(
                            automatic.voltageDropPercent,
                            automatic.voltageDropVolts
                        ) +
                        "Breaker = %.0f A".format(
                            automatic.protectiveDevice
                        )

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
                        ?: if (arabic) {
                            "تعذر اختيار الكابل."
                        } else {
                            "Cable selection failed."
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
                    "نتيجة اختيار الكابل"
                } else {
                    "Cable Selection Result"
                },
                value = it
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                    message = if (arabic) {
                        "أدخل تيار التصميم ثم اختر مقطع الكابل."
                    } else {
                        "Enter design current and select cable size."
                    }
                )
            }
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
    var voltage by remember { mutableStateOf("400") }
    var length by remember { mutableStateOf("30") }
    var section by remember { mutableStateOf("70") }
    var pf by remember { mutableStateOf("0.90") }

    var material by remember {
        mutableStateOf(ConductorMaterial.Copper)
    }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "هبوط الجهد" else "Voltage Drop",
        subtitle = if (arabic) {
            "التحقق من هبوط الجهد في المغذي"
        } else {
            "Feeder voltage-drop verification"
        },
        onBack = onBack
    ) {
        EngineeringCard(
            title = if (arabic) "بيانات هبوط الجهد" else "Voltage Drop Data"
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
                label = "Voltage (V)",
                value = voltage,
                onValueChange = {
                    voltage = it
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
                    val i = current.toDouble()
                    val v = voltage.toDouble()
                    val l = length.toDouble()
                    val s = section.toDouble()
                    val factor = pf.toDouble()

                    require(i > 0.0)
                    require(v > 0.0)
                    require(l >= 0.0)
                    require(s > 0.0)
                    require(factor > 0.0 && factor <= 1.0)

                    val calculation =
                        ElectricalCalculations.calculateVoltageDrop(
                            current = i,
                            length = l,
                            sectionMm2 = s,
                            powerFactor = factor,
                            currentType =
                                CurrentType.AlternatingThreePhase,
                            material = material,
                            voltage = v
                        )

                    result =
                        "Voltage Drop = %.3f V\n".format(
                            calculation.second
                        ) +
                        "Voltage Drop = %.3f %%".format(
                            calculation.first
                        )

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
                        ?: if (arabic) {
                            "تعذر تنفيذ الحساب."
                        } else {
                            "Calculation failed."
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
                    "نتيجة هبوط الجهد"
                } else {
                    "Voltage Drop Result"
                },
                value = it
            )
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                    message = if (arabic) {
                        "أدخل بيانات المغذي ثم اضغط حساب."
                    } else {
                        "Enter feeder data and calculate."
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

    var designCurrent by remember { mutableStateOf("100") }
    var cableAmpacity by remember { mutableStateOf("125") }
    var shortCircuit by remember { mutableStateOf("10") }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "اختيار القاطع" else "Breaker Selection",
        subtitle = if (arabic) {
            "اختيار القاطع والتحقق من التنسيق وقدرة القطع"
        } else {
            "Breaker rating and breaking-capacity verification"
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

                    require(current > 0.0)
                    require(ampacity > 0.0)
                    require(fault >= 0.0)

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

                    val catalog =
                        ElectricalCalculations.selectBreakerFromCatalog(
                            designCurrentA = current,
                            shortCircuitKA = fault,
                            standard = standard
                        )

                    result =
                        "Engineering Breaker = %.0f A\n".format(rating) +
                        "Coordination = %s\n".format(
                            if (coordination) "PASS" else "CHECK"
                        ) +
                        "Catalog = ${
                            catalog.selected?.model
                                ?: "NOT VERIFIED"
                        }"

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
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
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
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
            "Required transformer capacity and standard rating"
        },
        onBack = onBack
    ) {
        EngineeringCard {
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
                    val load = loadKw.toDouble()
                    val factor = pf.toDouble()
                    val growthFactor = growth.toDouble()

                    require(load > 0.0)
                    require(factor > 0.0 && factor <= 1.0)
                    require(growthFactor > 0.0)

                    val required =
                        ElectricalCalculations.calculateRequiredTransformerKva(
                            loadKw = load,
                            powerFactor = factor,
                            growthFactor = growthFactor
                        )

                    val selected =
                        ElectricalCalculations.selectTransformerRating(
                            requiredKva = required
                        )

                    val catalog =
                        ElectricalCalculations.selectTransformerFromCatalog(
                            requiredKva = required,
                            standard = standard
                        )

                    result =
                        "Required = %.1f kVA\n".format(required) +
                        "Standard Selection = %.0f kVA\n".format(selected) +
                        "Catalog = ${
                            catalog.selected?.model
                                ?: "NOT VERIFIED"
                        }"

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
                        ?: if (arabic) {
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
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
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

    var loadKw by remember { mutableStateOf("300") }
    var pf by remember { mutableStateOf("0.80") }
    var startingFactor by remember { mutableStateOf("1.50") }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "اختيار المولد" else "Generator Sizing",
        subtitle = if (arabic) {
            "حساب قدرة المولد واختيار كتالوج"
        } else {
            "Generator sizing and catalog selection"
        },
        onBack = onBack
    ) {
        EngineeringCard {
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
                label = "Starting Factor",
                value = startingFactor,
                onValueChange = {
                    startingFactor = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) "احسب المولد" else "Calculate Generator",
            onClick = {
                try {
                    val load = loadKw.toDouble()
                    val factor = pf.toDouble()
                    val starting = startingFactor.toDouble()

                    require(load > 0.0)
                    require(factor > 0.0 && factor <= 1.0)
                    require(starting > 0.0)

                    val required =
                        ElectricalCalculations.calculateKvaFromKw(
                            kw = load * starting,
                            powerFactor = factor
                        )

                    val catalog =
                        ElectricalCalculations.selectGeneratorFromCatalog(
                            requiredKva = required,
                            standard = standard
                        )

                    val selected =
                        catalog.selected?.ratedPowerKva

                    result =
                        if (selected != null) {
                            "Required = %.1f kVA\n".format(required) +
                            "Selected = %.1f kVA\n".format(selected) +
                            "Catalog = ${catalog.selected.model}"
                        } else {
                            "Required = %.1f kVA\n".format(required) +
                            "Selected = NOT VERIFIED\n" +
                            "Catalog = NOT VERIFIED"
                        }

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
                        ?: if (arabic) {
                            "تعذر حساب المولد."
                        } else {
                            "Generator calculation failed."
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
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                    message = if (arabic) {
                        "أدخل بيانات المولد ثم احسب القدرة المطلوبة."
                    } else {
                        "Enter generator data and calculate."
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

    var current by remember { mutableStateOf("400") }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "اختيار اللوحة" else "Panel Engineering",
        subtitle = if (arabic) {
            "اختيار القاطع الرئيسي للوحة"
        } else {
            "Panel main-breaker engineering"
        },
        onBack = onBack
    ) {
        EngineeringCard {
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
                    val designCurrent = current.toDouble()

                    require(designCurrent > 0.0)

                    val breaker =
                        ElectricalCalculations.selectBreakerRating(
                            designCurrentA = designCurrent,
                            cableAmpacityA = designCurrent * 1.25,
                            standard = standard
                        )

                    result =
                        "Panel In = %.1f A\n".format(designCurrent) +
                        "Main Breaker = %.0f A".format(breaker)

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
                        ?: if (arabic) {
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
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
                    message = if (arabic) {
                        "أدخل تيار اللوحة ثم اضغط اختيار اللوحة."
                    } else {
                        "Enter the panel current and run the selection."
                    }
                )
            }
        }

        if (onOpenSld != null) {
            EngineeringSecondaryButton(
                text = if (arabic) {
                    "فتح مصمم SLD"
                } else {
                    "Open SLD Designer"
                },
                onClick = onOpenSld
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
            "Prospective short-circuit current calculation"
        },
        onBack = onBack
    ) {
        EngineeringCard {
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
                    val v = voltage.toDouble()
                    val l = length.toDouble()
                    val s = section.toDouble()
                    val source = sourceIk.toDouble()

                    require(v > 0.0)
                    require(l >= 0.0)
                    require(s > 0.0)
                    require(source > 0.0)

                    val calculation =
                        ElectricalCalculations.calculateShortCircuitCurrent(
                            voltage = v,
                            length = l,
                            sectionMm2 = s,
                            material = material,
                            currentType =
                                CurrentType.AlternatingThreePhase,
                            sourceIkKA = source
                        )

                    result = calculation.toString()
                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
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
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
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
            "Breaker coordination and breaking-capacity checks"
        },
        onBack = onBack
    ) {
        EngineeringCard {
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
            text = if (arabic) "فحص الحماية" else "Validate Protection",
            onClick = {
                try {
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

                    result =
                        "Coordination = ${
                            if (coordination) "PASS" else "CHECK"
                        }\nBreaking Capacity = ${
                            if (breaking) "PASS" else "CHECK"
                        }"

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
                        ?: if (arabic) {
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
        } ?: run {
            if (error == null) {
                EngineeringEmptyState(
                    title = if (arabic) "لا توجد نتيجة بعد" else "No Result Yet",
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

/*
 * WATER
 *
 * The screen uses the hydraulic calculation engine instead of treating
 * friction/head/flow as independent values.
 *
 * Q + D -> V
 * Q + V -> D
 * D + V -> Q
 *
 * Head loss is calculated from the selected hydraulic geometry and
 * pipe length/material.
 */
@Composable
private fun WaterEngineeringModuleScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var project by remember {
        mutableStateOf(
            DesignProjectCoreBridge.getActiveProject()
        )
    }

    var flow by remember {
        mutableStateOf(
            project?.water?.requiredFlowM3PerHour
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: ""
        )
    }

    var diameter by remember { mutableStateOf("") }

    var velocity by remember { mutableStateOf("") }

    var length by remember {
        mutableStateOf(
            project?.water?.pipes
                ?.firstOrNull()
                ?.lengthM
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: "30"
        )
    }

    var material by remember {
        mutableStateOf(
            project?.water?.pipes
                ?.firstOrNull()
                ?.material
                ?.takeIf { it.isNotBlank() }
                ?: "PVC"
        )
    }

    var staticHead by remember {
        mutableStateOf(
            project?.water?.staticHeadM
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: ""
        )
    }

    var minorLoss by remember {
        mutableStateOf(
            project?.water?.minorLossHeadM
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: "0"
        )
    }

    var pressureHead by remember {
        mutableStateOf(
            project?.water?.requiredPressureHeadM
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: "0"
        )
    }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "تصميم المياه" else "Water Design",
        subtitle = if (arabic) {
            "حساب التصرف والسرعة والقطر والفواقد و TDH وربطها بالمشروع"
        } else {
            "Flow, velocity, diameter, losses, TDH and project integration"
        },
        onBack = onBack
    ) {
        EngineeringCard {
            EngineeringSectionTitle(
                text = if (arabic) "المدخلات الهيدروليكية" else "Hydraulic Inputs"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringInput(
                    value = flow,
                    label = if (arabic) "التصرف m³/h" else "Flow m³/h",
                    onValueChange = {
                        flow = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )

                EngineeringInput(
                    value = velocity,
                    label = if (arabic) "السرعة m/s" else "Velocity m/s",
                    onValueChange = {
                        velocity = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            EngineeringInput(
                value = diameter,
                label = if (arabic) {
                    "القطر mm — اتركه فارغًا للحساب"
                } else {
                    "Diameter mm — leave blank to calculate"
                },
                onValueChange = {
                    diameter = it
                    result = null
                    error = null
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringInput(
                    value = length,
                    label = if (arabic) "طول الخط m" else "Pipe Length m",
                    onValueChange = {
                        length = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )

                EngineeringInput(
                    value = material,
                    label = if (arabic) "الخامة" else "Material",
                    onValueChange = {
                        material = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringInput(
                    value = staticHead,
                    label = if (arabic) "الرأس الساكن m" else "Static Head m",
                    onValueChange = {
                        staticHead = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )

                EngineeringInput(
                    value = minorLoss,
                    label = if (arabic) "الفواقد الثانوية m" else "Minor Loss m",
                    onValueChange = {
                        minorLoss = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            EngineeringInput(
                value = pressureHead,
                label = if (arabic) {
                    "رأس الضغط المطلوب m"
                } else {
                    "Required Pressure Head m"
                },
                onValueChange = {
                    pressureHead = it
                    result = null
                    error = null
                }
            )
        }

        EngineeringPrimaryButton(
            text = if (arabic) {
                "احسب واحفظ في المشروع"
            } else {
                "Calculate & Save to Project"
            },
            onClick = {
                try {
                    val current =
                        project
                            ?: DesignProjectCoreBridge.getActiveProject()
                            ?: throw IllegalStateException(
                                if (arabic) {
                                    "لا يوجد مشروع نشط."
                                } else {
                                    "No active project."
                                }
                            )

                    val q = flow.toDoubleOrNull()
                    val d = diameter.toDoubleOrNull()
                    val v = velocity.toDoubleOrNull()
                    val l = length.toDoubleOrNull() ?: 0.0

                    require(l > 0.0) {
                        if (arabic) {
                            "طول خط المياه يجب أن يكون أكبر من صفر."
                        } else {
                            "Pipe length must be greater than zero."
                        }
                    }

                    require(q != null || d != null || v != null) {
                        if (arabic) {
                            "أدخل قيمًا هيدروليكية."
                        } else {
                            "Enter hydraulic values."
                        }
                    }

                    val calculatedFlow: Double
                    val calculatedDiameter: Double
                    val calculatedVelocity: Double

                    when {
                        q != null && q > 0.0 &&
                            d != null && d > 0.0 -> {

                            calculatedFlow = q
                            calculatedDiameter = d
                            calculatedVelocity =
                                WaterDesignEngine.calculateVelocity(
                                    flowM3PerHour = q,
                                    diameterMm = d
                                )
                        }

                        q != null && q > 0.0 &&
                            v != null && v > 0.0 -> {

                            calculatedFlow = q
                            calculatedVelocity = v

                            calculatedDiameter =
                                waterDiameterFromFlowAndVelocity(
                                    flowM3PerHour = q,
                                    velocityMPerS = v
                                )
                        }

                        d != null && d > 0.0 &&
                            v != null && v > 0.0 -> {

                            calculatedDiameter = d
                            calculatedVelocity = v

                            calculatedFlow =
                                waterFlowFromDiameterAndVelocity(
                                    diameterMm = d,
                                    velocityMPerS = v
                                )
                        }

                        q != null && q > 0.0 -> {
                            throw IllegalArgumentException(
                                if (arabic) {
                                    "أدخل القطر أو السرعة لحساب باقي البيانات."
                                } else {
                                    "Enter diameter or velocity to complete the hydraulic calculation."
                                }
                            )
                        }

                        else -> {
                            throw IllegalArgumentException(
                                if (arabic) {
                                    "أدخل أي قيمتين من التصرف والقطر والسرعة."
                                } else {
                                    "Enter any two of flow, diameter and velocity."
                                }
                            )
                        }
                    }

                    val calculatedFriction =
                        WaterDesignEngine.calculateFrictionLoss(
                            flowM3PerHour = calculatedFlow,
                            diameterMm = calculatedDiameter,
                            lengthM = l,
                            material = material
                        )

                    val calculatedTdh =
                        WaterDesignEngine.calculateTdh(
                            staticHeadM =
                                staticHead.toDoubleOrNull()
                                    ?: 0.0,
                            frictionHeadM = calculatedFriction,
                            minorLossHeadM =
                                minorLoss.toDoubleOrNull()
                                    ?: 0.0,
                            requiredPressureHeadM =
                                pressureHead.toDoubleOrNull()
                                    ?: 0.0
                        )

                    val pipe = WaterPipe(
                        name = "Main Water Line",
                        diameterMm = calculatedDiameter,
                        lengthM = l,
                        material = material,
                        flowM3PerHour = calculatedFlow,
                        velocityMPerS = calculatedVelocity,
                        frictionLossM = calculatedFriction
                    )

                    var updated =
                        WaterDesignModule.updateHydraulicDesign(
                            project = current,
                            flowM3PerHour = calculatedFlow,
                            staticHeadM =
                                staticHead.toDoubleOrNull()
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,
                            frictionHeadM = calculatedFriction,
                            minorLossHeadM =
                                minorLoss.toDoubleOrNull()
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,
                            requiredPressureHeadM =
                                pressureHead.toDoubleOrNull()
                                    ?.coerceAtLeast(0.0)
                                    ?: 0.0,
                            tdhM = calculatedTdh
                        )

                    updated =
                        WaterDesignModule.addPipe(
                            project = updated,
                            pipe = pipe
                        )

                    project = updated

                    flow = "%.3f".format(calculatedFlow)
                    diameter = "%.1f".format(calculatedDiameter)
                    velocity = "%.3f".format(calculatedVelocity)

                    result =
                        if (arabic) {
                            "التصرف = %.2f m³/h\n".format(
                                calculatedFlow
                            ) +
                            "القطر = %.1f mm\n".format(
                                calculatedDiameter
                            ) +
                            "السرعة = %.3f m/s\n".format(
                                calculatedVelocity
                            ) +
                            "فاقد الاحتكاك = %.2f m\n".format(
                                calculatedFriction
                            ) +
                            "TDH = %.2f m".format(
                                calculatedTdh
                            )
                        } else {
                            "Flow = %.2f m³/h\n".format(
                                calculatedFlow
                            ) +
                            "Diameter = %.1f mm\n".format(
                                calculatedDiameter
                            ) +
                            "Velocity = %.3f m/s\n".format(
                                calculatedVelocity
                            ) +
                            "Friction Loss = %.2f m\n".format(
                                calculatedFriction
                            ) +
                            "TDH = %.2f m".format(
                                calculatedTdh
                            )
                        }

                    error = null
                } catch (e: Exception) {
                    result = null
                    error = e.message
                        ?: if (arabic) {
                            "تعذر تنفيذ تصميم المياه."
                        } else {
                            "Water design calculation failed."
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
                    "نتيجة التصميم الهيدروليكي"
                } else {
                    "Hydraulic Design Result"
                },
                value = it
            )
        }

        project?.water?.let { water ->
            EngineeringResult(
                title = if (arabic) "حالة المشروع" else "Project Status",
                value = water.status.name,
                success = water.status.name != "INVALID"
            )

            EngineeringCard {
                EngineeringSectionTitle(
                    text = if (arabic) "نتائج المشروع" else "Project Results"
                )

                EngineeringValueRow(
                    label = if (arabic) "التصرف" else "Flow",
                    value = "%.2f m³/h".format(
                        water.requiredFlowM3PerHour
                    )
                )

                EngineeringValueRow(
                    label = if (arabic) "الرأس الساكن" else "Static Head",
                    value = "%.2f m".format(
                        water.staticHeadM
                    )
                )

                EngineeringValueRow(
                    label = if (arabic) "فاقد الاحتكاك" else "Friction Loss",
                    value = "%.2f m".format(
                        water.frictionHeadM
                    )
                )

                EngineeringValueRow(
                    label = if (arabic) "الفواقد الثانوية" else "Minor Losses",
                    value = "%.2f m".format(
                        water.minorLossHeadM
                    )
                )

                EngineeringValueRow(
                    label = if (arabic) "الرأس الكلي TDH" else "Total Dynamic Head",
                    value = "%.2f m".format(
                        water.tdhM
                    )
                )

                water.pipes.forEach { pipe ->
                    EngineeringValueRow(
                        label = if (arabic) {
                            "خط ${pipe.name}"
                        } else {
                            "Pipe ${pipe.name}"
                        },
                        value =
                            "%.1f mm | %.3f m/s | %.2f m".format(
                                pipe.diameterMm,
                                pipe.velocityMPerS,
                                pipe.frictionLossM
                            )
                    )
                }

                water.pumps.forEach { pump ->
                    EngineeringValueRow(
                        label = if (arabic) {
                            "المضخة ${pump.name}"
                        } else {
                            "Pump ${pump.name}"
                        },
                        value = "%.2f kW".format(
                            pump.motorPowerKw
                        )
                    )
                }
            }
        } ?: EngineeringEmptyState(
            title = if (arabic) "لا يوجد مشروع نشط" else "No Active Project",
            message = if (arabic) {
                "أنشئ أو اختر مشروعًا أولًا."
            } else {
                "Create or select a project first."
            }
        )
    }
}

/*
 * SEWAGE
 *
 * Sewage design is kept project-based:
 *
 * Average flow
 *      ↓
 * Peak flow
 *      ↓
 * Rising-main hydraulic calculation
 *      ↓
 * Velocity / Diameter / Friction
 *      ↓
 * TDH
 *      ↓
 * SewageDesignEngine / Pump calculation
 */
@Composable
private fun SewageEngineeringModuleScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    var project by remember {
        mutableStateOf(
            DesignProjectCoreBridge.getActiveProject()
        )
    }

    var average by remember {
        mutableStateOf(
            project?.sewage?.averageFlowM3PerDay
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: ""
        )
    }

    var peak by remember {
        mutableStateOf(
            project?.sewage?.peakFlowM3PerDay
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: ""
        )
    }

    var minimum by remember {
        mutableStateOf(
            project?.sewage?.minimumFlowM3PerDay
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: ""
        )
    }

    var diameter by remember { mutableStateOf("") }

    var velocity by remember { mutableStateOf("") }

    var length by remember {
        mutableStateOf(
            project?.sewage?.risingMain?.lengthM
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: "30"
        )
    }

    var material by remember {
        mutableStateOf(
            project?.sewage?.risingMain?.material
                ?.takeIf { it.isNotBlank() }
                ?: "PVC"
        )
    }

    var staticHead by remember {
        mutableStateOf(
            project?.sewage?.staticHeadM
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: ""
        )
    }

    var minorLoss by remember {
        mutableStateOf(
            project?.sewage?.risingMain?.minorLossHeadM
                ?.takeIf { it > 0.0 }
                ?.toString()
                ?: "0"
        )
    }

    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    EngineeringPage(
        title = if (arabic) "تصميم الصرف الصحي" else "Sewage Design",
        subtitle = if (arabic) {
            "التصرف والقطر والسرعة والفواقد و TDH وربطها بالمشروع"
        } else {
            "Flow, diameter, velocity, losses, TDH and project integration"
        },
        onBack = onBack
    ) {
        EngineeringCard {
            EngineeringSectionTitle(
                text = if (arabic) "بيانات التصرف" else "Flow Data"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringInput(
                    value = average,
                    label = if (arabic) {
                        "المتوسط m³/day"
                    } else {
                        "Average m³/day"
                    },
                    onValueChange = {
                        average = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )

                EngineeringInput(
                    value = peak,
                    label = if (arabic) {
                        "الأقصى m³/day"
                    } else {
                        "Peak m³/day"
                    },
                    onValueChange = {
                        peak = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            EngineeringInput(
                value = minimum,
                label = if (arabic) {
                    "الأدنى m³/day"
                } else {
                    "Minimum m³/day"
                },
                onValueChange = {
                    minimum = it
                    result = null
                    error = null
                }
            )

            EngineeringSectionTitle(
                text = if (arabic) {
                    "تصميم خط الطرد"
                } else {
                    "Rising Main Design"
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringInput(
                    value = diameter,
                    label = if (arabic) {
                        "القطر mm"
                    } else {
                        "Diameter mm"
                    },
                    onValueChange = {
                        diameter = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )

                EngineeringInput(
                    value = velocity,
                    label = if (arabic) {
                        "السرعة m/s"
                    } else {
                        "Velocity m/s"
                    },
                    onValueChange = {
                        velocity = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringInput(
                    value = length,
                    label = if (arabic) {
                        "طول الخط m"
                    } else {
                        "Main Length m"
                    },
                    onValueChange = {
                        length = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )

                EngineeringInput(
                    value = material,
                    label = if (arabic) {
                        "الخامة"
                    } else {
                        "Material"
                    },
                    onValueChange = {
                        material = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EngineeringInput(
                    value = staticHead,
                    label = if (arabic) {
                        "الرأس الساكن m"
                    } else {
                        "Static Head m"
                    },
                    onValueChange = {
                        staticHead = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )

                EngineeringInput(
                    value = minorLoss,
                    label = if (arabic) {
                        "الفواقد الثانوية m"
                    } else {
                        "Minor Loss m"
                    },
                    onValueChange = {
                        minorLoss = it
                        result = null
                        error = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            EngineeringPrimaryButton(
                text = if (arabic) {
                    "احسب واحفظ في المشروع"
                } else {
                    "Calculate & Save to Project"
                },
                onClick = {
                    try {
                        val current =
                            project
                                ?: DesignProjectCoreBridge
                                    .getActiveProject()
                                ?: throw IllegalStateException(
                                    if (arabic) {
                                        "لا يوجد مشروع نشط."
                                    } else {
                                        "No active project."
                                    }
                                )

                        val averageFlow =
                            average.toDoubleOrNull()
                                ?: throw IllegalArgumentException(
                                    if (arabic) {
                                        "أدخل متوسط التصرف."
                                    } else {
                                        "Enter average flow."
                                    }
                                )

                        require(averageFlow > 0.0)

                        val enteredPeak =
                            peak.toDoubleOrNull()

                        val calculatedPeak =
                            if (enteredPeak != null &&
                                enteredPeak > 0.0
                            ) {
                                enteredPeak
                            } else {
                                val factor =
                                    SewageDesignEngine.calculatePeakFactor(
                                        averageFlowM3PerDay =
                                            averageFlow,
                                        peakFlowM3PerDay =
                                            null
                                    )

                                SewageDesignEngine.calculatePeakFlow(
                                    averageFlowM3PerDay =
                                        averageFlow,
                                    peakFactor = factor
                                )
                            }

                        require(calculatedPeak >= averageFlow) {
                            if (arabic) {
                                "التصرف الأقصى يجب ألا يقل عن المتوسط."
                            } else {
                                "Peak flow must not be lower than average flow."
                            }
                        }

                        val peakHourly =
                            calculatedPeak / 24.0

                        val enteredVelocity =
                            velocity.toDoubleOrNull()

                        val enteredDiameter =
                            diameter.toDoubleOrNull()

                        val calculatedVelocity: Double
                        val calculatedDiameter: Double

                        when {
                            enteredDiameter != null &&
                                enteredDiameter > 0.0 -> {

                                calculatedDiameter =
                                    enteredDiameter

                                calculatedVelocity =
                                    SewageDesignEngine.calculateVelocity(
                                        flowM3PerHour =
                                            peakHourly,
                                        diameterMm =
                                            calculatedDiameter
                                    )
                            }

                            enteredVelocity != null &&
                                enteredVelocity > 0.0 -> {

                                calculatedVelocity =
                                    enteredVelocity

                                calculatedDiameter =
                                    sewageDiameterFromFlowAndVelocity(
                                        flowM3PerHour =
                                            peakHourly,
                                        velocityMPerS =
                                            calculatedVelocity
                                    )
                            }

                            else -> {
                                throw IllegalArgumentException(
                                    if (arabic) {
                                        "أدخل القطر أو السرعة لخط الطرد."
                                    } else {
                                        "Enter rising-main diameter or velocity."
                                    }
                                )
                            }
                        }

                        val mainLength =
                            length.toDoubleOrNull()
                                ?: throw IllegalArgumentException(
                                    if (arabic) {
                                        "أدخل طول خط الطرد."
                                    } else {
                                        "Enter rising-main length."
                                    }
                                )

                        require(mainLength > 0.0)

                        val calculatedFriction =
                            SewageDesignEngine.calculateFrictionLoss(
                                flowM3PerHour =
                                    peakHourly,
                                diameterMm =
                                    calculatedDiameter,
                                lengthM =
                                    mainLength,
                                material =
                                    material
                            )

                        val calculatedMinor =
                            minorLoss.toDoubleOrNull()
                                ?.coerceAtLeast(0.0)
                                ?: 0.0

                        val calculatedTdh =
                            SewageDesignEngine.calculateTdh(
                                staticHeadM =
                                    staticHead.toDoubleOrNull()
                                        ?.coerceAtLeast(0.0)
                                        ?: 0.0,
                                frictionLossM =
                                    calculatedFriction,
                                minorLossHeadM =
                                    calculatedMinor
                            )

                        val risingMain =
                            RisingMainDesign(
                                name = "Main Rising Main",
                                diameterMm =
                                    calculatedDiameter,
                                lengthM =
                                    mainLength,
                                material =
                                    material,
                                flowM3PerHour =
                                    peakHourly,
                                velocityMPerS =
                                    calculatedVelocity,
                                frictionLossM =
                                    calculatedFriction,
                                minorLossHeadM =
                                    calculatedMinor
                            )

                        var updated =
                            SewageDesignModule.updateFlows(
                                project = current,
                                averageFlowM3PerDay =
                                    averageFlow,
                                peakFlowM3PerDay =
                                    calculatedPeak,
                                minimumFlowM3PerDay =
                                    minimum.toDoubleOrNull()
                                        ?.coerceAtLeast(0.0)
                                        ?: 0.0
                            )

                        updated =
                            SewageDesignModule.setStaticHead(
                                project = updated,
                                staticHeadM =
                                    staticHead.toDoubleOrNull()
                                        ?.coerceAtLeast(0.0)
                                        ?: 0.0
                            )

                        updated =
                            SewageDesignModule.setRisingMain(
                                project = updated,
                                risingMain = risingMain
                            )

                        project = updated

                        peak =
                            "%.2f".format(
                                calculatedPeak
                            )

                        diameter =
                            "%.1f".format(
                                calculatedDiameter
                            )

                        velocity =
                            "%.3f".format(
                                calculatedVelocity
                            )

                        result =
                            if (arabic) {
                                "المتوسط = %.2f m³/day\n".format(
                                    averageFlow
                                ) +
                                "الأقصى = %.2f m³/day\n".format(
                                    calculatedPeak
                                ) +
                                "تصرف خط الطرد = %.2f m³/h\n".format(
                                    peakHourly
                                ) +
                                "القطر = %.1f mm\n".format(
                                    calculatedDiameter
                                ) +
                                "السرعة = %.3f m/s\n".format(
                                    calculatedVelocity
                                ) +
                                "فاقد الاحتكاك = %.2f m\n".format(
                                    calculatedFriction
                                ) +
                                "TDH = %.2f m".format(
                                    calculatedTdh
                                )
                            } else {
                                "Average = %.2f m³/day\n".format(
                                    averageFlow
                                ) +
                                "Peak = %.2f m³/day\n".format(
                                    calculatedPeak
                                ) +
                                "Rising Main Flow = %.2f m³/h\n".format(
                                    peakHourly
                                ) +
                                "Diameter = %.1f mm\n".format(
                                    calculatedDiameter
                                ) +
                                "Velocity = %.3f m/s\n".format(
                                    calculatedVelocity
                                ) +
                                "Friction Loss = %.2f m\n".format(
                                    calculatedFriction
                                ) +
                                "TDH = %.2f m".format(
                                    calculatedTdh
                                )
                            }

                        error = null
                    } catch (e: Exception) {
                        result = null
                        error = e.message
                            ?: if (arabic) {
                                "تعذر تنفيذ تصميم الصرف الصحي."
                            } else {
                                "Sewage design calculation failed."
                            }
                    }
                }
            )
        }

        error?.let {
            EngineeringStatus(
                text = it,
                success = false
            )
        }

        result?.let {
            EngineeringResult(
                title = if (arabic) {
                    "نتيجة التصميم الهيدروليكي"
                } else {
                    "Hydraulic Design Result"
                },
                value = it
            )
        }

        project?.sewage?.let { sewage ->
            EngineeringResult(
                title = if (arabic) "حالة المشروع" else "Project Status",
                value = sewage.status.name,
                success = sewage.status.name != "INVALID"
            )

            EngineeringCard {
                EngineeringSectionTitle(
                    text = if (arabic) {
                        "نتائج مشروع الصرف"
                    } else {
                        "Sewage Project Results"
                    }
                )

                EngineeringValueRow(
                    label = if (arabic) {
                        "التصرف المتوسط"
                    } else {
                        "Average Flow"
                    },
                    value = "%.2f m³/day".format(
                        sewage.averageFlowM3PerDay
                    )
                )

                EngineeringValueRow(
                    label = if (arabic) {
                        "التصرف الأقصى"
                    } else {
                        "Peak Flow"
                    },
                    value = "%.2f m³/day".format(
                        sewage.peakFlowM3PerDay
                    )
                )

                EngineeringValueRow(
                    label = if (arabic) {
                        "التصرف الأقصى بالساعة"
                    } else {
                        "Peak Hourly Flow"
                    },
                    value = "%.2f m³/h".format(
                        sewage.peakFlowM3PerDay / 24.0
                    )
                )

                EngineeringValueRow(
                    label = if (arabic) {
                        "الرأس الساكن"
                    } else {
                        "Static Head"
                    },
                    value = "%.2f m".format(
                        sewage.staticHeadM
                    )
                )

                EngineeringValueRow(
                    label = if (arabic) {
                        "الرأس الكلي TDH"
                    } else {
                        "Total Dynamic Head"
                    },
                    value = "%.2f m".format(
                        sewage.tdhM
                    )
                )

                sewage.wetWell?.let { wetWell ->
                    EngineeringValueRow(
                        label = if (arabic) {
                            "حجم الحوض الرطب"
                        } else {
                            "Wet Well Volume"
                        },
                        value = "%.2f m³".format(
                            wetWell.operatingVolumeM3
                        )
                    )
                }

                sewage.risingMain?.let { risingMain ->
                    EngineeringValueRow(
                        label = if (arabic) {
                            "قطر خط الطرد"
                        } else {
                            "Rising Main Diameter"
                        },
                        value = "%.1f mm".format(
                            risingMain.diameterMm
                        )
                    )

                    EngineeringValueRow(
                        label = if (arabic) {
                            "سرعة خط الطرد"
                        } else {
                            "Rising Main Velocity"
                        },
                        value = "%.3f m/s".format(
                            risingMain.velocityMPerS
                        )
                    )

                    EngineeringValueRow(
                        label = if (arabic) {
                            "فاقد الاحتكاك"
                        } else {
                            "Friction Loss"
                        },
                        value = "%.2f m".format(
                            risingMain.frictionLossM
                        )
                    )

                    EngineeringValueRow(
                        label = if (arabic) {
                            "الفواقد الثانوية"
                        } else {
                            "Minor Loss"
                        },
                        value = "%.2f m".format(
                            risingMain.minorLossHeadM
                        )
                    )
                }

                sewage.pumps.forEach { pump ->
                    EngineeringValueRow(
                        label = if (arabic) {
                            "المضخة ${pump.name}"
                        } else {
                            "Pump ${pump.name}"
                        },
                        value = "%.2f kW".format(
                            pump.motorPowerKw
                        )
                    )

                    EngineeringValueRow(
                        label = if (arabic) {
                            "الطاقة السنوية ${pump.name}"
                        } else {
                            "Yearly Energy ${pump.name}"
                        },
                        value = "%.0f kWh".format(
                            pump.yearlyEnergyKwh
                        )
                    )
                }
            }
        } ?: EngineeringEmptyState(
            title = if (arabic) "لا يوجد مشروع نشط" else "No Active Project",
            message = if (arabic) {
                "أنشئ أو اختر مشروعًا أولًا."
            } else {
                "Create or select a project first."
            }
        )
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
                value = "Electrical / Water / Sewage"
            )

            EngineeringValueRow(
                label = "SLD",
                value = "Interactive Engineering Diagram"
            )

            EngineeringValueRow(
                label = "PDF",
                value = "Engineering Report"
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
        text = when (value) {
            ConductorMaterial.Copper ->
                if (arabic) {
                    "الموصل: نحاس"
                } else {
                    "Conductor: Copper"
                }

            ConductorMaterial.Aluminum ->
                if (arabic) {
                    "الموصل: ألومنيوم"
                } else {
                    "Conductor: Aluminum"
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
        listOf(
            ConductorMaterial.Copper,
            ConductorMaterial.Aluminum
        ).forEach { material ->
            DropdownMenuItem(
                text = {
                    Text(
                        when (material) {
                            ConductorMaterial.Copper ->
                                if (arabic) {
                                    "نحاس"
                                } else {
                                    "Copper"
                                }

                            ConductorMaterial.Aluminum ->
                                if (arabic) {
                                    "ألومنيوم"
                                } else {
                                    "Aluminum"
                                }
                        }
                    )
                },
                onClick = {
                    onChange(material)
                    expanded = false
                }
            )
        }
    }
}

/*
 * Hydraulic helper calculations.
 *
 * These helpers are intentionally isolated from the composables so the
 * UI does not contain scattered hydraulic equations.
 *
 * Q is m³/h.
 * V is m/s.
 * D is mm.
 */
private fun waterDiameterFromFlowAndVelocity(
    flowM3PerHour: Double,
    velocityMPerS: Double
): Double {
    require(flowM3PerHour > 0.0)
    require(velocityMPerS > 0.0)

    val flowM3PerSecond =
        flowM3PerHour / 3600.0

    val area =
        flowM3PerSecond / velocityMPerS

    val diameterM =
        kotlin.math.sqrt(
            4.0 * area / kotlin.math.PI
        )

    return diameterM * 1000.0
}

private fun waterFlowFromDiameterAndVelocity(
    diameterMm: Double,
    velocityMPerS: Double
): Double {
    require(diameterMm > 0.0)
    require(velocityMPerS > 0.0)

    val diameterM =
        diameterMm / 1000.0

    val area =
        kotlin.math.PI *
            diameterM *
            diameterM /
            4.0

    val flowM3PerSecond =
        area * velocityMPerS

    return flowM3PerSecond * 3600.0
}

private fun sewageDiameterFromFlowAndVelocity(
    flowM3PerHour: Double,
    velocityMPerS: Double
): Double {
    require(flowM3PerHour > 0.0)
    require(velocityMPerS > 0.0)

    val flowM3PerSecond =
        flowM3PerHour / 3600.0

    val area =
        flowM3PerSecond / velocityMPerS

    val diameterM =
        kotlin.math.sqrt(
            4.0 * area / kotlin.math.PI
        )

    return diameterM * 1000.0
}
