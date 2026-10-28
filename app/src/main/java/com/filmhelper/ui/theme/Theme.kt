package com.filmhelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm amber, like a film canister label; dark mode leans on deep, darkroom-friendly tones.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB951),
    onPrimary = Color(0xFF452B00),
    primaryContainer = Color(0xFF633F00),
    onPrimaryContainer = Color(0xFFFFDDB4),
    secondary = Color(0xFFDDC2A1),
    onSecondary = Color(0xFF3E2D16),
    secondaryContainer = Color(0xFF56442A),
    onSecondaryContainer = Color(0xFFFBDEBC),
    background = Color(0xFF17130E),
    onBackground = Color(0xFFECE1D7),
    surface = Color(0xFF17130E),
    onSurface = Color(0xFFECE1D7),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF825500),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB4),
    onPrimaryContainer = Color(0xFF291800),
    secondary = Color(0xFF6F5B40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBDEBC),
    onSecondaryContainer = Color(0xFF271904),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF0E0CF),
    onSurfaceVariant = Color(0xFF4F4539),
    error = Color(0xFFBA1A1A),
)

@Composable
fun FilmHelperTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
