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

    var typeMenuExpanded by remember {
        mutableStateOf(false)
    }

    var conductorMenuExpanded by remember {
        mutableStateOf(false)
    }

    var insulationMenuExpanded by remember {
        mutableStateOf(false)
    }

    var installationMenuExpanded by remember {
        mutableStateOf(false)
    }

    var busbarMaterialMenuExpanded by remember {
        mutableStateOf(false)
    }

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
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedButton(
                        onClick = {
                            typeMenuExpanded = true
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
                        expanded =
                            typeMenuExpanded,

                        onDismissRequest = {
                            typeMenuExpanded = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("BUSBAR")
                            },

                            onClick = {

                                onConnectionTypeChange(
                                    "BUSBAR"
                                )

                                typeMenuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("CABLE")
                            },

                            onClick = {

                                onConnectionTypeChange(
                                    "CABLE"
                                )

                                typeMenuExpanded = false
                            }
                        )
                    }
                }

                if (isBusbar) {

                    Text(
                        if (arabic) {
                            "توصيل داخلي داخل اللوحة"
                        } else {
                            "Internal panel connection"
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            busbarMaterialMenuExpanded = true
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
                        expanded =
                            busbarMaterialMenuExpanded,

                        onDismissRequest = {
                            busbarMaterialMenuExpanded = false
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

                                    onBusbarMaterialChange(
                                        material
                                    )

                                    busbarMaterialMenuExpanded =
                                        false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value =
                            busbarRatedCurrent,

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
                            }
                        }
                    )

                    OutlinedTextField(
                        value =
                            busbarShortCircuit,

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
                            "لا يوجد طول كابل أو طريقة تنفيذ لهذا النوع من التوصيل."
                        } else {
                            "No cable length or installation method is required for a busbar connection."
                        }
                    )

                } else {

                    Text(
                        if (arabic) {
                            "توصيل خارجي بواسطة كابل"
                        } else {
                            "External feeder connection"
                        }
                    )

                    OutlinedTextField(
                        value =
                            length,

                        onValueChange =
                            onLengthChange,

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
                            conductorMenuExpanded = true
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
                        expanded =
                            conductorMenuExpanded,

                        onDismissRequest = {
                            conductorMenuExpanded = false
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

                                    onConductorMaterialChange(
                                        material
                                    )

                                    conductorMenuExpanded =
                                        false
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            insulationMenuExpanded = true
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
                        expanded =
                            insulationMenuExpanded,

                        onDismissRequest = {
                            insulationMenuExpanded = false
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

                                    onInsulationTypeChange(
                                        insulation
                                    )

                                    insulationMenuExpanded =
                                        false
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            installationMenuExpanded = true
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
                        expanded =
                            installationMenuExpanded,

                        onDismissRequest = {
                            installationMenuExpanded = false
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

                                    onInstallationMethodChange(
                                        method
                                    )

                                    installationMenuExpanded =
                                        false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value =
                            cableSize,

                        onValueChange =
                            onCableSizeChange,

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
                        value =
                            parallelRuns,

                        onValueChange =
                            onParallelRunsChange,

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
                        value =
                            resistance,

                        onValueChange =
                            onResistanceChange,

                        singleLine = true,

                        label = {
                            Text("R (Ω/km)")
                        }
                    )

                    OutlinedTextField(
                        value =
                            reactance,

                        onValueChange =
                            onReactanceChange,

                        singleLine = true,

                        label = {
                            Text("X (Ω/km)")
                        }
                    )

                    OutlinedTextField(
                        value =
                            capacity,

                        onValueChange =
                            onCapacityChange,

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
