package com.electrical.calculationspro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.Strings
import com.electrical.calculationspro.ui.components.SidebarItem
import com.electrical.calculationspro.ui.screens.ConductorSizingScreen
import com.electrical.calculationspro.ui.theme.*

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

data class MenuItem(
    val id: String,
    val titleKey: String,
    val icon: ImageVector? = null,
    val iconBg: Color = Color.Gray,
    val iconTint: Color = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedStandard by remember { mutableStateOf(Standard.IEC) }
    var selectedMenu by remember { mutableStateOf("conductor_sizing_protection") }
    var language by remember { mutableStateOf(AppLanguage.ARABIC) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val menuItems = listOf(
        MenuItem("conductor_sizing", "conductor_sizing", Icons.Default.Cable, Color(0xFF4CAF50)),
        MenuItem("conductor_sizing_protection", "conductor_sizing_protection", Icons.Default.Security, Color(0xFF607D8B)),
        MenuItem("voltage_drop", "voltage_drop", Icons.Default.Bolt, Color(0xFFE53935)),
        MenuItem("current", "current", Icons.Default.ElectricBolt, Color(0xFF4CAF50)),
        MenuItem("voltage", "voltage", Icons.Default.ElectricalServices, Color(0xFFE91E63)),
        MenuItem("active_power", "active_power", Icons.Default.Power, Color(0xFF9C27B0)),
        MenuItem("apparent_power", "apparent_power", Icons.Default.FlashOn, Color(0xFF3F51B5)),
        MenuItem("reactive_power", "reactive_power", Icons.Default.Tune, Color(0xFF00BCD4)),
        MenuItem("power_factor", "power_factor", Icons.Default.Speed, Color(0xFFFF9800)),
        MenuItem("resistance", "resistance", Icons.Default.LinearScale, Color(0xFFFFEB3B), Color.Black),
        MenuItem("impedance", "impedance", Icons.Default.Timeline, Color(0xFFFFEB3B), Color.Black),
    )

    fun t(key: String) = Strings.get(key, language)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = t(selectedMenu),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E2A3A),
                    titleContentColor = TextPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = "Language", tint = TextPrimary)
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(t("english")) },
                                onClick = {
                                    language = AppLanguage.ENGLISH
                                    showLanguageMenu = false
                                },
                                leadingIcon = {
                                    if (language == AppLanguage.ENGLISH) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryTeal)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(t("arabic")) },
                                onClick = {
                                    language = AppLanguage.ARABIC
                                    showLanguageMenu = false
                                },
                                leadingIcon = {
                                    if (language == AppLanguage.ARABIC) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryTeal)
                                    }
                                }
                            )
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Functions, contentDescription = null, tint = TextPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextPrimary)
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedStandard.ordinal,
                containerColor = Color(0xFF1E2A3A),
                contentColor = PrimaryTeal,
                edgePadding = 8.dp,
                divider = {}
            ) {
                Standard.values().forEach { standard ->
                    Tab(
                        selected = selectedStandard == standard,
                        onClick = { selectedStandard = standard },
                        text = {
                            Text(
                                text = when (standard) {
                                    Standard.IEC -> "IEC"
                                    Standard.EGYPTIAN -> t("egyptian_code")
                                    Standard.CEI -> "CEI"
                                    Standard.NEC -> "NEC"
                                    Standard.CEC -> "CEC"
                                },
                                fontWeight = if (selectedStandard == standard) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        selectedContentColor = PrimaryTeal,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .width(if (language == AppLanguage.ARABIC) 240.dp else 220.dp)
                        .fillMaxHeight()
                        .background(DarkSurface)
                ) {
                    items(menuItems) { item ->
                        SidebarItem(
                            title = t(item.titleKey),
                            icon = item.icon,
                            iconColor = item.iconTint,
                            iconBackground = item.iconBg,
                            isSelected = selectedMenu == item.id,
                            onClick = { selectedMenu = item.id }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DarkBackground)
                ) {
                    when (selectedMenu) {
                        "conductor_sizing_protection", "conductor_sizing" -> {
                            ConductorSizingScreen(
                                language = language,
                                standard = selectedStandard
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "\( {t("coming_soon")}\n \){t(selectedMenu)}",
                                    color = TextSecondary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
