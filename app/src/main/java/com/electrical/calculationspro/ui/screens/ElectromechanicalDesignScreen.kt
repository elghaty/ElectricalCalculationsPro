
package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard

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
    PUMP
}

@Composable
fun ElectromechanicalDesignScreen(
    language: AppLanguage,
    standard: Standard = Standard.IEC,
    onBack: () -> Unit,
    onOpenModule: ((ElectromechanicalModule) -> Unit)? = null
) {
    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (arabic) {
                        "التصميم الكهروميكانيكي"
                    } else {
                        "Electromechanical Design"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.padding(top = 3.dp))

                Text(
                    text = if (arabic) {
                        "منظومة الحسابات الهندسية"
                    } else {
                        "Engineering Calculation Workspace"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            TextButton(onClick = onBack) {
                Text(if (arabic) "رجوع" else "Back")
            }
        }

        Spacer(modifier = Modifier.padding(top = 10.dp))

        StandardCard(
            language = language,
            standard = standard
        )

        Spacer(modifier = Modifier.padding(top = 14.dp))

        Text(
            text = if (arabic) {
                "دورة التصميم الكهربائي"
            } else {
                "Electrical Design Workflow"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.padding(top = 8.dp))

        ModuleGrid(
            language = language,
            onModuleSelected = { module ->
                onOpenModule?.invoke(module)
            }
        )
    }
}

@Composable
private fun StandardCard(
    language: AppLanguage,
    standard: Standard
) {
    val arabic = language == AppLanguage.ARABIC

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (arabic) "الكود / المعيار" else "Design Standard",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.padding(top = 4.dp))

            Text(
                text = standard.name,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.padding(top = 2.dp))

            Text(
                text = if (arabic) {
                    "المعيار المحدد للمشروع"
                } else {
                    "The selected project design standard"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ModuleGrid(
    language: AppLanguage,
    onModuleSelected: (ElectromechanicalModule) -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    val modules = listOf(
        DesignModuleItem(
            ElectromechanicalModule.LOAD,
            if (arabic) "الأحمال" else "Loads",
            if (arabic) "جدول الأحمال" else "Load Schedule",
            Icons.Outlined.Power
        ),
        DesignModuleItem(
            ElectromechanicalModule.CURRENT,
            if (arabic) "التيار" else "Current",
            if (arabic) "تيار التصميم" else "Design Current",
            Icons.Outlined.Calculate
        ),
        DesignModuleItem(
            ElectromechanicalModule.CABLE,
            if (arabic) "الكابلات" else "Cables",
            if (arabic) "اختيار مقطع الكابل" else "Conductor Sizing",
            Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            ElectromechanicalModule.VOLTAGE_DROP,
            if (arabic) "هبوط الجهد" else "Voltage Drop",
            if (arabic) "دراسة هبوط الجهد" else "Voltage Drop Study",
            Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            ElectromechanicalModule.BREAKER,
            if (arabic) "القواطع" else "Breakers",
            if (arabic) "اختيار القواطع" else "Breaker Selection",
            Icons.Outlined.Security
        ),
        DesignModuleItem(
            ElectromechanicalModule.SHORT_CIRCUIT,
            if (arabic) "القصر الكهربائي" else "Short Circuit",
            if (arabic) "تيار القصر" else "Fault Current",
            Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            ElectromechanicalModule.PROTECTION,
            if (arabic) "الحماية" else "Protection",
            if (arabic) "دراسة الحماية" else "Protection Study",
            Icons.Outlined.Security
        ),
        DesignModuleItem(
            ElectromechanicalModule.TRANSFORMER,
            if (arabic) "المحولات" else "Transformers",
            if (arabic) "اختيار المحول" else "Transformer Design",
            Icons.Outlined.Memory
        ),
        DesignModuleItem(
            ElectromechanicalModule.GENERATOR,
            if (arabic) "المولدات" else "Generators",
            if (arabic) "اختيار المولد" else "Generator Design",
            Icons.Outlined.Factory
        ),
        DesignModuleItem(
            ElectromechanicalModule.PANEL,
            if (arabic) "اللوحات" else "Panels",
            if (arabic) "تصميم اللوحات" else "Panel Design",
            Icons.Outlined.ElectricalServices
        ),
        DesignModuleItem(
            ElectromechanicalModule.PUMP,
            if (arabic) "المضخات" else "Pumps",
            if (arabic) "التصرف والرفع والقدرة" else "Flow / Head / Power",
            Icons.Outlined.WaterDrop
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        modules.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { item ->
                    DesignModuleCard(
                        modifier = Modifier.weight(1f),
                        item = item,
                        onClick = { onModuleSelected(item.module) }
                    )
                }

                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
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
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title
            )

            Spacer(modifier = Modifier.padding(top = 7.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.padding(top = 2.dp))

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
