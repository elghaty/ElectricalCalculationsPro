package com.electrical.calculationspro.ui.screens

import androidx.compose.runtime.Composable
import com.electrical.calculationspro.data.AppLanguage
import com.electrical.calculationspro.ui.screens.sld.SldEditorScreen as ModularSldEditorScreen

@Composable
fun SldEditorScreen(
    language: AppLanguage,
    onBack: (() -> Unit)? = null
) {
    ModularSldEditorScreen(
        language = language,
        onBack = onBack
    )
}
