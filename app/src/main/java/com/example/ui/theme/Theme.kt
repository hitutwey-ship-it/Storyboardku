package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.TextScaleOption
import com.example.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = Color(0xFF4A1607),
    secondary = CinemaNavy,
    onSecondary = Color.White,
    secondaryContainer = CinemaNavyContainerLight,
    onSecondaryContainer = CinemaNavy,
    tertiary = AmberAccent,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF4A2B00),
    background = WarmPaperBg,
    onBackground = InkDark,
    surface = WarmPaperSurface,
    onSurface = InkDark,
    surfaceVariant = WarmPaperSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = WarmOutline,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = CoralPrimaryDark,
    onPrimary = Color(0xFF3E1205),
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFB5C7E0),
    onSecondary = CinemaNavy,
    secondaryContainer = CinemaNavyContainerDark,
    onSecondaryContainer = Color(0xFFD9E5F7),
    tertiary = Color(0xFFFFB963),
    onTertiary = Color(0xFF3D2200),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFFDDB5),
    background = DarkStudioBg,
    onBackground = LightInk,
    surface = DarkStudioSurface,
    onSurface = LightInk,
    surfaceVariant = DarkStudioSurfaceVariant,
    onSurfaceVariant = LightInkMuted,
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = CrimsonContainerDark,
    onErrorContainer = Color(0xFFFFDAD6)
)

val StoryboardShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun StoryboardKuTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    textScale: TextScaleOption = TextScaleOption.MEDIUM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = buildStoryboardTypography(textScale.multiplier),
        shapes = StoryboardShapes,
        content = content
    )
}
