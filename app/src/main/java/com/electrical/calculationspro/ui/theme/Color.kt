package com.electrical.calculationspro.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * The old names are intentionally preserved for compatibility
 * with existing screens.
 *
 * They are now LIGHT colors.
 */

val DarkBackground =
    Color(0xFFF4F7F9)

val DarkSurface =
    Color(0xFFFFFFFF)

val DarkSurfaceVariant =
    Color(0xFFEEF3F6)

val PrimaryTeal =
    Color(0xFF007C91)

val PrimaryTealDark =
    Color(0xFF005F70)

val TextPrimary =
    Color(0xFF17212B)

val TextSecondary =
    Color(0xFF60717D)

val AccentGreen =
    Color(0xFF2E7D32)

val AccentRed =
    Color(0xFFC62828)

val AccentOrange =
    Color(0xFFEF6C00)

val AccentPurple =
    Color(0xFF6A1B9A)

val AccentYellow =
    Color(0xFFF9A825)

val SidebarSelected =
    Color(0xFFDCEFF2)

val DividerColor =
    Color(0xFFD5DDE2)

// ============================================================
// FILE:
// app/src/main/java/com/electrical/calculationspro/ui/theme/Theme.kt
// ============================================================

package com.electrical.calculationspro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryTeal,
        onPrimary = Color.White,

        primaryContainer =
            Color(0xFFD7F2F6),

        onPrimaryContainer =
            Color(0xFF00363D),

        secondary =
            PrimaryTealDark,

        onSecondary =
            Color.White,

        secondaryContainer =
            Color(0xFFD6EDF1),

        onSecondaryContainer =
            Color(0xFF102F35),

        background =
            DarkBackground,

        onBackground =
            TextPrimary,

        surface =
            DarkSurface,

        onSurface =
            TextPrimary,

        surfaceVariant =
            DarkSurfaceVariant,

        onSurfaceVariant =
            TextSecondary,

        outline =
            DividerColor,

        error =
            AccentRed,

        onError =
            Color.White,

        errorContainer =
            Color(0xFFFFDAD6),

        onErrorContainer =
            Color(0xFF410002)
    )

@Composable
fun ElectricalCalculationsProTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    /*
     * The application is intentionally LIGHT.
     *
     * darkTheme is kept in the function signature so existing
     * calls from MainActivity do not break.
     */
    MaterialTheme(
        colorScheme =
            LightColorScheme,
        typography =
            Typography(),
        content =
            content
    )
}
