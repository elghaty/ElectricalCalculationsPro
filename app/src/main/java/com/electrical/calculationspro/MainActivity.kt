package com.electrical.calculationspro

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
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
                    modifier =
                        Modifier.fillMaxSize(),
                    color =
                        MaterialTheme.colorScheme.background
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

            val project =
                activeProject

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

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {

        Text(
            text =
                if (arabic)
                    "التصميم الكهربائي"
                else
                    "Electrical Design",
            style =
                MaterialTheme.typography.headlineMedium
        )

        Text(
            text =
                if (arabic)
                    "جميع الحسابات جزء من التصميم النشط للمشروع"
                else
                    "All calculations belong to the active project design"
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = {}
        ) {

            Text(
                if (arabic)
                    "الأحمال والتيارات"
                else
                    "Loads & Currents"
            )
        }

        Button(
            onClick = onCable
        ) {

            Text(
                if (arabic)
                    "اختيار الكابلات"
                else
                    "Cable Sizing"
            )
        }

        Button(
            onClick = onVoltageDrop
        ) {

            Text(
                if (arabic)
                    "هبوط الجهد"
                else
                    "Voltage Drop"
            )
        }

        Button(
            onClick = onSld
        ) {

            Text(
                if (arabic)
                    "المخطط الأحادي SLD"
                else
                    "Single Line Diagram"
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
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
