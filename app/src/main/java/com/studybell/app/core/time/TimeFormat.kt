package com.studybell.app.core.time

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Pure formatting helpers (unit-tested). */
object TimeFormat {
    /** Returns (time, amPm). amPm is empty in 24-hour mode. */
    fun timeParts(hour: Int, minute: Int, use24Hour: Boolean, locale: Locale = Locale.getDefault()): Pair<String, String> {
        val t = LocalTime.of(hour, minute)
        return if (use24Hour) {
            DateTimeFormatter.ofPattern("HH:mm", locale).format(t) to ""
        } else {
            DateTimeFormatter.ofPattern("h:mm", locale).format(t) to DateTimeFormatter.ofPattern("a", locale).format(t)
        }
    }

    fun clock(hour: Int, minute: Int, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val (t, suffix) = timeParts(hour, minute, use24Hour, locale)
        return if (suffix.isEmpty()) t else "$t $suffix"
    }

    fun clockWithSeconds(now: ZonedDateTime, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val pattern = if (use24Hour) "HH:mm:ss" else "h:mm:ss a"
        return DateTimeFormatter.ofPattern(pattern, locale).format(now)
    }

    fun date(now: ZonedDateTime, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(now)

    fun repeatSummary(days: Set<DayOfWeek>, locale: Locale = Locale.getDefault()): String {
        val weekdays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        return when {
            days.isEmpty() -> "Once"
            days.size == 7 -> "Every day"
            days == weekdays -> "Weekdays"
            days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
            else -> DayOfWeek.values().filter { it in days }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
        }
    }

    fun countdown(duration: Duration): String {
        val total = maxOf(0L, duration.seconds)
        val d = total / 86_400
        val h = (total % 86_400) / 3_600
        val m = (total % 3_600) / 60
        val s = total % 60
        return when {
            d > 0 -> "${d}d ${h}h ${m}m"
            h > 0 -> "${h}h ${m}m ${s}s"
            else -> "${m}m ${s}s"
        }
    }

    /** "Today at 7:00 AM", "Tomorrow at 7:00 AM", "Tue, 27 Oct at 7:00 AM". */
    fun describeOccurrence(next: ZonedDateTime, now: ZonedDateTime, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val nextDate = next.toLocalDate()
        val today = now.toLocalDate()
        val day = when (nextDate) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> DateTimeFormatter.ofPattern("EEE, d MMM", locale).format(next)
        }
        return "$day at ${clock(next.hour, next.minute, use24Hour, locale)}"
    }
}
