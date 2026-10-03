package com.electrical.calculationspro

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WaterDrop

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge

import com.electrical.calculationspro.ui.components.EngineeringAppTopBar

import com.electrical.calculationspro.ui.project.DesignGridItem
import com.electrical.calculationspro.ui.project.ThreeColumnDesignGrid
import com.electrical.calculationspro.ui.project.ProjectDashboardScreen

import com.electrical.calculationspro.ui.screens.EngineeringModule
import com.electrical.calculationspro.ui.screens.EngineeringModuleScreen
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
 * APPLICATION ROOT
 * ============================================================
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


    when (screen) {

        AppScreen.PROJECT_MANAGEMENT -> {

            ProjectManagementScreen(
                language = language,
                project = activeProject,

                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.PROJECT
                },

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

                    screen = AppScreen.HOME
                    mainTab = MainTab.PROJECT
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
                    mainTab = MainTab.DESIGN
                }
            )

            return
        }


        AppScreen.CURRENT -> {

            EngineeringModuleScreen(
                module = EngineeringModule.CURRENT,
                language = language,
                standard = standard,

                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.DESIGN
                }
            )

            return
        }


        AppScreen.CABLE -> {

            EngineeringModuleScreen(
                module = EngineeringModule.CABLE,
                language = language,
                standard = standard,

                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.DESIGN
                }
            )

            return
        }


        AppScreen.VOLTAGE_DROP -> {

            EngineeringModuleScreen(
                module = EngineeringModule.VOLTAGE_DROP,
                language = language,
                standard = standard,

                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.DESIGN
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
                    mainTab = MainTab.DESIGN
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
                    mainTab = MainTab.DESIGN
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
                    mainTab = MainTab.DESIGN
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
                    mainTab = MainTab.DESIGN
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
                    mainTab = MainTab.DESIGN
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
                    mainTab = MainTab.DESIGN
                },

                onOpenSld = {
                    mainTab = MainTab.SLD
                    screen = AppScreen.SLD
                }
            )

            return
        }


        AppScreen.WATER -> {

            EngineeringModuleScreen(
                module = EngineeringModule.WATER,
                language = language,
                standard = standard,

                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.DESIGN
                }
            )

            return
        }


        AppScreen.SEWAGE -> {

            EngineeringModuleScreen(
                module = EngineeringModule.SEWAGE,
                language = language,
                standard = standard,

                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.DESIGN
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
                    mainTab = MainTab.REPORTS
                }
            )

            return
        }


        AppScreen.PUMP -> {

            PumpEngineeringScreen(
                language = language,

                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.DESIGN
                }
            )

            return
        }


        AppScreen.SLD -> {

            SldEditorScreen(
                language = language,

                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.PROJECT
                }
            )

            return
        }


        AppScreen.HOME -> {
            // Main responsive shell
        }
    }


    val configuration =
        LocalConfiguration.current

    val isTablet =
        configuration.screenWidthDp >= 600


    if (isTablet) {

        TabletApplicationShell(
            mainTab = mainTab,
            language = language,
            activeProject = activeProject,

            onSelectTab = { selected ->

                mainTab = selected

                screen =
                    if (selected == MainTab.SLD) {
                        AppScreen.SLD
                    } else {
                        AppScreen.HOME
                    }
            },

            content = {

                MainWorkspaceContent(
                    mainTab = mainTab,
                    language = language,
                    project = activeProject,

                    onOpenProjectManagement = {
                        screen = AppScreen.PROJECT_MANAGEMENT
                    },

                    onOpenCalculators = {
                        mainTab = MainTab.DESIGN
                        screen = AppScreen.HOME
                    },

                    onOpenSld = {
                        mainTab = MainTab.SLD
                        screen = AppScreen.SLD
                    },

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

                    onWater = {
                        screen = AppScreen.WATER
                    },

                    onSewage = {
                        screen = AppScreen.SEWAGE
                    },

                    onPump = {
                        screen = AppScreen.PUMP
                    },

                    onReport = {
                        screen = AppScreen.REPORT
                    }
                )
            }
        )

    } else {

        PhoneApplicationShell(
            mainTab = mainTab,
            language = language,
            activeProject = activeProject,

            onSelectTab = { selected ->

                mainTab = selected

                screen =
                    if (selected == MainTab.SLD) {
                        AppScreen.SLD
                    } else {
                        AppScreen.HOME
                    }
            },

            content = {

                MainWorkspaceContent(
                    mainTab = mainTab,
                    language = language,
                    project = activeProject,

                    onOpenProjectManagement = {
                        screen = AppScreen.PROJECT_MANAGEMENT
                    },

                    onOpenCalculators = {
                        mainTab = MainTab.DESIGN
                        screen = AppScreen.HOME
                    },

                    onOpenSld = {
                        mainTab = MainTab.SLD
                        screen = AppScreen.SLD
                    },

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

                    onWater = {
                        screen = AppScreen.WATER
                    },

                    onSewage = {
                        screen = AppScreen.SEWAGE
                    },

                    onPump = {
                        screen = AppScreen.PUMP
                    },

                    onReport = {
                        screen = AppScreen.REPORT
                    }
                )
            }
        )
    }
}


