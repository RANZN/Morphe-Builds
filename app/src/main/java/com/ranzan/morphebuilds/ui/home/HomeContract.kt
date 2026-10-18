package com.ranzan.morphebuilds.ui.home

import com.ranzan.morphebuilds.core.mvi.UiEffect
import com.ranzan.morphebuilds.core.mvi.UiIntent
import com.ranzan.morphebuilds.core.mvi.UiState
import com.ranzan.morphebuilds.core.utils.Architecture
import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.domain.model.AppUpdateStatus
import com.ranzan.morphebuilds.domain.model.CleanableFile
import com.ranzan.morphebuilds.domain.model.DownloadItem
import com.ranzan.morphebuilds.domain.model.Release

enum class BuildFilterTab {
    ALL,
    APK_ONLY,
    MAGISK_MODULES
}

data class HomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val detectedDeviceArch: Architecture = Architecture.ARM64_V8A,
    val releases: List<Release> = emptyList(),
    val displayBuilds: List<AppBuild> = emptyList(),
    val availableArchsPerApp: Map<String, List<String>> = emptyMap(),
    val selectedTab: BuildFilterTab = BuildFilterTab.ALL,
    val searchQuery: String = "",
    val updateStatuses: Map<String, AppUpdateStatus> = emptyMap(),
    val activeDownloads: Map<String, DownloadItem> = emptyMap(),
    val oldBuildsFound: List<CleanableFile> = emptyList(),
    val error: String? = null,
    val showSetupDialog: Boolean = false,
    val showCleanDialog: Boolean = false,
    val showArchPickerForBuild: AppBuild? = null
) : UiState {
    val updateAvailableCount: Int
        get() = updateStatuses.values.count { it is AppUpdateStatus.UpdateAvailable }

    val cleanableSpaceBytes: Long
        get() = oldBuildsFound.filter { it.isOldVersion }.sumOf { it.sizeBytes }

    val installedBuilds: List<AppBuild>
        get() = displayBuilds.filter { build ->
            val status = updateStatuses[build.id]
            status is AppUpdateStatus.UpdateAvailable || status is AppUpdateStatus.UpToDate
        }

    val notInstalledBuilds: List<AppBuild>
        get() = displayBuilds.filter { build ->
            val status = updateStatuses[build.id]
            status !is AppUpdateStatus.UpdateAvailable && status !is AppUpdateStatus.UpToDate
        }
}

sealed interface HomeIntent : UiIntent {
    object LoadData : HomeIntent
    object Refresh : HomeIntent
    data class SelectTab(val tab: BuildFilterTab) : HomeIntent
    data class SelectRepository(val url: String) : HomeIntent
    data class Search(val query: String) : HomeIntent
    data class Download(val build: AppBuild) : HomeIntent
    data class CancelDownload(val assetId: String) : HomeIntent
    data class InstallApk(val fileUri: String) : HomeIntent
    data class OpenMagiskZip(val fileUri: String) : HomeIntent
    data class ChangeAppArchitecture(val appKey: String, val arch: String) : HomeIntent
    object OpenCleanDialog : HomeIntent
    object DismissCleanDialog : HomeIntent
    object ConfirmCleanOldBuilds : HomeIntent
    data class OpenArchPicker(val build: AppBuild) : HomeIntent
    object DismissArchPicker : HomeIntent

    data class CompleteSetup(
        val folderUri: String?,
        val folderPath: String?,
        val arch: String,
        val repoUrl: String,
        val showApks: Boolean,
        val showModules: Boolean
    ) : HomeIntent
    object DismissError : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data class ShowSnackbar(val message: String) : HomeEffect
    data class InstallApk(val fileUri: String) : HomeEffect
    data class ShareFile(val fileUri: String, val isModule: Boolean) : HomeEffect
    object NavigateToSettings : HomeEffect
}
