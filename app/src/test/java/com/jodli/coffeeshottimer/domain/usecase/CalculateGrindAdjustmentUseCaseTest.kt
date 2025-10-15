package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import com.jodli.coffeeshottimer.data.repository.GrinderConfigRepository
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import com.jodli.coffeeshottimer.domain.model.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for CalculateGrindAdjustmentUseCase.
 * Tests the core business logic for grind adjustment recommendations based on
 * taste feedback and extraction metrics.
 */
class CalculateGrindAdjustmentUseCaseTest {

    private lateinit var grinderConfigRepository: GrinderConfigRepository
    private lateinit var calculateGrindAdjustmentUseCase: CalculateGrindAdjustmentUseCase

    // Standard test grinder configuration
    private val standardGrinderConfig = GrinderConfiguration(
        scaleMin = 10,
        scaleMax = 20,
        stepSize = 0.5
    )

    // High precision grinder configuration
    private val precisionGrinderConfig = GrinderConfiguration(
        scaleMin = 1,
        scaleMax = 10,
        stepSize = 0.1
    )

    // Coarse step grinder configuration
    private val coarseStepGrinderConfig = GrinderConfiguration(
        scaleMin = 30,
        scaleMax = 80,
        stepSize = 1.0
    )

    @Before
    fun setup() {
        grinderConfigRepository = mockk()
        calculateGrindAdjustmentUseCase = CalculateGrindAdjustmentUseCase(grinderConfigRepository)
    }

    // === SOUR TASTE TESTS ===

