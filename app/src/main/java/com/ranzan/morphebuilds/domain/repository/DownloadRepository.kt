package com.ranzan.morphebuilds.domain.repository

import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.CleanableFile
import com.ranzan.morphebuilds.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    fun downloadBuild(build: AppBuild, folderUri: String?): Flow<DownloadItem>
    fun getActiveDownloads(): StateFlow<Map<String, DownloadItem>>
    fun cancelDownload(assetId: String)
    suspend fun scanDownloadedFiles(folderUri: String?): List<CleanableFile>
    suspend fun deleteFiles(fileUris: List<String>): Int
    suspend fun cleanOldVersionsForApp(appBuild: AppBuild, folderUri: String?): Int
}
