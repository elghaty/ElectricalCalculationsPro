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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.SldCableSizingResult
import com.electrical.calculationspro.data.SldCableSizingStudy
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@Composable
fun SldCableSizingResultsDialog(
    language: AppLanguage,
    study: SldCableSizingStudy,
    onDismiss: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        title = {
            Text(
                text = if (arabic)
                    "اختيار الكابلات التلقائي"
                else
                    "Automatic Cable Sizing",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                SummaryCard(
                    title = if (arabic)
                        "ملخص الدراسة"
                    else
                        "Study Summary",
                    rows = listOf(
                        (if (arabic)
                            "المغذيات الناجحة"
                        else
                            "Successful feeders") to
                            study.successfulFeeders.toString(),

                        (if (arabic)
                            "المغذيات غير المكتملة"
                        else
                            "Failed feeders") to
                            study.failedFeeders.toString()
                    )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        study.results.values.toList()
                    ) { result ->
                        CableSizingCard(
                            arabic = arabic,
                            result = result
                        )
                    }

                    if (study.notes.isNotEmpty()) {
                        item {
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
                                        text = if (arabic)
                                            "ملاحظات"
                                        else
                                            "Notes",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    study.notes.forEach { note ->
                                        Text(
                                            text = "• $note",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
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
                    text = if (arabic) "إغلاق" else "Close",
                    color = TextPrimary
                )
            }
        }
    )
}

@Composable
private fun CableSizingCard(
    arabic: Boolean,
    result: SldCableSizingResult
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
                text = "${result.fromNodeId} → ${result.toNodeId}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            ResultRow(
                label = if (arabic)
                    "تيار التصميم"
                else
                    "Design current",
                value = "${format(result.designCurrentA)} A"
            )

            ResultRow(
                label = if (arabic)
                    "التيار المطلوب للكابل"
                else
                    "Required cable capacity",
                value = "${format(result.requiredCurrentCapacityA)} A"
            )

            ResultRow(
                label = if (arabic)
                    "تيار القصر Ik''"
                else
                    "Short-circuit Ik''",
                value = "${format(result.shortCircuitCurrentKa)} kA"
            )

            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = 6.dp
                )
            )

            if (result.recommendedSizeMm2 > 0.0) {

                Text(
                    text = if (arabic)
                        "الكابل المقترح"
                    else
                        "Recommended cable",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                ResultRow(
                    label = if (arabic)
                        "المقطع"
                    else
                        "Size",
                    value = "${format(result.recommendedSizeMm2)} mm²"
                )

                ResultRow(
                    label = if (arabic)
                        "المادة"
                    else
                        "Material",
                    value = result.recommendedMaterial
                )

                ResultRow(
                    label = if (arabic)
                        "عدد القلوب"
                    else
                        "Cores",
                    value = result.recommendedCores.toString()
                )

                ResultRow(
                    label = if (arabic)
                        "عدد المسارات المتوازية"
                    else
                        "Parallel runs",
                    value = result.recommendedParallelRuns.toString()
                )

                ResultRow(
                    label = if (arabic)
                        "إجمالي سعة التيار"
                    else
                        "Total current capacity",
                    value =
                        "${format(result.recommendedCurrentCapacityA)} A"
                )

                ResultRow(
                    label = if (arabic)
                        "هبوط الجهد"
                    else
                        "Voltage drop",
                    value =
                        "${format(result.recommendedVoltageDropPercent)} %"
                )

                ResultRow(
                    label = if (arabic)
                        "تحمل القصر الحراري"
                    else
                        "Short-circuit withstand",
                    value =
                        "${format(result.recommendedShortCircuitWithstandKa)} kA"
                )

            } else {

                Text(
                    text = if (arabic)
                        "لم يتم العثور على مقاس يحقق جميع الشروط."
                    else
                        "No cable arrangement satisfies all criteria.",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (result.notes.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                result.notes.forEach { note ->
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
private fun SummaryCard(
    title: String,
    rows: List<Pair<String, String>>
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
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            rows.forEach { (label, value) ->
                ResultRow(
                    label = label,
                    value = value
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String
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
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun format(
    value: Double
): String {
    return when {
        value >= 100.0 ->
            "%.0f".format(value)

        value >= 10.0 ->
            "%.1f".format(value)

        else ->
            "%.2f".format(value)
    }
}
