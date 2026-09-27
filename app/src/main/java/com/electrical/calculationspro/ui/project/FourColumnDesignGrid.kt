package com.electrical.calculationspro.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.LocalFireDepartment
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
columns = GridCells.Adaptive(minSize = 155.dp),
modifier = modifier.fillMaxWidth(),
contentPadding = PaddingValues(4.dp),
horizontalArrangement = Arrangement.spacedBy(12.dp),
verticalArrangement = Arrangement.spacedBy(12.dp)
) {
items(
items = items,
key = { it.id }
) { item ->

        Card(
            onClick = item.onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp)
                    .padding(
                        horizontal = 14.dp,
                        vertical = 14.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    item.icon()
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )

                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
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
