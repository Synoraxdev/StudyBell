package com.studybell.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.studybell.app.core.notifications.NotificationHelper
import com.studybell.app.data.local.AppDatabase
import com.studybell.app.data.preferences.SettingsStore
import com.studybell.app.data.repository.AlarmRepository
import com.studybell.app.domain.scheduling.AlarmScheduler

/** Tiny manual dependency container (no DI framework needed for an app this size). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { Room.databaseBuilder(appContext, AppDatabase::class.java, "studybell.db").build() }
    val scheduler = AlarmScheduler(appContext)
    val settingsStore = SettingsStore(appContext)
    val repository by lazy { AlarmRepository(database.alarmDao(), scheduler) }
}

class StudyBellApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as StudyBellApp).container
