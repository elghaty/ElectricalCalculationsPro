package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldShortCircuitResult
import com.electrical.calculationspro.data.SldShortCircuitStudy
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@Composable
fun SldShortCircuitResultsDialog(
    language: AppLanguage,
    study: SldShortCircuitStudy?,
    onDismiss: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (arabic) {
                    "دراسة القصر الكهربائي"
                } else {
                    "Short-Circuit Study"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (study == null) {
                Text(
                    text = if (arabic) {
                        "لا توجد نتائج."
                    } else {
                        "No short-circuit results."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SummaryCard(
                            arabic = arabic,
                            study = study
                        )
                    }

                    item {
                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = if (arabic) {
                                "نتائج جميع العناصر"
                            } else {
                                "All Node Results"
                            },
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    items(
                        items = study.results.values.toList(),
                        key = {
                            it.nodeId
                        }
                    ) { result ->
                        ShortCircuitNodeCard(
                            arabic = arabic,
                            result = result
                        )
                    }

                    if (study.notes.isNotEmpty()) {
                        item {
                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = if (arabic) {
                                    "ملاحظات الدراسة"
                                } else {
                                    "Study Notes"
                                },
                                color = PrimaryTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        items(
                            items = study.notes
                        ) { note ->
                            Text(
                                text = note,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = if (arabic) {
                        "إغلاق"
                    } else {
                        "Close"
                    }
                )
            }
        }
    )
}

@Composable
private fun SummaryCard(
    arabic: Boolean,
    study: SldShortCircuitStudy
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14212B)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = if (arabic) {
                    "ملخص الدراسة"
                } else {
                    "Study Summary"
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            SummaryRow(
                label = if (arabic) {
                    "أقصى تيار قصر Ik''"
                } else {
                    "Maximum Ik''"
                },
                value = "%.3f kA"
                    .format(study.maximumFaultCurrentKa)
            )

            SummaryRow(
                label = if (arabic) {
                    "أقصى تيار لحظي"
                } else {
                    "Maximum Peak Current"
                },
                value = "%.3f kA"
                    .format(study.maximumPeakCurrentKa)
            )

            SummaryRow(
                label = if (arabic) {
                    "أقصى مستوى قصر"
                } else {
                    "Maximum Fault Level"
                },
                value = "%.3f MVA"
                    .format(study.maximumFaultMva)
            )

            SummaryRow(
                label = if (arabic) {
                    "عدد العناصر"
                } else {
                    "Calculated Nodes"
                },
                value = study.results.size.toString()
            )
        }
    }
}

@Composable
private fun ShortCircuitNodeCard(
    arabic: Boolean,
    result: SldShortCircuitResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF182630)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = result.nodeName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                text = "%.0f V".format(result.voltageV),
                color = TextSecondary,
                fontSize = 10.sp
            )

            SummaryRow(
                label = "Ik''",
                value = "%.3f kA"
                    .format(
                        result.initialSymmetricalCurrentKa
                    )
            )

            SummaryRow(
                label = "Ip",
                value = "%.3f kA"
                    .format(
                        result.peakCurrentKa
                    )
            )

            SummaryRow(
                label = if (arabic) {
                    "مستوى القصر"
                } else {
                    "Fault MVA"
                },
                value = "%.3f MVA"
                    .format(
                        result.shortCircuitMva
                    )
            )

            SummaryRow(
                label = "R",
                value = "%.6f Ω"
                    .format(
                        result.resistanceOhm
                    )
            )

            SummaryRow(
                label = "X",
                value = "%.6f Ω"
                    .format(
                        result.reactanceOhm
                    )
            )

            SummaryRow(
                label = "Z",
                value = "%.6f Ω"
                    .format(
                        result.impedanceOhm
                    )
            )

            SummaryRow(
                label = "X/R",
                value = "%.3f"
                    .format(
                        result.xrRatio
                    )
            )

            SummaryRow(
                label = if (arabic) {
                    "القاطع المطلوب"
                } else {
                    "Required Breaker"
                },
                value = "%.1f kA"
                    .format(
                        result.breakerRequiredKa
                    )
            )

            if (result.notes.isNotEmpty()) {
                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                result.notes.forEach { note ->
                    Text(
                        text = note,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )

        Text(
            text = value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
