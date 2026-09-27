package com.electrical.calculationspro

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.DesignProjects

import com.electrical.calculationspro.ui.project.DesignGridItem
import com.electrical.calculationspro.ui.project.FourColumnDesignGrid
import com.electrical.calculationspro.ui.project.ProjectDashboardScreen

import com.electrical.calculationspro.ui.screens.ConductorSizingScreen
import com.electrical.calculationspro.ui.screens.CurrentCalculationScreen
import com.electrical.calculationspro.ui.screens.EngineeringModule
import com.electrical.calculationspro.ui.screens.EngineeringModuleScreen
import com.electrical.calculationspro.ui.screens.ProfessionalVoltageDropScreen
import com.electrical.calculationspro.ui.screens.PumpEngineeringScreen

import com.electrical.calculationspro.ui.screens.sld.SldEditorScreen
import com.electrical.calculationspro.ui.theme.ElectricalCalculationsProTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ElectricalCalculationsProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ElectricalCalculationsProApp()
                }
            }
        }
    }
}


/**
 * ============================================================
 * MAIN APPLICATION
 * ============================================================
 *
 * Navigation hierarchy:
 *
 * Main
 * ├── Project
 * ├── Electromechanical Calculators
 * └── SLD Design
 *
 * Calculators and SLD are separate top-level destinations.
 *
 * No calculator is embedded inside the SLD screen.
 * No SLD screen is embedded inside the calculator dashboard.
 */
