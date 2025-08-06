package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for GetBeanPhotoUseCase.
 * Tests photo retrieval, validation, and error handling scenarios.
 */
class GetBeanPhotoUseCaseTest {

    private lateinit var photoStorageManager: PhotoStorageManager
    private lateinit var getBeanPhotoUseCase: GetBeanPhotoUseCase

    private val mockPhotoFile = mockk<File>()

    @Before
    fun setup() {
        photoStorageManager = mockk()
        getBeanPhotoUseCase = GetBeanPhotoUseCase(photoStorageManager)
    }

    @Test
    fun `execute should return photo file successfully`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(photoPath) } returns mockPhotoFile
        every { mockPhotoFile.exists() } returns true
        every { mockPhotoFile.canRead() } returns true

        // When
        val result = getBeanPhotoUseCase.execute(photoPath)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockPhotoFile, result.getOrNull())

        coVerify { photoStorageManager.getPhotoFile(photoPath) }
        coVerify { mockPhotoFile.exists() }
        coVerify { mockPhotoFile.canRead() }
    }

    @Test
    fun `execute should trim whitespace from photo path`() = runTest {
        // Given
        val photoPath = "  test-photo-path.jpg  "
        val trimmedPath = "test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(trimmedPath) } returns mockPhotoFile
        every { mockPhotoFile.exists() } returns true
        every { mockPhotoFile.canRead() } returns true

        // When
        val result = getBeanPhotoUseCase.execute(photoPath)

        // Then
        assertTrue(result.isSuccess)
        coVerify { photoStorageManager.getPhotoFile(trimmedPath) }
    }

    @Test
    fun `execute should fail with empty photo path`() = runTest {
        // Given
        val photoPath = ""

        // When
        val result = getBeanPhotoUseCase.execute(photoPath)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.PHOTO_INVALID_URI, (exception as DomainException).errorCode)
        assertTrue(exception.message?.contains("Photo path cannot be empty") == true)

        coVerify(exactly = 0) { photoStorageManager.getPhotoFile(any()) }
    }

    @Test
    fun `execute should fail with whitespace-only photo path`() = runTest {
        // Given
        val photoPath = "   "

        // When
        val result = getBeanPhotoUseCase.execute(photoPath)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.PHOTO_INVALID_URI, (exception as DomainException).errorCode)
    }

    @Test
    fun `execute should fail when photo storage manager returns null`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(photoPath) } returns null

        // When
        val result = getBeanPhotoUseCase.execute(photoPath)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.PHOTO_NOT_FOUND, (exception as DomainException).errorCode)
        assertTrue(exception.message?.contains("Photo file not found at path: $photoPath") == true)
    }

    @Test
    fun `execute should fail when file does not exist`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"
        val absolutePath = "/path/to/test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(photoPath) } returns mockPhotoFile
        every { mockPhotoFile.exists() } returns false
        every { mockPhotoFile.absolutePath } returns absolutePath

        // When
        val result = getBeanPhotoUseCase.execute(photoPath)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.PHOTO_NOT_FOUND, (exception as DomainException).errorCode)
        assertTrue(exception.message?.contains("Photo file does not exist: $absolutePath") == true)

        coVerify { mockPhotoFile.exists() }
        coVerify(exactly = 0) { mockPhotoFile.canRead() }
    }

    @Test
    fun `execute should fail when file is not readable`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"
        val absolutePath = "/path/to/test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(photoPath) } returns mockPhotoFile
        every { mockPhotoFile.exists() } returns true
        every { mockPhotoFile.canRead() } returns false
        every { mockPhotoFile.absolutePath } returns absolutePath

        // When
        val result = getBeanPhotoUseCase.execute(photoPath)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.PHOTO_FILE_ACCESS_ERROR, (exception as DomainException).errorCode)
        assertTrue(exception.message?.contains("Cannot read photo file: $absolutePath") == true)

        coVerify { mockPhotoFile.exists() }
        coVerify { mockPhotoFile.canRead() }
    }

    @Test
    fun `execute should handle unexpected exceptions`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"
        val unexpectedException = RuntimeException("Unexpected error")

        coEvery { photoStorageManager.getPhotoFile(photoPath) } throws unexpectedException

        // When
        val result = getBeanPhotoUseCase.execute(photoPath)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.UNKNOWN_ERROR, (exception as DomainException).errorCode)
        assertEquals(unexpectedException, exception.cause)
        assertTrue(exception.message?.contains("Unexpected error retrieving photo file") == true)
    }

    @Test
    fun `photoExists should return true when photo exists and is readable`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(photoPath) } returns mockPhotoFile
        every { mockPhotoFile.exists() } returns true
        every { mockPhotoFile.canRead() } returns true

        // When
        val result = getBeanPhotoUseCase.photoExists(photoPath)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)

        coVerify { photoStorageManager.getPhotoFile(photoPath) }
        coVerify { mockPhotoFile.exists() }
        coVerify { mockPhotoFile.canRead() }
    }

    @Test
    fun `photoExists should return false when photo does not exist`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(photoPath) } returns mockPhotoFile
        every { mockPhotoFile.exists() } returns false

        // When
        val result = getBeanPhotoUseCase.photoExists(photoPath)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)

        coVerify { mockPhotoFile.exists() }
        coVerify(exactly = 0) { mockPhotoFile.canRead() }
    }

    @Test
    fun `photoExists should return false when photo is not readable`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(photoPath) } returns mockPhotoFile
        every { mockPhotoFile.exists() } returns true
        every { mockPhotoFile.canRead() } returns false

        // When
        val result = getBeanPhotoUseCase.photoExists(photoPath)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)

        coVerify { mockPhotoFile.exists() }
        coVerify { mockPhotoFile.canRead() }
    }

    @Test
    fun `photoExists should return false when storage manager returns null`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(photoPath) } returns null

        // When
        val result = getBeanPhotoUseCase.photoExists(photoPath)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)
    }

    @Test
    fun `photoExists should return false with empty photo path`() = runTest {
        // Given
        val photoPath = ""

        // When
        val result = getBeanPhotoUseCase.photoExists(photoPath)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)

        coVerify(exactly = 0) { photoStorageManager.getPhotoFile(any()) }
    }

    @Test
    fun `photoExists should trim whitespace from photo path`() = runTest {
        // Given
        val photoPath = "  test-photo-path.jpg  "
        val trimmedPath = "test-photo-path.jpg"

        coEvery { photoStorageManager.getPhotoFile(trimmedPath) } returns mockPhotoFile
        every { mockPhotoFile.exists() } returns true
        every { mockPhotoFile.canRead() } returns true

        // When
        val result = getBeanPhotoUseCase.photoExists(photoPath)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        coVerify { photoStorageManager.getPhotoFile(trimmedPath) }
    }

    @Test
    fun `photoExists should handle unexpected exceptions`() = runTest {
        // Given
        val photoPath = "test-photo-path.jpg"
        val unexpectedException = RuntimeException("Unexpected error")

        coEvery { photoStorageManager.getPhotoFile(photoPath) } throws unexpectedException

        // When
        val result = getBeanPhotoUseCase.photoExists(photoPath)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DomainException)
        assertEquals(DomainErrorCode.UNKNOWN_ERROR, (exception as DomainException).errorCode)
        assertEquals(unexpectedException, exception.cause)
        assertTrue(exception.message?.contains("Unexpected error checking photo existence") == true)
    }
}