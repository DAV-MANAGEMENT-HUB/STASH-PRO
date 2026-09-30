package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.StashRepository
import com.example.engine.ReminderNotificationManager
import com.example.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StashApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var notificationManager: ReminderNotificationManager
        private set

    lateinit var repository: StashRepository
        private set

    lateinit var cloudSyncManager: CloudSyncManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        preferencesRepository = PreferencesRepository(this)
        notificationManager = ReminderNotificationManager(this)
        repository = StashRepository(
            context = this,
            stashDao = database.stashDao(),
            trackerDao = database.trackerDao(),
            folderDao = database.folderDao(),
            notificationManager = notificationManager
        )
        cloudSyncManager = CloudSyncManager(this, preferencesRepository)

        // Automatic 30-day retention cleanup on launch
        applicationScope.launch {
            repository.cleanupExpiredTrash()
        }
    }

    companion object {
        lateinit var instance: StashApplication
            private set
    }
}
