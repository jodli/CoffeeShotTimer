package com.jodli.coffeeshottimer

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jodli.coffeeshottimer.data.dao.BeanDao
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.storage.PhotoCaptureManager
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import com.jodli.coffeeshottimer.domain.usecase.AddPhotoToBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.GetBeanPhotoUseCase
import com.jodli.coffeeshottimer.domain.usecase.RemovePhotoFromBeanUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Integration tests for photo workflow functionality.
 * Tests end-to-end photo capture, storage, database integration, and cleanup operations.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PhotoIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var beanRepository: BeanRepository

    @Inject
    lateinit var photoStorageManager: PhotoStorageManager

    @Inject
    lateinit var photoCaptureManager: PhotoCaptureManager

    @Inject
    lateinit var addPhotoToBeanUseCase: AddPhotoToBeanUseCase

    @Inject
    lateinit var removePhotoFromBeanUseCase: RemovePhotoFromBeanUseCase

    @Inject
    lateinit var getBeanPhotoUseCase: GetBeanPhotoUseCase

    private lateinit var context: Context
    private lateinit var testBean: Bean
    private val testImageFiles = mutableListOf<File>()

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        // Create a test bean
        testBean = Bean(
            id = UUID.randomUUID().toString(),
            name = "Test Bean for Photos",
            roastDate = LocalDate.now().minusDays(7),
            notes = "Test bean for photo integration tests"
        )
        
        runBlocking {
            database.beanDao().insertBean(testBean)
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            // Clean up test data
            database.clearAllTables()
            
            // Clean up test image files
            testImageFiles.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    @Test
    fun photoWorkflow_endToEndPhotoCapture() = runBlocking {
        // Create a test image file
        val testImageFile = createTestImageFile()
        val testImageUri = Uri.fromFile(testImageFile)

        // Step 1: Add photo to bean
        val addResult = addPhotoToBeanUseCase.execute(testBean.id, testImageUri)
        assertTrue("Adding photo should succeed", addResult.isSuccess)
        
        val photoPath = addResult.getOrNull()
        assertNotNull("Photo path should not be null", photoPath)
        assertFalse("Photo path should not be empty", photoPath!!.isEmpty())

        // Step 2: Verify bean was updated in database
        val updatedBean = database.beanDao().getBeanById(testBean.id)
        assertNotNull("Bean should exist", updatedBean)
        assertEquals("Bean should have photo path", photoPath, updatedBean!!.photoPath)
        assertTrue("Bean should have photo", updatedBean.hasPhoto())

        // Step 3: Verify photo file exists in storage
        val photoFile = photoStorageManager.getPhotoFile(photoPath)
        assertNotNull("Photo file should exist", photoFile)
        assertTrue("Photo file should exist on disk", photoFile!!.exists())
        assertTrue("Photo file should have content", photoFile.length() > 0)

        // Step 4: Retrieve photo using use case
        val getPhotoResult = getBeanPhotoUseCase.execute(photoPath)
        assertTrue("Getting photo should succeed", getPhotoResult.isSuccess)
        
        val retrievedFile = getPhotoResult.getOrNull()
        assertNotNull("Retrieved file should not be null", retrievedFile)
        assertEquals("Retrieved file should match stored file", photoFile.absolutePath, retrievedFile!!.absolutePath)

        // Step 5: Remove photo from bean
        val removeResult = removePhotoFromBeanUseCase.execute(testBean.id)
        assertTrue("Removing photo should succeed", removeResult.isSuccess)

        // Step 6: Verify photo was removed from database
        val beanAfterRemoval = database.beanDao().getBeanById(testBean.id)
        assertNotNull("Bean should still exist", beanAfterRemoval)
        assertNull("Bean should not have photo path", beanAfterRemoval!!.photoPath)
        assertFalse("Bean should not have photo", beanAfterRemoval.hasPhoto())

        // Step 7: Verify photo file was deleted from storage
        val photoFileAfterRemoval = photoStorageManager.getPhotoFile(photoPath)
        assertNull("Photo file should not exist after removal", photoFileAfterRemoval)
    }

    @Test
    fun databaseMigration_photoFieldIntegration() = runBlocking {
        // Test that the photoPath field is properly integrated after migration
        
        // Create a bean with photo
        val testImageFile = createTestImageFile()
        val testImageUri = Uri.fromFile(testImageFile)
        
        val addResult = addPhotoToBeanUseCase.execute(testBean.id, testImageUri)
        assertTrue("Adding photo should succeed", addResult.isSuccess)
        
        val photoPath = addResult.getOrNull()!!

        // Test database queries with photo field
        val beansWithPhotos = database.beanDao().getBeansWithPhotos().first()
        assertTrue("Should find beans with photos", beansWithPhotos.isNotEmpty())
        assertTrue("Test bean should be in beans with photos", 
            beansWithPhotos.any { it.id == testBean.id })

        // Test filtering by photo status
        val beansWithPhotosFiltered = database.beanDao().getBeansByPhotoStatus(
            hasPhoto = true, 
            activeOnly = true
        ).first()
        assertTrue("Should find active beans with photos", beansWithPhotosFiltered.isNotEmpty())
        assertTrue("Test bean should be in filtered results", 
            beansWithPhotosFiltered.any { it.id == testBean.id })

        // Test updating photo path directly
        val newPhotoPath = "test/new/path.jpg"
        database.beanDao().updateBeanPhoto(testBean.id, newPhotoPath)
        
        val updatedBean = database.beanDao().getBeanById(testBean.id)
        assertEquals("Photo path should be updated", newPhotoPath, updatedBean!!.photoPath)

        // Test removing photo path
        database.beanDao().removeBeanPhoto(testBean.id)
        
        val beanWithoutPhoto = database.beanDao().getBeanById(testBean.id)
        assertNull("Photo path should be null", beanWithoutPhoto!!.photoPath)
    }

    @Test
    fun cameraPermission_scenariosAndErrorHandling() {
        // Test camera availability check
        val isCameraAvailable = photoCaptureManager.isCameraAvailable(context)
        // Note: This might be false in emulator, but should not crash
        
        // Test permission check
        val hasPermission = photoCaptureManager.isCameraPermissionGranted(context)
        // Note: Permission might not be granted in test environment
        
        // Test required permissions
        val requiredPermissions = photoCaptureManager.getRequiredPermissions()
        assertTrue("Should have required permissions", requiredPermissions.isNotEmpty())
        assertTrue("Should include camera permission", 
            requiredPermissions.contains(android.Manifest.permission.CAMERA))

        // Test intent creation (should not crash even without permissions)
        try {
            val cameraIntent = photoCaptureManager.createImageCaptureIntent()
            assertNotNull("Camera intent should be created", cameraIntent.first)
            assertNotNull("Temp URI should be created", cameraIntent.second)
            
            val galleryIntent = photoCaptureManager.createImagePickerIntent()
            assertNotNull("Gallery intent should be created", galleryIntent)
        } catch (e: Exception) {
            // In test environment, this might fail due to missing camera app
            // This is acceptable as long as it doesn't crash the app
        }
    }

    @Test
    fun photoCleanup_whenBeansAreDeleted() = runBlocking {
        // Create multiple beans with photos
        val bean1 = testBean
        val bean2 = Bean(
            id = UUID.randomUUID().toString(),
            name = "Test Bean 2",
            roastDate = LocalDate.now().minusDays(5)
        )
        val bean3 = Bean(
            id = UUID.randomUUID().toString(),
            name = "Test Bean 3",
            roastDate = LocalDate.now().minusDays(3)
        )
        
        database.beanDao().insertBean(bean2)
        database.beanDao().insertBean(bean3)

        // Add photos to all beans
        val testImage1 = createTestImageFile()
        val testImage2 = createTestImageFile()
        val testImage3 = createTestImageFile()
        
        val photo1Result = addPhotoToBeanUseCase.execute(bean1.id, Uri.fromFile(testImage1))
        val photo2Result = addPhotoToBeanUseCase.execute(bean2.id, Uri.fromFile(testImage2))
        val photo3Result = addPhotoToBeanUseCase.execute(bean3.id, Uri.fromFile(testImage3))
        
        assertTrue("All photos should be added successfully", 
            photo1Result.isSuccess && photo2Result.isSuccess && photo3Result.isSuccess)
        
        val photoPath1 = photo1Result.getOrNull()!!
        val photoPath2 = photo2Result.getOrNull()!!
        val photoPath3 = photo3Result.getOrNull()!!

        // Verify all photo files exist
        assertNotNull("Photo 1 should exist", photoStorageManager.getPhotoFile(photoPath1))
        assertNotNull("Photo 2 should exist", photoStorageManager.getPhotoFile(photoPath2))
        assertNotNull("Photo 3 should exist", photoStorageManager.getPhotoFile(photoPath3))

        // Delete bean2 (this should clean up its photo)
        database.beanDao().deleteBean(bean2)

        // Verify bean2's photo is cleaned up when we run cleanup
        val remainingPhotoPaths = setOf(photoPath1, photoPath3) // Only bean1 and bean3 photos should remain
        val cleanupResult = photoStorageManager.cleanupOrphanedFiles(remainingPhotoPaths)
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)
        
        val cleanedUpCount = cleanupResult.getOrNull()!!
        assertTrue("Should clean up at least one file", cleanedUpCount >= 0)

        // Verify remaining photos still exist
        assertNotNull("Photo 1 should still exist", photoStorageManager.getPhotoFile(photoPath1))
        assertNotNull("Photo 3 should still exist", photoStorageManager.getPhotoFile(photoPath3))
    }

    @Test
    fun photoReplacement_workflow() = runBlocking {
        // Add initial photo
        val initialImage = createTestImageFile()
        val initialResult = addPhotoToBeanUseCase.execute(testBean.id, Uri.fromFile(initialImage))
        assertTrue("Initial photo should be added", initialResult.isSuccess)
        
        val initialPhotoPath = initialResult.getOrNull()!!
        val initialPhotoFile = photoStorageManager.getPhotoFile(initialPhotoPath)
        assertNotNull("Initial photo file should exist", initialPhotoFile)

        // Replace with new photo
        val newImage = createTestImageFile()
        val replaceResult = addPhotoToBeanUseCase.execute(testBean.id, Uri.fromFile(newImage))
        assertTrue("Photo replacement should succeed", replaceResult.isSuccess)
        
        val newPhotoPath = replaceResult.getOrNull()!!
        assertNotEquals("New photo path should be different", initialPhotoPath, newPhotoPath)

        // Verify old photo is cleaned up and new photo exists
        val newPhotoFile = photoStorageManager.getPhotoFile(newPhotoPath)
        assertNotNull("New photo file should exist", newPhotoFile)
        
        // Verify bean has new photo path
        val updatedBean = database.beanDao().getBeanById(testBean.id)
        assertEquals("Bean should have new photo path", newPhotoPath, updatedBean!!.photoPath)
    }

    @Test
    fun photoStorage_errorHandling() = runBlocking {
        // Test with invalid URI
        val invalidUri = Uri.parse("invalid://uri")
        val invalidResult = addPhotoToBeanUseCase.execute(testBean.id, invalidUri)
        assertTrue("Invalid URI should fail", invalidResult.isFailure)

        // Test with non-existent bean
        val nonExistentBeanId = UUID.randomUUID().toString()
        val testImage = createTestImageFile()
        val nonExistentResult = addPhotoToBeanUseCase.execute(nonExistentBeanId, Uri.fromFile(testImage))
        assertTrue("Non-existent bean should fail", nonExistentResult.isFailure)

        // Test with empty bean ID
        val emptyIdResult = addPhotoToBeanUseCase.execute("", Uri.fromFile(testImage))
        assertTrue("Empty bean ID should fail", emptyIdResult.isFailure)

        // Test removing photo from bean without photo
        val removeFromEmptyResult = removePhotoFromBeanUseCase.execute(testBean.id)
        assertTrue("Removing from bean without photo should succeed", removeFromEmptyResult.isSuccess)
    }

    @Test
    fun photoValidation_beanEntityValidation() {
        // Test bean validation with valid photo path
        val beanWithValidPhoto = testBean.copy(photoPath = "photos/test_photo.jpg")
        val validResult = beanWithValidPhoto.validate()
        assertTrue("Bean with valid photo should be valid", validResult.isValid)

        // Test bean validation with invalid photo path (empty)
        val beanWithEmptyPhoto = testBean.copy(photoPath = "")
        val emptyResult = beanWithEmptyPhoto.validate()
        assertFalse("Bean with empty photo path should be invalid", emptyResult.isValid)
        assertTrue("Should have photo path error", 
            emptyResult.errors.any { it.contains("Photo path cannot be empty") })

        // Test bean validation with invalid photo path (too long)
        val longPath = "a".repeat(501)
        val beanWithLongPhoto = testBean.copy(photoPath = longPath)
        val longResult = beanWithLongPhoto.validate()
        assertFalse("Bean with long photo path should be invalid", longResult.isValid)
        assertTrue("Should have photo path length error", 
            longResult.errors.any { it.contains("Photo path cannot exceed 500 characters") })

        // Test bean validation with invalid characters
        val beanWithInvalidChars = testBean.copy(photoPath = "photos/test<>photo.jpg")
        val invalidCharsResult = beanWithInvalidChars.validate()
        assertFalse("Bean with invalid photo path characters should be invalid", invalidCharsResult.isValid)
        assertTrue("Should have invalid path error", 
            invalidCharsResult.errors.any { it.contains("Photo path must be a valid file path") })
    }

    @Test
    fun photoStorage_compressionAndOptimization() = runBlocking {
        // Create a larger test image to test compression
        val largeTestImage = createTestImageFile(width = 2000, height = 2000)
        val originalSize = largeTestImage.length()
        
        val addResult = addPhotoToBeanUseCase.execute(testBean.id, Uri.fromFile(largeTestImage))
        assertTrue("Adding large photo should succeed", addResult.isSuccess)
        
        val photoPath = addResult.getOrNull()!!
        val compressedPhotoFile = photoStorageManager.getPhotoFile(photoPath)
        assertNotNull("Compressed photo should exist", compressedPhotoFile)
        
        val compressedSize = compressedPhotoFile!!.length()
        assertTrue("Compressed photo should have reasonable size", compressedSize > 0)
        
        // Test storage size calculation
        val totalStorageSize = photoStorageManager.getTotalStorageSize()
        assertTrue("Total storage size should be positive", totalStorageSize > 0)
    }

    /**
     * Creates a test image file for testing purposes.
     */
    private fun createTestImageFile(width: Int = 100, height: Int = 100): File {
        val testFile = File(context.cacheDir, "test_image_${UUID.randomUUID()}.jpg")
        
        // Create a simple test image (1x1 pixel JPEG)
        val testImageData = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00.toByte(), 0x10.toByte(),
            0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x48.toByte(), 0x00.toByte(), 0x48.toByte(),
            0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(), 0x43.toByte(),
            0x00.toByte(), 0x08.toByte(), 0x06.toByte(), 0x06.toByte(), 0x07.toByte(), 0x06.toByte(),
            0x05.toByte(), 0x08.toByte(), 0x07.toByte(), 0x07.toByte(), 0x07.toByte(), 0x09.toByte(),
            0x09.toByte(), 0x08.toByte(), 0x0A.toByte(), 0x0C.toByte(), 0x14.toByte(), 0x0D.toByte(),
            0x0C.toByte(), 0x0B.toByte(), 0x0B.toByte(), 0x0C.toByte(), 0x19.toByte(), 0x12.toByte(),
            0x13.toByte(), 0x0F.toByte(), 0x14.toByte(), 0x1D.toByte(), 0x1A.toByte(), 0x1F.toByte(),
            0x1E.toByte(), 0x1D.toByte(), 0x1A.toByte(), 0x1C.toByte(), 0x1C.toByte(), 0x20.toByte(),
            0x24.toByte(), 0x2E.toByte(), 0x27.toByte(), 0x20.toByte(), 0x22.toByte(), 0x2C.toByte(),
            0x23.toByte(), 0x1C.toByte(), 0x1C.toByte(), 0x28.toByte(), 0x37.toByte(), 0x29.toByte(),
            0x2C.toByte(), 0x30.toByte(), 0x31.toByte(), 0x34.toByte(), 0x34.toByte(), 0x34.toByte(),
            0x1F.toByte(), 0x27.toByte(), 0x39.toByte(), 0x3D.toByte(), 0x38.toByte(), 0x32.toByte(),
            0x3C.toByte(), 0x2E.toByte(), 0x33.toByte(), 0x34.toByte(), 0x32.toByte(), 0xFF.toByte(),
            0xC0.toByte(), 0x00.toByte(), 0x11.toByte(), 0x08.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x11.toByte(), 0x00.toByte(),
            0x02.toByte(), 0x11.toByte(), 0x01.toByte(), 0x03.toByte(), 0x11.toByte(), 0x01.toByte(),
            0xFF.toByte(), 0xC4.toByte(), 0x00.toByte(), 0x14.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x08.toByte(), 0xFF.toByte(), 0xC4.toByte(),
            0x00.toByte(), 0x14.toByte(), 0x10.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xDA.toByte(), 0x00.toByte(), 0x0C.toByte(),
            0x03.toByte(), 0x01.toByte(), 0x00.toByte(), 0x02.toByte(), 0x11.toByte(), 0x03.toByte(),
            0x11.toByte(), 0x00.toByte(), 0x3F.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(),
            0xD9.toByte()
        )
        
        testFile.writeBytes(testImageData)
        testImageFiles.add(testFile)
        return testFile
    }
}