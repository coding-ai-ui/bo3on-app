package com.bo3on.teacher

import android.app.Application
import androidx.work.*
import com.bo3on.teacher.data.database.TeacherDatabase
import com.bo3on.teacher.data.repository.*
import com.bo3on.teacher.utils.ReminderWorker
import java.util.concurrent.TimeUnit

class Bo3onApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
    val database by lazy { TeacherDatabase.create(this) }
    val repo by lazy { TeacherRepository(database) }
    val settings by lazy { SettingsStore(this) }
    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("teacher-reminders", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build())
    }
}
