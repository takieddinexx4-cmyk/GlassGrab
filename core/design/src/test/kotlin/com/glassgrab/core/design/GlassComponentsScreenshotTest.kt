package com.glassgrab.core.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.glassgrab.core.design.components.GlassBadge
import com.glassgrab.core.design.components.GlassCard
import com.glassgrab.core.design.components.WavyProgressIndicator
import com.glassgrab.core.design.theme.GlassGrabTheme
import com.glassgrab.core.design.theme.GlassThemeVariant
import com.glassgrab.core.design.tokens.GlassSpacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class GlassComponentsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureGlassCardDarkTheme() {
        composeRule.setContent {
            GlassGrabTheme(variant = GlassThemeVariant.DARK, supportsRealBlur = false) {
                GlassCard(modifier = Modifier.padding(GlassSpacing.md)) {
                    Column {
                        Text("GlassGrab 4K Preview")
                        GlassBadge("No Watermark")
                        WavyProgressIndicator(progress = 0.65f)
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
