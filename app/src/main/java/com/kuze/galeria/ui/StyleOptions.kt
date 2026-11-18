package com.kuze.galeria.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Paletas de colores
// ---------------------------------------------------------------------------

data class PaletteInfo(val id: String, val label: String, val swatch: Color)

val paletteOptions = listOf(
    PaletteInfo("menta", "Menta", Color(0xFF1E6B4B)),
    PaletteInfo("oceano", "Océano", Color(0xFF00639B)),
    PaletteInfo("atardecer", "Atardecer", Color(0xFFA33D2E)),
    PaletteInfo("lavanda", "Lavanda", Color(0xFF6B4E9E)),
    PaletteInfo("bosque", "Bosque", Color(0xFF3F6212)),
    PaletteInfo("arena", "Arena", Color(0xFF7A5734))
)

private data class PaletteColors(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val background: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color
)

private val mentaColors = PaletteColors(
    primary = Color(0xFF1E6B4B),
    primaryContainer = Color(0xFFC7F0DA),
    onPrimaryContainer = Color(0xFF06331F),
    secondary = Color(0xFF3F6653),
    secondaryContainer = Color(0xFFDDEFE4),
    tertiary = Color(0xFF1D5D77),
    tertiaryContainer = Color(0xFFC4E8F8),
    background = Color(0xFFF7FAF8),
    surfaceVariant = Color(0xFFE9F1EC),
    onSurface = Color(0xFF14211A),
    onSurfaceVariant = Color(0xFF55665C),
    outline = Color(0xFFB9CBBF)
)

private val oceanoColors = PaletteColors(
    primary = Color(0xFF00639B),
    primaryContainer = Color(0xFFCFE5FF),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF4F616E),
    secondaryContainer = Color(0xFFD2E5F4),
    tertiary = Color(0xFF7B4E7D),
    tertiaryContainer = Color(0xFFFFD7F8),
    background = Color(0xFFF7F9FC),
    surfaceVariant = Color(0xFFE3E8EF),
    onSurface = Color(0xFF12161B),
    onSurfaceVariant = Color(0xFF556570),
    outline = Color(0xFFA9B8C4)
)

private val atardecerColors = PaletteColors(
    primary = Color(0xFFA33D2E),
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF3F0700),
    secondary = Color(0xFF77574E),
    secondaryContainer = Color(0xFFFFDBD1),
    tertiary = Color(0xFF6C5D2F),
    tertiaryContainer = Color(0xFFF5E1A7),
    background = Color(0xFFFCF9F7),
    surfaceVariant = Color(0xFFF1E4DF),
    onSurface = Color(0xFF201A18),
    onSurfaceVariant = Color(0xFF6B5A55),
    outline = Color(0xFFB8A9A4)
)

private val lavandaColors = PaletteColors(
    primary = Color(0xFF6B4E9E),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF25005A),
    secondary = Color(0xFF625B71),
    secondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFF7D5260),
    tertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFFFBF8FF),
    surfaceVariant = Color(0xFFEAE6F2),
    onSurface = Color(0xFF1C1B20),
    onSurfaceVariant = Color(0xFF5D5A66),
    outline = Color(0xFFB4B0BE)
)

private val bosqueColors = PaletteColors(
    primary = Color(0xFF3F6212),
    primaryContainer = Color(0xFFD7F0AC),
    onPrimaryContainer = Color(0xFF112000),
    secondary = Color(0xFF57624A),
    secondaryContainer = Color(0xFFDBE7C8),
    tertiary = Color(0xFF3D6473),
    tertiaryContainer = Color(0xFFC1E9F8),
    background = Color(0xFFF9FAF3),
    surfaceVariant = Color(0xFFE6E9DC),
    onSurface = Color(0xFF1A1C16),
    onSurfaceVariant = Color(0xFF5C6152),
    outline = Color(0xFFAEB49F)
)

private val arenaColors = PaletteColors(
    primary = Color(0xFF7A5734),
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF6F5B45),
    secondaryContainer = Color(0xFFFADCBE),
    tertiary = Color(0xFF56643A),
    tertiaryContainer = Color(0xFFD9E8B3),
    background = Color(0xFFFDF8F4),
    surfaceVariant = Color(0xFFEFE3D8),
    onSurface = Color(0xFF211A15),
    onSurfaceVariant = Color(0xFF6B5B4E),
    outline = Color(0xFFB9A99A)
)

private val palettes = mapOf(
    "menta" to mentaColors,
    "oceano" to oceanoColors,
    "atardecer" to atardecerColors,
    "lavanda" to lavandaColors,
    "bosque" to bosqueColors,
    "arena" to arenaColors
)

