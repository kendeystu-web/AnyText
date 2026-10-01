package com.anytext.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Violet = Color(0xFF9B8CFF)
private val VioletDeep = Color(0xFF5B4BC4)
private val Cyan = Color(0xFF54D6E8)
private val Pink = Color(0xFFFF8FB8)

private val DarkScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color(0xFF1B1440),
    primaryContainer = Color(0xFF2B2554),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Cyan,
    onSecondary = Color(0xFF00323B),
    secondaryContainer = Color(0xFF0E3E48),
    onSecondaryContainer = Color(0xFFBFF0F8),
    tertiary = Pink,
    onTertiary = Color(0xFF3E1029),
    tertiaryContainer = Color(0xFF4A1F37),
    onTertiaryContainer = Color(0xFFFFD9E6),
    background = Color(0xFF0E1015),
    onBackground = Color(0xFFE7E9F0),
    surface = Color(0xFF151821),
    onSurface = Color(0xFFE7E9F0),
    surfaceVariant = Color(0xFF1D2130),
    onSurfaceVariant = Color(0xFF9AA3B8),
    outline = Color(0xFF39405A),
    outlineVariant = Color(0xFF2A3048),
    error = Color(0xFFFF6B81),
    onError = Color(0xFF3B0913),
    errorContainer = Color(0xFF4A1420),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightScheme = lightColorScheme(
    primary = VioletDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF1F1650),
    secondary = Color(0xFF0E8FA3),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBFF0F8),
    onSecondaryContainer = Color(0xFF06323B),
    tertiary = Color(0xFFB3446E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E6),
    onTertiaryContainer = Color(0xFF3E1029),
    background = Color(0xFFF6F5FB),
    onBackground = Color(0xFF1A1C24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C24),
    surfaceVariant = Color(0xFFECEAF6),
    onSurfaceVariant = Color(0xFF5A6172),
    outline = Color(0xFFC6C9D8),
    outlineVariant = Color(0xFFE0E2EC),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun AnyTextTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        content()
    }
}

fun fontFamilyOf(name: String): FontFamily = when (name) {
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    else -> FontFamily.Default
}
