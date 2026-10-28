package com.example

import android.app.Application
import com.example.di.AppContainer
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

/**
 * Application class managing application-level singleton dependencies and DI container.
 */
class MBTrakerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Firebase and App Check
        Firebase.initialize(this)
        val appCheckProviderFactory = if (com.example.BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory)
        
        // Remote Config for AI Models
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(
            "milo_gemini_model" to "gemini-1.5-flash"
        ))
        remoteConfig.fetchAndActivate()

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
