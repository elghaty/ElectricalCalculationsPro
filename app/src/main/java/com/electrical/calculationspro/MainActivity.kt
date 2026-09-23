package com.electrical.calculationspro

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjects

import com.electrical.calculationspro.ui.project.ActiveProjectScreen
import com.electrical.calculationspro.ui.project.DesignGridItem
import com.electrical.calculationspro.ui.project.FourColumnDesignGrid
import com.electrical.calculationspro.ui.project.ProjectDashboardScreen

import com.electrical.calculationspro.ui.screens.ConductorSizingScreen
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
                    EngineeringDesignApp()
                }
            }
        }
    }
}

@Composable
private fun EngineeringDesignApp() {

    var language by remember {
        mutableStateOf(AppLanguage.ARABIC)
    }

    var screen by remember {
        mutableStateOf("projects")
    }

    var activeProject by remember {
        mutableStateOf<DesignProject?>(
            DesignProjects.getActive()
        )
    }

    var standard by remember {
        mutableStateOf(Standard.IEC)
    }

    when (screen) {

        "projects" -> {
            ProjectDashboardScreen(
                language = language,
                onOpenProject = { project ->
                    activeProject = project
                    standard =
                        project.electricalStandard
                            ?: Standard.IEC
                    screen = "active"
                }
            )
        }

        "active" -> {
            val project = activeProject

            if (project == null) {
                screen = "projects"
            } else {
                ActiveProjectScreen(
                    project = project,
                    language = language,
                    onElectrical = {
                        screen = "electrical"
                    },
                    onWater = {
                        screen = "water"
                    },
                    onSewage = {
                        screen = "sewage"
                    },
                    onSld = {
                        screen = "sld"
                    },
                    onBack = {
                        screen = "projects"
                    }
                )
            }
        }

        "electrical" -> {
            ElectricalDesignScreen(
                language = language,
                onCable = {
                    screen = "cable"
                },
                onVoltageDrop = {
                    screen = "voltage_drop"
                },
                onSld = {
                    screen = "sld"
                },
                onBack = {
                    screen = "active"
                }
            )
        }

        "water" -> {
            PumpEngineeringScreen(
                language = language,
                onBack = {
                    screen = "active"
                }
            )
        }

        "sewage" -> {
            PumpEngineeringScreen(
                language = language,
                onBack = {
                    screen = "active"
                }
            )
        }

        "cable" -> {
            ConductorSizingScreen(
                language = language,
                standard = standard,
                onBack = {
                    screen = "electrical"
                }
            )
        }

        "voltage_drop" -> {
            ProfessionalVoltageDropScreen(
                language = language,
                standard = standard,
                onBack = {
                    screen = "electrical"
                }
            )
        }

        "sld" -> {
            SldEditorScreen(
                language = language,
                onBack = {
                    screen = "active"
                }
            )
        }

        else -> {
            screen = "projects"
        }
    }
}

@Composable
private fun ElectricalDesignScreen(
    language: AppLanguage,
    onCable: () -> Unit,
    onVoltageDrop: () -> Unit,
    onSld: () -> Unit,
    onBack: () -> Unit
) {

    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Text(
            text = if (arabic)
                "التصميم الكهربائي"
            else
                "Electrical Design",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = if (arabic)
                "منظومة التصميم والحسابات الكهربائية للمشروع"
            else
                "Project electrical design and engineering calculations"
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        FourColumnDesignGrid(
            modifier = Modifier.weight(1f),
            items = listOf(

                DesignGridItem(
                    id = "load",
                    title = if (arabic)
                        "الأحمال"
                    else
                        "Loads",
                    subtitle = if (arabic)
                        "Load Schedule"
                    else
                        "Load Schedule",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Power,
                            contentDescription = null
                        )
                    },
                    onClick = {}
                ),

                DesignGridItem(
                    id = "current",
                    title = if (arabic)
                        "التيار"
                    else
                        "Current",
                    subtitle = if (arabic)
                        "Design Current"
                    else
                        "Design Current",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Calculate,
                            contentDescription = null
                        )
                    },
                    onClick = {}
                ),

                DesignGridItem(
                    id = "cable",
                    title = if (arabic)
                        "الكابلات"
                    else
                        "Cables",
                    subtitle = if (arabic)
                        "Conductor Sizing"
                    else
                        "Conductor Sizing",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Cable,
                            contentDescription = null
                        )
                    },
                    onClick = onCable
                ),

                DesignGridItem(
                    id = "voltage_drop",
                    title = if (arabic)
                        "هبوط الجهد"
                    else
                        "Voltage Drop",
                    subtitle = if (arabic)
                        "VD Study"
                    else
                        "VD Study",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null
                        )
                    },
                    onClick = onVoltageDrop
                ),

                DesignGridItem(
                    id = "breaker",
                    title = if (arabic)
                        "القواطع"
                    else
                        "Breakers",
                    subtitle = if (arabic)
                        "Selection"
                    else
                        "Selection",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ElectricalServices,
                            contentDescription = null
                        )
                    },
                    onClick = {}
                ),

                DesignGridItem(
                    id = "short_circuit",
                    title = if (arabic)
                        "القصر"
                    else
                        "Short Circuit",
                    subtitle = if (arabic)
                        "Fault Study"
                    else
                        "Fault Study",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Calculate,
                            contentDescription = null
                        )
                    },
                    onClick = {}
                ),

                DesignGridItem(
                    id = "protection",
                    title = if (arabic)
                        "الحماية"
                    else
                        "Protection",
                    subtitle = if (arabic)
                        "Protection Study"
                    else
                        "Protection Study",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null
                        )
                    },
                    onClick = {}
                ),

                DesignGridItem(
                    id = "transformer",
                    title = if (arabic)
                        "المحول"
                    else
                        "Transformer",
                    subtitle = if (arabic)
                        "Sizing"
                    else
                        "Sizing",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Power,
                            contentDescription = null
                        )
                    },
                    onClick = {}
                ),

                DesignGridItem(
                    id = "generator",
                    title = if (arabic)
                        "المولد"
                    else
                        "Generator",
                    subtitle = if (arabic)
                        "Sizing"
                    else
                        "Sizing",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Power,
                            contentDescription = null
                        )
                    },
                    onClick = {}
                ),

                DesignGridItem(
                    id = "panel",
                    title = if (arabic)
                        "اللوحات"
                    else
                        "Panels",
                    subtitle = if (arabic)
                        "Panel Design"
                    else
                        "Panel Design",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ElectricalServices,
                            contentDescription = null
                        )
                    },
                    onClick = {}
                ),

                DesignGridItem(
                    id = "sld",
                    title = if (arabic)
                        "SLD"
                    else
                        "SLD",
                    subtitle = if (arabic)
                        "Single Line Diagram"
                    else
                        "Single Line Diagram",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.AccountTree,
                            contentDescription = null
                        )
                    },
                    onClick = onSld
                ),

                DesignGridItem(
                    id = "report",
                    title = if (arabic)
                        "التقرير"
                    else
                        "Report",
                    subtitle = if (arabic)
                        "Engineering Report"
                    else
                        "Engineering Report",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null
                        )
                    },
                    onClick = {}
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
                    "رجوع"
                else
                    "Back"
            )
        }
    }
}
