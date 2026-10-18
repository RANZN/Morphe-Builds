package com.ranzan.morphebuilds.di

import android.content.Context
import com.ranzan.morphebuilds.data.local.FileDownloader
import com.ranzan.morphebuilds.data.local.PreferencesDataStore
import com.ranzan.morphebuilds.data.local.StorageManager
import com.ranzan.morphebuilds.data.remote.GitHubApiService
import com.ranzan.morphebuilds.data.repository.DownloadRepositoryImpl
import com.ranzan.morphebuilds.data.repository.PackageInspectorRepositoryImpl
import com.ranzan.morphebuilds.data.repository.ReleaseRepositoryImpl
import com.ranzan.morphebuilds.data.repository.SettingsRepositoryImpl
import com.ranzan.morphebuilds.domain.repository.DownloadRepository
import com.ranzan.morphebuilds.domain.repository.PackageInspectorRepository
import com.ranzan.morphebuilds.domain.repository.ReleaseRepository
import com.ranzan.morphebuilds.domain.repository.SettingsRepository
import com.ranzan.morphebuilds.domain.usecase.CheckUpdatesUseCase
import com.ranzan.morphebuilds.domain.usecase.CleanOldBuildsUseCase
import com.ranzan.morphebuilds.domain.usecase.DownloadBuildUseCase
import com.ranzan.morphebuilds.domain.usecase.GetReleasesUseCase
import com.ranzan.morphebuilds.domain.usecase.ManageSettingsUseCase
import com.ranzan.morphebuilds.domain.usecase.ScanOldBuildsUseCase

class AppContainer(val context: Context) {

    // Data sources
    val apiService: GitHubApiService by lazy { GitHubApiService() }
    val preferencesDataStore: PreferencesDataStore by lazy { PreferencesDataStore(context) }
    val storageManager: StorageManager by lazy { StorageManager(context) }
    val fileDownloader: FileDownloader by lazy { FileDownloader(storageManager) }

    // Repositories
    val releaseRepository: ReleaseRepository by lazy { ReleaseRepositoryImpl(apiService) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(preferencesDataStore) }
    val downloadRepository: DownloadRepository by lazy { DownloadRepositoryImpl(fileDownloader, storageManager) }
    val packageInspectorRepository: PackageInspectorRepository by lazy { PackageInspectorRepositoryImpl(context) }

    // Use cases
    val getReleasesUseCase: GetReleasesUseCase by lazy { GetReleasesUseCase(releaseRepository) }
    val scanOldBuildsUseCase: ScanOldBuildsUseCase by lazy { ScanOldBuildsUseCase(downloadRepository) }
    val checkUpdatesUseCase: CheckUpdatesUseCase by lazy { CheckUpdatesUseCase(packageInspectorRepository) }
    val downloadBuildUseCase: DownloadBuildUseCase by lazy { DownloadBuildUseCase(downloadRepository) }
    val cleanOldBuildsUseCase: CleanOldBuildsUseCase by lazy { CleanOldBuildsUseCase(scanOldBuildsUseCase, downloadRepository) }
    val manageSettingsUseCase: ManageSettingsUseCase by lazy { ManageSettingsUseCase(settingsRepository) }
}
