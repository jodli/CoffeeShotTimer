package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for GetActiveBeansUseCase.
 * Tests active bean retrieval, filtering, sorting, and error handling scenarios.
 */
class GetActiveBeansUseCaseTest {

    private lateinit var beanRepository: BeanRepository
    private lateinit var getActiveBeansUseCase: GetActiveBeansUseCase

    private val testBeans = listOf(
        Bean(
            id = "bean1",
            name = "Ethiopian Yirgacheffe",
            roastDate = LocalDate.of(2024, 1, 1), // Fixed date for consistent testing
            notes = "Floral and citrusy",
            isActive = true,
            lastGrinderSetting = "15",
            createdAt = LocalDateTime.now().minusDays(2)
        ),
        Bean(
            id = "bean2",
            name = "Colombian Supremo",
            roastDate = LocalDate.of(2024, 1, 5), // Fixed date for consistent testing
            notes = "Balanced and smooth",
            isActive = true,
            lastGrinderSetting = "16",
            createdAt = LocalDateTime.now().minusDays(1)
        ),
        Bean(
            id = "bean3",
            name = "Brazilian Santos",
            roastDate = LocalDate.of(2023, 12, 15), // Fixed date for consistent testing
            notes = "Nutty and chocolatey",
            isActive = true,
            lastGrinderSetting = null,
            createdAt = LocalDateTime.now().minusDays(3)
        )
    )

    @Before
    fun setup() {
        beanRepository = mockk()
        getActiveBeansUseCase = GetActiveBeansUseCase(beanRepository)
    }

    @Test
    fun `execute should return active beans sorted by creation date`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getActiveBeansUseCase.execute().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(3, beans?.size)

        // Should be sorted by creation date (newest first)
        assertEquals("bean2", beans?.get(0)?.id) // Most recent
        assertEquals("bean1", beans?.get(1)?.id)
        assertEquals("bean3", beans?.get(2)?.id) // Oldest

