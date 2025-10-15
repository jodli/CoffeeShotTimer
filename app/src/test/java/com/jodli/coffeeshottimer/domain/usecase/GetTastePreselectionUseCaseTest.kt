package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.domain.model.TastePrimary
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetTastePreselectionUseCase.
 * Tests the extraction time to taste mapping logic according to the specification:
 * - < 25s → SOUR (under-extracted)
 * - 25-30s → PERFECT (optimal range)
 * - > 30s → BITTER (over-extracted)
 */
class GetTastePreselectionUseCaseTest {

    private lateinit var useCase: GetTastePreselectionUseCase

    @Before
    fun setup() {
        useCase = GetTastePreselectionUseCase()
    }

    @Test
    fun `invoke with null extraction time returns null`() {
        // Given
        val extractionTime: Double? = null

        // When
        val result = useCase(extractionTime)

        // Then
        assertNull(result)
    }

    @Test
    fun `invoke with zero extraction time returns null`() {
        // Given
        val extractionTime = 0.0

        // When
        val result = useCase(extractionTime)

        // Then
        assertNull(result)
    }

    @Test
    fun `invoke with negative extraction time returns null`() {
        // Given
        val extractionTime = -5.0

        // When
        val result = useCase(extractionTime)

        // Then
        assertNull(result)
    }

    @Test
    fun `invoke with extraction time under 25 seconds returns SOUR`() {
        // Test boundary and typical under-extracted times
        val underExtractedTimes = listOf(5.0, 15.0, 20.0, 24.0, 24.9)

        underExtractedTimes.forEach { time ->
            // When
            val result = useCase(time)

            // Then
            assertEquals("Expected SOUR for ${time}s extraction", TastePrimary.SOUR, result)
        }
    }

    @Test
    fun `invoke with extraction time exactly 25 seconds returns PERFECT`() {
        // Given
        val extractionTime = 25.0

        // When
        val result = useCase(extractionTime)

        // Then
        assertEquals(TastePrimary.PERFECT, result)
    }

    @Test
    fun `invoke with extraction time between 25 and 30 seconds returns PERFECT`() {
        // Test optimal extraction times
        val optimalTimes = listOf(25.0, 25.5, 27.0, 28.5, 30.0)

        optimalTimes.forEach { time ->
            // When
            val result = useCase(time)

            // Then
            assertEquals("Expected PERFECT for ${time}s extraction", TastePrimary.PERFECT, result)
        }
    }

    @Test
    fun `invoke with extraction time exactly 30 seconds returns PERFECT`() {
        // Given
        val extractionTime = 30.0

        // When
        val result = useCase(extractionTime)

        // Then
        assertEquals(TastePrimary.PERFECT, result)
    }

    @Test
    fun `invoke with extraction time over 30 seconds returns BITTER`() {
        // Test over-extracted times
        val overExtractedTimes = listOf(30.1, 35.0, 40.0, 45.0, 60.0)

        overExtractedTimes.forEach { time ->
            // When
            val result = useCase(time)

            // Then
            assertEquals("Expected BITTER for ${time}s extraction", TastePrimary.BITTER, result)
        }
    }

    @Test
    fun `invoke with Int parameter delegates to Double version`() {
        // Given
        val extractionTimeInt = 28

        // When
        val result = useCase(extractionTimeInt)

        // Then
        assertEquals(TastePrimary.PERFECT, result)
    }

    @Test
    fun `invoke with null Int parameter returns null`() {
        // Given
        val extractionTime: Int? = null

        // When
        val result = useCase(extractionTime)

        // Then
        assertNull(result)
    }

    @Test
    fun `boundary conditions are handled correctly`() {
        // Test exact boundary conditions
        val testCases = mapOf(
            24.999 to TastePrimary.SOUR,
            25.0 to TastePrimary.PERFECT,
            25.001 to TastePrimary.PERFECT,
            29.999 to TastePrimary.PERFECT,
            30.0 to TastePrimary.PERFECT,
            30.001 to TastePrimary.BITTER
        )

        testCases.forEach { (time, expected) ->
            // When
            val result = useCase(time)

            // Then
            assertEquals("Boundary condition failed for ${time}s", expected, result)
        }
    }

    @Test
    fun `extreme values are handled correctly`() {
        // Test very small and very large values
        val extremeCases = mapOf(
            0.1 to TastePrimary.SOUR,
            1.0 to TastePrimary.SOUR,
            100.0 to TastePrimary.BITTER,
            300.0 to TastePrimary.BITTER
        )

        extremeCases.forEach { (time, expected) ->
            // When
            val result = useCase(time)

            // Then
            assertEquals("Extreme value failed for ${time}s", expected, result)
        }
    }
}
