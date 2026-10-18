package com.ranzan.morphebuilds.domain.usecase

import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.DownloadItem
import com.ranzan.morphebuilds.domain.model.DownloadState
import com.ranzan.morphebuilds.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class DownloadBuildUseCase(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(
        build: AppBuild,
        folderUri: String?,
        autoCleanOldVersions: Boolean
    ): Flow<DownloadItem> {
        return downloadRepository.downloadBuild(build, folderUri).transform { item ->
            emit(item)
            if (item.state == DownloadState.COMPLETED && autoCleanOldVersions) {
                downloadRepository.cleanOldVersionsForApp(build, folderUri)
            }
        }
    }
}
