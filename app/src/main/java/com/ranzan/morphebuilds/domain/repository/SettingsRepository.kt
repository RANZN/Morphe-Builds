package com.ranzan.morphebuilds.domain.repository

import com.ranzan.morphebuilds.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateRepoUrl(url: String)
    suspend fun updatePreferredArch(arch: String)
    suspend fun updateAppArchOverride(appKey: String, arch: String)
    suspend fun updateStorageFolder(uri: String?, displayPath: String?)
    suspend fun updateAutoClean(enabled: Boolean)
    suspend fun updateCleanupIntervalDays(days: Int)
    suspend fun updateBuildFormats(showApks: Boolean, showModules: Boolean)
    suspend fun updateGithubToken(token: String?)
    suspend fun markSetupCompleted()
}
