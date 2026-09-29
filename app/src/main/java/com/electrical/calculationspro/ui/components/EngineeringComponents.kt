package com.electrical.calculationspro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun EngineeringScreenHeader(
title: String,
subtitle: String? = null,
onBack: (() -> Unit)? = null,
trailing: @Composable (() -> Unit)? = null
) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(
horizontal = 4.dp,
vertical = 4.dp
),
verticalAlignment = Alignment.CenterVertically
) {
if (onBack != null) {
IconButton(
onClick = onBack,
modifier = Modifier.size(48.dp)
) {
Icon(
imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
contentDescription = "Back"
)
}
}

    Column(
        modifier = Modifier
            .weight(1f)
            .padding(
                horizontal = 8.dp
            )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    trailing?.invoke()
}

}

@Composable
fun EngineeringInput(
value: String,
label: String,
onValueChange: (String) -> Unit,
modifier: Modifier = Modifier,
enabled: Boolean = true,
isNumeric: Boolean = true
) {
OutlinedTextField(
value = value,
onValueChange = { input ->
if (!isNumeric) {
onValueChange(input)
} else {
onValueChange(
input.filter { character ->
character.isDigit() ||
character == '.' ||
character == '-' ||
character == ','
}.replace(',', '.')
)
}
},
modifier = modifier.fillMaxWidth(),
label = {
Text(text = label)
},
singleLine = true,
enabled = enabled,
keyboardOptions = KeyboardOptions(
keyboardType =
if (isNumeric) {
KeyboardType.Decimal
} else {
KeyboardType.Text
}
),
shape = RoundedCornerShape(10.dp)
)
}

@Composable
fun EngineeringPrimaryButton(
text: String,
onClick: () -> Unit,
modifier: Modifier = Modifier,
enabled: Boolean = true,
leadingIcon: @Composable (() -> Unit)? = null
) {
Button(
onClick = onClick,
modifier = modifier.fillMaxWidth(),
enabled = enabled,
shape = RoundedCornerShape(10.dp),
contentPadding = ButtonDefaults.ContentPadding
) {
if (leadingIcon != null) {
leadingIcon()
}

    Text(
        text = text,
        modifier = Modifier.padding(
            horizontal = 4.dp
        )
    )
}

}

@Composable
fun EngineeringSecondaryButton(
text: String,
onClick: () -> Unit,
modifier: Modifier = Modifier,
enabled: Boolean = true,
leadingIcon: @Composable (() -> Unit)? = null
) {
OutlinedButton(
onClick = onClick,
modifier = modifier.fillMaxWidth(),
enabled = enabled,
shape = RoundedCornerShape(10.dp),
contentPadding = ButtonDefaults.ContentPadding
) {
if (leadingIcon != null) {
leadingIcon()
}

    Text(
        text = text,
        modifier = Modifier.padding(
            horizontal = 4.dp
        )
    )
}

}

@Composable
fun EngineeringSection(
title: String? = null,
modifier: Modifier = Modifier,
content: @Composable () -> Unit
) {
Card(
modifier = modifier.fillMaxWidth(),
shape = RoundedCornerShape(14.dp),
colors = CardDefaults.cardColors(
containerColor =
MaterialTheme.colorScheme.surface
),
elevation = CardDefaults.cardElevation(
defaultElevation = 1.dp
)
) {
Column(
modifier = Modifier
.fillMaxWidth()
.padding(16.dp),
verticalArrangement =
Arrangement.spacedBy(10.dp)
) {
if (!title.isNullOrBlank()) {
Text(
text = title,
style =
MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.Bold,
color =
MaterialTheme.colorScheme.onSurface
)
}

        content()
    }
}

}

@Composable
fun EngineeringResult(
title: String,
value: String,
success: Boolean = true
) {
val container =
if (success) {
MaterialTheme.colorScheme.primaryContainer
} else {
MaterialTheme.colorScheme.errorContainer
}

val foreground =
    if (success) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
        containerColor = container
    )
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector =
                if (success) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.ErrorOutline
                },
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = foreground
            )

            Text(
                text = value,
                style =
                    MaterialTheme.typography.bodyLarge,
                color = foreground
            )
        }
    }
}

}

@Composable
fun EngineeringValueRow(
label: String,
value: String,
modifier: Modifier = Modifier
) {
Row(
modifier = modifier.fillMaxWidth(),
horizontalArrangement =
Arrangement.SpaceBetween,
verticalAlignment =
Alignment.CenterVertically
) {
Text(
text = label,
style =
MaterialTheme.typography.bodyMedium,
color =
MaterialTheme.colorScheme.onSurfaceVariant,
modifier = Modifier.weight(1f)
)

    Text(
        text = value,
        style =
            MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color =
            MaterialTheme.colorScheme.onSurface
    )
}

}

@Composable
fun EngineeringStatus(
text: String,
success: Boolean = true,
modifier: Modifier = Modifier
) {
Card(
modifier = modifier.fillMaxWidth(),
shape = RoundedCornerShape(10.dp),
colors = CardDefaults.cardColors(
containerColor =
if (success) {
MaterialTheme.colorScheme.primaryContainer
} else {
MaterialTheme.colorScheme.errorContainer
}
)
) {
Text(
text = text,
modifier = Modifier.padding(12.dp),
style = MaterialTheme.typography.bodyMedium,
color =
if (success) {
MaterialTheme.colorScheme.onPrimaryContainer
} else {
MaterialTheme.colorScheme.onErrorContainer
}
)
}
}

@Composable
fun EngineeringScreen(
title: String,
subtitle: String? = null,
onBack: (() -> Unit)? = null,
content: @Composable () -> Unit
) {
Column(
modifier = Modifier
.fillMaxWidth(),
verticalArrangement =
Arrangement.spacedBy(14.dp)
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
fun EngineeringEmptyState(
title: String,
message: String,
modifier: Modifier = Modifier
) {
Card(
modifier = modifier.fillMaxWidth(),
shape = RoundedCornerShape(14.dp)
) {
Column(
modifier = Modifier
.fillMaxWidth()
.padding(24.dp),
horizontalAlignment =
Alignment.CenterHorizontally,
verticalArrangement =
Arrangement.spacedBy(8.dp)
) {
Text(
text = title,
style =
MaterialTheme.typography.titleMedium,
fontWeight = FontWeight.Bold
)

        Text(
            text = message,
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

}
