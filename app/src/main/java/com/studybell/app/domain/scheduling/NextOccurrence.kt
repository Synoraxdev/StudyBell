package com.studybell.app.domain.scheduling

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pure calculation of the next time an alarm should ring (strictly after [now]).
 *
 * Daylight saving: the alarm is defined in local wall-clock time. If that local time does not
 * exist (spring-forward gap) java.time moves it forward by the gap length (02:30 -> 03:30);
 * if it occurs twice (fall-back overlap) the earlier instant is used.
 */
object NextOccurrence {
    fun compute(hour: Int, minute: Int, repeatDays: Set<DayOfWeek>, now: ZonedDateTime): ZonedDateTime {
        val time = LocalTime.of(hour, minute)
        val today = now.toLocalDate()
        for (offset in 0..7) {
            val date = today.plusDays(offset.toLong())
            if (repeatDays.isNotEmpty() && date.dayOfWeek !in repeatDays) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        // Unreachable: a candidate always exists within 8 days.
        return ZonedDateTime.of(today.plusDays(1), time, now.zone)
    }
}
