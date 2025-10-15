package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.preferences.GrindRecommendationPreferences
import com.jodli.coffeeshottimer.data.preferences.SerializableGrindRecommendation
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.AdjustmentDirection
import com.jodli.coffeeshottimer.domain.model.ConfidenceLevel
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import com.jodli.coffeeshottimer.domain.model.GrindAdjustmentRecommendation
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ManageGrindRecommendationUseCaseTest {

    private lateinit var mockGrindRecommendationPreferences: GrindRecommendationPreferences
    private lateinit var mockBeanRepository: BeanRepository
    private lateinit var useCase: ManageGrindRecommendationUseCase

    @Before
    fun setUp() {
        mockGrindRecommendationPreferences = mockk()
        mockBeanRepository = mockk()
        useCase = ManageGrindRecommendationUseCase(
            mockGrindRecommendationPreferences,
            mockBeanRepository
        )
    }

    @Test
    fun `saveRecommendation with taste feedback creates correct persistent recommendation`() = runTest {
        // Given
        val beanId = "bean123"
        val bean = createTestBean(id = beanId)
        val shot = createTestShot(
            beanId = beanId,
            extractionTimeSeconds = 24,
            tastePrimary = TastePrimary.SOUR
        )
        val recommendation = createTestGrindAdjustmentRecommendation(
            adjustmentDirection = AdjustmentDirection.FINER,
            suggestedGrindSetting = "5.5"
        )

        coEvery { mockBeanRepository.getBeanById(beanId) } returns Result.success(bean)
        coEvery { mockGrindRecommendationPreferences.saveRecommendation(any(), any()) } just Runs

        // When
        val result = useCase.saveRecommendation(beanId, recommendation, shot)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val persistentRecommendation = result.getOrNull()!!
        assertEquals(beanId, persistentRecommendation.beanId)
        assertEquals("5.5", persistentRecommendation.suggestedGrindSetting)
        assertEquals(AdjustmentDirection.FINER, persistentRecommendation.adjustmentDirection)
        assertEquals(18.0, persistentRecommendation.recommendedDose, 0.01)
        assertEquals("Last shot was sour (24s)", persistentRecommendation.reason)
        assertTrue("Should be based on taste", persistentRecommendation.basedOnTaste)
        assertFalse("Should not be marked as followed initially", persistentRecommendation.wasFollowed)
    }

    @Test
    fun `saveRecommendation without taste feedback creates timing-based recommendation`() = runTest {
        // Given
        val beanId = "bean123"
        val bean = createTestBean(id = beanId)
        val shot = createTestShot(
            beanId = beanId,
            extractionTimeSeconds = 22,
            tastePrimary = null // No taste feedback
        )
        val recommendation = createTestGrindAdjustmentRecommendation(
            adjustmentDirection = AdjustmentDirection.FINER,
            suggestedGrindSetting = "5.3"
        )

        coEvery { mockBeanRepository.getBeanById(beanId) } returns Result.success(bean)
        coEvery { mockGrindRecommendationPreferences.saveRecommendation(any(), any()) } just Runs

        // When
        val result = useCase.saveRecommendation(beanId, recommendation, shot)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val persistentRecommendation = result.getOrNull()!!
        assertEquals("Last shot ran too fast (22s)", persistentRecommendation.reason)
        assertFalse("Should not be based on taste", persistentRecommendation.basedOnTaste)
    }

    @Test
    fun `saveRecommendation fails when bean not found`() = runTest {
        // Given
        val beanId = "nonexistent"
        val shot = createTestShot(beanId = beanId)
        val recommendation = createTestGrindAdjustmentRecommendation()

        coEvery { mockBeanRepository.getBeanById(beanId) } returns Result.failure(
            DomainException(DomainErrorCode.BEAN_NOT_FOUND, "Bean not found")
        )

        // When
        val result = useCase.saveRecommendation(beanId, recommendation, shot)

        // Then
        assertTrue("Should fail", result.isFailure)
        val exception = result.exceptionOrNull() as DomainException
        assertEquals(DomainErrorCode.BEAN_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `getRecommendation returns null when no recommendation exists`() = runTest {
        // Given
        val beanId = "bean123"
        coEvery { mockGrindRecommendationPreferences.getRecommendation(beanId) } returns null

        // When
        val result = useCase.getRecommendation(beanId)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertNull("Should return null", result.getOrNull())
    }

    @Test
    fun `getRecommendation converts serializable recommendation to domain model`() = runTest {
        // Given
        val beanId = "bean123"
        val serializableRecommendation = SerializableGrindRecommendation.create(
            beanId = beanId,
            suggestedGrindSetting = "5.5",
            adjustmentDirection = "FINER",
            reason = "Last shot was sour (24s)",
            recommendedDose = 18.5,
            basedOnTaste = true,
            confidence = "HIGH"
        )

        coEvery { mockGrindRecommendationPreferences.getRecommendation(beanId) } returns serializableRecommendation

        // When
        val result = useCase.getRecommendation(beanId)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val persistentRecommendation = result.getOrNull()!!
        assertEquals(beanId, persistentRecommendation.beanId)
        assertEquals("5.5", persistentRecommendation.suggestedGrindSetting)
        assertEquals(AdjustmentDirection.FINER, persistentRecommendation.adjustmentDirection)
        assertEquals("Last shot was sour (24s)", persistentRecommendation.reason)
        assertEquals(18.5, persistentRecommendation.recommendedDose, 0.01)
        assertTrue(persistentRecommendation.basedOnTaste)
        assertEquals(ConfidenceLevel.HIGH, persistentRecommendation.confidence)
    }

    @Test
    fun `markRecommendationFollowed calls preferences correctly`() = runTest {
        // Given
        val beanId = "bean123"
        coEvery { mockGrindRecommendationPreferences.markRecommendationFollowed(beanId) } just Runs

        // When
        val result = useCase.markRecommendationFollowed(beanId)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockGrindRecommendationPreferences.markRecommendationFollowed(beanId) }
    }

    @Test
    fun `clearRecommendation calls preferences correctly`() = runTest {
        // Given
        val beanId = "bean123"
        coEvery { mockGrindRecommendationPreferences.clearRecommendation(beanId) } just Runs

        // When
        val result = useCase.clearRecommendation(beanId)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockGrindRecommendationPreferences.clearRecommendation(beanId) }
    }

    @Test
    fun `updateRecommendationWithTaste updates existing recommendation`() = runTest {
        // Given
        val beanId = "bean123"
        val existingRecommendation = SerializableGrindRecommendation.create(
            beanId = beanId,
            suggestedGrindSetting = "5.0",
            adjustmentDirection = "FINER",
            reason = "Last shot ran too fast (22s)",
            recommendedDose = 18.5,
            basedOnTaste = false, // Originally no taste
            confidence = "MEDIUM"
        )
        val updatedShot = createTestShot(
            beanId = beanId,
            extractionTimeSeconds = 22,
            tastePrimary = TastePrimary.SOUR // Now with taste
        )
        val updatedRecommendation = createTestGrindAdjustmentRecommendation(
            adjustmentDirection = AdjustmentDirection.FINER,
            suggestedGrindSetting = "5.3",
            confidence = ConfidenceLevel.HIGH
        )

        coEvery { mockGrindRecommendationPreferences.getRecommendation(beanId) } returns existingRecommendation
        coEvery { mockGrindRecommendationPreferences.saveRecommendation(eq(beanId), any()) } just Runs

        // When
        val result = useCase.updateRecommendationWithTaste(beanId, updatedRecommendation, updatedShot)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val updated = result.getOrNull()!!
        assertEquals("Last shot was sour (22s)", updated.reason) // Updated reason with taste
        assertTrue("Should now be based on taste", updated.basedOnTaste)
        assertEquals(ConfidenceLevel.HIGH, updated.confidence) // Updated confidence
        assertEquals("5.3", updated.suggestedGrindSetting) // Updated grind setting

        // Verify correct serializable recommendation was saved
        val serializableSlot = slot<SerializableGrindRecommendation>()
        coVerify { mockGrindRecommendationPreferences.saveRecommendation(eq(beanId), capture(serializableSlot)) }
        assertTrue("Saved recommendation should be based on taste", serializableSlot.captured.basedOnTaste)
    }

    @Test
    fun `updateRecommendationWithTaste creates new recommendation when none exists`() = runTest {
        // Given
        val beanId = "bean123"
        val bean = createTestBean(id = beanId)
        val shot = createTestShot(
            beanId = beanId,
            extractionTimeSeconds = 24,
            tastePrimary = TastePrimary.SOUR
        )
        val recommendation = createTestGrindAdjustmentRecommendation(
            adjustmentDirection = AdjustmentDirection.FINER,
            suggestedGrindSetting = "5.5"
        )

        coEvery { mockGrindRecommendationPreferences.getRecommendation(beanId) } returns null
        coEvery { mockBeanRepository.getBeanById(beanId) } returns Result.success(bean)
        coEvery { mockGrindRecommendationPreferences.saveRecommendation(any(), any()) } just Runs

        // When
        val result = useCase.updateRecommendationWithTaste(beanId, recommendation, shot)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val persistentRecommendation = result.getOrNull()!!
        assertEquals("Last shot was sour (24s)", persistentRecommendation.reason)
        assertTrue("Should be based on taste", persistentRecommendation.basedOnTaste)
    }

    @Test
    fun `getAllRecommendationBeanIds returns list from preferences`() = runTest {
        // Given
        val expectedBeanIds = listOf("bean1", "bean2", "bean3")
        coEvery { mockGrindRecommendationPreferences.getAllRecommendationBeanIds() } returns expectedBeanIds

        // When
        val result = useCase.getAllRecommendationBeanIds()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals(expectedBeanIds, result.getOrNull())
    }

    @Test
    fun `clearAllRecommendations calls preferences correctly`() = runTest {
        // Given
        coEvery { mockGrindRecommendationPreferences.clearAllRecommendations() } just Runs

        // When
        val result = useCase.clearAllRecommendations()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockGrindRecommendationPreferences.clearAllRecommendations() }
    }

    @Test
    fun `saveRecommendation handles storage exception`() = runTest {
        // Given
        val beanId = "bean123"
        val bean = createTestBean(id = beanId)
        val shot = createTestShot(beanId = beanId)
        val recommendation = createTestGrindAdjustmentRecommendation()

        coEvery { mockBeanRepository.getBeanById(beanId) } returns Result.success(bean)
        coEvery { mockGrindRecommendationPreferences.saveRecommendation(any(), any()) } throws RuntimeException("Storage error")

        // When
        val result = useCase.saveRecommendation(beanId, recommendation, shot)

        // Then
        assertTrue("Should fail", result.isFailure)
        val exception = result.exceptionOrNull() as DomainException
        assertEquals(DomainErrorCode.STORAGE_ERROR, exception.errorCode)
        assertTrue("Should contain error message", exception.message!!.contains("Failed to save recommendation"))
    }

    @Test
    fun `getRecommendation handles storage exception`() = runTest {
        // Given
        val beanId = "bean123"
        coEvery { mockGrindRecommendationPreferences.getRecommendation(beanId) } throws RuntimeException("Storage error")

        // When
        val result = useCase.getRecommendation(beanId)

        // Then
        assertTrue("Should fail", result.isFailure)
        val exception = result.exceptionOrNull() as DomainException
        assertEquals(DomainErrorCode.STORAGE_ERROR, exception.errorCode)
    }

    private fun createTestBean(
        id: String = "test_bean",
        name: String = "Test Bean"
    ): Bean {
        return Bean(
            id = id,
            name = name,
            roastDate = java.time.LocalDate.now().minusDays(5),
            notes = "Test notes",
            isActive = true,
            lastGrinderSetting = null,
            photoPath = null,
            createdAt = LocalDateTime.now().minusDays(5)
        )
    }

    private fun createTestShot(
        beanId: String = "test_bean",
        extractionTimeSeconds: Int = 27,
        tastePrimary: TastePrimary? = null
    ): Shot {
        return Shot(
            id = "test_shot",
            beanId = beanId,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = extractionTimeSeconds,
            grinderSetting = "5.0",
            notes = "",
            timestamp = LocalDateTime.now(),
            tastePrimary = tastePrimary,
            tasteSecondary = null
        )
    }

    private fun createTestGrindAdjustmentRecommendation(
        currentGrindSetting: String = "5.0",
        suggestedGrindSetting: String = "5.2",
        adjustmentDirection: AdjustmentDirection = AdjustmentDirection.FINER,
        confidence: ConfidenceLevel = ConfidenceLevel.HIGH
    ): GrindAdjustmentRecommendation {
        return GrindAdjustmentRecommendation(
            currentGrindSetting = currentGrindSetting,
            suggestedGrindSetting = suggestedGrindSetting,
            adjustmentDirection = adjustmentDirection,
            adjustmentSteps = 1,
            extractionTimeDeviation = -3,
            tasteIssue = TastePrimary.SOUR,
            confidence = confidence
        )
    }
}
