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
import java.time.LocalDateTime

/**
 * Unit tests for UpdateBeanUseCase.
 * Tests bean updates, validation, grinder setting memory, and error handling scenarios.
 */
class UpdateBeanUseCaseTest {

    private lateinit var beanRepository: BeanRepository
    private lateinit var updateBeanUseCase: UpdateBeanUseCase

    private val testBean = Bean(
        id = "test-id",
        name = "Ethiopian Yirgacheffe",
        roastDate = LocalDate.now().minusDays(7),
        notes = "Original notes",
        isActive = true,
        lastGrinderSetting = "15",
        createdAt = LocalDateTime.now().minusDays(1)
    )

    @Before
    fun setup() {
        beanRepository = mockk()
        updateBeanUseCase = UpdateBeanUseCase(beanRepository)
    }

    @Test
    fun `execute should update bean successfully with valid parameters`() = runTest {
        // Given
        val beanId = "test-id"
        val updatedName = "Ethiopian Yirgacheffe - Updated"
        val updatedRoastDate = LocalDate.now().minusDays(5)
        val updatedNotes = "Updated notes"
        val updatedActive = false

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBean)

        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.updateBean(any()) } returns Result.success(Unit)

        // When
        val result = updateBeanUseCase.execute(
            beanId,
            updatedName,
            updatedRoastDate,
            updatedNotes,
            updatedActive
        )

        // Then
        assertTrue(result.isSuccess)
        val updatedBean = result.getOrNull()
        assertNotNull(updatedBean)
        assertEquals(beanId, updatedBean?.id)
        assertEquals(updatedName, updatedBean?.name)
        assertEquals(updatedRoastDate, updatedBean?.roastDate)
        assertEquals(updatedNotes, updatedBean?.notes)
        assertEquals(updatedActive, updatedBean?.isActive)
        assertEquals(testBean.createdAt, updatedBean?.createdAt) // Should preserve original creation time

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify { beanRepository.validateBean(any()) }
        coVerify { beanRepository.updateBean(any()) }
    }

    @Test
    fun `execute should trim whitespace from inputs`() = runTest {
        // Given
        val beanId = "  test-id  "
        val updatedName = "  Ethiopian Yirgacheffe  "
        val updatedNotes = "  Updated notes  "
        val updatedRoastDate = LocalDate.now().minusDays(5)

        coEvery { beanRepository.getBeanById("test-id") } returns Result.success(testBean)

        val validationResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns validationResult
        coEvery { beanRepository.updateBean(any()) } returns Result.success(Unit)

        // When
        val result = updateBeanUseCase.execute(
            beanId,
            updatedName,
            updatedRoastDate,
            updatedNotes,
            true
        )

        // Then
        assertTrue(result.isSuccess)
        val updatedBean = result.getOrNull()
        assertEquals("Ethiopian Yirgacheffe", updatedBean?.name)
        assertEquals("Updated notes", updatedBean?.notes)
    }

    @Test
    fun `execute should fail with empty bean ID`() = runTest {
        // Given
        val beanId = ""
        val updatedName = "Ethiopian Yirgacheffe"
        val updatedRoastDate = LocalDate.now().minusDays(5)

        // When
        val result = updateBeanUseCase.execute(beanId, updatedName, updatedRoastDate)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals((exception as DomainException).errorCode, DomainErrorCode.BEAN_ID_EMPTY)

        coVerify(exactly = 0) { beanRepository.getBeanById(any()) }
    }

    @Test
    fun `execute should fail when bean not found`() = runTest {
        // Given
        val beanId = "non-existent-id"
        val updatedName = "Ethiopian Yirgacheffe"
        val updatedRoastDate = LocalDate.now().minusDays(5)

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(null)

        // When
        val result = updateBeanUseCase.execute(beanId, updatedName, updatedRoastDate)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals((exception as DomainException).errorCode, DomainErrorCode.BEAN_NOT_FOUND)
    }

    @Test
    fun `execute should fail when validation fails`() = runTest {
        // Given
        val beanId = "test-id"
        val updatedName = ""
        val updatedRoastDate = LocalDate.now().minusDays(5)

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBean)

        val validationResult = ValidationResult(
            isValid = false,
            errors = listOf("Bean name cannot be empty")
        )
        coEvery { beanRepository.validateBean(any()) } returns validationResult

        // When
        val result = updateBeanUseCase.execute(beanId, updatedName, updatedRoastDate)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertTrue(exception?.message?.contains("Bean name cannot be empty") == true)

        coVerify(exactly = 0) { beanRepository.updateBean(any()) }
    }

    @Test
    fun `updateActiveStatus should update active status successfully`() = runTest {
        // Given
        val beanId = "test-id"
        val isActive = false

        coEvery { beanRepository.updateBeanActiveStatus(beanId, isActive) } returns Result.success(Unit)

        // When
        val result = updateBeanUseCase.updateActiveStatus(beanId, isActive)

        // Then
        assertTrue(result.isSuccess)
        coVerify { beanRepository.updateBeanActiveStatus(beanId, isActive) }
    }

    @Test
    fun `updateActiveStatus should fail with empty bean ID`() = runTest {
        // Given
        val beanId = ""
        val isActive = false

        // When
        val result = updateBeanUseCase.updateActiveStatus(beanId, isActive)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals((exception as DomainException).errorCode, DomainErrorCode.BEAN_ID_EMPTY)
    }

    @Test
    fun `validateUpdateParameters should return validation result`() = runTest {
        // Given
        val beanId = "test-id"
        val name = "Ethiopian Yirgacheffe"
        val roastDate = LocalDate.now().minusDays(7)
        val notes = "Updated notes"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBean)

        val expectedResult = ValidationResult(isValid = true, errors = emptyList())
        coEvery { beanRepository.validateBean(any()) } returns expectedResult

        // When
        val result = updateBeanUseCase.validateUpdateParameters(beanId, name, roastDate, notes)

        // Then
        assertEquals(expectedResult, result)
        coVerify { beanRepository.getBeanById(beanId) }
        coVerify { beanRepository.validateBean(any()) }
    }

    @Test
    fun `isBeanNameAvailableForUpdate should return true when name is available`() = runTest {
        // Given
        val beanId = "test-id"
        val name = "New Bean Name"

        coEvery { beanRepository.getBeanByName(name) } returns Result.success(null)

        // When
        val result = updateBeanUseCase.isBeanNameAvailableForUpdate(beanId, name)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `isBeanNameAvailableForUpdate should return true when name belongs to same bean`() = runTest {
        // Given
        val beanId = "test-id"
        val name = "Ethiopian Yirgacheffe"

        coEvery { beanRepository.getBeanByName(name) } returns Result.success(testBean)

        // When
        val result = updateBeanUseCase.isBeanNameAvailableForUpdate(beanId, name)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `isBeanNameAvailableForUpdate should return false when name belongs to different bean`() = runTest {
        // Given
        val beanId = "test-id"
        val name = "Ethiopian Yirgacheffe"
        val otherBean = testBean.copy(id = "other-id")

        coEvery { beanRepository.getBeanByName(name) } returns Result.success(otherBean)

        // When
        val result = updateBeanUseCase.isBeanNameAvailableForUpdate(beanId, name)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)
    }

    @Test
    fun `getBeanForEditing should return bean successfully`() = runTest {
        // Given
        val beanId = "test-id"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBean)

        // When
        val result = updateBeanUseCase.getBeanForEditing(beanId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testBean, result.getOrNull())
    }

    @Test
    fun `getBeanForEditing should fail when bean not found`() = runTest {
        // Given
        val beanId = "non-existent-id"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(null)

        // When
        val result = updateBeanUseCase.getBeanForEditing(beanId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals((exception as DomainException).errorCode, DomainErrorCode.BEAN_NOT_FOUND)
    }

    @Test
    fun `bulkUpdateActiveStatus should update all beans successfully`() = runTest {
        // Given
        val beanIds = listOf("bean1", "bean2", "bean3")
        val isActive = false

        coEvery { beanRepository.updateBeanActiveStatus("bean1", isActive) } returns Result.success(Unit)
        coEvery { beanRepository.updateBeanActiveStatus("bean2", isActive) } returns Result.success(Unit)
        coEvery { beanRepository.updateBeanActiveStatus("bean3", isActive) } returns Result.success(Unit)

        // When
        val result = updateBeanUseCase.bulkUpdateActiveStatus(beanIds, isActive)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())

        coVerify { beanRepository.updateBeanActiveStatus("bean1", isActive) }
        coVerify { beanRepository.updateBeanActiveStatus("bean2", isActive) }
        coVerify { beanRepository.updateBeanActiveStatus("bean3", isActive) }
    }

    @Test
    fun `bulkUpdateActiveStatus should handle partial failures`() = runTest {
        // Given
        val beanIds = listOf("bean1", "bean2", "bean3")
        val isActive = false

        coEvery { beanRepository.updateBeanActiveStatus("bean1", isActive) } returns Result.success(Unit)
        coEvery { beanRepository.updateBeanActiveStatus("bean2", isActive) } returns Result.failure(
            RepositoryException.DatabaseError("Database error")
        )
        coEvery { beanRepository.updateBeanActiveStatus("bean3", isActive) } returns Result.success(Unit)

        // When
        val result = updateBeanUseCase.bulkUpdateActiveStatus(beanIds, isActive)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertTrue(exception?.message?.contains("Some beans failed to update") == true)
    }
}
