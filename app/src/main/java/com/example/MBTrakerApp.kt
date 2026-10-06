package com.example

import android.app.Application
import com.example.di.AppContainer
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Application class managing application-level singleton dependencies and DI container.
 */
class MBTrakerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        
        // Schedule periodic Lead Intent monitoring with battery and network constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<com.example.workers.LeadIntentWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LeadIntentWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        lateinit var instance: MBTrakerApp
            private set
    }
}
