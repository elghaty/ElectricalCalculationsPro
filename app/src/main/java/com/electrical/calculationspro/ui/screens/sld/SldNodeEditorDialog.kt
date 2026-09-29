package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.SldNodeType
import com.electrical.calculationspro.ui.components.EngineeringCard
import com.electrical.calculationspro.ui.components.EngineeringInput

@Composable
fun SldNodeEditorDialog(
    arabic: Boolean,
    editing: Boolean,
    type: SldNodeType,
    name: String,
    voltage: String,
    loadKw: String,
    pf: String,
    demand: String,
    kva: String,
    transformerZ: String,
    generatorXd: String,
    sourceMva: String,
    onNameChange: (String) -> Unit,
    onVoltageChange: (String) -> Unit,
    onLoadKwChange: (String) -> Unit,
    onPfChange: (String) -> Unit,
    onDemandChange: (String) -> Unit,
    onKvaChange: (String) -> Unit,
    onTransformerZChange: (String) -> Unit,
    onGeneratorXdChange: (String) -> Unit,
    onSourceMvaChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,

        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (editing) {
                        if (arabic) {
                            "تعديل العنصر"
                        } else {
                            "Edit Component"
                        }
                    } else {
                        if (arabic) {
                            "إضافة عنصر"
                        } else {
                            "Add Component"
                        }
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = sldNodeTypeLabel(
                        type = type,
                        arabic = arabic
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },

        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                EngineeringCard(
                    title = if (arabic) {
                        "بيانات العنصر"
                    } else {
                        "Component Data"
                    }
                ) {
                    EngineeringInput(
                        value = name,
                        label = if (arabic) {
                            "اسم العنصر"
                        } else {
                            "Component Name"
                        },
                        onValueChange = onNameChange,
                        isNumeric = false
                    )

                    EngineeringInput(
                        value = voltage,
                        label = if (arabic) {
                            "الجهد (V)"
                        } else {
                            "Voltage (V)"
                        },
                        onValueChange = onVoltageChange
                    )
                }

                when (type) {

                    SldNodeType.SOURCE -> {
                        EngineeringCard(
                            title = if (arabic) {
                                "بيانات المصدر"
                            } else {
                                "Source Data"
                            }
                        ) {
                            EngineeringInput(
                                value = sourceMva,
                                label = if (arabic) {
                                    "قدرة القصر للمصدر (MVA)"
                                } else {
                                    "Source Fault Level (MVA)"
                                },
                                onValueChange = onSourceMvaChange
                            )
                        }
                    }

                    SldNodeType.TRANSFORMER -> {
                        EngineeringCard(
                            title = if (arabic) {
                                "بيانات المحول"
                            } else {
                                "Transformer Data"
                            }
                        ) {
                            EngineeringInput(
                                value = kva,
                                label = if (arabic) {
                                    "قدرة المحول (kVA)"
                                } else {
                                    "Transformer Rating (kVA)"
                                },
                                onValueChange = onKvaChange
                            )

                            EngineeringInput(
                                value = transformerZ,
                                label = if (arabic) {
                                    "ممانعة المحول (%Z)"
                                } else {
                                    "Transformer Impedance (%Z)"
                                },
                                onValueChange = onTransformerZChange
                            )
                        }
                    }

                    SldNodeType.GENERATOR -> {
                        EngineeringCard(
                            title = if (arabic) {
                                "بيانات المولد"
                            } else {
                                "Generator Data"
                            }
                        ) {
                            EngineeringInput(
                                value = kva,
                                label = if (arabic) {
                                    "قدرة المولد (kVA)"
                                } else {
                                    "Generator Rating (kVA)"
                                },
                                onValueChange = onKvaChange
                            )

                            EngineeringInput(
                                value = generatorXd,
                                label = if (arabic) {
                                    "المفاعلة العابرة Xd'' (%)"
                                } else {
                                    "Subtransient Reactance Xd'' (%)"
                                },
                                onValueChange = onGeneratorXdChange
                            )
                        }
                    }

                    SldNodeType.PANEL -> {
                        EngineeringCard(
                            title = if (arabic) {
                                "بيانات اللوحة"
                            } else {
                                "Panel Data"
                            }
                        ) {
                            EngineeringInput(
                                value = kva,
                                label = if (arabic) {
                                    "قدرة اللوحة (kVA)"
                                } else {
                                    "Panel Rating (kVA)"
                                },
                                onValueChange = onKvaChange
                            )
                        }
                    }

                    SldNodeType.LOAD -> {
                        EngineeringCard(
                            title = if (arabic) {
                                "بيانات الحمل"
                            } else {
                                "Load Data"
                            }
                        ) {
                            EngineeringInput(
                                value = loadKw,
                                label = if (arabic) {
                                    "الحمل (kW)"
                                } else {
                                    "Load (kW)"
                                },
                                onValueChange = onLoadKwChange
                            )

                            EngineeringInput(
                                value = pf,
                                label = if (arabic) {
                                    "معامل القدرة"
                                } else {
                                    "Power Factor"
                                },
                                onValueChange = onPfChange
                            )

                            EngineeringInput(
                                value = demand,
                                label = if (arabic) {
                                    "معامل الطلب"
                                } else {
                                    "Demand Factor"
                                },
                                onValueChange = onDemandChange
                            )
                        }
                    }

                    SldNodeType.BUS,
                    SldNodeType.BREAKER -> {
                        EngineeringCard(
                            title = if (arabic) {
                                "معلومات العنصر"
                            } else {
                                "Component Information"
                            }
                        ) {
                            Text(
                                text = if (arabic) {
                                    when (type) {
                                        SldNodeType.BUS ->
                                            "الباسبار عنصر تجميعي داخل المخطط ولا يحتاج بيانات حمل مباشرة."

                                        SldNodeType.BREAKER ->
                                            "بيانات القاطع الهندسية يتم تحديدها من خلال دراسة الدائرة والـUpstream."

                                        else ->
                                            ""
                                    }
                                } else {
                                    when (type) {
                                        SldNodeType.BUS ->
                                            "The busbar is an aggregation element and does not require direct load data."

                                        SldNodeType.BREAKER ->
                                            "Breaker engineering data is derived from circuit and upstream studies."

                                        else ->
                                            ""
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    if (arabic) {
                        "حفظ"
                    } else {
                        "Save"
                    }
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    if (arabic) {
                        "إلغاء"
                    } else {
                        "Cancel"
                    }
                )
            }
        }
    )
}

private fun sldNodeTypeLabel(
    type: SldNodeType,
    arabic: Boolean
): String {
    return when (type) {
        SldNodeType.SOURCE ->
            if (arabic) "مصدر كهرباء" else "Utility Source"

        SldNodeType.TRANSFORMER ->
            if (arabic) "محول" else "Transformer"

        SldNodeType.GENERATOR ->
            if (arabic) "مولد" else "Generator"

        SldNodeType.BUS ->
            if (arabic) "باسبار" else "Busbar"

        SldNodeType.PANEL ->
            if (arabic) "لوحة" else "Panel"

        SldNodeType.BREAKER ->
            if (arabic) "قاطع" else "Breaker"

        SldNodeType.LOAD ->
            if (arabic) "حمل" else "Load"
    }
}
