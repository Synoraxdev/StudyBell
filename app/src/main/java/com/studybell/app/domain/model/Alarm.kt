package com.studybell.app.domain.model

import java.time.DayOfWeek

/** An alarm as the app logic sees it. Empty [repeatDays] means a one-time alarm. */
data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
    /** null = use the app-wide default ringtone from settings. */
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null,
    val vibrate: Boolean = true,
    val holdSeconds: Int = 3,
)

data class AppSettings(
    /** null = bundled ringtone. */
    val defaultRingtoneUri: String? = null,
    val defaultRingtoneName: String? = null,
    val defaultHoldSeconds: Int = 3,
    val defaultVibrate: Boolean = true,
    val use24Hour: Boolean = false,
    val onboardingDone: Boolean = false,
)

val HOLD_OPTIONS = listOf(1, 2, 3)
const val MAX_LABEL_LENGTH = 40
