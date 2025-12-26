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
 * Unit tests for GetBeanHistoryUseCase.
 * Tests bean history retrieval, filtering, grouping, and statistical analysis.
 */
class GetBeanHistoryUseCaseTest {

    private lateinit var beanRepository: BeanRepository
    private lateinit var getBeanHistoryUseCase: GetBeanHistoryUseCase

    private val testBeans = listOf(
        Bean(
            id = "bean1",
            name = "Ethiopian Yirgacheffe",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Floral and citrusy",
            isActive = true,
            lastGrinderSetting = "15",
            createdAt = LocalDateTime.now().minusDays(10)
        ),
        Bean(
            id = "bean2",
            name = "Colombian Supremo",
            roastDate = LocalDate.now().minusDays(3),
            notes = "Balanced and smooth",
            isActive = false,
            lastGrinderSetting = "16",
            createdAt = LocalDateTime.now().minusDays(5)
        ),
        Bean(
            id = "bean3",
            name = "Brazilian Santos",
            roastDate = LocalDate.now().minusDays(20),
            notes = "Nutty and chocolatey",
            isActive = true,
            lastGrinderSetting = null,
            createdAt = LocalDateTime.now().minusDays(2)
        ),
        Bean(
            id = "bean4",
            name = "Guatemalan Antigua",
            roastDate = LocalDate.now().minusDays(45),
            notes = "Full-bodied",
            isActive = false,
            lastGrinderSetting = "14",
            createdAt = LocalDateTime.now().minusDays(50)
        )
    )

    @Before
    fun setup() {
        beanRepository = mockk()
        getBeanHistoryUseCase = GetBeanHistoryUseCase(beanRepository)
    }

    @Test
    fun `execute should return all beans sorted by creation date`() = runTest {
        // Given
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getBeanHistoryUseCase.execute().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(4, beans?.size)

        // Should be sorted by creation date (newest first)
        assertEquals("bean3", beans?.get(0)?.id) // Most recent
        assertEquals("bean2", beans?.get(1)?.id)
        assertEquals("bean1", beans?.get(2)?.id)
        assertEquals("bean4", beans?.get(3)?.id) // Oldest

        coVerify { beanRepository.getAllBeans() }
    }

    @Test
    fun `execute should handle empty bean list`() = runTest {
        // Given
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(emptyList()))

