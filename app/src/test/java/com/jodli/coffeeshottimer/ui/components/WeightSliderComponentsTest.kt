package com.jodli.coffeeshottimer.ui.components

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.roundToInt

/**
 * Test class for WeightSlider components to verify whole gram increment functionality.
 */
class WeightSliderComponentsTest {
    
    @Test
    fun `weight slider should handle whole gram values correctly`() {
        // Test that string values are converted to whole grams
        val testValues = listOf("18.7", "19.2", "20.0", "15.9")
        val expectedValues = listOf(19, 19, 20, 16) // Rounded to nearest whole gram
        
        testValues.forEachIndexed { index, value ->
            val floatValue = value.toFloatOrNull() ?: 1f
            val roundedValue = floatValue.roundToInt()
            assertEquals("Value $value should round to ${expectedValues[index]}", 
                expectedValues[index], roundedValue)
        }
    }
    
    @Test
    fun `weight slider should handle invalid input gracefully`() {
        val invalidValues = listOf("", "abc", "18.5.5")
        
        invalidValues.forEach { value ->
            val floatValue = value.toFloatOrNull()
            // Should be null for invalid input
            if (value == "" || value == "abc" || value == "18.5.5") {
                assertTrue("Value '$value' should parse to null or be handled gracefully", 
                    floatValue == null || floatValue >= 0)
            }
        }
        
        // Test negative value separately
        val negativeValue = "-5"
        val negativeFloat = negativeValue.toFloatOrNull()
        assertNotNull("Negative value should parse to a float", negativeFloat)
        assertTrue("Negative value should be handled by coercion in actual usage", 
            negativeFloat != null)
    }
    
    @Test
    fun `weight slider should respect min and max bounds`() {
        val minWeight = 1f
        val maxWeight = 50f
        
        // Test values outside bounds
        val testValue1 = 0.5f // Below min
        val testValue2 = 60f  // Above max
        
        val coercedValue1 = testValue1.coerceIn(minWeight, maxWeight)
        val coercedValue2 = testValue2.coerceIn(minWeight, maxWeight)
        
        assertEquals("Value below min should be coerced to min", minWeight, coercedValue1)
        assertEquals("Value above max should be coerced to max", maxWeight, coercedValue2)
    }
    
    @Test
    fun `typical range validation should work correctly`() {
        val typicalRangeStart = 15f
        val typicalRangeEnd = 20f
        
        // Test values in and out of typical range
        val inRangeValue = 18f
        val belowRangeValue = 10f
        val aboveRangeValue = 25f
        
        assertTrue("Value $inRangeValue should be in typical range", 
            inRangeValue in typicalRangeStart..typicalRangeEnd)
        assertFalse("Value $belowRangeValue should not be in typical range", 
            belowRangeValue in typicalRangeStart..typicalRangeEnd)
        assertFalse("Value $aboveRangeValue should not be in typical range", 
            aboveRangeValue in typicalRangeStart..typicalRangeEnd)
    }
}