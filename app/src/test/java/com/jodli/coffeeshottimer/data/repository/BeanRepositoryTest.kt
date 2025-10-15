package com.jodli.coffeeshottimer.data.repository

import android.content.SharedPreferences
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.storage.PhotoStorageManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.LocalDate

/**
 * Integration tests for BeanRepository.
 * Tests repository operations with real database interactions.
 */
@RunWith(RobolectricTestRunner::class)
class BeanRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BeanRepository
    private lateinit var mockPhotoStorageManager: PhotoStorageManager
    private lateinit var mockSharedPreferences: SharedPreferences

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        mockPhotoStorageManager = mockk()
        mockSharedPreferences = mockk(relaxed = true)
        repository = BeanRepository(database.beanDao(), mockPhotoStorageManager, mockSharedPreferences)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun addBean_validBean_succeeds() = runTest {
        // Given
        val bean = createTestBean("Test Bean")

        // When
        val result = repository.addBean(bean)

        // Then
        assertTrue("Adding valid bean should succeed", result.isSuccess)

        val retrievedBean = repository.getBeanById(bean.id).getOrNull()
        assertNotNull("Bean should be retrievable after adding", retrievedBean)
        assertEquals("Bean name should match", bean.name, retrievedBean?.name)
    }

    @Test
    fun addBean_invalidBean_fails() = runTest {
        // Given
        val invalidBean = createTestBean("") // Empty name

        // When
        val result = repository.addBean(invalidBean)

        // Then
        assertTrue("Adding invalid bean should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }

    @Test
    fun addBean_duplicateName_fails() = runTest {
        // Given
        val bean1 = createTestBean("Duplicate Name")
        val bean2 = createTestBean("Duplicate Name")

        repository.addBean(bean1)

        // When
        val result = repository.addBean(bean2)

        // Then
        assertTrue("Adding bean with duplicate name should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }

    @Test
    fun updateBean_validBean_succeeds() = runTest {
        // Given
        val originalBean = createTestBean("Original Bean")
        repository.addBean(originalBean)

        val updatedBean = originalBean.copy(name = "Updated Bean", notes = "Updated notes")

        // When
        val result = repository.updateBean(updatedBean)

        // Then
        assertTrue("Updating valid bean should succeed", result.isSuccess)

        val retrievedBean = repository.getBeanById(originalBean.id).getOrNull()
        assertEquals("Bean name should be updated", "Updated Bean", retrievedBean?.name)
        assertEquals("Bean notes should be updated", "Updated notes", retrievedBean?.notes)
    }

    @Test
    fun updateBean_nonExistentBean_fails() = runTest {
        // Given
        val nonExistentBean = createTestBean("Non-existent Bean")

        // When
        val result = repository.updateBean(nonExistentBean)

        // Then
        assertTrue("Updating non-existent bean should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }

    @Test
    fun deleteBean_existingBean_succeeds() = runTest {
        // Given
        val bean = createTestBean("Bean to Delete")
        repository.addBean(bean)

        // When
        val result = repository.deleteBean(bean)

        // Then
        assertTrue("Deleting existing bean should succeed", result.isSuccess)

        val retrievedBean = repository.getBeanById(bean.id).getOrNull()
        assertNull("Bean should not be retrievable after deletion", retrievedBean)
    }

    @Test
    fun deleteBean_nonExistentBean_fails() = runTest {
        // Given
        val nonExistentBean = createTestBean("Non-existent Bean")

        // When
        val result = repository.deleteBean(nonExistentBean)

        // Then
        assertTrue("Deleting non-existent bean should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }

    @Test
    fun getBeanById_existingBean_returnsBean() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        repository.addBean(bean)

        // When
        val result = repository.getBeanById(bean.id)

        // Then
        assertTrue("Getting existing bean should succeed", result.isSuccess)
        assertEquals("Should return correct bean", bean.name, result.getOrNull()?.name)
    }

    @Test
    fun getBeanById_nonExistentBean_returnsNull() = runTest {
        // When
        val result = repository.getBeanById("non-existent-id")

        // Then
        assertTrue("Getting non-existent bean should succeed but return null", result.isSuccess)
        assertNull("Should return null for non-existent bean", result.getOrNull())
    }

    @Test
    fun getBeanById_emptyId_fails() = runTest {
        // When
        val result = repository.getBeanById("")

        // Then
        assertTrue("Getting bean with empty ID should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }

    @Test
    fun getBeanByName_existingBean_returnsBean() = runTest {
        // Given
        val bean = createTestBean("Unique Bean Name")
        repository.addBean(bean)

        // When
        val result = repository.getBeanByName("Unique Bean Name")

        // Then
        assertTrue("Getting existing bean by name should succeed", result.isSuccess)
        assertEquals("Should return correct bean", bean.id, result.getOrNull()?.id)
    }

    @Test
    fun getBeanByName_nonExistentBean_returnsNull() = runTest {
        // When
        val result = repository.getBeanByName("Non-existent Bean")

        // Then
        assertTrue("Getting non-existent bean by name should succeed but return null", result.isSuccess)
        assertNull("Should return null for non-existent bean", result.getOrNull())
    }

    @Test
    fun getAllBeans_returnsAllBeans() = runTest {
        // Given
        val bean1 = createTestBean("Bean 1")
        val bean2 = createTestBean("Bean 2")
        repository.addBean(bean1)
        repository.addBean(bean2)

        // When
        val result = repository.getAllBeans().first()

        // Then
        assertTrue("Getting all beans should succeed", result.isSuccess)
        val beans = result.getOrNull()
        assertEquals("Should return all beans", 2, beans?.size)
        assertTrue("Should contain bean 1", beans?.any { it.name == "Bean 1" } == true)
        assertTrue("Should contain bean 2", beans?.any { it.name == "Bean 2" } == true)
    }

    @Test
    fun getActiveBeans_returnsOnlyActiveBeans() = runTest {
        // Given
        val activeBean = createTestBean("Active Bean", isActive = true)
        val inactiveBean = createTestBean("Inactive Bean", isActive = false)
        repository.addBean(activeBean)
        repository.addBean(inactiveBean)

        // When
        val result = repository.getActiveBeans().first()

        // Then
        assertTrue("Getting active beans should succeed", result.isSuccess)
        val beans = result.getOrNull()
        assertEquals("Should return only active beans", 1, beans?.size)
        assertEquals("Should return the active bean", "Active Bean", beans?.first()?.name)
    }

    @Test
    fun updateLastGrinderSetting_validData_succeeds() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        repository.addBean(bean)

        // When
        val result = repository.updateLastGrinderSetting(bean.id, "15.5")

        // Then
        assertTrue("Updating grinder setting should succeed", result.isSuccess)

        val updatedBean = repository.getBeanById(bean.id).getOrNull()
        assertEquals("Grinder setting should be updated", "15.5", updatedBean?.lastGrinderSetting)
    }

    @Test
    fun updateLastGrinderSetting_nonExistentBean_fails() = runTest {
        // When
        val result = repository.updateLastGrinderSetting("non-existent-id", "15.5")

        // Then
        assertTrue("Updating grinder setting for non-existent bean should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }

    @Test
    fun updateBeanActiveStatus_validData_succeeds() = runTest {
        // Given
        val bean = createTestBean("Test Bean", isActive = true)
        repository.addBean(bean)

        // When
        val result = repository.updateBeanActiveStatus(bean.id, false)

        // Then
        assertTrue("Updating bean active status should succeed", result.isSuccess)

        val updatedBean = repository.getBeanById(bean.id).getOrNull()
        assertFalse("Bean should be inactive", updatedBean?.isActive ?: true)
    }

    @Test
    fun getFilteredBeans_filtersCorrectly() = runTest {
        // Given
        val activeBean1 = createTestBean("Active Coffee Bean", isActive = true)
        val activeBean2 = createTestBean("Active Espresso Bean", isActive = true)
        val inactiveBean = createTestBean("Inactive Coffee Bean", isActive = false)

        repository.addBean(activeBean1)
        repository.addBean(activeBean2)
        repository.addBean(inactiveBean)

        // When - filter active beans containing "Coffee"
        val result = repository.getFilteredBeans(activeOnly = true, searchQuery = "Coffee").first()

        // Then
        assertTrue("Getting filtered beans should succeed", result.isSuccess)
        val beans = result.getOrNull()
        assertEquals("Should return only active beans containing 'Coffee'", 1, beans?.size)
        assertEquals("Should return the active coffee bean", "Active Coffee Bean", beans?.first()?.name)
    }

    @Test
    fun getActiveBeanCount_returnsCorrectCount() = runTest {
        // Given
        val activeBean1 = createTestBean("Active Bean 1", isActive = true)
        val activeBean2 = createTestBean("Active Bean 2", isActive = true)
        val inactiveBean = createTestBean("Inactive Bean", isActive = false)

        repository.addBean(activeBean1)
        repository.addBean(activeBean2)
        repository.addBean(inactiveBean)

        // When
        val result = repository.getActiveBeanCount()

        // Then
        assertTrue("Getting active bean count should succeed", result.isSuccess)
        assertEquals("Should return correct count of active beans", 2, result.getOrNull())
    }

    @Test
    fun validateBean_validBean_returnsValid() = runTest {
        // Given
        val validBean = createTestBean("Valid Bean")

        // When
        val validationResult = repository.validateBean(validBean)

        // Then
        assertTrue("Valid bean should pass validation", validationResult.isValid)
        assertTrue("Valid bean should have no errors", validationResult.errors.isEmpty())
    }

    @Test
    fun validateBean_invalidBean_returnsInvalid() = runTest {
        // Given
        val invalidBean = createTestBean("") // Empty name

        // When
        val validationResult = repository.validateBean(invalidBean)

        // Then
        assertFalse("Invalid bean should fail validation", validationResult.isValid)
        assertFalse("Invalid bean should have errors", validationResult.errors.isEmpty())
    }

    @Test
    fun validateBean_duplicateName_returnsInvalid() = runTest {
        // Given
        val existingBean = createTestBean("Existing Bean")
        repository.addBean(existingBean)

        val duplicateBean = createTestBean("Existing Bean")

        // When
        val validationResult = repository.validateBean(duplicateBean)

        // Then
        assertFalse("Bean with duplicate name should fail validation", validationResult.isValid)
        assertTrue(
            "Should have uniqueness error",
            validationResult.errors.any { it.contains("already exists") }
        )
    }

    // Photo-related tests

    @Test
    fun addPhotoToBean_validData_succeeds() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        repository.addBean(bean)

        val mockUri = mockk<Uri>()
        val expectedPhotoPath = "/path/to/photo.jpg"

        coEvery { mockPhotoStorageManager.savePhoto(mockUri, bean.id) } returns Result.success(expectedPhotoPath)

        // When
        val result = repository.addPhotoToBean(bean.id, mockUri)

        // Then
        assertTrue("Adding photo to bean should succeed", result.isSuccess)
        assertEquals("Should return correct photo path", expectedPhotoPath, result.getOrNull())

        coVerify { mockPhotoStorageManager.savePhoto(mockUri, bean.id) }

        // Verify bean was updated with photo path
        val updatedBean = repository.getBeanById(bean.id).getOrNull()
        assertEquals("Bean should have photo path", expectedPhotoPath, updatedBean?.photoPath)
    }

    @Test
    fun addPhotoToBean_replacesExistingPhoto() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        repository.addBean(bean)

        val oldPhotoPath = "/path/to/old_photo.jpg"
        val newPhotoPath = "/path/to/new_photo.jpg"

        // Add initial photo
        coEvery { mockPhotoStorageManager.savePhoto(any(), bean.id) } returns Result.success(oldPhotoPath)
        repository.addPhotoToBean(bean.id, mockk())

        // Setup for replacement
        val mockNewUri = mockk<Uri>()
        coEvery { mockPhotoStorageManager.deletePhoto(oldPhotoPath) } returns Result.success(Unit)
        coEvery { mockPhotoStorageManager.savePhoto(mockNewUri, bean.id) } returns Result.success(newPhotoPath)

        // When
        val result = repository.addPhotoToBean(bean.id, mockNewUri)

        // Then
        assertTrue("Replacing photo should succeed", result.isSuccess)
        assertEquals("Should return new photo path", newPhotoPath, result.getOrNull())

        coVerify { mockPhotoStorageManager.deletePhoto(oldPhotoPath) }
        coVerify { mockPhotoStorageManager.savePhoto(mockNewUri, bean.id) }
    }

    @Test
    fun addPhotoToBean_nonExistentBean_fails() = runTest {
        // Given
        val nonExistentBeanId = "non-existent-id"
        val mockUri = mockk<Uri>()

        // When
        val result = repository.addPhotoToBean(nonExistentBeanId, mockUri)

        // Then
        assertTrue("Adding photo to non-existent bean should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }

    @Test
    fun addPhotoToBean_emptyBeanId_fails() = runTest {
        // Given
        val mockUri = mockk<Uri>()

        // When
        val result = repository.addPhotoToBean("", mockUri)

        // Then
        assertTrue("Adding photo with empty bean ID should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }

    @Test
    fun addPhotoToBean_storageFailure_fails() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        repository.addBean(bean)

        val mockUri = mockk<Uri>()
        val storageException = Exception("Storage failed")

        coEvery { mockPhotoStorageManager.savePhoto(mockUri, bean.id) } returns Result.failure(storageException)

        // When
        val result = repository.addPhotoToBean(bean.id, mockUri)

        // Then
        assertTrue("Adding photo with storage failure should fail", result.isFailure)
        assertTrue("Should be database error", result.exceptionOrNull() is RepositoryException.DatabaseError)
    }

    @Test
    fun removePhotoFromBean_existingPhoto_succeeds() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        repository.addBean(bean)

        val photoPath = "/path/to/photo.jpg"

        // Add photo first
        coEvery { mockPhotoStorageManager.savePhoto(any(), bean.id) } returns Result.success(photoPath)
        repository.addPhotoToBean(bean.id, mockk())

        // Setup for removal
        coEvery { mockPhotoStorageManager.deletePhoto(photoPath) } returns Result.success(Unit)

        // When
        val result = repository.removePhotoFromBean(bean.id)

        // Then
        assertTrue("Removing photo should succeed", result.isSuccess)

        coVerify { mockPhotoStorageManager.deletePhoto(photoPath) }

        // Verify bean photo path was removed
        val updatedBean = repository.getBeanById(bean.id).getOrNull()
        assertNull("Bean should not have photo path", updatedBean?.photoPath)
    }

    @Test
    fun removePhotoFromBean_nonExistentBean_fails() = runTest {
        // Given
        val nonExistentBeanId = "non-existent-id"

        // When
        val result = repository.removePhotoFromBean(nonExistentBeanId)

        // Then
        assertTrue("Removing photo from non-existent bean should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }

    @Test
    fun removePhotoFromBean_emptyBeanId_fails() = runTest {
        // When
        val result = repository.removePhotoFromBean("")

        // Then
        assertTrue("Removing photo with empty bean ID should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }

    @Test
    fun getBeanPhoto_existingPhoto_returnsFile() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        repository.addBean(bean)

        val photoPath = "/path/to/photo.jpg"
        val mockFile = mockk<File>()

        // Add photo first
        coEvery { mockPhotoStorageManager.savePhoto(any(), bean.id) } returns Result.success(photoPath)
        repository.addPhotoToBean(bean.id, mockk())

        coEvery { mockPhotoStorageManager.getPhotoFile(photoPath) } returns mockFile

        // When
        val result = repository.getBeanPhoto(bean.id)

        // Then
        assertTrue("Getting bean photo should succeed", result.isSuccess)
        assertEquals("Should return correct file", mockFile, result.getOrNull())

        coVerify { mockPhotoStorageManager.getPhotoFile(photoPath) }
    }

    @Test
    fun getBeanPhoto_noPhoto_returnsNull() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        repository.addBean(bean)

        // When
        val result = repository.getBeanPhoto(bean.id)

        // Then
        assertTrue("Getting bean photo should succeed", result.isSuccess)
        assertNull("Should return null when no photo", result.getOrNull())
    }

    @Test
    fun getPhotoFile_validPath_returnsFile() = runTest {
        // Given
        val photoPath = "/path/to/photo.jpg"
        val mockFile = mockk<File>()

        coEvery { mockPhotoStorageManager.getPhotoFile(photoPath) } returns mockFile

        // When
        val result = repository.getPhotoFile(photoPath)

        // Then
        assertTrue("Getting photo file should succeed", result.isSuccess)
        assertEquals("Should return correct file", mockFile, result.getOrNull())

        coVerify { mockPhotoStorageManager.getPhotoFile(photoPath) }
    }

    @Test
    fun getPhotoFile_emptyPath_fails() = runTest {
        // When
        val result = repository.getPhotoFile("")

        // Then
        assertTrue("Getting photo file with empty path should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }

    @Test
    fun getBeansWithPhotos_returnsCorrectBeans() = runTest {
        // Given
        val beanWithPhoto = createTestBean("Bean With Photo")
        val beanWithoutPhoto = createTestBean("Bean Without Photo")

        repository.addBean(beanWithPhoto)
        repository.addBean(beanWithoutPhoto)

        // Add photo to first bean
        coEvery { mockPhotoStorageManager.savePhoto(any(), beanWithPhoto.id) } returns Result.success("/path/to/photo.jpg")
        repository.addPhotoToBean(beanWithPhoto.id, mockk())

        // When
        val result = repository.getBeansWithPhotos().first()

        // Then
        assertTrue("Getting beans with photos should succeed", result.isSuccess)
        val beans = result.getOrNull()
        assertEquals("Should return only beans with photos", 1, beans?.size)
        assertEquals("Should return the bean with photo", "Bean With Photo", beans?.first()?.name)
    }

    @Test
    fun getBeansByPhotoStatus_filtersCorrectly() = runTest {
        // Given
        val activeBeanWithPhoto = createTestBean("Active Bean With Photo", isActive = true)
        val activeBeanWithoutPhoto = createTestBean("Active Bean Without Photo", isActive = true)
        val inactiveBeanWithPhoto = createTestBean("Inactive Bean With Photo", isActive = false)

        repository.addBean(activeBeanWithPhoto)
        repository.addBean(activeBeanWithoutPhoto)
        repository.addBean(inactiveBeanWithPhoto)

        // Add photos to beans with photos
        coEvery {
            mockPhotoStorageManager.savePhoto(any(), activeBeanWithPhoto.id)
        } returns Result.success("/path/to/photo1.jpg")
        coEvery {
            mockPhotoStorageManager.savePhoto(any(), inactiveBeanWithPhoto.id)
        } returns Result.success("/path/to/photo2.jpg")
        repository.addPhotoToBean(activeBeanWithPhoto.id, mockk())
        repository.addPhotoToBean(inactiveBeanWithPhoto.id, mockk())

        // When - filter for active beans with photos
        val result = repository.getBeansByPhotoStatus(hasPhoto = true, activeOnly = true).first()

        // Then
        assertTrue("Getting beans by photo status should succeed", result.isSuccess)
        val beans = result.getOrNull()
        assertEquals("Should return only active beans with photos", 1, beans?.size)
        assertEquals("Should return the active bean with photo", "Active Bean With Photo", beans?.first()?.name)
    }

    @Test
    fun cleanupOrphanedPhotos_removesUnreferencedFiles() = runTest {
        // Given
        val referencedPaths = setOf("/path/to/photo1.jpg", "/path/to/photo2.jpg")
        val cleanedUpCount = 3

        coEvery { mockPhotoStorageManager.cleanupOrphanedFiles(referencedPaths) } returns Result.success(cleanedUpCount)

        // When
        val result = repository.cleanupOrphanedPhotos()

        // Then
        assertTrue("Cleanup should succeed", result.isSuccess)
        assertEquals("Should return correct cleanup count", cleanedUpCount, result.getOrNull())

        coVerify { mockPhotoStorageManager.cleanupOrphanedFiles(any()) }
    }

    @Test
    fun getPhotoStorageSize_returnsCorrectSize() = runTest {
        // Given
        val expectedSize = 1024L * 1024L // 1MB

        coEvery { mockPhotoStorageManager.getTotalStorageSize() } returns expectedSize

        // When
        val result = repository.getPhotoStorageSize()

        // Then
        assertTrue("Getting photo storage size should succeed", result.isSuccess)
        assertEquals("Should return correct size", expectedSize, result.getOrNull())

        coVerify { mockPhotoStorageManager.getTotalStorageSize() }
    }

    @Test
    fun deleteBean_withPhoto_deletesPhotoFile() = runTest {
        // Given
        val bean = createTestBean("Bean With Photo")
        repository.addBean(bean)

        val photoPath = "/path/to/photo.jpg"

        // Add photo first
        coEvery { mockPhotoStorageManager.savePhoto(any(), bean.id) } returns Result.success(photoPath)
        repository.addPhotoToBean(bean.id, mockk())

        // Setup for deletion
        coEvery { mockPhotoStorageManager.deletePhoto(photoPath) } returns Result.success(Unit)

        // When
        val result = repository.deleteBean(bean)

        // Then
        assertTrue("Deleting bean with photo should succeed", result.isSuccess)

        coVerify { mockPhotoStorageManager.deletePhoto(photoPath) }

        // Verify bean is deleted
        val retrievedBean = repository.getBeanById(bean.id).getOrNull()
        assertNull("Bean should be deleted", retrievedBean)
    }

    @Test
    fun deleteBean_photoDeleteFails_stillDeletesBean() = runTest {
        // Given
        val bean = createTestBean("Bean With Photo")
        repository.addBean(bean)

        val photoPath = "/path/to/photo.jpg"

        // Add photo first
        coEvery { mockPhotoStorageManager.savePhoto(any(), bean.id) } returns Result.success(photoPath)
        repository.addPhotoToBean(bean.id, mockk())

        // Setup for deletion failure
        coEvery { mockPhotoStorageManager.deletePhoto(photoPath) } returns Result.failure(Exception("Delete failed"))

        // When
        val result = repository.deleteBean(bean)

        // Then
        assertTrue("Deleting bean should succeed even if photo delete fails", result.isSuccess)

        coVerify { mockPhotoStorageManager.deletePhoto(photoPath) }

        // Verify bean is still deleted
        val retrievedBean = repository.getBeanById(bean.id).getOrNull()
        assertNull("Bean should be deleted even if photo delete fails", retrievedBean)
    }

    /**
     * Helper function to create test beans with default values.
     */
    private fun createTestBean(
        name: String,
        roastDate: LocalDate = LocalDate.now().minusDays(7),
        notes: String = "Test notes",
        isActive: Boolean = true,
        lastGrinderSetting: String? = null
    ): Bean {
        return Bean(
            name = name,
            roastDate = roastDate,
            notes = notes,
            isActive = isActive,
            lastGrinderSetting = lastGrinderSetting
        )
    }
}
