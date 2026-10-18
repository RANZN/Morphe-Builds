package com.ranzan.morphebuilds.ui.home

import androidx.lifecycle.viewModelScope
import com.ranzan.morphebuilds.core.mvi.BaseViewModel
import com.ranzan.morphebuilds.core.utils.Architecture
import com.ranzan.morphebuilds.core.utils.FormatHelper
import com.ranzan.morphebuilds.data.local.StorageManager
import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.domain.model.DownloadState
import com.ranzan.morphebuilds.domain.model.Release
import com.ranzan.morphebuilds.domain.usecase.CheckUpdatesUseCase
import com.ranzan.morphebuilds.domain.usecase.CleanOldBuildsUseCase
import com.ranzan.morphebuilds.domain.usecase.DownloadBuildUseCase
import com.ranzan.morphebuilds.domain.usecase.GetReleasesUseCase
import com.ranzan.morphebuilds.domain.usecase.ManageSettingsUseCase
import com.ranzan.morphebuilds.domain.usecase.ScanOldBuildsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getReleasesUseCase: GetReleasesUseCase,
    private val checkUpdatesUseCase: CheckUpdatesUseCase,
    private val downloadBuildUseCase: DownloadBuildUseCase,
    private val scanOldBuildsUseCase: ScanOldBuildsUseCase,
    private val cleanOldBuildsUseCase: CleanOldBuildsUseCase,
    private val manageSettingsUseCase: ManageSettingsUseCase,
    private val storageManager: StorageManager
) : BaseViewModel<HomeState, HomeIntent, HomeEffect>(
    initialState = HomeState(detectedDeviceArch = Architecture.detectDeviceArch())
) {

    private var activeDownloadJob: Job? = null

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            manageSettingsUseCase.getSettings().collectLatest { settings ->
                setState {
                    copy(
                        settings = settings,
                        showSetupDialog = !settings.isSetupCompleted
                    )
                }
                loadReleasesAndFiles(settings)
            }
        }
    }

    override fun processIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> loadReleasesAndFiles(currentState.settings)
            is HomeIntent.Refresh -> refreshData()
            is HomeIntent.SelectTab -> {
                setState { copy(selectedTab = intent.tab) }
                updateDisplayBuilds()
            }
            is HomeIntent.SelectRepository -> {
                viewModelScope.launch {
                    manageSettingsUseCase.updateRepoUrl(intent.url)
                }
            }
            is HomeIntent.Search -> {
                setState { copy(searchQuery = intent.query) }
                updateDisplayBuilds()
            }
            is HomeIntent.Download -> handleDownload(intent.build)
            is HomeIntent.CancelDownload -> handleCancelDownload(intent.assetId)
            is HomeIntent.InstallApk -> handleInstallApk(intent.fileUri)
            is HomeIntent.OpenMagiskZip -> handleOpenMagiskZip(intent.fileUri)
            is HomeIntent.ChangeAppArchitecture -> handleArchChange(intent.appKey, intent.arch)
            is HomeIntent.OpenCleanDialog -> setState { copy(showCleanDialog = true) }
            is HomeIntent.DismissCleanDialog -> setState { copy(showCleanDialog = false) }
            is HomeIntent.ConfirmCleanOldBuilds -> handleCleanOldBuilds()
            is HomeIntent.OpenArchPicker -> setState { copy(showArchPickerForBuild = intent.build) }
            is HomeIntent.DismissArchPicker -> setState { copy(showArchPickerForBuild = null) }
            is HomeIntent.CompleteSetup -> handleCompleteSetup(intent)
            is HomeIntent.DismissError -> setState { copy(error = null) }
        }
    }

    private fun refreshData() {
        setState { copy(isRefreshing = true, error = null) }
        loadReleasesAndFiles(currentState.settings)
    }

    private fun loadReleasesAndFiles(settings: AppSettings) {
        viewModelScope.launch {
            setState { copy(isLoading = releases.isEmpty() && !isRefreshing) }

            // Scan files
            val files = scanOldBuildsUseCase(settings.downloadFolderUri)

            // Fetch releases
            getReleasesUseCase(settings).collectLatest { result ->
                result.fold(
                    onSuccess = { releases ->
                        val archsPerApp = extractAvailableArchs(releases)
                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                releases = releases,
                                availableArchsPerApp = archsPerApp,
                                oldBuildsFound = files,
                                error = null
                            )
                        }
                        updateDisplayBuilds()
                    },
                    onFailure = { error ->
                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = error.localizedMessage ?: "Failed to fetch releases",
                                oldBuildsFound = files
                            )
                        }
                    }
                )
            }
        }
    }

    private fun extractAvailableArchs(releases: List<Release>): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableSet<String>>()
        releases.forEach { release ->
            release.builds.forEach { build ->
                val appKey = getAppKey(build)
                val set = map.getOrPut(appKey) { mutableSetOf() }
                set.add(build.architecture)
            }
        }
        return map.mapValues { it.value.toList().sorted() }
    }

    private fun getAppKey(build: AppBuild): String {
        val identifier = build.customTitle ?: build.appType.name
        return "${identifier}_${if (build.isModule) "MODULE" else "APK"}"
    }

    private fun updateDisplayBuilds() {
        val state = currentState
        val allReleases = state.releases
        if (allReleases.isEmpty()) return

        // Flatten all builds across recent releases
        val allBuilds = allReleases.flatMap { it.builds }

        // Group by app variant and format
        val grouped = allBuilds.groupBy { getAppKey(it) }

        val resolvedBuilds = mutableListOf<AppBuild>()

        for ((appKey, builds) in grouped) {
            // Determine chosen architecture for this app
            val chosenArch = state.settings.appArchOverrides[appKey]
                ?: if (state.settings.preferredArch != "auto") state.settings.preferredArch
                else state.detectedDeviceArch.id

            // Find matching builds for this architecture
            val matching = builds.filter { Architecture.isMatch(chosenArch, it.architecture) }
            val chosenBuild = matching.firstOrNull() ?: builds.firstOrNull()

            if (chosenBuild != null) {
                resolvedBuilds.add(chosenBuild)
            }
        }

        // Apply Format and Tab Filter:
        // If user selected both APKs and modules, respect selectedTab.
        // If only one is selected, filter strictly to that format.
        val formatFiltered = when {
            state.settings.showApks && state.settings.showModules -> {
                when (state.selectedTab) {
                    BuildFilterTab.ALL -> resolvedBuilds
                    BuildFilterTab.APK_ONLY -> resolvedBuilds.filter { it.isApk }
                    BuildFilterTab.MAGISK_MODULES -> resolvedBuilds.filter { it.isModule }
                }
            }
            state.settings.showApks -> resolvedBuilds.filter { it.isApk }
            state.settings.showModules -> resolvedBuilds.filter { it.isModule }
            else -> resolvedBuilds.filter { it.isApk }
        }

        // Apply Search Filter
        val searchFiltered = if (state.searchQuery.isBlank()) {
            formatFiltered
        } else {
            val q = state.searchQuery.lowercase().trim()
            formatFiltered.filter {
                it.displayName.lowercase().contains(q) ||
                        it.fileName.lowercase().contains(q) ||
                        it.version.lowercase().contains(q)
            }
        }

        // Calculate update statuses
        val statuses = searchFiltered.associate { build ->
            build.id to checkUpdatesUseCase(build, state.oldBuildsFound)
        }

        setState {
            copy(
                displayBuilds = searchFiltered,
                updateStatuses = statuses
            )
        }
    }

    private fun handleDownload(build: AppBuild) {
        val folderUri = currentState.settings.downloadFolderUri
        val autoClean = currentState.settings.autoCleanOldVersions

        activeDownloadJob?.cancel()
        activeDownloadJob = viewModelScope.launch {
            downloadBuildUseCase(build, folderUri, autoClean).collect { item ->
                val downloads = currentState.activeDownloads.toMutableMap()
                downloads[build.id] = item
                setState { copy(activeDownloads = downloads) }

                if (item.state == DownloadState.COMPLETED) {
                    sendEffect(HomeEffect.ShowSnackbar("Downloaded ${build.fileName} successfully"))
                    // Rescan folder
                    val files = scanOldBuildsUseCase(folderUri)
                    setState { copy(oldBuildsFound = files) }
                    updateDisplayBuilds()
                } else if (item.state == DownloadState.FAILED) {
                    sendEffect(HomeEffect.ShowSnackbar("Download failed: ${item.errorMessage}"))
                }
            }
        }
    }

    private fun handleCancelDownload(assetId: String) {
        activeDownloadJob?.cancel()
        val downloads = currentState.activeDownloads.toMutableMap()
        downloads.remove(assetId)
        setState { copy(activeDownloads = downloads) }
        sendEffect(HomeEffect.ShowSnackbar("Download cancelled"))
    }

    private fun handleInstallApk(fileUri: String) {
        try {
            val intent = storageManager.createInstallIntent(fileUri)
            sendEffect(HomeEffect.LaunchInstallIntent(intent))
        } catch (e: Exception) {
            sendEffect(HomeEffect.ShowSnackbar("Failed to launch installer: ${e.localizedMessage}"))
        }
    }

    private fun handleOpenMagiskZip(fileUri: String) {
        try {
            val intent = storageManager.createOpenOrShareIntent(fileUri, isModule = true)
            sendEffect(HomeEffect.LaunchShareIntent(intent))
        } catch (e: Exception) {
            sendEffect(HomeEffect.ShowSnackbar("Failed to open file: ${e.localizedMessage}"))
        }
    }

    private fun handleArchChange(appKey: String, arch: String) {
        viewModelScope.launch {
            manageSettingsUseCase.updateAppArchOverride(appKey, arch)
            sendEffect(HomeEffect.ShowSnackbar("Architecture updated to $arch"))
        }
    }

    private fun handleCleanOldBuilds() {
        viewModelScope.launch {
            val result = cleanOldBuildsUseCase(currentState.settings.downloadFolderUri)
            setState { copy(showCleanDialog = false) }
            val freedStr = FormatHelper.formatBytes(result.freedBytes)
            sendEffect(HomeEffect.ShowSnackbar("Cleaned ${result.filesDeletedCount} old builds. Freed $freedStr"))
            // Rescan
            val files = scanOldBuildsUseCase(currentState.settings.downloadFolderUri)
            setState { copy(oldBuildsFound = files) }
            updateDisplayBuilds()
        }
    }

    private fun handleCompleteSetup(intent: HomeIntent.CompleteSetup) {
        viewModelScope.launch {
            manageSettingsUseCase.updateStorageFolder(intent.folderUri, intent.folderPath)
            manageSettingsUseCase.updatePreferredArch(intent.arch)
            manageSettingsUseCase.updateRepoUrl(intent.repoUrl)
            manageSettingsUseCase.updateBuildFormats(intent.showApks, intent.showModules)
            manageSettingsUseCase.markSetupCompleted()
            setState { copy(showSetupDialog = false) }
            sendEffect(HomeEffect.ShowSnackbar("Setup completed! Fetching builds..."))
        }
    }
}
