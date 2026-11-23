package com.glassgrab.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glassgrab.core.design.components.GlassCard
import com.glassgrab.core.design.components.GlassPrimaryButton
import com.glassgrab.core.design.tokens.GlassSpacing
import com.glassgrab.core.domain.model.AppLanguage
import com.glassgrab.core.domain.model.DefaultQuality
import com.glassgrab.core.domain.model.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(
    isDebugBuild: Boolean,
    onOpenDesignGallery: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prefs = state.prefs

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GlassSpacing.md, vertical = GlassSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlassSpacing.md),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.sm)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dynamic Color (Android 12+)", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = prefs.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                }
                Text("Language", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = prefs.language == lang,
                            onClick = { viewModel.setLanguage(lang) },
                            label = { Text(lang.nativeLabel) },
                        )
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.sm)) {
                Text("Downloads", style = MaterialTheme.typography.titleMedium)
                Text("Default Quality", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    DefaultQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = prefs.defaultQuality == quality,
                            onClick = { viewModel.setDefaultQuality(quality) },
                            label = { Text(quality.label) },
                        )
                    }
                }
                Text("Parallel Downloads: ${prefs.maxParallelDownloads}", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = prefs.maxParallelDownloads.toFloat(),
                    onValueChange = { viewModel.setMaxParallelDownloads(it.roundToInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Wi-Fi Only", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = prefs.wifiOnly, onCheckedChange = viewModel::setWifiOnly)
                }
                Text(
                    text = "Save location: ${prefs.downloadFolderName} (Scoped MediaStore)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.sm)) {
                Text("Download Engine (yt-dlp + FFmpeg)", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Version: ${prefs.engineVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Auto-update daily (Stable)", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = prefs.autoUpdateEngine, onCheckedChange = viewModel::setAutoUpdateEngine)
                }
                GlassPrimaryButton(
                    text = if (state.isUpdatingEngine) "Updating..." else "Update Engine Now",
                    icon = Icons.Rounded.SystemUpdate,
                    enabled = !state.isUpdatingEngine,
                    onClick = viewModel::updateEngineNow,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.engineStatusMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (isDebugBuild) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    Text("Developer Tools", style = MaterialTheme.typography.titleMedium)
                    GlassPrimaryButton(
                        text = "Open Design System Gallery",
                        icon = Icons.Rounded.Palette,
                        onClick = onOpenDesignGallery,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xxs)) {
                Text("Privacy & Content Rights", style = MaterialTheme.typography.titleMedium)
                Text(
                    "GlassGrab has zero ads, zero analytics, and keeps all history on your device. Only download content you own or have permission to save. DRM-protected services are not supported.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}
