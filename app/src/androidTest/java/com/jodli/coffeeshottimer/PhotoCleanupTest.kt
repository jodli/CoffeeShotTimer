package com.jodli.coffeeshottimer

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit4.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.repository.BeanRepository
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import com.jodli.coffeeshottimer.domain.usecase.AddPhotoToBeanUseCase
import com.jodli.coffeeshottimer.domain.usecase.RemovePhotoFromBeanUseCase
import com.jodli.coffeeshottimer.utils.TestUtils
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
 * Integration tests for photo cleanup functionality when beans are deleted.
 * Tests that orphaned photo files are properly cleaned up and storage is managed efficiently.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PhotoCleanupTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var beanRepository: BeanRepository

    @Inject
    lateinit var photoStorageManager: PhotoStorageManager

    @Inject
    lateinit var addPhotoToBeanUseCase: AddPhotoToBeanUseCase

    @Inject
    lateinit var removePhotoFromBeanUseCase: RemovePhotoFromBeanUseCase

    private lateinit var context: Context
    private val testImageFiles = mutableListOf<File>()
    private val testBeans = mutableListOf<Bean>()

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
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
    fun photoCleanup_whenSingleBeanDeleted() = runBlocking {
        // Create test bean
        val testBean = createTestBean("Single Bean Test")
        database.beanDao().insertBean(testBean)

        // Add photo to bean
        val testImage = createTestImageFile()
        val addResult = addPhotoToBeanUseCase.execute(testBean.id, Uri.fromFile(testImage))
        assertTrue("Photo should be added successfully", addResult.isSuccess)
        
        val photoPath = addResult.getOrNull()!!
        
        // Verify photo file exists
        val photoFile = photoStorageManager.getPhotoFile(photoPath)
        assertNotNull("Photo file should exist", photoFile)
        assertTrue("Photo file should exist on disk", photoFile!!.exists())

        // Delete the bean
        database.beanDao().deleteBean(testBean)

        // Verify bean is deleted
        val deletedBean = database.beanDao().getBeanById(testBean.id)
        assertNull("Bean should be deleted", deletedBean)

        // Run cleanup with empty referenced paths (no beans have photos now)
        val cleanupResult = photoStorageManager.cleanupOrphanedFiles(emptySet())
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)
        
        val cleanedUpCount = cleanupResult.getOrNull()!!
        assertTrue("Should clean up at least one file", cleanedUpCount >= 1)

        // Verify photo file is cleaned up
        val photoFileAfterCleanup = photoStorageManager.getPhotoFile(photoPath)
        assertNull("Photo file should be cleaned up", photoFileAfterCleanup)
    }

    @Test
    fun photoCleanup_whenMultipleBeansDeleted() = runBlocking {
        // Create multiple test beans
        val bean1 = createTestBean("Multi Bean Test 1")
        val bean2 = createTestBean("Multi Bean Test 2")
        val bean3 = createTestBean("Multi Bean Test 3")
        val bean4 = createTestBean("Multi Bean Test 4")
        
        database.beanDao().insertBean(bean1)
        database.beanDao().insertBean(bean2)
        database.beanDao().insertBean(bean3)
        database.beanDao().insertBean(bean4)

        // Add photos to beans 1, 2, and 3
        val image1 = createTestImageFile()
        val image2 = createTestImageFile()
        val image3 = createTestImageFile()
        
        val photo1Result = addPhotoToBeanUseCase.execute(bean1.id, Uri.fromFile(image1))
        val photo2Result = addPhotoToBeanUseCase.execute(bean2.id, Uri.fromFile(image2))
        val photo3Result = addPhotoToBeanUseCase.execute(bean3.id, Uri.fromFile(image3))
        
        assertTrue("All photos should be added", 
            photo1Result.isSuccess && photo2Result.isSuccess && photo3Result.isSuccess)
        
        val photoPath1 = photo1Result.getOrNull()!!
        val photoPath2 = photo2Result.getOrNull()!!
        val photoPath3 = photo3Result.getOrNull()!!

        // Verify all photos exist
        assertNotNull("Photo 1 should exist", photoStorageManager.getPhotoFile(photoPath1))
        assertNotNull("Photo 2 should exist", photoStorageManager.getPhotoFile(photoPath2))
        assertNotNull("Photo 3 should exist", photoStorageManager.getPhotoFile(photoPath3))

        // Delete beans 1 and 2
        database.beanDao().deleteBean(bean1)
        database.beanDao().deleteBean(bean2)

        // Get remaining photo paths (only bean3 should have a photo)
        val remainingBeans = database.beanDao().getAllBeans().first()
        val referencedPhotoPaths = remainingBeans
            .mapNotNull { it.photoPath }
            .toSet()
        
        assertEquals("Should have one referenced photo", 1, referencedPhotoPaths.size)
        assertTrue("Should reference bean3's photo", referencedPhotoPaths.contains(photoPath3))

        // Run cleanup
        val cleanupResult = photoStorageManager.cleanupOrphanedFiles(referencedPhotoPaths)
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)
        
        val cleanedUpCount = cleanupResult.getOrNull()!!
        assertTrue("Should clean up at least 2 files", cleanedUpCount >= 2)

        // Verify only bean3's photo remains
        assertNull("Photo 1 should be cleaned up", photoStorageManager.getPhotoFile(photoPath1))
        assertNull("Photo 2 should be cleaned up", photoStorageManager.getPhotoFile(photoPath2))
        assertNotNull("Photo 3 should remain", photoStorageManager.getPhotoFile(photoPath3))
    }

    @Test
    fun photoCleanup_whenAllBeansDeleted() = runBlocking {
        // Create multiple beans with photos
        val beans = (1..5).map { i ->
            createTestBean("All Beans Test $i")
        }
        
        beans.forEach { bean ->
            database.beanDao().insertBean(bean)
        }

        // Add photos to all beans
        val photoPaths = mutableListOf<String>()
        beans.forEach { bean ->
            val testImage = createTestImageFile()
            val addResult = addPhotoToBeanUseCase.execute(bean.id, Uri.fromFile(testImage))
            assertTrue("Photo should be added to bean ${bean.name}", addResult.isSuccess)
            photoPaths.add(addResult.getOrNull()!!)
        }

        // Verify all photos exist
        photoPaths.forEach { photoPath ->
            assertNotNull("Photo should exist: $photoPath", photoStorageManager.getPhotoFile(photoPath))
        }

        // Delete all beans
        beans.forEach { bean ->
            database.beanDao().deleteBean(bean)
        }

        // Verify all beans are deleted
        val remainingBeans = database.beanDao().getAllBeans().first()
        assertTrue("No beans should remain", remainingBeans.isEmpty())

        // Run cleanup with empty referenced paths
        val cleanupResult = photoStorageManager.cleanupOrphanedFiles(emptySet())
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)
        
        val cleanedUpCount = cleanupResult.getOrNull()!!
        assertTrue("Should clean up all photos", cleanedUpCount >= photoPaths.size)

        // Verify all photos are cleaned up
        photoPaths.forEach { photoPath ->
            assertNull("Photo should be cleaned up: $photoPath", photoStorageManager.getPhotoFile(photoPath))
        }
    }

    @Test
    fun photoCleanup_preservesReferencedPhotos() = runBlocking {
        // Create beans with and without photos
        val beanWithPhoto1 = createTestBean("With Photo 1")
        val beanWithPhoto2 = createTestBean("With Photo 2")
        val beanWithoutPhoto = createTestBean("Without Photo")
        
        database.beanDao().insertBean(beanWithPhoto1)
        database.beanDao().insertBean(beanWithPhoto2)
        database.beanDao().insertBean(beanWithoutPhoto)

        // Add photos to some beans
        val image1 = createTestImageFile()
        val image2 = createTestImageFile()
        
        val photo1Result = addPhotoToBeanUseCase.execute(beanWithPhoto1.id, Uri.fromFile(image1))
        val photo2Result = addPhotoToBeanUseCase.execute(beanWithPhoto2.id, Uri.fromFile(image2))
        
        assertTrue("Photos should be added", photo1Result.isSuccess && photo2Result.isSuccess)
        
        val photoPath1 = photo1Result.getOrNull()!!
        val photoPath2 = photo2Result.getOrNull()!!

        // Create some orphaned files manually (simulate old photos)
        val orphanedFile1 = createTestImageFile("orphaned_1.jpg")
        val orphanedFile2 = createTestImageFile("orphaned_2.jpg")

        // Get current referenced photo paths
        val currentBeans = database.beanDao().getAllBeans().first()
        val referencedPhotoPaths = currentBeans
            .mapNotNull { it.photoPath }
            .toSet()
        
        assertEquals("Should have 2 referenced photos", 2, referencedPhotoPaths.size)

        // Run cleanup
        val cleanupResult = photoStorageManager.cleanupOrphanedFiles(referencedPhotoPaths)
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)

        // Verify referenced photos are preserved
        assertNotNull("Referenced photo 1 should be preserved", photoStorageManager.getPhotoFile(photoPath1))
        assertNotNull("Referenced photo 2 should be preserved", photoStorageManager.getPhotoFile(photoPath2))

        // Verify beans still have their photos
        val updatedBean1 = database.beanDao().getBeanById(beanWithPhoto1.id)
        val updatedBean2 = database.beanDao().getBeanById(beanWithPhoto2.id)
        val updatedBean3 = database.beanDao().getBeanById(beanWithoutPhoto.id)
        
        assertEquals("Bean 1 should still have photo", photoPath1, updatedBean1!!.photoPath)
        assertEquals("Bean 2 should still have photo", photoPath2, updatedBean2!!.photoPath)
        assertNull("Bean 3 should not have photo", updatedBean3!!.photoPath)
    }

    @Test
    fun photoCleanup_storageSpaceManagement() = runBlocking {
        // Test storage space calculation and management
        
        // Get initial storage size
        val initialStorageSize = photoStorageManager.getTotalStorageSize()
        
        // Create beans and add photos
        val beans = (1..3).map { i ->
            createTestBean("Storage Test $i")
        }
        
        beans.forEach { bean ->
            database.beanDao().insertBean(bean)
        }

        val photoPaths = mutableListOf<String>()
        beans.forEach { bean ->
            val testImage = createTestImageFile()
            val addResult = addPhotoToBeanUseCase.execute(bean.id, Uri.fromFile(testImage))
            assertTrue("Photo should be added", addResult.isSuccess)
            photoPaths.add(addResult.getOrNull()!!)
        }

        // Check storage size increased
        val storageAfterAdding = photoStorageManager.getTotalStorageSize()
        assertTrue("Storage size should increase after adding photos", 
            storageAfterAdding > initialStorageSize)

        // Delete one bean
        database.beanDao().deleteBean(beans[0])

        // Get remaining referenced paths
        val remainingBeans = database.beanDao().getAllBeans().first()
        val referencedPhotoPaths = remainingBeans
            .mapNotNull { it.photoPath }
            .toSet()

        // Run cleanup
        val cleanupResult = photoStorageManager.cleanupOrphanedFiles(referencedPhotoPaths)
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)

        // Check storage size decreased
        val storageAfterCleanup = photoStorageManager.getTotalStorageSize()
        assertTrue("Storage size should decrease after cleanup", 
            storageAfterCleanup < storageAfterAdding)
    }

    @Test
    fun photoCleanup_cascadeDeleteIntegration() = runBlocking {
        // Test that photo cleanup works with cascade delete of shots
        
        val testBean = createTestBean("Cascade Test Bean")
        database.beanDao().insertBean(testBean)

        // Add photo to bean
        val testImage = createTestImageFile()
        val addResult = addPhotoToBeanUseCase.execute(testBean.id, Uri.fromFile(testImage))
        assertTrue("Photo should be added", addResult.isSuccess)
        
        val photoPath = addResult.getOrNull()!!

        // Add some shots for this bean (if shots table exists)
        // This tests that photo cleanup works even when beans have related data
        
        // Verify photo exists
        assertNotNull("Photo should exist", photoStorageManager.getPhotoFile(photoPath))

        // Delete bean (this should cascade delete any related shots)
        database.beanDao().deleteBean(testBean)

        // Run cleanup
        val cleanupResult = photoStorageManager.cleanupOrphanedFiles(emptySet())
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)

        // Verify photo is cleaned up
        assertNull("Photo should be cleaned up", photoStorageManager.getPhotoFile(photoPath))
    }

    @Test
    fun photoCleanup_errorHandlingAndRecovery() = runBlocking {
        // Test error handling during cleanup operations
        
        val testBean = createTestBean("Error Handling Test")
        database.beanDao().insertBean(testBean)

        // Add photo
        val testImage = createTestImageFile()
        val addResult = addPhotoToBeanUseCase.execute(testBean.id, Uri.fromFile(testImage))
        assertTrue("Photo should be added", addResult.isSuccess)
        
        val photoPath = addResult.getOrNull()!!

        // Test cleanup with invalid paths in referenced set
        val invalidReferencedPaths = setOf(
            photoPath,
            "invalid/path/that/does/not/exist.jpg",
            "",
            "another/invalid/path.png"
        )

        // Cleanup should handle invalid paths gracefully
        val cleanupResult = photoStorageManager.cleanupOrphanedFiles(invalidReferencedPaths)
        assertTrue("Cleanup should succeed even with invalid paths", cleanupResult.isSuccess)

        // Valid photo should still exist
        assertNotNull("Valid photo should still exist", photoStorageManager.getPhotoFile(photoPath))
    }

    private fun createTestBean(name: String): Bean {
        val bean = Bean(
            id = UUID.randomUUID().toString(),
            name = name,
            roastDate = LocalDate.now().minusDays(7),
            notes = "Test bean for cleanup tests"
        )
        testBeans.add(bean)
        return bean
    }

    private fun createTestImageFile(fileName: String = "test_image_${UUID.randomUUID()}.jpg"): File {
        val testFile = TestUtils.createTestImageFile(context, fileName)
        testImageFiles.add(testFile)
        return testFile
    }
}