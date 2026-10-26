package com.studybell.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybell.app.data.preferences.SettingsStore
import com.studybell.app.data.repository.AlarmRepository
import com.studybell.app.domain.model.Alarm
import com.studybell.app.domain.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val alarms: List<Alarm> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false,
)

class HomeViewModel(private val repo: AlarmRepository, store: SettingsStore) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(repo.alarms, store.settings) { alarms, settings ->
        HomeUiState(alarms, settings, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch { repo.setEnabled(alarm.id, enabled) }
    }

    fun delete(alarm: Alarm) {
        viewModelScope.launch { repo.delete(alarm.id) }
    }
}
