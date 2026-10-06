package com.electrical.calculationspro.ui.screens.sld

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electrical.calculationspro.data.AppLanguage

/**
 * ============================================================
 * SLD EDITOR
 * ============================================================
 *
 * Clean SLD entry screen.
 *
 * IMPORTANT:
 * - This file intentionally has no dependency on the deleted
 *   legacy SLD UI files.
 * - No engineering calculation is performed in the UI.
 * - MainActivity can safely navigate to this screen.
 * - The engineering/data layer remains outside the UI.
 *
 * Next SLD implementation will be connected to the existing
 * engineering/data layer after the clean UI baseline builds.
 */
@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {

    val arabic =
        language == AppLanguage.ARABIC

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        /*
         * ========================================================
         * HEADER
         * ========================================================
         */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (onBack != null) {

                IconButton(
                    onClick = onBack
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.ArrowBack,

                        contentDescription =
                            if (arabic) {
                                "رجوع"
                            } else {
                                "Back"
                            }
                    )
                }
            }

            Icon(
                imageVector =
                    Icons.Outlined.AccountTree,

                contentDescription = null,

                modifier = Modifier.padding(
                    horizontal = 8.dp
                )
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (arabic) {
                            "مصمم المخطط الأحادي"
                        } else {
                            "Single Line Diagram Designer"
                        },

                    style =
                        MaterialTheme.typography.titleLarge
                )

                Text(
                    text =
                        if (arabic) {
                            "التصميم الكهربائي الاحترافي"
                        } else {
                            "Professional Electrical Design"
                        },

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = {}
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Calculate,

                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                Text(
                    text =
                        if (arabic) {
                            "حساب"
                        } else {
                            "Calculate"
                        }
                )
            }
        }


        /*
         * ========================================================
         * TOOL BAR
         * ========================================================
         */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {}
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Add,

                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                Text(
                    text =
                        if (arabic) {
                            "إضافة عنصر"
                        } else {
                            "Add Component"
                        }
                )
            }


            OutlinedButton(
                onClick = {}
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.AccountTree,

                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                Text(
                    text =
                        if (arabic) {
                            "توصيل"
                        } else {
                            "Connect"
                        }
                )
            }


            OutlinedButton(
                onClick = {}
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Settings,

                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                Text(
                    text =
                        if (arabic) {
                            "إعدادات"
                        } else {
                            "Settings"
                        }
                )
            }
        }


        /*
         * ========================================================
         * STATUS
         * ========================================================
         */

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme
                            .surfaceVariant
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.AccountTree,

                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Column {

                    Text(
                        text =
                            if (arabic) {
                                "مصمم SLD جاهز"
                            } else {
                                "SLD Designer Ready"
                            },

                        style =
                            MaterialTheme.typography
                                .titleSmall
                    )

                    Text(
                        text =
                            if (arabic) {
                                "يمكن بدء إنشاء المخطط من مساحة العمل."
                            } else {
                                "The workspace is ready for a new diagram."
                            },

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }


        /*
         * ========================================================
         * CLEAN WORKSPACE
         * ========================================================
         */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),

            contentAlignment =
                Alignment.Center
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.AccountTree,

                        contentDescription = null,

                        modifier = Modifier
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        text =
                            if (arabic) {
                                "مساحة تصميم SLD"
                            } else {
                                "SLD Design Workspace"
                            },

                        style =
                            MaterialTheme.typography
                                .headlineSmall
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            if (arabic) {
                                "سيتم بناء المصمم الاحترافي هنا على طبقة البيانات والهندسة الموجودة بالمشروع، بدون وضع أي حسابات داخل واجهة المستخدم."
                            } else {
                                "The professional designer will be built here on top of the existing data and engineering layers, with no calculations inside the UI."
                            },

                        style =
                            MaterialTheme.typography.bodyMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Add,

                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text =
                                if (arabic) {
                                    "بدء تصميم جديد"
                                } else {
                                    "Start New Design"
                                }
                        )
                    }
                }
            }
        }
    }
}
