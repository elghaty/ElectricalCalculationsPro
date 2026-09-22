package com.electrical.calculationspro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

private val PageBackground = Color(0xFFF4F7F9)
private val CardBackground = Color.White
private val PrimaryText = Color(0xFF17212B)
private val SecondaryText = Color(0xFF60717D)

@Composable
fun EngineeringPage(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        subtitle?.let {
            Text(
                text = it,
                color = SecondaryText,
                fontSize = 13.sp
            )
        }

        content()
    }
}

@Composable
fun EngineeringCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    color = PrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            content()
        }
    }
}

/*
 * Real four-column grid.
 *
 * No FlowRow.
 * No experimental API.
 * No weight modifier outside RowScope.
 *
 * Every direct child becomes one grid cell.
 */
@Composable
fun FourColumnGrid(
    modifier: Modifier = Modifier,
    horizontalSpacing: Int = 10,
    verticalSpacing: Int = 10,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = content
    ) { measurables, constraints ->

        if (measurables.isEmpty()) {
            layout(
                width = constraints.minWidth,
                height = constraints.minHeight
            ) {}
        } else {

            val columns = 4

            val spacingX =
                horizontalSpacing.dp.roundToPx()

            val spacingY =
                verticalSpacing.dp.roundToPx()

            val availableWidth =
                constraints.maxWidth

            val columnWidth =
                if (availableWidth == Int.MAX_VALUE) {
                    0
                } else {
                    max(
                        0,
                        (
                            availableWidth -
                                spacingX * (columns - 1)
                            ) / columns
                    )
                }

            val measured =
                measurables.map { measurable ->

                    if (columnWidth > 0) {
                        measurable.measure(
                            constraints.copy(
                                minWidth = columnWidth,
                                maxWidth = columnWidth
                            )
                        )
                    } else {
                        measurable.measure(
                            constraints
                        )
                    }
                }

            val rowCount =
                (measured.size + columns - 1) / columns

            val rowHeights =
                IntArray(rowCount)

            measured.forEachIndexed { index, placeable ->
                val row = index / columns
                rowHeights[row] =
                    max(
                        rowHeights[row],
                        placeable.height
                    )
            }

            val totalHeight =
                rowHeights.sum() +
                    spacingY * (rowCount - 1)

            val finalWidth =
                if (constraints.maxWidth != Int.MAX_VALUE) {
                    constraints.maxWidth
                } else {
                    columnWidth * columns +
                        spacingX * (columns - 1)
                }

            val finalHeight =
                totalHeight
                    .coerceIn(
                        constraints.minHeight,
                        constraints.maxHeight
                    )

            layout(
                width = finalWidth,
                height = finalHeight
            ) {

                var y = 0

                rowHeights.forEachIndexed { row, rowHeight ->

                    for (column in 0 until columns) {

                        val index =
                            row * columns + column

                        if (index >= measured.size) {
                            continue
                        }

                        val placeable =
                            measured[index]

                        val x =
                            column *
                                (columnWidth + spacingX)

                        placeable.placeRelative(
                            x = x,
                            y = y
                        )
                    }

                    y += rowHeight + spacingY
                }
            }
        }
    }
}

/*
 * Compatibility helper for existing screens.
 *
 * Existing calls such as:
 *
 * FourColumnFields {
 *     item { ... }
 *     item { ... }
 *     item { ... }
 *     item { ... }
 * }
 *
 * continue to work.
 */
@Composable
fun FourColumnFields(
    content: @Composable FourColumnScope.() -> Unit
) {
    val scope =
        FourColumnScopeImpl()

    content(scope)

    FourColumnGrid {
        scope.items.forEach { item ->
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                item()
            }
        }
    }
}

interface FourColumnScope {
    fun item(
        content: @Composable () -> Unit
    )
}

private class FourColumnScopeImpl :
    FourColumnScope {

    val items =
        mutableListOf<@Composable () -> Unit>()

    override fun item(
        content: @Composable () -> Unit
    ) {
        items += content
    }
}

@Composable
fun FourColumnResults(
    content: @Composable FourColumnScope.() -> Unit
) {
    FourColumnFields(content)
}

@Composable
fun EngineeringResultCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFB)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = SecondaryText,
                fontSize = 12.sp
            )

            Text(
                text = value,
                color = PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EngineeringSectionTitle(
    text: String
) {
    Text(
        text = text,
        color = PrimaryText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            top = 4.dp,
            bottom = 2.dp
        )
    )
}
