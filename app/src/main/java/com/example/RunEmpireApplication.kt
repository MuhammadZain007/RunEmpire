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
    }

    companion object {
        lateinit var instance: RunEmpireApplication
            private set
    }
}
