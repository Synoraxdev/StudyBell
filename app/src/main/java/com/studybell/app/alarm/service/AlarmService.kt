package com.studybell.app.alarm.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.studybell.app.R
import com.studybell.app.alarm.AlarmIntents
import com.studybell.app.container
import com.studybell.app.core.audio.AlarmPlayer
import com.studybell.app.core.notifications.NotificationHelper
import com.studybell.app.domain.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service (type mediaPlayback) that owns the ringing lifecycle.
 *
 * Overlap policy (deterministic): exactly ONE AlarmPlayer exists. The first alarm that fires
 * rings; any alarm firing meanwhile is queued in firing order and starts ringing, with its own
 * settings, only after the current one is dismissed. A duplicate delivery for an alarm that is
 * already ringing/queued is ignored. The service ends when the queue is empty.
 */
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val queue = ArrayDeque<Alarm>()
    private lateinit var player: AlarmPlayer
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        player = AlarmPlayer(this)
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1L) ?: -1L
        when (intent?.action) {
            AlarmIntents.ACTION_FIRE -> {
                enterForeground()
                scope.launch { handleFire(id) }
            }
            AlarmIntents.ACTION_DISMISS -> dismiss(id)
            else -> if (queue.isEmpty()) finish()
        }
        // If the OS kills the service while ringing it is not resurrected (documented limitation).
        return START_NOT_STICKY
    }

    private fun enterForeground() {
        val n = NotificationHelper.buildRinging(
            this, getString(R.string.alarm_generic_title), getString(R.string.alarm_notification_text),
        )
        ServiceCompat.startForeground(this, NotificationHelper.NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    private suspend fun handleFire(id: Long) {
        val alarm = container.repository.consumeFire(id)
        if (alarm == null) {
            if (queue.isEmpty()) finish()
            return
        }
        if (queue.any { it.id == alarm.id }) return // duplicate delivery
        queue.addLast(alarm)
        if (queue.size == 1) startRinging(alarm) else publishState()
    }

    private suspend fun startRinging(alarm: Alarm) {
        val settings = container.settingsStore.settings.first()
        player.start(alarm.ringtoneUri ?: settings.defaultRingtoneUri)
        if (alarm.vibrate) startVibration() else vibrator?.cancel()

        val title = alarm.label.ifBlank { getString(R.string.alarm_generic_title) }
        val n = NotificationHelper.buildRinging(this, title, getString(R.string.alarm_notification_text))
        getSystemService(NotificationManager::class.java).notify(NotificationHelper.NOTIFICATION_ID, n)
        publishState()
    }

    /** Completes the current alarm exactly once: ignored unless [id] is the alarm now ringing. */
    private fun dismiss(id: Long) {
        val current = queue.firstOrNull()
        if (current == null || current.id != id) {
            if (queue.isEmpty()) finish()
            return
        }
        queue.removeFirst()
        player.stop()
        vibrator?.cancel()
        val next = queue.firstOrNull()
        if (next == null) finish() else scope.launch { startRinging(next) }
    }

    private fun publishState() {
        val cur = queue.firstOrNull()
        AlarmRuntime.state.value = cur?.let {
            RingingState(it.id, it.label, it.holdSeconds, queued = queue.size - 1)
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0)
        val attrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        vibrator?.vibrate(effect, attrs)
    }

    private fun finish() {
        player.stop()
        vibrator?.cancel()
        AlarmRuntime.state.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NotificationHelper.NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        player.stop()
        vibrator?.cancel()
        if (queue.isNotEmpty()) AlarmRuntime.state.value = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun fireIntent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmService::class.java)
                .setAction(AlarmIntents.ACTION_FIRE)
                .putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)

        fun dismissIntent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmService::class.java)
                .setAction(AlarmIntents.ACTION_DISMISS)
                .putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
    }
}
