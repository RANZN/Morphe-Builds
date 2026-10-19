package com.ranzan.morphebuilds

import android.app.Application
import androidx.work.WorkManager
import com.ranzan.morphebuilds.di.appModules
import com.ranzan.morphebuilds.domain.repository.CleanupScheduler
import com.ranzan.morphebuilds.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MorpheApplication : Application() {

    private val cleanupScheduler: CleanupScheduler by inject()
    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MorpheApplication)
            if (!WorkManager.isInitialized()) {
                workManagerFactory()
            }
            modules(appModules)
        }

        // Initialize background auto-cleanup schedule respecting user preference
        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsRepository.getSettings().first()
            if (settings.autoCleanOldVersions && settings.cleanupIntervalDays > 0) {
                cleanupScheduler.scheduleCleanup(settings.cleanupIntervalDays)
            }
        }
    }
}
