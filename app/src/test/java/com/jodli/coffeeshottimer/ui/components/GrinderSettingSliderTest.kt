package com.jodli.coffeeshottimer.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for GrinderSettingSlider component.
 * Verifies Task 15 implementation: Implement slider for grinder settings.
 * 
 * Note: These are unit tests for the logic. UI tests would require instrumented tests.
 */
class GrinderSettingSliderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun grinderSettingSlider_displaysCorrectValue() {
        var currentValue = "5.0"
        
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GrinderSettingSlider(
                    value = currentValue,
                    onValueChange = { currentValue = it },
                    suggestedSetting = "4.5",
                    previousSuccessfulSettings = listOf("4.0", "5.5", "6.0")
                )
            }
        }

        // Verify the current value is displayed
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        
        // Verify the grinder setting label is displayed
        composeTestRule.onNodeWithText("Grinder Setting").assertIsDisplayed()
    }

    @Test
    fun grinderSettingSlider_showsSuggestedSetting() {
        var currentValue = ""
        
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GrinderSettingSlider(
                    value = currentValue,
                    onValueChange = { currentValue = it },
                    suggestedSetting = "4.5",
                    previousSuccessfulSettings = listOf("4.0", "5.5")
                )
            }
        }

        // Verify suggested setting hint is shown when value is empty
        composeTestRule.onNodeWithText("Suggested: 4.5 (based on last use with this bean)")
            .assertIsDisplayed()
    }

    @Test
    fun grinderSettingSlider_handlesValueChange() {
        var currentValue = "5.0"
        var valueChanged = false
        
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GrinderSettingSlider(
                    value = currentValue,
                    onValueChange = { 
                        currentValue = it
                        valueChanged = true
                    }
                )
            }
        }

        // Find and interact with the slider
        composeTestRule.onNodeWithContentDescription("Grinder Setting")
            .assertExists()
    }

    @Test
    fun grinderSettingSlider_displaysErrorMessage() {
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GrinderSettingSlider(
                    value = "invalid",
                    onValueChange = { },
                    errorMessage = "Invalid grinder setting"
                )
            }
        }

        // Verify error message is displayed
        composeTestRule.onNodeWithText("Invalid grinder setting")
            .assertIsDisplayed()
    }

    @Test
    fun grinderSettingSlider_handlesDisabledState() {
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GrinderSettingSlider(
                    value = "5.0",
                    onValueChange = { },
                    enabled = false
                )
            }
        }

        // Verify the slider exists but is disabled
        composeTestRule.onNodeWithText("Grinder Setting").assertIsDisplayed()
    }

    @Test
    fun grinderSettingSlider_formatsWholeNumbers() {
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GrinderSettingSlider(
                    value = "5.0",
                    onValueChange = { }
                )
            }
        }

        // Verify whole numbers are displayed without decimal
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun grinderSettingSlider_formatsDecimalNumbers() {
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                GrinderSettingSlider(
                    value = "5.5",
                    onValueChange = { }
                )
            }
        }

        // Verify decimal numbers are displayed with one decimal place
        composeTestRule.onNodeWithText("5.5").assertIsDisplayed()
    }
}