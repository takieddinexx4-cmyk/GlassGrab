package com.glassgrab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.glassgrab.core.design.theme.GlassGrabTheme
import com.glassgrab.feature.home.HomeScreen
import com.glassgrab.feature.home.HomeUiState
import org.junit.Rule
import org.junit.Test

class HomeScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysUrlInputAndEnablesAnalyzeWhenPopulated() {
        composeRule.setContent {
            GlassGrabTheme(supportsRealBlur = false) {
                HomeScreen(
                    state = HomeUiState(urlInput = "https://www.tiktok.com/@demo/video/123"),
                    onIntent = {},
                    onPasteClick = {},
                    onOpenAnalyzed = {},
                )
            }
        }

        composeRule.onNodeWithText("GlassGrab").assertIsDisplayed()
        composeRule.onNodeWithTag("url_input_field").assertIsDisplayed()
        composeRule.onNodeWithTag("analyze_button").assertIsEnabled()
    }
}
