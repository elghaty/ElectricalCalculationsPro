package com.electrical.calculationspro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.ui.screens.AboutScreen
import com.electrical.calculationspro.ui.screens.ConductorSizingScreen
import com.electrical.calculationspro.ui.screens.EngineeringCalculatorScreen
import com.electrical.calculationspro.ui.screens.SldEditorScreen
import com.electrical.calculationspro.ui.theme.ElectricalCalculationsProTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ElectricalCalculationsProTheme {
                ElectricalCalculationsApp()
            }
        }
    }
}

@Composable
private fun ElectricalCalculationsApp() {

    var language by remember {
        mutableStateOf(AppLanguage.ENGLISH)
    }

    var standard by remember {
        mutableStateOf(Standard.IEC)
    }

    var selectedScreen by remember {
        mutableStateOf("home")
    }

    var showAbout by remember {
        mutableStateOf(false)
    }

    if (showAbout) {

        AboutScreen(
            language = language,
            onClose = {
                showAbout = false
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF0B1116)
            )
    ) {

        TopBar(
            language = language,
            standard = standard,
            onLanguageChanged = {
                language = it
            },
            onStandardChanged = {
                standard = it
            },
            onAbout = {
                showAbout = true
            }
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {

            SideMenu(
                language = language,
                selectedScreen = selectedScreen,
                onSelect = {
                    selectedScreen = it
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {

                when (selectedScreen) {

                    "sld" -> {

                        SldEditorScreen(
                            language = language,
                            onBack = {
                                selectedScreen = "home"
                            }
                        )
                    }

                    "conductor_sizing" -> {

                        ConductorSizingScreen(
                            language = language,
                            standard = standard
                        )
                    }

                    "voltage_drop",
                    "current",
                    "voltage",
                    "active_power",
                    "apparent_power",
                    "reactive_power",
                    "power_factor",
                    "resistance",
                    "impedance" -> {

                        EngineeringCalculatorScreen(
                            calculation = selectedScreen,
                            language = language
                        )
                    }

                    else -> {

                        HomeScreen(
                            language = language,
                            onSld = {
                                selectedScreen = "sld"
                            },
                            onConductor = {
                                selectedScreen =
                                    "conductor_sizing"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    language: AppLanguage,
    standard: Standard,
    onLanguageChanged: (AppLanguage) -> Unit,
    onStandardChanged: (Standard) -> Unit,
    onAbout: () -> Unit
) {

    var languageExpanded by remember {
        mutableStateOf(false)
    }

    var standardExpanded by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(
                Color(0xFF151D24)
            )
            .padding(
                horizontal = 12.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                if (language == AppLanguage.ARABIC) {
                    "الحسابات الكهربائية الاحترافية"
                } else {
                    "Electrical Calculations Pro"
                },
            color = Color.White,
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Box {

            OutlinedButton(
                onClick = {
                    languageExpanded = true
                }
            ) {

                Text(
                    if (
                        language ==
                        AppLanguage.ARABIC
                    ) {
                        "العربية"
                    } else {
                        "English"
                    }
                )
            }

            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = {
                    languageExpanded = false
                }
            ) {

                DropdownMenuItem(
                    text = {
                        Text("English")
                    },
                    onClick = {

                        languageExpanded = false

                        onLanguageChanged(
                            AppLanguage.ENGLISH
                        )
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("العربية")
                    },
                    onClick = {

                        languageExpanded = false

                        onLanguageChanged(
                            AppLanguage.ARABIC
                        )
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Box {

            OutlinedButton(
                onClick = {
                    standardExpanded = true
                }
            ) {

                Text(
                    standard.name
                )
            }

            DropdownMenu(
                expanded = standardExpanded,
                onDismissRequest = {
                    standardExpanded = false
                }
            ) {

                Standard.values()
                    .forEach { item ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    item.name
                                )
                            },
                            onClick = {

                                standardExpanded =
                                    false

                                onStandardChanged(
                                    item
                                )
                            }
                        )
                    }
            }
        }

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        TextButton(
            onClick = onAbout
        ) {

            Text(
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "حول"
                } else {
                    "About"
                }
            )
        }
    }
}

@Composable
private fun SideMenu(
    language: AppLanguage,
    selectedScreen: String,
    onSelect: (String) -> Unit
) {

    val items =
        listOf(

            "home" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "الرئيسية"
                } else {
                    "Home"
                },

            "sld" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "المخطط الأحادي SLD"
                } else {
                    "Professional SLD"
                },

            "conductor_sizing" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "اختيار مقطع الموصل"
                } else {
                    "Conductor Sizing"
                },

            "voltage_drop" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "هبوط الجهد"
                } else {
                    "Voltage Drop"
                },

            "current" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "التيار"
                } else {
                    "Current"
                },

            "voltage" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "الجهد"
                } else {
                    "Voltage"
                },

            "active_power" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "القدرة الفعالة kW"
                } else {
                    "Active Power kW"
                },

            "apparent_power" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "القدرة الظاهرية kVA"
                } else {
                    "Apparent Power kVA"
                },

            "reactive_power" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "القدرة غير الفعالة kvar"
                } else {
                    "Reactive Power kvar"
                },

            "power_factor" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "معامل القدرة"
                } else {
                    "Power Factor"
                },

            "resistance" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "المقاومة"
                } else {
                    "Resistance"
                },

            "impedance" to
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "الممانعة"
                } else {
                    "Impedance"
                }
        )

    Column(
        modifier = Modifier
            /*
             * مهم جدًا:
             * لا تستخدم fillMaxSize هنا.
             * القائمة يجب أن تأخذ عرضها فقط
             * وتترك باقي الشاشة لـ SLD.
             */
            .width(230.dp)
            .fillMaxHeight()
            .background(
                Color(0xFF111920)
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(10.dp),

        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        items.forEach { item ->

            val selected =
                selectedScreen ==
                    item.first

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        onSelect(
                            item.first
                        )
                    },

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (selected) {
                                Color(0xFF263A43)
                            } else {
                                Color(0xFF182129)
                            }
                    )
            ) {

                Text(
                    text = item.second,

                    modifier =
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 11.dp
                        ),

                    color = Color.White,

                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    language: AppLanguage,
    onSld: () -> Unit,
    onConductor: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text =
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "الحسابات الكهربائية الاحترافية"
                } else {
                    "Electrical Calculations Pro"
                },

            color = Color.White,

            fontSize = 28.sp
        )

        Text(
            text =
                if (
                    language ==
                    AppLanguage.ARABIC
                ) {
                    "برنامج هندسي متكامل للحسابات والتصميم الكهربائي."
                } else {
                    "Professional electrical calculation and design platform."
                },

            color =
                Color(0xFFAAB7C0),

            fontSize = 15.sp
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSld()
                },

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF151D24)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text =
                        if (
                            language ==
                            AppLanguage.ARABIC
                        ) {
                            "المخطط الأحادي الاحترافي SLD"
                        } else {
                            "Professional Single Line Diagram"
                        },

                    color = Color.White,

                    fontSize = 20.sp
                )

                Text(
                    text =
                        if (
                            language ==
                            AppLanguage.ARABIC
                        ) {
                            "إنشاء شبكة SLD وإضافة المصادر والمحولات والمولدات واللوحات والأحمال مع الحسابات الهندسية."
                        } else {
                            "Create an SLD network with sources, transformers, generators, panels and loads with engineering calculations."
                        },

                    color =
                        Color(0xFFAAB7C0),

                    fontSize = 14.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onConductor()
                },

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF151D24)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text =
                        if (
                            language ==
                            AppLanguage.ARABIC
                        ) {
                            "اختيار مقطع الموصل"
                        } else {
                            "Conductor Sizing"
                        },

                    color = Color.White,

                    fontSize = 20.sp
                )

                Text(
                    text =
                        if (
                            language ==
                            AppLanguage.ARABIC
                        ) {
                            "حساب تيار التصميم واختيار المقطع والتحقق من هبوط الجهد."
                        } else {
                            "Calculate design current, select conductor section and verify voltage drop."
                        },

                    color =
                        Color(0xFFAAB7C0),

                    fontSize = 14.sp
                )
            }
        }
    }
}
