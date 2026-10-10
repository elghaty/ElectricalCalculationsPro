
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Folder
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
import com.electrical.calculationspro.ui.project.ProjectDashboardScreen
import com.electrical.calculationspro.ui.screens.CurrentCalculationScreen
import com.electrical.calculationspro.ui.screens.ElectromechanicalDesignScreen
import com.electrical.calculationspro.ui.screens.ElectromechanicalModule
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

private enum class MainTab {
    PROJECT,
    DESIGN,
    SLD,
    REPORTS
}

private enum class AppScreen {
    HOME,
    PROJECT_MANAGEMENT,
    CURRENT,
    VOLTAGE_DROP,
    PUMP,
    MODULE_NOT_CONNECTED,
    SLD
}

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

    var selectedModule by remember {
        mutableStateOf<ElectromechanicalModule?>(null)
    }

    LaunchedEffect(Unit) {
        val project = DesignProjectCoreBridge.getActiveProject()
        activeProject = project
        standard = project?.electricalStandard ?: Standard.IEC
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
                    DesignProjectCoreBridge.selectProject(project.id)
                    activeProject = DesignProjectCoreBridge.getActiveProject()
                    standard = activeProject?.electricalStandard ?: Standard.IEC
                    screen = AppScreen.HOME
                    mainTab = MainTab.PROJECT
                }
            )
            return
        }

        AppScreen.CURRENT -> {
            CurrentCalculationScreen(
                language = language,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.DESIGN
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
                    mainTab = MainTab.DESIGN
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

        AppScreen.MODULE_NOT_CONNECTED -> {
            ModuleNotConnectedScreen(
                language = language,
                module = selectedModule,
                onBack = {
                    screen = AppScreen.HOME
                    mainTab = MainTab.DESIGN
                }
            )
            return
        }

        AppScreen.HOME -> Unit
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    if (isTablet) {
        TabletApplicationShell(
            mainTab = mainTab,
            language = language,
            activeProject = activeProject,
            onSelectTab = { selected ->
                mainTab = selected
                screen = if (selected == MainTab.SLD) {
                    AppScreen.SLD
                } else {
                    AppScreen.HOME
                }
            }
        ) {
            MainWorkspaceContent(
                mainTab = mainTab,
                language = language,
                project = activeProject,
                standard = standard,
                onOpenProjectManagement = {
                    screen = AppScreen.PROJECT_MANAGEMENT
                },
                onOpenModule = { module ->
                    selectedModule = module
                    when (module) {
                        ElectromechanicalModule.CURRENT ->
                            screen = AppScreen.CURRENT

                        ElectromechanicalModule.VOLTAGE_DROP ->
                            screen = AppScreen.VOLTAGE_DROP

                        ElectromechanicalModule.PUMP ->
                            screen = AppScreen.PUMP

                        else ->
                            screen = AppScreen.MODULE_NOT_CONNECTED
                    }
                },
                onOpenSld = {
                    screen = AppScreen.SLD
                    mainTab = MainTab.SLD
                }
            )
        }
    } else {
        PhoneApplicationShell(
            mainTab = mainTab,
            language = language,
            activeProject = activeProject,
            onSelectTab = { selected ->
                mainTab = selected
                screen = if (selected == MainTab.SLD) {
                    AppScreen.SLD
                } else {
                    AppScreen.HOME
                }
            }
        ) {
            MainWorkspaceContent(
                mainTab = mainTab,
                language = language,
                project = activeProject,
                standard = standard,
                onOpenProjectManagement = {
                    screen = AppScreen.PROJECT_MANAGEMENT
                },
                onOpenModule = { module ->
                    selectedModule = module
                    when (module) {
                        ElectromechanicalModule.CURRENT ->
                            screen = AppScreen.CURRENT

                        ElectromechanicalModule.VOLTAGE_DROP ->
                            screen = AppScreen.VOLTAGE_DROP

                        ElectromechanicalModule.PUMP ->
                            screen = AppScreen.PUMP

                        else ->
                            screen = AppScreen.MODULE_NOT_CONNECTED
                    }
                },
                onOpenSld = {
                    screen = AppScreen.SLD
                    mainTab = MainTab.SLD
                }
            )
        }
    }
}

@Composable
private fun MainWorkspaceContent(
    mainTab: MainTab,
    language: AppLanguage,
    project: DesignProject?,
    standard: Standard,
    onOpenProjectManagement: () -> Unit,
    onOpenModule: (ElectromechanicalModule) -> Unit,
    onOpenSld: () -> Unit
) {
    when (mainTab) {
        MainTab.PROJECT -> {
            ProjectHomeTab(
                language = language,
                project = project,
                onOpenProjectManagement = onOpenProjectManagement,
                onOpenDesign = {},
                onOpenSld = onOpenSld
            )
        }

        MainTab.DESIGN -> {
            ElectromechanicalDesignScreen(
                language = language,
                standard = standard,
                onBack = {
                    // The design hub is the main workspace tab.
                },
                onOpenModule = onOpenModule
            )
        }

        MainTab.SLD -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                WorkspaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.AccountTree,
                    title = "SLD Designer",
                    subtitle = if (language == AppLanguage.ARABIC) {
                        "فتح المصمم الأحادي"
                    } else {
                        "Open Single Line Diagram Designer"
                    },
                    onClick = onOpenSld
                )
            }
        }

        MainTab.REPORTS -> {
            ReportsHomeTab(language = language)
        }
    }
}

