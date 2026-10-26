package com.studybell.app.domain.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.studybell.app.alarm.AlarmIntents
import com.studybell.app.alarm.receiver.AlarmReceiver
import com.studybell.app.domain.model.Alarm
import com.studybell.app.ui.MainActivity
import java.time.ZonedDateTime

/**
 * Registers alarms with AlarmManager.setAlarmClock(), the API intended for user-facing alarm
 * clocks: exact, works in Doze, shows the system alarm icon, and needs no exact-alarm permission.
 *
 * Each alarm owns a unique PendingIntent (distinct data URI + request code = id), so alarms can
 * never overwrite one another.
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Schedules the next occurrence and returns it. */
    fun schedule(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val next = NextOccurrence.compute(alarm.hour, alarm.minute, alarm.repeatDays, now)
        val info = AlarmManager.AlarmClockInfo(next.toInstant().toEpochMilli(), showPendingIntent(alarm.id))
        alarmManager.setAlarmClock(info, firePendingIntent(alarm.id))
        return next
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(firePendingIntent(alarmId))
    }

    private fun firePendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmIntents.ACTION_FIRE)
            .setData(Uri.parse("studybell://alarm/$id"))
            .putExtra(AlarmIntents.EXTRA_ALARM_ID, id)
        return PendingIntent.getBroadcast(
            context, id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun showPendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).setData(Uri.parse("studybell://show/$id"))
        return PendingIntent.getActivity(
            context, id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
