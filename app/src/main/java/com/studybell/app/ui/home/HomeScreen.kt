package com.studybell.app.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studybell.app.R
import com.studybell.app.alarm.service.AlarmRuntime
import com.studybell.app.container
import com.studybell.app.core.theme.StudyColors
import com.studybell.app.core.theme.studySwitchColors
import com.studybell.app.core.time.TimeFormat
import com.studybell.app.domain.model.Alarm
import com.studybell.app.domain.scheduling.NextOccurrence
import com.studybell.app.ui.components.PrimaryButton
import com.studybell.app.ui.components.SectionLabel
import com.studybell.app.ui.components.SecondaryButton
import com.studybell.app.ui.components.StudyCard
import com.studybell.app.ui.components.rememberNow
import com.studybell.app.ui.components.ringtoneLabel
import com.studybell.app.ui.ringing.RingingActivity
import java.time.Duration

@Composable
fun HomeScreen(onAddAlarm: () -> Unit, onEditAlarm: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(container.repository, container.settingsStore) } },
    )
    val state by vm.state.collectAsState()
    val now by rememberNow()
    val ringing by AlarmRuntime.state.collectAsState()
    var pendingDelete by remember { mutableStateOf<Alarm?>(null) }
    val use24 = state.settings.use24Hour

    // Next enabled alarm, recomputed every tick so time/timezone changes are reflected immediately.
    val next = state.alarms.filter { it.enabled }
        .map { it to NextOccurrence.compute(it.hour, it.minute, it.repeatDays, now) }
        .minByOrNull { it.second.toInstant() }

    Scaffold(
        containerColor = StudyColors.Background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAlarm,
                containerColor = StudyColors.TextPrimary,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add alarm", fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResourceAppName(), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            }
            item {
                StudyCard(Modifier.fillMaxWidth()) {
                    Text(TimeFormat.date(now), color = StudyColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        TimeFormat.clockWithSeconds(now, use24),
                        style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (ringing != null) {
                item {
                    StudyCard(Modifier.fillMaxWidth()) {
                        Text("An alarm is ringing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton("Return to alarm", {
                            context.startActivity(Intent(context, RingingActivity::class.java))
                        }, Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                StudyCard(Modifier.fillMaxWidth()) {
                    Text("NEXT ALARM", style = MaterialTheme.typography.labelMedium, color = StudyColors.TextSecondary)
                    if (next == null) {
                        Spacer(Modifier.height(6.dp))
                        Text("No alarm is set", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    } else {
                        val (alarm, at) = next
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "in " + TimeFormat.countdown(Duration.between(now, at)),
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            TimeFormat.describeOccurrence(at, now, use24) +
                                if (alarm.label.isNotBlank()) " · ${alarm.label}" else "",
                            color = StudyColors.TextSecondary, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item { SectionLabel("Your alarms") }
            if (state.loaded && state.alarms.isEmpty()) {
                item {
                    StudyCard(Modifier.fillMaxWidth()) {
                        Text("No alarms yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Create your first alarm: pick a time, choose a ringtone, and decide how long you must hold to stop it.",
                            color = StudyColors.TextSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton("Create alarm", onAddAlarm, Modifier.fillMaxWidth())
                    }
                }
            } else {
                items(state.alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        use24 = use24,
                        defaultRingtoneName = state.settings.defaultRingtoneName,
                        onClick = { onEditAlarm(alarm.id) },
                        onToggle = { vm.setEnabled(alarm, it) },
                        onDelete = { pendingDelete = alarm },
                    )
                }
            }
        }
    }

    pendingDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = StudyColors.Elevated,
            title = { Text("Delete alarm?") },
            text = { Text("This removes ${alarm.label.ifBlank { "this alarm" }} and cancels it.") },
            confirmButton = { TextButton(onClick = { vm.delete(alarm); pendingDelete = null }) { Text("Delete", color = StudyColors.Danger) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun stringResourceAppName(): String = androidx.compose.ui.res.stringResource(R.string.app_name)

@Composable
private fun AlarmCard(
    alarm: Alarm,
    use24: Boolean,
    defaultRingtoneName: String?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val (time, ampm) = TimeFormat.timeParts(alarm.hour, alarm.minute, use24)
    StudyCard(Modifier.fillMaxWidth().alpha(if (alarm.enabled) 1f else 0.55f), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(time, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    if (ampm.isNotEmpty()) {
                        Text(ampm, style = MaterialTheme.typography.titleMedium, color = StudyColors.TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                if (alarm.label.isNotBlank()) {
                    Text(alarm.label, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                }
                Text(TimeFormat.repeatSummary(alarm.repeatDays), color = StudyColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "♪ " + ringtoneLabel(alarm.ringtoneUri, alarm.ringtoneName, defaultRingtoneName),
                    color = StudyColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = alarm.enabled, onCheckedChange = onToggle, colors = studySwitchColors(),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete alarm", tint = StudyColors.TextSecondary)
                }
            }
        }
    }
}
