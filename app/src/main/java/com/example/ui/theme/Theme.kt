package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RadiantGold,
    onPrimary = Color(0xFF1E1408),
    primaryContainer = DarkBookSurfaceVariant,
    onPrimaryContainer = RadiantGold,
    secondary = CandleLight,
    onSecondary = Color(0xFF261908),
    tertiary = RadiantGold,
    background = DarkBookBackground,
    onBackground = DarkBookText,
    surface = DarkBookSurface,
    onSurface = DarkBookText,
    surfaceVariant = DarkBookSurfaceVariant,
    onSurfaceVariant = DarkMutedText,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = WarmTerracotta,
    onPrimary = Color.White,
    primaryContainer = CreamSurfaceVariant,
    onPrimaryContainer = WarmTerracotta,
    secondary = GoldenAmber,
    onSecondary = Color.White,
    tertiary = GoldenAmber,
    background = CreamBackground,
    onBackground = VintageInk,
    surface = CreamSurface,
    onSurface = VintageInk,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = MutedSepia,
    outline = SoftBorderLight
)

@Composable
fun LivingPageTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
