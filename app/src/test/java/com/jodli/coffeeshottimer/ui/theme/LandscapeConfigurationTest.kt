package com.jodli.coffeeshottimer.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LandscapeConfigurationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `rememberLandscapeConfiguration detects portrait orientation correctly`() {
        val portraitConfig = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 360
            screenHeightDp = 640
        }

        var landscapeConfiguration: LandscapeConfiguration? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides portraitConfig) {
                landscapeConfiguration = rememberLandscapeConfiguration()
            }
        }

        composeTestRule.runOnIdle {
            landscapeConfiguration?.let { config ->
                assertFalse("Should not be landscape in portrait mode", config.isLandscape)
                assertEquals("Screen width should match", 360, config.screenWidthDp)
                assertEquals("Screen height should match", 640, config.screenHeightDp)
                assertEquals("Timer size should be portrait size", 200.dp, config.timerSize)
                assertEquals("Content spacing should be portrait spacing", 16.dp, config.contentSpacing)
            }
        }
    }

    @Test
    fun `rememberLandscapeConfiguration detects landscape orientation correctly`() {
        val landscapeConfig = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
            screenWidthDp = 640
            screenHeightDp = 360
        }

        var landscapeConfiguration: LandscapeConfiguration? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides landscapeConfig) {
                landscapeConfiguration = rememberLandscapeConfiguration()
            }
        }

        composeTestRule.runOnIdle {
            landscapeConfiguration?.let { config ->
                assertTrue("Should be landscape in landscape mode", config.isLandscape)
                assertEquals("Screen width should match", 640, config.screenWidthDp)
                assertEquals("Screen height should match", 360, config.screenHeightDp)
                assertEquals("Timer size should be landscape size", 160.dp, config.timerSize)
                assertEquals("Content spacing should be landscape spacing", 12.dp, config.contentSpacing)
            }
        }
    }

    @Test
    fun `Spacing landscapeTimerSize returns correct values`() {
        val spacing = Spacing()
        
        // Test portrait mode
        val portraitConfig = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
        }

        var portraitTimerSize: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides portraitConfig) {
                portraitTimerSize = spacing.landscapeTimerSize()
            }
        }

        composeTestRule.runOnIdle {
            assertEquals("Portrait timer size should be default", 200.dp, portraitTimerSize)
        }

        // Test landscape mode
        val landscapeConfig = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }

        var landscapeTimerSize: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides landscapeConfig) {
                landscapeTimerSize = spacing.landscapeTimerSize()
            }
        }

        composeTestRule.runOnIdle {
            assertEquals("Landscape timer size should be smaller", 160.dp, landscapeTimerSize)
        }
    }

    @Test
    fun `Spacing landscapeSpacing returns correct values`() {
        val spacing = Spacing()
        
        // Test portrait mode
        val portraitConfig = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
        }

        var portraitSpacing: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides portraitConfig) {
                portraitSpacing = spacing.landscapeSpacing()
            }
        }

        composeTestRule.runOnIdle {
            assertEquals("Portrait spacing should be medium", 16.dp, portraitSpacing)
        }

        // Test landscape mode
        val landscapeConfig = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }

        var landscapeSpacing: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides landscapeConfig) {
                landscapeSpacing = spacing.landscapeSpacing()
            }
        }

        composeTestRule.runOnIdle {
            assertEquals("Landscape spacing should be smaller", 12.dp, landscapeSpacing)
        }
    }

    @Test
    fun `LocalIsLandscape provides correct values in CoffeeShotTimerTheme`() {
        // Test portrait mode
        val portraitConfig = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
        }

        var isLandscapePortrait: Boolean? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides portraitConfig) {
                CoffeeShotTimerTheme {
                    isLandscapePortrait = LocalIsLandscape.current
                }
            }
        }

        composeTestRule.runOnIdle {
            assertFalse("LocalIsLandscape should be false in portrait", isLandscapePortrait ?: true)
        }

        // Test landscape mode
        val landscapeConfig = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }

        var isLandscapeLandscape: Boolean? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides landscapeConfig) {
                CoffeeShotTimerTheme {
                    isLandscapeLandscape = LocalIsLandscape.current
                }
            }
        }

        composeTestRule.runOnIdle {
            assertTrue("LocalIsLandscape should be true in landscape", isLandscapeLandscape ?: false)
        }
    }

    @Test
    fun `LandscapeConfiguration data class holds correct values`() {
        val config = LandscapeConfiguration(
            isLandscape = true,
            screenWidthDp = 800,
            screenHeightDp = 480,
            timerSize = 160.dp,
            contentSpacing = 12.dp
        )

        assertTrue("isLandscape should be true", config.isLandscape)
        assertEquals("screenWidthDp should match", 800, config.screenWidthDp)
        assertEquals("screenHeightDp should match", 480, config.screenHeightDp)
        assertEquals("timerSize should match", 160.dp, config.timerSize)
        assertEquals("contentSpacing should match", 12.dp, config.contentSpacing)
    }

    @Test
    fun `Spacing data class contains landscape-specific values`() {
        val spacing = Spacing()
        
        assertEquals("landscapeTimerSize should be 160dp", 160.dp, spacing.landscapeTimerSize)
        assertEquals("landscapeContentSpacing should be 12dp", 12.dp, spacing.landscapeContentSpacing)
    }
}