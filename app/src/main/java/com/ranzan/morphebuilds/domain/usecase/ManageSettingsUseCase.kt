package com.ranzan.morphebuilds.domain.usecase

import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ManageSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    fun getSettings(): Flow<AppSettings> = settingsRepository.getSettings()

    suspend fun updateRepoUrl(url: String) = settingsRepository.updateRepoUrl(url)

    suspend fun updatePreferredArch(arch: String) = settingsRepository.updatePreferredArch(arch)

    suspend fun updateAppArchOverride(appKey: String, arch: String) =
        settingsRepository.updateAppArchOverride(appKey, arch)

    suspend fun updateStorageFolder(uri: String?, displayPath: String?) =
        settingsRepository.updateStorageFolder(uri, displayPath)

    suspend fun updateAutoClean(enabled: Boolean) = settingsRepository.updateAutoClean(enabled)

    suspend fun updateCleanupIntervalDays(days: Int) =
        settingsRepository.updateCleanupIntervalDays(days)

    suspend fun updateBuildFormats(showApks: Boolean, showModules: Boolean) =
        settingsRepository.updateBuildFormats(showApks, showModules)

    suspend fun updateGithubToken(token: String?) = settingsRepository.updateGithubToken(token)

    suspend fun markSetupCompleted() = settingsRepository.markSetupCompleted()
}
