package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SldReportDialog(
    title: String,
    text: String,
    onClose: () -> Unit
) {

    AlertDialog(

        onDismissRequest = onClose,

        title = {
            Text(title)
        },

        text = {

            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                Text(text)
            }
        },

        confirmButton = {

            Button(
                onClick = onClose
            ) {
                Text("Close")
            }
        }
    )
}
