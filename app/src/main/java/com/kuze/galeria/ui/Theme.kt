package com.kuze.galeria.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6B4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7F0DA),
    onPrimaryContainer = Color(0xFF06331F),
    secondary = Color(0xFF3F6653),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDEFE4),
    onSecondaryContainer = Color(0xFF15291F),
    tertiary = Color(0xFF1D5D77),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4E8F8),
    onTertiaryContainer = Color(0xFF032A3A),
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF14211A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14211A),
    surfaceVariant = Color(0xFFE9F1EC),
    onSurfaceVariant = Color(0xFF55665C),
    outline = Color(0xFFB9CBBF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AE3B4),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF16523A),
    onPrimaryContainer = Color(0xFFBDF2D7),
    secondary = Color(0xFFB4D4C1),
    onSecondary = Color(0xFF1E3529),
    secondaryContainer = Color(0xFF2A4033),
    onSecondaryContainer = Color(0xFFD0EBDC),
    tertiary = Color(0xFF9FD3EF),
    onTertiary = Color(0xFF00344A),
    tertiaryContainer = Color(0xFF15485F),
    onTertiaryContainer = Color(0xFFC9EFFF),
    background = Color(0xFF0C1210),
    onBackground = Color(0xFFE4ECE6),
    surface = Color(0xFF131C18),
    onSurface = Color(0xFFE4ECE6),
    surfaceVariant = Color(0xFF1E2A24),
    onSurfaceVariant = Color(0xFFA9BCB1),
    outline = Color(0xFF44554B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val baseTypography = Typography()

private val AppTypography = baseTypography.copy(
    headlineLarge = baseTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = baseTypography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = baseTypography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = baseTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
    labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

/** Estilo Material 3 Expressive: claro por defecto, con verde menta y formas muy redondeadas. */
@Composable
fun GaleriaTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
