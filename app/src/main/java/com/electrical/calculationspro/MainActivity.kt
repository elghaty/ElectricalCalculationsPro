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
                                text = if (isArabic) "العربية" else "EN",
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
}package com.electrical.calculationspro

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
import androidx.compose.ui.draw.clip
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
    val symbol: String
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

    fun text(key: String): String {
        return Strings.get(
            key,
            language
        )
    }

    val menuItems = remember {
        listOf(
            CalculationMenuItem(
                id = "sld_editor",
                titleKey = "sld_editor",
                symbol = "SLD"
            ),
            CalculationMenuItem(
                id = "conductor_sizing_protection",
                titleKey = "conductor_sizing_protection",
                symbol = "⚡"
            ),
            CalculationMenuItem(
                id = "voltage_drop",
                titleKey = "voltage_drop",
                symbol = "↘"
            ),
            CalculationMenuItem(
                id = "current",
                titleKey = "current",
                symbol = "I"
            ),
            CalculationMenuItem(
                id = "voltage",
                titleKey = "voltage",
                symbol = "V"
            ),
            CalculationMenuItem(
                id = "active_power",
                titleKey = "active_power",
                symbol = "P"
            ),
            CalculationMenuItem(
                id = "apparent_power",
                titleKey = "apparent_power",
                symbol = "S"
            ),
            CalculationMenuItem(
                id = "reactive_power",
                titleKey = "reactive_power",
                symbol = "Q"
            ),
            CalculationMenuItem(
                id = "power_factor",
                titleKey = "power_factor",
                symbol = "cosφ"
            ),
            CalculationMenuItem(
                id = "resistance",
                titleKey = "resistance",
                symbol = "R"
            ),
            CalculationMenuItem(
                id = "impedance",
                titleKey = "impedance",
                symbol = "Z"
            )
        )
    }

    fun checkForUpdate() {

        if (checkingUpdate) {
            return
        }

        checkingUpdate = true

        coroutineScope.launch {

            val release = updateManager.checkForUpdate()

            checkingUpdate = false

            if (release == null) {

                Toast.makeText(
                    context,
                    if (
                        language == AppLanguage.ARABIC
                    ) {
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

    if (showAbout) {

        AboutScreen(
            language = language,
            onClose = {
                showAbout = false
            }
        )

        return
    }

    if (checkingUpdate) {

        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    if (
                        language == AppLanguage.ARABIC
                    ) {
                        "التحقق من التحديث"
                    } else {
                        "Checking for updates"
                    }
                )
            },
            text = {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = PrimaryTeal
                    )

                    Spacer(
                        modifier = Modifier.width(16.dp)
                    )

                    Text(
                        if (
                            language == AppLanguage.ARABIC
                        ) {
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
                    if (
                        language == AppLanguage.ARABIC
                    ) {
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
                            if (
                                language == AppLanguage.ARABIC
                            ) {
                                "الإصدار الجديد: ${release.versionName}"
                            } else {
                                "New version: ${release.versionName}"
                            },
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold
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
                        if (
                            language == AppLanguage.ARABIC
                        ) {
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
                                    language == AppLanguage.ARABIC
                                ) {
                                    "جاري تحميل التحديث..."
                                } else {
                                    "Downloading update..."
                                },
                                Toast.LENGTH_LONG
                            ).show()

                        } else {

                            updateManager.openInstallPermissionSettings()

                            Toast.makeText(
                                context,
                                if (
                                    language == AppLanguage.ARABIC
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
                            if (
                                language == AppLanguage.ARABIC
                            ) {
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

            TopBar(
                language = language,
                languageExpanded = languageExpanded,
                onLanguageClick = {
                    languageExpanded = true
                },
                onLanguageDismiss = {
                    languageExpanded = false
                },
                onArabic = {
                    language = AppLanguage.ARABIC
                    languageExpanded = false
                },
                onEnglish = {
                    language = AppLanguage.ENGLISH
                    languageExpanded = false
                },
                onUpdate = {
                    checkForUpdate()
                },
                onFunctions = {
                    showFunctions = true
                },
                onAbout = {
                    showAbout = true
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {

                CalculationMenu(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    items = menuItems,
                    selectedId = selectedMenu,
                    language = language,
                    onSelect = {
                        selectedMenu = it
                    }
                )

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
    }
}

@Composable
private fun HomeScreen(
    language: AppLanguage,
    standard: Standard,
    onStandardChanged: (Standard) -> Unit,
    onOpenConductorSizing: () -> Unit
) {

    val isArabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            text =
                if (isArabic) {
                    "Electrical Calculations Pro"
                } else {
                    "Electrical Calculations Pro"
                },
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                if (isArabic) {
                    "منصة الحسابات والتصميمات الكهربائية الاحترافية"
                } else {
                    "Professional electrical calculations and design platform"
                },
            color = TextSecondary,
            fontSize = 16.sp
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            HomeCard(
                modifier = Modifier.weight(1f),
                title =
                    if (isArabic) {
                        "Single Line Diagram"
                    } else {
                        "Single Line Diagram"
                    },
                description =
                    if (isArabic) {
                        "إنشاء المخطط الأحادي وإدخال الأحمال ثم إجراء الحسابات upstream"
                    } else {
                        "Create the SLD, enter loads, then perform upstream calculations"
                    },
                symbol = "SLD",
                onClick = {
                    // Navigation is handled by the main menu.
                }
            )

            HomeCard(
                modifier = Modifier.weight(1f),
                title =
                    if (isArabic) {
                        "تحديد الكابلات والحماية"
                    } else {
                        "Cable & Protection Sizing"
                    },
                description =
                    if (isArabic) {
                        "اختيار مقطع الموصل وأجهزة الحماية وفق المعايير"
                    } else {
                        "Select conductor sizes and protective devices"
                    },
                symbol = "⚡",
                onClick = onOpenConductorSizing
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        StandardCard(
            language = language,
            standard = standard,
            onStandardChanged = onStandardChanged
        )
    }
}

@Composable
private fun HomeCard(
    modifier: Modifier,
    title: String,
    description: String,
    symbol: String,
    onClick: () -> Unit
) {

    Column(
        modifier = modifier
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                Color(0xFF13232D)
            )
            .clickable {
                onClick()
            }
            .padding(20.dp)
    ) {

        Text(
            text = symbol,
            color = PrimaryTeal,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = description,
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StandardCard(
    language: AppLanguage,
    standard: Standard,
    onStandardChanged: (Standard) -> Unit
) {

    val isArabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                Color(0xFF13232D)
            )
            .padding(20.dp)
    ) {

        Text(
            text =
                if (isArabic) {
                    "المعيار الهندسي"
                } else {
                    "Engineering Standard"
                },
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                if (isArabic) {
                    "المعيار الحالي: ${standard.name}"
                } else {
                    "Current standard: ${standard.name}"
                },
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            StandardButton(
                title = "IEC",
                selected = standard == Standard.IEC,
                onClick = {
                    onStandardChanged(
                        Standard.IEC
                    )
                }
            )

            StandardButton(
                title = "NEC",
                selected = standard.name == "NEC",
                onClick = {

                    val nec =
                        Standard.entries.firstOrNull {
                            it.name == "NEC"
                        }

                    if (nec != null) {
                        onStandardChanged(nec)
                    }
                }
            )
        }
    }
}

@Composable
private fun StandardButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Text(
        text = title,
        color =
            if (selected) {
                TextPrimary
            } else {
                TextSecondary
            },
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(
                RoundedCornerShape(8.dp)
            )
            .background(
                if (selected) {
                    Color(0xFF174C57)
                } else {
                    Color(0xFF0E171F)
                }
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 18.dp,
                vertical = 10.dp
            )
    )
}

@Composable
private fun TopBar(
    language: AppLanguage,
    languageExpanded: Boolean,
    onLanguageClick: () -> Unit,
    onLanguageDismiss: () -> Unit,
    onArabic: () -> Unit,
    onEnglish: () -> Unit,
    onUpdate: () -> Unit,
    onFunctions: () -> Unit,
    onAbout: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(
                Color(0xFF101B24)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "Electrical Calculations Pro",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Box {

            Text(
                text =
                    if (
                        language == AppLanguage.ARABIC
                    ) {
                        "العربية"
                    } else {
                        "English"
                    },
                color = TextPrimary,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        onLanguageClick()
                    }
                    .padding(
                        horizontal = 10.dp,
                        vertical = 8.dp
                    )
            )

            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = onLanguageDismiss
            ) {

                DropdownMenuItem(
                    text = {
                        Text("العربية")
                    },
                    onClick = onArabic
                )

                DropdownMenuItem(
                    text = {
                        Text("English")
                    },
                    onClick = onEnglish
                )
            }
        }

        TextButton(
            onClick = onUpdate
        ) {

            Text(
                if (
                    language == AppLanguage.ARABIC
                ) {
                    "تحديث"
                } else {
                    "Update"
                }
            )
        }

        TextButton(
            onClick = onFunctions
        ) {

            Text(
                if (
                    language == AppLanguage.ARABIC
                ) {
                    "الوظائف"
                } else {
                    "Functions"
                }
            )
        }

        TextButton(
            onClick = onAbout
        ) {

            Text(
                if (
                    language == AppLanguage.ARABIC
                ) {
                    "عن البرنامج"
                } else {
                    "About"
                }
            )
        }
    }
}

@Composable
private fun CalculationMenu(
    modifier: Modifier,
    items: List<CalculationMenuItem>,
    selectedId: String,
    language: AppLanguage,
    onSelect: (String) -> Unit
) {

    LazyColumn(
        modifier = modifier
            .background(
                Color(0xFF0E171F)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        item {

            Text(
                text =
                    if (
                        language == AppLanguage.ARABIC
                    ) {
                        "الحسابات الهندسية"
                    } else {
                        "ENGINEERING CALCULATIONS"
                    },
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                )
            )
        }

        item {

            MenuItem(
                title =
                    if (
                        language == AppLanguage.ARABIC
                    ) {
                        "الرئيسية"
                    } else {
                        "Home"
                    },
                symbol = "⌂",
                selected = selectedId == "home",
                onClick = {
                    onSelect("home")
                }
            )
        }

        items(
            items = items,
            key = {
                it.id
            }
        ) { item ->

            MenuItem(
                title = Strings.get(
                    item.titleKey,
                    language
                ),
                symbol = item.symbol,
                selected = selectedId == item.id,
                onClick = {
                    onSelect(item.id)
                }
            )
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(8.dp)
            )
            .background(
                if (selected) {
                    Color(0xFF174C57)
                } else {
                    Color.Transparent
                }
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 11.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = symbol,
            color =
                if (selected) {
                    PrimaryTeal
                } else {
                    TextSecondary
                },
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = title,
            color =
                if (selected) {
                    TextPrimary
                } else {
                    TextSecondary
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
