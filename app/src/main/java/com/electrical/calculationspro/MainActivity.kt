app/src/main/java/com/electrical/calculationspro/MainActivity.kt

package com.electrical.calculationspro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard
import com.electrical.calculationspro.data.Strings
import com.electrical.calculationspro.ui.screens.ConductorSizingScreen
import com.electrical.calculationspro.ui.screens.EngineeringCalculatorScreen
import com.electrical.calculationspro.ui.theme.DarkBackground
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
    val icon: ImageVector
)

@Composable
fun MainScreen() {

    val context = LocalContext.current

    var language by remember {
        mutableStateOf(AppLanguage.ARABIC)
    }

    var selectedStandard by remember {
        mutableStateOf(Standard.IEC)
    }

    var selectedMenu by remember {
        mutableStateOf("home")
    }

    var languageExpanded by remember {
        mutableStateOf(false)
    }

    var showAbout by remember {
        mutableStateOf(false)
    }

    var showFunctions by remember {
        mutableStateOf(false)
    }

    val menuItems = remember {

        listOf(

            MenuItem(
                id = "conductor_sizing_protection",
                titleKey = "conductor_sizing_protection",
                icon = Icons.Default.Security
            ),

            MenuItem(
                id = "voltage_drop",
                titleKey = "voltage_drop",
                icon = Icons.Default.Bolt
            ),

            MenuItem(
                id = "current",
                titleKey = "current",
                icon = Icons.Default.ElectricBolt
            ),

            MenuItem(
                id = "voltage",
                titleKey = "voltage",
                icon = Icons.Default.Bolt
            ),

            MenuItem(
                id = "active_power",
                titleKey = "active_power",
                icon = Icons.Default.Power
            ),

            MenuItem(
                id = "apparent_power",
                titleKey = "apparent_power",
                icon = Icons.Default.ElectricBolt
            ),

            MenuItem(
                id = "reactive_power",
                titleKey = "reactive_power",
                icon = Icons.Default.Power
            ),

            MenuItem(
                id = "power_factor",
                titleKey = "power_factor",
                icon = Icons.Default.Calculate
            ),

            MenuItem(
                id = "resistance",
                titleKey = "resistance",
                icon = Icons.Default.Settings
            ),

            MenuItem(
                id = "impedance",
                titleKey = "impedance",
                icon = Icons.Default.Settings
            )
        )
    }

    fun t(key: String): String {
        return Strings.get(key, language)
    }

    fun openUpdatePage() {

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://github.com/elghaty/ElectricalCalculationsPro/releases/latest"
            )
        )

        context.startActivity(intent)
    }

    if (showAbout) {

        AlertDialog(

            onDismissRequest = {
                showAbout = false
            },

            title = {
                Text(
                    text = t("app_name")
                )
            },

            text = {
                Text(
                    text = t("about_description")
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showAbout = false
                    }
                ) {
                    Text(t("close"))
                }
            }
        )
    }

    if (showFunctions) {

        AlertDialog(

            onDismissRequest = {
                showFunctions = false
            },

            title = {
                Text(
                    text = t("functions")
                )
            },

            text = {
                Text(
                    text = t("functions_description")
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showFunctions = false
                    }
                ) {
                    Text(t("close"))
                }
            }
        )
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text =
                            if (selectedMenu == "home") {
                                t("app_name")
                            } else {
                                t(selectedMenu)
                            },

                        color = TextPrimary,

                        fontSize = 16.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = {
                            selectedMenu = "home"
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Menu,

                            contentDescription =
                                t("app_name"),

                            tint =
                                TextPrimary
                        )
                    }
                },

                actions = {

                    Box {

                        IconButton(
                            onClick = {
                                languageExpanded = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Language,

                                contentDescription =
                                    t("language"),

                                tint =
                                    TextPrimary
                            )
                        }

                        DropdownMenu(

                            expanded =
                                languageExpanded,

                            onDismissRequest = {
                                languageExpanded = false
                            }

                        ) {

                            DropdownMenuItem(

                                text = {
                                    Text(
                                        t("arabic")
                                    )
                                },

                                onClick = {

                                    language =
                                        AppLanguage.ARABIC

                                    languageExpanded =
                                        false
                                }
                            )

                            DropdownMenuItem(

                                text = {
                                    Text(
                                        t("english")
                                    )
                                },

                                onClick = {

                                    language =
                                        AppLanguage.ENGLISH

                                    languageExpanded =
                                        false
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            openUpdatePage()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Refresh,

                            contentDescription =
                                t("update_program"),

                            tint =
                                TextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            showFunctions = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Calculate,

                            contentDescription =
                                t("functions"),

                            tint =
                                TextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            showAbout = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Info,

                            contentDescription =
                                t("about"),

                            tint =
                                TextPrimary
                        )
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            Color(0xFF1E2A3A),

                        titleContentColor =
                            TextPrimary
                    )
            )
        },

        containerColor =
            DarkBackground

    ) { paddingValues ->

        Row(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {

            /*
             * ============================================================
             * LEFT SIDE
             * CALCULATION / DESIGN ELEMENTS MENU
             * ============================================================
             */

            LeftCalculationMenu(

                modifier = Modifier
                    .width(245.dp)
                    .fillMaxHeight(),

                items = menuItems,

                selectedId = selectedMenu,

                language = language,

                onSelect = {
                    selectedMenu = it
                }
            )

            /*
             * ============================================================
             * RIGHT SIDE
             * INPUT DATA / CALCULATION SCREEN
             * ============================================================
             */

            Box(

                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(DarkBackground)
            ) {

                when (selectedMenu) {

                    "home" -> {

                        WelcomeScreen(
                            language = language,
                            standard = selectedStandard,
                            onUpdate = {
                                openUpdatePage()
                            }
                        )
                    }

                    "conductor_sizing_protection" -> {

                        ConductorSizingScreen(
                            language = language,
                            standard = selectedStandard
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
                            calculation = selectedMenu,
                            language = language
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftCalculationMenu(
    modifier: Modifier,
    items: List<MenuItem>,
    selectedId: String,
    language: AppLanguage,
    onSelect: (String) -> Unit
) {

    Column(

        modifier = modifier
            .background(
                Color(0xFF182331)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 10.dp
            )
    ) {

        Text(

            text =
                Strings.get(
                    "engineering_tools",
                    language
                ),

            color =
                TextPrimary,

            fontSize =
                17.sp,

            fontWeight =
                FontWeight.Bold,

            modifier =
                Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 10.dp
                )
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        StandardSelector(

            language = language,

            selectedStandard =
                selectedStandardValue(
                    selectedId = selectedId
                ),

            onSelected = {}
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        LazyColumn(

            modifier =
                Modifier.fillMaxSize(),

            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {

            item {

                LeftMenuItem(

                    title =
                        Strings.get(
                            "conductor_sizing",
                            language
                        ),

                    icon =
                        Icons.Default.Cable,

                    selected =
                        selectedId ==
                            "conductor_sizing",

                    onClick = {
                        onSelect(
                            "conductor_sizing_protection"
                        )
                    }
                )
            }

            items(items) { item ->

                LeftMenuItem(

                    title =
                        Strings.get(
                            item.titleKey,
                            language
                        ),

                    icon =
                        item.icon,

                    selected =
                        selectedId ==
                            item.id,

                    onClick = {
                        onSelect(item.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun StandardSelector(
    language: AppLanguage,
    selectedStandard: Standard,
    onSelected: (Standard) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Card(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = true
                    },

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF263445)
                )
        ) {

            Column(

                modifier =
                    Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 9.dp
                    )
            ) {

                Text(

                    text =
                        Strings.get(
                            "standard",
                            language
                        ),

                    color =
                        TextSecondary,

                    fontSize =
                        11.sp
                )

                Text(

                    text =
                        when (selectedStandard) {

                            Standard.IEC ->
                                "IEC"

                            Standard.EGYPTIAN ->
                                Strings.get(
                                    "egyptian_code",
                                    language
                                )

                            Standard.CEI ->
                                "CEI"

                            Standard.NEC ->
                                "NEC"

                            Standard.CEC ->
                                "CEC"
                        },

                    color =
                        PrimaryTeal,

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        DropdownMenu(

            expanded =
                expanded,

            onDismissRequest = {
                expanded = false
            }
        ) {

            Standard.values().forEach { standard ->

                DropdownMenuItem(

                    text = {

                        Text(

                            text =
                                when (standard) {

                                    Standard.IEC ->
                                        "IEC"

                                    Standard.EGYPTIAN ->
                                        Strings.get(
                                            "egyptian_code",
                                            language
                                        )

                                    Standard.CEI ->
                                        "CEI"

                                    Standard.NEC ->
                                        "NEC"

                                    Standard.CEC ->
                                        "CEC"
                                }
                        )
                    },

                    onClick = {

                        onSelected(
                            standard
                        )

                        expanded = false
                    }
                )
            }
        }
    }
}

private fun selectedStandardValue(
    selectedId: String
): Standard {

    return Standard.IEC
}

@Composable
private fun LeftMenuItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {

    val background =
        if (selected) {
            Color(0xFF0D8F87)
        } else {
            Color.Transparent
        }

    val foreground =
        if (selected) {
            Color.White
        } else {
            TextSecondary
        }

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (selected) 2.dp else 0.dp
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 11.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    foreground,

                modifier =
                    Modifier.width(25.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(10.dp)
            )

            Text(

                text =
                    title,

                color =
                    foreground,

                fontSize =
                    13.sp,

                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },

                maxLines =
                    2
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    language: AppLanguage,
    standard: Standard,
    onUpdate: () -> Unit
) {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Icon(

            imageVector =
                Icons.Default.ElectricBolt,

            contentDescription =
                null,

            tint =
                PrimaryTeal,

            modifier =
                Modifier.width(70.dp)
        )

        Spacer(
            modifier =
                Modifier.height(15.dp)
        )

        Text(

            text =
                Strings.get(
                    "app_name",
                    language
                ),

            color =
                TextPrimary,

            fontSize =
                25.sp,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Text(

            text =
                Strings.get(
                    "engineering_tools",
                    language
                ),

            color =
                TextSecondary,

            fontSize =
                16.sp
        )

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        Card(

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF1E2A3A)
                )
        ) {

            Column(

                modifier =
                    Modifier.padding(20.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(

                    text =
                        when (standard) {

                            Standard.IEC ->
                                "IEC 60364"

                            Standard.EGYPTIAN ->
                                Strings.get(
                                    "egyptian_code",
                                    language
                                )

                            Standard.CEI ->
                                "CEI 64-8"

                            Standard.NEC ->
                                "NEC / NFPA 70"

                            Standard.CEC ->
                                "Canadian Electrical Code"
                        },

                    color =
                        PrimaryTeal,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(

                    text =
                        Strings.get(
                            "functions_description",
                            language
                        ),

                    color =
                        TextSecondary,

                    fontSize =
                        13.sp
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        OutlinedButton(
            onClick = onUpdate
        ) {

            Icon(
                imageVector =
                    Icons.Default.Refresh,

                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                Strings.get(
                    "update_program",
                    language
                )
            )
        }
    }
}
