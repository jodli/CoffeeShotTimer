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
}
