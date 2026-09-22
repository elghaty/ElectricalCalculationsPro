package com.electrical.calculationspro

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Standard

import com.electrical.calculationspro.ui.screens.AboutScreen
import com.electrical.calculationspro.ui.screens.ConductorSizingScreen
import com.electrical.calculationspro.ui.screens.EngineeringCalculatorScreen
import com.electrical.calculationspro.ui.screens.ProfessionalVoltageDropScreen
import com.electrical.calculationspro.ui.screens.sld.SldEditorScreen

import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.DarkSurface
import com.electrical.calculationspro.ui.theme.DividerColor
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary
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


/* -------------------------------------------------------------------------- */
/* DASHBOARD ITEM                                                             */
/* -------------------------------------------------------------------------- */

private data class CalculationItem(
    val id: String,
    val englishName: String,
    val arabicName: String,
    val englishDescription: String,
    val arabicDescription: String,
    val icon: ImageVector
)


/* -------------------------------------------------------------------------- */
/* DASHBOARD ITEMS                                                            */
/* -------------------------------------------------------------------------- */

private val calculationItems = listOf(

    CalculationItem(
        id = "current",
        englishName = "Current",
        arabicName = "حساب التيار",
        englishDescription = "Electrical current",
        arabicDescription = "حساب التيار الكهربائي",
        icon = Icons.Outlined.Bolt
    ),

    CalculationItem(
        id = "active_power",
        englishName = "Active Power",
        arabicName = "القدرة الفعالة",
        englishDescription = "Active power in kW",
        arabicDescription = "القدرة الفعالة بالكيلووات",
        icon = Icons.Outlined.ElectricalServices
    ),

    CalculationItem(
        id = "apparent_power",
        englishName = "Apparent Power",
        arabicName = "القدرة الظاهرية",
        englishDescription = "Apparent power in kVA",
        arabicDescription = "القدرة الظاهرية بالكيلو فولت أمبير",
        icon = Icons.Outlined.GridView
    ),

    CalculationItem(
        id = "reactive_power",
        englishName = "Reactive Power",
        arabicName = "القدرة غير الفعالة",
        englishDescription = "Reactive power in kvar",
        arabicDescription = "القدرة غير الفعالة kvar",
        icon = Icons.Outlined.Timeline
    ),

    CalculationItem(
        id = "power_factor",
        englishName = "Power Factor",
        arabicName = "معامل القدرة",
        englishDescription = "Power factor",
        arabicDescription = "معامل القدرة",
        icon = Icons.Outlined.Speed
    ),

    CalculationItem(
        id = "voltage_drop",
        englishName = "Voltage Drop",
        arabicName = "هبوط الجهد",
        englishDescription = "Professional voltage-drop study",
        arabicDescription = "دراسة احترافية لهبوط الجهد",
        icon = Icons.Outlined.Timeline
    ),

    CalculationItem(
        id = "conductor_sizing",
        englishName = "Cable Sizing",
        arabicName = "اختيار مقطع الكابل",
        englishDescription = "Conductor and cable sizing",
        arabicDescription = "اختيار مقطع الموصل والكابل",
        icon = Icons.Outlined.Cable
    ),

    CalculationItem(
        id = "resistance",
        englishName = "Resistance",
        arabicName = "المقاومة",
        englishDescription = "Electrical resistance",
        arabicDescription = "المقاومة الكهربائية",
        icon = Icons.Outlined.Calculate
    ),

    CalculationItem(
        id = "impedance",
        englishName = "Impedance",
        arabicName = "الممانعة",
        englishDescription = "Electrical impedance",
        arabicDescription = "الممانعة الكهربائية",
        icon = Icons.Outlined.AccountTree
    )
)


/* -------------------------------------------------------------------------- */
/* MAIN APP                                                                   */
/* -------------------------------------------------------------------------- */

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
            .background(DarkBackground)
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

        when (selectedScreen) {

            "sld" -> {

                SldEditorScreen(
                    language = language,
                    onBack = {
                        selectedScreen = "home"
                    }
                )
            }

            "voltage_drop" -> {

                ProfessionalVoltageDropScreen(
                    language = language,
                    standard = standard,
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
                    language = language,
                    onBack = {
                        selectedScreen = "home"
                    }
                )
            }

            else -> {

                HomeScreen(
                    language = language,

                    onSelectCalculation = {
                        selectedScreen = it
                    },

                    onSld = {
                        selectedScreen = "sld"
                    }
                )
            }
        }
    }
}


