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
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard

/**
 * Unified engineering workspace for electromechanical calculations.
 *
 * IMPORTANT:
 * - This screen contains navigation/presentation only.
 * - It does not implement engineering calculations.
 * - Existing calculation engines/screens remain the source of calculations.
 * - SLD is intentionally NOT included here.
 */
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
    var selectedModule by rememberSaveable {
        mutableStateOf<ElectromechanicalModule?>(null)
    }

    val module = selectedModule

    if (module != null && onOpenModule != null) {
        onOpenModule(module)
        return
    }

    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (arabic) {
                        "التصميم الكهروميكانيكي"
                    } else {
                        "Electromechanical Design"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(
                    modifier = Modifier.padding(top = 3.dp)
                )

                Text(
                    text = if (arabic) {
                        "منظومة الحسابات الهندسية"
                    } else {
                        "Engineering Calculation Workspace"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            TextButton(
                onClick = onBack
            ) {
                Text(
                    text = if (arabic) {
                        "رجوع"
                    } else {
                        "Back"
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.padding(top = 10.dp)
        )

        StandardCard(
            language = language,
            standard = standard
        )

        Spacer(
            modifier = Modifier.padding(top = 14.dp)
        )

        Text(
            text = if (arabic) {
                "دورة التصميم الكهربائي"
            } else {
                "Electrical Design Workflow"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.padding(top = 8.dp)
        )

        ModuleGrid(
            language = language,
            onModuleSelected = {
                selectedModule = it
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
                text = if (arabic) {
                    "الكود / المعيار"
                } else {
                    "Design Standard"
                },
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = standard.name,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = if (arabic) {
                    "سيتم استخدام المعيار المحدد في دورة التصميم والحسابات."
                } else {
                    "The selected standard is carried through the design workflow."
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
            module = ElectromechanicalModule.LOAD,
            title = if (arabic) "الأحمال" else "Loads",
            subtitle = if (arabic) "Load Schedule" else "Load Schedule",
            icon = Icons.Outlined.Power
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.CURRENT,
            title = if (arabic) "التيار" else "Current",
            subtitle = if (arabic) "Design Current" else "Design Current",
            icon = Icons.Outlined.Calculate
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.CABLE,
            title = if (arabic) "الكابلات" else "Cables",
            subtitle = if (arabic) "Conductor Sizing" else "Conductor Sizing",
            icon = Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.VOLTAGE_DROP,
            title = if (arabic) "هبوط الجهد" else "Voltage Drop",
            subtitle = if (arabic) "Voltage Drop Study" else "Voltage Drop Study",
            icon = Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.BREAKER,
            title = if (arabic) "القواطع" else "Breakers",
            subtitle = if (arabic) "Breaker Selection" else "Breaker Selection",
            icon = Icons.Outlined.Security
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.SHORT_CIRCUIT,
            title = if (arabic) "القصر الكهربائي" else "Short Circuit",
            subtitle = if (arabic) "Fault Current" else "Fault Current",
            icon = Icons.Outlined.Bolt
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.PROTECTION,
            title = if (arabic) "الحماية" else "Protection",
            subtitle = if (arabic) "Protection Study" else "Protection Study",
            icon = Icons.Outlined.Security
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.TRANSFORMER,
            title = if (arabic) "المحولات" else "Transformers",
            subtitle = if (arabic) "Transformer Design" else "Transformer Design",
            icon = Icons.Outlined.Memory
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.GENERATOR,
            title = if (arabic) "المولدات" else "Generators",
            subtitle = if (arabic) "Generator Design" else "Generator Design",
            icon = Icons.Outlined.Factory
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.PANEL,
            title = if (arabic) "اللوحات" else "Panels",
            subtitle = if (arabic) "Panel Design" else "Panel Design",
            icon = Icons.Outlined.ElectricalServices
        ),
        DesignModuleItem(
            module = ElectromechanicalModule.PUMP,
            title = if (arabic) "المضخات" else "Pumps",
            subtitle = if (arabic) "Flow / Head / Power" else "Flow / Head / Power",
            icon = Icons.Outlined.WaterDrop
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
                        onClick = {
                            onModuleSelected(item.module)
                        }
                    )
                }

                repeat(4 - row.size) {
                    Spacer(
                        modifier = Modifier.weight(1f)
                    )
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
                .padding(
                    horizontal = 10.dp,
                    vertical = 14.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title
            )

            Spacer(
                modifier = Modifier.padding(top = 7.dp)
            )

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