        // When
        val results = getBeanHistoryUseCase.execute().toList()

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
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.failure(repositoryError))

        // When
        val results = getBeanHistoryUseCase.execute().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isFailure)
        assertEquals(repositoryError, result.exceptionOrNull())
    }

    @Test
    fun `getBeanHistoryWithSearch should filter beans by search query`() = runTest {
        // Given
        val searchQuery = "Ethiopian"
        val activeOnly = false
        val filteredBeans = listOf(testBeans[0]) // Only Ethiopian bean

        coEvery {
            beanRepository.getFilteredBeans(activeOnly = activeOnly, searchQuery = searchQuery)
        } returns flowOf(Result.success(filteredBeans))

        // When
        val results = getBeanHistoryUseCase.getBeanHistoryWithSearch(searchQuery, activeOnly).toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(1, beans?.size)
        assertEquals("Ethiopian Yirgacheffe", beans?.get(0)?.name)

        coVerify { beanRepository.getFilteredBeans(activeOnly = activeOnly, searchQuery = searchQuery) }
    }

    @Test
    fun `getBeansGroupedByStatus should group beans by active status`() = runTest {
        // Given
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getBeanHistoryUseCase.getBeansGroupedByStatus().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val groupedBeans = result.getOrNull()
        assertNotNull(groupedBeans)

        // Check grouping
        assertTrue(groupedBeans?.containsKey(true) == true) // Active beans
        assertTrue(groupedBeans?.containsKey(false) == true) // Inactive beans

        assertEquals(2, groupedBeans?.get(true)?.size) // bean1, bean3
        assertEquals(2, groupedBeans?.get(false)?.size) // bean2, bean4

        // Check active beans
        val activeBeans = groupedBeans?.get(true)
        assertTrue(activeBeans?.any { it.id == "bean1" } == true)
        assertTrue(activeBeans?.any { it.id == "bean3" } == true)

        // Check inactive beans
        val inactiveBeans = groupedBeans?.get(false)
        assertTrue(inactiveBeans?.any { it.id == "bean2" } == true)
        assertTrue(inactiveBeans?.any { it.id == "bean4" } == true)
    }

    @Test
    fun `getBeansByRoastDateRange should filter beans by date range`() = runTest {
        // Given
        val startDate = LocalDate.now().minusDays(10)
        val endDate = LocalDate.now().minusDays(1)

        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getBeanHistoryUseCase.getBeansByRoastDateRange(startDate, endDate).toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(2, beans?.size) // bean1 (7 days) and bean2 (3 days)

        // Should be sorted by roast date (newest first)
        assertEquals("bean2", beans?.get(0)?.id) // 3 days ago
        assertEquals("bean1", beans?.get(1)?.id) // 7 days ago

        // Should not include bean3 (20 days) or bean4 (45 days)
        assertFalse(beans?.any { it.id == "bean3" } == true)
        assertFalse(beans?.any { it.id == "bean4" } == true)
    }

    @Test
    fun `getBeansGroupedByRoastMonth should group beans by year-month`() = runTest {
        // Given - Use fixed dates to avoid month boundary issues
        val currentMonthDate = LocalDate.of(2024, 8, 15) // August 15, 2024
        val lastMonthDate = LocalDate.of(2024, 7, 15) // July 15, 2024

        val currentMonth = "2024-08"
        val lastMonthKey = "2024-07"

        // Create beans with different roast months
        val beansWithDifferentMonths = listOf(
            testBeans[0].copy(roastDate = currentMonthDate), // Current month
            testBeans[1].copy(roastDate = currentMonthDate.minusDays(3)), // Current month
            testBeans[2].copy(roastDate = lastMonthDate), // Last month
            testBeans[3].copy(roastDate = lastMonthDate.minusDays(5)) // Last month
        )

        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(beansWithDifferentMonths))

        // When
        val results = getBeanHistoryUseCase.getBeansGroupedByRoastMonth().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val groupedBeans = result.getOrNull()
        assertNotNull(groupedBeans)

        // Should have groups for current month and last month
        assertTrue("Should contain current month key: $currentMonth", groupedBeans?.containsKey(currentMonth) == true)
        assertTrue("Should contain last month key: $lastMonthKey", groupedBeans?.containsKey(lastMonthKey) == true)

        assertEquals(2, groupedBeans?.get(currentMonth)?.size)
        assertEquals(2, groupedBeans?.get(lastMonthKey)?.size)
    }

    @Test
    fun `getRecentlyAddedBeans should return beans added within last 30 days`() = runTest {
        // Given
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getBeanHistoryUseCase.getRecentlyAddedBeans().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(3, beans?.size) // bean1 (10 days), bean2 (5 days), bean3 (2 days)

        // Should not include bean4 (50 days old)
        assertFalse(beans?.any { it.id == "bean4" } == true)

        // Should be sorted by creation date (newest first)
        assertEquals("bean3", beans?.get(0)?.id) // 2 days ago
        assertEquals("bean2", beans?.get(1)?.id) // 5 days ago
        assertEquals("bean1", beans?.get(2)?.id) // 10 days ago
    }

    @Test
    fun `getBeanHistoryStats should return correct statistics`() = runTest {
        // Given
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(testBeans))

        // When
        val result = getBeanHistoryUseCase.getBeanHistoryStats()

        // Then
        assertTrue(result.isSuccess)
        val stats = result.getOrNull()
        assertNotNull(stats)

        assertEquals(4, stats?.totalBeans)
        assertEquals(2, stats?.activeBeans) // bean1, bean3
        assertEquals(2, stats?.inactiveBeans) // bean2, bean4
        assertEquals(2, stats?.freshBeans) // bean1 (7 days) and bean3 (20 days) are fresh

        // Average days since roast: (7 + 3 + 20 + 45) / 4 = 18.75
        assertEquals(18.75, stats?.averageDaysSinceRoast ?: 0.0, 0.01)

        assertEquals(LocalDate.now().minusDays(45), stats?.oldestRoastDate) // bean4
        assertEquals(LocalDate.now().minusDays(3), stats?.newestRoastDate) // bean2
    }

    @Test
    fun `getBeanHistoryStats should handle empty bean list`() = runTest {
        // Given
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(emptyList()))

        // When
        val result = getBeanHistoryUseCase.getBeanHistoryStats()

        // Then
        assertTrue(result.isSuccess)
        val stats = result.getOrNull()
        assertNotNull(stats)

        assertEquals(0, stats?.totalBeans)
        assertEquals(0, stats?.activeBeans)
        assertEquals(0, stats?.inactiveBeans)
        assertEquals(0, stats?.freshBeans)
        assertEquals(0.0, stats?.averageDaysSinceRoast ?: 0.0, 0.01)
        assertNull(stats?.oldestRoastDate)
        assertNull(stats?.newestRoastDate)
    }

    @Test
    fun `getInactiveBeans should return only inactive beans`() = runTest {
        // Given
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getBeanHistoryUseCase.getInactiveBeans().toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(2, beans?.size) // bean2, bean4

        // Should only include inactive beans
        assertTrue(beans?.all { !it.isActive } == true)
        assertTrue(beans?.any { it.id == "bean2" } == true)
        assertTrue(beans?.any { it.id == "bean4" } == true)

        // Should not include active beans
        assertFalse(beans?.any { it.id == "bean1" } == true)
        assertFalse(beans?.any { it.id == "bean3" } == true)
    }

    @Test
    fun `searchBeansByName should return all beans when query is empty`() = runTest {
        // Given
        val query = ""
        coEvery { beanRepository.getAllBeans() } returns flowOf(Result.success(testBeans))

        // When
        val results = getBeanHistoryUseCase.searchBeansByName(query).toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(4, beans?.size)

        coVerify { beanRepository.getAllBeans() }
    }

    @Test
    fun `searchBeansByName should filter beans when query is provided`() = runTest {
        // Given
        val query = "Ethiopian"
        val filteredBeans = listOf(testBeans[0])

        coEvery {
            beanRepository.getFilteredBeans(activeOnly = false, searchQuery = query)
        } returns flowOf(Result.success(filteredBeans))

        // When
        val results = getBeanHistoryUseCase.searchBeansByName(query).toList()

        // Then
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result.isSuccess)

        val beans = result.getOrNull()
        assertNotNull(beans)
        assertEquals(1, beans?.size)
        assertEquals("Ethiopian Yirgacheffe", beans?.get(0)?.name)

        coVerify { beanRepository.getFilteredBeans(activeOnly = false, searchQuery = query) }
    }

    @Test
    fun `BeanHistoryStats should have correct default values`() {
        // When
        val stats = BeanHistoryStats()

        // Then
        assertEquals(0, stats.totalBeans)
        assertEquals(0, stats.activeBeans)
        assertEquals(0, stats.inactiveBeans)
        assertEquals(0, stats.freshBeans)
        assertEquals(0.0, stats.averageDaysSinceRoast, 0.01)
        assertNull(stats.oldestRoastDate)
        assertNull(stats.newestRoastDate)
    }
}
