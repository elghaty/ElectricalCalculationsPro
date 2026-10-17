package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.ui.components.EngineeringPage
import com.electrical.calculationspro.ui.components.EngineeringSection
import com.electrical.calculationspro.ui.components.EngineeringValueRow

enum class ElectromechanicalModule {
    LOAD,
    CURRENT,
    CABLE,
    VOLTAGE_DROP,
    BREAKER,
    SHORT_CIRCUIT,
    PROTECTION,
    TRANSFORMER,
    GENERATOR,
    PANEL,
    PUMP,
    WATER_FLOW_VELOCITY
}

@Composable
fun ElectromechanicalDesignScreen(
    language: AppLanguage,
    standard: Standard = Standard.IEC,
    onBack: () -> Unit,
    onOpenModule: ((ElectromechanicalModule) -> Unit)? = null
) {
    val arabic = language == AppLanguage.ARABIC

    EngineeringPage(
        title = if (arabic) {
            "التصميم الكهروميكانيكي"
        } else {
            "Electromechanical Design"
        },
        subtitle = if (arabic) {
            "منظومة الحسابات الهندسية للمشروع"
        } else {
            "Project engineering calculation workspace"
        },
        onBack = onBack
    ) {
        EngineeringSection(
            title = if (arabic) "معيار التصميم" else "Design Standard"
        ) {
            EngineeringValueRow(
                label = if (arabic) {
                    "معيار المشروع"
                } else {
                    "Project Standard"
                },
                value = standard.name
            )

            Text(
                text = if (arabic) {
                    "المعيار المعروض هو المعيار المحدد للمشروع. يجب أن تستخدمه محركات الحساب والاختيار عند تنفيذ كل دراسة."
                } else {
                    "This is the project's selected standard. Calculation and equipment-selection engines must apply it to each study."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        EngineeringSection(
            title = if (arabic) {
                "وحدات التصميم الكهروميكانيكي"
            } else {
                "Electromechanical Design Modules"
            }
        ) {
            ModuleGrid(
                language = language,
                modules = electricalModules(language),
                onModuleSelected = { module ->
                    onOpenModule?.invoke(module)
                }
            )
        }
    }
}

@Composable
private fun ModuleGrid(
    language: AppLanguage,
    modules: List<DesignModuleItem>,
    onModuleSelected: (ElectromechanicalModule) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        modules.chunked(4).forEach { moduleRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                moduleRow.forEach { item ->
                    DesignModuleCard(
                        modifier = Modifier.weight(1f),
                        item = item,
                        onClick = {
                            onModuleSelected(item.module)
                        }
                    )
                }

                repeat(4 - moduleRow.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun electricalModules(
    language: AppLanguage
): List<DesignModuleItem> {
    val arabic = language == AppLanguage.ARABIC

    return listOf(
        DesignModuleItem(
            module = ElectromechanicalModule.LOAD,
            title = if (arabic) "الأحمال" else "Loads",
            subtitle = if (arabic) "جدول الأحمال" else "Load Schedule",
            icon = Icons.Outlined.Power
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.CURRENT,
            title = if (arabic) "التيار" else "Current",
            subtitle = if (arabic) "تيار التصميم" else "Design Current",
            icon = Icons.Outlined.Calculate
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.CABLE,
            title = if (arabic) "الكابلات" else "Cables",
            subtitle = if (arabic) "اختيار المقطع" else "Conductor Sizing",
            icon = Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.VOLTAGE_DROP,
            title = if (arabic) "هبوط الجهد" else "Voltage Drop",
            subtitle = if (arabic) "دراسة الهبوط" else "Voltage Drop Study",
            icon = Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.BREAKER,
            title = if (arabic) "القواطع" else "Breakers",
            subtitle = if (arabic) "اختيار القاطع" else "Breaker Selection",
            icon = Icons.Outlined.Security
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.SHORT_CIRCUIT,
            title = if (arabic) "تيار القصر" else "Short Circuit",
            subtitle = if (arabic) "حساب تيار العطل" else "Fault Current",
            icon = Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.PROTECTION,
            title = if (arabic) "الحماية" else "Protection",
            subtitle = if (arabic) "دراسة الحماية" else "Protection Study",
            icon = Icons.Outlined.Security
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.TRANSFORMER,
            title = if (arabic) "المحولات" else "Transformers",
            subtitle = if (arabic) "اختيار المحول" else "Transformer Design",
            icon = Icons.Outlined.Memory
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.GENERATOR,
            title = if (arabic) "المولدات" else "Generators",
            subtitle = if (arabic) "اختيار المولد" else "Generator Design",
            icon = Icons.Outlined.Factory
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.PANEL,
            title = if (arabic) "اللوحات" else "Panels",
            subtitle = if (arabic) "تصميم اللوحات" else "Panel Design",
            icon = Icons.Outlined.ElectricalServices
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.PUMP,
            title = if (arabic) "المضخات" else "Pumps",
            subtitle = if (arabic) "التصرف والرفع والقدرة" else "Flow / Head / Power",
            icon = Icons.Outlined.WaterDrop
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.WATER_FLOW_VELOCITY,
            title = if (arabic) {
                "التصرف والسرعة والقطر"
            } else {
                "Flow / Velocity / Diameter",
            },
            subtitle = if (arabic) {
                "حسابات المواسير"
            } else {
                "Pipe Hydraulic Calculations"
            },
            icon = Icons.Outlined.WaterDrop
        )
    )
}

private data class DesignModuleItem(
    val module: ElectromechanicalModule,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
private fun DesignModuleCard(
    modifier: Modifier,
    item: DesignModuleItem,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
