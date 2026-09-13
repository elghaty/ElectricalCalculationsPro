package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.ProtectionStatus
import com.electrical.calculationspro.data.SldProtectionCoordinationResult
import com.electrical.calculationspro.data.SldProtectionDevice
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@Composable
fun SldProtectionCoordinationResultsDialog(
    language: AppLanguage,
    result: SldProtectionCoordinationResult,
    onDismiss: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = {
            Text(
                text = if (arabic) {
                    "تنسيق الحماية والانتقائية"
                } else {
                    "Protection Coordination"
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ProtectionSummaryCard(
                    arabic = arabic,
                    result = result
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = result.devices.values.toList(),
                        key = { it.nodeId }
                    ) { device ->
                        ProtectionDeviceCard(
                            arabic = arabic,
                            device = device
                        )
                    }

                    if (result.notes.isNotEmpty()) {
                        item {
                            NotesCard(
                                arabic = arabic,
                                notes = result.notes
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
                    text = if (arabic) "إغلاق" else "Close",
                    color = TextPrimary
                )
            }
        }
    )
}

@Composable
private fun ProtectionSummaryCard(
    arabic: Boolean,
    result: SldProtectionCoordinationResult
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = DarkBackground
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = if (arabic) {
                    "ملخص التنسيق"
                } else {
                    "Coordination Summary"
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "أزواج منسقة"
                } else {
                    "Coordinated pairs"
                },
                value = result.coordinatedPairs.toString(),
                valueColor = Color(0xFF4CAF50)
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "تحتاج مراجعة"
                } else {
                    "Warning pairs"
                },
                value = result.warningPairs.toString(),
                valueColor = Color(0xFFFFC107)
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "أزواج غير منسقة"
                } else {
                    "Failed pairs"
                },
                value = result.failedPairs.toString(),
                valueColor = Color(0xFFF44336)
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "أجهزة الحماية"
                } else {
                    "Protection devices"
                },
                value = result.devices.size.toString()
            )
        }
    }
}

@Composable
private fun ProtectionDeviceCard(
    arabic: Boolean,
    device: SldProtectionDevice
) {
    val statusColor = protectionStatusColor(
        device.status
    )

    val statusText = when (device.status) {
        ProtectionStatus.PASS ->
            if (arabic) "ناجح" else "PASS"

        ProtectionStatus.WARNING ->
            if (arabic) "تحذير" else "WARNING"

        ProtectionStatus.FAIL ->
            if (arabic) "فشل" else "FAIL"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = DarkBackground
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = device.nodeName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = device.deviceType,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "تيار الحمل/المغذي"
                } else {
                    "Downstream current"
                },
                value = "${format(device.downstreamCurrentA)} A"
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "القاطع المقترح"
                } else {
                    "Recommended breaker"
                },
                value = "${format(device.recommendedRatingA)} A"
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "قدرة القطع"
                } else {
                    "Short-circuit rating"
                },
                value = if (device.shortCircuitRatingKa > 0.0) {
                    "${format(device.shortCircuitRatingKa)} kA"
                } else {
                    if (arabic) "غير متاحة" else "Not available"
                }
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "ضبط الحماية الحرارية"
                } else {
                    "Long-time pickup"
                },
                value = "${format(device.longTimePickupA)} A"
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "ضبط الحماية اللحظية"
                } else {
                    "Instantaneous pickup"
                },
                value = "${format(device.instantaneousPickupA)} A"
            )

            ProtectionResultRow(
                label = if (arabic) {
                    "هامش الانتقائية"
                } else {
                    "Selectivity margin"
                },
                value = if (device.selectivityMarginA > 0.0) {
                    "${format(device.selectivityMarginA)} A"
                } else {
                    if (arabic) {
                        "يتطلب منحنيات TCC"
                    } else {
                        "Requires TCC curves"
                    }
                }
            )

            if (device.notes.isNotEmpty()) {
                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text = if (arabic) {
                        "ملاحظات"
                    } else {
                        "Notes"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                device.notes.forEach { note ->
                    Text(
                        text = "• $note",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(
                            vertical = 1.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    arabic: Boolean,
    notes: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = DarkBackground
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = if (arabic) {
                    "ملاحظات الدراسة"
                } else {
                    "Study Notes"
                },
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            notes.forEach { note ->
                Text(
                    text = "• $note",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(
                        vertical = 2.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun ProtectionResultRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun protectionStatusColor(
    status: ProtectionStatus
): Color =
    when (status) {
        ProtectionStatus.PASS ->
            Color(0xFF4CAF50)

        ProtectionStatus.WARNING ->
            Color(0xFFFFC107)

        ProtectionStatus.FAIL ->
            Color(0xFFF44336)
    }

private fun format(
    value: Double
): String =
    when {
        value >= 1000.0 ->
            "%.0f".format(value)

        value >= 100.0 ->
            "%.0f".format(value)

        value >= 10.0 ->
            "%.1f".format(value)

        else ->
            "%.2f".format(value)
    }