private fun lightScheme(p: PaletteColors) = lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.secondary,
    onSecondary = Color.White,
    secondaryContainer = p.secondaryContainer,
    onSecondaryContainer = p.onPrimaryContainer,
    tertiary = p.tertiary,
    onTertiary = Color.White,
    tertiaryContainer = p.tertiaryContainer,
    onTertiaryContainer = p.onPrimaryContainer,
    background = p.background,
    onBackground = p.onSurface,
    surface = Color.White,
    onSurface = p.onSurface,
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = p.onSurfaceVariant,
    outline = p.outline,
    error = Color(0xFFB3261E),
    onError = Color.White
)

/** El modo oscuro se deriva de la misma paleta para que todas se vean coherentes. */
private fun darkScheme(p: PaletteColors) = darkColorScheme(
    primary = lerp(p.primary, Color.White, 0.55f),
    onPrimary = lerp(p.primary, Color.Black, 0.85f),
    primaryContainer = lerp(p.primary, Color.Black, 0.58f),
    onPrimaryContainer = lerp(p.primary, Color.White, 0.86f),
    secondary = lerp(p.secondary, Color.White, 0.45f),
    onSecondary = lerp(p.secondary, Color.Black, 0.85f),
    secondaryContainer = lerp(p.secondary, Color.Black, 0.55f),
    onSecondaryContainer = lerp(p.secondary, Color.White, 0.86f),
    tertiary = lerp(p.tertiary, Color.White, 0.45f),
    onTertiary = lerp(p.tertiary, Color.Black, 0.85f),
    tertiaryContainer = lerp(p.tertiary, Color.Black, 0.55f),
    onTertiaryContainer = lerp(p.tertiary, Color.White, 0.86f),
    background = lerp(p.background, Color.Black, 0.93f),
    onBackground = lerp(p.background, Color.White, 0.90f),
    surface = lerp(p.background, Color.Black, 0.87f),
    onSurface = lerp(p.background, Color.White, 0.90f),
    surfaceVariant = lerp(p.surfaceVariant, Color.Black, 0.80f),
    onSurfaceVariant = lerp(p.onSurfaceVariant, Color.White, 0.60f),
    outline = lerp(p.outline, Color.Black, 0.55f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

fun colorSchemeFor(paletteId: String, dark: Boolean): ColorScheme {
    val p = palettes[paletteId] ?: mentaColors
    return if (dark) darkScheme(p) else lightScheme(p)
}

// ---------------------------------------------------------------------------
// Fuentes
// ---------------------------------------------------------------------------

enum class FontOption(val id: String, val label: String) {
    SANS("sans", "Predeterminada"),
    SERIF("serif", "Con serifas"),
    MONO("mono", "Monoespaciada"),
    CURSIVE("cursive", "Manuscrita"),
    LARGE("grande", "Grande y clara")
}

val fontOptions: List<FontOption> = FontOption.entries

fun FontOption.family(): FontFamily = when (this) {
    FontOption.SERIF -> FontFamily.Serif
    FontOption.MONO -> FontFamily.Monospace
    FontOption.CURSIVE -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}

private fun TextStyle.withFont(family: FontFamily, scale: Float): TextStyle {
    val withFamily = copy(fontFamily = family)
    return if (scale == 1f) withFamily else withFamily.copy(
        fontSize = withFamily.fontSize * scale,
        lineHeight = withFamily.lineHeight * scale
    )
}

fun typographyFor(fontId: String): Typography {
    val font = fontOptions.firstOrNull { it.id == fontId } ?: FontOption.SANS
    val family = font.family()
    val scale = if (font == FontOption.LARGE) 1.14f else 1f
    val base = Typography()
    val any = { style: TextStyle -> style.withFont(family, scale) }

    return Typography(
        displayLarge = any(base.displayLarge),
        displayMedium = any(base.displayMedium),
        displaySmall = any(base.displaySmall),
        headlineLarge = any(base.headlineLarge).copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp
        ),
        headlineMedium = any(base.headlineMedium).copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        ),
        headlineSmall = any(base.headlineSmall),
        titleLarge = any(base.titleLarge).copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = any(base.titleMedium),
        titleSmall = any(base.titleSmall).copy(fontWeight = FontWeight.Bold),
        bodyLarge = any(base.bodyLarge),
        bodyMedium = any(base.bodyMedium),
        bodySmall = any(base.bodySmall),
        labelLarge = any(base.labelLarge).copy(fontWeight = FontWeight.SemiBold),
        labelMedium = any(base.labelMedium),
        labelSmall = any(base.labelSmall)
    )
}

// ---------------------------------------------------------------------------
// Tamaño de las miniaturas según las columnas
// ---------------------------------------------------------------------------

/** Ancho real (px) de la miniatura que se pide al servidor. */
fun thumbWidthFor(columns: Int): Int = when (columns) {
    2 -> 480
    4 -> 240
    else -> 320
}

/** Peso aproximado de cada miniatura en KB. */
fun approxKbFor(columns: Int): Int = when (columns) {
    2 -> 35
    4 -> 12
    else -> 18
}
