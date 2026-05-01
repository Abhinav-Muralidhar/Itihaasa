package com.itihaasa.nammakathey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HeritageOchre,
    secondary = Parchment,
    tertiary = MutedClay,
    primaryContainer = ForestGreen,
    secondaryContainer = DeepIndigo,
    tertiaryContainer = MutedClay,
    background = Charcoal,
    surface = ForestGreenDark,
    surfaceVariant = DeepIndigo,
    outline = ParchmentVariant,
    outlineVariant = OutlineBrown,
    onPrimary = Charcoal,
    onSecondary = Charcoal,
    onTertiary = Parchment,
    onPrimaryContainer = Parchment,
    onSecondaryContainer = Parchment,
    onTertiaryContainer = Parchment,
    onBackground = Parchment,
    onSurface = Parchment,
    onSurfaceVariant = ParchmentVariant
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    secondary = HeritageOchre,
    tertiary = MutedClay,
    primaryContainer = ForestContainer,
    secondaryContainer = OchreContainer,
    tertiaryContainer = ClayContainer,
    background = Parchment,
    surface = ParchmentLight,
    surfaceVariant = ParchmentVariant,
    outline = OutlineBrown,
    outlineVariant = ParchmentVariant,
    onPrimary = Color.White,
    onSecondary = Charcoal,
    onTertiary = Color.White,
    onPrimaryContainer = Charcoal,
    onSecondaryContainer = Charcoal,
    onTertiaryContainer = Charcoal,
    onBackground = Charcoal,
    onSurface = Charcoal,
    onSurfaceVariant = Charcoal
)

@Composable
fun NammakatheyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
