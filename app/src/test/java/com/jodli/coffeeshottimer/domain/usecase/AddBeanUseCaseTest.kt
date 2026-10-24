package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.ValidationResult
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for AddBeanUseCase.
 * Tests bean creation, validation, and error handling scenarios.
 */
class AddBeanUseCaseTest {

    private lateinit var beanRepository: BeanRepository
    private lateinit var addBeanUseCase: AddBeanUseCase

    @Before
    fun setup() {
        beanRepository = mockk()
        addBeanUseCase = AddBeanUseCase(beanRepository)
    }

    @Test
    fun `execute should create bean successfully with valid parameters`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)
        val notes = "Floral and citrusy"
        val isActive = true

        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.success(Unit)

        // When
        val result = addBeanUseCase.execute(name, roastDate, notes, isActive)

        // Then
        assertTrue(result.isSuccess)
        val createdBean = result.getOrNull()
        assertNotNull(createdBean)
        assertEquals(name, createdBean?.name)
        assertEquals(roastDate, createdBean?.roastDate)
        assertEquals(notes, createdBean?.notes)
        assertEquals(isActive, createdBean?.isActive)

        coVerify { beanRepository.validateBean(any()) }
        coVerify { beanRepository.addBean(any()) }
    }

    @Test
    fun `execute should trim whitespace from inputs`() = runTest {
        // Given
        val name = "  Ethiopian Yirgacheffe  "
        val notes = "  Floral and citrusy  "
        val roastDate = LocalDate.now().minusDays(7)

        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.success(Unit)

        // When
        val result = addBeanUseCase.execute(name, roastDate, notes, true)

        // Then
        assertTrue(result.isSuccess)
        val createdBean = result.getOrNull()
        assertEquals("Ethiopian Yirgacheffe", createdBean?.name)
        assertEquals("Floral and citrusy", createdBean?.notes)
    }

    @Test
    fun `execute should handle empty grinder setting`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)

        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.success(Unit)

        // When
        val result = addBeanUseCase.execute(name, roastDate)

        // Then
        assertTrue(result.isSuccess)
        val createdBean = result.getOrNull()
    }

    @Test
    fun `execute should fail when validation fails`() = runTest {
        // Given
        val name = ""
        val roastDate = LocalDate.now().minusDays(7)

        val validationResult = ValidationResult(
            isValid = false,
            errors = listOf("Bean name cannot be empty")
        )
        coEvery { beanRepository.validateBean(any()) } returns validationResult

        // When
        val result = addBeanUseCase.execute(name, roastDate)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertTrue(exception?.message?.contains("Bean name cannot be empty") == true)

        coVerify { beanRepository.validateBean(any()) }
        coVerify(exactly = 0) { beanRepository.addBean(any()) }
    }

    @Test
    fun `execute should fail when repository add fails`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)

        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.addBean(any()) } returns Result.failure(
            RepositoryException.DatabaseError("Database error")
        )

        // When
        val result = addBeanUseCase.execute(name, roastDate)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RepositoryException.DatabaseError)
    }

    @Test
    fun `execute should handle repository exception`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)

        coEvery { beanRepository.validateBean(any()) } throws RuntimeException("Unexpected error")

        // When
        val result = addBeanUseCase.execute(name, roastDate)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertTrue(exception?.message?.contains("Unexpected error adding bean") == true)
    }

    @Test
    fun `isBeanNameAvailable should return true when name is available`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        coEvery { beanRepository.getBeanByName(name) } returns Result.success(null)

        // When
        val result = addBeanUseCase.isBeanNameAvailable(name)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `isBeanNameAvailable should return false when name is taken`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        val existingBean = Bean(
            name = name,
            roastDate = LocalDate.now().minusDays(7)
        )
        coEvery { beanRepository.getBeanByName(name) } returns Result.success(existingBean)

        // When
        val result = addBeanUseCase.isBeanNameAvailable(name)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)
    }

    @Test
    fun `isBeanNameAvailable should fail with empty name`() = runTest {
        // Given
        val name = ""

        // When
        val result = addBeanUseCase.isBeanNameAvailable(name)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals((exception as DomainException).errorCode, DomainErrorCode.BEAN_NAME_EMPTY)
    }

    @Test
    fun `isBeanNameAvailable should handle repository error`() = runTest {
        // Given
        val name = "Ethiopian Yirgacheffe"
        coEvery { beanRepository.getBeanByName(name) } returns Result.failure(
            RepositoryException.DatabaseError("Database error")
        )

        // When
        val result = addBeanUseCase.isBeanNameAvailable(name)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RepositoryException.DatabaseError)
    }
}
