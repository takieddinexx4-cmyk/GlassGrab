package com.glassgrab.core.design.tokens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single source of truth for all GlassGrab design tokens.
 * No feature module defines raw colors, corner radii, blur radii, or motion curves.
 */
object GlassColors {
    // Dark Glass Palette (Default)
    val DarkBackground = Color(0xFF070A12)
    val DarkSurface = Color(0xFF0E1422)
    val DarkGlassTint = Color(0xB3121A2D)
    val DarkGlassFallback = Color(0xE6141C30)
    val DarkBorderStart = Color(0x52FFFFFF)
    val DarkBorderEnd = Color(0x14FFFFFF)
    val DarkInnerGlow = Color(0x1E7AE7FF)

    // AMOLED Pure Black
    val AmoledBackground = Color(0xFF000000)
    val AmoledSurface = Color(0xFF07090F)
    val AmoledGlassTint = Color(0xBF0A0E18)

    // Light Glass Palette
    val LightBackground = Color(0xFFF3F6FC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightGlassTint = Color(0xB8FFFFFF)
    val LightGlassFallback = Color(0xEBFFFFFF)
    val LightBorderStart = Color(0xA6FFFFFF)
    val LightBorderEnd = Color(0x2E7A93B8)
    val LightInnerGlow = Color(0x240072E5)

    // Brand Accents (WCAG AA checked against surfaces)
    val CyanAccent = Color(0xFF5CE1FF)
    val IndigoAccent = Color(0xFF7B8BFF)
    val VioletAccent = Color(0xFFB388FF)
    val EmeraldSuccess = Color(0xFF2EE59D)
    val AmberWarning = Color(0xFFFFC857)
    val CoralError = Color(0xFFFF6B7A)

    val LightPrimary = Color(0xFF0B57D0)
    val LightSecondary = Color(0xFF5346D6)
    val LightTertiary = Color(0xFF006A78)

    // Mesh Gradient Orbs
    val DarkMeshOrb1 = Color(0xFF1A3A7A)
    val DarkMeshOrb2 = Color(0xFF3E1F6D)
    val DarkMeshOrb3 = Color(0xFF0D4E63)
    val LightMeshOrb1 = Color(0xFFC5DCFF)
    val LightMeshOrb2 = Color(0xFFE0D2FF)
    val LightMeshOrb3 = Color(0xFFC4F0F7)
}

object GlassSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val minTouchTarget: Dp = 48.dp
}

object GlassBlurLevels {
    val subtle: Dp = 14.dp
    val medium: Dp = 22.dp
    val heavy: Dp = 30.dp
}

object GlassRadii {
    val card: Dp = 24.dp
    val sheet: Dp = 32.dp
    val pill: Dp = 28.dp
    val badge: Dp = 12.dp
}

val GlassShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(GlassRadii.card),
    large = RoundedCornerShape(GlassRadii.pill),
    extraLarge = RoundedCornerShape(GlassRadii.sheet),
)

object GlassMotion {
    fun <T> expressiveSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> snappySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}

val GlassTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
