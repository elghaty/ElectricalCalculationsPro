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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
columns = GridCells.Adaptive(
minSize = 155.dp
),
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
                        horizontal = 12.dp,
                        vertical = 14.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
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

/*

* Standard icons used by the project.
* 
* These helpers deliberately use only icons that already exist
* in the current project dependency set.
  */

@Composable
fun ElectricalDesignIcon(
modifier: Modifier = Modifier
) {
Icon(
imageVector = Icons.Outlined.Bolt,
contentDescription = null,
modifier = modifier.size(30.dp)
)
}

@Composable
fun WaterDesignIcon(
modifier: Modifier = Modifier
) {
Icon(
imageVector = Icons.Outlined.WaterDrop,
contentDescription = null,
modifier = modifier.size(30.dp)
)
}

@Composable
fun SewageDesignIcon(
modifier: Modifier = Modifier
) {
Icon(
imageVector = Icons.Outlined.WaterDrop,
contentDescription = null,
modifier = modifier.size(30.dp)
)
}

@Composable
fun SldDesignIcon(
modifier: Modifier = Modifier
) {
Icon(
imageVector = Icons.Outlined.AccountTree,
contentDescription = null,
modifier = modifier.size(30.dp)
)
}

@Composable
fun ProjectDescriptionIcon(
modifier: Modifier = Modifier
) {
Icon(
imageVector = Icons.Outlined.Description,
contentDescription = null,
modifier = modifier.size(30.dp)
)
}
