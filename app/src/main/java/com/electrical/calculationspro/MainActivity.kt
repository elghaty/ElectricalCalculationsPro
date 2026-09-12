package com.electrical.calculationspro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.DarkSurface
import com.electrical.calculationspro.ui.theme.ElectricalCalculationsProTheme
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

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
    val icon: ImageVector,
    val iconBg: Color = Color.Gray,
    val iconTint: Color = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {

    var selectedStandard by remember {
        mutableStateOf(Standard.IEC)
    }

    var selectedMenu by remember {
        mutableStateOf("conductor_sizing_protection")
    }

    var language by remember {
        mutableStateOf(AppLanguage.ARABIC)
    }

    var showLanguageMenu by remember {
        mutableStateOf(false)
    }

    val menuItems = listOf(

        MenuItem(
            id = "conductor_sizing",
            titleKey = "conductor_sizing",
            icon = Icons.Default.Cable,
            iconBg = Color(0xFF4CAF50)
        ),

        MenuItem(
            id = "conductor_sizing_protection",
            titleKey = "conductor_sizing_protection",
            icon = Icons.Default.Security,
            iconBg = Color(0xFF607D8B)
        ),

        MenuItem(
            id = "voltage_drop",
            titleKey = "voltage_drop",
            icon = Icons.Default.Bolt,
            iconBg = Color(0xFFE53935)
        ),

        MenuItem(
            id = "current",
            titleKey = "current",
            icon = Icons.Default.ElectricBolt,
            iconBg = Color(0xFF4CAF50)
        ),

        MenuItem(
            id = "voltage",
            titleKey = "voltage",
            icon = Icons.Default.ElectricalServices,
            iconBg = Color(0xFFE91E63)
        ),

        MenuItem(
            id = "active_power",
            titleKey = "active_power",
            icon = Icons.Default.Power,
            iconBg = Color(0xFF9C27B0)
        ),

        MenuItem(
            id = "apparent_power",
            titleKey = "apparent_power",
            icon = Icons.Default.FlashOn,
            iconBg = Color(0xFF3F51B5)
        ),

        MenuItem(
            id = "reactive_power",
            titleKey = "reactive_power",
            icon = Icons.Default.Tune,
            iconBg = Color(0xFF00BCD4)
        ),

        MenuItem(
            id = "power_factor",
            titleKey = "power_factor",
            icon = Icons.Default.Speed,
            iconBg = Color(0xFFFF9800)
        ),

        MenuItem(
            id = "resistance",
            titleKey = "resistance",
            icon = Icons.Default.LinearScale,
            iconBg = Color(0xFFFFEB3B),
            iconTint = Color.Black
        ),

        MenuItem(
            id = "impedance",
            titleKey = "impedance",
            icon = Icons.Default.Timeline,
            iconBg = Color(0xFFFFEB3B),
            iconTint = Color.Black
        )
    )

    fun t(key: String): String {
        return Strings.get(key, language)
    }

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

                    IconButton(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = TextPrimary
                        )
                    }
                },

                actions = {

                    Box {

                        IconButton(
                            onClick = {
                                showLanguageMenu = true
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = t("language"),
                                tint = TextPrimary
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
                                    Text(t("english"))
                                },
                                onClick = {

                                    language = AppLanguage.ENGLISH
                                    showLanguageMenu = false
                                },
                                leadingIcon = {

                                    if (
                                        language ==
                                        AppLanguage.ENGLISH
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PrimaryTeal
                                        )
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(t("arabic"))
                                },
                                onClick = {

                                    language = AppLanguage.ARABIC
                                    showLanguageMenu = false
                                },
                                leadingIcon = {

                                    if (
                                        language ==
                                        AppLanguage.ARABIC
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PrimaryTeal
                                        )
                                    }
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextPrimary
                        )
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

                        selected =
                            selectedStandard == standard,

                        onClick = {
                            selectedStandard = standard
                        },

                        text = {

                            Text(

                                text = when (standard) {

                                    Standard.IEC ->
                                        "IEC"

                                    Standard.EGYPTIAN ->
                                        t("egyptian_code")

                                    Standard.CEI ->
                                        "CEI"

                                    Standard.NEC ->
                                        "NEC"

                                    Standard.CEC ->
                                        "CEC"
                                },

                                fontWeight =
                                    if (
                                        selectedStandard ==
                                        standard
                                    ) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },

                                fontSize = 13.sp
                            )
                        },

                        selectedContentColor =
                            PrimaryTeal,

                        unselectedContentColor =
                            TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxSize()
            ) {

                LazyColumn(

                    modifier = Modifier
                        .width(
                            if (
                                language ==
                                AppLanguage.ARABIC
                            ) {
                                240.dp
                            } else {
                                220.dp
                            }
                        )
                        .fillMaxHeight()
                        .background(DarkSurface)

                ) {

                    items(
                        items = menuItems,
                        key = {
                            it.id
                        }
                    ) { item ->

                        SidebarItem(

                            title =
                                t(item.titleKey),

                            icon =
                                item.icon,

                            iconColor =
                                item.iconTint,

                            iconBackground =
                                item.iconBg,

                            isSelected =
                                selectedMenu == item.id,

                            onClick = {
                                selectedMenu = item.id
                            }
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

                        "conductor_sizing_protection",
                        "conductor_sizing" -> {

                            ConductorSizingScreen(
                                language = language,
                                standard = selectedStandard
                            )
                        }

                        else -> {

                            Box(

                                modifier =
                                    Modifier.fillMaxSize(),

                                contentAlignment =
                                    Alignment.Center

                            ) {

                                Text(

                                    text =
                                        "${t("coming_soon")}\n" +
                                            t(selectedMenu),

                                    color =
                                        TextSecondary,

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
