package com.iptvplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = Color(0xFF03DAC5),
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ErrorRed
)

@Composable
fun IPTVPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
