package com.electrical.calculationspro.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

val standardText = project.electricalStandard?.name ?: "IEC"

Column(
    modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
) {

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (arabic)
                        "لوحة التصميم الهندسي"
                    else
                        "Engineering Design Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Electrical • Water • Sewage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Text(
            text = if (arabic)
                "المشروع الحالي"
            else
                "CURRENT PROJECT",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = project.projectName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (project.projectNumber.isNotBlank()) {
                            Text(
                                text = if (arabic)
                                    "رقم المشروع: ${project.projectNumber}"
                                else
                                    "Project No: ${project.projectNumber}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProjectInfoChip(
                        modifier = Modifier.weight(1f),
                        title = if (arabic) "الكود" else "Standard",
                        value = standardText
                    )

                    if (project.location.isNotBlank()) {
                        ProjectInfoChip(
                            modifier = Modifier.weight(1f),
                            title = if (arabic) "الموقع" else "Location",
                            value = project.location
                        )
                    }
                }

                if (project.clientName.isNotBlank()) {
                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = if (arabic)
                            "العميل: ${project.clientName}"
                        else
                            "Client: ${project.clientName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        DashboardSectionTitle(
            title = if (arabic)
                "التصميم الكهربائي"
            else
                "ELECTRICAL DESIGN",
            subtitle = if (arabic)
                "أدوات التصميم والحسابات الأساسية"
            else
                "Core electrical design and calculation tools"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        DashboardGrid(
            items = listOf(
                DashboardItem(
                    title = if (arabic) "الأحمال" else "Loads",
                    subtitle = "Load Schedule",
                    icon = Icons.Outlined.Speed,
                    onClick = onElectrical
                ),
                DashboardItem(
                    title = if (arabic) "الكابلات" else "Cables",
                    subtitle = "Cable Sizing",
                    icon = Icons.Outlined.ElectricalServices,
                    onClick = onElectrical
                ),
                DashboardItem(
                    title = if (arabic) "القواطع" else "Breakers",
                    subtitle = if (arabic)
                        "اختيار القواطع"
                    else
                        "Breaker Selection",
                    icon = Icons.Outlined.Power,
                    onClick = onElectrical
                ),
                DashboardItem(
                    title = if (arabic) "المحولات" else "Transformers",
                    subtitle = if (arabic)
                        "اختيار المحول"
                    else
                        "Transformer Sizing",
                    icon = Icons.Outlined.Memory,
                    onClick = onElectrical
                ),
                DashboardItem(
                    title = if (arabic) "المولدات" else "Generators",
                    subtitle = if (arabic)
                        "اختيار المولد"
                    else
                        "Generator Sizing",
                    icon = Icons.Outlined.Factory,
                    onClick = onElectrical
                ),
                DashboardItem(
                    title = if (arabic) "المضخات" else "Pumps",
                    subtitle = "Flow • Head • Power",
                    icon = Icons.Outlined.Waves,
                    onClick = onElectrical
                )
            )
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        DashboardSectionTitle(
            title = if (arabic)
                "الشبكة الكهربائية"
            else
                "ELECTRICAL NETWORK",
            subtitle = if (arabic)
                "تحليل وربط مكونات الشبكة"
            else
                "Network analysis and system coordination"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        DashboardGrid(
            items = listOf(
                DashboardItem(
                    title = "SLD",
                    subtitle = "Single Line Diagram",
                    icon = Icons.Outlined.AccountTree,
                    onClick = onSld
                ),
                DashboardItem(
                    title = if (arabic) "المياه" else "Water",
                    subtitle = if (arabic)
                        "التصميم الهيدروليكي"
                    else
                        "Hydraulic Design",
                    icon = Icons.Outlined.WaterDrop,
                    onClick = onWater
                ),
                DashboardItem(
                    title = if (arabic) "الصرف" else "Sewage",
                    subtitle = if (arabic)
                        "شبكة الصرف"
                    else
                        "Sewage Network",
                    icon = Icons.Outlined.Waves,
                    onClick = onSewage
                ),
                DashboardItem(
                    title = "Short Circuit",
                    subtitle = if (arabic)
                        "تحليل القصر"
                    else
                        "Fault Analysis",
                    icon = Icons.Outlined.Bolt,
                    onClick = onElectrical
                ),
                DashboardItem(
                    title = if (arabic)
                        "الحماية"
                    else
                        "Protection",
                    subtitle = if (arabic)
                        "التنسيق والحماية"
                    else
                        "Coordination",
                    icon = Icons.Outlined.Settings,
                    onClick = onElectrical
                )
            )
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        DashboardSectionTitle(
            title = if (arabic)
                "التقارير"
            else
                "REPORTS",
            subtitle = if (arabic)
                "مخرجات التصميم الهندسي"
            else
                "Engineering design deliverables"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            onClick = onElectrical
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(27.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (arabic)
                            "التقارير الهندسية"
                        else
                            "Engineering Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Panel Schedule • Engineering Report • Export",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = if (arabic)
                    "العودة إلى المشروعات"
                else
                    "Back to Projects"
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )
    }
}

}

private data class DashboardItem(
val title: String,
val subtitle: String,
val icon: ImageVector,
val onClick: () -> Unit
)

@Composable
private fun DashboardSectionTitle(
title: String,
subtitle: String
) {
Column(
modifier = Modifier.fillMaxWidth()
) {
Text(
text = title,
style = MaterialTheme.typography.titleLarge,
fontWeight = FontWeight.Bold
)

    Spacer(
        modifier = Modifier.height(2.dp)
    )

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

}

@Composable
private fun DashboardGrid(
items: List<DashboardItem>
) {
androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(
minSize = 155.dp
),
modifier = Modifier
.fillMaxWidth()
.height(
if (items.size <= 4) 330.dp else 500.dp
),
contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
horizontalArrangement = Arrangement.spacedBy(12.dp),
verticalArrangement = Arrangement.spacedBy(12.dp),
userScrollEnabled = false
) {
items(
count = items.size,
key = { index -> items[index].title }
) { index ->

        val item = items[index]

        Card(
            onClick = item.onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(15.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(29.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(
                    modifier = Modifier.height(9.dp)
                )

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

}

@Composable
private fun ProjectInfoChip(
modifier: Modifier,
title: String,
value: String
) {
Column(
modifier = modifier
) {
Text(
text = title,
style = MaterialTheme.typography.labelSmall,
color = MaterialTheme.colorScheme.onSurfaceVariant
)

    Spacer(
        modifier = Modifier.height(2.dp)
    )

    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2
    )
}

}