/**
 * ============================================================
 * MAIN TAB
 * ============================================================
 */
private enum class MainTab {

    PROJECT,

    DESIGN,

    SLD,

    REPORTS
}


/**
 * ============================================================
 * APP SCREEN
 * ============================================================
 */
private enum class AppScreen {

    HOME,

    PROJECT_MANAGEMENT,

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

    WATER,

    SEWAGE,

    REPORT,

    PUMP,

    SLD
}


/**
 * ============================================================
 * TABLET SHELL
 * ============================================================
 */
@Composable
private fun TabletApplicationShell(
    mainTab: MainTab,
    language: AppLanguage,
    activeProject: DesignProject?,
    onSelectTab: (MainTab) -> Unit,
    content: @Composable () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxSize()
    ) {

        ProfessionalNavigationRail(
            selected = mainTab,
            language = language,
            onSelect = onSelectTab
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {

            EngineeringAppTopBar(
                title = mainTabTitle(
                    mainTab,
                    language
                ),

                projectName =
                    activeProject?.projectName,

                onBack = null
            )

            content()
        }
    }
}


/**
 * ============================================================
 * PHONE SHELL
 * ============================================================
 */
@Composable
private fun PhoneApplicationShell(
    mainTab: MainTab,
    language: AppLanguage,
    activeProject: DesignProject?,
    onSelectTab: (MainTab) -> Unit,
    content: @Composable () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        EngineeringAppTopBar(
            title = mainTabTitle(
                mainTab,
                language
            ),

            projectName =
                activeProject?.projectName,

            onBack = null
        )

        content()

        ProfessionalNavigationBar(
            selected = mainTab,
            language = language,
            onSelect = onSelectTab
        )
    }
}


/**
 * ============================================================
 * MAIN CONTENT ROUTER
 * ============================================================
 */
@Composable
private fun MainWorkspaceContent(
    mainTab: MainTab,
    language: AppLanguage,
    project: DesignProject?,

    onOpenProjectManagement: () -> Unit,
    onOpenCalculators: () -> Unit,
    onOpenSld: () -> Unit,

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
    onWater: () -> Unit,
    onSewage: () -> Unit,
    onPump: () -> Unit,
    onReport: () -> Unit
) {

    when (mainTab) {

        MainTab.PROJECT -> {

            ProjectHomeTab(
                language = language,
                project = project,

                onOpenCalculators =
                    onOpenCalculators,

                onOpenSld =
                    onOpenSld,

                onOpenProjectManagement =
                    onOpenProjectManagement
            )
        }


        MainTab.DESIGN -> {

            CalculatorTab(
                language = language,

                onLoad = onLoad,
                onCurrent = onCurrent,
                onCable = onCable,
                onVoltageDrop = onVoltageDrop,
                onBreaker = onBreaker,
                onShortCircuit = onShortCircuit,
                onProtection = onProtection,
                onTransformer = onTransformer,
                onGenerator = onGenerator,
                onPanel = onPanel,
                onWater = onWater,
                onSewage = onSewage,
                onPump = onPump,
                onReport = onReport
            )
        }


        MainTab.SLD -> {

            onOpenSld()
        }


        MainTab.REPORTS -> {

            ReportsHomeTab(
                language = language,
                onOpenReport = onReport
            )
        }
    }
}


