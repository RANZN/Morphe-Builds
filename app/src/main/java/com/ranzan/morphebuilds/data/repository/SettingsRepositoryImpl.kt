package com.ranzan.morphebuilds.data.repository

import com.ranzan.morphebuilds.data.local.PreferencesDataStore
import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val preferencesDataStore: PreferencesDataStore
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> = preferencesDataStore.settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        preferencesDataStore.updateSettings(settings)
    }

    override suspend fun updateRepoUrl(url: String) {
        preferencesDataStore.updateRepoUrl(url)
    }

    override suspend fun updatePreferredArch(arch: String) {
        preferencesDataStore.updatePreferredArch(arch)
    }

    override suspend fun updateAppArchOverride(appKey: String, arch: String) {
        preferencesDataStore.updateAppArchOverride(appKey, arch)
    }

    override suspend fun updateStorageFolder(uri: String?, displayPath: String?) {
        preferencesDataStore.updateStorageFolder(uri, displayPath)
    }

    override suspend fun updateAutoClean(enabled: Boolean) {
        preferencesDataStore.updateAutoClean(enabled)
    }

    override suspend fun updateCleanupIntervalDays(days: Int) {
        preferencesDataStore.updateCleanupIntervalDays(days)
    }

    override suspend fun updateBuildFormats(showApks: Boolean, showModules: Boolean) {
        preferencesDataStore.updateBuildFormats(showApks, showModules)
    }

    override suspend fun updateGithubToken(token: String?) {
        preferencesDataStore.updateGithubToken(token)
    }

    override suspend fun markSetupCompleted() {
        preferencesDataStore.markSetupCompleted()
    }

}
