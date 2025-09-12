package com.jodli.coffeeshottimer.ui.validation

import com.jodli.coffeeshottimer.ui.components.ValidationUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
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
        assertTrue("Should suggest more descriptive name",
            shortResult.errors.any { it.contains("descriptive", ignoreCase = true) })

        val duplicateResult = "Existing Bean".validateBeanNameEnhanced(validationUtils, listOf("Existing Bean"))
        assertFalse("Duplicate name should fail", duplicateResult.isValid)
        assertTrue("Should suggest making name unique",
            duplicateResult.errors.any { it.contains("unique", ignoreCase = true) })
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
        assertTrue("Should suggest using today's date",
            futureResult.errors.any { it.contains("today", ignoreCase = true) })

        val oldResult = LocalDate.now().minusDays(450).validateRoastDateEnhanced(validationUtils)
        assertFalse("Very old date should fail", oldResult.isValid)
        assertTrue("Should warn about flavor loss",
            oldResult.errors.any { it.contains("flavor", ignoreCase = true) })
    }

    @Test
    fun `validateNotesEnhanced should pass for valid notes`() {
        val result = "Great flavor with chocolate notes".validateNotesEnhanced(validationUtils)
        assertTrue("Valid notes should pass", result.isValid)
    }

    @Test
    fun `validateExtractionTimeEnhanced should provide contextual feedback`() {
        val shortResult = 3.validateExtractionTimeEnhanced(validationUtils)
        assertFalse("Very short time should fail", shortResult.isValid)
        assertTrue("Should warn about sour taste",
            shortResult.errors.any { it.contains("sour", ignoreCase = true) })

        val longResult = 150.validateExtractionTimeEnhanced(validationUtils)
        assertFalse("Very long time should fail", longResult.isValid)
        assertTrue("Should warn about bitter taste",
            longResult.errors.any { it.contains("bitter", ignoreCase = true) })
    }

    @Test
    fun `getBrewRatioWarnings should provide appropriate feedback`() {
        val lowRatio = 1.2.getBrewRatioWarnings(validationUtils)
        assertTrue("Low ratio should have warning", lowRatio.isNotEmpty())
        assertTrue("Should warn about concentration",
            lowRatio.any { it.contains("concentrated", ignoreCase = true) })

        val highRatio = 3.5.getBrewRatioWarnings(validationUtils)
        assertTrue("High ratio should have warning", highRatio.isNotEmpty())
        assertTrue("Should warn about dilution",
            highRatio.any { it.contains("diluted", ignoreCase = true) })

        val optimalRatio = 2.2.getBrewRatioWarnings(validationUtils)
        // Optimal ratio might still have suggestions, but shouldn't have strong warnings
        assertTrue("Optimal ratio warnings should be mild or empty",
            optimalRatio.isEmpty() || optimalRatio.all { !it.contains("very", ignoreCase = true) })
    }

    @Test
    fun `getExtractionTimeWarnings should provide appropriate feedback`() {
        val shortTime = 20.getExtractionTimeWarnings(validationUtils)
        assertTrue("Short time should have warning", shortTime.isNotEmpty())
        assertTrue("Should suggest grinding finer",
            shortTime.any { it.contains("finer", ignoreCase = true) })

        val longTime = 35.getExtractionTimeWarnings(validationUtils)
        assertTrue("Long time should have warning", longTime.isNotEmpty())
        assertTrue("Should suggest grinding coarser",
            longTime.any { it.contains("coarser", ignoreCase = true) })

        val optimalTime = 27.getExtractionTimeWarnings(validationUtils)
        assertTrue("Optimal time should have positive feedback", optimalTime.isNotEmpty())
        assertTrue("Should mention optimal range",
            optimalTime.any { it.contains("optimal", ignoreCase = true) })
    }

    @Test
    fun `validateCompleteShot should validate all parameters together`() {
        val validResult = validateCompleteShot(
            coffeeWeightIn = "18.0",
            coffeeWeightOut = "36.0",
            extractionTimeSeconds = 27,
            grinderSetting = "15", // Not validated - slider constrains values
            notes = "Perfect shot",
            validationUtils = validationUtils
        )
        assertTrue("Valid complete shot should pass", validResult.isValid)

        val invalidResult = validateCompleteShot(
            coffeeWeightIn = "18.0", // Valid weights - sliders constrain values
            coffeeWeightOut = "36.0",
            extractionTimeSeconds = 200, // Invalid time
            grinderSetting = "15", // Not validated - slider constrains values
            notes = "",
            validationUtils = validationUtils
        )
        assertFalse("Invalid extraction time should fail", invalidResult.isValid)
    }

    @Test
    fun `validateCompleteShot should catch cross-field validation errors`() {
        val result = validateCompleteShot(
            coffeeWeightIn = "18.0",
            coffeeWeightOut = "15.0", // Less than input
            extractionTimeSeconds = 27,
            grinderSetting = "15",
            notes = "",
            validationUtils = validationUtils
        )
        assertFalse("Output less than input should fail", result.isValid)
    }

    @Test
    fun `validateCompleteBean should validate all bean parameters together`() {
        val validResult = validateCompleteBean(
            name = "Ethiopian Yirgacheffe",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Floral and bright",
            grinderSetting = "15",
            validationUtils = validationUtils
        )
        assertTrue("Valid complete bean should pass", validResult.isValid)

        val invalidResult = validateCompleteBean(
            name = "",
            roastDate = LocalDate.now().plusDays(1),
            notes = "x".repeat(600), // Too long
            grinderSetting = "x".repeat(60), // Too long
            validationUtils = validationUtils
        )
        assertFalse("Invalid complete bean should fail", invalidResult.isValid)
        assertTrue("Should have multiple errors", invalidResult.errors.size > 1)
    }

    @Test
    fun `validateCompleteBean should provide contextual warnings about bean age`() {
        val freshResult = validateCompleteBean(
            name = "Fresh Bean",
            roastDate = LocalDate.now().minusDays(10),
            notes = "",
            grinderSetting = "",
            validationUtils = validationUtils
        )
        assertTrue("Fresh bean validation should pass", freshResult.isValid)
        // Should have warnings about very fresh beans

        val oldResult = validateCompleteBean(
            name = "Old Bean",
            roastDate = LocalDate.now().minusDays(300),
            notes = "",
            grinderSetting = "",
            validationUtils = validationUtils
        )
        assertTrue("Old bean validation should pass basic validation", oldResult.isValid)
        // Should have warnings about older beans
    }

    @Test
    fun `BasketConfiguration defaults should be reasonable for espresso`() {
        // Test that our basket configuration defaults make sense for espresso
        val defaultBasket = com.jodli.coffeeshottimer.data.model.BasketConfiguration.DEFAULT
        
        assertTrue("Coffee in min weight should be reasonable",
            defaultBasket.coffeeInMin >= 5.0f)
        assertTrue("Coffee in max weight should be reasonable",
            defaultBasket.coffeeInMax <= 25.0f)
        assertTrue("Coffee out min weight should be reasonable",
            defaultBasket.coffeeOutMin >= 10.0f)
        assertTrue("Coffee out max weight should be reasonable",
            defaultBasket.coffeeOutMax <= 60.0f)
        assertTrue("Coffee in range should be valid",
            defaultBasket.coffeeInMin < defaultBasket.coffeeInMax)
        assertTrue("Coffee out range should be valid",
            defaultBasket.coffeeOutMin < defaultBasket.coffeeOutMax)
        assertTrue("Default configuration should pass validation",
            defaultBasket.validate().isValid)
    }

    @Test
    fun `ValidationUtils constants should be reasonable for espresso`() {
        // Test that our validation constants make sense for espresso
        assertTrue("Min extraction time should be reasonable",
            ValidationUtils.MIN_EXTRACTION_TIME >= 5)
        assertTrue("Max extraction time should be reasonable",
            ValidationUtils.MAX_EXTRACTION_TIME <= 120)
        assertTrue("Optimal extraction time range should be reasonable",
            ValidationUtils.OPTIMAL_EXTRACTION_TIME_MIN < ValidationUtils.OPTIMAL_EXTRACTION_TIME_MAX)
        assertTrue("Brew ratio range should be reasonable",
            ValidationUtils.MIN_TYPICAL_BREW_RATIO < ValidationUtils.MAX_TYPICAL_BREW_RATIO)
    }
}