/**
 * ============================================================
 * PROFESSIONAL NAVIGATION RAIL
 * ============================================================
 */
@Composable
private fun ProfessionalNavigationRail(
    selected: MainTab,
    language: AppLanguage,
    onSelect: (MainTab) -> Unit
) {

    NavigationRail(
        modifier = Modifier.fillMaxHeight()
    ) {

        Spacer(
            modifier = Modifier.height(10.dp)
        )


        NavigationRailItem(
            selected = selected == MainTab.PROJECT,

            onClick = {
                onSelect(MainTab.PROJECT)
            },

            icon = {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text =
                        if (language == AppLanguage.ARABIC) {
                            "المشروع"
                        } else {
                            "Project"
                        }
                )
            }
        )


        NavigationRailItem(
            selected = selected == MainTab.DESIGN,

            onClick = {
                onSelect(MainTab.DESIGN)
            },

            icon = {
                Icon(
                    imageVector = Icons.Outlined.ElectricalServices,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text =
                        if (language == AppLanguage.ARABIC) {
                            "التصميم"
                        } else {
                            "Design"
                        }
                )
            }
        )


        NavigationRailItem(
            selected = selected == MainTab.SLD,

            onClick = {
                onSelect(MainTab.SLD)
            },

            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = "SLD Designer"
                )
            },

            label = {
                Text(text = "SLD")
            }
        )


        NavigationRailItem(
            selected = selected == MainTab.REPORTS,

            onClick = {
                onSelect(MainTab.REPORTS)
            },

            icon = {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text =
                        if (language == AppLanguage.ARABIC) {
                            "التقارير"
                        } else {
                            "Reports"
                        }
                )
            }
        )
    }
}


/**
 * ============================================================
 * PROFESSIONAL PHONE NAVIGATION
 * ============================================================
 */
@Composable
private fun ProfessionalNavigationBar(
    selected: MainTab,
    language: AppLanguage,
    onSelect: (MainTab) -> Unit
) {

    NavigationBar {

        NavigationBarItem(
            selected = selected == MainTab.PROJECT,

            onClick = {
                onSelect(MainTab.PROJECT)
            },

            icon = {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text =
                        if (language == AppLanguage.ARABIC) {
                            "المشروع"
                        } else {
                            "Project"
                        }
                )
            }
        )


        NavigationBarItem(
            selected = selected == MainTab.DESIGN,

            onClick = {
                onSelect(MainTab.DESIGN)
            },

            icon = {
                Icon(
                    imageVector = Icons.Outlined.ElectricalServices,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text =
                        if (language == AppLanguage.ARABIC) {
                            "التصميم"
                        } else {
                            "Design"
                        }
                )
            }
        )


        NavigationBarItem(
            selected = selected == MainTab.SLD,

            onClick = {
                onSelect(MainTab.SLD)
            },

            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = "SLD Designer"
                )
            },

            label = {
                Text(text = "SLD")
            }
        )


        NavigationBarItem(
            selected = selected == MainTab.REPORTS,

            onClick = {
                onSelect(MainTab.REPORTS)
            },

            icon = {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null
                )
            },

            label = {
                Text(
                    text =
                        if (language == AppLanguage.ARABIC) {
                            "التقارير"
                        } else {
                            "Reports"
                        }
                )
            }
        )
    }
}


