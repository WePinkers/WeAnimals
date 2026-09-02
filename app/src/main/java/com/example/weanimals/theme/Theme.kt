package com.example.weanimals.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Pine800,
    onPrimary = Color.White,
    primaryContainer = Pine700,
    onPrimaryContainer = Color.White,
    secondary = Clay600,
    onSecondary = Color.White,
    secondaryContainer = Sand100,
    onSecondaryContainer = Ink900,
    tertiary = Brass500,
    background = Sand50,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Sand100,
    onSurfaceVariant = Ink600,
    outline = LineBorder
)

@Composable
fun WeAnimalsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
