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

@Composable
fun SldConnectionEditorDialog(
    arabic: Boolean,
    length: String,
    resistance: String,
    reactance: String,
    cableSize: String,
    parallelRuns: String,
    capacity: String,
    onLengthChange: (String) -> Unit,
    onResistanceChange: (String) -> Unit,
    onReactanceChange: (String) -> Unit,
    onCableSizeChange: (String) -> Unit,
    onParallelRunsChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,

        title = {
            Text(
                if (arabic) {
                    "بيانات المغذي والكابل"
                } else {
                    "Feeder & Cable Data"
                }
            )
        },

        text = {
            Column(
                modifier = Modifier.verticalScroll(
                    rememberScrollState()
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = length,
                    onValueChange = onLengthChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) "طول الكابل (m)" else "Cable Length (m)"
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
                            if (arabic) "عدد المسارات المتوازية" else "Parallel Runs"
                        )
                    }
                )

                OutlinedTextField(
                    value = capacity,
                    onValueChange = onCapacityChange,
                    singleLine = true,
                    label = {
                        Text(
                            if (arabic) "التيار المسموح (A)" else "Current Capacity (A)"
                        )
                    }
                )
            }
        },

        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    if (arabic) "حفظ" else "Save"
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    if (arabic) "إلغاء" else "Cancel"
                )
            }
        }
    )
}
