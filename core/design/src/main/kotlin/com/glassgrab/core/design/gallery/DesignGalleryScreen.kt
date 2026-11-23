package com.glassgrab.core.design.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.glassgrab.core.design.components.GlassBadge
import com.glassgrab.core.design.components.GlassCard
import com.glassgrab.core.design.components.GlassFloatingBottomBar
import com.glassgrab.core.design.components.GlassNavItem
import com.glassgrab.core.design.components.GlassPrimaryButton
import com.glassgrab.core.design.components.GlassShimmerSkeleton
import com.glassgrab.core.design.components.WavyProgressIndicator
import com.glassgrab.core.design.tokens.GlassColors
import com.glassgrab.core.design.tokens.GlassSpacing

/**
 * Debug component gallery displaying every GlassGrab design-system surface, token, and control.
 */
@Composable
fun DesignGalleryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val sampleNav = remember {
        listOf(
            GlassNavItem("home", "Home", Icons.Rounded.Home),
            GlassNavItem("downloads", "Queue", Icons.Rounded.Download, badgeCount = 2),
            GlassNavItem("library", "Library", Icons.Rounded.VideoLibrary),
            GlassNavItem("settings", "Settings", Icons.Rounded.Settings),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(GlassSpacing.md),
        verticalArrangement = Arrangement.spacedBy(GlassSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("GlassGrab Design Gallery", style = MaterialTheme.typography.headlineMedium)
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                Text("Frosted Glass Card", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Layered 24dp card with 1dp gradient border, inner glow, and Haze v2 blur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    GlassBadge("No Watermark", containerColor = GlassColors.EmeraldSuccess.copy(alpha = 0.22f), contentColor = GlassColors.EmeraldSuccess)
                    GlassBadge("4K 60fps")
                    GlassBadge("AV1")
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.sm)) {
                Text("Expressive Wavy Progress", style = MaterialTheme.typography.titleMedium)
                WavyProgressIndicator(progress = 0.68f)
                GlassPrimaryButton(text = "Primary Action", icon = Icons.Rounded.Download, onClick = {})
            }
        }

        Text("Shimmer Skeleton", style = MaterialTheme.typography.titleMedium)
        GlassShimmerSkeleton()

        Text("Floating Glass Bottom Bar", style = MaterialTheme.typography.titleMedium)
        GlassFloatingBottomBar(
            items = sampleNav,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
        )
        Spacer(modifier = Modifier.height(GlassSpacing.xl))
    }
}
