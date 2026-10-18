package com.ranzan.morphebuilds

import android.app.Application
import androidx.work.WorkManager
import com.ranzan.morphebuilds.di.appModules
import com.ranzan.morphebuilds.domain.repository.CleanupScheduler
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MorpheApplication : Application() {

    private val cleanupScheduler: CleanupScheduler by inject()

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

        // Initialize background auto-cleanup schedule (default 7 days)
        cleanupScheduler.scheduleCleanup(7)
    }
}
