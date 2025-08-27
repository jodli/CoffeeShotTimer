package com.jodli.coffeeshottimer.data.model

import org.junit.Assert.*
import org.junit.Test

class BasketConfigurationTest {

    @Test
    fun `validate returns valid result for valid configuration`() {
        // Given
        val validConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 20f,
            coffeeOutMin = 20f,
            coffeeOutMax = 50f
        )

        // When
        val result = validConfig.validate()

        // Then
        assertTrue("Configuration should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result when coffee in min is less than 5g`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 4.5f,
            coffeeInMax = 20f,
            coffeeOutMin = 20f,
            coffeeOutMax = 50f
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain coffee in min error", 
            result.errors.contains("Coffee in minimum cannot be less than 5g"))
    }

    @Test
    fun `validate returns invalid result when coffee in max exceeds 30g`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 31f,
            coffeeOutMin = 20f,
            coffeeOutMax = 50f
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain coffee in max error", 
            result.errors.contains("Coffee in maximum cannot exceed 30g"))
    }

    @Test
    fun `validate returns invalid result when coffee in min equals or exceeds max`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 20f,
            coffeeInMax = 20f,
            coffeeOutMin = 20f,
            coffeeOutMax = 50f
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain min/max error", 
            result.errors.contains("Coffee in minimum must be less than maximum"))
    }

    @Test
    fun `validate returns invalid result when coffee out min is less than 10g`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 20f,
            coffeeOutMin = 9f,
            coffeeOutMax = 50f
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain coffee out min error", 
            result.errors.contains("Coffee out minimum cannot be less than 10g"))
    }

    @Test
    fun `validate returns invalid result when coffee out max exceeds 80g`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 20f,
            coffeeOutMin = 20f,
            coffeeOutMax = 81f
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain coffee out max error", 
            result.errors.contains("Coffee out maximum cannot exceed 80g"))
    }

    @Test
    fun `validate returns invalid result when coffee out min equals or exceeds max`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 20f,
            coffeeOutMin = 50f,
            coffeeOutMax = 50f
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain min/max error", 
            result.errors.contains("Coffee out minimum must be less than maximum"))
    }

    @Test
    fun `validate returns invalid result when coffee in range is less than 3g`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 12f,  // Range = 2g
            coffeeOutMin = 20f,
            coffeeOutMax = 50f
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain range size error", 
            result.errors.contains("Coffee in range must be at least 3g"))
    }

    @Test
    fun `validate returns invalid result when coffee out range is less than 10g`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 20f,
            coffeeOutMin = 20f,
            coffeeOutMax = 29f  // Range = 9g
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain range size error", 
            result.errors.contains("Coffee out range must be at least 10g"))
    }

    @Test
    fun `validate returns invalid result when brew ratio is unreasonable`() {
        // Given
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 20f,
            coffeeOutMin = 10f,  // 10g out / 20g in = 0.5 ratio < 0.8
            coffeeOutMax = 30f
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should contain ratio error", 
            result.errors.contains("Weight ranges don't allow reasonable brew ratios"))
    }

    @Test
    fun `validate accepts edge case brew ratio of exactly 0_8`() {
        // Given
        val validConfig = BasketConfiguration(
            coffeeInMin = 10f,
            coffeeInMax = 20f,
            coffeeOutMin = 16f,  // 16g out / 20g in = 0.8 ratio (exactly)
            coffeeOutMax = 50f
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
        val invalidConfig = BasketConfiguration(
            coffeeInMin = 3f,   // Too low
            coffeeInMax = 35f,  // Too high
            coffeeOutMin = 5f,  // Too low
            coffeeOutMax = 90f  // Too high
        )

        // When
        val result = invalidConfig.validate()

        // Then
        assertFalse("Configuration should be invalid", result.isValid)
        assertTrue("Should have multiple errors", result.errors.size >= 4)
    }

    @Test
    fun `getCoffeeInRangeSize calculates correct range size`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When
        val result = config.getCoffeeInRangeSize()

        // Then
        assertEquals("Should calculate correct coffee in range size", 8f, result, 0.01f)
    }

    @Test
    fun `getCoffeeOutRangeSize calculates correct range size`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When
        val result = config.getCoffeeOutRangeSize()

        // Then
        assertEquals("Should calculate correct coffee out range size", 27f, result, 0.01f)
    }

    @Test
    fun `getCoffeeInMiddleValue calculates correct middle value`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When
        val result = config.getCoffeeInMiddleValue()

        // Then
        assertEquals("Should calculate correct coffee in middle value", 18f, result, 0.01f)
    }

    @Test
    fun `getCoffeeOutMiddleValue calculates correct middle value`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When
        val result = config.getCoffeeOutMiddleValue()

        // Then
        assertEquals("Should calculate correct coffee out middle value", 41.5f, result, 0.01f)
    }

    @Test
    fun `isCoffeeInValueInRange returns true for value within range`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When & Then
        assertTrue("Should accept value at minimum", config.isCoffeeInValueInRange(14f))
        assertTrue("Should accept value at maximum", config.isCoffeeInValueInRange(22f))
        assertTrue("Should accept value in middle", config.isCoffeeInValueInRange(18f))
    }

    @Test
    fun `isCoffeeInValueInRange returns false for value outside range`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When & Then
        assertFalse("Should reject value below minimum", config.isCoffeeInValueInRange(13.9f))
        assertFalse("Should reject value above maximum", config.isCoffeeInValueInRange(22.1f))
    }

    @Test
    fun `isCoffeeOutValueInRange returns true for value within range`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When & Then
        assertTrue("Should accept value at minimum", config.isCoffeeOutValueInRange(28f))
        assertTrue("Should accept value at maximum", config.isCoffeeOutValueInRange(55f))
        assertTrue("Should accept value in middle", config.isCoffeeOutValueInRange(40f))
    }

    @Test
    fun `isCoffeeOutValueInRange returns false for value outside range`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When & Then
        assertFalse("Should reject value below minimum", config.isCoffeeOutValueInRange(27.9f))
        assertFalse("Should reject value above maximum", config.isCoffeeOutValueInRange(55.1f))
    }

    @Test
    fun `clampCoffeeInValue clamps value to range`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When & Then
        assertEquals("Should clamp below minimum", 14f, config.clampCoffeeInValue(10f), 0.01f)
        assertEquals("Should clamp above maximum", 22f, config.clampCoffeeInValue(25f), 0.01f)
        assertEquals("Should not change value in range", 18f, config.clampCoffeeInValue(18f), 0.01f)
    }

    @Test
    fun `clampCoffeeOutValue clamps value to range`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // When & Then
        assertEquals("Should clamp below minimum", 28f, config.clampCoffeeOutValue(20f), 0.01f)
        assertEquals("Should clamp above maximum", 55f, config.clampCoffeeOutValue(60f), 0.01f)
        assertEquals("Should not change value in range", 40f, config.clampCoffeeOutValue(40f), 0.01f)
    }

    @Test
    fun `configuration has default values for optional fields`() {
        // Given
        val config = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // Then
        assertNotNull("createdAt should be set", config.createdAt)
        assertNotNull("id should be generated", config.id)
        assertTrue("id should not be empty", config.id.isNotEmpty())
        assertTrue("isActive should default to true", config.isActive)
    }

    @Test
    fun `configuration generates unique IDs`() {
        // Given
        val config1 = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )
        val config2 = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        // Then
        assertNotEquals("Configurations should have different IDs", config1.id, config2.id)
    }

    @Test
    fun `SINGLE_SHOT preset has correct values and is valid`() {
        // Given
        val singleShot = BasketConfiguration.SINGLE_SHOT

        // When
        val result = singleShot.validate()

        // Then
        assertEquals("Single shot coffee in min should be 7g", 7f, singleShot.coffeeInMin, 0.01f)
        assertEquals("Single shot coffee in max should be 12g", 12f, singleShot.coffeeInMax, 0.01f)
        assertEquals("Single shot coffee out min should be 20g", 20f, singleShot.coffeeOutMin, 0.01f)
        assertEquals("Single shot coffee out max should be 40g", 40f, singleShot.coffeeOutMax, 0.01f)
        assertTrue("Single shot preset should be valid", result.isValid)
        assertTrue("Single shot preset should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `DOUBLE_SHOT preset has correct values and is valid`() {
        // Given
        val doubleShot = BasketConfiguration.DOUBLE_SHOT

        // When
        val result = doubleShot.validate()

        // Then
        assertEquals("Double shot coffee in min should be 14g", 14f, doubleShot.coffeeInMin, 0.01f)
        assertEquals("Double shot coffee in max should be 22g", 22f, doubleShot.coffeeInMax, 0.01f)
        assertEquals("Double shot coffee out min should be 28g", 28f, doubleShot.coffeeOutMin, 0.01f)
        assertEquals("Double shot coffee out max should be 55g", 55f, doubleShot.coffeeOutMax, 0.01f)
        assertTrue("Double shot preset should be valid", result.isValid)
        assertTrue("Double shot preset should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `DEFAULT configuration is DOUBLE_SHOT`() {
        // Given
        val default = BasketConfiguration.DEFAULT
        val doubleShot = BasketConfiguration.DOUBLE_SHOT

        // Then
        assertEquals("Default should match double shot in coffee in min", 
            doubleShot.coffeeInMin, default.coffeeInMin, 0.01f)
        assertEquals("Default should match double shot in coffee in max", 
            doubleShot.coffeeInMax, default.coffeeInMax, 0.01f)
        assertEquals("Default should match double shot in coffee out min", 
            doubleShot.coffeeOutMin, default.coffeeOutMin, 0.01f)
        assertEquals("Default should match double shot in coffee out max", 
            doubleShot.coffeeOutMax, default.coffeeOutMax, 0.01f)
    }

    @Test
    fun `BasketPreset enum contains expected values`() {
        // Given
        val presetValues = BasketPreset.values()

        // Then
        assertEquals("Should have 2 presets", 2, presetValues.size)
        assertTrue("Should contain SINGLE", presetValues.contains(BasketPreset.SINGLE))
        assertTrue("Should contain DOUBLE", presetValues.contains(BasketPreset.DOUBLE))
    }

    @Test
    fun `presets allow typical brew ratios`() {
        // Given
        val singleShot = BasketConfiguration.SINGLE_SHOT
        val doubleShot = BasketConfiguration.DOUBLE_SHOT

        // When & Then for single shot
        // 1:2 ratio (ristretto): 10g in -> 20g out (both in range)
        assertTrue("Single shot should allow 1:2 ratio", 
            singleShot.isCoffeeInValueInRange(10f) && singleShot.isCoffeeOutValueInRange(20f))
        // 1:3 ratio (normale): 10g in -> 30g out (both in range)
        assertTrue("Single shot should allow 1:3 ratio", 
            singleShot.isCoffeeInValueInRange(10f) && singleShot.isCoffeeOutValueInRange(30f))

        // When & Then for double shot
        // 1:2 ratio (ristretto): 18g in -> 36g out (both in range)
        assertTrue("Double shot should allow 1:2 ratio", 
            doubleShot.isCoffeeInValueInRange(18f) && doubleShot.isCoffeeOutValueInRange(36f))
        // 1:2.5 ratio (normale): 18g in -> 45g out (both in range)
        assertTrue("Double shot should allow 1:2.5 ratio", 
            doubleShot.isCoffeeInValueInRange(18f) && doubleShot.isCoffeeOutValueInRange(45f))
    }
}
