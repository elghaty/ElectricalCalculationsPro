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

    /*
     * The active project is the single source of truth.
     * UI only refreshes its reference here.
     */
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

        /*
         * --------------------------------------------------
         * PROJECT DASHBOARD
         * --------------------------------------------------
         */

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

        /*
         * --------------------------------------------------
         * ACTIVE PROJECT
         * --------------------------------------------------
         */

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

        /*
         * --------------------------------------------------
         * ELECTRICAL DESIGN
         * --------------------------------------------------
         */

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

        /*
         * --------------------------------------------------
         * CABLE
         * --------------------------------------------------
         */

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

        /*
         * --------------------------------------------------
         * VOLTAGE DROP
         * --------------------------------------------------
         */

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

        /*
         * --------------------------------------------------
         * SHORT CIRCUIT
         *
         * The actual project SLD editor is used as the
         * engineering entry point. It uses the project
         * network instead of a duplicated calculation UI.
         * --------------------------------------------------
         */

        "short_circuit" -> {

            SldEditorScreen(
                language = language,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }

        /*
         * --------------------------------------------------
         * PROTECTION
         * --------------------------------------------------
         */

        "protection" -> {

            SldEditorScreen(
                language = language,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }

        /*
         * --------------------------------------------------
         * PANEL
         * --------------------------------------------------
         */

        "panel" -> {

            SldEditorScreen(
                language = language,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }

        /*
         * --------------------------------------------------
         * SLD
         * --------------------------------------------------
         */

        "sld" -> {

            SldEditorScreen(
                language = language,

                onBack = {
                    screen =
                        "active"
                }
            )
        }

        /*
         * --------------------------------------------------
         * WATER
         * --------------------------------------------------
         */

        "water" -> {

            PumpEngineeringScreen(
                language = language,

                onBack = {
                    screen =
                        "active"
                }
            )
        }

        /*
         * --------------------------------------------------
         * SEWAGE
         * --------------------------------------------------
         */

        "sewage" -> {

            PumpEngineeringScreen(
                language = language,

                onBack = {
                    screen =
                        "active"
                }
            )
        }

        /*
         * --------------------------------------------------
         * TEMPORARY ENGINEERING ROUTES
         *
         * No fake calculation is placed in MainActivity.
         * These routes are kept isolated until their
         * project-backed screens are connected to the Core.
         * --------------------------------------------------
         */

        "load",
        "current",
        "breaker",
        "transformer",
        "generator",
        "report" -> {

            EngineeringModulePlaceholder(
                language = language,
                module = screen,

                onBack = {
                    screen =
                        "electrical"
                }
            )
        }

        else -> {

            screen =
                "projects"
        }
    }
}

/*
 * ==========================================================
 * ELECTRICAL DESIGN DASHBOARD
 * ==========================================================
 */

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

                    DesignGridItem(
                        id = "load",

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

                    DesignGridItem(
                        id = "current",

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

                    DesignGridItem(
                        id = "cable",

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

                    DesignGridItem(
                        id = "voltage_drop",

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

                    DesignGridItem(
                        id = "breaker",

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

                    DesignGridItem(
                        id = "short_circuit",

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

                    DesignGridItem(
                        id = "protection",

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

                    DesignGridItem(
                        id = "transformer",

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

                    DesignGridItem(
                        id = "generator",

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

                    DesignGridItem(
                        id = "panel",

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

                    DesignGridItem(
                        id = "sld",

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

                    DesignGridItem(
                        id = "report",

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

/*
 * ==========================================================
 * TEMPORARY ROUTE PLACEHOLDER
 *
 * Contains no engineering formula and no duplicated Core.
 * It will be replaced by project-backed modules.
 * ==========================================================
 */

@Composable
private fun EngineeringModulePlaceholder(
    language: AppLanguage,
    module: String,
    onBack: () -> Unit
) {

    val arabic =
        language ==
            AppLanguage.ARABIC

    val title =
        when (module) {

            "load" ->
                if (arabic)
                    "الأحمال الكهربائية"
                else
                    "Electrical Loads"

            "current" ->
                if (arabic)
                    "حساب التيار"
                else
                    "Design Current"

            "breaker" ->
                if (arabic)
                    "اختيار القواطع"
                else
                    "Breaker Selection"

            "transformer" ->
                if (arabic)
                    "حساب المحول"
                else
                    "Transformer Sizing"

            "generator" ->
                if (arabic)
                    "حساب المولد"
                else
                    "Generator Sizing"

            "report" ->
                if (arabic)
                    "التقرير الهندسي"
                else
                    "Engineering Report"

            else ->
                if (arabic)
                    "وحدة التصميم"
                else
                    "Design Module"
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                title,

            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                if (arabic) {
                    "هذه الوحدة محفوظة كمسار مستقل لحين ربط واجهتها بمحرك المشروع والـ Core."
                } else {
                    "This module remains isolated until its UI is connected to the project engine and Core."
                }
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
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
