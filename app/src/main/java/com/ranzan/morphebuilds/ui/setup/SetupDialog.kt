package com.ranzan.morphebuilds.ui.setup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
import com.ranzan.morphebuilds.core.utils.Architecture
import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.ui.theme.AccentGreen
import com.ranzan.morphebuilds.ui.theme.AccentGreenContainer
import com.ranzan.morphebuilds.ui.theme.AccentGreenText
import com.ranzan.morphebuilds.ui.theme.DarkBorder
import com.ranzan.morphebuilds.ui.theme.DarkSurface
import com.ranzan.morphebuilds.ui.theme.DarkSurfaceElevated
import com.ranzan.morphebuilds.ui.theme.PureBlack
import com.ranzan.morphebuilds.ui.theme.TextMuted
import com.ranzan.morphebuilds.ui.theme.TextPrimary
import com.ranzan.morphebuilds.ui.theme.TextSecondary

@Composable
fun SetupDialog(
    detectedDeviceArch: Architecture,
    initialRepoUrl: String,
    onCompleteSetup: (folderUri: String?, folderPath: String?, arch: String, repoUrl: String, showApks: Boolean, showModules: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFolderUri by remember { mutableStateOf<String?>(null) }
    var selectedFolderPath by remember { mutableStateOf<String?>("Default: Downloads/MorpheBuilds") }
    var selectedArch by remember { mutableStateOf(detectedDeviceArch.id) }
    var showApks by remember { mutableStateOf(true) }
    var showModules by remember { mutableStateOf(false) }
    var repoUrlInput by remember { mutableStateOf(initialRepoUrl.ifBlank { AppSettings.DEFAULT_REPO_URL }) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                selectedFolderUri = treeUri.toString()
                selectedFolderPath = doc?.name ?: treeUri.lastPathSegment ?: "Custom Folder"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { /* Force completion on first setup */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "First-Time Setup",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure storage folder, architecture, and source repository.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Storage Folder Selection
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Storage & Cleanup Folder",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose or create a folder where builds are downloaded, allowing older versions to be automatically cleaned up after updates.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Folder status card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Selected Folder:",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedFolderPath ?: "Downloads/MorpheBuilds",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AccentGreenText,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Folder", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedFolderUri = null
                                selectedFolderPath = "Default: Downloads/MorpheBuilds"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Default", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Section 2: Architecture Selection
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Target Architecture",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Detected on device: ${detectedDeviceArch.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val archOptions = listOf(
                        "arm64-v8a",
                        "arm-v7a",
                        "x86_64",
                        "all"
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        archOptions.forEach { arch ->
                            val isSelected = selectedArch.equals(arch, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentGreenContainer else DarkSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentGreen else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedArch = arch }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = arch,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) AccentGreen else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 3: Build Formats
                Column {
                    Text(
                        text = "Build Formats to Display",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose which formats to show. If both are selected, filter tabs will appear:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // APKs Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (showApks) AccentGreenContainer else DarkSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (showApks) AccentGreen else DarkBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (!showApks || showModules) {
                                        showApks = !showApks
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "APKs",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = if (showApks) AccentGreenText else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (showApks) {
                                        Text("✓", color = AccentGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Standard Android apps (Default)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (showApks) TextSecondary else TextMuted
                                    )
                                )
                            }
                        }

                        // Magisk Modules Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (showModules) AccentGreenContainer else DarkSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (showModules) AccentGreen else DarkBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (!showModules || showApks) {
                                        showModules = !showModules
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Modules",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = if (showModules) AccentGreenText else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (showModules) {
                                        Text("✓", color = AccentGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Root / Magisk zips",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (showModules) TextSecondary else TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 4: Repository URL
                Column {
                    Text(
                        text = "Repository URL",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize GitHub releases repository URL at any time:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = repoUrlInput,
                        onValueChange = { repoUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCompleteSetup(
                        selectedFolderUri,
                        selectedFolderPath,
                        selectedArch,
                        repoUrlInput.trim().ifBlank { AppSettings.DEFAULT_REPO_URL },
                        showApks,
                        showModules
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Confirm & Start",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    )
}
