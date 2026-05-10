package com.itihaasa.nammakathey.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RoyalIndigo,
    secondary = HeritageOchre,
    tertiary = MutedClay,
    primaryContainer = IndigoContainer,
    secondaryContainer = OchreContainer,
    tertiaryContainer = ClayContainer,
    background = Parchment,
    surface = ParchmentLight,
    surfaceVariant = ParchmentVariant,
    outline = RoyalIndigo,
    outlineVariant = RoyalIndigo,
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
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
