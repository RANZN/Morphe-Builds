package com.ranzan.morphebuilds.di

import android.content.Context
import com.ranzan.morphebuilds.data.local.AndroidDownloadManagerDownloader
import com.ranzan.morphebuilds.data.local.PreferencesDataStore
import com.ranzan.morphebuilds.data.local.StorageManager
import com.ranzan.morphebuilds.data.remote.GitHubApiService
import com.ranzan.morphebuilds.data.repository.DownloadRepositoryImpl
import com.ranzan.morphebuilds.data.repository.PackageInspectorRepositoryImpl
import com.ranzan.morphebuilds.data.repository.ReleaseRepositoryImpl
import com.ranzan.morphebuilds.data.repository.SettingsRepositoryImpl
import com.ranzan.morphebuilds.data.work.WorkManagerScheduler
import com.ranzan.morphebuilds.domain.repository.CleanupScheduler
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
import com.ranzan.morphebuilds.domain.usecase.ScheduleCleanupUseCase
import com.ranzan.morphebuilds.domain.usecase.ValidateRepositoryUseCase

class AppContainer(val context: Context) {

    // Data sources
    val apiService: GitHubApiService by lazy { GitHubApiService() }
    val preferencesDataStore: PreferencesDataStore by lazy { PreferencesDataStore(context) }
    val storageManager: StorageManager by lazy { StorageManager(context) }
    val androidDownloadManagerDownloader: AndroidDownloadManagerDownloader by lazy {
        AndroidDownloadManagerDownloader(context, storageManager)
    }

    // Repositories
    val releaseRepository: ReleaseRepository by lazy { ReleaseRepositoryImpl(apiService) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(preferencesDataStore) }
    val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(androidDownloadManagerDownloader, storageManager)
    }
    val packageInspectorRepository: PackageInspectorRepository by lazy { PackageInspectorRepositoryImpl(context) }
    val cleanupScheduler: CleanupScheduler by lazy { WorkManagerScheduler(context) }

    // Use cases
    val getReleasesUseCase: GetReleasesUseCase by lazy { GetReleasesUseCase(releaseRepository) }
    val scanOldBuildsUseCase: ScanOldBuildsUseCase by lazy { ScanOldBuildsUseCase(downloadRepository) }
    val checkUpdatesUseCase: CheckUpdatesUseCase by lazy { CheckUpdatesUseCase(packageInspectorRepository) }
    val downloadBuildUseCase: DownloadBuildUseCase by lazy { DownloadBuildUseCase(downloadRepository) }
    val cleanOldBuildsUseCase: CleanOldBuildsUseCase by lazy { CleanOldBuildsUseCase(scanOldBuildsUseCase, downloadRepository) }
    val manageSettingsUseCase: ManageSettingsUseCase by lazy { ManageSettingsUseCase(settingsRepository) }
    val scheduleCleanupUseCase: ScheduleCleanupUseCase by lazy { ScheduleCleanupUseCase(cleanupScheduler) }
    val validateRepositoryUseCase: ValidateRepositoryUseCase by lazy { ValidateRepositoryUseCase(releaseRepository) }
}
