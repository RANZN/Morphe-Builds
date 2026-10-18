package com.ranzan.morphebuilds.domain.usecase

import com.ranzan.morphebuilds.domain.model.CleanableFile
import com.ranzan.morphebuilds.domain.repository.DownloadRepository

data class CleanResult(
    val filesDeletedCount: Int,
    val freedBytes: Long
)

class CleanOldBuildsUseCase(
    private val scanOldBuildsUseCase: ScanOldBuildsUseCase,
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(folderUri: String?): CleanResult {
        val files = scanOldBuildsUseCase(folderUri)
        val oldFiles = files.filter { it.isOldVersion }
        if (oldFiles.isEmpty()) {
            return CleanResult(0, 0L)
        }

        val totalBytes = oldFiles.sumOf { it.sizeBytes }
        val count = downloadRepository.deleteFiles(oldFiles.map { it.uriString })

        return CleanResult(
            filesDeletedCount = count,
            freedBytes = totalBytes
        )
    }

    suspend fun deleteSpecificFiles(files: List<CleanableFile>): CleanResult {
        val totalBytes = files.sumOf { it.sizeBytes }
        val count = downloadRepository.deleteFiles(files.map { it.uriString })
        return CleanResult(count, totalBytes)
    }
}