/* -------------------------------------------------------------------------- */
/* TOP BAR                                                                    */
/* -------------------------------------------------------------------------- */

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
            .height(68.dp)
            .background(DarkSurface)
            .padding(horizontal = 14.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text =
                    if (language == AppLanguage.ARABIC)
                        "الحسابات الكهربائية الاحترافية"
                    else
                        "Electrical Calculations Pro",

                color = TextPrimary,

                fontSize = 18.sp,

                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Professional Engineering Suite",

                color = TextSecondary,

                fontSize = 11.sp
            )
        }


        Box {

            OutlinedButton(
                onClick = {
                    standardExpanded = true
                }
            ) {

                Text(
                    text = standard.shortName,
                    fontSize = 12.sp
                )
            }

            DropdownMenu(
                expanded = standardExpanded,

                onDismissRequest = {
                    standardExpanded = false
                }
            ) {

                Standard.entries.forEach { item ->

                    DropdownMenuItem(
                        text = {
                            Text(item.shortName)
                        },

                        onClick = {
                            standardExpanded = false
                            onStandardChanged(item)
                        }
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.width(7.dp)
        )


        Box {

            OutlinedButton(
                onClick = {
                    languageExpanded = true
                }
            ) {

                Text(
                    text =
                        if (language == AppLanguage.ARABIC)
                            "العربية"
                        else
                            "English",

                    fontSize = 12.sp
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


        IconButton(
            onClick = onAbout
        ) {

            Icon(
                imageVector = Icons.Outlined.Info,

                contentDescription =
                    if (language == AppLanguage.ARABIC)
                        "حول البرنامج"
                    else
                        "About",

                tint = PrimaryTeal
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* HOME / DASHBOARD                                                           */
/* -------------------------------------------------------------------------- */

@Composable
private fun HomeScreen(
    language: AppLanguage,
    onSelectCalculation: (String) -> Unit,
    onSld: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            )
    ) {

        Text(
            text =
                if (arabic)
                    "لوحة التحكم الهندسية"
                else
                    "Engineering Dashboard",

            color = TextPrimary,

            fontSize = 26.sp,

            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text =
                if (arabic)
                    "أدوات التصميم والحسابات الكهربائية"
                else
                    "Electrical design and engineering calculations",

            color = TextSecondary,

            fontSize = 13.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )


        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),

            shape = RoundedCornerShape(18.dp),

            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),

            onClick = onSld
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(64.dp)
                        .background(
                            PrimaryTeal,
                            RoundedCornerShape(16.dp)
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.AccountTree,

                        contentDescription = null,

                        tint =
                            androidx.compose.ui.graphics.Color.White,

                        modifier = Modifier
                            .width(36.dp)
                            .height(36.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            if (arabic)
                                "تصميم الشبكة SLD"
                            else
                                "Single Line Diagram",

                        color = TextPrimary,

                        fontSize = 18.sp,

                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            if (arabic)
                                "مصدر • محول • مولد • Busbar • Breaker • Panel • Load"
                            else
                                "Source • Transformer • Generator • Busbar • Breaker • Panel • Load",

                        color = TextSecondary,

                        fontSize = 11.sp
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(18.dp)
        )


        Text(
            text =
                if (arabic)
                    "الحسابات والتصميم"
                else
                    "CALCULATIONS & DESIGN",

            color = TextSecondary,

            fontSize = 12.sp,

            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )


        LazyVerticalGrid(

            columns =
                GridCells.Adaptive(
                    minSize = 165.dp
                ),

            modifier =
                Modifier.fillMaxSize(),

            horizontalArrangement =
                Arrangement.spacedBy(11.dp),

            verticalArrangement =
                Arrangement.spacedBy(11.dp)
        ) {

            items(
                items = calculationItems,
                key = {
                    it.id
                }
            ) { item ->

                CalculationCard(
                    item = item,
                    language = language,
                    onClick = {
                        onSelectCalculation(
                            item.id
                        )
                    }
                )
            }
        }
    }
}


/* -------------------------------------------------------------------------- */
/* CALCULATION CARD                                                           */
/* -------------------------------------------------------------------------- */

@Composable
private fun CalculationCard(
    item: CalculationItem,
    language: AppLanguage,
    onClick: () -> Unit
) {

    val arabic =
        language == AppLanguage.ARABIC

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),

        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),

            verticalArrangement =
                Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp)
                    .background(
                        PrimaryTeal,
                        RoundedCornerShape(14.dp)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector = item.icon,

                    contentDescription = null,

                    tint =
                        androidx.compose.ui.graphics.Color.White,

                    modifier = Modifier
                        .width(28.dp)
                        .height(28.dp)
                )
            }

            Column {

                Text(
                    text =
                        if (arabic)
                            item.arabicName
                        else
                            item.englishName,

                    color = TextPrimary,

                    fontSize = 15.sp,

                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text =
                        if (arabic)
                            item.arabicDescription
                        else
                            item.englishDescription,

                    color = TextSecondary,

                    fontSize = 10.sp
                )
            }
        }
    }
}
