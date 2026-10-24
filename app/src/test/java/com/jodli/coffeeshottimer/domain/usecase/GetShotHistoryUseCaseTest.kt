package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.data.repository.ShotRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class GetShotHistoryUseCaseTest {

    private lateinit var shotRepository: ShotRepository
    private lateinit var getShotHistoryUseCase: GetShotHistoryUseCase

    private val testBeanId = "test-bean-id"
    private val testShot1 = Shot(
        id = UUID.randomUUID().toString(),
        beanId = testBeanId,
        coffeeWeightIn = 18.0,
        coffeeWeightOut = 36.0,
        extractionTimeSeconds = 28,
        grinderSetting = "15",
        notes = "Perfect shot",
        timestamp = LocalDateTime.now().minusHours(1)
    )

    private val testShot2 = Shot(
        id = UUID.randomUUID().toString(),
        beanId = testBeanId,
        coffeeWeightIn = 18.5,
        coffeeWeightOut = 37.0,
        extractionTimeSeconds = 30,
        grinderSetting = "14",
        notes = "Good extraction",
        timestamp = LocalDateTime.now().minusHours(2)
    )

    private val testShot3 = Shot(
        id = UUID.randomUUID().toString(),
        beanId = "different-bean-id",
        coffeeWeightIn = 20.0,
        coffeeWeightOut = 40.0,
        extractionTimeSeconds = 25,
        grinderSetting = "16",
        notes = "Fast extraction",
        timestamp = LocalDateTime.now().minusHours(3)
    )

    @Before
    fun setup() {
        shotRepository = mockk()
        getShotHistoryUseCase = GetShotHistoryUseCase(shotRepository)
    }

    @Test
    fun `getAllShots returns all shots from repository`() = runTest {
        // Given
        val expectedShots = listOf(testShot1, testShot2, testShot3)
        every { shotRepository.getAllShots() } returns flowOf(Result.success(expectedShots))

        // When
        val result = getShotHistoryUseCase.getAllShots().toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].isSuccess)
        assertEquals(expectedShots, result[0].getOrNull())
    }

    @Test
    fun `getFilteredShots applies basic filters through repository`() = runTest {
        // Given
        val filter = ShotHistoryFilter(
            beanId = testBeanId,
            startDate = LocalDateTime.now().minusDays(1),
            endDate = LocalDateTime.now()
        )
        val expectedShots = listOf(testShot1, testShot2)
        every {
            shotRepository.getFilteredShots(filter.beanId, filter.startDate, filter.endDate)
        } returns flowOf(Result.success(expectedShots))

        // When
        val result = getShotHistoryUseCase.getFilteredShots(filter).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].isSuccess)
        assertEquals(expectedShots, result[0].getOrNull())
    }

    @Test
    fun `getFilteredShots applies grinder setting filter`() = runTest {
        // Given
        val filter = ShotHistoryFilter(grinderSetting = "15")
        val allShots = listOf(testShot1, testShot2, testShot3)
        every {
            shotRepository.getFilteredShots(null, null, null)
        } returns flowOf(Result.success(allShots))

        // When
        val result = getShotHistoryUseCase.getFilteredShots(filter).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].isSuccess)
        val filteredShots = result[0].getOrNull()!!
        assertEquals(1, filteredShots.size)
        assertEquals(testShot1, filteredShots[0])
    }

    @Test
    fun `getFilteredShots applies brew ratio range filter`() = runTest {
        // Given
        val filter = ShotHistoryFilter(minBrewRatio = 1.9, maxBrewRatio = 2.1)
        val allShots = listOf(testShot1, testShot2, testShot3)
        every {
            shotRepository.getFilteredShots(null, null, null)
        } returns flowOf(Result.success(allShots))

        // When
        val result = getShotHistoryUseCase.getFilteredShots(filter).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].isSuccess)
        val filteredShots = result[0].getOrNull()!!
        // testShot1 has ratio 2.0, testShot2 has ratio 2.0, testShot3 has ratio 2.0
        // All should be included
        assertEquals(3, filteredShots.size)
    }

    @Test
    fun `getFilteredShots applies extraction time range filter`() = runTest {
        // Given
        val filter = ShotHistoryFilter(minExtractionTime = 25, maxExtractionTime = 28)
        val allShots = listOf(testShot1, testShot2, testShot3)
        every {
            shotRepository.getFilteredShots(null, null, null)
        } returns flowOf(Result.success(allShots))

        // When
        val result = getShotHistoryUseCase.getFilteredShots(filter).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].isSuccess)
        val filteredShots = result[0].getOrNull()!!
        assertEquals(2, filteredShots.size) // testShot1 (28s) and testShot3 (25s)
        assertTrue(filteredShots.contains(testShot1))
        assertTrue(filteredShots.contains(testShot3))
    }

    @Test
    fun `getFilteredShots applies optimal extraction time filter`() = runTest {
        // Given
        val filter = ShotHistoryFilter(onlyOptimalExtractionTime = true)
        val allShots = listOf(testShot1, testShot2, testShot3)
        every {
            shotRepository.getFilteredShots(null, null, null)
        } returns flowOf(Result.success(allShots))

        // When
        val result = getShotHistoryUseCase.getFilteredShots(filter).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].isSuccess)
        val filteredShots = result[0].getOrNull()!!
        // Only shots with 25-30 seconds should be included
        assertEquals(3, filteredShots.size) // All test shots are in optimal range
    }

    @Test
    fun `getFilteredShots applies limit filter`() = runTest {
        // Given
        val filter = ShotHistoryFilter(limit = 2)
        val allShots = listOf(testShot1, testShot2, testShot3)
        every {
            shotRepository.getFilteredShots(null, null, null)
        } returns flowOf(Result.success(allShots))

        // When
        val result = getShotHistoryUseCase.getFilteredShots(filter).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].isSuccess)
        val filteredShots = result[0].getOrNull()!!
        assertEquals(2, filteredShots.size)
    }

    @Test
    fun `ShotHistoryFilter hasFilters returns true when filters are set`() {
        // Given
        val filterWithBean = ShotHistoryFilter(beanId = "test")
        val filterWithDate = ShotHistoryFilter(startDate = LocalDateTime.now())
        val filterWithGrinder = ShotHistoryFilter(grinderSetting = "15")
        val filterWithRatio = ShotHistoryFilter(minBrewRatio = 2.0)
        val filterWithTime = ShotHistoryFilter(minExtractionTime = 25)
        val filterWithOptimal = ShotHistoryFilter(onlyOptimalExtractionTime = true)
        val emptyFilter = ShotHistoryFilter()

        // Then
        assertTrue(filterWithBean.hasFilters())
        assertTrue(filterWithDate.hasFilters())
        assertTrue(filterWithGrinder.hasFilters())
        assertTrue(filterWithRatio.hasFilters())
        assertTrue(filterWithTime.hasFilters())
        assertTrue(filterWithOptimal.hasFilters())
        assertFalse(emptyFilter.hasFilters())
    }
}
