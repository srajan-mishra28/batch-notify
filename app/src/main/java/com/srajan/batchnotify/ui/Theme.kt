package com.srajan.batchnotify.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Calm "fog and moss" palette: low contrast background, one deep green for actions.
private val Light = lightColorScheme(
    primary = Color(0xFF3E6259),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFDDD6),
    onPrimaryContainer = Color(0xFF1C2624),
    secondary = Color(0xFFB7862B),
    background = Color(0xFFE8ECE6),
    onBackground = Color(0xFF1C2624),
    surface = Color(0xFFF6F7F3),
    onSurface = Color(0xFF1C2624),
    surfaceVariant = Color(0xFFDAE1DA),
    onSurfaceVariant = Color(0xFF66726E),
    outline = Color(0xFFB4BEB8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CC3B6),
    onPrimary = Color(0xFF0F2420),
    primaryContainer = Color(0xFF2C4841),
    onPrimaryContainer = Color(0xFFDCEBE5),
    secondary = Color(0xFFE0B65A),
    background = Color(0xFF151C1B),
    onBackground = Color(0xFFE3E8E4),
    surface = Color(0xFF1D2625),
    onSurface = Color(0xFFE3E8E4),
    surfaceVariant = Color(0xFF28322F),
    onSurfaceVariant = Color(0xFF9AA6A1),
    outline = Color(0xFF3F4B48),
)

private val AppType = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun BatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppType,
        content = content,
    )
}
