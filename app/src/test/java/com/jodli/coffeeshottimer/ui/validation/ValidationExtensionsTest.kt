package com.jodli.coffeeshottimer.ui.validation

import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for validation extensions and utilities.
 */
class ValidationExtensionsTest {

    private lateinit var validationUtils: ValidationUtils
    private lateinit var mockStringProvider: ValidationStringProvider

    @Before
    fun setup() {
        mockStringProvider = mockk<ValidationStringProvider>(relaxed = true)

        // Setup common mock responses
        every { mockStringProvider.getValidNumberError() } returns "Please enter a valid number"
        every { mockStringProvider.getFieldRequiredError(any()) } returns "Field is required"
        every { mockStringProvider.getRatioConcentratedWarning() } returns "Ratio concentrated"
        every { mockStringProvider.getRatioDilutedWarning() } returns "Ratio diluted"
        every { mockStringProvider.getRatioHigherWarning() } returns "Consider higher ratio"
        every { mockStringProvider.getRatioLowerWarning() } returns "Consider lower ratio"
        every { mockStringProvider.getGrinderSettingTip() } returns "Remember your last setting"
        every { mockStringProvider.getDescriptiveNameTip() } returns "Use a more descriptive name"
        every { mockStringProvider.getUniqueNameTip() } returns "Make the name unique"
        every { mockStringProvider.getRoastDateTodayTip() } returns "Use today's date"
        every { mockStringProvider.getOldBeansNote() } returns "Beans may have lost flavor"
        every { mockStringProvider.getNotesHelpfulTip() } returns "Notes are helpful"
        every { mockStringProvider.getCharacterLimitNote() } returns "Approaching character limit"
        every { mockStringProvider.getShortExtractionSourTip() } returns "Short extraction may taste sour"
        every { mockStringProvider.getLongExtractionBitterTip() } returns "Long extraction may taste bitter"
        every { mockStringProvider.getGrindFinerWarning() } returns "Consider grinding finer"
        every { mockStringProvider.getGrindCoarserWarning() } returns "Consider grinding coarser"
        every { mockStringProvider.getOptimalTimeSuccess() } returns "Optimal extraction time"
        every { mockStringProvider.getVeryFreshBeansWarning() } returns "Beans are very fresh"
        every { mockStringProvider.getFreshBeansSuccess() } returns "Beans are fresh"
        every { mockStringProvider.getAgingBeansWarning() } returns "Beans are aging"
        every { mockStringProvider.getOldBeansWarning() } returns "Beans are old"
        every { mockStringProvider.getOutputWeightLessThanInputError() } returns "Output weight less than input"

        validationUtils = ValidationUtils(mockStringProvider)
    }

    // Weight validation tests removed - sliders now handle all weight validation by constraining values to basket configuration ranges

