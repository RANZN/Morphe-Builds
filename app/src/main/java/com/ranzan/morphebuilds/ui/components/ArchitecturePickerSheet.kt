package com.ranzan.morphebuilds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.ui.theme.AccentGreen
import com.ranzan.morphebuilds.ui.theme.AccentGreenContainer
import com.ranzan.morphebuilds.ui.theme.DarkBorder
import com.ranzan.morphebuilds.ui.theme.DarkSurface
import com.ranzan.morphebuilds.ui.theme.DarkSurfaceElevated
import com.ranzan.morphebuilds.ui.theme.PureBlack
import com.ranzan.morphebuilds.ui.theme.TextMuted
import com.ranzan.morphebuilds.ui.theme.TextPrimary
import com.ranzan.morphebuilds.ui.theme.TextSecondary

@Composable
fun ArchitecturePickerDialog(
    build: AppBuild,
    availableArchs: List<String>,
    currentArch: String,
    onSelectArch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val allOptions = if (availableArchs.isEmpty()) {
        listOf("arm64-v8a", "arm-v7a", "all")
    } else {
        availableArchs
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Select Architecture",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose architecture for ${build.displayName}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allOptions.forEach { arch ->
                    val isSelected = arch.equals(currentArch, ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentGreenContainer else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSelected) AccentGreen else DarkBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectArch(arch)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = arch,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (isSelected) AccentGreen else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                            val subtitle = when (arch.lowercase()) {
                                "arm64-v8a" -> "Most modern Android phones (64-bit)"
                                "arm-v7a" -> "Older Android phones (32-bit)"
                                "all" -> "Universal architecture"
                                else -> "Compatible devices"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PureBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
