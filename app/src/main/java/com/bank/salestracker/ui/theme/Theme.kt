package com.bank.salestracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Палитра: ВТБ-синий + liquid glass светлая/темная база */
val VtbBlue = Color(0xFF1E4FFF)
val VtbCyan = Color(0xFF13B8E0)
val VtbMint = Color(0xFF00C9A7)
val DeepNavy = Color(0xFF071426)
val Ink = Color(0xFF101A2B)
val Ocean = VtbBlue
val Mint = VtbMint
val Amber = Color(0xFFFFB547)
val Mist = Color(0xFFEFF6FF)
val CardWhite = Color(0xFFFFFFFF)

/* Градиент для hero-карточки дашборда и шапки логина */
val HeroGradient = Brush.linearGradient(listOf(VtbBlue, VtbCyan, VtbMint))

private val LightColors = lightColorScheme(
    primary = Ocean,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EAFF),
    onPrimaryContainer = Ink,
    secondary = Mint,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFD6F7EF),
    tertiary = Amber,
    background = Color(0xFFF3F8FF),
    surface = Color(0xF2FFFFFF),
    surfaceVariant = Color(0xFFE7F0FC),
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF5B6679),
    outline = Color(0xFF9AA5B8),
    error = Color(0xFFE5484D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DA0FF),
    secondary = Color(0xFF4FE0C4),
    tertiary = Amber,
    background = Color(0xFF020918),
    surface = Color(0xFF0F1B34),
    surfaceVariant = Color(0xFF17284A),
    onSurface = Color(0xFFE7ECF5),
    onSurfaceVariant = Color(0xFF9AA5B8)
)

/* Крупные мягкие скругления — современный «карточный» вид */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val AppTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Typography().labelMedium.copy(letterSpacing = 0.sp)
)

@Composable
fun BankTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
