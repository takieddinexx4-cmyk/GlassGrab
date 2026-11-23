package com.glassgrab.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowWidthSizeClass
import com.glassgrab.app.navigation.GlassGrabRoute
import com.glassgrab.core.common.DeviceCapabilities
import com.glassgrab.core.common.Formatters
import com.glassgrab.core.design.components.AnimatedMeshBackground
import com.glassgrab.core.design.components.GlassFloatingBottomBar
import com.glassgrab.core.design.components.GlassNavItem
import com.glassgrab.core.design.gallery.DesignGalleryScreen
import com.glassgrab.core.design.theme.GlassGrabTheme
import com.glassgrab.core.design.theme.GlassThemeVariant
import com.glassgrab.core.design.theme.LocalGlassHazeState
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.ThemeMode
import com.glassgrab.core.domain.repository.PreferencesRepository
import com.glassgrab.feature.downloads.DownloadsRoute
import com.glassgrab.feature.downloads.DownloadsViewModel
import com.glassgrab.feature.formats.FormatPickerBottomSheet
import com.glassgrab.feature.home.HomeRoute
import com.glassgrab.feature.library.LibraryRoute
import com.glassgrab.feature.player.PlayerScreen
import com.glassgrab.feature.settings.SettingsRoute
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var deviceCapabilities: DeviceCapabilities

    private val incomingSharedUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)

        setContent {
            val prefs by preferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = com.glassgrab.core.domain.model.UserPreferences(),
            )
            val themeVariant = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> GlassThemeVariant.SYSTEM
                ThemeMode.DARK -> GlassThemeVariant.DARK
                ThemeMode.LIGHT -> GlassThemeVariant.LIGHT
                ThemeMode.AMOLED -> GlassThemeVariant.AMOLED
            }

            GlassGrabTheme(
                variant = themeVariant,
                dynamicColor = prefs.dynamicColor,
                supportsRealBlur = deviceCapabilities.supportsRealBlur(),
            ) {
                GlassGrabRootApp(
                    sharedUrl = incomingSharedUrl.value,
                    onSharedUrlConsumed = { incomingSharedUrl.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            incomingSharedUrl.value = Formatters.extractFirstUrl(sharedText) ?: sharedText
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GlassGrabRootApp(
    sharedUrl: String?,
    onSharedUrlConsumed: () -> Unit,
    downloadsViewModel: DownloadsViewModel = hiltViewModel(),
) {
    val backStack = rememberNavBackStack(GlassGrabRoute.Home)
    val activeDownloads by downloadsViewModel.activeDownloads.collectAsStateWithLifecycle()
    var activeFormatSheetMedia by remember { mutableStateOf<MediaItemInfo?>(null) }

    val hazeState = rememberHazeState()
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val useNavigationRail = widthClass != WindowWidthSizeClass.COMPACT

    val topLevelRoutes = remember {
        listOf(
            GlassGrabRoute.Home,
            GlassGrabRoute.Downloads,
            GlassGrabRoute.Library,
            GlassGrabRoute.Settings,
        )
    }

    val navItems = listOf(
        GlassNavItem("home", stringResource(R.string.nav_home), Icons.Rounded.Home),
        GlassNavItem("downloads", stringResource(R.string.nav_downloads), Icons.Rounded.Download, badgeCount = activeDownloads.size),
        GlassNavItem("library", stringResource(R.string.nav_library), Icons.Rounded.VideoLibrary),
        GlassNavItem("settings", stringResource(R.string.nav_settings), Icons.Rounded.Settings),
    )

    val currentRoute = backStack.lastOrNull()
    val selectedTabIndex = topLevelRoutes.indexOf(currentRoute).takeIf { it >= 0 } ?: 0
    val showChrome = currentRoute in topLevelRoutes

    fun switchTab(index: Int) {
        val target = topLevelRoutes[index]
        if (backStack.lastOrNull() != target) {
            backStack.clear()
            backStack.add(target)
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalGlassHazeState provides hazeState) {
            AnimatedMeshBackground(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavigationRail && showChrome) {
                        NavigationRail(modifier = Modifier.statusBarsPadding()) {
                            navItems.forEachIndexed { index, item ->
                                NavigationRailItem(
                                    selected = selectedTabIndex == index,
                                    onClick = { switchTab(index) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .hazeSource(hazeState),
                        ) {
                            NavDisplay(
                                backStack = backStack,
                                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                                entryProvider = entryProvider {
                                    entry<GlassGrabRoute.Home> {
                                        HomeRoute(
                                            sharedUrl = sharedUrl,
                                            onSharedUrlConsumed = onSharedUrlConsumed,
                                            onOpenFormats = { media -> activeFormatSheetMedia = media },
                                        )
                                    }
                                    entry<GlassGrabRoute.Downloads> {
                                        DownloadsRoute()
                                    }
                                    entry<GlassGrabRoute.Library> {
                                        LibraryRoute(
                                            onPlayMedia = { uri, title ->
                                                backStack.add(GlassGrabRoute.Player(uri, title))
                                            },
                                        )
                                    }
                                    entry<GlassGrabRoute.Settings> {
                                        SettingsRoute(
                                            isDebugBuild = BuildConfig.DEBUG,
                                            onOpenDesignGallery = { backStack.add(GlassGrabRoute.DesignGallery) },
                                        )
                                    }
                                    entry<GlassGrabRoute.Player> { key ->
                                        PlayerScreen(
                                            mediaUri = key.mediaUri,
                                            title = key.title,
                                            onBack = { backStack.removeLastOrNull() },
                                        )
                                    }
                                    entry<GlassGrabRoute.DesignGallery> {
                                        DesignGalleryScreen(
                                            onBack = { backStack.removeLastOrNull() },
                                        )
                                    }
                                },
                            )
                        }

                        if (!useNavigationRail && showChrome) {
                            GlassFloatingBottomBar(
                                items = navItems,
                                selectedIndex = selectedTabIndex,
                                onSelect = ::switchTab,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding(),
                            )
                        }
                    }
                }

                activeFormatSheetMedia?.let { media ->
                    FormatPickerBottomSheet(
                        media = media,
                        onDismiss = { activeFormatSheetMedia = null },
                        onStartDownload = { requests ->
                            downloadsViewModel.enqueue(requests)
                            activeFormatSheetMedia = null
                            switchTab(1)
                        },
                    )
                }
            }
        }
    }
}
