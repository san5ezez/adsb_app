package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PlanesDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = CyanBright,
    secondary = AmberAccent,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF633F00),
    onSecondaryContainer = AmberLight,
    tertiary = EmeraldGreen,
    onTertiary = Color(0xFF00391A),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = CoralRed,
    onError = Color.White
)

@Composable
fun PlanesAdsbTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PlanesDarkColorScheme,
        typography = Typography,
        content = content
    )
}
