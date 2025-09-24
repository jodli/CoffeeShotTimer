package com.jodli.coffeeshottimer.ui.validation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ValidationHelpersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GrinderValidationHelpers returns appropriate suggestions`() {
        composeTestRule.setContent {
            // Test various error messages
            assertEquals(
                "Increase maximum or decrease minimum",
                GrinderValidationHelpers.getValidationSuggestion("Minimum scale value must be less than maximum scale value")
                    .takeIf { it.contains("Increase") || it.contains("maximum") || it.contains("minimum") } ?: "Found suggestion"
            )
            
            assertEquals(
                "",
                GrinderValidationHelpers.getValidationSuggestion("Unknown error")
            )
            
            assertEquals(
                "",
                GrinderValidationHelpers.getValidationSuggestion(null)
            )
        }
    }

    @Test
    fun `BasketValidationHelpers returns appropriate suggestions`() {
        composeTestRule.setContent {
            // Test various error messages
            assertEquals(
                "",
                BasketValidationHelpers.getValidationSuggestion("Unknown error")
            )
            
            assertEquals(
                "",
                BasketValidationHelpers.getValidationSuggestion(null)
            )
        }
    }
}