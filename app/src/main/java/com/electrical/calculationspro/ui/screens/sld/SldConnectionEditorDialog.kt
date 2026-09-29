package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.ui.components.EngineeringCard
import com.electrical.calculationspro.ui.components.EngineeringInput

@Composable
fun SldConnectionEditorDialog(
    arabic: Boolean,
    connectionType: String,
    conductorMaterial: String,
    insulationType: String,
    installationMethodCode: String,
    busbarMaterial: String,
    busbarRatedCurrent: String,
    busbarShortCircuit: String,
    length: String,
    resistance: String,
    reactance: String,
    cableSize: String,
    parallelRuns: String,
    capacity: String,
    onConnectionTypeChange: (String) -> Unit,
    onConductorMaterialChange: (String) -> Unit,
    onInsulationTypeChange: (String) -> Unit,
    onInstallationMethodChange: (String) -> Unit,
    onBusbarMaterialChange: (String) -> Unit,
    onBusbarRatedCurrentChange: (String) -> Unit,
    onBusbarShortCircuitChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onResistanceChange: (String) -> Unit,
    onReactanceChange: (String) -> Unit,
    onCableSizeChange: (String) -> Unit,
    onParallelRunsChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val isBusbar = connectionType.equals(
        "BUSBAR",
        ignoreCase = true
    )

    AlertDialog(
        onDismissRequest = onCancel,

        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (arabic) {
                        "بيانات التوصيل"
                    } else {
                        "Connection Data"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isBusbar) {
                        if (arabic) {
                            "توصيل داخلي داخل اللوحة"
                        } else {
                            "Internal panel busbar connection"
                        }
                    } else {
                        if (arabic) {
                            "تغذية خارجية بواسطة كابل"
                        } else {
                            "External feeder cable connection"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },

        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                EngineeringCard(
                    title = if (arabic) {
                        "نوع التوصيل"
                    } else {
                        "Connection Type"
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onConnectionTypeChange("CABLE")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CABLE")
                        }

                        OutlinedButton(
                            onClick = {
                                onConnectionTypeChange("BUSBAR")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("BUSBAR")
                        }
                    }

                    Text(
                        text = if (isBusbar) {
                            if (arabic) {
                                "الوضع الحالي: BUSBAR — لا يتم إدخال طول كابل أو معاملات كابل."
                            } else {
                                "Current type: BUSBAR — cable length and cable parameters are not used."
                            }
                        } else {
                            if (arabic) {
                                "الوضع الحالي: CABLE — بيانات الكابل مطلوبة للتغذية الخارجية."
                            } else {
                                "Current type: CABLE — cable data is required for the external feeder."
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isBusbar) {

                    EngineeringCard(
                        title = if (arabic) {
                            "بيانات الباسبار"
                        } else {
                            "Busbar Data"
                        }
                    ) {

                        Text(
                            text = if (arabic) {
                                "الباسبار جزء داخلي من اللوحة. لا يتم تطبيق طول أو R أو X أو طريقة تنفيذ كابل عليه."
                            } else {
                                "The busbar is an internal panel element. Cable length, R, X and installation method are not applicable."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                val nextMaterial =
                                    if (
                                        busbarMaterial.equals(
                                            "Copper",
                                            ignoreCase = true
                                        )
                                    ) {
                                        "Aluminium"
                                    } else {
                                        "Copper"
                                    }

                                onBusbarMaterialChange(
                                    nextMaterial
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (arabic) {
                                    "مادة الباسبار: $busbarMaterial"
                                } else {
                                    "Busbar Material: $busbarMaterial"
                                }
                            )
                        }

                        EngineeringInput(
                            value = busbarRatedCurrent,
                            label = if (arabic) {
                                "تيار الباسبار (A)"
                            } else {
                                "Busbar Rated Current (A)"
                            },
                            onValueChange = onBusbarRatedCurrentChange
                        )

                        EngineeringInput(
                            value = busbarShortCircuit,
                            label = if (arabic) {
                                "تحمل القصر (kA)"
                            } else {
                                "Busbar Short-Circuit Rating (kA)"
                            },
                            onValueChange = onBusbarShortCircuitChange
                        )
                    }

                } else {

                    EngineeringCard(
                        title = if (arabic) {
                            "بيانات الكابل"
                        } else {
                            "Cable Data"
                        }
                    ) {

                        EngineeringInput(
                            value = length,
                            label = if (arabic) {
                                "طول الكابل (m)"
                            } else {
                                "Cable Length (m)"
                            },
                            onValueChange = onLengthChange
                        )

                        OutlinedButton(
                            onClick = {
                                val nextMaterial =
                                    if (
                                        conductorMaterial.equals(
                                            "Copper",
                                            ignoreCase = true
                                        )
                                    ) {
                                        "Aluminium"
                                    } else {
                                        "Copper"
                                    }

                                onConductorMaterialChange(
                                    nextMaterial
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (arabic) {
                                    "الموصل: $conductorMaterial"
                                } else {
                                    "Conductor: $conductorMaterial"
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val nextInsulation =
                                    if (
                                        insulationType.equals(
                                            "PVC",
                                            ignoreCase = true
                                        )
                                    ) {
                                        "XLPE"
                                    } else {
                                        "PVC"
                                    }

                                onInsulationTypeChange(
                                    nextInsulation
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (arabic) {
                                    "العزل: $insulationType"
                                } else {
                                    "Insulation: $insulationType"
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onInstallationMethodChange(
                                    nextInstallationMethod(
                                        installationMethodCode
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (arabic) {
                                    "طريقة التنفيذ: $installationMethodCode"
                                } else {
                                    "Installation: $installationMethodCode"
                                }
                            )
                        }

                        EngineeringInput(
                            value = cableSize,
                            label = if (arabic) {
                                "مقطع الكابل (mm²)"
                            } else {
                                "Cable Section (mm²)"
                            },
                            onValueChange = onCableSizeChange
                        )

                        EngineeringInput(
                            value = parallelRuns,
                            label = if (arabic) {
                                "عدد المسارات المتوازية"
                            } else {
                                "Parallel Runs"
                            },
                            onValueChange = onParallelRunsChange
                        )
                    }

                    EngineeringCard(
                        title = if (arabic) {
                            "المعاملات الكهربائية"
                        } else {
                            "Electrical Parameters"
                        }
                    ) {

                        EngineeringInput(
                            value = resistance,
                            label = "R (Ω/km)",
                            onValueChange = onResistanceChange
                        )

                        EngineeringInput(
                            value = reactance,
                            label = "X (Ω/km)",
                            onValueChange = onReactanceChange
                        )

                        EngineeringInput(
                            value = capacity,
                            label = if (arabic) {
                                "التيار المسموح (A)"
                            } else {
                                "Current Capacity (A)"
                            },
                            onValueChange = onCapacityChange
                        )
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    text = if (arabic) {
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
                    text = if (arabic) {
                        "إلغاء"
                    } else {
                        "Cancel"
                    }
                )
            }
        }
    )
}

private fun nextInstallationMethod(
    current: String
): String {
    val methods = listOf(
        "A1",
        "A2",
        "B1",
        "B2",
        "C",
        "D1",
        "D2"
    )

    val index = methods.indexOf(current)

    return if (
        index < 0 ||
        index >= methods.lastIndex
    ) {
        methods.first()
    } else {
        methods[index + 1]
    }
}
