package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.iecInstallationMethods

@Composable
fun SldConnectionEditorDialog(
arabic: Boolean,

connectionType: String,

length: String,
resistance: String,
reactance: String,
cableSize: String,
parallelRuns: String,
capacity: String,

conductorMaterial: String,
insulationType: String,
installationMethodCode: String,

busbarMaterial: String,
busbarRatedCurrent: String,
busbarShortCircuit: String,

onConnectionTypeChange: (String) -> Unit,

onLengthChange: (String) -> Unit,
onResistanceChange: (String) -> Unit,
onReactanceChange: (String) -> Unit,
onCableSizeChange: (String) -> Unit,
onParallelRunsChange: (String) -> Unit,
onCapacityChange: (String) -> Unit,

onConductorMaterialChange: (String) -> Unit,
onInsulationTypeChange: (String) -> Unit,
onInstallationMethodChange: (String) -> Unit,

onBusbarMaterialChange: (String) -> Unit,
onBusbarRatedCurrentChange: (String) -> Unit,
onBusbarShortCircuitChange: (String) -> Unit,

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
            modifier = Modifier.verticalScroll(
                rememberScrollState()
            ),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            /*
             * =====================================================
             * CONNECTION TYPE
             * =====================================================
             */

            OutlinedButton(
                onClick = {
                    typeMenuExpanded = true
                }
            ) {
                Text(
                    if (isBusbar) {
                        if (arabic) {
                            "نوع التوصيل: Busbar داخلي"
                        } else {
                            "Connection: Internal Busbar"
                        }
                    } else {
                        if (arabic) {
                            "نوع التوصيل: كابل"
                        } else {
                            "Connection: Cable"
                        }
                    }
                )
            }

            DropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = {
                    typeMenuExpanded = false
                }
            ) {

                DropdownMenuItem(
                    text = {
                        Text(
                            if (arabic) {
                                "Busbar — توصيل داخلي داخل اللوحة"
                            } else {
                                "Busbar — Internal Panel Connection"
                            }
                        )
                    },
                    onClick = {
                        onConnectionTypeChange("BUSBAR")
                        typeMenuExpanded = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            if (arabic) {
                                "Cable — مغذي خارجي"
                            } else {
                                "Cable — External Feeder"
                            }
                        )
                    },
                    onClick = {
                        onConnectionTypeChange("CABLE")
                        typeMenuExpanded = false
                    }
                )
            }

            /*
             * =====================================================
             * BUSBAR
             * =====================================================
             */

            if (isBusbar) {

                Text(
                    if (arabic) {
                        "بيانات الـ Busbar"
                    } else {
                        "Busbar Data"
                    }
                )

                OutlinedButton(
                    onClick = {
                        busbarMaterialMenuExpanded = true
                    }
                ) {
                    Text(
                        if (arabic) {
                            "المادة: $busbarMaterial"
                        } else {
                            "Material: $busbarMaterial"
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

                    DropdownMenuItem(
                        text = {
                            Text("Copper")
                        },
                        onClick = {
                            onBusbarMaterialChange("Copper")
                            busbarMaterialMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Aluminum")
                        },
                        onClick = {
                            onBusbarMaterialChange("Aluminum")
                            busbarMaterialMenuExpanded = false
                        }
                    )
                }

                OutlinedTextField(
                    value = busbarRatedCurrent,
                    onValueChange =
                        onBusbarRatedCurrentChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) {
                                "تيار الـ Busbar المقنن (A)"
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
                                "تحمل القصر للـ Busbar (kA)"
                            } else {
                                "Busbar Short-Circuit Rating (kA)"
                            }
                        )
                    }
                )

                Text(
                    if (arabic) {
                        "هذا التوصيل داخلي داخل اللوحة؛ لذلك لا يوجد طول كابل أو طريقة تنفيذ."
                    } else {
                        "This is an internal panel connection; cable length and installation method do not apply."
                    }
                )
            }

            /*
             * =====================================================
             * CABLE
             * =====================================================
             */

            if (!isBusbar) {

                Text(
                    if (arabic) {
                        "بيانات المغذي والكابل"
                    } else {
                        "Feeder & Cable Data"
                    }
                )

                /*
                 * Cable length is required.
                 */
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

                /*
                 * Conductor.
                 */
                OutlinedButton(
                    onClick = {
                        conductorMenuExpanded = true
                    }
                ) {
                    Text(
                        if (arabic) {
                            "موصل: $conductorMaterial"
                        } else {
                            "Conductor: $conductorMaterial"
                        }
                    )
                }

                DropdownMenu(
                    expanded = conductorMenuExpanded,
                    onDismissRequest = {
                        conductorMenuExpanded = false
                    }
                ) {

                    DropdownMenuItem(
                        text = {
                            Text("Copper")
                        },
                        onClick = {
                            onConductorMaterialChange("Copper")
                            conductorMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Aluminum")
                        },
                        onClick = {
                            onConductorMaterialChange("Aluminum")
                            conductorMenuExpanded = false
                        }
                    )
                }

                /*
                 * Insulation.
                 */
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
                    expanded = insulationMenuExpanded,
                    onDismissRequest = {
                        insulationMenuExpanded = false
                    }
                ) {

                    DropdownMenuItem(
                        text = {
                            Text("PVC")
                        },
                        onClick = {
                            onInsulationTypeChange("PVC")
                            insulationMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("XLPE")
                        },
                        onClick = {
                            onInsulationTypeChange("XLPE")
                            insulationMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("EPR")
                        },
                        onClick = {
                            onInsulationTypeChange("EPR")
                            insulationMenuExpanded = false
                        }
                    )
                }

                /*
                 * Installation method.
                 *
                 * Comes from the existing installation-method
                 * dataset instead of being hard-coded into the
                 * SLD dialog.
                 */
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

                    iecInstallationMethods.forEach { method ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${method.code} - ${method.description}"
                                )
                            },
                            onClick = {
                                onInstallationMethodChange(
                                    method.code
                                )
                                installationMenuExpanded = false
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

                /*
                 * These remain available for the current
                 * engineering engine, but are no longer the
                 * primary engineering input for the user.
                 */
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
                    "حفظ وحساب"
                } else {
                    "Save & Calculate"
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
