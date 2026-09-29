package com.electrical.calculationspro.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Unified application top bar.
 *
 * [onBack] is nullable intentionally:
 * - null  -> main/root screen, no back button
 * - value -> internal screen, back button is shown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    showBack: Boolean = onBack != null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),

        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },

        navigationIcon = {
            if (showBack && onBack != null) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },

        actions = actions,

        colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor =
                    MaterialTheme.colorScheme.surface,
                titleContentColor =
                    MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor =
                    MaterialTheme.colorScheme.onSurface,
                actionIconContentColor =
                    MaterialTheme.colorScheme.onSurface
            )
    )
}


/**
 * Simple screen top bar.
 */
@Composable
fun SimpleAppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    AppTopBar(
        title = title,
        onBack = onBack,
        actions = actions
    )
}


/**
 * Engineering screen top bar.
 *
 * Back button is displayed automatically when [onBack]
 * is supplied.
 */
@Composable
fun EngineeringAppTopBar(
    title: String,
    projectName: String?,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    AppTopBar(
        title = title,
        subtitle = projectName?.takeIf { it.isNotBlank() },
        onBack = onBack,
        actions = actions
    )
}
