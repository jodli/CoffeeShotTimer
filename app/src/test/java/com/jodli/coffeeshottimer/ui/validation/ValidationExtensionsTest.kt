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
        every { mockStringProvider.getCoffeeInputWeightLabel() } returns "Coffee input weight"
        every { mockStringProvider.getCoffeeOutputWeightLabel() } returns "Coffee output weight"
        every { mockStringProvider.getValidNumberError() } returns "Please enter a valid number"
        every { mockStringProvider.getFieldRequiredError(any()) } returns "Field is required"
        every { mockStringProvider.getMinimumWeightError(any(), any()) } returns "Weight must be at least 12g"
        every { mockStringProvider.getMaximumWeightError(any(), any()) } returns "Weight cannot exceed 22g"
        every { mockStringProvider.getOneDecimalPlaceError(any()) } returns "Only one decimal place allowed"
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

    @Test
    fun `validateCoffeeWeightIn should pass for valid weights`() {
        val result = "18.5".validateCoffeeWeightIn(validationUtils)
        assertTrue("Valid weight should pass validation", result.isValid)
        assertTrue("Valid weight should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateCoffeeWeightIn should fail for empty input`() {
        val result = "".validateCoffeeWeightIn(validationUtils)
        assertFalse("Empty weight should fail validation", result.isValid)
        assertTrue("Empty weight should have error", result.errors.isNotEmpty())
        assertTrue("Error should mention requirement",
            result.errors.any { it.contains("required", ignoreCase = true) })
    }

    @Test
    fun `validateCoffeeWeightIn should fail for weight too low`() {
        val result = "10.0".validateCoffeeWeightIn(validationUtils)
        assertFalse("Weight too low should fail validation", result.isValid)
        assertTrue("Should have minimum weight error",
            result.errors.any { it.contains("at least", ignoreCase = true) })
    }

    @Test
    fun `validateCoffeeWeightIn should fail for weight too high`() {
        val result = "25.0".validateCoffeeWeightIn(validationUtils)
        assertFalse("Weight too high should fail validation", result.isValid)
        assertTrue("Should have maximum weight error",
            result.errors.any { it.contains("exceed", ignoreCase = true) })
    }

    @Test
    fun `validateCoffeeWeightIn should provide helpful tips for unusual weights`() {
        val lowResult = "5.0".validateCoffeeWeightIn(validationUtils) // Below 10.0 threshold
        assertFalse("Weight below minimum should fail", lowResult.isValid)
        assertTrue("Should have minimum weight error",
            lowResult.errors.any { it.contains("at least", ignoreCase = true) })

        val highResult = "35.0".validateCoffeeWeightIn(validationUtils) // Above 30.0 threshold
        assertFalse("Weight above maximum should fail", highResult.isValid)
        assertTrue("Should have maximum weight error",
            highResult.errors.any { it.contains("exceed", ignoreCase = true) })
    }

    @Test
    fun `validateCoffeeWeightOut should pass for valid weights`() {
        val result = "35.0".validateCoffeeWeightOut(validationUtils)
        assertTrue("Valid output weight should pass validation", result.isValid)
        assertTrue("Valid weight should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateCoffeeWeightOut should provide helpful tips for unusual weights`() {
        val lowResult = "15.0".validateCoffeeWeightOut(validationUtils) // Below 25.0 threshold
        assertFalse("Weight below minimum should fail", lowResult.isValid)
        assertTrue("Should have minimum weight error",
            lowResult.errors.any { it.contains("at least", ignoreCase = true) })

        val highResult = "60.0".validateCoffeeWeightOut(validationUtils) // Above 50.0 threshold
        assertFalse("Weight above maximum should fail", highResult.isValid)
        assertTrue("Should have maximum weight error",
            highResult.errors.any { it.contains("exceed", ignoreCase = true) })
    }

    @Test
    fun `validateGrinderSettingEnhanced should pass for valid settings`() {
        val result = "15".validateGrinderSettingEnhanced(validationUtils)
        assertTrue("Valid grinder setting should pass", result.isValid)
        assertTrue("Valid setting should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateGrinderSettingEnhanced should provide helpful tip for empty setting`() {
        val result = "".validateGrinderSettingEnhanced(true)
        assertFalse("Empty required setting should fail", result.isValid)
        assertTrue("Should provide helpful tip",
            result.errors.any { it.contains("remember", ignoreCase = true) })
    }

    @Test
    fun `validateBeanNameEnhanced should pass for valid names`() {
        val result = "Ethiopian Yirgacheffe".validateBeanNameEnhanced()
        assertTrue("Valid bean name should pass", result.isValid)
        assertTrue("Valid name should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateBeanNameEnhanced should provide helpful tips for problematic names`() {
        val shortResult = "A".validateBeanNameEnhanced()
        assertFalse("Single character name should fail", shortResult.isValid)
        assertTrue("Should suggest more descriptive name",
            shortResult.errors.any { it.contains("descriptive", ignoreCase = true) })

        val duplicateResult = "Existing Bean".validateBeanNameEnhanced(listOf("Existing Bean"))
        assertFalse("Duplicate name should fail", duplicateResult.isValid)
        assertTrue("Should suggest making name unique",
            duplicateResult.errors.any { it.contains("unique", ignoreCase = true) })
    }

    @Test
    fun `validateRoastDateEnhanced should pass for valid dates`() {
        val result = LocalDate.now().minusDays(7).validateRoastDateEnhanced()
        assertTrue("Valid roast date should pass", result.isValid)
        assertTrue("Valid date should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateRoastDateEnhanced should provide helpful tips for problematic dates`() {
        val futureResult = LocalDate.now().plusDays(1).validateRoastDateEnhanced()
        assertFalse("Future date should fail", futureResult.isValid)
        assertTrue("Should suggest using today's date",
            futureResult.errors.any { it.contains("today", ignoreCase = true) })

        val oldResult = LocalDate.now().minusDays(450).validateRoastDateEnhanced()
        assertFalse("Very old date should fail", oldResult.isValid)
        assertTrue("Should warn about flavor loss",
            oldResult.errors.any { it.contains("flavor", ignoreCase = true) })
    }

    @Test
    fun `validateNotesEnhanced should pass for valid notes`() {
        val result = "Great flavor with chocolate notes".validateNotesEnhanced()
        assertTrue("Valid notes should pass", result.isValid)
    }

    @Test
    fun `validateExtractionTimeEnhanced should provide contextual feedback`() {
        val shortResult = 3.validateExtractionTimeEnhanced()
        assertFalse("Very short time should fail", shortResult.isValid)
        assertTrue("Should warn about sour taste",
            shortResult.errors.any { it.contains("sour", ignoreCase = true) })

        val longResult = 150.validateExtractionTimeEnhanced()
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
        val shortTime = 20.getExtractionTimeWarnings()
        assertTrue("Short time should have warning", shortTime.isNotEmpty())
        assertTrue("Should suggest grinding finer",
            shortTime.any { it.contains("finer", ignoreCase = true) })

        val longTime = 35.getExtractionTimeWarnings()
        assertTrue("Long time should have warning", longTime.isNotEmpty())
        assertTrue("Should suggest grinding coarser",
            longTime.any { it.contains("coarser", ignoreCase = true) })

        val optimalTime = 27.getExtractionTimeWarnings()
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
            grinderSetting = "15",
            notes = "Perfect shot"
        )
        assertTrue("Valid complete shot should pass", validResult.isValid)

        val invalidResult = validateCompleteShot(
            coffeeWeightIn = "",
            coffeeWeightOut = "60.0", // Above new max of 55g
            extractionTimeSeconds = 200,
            grinderSetting = "",
            notes = ""
        )
        assertFalse("Invalid complete shot should fail", invalidResult.isValid)
        assertTrue("Should have multiple errors", invalidResult.errors.size > 1)
    }

    @Test
    fun `validateCompleteShot should catch cross-field validation errors`() {
        val result = validateCompleteShot(
            coffeeWeightIn = "18.0",
            coffeeWeightOut = "15.0", // Less than input
            extractionTimeSeconds = 27,
            grinderSetting = "15",
            notes = ""
        )
        assertFalse("Output less than input should fail", result.isValid)
    }

    @Test
    fun `validateCompleteBean should validate all bean parameters together`() {
        val validResult = validateCompleteBean(
            name = "Ethiopian Yirgacheffe",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Floral and bright",
            grinderSetting = "15"
        )
        assertTrue("Valid complete bean should pass", validResult.isValid)

        val invalidResult = validateCompleteBean(
            name = "",
            roastDate = LocalDate.now().plusDays(1),
            notes = "x".repeat(600), // Too long
            grinderSetting = "x".repeat(60) // Too long
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
            grinderSetting = ""
        )
        assertTrue("Fresh bean validation should pass", freshResult.isValid)
        // Should have warnings about very fresh beans

        val oldResult = validateCompleteBean(
            name = "Old Bean",
            roastDate = LocalDate.now().minusDays(300),
            notes = "",
            grinderSetting = ""
        )
        assertTrue("Old bean validation should pass basic validation", oldResult.isValid)
        // Should have warnings about older beans
    }

    @Test
    fun `Weight slider constants should be reasonable for espresso`() {
        // Test that our weight slider constants make sense for espresso
        assertTrue("Coffee in min weight should be reasonable",
            com.jodli.coffeeshottimer.ui.components.WeightSliderConstants.COFFEE_IN_MIN_WEIGHT >= 10.0f)
        assertTrue("Coffee in max weight should be reasonable",
            com.jodli.coffeeshottimer.ui.components.WeightSliderConstants.COFFEE_IN_MAX_WEIGHT <= 25.0f)
        assertTrue("Coffee out min weight should be reasonable",
            com.jodli.coffeeshottimer.ui.components.WeightSliderConstants.COFFEE_OUT_MIN_WEIGHT >= 20.0f)
        assertTrue("Coffee out max weight should be reasonable",
            com.jodli.coffeeshottimer.ui.components.WeightSliderConstants.COFFEE_OUT_MAX_WEIGHT <= 60.0f)
        assertTrue("Coffee in range should be valid",
            com.jodli.coffeeshottimer.ui.components.WeightSliderConstants.COFFEE_IN_MIN_WEIGHT <
            com.jodli.coffeeshottimer.ui.components.WeightSliderConstants.COFFEE_IN_MAX_WEIGHT)
        assertTrue("Coffee out range should be valid",
            com.jodli.coffeeshottimer.ui.components.WeightSliderConstants.COFFEE_OUT_MIN_WEIGHT <
            com.jodli.coffeeshottimer.ui.components.WeightSliderConstants.COFFEE_OUT_MAX_WEIGHT)
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
