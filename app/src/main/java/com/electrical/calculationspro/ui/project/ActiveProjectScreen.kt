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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = project.projectName,
                    style =
                        MaterialTheme.typography.headlineMedium
                )

                if (project.projectNumber.isNotBlank()) {

                    Text(
                        text =
                            if (arabic)
                                "رقم المشروع: ${project.projectNumber}"
                            else
                                "Project No: ${project.projectNumber}"
                    )
                }

                if (project.clientName.isNotBlank()) {

                    Text(
                        text =
                            if (arabic)
                                "العميل: ${project.clientName}"
                            else
                                "Client: ${project.clientName}"
                    )
                }

                if (project.location.isNotBlank()) {

                    Text(
                        text =
                            if (arabic)
                                "الموقع: ${project.location}"
                            else
                                "Location: ${project.location}"
                    )
                }
            }

            Icon(
                imageVector =
                    Icons.Outlined.Description,
                contentDescription = null
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text =
                if (arabic)
                    "التصميم داخل المشروع"
                else
                    "PROJECT DESIGN",
            style =
                MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        DesignModuleCard(
            title =
                if (arabic)
                    "التصميم الكهربائي"
                else
                    "Electrical Design",
            description =
                if (arabic)
                    "أحمال • تيارات • كابلات • هبوط جهد • قواطع • قصر • حماية"
                else
                    "Loads • Currents • Cables • Voltage Drop • Breakers • Short Circuit • Protection",
            icon = {
                Icon(
                    imageVector =
                        Icons.Outlined.Bolt,
                    contentDescription = null
                )
            },
            onClick = onElectrical
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        DesignModuleCard(
            title =
                if (arabic)
                    "تصميم المياه"
                else
                    "Water Design",
            description =
                if (arabic)
                    "تصرف • مواسير • فاقد • TDH • طلمبات • قدرة • استهلاك"
                else
                    "Flow • Pipes • Losses • TDH • Pumps • Power • Energy",
            icon = {
                Icon(
                    imageVector =
                        Icons.Outlined.WaterDrop,
                    contentDescription = null
                )
            },
            onClick = onWater
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        DesignModuleCard(
            title =
                if (arabic)
                    "تصميم الصرف الصحي"
                else
                    "Sewage Design",
            description =
                if (arabic)
                    "التصرفات • البيارة • خط الطرد • TDH • Duty/Standby • الطلمبات"
                else
                    "Flows • Wet Well • Rising Main • TDH • Duty/Standby • Pumps",
            icon = {
                Icon(
                    imageVector =
                        Icons.Outlined.WaterDrop,
                    contentDescription = null
                )
            },
            onClick = onSewage
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        DesignModuleCard(
            title =
                if (arabic)
                    "المخطط الأحادي SLD"
                else
                    "Single Line Diagram",
            description =
                if (arabic)
                    "يُنشأ من بيانات التصميم وليس من إدخال مستقل"
                else
                    "Generated from the project design data",
            icon = {
                Icon(
                    imageVector =
                        Icons.Outlined.AccountTree,
                    contentDescription = null
                )
            },
            onClick = onSld
        )

        Spacer(
            modifier = Modifier.height(18.dp)
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

@Composable
private fun DesignModuleCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            icon()

            Spacer(
                modifier = Modifier.height(1.dp)
            )

            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 14.dp
                    )
            ) {

                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleLarge
                )

                Text(
                    text = description,
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
