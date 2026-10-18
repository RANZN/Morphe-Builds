package com.ranzan.morphebuilds.di

import com.ranzan.morphebuilds.data.local.FileDownloader
import com.ranzan.morphebuilds.data.local.PreferencesDataStore
import com.ranzan.morphebuilds.data.local.StorageManager
import com.ranzan.morphebuilds.data.remote.GitHubApiService
import com.ranzan.morphebuilds.data.repository.DownloadRepositoryImpl
import com.ranzan.morphebuilds.data.repository.PackageInspectorRepositoryImpl
import com.ranzan.morphebuilds.data.repository.ReleaseRepositoryImpl
import com.ranzan.morphebuilds.data.repository.SettingsRepositoryImpl
import com.ranzan.morphebuilds.data.work.AutoCleanupWorker
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
import com.ranzan.morphebuilds.ui.home.HomeViewModel
import com.ranzan.morphebuilds.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val appModule = module {
    single { GitHubApiService() }
    single { PreferencesDataStore(androidContext()) }
    single { StorageManager(androidContext()) }
    single { FileDownloader(get()) }
}

val repositoryModule = module {
    single<ReleaseRepository> { ReleaseRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<DownloadRepository> { DownloadRepositoryImpl(get(), get()) }
    single<PackageInspectorRepository> { PackageInspectorRepositoryImpl(androidContext()) }
    single<CleanupScheduler> { WorkManagerScheduler(androidContext()) }
}

val useCaseModule = module {
    factory { GetReleasesUseCase(get()) }
    factory { ScanOldBuildsUseCase(get()) }
    factory { CheckUpdatesUseCase(get()) }
    factory { DownloadBuildUseCase(get()) }
    factory { CleanOldBuildsUseCase(get(), get()) }
    factory { ManageSettingsUseCase(get()) }
    factory { ScheduleCleanupUseCase(get()) }
    factory { ValidateRepositoryUseCase(get()) }
}

val viewModelModule = module {
    viewModel {
        HomeViewModel(
            getReleasesUseCase = get(),
            checkUpdatesUseCase = get(),
            downloadBuildUseCase = get(),
            scanOldBuildsUseCase = get(),
            cleanOldBuildsUseCase = get(),
            manageSettingsUseCase = get()
        )
    }
    viewModel {
        SettingsViewModel(
            manageSettingsUseCase = get(),
            validateRepositoryUseCase = get(),
            scheduleCleanupUseCase = get()
        )
    }
}

val workerModule = module {
    worker { AutoCleanupWorker(androidContext(), get()) }
}

val appModules = listOf(
    appModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
    workerModule
)
