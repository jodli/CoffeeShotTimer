package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.repository.RepositoryException
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
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
 * Unit tests for RemovePhotoFromBeanUseCase.
 * Tests photo removal, error handling, and edge case scenarios.
 */
class RemovePhotoFromBeanUseCaseTest {

    private lateinit var beanRepository: BeanRepository
    private lateinit var photoStorageManager: PhotoStorageManager
    private lateinit var removePhotoFromBeanUseCase: RemovePhotoFromBeanUseCase

    private val testBeanWithPhoto = Bean(
        id = "test-bean-id",
        name = "Ethiopian Yirgacheffe",
        roastDate = LocalDate.now().minusDays(7),
        notes = "Test notes",
        isActive = true,
        lastGrinderSetting = "15",
        photoPath = "test-photo-path.jpg",
        createdAt = LocalDateTime.now().minusDays(1)
    )

    private val testBeanWithoutPhoto = testBeanWithPhoto.copy(photoPath = null)

    @Before
    fun setup() {
        beanRepository = mockk()
        photoStorageManager = mockk()
        removePhotoFromBeanUseCase = RemovePhotoFromBeanUseCase(beanRepository, photoStorageManager)
    }

    @Test
    fun `execute should remove photo successfully`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val photoPath = "test-photo-path.jpg"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBeanWithPhoto)
        coEvery { beanRepository.updateBean(any()) } returns Result.success(Unit)
        coEvery { photoStorageManager.deletePhoto(photoPath) } returns Result.success(Unit)

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isSuccess)

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify { beanRepository.updateBean(testBeanWithPhoto.copy(photoPath = null)) }
        coVerify { photoStorageManager.deletePhoto(photoPath) }
    }

    @Test
    fun `execute should succeed when bean has no photo`() = runTest {
        // Given
        val beanId = "test-bean-id"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBeanWithoutPhoto)

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isSuccess)

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify(exactly = 0) { beanRepository.updateBean(any()) }
        coVerify(exactly = 0) { photoStorageManager.deletePhoto(any()) }
    }

    @Test
    fun `execute should succeed when bean has empty photo path`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val beanWithEmptyPhotoPath = testBeanWithPhoto.copy(photoPath = "")

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(beanWithEmptyPhotoPath)

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isSuccess)

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify(exactly = 0) { beanRepository.updateBean(any()) }
        coVerify(exactly = 0) { photoStorageManager.deletePhoto(any()) }
    }

    @Test
    fun `execute should continue when photo file deletion fails`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val photoPath = "test-photo-path.jpg"
        val deleteException = Exception("File deletion failed")

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBeanWithPhoto)
        coEvery { beanRepository.updateBean(any()) } returns Result.success(Unit)
        coEvery { photoStorageManager.deletePhoto(photoPath) } returns Result.failure(deleteException)

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isSuccess)

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify { beanRepository.updateBean(testBeanWithPhoto.copy(photoPath = null)) }
        coVerify { photoStorageManager.deletePhoto(photoPath) }
    }

    @Test
    fun `execute should trim whitespace from bean ID`() = runTest {
        // Given
        val beanId = "  test-bean-id  "
        val trimmedBeanId = "test-bean-id"

        coEvery { beanRepository.getBeanById(trimmedBeanId) } returns Result.success(testBeanWithoutPhoto)

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { beanRepository.getBeanById(trimmedBeanId) }
    }

    @Test
    fun `execute should fail with empty bean ID`() = runTest {
        // Given
        val beanId = ""

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.BEAN_ID_EMPTY, (exception as DomainException).errorCode)

        coVerify(exactly = 0) { beanRepository.getBeanById(any()) }
    }

    @Test
    fun `execute should fail with whitespace-only bean ID`() = runTest {
        // Given
        val beanId = "   "

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.BEAN_ID_EMPTY, (exception as DomainException).errorCode)
    }

    @Test
    fun `execute should fail when bean repository fails`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val repositoryException = RepositoryException.DatabaseError("Database error")

        coEvery { beanRepository.getBeanById(beanId) } returns Result.failure(repositoryException)

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(repositoryException, result.exceptionOrNull())

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify(exactly = 0) { beanRepository.updateBean(any()) }
        coVerify(exactly = 0) { photoStorageManager.deletePhoto(any()) }
    }

    @Test
    fun `execute should fail when bean not found`() = runTest {
        // Given
        val beanId = "non-existent-bean"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(null)

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.BEAN_NOT_FOUND, (exception as DomainException).errorCode)

        coVerify(exactly = 0) { beanRepository.updateBean(any()) }
        coVerify(exactly = 0) { photoStorageManager.deletePhoto(any()) }
    }

    @Test
    fun `execute should fail when database update fails`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val photoPath = "test-photo-path.jpg"
        val updateException = RepositoryException.DatabaseError("Update failed")

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBeanWithPhoto)
        coEvery { beanRepository.updateBean(any()) } returns Result.failure(updateException)

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(updateException, result.exceptionOrNull())

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify { beanRepository.updateBean(testBeanWithPhoto.copy(photoPath = null)) }
        coVerify(exactly = 0) { photoStorageManager.deletePhoto(any()) }
    }

    @Test
    fun `execute should handle unexpected exceptions`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val unexpectedException = RuntimeException("Unexpected error")

        coEvery { beanRepository.getBeanById(beanId) } throws unexpectedException

        // When
        val result = removePhotoFromBeanUseCase.execute(beanId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.UNKNOWN_ERROR, (exception as DomainException).errorCode)
        assertEquals(unexpectedException, exception.cause)
        assertTrue(exception.message?.contains("Unexpected error removing photo from bean") == true)
    }
}
