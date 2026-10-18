package com.ranzan.morphebuilds.data.repository

import com.ranzan.morphebuilds.data.local.FileDownloader
import com.ranzan.morphebuilds.data.local.StorageManager
import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.CleanableFile
import com.ranzan.morphebuilds.domain.model.DownloadItem
import com.ranzan.morphebuilds.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class DownloadRepositoryImpl(
    private val fileDownloader: FileDownloader,
    private val storageManager: StorageManager
) : DownloadRepository {

    private val _activeDownloads = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    override fun getActiveDownloads(): StateFlow<Map<String, DownloadItem>> = _activeDownloads.asStateFlow()

    override fun downloadBuild(build: AppBuild, folderUri: String?): Flow<DownloadItem> {
        return fileDownloader.download(build, folderUri).onEach { item ->
            val updated = _activeDownloads.value.toMutableMap()
            updated[build.id] = item
            _activeDownloads.value = updated
        }
    }

    override fun cancelDownload(assetId: String) {
        fileDownloader.cancel(assetId)
        val updated = _activeDownloads.value.toMutableMap()
        val current = updated[assetId]
        if (current != null) {
            updated[assetId] = current.copy(
                state = com.ranzan.morphebuilds.domain.model.DownloadState.CANCELLED,
                errorMessage = "Cancelled"
            )
            _activeDownloads.value = updated
        }
    }

    override suspend fun scanDownloadedFiles(folderUri: String?): List<CleanableFile> =
        withContext(Dispatchers.IO) {
            storageManager.scanFiles(folderUri)
        }

    override suspend fun deleteFiles(fileUris: List<String>): Int = withContext(Dispatchers.IO) {
        var count = 0
        fileUris.forEach { uri ->
            if (storageManager.deleteFile(uri)) {
                count++
            }
        }
        count
    }

    override suspend fun cleanOldVersionsForApp(appBuild: AppBuild, folderUri: String?): Int =
        withContext(Dispatchers.IO) {
            val allFiles = storageManager.scanFiles(folderUri)
            val olderFiles = allFiles.filter { file ->
                file.appType == appBuild.appType &&
                        file.isModule == appBuild.isModule &&
                        !file.name.equals(appBuild.fileName, ignoreCase = true)
            }
            var deleted = 0
            olderFiles.forEach { oldFile ->
                if (storageManager.deleteFile(oldFile.uriString)) {
                    deleted++
                }
            }
            deleted
        }
}
