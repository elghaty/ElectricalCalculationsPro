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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val coroutineScope = rememberCoroutineScope()

    var language by remember {
        mutableStateOf(AppLanguage.ARABIC)
    }

    var selectedStandard by remember {
        mutableStateOf(Standard.IEC)
    }

    var selectedMenu by remember {
        mutableStateOf("home")
    }

    var showLanguageMenu by remember {
        mutableStateOf(false)
    }

    var showStandardMenu by remember {
        mutableStateOf(false)
    }

    var showAbout by remember {
        mutableStateOf(false)
    }

    var updateChecking by remember {
        mutableStateOf(false)
    }

    var availableRelease by remember {
        mutableStateOf<AppReleaseInfo?>(null)
    }

    val menuItems = listOf(
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

    val isArabic = language == AppLanguage.ARABIC

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                                showLanguageMenu = true
                            }
                        ) {
                            Text(
                                text = if (isArabic) {
                                    "العربية"
                                } else {
                                    "EN"
                                },
                                color = PrimaryTeal
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = {
                                showLanguageMenu = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("العربية")
                                },
                                onClick = {
                                    language = AppLanguage.ARABIC
                                    showLanguageMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("English")
                                },
                                onClick = {
                                    language = AppLanguage.ENGLISH
                                    showLanguageMenu = false
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
                                showStandardMenu = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = if (isArabic) {
                                    "المعيار"
                                } else {
                                    "Standard"
                                },
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            Text(
                                text = selectedStandard.name,
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
                        expanded = showStandardMenu,
                        onDismissRequest = {
                            showStandardMenu = false
                        }
                    ) {

                        Standard.values().forEach { standard ->

                            DropdownMenuItem(
                                text = {
                                    Text(standard.name)
                                },
                                onClick = {
                                    selectedStandard = standard
                                    showStandardMenu = false
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
                        items = menuItems,
                        key = {
                            it.id
                        }
                    ) { item ->

                        val selected =
                            selectedMenu == item.id

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
                                    selectedMenu = item.id
                                }
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    TextButton(
                        onClick = {
                            showAbout = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isArabic) {
                                "عن البرنامج"
                            } else {
                                "About"
                            },
                            color = TextSecondary
                        )
                    }

                    TextButton(
                        onClick = {
                            updateChecking = true

                            coroutineScope.launch {
                                try {
                                    val result =
                                        AppUpdateManager.checkForUpdate(
                                            context
                                        )

                                    availableRelease = result
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        if (isArabic) {
                                            "تعذر التحقق من التحديثات"
                                        } else {
                                            "Unable to check for updates"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    updateChecking = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isArabic) {
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
                    .weight(1f)
                    .fillMaxHeight()
            ) {

                when (selectedMenu) {

                    "home" -> {

                        HomeScreen(
                            language = language,
                            standard = selectedStandard,
                            onStandardChanged = {
                                selectedStandard = it
                            },
                            onOpenConductorSizing = {
                                selectedMenu =
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
                            standard = selectedStandard
                        )
                    }

                    else -> {

                        EngineeringCalculatorScreen(
                            calculation = selectedMenu,
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
            onDismiss = {
                showAbout = false
            }
        )
    }

    if (updateChecking) {

        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    if (isArabic) {
                        "التحقق من التحديث"
                    } else {
                        "Checking for update"
                    }
                )
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            confirmButton = {}
        )
    }

    availableRelease?.let { release ->

        AlertDialog(
            onDismissRequest = {
                availableRelease = null
            },
            title = {
                Text(
                    if (isArabic) {
                        "تحديث جديد متاح"
                    } else {
                        "New update available"
                    }
                )
            },
            text = {
                Text(
                    if (isArabic) {
                        "الإصدار ${release.versionName} متاح."
                    } else {
                        "Version ${release.versionName} is available."
                    }
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        availableRelease = null

                        coroutineScope.launch {
                            try {
                                AppUpdateManager.startUpdate(
                                    context,
                                    release
                                )
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    if (isArabic) {
                                        "تعذر بدء التحديث"
                                    } else {
                                        "Unable to start update"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(
                        if (isArabic) {
                            "تحديث"
                        } else {
                            "Update"
                        }
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        availableRelease = null
                    }
                ) {
                    Text(
                        if (isArabic) {
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
