package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repo.RunRepository

class RunEmpireApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: RunRepository by lazy {
        RunRepository(
            database.profileDao(),
            database.activityDao(),
            database.territoryDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Osmdroid Configuration to resolve tile loading & cache folder initialization
        try {
            org.osmdroid.config.Configuration.getInstance().load(
                this,
                android.preference.PreferenceManager.getDefaultSharedPreferences(this)
            )
            org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: RunEmpireApplication
            private set
    }
}
