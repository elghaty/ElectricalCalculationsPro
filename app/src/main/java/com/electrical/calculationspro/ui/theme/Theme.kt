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
