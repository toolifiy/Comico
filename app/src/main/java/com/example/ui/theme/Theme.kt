package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CapCutLightColorScheme = lightColorScheme(
    primary = CapCutBlue,
    onPrimary = Color.White,
    primaryContainer = CapCutBlueLight,
    onPrimaryContainer = CapCutBlue,
    secondary = Gray900,
    onSecondary = Color.White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray900,
    tertiary = CapCutBlue,
    onTertiary = Color.White,
    background = PureWhite,
    onBackground = Gray900,
    surface = PureWhite,
    onSurface = Gray900,
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray600,
    outline = Gray200,
    outlineVariant = Gray100,
    error = CoralRed,
    onError = Color.White
)

private val CapCutDarkColorScheme = darkColorScheme(
    primary = CapCutBlue,
    onPrimary = Color.White,
    primaryContainer = Gray800,
    onPrimaryContainer = CapCutBlueLight,
    secondary = PureWhite,
    onSecondary = Gray900,
    secondaryContainer = Gray800,
    onSecondaryContainer = PureWhite,
    tertiary = CapCutBlue,
    onTertiary = Color.White,
    background = PureWhite,
    onBackground = Gray900,
    surface = PureWhite,
    onSurface = Gray900,
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray600,
    outline = Gray200,
    outlineVariant = Gray100,
    error = CoralRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Clean white theme as explicitly requested
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CapCutLightColorScheme,
        typography = Typography,
        content = content
    )
}
