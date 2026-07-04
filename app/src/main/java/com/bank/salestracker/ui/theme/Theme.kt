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

/* Палитра: глубокий чернильный + электрик-бирюза + тёплый янтарь для акцентов успеха */
val Ink = Color(0xFF101A2B)        // основной тёмный
val Ocean = Color(0xFF1E4FFF)      // primary — насыщенный синий
val Mint = Color(0xFF00C9A7)       // secondary — рост, успех
val Amber = Color(0xFFFFB547)      // tertiary — награды, медали
val Mist = Color(0xFFF4F6FB)       // фон
val CardWhite = Color(0xFFFFFFFF)

/* Градиент для hero-карточки дашборда и шапки логина */
val HeroGradient = Brush.linearGradient(listOf(Color(0xFF1E4FFF), Color(0xFF13B8E0), Color(0xFF00C9A7)))

private val LightColors = lightColorScheme(
    primary = Ocean,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EAFF),
    onPrimaryContainer = Ink,
    secondary = Mint,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFD6F7EF),
    tertiary = Amber,
    background = Mist,
    surface = CardWhite,
    surfaceVariant = Color(0xFFEDF1F9),
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF5B6679),
    outline = Color(0xFF9AA5B8),
    error = Color(0xFFE5484D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DA0FF),
    secondary = Color(0xFF4FE0C4),
    tertiary = Amber,
    background = Color(0xFF0B1220),
    surface = Color(0xFF141D30),
    surfaceVariant = Color(0xFF1C2740),
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
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Typography().labelMedium.copy(letterSpacing = 0.8.sp)
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
