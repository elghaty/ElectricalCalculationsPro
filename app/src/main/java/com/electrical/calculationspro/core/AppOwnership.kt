package com.electrical.calculationspro.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * ================================================================
 * ELECTRICAL CALCULATIONS PRO
 * INTELLECTUAL PROPERTY / COPYRIGHT NOTICE
 * ================================================================
 *
 * Application owner:
 *
 * المهندس الاستشاري عبدالرءوف الغيطي
 *
 * © 2026
 * جميع الحقوق محفوظة.
 *
 * This file intentionally contains the ownership identity used
 * throughout the application and engineering documents.
 *
 * ================================================================
 */

object AppOwnership {

    const val OWNER_NAME =
        "المهندس الاستشاري عبدالرءوف الغيطي"

    const val COPYRIGHT_YEAR =
        "2026"

    const val COPYRIGHT_NOTICE =
        "© 2026 جميع الحقوق محفوظة للمهندس الاستشاري عبدالرءوف الغيطي"

    const val INTELLECTUAL_PROPERTY_NOTICE =
        "جميع الحقوق والملكية الفكرية محفوظة للمهندس الاستشاري عبدالرءوف الغيطي"

    const val PRODUCT_NAME =
        "Electrical Calculations Pro"

    const val PRODUCT_DESCRIPTION =
        "Professional Electrical Engineering Design & Calculation System"

    /**
     * Short legal footer suitable for engineering reports.
     */
    const val REPORT_FOOTER =
        "© 2026 Electrical Calculations Pro — جميع الحقوق محفوظة للمهندس الاستشاري عبدالرءوف الغيطي"

    /**
     * Full ownership text suitable for an About / Legal screen.
     */
    const val FULL_NOTICE =
        "Electrical Calculations Pro\n\n" +
            "Professional Electrical Engineering Design & Calculation System\n\n" +
            "© 2026\n" +
            "جميع الحقوق والملكية الفكرية محفوظة\n" +
            "للمهندس الاستشاري عبدالرءوف الغيطي"
}


/**
 * ================================================================
 * COPYRIGHT FOOTER
 * ================================================================
 *
 * Use this component at the bottom of the main application shell
 * and later in the report preview screen.
 */
@Composable
fun CopyrightFooter(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        HorizontalDivider(
            modifier =
                Modifier.fillMaxWidth()
        )

        Text(
            text =
                AppOwnership.COPYRIGHT_NOTICE,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 6.dp
                    ),

            textAlign =
                TextAlign.Center,

            style =
                MaterialTheme.typography.labelSmall,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text =
                AppOwnership.PRODUCT_NAME,

            modifier =
                Modifier.fillMaxWidth(),

            textAlign =
                TextAlign.Center,

            style =
                MaterialTheme.typography.labelSmall,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
