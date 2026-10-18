package com.ranzan.morphebuilds.ui.settings

import com.ranzan.morphebuilds.core.mvi.UiEffect
import com.ranzan.morphebuilds.core.mvi.UiIntent
import com.ranzan.morphebuilds.core.mvi.UiState
import com.ranzan.morphebuilds.core.utils.Architecture
import com.ranzan.morphebuilds.domain.model.AppSettings

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val repoUrlInput: String = "",
    val githubTokenInput: String = "",
    val detectedDeviceArch: Architecture = Architecture.ARM64_V8A,
    val isTestingRepo: Boolean = false,
    val testRepoMessage: String? = null,
    val isTestRepoSuccess: Boolean = false
) : UiState

sealed interface SettingsIntent : UiIntent {
    data class ChangeRepoUrlInput(val url: String) : SettingsIntent
    data class ChangeGithubTokenInput(val token: String) : SettingsIntent
    object SaveRepoUrl : SettingsIntent
    object ResetRepoUrl : SettingsIntent
    object TestRepoUrl : SettingsIntent
    data class SelectArch(val arch: String) : SettingsIntent
    data class UpdateFolder(val uri: String?, val path: String?) : SettingsIntent
    data class ToggleAutoClean(val enabled: Boolean) : SettingsIntent
    data class SelectCleanupInterval(val days: Int) : SettingsIntent
    data class ToggleBuildFormats(val showApks: Boolean, val showModules: Boolean) : SettingsIntent
    object SaveGithubToken : SettingsIntent
    object DismissTestResult : SettingsIntent
}

sealed interface SettingsEffect : UiEffect {
    data class ShowSnackbar(val message: String) : SettingsEffect
}
