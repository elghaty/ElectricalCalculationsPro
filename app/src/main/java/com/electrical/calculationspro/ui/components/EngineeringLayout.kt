package com.electrical.calculationspro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun EngineeringPage(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EngineeringScreenHeader(
            title = title,
            subtitle = subtitle,
            onBack = onBack
        )

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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            content()
        }
    }
}

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

        val columns = 4

        val spacingX = horizontalSpacing.dp.roundToPx()
        val spacingY = verticalSpacing.dp.roundToPx()

        /*
         * Keep the measure policy valid for both bounded and unbounded
         * width constraints.
         */
        val availableWidth = constraints.maxWidth

        if (measurables.isEmpty()) {
            layout(
                width = constraints.minWidth,
                height = constraints.minHeight
            ) {
                // Nothing to place.
            }
        } else if (availableWidth == Int.MAX_VALUE) {

            /*
             * Unbounded width:
             * measure children naturally, then arrange them in four
             * columns using the largest child width.
             */
            val measured = measurables.map { measurable ->
                measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = constraints.maxWidth
                    )
                )
            }

            val naturalColumnWidth =
                measured.maxOfOrNull { it.width } ?: 0

            val columnWidth =
                max(1, naturalColumnWidth)

            val rowCount =
                (measured.size + columns - 1) / columns

            val rowHeights = IntArray(rowCount)

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
                    spacingY * max(0, rowCount - 1)

            val finalWidth =
                columnWidth * columns +
                    spacingX * (columns - 1)

            val finalHeight =
                totalHeight.coerceIn(
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
        } else {

            /*
             * Normal bounded layout:
             * divide the available width equally between four columns.
             */
            val columnWidth =
                max(
                    1,
                    (
                        availableWidth -
                            spacingX * (columns - 1)
                        ) / columns
                )

            val measured =
                measurables.map { measurable ->
                    measurable.measure(
                        constraints.copy(
                            minWidth = columnWidth,
                            maxWidth = columnWidth
                        )
                    )
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
                    spacingY * max(0, rowCount - 1)

            val finalHeight =
                totalHeight.coerceIn(
                    constraints.minHeight,
                    constraints.maxHeight
                )

            layout(
                width = availableWidth,
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

@Composable
fun FourColumnFields(
    content: @Composable FourColumnScope.() -> Unit
) {
    val scope = FourColumnScopeImpl()

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

private class FourColumnScopeImpl : FourColumnScope {

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
    EngineeringResult(
        title = title,
        value = value
    )
}

@Composable
fun EngineeringSectionTitle(
    text: String
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            top = 4.dp,
            bottom = 2.dp
        )
    )
}
