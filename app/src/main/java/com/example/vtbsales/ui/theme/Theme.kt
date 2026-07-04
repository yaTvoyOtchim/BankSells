package com.example.vtbsales.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

val VtbBlue = Color(0xFF0065FF)
val VtbBlueDark = Color(0xFF003A9B)
val VtbBlueDeep = Color(0xFF061A40)
val VtbCyan = Color(0xFF00B7D8)
val VtbMint = Color(0xFFE2FAF5)
val VtbSurface = Color(0xFFF3F6FB)
val VtbCard = Color(0xFFFFFFFF)
val VtbText = Color(0xFF141827)
val VtbMuted = Color(0xFF848AA0)
val VtbLine = Color(0xFFE4E8F2)
val VtbAmber = Color(0xFFFFB13B)
val VtbDanger = Color(0xFFF05268)
val VtbSuccess = Color(0xFF1FB573)

private val VtbColorScheme = lightColorScheme(
    primary = VtbBlue,
    onPrimary = Color.White,
    secondary = VtbCyan,
    onSecondary = Color.White,
    background = VtbSurface,
    onBackground = VtbText,
    surface = VtbCard,
    onSurface = VtbText,
    surfaceVariant = Color(0xFFEAF0FA),
    onSurfaceVariant = VtbMuted,
    outline = VtbLine
)

private val VtbTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 31.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 17.sp
    )
)

private val VtbShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun VtbSalesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VtbColorScheme,
        typography = VtbTypography,
        shapes = VtbShapes,
        content = content
    )
}
