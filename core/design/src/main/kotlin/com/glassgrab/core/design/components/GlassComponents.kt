package com.glassgrab.core.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glassgrab.core.design.theme.LocalGlassVisualConfig
import com.glassgrab.core.design.tokens.GlassBlurLevels
import com.glassgrab.core.design.tokens.GlassMotion
import com.glassgrab.core.design.tokens.GlassRadii
import com.glassgrab.core.design.tokens.GlassSpacing
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GlassRadii.badge))
            .background(containerColor)
            .padding(horizontal = GlassSpacing.xs, vertical = GlassSpacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(GlassRadii.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier.defaultMinSize(minHeight = GlassSpacing.minTouchTarget),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(GlassSpacing.xs))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Expressive wavy progress indicator for active downloads.
 */
@Composable
fun WavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
) {
    val config = LocalGlassVisualConfig.current
    val phase = if (!config.reducedMotion) {
        val transition = rememberInfiniteTransition(label = "wavy_progress")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wave_phase",
        )
        value
    } else {
        0f
    }

    val clamped = progress.coerceIn(0f, 1f)
    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val midY = size.height / 2f
        val strokeWidth = 4.dp.toPx()
        val amplitude = if (config.reducedMotion) 0f else 3.dp.toPx()
        val wavelength = 24.dp.toPx()

        // Background track
        drawLine(
            color = trackColor,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        val endX = size.width * clamped
        if (endX > 2f) {
            path.reset()
            var x = 0f
            val step = 3f
            while (x <= endX) {
                val y = midY + amplitude * sin((x / wavelength) * 2f * PI.toFloat() - phase)
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += step
            }
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun GlassShimmerSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
) {
    val config = LocalGlassVisualConfig.current
    val shimmerOffset = if (!config.reducedMotion) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val value by transition.animateFloat(
            initialValue = -300f,
            targetValue = 900f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer_x",
        )
        value
    } else {
        200f
    }

    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 260f, 180f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(GlassRadii.card))
            .background(brush),
    )
}

data class GlassNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

/**
 * Floating frosted-glass bottom bar with a fluid spring-animated sliding pill indicator.
 */
@Composable
fun GlassFloatingBottomBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlassSpacing.md, vertical = GlassSpacing.sm),
        shape = RoundedCornerShape(GlassRadii.sheet),
        blurRadius = GlassBlurLevels.heavy,
        contentPadding = GlassSpacing.xs,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            val slotWidth = maxWidth / items.size.coerceAtLeast(1)
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = GlassMotion.expressiveSpring(),
                label = "nav_indicator_offset",
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(GlassRadii.pill))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(GlassRadii.pill),
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(
                        modifier = Modifier
                            .width(slotWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(GlassRadii.pill))
                            .semantics {
                                selected = isSelected
                                contentDescription = item.label
                            }
                            .clickable(role = Role.Tab) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(index)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (item.badgeCount > 0) "${item.label} (${item.badgeCount})" else item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