@Composable
private fun ProjectHomeTab(
    language: AppLanguage,
    project: DesignProject?,
    onOpenProjectManagement: () -> Unit,
    onOpenDesign: () -> Unit,
    onOpenSld: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (arabic) "مساحة العمل الهندسية" else "Engineering Workspace",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WorkspaceCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.ElectricalServices,
                title = if (arabic) "التصميم الكهروميكانيكي" else "Electromechanical Design",
                subtitle = if (arabic) {
                    "الكهرباء والمياه والصرف والمضخات"
                } else {
                    "Electrical, water, sewage and pumps"
                },
                onClick = onOpenDesign
            )

            WorkspaceCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AccountTree,
                title = "SLD Designer",
                subtitle = if (arabic) {
                    "التصميم الأحادي"
                } else {
                    "Single Line Diagram"
                },
                onClick = onOpenSld
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = if (arabic) "المشروع النشط" else "Active Project",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = project?.projectName?.ifBlank {
                        if (arabic) "مشروع جديد" else "New Project"
                    } ?: if (arabic) "لا يوجد مشروع نشط" else "No Active Project",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (project != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Standard: ${project.electricalStandard}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(onClick = onOpenProjectManagement) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (arabic) "إدارة المشروعات" else "Project Management"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectManagementScreen(
    language: AppLanguage,
    project: DesignProject?,
    onBack: () -> Unit,
    onOpenProject: (DesignProject) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        EngineeringAppTopBar(
            title = if (language == AppLanguage.ARABIC) {
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

@Composable
private fun WorkspaceCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ModuleNotConnectedScreen(
    language: AppLanguage,
    module: ElectromechanicalModule?,
    onBack: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = moduleTitle(module, arabic),
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (arabic) {
                "شاشة الحسابات الخاصة بهذه الوحدة غير مربوطة حاليًا. لم يتم تنفيذ حساب بديل أو افتراضي."
            } else {
                "The calculation screen for this module is not connected yet. No substitute or placeholder calculation was performed."
            },
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onBack) {
            Text(if (arabic) "العودة للتصميم" else "Back to Design")
        }
    }
}

private fun moduleTitle(
    module: ElectromechanicalModule?,
    arabic: Boolean
): String {
    return when (module) {
        ElectromechanicalModule.LOAD ->
            if (arabic) "الأحمال" else "Loads"

        ElectromechanicalModule.CURRENT ->
            if (arabic) "التيار" else "Current"

        ElectromechanicalModule.CABLE ->
            if (arabic) "الكابلات" else "Cables"

        ElectromechanicalModule.VOLTAGE_DROP ->
            if (arabic) "هبوط الجهد" else "Voltage Drop"

        ElectromechanicalModule.BREAKER ->
            if (arabic) "القواطع" else "Breakers"

        ElectromechanicalModule.SHORT_CIRCUIT ->
            if (arabic) "القصر الكهربائي" else "Short Circuit"

        ElectromechanicalModule.PROTECTION ->
            if (arabic) "الحماية" else "Protection"

        ElectromechanicalModule.TRANSFORMER ->
            if (arabic) "المحولات" else "Transformers"

        ElectromechanicalModule.GENERATOR ->
            if (arabic) "المولدات" else "Generators"

        ElectromechanicalModule.PANEL ->
            if (arabic) "اللوحات" else "Panels"

        ElectromechanicalModule.PUMP ->
            if (arabic) "المضخات" else "Pumps"

        null -> if (arabic) "وحدة التصميم" else "Design Module"
    }
}

@Composable
private fun ReportsHomeTab(
    language: AppLanguage
) {
    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (arabic) "التقارير الهندسية" else "Engineering Reports",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (arabic) {
                "واجهة التقارير جاهزة للربط بمصدر بيانات التقارير."
            } else {
                "The reports workspace is ready to be connected to report data."
            },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun TabletApplicationShell(
    mainTab: MainTab,
    language: AppLanguage,
    activeProject: DesignProject?,
    onSelectTab: (MainTab) -> Unit,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
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
                title = mainTabTitle(mainTab, language),
                projectName = activeProject?.projectName,
                onBack = null
            )

            content()
        }
    }
}

