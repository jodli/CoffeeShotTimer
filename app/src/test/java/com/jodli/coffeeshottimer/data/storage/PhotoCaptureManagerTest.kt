package com.jodli.coffeeshottimer.data.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
class PhotoCaptureManagerTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var photoCaptureManager: PhotoCaptureManagerImpl
    private lateinit var testCacheDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        
        // Create temporary test cache directory
        testCacheDir = createTempDirectory("test_cache").toFile()
        
        every { context.cacheDir } returns testCacheDir
        every { context.packageManager } returns packageManager
        
        photoCaptureManager = PhotoCaptureManagerImpl(context)
    }

    @After
    fun cleanup() {
        // Clean up test directories
        testCacheDir.deleteRecursively()
        clearAllMocks()
    }

    @Test
    fun `createImageCaptureIntent should return camera intent with temp file URI`() {
        // Act
        val (intent, tempUri) = photoCaptureManager.createImageCaptureIntent()
        
        // Assert
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent.action)
        assertNotNull(intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java))
        assertNotNull(tempUri)
        assertTrue(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        
        // Verify temp file was created
        val tempFile = File(tempUri.path!!)
        assertTrue("Temp file should exist", tempFile.exists())
        assertTrue("Temp file should be in temp_photos directory", 
                  tempFile.parentFile?.name == "temp_photos")
    }


    @Test
    fun `isCameraPermissionGranted should return true when permission granted`() {
        // Arrange
        mockkStatic(ContextCompat::class)
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // Act
        val result = photoCaptureManager.isCameraPermissionGranted(context)
        
        // Assert
        assertTrue(result)
        
        // Cleanup
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `isCameraPermissionGranted should return false when permission denied`() {
        // Arrange
        mockkStatic(ContextCompat::class)
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
        } returns PackageManager.PERMISSION_DENIED
        
        // Act
        val result = photoCaptureManager.isCameraPermissionGranted(context)
        
        // Assert
        assertFalse(result)
        
        // Cleanup
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `isCameraAvailable should return true when camera feature available`() {
        // Arrange
        every { 
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) 
        } returns true
        
        // Act
        val result = photoCaptureManager.isCameraAvailable(context)
        
        // Assert
        assertTrue(result)
    }

    @Test
    fun `isCameraAvailable should return false when camera feature not available`() {
        // Arrange
        every { 
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) 
        } returns false
        
        // Act
        val result = photoCaptureManager.isCameraAvailable(context)
        
        // Assert
        assertFalse(result)
    }


    @Test
    fun `cleanupTempFile should delete temp file when it exists`() = runTest {
        // Arrange
        val tempFile = File.createTempFile("temp_photo_", ".jpg", File(testCacheDir, "temp_photos"))
        tempFile.parentFile?.mkdirs()
        tempFile.createNewFile()
        val tempUri = Uri.fromFile(tempFile)
        
        assertTrue("Temp file should exist before cleanup", tempFile.exists())
        
        // Act
        photoCaptureManager.cleanupTempFile(tempUri)
        
        // Assert
        assertFalse("Temp file should be deleted after cleanup", tempFile.exists())
    }

    @Test
    fun `cleanupTempFile should handle non-existent file gracefully`() = runTest {
        // Arrange
        val nonExistentFile = File(testCacheDir, "non_existent.jpg")
        val tempUri = Uri.fromFile(nonExistentFile)
        
        // Act & Assert - Should not throw exception
        photoCaptureManager.cleanupTempFile(tempUri)
    }

    @Test
    fun `cleanupTempFile should handle invalid URI gracefully`() = runTest {
        // Arrange
        val invalidUri = mockk<Uri>()
        every { invalidUri.path } returns null
        
        // Act & Assert - Should not throw exception
        photoCaptureManager.cleanupTempFile(invalidUri)
    }

    @Test
    fun `cleanupTempFile should only delete files in temp photos directory`() = runTest {
        // Arrange
        val tempPhotosDir = File(testCacheDir, "temp_photos")
        tempPhotosDir.mkdirs()
        
        val tempFile = File(tempPhotosDir, "temp_photo.jpg")
        tempFile.createNewFile()
        
        val otherFile = File(testCacheDir, "other_file.jpg")
        otherFile.createNewFile()
        
        val tempUri = Uri.fromFile(tempFile)
        val otherUri = Uri.fromFile(otherFile)
        
        assertTrue("Temp file should exist", tempFile.exists())
        assertTrue("Other file should exist", otherFile.exists())
        
        // Act
        photoCaptureManager.cleanupTempFile(tempUri)
        photoCaptureManager.cleanupTempFile(otherUri)
        
        // Assert
        assertFalse("Temp file should be deleted", tempFile.exists())
        assertTrue("Other file should not be deleted", otherFile.exists())
    }

    @Test
    fun `cleanupOldTempFiles should remove files older than specified age`() = runTest {
        // Arrange
        val tempPhotosDir = File(testCacheDir, "temp_photos")
        tempPhotosDir.mkdirs()
        
        val oldFile = File(tempPhotosDir, "old_temp.jpg")
        val newFile = File(tempPhotosDir, "new_temp.jpg")
        
        oldFile.createNewFile()
        newFile.createNewFile()
        
        // Make old file appear old by setting last modified time
        val oldTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000) // 25 hours ago
        oldFile.setLastModified(oldTime)
        
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        
        assertTrue("Old file should exist before cleanup", oldFile.exists())
        assertTrue("New file should exist before cleanup", newFile.exists())
        
        // Act
        val photoCaptureManagerImpl = photoCaptureManager as PhotoCaptureManagerImpl
        photoCaptureManagerImpl.cleanupOldTempFiles(maxAge)
        
        // Assert
        assertFalse("Old file should be deleted", oldFile.exists())
        assertTrue("New file should remain", newFile.exists())
    }

    @Test
    fun `cleanupOldTempFiles should handle non-existent directory gracefully`() = runTest {
        // Arrange
        val tempPhotosDir = File(testCacheDir, "temp_photos")
        // Don't create the directory
        
        // Act & Assert - Should not throw exception
        val photoCaptureManagerImpl = photoCaptureManager as PhotoCaptureManagerImpl
        photoCaptureManagerImpl.cleanupOldTempFiles()
    }
}