package com.example.simpletaskmanagement.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SplendBlueLight,
    onPrimary = Color.White,
    primaryContainer = SplendBlueDark,
    onPrimaryContainer = Color.White,
    secondary = SplendAccent,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = TextSecondaryDark,
    error = OverdueRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = SplendBlue,
    onPrimary = Color.White,
    primaryContainer = SplendBlueLight,
    onPrimaryContainer = Color.White,
    secondary = SplendAccent,
    onSecondary = Color.Black,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCardBg,
    onSurfaceVariant = TextSecondaryLight,
    error = OverdueRed
)

@Composable
fun SimpleTaskManagementTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}