@Composable
private fun PhoneApplicationShell(
    mainTab: MainTab,
    language: AppLanguage,
    activeProject: DesignProject?,
    onSelectTab: (MainTab) -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        EngineeringAppTopBar(
            title = mainTabTitle(mainTab, language),
            projectName = activeProject?.projectName,
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

@Composable
private fun ProfessionalNavigationRail(
    selected: MainTab,
    language: AppLanguage,
    onSelect: (MainTab) -> Unit
) {
    NavigationRail(modifier = Modifier.fillMaxHeight()) {
        NavigationRailItem(
            selected = selected == MainTab.PROJECT,
            onClick = { onSelect(MainTab.PROJECT) },
            icon = {
                Icon(Icons.Outlined.Folder, contentDescription = null)
            },
            label = {
                Text(if (language == AppLanguage.ARABIC) "المشروع" else "Project")
            }
        )

        NavigationRailItem(
            selected = selected == MainTab.DESIGN,
            onClick = { onSelect(MainTab.DESIGN) },
            icon = {
                Icon(Icons.Outlined.ElectricalServices, contentDescription = null)
            },
            label = {
                Text(if (language == AppLanguage.ARABIC) "التصميم" else "Design")
            }
        )

        NavigationRailItem(
            selected = selected == MainTab.SLD,
            onClick = { onSelect(MainTab.SLD) },
            icon = {
                Icon(Icons.Outlined.AccountTree, contentDescription = null)
            },
            label = { Text("SLD") }
        )

        NavigationRailItem(
            selected = selected == MainTab.REPORTS,
            onClick = { onSelect(MainTab.REPORTS) },
            icon = {
                Icon(Icons.Outlined.Description, contentDescription = null)
            },
            label = {
                Text(if (language == AppLanguage.ARABIC) "التقارير" else "Reports")
            }
        )
    }
}

@Composable
private fun ProfessionalNavigationBar(
    selected: MainTab,
    language: AppLanguage,
    onSelect: (MainTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == MainTab.PROJECT,
            onClick = { onSelect(MainTab.PROJECT) },
            icon = {
                Icon(Icons.Outlined.Folder, contentDescription = null)
            },
            label = {
                Text(if (language == AppLanguage.ARABIC) "المشروع" else "Project")
            }
        )

        NavigationBarItem(
            selected = selected == MainTab.DESIGN,
            onClick = { onSelect(MainTab.DESIGN) },
            icon = {
                Icon(Icons.Outlined.ElectricalServices, contentDescription = null)
            },
            label = {
                Text(if (language == AppLanguage.ARABIC) "التصميم" else "Design")
            }
        )

        NavigationBarItem(
            selected = selected == MainTab.SLD,
            onClick = { onSelect(MainTab.SLD) },
            icon = {
                Icon(Icons.Outlined.AccountTree, contentDescription = null)
            },
            label = { Text("SLD") }
        )

        NavigationBarItem(
            selected = selected == MainTab.REPORTS,
            onClick = { onSelect(MainTab.REPORTS) },
            icon = {
                Icon(Icons.Outlined.Description, contentDescription = null)
            },
            label = {
                Text(if (language == AppLanguage.ARABIC) "التقارير" else "Reports")
            }
        )
    }
}

private fun mainTabTitle(
    mainTab: MainTab,
    language: AppLanguage
): String {
    val arabic = language == AppLanguage.ARABIC

    return when (mainTab) {
        MainTab.PROJECT ->
            if (arabic) "المشروع" else "Project"

        MainTab.DESIGN ->
            if (arabic) "التصميم الكهروميكانيكي" else "Electromechanical Design"

        MainTab.SLD -> "SLD Designer"

        MainTab.REPORTS ->
            if (arabic) "التقارير" else "Reports"
    }
}