/**
 * ============================================================
 * MAIN TAB TITLE
 * ============================================================
 */
private fun mainTabTitle(
    tab: MainTab,
    language: AppLanguage
): String {

    val arabic =
        language == AppLanguage.ARABIC

    return when (tab) {

        MainTab.PROJECT ->
            if (arabic) {
                "المشروع الهندسي"
            } else {
                "Engineering Project"
            }


        MainTab.DESIGN ->
            if (arabic) {
                "التصميم الكهروميكانيكي"
            } else {
                "Electromechanical Design"
            }


        MainTab.SLD ->
            "SLD Designer"


        MainTab.REPORTS ->
            if (arabic) {
                "التقارير الهندسية"
            } else {
                "Engineering Reports"
            }
    }
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
                            if (arabic) {
                                "مشروع هندسي جديد"
                            } else {
                                "New Engineering Project"
                            }
                        }
                        ?: if (arabic) {
                            "لا يوجد مشروع نشط"
                        } else {
                            "No Active Project"
                        },

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
                        if (arabic) {
                            "المصمم"
                        } else {
                            "Designer"
                        },

                    value = "—"
                )


                Spacer(
                    modifier = Modifier.width(32.dp)
                )


                HeaderField(
                    title =
                        if (arabic) {
                            "الاستشاري"
                        } else {
                            "Consultant"
                        },

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
                    modifier = Modifier.height(4.dp)
                )


                Text(
                    text =
                        if (arabic) {
                            "رقم المشروع: ${project.projectNumber}"
                        } else {
                            "Project No.: ${project.projectNumber}"
                        },

                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


/**
 * ============================================================
 * HEADER FIELD
 * ============================================================
 */
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
            style = MaterialTheme.typography.labelMedium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


/**
 * ============================================================
 * PROJECT HOME
 * ============================================================
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
                vertical = 12.dp
            )
    ) {

        Text(
            text =
                if (arabic) {
                    "مساحة العمل الهندسية"
                } else {
                    "Engineering Workspace"
                },

            style =
                MaterialTheme.typography.headlineSmall
        )


        Spacer(
            modifier = Modifier.height(4.dp)
        )


        Text(
            text =
                if (arabic) {
                    "اختر بيئة التصميم التي تريد العمل عليها"
                } else {
                    "Select the engineering workspace"
                },

            style =
                MaterialTheme.typography.bodyMedium
        )


        Spacer(
            modifier = Modifier.height(18.dp)
        )


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            MainWorkspaceCard(
                modifier = Modifier.weight(1f),

                icon = Icons.Outlined.ElectricalServices,

                title =
                    if (arabic) {
                        "التصميم الكهروميكانيكي"
                    } else {
                        "Electromechanical Design"
                    },

                subtitle =
                    if (arabic) {
                        "الكهرباء والمياه والصرف والمضخات"
                    } else {
                        "Electrical, water, sewage and pumps"
                    },

                onClick = onOpenCalculators
            )


            MainWorkspaceCard(
                modifier = Modifier.weight(1f),

                icon = Icons.Outlined.AccountTree,

                title = "SLD Designer",

                subtitle =
                    if (arabic) {
                        "التصميم الأحادي والحسابات المرتبطة"
                    } else {
                        "Single Line Diagram & engineering study"
                    },

                onClick = onOpenSld
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
                        if (arabic) {
                            "المشروع النشط"
                        } else {
                            "Active Project"
                        },

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
                                if (arabic) {
                                    "مشروع جديد"
                                } else {
                                    "New Project"
                                }
                            }
                            ?: if (arabic) {
                                "لا يوجد مشروع نشط"
                            } else {
                                "No Active Project"
                            },

                    style =
                        MaterialTheme.typography.bodyLarge
                )


                if (project != null) {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    Text(
                        text =
                            "Standard: ${project.electricalStandard}",

                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }


                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                OutlinedButton(
                    onClick = onOpenProjectManagement
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null
                    )


                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )


                    Text(
                        text =
                            if (arabic) {
                                "إدارة المشروعات"
                            } else {
                                "Project Management"
                            }
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
 */
@Composable
private fun ProjectManagementScreen(
    language: AppLanguage,
    project: DesignProject?,
    onBack: () -> Unit,
    onOpenProject: (DesignProject) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        EngineeringAppTopBar(
            title =
                if (language == AppLanguage.ARABIC) {
                    "إدارة المشروعات"
                } else {
                    "Project Management"
                },

            projectName = project?.projectName,

            onBack = onBack
        )


        ProjectDashboardScreen(
            language = language,
            onOpenProject = onOpenProject
        )
    }
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

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(42.dp)
                )
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )


            Spacer(
                modifier = Modifier.height(4.dp)
            )


            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


