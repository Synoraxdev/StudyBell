package com.studybell.app.data.preferences

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studybell.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studybell_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val ringtoneUri = stringPreferencesKey("default_ringtone_uri")
        val ringtoneName = stringPreferencesKey("default_ringtone_name")
        val holdSeconds = intPreferencesKey("default_hold_seconds")
        val vibrate = booleanPreferencesKey("default_vibrate")
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            defaultRingtoneUri = p[Keys.ringtoneUri],
            defaultRingtoneName = p[Keys.ringtoneName],
            defaultHoldSeconds = (p[Keys.holdSeconds] ?: 3).coerceIn(1, 3),
            defaultVibrate = p[Keys.vibrate] ?: true,
            use24Hour = p[Keys.use24Hour] ?: DateFormat.is24HourFormat(context),
            onboardingDone = p[Keys.onboardingDone] ?: false,
        )
    }

    suspend fun setDefaultRingtone(uri: String?, name: String?) {
        context.dataStore.edit {
            if (uri == null) {
                it.remove(Keys.ringtoneUri); it.remove(Keys.ringtoneName)
            } else {
                it[Keys.ringtoneUri] = uri
                if (name != null) it[Keys.ringtoneName] = name else it.remove(Keys.ringtoneName)
            }
        }
    }

    suspend fun setDefaultHoldSeconds(seconds: Int) { context.dataStore.edit { it[Keys.holdSeconds] = seconds.coerceIn(1, 3) } }
    suspend fun setDefaultVibrate(value: Boolean) { context.dataStore.edit { it[Keys.vibrate] = value } }
    suspend fun setUse24Hour(value: Boolean) { context.dataStore.edit { it[Keys.use24Hour] = value } }
    suspend fun setOnboardingDone(value: Boolean) { context.dataStore.edit { it[Keys.onboardingDone] = value } }
}
