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
        getShotDetailsUseCase = GetShotDetailsUseCase(shotRepository, beanRepository)
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
    fun `compareShotsDetails returns comparison successfully`() = runTest {
        // Given
        val shot1Id = "shot1"
        val shot2Id = "shot2"
        val shot1 = testShot.copy(id = shot1Id)
        val shot2 = previousShot.copy(id = shot2Id)

        coEvery { shotRepository.getShotById(shot1Id) } returns Result.success(shot1)
        coEvery { shotRepository.getShotById(shot2Id) } returns Result.success(shot2)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(testBean)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(listOf(shot1, shot2)))

        // When
        val result = getShotDetailsUseCase.compareShotsDetails(shot1Id, shot2Id)

        // Then
        assertTrue(result.isSuccess)
        val comparison = result.getOrNull()!!

        assertEquals(shot1, comparison.shot1.shot)
        assertEquals(shot2, comparison.shot2.shot)
        assertEquals(0.5, comparison.weightInDifference, 0.01) // 18.5 - 18.0
        assertEquals(1.0, comparison.weightOutDifference, 0.01) // 37.0 - 36.0
        assertEquals(2, comparison.extractionTimeDifference) // 30 - 28
        assertTrue(comparison.sameBeanUsed)
        assertFalse(comparison.sameGrinderSetting) // "14" vs "15"
    }

    @Test
    fun `compareShotsDetails returns error when first shot fails`() = runTest {
        // Given
        val shot1Id = "shot1"
        val shot2Id = "shot2"
        val exception = Exception("Shot 1 not found")
        coEvery { shotRepository.getShotById(shot1Id) } returns Result.failure(exception)

        // When
        val result = getShotDetailsUseCase.compareShotsDetails(shot1Id, shot2Id)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `compareShotsDetails returns error when second shot fails`() = runTest {
        // Given
        val shot1Id = "shot1"
        val shot2Id = "shot2"
        val shot1 = testShot.copy(id = shot1Id)

        coEvery { shotRepository.getShotById(shot1Id) } returns Result.success(shot1)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(testBean)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(listOf(shot1)))

        val exception = Exception("Shot 2 not found")
        coEvery { shotRepository.getShotById(shot2Id) } returns Result.failure(exception)

        // When
        val result = getShotDetailsUseCase.compareShotsDetails(shot1Id, shot2Id)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getLastShotForBean returns last shot details successfully`() = runTest {
        // Given
        coEvery { shotRepository.getLastShotForBean(testBeanId) } returns Result.success(testShot)
        coEvery { shotRepository.getShotById(testShotId) } returns Result.success(testShot)
        coEvery { beanRepository.getBeanById(testBeanId) } returns Result.success(testBean)
        every { shotRepository.getShotsByBean(testBeanId) } returns flowOf(Result.success(listOf(testShot)))

        // When
        val result = getShotDetailsUseCase.getLastShotForBean(testBeanId)

        // Then
        assertTrue(result.isSuccess)
        val shotDetails = result.getOrNull()!!
        assertEquals(testShot, shotDetails.shot)
        assertEquals(testBean, shotDetails.bean)
    }

    @Test
    fun `getLastShotForBean returns null when no shots exist`() = runTest {
        // Given
        coEvery { shotRepository.getLastShotForBean(testBeanId) } returns Result.success(null)

        // When
        val result = getShotDetailsUseCase.getLastShotForBean(testBeanId)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getLastShotForBean returns error when repository fails`() = runTest {
        // Given
        val exception = Exception("Repository error")
        coEvery { shotRepository.getLastShotForBean(testBeanId) } returns Result.failure(exception)

        // When
        val result = getShotDetailsUseCase.getLastShotForBean(testBeanId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
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

    @Test
    fun `ShotComparison getFormattedDifferences returns correct format`() {
        // Given
        val shot1Details = ShotDetails(
            shot = testShot,
            bean = testBean,
            daysSinceRoast = 7,
            previousShot = null,
            nextShot = null,
            analysis = ShotAnalysis(
                qualityScore = 80,
                isOptimalExtraction = true,
                isTypicalRatio = true,
                isConsistentWithHistory = true,
                brewRatioDeviation = 0.0,
                extractionTimeDeviation = 0.0,
                weightInDeviation = 0.0,
                weightOutDeviation = 0.0,
                avgBrewRatioForBean = 2.0,
                avgExtractionTimeForBean = 28.0,
                avgWeightInForBean = 18.0,
                avgWeightOutForBean = 36.0,
                recommendations = emptyList()
            ),
            relatedShotsCount = 1
        )

        val shot2Details = shot1Details.copy(
            shot = previousShot
        )

        val comparison = ShotComparison(
            shot1 = shot1Details,
            shot2 = shot2Details,
            weightInDifference = 0.5,
            weightOutDifference = 1.0,
            extractionTimeDifference = 2,
            brewRatioDifference = 0.0,
            sameBeanUsed = true,
            sameGrinderSetting = false
        )

        // When
        val formatted = comparison.getFormattedDifferences()

        // Then
        assertEquals("+0.5g", formatted["weightIn"])
        assertEquals("+1.0g", formatted["weightOut"])
        assertEquals("+2s", formatted["extractionTime"])
        assertEquals("+0.00", formatted["brewRatio"])
    }
}