/**
 * ============================================================
 * CALCULATOR GRID
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
    onWater: () -> Unit,
    onSewage: () -> Unit,
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
                    horizontal = 14.dp,
                    vertical = 8.dp
                )
    ) {

        Text(
            text =
                if (arabic) {
                    "التصميم الكهروميكانيكي"
                } else {
                    "Electromechanical Design"
                },

            style =
                MaterialTheme.typography.headlineSmall
        )


        Spacer(
            modifier = Modifier.height(4.dp)
        )


        Text(
            text =
                if (arabic) {
                    "دورة التصميم الهندسي الكاملة"
                } else {
                    "Complete engineering design workflow"
                },

            style =
                MaterialTheme.typography.bodyMedium
        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        ThreeColumnDesignGrid(
            modifier = Modifier.weight(1f),

            items = listOf(

                DesignGridItem(
                    id = "load",

                    title =
                        if (arabic) "الأحمال"
                        else "Loads",

                    subtitle =
                        if (arabic) "جدول الأحمال"
                        else "Load Schedule",

                    icon = {
                        AppIcon(Icons.Outlined.Power)
                    },

                    onClick = onLoad
                ),


                DesignGridItem(
                    id = "current",

                    title =
                        if (arabic) "التيار"
                        else "Current",

                    subtitle =
                        if (arabic) "تيار التصميم"
                        else "Design Current",

                    icon = {
                        AppIcon(Icons.Outlined.Calculate)
                    },

                    onClick = onCurrent
                ),


                DesignGridItem(
                    id = "cable",

                    title =
                        if (arabic) "الكابلات"
                        else "Cables",

                    subtitle =
                        if (arabic) "اختيار مقطع الكابل"
                        else "Conductor Sizing",

                    icon = {
                        AppIcon(Icons.Outlined.Bolt)
                    },

                    onClick = onCable
                ),


                DesignGridItem(
                    id = "voltage_drop",

                    title =
                        if (arabic) "هبوط الجهد"
                        else "Voltage Drop",

                    subtitle =
                        if (arabic) "التحقق من هبوط الجهد"
                        else "Voltage Drop Study",

                    icon = {
                        AppIcon(Icons.Outlined.Bolt)
                    },

                    onClick = onVoltageDrop
                ),


                DesignGridItem(
                    id = "breaker",

                    title =
                        if (arabic) "القواطع"
                        else "Breakers",

                    subtitle =
                        if (arabic) "اختيار والتحقق"
                        else "Selection & Verification",

                    icon = {
                        AppIcon(Icons.Outlined.Power)
                    },

                    onClick = onBreaker
                ),


                DesignGridItem(
                    id = "protection",

                    title =
                        if (arabic) "الحماية"
                        else "Protection",

                    subtitle =
                        if (arabic) "دراسة الحماية"
                        else "Protection Study",

                    icon = {
                        AppIcon(Icons.Outlined.Security)
                    },

                    onClick = onProtection
                ),


                DesignGridItem(
                    id = "short_circuit",

                    title =
                        if (arabic) "القصر الكهربائي"
                        else "Short Circuit",

                    subtitle =
                        if (arabic) "تيار القصر"
                        else "Fault Current",

                    icon = {
                        AppIcon(Icons.Outlined.Bolt)
                    },

                    onClick = onShortCircuit
                ),


                DesignGridItem(
                    id = "panel",

                    title =
                        if (arabic) "اللوحات"
                        else "Panels",

                    subtitle =
                        if (arabic) "تصميم اللوحات"
                        else "Panel Design",

                    icon = {
                        AppIcon(Icons.Outlined.ElectricalServices)
                    },

                    onClick = onPanel
                ),


                DesignGridItem(
                    id = "transformer",

                    title =
                        if (arabic) "المحولات"
                        else "Transformers",

                    subtitle =
                        if (arabic) "اختيار المحول"
                        else "Transformer Design",

                    icon = {
                        AppIcon(Icons.Outlined.Memory)
                    },

                    onClick = onTransformer
                ),


                DesignGridItem(
                    id = "generator",

                    title =
                        if (arabic) "المولدات"
                        else "Generators",

                    subtitle =
                        if (arabic) "تصميم المولد"
                        else "Generator Design",

                    icon = {
                        AppIcon(Icons.Outlined.Factory)
                    },

                    onClick = onGenerator
                ),


                DesignGridItem(
                    id = "water",

                    title =
                        if (arabic) "المياه"
                        else "Water Design",

                    subtitle =
                        if (arabic) {
                            "التصرف والمواسير والمضخات"
                        } else {
                            "Flow / Pipe / TDH / Pump"
                        },

                    icon = {
                        AppIcon(Icons.Outlined.WaterDrop)
                    },

                    onClick = onWater
                ),


                DesignGridItem(
                    id = "sewage",

                    title =
                        if (arabic) "الصرف الصحي"
                        else "Sewage Design",

                    subtitle =
                        if (arabic) {
                            "التصرف وخط الطرد والمضخات"
                        } else {
                            "Flow / Rising Main / Pump"
                        },

                    icon = {
                        AppIcon(Icons.Outlined.WaterDrop)
                    },

                    onClick = onSewage
                ),


                DesignGridItem(
                    id = "pump",

                    title =
                        if (arabic) "المضخات"
                        else "Pumps",

                    subtitle =
                        if (arabic) {
                            "التصرف والرفع والقدرة"
                        } else {
                            "Flow / Head / Power"
                        },

                    icon = {
                        AppIcon(Icons.Outlined.WaterDrop)
                    },

                    onClick = onPump
                ),


                DesignGridItem(
                    id = "report",

                    title =
                        if (arabic) "التقرير"
                        else "Engineering Report",

                    subtitle =
                        if (arabic) {
                            "تقرير الحسابات"
                        } else {
                            "Calculation Report"
                        },

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
 * REPORTS HOME
 * ============================================================
 */
@Composable
private fun ReportsHomeTab(
    language: AppLanguage,
    onOpenReport: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        Text(
            text =
                if (arabic) {
                    "التقارير الهندسية"
                } else {
                    "Engineering Reports"
                },

            style =
                MaterialTheme.typography.headlineMedium
        )


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        Text(
            text =
                if (arabic) {
                    "تقارير الحسابات والدراسات الهندسية"
                } else {
                    "Calculation and engineering study reports"
                },

            style =
                MaterialTheme.typography.bodyLarge
        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        Button(
            onClick = onOpenReport
        ) {

            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null
            )


            Spacer(
                modifier = Modifier.width(8.dp)
            )


            Text(
                text =
                    if (arabic) {
                        "فتح التقارير"
                    } else {
                        "Open Reports"
                    }
            )
        }
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
