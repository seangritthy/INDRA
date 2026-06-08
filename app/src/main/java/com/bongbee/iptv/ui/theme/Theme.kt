package com.bongbee.iptv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = PrimaryBlue,
    tertiary = AccentPink,
    background = Background,
    surface = Surface,
    surfaceVariant = ElevatedSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = PrimaryBlue,
    tertiary = AccentPink,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = ElevatedSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderColorLight
)

@Composable
fun IptvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PremiumDarkColorScheme else PremiumLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
