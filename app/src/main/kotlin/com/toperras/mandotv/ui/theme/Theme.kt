package com.toperras.mandotv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.toperras.mandotv.data.ThemePreference

/*
 * Identidad visual: base azul noche, superficies limpias y degradado
 * índigo → cian como acento. Contrastes altos para usarla con poca luz.
 */

/** Colores propios que Material 3 no tiene. */
@Immutable
data class BrandColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val surfaceRaised: Color,
    val warning: Color,
    val warningSoft: Color,
) {
    val gradient: Brush get() = Brush.linearGradient(listOf(gradientStart, gradientEnd))
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    background = Color(0xFFF3F5FA),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8ECF5),
    onSurfaceVariant = Color(0xFF5B6478),
    outlineVariant = Color(0xFFDCE1EC),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF0B0D10),
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF070B16),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1C2536),
    onSurfaceVariant = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF243047),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF3B1219),
)

private val LightBrand = BrandColors(
    gradientStart = Color(0xFF4F46E5),
    gradientEnd = Color(0xFF0891B2),
    surfaceRaised = Color(0xFFE8ECF5),
    warning = Color(0xFFB45309),
    warningSoft = Color(0xFFFEF3C7),
)

private val DarkBrand = BrandColors(
    gradientStart = Color(0xFF6366F1),
    gradientEnd = Color(0xFF06B6D4),
    surfaceRaised = Color(0xFF1C2536),
    warning = Color(0xFFFBBF24),
    warningSoft = Color(0xFF3A2A0A),
)

val LocalBrandColors = staticCompositionLocalOf { LightBrand }

@Composable
fun isDarkTheme(preference: ThemePreference): Boolean = when (preference) {
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}

@Composable
fun MandoTvTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBrandColors provides if (dark) DarkBrand else LightBrand) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            content = content,
        )
    }
}
