package com.studybell.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybell.app.data.preferences.SettingsStore
import com.studybell.app.data.repository.AlarmRepository
import com.studybell.app.domain.model.Alarm
import com.studybell.app.domain.model.HOLD_OPTIONS
import com.studybell.app.domain.model.MAX_LABEL_LENGTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class EditorState(
    val loaded: Boolean = false,
    val notFound: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val days: Set<DayOfWeek> = emptySet(),
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null,
    val vibrate: Boolean = true,
    val holdSeconds: Int = 3,
    val use24Hour: Boolean = false,
    val defaultRingtoneUri: String? = null,
    val defaultRingtoneName: String? = null,
    val saving: Boolean = false,
)

class EditorViewModel(
    private val repo: AlarmRepository,
    private val store: SettingsStore,
    private val alarmId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var original: Alarm? = null

    init {
        viewModelScope.launch {
            val s = store.settings.first()
            val existing = if (alarmId > 0) repo.get(alarmId) else null
            original = existing
            _state.value = when {
                alarmId > 0 && existing == null -> EditorState(loaded = true, notFound = true)
                existing != null -> EditorState(
                    loaded = true, hour = existing.hour, minute = existing.minute, label = existing.label,
                    days = existing.repeatDays, ringtoneUri = existing.ringtoneUri, ringtoneName = existing.ringtoneName,
                    vibrate = existing.vibrate, holdSeconds = existing.holdSeconds, use24Hour = s.use24Hour,
                    defaultRingtoneUri = s.defaultRingtoneUri, defaultRingtoneName = s.defaultRingtoneName,
                )
                else -> EditorState(
                    loaded = true, vibrate = s.defaultVibrate, holdSeconds = s.defaultHoldSeconds, use24Hour = s.use24Hour,
                    defaultRingtoneUri = s.defaultRingtoneUri, defaultRingtoneName = s.defaultRingtoneName,
                )
            }
        }
    }

    fun setLabel(value: String) = _state.update { it.copy(label = value.take(MAX_LABEL_LENGTH)) }

    fun toggleDay(day: DayOfWeek) = _state.update {
        it.copy(days = if (day in it.days) it.days - day else it.days + day)
    }

    fun setRingtone(uri: String?, name: String?) = _state.update { it.copy(ringtoneUri = uri, ringtoneName = name) }
    fun setVibrate(value: Boolean) = _state.update { it.copy(vibrate = value) }
    fun setHold(seconds: Int) = _state.update { it.copy(holdSeconds = seconds.takeIf { s -> s in HOLD_OPTIONS } ?: 3) }

    /** Saving always (re)enables the alarm and schedules its next occurrence. */
    fun save(hour: Int, minute: Int, onSaved: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repo.save(
                Alarm(
                    id = original?.id ?: 0, hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59),
                    label = s.label.trim(), repeatDays = s.days, enabled = true,
                    ringtoneUri = s.ringtoneUri, ringtoneName = s.ringtoneName,
                    vibrate = s.vibrate, holdSeconds = s.holdSeconds,
                ),
            )
            onSaved()
        }
    }
}
