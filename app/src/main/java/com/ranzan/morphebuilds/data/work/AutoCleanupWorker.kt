package com.ranzan.morphebuilds.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ranzan.morphebuilds.domain.repository.DownloadRepository
import com.ranzan.morphebuilds.domain.repository.SettingsRepository
import com.ranzan.morphebuilds.domain.usecase.ScanOldBuildsUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AutoCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val scanOldBuildsUseCase: ScanOldBuildsUseCase by inject()
    private val downloadRepository: DownloadRepository by inject()
    private val settingsRepository: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.getSettings().first()
            if (!settings.autoCleanOldVersions || settings.cleanupIntervalDays <= 0) {
                return Result.success()
            }

            val folderUri = settings.downloadFolderUri
            val files = scanOldBuildsUseCase(folderUri)
            val thresholdMs = settings.cleanupIntervalDays * 24L * 60L * 60L * 1000L
            val now = System.currentTimeMillis()

            // Delete files that are marked as old versions OR older than the selected retention window
            val filesToDelete = files.filter { file ->
                file.isOldVersion && (now - file.lastModified) >= thresholdMs
            }

            if (filesToDelete.isNotEmpty()) {
                downloadRepository.deleteFiles(filesToDelete.map { it.uriString })
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
