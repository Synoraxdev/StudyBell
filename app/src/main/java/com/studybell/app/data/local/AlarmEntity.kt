package com.studybell.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.studybell.app.domain.model.Alarm
import java.time.DayOfWeek

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    /** Bit (DayOfWeek.value - 1) set = repeats on that day. 0 = one-time. */
    val repeatMask: Int,
    val enabled: Boolean,
    val ringtoneUri: String?,
    val ringtoneName: String?,
    val vibrate: Boolean,
    val holdSeconds: Int,
)

fun daysToMask(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

fun maskToDays(mask: Int): Set<DayOfWeek> =
    DayOfWeek.values().filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

fun AlarmEntity.toDomain() = Alarm(
    id = id, hour = hour, minute = minute, label = label, repeatDays = maskToDays(repeatMask),
    enabled = enabled, ringtoneUri = ringtoneUri, ringtoneName = ringtoneName,
    vibrate = vibrate, holdSeconds = holdSeconds,
)

fun Alarm.toEntity() = AlarmEntity(
    id = id, hour = hour, minute = minute, label = label, repeatMask = daysToMask(repeatDays),
    enabled = enabled, ringtoneUri = ringtoneUri, ringtoneName = ringtoneName,
    vibrate = vibrate, holdSeconds = holdSeconds,
)
