package com.electrical.calculationspro.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.project.DesignProject

@Composable
fun ActiveProjectScreen(
    project: DesignProject,
    language: AppLanguage,
    onElectrical: () -> Unit,
    onWater: () -> Unit,
    onSewage: () -> Unit,
    onSld: () -> Unit,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.projectName,
                    style = MaterialTheme.typography.headlineMedium
                )

                if (project.projectNumber.isNotBlank()) {
                    Text(
                        text = if (arabic)
                            "رقم المشروع: ${project.projectNumber}"
                        else
                            "Project No: ${project.projectNumber}"
                    )
                }

                if (project.clientName.isNotBlank()) {
                    Text(
                        text = if (arabic)
                            "العميل: ${project.clientName}"
                        else
                            "Client: ${project.clientName}"
                    )
                }

                if (project.location.isNotBlank()) {
                    Text(
                        text = if (arabic)
                            "الموقع: ${project.location}"
                        else
                            "Location: ${project.location}"
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = if (arabic)
                "التصميم داخل المشروع"
            else
                "PROJECT DESIGN",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        FourColumnDesignGrid(
            modifier = Modifier.weight(1f),
            items = listOf(
                DesignGridItem(
                    id = "electrical",
                    title = if (arabic)
                        "التصميم الكهربائي"
                    else
                        "Electrical Design",
                    subtitle = if (arabic)
                        "أحمال • تيارات • كابلات • قواطع • حماية"
                    else
                        "Loads • Cables • Breakers • Protection",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null
                        )
                    },
                    onClick = onElectrical
                ),

                DesignGridItem(
                    id = "water",
                    title = if (arabic)
                        "تصميم المياه"
                    else
                        "Water Design",
                    subtitle = if (arabic)
                        "تصرف • مواسير • TDH • طلمبات • استهلاك"
                    else
                        "Flow • Pipes • TDH • Pumps • Energy",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = null
                        )
                    },
                    onClick = onWater
                ),

                DesignGridItem(
                    id = "sewage",
                    title = if (arabic)
                        "الصرف الصحي"
                    else
                        "Sewage Design",
                    subtitle = if (arabic)
                        "تصرفات • بيارة • خط طرد • طلمبات"
                    else
                        "Flows • Wet Well • Rising Main • Pumps",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = null
                        )
                    },
                    onClick = onSewage
                ),

                DesignGridItem(
                    id = "sld",
                    title = if (arabic)
                        "المخطط SLD"
                    else
                        "Single Line Diagram",
                    subtitle = if (arabic)
                        "مرتبط ببيانات التصميم"
                    else
                        "Linked to design data",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.AccountTree,
                            contentDescription = null
                        )
                    },
                    onClick = onSld
                )
            )
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onBack
        ) {
            Text(
                if (arabic)
                    "العودة إلى المشروعات"
                else
                    "Back to Projects"
            )
        }
    }
}
