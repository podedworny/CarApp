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
    onSurface = Color(0xFF19251F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD5B6),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF174F3E),
    onPrimaryContainer = Color(0xFFD5F3E5),
    secondary = Color(0xFFB5CCBF),
    background = Color(0xFF101914),
    surface = Color(0xFF101914)
)

@Composable
fun CarAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, content = content)
}