    @Test
    fun `calculateAdjustment with sour taste and fast extraction recommends finer grind`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 22 // 3 seconds too fast
        val tasteFeedback = TastePrimary.SOUR

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals("Should recommend finer grind", AdjustmentDirection.FINER, recommendation.adjustmentDirection)
        assertEquals("Should suggest one step finer", "14.5", recommendation.suggestedGrindSetting)
        assertEquals("Should be 1 step adjustment", 1, recommendation.adjustmentSteps)
        assertEquals("Should be high confidence", ConfidenceLevel.HIGH, recommendation.confidence)
        assertEquals("Should track extraction deviation", -3, recommendation.extractionTimeDeviation)
        assertEquals("Should track taste issue", TastePrimary.SOUR, recommendation.tasteIssue)
    }

    @Test
    fun `calculateAdjustment with sour taste and optimal timing still recommends finer grind`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 27 // Optimal timing
        val tasteFeedback = TastePrimary.SOUR

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.FINER, recommendation.adjustmentDirection)
        assertEquals("14.5", recommendation.suggestedGrindSetting)
        assertEquals(ConfidenceLevel.MEDIUM, recommendation.confidence) // Lower confidence due to good timing
        assertEquals(0, recommendation.extractionTimeDeviation)
    }

    // === BITTER TASTE TESTS ===

    @Test
    fun `calculateAdjustment with bitter taste and slow extraction recommends coarser grind`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 35 // 5 seconds too slow
        val tasteFeedback = TastePrimary.BITTER

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.COARSER, recommendation.adjustmentDirection)
        assertEquals("16.0", recommendation.suggestedGrindSetting) // 2 steps coarser (5s deviation)
        assertEquals(2, recommendation.adjustmentSteps)
        assertEquals(ConfidenceLevel.HIGH, recommendation.confidence)
        assertEquals(5, recommendation.extractionTimeDeviation)
        assertEquals(TastePrimary.BITTER, recommendation.tasteIssue)
    }

    @Test
    fun `calculateAdjustment with bitter taste and optimal timing still recommends coarser grind`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 28 // Optimal timing
        val tasteFeedback = TastePrimary.BITTER

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.COARSER, recommendation.adjustmentDirection)
        assertEquals(ConfidenceLevel.MEDIUM, recommendation.confidence)
        assertEquals(0, recommendation.extractionTimeDeviation)
    }

    // === PERFECT TASTE TESTS ===

    @Test
    fun `calculateAdjustment with perfect taste and optimal timing recommends no change`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 27 // Optimal timing
        val tasteFeedback = TastePrimary.PERFECT

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.NO_CHANGE, recommendation.adjustmentDirection)
        assertEquals("15.0", recommendation.suggestedGrindSetting)
        assertEquals(0, recommendation.adjustmentSteps)
        assertEquals(ConfidenceLevel.HIGH, recommendation.confidence)
        assertFalse("Should not have adjustment", recommendation.hasAdjustment())
    }

    @Test
    fun `calculateAdjustment with perfect taste but bad timing prioritizes timing`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 20 // Too fast
        val tasteFeedback = TastePrimary.PERFECT

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        // Should recommend finer grind due to fast extraction, despite perfect taste
        assertEquals(AdjustmentDirection.FINER, recommendation.adjustmentDirection)
        assertEquals(ConfidenceLevel.MEDIUM, recommendation.confidence)
    }

    // === NO TASTE FEEDBACK TESTS ===

    @Test
    fun `calculateAdjustment with no taste feedback and optimal timing recommends no change`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 27 // Optimal timing
        val tasteFeedback = null

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.NO_CHANGE, recommendation.adjustmentDirection)
        assertEquals(ConfidenceLevel.HIGH, recommendation.confidence) // No change is always high confidence
    }

    @Test
    fun `calculateAdjustment with no taste feedback but fast extraction recommends finer grind`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 20 // Too fast
        val tasteFeedback = null

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.FINER, recommendation.adjustmentDirection)
        assertEquals(ConfidenceLevel.MEDIUM, recommendation.confidence) // Medium due to no taste feedback
    }

    // === STEP CALCULATION TESTS ===

    @Test
    fun `calculateAdjustmentSteps scales with deviation magnitude`() = runTest {
        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        val testCases = listOf(
            // deviation -> expected steps
            Pair(22, 1), // 3s deviation -> 1 step
            Pair(21, 2), // 4s deviation -> 2 steps
            Pair(18, 3), // 7s deviation -> 3 steps
            Pair(15, 3), // 10s deviation -> 3 steps (max)
            Pair(33, 1), // 3s deviation -> 1 step
            Pair(36, 2), // 6s deviation -> 2 steps
            Pair(40, 3) // 10s deviation -> 3 steps
        )

        testCases.forEach { (extractionTime, expectedSteps) ->
            val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
                "15.0",
                extractionTime,
                null
            )

            assertTrue("Result should be successful for $extractionTime", result.isSuccess)
            val recommendation = result.getOrNull()!!
            assertEquals(
                "Steps should be $expectedSteps for deviation from $extractionTime",
                expectedSteps,
                recommendation.adjustmentSteps
            )
        }
    }

    // === DIFFERENT GRINDER CONFIGURATIONS TESTS ===

    @Test
    fun `calculateAdjustment with high precision grinder uses smaller steps`() = runTest {
        // Given - use a value well within bounds that allows for proper step adjustment
        val currentGrindSetting = "5.5" // Middle of range (1-10), allows adjustment
        val extractionTime = 22 // 3s too fast
        val tasteFeedback = TastePrimary.SOUR

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(precisionGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.FINER, recommendation.adjustmentDirection)
        assertEquals("5.4", recommendation.suggestedGrindSetting) // 0.1 step finer
        assertEquals(1, recommendation.adjustmentSteps)
    }

    @Test
    fun `calculateAdjustment with coarse step grinder uses larger steps`() = runTest {
        // Given
        val currentGrindSetting = "55"
        val extractionTime = 22 // 3s too fast
        val tasteFeedback = TastePrimary.SOUR

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(coarseStepGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.FINER, recommendation.adjustmentDirection)
        assertEquals(
            "54",
            recommendation.suggestedGrindSetting
        ) // 1.0 step finer (formatted as integer since step size >= 1.0)
        assertEquals(1, recommendation.adjustmentSteps)
    }

    // === BOUNDARY TESTS ===

    @Test
    fun `calculateAdjustment respects grinder minimum boundary`() = runTest {
        // Given - already at minimum
        val currentGrindSetting = "10.0" // At minimum
        val extractionTime = 20 // Should recommend finer
        val tasteFeedback = TastePrimary.SOUR

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.FINER, recommendation.adjustmentDirection)
        assertEquals("10.0", recommendation.suggestedGrindSetting) // Can't go below minimum
        assertEquals(0, recommendation.adjustmentSteps) // No actual steps possible
    }

    @Test
    fun `calculateAdjustment respects grinder maximum boundary`() = runTest {
        // Given - already at maximum
        val currentGrindSetting = "20.0" // At maximum
        val extractionTime = 35 // Should recommend coarser
        val tasteFeedback = TastePrimary.BITTER

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.COARSER, recommendation.adjustmentDirection)
        assertEquals("20.0", recommendation.suggestedGrindSetting) // Can't go above maximum
        assertEquals(0, recommendation.adjustmentSteps) // No actual steps possible
    }

    @Test
    fun `calculateAdjustment near boundary limits adjustment steps correctly`() = runTest {
        // Given - near minimum, large deviation would normally be 3 steps
        val currentGrindSetting = "10.5" // Only 1 step from minimum
        val extractionTime = 15 // 10s deviation, would normally be 3 steps
        val tasteFeedback = TastePrimary.SOUR

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isSuccess)
        val recommendation = result.getOrNull()!!

        assertEquals(AdjustmentDirection.FINER, recommendation.adjustmentDirection)
        assertEquals("10.0", recommendation.suggestedGrindSetting) // Hit minimum boundary
        assertEquals(1, recommendation.adjustmentSteps) // Only 1 step actually possible
    }

    // === ERROR HANDLING TESTS ===

    @Test
    fun `calculateAdjustment with invalid grind setting returns error`() = runTest {
        // Given
        val currentGrindSetting = "invalid"
        val extractionTime = 27
        val tasteFeedback = TastePrimary.PERFECT

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue(
            "Should be domain exception",
            exception is com.jodli.coffeeshottimer.domain.exception.DomainException
        )
    }

    @Test
    fun `calculateAdjustment with negative extraction time returns error`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = -5 // Invalid negative time
        val tasteFeedback = TastePrimary.PERFECT

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? com.jodli.coffeeshottimer.domain.exception.DomainException
        assertNotNull(exception)
        assertEquals(DomainErrorCode.INVALID_EXTRACTION_TIME, exception!!.errorCode)
    }

    @Test
    fun `calculateAdjustment with repository error returns error`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 27
        val tasteFeedback = TastePrimary.PERFECT

        val repositoryError = RepositoryException.DatabaseError("Database error", Exception())
        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.failure(repositoryError)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals(repositoryError, result.exceptionOrNull())
    }

    @Test
    fun `calculateAdjustment with missing grinder config returns error`() = runTest {
        // Given
        val currentGrindSetting = "15.0"
        val extractionTime = 27
        val tasteFeedback = TastePrimary.PERFECT

        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(null)

        // When
        val result = calculateGrindAdjustmentUseCase.calculateAdjustment(
            currentGrindSetting,
            extractionTime,
            tasteFeedback
        )

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? com.jodli.coffeeshottimer.domain.exception.DomainException
        assertNotNull(exception)
        assertEquals(DomainErrorCode.GRINDER_CONFIG_NOT_FOUND, exception!!.errorCode)
    }

    // === RECOMMENDATION UTILITY METHODS TESTS ===

    @Test
    fun `hasAdjustment returns correct values`() = runTest {
        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        // Test adjustment recommendation
        val adjustmentResult = calculateGrindAdjustmentUseCase.calculateAdjustment("15.0", 22, TastePrimary.SOUR)
        assertTrue(adjustmentResult.isSuccess)
        assertTrue("Should have adjustment", adjustmentResult.getOrNull()!!.hasAdjustment())

        // Test no-change recommendation
        val noChangeResult = calculateGrindAdjustmentUseCase.calculateAdjustment("15.0", 27, TastePrimary.PERFECT)
        assertTrue(noChangeResult.isSuccess)
        assertFalse("Should not have adjustment", noChangeResult.getOrNull()!!.hasAdjustment())
    }

    // === CONFIDENCE LEVEL TESTS ===

    @Test
    fun `confidence levels are calculated correctly`() = runTest {
        coEvery { grinderConfigRepository.getCurrentConfig() } returns Result.success(standardGrinderConfig)

        val testCases = listOf(
            // (extractionTime, taste, expectedConfidence)
            Triple(20, TastePrimary.SOUR, ConfidenceLevel.HIGH), // Strong time + taste evidence
            Triple(20, null, ConfidenceLevel.MEDIUM), // Strong time evidence only
            Triple(27, TastePrimary.SOUR, ConfidenceLevel.MEDIUM), // Taste evidence only
            Triple(27, null, ConfidenceLevel.HIGH), // No adjustment needed = high confidence
            Triple(27, TastePrimary.PERFECT, ConfidenceLevel.HIGH) // Perfect case
        )

        testCases.forEach { (extractionTime, taste, expectedConfidence) ->
            val result = calculateGrindAdjustmentUseCase.calculateAdjustment("15.0", extractionTime, taste)
            assertTrue("Result should be successful for $extractionTime/$taste", result.isSuccess)
            assertEquals(
                "Confidence should be $expectedConfidence for $extractionTime/$taste",
                expectedConfidence,
                result.getOrNull()!!.confidence
            )
        }
    }
}
