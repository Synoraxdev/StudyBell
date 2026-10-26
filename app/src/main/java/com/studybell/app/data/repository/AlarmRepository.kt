package com.studybell.app.data.repository

import com.studybell.app.data.local.AlarmDao
import com.studybell.app.data.local.toDomain
import com.studybell.app.data.local.toEntity
import com.studybell.app.domain.model.Alarm
import com.studybell.app.domain.scheduling.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single owner of "database + AlarmManager" consistency. */
class AlarmRepository(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduler,
) {
    val alarms: Flow<List<Alarm>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): Alarm? = dao.get(id)?.toDomain()

    suspend fun save(alarm: Alarm): Long {
        val id = dao.upsert(alarm.toEntity())
        val saved = alarm.copy(id = id)
        if (saved.enabled) scheduler.schedule(saved) else scheduler.cancel(id)
        return id
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = get(id) ?: return
        dao.setEnabled(id, enabled)
        if (enabled) scheduler.schedule(alarm.copy(enabled = true)) else scheduler.cancel(id)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    /** After boot, app update, or time/timezone change. */
    suspend fun rescheduleAll() {
        dao.getAll().map { it.toDomain() }.forEach {
            if (it.enabled) scheduler.schedule(it) else scheduler.cancel(it.id)
        }
    }

    /**
     * Called when an alarm fires. Returns the alarm if it should ring, otherwise null
     * (deleted, disabled, or already consumed = duplicate delivery of a one-time alarm).
     * Repeating alarms get their next occurrence registered immediately, so the chain
     * survives even if the ringing process dies. One-time alarms are switched off.
     */
    suspend fun consumeFire(id: Long): Alarm? {
        val alarm = get(id) ?: return null
        if (!alarm.enabled) return null
        if (alarm.repeatDays.isEmpty()) {
            dao.setEnabled(id, false)
            scheduler.cancel(id)
        } else {
            scheduler.schedule(alarm)
        }
        return alarm
    }
}
