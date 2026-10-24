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
}
