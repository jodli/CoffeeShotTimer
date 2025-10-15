package com.jodli.coffeeshottimer.data.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
class PhotoStorageManagerTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var photoStorageManager: PhotoStorageManagerImpl
    private lateinit var testFilesDir: File
    private lateinit var testPhotosDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)

        // Create temporary test directories using modern API
        testFilesDir = createTempDirectory("test_files").toFile()
        testPhotosDir = File(testFilesDir, "photos")
        testPhotosDir.mkdirs()

        every { context.filesDir } returns testFilesDir
        every { context.contentResolver } returns contentResolver
        every { context.cacheDir } returns createTempDirectory("test_cache").toFile()

        photoStorageManager = PhotoStorageManagerImpl(context)
    }

    @After
    fun cleanup() {
        // Clean up test directories
        testFilesDir.deleteRecursively()
        clearAllMocks()
    }

    @Test
    fun `savePhoto should save compressed image and return file path`() = runTest {
        // Arrange
        val beanId = "test-bean-123"
        val imageUri = mockk<Uri>()
        val testBitmap = createTestBitmap()
        val inputStream1 = createBitmapInputStream(testBitmap)
        val inputStream2 = createBitmapInputStream(testBitmap)

        // Create a temporary compressed file
        val tempCompressedFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
        val compressedUri = Uri.fromFile(tempCompressedFile)

        // Write test bitmap to the temp file
        FileOutputStream(tempCompressedFile).use { outputStream ->
            testBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }

        // Mock the compression process
        val photoStorageManagerSpy = spyk(photoStorageManager)
        coEvery { photoStorageManagerSpy.compressImage(imageUri) } returns Result.success(compressedUri)
        every { contentResolver.openInputStream(compressedUri) } returns inputStream2

        // Act
        val result = photoStorageManagerSpy.savePhoto(imageUri, beanId)

        // Assert
        assertTrue(result.isSuccess)
        val savedPath = result.getOrThrow()
        assertTrue(savedPath.contains("${beanId}_photo.jpg"))

        val savedFile = File(savedPath)
        assertTrue(savedFile.exists())
        assertTrue(savedFile.length() > 0)

        // Cleanup
        tempCompressedFile.delete()
    }

    @Test
    fun `savePhoto should return failure when input stream is null`() = runTest {
        // Arrange
        val beanId = "test-bean-123"
        val imageUri = mockk<Uri>()

        every { contentResolver.openInputStream(any()) } returns null

        // Act
        val result = photoStorageManager.savePhoto(imageUri, beanId)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }

    @Test
    fun `deletePhoto should delete existing file and return success`() = runTest {
        // Arrange
        val testFile = File(testPhotosDir, "test_photo.jpg")
        testFile.createNewFile()
        assertTrue(testFile.exists())

        // Act
        val result = photoStorageManager.deletePhoto(testFile.absolutePath)

        // Assert
        assertTrue(result.isSuccess)
        assertFalse(testFile.exists())
    }

    @Test
    fun `deletePhoto should return failure when file does not exist`() = runTest {
        // Arrange
        val nonExistentPath = "/non/existent/path.jpg"

        // Act
        val result = photoStorageManager.deletePhoto(nonExistentPath)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `getPhotoFile should return file when it exists`() = runTest {
        // Arrange
        val testFile = File(testPhotosDir, "test_photo.jpg")
        testFile.createNewFile()

        // Act
        val result = photoStorageManager.getPhotoFile(testFile.absolutePath)

        // Assert
        assertNotNull(result)
        assertEquals(testFile.absolutePath, result?.absolutePath)
    }

    @Test
    fun `getPhotoFile should return null when file does not exist`() = runTest {
        // Arrange
        val nonExistentPath = "/non/existent/path.jpg"

        // Act
        val result = photoStorageManager.getPhotoFile(nonExistentPath)

        // Assert
        assertNull(result)
    }

    @Test
    fun `compressImage should compress and return temporary file URI`() = runTest {
        // Arrange
        val imageUri = mockk<Uri>()
        val testBitmap = createTestBitmap(width = 3000, height = 2000) // Large image
        val inputStream1 = createBitmapInputStream(testBitmap)
        val inputStream2 = createBitmapInputStream(testBitmap)

        // Mock multiple calls to openInputStream for EXIF reading and bitmap decoding
        every { contentResolver.openInputStream(imageUri) } returnsMany listOf(inputStream1, inputStream2)

        // Act
        val result = photoStorageManager.compressImage(imageUri)

        // Assert
        assertTrue("Compression should succeed", result.isSuccess)
        val compressedUri = result.getOrThrow()
        assertNotNull(compressedUri)

        // Verify the compressed file exists
        val compressedFile = File(compressedUri.path!!)
        assertTrue("Compressed file should exist", compressedFile.exists())
        assertTrue("Compressed file should have content", compressedFile.length() > 0)
    }

    @Test
    fun `compressImage should handle invalid URI gracefully`() = runTest {
        // Arrange
        val invalidUri = mockk<Uri>()
        every { contentResolver.openInputStream(invalidUri) } returns null

        // Act
        val result = photoStorageManager.compressImage(invalidUri)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `cleanupOrphanedFiles should remove unreferenced files`() = runTest {
        // Arrange
        val referencedFile = File(testPhotosDir, "bean1_photo.jpg")
        val orphanedFile1 = File(testPhotosDir, "bean2_photo.jpg")
        val orphanedFile2 = File(testPhotosDir, "bean3_photo.jpg")

        referencedFile.createNewFile()
        orphanedFile1.createNewFile()
        orphanedFile2.createNewFile()

        val referencedPaths = setOf(referencedFile.absolutePath)

        // Act
        val result = photoStorageManager.cleanupOrphanedFiles(referencedPaths)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow()) // Should delete 2 orphaned files

        assertTrue(referencedFile.exists()) // Referenced file should remain
        assertFalse(orphanedFile1.exists()) // Orphaned files should be deleted
        assertFalse(orphanedFile2.exists())
    }

    @Test
    fun `cleanupOrphanedFiles should return 0 when photos directory does not exist`() = runTest {
        // Arrange
        testPhotosDir.deleteRecursively()
        val referencedPaths = setOf("/some/path.jpg")

        // Act
        val result = photoStorageManager.cleanupOrphanedFiles(referencedPaths)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun `getTotalStorageSize should return correct total size`() = runTest {
        // Arrange
        val file1 = File(testPhotosDir, "photo1.jpg")
        val file2 = File(testPhotosDir, "photo2.jpg")

        file1.writeText("test content 1")
        file2.writeText("test content 2 longer")

        val expectedSize = file1.length() + file2.length()

        // Act
        val totalSize = photoStorageManager.getTotalStorageSize()

        // Assert
        assertEquals(expectedSize, totalSize)
    }

    @Test
    fun `getTotalStorageSize should return 0 when photos directory does not exist`() = runTest {
        // Arrange
        testPhotosDir.deleteRecursively()

        // Act
        val totalSize = photoStorageManager.getTotalStorageSize()

        // Assert
        assertEquals(0L, totalSize)
    }

    @Test
    fun `getTotalStorageSize should ignore subdirectories`() = runTest {
        // Arrange
        val file = File(testPhotosDir, "photo.jpg")
        val subDir = File(testPhotosDir, "subdir")
        val subDirFile = File(subDir, "subfile.jpg")

        file.writeText("test content")
        subDir.mkdirs()
        subDirFile.writeText("sub content") // Should be ignored

        val expectedSize = file.length() // Only the direct file should be counted

        // Act
        val totalSize = photoStorageManager.getTotalStorageSize()

        // Assert
        assertEquals(expectedSize, totalSize)
    }

    // Helper methods
    private fun createTestBitmap(width: Int = 100, height: Int = 100): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun createBitmapInputStream(bitmap: Bitmap): InputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }
}
