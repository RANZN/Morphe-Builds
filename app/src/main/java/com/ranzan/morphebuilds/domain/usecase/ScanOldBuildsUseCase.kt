package com.ranzan.morphebuilds.domain.usecase

import com.ranzan.morphebuilds.domain.model.CleanableFile
import com.ranzan.morphebuilds.domain.repository.DownloadRepository

class ScanOldBuildsUseCase(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(folderUri: String?): List<CleanableFile> {
        val files = downloadRepository.scanDownloadedFiles(folderUri)
        // Group files by (appType, isModule)
        val grouped = files.groupBy { Pair(it.appType, it.isModule) }

        return files.map { file ->
            val siblingFiles = grouped[Pair(file.appType, file.isModule)] ?: emptyList()
            // If there is more than 1 file for this app and this isn't the most recently modified or highest version
            val isOld = siblingFiles.size > 1 && file != siblingFiles.maxByOrNull { it.lastModified }
            file.copy(isOldVersion = isOld)
        }
    }
}
