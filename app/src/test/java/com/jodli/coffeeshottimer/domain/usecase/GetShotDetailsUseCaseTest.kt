package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GetShotDetailsUseCaseTest {

    private lateinit var shotRepository: ShotRepository
    private lateinit var beanRepository: BeanRepository
    private lateinit var getShotQualityAnalysisUseCase: GetShotQualityAnalysisUseCase
    private lateinit var getShotDetailsUseCase: GetShotDetailsUseCase

    private val testBeanId = "test-bean-id"
    private val testShotId = "test-shot-id"

    private val testBean = Bean(
        id = testBeanId,
        name = "Test Bean",
        roastDate = LocalDate.now().minusDays(7),
        notes = "Great bean",
        isActive = true,
        lastGrinderSetting = "15"
    )

    private val testShot = Shot(
        id = testShotId,
        beanId = testBeanId,
        coffeeWeightIn = 18.0,
        coffeeWeightOut = 36.0,
        extractionTimeSeconds = 28,
        grinderSetting = "15",
        notes = "Perfect shot",
        timestamp = LocalDateTime.now()
    )

    private val previousShot = Shot(
        id = UUID.randomUUID().toString(),
        beanId = testBeanId,
        coffeeWeightIn = 18.5,
        coffeeWeightOut = 37.0,
        extractionTimeSeconds = 30,
        grinderSetting = "14",
        notes = "Previous shot",
        timestamp = LocalDateTime.now().minusHours(1)
    )

    private val nextShot = Shot(
        id = UUID.randomUUID().toString(),
        beanId = testBeanId,
        coffeeWeightIn = 17.5,
        coffeeWeightOut = 35.0,
        extractionTimeSeconds = 26,
        grinderSetting = "16",
        notes = "Next shot",
        timestamp = LocalDateTime.now().plusHours(1)
    )

    @Before
    fun setup() {
        shotRepository = mockk()
        beanRepository = mockk()
        getShotQualityAnalysisUseCase = GetShotQualityAnalysisUseCase(shotRepository)
        getShotDetailsUseCase = GetShotDetailsUseCase(shotRepository, beanRepository, getShotQualityAnalysisUseCase)
    }

    @Test
    fun `getShotDetails returns complete shot details successfully`() = runTest {
        // Given
        val relatedShots = listOf(previousShot, testShot, nextShot)
        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(testShot)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(testBean)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(relatedShots))

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isSuccess)
        val shotDetails = result.getOrNull()!!

        assertEquals(testShot, shotDetails.shot)
        assertEquals(testBean, shotDetails.bean)
        assertEquals(7, shotDetails.daysSinceRoast) // 7 days since roast
        assertEquals(previousShot, shotDetails.previousShot)
        assertEquals(nextShot, shotDetails.nextShot)
        assertEquals(3, shotDetails.relatedShotsCount)
        assertNotNull(shotDetails.analysis)
        // All bean shots, ascending by timestamp
        assertEquals(
            relatedShots.sortedBy { it.timestamp }.map { it.id },
            shotDetails.beanShots.map { it.id }
        )
    }

    @Test
    fun `getShotDetails returns error when shot not found`() = runTest {
        // Given
        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(null)

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals((exception as DomainException).errorCode, DomainErrorCode.SHOT_NOT_FOUND)
    }

    @Test
    fun `getShotDetails returns error when shot repository fails`() = runTest {
        // Given
        val exception = Exception("Database error")
        coEvery { shotRepository.getShotById(testShotId) } returns Result.failure(exception)

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getShotDetails returns error when bean not found`() = runTest {
        // Given
        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(testShot)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(null)

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals((exception as DomainException).errorCode, DomainErrorCode.ASSOCIATED_BEAN_NOT_FOUND)
    }

    @Test
    fun `getShotDetails returns error when bean repository fails`() = runTest {
        // Given
        val exception = Exception("Bean database error")
        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(testShot)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.failure(exception)

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getShotDetails handles shot with no previous or next shots`() = runTest {
        // Given
        val singleShot = listOf(testShot)
        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(testShot)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(testBean)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(singleShot))

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isSuccess)
        val shotDetails = result.getOrNull()!!

        assertNull(shotDetails.previousShot)
        assertNull(shotDetails.nextShot)
        assertEquals(1, shotDetails.relatedShotsCount)
    }

    @Test
    fun `getShotDetails calculates analysis correctly`() = runTest {
        // Given
        val relatedShots = listOf(previousShot, testShot, nextShot)
        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(testShot)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(testBean)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(relatedShots))

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!.analysis

        assertTrue(analysis.isOptimalExtraction) // 28 seconds is optimal
        assertTrue(analysis.isTypicalRatio) // 2.0 ratio is typical
        assertTrue(analysis.qualityScore > 50) // Should have decent quality score
        assertNotNull(analysis.avgBrewRatioForBean)
        assertNotNull(analysis.avgExtractionTimeForBean)
    }

    @Test
    fun `ShotAnalysis calculates quality score correctly for optimal shot`() = runTest {
        // Given - optimal shot (25-30s extraction, 1.5-3.0 ratio)
        val optimalShot = testShot.copy(
            extractionTimeSeconds = 27,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0 // 2.0 ratio
        )
        val relatedShots = listOf(optimalShot)

        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(optimalShot)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(testBean)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(relatedShots))

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!.analysis

        assertTrue(analysis.isOptimalExtraction)
        assertTrue(analysis.isTypicalRatio)
        assertTrue(analysis.isConsistentWithHistory)
        assertTrue(analysis.qualityScore >= 85) // Should be high for optimal shot
    }

    @Test
    fun `ShotAnalysis provides recommendations for non-optimal shots`() = runTest {
        // Given - non-optimal shot (too fast extraction)
        val fastShot = testShot.copy(
            extractionTimeSeconds = 20, // Too fast
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0
        )
        val relatedShots = listOf(fastShot)

        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(fastShot)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(testBean)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(relatedShots))

        // When
        val result = getShotDetailsUseCase.getShotDetails(testShotId)

        // Then
        assertTrue(result.isSuccess)
        val analysis = result.getOrNull()!!.analysis

        assertFalse(analysis.isOptimalExtraction)
        assertTrue(analysis.recommendations.isNotEmpty())
    }
}
