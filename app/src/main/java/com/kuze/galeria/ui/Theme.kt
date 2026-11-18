package com.kuze.galeria.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Estilo Material 3 Expressive: paleta y fuente elegidas por el usuario,
 * formas muy redondeadas y modo claro u oscuro.
 */
@Composable
fun GaleriaTheme(
    darkTheme: Boolean = false,
    palette: String = "menta",
    font: String = "sans",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(palette, darkTheme),
        shapes = AppShapes,
        typography = typographyFor(font),
        content = content
    )
}
