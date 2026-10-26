package com.studybell.app.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studybell.app.core.permissions.Readiness
import com.studybell.app.core.theme.StudyColors

/** Live status of every capability that affects alarm reliability. Never shows a fake "granted". */
@Composable
fun SetupChecklist(holder: ReadinessHolder, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val r = holder.value
    var asked by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { holder.refresh() }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChecklistRow(
            title = "Notifications",
            body = "Shows the ringing alarm and lets you return to it. Without this, the alarm may still sound but you may see no alarm screen or notification.",
            ok = r.notificationsAllowed,
            okText = "Allowed",
            problemText = "Not allowed",
            actionText = "Allow",
        ) {
            if (Build.VERSION.SDK_INT >= 33 && !asked) {
                asked = true
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Readiness.open(context, Readiness.appNotificationSettings(context))
            }
        }
        ChecklistRow(
            title = "Alarm screen over the lock screen",
            body = if (r.fullScreenNeedsUserAction)
                "Android 14+ can require you to allow full-screen alarms. If off, you only get a notification you must tap."
            else "Allowed automatically on this Android version. Some phones still hide it while you are actively using the screen.",
            ok = r.fullScreenAllowed,
            okText = "Allowed",
            problemText = "Not allowed",
            actionText = "Open settings",
        ) { Readiness.open(context, Readiness.fullScreenSettings(context)) }
        ChecklistRow(
            title = "Alarm scheduling",
            body = "Uses Android's alarm-clock scheduler, which needs no extra permission. It cannot ring if the device is powered off or the app was force-stopped.",
            ok = true,
            okText = "Built in",
            problemText = "",
            actionText = null,
        ) {}
        ChecklistRow(
            title = "Battery restrictions",
            body = "Optional. Some manufacturers (Xiaomi, Oppo, Vivo, Samsung, Huawei…) add extra limits. If alarms are late or missing, set StudyBell to unrestricted.",
            ok = r.batteryUnrestricted,
            okText = "Unrestricted",
            problemText = "Optimized",
            problemColor = StudyColors.TextSecondary,
            actionText = "Battery settings",
        ) { Readiness.open(context, Readiness.batterySettings()) }
        ChecklistRow(
            title = "Alarm volume",
            body = "StudyBell never changes your volume. Alarms follow the device's alarm volume and are quiet if you silenced it.",
            ok = !r.alarmVolumeMuted,
            okText = "Audible",
            problemText = "Muted",
            actionText = "Sound settings",
        ) { Readiness.open(context, Readiness.soundSettings()) }
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    body: String,
    ok: Boolean,
    okText: String,
    problemText: String,
    actionText: String?,
    problemColor: Color = StudyColors.Warning,
    onAction: () -> Unit,
) {
    StudyCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusBadge(if (ok) okText else problemText, if (ok) StudyColors.Success else problemColor)
        }
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = StudyColors.TextSecondary)
        if (!ok && actionText != null) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(actionText, onAction, Modifier.fillMaxWidth())
        }
    }
}
