package com.studybell.app.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studybell.app.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.TimeZone

/**
 * Re-registers every enabled alarm after reboot (BOOT_COMPLETED is delivered once the user has
 * unlocked the device, because the database is credential-encrypted), app update, and
 * clock/timezone changes.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) TimeZone.setDefault(null)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        context.container.repository.rescheduleAll()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
