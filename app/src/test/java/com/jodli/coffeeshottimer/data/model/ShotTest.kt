package com.jodli.coffeeshottimer.data.model

import org.junit.Assert.*
import org.junit.Test

class ShotTest {

    private fun createValidShot(): Shot {
        return Shot(
            beanId = "test-bean-id",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )
    }

    @Test
    fun `validate returns valid result for valid shot`() {
        // Given
        val validShot = createValidShot()

        // When
        val result = validShot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for coffee weight in below minimum`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 0.05)

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain weight in error", 
            result.errors.contains("Coffee input weight must be at least 0.1g"))
    }

    @Test
    fun `validate accepts coffee weight in at minimum boundary`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 0.1)

        // When
        val result = shot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for coffee weight in above maximum`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 50.1)

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain weight in error", 
            result.errors.contains("Coffee input weight cannot exceed 50.0g"))
    }

    @Test
    fun `validate accepts coffee weight in at maximum boundary`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 50.0)

        // When
        val result = shot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for coffee weight out below minimum`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightOut = 0.05)

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain weight out error", 
            result.errors.contains("Coffee output weight must be at least 0.1g"))
    }

    @Test
    fun `validate accepts coffee weight out at minimum boundary`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightOut = 0.1)

        // When
        val result = shot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for coffee weight out above maximum`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightOut = 100.1)

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain weight out error", 
            result.errors.contains("Coffee output weight cannot exceed 100.0g"))
    }

    @Test
    fun `validate accepts coffee weight out at maximum boundary`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightOut = 100.0)

        // When
        val result = shot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for extraction time below minimum`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 4)

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain extraction time error", 
            result.errors.contains("Extraction time must be at least 5 seconds"))
    }

    @Test
    fun `validate accepts extraction time at minimum boundary`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 5)

        // When
        val result = shot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for extraction time above maximum`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 121)

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain extraction time error", 
            result.errors.contains("Extraction time cannot exceed 120 seconds"))
    }

    @Test
    fun `validate accepts extraction time at maximum boundary`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 120)

        // When
        val result = shot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for empty grinder setting`() {
        // Given
        val shot = createValidShot().copy(grinderSetting = "")

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain grinder setting error", 
            result.errors.contains("Grinder setting cannot be empty"))
    }

    @Test
    fun `validate returns invalid result for blank grinder setting`() {
        // Given
        val shot = createValidShot().copy(grinderSetting = "   ")

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain grinder setting error", 
            result.errors.contains("Grinder setting cannot be empty"))
    }

    @Test
    fun `validate returns invalid result for grinder setting exceeding 50 characters`() {
        // Given
        val longGrinderSetting = "a".repeat(51)
        val shot = createValidShot().copy(grinderSetting = longGrinderSetting)

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain grinder setting length error", 
            result.errors.contains("Grinder setting cannot exceed 50 characters"))
    }

    @Test
    fun `validate accepts grinder setting with exactly 50 characters`() {
        // Given
        val maxLengthGrinderSetting = "a".repeat(50)
        val shot = createValidShot().copy(grinderSetting = maxLengthGrinderSetting)

        // When
        val result = shot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for empty bean ID`() {
        // Given
        val shot = createValidShot().copy(beanId = "")

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain bean ID error", 
            result.errors.contains("Bean ID cannot be empty"))
    }

    @Test
    fun `validate returns invalid result for blank bean ID`() {
        // Given
        val shot = createValidShot().copy(beanId = "   ")

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain bean ID error", 
            result.errors.contains("Bean ID cannot be empty"))
    }

    @Test
    fun `validate returns invalid result for notes exceeding 500 characters`() {
        // Given
        val longNotes = "a".repeat(501)
        val shot = createValidShot().copy(notes = longNotes)

        // When
        val result = shot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertTrue("Should contain notes length error", 
            result.errors.contains("Notes cannot exceed 500 characters"))
    }

    @Test
    fun `validate accepts notes with exactly 500 characters`() {
        // Given
        val maxLengthNotes = "a".repeat(500)
        val shot = createValidShot().copy(notes = maxLengthNotes)

        // When
        val result = shot.validate()

        // Then
        assertTrue("Shot should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns multiple errors for multiple invalid fields`() {
        // Given
        val invalidShot = Shot(
            beanId = "",
            coffeeWeightIn = 0.05,
            coffeeWeightOut = 100.1,
            extractionTimeSeconds = 4,
            grinderSetting = "",
            notes = "a".repeat(501)
        )

        // When
        val result = invalidShot.validate()

        // Then
        assertFalse("Shot should be invalid", result.isValid)
        assertEquals("Should have 6 errors", 6, result.errors.size)
        assertTrue("Should contain bean ID error", result.errors.contains("Bean ID cannot be empty"))
        assertTrue("Should contain weight in error", 
            result.errors.contains("Coffee input weight must be at least 0.1g"))
        assertTrue("Should contain weight out error", 
            result.errors.contains("Coffee output weight cannot exceed 100.0g"))
        assertTrue("Should contain extraction time error", 
            result.errors.contains("Extraction time must be at least 5 seconds"))
        assertTrue("Should contain grinder setting error", 
            result.errors.contains("Grinder setting cannot be empty"))
        assertTrue("Should contain notes length error", 
            result.errors.contains("Notes cannot exceed 500 characters"))
    }

    @Test
    fun `brewRatio calculates correct ratio`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 18.0, coffeeWeightOut = 36.0)

        // When
        val ratio = shot.brewRatio

        // Then
        assertEquals("Should calculate correct brew ratio", 2.0, ratio, 0.01)
    }

    @Test
    fun `brewRatio rounds to 2 decimal places`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 18.0, coffeeWeightOut = 45.0)

        // When
        val ratio = shot.brewRatio

        // Then
        assertEquals("Should round to 2 decimal places", 2.5, ratio, 0.01)
    }

    @Test
    fun `brewRatio handles complex decimal calculation`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 18.5, coffeeWeightOut = 37.3)

        // When
        val ratio = shot.brewRatio

        // Then
        assertEquals("Should handle complex decimal calculation", 2.02, ratio, 0.01)
    }

    @Test
    fun `isOptimalExtractionTime returns true for 25 seconds`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 25)

        // When
        val result = shot.isOptimalExtractionTime()

        // Then
        assertTrue("25 seconds should be optimal", result)
    }

    @Test
    fun `isOptimalExtractionTime returns true for 30 seconds`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 30)

        // When
        val result = shot.isOptimalExtractionTime()

        // Then
        assertTrue("30 seconds should be optimal", result)
    }

    @Test
    fun `isOptimalExtractionTime returns true for 28 seconds`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 28)

        // When
        val result = shot.isOptimalExtractionTime()

        // Then
        assertTrue("28 seconds should be optimal", result)
    }

    @Test
    fun `isOptimalExtractionTime returns false for 24 seconds`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 24)

        // When
        val result = shot.isOptimalExtractionTime()

        // Then
        assertFalse("24 seconds should not be optimal", result)
    }

    @Test
    fun `isOptimalExtractionTime returns false for 31 seconds`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 31)

        // When
        val result = shot.isOptimalExtractionTime()

        // Then
        assertFalse("31 seconds should not be optimal", result)
    }

    @Test
    fun `isTypicalBrewRatio returns true for ratio 1_5`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 20.0, coffeeWeightOut = 30.0) // 1:1.5

        // When
        val result = shot.isTypicalBrewRatio()

        // Then
        assertTrue("1:1.5 ratio should be typical", result)
    }

    @Test
    fun `isTypicalBrewRatio returns true for ratio 3_0`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 18.0, coffeeWeightOut = 54.0) // 1:3.0

        // When
        val result = shot.isTypicalBrewRatio()

        // Then
        assertTrue("1:3.0 ratio should be typical", result)
    }

    @Test
    fun `isTypicalBrewRatio returns true for ratio 2_0`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 18.0, coffeeWeightOut = 36.0) // 1:2.0

        // When
        val result = shot.isTypicalBrewRatio()

        // Then
        assertTrue("1:2.0 ratio should be typical", result)
    }

    @Test
    fun `isTypicalBrewRatio returns false for ratio below 1_5`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 20.0, coffeeWeightOut = 29.0) // 1:1.45

        // When
        val result = shot.isTypicalBrewRatio()

        // Then
        assertFalse("1:1.45 ratio should not be typical", result)
    }

    @Test
    fun `isTypicalBrewRatio returns false for ratio above 3_0`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 18.0, coffeeWeightOut = 55.0) // 1:3.06

        // When
        val result = shot.isTypicalBrewRatio()

        // Then
        assertFalse("1:3.06 ratio should not be typical", result)
    }

    @Test
    fun `getFormattedBrewRatio returns correct format for whole number ratio`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 18.0, coffeeWeightOut = 36.0) // 1:2.0

        // When
        val formatted = shot.getFormattedBrewRatio()

        // Then
        assertEquals("Should format whole number ratio correctly", "1:2.0", formatted)
    }

    @Test
    fun `getFormattedBrewRatio returns correct format for decimal ratio`() {
        // Given
        val shot = createValidShot().copy(coffeeWeightIn = 18.0, coffeeWeightOut = 45.0) // 1:2.5

        // When
        val formatted = shot.getFormattedBrewRatio()

        // Then
        assertEquals("Should format decimal ratio correctly", "1:2.5", formatted)
    }

    @Test
    fun `getFormattedExtractionTime formats seconds correctly`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 28)

        // When
        val formatted = shot.getFormattedExtractionTime()

        // Then
        assertEquals("Should format seconds correctly", "28s", formatted)
    }

    @Test
    fun `getFormattedExtractionTime formats minutes and seconds correctly`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 95) // 1:35

        // When
        val formatted = shot.getFormattedExtractionTime()

        // Then
        assertEquals("Should format minutes and seconds correctly", "01:35", formatted)
    }

    @Test
    fun `getFormattedExtractionTime pads single digits with zeros`() {
        // Given
        val shot = createValidShot().copy(extractionTimeSeconds = 65) // 1:05

        // When
        val formatted = shot.getFormattedExtractionTime()

        // Then
        assertEquals("Should pad single digits with zeros", "01:05", formatted)
    }

    @Test
    fun `shot has default values for optional fields`() {
        // Given
        val shot = Shot(
            beanId = "test-bean-id",
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 28,
            grinderSetting = "15"
        )

        // Then
        assertEquals("Notes should default to empty string", "", shot.notes)
        assertNotNull("timestamp should be set", shot.timestamp)
        assertNotNull("id should be generated", shot.id)
        assertTrue("id should not be empty", shot.id.isNotEmpty())
    }

    @Test
    fun `shot generates unique IDs`() {
        // Given
        val shot1 = createValidShot()
        val shot2 = createValidShot()

        // Then
        assertNotEquals("Shots should have different IDs", shot1.id, shot2.id)
    }
}