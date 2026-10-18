package com.ranzan.morphebuilds.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranzan.morphebuilds.ui.components.AppBuildCard
import com.ranzan.morphebuilds.ui.components.ArchitecturePickerDialog
import com.ranzan.morphebuilds.ui.components.CleanOldBuildsDialog
import com.ranzan.morphebuilds.ui.components.CleanupBanner
import com.ranzan.morphebuilds.ui.setup.SetupDialog
import com.ranzan.morphebuilds.ui.theme.AccentGreen
import com.ranzan.morphebuilds.ui.theme.AccentGreenContainer
import com.ranzan.morphebuilds.ui.theme.AccentGreenText
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* result handled by installer */ }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is HomeEffect.LaunchInstallIntent -> {
                    try {
                        installLauncher.launch(effect.intent)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Cannot open installer: ${e.localizedMessage}")
                    }
                }
                is HomeEffect.LaunchShareIntent -> {
                    try {
                        context.startActivity(Intent.createChooser(effect.intent, "Open with"))
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Cannot open file: ${e.localizedMessage}")
                    }
                }
                is HomeEffect.NavigateToSettings -> onNavigateToSettings()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PureBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Morphe Builds",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                    }
                },
                actions = {
                    // Device Architecture pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = state.detectedDeviceArch.id,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
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
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.processIntent(HomeIntent.Refresh) },
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = state.isRefreshing,
                    containerColor = DarkSurfaceElevated,
                    color = AccentGreen,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter Tabs (Only shown if user enabled BOTH APKs and Modules)
                if (state.settings.showApks && state.settings.showModules) {
                    TabRow(
                        selectedTabIndex = state.selectedTab.ordinal,
                        containerColor = PureBlack,
                        contentColor = TextPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                                color = AccentGreen,
                                height = 3.dp
                            )
                        },
                        divider = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(DarkBorder)
                            )
                        }
                    ) {
                        Tab(
                            selected = state.selectedTab == BuildFilterTab.ALL,
                            onClick = { viewModel.processIntent(HomeIntent.SelectTab(BuildFilterTab.ALL)) },
                            text = {
                                Text(
                                    text = "All Apps",
                                    fontWeight = if (state.selectedTab == BuildFilterTab.ALL) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.selectedTab == BuildFilterTab.ALL) TextPrimary else TextSecondary
                                )
                            }
                        )
                        Tab(
                            selected = state.selectedTab == BuildFilterTab.APK_ONLY,
                            onClick = { viewModel.processIntent(HomeIntent.SelectTab(BuildFilterTab.APK_ONLY)) },
                            text = {
                                Text(
                                    text = "APKs",
                                    fontWeight = if (state.selectedTab == BuildFilterTab.APK_ONLY) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.selectedTab == BuildFilterTab.APK_ONLY) TextPrimary else TextSecondary
                                )
                            }
                        )
                        Tab(
                            selected = state.selectedTab == BuildFilterTab.MAGISK_MODULES,
                            onClick = { viewModel.processIntent(HomeIntent.SelectTab(BuildFilterTab.MAGISK_MODULES)) },
                            text = {
                                Text(
                                    text = "Magisk Modules",
                                    fontWeight = if (state.selectedTab == BuildFilterTab.MAGISK_MODULES) FontWeight.Bold else FontWeight.Normal,
                                    color = if (state.selectedTab == BuildFilterTab.MAGISK_MODULES) TextPrimary else TextSecondary
                                )
                            }
                        )
                    }
                }

                // Search Bar
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.processIntent(HomeIntent.Search(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps or versions...", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.processIntent(HomeIntent.Search("")) }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DarkBorder,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                // Updates available summary pill
                if (state.updateAvailableCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreenContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚡ ${state.updateAvailableCount} app update(s) available for your device",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

            // Cleanup Banner if old builds exist
            if (state.cleanableSpaceBytes > 0) {
                CleanupBanner(
                    oldFiles = state.oldBuildsFound,
                    onCleanClick = { viewModel.processIntent(HomeIntent.OpenCleanDialog) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Main Content Area
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PureBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentGreen)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Fetching builds from GitHub...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }
                }

                state.error != null && state.displayBuilds.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(48.dp)
                            )

                            Text(
                                text = "Failed to Load Releases",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Text(
                                text = state.error ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.processIntent(HomeIntent.Refresh) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGreen,
                                        contentColor = PureBlack
                                    )
                                ) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onNavigateToSettings,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Text("Change URL / Token")
                                }
                            }
                        }
                    }
                }

                state.displayBuilds.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) "No builds match '${state.searchQuery}'" else "No builds found.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }

                else -> {
                    val installedList = state.installedBuilds
                    val notInstalledList = state.notInstalledBuilds

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // SECTION 1: INSTALLED APPS (ON TOP)
                        if (installedList.isNotEmpty()) {
                            item(key = "header_installed") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 4.dp),
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
                                        text = "INSTALLED APPS (${installedList.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = AccentGreenText,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }

                            items(
                                items = installedList,
                                key = { it.id }
                            ) { build ->
                                val appKey = "${build.customTitle ?: build.appType.name}_${if (build.isModule) "MODULE" else "APK"}"
                                val availableArchs = state.availableArchsPerApp[appKey] ?: emptyList()

                                AppBuildCard(
                                    build = build,
                                    updateStatus = state.updateStatuses[build.id],
                                    downloadItem = state.activeDownloads[build.id],
                                    availableArchs = availableArchs,
                                    onDownloadClick = { viewModel.processIntent(HomeIntent.Download(build)) },
                                    onCancelDownload = { viewModel.processIntent(HomeIntent.CancelDownload(build.id)) },
                                    onInstallClick = { uri -> viewModel.processIntent(HomeIntent.InstallApk(uri)) },
                                    onOpenZipClick = { uri -> viewModel.processIntent(HomeIntent.OpenMagiskZip(uri)) },
                                    onArchSelectorClick = { viewModel.processIntent(HomeIntent.OpenArchPicker(build)) }
                                )
                            }
                        }

                        // SEPARATOR BETWEEN INSTALLED AND NOT INSTALLED
                        if (installedList.isNotEmpty() && notInstalledList.isNotEmpty()) {
                            item(key = "separator_installed_not_installed") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(DarkBorder)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "AVAILABLE TO INSTALL (${notInstalledList.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                        } else if (installedList.isEmpty() && notInstalledList.isNotEmpty()) {
                            item(key = "header_available_only") {
                                Text(
                                    text = "AVAILABLE TO INSTALL (${notInstalledList.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                        }

                        // SECTION 2: NOT INSTALLED APPS
                        items(
                            items = notInstalledList,
                            key = { it.id }
                        ) { build ->
                            val appKey = "${build.customTitle ?: build.appType.name}_${if (build.isModule) "MODULE" else "APK"}"
                            val availableArchs = state.availableArchsPerApp[appKey] ?: emptyList()

                            AppBuildCard(
                                build = build,
                                updateStatus = state.updateStatuses[build.id],
                                downloadItem = state.activeDownloads[build.id],
                                availableArchs = availableArchs,
                                onDownloadClick = { viewModel.processIntent(HomeIntent.Download(build)) },
                                onCancelDownload = { viewModel.processIntent(HomeIntent.CancelDownload(build.id)) },
                                onInstallClick = { uri -> viewModel.processIntent(HomeIntent.InstallApk(uri)) },
                                onOpenZipClick = { uri -> viewModel.processIntent(HomeIntent.OpenMagiskZip(uri)) },
                                onArchSelectorClick = { viewModel.processIntent(HomeIntent.OpenArchPicker(build)) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

    // First-Time Setup Dialog
    if (state.showSetupDialog) {
        SetupDialog(
            detectedDeviceArch = state.detectedDeviceArch,
            initialRepoUrl = state.settings.repoUrl,
            onCompleteSetup = { folderUri, folderPath, arch, repoUrl, showApks, showModules ->
                viewModel.processIntent(
                    HomeIntent.CompleteSetup(
                        folderUri = folderUri,
                        folderPath = folderPath,
                        arch = arch,
                        repoUrl = repoUrl,
                        showApks = showApks,
                        showModules = showModules
                    )
                )
            }
        )
    }

    // Architecture Picker Dialog (Per-App)
    state.showArchPickerForBuild?.let { build ->
        val appKey = "${build.customTitle ?: build.appType.name}_${if (build.isModule) "MODULE" else "APK"}"
        val availableArchs = state.availableArchsPerApp[appKey] ?: emptyList()
        val currentArch = state.settings.appArchOverrides[appKey]
            ?: if (state.settings.preferredArch != "auto") state.settings.preferredArch
            else state.detectedDeviceArch.id

        ArchitecturePickerDialog(
            build = build,
            availableArchs = availableArchs,
            currentArch = currentArch,
            onSelectArch = { newArch ->
                viewModel.processIntent(HomeIntent.ChangeAppArchitecture(appKey, newArch))
            },
            onDismiss = { viewModel.processIntent(HomeIntent.DismissArchPicker) }
        )
    }

    // Clean Old Builds Dialog
    if (state.showCleanDialog) {
        CleanOldBuildsDialog(
            oldFiles = state.oldBuildsFound,
            onConfirmClean = { viewModel.processIntent(HomeIntent.ConfirmCleanOldBuilds) },
            onDismiss = { viewModel.processIntent(HomeIntent.DismissCleanDialog) }
        )
    }
}
