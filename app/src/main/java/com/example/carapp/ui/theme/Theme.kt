package com.example.carapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF126B55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F3E5),
    onPrimaryContainer = Color(0xFF08392D),
    secondary = Color(0xFF53665E),
    background = Color(0xFFF6F8F5),
    surface = Color(0xFFF6F8F5),
    onSurface = Color(0xFF19251F),
    onSurfaceVariant = Color(0xFF65746D),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEDF2EE),
    surfaceContainerHigh = Color(0xFFE6EDE7),
    outlineVariant = Color(0xFFDDE5DF),
    secondaryContainer = Color(0xFFE2EEE6),
    onSecondaryContainer = Color(0xFF184D3C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD5B6),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF174F3E),
    onPrimaryContainer = Color(0xFFD5F3E5),
    secondary = Color(0xFFB5CCBF),
    background = Color(0xFF101914),
    surface = Color(0xFF101914),
    surfaceContainerLow = Color(0xFF1A251E),
    surfaceContainer = Color(0xFF223027),
    surfaceContainerHigh = Color(0xFF2A3A30),
    onSurfaceVariant = Color(0xFFB4C6BA),
    outlineVariant = Color(0xFF354A3D),
    secondaryContainer = Color(0xFF294236),
    onSecondaryContainer = Color(0xFFC6EBD7)
)

@Composable
fun CarAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, content = content)
}
