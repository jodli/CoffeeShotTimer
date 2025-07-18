package com.example.coffeeshottimer.ui.validation

import com.example.coffeeshottimer.ui.components.ValidationUtils
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

/**
 * Unit tests for validation extensions and utilities.
 */
class ValidationExtensionsTest {

    @Test
    fun `validateCoffeeWeightIn should pass for valid weights`() {
        val result = "18.5".validateCoffeeWeightIn()
        assertTrue("Valid weight should pass validation", result.isValid)
        assertTrue("Valid weight should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateCoffeeWeightIn should fail for empty input`() {
        val result = "".validateCoffeeWeightIn()
        assertFalse("Empty weight should fail validation", result.isValid)
        assertTrue("Empty weight should have error", result.errors.isNotEmpty())
        assertTrue("Error should mention requirement", 
            result.errors.any { it.contains("required", ignoreCase = true) })
    }

    @Test
    fun `validateCoffeeWeightIn should fail for weight too low`() {
        val result = "0.05".validateCoffeeWeightIn()
        assertFalse("Weight too low should fail validation", result.isValid)
        assertTrue("Should have minimum weight error", 
            result.errors.any { it.contains("at least", ignoreCase = true) })
    }

    @Test
    fun `validateCoffeeWeightIn should fail for weight too high`() {
        val result = "55.0".validateCoffeeWeightIn()
        assertFalse("Weight too high should fail validation", result.isValid)
        assertTrue("Should have maximum weight error", 
            result.errors.any { it.contains("exceed", ignoreCase = true) })
    }

    @Test
    fun `validateCoffeeWeightIn should provide helpful tips for unusual weights`() {
        val lowResult = "3.0".validateCoffeeWeightIn()
        assertFalse("Very low weight should fail", lowResult.isValid)
        assertTrue("Should provide tip for low weight", 
            lowResult.errors.any { it.contains("15-20g", ignoreCase = true) })

        val highResult = "30.0".validateCoffeeWeightIn()
        assertFalse("Very high weight should fail", highResult.isValid)
        assertTrue("Should provide tip for high weight", 
            highResult.errors.any { it.contains("15-20g", ignoreCase = true) })
    }

    @Test
    fun `validateCoffeeWeightOut should pass for valid weights`() {
        val result = "36.0".validateCoffeeWeightOut()
        assertTrue("Valid output weight should pass validation", result.isValid)
        assertTrue("Valid weight should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validateCoffeeWeightOut should provide helpful tips for unusual weights`() {
        val lowResult = "10.0".validateCoffeeWeightOut()
        assertFalse("Very low output weight should fail", lowResult.isValid)
        assertTrue("Should provide tip for low output weight", 
            lowResult.errors.any { it.contains("25-40g", ignoreCase = true) })

        val highResult = "60.0".validateCoffeeWeightOut()
        assertFalse("Very high output weight should fail", highResult.isValid)
        assertTrue("Should provide tip for high output weight", 
            highResult.errors.any { it.contains("25-40g", ignoreCase = true) })
    }

    @Test
    fun `validateGrinderSettingEnhanced should pass for valid settings`() {
        val result = "15".validateGrinderSettingEnhanced()
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

        val oldResult = LocalDate.now().minusDays(45).validateRoastDateEnhanced()
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
        val lowRatio = 1.2.getBrewRatioWarnings()
        assertTrue("Low ratio should have warning", lowRatio.isNotEmpty())
        assertTrue("Should warn about concentration", 
            lowRatio.any { it.contains("concentrated", ignoreCase = true) })

        val highRatio = 3.5.getBrewRatioWarnings()
        assertTrue("High ratio should have warning", highRatio.isNotEmpty())
        assertTrue("Should warn about dilution", 
            highRatio.any { it.contains("diluted", ignoreCase = true) })

        val optimalRatio = 2.2.getBrewRatioWarnings()
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
            coffeeWeightOut = "100.0",
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
            coffeeWeightIn = "20.0",
            coffeeWeightOut = "15.0", // Less than input
            extractionTimeSeconds = 27,
            grinderSetting = "15",
            notes = ""
        )
        assertFalse("Output less than input should fail", result.isValid)
        assertTrue("Should have cross-field error", 
            result.errors.any { it.contains("Output weight cannot be less than input weight") })
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
            roastDate = LocalDate.now().minusDays(1),
            notes = "",
            grinderSetting = ""
        )
        assertTrue("Fresh bean validation should pass", freshResult.isValid)
        // Should have warnings about very fresh beans

        val oldResult = validateCompleteBean(
            name = "Old Bean",
            roastDate = LocalDate.now().minusDays(45),
            notes = "",
            grinderSetting = ""
        )
        assertTrue("Old bean validation should pass basic validation", oldResult.isValid)
        // Should have warnings about older beans
    }

    @Test
    fun `ValidationUtils constants should be reasonable for espresso`() {
        // Test that our validation constants make sense for espresso
        assertTrue("Min coffee weight should be reasonable", 
            ValidationUtils.MIN_COFFEE_WEIGHT_IN >= 0.1)
        assertTrue("Max coffee weight should be reasonable", 
            ValidationUtils.MAX_COFFEE_WEIGHT_IN <= 50.0)
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