package com.ranzan.morphebuilds.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ranzan.morphebuilds.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "morphe_settings")

class PreferencesDataStore(private val context: Context) {

    private object PreferencesKeys {
        val REPO_URL = stringPreferencesKey("repo_url")
        val PREFERRED_ARCH = stringPreferencesKey("preferred_arch")
        val ARCH_OVERRIDES = stringPreferencesKey("arch_overrides_json")
        val STORAGE_FOLDER_URI = stringPreferencesKey("storage_folder_uri")
        val STORAGE_FOLDER_PATH = stringPreferencesKey("storage_folder_path")
        val AUTO_CLEAN = booleanPreferencesKey("auto_clean_old_versions")
        val CLEANUP_INTERVAL = intPreferencesKey("cleanup_interval_days")
        val SHOW_APKS = booleanPreferencesKey("show_apks")
        val SHOW_MODULES = booleanPreferencesKey("show_modules")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val overridesJson = preferences[PreferencesKeys.ARCH_OVERRIDES] ?: "{}"
            val overridesMap = parseOverrides(overridesJson)

            AppSettings(
                repoUrl = preferences[PreferencesKeys.REPO_URL] ?: AppSettings.DEFAULT_REPO_URL,
                preferredArch = preferences[PreferencesKeys.PREFERRED_ARCH] ?: "auto",
                appArchOverrides = overridesMap,
                downloadFolderUri = preferences[PreferencesKeys.STORAGE_FOLDER_URI],
                downloadFolderPath = preferences[PreferencesKeys.STORAGE_FOLDER_PATH],
                autoCleanOldVersions = preferences[PreferencesKeys.AUTO_CLEAN] ?: true,
                cleanupIntervalDays = preferences[PreferencesKeys.CLEANUP_INTERVAL] ?: 7,
                showApks = preferences[PreferencesKeys.SHOW_APKS] ?: true,
                showModules = preferences[PreferencesKeys.SHOW_MODULES] ?: false,
                githubToken = preferences[PreferencesKeys.GITHUB_TOKEN],
                isSetupCompleted = preferences[PreferencesKeys.SETUP_COMPLETED] ?: false
            )
        }


    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPO_URL] = settings.repoUrl
            preferences[PreferencesKeys.PREFERRED_ARCH] = settings.preferredArch
            preferences[PreferencesKeys.ARCH_OVERRIDES] = encodeOverrides(settings.appArchOverrides)
            if (settings.downloadFolderUri != null) {
                preferences[PreferencesKeys.STORAGE_FOLDER_URI] = settings.downloadFolderUri
            } else {
                preferences.remove(PreferencesKeys.STORAGE_FOLDER_URI)
            }
            if (settings.downloadFolderPath != null) {
                preferences[PreferencesKeys.STORAGE_FOLDER_PATH] = settings.downloadFolderPath
            } else {
                preferences.remove(PreferencesKeys.STORAGE_FOLDER_PATH)
            }
            preferences[PreferencesKeys.AUTO_CLEAN] = settings.autoCleanOldVersions
            preferences[PreferencesKeys.CLEANUP_INTERVAL] = settings.cleanupIntervalDays
            preferences[PreferencesKeys.SHOW_APKS] = settings.showApks
            preferences[PreferencesKeys.SHOW_MODULES] = settings.showModules
            if (settings.githubToken != null) {
                preferences[PreferencesKeys.GITHUB_TOKEN] = settings.githubToken
            } else {
                preferences.remove(PreferencesKeys.GITHUB_TOKEN)
            }
            preferences[PreferencesKeys.SETUP_COMPLETED] = settings.isSetupCompleted
        }
    }

    suspend fun updateRepoUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPO_URL] = url
        }
    }

    suspend fun updatePreferredArch(arch: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_ARCH] = arch
        }
    }

    suspend fun updateAppArchOverride(appKey: String, arch: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.ARCH_OVERRIDES] ?: "{}"
            val map = parseOverrides(currentJson).toMutableMap()
            map[appKey] = arch
            preferences[PreferencesKeys.ARCH_OVERRIDES] = encodeOverrides(map)
        }
    }

    suspend fun updateStorageFolder(uri: String?, displayPath: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[PreferencesKeys.STORAGE_FOLDER_URI] = uri
            } else {
                preferences.remove(PreferencesKeys.STORAGE_FOLDER_URI)
            }
            if (displayPath != null) {
                preferences[PreferencesKeys.STORAGE_FOLDER_PATH] = displayPath
            } else {
                preferences.remove(PreferencesKeys.STORAGE_FOLDER_PATH)
            }
        }
    }

    suspend fun updateAutoClean(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CLEAN] = enabled
        }
    }

    suspend fun updateGithubToken(token: String?) {
        context.dataStore.edit { preferences ->
            if (token != null) {
                preferences[PreferencesKeys.GITHUB_TOKEN] = token
            } else {
                preferences.remove(PreferencesKeys.GITHUB_TOKEN)
            }
        }
    }

    suspend fun updateCleanupIntervalDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLEANUP_INTERVAL] = days
        }
    }

    suspend fun updateBuildFormats(showApks: Boolean, showModules: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_APKS] = showApks
            preferences[PreferencesKeys.SHOW_MODULES] = showModules
        }
    }

    suspend fun markSetupCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SETUP_COMPLETED] = true
        }
    }


    private fun parseOverrides(jsonStr: String): Map<String, String> {
        return try {
            val json = JSONObject(jsonStr)
            val result = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = json.optString(key)
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun encodeOverrides(map: Map<String, String>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }
}
