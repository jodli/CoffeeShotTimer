package com.jodli.coffeeshottimer.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for IntroductionScreen navigation and interactions
 * Tests Requirements: 1.4, 1.7
 */
@RunWith(AndroidJUnit4::class)
class IntroductionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun introductionScreen_displaysCorrectly() {
        // Arrange & Act
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                IntroductionScreen(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }

        // Assert - Verify initial screen elements
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome to Coffee Shot Timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun introductionScreen_skipButtonCallsCallback() {
        // Arrange
        var skipCalled = false

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                IntroductionScreen(
                    onComplete = {},
                    onSkip = { skipCalled = true }
                )
            }
        }

        // Act
        composeTestRule.onNodeWithText("Skip").performClick()

        // Assert
        assert(skipCalled) { "Skip callback should be called when skip button is clicked" }
    }

    @Test
    fun introductionScreen_nextButtonNavigatesToSecondSlide() {
        // Arrange
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                IntroductionScreen(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }

        // Act
        composeTestRule.onNodeWithText("Next").performClick()

        // Assert - Should show second slide content
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Main Features").assertIsDisplayed()
    }

    @Test
    fun introductionScreen_getStartedButtonAppearsOnLastSlide() {
        // Arrange
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                IntroductionScreen(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }

        // Act - Navigate through all slides to the last one
        repeat(4) {
            composeTestRule.onNodeWithText("Next").performClick()
            composeTestRule.waitForIdle()
        }

        // Assert
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
    }

    @Test
    fun introductionScreen_getStartedButtonCallsOnComplete() {
        // Arrange
        var completeCalled = false

        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                IntroductionScreen(
                    onComplete = { completeCalled = true },
                    onSkip = {}
                )
            }
        }

        // Act - Navigate to last slide and click Get Started
        repeat(4) {
            composeTestRule.onNodeWithText("Next").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithText("Get Started").performClick()

        // Assert
        assert(completeCalled) { "Complete callback should be called when Get Started is clicked" }
    }

    @Test
    fun introductionScreen_allSlidesHaveCorrectContent() {
        // Arrange
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                IntroductionScreen(
                    onComplete = {},
                    onSkip = {}
                )
            }
        }

        // Test slide 1
        composeTestRule.onNodeWithText("Welcome to Coffee Shot Timer").assertIsDisplayed()

        // Navigate to slide 2
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Main Features").assertIsDisplayed()

        // Navigate to slide 3
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Flexible Workflow").assertIsDisplayed()

        // Navigate to slide 4
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Smart Timer").assertIsDisplayed()

        // Navigate to slide 5
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ready to Begin").assertIsDisplayed()
    }

    @Test
    fun introductionScreen_errorHandlingDisplaysErrorCard() {
        // Arrange
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                IntroductionScreen(
                    onComplete = { throw RuntimeException("Test error") },
                    onSkip = {}
                )
            }
        }

        // Act - Navigate to last slide and trigger error
        repeat(4) {
            composeTestRule.onNodeWithText("Next").performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithText("Get Started").performClick()

        // Assert - Error should be displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Navigation Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed()
    }
}
