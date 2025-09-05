package com.jodli.coffeeshottimer.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GrinderConfiguration step size functionality
 */
class GrinderConfigurationStepSizeTest {

    @Test
    fun `validate accepts valid step size`() {
        val config = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10,
            stepSize = 0.5
        )
        val result = config.validate()
        assertTrue("Valid step size should pass validation", result.isValid)
    }

    @Test
    fun `validate rejects step size below minimum`() {
        val config = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10,
            stepSize = 0.005 // Below 0.01 minimum
        )
        val result = config.validate()
        assertFalse("Step size below 0.01 should fail validation", result.isValid)
        assertTrue(
            "Error message should mention step size minimum",
            result.errors.any { it.contains("Step size must be at least 0.01") }
        )
    }

    @Test
    fun `validate rejects step size above maximum`() {
        val config = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10,
            stepSize = 15.0 // Above 10.0 maximum
        )
        val result = config.validate()
        assertFalse("Step size above 10.0 should fail validation", result.isValid)
        assertTrue(
            "Error message should mention step size maximum",
            result.errors.any { it.contains("Step size cannot exceed 10.0") }
        )
    }

    @Test
    fun `validate rejects step size larger than range`() {
        val config = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 5,
            stepSize = 5.0 // Larger than range of 4
        )
        val result = config.validate()
        assertFalse("Step size larger than range should fail validation", result.isValid)
        assertTrue(
            "Error message should mention step size too large for range",
            result.errors.any { it.contains("Step size cannot be larger than the range") }
        )
    }

    @Test
    fun `getValidGrindValues returns correct values for step size`() {
        val config = GrinderConfiguration(
            scaleMin = 0,
            scaleMax = 10,
            stepSize = 2.5
        )
        val values = config.getValidGrindValues()
        
        val expected = listOf(0.0, 2.5, 5.0, 7.5, 10.0)
        assertEquals("Should return correct step values", expected, values)
    }

    @Test
    fun `getValidGrindValues handles non-divisible ranges`() {
        val config = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10,
            stepSize = 0.3
        )
        val values = config.getValidGrindValues()
        
        assertTrue("Should start at minimum", values.first() == 1.0)
        assertTrue("Should not exceed maximum", values.last() <= 10.0)
        
        // Check that steps are consistent
        for (i in 1 until values.size) {
            val diff = values[i] - values[i - 1]
            assertEquals("Step size should be consistent", 0.3, diff, 0.001)
        }
    }

    @Test
    fun `roundToNearestStep rounds correctly`() {
        val config = GrinderConfiguration(
            scaleMin = 0,
            scaleMax = 10,
            stepSize = 0.5
        )
        
        assertEquals(2.5, config.roundToNearestStep(2.4), 0.001)
        assertEquals(2.5, config.roundToNearestStep(2.6), 0.001)
        assertEquals(3.0, config.roundToNearestStep(2.8), 0.001)
        
        // Test boundaries
        assertEquals(0.0, config.roundToNearestStep(-1.0), 0.001)
        assertEquals(10.0, config.roundToNearestStep(11.0), 0.001)
    }

    @Test
    fun `formatGrindValue formats according to step size`() {
        // Whole number step size
        val config1 = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10,
            stepSize = 1.0
        )
        assertEquals("5", config1.formatGrindValue(5.0))
        
        // Half step size
        val config2 = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10,
            stepSize = 0.5
        )
        assertEquals("5.5", config2.formatGrindValue(5.5))
        assertEquals("5.0", config2.formatGrindValue(5.0))
        
        // Tenth step size
        val config3 = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10,
            stepSize = 0.1
        )
        assertEquals("5.6", config3.formatGrindValue(5.55))
        assertEquals("5.5", config3.formatGrindValue(5.50))
    }

    @Test
    fun `STEP_SIZE_PRESETS contains common values`() {
        val presets = GrinderConfiguration.STEP_SIZE_PRESETS
        
        assertTrue("Should contain 0.1", 0.1 in presets)
        assertTrue("Should contain 0.2", 0.2 in presets)
        assertTrue("Should not contain 0.25 anymore", 0.25 !in presets)
        assertTrue("Should contain 0.5", 0.5 in presets)
        assertTrue("Should contain 1.0", 1.0 in presets)
        assertEquals("Should have 4 presets", 4, presets.size)
    }

    @Test
    fun `DEFAULT_CONFIGURATION has step size`() {
        val defaultConfig = GrinderConfiguration.DEFAULT_CONFIGURATION
        
        assertEquals("Default step size should be 0.5", 0.5, defaultConfig.stepSize, 0.001)
        assertTrue("Default config should be valid", defaultConfig.validate().isValid)
    }

    @Test
    fun `COMMON_PRESETS all have valid step sizes`() {
        GrinderConfiguration.COMMON_PRESETS.forEach { preset ->
            val result = preset.validate()
            assertTrue(
                "Preset with range ${preset.scaleMin}-${preset.scaleMax} and step ${preset.stepSize} should be valid",
                result.isValid
            )
        }
    }
}
