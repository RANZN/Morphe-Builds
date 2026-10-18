package com.ranzan.morphebuilds.ui.settings

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.ui.theme.AccentGreen
import com.ranzan.morphebuilds.ui.theme.AccentGreenContainer
import com.ranzan.morphebuilds.ui.theme.AccentGreenText
import com.ranzan.morphebuilds.ui.theme.AccentOrange
import com.ranzan.morphebuilds.ui.theme.AccentOrangeContainer
import com.ranzan.morphebuilds.ui.theme.AccentRed
import com.ranzan.morphebuilds.ui.theme.AccentRedContainer
import com.ranzan.morphebuilds.ui.theme.DarkBorder
import com.ranzan.morphebuilds.ui.theme.DarkSurface
import com.ranzan.morphebuilds.ui.theme.DarkSurfaceElevated
import com.ranzan.morphebuilds.ui.theme.PureBlack
import com.ranzan.morphebuilds.ui.theme.TextMuted
import com.ranzan.morphebuilds.ui.theme.TextPrimary
import com.ranzan.morphebuilds.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                viewModel.processIntent(
                    SettingsIntent.UpdateFolder(
                        uri = treeUri.toString(),
                        path = doc?.name ?: treeUri.lastPathSegment ?: "Custom Folder"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PureBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: GitHub Repository Settings
            SettingsSectionCard(title = "GitHub Repository") {
                Text(
                    text = "Source repository releases URL. You can change this to any public or private GitHub repository:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.repoUrlInput,
                    onValueChange = { viewModel.processIntent(SettingsIntent.ChangeRepoUrlInput(it)) },
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

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isMorphe = state.repoUrlInput.trim().equals(AppSettings.DEFAULT_REPO_URL.trim(), ignoreCase = true)
                    val isMicroG = state.repoUrlInput.trim().equals(AppSettings.MICROG_REPO_URL.trim(), ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMorphe) AccentGreenContainer else DarkSurfaceElevated)
                            .border(1.dp, if (isMorphe) AccentGreen else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.processIntent(SettingsIntent.ChangeRepoUrlInput(AppSettings.DEFAULT_REPO_URL))
                                viewModel.processIntent(SettingsIntent.SaveRepoUrl)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Morphe Builds",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isMorphe) AccentGreen else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMicroG) AccentGreenContainer else DarkSurfaceElevated)
                            .border(1.dp, if (isMicroG) AccentGreen else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.processIntent(SettingsIntent.ChangeRepoUrlInput(AppSettings.MICROG_REPO_URL))
                                viewModel.processIntent(SettingsIntent.SaveRepoUrl)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MicroG-RE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isMicroG) AccentGreen else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.processIntent(SettingsIntent.SaveRepoUrl) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save URL", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.processIntent(SettingsIntent.ResetRepoUrl) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset Default")
                    }

                    Button(
                        onClick = { viewModel.processIntent(SettingsIntent.TestRepoUrl) },
                        enabled = !state.isTestingRepo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isTestingRepo) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test")
                        }
                    }
                }

                // Test result feedback
                if (state.testRepoMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.isTestRepoSuccess) AccentGreenContainer else AccentRedContainer)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = state.testRepoMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (state.isTestRepoSuccess) AccentGreenText else AccentRed,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GitHub Token Field (Optional)
                Text(
                    text = "GitHub Token (Optional - to bypass 60 req/hr rate limits):",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.githubTokenInput,
                        onValueChange = { viewModel.processIntent(SettingsIntent.ChangeGithubTokenInput(it)) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("ghp_xxxxxxxxxxxx", color = TextMuted) },
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

                    Button(
                        onClick = { viewModel.processIntent(SettingsIntent.SaveGithubToken) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                }
            }

            // Section 2: Preferred Architecture
            SettingsSectionCard(title = "Default Architecture") {
                Text(
                    text = "Select default architecture for app downloads. Device native: ${state.detectedDeviceArch.displayName}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val archOptions = listOf("auto", "arm64-v8a", "arm-v7a", "x86_64", "all")

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    archOptions.forEach { arch ->
                        val isSelected = state.settings.preferredArch.equals(arch, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentGreenContainer else DarkSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) AccentGreen else DarkBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.processIntent(SettingsIntent.SelectArch(arch)) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (arch == "auto") "Auto (${state.detectedDeviceArch.id})" else arch,
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
            SettingsSectionCard(title = "Build Formats") {
                Text(
                    text = "Select which build formats to display. If both are enabled, filter tabs will appear on the main screen:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val showApks = state.settings.showApks
                    val showModules = state.settings.showModules

                    // APKs Toggle Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (showApks) AccentGreenContainer else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (showApks) AccentGreen else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.processIntent(
                                    SettingsIntent.ToggleBuildFormats(
                                        showApks = !showApks,
                                        showModules = showModules
                                    )
                                )
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                text = "Standard Android apps",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (showApks) TextSecondary else TextMuted
                                )
                            )
                        }
                    }

                    // Magisk Modules Toggle Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (showModules) AccentGreenContainer else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (showModules) AccentGreen else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.processIntent(
                                    SettingsIntent.ToggleBuildFormats(
                                        showApks = showApks,
                                        showModules = !showModules
                                    )
                                )
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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

            // Section 4: Storage & Cleanup Settings
            SettingsSectionCard(title = "Storage & Cleanup") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Download Folder:",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                    )
                    if (state.settings.downloadFolderUri != null) {
                        Text(
                            text = "✓ Permission Granted",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.settings.downloadFolderPath ?: "Downloads/MorpheBuilds",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (state.settings.downloadFolderUri != null) AccentGreenText else TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                        Text("Change Folder", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.processIntent(
                                SettingsIntent.UpdateFolder(
                                    uri = null,
                                    path = "Default: Downloads/MorpheBuilds"
                                )
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Use Default")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-clean switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Clean Old Versions",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Automatically delete previous APKs/modules when a new update finishes downloading.",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = state.settings.autoCleanOldVersions,
                        onCheckedChange = { viewModel.processIntent(SettingsIntent.ToggleAutoClean(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureBlack,
                            checkedTrackColor = AccentGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }

                if (state.settings.autoCleanOldVersions) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Auto-Cleanup Retention (WorkManager):",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val intervals = listOf(1 to "1 Day", 7 to "7 Days", 30 to "30 Days")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervals.forEach { (days, label) ->
                            val isSelected = state.settings.cleanupIntervalDays == days
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentGreenContainer else DarkSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentGreen else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.processIntent(SettingsIntent.SelectCleanupInterval(days))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) AccentGreen else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }


            // Section 4: App Information
            SettingsSectionCard(title = "About Morphe Builds") {
                Text(
                    text = "Morphe Builds v1.0",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Clean Architecture with MVI • Pure Black AMOLED Theme • Storage Access Framework Auto-Cleanup",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
