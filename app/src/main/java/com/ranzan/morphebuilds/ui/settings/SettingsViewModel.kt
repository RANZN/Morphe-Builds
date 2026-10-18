package com.ranzan.morphebuilds.ui.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ranzan.morphebuilds.core.mvi.BaseViewModel
import com.ranzan.morphebuilds.core.utils.Architecture
import com.ranzan.morphebuilds.data.remote.GitHubApiService
import com.ranzan.morphebuilds.data.work.WorkManagerScheduler
import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.domain.usecase.ManageSettingsUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val manageSettingsUseCase: ManageSettingsUseCase,
    private val apiService: GitHubApiService,
    private val context: Context
) : BaseViewModel<SettingsState, SettingsIntent, SettingsEffect>(
    initialState = SettingsState(detectedDeviceArch = Architecture.detectDeviceArch())
) {

    init {
        viewModelScope.launch {
            manageSettingsUseCase.getSettings().collectLatest { settings ->
                setState {
                    copy(
                        settings = settings,
                        repoUrlInput = settings.repoUrl,
                        githubTokenInput = settings.githubToken ?: ""
                    )
                }
            }
        }
    }

    override fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ChangeRepoUrlInput -> setState { copy(repoUrlInput = intent.url) }
            is SettingsIntent.ChangeGithubTokenInput -> setState { copy(githubTokenInput = intent.token) }
            is SettingsIntent.SaveRepoUrl -> handleSaveRepoUrl()
            is SettingsIntent.ResetRepoUrl -> handleResetRepoUrl()
            is SettingsIntent.TestRepoUrl -> handleTestRepoUrl()
            is SettingsIntent.SelectArch -> handleSelectArch(intent.arch)
            is SettingsIntent.UpdateFolder -> handleUpdateFolder(intent.uri, intent.path)
            is SettingsIntent.ToggleAutoClean -> handleToggleAutoClean(intent.enabled)
            is SettingsIntent.SelectCleanupInterval -> handleSelectCleanupInterval(intent.days)
            is SettingsIntent.ToggleBuildFormats -> handleToggleBuildFormats(intent.showApks, intent.showModules)
            is SettingsIntent.SaveGithubToken -> handleSaveGithubToken()
            is SettingsIntent.DismissTestResult -> setState { copy(testRepoMessage = null) }
        }
    }

    private fun handleToggleBuildFormats(showApks: Boolean, showModules: Boolean) {
        if (!showApks && !showModules) {
            sendEffect(SettingsEffect.ShowSnackbar("At least one format (APKs or Modules) must be selected."))
            return
        }
        viewModelScope.launch {
            manageSettingsUseCase.updateBuildFormats(showApks, showModules)
            sendEffect(SettingsEffect.ShowSnackbar("Build format preferences saved"))
        }
    }

    private fun handleSaveRepoUrl() {
        val url = currentState.repoUrlInput.trim()
        if (url.isBlank()) return
        viewModelScope.launch {
            manageSettingsUseCase.updateRepoUrl(url)
            sendEffect(SettingsEffect.ShowSnackbar("Repository URL saved"))
        }
    }

    private fun handleResetRepoUrl() {
        viewModelScope.launch {
            manageSettingsUseCase.updateRepoUrl(AppSettings.DEFAULT_REPO_URL)
            setState { copy(repoUrlInput = AppSettings.DEFAULT_REPO_URL) }
            sendEffect(SettingsEffect.ShowSnackbar("Reset to default repository"))
        }
    }

    private fun handleTestRepoUrl() {
        val currentSettings = currentState.settings.copy(
            repoUrl = currentState.repoUrlInput.trim(),
            githubToken = currentState.githubTokenInput.trim().ifBlank { null }
        )
        val repoPath = currentSettings.extractRepoPath()
        if (repoPath == null) {
            setState {
                copy(
                    testRepoMessage = "Invalid repository URL format. Please use 'owner/repo' or 'https://github.com/owner/repo'",
                    isTestRepoSuccess = false
                )
            }
            return
        }

        viewModelScope.launch {
            setState { copy(isTestingRepo = true, testRepoMessage = null) }
            try {
                val releases = apiService.getReleases(
                    owner = repoPath.first,
                    repo = repoPath.second,
                    token = currentSettings.githubToken,
                    perPage = 1
                )
                if (releases.isNotEmpty()) {
                    val count = releases.first().assets.size
                    setState {
                        copy(
                            isTestingRepo = false,
                            testRepoMessage = "Connected successfully! Latest release '${releases.first().tagName}' has $count assets.",
                            isTestRepoSuccess = true
                        )
                    }
                } else {
                    setState {
                        copy(
                            isTestingRepo = false,
                            testRepoMessage = "Repository found, but has no published releases.",
                            isTestRepoSuccess = false
                        )
                    }
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isTestingRepo = false,
                        testRepoMessage = "Connection failed: ${e.localizedMessage}",
                        isTestRepoSuccess = false
                    )
                }
            }
        }
    }

    private fun handleSelectArch(arch: String) {
        viewModelScope.launch {
            manageSettingsUseCase.updatePreferredArch(arch)
            sendEffect(SettingsEffect.ShowSnackbar("Preferred architecture set to $arch"))
        }
    }

    private fun handleUpdateFolder(uri: String?, path: String?) {
        viewModelScope.launch {
            manageSettingsUseCase.updateStorageFolder(uri, path)
            sendEffect(SettingsEffect.ShowSnackbar("Download folder updated"))
        }
    }

    private fun handleToggleAutoClean(enabled: Boolean) {
        viewModelScope.launch {
            manageSettingsUseCase.updateAutoClean(enabled)
            val msg = if (enabled) "Auto-clean old versions enabled" else "Auto-clean disabled"
            sendEffect(SettingsEffect.ShowSnackbar(msg))
        }
    }

    private fun handleSelectCleanupInterval(days: Int) {
        viewModelScope.launch {
            manageSettingsUseCase.updateCleanupIntervalDays(days)
            WorkManagerScheduler.scheduleCleanup(context, days)
            val msg = if (days > 0) "Auto-clean schedule set to every $days days" else "Auto-clean schedule disabled"
            sendEffect(SettingsEffect.ShowSnackbar(msg))
        }
    }

    private fun handleSaveGithubToken() {
        val token = currentState.githubTokenInput.trim().ifBlank { null }
        viewModelScope.launch {
            manageSettingsUseCase.updateGithubToken(token)
            sendEffect(SettingsEffect.ShowSnackbar("GitHub token saved"))
        }
    }
}

