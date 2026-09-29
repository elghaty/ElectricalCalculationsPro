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
 * Rules:
 * - Back button is always visible when [showBack] is true.
 * - Title is the current engineering workspace/screen.
 * - Subtitle is normally the active project.
 * - Actions are placed on the trailing side.
 * - RTL/LTR is handled by Compose automatically.
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
            if (showBack) {
                IconButton(
                    onClick = {
                        onBack?.invoke()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Simple screen title bar.
 *
 * Use this for screens that do not need a project subtitle.
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
 * Engineering screen title bar.
 *
 * This is the preferred version for calculation/design screens.
 */
@Composable
fun EngineeringAppTopBar(
    title: String,
    projectName: String?,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    AppTopBar(
        title = title,
        subtitle = projectName?.takeIf { it.isNotBlank() },
        onBack = onBack,
        actions = actions
    )
}
