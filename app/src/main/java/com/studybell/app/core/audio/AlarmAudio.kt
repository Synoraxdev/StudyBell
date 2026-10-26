package com.studybell.app.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.provider.OpenableColumns
import android.media.RingtoneManager
import com.studybell.app.R

object AlarmAudio {
    fun bundledUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.default_ringtone}")

    /** True when the alarm stream volume is 0 (the alarm would be silent). */
    fun isAlarmVolumeMuted(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getStreamVolume(AudioManager.STREAM_ALARM) == 0
    }

    /** Human-readable name for a picked ringtone / audio file. Never throws. */
    fun displayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getString(0)
                    if (!n.isNullOrBlank()) return n
                }
            }
        }
        runCatching {
            val title = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
            if (!title.isNullOrBlank()) return title
        }
        return "Custom audio"
    }
}

/**
 * Plays one alarm sound at a time using USAGE_ALARM (follows the alarm volume stream; the app never
 * changes system volume). If the chosen sound cannot be opened or fails mid-playback, it falls back
 * to the bundled ringtone.
 */
class AlarmPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    fun start(uriString: String?, loop: Boolean = true, onFinished: (() -> Unit)? = null) {
        stop()
        requestFocus()
        val custom = uriString?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val ok = custom != null && play(custom, loop, onFinished, isFallback = false)
        if (!ok) play(AlarmAudio.bundledUri(context), loop, onFinished, isFallback = true)
    }

    private fun play(uri: Uri, loop: Boolean, onFinished: (() -> Unit)?, isFallback: Boolean): Boolean {
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(attributes)
            mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mp.setDataSource(context, uri)
            mp.isLooping = loop
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { if (!loop) onFinished?.invoke() }
            mp.setOnErrorListener { _, _, _ ->
                if (player === mp) {
                    release()
                    if (!isFallback) play(AlarmAudio.bundledUri(context), loop, onFinished, true) else onFinished?.invoke()
                }
                true
            }
            player = mp
            mp.prepareAsync()
            true
        } catch (e: Exception) {
            runCatching { mp.release() }
            if (player === mp) player = null
            false
        }
    }

    fun stop() {
        release()
        abandonFocus()
    }

    private fun release() {
        player?.let { mp ->
            player = null
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.release() }
        }
    }

    private fun requestFocus() {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { /* Alarm keeps playing: it must not be silenced by other apps. */ }
            .build()
        focusRequest = req
        audioManager.requestAudioFocus(req)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
