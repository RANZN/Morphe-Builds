package com.ranzan.morphebuilds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ranzan.morphebuilds.core.utils.FormatHelper
import com.ranzan.morphebuilds.domain.model.CleanableFile
import com.ranzan.morphebuilds.ui.theme.AccentOrange
import com.ranzan.morphebuilds.ui.theme.AccentOrangeContainer
import com.ranzan.morphebuilds.ui.theme.AccentOrangeText
import com.ranzan.morphebuilds.ui.theme.DarkBorder
import com.ranzan.morphebuilds.ui.theme.DarkSurface
import com.ranzan.morphebuilds.ui.theme.PureBlack
import com.ranzan.morphebuilds.ui.theme.TextMuted
import com.ranzan.morphebuilds.ui.theme.TextPrimary
import com.ranzan.morphebuilds.ui.theme.TextSecondary

@Composable
fun CleanupBanner(
    oldFiles: List<CleanableFile>,
    onCleanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val oldOnly = oldFiles.filter { it.isOldVersion }
    if (oldOnly.isEmpty()) return

    val totalBytes = oldOnly.sumOf { it.sizeBytes }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, AccentOrange.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentOrangeContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${oldOnly.size} Older Builds Found",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Free up ${FormatHelper.formatBytes(totalBytes)} of storage",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Button(
                onClick = onCleanClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Clean",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
