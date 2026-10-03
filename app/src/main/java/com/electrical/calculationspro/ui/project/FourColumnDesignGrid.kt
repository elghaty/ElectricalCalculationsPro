package com.electrical.calculationspro.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


data class DesignGridItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)


/**
 * ============================================================
 * THREE COLUMN DESIGN GRID
 * ============================================================
 *
 * Professional calculator layout:
 *
 *  1   2   3
 *  4   5   6
 *  7   8   9
 * 10  11  12
 *
 * Exactly three items per row.
 */
@Composable
fun ThreeColumnDesignGrid(
    modifier: Modifier = Modifier,
    items: List<DesignGridItem>
) {

    val rows =
        items.chunked(3)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                ),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        rows.forEach { rowItems ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),

                verticalAlignment =
                    Alignment.Top
            ) {

                rowItems.forEach { item ->

                    DesignGridCard(
                        item = item,

                        modifier =
                            Modifier.weight(1f)
                    )
                }

                /*
                 * Preserve equal column widths
                 * when the last row has fewer than 3 items.
                 */
                repeat(
                    3 - rowItems.size
                ) {

                    Spacer(
                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


/**
 * ============================================================
 * FOUR COLUMN DESIGN GRID
 * ============================================================
 *
 * Retained for compatibility with any existing screen.
 *
 * Do not use this for the main calculator screen.
 * MainActivity now uses ThreeColumnDesignGrid.
 */
@Composable
fun FourColumnDesignGrid(
    modifier: Modifier = Modifier,
    items: List<DesignGridItem>
) {

    val rows =
        items.chunked(4)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                ),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        rows.forEach { rowItems ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp),

                verticalAlignment =
                    Alignment.Top
            ) {

                rowItems.forEach { item ->

                    DesignGridCard(
                        item = item,

                        modifier =
                            Modifier.weight(1f)
                    )
                }

                repeat(
                    4 - rowItems.size
                ) {

                    Spacer(
                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


/**
 * ============================================================
 * DESIGN GRID CARD
 * ============================================================
 */
@Composable
private fun DesignGridCard(
    item: DesignGridItem,
    modifier: Modifier = Modifier
) {

    Card(
        modifier =
            modifier,

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Button(
                onClick =
                    item.onClick,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                item.icon()
            }


            Spacer(
                modifier =
                    Modifier.size(7.dp)
            )


            Text(
                text =
                    item.title,

                style =
                    MaterialTheme.typography.titleSmall,

                maxLines = 2
            )


            Spacer(
                modifier =
                    Modifier.size(2.dp)
            )


            Text(
                text =
                    item.subtitle,

                style =
                    MaterialTheme.typography.bodySmall,

                maxLines = 2
            )
        }
    }
}


/**
 * ============================================================
 * COMMON ICON
 * ============================================================
 */
@Composable
fun AppIcon(
    imageVector: ImageVector
) {

    Icon(
        imageVector =
            imageVector,

        contentDescription =
            null,

        modifier =
            Modifier.size(30.dp)
    )
}
