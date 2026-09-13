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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

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

    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()


    var language by remember {

        mutableStateOf(
            AppLanguage.ARABIC
        )
    }


    var selectedStandard by remember {

        mutableStateOf(
            Standard.IEC
        )
    }


    var selectedMenu by remember {

        mutableStateOf(
            "home"
        )
    }


    var languageExpanded by remember {

        mutableStateOf(
            false
        )
    }


    var showAbout by remember {

        mutableStateOf(
            false
        )
    }


    var showFunctions by remember {

        mutableStateOf(
            false
        )
    }


    var checkingUpdate by remember {

        mutableStateOf(
            false
        )
    }


    var updateRelease by remember {

        mutableStateOf<AppReleaseInfo?>(
            null
        )
    }


    val updateManager =
        remember {

            AppUpdateManager(
                context
            )
        }


    fun text(
        key: String
    ): String {

        return Strings.get(
            key,
            language
        )
    }


    val menuItems =
        remember {

            listOf(

                /*
                 * ---------------------------------------------------------
                 * SLD
                 * ---------------------------------------------------------
                 *
                 * New professional Single Line Diagram designer.
                 *
                 * Existing menu items are kept unchanged.
                 */

                CalculationMenuItem(

                    id = "sld_editor",

                    titleKey = "sld_editor",

                    symbol = "SLD"
                ),


                CalculationMenuItem(

                    id =
                        "conductor_sizing_protection",

                    titleKey =
                        "conductor_sizing_protection",

                    symbol = "⚡"
                ),


                CalculationMenuItem(

                    id =
                        "voltage_drop",

                    titleKey =
                        "voltage_drop",

                    symbol = "↘"
                ),


                CalculationMenuItem(

                    id =
                        "current",

                    titleKey =
                        "current",

                    symbol = "I"
                ),


                CalculationMenuItem(

                    id =
                        "voltage",

                    titleKey =
                        "voltage",

                    symbol = "V"
                ),


                CalculationMenuItem(

                    id =
                        "active_power",

                    titleKey =
                        "active_power",

                    symbol = "P"
                ),


                CalculationMenuItem(

                    id =
                        "apparent_power",

                    titleKey =
                        "apparent_power",

                    symbol = "S"
                ),


                CalculationMenuItem(

                    id =
                        "reactive_power",

                    titleKey =
                        "reactive_power",

                    symbol = "Q"
                ),


                CalculationMenuItem(

                    id =
                        "power_factor",

                    titleKey =
                        "power_factor",

                    symbol = "cosφ"
                ),


                CalculationMenuItem(

                    id =
                        "resistance",

                    titleKey =
                        "resistance",

                    symbol = "R"
                ),


                CalculationMenuItem(

                    id =
                        "impedance",

                    titleKey =
                        "impedance",

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

            val release =
                updateManager
                    .checkForUpdate()


            checkingUpdate = false


            if (release == null) {

                Toast.makeText(

                    context,

                    if (
                        language ==
                            AppLanguage.ARABIC
                    ) {

                        "البرنامج محدث بالفعل"

                    } else {

                        "The program is already up to date"
                    },

                    Toast.LENGTH_LONG

                ).show()

            } else {

                updateRelease =
                    release
            }
        }
    }


    if (showAbout) {

        AboutScreen(

            language =
                language,

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
                        language ==
                            AppLanguage.ARABIC
                    ) {

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

                        modifier =
                            Modifier.size(
                                30.dp
                            ),

                        color =
                            PrimaryTeal
                    )


                    Spacer(

                        modifier =
                            Modifier.width(
                                16.dp
                            )
                    )


                    Text(

                        if (
                            language ==
                                AppLanguage.ARABIC
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
                        language ==
                            AppLanguage.ARABIC
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
                                language ==
                                    AppLanguage.ARABIC
                            ) {

                                "الإصدار الجديد: ${release.versionName}"

                            } else {

                                "New version: ${release.versionName}"
                            },

                        color =
                            PrimaryTeal,

                        fontWeight =
                            FontWeight.Bold
                    )


                    if (
                        release
                            .releaseNotes
                            .isNotBlank()
                    ) {

                        Spacer(

                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )


                        Text(

                            text =
                                release.releaseNotes,

                            color =
                                TextSecondary
                        )
                    }
                }
            },


            dismissButton = {

                TextButton(

                    onClick = {

                        updateRelease =
                            null
                    }
                ) {

                    Text(

                        if (
                            language ==
                                AppLanguage.ARABIC
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


                        updateRelease =
                            null


                        if (
                            releaseToInstall ==
                            null
                        ) {

                            return@TextButton
                        }


                        if (
                            updateManager
                                .canInstallPackages()
                        ) {

                            updateManager
                                .downloadAndInstall(
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
                            if (
                                language ==
                                    AppLanguage.ARABIC
                            ) {

                                "تحديث الآن"

                            } else {

                                "Update now"
                            },

                        color =
                            PrimaryTeal,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        )
    }


    if (showFunctions) {

        AlertDialog(

            onDismissRequest = {

                showFunctions =
                    false
            },


            title = {

                Text(
                    text("functions")
                )
            },


            text = {

                Text(
                    text(
                        "functions_description"
                    )
                )
            },


            confirmButton = {

                TextButton(

                    onClick = {

                        showFunctions =
                            false
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

        containerColor =
            DarkBackground

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .background(
                        DarkBackground
                    )
        ) {

            TopBar(

                language =
                    language,

                languageExpanded =
                    languageExpanded,

                onLanguageClick = {

                    languageExpanded =
                        true
                },

                onLanguageDismiss = {

                    languageExpanded =
                        false
                },

                onArabic = {

                    language =
                        AppLanguage.ARABIC

                    languageExpanded =
                        false
                },

                onEnglish = {

                    language =
                        AppLanguage.ENGLISH

                    languageExpanded =
                        false
                },

                onUpdate = {

                    checkForUpdate()
                },

                onFunctions = {

                    showFunctions =
                        true
                },

                onAbout = {

                    showAbout =
                        true
                }
            )


            Row(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            DarkBackground
                        )
            ) {

                CalculationMenu(

                    modifier =
                        Modifier
                            .width(
                                300.dp
                            )
                            .fillMaxHeight(),

                    items =
                        menuItems,

                    selectedId =
                        selectedMenu,

                    language =
                        language,

                    onSelect = {

                        selectedMenu =
                            it
                    }
                )


                Box(

                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                ) {

                    when (
                        selectedMenu
                    ) {

                        "home" -> {

                            HomeScreen(

                                language =
                                    language,

                                standard =
                                    selectedStandard,

                                onStandardChanged = {

                                    selectedStandard =
                                        it
                                },

                                onOpenConductorSizing = {

                                    selectedMenu =
                                        "conductor_sizing_protection"
                                }
                            )
                        }


                        /*
                         * -------------------------------------------------
                         * SLD SCREEN
                         * -------------------------------------------------
                         */

                        "sld_editor" -> {

                            SldEditorScreen(

                                language =
                                    language
                            )
                        }


                        "conductor_sizing_protection" -> {

                            ConductorSizingScreen(

                                language =
                                    language,

                                standard =
                                    selectedStandard
                            )
                        }


                        else -> {

                            EngineeringCalculatorScreen(

                                calculation =
                                    selectedMenu,

                                language =
                                    language
                            )
                        }
                    }
                }
            }
        }
    }
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

        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    64.dp
                )
                .background(
                    Color(0xFF182536)
                )
                .padding(
                    horizontal = 12.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(

            modifier =
                Modifier
                    .size(42.dp)
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .background(
                        PrimaryTeal
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(

                text = "⚡",

                fontSize =
                    25.sp,

                color =
                    Color.White
            )
        }


        Spacer(

            modifier =
                Modifier.width(
                    12.dp
                )
        )


        Column(

            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(

                text =
                    Strings.get(
                        "app_name",
                        language
                    ),

                color =
                    TextPrimary,

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(

                text =
                    if (
                        language ==
                            AppLanguage.ARABIC
                    ) {

                        "الهندسة الكهربائية والحسابات"

                    } else {

                        "Electrical Engineering Calculations"
                    },

                color =
                    TextSecondary,

                fontSize =
                    11.sp
            )
        }


        Box {

            TextButton(

                onClick =
                    onLanguageClick

            ) {

                Text(

                    text =
                        if (
                            language ==
                                AppLanguage.ARABIC
                        ) {

                            "العربية"

                        } else {

                            "EN"
                        },

                    color =
                        TextPrimary,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            DropdownMenu(

                expanded =
                    languageExpanded,

                onDismissRequest =
                    onLanguageDismiss

            ) {

                DropdownMenuItem(

                    text = {

                        Text(
                            "العربية"
                        )
                    },

                    onClick =
                        onArabic
                )


                DropdownMenuItem(

                    text = {

                        Text(
                            "English"
                        )
                    },

                    onClick =
                        onEnglish
                )
            }
        }


        TextButton(

            onClick =
                onUpdate

        ) {

            Text(

                text =
                    if (
                        language ==
                            AppLanguage.ARABIC
                    ) {

                        "تحديث"

                    } else {

                        "Update"
                    },

                color =
                    TextPrimary
            )
        }


        TextButton(

            onClick =
                onFunctions

        ) {

            Text(

                text =
                    if (
                        language ==
                            AppLanguage.ARABIC
                    ) {

                        "الحسابات"

                    } else {

                        "Calculations"
                    },

                color =
                    TextPrimary
            )
        }


        TextButton(

            onClick =
                onAbout

        ) {

            Text(

                text =
                    if (
                        language ==
                            AppLanguage.ARABIC
                    ) {

                        "حول"

                    } else {

                        "About"
                    },

                color =
                    TextPrimary
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

    Column(

        modifier =
            modifier
                .background(
                    Color(0xFF111C29)
                )
                .padding(
                    10.dp
                )
    ) {

        Text(

            text =
                if (
                    language ==
                        AppLanguage.ARABIC
                ) {

                    "الحسابات الكهربائية"

                } else {

                    "Electrical Calculations"
                },

            color =
                PrimaryTeal,

            fontSize =
                17.sp,

            fontWeight =
                FontWeight.Bold,

            modifier =
                Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 14.dp
                )
        )


        LazyColumn(

            modifier =
                Modifier.fillMaxSize()

        ) {

            item {

                MenuHomeItem(

                    selected =
                        selectedId ==
                            "home",

                    language =
                        language,

                    onClick = {

                        onSelect(
                            "home"
                        )
                    }
                )
            }


            items(items) { item ->

                MenuCalculationItem(

                    item =
                        item,

                    selected =
                        selectedId ==
                            item.id,

                    language =
                        language,

                    onClick = {

                        onSelect(
                            item.id
                        )
                    }
                )
            }
        }
    }
}


@Composable
private fun MenuHomeItem(

    selected: Boolean,

    language: AppLanguage,

    onClick: () -> Unit

) {

    val background =

        if (selected) {

            Color(
                0xFF164E55
            )

        } else {

            Color.Transparent
        }


    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .background(
                    background
                )
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(

            text = "⌂",

            color =
                PrimaryTeal,

            fontSize =
                24.sp,

            modifier =
                Modifier.width(
                    38.dp
                )
        )


        Text(

            text =
                if (
                    language ==
                        AppLanguage.ARABIC
                ) {

                    "الرئيسية"

                } else {

                    "Home"
                },

            color =
                TextPrimary,

            fontSize =
                15.sp,

            fontWeight =

                if (selected) {

                    FontWeight.Bold

                } else {

                    FontWeight.Normal
                }
        )
    }
}


@Composable
private fun MenuCalculationItem(

    item: CalculationMenuItem,

    selected: Boolean,

    language: AppLanguage,

    onClick: () -> Unit

) {

    val background =

        if (selected) {

            Color(
                0xFF164E55
            )

        } else {

            Color.Transparent
        }


    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .background(
                    background
                )
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(

            modifier =
                Modifier
                    .size(38.dp)
                    .clip(
                        RoundedCornerShape(
                            10.dp
                        )
                    )
                    .background(

                        if (selected) {

                            PrimaryTeal

                        } else {

                            Color(
                                0xFF26384B
                            )
                        }
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(

                text =
                    item.symbol,

                color =
                    Color.White,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center
            )
        }


        Spacer(

            modifier =
                Modifier.width(
                    12.dp
                )
        )


        Text(

            text =
                Strings.get(
                    item.titleKey,
                    language
                ),

            color =
                TextPrimary,

            fontSize =
                14.sp,

            fontWeight =

                if (selected) {

                    FontWeight.Bold

                } else {

                    FontWeight.Normal
                }
        )
    }
}


@Composable
private fun HomeScreen(

    language: AppLanguage,

    standard: Standard,

    onStandardChanged:
        (Standard) -> Unit,

    onOpenConductorSizing:
        () -> Unit

) {

    val arabic =
        language ==
            AppLanguage.ARABIC


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    DarkBackground
                )
                .padding(
                    28.dp
                )
    ) {

        Text(

            text =
                "Electrical Calculations Pro",

            color =
                TextPrimary,

            fontSize =
                30.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(

            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Text(

            text =
                if (arabic) {

                    "منصة احترافية للحسابات والتصميمات الكهربائية"

                } else {

                    "Professional platform for electrical calculations and design"
                },

            color =
                TextSecondary,

            fontSize =
                16.sp
        )


        Spacer(

            modifier =
                Modifier.height(
                    28.dp
                )
        )


        Card(

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                CardDefaults.cardColors(

                    containerColor =
                        Color(
                            0xFF1C2A3A
                        )
                ),

            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        3.dp
                )
        ) {

            Column(

                modifier =
                    Modifier.padding(
                        24.dp
                    )
            ) {

                Text(

                    text =
                        if (arabic) {

                            "المعيار الهندسي"

                        } else {

                            "Engineering Standard"
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
                        Modifier.height(
                            10.dp
                        )
                )


                Text(

                    text =
                        standard.name,

                    color =
                        TextPrimary,

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(

                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )


                Row(

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    Button(

                        onClick = {

                            onStandardChanged(
                                Standard.IEC
                            )
                        }

                    ) {

                        Text(
                            "IEC"
                        )
                    }


                    Button(

                        onClick =
                            onOpenConductorSizing

                    ) {

                        Text(

                            if (arabic) {

                                "ابدأ التصميم"

                            } else {

                                "Start Design"
                            }
                        )
                    }
                }
            }
        }


        Spacer(

            modifier =
                Modifier.height(
                    24.dp
                )
        )


        Text(

            text =
                if (arabic) {

                    "اختر عملية الحساب من القائمة الجانبية للبدء."

                } else {

                    "Choose a calculation from the menu to begin."
                },

            color =
                TextSecondary,

            fontSize =
                15.sp
        )
    }
}
