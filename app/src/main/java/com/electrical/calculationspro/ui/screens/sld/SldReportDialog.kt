package com.electrical.calculationspro.ui.screens.sld

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SldReportDialog(
    title: String,
    text: String,
    onClose: () -> Unit
) {

    val context =
        LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    "application/pdf"
                )
        ) { uri ->

            if (uri != null) {

                try {

                    context
                        .contentResolver
                        .openOutputStream(
                            uri
                        )
                        ?.use { output ->

                            SldPdfReportExporter.export(
                                outputStream =
                                    output,

                                title =
                                    title,

                                reportText =
                                    text
                            )
                        }

                } catch (
                    _: Exception
                ) {
                    // File picker / storage errors are
                    // intentionally kept out of the UI state.
                }
            }
        }

    AlertDialog(

        onDismissRequest =
            onClose,

        title = {

            Text(
                text =
                    title
            )
        },

        text = {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            bottom = 8.dp
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    )
            ) {

                Text(
                    text =
                        text
                )
            }
        },

        confirmButton = {

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                Button(
                    onClick = {

                        launcher.launch(
                            buildPdfFileName(
                                title
                            )
                        )
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.PictureAsPdf,

                        contentDescription =
                            null
                    )

                    Text(
                        text =
                            " PDF"
                    )
                }

                OutlinedButton(
                    onClick =
                        onClose
                ) {

                    Text(
                        text =
                            "Close"
                    )
                }
            }
        }
    )
}

private fun buildPdfFileName(
    title: String
): String {

    val safe =
        title
            .replace(
                Regex("[^A-Za-z0-9_-]"),
                "_"
            )
            .take(50)

    return if (
        safe.isBlank()
    ) {
        "Electrical_Engineering_Report.pdf"
    } else {
        "${safe}_Engineering_Report.pdf"
    }
}
