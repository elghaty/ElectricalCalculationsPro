package com.electrical.calculationspro

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.Strings
import com.electrical.calculationspro.ui.screens.AboutScreen
import com.electrical.calculationspro.ui.screens.ConductorSizingScreen
import com.electrical.calculationspro.ui.screens.EngineeringCalculatorScreen
import com.electrical.calculationspro.ui.screens.SldEditorScreen
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.ElectricalCalculationsProTheme
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary
import com.electrical.calculationspro.update.AppReleaseInfo
import com.electrical.calculationspro.update.AppUpdateManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ElectricalCalculationsProTheme {
                MainScreen()
            }
        }
    }
}

private data class CalculationMenuItem(
    val id: String,
    val titleKey: String,
    val icon: String
)

@Composable
private fun MainScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val updateManager = remember {
        AppUpdateManager(context)
    }

    var language by remember {
        mutableStateOf(AppLanguage.ARABIC)
    }

    var standard by remember {
        mutableStateOf(Standard.IEC)
    }

    var selectedScreen by remember {
        mutableStateOf("home")
    }

    var languageMenuExpanded by remember {
        mutableStateOf(false)
    }

    var standardMenuExpanded by remember {
        mutableStateOf(false)
    }

    var showAbout by remember {
        mutableStateOf(false)
    }

    var checkingUpdate by remember {
        mutableStateOf(false)
    }

    var release by remember {
        mutableStateOf<AppReleaseInfo?>(null)
    }

    val arabic = language == AppLanguage.ARABIC

    val menu = listOf(
        CalculationMenuItem(
            id = "home",
            titleKey = "home",
            icon = "⌂"
        ),
        CalculationMenuItem(
            id = "sld_editor",
            titleKey = "sld",
            icon = "⌁"
        ),
        CalculationMenuItem(
            id = "conductor_sizing_protection",
            titleKey = "conductor_sizing",
            icon = "⚡"
        ),
        CalculationMenuItem(
            id = "voltage_drop",
            titleKey = "voltage_drop",
            icon = "↕"
        ),
        CalculationMenuItem(
            id = "short_circuit",
            titleKey = "short_circuit",
            icon = "⚠"
        ),
        CalculationMenuItem(
            id = "transformer",
            titleKey = "transformer",
            icon = "T"
        ),
        CalculationMenuItem(
            id = "motor",
            titleKey = "motor",
            icon = "M"
        ),
        CalculationMenuItem(
            id = "power_factor",
            titleKey = "power_factor",
            icon = "PF"
        )
    )

    Scaffold(
        containerColor = DarkBackground
    ) { padding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
        ) {

            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Electrical",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Calculations Pro",
                            color = PrimaryTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box {

                        TextButton(
                            onClick = {
                                languageMenuExpanded = true
                            }
                        ) {
                            Text(
                                text = if (arabic) {
                                    "العربية"
                                } else {
                                    "EN"
                                },
                                color = PrimaryTeal
                            )
                        }

                        DropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = {
                                languageMenuExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("العربية")
                                },
                                onClick = {
                                    language = AppLanguage.ARABIC
                                    languageMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("English")
                                },
                                onClick = {
                                    language = AppLanguage.ENGLISH
                                    languageMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(12.dp)
                            )
                            .background(
                                Color(0xFF151D24)
                            )
                            .clickable {
                                standardMenuExpanded = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = if (arabic) {
                                    "المعيار"
                                } else {
                                    "Standard"
                                },
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            Text(
                                text = standard.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "▼",
                            color = PrimaryTeal
                        )
                    }

                    DropdownMenu(
                        expanded = standardMenuExpanded,
                        onDismissRequest = {
                            standardMenuExpanded = false
                        }
                    ) {

                        Standard.values().forEach { item ->

                            DropdownMenuItem(
                                text = {
                                    Text(item.name)
                                },
                                onClick = {
                                    standard = item
                                    standardMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    items(
                        items = menu,
                        key = {
                            it.id
                        }
                    ) { item ->

                        val selected =
                            selectedScreen == item.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(10.dp)
                                )
                                .background(
                                    if (selected) {
                                        PrimaryTeal.copy(
                                            alpha = 0.16f
                                        )
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable {
                                    selectedScreen = item.id
                                }
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = item.icon,
                                modifier = Modifier.width(32.dp),
                                color = if (selected) {
                                    PrimaryTeal
                                } else {
                                    TextSecondary
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = Strings.get(
                                    item.titleKey,
                                    language
                                ),
                                color = if (selected) {
                                    PrimaryTeal
                                } else {
                                    TextPrimary
                                },
                                fontSize = 14.sp,
                                fontWeight = if (selected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showAbout = true
                        }
                    ) {
                        Text(
                            text = if (arabic) {
                                "عن البرنامج"
                            } else {
                                "About"
                            },
                            color = TextSecondary
                        )
                    }

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {

                            checkingUpdate = true

                            scope.launch {

                                try {

                                    release =
                                        updateManager
                                            .checkForUpdate()

                                    if (release == null) {
                                        Toast.makeText(
                                            context,
                                            if (arabic) {
                                                "البرنامج محدث بالفعل"
                                            } else {
                                                "The app is already up to date"
                                            },
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                } catch (_: Exception) {

                                    Toast.makeText(
                                        context,
                                        if (arabic) {
                                            "تعذر التحقق من التحديثات"
                                        } else {
                                            "Unable to check for updates"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } finally {

                                    checkingUpdate = false
                                }
                            }
                        }
                    ) {
                        Text(
                            text = if (arabic) {
                                "التحقق من التحديث"
                            } else {
                                "Check for update"
                            },
                            color = PrimaryTeal
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {

                when (selectedScreen) {

                    "home" -> {
                        HomeContent(
                            language = language,
                            onOpenSld = {
                                selectedScreen = "sld_editor"
                            },
                            onOpenConductorSizing = {
                                selectedScreen =
                                    "conductor_sizing_protection"
                            }
                        )
                    }

                    "sld_editor" -> {
                        SldEditorScreen(
                            language = language
                        )
                    }

                    "conductor_sizing_protection" -> {
                        ConductorSizingScreen(
                            language = language,
                            standard = standard
                        )
                    }

                    else -> {
                        EngineeringCalculatorScreen(
                            calculation = selectedScreen,
                            language = language
                        )
                    }
                }
            }
        }
    }

    if (showAbout) {

        AboutScreen(
            language = language,
            onClose = {
                showAbout = false
            }
        )
    }

    if (checkingUpdate) {

        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = if (arabic) {
                        "التحقق من التحديث"
                    } else {
                        "Checking for update"
                    }
                )
            },
            text = {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryTeal
                    )

                    Spacer(
                        modifier = Modifier.width(16.dp)
                    )

                    Text(
                        text = if (arabic) {
                            "جارٍ التحقق..."
                        } else {
                            "Checking..."
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    release?.let { currentRelease ->

        AlertDialog(
            onDismissRequest = {
                release = null
            },
            title = {
                Text(
                    text = if (arabic) {
                        "تحديث جديد متاح"
                    } else {
                        "New update available"
                    }
                )
            },
            text = {
                Column {

                    Text(
                        text = currentRelease.versionName,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = currentRelease.releaseNotes
                            .ifBlank {
                                if (arabic) {
                                    "يتوفر إصدار جديد من البرنامج."
                                } else {
                                    "A new version is available."
                                }
                            }
                    )
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        try {
                            updateManager
                                .downloadAndInstall(
                                    currentRelease
                                )
                        } catch (_: Exception) {

                            Toast.makeText(
                                context,
                                if (arabic) {
                                    "تعذر بدء التحديث"
                                } else {
                                    "Unable to start update"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        release = null
                    }
                ) {
                    Text(
                        text = if (arabic) {
                            "تحديث"
                        } else {
                            "Update"
                        },
                        color = PrimaryTeal
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        release = null
                    }
                ) {
                    Text(
                        text = if (arabic) {
                            "إلغاء"
                        } else {
                            "Cancel"
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun HomeContent(
    language: AppLanguage,
    onOpenSld: () -> Unit,
    onOpenConductorSizing: () -> Unit
) {

    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = if (arabic) {
                "Electrical Calculations Pro"
            } else {
                "Electrical Calculations Pro"
            },
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (arabic) {
                "منصة الحسابات والتصميمات الكهربائية الاحترافية"
            } else {
                "Professional electrical calculation and design platform"
            },
            color = TextSecondary,
            fontSize = 15.sp
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenSld()
                },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF151D24)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = if (arabic) {
                        "SLD — Single Line Diagram"
                    } else {
                        "SLD — Single Line Diagram"
                    },
                    color = PrimaryTeal,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = if (arabic) {
                        "ارسم المخطط الأحادي بالأحمال والمعدات ثم نفذ حسابات Upstream وقصر الدائرة والكابلات والحماية وجدول اللوحات."
                    } else {
                        "Draw the single-line diagram with loads and equipment, then perform upstream, short-circuit, cable sizing, protection and panel-schedule calculations."
                    },
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = if (arabic) {
                        "فتح محرر SLD →"
                    } else {
                        "Open SLD Editor →"
                    },
                    color = PrimaryTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenConductorSizing()
                },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF151D24)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = if (arabic) {
                        "اختيار الكابلات والحماية"
                    } else {
                        "Conductor Sizing & Protection"
                    },
                    color = PrimaryTeal,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = if (arabic) {
                        "حساب التيار واختيار مقطع الموصل والحماية طبقًا للمعيار المختار."
                    } else {
                        "Calculate current and select conductor size and protection according to the selected standard."
                    },
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = if (arabic) {
                        "فتح الحسابات →"
                    } else {
                        "Open calculations →"
                    },
                    color = PrimaryTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
