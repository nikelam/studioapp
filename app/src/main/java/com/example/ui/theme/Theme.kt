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

private val DarkColorScheme = darkColorScheme(
    primary = StudioAmberBright,
    onPrimary = Color(0xFF231600),
    primaryContainer = StudioAmberContainerDark,
    onPrimaryContainer = Color(0xFFFFDEA6),
    secondary = StudioCoral,
    onSecondary = Color(0xFF3E0012),
    secondaryContainer = StudioCoralContainerDark,
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = StudioEmerald,
    onTertiary = Color(0xFF002116),
    tertiaryContainer = StudioEmeraldContainerDark,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateTextMuted,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF272C3A),
    error = StudioCoral,
    onError = Color(0xFF3E0012)
)

private val LightColorScheme = lightColorScheme(
    primary = StudioAmberDeep,
    onPrimary = Color.White,
    primaryContainer = StudioAmberContainerLight,
    onPrimaryContainer = Color(0xFF451A03),
    secondary = StudioCoralDeep,
    onSecondary = Color.White,
    secondaryContainer = StudioCoralContainerLight,
    onSecondaryContainer = Color(0xFF4C0519),
    tertiary = Color(0xFF047857),
    onTertiary = Color.White,
    tertiaryContainer = StudioEmeraldContainerLight,
    onTertiaryContainer = Color(0xFF064E3B),
    background = WarmAlabasterBg,
    onBackground = CharcoalText,
    surface = WarmPaperSurface,
    onSurface = CharcoalText,
    surfaceVariant = WarmStoneVariant,
    onSurfaceVariant = CharcoalTextMuted,
    surfaceContainerHigh = WarmStoneVariant,
    outline = WarmOutline,
    outlineVariant = Color(0xFFE7E2D8),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudioFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
