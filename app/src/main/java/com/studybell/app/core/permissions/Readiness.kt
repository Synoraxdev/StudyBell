package com.studybell.app.core.permissions

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.studybell.app.core.audio.AlarmAudio

/** Real, re-checkable status of everything that affects whether an alarm will be noticed. */
data class Readiness(
    val notificationsAllowed: Boolean,
    /** True on Android < 14 (granted at install) or when the user allowed it on 14+. */
    val fullScreenAllowed: Boolean,
    val fullScreenNeedsUserAction: Boolean,
    val batteryUnrestricted: Boolean,
    val alarmVolumeMuted: Boolean,
) {
    companion object {
        fun check(context: Context): Readiness {
            val nm = context.getSystemService(NotificationManager::class.java)
            val fullScreen = if (Build.VERSION.SDK_INT >= 34) nm.canUseFullScreenIntent() else true
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return Readiness(
                notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled(),
                fullScreenAllowed = fullScreen,
                fullScreenNeedsUserAction = Build.VERSION.SDK_INT >= 34,
                batteryUnrestricted = pm.isIgnoringBatteryOptimizations(context.packageName),
                alarmVolumeMuted = AlarmAudio.isAlarmVolumeMuted(context),
            )
        }

        fun appNotificationSettings(context: Context): Intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

        fun fullScreenSettings(context: Context): Intent =
            if (Build.VERSION.SDK_INT >= 34) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
            } else appDetails(context)

        fun batterySettings(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        fun soundSettings(): Intent = Intent(Settings.ACTION_SOUND_SETTINGS)

        fun appDetails(context: Context): Intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

        /** Opens [intent]; falls back to the app's system details page if no handler exists. */
        fun open(context: Context, intent: Intent) {
            runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { runCatching { context.startActivity(appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        }
    }
}