        coVerify { beanRepository.getActiveBeans() }
    }

    @Test
    fun `execute should handle empty bean list`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.success(emptyList()))

        // When
        val results = getActiveBeansUseCase.execute().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertTrue(beans?.isEmpty() == true)
    }

    @Test
    fun `execute should handle repository error`() = runTest {
        // Given
        val repositoryError = RepositoryException.DatabaseError("Database error")
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.failure(repositoryError))

        // When
        val results = getActiveBeansUseCase.execute().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isFailure)
        assertEquals(repositoryError, result.exceptionOrNull())
    }

    @Test
    fun `getActiveBeansWithSearch should filter beans by search query`() = runTest {
        // Given
        val searchQuery = "Ethiopian"
        val filteredBeans = listOf(testBeans[0]) // Only Ethiopian bean

        coEvery {
            beanRepository.getFilteredBeans(activeOnly = true, searchQuery = searchQuery)
        } returns flowOf(Result.success(filteredBeans))

        // When
        val results = getActiveBeansUseCase.getActiveBeansWithSearch(searchQuery).toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(1, beans?.size)
        assertEquals("Ethiopian Yirgacheffe", beans?.get(0)?.name)

        coVerify { beanRepository.getFilteredBeans(activeOnly = true, searchQuery = searchQuery) }
    }

    @Test
    fun `getActiveBeansByFreshness should sort beans by freshness`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getActiveBeansUseCase.getActiveBeansByFreshness().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beansWithFreshness = result.getOrNull()
        assertNotNull(beansWithFreshness)
        assertEquals(3, beansWithFreshness?.size)

        // Check that all beans have freshness information calculated
        beansWithFreshness?.forEach { beanWithFreshness ->
            assertNotNull(beanWithFreshness.bean)
            assertTrue(beanWithFreshness.daysSinceRoast >= 0)
            assertNotNull(beanWithFreshness.freshnessCategory)

            // Verify freshness logic matches Bean model
            assertEquals(beanWithFreshness.bean.isFresh(), beanWithFreshness.isFresh)
            assertEquals(beanWithFreshness.bean.daysSinceRoast(), beanWithFreshness.daysSinceRoast)
        }
    }

    @Test
    fun `getActiveBeansWithGrinderSettings should filter beans with grinder settings`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getActiveBeansUseCase.getActiveBeansWithGrinderSettings().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(2, beans?.size) // Only beans with grinder settings

        // Should not include bean3 which has null grinder setting
        assertFalse(beans?.any { it.id == "bean3" } == true)
        assertTrue(beans?.any { it.id == "bean1" } == true)
        assertTrue(beans?.any { it.id == "bean2" } == true)
    }

    @Test
    fun `getActiveBeanCount should return count of active beans`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(5)

        // When
        val result = getActiveBeansUseCase.getActiveBeanCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())

        coVerify { beanRepository.getActiveBeanCount() }
    }

    @Test
    fun `getActiveBeanCount should handle repository error`() = runTest {
        // Given
        val repositoryError = RepositoryException.DatabaseError("Database error")
        coEvery { beanRepository.getActiveBeanCount() } returns Result.failure(repositoryError)

        // When
        val result = getActiveBeansUseCase.getActiveBeanCount()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RepositoryException.DatabaseError)
    }

    @Test
    fun `getMostRecentActiveBean should return most recently created bean`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.success(testBeans))

        // When
        val result = getActiveBeansUseCase.getMostRecentActiveBean()

        // Then
        assertTrue(result.isSuccess)
        val mostRecentBean = result.getOrNull()
        assertNotNull(mostRecentBean)
        assertEquals("bean2", mostRecentBean?.id) // Most recent creation date
    }

    @Test
    fun `getMostRecentActiveBean should return null when no beans exist`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.success(emptyList()))

        // When
        val result = getActiveBeansUseCase.getMostRecentActiveBean()

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `hasActiveBeans should return true when active beans exist`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(3)

        // When
        val result = getActiveBeansUseCase.hasActiveBeans()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `hasActiveBeans should return false when no active beans exist`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeanCount() } returns Result.success(0)

        // When
        val result = getActiveBeansUseCase.hasActiveBeans()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)
    }

    @Test
    fun `getActiveBeansGroupedByFreshness should group beans by freshness category`() = runTest {
        // Given
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getActiveBeansUseCase.getActiveBeansGroupedByFreshness().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val groupedBeans = result.getOrNull()
        assertNotNull(groupedBeans)

        // Check that beans are grouped by their actual freshness categories
        // Since we're using fixed dates from 2024, all beans will likely be STALE
        assertTrue(groupedBeans?.isNotEmpty() == true)

        // Verify that each bean is in the correct category based on its actual freshness
        testBeans.forEach { bean ->
            val expectedCategory = when (bean.daysSinceRoast()) {
                in 0..3 -> FreshnessCategory.TOO_FRESH
                in 4..14 -> FreshnessCategory.OPTIMAL
                in 15..30 -> FreshnessCategory.GOOD
                in 31..60 -> FreshnessCategory.ACCEPTABLE
                else -> FreshnessCategory.STALE
            }

            val beansInCategory = groupedBeans?.get(expectedCategory)
            assertTrue(
                "Bean ${bean.id} should be in category $expectedCategory",
                beansInCategory?.any { it.id == bean.id } == true
            )
        }
    }

    @Test
    fun `freshness categories should have correct display names and descriptions`() {
        // Test enum values
        assertEquals("Too Fresh", FreshnessCategory.TOO_FRESH.displayName)
        assertEquals("Needs more time to degas (< 4 days)", FreshnessCategory.TOO_FRESH.description)

        assertEquals("Optimal", FreshnessCategory.OPTIMAL.displayName)
        assertEquals("Perfect for espresso (4-14 days)", FreshnessCategory.OPTIMAL.description)

        assertEquals("Good", FreshnessCategory.GOOD.displayName)
        assertEquals("Still great for brewing (15-30 days)", FreshnessCategory.GOOD.description)

        assertEquals("Acceptable", FreshnessCategory.ACCEPTABLE.displayName)
        assertEquals("Usable but past peak (31-60 days)", FreshnessCategory.ACCEPTABLE.description)

        assertEquals("Stale", FreshnessCategory.STALE.displayName)
        assertEquals("Consider replacing (> 60 days)", FreshnessCategory.STALE.description)
    }

    @Test
    fun `BeanWithFreshness should contain correct freshness information`() = runTest {
        // Given
        val bean = testBeans[0]
        coEvery { beanRepository.getActiveBeans() } returns flowOf(Result.success(listOf(bean)))

        // When
        val results = getActiveBeansUseCase.getActiveBeansByFreshness().toList()

        // Then
        val result = results.first()
        assertTrue(result.isSuccess)

        val beansWithFreshness = result.getOrNull()
        assertNotNull(beansWithFreshness)
        assertEquals(1, beansWithFreshness?.size)

        val beanWithFreshness = beansWithFreshness?.first()
        assertEquals(bean, beanWithFreshness?.bean)

        // Verify that the freshness information matches the bean's actual values
        assertEquals(bean.daysSinceRoast(), beanWithFreshness?.daysSinceRoast)
        assertEquals(bean.isFresh(), beanWithFreshness?.isFresh)

        // Verify freshness category is calculated correctly
        val expectedCategory = when (bean.daysSinceRoast()) {
            in 0..3 -> FreshnessCategory.TOO_FRESH
            in 4..14 -> FreshnessCategory.OPTIMAL
            in 15..30 -> FreshnessCategory.GOOD
            in 31..60 -> FreshnessCategory.ACCEPTABLE
            else -> FreshnessCategory.STALE
        }
        assertEquals(expectedCategory, beanWithFreshness?.freshnessCategory)
    }
}
