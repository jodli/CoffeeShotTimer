package com.jodli.coffeeshottimer.domain.usecase

import android.net.Uri
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
 * Unit tests for AddPhotoToBeanUseCase.
 * Tests photo addition, error handling, and cleanup scenarios.
 */
class AddPhotoToBeanUseCaseTest {

    private lateinit var beanRepository: BeanRepository
    private lateinit var photoStorageManager: PhotoStorageManager
    private lateinit var addPhotoToBeanUseCase: AddPhotoToBeanUseCase

    private val testBean = Bean(
        id = "test-bean-id",
        name = "Ethiopian Yirgacheffe",
        roastDate = LocalDate.now().minusDays(7),
        notes = "Test notes",
        isActive = true,
        lastGrinderSetting = "15",
        photoPath = null,
        createdAt = LocalDateTime.now().minusDays(1)
    )

    private val testBeanWithPhoto = testBean.copy(photoPath = "old-photo-path.jpg")
    private val mockImageUri = mockk<Uri>()

    @Before
    fun setup() {
        beanRepository = mockk()
        photoStorageManager = mockk()
        addPhotoToBeanUseCase = AddPhotoToBeanUseCase(beanRepository, photoStorageManager)
    }

    @Test
    fun `execute should add photo successfully to bean without existing photo`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val expectedPhotoPath = "test-bean-id_photo.jpg"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBean)
        coEvery { photoStorageManager.savePhoto(mockImageUri, beanId) } returns Result.success(expectedPhotoPath)
        coEvery { beanRepository.updateBean(any()) } returns Result.success(Unit)

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedPhotoPath, result.getOrNull())

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify { photoStorageManager.savePhoto(mockImageUri, beanId) }
        coVerify { beanRepository.updateBean(testBean.copy(photoPath = expectedPhotoPath)) }
        coVerify(exactly = 0) { photoStorageManager.deletePhoto(any()) }
    }

    @Test
    fun `execute should replace existing photo successfully`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val oldPhotoPath = "old-photo-path.jpg"
        val newPhotoPath = "test-bean-id_photo.jpg"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBeanWithPhoto)
        coEvery { photoStorageManager.deletePhoto(oldPhotoPath) } returns Result.success(Unit)
        coEvery { photoStorageManager.savePhoto(mockImageUri, beanId) } returns Result.success(newPhotoPath)
        coEvery { beanRepository.updateBean(any()) } returns Result.success(Unit)

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(newPhotoPath, result.getOrNull())

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify { photoStorageManager.deletePhoto(oldPhotoPath) }
        coVerify { photoStorageManager.savePhoto(mockImageUri, beanId) }
        coVerify { beanRepository.updateBean(testBeanWithPhoto.copy(photoPath = newPhotoPath)) }
    }

    @Test
    fun `execute should continue when old photo deletion fails`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val oldPhotoPath = "old-photo-path.jpg"
        val newPhotoPath = "test-bean-id_photo.jpg"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBeanWithPhoto)
        coEvery { photoStorageManager.deletePhoto(oldPhotoPath) } returns Result.failure(Exception("Delete failed"))
        coEvery { photoStorageManager.savePhoto(mockImageUri, beanId) } returns Result.success(newPhotoPath)
        coEvery { beanRepository.updateBean(any()) } returns Result.success(Unit)

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(newPhotoPath, result.getOrNull())

        coVerify { photoStorageManager.deletePhoto(oldPhotoPath) }
        coVerify { photoStorageManager.savePhoto(mockImageUri, beanId) }
        coVerify { beanRepository.updateBean(testBeanWithPhoto.copy(photoPath = newPhotoPath)) }
    }

    @Test
    fun `execute should trim whitespace from bean ID`() = runTest {
        // Given
        val beanId = "  test-bean-id  "
        val trimmedBeanId = "test-bean-id"
        val expectedPhotoPath = "test-bean-id_photo.jpg"

        coEvery { beanRepository.getBeanById(trimmedBeanId) } returns Result.success(testBean)
        coEvery { photoStorageManager.savePhoto(mockImageUri, trimmedBeanId) } returns Result.success(expectedPhotoPath)
        coEvery { beanRepository.updateBean(any()) } returns Result.success(Unit)

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isSuccess)
        coVerify { beanRepository.getBeanById(trimmedBeanId) }
        coVerify { photoStorageManager.savePhoto(mockImageUri, trimmedBeanId) }
    }

    @Test
    fun `execute should fail with empty bean ID`() = runTest {
        // Given
        val beanId = ""

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.BEAN_ID_EMPTY, (exception as DomainException).errorCode)

        coVerify(exactly = 0) { beanRepository.getBeanById(any()) }
        coVerify(exactly = 0) { photoStorageManager.savePhoto(any(), any()) }
    }

    @Test
    fun `execute should fail with whitespace-only bean ID`() = runTest {
        // Given
        val beanId = "   "

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

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
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isFailure)
        assertEquals(repositoryException, result.exceptionOrNull())

        coVerify { beanRepository.getBeanById(beanId) }
        coVerify(exactly = 0) { photoStorageManager.savePhoto(any(), any()) }
    }

    @Test
    fun `execute should fail when bean not found`() = runTest {
        // Given
        val beanId = "non-existent-bean"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(null)

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.BEAN_NOT_FOUND, (exception as DomainException).errorCode)

        coVerify(exactly = 0) { photoStorageManager.savePhoto(any(), any()) }
    }

    @Test
    fun `execute should fail when photo save fails`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val saveException = Exception("Save failed")

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBean)
        coEvery { photoStorageManager.savePhoto(mockImageUri, beanId) } returns Result.failure(saveException)

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isFailure)
        assertEquals(saveException, result.exceptionOrNull())

        coVerify { photoStorageManager.savePhoto(mockImageUri, beanId) }
        coVerify(exactly = 0) { beanRepository.updateBean(any()) }
    }

    @Test
    fun `execute should fail when photo save returns empty path`() = runTest {
        // Given
        val beanId = "test-bean-id"

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBean)
        coEvery { photoStorageManager.savePhoto(mockImageUri, beanId) } returns Result.success("")

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.PHOTO_SAVE_FAILED, (exception as DomainException).errorCode)
        assertTrue(exception.message?.contains("Photo path is empty") == true)
    }

    @Test
    fun `execute should cleanup photo when database update fails`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val photoPath = "test-bean-id_photo.jpg"
        val updateException = RepositoryException.DatabaseError("Update failed")

        coEvery { beanRepository.getBeanById(beanId) } returns Result.success(testBean)
        coEvery { photoStorageManager.savePhoto(mockImageUri, beanId) } returns Result.success(photoPath)
        coEvery { beanRepository.updateBean(any()) } returns Result.failure(updateException)
        coEvery { photoStorageManager.deletePhoto(photoPath) } returns Result.success(Unit)

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isFailure)
        assertEquals(updateException, result.exceptionOrNull())

        coVerify { photoStorageManager.savePhoto(mockImageUri, beanId) }
        coVerify { beanRepository.updateBean(testBean.copy(photoPath = photoPath)) }
        coVerify { photoStorageManager.deletePhoto(photoPath) }
    }

    @Test
    fun `execute should handle unexpected exceptions`() = runTest {
        // Given
        val beanId = "test-bean-id"
        val unexpectedException = RuntimeException("Unexpected error")

        coEvery { beanRepository.getBeanById(beanId) } throws unexpectedException

        // When
        val result = addPhotoToBeanUseCase.execute(beanId, mockImageUri)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.UNKNOWN_ERROR, (exception as DomainException).errorCode)
        assertEquals(unexpectedException, exception.cause)
        assertTrue(exception.message?.contains("Unexpected error adding photo to bean") == true)
    }
}