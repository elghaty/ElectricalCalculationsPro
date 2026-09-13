package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.PanelScheduleStatus
import com.electrical.calculationspro.data.SldPanelSchedule
import com.electrical.calculationspro.data.SldPanelScheduleRow
import java.util.Locale
import kotlin.math.roundToInt

private val PanelBackground = Color(0xFF101418)
private val PanelCard = Color(0xFF182027)
private val PanelCardSecondary = Color(0xFF202A33)
private val PanelPrimary = Color(0xFF00BCD4)
private val PanelTextPrimary = Color(0xFFF1F5F9)
private val PanelTextSecondary = Color(0xFFB0BEC5)
private val PanelSuccess = Color(0xFF4CAF50)
private val PanelWarning = Color(0xFFFFC107)
private val PanelError = Color(0xFFF44336)

@Composable
fun SldPanelScheduleResultsDialog(
    language: String,
    schedule: SldPanelSchedule,
    onDismiss: () -> Unit
) {
    val isArabic =
        language.equals("ar", ignoreCase = true) ||
            language.equals("arabic", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelBackground,
        titleContentColor = PanelTextPrimary,
        textContentColor = PanelTextSecondary,
        title = {
            Text(
                text = if (isArabic) {
                    "جدول اللوحة"
                } else {
                    "Panel Schedule"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                PanelHeader(
                    isArabic = isArabic,
                    schedule = schedule
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (schedule.rows.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = PanelCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) {
                                "لا توجد مغذيات خارجة من اللوحة المحددة."
                            } else {
                                "No outgoing feeders are connected to the selected panel."
                            },
                            modifier = Modifier.padding(16.dp),
                            color = PanelTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(430.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = schedule.rows,
                            key = { it.connectionId }
                        ) { row ->
                            PanelScheduleRowCard(
                                isArabic = isArabic,
                                row = row
                            )
                        }

                        if (schedule.notes.isNotEmpty()) {
                            item {
                                PanelNotesCard(
                                    isArabic = isArabic,
                                    notes = schedule.notes
                                )
                            }
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
                    text = if (isArabic) "إغلاق" else "Close",
                    color = PanelPrimary
                )
            }
        }
    )
}

@Composable
private fun PanelHeader(
    isArabic: Boolean,
    schedule: SldPanelSchedule
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PanelCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = schedule.panelName,
                color = PanelTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                color = PanelCardSecondary
            )

            PanelSummaryRow(
                label = if (isArabic) "الجهد" else "Voltage",
                value = "${formatNumber(schedule.panelVoltageV)} V"
            )

            PanelSummaryRow(
                label = if (isArabic) "الحمل المتصل" else "Connected Load",
                value = "${formatNumber(schedule.totalConnectedLoadKw)} kW"
            )

            PanelSummaryRow(
                label = if (isArabic) "حمل الطلب" else "Demand Load",
                value = "${formatNumber(schedule.totalDemandLoadKw)} kW"
            )

            PanelSummaryRow(
                label = if (isArabic) "تيار الطلب" else "Demand Current",
                value = "${formatNumber(schedule.totalDemandCurrentA)} A"
            )

            PanelSummaryRow(
                label = if (isArabic) "عدد المغذيات" else "Feeders",
                value = schedule.rows.size.toString()
            )
        }
    }
}

@Composable
private fun PanelScheduleRowCard(
    isArabic: Boolean,
    row: SldPanelScheduleRow
) {
    val statusColor =
        when (row.status) {
            PanelScheduleStatus.PASS -> PanelSuccess
            PanelScheduleStatus.WARNING -> PanelWarning
            PanelScheduleStatus.FAIL -> PanelError
        }

    val statusText =
        when (row.status) {
            PanelScheduleStatus.PASS ->
                if (isArabic) "مطابق" else "PASS"

            PanelScheduleStatus.WARNING ->
                if (isArabic) "تحذير" else "WARNING"

            PanelScheduleStatus.FAIL ->
                if (isArabic) "فشل" else "FAIL"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PanelCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.feederName,
                        color = PanelTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "${row.fromNodeId} → ${row.toNodeId}",
                        color = PanelTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        )
                )
            }

            HorizontalDivider(
                color = PanelCardSecondary
            )

            PanelSummaryRow(
                label = if (isArabic) "الحمل" else "Load",
                value = "${formatNumber(row.loadKw)} kW"
            )

            PanelSummaryRow(
                label = if (isArabic) "معامل القدرة" else "Power Factor",
                value = formatNumber(row.powerFactor)
            )

            PanelSummaryRow(
                label = if (isArabic) "معامل الطلب" else "Demand Factor",
                value = formatNumber(row.demandFactor)
            )

            PanelSummaryRow(
                label = if (isArabic) "تيار التصميم" else "Design Current",
                value = "${formatNumber(row.designCurrentA)} A"
            )

            PanelSummaryRow(
                label = if (isArabic) "القاطع المقترح" else "Recommended Breaker",
                value = "${formatNumber(row.recommendedBreakerA)} A"
            )

            PanelSummaryRow(
                label = if (isArabic) "الكابل" else "Cable",
                value =
                    if (row.cableSizeMm2 > 0.0) {
                        "${formatNumber(row.cableSizeMm2)} mm²"
                    } else {
                        if (isArabic) "غير محدد" else "Not selected"
                    }
            )

            PanelSummaryRow(
                label = if (isArabic) "عدد المسارات" else "Parallel Runs",
                value = row.parallelRuns.toString()
            )

            PanelSummaryRow(
                label = if (isArabic) "سعة الكابل" else "Cable Capacity",
                value =
                    if (row.currentCapacityA > 0.0) {
                        "${formatNumber(row.currentCapacityA)} A"
                    } else {
                        if (isArabic) "غير متاحة" else "N/A"
                    }
            )

            PanelSummaryRow(
                label = if (isArabic) "هبوط الجهد" else "Voltage Drop",
                value =
                    "${formatNumber(row.voltageDropPercent)} %"
            )

            PanelSummaryRow(
                label = if (isArabic) "تيار القصر" else "Short Circuit",
                value =
                    if (row.shortCircuitCurrentKa > 0.0) {
                        "${formatNumber(row.shortCircuitCurrentKa)} kA"
                    } else {
                        if (isArabic) "غير محسوب" else "Not calculated"
                    }
            )

            if (row.notes.isNotEmpty()) {
                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = if (isArabic) "ملاحظات" else "Notes",
                    color = PanelTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                row.notes.forEach { note ->
                    Text(
                        text = "• $note",
                        color = PanelTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelNotesCard(
    isArabic: Boolean,
    notes: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PanelCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isArabic) "ملاحظات الجدول" else "Schedule Notes",
                color = PanelTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            notes.forEach { note ->
                Text(
                    text = "• $note",
                    color = PanelTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PanelSummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = PanelTextSecondary,
            fontSize = 12.sp
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = value,
            color = PanelTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatNumber(
    value: Double
): String {
    if (!value.isFinite()) {
        return "0"
    }

    val rounded =
        (value * 100.0).roundToInt() / 100.0

    return String.format(
        Locale.US,
        "%.2f",
        rounded
    ).trimEnd('0')
        .trimEnd('.')
}
