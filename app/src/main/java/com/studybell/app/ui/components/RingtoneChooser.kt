package com.studybell.app.ui.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.IntentCompat
import com.studybell.app.R
import com.studybell.app.core.audio.AlarmAudio
import com.studybell.app.core.theme.StudyColors

/**
 * Ringtone source chooser. Uses the system ringtone picker and the Storage Access Framework
 * document picker, so no storage permission is needed. Picked documents get a persistable
 * read grant; if access is lost later the player falls back to the bundled tone.
 */
@Composable
fun RingtoneChooser(
    currentUri: String?,
    defaultOptionLabel: String?,
    onDismiss: () -> Unit,
    onChosen: (uri: String?, name: String?) -> Unit,
) {
    val context = LocalContext.current
    val bundledName = stringResource(R.string.bundled_tone_name)

    val systemPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uri = res.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        if (res.resultCode == Activity.RESULT_OK && uri != null) {
            onChosen(uri.toString(), AlarmAudio.displayName(context, uri))
        } else onDismiss()
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            onDismiss()
        } else {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onChosen(uri.toString(), AlarmAudio.displayName(context, uri))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudyColors.Elevated,
        title = { Text("Choose ringtone") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                if (defaultOptionLabel != null) {
                    ChooserOption("Use default ($defaultOptionLabel)") { onChosen(null, null) }
                }
                ChooserOption(bundledName) { onChosen(AlarmAudio.bundledUri(context).toString(), bundledName) }
                ChooserOption("System ringtone…") {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose alarm sound")
                    currentUri?.takeIf { it.startsWith("content://") }?.let {
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                    }
                    systemPicker.launch(intent)
                }
                ChooserOption("Audio file from this device…") { filePicker.launch(arrayOf("audio/*")) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ChooserOption(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    }
}

/** Label shown on cards/rows for an alarm's ringtone. */
@Composable
fun ringtoneLabel(alarmUri: String?, alarmName: String?, defaultName: String?): String {
    val bundled = stringResource(R.string.bundled_tone_name)
    return if (alarmUri != null) alarmName ?: "Custom audio" else "Default · ${defaultName ?: bundled}"
}
