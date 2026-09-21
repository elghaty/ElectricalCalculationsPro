package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.SldNodeType

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
            Text(
                if (editing) {
                    if (arabic) {
                        "تعديل العنصر"
                    } else {
                        "Edit Element"
                    }
                } else {
                    if (arabic) {
                        "إضافة عنصر"
                    } else {
                        "Add Element"
                    }
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

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = {
                        Text(
                            if (arabic) {
                                "اسم العنصر"
                            } else {
                                "Name"
                            }
                        )
                    }
                )

                OutlinedTextField(
                    value = voltage,
                    onValueChange = onVoltageChange,
                    label = {
                        Text("Voltage (V)")
                    }
                )

                when (type) {

                    SldNodeType.SOURCE -> {

                        OutlinedTextField(
                            value = sourceMva,
                            onValueChange = onSourceMvaChange,
                            label = {
                                Text(
                                    "Source Fault MVA"
                                )
                            }
                        )
                    }

                    SldNodeType.TRANSFORMER -> {

                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            label = {
                                Text(
                                    "Rating (kVA)"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = transformerZ,
                            onValueChange =
                                onTransformerZChange,
                            label = {
                                Text("%Z")
                            }
                        )
                    }

                    SldNodeType.GENERATOR -> {

                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            label = {
                                Text(
                                    "Rating (kVA)"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = generatorXd,
                            onValueChange =
                                onGeneratorXdChange,
                            label = {
                                Text(
                                    "Xd'' (%)"
                                )
                            }
                        )
                    }

                    SldNodeType.PANEL -> {

                        OutlinedTextField(
                            value = kva,
                            onValueChange = onKvaChange,
                            label = {
                                Text(
                                    "Rating (kVA)"
                                )
                            }
                        )
                    }

                    SldNodeType.LOAD -> {

                        OutlinedTextField(
                            value = loadKw,
                            onValueChange = onLoadKwChange,
                            label = {
                                Text(
                                    "Load (kW)"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = pf,
                            onValueChange = onPfChange,
                            label = {
                                Text(
                                    "Power Factor"
                                )
                            }
                        )

                        OutlinedTextField(
                            value = demand,
                            onValueChange = onDemandChange,
                            label = {
                                Text(
                                    "Demand Factor"
                                )
                            }
                        )
                    }

                    SldNodeType.BUS,
                    SldNodeType.BREAKER -> Unit
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
