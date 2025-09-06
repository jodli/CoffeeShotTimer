package com.jodli.coffeeshottimer.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import com.jodli.coffeeshottimer.ui.theme.CoffeeShotTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for TasteQuickPick component.
 * Tests UI interactions, state management, and accessibility.
 */
@RunWith(AndroidJUnit4::class)
class TasteQuickPickTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tasteQuickPick_displaysAllPrimaryTasteButtons() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Sour").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfect").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bitter").assertIsDisplayed()
    }

    @Test
    fun tasteQuickPick_displaysCorrectEmojis() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {}
                )
            }
        }

        // Then - emojis should be present
        composeTestRule.onNodeWithText("😖").assertIsDisplayed() // Sour
        composeTestRule.onNodeWithText("😊").assertIsDisplayed() // Perfect
        composeTestRule.onNodeWithText("😣").assertIsDisplayed() // Bitter
    }

    @Test
    fun tasteQuickPick_withRecommendation_highlightsRecommendedButton() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    recommended = TastePrimary.PERFECT,
                    onSelectPrimary = {}
                )
            }
        }

        // Then - Perfect button should have accessibility text indicating it's recommended
        composeTestRule.onNodeWithContentDescription("Perfect (recommended)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Sour").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Bitter").assertIsDisplayed()
    }

    @Test
    fun tasteQuickPick_clickingSourButton_callsOnSelectPrimary() {
        // Given
        var selectedTaste: TastePrimary? = null
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = { selectedTaste = it }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Sour").performClick()

        // Then
        assert(selectedTaste == TastePrimary.SOUR)
    }

    @Test
    fun tasteQuickPick_clickingPerfectButton_callsOnSelectPrimary() {
        // Given
        var selectedTaste: TastePrimary? = null
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = { selectedTaste = it }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Perfect").performClick()

        // Then
        assert(selectedTaste == TastePrimary.PERFECT)
    }

    @Test
    fun tasteQuickPick_clickingBitterButton_callsOnSelectPrimary() {
        // Given
        var selectedTaste: TastePrimary? = null
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = { selectedTaste = it }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Bitter").performClick()

        // Then
        assert(selectedTaste == TastePrimary.BITTER)
    }

    @Test
    fun tasteQuickPick_withSecondaryOptions_displaysSecondaryButtons() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    onSelectSecondary = {},
                    showSecondaryOptions = true
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Weak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Strong").assertIsDisplayed()
        composeTestRule.onNodeWithText("💧").assertIsDisplayed() // Weak emoji
        composeTestRule.onNodeWithText("💪").assertIsDisplayed() // Strong emoji
    }

    @Test
    fun tasteQuickPick_withoutSecondaryOptions_hidesSecondaryButtons() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    showSecondaryOptions = false
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Weak").assertDoesNotExist()
        composeTestRule.onNodeWithText("Strong").assertDoesNotExist()
    }

    @Test
    fun tasteQuickPick_clickingWeakButton_callsOnSelectSecondary() {
        // Given
        var selectedSecondary: TasteSecondary? = null
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    onSelectSecondary = { selectedSecondary = it },
                    showSecondaryOptions = true
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Weak").performClick()

        // Then
        assert(selectedSecondary == TasteSecondary.WEAK)
    }

    @Test
    fun tasteQuickPick_clickingStrongButton_callsOnSelectSecondary() {
        // Given
        var selectedSecondary: TasteSecondary? = null
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    onSelectSecondary = { selectedSecondary = it },
                    showSecondaryOptions = true
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Strong").performClick()

        // Then
        assert(selectedSecondary == TasteSecondary.STRONG)
    }

    @Test
    fun tasteQuickPick_clickingSelectedSecondaryButton_deselectsIt() {
        // Given
        var selectedSecondary: TasteSecondary? = TasteSecondary.WEAK
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    onSelectSecondary = { selectedSecondary = it },
                    selectedSecondary = selectedSecondary,
                    showSecondaryOptions = true
                )
            }
        }

        // When - click the already selected button
        composeTestRule.onNodeWithText("Weak").performClick()

        // Then - should deselect (set to null)
        assert(selectedSecondary == null)
    }

    @Test
    fun tasteQuickPick_withSkipOption_displaysSkipButton() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    onSkip = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
    }

    @Test
    fun tasteQuickPick_withoutSkipOption_hidesSkipButton() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    onSkip = null
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Skip").assertDoesNotExist()
    }

    @Test
    fun tasteQuickPick_clickingSkipButton_callsOnSkip() {
        // Given
        var skipCalled = false
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    onSkip = { skipCalled = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Skip").performClick()

        // Then
        assert(skipCalled)
    }

    @Test
    fun tasteQuickPick_withRecommendation_displaysRecommendationHint() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    recommended = TastePrimary.SOUR,
                    onSelectPrimary = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Recommended based on extraction time").assertIsDisplayed()
    }

    @Test
    fun tasteQuickPick_withoutRecommendation_hidesRecommendationHint() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    recommended = null,
                    onSelectPrimary = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Recommended based on extraction time").assertDoesNotExist()
    }

    @Test
    fun tasteQuickPick_displaysTitle() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("How did it taste?").assertIsDisplayed()
    }

    @Test
    fun tasteQuickPick_hasProperAccessibilitySupport() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    recommended = TastePrimary.PERFECT,
                    onSelectPrimary = {},
                    onSelectSecondary = {},
                    showSecondaryOptions = true,
                    onSkip = {}
                )
            }
        }

        // Then - verify accessibility content descriptions
        composeTestRule.onNodeWithContentDescription("Perfect (recommended)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Sour").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Bitter").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Skip taste feedback").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Weak qualifier not selected").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Strong qualifier not selected").assertIsDisplayed()
    }

    @Test
    fun tasteQuickPick_selectedSecondaryHasCorrectAccessibilityState() {
        // Given
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = {},
                    onSelectSecondary = {},
                    selectedSecondary = TasteSecondary.STRONG,
                    showSecondaryOptions = true
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Weak qualifier not selected").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Strong qualifier selected").assertIsDisplayed()
    }

    @Test
    fun tasteQuickPick_allButtonsAreClickable() {
        // Given
        var primaryClicked = false
        var secondaryClicked = false
        var skipClicked = false
        
        composeTestRule.setContent {
            CoffeeShotTimerTheme {
                TasteQuickPick(
                    onSelectPrimary = { primaryClicked = true },
                    onSelectSecondary = { secondaryClicked = true },
                    showSecondaryOptions = true,
                    onSkip = { skipClicked = true }
                )
            }
        }

        // When & Then
        composeTestRule.onNodeWithText("Perfect").assertHasClickAction().performClick()
        assert(primaryClicked)

        composeTestRule.onNodeWithText("Weak").assertHasClickAction().performClick()
        assert(secondaryClicked)

        composeTestRule.onNodeWithText("Skip").assertHasClickAction().performClick()
        assert(skipClicked)
    }

    @Test
    fun tasteQuickPick_allRecommendationStates_workCorrectly() {
        val recommendations = listOf(
            TastePrimary.SOUR,
            TastePrimary.PERFECT,
            TastePrimary.BITTER,
            null
        )

        recommendations.forEach { recommendation ->
            // Given
            composeTestRule.setContent {
                CoffeeShotTimerTheme {
                    TasteQuickPick(
                        recommended = recommendation,
                        onSelectPrimary = {}
                    )
                }
            }

            // Then
            if (recommendation != null) {
                composeTestRule.onNodeWithContentDescription("${recommendation.name.lowercase().replaceFirstChar { it.uppercase() }} (recommended)")
                    .assertIsDisplayed()
                composeTestRule.onNodeWithText("Recommended based on extraction time").assertIsDisplayed()
            } else {
                composeTestRule.onNodeWithText("Recommended based on extraction time").assertDoesNotExist()
            }
        }
    }
}
