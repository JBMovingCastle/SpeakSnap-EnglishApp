package com.speaksnap.english.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val SpeakSnapColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = TextWhite,
    primaryContainer = PrimaryViolet,
    secondary = SecondaryIndigo,
    onSecondary = TextWhite,
    secondaryContainer = SecondaryMedium,
    tertiary = SuccessGreen,
    background = BackgroundDeep,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextMuted,
    outline = BorderDark,
    outlineVariant = BorderLight,
    error = ErrorRed,
    onError = TextWhite,
)

@Composable
fun SpeakSnapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpeakSnapColorScheme,
        typography = Typography(),
        content = content
    )
}
