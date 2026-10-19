package com.ranzan.morphebuilds.data.local

import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for downloading build assets (APKs and Magisk modules).
 * Enables seamless switching between OkHttp-based streaming and Android DownloadManager.
 */
interface BuildDownloader {
    fun download(build: AppBuild, folderUri: String?): Flow<DownloadItem>
    fun cancel(assetId: String)
}