    @Test
    fun `validateBeanNameEnhanced should pass for valid names`() {
        val result = "Ethiopian Yirgacheffe".validateBeanNameEnhanced(validationUtils)
        assertTrue("Valid bean name should pass", result.isValid)
        assertTrue("Valid name should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateBeanNameEnhanced should provide helpful tips for problematic names`() {
        val shortResult = "A".validateBeanNameEnhanced(validationUtils)
        assertFalse("Single character name should fail", shortResult.isValid)
        assertTrue(
            "Should suggest more descriptive name",
            shortResult.errors.any { it.contains("descriptive", ignoreCase = true) }
        )

        val duplicateResult = "Existing Bean".validateBeanNameEnhanced(validationUtils, listOf("Existing Bean"))
        assertFalse("Duplicate name should fail", duplicateResult.isValid)
        assertTrue(
            "Should suggest making name unique",
            duplicateResult.errors.any { it.contains("unique", ignoreCase = true) }
        )
    }

    @Test
    fun `validateRoastDateEnhanced should pass for valid dates`() {
        val result = LocalDate.now().minusDays(7).validateRoastDateEnhanced(validationUtils)
        assertTrue("Valid roast date should pass", result.isValid)
        assertTrue("Valid date should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateRoastDateEnhanced should provide helpful tips for problematic dates`() {
        val futureResult = LocalDate.now().plusDays(1).validateRoastDateEnhanced(validationUtils)
        assertFalse("Future date should fail", futureResult.isValid)
        assertTrue(
            "Should suggest using today's date",
            futureResult.errors.any { it.contains("today", ignoreCase = true) }
        )

        val oldResult = LocalDate.now().minusDays(450).validateRoastDateEnhanced(validationUtils)
        assertFalse("Very old date should fail", oldResult.isValid)
        assertTrue(
            "Should warn about flavor loss",
            oldResult.errors.any { it.contains("flavor", ignoreCase = true) }
        )
    }

    @Test
    fun `validateNotesEnhanced should pass for valid notes`() {
        val result = "Great flavor with chocolate notes".validateNotesEnhanced(validationUtils)
        assertTrue("Valid notes should pass", result.isValid)
    }

    @Test
    fun `getBrewRatioWarnings should provide appropriate feedback`() {
        val lowRatio = 1.2.getBrewRatioWarnings(validationUtils)
        assertTrue("Low ratio should have warning", lowRatio.isNotEmpty())
        assertTrue(
            "Should warn about concentration",
            lowRatio.any { it.contains("concentrated", ignoreCase = true) }
        )

        val highRatio = 3.5.getBrewRatioWarnings(validationUtils)
        assertTrue("High ratio should have warning", highRatio.isNotEmpty())
        assertTrue(
            "Should warn about dilution",
            highRatio.any { it.contains("diluted", ignoreCase = true) }
        )

        val optimalRatio = 2.2.getBrewRatioWarnings(validationUtils)
        // Optimal ratio might still have suggestions, but shouldn't have strong warnings
        assertTrue(
            "Optimal ratio warnings should be mild or empty",
            optimalRatio.isEmpty() || optimalRatio.all { !it.contains("very", ignoreCase = true) }
        )
    }

    @Test
    fun `BasketConfiguration defaults should be reasonable for espresso`() {
        // Test that our basket configuration defaults make sense for espresso
        val defaultBasket = com.jodli.coffeeshottimer.data.model.BasketConfiguration.DEFAULT

        assertTrue(
            "Coffee in min weight should be reasonable",
            defaultBasket.coffeeInMin >= 5.0f
        )
        assertTrue(
            "Coffee in max weight should be reasonable",
            defaultBasket.coffeeInMax <= 25.0f
        )
        assertTrue(
            "Coffee out min weight should be reasonable",
            defaultBasket.coffeeOutMin >= 10.0f
        )
        assertTrue(
            "Coffee out max weight should be reasonable",
            defaultBasket.coffeeOutMax <= 60.0f
        )
        assertTrue(
            "Coffee in range should be valid",
            defaultBasket.coffeeInMin < defaultBasket.coffeeInMax
        )
        assertTrue(
            "Coffee out range should be valid",
            defaultBasket.coffeeOutMin < defaultBasket.coffeeOutMax
        )
        assertTrue(
            "Default configuration should pass validation",
            defaultBasket.validate().isValid
        )
    }

    @Test
    fun `ValidationUtils constants should be reasonable for espresso`() {
        // Test that our validation constants make sense for espresso
        assertTrue(
            "Min extraction time should be reasonable",
            ValidationUtils.MIN_EXTRACTION_TIME >= 5
        )
        assertTrue(
            "Max extraction time should be reasonable",
            ValidationUtils.MAX_EXTRACTION_TIME <= 120
        )
        assertTrue(
            "Optimal extraction time range should be reasonable",
            ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN < ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX
        )
        assertTrue(
            "Brew ratio range should be reasonable",
            ValidationUtils.MIN_TYPICAL_BREW_RATIO < ValidationUtils.MAX_TYPICAL_BREW_RATIO
        )
    }
}
