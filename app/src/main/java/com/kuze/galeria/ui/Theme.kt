package com.kuze.galeria.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD7FF),
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF004C69),
    onPrimaryContainer = Color(0xFFC9EFFF),
    secondary = Color(0xFFF2C879),
    onSecondary = Color(0xFF3D2E00),
    background = Color(0xFF0D1012),
    onBackground = Color(0xFFE6EBEF),
    surface = Color(0xFF121619),
    onSurface = Color(0xFFE6EBEF),
    surfaceVariant = Color(0xFF242C31),
    onSurfaceVariant = Color(0xFFAEB8C0),
    outline = Color(0xFF3A444A),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3A0906)
)

/** Tema oscuro fijo, estilo Material 3. */
@Composable
fun GaleriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content
    )
}
