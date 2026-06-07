package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),       // Blue Accent
    secondary = Color(0xFF22D3EE),     // Cyan Accent
    tertiary = Color(0xFF60A5FA),
    background = Color(0xFF000000),    // Pure Black Background
    surface = Color(0xFF111827),       // Dark Gray Surface
    surfaceVariant = Color(0xFF111827), // Dark Gray Cards
    onBackground = Color(0xFFFFFFFF),  // White Text
    onSurface = Color(0xFFFFFFFF),     // White Text
    onSurfaceVariant = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFF3B82F6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),       // Purple Primary
    secondary = Color(0xFFA78BFA),     // Lavender Secondary
    tertiary = Color(0xFFC4B5FD),
    background = Color(0xFFFFFFFF),    // White Background
    surface = Color(0xFFF3F4F6),       // Light Gray Surface
    surfaceVariant = Color(0xFFF3F4F6), // Light Gray Cards
    onBackground = Color(0xFF000000),  // Black Text
    onSurface = Color(0xFF000000),     // Black Text
    onSurfaceVariant = Color(0xFF000000),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF7C3AED)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
