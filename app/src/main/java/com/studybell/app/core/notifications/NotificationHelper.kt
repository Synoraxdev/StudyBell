package com.studybell.app.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.studybell.app.R
import com.studybell.app.ui.ringing.RingingActivity

object NotificationHelper {
    const val CHANNEL_RINGING = "alarm_ringing"
    const val NOTIFICATION_ID = 4101

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_RINGING, context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarm_desc)
            setSound(null, null)            // audio is played by AlarmPlayer, not by the notification
            enableVibration(false)          // vibration is handled by the service
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    fun ringingActivityIntent(context: Context): PendingIntent {
        val intent = Intent(context, RingingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /**
     * Ongoing alarm notification. Full-screen intent opens the ringing screen over the lock screen
     * when the system allows it; otherwise it shows as a heads-up notification and tapping it
     * returns to the ringing screen. It has no dismiss action: only the hold gesture stops an alarm.
     */
    fun buildRinging(context: Context, title: String, text: String): Notification {
        val pi = ringingActivityIntent(context)
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
    }
}
