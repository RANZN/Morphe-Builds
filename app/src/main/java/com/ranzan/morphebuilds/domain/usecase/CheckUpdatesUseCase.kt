package com.ranzan.morphebuilds.domain.usecase

import com.ranzan.morphebuilds.core.utils.FormatHelper
import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.AppType
import com.ranzan.morphebuilds.domain.model.AppUpdateStatus
import com.ranzan.morphebuilds.domain.model.CleanableFile
import com.ranzan.morphebuilds.domain.repository.PackageInspectorRepository

class CheckUpdatesUseCase(
    private val packageInspectorRepository: PackageInspectorRepository
) {
    operator fun invoke(
        build: AppBuild,
        downloadedFiles: List<CleanableFile>
    ): AppUpdateStatus {
        val installedVersion = packageInspectorRepository.findInstalledVersionForApp(build.appType)

        // Find if this build is already downloaded in storage
        val matchingFile = downloadedFiles.firstOrNull { file ->
            file.appType == build.appType &&
                    file.isModule == build.isModule &&
                    file.name.equals(build.fileName, ignoreCase = true)
        }

        val downloadedVersion = matchingFile?.version
        val downloadedFileUri = matchingFile?.uriString

        return when {
            installedVersion != null -> {
                val comparison = FormatHelper.compareVersions(build.version, installedVersion)
                if (comparison > 0) {
                    AppUpdateStatus.UpdateAvailable(
                        installedVersion = installedVersion,
                        latestVersion = build.version,
                        downloadedVersion = downloadedVersion,
                        downloadedFileUri = downloadedFileUri
                    )
                } else {
                    AppUpdateStatus.UpToDate(
                        currentVersion = installedVersion,
                        downloadedVersion = downloadedVersion,
                        downloadedFileUri = downloadedFileUri
                    )
                }
            }
            matchingFile != null -> {
                AppUpdateStatus.DownloadedOnly(
                    downloadedVersion = matchingFile.version,
                    fileUri = matchingFile.uriString,
                    isLatest = matchingFile.version == build.version
                )
            }
            else -> {
                AppUpdateStatus.NotInstalled(
                    latestVersion = build.version,
                    downloadedVersion = downloadedVersion,
                    downloadedFileUri = downloadedFileUri
                )
            }
        }
    }
}
