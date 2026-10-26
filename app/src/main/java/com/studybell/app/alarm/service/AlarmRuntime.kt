package com.studybell.app.alarm.service

import kotlinx.coroutines.flow.MutableStateFlow

data class RingingState(
    val alarmId: Long,
    val label: String,
    val holdSeconds: Int,
    /** Other alarms waiting behind this one. */
    val queued: Int,
)

/** Process-wide view of what is ringing right now; written only by [AlarmService]. */
object AlarmRuntime {
    val state = MutableStateFlow<RingingState?>(null)
}
