package com.ranzan.morphebuilds.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranzan.morphebuilds.core.utils.FormatHelper
import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.AppType
import com.ranzan.morphebuilds.domain.model.AppUpdateStatus
import com.ranzan.morphebuilds.domain.model.DownloadItem
import com.ranzan.morphebuilds.domain.model.DownloadState
import com.ranzan.morphebuilds.ui.theme.AccentBlue
import com.ranzan.morphebuilds.ui.theme.AccentGreen
import com.ranzan.morphebuilds.ui.theme.AccentGreenContainer
import com.ranzan.morphebuilds.ui.theme.AccentGreenText
import com.ranzan.morphebuilds.ui.theme.AccentOrange
import com.ranzan.morphebuilds.ui.theme.AccentOrangeContainer
import com.ranzan.morphebuilds.ui.theme.AccentOrangeText
import com.ranzan.morphebuilds.ui.theme.DarkBorder
import com.ranzan.morphebuilds.ui.theme.DarkSurface
import com.ranzan.morphebuilds.ui.theme.DarkSurfaceElevated
import com.ranzan.morphebuilds.ui.theme.PureBlack
import com.ranzan.morphebuilds.ui.theme.TextMuted
import com.ranzan.morphebuilds.ui.theme.TextPrimary
import com.ranzan.morphebuilds.ui.theme.TextSecondary

@Composable
fun AppBuildCard(
    build: AppBuild,
    updateStatus: AppUpdateStatus?,
    downloadItem: DownloadItem?,
    availableArchs: List<String>,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstallClick: (String) -> Unit,
    onOpenZipClick: (String) -> Unit,
    onArchSelectorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloading = downloadItem?.isRunning == true

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        color = DarkSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: App Icon, Name, Architecture Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconBadge(appType = build.appType, isModule = build.isModule)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = build.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        FormatTypeBadge(isModule = build.isModule)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Version: ${build.version}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                // Architecture selector chip
                ArchitectureChip(
                    currentArch = build.architecture,
                    hasMultipleArchs = availableArchs.size > 1,
                    onClick = onArchSelectorClick
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Update Status Badge Row
            UpdateStatusRow(updateStatus = updateStatus)

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata row: file name, date, size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FormatHelper.formatDate(build.publishedAt),
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )

                Text(
                    text = FormatHelper.formatBytes(build.sizeBytes),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download Progress / Action Buttons
            if (downloadItem != null && downloadItem.isRunning) {
                DownloadProgressSection(
                    item = downloadItem,
                    onCancel = onCancelDownload
                )
            } else {
                ActionButtonsSection(
                    build = build,
                    updateStatus = updateStatus,
                    downloadItem = downloadItem,
                    onDownloadClick = onDownloadClick,
                    onInstallClick = onInstallClick,
                    onOpenZipClick = onOpenZipClick
                )
            }
        }
    }
}

@Composable
private fun AppIconBadge(appType: AppType, isModule: Boolean) {
    val (icon, bg, fg) = when {
        isModule -> Triple(Icons.Default.Extension, AccentOrangeContainer, AccentOrange)
        appType == AppType.YOUTUBE -> Triple(Icons.Default.PlayArrow, Color(0xFF330B0B), Color(0xFFFF4444))
        appType == AppType.YOUTUBE_MUSIC -> Triple(Icons.Default.MusicNote, Color(0xFF330B1C), Color(0xFFFF4081))
        appType == AppType.REDDIT -> Triple(Icons.Default.Android, Color(0xFF33160B), Color(0xFFFF5722))
        appType == AppType.TWITTER -> Triple(Icons.Default.Android, Color(0xFF0D1B2A), Color(0xFF1DA1F2))
        else -> Triple(Icons.Default.Android, DarkSurfaceElevated, TextPrimary)
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun FormatTypeBadge(isModule: Boolean) {
    val text = if (isModule) "MAGISK" else "APK"
    val bg = if (isModule) AccentOrangeContainer else Color(0xFF0F1E2E)
    val textColor = if (isModule) AccentOrangeText else Color(0xFF64B5F6)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

@Composable
private fun ArchitectureChip(
    currentArch: String,
    hasMultipleArchs: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = hasMultipleArchs, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentArch,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (hasMultipleArchs) AccentGreenText else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            if (hasMultipleArchs) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Architecture",
                    tint = AccentGreenText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UpdateStatusRow(updateStatus: AppUpdateStatus?) {
    when (updateStatus) {
        is AppUpdateStatus.UpdateAvailable -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentGreenContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Update Available: installed ${updateStatus.installedVersion ?: "v?"} → latest ${updateStatus.latestVersion}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentGreenText,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        is AppUpdateStatus.UpToDate -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✓ Up to date (${updateStatus.currentVersion})",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
        is AppUpdateStatus.DownloadedOnly -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1E2E))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloaded file in folder (v${updateStatus.downloadedVersion})",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF90CAF9))
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun DownloadProgressSection(
    item: DownloadItem,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.progressPercent}% • ${FormatHelper.formatSpeed(item.speedBytesPerSec)}",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "${FormatHelper.formatBytes(item.downloadedBytes)} / ${FormatHelper.formatBytes(item.totalBytes)}",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
            )

            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AccentGreen,
            trackColor = DarkBorder
        )
    }
}

@Composable
private fun ActionButtonsSection(
    build: AppBuild,
    updateStatus: AppUpdateStatus?,
    downloadItem: DownloadItem?,
    onDownloadClick: () -> Unit,
    onInstallClick: (String) -> Unit,
    onOpenZipClick: (String) -> Unit
) {
    val downloadedUri = (updateStatus as? AppUpdateStatus.DownloadedOnly)?.fileUri
        ?: (updateStatus as? AppUpdateStatus.UpdateAvailable)?.downloadedVersion?.let { downloadItem?.destinationUri }
        ?: downloadItem?.destinationUri

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (downloadedUri != null) {
            // Install / Open button
            Button(
                onClick = {
                    if (build.isModule) {
                        onOpenZipClick(downloadedUri)
                    } else {
                        onInstallClick(downloadedUri)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (build.isModule) Icons.Default.Share else Icons.Default.InstallMobile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (build.isModule) "Flash / Share" else "Install APK",
                    fontWeight = FontWeight.Bold
                )
            }

            // Re-download option
            OutlinedButton(
                onClick = onDownloadClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Re-download",
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            // Primary Download Button
            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (updateStatus is AppUpdateStatus.UpdateAvailable) AccentGreen else DarkSurfaceElevated,
                    contentColor = if (updateStatus is AppUpdateStatus.UpdateAvailable) PureBlack else TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val label = if (updateStatus is AppUpdateStatus.UpdateAvailable) {
                    "Download Update (${FormatHelper.formatBytes(build.sizeBytes)})"
                } else {
                    "Download (${FormatHelper.formatBytes(build.sizeBytes)})"
                }
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
