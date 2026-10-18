package com.ranzan.morphebuilds.domain.model

enum class DownloadState {
    IDLE,
    CONNECTING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadItem(
    val assetId: String,
    val fileName: String,
    val appType: AppType,
    val version: String,
    val isModule: Boolean,
    val state: DownloadState = DownloadState.IDLE,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val destinationUri: String? = null,
    val destinationPath: String? = null,
    val errorMessage: String? = null
) {
    val isFinished: Boolean
        get() = state == DownloadState.COMPLETED || state == DownloadState.FAILED || state == DownloadState.CANCELLED

    val isRunning: Boolean
        get() = state == DownloadState.CONNECTING || state == DownloadState.DOWNLOADING
}