@Composable
private fun ElectricalCalculationsProApp() {

    var language by remember {
        mutableStateOf(AppLanguage.ARABIC)
    }

    var activeProject by remember {
        mutableStateOf<DesignProject?>(
            DesignProjectCoreBridge.getActiveProject()
        )
    }

    var standard by remember {
        mutableStateOf(
            activeProject?.electricalStandard ?: Standard.IEC
        )
    }

    var mainTab by remember {
        mutableStateOf(MainTab.PROJECT)
    }

    var screen by remember {
        mutableStateOf(AppScreen.HOME)
    }

    LaunchedEffect(Unit) {

        val project =
            DesignProjectCoreBridge.getActiveProject()

        activeProject = project

        standard =
            project?.electricalStandard ?: Standard.IEC
    }

    /*
     * ----------------------------------------------------------
     * FULL SCREEN DESTINATIONS
     * ----------------------------------------------------------
     *
     * These screens replace the main shell completely.
     */

    when (screen) {

        AppScreen.LOAD -> {

            EngineeringModuleScreen(
                module = EngineeringModule.LOAD,
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.CURRENT -> {

            CurrentCalculationScreen(
                language = language,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.CABLE -> {

            ConductorSizingScreen(
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.VOLTAGE_DROP -> {

            ProfessionalVoltageDropScreen(
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.BREAKER -> {

            EngineeringModuleScreen(
                module = EngineeringModule.BREAKER,
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.SHORT_CIRCUIT -> {

            EngineeringModuleScreen(
                module = EngineeringModule.SHORT_CIRCUIT,
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.PROTECTION -> {

            EngineeringModuleScreen(
                module = EngineeringModule.PROTECTION,
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.TRANSFORMER -> {

            EngineeringModuleScreen(
                module = EngineeringModule.TRANSFORMER,
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.GENERATOR -> {

            EngineeringModuleScreen(
                module = EngineeringModule.GENERATOR,
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.PANEL -> {

            EngineeringModuleScreen(
                module = EngineeringModule.PANEL,
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                },
                onOpenSld = {
                    screen = AppScreen.SLD
                }
            )

            return
        }

        AppScreen.REPORT -> {

            EngineeringModuleScreen(
                module = EngineeringModule.REPORT,
                language = language,
                standard = standard,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.PUMP -> {

            PumpEngineeringScreen(
                language = language,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.CALCULATORS
                }
            )

            return
        }

        AppScreen.SLD -> {

            SldEditorScreen(
                language = language,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.SLD
                }
            )

            return
        }

        AppScreen.HOME -> {
            // Continue to the main shell.
        }
    }


    /*
     * ----------------------------------------------------------
     * MAIN SHELL
     * ----------------------------------------------------------
     */

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        ProjectHeader(
            project = activeProject,
            language = language
        )

        when (mainTab) {

            MainTab.PROJECT -> {

                ProjectHomeTab(
                    language = language,
                    project = activeProject,

                    onOpenCalculators = {
                        mainTab = MainTab.CALCULATORS
                        screen = AppScreen.HOME
                    },

                    onOpenSld = {
                        mainTab = MainTab.SLD
                        screen = AppScreen.HOME
                    },

                    onOpenProjectManagement = {
                        ProjectManagementTab(
                            language = language,
                            onOpenProject = { project ->

                                DesignProjectCoreBridge
                                    .selectProject(project.id)

                                activeProject =
                                    DesignProjectCoreBridge
                                        .getActiveProject()

                                standard =
                                    activeProject
                                        ?.electricalStandard
                                        ?: Standard.IEC
                            }
                        )
                    }
                )
            }

            MainTab.CALCULATORS -> {

                CalculatorTab(
                    language = language,

                    onLoad = {
                        screen = AppScreen.LOAD
                    },

                    onCurrent = {
                        screen = AppScreen.CURRENT
                    },

                    onCable = {
                        screen = AppScreen.CABLE
                    },

                    onVoltageDrop = {
                        screen = AppScreen.VOLTAGE_DROP
                    },

                    onBreaker = {
                        screen = AppScreen.BREAKER
                    },

                    onShortCircuit = {
                        screen = AppScreen.SHORT_CIRCUIT
                    },

                    onProtection = {
                        screen = AppScreen.PROTECTION
                    },

                    onTransformer = {
                        screen = AppScreen.TRANSFORMER
                    },

                    onGenerator = {
                        screen = AppScreen.GENERATOR
                    },

                    onPanel = {
                        screen = AppScreen.PANEL
                    },

                    onPump = {
                        screen = AppScreen.PUMP
                    },

                    onReport = {
                        screen = AppScreen.REPORT
                    }
                )
            }

            MainTab.SLD -> {

                SldHomeTab(
                    language = language,
                    onOpenSld = {
                        screen = AppScreen.SLD
                    }
                )
            }
        }

        MainNavigationBar(
            selected = mainTab,
            language = language,
            onSelect = { selected ->

                mainTab = selected
                screen = AppScreen.HOME
            }
        )
    }
}


/**
 * ============================================================
 * MAIN TABS
 * ============================================================
 */

private enum class MainTab {

    PROJECT,

    CALCULATORS,

    SLD
}


/**
 * ============================================================
 * APPLICATION SCREENS
 * ============================================================
 */

private enum class AppScreen {

    HOME,

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

    REPORT,

    PUMP,

    SLD
}


/**
 * ============================================================
 * PROJECT HEADER
 * ============================================================
 */

@Composable
private fun ProjectHeader(
    project: DesignProject?,
    language: AppLanguage
) {

    val arabic =
        language == AppLanguage.ARABIC

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 12.dp,
                bottom = 6.dp
            ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 14.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    project
                        ?.projectName
                        ?.ifBlank {
                            if (arabic)
                                "مشروع هندسي جديد"
                            else
                                "New Engineering Project"
                        }
                        ?: if (arabic)
                            "لا يوجد مشروع نشط"
                        else
                            "No Active Project",
                style =
                    MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                HeaderField(
                    title =
                        if (arabic)
                            "المصمم"
                        else
                            "Designer",
                    value = "—"
                )

                Spacer(
                    modifier = Modifier.width(32.dp)
                )

                HeaderField(
                    title =
                        if (arabic)
                            "الاستشاري"
                        else
                            "Consultant",
                    value =
                        project
                            ?.consultantName
                            ?.ifBlank { "—" }
                            ?: "—"
                )
            }

            if (
                project != null &&
                project.projectNumber.isNotBlank()
            ) {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        if (arabic)
                            "رقم المشروع: ${project.projectNumber}"
                        else
                            "Project No.: ${project.projectNumber}",
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
private fun HeaderField(
    title: String,
    value: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = title,
            style =
                MaterialTheme.typography.labelMedium
        )

        Text(
            text = value,
            style =
                MaterialTheme.typography.bodyMedium
        )
    }
}


/**
 * ============================================================
 * PROJECT HOME
 * ============================================================
 *
 * This is now the real first screen.
 *
 * It does NOT embed ProjectDashboardScreen.
 * It shows the project information once and then provides
 * two clear engineering entry points:
 *
 * 1. Electromechanical Calculators
 * 2. SLD Design
 */
@Composable
private fun ProjectHomeTab(
    language: AppLanguage,
    project: DesignProject?,
    onOpenCalculators: () -> Unit,
    onOpenSld: () -> Unit,
    onOpenProjectManagement: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 18.dp,
                vertical = 10.dp
            )
    ) {

        Text(
            text =
                if (arabic)
                    "Engineering Design"
                else
                    "Engineering Design",
            style =
                MaterialTheme.typography.titleLarge
        )

        Text(
            text =
                if (arabic)
                    "اختر نظام التصميم الذي تريد العمل عليه"
                else
                    "Select the engineering design workspace",
            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            MainWorkspaceCard(
                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Outlined.ElectricalServices,

                title =
                    "Electromechanical Calculators",

                subtitle =
                    if (arabic)
                        "الحسابات والتصميم"
                    else
                        "Calculations & Design",

                onClick =
                    onOpenCalculators
            )

            MainWorkspaceCard(
                modifier =
                    Modifier.weight(1f),

                icon =
                    Icons.Outlined.AccountTree,

                title =
                    "SLD Design",

                subtitle =
                    if (arabic)
                        "Single Line Diagram"
                    else
                        "Single Line Diagram",

                onClick =
                    onOpenSld
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {

                Text(
                    text =
                        if (arabic)
                            "المشروع الحالي"
                        else
                            "Current Project",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        project
                            ?.projectName
                            ?.ifBlank {
                                if (arabic)
                                    "مشروع جديد"
                                else
                                    "New Project"
                            }
                            ?: if (arabic)
                                "لا يوجد مشروع نشط"
                            else
                                "No Active Project",
                    style =
                        MaterialTheme.typography.bodyLarge
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedButton(
                    onClick =
                        onOpenProjectManagement
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Description,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        text =
                            if (arabic)
                                "إدارة المشروعات"
                            else
                                "Project Management"
                    )
                }
            }
        }
    }
}


/**
 * ============================================================
 * PROJECT MANAGEMENT
 * ============================================================
 *
 * The existing ProjectDashboardScreen is retained.
 * It is opened explicitly instead of being nested inside
 * the first screen.
 */
@Composable
private fun ProjectManagementTab(
    language: AppLanguage,
    onOpenProject: (DesignProject) -> Unit
) {

    ProjectDashboardScreen(
        language = language,
        onOpenProject = onOpenProject
    )
}


/**
 * ============================================================
 * MAIN WORKSPACE CARD
 * ============================================================
 */

@Composable
private fun MainWorkspaceCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(58.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = subtitle,
                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }
}


/**
 * ============================================================
 * CALCULATORS TAB
 * ============================================================
 */

@Composable
private fun CalculatorTab(
    language: AppLanguage,

    onLoad: () -> Unit,
    onCurrent: () -> Unit,
    onCable: () -> Unit,
    onVoltageDrop: () -> Unit,
    onBreaker: () -> Unit,
    onShortCircuit: () -> Unit,
    onProtection: () -> Unit,
    onTransformer: () -> Unit,
    onGenerator: () -> Unit,
    onPanel: () -> Unit,
    onPump: () -> Unit,
    onReport: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 14.dp
            )
    ) {

        Text(
            text = "Electromechanical Calculators",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Text(
            text =
                if (arabic)
                    "الحسابات والتصميم الكهروميكانيكي"
                else
                    "Engineering calculations and design tools",
            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        FourColumnDesignGrid(
            modifier = Modifier.weight(1f),
            items = listOf(

                DesignGridItem(
                    id = "load",
                    title =
                        if (arabic)
                            "الأحمال"
                        else
                            "Loads",
                    subtitle = "Load Schedule",
                    icon = {
                        AppIcon(Icons.Outlined.Power)
                    },
                    onClick = onLoad
                ),

                DesignGridItem(
                    id = "current",
                    title =
                        if (arabic)
                            "التيار"
                        else
                            "Current",
                    subtitle = "Design Current",
                    icon = {
                        AppIcon(Icons.Outlined.Calculate)
                    },
                    onClick = onCurrent
                ),

                DesignGridItem(
                    id = "cable",
                    title =
                        if (arabic)
                            "الكابلات"
                        else
                            "Cables",
                    subtitle = "Conductor Sizing",
                    icon = {
                        AppIcon(Icons.Outlined.Bolt)
                    },
                    onClick = onCable
                ),

                DesignGridItem(
                    id = "voltage_drop",
                    title =
                        if (arabic)
                            "هبوط الجهد"
                        else
                            "Voltage Drop",
                    subtitle = "Voltage Drop",
                    icon = {
                        AppIcon(Icons.Outlined.Bolt)
                    },
                    onClick = onVoltageDrop
                ),

                DesignGridItem(
                    id = "breaker",
                    title =
                        if (arabic)
                            "القواطع"
                        else
                            "Breakers",
                    subtitle = "Breaker Selection",
                    icon = {
                        AppIcon(Icons.Outlined.Power)
                    },
                    onClick = onBreaker
                ),

                DesignGridItem(
                    id = "short_circuit",
                    title =
                        if (arabic)
                            "القصر الكهربائي"
                        else
                            "Short Circuit",
                    subtitle = "Fault Current",
                    icon = {
                        AppIcon(Icons.Outlined.Bolt)
                    },
                    onClick = onShortCircuit
                ),

                DesignGridItem(
                    id = "protection",
                    title =
                        if (arabic)
                            "الحماية"
                        else
                            "Protection",
                    subtitle = "Protection Study",
                    icon = {
                        AppIcon(Icons.Outlined.Security)
                    },
                    onClick = onProtection
                ),

                DesignGridItem(
                    id = "transformer",
                    title =
                        if (arabic)
                            "المحولات"
                        else
                            "Transformers",
                    subtitle = "Transformer Design",
                    icon = {
                        AppIcon(Icons.Outlined.Memory)
                    },
                    onClick = onTransformer
                ),

                DesignGridItem(
                    id = "generator",
                    title =
                        if (arabic)
                            "المولدات"
                        else
                            "Generators",
                    subtitle = "Generator Design",
                    icon = {
                        AppIcon(Icons.Outlined.Factory)
                    },
                    onClick = onGenerator
                ),

                DesignGridItem(
                    id = "panel",
                    title =
                        if (arabic)
                            "اللوحات"
                        else
                            "Panels",
                    subtitle = "Panel Design",
                    icon = {
                        AppIcon(Icons.Outlined.ElectricalServices)
                    },
                    onClick = onPanel
                ),

                DesignGridItem(
                    id = "pump",
                    title =
                        if (arabic)
                            "المضخات"
                        else
                            "Pumps",
                    subtitle = "Flow / Head / Power",
                    icon = {
                        AppIcon(Icons.Outlined.WaterDrop)
                    },
                    onClick = onPump
                ),

                DesignGridItem(
                    id = "report",
                    title =
                        if (arabic)
                            "التقرير"
                        else
                            "Engineering Report",
                    subtitle = "Calculation Report",
                    icon = {
                        AppIcon(Icons.Outlined.Description)
                    },
                    onClick = onReport
                )
            )
        )
    }
}


/**
 * ============================================================
 * SLD HOME TAB
 * ============================================================
 *
 * This is only the entry screen.
 * The actual SLD editor remains in:
 *
 * ui/screens/sld/SldEditorScreen.kt
 */
@Composable
private fun SldHomeTab(
    language: AppLanguage,
    onOpenSld: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Icon(
            imageVector =
                Icons.Outlined.AccountTree,
            contentDescription = "SLD Design",
            modifier = Modifier.size(72.dp)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "SLD Design",
            style =
                MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                if (arabic)
                    "التصميم الأحادي للمنظومة الكهربائية"
                else
                    "Professional Single Line Diagram",
            style =
                MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = onOpenSld
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.AccountTree,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "Open SLD Design"
            )
        }
    }
}


/**
 * ============================================================
 * MAIN NAVIGATION
 * ============================================================
 *
 * Exactly three top-level tabs.
 */
@Composable
private fun MainNavigationBar(
    selected: MainTab,
    language: AppLanguage,
    onSelect: (MainTab) -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    NavigationBar {

        NavigationBarItem(
            selected =
                selected == MainTab.PROJECT,

            onClick = {
                onSelect(MainTab.PROJECT)
            },

            icon = {
                Icon(
                    imageVector =
                        Icons.Outlined.Description,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text =
                        if (arabic)
                            "المشروع"
                        else
                            "Project"
                )
            }
        )

        NavigationBarItem(
            selected =
                selected == MainTab.CALCULATORS,

            onClick = {
                onSelect(MainTab.CALCULATORS)
            },

            icon = {
                Icon(
                    imageVector =
                        Icons.Outlined.ElectricalServices,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text =
                        if (arabic)
                            "الحسابات"
                        else
                            "Calculators"
                )
            }
        )

        NavigationBarItem(
            selected =
                selected == MainTab.SLD,

            onClick = {
                onSelect(MainTab.SLD)
            },

            icon = {
                Icon(
                    imageVector =
                        Icons.Outlined.AccountTree,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text = "SLD Design"
                )
            }
        )
    }
}


/**
 * ============================================================
 * COMMON ICON
 * ============================================================
 */

@Composable
private fun AppIcon(
    imageVector: ImageVector
) {

    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(30.dp)
    )
}
