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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

val isBusbar =
    connectionType == "BUSBAR"

AlertDialog(
    onDismissRequest = onCancel,
    title = {
        Text(
            if (arabic) {
                "بيانات التوصيل"
            } else {
                "Connection Data"
            }
        )
    },
    text = {
        Column(
            modifier =
                Modifier.verticalScroll(
                    rememberScrollState()
                ),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                if (arabic) {
                    "نوع التوصيل"
                } else {
                    "Connection Type"
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        typeExpanded = true
                    }
                ) {
                    Text(
                        if (isBusbar) {
                            "BUSBAR"
                        } else {
                            "CABLE"
                        }
                    )
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

            if (isBusbar) {

                Text(
                    if (arabic) {
                        "توصيل داخلي داخل اللوحة بواسطة باسبار"
                    } else {
                        "Internal panel connection by busbar"
                    }
                )

                OutlinedButton(
                    onClick = {
                        busbarMaterialExpanded = true
                    }
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

                OutlinedTextField(
                    value = busbarRatedCurrent,
                    onValueChange =
                        onBusbarRatedCurrentChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) {
                                "تيار الباسبار (A)"
                            } else {
                                "Busbar Rated Current (A)"
                            }
                        )
                    }
                )

                OutlinedTextField(
                    value = busbarShortCircuit,
                    onValueChange =
                        onBusbarShortCircuitChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) {
                                "تحمل القصر (kA)"
                            } else {
                                "Busbar Short-Circuit Rating (kA)"
                            }
                        )
                    }
                )

                Text(
                    if (arabic) {
                        "لا يوجد طول كابل أو طريقة تنفيذ للباسبار."
                    } else {
                        "Cable length and installation method are not used for busbar connections."
                    }
                )

            } else {

                Text(
                    if (arabic) {
                        "توصيل خارجي بواسطة كابل"
                    } else {
                        "External feeder connection by cable"
                    }
                )

                OutlinedTextField(
                    value = length,
                    onValueChange = onLengthChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) {
                                "طول الكابل (m)"
                            } else {
                                "Cable Length (m)"
                            }
                        )
                    }
                )

                OutlinedButton(
                    onClick = {
                        conductorExpanded = true
                    }
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
                    }
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
                    }
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

                OutlinedTextField(
                    value = cableSize,
                    onValueChange = onCableSizeChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) {
                                "مقطع الكابل (mm²)"
                            } else {
                                "Cable Section (mm²)"
                            }
                        )
                    }
                )

                OutlinedTextField(
                    value = parallelRuns,
                    onValueChange = onParallelRunsChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) {
                                "عدد المسارات المتوازية"
                            } else {
                                "Parallel Runs"
                            }
                        )
                    }
                )

                OutlinedTextField(
                    value = resistance,
                    onValueChange = onResistanceChange,
                    singleLine = true,
                    label = {
                        Text("R (Ω/km)")
                    }
                )

                OutlinedTextField(
                    value = reactance,
                    onValueChange = onReactanceChange,
                    singleLine = true,
                    label = {
                        Text("X (Ω/km)")
                    }
                )

                OutlinedTextField(
                    value = capacity,
                    onValueChange = onCapacityChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) {
                                "التيار المسموح (A)"
                            } else {
                                "Current Capacity (A)"
                            }
                        )
                    }
                )
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
