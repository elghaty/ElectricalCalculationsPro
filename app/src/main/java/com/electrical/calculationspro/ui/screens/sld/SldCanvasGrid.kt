package com.electrical.calculationspro.ui.screens.sld

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

private const val GRID_STEP = 50f

fun DrawScope.drawSldGrid() {
    val width = size.width
    val height = size.height

    var x = 0f

    while (x <= width) {
        drawLine(
            color = Color(0x182B3A42),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += GRID_STEP
    }

    var y = 0f

    while (y <= height) {
        drawLine(
            color = Color(0x182B3A42),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += GRID_STEP
    }
}
