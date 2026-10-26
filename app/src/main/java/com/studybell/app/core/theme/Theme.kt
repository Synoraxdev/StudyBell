package com.studybell.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Monochrome palette. Change here to re-skin the whole app. */
object StudyColors {
    val Background = Color(0xFF090909)
    val Surface = Color(0xFF141414)
    val Elevated = Color(0xFF1B1B1B)
    val Border = Color(0xFF2A2A2A)
    val TextPrimary = Color(0xFFFAFAFA)
    val TextSecondary = Color(0xFFA1A1AA)
    val Danger = Color(0xFFF87171)
    val Warning = Color(0xFFFBBF24)
    val Success = Color(0xFF4ADE80)
}

private val StudyScheme = darkColorScheme(
    primary = StudyColors.TextPrimary,
    onPrimary = Color.Black,
    primaryContainer = StudyColors.Elevated,
    onPrimaryContainer = StudyColors.TextPrimary,
    secondary = StudyColors.TextSecondary,
    onSecondary = Color.Black,
    secondaryContainer = StudyColors.Elevated,
    onSecondaryContainer = StudyColors.TextPrimary,
    tertiary = StudyColors.TextSecondary,
    tertiaryContainer = StudyColors.Border,
    onTertiaryContainer = StudyColors.TextPrimary,
    background = StudyColors.Background,
    onBackground = StudyColors.TextPrimary,
    surface = StudyColors.Surface,
    onSurface = StudyColors.TextPrimary,
    surfaceVariant = StudyColors.Elevated,
    onSurfaceVariant = StudyColors.TextSecondary,
    surfaceContainerLowest = StudyColors.Background,
    surfaceContainerLow = StudyColors.Surface,
    surfaceContainer = StudyColors.Surface,
    surfaceContainerHigh = StudyColors.Elevated,
    surfaceContainerHighest = StudyColors.Elevated,
    outline = StudyColors.Border,
    outlineVariant = StudyColors.Border,
    error = StudyColors.Danger,
    onError = Color.Black,
)

// Typography: Material 3 defaults, which use the device's system sans-serif font. No runtime font download.
// To use Inter, add the .ttf files to res/font and set a FontFamily in a custom Typography here.
@Composable
fun StudyBellTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StudyScheme, content = content)
}

@Composable
fun studySwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.Black,
    checkedTrackColor = StudyColors.TextPrimary,
    checkedBorderColor = StudyColors.TextPrimary,
    uncheckedThumbColor = StudyColors.TextSecondary,
    uncheckedTrackColor = StudyColors.Elevated,
    uncheckedBorderColor = StudyColors.Border,
)
