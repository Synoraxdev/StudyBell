package com.studybell.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.studybell.app.core.permissions.Readiness
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

/** Current local time, refreshed once per second only while the screen is at least STARTED. */
@Composable
fun rememberNow(): State<ZonedDateTime> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state = remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                state.value = ZonedDateTime.now()
                delay(1000L - (System.currentTimeMillis() % 1000L))
            }
        }
    }
    return state
}

class ReadinessHolder(private val context: android.content.Context) {
    var value by mutableStateOf(Readiness.check(context))
        private set

    fun refresh() { value = Readiness.check(context) }
}

/** Permission/capability status that is re-read every time the screen resumes (e.g. back from system settings). */
@Composable
fun rememberReadiness(): ReadinessHolder {
    val context = LocalContext.current
    val holder = remember { ReadinessHolder(context) }
    LifecycleResumeEffect(Unit) {
        holder.refresh()
        onPauseOrDispose { }
    }
    return holder
}
