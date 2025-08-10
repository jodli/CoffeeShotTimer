package com.jodli.coffeeshottimer.data.model

import org.junit.Assert.*
import org.junit.Test

class GrinderConfigurationTest {

    @Test
    fun `validate returns valid result for valid configuration`() {
        // Given
        val validConfig = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10
        )

        // When
        val result = validConfig.validate()

        // Then
        assertTrue("Configuration should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result when min equals max`() {
        // Given
        val invalidConfig = GrinderConfiguration(
            scaleMin = 5,
            scaleMax = 5
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain min/max error", 
            result.errors.contains("Minimum scale value must be less than maximum scale value"))
    }

    @Test
    fun `validate returns invalid result when min is greater than max`() {
        // Given
        val invalidConfig = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 5
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain min/max error", 
            result.errors.contains("Minimum scale value must be less than maximum scale value"))
    }

    @Test
    fun `validate returns invalid result for negative minimum value`() {
        // Given
        val invalidConfig = GrinderConfiguration(
            scaleMin = -1,
            scaleMax = 10
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain negative min error", 
            result.errors.contains("Minimum scale value cannot be negative"))
    }

    @Test
    fun `validate accepts zero as minimum value`() {
        // Given
        val validConfig = GrinderConfiguration(
            scaleMin = 0,
            scaleMax = 10
        )

        // When
        val result = validConfig.validate()

        // Then
        assertTrue("Configuration should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for maximum value exceeding 1000`() {
        // Given
        val invalidConfig = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 1001
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain max value error", 
            result.errors.contains("Maximum scale value cannot exceed 1000"))
    }

    @Test
    fun `validate accepts maximum value of exactly 1000`() {
        // Given
        val validConfig = GrinderConfiguration(
            scaleMin = 900,  // Range size = 100, which is exactly the limit
            scaleMax = 1000
        )

        // When
        val result = validConfig.validate()

        // Then
        assertTrue("Configuration should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for range size less than 3`() {
        // Given
        val invalidConfig = GrinderConfiguration(
            scaleMin = 5,
            scaleMax = 7  // Range size = 2
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain range size error", 
            result.errors.any { it.contains("Scale range must have at least 3 steps") })
    }

    @Test
    fun `validate accepts range size of exactly 3`() {
        // Given
        val validConfig = GrinderConfiguration(
            scaleMin = 5,
            scaleMax = 8  // Range size = 3
        )

        // When
        val result = validConfig.validate()

        // Then
        assertTrue("Configuration should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for range size exceeding 100`() {
        // Given
        val invalidConfig = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 102  // Range size = 101
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain range size error", 
            result.errors.any { it.contains("Scale range cannot exceed 100 steps") })
    }

    @Test
    fun `validate accepts range size of exactly 100`() {
        // Given
        val validConfig = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 101  // Range size = 100
        )

        // When
        val result = validConfig.validate()

        // Then
        assertTrue("Configuration should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns multiple errors for multiple invalid fields`() {
        // Given
        val invalidConfig = GrinderConfiguration(
            scaleMin = -1,
            scaleMax = 1001
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should have multiple errors", result.errors.size >= 2)
        assertTrue("Should contain negative min error", 
            result.errors.contains("Minimum scale value cannot be negative"))
        assertTrue("Should contain max value error", 
            result.errors.contains("Maximum scale value cannot exceed 1000"))
    }

    @Test
    fun `getRangeSize calculates correct range size`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 20
        )

        // When
        val result = config.getRangeSize()

        // Then
        assertEquals("Should calculate correct range size", 10, result)
    }

    @Test
    fun `getRangeSize returns 0 for adjacent values`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 10
        )

        // When
        val result = config.getRangeSize()

        // Then
        assertEquals("Should return 0 for equal min/max", 0, result)
    }

    @Test
    fun `getMiddleValue calculates correct middle value for even range`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 20
        )

        // When
        val result = config.getMiddleValue()

        // Then
        assertEquals("Should calculate correct middle value", 15, result)
    }

    @Test
    fun `getMiddleValue calculates correct middle value for odd range`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 21
        )

        // When
        val result = config.getMiddleValue()

        // Then
        assertEquals("Should calculate correct middle value", 15, result)
    }

    @Test
    fun `isValueInRange returns true for value within range`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 20
        )

        // When & Then
        assertTrue("Should accept value at minimum", config.isValueInRange(10))
        assertTrue("Should accept value at maximum", config.isValueInRange(20))
        assertTrue("Should accept value in middle", config.isValueInRange(15))
    }

    @Test
    fun `isValueInRange returns false for value outside range`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 20
        )

        // When & Then
        assertFalse("Should reject value below minimum", config.isValueInRange(9))
        assertFalse("Should reject value above maximum", config.isValueInRange(21))
    }

    @Test
    fun `clampValue returns value within range unchanged`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 20
        )

        // When & Then
        assertEquals("Should not change value at minimum", 10, config.clampValue(10))
        assertEquals("Should not change value at maximum", 20, config.clampValue(20))
        assertEquals("Should not change value in middle", 15, config.clampValue(15))
    }

    @Test
    fun `clampValue clamps value below range to minimum`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 20
        )

        // When
        val result = config.clampValue(5)

        // Then
        assertEquals("Should clamp to minimum", 10, result)
    }

    @Test
    fun `clampValue clamps value above range to maximum`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 10,
            scaleMax = 20
        )

        // When
        val result = config.clampValue(25)

        // Then
        assertEquals("Should clamp to maximum", 20, result)
    }

    @Test
    fun `configuration has default values for optional fields`() {
        // Given
        val config = GrinderConfiguration(
            scaleMin = 1,
            scaleMax = 10
        )

        // Then
        assertNotNull("createdAt should be set", config.createdAt)
        assertNotNull("id should be generated", config.id)
        assertTrue("id should not be empty", config.id.isNotEmpty())
    }

    @Test
    fun `configuration generates unique IDs`() {
        // Given
        val config1 = GrinderConfiguration(scaleMin = 1, scaleMax = 10)
        val config2 = GrinderConfiguration(scaleMin = 1, scaleMax = 10)

        // Then
        assertNotEquals("Configurations should have different IDs", config1.id, config2.id)
    }

    @Test
    fun `common presets are valid configurations`() {
        // Given & When & Then
        GrinderConfiguration.COMMON_PRESETS.forEach { preset ->
            val result = preset.validate()
            assertTrue("Preset ${preset.scaleMin}-${preset.scaleMax} should be valid", result.isValid)
            assertTrue("Preset should have no errors", result.errors.isEmpty())
        }
    }

    @Test
    fun `default configuration is valid`() {
        // Given
        val defaultConfig = GrinderConfiguration.DEFAULT_CONFIGURATION

        // When
        val result = defaultConfig.validate()

        // Then
        assertTrue("Default configuration should be valid", result.isValid)
        assertTrue("Default configuration should have no errors", result.errors.isEmpty())
        assertEquals("Default min should be 1", 1, defaultConfig.scaleMin)
        assertEquals("Default max should be 10", 10, defaultConfig.scaleMax)
    }

    @Test
    fun `common presets contain expected ranges`() {
        // Given
        val presets = GrinderConfiguration.COMMON_PRESETS

        // Then
        assertTrue("Should contain 1-10 preset", 
            presets.any { it.scaleMin == 1 && it.scaleMax == 10 })
        assertTrue("Should contain 30-80 preset", 
            presets.any { it.scaleMin == 30 && it.scaleMax == 80 })
        assertTrue("Should contain 50-60 preset", 
            presets.any { it.scaleMin == 50 && it.scaleMax == 60 })
        assertTrue("Should contain 0-100 preset", 
            presets.any { it.scaleMin == 0 && it.scaleMax == 100 })
    }

    @Test
    fun `common presets have reasonable count`() {
        // Given
        val presets = GrinderConfiguration.COMMON_PRESETS

        // Then
        assertTrue("Should have at least 3 presets", presets.size >= 3)
        assertTrue("Should have at most 10 presets", presets.size <= 10)
    }
}