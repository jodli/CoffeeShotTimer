package com.jodli.coffeeshottimer.data.database

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Test class for database models and validation.
 * Tests model validation and business logic.
 */
class AppDatabaseTest {
    
    @Test
    fun testBeanModelValidation() {
        // Test valid bean
        val validBean = Bean(
            name = "Test Bean",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Test notes"
        )
        
        val validationResult = validBean.validate()
        assertTrue("Valid bean should pass validation", validationResult.isValid)
        assertTrue("Valid bean should have no errors", validationResult.errors.isEmpty())
    }
    
    @Test
    fun testBeanModelInvalidName() {
        // Test bean with empty name
        val emptyNameBean = Bean(
            name = "",
            roastDate = LocalDate.now().minusDays(7)
        )
        
        val validationResult = emptyNameBean.validate()
        assertFalse("Bean with empty name should fail validation", validationResult.isValid)
        assertTrue("Should have name error", validationResult.errors.any { it.contains("name cannot be empty") })
    }
    
    @Test
    fun testBeanDaysSinceRoast() {
        val bean = Bean(
            name = "Test Bean",
            roastDate = LocalDate.now().minusDays(5)
        )
        
        assertEquals("Should calculate days since roast correctly", 5, bean.daysSinceRoast())
    }
    
    @Test
    fun testBeanFreshnessCheck() {
        // Fresh bean (7 days old)
        val freshBean = Bean(
            name = "Fresh Bean",
            roastDate = LocalDate.now().minusDays(7)
        )
        assertTrue("7-day old bean should be fresh", freshBean.isFresh())
        
        // Too fresh bean (2 days old)
        val tooFreshBean = Bean(
            name = "Too Fresh Bean",
            roastDate = LocalDate.now().minusDays(2)
        )
        assertFalse("2-day old bean should not be fresh", tooFreshBean.isFresh())
        
        // Old bean (25 days old)
        val oldBean = Bean(
            name = "Old Bean",
            roastDate = LocalDate.now().minusDays(25)
        )
        assertFalse("25-day old bean should not be fresh", oldBean.isFresh())
    }
    
    @Test
    fun testShotModelValidation() {
        // Test valid shot
        val validShot = Shot(
            beanId = "test-bean-id",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )
        
        val validationResult = validShot.validate()
        assertTrue("Valid shot should pass validation", validationResult.isValid)
        assertTrue("Valid shot should have no errors", validationResult.errors.isEmpty())
    }
    
    @Test
    fun testShotBrewRatioCalculation() {
        val shot = Shot(
            beanId = "test-bean-id",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )
        
        assertEquals("Brew ratio should be calculated correctly", 2.0, shot.brewRatio, 0.01)
        assertEquals("Formatted brew ratio should be correct", "1:2.0", shot.getFormattedBrewRatio())
    }
    
    @Test
    fun testShotExtractionTimeFormatting() {
        val shot = Shot(
            beanId = "test-bean-id",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 95, // 1 minute 35 seconds
            grinderSetting = "15"
        )
        
        assertEquals("Extraction time should be formatted correctly", "01:35", shot.getFormattedExtractionTime())
    }
    
    @Test
    fun testShotOptimalChecks() {
        // Optimal shot
        val optimalShot = Shot(
            beanId = "test-bean-id",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 27,
            grinderSetting = "15"
        )
        
        assertTrue("27-second extraction should be optimal", optimalShot.isOptimalExtractionTime())
        assertTrue("1:2 ratio should be typical", optimalShot.isTypicalBrewRatio())
        
        // Non-optimal shot
        val fastShot = Shot(
            beanId = "test-bean-id",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 54.0, // 1:3 ratio
            extractionTimeSeconds = 15, // Too fast
            grinderSetting = "15"
        )
        
        assertFalse("15-second extraction should not be optimal", fastShot.isOptimalExtractionTime())
        assertTrue("1:3 ratio should still be typical", fastShot.isTypicalBrewRatio())
    }
    
    @Test
    fun testTypeConverters() {
        // Test that the Converters class works correctly
        val converters = Converters()
        
        // Test LocalDate conversion
        val testDate = LocalDate.of(2024, 1, 15)
        val dateString = converters.fromLocalDate(testDate)
        val convertedDate = converters.toLocalDate(dateString)
        
        assertEquals("LocalDate should convert correctly", testDate, convertedDate)
        
        // Test null handling
        assertNull("Null LocalDate should convert to null string", converters.fromLocalDate(null))
        assertNull("Null string should convert to null LocalDate", converters.toLocalDate(null))
    }
}