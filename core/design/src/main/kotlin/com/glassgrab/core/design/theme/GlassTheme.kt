package com.glassgrab.core.design.theme

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.glassgrab.core.design.tokens.GlassColors
import com.glassgrab.core.design.tokens.GlassShapes
import com.glassgrab.core.design.tokens.GlassTypography
import dev.chrisbanes.haze.HazeState

@Immutable
data class GlassVisualConfig(
    val isDark: Boolean,
    val isAmoled: Boolean,
    val blurEnabled: Boolean,
    val reducedMotion: Boolean,
    val glassTint: Color,
    val glassFallback: Color,
    val borderStart: Color,
    val borderEnd: Color,
    val innerGlow: Color,
)

val LocalGlassVisualConfig = staticCompositionLocalOf {
    GlassVisualConfig(
        isDark = true,
        isAmoled = false,
        blurEnabled = true,
        reducedMotion = false,
        glassTint = GlassColors.DarkGlassTint,
        glassFallback = GlassColors.DarkGlassFallback,
        borderStart = GlassColors.DarkBorderStart,
        borderEnd = GlassColors.DarkBorderEnd,
        innerGlow = GlassColors.DarkInnerGlow,
    )
}

val LocalGlassHazeState = staticCompositionLocalOf<HazeState?> { null }

private val DarkGlassColorScheme = darkColorScheme(
    primary = GlassColors.CyanAccent,
    onPrimary = Color(0xFF002028),
    primaryContainer = Color(0xFF0E394C),
    onPrimaryContainer = Color(0xFFC5F3FF),
    secondary = GlassColors.IndigoAccent,
    onSecondary = Color(0xFF0E1642),
    tertiary = GlassColors.VioletAccent,
    background = GlassColors.DarkBackground,
    onBackground = Color(0xFFF0F4FF),
    surface = GlassColors.DarkSurface,
    onSurface = Color(0xFFF0F4FF),
    surfaceVariant = Color(0xFF1A2338),
    onSurfaceVariant = Color(0xFFB5C2DC),
    error = GlassColors.CoralError,
    onError = Color(0xFF3A0710),
)

private val AmoledGlassColorScheme = DarkGlassColorScheme.copy(
    background = GlassColors.AmoledBackground,
    surface = GlassColors.AmoledSurface,
)

private val LightGlassColorScheme = lightColorScheme(
    primary = GlassColors.LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = GlassColors.LightSecondary,
    onSecondary = Color.White,
    tertiary = GlassColors.LightTertiary,
    background = GlassColors.LightBackground,
    onBackground = Color(0xFF101828),
    surface = GlassColors.LightSurface,
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFE4ECF7),
    onSurfaceVariant = Color(0xFF475467),
    error = Color(0xFFC62839),
    onError = Color.White,
)

enum class GlassThemeVariant {
    SYSTEM,
    DARK,
    LIGHT,
    AMOLED,
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlassGrabTheme(
    variant: GlassThemeVariant = GlassThemeVariant.DARK,
    dynamicColor: Boolean = false,
    supportsRealBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (variant) {
        GlassThemeVariant.SYSTEM -> systemDark
        GlassThemeVariant.DARK, GlassThemeVariant.AMOLED -> true
        GlassThemeVariant.LIGHT -> false
    }
    val isAmoled = variant == GlassThemeVariant.AMOLED

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) {
                val dynamic = dynamicDarkColorScheme(context)
                if (isAmoled) dynamic.copy(background = Color.Black, surface = GlassColors.AmoledSurface) else dynamic
            } else {
                dynamicLightColorScheme(context)
            }
        }
        isAmoled -> AmoledGlassColorScheme
        isDark -> DarkGlassColorScheme
        else -> LightGlassColorScheme
    }

    val reducedMotion = isReducedMotionEnabled(context)
    val visualConfig = GlassVisualConfig(
        isDark = isDark,
        isAmoled = isAmoled,
        blurEnabled = supportsRealBlur && !reducedMotion,
        reducedMotion = reducedMotion,
        glassTint = when {
            isAmoled -> GlassColors.AmoledGlassTint
            isDark -> GlassColors.DarkGlassTint
            else -> GlassColors.LightGlassTint
        },
        glassFallback = if (isDark) GlassColors.DarkGlassFallback else GlassColors.LightGlassFallback,
        borderStart = if (isDark) GlassColors.DarkBorderStart else GlassColors.LightBorderStart,
        borderEnd = if (isDark) GlassColors.DarkBorderEnd else GlassColors.LightBorderEnd,
        innerGlow = if (isDark) GlassColors.DarkInnerGlow else GlassColors.LightInnerGlow,
    )

    CompositionLocalProvider(LocalGlassVisualConfig provides visualConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GlassTypography,
            shapes = GlassShapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

private fun isReducedMotionEnabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)
