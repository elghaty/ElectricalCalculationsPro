package com.electrical.calculationspro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.data.Strings
import com.electrical.calculationspro.ui.theme.DarkBackground
import com.electrical.calculationspro.ui.theme.PrimaryTeal
import com.electrical.calculationspro.ui.theme.TextPrimary
import com.electrical.calculationspro.ui.theme.TextSecondary

@Composable
fun AboutScreen(
    language: AppLanguage,
    onClose: () -> Unit
) {
    val arabic = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = Icons.Default.ElectricBolt,
            contentDescription = null,
            tint = PrimaryTeal,
            modifier = Modifier.height(72.dp)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = Strings.get(
                "app_name",
                language
            ),
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                if (arabic) {
                    "الإصدار 1.0"
                } else {
                    "Version 1.0"
                },
            color = PrimaryTeal,
            fontSize = 14.sp
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        AboutCard(
            title =
                if (arabic) {
                    "المصمم والمطور"
                } else {
                    "Designed and Developed by"
                },
            icon = Icons.Default.Verified
        ) {

            Text(
                text =
                    if (arabic) {
                        "المهندس الاستشاري عبدالرءوف حامد الغيطى"
                    } else {
                        "Consultant Engineer Abdelraouf Hamed Elghaty"
                    },
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        AboutCard(
            title =
                if (arabic) {
                    "حقوق الملكية الفكرية"
                } else {
                    "Intellectual Property"
                },
            icon = Icons.Default.Security
        ) {

            Text(
                text =
                    if (arabic) {
                        "حقوق النشر © 2026 عبدالرءوف حامد الغيطى\n\nجميع الحقوق محفوظة."
                    } else {
                        "Copyright © 2026 Abdelraouf Hamed Elghaty\n\nAll Rights Reserved."
                    },
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text =
                    if (arabic) {
                        "تصميم البرنامج الأصلي، والشفرة المصدرية، ومنطق الحسابات، وترتيب البيانات الهندسية، والمحتوى والوثائق الأصلية هي ملكية فكرية محمية لصاحب الحقوق، باستثناء مكونات وبرمجيات الطرف الثالث الخاضعة لتراخيصها الخاصة."
                    } else {
                        "The original application design, source code, calculation logic, engineering data arrangement, original content and documentation are protected intellectual property of the rights holder, except for third-party components that remain subject to their respective licenses."
                    },
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        AboutCard(
            title =
                if (arabic) {
                    "إشعار الاستخدام"
                } else {
                    "Usage Notice"
                },
            icon = Icons.Default.Security
        ) {

            Text(
                text =
                    if (arabic) {
                        "يُحظر نسخ أو إعادة إنتاج أو تعديل أو نشر أو إعادة توزيع أو بيع أو ترخيص أو استخدام أي جزء أصلي من البرنامج تجارياً أو إجراء هندسة عكسية عليه دون إذن كتابي مسبق من صاحب الحقوق."
                    } else {
                        "Copying, reproducing, modifying, publishing, redistributing, selling, sublicensing, commercial use or reverse engineering of any original part of the application is prohibited without prior written permission from the rights holder."
                    },
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        AboutCard(
            title =
                if (arabic) {
                    "إخلاء المسؤولية الهندسية"
                } else {
                    "Engineering Disclaimer"
                },
            icon = Icons.Default.Security
        ) {

            Text(
                text =
                    if (arabic) {
                        "البرنامج أداة مساعدة في الحسابات والتصميمات الهندسية. يجب مراجعة النتائج والتحقق منها بصورة مستقلة بواسطة مهندس مؤهل وفقاً للكود والمعايير السارية ومواصفات المشروع وبيانات الشركات المصنعة والمتطلبات الهندسية قبل استخدامها في التنفيذ أو الشراء أو الاعتماد أو قرارات التصميم النهائي."
                    } else {
                        "This application is an engineering calculation and design aid. Results must be independently reviewed and verified by a qualified engineer against applicable current codes, standards, project specifications, manufacturer data and professional engineering requirements before use in construction, procurement, approval or final design decisions."
                    },
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        AboutCard(
            title =
                if (arabic) {
                    "المعايير والمرجعيات"
                } else {
                    "Standards & References"
                },
            icon = Icons.Default.Verified
        ) {

            Text(
                text =
                    if (arabic) {
                        "IEC 60364-5-52\nالكود المصري – مبني على أساس IEC\nCEI\nNEC\nCEC\n\nتظل كل المواصفات والمعايير والبيانات التابعة للغير خاضعة لحقوق وتراخيص أصحابها."
                    } else {
                        "IEC 60364-5-52\nEgyptian Code – IEC based\nCEI\nNEC\nCEC\n\nThird-party standards, specifications and data remain subject to the rights and licenses of their respective owners."
                    },
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {

            TextButton(
                onClick = onClose
            ) {

                Text(
                    text = Strings.get(
                        "close",
                        language
                    ),
                    color = PrimaryTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )
    }
}

@Composable
private fun AboutCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF202D3D)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryTeal
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = title,
                    color = PrimaryTeal,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            content()
        }
    }
}
