package com.ranzan.morphebuilds

import android.app.Application
import androidx.work.WorkManager
import com.ranzan.morphebuilds.data.work.WorkManagerScheduler
import com.ranzan.morphebuilds.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MorpheApplication : Application() {

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

        // Initialize WorkManager periodic auto-cleanup
        WorkManagerScheduler.scheduleCleanup(this, 7)
    }
}
