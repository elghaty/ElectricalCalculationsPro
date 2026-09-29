package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var typeExpanded by remember { mutableStateOf(false) }
    var conductorExpanded by remember { mutableStateOf(false) }
    var insulationExpanded by remember { mutableStateOf(false) }
    var installationExpanded by remember { mutableStateOf(false) }
    var busbarMaterialExpanded by remember { mutableStateOf(false) }

    val isBusbar = connectionType == "BUSBAR"

    AlertDialog(
        onDismissRequest = onCancel,

        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (arabic) {
                        "بيانات التوصيل"
                    } else {
                        "Connection Data"
                    },
                    style = MaterialTheme.typography.titleLarge
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
                    .verticalScroll(rememberScrollState()),
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
                                typeExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isBusbar) {
                                    "BUSBAR"
                                } else {
                                    "CABLE"
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = {
                                typeExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("BUSBAR")
                                },
                                onClick = {
                                    onConnectionTypeChange("BUSBAR")
                                    typeExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("CABLE")
                                },
                                onClick = {
                                    onConnectionTypeChange("CABLE")
                                    typeExpanded = false
                                }
                            )
                        }
                    }
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
                                "توصيل داخلي داخل اللوحة. لا يتم استخدام طول كابل أو طريقة تنفيذ."
                            } else {
                                "Internal panel connection. Cable length and installation method are not applicable."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                busbarMaterialExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (arabic) {
                                    "مادة الباسبار: $busbarMaterial"
                                } else {
                                    "Busbar Material: $busbarMaterial"
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = busbarMaterialExpanded,
                            onDismissRequest = {
                                busbarMaterialExpanded = false
                            }
                        ) {
                            listOf(
                                "Copper",
                                "Aluminium"
                            ).forEach { material ->
                                DropdownMenuItem(
                                    text = {
                                        Text(material)
                                    },
                                    onClick = {
                                        onBusbarMaterialChange(material)
                                        busbarMaterialExpanded = false
                                    }
                                )
                            }
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
                                conductorExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (arabic) {
                                    "الموصل: $conductorMaterial"
                                } else {
                                    "Conductor: $conductorMaterial"
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = conductorExpanded,
                            onDismissRequest = {
                                conductorExpanded = false
                            }
                        ) {
                            listOf(
                                "Copper",
                                "Aluminium"
                            ).forEach { material ->
                                DropdownMenuItem(
                                    text = {
                                        Text(material)
                                    },
                                    onClick = {
                                        onConductorMaterialChange(material)
                                        conductorExpanded = false
                                    }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                insulationExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (arabic) {
                                    "العزل: $insulationType"
                                } else {
                                    "Insulation: $insulationType"
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = insulationExpanded,
                            onDismissRequest = {
                                insulationExpanded = false
                            }
                        ) {
                            listOf(
                                "PVC",
                                "XLPE"
                            ).forEach { insulation ->
                                DropdownMenuItem(
                                    text = {
                                        Text(insulation)
                                    },
                                    onClick = {
                                        onInsulationTypeChange(insulation)
                                        insulationExpanded = false
                                    }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                installationExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (arabic) {
                                    "طريقة التنفيذ: $installationMethodCode"
                                } else {
                                    "Installation: $installationMethodCode"
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = installationExpanded,
                            onDismissRequest = {
                                installationExpanded = false
                            }
                        ) {
                            listOf(
                                "A1",
                                "A2",
                                "B1",
                                "B2",
                                "C",
                                "D1",
                                "D2"
                            ).forEach { method ->
                                DropdownMenuItem(
                                    text = {
                                        Text(method)
                                    },
                                    onClick = {
                                        onInstallationMethodChange(method)
                                        installationExpanded = false
                                    }
                                )
                            }
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
