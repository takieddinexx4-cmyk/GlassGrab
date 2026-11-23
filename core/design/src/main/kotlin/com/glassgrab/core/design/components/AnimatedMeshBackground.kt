package com.glassgrab.core.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.glassgrab.core.design.theme.LocalGlassVisualConfig
import com.glassgrab.core.design.tokens.GlassColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated ambient gradient mesh background.
 * Automatically pauses when the lifecycle is paused, in battery saver, or with reduced motion enabled.
 */
@Composable
fun AnimatedMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val config = LocalGlassVisualConfig.current
    var isResumed by remember { mutableStateOf(true) }

    LifecycleResumeEffect(Unit) {
        isResumed = true
        onPauseOrDispose { isResumed = false }
    }

    val shouldAnimate = isResumed && !config.reducedMotion && !config.isAmoled
    val phase = if (shouldAnimate) {
        val transition = rememberInfiniteTransition(label = "mesh_transition")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "mesh_phase",
        )
        animated
    } else {
        0.8f
    }

    val orb1 = if (config.isDark) GlassColors.DarkMeshOrb1 else GlassColors.LightMeshOrb1
    val orb2 = if (config.isDark) GlassColors.DarkMeshOrb2 else GlassColors.LightMeshOrb2
    val orb3 = if (config.isDark) GlassColors.DarkMeshOrb3 else GlassColors.LightMeshOrb3
    val baseBg = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg),
    ) {
        if (!config.isAmoled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val radius = maxOf(w, h) * 0.65f

                val c1 = Offset(w * (0.25f + 0.12f * cos(phase)), h * (0.22f + 0.08f * sin(phase)))
                val c2 = Offset(w * (0.80f + 0.10f * sin(phase)), h * (0.48f + 0.10f * cos(phase)))
                val c3 = Offset(w * (0.35f + 0.14f * sin(phase * 0.7f)), h * (0.85f + 0.08f * cos(phase * 0.7f)))

                drawCircle(
                    brush = Brush.radialGradient(listOf(orb1.copy(alpha = 0.7f), Color.Transparent), center = c1, radius = radius),
                    radius = radius,
                    center = c1,
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(orb2.copy(alpha = 0.65f), Color.Transparent), center = c2, radius = radius),
                    radius = radius,
                    center = c2,
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(orb3.copy(alpha = 0.6f), Color.Transparent), center = c3, radius = radius),
                    radius = radius,
                    center = c3,
                )
            }
        }
        content()
    }
}
