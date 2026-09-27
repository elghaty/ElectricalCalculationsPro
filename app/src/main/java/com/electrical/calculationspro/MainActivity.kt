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
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves

import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
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
 * MAIN APPLICATION SHELL
 * ============================================================
 *
 * One navigation owner only.
 *
 * The application has three top-level tabs:
 *
 * 1. Project
 * 2. Electromechanical Calculators
 * 3. SLD Design
 *
 * Individual calculators are opened from the second tab.
 *
 * SLD is intentionally independent from the calculator dashboard.
 */
@Composable
private fun ElectricalCalculationsProApp() {

    var language by remember {
        mutableStateOf(
            AppLanguage.ARABIC
        )
    }

    var activeProject by remember {
        mutableStateOf<DesignProject?>(
            DesignProjects.getActive()
        )
    }

    var standard by remember {
        mutableStateOf(
            Standard.IEC
        )
    }

    var mainTab by remember {
        mutableStateOf(
            MainTab.PROJECT
        )
    }

    var screen by remember {
        mutableStateOf(
            AppScreen.HOME
        )
    }

    LaunchedEffect(Unit) {

        val project =
            DesignProjectCoreBridge
                .getActiveProject()

        activeProject =
            project

        standard =
            project
                ?.electricalStandard
                ?: Standard.IEC
    }

    /*
     * ----------------------------------------------------------
     * FULL SCREEN CALCULATORS
     * ----------------------------------------------------------
     */

    when (screen) {

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
            /*
             * Continue below.
             */
        }
    }


    /*
     * ----------------------------------------------------------
     * MAIN HOME SHELL
     * ----------------------------------------------------------
     */

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        /*
         * Fixed project header.
         *
         * This is visible on all three main tabs.
         */

        ProjectHeader(
            project = activeProject,
            language = language
        )


        /*
         * Main content.
         */

        when (mainTab) {

            MainTab.PROJECT -> {

                ProjectTab(
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

                SldTab(
                    language = language,

                    onOpenSld = {
                        screen = AppScreen.SLD
                    }
                )
            }
        }


        /*
         * Three and ONLY three top-level navigation tabs.
         */

        MainNavigationBar(
            selected = mainTab,
            language = language,
            onSelect = { selected ->

                mainTab =
                    selected

                screen =
                    AppScreen.HOME
            }
        )
    }
}


/**
 * ============================================================
 * MAIN TAB ENUM
 * ============================================================
 */

private enum class MainTab {

    PROJECT,

    CALCULATORS,

    SLD
}


/**
 * ============================================================
 * APPLICATION SCREEN ENUM
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 12.dp,
                    bottom = 6.dp
                )
    ) {

        Column(
            modifier =
                Modifier
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
                    MaterialTheme.typography
                        .headlineSmall
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
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
                    value =
                        "—"
                )

                Spacer(
                    modifier =
                        Modifier.width(24.dp)
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
                            ?.ifBlank {
                                "—"
                            }
                            ?: "—"
                )
            }


            if (
                project != null &&
                project.projectNumber.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
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
                MaterialTheme.typography
                    .labelMedium
        )

        Text(
            text = value,
            style =
                MaterialTheme.typography
                    .bodyMedium
        )
    }
}


/**
 * ============================================================
 * PROJECT TAB
 * ============================================================
 */

@Composable
private fun ProjectTab(
    language: AppLanguage,
    onOpenProject: (DesignProject) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 14.dp
                )
    ) {

        Text(
            text =
                if (
                    language == AppLanguage.ARABIC
                )
                    "المشروع"
                else
                    "Project",
            style =
                MaterialTheme.typography
                    .titleLarge
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                if (
                    language == AppLanguage.ARABIC
                )
                    "إدارة المشروع والبيانات الأساسية"
                else
                    "Project management and basic information",
            style =
                MaterialTheme.typography
                    .bodyMedium
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        ProjectDashboardScreen(
            language = language,
            onOpenProject = onOpenProject
        )
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
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 14.dp
                )
    ) {

        Text(
            text =
                if (arabic)
                    "Electromechanical Calculators"
                else
                    "Electromechanical Calculators",
            style =
                MaterialTheme.typography
                    .headlineSmall
        )

        Text(
            text =
                if (arabic)
                    "الحسابات والتصميم الكهروميكانيكي"
                else
                    "Engineering calculations and design tools",
            style =
                MaterialTheme.typography
                    .bodyMedium
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        FourColumnDesignGrid(

            modifier =
                Modifier.weight(1f),

            items =
                listOf(

                    DesignGridItem(
                        id = "load",
                        title =
                            if (arabic)
                                "الأحمال"
                            else
                                "Loads",
                        subtitle = "Load Schedule",
                        icon = {
                            AppIcon(
                                Icons.Outlined.Power
                            )
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
                            AppIcon(
                                Icons.Outlined.Calculate
                            )
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
                            AppIcon(
                                Icons.Outlined.Cable
                            )
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
                            AppIcon(
                                Icons.Outlined.Bolt
                            )
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
                            AppIcon(
                                Icons.Outlined.Power
                            )
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
                            AppIcon(
                                Icons.Outlined.Bolt
                            )
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
                            AppIcon(
                                Icons.Outlined.Settings
                            )
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
                            AppIcon(
                                Icons.Outlined.Memory
                            )
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
                            AppIcon(
                                Icons.Outlined.Factory
                            )
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
                            AppIcon(
                                Icons.Outlined.ElectricalServices
                            )
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
                            AppIcon(
                                Icons.Outlined.Waves
                            )
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
                            AppIcon(
                                Icons.Outlined.Description
                            )
                        },
                        onClick = onReport
                    )
                )
        )
    }
}


/**
 * ============================================================
 * SLD TAB
 * ============================================================
 */

@Composable
private fun SldTab(
    language: AppLanguage,
    onOpenSld: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(18.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Icon(
            imageVector =
                Icons.Outlined.AccountTree,
            contentDescription = null,
            modifier =
                Modifier.size(72.dp)
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                if (arabic)
                    "SLD Design"
                else
                    "SLD Design",
            style =
                MaterialTheme.typography
                    .headlineMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                if (arabic)
                    "التصميم الأحادي للمنظومة الكهربائية"
                else
                    "Professional Single Line Diagram",
            style =
                MaterialTheme.typography
                    .bodyLarge
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        if (arabic)
                            "فتح محرر SLD"
                        else
                            "Open SLD Editor",
                    style =
                        MaterialTheme.typography
                            .titleLarge
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        if (arabic)
                            "المحرر منفصل عن شاشات الحسابات."
                        else
                            "The SLD editor is independent from the calculation screens.",
                    style =
                        MaterialTheme.typography
                            .bodyMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
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
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            if (arabic)
                                "SLD Design"
                            else
                                "SLD Design"
                    )
                }
            }
        }
    }
}


/**
 * ============================================================
 * BOTTOM NAVIGATION
 * ============================================================
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
                onSelect(
                    MainTab.PROJECT
                )
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
                onSelect(
                    MainTab.CALCULATORS
                )
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
                onSelect(
                    MainTab.SLD
                )
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
                    text =
                        "SLD Design"
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
        modifier =
            Modifier.size(30.dp)
    )
}
