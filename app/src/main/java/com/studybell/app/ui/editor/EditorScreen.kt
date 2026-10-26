package com.studybell.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.studybell.app.container
import com.studybell.app.core.audio.AlarmPlayer
import com.studybell.app.core.theme.StudyColors
import com.studybell.app.core.theme.studySwitchColors
import com.studybell.app.core.time.TimeFormat
import com.studybell.app.domain.model.HOLD_OPTIONS
import com.studybell.app.domain.model.MAX_LABEL_LENGTH
import com.studybell.app.domain.scheduling.NextOccurrence
import com.studybell.app.ui.components.PillSelector
import com.studybell.app.ui.components.PrimaryButton
import com.studybell.app.ui.components.RingtoneChooser
import com.studybell.app.ui.components.SecondaryButton
import com.studybell.app.ui.components.SectionLabel
import com.studybell.app.ui.components.StudyCard
import com.studybell.app.ui.components.StudyTopBar
import com.studybell.app.ui.components.rememberNow
import com.studybell.app.ui.components.ringtoneLabel
import java.time.DayOfWeek
import java.time.Duration
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(alarmId: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val vm: EditorViewModel = viewModel(
        factory = viewModelFactory { initializer { EditorViewModel(container.repository, container.settingsStore, alarmId) } },
    )
    val state by vm.state.collectAsState()

    if (!state.loaded) {
        Box(Modifier.fillMaxSize().background(StudyColors.Background))
        return
    }
    if (state.notFound) {
        LaunchedEffect(Unit) { onClose() }
        Box(Modifier.fillMaxSize().background(StudyColors.Background))
        return
    }

    val timeState = rememberTimePickerState(state.hour, state.minute, state.use24Hour)
    val now by rememberNow()
    val nextAt = NextOccurrence.compute(timeState.hour, timeState.minute, state.days, now)

    var showChooser by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    val previewPlayer = remember { AlarmPlayer(context) }
    DisposableEffect(Unit) { onDispose { previewPlayer.stop() } }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = StudyColors.Background,
        topBar = { StudyTopBar(if (alarmId > 0) "Edit alarm" else "New alarm", onBack = onClose) },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(StudyColors.Background).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton("Cancel", onClose, Modifier.weight(1f))
                PrimaryButton(
                    "Save", { previewPlayer.stop(); vm.save(timeState.hour, timeState.minute, onClose) },
                    Modifier.weight(1f), enabled = !state.saving,
                )
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StudyCard(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timeState) }
            }

            StudyCard(Modifier.fillMaxWidth()) {
                Text("NEXT RING", style = MaterialTheme.typography.labelMedium, color = StudyColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    TimeFormat.describeOccurrence(nextAt, now, state.use24Hour),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "in " + TimeFormat.countdown(Duration.between(now, nextAt)),
                    color = StudyColors.TextSecondary, style = MaterialTheme.typography.bodyMedium,
                )
            }

            OutlinedTextField(
                value = state.label,
                onValueChange = vm::setLabel,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Label (optional)") },
                placeholder = { Text("Morning Study") },
                singleLine = true,
                supportingText = { Text("${state.label.length}/$MAX_LABEL_LENGTH") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudyColors.TextPrimary, unfocusedBorderColor = StudyColors.Border,
                    focusedContainerColor = StudyColors.Surface, unfocusedContainerColor = StudyColors.Surface,
                ),
            )

            SectionLabel("Repeat")
            StudyCard(Modifier.fillMaxWidth()) {
                DayChips(state.days, vm::toggleDay)
                Spacer(Modifier.height(8.dp))
                Text(TimeFormat.repeatSummary(state.days), color = StudyColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }

            SectionLabel("Ringtone")
            StudyCard(Modifier.fillMaxWidth()) {
                val label = ringtoneLabel(state.ringtoneUri, state.ringtoneName, state.defaultRingtoneName)
                Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Change", { previewPlayer.stop(); previewing = false; showChooser = true }, Modifier.weight(1f))
                    SecondaryButton(
                        if (previewing) "Stop" else "Preview",
                        {
                            if (previewing) {
                                previewPlayer.stop(); previewing = false
                            } else {
                                previewing = true
                                previewPlayer.start(state.ringtoneUri ?: state.defaultRingtoneUri, loop = false) { previewing = false }
                            }
                        },
                        Modifier.weight(1f),
                    )
                }
            }

            StudyCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Vibration", style = MaterialTheme.typography.titleMedium)
                        Text("Vibrate while ringing", color = StudyColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = state.vibrate, onCheckedChange = vm::setVibrate, colors = studySwitchColors())
                }
            }

            SectionLabel("Hold to dismiss")
            StudyCard(Modifier.fillMaxWidth()) {
                PillSelector(
                    options = HOLD_OPTIONS.map { "$it s" },
                    selectedIndex = HOLD_OPTIONS.indexOf(state.holdSeconds).coerceAtLeast(0),
                    onSelect = { vm.setHold(HOLD_OPTIONS[it]) },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You must keep your finger on the button for ${state.holdSeconds} second${if (state.holdSeconds == 1) "" else "s"}.",
                    color = StudyColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showChooser) {
        RingtoneChooser(
            currentUri = state.ringtoneUri,
            defaultOptionLabel = state.defaultRingtoneName ?: "bundled tone",
            onDismiss = { showChooser = false },
            onChosen = { uri, name -> vm.setRingtone(uri, name); showChooser = false },
        )
    }
}

@Composable
private fun DayChips(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val locale = Locale.getDefault()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.values().forEach { day ->
            val isOn = day in selected
            val full = day.getDisplayName(TextStyle.FULL, locale)
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(if (isOn) StudyColors.TextPrimary else StudyColors.Elevated, RoundedCornerShape(12.dp))
                    .border(1.dp, if (isOn) StudyColors.TextPrimary else StudyColors.Border, RoundedCornerShape(12.dp))
                    .toggleable(value = isOn, role = Role.Checkbox, onValueChange = { onToggle(day) })
                    .semantics { contentDescription = full },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    day.getDisplayName(TextStyle.NARROW, locale),
                    color = if (isOn) Color.Black else StudyColors.TextPrimary,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                )
            }
        }
    }
}
