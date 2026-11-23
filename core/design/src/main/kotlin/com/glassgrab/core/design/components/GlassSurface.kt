package com.glassgrab.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glassgrab.core.design.theme.LocalGlassHazeState
import com.glassgrab.core.design.theme.LocalGlassVisualConfig
import com.glassgrab.core.design.tokens.GlassBlurLevels
import com.glassgrab.core.design.tokens.GlassRadii
import com.glassgrab.core.design.tokens.GlassSpacing
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Core frosted glass card using Haze v2 real backdrop blur when supported,
 * with a clean translucent gradient fallback on low-end or pre-Android 12 devices.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassRadii.card),
    blurRadius: Dp = GlassBlurLevels.medium,
    contentPadding: Dp = GlassSpacing.md,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val config = LocalGlassVisualConfig.current
    val hazeState = LocalGlassHazeState.current

    val blurStyle = remember(blurRadius) {
        HazeBlurStyle {
            blurRadius(blurRadius)
        }
    }

    val borderBrush = remember(config.borderStart, config.borderEnd) {
        Brush.linearGradient(listOf(config.borderStart, config.borderEnd))
    }

    val surfaceBrush = remember(config.glassTint, config.glassFallback, config.innerGlow, config.blurEnabled) {
        val base = if (config.blurEnabled && hazeState != null) config.glassTint else config.glassFallback
        Brush.verticalGradient(listOf(config.innerGlow, base, base))
    }

    var surfaceModifier = modifier.clip(shape)
    if (config.blurEnabled && hazeState != null) {
        surfaceModifier = surfaceModifier.hazeBlur(
            input = HazeInput.Sources(hazeState),
            style = blurStyle,
        )
    }
    surfaceModifier = surfaceModifier
        .background(surfaceBrush)
        .border(width = 1.dp, brush = borderBrush, shape = shape)

    if (onClick != null) {
        surfaceModifier = surfaceModifier
            .defaultMinSize(minWidth = GlassSpacing.minTouchTarget, minHeight = GlassSpacing.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
    }

    Box(
        modifier = surfaceModifier.padding(contentPadding),
        content = content,
    )
}
