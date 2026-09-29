package com.electrical.calculationspro.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class DesignGridItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)

@Composable
fun FourColumnDesignGrid(
    items: List<DesignGridItem>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = 4.dp,
            vertical = 6.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = items,
            key = { item -> item.id }
        ) { item ->

            DesignEngineeringCard(
                item = item
            )
        }
    }
}

@Composable
private fun DesignEngineeringCard(
    item: DesignGridItem
) {
    Card(
        onClick = item.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                item.icon()
            }

            Text(
                text = item.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EngineeringIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(30.dp)
    )
}

@Composable
fun ElectricalDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.ElectricalServices,
        modifier = modifier
    )
}

@Composable
fun LoadDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Speed,
        modifier = modifier
    )
}

@Composable
fun CableDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Tune,
        modifier = modifier
    )
}

@Composable
fun BreakerProtectionIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Power,
        modifier = modifier
    )
}

@Composable
fun TransformerDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Memory,
        modifier = modifier
    )
}

@Composable
fun GeneratorDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Factory,
        modifier = modifier
    )
}

@Composable
fun MotorDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Settings,
        modifier = modifier
    )
}

@Composable
fun PumpDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Waves,
        modifier = modifier
    )
}

@Composable
fun WaterDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.WaterDrop,
        modifier = modifier
    )
}

@Composable
fun SewageDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.WaterDrop,
        modifier = modifier
    )
}

@Composable
fun SldDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.AccountTree,
        modifier = modifier
    )
}

@Composable
fun NetworkDesignIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.GridView,
        modifier = modifier
    )
}

@Composable
fun ShortCircuitIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Bolt,
        modifier = modifier
    )
}

@Composable
fun ReportIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Description,
        modifier = modifier
    )
}

@Composable
fun ProjectDescriptionIcon(
    modifier: Modifier = Modifier
) {
    EngineeringIcon(
        imageVector = Icons.Outlined.Description,
        modifier = modifier
    )
}
