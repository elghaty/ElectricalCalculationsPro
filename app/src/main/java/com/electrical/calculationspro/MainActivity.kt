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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Transform
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class CalculationItem(
    val id: String,
    val englishName: String,
    val arabicName: String,
    val englishDescription: String,
    val arabicDescription: String,
    val icon: ImageVector
)

private val calculationItems = listOf(

    CalculationItem(
        id = "current",
        englishName = "Current",
        arabicName = "حساب التيار",
        englishDescription = "Calculate electrical current",
        arabicDescription = "حساب التيار الكهربائي",
        icon = Icons.Outlined.Bolt
    ),

    CalculationItem(
        id = "voltage",
        englishName = "Voltage",
        arabicName = "حساب الجهد",
        englishDescription = "Voltage and electrical quantities",
        arabicDescription = "حساب الجهد والكميات الكهربائية",
        icon = Icons.Outlined.Power
    ),

    CalculationItem(
        id = "active_power",
        englishName = "Active Power",
        arabicName = "القدرة الفعالة",
        englishDescription = "Active power in kW",
        arabicDescription = "القدرة الفعالة بالكيلووات kW",
        icon = Icons.Outlined.ElectricalServices
    ),

    CalculationItem(
        id = "apparent_power",
        englishName = "Apparent Power",
        arabicName = "القدرة الظاهرية",
        englishDescription = "Apparent power in kVA",
        arabicDescription = "القدرة الظاهرية بالكيلو فولت أمبير kVA",
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
        englishDescription = "Power factor calculation",
        arabicDescription = "حساب معامل القدرة",
        icon = Icons.Outlined.Speed
    ),

    CalculationItem(
        id = "voltage_drop",
        englishName = "Voltage Drop",
        arabicName = "هبوط الجهد",
        englishDescription = "Voltage drop calculation",
        arabicDescription = "حساب هبوط الجهد",
        icon = Icons.Outlined.Timeline
    ),

    CalculationItem(
        id = "conductor_sizing",
        englishName = "Cable Sizing",
        arabicName = "اختيار مقطع الكابل",
        englishDescription = "Cable and conductor sizing",
        arabicDescription = "اختيار مقطع الموصل والكابل",
        icon = Icons.Outlined.Cable
    ),

    CalculationItem(
        id = "resistance",
        englishName = "Resistance",
        arabicName = "المقاومة",
        englishDescription = "Electrical resistance",
        arabicDescription = "حساب المقاومة الكهربائية",
        icon = Icons.Outlined.Calculate
    ),

    CalculationItem(
        id = "impedance",
        englishName = "Impedance",
        arabicName = "الممانعة",
        englishDescription = "Electrical impedance",
        arabicDescription = "حساب الممانعة الكهربائية",
        icon = Icons.Outlined.AccountTree
    )
)

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
            .background(Color(0xFF0B1116))
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {

                    SldEditorScreen(
                        language = language,
                        onBack = {
                            selectedScreen = "home"
                        }
                    )
                }
            }

            "conductor_sizing" -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {

                    ConductorSizingScreen(
                        language = language,
                        standard = standard
                    )
                }
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {

                    EngineeringCalculatorScreen(
                        calculation = selectedScreen,
                        language = language
                    )
                }
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
            .height(64.dp)
            .background(Color(0xFF151D24))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {

            Text(
                text =
                    if (language == AppLanguage.ARABIC)
                        "الحسابات الكهربائية الاحترافية"
                    else
                        "Electrical Calculations Pro",
                color = Color.White,
                fontSize = 18.sp
            )

            Text(
                text =
                    if (language == AppLanguage.ARABIC)
                        "Professional Engineering Suite"
                    else
                        "Professional Engineering Suite",
                color = Color(0xFF8797A2),
                fontSize = 11.sp
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Box {

            OutlinedButton(
                onClick = {
                    standardExpanded = true
                }
            ) {

                Text(
                    text = standard.name,
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
                            Text(item.name)
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
            modifier = Modifier.width(8.dp)
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

        Spacer(
            modifier = Modifier.width(4.dp)
        )

        IconButton(
            onClick = onAbout
        ) {

            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "About",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun HomeScreen(
    language: AppLanguage,
    onSelectCalculation: (String) -> Unit,
    onSld: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 18.dp,
                vertical = 14.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (language == AppLanguage.ARABIC)
                            "الحسابات الهندسية"
                        else
                            "Engineering Calculations",
                    color = Color.White,
                    fontSize = 26.sp
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        if (language == AppLanguage.ARABIC)
                            "اختر نوع الحساب"
                        else
                            "Select a calculation",
                    color = Color(0xFFAAB7C0),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF172832)
            ),
            onClick = onSld
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF243D49)),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Outlined.AccountTree,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .width(34.dp)
                            .height(34.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Column {

                    Text(
                        text =
                            if (language == AppLanguage.ARABIC)
                                "المخطط الأحادي الاحترافي SLD"
                            else
                                "Professional Single Line Diagram",
                        color = Color.White,
                        fontSize = 17.sp
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            if (language == AppLanguage.ARABIC)
                                "إنشاء الشبكة وإضافة المصادر والمحولات والمولدات واللوحات والأحمال"
                            else
                                "Design networks, sources, transformers, generators, panels and loads",
                        color = Color(0xFF9EADB5),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(
                minSize = 170.dp
            ),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(calculationItems) { item ->

                CalculationCard(
                    item = item,
                    language = language,
                    onClick = {
                        onSelectCalculation(item.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun CalculationCard(
    item: CalculationItem,
    language: AppLanguage,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151F27)
        ),
        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF24343E)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .width(30.dp)
                        .height(30.dp)
                )
            }

            Column {

                Text(
                    text =
                        if (language == AppLanguage.ARABIC)
                            item.arabicName
                        else
                            item.englishName,
                    color = Color.White,
                    fontSize = 16.sp
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text =
                        if (language == AppLanguage.ARABIC)
                            item.arabicDescription
                        else
                            item.englishDescription,
                    color = Color(0xFF8999A3),
                    fontSize = 11.sp
                )
            }
        }
    }
}
