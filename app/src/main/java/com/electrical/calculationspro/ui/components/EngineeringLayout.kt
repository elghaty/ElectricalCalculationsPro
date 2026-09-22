package com.electrical.calculationspro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PageBackground = Color(0xFFF4F7F9)
private val CardBackground = Color.White
private val PrimaryText = Color(0xFF17212B)
private val SecondaryText = Color(0xFF60717D)
private val BorderColor = Color(0xFFD5DDE2)

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

/**
 * Main four-column input grid.
 *
 * Each item occupies one quarter of the available width.
 * This is intended primarily for tablets and landscape layouts.
 */
@Composable
fun FourColumnGrid(
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 4
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .padding(end = 2.dp)
        ) {
            content()
        }
    }
}

/**
 * Four-column container where each supplied field is one grid item.
 *
 * Usage:
 *
 * FourColumnFields {
 *     item { NumberField(...) }
 *     item { NumberField(...) }
 *     item { Dropdown(...) }
 *     item { NumberField(...) }
 * }
 */
@Composable
fun FourColumnFields(
    content: @Composable FourColumnScope.() -> Unit
) {
    val scope = FourColumnScopeImpl()

    content(scope)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 4
    ) {
        scope.items.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.25f)
                    .padding(end = 2.dp)
            ) {
                item()
            }
        }
    }
}

interface FourColumnScope {
    fun item(content: @Composable () -> Unit)
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

/**
 * Standard result grid.
 */
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
