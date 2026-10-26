package com.studybell.app.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.studybell.app.alarm.AlarmIntents
import com.studybell.app.alarm.service.AlarmService

/** Receives the AlarmManager trigger and hands off to the foreground ringing service immediately. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmIntents.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1L)
        if (id < 0) return
        ContextCompat.startForegroundService(context, AlarmService.fireIntent(context, id))
    }
}
