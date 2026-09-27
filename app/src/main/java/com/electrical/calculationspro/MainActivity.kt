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
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Calculate
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.project.DesignProject
import com.electrical.calculationspro.data.project.DesignProjectCoreBridge
import com.electrical.calculationspro.data.project.DesignProjects

import com.electrical.calculationspro.ui.project.ActiveProjectScreen
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

                    EngineeringDesignApp()
                }
            }
        }
    }
}


@Composable
private fun EngineeringDesignApp() {

    var language by remember {
        mutableStateOf(
            AppLanguage.ARABIC
        )
    }

    var screen by remember {
        mutableStateOf(
            "projects"
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

    LaunchedEffect(screen) {

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


    when (screen) {

        // ========================================================
        // PROJECTS
        // ========================================================

        "projects" -> {

            ProjectDashboardScreen(
                language = language,

                onOpenProject = { project ->

                    DesignProjectCoreBridge
                        .selectProject(
                            project.id
                        )

                    activeProject =
                        DesignProjectCoreBridge
                            .getActiveProject()

                    standard =
                        activeProject
                            ?.electricalStandard
                            ?: Standard.IEC

                    screen =
                        "active"
                }
            )
        }


        // ========================================================
        // ACTIVE PROJECT
        // ========================================================

        "active" -> {

            val project =
                activeProject
                    ?: DesignProjectCoreBridge
                        .getActiveProject()

            if (project == null) {

                screen =
                    "projects"

            } else {

                ActiveProjectScreen(
                    project = project,
                    language = language,

                    onElectrical = {
                        screen =
                            "electrical"
                    },

                    onWater = {
                        screen =
                            "water"
                    },

                    onSewage = {
                        screen =
                            "sewage"
                    },

                    onSld = {
                        screen =
                            "sld"
                    },

                    onBack = {
                        screen =
                            "projects"
                    }
                )
            }
        }


        // ========================================================
        // ELECTRICAL DESIGN DASHBOARD
        // ========================================================

        "electrical" -> {

            ElectricalDesignScreen(
                language = language,

                onLoad = {
                    screen =
                        "load"
                },

                onCurrent = {
                    screen =
                        "current"
                },

                onCable = {
                    screen =
                        "cable"
                },

                onVoltageDrop = {
                    screen =
                        "voltage_drop"
                },

                onBreaker = {
                    screen =
                        "breaker"
                },

                onShortCircuit = {
                    screen =
                        "short_circuit"
                },

                onProtection = {
                    screen =
                        "protection"
                },

                onTransformer = {
                    screen =
                        "transformer"
                },

                onGenerator = {
                    screen =
                        "generator"
                },

                onPanel = {
                    screen =
                        "panel"
                },

                onSld = {
                    screen =
                        "sld"
                },

                onReport = {
                    screen =
                        "report"
                },

                onBack = {
                    screen =
                        "active"
                }
            )
        }


        // ========================================================
        // CURRENT
        // ========================================================

        "current" -> {

            CurrentCalculationScreen(
                language = language,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // CABLE
        // ========================================================

        "cable" -> {

            ConductorSizingScreen(
                language = language,
                standard = standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // VOLTAGE DROP
        // ========================================================

        "voltage_drop" -> {

            ProfessionalVoltageDropScreen(
                language = language,
                standard = standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // LOAD
        // ========================================================

        "load" -> {

            EngineeringModuleScreen(
                module =
                    EngineeringModule.LOAD,

                language =
                    language,

                standard =
                    standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // BREAKER
        // ========================================================

        "breaker" -> {

            EngineeringModuleScreen(
                module =
                    EngineeringModule.BREAKER,

                language =
                    language,

                standard =
                    standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // TRANSFORMER
        // ========================================================

        "transformer" -> {

            EngineeringModuleScreen(
                module =
                    EngineeringModule.TRANSFORMER,

                language =
                    language,

                standard =
                    standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // GENERATOR
        // ========================================================

        "generator" -> {

            EngineeringModuleScreen(
                module =
                    EngineeringModule.GENERATOR,

                language =
                    language,

                standard =
                    standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // PANEL
        // ========================================================

        "panel" -> {

            EngineeringModuleScreen(
                module =
                    EngineeringModule.PANEL,

                language =
                    language,

                standard =
                    standard,

                onBack = {
                    screen =
                        "electrical"
                },

                onOpenSld = {
                    screen =
                        "sld"
                }
            )
        }


        // ========================================================
        // SHORT CIRCUIT
        // ========================================================

        "short_circuit" -> {

            EngineeringModuleScreen(
                module =
                    EngineeringModule.SHORT_CIRCUIT,

                language =
                    language,

                standard =
                    standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // PROTECTION
        // ========================================================

        "protection" -> {

            EngineeringModuleScreen(
                module =
                    EngineeringModule.PROTECTION,

                language =
                    language,

                standard =
                    standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // REPORT
        // ========================================================

        "report" -> {

            EngineeringModuleScreen(
                module =
                    EngineeringModule.REPORT,

                language =
                    language,

                standard =
                    standard,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // SLD
        // ========================================================

        "sld" -> {

            SldEditorScreen(
                language =
                    language,

                onBack = {
                    /*
                     * SLD is an independent screen under the
                     * Electrical Design dashboard.
                     *
                     * It must NOT return to the Active Project
                     * screen.
                     */
                    screen =
                        "electrical"
                }
            )
        }


        // ========================================================
        // WATER
        // ========================================================

        "water" -> {

            PumpEngineeringScreen(
                language =
                    language,

                onBack = {
                    screen =
                        "active"
                }
            )
        }


        // ========================================================
        // SEWAGE
        // ========================================================

        "sewage" -> {

            PumpEngineeringScreen(
                language =
                    language,

                onBack = {
                    screen =
                        "active"
                }
            )
        }


        // ========================================================
        // FALLBACK
        // ========================================================

        else -> {

            screen =
                "projects"
        }
    }
}


// ==================================================================
// ELECTRICAL DESIGN DASHBOARD
// ==================================================================

@Composable
private fun ElectricalDesignScreen(
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
    onSld: () -> Unit,
    onReport: () -> Unit,

    onBack: () -> Unit
) {

    val arabic =
        language ==
            AppLanguage.ARABIC


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(18.dp)
    ) {

        Text(
            text =
                if (arabic) {
                    "التصميم الكهربائي"
                } else {
                    "Electrical Design"
                },

            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )


        Spacer(
            modifier =
                Modifier.height(6.dp)
        )


        Text(
            text =
                if (arabic) {
                    "منظومة التصميم والحسابات الكهربائية للمشروع"
                } else {
                    "Integrated project electrical design"
                }
        )


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        FourColumnDesignGrid(

            modifier =
                Modifier.weight(1f),

            items =
                listOf(

                    // ------------------------------------------------
                    // LOAD
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "load",

                        title =
                            if (arabic)
                                "الأحمال"
                            else
                                "Loads",

                        subtitle =
                            "Load Schedule",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Power,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onLoad
                    ),


                    // ------------------------------------------------
                    // CURRENT
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "current",

                        title =
                            if (arabic)
                                "التيار"
                            else
                                "Current",

                        subtitle =
                            "Design Current",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Calculate,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onCurrent
                    ),


                    // ------------------------------------------------
                    // CABLE
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "cable",

                        title =
                            if (arabic)
                                "الكابلات"
                            else
                                "Cables",

                        subtitle =
                            "Conductor Sizing",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Cable,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onCable
                    ),


                    // ------------------------------------------------
                    // VOLTAGE DROP
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "voltage_drop",

                        title =
                            if (arabic)
                                "هبوط الجهد"
                            else
                                "Voltage Drop",

                        subtitle =
                            "Voltage Drop Study",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Bolt,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onVoltageDrop
                    ),


                    // ------------------------------------------------
                    // BREAKER
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "breaker",

                        title =
                            if (arabic)
                                "القواطع"
                            else
                                "Breakers",

                        subtitle =
                            "Breaker Selection",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.ElectricalServices,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onBreaker
                    ),


                    // ------------------------------------------------
                    // SHORT CIRCUIT
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "short_circuit",

                        title =
                            if (arabic)
                                "تيارات القصر"
                            else
                                "Short Circuit",

                        subtitle =
                            "Fault Study",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Calculate,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onShortCircuit
                    ),


                    // ------------------------------------------------
                    // PROTECTION
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "protection",

                        title =
                            if (arabic)
                                "الحماية"
                            else
                                "Protection",

                        subtitle =
                            "Protection & Coordination",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Settings,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onProtection
                    ),


                    // ------------------------------------------------
                    // TRANSFORMER
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "transformer",

                        title =
                            if (arabic)
                                "المحولات"
                            else
                                "Transformers",

                        subtitle =
                            "Transformer Sizing",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Power,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onTransformer
                    ),


                    // ------------------------------------------------
                    // GENERATOR
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "generator",

                        title =
                            if (arabic)
                                "المولدات"
                            else
                                "Generators",

                        subtitle =
                            "Generator Sizing",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Power,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onGenerator
                    ),


                    // ------------------------------------------------
                    // PANEL
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "panel",

                        title =
                            if (arabic)
                                "اللوحات"
                            else
                                "Panels",

                        subtitle =
                            "Panel Design",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.ElectricalServices,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onPanel
                    ),


                    // ------------------------------------------------
                    // SLD
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "sld",

                        title =
                            "SLD",

                        subtitle =
                            if (arabic)
                                "المخطط الأحادي"
                            else
                                "Single Line Diagram",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.AccountTree,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onSld
                    ),


                    // ------------------------------------------------
                    // REPORT
                    // ------------------------------------------------

                    DesignGridItem(
                        id =
                            "report",

                        title =
                            if (arabic)
                                "التقرير"
                            else
                                "Report",

                        subtitle =
                            "Engineering Report",

                        icon = {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Description,

                                contentDescription =
                                    null
                            )
                        },

                        onClick =
                            onReport
                    )
                )
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        OutlinedButton(
            onClick =
                onBack
        ) {

            Text(
                text =
                    if (arabic)
                        "رجوع"
                    else
                        "Back"
            )
        }
    }
}
