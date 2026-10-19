package com.ranzan.morphebuilds.domain.model

sealed interface AppUpdateStatus {
    object Checking : AppUpdateStatus

    data class UpdateAvailable(
        val installedVersion: String?,
        val latestVersion: String,
        val downloadedVersion: String? = null,
        val downloadedFileUri: String? = null
    ) : AppUpdateStatus

    data class UpToDate(
        val currentVersion: String,
        val downloadedVersion: String? = null,
        val downloadedFileUri: String? = null
    ) : AppUpdateStatus

    data class NotInstalled(
        val latestVersion: String,
        val downloadedVersion: String? = null,
        val downloadedFileUri: String? = null
    ) : AppUpdateStatus

    data class DownloadedOnly(
        val downloadedVersion: String,
        val fileUri: String,
        val isLatest: Boolean
    ) : AppUpdateStatus
}
