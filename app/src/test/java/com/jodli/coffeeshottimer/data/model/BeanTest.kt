package com.jodli.coffeeshottimer.data.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BeanTest {

    @Test
    fun `validate returns valid result for valid bean`() {
        // Given
        val validBean = Bean(
            name = "Ethiopian Yirgacheffe",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Floral and citrusy notes"
        )

        // When
        val result = validBean.validate()

        // Then
        assertTrue("Bean should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for empty bean name`() {
        // Given
        val beanWithEmptyName = Bean(
            name = "",
            roastDate = LocalDate.now().minusDays(7)
        )

        // When
        val result = beanWithEmptyName.validate()

        // Then
        assertFalse("Bean should be invalid", result.isValid)
        assertTrue("Should contain name error", result.errors.contains("Bean name cannot be empty"))
    }

    @Test
    fun `validate returns invalid result for blank bean name`() {
        // Given
        val beanWithBlankName = Bean(
            name = "   ",
            roastDate = LocalDate.now().minusDays(7)
        )

        // When
        val result = beanWithBlankName.validate()

        // Then
        assertFalse("Bean should be invalid", result.isValid)
        assertTrue("Should contain name error", result.errors.contains("Bean name cannot be empty"))
    }

    @Test
    fun `validate returns invalid result for bean name exceeding 100 characters`() {
        // Given
        val longName = "a".repeat(101)
        val beanWithLongName = Bean(
            name = longName,
            roastDate = LocalDate.now().minusDays(7)
        )

        // When
        val result = beanWithLongName.validate()

        // Then
        assertFalse("Bean should be invalid", result.isValid)
        assertTrue("Should contain name length error", 
            result.errors.contains("Bean name cannot exceed 100 characters"))
    }

    @Test
    fun `validate accepts bean name with exactly 100 characters`() {
        // Given
        val maxLengthName = "a".repeat(100)
        val beanWithMaxName = Bean(
            name = maxLengthName,
            roastDate = LocalDate.now().minusDays(7)
        )

        // When
        val result = beanWithMaxName.validate()

        // Then
        assertTrue("Bean should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for future roast date`() {
        // Given
        val beanWithFutureDate = Bean(
            name = "Future Bean",
            roastDate = LocalDate.now().plusDays(1)
        )

        // When
        val result = beanWithFutureDate.validate()

        // Then
        assertFalse("Bean should be invalid", result.isValid)
        assertTrue("Should contain future date error", 
            result.errors.contains("Roast date cannot be in the future"))
    }

    @Test
    fun `validate returns invalid result for roast date more than 365 days ago`() {
        // Given
        val beanWithOldDate = Bean(
            name = "Old Bean",
            roastDate = LocalDate.now().minusDays(366)
        )

        // When
        val result = beanWithOldDate.validate()

        // Then
        assertFalse("Bean should be invalid", result.isValid)
        assertTrue("Should contain old date error", 
            result.errors.contains("Roast date cannot be more than 365 days ago"))
    }

    @Test
    fun `validate accepts roast date exactly 365 days ago`() {
        // Given
        val beanWithOldestValidDate = Bean(
            name = "Old But Valid Bean",
            roastDate = LocalDate.now().minusDays(365)
        )

        // When
        val result = beanWithOldestValidDate.validate()

        // Then
        assertTrue("Bean should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns invalid result for notes exceeding 500 characters`() {
        // Given
        val longNotes = "a".repeat(501)
        val beanWithLongNotes = Bean(
            name = "Bean with Long Notes",
            roastDate = LocalDate.now().minusDays(7),
            notes = longNotes
        )

        // When
        val result = beanWithLongNotes.validate()

        // Then
        assertFalse("Bean should be invalid", result.isValid)
        assertTrue("Should contain notes length error", 
            result.errors.contains("Notes cannot exceed 500 characters"))
    }

    @Test
    fun `validate accepts notes with exactly 500 characters`() {
        // Given
        val maxLengthNotes = "a".repeat(500)
        val beanWithMaxNotes = Bean(
            name = "Bean with Max Notes",
            roastDate = LocalDate.now().minusDays(7),
            notes = maxLengthNotes
        )

        // When
        val result = beanWithMaxNotes.validate()

        // Then
        assertTrue("Bean should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun `validate returns multiple errors for multiple invalid fields`() {
        // Given
        val invalidBean = Bean(
            name = "",
            roastDate = LocalDate.now().plusDays(1),
            notes = "a".repeat(501)
        )

        // When
        val result = invalidBean.validate()

        // Then
        assertFalse("Bean should be invalid", result.isValid)
        assertEquals("Should have 3 errors", 3, result.errors.size)
        assertTrue("Should contain name error", result.errors.contains("Bean name cannot be empty"))
        assertTrue("Should contain future date error", 
            result.errors.contains("Roast date cannot be in the future"))
        assertTrue("Should contain notes length error", 
            result.errors.contains("Notes cannot exceed 500 characters"))
    }

    @Test
    fun `daysSinceRoast calculates correct days for recent roast`() {
        // Given
        val daysAgo = 7L
        val bean = Bean(
            name = "Recent Bean",
            roastDate = LocalDate.now().minusDays(daysAgo)
        )

        // When
        val result = bean.daysSinceRoast()

        // Then
        assertEquals("Should calculate correct days since roast", daysAgo, result)
    }

    @Test
    fun `daysSinceRoast returns 0 for today's roast`() {
        // Given
        val bean = Bean(
            name = "Today's Bean",
            roastDate = LocalDate.now()
        )

        // When
        val result = bean.daysSinceRoast()

        // Then
        assertEquals("Should return 0 for today's roast", 0L, result)
    }

    @Test
    fun `isFresh returns true for bean roasted 7 days ago`() {
        // Given
        val bean = Bean(
            name = "Fresh Bean",
            roastDate = LocalDate.now().minusDays(7)
        )

        // When
        val result = bean.isFresh()

        // Then
        assertTrue("Bean roasted 7 days ago should be fresh", result)
    }

    @Test
    fun `isFresh returns true for bean roasted 4 days ago`() {
        // Given
        val bean = Bean(
            name = "Fresh Bean",
            roastDate = LocalDate.now().minusDays(4)
        )

        // When
        val result = bean.isFresh()

        // Then
        assertTrue("Bean roasted 4 days ago should be fresh", result)
    }

    @Test
    fun `isFresh returns true for bean roasted 21 days ago`() {
        // Given
        val bean = Bean(
            name = "Fresh Bean",
            roastDate = LocalDate.now().minusDays(21)
        )

        // When
        val result = bean.isFresh()

        // Then
        assertTrue("Bean roasted 21 days ago should be fresh", result)
    }

    @Test
    fun `isFresh returns false for bean roasted 3 days ago`() {
        // Given
        val bean = Bean(
            name = "Too Fresh Bean",
            roastDate = LocalDate.now().minusDays(3)
        )

        // When
        val result = bean.isFresh()

        // Then
        assertFalse("Bean roasted 3 days ago should not be fresh (too fresh)", result)
    }

    @Test
    fun `isFresh returns false for bean roasted 22 days ago`() {
        // Given
        val bean = Bean(
            name = "Old Bean",
            roastDate = LocalDate.now().minusDays(22)
        )

        // When
        val result = bean.isFresh()

        // Then
        assertFalse("Bean roasted 22 days ago should not be fresh (too old)", result)
    }

    @Test
    fun `isFresh returns false for bean roasted today`() {
        // Given
        val bean = Bean(
            name = "Today's Bean",
            roastDate = LocalDate.now()
        )

        // When
        val result = bean.isFresh()

        // Then
        assertFalse("Bean roasted today should not be fresh (too fresh)", result)
    }

    @Test
    fun `bean has default values for optional fields`() {
        // Given
        val bean = Bean(
            name = "Simple Bean",
            roastDate = LocalDate.now().minusDays(7)
        )

        // Then
        assertEquals("Notes should default to empty string", "", bean.notes)
        assertTrue("isActive should default to true", bean.isActive)
        assertNull("lastGrinderSetting should default to null", bean.lastGrinderSetting)
        assertNotNull("createdAt should be set", bean.createdAt)
        assertNotNull("id should be generated", bean.id)
        assertTrue("id should not be empty", bean.id.isNotEmpty())
    }

    @Test
    fun `bean generates unique IDs`() {
        // Given
        val bean1 = Bean(name = "Bean 1", roastDate = LocalDate.now().minusDays(7))
        val bean2 = Bean(name = "Bean 2", roastDate = LocalDate.now().minusDays(7))

        // Then
        assertNotEquals("Beans should have different IDs", bean1.id, bean2.id)
    }
}