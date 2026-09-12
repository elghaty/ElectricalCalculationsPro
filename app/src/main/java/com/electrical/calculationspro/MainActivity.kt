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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

data class MenuItem(
    val id: String,
    val titleKey: String,
    val icon: ImageVector,
    val iconBackground: Color
)

@Composable
fun MainScreen() {

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

    var languageExpanded by remember {
        mutableStateOf(false)
    }

    var showAbout by remember {
        mutableStateOf(false)
    }

    var showFunctions by remember {
        mutableStateOf(false)
    }

    var checkingUpdate by remember {
        mutableStateOf(false)
    }

    var updateRelease by remember {
        mutableStateOf<AppReleaseInfo?>(null)
    }

    val updateManager = remember {
        AppUpdateManager(context)
    }

    val menuItems = remember {

        listOf(

            MenuItem(
                id = "conductor_sizing_protection",
                titleKey = "conductor_sizing_protection",
                icon = Icons.Default.Security,
                iconBackground = Color(0xFF607D8B)
            ),

            MenuItem(
                id = "voltage_drop",
                titleKey = "voltage_drop",
                icon = Icons.Default.Bolt,
                iconBackground = Color(0xFFE53935)
            ),

            MenuItem(
                id = "current",
                titleKey = "current",
                icon = Icons.Default.ElectricBolt,
                iconBackground = Color(0xFF4CAF50)
            ),

            MenuItem(
                id = "voltage",
                titleKey = "voltage",
                icon = Icons.Default.Bolt,
                iconBackground = Color(0xFFE91E63)
            ),

            MenuItem(
                id = "active_power",
                titleKey = "active_power",
                icon = Icons.Default.Power,
                iconBackground = Color(0xFF9C27B0)
            ),

            MenuItem(
                id = "apparent_power",
                titleKey = "apparent_power",
                icon = Icons.Default.ElectricBolt,
                iconBackground = Color(0xFF3F51B5)
            ),

            MenuItem(
                id = "reactive_power",
                titleKey = "reactive_power",
                icon = Icons.Default.Power,
                iconBackground = Color(0xFF00BCD4)
            ),

            MenuItem(
                id = "power_factor",
                titleKey = "power_factor",
                icon = Icons.Default.Calculate,
                iconBackground = Color(0xFFFF9800)
            ),

            MenuItem(
                id = "resistance",
                titleKey = "resistance",
                icon = Icons.Default.Settings,
                iconBackground = Color(0xFFFFEB3B)
            ),

            MenuItem(
                id = "impedance",
                titleKey = "impedance",
                icon = Icons.Default.Settings,
                iconBackground = Color(0xFFFFEB3B)
            )
        )
    }

    fun text(key: String): String {
        return Strings.get(key, language)
    }

    fun checkForUpdate() {

        if (checkingUpdate) {
            return
        }

        checkingUpdate = true

        coroutineScope.launch {

            val release =
                updateManager.checkForUpdate()

            checkingUpdate = false

            if (release == null) {

                Toast.makeText(
                    context,
                    if (language == AppLanguage.ARABIC) {
                        "البرنامج محدث بالفعل"
                    } else {
                        "The program is already up to date"
                    },
                    Toast.LENGTH_LONG
                ).show()

            } else {

                updateRelease = release
            }
        }
    }

    if (checkingUpdate) {

        AlertDialog(

            onDismissRequest = {
                // Prevent closing while checking
            },

            title = {
                Text(
                    if (language == AppLanguage.ARABIC) {
                        "التحقق من التحديث"
                    } else {
                        "Checking for updates"
                    }
                )
            },

            text = {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = PrimaryTeal
                    )

                    Spacer(
                        modifier = Modifier.width(16.dp)
                    )

                    Text(
                        if (language == AppLanguage.ARABIC) {
                            "جاري البحث عن أحدث إصدار..."
                        } else {
                            "Checking for the latest version..."
                        }
                    )
                }
            },

            confirmButton = {}
        )
    }

    updateRelease?.let { release ->

        AlertDialog(

            onDismissRequest = {
                updateRelease = null
            },

            title = {
                Text(
                    if (language == AppLanguage.ARABIC) {
                        "تحديث جديد متاح"
                    } else {
                        "New update available"
                    }
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            if (language == AppLanguage.ARABIC) {
                                "الإصدار الجديد: ${release.versionName}"
                            } else {
                                "New version: ${release.versionName}"
                            },
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )

                    if (release.releaseNotes.isNotBlank()) {

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = release.releaseNotes,
                            color = TextSecondary
                        )
                    }
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        updateRelease = null
                    }
                ) {

                    Text(
                        if (language == AppLanguage.ARABIC) {
                            "لاحقاً"
                        } else {
                            "Later"
                        }
                    )
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val releaseToInstall =
                            updateRelease

                        updateRelease = null

                        if (releaseToInstall == null) {
                            return@TextButton
                        }

                        if (
                            updateManager.canInstallPackages()
                        ) {

                            updateManager.downloadAndInstall(
                                releaseToInstall
                            )

                            Toast.makeText(
                                context,
                                if (
                                    language ==
                                    AppLanguage.ARABIC
                                ) {
                                    "جاري تحميل التحديث..."
                                } else {
                                    "Downloading update..."
                                },
                                Toast.LENGTH_LONG
                            ).show()

                        } else {

                            updateManager
                                .openInstallPermissionSettings()

                            Toast.makeText(
                                context,
                                if (
                                    language ==
                                    AppLanguage.ARABIC
                                ) {
                                    "اسمح للبرنامج بتثبيت التطبيقات من هذا المصدر ثم اضغط تحديث مرة أخرى"
                                } else {
                                    "Allow this app to install unknown apps, then press Update again"
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {

                    Text(
                        text =
                            if (language == AppLanguage.ARABIC) {
                                "تحديث الآن"
                            } else {
                                "Update now"
                            },
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    if (showAbout) {

        AlertDialog(

            onDismissRequest = {
                showAbout = false
            },

            title = {
                Text(
                    text("app_name")
                )
            },

            text = {
                Text(
                    text("about_description")
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showAbout = false
                    }
                ) {

                    Text(
                        text("close")
                    )
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
                    text("functions")
                )
            },

            text = {
                Text(
                    text("functions_description")
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showFunctions = false
                    }
                ) {

                    Text(
                        text("close")
                    )
                }
            }
        )
    }

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {

            /*
             * ============================================================
             * TOP BAR
             * ============================================================
             */

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(
                        Color(0xFF1E2A3A)
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = {
                        selectedMenu = "home"
                    }
                ) {

                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription =
                            text("app_name"),
                        tint = TextPrimary
                    )
                }

                Text(
                    text =
                        if (selectedMenu == "home") {
                            text("app_name")
                        } else {
                            text(selectedMenu)
                        },
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

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
                                text("language"),
                            tint = TextPrimary
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
                                    text("arabic")
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
                                    text("english")
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

                /*
                 * ========================================================
                 * UPDATE BUTTON
                 * ========================================================
                 */

                IconButton(
                    onClick = {
                        checkForUpdate()
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.SystemUpdate,
                        contentDescription =
                            text("update_program"),
                        tint = TextPrimary
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
                            text("functions"),
                        tint = TextPrimary
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
                            text("about"),
                        tint = TextPrimary
                    )
                }
            }

            /*
             * ============================================================
             * MAIN SPLIT SCREEN
             *
             * LEFT  = calculation/design list
             * RIGHT = input/data/calculation screen
             * ============================================================
             */

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {

                /*
                 * ========================================================
                 * LEFT MENU
                 * ========================================================
                 */

                LeftCalculationMenu(
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight(),

                    items = menuItems,

                    selectedId = selectedMenu,

                    selectedStandard =
                        selectedStandard,

                    language = language,

                    onStandardSelected = {
                        selectedStandard = it
                    },

                    onSelect = {
                        selectedMenu = it
                    }
                )

                /*
                 * ========================================================
                 * RIGHT DATA AREA
                 * ========================================================
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
                                standard =
                                    selectedStandard,
                                onUpdate = {
                                    checkForUpdate()
                                }
                            )
                        }

                        "conductor_sizing_protection" -> {

                            ConductorSizingScreen(
                                language = language,
                                standard =
                                    selectedStandard
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
                                calculation =
                                    selectedMenu,
                                language = language
                            )
                        }
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
    selectedStandard: Standard,
    language: AppLanguage,
    onStandardSelected: (Standard) -> Unit,
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
            text = Strings.get(
                "engineering_tools",
                language
            ),
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 10.dp
            )
        )

        StandardSelector(
            language = language,
            selectedStandard =
                selectedStandard,
            onSelected =
                onStandardSelected
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {

            item {

                LeftMenuItem(
                    title = Strings.get(
                        "conductor_sizing",
                        language
                    ),
                    icon =
                        Icons.Default.Cable,
                    iconBackground =
                        Color(0xFF4CAF50),
                    selected =
                        selectedId ==
                            "conductor_sizing_protection",
                    onClick = {

                        onSelect(
                            "conductor_sizing_protection"
                        )
                    }
                )
            }

            items(
                items = items,
                key = {
                    it.id
                }
            ) { item ->

                LeftMenuItem(
                    title = Strings.get(
                        item.titleKey,
                        language
                    ),
                    icon = item.icon,
                    iconBackground =
                        item.iconBackground,
                    selected =
                        selectedId == item.id,
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
        modifier = Modifier.fillMaxWidth()
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = true
                },
            colors = CardDefaults.cardColors(
                containerColor =
                    Color(0xFF263445)
            )
        ) {

            Column(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 9.dp
                )
            ) {

                Text(
                    text = Strings.get(
                        "standard",
                        language
                    ),
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = standardName(
                        selectedStandard,
                        language
                    ),
                    color = PrimaryTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            Standard.values().forEach { standard ->

                DropdownMenuItem(
                    text = {

                        Text(
                            standardName(
                                standard,
                                language
                            )
                        )
                    },
                    onClick = {

                        onSelected(standard)

                        expanded = false
                    }
                )
            }
        }
    }
}

private fun standardName(
    standard: Standard,
    language: AppLanguage
): String {

    return when (standard) {

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
}

@Composable
private fun LeftMenuItem(
    title: String,
    icon: ImageVector,
    iconBackground: Color,
    selected: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) {
                    Color(0xFF294B55)
                } else {
                    Color(0xFF202D3D)
                }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = iconBackground,
                        shape =
                            androidx.compose.foundation
                                .shape
                                .RoundedCornerShape(
                                    8.dp
                                )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint =
                        if (
                            iconBackground ==
                            Color(0xFFFFEB3B)
                        ) {
                            Color.Black
                        } else {
                            Color.White
                        }
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = title,
                color =
                    if (selected) {
                        PrimaryTeal
                    } else {
                        TextPrimary
                    },
                fontSize = 13.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
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
            .padding(24.dp)
            .background(DarkBackground),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Icon(
            imageVector =
                Icons.Default.ElectricBolt,
            contentDescription = null,
            tint = PrimaryTeal,
            modifier = Modifier.size(72.dp)
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            text = Strings.get(
                "app_name",
                language
            ),
            color = TextPrimary,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = Strings.get(
                "engineering_tools",
                language
            ),
            color = TextSecondary,
            fontSize = 16.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = standardName(
                standard,
                language
            ),
            color = PrimaryTeal,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        TextButton(
            onClick = onUpdate
        ) {

            Icon(
                imageVector =
                    Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = PrimaryTeal
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = Strings.get(
                    "update_program",
                    language
                ),
                color = PrimaryTeal
            )
        }
    }
}
