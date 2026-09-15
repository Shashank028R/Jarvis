package com.jarvis.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = JarvisRedPrimary,
    onPrimary = JarvisBlack,
    primaryContainer = JarvisRedDark,
    onPrimaryContainer = JarvisTextPrimary,
    secondary = JarvisRedBright,
    onSecondary = JarvisBlack,
    background = JarvisBlack,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurfaceDark,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceBorder,
    onSurfaceVariant = JarvisTextSecondary
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
